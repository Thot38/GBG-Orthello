package Othello;

import controllers.PlayAgent;
import games.Arena;
import games.Evaluator;
import games.Feature;
import games.GameBoard;
import games.XNTupleFuncs;

public class ArenaOthello extends Arena {

	public ArenaOthello() {
		super();
	}

	public ArenaOthello(String title) {
		super(title);
	}
	
	@Override
	public String getGameName() {
		// TODO Auto-generated method stub
		return "Othello";
	}


	/**
	 * @return Factory pattern to create a new GameBoard
	 */
	@Override
	public GameBoard makeGameBoard() {
		// TODO Auto-generated method stub
		gb = new GameBoardOthello(this);
		return gb;
	}
	/**
	 * @return Factory pattern to create a new Evaluator
	 */
	@Override
	public Evaluator makeEvaluator(PlayAgent pa, GameBoard gb, int stopEval, int mode, int verbose) {
		return new EvaluatorOthello(pa,gb,stopEval,mode,verbose);
	}

	/**
	 * 
	 * @param featmode
	 * @return Factory pattern to create a new Feature
	 */
	public Feature makeFeaturClass(int featmode) {
		throw new RuntimeException("Feature not implemented for XYZ");
		}
	
	/**
	 * @return Factory pattern to create a new XNTupleFuncs
	 */
	public XNTupleFuncs makeXNTupleFuncs() {
		return new XNTupleFuncsOthello();
	}
	
	@Override
	public void performArenaDerivedTasks() {
		// TODO Auto-generated method stub
		
	}
	public static void main(String[] args) {
		ArenaOthello t_Frame = new ArenaOthello("General Board Game Playing");
		if(args.length == 0) {
			t_Frame.init();
		}else throw new RuntimeException("[Arena.main] args="+args+ "not allowed");
	}
	
}
