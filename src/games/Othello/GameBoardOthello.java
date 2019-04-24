package games.Othello;

import java.awt.BorderLayout;
import java.awt.Color;
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


public class GameBoardOthello extends JFrame implements GameBoard {

	/**
	 * SerialNumber
	 */
	private static final long serialVersionUID = 12L;
	
	/**
	 * Game Attributes
	 */
	protected Arena m_Arena;
	private StateObserverOthello m_so;
	private int[][] gameState;  // 1 = White   -1 = Black
	private double[][] vGameState;
	private boolean arenaActReq = false;
	protected Random rand;
	
	/**
	 * Gui Stuff
	 */
	private JLabel leftInfoTurn, leftInfoBlack, leftInfoWhite;
	private JPanel boardPanel;
	protected JButton[][] board;
	private JButton restart;
	private JLabel winner;
	
	private int counterWhite, counterBlack;

	
	public GameBoardOthello()
	{
	}
	
	public GameBoardOthello(Arena arena)
	{
		super("Othello");
		initGameBoard(arena);
		setSize(1000, 1000);
	}
	
	public void initGameBoard(Arena arena)
	{
		m_Arena = arena;
		board = new JButton[ConfigOthello.BOARD_SIZE][ConfigOthello.BOARD_SIZE];
		gameState = new int[ConfigOthello.BOARD_SIZE][ConfigOthello.BOARD_SIZE];
		vGameState = new double[ConfigOthello.BOARD_SIZE][ConfigOthello.BOARD_SIZE];
		m_so = new StateObserverOthello();
		rand = new Random();
		// LeftInfo Block
		leftInfoTurn= new JLabel("");
		leftInfoBlack = new JLabel("White: 2");
		leftInfoWhite = new JLabel("Black: 2");
		JPanel leftInfo = new JPanel();
		leftInfo.setLayout(new GridLayout(3,0));
		leftInfo.add(leftInfoTurn);
		leftInfo.add(leftInfoWhite);
		leftInfo.add(leftInfoBlack);
		
		//rightInfo
		restart = new JButton("Restart");
		restart.setPreferredSize(new Dimension(50,50));
		restart.addActionListener(
				new ActionHandler() {
					public void actionPerformed(ActionEvent e) {
						clearBoard(true,true);
						}
					});
		winner = new JLabel();
		JPanel bottomInfo = new JPanel();
		bottomInfo.setLayout(new GridLayout(0,4));
		bottomInfo.add(new JLabel("")); //Added Dummy for space
		bottomInfo.add(new JLabel("")); //Added Dummy for space
		bottomInfo.add(winner);
		bottomInfo.add(restart);
		// Game Block
		boardPanel = initBoard();
		//Main Frame Block
		setLayout(new BorderLayout(0,4));
		add(boardPanel, BorderLayout.CENTER);
		add(leftInfo, BorderLayout.WEST);
		add(bottomInfo, BorderLayout.SOUTH);
		setSize(1000, 1000);
		pack();
		setVisible(true);
		updateBoard(m_so, false, false);
	}
	
	private JPanel initBoard()
	{
		JPanel retVal = new JPanel();
		retVal.setLayout(new GridLayout(ConfigOthello.BOARD_SIZE,ConfigOthello.BOARD_SIZE,2,2));
		Dimension minSize = new Dimension(20,20);
		for(int i = 0; i < ConfigOthello.BOARD_SIZE; i++)
		{
			for(int j = 0; j < ConfigOthello.BOARD_SIZE; j++)
			{
				JButton dummy = new JButton();
				dummy.setMargin(new Insets(0,0,0,0));
				dummy.setPreferredSize(minSize); 
				dummy.addActionListener(
						new ActionHandler(i,j) {
							public void actionPerformed(ActionEvent e) {
								Arena.Task aTaskState = m_Arena.taskState;
								if(aTaskState == Arena.Task.PLAY) {
									HGameMove(i,j); // Human play
									//TODO: Adding for other
								}else if( aTaskState == Arena.Task.INSPECTV) {
									//TODO: Arena taskstate 
								}
							}
						}
				);
				board[i][j] = dummy;
				retVal.add(board[i][j]);
			}
		}
		updateCells();
		return retVal;
	}
	
	public void HGameMove(int x, int y) {
		int iAction = ConfigOthello.BOARD_SIZE * x + y;
		//TODO: MAY CHANGE
		Types.ACTIONS act = Types.ACTIONS.fromInt(iAction);
		if( m_so.isLegalAction(act)) {			
			m_so.advance(act);
			(m_Arena.getLogManager()).addLogEntry(act, m_so, m_Arena.getLogSessionID());
			arenaActReq = true;	
			updateBoard(m_so, false, false);
		}
		else {
			System.out.println("Not Allowed: illegal Action");
		}
	}
	
	
	/**
	 * Updating the cell's color and text.
	 * Updating the tokenCounters for both player.
	 */
	private void updateCells()
	{
		counterBlack = 0;
		counterWhite = 0;
		for(int i = 0; i < board.length; i++)
		{
			for(int j = 0; j < board[i].length; j++)
			{
				if(m_so.getCurrentGameState()[i][j] == 1) {
					board[i][j].setText("White");
					board[i][j].setForeground(Color.BLACK);
					board[i][j].setBackground(Color.WHITE);
					counterWhite++;
				}
				else if(m_so.getCurrentGameState()[i][j] == -1) {
					board[i][j].setText("Black");
					board[i][j].setForeground(Color.WHITE);
					board[i][j].setBackground(Color.BLACK);
					counterBlack++;
				}
				else {
					board[i][j].setText("Empty");
					board[i][j].setForeground(Color.GREEN);
					board[i][j].setBackground(Color.GREEN);
				}
			}
		}
		leftInfoWhite.setText("White: " + counterWhite);
		leftInfoBlack.setText("Black: " + counterBlack);
	}
	
	@Override
	public void initialize() {
		// TODO Auto-generated method stub
	}

	@Override
	public void clearBoard(boolean boardClear, boolean vClear) {
		// TODO Auto-generated method stub
		if(boardClear) {
			m_so = new StateObserverOthello();
			for(int i = 0; i < ConfigOthello.BOARD_SIZE; i++)
			{
				for(int j = 0; j < ConfigOthello.BOARD_SIZE; j++)
				{
					if(m_so.getCurrentGameState()[i][j] == 1) board[i][j].setText("White");
					else if(m_so.getCurrentGameState()[i][j] == -1) board[i][j].setText("Black");
					else board[i][j].setText("Empty");
				}
			}
			
		}
		if(vClear) {
			vGameState = new double[ConfigOthello.BOARD_SIZE][ConfigOthello.BOARD_SIZE];
			for(int i = 0; i < ConfigOthello.BOARD_SIZE; i++){
				for(int j = 0; j < ConfigOthello.BOARD_SIZE; j++){
					vGameState[i][j] = Double.NaN;
				}
			}
		}
		updateCells();
	}

	//TODO: implement reset and Value
	@Override
	public void updateBoard(StateObservation so, boolean withReset, boolean showValueOnGameboard) {
		if(so != null) {
			assert ( so instanceof StateObserverOthello) : "so is not an instance of StateOberverOthello";
			m_so = (StateObserverOthello) so.copy();
			updateCells();
			int player=Types.PLAYER_PM[m_so.getPlayer()];
			switch(player) 
			{
			case(-1):
				leftInfoTurn.setText("White has to move");
				break;
			case(1): 
				leftInfoTurn.setText("Black has to move");
				break;
			}
			System.out.println(m_so.isGameOver());
			if(m_so.isGameOver()) 
			{
				if(counterBlack > counterWhite) winner.setText("Black won: " + counterBlack + " to " + counterWhite);
				else if(counterBlack < counterWhite) winner.setText("White won: " + counterWhite + " to " + counterBlack);
				else winner.setText("Tie: " + counterWhite + " to " + counterBlack);
				
			}
			
		}
	}

	@Override
	public void showGameBoard(Arena arena, boolean alignToMain) {
		// TODO Auto-generated method stub
		this.repaint();
	}

	@Override
	public void toFront() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isActionReq() {
		// TODO Auto-generated method stub
		return arenaActReq;
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
		clearBoard(true,true);
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
		ActionHandler(){
			
		}
		public void actionPerformed(ActionEvent e) {}
	}

	
}
