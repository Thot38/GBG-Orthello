package Othello;

import java.io.IOException;

import controllers.PlayAgent;
import games.ArenaTrain;
import games.Evaluator;
import games.Feature;
import games.GameBoard;
import games.XNTupleFuncs;
import games.TicTacToe.ArenaTrainTTT;
import games.TicTacToe.FeatureTTT;
import games.TicTacToe.XNTupleFuncsTTT;

public class ArenaTrainOthello extends ArenaTrain {

	public ArenaTrainOthello() {
		super();
	}
	
	public ArenaTrainOthello(String title) {
		super(title);
	}
	
	@Override
	public String getGameName() {
		return "Othello";
	}

	@Override
	public GameBoard makeGameBoard() {
		gb = new GameBoardOthello(this);
		return gb;
	}

	@Override
	public Evaluator makeEvaluator(PlayAgent pa, GameBoard gb, int stopEval, int mode, int verbose) {
		// TODO Auto-generated method stub
		return new EvaluatorOthello(pa,gb,stopEval,mode,verbose);
	}

	public Feature makeFeatureClass(int featmode) {
		throw new RuntimeException("no implemented Feature class");
	}

	public XNTupleFuncs makeXNTupleFuncs() {
		return new XNTupleFuncsOthello();
	}
	
	
	public static void main(String[] args) throws IOException 
	{
		ArenaTrainOthello t_Frame = new ArenaTrainOthello("General Board Game Playing");
		
		if (args.length==0) {
			t_Frame.init();
		} else {
			throw new RuntimeException("[ArenaTrainTTT.main] args="+args+" not allowed. Use TicTacToeBatch.");
		}
	}
}