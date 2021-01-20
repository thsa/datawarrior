package com.actelion.research.table.view.card;

import com.actelion.research.table.model.CompoundRecord;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;


public class JCardApp {

    public static  class DummyRecord extends CompoundRecord{
        public DummyRecord(){
            super((int) Math.random()*10000000, 5);
        }
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);

        //Create and set up the window.
        JFrame frame = new JFrame("HelloWorldSwing");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        //JCardPane cardPane = new JCardPane(null, null, null,null);
        JCardPane cardPane = new JCardPane(null, null );

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(cardPane,BorderLayout.CENTER);

        //frame.getContentPane().add(label);

        //Display the window.
        frame.pack();
        frame.setVisible(true);

        frame.setSize(800,800);


        for(int ci = 0; ci<4000; ci++){
            CompoundRecord cd = new JCardApp.DummyRecord();
            ArrayList<CompoundRecord> list = new ArrayList<>();
            list.add(cd);
            double max_radius  = 2000;
            double rand_radius = (Math.random()*max_radius);
            double rand_angle  = Math.random()*2*Math.PI;
            cardPane.addCardElement( list , (int) (rand_radius * Math.cos(rand_angle)) , (int) (rand_radius * Math.sin(rand_angle)) );
        }
        //cardPane.recomputeHiddenCardElements(true);
    }


    public static void main(String args[]){
        createAndShowGUI();
    }


}
