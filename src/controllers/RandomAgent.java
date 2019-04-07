package controllers;
import games.StateObservation;
import params.ParOther;
import tools.Types;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * the Random {@link PlayAgent} for TicTacToe 
 * 
 * @author Wolfgang Konen, TH K�ln, Nov'16
 * 
 */
public class RandomAgent extends AgentBase implements PlayAgent {
	private Random rand;
	private int[][] m_trainTable = null;
	private double[][] m_deltaTable = null;
	/**
	 * change the version ID for serialization only if a newer version is no longer
	 * compatible with an older one (older .agt.zip will become unreadable or you have
	 * to provide a special version transformation)
	 */
	private static final long  serialVersionUID = 12L;
		
	public RandomAgent(String name) {
		super(name);
		super.setMaxGameNum(1000); 		
		super.setGameNum(0); 			
        rand = new Random(System.currentTimeMillis());
		setAgentState(AgentState.TRAINED);
	}

	public RandomAgent(String name, long randomSeed) {
		super(name);
		super.setMaxGameNum(1000);
		super.setGameNum(0);
		rand = new Random(randomSeed);
		setAgentState(AgentState.TRAINED);
	}
		
    public RandomAgent(String name, ParOther oPar) {
        super(name);
		super.setMaxGameNum(1000); 		
		super.setGameNum(0); 			
		this.m_oPar = new ParOther(oPar);
        rand = new Random(System.currentTimeMillis());
        setAgentState(AgentState.TRAINED);
    }

    /**
	 * Get the best next action and return it 
	 * (NEW version: returns ACTIONS_VT and has a recursive part for multi-moves)
	 * 
	 * @param so			current game state (is returned unchanged)
	 * @param random		allow random action selection with probability m_epsilon
	 * @param silent		execute silently without outputs
	 * @return actBest		the best action. If several actions have the same
	 * 						score, break ties by selecting one of them at random. 
	 * <p>						
	 * actBest has predicate isRandomAction()  (true: if action was selected 
	 * at random, false: if action was selected by agent).<br>
	 * actBest has also the members vTable to store the value for each available
	 * action (as returned by so.getAvailableActions()) and vBest to store the value for the best action actBest.
	 */
	@Override
	public Types.ACTIONS_VT getNextAction2(StateObservation so, boolean random, boolean silent) {
        Types.ACTIONS actBest = null;
        Types.ACTIONS_VT actBestVT = null;
        ArrayList<Types.ACTIONS> acts = so.getAvailableActions();
//        Types.ACTIONS[] actions = new Types.ACTIONS[acts.size()];
        List<Types.ACTIONS> bestActions = new ArrayList<>();
		double[] vtable = new double[acts.size()];  
		
		int i,j;
		double MaxScore = -Double.MAX_VALUE;
		double CurrentScore = 0; 	// the quantity to be maximized
		
//		StateObservation NewSO;
//		int count = 1; // counts the moves with same MaxScore
//      int iBest;

		assert so.isLegalState() : "Not a legal state";

        for(i = 0; i < acts.size(); ++i)
        {
//        	actions[i] = acts.get(i);
//        	NewSO = so.copy();
//        	NewSO.advance(actions[i]);
        	CurrentScore = rand.nextDouble();
        	vtable[i] = CurrentScore;
        	if (MaxScore < CurrentScore) {
        		MaxScore = CurrentScore;
                bestActions.clear();
                bestActions.add(acts.get(i));
//        		actBest = actions[i];
//        		iBest  = i; 
//        		count = 1;
        	} else if (MaxScore == CurrentScore) {
                bestActions.add(acts.get(i));
//        		count++;	        
        	}
        } // for
        actBest = bestActions.get(rand.nextInt(bestActions.size()));
        // if several actions have the same best score, select one of them randomly
// --- Old code, unnecessary complicated:         
//        if (count>1) {  // more than one action with MaxScore: 
//        	// break ties by selecting one of them randomly
//        	int selectJ = (int)(rand.nextDouble()*count);
//        	for (i=0, j=0; i < actions.length; ++i) 
//        	{
//        		if (vtable[i]==MaxScore) {
//        			if (j==selectJ) actBest = actions[i];
//        			j++;
//        		}
//        	}
//        }

        // optional: show the best action
        assert actBest != null : "Oops, no best action actBest";
        if (!silent) {
        	StateObservation NewSO = so.copy();
        	NewSO.advance(actBest);
        	System.out.print("---Best Move: "+NewSO.stringDescr()+"   "+MaxScore);
        }			
		actBest.setRandomSelect(true);		// the action was a random move
	
		actBestVT = new Types.ACTIONS_VT(actBest.toInt(), true, vtable, MaxScore);
        return actBestVT;
	}


	@Override
	public double getScore(StateObservation sob) {
		return rand.nextDouble();
	}

}