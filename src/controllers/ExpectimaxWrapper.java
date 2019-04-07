package controllers;

import java.io.Serializable;

import games.StateObservation;
import tools.Types.ScoreTuple;

/**
 * Wrapper based on {@link ExpectimaxNAgent} for n-ply look-ahead in nondeterministic games.
 * Wrap agent {@code pa} into an {@link ExpectimaxNAgent} with {@code nply} plies look-ahead.
 * Override {@link #estimateGameValue(StateObservation)} such that it returns the score tuple
 * of the wrapped agent {@code pa}.
 * 
 * @author Wolfgang Konen, TH K�ln, Dec'17
 * 
 * @see ExpectimaxNAgent
 */
public class ExpectimaxWrapper extends ExpectimaxNAgent implements Serializable {
	private PlayAgent wrapped_pa;
	
	public ExpectimaxWrapper(PlayAgent pa, int nply) {
		super("ExpectimaxWrapper", nply);
		this.wrapped_pa = pa;
	}
	
	/**
	 * When the recursion tree has reached its maximal depth m_depth, then return
	 * an estimate of the game score.  
	 * <p>
	 * Here we use the wrapped {@link PlayAgent} to return a game value.
	 * 
	 * @param sob	the state observation
	 * @return		the estimated score 
	 */
	@Override
	public double estimateGameValue(StateObservation sob) {	
		return wrapped_pa.getScore(sob);
	}

	/**
	 * When the recursion tree has reached its maximal depth m_depth, then return
	 * an estimate of the game score (tuple for all players).  
	 * <p>
	 * Here we use the wrapped {@link PlayAgent} to return a tuple of game values.
	 * 
	 * @param sob	the state observation
	 * @return		the tuple of estimated score 
	 */
	@Override
	public ScoreTuple estimateGameValueTuple(StateObservation sob) {
		return wrapped_pa.getScoreTuple(sob);
		//
		// the following would be too specific to TDNTuple2Agt, we delegate it to  
		// getScoreTuple of the wrapped agent:
//		boolean rgs = m_oPar.getRewardIsGameScore();
//		ScoreTuple sc = new ScoreTuple(sob);
//		//sc = wrapped_pa.getScoreTuple(sob);
//		sc.scTup[0] = wrapped_pa.getScore(sob);
//		for (int i=0; i<sob.getNumPlayers(); i++) 
//			sc.scTup[i] += sob.getReward(i, rgs);
//		return sc;
	}
	
	public PlayAgent getWrappedPlayAgent() {
		return wrapped_pa;
	}

	@Override
	public String stringDescr() {
		String cs = wrapped_pa.getClass().getSimpleName();
		cs = cs + "[nPly="+m_depth+"]";
		return cs;
	}

	// getName: use method ObserverBase::getName()
	
	public String getFullName() {
		String cs = wrapped_pa.getClass().getSimpleName();
		cs = cs + "[nPly="+m_depth+"]";
		return cs;
	}

}
