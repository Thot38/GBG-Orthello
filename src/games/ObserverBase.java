package games;

import controllers.PlayAgent;
import tools.Types;
import tools.Types.ScoreTuple;

/**
 * Class {@link ObserverBase} implements functionality of the interface {@link StateObservation} 
 * common to all games (things related to advance, game value, reward, ...).
 * <p>
 * This default behavior in {@link ObserverBase} - which may be overridden in derived classes -
 * is for deterministic, 2-player games, where reward and game score are the same.
 * 
 * @see StateObservation
 */
abstract public class ObserverBase {
	protected int m_counter = 0;
	
	/**
	 * This is just to signal that derived classes will be either abstract or implement
	 * advance(ACTIONS), as required by the interface {@link StateObservation}.
	 */
	abstract public void advance(Types.ACTIONS action);
	
	/**
     * Advance the current state to a new afterstate (do the deterministic part of advance)
     *
     * @param action the action
     */
    public void advanceDeterministic(Types.ACTIONS action) {
    	// since ObserverBase is for a deterministic game, advanceDeterministic()
    	// is the same as advance():
    	advance(action);
    }

    /**
     * Advance the current afterstate to a new state (do the nondeterministic part of advance)
     */
    public void advanceNondeterministic() {
    	// nothing to do here, since ObserverBase is for a deterministic game    	
    }

	abstract public int getPlayer();
	abstract public int getNumPlayers();

	public int getMinEpisodeLength() {
		return 1;
	}
	
	/**
	 * @return number of moves in the episode where {@code this} is part of.
	 */
	public int getMoveCounter() {
		return m_counter;
	}

	public void resetMoveCounter() {
		m_counter = 0;
	}
	
	protected void incrementMoveCounter() {
		m_counter++;
	}
	
	/**
	 * This is just to signal that derived classes will be either abstract or implement
	 * getGameScore(), as required by the interface {@link StateObservation} as well.
	 */
	abstract public double getGameScore();

	/**
	 * Same as getGameScore(), but relative to referingState. This relativeness
	 * is usually only relevant for games with more than one player.
	 * <p>
	 * This implementation is valid for all classes implementing {@link StateObservation}, once
	 * they have a valid implementation for {@link #getGameScore(int)}.
	 * 
	 * @param referringState see below
	 * @return  If referringState has the same player as this, then it is getGameScore().<br> 
	 * 			If referringState has opposite player, then it is getGameScore()*(-1). 
	 */
    public double getGameScore(StateObservation referringState) {
    	return getGameScore(referringState.getPlayer());
    }
	

	/**
	 * Same as {@link #getGameScore()}, but from the perspective of player {@code player}. The 
	 * perspective shift is usually only relevant for games with more than one player.
	 * <p>
	 * This implementation in {@link ObserverBase} is only valid for 1- or 2-player games.
	 * 
	 * @param player the player whose perspective is taken, a number in 0,1,...,N.
	 * @return  If {@code player} and {@code this.player} are the same, then it is getGameScore().<br> 
	 * 			If they are different, then it is getGameScore()*(-1). 
	 */
	public double getGameScore(int player) {
    	assert (this.getNumPlayers()<=2) : "ObserverBase's implementation of getGameScore(ref) is not valid for current class";
		return (this.getPlayer() == player ? getGameScore() : (-1)*getGameScore() );
	}
	
	/**
	 * This implementation is valid for all classes implementing {@link StateObservation}, once
	 * they have a valid implementation for {@link #getGameScore(int)}.
	 * 
	 * @return	a score tuple which has as {@code i}th value  {@link #getGameScore(int)}
	 * 			with {@code i} as argument
	 */
	public ScoreTuple getGameScoreTuple() {
		int N = this.getNumPlayers();
		ScoreTuple sc = new ScoreTuple(N);
		for (int i=0; i<N; i++)
			sc.scTup[i] = this.getGameScore(i);
		return sc;
	}

	/**
	 * The cumulative reward. 
	 * The default implementation here in {@link ObserverBase} implements the reward as game score.
	 * 
	 * @param rewardIsGameScore if true, use game score as reward; if false, use a different, 
	 * 		  game-specific reward
	 * @return the cumulative reward
	 */
	public double getReward(boolean rewardIsGameScore) {
		return getGameScore();
	}
	
	/**
	 * Same as getReward(), but relative to referringState. 
	 * The default implementation here in {@link ObserverBase} implements the reward as game score.
	 * 
	 * @param referringState
	 * @param rewardIsGameScore if true, use game score as reward; if false, use a different, 
	 * 		  game-specific reward
	 * @return the cumulative reward 
	 */
	public double getReward(StateObservation referringState, boolean rewardIsGameScore) {
		return getGameScore(referringState);
	}

	/**
	 * Same as {@link #getReward(StateObservation,boolean)}, but with the player of referringState.
	 * The default implementation here in {@link ObserverBase} implements the reward as game score.
	 *  
	 * @param player the player of referringState, a number in 0,1,...,N.
	 * @param rewardIsGameScore if true, use game score as reward; if false, use a different, 
	 * 		  game-specific reward
	 * @return  the cumulative reward 
	 */
	public double getReward(int player, boolean rewardIsGameScore) {
        return getGameScore(player);
	}
	
	/**
	 * This implementation is valid for all classes implementing {@link StateObservation}, once
	 * they have a valid implementation for {@link #getReward(int,boolean)}.
	 * 
	 * @param rewardIsGameScore if true, use game score as reward; if false, use a different, 
	 * 		  game-specific reward
	 * @return	a score tuple which has as {@code i}th value  
	 * 			{@link #getReward(int, boolean)} with {@code i} as first argument
	 */
	public ScoreTuple getRewardTuple(boolean rewardIsGameScore) {
		int N = this.getNumPlayers();
		ScoreTuple sc = new ScoreTuple(N);
		for (int i=0; i<N; i++)
			sc.scTup[i] = this.getReward(i,rewardIsGameScore);
		return sc;		
	}


	/**
	 * This is just to signal that derived classes will be either abstract or implement
	 * stringDescr(), as required by the interface {@link StateObservation} as well.
	 * The definition of stringDescr() is needed here, because  {@link #toString()} needs it.
	 */
	abstract public String stringDescr();

    public String toString() {
        return stringDescr();
    }
    
    public boolean stopInspectOnGameOver() {
    	return true;
    }
}