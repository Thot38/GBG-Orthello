package games.ZweiTausendAchtundVierzig;

import tools.Types;

import javax.swing.*;

import games.Arena;
import games.ArenaTrain;
import games.XArenaFuncs;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

/**
 * Launch class used to start game 2048 in class {@link Arena} via 
 * a <b>main method</b>: <br> 
 *  
 * @author Wolfgang Konen, TH Cologne, Nov'16
 * 
 * @see Arena
 * @see ArenaTrain
 * @see XArenaFuncs
 */
public class LaunchArena2048 extends JFrame{
    private static final long serialVersionUID = 1L;
    public Arena2048 m_Arena;

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException
    {
        LaunchArena2048 t_Frame = new LaunchArena2048("General Board Game Playing");

        if (args.length==0) {
            t_Frame.init();
        } else {
            throw new RuntimeException("[LaunchArena2048.main] args="+args+" not allowed. Use batch facility.");
        }

    }

    /**
     * Initialize the frame and {@link #m_Arena}.
     */
    public void init()
    {
        addWindowListener(new LaunchArena2048.WindowClosingAdapter());
        m_Arena.init();
        setSize(Types.GUI_ARENATRAIN_WIDTH,Types.GUI_ARENATRAIN_HEIGHT);
        setBounds(0,0,Types.GUI_ARENATRAIN_WIDTH,Types.GUI_ARENATRAIN_HEIGHT);
        //pack();
        setVisible(true);
    }

    public LaunchArena2048(String title) {
        super(title);
        m_Arena = new Arena2048(this);
        setLayout(new BorderLayout(10,10));
        setJMenuBar(m_Arena.m_menu);
        add(m_Arena,BorderLayout.CENTER);
    }

    protected static class WindowClosingAdapter
            extends WindowAdapter
    {
        public WindowClosingAdapter()  {  }

        public void windowClosing(WindowEvent event)
        {
            event.getWindow().setVisible(false);
            event.getWindow().dispose();
            System.exit(0);
        }
    }
}
