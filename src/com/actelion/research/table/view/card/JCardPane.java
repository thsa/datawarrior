package com.actelion.research.table.view.card;

import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.view.card.animators.AnimatorInterface;
import com.actelion.research.table.view.card.animators.PathAnimator;
import com.actelion.research.table.view.card.positioning.CardPositionerInterface;
import com.actelion.research.table.view.card.positioning.GridPositioner;
import com.actelion.research.table.view.card.positioning.RandomCardPositioner;
import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.model.*;
import com.actelion.research.table.view.*;
import com.actelion.research.table.view.card.cardsurface.*;
import com.actelion.research.table.view.card.tools.DataWarriorLink;
import com.actelion.research.table.view.card.tools.IntOrderedPair;


import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceMotionListener;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Function;
import java.util.stream.Collectors;




/**
 * Returns higher z value as higher.
 */
class CardElementZComparator implements Comparator<CardElement>{
    @Override
    public int compare(CardElement o1, CardElement o2) {
        return Integer.compare(o1.getZ(), o2.getZ() );
    }
}


/**
 * Some documentation:
 *
 * The mCardDrawer and mStackDrawer are responsible for drawing card / stack SURFACEs (i.e. the non-transparent part).
 * There exists a simple buffering for these card / stack surfaces, provided by the CachedCardProvider class:
 *   at every paintComponent(..) call, the following steps are performed:
 *     1. the sets of compound records of every CardElement are computed.
 *     2. the CachedCardProvider is informed about the precise set of CardElements (including the non-hidden compound records) for each
 *           --> the CachedCardProvider ensures that the requested cards will be available eventually (it computed BufferedImages in separate threads)
 *           --> based on the number of CardElements to draw, it decides on the optimal resolution for
 *               buffered images (this can be tuned via the "CardView Options -> Render -> BufferedImageQuality" option )
 *               if the stored images are much too high / low res, the it recomputes them.$
 *
 * The mCardElementBackgroundDrawer is responsible for drawing all card backgrounds. It already provides a buffering mechanism
 * for the background images.
 *
 * NOTE! Background images (i)  require transparency (ALWAYS, also just stacks req. transparency..),
 *                         (ii) are quite large (larger than the card surfaces..)
 *                  --> therefore drawing backgrounds is expensive (and stack backgrounds are equally expensive as
 *                      shadows, as both require transparency)
 *
 * The graphic modes:
 * We distinguish between 4 different drawing modes:
 *
 * RENDER_QUALITY."FULL"
 *     --> draws card/stack surfaces (no buffering)
 *     --> draws the normal backgrounds (they are buffered anyway (-> shadows..))
 *
 * RENDER_QUALITY."NORMAL"
 *     --> draws buffered card/stack surfaces. The resolution is governed by the "CardView Options -> "Render" -> BufferedImageQuality" option
 *     --> draw the normal backgrounds (as in "FULL", they are buffered anyway)
 *
 * RENDER_QUALITY."FAST"
 *     --> draws buffered card/stack surfaces as in "NORMAL"
 *     --> does not draw backgrounds
 *
 * RENDER_QUALITY."SUPERFAST"
 *     --> draws just rectangles for the cards / stacks. This is not available as an option currently, as it is not really much faster than "FAST"
 *
 *
 * The graphics mode is chosen based on the thresholds defined by:
 * mThreshCards_NORMAL / _FAST / SUPERFAST. These values define the number of cards (i.e. the lb) where we switch to the
 * the given mode.
 *
 *
 *
 * Explanation of Drag and Drop:
 *
 * The JCardPane can handle CardPaneModel.ListOfListOfCompoundRecords objects.
 * If a dragged card element leaves the component, a Transferable of this type is created.
 * If a Transferable which supoports the CardPaneModel.ListOfListOfCompoundRecords flavor enters the JCardPane, then the
 * card pane will accept the drop.
 *
 * On an accepted drop the following will happen:
 * (1) new card elements according to the data are created, added to the CardPaneModel, positioned close to where the
 *     drop took place. In case of multiple cards involved, and if we allow for proposing dnd stacks, then the multiple
 *     cards will be dropped as a single stack.
 * (2) A CardPaneDragAndDrop event will be fired, the .dropInto event will be called
 *
 * On an export done event the following will happen:
 * (1) the corresponding card elements will be removed from the CardPaneModel
 * (2) A CardPaneDragAndDrop event will be fired, the .dropOutOf event will be called
 *
 * NOTE! on DnD and proposing stacks: The mechanism used here is completely independent of the regular stack proposing
 *                                    mechanism! The code handling of this is all implemented in the DragAndDrop
 *                                    listener in the MyTransferHandler!
 *
 *
 *
 */
public class JCardPane extends JPanel implements  CompoundTableListener , MouseListener, MouseMotionListener, MouseWheelListener , KeyListener, CardDrawingConfig.CardDrawingConfigChangeListener {

    private static final double ZOOM_STEP_RELATIVE = 0.945; //0.965; //1.025;


    /**
     * Modes which describe whether dragging of the viewport via mouse is possible
     */
    private static final int MOUSE_DRAGGING_FUNCTIONALITY_DISABLED         = 0;
    private static final int MOUSE_DRAGGING_FUNCTIONALITY_NORMAL           = 1;
    private static final int MOUSE_DRAGGING_FUNCTIONALITY_ONLY_HORIZONTAL  = 2;
    private static final int MOUSE_DRAGGING_FUNCTIONALITY_ONLY_VERTICAL    = 3;


    private static final int MOUSE_MODE_NONE               = 0;
    private static final int MOUSE_MODE_DRAG_VIEWPORT      = 1;
    private static final int MOUSE_MODE_DRAG_CARDELEMENT   = 2;
    private static final int MOUSE_MODE_DRAG_SELECTION     = 3; // different, does not merge stacks
    private static final int MOUSE_MODE_SELECTION_LASSO    = 4;
    private static final int MOUSE_MODE_SELECTION_RECT     = 5;


    private double STACKING_REL_OVERLAP_REQ = 0.75;

    // drag and drop setttings
    private static final int DND_BEHAVIOR_SIMPLE_DROP_AS_STACK      = 1;
    private static final int DND_BEHAVIOR_SIMPLE_DROP_RANDOM_SINGLE = 2;
    private static final int DND_BEHAVIOR_PROPOSE_STACKS = 3;

    private int m_DD_Behavior = DND_BEHAVIOR_SIMPLE_DROP_AS_STACK;


    /**
     * Options: DND_BEHAVIOR_x
     *
     * @param dnd
     */
    public void setDragAndDropBehavior(int dnd) {
        m_DD_Behavior = dnd;
    }

    public int getDragAndDropBehavior() { return m_DD_Behavior; }


    // Variables for viewport / zooming / moving the view
    private double mViewportCenterX;
    private double mViewportCenterY;
    private double mViewportZoomFactor;


    // Decides whether dragging via mouse is possible
    private int mDraggingFunctionality = 1;
    private boolean mZoomEnabled       = true;


    // Variables for dragging with the mouse
    // Dragging can shift the view or CardElements
    private int  mDragMode = 0;

    private double mDragStartX; // in untransformed coordinates on the component!
    private double mDragStartY;


    private Point2D mDragStartTransformed;

    private Point2D mLastClickedPointTransformed;

    // we need the viewport center to move the viewport according to the drag
    private double mDragStartViewportCenterX;
    private double mDragStartViewportCenterY;

    // popup thread for delayed popup after right mouse click
    Thread mPopupThread = null;

    // time until popup menu
    public int RIGHT_MOUSE_POPUP_DELAY = 400;


    // highlighted / dragged cards:
    private CardElement mHighlightedCardElement = null;
    private CardElement mDraggedCardElement = null;
    private double mDraggedCardStartX;
    private double mDraggedCardStartY;

    // for draggin selection
    private List<Point2D> mDraggedSelectionStartCoordinates = new ArrayList<>();


    // selection:
    //private List<CardElement> mSelection = null;


    private int mTopZIndex = 1;


    // contains the stack onto which we provisionally put the cardelement while dragging.
    private CardElement mProposedStackElement = null;


    // limit zooming out too far:
    // if height / width hits MaxZoomOutFactor* height/width of the bounding rectangle we stop zooming out further.
    private double mMaxZoomOutFactor = 4.0;



    // contains the proposed snap area if one is active:
    private SnapArea mProposedSnapArea = null;

    // drawers
    AbstractCardDrawer mCardDrawer = null;//new DefaultCardDrawer();
    AbstractStackDrawer mStackDrawer = null; //new DefaultStackDrawer();

    AbstractCardElementBackgroundDrawer mCardElementBackgroundDrawer = null;


    // buffered image provides
    CachedCardProvider mCachedCardProvider = null;


    /**
     *
     * We use the following sets of card elements:
     *
     * mElements : this is the set of card elements which represents the state of all records which are in the CTM
     *
     * mNonExcludedElements : this is the set of card elements which results from removing all "excluded" records from mElements
     *
     * The drawing system draws the following cards:
     *
     * -> mNonExcludedElements   (this is computed in a separate thread)
     *
     * -> mExtraElements    (these elements are save from removal by "excluding records")
     *
     * CardElements:
     *
     *
     */

//    List<CardElement> mElements;
//
//
//    // Non-invisible card elements
//    // Will be recomputed after capturing corresponding CompoundTableModel changed events
//    List<CardElement> mNonExcludedElements = new ArrayList<>();


    /**
     * The Card Pane Model
     */
    CardPaneModel<CardElement,CardElement.CardElementFactory> mCardPaneModel = null;


    // Table model
    CompoundTableModel mTableModel = null;

    CompoundListSelectionModel mListSelectionModel = null;


    // Selected CardElements:
    // == null if there is no list selection
    List<CardElement> mSelectedElements = new ArrayList<>();


    // the view
    JCardView mCardView = null;

    // the color handler
    CompoundTableColorHandler mColorHandler = null;

    // THIS contains all links to data warrior ()
    DataWarriorLink mDWL = null;


    // HighlightListener
    MyHighlightListener mHighlightListener = new MyHighlightListener();

    // Lasso
    private Polygon	mLassoRegion;

    // rectangular selection:
    private Point2D mRectangularSelectionAnchor = null;
    private Point2D mRectangularSelectionP2     = null;


    // Render Quality / Buffered Images Setup
    //public static final int RENDER_QUALITY_FULL = 10;
    //public static final int RENDER_QUALITY_LOW  = 5;

    private final Color COLOR_MOUSE_OVER     = new Color(0,0  ,255);
    private final Color COLOR_SELECTION      = VisualizationColor.cSelectedColor;
    private final Color COLOR_ACTIVE         = new Color(255,0,0);

    private Color mConf_Color_Selected      = COLOR_SELECTION; //Color.blue;
    private Color mConf_Color_MouseOver     = COLOR_MOUSE_OVER;//Color.red.brighter(); //Color.red;
    private Color mConf_Color_MouseClicked  = COLOR_ACTIVE;//Color.magenta.darker();





    public static enum RenderQuality { FULL, NORMAL , FAST , SUPERFAST};

    private RenderQuality mRenderQuality;


    // NOTE: if render quality = 0, we just draw rectangles..
    //public static final int RENDER_QUALITY_NUM_PIXEL[] = new int[]{ 0 , 10000 , 20000 , 40000 , 80000 , 160000 , 320000 , 640000 , 1280000 , 2560000 };

    //private int mRenderQuality = RENDER_QUALITY_LOW;

    //private boolean mRenderQuality_DrawShadows = true;
    //private ConcurrentHashMap<CardElement,BufferedImage> mBufferedImages = new ConcurrentHashMap<>();


    // HARD THRESHOLDS FOR SWITCHING TO LOWER RENDERING QUALITY..

    /**
     * If the number of card surfaces to draw exceeds this number, we switch to just drawing rectangles..
     */
    //private int mThreshCards_SUPERFAST = 2000;
    private int mThreshCards_SUPERFAST     = 4000;
    private int mThreshCards_FAST          = 600;
    private int mThreshCards_NORMAL        = 100;


    private boolean mSwitchToHiQuality = true;

    /**
     * If true, then in all drawing modes shadows are omitted.
     */
    private boolean mDisableShadows        = true;

    /**
     * If the number of card surfaces to draw exceeds this number, we switch to just drawing the buffered images..
     */
    //private int mThreshCards_HQ      = 200;
    //private int mThreshCards_HQ        = 60;//20;


    private int logLevel = 0;



    /**
     * At this number of cards to draw we switch to full quality drawing
     * @return
     */
    public int getNumCards_HQ(){
        return this.mThreshCards_NORMAL;
    }

    public void setNumCards_HQ(int numCards){
        this.mThreshCards_NORMAL = numCards;
    }

    /**
     * At this number of cards to draw we switch to fastest possible drawing
     * @return
     */
    public int getNumCards_SUPERFAST(){
        return this.mThreshCards_SUPERFAST;
    }

    public void setNumCards_SUPERFAST(int numCards){
        this.mThreshCards_SUPERFAST = numCards;
    }

    public boolean isSwitchToHiQuality() {
        return this.mSwitchToHiQuality;
    }

    public void setSwitchToHiQuality(boolean enabled) {
        this.mSwitchToHiQuality = enabled;
    }



    /**
     * Set number of threads for buffered image recomputation.
     * @param nThreads
     */
    public void setCardCacheThreadCount(int nThreads) {
        this.mCachedCardProvider.setThreadCount(nThreads);
    }

    /**
     *Returns number of threads for buffered image recomputation.
     * @return
     */
    public int getCardCacheThreadCount() {
        return this.mCachedCardProvider.getThreadCount();
    }

    /**
     * True means, shadows are disabled.
     *
     * @return
     */
    public boolean isDisableShadowsEnabled(){
        return mDisableShadows;
    }

    /**
     * If enable=true, then shadows will be disabled.
     *
     * @param enable
     */
    public void setDisableShadowsEnabled(boolean enable){
        this.mDisableShadows = enable;
    }


    /**
     * CardEvent Listener..
     */
    private List<CardPaneEventListener> mCardPaneEventListeners = new ArrayList<>();


    // Computation of hidden Cards service:
    //TimedBufferedServiceExecutor<CardElement> mHiddenCardComputeService      = new TimedBufferedServiceExecutor<>(new HiddenCardsComputeService(this));

    // Computation of buffered images service:
    // TimedBufferedServiceExecutor<CardElement> mCreateBufferedImagesService   = new TimedBufferedServiceExecutor<>(new BufferedImageCreatorService(this));


    //TimedServiceExecutor<List<CardElement>> mNonInvisibleCardsService        = new TimedServiceExecutor( new NonInvisibleCardsRecomputation() );


    /**
     * Provide 2d binning, but recompute it only every 0.1
     */
    public TimedServiceExecutor<CardPaneModel.BinnedCards> mBinnedCards_All;
    public TimedServiceExecutor<CardPaneModel.BinnedCards> mBinnedCards_Nonexcluded;


    private CardPositionerInterface mCardPositioner = new RandomCardPositioner(this);

    public void setDefaultCardPositioner(CardPositionerInterface positioner){
        this.mCardPositioner = positioner;
    }


    // internal settings
    //private boolean flagGraphics_drawStackBackgroundInLowRes = true;


    // debug stuff
    private boolean flagDebug_drawCardElementRectangles = false;
    private boolean flagDebug_showDebugOutputOnScreen   = true;//true;
    private Font    debug_OutputFont = new Font("Verdana", Font.BOLD, 12);

    private boolean flagDebug_outputMouseListeners = false;


    private long    debug_TimestampLastDebugOutput = 0;
    private double  debug_TimeGraphicsDebugOutput  = 4.0;




    //public JCardPane(JCardView view, CompoundTableModel tableModel, CompoundListSelectionModel listSelectionModel , CompoundTableColorHandler colorHandler){
    public JCardPane(JCardView view, DataWarriorLink dwl) {

        this.mDWL = dwl;

        this.mCardView = view;
        //this.mElements     = new ArrayList<>();//new CardElementZComparator());
        this.mTableModel   = dwl.getCTM();
        this.mListSelectionModel = dwl.getListSelectionModel();
        this.mColorHandler = dwl.getTableColorHandler();

        //this.mCardDrawer = new DefaultCardDrawer(tableModel,colorHandler);

        this.setViewport(0,0,1.0);

        this.mTableModel.addCompoundTableListener(this );


        // construct pane model:
        this.mCardPaneModel = new CardPaneModel( this.mTableModel , this.mListSelectionModel , new CardElement.CardElementFactory( this ) );
        this.registerCardPaneEventListener( mCardPaneModel );

        this.addMouseListener(this);
        this.addMouseWheelListener(this);
        this.addMouseMotionListener(this);

        this.addKeyListener(this);
        this.setFocusable(true);


        this.setCardDrawer( new DefaultCardDrawer( dwl.getCTM(),dwl.getTableColorHandler())  );
        this.setStackDrawer( new DefaultStackDrawer( dwl.getCTM(),dwl.getTableColorHandler() ) );
        this.mCardElementBackgroundDrawer = new DefaultCardElementBackgroundDrawer(this);

        this.getCardDrawer().getCardDrawingConfig().registerConfigChangeListener(this);
        this.getCardDrawer().getCardDrawingConfig().fireConfigChangeEvent();


        // deactivate row hiding by default (?) (advantage: smoother zooming..)
        this.getCardPaneModel().setRowHiding(false);

//        recomputeExcludedCardElements();
        //this.mHiddenCardComputeService.start();
//        this.mCreateBufferedImagesService.start();

        mCachedCardProvider = new CachedCardProvider(this, 2000000, 100000000, 2);

        // configure binned cards providers:
        mBinnedCards_All = new TimedServiceExecutor<>(new BinnedCardsService_All());
        mBinnedCards_All.setTimeIntervalMilliseconds(50);
        mBinnedCards_Nonexcluded = new TimedServiceExecutor<>(new BinnedCardsService_Nonexcluded());
        mBinnedCards_Nonexcluded.setTimeIntervalMilliseconds(50);


        mTableModel.addHighlightListener( mHighlightListener );


        this.setViewport( 800,800,0.5 );

        // init the viewport zoom listener:
        this.addComponentListener(mResizeAndZoomListener);


        // Init dnd
        this.setTransferHandler( new MyTransferHandler() );
        this.getDropTarget().setDefaultActions(DnDConstants.ACTION_MOVE);
        this.getDropTarget().setActive(true);
        this.setDragAndDropBehavior(DND_BEHAVIOR_PROPOSE_STACKS);

    }



    public void zoom(int clicks){
        setViewport(getViewport().getCenterX(), getViewport().getCenterY(), this.getZoomFactor() * Math.pow( ZOOM_STEP_RELATIVE , clicks ) );
        //fireCardPaneEvent( new CardPaneEvent( CardPaneEvent.EventType.VISIBLE_ELEMENTS_CHANGED , this.getViewport() ) );
    }

    public void zoomTowards(int clicks, double px, double py){
        Rectangle2D vp = this.getViewport();
        double relPosX =  ( px-vp.getMinX() ) / ( vp.getMaxX()-vp.getMinX() ) ;
        double relPosY =  ( py-vp.getMinY() ) / ( vp.getMaxY()-vp.getMinY() ) ;

        double zoomfactor = Math.pow( ZOOM_STEP_RELATIVE , clicks );

        double wNewVP  = this.getViewport().getWidth() * zoomfactor;
        double hNewVP  = this.getViewport().getHeight() * zoomfactor;

        // now compute the where the transformed point goes if we zoom towards the center. The distance is the shift that we have to do
        double xNewPosX =  vp.getCenterX() + ( px-vp.getCenterX() ) * zoomfactor;
        double yNewPosY =  vp.getCenterY() + ( py-vp.getCenterY() ) * zoomfactor;

        setViewport(getViewport().getCenterX() - (px-xNewPosX), getViewport().getCenterY() - (py-yNewPosY) , this.getZoomFactor() * zoomfactor );
    }


    public void moveViewport(double dx, double dy){
        moveViewport(dx,dy,false);
    }

    /**
     *
     * @param dx
     * @param dy
     * @param obeyDraggingFunctionality whether MOUSE_DRAGGING_FUNCTIONALITY is considered
     */
    public void moveViewport(double dx, double dy, boolean obeyDraggingFunctionality){

        if(obeyDraggingFunctionality){
            if (this.mDraggingFunctionality == MOUSE_DRAGGING_FUNCTIONALITY_NORMAL ) {

            }
            if (this.mDraggingFunctionality == MOUSE_DRAGGING_FUNCTIONALITY_ONLY_HORIZONTAL ){
                dy = 0;
            }
            if(this.mDraggingFunctionality == MOUSE_DRAGGING_FUNCTIONALITY_ONLY_VERTICAL ){
                dx = 0;
            }
        }

        this.setViewport(getViewportCenterX()+dx,getViewportCenterY()+dy,this.getZoomFactor());
        //fireCardPaneEvent( new CardPaneEvent( CardPaneEvent.EventType.VISIBLE_ELEMENTS_CHANGED , this.getViewport() ) );
    }

    /**
     * This is a function to change the zoom / position of the view.
     *
     * Here would be the place to implement improved double-buffering / only redrawing of dirty regions functionality.
     *
     * NOTE! A call to this function will stop potential animateViewport threads and set the viewport to the
     * supplied value.
     *
     * @param x  center of view in x coord
     * @param y  center of view in y coord
     * @param zoomFactor    zoomFactor, i.e. "component pixels" / "viewport pixels"
     *
     */
    public void setViewport(double x, double y, double zoomFactor){

            if(mThreadAnimateViewport != null) {
                try {
                    mThreadAnimateViewport.interrupt();
                    mThreadAnimateViewport.join();
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                } catch( NullPointerException e) {

                }
            }

        this.mViewportCenterX     = x;
        this.mViewportCenterY     = y;
        this.mViewportZoomFactor  = zoomFactor;

        fireCardPaneEvent( new CardPaneEvent( CardPaneEvent.EventType.VISIBLE_ELEMENTS_CHANGED , this.getViewport() ) );
        repaint();
    }

    public double getViewportCenterX() {
        return mViewportCenterX;
    }

    public double getViewportCenterY() {
        return mViewportCenterY;
    }

    public double getZoomFactor(){
        return this.mViewportZoomFactor;
    }


    public Rectangle2D getViewport(){

        double vw = this.getWidth()  / this.getZoomFactor();
        double vh = this.getHeight() / this.getZoomFactor();

        if(this.getWidth()==0 || this.getHeight()==0){
            vw = this.getPreferredSize().getWidth();
            vh = this.getPreferredSize().getHeight();
        }

        Rectangle2D viewport = new Rectangle2D.Double( getViewportCenterX() -0.5*vw , getViewportCenterY() - 0.5*vh , vw,vh );

        return viewport;
    }


    /**
     * Gets the current affine transform. This is computed from the current viewport settings.
     */
    public AffineTransform getAffineTransform(){

        AffineTransform at = null;
        if(false) {
            at = AffineTransform.getTranslateInstance((0.5 * this.getWidth()) - this.getViewportCenterX(), (0.5 * this.getHeight()) - this.getViewportCenterY());
            double zf = getZoomFactor();
            at.scale(zf, zf);
        }
        if(false){
            at = AffineTransform.getScaleInstance(getZoomFactor(),getZoomFactor());
            at.translate( (0.5 * this.getWidth()  ) - this.getViewportCenterX(), (0.5 * this.getHeight() ) - this.getViewportCenterY() );
            //at.translate( this.getViewportCenterX() -  (0.5 * this.getWidth() +this.getX() ) ,   this.getViewportCenterX() -  (0.5 * this.getHeight() + this.getY() )  );

            //at.scale(zf, zf);
        }
        if(true){

            double vw = this.getWidth() / this.getZoomFactor() ;
            double vh = this.getHeight() / this.getZoomFactor() ;

            at = AffineTransform.getTranslateInstance( (int) this.getWidth()/2 , (int) this.getHeight()/2  );
            at.scale(getZoomFactor(),getZoomFactor());
            at.translate(-this.getViewportCenterX() , -this.getViewportCenterY());

            //at = AffineTransform.getScaleInstance(getZoomFactor(),getZoomFactor());
            //at.translate(this.getViewportCenterX(),this.getViewportCenterY());
            //at.translate( (this.getViewportCenterX()-this.getWidth()/this.getZoomFactor()), ( this.getViewportCenterY()-this.getHeight()/this.getZoomFactor() ) );
            //at = AffineTransform.getTranslateInstance((this.getViewportCenterX()-this.getWidth()/this.getZoomFactor()), ( this.getViewportCenterY()-this.getHeight()/this.getZoomFactor() ));
            //at.scale(getZoomFactor(),getZoomFactor());

        }
        return at;
    }



    @Override
    public void compoundTableChanged(CompoundTableEvent e) {

        if(logLevel>1) {
            System.out.println("EVENT!!");
            System.out.println(e);
        }

        if( e.isAdjusting() ){return;}

        if( e.getType() == CompoundTableEvent.cChangeExcluded){

            //recomputeHiddenCardElements(true);
            recomputeHiddenCardElements(true , false );
            this.mCachedCardProvider.setAllStacksToDirty();
            // we have to recompute hidden cards..
            //this.getAllCardElements().stream().forEach( ci -> ci.setHiddenStatus( CardElement.HIDDEN_STATUS_TO_COMPUTE ) );
        }
        if( e.getType() == CompoundTableEvent.cChangeSelection){
            this.mCachedCardProvider.setAllStacksToDirty();
        }

        //@TODO implement other cases..
    }

//    public void recomputeExcludedCardElements(){
//        Thread computeNXCE = new Thread( new NonExcludedCardsRecomputation() );
//        computeNXCE.start();
//    }


    private boolean mProposeStacks = true;

    /**
     * Should the JCardPane propose stacks when moving cards on top of eachother?
     * @param enable
     */
    public void setProposeStacks(boolean enable){
        this.mProposeStacks = enable;
    }

    private boolean mCheckForSnapAreas = true;

    private boolean mOnlyAllowSnapAreaPositions = false;

    public void setCheckForSnapAreas(boolean enable){
        this.mCheckForSnapAreas = true;
    }

    /**
     * If enabled, we assume that all cards are in a snapped area, and drags can only go to SnapAreas. If a dragged
     * card is released while not on a SnapArea, it is reverted to it's original position.
     * @param enable
     */
    public void setOnlyAllowSnapAreaPositions(boolean enable){
        this.mOnlyAllowSnapAreaPositions = enable;
    }

    /**
     * We revert to this point if we are in OnlyAllowSnapAreaPositions mode, and a dragged card is released while not
     * on a drag area.
     */
    private Point2D mOnlyAllowSnapAreasOriginalDragPosition = null;


    /**
     * Sets mHighlightedCardElement
     * and fires the highlighted card element changed event to the CompoundTableModel
     *
     * NOTE: highlightedCardElement is only set if we are over a Card, for a stack there is not a single
     * row to highlight.
     *
     * @param ce
     */
    public void setHighlightedCardElement(CardElement ce){
        //if(this.mHighlightedCardElement != ce) {
        if(ce==null){
            if(this.mHighlightedCardElement==null){

            }
            else {
                this.mHighlightedCardElement=null;repaint();
                this.getCompoundTableModel().setHighlightedRow(null);
            }
            return;
        }
        else{
            if(logLevel>1) {
                System.out.println("Set highlighted card element: " + ce.toString());
            }
            // set
            if(this.mHighlightedCardElement==null){
                this.mHighlightedCardElement = ce;
                this.repaint();

                if(ce.isCardAfterExclusion()){
                    this.getCompoundTableModel().setHighlightedRow( ce.getAllNonexcludedRecords().get(0) );
                }
                else{
                    this.getCompoundTableModel().setHighlightedRow( null );
                }

            }
            else{
                if(!this.mHighlightedCardElement.equals(ce)) {
                    //System.out.println("set highlighted card = "+ce);
                    this.mHighlightedCardElement = ce;
                    this.repaint();

                    if(ce.isCardAfterExclusion()){
                        this.getCompoundTableModel().setHighlightedRow( ce.getAllNonexcludedRecords().get(0) );
                    }
                    else{
                        this.getCompoundTableModel().setHighlightedRow( null );
                    }

                }
            }
        }

    }

    public CardElement getHighlightedCardElement(){
        return this.mHighlightedCardElement;
    }

    /**
     * Has to be called to update the z-sorted set after changes to z value of specific cardelements.
     *
     * @param ce  CardElement that changed z value.
     */
    public void updateZSortedSet(CardElement ce){
        //this.getCardElementsZSorted().remove(ce);
        //this.getCardElementsZSorted().add(ce);
    }


    public void setCardElementToFront(CardElement ce){
        int topZIndex = this.mCardPaneModel.getReducedAllElements().stream().mapToInt( cei -> cei.getZ()).max().orElse(10);
        this.mTopZIndex = topZIndex+1;
        //ce.setZ(this.mTopZIndex);
        ce.setZ(this.mTopZIndex);
        ce.setHiddenStatus(CardElement.HIDDEN_STATUS_VISIBLE);
        //updateZSortedSet(ce);
    }

    public void setCardElementsToFront(Collection<CardElement> ce) {

        int topZIndex = this.mCardPaneModel.getReducedAllElements().stream().mapToInt( cei -> cei.getZ()).max().orElse(10) + 2;
        for(CardElement ci : ce){
            ci.setZ(topZIndex++);
            // maybe we better set this to "to_compute", else if we have tons of overlapping cards, we set way too many to visible..
            ci.setHiddenStatus(CardElement.HIDDEN_STATUS_TO_COMPUTE_WAS_VISIBLE);
        }
        //ce.forEach(c -> setCardElementToFront(c) );
    }


    /**
     * NOTE! In case that currently a number of records of this card elements is excluded, this will SPLIT the selected stack.
     * I.e. the dragged card element will contain the currently non-excluded ones, and in addition there will be a new stack
     * created at this position containing the excluded ones.
     *
     * @param ce
     */
    public void setDraggedCardElement(CardElement ce){


        this.mDraggedCardElement = ce;

        if(ce==null){return;}

        if(ce.getAllRecords().size() > ce.getNonexcludedRecords().size()){
            if(logLevel>1) {
                System.out.println("We hit a stack containing hidden compound records -> split");
            }

            List<CompoundRecord> excluded = new ArrayList<>(ce.getAllExternallyExcludedRecords());
            this.getCardPaneModel().removeRecordsFromCE(ce,excluded);

            this.getCardPaneModel().addCE(excluded, ce.getPosX() + 0.1 * ce.getRectangle().getWidth() , ce.getPosY() + 0.1 * ce.getRectangle().getHeight());
            this.getCardPaneModel().updateRowHiding();
        }


        if(ce!=null) {
            this.mDraggedCardStartX  = ce.getRectangle().getCenterX();
            this.mDraggedCardStartY  = ce.getRectangle().getCenterY();

            // bring to foreground
            setCardElementToFront(ce);
            this.repaint();
        }
    }

    public CardElement getDraggedCardElement(){
        return this.mDraggedCardElement;
    }


    /**
     * updates internal mSelectedElements collection AND updates DataWarrior selection.
     *
     * @param cards
     */
    public void setSelection(Collection<CardElement> cards){
        setSelection(cards,true);
    }


    /**
     * If true, then the global selection does not change the selected card elements in the cardview.
     * This is enabled while we sync the selection from the card view to global.
     */
    private boolean mIgnoreExternalSelection = false;

    /**
     * updates internal mSelectedElements collection and IF changeGlobaSelection is true, it also
     * updates the global DataWarrior selection.
     *
     * @param cards
     */
    public void setSelection(Collection<CardElement> cards, boolean changeGlobalSelection){
        // return when we just react to an event that was caused by syncing the cardpane selection to the global selection.
        if(mIgnoreExternalSelection){return;}

        this.mSelectedElements.clear();
        this.mSelectedElements.addAll(cards);

        //this.mElements.stream().forEach( e -> e.getRecords().stream().forEach( r -> r.setSelection(false) ) );

        if(changeGlobalSelection) {
            if (mCardView != null) {
                mIgnoreExternalSelection = true;
                this.mCardView.getCompoundListSelectionModel().clearSelection();
                mIgnoreExternalSelection = false;
                for (CardElement ce : this.mSelectedElements) {
                    for (CompoundRecord cr : ce.getAllRecords()) {
                        cr.setSelection(true);
                        //this.mCardView.getCompoundListSelectionModel().addSelectionInterval(cr.getID(), cr.getID());
                    }
                }
                this.mCardView.getCompoundListSelectionModel().invalidate();
            }
        }

        if(logLevel>1) {
            System.out.println("selected elements: ");
            for (CardElement cei : this.mSelectedElements) {
                System.out.println(cei.toString());
            }
        }
    }


    /**
     * NOTE! In case that currently a number of records of card elements in this selction are excluded, this will SPLIT
     * all selected stacks.
     * I.e. the dragged card elements will contain the currently non-excluded ones, and in addition there will be new stacks
     * created at their original positions with the excluded ones.
     *
     *
     */
    private void startDraggingSelection(){
        this.mDraggedSelectionStartCoordinates.clear();
        for(CardElement ce : this.mSelectedElements){
            this.mDraggedSelectionStartCoordinates.add( new Point2D.Double( ce.getRectangle().getCenterX() , ce.getRectangle().getCenterY() ) );
        }

        // check for all card elements whether we have to split, and do the split if necessary:
        for(CardElement ce : this.mSelectedElements){
            if(ce.getAllRecords().size() > ce.getNonexcludedRecords().size() ) {
                if(logLevel>1) {
                    System.out.println("While start dragging the selection we hit a stack containing hidden compound records -> split");
                }
                List<CompoundRecord> excluded = new ArrayList<>(ce.getAllExternallyExcludedRecords());
                this.getCardPaneModel().removeRecordsFromCE(ce, excluded);

                this.getCardPaneModel().addCE(excluded, ce.getPosX() + 0.1 * ce.getRectangle().getWidth(), ce.getPosY() + 0.1 * ce.getRectangle().getHeight());
            }
        }

        this.getCardPaneModel().updateRowHiding();

        // bring to foreground
        setCardElementsToFront(this.mSelectedElements);
//        this.mTopZIndex += 2;
//        for( CardElement ce : this.getSelection()){
//            ce.setZ(this.mTopZIndex);
//            this.mTopZIndex+=2;
//        }
    }


    /**
     * Returns all selected card elements
     *
     * @return
     */
    public List<CardElement> getSelection(){
        return this.mSelectedElements;
    }



    private CardElement getProposedStackElement(){
        return this.mProposedStackElement;
    }

    private void setProposedStackElement(CardElement ce){
        this.mProposedStackElement = ce;
    }


    public AbstractCardDrawer getCardDrawer() {
        return mCardDrawer;
    }

    public AbstractStackDrawer getStackDrawer() {
        return mStackDrawer;
    }


    /**
     * Causes, that in the CachedCardProvider all stacks are set to dirty, which will have them recomputed.
     */
    public void recomputeAllBufferedStacks(){
        this.mCachedCardProvider.setAllStacksToDirty();
    }


// just commented out until we replaced all getElements..@TODO: uncomment..
//
//    public List<CardElement> getCardElements(){
//        return this.mCardPaneModel.getAllElements();
//        //return this.mElements;
//    }




//    /**
//     * milliseconds to wait inbetween recomputations of this one..
//     */
//    //private int CONF_TIME_INTERVAL_CARDS_NON_INVISIBLE_MS = 200;
//
//
//    /**
//     * Returns the list of card elements after removing all invisible elements.
//     *
//     * @return
//     */
//    public List<CardElement> getNonExcludedCardElements(){
//        return mNonExcludedElements;
//    }
//
//
//
//    private class NonExcludedCardsRecomputation implements Runnable {
//        @Override
//        public void run() {
//            List<CardElement> newList = computeNonExcludedCardElements();
//            mNonExcludedElements = newList;
//        }
//
//        public List<CardElement> computeNonExcludedCardElements() {
//            System.out.println("NonExcludedCardsRecomputation::compute");
//            List<CardElement> listCardsNonInvisible = new ArrayList<>();
//            if(getCardElements()==null){
//                // leave the empty list..
//            }
//            else {
//                for (CardElement cei : new ArrayList<>(getCardElements())) {
//                    List<CompoundRecord> listNonInvisibleRecords = cei.getRecords().stream().filter(cri -> mTableModel.isVisible(cri)).collect(Collectors.toList());
//                    if (listNonInvisibleRecords.size() > 0) {
//                        //listCardsNonInvisible.add( (CardElement) cei.clone() );
//                        listCardsNonInvisible.add( cei );
//                    }
//                }
//            }
//            return listCardsNonInvisible; //Collections.unmodifiableList( listCardsNonInvisible );
//        }
//    }

    long timestampPaintComponent = 0;


    /**
     *
     * 1. computes card elements that have to be drawn
     * 2. loops over them and draws them (sorted by their zorder)
     * 3. depending on performance settings and number of cards to draw, decides on the drawing mode for the card elements
     *
     *
     *
     * NOTES:
     *
     * Greying out of CardElements:
     * Some general comments:
     * 1. greyed out records NEVER change a stack into a card, stack remains stack (this is the
     *    only way to maintain consistent behavior)
     * 2. card
     *
     *
     * @param g1
     */
    @Override
    public void paintComponent(Graphics g1){
        timestampPaintComponent = System.currentTimeMillis();

        Graphics2D g2 = (Graphics2D) g1;


        //g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        //g2.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_SPEED);


        g2.setStroke(new BasicStroke(2));

        AffineTransform originalTransform = g2.getTransform();

        // Set Transform:
        Rectangle2D viewport = getViewport();

        //g2.setTransform(getAffineTransform());
        g2.transform(getAffineTransform());
        //g1.clipRect( (int) viewport.getX(), (int) viewport.getY(), (int) viewport.getWidth(), (int) viewport.getHeight());


        // compute "safe viewport" which contains card centers of cards on the boundary of the viewport
//        double safetyW = Math.max( 0.1 * viewport.getWidth() , 1.1 * this.getCardDrawer().getCardDrawingConfig().getCardWidth() );
//        double safetyH = Math.max( 0.1 * viewport.getHeight() , 1.1 * this.getCardDrawer().getCardDrawingConfig().getCardHeight() );
        double safetyW = 0.2 * this.getCardDrawer().getCardDrawingConfig().getCardWidth() ;
        double safetyH = 0.2 * this.getCardDrawer().getCardDrawingConfig().getCardHeight() ;

        Rectangle2D safeViewport = new Rectangle((int) (viewport.getX()-safetyW),(int) ( viewport.getY()-safetyH),(int) (viewport.getWidth()+2*safetyW), (int) (viewport.getHeight()+2*safetyH) );



        //g2.setColor(Color.white);
        //g2.setPaint(new GradientPaint(((Rectangle) safeViewport).getLocation(),new Color(40,40,40),new Point2D.Double(safeViewport.getMaxX(),safeViewport.getMaxY()),new Color(200,200,200)));
        g2.setPaint( new Color(240 , 250 , 250) );
        g2.fillRect((int) (viewport.getX()-safetyW),(int) ( viewport.getY()-safetyH),(int) (viewport.getWidth()+2*safetyW), (int) (viewport.getHeight()+2*safetyH));


        //dark
        //paintTestBackground(g1, new int[]{-2000000,2000000}, new int[]{-2000000,2000000} , 2000 , 2000, Color.DARK_GRAY.darker() ,Color. DARK_GRAY );
        //bright
        //paintTestBackground(g1, new int[]{-2000000,2000000}, new int[]{-2000000,2000000} , 2000 , 2000, Color.lightGray ,Color.WHITE );




        double defaultBoundaryStrokeSize =  (2.0 /  this.getZoomFactor());
        Stroke defaultBoundaryStroke     = new BasicStroke((float)defaultBoundaryStrokeSize);


        // get all cards
        List<CardElement> cardsAll = this.mCardPaneModel.getAllElements();// getReducedAllElements();

        // sort out empty cards
        List<CardElement> cardsAllReduced = cardsAll.stream().filter( ce -> ! ce.isEmptyAfterExclusion() ).collect( Collectors.toList() ) ;// getReducedAllElements();

        // sort out cards by intersection with (safe) viewport
        List<CardElement> cardsToDrawWithHidden = cardsAllReduced.stream().filter(cei -> cei.getRectangle().intersects(safeViewport)).collect(Collectors.toList());    //  getVisibleElements(xce -> xce.getRectangle().intersects(safeViewport) );

        // sort out "hidden" cards (hidden means fully overlapped by other cards, this is computed in a separate thread)
        List<CardElement> cardsToDraw = cardsToDrawWithHidden.stream().filter( ce -> !ce.isHidden() ).collect(Collectors.toList());

        cardsToDraw.sort(new CardElementZComparator());

        // determine graphics mode based on performance settings:

        if(cardsToDraw.size() < mThreshCards_NORMAL && mSwitchToHiQuality ) {
            this.mRenderQuality = RenderQuality.FULL;
        }
        else if(cardsToDraw.size() < mThreshCards_FAST) {
            this.mRenderQuality = RenderQuality.NORMAL;
        }
        else if(cardsToDraw.size() < mThreshCards_SUPERFAST){
            this.mRenderQuality = RenderQuality.FAST;
        }
        else{
            this.mRenderQuality = RenderQuality.SUPERFAST;
        }

        //g2.setRenderingHint( RenderingHints.KEY_INTERPOLATION , RenderingHints.VALUE_INTERPOLATION_BICUBIC );
        //g2.setRenderingHint( RenderingHints.KEY_INTERPOLATION , RenderingHints.VALUE_INTERPOLATION_BILINEAR );

        AffineTransform atZero = g2.getTransform();

        // inform the card buffering mechanism about the required cards
        this.mCachedCardProvider.setRequestedCards(cardsToDraw);

        for(CardElement c : cardsToDraw){

            // drag and drop : hide card potentially:
            if(DD_getHiddenCardElement()==c) {
                g2.setStroke(new BasicStroke(2));
                g2.setColor(new Color(0.2f,0.2f,0.2f,0.4f));
                g2.drawRect( (int) c.getRectangle().getMinX() , (int) c.getRectangle().getMinY() ,
                        (int) c.getRectangle().getWidth() , (int) c.getRectangle().getHeight() );
                continue;
            }

            g2.setRenderingHint( RenderingHints.KEY_INTERPOLATION , RenderingHints.VALUE_INTERPOLATION_BILINEAR );

            if(c.isEmptyAfterExclusion()){
                System.out.println("!!!ERROR!!! Something very wrong..");
                continue;
            }
            if(c.isHidden()){
                // This can happen, if inbetween selecting the cardsToDraw and the loop the HiddenCardsComputeThread processed this one.
                continue;
            }
            Rectangle2D cr = c.getRectangle();

            // rotate
            g2.setTransform(atZero);
            AffineTransform atRotateCard = AffineTransform.getTranslateInstance( cr.getCenterX() , cr.getCenterY() );
            atRotateCard.rotate(c.getAngle() );
            atRotateCard.translate( -cr.getCenterX() , -cr.getCenterY() );
            g2.transform(atRotateCard);

            if(c.isCardAfterExclusion()) {

                boolean isGreyedOut = c.containsNonexcludedGreyedOutRecords();

                // Draw Card
                if(flagDebug_drawCardElementRectangles) {
                    g2.setColor(c.debugColor);
                    g2.fillRect((int) cr.getX(), (int) cr.getY(), (int) cr.getWidth(), (int) cr.getHeight());
                    g2.setColor(new Color(0, 0, 0));
                    g2.drawString("" + c.getNonexcludedRecords().size(), (float) cr.getX(), (float) cr.getY());
                }

                if(!c.isHidden()) {

                    if ( this.mRenderQuality == RenderQuality.FULL ) {
                        g2.transform(AffineTransform.getTranslateInstance(cr.getX(), cr.getY()));
                        this.mCardElementBackgroundDrawer.drawBackground( g1 , (int) cr.getWidth() , (int) cr.getHeight() , 1 , !this.isDisableShadowsEnabled(), 1 );
                        this.getCardDrawer().drawCard(g1, mTableModel, c.getNonexcludedRecords().get(0));
                        g2.transform(AffineTransform.getTranslateInstance(-cr.getX(), -cr.getY()));
                    }
                    else if(this.mRenderQuality == RenderQuality.NORMAL || this.mRenderQuality == RenderQuality.FAST ) {

                        // draw background (only for NORMAL)
                        //if(this.mRenderQuality == RenderQuality.NORMAL && !this.isDisableShadowsEnabled() ) {
                        if(this.mRenderQuality == RenderQuality.NORMAL ) {
                            g2.translate(cr.getBounds2D().getMinX(), cr.getBounds2D().getMinY());
                            this.mCardElementBackgroundDrawer.drawBackground(g1, (int) cr.getWidth(), (int) cr.getHeight(), 1, !this.isDisableShadowsEnabled() , 1);
                            g2.translate(-cr.getBounds2D().getMinX(), -cr.getBounds2D().getMinY());
                        }
                        // draw the buffered image of the card surface
                        BufferedImage bim = this.mCachedCardProvider.getImage(c);
                        if (bim != null) {
                            g2.drawImage(bim, (int) cr.getX(), (int) cr.getY(), (int) (cr.getX() + cr.getWidth()), (int) (cr.getY() + cr.getHeight()),
                                    0, 0, bim.getWidth(), bim.getHeight(),
                                    this);

                        } else {
                        }
                    }
                    else if(this.mRenderQuality == RenderQuality.SUPERFAST){
                        g2.setColor(Color.lightGray);
                        g2.fillRect( (int) cr.getX(), (int) cr.getY(), (int) (cr.getWidth()), (int) (cr.getHeight()) );
                    }

                }

                if(isGreyedOut){
                    g2.setColor(new Color(0,0,0,125));
                    g2.fillRect((int) cr.getX(), (int) cr.getY(), (int) (cr.getWidth()), (int) (cr.getHeight()));
                }

            }
            else{

                // DRAW STACK..

                // grey out if all are excluded..
                boolean isGreyedOut = (c.getNonexcludedRecords().size()==c.getNonExcludedGreyedOutRecords().size());

                if(flagDebug_drawCardElementRectangles) {
                    g2.setColor(new Color(0.9f, 0.8f, 0.8f, 1.0f));
                    g2.fillRect((int) cr.getX(), (int) cr.getY(), (int) cr.getWidth(), (int) cr.getHeight());
                    g2.setColor(new Color(0, 0, 0));
                    g2.drawString("" + c.getNonexcludedRecords().size(), (float) cr.getX(), (float) cr.getY());
                }
                if(!c.isHidden()){
                    if ( this.mRenderQuality == RenderQuality.FULL ) {
                        g2.transform(AffineTransform.getTranslateInstance(cr.getX(), cr.getY()));
                        this.mCardElementBackgroundDrawer.drawBackground( g1 , (int) cr.getWidth() , (int) cr.getHeight() , c.getNonexcludedRecords().size() , !this.isDisableShadowsEnabled() , 1 );
                        this.mStackDrawer.drawStack(g1, mTableModel, mCardView.getFocusListFlag(), c.getNonexcludedRecords() , c );
                        g2.transform(AffineTransform.getTranslateInstance(-cr.getX(), -cr.getY()));
                    }
                    else if(this.mRenderQuality == RenderQuality.NORMAL || this.mRenderQuality == RenderQuality.FAST ) {

                        // draw background (only for NORMAL)
                        if(this.mRenderQuality == RenderQuality.NORMAL) {
                            g2.translate(cr.getBounds2D().getMinX(), cr.getBounds2D().getMinY());
                            this.mCardElementBackgroundDrawer.drawBackground(g1, (int) cr.getWidth(), (int) cr.getHeight(), c.getNonexcludedRecords().size(), !this.isDisableShadowsEnabled() , 1);
                            g2.translate(-cr.getBounds2D().getMinX(), -cr.getBounds2D().getMinY());
                        }

                        // draw stack surface
                        BufferedImage bim = this.mCachedCardProvider.getImage(c);
                        if (bim != null) {
                            g2.drawImage(bim,
                                    (int) cr.getX(), (int) cr.getY(), (int) (cr.getX() + cr.getWidth()), (int) (cr.getY() + cr.getHeight()),
                                    0, 0, bim.getWidth(), bim.getHeight(),
                                    this);

                        } else {

                        }
                    }
                    else if(this.mRenderQuality == RenderQuality.SUPERFAST){
                        g2.setColor(Color.gray);
                        g2.fillRect( (int) cr.getX(), (int) cr.getY(), (int) (cr.getWidth()), (int) (cr.getHeight()) );
                    }

                    if(isGreyedOut){
                        g2.setColor(new Color(0,0,0,125));
                        g2.fillRect((int) cr.getX(), (int) cr.getY(), (int) (cr.getWidth()), (int) (cr.getHeight()));
                    }

                }
                else{
//                    g2.setColor( new Color(1.0f,0.0f,0.0f,0.25f) );
//                    g2.fillRect( (int) c.getRectangle().getX(), (int) c.getRectangle().getY(), (int) c.getRectangle().getWidth(), (int) c.getRectangle().getHeight() );
                }
            }

            // draw card border:
            g2.setColor(Color.darkGray.darker());
            g2.setStroke(defaultBoundaryStroke);
            g2.drawRect((int) cr.getX(), (int) cr.getY(), (int) cr.getWidth(), (int) cr.getHeight());

            // draw selection
            if(c.getNonexcludedRecords().get(0).isSelected()){
                //g2.setColor(Color.orange.darker());

                if(mHighlightedCardElement==c){
                    g2.setColor(mConf_Color_MouseClicked);
                }
                else {
                    g2.setColor(mConf_Color_Selected);
                }
                //g2.setStroke(new BasicStroke( (int) (4.0 /  this.getZoomFactor()) ));
                g2.setStroke(new BasicStroke(  (int) Math.max( 2 , (2.0 /  this.getZoomFactor())) ) );

                g2.drawRect((int) cr.getX()-1, (int) cr.getY()-1,(int) mCardDrawer.getCardDrawingConfig().getCardWidth()+2,(int) mCardDrawer.getCardDrawingConfig().getCardHeight()+2);
            }

            if(c.getNonexcludedRecords().size()==1) {
                if( getCompoundTableModel().getActiveRow() == c.getNonexcludedRecords().get(0) ) {
                    g2.setColor(COLOR_ACTIVE);
                    g2.setStroke(new BasicStroke(  (int) Math.max( 2 , (2.0 /  this.getZoomFactor())) ) );
                    g2.drawRect( (int) cr.getX()-3 , (int) cr.getY()-3 , (int) cr.getWidth()+6 , (int) cr.getHeight()+6 );
                }
            }

            if(c==this.getHighlightedCardElement()){
                //new BasicStroke( (int) (4.0 /  this.getZoomFactor()) );
                g2.setStroke(new BasicStroke(  (int) Math.max( 2 , (2.0 /  this.getZoomFactor())) ) );

                //g2.setColor(new Color(0.75f,0.4f,0.1f,1.0f));
                g2.setColor(mConf_Color_MouseOver);
                g2.drawRect( (int) cr.getX()-1 , (int) cr.getY()-1 , (int) cr.getWidth()+2 , (int) cr.getHeight()+2 );
            }

        }

        g2.setTransform(atZero);

        if(this.mCardView==null) {
            // we assume, in this case we do not have a tablemodel etc.. i.e. we can rely on just the mSelectedElements:
            // else, the CardSurfaceDrawer / StackSurfaceDrawer will handle this..
            if (!mSelectedElements.isEmpty()) {
                for (CardElement ce : mSelectedElements) {
                    //g2.setColor(Color.blue.darker());
                    g2.setColor(COLOR_SELECTION);
                    new BasicStroke( (int) (4.0 /  this.getZoomFactor()) );
                    g2.drawRect((int) ce.getRectangle().getX(), (int) ce.getRectangle().getY(), (int) ce.getRectangle().getWidth(), (int) ce.getRectangle().getHeight());
                }
            }
        }


        // draw selection lass / rect
        // determine stroke based on zoom level..
        g2.setColor(Color.red.darker());
        g2.setStroke( new BasicStroke( (int) (4.0 /  this.getZoomFactor()) ) );
        drawSelectionOutline(g2);

        // drag and drop: draw phantom card on top..
        if(DD_isDrawPhantomCard()) {
            double cw = this.getCardDrawer().getCardDrawingConfig().getCardWidth();
            double ch = this.getCardDrawer().getCardDrawingConfig().getCardHeight();

            if(m_DD_Image!=null) {
                g2.drawImage(m_DD_Image,(int) (m_DD_PhantomCard_x - 0.5 * ch), (int) (m_DD_PhantomCard_y - 0.5 * ch), (int) cw, (int) ch,this);
            }
            else {
                g2.setColor(Color.red.darker());
                g2.setStroke(new BasicStroke((int) (4.0 / this.getZoomFactor())));
                g2.drawRect( (int) (m_DD_PhantomCard_x - 0.5 * ch), (int) (m_DD_PhantomCard_y - 0.5 * ch), (int) cw, (int) ch);
            }
        }
        if(DD_getPhantomProposedStack() != null) {

            CardElement  ce_pp     = DD_getPhantomProposedStack();
            Rectangle2D  ce_ppr_2d = ce_pp.getRectangle();
            Rectangle    ce_r      = new Rectangle( (int) ce_ppr_2d.getX() , (int) ce_ppr_2d.getY() , (int) ce_ppr_2d.getWidth() , (int) ce_ppr_2d.getHeight() );

            g2.setColor(new Color(160,20,20,140));
            g2.fillRect((int) ce_r.getX(),(int) ce_r.getY(),(int) ce_r.getWidth(), (int) ce_r.getHeight());
            g2.setColor(Color.red.darker());
            g2.setStroke(new BasicStroke((int) (4.0 / this.getZoomFactor())));
            g2.drawRect( (int) ce_r.getX(),(int) ce_r.getY(),(int) ce_r.getWidth(), (int) ce_r.getHeight() );
        }


        // done drawing, reset transform..
        g2.setTransform(originalTransform);


//        g2.setColor(Color.red);
//        g2.drawRect( (int) this.getViewport().getX() , (int) this.getViewport().getY() , (int) this.getViewport().getWidth() , (int) this.getViewport().getHeight() );
//        g2.drawRect( (int) safeViewport.getX() , (int) safeViewport.getY() , (int) safeViewport.getWidth() , (int) safeViewport.getHeight() );
//        g2.setColor(Color.green.darker());
//        g2.fillRect(  (int)this.getViewportCenterX()-8 , (int) this.getViewportCenterX()-8 , 16,16 );


        if( System.currentTimeMillis() - debug_TimestampLastDebugOutput > (long) ( 1000*debug_TimeGraphicsDebugOutput ) && (System.getProperty("development") != null) ){
            debug_TimestampLastDebugOutput = System.currentTimeMillis();

            if(logLevel>1) {
                System.out.println("|GraphicsDebugOutput");
                System.out.println("|--Cards To Draw : " + cardsToDraw.size());
                System.out.println("|--RenderQuality : " + getRenderQuality());
            }
        }

        //if( flagDebug_showDebugOutputOnScreen){
        if( (System.getProperty("development") != null) && flagDebug_showDebugOutputOnScreen ) {
            // compute cards intersecting screen but hidden:
            Rectangle2D xvp      = this.getViewport();
            long numCardElementsInViewport  = cardsToDrawWithHidden.size();
            long numHiddenCardsInViewport   = cardsToDrawWithHidden.stream().filter( ci -> ci.isHidden() ).count();
            long numCardsToDraw             = cardsToDraw.size();

            //g2.setColor(Color.cyan.darker().darker().darker());
            g2.setColor(Color.red.darker());
            double timePaintComponentSeconds = (1.0*System.currentTimeMillis()-timestampPaintComponent)/1000.0;
            if(logLevel>0) {
                String debugString = String.format("FPS= %5.3f   CEs/s= %6.3f   CardElementsToDraw= %d  CardElementsInViewport= %d    Hidden CEs in Viewport= %d", (1.0 / timePaintComponentSeconds), (numCardsToDraw / timePaintComponentSeconds), numCardsToDraw, numCardElementsInViewport, numHiddenCardsInViewport);
                g2.setFont(debug_OutputFont);
                g2.drawString(debugString, 20, 20);
            }
        }
    }


    public static enum CardPaneHQDrawOptions {DRAW_ALL , DRAW_VIEWPORT , DRAW_SELECTION};

//    /**
//     * @TODO implement..
//     *
//     * @param options
//     */
//    public void paintCardPane_HQ(CardPaneHQDrawOptions options){
//        //JList<CardElement>
//        System.out.println("NOT YET IMPLEMENTED..");
//        //throw new NotImplementedException();
//    }


    public void paintImmediately(int x, int y, int w, int h){
        super.paintImmediately(x,y,w,h);
    }

    protected void drawSelectionOutline(Graphics g) {
        g.setColor(VisualizationColor.cSelectedColor);
        if (mDragMode == MOUSE_MODE_SELECTION_RECT) {
            double x0 = Math.min( mRectangularSelectionAnchor.getX() , mRectangularSelectionP2.getX() );
            double x1 = Math.max( mRectangularSelectionAnchor.getX() , mRectangularSelectionP2.getX() );
            double y0 = Math.min( mRectangularSelectionAnchor.getY() , mRectangularSelectionP2.getY() );
            double y1 = Math.max( mRectangularSelectionAnchor.getY() , mRectangularSelectionP2.getY() );
            g.drawRect((int)x0,(int)y0, (int)(x1-x0), (int)( y1-y0 ) );
//            g.drawRect((mMouseX1<mMouseX2) ? mMouseX1 : mMouseX2,
//                    (mMouseY1<mMouseY2) ? mMouseY1 : mMouseY2,
//                    Math.abs(mMouseX2-mMouseX1),
//                    Math.abs(mMouseY2-mMouseY1));
        }
        if (mDragMode == MOUSE_MODE_SELECTION_LASSO) {
            g.drawPolygon(mLassoRegion);
        }
    }


    /**
     * Implement the graphics export..
     *
     * @param g
     * @param bounds
     * @param fontScaling
     * @param transparentBG
     * @param isPrinting
     */
    public void paintHighResolution(Graphics2D g, Rectangle bounds, float fontScaling, boolean transparentBG, boolean isPrinting) {
        // boundary stroke
        double defaultBoundaryStrokeSize =  (2.0 /  this.getZoomFactor());
        Stroke defaultBoundaryStroke     = new BasicStroke((float)defaultBoundaryStrokeSize);

        // get all cards
        List<CardElement> cardsAll = this.mCardPaneModel.getAllElements();// getReducedAllElements();

        // sort out empty cards
        List<CardElement> cardsAllReduced = cardsAll.stream().filter( ce -> ! ce.isEmptyAfterExclusion() ).collect( Collectors.toList() ) ;// getReducedAllElements();

        // sort out cards by intersection with (safe) viewport
        List<CardElement> cardsToDraw = cardsAllReduced.stream().filter(cei -> cei.getRectangle().intersects(bounds)).collect(Collectors.toList());    //  getVisibleElements(xce -> xce.getRectangle().intersects(safeViewport) );

        cardsToDraw.sort(new CardElementZComparator());

        AffineTransform atZero = g.getTransform();

        for(CardElement c : cardsToDraw) {
            Rectangle2D cr = c.getRectangle();
            // rotate
            g.setTransform(atZero);
            AffineTransform atRotateCard = AffineTransform.getTranslateInstance( cr.getCenterX() , cr.getCenterY() );
            atRotateCard.rotate(c.getAngle() );
            atRotateCard.translate( -cr.getCenterX() , -cr.getCenterY() );
            g.transform(atRotateCard);
            if(c.isCardAfterExclusion()){
                g.transform(AffineTransform.getTranslateInstance(cr.getX(), cr.getY()));
                this.mCardElementBackgroundDrawer.drawBackground( g , (int) cr.getWidth() , (int) cr.getHeight() , 1 , !this.isDisableShadowsEnabled(), 1 );
                this.getCardDrawer().drawCard(g, mTableModel, c.getNonexcludedRecords().get(0));
                g.transform(AffineTransform.getTranslateInstance(-cr.getX(), -cr.getY()));

            }
            else{
                g.transform(AffineTransform.getTranslateInstance(cr.getX(), cr.getY()));
                this.mCardElementBackgroundDrawer.drawBackground( g , (int) cr.getWidth() , (int) cr.getHeight() , c.getNonexcludedRecords().size() , !this.isDisableShadowsEnabled() , 1 );
                this.mStackDrawer.drawStack(g, mTableModel, mCardView.getFocusListFlag(), c.getNonexcludedRecords() , c );
                g.transform(AffineTransform.getTranslateInstance(-cr.getX(), -cr.getY()));
            }
            // draw card border:
            g.setColor(Color.darkGray.darker());
            g.setStroke(defaultBoundaryStroke);
            g.drawRect((int) cr.getX(), (int) cr.getY(), (int) cr.getWidth(), (int) cr.getHeight());
        }
    }


    public void paintTestBackground(Graphics g1, int x[], int y[], int rasterx, int rastery , Color colA , Color colB){

        Graphics2D g = (Graphics2D) g1;
        Rectangle2D vp = getViewport();

        int xi0 = (int) Math.signum( vp.getMinX() ) * (( (int) Math.abs( vp.getMinX() ) ) / rasterx) - 1;
        int yi0 = (int) Math.signum( vp.getMinY() ) * (( (int) Math.abs( vp.getMinY() ) ) / rastery) - 1;

        int xi1 = (int) Math.signum( vp.getMaxX() ) * (  ( (int) Math.abs( vp.getMaxX() ) ) / rasterx )  + 1;
        int yi1 = (int) Math.signum( vp.getMaxY() ) * (  ( (int) Math.abs( vp.getMaxY() ) ) / rastery )  + 1;


        int x0 = xi0*rasterx;
        int y0 = yi0*rastery;

        int x1 = xi1*rasterx;
        int y1 = yi1*rastery;


        if( (xi1-xi0) * (yi1-yi0) > 2000 ){
            //g.setPaint(new GradientPaint(x0, y0, Color.DARK_GRAY.darker(), x1-x0, y1-y0, Color.DARK_GRAY.darker().darker()));
            g.setPaint(new GradientPaint(x0, y0, colA, x1-x0, y1-y0, colA.darker()));
            g.fillRect(x0,y0,x1-x0, y1-y0);
        }
        else{

            int fcnt = 0;
            int xcnt = 0;
            int ycnt = 0;
            //for(int xi=x[0];xi<=x[1];xi+=rasterx){
            //    for(int yi=x[0];yi<=y[1];yi+=rastery){
            for(int xi=x0;xi<=x1;xi+=rasterx) {
                for (int yi = y0; yi <= y1; yi += rastery) {
                    boolean blackwhite = (xcnt + ycnt) % 2 == 0;
                    if ((xi0 + yi0) % 2 == 0) {
                        blackwhite = !blackwhite;
                    }

                    if (blackwhite) {
                        //g.setColor(Color.DARK_GRAY.darker());
                        //g.setPaint(new GradientPaint(xi, yi, Color.DARK_GRAY.darker(), xi + rasterx, yi + rastery, Color.DARK_GRAY.darker().darker()));
                        g.setPaint(new GradientPaint(xi, yi, colA, xi + rasterx, yi + rastery, colA.darker()));
                    } else {
                        //g.setColor(Color.DARK_GRAY);
                        //g.setPaint(new GradientPaint(xi, yi, Color.DARK_GRAY, xi + rasterx, yi + rastery, Color.DARK_GRAY.darker()));
                        g.setPaint(new GradientPaint(xi, yi, colB, xi + rasterx, yi + rastery, colB.darker()));
                    }
                    //if(fcnt%2==0){g.setColor(Color.DARK_GRAY.darker());}else{g.setColor(Color.DARK_GRAY);}
                    g.fillRect(xi, yi, rasterx, rastery);
                    g.setColor(Color.green.darker().darker());
                    g.drawString("" + xi + "," + yi, xi, yi);
                    fcnt++;
                    ycnt++;
                }
                ycnt = 0;
                xcnt++;
            }
        }
    }


    public JCardView getCardView(){return mCardView;}



    public CardPaneModel<CardElement, CardElement.CardElementFactory> getCardPaneModel(){
        return this.mCardPaneModel;
    }


    /**
     *
     * @param records
     * @param px
     * @param py
     * @return the newly created card element
     */
    public CardElement addCardElement( Collection<CompoundRecord> records , double px, double py ){

        CardElement ce = createNewCardElement(new Point2D.Double(px,py),records);
        //mTopZIndex+=2;
        //this.mElements.add( ce );
        this.mCardPaneModel.addCE(ce);
        this.fireCardElementChanged( ce );
        return ce;
    }


//    public static void main(String args[]) {
//        JFrame f = new JFrame();
//        f.setSize(800,800);
//        f.setVisible(true);
//
//        f.getContentPane().addMouseListener( new FastMouseListener() );
//    }
//    static class FastMouseListener implements MouseListener {
//        boolean pressed = false;
//        @Override
//        public void mouseClicked(MouseEvent e) {
//        }
//
//        @Override
//        public void mousePressed(MouseEvent e) {
//            if(pressed){
//                System.out.println("WRONG ORDERING!!");
//            }
//            pressed = true;
//        }
//
//        @Override
//        public void mouseReleased(MouseEvent e) {
//            if(!pressed){
//                System.out.println("WRONG ORDERING!!");
//            }
//            pressed=false;
//        }
//
//        @Override
//        public void mouseEntered(MouseEvent e) {
//        }
//
//        @Override
//        public void mouseExited(MouseEvent e) {
//        }
//    }





    /**
     *
     * @param record
     * @param px
     * @param py
     * @return the newly created card element
     */
    public CardElement addCardElement( CompoundRecord record , double px, double py ){
        ArrayList<CompoundRecord> recList = new ArrayList<>();
        recList.add(record);
        return this.addCardElement( recList ,px,py);
    }


    public int getMaxZ(){
        return  ( new ArrayList<>(this.mCardPaneModel.getAllElements()) ).stream().mapToInt( cei ->  cei.getZ() ).max().orElse(1);
    }

    public void addNewCardForRecord( CompoundRecord record ){
        Point2D position = this.mCardPositioner.positionSingleCard(null, record);
        addCardElement(record,position.getX(),position.getY());
    }

    /**
     * Initializes a new "card" card element for each CR in the list.
     *
     * @param records
     */
    public void addNewCardsForRecords( List<CompoundRecord> records ){

        List<Collection<CompoundRecord>> crs_sorted = new ArrayList<>();
        List<Point2D>     new_dummy_pos = new ArrayList<>();
        for(int zi=0;zi<records.size();zi++){
            ArrayList<CompoundRecord> crs_i = new ArrayList<>();
            crs_i.add(records.get(zi));
            crs_sorted.add(crs_i);
            new_dummy_pos.add(new Point2D.Double(0,0));
        }
        List<CardElement> added_ces = this.createNewCardElements(new_dummy_pos,crs_sorted);

        List<Point2D> positions     = null;
        // position and add them to the CardPaneModel of the JCardPane..
        try {
            positions = this.mCardPositioner.positionAllCards(mTableModel, added_ces);
        }
        catch(InterruptedException e){
            System.out.println("Error : InterruptedException");
            return;
        }
        for(int zi=0;zi<added_ces.size();zi++){
            added_ces.get(zi).setPosX(positions.get(zi).getX());
            added_ces.get(zi).setPosY(positions.get(zi).getY());
            this.mCardPaneModel.addCE(added_ces.get(zi));
        }

    }


//    /**
//     * Removes the card elements which correspond to the given records.
//     *
//     * @param recordsToRemove
//     */
//    public void removeCardElementsForRecords(List<CompoundRecord> recordsToRemove){
//        this.mCardPaneModel.removeCardElementsForRecords(recordsToRemove);
//    }

//    /**
//     * Synchronizes the set of Card Elements according to changes in the set of records that should be shown.
//     * Works very simply:
//     * (1) if records contains new record (not contained in any cardelement) --> creates new cardelement for the record
//     * (2) if cardelements contain records which are not in records --> the corresponding cardelements are removed (i.e. cards removed, crs from stacks removed)
//     *
//     * @param records
//     */
//    public void synchronizeVisibleCardElements( List<CompoundRecord> records){
//         Map<CompoundRecord,CardElement> cr2ce = this.getMap_CompoundRecord2CE();
//         //Map<CardElement,Set<CompoundRecord>> ce2cr  = this.getMap_CE2CompoundRecord();
//
//        // find new records:
//        Set<CompoundRecord> newRecordsSet = new HashSet<>(records);
//        newRecordsSet.removeAll( cr2ce.keySet() );
//        for( CompoundRecord cr : newRecordsSet ){
//            this.addNewCardForRecord(cr);
//        }
//
//        // remove records:
//        Set<CompoundRecord> toRemoveRecordsSet = new HashSet<>(cr2ce.keySet());
//        this.removeCardElementsForRecords(new ArrayList<>(toRemoveRecordsSet));
//    }

    // public void addCardElement(CardElement ce){
    //     this.getCardElements().add(ce);
    // }






    /**
     * Set whether dragging via mouse is possible
     * @param functionality
     */
    public void setDraggingFunctionality(int functionality){
        this.mDraggingFunctionality = functionality;
    }

    /**
     * Activates / deactivates zoom via mouse
     * @param zoomEnabled
     */
    public void setZoomEnabled(boolean zoomEnabled){
        this.mZoomEnabled = zoomEnabled;
    }






    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if(this.mZoomEnabled) {
            //this.zoom(e.getWheelRotation());

            // check if we have MaxZoomOutFactor set and if yes we compute if we can zoom out further:
            if(this.mMaxZoomOutFactor > 0 && e.getWheelRotation() > 0 ){

                List<CardElement> allElements = new ArrayList<>( this.getAllCardElements() );
                DoubleSummaryStatistics dss_x = allElements.stream().mapToDouble( ce -> ce.getPosX() ).summaryStatistics();
                DoubleSummaryStatistics dss_y = allElements.stream().mapToDouble( ce -> ce.getPosY() ).summaryStatistics();

                double wx = dss_x.getMax()-dss_x.getMin();
                double wy = dss_y.getMax()-dss_y.getMin();

                // set minimal wx/wy..
                wx = Math.max( wx , this.getCardDrawer().getCardDrawingConfig().getCardWidth() * 8 );
                wy = Math.max( wy , this.getCardDrawer().getCardDrawingConfig().getCardHeight() * 8 );

                if(this.getViewport().getWidth() > mMaxZoomOutFactor * wx && this.getViewport().getHeight() > mMaxZoomOutFactor * wy ){
                    return;
                }

            }

            Point2D p = null;
            Point   p1 =null;
            try {
                p = this.getAffineTransform().inverseTransform(new Point2D.Double(e.getX(), e.getY()), null);
                //p  = this.getAffineTransform().transform(new Point2D.Double(e.getX(), e.getY()), null);
                p1 = new Point((int) p.getX(), (int) p.getY());
                this.zoomTowards(e.getWheelRotation(),p1.getX(),p1.getY() );
            }
            catch(Exception exc){}


        }
    }



    @Override
    public void mouseClicked(MouseEvent e) {

        // set as active view..
        if( getCardView().getViewSelectionHelper() != null) {
            getCardView().getViewSelectionHelper().setSelectedView(getCardView());
        }

        if( e.getButton() == MouseEvent.BUTTON1 ){
            if(e.getClickCount()==2){
                if(this.getHighlightedCardElement()!=null){
                    if(this.getHighlightedCardElement().isStackAfterExclusion()){

                        // can only open if it is not yet opened..
                        if(!mOpenedStacks.contains(this.getHighlightedCardElement())){
                            mOpenedStacks.add(this.getHighlightedCardElement());
                            createStackDialog(this.getHighlightedCardElement(),e);
                        }
                    }
                }
            }
        }

    }

    /**
     *
     * @return last clicked point in cardpane coordinates
     */
    public Point2D getLastClickedPoint(){
        return this.mLastClickedPointTransformed;
    }

    @Override
    public void mousePressed(MouseEvent e) {

        if(myMousePressedStatus){
            if(logLevel>0) {
                System.out.println("ERROR myMousePressedStatus");
            }
        }
        myMousePressedStatus = true;


        this.mDragStartX = e.getX();
        this.mDragStartY = e.getY();
        this.mDragStartViewportCenterX = this.mViewportCenterX;
        this.mDragStartViewportCenterY = this.mViewportCenterY;

        Point2D p = null;
        Point   p1 =null;
        try {
            p  = this.getAffineTransform().inverseTransform(new Point2D.Double(e.getX(), e.getY()), null);
            //p  = this.getAffineTransform().transform(new Point2D.Double(e.getX(), e.getY()), null);
            p1 = new Point((int) p.getX(),(int) p.getY());
            this.mDragStartTransformed = p;
            if((System.getProperty("development") != null) && flagDebug_outputMouseListeners){ System.out.println("pressed at : "+p1.getX()+"/"+p1.getY());}
            Rectangle2D vp = this.getViewport();
            if((System.getProperty("development") != null) && flagDebug_outputMouseListeners){ System.out.println("viewport   : "+vp.getX()+"/"+vp.getY()+" , w="+vp.getWidth()+" h="+vp.getHeight()); }
            this.mLastClickedPointTransformed = (Point2D) p.clone();
        }
        catch(Exception exc){};



        if(e.getButton() == MouseEvent.BUTTON1) {


            boolean ctrlDown = e.isControlDown(); //(e.getModifiers() & MouseEvent.CTRL_MASK) != 0;

            // ctrl is used to move the pane
            if(ctrlDown){
                CardElement ce_intersected = testIntersection_safe(p1);

                if(ce_intersected==null){
                    this.mDragMode = MOUSE_MODE_DRAG_VIEWPORT;
                    return;
                }
                else {
                    List<CardElement> new_selection = new ArrayList<>(getSelection());
                    // add / remove card from/to selection..
                    if( new_selection.contains(ce_intersected) ) {
                        new_selection.remove(ce_intersected);
                        this.setSelection(new_selection);
                    }
                    else{
                        new_selection.add(ce_intersected);
                        this.setSelection(new_selection);
                    }
                    return;
                }
            }

            // determine if we hit a cardelement (i.e. the top card element)
            //boolean hitCard = this.getHighlightedCardElement() != null; // DONT DO IT LIKE THIS!!
            CardElement ce_intersected = testIntersection_safe(p1);

            if(!mCardPaneModel.getAllElements().contains(ce_intersected)){
                //System.out.println("Catastrophic error.. we hit card element that is not in card model..");
            }

            boolean hitCard = ce_intersected!= null;

            // determine if we (also) hit a selection:
            //boolean hitSelection = this.mSelectedElements.contains( this.getHighlightedCardElement() );
            boolean hitSelection = this.mSelectedElements.contains( ce_intersected );

            // switch back to hitCard if selection is a single card..
            if(hitSelection){
                if(this.mSelectedElements.size()==1){
                    hitSelection = false;

                    // set reference row (?)
                    if(this.mSelectedElements.get(0).getNonexcludedRecords().size()==1) {
                        getCardPaneModel().getTableModel().setActiveRow(mSelectedElements.get(0).getNonexcludedRecords().get(0));
                    }
                }
            }

            if (hitCard && !hitSelection) {
                //System.out.println("start dragging card.. "+this.getHighlightedCardElement() );
                //this.setDraggedCardElement(this.getHighlightedCardElement());
                if((System.getProperty("development") != null) && flagDebug_outputMouseListeners){  System.out.println("start dragging card.. "+ce_intersected ); }
                this.setDraggedCardElement(ce_intersected);
                this.mDragMode = MOUSE_MODE_DRAG_CARDELEMENT;
                ArrayList<CardElement> sList = new ArrayList<>();
                //sList.add(this.getHighlightedCardElement());
                sList.add(ce_intersected);
                this.setSelection( sList );

                if(this.mOnlyAllowSnapAreaPositions){
                    //this.mOnlyAllowSnapAreasOriginalDragPosition = new Point2D.Double( this.getHighlightedCardElement().getPosX(), this.getHighlightedCardElement().getPosY() );
                    this.mOnlyAllowSnapAreasOriginalDragPosition = new Point2D.Double( ce_intersected.getPosX(), ce_intersected.getPosY() );
                }

            }
            else if(hitCard && hitSelection){
                this.startDraggingSelection();
                this.mDragMode = MOUSE_MODE_DRAG_SELECTION;
            }
            else if (e.isControlDown()) {
                mDragMode = MOUSE_MODE_DRAG_VIEWPORT;
                //mDragMode = MOUSE;
            } else if (e.isAltDown()) {
                mDragMode = MOUSE_MODE_SELECTION_RECT;
                mRectangularSelectionAnchor = (Point2D) p1.clone();
            }
            //else if (mHighlightedLabelPosition != null) {
            //    mDragMode = DRAG_MODE_MOVE_LABEL;
            //}
            else {
                mDragMode = MOUSE_MODE_SELECTION_LASSO;
                mLassoRegion = new Polygon();
                mLassoRegion.addPoint( (int)p1.getX(), (int)p1.getY() );
                mLassoRegion.addPoint( (int)p1.getX(), (int)p1.getY() );
            }
        }

        if(e.getButton() == MouseEvent.BUTTON3) {
            startPopupTimer(e);
            this.mDragMode = MOUSE_MODE_DRAG_VIEWPORT;
//            // determine if we hit a cardelement
//            boolean hitCard = this.getHighlightedCardElement() != null;
//
//            if(hitCard) {
//                if(this.mCardView!=null) {
//                    this.mCardView.handlePopupTrigger(e,this.getSelection());
//                }
//
//            } else {
//                // start dragging the viewport:
//                this.mDragMode = MOUSE_MODE_DRAG_VIEWPORT;
//            }
        }
    }

    MouseEvent mMouseEventDelayedPopupMenuTrigger = null;

    /**
     * If e is null, then we take the mMouseEventDelayedPopupMenuTrigger instead.
     *
     * @param e
     */
    public void showPopupMenu(MouseEvent e){
        if(e==null){
            e = mMouseEventDelayedPopupMenuTrigger;
        }

        boolean hitCard = this.getHighlightedCardElement() != null;
        if(hitCard) {
            if (this.mCardView != null) {
                this.mCardView.handlePopupTrigger(e, this.getSelection());
            }
        }
        else{
            if (this.mCardView != null) {
                this.mCardView.handlePopupTrigger(e, new ArrayList<>());
            }
        }
    }


    /**
     * Returns the visible card elements from the card pane model.
     * Visible means, it is currently intersecting the viewport (the "safeViewport", to be precise).
     *
     * @return
     */
    public List<CardElement> getVisibleCardElements(){
        Rectangle2D viewport = this.getViewport();

        double safetyW = Math.max( 0.1 * viewport.getWidth() , 1.1 * this.getCardDrawer().getCardDrawingConfig().getCardWidth() );
        double safetyH = Math.max( 0.1 * viewport.getHeight() , 1.1 * this.getCardDrawer().getCardDrawingConfig().getCardHeight() );
        Rectangle2D safeViewport = new Rectangle((int) (viewport.getX()-safetyW),(int) ( viewport.getY()-safetyH),(int) (viewport.getWidth()+2*safetyW), (int) (viewport.getHeight()+2*safetyH) );

        //return this.mCardPaneModel.getAllElements().stream().filter(xce -> xce.getRectangle().intersects( vp )).collect(Collectors.toList());
        //return this.mCardPaneModel.getAllElements().stream().filter(xce -> !xce.isEmptyAfterExclusion() ).filter(xce -> xce.getRectangle().intersects( vp )).collect(Collectors.toList());
        //return this.mCardPaneModel.getAllElements().stream().filter(xce -> !xce.isEmptyAfterExclusion() ).filter(xce -> xce.getRectangle().intersects( vp )).collect(Collectors.toList());
        return this.mCardPaneModel.getReducedAllElements().stream().filter(xce -> xce.getRectangle().intersects( safeViewport )).collect(Collectors.toList());
    }

    /**
     * Returns the visible card elements from the card pane model which show currently stacks.
     * Visible means, it is currently intersecting the viewport (the "safeViewport", to be precise).
     *
     * @return
     */
    public List<CardElement> getVisibleStacks(){
        Rectangle2D viewport = this.getViewport();

        double safetyW = Math.max( 0.1 * viewport.getWidth() , 1.1 * this.getCardDrawer().getCardDrawingConfig().getCardWidth() );
        double safetyH = Math.max( 0.1 * viewport.getHeight() , 1.1 * this.getCardDrawer().getCardDrawingConfig().getCardHeight() );
        Rectangle2D safeViewport = new Rectangle((int) (viewport.getX()-safetyW),(int) ( viewport.getY()-safetyH),(int) (viewport.getWidth()+2*safetyW), (int) (viewport.getHeight()+2*safetyH) );
        return this.mCardPaneModel.getReducedAllElements().stream().filter(xce -> xce.isStackAfterExclusion()).filter(xce -> xce.getRectangle().intersects( safeViewport )).collect(Collectors.toList());
    }



    public List<CardElement> getAllCardElements(){
        return this.mCardPaneModel.getAllElements();
    }

    boolean myMousePressedStatus = false;

    @Override
    public void mouseReleased(MouseEvent e) {


        if(!myMousePressedStatus){
            if(logLevel>0) {
                System.out.println("ERROR mouse released while not pressed..");
            }
        }
        myMousePressedStatus = false;


        if(DEBUG_validateRecordset){
            DEBUG_AllRecords = new ArrayList<>(); mCardPaneModel.getAllElements().stream().flatMap( ei -> ei.getAllRecords().stream() ).forEach( ci -> DEBUG_AllRecords.add(ci));
            DEBUG_AllRecords.sort( (ca,cb) -> Integer.compare(ca.getID(),cb.getID()) );
        }

        if((System.getProperty("development") != null) && flagDebug_outputMouseListeners){  System.out.println("mouse released!"); }

        if(this.mPopupThread != null){
            // show the popup menu now:
            this.mPopupThread = null;
            showPopupMenu(e);
        }

        if(mDragMode==MOUSE_MODE_DRAG_CARDELEMENT) {
            if(this.mOnlyAllowSnapAreaPositions){
                if( this.mProposedSnapArea == null ){
                    // reset snap event..
                    this.mDraggedCardElement.setPosX( this.mOnlyAllowSnapAreasOriginalDragPosition.getX() );
                    this.mDraggedCardElement.setPosY( this.mOnlyAllowSnapAreasOriginalDragPosition.getY() );

                    this.fireCardPaneEvent(  new CardPaneEvent( CardPaneEvent.EventType.RESET_SNAP_EVENT , this.mDraggedCardElement , null ) );
                }
                else{
                    // final_snap_event:
                    this.fireCardPaneEvent( new CardPaneEvent( CardPaneEvent.EventType.FINAL_SNAP_EVENT , this.mDraggedCardElement , this.mProposedSnapArea ) );
                    this.mProposedSnapArea = null;
                }
            }
        }
        if(mDragMode==MOUSE_MODE_SELECTION_LASSO) {
            //mSelectedElements.clear();
            List<CardElement> selectedElements = new ArrayList<>();
            for (CardElement ce : getVisibleCardElements()) {
                boolean isSelected = mLassoRegion.contains( ce.getRectangle() );
                if(isSelected){
                    selectedElements.add(ce);
                }
            }
            this.setSelection(selectedElements);
        }
        if(mDragMode==MOUSE_MODE_SELECTION_RECT) {
            double x0 = Math.min( mRectangularSelectionAnchor.getX() , mRectangularSelectionP2.getX() );
            double x1 = Math.max( mRectangularSelectionAnchor.getX() , mRectangularSelectionP2.getX() );
            double y0 = Math.min( mRectangularSelectionAnchor.getY() , mRectangularSelectionP2.getY() );
            double y1 = Math.max( mRectangularSelectionAnchor.getY() , mRectangularSelectionP2.getY() );
            Rectangle2D rselect = new Rectangle2D.Double(x0,y0,x1-x0,y1-y0);

            List<CardElement> selectedElements = new ArrayList<>();
            for (CardElement ce : getVisibleCardElements()) {
                boolean isSelected = rselect.contains( ce.getRectangle() );
                if(isSelected){
                    selectedElements.add(ce);
                }
            }
            this.setSelection(selectedElements);
        }


        if((System.getProperty("development") != null) && flagDebug_outputMouseListeners){  System.out.println("clear proposed stack element, as we receive a mouse release event"); }
        this.setDraggedCardElement(null);
        this.setProposedStackElement(null);
        this.mOnlyAllowSnapAreasOriginalDragPosition = null;

        this.mDragMode = MOUSE_MODE_NONE;


        if(DEBUG_validateRecordset){
            List<CompoundRecord> testAllRecords = new ArrayList<>(); mCardPaneModel.getAllElements().stream().flatMap( ei -> ei.getAllRecords().stream() ).forEach( ci -> testAllRecords.add(ci));
            testAllRecords.sort( (ca,cb) -> Integer.compare(ca.getID(),cb.getID()) );

            if(! testAllRecords.equals(DEBUG_AllRecords) ){
                System.out.println("discrepancy..");
            }

            HashSet<CompoundRecord> testset = new HashSet<>(DEBUG_AllRecords);
            if(testset.size() != testAllRecords.size()){
                System.out.println("duplicates..");
            }

        }

    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // start dnd action, if we are currently dragging something..
        if(this.mDragMode == MOUSE_MODE_DRAG_CARDELEMENT || this.mDragMode == MOUSE_MODE_DRAG_SELECTION) {
            if(logLevel>1) {
                System.out.println("[DND] --> Initiate Drag!");
            }
            List<CardElement> ce_dragged = this.getSelection();
            this.getTransferHandler().exportAsDrag(this, e, TransferHandler.MOVE);

            this.mDragMode = MOUSE_MODE_NONE;
            this.myMousePressedStatus = false;
        }
    }





    boolean DEBUG_validateRecordset = true;
    private List<CompoundRecord> DEBUG_AllRecords;


    @Override
    public void mouseDragged(MouseEvent e) {

//        if(m_DD_PhantomCard) {
//            try {
//                Point2D p = this.getAffineTransform().inverseTransform(new Point2D.Double(e.getX(), e.getY()), null);
//                //p  = this.getAffineTransform().transform(new Point2D.Double(e.getX(), e.getY()), null);
//                Point2D p1 = new Point((int) p.getX(), (int) p.getY());
//
//                m_DD_PhantomCard_x = p1.getX();
//                m_DD_PhantomCard_y = p1.getY();
//                repaint();
//            }
//            catch(Exception ex) {
//                ex.printStackTrace();
//            }
//        }
//        else{
//            m_DD_PhantomCard_x = Double.NaN;
//            m_DD_PhantomCard_y = Double.NaN;
//        }


        if(!myMousePressedStatus) {
            if(logLevel>0) {
                System.out.println("ERROR, dragged, while mouse is not pressed..");
            }
            return;
        }

        if(mDragMode==MOUSE_MODE_NONE) {
            if(logLevel>1) {
                System.out.println("mouseDragged, but we are not currently dragging anything.. just return");
            }
            return;
        }

        if(DEBUG_validateRecordset){
            DEBUG_AllRecords = new ArrayList<>(); mCardPaneModel.getAllElements().stream().flatMap( ei -> ei.getAllRecords().stream() ).forEach( ci -> DEBUG_AllRecords.add(ci));
            DEBUG_AllRecords.sort( (ca,cb) -> Integer.compare(ca.getID(),cb.getID()) );
        }

        // cancel delayed popup menu:
        this.mPopupThread = null;

        Point2D p = null;
        try {
            p = this.getAffineTransform().inverseTransform(new Point2D.Double(e.getX(), e.getY()), null);
        }
        catch(Exception exc){};

        if(this.mDragMode==MOUSE_MODE_DRAG_VIEWPORT) {
            double dx = 0;
            double dy = 0;
            if(this.mDraggingFunctionality==MOUSE_DRAGGING_FUNCTIONALITY_DISABLED){

            }
            else {
                if (this.mDraggingFunctionality == MOUSE_DRAGGING_FUNCTIONALITY_NORMAL ) {
                    dx = -(e.getX() - mDragStartX) / getZoomFactor();
                    dy = -(e.getY() - mDragStartY) / getZoomFactor();
                }
                if (this.mDraggingFunctionality == MOUSE_DRAGGING_FUNCTIONALITY_ONLY_HORIZONTAL ){
                    dx = -(e.getX() - mDragStartX) / getZoomFactor();
                }
                if(this.mDraggingFunctionality == MOUSE_DRAGGING_FUNCTIONALITY_ONLY_VERTICAL ){
                    dy = -(e.getY() - mDragStartY) / getZoomFactor();
                }
            }

            this.setViewport(mDragStartViewportCenterX + dx, mDragStartViewportCenterY + dy, this.getZoomFactor());
            //this.moveViewport(mDragStartViewportCenterX + dx, mDragStartViewportCenterY + dy);
            //this.moveViewport(0.1*dx, 0.1*dy);
            repaint();
        }

        if( this.mDragMode==MOUSE_MODE_DRAG_CARDELEMENT   ) {
            //System.out.println("drag card..");

            double dx = (e.getX() - mDragStartX) / getZoomFactor();
            double dy = (e.getY() - mDragStartY) / getZoomFactor();
            CardElement de = this.getDraggedCardElement();
            de.setCenter(mDraggedCardStartX + dx, mDraggedCardStartY + dy);
            //repaint();

            if(m_DD_PhantomCard) {
                m_DD_PhantomCard_x = mDraggedCardStartX + dx;
                m_DD_PhantomCard_y = mDraggedCardStartY + dy;
                repaint();
            }
            else{
                m_DD_PhantomCard_x = Double.NaN;
                m_DD_PhantomCard_y = Double.NaN;
            }

            //Point2D p = this.getAffineTransform().transform(new Point2D.Double(e.getX(), e.getY()), null);

            // check if we are close to other stack..
            //CardElement intersectedStack = testIntersectionWithCardElement(de,true);
            CardElement intersectedStack_proto = testIntersectionWithCardElement_fast(de,true);

            // prohibit intersection with opened stacks!
            if( getOpenedStacks().contains( intersectedStack_proto ) ) { intersectedStack_proto= null; }
            CardElement intersectedStack = intersectedStack_proto;

            if (intersectedStack == de) {
                System.out.println("ERROR!!! intersected stack = de");
            }

            if(logLevel>1) {
                System.out.println("drag..");
            }

            if (intersectedStack != null && mProposeStacks ) {

                //if (intersectedStack == this.getProposedStackElement()) {
                if (intersectedStack==this.getProposedStackElement() ) {
                    if(logLevel>1) {
                        System.out.println("old");
                    }
                    // nothing to do
                } else if( de.getAllNonexcludedRecords().stream().filter( cdi -> intersectedStack.getAllNonexcludedRecords().contains( cdi  ) ).count() > 0) {
                    if(logLevel>1) {
                        System.out.println("old2 and wrong..");
                    }
                    // no idea how we ended up here, but we resolve it this way..
                }
                else
                {
                    if (this.getProposedStackElement() != null) {
                        if(logLevel>1) {
                            System.out.println("switch stack");
                        }

                        if(true){
                            this.mCardPaneModel.removeRecordsFromCE(this.getProposedStackElement(), de.getAllRecords());
                            this.mCardPaneModel.addRecordsToCE(intersectedStack, de.getAllRecords());
                        }
                        else {
                            // remove card from old proposed stack, and add to new proposed stack
                            this.mCardPaneModel.removeRecordsFromCE(this.getProposedStackElement(), de.getAllRecords());
                            this.mCardPaneModel.addRecordsToCE(intersectedStack, de.getAllRecords());
                        }
                        setCardElementToFront(intersectedStack);
                        this.setProposedStackElement(intersectedStack);
                        //this.recomputeHiddenCardElements(false);
                        fireCardPaneEvent( new CardPaneEvent( CardPaneEvent.EventType.VISIBLE_ELEMENTS_CHANGED , this.getViewport() ) );

                    } else {

                        if(logLevel>1) {
                            System.out.println("new stack");
                            System.out.println("de_size: " + de.getAllNonexcludedRecords().size() + "  intersectedStack size: " + intersectedStack.getAllNonexcludedRecords().size());
                        }
                        // add card to proposed stack, and remove de from card elements:

                        //intersectedStack.getRecords().addAll(de.getRecords());
                        //intersectedStack.addRecords(de.getRecords() );

                        if(true){

                            boolean didRemove = this.getCardPaneModel().removeCE(de);
                            this.getCardPaneModel().addRecordsToCE(intersectedStack,de.getAllRecords());

                            if(!didRemove){
                                System.out.println("ERROR in stack proposal code.. could not remove dragged card element?!");
                            }
//                            List<CardElement> toStack = new ArrayList<>();
//                            toStack.add(de); toStack.add(intersectedStack);
//                            intersectedStack = this.getCardPaneModel().createStack(toStack, intersectedStack.getPosX(),intersectedStack.getPosY() ,true);
//                            this.setProposedStackElement(intersectedStack);

//List<CardElement> ces_for_stack = new ArrayList<>();
//                            ces_for_stack.add(de);
//                            ces_for_stack.add(intersectedStack);
//                            int numA = this.getCardPaneModel().getAllElements().size();
//                            this.getCardPaneModel().createStack( ces_for_stack , intersectedStack.getPosX() , intersectedStack.getPosY() ,true);
//                            int numB = this.getCardPaneModel().getAllElements().size();
//                            System.out.println("CARDS A: " + numA + "  CARDS B: " + numB);
                        }
                        else {
                            this.mCardPaneModel.addRecordsToCE(intersectedStack, de.getAllRecords());
                            //System.out.println("remove de");
                            //this.getCardElements().remove(de);
                            if (!this.mCardPaneModel.getAllElements().contains(de)) {
                                System.out.println("ERROR!!! in stack proposal code");
                            }
                            int numA = this.mCardPaneModel.getAllElements().size();
                            this.mCardPaneModel.removeCE(de);
                            int numB = this.mCardPaneModel.getAllElements().size();

                            if(logLevel>1) {
                                System.out.println("CARDS A: " + numA + "  CARDS B: " + numB);
                            }
                            //this.recomputeHiddenCardElements(false);
                        }
                        setCardElementToFront(intersectedStack);
                        this.setProposedStackElement(intersectedStack);
                        fireCardPaneEvent( new CardPaneEvent( CardPaneEvent.EventType.VISIBLE_ELEMENTS_CHANGED , this.getViewport() ) );
                    }
                }
            } else {
                //System.out.println("no intersection..");

                // check if we have to undo a proposed stacking:
                if (this.getProposedStackElement() != null) {
                    if(logLevel>1) {
                        System.out.println("undo proposed stack..");
                    }
                    //System.out.println("clean up");
                    // clean up and restore de card:

                    //this.getProposedStackElement().getRecords().removeAll(de.getRecords());
                    //this.getProposedStackElement().removeRecords(de.getRecords());
                    this.mCardPaneModel.removeRecordsFromCE(  this.getProposedStackElement() , de.getAllRecords() );

                    //this.getCardElements().add(de);
                    this.mCardPaneModel.addCE(de);

                    this.setProposedStackElement(null);
                    // set card to foreground again..
                    this.setCardElementToFront( de );

                    fireCardPaneEvent( new CardPaneEvent( CardPaneEvent.EventType.VISIBLE_ELEMENTS_CHANGED , this.getViewport() ) );
                } else {
                    //System.out.println("no stack to undo..");
                    this.setProposedStackElement(null);
                }
            }

            if( this.mCheckForSnapAreas ){
                if(this.mProposedSnapArea!=null){
                    // check if we leave it:
                    if(!this.mProposedSnapArea.getSnapArea().contains(p)) {
                        this.mProposedSnapArea = null;
                        mDraggedCardElement.setPosX(p.getX());
                        mDraggedCardElement.setPosY(p.getY());
                    }
                }

                if(this.mSnapAreas != null) {

                    final Point2D p_final = p;
                    // check if we're in snap area range:
                    List<SnapArea> saList = this.mSnapAreas.parallelStream().filter(sa -> sa.mSnapArea.contains(p_final)).collect(Collectors.toList());
                    if (saList.isEmpty()) {
                        // nothing to do..
                    } else {
                        if (saList.size() > 1) {
                            System.out.println("warning: overlapping snap areas , take the first one");
                        }

                        SnapArea sa = saList.get(0);
                        this.mProposedSnapArea = sa;
                        mDraggedCardElement.setPosX(sa.getSnapPoint().getX());
                        mDraggedCardElement.setPosY(sa.getSnapPoint().getY());

                        // and compute the reordering:
                        Map<CardElement, Point2D> reordering = sa.mReordering.apply(mDraggedCardElement);

                        List<CardElement> roCEs = new ArrayList<>();
                        List<Point2D> roPos = new ArrayList<>();
                        for (Map.Entry<CardElement, Point2D> mei : reordering.entrySet()) {
                            roCEs.add(mei.getKey());
                            roPos.add(mei.getValue());
                        }

                        // animate to the new positions:
                        this.startAnimation(PathAnimator.createToPositionPathAnimator(roCEs, roPos, 0.15));

                        // FIRE EVENTS..
                        this.fireCardPaneEvent( new CardPaneEvent( CardPaneEvent.EventType.PROPOSAL_SNAP_EVENT ) );
                    }
                }
            }

        }

        if(this.mDragMode==MOUSE_MODE_DRAG_SELECTION) {
            double dx = (e.getX() - mDragStartX) / getZoomFactor();
            double dy = (e.getY() - mDragStartY) / getZoomFactor();
            //CardElement de = this.getDraggedCardElement();

            int ce_cnt = 0;
            for( CardElement cei : this.mSelectedElements) {
                cei.setCenter(mDraggedSelectionStartCoordinates.get(ce_cnt).getX() + dx, mDraggedSelectionStartCoordinates.get(ce_cnt).getY() + dy);
                ce_cnt++;
            }
        }

        if(mDragMode == MOUSE_MODE_SELECTION_LASSO) {
            mLassoRegion.addPoint((int)p.getX(),(int)p.getY());
            //System.out.println("n lasso: "+mLassoRegion.npoints);
        }

        if(mDragMode == MOUSE_MODE_SELECTION_RECT) {
            mRectangularSelectionP2 = (Point2D) p.clone();

        }

        if(DEBUG_validateRecordset){
            List<CompoundRecord> testAllRecords = new ArrayList<>(); mCardPaneModel.getAllElements().stream().flatMap( ei -> ei.getAllRecords().stream() ).forEach( ci -> testAllRecords.add(ci));
            testAllRecords.sort( (ca,cb) -> Integer.compare(ca.getID(),cb.getID()) );

            if(! testAllRecords.equals(DEBUG_AllRecords) ){
                System.out.println("Suspicious event in stack proposal code.. discrepancy.. size_old="+DEBUG_AllRecords.size()+ " size_new="+testAllRecords.size());
            }
            HashSet<CompoundRecord> testset = new HashSet<>(DEBUG_AllRecords);
            if(testset.size() != testAllRecords.size()){
                System.out.println("Suspicious event in stack proposal code.. duplicates?..");
            }

        }


        repaint();
    }



    @Override
    public void mouseMoved(MouseEvent e) {

        //System.out.println("MMM");

        if(myMousePressedStatus){
            if(logLevel>0) {
                System.out.println("ERROR moved while pressed..");
            }
            return;
        }

        Point2D p = null;
        try {
            p = this.getAffineTransform().inverseTransform(e.getPoint(), null);
        }
        catch(Exception exc){}


        // test if we hit card:
        //CardElement old_card = this.getHighlightedCardElement();
        CardElement card = null;

        for(CardElement ce : this.getVisibleCardElements() ){
            //for(CardElement ce : this.getNonExcludedCardElements()){
            if( ce.getRectangle().contains( p ) ){
                //System.out.println("HIT!");
                if(card==null){card = ce;}
                else{
                    if( ce.getZ() > card.getZ() ){
                        card = ce;
                    }
                }
            }
        }

        this.setHighlightedCardElement(card);
        //if(card!=null){ System.out.println("selected z: "+card.getZ()); }
    }



    public void startAnimation(AnimatorInterface animator){
        AnimatorThread at = new AnimatorThread(this,animator);
        Thread waitForAnimation = new Thread(){
            public void run(){ try{ at.join();}catch(Exception e){};fireCardElementChanged( new ArrayList<>( mCardPaneModel.getAllElements() ) ); }
        };
        at.start();
        waitForAnimation.start();
    }



    public void startAnimateViewportToShowAllCards() {


        DoubleSummaryStatistics dss_x = this.getCardPaneModel().getReducedAllElements().stream().mapToDouble(cxi -> cxi.getPosX() ).summaryStatistics();
        DoubleSummaryStatistics dss_y = this.getCardPaneModel().getReducedAllElements().stream().mapToDouble(cxi -> cxi.getPosY() ).summaryStatistics();

        double bx[] = new double[] {dss_x.getMin(),dss_x.getMax()};
        double by[] = new double[] {dss_y.getMin(),dss_y.getMax()};

        double cx = (bx[0] + bx[1]) * 0.5;
        double cy = (by[0] + by[1]) * 0.5;

        double dx = bx[1] - bx[0];
        double dy = by[1] - by[0];

        double zoom = Math.min(this.getWidth() / dx, this.getHeight() / dy);

        // check if we actually have a size greater than zero. Otherwise assume something..
        if(this.getWidth()<=0 || this.getHeight()<=0) {
            zoom =Math.min(200 / dx, 200 / dy);
        }

        this.startAnimateViewportToNewPosition(cx, cy, zoom, 0.5);
    }


    private Object mLock_mThreadAnimateViewport = new Object();
    private Thread mThreadAnimateViewport = null;

    /**
     * NOTE! The animation thread does call only the trySetViewport function,
     * which can be overruled by actual concurrent calls to the setViewport function!
     * I.e. a call to setViewport while an animation is running will stop the given
     * animation and just set the viewport.
     *
     * @param centerX
     * @param centerY
     * @param zoom
     * @param time
     */
    public void startAnimateViewportToNewPosition(double centerX, double centerY, double zoom, double time){

        // cancel potentially already running animate threads:
        synchronized (mLock_mThreadAnimateViewport) {
            if(mThreadAnimateViewport != null) {
                mThreadAnimateViewport.interrupt();
                try {
                    mThreadAnimateViewport.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mThreadAnimateViewport = null;
            }
        }

        long timestampStart = System.currentTimeMillis();
        double oldCX = this.getViewportCenterX();
        double oldCY = this.getViewportCenterY();
        double oldZoom = this.getZoomFactor();

        Thread runAnimation = new Thread(){
            @Override
            public void run( ) {
                while( System.currentTimeMillis() < timestampStart + (long) ( time*1000 ) ) {
                    if(Thread.interrupted()) {
                        setViewport(centerX,centerY,zoom);
                        mThreadAnimateViewport = null;
                        return;
                    }

                    double t = ((System.currentTimeMillis() - timestampStart) * 0.001);
                    double r = t / time;

                    double ccx = oldCX + r * (centerX - oldCX);
                    double ccy = oldCY + r * (centerY - oldCY);
                    double zz = oldZoom + r * (zoom - oldZoom);
                    setViewport(ccx, ccy, zz);
                    repaint();
                    try{Thread.sleep(10);}catch(Exception e){}
                }
                setViewport(centerX,centerY,zoom);
                mThreadAnimateViewport = null;
            }
        };
        synchronized(mLock_mThreadAnimateViewport) {
            runAnimation.start();
            mThreadAnimateViewport = runAnimation;
        }
    }




    /**
     *
     * Tests intersection wiht all visible card elements.
     *
     * @param ce
     * @return
     */
    public CardElement testIntersectionWithCardElement(CardElement ce, boolean fireIntersectionEvents ) {
        long timestamp = System.currentTimeMillis();
        CardElement result = null;

        for(CardElement ci : this.getVisibleCardElements()){
            //for(CardElement ci : this.getNonExcludedCardElements()){
            //if(ce==ci){ if(fireIntersectionEvents){this.fireCardElementChanged(ci); continue;} }

            if(ce.equals(ci)){
                if(fireIntersectionEvents){
                    this.fireCardElementChanged(ci); continue;
                }
            }

            //if(ce==ci){continue;}
            if(ce.getRectangle().intersects(ci.getRectangle())) {
                if(fireIntersectionEvents){this.fireCardElementChanged(ci);}

                Rectangle2D intersection = ci.getRectangle().createIntersection(ce.getRectangle());
                if( intersection.getWidth()*intersection.getHeight() >= STACKING_REL_OVERLAP_REQ * ce.getRectangle().getWidth()*ce.getRectangle().getHeight() ){
                    //System.out.println("intersection!");
                    result = ci;
                }
            }
        }

        if(logLevel>1) {
            System.out.println("[JCardPane::testIntersectionWithCardElement] --> done in " + (System.currentTimeMillis() - timestamp) + " ms");
        }

        return result;
    }

    /**
     *
     * Tests intersection wiht all visible card elements.
     *
     * @param ce_r
     * @return
     */
    public CardElement testIntersectionWithCardElement(Rectangle ce_r, boolean fireIntersectionEvents ){

        long timestamp = System.currentTimeMillis();
        CardElement result = null;

        for(CardElement ci : this.getVisibleCardElements()){
            //for(CardElement ci : this.getNonExcludedCardElements()){
            //if(ce==ci){ if(fireIntersectionEvents){this.fireCardElementChanged(ci); continue;} }

            //if(ce==ci){continue;}
            if(ce_r.intersects(ci.getRectangle())) {
                if(fireIntersectionEvents){this.fireCardElementChanged(ci);}

                Rectangle2D intersection = ci.getRectangle().createIntersection(ce_r);
                if( intersection.getWidth()*intersection.getHeight() >= STACKING_REL_OVERLAP_REQ * ce_r.getWidth() * ce_r.getHeight() ){
                    //System.out.println("intersection!");
                    result = ci;
                }
            }
        }

        if(logLevel>1) {
            System.out.println("[JCardPane::testIntersectionWithCardElement] --> done in " + (System.currentTimeMillis() - timestamp) + " ms");
        }

        return result;
    }

    /**
     *
     * Tests intersection with all visible card elements, uses the 2d binning
     *
     * @param ce
     * @return
     */
    public CardElement testIntersectionWithCardElement_fast(CardElement ce, boolean fireIntersectionEvents ){

        long timestamp = System.currentTimeMillis();
        CardElement result = null;

        //CardElement ci : this.getVisibleCardElements();
        List<CardElement> nearCards = mBinnedCards_Nonexcluded.getComputedObject().getAllElementsInNeighborhood(ce.getPosX(),ce.getPosY());

        //System.out.println("[JCardPane::testIntersectionWithCardElement_fast] -->  test "+nearCards.size()+" cards");

        for(CardElement ci : nearCards){
            //for(CardElement ci : this.getNonExcludedCardElements()){
            //if(ce==ci){ if(fireIntersectionEvents){this.fireCardElementChanged(ci); continue;} }

            if(ce.equals(ci)){
                if(fireIntersectionEvents){
                    this.fireCardElementChanged(ci); continue;
                }
            }

            //if(ce==ci){continue;}
            if(ce.getRectangle().intersects(ci.getRectangle())) {
                if(fireIntersectionEvents){this.fireCardElementChanged(ci);}

                Rectangle2D intersection = ci.getRectangle().createIntersection(ce.getRectangle());
                if( intersection.getWidth()*intersection.getHeight() >= STACKING_REL_OVERLAP_REQ * ce.getRectangle().getWidth()*ce.getRectangle().getHeight() ){
                    //System.out.println("intersection!");
                    result = ci;
                }
            }
        }

        //System.out.println("[JCardPane::testIntersectionWithCardElement_fast] --> done in "+(System.currentTimeMillis()-timestamp)+" ms");

        return result;
    }


    /**
     * Returns the top card element at the given position.
     *
     * @param p
     * @return
     */
    public CardElement testIntersection_safe(Point2D p) {
        List<CardElement> hit_cards = new ArrayList<>();
        synchronized(mCardPaneModel) {
            ArrayList<CardElement> ce_list = new ArrayList<>(mCardPaneModel.getReducedAllElements());
            hit_cards = ce_list.parallelStream().filter(ci -> ci.getRectangle().contains(p)).collect(Collectors.toList());
        }

        // sort descending
        hit_cards.sort( (CardElement ca , CardElement cb) -> -Integer.compare(ca.getZ(),cb.getZ()));

        if(!hit_cards.isEmpty()){
            return hit_cards.get(0);
        }

        return null;
    }



    /**
     *
     * Tests intersection with all visible card elements, uses the 2d binning
     *
     * Takes care for z sorting, i.e. it returns the top intersected card
     *
     * NOTE! This intersection check returns the results oaccording to its last computed status, i.e. there is
     * some probability that (in case that lots of things change fast) the result is WRONG for the given situation.
     *
     * @param p
     * @return
     */
    public CardElement testIntersection_fast(Point2D p) {
        List<CardElement> nearCards = mBinnedCards_Nonexcluded.getComputedObject().getAllElementsInNeighborhood(p.getX(),p.getY());

        // sort descending
        nearCards.sort( (CardElement ca , CardElement cb) -> -Integer.compare(ca.getZ(),cb.getZ()));

        for(CardElement ci : nearCards){
            if(ci.getRectangle().contains(p)){
                return ci;
            }

        }
        return null;
    }


    @Override
    public void keyTyped(KeyEvent e) {
        if(logLevel>1) {
            System.out.println("key typed: " + e.getKeyCode());
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if(logLevel>1) {
            System.out.println("key pressed: " + e.getKeyCode());
        }
        //if(e.getKeyCode()== KeyEvent.VK_SPACE){

        if(e.getKeyCode()==KeyEvent.VK_SPACE) {
            // determine grid size:
            int gx = (int) Math.ceil(Math.sqrt(mCardPaneModel.getAllElements().size()));
            int gy = (int) Math.ceil(Math.sqrt(mCardPaneModel.getAllElements().size()));

            PathAnimator pa = PathAnimator.createToGridPathAnimator(mCardPaneModel.getAllElements(), 1.0, gx, gy);
            startAnimation(pa);
        }


//        if(e.getKeyCode()==KeyEvent.VK_L) {
//            this.setRenderQuality(RENDER_QUALITY_LOW);
//            repaint();
//        }
//        if(e.getKeyCode()==KeyEvent.VK_H) {
//            this.setRenderQuality(RENDER_QUALITY_FULL);
//            repaint();
//        }

        //}
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }


    /**
     * if returns true, then the highlighted card element currently is a stack.
     *
     * @return
     */
    public boolean isMouseOverStack(){
        if(this.mHighlightedCardElement==null){
            return false;
        }
        return this.mHighlightedCardElement.isStackAfterExclusion();
    }

    public void setCardDrawer(AbstractCardDrawer cardDrawer){
        this.mCardDrawer = cardDrawer;
        this.mCardDrawer.getCardDrawingConfig().registerConfigChangeListener(this);
    }

    public void setStackDrawer(AbstractStackDrawer stackDrawer){
        this.mStackDrawer = stackDrawer;
    }


    private boolean mShowDelayedPopupMenu = true;

    public boolean showDelayedPopupMenu(){
        return mShowDelayedPopupMenu;
    }

    private void startPopupTimer(MouseEvent e) {
        mMouseEventDelayedPopupMenuTrigger = e;

        mPopupThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(RIGHT_MOUSE_POPUP_DELAY);
                    if (showDelayedPopupMenu())
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                if (mPopupThread != null)
                                    showPopupMenu(null); // we pass null, such that the mouse event that triggered the delayedPopupMenu is passed.
                            }
                        });
                } catch (InterruptedException ie) {}
            }
        } );
        mPopupThread.start();
    }




    public Map<CardElement,Set<CompoundRecord>> getMap_CE2CompoundRecord(){
        return this.mCardPaneModel.getMap_CE2CompoundRecord();
    }

    public Map<CompoundRecord,CardElement> getMap_CompoundRecord2CE(){
        return this.mCardPaneModel.getMap_CompoundRecord2CE();
    }

    public CompoundTableModel getCompoundTableModel(){
        return this.mTableModel;
    }


    private long mTimeStampLastHiddenCardsComputationStarted = -1;
    private boolean mHiddenCardsComputationRequested = false;
    private int mConfTimeIntervalHiddenCardsComputationMilliSeconds = 200;

    private WaitThenCompute mHiddenCardsComputeThread = null;

    public JCardPane getThis(){return this;}


    class WaitThenCompute extends Thread{
        long mMilliSecondsWait = 0;
        public WaitThenCompute(long milliSecondsWait){
            this.mMilliSecondsWait = milliSecondsWait;
        }
        @Override
        public void run() {
            try{Thread.sleep(mMilliSecondsWait);}catch(Exception e){}
            mHiddenCardsComputationRequested = false;
            mTimeStampLastHiddenCardsComputationStarted = System.currentTimeMillis();
            NewHiddenCardsComputeThread computation = new NewHiddenCardsComputeThread(getThis() );
            computation.run();
        }
    }

    private boolean mComputeHiddenCardElements = true;

    public void setComputeHiddenCardElementsEnabled(boolean enabled){
        this.mComputeHiddenCardElements = enabled;
        // set all to "to_compute" anyway..
        this.getAllCardElements().stream().forEach( ci -> ci.setHiddenStatus(CardElement.HIDDEN_STATUS_TO_COMPUTE_WAS_VISIBLE) );
    }

    public boolean isComputeHiddenCardsEnabled(){return mComputeHiddenCardElements;}

    /**
     *
     * @param recomputeAll sets all to a TO_COMPUTE state
     * @param setAllToNonhidden only has an effect if recomputeAll is true. Sets all cards to WAS_VISIBLE state instead
     *                          of setting this depending on the former state.
     */
    public synchronized void recomputeHiddenCardElements( boolean recomputeAll , boolean setAllToNonhidden ){

        if(!mComputeHiddenCardElements){
            return;
        }

        if(recomputeAll) {
            //System.out.println("request recompute hidden card elements");
            if (setAllToNonhidden) {
                this.mCardPaneModel.getAllElements().forEach(ce -> ce.setHiddenStatus(CardElement.HIDDEN_STATUS_TO_COMPUTE_WAS_VISIBLE));
            } else {
                this.mCardPaneModel.getAllElements().forEach(ce -> ce.setHiddenStatusNeedsRecompute());
            }
        }

        boolean currentlyRunning = false;
        if(this.mHiddenCardsComputeThread!=null){
            if(this.mHiddenCardsComputeThread.isAlive()){
                currentlyRunning = true;
            }
        }

        if(currentlyRunning){
            return;
        }

        if(System.currentTimeMillis() < mTimeStampLastHiddenCardsComputationStarted + mConfTimeIntervalHiddenCardsComputationMilliSeconds ){
            if(mHiddenCardsComputationRequested){

            }
            else{
                if(logLevel>1) {
                    System.out.println("request recompute hidden card elements --> SCHEDULED!");
                }
                mHiddenCardsComputationRequested = true;
                WaitThenCompute wtc = new WaitThenCompute( mTimeStampLastHiddenCardsComputationStarted + mConfTimeIntervalHiddenCardsComputationMilliSeconds - System.currentTimeMillis() );
                wtc.start();
            }
        }
        else{
            if(logLevel>1) {
                System.out.println("request recompute hidden card elements --> DO!");
            }
            //WaitThenCompute wtc = new WaitThenCompute( 0 );
            mHiddenCardsComputeThread = new WaitThenCompute( 0 );
            mHiddenCardsComputeThread.start();
        }
    }


    /**
     * This will be called when cards are
     * moved, intersected by moved card, changed z order, overlapping card changed z-order etc..
     *
     * Will immediately do: set the card to non-hidden and request recomputation of hidden status
     */
    public void fireCardElementChanged(List<CardElement> cardElements){
//        System.out.println("CARD ELEMENTS CHANGED: "+cardElements.size() );
//        Thread t_setHidden = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                for ( CardElement ce : cardElements) {
//                    new ArrayList<>(getAllCardElements()).stream().filter(cei -> cei.getRectangle().intersects(ce.getRectangle())).forEach(ceix -> ceix.setHiddenStatus(CardElement.HIDDEN_STATUS_TO_COMPUTE));
//                    ce.setHiddenStatus(CardElement.HIDDEN_STATUS_TO_COMPUTE);
//                }
//            }
//        });
        //recomputeHiddenCardElements( true,false);
        recomputeHiddenCardElements( false,false);
        fireCardPaneEvent(new CardPaneEvent(CardPaneEvent.EventType.NEW_CARD_POSITIONS) );
    }

    /**
     * This will be called when cards are
     *  moved, intersected by moved card, changed z order, overlapping card changed z-order etc..
     *
     *  Will immediately do: set the card to non-hidden and request recomputation of hidden status
     */
    public void fireCardElementChanged(CardElement ce){
        ArrayList<CardElement> cel = new ArrayList<>();
        cel.add(ce);
        fireCardElementChanged(cel);
    }

//    public void setRenderQuality(int quality){
//        this.mRenderQuality = quality;
//        if(quality < RENDER_QUALITY_FULL) {
//            //this.getImageBuffer().clear();
//
//            ArrayList<CardElement> ceList = new ArrayList<>( mCardPaneModel.getAllElements() );
//            ceList.sort( (ca,cb) -> (new Boolean(ca.isHidden())).compareTo(new Boolean(cb.isHidden())) );
//            //BufferedImageCreatorThread bict = new BufferedImageCreatorThread(this);
//            //bict.start();
//            //this.mCreateBufferedImagesService.addRequests(ceList);
//        }
//    }

    public RenderQuality getRenderQuality() {
        return this.mRenderQuality;
    }




    private boolean mZoomOnResize = false;

    private int mZoomOnResize_LastW = -1;
    private int mZoomOnResize_LastH = -1;

    public void setZoomOnResizeEnabled(boolean enabled) {
        mZoomOnResize = enabled;
    }

    public boolean isZoomOnResizeEnabled() {
        return mZoomOnResize;
    }


    public double getViewportZoomFactor() {
        return mViewportZoomFactor;
    }


    ComponentListener mResizeAndZoomListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
            if(mZoomOnResize && mZoomOnResize_LastW>=0 &&  mZoomOnResize_LastH>=0) {
//                // compute increase:
//                double r_w = (1.0*getWidth()) / mZoomOnResize_LastW;
//                double r_h = (1.0*getHeight()) / mZoomOnResize_LastH;
//
//                double r_z = Math.max(r_w,r_h);
//
//                // zoom in:
//                double zf  = getViewportZoomFactor();
//                double zvx = getViewportCenterX();
//                double zvy = getViewportCenterY();
//                setViewport(zvx,zvy,zf * r_z);

                setViewportAroundAllNonexcludedCardElement(0.05);
            }

            mZoomOnResize_LastW = (int) getWidth();
            mZoomOnResize_LastH = (int) getHeight();
        }
    };

    @Override
    public void setSize(int w, int h) {
        this.setSize(new Dimension(w,h));
    }

    @Override
    public void setSize(Dimension d){
        super.setSize(d);
    }




    // NEW CODE FOR HANDLING Drag and Drop better:
    private CardElement m_DD_HiddenCardElement = null;
    private boolean m_DD_PhantomCard = false;
    private double m_DD_PhantomCard_x = Double.NaN;
    private double m_DD_PhantomCard_y = Double.NaN;
    private Image m_DD_Image = null;
    private CardElement m_DD_PhantomProposedStack = null;
    //private double m_DD_Phantom_x = Double.NaN;
    //private double m_DD_Phantom_y = Double.NaN;


    public void DD_setHiddenCardElement(CardElement ce) {m_DD_HiddenCardElement = ce;}
    public CardElement DD_getHiddenCardElement() {return m_DD_HiddenCardElement;}
    public void    DD_setDrawPhantomCard(boolean enabled){m_DD_PhantomCard = enabled;}
    public boolean DD_isDrawPhantomCard(){ return m_DD_PhantomCard; }

    public void DD_setPhantomCardImage(Image img){ m_DD_Image = img; }
    public void DD_setPhantomProposedStack(CardElement intersectedStack) {
        m_DD_PhantomProposedStack = intersectedStack;
    }
    public CardElement DD_getPhantomProposedStack(){ return m_DD_PhantomProposedStack; }

    /**
     *
     * @param pt (CardPane coordinates)
     */
    public void    DD_setPhantomCardCoordinates(Point pt){
        m_DD_PhantomCard_x = pt.getX();
        m_DD_PhantomCard_y = pt.getY();
    }
    //public double[] DD_getPhantomCardElement(){return new double[]{ m_DD_Phantom_x , m_DD_Phantom_y };}




    /**
     * This function should always be used to create new card elements.
     *
     * NOTE! This function does NOT add the card elements to the CardPaneModel of this JCardPane!
     *
     * @return the newly created card element
     */
    public CardElement createNewCardElement(Point2D pos, CompoundRecord record){
        ArrayList<CompoundRecord> list = new ArrayList<>();
        list.add(record);
        return createNewCardElement(pos,list);
    }

    /**
     * This function should always be used to create new card elements.
     *
     * NOTE! This function does NOT add the card elements to the CardPaneModel of this JCardPane!
     *
     * @return the newly created card element
     */
    public CardElement createNewCardElement(Point2D pos, Collection<CompoundRecord> records){
        double cw = this.mCardDrawer.getCardDrawingConfig().getCardWidth();
        double ch = this.mCardDrawer.getCardDrawingConfig().getCardHeight();

        mTopZIndex+=2;
        //CardElement ce = new CardElement( records, new Rectangle.Double( pos.getX()-0.5*cw , pos.getY()-0.5*ch, cw,ch) , mTopZIndex);
        CardElement ce = new CardElement( this , records, new Point2D.Double( pos.getX() , pos.getY() ) , this.getMaxZ()+1 );

        return ce;
    }


    /**
     * Use this to initialize a bunch of card elements at the same time. More efficient, as it does not recompute the
     * max z value for each new card element.
     *
     * NOTE! This function does NOT add the card elements to the CardPaneModel of this JCardPane!
     *
     * @param pos
     * @param records
     * @return
     */
    public List<CardElement> createNewCardElements( List<Point2D> pos , List<Collection<CompoundRecord>> records ) {
        double cw = this.mCardDrawer.getCardDrawingConfig().getCardWidth();
        double ch = this.mCardDrawer.getCardDrawingConfig().getCardHeight();

        mTopZIndex+=this.getMaxZ();
        mTopZIndex+=1;
        List<CardElement> card_elements = new ArrayList<>();

        for( int zi = 0; zi< records.size() ; zi++ ) {
            Collection<CompoundRecord> cri = records.get(zi);
            Point2D pi = pos.get(zi);
            CardElement ce = new CardElement(this, cri, new Point2D.Double(pi.getX(), pi.getY()), this.mTopZIndex+=1);
            card_elements.add(ce);
        }

        return card_elements;
    }

    public static Rectangle2D getBoundingBox( Collection<CardElement> elements ){
        return elements.stream().map(  (x) -> x.getRectangle() ).reduce(elements.iterator().next().getRectangle() , (x,y) -> x.createUnion(y) );
    }

//    public ConcurrentHashMap<CardElement,BufferedImage> getImageBuffer(){
//        return mBufferedImages;
//    }


    @Override
    public void configChanged() {
        this.repaint();
        // TODO: start recomputation of buffered images..
        //this.getImageBuffer().clear();
    }





    public void registerCardPaneEventListener(CardPaneEventListener cpel){
        this.mCardPaneEventListeners.add(cpel);
    }

    public void removeAllCardPaneListeners(){
        this.mCardPaneEventListeners.clear();
    }

    /**
     * Removes all listeners which are instances of the given class.
     *
     * @param type
     */
    public void removeAllCardPaneListeners(java.lang.Class type){
        List<CardPaneEventListener> listenersToRemove = this.mCardPaneEventListeners.stream().filter( peli -> type.isInstance(peli) ).collect(Collectors.toList());

    }



    public void fireCardPaneEvent(CardPaneEvent cpe){

        List<CardPaneEventListener> listeners = new ArrayList<>(mCardPaneEventListeners);
        for(CardPaneEventListener cpel : listeners){
            cpel.onCardPaneEvent(cpe);
        }
    }


    /**
     * Helper function to arrange the selected card elements nicely
     *
     * positioning: 1 = to the right , 2 = optimal (optimal means either below or right, depending on )
     *
     */
    public void arrangeSelectedCardElements( boolean expandStacks , int positioning , boolean setViewport ) {

        // 1. determine set of cards to arrange

        List<int[]>         to_arrange              = new ArrayList<>();
        List<Integer>       to_arrange_all_records  = new ArrayList<>();

        List<Integer> all_visible = new ArrayList<>();
        List<Integer> all_with_invisible = new ArrayList<>();

        List<Integer> remaining   = new ArrayList<>();

        boolean selected_only = true;

        if( selected_only ) {

            // find card elements to arrange
            for( CardElement cei : getCardPaneModel().getReducedAllElements() ) {
                int[] sri = cei.getSelectedRecords().stream().mapToInt(ri -> ri.getID()).toArray() ;
                if(sri.length==0){
                    continue;
                }
                else {
                    to_arrange.add(sri);
                    for (int tri : sri) {
                        to_arrange_all_records.add(tri);
                    }
                }
            }

            // find visible
            for (int zi = 0; zi < getCompoundTableModel().getTotalRowCount(); zi++) {
                all_with_invisible.add(zi);
                if ( getCompoundTableModel().isVisible( getCompoundTableModel().getTotalRecord(zi))) {
                    all_visible.add(zi);
                }
            }
            remaining.addAll(all_visible);
            remaining.removeAll(to_arrange_all_records);
        }

        if(expandStacks) {
            to_arrange = to_arrange_all_records.stream().map( ti -> new int[]{ti} ).collect(Collectors.toList());
        }

//        else{
//            for (int zi = 0; zi < mCTM.getTotalRowCount(); zi++) {
//                if (mCTM.isSelected(mCTM.getTotalRecord(zi))) {
//                    all_records.add(zi);
//                }
//            }
//        }

        // check if we can just return
        if(to_arrange.size()==0) {
            // no cards, so just return..
            if(logLevel>1) {
                System.out.println("[INFO] CardView::arrangeSelectedCards : no cards selected --> return");
            }
            return;
        }


        // 2. determine "starting pos x" and "anchor y":
        // for x we also consider invisible cards
        // for y we take the mean of visible cards


        double start_x = this.getCardPaneModel().getAllElements().stream().mapToDouble( ei -> ei.getPosX() ).max().getAsDouble();
        double pos_y   = this.getCardPaneModel().getReducedAllElements().stream().mapToDouble( ei -> ei.getPosY() ).average().getAsDouble();


        // 3. Compute some basic alignment values
        // maybe make relative to card width..
        double cw = this.getCardDrawer().getCardDrawingConfig().getCardWidth();
        double ch = this.getCardDrawer().getCardDrawingConfig().getCardHeight();

        double gap_x = 6*cw;

        double pos_x = start_x + gap_x;

        // sort?..
        //boolean invert = false;
        //to_arrange.sort( (Integer a , Integer b) -> (invert?-1:1) * Double.compare(  this.getCardPaneModel().getTableModel().getTotalDoubleAt(a,col_x) , this.getCardPaneModel().getTableModel().getTotalDoubleAt(b,col_x) )  );

        double shift_x_afterwards = 0.0;

        // 4.1 Compute positions:
        Map<Integer, Point2D> pos = null;
        int nPos = to_arrange.size();

        double aspect_ratio_viewport = (this.getSize().getWidth()*1.0) / (this.getSize().getHeight()*1.0);
        pos = computeOptimalGrid(nPos, cw, ch, aspect_ratio_viewport );

        // 4.2. put right to pos_x
        double x_pos_min = pos.values().stream().mapToDouble(pi -> pi.getX()).min().getAsDouble();
        double x_pos_max = pos.values().stream().mapToDouble(pi -> pi.getX()).max().getAsDouble();

        double shift_this = -x_pos_min;
        shift_x_afterwards = x_pos_max - x_pos_min;

        List<Double> px_all = new ArrayList<Double>();
        List<Double> py_all = new ArrayList<Double>();
        List<CardElement> new_card_elements = new ArrayList<>();

        List<CompoundRecord> all_to_remove = to_arrange_all_records.stream().map( ti -> getCompoundTableModel().getTotalRecord(ti) ).collect(Collectors.toList());
        this.getCardPaneModel().removeRecords(all_to_remove);

        for (int zi = 0; zi < nPos; zi++) {
            ArrayList<CompoundRecord> crlist = new ArrayList<>();
            for( int cri : to_arrange.get(zi) ) {
                crlist.add(this.getCardPaneModel().getTableModel().getTotalRecord(cri));
            }

            double pxi = pos_x + shift_this + pos.get(zi).getX();
            double pyi = pos.get(zi).getY();
            px_all.add(pxi); px_all.add(pyi);

            //this.getCardPaneModel().removeRecords(crlist);
            CardElement ce_new = this.getCardPaneModel().addCE(crlist,pos_x + shift_this + pos.get(zi).getX(), pos.get(zi).getY());

            new_card_elements.add(ce_new);
            //mFastCardPane.getCardPaneModel().addCE(crlist, pos_x + shift_this + pos.get(zi).getX(), pos.get(zi).getY());
        }

        // 5. move viewport
        if(setViewport) {
            DoubleSummaryStatistics dss_x = px_all.stream().mapToDouble(di -> di.doubleValue()).summaryStatistics();
            DoubleSummaryStatistics dss_y = px_all.stream().mapToDouble(di -> di.doubleValue()).summaryStatistics();
            this.setViewportAroundSpecificCardElements(new_card_elements, 0.2, 8 , 3);
        }
    }

    /**
     * Helper function that computes grid for cards, closest to the given target aspect ratio
     *
     * @param n
     * @param cw
     * @param ch
     * @return
     */
    public Map<Integer,Point2D> computeOptimalGrid( int n , double cw , double ch , double target_ratio) {
        // define gap
        double gap = 0.2 * cw;

        Map<Integer,Point2D> grid_points = new HashMap<>();

        // we loop over all possible rectangles and take the one closest to the target ratio:
        //double target_ratio = 4.0/3.0;

        int n_best = 100; double ratio_best=Double.MAX_VALUE;
        for(int zi=1;zi<n;zi++){
            //int y = n / zi;
            int y = (int) Math.ceil( (0.99999999*n) / zi );
            double ratio = (1.0*zi*cw )/ (1.0*y*ch);
            double ratio_distance = Math.abs( ratio-target_ratio );
            if(ratio_distance < ratio_best) {
                n_best = zi; ratio_best = ratio_distance;
            }
        }

        // position cards at this ratio.. first place just place the square, afterward make it zero centered..
        double x = 0; double y = 0;
        for( int zi=0;zi<n;zi++ ){
            if( zi % n_best == 0 ){ y += gap + ch; }
            x = (zi % n_best) * ( gap + cw ) ;
            grid_points.put( zi , new Point2D.Double( x , y ) );
        }

        // find center of gravity and subtract..
        double xmean = grid_points.values().stream().mapToDouble( pi -> pi.getX() ).average().getAsDouble();
        double ymean = grid_points.values().stream().mapToDouble( pi -> pi.getY() ).average().getAsDouble();

        Map<Integer,Point2D> grid_points_centered = new HashMap<>();
        for( Integer ki : grid_points.keySet() ){
            double xc = grid_points.get(ki).getX() - xmean;
            double yc = grid_points.get(ki).getY() - ymean;
            grid_points_centered.put( ki , new Point2D.Double(xc,yc) );
        }

        return grid_points_centered;
    }


    /**
     * relCardSizePadding is the rel. card width / height around the outemrost cards.
     *
     * @param relCardSizePadding
     */
    public void setViewportAroundAllNonexcludedCardElement(double relCardSizePadding){
        DoubleSummaryStatistics dss_x = this.getCardPaneModel().getReducedAllElements().stream().mapToDouble( ci -> ci.getPosX() ).summaryStatistics();
        DoubleSummaryStatistics dss_y = this.getCardPaneModel().getReducedAllElements().stream().mapToDouble( ci -> ci.getPosY() ).summaryStatistics();

        double cw = this.getAllCardElements().get(0).getRectangle().getWidth();
        double ch = this.getAllCardElements().get(0).getRectangle().getHeight();

        double xa = dss_x.getMin() - (0.5+relCardSizePadding) * cw;
        double xb = dss_x.getMax() + (0.5+relCardSizePadding) * cw;
        double ya = dss_y.getMin() - (0.5+relCardSizePadding) * ch;
        double yb = dss_y.getMax() + (0.5+relCardSizePadding) * ch;

        double ix = xb-xa;
        double iy = yb-ya;

        double w_div_h = (1.0*this.getWidth()) / this.getHeight();
        double aspect_ratio_cards = (xb-xa) / ( xb-ya ) ;
        //if(w_div_h > cw / ch){
        if(w_div_h > aspect_ratio_cards) {
            // we have enough width, height is limiting
            this.setViewport( xa + 0.5 * ix , ya + 0.5 * iy ,  (1.0*this.getHeight()) / iy );
        }
        else{
            // we have enough height, width is limiting
            this.setViewport( xa + 0.5 * ix , ya + 0.5 * iy ,  (1.0*this.getWidth()) / ix );
        }
    }

    public void setViewportAroundSpecificCardElements(List<CardElement> ce, double relCardSizePadding) {
        setViewportAroundSpecificCardElements(ce,relCardSizePadding,0,0);
    }

    public void setViewportAroundSpecificCardElements(List<CardElement> ce, double relCardSizePadding,
                                                      double minWidthInCardWidths, double minHeightInCardHeights) {
        DoubleSummaryStatistics dss_x = ce.stream().mapToDouble( ci -> ci.getPosX() ).summaryStatistics();
        DoubleSummaryStatistics dss_y = ce.stream().mapToDouble( ci -> ci.getPosY() ).summaryStatistics();

        double cw = this.getAllCardElements().get(0).getRectangle().getWidth();
        double ch = this.getAllCardElements().get(0).getRectangle().getHeight();

        double xa = dss_x.getMin() - (0.5+relCardSizePadding) * cw;
        double xb = dss_x.getMax() + (0.5+relCardSizePadding) * cw;
        double ya = dss_y.getMin() - (0.5+relCardSizePadding) * ch;
        double yb = dss_y.getMax() + (0.5+relCardSizePadding) * ch;

        double ix = xb-xa;
        double iy = yb-ya;

        double w_div_h = (1.0*this.getWidth()) / this.getHeight();
        //double aspect_ratio_cards = (xb-xa) / ( xb-ya ) ;
        double aspect_ratio_cards = (xb-xa) / ( yb-ya );

        // max zoom factor:
        double max_zf_a = this.getWidth()  / ( this.getCardDrawer().getCardDrawingConfig().getCardWidth() * minWidthInCardWidths );
        double max_zf_b = this.getHeight() / ( this.getCardDrawer().getCardDrawingConfig().getCardHeight() * minHeightInCardHeights );
        double max_zf   = Math.min( max_zf_a , max_zf_b );

        //if(w_div_h > cw / ch){
        if(w_div_h > aspect_ratio_cards) {
            // we have enough width, height is limiting
            this.setViewport( xa + 0.5 * ix , ya + 0.5 * iy ,
                    Math.min( max_zf, (1.0*this.getHeight()) / iy ) );
        }
        else{
            // we have enough height, width is limiting
            this.setViewport( xa + 0.5 * ix , ya + 0.5 * iy ,
                    Math.min( max_zf, (1.0*this.getWidth()) / ix ) );
        }

    }

    /**
     * Holds the card pane showing an expanded stack in case we are currently showing an expanded stack.
     * Otherwise it is null.
     */
    //private JCardPane mStackShowingCardPane = null;


    /**
     * Keep track of opened stack dialogs. This is to prohibit that the same stack is opened twice, which is
     * potentially bad.
     * Also, this is used in the drag and drop code, because we should forbid dragging onto opened stack elements!
     */
    private Set<CardElement> mOpenedStacks = new HashSet<>();

    public List<CardElement> getOpenedStacks() { return new ArrayList<>(mOpenedStacks); }

    public void createStackDialog( CardElement ce , MouseEvent e){

        // TODO: determine optmal size and position..
        // just set positions (don't know any way how we should determine what size should be right..)

        //ar: width / height
        double cards_ar = this.getCardDrawer().getCardDrawingConfig().getCardWidth() / (1.0*this.getCardDrawer().getCardDrawingConfig().getCardHeight());

        //int
        if(logLevel>0) {
            System.out.println("[JCardPane] : createStackDialog..");
        }

        JFrame f = new JFrame();
        //JCardPane cardPane = new JCardPane( this.mCardView, this.mTableModel, this.mListSelectionModel, this.mColorHandler);

        JCardPane cardPane = new JCardPane( this.mCardView, this.mDWL);
        f.setLayout(new BorderLayout());
        f.add(cardPane, BorderLayout.CENTER);



        Point2D p_card = new Point2D.Double();
        this.getAffineTransform().transform( this.getHighlightedCardElement().getPos() , p_card );


        if(ce.getNonexcludedRecords().size()<1){
            if(logLevel>1) {
                System.out.println("Don't create dialog, not a stack..");
            }
            return;
        }

        f.setVisible(true);

        //CardElement ce_reference = cardPane.addCard(ce.getNonexcludedRecords().get(0));
        ce.getNonexcludedRecords().stream().forEachOrdered( ci -> cardPane.addNewCardForRecord(ci) );
        GridPositioner gp   = new GridPositioner( cardPane.getCardPaneModel().getReducedAllElements() , 0,0,cardPane.getAllCardElements().get(0).getRectangle().getWidth() , cardPane.getAllCardElements().get(0).getRectangle().getHeight() );
        List<Point2D> posgp = gp.positionAllCards( null , cardPane.getCardPaneModel().getReducedAllElements() );
        int cnti = 0;
        double minx = Double.POSITIVE_INFINITY; double maxx = Double.NEGATIVE_INFINITY;
        double miny = Double.POSITIVE_INFINITY; double maxy = Double.NEGATIVE_INFINITY;
        double cw = this.getCardDrawer().getCardDrawingConfig().getCardWidth();
        double ch = this.getCardDrawer().getCardDrawingConfig().getCardHeight();

        for(CardElement cei :  cardPane.getCardPaneModel().getReducedAllElements()){
            cei.setCenter( posgp.get(cnti).getX() , posgp.get(cnti).getY() );

            minx = Math.min( minx , posgp.get(cnti).getX() - 0.5*cw ); maxx = Math.max( maxx , posgp.get(cnti).getX() + 0.5*cw );
            miny = Math.min( miny , posgp.get(cnti).getY() - 0.5*ch ); maxy = Math.max( maxy , posgp.get(cnti).getY() + 0.5*ch );

            cnti++;
        }

        // check which side is limiting..
//        double psx = maxx-minx; double psy = maxy-miny;
//        if( (0.3 * this.getWidth() * (psy/psx) <= this.getHeight()) ) {
//            f.setSize( (int) ( 0.3 * this.getWidth() ) , (int) (0.3 * this.getWidth() * (psy/psx)) );
//        }
//        else{
//            f.setSize( (int) (0.3 * this.getHeight() * (psx/psy)) , (int) ( 0.3 * this.getHeight() ) );
//        }

        double fw = SwingUtilities.getWindowAncestor(this).getSize().getWidth();
        double fh = SwingUtilities.getWindowAncestor(this).getSize().getHeight();

        double max_fw = 0.5*fw;
        double max_fh = 0.5*fh;

        int gp_dim_x = gp.getLastDimensions()[0];
        int gp_dim_y = gp.getLastDimensions()[1];
        // first part is for the 10% gap
        double wished_w = (1+0.2 + 0.1*(gp_dim_x-1) ) * HiDPIHelper.scale(60) + HiDPIHelper.scale(60) * (gp_dim_x) ;
        double wished_h = (1+0.2 + 0.1*(gp_dim_y-1) ) * HiDPIHelper.scale(60) + (HiDPIHelper.scale(60) / cards_ar) * (gp_dim_y);

        if(wished_w > 0.5*fw || wished_h > 0.5*fh ) {
            if( wished_w/max_fw > wished_h/ max_fh ) {
                // w is limiting
                f.setSize( (int)max_fw, (int) (wished_h * (max_fw/wished_w)) );
            }
            else {
                // h is limiting
                f.setSize( (int) ( wished_w * (max_fh/wished_h) ) , (int) max_fh );
            }
        }
        else {
            f.setSize( (int)wished_w, (int)wished_h);
        }

//        double psx = maxx-minx; double psy = maxy-miny;
//        if( (0.6 * this.getWidth() * (psy/psx) <= this.getHeight()) ) {
//            f.setSize( (int) ( 0.6 * this.getWidth() ) , (int) (0.6 * this.getWidth() * (psy/psx)) );
//        }
//        else{
//            f.setSize( (int) (0.6 * this.getHeight() * (psx/psy)) , (int) ( 0.6 * this.getHeight() ) );
//        }


        int px = (int) (  getCardView().getLocationOnScreen().getX() ) + (int) p_card.getX() - (int) (f.getWidth()/2) ;
        int py = (int) (  getCardView().getLocationOnScreen().getY() ) + (int) p_card.getY() - (int) (f.getHeight()/2) ;

        // pay attentions that popup window is not too heigh (such that it may cut the bar..)
        py = Math.max( py , HiDPIHelper.scale(40));
        f.setBounds( px , py , f.getWidth(), f.getHeight() );

        f.validate();

        //this.mStackShowingCardPane = cardPane;
        cardPane.setViewportAroundAllNonexcludedCardElement( 0.5 );

        // set the drawers in the stack showing card pane, then it should look somewhat similar..
        cardPane.setCardDrawer( this.getCardDrawer() );
        cardPane.setStackDrawer( this.getStackDrawer() );

        // disable stack creation. Currently, the stack proposal mouse handling creates events such that we "lose crs" in the goverend stack when stacks are proposed due to the event handling..
        cardPane.setProposeStacks(false);

        cardPane.setZoomOnResizeEnabled(true);

        f.addWindowListener( new WindowAdapter(){
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                //mStackShowingCardPane = null;
                // remove card element from "opened stacks" set:
                if(logLevel>0) {
                    System.out.println("WINDOW WAS CLOSED..");
                }
                mOpenedStacks.remove(ce);
            }
        });


        f.setTitle( ce.getStackName() );

        f.setAlwaysOnTop(true);
        // link the dialog card pane model to the stack:
        CardPaneModel.propagateAddAndRemoveToStack(cardPane.getCardPaneModel(), this.getCardPaneModel(),  ce);


    }


    /**
     * Behavior is partially defined by the m_DD_Behavior (setDragAndDropBehavior(..)) variable.
     *
     */
    class MyTransferHandler extends TransferHandler {

        private JCardPane mCardPane_Source = null;
        private Image mImage = null;

        public MyTransferHandler() {
            super();

            DragSource.getDefaultDragSource().addDragSourceMotionListener(new DragSourceMotionListener(){
                @Override
                public void dragMouseMoved(DragSourceDragEvent dsde) {

                    boolean propose_stacks = getDragAndDropBehavior() == DND_BEHAVIOR_PROPOSE_STACKS;

                    //Point pt = dsde.getLocation();
                    Point pt = getMousePosition();

                    if(pt!=null) {

                        // transform to card pane coordinates!
                        Point pt_transformed = new Point();
                        try {
                            getAffineTransform().inverseTransform(pt, pt_transformed);
                        }
                        catch(Exception e) {
                            e.printStackTrace();
                        }

                        CardElement found_proposed_stack = null;

                        if(propose_stacks && mProposeStacks) {
                            double cw = getCardDrawer().getCardDrawingConfig().getCardWidth();
                            double ch = getCardDrawer().getCardDrawingConfig().getCardHeight();
                            Rectangle cr = new Rectangle( (int) (  pt_transformed.getX() - 0.5*cw ) , (int) (pt_transformed.getY() - 0.5*ch )
                                    , (int) ( cw ) , (int) ( ch ) );
                            CardElement intersectedStack = testIntersectionWithCardElement(cr,false);

                            // prohibit intersection with opened stacks!
                            if( getOpenedStacks().contains( intersectedStack ) ) { intersectedStack = null; }

                            if(intersectedStack != null) {
                                if(!intersectedStack.isEmptyAfterExclusion()) {
                                    found_proposed_stack = intersectedStack;
                                }
                            }
                        }

                        if( found_proposed_stack !=null ) {
                            DD_setDrawPhantomCard(false);
                            //SwingUtilities.convertPointFromScreen(pt,getCardView().getCardPane() );
                            getThis().DD_setPhantomCardCoordinates(pt);
                            getThis().DD_setPhantomCardImage(mImage);
                            getThis().DD_setPhantomProposedStack(found_proposed_stack);
                        }
                        else {
                            DD_setPhantomProposedStack(null);
                            DD_setDrawPhantomCard(true);
                            //SwingUtilities.convertPointFromScreen(pt,getCardView().getCardPane() );
                            getThis().DD_setPhantomCardCoordinates(pt_transformed);
                            getThis().DD_setPhantomCardImage(mImage);
                        }

                        repaint();
                    }
                    else{
                        DD_setDrawPhantomCard(false);
                        DD_setPhantomProposedStack(null);
                        repaint();
                    }
                }
            });
        }

//        @Override
//        public Icon getVisualRepresentation(Transferable t) {
//            return new ImageIcon( getDragImage() );
//        }

        public int getSourceActions(JComponent c) {

            if(logLevel>1) {
                System.out.println("getSourceActions");
            }
//            setDragImage( ((JCardPane)c).getCachedCardProvider().getImage(((JCardPane)c).getDraggedCardElement()));
//            getThis().DD_setPhantomCardImage(((JCardPane)c).getCachedCardProvider().getImage(((JCardPane)c).getDraggedCardElement()));
//            mImage = ((JCardPane)c).getCachedCardProvider().getImage(((JCardPane)c).getDraggedCardElement());
////            ((JCardPane)c).DD_setPhantomCardImage(( getCachedCardProvider().getImage(getDraggedCardElement())));
//            ((JCardPane)c).DD_setPhantomCardImage(( getCachedCardProvider().getImage(getDraggedCardElement())));

//            BufferedImage bi = new BufferedImage(200,200,BufferedImage.TYPE_INT_RGB);
//            Graphics g = bi.getGraphics();
//            g.setColor(Color.green);
//            g.clearRect(0,0,200,200);
//            g.fillRect(0,0,150,150);
//            g.dispose();
//            setDragImage(bi);
//            DD_setPhantomCardImage(bi);
            //setDragImage(getCachedCardProvider().getImage(getDraggedCardElement()));


            //setDragImageOffset(new Point(40,40));
//            Point p_mouse = c.getMousePosition();
//            if(p_mouse != null) {
//                System.out.println("set drag image offset!");
//                setDragImageOffset( p_mouse );
//            }

            return MOVE;
        }

        public Transferable createTransferable(JComponent c) {
            List<List<CompoundRecord>> crs = new ArrayList<>();

            List<CardPaneModel.CardElementInterface> sel = new ArrayList<>(getSelection());
            for (CardPaneModel.CardElementInterface cei : sel) {
                if (!cei.isEmptyAfterExclusion()) {
                    crs.add(new ArrayList<>(cei.getNonexcludedRecords()));
                }
            }

            CardPaneModel.ListOfListOfCompoundRecords ce_list = CardPaneModel.ListOfListOfCompoundRecords.createFromListOfListOfCardElements_Nonexcluded(sel);
            if(logLevel>1) {
                System.out.println("[DND] --> Transferable created: " + ce_list.toString());
            }

            // hide in parent view:
            mCardPane_Source = ((JCardPane)c);
            ((JCardPane)c).DD_setHiddenCardElement( ((JCardPane)c).getDraggedCardElement() );
            //setDragImage( ((JCardPane)c).getCachedCardProvider().getImage( ((JCardPane)c).getDraggedCardElement() ) );

            // on source:
            this.mCardPane_Source.DD_setHiddenCardElement(this.mCardPane_Source.getDraggedCardElement());
            this.mCardPane_Source.repaint();

            //this.setDragImage( this.mCardPane_Source.getCachedCardProvider().getImage(this.mCardPane_Source.getDraggedCardElement()) );

            // seems to work on windows (kind of), but not on ubuntu
            //ce_list.setDragImage( this.mCardPane_Source.getCachedCardProvider().getImage(this.mCardPane_Source.getDraggedCardElement()) );

            return ce_list;
            //return new StringSelection("abc!");
        }

//        private Image mDragImage = null;
//        @Override
//        public Image getDragImage() {
//            return mDragImage;
//        }
//        @Override
//        public void setDragImage(Image img) {
//            super.setDragImage(img);
//            mDragImage = img;
//        }



        public void exportDone(JComponent c, Transferable t, int action) {
            if (action == MOVE) {
                //c.removeSelection();
                //getCardPaneModel().removeCE()
                if(logLevel>1) {
                    System.out.println("[DND] REMOVE THAT");
                }

                if( t instanceof CardPaneModel.ListOfListOfCompoundRecords){
                    CardPaneModel.ListOfListOfCompoundRecords tlist =  (CardPaneModel.ListOfListOfCompoundRecords) t;
                    // this should contain the card elements..
                    tlist.getListOfListOfCompoundRecords().stream().forEach( cli -> getCardPaneModel().removeRecordsFromCE(cli.getCardElement() , cli.getCRs() ) );

                    // and fire event:
                    fireCardDragAndDropEvent(tlist,false);
                }
            }
            if(logLevel>1) {
                System.out.println("export done..");
            }
            setCursor(Cursor.getDefaultCursor());

            // NOTE! CLEAR the dragged cards in this view!! (else we can restart a drag, which is bad..)
            mCardPane_Source.setDraggedCardElement(null);
            mCardPane_Source = null;
        }

        public boolean canImport(TransferSupport supp) {
            //System.out.println("[DND] --> guguus!");
            // Check for String flavor
            if (!supp.isDataFlavorSupported( CardPaneModel.listOfListOfCompoundRecordsFlavor ) ) {
                return false;
            }

            try {
                if(logLevel>1) {
                    System.out.println("Can Import --> setPhantomCardImage : " + ((CardPaneModel.ListOfListOfCompoundRecords) supp.getTransferable().getTransferData(CardPaneModel.listOfListOfCompoundRecordsFlavor)).getDragImage());
                }
                //setDragImage( ( (CardPaneModel.ListOfListOfCompoundRecords) supp.getTransferable().getTransferData(CardPaneModel.listOfListOfCompoundRecordsFlavor) ).getDragImage() );
                //System.out.println(( "SET DRAGGED IMAGE (ffs): "+    (((CardPaneModel.ListOfListOfCompoundRecords) supp.getTransferable().getTransferData(CardPaneModel.listOfListOfCompoundRecordsFlavor) ).getDragImage() ) ) );
                DD_setPhantomCardImage( ( (CardPaneModel.ListOfListOfCompoundRecords) supp.getTransferable().getTransferData(CardPaneModel.listOfListOfCompoundRecordsFlavor) ).getDragImage() );
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            // on receiver:
            //((JCardPane) supp.getComponent() ).DD_setDrawPhantomCard(true);

            return true;
        }

        public boolean importData(TransferSupport supp) {
            if (!canImport(supp)) {
                return false;
            }

            try {
                // Fetch the Transferable and its data
                Transferable t = supp.getTransferable();
                CardPaneModel.ListOfListOfCompoundRecords data = (CardPaneModel.ListOfListOfCompoundRecords) t.getTransferData( CardPaneModel.listOfListOfCompoundRecordsFlavor );

                //DD_setDrawPhantomCard(true);

                // Insert the data at this location
                //insertAt(loc, data);
                if(logLevel>1) {
                    System.out.println("[DND] DROPPED THAT!! --> Create CEs and fire events!");
                }

                Random r01 = new Random(7643);
                double cw  = getCardDrawer().getCardDrawingConfig().getCardWidth();
                double ch  = getCardDrawer().getCardDrawingConfig().getCardHeight();

                if(getDragAndDropBehavior()==DND_BEHAVIOR_SIMPLE_DROP_RANDOM_SINGLE) {
                    int nDrops = data.getListOfListOfCompoundRecords().size();
                    for (CardPaneModel.ListOfCompoundRecords licr : data.getListOfListOfCompoundRecords()) {
                        List<CompoundRecord> lcr = licr.getCRs();
                        Point2D dp_component = supp.getDropLocation().getDropPoint();
                        Point2D dropPoint = getAffineTransform().inverseTransform(dp_component, null);

                        dropPoint.setLocation(dropPoint.getX() - 0.5 * cw + r01.nextDouble() * cw * Math.sqrt(nDrops), dropPoint.getY() - 0.5 * ch + r01.nextDouble() * cw * Math.sqrt(nDrops));
                        addCardElement(lcr, dropPoint.getX(), dropPoint.getY());
                    }
                }
                if( getDragAndDropBehavior()==DND_BEHAVIOR_SIMPLE_DROP_AS_STACK || getDragAndDropBehavior()==DND_BEHAVIOR_PROPOSE_STACKS ) {
                    List<CompoundRecord> all_records = data.getListOfListOfCompoundRecords().stream().flatMap( si -> si.getCRs().stream() ).collect(Collectors.toList());
                    // sanity check ;)
                    if(all_records.stream().distinct().count() != all_records.size()) {
                        System.out.println("[ERROR] : Multiple CRs dected in dragged stack --> very bad!");
                    }
                    if(DD_getPhantomProposedStack()!=null) {
                        DD_getPhantomProposedStack().addRecords(all_records);
                    }
                    else{
                        Point2D dp_component = supp.getDropLocation().getDropPoint();
                        Point2D dropPoint = getAffineTransform().inverseTransform(dp_component, null);
                        addCardElement(all_records, dropPoint.getX(), dropPoint.getY() );
                    }
                }


                // set DD settings..
                DD_setDrawPhantomCard(false);
                DD_setPhantomCardImage(null);
                DD_setPhantomProposedStack(null);
                repaint();

                fireCardDragAndDropEvent(data,true);

            } catch (Exception e) {
                System.out.println("[DND] -> EXCEPTION, something went wrong in importData..");
                System.out.println(e);
            }
            // Fetch the drop location
            // DropLocation loc = supp.getDropLocation();

            return true;
        }
    }

    /**
     * Clears all buffered cards, and starts reloading them..
     */
    public void reinitAllCards() {
        this.mCachedCardProvider.clearAll();
    }

    private List<CardDragAndDropListener> mCardDragAndDropListeners = new ArrayList<>();

    public void registerCardDragAndDropListener(CardDragAndDropListener listener){
        this.mCardDragAndDropListeners.add(listener);
    }

    public void removeCardDragAndDropListener(CardDragAndDropListener listener){
        this.mCardDragAndDropListeners.remove(listener);
    }

    /**
     * fires a card drag and drop event. if into=true, then cardDroppedInto(..) is fired, otherwise cardDroppedOutOf(..)
     *
     * @param into
     */
    public void fireCardDragAndDropEvent( CardPaneModel.ListOfListOfCompoundRecords crs, boolean into ){
        if(into){
            for(CardDragAndDropListener cdd : mCardDragAndDropListeners){
                cdd.cardDroppedInto(crs);
            }
        }
        else{
            for(CardDragAndDropListener cdd : mCardDragAndDropListeners){
                cdd.cardDroppedOutOf(crs);
            }
        }
    }


    public static interface CardDragAndDropListener {
        /**
         * Gets called by the card pane that gained a new card element
         *
         * @param records
         */
        public void cardDroppedInto(CardPaneModel.ListOfListOfCompoundRecords records);


        /**
         * Gets called by the card pane that lost a card element
         *
         * @param records
         */
        public void cardDroppedOutOf(CardPaneModel.ListOfListOfCompoundRecords records);

    }






    public static interface CardPaneEventListener{
        public void onCardPaneEvent(CardPaneEvent cpe);
    }



    private static final int m_Conf_FastHiddenTest_nPerSide = 6;
    private static double m_Conf_FastHiddenTest_testpoints[][] = new double[m_Conf_FastHiddenTest_nPerSide*m_Conf_FastHiddenTest_nPerSide][2];
    static{
        for(int zi = 0; zi< m_Conf_FastHiddenTest_nPerSide; zi++){ for(int zj = 0; zj< m_Conf_FastHiddenTest_nPerSide; zj++){ m_Conf_FastHiddenTest_testpoints[zi+ m_Conf_FastHiddenTest_nPerSide *zj][0] = (1.0*zi)/ m_Conf_FastHiddenTest_nPerSide; m_Conf_FastHiddenTest_testpoints[zi+ m_Conf_FastHiddenTest_nPerSide *zj][1] = (1.0*zj)/ m_Conf_FastHiddenTest_nPerSide; } }
    }

    public static boolean testCardElementIsHidden_Fast(CardElement ceTest, LinkedList<CardElement> elementsP_Sorted ){

        int start = -1;
        int cnti = 0;
        for(CardElement cei : elementsP_Sorted){
            if(cei.getZ() > ceTest.getZ() ){
                start = cnti;
                break;
            }
            cnti++;
        }

        if(start<0){return false;}

        double testpoints[][] = new double[m_Conf_FastHiddenTest_nPerSide*m_Conf_FastHiddenTest_nPerSide][2];

        //System.out.println("Rectangle: xy:"+ceTest.getRectangle().getX()+"/"+ceTest.getRectangle().getX());

        for(int zi = 0; zi< m_Conf_FastHiddenTest_testpoints.length; zi++){
            testpoints[zi][0] =  ceTest.getRectangle().getX() + m_Conf_FastHiddenTest_testpoints[zi][0]*ceTest.getRectangle().getWidth();
            testpoints[zi][1] =  ceTest.getRectangle().getY() + m_Conf_FastHiddenTest_testpoints[zi][1]*ceTest.getRectangle().getHeight();
            //System.out.println("Testpoint: "+testpoints[zi][0]+"/"+testpoints[zi][1]);
        }

        boolean testCard[] = new boolean[testpoints.length];

        List<CardElement> listC = elementsP_Sorted.subList( start , elementsP_Sorted.size() );

        for( CardElement cei : listC ){

            // !! THIS IS NECESSARY, BECAUSE IT IS POSSIBLE THAT SINGLE CARDELEMENTS CHANGE THEIR Z-ORDER
            // in that case, the starting index is wrong, and we have to loop through much more elements, but still get the correct result.
            if( cei.getZ() <= ceTest.getZ()  ){
                continue;
            }

            for( int cc = 0 ; cc < testCard.length ; cc++ ){
                testCard[cc] = testCard[cc] || cei.getRectangle().contains( testpoints[cc][0] , testpoints[cc][1] );
            }
            if(checkAllTrue(testCard)){
                //System.out.println("Hide card..");
                break;
            }
            // System.out.println();
//            for(int zi=0;zi<testCard.length;zi++){
//                System.out.print( (testCard[zi]) ? "X" : "0" );
//            }
        }

        return checkAllTrue(testCard);
    }

    private static boolean checkAllTrue(boolean[] array)
    {
        for(boolean b : array) if(!b) return false;
        return true;
    }




    public static boolean testCardElementIsHidden(CardElement ceTest, List<CardElement> elementsP){
        //List<CardElement> elements = mCardPane.getCardElements();


        ArrayList<CardElement> elements = new ArrayList<>(elementsP);
        // very stupid algo:
        // 1. find all with higher z
        // 2. find all overlapping
        // 3. check if hidden by overlapping
        Rectangle2D ce_rectangle = ceTest.getRectangle();
        List<CardElement> checkList1  = elements.stream().filter(c -> c.getZ() > ceTest.getZ() ).collect(Collectors.toList());
        List<Rectangle2D> checkList1r = checkList1.stream().map( (c -> c.getRectangle())).collect(Collectors.toList());
        List<Rectangle2D> checkList2  = checkList1r.stream().filter( r -> r.intersects(ce_rectangle) ).collect(Collectors.toList());


        Area area = new Area();
        checkList2.forEach(r -> area.add( new Area(r)));
        boolean contained = area.contains(ce_rectangle);
        return contained;
    }



    public BufferedImage createBufferedImageForCard(CardElement ce, int res_x, int res_y){

        // probably this should be synchronized with the CardPaneModel, as in theory it is possible
        // that the mocdel changes during ce.isCardAfterExclusion() query and when the card is painted..
        // However, we should check if we risk any deadlocks with this sync.. (probably it should be fine)

        if(ce.isCardAfterExclusion()) {
            double cw = this.getCardDrawer().getCardDrawingConfig().getCardWidth();
            double ch = this.getCardDrawer().getCardDrawingConfig().getCardHeight();

            //BufferedImage bim = new BufferedImage(res_x, res_y, BufferedImage.TYPE_INT_ARGB);
            BufferedImage bim = new BufferedImage(res_x, res_y, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2     = (Graphics2D) bim.createGraphics();

//            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
//            g2.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);

            g2.setComposite(AlphaComposite.Clear);
            g2.fillRect(0, 0, res_x, res_y);
            g2.scale( res_x / cw, res_y / ch);
            //g2.setComposite(AlphaComposite.Src);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
            this.getCardDrawer().drawCard(g2, this.getCompoundTableModel(), ce.getNonexcludedRecords().get(0));
            g2.dispose();//g2.finalize();
            //this.getImageBuffer().put(ce, bim);
            return bim;
        }
        else{
            double cw = this.getStackDrawer().getStackDrawingConfig().getStackWidth();
            double ch = this.getStackDrawer().getStackDrawingConfig().getStackHeight();

            //BufferedImage bim = new BufferedImage(res_x, res_y, BufferedImage.TYPE_INT_ARGB);
            BufferedImage bim = new BufferedImage(res_x, res_y, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2     = (Graphics2D) bim.createGraphics();

//            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
//            g2.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);

            g2.setComposite(AlphaComposite.Clear);
            g2.fillRect(0, 0, res_x, res_y);
            g2.scale( res_x / cw, res_y / ch);
            //g2.setComposite(AlphaComposite.Src);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
            this.getStackDrawer().drawStack(g2, this.getCompoundTableModel(), mCardView.getFocusListFlag(), ce.getNonexcludedRecords() , ce);
            g2.dispose();//g2.finalize();
            //this.getImageBuffer().put(ce, bim);
            return bim;
        }
    }


    //}

//    public BufferedImage createBufferedImageForCard(CardElement ce){
//        if(ce.isCardAfterExclusion()) {
//            double cw = this.getCardDrawer().getCardDrawingConfig().getCardWidth();
//            double ch = this.getCardDrawer().getCardDrawingConfig().getCardHeight();
//            //double aspect_ratio =  cw / ch;
//            //int npixel =  this.RENDER_QUALITY_NUM_PIXEL[ this.getRenderQuality() ];
//            int npixel =  this.RENDER_QUALITY_NUM_PIXEL[ this.RENDER_QUALITY_LOW ];
//            int res_x = (int) Math.ceil( Math.sqrt(npixel) * (cw/(ch)) );
//            int res_y = (int) Math.ceil( Math.sqrt(npixel) * (ch/(cw)) );
//
//            BufferedImage bim = new BufferedImage(res_x, res_y, BufferedImage.TYPE_INT_ARGB);
//            Graphics2D g2     = (Graphics2D) bim.createGraphics();
//            g2.setComposite(AlphaComposite.Clear);
//            g2.fillRect(0, 0, res_x, res_y);
//            g2.scale( res_x / cw, res_y / ch);
//            g2.setComposite(AlphaComposite.Src);
//            this.getCardDrawer().drawCard(g2, this.getCompoundTableModel(), ce.getNonexcludedRecords().get(0));
//            g2.finalize();
//            //this.getImageBuffer().put(ce, bim);
//            return bim;
//        }
//        else{
//            double cw = this.getCardDrawer().getCardDrawingConfig().getCardWidth();
//            double ch = this.getCardDrawer().getCardDrawingConfig().getCardHeight();
//            //double aspect_ratio =  cw / ch;
//            //int npixel =  this.RENDER_QUALITY_NUM_PIXEL[this.getRenderQuality()];
//            int npixel =  this.RENDER_QUALITY_NUM_PIXEL[this.RENDER_QUALITY_LOW];
//            int res_x = (int) Math.ceil( Math.sqrt(npixel) * (cw/(ch)) );
//            int res_y = (int) Math.ceil( Math.sqrt(npixel) * (ch/(cw)) );
//
//            BufferedImage bim = new BufferedImage(res_x, res_y, BufferedImage.TYPE_INT_ARGB);
//            Graphics2D g2     = (Graphics2D) bim.createGraphics();
//            g2.setComposite(AlphaComposite.Clear);
//            g2.fillRect(0, 0, res_x, res_y);
//            g2.scale( res_x / cw, res_y / ch);
//            g2.setComposite(AlphaComposite.Src);
//            this.getStackDrawer().drawStack(g2, this.getCompoundTableModel(), ce.getNonexcludedRecords());
//            g2.finalize();
//            //this.getImageBuffer().put(ce, bim);
//            return bim;
//        }
//    }


    class MyHighlightListener implements HighlightListener{
        @Override
        public void highlightChanged(CompoundRecord record) {
            //mCachedCardProvider.addDirtyRecord( record );
        }
    }


    private CachedCardProvider getCachedCardProvider(){
        return this.mCachedCardProvider;
    }

    public void setCachedCardProviderNumberOfPixelsToDraw(int pixels) {
        this.getCachedCardProvider().setPixelsToDraw(pixels);
    }

    public int getCachedCardProviderNumberOfPixelsToDraw() {
        return this.getCachedCardProvider().getPixelsToDraw();
    }


    private List<SnapArea> mSnapAreas = new ArrayList<>();

//	public void addSnapArea(){
//		this.mSnapAreas
//	}

    public void addSnapArea(SnapArea sa){
        this.mSnapAreas.add(sa);
    }

    public void clearSnapAreas(){
        this.mSnapAreas.clear();
    }

    public static class SnapArea {
        private Point2D mSnapPoint;
        private Shape   mSnapArea;
        private Function< CardElement , Map<CardElement,Point2D>> mReordering;

        private Object mDescription;

        public SnapArea(Point2D snapPoint , Shape snapArea , Function< CardElement , Map<CardElement,Point2D>> reordering, Object description){
            this.mSnapPoint = snapPoint;
            this.mSnapArea = snapArea;
            this.mReordering = reordering;
            this.mDescription = description;
        }
        public Point2D getSnapPoint(){ return mSnapPoint; }
        public Shape getSnapArea(){ return mSnapArea; }
        public Function< CardElement , Map<CardElement,Point2D>> getReorderingFunction(){ return mReordering; }
        public Object getDescription(){return this.mDescription;}
    }





    public JCardPane createReorderingPane(JCardView view, CardElement stack , int nCardsToShow , int widthPx ){

        //JCardPane rp = new JCardPane(view, this.mTableModel, this.mListSelectionModel, this.mColorHandler);
        JCardPane rp = new JCardPane(view, this.mDWL);
        rp.setCardDrawer( this.getCardDrawer() );
        rp.getCardPaneModel().setActiveExclusion(false);

        double cw = stack.getRectangle().getWidth();
        double ch = stack.getRectangle().getHeight();
        double conf_DX = 1.25; // space for one card, relative to card witdth
        ListReorderer reorderer = ListReorderer.createReordererPaneAndReordererForStack_1DGrid( stack , rp , 1.25 );
        List<CardElement> newCardElements = rp.getAllCardElements();
        // create and add the snap areas for reordering:
        //int ci=0;
        for(CardElement cedi : newCardElements ){
            int cei = reorderer.getOrder().get(cedi);
            rp.addSnapArea( new SnapArea( reorderer.getPositions().get(cei) , reorderer.getSnapAreas().get(cei) , (cex) -> reorderer.fReorder(cex, cei ) , new Integer(cei) ));
        }

        double vpx = (0.5* (nCardsToShow+1.5) ) * (cw*conf_DX );

        // set preferred size:
        double pWidth   = widthPx;
        double pHeight  = 1.2 * (( widthPx / (2*vpx/ch) ) );
        rp.setPreferredSize( new Dimension( (int) pWidth, (int) pHeight ) );
        rp.setMinimumSize( new Dimension( (int) pWidth, (int) pHeight ) );
        rp.setSize( new Dimension( (int) pWidth, (int) pHeight ) );

        // compute zoom factor:
        // here this PROBABLY wont work, as the pane does not have any size yet..
        // we will do this again where we create the dialog which contains the pane..
        rp.setViewport( vpx , 0 ,   (widthPx / (2*vpx) )   );

        rp.setZoomEnabled(false);
        rp.setDraggingFunctionality( JCardPane.MOUSE_DRAGGING_FUNCTIONALITY_ONLY_HORIZONTAL );
        rp.setOnlyAllowSnapAreaPositions(true);
        rp.registerCardPaneEventListener( new ReorderingPaneListener( stack ,reorderer,this,rp ) );
        return rp;
    }

    /**
     * Listens for "incomplete" drags, i.e. RESET_SNAP events. Here, it resets cards to original
     * positions.
     * Listens for "complete" drags, i.e. FINAL_SNAP_EVENT events. Here, it performs the performed snap inside the stack,
     * and it reloads the ListReorderer.
     *
     *
     */
    static class ReorderingPaneListener implements CardPaneEventListener {


        private CardElement mStack;

        private ListReorderer mReorderer;
        private JCardPane mMasterPane;
        private JCardPane mReorderingPane;


        public ReorderingPaneListener( CardElement stack, ListReorderer reorderer,  JCardPane masterPane, JCardPane reorderingPane ){
            this.mStack     = stack;
            this.mReorderer = reorderer;
            this.mMasterPane = masterPane;
            this.mReorderingPane = reorderingPane;
        }

        @Override
        public void onCardPaneEvent(CardPaneEvent cpe) {

            if (cpe.getType() == CardPaneEvent.EventType.RESET_SNAP_EVENT) {
                List<CardElement> roCEs = new ArrayList<>();
                List<Point2D> roPos = new ArrayList<>();
                for (Map.Entry<CardElement, Integer> eei : mReorderer.getOrder().entrySet()) {
                    Point2D posi = mReorderer.getPositions().get(eei.getValue());
                    roCEs.add(eei.getKey());
                    roPos.add(posi);
                }
                // animate to the new positions:
                mReorderingPane.startAnimation(PathAnimator.createToPositionPathAnimator(roCEs, roPos, 0.15));
            }

            if (cpe.getType() == CardPaneEvent.EventType.FINAL_SNAP_EVENT) {
                // (1) change the stack order,
                // (2) recompute the Reorderer functions (we can overtake all the rest)
                //mMasterPane.getCardPaneModel().reorderRecordsInStack(  );
                mMasterPane.getCardPaneModel().reorderRecordsInStack(  mStack , cpe.getDraggedCardElement().getAllRecords().get(0), ((Integer) cpe.getSnapArea().getDescription()).intValue());

                // reininit Reorderer:

                mReorderingPane.getCardPaneModel().clearAllCEs();
                ListReorderer reorderer = ListReorderer.createReordererPaneAndReordererForStack_1DGrid( mStack , mReorderingPane , 1.25 );
                List<CardElement> newCardElements = mReorderingPane.getAllCardElements();
                // create and add the snap areas for reordering:
                //int ci=0;
                mReorderingPane.clearSnapAreas();

                for(CardElement cedi : newCardElements ){
                    int cei = reorderer.getOrder().get(cedi);
                    mReorderingPane.addSnapArea( new SnapArea( reorderer.getPositions().get(cei) , reorderer.getSnapAreas().get(cei) , (cex) -> reorderer.fReorder(cex, cei ) , new Integer(cei) ));
                }

                mReorderingPane.removeAllCardPaneListeners( ReorderingPaneListener.class );
                mReorderingPane.registerCardPaneEventListener( new ReorderingPaneListener( mStack , reorderer,mMasterPane,mReorderingPane ) );

            }
        }
    }

    /**
     * Provides the list reordering functions, e.g. for the reordering pane
     */
    public static class ListReorderer {

        private Map<CardElement, Integer> mOrder;
        private Map<Integer, CardElement> mOrderInverse;
        private Map<Integer,Point2D> mPositions;

        private Map<Integer,Rectangle2D> mSnapAreas;

        private CardPaneModel mCardPaneModel = null;
        private CardElement   mGovernedStack = null;

        public ListReorderer(Map<CardElement, Integer> order, Map<Integer, Point2D> positions, Map<Integer,Rectangle2D> snapAreas){
            this.mOrder = order;
            this.mPositions = positions;
            this.mSnapAreas = snapAreas;

            mOrderInverse = new HashMap<>();
            for( Map.Entry<CardElement,Integer> ei : order.entrySet() ){
                mOrderInverse.put(ei.getValue(),ei.getKey());
            }
        }

        /**
         * With this constructor we directly link a stack of the card pane model which is governed by this reorderer.
         * That means, in case of calling the reorder function, the stack is actually reordered according to the
         * performed reordering.
         *
         * @param model
         * @param order
         * @param positions
         */
        public ListReorderer(CardPaneModel model, CardElement governedStack , Map<CardElement, Integer> order, Map<Integer, Point2D> positions, Map<Integer,Rectangle2D> snapAreas ){
            this(order,positions,snapAreas);
            this.mCardPaneModel = model;
            this.mGovernedStack = governedStack;
            this.mSnapAreas     = snapAreas;
        }

        /**
         * Creates all new positions, after reordering the card element ce to position posMovedTo
         *
         * @param ce
         * @param posMovedTo
         * @return
         */
        public Map<CardElement,Point2D> fReorder(CardElement ce , int posMovedTo ){
            Map<CardElement,Point2D> reordering = new HashMap<>();
//            int cePos = mOrder.get(ce);
//            if(cePos < posMovedTo) {
//                for(int zi=cePos+1 ; zi <= posMovedTo ; zi++){
//                    reordering.put( mOrderInverse.get(zi) , mPositions.get(zi-1) );
//                }
//            }
//            else{
//                for(int zi=posMovedTo ; zi <= cePos-1 ; zi++){
//                    reordering.put( mOrderInverse.get(zi) , mPositions.get(zi+1) );
//                }
//            }
//
            int cePos = mOrder.get(ce);

            for(CardElement cei  : this.mOrder.keySet()){
                if(ce.equals(cei)){continue;}
                int cei_pos = this.mOrder.get(cei);
                int cei_new_pos = cei_pos;

                if( cePos > posMovedTo ) {
                    if (cei_pos >= posMovedTo && cei_pos < cePos) {
                        cei_new_pos++;
                    }
                }
                else{
                    if (cei_pos <= posMovedTo && cei_pos > cePos) {
                        cei_new_pos--;
                    }
                }

                reordering.put( cei , mPositions.get(cei_new_pos) );
            }

            if(this.mCardPaneModel != null && this.mGovernedStack != null){
                // do reordering:
                mCardPaneModel.reorderRecordsInStack(this.mGovernedStack, cePos,posMovedTo);
            }



            return reordering;
        }

        public Map<CardElement, Integer> getOrder() { return mOrder; }
        public Map<Integer, CardElement> getOrderInverse() { return mOrderInverse; }
        public Map<Integer, Point2D>     getPositions() { return mPositions; }
        public Map<Integer, Rectangle2D>     getSnapAreas() {return mSnapAreas; }


        /**
         * Creates for each record in stack a new element in the reordererPane. Also computes the reorderer which
         * handles list reordering.
         *
         * The new CardElements are created in a horizontal grid.
         *
         * @param stack
         * @param reordererPane
         * @param conf_DX  grid spacing between elements
         * @return
         */
        public static ListReorderer createReordererPaneAndReordererForStack_1DGrid( CardElement stack , JCardPane reordererPane , double conf_DX ){
            // compute initial layout:
            double cw      = stack.getRectangle().getWidth();  //  rp.getCardDrawer().getCardDrawingConfig().getCardWidth();
            double ch      = stack.getRectangle().getHeight(); //  rp.getCardDrawer().getCardDrawingConfig().getCardHeight();
            //double conf_DX = 1.25;

            Map<Integer,Point2D> listPositions = new HashMap<>();
            Map<CardElement,Integer> listOrder     = new HashMap<>();

            Map<Integer,Rectangle2D> listSnapAreas = new HashMap<>();
            List<CardElement> newCardElements      = new ArrayList<>();

            // first card is at x = 0, i'th card is at i*1.25*cw
            int ci = 0;
            for(CompoundRecord ri : stack.getAllRecords() ) {
                double px = ci*conf_DX*cw;
                double py = 0;

                CardElement ce_new = reordererPane.addCardElement(ri, px, py);
                newCardElements.add(ce_new);

                listPositions.put(ci,new Point2D.Double(px,py));
                listOrder.put(ce_new,ci);

                listSnapAreas.put( ci , new Rectangle2D.Double(  px-0.3*cw , py-0.5*ch , 0.6*cw , ch ) ); // snap area

                ci++;
            }
            // initialize the ListReorderer..
            ListReorderer reorderer = new ListReorderer(listOrder,listPositions,listSnapAreas);
            return reorderer;
        }
    }



    public static class ComponentRepaintListener implements CardPaneEventListener {

        private JComponent mPane;
        public ComponentRepaintListener(JComponent pane ){
            this.mPane = pane;
        }

        @Override
        public void onCardPaneEvent(CardPaneEvent cpe) {
            mPane.repaint();
        }
    }


    public void setPixelDrawingPerformance(int pixels){
        this.getCachedCardProvider().setPixelsToDraw(pixels);
    }

    public int getPixelDrawingPerformance(){
        return this.getCachedCardProvider().getPixelsToDraw();
    }


    private double mConfig_Binning_RelBinWidth  = 4.0;
    private double mConfig_Binning_RelBinHeight = 4.0;

    class BinnedCardsService_All extends TimedServiceExecutor.RecomputeTask<CardPaneModel.BinnedCards> {
        @Override
        public CardPaneModel.BinnedCards compute(CardPaneModel.BinnedCards template) {
            return getCardPaneModel().getBinned2D_All(mConfig_Binning_RelBinWidth * getCardDrawer().getCardDrawingConfig().getCardWidth(),mConfig_Binning_RelBinHeight * getCardDrawer().getCardDrawingConfig().getCardHeight() , 0 , 0 );
        }
    }

    class BinnedCardsService_Nonexcluded extends TimedServiceExecutor.RecomputeTask<CardPaneModel.BinnedCards> {
        @Override
        public CardPaneModel.BinnedCards compute(CardPaneModel.BinnedCards template) {
            return getCardPaneModel().getBinned2D_Nonexcluded(mConfig_Binning_RelBinWidth * getCardDrawer().getCardDrawingConfig().getCardWidth(),mConfig_Binning_RelBinHeight * getCardDrawer().getCardDrawingConfig().getCardHeight() , 0 , 0 );
        }
    }




    public void activateInsaneMode(){
        this.startAnimation(new InsaneMode(this));
    }

}



class AnimatorThread extends Thread{

    JCardPane mCardPane;
    private AnimatorInterface mAnimator;

    public AnimatorThread(JCardPane cardPane, AnimatorInterface animator){
        this.mCardPane = cardPane;
        this.mAnimator = animator;
    }


    public void run(){
        this.mAnimator.initAnimation();
        long timestamp = System.currentTimeMillis();
        while(mAnimator.animate(  (System.currentTimeMillis() - timestamp)*1.0 / 1000.0 )){
            mCardPane.repaint();
        }
    }
}



/**
 *
 * Provides buffered images of cards.
 * The paintComponent method first computes the set of cards that are required and informs the CCP about it.
 * Based on the number of cards, the CCP decides on the optimal resolution for BIMs. Based on the set of cards it
 * lodas / recomputes missing or wrong size BIMs.
 *
 * In addition to this, the CCP provides a mechanism to request CardElement recomputations when CardElements change.
 *
 *
 *
 *
 */
class CachedCardProvider implements CardPaneModel.CardPaneModelListener {

    JCardPane mCardPane = null;

    //    int mPixelsToDraw    = 2000000;
//    int mPixelsToStore   = 2000000;
    int mPixelsToDraw    = 8000000;
    int mPixelsToStore   = 8000000;

    // number of threads to use
    volatile int mThreads        = -1;

    volatile int mThreadsRunning = 0;

    public void setPixelsToDraw(int pixels){
        this.mPixelsToDraw = pixels;
    }
    public int getPixelsToDraw(){return mPixelsToDraw;}


    public int  getThreadCount(){return mThreads;}
    public void setThreadCount(int n){ mThreads = n;}


    public CachedCardProvider( JCardPane cardPane, int pixelsToDraw, int pixelsToStore, int nThreadsForCards){
        this.mCardPane = cardPane;
        this.mPixelsToDraw = pixelsToDraw;
        this.mPixelsToStore = pixelsToStore;

        this.mThreads       = nThreadsForCards;

        // register CardPaneModelListener:
        this.mCardPane.getCardPaneModel().registerListener(this);
    }

    private ConcurrentHashMap<IdentityCheckedCardElement,BufferedImage> mBufferedImages = new ConcurrentHashMap<>();


    /**
     * Can be filled via setCardElementToDirty()
     */
    private ConcurrentLinkedDeque<IdentityCheckedCardElement> mDirtyCardElements = new ConcurrentLinkedDeque<>();

    /**
     * Can be filled via addDirtyRecord(), contains the IDs of the records
     */
    private ConcurrentLinkedDeque<Integer> mDirtyRecords = new ConcurrentLinkedDeque<>();



    /**
     * synchronized, because this guarantees that the mDirtyCardElements does stay the same during the setRequestedCards!
     * (as long as this class does not provide any other non-synchronized access to mDirtyCardElements..)
     *
     * @param ce
     */
    public synchronized void setCardElementToDirty(CardElement ce){
        this.mDirtyCardElements.add( new IdentityCheckedCardElement(ce) );
        mCardPane.repaint();
    }

    public synchronized void setCardElementsToDirty(Collection<CardElement> ces){
        List<IdentityCheckedCardElement> cesi =  ces.stream().map( cei -> new IdentityCheckedCardElement(cei)).collect(Collectors.toList());
        this.mDirtyCardElements.addAll(cesi);
    }

    /**
     * This is what has to be called after each VisibleElementsChanged , or SelectionChanged.
     */
    public synchronized void setAllStacksToDirty(){
        if(mDebug_FlagPrintOutput){
            System.out.println("SetAllStacksToDirty!");
        }
        this.mCardPane.getVisibleStacks().stream().forEach( si  -> setCardElementToDirty(si) );

        mCardPane.repaint();
    }


    /**
     * This reinits everything..
     */
    public synchronized void setAllCardElementsToDirty(){
        this.mCardPane.getVisibleCardElements().stream().forEach( ci -> setCardElementToDirty(ci) );

        mCardPane.repaint();
    }






//    public synchronized void addDirtyRecord( CompoundRecord record ){
//        if(record!=null) {
//            mDirtyRecords.add(record.getID());
//        }
//        mCardPane.repaint();
//    }

    private static BufferedImage mBufferedImageEmpty = new BufferedImage(16,16, BufferedImage.TYPE_INT_RGB);
    static {
        Graphics g = mBufferedImageEmpty.getGraphics();
        g.setColor(Color.gray);
        g.fillRect(0,0,16,16);
        g.dispose();
    }

    boolean mDebug_FlagPrintOutput = false;

    public synchronized void setRequestedCards( List<CardElement> ceList ){

        if(ceList==null){return;}
        if(ceList.size()==0){return;}

        CardElement ceA = ceList.get(0);

        int n = ceList.size();

        // compute optimal / max resolution for cards:
        double maxres = 1.0*(mPixelsToDraw) / n;

        double aspectRatio = ceA.getRectangle().getWidth() / ceA.getRectangle().getHeight();

        double xh  = Math.sqrt( maxres / aspectRatio );
        int    xhi = (int) Math.ceil(xh);
        int    xw  = (int) Math.ceil( ( 1.0*maxres / xhi) );

        // now check for all cards if they are within 0.3 to 2.5 times the wished values. If not, replace..
        double confLB = 0.3; double confUB = 2.5;

        // List of required computations:
        List<CardElement> needsRecomputation = new ArrayList<>();

        int cnt_found    = 0;
        int cnt_found_ok = 0;

        for( CardElement ce : ceList ){
            BufferedImage bim = mBufferedImages.get( new IdentityCheckedCardElement(ce) );

            boolean needsRecompute = true;

            if(bim!=null){
                cnt_found++;
                int pixelsCE = bim.getWidth() * bim.getHeight();
                if( pixelsCE >= confLB * maxres && pixelsCE <= confUB * maxres ){
                    cnt_found_ok++;
                    needsRecompute = false;
                }
            }
            else{
                // we put a dummy image:
                mBufferedImages.put( new IdentityCheckedCardElement(ce) , mBufferedImageEmpty );
            }

            if(needsRecompute){
                needsRecomputation.add(ce);
            }
        }



        if(this.mDebug_FlagPrintOutput){ System.out.println("CachedCardProvider : requested: "+ceList.size()+ " found="+cnt_found +"   found ok="+cnt_found_ok+"   to_recompute="+needsRecomputation.size()); }
        // create card computation threads:
        if(needsRecomputation.size()>0){
            if(this.mDebug_FlagPrintOutput){ System.out.println("CachedCardProvider : recompute "+needsRecomputation.size()+" cards at res: "+xw+"/"+xhi); }
        }

        // add dirty ones:
        LinkedList<CardElement> dirtyCEs_A = new LinkedList<>();
        while( !mDirtyCardElements.isEmpty() ){ dirtyCEs_A.add( mDirtyCardElements.pollFirst().getCardElement() ); }
        List<CardElement> dirtyCEs = dirtyCEs_A.stream().distinct().collect(Collectors.toList());
        if(this.mDebug_FlagPrintOutput){ System.out.println("CachedCardProvider : Dirty CEs: "+dirtyCEs.size()); }

        // remove dirty ones that are not requested..
        if(!dirtyCEs.isEmpty()) {
            Set<IdentityCheckedCardElement> requestedSet = new HashSet<>( ceList.stream().map( cei -> new IdentityCheckedCardElement(cei) ).collect(Collectors.toList()));
            dirtyCEs.stream().filter( cei -> requestedSet.contains( new IdentityCheckedCardElement(cei))).forEach( cei -> needsRecomputation.add(cei) ) ;
        }


//        // add elements with dirty records:
//        if(!mDirtyRecords.isEmpty()) {
//            LinkedList<Integer> dirtyRecs_A = new LinkedList<>();
//            while ( ! mDirtyRecords.isEmpty() ) {
//                dirtyRecs_A.add(mDirtyRecords.pollFirst());
//            }
//            Set<Integer> dirtyRecs = dirtyRecs_A.stream().collect(Collectors.toSet());
//            System.out.println("CachedCardProvider : Dirty records: "+dirtyRecs.size());
//
//            List<CardElement> ces = new ArrayList<>(mCardPane.getVisibleCardElements());
//            List<CardElement> cesWithDirtyRecs = new ArrayList<>();
//            for (CardElement cei : ces) {
//                if (cei.getNonexcludedRecords().size() == 1) {
//                    if (dirtyRecs.contains(cei.getNonexcludedRecords().get(0).getID())) {
//                        cesWithDirtyRecs.add(cei);
//                    }
//                    continue;
//                } else {
//                    if (cei.getNonexcludedRecords().stream().filter(cr -> dirtyRecs.contains(cr.getID())).count() > 0) {
//                        cesWithDirtyRecs.add(cei);
//                    }
//                }
//            }
//            System.out.println("CachedCardProvider : CEs with dirty CRs: " + cesWithDirtyRecs.size());
//            needsRecomputation.addAll( cesWithDirtyRecs );
//        }
//        else
//        {
//            System.out.println("No dirty records..");
//        }



        int numBuckets = mThreads - mThreadsRunning;
        int numItems   = needsRecomputation.size();

        if( numBuckets==0 || numItems==0 ){
            return;
        }


        int itemsPerBucket = (numItems / numBuckets);
        int remainingItems = (numItems % numBuckets);

        int itemsCnt = 0;
        for (int zi = 1; zi <= numBuckets; zi++){
            int extra = (zi <= remainingItems) ? 1:0;
            if(this.mDebug_FlagPrintOutput) {
                System.out.println("numBuckets: " + numBuckets + " sublist: " + itemsCnt + " : " + (itemsCnt + (itemsPerBucket + extra)));
            }
            List<CardElement> ceThread_i = new ArrayList<>( needsRecomputation.subList(itemsCnt,itemsCnt+ (itemsPerBucket+extra) ) );
            //itemsCnt += itemsCnt+ (itemsPerBucket+extra);
            itemsCnt +=(itemsPerBucket+extra);

            BimComputeThread thread = new BimComputeThread(ceThread_i, xw, xhi);
            mThreadsRunning++;
            thread.start();

            if(this.mDebug_FlagPrintOutput) {
                System.out.println("Threads running: " + mThreadsRunning + "\n");
            }
        }

    }

    public void clearAll() {
        this.mBufferedImages.clear();
        this.setAllCardElementsToDirty();
    }

    class BimComputeThread extends Thread {

        private int mPXW;
        private int mPXH;
        private List<CardElement> mWorkPackage;
        public BimComputeThread(List<CardElement> workPackage, int pxw , int pxh){
            this.mWorkPackage = workPackage;
            this.mPXW = pxw;
            this.mPXH = pxh;
        }

        public void run(){
            long timeStamp = System.currentTimeMillis();
            // order with respect to distance from the center of the viewport:
            try{
                mWorkPackage.sort( new Comparator<CardElement>(){
                    @Override
                    public int compare(CardElement o1, CardElement o2) {
                        Point2D vpc = new Point2D.Double( mCardPane.getViewport().getCenterX() , mCardPane.getViewport().getCenterY() );
                        return Double.compare( o1.getPos().distance( vpc) , o2.getPos().distance( vpc)  )  ;
                    }
                } );
            }
            catch(Exception e){
                // here we can end up occasionally, when elements move too much during execution of this thread.
                System.out.println("Exception in BimComputeThead. This can happen occasionally:");
                System.out.println(e.toString());
            }


            if(mWorkPackage.size()>0) {
                for (CardElement cei : mWorkPackage) {
                    //BufferedImage bim = mBufferedImages.get(new IdentityCheckedCardElement(cei));
                    //if(bim!=null){}
                    try {
                        mBufferedImages.put(new IdentityCheckedCardElement(cei), mCardPane.createBufferedImageForCard(cei, mPXW, mPXH));
                    }
                    catch(Exception e) {
                        System.out.println("BIM Compute Thread: exception wihle creating card..");
                        e.printStackTrace();
                    }
                }
                mCardPane.repaint(); // repaint does make sense, because missing images can be shown now..
            }
            mThreadsRunning--;

            long msTime = System.currentTimeMillis()-timeStamp;
            if(mDebug_FlagPrintOutput){ System.out.println("BimComputeThread::done! Recomputed "+mWorkPackage.size()+" CEs in "+msTime+" ms --> "+(1000.0*this.mWorkPackage.size()/msTime)+"/sec"); }
        }
    }

    public BufferedImage getImage(CardElement ce){
        return mBufferedImages.get( new IdentityCheckedCardElement(ce) );
    }

    @Override
    public void cardPaneModelChanged(CardPaneModelEvent e) {
        //e.getCards().stream().forEach( ce -> this.setCardElementToDirty( (CardElement) ce) );

        List<CardElement> ces = e.getCards().stream().map( ci -> (CardElement) ci ).collect(Collectors.toList());
        this.setCardElementsToDirty(ces);
        this.mCardPane.repaint();
    }



    class IdentityCheckedCardElement {

        private CardElement mCardElement;

        public IdentityCheckedCardElement(CardElement cardElement){
            mCardElement = cardElement;
        }

        public CardElement getCardElement(){
            return mCardElement;
        }

        @Override
        public boolean equals(Object o){
            if( ! (o instanceof IdentityCheckedCardElement ) ){
                return false;
            }

            IdentityCheckedCardElement io = (IdentityCheckedCardElement) o;

            return (io.getCardElement()==mCardElement);
        }

        @Override
        public int hashCode(){
            return System.identityHashCode(mCardElement);
        }
    }

}





/**
 * Goes through the CardElements and identifies "hidden" cards
 *
 * @ TODO: add something like synchronized skip list buffer which is processed at specific time intervals.
 *
 */
class NewHiddenCardsComputeThread implements Runnable{

    int logLevel_hc = 0;

    JCardPane mCardPane;

    public NewHiddenCardsComputeThread(JCardPane cardPane){
        this.mCardPane = cardPane;
    }

    public void run(){

        if(logLevel_hc>0) {
            System.out.println("COMPUTE HIDDEN CARDS:\n");
        }
        long timestamp = System.currentTimeMillis();


        int alreadyComputed = 0;
        int newlyComputed   = 0;
        int setToHidden     = 0;
        int setToVisible    = 0;


        double cw = Math.max ( mCardPane.getCardDrawer().getCardDrawingConfig().getCardWidth()  , mCardPane.getStackDrawer().getStackDrawingConfig().getStackWidth() );
        double ch = Math.max ( mCardPane.getCardDrawer().getCardDrawingConfig().getCardHeight() , mCardPane.getStackDrawer().getStackDrawingConfig().getStackHeight() );

        Map<IntOrderedPair,List<CardElement>> binnedMap = mCardPane.getCardPaneModel().getBinned2D_Nonexcluded(4*cw,4*ch,0.5*cw,0.5*ch).getBinnedCards();

        // @TODO sort by distance to viewport..


        for(List<CardElement> elements_i : binnedMap.values() )
        {
            //LinkedList<CardElement> elements = new LinkedList<>(mCardPane.getVisibleCardElements());
            LinkedList<CardElement> elements = new LinkedList<>(elements_i);

            try {
                elements.sort(new CardElementZComparator());
            } catch (Exception e) {
                // we can occasionally end up here, when reloading happens while cards change too much..
                if(logLevel_hc>0) {
                    System.out.println("HiddenCardsComputeThread : EXCEPTION! (this can happen occasionally..)");
                    System.out.println(e);
                }
            }

            for (CardElement ei : elements) {
                if (ei.getHiddenStatus_NeedsRecompute()) {
                    newlyComputed++;
                    //boolean isHidden = JCardPane.testCardElementIsHidden(ei, mCardPane.getVisibleCardElements());
                    boolean isHidden = JCardPane.testCardElementIsHidden_Fast(ei, elements);
                    if (isHidden) {
                        //System.out.println("set card to hidden at pos: "+ei.getPosX()+"/"+ei.getPosY() );
                        ei.setHiddenStatus(CardElement.HIDDEN_STATUS_HIDDEN);
                        setToHidden++;
                        if (false) {
                            if(logLevel_hc>0) {
                                System.out.println("card at " + ei.getPosX() + "/" + ei.getPosY() + " overlapped by:");
                            }
                            elements.stream().filter(eci -> eci.getRectangle().intersects(ei.getRectangle())).collect(Collectors.toList()).stream().forEach(exi -> System.out.println("card at: " + exi.getPosX() + "/" + exi.getPosY()));
                        }
                    } else {
                        ei.setHiddenStatus(CardElement.HIDDEN_STATUS_VISIBLE);
                        setToVisible++;
                    }
                } else {
                    alreadyComputed++;
                }
            }
        }

        long msTime = System.currentTimeMillis() - timestamp;
        if(logLevel_hc>0) {
            System.out.println("COMP TIME: " + msTime + "ms for " + newlyComputed + " elements --> " + (1000.0 * newlyComputed / msTime) + " ce/s");
            System.out.println("SET TO HIDDEN: " + setToHidden + " SET TO NONHIDDEN:" + setToVisible);
        }
    }
}





//
///**
// * Goes through the CardElements and identifies "hidden" cards
// *
// * @ TODO: add something like synchronized skip list buffer which is processed at specific time intervals.
// *
// */
//class HiddenCardsComputeThread extends Thread{
//
//    JCardPane mCardPane;
//
//    private boolean mCheckHidden    = true;
//    private boolean mCheckNonhidden = true;
//
//    public HiddenCardsComputeThread(JCardPane cardPane){
//        this(cardPane,true,true);
//    }
//    public HiddenCardsComputeThread(JCardPane cardPane, boolean checkHidden , boolean checkNonhidden){
//        this.mCardPane = cardPane;
//        this.mCheckHidden = checkHidden;
//        this.mCheckNonhidden = checkNonhidden;
//    }
//
//    public void run(){
//
//        Set<CardElement> toDetermine = new HashSet<>( mCardPane.getAllCardElements() );
//
//        System.out.println("COMPUTE:\n");
//        long timestamp = System.currentTimeMillis();
//        int setToHidden    = 0;
//        int setToNonhidden = 0;
//        while(!toDetermine.isEmpty()){
//            CardElement ce = toDetermine.iterator().next();
//
//
//            List<CardElement> elements = mCardPane.getAllCardElements();
//
//            toDetermine.remove(ce);
//            if( !mCheckHidden    &&  ce.isHidden()){ continue; }
//            if( !mCheckNonhidden && !ce.isHidden()){ continue; }
//
//            //boolean isHidden = JCardPane.testCardElementIsHidden(ce, mCardPane.getAllCardElements());
//            boolean isHidden = JCardPane.testCardElementIsHidden(ce, mCardPane.getVisibleCardElements());
//
//            if(isHidden){ ce.setHiddenStatus(CardElement.HIDDEN_STATUS_HIDDEN); setToHidden++; }
//            else{ce.setHiddenStatus(CardElement.HIDDEN_STATUS_VISIBLE);setToNonhidden++;}
////            if(isHidden){ ce.setHidden(true);setToHidden++;}
////            else{ ce.setHidden(false);setToNonhidden++;}
//
//        }
//        long msTime = System.currentTimeMillis() - timestamp;
//        System.out.println("COMP TIME: "+msTime+"ms for "+mCardPane.getAllCardElements().size()+" elements --> "+ (1000*mCardPane.getAllCardElements().size() / msTime) +" ce/s");
//        System.out.println("SET TO HIDDEN: "+setToHidden+ " SET TO NONHIDDEN:"+setToNonhidden);
//    }
//}
//
//
//class HiddenCardsComputeService extends AbstractTimedBufferedService<CardElement>{
//
//    JCardPane mCardPane;
//
//    private boolean mCheckHidden    = true;
//    private boolean mCheckNonhidden = true;
//
//    public HiddenCardsComputeService(JCardPane cardPane){
//        this(cardPane,true,true);
//    }
//    public HiddenCardsComputeService(JCardPane cardPane, boolean checkHidden , boolean checkNonhidden){
//        this.mCardPane = cardPane;
//        this.mCheckHidden = checkHidden;
//        this.mCheckNonhidden = checkNonhidden;
//    }
//
//
//
//    @Override
//    public void computeWorkPackage(List<CardElement> elementsListP) {
//        ArrayList<CardElement>  elementsToCheckList = new ArrayList<CardElement>(elementsListP);
//        long timestamp = System.currentTimeMillis();
//        int setToHidden    = 0;
//        int setToNonhidden = 0;
//        List<CardElement> elements = mCardPane.getVisibleCardElements(); //mCardPane.getAllCardElements();
//
//        //List<CardElement> allCardElements = mCardPane.getAllCardElements();
//        List<CardElement> allCardElements = mCardPane.getVisibleCardElements();
//
//        for(CardElement ce : elementsToCheckList){
//            if( !mCheckHidden    &&  ce.isHidden()){ continue; }
//            if( !mCheckNonhidden && !ce.isHidden()){ continue; }
//
//            boolean isHidden = JCardPane.testCardElementIsHidden(ce, allCardElements );
//
//            if(isHidden){ ce.setHidden(true);setToHidden++;}
//            else{ ce.setHidden(false);setToNonhidden++;}
//            //if(contained){System.out.print(".");}else{System.out.print("h");}
//        }
//        long msTime = System.currentTimeMillis() - timestamp;
//        //System.out.println("COMP TIME: "+msTime+"ms for "+mCardPane.getAllCardElements().size()+" elements --> "+ (1000*mCardPane.getAllCardElements().size() / (msTime+1) ) +" ce/s");
//        System.out.println("COMP TIME: "+msTime+"ms for "+mCardPane.getVisibleCardElements().size()+" elements --> "+ (1000*mCardPane.getVisibleCardElements().size() / (msTime+1) ) +" ce/s");
//        System.out.println("SET TO HIDDEN: "+setToHidden+ " SET TO NONHIDDEN:"+setToNonhidden);
//    }
//
//    @Override
//    public void runAfterWorkPackageComputed(){
//        //probably not needed?
//        mCardPane.repaint();
//    }
//
//}

//
//class BufferedImageCreatorService extends AbstractTimedBufferedService<CardElement> {
//
//    JCardPane mCardPane = null;
//
//    public BufferedImageCreatorService(JCardPane cardPane){
//        this.mCardPane = cardPane;
//    }
//
//    @Override
//    public void computeWorkPackage(List<CardElement> elementsListP) {
//        ArrayList<CardElement> ceList = new ArrayList<>(elementsListP);
//        for(CardElement ce : ceList){
//            this.mCardPane.createBufferedImageForCard(ce);
//        }
//    }
//    @Override
//    public void runAfterWorkPackageComputed(){
//        mCardPane.repaint();
//    }
//}

//
//class BufferedImageCreatorThread extends Thread{
//
//    JCardPane mCardPane = null;
//
//    public BufferedImageCreatorThread(JCardPane cardPane) {
//        this.mCardPane= cardPane;
//    }
//
//    public void run(){
//        // @ TODO: process non-hidden cards first!
//
//        AbstractCardDrawer drawer = mCardPane.getCardDrawer();
//        List<CardElement>  mycards = new ArrayList<>(mCardPane.getAllCardElements());
//        for(CardElement ce : mycards){
//            if(ce.isCard()) {
//                double cw = this.mCardPane.getCardDrawer().getCardDrawingConfig().getCardWidth();
//                double ch = this.mCardPane.getCardDrawer().getCardDrawingConfig().getCardHeight();
//                //double aspect_ratio =  cw / ch;
//                int npixel =  this.mCardPane.RENDER_QUALITY_NUM_PIXEL[this.mCardPane.getRenderQuality()];
//                int res_x = (int) Math.ceil( Math.sqrt(npixel) * (cw/(ch)) );
//                int res_y = (int) Math.ceil( Math.sqrt(npixel) * (ch/(cw)) );
//
//                BufferedImage bim = new BufferedImage(res_x, res_y, BufferedImage.TYPE_INT_ARGB);
//                Graphics2D g2     = (Graphics2D) bim.createGraphics();
//                g2.setComposite(AlphaComposite.Clear);
//                g2.fillRect(0, 0, res_x, res_y);
//                g2.scale( res_x / cw, res_y / ch);
//                g2.setComposite(AlphaComposite.Src);
//                drawer.drawCard(g2, mCardPane.getCompoundTableModel(), ce.getNonexcludedRecords().get(0));
//                g2.finalize();
//                mCardPane.getImageBuffer().put(ce, bim);
//            }
//            else{
//                double cw = this.mCardPane.getCardDrawer().getCardDrawingConfig().getCardWidth();
//                double ch = this.mCardPane.getCardDrawer().getCardDrawingConfig().getCardHeight();
//                //double aspect_ratio =  cw / ch;
//                int npixel =  this.mCardPane.RENDER_QUALITY_NUM_PIXEL[this.mCardPane.getRenderQuality()];
//                int res_x = (int) Math.ceil( Math.sqrt(npixel) * (cw/(ch)) );
//                int res_y = (int) Math.ceil( Math.sqrt(npixel) * (ch/(cw)) );
//
//                BufferedImage bim = new BufferedImage(res_x, res_y, BufferedImage.TYPE_INT_ARGB);
//                Graphics2D g2     = (Graphics2D) bim.createGraphics();
//                g2.setComposite(AlphaComposite.Clear);
//                g2.fillRect(0, 0, res_x, res_y);
//                g2.scale( res_x / cw, res_y / ch);
//                g2.setComposite(AlphaComposite.Src);
//                drawer.drawCard(g2, mCardPane.getCompoundTableModel(), ce.getNonexcludedRecords().get(0));
//                g2.finalize();
//                mCardPane.getImageBuffer().put(ce, bim);
//            }
//            System.out.println("draw");
//        }
//    }
//}
//
//
//


class InsaneMode implements AnimatorInterface {

    //List<CardElement> mElements = null;$
    JCardPane mCardPane           = null;

    double rotationSpeeds[]       = null;
    double vx[][]  = null;

    public InsaneMode( JCardPane pane ){
        mCardPane = pane;
        lastTimeStamp = System.currentTimeMillis();
    }

    @Override
    public void initAnimation() {

        rotationSpeeds = new double[mCardPane.getAllCardElements().size()];
        vx             = new double[mCardPane.getAllCardElements().size()][];
        for(int zi=0;zi<mCardPane.getAllCardElements().size();zi++){rotationSpeeds[zi] = 100* 0.25*( Math.random()-0.5 );  }
        for(int zi=0;zi<mCardPane.getAllCardElements().size();zi++){vx[zi] = new double[]{ 100*( Math.random()-0.5 ) , 10*( Math.random()-0.5 ) }; }
    }

    long lastTimeStamp = 0;

    @Override
    public boolean animate(double timeInSeconds) {
        if( System.currentTimeMillis() - lastTimeStamp > 20 ) {
            double t = 0.05;//( System.currentTimeMillis() - lastTimeStamp ) * 0.001;
            int zi=0;
            for(CardElement ei : mCardPane.getAllCardElements()) {
                ei.setCenter( ei.getRectangle().getCenterX()+vx[zi][0] * t , ei.getRectangle().getCenterY()+vx[zi][1] * t );
                ei.setAngle( ei.getAngle() +  rotationSpeeds[zi] * t );
                zi++;
                //System.out.println(ei.getAngle());
            }

            for(zi=0;zi<mCardPane.getAllCardElements().size();zi++){ vx[zi] = new double[]{ vx[zi][0]+ 1*( Math.random()-0.5 ) , vx[zi][1]+ 1*( Math.random()-0.5 ) }; }

            lastTimeStamp = System.currentTimeMillis();
        }
        else{
            try{Thread.sleep(10);}catch(Exception e){}
        }
        return true;
    }
}



