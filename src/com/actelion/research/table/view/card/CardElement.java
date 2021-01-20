package com.actelion.research.table.view.card;

import com.actelion.research.table.view.card.cardsurface.CardDrawingConfig;
import com.actelion.research.table.view.card.cardsurface.StackDrawingConfig;
import com.actelion.research.table.model.CompoundRecord;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a graphical card or stack on the card view.
 *
 *
 */
public class CardElement implements Cloneable , CardPaneModel.CardElementInterface {

    public static final Rectangle2D CONF_SIZE_RECTANGLE = new Rectangle2D.Double(0,0,40,100);


    private ArrayList<CompoundRecord> mRecords = new ArrayList<>();
    //private Rectangle2D mPos;
    private int                          mZ;

    private JCardPane mCardPane = null;

    //private AbstractCardDrawer mCardDrawer;
    //private AbstractStackDrawer mStackDrawer;

    private CardDrawingConfig    mCDC;
    private StackDrawingConfig   mSDC;

    /**
     * Default is just an empty string.
     */
    private String mStackName = "";

    private double mAngle = 0.0;

    public Color debugColor;

    private Point2D mPos;

    private Set<CompoundRecord> mExternallyExcludedRecords = new HashSet<>();

    private Set<CompoundRecord> mGreyedOutRecords = new HashSet<>();

    /**
     * Contains the cached list of non-externally excluded records
     */
    private List<CompoundRecord> mNonexcludedRecords = new ArrayList<>();

    /**
     * Contains the cached list of non-externally excluded and not greyed-out records
     * */
    private List<CompoundRecord> mNonexcludedNonGreyedOutRecords = new ArrayList<>();

    /**
     * Contains the cached list of non-externally excluded and not greyed-out records
     * */
    private List<CompoundRecord> mNonexcludedGreyedOutRecords = new ArrayList<>();


    // true if it does not have to be drawn due to cards on top.
    public static final int HIDDEN_STATUS_HIDDEN       = 0;
    public static final int HIDDEN_STATUS_VISIBLE      = 1;
    public static final int HIDDEN_STATUS_TO_COMPUTE_WAS_VISIBLE   = 2;
    public static final int HIDDEN_STATUS_TO_COMPUTE_WAS_HIDDEN    = 3;

    private int mHiddenStatus = HIDDEN_STATUS_TO_COMPUTE_WAS_VISIBLE;


    private static List<CompoundRecord> asList(CompoundRecord cr){ List<CompoundRecord> crl = new ArrayList<>(); crl.add(cr); return crl; }


    public CardElement(JCardPane cardPane, Collection<CompoundRecord> records, Point2D pos, int z){
        this.mCardPane = cardPane;
        //this.mCardDrawer = cardPane.getCardDrawer();
        //this.mStackDrawer = cardPane.getStackDrawer();

        this.mCDC = cardPane.getCardDrawer().getCardDrawingConfig();
        this.mSDC = cardPane.getStackDrawer().getStackDrawingConfig();

//        this.mRecords = new ArrayList<>(records);
//        this.mNonexcludedRecords = new ArrayList<>();
//        this.mNonexcludedRecords.addAll(records);
        this.addRecords( new ArrayList<>(records) );

        this.mPos = pos;
        this.mZ = z;
        this.debugColor = new Color((float)Math.random(),(float)Math.random(),(float)Math.random());

        initCardElement();
    }

    public CardElement(JCardPane cardPane, CompoundRecord record , Point2D pos , int z){
        this(cardPane,asList(record),pos,z);
    }

    public CardElement( CardDrawingConfig cdc , StackDrawingConfig sdc , CompoundRecord record , Point2D pos , int z) {
        this(cdc,sdc,asList(record),pos,z);
    }

    public CardElement( CardDrawingConfig cdc , StackDrawingConfig sdc , List<CompoundRecord> records , Point2D pos , int z) {
        this.mCardPane = null;
        this.mCDC   = cdc;
        this.mSDC   = sdc;

//        this.mRecords = new ArrayList<>(record);
//        this.mNonexcludedRecords = new ArrayList<>();
//        this.mNonexcludedRecords.addAll(record);
//        this.mNonexcludedNonGreyedOutRecords.addAll(record);
        this.addRecords(records);

        this.mPos = pos;
        this.mZ = z;
        this.debugColor = new Color((float)Math.random(),(float)Math.random(),(float)Math.random());

        initCardElement();
    }

    /**
     * Contains some logic to ensure proper CardElement init.
     *
     * It does:
     * 1. if mCardPane!=null, and if a mFocusListFlag is set in the CardView, then we update the CardElement accordingly.
     */
    private void initCardElement(){
        if(mCardPane!=null) {
            if (mCardPane.getCardView() != null) {
                int focusListFlag = mCardPane.getCardView().getFocusListFlag();
                if (focusListFlag >= 0) {
                    List<CompoundRecord> greyedOut = mNonexcludedRecords.stream().filter(ri -> !ri.isFlagSet(focusListFlag)).collect(Collectors.toList());
                    this.setGreyedOutRecords(greyedOut);
                }
            }
        }
    }


    public JCardPane getCardPane(){ return mCardPane; }

    /**
     * Copy constructor
     *
     * @param ce
     */
    public CardElement( CardElement ce ){
        this.mCardPane  = ce.getCardPane();
        this.mCDC       = mCardPane.getCardDrawer().getCardDrawingConfig();
        this.mSDC       = mCardPane.getStackDrawer().getStackDrawingConfig();

        this.mRecords  = new ArrayList<>( ce.getAllRecords() );
        //this.mPos      = (Rectangle2D) ce.getRectangle().clone();

        this.mExternallyExcludedRecords = new HashSet<>( ce.getAllExternallyExcludedRecords() );
        this.mNonexcludedRecords = new ArrayList<>( ce.getAllNonexcludedRecords() );
        this.mNonexcludedNonGreyedOutRecords = new ArrayList<>( ce.getNonExcludedNonGreyedOutRecords() );
        this.mNonexcludedGreyedOutRecords = new ArrayList<>( ce.getNonExcludedGreyedOutRecords() );

        this.mZ        = ce.getZ();
        this.mAngle    = ce.getAngle();

        this.mPos      = new Point2D.Double( ce.getPosX() , ce.getPosY() );

        this.debugColor = new Color((float)Math.random(),(float)Math.random(),(float)Math.random());
    }

    public boolean isCard(){
        return this.mRecords.size()==1;
    }
    public boolean isStack(){
        return this.mRecords.size()>1;
    }

    public Set<CompoundRecord> getAllExternallyExcludedRecords(){return this.mExternallyExcludedRecords;}
    public List<CompoundRecord> getAllNonexcludedRecords(){return this.mNonexcludedRecords;}

    @Override
    public synchronized List<CompoundRecord> getNonExcludedNonGreyedOutRecords() {
        return mNonexcludedNonGreyedOutRecords;
    }

    @Override
    public synchronized List<CompoundRecord> getNonExcludedGreyedOutRecords() {
        return mNonexcludedGreyedOutRecords;
    }

    @Override
    public boolean containsNonexcludedGreyedOutRecords() {
        return getNonexcludedRecords().size() > getNonExcludedNonGreyedOutRecords().size();
    }

    public int getZ(){
        return this.mZ;
    }

    public void setZ(int z){
        this.mZ = z;
    }

//    public Rectangle2D getRectangle() {
//        if(this.isCardAfterExclusion()) {
//            double cw = mCDC.getCardWidth();
//            double ch = mCDC.getCardHeight();
//            return new Rectangle2D.Double( this.getPosX() - 0.5*cw , this.getPosY() - 0.5*ch , cw,ch);
//        }else{
//            double cw = mSDC.getStackWidth();
//            double ch = mSDC.getStackHeight();
//            return new Rectangle2D.Double( this.getPosX() - 0.5*cw , this.getPosY() - 0.5*ch , cw,ch);
//        }
//    }

    private Rectangle2D mRectangle = new Rectangle2D.Double(0,0,0,0);

    public Rectangle2D getRectangle(){
        if(this.isCardAfterExclusion()) {
            double cw = mCDC.getCardWidth();
            double ch = mCDC.getCardHeight();
            mRectangle.setRect(this.getPosX() - 0.5*cw , this.getPosY() - 0.5*ch , cw,ch);
        }else{
            double cw = mSDC.getStackWidth();
            double ch = mSDC.getStackHeight();
            mRectangle.setRect(this.getPosX() - 0.5*cw , this.getPosY() - 0.5*ch , cw,ch);
        }
        return mRectangle;
    }



    public double getPosX(){ return mPos.getX(); }
    public double getPosY(){ return mPos.getY(); }
    public void   setPosX(double px){ this.setCenter( px , this.getPosY() ); }
    public void   setPosY(double py){ this.setCenter( this.getPosX() , py ); }

    public double[] getDimension(){
        return new double[]{ this.getRectangle().getWidth(),this.getRectangle().getHeight()};
    }
    public void setDimension(double dimension[]){
        //System.out.println("NOT IMPLEMENTED!!..");
        //this.mPos.setRect(  this.getPosX() - 0.5*dimension[0] , this.getPosY() - 0.5*dimension[1] , dimension[0] , dimension[1] );
    }



//    public void setPos(Rectangle2D pos){
//        System.out.println("NOT IMPLEMENTED!!..");
//    }

    public Point2D getPos(){
        return mPos;
    }

    public void setCenter(double px, double py){
//        double rw = this.mPos.getWidth();
//        double rh = this.mPos.getHeight();
        this.mPos = new Point2D.Double(px,py);
    }


//    @Override
//    public synchronized List<CompoundRecord> getRecords(){
//        return Collections.unmodifiableList( new  ArrayList<>(this.mRecords) );
//    }


    @Override
    public String getStackName() {
        return mStackName;
    }

    @Override
    public void setStackName(String name) {
        this.mStackName = name;
    }

    @Override
    public boolean isEmptyAfterExclusion() {
        return this.getNonexcludedRecords().size()==0;
    }

    @Override
    public boolean isCardAfterExclusion() {
        return this.getNonexcludedRecords().size()==1;
    }

    @Override
    public boolean isStackAfterExclusion() {
        return this.getNonexcludedRecords().size()>1;
    }

    @Override
    public synchronized void addRecord(CompoundRecord r) {
        this.mRecords.add(r);
        if( this.getCardPane().getCompoundTableModel().isVisible( r ) ){
            this.mNonexcludedRecords.add(r);
        }
        else{
            this.mExternallyExcludedRecords.add(r);
        }
    }

    /**
     * Central function to add records to card element.
     * Takes care for maintaining all the caches.
     *
     * @param r
     */
    @Override
    public void addRecords(List<CompoundRecord> r) {
        this.mRecords.addAll(r);
        for(CompoundRecord ri : r){

            // we check if we are in "Full Mode" (with a JCardPane and a CTM), OR if we are just
            // in some simple mode where we do not have to care for all the caches..
            if(this.getCardPane()!=null) {

                if (this.getCardPane().getCompoundTableModel().isVisible(ri)) {
                    this.mNonexcludedRecords.add(ri);
                    if (mGreyedOutRecords.contains(ri)) {
                        this.mNonexcludedGreyedOutRecords.add(ri);
                    } else {
                        this.mNonexcludedNonGreyedOutRecords.add(ri);
                    }
                } else {
                    this.mExternallyExcludedRecords.add(ri);
                }
            }
            else{
                this.mNonexcludedRecords.addAll(r);
                this.mNonexcludedNonGreyedOutRecords.addAll(r);
                this.mNonexcludedGreyedOutRecords.addAll(r);
            }
        }
    }

    @Override
    public synchronized void removeRecord(CompoundRecord r){
        //this.mRecords.remove(r);
        List<CompoundRecord> rem = this.mRecords.stream().filter(ri -> ri.getID() == r.getID() ).collect(Collectors.toList());
        if(rem.size()>1){
            System.out.println("WARNING: record found multiple times!");
        }
        this.mRecords.removeAll( rem );
        this.mExternallyExcludedRecords.removeAll( rem );
        this.mNonexcludedRecords.removeAll( rem );
        this.mNonexcludedNonGreyedOutRecords.removeAll( rem );
        this.mNonexcludedGreyedOutRecords.removeAll( rem );
    }

    @Override
    public synchronized void removeRecords(Collection<CompoundRecord> r) {
        //this.mRecords.removeAll(r);
        Set<Integer> rem_ids = new HashSet<>();
        for(CompoundRecord ri : r){ rem_ids.add( ri.getID() ); }

        List<CompoundRecord> rem = this.mRecords.stream().filter(ri -> rem_ids.contains(ri.getID())  ).collect(Collectors.toList());
        this.mRecords.removeAll( rem );
        this.mExternallyExcludedRecords.removeAll( rem );
        this.mNonexcludedRecords.removeAll( rem );
        this.mNonexcludedNonGreyedOutRecords.removeAll( rem );
        this.mNonexcludedGreyedOutRecords.removeAll( rem );
    }

    public synchronized void removeRecordsByTotalID(Set<Integer> r) {

        List<CompoundRecord> rem = this.mRecords.stream().filter(ri -> r.contains(ri.getID())  ).collect(Collectors.toList());
        this.mRecords.removeAll( rem );
        this.mExternallyExcludedRecords.removeAll( rem );
        this.mNonexcludedRecords.removeAll( rem );
        this.mNonexcludedNonGreyedOutRecords.removeAll( rem );
        this.mNonexcludedGreyedOutRecords.removeAll( rem );
    }



    @Override
    public synchronized List<CompoundRecord> getAllRecords() {
        return this.mRecords;
    }

    @Override
    public synchronized List<CompoundRecord> getNonexcludedRecords() {
//        if(this.mExternallyExcludedRecords.isEmpty()){
//            return Collections.unmodifiableList(this.mRecords);
//        }
//        List<CompoundRecord> nonexcludedRecords = this.mRecords.stream().filter( cri -> !this.mExternallyExcludedRecords.contains(cri) ).collect(Collectors.toList());
        return mNonexcludedRecords;
    }

    /**
     *
     * @TODO: greyed out cards handling (probably just via setGreyedOut.. (?) )
     * @TODO: it looks like this method currently does not recover earlier excluded records, and unsets their status. Probably this should be changed!?..
     *
     * @param rxs
     */
    @Override
    public synchronized void setExternallyExcludedRecords(Collection<CompoundRecord> rxs) {
        this.mExternallyExcludedRecords = new HashSet<>(rxs);
        this.mNonexcludedRecords        = new ArrayList<>( this.getAllRecords() );
        this.mNonexcludedRecords.removeAll(mExternallyExcludedRecords);

//        this.mNonexcludedNonGreyedOutRecords = new ArrayList<>(mNonexcludedRecords);
//        this.mNonexcludedGreyedOutRecords = new ArrayList<>();
         this.updateGreyedOutRecords();
        //this.mNonexcludedNonGreyedOutRecords.removeAll(mExternallyExcludedRecords);
        //this.mNonexcludedGreyedOutRecords.removeAll(mExternallyExcludedRecords);
    }


    /**
     * This method assumes that:
     *    the currently set values are correct for:
     *
     *    mGreyedOutRecords , mNonexcludedRecords
     *
     *    based on this it updates the values for:
     *
     *    mNonexcludedNonGreyedOutRecords , mNonexcludedGreyedOutRecords
     *
     */
    public synchronized void updateGreyedOutRecords(){
        this.mNonexcludedNonGreyedOutRecords = new ArrayList<>(mNonexcludedRecords);
        this.mNonexcludedGreyedOutRecords.clear();

        // update caches:
        List<CompoundRecord> crList = new ArrayList<>();
        // update caches: 1. fetch crs which require update
        for( CompoundRecord cri : mNonexcludedNonGreyedOutRecords ) {
            if(mGreyedOutRecords.contains(cri)){
                crList.add(cri);
            }
        }
        // update caches: 2. update
        mNonexcludedNonGreyedOutRecords.removeAll(crList);
        mNonexcludedGreyedOutRecords.addAll(crList);
    }

    /**
     *
     * This method provides the standard (and the easiest) way to correctly handle greying out of card element.
     * This method does:
     * 1. clear the current mGreyedOutRecords
     * 2. set all records to non greyed out, i.e. it corrects all cache entries (mNonexcludedNonGreyedOut and mNonexcludedGreyedOut)
     * 3. set the new greyed out records
     * 4. updates the cache entries according to the newly greyed out set
     *
     * @TODO: probably we should send card element changed events, think about..
     *
     * @param rs
     */
    @Override
    public synchronized void setGreyedOutRecords( Collection<CompoundRecord> rs ){
        this.setAllRecordsToNonGreyedOut();

        // init
        this.mGreyedOutRecords = new HashSet<>(rs);

        // update caches:
        List<CompoundRecord> crList = new ArrayList<>();
        // update caches: 1. fetch crs which require update
        for( CompoundRecord cri : mNonexcludedNonGreyedOutRecords ) {
            if(mGreyedOutRecords.contains(cri)){
                crList.add(cri);
            }
        }
        // update caches: 2. update
        mNonexcludedNonGreyedOutRecords.removeAll(crList);
        mNonexcludedGreyedOutRecords.addAll(crList);
    }


    /**
     * Resets the non-greyed out caches. This is the first step of the setGreyedOutRecords(..) function
     */
    private void setAllRecordsToNonGreyedOut(){
        //this.mNonexcludedNonGreyedOutRecords.addAll(mNonexcludedGreyedOutRecords);
        this.mNonexcludedNonGreyedOutRecords = new ArrayList<>(mNonexcludedRecords);
        this.mNonexcludedGreyedOutRecords.clear();
        this.mGreyedOutRecords.clear();
    }




    public java.util.List<CompoundRecord> getSelectedRecords(){
        java.util.List<CompoundRecord> records = ( mRecords.stream().filter(cr -> cr.isSelected() ).collect(Collectors.toList()) );
        return records;
    }

    public boolean containsSelectedRecords(){
        return mRecords.stream().anyMatch(cr -> cr.isSelected());
    }


    public boolean isHidden()
    {
        return mHiddenStatus==HIDDEN_STATUS_HIDDEN || mHiddenStatus==HIDDEN_STATUS_TO_COMPUTE_WAS_HIDDEN;
    }

    public void setHiddenStatus(int hiddenStatus) {
        mHiddenStatus = hiddenStatus;
    }

    /**
     * Switches the hidden status to the correct NEEDS_RECOMPUTE status, based on the old status.
     */
    public void setHiddenStatusNeedsRecompute(){
        if(this.mHiddenStatus==HIDDEN_STATUS_VISIBLE){
            this.mHiddenStatus = HIDDEN_STATUS_TO_COMPUTE_WAS_VISIBLE;
            //System.out.println(this.getAllRecords().get(0).getID()+" --> to compute was visible");
        }
        if(this.mHiddenStatus==HIDDEN_STATUS_HIDDEN){
            this.mHiddenStatus = HIDDEN_STATUS_TO_COMPUTE_WAS_HIDDEN;
            //System.out.println(this.getAllRecords().get(0).getID()+" --> to compute was hidden");
        }
    }


    public int getHiddenStatus(){
        return mHiddenStatus;
    }

    public boolean getHiddenStatus_NeedsRecompute(){
        return (this.mHiddenStatus==HIDDEN_STATUS_TO_COMPUTE_WAS_VISIBLE) || (this.mHiddenStatus==HIDDEN_STATUS_TO_COMPUTE_WAS_HIDDEN);
    }

    public Set<Integer> getRecordIDsSet_Nonexcluded(){
        return new HashSet<>( this.mNonexcludedRecords.stream().mapToInt( ri -> ri.getID() ).boxed().collect(Collectors.toList()) );
    }

//    @Override
//    public int hashCode(){
//        int hashCode = 12345 ^ this.getAllRecords().hashCode() ^ (Integer.hashCode(this.getAllRecords().size()));
//        return hashCode;
//    }
//
//    @Override
//    public boolean equals(Object o){
//        if(!(o instanceof CardElement)){return false;}
//        return ((CardElement)o).getAllRecords().equals( this.getAllRecords() );
//    }

    BufferedImage mBufferedImage = null;


    public double getAngle(){
        return this.mAngle;
    }

    public void setAngle(double angle){
        this.mAngle = angle;
    }

    @Override
    public synchronized Object clone(){
        //Rectangle2D rect = (Rectangle2D) this.mPos.clone();
        CardElement ce = new CardElement(this.mCardPane,this.mRecords,this.mPos,this.mZ);
        ce.setAngle(this.getAngle());
        return ce;
    }

//    public List<CompoundRecord> getVisibleRecords( CompoundTableModel ctm ){
//        return this.getNonexcludedRecords().stream().filter( ri -> ctm.isVisible(ri) ).collect(Collectors.toList());
//    }

    @Override
    public synchronized void setRecords(List<CompoundRecord> r) {
        this.mRecords.clear();
        this.mNonexcludedRecords.clear();
        this.mExternallyExcludedRecords.clear();
        this.mNonexcludedNonGreyedOutRecords.clear();

        this.addRecords(r);
    }

    @Override
    public CardPaneModel.CardElementInterface copy() {
        CardElement clone = new CardElement(this.getCardPane(),this.getAllRecords(),this.getPos(),this.getZ());
        return clone;
    }

    /**
     * This probably is never really useful. Note that it does not care for CTM::isVislbe(..) etc..
     *
     * @param g
     * @param x
     * @param y
     * @param w
     * @param h
     */
    public void drawImmediately(Graphics g, int x, int y, int w, int h){
        if(this.mBufferedImage!=null){

        }
        else{
            g.setColor(Color.lightGray);
            g.fillRect(x,y,w,h);
        }
    }


    // @TODO: think about initial z positions, is there a nice way to ensure that new cards are on top?

    public static class CardElementFactory implements CardPaneModel.CardElementInterfaceFactory<CardElement> {

        private JCardPane mCardPane;

        public CardElementFactory(JCardPane cardPane){
            mCardPane = cardPane;
        }

        @Override
        public CardElement createCardElement(CompoundRecord r) {
            List<CompoundRecord> rlist = new ArrayList<>();
            rlist.add(r);
            return createCardElement(rlist);
        }

        @Override
        public CardElement createCardElement(List<CompoundRecord> r) {
            //CardElement newCE = new CardElement(r, (Rectangle2D) CONF_SIZE_RECTANGLE.clone() , (int)(Math.random()*10000) );
            CardElement newCE = new CardElement(mCardPane,  r , new Point2D.Double(0,0) , (int)(Math.random()*10000) );

            if(mCardPane.getCardView()!=null){
                int focusListFlag = mCardPane.getCardView().getFocusListFlag();
                if(focusListFlag>=0){
                    List<CompoundRecord> greyedOut = r.stream().filter( ri -> !ri.isFlagSet(focusListFlag) ).collect(Collectors.toList());
                    newCE.setGreyedOutRecords(greyedOut);
                }
            }

            return newCE;
        }

        @Override
        public CardElement clone(CardElement element) {
            return new CardElement(element);
        }

    }

}
