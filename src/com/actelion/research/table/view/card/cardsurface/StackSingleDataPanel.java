package com.actelion.research.table.view.card.cardsurface;

import com.actelion.research.table.view.card.CardElement;
import com.actelion.research.table.view.card.cardsurface.density.DensityDrawer;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class StackSingleDataPanel extends AbstractStackSurfacePanel {

    public static final String NAME = "Single Data [Stack]";

    private int mColumns[] = new int[]{-1};

    private boolean log_recompute = false;

    /**
     * Space used for the column name
     */
    private double mDividerPosA = 0.75;

    /**
     * Rel. beginning of Histogram
     */
    private double mDividerPosB = 0.25;



    /**
     * Buffered image showing a density estimate of the full dataset
     */
    private BufferedImage mBimDistribution = null;

    /**
     * Buffered image showing the slices of the stack, to be painted on top of the mBimDistribution
     */
    private HashMap<Set<Integer>,BufferedImage> mCacheBimDistributionSlices = new HashMap<>();

    /**
     * Buffered image showing the distribution of the stack elements, with the scaling of the full distribution.
     */
    private HashMap<Set<Integer>,BufferedImage> mCacheBimSubDistributions = new HashMap<>();


    //Set<Integer> mBufferedStackIDs = new HashSet<>();


    /**
     * Density drawer
     */
    private DensityDrawer mDensityDrawer = null;

    /**
     * Density drawer for subdistribution
     */
    private DensityDrawer mDensityDrawerSubDistribution = null;

    /**
     * Bandwidth used in the kernel density estimation
     */
    private double mConfRelBandwidth = 0.1;


    /**
     * Sampling resolution (computation / slices)
     */
    private int    mConfSamplingResolution = 80;
    //private int    mConfSamplingResolution = 80;

    //private int mConfOversampling = 4;
    //private int mConfOversampling = 4;

    /**
     * Image resolution
     */
    private int mConfImageResolutionY = 200;
    private int mConfImageResolutionX = 400;


    private double mMaxY_FullDistribution;
    //private double mMaxY_SubDistribution;

    private int mConfMarginY = 4;
    private int mConfMarginX = 4;


    /**
     * Switch density and bar chart (if num categories > this value, then we draw density plot)
     */
    private int mThresholdCategorical = 32;


    private int CONF_MARGIN_LEFT = 4; //HiDPIHelper.scale(4);




    /**
     * This is the font which is used to initialize the "shrinking procedure". The final font which will be used in
     * this panel is mFinalFont
     */
    private Font mBaseFont = new Font("LucidaSans", Font.BOLD, 10);

    /**
     * Final font that will be used for rendering the panel.
     */
    private Font mFinalFont = null;

    /**
     * Name of the column that we show
     */
    private String mFinalColumnName = "";


    /**
     *
     * @param g
     * @param conf
     * @param model
     * @param rec
     * @param columns
     * @param w
     * @param h
     */
    @Override
    public synchronized void drawPanel(Graphics g, StackDrawingConfig conf, CompoundTableModel model, List<CompoundRecord> rec, CardElement ce, int[] columns, int w, int h) {

        //long timestamp = System.currentTimeMillis();
        Set<Integer> recordSet = rec.stream().mapToInt( ri -> ri.getID() ).boxed().collect(Collectors.toSet());
        //System.out.println(rec.size()+" Cards --> Took: "+ (System.currentTimeMillis()-timestamp) + " ms");


        boolean isCategorical = model.isColumnTypeCategory(columns[0]);
        boolean isDouble      = model.isColumnTypeDouble(columns[0]);

        if( isCategorical ) {
            int numCat = model.getCategoryCount(columns[0]);
            if( numCat <= mThresholdCategorical ) {
                // draw bars..
                drawBarChart(g, conf, model, rec, columns, w, h);
            }
            else{
                isCategorical = false;
            }
        }
        if( !isCategorical && isDouble ){
            // draw density
            drawDensityChart(g, conf, model, rec, columns, w, h);
        }

        g.setFont(mFinalFont);
        double posTextY = ( h- (h-g.getFontMetrics().getHeight())/2 ) - g.getFontMetrics().getDescent();

        g.setColor(Color.black);
        g.drawString(mFinalColumnName+":",(int)(0.00*w) + CONF_MARGIN_LEFT , (int)( posTextY ) );
    }


//-------- For nuemric values / drawing density ------------------------------------------------------------------------

    private void drawDensityChart(Graphics g, StackDrawingConfig conf, CompoundTableModel model, List<CompoundRecord> rec, int[] columns, int w, int h)
    {
        //long timestamp = System.currentTimeMillis();
        Set<Integer> recordSet = rec.stream().mapToInt( ri -> ri.getID() ).boxed().collect(Collectors.toSet());

        BufferedImage bimDistributionSlice = mCacheBimDistributionSlices.get(recordSet);
        if(bimDistributionSlice==null){
            if(log_recompute){System.out.println("StackSingleDataPanel::drawPanel : recompute DistributionSlice BIM..");};
            double values[] = rec.stream().mapToDouble( x -> x.getDouble(columns[0])).filter( x -> Double.isFinite(x) ).toArray();
            bimDistributionSlice = initDistributionSliceBIM( values , w , h);
            mCacheBimDistributionSlices.put( recordSet , bimDistributionSlice );
        }

        BufferedImage bimSubDistribution = mCacheBimSubDistributions.get(recordSet);
        if(bimSubDistribution==null){
            if(log_recompute){System.out.println("StackSingleDataPanel::drawPanel : recompute SubDistribution BIM..");};
            double values[] = rec.stream().mapToDouble( x -> x.getDouble(columns[0])).filter(di -> Double.isFinite(di) ).toArray();
            bimSubDistribution = initSubDistributionBIM( recordSet, values , w , h);
            mCacheBimSubDistributions.put(recordSet,bimSubDistribution);
        }

//        g.setFont(mFinalFont);
//        double posTextY = ( h- (h-g.getFontMetrics().getHeight())/2 ) - g.getFontMetrics().getDescent();
//
//        g.setColor(Color.black);
//        g.drawString(mFinalColumnName+":",(int)(0.00*w) + CONF_MARGIN_LEFT , (int)( posTextY ) );


        int w2           = (int) ( (1.0-mDividerPosB) * w );

        int h_density    = h - 2 * mConfMarginY;

        if(mBimDistribution==null){}

        // determine the difference between maxY values..
        if(true) {
            g.drawImage(mBimDistribution, mConfMarginX + (int) (mDividerPosB * w), mConfMarginY, w2 - 2 * mConfMarginX, h_density, null);
        }
        //g.drawImage(mBimSubDistribution     ,mConfMarginX + (int)(mDividerPosA*w) , mConfMarginY,w2-2*mConfMarginX,h-2*mConfMarginY, null);

        // determine height / position of subdensity:
        double maxY_SubDistribution = mCacheMaxY_SubDistributions.get(recordSet);
        int h_subdensity   = (int) Math.max( 2 , ( (maxY_SubDistribution / mMaxY_FullDistribution) * h_density ) );
        h_subdensity       = Math.min( h_subdensity, h_density );
        int y_subdensity   = (int) ( mConfMarginY + ( h_density - h_subdensity ) );

        //System.out.println("h-density="+h_density+" h-subdensity="+h_subdensity);

        if(true) {
            g.drawImage(bimDistributionSlice, mConfMarginX + (int) (mDividerPosB * w), mConfMarginY, w2 - 2 * mConfMarginX, h_density, null);
            g.drawImage(bimSubDistribution, mConfMarginX + (int) (mDividerPosB * w), y_subdensity, w2 - 2 * mConfMarginX, h_subdensity, null);

        }
    }


    private BufferedImage initDistributionSliceBIM(double values[] , int w, int h)
    {
        List<Double> dlist = Arrays.stream(values).filter( di -> Double.isFinite(di) ).boxed().collect(Collectors.toList());
        BufferedImage bimDistributionSlice = null;

        if(dlist.isEmpty()){
            bimDistributionSlice = new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
        }
        else {
            mDensityDrawer.setDensityPaint( new Color( 0,0,200 , 60 ) );
            mDensityDrawer.setDensityLinePaint( null );// new Color( 0,0,0 , 60 ) );

            //int oversampling = mConfOversampling;
            //int w2           = (int) w ; //( (1.0-mDividerPosB) * w );
            //bimDistributionSlice = mDensityDrawer.drawDensitySlices(      oversampling * ( w2 - 2 * mConfMarginX ), oversampling * ( h - 2 * mConfMarginY ), dlist  );
            bimDistributionSlice   = mDensityDrawer.drawDensitySlices(mConfImageResolutionX, mConfImageResolutionY, dlist );
            //mBimDistributionSlice = mDensityDrawer.drawDensitySlices(      oversampling * ( w2 - 2 * mConfMarginX ), oversampling * ( h - 2 * mConfMarginY ), dlist  );
        }
        return bimDistributionSlice;
    }


    /**
     * This is set in initDistributionBIM.
     * This value is then used to draw the subdistribution BIM.
     */
    //private double mPixelPerY = -1;
    private int    mNumSamplesFullDistribution = -1;
    //private double mMaxY_FullDistribution = -1;
    //private double  mCacheMaxY_FullDistributions = -1;

    private HashMap<Set<Integer>,Double> mCacheMaxY_SubDistributions  = new HashMap<>();
    //private double mMaxY_SubDistribution  = -1;


    private void initDistributionBIM(List<Double> dlist , int w, int h){

        this.mDensityDrawer = new DensityDrawer( dlist , mConfRelBandwidth , mConfSamplingResolution );

        // !! This is the way to count the number of samples, i.e. without NaN/Inf values !!
        this.mNumSamplesFullDistribution = mDensityDrawer.getDensityEstimator().getSamples().size();

        mDensityDrawer.setDensityLinePaint( null ); //) new Color( 0,0,0 , 160 ) );
        mDensityDrawer.setDensityLineStroke(new BasicStroke(2));
        //mDensityDrawer.setDensityPaint(Color.orange);

        //int oversampling = mConfOversampling;
        int w2           = (int) w; //( (1.0-mDividerPosB) * w );

        Paint densityPaint = new Color( 40,40,40 , 160 );  //new GradientPaint( 0 ,0, Color.orange.brighter().brighter()  , this.mConfOversampling * w2  ,0 ,  Color.red.darker().darker() );
        mDensityDrawer.setDensityPaint(densityPaint);

        // set MaxY value:
        this.mMaxY_FullDistribution = Arrays.stream( mDensityDrawer.getDensityEstimate()[1] ).max().getAsDouble();

        double pixelY        = mConfImageResolutionY;
        this.mBimDistribution = mDensityDrawer.drawDensity(mConfImageResolutionX, mConfImageResolutionY);


        //this.mBimDistribution = mDensityDrawer.drawDensity(  oversampling * ( w2 ), oversampling * ( h ) );
        //this.mBimDistribution = mDensityDrawer.drawDensity_A(  oversampling * ( w2 ), oversampling * mPixelPerY );
        //this.mBimDistribution = mDensityDrawer.drawDensity(  oversampling * ( w2 - 2 * mConfMarginX ), oversampling * ( h - 2 * mConfMarginY ) );
        //this.mBimDistribution = mDensityDrawer.drawDensity(  oversampling * ( w2 - 2 * mConfMarginX ), mPixelPerY );

    }


    /**
     *
     * @param values
     * @param w
     * @param h
     */
    private BufferedImage initSubDistributionBIM( Set<Integer> recordSet , double values[] , int w, int h) {

        List<Double> dlist = Arrays.stream(values).filter( di -> Double.isFinite(di) ).boxed().collect(Collectors.toList());
        double interval[] = this.mDensityDrawer.getInterval();

        // !! This is the right way to count, i.e. no Inf/NaN values !!
        double normalization = ((double)dlist.size()) / mNumSamplesFullDistribution;
//        double normalization = 1.0;

        //System.out.println("NORMALIZATION: "+normalization);

        this.mDensityDrawerSubDistribution = new DensityDrawer( dlist , mConfRelBandwidth , mConfSamplingResolution , interval[0] , interval[1] , normalization  );
        //mDensityDrawerSubDistribution.setDensityPaint(Color.cyan.darker().darker());
        //mDensityDrawerSubDistribution.setDensityLinePaint(Color.black);
        mDensityDrawerSubDistribution.setDensityPaint( Color.red.darker());
        mDensityDrawerSubDistribution.setDensityLinePaint(null);

        // set MaxY value:
        double maxY_SubDistribution = Arrays.stream( mDensityDrawerSubDistribution.getDensityEstimate()[1] ).max().getAsDouble();
        //mMaxY_SubDistribution       = maxY_SubDistribution;
        mCacheMaxY_SubDistributions.put(recordSet,maxY_SubDistribution);

        //int oversampling = mConfOversampling;
        //int w2           = (int) w;//( (1.0-mDividerPosB) * w );

        BufferedImage bimSubDistribution = null;
        bimSubDistribution               = mDensityDrawerSubDistribution.drawDensity(this.mConfImageResolutionX, this.mConfImageResolutionY); //oversampling * this.mPixelPerY);

        return bimSubDistribution;
    }


//-------- For categoric values / drawing bars ------------------------------------------------------------------------

    //private Map<Integer,Integer> mNumCategories = new HashMap<>();

    /**
     * Maps column number to counts for the "normalization"
      */
    private Map<Integer , Map<Integer,Integer> > mCatIndexToCount = new HashMap<>();

    /**
     * Maps column number to max number of crs in a category
     */
    private Map<Integer , Integer> mCatMaxCount = new HashMap<>();


    private Map<Integer,Double> mBarPlot_FinalWidthBar = new HashMap<>();
    private double mConf_BarPlot_MaxWidth = 28; // Width including margins around bar..,

    // if we have less than xx of the total amount, we anyway show a bar of this height.
    private int mConf_MinAbsBarHeight = 2;

    public void initCategoricalColumn( Graphics g, StackDrawingConfig conf, CompoundTableModel model, List<CompoundRecord> records, int[] columns, int w, int h ) {

        int col = columns[0];

        int numCategories = model.getCategoryCount(col);

        // compute counts for different categories:
        Map<Integer,Integer> catIndexToCount = new HashMap<Integer,Integer>();
        for(CompoundRecord cri : records){
            //int cat_i = model.getCategoryIndex(col,cri);
            int cat_i = model.getCategoryIndex(col, model.getTotalValueAt(cri.getID(),col) );
            if(catIndexToCount.containsKey( cat_i ) ) {
                catIndexToCount.put( cat_i , catIndexToCount.get( cat_i )+1 );
            }
            else{
               catIndexToCount.put( cat_i , 1 );
            }
        }

        // compute max count.
        int catMaxCount = catIndexToCount.entrySet().stream().mapToInt( ki -> ki.getValue() ).max().getAsInt();

        // compute width per bar..
        double barPlot_FinalWidthBar = Math.min( mConf_BarPlot_MaxWidth , (1.0*w-2*mConfMarginX-mDividerPosB*w) / (1.0*numCategories) );

        mCatIndexToCount.put(col,catIndexToCount);
        mCatMaxCount.put(col,catMaxCount);
        mBarPlot_FinalWidthBar.put(col,barPlot_FinalWidthBar);
    }

    private void drawBarChart(Graphics g, StackDrawingConfig conf, CompoundTableModel model, List<CompoundRecord> rec, int[] columns, int w, int h)
    {
        int col = columns[0];
        int numCategories = model.getCategoryCount(col);

        double y0        = (1.0*h - mConfMarginY );
        double yPerCount = ((1.0*h - 2*mConfMarginY )) / mCatMaxCount.get(col);

        // count categories in element:
        int catCounts[] = new int[numCategories];

        try
        {
            for (CompoundRecord ri : rec) {
                //catCounts[model.getCategoryIndex(col, ri)]++;
                int cat_idx = model.getCategoryIndex(col, model.getTotalValueAt(ri.getID(),col) );
                catCounts[ cat_idx ]++;
            }
        }
        catch(Exception e) {
            System.out.println("Exception in StackSingleDataPanel::drawBarChart() code..");
            e.printStackTrace();
        }

        // some more values for the drawing:
        double bar_xgap = 1.0;
        double barw     = this.mBarPlot_FinalWidthBar.get(col)-2*bar_xgap;

        int cscnt = 0;
        for( String cs : model.getCategoryList(col) ){
            double x0 = this.mConfMarginX + this.mDividerPosB * w + cscnt * mBarPlot_FinalWidthBar.get(col);

            // 1. draw background, then draw set
            g.setColor(Color.darkGray);
            int barb_h = 0;
            try {
                barb_h = (int) ((mCatIndexToCount.get(col).get(cscnt) * yPerCount));
            }
            catch(Exception e){
                System.out.println("Some exception in drawBarChart..\n"+e.toString());
                continue;
            }
            g.fillRect( (int)( x0+bar_xgap  ), (int)(y0 - barb_h), (int)barw , barb_h );

            g.setColor(Color.red.darker());
            double barx_h =  ( ( catCounts[cscnt] * yPerCount ) );

            if(barx_h>0){ barx_h = Math.max(barx_h,mConf_MinAbsBarHeight);}
            int barx_h_i = (int) barx_h;

            g.fillRect( (int) ( x0+bar_xgap ) , (int)( y0 - barx_h_i) , (int) barw , (int) barx_h_i );

            cscnt++;
        }
    }



//----------- END DRAWING ---------------------------------------------------------------------------------------------



    public void setDivA(double divA){
        this.mDividerPosA = divA;
    }

    public void setDivB(double divB){
        this.mDividerPosB = divB;
    }


    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public JComponent getLabel() {
        return null;
    }

    @Override
    public JPanel getConfigDialog() {
        return null;
    }

    @Override
    public int getNumberOfDataColumnsRequired() {
        return 1;
    }

    @Override
    public boolean canHandleColumnForSlot(CompoundTableModel model, int slot, int column) {
        return(model.isColumnTypeDouble(column));
    }

    @Override
    public void setColumns(int columns[]){
        if(columns.length!=1){
            System.out.println("SingleDataPanel : Wrong number of data columns supplied! "+columns.length+" instead of "+1);
        }
        this.mColumns[0] = columns[0];
    }

    @Override
    public int[] getColumns() {
        return mColumns;
    }

    @Override
    public void initalizeStackPanel(Graphics g, StackDrawingConfig conf, CompoundTableModel model, int focusListFlag, List<CompoundRecord> records, int[] columns, int w, int h) {

        // Determine text size and string to show:
        // determine font size:
        //this.mFinalFont = FontAndTextHelper.determineFittingFontByShrinking(g, mBaseFont, 16,  (int)(mDividerPosA * w) );
        //this.mFinalFont = FontAndTextHelper.determineFittingFontByShrinking(g, mBaseFont, 20,  (int)(w) );
        this.mFinalFont = mBaseFont.deriveFont(Font.PLAIN,12);

        String columnTitle = "<UNKNOWN NAME>";
        try {
            columnTitle = model.getColumnTitle(columns[0]);
            if(model.isLogarithmicViewMode(columns[0])){
                columnTitle = "log("+columnTitle+")";
            }
        }
        catch(Exception e){}

        try {
            mFinalColumnName = FontAndTextHelper.shrinkString(g, columnTitle, (int) (mDividerPosA * w));
        }
        catch(Exception e){
            System.out.println("ColumnTitle is null..");
            return;
        }


        boolean isCategorical = model.isColumnTypeCategory(columns[0]);
        boolean isDouble      = model.isColumnTypeDouble(columns[0]);

        if( isCategorical ) {
            int numCat = model.getCategoryCount(columns[0]);
            if(numCat < mThresholdCategorical ) {
                initCategoricalColumn(g, conf, model, records, columns, w, h);
            }
            else{
                isCategorical = false;
            }
        }

        if( !isCategorical && isDouble ) {
            // Init distribution BIM:
            List<Double> dlist = records.stream().mapToDouble(x -> x.getDouble(columns[0])).filter(xd -> Double.isFinite(xd)).boxed().collect(Collectors.toList());
            initDistributionBIM(dlist, mConfImageResolutionX, mConfImageResolutionY);
            //initDistributionBIM(dlist, (int) ((1.0-mDividerPosA)*w) , h );
        }

    }

    public void clearAllBufferedImages() {
        this.mCacheBimDistributionSlices = new HashMap<>();
        this.mCacheBimSubDistributions   = new HashMap<>();
        this.mCacheMaxY_SubDistributions = new HashMap<>();
    }

}
