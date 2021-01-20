package com.actelion.research.table.view.card;

import com.actelion.research.table.view.card.positioning.CardPositionerInterface;
import com.actelion.research.table.model.*;
import com.actelion.research.table.view.card.positioning.JCardSortingPanel;
import com.actelion.research.table.view.card.tools.IntOrderedPair;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * Implements the model of the card pane.
 *
 * This class handles the card elements which are shown in the pane.
 *
 * It provides support for exclusion of records by the CompountTableModel. Exclusion of externally excluded CRs is
 * computed for every ce after a "VisibilityChange" event in the CTM.
 *
 * It provides support for propagating exclusion of records which are not visible in the card pane, via the setRowHiding(..) method.
 *
 * This class is thread-safe, all methods for adding / removing comopund records to card elements are synchronized.
 *
 * NOTE! Please do not alter the list of compound records in the CardElement objects directly, but use the functions provided
 * in this class to do this.
 *
 */
//public class CardPaneModel<T extends CardPaneModel.CardElementInterface, TFactory extends CardPaneModel<T,?>.CardElementInterfaceFactory > {
public class CardPaneModel<T extends CardPaneModel.CardElementInterface, TFactory extends CardPaneModel.CardElementInterfaceFactory<T> > implements CompoundTableListener , JCardPane.CardPaneEventListener , ListSelectionListener {



    private int logLevel = 0;


    //private Map<T,T> mAllElements = new HashMap<>();
    List<T> mAllElements = new ArrayList<>();


    private int mFlagExclusion = -1;

    private CompoundTableModel mCTM = null;

    private CompoundListSelectionModel mListSelectionModel = null;

    private TFactory mFactory = null;


    private Rectangle2D mViewport = null;





    /**
     * Listeners
     */
    private List<CardPaneModelListener> mListeners = new ArrayList<>();

    /**
     * Apply row hiding based on the visible cards.
     */
    private boolean mRowHiding = true;


    /**
     * Positioner will be always used when new cards have to be placed and there is no explicit position given.
     */
    private CardPositionerInterface mPositioner;



    public CardPaneModel( CompoundTableModel ctm , CompoundListSelectionModel selectionModel , TFactory factory ){

        mCTM = ctm;
        mListSelectionModel = selectionModel;
        mFactory = factory;

        mFlagExclusion = ctm.getUnusedRowFlag(true );
        if(mFlagExclusion<0){
            System.out.println("Warning: Exclusion currently not possible, no flag left..");
        }

        mCTM.addCompoundTableListener(this );

        mListSelectionModel.addListSelectionListener(this);
    }

    /**
     * Copy constructor
     */
    public CardPaneModel( CardPaneModel<T,TFactory> model ){
        this.mFactory = model.getCardElementFactory();
        this.mFlagExclusion = model.getFlagExclusion(); // -1
        this.mCTM           = model.getTableModel();
        this.mPositioner    = model.getPositioner();

        this.mAllElements = new ArrayList<>(); //new  HashMap<>();

        for( T ce : model.getAllElements() ){
            T ce_cloned = model.mFactory.clone(ce) ;
            //this.mAllElements.put(ce,ce);
            this.mAllElements.add(ce_cloned);
        }
    }

    @Override
    public void compoundTableChanged(CompoundTableEvent e) {
        if(e.isAdjusting()){return;}

        if(e.getType()==CompoundTableEvent.cChangeExcluded){
            for(T ce : this.getAllElements()){
                int rcIdsBefore[] = ce.getNonexcludedRecords().stream().mapToInt( ri -> ri.getID() ).sorted().toArray();

                List<CompoundRecord> excludedCE = ce.getAllRecords().stream().filter( ri -> !mCTM.isVisibleNeglecting(ri,mFlagExclusion)).collect(Collectors.toList());
                ce.setExternallyExcludedRecords(excludedCE);


                int rcIdsAfter[] = ce.getNonexcludedRecords().stream().mapToInt( ri -> ri.getID() ).sorted().toArray();

                if(rcIdsBefore.length!=rcIdsAfter.length)
                {
                    fireCardPaneModelEvent( new CardPaneModelEvent(CardPaneModelEvent.EventType.CARD_ELEMENT_CHANGED,ce) );
                }
                else{
                    if(! Arrays.equals(rcIdsBefore, rcIdsAfter)){
                        fireCardPaneModelEvent( new CardPaneModelEvent(CardPaneModelEvent.EventType.CARD_ELEMENT_CHANGED,ce) );
                    }
                }
            }
            // ! Update CardElement objects whether they are empty / card / stack..
        }
    }

    public void addCE(T ce){
        addCE(ce,false);
    }

    public synchronized void addCE(T ce, boolean usePositioner){
        mAllElements.add(ce);

        if(usePositioner){
            Point2D pos = mPositioner.positionSingleCard(ce, null);
        }

        fireCardPaneModelEvent(new CardPaneModelEvent(CardPaneModelEvent.EventType.NEW_CARD_ELEMENT, ce) );
    }


    /**
     * 1. Verifies for all compound records in all CardElements that they exist in the compound table model, and
     *    in case that they do not exist anymore, remove them.
     *
     * 2. Verify for all compound records in the compound table that they exist in some CardElement. For new compound
     *    records, add additional cards.
     */
    public synchronized void synchronizeWithCompoundTableModel() {

        Set<CompoundRecord> crs_card_view = new HashSet<>();
        Set<CompoundRecord> crs_ctm       = new HashSet<>();

        for(int zi=0; zi<mCTM.getTotalRowCount(); zi++) {
            crs_ctm.add(mCTM.getTotalRecord(zi));
        }
        this.getAllElements().forEach( ei -> crs_card_view.addAll( ei.getAllRecords() ) );

        if( crs_card_view.equals(crs_ctm) ) {
            // nothing to do..
            if(logLevel>0) {
                System.out.println("CardPaneModel::synchronizeWithCompoundTableModel() -> nothing to synch, everything alright");
            }
            return;
        }

        if(logLevel>0) {
            System.out.println("CardPaneModel::synchronizeWithCompoundTableModel() -> synch necessary!");
        }

        Set<CompoundRecord> to_add     = new HashSet<>( crs_ctm ); to_add.removeAll(crs_card_view);
        Set<CompoundRecord> to_remove  = new HashSet<>( crs_card_view ); to_remove.removeAll(crs_ctm);

        if(logLevel>0) {
            System.out.println("CardPaneModel::synchronizeWithCompoundTableModel() to add:    " + to_add.size() + " cards");
            System.out.println("CardPaneModel::synchronizeWithCompoundTableModel() to remove: " + to_remove.size() + " cards");
        }

        this.removeRecords(to_remove);

        // determine random positions for cards to be added:
        Random r = new Random();

        for(CompoundRecord cri : to_add) {
            double px = this.mViewport.getBounds2D().getCenterX() + r.nextDouble() * 0.25*this.mViewport.getBounds2D().getWidth();
            double py = this.mViewport.getBounds2D().getCenterY() + r.nextDouble() * 0.25*this.mViewport.getBounds2D().getHeight();
            ArrayList<CompoundRecord> cr_new_list = new ArrayList<>();
            cr_new_list.add(cri);
            this.addCE(cr_new_list,px,py);
        }

        if(logLevel>0) {
            System.out.println("CardPaneModel::synchronizeWithCompoundTableModel() done..");
        }
    }


    public synchronized void addCE(T ce, double px, double py){
        ce.setPosX(px); ce.setPosY(py);
        mAllElements.add(ce);

        fireCardPaneModelEvent(new CardPaneModelEvent(CardPaneModelEvent.EventType.NEW_CARD_ELEMENT, ce) );
    }


    public synchronized boolean removeCE(T ce){
        boolean removed = mAllElements.remove(ce);
        this.fireCardPaneModelEvent(new CardPaneModelEvent(CardPaneModelEvent.EventType.DELETED_CARD_ELEMENT , ce, ce.getAllRecords()));
        return removed;
    }

    public synchronized T addCE( CompoundRecord r ) {
        T newCE = addCE(r,false);
        return newCE;
    }

    public synchronized T addCE( CompoundRecord r , boolean usePositioner ){
        T newCE = mFactory.createCardElement(r);
        addCE(newCE,usePositioner);
        return newCE;
    }

//    public synchronized T addCE( List<CompoundRecord> r  ){
//        return addCE(r,false);
//    }

    public synchronized T addCE( List<CompoundRecord> r , double px, double py ){
        T t_new = addCE(r,false);
        t_new.setPosX(px);
        t_new.setPosY(py);
        return t_new;
    }

    public synchronized T addCE( List<CompoundRecord> r , boolean usePositioner ){
        T newCE = mFactory.createCardElement(r);
        addCE(newCE,usePositioner);
        return newCE;
    }


    public synchronized List<CardElementData> serializeToCardElementData() {
        List<CardElementData> ced = new ArrayList<>();

        this.getAllElements().stream().forEach( ci -> ced.add( new CardElementData( ci.getAllRecords().stream().map(ri -> ri.getID()).collect(Collectors.toList()), ci.getPosX(),ci.getPosY()) ) );
        return ced;
    }

    /**
     * This function can be used to update positions after computing new positions for specific card elements..
     *
     * What it does:
     * It goes through the list of CardElementData.
     * For each CardElementData the following happens:
     *   1. we go through the list of all records and remove them from the card pane model
     *   2. we add a new card element for the given CardElementData object.
     */
    public synchronized void loadSerializedCardElementDataPositions(List<CardElementData> ced) {

        List<CompoundRecord> records_to_delete = ced.stream().flatMap( ci -> ci.getCompoundRecords(mCTM).stream() ).collect(Collectors.toList());
        this.removeRecords(records_to_delete);
        for(CardElementData cedi : ced){
            this.addCE( cedi.getCompoundRecords(mCTM) , cedi.getPos().getX(), cedi.getPos().getY() );
        }

//        for(CardElementData cedi : ced){
//            this.removeRecords( cedi.getCompoundRecords(mCTM) );
//            this.addCE( cedi.getCompoundRecords(mCTM) , cedi.getPos().getX(), cedi.getPos().getY() );
//        }
    }

    /**
     * Assumes that stacks is a clustering of the given card elements. All card elements with the same entry in stacks
     * will be merged. Works on ALL card elements, not just the nonexcluded ones. However, positions of the newly clustered
     * card elements are computed based only on the currently visible ones.
     *
     */
    public synchronized void stackCardElements_All(int clustering[]){

        if(clustering.length!=this.getAllElements().size()){
            System.out.println("[CardPaneModel] ERROR: stackCardElements_Nonexcluded : stacks[] has wrong size");
            return;
        }

        // 1. init clusters:
        Map<Integer,List<T>> clusteredCardElements = new HashMap<>();
        Arrays.stream(clustering).distinct().forEach( ci -> clusteredCardElements.put( ci,new ArrayList<T>() ) );
        for( int zi=0;zi<this.getAllElements().size();zi++ ){
            clusteredCardElements.get( clustering[zi] ).add( this.getAllElements().get(zi) );
        }

        //this.clearAllCEs();
        // we move the crs of all to the first card element. All non-first card elements will be removed.
        for( Integer ki : clusteredCardElements.keySet() ){
            // we put all records into this one.
            T collector = clusteredCardElements.get(ki).get(0);

            List<CompoundRecord> additional_records = new ArrayList<>();
            double cepx = 0.0;
            double cepy = 0.0;
            for( T ce : clusteredCardElements.get(ki) ){
                if(ce!=collector) {
                    additional_records.addAll(ce.getAllRecords());
                }
                cepx+=ce.getPosX(); cepy+=ce.getPosY();
            }
            cepx = cepx / clusteredCardElements.get(ki).size();
            cepy = cepy / clusteredCardElements.get(ki).size();

            this.addRecordsToCE(collector,additional_records);
            collector.setPosX(cepx);
            collector.setPosY(cepy);

            // and remove other card elements:
            for(T tci : clusteredCardElements.get(ki) ) {
                if(tci!=collector) {
                    this.removeCE(tci);
                }
            }
        }
    }



    /**
     * Assumes that stacks is a clustering of the given card elements. All card elements with the same entry in stacks
     * will be merged. Works on nonexcluded card elements and currently visible records.
     *
     */
    public synchronized void stackCardElements_Nonexcluded(int clustering[]){

        List<T> nonexcludedCEs = getReducedAllElements();

        if(clustering.length!=nonexcludedCEs.size()){
            System.out.println("[CardPaneModel] ERROR: stackCardElements_Nonexcluded : stacks[] has wrong size");
            return;
        }

        // 1. init clusters:
        Map<Integer,List<T>> clusteredCardElements = new HashMap<>();
        Arrays.stream(clustering).distinct().forEach( ci -> clusteredCardElements.put( ci,new ArrayList<T>() ) );
        for( int zi=0;zi<nonexcludedCEs.size();zi++ ){
            clusteredCardElements.get( clustering[zi] ).add( nonexcludedCEs.get(zi) );
        }

        //this.clearAllCEs();

        // we move the crs of all to the first card element. All non-first card elements will be removed.
        for( Integer ki : clusteredCardElements.keySet() ){
            // we put all records into this one.
            T collector = clusteredCardElements.get(ki).get(0);

            List<CompoundRecord> additional_records = new ArrayList<>();
            double cepx = 0.0;
            double cepy = 0.0;
            for( T ce : clusteredCardElements.get(ki) ){
                if(ce!=collector) {
                    additional_records.addAll(ce.getNonexcludedRecords());
                    this.removeRecordsFromCE(ce, ce.getNonexcludedRecords());
                }
                cepx+=ce.getPosX(); cepy+=ce.getPosY();
            }
            cepx = cepx / clusteredCardElements.get(ki).size();
            cepy = cepy / clusteredCardElements.get(ki).size();

            this.addRecordsToCE(collector,additional_records);
            collector.setPosX(cepx);
            collector.setPosY(cepy);
        }
    }







    /**
     * Remvoes the supplied card elements in toStack from mAllElements. Creates new element with the combined compound
     * records and adds it to mAllElements.
     *
     * @param toStack
     * @param px
     * @param py
     * @param includeOnlyVisible
     * @return
     */
    public synchronized T createStack( Collection<T> toStack , double px, double py , boolean includeOnlyVisible ){
        if(toStack.isEmpty()){return null;}

        if(!includeOnlyVisible) {
            T newCE = mFactory.createCardElement(toStack.stream().flatMap(ti -> ti.getAllRecords().stream()).collect(Collectors.toList()));  //map( ti -> ti.getAllRecords() ).reduce( (la,lb) -> la.addAll(lb) ).get() );
            T roleModel = toStack.iterator().next();
            newCE.setDimension( roleModel.getDimension() );
            //removeRecords( toStack.stream().flatMap( ce -> ce.getAllRecords().stream() ).collect(Collectors.toList()) );
            this.mAllElements.removeAll(toStack);

            toStack.stream().forEach( ces -> fireCardPaneModelEvent(new CardPaneModelEvent(CardPaneModelEvent.EventType.DELETED_CARD_ELEMENT,ces ) ) );
            addCE(newCE, px, py);
            //fireCardPaneModelEvent(new CardPaneModelEvent(CardPaneModelEvent.EventType.NEW_CARD_ELEMENT,newCE)); // this happens already..

            return newCE;
        }
        else{
            T newCE = mFactory.createCardElement(toStack.stream().flatMap(ti -> ti.getNonexcludedRecords().stream()).collect(Collectors.toList()));  //map( ti -> ti.getAllRecords() ).reduce( (la,lb) -> la.addAll(lb) ).get() );
            T roleModel = toStack.iterator().next();
            //newCE.setDimension( roleModel.getDimension() );
            removeRecords( newCE.getAllRecords() );
            //this.mAllElements.removeAllRe(toStack);

            toStack.stream().forEach( ces -> fireCardPaneModelEvent(new CardPaneModelEvent(CardPaneModelEvent.EventType.DELETED_CARD_ELEMENT,ces) ) );
            addCE(newCE, px, py);
            return newCE;
        }
    }


    /**
     * NOTE! and TODO
     * For some reason this method alters the rem collection object. I.e. it removes all elements
     * from it, at least in some cases. Why is this? I don't understand hwo this is possible?..
     *
     * @param rem
     */
    public synchronized void removeRecords( Collection<CompoundRecord> rem ){

        //List<CompoundRecord> rem = new ArrayList<>(rem_);

        Set<Integer> idsToRemove = new HashSet<>( rem.stream().map( ri -> ri.getID() ).collect(Collectors.toList()) );

        List<T> toRemove = new ArrayList<>();
        for( T ce : this.getAllElements()){
            //System.out.println(rem.size());
            //ce.removeRecords(rem);
            ce.removeRecordsByTotalID(idsToRemove);

            if(ce.getAllRecords().size()==0){
                toRemove.add(ce);
                fireCardPaneModelEvent(new CardPaneModelEvent(CardPaneModelEvent.EventType.DELETED_CARD_ELEMENT,ce, new ArrayList<>(rem) ) );
            }
            else{
                fireCardPaneModelEvent(new CardPaneModelEvent(CardPaneModelEvent.EventType.CARD_ELEMENT_CHANGED,ce, new ArrayList<>(rem) ) );
            }
        }
        this.mAllElements.removeAll(toRemove);

        toRemove.stream().forEach( ces -> fireCardPaneModelEvent(new CardPaneModelEvent(CardPaneModelEvent.EventType.DELETED_CARD_ELEMENT,ces,rem) ) );
        //toRemove.stream().forEach( ti -> this.mAllElements.remove(ti) );
        // remove all zero elements..
        //List<T> toRemove2 = this.mAllElements.keySet().stream().filter( ei -> ei.getAllRecords().size() == 0 ).collect(Collectors.toList());
//        for( T ei : toRemove2 ){
//            if( (this.mAllElements.remove(ei))==null ){
//                System.out.println("ERORR: Something wrong!");
//            }
//        }
    }


//    public static int STACK_PROPOSAL_STATE_NONE = 0;
//
//    public static int STACK_PROPOSAL_STATE_STACK_PROPOSED = 1;


//    /**
//     * Here we remmeber the card element that was dragged on a stack during a stack proposal event.
//     * We need this to unstack or switch stacks.
//     */
//    private CardElement mCardElementDraggedAndInProposedStack = null;
//
//    /**
//     * We keep track of the "stack proposal state", i.e. the situation that results from dragging a card over other
//     * card elements.
//     */
//    private int mStackProposalState = 0;
//
//    public int getStackProposalState() {
//        return mStackProposalState;
//    }
//
//    /**
//     * Puts the dragged card element onto the hitCard card element.
//     *
//     * @param draggedCard
//     * @param hitCard
//     * @return The resulting stack (which is the same as the hitCard parameter card element)
//     */
//    public synchronized CardElement proposeStack( CardElement draggedCardElement , CardElement hitCardElement ) {
//
//        mCardElementDraggedAndInProposedStack = mFactory.createCardElement( draggedCard.get );
//        mStackProposalState = STACK_PROPOSAL_STATE_STACK_PROPOSED;
//    }
//
//    /**
//     * Unproposes the stack and
//     *
//     * @param draggedCard
//     * @param hitCard
//     */
//    public synchronized CardElement unproposeStack( CardElement draggedCard , CardElement hitCard , Point2D mousePos ) {
//
//        mStackProposalState = STACK_PROPOSAL_STATE_STACK_PROPOSED;
//        return
//    }






//    public synchronized void addRecordToCE( T ce , CompoundRecord cr ){
//        ce.getAllRecords().add( cr );
//    }

    public synchronized void addRecordsToCE( T ce , List<CompoundRecord> cr ){
        ce.addRecords(cr);
        fireCardPaneModelEvent(new CardPaneModelEvent(CardPaneModelEvent.EventType.CARD_ELEMENT_CHANGED,ce,cr) );
    }

    public synchronized void removeRecordFromCE( T ce , CompoundRecord cr ){
        List<CompoundRecord> crlist = new ArrayList<>(); crlist.add(cr);
        ce.removeRecord(cr);
        if(ce.getAllRecords().size()==0) {
            this.mAllElements.remove(ce);
            fireCardPaneModelEvent(new CardPaneModelEvent(CardPaneModelEvent.EventType.DELETED_CARD_ELEMENT,ce,crlist) );
        }
        else{
            fireCardPaneModelEvent(new CardPaneModelEvent(CardPaneModelEvent.EventType.CARD_ELEMENT_CHANGED,ce,crlist) );
        }
    }

    /**
     * Also removes the CardElement if empty afterwards.
     *
     * @param ce
     * @param cr
     */
    public synchronized void removeRecordsFromCE( CardElementInterface ce , List<CompoundRecord> cr ){
        ce.removeRecords(cr);
        if(ce.getAllRecords().size()==0) {
            this.mAllElements.remove(ce);
            fireCardPaneModelEvent(new CardPaneModelEvent(CardPaneModelEvent.EventType.DELETED_CARD_ELEMENT,ce,cr) );
        }
        else{
            fireCardPaneModelEvent(new CardPaneModelEvent(CardPaneModelEvent.EventType.CARD_ELEMENT_CHANGED,ce,cr) );
        }
    }





    /**
     * Returns all currently used stack ids
     */
    public synchronized Set<String> getUsedStackIDs() {
        return mAllElements.stream().map( si -> si.getStackName() ).collect(Collectors.toSet());
    }

    public synchronized boolean renameStack(int stack_defining_compound_record_id, String new_name) {


        List<CardElementInterface> to_rename = this.getAllElements().stream().filter( ci -> ci.getAllRecords().stream().filter( ri -> ri.getID()==stack_defining_compound_record_id ).count() > 0 ).collect(Collectors.toList());
        if(to_rename.size() != 1) {
            System.out.println("CardPaneModel::renameStack(..) : Something VERY wrong : stack-defining crid  "+stack_defining_compound_record_id+" exists "+to_rename.size()+" times..");
            return false;
        }

        if(new_name.isEmpty()) {
            // in this case we just set it, and do not care for conflicts etc..
            to_rename.forEach( ci -> ci.setStackName("") );
            return true;
        }

        String conflict_resolvers[] = new String[]{"_a","_b","_c","_d","_e","_f","_g","_h","_i","_j","_k"};


        Set<String> used_stack_ids = this.getUsedStackIDs();
        String try_name = new_name;
        int n_try = 0;
        while( used_stack_ids.contains( try_name )) {
            if(n_try<conflict_resolvers.length){
                try_name = new_name+conflict_resolvers[n_try];
            }
            else{
                try_name = new_name + "_" + ( n_try+1-conflict_resolvers.length );
            }
            n_try++;
        }

        to_rename.get(0).setStackName(try_name);
        return true;
    }

    public synchronized String createAutomaticStackName() {

        Set<String> used_stack_ids = this.getUsedStackIDs();

        boolean found_id = false;
        int zi = 1;
        String next_id = "";
        while(!found_id) {
            String next_proposal = "Stack "+zi;
            if(!used_stack_ids.contains(next_proposal)) {
                next_id = next_proposal;
                found_id = true;
            }
            zi++;
        }
        return next_id;
    }

//    public void mergeCardElement( CardElement ca , CardElement cb ) {
//        ca.get
//    }






    /**
     * Moves elements a to position b. I.e. removes element a andn inserts it at position b
     *
     * @param ce
     * @param a
     * @param b
     */
    public synchronized void reorderRecordsInStack( CardElementInterface ce , int a , int b ){
        if(a>b){
            CompoundRecord transfer = ce.getAllRecords().remove(a);
            ce.getAllRecords().add(b,transfer);
        }
        if(a<b){
            CompoundRecord transfer = ce.getAllRecords().remove(a);
            ce.getAllRecords().add( b , transfer );
        }
        fireCardPaneModelEvent(new CardPaneModelEvent(CardPaneModelEvent.EventType.CARD_ELEMENT_CHANGED,ce) );
    }

    /**
     * Moves elements a to position b. I.e. removes element a and inserts it at position b
     *
     * @param ce
     * @param a
     * @param b
     */
    public synchronized void reorderRecordsInStack( T ce , CompoundRecord a , int b ){

        int ia = -1;
        for( int zi=0; zi<ce.getAllRecords().size() ; zi++){
            CompoundRecord ri = ce.getAllRecords().get(zi);
            if(ri.getID() == a.getID()){
                ia = zi;
                break;
            }
        }
        reorderRecordsInStack(ce, ia, b);
    }



    public synchronized void setActiveExclusion(boolean enabled){
        this.mRowHiding = enabled;

        if(!this.mRowHiding){
            //this.setLocalExcluded(new ArrayList<>());
        }
        else{
        }

        this.updateRowHiding();
    }

    /**
     * This function initializes the pane such that it contains all records from the shows all records which are non-excluded
     *
     */
    public synchronized void initAccordingToCompoundTableModel( CardPositionerInterface positioner ){
        this.mPositioner = positioner;

        boolean usePositioner = (mPositioner!=null);

        for(int zi=0;zi<mCTM.getRowCount();zi++){
            addCE( mCTM.getRecord(zi) , usePositioner );
        }
    }

    public synchronized void setRowHiding(boolean enabled){
        this.mRowHiding = enabled;
        updateRowHiding();
    }


    public synchronized boolean isRowHiding(){
        return mRowHiding;
    }

    /**
     * Sets the exclusion flag from the card pane. I.e. sets it for all excluded, unsets it for all nonexcluded.
     */
    public synchronized void setLocalExcluded( List<CardElement> ce ){
        if(mFlagExclusion<0){
            System.out.println("Warning: Exclusion currently not possible, no flag left.. RETRY..");
            mFlagExclusion = mCTM.getUnusedRowFlag(true);
            if(mFlagExclusion<0){
                System.out.println("Warning: still failing..");
                return;
            }
            else{
                System.out.println("OK! Got exclusion flag! We're good to go!");
            }
        }
        Thread tSetExclusion = new Thread( new Runnable() {
            @Override
            public void run() {
                Set<T> complement = new HashSet<>( getAllElements() );
                complement.removeAll(ce);

                for( CardElement cei : ce ){
                    for( CompoundRecord cr : cei.getAllRecords() ){
                        cr.setFlag(mFlagExclusion);
                    }
                }
                for( CardElement cei : ce ){
                    for( CompoundRecord cr : cei.getAllRecords() ){
                        cr.clearFlag(mFlagExclusion);
                    }
                }
            }
        });
        tSetExclusion.start();
    }

    /**
     * Updates the greyed out status of all card elements.
     *
     * @param crList
     */
    public synchronized void setGreyedOutCompoundRecords( Collection<CompoundRecord> crList ) {
        for( T cei : mAllElements ) {
            cei.setGreyedOutRecords(crList);
        }
    }

    /**
     * NOTE! The returned list does not contain references to the original CardElements in mElements..
     *
     * Returns the visible card elements, after exclusion of of excluded records.
     * This is mainly used when actually drawing the card elements.
     *
     * @return
     */
    public synchronized List<T> getReducedVisibleElements(Predicate<T> testLocalVisibility ){

        List<T> allNonExcluded = new ArrayList<>();
        List<T> elementsList = new ArrayList<>(this.getAllElements());

        for( T ce : elementsList ){
            List<CompoundRecord> rr = ce.getNonexcludedRecords().stream().filter( ri -> mCTM.isVisible(ri) ).collect(Collectors.toList());
            if(rr.size()>0){
                T newCardElement = mFactory.clone(ce);
                newCardElement.setRecords(rr);
                allNonExcluded.add( newCardElement );
            }
        }

        List<T> listVisible      = new ArrayList<>();
        List<T> listCEsToExclude = new ArrayList<>();

        // for these compute the local visibility:
        for( T ce : allNonExcluded ){
            if( testLocalVisibility.test( ce )){
                listVisible.add(ce);
            }
            else{
                listCEsToExclude.add(ce);
            }
        }
        return listVisible;
    }

    public synchronized List<T> getAllElements(){
        //return Collections.unmodifiableList( new ArrayList<>( this.mAllElements.keySet() ) );
        return Collections.unmodifiableList( new ArrayList<>( this.mAllElements ) );
    }

    public synchronized void clearAllCEs(){
        this.mAllElements.clear();
    }


    /**
     * Returns a list of all card elements that are not empty after exclusion
     *
     * @return
     */
    public synchronized List<T> getReducedAllElements(){
        return this.mAllElements.parallelStream().filter( ec -> !ec.isEmptyAfterExclusion() ).collect(Collectors.toList());
        //return getReducedVisibleElements( (xt) -> true );
    }


    //    public void removeCardElementsForRecords(List<CompoundRecord> recordsToRemove){
//        Map<CompoundRecord,T> cr2ce = this.getMap_CompoundRecord2CE();
//        for(CompoundRecord rc : recordsToRemove){
//            T ce = cr2ce.get(rc);
//            if(ce.isCard()){ this.getAllElements().remove(ce); }
//            else{ ce.getRecords().remove(rc); }
//        }
//    }





    public void setHitlist(List<CompoundRecord> hitlist){

        Set<CompoundRecord> notGreyedOut = new HashSet<>( hitlist );
        List<CompoundRecord> greyedOut   = new ArrayList<>();

        for(int zi=0;zi<mCTM.getTotalRowCount();zi++){
            if(!notGreyedOut.contains(mCTM.getTotalRecord(zi))){ greyedOut.add(mCTM.getTotalRecord(zi)); }
        }

        for(T cei : mAllElements){
            cei.setGreyedOutRecords(greyedOut);
        }
    }



    public Map<T, Set<CompoundRecord>> getMap_CE2CompoundRecord(){
        HashMap<T,Set<CompoundRecord>> map = new HashMap<>();
        for(T ce : this.getAllElements()){
            map.put(ce,new HashSet<>(ce.getAllRecords()));
        }
        return map;
    }

    public Map<CompoundRecord,T> getMap_CompoundRecord2CE(){
        HashMap<CompoundRecord,T> map = new HashMap<>();
        for(T ce : this.getAllElements()){
            for(CompoundRecord cr : ce.getAllRecords()){
                map.put(cr,ce);
            }
        }
        return map;
    }


    public void registerListener(CardPaneModelListener cpel){
        this.mListeners.add(cpel);
    }
    public void removeListener(CardPaneModelListener cpel){
        this.mListeners.remove(cpel);
    }

    private void fireCardPaneModelEvent(CardPaneModelEvent cpme){
        for(CardPaneModelListener li : this.mListeners){
            li.cardPaneModelChanged(cpme);
        }
    }

    private Set<Integer> mChangedListRecords = new HashSet<>();

    /**
     *
     *
     *
     * @param e
     */
    @Override
    public void valueChanged(ListSelectionEvent e) {
        if(e.getFirstIndex()>=0) {
            for (int zi = e.getFirstIndex(); zi <= e.getLastIndex(); zi++) {
                mChangedListRecords.add(zi);
            }
        }
        else{
            // hmm.. add all(?)
            for (int zi = 0 ; zi <= mCTM.getTotalRowCount(); zi++) {
                mChangedListRecords.add(zi);
            }
        }
        if( e.getValueIsAdjusting() ){
        }
        else{
            // start thread which computes the ces which have changed selection..
            NotifySelectionChangedThread nsct = new NotifySelectionChangedThread(  new ArrayList<Integer>(mChangedListRecords), true);
            mChangedListRecords.clear();
            nsct.start();
        }
    }



    public synchronized int getFlagExclusion(){return this.mFlagExclusion;}

    public CardPositionerInterface getPositioner() {
        return mPositioner;
    }

    public TFactory getCardElementFactory(){
        return this.mFactory;
    }



    public synchronized CardPaneModel<T,TFactory> cloneModel() {
        return new CardPaneModel(this);
    }

    public CompoundTableModel getTableModel(){
        return this.mCTM;
    }


    @Override
    public void onCardPaneEvent(CardPaneEvent cpe) {
        if( cpe.getType() == CardPaneEvent.EventType.VISIBLE_ELEMENTS_CHANGED ){
            this.mViewport = cpe.getViewport();
            //if(this.mRowHiding){
            this.updateRowHiding(); // !!Always necessary!!
            //}
        }
    }

    public synchronized void updateRowHiding(){
        if(!this.mRowHiding){
            if(this.mFlagExclusion>=0){
                //mCTM.clearRowFlag();
                this.mAllElements.stream().forEach( ci -> ci.getAllRecords().stream().forEach( ri -> ri.clearFlag(mFlagExclusion)));
                mCTM.updateExternalExclusion(mFlagExclusion, false , true );
            }
            return;
        }

//        List<T> hiddenCards    = this.mAllElements.keySet().parallelStream().filter( kce -> !this.mViewport.contains( kce.getPosX() , kce.getPosY()) ).collect(Collectors.toList());
//        List<T> nonhiddenCards = this.mAllElements.keySet().parallelStream().filter( kce -> this.mViewport.contains( kce.getPosX() , kce.getPosY()) ).collect(Collectors.toList());

        List<T> hiddenCards    = this.mAllElements.parallelStream().filter( kce -> !this.mViewport.contains( kce.getPosX() , kce.getPosY()) ).collect(Collectors.toList());
        List<T> nonhiddenCards = this.mAllElements.parallelStream().filter( kce -> this.mViewport.contains( kce.getPosX() , kce.getPosY()) ).collect(Collectors.toList());

        if(this.mFlagExclusion>=0) {
            hiddenCards.parallelStream().forEach(hci -> hci.getAllRecords().stream().forEach(ri -> ri.setFlag(mFlagExclusion)));
            nonhiddenCards.parallelStream().forEach(hci -> hci.getAllRecords().stream().forEach(ri -> ri.clearFlag(mFlagExclusion)));
        }
        //System.out.println("Nonhidden: "+ hiddenCards.size() +"  Hidden: "+nonhiddenCards.size());

        mCTM.updateExternalExclusion(mFlagExclusion, false , true );
    }


    /**
     * Automatically builds a grid around all card elements (i.e. around the centers of all card elements).
     * The grid consists of equally sized bins which overlap by a certain amount that can be specified.
     * A card element is contained in a bin if it's CENTER is contained in the bin.
     *
     * @param bin_width
     * @param bin_height
     * @param overlap_x
     * @param overlap_y
     * @return
     */
    public synchronized BinnedCards getBinned2D_Nonexcluded(double bin_width, double bin_height, double overlap_x, double overlap_y) {
        return createBins2D( this.getReducedAllElements(),bin_width,bin_height,overlap_x,overlap_y );
    }

    /**
     * Automatically builds a grid around all card elements (i.e. around the centers of all card elements).
     * The grid consists of equally sized bins which overlap by a certain amount that can be specified.
     * A card element is contained in a bin if it's CENTER is contained in the bin.
     *
     * @param bin_width
     * @param bin_height
     * @param overlap_x
     * @param overlap_y
     * @return
     */
    public synchronized BinnedCards getBinned2D_All(double bin_width, double bin_height, double overlap_x, double overlap_y) {
        return createBins2D( this.getAllElements(),bin_width,bin_height,overlap_x,overlap_y );
    }

    /**
     * Automatically builds a grid around all card elements (i.e. around the centers of all card elements).
     * The grid consists of equally sized bins which overlap by a certain amount that can be specified.
     * A card element is contained in a bin if it's CENTER is contained in the bin.
     *
     * NOTE! This can be easily used for computing 2D bins WITHOUT any overlap. For this, we can just set both values
     * to zero. Then, the two x-binning / the two y-binning functions are equal, and you can just use one of them
     * to bin.
     *
     */
    //private Map<IntOrderedPair,List<T>> createBins2D(List<T> cardElements, double bin_width, double bin_height, double overlap_x, double overlap_y) {
    private BinnedCards createBins2D(List<T> cardElements, double bin_width, double bin_height, double overlap_x_, double overlap_y_) {

        // by definition we put a padding of 0.1 * overlap_x, and 0.1 * overlap_y around the min/max x/y values:

        double overlap_x = Math.abs(overlap_x_);
        double overlap_y = Math.abs(overlap_y_);

        DoubleSummaryStatistics dss_x = cardElements.stream().mapToDouble( ci -> ci.getPosX() ).summaryStatistics();
        DoubleSummaryStatistics dss_y = cardElements.stream().mapToDouble( ci -> ci.getPosY() ).summaryStatistics();

        double x0 = dss_x.getMin()-0.1*overlap_x;
        double y0 = dss_y.getMin()-0.1*overlap_y;
        //double x1 = dss_x.getMax()+0.1*overlap_x;
        //double y1 = dss_y.getMax()+0.1*overlap_y;

        // bins including left overlap..
        Function<Double,Integer> f_binning_x_a = ( cex -> (int) Math.floor(( cex - x0 ) / bin_width ) );
        // bins including right overlap..
        Function<Double,Integer> f_binning_x_b = ( cex -> (int) Math.floor(( cex - x0 + overlap_x ) / bin_width ) );

        // bins including top overlap..
        Function<Double,Integer> f_binning_y_a = ( cey -> (int) Math.floor(( cey - y0 ) / bin_height ) );
        // bins including bottom overlap..
        Function<Double,Integer> f_binning_y_b = ( cey -> (int) Math.floor(( cey - y0 + overlap_y ) / bin_height ) );


        Map<IntOrderedPair,List<T>> binnedMap = new HashMap<>();

        for( T ce : cardElements ){
            int xa = f_binning_x_a.apply(ce.getPosX());
            int xb = f_binning_x_b.apply(ce.getPosX());
            int ya = f_binning_y_a.apply(ce.getPosY());
            int yb = f_binning_y_b.apply(ce.getPosY());

            if(xa==xb){
                if(ya==yb){
                    addToMapList(binnedMap,xa,ya,ce);
                }
                else{
                    addToMapList(binnedMap,xa,ya,ce);
                    addToMapList(binnedMap,xa,yb,ce);
                }
            }
            else{
                if(ya==yb){
                    addToMapList(binnedMap,xa,ya,ce);
                    addToMapList(binnedMap,xb,ya,ce);
                }
                else{
                    addToMapList(binnedMap,xa,ya,ce);
                    addToMapList(binnedMap,xa,yb,ce);
                    addToMapList(binnedMap,xb,ya,ce);
                    addToMapList(binnedMap,xb,yb,ce);
                }
            }
        }

        boolean overlapIsZero = (overlap_x<=0) && (overlap_y<=0);

        return new BinnedCards( !overlapIsZero , binnedMap , f_binning_x_a , f_binning_x_b , f_binning_y_a , f_binning_y_b );
    }

    private void addToMapList( Map<IntOrderedPair,List<T>> map, int x, int y, T ce ){
        List<T> mxi = map.get(new IntOrderedPair(x,y));
        if(mxi==null){
            mxi = new ArrayList<>();
            map.put( new IntOrderedPair(x,y),mxi);
        }
        mxi.add(ce);
    }




    /**
     * Returns the flag in CompoundTableModel
     */
    public void cleanup(){
        if(this.mFlagExclusion>=0){
            this.getTableModel().freeRowFlag(mFlagExclusion);
        }
    }




//
//    /**
//     * Creates a submodel for a given stack.
//     * Changes (i.e. added cards / removed cards) to the SUBMODEL will be propagated to the governing stack.
//     * The other way, (i.e. changes to the original stack) are NOT propagated.
//     *
//     * NOTE! The submodel will only contain the nonexcluded compound records.
//     *
//     * @return
//     */
//    public CardPaneModel<T,TFactory> createSubModelFromStack_nonexcluded( CardElementInterface stack ){
//        CardPaneModel<T,TFactory> sm = new CardPaneModel<>(this.mCTM,this.mListSelectionModel,this.mFactory);
//        stack.getNonexcludedRecords().stream().forEach( cri -> sm.add )
//    }


    /**
     * Listens to add and remove events in the stackmodel and propagates them to the stack. I.e. removing cards from
     * the stackmodel will remove cards from the stack, same for adding.
     *
     * Assumption: stack is a card element in the parent_model.
     *
     * @param stackmodel
     * @param stack
     */
    public static void propagateAddAndRemoveToStack( CardPaneModel stackmodel , CardPaneModel parent_model , CardElementInterface stack ){
        stackmodel.registerListener(new CardPaneModelListener(){
            @Override
            public void cardPaneModelChanged(CardPaneModelEvent e) {
                if(e.getType()== CardPaneModelEvent.EventType.DELETED_CARD_ELEMENT){
                    if(e.getCards().size()!=1){
                        System.out.println("!!ERROR!! [CardPaneModel::propagateAndRemoveToStack] -> captured DELETED_CARD_ELEMENT contains wrong number of cards..");
                    }
                    parent_model.removeRecordsFromCE( stack , e.getCompoundRecords() );
                }
                if(e.getType()== CardPaneModelEvent.EventType.NEW_CARD_ELEMENT){
                    if(e.getCards().size()!=1){
                        System.out.println("!!ERROR!! [CardPaneModel::propagateAndRemoveToStack] -> captured NEW_CARD_ELEMENT contains wrong number of cards..");
                    }
                    parent_model.addRecordsToCE( stack , e.getCards().get(0).getNonexcludedRecords() );
                }
            }
        });
    }

    /**
     * 2d-binned representation of hte CardElements
     */
    public class BinnedCards {

        /**
         * If there is no overlap, then the getAllElementsOfAllBins computation is faster, as we need less binning
         * function evaluations, because the a/b versions are the same.
         */
        private final boolean mWithOverlap;

        private Map<IntOrderedPair,List<T>> mBinnedCards;

        // bins including left overlap..
        private Function<Double,Integer> mBinningFunction_X_a;
        // bins including right overlap..
        private Function<Double,Integer> mBinningFunction_X_b;

        // bins including top overlap..
        private Function<Double,Integer> mBinningFunction_Y_a;
        // bins including bottom overlap..
        private Function<Double,Integer> mBinningFunction_Y_b;

        public Map<IntOrderedPair,List<T>> getBinnedCards(){ return mBinnedCards; }

        /**
         * Collect all elements (without overlap from one bin, else from 1-4 bins).
         *
         * @param x
         * @param y
         * @return
         */
        public List<T> getAllElements( double x, double y ) {

            if(!mWithOverlap){
                int xa = mBinningFunction_X_a.apply(x);
                int xb = mBinningFunction_X_b.apply(x);
                int ya = mBinningFunction_Y_a.apply(y);
                int yb = mBinningFunction_Y_b.apply(y);

                ArrayList<T> result = new ArrayList<>();
                if(xa==xb){
                    if(ya==yb){
                        result.addAll(mBinnedCards.get(new IntOrderedPair(xa,ya)));
                    }
                    else{
                        result.addAll(mBinnedCards.get(new IntOrderedPair(xa,ya)));
                        result.addAll(mBinnedCards.get(new IntOrderedPair(xa,yb)));
                    }
                }
                else{
                    if(ya==yb){
                        result.addAll(mBinnedCards.get(new IntOrderedPair(xa,ya)));
                        result.addAll(mBinnedCards.get(new IntOrderedPair(xb,ya)));
                    }
                    else{
                        result.addAll(mBinnedCards.get(new IntOrderedPair(xa,ya)));
                        result.addAll(mBinnedCards.get(new IntOrderedPair(xa,yb)));
                        result.addAll(mBinnedCards.get(new IntOrderedPair(xb,ya)));
                        result.addAll(mBinnedCards.get(new IntOrderedPair(xb,yb)));
                    }
                }
            }
            else{
                return mBinnedCards.get( new IntOrderedPair( mBinningFunction_X_a.apply(x) , mBinningFunction_Y_a.apply(y) ) );
            }
            // we cannot end up here
            return null;
        }

        /**
         * Collects cards from the bin which contains the coordiante, and from all (up to eight) neighboring bins.
         *
         * @return
         */
        public List<T> getAllElementsInNeighborhood(double x, double y){
           if(mWithOverlap) {
               System.out.println("[CardPaneModel::BinnedCards] WARNING! getAllElementsInNeighborhood does not really work for BinnedCards with overlap..");
           }

           ArrayList<T> result = new ArrayList<>();

           int xi = mBinningFunction_X_a.apply(x);
           int yi = mBinningFunction_Y_a.apply(y);

           List<T> elements_i = null;
           elements_i = mBinnedCards.get( new IntOrderedPair(xi, yi) ); if( elements_i!=null ){ result.addAll(elements_i); }
           elements_i = mBinnedCards.get( new IntOrderedPair(xi, yi+1) ); if( elements_i!=null ){ result.addAll(elements_i); }
           elements_i = mBinnedCards.get( new IntOrderedPair(xi, yi-1) ); if( elements_i!=null ){ result.addAll(elements_i); }
           elements_i = mBinnedCards.get( new IntOrderedPair(xi-1, yi) ); if( elements_i!=null ){ result.addAll(elements_i); }
           elements_i = mBinnedCards.get( new IntOrderedPair(xi-1, yi+1) ); if( elements_i!=null ){ result.addAll(elements_i); }
           elements_i = mBinnedCards.get( new IntOrderedPair(xi-1, yi-1) ); if( elements_i!=null ){ result.addAll(elements_i); }
           elements_i = mBinnedCards.get( new IntOrderedPair(xi+1, yi) ); if( elements_i!=null ){ result.addAll(elements_i); }
           elements_i = mBinnedCards.get( new IntOrderedPair(xi+1, yi+1) ); if( elements_i!=null ){ result.addAll(elements_i); }
           elements_i = mBinnedCards.get( new IntOrderedPair(xi+1, yi-1) ); if( elements_i!=null ){ result.addAll(elements_i); }

           return result;
        }

        public Function<Double,Integer> getBinningFunction_XA() {return mBinningFunction_X_a;}
        public Function<Double,Integer> getBinningFunction_XB() {return mBinningFunction_X_b;}
        public Function<Double,Integer> getBinningFunction_YA() {return mBinningFunction_Y_a;}
        public Function<Double,Integer> getBinningFunction_YB() {return mBinningFunction_Y_b;}

        public BinnedCards(boolean withOverlap , Map<IntOrderedPair,List<T>> binnedCards , Function<Double,Integer> binningFunction_X_a , Function<Double,Integer> binningFunction_X_b , Function<Double,Integer> binningFunction_Y_a , Function<Double,Integer> binningFunction_Y_b ) {
            this.mWithOverlap = withOverlap;
            this.mBinnedCards = binnedCards;
            this.mBinningFunction_X_a = binningFunction_X_a;
            this.mBinningFunction_X_b = binningFunction_X_b;
            this.mBinningFunction_Y_a = binningFunction_Y_a;
            this.mBinningFunction_Y_b = binningFunction_Y_b;
        }


    }

    public static class ListOfCompoundRecords {
        private List<CompoundRecord> mCRs = null;

        /**
         * This can also be null..
         */
        private CardElementInterface mCE = null;

        public ListOfCompoundRecords( Collection<CompoundRecord> crs ) {
            this.mCRs = new ArrayList<>(crs);
        }
        public ListOfCompoundRecords( Collection<CompoundRecord> crs , CardElementInterface ce) {
            this.mCRs = new ArrayList<>(crs);
            this.mCE  = ce;
        }


        public List<CompoundRecord>  getCRs() {return mCRs;}
        public CardElementInterface  getCardElement() {return mCE;}
    }

    public static class ListOfListOfCompoundRecords implements Transferable {
        private List<ListOfCompoundRecords> mList = null;

        public ListOfListOfCompoundRecords( Collection<ListOfCompoundRecords> crs ){
            this.mList = new ArrayList<>(crs);
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{ listOfListOfCompoundRecordsFlavor };
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            if(flavor.equals(listOfListOfCompoundRecordsFlavor)){
                return true;
            }
            return false;
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if( flavor.equals( listOfListOfCompoundRecordsFlavor )  ){
                return this;
            }

            return null;
        }

        public List<ListOfCompoundRecords> getListOfListOfCompoundRecords(){return this.mList;}

        private Image mDragImage = null;

        public void setDragImage(Image img) {
            mDragImage = img;
        }
        public Image getDragImage() {
            return mDragImage;
        }

        /**
         * Assumes that data is a list List<List<CompoundRecord>> and creates the object.
         *
         * @return
         */
        public static ListOfListOfCompoundRecords createFromListOfListOfCompoundRecords(List<List<CompoundRecord>> data){

            List<ListOfCompoundRecords> cr = new ArrayList<>();

            for( List<CompoundRecord> o : data ){
                cr.add( new ListOfCompoundRecords(o) );
            }
            return new ListOfListOfCompoundRecords(cr);
        }

        /**
         * Assumes that data is a list List<CardElement> and creates the object.
         * NOTE! Adds only all nonexcluded compound records
         *
         * @return
         */
        public static ListOfListOfCompoundRecords createFromListOfListOfCardElements_Nonexcluded(List<CardElementInterface> data){
            List<ListOfCompoundRecords> cr = new ArrayList<>();
            for( CardElementInterface o : data ){
                cr.add( new ListOfCompoundRecords(o.getNonexcludedRecords() , o) );
            }
            return new ListOfListOfCompoundRecords(cr);
        }

    }

    public final static DataFlavor listOfListOfCompoundRecordsFlavor = new DataFlavor(ListOfListOfCompoundRecords.class,"ListOfListOfCompoundRecords");


    public static interface CardElementInterface {

        public boolean isCard();
        public boolean isStack();

        public boolean isEmptyAfterExclusion();
        public boolean isCardAfterExclusion();
        public boolean isStackAfterExclusion();

        public String getStackName();
        public void   setStackName(String name);

        public double[] getDimension();
        public void setDimension(double dim[]);

        public double getPosX();
        public double getPosY();
        public void   setPosX(double px);
        public void   setPosY(double py);


        public int getZ();
        public double getAngle();

        /**
         * Returns all records, including the externally excluded ones.
         *
         * @return list of all records
         */
        public List<CompoundRecord> getAllRecords();

        /**
         * Returns only the records which are not externally excluded.
         *
         * @return list of non-externally excluded records
         */
        public List<CompoundRecord> getNonexcludedRecords();

        /**
         * Returns only the records which are not externally excluded and non-greyed out.
         *
         * @return list of non-externally excluded, non-greyed out records
         */
        public List<CompoundRecord> getNonExcludedNonGreyedOutRecords();

        /**
         * Returns only the records which are not externally excluded and greyed out.
         *
         * @return
         */
        public List<CompoundRecord> getNonExcludedGreyedOutRecords();


        /**
         * This tests:
         *      getNonexcludedRecords().size() > getAllNonExcludedNonGreyedOutRecords.size()
         * @return
         */
        public boolean containsNonexcludedGreyedOutRecords();

        public void setRecords(List<CompoundRecord> r);
        public void addRecord(CompoundRecord r);
        public void addRecords(List<CompoundRecord> r);
        public void removeRecord(CompoundRecord r);
        public void removeRecords(Collection<CompoundRecord> r);
        public void removeRecordsByTotalID(Set<Integer> r);


        public void setExternallyExcludedRecords( Collection<CompoundRecord> rxs );

        public void setGreyedOutRecords( Collection<CompoundRecord> rs );


        public CardElementInterface copy();
    }

    public static interface CardElementInterfaceFactory<TT>{
        public TT createCardElement( CompoundRecord r );
        public TT createCardElement( List<CompoundRecord> r );
        public TT clone(TT element);
    }

    public static interface CardPaneModelListener {
        public void cardPaneModelChanged(CardPaneModelEvent e);
    }


    public class NotifySelectionChangedThread extends FindCEsForRecordsThread {
        public NotifySelectionChangedThread(List<Integer> cr, boolean onlyVisible){
            super(cr,onlyVisible);
        }

        @Override
        public void processCEs(List<CardElementInterface> ces) {
            ces.stream().forEach( ce -> fireCardPaneModelEvent( new CardPaneModelEvent( CardPaneModelEvent.EventType.CARD_ELEMENT_CHANGED , ce )));
        }
    }


    public abstract class FindCEsForRecordsThread extends Thread{

        private List<Integer> mDirtyRecordIDs;
        private boolean mOnlyVisible = false;
        private List<CardElementInterface> mElementsToCheck;

        private List<CardElementInterface> mFoundCEs;

        /**
         * Is called after computation of CEs if finished.
         *
         * @param ces
         */
        public abstract void processCEs( List<CardElementInterface> ces );

//        public FindCEsForRecordsThread(List<CompoundRecord> records, boolean onlyVisible) {
//            this.mDirtyRecordIDs = records.stream().mapToInt( ri -> ri.getID() ).boxed().collect(Collectors.toList());
//            this.mOnlyVisible = onlyVisible;
//            if(!onlyVisible){ mElementsToCheck = new ArrayList<>(getAllElements());}
//            else{ mElementsToCheck = new ArrayList<>(getReducedAllElements()); }
//        }

        public FindCEsForRecordsThread(List<Integer> recordIDs, boolean onlyVisible) {
            this.mDirtyRecordIDs = new ArrayList<>(recordIDs);
            this.mOnlyVisible = onlyVisible;
            if(!onlyVisible){ mElementsToCheck = new ArrayList<>(getAllElements());}
            else{ mElementsToCheck = new ArrayList<>(getReducedAllElements()); }
        }


        @Override
        public void run(){
            mFoundCEs = new ArrayList<>();

            // add elements with dirty records:
            if(!mDirtyRecordIDs.isEmpty()) {
//                LinkedList<Integer> dirtyRecs_A = new LinkedList<>();
//                while ( ! mDirtyRecords.isEmpty() ) {
//                    dirtyRecs_A.add(mDirtyRecords.pollFirst());
//                }
                Set<Integer> dirtyRecs = new HashSet<>(mDirtyRecordIDs); ; //mDirtyRecordIDs.stream().mapToInt( di -> di.getID() ).boxed().collect(Collectors.toSet());

                if(logLevel>0) {
                    System.out.println("FindCEsForRecordsThread : Dirty records: " + dirtyRecs.size());
                }

                List<CardElementInterface> ces = mElementsToCheck;
                List<CardElementInterface> cesWithDirtyRecs = new ArrayList<>();
                for (CardElementInterface cei : ces) {
                    if(cei==null){continue;}
                    if (cei.getNonexcludedRecords().size() == 1) {
                        if (dirtyRecs.contains(cei.getNonexcludedRecords().get(0).getID())) {
                            cesWithDirtyRecs.add(cei);
                        }
                        continue;
                    } else {
                        if (cei.getNonexcludedRecords().stream().filter(cr -> dirtyRecs.contains(cr.getID())).count() > 0) {
                            cesWithDirtyRecs.add(cei);
                        }
                    }
                }
                if(logLevel>0) {
                    System.out.println("FindCEsForRecordsThread : CEs with dirty CRs: " + cesWithDirtyRecs.size());
                }
                mFoundCEs.addAll( cesWithDirtyRecs );
            }
            else
            {
                if(logLevel>0) {
                    System.out.println("FindCEsForRecordsThread : No dirty records..");
                }
            }

            processCEs(mFoundCEs);
        }
    }


    public static class CardElementData {
        private List<Integer> mRecords;
        private double mPosX;
        private double mPosY;
        private String mStackName = "";

        public CardElementData(List<Integer> records, double px, double py){
            this.mRecords = new ArrayList<>(records);
            this.mPosX = px;
            this.mPosY = py;
        }

        public void   setStackName(String s) { this.mStackName = s;}
        public String getStackName() { return this.mStackName;}

        public List<CompoundRecord> getCompoundRecords(CompoundTableModel ctm){
            List<CompoundRecord> crs = new ArrayList<>();
            mRecords.stream().forEachOrdered( ei -> crs.add( ctm.getTotalRecord(ei)) );
            return crs;
        }

        public Point2D getPos(){return new Point2D.Double(mPosX,mPosY);}
    }

    public static class CardPaneData {
        private List<CardElementData> mElements;
        public CardPaneData(List<CardElementData> elements){
            this.mElements = new ArrayList<>(elements);
        }
    }

}



