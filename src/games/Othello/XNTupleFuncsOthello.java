package games.Othello;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import games.StateObservation;
import games.XNTupleFuncs;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class XNTupleFuncsOthello implements XNTupleFuncs, Serializable {

	private int[] actionVector;	
	
	public XNTupleFuncsOthello() {
		
	}
	
	
	private void calcActionVector() {
		for(int i = 0; i < getNumCells(); i++)
		{
			
		}
	}
	
	/**
	 * @return integer of total board cells
	 */
	@Override
	public int getNumCells() {
		return ConfigOthello.BOARD_SIZE * ConfigOthello.BOARD_SIZE;
	}

	@Override
	public int getNumPositionValues() {
		return 3;
	}

	@Override
	public int getNumPlayers() {
		return 2;
	}
	/**
	 * The board vector is an {@code int[]} vector where each entry corresponds to one 
	 * cell of the board. In the case of Othello the mapping is
	 * <pre>
	 * 00 01 02 03 04 05 06 07			
	 * 08 09 10 11 12 13 14 15			
	 * 16 17 18 19 20 21 22 23			
	 * 24 25 26 27 28 29 30 31 
	 * 32 33 34 35 36 37 38 39	
	 * 40 41 42 43 44 45 46 47	
	 * 48 49 50 51 52 53 54 55
	 * 56 57 58 59 60 61 62 63
	 * </pre>
	 * @param The StateObservation of the current game state
	 * @return a vector of length {@link #getNumCells()}, holding for each board cell its 
	 * position value with 0 = empty , 1 = white, -1 = black.
	 */
	@Override
	public int[] getBoardVector(StateObservation so) {
		assert ( so instanceof StateObserverOthello);
		int[][] gameState = ((StateObserverOthello) so).getCurrentGameState();
		int[] retVal = new int[getNumCells()];
		for(int i = 0, n = 0; i < ConfigOthello.BOARD_SIZE; i++){
			for(int j = 0; j < ConfigOthello.BOARD_SIZE; j++, n++)
				retVal[n] = gameState[i][j];
		}
		return retVal;
	}

	/**
	 * Given a board vector from {@link #getBoardVector(StateObservation)} and given that the 
	 * game has s symmetries, return an array which holds s symmetric board vectors: <ul>
	 * <li> the first row {@code boardArray[0]} is the board vector itself
	 * <li> the other rows are the board vectors when transforming {@code boardVector}
	 * 		according to the s-1 other symmetries (e. g. rotation, reflection, if applicable).
	 * </ul>
	 * In the case of Othello we have s=8 symmetries (4 board rotations * 2 board flips)
	 * 
	 * @param boardVector
	 * @return boardArray
	 */
	@Override
	public int[][] symmetryVectors(int[] boardVector) {
		int s = 8; // 4 Symmetries archieved by rotating the board plus each rotation Mirrored
		int[][] symmetryVectors = new int[s][boardVector.length];
		symmetryVectors[0] = boardVector;
		
		for(int i = 1; i < 4; i++) {
			symmetryVectors[i] = rotate(symmetryVectors[i-1]);
		}
		symmetryVectors[4] = flip(symmetryVectors[0]);
		for(int j = 5; j < 8; j++)
		{
			symmetryVectors[j] = rotate(symmetryVectors[j-1]);
		}
		return symmetryVectors;
	}
	
	/**
	 * Helper function for  {@link #symmetryVectors(int[])}: 
	 * Rotates the given boardVector 90 degrees clockwise
	 * 
	 * <pre>
	 * 
	 * 00 01 02 03 04 05 06 07			56 48 40 32 24 16 08 00
	 * 08 09 10 11 12 13 14 15			57 49 41 33 25 17 09 01
	 * 16 17 18 19 20 21 22 23			58 50 42 34 26 18 10 02
	 * 24 25 26 27 28 29 30 31  ---> 	59 51 43 35 27 19 11 03
	 * 32 33 34 35 36 37 38 39			60 52 44 36 28 20 12 04
	 * 40 41 42 43 44 45 46 47			61 53 45 37 29 21 13 05
	 * 48 49 50 51 52 53 54 55			62 54 46 38 30 22 14 06
	 * 56 57 58 59 60 61 62 63			63 55 47 39 31 23 15 07
	 * 
	 * </pre>
	 * @param boardVector
	 * @return rotatedBoard
	 */
	private int[] rotate(int[] boardVector)
	{	    
//		int[] rotationIndex = new int[] { 56, 48, 40, 32, 24, 16, 8, 0, 57, 49, 41, 33, 25, 17, 9, 1, 58, 50, 42, 34, 26, 18, 10, 2, 59, 51, 43, 35, 27, 19, 11, 03, 
//			60, 52, 44, 36, 28, 20, 12, 4, 61, 53, 45, 37, 29, 21, 13, 5, 62, 54, 46, 38, 30, 22, 14, 6, 63, 55, 47, 39, 31, 23, 15, 7 };

		int[] result = new int[boardVector.length];
		for(int i = 0; i < ConfigOthello.BOARD_SIZE; i++)
		{
			for(int j = 0; j < ConfigOthello.BOARD_SIZE; j++)
			{
				int oldPosition = i * ConfigOthello.BOARD_SIZE + j;
				int newPosition = (ConfigOthello.BOARD_SIZE * ConfigOthello.BOARD_SIZE - ConfigOthello.BOARD_SIZE) + i - (j * ConfigOthello.BOARD_SIZE);
				
				result[newPosition] = boardVector[oldPosition];
			}	
		}
		for(int i = 0; i < result.length / 2; i++)
		{
			int temp = result[i];
			result[i] = result[result.length - 1 - i];
			result[result.length - 1 - i] = temp;
		}
		
		return result;
		
	}
	
	/**
	 * Helper function for  {@link #symmetryVectors(int[])}: 
	 * Mirrors the board along its diagonal from top left to bottom right
	 * 
	 * <pre>
	 * 
	 * 00 01 02 03 04 05 06 07			00 08 16 24 32 40 48 56
	 * 08 09 10 11 12 13 14 15			01 09 17 25 32 41 49 57
	 * 16 17 18 19 20 21 22 23			02 10 18 26 33 42 50 58
	 * 24 25 26 27 28 29 30 31  ---> 	03 11 19 27 34 43 51 59
	 * 32 33 34 35 36 37 38 39			04 12 20 28 35 44 52 60
	 * 40 41 42 43 44 45 46 47			05 13 21 29 36 45 53 61
	 * 48 49 50 51 52 53 54 55			06 14 22 30 37 46 54 62
	 * 56 57 58 59 60 61 62 63			07 15 23 31 38 47 55 63
	 * 
	 * </pre>
	 * @param boardVector
	 * @return
	 */
	private int[] flip(int[] boardVector)
	{
		int[] result = new int[boardVector.length];
		for(int i = 0; i < ConfigOthello.BOARD_SIZE; i++)
		{
			for(int j = 0; j < ConfigOthello.BOARD_SIZE; j++)
			{
				int oldPosition = i * ConfigOthello.BOARD_SIZE + j;
				int newPosition = j * ConfigOthello.BOARD_SIZE + i;
				
				result[newPosition] = boardVector[oldPosition];
			}	
		}
		return result;
	}

	@Override
	public int[] symmetryActions(int actionKey) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[][] fixedNTuples(int mode) {
		// TODO Auto-generated method stub
		int[][] nTuple = {{19,20,27,28}};
		return nTuple;
	}

	@Override
	public String fixedTooltipString() {
		// TODO Auto-generated method stub
		return "<html>"
				+ "1: TODO"
				+ "</html>";
	}

	@Override
	public int[] getAvailFixedNTupleModes() {
		// TODO Auto-generated method stub
		return fixedModes;
	}

	@Override
	public HashSet adjacencySet(int iCell) {
		// TODO Auto-generated method stub
		return null;
	}
	
	 private static int[] fixedModes = {1};
		


}
