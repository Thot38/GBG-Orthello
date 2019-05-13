package games.Othello;

import java.io.Serializable;

import games.Feature;
import games.StateObservation;
import games.CFour.StateObserverC4;
import games.TicTacToe.StateObserverTTT;
import tools.Types;
import tools.Types.ACTIONS_VT;

public class FeatureOthello implements Feature, Serializable{

	int featmode;
	
	private static final long serialVersionUID = 12L;
	
	public FeatureOthello(int featmode)
	{
		this.featmode = featmode;
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
	
	/**
	 * Class {@link FeatureOthello} calculate value the value based on given lines rows and blocks.
	 * <pre>
	 * 	As a Reminder please see below the board numbers:
	 * 												row
	 * 			0	1	2	3	4	5	6	7   	0
	 * 			8	9	10	11	12	13	14	15		1
	 * 			16	17	18	19	20	21	22	23		2
	 * 			24	25	26	27	28	29	30	31 		3
	 * 			32	33	34	35	36	37	38	39		4
	 * 			40	41	42	43	44	45	46	47		5	
	 * 			48	49	50	51	52	53	54	55		6
	 * 			56	57	58	59	60	61	62	63		7
	 *
	 *  	col	0	1	2	3	4	5	6	7	
	 *  
	 *  
	 */
	public double[] prepareVector(StateObservation sob) {
		assert(sob instanceof StateObserverOthello) : "sob not instance of StateObserverOthello";
		StateObserverOthello so = (StateObserverOthello)sob;
		return createFeatureVector(getOpponent(so.getPlayer()), so.getCurrentGameState());
	}
	/**
	 * Adding the value to all possible feature vectors additionally there are blocks
	 * @param player
	 * @param table
	 * @return
	 */
	public double[] createFeatureVector(int player, int table[][]) {
		return null;
	}
	
	public void isunChangeable(int[][] table)
	{
		double[] retVal = new double[2];
		// corner check
		int player= (table[0][0] == 1) ? 1 : (table[0][0] == 2) ? 2 : 0;
		if( player != 0);
			
	}
	
	/**
	 * looking for any tokens, which are not changeable
	 * @param vector the vector, which has to become evaluated
	 * @param player		player, who made the turn
	 * @param calledFrom	
	 * 
	 * @return
	 */
	private void getStatus(double[] input,int[] vector, int player)
	{
		int score = 0;
		for(int i = 0; i < input.length; i++)
		{
			if(input[i] == player) score++;
			else if( input[i] == getOpponent(player)) score--;
		}
		input[score-1]++;
	}
	
	private int getOpponent(int player)
	{
		return player == 1 ? 2 : 1;
	}
	
	/**
	 * The 'raw' feature vector for featmode == ? <ul>
	 * <li> n0 ... n63 = the raw board position (afterstate </li>
	 * </ul>
	 * @param player	player who made the last move either +1 or -1
	 * @param table		the board position
	 * @return double[]	the raw feature vector
	 */
	private double[] prepareInputVector9(int player, int table[][]) {
		double[] retVal = new double[64];
		for(int i=0, z=0; i < 8; i++) {
			for(int j=0; j < 8; j++, z++) {
				retVal[z] = table[i][j];
			}
		}
		return retVal;
	
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
