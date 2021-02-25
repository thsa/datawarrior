package com.actelion.research.table.view;

import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.DetailPopupProvider;
import com.actelion.research.table.model.*;
import com.actelion.research.table.view.card.*;
import com.actelion.research.table.view.card.animators.PathAnimator;
import com.actelion.research.table.view.card.cardsurface.CardElementLayoutLogic;
import com.actelion.research.table.view.card.cardsurface.gui.JCardWizard2;
import com.actelion.research.table.view.card.positioning.GridPositioner;
import com.actelion.research.table.view.card.positioning.RandomCardPositioner;
import com.actelion.research.table.view.card.tools.DataWarriorLink;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;


/**
 * This class implements the cards view of DataWarrior.
 *
 * When a new cards view is created (via the task DETaskNewCardView), then the constructor is called,
 * and the cards are initialized from the current compound table model.
 *
 *
 * When the cards view is created from reading a .dwar file, then this is a four steps procedure:
 *
 * 1. the .dwar file reader finds a "CardsData" section. This part can be read with the static function .read(..)
 *    this creates a List<CardPane.CardElementData>
 *
 * 2. the JCardView object can be created with the default constructor
 *
 * 3. the cards view properties are then set with the setProperties(..) function. This configures the
 *    card surface etc..
 *
 * 4. the actual card pane configuration is initialized with .initializeCards(..), where
 *    the List<CardPane.CardElementData> from step one is used.
 *
 *
 */
public class JCardView extends JPanel implements CompoundTableView, HighlightListener, ListSelectionListener, CompoundTableListListener, VisualizationColorListener, FocusableView, KeyListener {
	private Frame mParentFrame;
	private CompoundTableModel			mTableModel;
	private CompoundListSelectionModel	mListSelectionModel;
	private CompoundTableColorHandler	mColorHandler;
	private DetailPopupProvider			mDetailPopupProvider;

	private DataWarriorLink             mDWL;

	// the layered pane to put the fastcardpane ontop of the JCardPane..
	private JMyLayeredPane                mLayeredPane = null;

	// the card pane
	private JCardPane                   mCardPane = null;

	// the right pane, with tools and the fastmap etc..
	private JPanel                      mRightPane = null;


	//private CardPositionerInterface     mCardPositioner = new RandomCardPositioner();
	private JFastCardPane               mFastCardPane = null;

	// Button to expand FastCardPane..
	private JButton                     mFastCardPaneExpandButton = new JButton("MAP");

	private JCardWizard2.FullCardWizardConfig    mCardWizardConfig = null;



	private CardElementLayoutLogic mCardElementLayoutLogic = null;


	int logLevel = 0;


	public JCardView(Frame parentFrame, CompoundTableModel tableModel, CompoundTableColorHandler colorHandler,
					 CompoundListSelectionModel listSelectionModel) {
		mParentFrame = parentFrame;
		mTableModel = tableModel;
		mColorHandler = colorHandler;
		mListSelectionModel = listSelectionModel;

		//this.setCardConfiguration(new CardConfiguration());
		//this.addMouseListener(this);

        this.mListSelectionModel.addListSelectionListener(this);
        //this.mListSelectionModel.addListSelectionListener(this);

        // register HitListListener:
        // we handle updates in this very simple way, as we always have to reinit all caches of all CardElements
        // in the CardPaneModel
        tableModel.getListHandler().addCompoundTableListListener(new HitListListener());

		// to receive key events
		this.setFocusable(true);
		this.requestFocus();
		this.addKeyListener(this);
		this.setVisible(true);


		initCardView(tableModel,listSelectionModel,colorHandler);
		initializeCards();

		initCardViewDefaultSettings();

		// init card element layout logic
        mCardElementLayoutLogic = new CardElementLayoutLogic(mDWL);
        // find initial configuration:
        initInitialCardConfiguration();

		mCardPane.reinitAllCards();
	}


	private void initCardViewDefaultSettings() {
		mCardPane.setCardCacheThreadCount(1);
		mCardPane.setCachedCardProviderNumberOfPixelsToDraw( 2000000 );

		mCardPane.setNumCards_HQ(20);
		mCardPane.setNumCards_SUPERFAST(400);
	}

	public DataWarriorLink getDataWarriorLink() { return this.mDWL; }

	public JCardWizard2.FullCardWizardConfig getFullCardViewConfig() {
		return mCardWizardConfig;
	}

	public void initCardView(CompoundTableModel tableModel, CompoundListSelectionModel listSelectionModel , CompoundTableColorHandler colorHelper){

		this.setLayout(new BorderLayout());
		DataWarriorLink dwl = new DataWarriorLink(mParentFrame,tableModel,listSelectionModel, colorHelper, mDetailPopupProvider );
		this.mDWL = dwl;

		mCardPane = new JCardPane(this, dwl);

		//this.add(mCardPane,BorderLayout.CENTER);

		mLayeredPane = new JMyLayeredPane();
		mLayeredPane.setVisible(true);
		mLayeredPane.setBackground(Color.darkGray);

//		JPanel sizer = new JPanel();
//		sizer.setPreferredSize(new Dimension(4000,4000));
//		mLayeredPane.add(sizer,new Integer(100));

		//this.add(mLayeredPane,BorderLayout.CENTER);
//		JPanel test = new JPanel();
//		test.setBackground(Color.DARK_GRAY);
		this.add(mLayeredPane,BorderLayout.CENTER);
		this.doLayout();
		this.revalidate();


        if(logLevel>0) {
            System.out.println("LayeredPane size: " + mLayeredPane.getWidth() + " , " + mLayeredPane.getHeight());
        }

		mLayeredPane.setMainComponent(mCardPane);

		// no top right overview pane anymore..
		if(false) {
			mFastCardPane = new JFastCardPane(mCardPane.getCardPaneModel(), JFastCardPane.FAST_PANE_MODE.SHOWING_FULL_CARD_PANE_MODEL);
			mFastCardPane.setViewportShowingCardPane(mCardPane);
			mCardPane.registerCardPaneEventListener(mFastCardPane);

			mLayeredPane.setTopRightComponent(mFastCardPaneExpandButton,20,20,false,false);
			this.turnOffOverviewPane();
			//mLayeredPane.setTopRightComponent(mFastCardPane, 200, 200);
		}

		this.revalidate();


//		mRightPane = new JPanel();
//		mRightPane.setMinimumSize( new Dimension(200,400) );
//		mRightPane.setMaximumSize( new Dimension(200,4000) );
//		mRightPane.setPreferredSize( new Dimension(200,400) );
//		mRightPane.setBackground(Color.blue);
//		this.add(mRightPane,BorderLayout.EAST);
//
		this.doLayout();
		this.revalidate();
		this.repaint();

//		initRightPanel();

		mLayeredPane.layoutComponents();

		this.revalidate();


//		this.initCardAndStackConfig();


		// start the card wizard:
//		if(false) {
//			JDialog cardWizardDialog = new JDialog(this.mParentFrame, "Card Wizard", false);
//			cardWizardDialog.getContentPane().setLayout(new BorderLayout());
//			cardWizardDialog.add(cardWizard);
//			cardWizardDialog.pack();
//			cardWizardDialog.setResizable(false);
//			cardWizardDialog.setVisible(true);
//		}

	}

//	private void initCardAndStackConfig()  {
//
//		// create the card wizard (also if we do not show it, we use it to set the initial configuration..)
//		//JCardWizard2 cardWizard = new JCardWizard2( mDWL, this, mCardPane, true, null);
//
//        // we just add the first structure, if there is any..
//
//        List<JCardWizard2.ColumnWithType> colConfig = new ArrayList<>();
//
//        for( int zi=0;zi<mTableModel.getColumnCount();zi++) {
//            if( mTableModel.isColumnTypeStructure(zi,true) ){
//                colConfig.add(new JCardWizard2.ColumnWithType(mTableModel,zi, JCardWizard2.ColumnWithType.TYPE_CHEM_STRUCTURE));
//                break;
//            }
//        }
//        JCardWizard2.CardWizardGlobalConfig cwgc = new JCardWizard2.CardWizardGlobalConfig(colConfig);
//        JCardWizard2.CardWizardCardConfig cwcc = new JCardWizard2.CardWizardCardConfig(1.0, 0.0, 0.0, 0.75, 0.0,  JCardWizard2.CardWizardCardConfig.MODE_DATA_SHOW[0] );
//        JCardWizard2.CardWizardStackConfig cwsc = new JCardWizard2.CardWizardStackConfig(1.0, 0.0, 0.0,0.75,0.25);
//        JCardWizard2.FullCardWizardConfig fcwc = new JCardWizard2.FullCardWizardConfig(cwgc,cwcc,cwsc);
//
//        this.setCardWizardConfig(fcwc);
//	}


//	public JCardWizard2.FullCardWizardConfig getCardWizardConfig(){
//		return mCardWizardConfig;
//	}
//
//	public void setCardWizardConfig( JCardWizard2.FullCardWizardConfig config ){
//		this.mCardWizardConfig = config;
//	}

	/**
	 *
	 * @TODO add auto-stacking options (?) (i.e. have in addition to CardPositioner an AutomaticCardStacker)
	 * @TODO add recycle.. option
	 *
	 */
	public void initializeCards() {
		//GridPositioner gp = new GridPositioner(records,0,0,mCardPane.getCardDrawer().getCardDrawingConfig().getCardWidth(),mCardPane.getCardDrawer().getCardDrawingConfig().getCardHeight());
		//RandomCardPositioner rcp = new RandomCardPositioner(mCardPane);

		//GridPositioner gp = new GridPositioner( mCardPane.getCardPaneModel().getAllElements() , 0,0,mCardPane.getCardDrawer().getCardDrawingConfig().getCardWidth(),mCardPane.getCardDrawer().getCardDrawingConfig().getCardHeight() );
		//List<Point2D> pos = gp.positionAllCards( mTableModel , mCardPane.getCardPaneModel().getAllElements() );

		List<CardPaneModel.CardElementData> elements = new ArrayList<>();

		for (int i=0; i< mTableModel.getTotalRowCount(); i++) {
			List<Integer> ilist = new ArrayList<>(); ilist.add(i);
			Point2D pxy = new Point2D.Double(Double.NaN,Double.NaN);//gp.positionSingleCard(null, mTableModel.getTotalRecord(i));
			elements.add( new CardPaneModel.CardElementData(ilist,pxy.getX(),pxy.getY()));
		}

		initializeCards(elements, true);
	}

	public void initializeCards(List<CardPaneModel.CardElementData> elements, boolean adjustViewport) {

		// check if we have to arrange the cards
		boolean needs_grid_layout = elements.stream().allMatch( ci -> Double.isNaN( ci.getPos().getX() ) );

		List<CompoundRecord> records = new ArrayList<>();
		for (int i=0; i< mTableModel.getTotalRowCount(); i++) {
			CompoundRecord record = mTableModel.getTotalRecord(i);
			records.add(record);
		}


		RandomCardPositioner rcp = new RandomCardPositioner(mCardPane);
		mCardPane.setDefaultCardPositioner( rcp );
		mCardPane.setComputeHiddenCardElementsEnabled(false);

		//records.stream().forEach( ri -> mCardPane.addNewCardForRecord(ri) );
		//mCardPane.addNewCardsForRecords(records);
		mCardPane.getCardPaneModel().clearAllCEs();
		for(CardPaneModel.CardElementData cedi : elements) {
			List<CompoundRecord> crList = cedi.getCompoundRecords(mTableModel);
			CardElement cei = mCardPane.getCardPaneModel().addCE(crList,cedi.getPos().getX(),cedi.getPos().getY());
			mCardPane.getCardPaneModel().renameStack(crList.get(0).getID(),cedi.getStackName());
		}
//		mCardPane.getCardPaneModel().addCE(  )

		// reset back..
		mCardPane.setComputeHiddenCardElementsEnabled(false);
//
//		RandomCardPositioner gp_new = new RandomCardPositioner(mCardPane);
//		mCardPane.setDefaultCardPositioner(gp_new);
//
		if(needs_grid_layout) {
			GridPositioner gp = new GridPositioner(mCardPane.getCardPaneModel().getAllElements(), 0, 0, mCardPane.getCardDrawer().getCardDrawingConfig().getCardWidth(), mCardPane.getCardDrawer().getCardDrawingConfig().getCardHeight());
			List<Point2D> pos = gp.positionAllCards(mTableModel, mCardPane.getCardPaneModel().getAllElements());
			for (int zi = 0; zi < mCardPane.getCardPaneModel().getAllElements().size(); zi++) {
				mCardPane.getCardPaneModel().getAllElements().get(zi).setCenter(pos.get(zi).getX(), pos.get(zi).getY());
			}
		}

		if(adjustViewport) {
		    // we may have to wait a tiny bit, because we need a correct size for the cardview.

			//mCardPane.startAnimateViewportToShowAllCards();
            Thread waitForCorrectSizeThenSetViewport = new Thread(){
                @Override
                public void run() {
                    try {
                        Thread.sleep(400);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //getCardPane().startAnimateViewportToShowAllCards();
                    getCardPane().setViewportAroundAllNonexcludedCardElement(0.4);
                }
            };
            waitForCorrectSizeThenSetViewport.start();
		}

	}


//	/**
//	 * Serializes the Card View config. Config means the configuration of the card.
//	 *
//	 * Technically this is done by serializing the mGlobalConfig object.
//	 */
//	public void writeConfig(BufferedReader in) {
//		for(JCardWizard2.ColumnWithType ci : mGlobalConfig) {
//			ci.
//		}
//	}





	/**
	 * Serializes the card elements with their position.
	 * This function writes a single line (terminated by a \n) into the BufferedWriter.
	 *
	 * The written data can be read by the .read() function
	 *
	 * @param out
	 */
	public void write_old(BufferedWriter out) throws IOException {
		List<CardElement> ceList = this.getCardPane().getCardPaneModel().getAllElements();
		for(int zi=0;zi<ceList.size();zi++) {
			CardElement cei = ceList.get(zi);
			out.write( serializeCardElementToString(cei) );
			if(zi<ceList.size()-1) {
				out.write(";");
			}
		}
		out.write("\n");
		out.flush();
	}





	/**
	 * Deserializes the card elements with their position. The return value of this function can be used
	 * to initialize the cards via the initializeCards(..) function.
	 *
	 * This function reads a single line from the BufferedReader.
	 *
	 * @param in
	 */
	public static List<CardPaneModel.CardElementData> read_old(BufferedReader in) throws IOException {

		String serialized = in.readLine();
		String splits[] = serialized.split(";");

		List<CardPaneModel.CardElementData> ceList = new ArrayList<>();
		for(int zi=0;zi<splits.length;zi++){
			CardPaneModel.CardElementData cedata = deserializeCardElementString(splits[zi]);
			ceList.add(cedata);
		}

		return ceList;
		//this.initializeCards(ceList);
	}

	/**
	 * Serializes:
     * 1. the viewport (first line)
     * 2. the the card elements with their position.
	 * This function writes lines (terminated by a \n) into the BufferedWriter.
	 *
	 * The written data can be read by the .read() function
	 *
	 * @param out
	 */
	public void write(BufferedWriter out) throws IOException {
	    out.write(READ_LINE_ID_VIEWPORT+this.mCardPane.getViewportCenterX()+","
                +this.mCardPane.getViewportCenterY()+","+this.mCardPane.getViewportZoomFactor()+"\n");
		List<CardElement> ceList = this.getCardPane().getCardPaneModel().getAllElements();
		for(int zi=0;zi<ceList.size();zi++) {
			CardElement cei = ceList.get(zi);
			out.write( serializeCardElementToString(cei) );
			out.write("\n");
		}
		out.flush();
	}

    /**
     * Line start indicating that it contains the viewport data.
     */
	private static final String READ_LINE_ID_VIEWPORT = "<<ViewPort>>";

	/**
	 * Deserializes:
     * 1. the viewport (first line)
     * 2. the card elements with their position. The return value of this function can be used
	 * to initialize the cards via the initializeCards(..) function.
	 *
	 * This function reads lines until the line that equals the terminator String.
	 *
	 * When the function returns, the last operation on the BufferedReader was reading
	 * the terminator line with .readLine()
	 * (or, in case that the terminator was not found, after reading the last line of the file).
	 *
	 *
	 * @param in
	 */
	public static CardViewDataDependentStorageValues read(BufferedReader in, String terminator) throws IOException {

		List<CardPaneModel.CardElementData> ceList = new ArrayList<>();

		double vp_cx=0;
		double vp_cy=0;
        double vp_zoom=1.0;

		String line = null;
		while( (line = in.readLine() ) != null ) {
			if(line.equals(terminator)) {
				break;
			}

			if(line.startsWith(READ_LINE_ID_VIEWPORT)) {
			    // viewport:
                line = line.replace(READ_LINE_ID_VIEWPORT,"");
                String ls[] = line.split(",");
                vp_cx = Double.parseDouble(ls[0]);
                vp_cy = Double.parseDouble(ls[1]);
                vp_zoom = Double.parseDouble(ls[2]);
			}
			else {
                line.split("");
                CardPaneModel.CardElementData cedata = deserializeCardElementString(line);
                ceList.add(cedata);
            }
		}
		return new CardViewDataDependentStorageValues(ceList,vp_cx,vp_cy,vp_zoom);
	}








	/**
	 * Returns a properties object that can be used to configure the card surface via
	 * the function setCardConfiguration(..)
	 *
	 * It just returns the Properties object returned by the FullCardWizardConfig variable
	 *
	 * @return
	 */
	public Properties getCardConfiguration(){
		Properties p = new Properties();
		return mCardWizardConfig.toProperties();
	}

	/**
	 * Initializes the FullCardWizardConfig object with the supplied Properties object. This initializes the
	 * cards view.
	 *
	 * @param p
	 */
//	public void setCardConfiguration(Properties p){
//		JCardWizard2.FullCardWizardConfig config = new JCardWizard2.FullCardWizardConfig(this.getTableModel(), p);
//		this.setCardWizardConfig(config);
//	}

	private static DecimalFormat mDecimalFormat_CardCoordinates = new DecimalFormat("#.##");

	static {
		mDecimalFormat_CardCoordinates.setRoundingMode(RoundingMode.HALF_UP);
	}

	private static String serializeCardElementToString(CardPaneModel.CardElementInterface ce){
		StringBuilder sb = new StringBuilder();
		for(CompoundRecord cr : ce.getAllRecords()) {
			sb.append(cr.getID());sb.append("<,,>");
		}
		sb.delete(sb.length()-4, sb.length());
		sb.append("<::>");
		sb.append(mDecimalFormat_CardCoordinates.format(ce.getPosX()));
		sb.append("<::>");
		sb.append(mDecimalFormat_CardCoordinates.format(ce.getPosY()));
		sb.append("<::>");
		sb.append(ce.getStackName());
		return sb.toString();
	}

    public static class CardViewDataDependentStorageValues {
	    public final List<CardPaneModel.CardElementData> mCEData;
        public final double viewport_cx;
        public final double viewport_cy;
        public final double viewport_zoom;
	    public CardViewDataDependentStorageValues(List<CardPaneModel.CardElementData> ce_data, double vp_cx, double vp_cy, double vp_zoom) {
	        this.mCEData = ce_data;
	        this.viewport_cx = vp_cx;
	        this.viewport_cy = vp_cy;
	        this.viewport_zoom = vp_zoom;
        }
    }

	private static CardPaneModel.CardElementData deserializeCardElementString(String in){
		String splits[]   = in.split("<::>");
		String crSplits[] = splits[0].split("<,,>");
		List<Integer> crl = new ArrayList<>();
		Arrays.stream(crSplits).forEachOrdered(si -> crl.add( Integer.parseInt(si) )  );
		CardPaneModel.CardElementData ced = new CardPaneModel.CardElementData(crl,Double.parseDouble(splits[1]),Double.parseDouble(splits[2]));
		if(splits.length>3) {
			ced.setStackName(splits[3]);
		}
		return ced;
	}


	@Override
	public void compoundTableChanged(CompoundTableEvent e) {

		if (e.getType() == CompoundTableEvent.cChangeExcluded  ){

		}

		if (e.getType() == CompoundTableEvent.cAddRows
				|| e.getType() == CompoundTableEvent.cDeleteRows ) {

			// this removes all deledted rows and adds new elements (at random positions..)
			mCardPane.getCardPaneModel().synchronizeWithCompoundTableModel();

            // always: clear the graphics buffers that store all the cards..
            mCardPane.reinitAllCards();
		}

	}

	private ViewSelectionHelper mViewSelectionHelper = null;

	@Override
	public void setViewSelectionHelper(ViewSelectionHelper l) {
		mViewSelectionHelper = l;
	}

	/**
	 * @return A ViewSelectionHelper
	 */
	public ViewSelectionHelper getViewSelectionHelper() {
		return this.mViewSelectionHelper;
	}


	@Override
	public void listChanged(CompoundTableListEvent e) {
        // JCardPane has to recompute all buffered stacks..
        //this.mCardPane.recomputeAllBufferedStacks();

        if(logLevel>0) {
            System.out.println("ListEvent!! " + e.toString());
        }
    }

	@Override
	public void cleanup() {
	    // @TODO: reset hidden rows (i.e. return flag), clean up all objects
        this.getCardPane().getCardPaneModel().cleanup();

        // release all buffered images
        //this.getCardPane().cleanup();
//        mCardPane = null;
//        mFastCardPane = null;
    }

	@Override
	public boolean copyViewContent() {
		return true;
	}

	@Override
	public CompoundTableModel getTableModel() {
		return mTableModel;
	}

	public void setDetailPopupProvider(DetailPopupProvider p) {
		mDetailPopupProvider = p;
	}


    /**
     * This is called from the JCardPane
     *
     * @param e
     * @return
     */
	public boolean handlePopupTrigger(MouseEvent e, List<CardElement> selection ){
//		if(e.isPopupTrigger()){
            //mDetailPopupProvider.createPopupMenu(null, this, -1);
            if (mDetailPopupProvider != null) {
                JPopupMenu popup = mDetailPopupProvider.createPopupMenu(null, this, -1, e.isControlDown());
                if (popup != null) {
                    popup.show(this, e.getX(), e.getY());
                    return true;
                }
            }
//        }
		return false;
	}

	public CompoundTableColorHandler getColorHandler(){
		return this.mColorHandler;
	}


	public boolean isOverviewPaneEnabled(){
		return this.mLayeredPane.getTopRightComponent()!=null;
	}

	public void setOverviewPaneEnabled(boolean enabled) {
		if(this.mLayeredPane.getTopRightComponent()!=null && enabled){return;}
		if(this.mLayeredPane.getTopRightComponent()!=null && !enabled){turnOffOverviewPane();}
		if(this.mLayeredPane.getTopRightComponent()==null && !enabled){return;}
		if(this.mLayeredPane.getTopRightComponent()==null && enabled){turnOnOverviewPane();}
	}

	private void turnOffOverviewPane(){
		//this.mLayeredPane.setTopRightComponent(null,mLayeredPane.mTopRightWidth,mLayeredPane.mTopRightHeight,false);
		mFastCardPaneExpandButton = new JButton("Map");
		mFastCardPaneExpandButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				turnOnOverviewPane();
			}
		});

		this.mLayeredPane.setTopRightComponent(mFastCardPaneExpandButton,40,20,false,false);



		this.mLayeredPane.layoutComponents();
	}

	private void turnOnOverviewPane(){
		if(this.mFastCardPane==null){
			System.out.println("Error.. mFastCardPane missing, cannot initialize overview pane..");
			return;
		}
		this.mLayeredPane.setTopRightComponent(this.mFastCardPane,200,200,true,true);
		this.mLayeredPane.layoutComponents();
	}



//	@Override
//	public void mouseClicked(MouseEvent e) {
//        System.out.println("Mouse clicked");
//	}
//
//	@Override
//	public void mousePressed(MouseEvent e) {
//        System.out.println("Mouse pressed");
//		if(handlePopupTrigger(e)){ return;}
//	}
//
//	@Override
//	public void mouseReleased(MouseEvent e) {
//        System.out.println("Mouse released");
//	}
//
//	@Override
//	public void mouseEntered(MouseEvent e) {
//
//	}
//
//	@Override
//	public void mouseExited(MouseEvent e) {
//
//	}

//	public void setCardPositioner(CardPositionerInterface cardPositioner){
//		this.mCardPositioner = cardPositioner;
//	}
//
//	public CardPositionerInterface getCardPositioner(){
//		return this.mCardPositioner;
//	}


	//public void setCardConfiguration(CardConfiguration cardConfig){
		//this.mCardConfiguration = cardConfig;
		//@TODO0
		//and request recomputation of cards..
	//}


    public JCardPane getCardPane(){
	    return this.mCardPane;
    }

    public JFastCardPane getFastCardPane(){
		return this.mFastCardPane;
	}

    public CompoundListSelectionModel getCompoundListSelectionModel(){
	    return this.mListSelectionModel;
    }








    @Override
    public void highlightChanged(CompoundRecord record) {
        // @TODO implement..
		//mCardPane.setRecordDirty(record);
    }

    @Override
    public int getFocusList() {
        // @TODO implement..
        return -1;
    }

    private int mFocusListFlag = -1;


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
		this.mCardPane.paintHighResolution(g,bounds,fontScaling,transparentBG,isPrinting);
	}

    @Override
    public void setFocusList(int no) {

    	if(no<0){
    		// then we have to check whats going on:
			//cFocusOnSelection or cHitlistUnassigned

			if(no==FocusableView.cFocusNone) {
				mFocusListFlag = no;
				this.mCardPane.getCardPaneModel().setGreyedOutCompoundRecords(new ArrayList<>());
			}
			if(no==FocusableView.cFocusOnSelection){
				mFocusListFlag = no;

				List<CardElement> greyed_out = mCardPane.getCardPaneModel().getReducedAllElements();
				greyed_out.removeAll( mCardPane.getSelection() );

				// hmm.. we grey out ALL records, not just the nonexcluded.. This makes more sense imo, i.e. the
				// focus selection acts on the CardElement including ALL crs.

				this.mCardPane.getCardPaneModel().setGreyedOutCompoundRecords(  greyed_out.stream().flatMap( ci -> ci.getAllRecords().stream() ).collect(Collectors.toList()) );
			}

			return;
		}

        int flag = mTableModel.getListHandler().getListFlagNo(no);
        mFocusListFlag = flag;

        List<CompoundRecord> crList = new ArrayList<>();
        for( int zi = 0; zi<mTableModel.getRowCount();zi++){
            CompoundRecord cri = mTableModel.getRecord(zi);
            if(cri.isFlagSet(flag)){
                crList.add(cri);
            }
        }
        this.mCardPane.getCardPaneModel().setHitlist(crList);
    }


    public int getFocusListFlag() {
	    return mFocusListFlag;
    }

    @Override
    public void colorChanged(VisualizationColor source) {
		this.getCardPane().reinitAllCards();
		this.getCardPane().repaint();
    }


    @Override
    public void valueChanged(ListSelectionEvent e) {

		//if (e.getValueIsAdjusting()) { return; }

        if(logLevel>0) {
            System.out.println("ListSelectionEvent.. " + e.getFirstIndex() + ".." + e.getLastIndex());
        }

		int idx_a = e.getFirstIndex();
		int idx_b = e.getLastIndex();

    	if(idx_a<0){
            if(logLevel>0) {
                System.out.println("ListSelectionEvent.. (-1) unselect");
            }
			idx_a = 0; idx_b = mTableModel.getRowCount()-1;
			return;
    	}



		HashSet<Integer> hSet = new HashSet<>();
		for(int zi=idx_a;zi<=idx_b;zi++){
			if(mTableModel.isSelected(zi)){ hSet.add(zi);}
		}

		List<CardElement> selectedCardElements = new ArrayList<>();

		for(CardElement ce : mCardPane.getCardPaneModel().getReducedAllElements()){
			boolean contains = false;
			for(CompoundRecord cr : ce.getAllNonexcludedRecords()) {
				contains = contains | hSet.contains(cr.getID());
			}
			if(contains){
				selectedCardElements.add(ce);
			}
		}

		mCardPane.setSelection(selectedCardElements,false);
        this.getCardPane().repaint();
    }


	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
        if(this.logLevel>0) {
            System.out.println("Key pressed: " + e.getKeyCode());
        }
		int code = e.getKeyCode();
		switch(e.getKeyCode()){
//			case KeyEvent.VK_H:{
//				this.getCardPane().setRenderQuality(JCardPane.RENDER_QUALITY_FULL);
//			}
//			break;
//			case KeyEvent.VK_L:{
//				this.getCardPane().setRenderQuality(JCardPane.RENDER_QUALITY_LOW);
//			}
//			break;
			case KeyEvent.VK_SPACE:{
				// determine grid size:
				int gx = (int) Math.ceil(Math.sqrt(this.getCardPane().getAllCardElements().size()));
				int gy = (int) Math.ceil(Math.sqrt(this.getCardPane().getAllCardElements().size()));

				PathAnimator pa = PathAnimator.createToGridPathAnimator(this.getCardPane().getAllCardElements(), 1.0, gx, gy);
				getCardPane().startAnimation(pa);
			}
			break;
			case KeyEvent.VK_Q: {
				//this.getCardPane().activateInsaneMode();
			}
			break;
			case KeyEvent.VK_P:{
				this.setOverviewPaneEnabled( ! this.isOverviewPaneEnabled() );
			}
			break;
			case KeyEvent.VK_A:{
				if( (e.isControlDown())) {
					List<CompoundRecord> crs = this.mCardPane.getCardPaneModel().getReducedAllElements().stream().flatMap( cei -> cei.getAllNonexcludedRecords().stream() ).collect(Collectors.toList());
//					List<CardElement> crs = this.mCardPane.getCardPaneModel().getReducedAllElements();
//					System.out.println("Select all -> select "+crs.size()+" card elements..");
//					this.mCardPane.setSelection(crs);
//					this.mCardPane.setSelection(crs);
					for(int zi=0;zi<this.mTableModel.getRowCount();zi++){
						this.mTableModel.getRecord(zi).setSelection(true);
					}
					//this.mCardPane.repaint();
				}
			}

//			case KeyEvent.VK_UP: {
//				StringWriter sw = new StringWriter();
//				BufferedWriter bw = new BufferedWriter(sw);
//				try {
//					this.write(bw);
//					String serialized = sw.toString();
//					serialized = serialized + "END\n";
//					StringReader sr = new StringReader(serialized);
//					List<CardPaneModel.CardElementData> card_view_data = JCardView.read( new BufferedReader(sr) ,"END");
//					System.out.println("ok! tested serialization..");
//				}
//				catch(Exception ex) {
//					ex.printStackTrace();
//				}
//			}
//			break;

//			case KeyEvent.VK_CONTROL: {
//				CardElement highlighted = this.getCardPane().getHighlightedCardElement();
////				if( highlighted.isStackAfterExclusion() ){
////				}
//				if(highlighted!=null)
//				{
//					if( highlighted.isStack()) {
//						// create stack reordering dialog
//						int nCardsToShow = 10;
//						int widthPx = 800;
//
//						widthPx = HiDPIHelper.scale(Math.min(widthPx, (int) (highlighted.getAllRecords().size() * 1.25 * highlighted.getRectangle().getWidth())));
//
//						int heightFastPane = HiDPIHelper.scale(60);
//
//						// create the right panel with the hidden / nonhidden toggle:
//						JPanel pRight = new JPanel();
//						pRight.setLayout(new BorderLayout());
//						JToggleButton tButton = new JToggleButton("Hidden");
//						pRight.add(tButton, BorderLayout.CENTER);
//
//						JCardPane reorderingPane = mCardPane.createReorderingPane(this, highlighted, nCardsToShow, widthPx);
//						reorderingPane.setProposeStacks(false);
//
//						JFastCardPaneForSorting reorderingFastPane = new JFastCardPaneForSorting(reorderingPane.getCardPaneModel());
//						reorderingFastPane.setPreferredSize(new Dimension(widthPx, heightFastPane));
//						reorderingFastPane.setSize(widthPx, 200);
//
//						JDialog dialog = new JDialog(this.mParentFrame, "Stack Cards", false);
//						dialog.getContentPane().setLayout(new BorderLayout());
//
//
//						reorderingFastPane.setViewportShowingCardPane(reorderingPane);
//
//
//						dialog.getContentPane().add(reorderingFastPane, BorderLayout.NORTH);
//						dialog.getContentPane().add(reorderingPane, BorderLayout.CENTER);
//						dialog.getContentPane().add(pRight, BorderLayout.EAST);
//						//dialog.getContentPane().setPreferredSize( new Dimension( reorderingPane.getPreferredSize().getWidth() +  ) );
//						dialog.pack();
//						dialog.setVisible(true);
//
//						reorderingPane.registerCardPaneEventListener(new JCardPane.ComponentRepaintListener(reorderingFastPane));
//
//						//					// set viewport and zoom factor..
//						//					double cw = highlighted.getRectangle().getWidth();
//						//					double ch = highlighted.getRectangle().getHeight();
//						//					double vpx = cw * 1.25 * nCardsToShow;
//						//					reorderingPane.setViewport( vpx , 0 , reorderingPane.getWidth() / (2*vpx) );
//					}
//				}
//			}
//
//			break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {

	}


    public void initRightPanel() {
		this.mRightPane.setLayout(new BoxLayout(this.mRightPane,BoxLayout.Y_AXIS));

		// init the top row of buttons..
		JPanel pTopRow =  getHorizontalGridLayoutPanel(1, 4);
		JButton b1 = new JButton("grid");
		JButton b2 = new JButton("rad+ang");
		JButton b3 = new JButton("circle");
		JButton b4 = new JButton("spiral");

		pTopRow.add(b1);
		pTopRow.add(b2);
		pTopRow.add(b3);
		pTopRow.add(b4);

		this.mRightPane.add(pTopRow);
	}

	private static JPanel getHorizontalGridLayoutPanel(int rows, int cols){
		JPanel p = new JPanel(); p.setLayout(new GridLayout(rows,cols));
		p.setAlignmentX( JComponent.LEFT_ALIGNMENT );
		p.setPreferredSize( new Dimension(2000,40) );
		p.setMaximumSize( new Dimension(2000,40) );
		return p;
	}

	private static JPanel getHorizontalBoxLayoutPanel(){
		JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
		p.setAlignmentX( JComponent.LEFT_ALIGNMENT );
		p.setPreferredSize( new Dimension(2000,40) );
		return p;
	}


	private List<JCardWizard2.ColumnWithType> mGlobalConfig = new ArrayList<>();

	public void setGlobalConfig(List<JCardWizard2.ColumnWithType> columns) {
		this.mGlobalConfig = columns;
	}

	public List<JCardWizard2.ColumnWithType> getGlobalConfig() {
		return mGlobalConfig;
	}


//	private static final String TEXT_CARD_STACK = com.actelion.research.datawarrior   DETaskCreateStackFromSelection.TASK_NAME;
//	private static final String TEXT_CARD_CONFIGURE = "Configure Card"+"...";
//	private static final String TEXT_CARD_POSITION = "Position Cards"+"...";
//	private static final String TEXT_CARD_WIZARD = "Card Wizard"+"...";
//
//	public void createPopupMenu(JPopupMenu popup){
//	}


    /**
     * Picks the first structure column from the model, and the first three numeric columns.
     *
     */
    public void initInitialCardConfiguration() {

        List<JCardWizard2.ColumnWithType> columns_selected = JCardWizard2.createDefaultConfig_2(mTableModel);
        CardElementLayoutLogic.CardAndStackConfig config = mCardElementLayoutLogic.createFullCardWizardConfig(columns_selected);

        mCardPane.getCardView().setGlobalConfig( columns_selected );
        mCardPane.getCardDrawer().setCardDrawingConfig(config.CDC);
        mCardPane.getStackDrawer().setStackDrawingConfig(config.SDC);
    }

    public CardElementLayoutLogic getCardElementLayoutLogic() {
        return mCardElementLayoutLogic;
    }


    /**
	 * Layered pane, filled with one main component.
	 * Provides slots to put components on top of the main component.
	 *
	 */
	//public static class JMyLayeredPane extends JPanel implements ComponentListener {
	public class JMyLayeredPane extends JLayeredPane implements ComponentListener {

		JComponent mMainComponent = null;

		int mTopRightWidth = 0;
		int mTopRightHeight = 0;
		JComponent mTopRight = null;

		/**
		 *
		 *
		 */
		public JMyLayeredPane(){
			super();
			this.setLayout(null);
			this.setVisible(true);
			this.setOpaque(true);

			//this.setMinimumSize(new Dimension(400,400));

			this.addComponentListener(this);
		}



//		public Dimension getPreferredSize(){
//			return new Dimension(4000,4000);
//		}
//
//		public Dimension getMinimumSize(){
//			return new Dimension(400,400);
//		}
//
//		public Dimension getMaximumSize(){
//			return new Dimension(8000,8000);
//		}



		public JComponent getTopRightComponent(){ return this.mTopRight; }

		@Override
		public void componentResized(ComponentEvent e) {
			this.layoutComponents();
		}

		@Override
		public void componentMoved(ComponentEvent e) {

		}

		@Override
		public void componentShown(ComponentEvent e) {

		}

		@Override
		public void componentHidden(ComponentEvent e) {

		}

		private boolean mTopRightResizable = false;
		private boolean mTopRightCloseable = false;

		public void setTopRightComponent(JComponent c, int w, int h, boolean resizable, boolean closeable) {
			this.mTopRightWidth = w;
			this.mTopRightHeight = h;

			this.mTopRightResizable = resizable;
			this.mTopRightCloseable = closeable;

			if(mTopRight!=null){
				this.remove(mTopRight);
			}

			mTopRight = c;
			if(c!=null) {
				this.add(mTopRight, 2);
				//if(mResizeKnob_TopRight!=null){ this.mResizeKnob_TopRight.setEnabled(true);}
				//if(mResizeKnob_TopRight!=null){ this.mResizeKnob_TopRight.setEnabled(true);}
			}
			else{
				//if(mResizeKnob_TopRight!=null){ this.mResizeKnob_TopRight.setEnabled(false);}
			}

			this.layoutComponents();
		}

		public void setSizeTopRightComponent(int w, int h){
			this.mTopRightWidth = w;
			this.mTopRightHeight = h;
			this.layoutComponents();
		}

		public void setMainComponent(JComponent c){
			if(mMainComponent!=null){this.remove(mMainComponent);}
			this.mMainComponent = c;
			this.add( c , new Integer(1) );
			this.layoutComponents();
		}


		JResizeKnob  mResizeKnob_TopRight = null;
		JCloseButton mCloseButton_TopRight = null;


		public void layoutComponents() {
            if(logLevel>0) {
                System.out.println("layoutComponents: size " + this.getWidth() + "," + this.getHeight());
            }

			//super.validate();
			if(this.mMainComponent!=null) {
				//this.add(mMainComponent,1);

				this.mMainComponent.setBounds(0, 0, this.getWidth(), this.getHeight());
				//this.mMainComponent.setBounds(0, 0, 600, 600);
				this.mMainComponent.setOpaque(true);
				//this.mMainComponent.setComponentZOrder( mMainComponent ,5);
			}
			if(this.mTopRight!=null) {

				this.mTopRight.setBounds(new Rectangle(this.getWidth() - this.mTopRightWidth, 0, this.mTopRightWidth, this.mTopRightHeight));
				// put it to front..
				this.setComponentZOrder( mTopRight ,0);

				// add resize knob:
				if(mResizeKnob_TopRight==null){
					//System.out.println("Crete Resize Knob TR");
					mResizeKnob_TopRight = new JResizeKnob();
					MyResizeKnobMouseResizeListener_TopRight rkmrl = new MyResizeKnobMouseResizeListener_TopRight();
					mResizeKnob_TopRight.addMouseListener(rkmrl);
					mResizeKnob_TopRight.addMouseMotionListener(rkmrl);
					mResizeKnob_TopRight.setVisible(true);
					mResizeKnob_TopRight.setOpaque(true);
					this.add(mResizeKnob_TopRight);
					this.setComponentZOrder(mResizeKnob_TopRight,0);
				}
				if(mCloseButton_TopRight==null){
					//System.out.println("Crete Close Button TR");
					mCloseButton_TopRight = new JCloseButton();
					MyCloseButtonActionListener_TopRight mcbml = new MyCloseButtonActionListener_TopRight();
					mCloseButton_TopRight.addActionListener(mcbml);
					mCloseButton_TopRight.setVisible(true);
					mCloseButton_TopRight.setOpaque(true);
					this.add(mCloseButton_TopRight);
					this.setComponentZOrder(mCloseButton_TopRight,0);
					//this.setLayer(mCloseButton_TopRight,10);
					this.moveToFront(mCloseButton_TopRight);
				}


				if(mResizeKnob_TopRight.isEnabled()) {
					mResizeKnob_TopRight.setBounds(this.getWidth() - this.mTopRightWidth - mConf_SizeKnob / 2, this.mTopRightHeight - mConf_SizeKnob / 2, mConf_SizeKnob, mConf_SizeKnob);
					this.moveToFront(mResizeKnob_TopRight);
				}
				if(mCloseButton_TopRight.isEnabled()) {
					mCloseButton_TopRight.setBounds( this.getWidth() - 16 , 0 , 16,16 );
					this.moveToFront(mCloseButton_TopRight);
				}
			}
			repaint();
		}



		// @TODO : add resize functionality..

		private int mConf_SizeKnob = HiDPIHelper.scale(8);

		@Override
		public void paintComponent(Graphics g){
			super.paintComponent(g);
			//g.setColor(Color.cyan);
			//g.drawString("GUUUGGGUUUUUUUSSS!!!",200,200);

			// draw top right size knob:
			//g.setColor(Color.blue);
			//g.fillRect( this.getWidth()-this.mTopRightWidth-mConf_SizeKnob/2, this.mTopRightHeight-mConf_SizeKnob/2 , mConf_SizeKnob , mConf_SizeKnob );
		}

		private boolean mMouseOverTopRightResizeKnob = false;

		@Override
		public void paint(Graphics g){
			super.paint(g);
			// draw top right size knob:
			if(mMouseOverTopRightResizeKnob) {
				g.setColor(Color.magenta.darker());
			}
			else{
				g.setColor(Color.blue);
			}
			//g.fillRect( this.getWidth()-this.mTopRightWidth-mConf_SizeKnob/2, this.mTopRightHeight-mConf_SizeKnob/2 , mConf_SizeKnob , mConf_SizeKnob );
		}


		private Rectangle2D getTopRightKnobRectangle(){
			return new Rectangle2D.Double( this.getWidth()-this.mTopRightWidth-mConf_SizeKnob/2, this.mTopRightHeight-mConf_SizeKnob/2 , mConf_SizeKnob , mConf_SizeKnob );
		}


		class MyCloseButtonActionListener_TopRight implements ActionListener {
			@Override
			public void actionPerformed(ActionEvent e) {
				turnOffOverviewPane();
			}
		}

		class MyResizeKnobMouseResizeListener_TopRight implements MouseListener , MouseMotionListener {

			boolean mKnobDragged = false;

			Point2D mKnobDragStart;

			public void startDrag(Point2D p){
				this.mKnobDragged = true;
				this.mKnobDragStart = p;
			}

			public void endDrag(){
				this.mKnobDragged = false;
			}

			@Override
			public void mouseClicked(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
				this.startDrag(e.getPoint());
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				this.endDrag();
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				mMouseOverTopRightResizeKnob = true;
				repaint();
			}

			@Override
			public void mouseExited(MouseEvent e) {
				mMouseOverTopRightResizeKnob = false;
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				//System.out.println("mouse dragged!! ... ... ... knob dragged = "+this.mKnobDragged);
				//if(mKnobDragged){
					double topRightWidth  = mTopRightWidth  - ( e.getX() - this.mKnobDragStart.getX() );
					double topRightHeight = mTopRightHeight + ( e.getY() - this.mKnobDragStart.getY() );

					setSizeTopRightComponent( (int) topRightWidth, (int) topRightHeight);
				//}
				repaint();
			}

			@Override
			public void mouseMoved(MouseEvent e) {
			}
		}

		public class JResizeKnob extends JLabel {
			boolean mMouseOver = false;

			public JResizeKnob(){
				this.addMouseListener( new MyMouseOverListener() );
			}

			public void paintComponent(Graphics g){
				if(!this.isEnabled() || !mTopRightResizable ){
					return;
				}

				if(this.mMouseOver){
					g.setColor(Color.magenta.darker());
				}
				else{
					g.setColor(Color.blue);
				}

				g.fillRect(0,0,this.getWidth(),this.getHeight());
			}

			class MyMouseOverListener implements MouseListener{
				@Override
				public void mouseClicked(MouseEvent e) {

				}

				@Override
				public void mousePressed(MouseEvent e) {

				}

				@Override
				public void mouseReleased(MouseEvent e) {

				}

				@Override
				public void mouseEntered(MouseEvent e) {
					mMouseOver = true;
				}

				@Override
				public void mouseExited(MouseEvent e) {
					mMouseOver = false;
				}
			}
		}

		public class JCloseButton extends JButton {
			boolean mMouseOver = false;

			public JCloseButton()
			{
				super("X");
				this.addMouseListener( new MyMouseOverListener() );
			}

			public void paintComponent(Graphics g){
				if(!this.isEnabled() || !mTopRightCloseable){
					return;
				}
				super.paintComponent(g);

//				if(this.mMouseOver){
//					g.setColor(Color.magenta.darker());
//				}
//				else{
//					g.setColor(Color.blue);
//				}
//
//				g.fillRect(0,0,this.getWidth(),this.getHeight());
			}

			class MyMouseOverListener implements MouseListener{
				@Override
				public void mouseClicked(MouseEvent e) {

				}

				@Override
				public void mousePressed(MouseEvent e) {

				}

				@Override
				public void mouseReleased(MouseEvent e) {

				}

				@Override
				public void mouseEntered(MouseEvent e) {
					mMouseOver = true;
				}

				@Override
				public void mouseExited(MouseEvent e) {
					mMouseOver = false;
				}
			}
		}




	}





	class HitListListener implements CompoundTableListListener {
        @Override
        public void listChanged(CompoundTableListEvent e) {
            setFocusList(e.getListIndex());
        }

    }











//
//    class MyListSelectionListener implements ListSelectionListener {
//		@Override
//		public void valueChanged(ListSelectionEvent e) {
//
//		}
//	}
}


//
//class CardPaneDetailPopupMenu extends JPopupMenu implements ActionListener, ItemListener {
//
//    public CardPaneDetailPopupMenu(DEMainPane mainPane, List<CardElement> selection){
//        super();
//    }
//
//
//    @Override
//    public void actionPerformed(ActionEvent e) {
//
//    }
//
//    @Override
//    public void itemStateChanged(ItemEvent e) {
//
//    }
//}
//}