package com.actelion.research.table.view.card.positioning;

import com.actelion.research.table.view.card.CardElement;
import com.actelion.research.table.view.card.CardPaneModel;
import com.actelion.research.table.view.card.cardsurface.gui.JComboBoxForTableColumns;
import com.actelion.research.table.view.card.cardsurface.gui.JMyOptionsTable;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class XYSorter extends CardPositionerWithGUI{

    public static final String NAME = "XYSorter";


    private CardNumericalSorter mSorterX = null;
    private CardNumericalSorter mSorterY = null;

    private List<CardElement>   mElements = null;


    public XYSorter(CompoundTableModel model, List<CardElement> elements){

        this.mElements = elements;

        ArrayList<Integer> numericalRows = new ArrayList<>();
        for(int zi=0;zi<model.getColumnCount();zi++){
            if(model.isColumnTypeDouble( zi )){ numericalRows.add(zi); }
        }
        int colX = -1; int colY = -1;
        if(numericalRows.size()==1){ colX = numericalRows.get(0); colY = numericalRows.get(0); }
        if(numericalRows.size()>1){  colX = numericalRows.get(0); colY = numericalRows.get(1); }


        // init sorters with default values..
        this.mSorterX = CardNumericalSorter.createNumericalSorter(model , mElements, colX, CardNumericalSorter.AXIS_X);
        this.mSorterY = CardNumericalSorter.createNumericalSorter(model , mElements, colY, CardNumericalSorter.AXIS_Y);
        this.setTableModel(model);
    }

    public String getName(){
        return NAME;
    }

    @Override
    public JPanel getGUI() {
        XYSorterConfigPanel2 configPanel = new XYSorterConfigPanel2();
        //configPanel.mComboBoxXColumn.setSelectedItem(this.mSorterX.getColumn());
        //configPanel.mComboBoxYColumn.setSelectedItem(this.mSorterY.getColumn());
        return configPanel;
    }


    public CardNumericalSorter getSorterX() {return mSorterX;}

    public CardNumericalSorter getSorterY() {return mSorterY;}

    public void setTableModel(CompoundTableModel tableModel){
        super.setTableModel(tableModel);

        this.mSorterX.setTableModel(tableModel);
        this.mSorterY.setTableModel(tableModel);
    }



    @Override
    public boolean requiresTableModel() {
        return true;
    }

    @Override
    public boolean supportPositionSingleCard() {
        return false;
    }

    @Override
    public boolean shouldCreateStacks() {
        return false;
    }

    @Override
    public void configure(Properties p) {

    }

    @Override
    public Properties getConfiguration() {
        return null;
    }

    @Override
    public Point2D positionSingleCard(CardPaneModel.CardElementInterface ce, CompoundRecord cr) {

        double px = this.mSorterX.positionSingleCard(ce, cr).getX();
        double py = this.mSorterY.positionSingleCard(ce, cr).getY();

        //System.out.println("POS: "+px+"/"+py);

        if(px>1000000){px=1000000;}
        if(py>1000000){py=1000000;}
        if(Double.isNaN(px)){px=0;}
        if(Double.isNaN(py)){py=0;}

        return new Point2D.Double(px,py);
    }


    class XYSorterConfigPanel2 extends JPanel implements JMyOptionsTable.OptionTableListener {

        private JMyOptionsTable mOptionsTable;

        public XYSorterConfigPanel2() {
            initOptionsTable();
            this.setLayout(new BorderLayout());
            this.add(mOptionsTable,BorderLayout.CENTER);
            this.setVisible(true);
            //this.setBackground(Color.blue);
        }

        @Override
        public void optionTableEventHappened(JMyOptionsTable.OptionTableEvent event) {
            if(event.isAdjusting()){return;}

            Properties p = mOptionsTable.collectValues();

            getThis().mSorterX.setColumn( Integer.parseInt( (String) p.get(PROPERTY_NAME_COLUMN_X) ) , mElements );
            getThis().mSorterY.setColumn( Integer.parseInt( (String) p.get(PROPERTY_NAME_COLUMN_Y) ) , mElements );

            getThis().mSorterX.setStretch( Double.parseDouble((String) p.get(PROPERTY_NAME_STRETCH_X)) );
            getThis().mSorterY.setStretch( Double.parseDouble((String) p.get(PROPERTY_NAME_STRETCH_Y)) );

            getThis().mSorterX.setJitter( Double.parseDouble((String) p.get(PROPERTY_NAME_JITTER_X )) );
            getThis().mSorterY.setJitter( Double.parseDouble((String) p.get(PROPERTY_NAME_JITTER_Y )) );

            getThis().fireSortingConfigEvent(getThis(),"Something changed",true);
        }


        public static final String PROPERTY_NAME_COLUMN_X  = "ColumnX";
        public static final String PROPERTY_NAME_COLUMN_Y  = "ColumnY";
        public static final String PROPERTY_NAME_STRETCH_X = "StretchX";
        public static final String PROPERTY_NAME_STRETCH_Y = "StretchY";
        public static final String PROPERTY_NAME_JITTER_X  = "JitterX";
        public static final String PROPERTY_NAME_JITTER_Y  = "JitterY";

        public void initOptionsTable(){
            //mOptionsTable = new JMyOptionsTable(1,6);
            mOptionsTable = new JMyOptionsTable();

            //mOptionsTable.setComboBoxDoubleAndCategoricalColumns(0,0,PROPERTY_NAME_COLUMN, mModel);
            mOptionsTable.setComboBoxDoubleAndCategoricalColumns(0,0,PROPERTY_NAME_COLUMN_X,"Column X:", getTableModel() );
            mOptionsTable.setComboBoxDoubleAndCategoricalColumns(0,1,PROPERTY_NAME_COLUMN_Y,"Column Y:", getTableModel() );

            // set columns..
            mOptionsTable.setValue(0,0,mSorterX.getColumn());
            mOptionsTable.setValue(0,1,mSorterY.getColumn());

            mOptionsTable.setSlider(0,2,PROPERTY_NAME_STRETCH_X, "Stretch X:",0,1,0.25);
            mOptionsTable.setSlider(0,3,PROPERTY_NAME_STRETCH_Y, "Stretch Y:",0,1,0.25);

            mOptionsTable.setSlider(0,4,PROPERTY_NAME_JITTER_X, "Jitter X:" ,0,1,0.0);
            mOptionsTable.setSlider(0,5,PROPERTY_NAME_JITTER_Y, "Jitter Y:" ,0,1,0.0);

            mOptionsTable.reinit();

            mOptionsTable.registerOptionTableListener(this);
        }

    }



    public XYSorter getThis(){
        return this;
    }

    public static XYSorter createDefaultXYSorter(CompoundTableModel ctm, List<CardElement> ceList) {
        XYSorter sorter = new XYSorter(ctm,ceList);
        return sorter;
    }




    class XYSorterConfigPanel extends JPanel implements ActionListener , ChangeListener {


        /**
         * Column choice
         */
        private JComboBoxForTableColumns mComboBoxXColumn = null;
        private JComboBoxForTableColumns mComboBoxYColumn = null;

        /**
         * Stretch
         */
        private JSlider mSliderStretchX = new JSlider();
        private JSlider mSliderStretchY = new JSlider();

        /**
         * Jitter
         */
        private JSlider mSliderJitterX = new JSlider();
        private JSlider mSliderJitterY = new JSlider();


        public XYSorterConfigPanel(){
            initConfigPanel();

            mComboBoxXColumn.setSize(400,20);
            mComboBoxYColumn.setSize(400,20);
            mComboBoxXColumn.setMaximumSize(new Dimension(400,20));
            mComboBoxYColumn.setMaximumSize(new Dimension(400,20));

            this.setSize(600,600);
            this.setVisible(true);
            //this.setBackground(Color.green.darker());
        }

        public void initConfigPanel(){


//            // Columns Choice Panel
//            for( int zi=0; zi<getModel().getColumnCount();zi++){
//                //if(getModel().isColumnTypeDouble(zi) || getModel().isColumnTypeCategory(zi)){
//                if(getModel().isColumnTypeDouble(zi)){
//                    mComboBoxXColumn.addItem(zi);
//                    mComboBoxYColumn.addItem(zi);
//                }
//            }

            mComboBoxXColumn = JComboBoxForTableColumns.createComboBoxDoubleAndCategorical(getTableModel());
            mComboBoxYColumn = JComboBoxForTableColumns.createComboBoxDoubleAndCategorical(getTableModel());


            this.removeAll();
            this.setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));

            JPanel panelColumns = new JPanel();
            panelColumns.setLayout(new BoxLayout(panelColumns,BoxLayout.X_AXIS));
            panelColumns.setAlignmentX(LEFT_ALIGNMENT);
            panelColumns.add(new JLabel("Column X"));
            panelColumns.add(mComboBoxXColumn);
            panelColumns.add(new JLabel("Column Y"));
            panelColumns.add(mComboBoxYColumn);

            this.add(panelColumns);

            mComboBoxXColumn.addActionListener(this);
            mComboBoxYColumn.addActionListener(this);


            // "Stretch" Sliders:

            mSliderStretchX.setMinimum(0); mSliderStretchX.setMaximum(100);
            mSliderStretchY.setMinimum(0); mSliderStretchY.setMaximum(100);
            mSliderStretchX.setMinorTickSpacing(1); mSliderStretchY.setMinorTickSpacing(1);
            mSliderStretchX.setValue(50);  mSliderStretchY.setValue(50);

            mSliderStretchX.setSize(400,20);
            mSliderStretchY.setSize(400,20);

            mSliderStretchX.addChangeListener(this);
            mSliderStretchY.addChangeListener(this);



            JPanel panelStretch = new JPanel();

            panelStretch.setLayout(new BoxLayout(panelStretch,BoxLayout.X_AXIS));
            panelStretch.setAlignmentX(LEFT_ALIGNMENT);
            panelStretch.add(new JLabel("Stretch X"));
            panelStretch.add(mSliderStretchX);
            panelStretch.add(new JLabel("Stretch Y"));
            panelStretch.add(mSliderStretchY);

            this.add(panelStretch);

            // "Jitter" Sliders:
            mSliderJitterX.setMinimum(0); mSliderJitterX.setMaximum(100);
            mSliderJitterX.setValue(0);
            mSliderJitterY.setMinimum(0); mSliderJitterY.setMaximum(100);
            mSliderJitterY.setValue(0);

            mSliderJitterX.addChangeListener(this);
            mSliderJitterY.addChangeListener(this);

            JPanel panelJitter = new JPanel();
            panelJitter.setLayout(new BoxLayout(panelJitter,BoxLayout.X_AXIS));
            panelJitter.setAlignmentX(LEFT_ALIGNMENT);

            panelJitter.add(new JLabel("Jitter X"));
            panelJitter.add(mSliderJitterX);
            panelJitter.add(new JLabel("Jitter Y"));
            panelJitter.add(mSliderJitterY);

            this.add(panelJitter);

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean changed = false;
            if(mComboBoxXColumn!=null && mSorterX!=null) {
                if(mComboBoxXColumn.getSelectedItem()!=null) {
                    mSorterX.setColumn((Integer) mComboBoxXColumn.getSelectedItem(), mElements);
                    changed = true;
                }
            }
            if(mComboBoxYColumn!=null && mSorterY!=null) {
                if(mComboBoxYColumn.getSelectedItem()!=null) {
                    mSorterY.setColumn((Integer) mComboBoxYColumn.getSelectedItem(), mElements);
                    changed = true;
                }
            }

            if(changed){
                getThis().fireSortingConfigEvent(getThis(), "XYSorter::ColumnChanged", true);
            }
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            int sxi = mSliderStretchX.getValue();
            int syi = mSliderStretchY.getValue();

            int jxi = mSliderJitterX.getValue();
            int jyi = mSliderJitterY.getValue();


            double sx = 1.0+(sxi-50.0)/100.0; //Math.pow( (sxi-50.0)/50.0 , 3);
            double sy = 1.0+(syi-50.0)/100.0; //Math.pow( (syi-50.0)/50.0 , 3);

            double jx = 0.5 * Math.pow( jxi/100.0 , 2 ); //Math.pow( (sxi-50.0)/50.0 , 3);
            double jy = 0.5 * Math.pow( jyi/100.0 , 2 ); //Math.pow( (syi-50.0)/50.0 , 3);


            JSlider source = (JSlider)e.getSource();
            if (source.getValueIsAdjusting()) {

            }
            else {
                System.out.println("set stretch: " + sx + " / " + sy);
                System.out.println("set jitter:  " + jx + " / " + jy);
                mSorterX.setStretch(sx);
                mSorterY.setStretch(sy);

                mSorterX.setJitter(jx);
                mSorterY.setJitter(jy);

                getThis().fireSortingConfigEvent(getThis(), "XYSorter::ColumnChanged", true);
            }
        }
    }



}
