package games.Othello;

import java.io.Serializable;

import games.Feature;
import games.StateObservation;
import games.CFour.StateObserverC4;
import games.TicTacToe.StateObserverTTT;
import tools.Types;
import tools.Types.ACTIONS_VT;

public class FeatureOthello implements Feature, Serializable{

	
	private static final long serialVersionUID = 12L;
	
	public FeatureOthello(int featmode)
	{
		
	}

	@Override
	public double[] prepareFeatVector(StateObservation sob) {
		assert (sob instanceof StateObserverOthello) : "Input 'sob' is not of class StateObserverOthello";
		StateObserverOthello so = (StateObserverOthello) sob;
//		int[][] table = so.getTable();
		int player = Types.PLAYER_PM[so.getPlayer()];
		// note that TicTDBase.prepareInputVector requires the player who
		// **made** the last move, therefore '-player':
//		double[] input = super.prepareInputVector(-player, table);
		double[] input = new double[5]; /* DUMMY */
		return input;
	}

	@Override
	public String stringRepr(double[] featVec) {
		// TODO Auto-generated method stub
		return "";
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
	
	



	
	

}
