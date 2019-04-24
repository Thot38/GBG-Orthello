package Othello;

import controllers.MaxNAgent;
import controllers.PlayAgent;
import controllers.RandomAgent;
import controllers.MCTS.MCTSAgentT;
import games.Evaluator;
import games.GameBoard;
import games.XArenaFuncs;
import games.Hex.StateObserverHex;
import params.ParMCTS;
import params.ParMaxN;
import params.ParOther;

public class EvaluatorOthello extends Evaluator{

	private MaxNAgent maxNAgent=null; 
    protected int verbose = 0;
    private MCTSAgentT mctsAgent = null;
    private RandomAgent randomAgent = new RandomAgent("Random");
    private double trainingThreshold = 0.7;
    private GameBoard m_gb;
    private PlayAgent playAgent;
    private int numStartStates = 1;
	
	public EvaluatorOthello(PlayAgent e_PlayAgent, GameBoard gb, int stopEval, int mode, int verbose) {
		super(e_PlayAgent, mode, stopEval);
		if(verbose == 1)
		{
			System.out.println("Using evaluation mode" + mode);
		}
		initEvaluator(e_PlayAgent, gb);

	}
	
	public void initEvaluator(PlayAgent playAgent, GameBoard gb)
	{
		this.m_gb = gb;
		this.playAgent = playAgent;
		
		ParMaxN parM = new ParMaxN();
		parM.setMaxNDepth(15);
		parM.setMaxNUseHashmap(true);
		maxNAgent = new MaxNAgent("MaxNAgent", parM, new ParOther());
	}

	@Override
	protected boolean eval_Agent(PlayAgent playAgent) {
		// TODO Auto-generated method stub
		return evalAgent(playAgent);
	}

	protected boolean evalAgent(PlayAgent playAgent)
	{
		this.playAgent = playAgent;
		double result;
		switch(m_mode) {
		case 0:
			result = competeAgainstMCTS(playAgent, m_gb,10);
			break;
		case 1:
			result = competeAgainstRandom(playAgent, m_gb);
			break;
		case 2:
			result = competeAgainstMaxN(playAgent, m_gb,10);
			break;
		default: return false;
		}
		
		return result >= 0.5;
	}
	
	 private double competeAgainstRandom(PlayAgent playAgent, GameBoard gameBoard) {
	        //double success = XArenaFuncs.competeBoth(playAgent, randomAgent, 10, gameBoard);
	        double[] res = XArenaFuncs.compete(playAgent, randomAgent, new StateObserverHex(), 100, verbose, null);
	        double success = res[0]-res[2];
	        m_msg = playAgent.getName() + ": " + this.getPrintString() + success;
	        if (this.verbose > 0) System.out.println(m_msg);
	        lastResult = success;
	        return success;
	    }
	 
	  private double competeAgainstMaxN(PlayAgent playAgent, GameBoard gameBoard, int numEpisodes) {
	        double[] res = XArenaFuncs.compete(playAgent, maxNAgent, new StateObserverHex(), numEpisodes, verbose, null);
	        double success = res[0]-res[2];
	        m_msg = playAgent.getName() + ": " + this.getPrintString() + success + "  (#="+numEpisodes+")";
	        if (this.verbose > 0) System.out.println(m_msg);
	        lastResult = success;
	        return success;
	    }
	
	  private double competeAgainstMCTS(PlayAgent playAgent, GameBoard gameBoard, int numEpisodes) {
	        ParMCTS params = new ParMCTS();
	        int numIterExp =  (Math.min(ConfigOthello.BOARD_SIZE,5) - 1);
	        params.setNumIter((int) Math.pow(10, numIterExp));
	        mctsAgent = new MCTSAgentT("MCTS", new StateObserverHex(), params);

	        double[] res = XArenaFuncs.compete(playAgent, mctsAgent, new StateObserverHex(), numEpisodes, 0, null);
	        double success = res[0]-res[2];        	
	        m_msg = playAgent.getName() + ": " + this.getPrintString() + success;
	        //if (this.verbose > 0) 
	        	System.out.println(m_msg);
	        lastResult = success;
	        return success;
	    }
	
	
	@Override
	public int[] getAvailableModes() {
		// TODO Auto-generated method stub
		return new int[] {-1,0,1,2};
	}

	@Override
	public int getQuickEvalMode() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getTrainEvalMode() {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public String getPrintString() {
		 switch (m_mode) {
			case -1: return "no evaluation done ";
         case 0:  return "success against MCTS (best is 1.0): ";
         case 1:  return "success against Random (best is 1.0): ";
         case 2:  return "success against Max-N (best is 1.0): ";
         case 10: return "success against MCTS (" + numStartStates + " diff. start states, best is 1.0): ";
         case 11: return "success against TDReferee (" + numStartStates + " diff. start states, best is 1.0): ";
         default: return null;
     }
	}

	@Override
	public String getTooltipString() {
		return "<html>-1: none<br>"
				+ "0: against MCTS, best is 1.0<br>"
				+ "1: against Random, best is 1.0<br>"
				+ "2: against Max-N, best is 1.0<br>"
				+ "10: against MCTS, different starts, best is 1.0<br>"
				+ "11: against TDReferee.agt.zip, different starts"
				+ "</html>";
	}

	@Override
	public String getPlotTitle() {
		switch (m_mode) {
          case 0:  return "success against MCTS";
          case 1:  return "success against Random";
          case 2:  return "success against Max-N";
          case 10: return "success against MCTS";
          case 11: return "success against TDReferee";
          default: return null;
		 }
	}

}
