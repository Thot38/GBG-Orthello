package controllers;

import games.StateObservation;
import params.ParOther;
import games.Evaluator;
import games.GameBoard;
import tools.Types;
import tools.Types.ScoreTuple;

/**
 * The abstract interface for game playing agents.
 * <p>
 * See {@link AgentBase} for a base implementation of this interface. Other implementing classes 
 * should usually inherit from {@link AgentBase}.
 *
 * @author Wolfgang Konen, TH K�ln
 */
public interface PlayAgent {
	public enum AgentState {RAW, INIT, TRAINED};
	
//	/**
//	 * <em> This function is now deprecated. Use instead: </em>
//	 * {@code ACTION_VT} {@link PlayAgent#getNextAction2(StateObservation, boolean, boolean)}. 
//	 * <p>
//	 * Get the best next action and return it
//	 * @param sob			current game state (not changed on return)
//	 * @param random		allow epsilon-greedy random action selection	
//	 * @param vtable		must be an array of size n+1 on input, where 
//	 * 						n=sob.getNumAvailableActions(). On output,
//	 * 						elements 0,...,n-1 hold the score for each available 
//	 * 						action (corresponding to sob.getAvailableActions())
//	 * 						In addition, vtable[n] has the score for the 
//	 * 						best action.
//	 * @param silent
//	 * @return actBest		the best action 
//	 * 
//	 * Side effect: sets member randomSelect (true: if action was selected 
//	 * at random, false: if action was selected by agent).
//	 * 
//	 */	
//	@Deprecated
//	public Types.ACTIONS getNextAction(StateObservation sob, boolean random, 
//			double[] vtable, boolean silent);
	
	/**
	 * Get the best next action and return it 
	 * (NEW version: returns ACTIONS_VT and has a recursive part for multi-moves)
	 * 
	 * @param sob			current game state (is returned unchanged)
	 * @param random		allow random action selection with probability m_epsilon
	 * @param silent
	 * @return actBest		the best action. If several actions have the same
	 * 						score, break ties by selecting one of them at random. 
	 * <p>						
	 * actBest has predicate isRandomAction()  (true: if action was selected 
	 * at random, false: if action was selected by agent).<br>
	 * actBest has also the members vTable to store the value for each available
	 * action (as returned by so.getAvailableActions()) and vBest to store the value for the best action actBest.
	 */
	public Types.ACTIONS_VT getNextAction2(StateObservation sob, boolean random, boolean silent);
	
	/**
	 * Return the agent's estimate of the final score for that game state.
	 * @param sob			the current game state;
	 * @return				the agent's estimate of the final score for that state. 
	 * 						For 2-player games this is usually the probability 
	 * 						that the player to move wins from that state.
	 * 						If game is over: the score for the player who *would*
	 * 						move (if the game were not over).<p>
	 * Each player wants to maximize *its* score.	 
	 */
	public double getScore(StateObservation sob);
	
	public ScoreTuple getScoreTuple(StateObservation sob);
	
	/**
	 * Return the agent's estimate of the final game value (final reward). Is called when
	 * maximum episode length (TD) or maximum tree depth for certain agents (Minimax) 
	 * is reached.
	 * 
	 * @param sob			the current game state;
	 * @return				the agent's estimate of the final reward. This may be 
	 * 						the same as {@link #getScore(StateObservation)} (as 
	 * 						implemented in {@link AgentBase}). But it may as well be 
	 * 						overridden by derived classes.
	 */
	public double estimateGameValue(StateObservation sob);
	
	/**
	 * Return the agent's estimate of {@code sob}'s final game value (final reward) <b>for all players</b>. 
	 * Is called when maximum episode length (TD) or maximum tree depth for certain agents 
	 * (Max-N, Expectimax-N) is reached.
	 * 
	 * @param sob			the current game state
	 * @return				the agent's estimate of the final reward <b>for all players</b>. 
	 * 						The return value is a tuple containing  
	 * 						{@link StateObservation#getNumPlayers()} {@code double}'s. 
	 */
	public Types.ScoreTuple estimateGameValueTuple(StateObservation sob);
	
	/**
	 * Train the Agent for one complete game episode. <p>
	 * Side effects: Increment m_GameNum by +1. Change the agent's internal  
	 * parameters (weights and so on).
	 * @param so		the state from which the episode is played (usually the
	 * 					return value of {@link GameBoard#chooseStartState(PlayAgent)} to get
	 * 					some exploration of different game paths)
	 * @return			true, if agent raised a stop condition (only CMAPlayer)	 
	 */
	public boolean trainAgent(StateObservation so /*, int epiLength, boolean learnFromRM*/);
	
	public String printTrainStatus();
	
	public boolean isTrainable();
	
	/**
	 * @return a string with information about this agent
	 * @see #stringDescr2()
	 */
	public String stringDescr();
	/**
	 * @return a string with additional information about this agent
	 * @see #stringDescr()
	 */
	public String stringDescr2();
	
	public byte getSize();		// estimated size of agent object

	/**
	 * @return maximum number of training games
	 */
	public int getMaxGameNum();
	/**
	 * @return number of training games that this agent actually has performed
	 */
	public int getGameNum();
	/**
	 * 
	 * @return number of learn actions (calls to update()) for trainable agents.
	 * (For non-trainable agents, {@link AgentBase} will return 0L.)
	 */
	public long getNumLrnActions();
	/**
	 * 
	 * @return number of training moves for trainable agents. Difference to 
	 * {@link #getNumLrnActions()}: it is incremented on random actions as well.
	 * (For non-trainable agents, {@link AgentBase} will return 0L.)
	 */
	public long getNumTrnMoves();
	
	/**
	 * @return number of moves in the last train episode for trainable agents.
	 * (For non-trainable agents, {@link AgentBase} will return 0.)
	 */
	public int getMoveCounter();

	public void setMaxGameNum(int num);
	public void setGameNum(int num);
	
	public ParOther getParOther();
	
	/**
	 * @return During training: Call {@link Evaluator} after this number of training games
	 */
	public int getNumEval();
	public void setNumEval(int num);	
	
	public AgentState getAgentState(); 
	public void setAgentState(AgentState aState);

	public String getName();
	public void setName(String name);
	
}
