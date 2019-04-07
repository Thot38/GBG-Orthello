package games;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *  {@link TStats} is a class to store a tuple of int's with diagnostic information
 *  about the last training episode: <ul>
 *  <li> <b>n</b> 			the episode counter (gameNum)
 *  <li> <b>p</b> 			the minimum episode length 
 *  <li> <b>moveNum</b>		the actual number of moves in this episode
 *  <li> <b>epiLength</b>	the maximum allowed episode length
 *  </ul>
 *  
 *  This class is mainly useful for game RubiksCube, but may not be completely useless
 *  for other (puzzle) games as well.
 */
public class TStats {
	int n;
	int p;
	int moveNum;
	int epiLength;
	
	public TStats(int n, int p, int moveNum, int epiLength) {
		this.n=n;
		this.p=p;
		this.moveNum=moveNum;
		this.epiLength=epiLength;
	}		

	public static void printTStatsList(ArrayList<TStats> csList) {
		DecimalFormat form = new DecimalFormat("000");
		Iterator it = csList.iterator();
	    while (it.hasNext()) {
		    TStats tint = (TStats)it.next();
		    System.out.println(form.format(tint.n) + ", " + form.format(tint.p) + ", "+ tint.moveNum 
		    		+ ", epiLength="+tint.epiLength);
        } 		
	}

	public static void printLastTStats(ArrayList<TStats> csList) {
		DecimalFormat form = new DecimalFormat("000");
		TStats tint = csList.get(csList.size()-1);
	    System.out.println(form.format(tint.n) + ", " + form.format(tint.p) + ", "+ tint.moveNum 
	    		+ ", epiLength="+tint.epiLength);
	}

	/**
	 * Nested class for aggregating the results in a list of {@link TStats} objects: All objects 
	 * with a given {@code p} (minimum episode length) are aggregated to obtain: <ul>
	 *  <li> <b>size</b> 		the count
	 *  <li> <b>percSolved</b> 	the percentage of episodes solved in minimum episode length  
	 *  <li> <b>percLonger</b>	the percentage of longer episodes, but below max. episode length
	 *  <li> <b>epiLength</b>	the percentage of episodes with maximum episode length
	 *  </ul>
	 *  
	 *  This class is mainly useful for game RubiksCube, but may not be completely useless
	 *  for other (puzzle) games as well.
	 */
	public static class TAggreg {
		int size;
		int p;
		double percSolved;
		double percLonger;
		double percNotSol;
		
		public TAggreg(ArrayList<TStats> tsList, int p) {
			Iterator it = tsList.iterator();
			int nSolved=0;
			int nLonger=0;
			int nNot=0;
			int size=0;
		    while (it.hasNext()) {
			    TStats cs = (TStats)it.next();
			    if (cs.p==p) {
			    	size++;
				    this.p = cs.p;
				    if (cs.moveNum==cs.p) nSolved++;
				    if (cs.p<cs.moveNum && cs.moveNum<cs.epiLength) nLonger++;
				    if (cs.moveNum==cs.epiLength) nNot++;			    	
			    }
	        } 
		    this.size = size;
			this.percSolved = ((double)nSolved)/size;
			this.percLonger = ((double)nLonger)/size;
			this.percNotSol = ((double)nNot)/size;
		}		

	} // nested class TAggreg
	
	public static void printTAggregList(ArrayList<TAggreg> taList) {
		DecimalFormat form = new DecimalFormat("000");
		DecimalFormat fper = new DecimalFormat("0.0%"); 
		Iterator it = taList.iterator();
	    while (it.hasNext()) {
			TAggreg tint = (TAggreg)it.next();
		    System.out.println(form.format(tint.p) + ", " + form.format(tint.size) + ": "
		    		+ fper.format(tint.percSolved) + ", "
		    		+ fper.format(tint.percLonger) + ", "
		    		+ fper.format(tint.percNotSol) );
        } 		
	}
	
	/**
	 * @param taList
	 * @return the average 'solved' percentage of taList
	 */
	public static double avgResTAggregList(ArrayList<TAggreg> taList) {
		Iterator it = taList.iterator();
		double res=0;
	    while (it.hasNext()) {
			TAggreg tagg = (TAggreg)it.next();
			res += tagg.percSolved;
        } 		
		return res/taList.size();
	}

	/**
	 * @param taList
	 * @param weight the weights w, each entry in taList gets the relative weight w[p]/sum(w[p])
	 * @param mode =0: percent solved within minimal twists, =1: percent solved below epiLength
	 * @return the weighted average of 'solved' percentages in taList
	 */
	public static double weightedAvgResTAggregList(ArrayList<TAggreg> taList, int[] weight, int mode) {
		assert (weight.length >= taList.size());
		Iterator it = taList.iterator();
		double res=0;
		double wghtSum = 0.0;
		double val;
		int count=0;
	    while (it.hasNext()) {
			TAggreg tagg = (TAggreg)it.next();
			val = (mode==0) ? tagg.percSolved : (1-tagg.percNotSol);
			wghtSum += weight[count];
			res += val * weight[count++];
        } 		
		return res/wghtSum;
	}

} // class TStats
