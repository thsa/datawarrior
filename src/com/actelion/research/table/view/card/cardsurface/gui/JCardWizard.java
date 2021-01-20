package com.actelion.research.table.view.card.cardsurface.gui;

import com.actelion.research.table.view.card.JCardPane;
import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.card.cardsurface.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.awt.datatransfer.DataFlavor.stringFlavor;

/**
 * A dialog which helps initializing Cards and Stacks in a reasonable way.
 * The key component are lists of columns that can be shown. These can be drag&dropped into a list. This, with a minimal
 * amount of additional data is then used to construct the card configuration, and a template for the stack configuration
 * as well.
 *
 * LAYOUT:
 * Top level is a box plot (left to right) with panels:
 * Available categories | To Show List , and below some config stuff | PREVIEW PANEL CARD / STACK | Stack config
 *
 *
 *
 *
// * Technical: Drag and drop of columns:
// * Transfer objects are strings, using the following encoding: "d/x", where d is the integer number of the column, and
// * x indicates the type number (ColumnWithType.TYPE_ , i.e. "s","r","n"), so e.g.  "15/n" would be the column 15 with type numeric.
 *
 */
public class JCardWizard extends JPanel implements ListDataListener , ActionListener {

//    private static final String TYPE_CHARACTER_CHEM_STRUCTURE  = "s";
//    private static final String TYPE_CHARACTER_CHEM_REACTION   = "r";
//    private static final String TYPE_CHARACTER_NUMERICE        = "n";

    private static final String ACTION_COMMAND_APPLY_BUTTON = "ApplyButton";

    /**
     * Size of leftmost panel (available columns)
     */
    private static final int CONF_WIDTH_PANEL_A = 200;

    /**
     * Size of second leftmost panel (card config)
     */
    private static final int CONF_WIDTH_PANEL_B = 200;




    /**
     * Left-most panel, will contain the available columns
     */
    private JPanel mPanelAvailableColumns = null;

    /**
     * Second panel, contains drop-receiver table and additional config panel below..
     */
    private JPanel mPanelCardAndStackConfig = null;

    /**
     * PREVIEW PANEL CARD
     */
    private JPanel mPanelPreviewCard = null;

    /**
     */
    private JCardPreviewPanel mCardPreviewPanel_Card  = null;

    /**
     */
    private JCardPreviewPanel mCardPreviewPanel_Stack = null;


    /**
     * PREVIEW PANEL STACK
     */
    private JPanel mPanelPreviewStack = null;

    /**
     * Stack config panel
     */
    private JPanel mPanelStackConfig = null;


    /**
     * Card drawing config..
     */
    private CardDrawingConfig mCDC = null;

    /**
     * Stack drawing config..
     */
    private StackDrawingConfig mSDC = null;



    private CompoundTableModel mModel = null;

    private CompoundTableColorHandler mColorHandler = null;

    private JCardPane mCardPane = null;

    public JCardWizard(CompoundTableModel model, CompoundTableColorHandler colorHandler, JCardPane cardPane , CardDrawingConfig cdc, StackDrawingConfig sdc){

        this.mModel = model;
        this.mColorHandler = colorHandler;
        this.mCDC   = cdc;
        this.mSDC   = sdc;

        this.mCardPane = cardPane;

        initAvailableColumnsPanel();

        initCardConfigPanel();

        this.initGUIfromCDC();


        this.setLayout(new BoxLayout(this,BoxLayout.X_AXIS));

        this.add(mPanelAvailableColumns);
        this.add(mPanelCardAndStackConfig);
        this.add(mPanelPreviewCard);

        initCardPreviewPanel();

        this.mListColumnsToShow.getModel().addListDataListener(this);

        this.revalidate();
    }


    @Override
    public void intervalAdded(ListDataEvent e) {
        this.reinitCardAndStackWithCurrentValues();
    }

    @Override
    public void intervalRemoved(ListDataEvent e) {
        this.reinitCardAndStackWithCurrentValues();
    }

    @Override
    public void contentsChanged(ListDataEvent e) {
        this.reinitCardAndStackWithCurrentValues();
    }

    /**
 * ----------- Available columns panel --------------------------------------------------------------------
 */
    private JList<Integer> mListChemistryStructure = new JList<>();
    private JList<Integer> mListChemistryRxn       = new JList<>();
    private JList<Integer> mListNumeric            = new JList<>();

    private JButton        mApplyButton            = new JButton("Apply!");

    /**
     * Collect the chemistry and numeric columns from the compound table
     */
    public void initAvailableColumnsPanel(){

        mPanelAvailableColumns = new JPanel();
        mPanelAvailableColumns.setLayout(new BoxLayout(mPanelAvailableColumns,BoxLayout.Y_AXIS));


        ArrayList<Integer> vChemS  = new ArrayList<>();
        ArrayList<Integer> vChemR  = new ArrayList<>();
        ArrayList<Integer> vNum    = new ArrayList<>();

        for(int zi=0;zi<mModel.getColumnCount();zi++){
            if( mModel.isColumnTypeStructure(zi) ){
                vChemS.add(zi);
            }
            if( mModel.isColumnTypeReaction(zi) ){
                vChemR.add(zi);
            }
            if( mModel.isColumnTypeDouble(zi) ){
                vNum.add(zi);
            }
        }

        mListChemistryStructure.setListData( vChemS.toArray(new Integer[0]) );
        mListChemistryRxn.setListData( vChemR.toArray(new Integer[0]) );
        mListNumeric.setListData( vNum.toArray(new Integer[0]) );

        // set list cell renderer which renders column names / type..
        mListChemistryStructure.setCellRenderer( new ColumnWithConstantTypeRenderer( ColumnWithType.TYPE_CHEM_STRUCTURE ) );
        mListChemistryRxn.setCellRenderer( new ColumnWithConstantTypeRenderer( ColumnWithType.TYPE_CHEM_REACTION) );
        mListNumeric.setCellRenderer( new ColumnWithConstantTypeRenderer( ColumnWithType.TYPE_NUMERIC ) );


        JScrollPane jspA = new JScrollPane(mListChemistryStructure);
        JScrollPane jspB = new JScrollPane(mListChemistryRxn);
        JScrollPane jspC = new JScrollPane(mListNumeric);

        jspA.setSize(CONF_WIDTH_PANEL_A,240);
        jspB.setSize(CONF_WIDTH_PANEL_A,240);
        jspC.setSize(CONF_WIDTH_PANEL_A,240);

        mPanelAvailableColumns.add(new JLabel("CHEMICAL STRUCTURES"));
        mPanelAvailableColumns.add(jspA);
        mPanelAvailableColumns.add(new JLabel("CHEMICAL REACTIONS"));
        mPanelAvailableColumns.add(jspB);
        mPanelAvailableColumns.add(new JLabel("NUMERICAL VALUES"));
        mPanelAvailableColumns.add(jspC);

        // configure drag and drop:

        mListChemistryStructure.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mListChemistryRxn.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mListNumeric.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // transfer handler for type encoding:
        mListChemistryStructure.setTransferHandler( new MyTransferHandlerWithTypeData( ColumnWithType.TYPE_CHEM_STRUCTURE ) );
        mListChemistryRxn.setTransferHandler( new MyTransferHandlerWithTypeData( ColumnWithType.TYPE_CHEM_REACTION ) );
        mListNumeric.setTransferHandler( new MyTransferHandlerWithTypeData( ColumnWithType.TYPE_NUMERIC ) );


//        mListChemistryStructure.setDropMode(null);
//        mListChemistryRxn.setDropMode(null);
//        mListNumeric.setDropMode(null);

        mListChemistryStructure.setDragEnabled(true);
        mListChemistryRxn.setDragEnabled(true);
        mListNumeric.setDragEnabled(true);


        // and add apply button below..
        mApplyButton.setMinimumSize(new Dimension( 200,80 ));
        mPanelAvailableColumns.add(mApplyButton);
        mApplyButton.setActionCommand(ACTION_COMMAND_APPLY_BUTTON);
        mApplyButton.addActionListener(this);
    }

/**
 * ----------- Card config panel --------------------------------------------------------------------
 */

    private JList<ColumnWithType> mListColumnsToShow = null;


    private JSlider mSliderCardSizeStructure = null;
    private JSlider mSliderCardSizeRxn = null;
    private JSlider mSliderCardSizeNumerical = null;

    private JSlider mSliderStackSizeStructure = null;
    private JSlider mSliderStackSizeRxn = null;
    private JSlider mSliderStackSizeNumerical = null;


    private JPanel mPanelSuperCardConfig    = null;
    private JPanel mPanelSuperStackConfig   = null;
    private JPanel mPanelCardsAdvanced  = null;
    private JPanel mPanelStacksAdvanced = null;


    public void initCardConfigPanel(){
        mPanelCardAndStackConfig = new JPanel();
        mPanelCardAndStackConfig.setLayout(new BoxLayout(mPanelCardAndStackConfig,BoxLayout.Y_AXIS));

        mPanelCardAndStackConfig.add(new JLabel( "TO SHOW:" ));

        mListColumnsToShow = new JList();
        mListColumnsToShow.setModel(new DefaultListModel());
        mListColumnsToShow.setDropMode(DropMode.USE_SELECTION);

        JScrollPane jspA = new JScrollPane(mListColumnsToShow);
        jspA.setSize(CONF_WIDTH_PANEL_B,400);

        mPanelCardAndStackConfig.add(jspA);
        // init drag and drop
        mListColumnsToShow.setDropMode(DropMode.USE_SELECTION);
        mListColumnsToShow.setDragEnabled(true);
        mListColumnsToShow.setTransferHandler(new MyTransferHandler());


        //@TODO: add some more cool config stuff.. (choose num/bg-col/hist/...)

        // add sliders for size of structures / reactions / numerical..
        mSliderCardSizeStructure = new JSlider();
        mSliderCardSizeRxn = new JSlider();
        mSliderCardSizeNumerical = new JSlider();

        List<JSlider> sliders = new ArrayList<>();
        sliders.add(mSliderCardSizeStructure);sliders.add(mSliderCardSizeRxn);sliders.add(mSliderCardSizeNumerical);
        sliders.stream().forEach( s -> {s.setMinimum(0); s.setMaximum(100); s.setSize(CONF_WIDTH_PANEL_B,24); s.addChangeListener(new MyChangeListener()); } );

        JLabel labelA1 = new JLabel("Size Structures"); labelA1.setMaximumSize(new Dimension(80,24) );
        JLabel labelB1 = new JLabel("Size Reactions");  labelB1.setMaximumSize(new Dimension(80,24) );
        JLabel labelC1 = new JLabel("Size Numerical");  labelC1.setMaximumSize(new Dimension(80,24) );
        JLabel labelA2 = new JLabel("Size Structures"); labelA2.setMaximumSize(new Dimension(80,24) );
        JLabel labelB2 = new JLabel("Size Reactions");  labelB2.setMaximumSize(new Dimension(80,24) );
        JLabel labelC2 = new JLabel("Size Numerical");  labelC2.setMaximumSize(new Dimension(80,24) );


        JPanel cspA = getLeftAlignedHorizontalBoxLayoutPanel(); cspA.add(labelA1)  ; cspA.add(mSliderCardSizeStructure);
        JPanel cspB = getLeftAlignedHorizontalBoxLayoutPanel(); cspB.add(labelB1)  ; cspB.add(mSliderCardSizeRxn);
        JPanel cspC = getLeftAlignedHorizontalBoxLayoutPanel(); cspC.add(labelC1)  ; cspC.add(mSliderCardSizeNumerical);


        JPanel pSliders = getTopAlignedVerticalBoxLayoutPanel();
        pSliders.add(cspA); pSliders.add(cspB); pSliders.add(cspC);
//        mPanelCardConfig.add(spA); mPanelCardConfig.add(spB); mPanelCardConfig.add(spC);

        // super panel for pCardConfig and others
        mPanelSuperCardConfig = getLeftAlignedHorizontalBoxLayoutPanel();

        JPanel pCardConfig = getTopAlignedVerticalBoxLayoutPanel();
        pCardConfig.add(new JLabel("CARDS"));
        pCardConfig.add(pSliders);

        mPanelSuperCardConfig.add(pCardConfig);




        // CREATE STACKS CONFIG PANEL:
        // add sliders for size of structures / reactions / numerical..
        mSliderStackSizeStructure = new JSlider();
        mSliderStackSizeRxn = new JSlider();
        mSliderStackSizeNumerical = new JSlider();

        List<JSlider> sliders2 = new ArrayList<>();
        sliders2.add(mSliderStackSizeStructure);sliders2.add(mSliderStackSizeRxn);sliders2.add(mSliderStackSizeNumerical);
        sliders2.stream().forEach( s -> {s.setMinimum(0); s.setMaximum(100); s.setSize(CONF_WIDTH_PANEL_B,24); s.addChangeListener(new MyChangeListener()); } );


        JPanel pStackConfig = getTopAlignedVerticalBoxLayoutPanel();
        pStackConfig.add(new JLabel("STACKS"));

        JPanel sspA = getLeftAlignedHorizontalBoxLayoutPanel(); sspA.add(labelA2)  ; sspA.add(mSliderStackSizeStructure);
        JPanel sspB = getLeftAlignedHorizontalBoxLayoutPanel(); sspB.add(labelB2)  ; sspB.add(mSliderStackSizeRxn);
        JPanel sspC = getLeftAlignedHorizontalBoxLayoutPanel(); sspC.add(labelC2)  ; sspC.add(mSliderStackSizeNumerical);

        JPanel pStackSliders = getTopAlignedVerticalBoxLayoutPanel();
        pStackSliders.add(sspA); pStackSliders.add(sspB); pStackSliders.add(sspC);
        pStackConfig.add(pStackSliders);


        mPanelSuperStackConfig = getLeftAlignedHorizontalBoxLayoutPanel();
        mPanelSuperStackConfig.add(pStackConfig);


        mPanelCardAndStackConfig.add(mPanelSuperCardConfig);
        mPanelCardAndStackConfig.add(mPanelSuperStackConfig);




    }

    private static JPanel getLeftAlignedHorizontalBoxLayoutPanel(){
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setAlignmentY(Component.TOP_ALIGNMENT);
        return p;
    }
    private static JPanel getTopAlignedVerticalBoxLayoutPanel(){
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setAlignmentY(Component.TOP_ALIGNMENT);
        return p;
    }





    class MyChangeListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            if( e.getSource() instanceof JSlider ){
                JSlider es = (JSlider) e.getSource();
                if(! es.getValueIsAdjusting() ){
                    //reininit card..
                    reinitCardAndStackWithCurrentValues();
                }
            }
            //if( e.getSource() )
        }
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equals( ACTION_COMMAND_APPLY_BUTTON )){
            // apply the card / stack design:
            this.mCardPane.getCardDrawer().setCardDrawingConfig(this.mCDC);
            this.mCardPane.getStackDrawer().setStackDrawingConfig(this.mSDC);
        }
    }

    // Transfer Handler for JLists to export type together with column number.. :
    class MyTransferHandlerWithTypeData extends TransferHandler{
        int mType = -1;

        public MyTransferHandlerWithTypeData(int type){ this.mType = type;}

        @Override
        public int getSourceActions(JComponent comp) {
            return COPY;
        }

        /**
         * Add type to column number
         */
        @Override
        protected Transferable createTransferable(JComponent c) {
            JList myList = (JList) c;
            return new java.awt.datatransfer.StringSelection( ( myList.getModel().getElementAt( myList.getSelectedIndex() ) ) + "/" + this.mType );
        }
    }


    class ColumnWithConstantTypeRenderer extends DefaultListCellRenderer {

        private int mType = -1;

        public ColumnWithConstantTypeRenderer(int type){
            this.mType = type;
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel renderer = (JLabel) super.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);
//            if (isSelected) {
//                renderer.setForeground(Color.blue.brighter());
//            }
            renderer.setText( new ColumnWithType(mModel, (int) value, this.mType).toString() );
            return renderer;
        }
    }

    // Transfer Handler for JList with columsn to show:
    class MyTransferHandler extends TransferHandler{

        /**
         * Indicates the start position of an internal dnd.
         */
        private int mIndexDraggedInternally = -1;

        /**
         * Indicates the index where the drop was inserted.
         */
        private int mIndexDroppedTo         = -1;

        /**
         * We only support importing ints.
         */
        public boolean canImport(TransferHandler.TransferSupport info) {
            // Check for String flavor
            boolean supported = false;
            if (info.isDataFlavorSupported(stringFlavor)) {
                supported = true;
            }
            //System.out.println("SUPPORTED: "+supported);
            return supported;
        }

        public int getSourceActions(JComponent c) {
            return TransferHandler.MOVE;
        }

        /**
         */
        protected Transferable createTransferable(JComponent c) {

            JList myList = (JList) c;
            this.mIndexDraggedInternally = myList.getSelectedIndex();

            return new java.awt.datatransfer.StringSelection( ((ColumnWithType) myList.getModel().getElementAt( this.mIndexDraggedInternally)).getEncoded() ) ;

            //return new java.awt.datatransfer.StringSelection( (String) myList.getModel().getElementAt( this.mIndexDraggedInternally ));
        }


        protected void exportDone(JComponent c, Transferable t, int action) {
            if (action == MOVE) {
                //@TODO..
                //c.remove removeSelection();
                if(c.equals(mListColumnsToShow) ){
                    // internal dnd, remove the drag start index
                    try {
                        int indexToRemove = this.mIndexDraggedInternally + ( this.mIndexDroppedTo<this.mIndexDraggedInternally ? 1 : 0 );
                        (((DefaultListModel) (((JList) c).getModel()))).remove( indexToRemove );
                    }
                    catch(Exception e){}

                }
                else{
                    // we dont remove..
                }
            }
        }


        /**
         * Decode the transferred type (if included..)
         *
         * @param supp
         * @return
         */
        public boolean importData(TransferSupport supp) {
            if (!canImport(supp)) {
                return false;
            }
            try {
                System.out.println("Do import!");

                JList list = (JList)( supp.getComponent() );
                DefaultListModel listModel = (DefaultListModel) (list.getModel()) ;

                // Fetch the Transferable and its data
                Transferable t = supp.getTransferable();
                //String data = (String) t.getTransferData(stringFlavor);
                ColumnWithType data =  new ColumnWithType( mModel, (String) t.getTransferData(stringFlavor) );

                // Fetch the drop location
                int loc = ((JList.DropLocation) supp.getDropLocation()).getIndex();

                // Insert the data at this location
                if(loc<0){
                    if(listModel.size()==0){loc=0;}
                    else{ loc=listModel.size();}
                }
                System.out.println("ADD NEW: "+data+" at: "+loc);
                mIndexDroppedTo = loc;
                listModel.add(loc, data);
                //listModel.add(loc, new ColumnWithType(mModel,Integer.parseInt(data),1) );
            }
            catch(Exception e){
                System.out.println("something went wrong: "+e.toString());
            }
            return true;
        }
    }


    /**
     * Initializes the mListColumnsToShow witht the values from the current card drawing config.
     *
     */
    public void initGUIfromCDC(){
        ((DefaultListModel<ColumnWithType>) this.mListColumnsToShow.getModel() ).removeAllElements();

        for( CardDrawingConfig.CardSurfaceSlot css : this.mCDC.getSlots() ){
            boolean foundEntry = false;
            if(  css.getSurfacePanel() instanceof StructurePanel){
                foundEntry=true;
                ((DefaultListModel<ColumnWithType>) this.mListColumnsToShow.getModel()).addElement(  new ColumnWithType(mModel, css.getColumns()[0],ColumnWithType.TYPE_CHEM_STRUCTURE ) );
            }
            if(css.getSurfacePanel() instanceof MultiDataPanel){
                foundEntry=true;
                for( int ci : css.getSurfacePanel().getColumns() ){
                    ((DefaultListModel<ColumnWithType>) this.mListColumnsToShow.getModel()).addElement(  new ColumnWithType(mModel, ci , ColumnWithType.TYPE_NUMERIC ) );
                }
            }
            if(css.getSurfacePanel() instanceof SingleDataPanel){
                foundEntry=true;
                ((DefaultListModel<ColumnWithType>) this.mListColumnsToShow.getModel()).addElement(  new ColumnWithType(mModel, css.getSurfacePanel().getColumns()[0] , ColumnWithType.TYPE_NUMERIC ) );

            }
            if(!foundEntry){
                System.out.println("ERROR: could not initialize the card wizard GUI from the current CDC data..");
            }
        }

        this.reinitCardAndStackWithCurrentValues();
    }


    /**
     * ----------- Card Preview Panel --------------------------------------------------------------------------
     *
     * i.e. card and stack preview panels
     *
     */

    public void initCardPreviewPanel(){
        //mPanelPreviewCard = new JPanel();

        if(this.mPanelPreviewCard==null){
            //this.mPanelPreviewCard=new JPanel();
            //this.mPanelPreviewCard.setLayout(new BoxLayout(this.mPanelPreviewCard,BoxLayout.Y_AXIS));
            this.mPanelPreviewCard = getTopAlignedVerticalBoxLayoutPanel();
        }

        this.mPanelPreviewCard.setMinimumSize(new Dimension( 300,300 ));
        this.mPanelPreviewCard.removeAll();

        // card preview:
        //this.mCardPreviewPanel_Card = new JCardPreviewPanel(mModel, new DefaultCardDrawer(mModel, mColorHandler), this.mCDC , new DefaultStackDrawer(mModel, mColorHandler) , this.mSDC );
        //this.mCardPreviewPanel_Card = new JCardPreviewPanel(mModel, new DefaultCardDrawer(mModel, mColorHandler), this.mCDC , new DefaultStackDrawer(mModel, mColorHandler) , this.mSDC );
        this.mCardPreviewPanel_Card = new JCardPreviewPanel(mModel, mColorHandler , this.mCDC , this.mSDC );
        this.mCardPreviewPanel_Card.setShowRandomCard();
        mPanelPreviewCard.add(mCardPreviewPanel_Card);
        this.mCDC.registerConfigChangeListener(mCardPreviewPanel_Card);

        // stack preview:
        //this.mCardPreviewPanel_Stack = new JCardPreviewPanel(mModel, new DefaultCardDrawer(mModel, mColorHandler), this.mCDC , new DefaultStackDrawer(mModel, mColorHandler) , this.mSDC);
        this.mCardPreviewPanel_Stack = new JCardPreviewPanel(mModel, mColorHandler , this.mCDC , this.mSDC);
        this.mCardPreviewPanel_Stack.setShowRandomStack(4);
        mPanelPreviewCard.add(mCardPreviewPanel_Stack);
        this.mSDC.registerConfigChangeListener(mCardPreviewPanel_Stack);

        this.mCDC.fireConfigChangeEvent();
        this.mSDC.fireConfigChangeEvent();

    }










    /**
     * ----------- Create card configuration ------------------------------------------------------------------
     */


    private CardWizardCardConfig  mCWCC = null;

    private CardWizardStackConfig mCWSC = null;

    /**
     * Reads all the values from the GUI elements, creates the CardWizardCardConfig object and calls
     * createCardConfiguration(..)
     *
     */
    public void reinitCardAndStackWithCurrentValues(){

        List<ColumnWithType> listColumns = new ArrayList<>();

        for(int zi=0; zi<this.mListColumnsToShow.getModel().getSize(); zi++){
            listColumns.add( mListColumnsToShow.getModel().getElementAt(zi) );
        }

        // CARD CONFIG..
        double sCS = this.mSliderCardSizeStructure.getValue()/100.0;
        double sCR = this.mSliderCardSizeRxn.getValue()/100.0;
        double sNu = this.mSliderCardSizeNumerical.getValue()/100.0;

        CardWizardCardConfig cwcc = new CardWizardCardConfig( listColumns , sCS , sCR , sNu );
        this.mCWCC = cwcc;

        // now create actual card configuration:
        createCardConfiguration( this.mCWCC );

        // STACK CONFIG..
        double ssCS = this.mSliderStackSizeStructure.getValue()/100.0;
        double ssCR = this.mSliderStackSizeRxn.getValue()/100.0;
        double ssNu = this.mSliderStackSizeNumerical.getValue()/100.0;

        CardWizardStackConfig cwsc = new CardWizardStackConfig( listColumns , ssCS , ssCR , ssNu );
        this.mCWSC = cwsc;

        // now create actual stack configuration:
        createStackConfiguration( this.mCWSC );


    }



    /**
     * Creates the actual card config from the wizard configuration.
     *
     * @param config
     */
    public void createCardConfiguration( CardWizardCardConfig config )
    {

        //  1. count the number of columns / types that we have:
        List<ColumnWithType> listCS = config.getColumns().stream().filter( ci -> ci.type == ColumnWithType.TYPE_CHEM_STRUCTURE ).collect(Collectors.toList());
        List<ColumnWithType> listCR = config.getColumns().stream().filter( ci -> ci.type == ColumnWithType.TYPE_CHEM_REACTION ).collect(Collectors.toList());
        List<ColumnWithType> listNu = config.getColumns().stream().filter( ci -> ci.type == ColumnWithType.TYPE_NUMERIC ).collect(Collectors.toList());

        int total = config.getColumns().size();
        int nCS   = listCS.size();
        int nCR   = listCR.size();
        int nNu   = listNu.size();

        // magic formula to compute percentage for different parts:
        double relCS = config.mSizeChemistryStructure;
        double relCR = config.mSizeChemistryRxn;
        double relNu = config.mSizeNumeric;

        double preCS = 1.0 * relCS * nCS;
        double preCR = 1.0 * relCR * nCR;
        double preNu = 1.0 * relNu; // here we dont scale up

        // normalize: --> final values
        double normalization = preCS + preCR + preNu;
        preCS = preCS / normalization;
        preCR = preCR / normalization;
        preNu = preNu / normalization;

        double relPerCS = preCS / nCS;
        double relPerCR = preCR / nCR;
        double relPerNu = preNu / nNu;

        // put thems panels
        // We just use the order chem stuff / then all numeric stuff.. this is for simplicity, could be extended to mix stuff, but this requires quite some additional code..
        CardDrawingConfig cdc = new CardDrawingConfig(mColorHandler);
        // transfer all config change listeners:
        this.mCDC.getConfigChangeListeners().forEach( ccl -> cdc.registerConfigChangeListener(ccl) );

        List<ColumnWithType> chemicalColumns  = new ArrayList<>();
        List<ColumnWithType> numericalColumns = new ArrayList<>();

        for(ColumnWithType cwt : config.getColumns()){
            if( cwt.isChemical() ){
                chemicalColumns.add(cwt);
            }
            else{
                numericalColumns.add( cwt );
            }
        }

        // add themz:
        double currentPosition = 0.0;
        for( ColumnWithType cc : chemicalColumns ){
            if(cc.type==ColumnWithType.TYPE_CHEM_STRUCTURE) {
                StructurePanel sp = new StructurePanel();
                sp.setColumns(new int[]{cc.column});
                cdc.getSlots().add(new CardDrawingConfig.CardSurfaceSlot(cdc, sp, currentPosition, relPerCS));
                currentPosition += relPerCS;
            }
            if(cc.type==ColumnWithType.TYPE_CHEM_REACTION) {
                // @TODO add reaction panel (i.e. figure out what has to be changed..)
                StructurePanel sp = new StructurePanel();
                sp.setColumns(new int[]{cc.column});
                cdc.getSlots().add(new CardDrawingConfig.CardSurfaceSlot(cdc, sp, currentPosition, relPerCR));
                currentPosition += relPerCR;
            }
        }

        // add multipanel for numerical values below:
        MultiDataPanel mdp = new MultiDataPanel();
        mdp.setNumberOfColumns(numericalColumns.size());
        mdp.setColumns( numericalColumns.stream().mapToInt(  nci -> nci.column).toArray() );

        cdc.getSlots().add( new CardDrawingConfig.CardSurfaceSlot( cdc , mdp, currentPosition , numericalColumns.size() * relPerNu ) );
        cdc.registerConfigChangeListener(this.mCardPreviewPanel_Card);

        this.setCardDrawingConfig(cdc);
    }




    public void createStackConfiguration( CardWizardStackConfig config ){
        //  1. count the number of columns / types that we have:
        List<ColumnWithType> listCS = config.getColumns().stream().filter( ci -> ci.type == ColumnWithType.TYPE_CHEM_STRUCTURE ).collect(Collectors.toList());
        List<ColumnWithType> listCR = config.getColumns().stream().filter( ci -> ci.type == ColumnWithType.TYPE_CHEM_REACTION ).collect(Collectors.toList());
        List<ColumnWithType> listNu = config.getColumns().stream().filter( ci -> ci.type == ColumnWithType.TYPE_NUMERIC ).collect(Collectors.toList());

        int total = config.getColumns().size();
        int nCS   = listCS.size();
        int nCR   = listCR.size();
        int nNu   = listNu.size();

        // magic formula to compute percentage for different parts:
        double relCS = config.mSizeChemistryStructure;
        double relCR = config.mSizeChemistryRxn;
        double relNu = config.mSizeNumeric;

        double preCS = 1.0 * relCS * nCS;
        double preCR = 1.0 * relCR * nCR;
        double preNu = 1.0 * relNu; // here we dont scale up

        // normalize: --> final values
        double normalization = preCS + preCR + preNu;
        preCS = preCS / normalization;
        preCR = preCR / normalization;
        preNu = preNu / normalization;

        double relPerCS = preCS / nCS;
        double relPerCR = preCR / nCR;
        double relPerNu = preNu / nNu;

        // put thems panels
        // We just use the order chem stuff / then all numeric stuff.. this is for simplicity, could be extended to mix stuff, but this requires quite some additional code..
        StackDrawingConfig sdc = new StackDrawingConfig(mColorHandler);
        // transfer all config change listeners:
        this.mSDC.getConfigChangeListeners().forEach( ccl -> sdc.registerConfigChangeListener(ccl) );

        List<ColumnWithType> chemicalColumns  = new ArrayList<>();
        List<ColumnWithType> numericalColumns = new ArrayList<>();

        for(ColumnWithType cwt : config.getColumns()){
            if( cwt.isChemical() ){
                chemicalColumns.add(cwt);
            }
            else{
                numericalColumns.add( cwt );
            }
        }

        // add themz:
        double currentPosition = 0.0;
        for( ColumnWithType cc : chemicalColumns ){
            if(cc.type==ColumnWithType.TYPE_CHEM_STRUCTURE) {
                StackChemStructurePanel sp = new StackChemStructurePanel();
                sp.setColumns(new int[]{cc.column});
                sdc.getSlots().add(new StackDrawingConfig.StackSurfaceSlot( sp,   currentPosition, relPerCS));
                currentPosition += relPerCS;
            }
            if(cc.type==ColumnWithType.TYPE_CHEM_REACTION) {
                // @TODO add reaction panel (i.e. figure out what has to be changed..)
                StackChemStructurePanel sp = new StackChemStructurePanel();
                sp.setColumns(new int[]{cc.column});
                sdc.getSlots().add(new StackDrawingConfig.StackSurfaceSlot( sp,  currentPosition, relPerCR));
                currentPosition += relPerCR;
            }
        }

        // add multipanel for numerical values below:
        StackMultiDataPanel mdp = new StackMultiDataPanel();
        mdp.setNumberOfColumns(numericalColumns.size());
        mdp.setColumns( numericalColumns.stream().mapToInt(  nci -> nci.column).toArray() );

        sdc.getSlots().add( new StackDrawingConfig.StackSurfaceSlot( mdp,  currentPosition , numericalColumns.size() * relPerNu ) );

        this.setStackDrawingConfig(sdc);
    }


    public void setCardDrawingConfig(CardDrawingConfig cdc){
        // transfer all listeners to new cdc..
        CardDrawingConfig oldCDC = this.mCDC;
        this.mCDC = cdc;

        for( CardDrawingConfig.CardDrawingConfigChangeListener cdccl : oldCDC.getConfigChangeListeners() ){
            this.mCDC.registerConfigChangeListener( cdccl );
        }

        this.mCDC.fireConfigChangeEvent();
        // redraw..
        this.initCardPreviewPanel();
        this.revalidate();
    }

    public void setStackDrawingConfig(StackDrawingConfig sdc){
        // transfer all listeners to the new scd..
        StackDrawingConfig oldSDC = this.mSDC;
        this.mSDC = sdc;

        for( StackDrawingConfig.StackDrawingConfigChangeListener sdccl : oldSDC.getConfigChangeListeners() ){
            this.mSDC.registerConfigChangeListener( sdccl );
        }

        this.mSDC.fireConfigChangeEvent();
        // redraw..
        this.initCardPreviewPanel();
        this.revalidate();
    }



    // @TODO add numeric column config stuff..
    public static class CardWizardCardConfig{

        public List<ColumnWithType> getColumns() {
            return mColumns;
        }

        public void setColumns(List<ColumnWithType> mColumns) {
            this.mColumns = mColumns;
        }

        public double getSizeChemistryStructure() {
            return mSizeChemistryStructure;
        }

        public void setSizeChemistryStructure(double mSizeChemistryStructure) {
            this.mSizeChemistryStructure = mSizeChemistryStructure;
        }

        public double getSizeChemistryRxn() {
            return mSizeChemistryRxn;
        }

        public void setSizeChemistryRxn(double mSizeChemistryRxn) {
            this.mSizeChemistryRxn = mSizeChemistryRxn;
        }

        public double getSizeNumeric() {
            return mSizeNumeric;
        }

        public void setmSizeNumeric(double mSizeNumeric) {
            this.mSizeNumeric = mSizeNumeric;
        }

        private List<ColumnWithType> mColumns;

        private double mSizeChemistryStructure;
        private double mSizeChemistryRxn;
        private double mSizeNumeric;

        public CardWizardCardConfig(List<ColumnWithType> columns , double sizeChemistryStructure , double sizeChemistryRxn, double sizeNumeric ){
            this.mColumns = columns;

            double totalSize = sizeChemistryStructure + sizeChemistryRxn + sizeNumeric;

            this.mSizeChemistryStructure = sizeChemistryStructure / totalSize;
            this.mSizeChemistryRxn       = sizeChemistryRxn / totalSize;
            this.mSizeNumeric            = sizeNumeric / totalSize;
        }

    }


    public static class CardWizardStackConfig{

        public List<ColumnWithType> getColumns(){ return mColumns; }

        public void setColumns(List<ColumnWithType> mColumns) {
            this.mColumns = mColumns;
        }

        public double getSizeChemistryStructure() {
            return mSizeChemistryStructure;
        }

        public void setSizeChemistryStructure(double mSizeChemistryStructure) {
            this.mSizeChemistryStructure = mSizeChemistryStructure;
        }

        public double getSizeChemistryRxn() {
            return mSizeChemistryRxn;
        }

        public void setSizeChemistryRxn(double mSizeChemistryRxn) {
            this.mSizeChemistryRxn = mSizeChemistryRxn;
        }

        public double getSizeNumeric() {
            return mSizeNumeric;
        }

        public void setmSizeNumeric(double mSizeNumeric) {
            this.mSizeNumeric = mSizeNumeric;
        }

        private List<ColumnWithType> mColumns;

        private double mSizeChemistryStructure;
        private double mSizeChemistryRxn;
        private double mSizeNumeric;

        public CardWizardStackConfig(List<ColumnWithType> columns , double sizeChemistryStructure , double sizeChemistryRxn, double sizeNumeric ){
            this.mColumns = columns;

            double totalSize = sizeChemistryStructure + sizeChemistryRxn + sizeNumeric;

            this.mSizeChemistryStructure = sizeChemistryStructure / totalSize;
            this.mSizeChemistryRxn       = sizeChemistryRxn / totalSize;
            this.mSizeNumeric            = sizeNumeric / totalSize;
        }
    }



    public static void main(String args[]){
        JFrame f = new JFrame();
        f.getContentPane().setLayout(new BorderLayout());
        MockCompoundTableModel mockModel = new MockCompoundTableModel();
        CompoundTableColorHandler colorHandler = new CompoundTableColorHandler(new MockCompoundTableModel());
        JCardWizard jp = new JCardWizard(mockModel, colorHandler, null, new CardDrawingConfig(colorHandler) , new StackDrawingConfig(colorHandler) );
        f.getContentPane().add(jp, BorderLayout.CENTER);
        f.setSize(800, 400);
        f.getContentPane().validate();
        f.setVisible(true);
    }


    public class ColumnWithType{
        public static final int TYPE_CHEM_STRUCTURE = 1;
        public static final int TYPE_CHEM_REACTION  = 2;
        public static final int TYPE_NUMERIC        = 3;

        public int column = -1;
        public int type   = -1;
        private CompoundTableModel mModel = null;


        /**
         * Automatically decodes either "colum" (e.g. "13") or column with type (e.g. "13/1")
         *
         * @param model
         * @param encoded
         */
        public ColumnWithType(CompoundTableModel model, String encoded){
            this.mModel = model;
            if(encoded.contains("/")){
                String substrings[] = encoded.split("/");
                if (substrings.length != 2) {
                    throw new Error("Could not decode column with type");
                }
                this.column = Integer.parseInt(substrings[0]);
                this.type = Integer.parseInt(substrings[1]);
            }
            else{
                throw new Error("not yet implemented");
            }
            System.out.println("ColumnWithType created!");
        }


        public ColumnWithType(CompoundTableModel model, int column , int type){
            this.mModel = model;
            this.column = column;
            this.type   = type;
        }

        public String getColumnName(){
            return mModel.getColumnTitle(this.column);
        }

        /**
         * Returns the encoded ("col/type") string.
         * @return encoded string
         */
        public String getEncoded(){
            return this.column+"/"+this.type;
        }

        public boolean isChemical(){
            return (this.type == TYPE_CHEM_STRUCTURE) || (this.type == TYPE_CHEM_STRUCTURE);
        }

        @Override
        public String toString(){
            String str = getColumnName();
            if(this.type==TYPE_CHEM_STRUCTURE){ str += "[Structure]"; }
            if(this.type==TYPE_CHEM_REACTION){  str += "[Reaction]"; }
            if(this.type==TYPE_NUMERIC){  str += "[Numeric]"; }
            return str;
//            return "TEST";
        }

    }

}
