package games;

import java.io.Serializable;
import java.util.ArrayList;

import controllers.AgentBase;
import controllers.PlayAgent;
import controllers.TD.ntuple2.*;
import tools.Types;
import tools.Types.ACTIONS;
import tools.Types.ScoreTuple;

/**
 * Class StateObservation observes the current state of the game, it has utility functions for
 * <ul>
 * <li> returning the available actions ({@link #getAvailableActions()}), 
 * <li> advancing the state of the game with a specific action ({@link #advance(Types.ACTIONS)}),
 * <li> copying the current state
 * <li> signaling end, score and winner of the game
 * <li> and others.
 * </ul><p>
 * 
 * StateObservation is for deterministic games.
 * 
 * @see StateObsNondeterministic
 * 
 * @author Wolfgang Konen, TH K�ln, Feb'17
 */
public interface StateObservation extends Serializable{
    //Types.ACTIONS[] actions=null;
	
	public StateObservation copy();

	public boolean isGameOver();

	public boolean isDeterministicGame();
	
	/**
	 * @return true, if the game emits rewards only in the final game position
	 */
	public boolean isFinalRewardGame();

	public boolean isLegalState();
	
	public boolean stopInspectOnGameOver();
	
//	/**
//	 * @return this predicate is true only for 2-player games where the reward of player 0
//	 * is always the negative of the reward of player 1. E.g. TTT, Hex, where a reward of +1
//	 * for one player means -1 for the other. 
//	 */
//	public boolean has2OppositeRewards();
	
	/**
	 * 
	 * @return a string representation of the current state
	 */
	@Deprecated
	// Why? - Because java.Object has already a default for toString() and thus it can
	// go unnoticed if a class implementing StateObservation does not implement toString().
	// Better use stringDescr()
	public String toString();

	/**
	 * 
	 * @return a string representation of the current state
	 */
	public String stringDescr();

	/**
	 * 
	 * @return a string representation of action {@code act}
	 */
	public String stringActionDescr(ACTIONS act);

	/**
	 * This method should be only called if game is over. The player is 
	 * the player who would be next in turn (if the game were not over)
	 * <p>
	 * WINNER::key() returns the number of the player who has won.
	 * 
	 * @return PLAYER_LOSES(-1), TIE(0), PLAYER_WINS(1)
	 */
	public Types.WINNER getGameWinner();
	// Do we need this method really?

//	/**
//	 * @return 	the game value, i.e. an <em>estimate of the final score</em> which 
//	 * 			can be reached from this state (assuming perfect play).  
//	 * 			This member function can be {@link StateObservation}'s heuristic  
//	 * 			for the <em>potential</em> of that state. If such a heuristic is not known, 
//	 * 			{@link #getGameValue()} might simply return {@link #getGameScore()}.
//	 */
//	public double getGameValue();
	
	/**
	 * This method is deprecated, use instead 
	 * {@link #getGameScore(StateObservation) this.getGameScore(this)}.
	 * 
	 * @return 	the game score of {@code this}, from the perspective of the player to move in {@code this}.
	 * 			For a 2-player game the score is often only non-zero for a 
	 * 			game-over state. For all games it is the cumulative score.
	 * <p>
	 */
	@Deprecated
	public double getGameScore();
	
	/**
	 * The game score, seen from the perspective of {@code referingState}'s player. This 
	 * relativeness is usually only relevant for games with more than one player.
	 * <p>
	 * The keyword abstract signals that derived classes will be either abstract or implement
	 * {@link #getGameScore(StateObservation)}, as required by the interface {@link StateObservation} as well.
	 * 
	 * @param referringState see below
	 * @return  If referringState has the same player as {@code this}, then it is getGameScore().<br> 
	 * 			If referringState has opposite player, then it is getGameScore()*(-1). 
	 */
	public double getGameScore(StateObservation referringState);
	
	/**
	 * Same as {@link #getGameScore(StateObservation refer)}, but with the player of state refer. 
	 * @param player the player whose perspective is taken, a number in 0,1,...,N.
	 * @return  If {@code player} and {@code this.player} are the same, then it is getGameScore().<br> 
	 * 			If they are different, then it is getGameScore()*(-1). 
	 */
	public double getGameScore(int player);
	
	/**
	 * @return	a score tuple which has as {@code i}th value  {@link #getGameScore(int)} 
	 * 			with {@code i} as argument
	 */
	public ScoreTuple getGameScoreTuple();

	/**
	 * The cumulative reward of {@code this}, from the perspective of the player to move in {@code this}.
	 * <p>
	 * This method is deprecated, use instead
	 * {@link #getReward(StateObservation,boolean) this.getGameScore(this,rewardIsGameScore)}.
	 * 
	 * @param rewardIsGameScore if true, use game score as reward; if false, use a different, 
	 * 		  game-specific reward
	 * @return the cumulative reward
	 */
	@Deprecated
	public double getReward(boolean rewardIsGameScore);
	
	/**
	 * The cumulative reward, seen from the perspective of {@code referingState}'s player. This 
	 * relativeness is usually only relevant for games with more than one player.
	 * @param referringState	see {@link #getGameScore(StateObservation)}
	 * @param rewardIsGameScore if true, use game score as reward; if false, use a different, 
	 * 		  game-specific reward
	 * @return  the cumulative reward 
	 */
	public double getReward(StateObservation referringState, boolean rewardIsGameScore);

	/**
	 * Same as {@link #getReward(StateObservation,boolean)}, but with the player of referringState. 
	 * @param player the player of referringState, a number in 0,1,...,N.
	 * @param rewardIsGameScore if true, use game score as reward; if false, use a different, 
	 * 		  game-specific reward
	 * @return  the cumulative reward 
	 */
	public double getReward(int player, boolean rewardIsGameScore);

	/**
	 * @param rewardIsGameScore if true, use game score as reward; if false, use a different, 
	 * 		  game-specific reward
	 * @return	a score tuple which has as {@code i}th value  
	 * 			{@link #getReward(int, boolean)} with {@code i} as first argument
	 */
	public ScoreTuple getRewardTuple(boolean rewardIsGameScore);
	
	public double getMinGameScore();
	public double getMaxGameScore();
	
	public int getMinEpisodeLength();

	/**
	 * @return number of moves in the episode where {@code this} is part of.
	 */
	public int getMoveCounter();

	public void resetMoveCounter();
	
	/**
	 *
	 * @return the name of the Game (should be a valid directory name)
	 */
	public String getName();

	/**
	 * Advance the current state with {@code action} to a new state
	 * 
	 * @param action the action
	 */
	public void advance(ACTIONS action);

    /**
     * Advance the current state to a new afterstate (do the deterministic part of advance).<p>
     *
     * (This method is not really necessary for deterministic games - it does the same as 
     * {@link #advance(ACTIONS)} - but we have it in the interface to allow the same syntax in 
     * {@link TDNTuple3Agt} when making an action for any StateObservation, deterministic 
     * or nondeterministic.)
     * 
     * @param action the action
     */
    public void advanceDeterministic(ACTIONS action);

    /**
     * Advance the current afterstate to a new state (do the nondeterministic part of advance).<p>
     * 
     * (This method is not really necessary for deterministic games - it does just nothing - but we
     * have it here to allow the same syntax in {@link TDNTuple3Agt} when making an action for
     * any StateObservation, deterministic or nondeterministic.)
     */
    public void advanceNondeterministic();
    
    /**
     * Return the afterstate preceding {@code this}. The afterstate is the state resulting 
     * after the deterministic part of the preceding action. Return {@code null}, if this
     * afterstate is not known.
     *  
     * @return the afterstate or {@code null}. <p>
     * 
     * For deterministic games, the afterstate is identical to {@code this}. For 
     * nondeterministic games, it depends on the game: E.g. for 2048 it is usually not known, 
     * if we know only the current state {@code this}. For Backgammon it is the preceding 
     * board position (if known) without the nondeterministic dice part. 
     */
    public StateObservation getPrecedingAfterstate();

    /**
     * Return all available actions (all actions possible in this game)
     * @return {@code ArrayList<ACTIONS>}
     */
	public ArrayList<ACTIONS> getAllAvailableActions();

	/**
	 * Return the actions available in this specific state
	 * @return {@code ArrayList<ACTIONS>}
	 */
	public ArrayList<ACTIONS> getAvailableActions();

	public int getNumAvailableActions();

	/**
	 * Given the current state, what are the available actions? 
	 * Set them in member ACTIONS[] actions.
	 */
	public void setAvailableActions();
	
	public Types.ACTIONS getAction(int i);
	
	/**
	 * Given the current state, store some info useful for inspecting the  
	 * action actBest and double[] vtable returned by a call to <br>
	 * {@code ACTION_VT} {@link PlayAgent#getNextAction2(StateObservation, boolean, boolean)}. 
	 *  
	 * @param actBest	the best action
	 * @param vtable	one double for each action in {@link #getAvailableActions()}:
	 * 					it stores the value of that action (as given by the double[] 
	 * 					from {@link Types.ACTIONS_VT#getVTable()}) 
	 */
	public void storeBestActionInfo(ACTIONS actBest, double[] vtable); 
	
	/**
	 * 
	 * @return  {0,1,...,n-1} for an n-player game: <b>who moves next</b>
	 */
	public int getPlayer();

//	/**
//	 * @return  1 for a 1-player game (e.g. 2048),  
//	 * 			{+1,-1} for a 2-player game (e.g. TicTacToe): who moves next
//	 * 			{0,1,...,n-1} for an n-player game: who moves next
//	 */
//	@Deprecated
//	public int getPlayerPM();

	/**
	 * @return  1 for a 1-player game (e.g. 2048), 2 for a 2-player game
	 * 			(e.g. TicTacToe) and so on
	 */
	public int getNumPlayers();
	
}
