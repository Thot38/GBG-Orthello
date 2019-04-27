package games.Othello;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import controllers.AgentBase;
import controllers.MinimaxAgent;
import controllers.PlayAgent;
import games.StateObservation;
import tools.Types.ACTIONS;
import tools.Types.ACTIONS_ST;
import tools.Types.ACTIONS_VT;
import tools.Types.ScoreTuple;


abstract public class BaseOthello extends AgentBase implements PlayAgent, Serializable{

	
	protected Random rand;
	protected int m_depth=10;
	protected boolean m_useHashMap=false; //true;
	private HashMap<String,ScoreTuple> hm;
	private int featmode = 0;
	
	public static final long serialVersionUID = 12L;
	
	private static transient int[] heur = {100,-25,10,5,5,10,-25,-100,-25,-25,2,2,2,2,-25,-25,10,2,5
			,1,1,5,2,10,5,2,1,2,2,1,2,5,5,2,1,2,2,1,2,5,10,2,5,1,1,5,2,10,-25,-25,2,2,2,2,-25,-25,100,-25,10,5,5,10,-25,100 };

	
	
	protected BaseOthello(String name) {
		super(name);
		super.setMaxGameNum(1000);
		super.setGameNum(0);
		this.rand = new Random(System.currentTimeMillis());
		hm = new HashMap<String, ScoreTuple>();
	
	}
	
	protected BaseOthello(String name, int depth){
		super(name);
		m_depth = depth;
	}

	/**
	 * The Feature vector consist of the depending player which is activated in featureclass
	 * Therefor the optimal feature will be calculated given by a board pattern which contains the cummulative value for 
	 * the current players move. We will try to maximize the Score 
	 */
	protected double[] prepareInputVector(int[][] table, int player) {
		if( featmode >= 0)
		{
			return prepareInputVector0(table, player*-1);
		} else
		{
			throw new RuntimeException("[BaseOthello] featmode = " + featmode + " not allowed!");
		}
		
	}
	
	private double[] prepareInputVector0(int[][] table, int player)
	{
		double[] retVal = new double[64];
		for(int i = 0; i < 64; i++)
			retVal[i] = ConfigOthello.heur[i];
//		double[] input = new double[64];
//		// Mapping the current table to the gamestate and calculating the correct spot
//		int[] mappedTable = tableToVector(table);
//		int currentGameValue;
//		for(int i = 0; i < 64; i ++)
//		{
//			if( mappedTable[i] == player) {
//				input[i] = ConfigOthello.heur[i];
//			}
//		}
		
		
		
		return retVal;
	}
	

	
	
	public int getFeatmode() {
		return featmode;
	}
	
	public void setFeatmode(int f){
		featmode = f;
	}
	
	/**
	 * calculate the current state value and returns points
	 * @param vector
	 * @return
	 */
	private int getBoardStatus(int[] vector) {
		//TODO: Add HEUR PLAYER SKILL
		return 0;
	}
	
	
	/**
	 * Given a state as string, return the corresponding board position table.
	 * -1 for 'X' +1 for 'O' and 0 for '-'
	 * @param state [input] board position as string
	 * @param table [output] the corresponding board
	 */
	protected void stringToTable(String state, int[][] table)
	{
		for (int i=0, x=0;i<ConfigOthello.BOARD_SIZE;i++)
			for (int j=0;j<ConfigOthello.BOARD_SIZE;j++, x++) {
				if (state.charAt(x)=='X') table[i][j] = -1;
				else if (state.charAt(x)=='O') table[i][j] = 1;
				else table[i][j] = 0;
			}
	}
	
	
	protected String tableToString(int table[][])
	{
		String str="";
		for(int i = 0; i < ConfigOthello.BOARD_SIZE; i++)
		{
			for( int j = 0; j < ConfigOthello.BOARD_SIZE; j++)
			{
				if (table[i][j] == 1) str += "O";
				else if (table[i][j] == -1) str += "X";
				else str += "-";
			}
		}
		return str;
	}
	
	
	private int[] tableToVector(int[][] table)
	{
		int[] retVal = new int[64];
		for(int i = 0, z = 0; i < 8; i++)
		{
			for(int j = 0; j < 8; j++, z++)
			{
				retVal[z] = table[i][j];
			}
		}
		return retVal;
	}
	
	
	protected String inputToString(double[] vec)
	{
		
		
		return "";
	}
	
	
	
	
	
	
	
	
	
	/**
	 * Used to determine availableActions
	 */
	public static final Modifier[] modifier = {
			new Modifier(-1,-1), 
			new Modifier(0,-1), 
			new Modifier(+1,-1),
			new Modifier(-1,0),
			new Modifier(+1,0),
			new Modifier(-1,+1),
			new Modifier(0,+1),
			new Modifier(+1,+1)
	};

	private static class Modifier{
		int x,y;
		public Modifier(int x, int y)
		{
			this.x = x;
			this.y = y;
		}
	}

	public static void deepCopyGameState(int[][] toCopy, int[][] result)
	{
		for(int i = 0; i < toCopy.length; i++)
		{
			for(int j = 0; j < toCopy[i].length; j++)
			{
				result[i][j] = toCopy[i][j]; 
			}
		}
	}


	public static double nextState(StateObservation sob, int a, int b) {
		// TODO Auto-generated method stub
		int score = 0, player = ((StateObserverOthello) sob).getPlayer();
		int[][] currentGameState = ((StateObserverOthello) sob).getCurrentGameState();
		
		for(int i = 0, x = 0; i < ConfigOthello.BOARD_SIZE; i++) {
			for(int j = 0; j < ConfigOthello.BOARD_SIZE; j++, x++) {
				if(currentGameState[i][j] == player) score+= heur[x];
				else if(currentGameState[i][j] == player * -1) score-= heur[x];
				if(a == i && b == j) score += heur[x];
			}
		}
		return score;
	}

	/**
	 * 
	 * @param cgs  current game state
	 * @return true if no possible action is available for any player.
	 */
	public static boolean isGameOver(int[][] cgs)
	{
		if(possibleActionsTotal(cgs,1) == 0) return true;
		return false;
	}

	public static int possibleActionsTotal(int[][] currentGameState, int player)
	{
		int retVal = 0;
		for(int i = 0, n = 0; i < currentGameState.length; i++) {
			for(int j = 0; j < currentGameState[0].length; j++,n++)
			{
				if(currentGameState[i][j] == 0) {
					if(isLegalAction(currentGameState,i,j,player)) retVal++;
					if(isLegalAction(currentGameState,i,j,(player * -1))) retVal++;
				}
			}
		}
		return retVal;
	}

	/**
	 * 
	 * @param currentGameState the game state of the board
	 * @param player who has to place a token   -1 = Black    1 = White
	 * @return returns an ArrayList with all possible Actions from which can be picked from.
	 */
	public static ArrayList<ACTIONS> possibleActions(int[][] currentGameState, int player)
	{
		ArrayList<ACTIONS> retVal = new ArrayList<ACTIONS>();
		for(int i = 0, n = 0; i < currentGameState.length; i++) {
			for(int j = 0; j < currentGameState[0].length; j++, n++)
			{
				if(currentGameState[i][j] == 0) {
					if(isLegalAction(currentGameState,i,j,player)) {
						
						retVal.add(new ACTIONS(n));
					}
				}
					
			}
		}
		return retVal;
	}

	/**
	 * 0 = Empty 1 = white -1 = Black
	 * @param cgs currentGameState[i][j]
	 * @param i index
	 * @param j index
	 * @param player who has to place a token   -1 = Black    1 = White
	 * @return
	 */
	private static boolean isLegalAction(int[][] cgs, int i, int j, int player) 
	{
		int playerColor = player; 
		int opponentColor = player * -1;
		for(Modifier x : modifier) {
			int setX = (i+x.x), setY = (j+x.y);
			if(inBounds((setX),(setY))) {
				if(cgs[setX][setY] == opponentColor) {
					if(validateAction(cgs,(setX),(setY),x,playerColor)) return true;
				}
			}
		}
		return false;
	}

	/**
	 * Iterative method to find the own stone in a given row
	 * @param cgs 			current game state int[][] {-1,0,1}
	 * @param i 			index of current game state
	 * @param j				index of current game state
	 * @param x				modifier to iterate through cell.
	 * @param playerColor	integer -1 or 1 representing the actual player.
	 * @return
	 */
	private static boolean validateAction(int[][] cgs, int i, int j, Modifier x, int playerColor) 
	{
		while(inBounds(i+x.x,j+x.y))
		{
			if(cgs[(i+x.x)][(j+x.y)] == playerColor) return true;
			if(cgs[(i+x.x)][(j+x.y)] == 0) return false; 
			
			i += x.x;
			j += x.y;
		}
		return false;
	}

	/**
	 * Used to advance the game state. Checking for all modifiers (see above)
	 *  to find an opponent stone,
	 * then adding them to the flipList until an own stone has been found.
	 * @param cgs current game state
	 * @param i index
	 * @param j index
	 * @param player player 
	 */
	public static void flip(int[][] cgs, int i, int j, int player){
		HashSet<Modifier> flipSet = new HashSet<Modifier>();
		for(Modifier x : modifier) {
			flipSet.clear();
			boolean flipping = false;
			int setX = i;
			int setY = j;
			while(inBounds(setX += x.x, setY += x.y))
			{
				if(cgs[setX][setY] == 0) {
					break;
				}

				if(cgs[setX][setY] == (player * -1)) {
					flipSet.add(new Modifier(setX, setY));
				}
				if(cgs[setX][setY] == player)								
				{
					flipping = true;
					break;
				}
			}
			if(flipping)
			{
				for(Modifier y : flipSet)
				{
					cgs[y.x][y.y] = player;
				}
			}
		}	
	}

	/**
	 * @param row	index representing int[row]
	 * @param col	index representing int[x][col]
	 * @return		boolean if the given integers are in our board
	 */
	private static boolean inBounds(int row, int col)
	{
		return 0 <= row && row < ConfigOthello.BOARD_SIZE && 0 <= col && col <  ConfigOthello.BOARD_SIZE;
	}

	

	




}
