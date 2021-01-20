package com.actelion.research.table.view.card.positioning;

import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.card.CardElement;
import com.actelion.research.table.view.card.CardPaneModel;
import com.actelion.research.table.view.card.cardsurface.gui.JCardWizard2;
import com.actelion.research.table.view.card.cardsurface.gui.JMyOptionsTable;
import com.actelion.research.table.view.card.tools.ColWithType;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

public class CardStackPositioner extends CardPositionerWithGUI  {


    /**
     * Configuration options:
     *
     *
     *
     */


    public static String NAME = "StackPositioner";

    private List<CardElement>   mElements = null;

    private CompoundTableModel mCTM;

    private ColWithType mColX = null;
    private ColWithType mColY = null;

    private int mBinsX = 10;
    private int mBinsY = 10;


    public final static String PROP_COL_X = "ColX";
    public final static String PROP_COL_Y = "ColY";
    public final static String PROP_BINS_X = "BinsX";
    public final static String PROP_BINS_Y = "BinsY";

    public CardStackPositioner(CompoundTableModel ctm, List<CardElement> ces){
        this.mCTM = ctm;
        this.mElements = new ArrayList<>(ces);


    }

    @Override
    public JPanel getGUI() {

        MyConfigPanel mcp = new MyConfigPanel();
        mcp.reinit();

        return mcp;
    }

    @Override
    public String getName() {
        return null;
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
        return null;
    }


    @Override
    public List<Point2D> positionAllCards(CompoundTableModel tableModel, List<CardElement> cards) throws InterruptedException {

        if(mColX==null || mColY==null){
            List<Point2D> pxy = new ArrayList<>(); cards.stream().forEachOrdered( cei -> cei.getPos() );//for(int zi=0;zi<cards.size();zi++){pxy.add( new Point2D.Double( r.nextGaussian()*200,r.nextGaussian()*200 ));}
            return pxy;
        }

        double px[] = null;
        double py[] = null;



//        if(mColX.isNumeric()){
//
//        }

        //new double[mBinsX];
        Random r = new Random();

        List<Point2D> pxy = new ArrayList<>();
        //for(int zi=0;zi<cards.size();zi++){pxy.add( new Point2D.Double( r.nextGaussian()*200,r.nextGaussian()*200 ));}

        Map<CardElement,Double> pos_x = new HashMap<>();
        Map<CardElement,Double> pos_y = new HashMap<>();


        double cw = cards.get(0).getRectangle().getWidth();
        double ch = cards.get(0).getRectangle().getHeight();

        // multiplicator for computing distances inbetween gridded stacks
        double conf_RelDistGrid = 4;
        double distGrid         = conf_RelDistGrid * Math.max(cw,ch);

        if(mColX.getType()== ColWithType.ColType.NUMERICAL){

            // bin..
            double vx[] = cards.stream().mapToDouble( cei -> CardNumericalSorter.myGetDoubleAndAverageForStack(mCTM,cei,mColX.getCol())).toArray();
            List<List<CardElement>> stacks = binElements(cards,vx,mBinsX);

            int bin_xi = 0;
            for(List<CardElement> celi : stacks){
                int bin_xii = bin_xi;
                celi.stream().forEach( cxi -> pos_x.put(cxi,new Double(bin_xii) ) );
                bin_xi++;
            }
        }
        else{
            double vx[] = cards.stream().mapToDouble( cei -> CardNumericalSorter.myGetDoubleAndAverageForStack(mCTM,cei,mColX.getCol())).toArray();
            for(int zi=0;zi<vx.length;zi++){
                pos_x.put( cards.get(zi), vx[zi]);
            }
        }

        if(mColY.getType()== ColWithType.ColType.NUMERICAL){
            // bin..
            double vy[] = cards.stream().mapToDouble( cei -> CardNumericalSorter.myGetDoubleAndAverageForStack(mCTM,cei,mColY.getCol())).toArray();
            List<List<CardElement>> stacks = binElements(cards,vy,mBinsY);
            int bin_yi = 0;
            for(List<CardElement> celi : stacks){
                int bin_yii = bin_yi;
                celi.stream().forEach( cxi -> pos_y.put(cxi,new Double(bin_yii)) );
                bin_yi++;
            }
        }
        else{
            double vy[] = cards.stream().mapToDouble( cei -> CardNumericalSorter.myGetDoubleAndAverageForStack(mCTM,cei,mColY.getCol())).toArray();
            for(int zi=0;zi<vy.length;zi++){
                pos_y.put( cards.get(zi), vy[zi]);
            }
        }

        for(int zi=0;zi<cards.size();zi++){
            pxy.add( new Point2D.Double( pos_x.get(cards.get(zi)).doubleValue() * distGrid , pos_y.get(cards.get(zi)).doubleValue() * distGrid ) );
        }

        return pxy;
    }

    private List<List<CardElement>> binElements(List<CardElement> ces, double values[], double n_bins){
        DoubleSummaryStatistics dss = Arrays.stream(values).summaryStatistics();
        double a = dss.getMin(); double b = dss.getMax();
        double intv = b-a;

        // make the cards be strictly inside the bins..
        a -= 0.001*intv;
        b += 0.001*intv;
        intv = b-a;

        double binw = intv / n_bins;

        if(binw==0){
            List<List<CardElement>> binned = new ArrayList<>(); binned.add(new ArrayList<>());
            ces.stream().forEach(cei -> binned.get(0).add(cei));
            return binned;
        }

        List<List<CardElement>> binned = new ArrayList<>();
        for(int zi=0;zi<n_bins;zi++){
            List<CardElement> bin_i = new ArrayList<>();
            binned.add(bin_i);
        }

        for(int zi=0;zi<ces.size();zi++){
            int bini =  (int) Math.floor( (values[zi]-a) / binw );
            // make sure first elements go into first bin:
            bini = Math.max(bini,0);
            binned.get(bini).add(ces.get(zi));
        }

        return binned;
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
        return true;
    }

    public void setProperties(Properties p){

        ColWithType colX = ColWithType.deserializeFromString( mCTM , p.getProperty(PROP_COL_X) );
        ColWithType colY = ColWithType.deserializeFromString( mCTM , p.getProperty(PROP_COL_Y) );
        mColX = colX;
        mColY = colY;

        if(mColX.getType()== ColWithType.ColType.NUMERICAL){
            if(!mTable.isActivated(1,0)) {
                mTable.activate(1, 0);
                mTable.setValue(1, 0, 10.0);
                mBinsX = (int) Double.parseDouble( mTable.collectValues().getProperty(PROP_BINS_X));
            }
            mBinsX = (int) Double.parseDouble( mTable.collectValues().getProperty(PROP_BINS_X));
        }
        else{
            mTable.deactivate(1,0);
        }
        if(mColY.getType()== ColWithType.ColType.NUMERICAL){
            if(!mTable.isActivated(1,1)) {
                mTable.activate(1, 1);
                mTable.setValue(1, 1, 10.0);
            }
            mBinsY = (int) Double.parseDouble( mTable.collectValues().getProperty(PROP_BINS_Y));
        }
        else{
            mTable.deactivate(1, 1);
        }

        //this.fireSortingConfigEvent(this, "CardStackPositioner changed",true);
    }

    JMyOptionsTable mTable = new JMyOptionsTable();

    class MyConfigPanel extends JPanel{

        public MyConfigPanel(){}

        /**
         * reinitializes the JMyOptionTable with the options in CardStackPositioner..
         */
        public void reinit(){
            mTable.setComboBoxDoubleAndCategoricalColumns2(0,0,PROP_COL_X, "ColX ", mCTM);
            mTable.setComboBoxDoubleAndCategoricalColumns2(0,1,PROP_COL_Y, "ColY ", mCTM);

            mTable.setSlider(1,0,PROP_BINS_X,"BinsX ",1 ,20 ,5 );
            mTable.setSlider(1,1,PROP_BINS_Y,"BinsY ",1 ,20 ,5 );

            mTable.reinit();

            mTable.registerOptionTableListener(new MyTableListener() );

            this.setLayout(new BorderLayout());
            this.add(mTable, BorderLayout.CENTER);
        }
    }

    class MyTableListener implements JMyOptionsTable.OptionTableListener {
        @Override
        public void optionTableEventHappened(JMyOptionsTable.OptionTableEvent event) {
            if( event.isAdjusting() ){return;}
            Properties p = mTable.collectValues();
            setProperties(p);
            fireSortingConfigEvent(getThis(), "CardStackPositioner changed",true);
        }
    }

    public CardStackPositioner getThis() {return this;}

}
