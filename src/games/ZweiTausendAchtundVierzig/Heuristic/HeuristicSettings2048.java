package games.ZweiTausendAchtundVierzig.Heuristic;

import controllers.MCTSExpectimax.MCTSExpectimaxAgt;
import games.ZweiTausendAchtundVierzig.StateObserver2048;
import params.ParMCTSE;
import tools.Types;
import tools.cmaes.fitness.IObjectiveFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Johannes Kutsch on 27.06.2017.
 */
public class HeuristicSettings2048 implements IObjectiveFunction {
    private int geneLength = 15;
    private double[] genes;
    public double fitness;
    private Random random = new Random();
    private ExecutorService executorService = Executors.newWorkStealingPool();

    //empty tiles
    public boolean enableEmptyTiles = true;
    public double emptyTilesWeighting0 = 0;
    public double emptyTilesWeighting1 = 0.7214131449043863;
    public double emptyTilesWeighting2 = 0;
    public int emptyTilesMethod = 1;            //valid methods are 0 => numeEmptyTiles^weighting, 1 => highestTileValue * numEmptyTile * weighting, 2 => score * numEmptyTile * Weighting

    //highest tile in corner
    public boolean enableHighestTileInCorner = true;
    public double highestTileIncornerWeighting = 0.48066258192020583;

    //row
    public boolean enableRow = true;
    public double rowWeighting0 = 0.4778659656963287;
    public double rowWeighting1 = 0;
    public int rowMethod = 0;                   //valid methods are 0 => tile in a row has to be lower then the previous tile, 1 => tile in a row is exactly half of the previous tile

    //merge
    public boolean enableMerge = true;
    public double mergeWeighting = 0.8079838161879548;

    //rollout
    public boolean enableRollout = true;
    public double rolloutWeighting = 0.8435028456190605;

    public HeuristicSettings2048() {
        genes = new double[geneLength];
        for (int i = 0; i < genes.length; i++) {
            genes[i] = random.nextDouble();
        }

        applyGenes();
    }

    /**
     * calculate the fitness
     */
    public void calcFitness () {
        System.out.print("Calculating Fitness");
        if(!enableEmptyTiles && !enableHighestTileInCorner && !enableRow && !enableMerge && !enableRollout) {
            fitness = 0;
            return;
        }

        List<Callable<StateObserver2048>> callables = new ArrayList<>();
//        MCTSExpectimaxParams mctseParams = new MCTSExpectimaxParams();
        ParMCTSE mctseParams = new ParMCTSE();
        mctseParams.setHeuristicSettings2048(this);
        mctseParams.setNumIter(3500);
        mctseParams.setMaxNodes(500);
        mctseParams.setRolloutDepth(150);
        mctseParams.setTreeDepth(10);
        mctseParams.setEnableHeuristics(true);

        double totScore = 0;


        for(int i = 0; i < 25; i++) {
            callables.add(() -> {
                MCTSExpectimaxAgt mctseAgt = new MCTSExpectimaxAgt("MCTSE", mctseParams);

                StateObserver2048 so = new StateObserver2048();

                while (!so.isGameOver()) {
                    Types.ACTIONS action = mctseAgt.getNextAction2(so, false, true);
                    so.advance(action);
                }

                System.out.print(".");

                return so;
            });
        }

        List<StateObserver2048> stateObservers = new ArrayList<>();

        try {
            executorService.invokeAll(callables).stream().map(future -> {
                try {
                    return future.get();
                }
                catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).forEach(stateObservers::add);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (StateObserver2048 so: stateObservers) {
            totScore += so.score;
        }

        fitness = totScore/25;
        System.out.println("\nFitness is: " + fitness);
        System.out.println(toString());
    }

    /**
     * apply the current genes
     */
    private void applyGenes() {
        enableEmptyTiles = genes[0] < 0.5;
        emptyTilesWeighting0 = genes[1];
        emptyTilesWeighting1 = genes[2];
        emptyTilesWeighting2 = genes[3];
        emptyTilesMethod = (int) (genes[4] * 3);

        enableHighestTileInCorner = genes[5] < 0.5;
        highestTileIncornerWeighting = genes[6];

        enableRow = genes[7] < 0.5;
        rowWeighting0 = genes[8];
        rowWeighting1 = genes[9];
        rowMethod = (int) (genes[10] * 2);

        enableMerge = genes[11] < 0.5;
        mergeWeighting = genes[12];

        enableRollout = genes[13] < 0.5;
        rolloutWeighting = genes[14];
    }

    /**
     * create a string which containes the current settings
     *
     * @return the string
     */
    public String toString() {
        String string = "enableEmptyTiles: " + enableEmptyTiles +
                "\nemptyTilesMethod: " + emptyTilesMethod;
        switch (emptyTilesMethod) {
            case 0:
                string += "\nemptyTilesWeighting: " + emptyTilesWeighting0;
                break;
            case 1:
                string += "\nemptyTilesWeighting: " + emptyTilesWeighting1;
                break;
            case 2:
                string += "\nemptyTilesWeighting: " + emptyTilesWeighting2;
                break;
        }

        string += "\nenableHighestTileInCorner: " + enableHighestTileInCorner +
                "\nhighestTileIncornerWeighting: " + highestTileIncornerWeighting;

        string += "\nenableRow: " + enableRow +
                "\nrowMethod: " + rowMethod;

        switch (rowMethod) {
            case 0:
                string += "\nrowWeighting: " + rowWeighting0;
                break;
            case 1:
                string += "\nrowWeighting: " + rowWeighting1;
                break;
        }

        string += "\nenableMerge: " + enableMerge +
                "\nmergeWeighting: " + mergeWeighting;

        string += "\nenableRollout: " + enableRollout +
                "\nrolloutWeighting: " + rolloutWeighting;

        return string;
    }

    @Override
    public double valueOf(double[] x) {
        genes = x;
        applyGenes();
        calcFitness();
        return 1-(fitness/100000);
    }

    @Override
    public boolean isFeasible(double[] x) {
        for(int i = 0; i < x.length; i++) {
            if(x[i] < 0 || x[i] > 1) {
                return false;
            }
        }

        genes = x;
        applyGenes();
        if(!enableRollout && !enableMerge && !enableHighestTileInCorner && !enableEmptyTiles && !enableRow) {
            return false;
        } else {
            return true;
        }
    }
}