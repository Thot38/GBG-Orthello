package TournamentSystem.tools;

import TournamentSystem.jheatchart.HeatChart;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This class is just used to test code outside the GBG and GBG-TS.
 *
 * @author Felix Barsnick, University of Applied Sciences Cologne, 2018
 */
@Deprecated
public class Stuff {

    public static void main(String[] args) { new Stuff(); }

    //--------------------------------------

    public Stuff() {
        //makeJTable();
        /*
        ArrayList<Double> al = new ArrayList<>();
        al.add(5.0);
        al.add(2.0);
        al.add(1.0);
        al.add(8.0);
        al.add(3.0);
        System.out.println(Collections.min(al));
        */
        /*
        String a = "a";
        String b = a;
        System.out.println(b);
        a = "c";
        System.out.println(b);
        */
        /*
        int i = 5;
        System.out.println((i/2)+1);
        */
        /*
        for (int j=0; j<15; j++)
            System.out.println("random "+getRandomNum(1,9));
        */

        int[] safe = {1,2,3,4,5};
        //System.out.println(Arrays.toString(getNRandomNums(1, 15, 8, safe)));
        for (int i=0; i<safe.length; i+=2) {
            if (i+1 == safe.length)
                System.out.println(safe[i] + " " + safe[0]);
            else
                System.out.println(safe[i] + " " + safe[i + 1]);
        }
    }

    private int[] getNRandomNums(int low, int high, int count, int[] safe) {
        int[] randoms = new int[count];
        int pos = 0;

        while (pos<count) {
            boolean failed = false;
            int rnd = getRandomNum(low, high);

            for (int i:safe) {
                if (i == rnd) {
                    failed = true;
                }
            }

            for (int i=0; i<pos; i++) {
                if (randoms[i] == rnd) {
                    failed = true;
                }
            }

            if (!failed) {
                randoms[pos++] = rnd;
            }
        }

        return randoms;
    }

    private int getRandomNum(int low, int high) {
        return ThreadLocalRandom.current().nextInt(low, high + 1);
    }

    private void makeJTable() {
        // http://www.codejava.net/java-se/swing/a-simple-jtable-example-for-display
        //headers for the table
        String[] columns = new String[] {
                "Y vs X", "Agent#1", "Agent#2", "Agent#3"
        };

        //actual data for the table in a 2d array
        /*
        Object[][] data = new Object[][] {
                {1, "John",  40.0, false },
                {2, "Rambo", 70.0, false },
                {3, "Zorro", 60.0, true },
        };
        */
        String s = "W:1 | T:1 | L:1";
        Object[][] data = new Object[][] {
                {"Agent#1", "xxx",  s, s},
                {"Agent#2", s,  "xxx", s},
                {"Agent#3", s,  s, "xxx"},
        };
        //create table with data
        JTable table = new JTable(data, columns);
        JTable table2 = new JTable(data, columns);

        /** Make Heatmap
         * http://www.tc33.org/jheatchart/javadoc/
         * http://www.javaheatmap.com/
         */
        // Create some dummy data.
        double[][] dataHM = {
                {3,2,0,4,5,6},
                {2,1,4,0,6,7},
                {3,4,5,6,0,6},
                {4,5,6,7,6,0}
        };
        System.out.println("HeatChart.max(dataHM): "+ HeatChart.max(dataHM));

        // Step 1: Create our heat map chart using our data.
        HeatChart map = new HeatChart(dataHM, 1, 7, false);
        //HeatChart map = new HeatChart(dataHM);

        // Step 2: Customise the chart.
        map.setTitle("This is my heat chart title");
        map.setXAxisLabel("X Axis");
        map.setYAxisLabel("Y Axis");
        Object[] tmpX = {"Agent1","Agent2","Agent3","Agent4","Agent5","Agent6"};
        map.setXValues(tmpX);
        Object[] tmpY = {"Agent1","Agent2","Agent3","Agent4"};
        map.setYValues(tmpY);
        map.setCellSize(new Dimension(50,50));

        // Step 3: Output the chart to a file.
        //map.saveToFile(new File("java-heat-chart.png"));
        Image hm = map.getChartImage();

        /** End Heatmap*/

        //add the table to the frame
        JFrame frame = new JFrame();
        Container c  = frame.getContentPane();
        c.setLayout(new GridLayout(3,0));
        c.add(new JScrollPane(table));
        c.add(new JScrollPane(table2));
        c.add(new JLabel(new ImageIcon(hm)));

        frame.setTitle("Table Example");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
