package com.actelion.research.table.view.card;

import com.actelion.research.table.view.card.animators.AnimatorInterface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

public class JFastCardPane extends JPanel implements JCardPane.CardPaneEventListener {

    /**
     * Modes explained:
     * SHOWING_REDUCED_CARD_PANE_MODEL : always shows the current reduced card elements from the supplied model
     * SHOWING_FULL_CARD_PANE_MODEL    : always shows the current all card elements from the supplied model
     * AUTONOMOUS_FROM_FULL            : means that it only at construction grabs the full card pane model, and then
     *                                   does not further care for the (original) card pane model.
     * AUTONOMOUS_FROM_REDUCED         : means that it only at construction grabs the reduced card pane model, and then
     *                                   does not further care for the (original) card pane model.
     *
     */
    public static enum FAST_PANE_MODE { SHOWING_REDUCED_CARD_PANE_MODEL , SHOWING_FULL_CARD_PANE_MODEL , AUTONOMOUS_FROM_FULL , AUTONOMOUS_FROM_REDUCED }

    // contains the card elements, depending on the mode it is updated in every paintComponent call or never.
    //private java.util.List<CardElement> mElements;


    private CardPaneModel<CardElement,?> mModel = null;

    private boolean mInternalBuffering = true;


    private double mConfig_RelativePadding = 0.075;
    private double mConfig_AbsolutePadding = 150;


    /**
     * If true, it is possible to drag the viewport on the fastcardpane.
     */
    private boolean mEnableViewportMovement = true;


    private FAST_PANE_MODE mMode;


    public JFastCardPane( CardPaneModel<CardElement,?> model , FAST_PANE_MODE mode  ){
        //this.mCardPane = cardPane;
        //this.mElements = elements;
        this.mModel = model;

        this.setOpaque(false);
        this.setVisible(true);

        this.mMode = mode;
        switch(mMode){
            case AUTONOMOUS_FROM_FULL:
            {
                //mElements = mModel.getAllElements();
                mModel = model.cloneModel();
            }
            break;
            case AUTONOMOUS_FROM_REDUCED:
            {
                //mElements = mModel.getReducedAllElements();
                mModel = model.cloneModel();
            }
            break;
        }

        ViewportMoverMouseListener vmml = new ViewportMoverMouseListener();
        this.addMouseListener( vmml );
        this.addMouseMotionListener( vmml );

        //this.setDoubleBuffered(false);
        this.mRecomputeService.setTimeIntervalMilliseconds(100);
    }



    public void setInternalBufferingEnabled(boolean enable){
        this.mInternalBuffering = enable;
    }

    public boolean isInternatlBufferingEnabled(){
        return this.mInternalBuffering;
    }

    public int  getUpdateRate(){ return this.mRecomputeService.getTimeIntervalMilliseconds(); }

    public void setUpdateRate(int intervalMS){ this.mRecomputeService.setTimeIntervalMilliseconds( intervalMS ); }

    long timestamp = 0;

    /**
     * Returns the visible elements in the pane.
     *
     * @return
     */
    //public List<CardElement> getVisibleElements() { return this.mElements;  }


    private Paint mPaintCardFill   = new Color( 10,10, 250 , 80 );
    private Paint mPaintCardBorder = Color.blue.darker();

    private Function<Integer,Color> mPaintStackFill    = (ci) -> new Color( Math.max(80,255-2*ci) ,  Math.max(60,120-ci) , Math.max(0,60-ci) , Math.min( 100 + ci , 255 ) );
    private Function<Integer,Color> mPaintStackBorder  = (ci) -> Color.orange.darker().darker();

    private Paint mPaintNonVisibleCardFill   = new Color( 160,160, 190 , 40 );
    private Paint mPaintNonVisibleCardBorder = new Color( 110,110, 140 , 80);


    public void setPaintCardFill(Paint p){this.mPaintCardFill   = p;}
    public void setPaintCardBorder(Paint p){this.mPaintCardBorder = p;}

    public void setPaintStackFill(Function<Integer,Color> ps){ this.mPaintStackFill   = ps; }
    public void setPaintStackBorder(Function<Integer,Color> ps){ this.mPaintStackBorder = ps; }


    /**
     * For the currently non-visible cards, only applies when the full card model is shown
     * @param p
     */
    public void setNonReducedCardFill(Paint p){
        this.mPaintNonVisibleCardFill = p;
    }

    public void setNonReducedCardBorder(Paint p){
        this.mPaintNonVisibleCardBorder = p;
    }



    private int mConfMaxElements = 100000;



    private BufferedImage mBIM_Active = null;
    private BufferedImage mBIM_Hidden = null;


    Object mMonitor_A = new Object();

    Object mMonitor_B = new Object();

    public void initBIMs(int w, int h){

        w=Math.max(1,w);
        h=Math.max(1,h);
        System.out.println("JFastCardPane:: INIT / REINIT BIMs!   w="+getWidth()+"  h="+getHeight());
        synchronized(mMonitor_B) {
            synchronized(mMonitor_A) {

                mBIM_Active = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                mBIM_Hidden = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            }
        }
    }

    @Override
    public void paintComponent(Graphics g){


        if(mInternalBuffering) {
            long timestamp = System.currentTimeMillis();
            super.paintComponent(g);

            synchronized (mMonitor_A) {
                g.drawImage(mBIM_Active, 0, 0, null);
            }
            requestRecompute();

            //System.out.println("BLOCKED PAINT THREAD FOR: " + (System.currentTimeMillis() - timestamp) + " ms");
        }
        else{
            paintComponentToBuffer(g);
        }
    }

    public void requestRecompute(){
        mRecomputeService.requestRecomputation();
    }


    private TimedServiceExecutor<BufferedImage> mRecomputeService = new TimedServiceExecutor<>(new RecomputeBIM());

    class RecomputeBIM extends TimedServiceExecutor.RecomputeTask<BufferedImage>{
        @Override
        public BufferedImage compute(BufferedImage template) {

            BufferedImage bim = null;
            synchronized(mMonitor_B) {

                // check if BIMs are initialized and right size..
                if(mBIM_Active==null || mBIM_Hidden==null){ initBIMs(getWidth(),getHeight()); }
                else{
                    if( mBIM_Active.getWidth()!=getWidth() || mBIM_Active.getHeight()!=getHeight() || mBIM_Hidden.getWidth()!=getWidth() || mBIM_Hidden.getHeight()!=getHeight() ){
                        initBIMs(getWidth(),getHeight());
                    }
                }

                // we dont care for the template..
                bim = mBIM_Hidden;
                Graphics2D g2 = (Graphics2D) bim.getGraphics();
                // reset to transparent..
                //clear
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
                g2.fillRect(0, 0, 256, 256);
                //reset composite
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
                paintComponentToBuffer(g2);

                // switch
                synchronized(mMonitor_A){
                    BufferedImage bimHiddenStored = mBIM_Hidden;
                    mBIM_Hidden = mBIM_Active;
                    mBIM_Active = bimHiddenStored;
                }
            }


            return bim;
        }
    }

    /**
     * Does the actual rendering of the component.
     *
     * @param g
     */
    public void paintComponentToBuffer(Graphics g) {

        Random r = new Random();
        Graphics2D g2 = (Graphics2D) g;


        g2.setColor(new Color(200, 200, 200, 150));
        g2.fillRect(0, 0, this.getWidth(), this.getHeight());

        g2.setColor(Color.blue);
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawRect(0, 0, this.getWidth(), this.getHeight());



        if(this.mModel==null){return;}
        if(this.mModel.getAllElements().size() > this.mConfMaxElements){
            return;
        }

        //g2.setColor(Color.darkGray);
        //g2.fillRect(-1000,-1000,3000,3000);
        //g2.setColor( new Color(0,0,0,0) );
        //g2.clearRect(0,0,this.getWidth(),this.getHeight());

//        for(int zi=0;zi<50000;zi++){
//            g2.setColor(new Color(r.nextInt(30),r.nextInt(40),200+r.nextInt(40)));
//            g2.fillRect(r.nextInt(600),r.nextInt(600),r.nextInt(80),r.nextInt(80));
//        }


        if(this.mMode.equals(FAST_PANE_MODE.AUTONOMOUS_FROM_REDUCED) || this.mMode.equals(FAST_PANE_MODE.AUTONOMOUS_FROM_FULL )){
            //System.out.println("mkay!");
        }

        double xinterval[] = new double[]{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        double yinterval[] = new double[]{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};

        //List<CardElement> mElements = mModel.getReducedAllElements();

//        switch( mMode ){
//            case SHOWING_REDUCED_CARD_PANE_MODEL:
//            {
//                this.mElements = mModel.getReducedAllElements();
//            }
//            break;
//            case AUTONOMOUS_FROM_REDUCED: {
//
//            }
//            break;
//            case SHOWING_FULL_CARD_PANE_MODEL:{
//                this.mElements = mModel.getAllElements();
//            }
//            break;
//            case AUTONOMOUS_FROM_FULL: {
//
//            }
//            break;
//        }


        FastPaneViewport vp = getViewport(mConfig_RelativePadding , mConfig_AbsolutePadding);
        g2.transform( vp.transform );

        //System.out.println("Num Cards: "+mElements.size());

        List<CardElement> elementsSnapshot = new ArrayList<>( mModel.getAllElements() );  //new ArrayList<>(mElements);
        for(CardElement ce : elementsSnapshot) {

            // number of cards in the card element that we're gonna show
            int nCardsTot = 0;
            int nCardsAfterExclusion = 0;
            boolean drawEmptyCE = false;


            if (mMode.equals(FAST_PANE_MODE.SHOWING_FULL_CARD_PANE_MODEL) || mMode.equals(FAST_PANE_MODE.AUTONOMOUS_FROM_FULL)) {
                nCardsTot = ce.getAllRecords().size();
                nCardsAfterExclusion = ce.getNonexcludedRecords().size();
                if (nCardsAfterExclusion == 0) {
                    drawEmptyCE = true;
                }
            }
            if (mMode.equals(FAST_PANE_MODE.SHOWING_REDUCED_CARD_PANE_MODEL) || mMode.equals(FAST_PANE_MODE.AUTONOMOUS_FROM_REDUCED)) {
                nCardsTot = ce.getNonexcludedRecords().size();
            }


            if (drawEmptyCE) {
                drawCE(g2, mPaintNonVisibleCardFill, mPaintNonVisibleCardFill, ce);
            } else {
                if (nCardsTot > 1) {
                    drawCE(g2, mPaintStackFill.apply(nCardsTot), mPaintStackBorder.apply(nCardsTot), ce);
                }
                if (nCardsTot == 1) {
                    drawCE(g2, mPaintCardFill, mPaintCardBorder, ce);
                }
            }
        }
//            if(nCards==1){
//                    if( ce.getNonexcludedRecords().isEmpty() ){
//                        //g2.setColor(Color.blue.darker());
//                        g2.setPaint(mPaintNonVisibleCardFill);
//                        g2.fillRect((int) ce.getRectangle().getX(), (int) ce.getRectangle().getY(), (int) ce.getRectangle().getWidth(), (int) ce.getRectangle().getHeight());
//
//                        //g2.setColor(Color.darkGray.darker());
//                        g2.setPaint(mPaintNonVisibleCardBorder);
//                        g2.drawRect((int) ce.getRectangle().getX(), (int) ce.getRectangle().getY(), (int) ce.getRectangle().getWidth(), (int) ce.getRectangle().getHeight());
//                    }
//                    else{
//                        //g2.setColor(Color.blue.darker());
//                        g2.setPaint(mPaintCardFill);
//                        g2.fillRect((int) ce.getRectangle().getX(), (int) ce.getRectangle().getY(), (int) ce.getRectangle().getWidth(), (int) ce.getRectangle().getHeight());
//
//                        //g2.setColor(Color.darkGray.darker());
//                        g2.setPaint(mPaintCardBorder);
//                        g2.drawRect((int) ce.getRectangle().getX(), (int) ce.getRectangle().getY(), (int) ce.getRectangle().getWidth(), (int) ce.getRectangle().getHeight());
//                    }
//                }
//                else {
//                    //g2.setColor(Color.blue.darker());
//                    g2.setPaint(mPaintCardFill);
//                    g2.fillRect((int) ce.getRectangle().getX(), (int) ce.getRectangle().getY(), (int) ce.getRectangle().getWidth(), (int) ce.getRectangle().getHeight());
//
//                    //g2.setColor(Color.darkGray.darker());
//                    g2.setPaint(mPaintCardBorder);
//                    g2.drawRect((int) ce.getRectangle().getX(), (int) ce.getRectangle().getY(), (int) ce.getRectangle().getWidth(), (int) ce.getRectangle().getHeight());
//                }
//            }
//            else{
//                if( this.mMode == FAST_PANE_MODE.SHOWING_FULL_CARD_PANE_MODEL ){
//
//                }
//                else {
//                    //g2.setColor(Color.blue.darker().darker());
//                    g2.setPaint(mPaintStackFill.apply(ce.getAllRecords().size()));
//                    g2.fillRect((int) ce.getRectangle().getX(), (int) ce.getRectangle().getY(), (int) ce.getRectangle().getWidth(), (int) ce.getRectangle().getHeight());
//
//                    //g2.setColor(Color.black);
//                    g2.setPaint(mPaintStackBorder.apply(ce.getAllRecords().size()));
//                    g2.drawRect((int) ce.getRectangle().getX(), (int) ce.getRectangle().getY(), (int) ce.getRectangle().getWidth(), (int) ce.getRectangle().getHeight());
//                }
//            }
//        }

        if(this.mCardPaneForViewport!=null){
            g2.setColor(Color.red);
            g2.setStroke( new BasicStroke(1.0f) );
            Rectangle2D rvp = mCardPaneForViewport.getViewport();
            g2.drawRect( (int) rvp.getX(), (int) rvp.getY(), (int) rvp.getWidth(), (int) rvp.getHeight() );
        }

        //System.out.println("FPS= "+ 1000.0/(1.0*System.currentTimeMillis()-1.0*timestamp) );
        timestamp = System.currentTimeMillis();
        //this.repaint();
    }

    public void drawCE( Graphics2D g2, Paint p_fill , Paint p_border , CardElement ce ){
        Rectangle2D r_ce = ce.getRectangle();
        if(p_fill!=null){
            g2.setPaint(p_fill);
            //g2.setStroke( new BasicStroke(1.0f) );
            g2.fillRect( (int) r_ce.getX(), (int) r_ce.getY(), (int) r_ce.getWidth(), (int) r_ce.getHeight() );
        }
        if(p_border!=null){
            g2.setPaint(p_border);
            g2.setStroke( new BasicStroke(1.0f) );
            g2.drawRect( (int) r_ce.getX(), (int) r_ce.getY(), (int) r_ce.getWidth(), (int) r_ce.getHeight() );
        }
    }


    public void setCardPaneModel(CardPaneModel cpm){
        this.mModel = cpm;
        this.repaint();
    }


    static class FastPaneViewport{
        public double xinterval[];
        public double yinterval[];
        public double scaleWidth;
        public double scaleHeight;
        public double scale;
        AffineTransform transform;

        public FastPaneViewport( double xint[], double yint[] , double scaleWidth, double scaleHeight, double scale, AffineTransform tf ){
            this.xinterval = xint;
            this.yinterval = yint;
            this.scaleWidth = scaleWidth;
            this.scaleHeight = scaleHeight;
            this.scale       = scale;
            this.transform   = tf;
        }

    }

    public FastPaneViewport getViewport(double relative_padding, double absolute_padding ){
        double xinterval[] = new double[]{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        double yinterval[] = new double[]{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};


        for (CardElement ce : new ArrayList<>( this.getAllElements() ) ) {
            xinterval[0] = Math.min(xinterval[0], ce.getRectangle().getCenterX());
            xinterval[1] = Math.max(xinterval[1], ce.getRectangle().getCenterX());

            yinterval[0] = Math.min(yinterval[0], ce.getRectangle().getCenterY());
            yinterval[1] = Math.max(yinterval[1], ce.getRectangle().getCenterY());
        }

        // add relative padding:
        double xdi = xinterval[1]-xinterval[0];
        double ydi = yinterval[1]-yinterval[0];
//        xinterval[0] = xinterval[0] - 0.5*relative_padding * xdi; xinterval[1] = xinterval[1] + 0.5*relative_padding * xdi;
//        yinterval[0] = yinterval[0] - 0.5*relative_padding * ydi; yinterval[1] = yinterval[1] + 0.5*relative_padding * ydi;
        xinterval[0] = Math.min( xinterval[0] - 0.5*relative_padding * xdi , xinterval[0] - absolute_padding );
        xinterval[1] = Math.max( xinterval[1] + 0.5*relative_padding * xdi , xinterval[1] + absolute_padding );
        yinterval[0] = Math.min( yinterval[0] - 0.5*relative_padding * ydi , yinterval[0] - absolute_padding );
        yinterval[1] = Math.max( yinterval[1] + 0.5*relative_padding * ydi , yinterval[1] + absolute_padding );

        // left upper corner:
        double x0 = xinterval[0];
        double y0 = yinterval[0];

        // scale: this is the scale from card coordinates to screen coordinates
        double scale = Double.NaN;

        //double aspectRatio = (1.0 * this.getWidth()) / (1.0 * this.getHeight());
        //double planeRatio = (xinterval[1] - xinterval[0]) / (yinterval[1] - yinterval[0]);

        double scaleWidth = this.getWidth() / (xinterval[1] - xinterval[0]);
        double scaleHeight = this.getHeight() / (yinterval[1] - yinterval[0]);

        if (scaleWidth > 1000000) {
            scaleWidth = 1000000;
        }
        if (scaleHeight > 1000000) {
            scaleHeight = 1000000;
        }

        //System.out.println("scale width: "+scaleWidth+"  scale height: "+scaleHeight);

        scale = Math.min(scaleWidth, scaleHeight);

        AffineTransform at = AffineTransform.getScaleInstance(1, 1);
        if(true) {
            //AffineTransform at = AffineTransform.getScaleInstance(1, 1);
            if (scaleWidth < scaleHeight) {
                //at.translate(0,  0.5*((this.getHeight() / scale) - (yinterval[1] - yinterval[0]) ) );
                at.translate(0,  0.5*( this.getHeight() - (yinterval[1] - yinterval[0])*scale ) );

                //at.translate(0,  40 );
            } else {
                //at.translate( 0.5*( (this.getWidth() / scale) - (xinterval[1] - xinterval[0]) ), 0);
                at.translate( 0.5*( (this.getWidth()) - (xinterval[1] - xinterval[0])*scale  ), 0);
            }
            at.scale(scale, scale);
            at.translate(-x0, -y0);
            //g2.transform(at);
        }

        return new FastPaneViewport(xinterval,yinterval,scaleWidth,scaleHeight,scale,at);
    }

    public List<CardElement> getAllElements(){
        return this.mModel.getAllElements();
    }

//    public List<CardElement> getVisibleElements(){
//        return this.mModel.getAllElements();
//    }

    public CardPaneModel<CardElement,?> getCardPaneModel(){
        return this.mModel;
    }


    public double[] getBoundsX(){
        ArrayList<CardElement> allElements = new ArrayList<>(this.getAllElements());
        double min = allElements.stream().mapToDouble( e -> e.getRectangle().getCenterX() ).min().getAsDouble();
        double max = allElements.stream().mapToDouble( e -> e.getRectangle().getCenterX() ).max().getAsDouble();
        return new double[]{min,max};
    }

    public double[] getBoundsY(){
        ArrayList<CardElement> allElements = new ArrayList<>(this.getAllElements());
        double min = allElements.stream().mapToDouble( e -> e.getRectangle().getCenterY() ).min().getAsDouble();
        double max = allElements.stream().mapToDouble( e -> e.getRectangle().getCenterY() ).max().getAsDouble();
        return new double[]{min,max};
    }

    JCardPane mCardPaneForViewport = null;
    /**
     * Show viewport of the given card pane..
     *
     * @param cpViewport
     */
    public void setViewportShowingCardPane(JCardPane cpViewport){
        this.mCardPaneForViewport = cpViewport;
    }



    volatile AnimatorThread mAnimatorThread = null;


    @Override
    public void onCardPaneEvent(CardPaneEvent cpe) {
        this.repaint();
    }


    public void startAnimation(AnimatorInterface animator , List<CardElement> newElements ){
        System.out.println("START ANIMATION!");

        if(mAnimatorThread != null){
            System.out.println("Wait for thread to finish..");
            try {
                this.mAnimatorThread.join();
            }
            catch(Exception e){}
        }

        AnimatorThread at = new AnimatorThread(this , animator , newElements);
        this.mAnimatorThread = at;
        at.start();

    }

    public void setViewportMovement(boolean enable){
        mEnableViewportMovement = enable;
    }

    class ViewportMoverMouseListener implements MouseListener, MouseMotionListener {

        private Point2D mDragViewportStart = null;
        private Point2D mDragViewportLastPoint = null;

        @Override
        public void mouseClicked(MouseEvent e) {

        }

        @Override
        public void mousePressed(MouseEvent e) {
            System.out.println("FCP: mouse clicked");


            //if(mEnableViewportMovement) {
            if(mEnableViewportMovement) {
                FastPaneViewport vp = getViewport(mConfig_RelativePadding,mConfig_AbsolutePadding);
                Point2D xp = null;
                try {
                    xp = vp.transform.inverseTransform(new Point2D.Double(e.getX(), e.getY()), null);
                } catch (Exception exception) {
                }

                System.out.println("x:" + xp.getX() + "y:" + xp.getY());

                Rectangle2D rvp = mCardPaneForViewport.getViewport();
                System.out.println("viewort: " + rvp.toString());

                if (rvp.contains(xp)) {
                    System.out.println("Hit Viewport");
                    mDragViewportStart = xp;
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            mDragViewportStart = null;
            mDragViewportLastPoint = null;
        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {
            mDragViewportStart = null;
            mDragViewportLastPoint = null;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if(mDragViewportStart!=null){

                // move viewport according to dragged difference..
                FastPaneViewport vp = getViewport(mConfig_RelativePadding,mConfig_AbsolutePadding);
                Point2D xp = null;
                try {
                    xp = vp.transform.inverseTransform(new Point2D.Double(e.getX(), e.getY()), null);
                } catch (Exception exception) {
                }

                System.out.println("dragged to: x:" + xp.getX() + "y:" + xp.getY());
                if(mDragViewportLastPoint==null){mDragViewportLastPoint = mDragViewportStart;}

                mCardPaneForViewport.moveViewport( xp.getX() - mDragViewportLastPoint.getX(), xp.getY() - mDragViewportLastPoint.getY() , true );
                mDragViewportLastPoint = xp;
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {

        }
    }


    class AnimatorThread extends Thread{

        JFastCardPane mCardPane;
        private AnimatorInterface mAnimator;
        private List<CardElement> mFinalElements;


        public AnimatorThread(JFastCardPane cardPane, AnimatorInterface animator){
            this(cardPane,animator,null);
        }

        public AnimatorThread(JFastCardPane cardPane, AnimatorInterface animator , List<CardElement> finalElements){
            this.mCardPane = cardPane;
            this.mAnimator = animator;
            this.mFinalElements = finalElements;
        }

        public void run(){
            this.mAnimator.initAnimation();
            long timestamp = System.currentTimeMillis();
            while(mAnimator.animate(  (System.currentTimeMillis() - timestamp)*1.0 / 1000.0 )){
                repaint();//mCardPane.repaint();
                //System.out.println("A");
            }

            if(mFinalElements!=null){
//                mCardPane.getVisibleElements().clear();
//                mCardPane.getVisibleElements().addAll(mFinalElements);
                mCardPane.getCardPaneModel().clearAllCEs();

                for(CardElement ce : mFinalElements) {
                    mCardPane.getCardPaneModel().addCE(ce);
                }
                repaint();
            }

            mAnimatorThread = null;
        }
    }


}
