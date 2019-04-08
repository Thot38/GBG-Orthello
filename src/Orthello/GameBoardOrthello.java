package Orthello;

import java.util.Random;



import controllers.PlayAgent;
import games.Arena;
import games.GameBoard;
import games.StateObservation;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class GameBoardOrthello extends Application implements GameBoard {

	
	protected Arena m_Arena;
	private StateObserverOrthello m_so;
	private int[][] gameState;
	private double[][] vGameState;
	
	protected Random rand;
	
	protected Button[][] board;
	private boolean arenaActReq = false;
	
	
	
	
	public GameBoardOrthello() {
		
	}
	
	public GameBoardOrthello(Arena arena) {
		initGameBoard(arena);
	}
	
	
	public void initGameBoard(Arena arena)
	{
		m_Arena = arena;
		board = new Button[ConfigOrthello.BOARD_SIZE][ConfigOrthello.BOARD_SIZE];
		gameState = new int[ConfigOrthello.BOARD_SIZE][ConfigOrthello.BOARD_SIZE];
		vGameState = new double[ConfigOrthello.BOARD_SIZE][ConfigOrthello.BOARD_SIZE];
		m_so = new StateObserverOrthello();
		rand = new Random(System.currentTimeMillis());
	}
	
	
	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearBoard(boolean boardClear, boolean vClear) {
		// TODO Auto-generated method stub
		if(boardClear) {
			m_so = new StateObserverOrthello();
			for(int i = 0; i < ConfigOrthello.BOARD_SIZE; i++)
			{
				for(int j = 0; j < ConfigOrthello.BOARD_SIZE; j++)
				{
					//TODO: Empty board
				}
			}
		}
		if(vClear) {
			vGameState = new double[ConfigOrthello.BOARD_SIZE][ConfigOrthello.BOARD_SIZE]
			for(int i = 0; i < ConfigOrthello.BOARD_SIZE; i++)
			{
				for(int j = 0; j < ConfigOrthello.BOARD_SIZE; j++)
				{
					//TODO: Empty board
					vGameState = Double.NaN;
				}
			}
		}
		
	}

	@Override
	public void updateBoard(StateObservation so, boolean withReset, boolean showValueOnGameboard) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showGameBoard(Arena arena, boolean alignToMain) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void toFront() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isActionReq() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setActionReq(boolean actionReq) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void enableInteraction(boolean enable) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public StateObservation getStateObs() {
		// TODO Auto-generated method stub
		return m_so;
	}

	@Override
	public String getSubDir() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Arena getArena() {
		// TODO Auto-generated method stub
		return m_Arena;
	}

	@Override
	public StateObservation getDefaultStartState() {
		// TODO Auto-generated method stub
		//Clear oard;
		return m_so;
	}

	@Override
	public StateObservation chooseStartState(PlayAgent pa) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StateObservation chooseStartState() {
		// TODO Auto-generated method stub
		clearBoard(true,true);
		return
		
	}


	@Override
	public void start(Stage primaryStage) throws Exception {
		// TODO Auto-generated method stub
		primaryStage.setTitle("Othello");
		Scene scene = new Scene(new BorderPane());
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	
}
