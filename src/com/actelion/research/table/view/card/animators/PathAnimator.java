package com.actelion.research.table.view.card.animators;

import com.actelion.research.table.view.card.CardElement;
import com.actelion.research.table.view.card.JCardPane;
import com.actelion.research.table.view.card.positioning.CardPositionerInterface;

import java.awt.geom.*;
import java.util.HashMap;
import java.util.List;







// @ TODO implement sequential animation mode

public  class PathAnimator implements AnimatorInterface{

    public static final int ANIMATION_PATH_STYLE_STRAIGHT = 1;

    public static final int ANIMATION_MODE_PARALLEL   = 1;
    public static final int ANIMATION_MODE_SEQUENTIAL = 2;


    // determines the number of positions computed for every path
    public static final int PATH_RESOLUTION = 200;


    HashMap<CardElement,Point2D> mDestinations = new HashMap<>();



    // Animation style settings:

    private int mAnimationPathStyle = ANIMATION_PATH_STYLE_STRAIGHT;
    private int mAnimationMode      = ANIMATION_MODE_PARALLEL;


    // animation time:
    private double mAnimationTime = 1.0;

    // time stamp which defines the time when the animation was started
    private long mStartTime = -1;

    // parametrized curves (computed in initAnimation)
    private HashMap<CardElement, SampledPath> mPaths = new HashMap<>();


    // order of cards processing in case of sequential animation:
    private List<CardElement> mCardsOrder;


    public PathAnimator(){

    }


    public void setDestination(CardElement ce, Point2D pos){
        this.mDestinations.put(ce,pos);
    }

    public void setOrder(List<CardElement> cardsOrder){
        this.mCardsOrder = cardsOrder;
    }


    public int getAnimationPathStyle() {
        return mAnimationPathStyle;
    }

    public int getAnimationMode() {
        return mAnimationMode;
    }

    public void setAnimationPathStyle(int style){
        this.mAnimationPathStyle = style;
    }


    @Override
    public void initAnimation() {
        switch(this.mAnimationPathStyle){
            case ANIMATION_PATH_STYLE_STRAIGHT:
            {
                initStraightLines();
            }
            break;
        }
    }

    @Override
    public boolean animate(double timeInSeconds) {

        //System.out.println("animate.. t="+timeInSeconds);

        switch(this.getAnimationMode()){
            case ANIMATION_MODE_PARALLEL:
            {
                for(CardElement ce : this.mDestinations.keySet()){

                    SampledPath path = this.mPaths.get(ce);
                    Point2D point = path.at( timeInSeconds / this.mAnimationTime );

                    ce.setCenter(point.getX(), point.getY());
                }
            }
            break;
            case ANIMATION_MODE_SEQUENTIAL:
            {
                System.out.println("NOT YET SUPPORTED!");
            }
            break;
        }

        return timeInSeconds <= this.mAnimationTime;
    }


    // @TODO should probably be added to TimedAnimation interface
    public void setAnimationTime(double time) {
        this.mAnimationTime = time;
    }


    private void initStraightLines() {
        for( CardElement ce : this.mDestinations.keySet()){
            Line2D path = new Line2D.Double();
            path.setLine( new Point2D.Double( ce.getRectangle().getCenterX() , ce.getRectangle().getCenterY() ) , this.mDestinations.get(ce) );
            this.mPaths.put( ce , new SampledPath( new Path2D.Double(path) , -1.0 , 50 ) );
            //System.out.println("SampledPath n="+this.mPaths.get(ce).getSampledPoints().size());
        }
    }


    /**
     * Creates animator which spreads cards into grid. Currently, target slots (i.e. the specific grid positions) are
     * pretty much random for cards..
     * --> @TODO create MatchToDestinationsAnimator , which gets target slots, and then computes minimal matching between cards and slots and uses this as assignment.
     *
     *
     * @return the animator
     */
    public static PathAnimator createToGridPathAnimator( List<CardElement> cardsToGrid , double timeInSeconds , int gridX , int gridY ) {

        PathAnimator pa = new PathAnimator();

        // determine nice layout for grid:
        // take as center the center of the bounding box:
        Rectangle2D boundingbox =  JCardPane.getBoundingBox(cardsToGrid);

        double cx = boundingbox.getCenterX();
        double cy = boundingbox.getCenterY();

        double rel_grid_gap = 0.05;

        // put cards on grid of size gridX, gridY, starting from left upper corner
        double grid_height = gridY * ( cardsToGrid.get(0).getRectangle().getHeight() * (1.0+rel_grid_gap) ) ;
        double grid_width  = gridX * ( cardsToGrid.get(0).getRectangle().getWidth()  * (1.0+rel_grid_gap) ) ;

        double lu_x = cx - 0.5 * grid_width;
        double lu_y = cy - 0.5 * grid_height;

        for(int gxi = 0; gxi < gridX ; gxi++){
            for(int gyi = 0; gyi < gridY ; gyi++){
                int xy_count = (gxi + gridX*gyi);
                if( xy_count >= cardsToGrid.size() ){continue;}

                double cpx  =  lu_x + gxi * grid_width / gridX;
                double cpy  =  lu_y + gyi * grid_height / gridY;

                pa.setDestination( cardsToGrid.get(xy_count) , new Point2D.Double( cpx , cpy ) );
            }
        }

        pa.setAnimationTime(timeInSeconds);

        return pa;
    }



    public static PathAnimator createToPositionPathAnimator(List<CardElement> cardsToPosition , CardPositionerInterface positioner , double timeInSeconds ){
        PathAnimator pa = new PathAnimator();

        for( CardElement ce : cardsToPosition ){
             Point2D pos =  positioner.positionSingleCard(ce,null);
             pa.setDestination(ce,pos);
        }
        pa.setAnimationTime(timeInSeconds);

        return pa;
    }

    public static PathAnimator createToPositionPathAnimator(List<CardElement> cardsToPosition , List<Point2D> positions , double timeInSeconds ) {
        PathAnimator pa = new PathAnimator();

        int cCnt = 0;
        for( CardElement ce : cardsToPosition ){

            Point2D pos =  positions.get(cCnt);
            pa.setDestination(ce,pos);
            cCnt++;

            //System.out.println("Card "+cCnt+" "+pos.getX()+"/"+pos);
            //System.out.println("Card "+cCnt+" "+pos.getY()+"/"+pos);

        }
        pa.setAnimationTime(timeInSeconds);
        return pa;
    }

}

//
//
//// Is this functionality somewhere in java2d?
//
///**
// * Generates equidistant samples at distance "equidistance" from Path2D
// */
//class SampledPath{
//
//    public Path2D mPath;
//
//    List<Point2D> mSampledPoints;
//
//    /**
//     * Decision whether equidistance or n_samples is used based on which one is not negative.
//     *
//     * @param path
//     * @param equidistance
//     * @param n_samples
//     */
//    public SampledPath(Path2D path, double equidistance, int n_samples ) {
//
//        PathIterator pi = path.getPathIterator(null);
//        FlatteningPathIterator iter = new FlatteningPathIterator(pi, 0.01, 8);
//
//
//        this.mSampledPoints = new ArrayList<>();
//        List<Point2D> mSampledPointsPre = new ArrayList<>();
//
//        double coords[] = new double[2];
//
//        double totalLength = 0.0;
//
//        Point2D p_last = null;
//        while (!iter.isDone()) {
//            iter.currentSegment(coords);
//            int x = (int) coords[0];
//            int y = (int) coords[1];
//            Point2D p_next = new Point2D.Double(x, y);
//
//            mSampledPointsPre.add(p_next);
//            iter.next();
//            if(p_last!=null){
//                totalLength += p_last.distance(p_next) ;
//            }
//            p_last = p_next;
//        }
//
//        // Use n_samples definition if possible..
//        if(n_samples > 0){
//            equidistance = totalLength / n_samples;
//        }
//
//        // Briefly check if we actually dont move. Then we will end up in an endless loop if we don't break here..
//        if(totalLength==0.0){
//            mSampledPoints.addAll(mSampledPointsPre);
//        }
//        else
//        {
//
//            // sample equidistant:
//            this.mSampledPoints = new ArrayList<>();
//
//            double remainder_from_last = 0.0; // remainder from last segment
//            p_last = null;
//
//            // current sampling position
//            Point2D position = null;
//
//            for (Point2D p : mSampledPointsPre) {
//                if (p_last != null) {
//                    remainder_from_last += p.distance(p_last);
//                    double v_normed_x = (p.getX() - p_last.getX()) / remainder_from_last;
//                    double v_normed_y = (p.getY() - p_last.getY()) / remainder_from_last;
//
//                    while (remainder_from_last >= equidistance) {
//                        position = new Point2D.Double(position.getX() + v_normed_x * equidistance, position.getY() + v_normed_y * equidistance);
//                        mSampledPoints.add(position);
//                        remainder_from_last -= equidistance;
//                        //System.out.println("x="+position.getX()+"y="+position.getY());
//                    }
//
//                } else {
//                    mSampledPoints.add(p);
//                    position = p;
//                }
//
//                p_last = p;
//            }
//            //System.out.println("Sampled points: "+this.getSampledPoints().size());
//            // add endpoint extra..
//            mSampledPoints.remove(mSampledPoints.size() - 1);
//            mSampledPoints.add(p_last);
//        }
//    }
//
//    public List<Point2D> getSampledPoints(){
//        return this.mSampledPoints;
//    }
//
//    public Point2D at(double t){
//        int pos = (int) ( t * (double) this.getSampledPoints().size() );
//        pos = Math.min(mSampledPoints.size()-1,pos);
//        pos = Math.max(0,pos);
//        return mSampledPoints.get(pos);
//    }
//
//
//
//}
//
//
