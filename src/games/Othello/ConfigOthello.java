package games.Othello;

public class ConfigOthello {

	/**
	 *  Board size
	 */
	public static final int BOARD_SIZE = 8;
	
	
	
	/**
	 * 
	 */
	public static int[] heur = {100,-25,10,5,5,10,-25,-100,-25,-25,2,2,2,2,-25,-25,10,2,5
			,1,1,5,2,10,5,2,1,2,2,1,2,5,5,2,1,2,2,1,2,5,10,2,5,1,1,5,2,10,-25,-25,2,2,2,2,-25,-25,100,-25,10,5,5,10,-25,100 };
	public static int heurMaxValue = 488;
}
