package com.actelion.research.table.view.card.cardsurface.density;

import java.util.*;

public class KernelDensityEstimator1D implements DensityEstimator1D{


    /**
     * NOTE: is guaranteed to be sorted in ascending order
     */
    private List<Double> mData;


    /**
     * Interval. If NaN, then this will be done automatically
     */
    double mA = Double.NaN;
    double mB = Double.NaN;

    /**
     * Value of the integral of the estimated density
     */
    double mNormalization = 1.0;


    /**
     * Bandwidth
     */
    private double mBandwidth = 0.1;


    /**
     * Kernel used
     */
    private int mKernel = KERNEL_EPANECHNIKOV ;//KERNEL_EPANECHNIKOV;

    public static final int KERNEL_UNIFORM = 1;
    public static final int KERNEL_TRIANGULAR = 2;
    public static final int KERNEL_EPANECHNIKOV = 3;


    /**
     * XLim is set by a and b. If NaN is supplied, we just take the value of the smallest / largest sample.
     * Normalization is the value of the integral of the estimated density.
     *
     * @param data
     * @param relBandwidth
     * @param a
     * @param b
     */
    public KernelDensityEstimator1D( double data[], double relBandwidth , double a, double b, double normalization){

        this.mA = a;
        this.mB = b;

        this.setData( data );
        this.setRelativeBandwidth(relBandwidth);

        this.mNormalization = normalization;

        //this.recomputeDensity(mResolution);
    }


    /**
     * Sets the bandwidth relative to max(data)-min(data)..
     *
     * @param rel
     */
    public void setRelativeBandwidth(double rel){
        double fullInterval[] = this.getInterval();
        this.setBandwidth( rel * (fullInterval[1] - fullInterval[0] ) );
    }

    @Override
    public void setData(double[] data) {
        this.mData = new ArrayList<>(data.length);
        Arrays.stream( data ).sorted().forEachOrdered( di -> mData.add(di) );
    }

    public void setData(Collection<Double> data){
        this.setData( data.stream().mapToDouble(  di -> di.doubleValue() ).toArray() );
    }

    @Override
    public List<Double> getSamples() {
        return mData;
    }

    @Override
    public void setBandwidth(double bw) {
        mBandwidth = bw;
    }

    @Override
    public void setInterval(double a, double b) {
        this.mA = a; this.mB = b;
    }

    @Override
    public double[] estimate(double vx[]){

        // @TODO: should we start at min - half bandwidth? (Maybe not and the current one is better..)
        // init vx:

        int n_elements = this.getSamples().size();

        double intervalX[] = this.getInterval();
        double xa = intervalX[0];
        double xb = intervalX[1];

        double ab  = xb-xa;

        int n = vx.length;

        double vy[] = new double[n];

        // compute vy: (without normalization, we do this afterwards..)
        for(int zi=0;zi<n;zi++){
            double x  = vx[zi];

            // find first/last relevant data point:
            int ai=0;
            for(;ai<mData.size();ai++){
                if( mData.get(ai) >= x - 0.5*mBandwidth ){
                    break;
                }
            }
            int bi=ai;
            for(;bi<mData.size();bi++){
                if( mData.get(bi) > x + 0.5*mBandwidth ) {
                    break;
                }
            }

            // apply kernel:
            double vyi = 0.0;

            switch( mKernel ) {
                case KERNEL_UNIFORM:
                    vyi = (bi - ai) / ( 4*mBandwidth ) ; // right normalization(?)
                    break;
                case KERNEL_TRIANGULAR:
                    //for(int vi=ai;vi<bi;vi++){ vy +=  mBandwidth - Math.max(0, 0.5*mBandwidth-Math.abs((mData.get(vi)-x)) ); }
                    for(int vi=ai;vi<bi;vi++){ double xu = Math.abs( mData.get(vi)-x) / (0.5*mBandwidth); vyi +=  1.0-xu; }
                    break;
                case KERNEL_EPANECHNIKOV:
                    for(int vi=ai;vi<bi;vi++){ double xu = Math.abs( mData.get(vi)-x) / (0.5*mBandwidth); vyi += 0.75 * ( 1-xu*xu ); }
                    break;
            }
            vy[zi] = mNormalization * vyi / ( n_elements * 0.5*mBandwidth );
        }



        return vy;
    }

//    public static double[] getLinSpace(double pa, double pb, int resolution) {
//        double xx[] = new double[resolution];
//        double ab  = pb-pa;
//        double abi = ab / (resolution-1);
//        for(int zi=0;zi<resolution;zi++){ xx[zi] = pa + abi * zi; }
//        return xx;
//    }


    @Override
    public double getBandwidth() {
        return 0;
    }


    public double[] getInterval() {

        DoubleSummaryStatistics dss = mData.stream().mapToDouble(dd -> dd.doubleValue() ).summaryStatistics();

        // A automatically?
        double xa = this.mA;
        if( Double.isNaN( this.mA ) ){
            xa = dss.getMin();
        }
        // B automatically?
        double xb = this.mB;
        if( Double.isNaN( this.mB ) ){
            xb = dss.getMax();
        }

        return new double[]{xa,xb};
    }


    public double[] getIntervalRaw() {
        return new double[]{mA,mB};
    }

    @Override
    public void setNormalization(double normalization) {
        this.mNormalization = normalization;
    }

    public static void main(String args[]){

        double data_01[] = new double[]{ 0.01 , 0.4 , 0.8 , 1.15 , 1.6 , 2.1 , 2.5 , 2.9 , 3.2 , 3.5 , 3.9 };

        KernelDensityEstimator1D kde = new KernelDensityEstimator1D( data_01 , 0.2 , 0.0,4.0,1.0 );

        double test_01[]  = new double[]{ 0.1,0.2,0.3,0.6,0.7,0.8,0.9 };
        double esti_01[]  = kde.estimate( test_01 );
        System.out.println("");
        Arrays.stream( esti_01 ).forEachOrdered( di -> System.out.println(di+""));
    }


}
