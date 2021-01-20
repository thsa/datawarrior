package com.actelion.research.table.view.card.positioning;

import com.actelion.research.table.view.card.CardElement;
import com.actelion.research.table.view.card.CardPaneModel;
import com.actelion.research.table.view.card.cardsurface.gui.JMyOptionsTable;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class XSorter extends CardPositionerWithGUI {


    public static final String NAME = "XSorter";


    private CardNumericalShaper1D mSorterX = null;

    private SpiralOutPositioner   mSpiralOutPositioner = null;


    private List<CardElement> mElements = null;

    private CompoundTableModel mModel = null;

    private int mColumn = -1;

    public void setColumn(int col){this.mColumn = col;}

    /**
     * Indicates the mode. We support some modes from CardNumericalShaper1D, and the SpiralOutPositioner mode.
     *
     */
    private String mMode = CardNumericalShaper1D.MODE_GRID;


    /**
     * Contains all modes for CardNumericalShaper1D
     */
    private static List<String> mModesShaper1D = new ArrayList<>();


    static{
        mModesShaper1D.add(CardNumericalShaper1D.MODE_GRID);
        mModesShaper1D.add(CardNumericalShaper1D.MODE_CIRCLE);
        mModesShaper1D.add(CardNumericalShaper1D.MODE_SPIRAL_X);
    }



    public XSorter(CompoundTableModel model, List<CardElement> elements){

        this.setModel(model);

        this.mElements = elements;

        ArrayList<Integer> numericalRows = new ArrayList<>();
        for(int zi=0;zi<model.getColumnCount();zi++){
            if(model.isColumnTypeDouble( zi )){ numericalRows.add(zi); }
        }
        int colX = -1; int colY = -1;
        if(numericalRows.size()==1){ colX = numericalRows.get(0); colY = numericalRows.get(0); }
        if(numericalRows.size()>1){  colX = numericalRows.get(0); colY = numericalRows.get(1); }


        // init sorters with default values..

        this.mSorterX = new CardNumericalShaper1D(mModel); // CardNumericalSorter.createNumericalSorter(model , mElements, colX, CardNumericalSorter.AXIS_X);
        this.mSpiralOutPositioner = new SpiralOutPositioner();

        this.setModel(model);
    }

    public String getName(){
        return NAME;
    }

    public void setModel(CompoundTableModel model){
        this.mModel = model;
        if(this.mSorterX!=null){ this.mSorterX.setTableModel(model); }
        if(this.mSpiralOutPositioner!=null){ this.mSpiralOutPositioner.setTableModel(model); }
    }

    @Override
    public JPanel getGUI() {
        XSorter.XSorterConfigPanel2 configPanel = new XSorter.XSorterConfigPanel2();
        //configPanel.mComboBoxXColumn.setSelectedItem(this.mSorterX.getColumn());
        //configPanel.mComboBoxXColumn.setSelectedItem(this.mSorterX.getColumn());

        return configPanel;
    }


    @Override
    public void configure(Properties p) {

    }

    @Override
    public Properties getConfiguration() {
        return null;
    }


    @Override
    public List<Point2D> positionAllCards(CompoundTableModel tableModel, List<CardElement> cards ) throws InterruptedException {
        if( mModesShaper1D.contains(mMode) ) {
            return this.mSorterX.positionAllCards(tableModel, cards);
        }
        if( mMode.equals( SpiralOutPositioner.MODE_SPIRAL_OUT )){
            this.mSpiralOutPositioner.setColumn(mColumn);
            return this.mSpiralOutPositioner.positionAllCards(tableModel, cards);
        }

        return null;
    }

    @Override
    public Point2D positionSingleCard(CardPaneModel.CardElementInterface ce, CompoundRecord cr) {
        //return this.mSorterX.positionSingleCard(ce,cr);
        return null;
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
    public boolean requiresTableModel() {
        return true;
    }

    public void setTableModel(CompoundTableModel tableModel){
        super.setTableModel(tableModel);
        this.setModel(tableModel);
    }


    class XSorterConfigPanel2 extends JPanel implements JMyOptionsTable.OptionTableListener {

        private JMyOptionsTable mOptionsTable;

        public XSorterConfigPanel2() {
            initOptionsTables();
            reinitLayout();
//            this.setLayout(new BorderLayout());
//            this.add(mOptionsTable,BorderLayout.CENTER);
//            this.setVisible(true);
            //this.setBackground(Color.blue);
        }

        /**
         * Just removes all components and adds them back together. Without reinit of any JMyOptionsTables..
         */
        public void reinitLayout(){
            this.removeAll();
            this.revalidate();

            if(false) {
                //this.setLayout(new BorderLayout());
                this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
                //this.add(mOptionsTable,BorderLayout.CENTER);
                this.add(mOptionsTable_ColAndShape);
                if (this.mRequireSpiralSettings) {
                    this.add(mOptionsTable_SpiralXOptions);
                }
                if (this.mRequireSpiralOutSettings) {
                    this.add(mOptionsTable_SpiralOutOptions);
                }
                if (this.mRequireBaseOptions) {
                    this.add(mOptionsTable_BaseOptions);
                }
                this.setVisible(true);
            }
            else{
                // layout..
                double  colsTL[] = new double[]{ TableLayout.PREFERRED };
                List<Double> rowsTL = new ArrayList<>();

                // for mOptionsTable_ColAndShape..
                rowsTL.add(TableLayout.PREFERRED);

                if (this.mRequireSpiralSettings) {
                    rowsTL.add(TableLayout.PREFERRED);
                }
                if (this.mRequireSpiralOutSettings) {
                    rowsTL.add(TableLayout.PREFERRED);
                }
                if (this.mRequireBaseOptions) {
                    rowsTL.add(TableLayout.PREFERRED);
                }
                this.setLayout(new TableLayout( colsTL , rowsTL.stream().mapToDouble(xi ->xi.doubleValue()).toArray()));

                int compCnt = 0;

                this.add(mOptionsTable_ColAndShape,0+","+compCnt);
                compCnt++;

                if (this.mRequireSpiralSettings) {
                    this.add(mOptionsTable_SpiralXOptions,0+","+compCnt);
                    compCnt++;
                }
                if (this.mRequireSpiralOutSettings) {
                    this.add(mOptionsTable_SpiralOutOptions,0+","+compCnt);
                    compCnt++;
                }
                if (this.mRequireBaseOptions) {
                    this.add(mOptionsTable_BaseOptions,0+","+compCnt);
                    compCnt++;
                }
                this.setVisible(true);
            }

            Window windowAncestor = SwingUtilities.getWindowAncestor(this);
            if(windowAncestor!=null){
                windowAncestor.pack();
            }

            this.revalidate();
            this.repaint();
        }



        @Override
        public void optionTableEventHappened(JMyOptionsTable.OptionTableEvent event) {
            if(event.isAdjusting()){return;}

            //Properties p = mOptionsTable.collectValues();
            Properties p1 = mOptionsTable_ColAndShape.collectValues();
            Properties p2 = mOptionsTable_BaseOptions.collectValues();
            Properties p3 = mOptionsTable_SpiralXOptions.collectValues();
            Properties p4 = mOptionsTable_SpiralOutOptions.collectValues();

            Properties p  = new Properties();
            p.putAll(p1);p.putAll(p2);p.putAll(p3);p.putAll(p4);

            try {

                mColumn = Integer.parseInt( p.getProperty(PROPERTY_NAME_COLUMN) );
                mMode   = p.getProperty(PROPERTY_NAME_SHAPE);

                if( mModesShaper1D.contains( p1.getProperty(PROPERTY_NAME_SHAPE) ) ) {
                    mRequireBaseOptions = true;

                    getThis().mSorterX.setStretchX(Double.parseDouble((String) p.get(PROPERTY_NAME_STRETCH_X)), mElements);
                    getThis().mSorterX.setStretchY(Double.parseDouble((String) p.get(PROPERTY_NAME_STRETCH_Y)), mElements);

                    getThis().mSorterX.setJitter(Double.parseDouble((String) p.get(PROPERTY_NAME_JITTER)), mElements);
                    getThis().mSorterX.setColumn(Integer.parseInt((String) p.get(PROPERTY_NAME_COLUMN)), mElements);
                    getThis().mSorterX.setShape((String) p.get(PROPERTY_NAME_SHAPE), mElements);

                    if (mMode.equals(CardNumericalShaper1D.MODE_SPIRAL_X)) {
                        mRequireSpiralSettings = true;

                        try {
                            mSorterX.setSpiralOptions(Double.parseDouble(p.getProperty(PROPERTY_NAME_SPIRALX_RADIUS_RATIO)),
                                    (int) Double.parseDouble(p.getProperty(PROPERTY_NAME_SPIRALX_TURNS)));
                        } catch (Exception e) {
                        }

                    } else {
                        mRequireSpiralSettings = false;
                    }
                }
                else{
                    mRequireSpiralSettings = false;
                }

                if (mMode.equals(SpiralOutPositioner.MODE_SPIRAL_OUT)) {
                    getThis().mSpiralOutPositioner.setCardSpread( Double.parseDouble( (String) p.get(PROPERTY_NAME_SPIRALOUT_CARD_SPREAD) ) );
                    getThis().mSpiralOutPositioner.setSpiralSpread( Double.parseDouble( (String) p.get(PROPERTY_NAME_SPIRALOUT_SPIRAL_SPREAD) ) );

                    mRequireSpiralOutSettings = true;
                    mRequireBaseOptions = false;
                }
                else{
                    mRequireSpiralOutSettings = false;
                }


                //this.initOptionsTables();
                this.reinitLayout();

                getThis().fireSortingConfigEvent(getThis(), "Something changed", true);
            }
            catch(Exception e){
                System.out.println("[XSorter] Exception in optionTableEventHappened..\n"+e.toString());
            }
        }

        public static final String PROPERTY_NAME_JITTER    = "Jitter";
        public static final String PROPERTY_NAME_STRETCH_X = "StretchX";
        public static final String PROPERTY_NAME_STRETCH_Y = "StretchY";
        public static final String PROPERTY_NAME_SHAPE     = "Shape";
        public static final String PROPERTY_NAME_COLUMN    = "Column";

        public static final String PROPERTY_NAME_SPIRALX_TURNS           = "SpiralXNumTurns";
        public static final String PROPERTY_NAME_SPIRALX_RADIUS_RATIO    = "SpiralXRadiusRatio";

        public static final String PROPERTY_NAME_SPIRALOUT_CARD_SPREAD    = "SpiralOutCardSpread";
        public static final String PROPERTY_NAME_SPIRALOUT_SPIRAL_SPREAD  = "SpiralSpread";



        /**
         * This enables StretchX and StretchY and Jitter options
         */
        private boolean mRequireBaseOptions    = true;

        /**
         * This enables NumTurns and Ratio options
         */
        private boolean mRequireSpiralSettings = false;

        /**
         * This enables the spiral out options ()
         */
        private boolean mRequireSpiralOutSettings = false;



        JMyOptionsTable mOptionsTable_ColAndShape   = null;
        JMyOptionsTable mOptionsTable_BaseOptions   = null;
        JMyOptionsTable mOptionsTable_SpiralXOptions = null;
        JMyOptionsTable mOptionsTable_SpiralOutOptions = null;

        public void initOptionsTable_ColumnAndShape() {

        }

        private void initOptionsTables() {

//            // number of y rows..
//            if(mOptionsTable==null) {
//                //mOptionsTable = new JMyOptionsTable(1, numOptionsRequired);
//                mOptionsTable = new JMyOptionsTable();
//            }
//            else{
//                mOptionsTable.reset();
//            }

            mOptionsTable_ColAndShape = new JMyOptionsTable();
            mOptionsTable_ColAndShape.setComboBoxDoubleAndCategoricalColumns(0, 0, PROPERTY_NAME_COLUMN, "Column:  ", mModel);
            List<String> shapes = new ArrayList<>();
            shapes.add(CardNumericalShaper1D.MODE_GRID);
            shapes.add(CardNumericalShaper1D.MODE_CIRCLE);
            shapes.add(CardNumericalShaper1D.MODE_SPIRAL_X);
            //shapes.add(CardNumericalShaper1D.MODE_SPIRAL_1);
            //shapes.add(CardNumericalShaper1D.MODE_SPIRAL_2);
            shapes.add(SpiralOutPositioner.MODE_SPIRAL_OUT);

            mOptionsTable_ColAndShape.setComboBox(0, 1, PROPERTY_NAME_SHAPE, "Shape:  ", shapes, shapes.get(0));
            mOptionsTable_ColAndShape.reinit();

            mOptionsTable_BaseOptions = new JMyOptionsTable();
            mOptionsTable_BaseOptions.setSlider(0, 0, PROPERTY_NAME_STRETCH_X, "Stretch X:", 0, 1, 0.1);
            mOptionsTable_BaseOptions.setSlider(0, 1, PROPERTY_NAME_STRETCH_Y, "Stretch Y:", 0, 1, 0.1);
            mOptionsTable_BaseOptions.setSlider(0, 2, PROPERTY_NAME_JITTER, "Jitter:", 0, 1, 0.0);
            mOptionsTable_BaseOptions.reinit();


            mOptionsTable_SpiralXOptions = new JMyOptionsTable();
            mOptionsTable_SpiralXOptions.setSlider(0, 0, PROPERTY_NAME_SPIRALX_TURNS, "Spiral Turns: ", 1, 10, 3);
            mOptionsTable_SpiralXOptions.setSlider(0, 1, PROPERTY_NAME_SPIRALX_RADIUS_RATIO, "Spiral Inner Radius: ", 0.01, 0.95, 0.15);
            mOptionsTable_SpiralXOptions.reinit();

            mOptionsTable_SpiralOutOptions = new JMyOptionsTable();
            mOptionsTable_SpiralOutOptions.setSlider(0, 0, PROPERTY_NAME_SPIRALOUT_CARD_SPREAD, "Card Spread: ", 0.25, 4, 1.0);
            mOptionsTable_SpiralOutOptions.setSlider(0, 1, PROPERTY_NAME_SPIRALOUT_SPIRAL_SPREAD, "Spiral Spread: ", 0.25, 4, 1.0);
            mOptionsTable_SpiralOutOptions.reinit();


            mOptionsTable_ColAndShape.registerOptionTableListener(this);
            mOptionsTable_BaseOptions.registerOptionTableListener(this);
            mOptionsTable_SpiralXOptions.registerOptionTableListener(this);
            mOptionsTable_SpiralOutOptions.registerOptionTableListener(this);


//------------------------------------------------------------------------------------------------------
//            if(false){
//                //mOptionsTable.setComboBoxDoubleAndCategoricalColumns(0,0,PROPERTY_NAME_COLUMN, mModel);
//                mOptionsTable.setComboBoxDoubleAndCategoricalColumns(0, 0, PROPERTY_NAME_COLUMN, "Column:  ", mModel);
//
//                List<String> shapes = new ArrayList<>();
//                shapes.add(CardNumericalShaper1D.MODE_GRID);
//                shapes.add(CardNumericalShaper1D.MODE_CIRCLE);
//                shapes.add(CardNumericalShaper1D.MODE_SPIRAL_X);
//                shapes.add(CardNumericalShaper1D.MODE_SPIRAL_1);
//                shapes.add(CardNumericalShaper1D.MODE_SPIRAL_2);
//                mOptionsTable.setComboBox(0, 1, PROPERTY_NAME_SHAPE, "Shape:  ", shapes, shapes.get(0));
//
//
//                // count up to add the additionally required options..
//                int ypos = 2;
//
//                if (mRequireSpiralSettings) {
//                    mOptionsTable.setSlider(0, ypos, PROPERTY_NAME_SPIRALX_TURNS, "Spiral Turns: ", 1, 10, 3);
//                    ypos++;
//                    mOptionsTable.setSlider(0, ypos, PROPERTY_NAME_SPIRALX_RADIUS_RATIO, "Spiral Turns: ", 0.01, 0.95, 0.15);
//                    ypos++;
//                }
//
//                mOptionsTable.setSlider(0, ypos, PROPERTY_NAME_STRETCH_X, "Stretch X:", 0, 1, 0.1);
//                ypos++;
//                mOptionsTable.setSlider(0, ypos, PROPERTY_NAME_STRETCH_Y, "Stretch Y:", 0, 1, 0.1);
//                ypos++;
//                mOptionsTable.setSlider(0, ypos, PROPERTY_NAME_JITTER, "Jitter:", 0, 1, 0.0);
//                ypos++;
//                mOptionsTable.reinit();
//
//                mOptionsTable.registerOptionTableListener(this);
//            }
        }

    }

    public XSorter getThis(){
        return this;
    }

}
