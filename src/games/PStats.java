package games;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Locale;

import controllers.PlayAgent;
import games.Arena;
import tools.Types;
import tools.Types.ACTIONS_VT;

/**
 *  Class PStats holds the results from one move during game play.
 *  When playing one or multiple games, an object {@code ArrayList<PStats> psList} is 
 *  created and finally written with
 *  {@link PStats#printPlayStats(ArrayList, PlayAgent, Arena)} <br>
 *  to file {@code playStats.csv}. <p>
 *  
 *  This class is currently in part specific to game 2048 (it records for example the number of 
 *  empty tiles). This is the reason why it is needed in addition to the information stored
 *  by {@link games.LogManager}. If called in other games, PStats receives just 0 for the 
 *  number of empty tiles.<p>
 *   
 *  Class PStats is just for diagnostic purposes, it has no influence on the game strategy. 
 *  
 *  @see Arena#PlayGame()
 *  @see games.ZweiTausendAchtundVierzig.Evaluator2048#evalAgent(PlayAgent)
 *  @see games.MTrain
 */
public class PStats {
	public int i;				// episode number (if needed)
	public int moveNum;			// move number within a play game episode
	public int player;			// player who has to move in current state
	public int action;			// action int taken to *create* current state (-1 if not known)
	public double gScore;		// game score (cumulative reward, from perspective of 'player')
	public double nEmptyTile; 	// actual number of empty tiles (specific to 2048)
	public double cumEmptyTl; 	// cumulative number of empty tiles (specific to 2048)
	
	/**
	 * 
	 * @param i	run number
	 * @param moveNum move number
	 * @param gScore game score (cumulative reward, from perspective of 'player')
	 * @param nEmptyTile actual number of empty tiles (specific to 2048)
	 * @param cumEmptyTiles cumulative number of empty tiles (specific to 2048)
	 */
	public PStats(int i, int moveNum, double gScore, double nEmptyTile, double cumEmptyTiles) {
		this.i=i;
		this.moveNum=moveNum;
		this.gScore=gScore;
		this.nEmptyTile=nEmptyTile;
		this.cumEmptyTl=cumEmptyTiles;
		this.player = 0;
		this.action = -1; 
	}
	
	/**
	 * 
	 * @param i	run number
	 * @param moveNum move number
	 * @param player player who has to move in current state
	 * @param action action int taken to *create* current state (-1 if not known)
	 * @param gScore game score (cumulative reward, from perspective of 'player')
	 * @param nEmptyTile actual number of empty tiles (specific to 2048)
	 * @param cumEmptyTiles cumulative number of empty tiles (specific to 2048)
	 */
	public PStats(int i, int moveNum, int player, int action, double gScore, double nEmptyTile, double cumEmptyTiles) {
		this.i=i;
		this.moveNum=moveNum;
		this.gScore=gScore;
		this.nEmptyTile=nEmptyTile;
		this.cumEmptyTl=cumEmptyTiles;
		this.player = player;
		this.action = action; 
	}
	
	public void print(PrintWriter mtWriter)  {
		DecimalFormat df = new DecimalFormat();				
		df = (DecimalFormat) NumberFormat.getNumberInstance(Locale.UK);		
		df.applyPattern("+0.00;-0.00");  
		DecimalFormat form = new DecimalFormat("+0.00;-0.00");
		String sep = ", ";
		mtWriter.print(i + sep + moveNum + sep + player + sep + action + sep);
		mtWriter.println(df.format(gScore) + sep + (int)(nEmptyTile+0.5) + sep + (int)(cumEmptyTl+0.5));
	}
	
	/**
	 * Print the results from playing one or multiple games to
	 * file <br>
	 *    {@link Types#GUI_DEFAULT_DIR_AGENT}{@code /<gameName>[/subDir]/csv/playStats.csv} <br>
	 * where the optional {@code subdir} is for games with different flavors (like Hex: board size). 
	 * The directory of the file is created, if it does not exist.   
	 * 
	 * @param psList	the results from playing the game(s)
	 * @param pa		the agent(s) used when playing a game 
	 * @param ar		needed for accessing {@code gameName} and the (optional) {@code subDir}
	 */
	public static void printPlayStats(ArrayList<PStats> psList, StateObservation startSO, PlayAgent[] paVector, Arena ar){
		String strDir = Types.GUI_DEFAULT_DIR_AGENT+"/"+ar.getGameName();
		String subDir = ar.getGameBoard().getSubDir();
		if (subDir != null){
			strDir += "/"+subDir;
		}
		strDir += "/csv";
		tools.Utils.checkAndCreateFolder(strDir);

		boolean retry=true;
		PrintWriter mtWriter = null;
		BufferedReader bufIn=new BufferedReader(new InputStreamReader(System.in));
		while (retry) {
			try {
				mtWriter = new PrintWriter(new FileWriter(strDir+"/"+"playStats.csv",false));
				retry = false;
			} catch (IOException e) {
				try {
					// We may get here if playStats.csv is open in another application (e.g. Excel).
					// Here we give the user the chance to close the file in the other application:
				    System.out.print("*** Warning *** Could not open "+strDir+"/"+"playStats.csv. Retry? (y/n): ");
				    String s = bufIn.readLine();
				    retry = (s.contains("y")) ? true : false;
				} catch (IOException e2) {
					e2.printStackTrace();					
				}
			}			
		}
		
		if (mtWriter!=null) {
			for (int i=0; i<paVector.length; i++) {
				mtWriter.println("P"+i+": "+paVector[i].stringDescr());		
				mtWriter.println("P"+i+": "+paVector[i].stringDescr2());				
			}
			if (startSO!=null) mtWriter.println("Start state = "+startSO.stringDescr());
			
			mtWriter.println("run, moveNum, player, action, gameScore, nEmptyTile, cumEmptyTl");
			ListIterator<PStats> iter = psList.listIterator();		
			while(iter.hasNext()) {
				(iter.next()).print(mtWriter);
			}

		    mtWriter.close();			
		} else {
			System.out.print("*** Warning *** Could not write "+strDir+"/"+"playStats.csv.");
		}
	}

}

