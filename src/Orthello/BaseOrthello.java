package Orthello;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;

import controllers.AgentBase;
import controllers.PlayAgent;
import games.StateObservation;
import tools.Types.ACTIONS;
import tools.Types.ACTIONS_VT;

public class BaseOrthello extends AgentBase implements Serializable{

	public static final long serialVersionUID = 12L;
	
	/**
	 * Used to determine availableActions
	 */
	public static final Modifier[] modifier = {
			new Modifier(-1,-1), 
			new Modifier(0,-1), 
			new Modifier(0,+1),
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

	@Override
	public ACTIONS_VT getNextAction2(StateObservation sob, boolean random, boolean silent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getScore(StateObservation sob) {
		// TODO Auto-generated method stub
		return 0;
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
			for(int j = 0; j < currentGameState[0].length; j++,n++)
			{
				if(currentGameState[i][j] == 0)
					if(isLegalAction(currentGameState,i,j,player)) retVal.add(new ACTIONS(n));
 			}
		}
		return retVal;
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
			while(inBounds(i+x.x, j+x.y)){
				if(cgs[i+x.x][j+x.y] == (player * -1)) {
					flipSet.add(new Modifier(i+x.x,j+x.y));
				if(cgs[i+x.x][j+x.y] == player)	
					{
						flipping = true;
						break;
					}
				}
			}
			if(flipping)
			{
				for(Modifier y : flipSet)
				{
					cgs[x.x][x.y] = player;
				}
			}
		}	
	}
	
	
	
	
	/**
	 * TODO: SHOULD BE USED TO GET FLIPLIST LATER ON
	 * 0 = Empty 1 = white -1 = Black
	 * @param cgs currentGameState[i][j]
	 * @param i index
	 * @param j index
	 * @param player who has to place a token   -1 = Black    1 = White
	 * @return
	 */
	private static boolean isLegalAction(int[][] cgs, int i, int j, int player) {
		
		
		boolean retVal = false;
		
		int playerColor = (player == 1) ? 1 : -1; 
		int opponentColor = (player == -1) ? 1 : -1;
		for(Modifier x : modifier) {
			if(inBounds(i+x.x,j+x.y))
				if(cgs[i+x.x][j+x.y] == opponentColor) 
					return validateAction(cgs,i+x.x,j+x.y,x,playerColor);
		}
		return false;
	}
	
	
	private static boolean validateAction(int[][] cgs, int i, int j, Modifier x, int playerColor) {
		while(inBounds(i+x.x,j+x.y))
		{
			if(cgs[i+x.x][j+x.y] == playerColor) return true;
			i += x.x;
			j = x.y;
		}
		return false;
	}
	
	
	private static boolean inBounds(int row, int col)
    {
        return row >= 0 && row < ConfigOrthello.BOARD_SIZE && col >= 0 && col <  ConfigOrthello.BOARD_SIZE;
    }
	
	
}
