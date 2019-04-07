package controllers.MCTS;

import games.StateObservation;
import tools.ElapsedCpuTimer;
import tools.Types;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Random;

//import controllers.MCTS0.SingleTreeNode0;
import controllers.MCTSExpectimax.MCTSETreeNode;

/**
 * This is adapted from Diego Perez MCTS reference implementation<br>
 * <a href="http://gvgai.net/cont.php">http://gvgai.net/cont.php</a><br>
 * (with a bug fix concerning the number of available actions and an extension
 * for 1- and 2-player games. And the return of VTable information.)
 */
public class SingleTreeNode implements Serializable 
{
	public static double epsilon = 1e-6; // tiebreaker
	public StateObservation m_state = null;
	public SingleTreeNode parent = null;
	public SingleTreeNode[] children = null;
	public SingleMCTSPlayer m_player = null;
	/**
	 * the action which leads from parent's  state to this state
	 */
	public Types.ACTIONS m_act = null; 
	/**
	 * the total value of {@code this} as a child for the parent of {@code this}
	 */
	public double totValue;
	private int nVisits=0;
	public static Random m_rnd = null;
	private int m_depth;
//	private static double[] lastBounds = new double[] { 0, 1 };
//	private static double[] curBounds = new double[] { 0, 1 };
	/**
	 * cumulative probability, needed in {@link #rouletteWheel()}
	 */
    private double cumProb=0;				

	/**
	 * change the version ID for serialization only if a newer version is no
	 * longer compatible with an older one (older .agt.zip will become
	 * unreadable or you have to provide a special version transformation)
	 */
	private static final long serialVersionUID = 12L;

	// --- probably never needed ---
	// public SingleTreeNode() {
	//
	// }

	/**
	 * (does it make sense to pass {@code null} for the state of the node??)
	 * 
	 * @param rnd
	 * @param mplay
	 */
	public SingleTreeNode(Random rnd, SingleMCTSPlayer mplay) {
		this(null, null, null, rnd, mplay);
	}

	/**
	 * 
	 * @param state
	 *            the state of the node
	 * @param act
	 *            the action which leads from parent's state to this state (
	 *            {@code null} for root node)
	 * @param parent
	 *            the parent node ({@code null} for root node)
	 * @param rnd
	 *            a random number generator
	 * @param mplay
	 *            a reference to the one MCTS agent where {@code this} is part
	 *            of (needed to access several parameters of the MCTS agent)
	 */
	public SingleTreeNode(StateObservation state, Types.ACTIONS act, SingleTreeNode parent, 
			Random rnd,	SingleMCTSPlayer mplay) {
		this.m_state = state;
		this.m_act = act;
		this.parent = parent;
		this.m_player = mplay;
		this.m_rnd = rnd;
		if (state == null) {
			children = new SingleTreeNode[m_player.getNUM_ACTIONS()];
		} else {
			children = new SingleTreeNode[state.getNumAvailableActions()]; // /WK/
																			// NEW!
		}
		totValue = 0.0;
		if (parent == null)
			m_depth = 0;
		else
			m_depth = parent.m_depth + 1;
	}

	/**
	 * Perform an MCTS search, i.e. a selection of the best next action given
	 * the state in the root node of the tree. <br>
	 * Called by {@link SingleMCTSPlayer#run(ElapsedCpuTimer, double[])}.
	 * <p>
	 *
	 * Do for {@code m_player.NUM_ITERS} iterations:
	 * <ul>
	 * <li>select a leaf node via {@link #treePolicy()} (this includes
	 *    {@link #expand()} of not fully expanded nodes, as long as the maximum
	 *    tree depth is not yet reached),
	 * <li>make a {@link #rollOut()} starting from this leaf node (a game with
	 *    random actions until game is over or until the maximum rollout depth is
	 *    reached)
	 * <li>{@link #backUp(SingleTreeNode, double)} the resulting score
	 *    {@code delta} and the number of visits for all nodes on {@code totValue}
	 *    and {@code nVisits}. Do this for all nodes on the path from the leaf up
	 *    to the root.
	 * </ul>
	 * 
	 * Once this method is completed, the method {@link #bestAction()} will
	 * return the index {@code i} of the root's children which maximizes
	 * 
	 * <pre>
	 * U(i) = children[i].totValue / children[i].nVisits
	 * </pre>
	 * 
	 * @param elapsedTimer
	 *            currently not used
	 * @param VTable
	 *            on input an array of length K+1, where K is the number of
	 *            available moves for the root state. Contains on output
	 *            {@code U(i)} in the first K entries and the maximum of all
	 *            {@code U(i)} in {@code VTable[K]}
	 */
	public void mctsSearch(ElapsedCpuTimer elapsedTimer, double[] VTable) {

//		lastBounds[0] = curBounds[0];
//		lastBounds[1] = curBounds[1];

		double avgTimeTaken = 0;
		double acumTimeTaken = 0;
		long remaining = elapsedTimer.remainingTimeMillis();
		int numIters = 0;

		int remainingLimit = 5;
		while (numIters < m_player.getNUM_ITERS()) { // /WK/ fixed number of
														// iterations while
														// debugging
			// while(remaining > 2*avgTimeTaken && remaining > remainingLimit){
			ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
			SingleTreeNode selected = treePolicy();
			double delta = selected.rollOut();
// --- this is now equivalently in backUp(), in the first pass through while-loop ---			
//			if (m_state.getNumPlayers() == 2) {
////				delta = -delta;
//				delta = negate(delta);		
//				// why 'negate'? - If the score of 'selected' is a loss for the player who
//				// has to move on 'selected', then it is a win for the player who created 
//				// 'selected' (negamax principle)
//			}
			backUp(selected, delta);
			
			// if (numIters%10==0) System.out.println("numIters="+numIters+",
			// remaining="+remaining+", avgTimeTaken="+avgTimeTaken);
			// if (numIters%10==0) System.out.println("numIters="+numIters+",
			// remaining="+remaining+",
			// elapsedMillis="+elapsedTimer.elapsedMillis());

			numIters++;
			acumTimeTaken += (elapsedTimerIteration.elapsedMillis());

			avgTimeTaken = acumTimeTaken / numIters;
			remaining = elapsedTimer.remainingTimeMillis();
			// System.out.println(elapsedTimerIteration.elapsedMillis() + " -->
			// " + acumTimeTaken + " (" + remaining + ")");
		}

		// fill VTable
		double bestValue = -Double.MAX_VALUE;
		double v;
		double deltaGS = (m_state.getMaxGameScore()-m_state.getMinGameScore());
		double minGS = m_state.getMinGameScore();
		int K = m_state.getNumAvailableActions();
		for (int k = 0; k < K; k++) {
			for (int i = 0; i < children.length; i++) {
				if (children[i].m_act == m_state.getAction(k)) {
					v = children[i].totValue / children[i].nVisits;
					if (m_player.getNormalize()) {
						VTable[k] = v*deltaGS + minGS;		// map back to interval [minGS,maxGS]
					} else {
						VTable[k] = v;
					}
					if (VTable[k] > bestValue) bestValue = VTable[k];
				}
			}
		}
		VTable[K] = bestValue;

		// /WK/ some diagnostic checks (not required for normal operation)
		assert this.nVisits == numIters : "mroot's visits do not match numIters!";
		this.printChildInfo(0, true);

		/*
		 * --- just a sanity check, not required for normal operation: --- 
		 */
//		int k=0; 
//		for (SingleTreeNode c : this.children) { 
//			if (c!=null) { 
//				int nGrandchilds=0; 
//				for (SingleTreeNode g : c.children) 
//					if (g!=null) nGrandchilds++; 
//				System.out.println(k+": nGrandchilds=" +nGrandchilds); 
//				assert nGrandchilds < children.length : "Too many grandchilds!"; 
//				k++; 
//			} 
//		} 
//		System.out.println("");

		if (m_player.getVerbosity() > 0) {
			System.out.println(
					"--  iter=" + numIters + " -- "+
							
					" "+m_state.stringDescr()+" "+
					"( nodes=" + this.numDescendants() + ", time=" + avgTimeTaken + ")"+
					"  value = "+this.totValue/this.nVisits);
			// for UCT and more than 10.000 iteration, bestAction() and  
			// mostVisitedAction() are normally the same:
			System.out.println("-- bestAction="+this.bestAction()+
					   ", mostVisitedAction="+this.mostVisitedAction());
		}
	}

	
	public void printChildInfo(int nIndention, boolean isRootNode) {
		DecimalFormat form = new DecimalFormat("0.0000");
		DecimalFormat for2 = new DecimalFormat("+0.0000;-0.0000");
		DecimalFormat ifor = new DecimalFormat("0000");
		int cVisits = 0;
		int verbose = m_player.getVerbosity();
		String indention = "";
		for (int n = 0; n < nIndention; n++)
			indention += "  ";

		for (SingleTreeNode c : this.children) {
			if (c != null) {
				cVisits += c.nVisits;
				if (verbose > 1) { 	// =2: print direct child info
					double uct_exploit = c.totValue / (c.nVisits + this.epsilon);
					double uct_explore = m_player.getK()
							* Math.sqrt(2*Math.log(this.nVisits + 1) / (c.nVisits + this.epsilon));
//							* Math.pow(Math.log(this.nVisits + 1) / (c.nVisits + this.epsilon),4.0);
					double c_value = c.allChildrenValue();
					if (c.m_state.getNumPlayers()==2) c_value = negate(c_value);
					// Why is uct_exploit=c.totValue/c.nVisits not the same as c.allChildrenvalue()? - 
					// Because uct_exploit has one visit more (from the 'birth' of c, where it did not have 
					// any children yet). If c has few visits, this value from the first visit might spoil the 
					// result (it is from a less precise tree), but in the limit of many visits to c, both 
					// values will approach.
					System.out.println(indention + c.m_state.stringDescr() + ": " + ifor.format(c.nVisits) + ", "
							+ for2.format(uct_exploit) + " + " + form.format(uct_explore) + " = "
							+ form.format(uct_exploit + uct_explore)
							+ "  [c_value = " + form.format(c_value)+"]");
				}
				if (verbose > 2)	// =3: children+grandchildren, =4: 3 generations, =5: 4 gen, ...
					if (c.m_depth<=(verbose-2)) c.printChildInfo(nIndention + 1, false);
			}
		}
		int offs = (isRootNode) ? 0 : 1; // why '1'? - every non-root node n has one visit more 
		// than all its children. This stems from its 'birth' visit, i.e. the time 
		// where it was 'selected' but did not yet have any children.
		assert offs + cVisits == this.nVisits : "children visits do not match the visits of this!";
	}
	
	/**
	 * 
	 * @return (total value of all children)/(total visits of all children)  of {@code this}
	 */
	private double allChildrenValue() {
		double val = 0.0;
		double visits = 0;
		for (SingleTreeNode c : this.children) {
			if (c!=null) {
				val+=c.totValue;
				visits+=c.nVisits;
			}
		}
		if (visits==0) return Double.NaN;	// not availabe, 'this' is leaf node
		return val/visits;
	}

	public SingleTreeNode treePolicy() {

		SingleTreeNode cur = this;

		while (!cur.m_state.isGameOver() && cur.m_depth < m_player.getTREE_DEPTH()) 
		{
			if (cur.notFullyExpanded()) {
				return cur.expand();

			} else {
				switch(m_player.getParMCTS().getSelectMode()) {
				case 0: 
//					SingleTreeNode next0 = cur.uct();
//					cur = next0;
					cur = cur.uct();
					break;
				case 1: 
//					SingleTreeNode next1 = cur.egreedy();
//					cur = next1;
					cur = cur.egreedy();
					break; 
				case 2: 
//					SingleTreeNode next2 = cur.rouletteWheel();
//					cur = next2;
					cur = cur.rouletteWheel();
					break; 
				default: 
					throw new RuntimeException("this selectMode is not yet implemented");
				}
			}
		}

		return cur;
	}

	/**
	 * Expand the current node {@code this}, i. e. select randomly one of those
	 * children {@code children[i]} being yet {@code null}. Then advance the
	 * state of {@code this} with the {@code i}th available action and construct
	 * child node {@code children[i]} from this advanced state.
	 * 
	 * @return {@code children[i]}
	 */
	public SingleTreeNode expand() {

		int bestAction = 0;
		double bestValue = -1;

		//System.out.println("expand() for m_state.actions.length = "+m_state.getNumAvailableActions());

        for (int i = 0; i < children.length; i++) {
            double x = m_rnd.nextDouble();
            if (x > bestValue && children[i] == null) {
                bestAction = i;
                bestValue = x;
            }
        }

		assert m_state != null : "Warning: state is null!";
		assert m_state.getNumAvailableActions() == children.length : "s.th. wrong with children.length";
		Types.ACTIONS actBest = m_state.getAction(bestAction);

		StateObservation nextState = m_state.copy();
		// nextState.advance(m_player.actions[bestAction]);
		nextState.advance(actBest); 		// /WK/ NEW!

		SingleTreeNode tn = new SingleTreeNode(nextState, actBest, this, this.m_rnd, this.m_player);
		children[bestAction] = tn;
		return tn;

	}

    /**
     * Select the child node with the highest UCT value
     *
     * @return the selected child node
     * 
     * @see #rouletteWheel()
     * @see #egreedy()
     */
	public SingleTreeNode uct() {

		SingleTreeNode selected = null;
		double bestValue = -Double.MAX_VALUE;
		for (SingleTreeNode child : this.children) 
		{
			double childValue = child.totValue / (child.nVisits + this.epsilon);
			if (m_player.getNormalize()) assert (childValue >= 0) : "childValue is negative";
				
			double uctValue = childValue
					+ m_player.getK() * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + this.epsilon))
					+ this.m_rnd.nextDouble() * this.epsilon;
					// small random numbers: break ties in unexpanded nodes

			if (uctValue > bestValue) {
				selected = child;
				bestValue = uctValue;
			}
		}

		if (selected == null) {
			throw new RuntimeException("Warning! returning null: " + bestValue + " : " + this.children.length);
		}

		return selected;
	}

    /**
     * Epsilon-Greedy, a variant to UCT
     *
     * @return the selected child node
     * 
     * @see #uct()
     * @see #rouletteWheel()
     */
	public SingleTreeNode egreedy() {

		SingleTreeNode selected = null;
		double epsGreedy = m_player.getParMCTS().getEpsGreedy();

		if (m_rnd.nextDouble() < epsGreedy) {
			// Choose randomly
			int selectedIdx = m_rnd.nextInt(children.length);
			selected = this.children[selectedIdx];

		} else {
			// pick the best Q.
			double bestValue = -Double.MAX_VALUE;
			for (SingleTreeNode child : this.children) {
				double cval = child.totValue / (child.nVisits + this.epsilon);

				// small sampleRandom numbers: break ties in unexpanded nodes
				if (cval > bestValue) {
					selected = child;
					bestValue = cval;
				}
			}

		}

		if (selected == null) {
			throw new RuntimeException("Warning! returning null: " + this.children.length);
		}

		return selected;
	}

    /**
     * Roulette-wheel selection, a variant to UCT. See Sec. 2.5.1 in [Swiechowski15],
     * <a href="http://dx.doi.org/10.1155/2015/986262">http://dx.doi.org/10.1155/2015/986262</a>: <br>
     * Swiechowski, M., et al.: <i>Recent Advances in General Game Playing</i>, The Scientific World Journal, Volume 2015, Article ID 986262.
     * <p>
     * Each child gets a sector on the roulette wheel (0,1] with its sector size being  
     * proportional to the child's utility
     * <pre>
     * 		 U(child) = child.value/child.visits. </pre> 
     * Now a random number from (0,1] is chosen and the child whose sector 
     * contains this random number is selected.
     * <p>
     * @return the selected child node
     * 
     * @see #uct()
     * @see #egreedy()
     */
    public SingleTreeNode rouletteWheel() {
    	// TODO: implement one-move wins and one-move losses acc. to [Swiechowski15]
        double rnd = m_rnd.nextDouble();
        double vTotal = 0.0;
        double vMin = 0.0;
        double cumProb = 0.0;		// cumulative probability of all children up to current child
        SingleTreeNode selected = null;

        // We assign to each child a probability which is the softmax of its utility U(child):
        // 		p(i)  =  U(child) / vTotal  =  U(child) / sum_j U(child_j)
        // Example: 3 children with probabilities p(0)=0.2, p(1)=0.5, p(2)=0.3. This defines a 
        // segmentation of the roulette wheel in three sectors:
        //		child_0: (0,0.2], child_1: (0.2,0.7], child_3: (0.7,1.0].
        // The LHS of the intervals are the cumulative probabilities child.cumProb. We select 
        // that child which is the first with its LHS >= rnd.
		vMin = (m_player.getNormalize()) ? 0.0 : m_state.getMinGameScore();
        for (SingleTreeNode child : this.children) {
        	vTotal += (child.totValue/child.nVisits)-vMin; 
        }
        for (SingleTreeNode child : this.children) {
        	cumProb = cumProb + ((child.totValue/child.nVisits)-vMin)/vTotal;
        	child.cumProb = cumProb;
        	// We do not really need child.cumProb, we could just work with the local variable
        	// cumProb. We have child.cumProb only for debugging purposes in order to have in 
        	// this.children all cumulative probabilities and see if they converge to 1.
        	if (cumProb>=rnd) {
        		return child;		// the normal return
        	}
        }
        // --- only for debugging, if we comment 'if (cumProb...)' above out in order to ensure
        // --- calculation of all child.cumProb:
//        for (SingleTreeNode child : this.children) {
//        	if (child.cumProb>=rnd) {
//        		return child;		// the normal return
//        	}
//        }

        throw new RuntimeException("rouletteWheel: We should not arrive here!");
    }

	/**
	 * Play a rollout from {@code this.m_state}
	 * 
	 * @return the value (game score) of the rollout from the perspective of
	 *         {@code this.m_state}. I. e. if the rollout produces a win for the
	 *         player who has to move in {@code m_state} then return a positive
	 *         reward.
	 */
	public double rollOut() 
	{
		StateObservation rollerState = m_state.copy();
		int thisDepth = this.m_depth;

		while (!finishRollout(rollerState, thisDepth)) {

			// int action = m_rnd.nextInt(m_player.NUM_ACTIONS);
			// rollerState.advance(m_player.actions[action]);
			rollerState.setAvailableActions();
			int action = m_rnd.nextInt(rollerState.getNumAvailableActions());
			rollerState.advance(rollerState.getAction(action));
			thisDepth++;
		}
		if (rollerState.isGameOver())
			m_player.nRolloutFinished++;

		double delta = value(rollerState, this.m_state);

		// // /WK/ not really clear what these normalizations are for.
		// // Is it part of MCTS or part of the special GVGP implementation?
		// if(delta < curBounds[0]) curBounds[0] = delta;
		// if(delta > curBounds[1]) curBounds[1] = delta;
		// double normDelta = Utils.normalise(delta ,lastBounds[0], lastBounds[1]);
		// return normDelta;
		//
		return delta;
	}

	/**
	 * Assign the final rollerState a value (reward).
	 * <p>
	 * If 'Normalize' is checked, the reward is passed through a normalizing function q()
	 * which maps to [0,1]. Otherwise q() is the identity function.
	 * 
	 * @param so
	 *            the final state
	 * @param referingState
	 *            the state where the rollout (playout) started
	 * 
	 * @return q(reward), the reward or game score for {@code so} (relative to {@code referingState})
	 */
	public double value(StateObservation so, StateObservation referingState) {
		boolean rgs = m_player.getParOther().getRewardIsGameScore();
		double v = so.getReward(referingState, rgs);
//		double v = so.getGameScore(referingState);
		if (m_player.getNormalize()) {
			// /WK/ bug fix 2019-02-09: map v to [0,1] (this is q(reward) in notes_MCTS.docx)
			v = (v - so.getMinGameScore()) / (so.getMaxGameScore() - so.getMinGameScore());
			assert ((v >= 0) && (v <= 1)) : "Error: value is not in range [0,1]";
		}
		return v;
	}

    /**
     * checks if a rollout is finished
     *
     * @param rollerState the current game state
     * @param depth the current rollout depth
     * @return true if the rollout is finished, false if not
     */
	public boolean finishRollout(StateObservation rollerState, int depth) 
	{
		if (depth >= m_player.getROLLOUT_DEPTH()) // rollout end condition.
			return true;

		if (rollerState.isGameOver()) // end of game
			return true;

		return false;
	}

	public void backUp(SingleTreeNode selected, double delta) 
	{
		SingleTreeNode n = selected;
		while (n != null) {
			switch (m_state.getNumPlayers()) {
			case (1):
				break;
			case (2):	// negamax variant for 2-player tree
				// Why do we call 'negate' *before* the first '+=' to n.totValue is made? - 
				// If the score of 'selected' is a loss for the player who has to move on 
				// 'selected', then it is a win for the player who created 'selected'  
				// (negamax principle)
				delta = negate(delta);
				break;
			default: // i.e. n-player, n>2
				throw new RuntimeException("MCTS.backUp is not yet implemented for (n>2)-player games (n>2).");
			}

			n.nVisits++;
			n.totValue += delta;
//            switch (m_state.getNumPlayers()) {
//            case (1): break;
//            case (2): 
////            	delta = - delta; 		// /WK/ negamax variant for 2-player tree
//            	delta = negate(delta);	// /WK/ negamax variant for 2-player tree
//            	break;
//            default:		// i.e. n-player, n>2
//            	throw new RuntimeException("MCTS.backUp is not yet implemented for n-player games (n>2).");
//            }

			// --- only a debug assertion
//			if (!n.isLeafNode() && n.parent!=null) {
//				int cVisits=0;
//				for (SingleTreeNode c : n.children) {
//					if (c!=null) cVisits+=c.nVisits;
//				}
//				int offs=1;
//				assert (n.nVisits == offs+cVisits) :	// why '1+'? - every non-root node n has one visit more 
//					// than all its children. This stems from its 'birth' visit, i.e. the time 
//					// where it was 'selected' but did not yet have any children.
//					"node n's nVisits differs from the 1+(sum of its children visits)";
//				
//			}
			
			n = n.parent;
		}
	}
	
	private boolean isLeafNode() {
		for (SingleTreeNode c : this.children) {
			if (c!=null) return false;
		}
		return true;
	}

	private double negate(double delta) {
		if (m_player.getNormalize()) {
			// map a normalized delta \in [0,1] again to [0,1], but reverse the order.
			// /WK/ "1-" is the bug fix 2019-02-09 needed to achieve always child.totValue>=0
			return 1-delta; 
		} else {
			// reverse the delta-order for arbitrarily distributed delta;
			// maps from interval [a,b] to [-b,-a] (this can be problematic for UCT-rule)
			return -delta;
		}
	}
	
	public int mostVisitedAction() {
		int selected = -1;
		double bestValue = -Double.MAX_VALUE;
		double tieBreaker, dVisit;
		boolean allEqual = true;
		double first = -1;

		assert children.length > 0 : "No children in mostVisitedAction!";

		for (int i = 0; i < children.length; i++) {

			if (children[i] != null) {
				if (first == -1)
					first = children[i].nVisits;
				else if (first != children[i].nVisits) {
					allEqual = false;
				}

				tieBreaker = m_rnd.nextDouble() * epsilon;
				dVisit = children[i].nVisits + tieBreaker;
				if (dVisit > bestValue) {
					bestValue = dVisit;
					selected = i;
				}
			}
		}

		assert (selected != -1) : "Unexpected selection 1!";

		if (allEqual) {
			// If all are equal, we opt to choose for the one with the best Q.
			selected = bestAction();
		}
		return selected;
	}

	/**
	 * 
	 * @return the index {@code i} of the child maximizing
	 * 
	 *         <pre>
	 *         U(i) = children[i].totValue / children[i].nVisits
	 *         </pre>
	 */
	public int bestAction() {
		int selected = -1;
		double bestValue = -Double.MAX_VALUE;
		double tieBreaker, dTotVal;

		assert children.length > 0 : "No children in function bestAction()!";

		for (int i = 0; i < children.length; i++) {

			tieBreaker = m_rnd.nextDouble() * epsilon;
			dTotVal = children[i].totValue / children[i].nVisits + tieBreaker;
			// /WK/: bug fix: '/children[i].nVisits' added (!)
			if (children[i] != null && dTotVal > bestValue) {
				bestValue = dTotVal;
				selected = i;
			}
		}

		assert (selected != -1) : "Selection in bestAction() did not work!";

		return selected;
	}

	public boolean notFullyExpanded() {
		for (SingleTreeNode tn : children) {
			if (tn == null) {
				return true;
			}
		}

		return false;
	}

	/**
	 * just for diagnostics:
	 * 
	 * @return number of nodes in the MCTS tree from {@code this} downwards. <br>
	 * 		   If {@code this} is the root node, it is the number of nodes in the whole tree. 
	 */
	public int numDescendants() {
		int N = 1; // include this
		for (SingleTreeNode c : this.children)
			if (c != null)
				N += c.numDescendants();
		return N;
	}

}
