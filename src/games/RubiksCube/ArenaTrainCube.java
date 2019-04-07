package games.RubiksCube;

import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JFrame;

import controllers.PlayAgent;
import games.Arena;
import games.Evaluator;
import games.Feature;
import games.GameBoard;
import games.XNTupleFuncs;
import games.ArenaTrain;

/**
 * {@link ArenaTrain} for Rubik's Cube. It borrows all functionality
 * from the general class {@link ArenaTrain} derived from {@link Arena}. It only overrides 
 * the abstract methods <ul>
 * <li> {@link Arena#makeGameBoard()}, 
 * <li> {@link Arena#makeEvaluator(PlayAgent, GameBoard, int, int, int)}, and 
 * <li> {@link Arena#makeFeatureClass(int)}, 
 * </ul> such that 
 * these factory methods return objects of class {@link GameBoardCube}, 
 * {@link EvaluatorCube}, and {@link FeatureCube}, respectively.
 * 
 * @see GameBoardCube
 * @see EvaluatorCube
 * 
 * @author Wolfgang Konen, TH K�ln, Feb'18
 */
public class ArenaTrainCube extends ArenaTrain   {
	
	public ArenaTrainCube() {
		super();
	}

	public ArenaTrainCube(String title) {
		super(title);		
	}
	
	/**
	 * @return a name of the game, suitable as subdirectory name in the 
	 *         {@code agents} directory
	 */
	public String getGameName() {
		return "RubiksCube";
	}
	
	/**
	 * Factory pattern method
	 */
	public GameBoard makeGameBoard() {
		gb = new GameBoardCube(this);	
		return gb;
	}
	/**
	 * Factory pattern method: make a new Evaluator
	 * @param pa		the agent to evaluate
	 * @param gb		the game board
	 * @param stopEval	the number of successful evaluations needed to reach the 
	 * 					evaluator goal (may be used to stop training prematurely)
	 * @param mode		which evaluator mode: 0,1,2,9. Throws a runtime exception 
	 * 					if {@code mode} is not in the set {@link Evaluator#getAvailableModes()}.
	 * @param verbose	how verbose or silent the evaluator is
	 * @return
	 */
	public Evaluator makeEvaluator(PlayAgent pa, GameBoard gb, int stopEval, int mode, int verbose) {
//		if (mode==-1) mode=EvaluatorCube.getDefaultEvalMode();
		return new EvaluatorCube(pa,gb,stopEval,mode,verbose);
	}

	public Feature makeFeatureClass(int featmode) {
		return new FeatureCube(featmode);
	}

	public XNTupleFuncs makeXNTupleFuncs() {
		return new XNTupleFuncsCube();
	}

	/**
	 * Start GBG for Rubik's Cube (trainable version)
	 * 
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException 
	{
		ArenaTrainCube t_Frame = new ArenaTrainCube("General Board Game Playing");

		if (args.length==0) {
			t_Frame.init();
		} else {
			throw new RuntimeException("[ArenaTrainCube.main] args="+args+" not allowed. Use batch facility.");
		}
	}
	
}
