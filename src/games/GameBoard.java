package games;

import games.StateObservation;
import controllers.PlayAgent;
import games.Arena;
import games.RubiksCube.CubeConfig;
import games.RubiksCube.GameBoardCube;

/**
 * Each class implementing interface GameBoard has the board game GUI. 
 * It shows board game states, optionally the values of possible next actions,
 * and allows user interactions with the board to enter legal moves during 
 * game play or to enter board positions for which the agent reaction is 
 * inspected. 
 * <p>
 * {@link GameBoard} has an internal object derived from {@link StateObservation} which represents the 
 * current game state. This game state can be retrieved (getStateObs()), 
 * reset-retrieved (getDefaultStartState()) , or a random start state can be retrieved 
 * with {@link #chooseStartState(PlayAgent)}.  
 * 
 * @author Wolfgang Konen, TH K�ln, Nov'16
 *
 */
public interface GameBoard {
	/**
	 * things to be initialized prior to starting a training 
	 */
	public void initialize();
	public void clearBoard(boolean boardClear, boolean vClear);
	//public void updateBoard();
	//public void updateBoard(StateObservation so);
	/**
	 * Update the play board and the associated values (labels).
	 * 
	 * @param so	the game state
	 * @param withReset  if true, reset the board prior to updating it to state so
	 * @param showValueOnGameboard	if true, show the game values for the available actions
	 * 				(only if they are stored in state {@code so}).
	 */
	public void updateBoard(StateObservation so, boolean withReset
			, boolean showValueOnGameboard);
	public void showGameBoard(Arena arena,boolean alignToMain);
	public void toFront();
	/**
	 * Is an action requested from Arena (i.e. was human interaction done)?
	 * @return true if action from Arena is requested 
	 */
	public boolean isActionReq();
	public void setActionReq(boolean actionReq);
	public void enableInteraction(boolean enable);
	public StateObservation getStateObs();
	
	/**
	 * If logs and agents should be placed in a subdirectory (e.g. Hex: BoardSize), then
	 * this method returns a suitable string. If it returns {@code null}, then logs and 
	 * agents are placed in the {@code gameName} directory directly. 
	 * @return subdir string
	 */
	public String getSubDir();
	
	public Arena getArena();
	
	/**
	 * @return the 'empty-board' start state
	 */
	public StateObservation getDefaultStartState();
	
	/**
	 * Choose a random start state. Used when training an agent via self-play.
	 * 
	 * @param pa the agent to be trained, we need it (currently only in Rubik's Cube) 
	 * 			for its {@link PlayAgent#getGameNum()} and {@link PlayAgent#getMaxGameNum()}
	 * 			in order to get the number of training games (this influences the returned
	 * 			state).
	 * @return a) for 2-player games: a start state which is with probability 0.5 the empty board 
	 * 		and with probability 0.5 one of the possible one-ply successors. <br>
	 *   b) for RubiksCube: a random start state which is p twists away from the solved cube. 
	 *      Which p from {1,...,{@link CubeConfig#pMax}} depends on the proportion of training games conducted.
	 *   
	 * @see #chooseStartState()     
	 * @see GameBoardCube    
	 */
	public StateObservation chooseStartState(PlayAgent pa);

	/**
	 * Choose a random start state when playing a game.
	 * 
	 * @return a) for 2-player games: a start state which is with probability 0.5 the empty board 
	 * 		and with probability 0.5 one of the possible one-ply successors. <br>
	 *   b) for RubiksCube: a random start state which is p twists away from the solved cube. 
	 *      p is picked randomly from {1,...,{@link CubeConfig#pMax}}.
	 *      
	 * @see Arena#PlayGame()
	 */
	public StateObservation chooseStartState();
}
