package com.actelion.research.table.view.card.cardsurface.density;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;


/**
 *
 *
 *
 *
 */
public class DensityDrawer {



    /**
     * X / Y values of the estimated denstiy
     */
    double mVX[];
    double mVY[];


    /**
     * NOTE: this is the sampling resolution, and has nothing to do with any graphics properties of the rendered images etc..
     *
     */
    private int mSamplingResolution   = 200;

    /**
     * The minimal relative width of drawn slices (relative to the full interval).
     */
    private double mMinRelSliceWidth  = 0.02;



    KernelDensityEstimator1D mEstimator = null;

    /**
     * XLim is set by a and b. If NaN is supplied, we just take the value of the smallest / largest sample.
     * Normalization is the value of the integral of the estimated density.
     *
     * @param data
     * @param relBandwidth
     * @param a
     * @param b
     */
    public DensityDrawer(Collection<Double> data, double relBandwidth , int samplingResolution, double a, double b, double normalization){

        double okDoubles[] = data.stream().filter( di ->  ( !di.isInfinite() ) && ( !di.isNaN() ) ).mapToDouble(dxi -> dxi.doubleValue()).toArray();
        if(okDoubles.length<data.size()){
            System.out.println("Warning: removed NaN/Inf values");
        }

        mEstimator = new KernelDensityEstimator1D(  data.stream().mapToDouble( di -> di.doubleValue() ).filter( di->Double.isFinite(di) ).toArray() , relBandwidth , a, b, normalization);

        this.mSamplingResolution = samplingResolution;

        this.recomputeDensity();
    }

    public DensityDrawer(Collection<Double> data, double relBandwidth , int resolution){
        this(data,relBandwidth,resolution,Double.NaN,Double.NaN, 1.0);
    }


    public DensityEstimator1D getDensityEstimator(){
        return mEstimator;
    }


    /**
     * Removes all NaN and +- Infs..
     *
     */
    public void setData(List<Double> data){
        double okDoubles[] = data.stream().filter( di ->  ( !di.isInfinite() ) && ( !di.isNaN() ) ).mapToDouble(dxi -> dxi.doubleValue()).toArray();
        if(okDoubles.length<data.size()){
            System.out.println("Warning: removed NaN/Inf values");
        }
        this.mEstimator.setData(okDoubles);
        this.recomputeDensity();
    }

    /**
     * Sets the bandwidth relative to max(data)-min(data)..
     *
     * @param rel
     */
    public void setRelativeBandwidth(double rel){
        double fullInterval[] = this.getInterval();
        this.mEstimator.setBandwidth( rel * (fullInterval[1] - fullInterval[0] ) );
    }


    public void setInterval(double a, double b){
        this.mEstimator.setInterval(a,b);
    }

    public double[] getInterval(){
        return this.mEstimator.getInterval();
    }

    public double getMaxDensityValue(){
        return Arrays.stream(this.mVY).max().orElseGet(()-> 0.0);
    }

    public static double[] getLinSpace(double pa, double pb, int resolution) {
        double xx[] = new double[resolution];
        double ab  = pb-pa;
        double abi = ab / (resolution-1);
        for(int zi=0;zi<resolution;zi++){ xx[zi] = pa + abi * zi; }
        return xx;
    }

    Paint mPaintDensity      = Color.orange.darker();

    Stroke mStrokeDensity    = new BasicStroke(1.5f);

    Color mPaintDensityLine  = Color.white;


    /**
     * Returns two double arrays, first one contains the x values, second one the y values of the estimated density
     *
     * @return array of two double arrays containing x and y coordinates.
     */
    public double[][] getDensityEstimate(){
        return new double[][]{ mVX , mVY };
    }

    public void setDensityPaint(Paint p){
        this.mPaintDensity = p;
    }

    public void setDensityLineStroke(Stroke s){
        this.mStrokeDensity = s;
    }

    public void setDensityLinePaint( Color c ){
        this.mPaintDensityLine = c;
    }

    public void recomputeDensity(){
        //this.mSamplingResolution = resolution;

        //mVX = new double[mResolution];
        double interval[] = this.getInterval();

        double mA = interval[0];
        double xb = interval[1];

        mVX = getLinSpace(mA,xb,mSamplingResolution);
        mVY = mEstimator.estimate(mVX) ;
    }


    /**
     * Retuns Path2D containing the contour of the density.
     *
     *
     * @param w
     * @param h
     * @param startX
     * @param endX
     * @return
     */
    public Path2D getDensitySlice( int w, int h, double startX, double endX){
        startX = Math.max(mVX[0],startX);
        endX   = Math.min(mVX[mVX.length-1],endX);

        double max_y = Arrays.stream(mVY).max().getAsDouble();
        double yScale = h / max_y; // y-pixel per y value

        // don't go below sampling resolution.. find region:
        //double resolutionPerSample =  (1.0*this.mSamplingResolution) / this.mData.size();

        // find exact position
        double posA = mSamplingResolution * (startX - this.mVX[0]) / ( mVX[mVX.length-1] - mVX[0] );
        double posB = mSamplingResolution * (endX   - this.mVX[0]) / ( mVX[mVX.length-1] - mVX[0] );

        return getDensitySlice(w, h, (int) posA, (int) posB);
    }

    /**
     * Draw density. Parameters indicate indeces in mVX, both sides are inclusive.
     *
     *
     * @param xStartIdx
     * @param xEndIdx
     */
    private Path2D getDensitySlice( int w, int h, double yScale, int xStartIdx , int xEndIdx ){

        // create polygon: start at x(a) -> y(a) ->  ... y(..) ... y(b) -> x(b) -> x(a)
        Path2D p = new Path2D.Double();

        double xa = xStartIdx   * ( (1.0*w) / (mSamplingResolution-1) );
        double xb = (xEndIdx)   * ( (1.0*w) / (mSamplingResolution-1) );

        p.moveTo(xa, h- 0 );
        p.lineTo(xa, h- mVY[xStartIdx] * yScale );
        for(int zi=xStartIdx+1;zi<=xEndIdx;zi++){ p.lineTo(   zi*(1.0*w) / (mSamplingResolution-1)  , h - mVY[zi] * yScale ); }
        //p.lineTo(0, 0);

        p.lineTo( xb , h- 0 );
        p.closePath();
        return p;
    }


    //public Path2D createPath

    /**
     *
     * Draws the density estimate.
     *
     * Drawing coordinates: (mVX[0],0) -> (0,h) ; mVX[end] -> (w) ;  max(mVY) ->  0 ; ( mVX[end],max(mVY) ) -> (w,0)
     *
     *
     * @param w
     * @param h
     * @return
     */
    public BufferedImage drawDensity(int w, int h){


        //if(lineStyle==null){ lineStyle = new BasicStroke(1.0f); }
        //if(fillStyle==null){ fillStyle = Color.ORANGE.darker(); }

        BufferedImage bim = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = bim.createGraphics();

        // Flip the image vertically with this transform..
        //AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
        //tx.translate(0, -h);
        //g.transform(tx);

        double max_y = Arrays.stream(mVY).max().getAsDouble();
        double yScale = h / max_y; // y-pixel per y value

        // create polygon:
        Path2D p = getDensitySlice(w, h, yScale, 0 , mSamplingResolution-1 );

        g.setPaint( mPaintDensity );
        g.fill(p);

        if( mPaintDensityLine != null ) {
            g.setColor(mPaintDensityLine);
            g.setStroke(mStrokeDensity);
            g.draw(p);
        }
        g.dispose();

        return bim;
    }


    public BufferedImage drawDensity_A(int w, double pixelPerY ){

        double max_y = Arrays.stream(mVY).max().getAsDouble();

        int h        = (int) ( max_y * pixelPerY );
        if(h==0){h = 4;}

        double yScale = pixelPerY;

        BufferedImage bim = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bim.createGraphics();

        // create polygon:
        Path2D p = getDensitySlice(w, h, yScale, 0 , mSamplingResolution-1 );

        g.setPaint( mPaintDensity );
        g.fill(p);

        if( mPaintDensityLine != null ) {
            g.setColor(mPaintDensityLine);
            g.setStroke(mStrokeDensity);
            g.draw(p);
        }
        g.dispose();

        return bim;
    }


    /**
     * Draw density slices for the samples in "marked".
     *
     * Drawing coordinates: mVX[0] -> (0) ; mVX[end] -> (w) ; max(mVY) ->  h ; ( mVX[end],max(mVY) ) -> (w,h)
     *
     * @param w
     * @param h
     * @param marked
     * @return
     */
    public BufferedImage drawDensitySlices(int w, int h, List<Double> marked){
        BufferedImage bim = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = bim.createGraphics();

        double max_y = Arrays.stream(mVY).max().getAsDouble();
        double yScale = h / max_y; // y-pixel per y value

        // don't go sampling resolution.. find slices which are "in":
        double resolutionPerSample = mMinRelSliceWidth;  //(1.0*this.mSamplingResolution) / this.mData.size();

        boolean inSamples[] = new boolean[mSamplingResolution];

        for( double m : marked )
        {
           // find exact position
           double pos = mSamplingResolution * (m - this.mVX[0]) / ( mVX[mVX.length-1] - mVX[0] );
           int sa  = (int) ( pos-resolutionPerSample/2 ); //(int) Math.floor( pos-resolutionPerSample/2 );
           int sb  = (int) ( pos+resolutionPerSample/2 ); //Math.ceil( pos+resolutionPerSample/2 );

           sa = Math.max(0,sa);
           sb = Math.min(inSamples.length-1,sb);
           for(int sc = sa;sc<=sb;sc++){ inSamples[sc] = true; }
        }


        List<int[]> intervalsToMark = new ArrayList<>();
        int bPos          = 0;
        int startInterval = -1;

        while( bPos < mSamplingResolution ){
            if(startInterval < 0){
                if(inSamples[bPos]){
                    startInterval = bPos;
                    bPos++;
                }
                else{
                    bPos++;
                }
            }
            else{
                if(inSamples[bPos]){
                    bPos++;
                }
                else{
                    // draw interval startInterval : (bPos-1)
                    intervalsToMark.add(new int[]{startInterval,(bPos-1)});
                    startInterval = -1;
                    bPos++;
                }
            }
        }
        // check for last open interval:
        if(startInterval > 0){ intervalsToMark.add( new int[]{ startInterval,mSamplingResolution-1} ); }

        // now draw those intervals..
        Random r = new Random();
        for(int interval[] : intervalsToMark){
            g.setPaint( mPaintDensity );
            Path2D ds = getDensitySlice(w, h, yScale,interval[0],interval[1]);

            g.fill(ds);

            if(mPaintDensityLine!=null) {
                g.setColor(mPaintDensityLine);
                g.draw(ds);
            }
        }
        g.dispose();
        return bim;
    }



        /**
         * Create the GUI and show it.  For thread safety,
         * this method should be invoked from the
         * event-dispatching thread.
         */
        private static void createAndShowGUI() {
            //Create and set up the window.
            JFrame frame = new JFrame("HelloWorldSwing");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            //Add the ubiquitous "Hello World" label.
            JLabel label = new JLabel("Hello World");

            frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(),BoxLayout.Y_AXIS));
            frame.getContentPane().add(label);

            Random r = new Random();
            List<Double> dataA = new ArrayList<>();
            for(int zi=0;zi<500;zi++){ dataA.add(r.nextGaussian());}

            List<Double> slices = dataA.subList(30,130);

            DensityDrawer dd1 = new DensityDrawer( dataA , 0.05 , 200);
            //dd1.setKernel(DensityDrawer.KERNEL_UNIFORM);
            dd1.recomputeDensity();

            DensityDrawer dd2 = new DensityDrawer( dataA , 0.05 , 200);
            //dd2.setKernel(DensityDrawer.KERNEL_TRIANGULAR);
            dd2.recomputeDensity();

            DensityDrawer dd3 = new DensityDrawer( dataA , 0.05 , 200);

            DensityDrawer dd4 = new DensityDrawer( dd3.getDensityEstimator().getSamples().subList(0,250) , 0.05 , 200, dd3.getInterval()[0] , dd3.getInterval()[1] , 0.5  );
            //dd3.setKernel(DensityDrawer.KERNEL_EPANECHNIKOV);
            dd3.recomputeDensity();

            GradientPaint gp = new GradientPaint(0,0,Color.orange,800,0, Color.red );
            dd1.setDensityPaint(gp);
            dd2.setDensityPaint(gp);
            dd3.setDensityPaint(gp);
            dd4.setDensityPaint(gp);


            JLabel labelX1 = new JLabel( new ImageIcon( dd1.drawDensity(800,200) ) );
            frame.getContentPane().add( labelX1 );

            JLabel labelX1s = new JLabel( new ImageIcon( dd1.drawDensitySlices(800,200, slices) ) );
            frame.getContentPane().add( labelX1s );

            JLabel labelX2 = new JLabel( new ImageIcon( dd2.drawDensity(800,200) ) );
            frame.getContentPane().add( labelX2 );

            JLabel labelX2s = new JLabel( new ImageIcon( dd2.drawDensitySlices(800,200, slices) ) );
            frame.getContentPane().add( labelX2s );

            JLabel labelX3 = new JLabel( new ImageIcon( dd2.drawDensity(800,200) ) );
            frame.getContentPane().add( labelX3 );

            JLabel labelX3s = new JLabel( new ImageIcon( dd3.drawDensitySlices(800,200, slices) ) );
            frame.getContentPane().add( labelX3s );

            JLabel labelX4 = new JLabel( new ImageIcon( dd3.drawDensity_A(800, 200 / dd3.getMaxDensityValue() ) ) );
            frame.getContentPane().add( labelX4 );

            JLabel labelX5 = new JLabel( new ImageIcon( dd4.drawDensity_A(800, 200 / dd3.getMaxDensityValue() ) ) );
            frame.getContentPane().add( labelX5 );






//            JLabel label3 = new JLabel( new ImageIcon( dd.drawDensitySlices(800,200, dataA.subList(0, 200)) ) );
//            frame.getContentPane().add( label3 );
            System.out.println( String.format( "Max1 = %6.3f , Max2 = %6.3f , Max3 = %6.3f " , Arrays.stream(dd1.mVX).max().getAsDouble() , Arrays.stream(dd2.mVX).max().getAsDouble() , Arrays.stream(dd3.mVX).max().getAsDouble() ) );

            frame.getContentPane().setBackground(Color.black);

            //Display the window.
            frame.pack();
            frame.setVisible(true);
        }

        public static void main(String[] args) {
            //Schedule a job for the event-dispatching thread:
            //creating and showing this application's GUI.
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    createAndShowGUI();
                }
            });



        }
    }


