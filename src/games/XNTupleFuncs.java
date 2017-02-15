package games;

public interface XNTupleFuncs {

	//
	// The following four functions are only required for the n-tuple interface.
	// If an implementing class does not need that part, it may just code stubs 
	// returning 0, {@code null}, or throwing a RuntimeException.
	//
	/**
	 * @return the number of board cells
	 */
	public int getNumCells();
	
	/**
	 * @return the number P of position values 0, 1, 2,..., P-1 that each board cell 
	 * can have (e. g. P=3 for TicTacToe with 0:"O", 1=empty, 2="X") 
	 */
	public int getNumPositionValues();
	
	/**
	 * @return a vector of length {@link #getNumCells()}, holding for each board cell its 
	 * position value 0, 1, 2,..., P-1.
	 */
	public int[] getBoardVector(StateObservation so);

	/**
	 * Given a board vector from {@link #getBoardVector()} and given that the game has 
	 * s symmetries, return an array which holds s symmetric board vectors: <ul>
	 * <li> the first row {@code boardArray[0]} is the board vector itself
	 * <li> the other rows are the board vectors when transforming {@code boardVector}
	 * 		according to the s-1 other symmetries (e. g. rotation, reflection, if applicable).
	 * </ul>
	 * @param boardVector
	 * @return boardArray
	 */
	public int[][] symmetryVectors(int[] boardVector);
	
	/** 
	 * Return a fixed set of {@code numTuples} n-tuples suitable for that game. 
	 * Different n-tuples may have different length. An n-tuple {0,1,4} means a 3-tuple 
	 * containing the cells 0, 1, and 4.
	 * 
	 * @return nTuples[numTuples][]
	 */
	public int[][] fixedNTuples();

}