package Orthello;

import java.util.ArrayList;

import games.ObserverBase;
import games.StateObservation;
import tools.Types;
import tools.Types.ACTIONS;
import tools.Types.WINNER;

public class StateObserverOrthello extends ObserverBase{

	
	
	public static final long serialVersionUID = 12L;
	private static final double REWARD_NEGATIVE = -1, REWARD_POSITIVE = 1;
	
	private int[][] currentGameState;
	private int playerNextMove;
	private ArrayList<ACTIONS> availableActions = new ArrayList<ACTIONS>();
	
	public StateObserverOrthello()
	{
		currentGameState= new int[ConfigOrthello.BOARD_SIZE][ConfigOrthello.BOARD_SIZE];
		currentGameState[3][3] = 1;
		currentGameState[3][4] = -1;
		currentGameState[4][3] = -1;
		currentGameState[4][4] = 1;
		playerNextMove = 1;
		setAvailableActions();
	}
	
	public StateObserverOrthello(int[][] gameState, int playerMove)
	{
		currentGameState= new int[ConfigOrthello.BOARD_SIZE][ConfigOrthello.BOARD_SIZE];
		playerNextMove = playerMove;
		BaseOrthello.deepCopyGameState(gameState, currentGameState);
		setAvailableActions();
	}
	
	@Override
	public StateObservation copy() {
		return new StateObserverOrthello(currentGameState,playerNextMove);
	}
	
	@Override
	public boolean isGameOver() {
		return BaseOrthello.isGameOver(currentGameState);
	}

	@Override
	public boolean isDeterministicGame() {
		return true;
	}

	@Override
	public boolean isFinalRewardGame() {
		return true;
	}

	@Override
	public boolean isLegalState() {
		return true;
	}

	@Override
	public WINNER getGameWinner() {
		assert isGameOver() :"Game isn't over";
		int countPlayer = 0, countOpponent = 0;
		for(int i = 0; i < ConfigOrthello.BOARD_SIZE; i++)
		{
			for( int j = 0; j < ConfigOrthello.BOARD_SIZE; j++)
			{
				if(currentGameState[i][j] == playerNextMove) countPlayer++;
				if(currentGameState[i][j] == (playerNextMove * -1)) countOpponent++;
			}
		}
		if(countPlayer > countOpponent) return WINNER.PLAYER_WINS;
		return WINNER.PLAYER_LOSES;
	}

	@Override
	public double getMinGameScore() {
		return 0;
	}

	@Override
	public double getMaxGameScore() {
		return 64;
	}

	@Override
	public String getName() {
		return "Othello";
	}

	/**
	 *
	 * @return the afterState for deterministic games is the same.
	 */
	@Override
	public StateObservation getPrecedingAfterstate() {
			return this;
	}

	@Override
	public int getNumAvailableActions() {
		// TODO Auto-generated method stub
		return availableActions.size();
	}

	@Override
	public void setAvailableActions() {
		// TODO Auto-generated method stub
		availableActions = BaseOrthello.possibleActions(currentGameState, playerNextMove);
	}

	@Override
	public ACTIONS getAction(int i) {
		return availableActions.get(i);
	}

	@Override
	public ArrayList<ACTIONS> getAvailableActions() {
		return availableActions;
	}

	public boolean isLegalAction(ACTIONS act)
	{
		return availableActions.contains(act) ? true : false;
	}
	
	/**
	 * Changing the game state with a valid Action
	 */
	@Override
	public void advance(ACTIONS action) {
		// TODO Auto-generated method stub
		int iAction = action.toInt();
		int j = iAction % ConfigOrthello.BOARD_SIZE;
		int i = (iAction-j) / ConfigOrthello.BOARD_SIZE;
		BaseOrthello.flip(currentGameState, i, j, playerNextMove);
		currentGameState[i][j] = playerNextMove;
		playerNextMove *= -1;
		setAvailableActions();
		super.incrementMoveCounter();
	}

	@Override
	public int getPlayer() {
		return playerNextMove;
	}

	@Override
	public int getNumPlayers() {
		return 2;
	}

	@Override
	public double getGameScore(StateObservation referringState) {
//TODO: implement
		int retVal = (referringState.getPlayer() == this.playerNextMove) ? 1 : (-1);
		if(this.getGameWinner() == WINNER.PLAYER_WINS) return retVal * REWARD_POSITIVE;
		return retVal * REWARD_NEGATIVE;
	
	
	}

	@Override
	public String stringDescr() {
		String sout = "";
		for(int i = 0; i < ConfigOrthello.BOARD_SIZE; i++) {
			for(int j = 0; j < ConfigOrthello.BOARD_SIZE; j++) {
				sout += Integer.toString(currentGameState[i][j]);
			}
		}
		return sout;
	}

	public int[][] getCurrentGameState(){return currentGameState;}
	
	
	
}
