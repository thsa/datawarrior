package com.actelion.research.table.view.card.positioning;

import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.view.card.CardElement;
import com.actelion.research.table.view.card.CardPaneModel;
import com.actelion.research.table.view.card.JCardPane;
import com.actelion.research.table.view.card.JFastCardPane;
import com.actelion.research.table.view.card.animators.PathAnimator;
import com.actelion.research.table.view.card.cardsurface.gui.JMyOptionsTable;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;
import smile.clustering.HierarchicalClustering;
import smile.clustering.linkage.CompleteLinkage;
import smile.clustering.linkage.SingleLinkage;
import smile.clustering.linkage.UPGMALinkage;
import smile.clustering.linkage.WPGMALinkage;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Point2d;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class JCardSortingPanel extends JPanel implements SortingConfigurationListener , ActionListener , ChangeListener {


    private final static double CONF_TIME_ANIMATION = 0.5;


    private JSplitPane mSplitPaneA = null;

    private JPanel     mLeftPanel    = null;
    private JPanel     mRightPanel   = null;

    private JPanel     mGeneralSettingsPanel = null;


    /**
     * Here we store a cloned version of the CPM when the dialog is initialized.
     * NOTE! Whenever we compute new positions for the JFastCardPane, we create a NEW CPM (!), starting from the
     * mOriginalModel
     */
    private CardPaneModel mOriginalModel = null;

    private JFastCardPane mFastPane  = null;

//    private java.util.List<CardElement> mElements = null;


    JTabbedPane mSorterPane = null;


    private CompoundTableModel mModel = null;


    /**
     * We only start positioning on all GUI stateChanged events after the dialog is up.
     */
    private boolean mPositioningEnabled = false;


    private RecomputePositionsThread mPositionComputeThread = null;


    //@TODO remove these three..
    private JComboBox mComboBoxPositioners = new JComboBox();
    private JCheckBox mCheckBoxDissolveStacks = new JCheckBox();
    private JSlider   mSliderCreateStacks = new JSlider();

    private JMyOptionsTable mClusteringOptions;

    private JButton   mButtonApply = new JButton();


    //private List<String> mPositioners = null;
    private List<CardPositionerInterface> mPositioners = null;

    private CardPositionerInterface mPositioner = null;

    private final static String ACTION_APPLY_BUTTON_PRESSED = "ApplyButtonPressed";

    private final static String PROPERTY_DISSOLVE_STACKS    = "DissolveStacks";
    private final static String PROPERTY_CLUSTERING_METHOD  = "ClusteringMethod";
    private final static String PROPERTY_NUM_CLUSTERS       = "NumClusters";

    private final static String NAMES_CLUSTERING_METHODS[] = new String[]{ "None","UPGMA" ,"WPGMA", "Single","Complete"};


    private JCardPane mCardPane = null;


    //private List<CardElement> mOriginalElements = null;


    public JCardSortingPanel(JCardPane cardPane , CompoundTableModel model, List<CardElement> elements){

        this.mCardPane = cardPane;
        this.mModel = model;

        this.mOriginalModel = cardPane.getCardPaneModel().cloneModel();

        //this.mOriginalElements = new ArrayList<>(elements);

        init();
        initPositioners();

        // set initial positioner..
        this.mPositioner = this.mPositioners.get(0);

        this.mPositioningEnabled = true;
    }



    final static private String ACTION_COMMAND_SORTER_COMBOBOX = "SorterComboBoxChanged";


    public void initPositioners(){
//        mPositioners = new ArrayList<String>();
//        mPositioners.add(XYSorter.NAME);
//        mPositioners.add(XSorter.NAME);

        // init sorters:
        CardPositionerWithGUI sorter1d = new XSorter(mModel,getElements());
        CardPositionerWithGUI sorter2d = new XYSorter(mModel,getElements());

        CardPositionerWithGUI sorter2d_magic = new MagicSorter2D(mModel,getElements());

        CardPositionerWithGUI sorter_stacks  = new CardStackPositioner(mModel,getElements());

        sorter1d.registerSortingConfigListener(this);
        sorter2d.registerSortingConfigListener(this);
        sorter_stacks.registerSortingConfigListener(this);

        // init tabbed pane and list of positioners
        this.mPositioners = new ArrayList<>();

        this.mPositioners.add(sorter1d);
        mSorterPane.add("1D Sorting",  sorter1d.getGUI() );

        this.mPositioners.add(sorter2d);
        mSorterPane.add("Scatter X/Y",  sorter2d.getGUI() );

        this.mPositioners.add(sorter_stacks);
        mSorterPane.add( "Stacks", sorter_stacks.getGUI());

        this.mPositioners.add(sorter2d_magic);
        mSorterPane.add( "2D Sorting", sorter2d_magic.getGUI());

    }

    public void init(){
        //this.mSplitPaneA = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);


        JPanel mContainer = new JPanel();

        int gap8 = HiDPIHelper.scale(8);

        double tableLayoutConfig[][] = new double[][] { { gap8 , TableLayout.PREFERRED , gap8 , TableLayout.PREFERRED , gap8 } , { gap8 , TableLayout.PREFERRED , gap8 , TableLayout.PREFERRED , gap8 } };
        mContainer.setLayout(new TableLayout(tableLayoutConfig));

        this.mLeftPanel = new JPanel();
        this.mRightPanel = new JPanel();

        JPanel mPanelRightLow = new JPanel();

        //this.mSplitPaneA.setLeftComponent(mLeftPanel);
        //this.mSplitPaneA.setRightComponent(mRightPanel);
        mContainer.add(mLeftPanel,"1,1");
        mContainer.add(mRightPanel,"3,1");
        mContainer.add(mPanelRightLow,"3,3");

        this.mLeftPanel.setLayout(new BoxLayout(this.mLeftPanel,BoxLayout.Y_AXIS));

        // init the JFastPane..
        mFastPane = new JFastCardPane( this.mCardPane.getCardPaneModel().cloneModel() , JFastCardPane.FAST_PANE_MODE.AUTONOMOUS_FROM_REDUCED );

        //mFastPane.setUpdateRate(25);
        mFastPane.setInternalBufferingEnabled(false);

        mFastPane.setViewportMovement(false);

        //mFastPane.setSize(400,400);
        mFastPane.setMinimumSize(new Dimension(400,400));
        mFastPane.setPreferredSize(new Dimension(400,400));
        this.mLeftPanel.add(mFastPane);

        this.mGeneralSettingsPanel = new JPanel();
        this.mGeneralSettingsPanel.setMinimumSize(new Dimension(200,400));
        this.mGeneralSettingsPanel.setLayout(new BoxLayout(this.mGeneralSettingsPanel,BoxLayout.Y_AXIS));

//        JPanel comboboxPanel = new JPanel();
//        comboboxPanel.setLayout(new BoxLayout(comboboxPanel,BoxLayout.X_AXIS));
//        comboboxPanel.add(new JLabel("Positioner: "));
//        comboboxPanel.add(mComboBoxPositioners);
//        mComboBoxPositioners.setSize(200,20);
//        mComboBoxPositioners.setMaximumSize( new Dimension(600,20));
//        mComboBoxPositioners.addActionListener(new MyComboBoxListener());

//        mComboBoxPositioners.setActionCommand(ACTION_COMMAND_SORTER_COMBOBOX);
//        for(String pName : this.mPositioners){
//            mComboBoxPositioners.addItem(pName);
//        }

//        this.mGeneralSettingsPanel.add(comboboxPanel);

        //mClusteringOptions = new JMyOptionsTable(2,2);
        mClusteringOptions = new JMyOptionsTable();
        mClusteringOptions.setCheckBox(0,0, PROPERTY_DISSOLVE_STACKS,"Dissolve stacks: ", false);
        mClusteringOptions.deactivate(0,0);

        mClusteringOptions.setComboBox(0,1, PROPERTY_CLUSTERING_METHOD,"Clustering: ", Arrays.stream(NAMES_CLUSTERING_METHODS).collect(toList()),NAMES_CLUSTERING_METHODS[0]);
        // NOTE! We actually set the mClusteringOptions_numClusters value to 1.0 - this value!
        // i.e. slider at 0.0 means: mClusteringOptions_numCluster=1.0 --> no clustering (i.e. max number of clusters)
        mClusteringOptions.setSlider(1,1, PROPERTY_NUM_CLUSTERS,"Num. Clusters:", 0,1,0.2);

        mClusteringOptions.reinit();
        mClusteringOptions.deactivate(1,1);

        mClusteringOptions.registerOptionTableListener( new MyClusteringOptionsTableListener() );

        this.mGeneralSettingsPanel.add(mClusteringOptions);

//        JPanel dissolveStacksPanel = new JPanel();
//        dissolveStacksPanel.setLayout(new BoxLayout(dissolveStacksPanel,BoxLayout.X_AXIS));
//        dissolveStacksPanel.add(new JLabel("Dissolve Stacks "));
//        dissolveStacksPanel.add(mCheckBoxDissolveStacks);
//        this.mGeneralSettingsPanel.add(dissolveStacksPanel);
//
//
//        JPanel createStacksPanel = new JPanel();
//        createStacksPanel.setLayout(new BoxLayout(createStacksPanel,BoxLayout.X_AXIS));
//        createStacksPanel.add(new JLabel("Create Stacks "));
//        mSliderCreateStacks.setMinimum(0);
//        mSliderCreateStacks.setMaximum(100);
//        mSliderCreateStacks.setValue(0);
//        mSliderCreateStacks.setSize(200,20);
//        mSliderCreateStacks.setMaximumSize( new Dimension(600,20));
//        mSliderCreateStacks.addChangeListener(this);
//        createStacksPanel.add(mSliderCreateStacks);
//
//        this.mGeneralSettingsPanel.add(createStacksPanel);
//
//
//
        this.mLeftPanel.add(this.mGeneralSettingsPanel);

        //this.setLayout(new BorderLayout());
        //this.add(mSplitPaneA,BorderLayout.CENTER);


        mButtonApply = new JButton("APPLY!");
        mButtonApply.setSize(200,20);
        mButtonApply.setActionCommand(ACTION_APPLY_BUTTON_PRESSED);
        mButtonApply.addActionListener(this);
        //this.mLeftPanel.add(mButtonApply);
        mPanelRightLow.setLayout(new BorderLayout());
        JPanel mPanelRightLow2 = new JPanel(); mPanelRightLow2.setLayout(new BorderLayout());
        mPanelRightLow.add(mPanelRightLow2,BorderLayout.SOUTH);
        mPanelRightLow2.add(mButtonApply,BorderLayout.EAST);


        this.revalidate();
        this.repaint();

        // init right panel..
        mSorterPane = new JTabbedPane();

        mSorterPane.addChangeListener(this);

        this.mRightPanel.setLayout(new BoxLayout(mRightPanel, BoxLayout.Y_AXIS));
        this.mRightPanel.add(mSorterPane);

//        Window windowAncestor = SwingUtilities.getWindowAncestor(this);
//        if(windowAncestor!=null){
//            windowAncestor.pack();
//        }


        this.setLayout(new BorderLayout());
        this.add(mContainer,BorderLayout.CENTER);

        this.revalidate();
        this.repaint();
    }


    HierarchicalClustering mHClustering = null;


    private double[][] computeProximity(){

        List<CardPaneModel.CardElementInterface> elements = new ArrayList<>( mFastPane.getCardPaneModel().getReducedAllElements() );

        int n = elements.size();
        double prox[][] = new double[n][n];

        int xa=0; int xb = 0;
        for(CardPaneModel.CardElementInterface eia : elements){
            for(CardPaneModel.CardElementInterface eib : elements){
                prox[xa][xb] = Math.sqrt( (eia.getPosX() - eib.getPosX())*(eia.getPosX() - eib.getPosX()) + (eia.getPosY() - eib.getPosY())*(eia.getPosY() - eib.getPosY()) );
                xb++;
            }
            xa++;
            xb = 0;
        }
        return prox;
    }

    private double[][] computeProximity( List<Point2D> pos ){
        int n = pos.size();
        double prox[][] = new double[n][n];

        int xa=0; int xb = 0;
        for(Point2D eia : pos){
            for(Point2D eib : pos){
                prox[xa][xb] = Math.sqrt( (eia.getX() - eib.getX())*(eia.getX() - eib.getX()) + (eia.getY() - eib.getY())*(eia.getY() - eib.getY()) );
                xb++;
            }
            xa++;
            xb = 0;
        }
        return prox;
    }

    private boolean  mClusteringOptions_dissolveStacks = false;
    private String   mClusteringOptions_clusterMethod  = NAMES_CLUSTERING_METHODS[0];
    private double   mClusteringOptions_numcCluster    = 1.0; // 1.0=one cluster, 0.0=n clusters


    private int[] getZeroClustering(int nAll){
        int noClustering[] = new int[nAll];
        for(int zi=0;zi<nAll;zi++){
            noClustering[zi] = zi;
        }
        return noClustering;
    }

    /**
     * Recomputes the clustering, based on the mClusteringOptions_x settings, and the current CardElement positions in the
     * mFastCardPane.
     *
     * I.e. call when options change, or when CardElement positions changed.
     *
     */
    public int[] recomputeClustering(double proximity[][]){


        // number of card elements..
        int nAll = proximity.length;

        if(mClusteringOptions_clusterMethod.equals( NAMES_CLUSTERING_METHODS[0] )){
            // return no clustering..
            return getZeroClustering(nAll);
        }

        if(mClusteringOptions_clusterMethod.equals( NAMES_CLUSTERING_METHODS[1] )) {
            mHClustering = new HierarchicalClustering(new UPGMALinkage(proximity));
        }
        if(mClusteringOptions_clusterMethod.equals( NAMES_CLUSTERING_METHODS[2] )) {
            mHClustering = new HierarchicalClustering(new WPGMALinkage(proximity));
        }
        if(mClusteringOptions_clusterMethod.equals( NAMES_CLUSTERING_METHODS[3] )) {
            mHClustering = new HierarchicalClustering(new SingleLinkage(proximity));
        }
        if(mClusteringOptions_clusterMethod.equals( NAMES_CLUSTERING_METHODS[4] )) {
            mHClustering = new HierarchicalClustering(new CompleteLinkage(proximity));
        }

        // compute num clusters:
        int nTargetClusters = (int)  ((mClusteringOptions_numcCluster) * nAll);
        int clustering[] = mHClustering.partition(nTargetClusters);

        System.out.println("computed new clustering: nclusters="+clustering.length);
        return clustering;
    }

    class MyClusteringOptionsTableListener implements JMyOptionsTable.OptionTableListener{

        //private String currentClusteringMethod = NAMES_CLUSTERING_METHODS[0];

        public MyClusteringOptionsTableListener(){
        }

        @Override
        public void optionTableEventHappened(JMyOptionsTable.OptionTableEvent event) {
            if( event.isAdjusting() ){return;}

            Properties clusteringProp = mClusteringOptions.collectValues();//= ((JMyOptionsTable) event.getSource()).collectValues();

            String new_clm = clusteringProp.getProperty(PROPERTY_CLUSTERING_METHOD);
            if( ! new_clm.equals(NAMES_CLUSTERING_METHODS[0])){

                if(mClusteringOptions_clusterMethod.equals(NAMES_CLUSTERING_METHODS[0])){
                    mClusteringOptions.activate(1,1);
                }

                double property_num_cluster = ( clusteringProp.containsKey(PROPERTY_NUM_CLUSTERS) ) ?
                        (1.0 - Double.parseDouble(clusteringProp.getProperty(PROPERTY_NUM_CLUSTERS))) :
                        (1.0);

                mClusteringOptions_numcCluster = property_num_cluster;


                if(! mClusteringOptions_clusterMethod.equals( new_clm ) ){
                    mClusteringOptions_clusterMethod = clusteringProp.getProperty(PROPERTY_CLUSTERING_METHOD);
                }

            }
            else{
                mClusteringOptions.deactivate(1,1);
            }
            mClusteringOptions_clusterMethod = clusteringProp.getProperty(PROPERTY_CLUSTERING_METHOD);

            boolean dissolveStacks = Boolean.parseBoolean( clusteringProp.getProperty( PROPERTY_DISSOLVE_STACKS ) );


            recomputePositions(false);
        }
    }


    private int mClustering[] = null;

    @Override
    public void actionPerformed(ActionEvent e) {

        if(e.getActionCommand().equals(ACTION_APPLY_BUTTON_PRESSED) ){

            // apply position to real card view
//            List<CardElement> allElements = new ArrayList<>( mCardPane.getCardPaneModel().getAllElements() );
//            List<Point2D> positions = mPositioner.positionAllCards( mModel , allElements );
            List<CardElement> allElements = new ArrayList<>( mCardPane.getCardPaneModel().getReducedAllElements() );
            List<Point2D> positions       = null;
            try {
               positions = mPositioner.positionAllCards(mModel, allElements);
            }
            catch(InterruptedException exc)
            {// here we cannot do anything, we just should not end up here..
                System.out.println("ERROR.. InterruptedException..\n"+exc.toString());
                return;
            }

            for(int zi=0;zi<positions.size();zi++){ allElements.get(zi).setCenter(positions.get(zi).getX(),positions.get(zi).getY()); }
            if(this.mClustering!=null) {
                mCardPane.getCardPaneModel().stackCardElements_Nonexcluded(this.mClustering);
            }

//@TODO: readd the animation stuff. Could be done easily, just has to apply clustering after animation finished..

//            PathAnimator pa = PathAnimator.createToPositionPathAnimator(mCardPane.getAllCardElements(), this.mPositioner, 0.75);
//            mCardPane.startAnimation(pa);

            double bx[] = mFastPane.getBoundsX();
            double by[] = mFastPane.getBoundsY();

            double cx = (bx[0]+bx[1])*0.5;
            double cy = (by[0]+by[1])*0.5;

            double dx = bx[1]-bx[0];
            double dy = by[1]-by[0];

            double zoom = Math.min( mCardPane.getWidth() / dx , mCardPane.getHeight() / dy ) ;


            mCardPane.startAnimateViewportToNewPosition(cx,cy,zoom,0.5);
        }
    }


    @Override
    public void stateChanged(ChangeEvent e) {

        System.out.println("!!CHANGE EVENT!!");
        System.out.println(e.toString());

        if( e.getSource() instanceof JTabbedPane){
            JTabbedPane tb = (JTabbedPane) e.getSource();
            this.mPositioner = this.mPositioners.get( tb.getSelectedIndex() );

            if(this.mPositioningEnabled) {
                // recompute..
                this.recomputePositions(true);
            }
        }


        if( e.getSource() == mSliderCreateStacks ){
            if( mSliderCreateStacks.getValueIsAdjusting() ){
            }
            else {
                if(this.mPositioningEnabled) {
                    this.recomputePositions(true);
                }
            }
        }

    }

    public List<CardElement> getElements() {
        //return this.mOriginalModel.getAllElements();  //this.mFastPane.getAllElements(); //getVisibleElements();
        System.out.println("JCardSortingPanel::getElements()");
        return this.mOriginalModel.getReducedAllElements();
    }

    class CEandPos {
        private CardElement mCE = null;
        private Point2D     mPos = null;
        public CEandPos(CardElement ce, Point2D pos){
            this.mCE =ce;
            this.mPos=pos;
        }
        public CardElement getCE(){return mCE;}
        public Point2D getPos(){return mPos;}
    }

    public void recomputePositions(boolean animate) {
        if(this.mPositionComputeThread==null){
            mPositionComputeThread = new RecomputePositionsThread();
            mPositionComputeThread.start();
            return;
        }
        else{
            if(this.mPositionComputeThread.mComputationDone){
                System.out.println("JCardSortingPanel::recompute : Interrupt!");
                mPositionComputeThread.interrupt();
                Thread oldThread = mPositionComputeThread;
                try {
                    oldThread.join();
                }
                catch(InterruptedException exc){
                    System.out.println("ERROR: InterruptedException in recomputePosition  while joining old thread..");
                    return;
                }
                System.out.println("JCardSortingPanel::recompute : Success! --> Start new computation..");
                mPositionComputeThread = new RecomputePositionsThread();
                mPositionComputeThread.start();
            }
            else{
                mPositionComputeThread = new RecomputePositionsThread();
                mPositionComputeThread.start();
            }
        }
    }

    class RecomputePositionsThread extends Thread {

        public volatile boolean mComputationDone = false;

        public void run() {
            mComputationDone = false;

            if (mPositioner == null) {
                return;
            }
            System.out.println("Recompute positions!");

            //List<CardElement> positionedCards = new ArrayList<>( this.mOriginalElements.size() );
            //for( CardElement cei : this.mOriginalElements ){ positionedCards.add( (CardElement) cei.copy() ); }
            List<CardElement> positionedCards = new ArrayList<>(getElements());

            // dissolve stacks..
            if (mCheckBoxDissolveStacks.isSelected()) {
//                List<CardElement> stacks = positionedCards.stream().filter( cxi -> cxi.isStackAfterExclusion() ).collect(Collectors.toList());
//                positionedCards.removeAll(stacks);
//                for (CardElement cs : stacks ) {
//                    //@TODO
//                    for( CompoundRecord cri : cs.getAllNonexcludedRecords() ){
//                        positionedCards.add( new CardElement( cs.getCardPane() , cri , cs.getPos() , 5)  );
//                    }
//                }
            }

            //        // sort..
            //        List<CEandPos> positions = new ArrayList<>(positionedCards.size());
            //        //for( CardElement ce : this.getElements() ) {
            //        for (CardElement ce : positionedCards) {
            //            Point2D cepos = mPositioner.positionCard(ce, null);
            //            positions.add( new CEandPos(ce,cepos) );
            //            //ce.setPosX( cepos.getX() ); ce.setPosY( cepos.getY() );
            //        }

            // sort..
            List<CEandPos> positions = new ArrayList<>(positionedCards.size());
            List<Point2D> pos =        null;
            try {
                pos = mPositioner.positionAllCards(mModel, getElements());
            }
            catch(InterruptedException e)
            {
                // ok, we just return..
                return;
            }
            // check if we got enough positions, else we just return..
            if(pos.size()!=positionedCards.size()){
                System.out.println("JCardSortingPanel : card positinoer returned wrong number of positions.. -> return");
                return;
            }

            List<CardElement> cpElements = getElements();
            for (int zi = 0; zi < pos.size(); zi++) {
                positions.add(new CEandPos(cpElements.get(zi), pos.get(zi)));
            }


            int clustering[] = null;

            // check if we should create stacks based on equally positioned cards:
            if( mPositioner.shouldCreateStacks() ){
//                // adhoc hash approach..
//                int number_of_bits = 10;
//                Map<Integer,List<CardElement>> pos_hash = new HashMap<>();
//                for(int zi=0;zi<(int) Math.pow(2,number_of_bits);zi++){
//                    pos_hash.put(zi,new ArrayList<>());
//                }
//                // hash positions..
                Map<Point2D,List<CardElement>> pos_hm = new HashMap<>();

                for(CEandPos cepi : positions ) {
                    if(pos_hm.containsKey(cepi.getPos())) {
                        pos_hm.get(cepi.getPos()).add(cepi.getCE());
                    }
                    else{
                        List<CardElement> celist = new ArrayList<>(); celist.add(cepi.getCE());
                        pos_hm.put(cepi.getPos(),celist);
                    }
                }

                // create clustering array..
                Map<CardElement,Integer> ce_list_pos = new HashMap<>();
                for(int zi=0;zi<cpElements.size();zi++){ ce_list_pos.put(cpElements.get(zi),zi); }

                clustering = new int[ce_list_pos.size()];
                int clustering_idx = 0;
                for(Point2D poi : pos_hm.keySet()){
                    for( CardElement cexi : pos_hm.get(poi) ) {
                        clustering[ ce_list_pos.get(cexi) ] = clustering_idx;
                    }
                    clustering_idx++;
                }
                mClustering = clustering;
            }
            else {
                // check if we have to create stacks..
                if (!mClusteringOptions_clusterMethod.equals(NAMES_CLUSTERING_METHODS[0])) {
                    // compute proximities and compute clustering:
                    clustering = recomputeClustering(computeProximity(positions.stream().map(cep -> cep.getPos()).collect(Collectors.toList())));
                    mClustering = clustering;
                } else {
                    clustering = getZeroClustering(positions.size());
                    mClustering = clustering;
                }
            }


            // create a new card pane model with the final positions:

            // 1. init clusters:
            Map<Integer, List<CEandPos>> clusteredCardElements = new HashMap<>();
            Arrays.stream(clustering).distinct().forEach(ci -> clusteredCardElements.put(ci, new ArrayList<CEandPos>()));
            for (int zi = 0; zi < positionedCards.size(); zi++) {
                clusteredCardElements.get(clustering[zi]).add(positions.get(zi));
            }

            // 2. create new CardPaneModel with final configuration:
            CardPaneModel<CardElement, ?> cpm = mFastPane.getCardPaneModel().cloneModel();

            cpm.clearAllCEs();
            for (Integer ki : clusteredCardElements.keySet()) {
                List<CompoundRecord> records = new ArrayList<>();
                double cepx = 0.0;
                double cepy = 0.0;
                for (CEandPos ce : clusteredCardElements.get(ki)) {
                    records.addAll(ce.getCE().getNonexcludedRecords());
                    cepx += ce.getPos().getX();
                    cepy += ce.getPos().getY();
                }
                cepx = cepx / clusteredCardElements.get(ki).size();
                cepy = cepy / clusteredCardElements.get(ki).size();
                cpm.addCE(records, cepx, cepy);
            }

            // set new cpm..
            mFastPane.setCardPaneModel(cpm);
            mFastPane.repaint();

            mComputationDone = true;
        }
    }


//        // List<CardElement> elementsAfterStacking = new ArrayList<>();
//        //elementsAfterStacking.addAll( this.getElements() );
//        elementsAfterStacking.addAll( positionedCards );
//
//        // save this list for creating the animation
//        List<CardElement> listCE = new ArrayList<>( this.getElements());
//        //List<CardElement> listCE = new ArrayList<>( positionedCards );
//        //List<CardElement> listCE = new ArrayList<>( mOriginalElements );
//
//
//        // create / start animation
//        if(animate){
//            PathAnimator pa = PathAnimator.createToPositionPathAnimator(listCE, positions ,CONF_TIME_ANIMATION);
//            this.mFastPane.startAnimation(pa,elementsAfterStacking);
//        }
//
//
//    }


//        if(this.mSliderCreateStacks.getValue() > 0){
//
//
//
//            elementsAfterStacking.addAll( positionedCards );
//
//            Map<Integer,CardElement> mapIntToCE = new HashMap<>();
//            for(int zi=0;zi<elementsAfterStacking.size();zi++){ mapIntToCE.put(zi,elementsAfterStacking.get(zi)); }
//            List<List<Integer>> stacks = createStacks( positions , 0.01*mSliderCreateStacks.getValue() );
//
//            for( List<Integer> szi : stacks ){
//                if(szi.size()>1){
//                    List<CardElement> stackCEs = szi.stream().map( szii -> mapIntToCE.get(szii) ).collect(Collectors.toList());
//                    List<CompoundRecord>  stackRecords = stackCEs.stream().flatMap( cei -> cei.getAllRecords().stream() ).collect(Collectors.toList());
//                    elementsAfterStacking.removeAll( stackCEs );
//                    CardElement newCE =  new CardElement( this.mCardPane, stackRecords , stackCEs.get(0).getPos() , stackCEs.stream().mapToInt( cei -> cei.getZ() ).max().getAsInt()+10 );
//                    newCE.setPosX( stackCEs.stream().mapToDouble( cei -> cei.getPosX() ).average().getAsDouble() );
//                    newCE.setPosY( stackCEs.stream().mapToDouble( cei -> cei.getPosY() ).average().getAsDouble() );
//                    elementsAfterStacking.add(newCE);
//                }
//            }
//        }
//    }



    class BidiMap{
        public Map<Point2D,int[]> mMapAtoB = new HashMap<>();
        public Map<int[],Point2D> mMapBtoA = new HashMap<>();
        public BidiMap(){}

        public void put(Point2D a,int b[]){ mMapAtoB.put(a,b); mMapBtoA.put(b,a); }
    }

    @Override
    public void sortingConfigurationChanged(SortingCofigurationEvent e) {
        this.mPositioner = e.getSource();
        recomputePositions(true);
    }

    public static void main(String args[]){
        JFrame frame = new JFrame("FastCardPane Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Random r = new Random(1234);
        List<CardElement> ceList = new ArrayList<>();
        for(int zi=0;zi<1000;zi++){
            List<CompoundRecord> crlist = new ArrayList<CompoundRecord>();
            crlist.add(null);
            //ceList.add(new CardElement( crlist , new Rectangle2D.Double(r.nextDouble()*4000,r.nextDouble()*4000,100,140),r.nextInt(20000)) );
            //ceList.add(new CardElement( crlist , new Rectangle2D.Double(r.nextDouble()*4000,r.nextDouble()*4000,10,14),r.nextInt(20000)) );
        }

        frame.getContentPane().add(new JCardSortingPanel(null,null, ceList));

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

//
//    class MyComboBoxListener implements ActionListener {
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            String actionCommand = e.getActionCommand();
//
//            if(actionCommand.equals( ACTION_COMMAND_SORTER_COMBOBOX )){
//                String sorterName = (String)mComboBoxPositioners.getSelectedItem();
//
//                if(sorterName.equals(XYSorter.NAME)) {
//                    //@TODO don't do anything if no change
//
//                    mRightPanel.removeAll();
//                    mRightPanel.setLayout(new BorderLayout());
//
//                    mPositioner = new XYSorter(mModel, mElements);
//                    mRightPanel.add(mPositioner.getGUI(), BorderLayout.CENTER);
//                    mPositioner.registerSortingConfigListener(getThis());
//                }
//                if(sorterName.equals(XSorter.NAME)) {
//
//                    mRightPanel.removeAll();
//                    mRightPanel.setLayout(new BorderLayout());
//
//                    mPositioner = new XSorter(mModel, mElements);
//                    mRightPanel.add(mPositioner.getGUI(), BorderLayout.CENTER);
//                    mPositioner.registerSortingConfigListener(getThis());
//                }
//
//
//                revalidate();
//            }
//        }
//    }

    private JCardSortingPanel getThis(){
        return this;
    }

}




