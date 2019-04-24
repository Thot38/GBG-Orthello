package controllers.MC;

import controllers.AgentBase;
import controllers.ExpectimaxWrapper;
import controllers.MaxNWrapper;
import controllers.PlayAgent;
import games.StateObservation;
import games.XArenaMenu;
import games.ZweiTausendAchtundVierzig.ConfigEvaluator;
import games.ZweiTausendAchtundVierzig.StateObserver2048;
import params.MCParams;
import params.OtherParams;
import params.ParMC;
import params.ParOther;
import tools.Types;
import tools.Types.ScoreTuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Monte Carlo (MC) agent for N-player games.
 * <p>
 * Similar to {@link MCAgent}, but it operates on {@link ScoreTuple} and is thus general for 
 * N-player games with arbitrary N. It might be a bit slower than {@link MCAgent}.
 * <p>
 * (Note: {@link MCAgent} can operate for N-player games, but it cannot return a {@link ScoreTuple}
 * for N>2. Returning a {@link ScoreTuple} is however needed for wrapping {@link MCAgent} in 
 * {@link MaxNWrapper} or {@link ExpectimaxWrapper}.)
 * 
 * @see MCAgent
 * @see MCAgentConfig
 * @see RandomSearch
 */
public class MCAgentN extends AgentBase implements PlayAgent {
	ScoreTuple sc;
    private Random random = new Random();
    private ExecutorService executorService = Executors.newWorkStealingPool();

    private int totalRolloutDepth = 0;  // saves the average rollout depth for the mc Agent
    private int nRolloutFinished = 0; 	// counts the number of rollouts ending with isGameOver==true
    private int nIterations = 0; 		// counts the total number of iterations

    public ParMC m_mcPar = new ParMC();

	/**
	 * change the version ID for serialization only if a newer version is no longer 
	 * compatible with an older one (older .agt.zip will become unreadable or you have
	 * to provide a special version transformation)
	 */
	private static final long  serialVersionUID = 12L;

    
    public MCAgentN(ParMC mcParams){
        this("MC", mcParams, new ParOther());    	
    }
    
    public MCAgentN(String name, ParMC mcParams, ParOther oPar)
    {
        super(name);
        this.m_mcPar = new ParMC(mcParams);
		this.m_oPar = new ParOther(oPar);
        setAgentState(AgentState.TRAINED);
    }

	/**
	 * Get the best next action and return it 
	 * (NEW version: returns ACTIONS_VT)
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
	 * <p>
	 * {@code actBest.getScoreTuple()} returns the {@link ScoreTuple} connected with this
     * selected action.
	 */
	@Override
	public Types.ACTIONS_VT getNextAction2(StateObservation so, boolean random, boolean silent) {
        int iterations = m_mcPar.getNumIter();
        int numberAgents = m_mcPar.getNumAgents();
        int depth = m_mcPar.getRolloutDepth();
        
        if(numberAgents > 1) {
            //more than one agent (majority vote)
        	return getNextAction2MultipleAgents(so, iterations, numberAgents, depth);
        } else {
            //only one agent
        	return getNextAction_PAR(so, iterations, depth);
        }
	}

    /**
     * Get the best next action and return it (multi-core [parallel] version).
     * Called by calcCertainty and getNextAction2.
     * 
     * @param sob			current game state (not changed on return)
     * @param iterations    rollout repeats (for each available action)
     * @param depth			rollout depth
     * @return actBest		the best next action
 	 * <p>						
	 * actBest has predicate isRandomAction()  (true: if action was selected 
	 * at random, false: if action was selected by agent).<br>
	 * actBest has also the members vTable and vBest to store the value for each available
	 * action (as returned by so.getAvailableActions()) and the value for the best action actBest.
	 * <p>
	 * {@code actBest.getScoreTuple()} returns the {@link ScoreTuple} connected with this
     * selected action.
     */
    private Types.ACTIONS_VT getNextAction_PAR2(StateObservation sob, int iterations, int depth) {
    	//the functions which are to be distributed on the cores: 
        List<Callable<ResultContainer>> callables = new ArrayList<>();
        //the results of these functions:
        List<ResultContainer> resultContainers = new ArrayList<>();
        // all available actions for state sob:
        List<Types.ACTIONS> actions = sob.getAvailableActions();

        Types.ACTIONS actBest = null;
        Types.ACTIONS_VT actBestVT = null;
		double[] vtable;
        vtable = new double[actions.size()];  
		double currProbab = 1.0/iterations;
		int sobPlayer = sob.getPlayer();
		ScoreTuple.CombineOP cOP1 = ScoreTuple.CombineOP.AVG;
		ScoreTuple bestActionScoreTuple = null;
		ScoreTuple[] nextActionScoreTuple = new ScoreTuple[actions.size()];

		for (int i=0; i<actions.size(); i++) {
			nextActionScoreTuple[i] = new ScoreTuple(sob);
		}
		
        nRolloutFinished = 0;
        nIterations = sob.getNumAvailableActions()* iterations;
        totalRolloutDepth = 0;

//        //if only one action is available, return it immediately:
//        if(sob.getNumAvailableActions() == 1) {
//        	actBest = actions.get(0);
//            return new Types.ACTIONS_VT(actBest.toInt(), false, vtable, 0.0);       		
//        }
        // we do  not return early but run the code below even in case sob.getNumAvailableActions()==1  
        // in order to get a meaningful nextAction.getScoreTuple() (score tuple for the action taken)

        //build the functions to be distributed on the scores.
        //For each iteration a function is built for each available action:
        for (int j = 0; j < iterations; j++) {
            for (int i = 0; i < sob.getNumAvailableActions(); i++) {

                //make a copy of the game state:
                StateObservation newSob = sob.copy();

                //the actual action i has to be saved on a new variable, since
                //the  for-loop value i is not accessible at the time of
                //execution of an callables-element:
                int firstActionIdentifier = i;

                //The callables, that is, the functions which are later executed on
                //multiple cores in parallel, are built. The callables are only
                //built here, they will be executed only later with 
                //invokeAll(callables) on executorService :
                callables.add(() -> {

                	//fetch the first action and execute it on the game state:
                    Types.ACTIONS firstAction = actions.get(firstActionIdentifier);
                    newSob.advance(firstAction);

                    //construct Random Agent and let it simulate a (random) rollout:
                    RandomSearch agent = new RandomSearch();
                    agent.startAgent(newSob, depth);			// contains BUG1 fix

                    //return result of simulation in an object of class ResultContainer:
                    return new ResultContainer(firstActionIdentifier, newSob, agent.getRolloutDepth());
                });
            }
        }

        try {
        	//executerService is called and it distributes all callables on all cores
        	//of the CPU. The callables perform the simulations and the results of these
        	//simulations are written to a stream:
            executorService.invokeAll(callables).stream().map(future -> {
                try {
                    return future.get();
                }
                catch (Exception e) {
                    throw new IllegalStateException(e);
                }

            //each result written to the stream is added to list resultContainers:    
            }).forEach(resultContainers::add);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //for each resultContainer in list resultContainers: add its game score
        //to the appropriate action in vtable:
        int r_i;
        for(ResultContainer resultContainer : resultContainers) {
        	r_i = resultContainer.firstAction;
        	vtable[r_i] += resultContainer.sob.getGameScore(sob);
        	nextActionScoreTuple[r_i].combine(resultContainer.sob.getGameScoreTuple(), cOP1, sobPlayer, currProbab);
            totalRolloutDepth += resultContainer.rolloutDepth;
            if(resultContainer.sob.isGameOver()) {
                nRolloutFinished++;
            }
        }

        //find the best next action:
        Types.ACTIONS bestAction = null;
        double bestActionScore = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < sob.getNumAvailableActions(); i++) {

            //calculate average score:
            vtable[i] /= iterations;

            if (bestActionScore < vtable[i]) {
            	actBest = actions.get(i);
                bestActionScore = vtable[i];
                bestActionScoreTuple = nextActionScoreTuple[i];
            }
        }

		actBestVT = new Types.ACTIONS_VT(actBest.toInt(), false, vtable, 
				 						 bestActionScore, bestActionScoreTuple);
        return actBestVT;
    }

    /**
     * Get the best next action and return it (single-core version, multiple agents).<br>
     * Called by {@code calcCertainty(..)} and {@link #getNextAction2(StateObservation, boolean, boolean)}.
     * 
     * @param sob			current game state (not changed on return)
     * @param iterations    rollout repeats (for each available action)
     * @param numberAgents	how many MC agents to calculate
     * @param depth			rollout depth
     * @return actBest		the best next action (including VTable and best score)
     * <p>
     * Each element of {@code actBest.getVTable()} holds the score for each available
     * action (corresponding to {@code sob.getAvailableActions()}). The score is in the case 
     * of multiple agents the <b>number of agents</b> which did select this action. 
     * <p>
     * The next action is one of the actions where <b>number of agents</b> is maximal. (If 
     * there are multiple such actions, one of them is selected at random.)  
     * <p>
     * {@code actBest.getScoreTuple()} returns the {@link ScoreTuple} connected with this
     * selected action.
     */
    private Types.ACTIONS_VT getNextAction2MultipleAgents(StateObservation sob, 
    		int iterations, int numberAgents, int depth) {
        Types.ACTIONS actBest = null;
        Types.ACTIONS_VT actBestVT = null;
        List<Types.ACTIONS> actions = sob.getAvailableActions();
		double[] vtable;
        vtable = new double[actions.size()];  
		double currProbab = 1.0/iterations;
		int sobPlayer = sob.getPlayer();
		ScoreTuple.CombineOP cOP1 = ScoreTuple.CombineOP.AVG;
		ScoreTuple.CombineOP cOP2 = ScoreTuple.CombineOP.MAX;
		ScoreTuple[] nextActionScoreTuple = new ScoreTuple[actions.size()];
		ScoreTuple bestActionScoreTuple = null;

		for (int i=0; i<actions.size(); i++) {
			nextActionScoreTuple[i] = new ScoreTuple(sob);
		}
		
        nRolloutFinished = 0;
        nIterations = sob.getNumAvailableActions()* iterations * numberAgents;
        totalRolloutDepth = 0;

//        if(sob.getNumAvailableActions() == 1) {
//        	actBest = actions.get(0);
//            return new Types.ACTIONS_VT(actBest.toInt(), false, vtable, 0.0);       		
//        }
        // we do  not return early but run the code below even in case sob.getNumAvailableActions()==1  
        // in order to get a meaningful nextAction.getScoreTuple() (score tuple for the action taken)

        for (int i = 0; i < numberAgents; i++) {
            int nextAction = 0;
            double nextActionScore = Double.NEGATIVE_INFINITY;

            for (int j = 0; j < sob.getNumAvailableActions(); j++) {
                double averageScore = 0;
                ScoreTuple avgScoreTuple = new ScoreTuple(sob);;

                for (int k = 0; k < iterations; k++) {
                    StateObservation newSob = sob.copy();

                    newSob.advance(actions.get(j));

                    RandomSearch agent = new RandomSearch();
                    agent.startAgent(newSob, depth);			// contains BUG1 fix

                    avgScoreTuple.combine(newSob.getGameScoreTuple(), cOP1, sobPlayer, currProbab);
                    if (newSob.isGameOver()) nRolloutFinished++;
                    totalRolloutDepth += agent.getRolloutDepth();
                }

                averageScore = avgScoreTuple.scTup[sobPlayer];
                if (nextActionScore <= averageScore) {
                    nextAction = j;
                    nextActionScore = averageScore;
                    nextActionScoreTuple[j].combine(avgScoreTuple, cOP2, sobPlayer, 0.0);
                }
            }
            //store in vtable[k] how many of the multiple agents did select action k 
            //as the best next action
            vtable[nextAction]++;
        }

        List<Types.ACTIONS> nextActions = new ArrayList<>();
        double bestActionScore = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < sob.getNumAvailableActions(); i++) {
            if (bestActionScore < vtable[i]) {
                nextActions.clear();
                nextActions.add(actions.get(i));
                bestActionScore = vtable[i];
                bestActionScoreTuple = nextActionScoreTuple[i];
            } else if(bestActionScore == vtable[i]) {
                nextActions.add(actions.get(i));
            }
        }
        
        actBest = nextActions.get(random.nextInt(nextActions.size()));
		actBestVT = new Types.ACTIONS_VT(actBest.toInt(), false, vtable, 
										 bestActionScore, bestActionScoreTuple);
        return actBestVT;
    }

	@Override
    public double getScore(StateObservation sob) {
        
		// This if branch is vital: It was missing before, and if 'sob' was a game-over state
		// this could result in wrong scores.
		// Now we fix this by returning sob.getGameScore(sob) on a game-over situation:
        if (sob.isGameOver()) {
        	return sob.getGameScore(sob);
        } else {       	
            Types.ACTIONS_VT actBestVT = getNextAction2(sob, false, true);
            return actBestVT.getVBest();
        }

    }
    
	/**
	 * Return a tuple with the agent's estimate of {@code sob}'s final game value (final reward) 
	 * <b>for all players</b>. <br>
	 * Is called by the n-ply wrappers ({@link MaxNWrapper}, {@link ExpectimaxWrapper}). 
	 * 
	 * @param so	the state s_t for which the value is desired
	 * @return		an N-tuple with elements V(s_t|i), i=0,...,N-1, the agent's estimate of 
	 * 				the future score for s_t from the perspective of player i.
	 * <p>
	 * The {@link ScoreTuple} is obtained by running  
	 * {@link #getNextAction2(StateObservation, boolean, boolean)} and retrieving from 
	 * the returned {@link Types.ACTIONS_VT} object the score tuple.
	 */
	@Override
	public ScoreTuple getScoreTuple(StateObservation so) {
		ScoreTuple sc = new ScoreTuple(so);
		switch (so.getNumPlayers()) {
		case 1: 
			sc.scTup[0] = this.getScore(so);
			break;
		case 2:
			int player = so.getPlayer();
			int opponent = (player==0) ? 1 : 0;
			sc.scTup[player] = this.getScore(so);
			sc.scTup[opponent] = -sc.scTup[player];
			break;
		default: 
	        if (so.isGameOver()) {
	        	return so.getGameScoreTuple();
	        } else {
	        	
	            Types.ACTIONS_VT actBestVT = getNextAction2(so, false, true);

	            return actBestVT.getScoreTuple();
	        }
		}
    	return sc;
	}

	/**
	 * Return the agent's estimate of {@code sob}'s final game value (final reward) <b>for all players</b>.
	 * <p>
	 * The difference to {@link #getScoreTuple(StateObservation)} is that this function should 
	 * <b>not</b> call getScore or getScoreTuple in any way, to avoid endless recursion.
	 * 
	 * @param sob			the current game state
	 * @return				the agent's estimate of the final game value <b>for all players</b>. 
	 * 						The return value is a tuple containing  
	 * 						{@link StateObservation#getNumPlayers()} {@code double}'s. 
	 */
	@Override
	public ScoreTuple estimateGameValueTuple(StateObservation sob) {
		ScoreTuple sc = new ScoreTuple(sob);
		boolean rgs = m_oPar.getRewardIsGameScore();
		for (int i=0; i<sob.getNumPlayers(); i++) 
			sc.scTup[i] += sob.getReward(i, rgs);
		return sc;
	}

    /**
     * Calculate the certainty by repeating the next-action calculation 
     * NC times and calculating the relative frequency of the most frequent
     * next action
     * 
     * @param sob			the game state
     * @param numberAgents	= 1: use getNextAction2 (parallel version) <br>
     *                      &gt; 1: use getNextActionMultipleAgents with NUMBERAGENTS=numberAgents
     * @param silent
     * @param NC			number of repeats for next-action calculation
     * @param iterations    rollout repeats (for each available action)
     * @param depth			rollout depth
     * @return the certainty (highest bin in the relative-frequency histogram of possible actions)
     */
    public double calcCertainty(StateObservation sob, /*double[] vtable,*/
    		int numberAgents, boolean silent, int NC, int iterations, int depth) {
        double[] wtable = new double[4];
        double highestBin;
        int nextAction;
        
        if(sob.getNumAvailableActions() == 1) {
            return 1.0;
        }

        for (int i = 0; i < NC; i++) {
        	if (numberAgents==1) {
        		nextAction = getNextAction_PAR(sob,iterations, depth).toInt();
        	} else {
        		nextAction = getNextAction2MultipleAgents(sob,iterations,numberAgents, depth).toInt();
        		if (!silent) System.out.print(".");
        	}
            wtable[nextAction]++;
        }
        if (numberAgents!=1 & !silent) System.out.println("");

        highestBin = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < wtable.length; i++) {
            if (highestBin < wtable[i]) {
                highestBin = wtable[i];
            } 
        }
        double cert = highestBin/NC;
        
    	return cert;
    }

    public double getAverageRolloutDepth() {
        return totalRolloutDepth/nIterations;
    }

    public int getNRolloutFinished() {
        return nRolloutFinished;
    }

    public int getNIterations() {
        return nIterations;
    }
	public ParMC getMCPar() {
		return m_mcPar;
	}
	
    @Override
    public String stringDescr() {
        String cs = getClass().getName();
		String str = cs + ": iterations:" + m_mcPar.getNumIter() 
				+ ", rollout depth:" + m_mcPar.getRolloutDepth()
				+ ", # agents:"+ m_mcPar.getNumAgents();
		return str;
    }
}




