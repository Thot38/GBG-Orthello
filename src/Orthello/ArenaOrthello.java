package Orthello;

import controllers.PlayAgent;
import games.Arena;
import games.Evaluator;
import games.Feature;
import games.GameBoard;
import games.XNTupleFuncs;

public class ArenaOrthello extends Arena {

	public ArenaOrthello() {
		super();
	}

	public ArenaOrthello(String title) {
		super(title);
	}
	
	@Override
	public String getGameName() {
		// TODO Auto-generated method stub
		return "Othello";
	}


	/**
	 * @return Factory pattern method to create a new GameBoard
	 */
	@Override
	public GameBoard makeGameBoard() {
		// TODO Auto-generated method stub
		gb = new GameBoardOrthello(this);
		return gb;
	}

	@Override
	public Evaluator makeEvaluator(PlayAgent pa, GameBoard gb, int stopEval, int mode, int verbose) {
		return new EvaluatorOrthello(pa,stopEval,mode);
	}

	public Feature makeFeaturClass(int featmode) {
		throw new RuntimeException("Feature not implemented for XYZ");
		}
	
	public XNTupleFuncs makeXNTupleFuncs() {
		return new XNTupleFuncsOrthello();
	}
	
	@Override
	public void performArenaDerivedTasks() {
		// TODO Auto-generated method stub
		
	}
	public static void main(String[] args) {
		ArenaOrthello t_Frame = new ArenaOrthello("General Board Game Playing");
		if(args.length == 0) {
			t_Frame.init();
		}else throw new RuntimeException("[Arena.main] args="+args+ "not allowed");
	}
	
}
