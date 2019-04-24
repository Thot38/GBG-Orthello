package Othello;

import java.io.Serializable;

import games.Feature;
import games.StateObservation;
import tools.Types;
import tools.Types.ACTIONS_VT;

public class FeatureOthello extends BaseOthello implements Feature, Serializable{

	
	private static final long serialVersionUID = 12L;
	
	public FeatureOthello(int featmode)
	{
		super();
	}
	
	
	@Override
	public double[] prepareFeatVector(StateObservation so) {
		assert(so instanceof StateObserverOthello) : "Input 'sob' is not of class StateObserverOthello";
		StateObserverOthello sob = (StateObserverOthello) so;
		int[][] table = sob.getCurrentGameState();
		int player = Types.PLAYER_PM[sob.getPlayer()];
		double[] input = super.prepareInputVector(-player, table);
		return input;
	}

	@Override
	public String stringRepr(double[] featVec) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getFeatmode() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int[] getAvailFeatmode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getInputSize(int featmode) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getScore(StateObservation sob) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public ACTIONS_VT getNextAction2(StateObservation sob, boolean random, boolean silent) {
		throw new RuntimeException("FeatureTTT does not implement getNextAction2");
	}

}
