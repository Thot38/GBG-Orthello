package TournamentSystem.tools;

import TournamentSystem.jheatchart.HeatChart;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;

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
        ArrayList<Double> al = new ArrayList<>();
        al.add(5.0);
        al.add(2.0);
        al.add(1.0);
        al.add(8.0);
        al.add(3.0);
        System.out.println(Collections.min(al));

        String a = "a";
        String b = a;
        System.out.println(b);
        a = "c";
        System.out.println(b);
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
        HeatChart map = new HeatChart(dataHM, 1, 7);
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
