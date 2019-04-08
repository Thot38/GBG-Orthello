package Orthello;

import java.io.Serializable;
import java.util.HashSet;

import games.StateObservation;
import games.XNTupleFuncs;

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
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] symmetryActions(int actionKey) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[][] fixedNTuples(int mode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String fixedTooltipString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] getAvailFixedNTupleModes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HashSet adjacencySet(int iCell) {
		// TODO Auto-generated method stub
		return null;
	}

}
