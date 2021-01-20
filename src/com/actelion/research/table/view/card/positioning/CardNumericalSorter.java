package com.actelion.research.table.view.card.positioning;

import com.actelion.research.table.view.card.CardElement;
import com.actelion.research.table.view.card.CardPaneModel;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

public class CardNumericalSorter extends AbstractCardPositioner
{




    public static final int AXIS_X = 1;
    public static final int AXIS_Y = 2;


    private int mColumn;
    private double mXA, mXB;
    private double mVA, mVB;


    private CompoundTableModel mModel = null;

    /**
     * Definition of jitter:
     * it goes from 0 to 1, where one corresponds to the interval XA to XB
     */
    private double mJitter = 0;

    private int mAxis;


    /**
     * Stretches / shrinks the default interval
     */
    private double mStretch = 1.0;


    // CONFIG:

    /**
     * We choose the spread interval in "card space" proportional to the square root of the number of cards.
     * This factor determines the actual spread interval, which is  sqrt(#cards) * CONF_PROPORTIONALITY_FACTOR_FOR_SPREAD
     */
    private static final double CONF_PROPORTIONALITY_FACTOR_FOR_SPREAD = 1.5;


    public CardNumericalSorter(CompoundTableModel model , int axis, int column , double xA , double vA, double xB, double vB ){
        this.mAxis   = axis;
        this.mColumn = column;

        this.mXA = xA;
        this.mXB = xB;
        this.mVA = vA;
        this.mVB = vB;

        this.mModel = model;
    }


    public void setStretch(double stretch){
        this.mStretch = stretch;
    }


    private static Random mRandom = new Random(12435687);


    @Override
    public boolean requiresTableModel() {
        return false;
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
    public List<Point2D> positionAllCards(CompoundTableModel tableModel, List<CardElement> ce){
        this.setTableModel(tableModel);
        this.recomputeInterval( ce );

        List<Point2D> pos = new ArrayList<Point2D>();
        int zi=0;
        for(CardElement cei : ce) {
            pos.add(positionSingleCard(cei,null));
            zi++;
        }
        return pos;
    }

    @Override
    public void setTableModel(CompoundTableModel tableModel){
        super.setTableModel(tableModel);
    }


    @Override
    public Point2D positionSingleCard(CardPaneModel.CardElementInterface ce, CompoundRecord cr) {

        double v = Double.NaN;

        if(ce!=null){
            if(ce.isCard()){
                v = ce.getAllRecords().get(0).getDouble(mColumn);
                //System.out.println("Fetched number from record: "+v + " in column "+mColumn);
            }
            else{
                // compute position for stack.. take average..
                //v = ce.getAllRecords().stream().mapToDouble( c -> c.getDouble(mColumn) ).average().getAsDouble();
                v = ce.getAllRecords().stream().mapToDouble( c -> c.getDouble(mColumn) ).average().orElse( Double.NaN );
            }
        }
        else {
            cr.getDouble(mColumn);
        }

        if( Double.isNaN(v) ){
            //System.out.println("WARNING: CardNumericalSorter::positionCard : NaN value encountered..");
            v = new Random().nextDouble() * (mVB-mVA) * mStretch + mVA;
        }

        double x_new = mXA +  (v - mVA) * ( mStretch * (mXB - mXA) / (mVB - mVA));

        if(mJitter > 0){
            x_new += (mRandom.nextDouble()-0.5) * mJitter * ( mStretch * (mXB-mXA) );
        }

        if(mAxis==AXIS_X){
            return new Point2D.Double( x_new , ce.getPosY() );
        }
        if(mAxis==AXIS_Y){
            return new Point2D.Double( ce.getPosX() , x_new );
        }
        return null;
    }




    public static CardNumericalSorter createNumericalSorter(CompoundTableModel model, List<CardElement> ces , int column , int axis){
        if(ces.size()==0){return null;}

        CardNumericalSorter cns = new CardNumericalSorter( model, axis , column , Double.NaN , Double.NaN , Double.NaN , Double.NaN );
        cns.recomputeInterval(ces);

        return cns;
    }


    /**
     * recomputes the interval, based on the set column
     *
     * @param ces all card elements..
     */
    public void recomputeInterval(List<CardElement> ces){

        double vmin = Double.POSITIVE_INFINITY;
        double vmax = Double.NEGATIVE_INFINITY;

        for( CardElement ce : ces ){
            double ve = Double.NaN;
            if(ce.isCard()){
                ve = ce.getAllRecords().get(0).getDouble(mColumn);
            }
            else{ ve = ce.getAllRecords().stream().mapToDouble(c -> c.getDouble(mColumn)).average().getAsDouble(); }

            if(!Double.isNaN(ve)) {
                vmin = Math.min(vmin, ve);
                vmax = Math.max(vmax, ve);
            }
        }

        if(Double.isNaN(vmin) || Double.isNaN(vmax) ){
            System.out.println("WARNING: CardNumericalSorter init failed.. only NaN entries..");
            vmin=0; vmax = 1;
        }

        double cardSpread = Math.sqrt( ces.size()  ) * ces.get(0).getRectangle().getWidth() * CONF_PROPORTIONALITY_FACTOR_FOR_SPREAD ;
        double center     = (vmin+vmax) * 0.5;

        this.mXA =  (int) (center-0.5*cardSpread);
        this.mVA =  (int) vmin;
        this.mXB =  (int) ( center+0.5*cardSpread);
        this.mVB =  (int) vmax;

        System.out.println("SORTER: "+vmin+" / " +vmax+" / "+center);
    }

    public void setColumn(int col, List<CardElement> ces){
        this.mColumn = col;
        this.recomputeInterval(ces);
    }

    public void setJitter(double jitter){
        this.mJitter = jitter;
    }

    public int getColumn(){
        return this.mColumn;
    }


    @Override
    public void configure(Properties properties) {
        this.mColumn = Integer.parseInt( properties.getProperty("Column") );
        this.mAxis = Integer.parseInt( properties.getProperty("Axis") );
        this.mVA = Double.parseDouble( properties.getProperty("VA") );
        this.mVB = Double.parseDouble( properties.getProperty("VB") );
        this.mXA = Double.parseDouble( properties.getProperty("XA") );
        this.mXB = Double.parseDouble( properties.getProperty("XB") );
        this.mStretch = Double.parseDouble( properties.getProperty("Stretch") );
        this.mJitter = Double.parseDouble( properties.getProperty("Jitter") );
    }

    @Override
    public Properties getConfiguration() {
        Properties p = new Properties();

        p.setProperty("Column", ""+this.mColumn );
        p.setProperty("Axis", ""+this.mAxis );
        p.setProperty("VA", ""+this.mVA );
        p.setProperty("VB", ""+this.mVB );
        p.setProperty("XA", ""+this.mXA );
        p.setProperty("XB", ""+this.mXB );
        p.setProperty("Stretch",""+this.mStretch);
        p.setProperty("Jitter",""+this.mJitter);

        return p;
    }
}
