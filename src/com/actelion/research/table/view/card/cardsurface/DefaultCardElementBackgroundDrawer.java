package com.actelion.research.table.view.card.cardsurface;

import com.actelion.research.table.view.card.JCardPane;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.*;
import java.util.List;


/**
 * This class implements the AbstractCardElementBackgroundDrawer, i.e. mainly the function drawBackground.
 *
 * NOTE: The actual function drawBackground(..) uses a caching mechanism, which provides very fast drawing of buffered
 * images for the backgrounds.
 *
 * The actual methods for drawing the background is:
 *
 * drawBackground_hq(..)
 *
 *
 * NOTE: Explanation of the "randseed": this is used to make stacks look "random". The variable mNumRandStacks controls
 * the number of different stack seeds that we actually consider. After calling drawBackground(..) we compute the provided
 * randseed module mNumRandStacks to limit the number.
 *
 */
public class DefaultCardElementBackgroundDrawer extends AbstractCardElementBackgroundDrawer {


    //private List<Shape> mStackShapes;
    private List<StackCardConfig> mStackedCards;

    private BufferedImage mShadow;

    private int mConfNumRotations = 40;
    private int mConfNumShifts     = 40;

    private double mConf_max_rot = 0.15;
    private double mConf_max_shift = 0.08;


    //private int mConf_BIM_Resolution_x = 40;
    private int mConf_BIM_Resolution_x = 800;


    //private int mConf_BIM_Resolution_y = 160;
    private int mConf_Shadow_Padding = 10;


    private int mNumRandStacks = 8;

    private int mInitializedCardWidth  = -1;
    private int mInitializedCardHeight = -1;


    private int mConfShadowAlpha  = 80;


    /**
     * CardPane, required to notify it when new buffered cards are availabe to draw (i.e. by calling .repaint())
     */
    private JCardPane mCardPane = null;

    // caching mechanism:
    private Map<BackgroundDescriptor,BufferedBackgroundImage> mCachedBackgrounds = new HashMap<>();



    public DefaultCardElementBackgroundDrawer(JCardPane cardPane){
        //initBackgroundDrawer();
        this.mCardPane = cardPane;

    }


    public void initBackgroundDrawer(int w, int h){

        mInitializedCardWidth  = w;
        mInitializedCardHeight = h;

        Random r = new Random(12321);

        RoundRectangle2D rr = new RoundRectangle2D.Double(0,0,w,h,10,10);

        //mStackShapes = new ArrayList<>();
        mStackedCards = new ArrayList<>();

        for(int zi=0;zi<mConfNumRotations;zi++){
            for(int zj=0;zj<mConfNumShifts;zj++) {
                double rot = r.nextDouble() * 2 * mConf_max_rot - mConf_max_rot;

                double shift_x = (0.5 - r.nextDouble()) * mConf_max_shift * w;
                double shift_y = (0.5 - r.nextDouble()) * mConf_max_shift * h;

                AffineTransform rotateAndShift = AffineTransform.getRotateInstance(rot, w / 2, h / 2);
                rotateAndShift.translate(shift_x, shift_y);
                Shape rr_rot = rotateAndShift.createTransformedShape(rr);

                //mStackShapes.add(rr_rot);
                mStackedCards.add(new StackCardConfig(rr_rot,shift_x,shift_y,rot));
            }
        }


        // init shadow:

        int sx = mConf_BIM_Resolution_x;
        int sy = (int)( mConf_BIM_Resolution_x*( (1.0*h)/(1.0*w)));
        mShadow = new BufferedImage( sx+2*mConf_Shadow_Padding , sy+2*mConf_Shadow_Padding , BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = (Graphics2D) mShadow.createGraphics();

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        g2.fillRect(0,0,mShadow.getWidth(),mShadow.getHeight());
        //reset composite
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

        g2.setColor( new Color(0,0,0,80) );
        g2.fillRoundRect( mConf_Shadow_Padding , mConf_Shadow_Padding , sx, sy , 10,10 );

//        Kernel kernel = new Kernel(3, 3, new float[] { 1f / 9f, 1f / 9f, 1f / 9f,
//                1f / 9f, 1f / 9f, 1f / 9f, 1f / 9f, 1f / 9f, 1f / 9f });

        Kernel kernel = new Kernel(6,6,getGaussianKernel(6, 6, 1, 1));

        BufferedImageOp op = new ConvolveOp(kernel);
        mShadow = op.filter(mShadow, null);



    }

    /**
     * Provides a caching mechanism for stack background cards.
     *
     *
     * // @todo: take care for max number of stack cards etc..!
     *
     * @param g
     * @param cw
     * @param ch
     * @param nCards
     * @param shadow
     * @param randSeed
     */
    @Override
    public void drawBackground(Graphics g, int cw, int ch, int nCards, boolean shadow, int randSeed){

        if(nCards<=1 && !shadow){
            return;
        }

        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);

        randSeed = randSeed % mNumRandStacks;


        // check for cached image:
        BackgroundDescriptor descr = new BackgroundDescriptor(nCards,shadow,randSeed);

        if(mCachedBackgrounds.containsKey(descr)){
            BufferedBackgroundImage bbim = mCachedBackgrounds.get(descr);
            // draw the background:
            int bgw = (int) ( cw + bbim.getPaddingXL()+bbim.getPaddingXR() );
            int bgh = (int) ( ch + bbim.getPaddingYB()+bbim.getPaddingYT() );

            g.drawImage(bbim.getBIM(),(int) -bbim.getPaddingXL(),(int) -bbim.getPaddingYT(),bgw,bgh,null);

//            AffineTransform at_old = g2.getTransform();
//            g2.scale(1.0/mConf_ResolutionBackground,1.0/mConf_ResolutionBackground);
//            g2.translate(-bbim.getAnchorPointX(),-bbim.getAnchorPointX());
//
//
//            g2.drawImage( bbim.mBIM , (int)( -bbim.getPaddingXL()/mConf_ResolutionBackground ) , (int) ( -bbim.getPaddingYT()/mConf_ResolutionBackground ) , null );
//            g2.setTransform(at_old);
        }
        else{
            // @TODO: potentially put into thread.. (maybe that's not even needed, this seems to work pretty quick..)
            BufferedBackgroundImage bbi_new = createBufferedBackgroundImage(g,cw,ch,nCards,shadow,randSeed);
            this.mCachedBackgrounds.put(descr, bbi_new);
            // notify the card pane about the newly available image..
            this.mCardPane.repaint();
        }


    }


    /**
     * Is the number of pixels per "CardPaneUnit"
     */
    private double mConf_ResolutionBackground = 2.0;

    public BufferedBackgroundImage createBufferedBackgroundImage(Graphics g, int cw, int ch, int nCards, boolean shadow, int randSeed){
        // @TODO: precompute the image size that we need.. (that might help quite a bit perf./memory-wise)

        // for now: just take a guess for the padding that we need..
        double relPaddingXL = 0.25; // left
        double relPaddingXR = 0.25; // right
        double relPaddingYB = 0.25; // bottom
        double relPaddingYT = 0.25; // top


        int pxPaddingXL     = (int) (cw * relPaddingXL);
        int pxPaddingXR     = (int) (cw * relPaddingXR);
        int pxPaddingYB     = (int) (cw * relPaddingYB);
        int pxPaddingYT     = (int) (cw * relPaddingYT);

        int resX = (int) ( mConf_ResolutionBackground *  (cw + pxPaddingXL + pxPaddingXR) );
        int resY = (int) ( mConf_ResolutionBackground *  (ch + pxPaddingYB + pxPaddingYT) );

        BufferedImage bim = new BufferedImage( resX , resY , BufferedImage.TYPE_INT_ARGB);

        Graphics2D bg2 = bim.createGraphics();

        bg2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        bg2.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);

        // correct order here! First scale, translations are relative to before scaling
        bg2.scale(mConf_ResolutionBackground,mConf_ResolutionBackground);
        bg2.translate( (pxPaddingXL),(pxPaddingYT));

        drawBackground_hq(bg2,cw,ch,nCards,shadow,randSeed);
        bg2.dispose();

        return new BufferedBackgroundImage(bim,pxPaddingXL,pxPaddingYT,pxPaddingYB, pxPaddingYT);

    }

    public void drawBackground_hq(Graphics g, int cw, int ch, int nCards, boolean shadow, int randSeed) {

        Graphics2D g2 = (Graphics2D) g;

        randSeed = randSeed % mNumRandStacks;

        Random r = new Random(randSeed);

        // check if reinit is required:
        if( cw != mInitializedCardWidth || ch != mInitializedCardHeight ){
            initBackgroundDrawer(cw,ch);
        }

        g.setColor(Color.red);


        if(shadow && ! (nCards>1) ){
            // draw shadow:
            drawCardShadow(g,cw,ch);
        }

        if(nCards > 1){
            if(shadow) {
                drawStackShadow(g, cw, ch, nCards - 1, randSeed);
            }
            drawStackedCards(g,cw,ch,nCards-1,randSeed);
        }

        // draw the normal card background:

        if(false) {
            GradientPaint gradient = new GradientPaint(0, 0, Color.white.darker(), cw / 2, ch, Color.lightGray.brighter());
            g2.setPaint(gradient);
            g2.fillRoundRect(0, 0, cw, ch, 10, 10);
            g2.setColor(Color.black);
        }
    }


    private int mConf_ShadowDistX = 10;
    private int mConf_ShadowDistY = 10;

    private int mConf_ShadowStackDistX = 20;
    private int mConf_ShadowStackDistY = 20;


    public void drawCardShadow(Graphics g, int cw, int ch){

        double ratio_cw_to_bim = ((1.0*cw)/mConf_BIM_Resolution_x);

        // compute padding in card coordinates:   (cw=mConf_BIM_resolution_x  )
        int padding_x = (int) ( ratio_cw_to_bim * mConf_Shadow_Padding );
        int padding_y = (int) ( ratio_cw_to_bim * mConf_Shadow_Padding ); // ! Ratio is the same, therefore we can
        g.drawImage( mShadow , mConf_ShadowDistX + (-padding_x), mConf_ShadowDistY + (-padding_y) , cw + 2*padding_x , ch + 2*padding_y , null );
    }

    public void drawStackShadow(Graphics g, int cw, int ch, int nStacked, int randSeed){

        Random r = new Random(randSeed);

        Graphics2D g2x = (Graphics2D) g;

        int numStackCards = nStacked; //Math.min( rec.size() , CONF_MAX_STACK_CARDS_DRAWN );

        Area combinedShape = new Area();

        for(int zi=0;zi<numStackCards;zi++) {
            combinedShape.add( new Area(mStackedCards.get( Math.abs(r.nextInt()) % mStackedCards.size() ).getShape()) );
        }
        //combinedShape.transform( AffineTransform.getTranslateInstance(12,12) );

        double csw = combinedShape.getBounds2D().getWidth();
        double csh = combinedShape.getBounds2D().getHeight();

        // shift:
        double cspx = combinedShape.getBounds2D().getMinX();
        double cspy = combinedShape.getBounds2D().getMinY();

        // init shadow:

        int sx = mConf_BIM_Resolution_x;
        int sy = (int)( mConf_BIM_Resolution_x*( csh / csw ));
        BufferedImage stackShadow = new BufferedImage( sx+2*mConf_Shadow_Padding , sy+2*mConf_Shadow_Padding , BufferedImage.TYPE_INT_ARGB );

        // compute padding in card coordinates:   (cw=mConf_BIM_resolution_x  )
        int padding_x = (int) ( ( 1.0*mConf_Shadow_Padding / (stackShadow.getWidth()  - 2 * mConf_Shadow_Padding ) ) * csw ) ;
        int padding_y = (int) ( ( 1.0*mConf_Shadow_Padding / (stackShadow.getHeight() - 2 * mConf_Shadow_Padding ) ) * csh ) ;


        Graphics2D g2 = (Graphics2D) stackShadow.createGraphics();

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        g2.fillRect(0,0,stackShadow.getWidth(),stackShadow.getHeight());
        //reset composite
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));


        double ratioResolution_x = sx / csw ;  //(sx+2*mConf_Shadow_Padding) / csw;
        double ratioResolution_y = sy / csh ;  //(sy+2*mConf_Shadow_Padding) / csh;

        g2.transform( AffineTransform.getScaleInstance( ratioResolution_x , ratioResolution_y ) );
        g2.transform( AffineTransform.getTranslateInstance( -cspx +padding_x , -cspy + padding_y ) );



        g2.setColor( new Color(0,0,0,80) );
        //g2.fillRoundRect( mConf_Shadow_Padding , mConf_Shadow_Padding , sx, sy , 10,10 );
        g2.fill(combinedShape);

        Kernel kernel = new Kernel(6,6,getGaussianKernel(6, 6, 1, 1));

        BufferedImageOp op = new ConvolveOp(kernel);
        stackShadow = op.filter(stackShadow,null);

        // draw the created image:

        double ratio_csw_to_bim = ((1.0*csw)/sx);
        double ratio_csh_to_bim = ((1.0*csh)/sy);

        double cspx_in_cardspace = cspx * ( stackShadow.getWidth() /  ( csw + 2*padding_x ) );
        double cspy_in_cardspace = cspx * ( stackShadow.getHeight() /  ( csh + 2*padding_y ) );

        //g2x.drawImage( stackShadow , mConf_ShadowDistX + (-padding_x), mConf_ShadowDistY + (-padding_y) , (int) (csw + 2*padding_x) , (int) (csh + 2*padding_y) , null );
        g2x.drawImage( stackShadow , mConf_ShadowDistX + (-padding_x) - (int) cspx_in_cardspace , mConf_ShadowDistY + (-padding_y) - (int) cspy_in_cardspace , (int) (csw + 2*padding_x) , (int) (csh + 2*padding_y) , null );
    }

    public void drawStackShadow_old(Graphics g, int cw, int ch, int nStacked, int randSeed){

        double ratio_cw_to_bim = ((1.0*cw)/mConf_BIM_Resolution_x);

        Random r = new Random(randSeed);

        Graphics2D g2 = (Graphics2D) g;

        int numStackCards = nStacked; //Math.min( rec.size() , CONF_MAX_STACK_CARDS_DRAWN );

        AffineTransform baseTransform = g2.getTransform();

        AffineTransform shiftedBaseTransform = (AffineTransform) baseTransform.clone();
        shiftedBaseTransform.translate(20,20);

        for(int zi=0;zi<numStackCards;zi++){
            // this try/catch is just becuase of the weird "IndexOutOfBoundsException" with index smaller than size that occurs occasionally. (I dont understand how/why..)
            try {
                StackCardConfig sc = mStackedCards.get( Math.abs(r.nextInt()) % mStackedCards.size() );

                g2.setTransform(shiftedBaseTransform);
                AffineTransform rotateAndShift = AffineTransform.getRotateInstance(sc.rot(), cw / 2, ch / 2);
                rotateAndShift.translate( sc.px, sc.py);

                g2.transform(rotateAndShift);

                // compute padding in card coordinates:   (cw=mConf_BIM_resolution_x  )
                int padding_x = (int) ( ratio_cw_to_bim * mConf_Shadow_Padding );
                int padding_y = (int) ( ratio_cw_to_bim * mConf_Shadow_Padding ); // ! Ratio is the same, therefore we can
                g.drawImage( mShadow , mConf_ShadowDistX + (-padding_x), mConf_ShadowDistY + (-padding_y) , cw + 2*padding_x , ch + 2*padding_y , null );
            }
            catch(Exception e){
                System.out.println("Exception while drawing stack background..");
            }
        }
        g2.setTransform(baseTransform);

    }

    //private int CONF_MAX_STACK_CARDS_DRAWN = 1000;


    /**
     *
     * @param g
     * @param cw
     * @param ch
     * @param nStacked Number of stack cards to draw.
     * @param randSeed
     */
    public void drawStackedCards(Graphics g, int cw, int ch, int nStacked, int randSeed){

        Random r = new Random(randSeed);

        Graphics2D g2 = (Graphics2D) g;
        int w = cw;
        int h = ch;

        int numStackCards = nStacked; //Math.min( rec.size() , CONF_MAX_STACK_CARDS_DRAWN );

        for(int zi=0;zi<numStackCards;zi++){
            // this try/catch is just becuase of the weird "IndexOutOfBoundsException" with index smaller than size that occurs occasionally. (I dont understand how/why..)
            try {
                //Shape stackedCard = mStackShapes.get( Math.abs(r.nextInt()) % mStackShapes.size() );
                Shape stackedCard = mStackedCards.get( Math.abs(r.nextInt()) % mStackedCards.size() ).getShape();
                int cardColor = (int) (40 * ((1.0 * zi) / (numStackCards - 1)));
                g2.setColor(new Color(cardColor + 120, cardColor + 120, cardColor + 120));
                g2.fill(stackedCard);
                g2.setColor(Color.black);
                g2.draw(stackedCard);
            }
            catch(Exception e){
                System.out.println("Exception while drawing stack background..");
            }
        }


        GradientPaint gradient = new GradientPaint(0, 0, Color.white.darker(), w/2, h, Color.lightGray.brighter());
        g2.setPaint(gradient);
        g2.fillRoundRect(0,0,w,h,10,10);
        g2.setColor(Color.black);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(0,0,w,h,10,10);
    }




    float[] getGaussianKernel(int rows, int cols, double sigmax, double sigmay)
    {
        double y_mid = (rows-1) / 2.0;
        double x_mid = (cols-1) / 2.0;

        double x_spread = 1. / (sigmax*sigmax*2);
        double y_spread = 1. / (sigmay*sigmay*2);

        double denominator = 8 * Math.atan(1) * sigmax * sigmay;

        double gauss_x[] = new double[rows];
        double gauss_y[] = new double[cols];

        for (int i = 0; i < cols; ++i) {
            double x = i - x_mid;
            gauss_x[i] = Math.exp(-x * x * x_spread);
        }

        for (int i = 0; i < rows; ++i) {
            double y = i - y_mid;
            gauss_y[i] = Math.exp(-y * y * y_spread);
        }

        float kernel[] = new float[rows*cols];

        for (int j = 0; j < rows; ++j) {
            for (int i = 0; i < cols; ++i) {
                kernel[ i + j*rows ] = (float) ( gauss_x[i] * gauss_y[j] / denominator );
            }
        }

        return kernel;
    }
}


class StackCardConfig{

    Shape  mShape;
    double px, py;
    double rot;
    public StackCardConfig(Shape s, double px, double py, double rot){
        mShape = s;
        this.px=px;
        this.py=py;
        this.rot=rot;
    }

    public Shape getShape() {
        return mShape;
    }

    public double px() {
        return px;
    }

    public double py() {
        return py;
    }

    public double rot() {
        return rot;
    }

}

/**
 * Describes the buffered background images
 */
class BackgroundDescriptor {

    int mNumCards;
    boolean mShadow;

    int mRandSeed;

    public BackgroundDescriptor(int nCards, boolean shadow, int randSeed){
        mNumCards = nCards;
        mShadow = shadow;
        mRandSeed = randSeed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BackgroundDescriptor that = (BackgroundDescriptor) o;
        return mNumCards == that.mNumCards &&
                mShadow == that.mShadow &&
                mRandSeed == that.mRandSeed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mNumCards, mShadow, mRandSeed);
    }
}


