package games.Othello;

import java.io.Serializable;

import games.Feature;
import games.StateObservation;
import games.TicTacToe.StateObserverTTT;
import tools.Types;
import tools.Types.ACTIONS_VT;

public class FeatureOthello extends BaseOthello implements Feature, Serializable{

	
	private static final long serialVersionUID = 12L;
	
	public FeatureOthello(int featmode)
	{
		super("", featmode);
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
	public double[] prepareFeatVector(StateObservation sob) {
		assert (sob instanceof StateObserverOthello) : "Input 'sob' is not of class StateObserverOthello";
		StateObserverOthello so = (StateObserverOthello) sob;
		int table[][] = so.getCurrentGameState();
		int player = so.getPlayer();
		double[] input = prepareInputVector(table,player); // from super
		return input;
		
	}

	@Override
	public double getScore(StateObservation sob) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ACTIONS_VT getNextAction2(StateObservation sob, boolean random, boolean silent) {
		// TODO Auto-generated method stub
		throw new RuntimeException("FeatureTTT does not implement getNextAction2");
	}

}
