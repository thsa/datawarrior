package com.actelion.research.table.view.card.positioning;

import com.actelion.research.table.view.card.CardElement;
import com.actelion.research.table.view.card.CardPaneModel;
import com.actelion.research.table.view.card.animators.SampledPath;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class CardNumericalShaper1D extends AbstractCardPositioner {


    public static final String MODE_GRID     = "GRID";
    public static final String MODE_CIRCLE   = "CIRCLE";
    public static final String MODE_SPIRAL_X = "SPIRAL_X";

    //public static final String MODE_SPIRAL_OUT = "SPIRAL_OUT";

    public static final String MODE_SPIRAL_1 = "SPIRAL_1";
    public static final String MODE_SPIRAL_2 = "SPIRAL_2";



    //public static final String[] ALL_MODES = new String[]{ MODE_GRID,MODE_CIRCLE,MODE_SPIRAL_X,MODE_SPIRAL_1,MODE_SPIRAL_2 };
    public static final String[] ALL_MODES = new String[]{ MODE_GRID,MODE_CIRCLE,MODE_SPIRAL_X};

    private int mColumn;

    /**
     * Interval bounds of the column value
     */
    private double mVA = 0;
    private double mVB = 1;



    private String mMode;

    private Shape mShape;

    private Rectangle2D mShapeBoundingBox;

    private Path2D mSampledPath;

    private List<Point2D> mSampledPathPoints;

    //private Map<IdentityHashedObject<CardElement>,Point2D> mGridPositions;

    // we use the nonexcluded records set as key, to identify card elements..
    private Map<Set<Integer>,Point2D> mGridPositions;

    private CompoundTableModel mModel = null;



    /**
     * Definition of jitter:
     * it goes from 0 to 1, where one corresponds to the interval XA to XB
     */
    private double mJitter = 0;


    /**
     * Number of spiral turns in spiral x mode
     */
    private int mNumSpiralTurns = 3;

    /**
     * Ratio between innermost and outermost radius
     */
    private double mInnerToOuterSpiralRatio = 0.25;

    /**
     * Stretches / shrinks the default interval
     */
    private double mStretchX = 1.0;
    private double mStretchY = 1.0;


    public static int getFirstDoubleColumn(CompoundTableModel model){
        if(model==null){return -1;}

        int col_double = -1;
        for(int zi=0;zi<model.getColumnCount();zi++){
            if(model.isColumnTypeDouble(zi)){
                col_double = zi;
                break;
            }
        }
        return col_double;
    }

    public CardNumericalShaper1D( CompoundTableModel model ){
        this(model,getFirstDoubleColumn(model),MODE_GRID);
    }

    public CardNumericalShaper1D( CompoundTableModel model, int column, String mode){
        this.mModel  = model;
        this.mColumn = column;
        this.mMode   = mode;
    }


    @Override
    public boolean requiresTableModel() {
        return true;
    }

    @Override
    public boolean shouldCreateStacks() {
        return false;
    }

    @Override
    public boolean supportPositionSingleCard() {
        return false;
    }

// CONFIG:

    /**
     * We choose the spread interval in "card space" proportional to the square root of the number of cards.
     * This factor determines the actual spread interval, which is  sqrt(#cards) * CONF_PROPORTIONALITY_FACTOR_FOR_SPREAD
     */
    private static final double CONF_PROPORTIONALITY_FACTOR_FOR_SPREAD = 1.5;


    /**
     * Inits Shape based on mode, number of columns in the model, (potentially number of categories in the column),
     * and stretch factors..
     */
    public synchronized void initShape( List<CardElement> ces ){

        System.out.println("!!!Init Shape Called!!! Sort column: "+mColumn);

        double cardWidth  = ces.get(0).getRectangle().getWidth();
        double cardHeight = ces.get(0).getRectangle().getHeight();

        Shape shape = null;


        // compute a "width" that we can use in further computations..
        // i.e. a square of that size provides easily enough space to lay out the components.
        //int width = (int) (  (  Math.sqrt( ces.size() ) + 20 ) * cardWidth   );
        //int width = (int) (  (  Math.sqrt( ces.size() ) + 20 ) * cardWidth * 4 );
        int width = (int) (  (  Math.sqrt( ces.size() ) + 200 ) * cardWidth * 4 );


        if(mMode.equals(MODE_GRID)){
            //Map<IdentityHashedObject<CardElement>,Point2D> gridPositions = new HashMap<>();
            Map<Set<Integer>,Point2D> gridPositions = new HashMap<>();

            double width_scaled  = (cardWidth+(1+40*Math.sqrt(mStretchX)) ) * Math.sqrt( ces.size() ) ;
            double height_scaled = (cardHeight+(1+40*Math.sqrt(mStretchY)) ) * Math.sqrt( ces.size() ) ;

            double x0 = -0.5*width_scaled; double y0 = -0.5*height_scaled;
            double dx = width_scaled / Math.sqrt(ces.size()); double dy = height_scaled / Math.sqrt(ces.size());


            List<CardElement> sortedCEs = new ArrayList<>(ces);

            sortedCEs.sort( (x,y) -> Double.compare( CardNumericalSorter.myGetDoubleAndAverageForStack(mModel, x, mColumn) , CardNumericalSorter.myGetDoubleAndAverageForStack(mModel, y, mColumn) ) );

            //sortedCEs.stream().forEachOrdered( (x) -> System.out.println("V="+x.getAllRecords().get(0).getDouble(mColumn)) );

            int cnt = 0;
            for(int cy = 0;cy< (int) Math.ceil( Math.sqrt(ces.size()) );cy++ ){
                for(int cx = 0;cx< (int) Math.ceil( Math.sqrt(ces.size()) );cx++ ){
                    if(cnt>=ces.size()){break;}
                    //gridPositions.put( new IdentityHashedObject<>(sortedCEs.get(cnt)) , new Point2D.Double( x0 + cx * dx , y0 + cy * dy ) );
                    gridPositions.put( sortedCEs.get(cnt).getRecordIDsSet_Nonexcluded() , new Point2D.Double( x0 + cx * dx , y0 + cy * dy ) );
                    cnt++;
                }
            }

            mGridPositions = gridPositions;
        }

        else {

            if (mMode.equals(MODE_CIRCLE)) {
                //double radius = Math.sqrt( mModel.getTotalRowCount() )   ;
                Ellipse2D c = new Ellipse2D.Double(0d, 0d, width, width);
                shape = c;
            }
            if (mMode.equals(MODE_SPIRAL_X)) {
                double radius_out = width;
                Path2D spiral = createSpiral(0d, 0d, radius_out, mInnerToOuterSpiralRatio * radius_out, mNumSpiralTurns);
                shape = spiral.createTransformedShape(null);
            }
            if (mMode.equals(MODE_SPIRAL_1)) {
                double radius_out = width;
                Path2D spiral = createSpiral(0d, 0d, radius_out, 0.25 * radius_out, 2);
                shape = spiral.createTransformedShape(null);
            }
            if (mMode.equals(MODE_SPIRAL_2)) {
                double radius_out = width;
                Path2D spiral = createSpiral(0d, 0d, radius_out, 0.15 * radius_out, 4);
                shape = spiral.createTransformedShape(null);
            }


            Shape trShape = AffineTransform.getScaleInstance(this.mStretchX, this.mStretchY).createTransformedShape(shape);
            this.mShape = trShape;

            this.mShapeBoundingBox = this.mShape.getBounds2D();

            // sample the mShape:
            int numSamples = 100;
            if (mModel.isColumnTypeCategory(mColumn)) {
                //numSamples = mModel.getCategoryCount(mColumn) + 1; // No, we can have inbetween values from stacks etc.. we have to sample full..
                mVA = 0;
                mVB = mModel.getCategoryCount(mColumn);
            } else if (mModel.isColumnTypeDouble(mColumn)) {
                numSamples = this.mModel.getRowCount() + 1;
                mVA = ces.stream().mapToDouble(ci -> ci.isCard() ? ci.getAllRecords().get(0).getDouble(mColumn) : ci.getAllRecords().stream().mapToDouble(xi -> xi.getDouble(mColumn)).min().getAsDouble()).min().getAsDouble();
                mVB = ces.stream().mapToDouble(ci -> ci.isCard() ? ci.getAllRecords().get(0).getDouble(mColumn) : ci.getAllRecords().stream().mapToDouble(xi -> xi.getDouble(mColumn)).max().getAsDouble()).max().getAsDouble();
            }
            SampledPath sp = new SampledPath(mShape.getPathIterator(null), 0, numSamples);
            this.mSampledPathPoints = sp.getSampledPoints();
            this.mSampledPath = new Path2D.Double();
            this.mSampledPathPoints.stream().forEachOrdered(pi -> this.mSampledPath.moveTo(pi.getX(), pi.getY()));
        }

    }


    public int getColumn(){
        return mColumn;
    }


    public void setColumn(int col, List<CardElement> elements){
        this.mColumn = col;
        this.initShape(elements);
    }


    private static Random random = new Random(2244665);


    @Override
    public List<Point2D> positionAllCards(CompoundTableModel tableModel, List<CardElement> ce) throws InterruptedException {
        this.setTableModel(tableModel);
        this.initShape( ce );

        List<Point2D> pos = new ArrayList<>();
        int zi=0;
        for(CardElement cei : ce) {
            if(Thread.currentThread().isInterrupted()){
                throw new InterruptedException();
            }
            pos.add(positionSingleCard(cei,null));
            zi++;
        }
        return pos;
    }

    @Override
    public synchronized Point2D positionSingleCard(CardPaneModel.CardElementInterface ce, CompoundRecord cr_p) {

        if(this.mMode.equals( MODE_GRID )){
            //System.out.println(""+ce.getAllRecords().get(0).getDouble( mColumn )+" --> "+mGridPositions.get(ce).getX()+"/"+mGridPositions.get(ce).getY());

            //Point2D pgi = mGridPositions.get(new IdentityHashedObject<>(ce));
            Point2D pgi = mGridPositions.get( ((CardElement)ce).getRecordIDsSet_Nonexcluded() );
            if(pgi==null){
                System.out.println("WARNING! CardNumericalShaper1D::positionCard(..) -> GRID POINT MISSING (pgi is null)..");
                pgi = new Point2D.Double(0,0);
            }

            return pgi;
        }
        else {

            if(this.mSampledPathPoints==null){ return new Point2D.Double(ce.getPosX(),ce.getPosY()); }

            double relPos = Double.NaN;

            if (mModel.isColumnTypeCategory(mColumn)) {
                List<CompoundRecord> rec = null;
                if (cr_p == null) {
                    //rec = ce.getRecords();
                    rec = new ArrayList<>(ce.getAllRecords());
                } else {
                    rec = new ArrayList<>();
                    rec.add(cr_p);
                }
                double pi = rec.stream().mapToDouble(ci -> CardNumericalSorter.myGetDouble(mModel, ci, mColumn)).average().getAsDouble();
                relPos = (pi - mVA) / (mVB - mVA);
            } else if (mModel.isColumnTypeDouble(mColumn)) {
                List<CompoundRecord> rec = null;
                if (cr_p == null) {
                    //rec = ce.getRecords();
                    rec = new ArrayList<>(ce.getAllRecords());
                } else {
                    rec = new ArrayList<>();
                    rec.add(cr_p);
                }
                double pi = rec.stream().mapToDouble(ci -> CardNumericalSorter.myGetDouble(mModel, ci, mColumn)).average().getAsDouble();
                relPos = (pi - mVA) / (mVB - mVA);
            }

            int index = -1;

            // compute the point index:
            index = (int) (relPos * (this.mSampledPathPoints.size() - 1));

            // ;
            if (mJitter > 0) {
                double jitterFactor =  ( (0.5*this.mJitter) *(0.5*this.mJitter) * (0.5*this.mJitter) );
                return new Point2D.Double(this.mSampledPathPoints.get(index).getX() + random.nextDouble() * (jitterFactor) *  mShapeBoundingBox.getHeight(), this.mSampledPathPoints.get(index).getY() + random.nextDouble() * (jitterFactor) * mShapeBoundingBox.getHeight());
            } else {
                return new Point2D.Double(this.mSampledPathPoints.get(index).getX(), this.mSampledPathPoints.get(index).getY());
            }
        }
    }

    public void setStretchX(double s, List<CardElement> elements){
        this.mStretchX = s;
        this.initShape(elements);
    }
    public void setStretchY(double s, List<CardElement> elements){
        this.mStretchY = s;
        this.initShape(elements);
    }

    public void setJitter(double j, List<CardElement> elements){
        this.mJitter = j;
        this.initShape(elements);
    }

    public void setShape(String shape, List<CardElement> elements){
        if( Arrays.asList(ALL_MODES).contains(shape) ){
            this.mMode = shape;
            this.initShape(elements);
        }
    }


    @Override
    public void configure(Properties properties) {

    }

    @Override
    public Properties getConfiguration() {
        return null;
    }

    public static Path2D createSpiral( double centerX, double centerY, double radius_out, double radius_in, int turns){

        Path2D.Double spiral = new Path2D.Double();

        // start at top..
        double angle0 = 0.5 * Math.PI;
        //spiral.moveTo(Math.cos(angle0) * radius_out,Math.cos(angle0) * radius_out);

        double totalAngle = turns * Math.PI*2;

        double xi, yi, radius_i;
        double angleUsed = 0;

        int numSamples = 200*turns;


        //for(int zi=0;zi<numSamples;zi++){
        for(int zi=1;zi<numSamples;zi++){
            //radius_i  = radius_in + (radius_out-radius_in)*(1.0*(numSamples-zi))/numSamples;
            radius_i = radius_out - (radius_out-radius_in)*(1.0*(numSamples-zi))/numSamples;
            angleUsed = totalAngle*(1.0*zi)/numSamples;
            xi = Math.cos(angle0+angleUsed) * radius_i;
            yi = Math.sin(angle0+angleUsed) * radius_i;
            //System.out.println("x: "+xi+"  y:"+yi);

            if(zi==1){spiral.moveTo(xi,yi); }
            else{     spiral.lineTo(xi,yi); }
        }
        return spiral;
    }

    public void setSpiralOptions(double radiusRatio, int turns) {
        this.mInnerToOuterSpiralRatio = radiusRatio;
        this.mNumSpiralTurns = turns;
    }



    public static CardNumericalShaper1D createDefaultXSorter_Grid( CompoundTableModel ctm, int column) {
        CardNumericalShaper1D cns = new CardNumericalShaper1D(ctm,column,CardNumericalShaper1D.MODE_GRID);
        return cns;
    }


}
