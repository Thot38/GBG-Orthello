package Orthello;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import controllers.PlayAgent;
import games.Arena;
import games.GameBoard;
import games.StateObservation;
import tools.Types;


public class GameBoardOrthello extends JFrame implements GameBoard {

	
	protected Arena m_Arena;
	private StateObserverOrthello m_so;
	private int[][] gameState;  // 1 = White   -1 = Black
	private double[][] vGameState;
	
	protected Random rand;
	
	private JLabel leftInfo= new JLabel(""), rightInfo = new JLabel("");
	
	private JPanel boardPanel;
	protected JButton[][] board;
	private boolean arenaActReq = false;
	
	
	
	
	public GameBoardOrthello() {
	}
	
	public GameBoardOrthello(Arena arena) {
		super("Othello");
		initGameBoard(arena);
	}
	
	
	public void initGameBoard(Arena arena)
	{
		m_Arena = arena;
		board = new JButton[ConfigOrthello.BOARD_SIZE][ConfigOrthello.BOARD_SIZE];
		gameState = new int[ConfigOrthello.BOARD_SIZE][ConfigOrthello.BOARD_SIZE];
		vGameState = new double[ConfigOrthello.BOARD_SIZE][ConfigOrthello.BOARD_SIZE];
		m_so = new StateObserverOrthello();
		rand = new Random(System.currentTimeMillis());
		
		boardPanel = initBoard();
		setLayout(new BorderLayout(1,0));
		add(boardPanel, BorderLayout.CENTER);
		pack();
		setVisible(true);
	}
	
	
	private JPanel initBoard()
	{
		JPanel retVal = new JPanel();
		retVal.setLayout(new GridLayout(ConfigOrthello.BOARD_SIZE,ConfigOrthello.BOARD_SIZE,2,2));
		Dimension minSize = new Dimension(20,20);
		for(int i = 0; i < ConfigOrthello.BOARD_SIZE; i++)
		{
			for(int j = 0; j < ConfigOrthello.BOARD_SIZE; j++)
			{
				if(m_so.getCurrentGameState()[i][j] == 1) board[i][j] = new JButton("White");
				else if(m_so.getCurrentGameState()[i][j] == -1) board[i][j] = new JButton("Black");
				else board[i][j] = new JButton("Empty");
				board[i][j].setMargin(new Insets(0,0,0,0));
				board[i][j].setPreferredSize(minSize); 
				board[i][j].addActionListener(
						new ActionHandler(i,j) {
							public void actionPerformed(ActionEvent e) {
								Arena.Task aTaskState = m_Arena.taskState;
								if(aTaskState == Arena.Task.PLAY) {
									HGameMove(i,j); // Human play
									//TODO: Adding for other
								}
							}
						}
				);
				retVal.add(board[i][j]);
			}
		}
		return retVal;
	}
	
	public void HGameMove(int x, int y) {
		int iAction = ConfigOrthello.BOARD_SIZE * x + y;
		//TODO: MAY CHANGE
		Types.ACTIONS act = Types.ACTIONS.fromInt(iAction);
		assert m_so.isLegalAction(act) : "Not Allowed: illegal Action";
		m_so.advance(act);
		(m_Arena.getLogManager()).addLogEntry(act, m_so, m_Arena.getLogSessionID());
		arenaActReq = true;	
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
					board[i][j].setText(" ");
				}
			}
		}
		if(vClear) {
			vGameState = new double[ConfigOrthello.BOARD_SIZE][ConfigOrthello.BOARD_SIZE];
			for(int i = 0; i < ConfigOrthello.BOARD_SIZE; i++){
				for(int j = 0; j < ConfigOrthello.BOARD_SIZE; j++){
					vGameState[i][j] = Double.NaN;
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
		arenaActReq=actionReq;
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
		return chooseStartState();
	}

	@Override
	public StateObservation chooseStartState() {
		// TODO Auto-generated method stub
		clearBoard(true,true);
		return m_so;
	}
	
	class ActionHandler implements ActionListener
	{
		int i, j;
		ActionHandler(int i, int j){
			this.i = i;
			this.j = j;
		}
		public void actionPerformed(ActionEvent e) {}
	}

	
}
