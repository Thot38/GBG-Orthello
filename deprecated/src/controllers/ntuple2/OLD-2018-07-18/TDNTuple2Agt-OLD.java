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

import org.apache.commons.math3.stat.descriptive.rank.Min;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

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
import controllers.PlayAgent.AgentState;
import games.Feature;
import games.GameBoard;
import games.StateObservation;
import games.StateObsNondeterministic;
import games.XNTupleFuncs;
import games.CFour.StateObserverC4;
import games.RubiksCube.StateObserverCube;
import games.XArenaMenu;
import games.ZweiTausendAchtundVierzig.StateObserver2048;

/**
 * The TD-Learning {@link PlayAgent} (Temporal Difference reinforcement learning)
 * <b>with n-tuples</b>. 
 * It has a one-layer (perceptron-like) neural network with or without output-nonlinearity  
 * {@code tanh} to model the value function. 
 * The net follows closely the (pseudo-)code by [SuttonBonde93]. 
 * <p>
 * Some functionality is packed in the superclass 
 * {@link AgentBase} (gameNum, maxGameNum, AgentState, ...)
 * <p>
 * {@link TDNTuple2Agt} replaces the older {@code TDNTupleAgt}. 
 * The differences of {@link TDNTuple2Agt} to {@code TDNTupleAgt} are:
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
 * 
 * @author Wolfgang Konen, TH K�ln, Aug'17
 */
public class TDNTuple2Agt extends AgentBase implements PlayAgent,Serializable {
	/**
	 * UpdateType characterizes the weight update mode: <ul>
	 * <li> <b>SINGLE_UPDATE</b>: Each learn action updates only a single value function (that of the current player)
	 * <li> <b>MULTI_UPDATE</b>: Each learn action updates multiple value functions (of all players) 
	 * </ul>
	 * @see TDNTuple2Agt#getUpdateType(StateObservation)
	 */
	private enum UpdateType {SINGLE_UPDATE, MULTI_UPDATE};
	
	/**
	 * EligType characterizes the eligibility trace mode: <ul>
	 * <li> <b>STANDARD</b>: nothing happens on random move (RM) with the elig traces (only no update on RM)
	 * <li> <b>RESET</b>: all elig traces are reset after RM (because a later 
	 * 		reward has no meaning for states prior to RM)
	 * </ul>
	 * @see NTuple2ValueFunc#clearEligList(EligType)
	 */
	enum EligType {STANDARD, RESET}; // may later contain also REPLACE, RESREP
	private EligType m_elig;
	
	private Random rand; // generate random Numbers 
	
	/**
	 * change the version ID for serialization only if a newer version is no longer 
	 * compatible with an older one (older .agt.zip will become unreadable or you have
	 * to provide a special version transformation)
	 */
	private static final long  serialVersionUID = 13L;

	/**
	 * Controls the amount of explorative moves in
	 * {@link #getNextAction2(StateObservation, boolean, boolean)}
	 * during training. <br>
	 * m_epsilon = 0.0: no random moves, <br>
	 * m_epsilon = 0.1 (def.): 10% of the moves are random, and so forth
	 * m_epsilon undergoes a linear change from {@code tdPar.getEpsilon()} 
	 * to {@code tdPar.getEpsilonFinal()}. 
	 * This is realized in {@link #finishUpdateWeights()}.
	 */
	private double m_epsilon = 0.1;
	
	/**
	 * m_EpsilonChangeDelta is the epsilon change per episode.
	 */
	private double m_EpsilonChangeDelta = 0.001;
	
	private double BestScore;
	
	private boolean TC; //true: using Temporal Coherence algorithm
	private int tcIn; 	//temporal coherence interval: after tcIn games tcFactor will be updates
	private boolean tcImm=true;		//true: immediate TC update, false: batch update (epochs)
	
	// not needed anymore here, we hand in int[][] nTuples = = ntupfac.makeNTupleSet(...) 
	// to the constructor: 
//	private boolean randomness=false; //true: ntuples are created randomly (walk or points)
//	private boolean randWalk=true; 	//true: random walk is used to generate nTuples
//									//false: random points is used to generate nTuples//samine//

	// Value function of the agent.
	private NTuple2ValueFunc m_Net;
	
	// m_Net3 is only needed for DEBG_NEW_3P: it is a parallel structure learning the value 
	// function for the old TD update scheme (as if VER_3P==false) even in the case
	// VER_3P==true. The purpose is to compare step-by-step with the results from m_Net.
	private NTuple2ValueFunc m_Net3;	
	private double ZScore_NTL2;

	// Use symmetries (rotation, mirror) in NTuple-System
//	protected boolean USESYMMETRY = true; 	// use now m_ntPar.getUseSymmetry() - don't store/maintain value twice
//	private boolean learnFromRM = false;    // use now m_oPar.useLearnFromRM() - don't store/maintain value twice
	
//	/**
//	 * NORMALIZE gets the value tdPar.getNormalize(). If true, perform score range normalization
//	 * in {@link #normalize2(double, StateObservation)}.
//	 */
//	private boolean NORMALIZE = false; 
	// --- commented out, we uses now tdPar.getNormalize() directly ---
	
	private boolean RANDINITWEIGHTS = false;// If true, init weights of value function randomly

	private boolean PRINTTABLES = false;	// /WK/ control the printout of tableA, tableN, epsilon
//	public static boolean NEWTARGET=true;	// this is now always true
//	public static boolean WITH_NS=true;		// this is now always true
	/**
	 * {@link #VER_3P} (Version 3P) is a boolean switch to decide whether to use the new logic
	 * valid for 1,2,3-,...,N-player games or to use the older 2-player logic.
	 * <p>
	 * If {@link #VER_3P}==true, use the new logic for 1,2,3-,...,N-player games: 
	 * <ul>
	 * <li> If {@link #MODE_3P}==0: Each player has its own reward function 
	 * and its own value function V(s_t|p^(i)). In each update step N value function updates occur. 
	 * <li> If {@link #MODE_3P}==1: Use the new n-ply logic with n=N as 
	 * described in TR-TDNTuple.pdf: one value function V(s_t|p_t) and each player maximizes
	 * its own game value or minimizes the next opponent's game value. 
	 * <li> If {@link #MODE_3P}==2 [<b>recommended</b>]: Use {@link #MODE_3P}==0 for N=1 and N &gt; 2.  
	 * For N==2, use a logic equivalent to 1-ply {@link #MODE_3P}==1 where the symmetry 
	 * 		V(s_t+1|p_t) = - V(s_t+1|p_t+1)
	 * allows to make only one weight update for V(s_t|p_t) but to have all V(s_t|p^(i)).
	 * </ul>
	 * If {@link #VER_3P}==false, proceed with a 2-player logic (see Javadoc for 
	 * {@link #NEW_2P}).
	 * <p>
	 * More details in document TR_GBG/TR-TDNTuple.pdf. 
	 */
	public static boolean VER_3P=true; 
	/**
	 * The actual value is set from {@link #m_tdPar}{@code .getMode3P()}. <br>
	 * See {@link #VER_3P} for more details.
	 */
	public int MODE_3P=1; 		// 0/1/2, the actual value is set in initNet from m_tdPar.getMode3P()
//	public static boolean OLD_3P=false; // OLD_3P=true  <--> MODE_3P=0
										// OLD_3P=false	<--> MODE_3P=1		
	
	/**
	 * This is a switch for 2-player games with antagonistic rewards. It is relevant only if 
	 * {@link #VER_3P}==false.
	 * <ul>
	 * <li> If {@link #NEW_2P}==false, use the old logic (prior to 10/2017): The target to learn is the 
	 * value/reward for the 1st player and only one value function update is needed (exploits 
	 * the 1- and 2-player symmetry). 
	 * <li> If {@link NEW_2P}==true, use the new logic: The target to learn is the negative of the 
	 * next state's reward+value from the perspective of the opponent.<p>
	 * </ul>
	 * Both techniques are equivalent in results and yield -- if applicable -- better results 
	 * than [{@link #VER_3P}==true, {@link #MODE_3P}==0]. But they are not generalizable to other than 2-player games.
	 * <p>
	 * {@link #NEW_2P}==true is the <b>recommended</b> choice (simpler to explain and to code).
	 */
	public static boolean NEW_2P=true;

//	// if NEW_GNA==true: use the new function getNextAction2,3 in getNextAction;
//	//    (the new version which returns Types.ACTIONS_VT and allows deterministic multi-moves)
//	// if NEW_GNA==false: use the old function getNextAction1 in getNextAction;
//	//    (the old version which returns Types.ACTIONS and does not allow multi-moves)
//	private static boolean NEW_GNA=true;	// this is now always true
	
	private boolean m_DEBG = false;
	static transient public PrintStream pstream = System.out;
	// debug printout in getNextAction, trainAgent:
	public static boolean DBG2_TARGET=false;
	// debug printout in updateWeightsNewTerminal:
	public static boolean DBGF_TARGET=false;
	// debug: repeat always the same sequence in one episode (to check a trivial convergence)
	public static boolean DBG2_FIXEDSEQUENCE=false;
	// debug printout in updateWeightsNew, g3_Evaluate, NextState:
	public static boolean DBG_REWARD=false;
	// debug VER_3P=true, MODE_3P=0: m_Net3 is updated in parallel to m_Net (compare 2P and 3P), 
	// extra printout v_old,v_new in NTuple2ValueFunc::updateWeightsNew,updateWeightsNewTerminal 
	public static boolean DBG_OLD_3P=false;
	// debug VER_3P=true, MODE_3P=1:  
	public static boolean DBG_NEW_3P=false;
	// debug for Rubik's Cube:
	public static boolean DBG_CUBE=true;
	// use ternary (old) target in update rule:
	public static boolean TERNARY=true;
	
	// is set to true in getNextAction3(...), if the next action is a random selected one:
	private boolean randomSelect = false;
	
	/**
	 * Members {@link #m_tdPar}, {@link #m_ntPar}, {@link AgentBase#m_oPar} are needed for 
	 * saving and loading the agent (to restore the agent with all its parameter settings)
	 */
	private ParTD m_tdPar;
	private ParNT m_ntPar;
	
	//
	// variables needed in various train methods
	//
	private int m_counter = 0;				// episode move counter (trainAgent)
	private boolean m_finished = false;		// whether a training game is finished
	private boolean m_randomMove = false;	// whether the last action was random
	
	// info only:
	private int tieCounter = 0;
	private int winXCounter = 0;
	private int winOCounter = 0;


	/**
	 * Default constructor for {@link TDNTuple2Agt}, needed for loading a serialized version
	 */
	public TDNTuple2Agt() throws IOException {
		super();
		ParTD tdPar = new ParTD();
		ParNT ntPar = new ParNT();
		ParOther oPar = new ParOther();
		initNet(ntPar, tdPar, oPar, null, null, 1000);
	}

	/**
	 * Create a new {@link TDNTuple2Agt}
	 * 
	 * @param name			agent name
	 * @param tdPar			temporal difference parameters
	 * @param ntPar			n-tuples and temporal coherence parameter
	 * @param nTuples		the set of n-tuples
	 * @param xnf			contains game-specific n-tuple functions
	 * @param maxGameNum	maximum number of training games
	 * @throws IOException
	 */
	public TDNTuple2Agt(String name, ParTD tdPar, ParNT ntPar, ParOther oPar, 
			int[][] nTuples, XNTupleFuncs xnf, int maxGameNum) throws IOException {
		super(name);
		initNet(ntPar,tdPar,oPar, nTuples, xnf, maxGameNum);			
	}

	/**
	 * 
	 * @param tdPar			temporal difference parameters
	 * @param ntPar			n-tuples and temporal coherence parameter
	 * @param nTuples		the set of n-tuples
	 * @param xnf			contains game-specific n-tuple functions
	 * @param maxGameNum	maximum number of training games
	 * @throws IOException
	 */
	private void initNet(ParNT ntPar, ParTD tdPar, ParOther oPar,  
			int[][] nTuples, XNTupleFuncs xnf, int maxGameNum) throws IOException {
		m_tdPar = new ParTD(tdPar);
		m_ntPar = ntPar;
		m_oPar = new ParOther(oPar);		// m_oPar is in AgentBase
		MODE_3P = m_tdPar.getMode3P();
		m_elig = (m_tdPar.getEligMode()==0) ? EligType.STANDARD : EligType.RESET;
		rand = new Random(System.currentTimeMillis());		//(42); 
		
		int posVals = xnf.getNumPositionValues();
		int numCells = xnf.getNumCells();
		
		m_Net = new NTuple2ValueFunc(this,nTuples, xnf, posVals,
				RANDINITWEIGHTS,ntPar,numCells);
		if (DBG_OLD_3P || DBG_NEW_3P) {
			m_Net3 = new NTuple2ValueFunc(this,nTuples, xnf, posVals,
					RANDINITWEIGHTS,ntPar,numCells);
		}
		
		setNTParams(ntPar);
		
		setTDParams(tdPar, maxGameNum);
		
		setAgentState(AgentState.INIT);
	}

	/**
	 * Get the best next action and return it 
	 * (NEW version: ACTIONS_VT and recursive part for multi-moves)
	 * 
	 * @param so			current game state (is returned unchanged)
	 * @param random		allow random action selection with probability m_epsilon
	 * @param silent
	 * @return actBest		the best action. If several actions have the same
	 * 						score, break ties by selecting one of them at random. 
	 * <p>						
	 * actBest has predicate isRandomAction()  (true: if action was selected 
	 * at random, false: if action was selected by agent).<br>
	 * actBest has also the members vTable and vBest to store the value for each available
	 * action (as returned by so.getAvailableActions()) and the value for the best action actBest.
	 */
	@Override
	public Types.ACTIONS_VT getNextAction2(StateObservation so, boolean random, boolean silent) {
		return getNextAction3(so, so, random, silent);
	}
	
	// this function was just needed for EvaluatorCube if we want to avoid symmetries
	// definitely during evaluation. But it is no longer really needed, since we avoid 
	// symmetries throughout when doing Rubiks Cube.
	@Deprecated
	public Types.ACTIONS_VT getNextAction2SYM(StateObservation so, boolean random, boolean silent, boolean useSymmetry) {
		boolean oldSymmetry = m_ntPar.getUSESYMMETRY();
		m_ntPar.setUSESYMMETRY(useSymmetry);
		Types.ACTIONS_VT actBest = getNextAction3(so, so, random, silent);
		m_ntPar.setUSESYMMETRY(oldSymmetry);
		return actBest;
	}
	
	// 
	// this private function is needed so that the recursive call inside getNextAction3 (see 
	// g3_Evalualte) can transfer the referring state refer. 
	// (function GETNEXTACTIONNPLY in TR-TDNTuple.pdf, Algorithm 1)
	private Types.ACTIONS_VT getNextAction3(StateObservation so, StateObservation refer, 
			boolean random, boolean silent) {
		int i, j;
		double BestScore3;
		double CurrentScore = 0; 	// NetScore*Player, the quantity to be
									// maximized
		boolean rgs = m_oPar.getRewardIsGameScore();
		StateObservation NewSO;
	    int iBest;
		int count = 1; // counts the moves with same BestScore3
        Types.ACTIONS actBest = null;
        Types.ACTIONS_VT actBestVT = null;
    	BestScore3 = -Double.MAX_VALUE;
		double[] VTable;		
	
        randomSelect = false;
		if (random) {
			randomSelect = (rand.nextDouble() < m_epsilon);
		}
		
		// get the best (or eps-greedy random) action
        ArrayList<Types.ACTIONS> acts = so.getAvailableActions();
        List<Types.ACTIONS> bestActions = new ArrayList<>();
        double agentScore;
        
        VTable = new double[acts.size()];  
        
		UpdateType UPDATE = getUpdateType(so);
		
//		if (acts.size()==0) {
//			int dummy=1;
//		}
        assert acts.size()>0 : "Oops, no available action";
        for(i = 0; i < acts.size(); ++i)
        {
            if (VER_3P && UPDATE==UpdateType.SINGLE_UPDATE) {
            	// this has n-ply recursion, but does not reflect multi-moves:
            	//
            	int nply = (MODE_3P==1) ? so.getNumPlayers() : 1;
                CurrentScore = g3_Eval_NPly(so,acts.get(i),refer,nply,silent);   

//              if (DBG_NEW_3P && so.getNumPlayers()==1) 
//                sanityCheck1P(so,acts,i,refer,silent,CurrentScore);
                
            } else {	// i.e. !VER_3P OR (UPDATE==UpdateType.MULTI_UPDATE)
            	// this has recursive multi-move recursion:
                CurrentScore = g3_Evaluate(so,acts.get(i),refer,silent);
            }
            
			// just a debug check:
			if (Double.isInfinite(CurrentScore)) {
				System.out.println("getScore(NewSO) is infinite!");
			}
			
			CurrentScore = normalize2(CurrentScore,so);					
			VTable[i] = CurrentScore;
			
			//
			// Calculate the best score BestScore3 and actBest.
			// If there are multiple best actions, select afterwards one of them randomly 
			// (better exploration)
			//
			if (BestScore3 < CurrentScore) {
				BestScore3 = CurrentScore;
                bestActions.clear();
                bestActions.add(acts.get(i));
			} else if (BestScore3 == CurrentScore) {
                bestActions.add(acts.get(i));
			}

        } // for (i)
        actBest = bestActions.get(rand.nextInt(bestActions.size()));
        // if several actions have the same best score, select one of them randomly

        assert actBest != null : "Oops, no best action actBest";
		if (!silent) {
			int playerPM = calculatePlayerPM(refer); 	 
			System.out.print("---Best Move: ");
            NewSO = so.copy();
            NewSO.advance(actBest);
			System.out.println(NewSO.stringDescr()+", "+(2*BestScore3*playerPM-1));
		}			
		if (DBG2_TARGET) {
			int playerPM = calculatePlayerPM(refer); 	 
			final double MAXSCORE = ((so instanceof StateObserver2048) ? 3932156 : 1);
			// here we use the NextState version, because computation time does not matter
			// inside DBG2_TARGET and because this version is correct for both values of 
			// getAFTERSTATE():
	        NextState ns = new NextState(so,actBest);
			double deltaReward = ns.getNextSO().getReward(so,rgs) - so.getReward(so,rgs);
			double sc = (deltaReward + playerPM * getScore(ns.getAfterState()))*MAXSCORE;
			
			System.out.println("getScore((so,actbest)-afterstate): "+sc+"   ["+so.stringDescr()+"]");
			int dummy=1;
		}
		
		actBestVT = new Types.ACTIONS_VT(actBest.toInt(), randomSelect, VTable, BestScore3);
		return actBestVT;
	} // getNextAction3

    // calculate CurrentScore: 
	// (g3_Evaluate is helper function for getNextAction3, if MODE_3P==0 or ==2)
    // (function EVALUATE in TR-TDNTuple.pdf, Algorithm 6)
    private double g3_Evaluate(	StateObservation so, Types.ACTIONS act, 
    							StateObservation refer, boolean silent) {
    	double CurrentScore,agentScore;
		int rplayer = refer.getPlayer();
		boolean rgs = m_oPar.getRewardIsGameScore();

        double referReward = refer.getReward(refer,rgs); // 0; 
    	Types.ACTIONS_VT actBestVT;
    	StateObservation NewSO;
    	StateObservation refer2 = refer; 
        double kappa = 1.0;
    			
		if (randomSelect) {
			CurrentScore = rand.nextDouble();
			return CurrentScore;
		} 
		
        NewSO = so.copy();
    	if (VER_3P==true && MODE_3P==2 && so.getNumPlayers()==2) {
    		refer2 = NewSO;
        	kappa = (NewSO.getPlayer()==refer.getPlayer()) ? +1.0 : -1.0;
    	}
    	// This will normally result in kappa = -1.0. Only in the case of multi-moves, 
    	// where NewSO.getPlayer()==so.getPlayer() can happen, we may have kappa = +1.0.
        
        if (getAFTERSTATE()) {
        	NewSO.advanceDeterministic(act); 	// the afterstate
        	agentScore = getScore(NewSO,refer2); // this is V(s')
            NewSO.advanceNondeterministic(); 
        } else { 
        	// the non-afterstate logic for the case of single moves:
            NewSO.advance(act);
        	agentScore = getScore(NewSO,refer2); // this is V(s'')
        }
        // both ways of calculating the agent score are the same for deterministic games (s'=s''),
        // but they usually differ for nondeterministic games.
        
        boolean CUBE_DBG=false;
        if (CUBE_DBG && !m_ntPar.getUSESYMMETRY()) {
        	System.out.println(so + " <|> " +NewSO + " : " + agentScore);
        }
        
        agentScore *= kappa; 
        // kappa is -1 (and not 1) only for VER_3P==true && MODE_3P==2 && so.getNumPlayers()==2 
        // and only if NewSO and refer have different players.


        // the recursive part (only for deterministic games) is for the case of 
        // multi-moves: the player who just moved gets from StateObservation 
        // the signal for one (or more) additional move(s)
        if (so.isDeterministicGame() && so.getNumPlayers()>1 && !NewSO.isGameOver()) {
            if (NewSO.getPlayer()==rplayer) {
            	actBestVT = getNextAction3(NewSO, refer, false, silent);
            	NewSO.advance(actBestVT);
            	CurrentScore = actBestVT.getVBest();
            	return CurrentScore;
            }
        }
	            
		int playerPM = calculatePlayerPM(refer); 	
		// new target logic:
		// the score is the reward received for the transition from refer to NewSO 
		// 		(NewSO.getReward(refer)-referReward)
		// plus the estimated future rewards until game over (agentScore=getScore(NewSO), 
		// the agent's value function for NewSO)
		CurrentScore = (NewSO.getReward(refer,rgs) - referReward) + getGamma()*playerPM*agentScore;				

		if (!silent || DBG_REWARD) {
			//System.out.println(NewSO.stringDescr()+", "+(2*CurrentScore*playerPM-1));
			double deltaReward = (NewSO.getReward(refer,rgs) - referReward);
			System.out.println(NewSO.stringDescr()+", "+CurrentScore+", "+deltaReward);
			//print_V(Player, NewSO.getTable(), 2 * CurrentScore * Player - 1);
		}

		return CurrentScore;
    }  // g3_Evaluate

    // calculate CurrentScore: 
	// (g3_Eval_NPly is helper function for getNextAction3, if SINGLE_UPDATE, i.e. 1) MODE_3P==1 or 
    // 2) (MODE_3P==2 && N=2). In case 2) we have nply=1. )
    // (function EVALUATENPLY in TR-TDNTuple.pdf, Algorithm 2)
    private double g3_Eval_NPly(StateObservation so, Types.ACTIONS act, 
    							StateObservation refer, int nply, boolean silent) {
    	double CurrentScore,agentScore;
    	double g3BestScore = -Double.MAX_VALUE;
		int rplyer = refer.getPlayer();
		boolean rgs = m_oPar.getRewardIsGameScore();

        double referReward = refer.getReward(refer,rgs); // 0; 
        double rtilde,otilde;
        double kappa;
	    int iBest;
		int count = 1; // counts the moves with same g3BestScore
        Types.ACTIONS actBest = null;
    	StateObservation NewSO;
    	StateObservation oldSO = so.copy();
    	Types.ACTIONS_VT actBestVT;

		if (randomSelect) {
			CurrentScore = rand.nextDouble();
			return CurrentScore;
		} 
		
		for (int j=1; j<=nply; j++) {
	        NewSO = oldSO.copy();
	        
	        if (getAFTERSTATE()) {
	        	NewSO.advanceDeterministic(act); 	// the afterstate
	        	agentScore = getScore(NewSO,NewSO); // this is V(s')
	            NewSO.advanceNondeterministic(); 
	        } else { 
	        	// the non-afterstate logic for the case of single moves:
            	//System.out.println("NewSO: "+NewSO.stringDescr()+", act: "+act.toInt()+", j(nply)="+j); // DEBUG
	            NewSO.advance(act);
	        	agentScore = getScore(NewSO,NewSO); // this is V(s'')
	        }
	        // both ways of calculating the agent score are the same for deterministic games (s'=s''),
	        // but they usually differ for nondeterministic games.
	        
	        //otilde = oldSO.getReward(oldSO,rgs);
	        //  -- the above line was a possible bug, it should be ...getReward(refer,...) always --
	        otilde = oldSO.getReward(refer,rgs);
	        rtilde = NewSO.getReward(refer,rgs)-otilde;
        	kappa = (NewSO.getPlayer()==refer.getPlayer()) ? +1 : -1;
	        
        	boolean TST_VERSION=false;
        	if (TST_VERSION) {
        		// this longer debug version is only to make some extra assertions for C4
        		// or other 'only-final-reward' games
    	        if (NewSO.isGameOver()) {
    	        	if (NewSO instanceof StateObserverC4) 
    	        		assert otilde==0 : "Oops, otilde is not zero!";  
    	        		// the state before the game-over state NewSO should have no reward
    	        	
    	        	return rtilde+kappa*getGamma()*agentScore;		// game over, terminate for-loop
    	        }
    	        if (j==nply) {
    	        	if (NewSO instanceof StateObserverC4) 
    		        	assert rtilde==0 : "Oops, rtilde is not zero!";  
    	        		// an in-game state should have no reward	        	

    	        	return rtilde+kappa*getGamma()*agentScore;		// normal return (n-ply recursion)
    	        }
        	} else {
    	        if (NewSO.isGameOver() || j==nply) {
    	        	if (TERNARY) {
    	        		return NewSO.isGameOver() ? rtilde : kappa*getGamma()*agentScore;
    	        	}
    	        	return rtilde+kappa*getGamma()*agentScore;		// game over or n-ply-recursion over, 
    	        													// --> terminate for-loop
    	        }
        	}
			
        	//
        	// we get here only in case nply>1 (and j<nply) :
        	//
        	
	        // find the best action for NewSO's player by
	        // maximizing the return from evalNPly
	        ArrayList<Types.ACTIONS> acts = NewSO.getAvailableActions();

	        assert acts.size()>0 : "Oops, no available action";
	        g3BestScore = -Double.MAX_VALUE;		// bug fix!
	        List<Types.ACTIONS> nextActions = new ArrayList<>();
	        for(int i = 0; i < acts.size(); ++i)
	        {
	            CurrentScore = evalNPly(NewSO,acts.get(i), rgs); 
	            
				//
				// Calculate g3BestScore and best action act.
				// If there are multiple best actions, select afterwards one of them randomly 
				// (better exploration)
				//
				if (g3BestScore < CurrentScore) {
					g3BestScore = CurrentScore;
	                nextActions.clear();
	                nextActions.add(acts.get(i));
				} else if (g3BestScore == CurrentScore) {
	                nextActions.add(acts.get(i));
				}
	        } // for (i)
	        act = nextActions.get(rand.nextInt(nextActions.size()));
	        
	        oldSO = NewSO.copy();
	        
		} // for (j)
		
		throw new RuntimeException("g3_Eval_NPly: we should not arrive here!");
		// the return should happen in last pass through for-j-loop ('if (j==nply)')

    } // g3_Eval_NPly

		
    // helper function for g3_Eval_NPly
    // (function EVAL in TR-TDNTuple.pdf, Algorithm 2)
    private double evalNPly(StateObservation s_v, Types.ACTIONS a_v, boolean rgs) {
    	double agentScore,kappa,reward;
    	StateObservation NewSO;
    	
    	NewSO = s_v.copy();
        
        if (getAFTERSTATE()) {
        	NewSO.advanceDeterministic(a_v); 	// the afterstate
        	agentScore = getScore(NewSO,NewSO); // this is V(s')
            NewSO.advanceNondeterministic(); 
        } else { 
        	// the non-afterstate logic for the case of single moves:
            NewSO.advance(a_v);
        	agentScore = getScore(NewSO,NewSO); // this is V(s'')
        }
        // both ways of calculating the agent score are the same for deterministic games (s'=s''),
        // but they usually differ for nondeterministic games.
        
    	kappa = (NewSO.getPlayer()==s_v.getPlayer()) ? +1 : -1;
    	reward = (NewSO.getReward(s_v,rgs)-s_v.getReward(s_v,rgs));
    	return reward + kappa*getGamma()*agentScore;
    }

    /**
     * This function returns normally (if VER_3P==true) just 1.
     * Only in the (deprecated) case VER_3P==NEW_2P==false, it returns +1 or -1, depending on refer's player being 0 or 1, resp.
     * In case VER_3P==false, NEW_2P==true, it returns -1.
     */
	private int calculatePlayerPM(StateObservation refer) {
        if (VER_3P) {
        	return 1;
        } else {
        	return (NEW_2P) ? -1 : Types.PLAYER_PM[refer.getPlayer()];
        }	
	}
    

		
	// DEBUG only: return always the 1st available action (for deterministic training games)
	public Types.ACTIONS_VT getFirstAction(StateObservation so, boolean random, double[] VTable, boolean silent) {
		boolean rgs = m_oPar.getRewardIsGameScore();
        Types.ACTIONS_VT actBest = null;
		StateObservation NewSO;
		
		// get the first action
        ArrayList<Types.ACTIONS> acts = so.getAvailableActions();
        actBest = (Types.ACTIONS_VT) acts.get(0);
        actBest.setRandomSelect(randomSelect);
        
		if (DBG2_TARGET) {
            NewSO = so.copy();
            NewSO.advance(actBest);
            double deltaReward = NewSO.getReward(so,rgs) - so.getReward(so,rgs);
			final double MAXSCORE = ((so instanceof StateObserver2048) ? 3932156 : 1);
			double sc = (deltaReward + getScore(NewSO))*MAXSCORE;
			System.out.println("getScore(NewSO): "+sc+"   ["+so.stringDescr()+"]");
			int dummy=1;
		}
		return actBest;
	}

	/**
	 * Return the agent's estimate of the score for that after state 
	 * (old version, {@link VER_3P}==false).
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
    	if (VER_3P) {
    		int[] bvec = m_Net.xnf.getBoardVector(so);
    		score = m_Net.getScoreI(bvec,refer.getPlayer());
    		//score = getScore(so,refer.getPlayer());	
    	} else {
    		score = getScore(so);			        		
    	}
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
		ScoreTuple sc = new ScoreTuple(so);
		int[] bvec = m_Net.xnf.getBoardVector(so);
		switch (so.getNumPlayers()) {
		case 1: 
			sc.scTup[0] = m_Net.getScoreI(bvec,so.getPlayer());
			break;
		case 2:
			int player = so.getPlayer();
			int opponent = (player==0) ? 1 : 0;
			sc.scTup[player] = m_Net.getScoreI(bvec,player);
			sc.scTup[opponent] = -sc.scTup[player];
			break;
		default: 
	    	if (VER_3P) {
	    		for (int i=0; i<so.getNumPlayers(); i++) 
	    			sc.scTup[i] = m_Net.getScoreI(bvec,i);
	    			// CAUTION: This might not work for all i, if n-tuples were not trained
	    			// for all i in this state 'so' (MODE_3P==1). 
	    			// It should work however in the cases MODE_3P==0 and MODE_3P==2.
	    	} else {
	    		throw new RuntimeException("Cannot create ScoreTuple if VER_3P==false");			        		
	    	}
	    	break;
		}
		
		// In any case: add the reward obtained so far, since the net predicts
		// with getScoreI only the expected future reward.
		boolean rgs = m_oPar.getRewardIsGameScore();
		for (int i=0; i<so.getNumPlayers(); i++) 
			sc.scTup[i] += so.getReward(i, rgs);
    	return sc;
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
//		sc = this.getScoreTuple(sob);		// NO recursion!!
		for (int i=0; i<sob.getNumPlayers(); i++) 
			sc.scTup[i] = sob.getReward(i, rgs);
			// this is valid, but it may be a bad estimate in games where the reward is only 
			// meaningful for game-over-states.
		return sc;
	}

	/**
	 * Train the agent for one complete game episode. <p>
	 * Side effects: Increment m_GameNum by +1. Change the agent's internal  
	 * parameters (weights and so on).
	 * @param so		the state from which the episode is played (usually the
	 * 					return value of {@link GameBoard#chooseStartState(PlayAgent)} to get
	 * 					some exploration of different game paths)
	 * @return			true, if agent raised a stop condition (only CMAPlayer)	 
	 */
// --- epiLength, learnFromRM are now available via the agent's member ParOther m_oPar: ---
//	 * @param epiLength	maximum number of moves in an episode. If reached, stop training 
//	 * 					prematurely.  
//	 * @param learnFromRM if true, learn from random moves during training
	public boolean trainAgent(StateObservation so) {
		double[] VTable = null;
		double reward = 0.0, oldReward = 0.0;
		double reward2=0.0;
		ScoreTuple rewardTuple, oldRewardTuple;
		boolean wghtChange = false;
		boolean upTC=false;
		double Input[], oldInput[];
		double Z = 0,ZZ=0,Z_nply;
		String S_old = null;   // only as debug info
		Types.ACTIONS_VT actBest;
		StateObservation oldSO;
		StateObservation afterSO;
		int[] curBoard;
		int   curPlayer=so.getPlayer();
		int[] nextBoard = null;
		int   nextPlayer;
		NextState ns = null;

		boolean learnFromRM = m_oPar.useLearnFromRM();
		int epiLength = m_oPar.getEpisodeLength();
		if (epiLength==-1) epiLength = Integer.MAX_VALUE;
		
		UpdateType UPDATE = getUpdateType(so);
		
		rewardTuple = new ScoreTuple(so);
		oldRewardTuple = new ScoreTuple(so);


		afterSO = so.getPrecedingAfterstate();
    	curBoard = (afterSO==null) ? null : m_Net.xnf.getBoardVector(afterSO);

		//System.out.println("Random test: "+ rand.nextDouble());
		//System.out.println("Random test: "+ rand.nextDouble());
		
		m_Net.clearEligList();
		m_Net.setHorizon();
		if (DBG_OLD_3P || DBG_NEW_3P) {
			m_Net3.clearEligList();
			m_Net3.setHorizon();
		}
		
		m_counter=0;		// count the number of moves
		m_finished=false;
		while (true) {
	        if (TDNTuple2Agt.DBG2_FIXEDSEQUENCE) {
				VTable = new double[so.getNumAvailableActions()+1];
	        	actBest = this.getFirstAction(so, true, VTable, true);	// DEBUG only
	        } else {
				actBest = this.getNextAction2(so, true, true);	// the normal case 
	        }

	        m_randomMove = actBest.isRandomAction();
	        
	        m_numTrnMoves++;		// number of train moves (including random moves)
	               
	        ns = new NextState(so,actBest);	        
	        nextPlayer = ns.getNextSO().getPlayer();
	        
	        if (DBG_CUBE) {
	        	if (so instanceof StateObserverCube) {
	        		if (((StateObserverCube)so).getMinEpisodeLength()==3) {
	        			int dummy=1;
	        		}
	        	}
	        }
	        
	        if (DBG_NEW_3P && !m_randomMove) {
	        	nextBoard = m_Net.xnf.getBoardVector(ns.getAfterState());
	        	Z = - getGamma() * m_Net.getScoreI(nextBoard,nextPlayer);
	        	ZZ = - getGamma() * getScore(ns.getAfterState(),ns.getAfterState());
	        }
	        
	        if (DBG_REWARD) {
	        	System.out.println("nextSO: ["+ns.getNextSO().stringDescr()+"] for player "+nextPlayer);
	        }

	        if (VER_3P) 
	        {
	        	switch (UPDATE) {
	        	case MULTI_UPDATE:
	    			// do one training step (NEW target) for all players' value functions (VER_3P)
		        	int refPlayer=ns.getSO().getPlayer();
		        	rewardTuple=trainMultiUpdate_3P(ns,curBoard,learnFromRM,epiLength,oldRewardTuple);
		        	reward=rewardTuple.scTup[refPlayer];
//		        	if (DBG_OLD_3P) {
//		        		double reward3=trainNewTargetLogic2(ns,curBoard,learnFromRM,epiLength,oldReward,m_Net3);
//		        	}
//		        	if (DBG_REWARD) {
//						reward2=trainNewTargetLogic2(ns,curBoard,learnFromRM,epiLength,oldReward);	 
//						System.out.println("reward,reward2: "+reward+","+reward2);
//		        	}
		        	oldReward=oldRewardTuple.scTup[refPlayer];	        		
	        		break;	// out of switch
	        	case SINGLE_UPDATE:
	    			// do one training step (NEW target) only for current player
	        		Z_nply = actBest.getVBest();
	        		reward=trainSingleUpdate_3P(ns,Z_nply,curBoard,learnFromRM,epiLength,oldReward,m_Net);
	        		if (DBG_NEW_3P) dbg_Z_NEW_3P(ns,curBoard,learnFromRM,epiLength,reward,oldReward,Z,ZZ,Z_nply);
	        		break;	// out of switch
	        	} // switch
	        } 
	        else // i.e. VER_3P==false:
	        { 
				reward=trainNewTargetLogic2(ns,curBoard,learnFromRM,epiLength,oldReward,m_Net);
	        }
	        
			if (DBG2_TARGET) {
				final double MAXSCORE = ((so instanceof StateObserver2048) ? 3932156 : 1);
				System.out.print("R_t+1, r_t+1: "+reward*MAXSCORE
						+", "+(int)((reward-oldReward)*MAXSCORE+0.5) );
				System.out.println("|   est. final score: "+(reward+this.getScore(ns.getAfterState()))*MAXSCORE);
				if (reward==-1 && oldReward==-1) {
					int dummy=1;
				}
			}

			/* prepare for next pass through while-loop or for terminal update */ 
			so = ns.getNextSO();	// Only together with trainNewTargetLogic2	or trainNewTargetNEW_3P		
			curBoard = m_Net.xnf.getBoardVector(ns.getAfterState());  
			curPlayer= nextPlayer;
			if (VER_3P && UPDATE==UpdateType.MULTI_UPDATE) {
				oldRewardTuple.scTup = rewardTuple.scTup.clone();
			} else {
				oldReward= reward;				
			}
			
			if (m_finished) {
				if (m_DEBG)
					if (m_randomMove) {
						pstream.println("Terminated game "+(getGameNum()) + " by random move. Reward = "+reward);						
					} else {
						pstream.println("Terminated game "+(getGameNum()) + ". Reward = "+reward);
					}
				
				break;		// out of while
			}

		} // while

		// learn for each final state that its value function (estimated further reward)
		// should be zero:
		if (VER_3P)  {
        	switch (UPDATE) {
        	case MULTI_UPDATE:
    			// do one training step (NEW target) for all players' value functions (VER_3P)
    			for (int i=0; i<so.getNumPlayers(); i++) {
    					m_Net.updateWeightsNewTerminal(curBoard, i,so, true);
    			}			
            	if (DBG_OLD_3P || DBG_NEW_3P) {
            		m_Net3.updateWeightsNewTerminal(curBoard, curPlayer, so, false);
            	}
            	break;
        	case SINGLE_UPDATE:
        		if (!TERNARY) {
        			// do one training step (NEW target) only for current player
        			m_Net.updateWeightsNewTerminal(curBoard, curPlayer, so, false);
        		}
        		break;
        	} // switch
        } 
        else // i.e. VER_3P==false:
        { 
			// do one training step (NEW target) for current player 
			m_Net.updateWeightsNewTerminal(curBoard, curPlayer, so, false);
		}
		
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
		
		// is now irrelevant since tcImm is always true
		if(getGameNum()%tcIn==0 && TC && !tcImm) {
			System.out.println("we should not get here!");
			m_Net.updateTC();
			if (DBG_OLD_3P || DBG_NEW_3P) m_Net3.updateTC();
		}
					
		//if (DEBG) m_Net.printLutSum(pstream);
		if (m_DEBG) m_Net.printLutHashSum(pstream);
		if (PRINTTABLES) {
			if(getGameNum()%10==0 && TC)
				m_Net.printTables();
		}
		
		return false;
		
	} // trainAgent

	/**
	 * This is for the 3P target logic, i.e. {@link #VER_3P}==true and {@code MODE_3P}==0
	 * or [{@code MODE_3P}==2 AND N!=2 ] (since it is sub-optimal for 2-player games).
	 * <p>
	 * Each player has its own value function and the target for player p_i is <br>
	 * 		r(s_t+1, p_i) + \gamma*V(s_t+1,p_i) <br>
	 * which is the target for the actual value function V(s_t,p_i) and we perform a 
	 * multiple update (MULTI_UPDATE) of these value functions.
	 * 
	 * @return rewardTuple the tuple of rewards r(s_t+1, p_i) of {@code ns} for each player p_i
	 */
	private ScoreTuple trainMultiUpdate_3P(NextState ns,
			int[] curBoard, 
			boolean learnFromRM, int epiLength,  
			ScoreTuple oldRewardTuple) 
	{
		StateObservation thisSO=ns.getSO();
		StateObservation nextSO=ns.getNextSO();
		int[] nextBoard = m_Net.xnf.getBoardVector(ns.getAfterState());
		int thisPlayer= thisSO.getPlayer();
		int nextPlayer= nextSO.getPlayer();
		ScoreTuple rewardTuple;
		
		assert (VER_3P==true);
		
		rewardTuple = ns.getNextRewardTupleCheckFinished(epiLength); // updates m_counter, m_finished
//		rewardTuple = ns.getNextRewardTuple();
//		if (nextSO.isGameOver()) {
//			m_finished = true;
//		}
//		m_counter++;
//		if (m_counter==epiLength) {
//			rewardTuple=estimateGameValueTuple(nextSO);
//			m_finished = true; 
//		}
		
		if (m_randomMove && !learnFromRM) {
			// no training, go to next move.
			if (m_DEBG)  pstream.println("random move");
			
			m_Net.clearEligList(m_elig);	// the list is only cleared if m_elig==RESET
				
		} else {
			// do one training step (NEW target) for all players' value functions (VER_3P)
			for (int i=0; i<thisSO.getNumPlayers(); i++) {
				if (curBoard!=null) {
					double rw = rewardTuple.scTup[i]-oldRewardTuple.scTup[i];
					// target is (reward + GAMMA * value of the after-state) for non-final states
					double target = rw + getGamma() * m_Net.getScoreI(nextBoard,i);
					m_Net.updateWeightsNew(curBoard, i /*thisPlayer*/, nextBoard, i /*nextPlayer*/,
							rw,target,thisSO);
				}
			}
		}
		
		return rewardTuple;
		
	} // trainMultiUpdate_3P
	
	/**
	 * This is for the 3P target logic, i.e. {@link #VER_3P}==true and {@code MODE_3P}==1,
	 * that is the N-ply logic for all N-player games in its new form. 
	 * <p>
	 * Do only an update for the value function of {@code ns.getSO()}'s player 
	 * (i.e. SINGLE_UPDATE, only a single value function is updated)
	 * 
	 * @param ns
	 * @param  target 
	 * 				the target for the weight update, which is here the Z-score returned
	 * 				from {@code g3_Eval_NPly()}
	 * @param curBoard
	 * @param learnFromRM
	 * @param epiLength
	 * @param oldReward
	 * 
	 * @return reward, which is {@code ns.getNextReward()}
	 */
	private double trainSingleUpdate_3P(NextState ns, double target,
			int[] curBoard, 
			boolean learnFromRM, int epiLength,  
			double oldReward, NTuple2ValueFunc my_Net) 
	{
		double reward;
		
		StateObservation thisSO=ns.getSO();
		StateObservation nextSO=ns.getNextSO();
		int[] nextBoard = m_Net.xnf.getBoardVector(ns.getAfterState());
		int thisPlayer= thisSO.getPlayer();
		int nextPlayer= nextSO.getPlayer();
		
		assert (VER_3P==true);

		reward = ns.getNextRewardCheckFinished(epiLength);	// updates m_counter, m_finished
//		reward = ns.getNextReward();
//
//		if (nextSO.isGameOver()) {
//			m_finished = true;
//		}
//
//		m_counter++;
//		if (m_counter==epiLength) {
//			reward=estimateGameValue(nextSO);
//			m_finished = true; 
//		}
		
		if (m_randomMove && !learnFromRM) {
			// no training, go to next move.
			if (m_DEBG)  pstream.println("random move");
			
			my_Net.clearEligList(m_elig);	// the list is only cleared if m_elig==RESET
				
		} else {
			// do one training step (NEW target)
			// target is passed as argument from g3_Eval_NPly()  (n-ply look-ahead)
			if (curBoard!=null) {
				my_Net.updateWeightsNew(curBoard, thisPlayer, nextBoard, nextPlayer,
						reward-oldReward,target,thisSO);
			}
		}
		
		return reward;
		
	} // trainSingleUpdate_3P
	
	/**
	 * This is the new target logic for {@link VER_3P}=false (i.e. only for 1- and 2-player games)
	 * 
	 * @return reward
	 */
	private double trainNewTargetLogic2(NextState ns,
			int[] curBoard, 
			boolean learnFromRM, int epiLength,  
			double oldReward, NTuple2ValueFunc my_Net) 
	{
		double reward;
		
		StateObservation thisSO=ns.getSO();
		StateObservation nextSO=ns.getNextSO();
		int[] nextBoard = m_Net.xnf.getBoardVector(ns.getAfterState());
		int thisPlayer= thisSO.getPlayer();
		int nextPlayer= nextSO.getPlayer();
		
		reward = ns.getNextRewardCheckFinished(epiLength);	// updates m_counter, m_finished
//		reward = ns.getNextReward();
//		
//		if (nextSO.isGameOver()) {
//			m_finished = true;
//		}
//
//		m_counter++;
//		if (m_counter==epiLength) {
//			reward=estimateGameValue(nextSO);
//			m_finished = true; 
//		}
		
		if (m_randomMove && !learnFromRM) {
			// no training, go to next move.
			if (m_DEBG)  pstream.println("random move");
			
			my_Net.clearEligList(m_elig);	// the list is only cleared if m_elig==RESET
				
		} else {
			// do one training step (NEW target)
			if (curBoard!=null) {
				double target;
				int sign=1;
				if (NEW_2P==true) sign=-1;
				// target is (reward + GAMMA * value of the after-state) for non-final states
				target = reward-oldReward + sign*getGamma() * my_Net.getScoreI(nextBoard,nextPlayer);
				my_Net.updateWeightsNew(curBoard, thisPlayer, nextBoard, nextPlayer,
						reward-oldReward,target,thisSO);
				if (DBG_NEW_3P) ZScore_NTL2=target;
			}
		}
		
		return reward;
		
	} 
	
	/**
	 * Fetch the reward for StateObservation so (relative to refer). <p>
	 * 
	 * Now deprecated: This function was needed in the old version (VER_3P=NEW_2P=false, prior
	 * to 10/2017), but it is neither very logical ('player*reward') nor is it extensible 
	 * to more than 2 players.
	 * 
	 * @param so		actual state
	 * @param refer		referring state
	 * @param playerPM	the player to move in state refer. 
	 * 					1-player games: always +1; 2-player games: +1 or -1
	 * @return playerPM*reward for actual state {@code so} 
	 */
	@Deprecated
	private double fetchReward(StateObservation so, StateObservation refer, int playerPM) 
	{
		boolean rgs = m_oPar.getRewardIsGameScore();
		double reward = playerPM*so.getReward(refer,rgs);
		return normalize2(reward,so);
	}
	
	/**
	 * Fetch the reward for state {@code so} from the perspective of player in state 
	 * {@code refer}. This function is needed for {@link #VER_3P}=false, {@link #NEW_2P}=true.<p>
	 * 
	 * @param so		actual state
	 * @param refer		referring state
	 * @return reward for actual state {@code so} 
	 */
	private double fetchReward2P(StateObservation so, StateObservation refer) 
	{
		boolean rgs = m_oPar.getRewardIsGameScore();
		double reward = so.getReward(refer,rgs);
		return normalize2(reward,so);
	}
	
	/**
	 * 
	 * @param score
	 * @param so	needed for accessing getMinGameScore(), getMaxGameScore()
	 * @return normalized score to [-1,+1] (the appropriate range for tanh-sigmoid) if 
	 * 		switch {@link #m_tdPar}{@code .getNormalize()} is set.
	 */
	private double normalize2(double score, StateObservation so) {
		if (m_tdPar.getNormalize()) {
			// since we have - in contrast to TDAgent - here only one sigmoid
			// choice, namely tanh, we can take fixed [min,max] = [-1,+1]. 
			// If we would later extend to several sigmoids, we would have to 
			// adapt here:		
			score = normalize(score,so.getMinGameScore(),
							   		so.getMaxGameScore(),-1.0,+1.0);
		}
		return score;
	}
	
	/**
	 * Adjust {@code ALPHA} and adjust {@code m_epsilon}.
	 */
	public void finishUpdateWeights() {

		m_Net.finishUpdateWeights(); // adjust learn param ALPHA
		if (DBG_OLD_3P || DBG_NEW_3P) 
			m_Net3.finishUpdateWeights();

		// linear decrease of m_epsilon (re-activated 08/2017)
		m_epsilon = m_epsilon - m_EpsilonChangeDelta;

		if (PRINTTABLES) {
			try {
				print(m_epsilon);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private void print(double m_epsilon2) throws IOException {
		PrintWriter epsilon = new PrintWriter(new FileWriter("epsilon",true));
		epsilon.println("" +m_epsilon2);
		epsilon.close();
	}

	public void setTDParams(ParTD tdPar, int maxGameNum) {
		double alpha = tdPar.getAlpha();
		double alphaFinal = tdPar.getAlphaFinal();
		double alphaChangeRatio = Math
				.pow(alphaFinal / alpha, 1.0 / maxGameNum);
		m_Net.setAlpha(tdPar.getAlpha());
		m_Net.setAlphaChangeRatio(alphaChangeRatio);

//		NORMALIZE=tdPar.getNormalize();
		m_epsilon = tdPar.getEpsilon();
		m_EpsilonChangeDelta = (m_epsilon - tdPar.getEpsilonFinal()) / maxGameNum;
	}

	public void setNTParams(ParNT ntPar) {
		tcIn=ntPar.getTcInterval();
		TC=ntPar.getTc();
		tcImm=ntPar.getTcImm();		
//		randomness=ntPar.getRandomness();
//		randWalk=ntPar.getRandomWalk();
		m_Net.setTdAgt(this);						 // WK: needed when loading an older agent
	}

	public void setAlpha(double alpha) {
		m_Net.setAlpha(alpha);
	}

	public double getAlpha() {
		// only for debug & testing
		// super.counStates(1);
		int verbose1 = 1; // 0: skip analyze_hmX, 1: one-line output, 2:
							// multi-line output in analyse_hmX
		//super.analyze_hmC(getGameNum(), verbose1);
		int verbose2 = 1; // 0: skip analyze_hmX, 1: one-line output, 2:
							// multi-line output in analyse_hmX
		//super.analyze_hmX(verbose2);

		return m_Net.getAlpha();
	}

	public double getEpsilon() {
		return m_epsilon;
	}
	
	public double getGamma() {
		return m_tdPar.getGamma();
	}
	
	/**
	 * the number of calls to {@link NTuple2ValueFunc#update(int[], int, double, double)}
	 */
	@Override
	public long getNumLrnActions() {
		return m_Net.getNumLearnActions();
	}

	public void resetNumLearnActions() {
		m_Net.resetNumLearnActions();
	}
	
	@Override
	public int getMoveCounter() {
		return m_counter;
	}

	public NTuple2ValueFunc getNTupleValueFunc() {
		return m_Net;
	}
	

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
						+ ", gamma: " + m_tdPar.getGamma() +", MODE_3P: "+ m_tdPar.getMode3P();
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
		str = str + ", (winX/tie/winO)=("+winXCounter+"/"+tieCounter+"/"+winOCounter+")";
		winXCounter=tieCounter=winOCounter=0;
		return str;
	}

	@Override
	public boolean isTrainable() { return true; }

	public ParTD getParTD() {
		return m_tdPar;
	}
	public ParNT getParNT() {
		return m_ntPar;
	}
	// getParOther() is in AgentBase
	
	public boolean getAFTERSTATE() {
		return m_ntPar.getAFTERSTATE();
	}
	public boolean getLearnFromRM() {
		return m_oPar.useLearnFromRM();
	}

	/**
	 * Return the weight update mode: <ul>
	 * <li> If {@link #MODE_3P}==0, do weight update for each player's V (MULTI)
	 * <li> If {@link #MODE_3P}==1, do weight update only for p_t in V(s_t) (SINGLE, nply=N)
	 * <li> If {@link #MODE_3P}==2: <ul>
	 * 		<li> If N==2, do weight update only for p_t in V(s_t) (SINGLE, nply=1)
	 * 		<li> else do weight update for each player's V (MULTI)
	 * 		</ul>
	 * </ul>
	 * See {@link #VER_3P} for more details.
	 * @param so	to get N=={@code so.getNumPlayers()}
	 * @return the update type
	 */
	private UpdateType getUpdateType(StateObservation so) {
		if (MODE_3P==1 || (MODE_3P==2 && so.getNumPlayers()==2))
			return UpdateType.SINGLE_UPDATE;	
		return UpdateType.MULTI_UPDATE;
	}
	

	// Debug only: 
	//
	private void printTable(PrintStream pstream, int[] board) {
		String s = NTuple2.stringRep(board);
		pstream.println(s + " : BestScore= "+BestScore);
	}

	private void printVTable(PrintStream pstream, double[] VTable) {
		for (int i=0; i<VTable.length; i++) {
			pstream.print(VTable[i]+", ");
		}
		System.out.println("");
	}
	
	private void sanityCheck1P(StateObservation so, ArrayList<ACTIONS> acts, int i,
			StateObservation refer, boolean silent, double CurrentScore)
	{
		// This is just a sanity check for  the special case of 1-player games (N_P=nply=1), 
		// e.g. 2048: Do g3_Evaluate and g3_Eval_NPly return the same value ?
		//  
		// (Does this work, if the nondeterministic part of advance may return something 
		// different? - Yes, it does, since we do only 1 ply and the nondeterministic part 
		// does not influence the value of CurrentScore, neither reward nor value 
		// function (at least in the game 2048). ) 
	    double CurrentScoreOLD = g3_Evaluate(so,acts.get(i),refer,silent);
	    double delta = CurrentScoreOLD-CurrentScore;
	    if (Math.abs(delta)>0.0) {
	        System.out.println("CurrentScore, CurrentScoreOLD, delta="+CurrentScore+",  "+CurrentScoreOLD+",  "+delta);
	        int dummy=1;
	        // We should never get here, if everything is OK. But if we get here, the following two 
	        // lines allow debugging the differences in both calls for the specific setting:
	        CurrentScore = g3_Eval_NPly(so,acts.get(i),refer,so.getNumPlayers(),silent);   
	        CurrentScoreOLD = g3_Evaluate(so,acts.get(i),refer,silent);
	    }
	}
	
	private void dbg_Z_NEW_3P(NextState ns, int[] curBoard,boolean learnFromRM, int epiLength, 
							  double reward, double oldReward,double Z, double ZZ, double Z_nply) 
	{
		double reward2=0.0;
		reward2=trainNewTargetLogic2(ns,curBoard,learnFromRM,epiLength,oldReward,m_Net3);	 
		//System.out.println("reward,reward2: "+reward+",  "+reward2);
		if (!m_randomMove) {
			//System.out.println("Z_nply, ZScore_3: "+Z_nply+",  "+ZScore_NTL2);
			double rw = reward-oldReward;
			Z+=rw; ZZ+=rw;
			//System.out.println("Z_nply, Z, ZZ: "+Z_nply+",  "+Z+",   "+ZZ);
			if (Math.abs(Z_nply-Z)>1e-6 || Math.abs(Z_nply-Z)>1e-6) {
				int dummy=1;								
			}
		}
		if (reward!=reward2) {
			int dummy=1;
		}
	}
	
	/**
	 * Analyze the weight distribution and - if TCL is active - the tcFactorArray distribution 
	 * by calculating certain quantiles (percentiles)
	 * @param per the quantiles to calculate. If null, then use DEFAULT_PER 
	 * 			= {0,25,50,75,100}, where the numbers are given in percent.
	 * @return res[][] with <ul>
	 * 		<li> res[0][]: a copy of {@code per}
	 * 		<li> res[1][]: the corresponding quantiles of the weights (from all LUTs)
	 * 		<li> res[2][]: the corresponding quantiles of tcFactorArray (from all LUTs)
	 * </ul>
	 */
	public double[][] weightAnalysis(double[] per) {
		double[] DEFAULT_PER = {0,25,50,75,100};
		if (per==null) per = DEFAULT_PER;
		double[][] res = new double[3][per.length];  	// res[0]: per, res[1]: quantiles of data
														// res[2]: quantiles of tcdat;
		System.arraycopy(per, 0, res[0], 0, per.length);
		
		int count = 0;
		NTuple2[] ntuples = m_Net.getNTuples();
		for (int i=0; i<ntuples.length; i++) {
			count += ntuples[i].getLutLength();
		}
		double[] data = new double[count];
		double[] tcdat = new double[count];
		double[] lut;
		double[] tcf=null;
		int i, pos=0;
		
		// --- this is fast, but has the disadvantage that many zeros are included --> 
		// --- 25%- 50%- and 75%-quantiles tend to be exactly zero
		// --- (the zeros come from the fact that many weights are never visited in training)
//		for (i=0,len=0; i<ntuples.length; i++) {
//			len = ntuples[i].getLutLength();
//			lut = ntuples[i].getWeights();
//			assert lut.length==len : "length assertion error";
//			System.arraycopy(lut, 0, data, pos, len);
//			pos += len; 
//		}

		// --- this is slower, but quantiles for active weights are more meaningful
		// --- and not always 0.0 (!)
		for (i=0; i<ntuples.length; i++) {
			lut = ntuples[i].getWeights();
			tcf = ntuples[i].getTcFactorArray();
			for (int j=0; j<lut.length; j++) {
				if (lut[j]!=0) {
					 if (tcf!=null) tcdat[pos] = tcf[j];
					data[pos++] = lut[j];
				}
			}
		}
		int nActive=pos;
		int pActive = (int) (((double)nActive)/count*100);
		double[] data2 = new double[nActive];
		double[] tcdat2 = new double[nActive];
		System.arraycopy(data, 0, data2, 0, nActive);
		System.arraycopy(tcdat, 0, tcdat2, 0, nActive);
		data = data2;
		tcdat = tcdat2;
		if (tcf==null) System.out.println("WARNING: tcFactorArray is null");

		// --- only testing / debug ---
//		double[] data = {0,1,2,3,4,5,6,7,8,9};
//		int length2 = 1000;
//		double[] data2 = new double[length2];
//		System.arraycopy(data, 0, data2, 0, length2);
//		data = data2;
		
		Percentile p = new Percentile(); // from commons-math3-3.6.1.jar, see https://commons.apache.org/proper/commons-math/javadocs/api-3.0/org/apache/commons/math3/stat/descriptive/rank/Percentile.html
		Min m = new Min(); 
		Percentile p2 = new Percentile();
		Min m2 = new Min(); 
		
		p.setData(data);	
		p2.setData(tcdat);
		for (i=0; i<per.length; i++) {
			if (per[i]==0) {
				res[1][i] = m.evaluate(data);
				res[2][i] = m2.evaluate(tcdat);
			} else { 
				res[1][i] = p.evaluate(per[i]);
				res[2][i] = p2.evaluate(per[i]);
			}
		}

		DecimalFormat form = new DecimalFormat("000");
		DecimalFormat df = new DecimalFormat();				
		df = (DecimalFormat) NumberFormat.getNumberInstance(Locale.UK);		
		df.applyPattern("+0.0000000;-0.0000000");  
		System.out.println("weight analysis TDNTuple2Agt ("
				+count+" weights, "+nActive+" active ("+pActive+"%)): ");
		for (i=0; i<per.length; i++) {
			System.out.println("   Quantile [" + form.format(per[i]) + "] = "	
					+df.format(res[1][i]) + " / " + ((tcf==null)?"NA":df.format(res[2][i])) );			
		}
		
		return res;
	}

	
	/**
	 * Class NextState bundles the different states in state advancing and two different modes of 
	 * state advancing.
	 * <p>
	 * If {@link TDNTuple2Agt#getAFTERSTATE()}==false, then {@code ns=new NextState(so,actBest)}  
	 * simply advances {@code so} and lets ns.getAfterState() and ns.getNextSO() return the same next 
	 * state so.advance(actBest). 
	 * <p>
	 * If {@link TDNTuple2Agt#getAFTERSTATE()}==true, then {@code ns=new NextState(so,actBest)}  
	 * advances {@code so} in two steps: ns.getAfterState() returns the <b>afterstate s'</b> (after  
	 * the deterministic advance (e.g. the merge in case of 2048)) and ns.getNextSO() returns  
	 * the next state <b>s''</b> (after adding the nondeterministic part (e.g. adding the random 
	 * tile in case of 2048)).
	 * <p>
	 * The nondeterministic part is done once at the time of constructing an object of class
	 * {@code NextState}, so multiple calls to {@code ns.getNextSO()} are guaranteed to return
	 * the  same state.
	 * <p> 
	 * For deterministic games, the behavior is identical to the case with 
	 * {@code getAFTERSTATE()==false}: ns.getAfterState() and ns.getNextSO() return the same  
	 * next state so.advance(actBest).
	 * 
	 * @see TDNTuple2Agt#getAFTERSTATE()
	 * @see TDNTuple2Agt#trainAgent(StateObservation)
	 */
	class NextState {
			StateObservation refer;
			StateObservation afterState;
			StateObservation nextSO;
			/**
			 * the reward of next state <b>s''</b> from the perspective of state <b>s</b> = {@code so} 
			 */
			double nextReward;
			
			/**
			 * a ScoreTuple holding  the rewards of 
			 * {@code nextSO} from the perspective of player 0,1,...N-1
			 */
			private ScoreTuple nextRewardTuple = null;
			
			/**
			 * Advance state <b>s</b> = {@code so} by action {@code actBest}. Store the afterstate 
			 * <b>s'</b> and the next state <b>s''</b>. 
			 */
			NextState(StateObservation so, Types.ACTIONS actBest) {
				refer = so.copy();
				int nPlayers= refer.getNumPlayers();
		        if (getAFTERSTATE()) {   // if checkbox "AFTERSTATE" is checked
		        	
	            	// implement it in such a way that StateObservation so is *not* changed -> that is why 
	            	// we copy *first* to afterState, then advance:
	                afterState = so.copy();
	                afterState.advanceDeterministic(actBest);
	                nextSO = afterState.copy();
	                nextSO.advanceNondeterministic(); 
		        	
		        } else {
	                nextSO = so.copy();
	                nextSO.advance(actBest);
					afterState = nextSO.copy();
		        }
		        
				UpdateType UPDATE = getUpdateType(so);

				if (VER_3P) {
		        	switch (UPDATE) {
		        	case MULTI_UPDATE:
						boolean rgs = m_oPar.getRewardIsGameScore();
						nextRewardTuple = new ScoreTuple(so);
						for (int i=0; i<nPlayers; i++) {
							nextRewardTuple.scTup[i] = normalize2(nextSO.getReward(i,rgs),nextSO);
						}
						break;
		        	case SINGLE_UPDATE:
						nextReward = fetchReward2P(nextSO,refer);
			        	// this is r(s_{t+1}|p_t). It is equivalent to (-1)*r(s_{t+1}|p_{t+1}) for 
						// 2-player games.
						break;
					}
				} else {	// i.e. VER_3P==false
			        if (NEW_2P) {
						nextReward = fetchReward2P(nextSO,refer);
			        	// this is r(s_{t+1}|p_t). It is equivalent to (-1)*r(s_{t+1}|p_{t+1}).
			        } else {
						nextReward = fetchReward(nextSO,refer,Types.PLAYER_PM[refer.getPlayer()]);
			        }
				}

				if (DBG_REWARD && nextSO.isGameOver()) {
					if (VER_3P && MODE_3P==0) {
						System.out.print("Rewards: ");
						System.out.print(nextRewardTuple.toString());
					} else {
						System.out.print("Reward: "+nextReward);
					}
					System.out.println("   ["+nextSO.stringDescr()+"]  " + nextSO.getGameScore() + " for player " + nextSO.getPlayer());
				}
			}

			/**
			 * @return the original state <b>s</b> = {@code so}
			 */
			public StateObservation getSO() {
				return refer;
			}

			/**
			 * @return the afterstate <b>s'</b>
			 */
			public StateObservation getAfterState() {
				return afterState;
			}

			/**
			 * @return the next state <b>s''</b> (with random part from environment added).
			 */
			public StateObservation getNextSO() {
				return nextSO;
			}

			/**
			 * @return the reward of next state <b>s''</b> from the perspective of state <b>s</b> = {@code so}
			 */
			public double getNextReward() {
				return nextReward;
			}

			public double getNextRewardCheckFinished(int epiLength) {
				double reward = this.getNextReward();  // r(s_{t+1}|p_t)
				
				if (nextSO.isGameOver()) {
					m_finished = true;
					
					// only info
					if (reward==0.0) tieCounter++;
					if (reward==1.0 & nextSO.getPlayer()==0) winOCounter++;
					if (reward==1.0 & nextSO.getPlayer()==1) winXCounter++;
				}

				m_counter++;
				if (m_counter==epiLength) {
					reward=estimateGameValue(nextSO);
					m_finished = true; 
				}
				
				return reward;
			}

			/**
			 * @return the tuple of rewards of {@code nextSO} from the perspective of player 0,1,...N-1
			 */
			public ScoreTuple getNextRewardTuple() {
				return nextRewardTuple;
			}

			public ScoreTuple getNextRewardTupleCheckFinished(int epiLength) {
				ScoreTuple rewardTuple = this.getNextRewardTuple();
				
				if (nextSO.isGameOver()) {
					m_finished = true;
				}

				m_counter++;
				if (m_counter==epiLength) {
					rewardTuple=estimateGameValueTuple(nextSO);
					m_finished = true; 
				}
				
				return rewardTuple;
			}


	} // class NextState

//	   public static void main(String[] args) {
//
//		      int arr1[] = { 0, 1, 2, 3, 4, 5 };
//		      int arr2[] = { 5, 10, 20, 30, 40, 50 };
//		    
//		      // copies an array from the specified source array
//		      System.arraycopy(arr1, 0, arr2, 0, 4);
//		      System.out.print("array2 = ");
//		      System.out.print(arr2[0] + " ");
//		      System.out.print(arr2[1] + " ");
//		      System.out.print(arr2[2] + " ");
//		      System.out.print(arr2[3] + " ");
//		      System.out.print(arr2[4] + " ");
//		   }
}