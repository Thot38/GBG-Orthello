package controllers.MCTS0;

import games.StateObservation;
import tools.ElapsedCpuTimer;
import tools.Types;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Random;

import controllers.MCTS.SingleTreeNode;

/**
 * --- THIS IS THE SNAPSHOT FROM 2019-01-22 ----------------------------------
 * 
 * This is adapted from Diego Perez MCTS reference implementation<br>
 * 		<a href="http://gvgai.net/cont.php">http://gvgai.net/cont.php</a><br>
 * (with a bug fix concerning the number of available actions and an 
 *  extension for 1- and 2-player games. And the return of VTable information.)
 */
public class SingleTreeNode0 implements Serializable
{
    public static double epsilon = 1e-6;		// tiebreaker
    public StateObservation m_state=null;
    public Types.ACTIONS m_act=null;		// the action which leads from parent's state to this state
    public SingleTreeNode0 parent=null;
    public SingleTreeNode0[] children=null;
    public SingleMCTSPlayer0 m_player=null;
    public double totValue;
    public int nVisits;
    public static Random m_rnd=null;
    private int m_depth;
//    private static double[] lastBounds = new double[]{0,1};
//    private static double[] curBounds = new double[]{0,1};
    /**
     *  egreedyEpsilon = probability that a random action is taken (instead 
     *  greedy action). This is *only* relevant, if function egreedy() is 
     *  used as variant to uct() (which is currently *not* the case).
     */
    public static double egreedyEpsilon = 0.05; 

	/**
	 * change the version ID for serialization only if a newer version is no longer 
	 * compatible with an older one (older .agt.zip will become unreadable or you have
	 * to provide a special version transformation)
	 */
	private static final long  serialVersionUID = 12L;

// --- probably never needed ---
//    public SingleTreeNode() {
//  	
//    }
    
    /**
     * (does it make sense to pass {@code null} for the state of the node??)
     * @param rnd
     * @param mplay
     */
    public SingleTreeNode0(Random rnd, SingleMCTSPlayer0 mplay) {
        this(null, null, null, rnd, mplay);
    }

    /**
     * 
     * @param state the state of the node 
     * @param act	the action which leads from parent's state to this state ({@code null} for root node)
     * @param parent the parent node ({@code null} for root node)
     * @param rnd   a random number generator
     * @param mplay a reference to the one MCTS agent where {@code this} is part of (needed
     * 				to access several parameters of the MCTS agent)
     */
    public SingleTreeNode0(StateObservation state, Types.ACTIONS act, SingleTreeNode0 parent, 
    		Random rnd, SingleMCTSPlayer0 mplay) {
        this.m_state = state;
        this.m_act = act;
        this.parent = parent;
        this.m_player = mplay;
        this.m_rnd = rnd;
        if (state==null) {
        	children = new SingleTreeNode0[m_player.getNUM_ACTIONS()];
        } else {
            children = new SingleTreeNode0[state.getNumAvailableActions()];			// /WK/ NEW!        	
        }
        totValue = 0.0;
        if(parent != null)
            m_depth = parent.m_depth+1;
        else
            m_depth = 0;
    }


    /**
     * Perform an MCTS search, i.e. a selection of the best next action given the state 
     * in the root node of the tree. <br>
     * Called by {@link SingleMCTSPlayer0#run(ElapsedCpuTimer, double[])}.<p>
     *
     * Do for {@code m_player.NUM_ITERS} iterations:
     * <ul>
     * <li> select a leaf node via {@link #treePolicy()} (this includes {@link #expand()} of 
     * 		not fully expanded nodes, as long as the maximum tree depth is not yet reached),
     * <li> make a {@link #rollOut()} starting from this leaf node (a game with random actions 
     * 		until game is over or until the maximum rollout depth is reached)
     * <li> {@link #backUp(SingleTreeNode0, double)} the resulting score {@code delta} and 
     * 		the number of visits for all nodes on {@code totValue} and {@code nVisits}.
     * 		Do this for all nodes on the path from the leaf up to the root.
     * </ul>
     * 
     * Once this method is completed, the method {@link #bestAction()} will return the index
     * {@code i} of the root's children which maximizes 
     * <pre> U(i) = children[i].totValue/children[i].nVisits </pre>
     * 
     * @param elapsedTimer currently not used
     * @param VTable on input an array of length K+1, where K is the number of available 
     * 		  moves for the root state. Contains on output {@code U(i)} in the first 
     * 		  K entries and the maximum of all {@code U(i)} in {@code VTable[K]}
     */
    public void mctsSearch(ElapsedCpuTimer elapsedTimer, double[] VTable) {

//        lastBounds[0] = curBounds[0];
//        lastBounds[1] = curBounds[1];

        double avgTimeTaken = 0;
        double acumTimeTaken = 0;
        long remaining = elapsedTimer.remainingTimeMillis();
        int numIters = 0;

        int remainingLimit = 5;
        while(numIters<m_player.getNUM_ITERS()){ 		// /WK/ fixed number of iterations while debugging
        //while(remaining > 2*avgTimeTaken && remaining > remainingLimit){
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
            SingleTreeNode0 selected = treePolicy();
            double delta = selected.rollOut();
            if (m_state.getNumPlayers()==2) {
            	delta = - delta;
            	// why '-'? - If selected's score is a loss for the player who has to move on 'selected',  
            	// then it is a win for the player who created 'selected' (negamax principle)
            }
            backUp(selected, delta);
            //if (numIters%10==0) System.out.println("numIters="+numIters+", remaining="+remaining+", avgTimeTaken="+avgTimeTaken);
            //if (numIters%10==0) System.out.println("numIters="+numIters+", remaining="+remaining+", elapsedMillis="+elapsedTimer.elapsedMillis());
            
            numIters++;
            acumTimeTaken += (elapsedTimerIteration.elapsedMillis()) ;

            avgTimeTaken  = acumTimeTaken/numIters;
            remaining = elapsedTimer.remainingTimeMillis();
            //System.out.println(elapsedTimerIteration.elapsedMillis() + " --> " + acumTimeTaken + " (" + remaining + ")");
        }
        
        // fill VTable
        double bestValue = -Double.MAX_VALUE;
        int K=m_state.getNumAvailableActions();
        for (int k=0; k<K; k++) {
            for (int i=0; i<children.length; i++) {
            	if (children[i].m_act==m_state.getAction(k)) {
            		VTable[k] = children[i].totValue/children[i].nVisits;
            		if (VTable[k]>bestValue) bestValue=VTable[k];
            	}
            }        	
        }
        VTable[K]=bestValue;

        
        // /WK/ some diagnostic checks (not required for normal operation)
        assert this.nVisits==numIters : "mroot's visits do not match numIters!";
		this.printChildInfo(0, true);
//        int cVisits=0;
//        for (SingleTreeNode0 c : this.children) {
//        	if (c!=null) {
//        		cVisits += c.nVisits;
//        		if (m_player.getVerbosity()>1)
//        			System.out.println(c.m_state.stringDescr() + ": " + c.nVisits + ", " + c.totValue*3932156/c.nVisits);
//    				//System.out.println(c.m_state.stringDescr() + ": " + c.nVisits + ", " + c.totValue/c.nVisits);
//        	}
//        }
//        assert cVisits==numIters : "children visits do not match numIters!";
        
        /*
         * --- just a sanity check, not required for normal operation: ---
        int k=0;
        for (SingleTreeNode c : this.children) {
        	if (c!=null) { 
        		int nGrandchilds=0;
        		for (SingleTreeNode g : c.children) 
        			if (g!=null) nGrandchilds++;
        		//System.out.println(k+": nGrandchilds="+nGrandchilds);
        		assert nGrandchilds < children.length : "Too many grandchilds!";
        		k++;
        	}
        }
        //System.out.println("");
        */

		if (m_player.getVerbosity() > 0) {
			System.out.println(
					"--  iter=" + numIters + " -- "+
							
					" "+m_state.stringDescr()+" "+
					"( nodes=" + this.numDescendants() + ", time=" + avgTimeTaken + ")"+
					"  value = "+this.totValue/this.nVisits);
			// for UCT and more than 10.000 iteration, bestAction() and  
			// mostVisitedAction() are normally the same:
//			System.out.println("-- bestAction="+this.bestAction()+
//					   ", mostVisitedAction="+this.mostVisitedAction());
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

		for (SingleTreeNode0 c : this.children) {
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
		for (SingleTreeNode0 c : this.children) {
			if (c!=null) {
				val+=c.totValue;
				visits+=c.nVisits;
			}
		}
		if (visits==0) return Double.NaN;	// not availabe, 'this' is leaf node
		return val/visits;
	}


	public SingleTreeNode0 treePolicy() {

		SingleTreeNode0 cur = this;

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
					SingleTreeNode0 next1 = cur.egreedy();
					cur = next1;
					break; 
//				case 2: 
//					next = cur.rouletteWheel();
//					break; 
				default: 
					throw new RuntimeException("this selectMode is not yet implemented");
				}
			}
		}

		return cur;
	}

//    public SingleTreeNode0 treePolicy() {
//
//        SingleTreeNode0 cur = this;
//
//        while (!cur.m_state.isGameOver() && cur.m_depth < m_player.getTREE_DEPTH())
//        {
//            if (cur.notFullyExpanded()) {
//                return cur.expand();
//
//            } else {
//                SingleTreeNode0 next = cur.uct();
//                //SingleTreeNode next = cur.egreedy();
//                cur = next;
//            }
//        }
//
//        return cur;
//    }


    /**
     * Expand the current node {@code this}, i. e. select randomly one of those children {@code children[i]} 
     * being yet {@code null}. Then advance the state of {@code this} with the {@code i}th  
     * available action and construct child node {@code children[i]} from this advanced state.
     * 
     * @return {@code children[i]}
     */
    public SingleTreeNode0 expand() {

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
        //nextState.advance(m_player.actions[bestAction]);
        nextState.advance(actBest);			// /WK/ NEW!

        SingleTreeNode0 tn = new SingleTreeNode0(nextState, actBest, this, this.m_rnd,this.m_player);
        children[bestAction] = tn;
        return tn;

    }

    public SingleTreeNode0 uct() {

        SingleTreeNode0 selected = null;
        double bestValue = -Double.MAX_VALUE;
        for (SingleTreeNode0 child : this.children)
        {
//            double hvVal = child.totValue;
            double childValue =  child.totValue / (child.nVisits + this.epsilon);

            double uctValue = childValue 
            		+ m_player.getK() * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + this.epsilon)) 
                    + this.m_rnd.nextDouble() * this.epsilon;
            		// small sampleRandom numbers: break ties in unexpanded nodes
            
            if (uctValue > bestValue) {
                selected = child;
                bestValue = uctValue;
            }
        }

        if (selected == null)
        {
            throw new RuntimeException("Warning! returning null: " + bestValue + " : " + this.children.length);
        }

        return selected;
    }
    
    /**
     * Epsilon-Greedy, a variant to UCT
     * @return the best child node
     */
    public SingleTreeNode0 egreedy() {


        SingleTreeNode0 selected = null;

        if(m_rnd.nextDouble() < egreedyEpsilon)
        {
            //Choose randomly
            int selectedIdx = m_rnd.nextInt(children.length);
            selected = this.children[selectedIdx];

        }else{
            //pick the best Q.
            double bestValue = -Double.MAX_VALUE;
            for (SingleTreeNode0 child : this.children)
            {
                double hvVal = child.totValue;

                // small sampleRandom numbers: break ties in unexpanded nodes
                if (hvVal > bestValue) {
                    selected = child;
                    bestValue = hvVal;
                }
            }

        }


        if (selected == null)
        {
            throw new RuntimeException("Warning! returning null: " + this.children.length);
        }

        return selected;
    }


    public double rollOut()
    {
        StateObservation rollerState = m_state.copy();
        int thisDepth = this.m_depth;

        while (!finishRollout(rollerState,thisDepth)) {

            // int action = m_rnd.nextInt(m_player.NUM_ACTIONS);
            // rollerState.advance(m_player.actions[action]);
        	rollerState.setAvailableActions();
        	int action = m_rnd.nextInt(rollerState.getNumAvailableActions());
            rollerState.advance(rollerState.getAction(action));
            thisDepth++;
        }
        if (rollerState.isGameOver()) 
        	m_player.nRolloutFinished++;

        double delta = value(rollerState,this.m_state);

//        // /WK/ not really clear what these normalizations are for.
//        //      Is it part of MCTS or part of the special GVGP implementation?
//        if(delta < curBounds[0]) curBounds[0] = delta;
//        if(delta > curBounds[1]) curBounds[1] = delta;
//
//        double normDelta = Utils.normalise(delta ,lastBounds[0], lastBounds[1]);
//
//        return normDelta;
        return delta;
    }

    /**
     * Assign the final rollerState a value (reward). 
     * @param a_gameState	the final state
     * @param referingState	the state where the rollout (playout) started
     * 
     * @return reward		the game score (relative to referingState)
     */
    public double value(StateObservation a_gameState, StateObservation referingState) {
    	double v = a_gameState.getGameScore(referingState);
        return v;
    }

    public boolean finishRollout(StateObservation rollerState, int depth)
    {
        if(depth >= m_player.getROLLOUT_DEPTH())      //rollout end condition.
            return true;

        if(rollerState.isGameOver())               //end of game
            return true;

        return false;
    }

    public void backUp(SingleTreeNode0 node, double delta)
    {
        SingleTreeNode0 n = node;
        while(n != null)
        {
            n.nVisits++;
            n.totValue += delta;
            switch (m_state.getNumPlayers()) {
            case (1): break;
            case (2): 
            	delta = - delta; 		// /WK/ negamax variant for 2-player tree
            	break;
            default:		// i.e. n-player, n>2
            	throw new RuntimeException("MCTS.backUp is not yet implemented for n-player games (n>2).");
            }
            
            n = n.parent;
        }
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

        assert children.length>0 : "No children in mostVisitedAction!";

        for (int i=0; i<children.length; i++) {

            if(children[i] != null)
            {
                if(first == -1)
                    first = children[i].nVisits;
                else if(first != children[i].nVisits)
                {
                    allEqual = false;
                }

                tieBreaker = m_rnd.nextDouble() * epsilon;
                dVisit = children[i].nVisits + tieBreaker;
                if ( dVisit > bestValue) {
                    bestValue = dVisit;
                    selected = i;
                }
            }
        }

        assert (selected != -1) : "Unexpected selection 1!";
        	
        if(allEqual)
        {
            //If all are equal, we opt to choose for the one with the best Q.
            selected = bestAction();
        }
        return selected;
    }

    /**
     * 
     * @return the index {@code i} of the child maximizing 
     * <pre> U(i) = children[i].totValue/children[i].nVisits </pre>
     */
    public int bestAction()
    {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;
        double tieBreaker, dTotVal;

        assert children.length>0 : "No children in function bestAction()!";
        
        for (int i=0; i<children.length; i++) {

            tieBreaker = m_rnd.nextDouble() * epsilon;
            dTotVal = children[i].totValue/children[i].nVisits + tieBreaker;   
            			// /WK/: bug fix: '/children[i].nVisits' added (!)
            if(children[i] != null && dTotVal > bestValue) {
                bestValue = dTotVal;
                selected = i;
            }
        }

        assert (selected != -1) : "Unexpected selection 2!";

        return selected;
    }


    public boolean notFullyExpanded() {
        for (SingleTreeNode0 tn : children) {
            if (tn == null) {
                return true;
            }
        }

        return false;
    }
    
    /**
     * just for diagnostics: 
     * @return  number of nodes in the MCTS tree
     */
    public int numDescendants() {
    	int N=1;  // include this
		for (SingleTreeNode0 c : this.children) 
			if (c!=null) 
				N += 1+c.numDescendants();
    	return N;
    }
    
}
