package Orthello;

import java.io.Serializable;
import java.util.HashSet;

import games.StateObservation;
import games.XNTupleFuncs;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class XNTupleFuncsOrthello implements XNTupleFuncs, Serializable {

	private int[] actionVector;	
	
	public XNTupleFuncsOrthello() {
		
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
		return ConfigOrthello.BOARD_SIZE * ConfigOrthello.BOARD_SIZE;
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
	 * 
	 */
	@Override
	public int[] getBoardVector(StateObservation so) {
		assert ( so instanceof StateObserverOrthello);
		int[][] gameState = ((StateObserverOrthello) so).getCurrentGameState();
		int[] retVal = new int[getNumCells()];
		for(int i = 0, n = 0; i < ConfigOrthello.BOARD_SIZE; i++){
			for(int j = 0; j < ConfigOrthello.BOARD_SIZE; j++, n++)
				retVal[n] = gameState[i][j];
		}
		return retVal;
	}

	@Override
	public int[][] symmetryVectors(int[] boardVector) {
		int s = 8; // 4 Symmetries archieved by rotating the board plus each rotation Mirrored
		int[][] symmetryVectors = new int[s][boardVector.length];
		
		for(int i = 1; i < 4; i++) {
			symmetryVectors[i] = rotate(symmetryVectors[i-1]);
		}
		symmetryVectors[5] = flip(symmetryVectors[0]);
		for(int j = 5; j < 8; j++)
		{
			symmetryVectors[j] = rotate(symmetryVectors[j-1]);
		}
		return symmetryVectors;
	}
	
	/**
	 * TODO: TEST
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
	 * @return
	 */
	private int[] rotate(int[] boardVector)
	{
//		int[] rotationIndex = new int[] { 56, 48, 40, 32, 24, 16, 8, 0, 57, 49, 41, 33, 25, 17, 9, 1, 58, 50, 42, 34, 26, 18, 10, 2, 59, 51, 43, 35, 27, 19, 11, 03, 
//			60, 52, 44, 36, 28, 20, 12, 4, 61, 53, 45, 37, 29, 21, 13, 5, 62, 54, 46, 38, 30, 22, 14, 6, 63, 55, 47, 39, 31, 23, 15, 7 };
		int[] result = new int[boardVector.length]; 
		int startIndex = boardVector.length - ConfigOrthello.BOARD_SIZE;
		int runningIndex = startIndex;
		for(int i = 0; i < boardVector.length; i++)
		{
			if(runningIndex - ConfigOrthello.BOARD_SIZE < 0)
			{
				runningIndex = ++startIndex;
			}
			
			result[i] = boardVector[runningIndex];
		}
		return result;
	}
	
	/**
	 * TODO
	 * @param boardVector
	 * @return
	 */
	private int[] flip(int[] boardVector)
	{
		throw new NotImplementedException();
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
