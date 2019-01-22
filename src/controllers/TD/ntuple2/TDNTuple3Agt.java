package controllers.TD.ntuple2;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import params.ParNT;
import params.ParOther;
import params.ParTD;
import tools.Types;
import tools.Types.ACTIONS;
import tools.Types.ScoreTuple;
import controllers.AgentBase;
import controllers.ExpectimaxWrapper;
import controllers.MaxNWrapper;
import controllers.PlayAgent;
import controllers.RandomAgent;
import controllers.PlayAgent.AgentState;
import controllers.TD.ntuple2.ZValueMulti;
import controllers.TD.ntuple2.TDNTuple2Agt.UpdateType;
import games.Feature;
import games.GameBoard;
import games.StateObservation;
import games.StateObsNondeterministic;
import games.XNTupleFuncs;
import games.XArenaMenu;

/**
 * The SARSA {@link PlayAgent} <b>with n-tuples</b>. 
 * It has a one-layer (perceptron-like) neural network with or without output-nonlinearity  
 * {@code tanh} to model the Q-function. 
 * The net follows closely the (pseudo-)code by [SuttonBarto98]. 
 * <p>
 * Some functionality is packed in the superclasses 
 * {@link AgentBase} (gameNum, maxGameNum, AgentState, ...) and
 * {@link NTupleBase} (finishUpdateWeights, increment*Counters, isTrainable, normalize2, ...)
 * <p>
 * {@link TDNTuple3Agt} is an alternative to {@link TDNTuple2Agt}. 
 * The differences between {@link TDNTuple3Agt} and {@link TDNTuple2Agt} are:
 * <ul>
 * <li> {@link TDNTuple3Agt} updates the value of a state for a player based on the value/reward
 * 		that the <b>same</b> player achieves in his next turn. It is in this way more similar to 
 * 		{@link SarsaAgt}. The updates of {@link TDNTuple2Agt} are based on the value/reward of 
 * 		the <b>next state</b> (may require sign change, depending on the number of players).
 * <li> Eligible states: {@link TDNTuple3Agt} updates with ELIST_PP=true, i.e. it has a separate 
 * 		{@code eList[p]} per player p. {@link TDNTuple2Agt} uses only one common {@code eList[0]}. 
 * 		Only relevant for LAMBDA>0. 
 * </ul>
 * The similarities of {@link TDNTuple3Agt} and {@link TDNTuple2Agt} are:
 * <ul>
 * <li> no eligibility traces, instead LAMBDA-horizon mechanism of [Jaskowski16] (faster and less
 * 		memory consumptive)
 * <li> option AFTERSTATE (relevant only for nondeterministic games like 2048), which builds the value 
 * 		function on the argument afterstate <b>s'</b> (before adding random element) instead 
 * 		of next state <b>s''</b> (faster learning and better generalization).
 * <li> has the random move rate bug fixed: Now EPSILON=0.0 means really 'no random moves'.
 * <li> learning rate ALPHA differently scaled: if ALPHA=1.0, the new value for a
 * 		state just trained will be exactly the target. Therefore, recommended ALPHA values are 
 * 		m*N_s bigger than in {@code TDNTupleAgt}, where m=number of n-tuples, N_s=number of 
 * 		symmetric (equivalent) states. 
 * <li> a change in the update formula: when looping over different equivalent
 * 		states, at most one update per index is allowed (see comment in {@link NTuple2} for 
 * 		member {@code indexList}).
 * </ul>
 * 
 * @see PlayAgent
 * @see AgentBase
 * @see NTupleBase
 * 
 * @author Wolfgang Konen, TH K�ln, Dec'18
 */
public class TDNTuple3Agt extends NTupleBase implements PlayAgent,NTupleAgt,Serializable {
	
	private NTupleAgt.EligType m_elig;
	
	/**
	 * change the version ID for serialization only if a newer version is no longer 
	 * compatible with an older one (older .agt.zip will become unreadable or you have
	 * to provide a special version transformation)
	 */
	private static final long  serialVersionUID = 13L;

	private double BestScore;
	
	private int numPlayers;
	/**
	 * sLast[curPlayer] stores the last afterstate that curPlayer generated in his previous move
	 * (initially null)
	 */
	transient private StateObservation[] sLast;	// last state of player p
//	transient private Types.ACTIONS[] aLast;	// last action of player p
	transient private boolean[] randLast;		// whether last action of player p was a random action
	transient private ScoreTuple rLast;

	private boolean RANDINITWEIGHTS = false;// If true, init weights of value function randomly

	private boolean m_DEBG = false; //false;true;
	// debug printout in collectReward:
	public static boolean DBG_REWARD=false;
	
	// use ternary target in update rule:
	public boolean TERNARY=true;		// remains true only if it is a final-reward-game (see getNextAction2)
	
	
	private int acount=0;
	/**
	 * Default constructor for {@link TDNTuple3Agt}, needed for loading a serialized version
	 */
	public TDNTuple3Agt() throws IOException {
		super();
		ParTD tdPar = new ParTD();
		ParNT ntPar = new ParNT();
		ParOther oPar = new ParOther();
		initNet(ntPar, tdPar, oPar, null, null, 1000);
	}

	/**
	 * Create a new {@link TDNTuple3Agt}
	 * 
	 * @param name			agent name
	 * @param tdPar			temporal difference parameters
	 * @param ntPar			n-tuples and temporal coherence parameter
	 * @param nTuples		the set of n-tuples
	 * @param xnf			contains game-specific n-tuple functions
	 * @param numOutputs	the number of outputs of the n-tuple network (=number of all
	 * 						available actions)
	 * @param maxGameNum	maximum number of training games
	 * @throws IOException
	 */
	public TDNTuple3Agt(String name, ParTD tdPar, ParNT ntPar, ParOther oPar, 
			int[][] nTuples, XNTupleFuncs xnf, int maxGameNum) throws IOException {
		super(name);
		this.numPlayers = xnf.getNumPlayers();
		this.sLast = new StateObservation[numPlayers];
//		this.aLast = new Types.ACTIONS[numPlayers];
		this.randLast = new boolean[numPlayers];
		initNet(ntPar,tdPar,oPar, nTuples, xnf, maxGameNum);			
	}

	/**
	 * 
	 * @param tdPar			temporal difference parameters
	 * @param ntPar			n-tuples and temporal coherence parameter
	 * @param nTuples		the set of n-tuples
	 * @param xnf			contains game-specific n-tuple functions
	 * @param numOutputs	the number of outputs of the n-tuple network (=number of all
	 * 						available actions)
	 * @param maxGameNum	maximum number of training games
	 * @throws IOException
	 */
	private void initNet(ParNT ntPar, ParTD tdPar, ParOther oPar,  
			int[][] nTuples, XNTupleFuncs xnf, int maxGameNum) throws IOException {
		m_tdPar = new ParTD(tdPar);
		m_ntPar = ntPar;
		m_oPar = new ParOther(oPar);		// m_oPar is in AgentBase
		m_elig = (m_tdPar.getEligMode()==0) ? EligType.STANDARD : EligType.RESET;
		rand = new Random(System.currentTimeMillis()); //(System.currentTimeMillis());		(42); 
		
		int posVals = xnf.getNumPositionValues();
		int numCells = xnf.getNumCells();
		
		m_Net = new NTuple2ValueFunc(this,nTuples, xnf, posVals,
				RANDINITWEIGHTS,ntPar,numCells,1);
		
		setNTParams(ntPar);
		
		setTDParams(tdPar, maxGameNum);
		
		setAgentState(AgentState.INIT);
	}

	/**
	 * Get the best next action and return it 
	 * 
	 * @param so			current game state (is returned unchanged)
	 * @param random		allow random action selection with probability m_epsilon
	 * @param silent
	 * @return actBest,		the best action. If several actions have the same
	 * 						score, break ties by selecting one of them at random. 
	 * <p>						
	 * actBest has predicate isRandomAction()  (true: if action was selected 
	 * at random, false: if action was selected by agent).<br>
	 * actBest has also the members vTable and vBest to store the Q-value for each available
	 * action (as returned by so.getAvailableActions()) and the Q-value for the best action actBest, resp.
	 */
	@Override
	public Types.ACTIONS_VT getNextAction2(StateObservation so, boolean random, boolean silent) {
		int i, j;
		double bestValue;
        double value=0;			// the quantity to be maximized
        double otilde, rtilde;
		boolean rgs = this.getParOther().getRewardIsGameScore();
		if (!so.isFinalRewardGame()) this.TERNARY=false;		// we have to use TD target
		StateObservation NewSO;
        Types.ACTIONS actBest = null;
        Types.ACTIONS_VT actBestVT = null;
    	bestValue = -Double.MAX_VALUE;
		double[] VTable;		
		
        otilde = so.getReward(so,rgs);

    	
    	boolean randomSelect;		// true signals: the next action is a random selected one
    	randomSelect = false;
		if (random) {
			randomSelect = (rand.nextDouble() < m_epsilon);
		}
		
        ArrayList<Types.ACTIONS> acts = so.getAvailableActions();
        List<Types.ACTIONS> bestActions = new ArrayList<>();
        
        VTable = new double[acts.size()];  
        
        assert acts.size()>0 : "Oops, no available action";
        for(i = 0; i < acts.size(); ++i)
        {
        	NewSO = so.copy();

    		if (randomSelect) {
    			value = rand.nextDouble();
    		} else {
    	        if (this.getAFTERSTATE()) {
    	        	NewSO.advanceDeterministic(acts.get(i)); 	// the afterstate
    	        	value = this.getScore(NewSO,so); // this is V(s') from so-perspective
    	            NewSO.advanceNondeterministic(); 
    	        } else { 
    	        	// the non-afterstate logic for the case of single moves:
    	        	//System.out.println("NewSO: "+NewSO.stringDescr()+", act: "+act.toInt()); // DEBUG
    	            NewSO.advance(acts.get(i));
    	            value = this.getScore(NewSO,so); // this is V(s'') from so-perspective
    	        }
    	        // both ways of calculating the agent score are the same for deterministic games (s'=s''),
    	        // but they usually differ for nondeterministic games.
            	
    	        rtilde = NewSO.getReward(so,rgs)-otilde;
            	if (TERNARY) {
            		value = NewSO.isGameOver() ? rtilde : getGamma()*value;
            	} else {
        	        value = rtilde + getGamma()*value;
            	}
    	        
    		}
       	
			// just a debug check:
			if (Double.isInfinite(value)) System.out.println("value(NewSO) is infinite!");
			
			value = normalize2(value,so);					
			VTable[i] = value;

			//
			// Calculate the best value and actBest.
			// If there are multiple best actions, select afterwards one of them randomly 
			// (better exploration)
			//
			if (bestValue < value) {
				bestValue = value;
                bestActions.clear();
                bestActions.add(acts.get(i));
			} else if (bestValue == value) {
                bestActions.add(acts.get(i));
			}

        }
        actBest = bestActions.get(rand.nextInt(bestActions.size()));
        // if several actions have the same best value, select one of them randomly

        assert actBest != null : "Oops, no best action actBest";
		if (!silent) {
            NewSO = so.copy();
            NewSO.advance(actBest);
			System.out.println("---Best Move: "+NewSO.stringDescr()+", "+(bestValue));
		}	
		
		actBestVT = new Types.ACTIONS_VT(actBest.toInt(), randomSelect, VTable, bestValue);
		return actBestVT;
	}	
	

		
	/**
	 * Return the agent's estimate of the score for that after state 
	 * For 2-player games like TTT, the score is V(), the probability that 
	 * X (Player +1) wins from that after state. V(s_t|p_t) learns this probability for every t.
	 * p_t*V(s_t) is the quantity to be maximized by getNextAction2.
	 * For 1-player games like 2048 it is the estimated (total or future) reward.
	 * 
	 * @param so			the state for which the value is desired
	 * @return the agent's estimate of the future score for that after state
	 */
	public double getScore(StateObservation so) {
		int[] bvec = m_Net.xnf.getBoardVector(so);
		double score = m_Net.getScoreI(bvec,so.getPlayer());
		return score;
	}

	/**
	 * Return the agent's estimate of the score for that after state 
	 * (both versions, {@link VER_3P}==true/false).
	 * Return V(s_t|p_refer), that is the value function from the perspective of the player
	 * who moves in state {@code refer}. 
	 * For 1-player games like 2048 it is the estimated (total or future) reward.
	 * 
	 * @param so	the state s_t for which the value is desired
	 * @param refer	the referring state
	 * @return		V(s_t|p_refer), the agent's estimate of the future score for s_t
	 * 				from the perspective of the player in state {@code refer}
	 */
	public double getScore(StateObservation so, StateObservation refer) {
		double score;
		int[] bvec = m_Net.xnf.getBoardVector(so);
		score = m_Net.getScoreI(bvec,refer.getPlayer());
		//score = getScore(so,refer.getPlayer());	
    	return score;
	}

	/**
	 * Return the agent's estimate of {@code sob}'s final game value (final reward) <b>for all players</b>. 
	 * Is called by the n-ply wrappers ({@link MaxNWrapper}, {@link ExpectimaxWrapper}). 
	 * 
	 * @param so	the state s_t for which the value is desired
	 * @return		an N-tuple with elements V(s_t|i), i=0,...,N-1, the agent's estimate of 
	 * 				the future score for s_t from the perspective of player i
	 */
	@Override
	public ScoreTuple getScoreTuple(StateObservation so) {
		throw new RuntimeException("getScoreTuple(StateObservation so) not available for TDNTuple3Agt");			        		
	}

	/**
	 * Return the agent's estimate of {@code sob}'s final game value (final reward) <b>for all players</b>. <br>
	 * Is only called when training an agent in multi-update mode AND the maximum episode length
	 * is reached. 
	 * 
	 * @param sob			the current game state
	 * @return				the agent's estimate of the final game value <b>for all players</b>. 
	 * 						The return value is a tuple containing  
	 * 						{@link StateObservation#getNumPlayers()} {@code double}'s. 
	 */
	@Override
	public ScoreTuple estimateGameValueTuple(StateObservation sob) {
		boolean rgs = m_oPar.getRewardIsGameScore();
		ScoreTuple sc = new ScoreTuple(sob);
		for (int i=0; i<sob.getNumPlayers(); i++) 
			sc.scTup[i] = sob.getReward(i, rgs);
			// this is valid, but it may be a bad estimate in games where the reward is only 
			// meaningful for game-over-states.
		return sc;
	}

	/**
	 * Adapt the n-tuple weights for state {@code sLast[curPlayer]}, the last afterstate that current
	 * player generated, towards the target derived from {@code s_next}.
	 * 
	 * @param curPlayer		the player to move in state {@code ns.getSO()}
	 * @param R				the (cumulative) reward tuple received when moving into {@code s_next}
	 * @param ns			the {@code NextState} object holds the afterstate {@code ns.getAfterState()}
	 * 						and the next state {@code s_next=ns.getNextSO()}
	 */
	private void adaptAgentV(int curPlayer, ScoreTuple R, NextState ns) {
		StateObservation s_after = ns.getAfterState();
		StateObservation s_next = ns.getNextSO();
		int[] curBoard;
		double v_next,vLast,vLastNew,target;
		boolean learnFromRM = m_oPar.useLearnFromRM();
		
		if (s_next.isGameOver()) {
			v_next = 0.0;
		} else {
			int[] bvec = m_Net.xnf.getBoardVector(s_after);
        	v_next = m_Net.getScoreI(bvec,curPlayer);
		}
		
		if (sLast[curPlayer]!=null) {
			// delta reward from curPlayer's perspective when moving into s_next
			double r_next = R.scTup[curPlayer] - rLast.scTup[curPlayer];  
        	if (TERNARY) {
        		target = s_next.isGameOver() ? r_next : getGamma()*v_next;
        	} else {
    			target = r_next + getGamma()*v_next;        		
        	}
//			if (target==-1.0) {
//				int dummy=0;
//			}
//			if (Math.abs(v_next)>0.7) {
//				int dummy=0;
//			}
			curBoard = m_Net.xnf.getBoardVector(sLast[curPlayer]); 
        	vLast = m_Net.getScoreI(curBoard,curPlayer);
        	
        	// if last action of nextPlayer was a random move: 
    		if (randLast[curPlayer] && !learnFromRM && !s_next.isGameOver()) {
    			// no training, go to next move.
    			if (m_DEBG) System.out.println("random move");
    			
    			m_Net.clearEligList(m_elig);	// the list is only cleared if m_elig==RESET
    				
    		} else {
    			m_Net.updateWeightsTD(curBoard, curPlayer, vLast, r_next,target,ns.getSO());
    		}
    		
    		//debug only:
			if (m_DEBG) {
	    		if (s_next.isGameOver()) {
	            	vLastNew = m_Net.getScoreI(curBoard,curPlayer);
	            	int dummy=1;
	    		}
	    		String s1 = sLast[curPlayer].stringDescr();
	    		String s2 = s_next.stringDescr();
	    		if (target!=0.0) {//(target==-1.0) { //(s_next.stringDescr()=="XoXX-oXo-") {
	            	vLastNew = m_Net.getScoreI(curBoard,curPlayer);
	            	System.out.println(s1+" "+s2+","+vLast+"->"+vLastNew+" target="+target
	            			+" player="+(curPlayer==0 ? "X" : "O"));
	            	if (++acount % 50 ==0) {
	            		int dummy=1;
	            	}
	    		}
			}

		}  // if(sLast[..]!=null)
	}
	
	private void finalAdaptAgents(int curPlayer, ScoreTuple R, NextState ns) {
		double target,vLast,vLastNew;
		int[] curBoard, nextBoard;
		StateObservation s_after = ns.getAfterState();
		StateObservation s_next = ns.getNextSO();
		
		for (int n=0; n<numPlayers; n++) {
			if (n!=curPlayer) {
				// adapt the value of the last state sLast[n] of each player other than curPlayer
				// towards the reward received when curPlayer did his terminal move
				if (sLast[n]!=null ) { 
					target = R.scTup[n] - rLast.scTup[n]; 		// delta reward
			        // TODO: think whether the subtraction rlast.scTup[n] is right for every n
					//		 (or whether we need to correct rLast before calling finalAdaptAgents)
					curBoard = m_Net.xnf.getBoardVector(sLast[n]); 
		        	vLast = m_Net.getScoreI(curBoard,n);
		        	
	    			m_Net.updateWeightsTD(curBoard, n, vLast, R.scTup[n], target, ns.getSO());

	    			//debug only:
	    			if (m_DEBG) {
	    	    		if (s_next.isGameOver()) {
	    	            	vLastNew = m_Net.getScoreI(curBoard,n);
	    	            	int dummy=1;
	    	    		}
	    	    		String s1 = sLast[n].stringDescr();
	    	    		String s2 = s_next.stringDescr();
	    	    		if (target!=0.0) {//(target==-1.0) { //(s_next.stringDescr()=="XoXX-oXo-") {
	    	            	vLastNew = m_Net.getScoreI(curBoard,n);
	    	            	System.out.println(s1+" "+s2+","+vLast+"->"+vLastNew+" target="+target
	    	            			+" player="+(n==0 ? "X" : "O")+" (f)"+this.getGameNum());
	    	            	if (++acount % 50 ==0) {
	    	            		int dummy=1;
	    	            	}
	    	    		}
	    			}
				}
			} else { // if n==curPlayer
				// adapt the value of the *next state* that curPlayer observed after he did his 
				// final move towards target 0. (This is only relevant for TERNARY==false, since 
				// only then the value of this next state is used in getNextAction2.)
				curBoard = m_Net.xnf.getBoardVector(s_next); 
	        	vLast = m_Net.getScoreI(curBoard,curPlayer);
	        	
    			m_Net.updateWeightsTD(curBoard, curPlayer, vLast, R.scTup[curPlayer], 0.0, s_next);
				
			}
		} // for
		
	}
	
	/**
	 * Train the agent for one complete game episode <b>using self-play</b>. <p>
	 * Side effects: Increment m_GameNum by +1. Change the agent's internal  
	 * parameters (weights and so on).
	 * @param so		the state from which the episode is played (usually the
	 * 					return value of {@link GameBoard#chooseStartState(PlayAgent)} to get
	 * 					some exploration of different game paths)
	 * @return			true, if agent raised a stop condition (only CMAPlayer)	 
	 */
	public boolean trainAgent(StateObservation so) {
		double[] VTable = null;
		double reward = 0.0;
		Types.ACTIONS_VT actBest;
		Types.ACTIONS a_next, a_t;
		int   nextPlayer, curPlayer=so.getPlayer();
		NextState ns = null;
		ScoreTuple R = new ScoreTuple(so);
		rLast = new ScoreTuple(so);

		boolean learnFromRM = m_oPar.useLearnFromRM();
		int epiLength = m_oPar.getEpisodeLength();
		if (epiLength==-1) epiLength = Integer.MAX_VALUE;
				
		int t=0;
		StateObservation s_t = so.copy();
		for (int n=0; n<numPlayers; n++) {
			sLast[n] = null;
//			sLast[n] = (n==curPlayer ? s_t.getPrecedingAfterstate() : null); // curPlayer is so.getPlayer()
		}
		do {
	        m_numTrnMoves++;		// number of train moves (including random moves)
	        
	        // choose action a_t, using epsilon-greedy policy based on V
			a_t = getNextAction2(s_t, true, true);
	               
	        // take action a_t and observe reward & next state 
	        ns = new NextState(this,s_t,a_t);	
	        curPlayer = ns.getSO().getPlayer();
	        nextPlayer = ns.getNextSO().getPlayer();
	        R = ns.getNextRewardTupleCheckFinished(epiLength);
	        
	        adaptAgentV(curPlayer, R, ns);
	        
	        // we differentiate between the afterstate (on which we learn) and the 
	        // next state s_t, which may have environment random elements added and from which 
	        // we advance. 
	        // (for deterministic games, ns.getAfterState() and ns.getNextSO() are the same)
	        sLast[curPlayer] = ns.getAfterState();
	        randLast[curPlayer] = a_t.isRandomAction();
	        rLast.scTup[curPlayer] = R.scTup[curPlayer];
	        s_t = ns.getNextSO();
			t++;
			
		} while(!s_t.isGameOver());
		
		finalAdaptAgents(curPlayer, R, ns);
		
		
		try {
			this.finishUpdateWeights();		// adjust learn params ALPHA & m_epsilon
		} catch (Throwable e) {
			e.printStackTrace();
		}
		
		//System.out.println("episode: "+getGameNum()+", moveNum="+m_counter);
		incrementGameNum();
		if (this.getGameNum() % 500 == 0) {
			System.out.println("gameNum: "+this.getGameNum());
		}
		
		return false;
		
	} // trainAgent


	public String stringDescr() {
		m_Net.setHorizon();
		String cs = getClass().getName();
		String str = cs + ": USESYMMETRY:" + (m_ntPar.getUSESYMMETRY()?"true":"false")
						+ ", NORMALIZE:" + (m_tdPar.getNormalize()?"true":"false")
						+ ", sigmoid:"+(m_Net.hasSigmoid()? "tanh":"none")
						+ ", lambda:" + m_Net.getLambda()
						+ ", horizon:" + m_Net.getHorizon()
						+ ", AFTERSTATE:" + (m_ntPar.getAFTERSTATE()?"true":"false")
						+ ", learnFromRM: " + (m_oPar.useLearnFromRM()?"true":"false");
		return str;
	}
		
	public String stringDescr2() {
		String cs = getClass().getName();
		String str = cs + ": alpha_init->final:" + m_tdPar.getAlpha() + "->" + m_tdPar.getAlphaFinal()
						+ ", epsilon_init->final:" + m_tdPar.getEpsilon() + "->" + m_tdPar.getEpsilonFinal()
						+ ", gamma: " + m_tdPar.getGamma()
						+ ", "+stringDescrNTuple();		// see NTupleBase
		return str;
	}
		
	public String printTrainStatus() {
		DecimalFormat frm = new DecimalFormat("#0.0000");
		DecimalFormat frme= new DecimalFormat();
		frme = (DecimalFormat) NumberFormat.getNumberInstance(Locale.UK);		
		frme.applyPattern("0.0E00");  

		String cs = ""; //getClass().getName() + ": ";   // optional class name
		String str = cs + "alpha="+frm.format(m_Net.getAlpha()) 
				   + ", epsilon="+frm.format(getEpsilon())
				   //+ ", lambda:" + m_Net.getLambda()
				   + ", "+getGameNum() + " games"
				   + " ("+frme.format(getNumLrnActions()) + " learn actions)";
		if (this.m_Net.getNumPlayers()==2) 
			str = str + ", (winX/tie/winO)=("+winXCounter+"/"+tieCounter+"/"+winOCounter+")";
		winXCounter=tieCounter=winOCounter=0;
		return str;
	}

	// Callback function from constructor NextState(NTupleAgt,StateObservation,ACTIONS). 
	// It sets various elements of NextState ns (nextReward, nextRewardTuple).
	// It is part of TDNTuple3Agt (and not part of NextState), because it uses various elements
	// private to TDNTuple3Agt (DBG_REWARD, referringState, normalize2)
	public void collectReward(NextState ns) {
		boolean rgs = m_oPar.getRewardIsGameScore();
		ns.nextRewardTuple = new ScoreTuple(ns.refer);
		for (int i=0; i<ns.refer.getNumPlayers(); i++) {
			ns.nextRewardTuple.scTup[i] = normalize2(ns.nextSO.getReward(i,rgs),ns.nextSO);
		}

		// for completeness, ns.nextReward is not really needed in TDNTuple3Agt
		ns.nextReward = normalize2(ns.nextSO.getReward(ns.nextSO,rgs),ns.refer);

		if (DBG_REWARD && ns.nextSO.isGameOver()) {
			System.out.print("Rewards: ");
			System.out.print(ns.nextRewardTuple.toString());
//			System.out.print("Reward: "+ns.nextReward);
			System.out.println("   ["+ns.nextSO.stringDescr()+"]  " + ns.nextSO.getGameScore() + " for player " + ns.nextSO.getPlayer());
		}
	}
	
}