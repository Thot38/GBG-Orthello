package Orthello;

import controllers.PlayAgent;
import games.Evaluator;

public class EvaluatorOrthello extends Evaluator{

	public EvaluatorOrthello(PlayAgent e_PlayAgent, int mode, int stopEval) {
		super(e_PlayAgent, mode, stopEval);
		// TODO Auto-generated constructor stub
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
