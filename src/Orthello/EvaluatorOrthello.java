package Orthello;

import controllers.MaxNAgent;
import controllers.PlayAgent;
import games.Evaluator;
import games.GameBoard;

public class EvaluatorOrthello extends Evaluator{

	private MaxNAgent maxNAgent = new MaxNAgent("MaxNAgent");
	GameBoard gb;
	
	public EvaluatorOrthello(PlayAgent e_PlayAgent, int mode, int stopEval, GameBoard gb) {
		super(e_PlayAgent, mode, stopEval);
		this.gb = gb;
		
	}

	@Override
	protected boolean eval_Agent(PlayAgent playAgent) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int[] getAvailableModes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getQuickEvalMode() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getTrainEvalMode() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getPrintString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTooltipString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPlotTitle() {
		// TODO Auto-generated method stub
		return null;
	}

}
