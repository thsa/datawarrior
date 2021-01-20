package com.actelion.research.table.view.card.cardsurface.gui;

import com.actelion.research.table.view.card.JCardPane;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.JCardView;
import com.actelion.research.table.view.card.cardsurface.*;
import com.actelion.research.table.view.card.tools.DataWarriorLink;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.awt.datatransfer.DataFlavor.stringFlavor;


/**
 *
 * Internal logic:
 *
 * Initialization: either, we already have CWCC / CWSC objects for configuration, or we create them new.
 *
 * Wwhen the GUI changes (or also when we init the dialog), we use the setFullConfig(..) function, which then calls the
 * createCardConfiguration and createStackConfiguration functions, which translate the CWCC/CWSC objects into actual
 * CardDrawingConfig / StackDrawingConfig objects.
 *
 * After creation of those, we reinit the card / stack drawers.
 *
 *
 *
 * When a .dwar file is loaded anc contains data which describes a cardview, then it also contains a properties object
 * describing the CWCC / CWSC objects.
 *
 *
 *
 */
public class JCardWizard2 extends JComponent {

    /**
     * Layouted panels
     */
    private JPanel mListAvailableColumnsPanel = new JPanel();
    private JPanel mListSelectedColumnsPanel  = new JPanel();

    /**
     * Container for the actual preview panel
     */
    private JPanel mPreviewPanelCard          = new JPanel();

    /**
     * Container for the actual preview panel
     */
    private JPanel mPreviewPanelStack         = new JPanel();



    /**
     * PREVIEW PANEL CARD
     */
    private JCardPreviewPanel mPanelPreviewCard  = null;

    /**
     * PREVIEW PANEL STACK
     */
    private JCardPreviewPanel mPanelPreviewStack = null;




    private JPanel      mOptionsPane          = new JPanel();
    private JTabbedPane mOptionsTabbedPane    = new JTabbedPane();

    private JButton     mButtonApply = null;
    private static final String CMD_APPLYBUTTON = "ApplyButtonPressed";


    private CompoundTableModel mModel = null;
    private CompoundTableColorHandler mColorHandler = null;

    private JCardView mCardView = null;

    private JCardPane mCardPane = null;


    private JList<ColumnWithType> mListAvailableColumns = new JList<>( new DefaultListModel<>() );
    private JList<ColumnWithType> mListColumnsToShow = new JList<>( new DefaultListModel<>() );


    /**
     * Card drawing config of the preview panel Card Drawer
     */
    private CardDrawingConfig mCDC = null;

    /**
     * Stack drawing config of the preview panel Stack Drawer
     */
    private StackDrawingConfig mSDC = null;


    JCardWizard2.CardWizardGlobalConfig mCWGC = null;
    JCardWizard2.CardWizardCardConfig   mCWCC = null;
    JCardWizard2.CardWizardStackConfig  mCWSC = null;


    /**
     *
     * @param dwl
     * @param cardView
     * @param cardPane
     * @param clearCurrentConfig
     * @param config
     */
    //public JCardWizard2(CompoundTableModel model, CompoundTableColorHandler colorHandler, JCardView cardView , JCardPane cardPane , boolean clearCurrentConfig , FullCardWizardConfig config ){
    public JCardWizard2(DataWarriorLink dwl, JCardView cardView , JCardPane cardPane , boolean clearCurrentConfig , FullCardWizardConfig config ){

        this.mModel = dwl.getCTM();
        this.mColorHandler = dwl.getTableColorHandler();

        this.mCardView = cardView;
        this.mCardPane = cardPane;
        this.mCDC      = new CardDrawingConfig( cardPane.getCardDrawer().getCardDrawingConfig() ); //cdc;
        this.mSDC      = new StackDrawingConfig( cardPane.getStackDrawer().getStackDrawingConfig() );//sdc;

        initLayout();

        initAvailableColumnsPanel();
        initSelectedColumnsPanel();

        // ! Create new drawer with its own config:
        // ! Cloning also duplicates (i.e. creates a new) config object
        AbstractCardDrawer cdrawer = mCardPane.getCardDrawer().clone();
        CardDrawingConfig  cdc     = cdrawer.getCardDrawingConfig();
        AbstractStackDrawer sdrawer = mCardPane.getStackDrawer().clone();
        StackDrawingConfig  sdc     = sdrawer.getStackDrawingConfig();

        this.mCDC = cdc;
        this.mSDC = sdc;



        initCardPreviewPanel();

        initOptionsPane();
        revalidate();






        // clear the current config and reinit..
        if(clearCurrentConfig) {
            cdc.getSlots().clear();
            sdc.getSlots().clear();

            FullCardWizardConfig proposedConfig = createDefaultConfig(mModel, mColorHandler);
            this.setFullConfig(proposedConfig);
            mCardPane.getCardDrawer().setCardDrawingConfig(mCDC);
            mCardPane.getStackDrawer().setStackDrawingConfig(mSDC);
        }
        else{
            // overtake config:
            this.setFullConfig(config);
        }
    }





    public void setFullConfig( FullCardWizardConfig fullConfig ) {
        this.mCWGC = fullConfig.mGlobalConfig;
        this.mCWCC = fullConfig.mCardConfig;
        this.mCWSC = fullConfig.mStackConfig;
        this.syncConfigToEverything(true);
        //this.syncConfigToGUI();
    }


    public void initLayout(){

        double layout[][] = new double[][]{ { HiDPIHelper.scale(8) , TableLayout.PREFERRED , HiDPIHelper.scale(8) , TableLayout.PREFERRED , HiDPIHelper.scale(8) , TableLayout.PREFERRED , HiDPIHelper.scale(8) , TableLayout.PREFERRED , HiDPIHelper.scale(8) } ,
                { HiDPIHelper.scale(8) , TableLayout.PREFERRED , HiDPIHelper.scale(8) , TableLayout.PREFERRED , HiDPIHelper.scale(8) , HiDPIHelper.scale(24) , HiDPIHelper.scale(8)  }};

        this.setLayout( new TableLayout( layout ) );

        if(true) {
            if (true) {
                mListAvailableColumnsPanel.setBackground(Color.green);
                mListSelectedColumnsPanel.setBackground(Color.green.darker());
                mPreviewPanelCard.setBackground(Color.blue);
                mPreviewPanelStack.setBackground(Color.blue.darker());
                mOptionsPane.setBackground(Color.orange);
            }
            if (false) {
                mListAvailableColumnsPanel.setPreferredSize(new Dimension(200, 200));
                mOptionsTabbedPane.setPreferredSize(new Dimension(600, 200));
            }
        }


        this.mListAvailableColumns.setModel( new DefaultListModel<ColumnWithType>() );
        this.mListColumnsToShow.setModel( new DefaultListModel<ColumnWithType>() );

        this.mListColumnsToShow.getModel().addListDataListener(  new MyListDataListener() );
        //this.mListColumnsToShow.addList
        this.mListColumnsToShow.setRequestFocusEnabled(true);
        this.mListColumnsToShow.addKeyListener(  new MyDeleteKeyListener() );

        this.add( mListAvailableColumnsPanel , "1 , 1 " );
        this.add( mListSelectedColumnsPanel  , "3 , 1 " );

        this.add( mOptionsTabbedPane , "1,3,3,3" );

        mButtonApply = new JButton("Apply!");
        this.add( mButtonApply , "1,5,3,5" );
        mButtonApply.setActionCommand(CMD_APPLYBUTTON);
        mButtonApply.addActionListener(new MyActionListener() );

        this.add ( mPreviewPanelCard  , "5 , 1 , 5 , 5 ");
        this.add ( mPreviewPanelStack , "7 , 1 , 7 , 5 ");

    }


    /**
     * Collect the chemistry and numeric columns from the compound table
     */
    public void initAvailableColumnsPanel() {

        mListAvailableColumnsPanel.setLayout( new BoxLayout(mListAvailableColumnsPanel,BoxLayout.Y_AXIS) );
        mListAvailableColumnsPanel.add( new JLabel( "AVAILABLE COLUMNS" ) );


        /**
         * Collect all the available data columns..
         */
        ArrayList<ColumnWithType> vAvailableColumns    = new ArrayList<>();

        for(int zi=0;zi<mModel.getTotalColumnCount();zi++){
            if( mModel.isColumnTypeStructure(zi) ){
                vAvailableColumns.add( new ColumnWithType(mModel,  zi , ColumnWithType.TYPE_CHEM_STRUCTURE) );
            }
            if( mModel.isColumnTypeReaction(zi) ){
                vAvailableColumns.add( new ColumnWithType(mModel,  zi , ColumnWithType.TYPE_CHEM_REACTION) );
            }
            if( mModel.isColumnTypeDouble(zi) ){
                vAvailableColumns.add( new ColumnWithType(mModel,  zi , ColumnWithType.TYPE_NUMERIC) );
            }
        }

        vAvailableColumns.sort( ColumnWithType::compareTo );
        vAvailableColumns.stream().forEachOrdered ( cti -> ((DefaultListModel<JCardWizard2.ColumnWithType>) mListAvailableColumns.getModel() ).addElement( cti ) );

        mListAvailableColumns.setTransferHandler( new JCardWizard2.MyTransferHandlerForTypeData() );


        JScrollPane jspA = new JScrollPane(mListAvailableColumns);
        mListAvailableColumnsPanel.add( jspA );

        mListAvailableColumns.setDragEnabled(true);
    }


    public void initSelectedColumnsPanel(){

        mListSelectedColumnsPanel.setLayout(new BoxLayout(mListSelectedColumnsPanel, BoxLayout.Y_AXIS));
        //jspA.setSize(CONF_WIDTH_PANEL_B, 400);

        mListSelectedColumnsPanel.add(new JLabel( "TO SHOW:" ));

        //mListColumnsToShow = new JList();
        mListColumnsToShow.setModel(new DefaultListModel<>());
        // init drag and drop
        mListColumnsToShow.setDropMode(DropMode.USE_SELECTION);
        mListColumnsToShow.setDragEnabled(true);
        mListColumnsToShow.setTransferHandler(new JCardWizard2.MyTransferHandler());
        //mListColumnsToShow.getModel().addListDataListener(new MyListDataListener() );

        JScrollPane jspA = new JScrollPane(mListColumnsToShow);
        mListSelectedColumnsPanel.add(jspA);
    }


    /**
     * Takes the values from the mCWCC and mCWSC and initializes the GUI accordingly.
     *
     * NOTE: this takes the columns from the mCWGC.
     *
     * NOTE! The GUI means the JMyOptionPanels and the List with the column numbers.
     *
     */
    public void syncConfigToGUI(){
        // add columns to mListAvailableColumns:
        DefaultListModel<ColumnWithType> listModel = (DefaultListModel<ColumnWithType>)  this.mListColumnsToShow.getModel() ;
        listModel.removeAllElements();
        for( ColumnWithType cwt : this.mCWGC.getColumns() ){
            listModel.addElement(cwt);
        }
        this.mListColumnsToShow.setModel(listModel);
        //this.mListAvailableColumns.revalidate();

//        // options panes:
//        this.mCardsOptionsPane_DataPanelConfigPanel.synchConfigToGUI();
//        this.mStacksOptionsPane_DataPanelConfigPanel.synchConfigToGUI();
//
//        // reinint drawers(?)
//        this.mPanelPreviewCard.setCardDrawingConfig(this.mCDC);
//        this.mPanelPreviewStack.setStackDrawingConfig(this.mSDC);


        this.invalidate();
    }




    private void initOptionsPane(){
        //mOptionsPane.setLayout(new BorderLayout());
        //mOptionsPane.add( mOptionsTabbedPane , BorderLayout.CENTER );

        initCardsOptionsPane();
        mOptionsTabbedPane.addTab("Cards Options", mCardsOptionsPane);

        initStacksOptionsPane();
        mOptionsTabbedPane.addTab("Stack Options", mStacksOptionsPane);
        //mCardsOptionsPane.validate();

    }

    private JPanel mCardsOptionsPane = new JPanel();
    private JCardConfigPanel mCardsOptionsPane_DataPanelConfigPanel = null;


    // Slider width
    private static final int CONF_WIDTH_SLIDERS = 80;


    private void initCardsOptionsPane(){
        mCardsOptionsPane.setLayout( new TableLayout( new double[][]{{TableLayout.PREFERRED},{TableLayout.PREFERRED}} ) );

        mCardsOptionsPane_DataPanelConfigPanel = new JCardConfigPanel();
        mCardsOptionsPane.add(mCardsOptionsPane_DataPanelConfigPanel, "0 , 0");
    }


    private JPanel mStacksOptionsPane = new JPanel();
    private JStackConfigPanel mStacksOptionsPane_DataPanelConfigPanel = null;

    private void initStacksOptionsPane(){
        mStacksOptionsPane.setLayout( new TableLayout( new double[][]{{TableLayout.PREFERRED},{TableLayout.PREFERRED}} ) );

        mStacksOptionsPane_DataPanelConfigPanel = new JStackConfigPanel();
        mStacksOptionsPane.add(mStacksOptionsPane_DataPanelConfigPanel, "0 , 0");
    }

//    private JSlider mSliderStackSizeStructure = null;
//    private JSlider mSliderStackSizeRxn = null;
//    private JSlider mSliderStackSizeNumerical = null;



    /**
     * ----------- Card Preview Panel --------------------------------------------------------------------------
     *
     * i.e. card and stack preview panels
     *
     */

    public void initCardPreviewPanel(){

        this.mPreviewPanelCard.setLayout(new BoxLayout( mPreviewPanelCard,BoxLayout.Y_AXIS ) );
        this.mPreviewPanelStack.setLayout(new BoxLayout( mPreviewPanelStack,BoxLayout.Y_AXIS ) );

        this.mPreviewPanelCard.removeAll();
        this.mPreviewPanelStack.removeAll();

        // card preview:
        //this.mPanelPreviewCard = new JCardPreviewPanel(mModel, new DefaultCardDrawer(mModel, mColorHandler), this.mCDC , new DefaultStackDrawer(mModel, mColorHandler) , this.mSDC );


        //this.mPanelPreviewCard = new JCardPreviewPanel(mModel, mCardPane );
        this.mPanelPreviewCard = new JCardPreviewPanel(mModel, this.mColorHandler, this.mCDC, this.mSDC );
        this.mPanelPreviewCard.setShowRandomCard();
        mPreviewPanelCard.add(mPanelPreviewCard);
        this.mCDC.registerConfigChangeListener(mPanelPreviewCard);

        // stack preview:
        //this.mPanelPreviewStack = new JCardPreviewPanel(mModel, new DefaultCardDrawer(mModel, mColorHandler), this.mCDC , new DefaultStackDrawer(mModel, mColorHandler) , this.mSDC);
        //this.mPanelPreviewStack = new JCardPreviewPanel(mModel , this.mCardPane );
        this.mPanelPreviewStack = new JCardPreviewPanel(mModel, this.mColorHandler, this.mCDC, this.mSDC );
        this.mPanelPreviewStack.setShowRandomStack(3);
        mPreviewPanelStack.add(mPanelPreviewStack);
        this.mSDC.registerConfigChangeListener(mPanelPreviewStack);



        this.mPreviewPanelCard.setPreferredSize(new Dimension(200,320) );
        this.mPreviewPanelStack.setPreferredSize(new Dimension(200,320) );
//        this.mPreviewPanelCard.setPreferredSize(new Dimension( 400,600 ));
//        this.mPreviewPanelStack.setPreferredSize(new Dimension( 400,600 ));
    }



    public static class ColumnWithType implements Comparable<ColumnWithType>{
        public static final int TYPE_CHEM_STRUCTURE = 1;
        public static final int TYPE_CHEM_REACTION  = 2;
        public static final int TYPE_NUMERIC        = 3;
        public static final int TYPE_TEXT           = 4;

        public int column = -1;
        public int type   = -1;
        private CompoundTableModel mModel = null;


        /**
         * Automatically decodes either "colum" (e.g. "13") or column with type (e.g. "13/1")
         * Types: "1"->structure, 3->"numeric"
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
            //System.out.println("ColumnWithType created!");
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

        public boolean isNumeric() { return (this.type == TYPE_NUMERIC); }

        @Override
        public String toString(){
            String str = getColumnName();
            if(this.type==TYPE_CHEM_STRUCTURE){ str += "[Structure]"; }
            if(this.type==TYPE_CHEM_REACTION){  str += "[Reaction]"; }
            //if(this.type==TYPE_NUMERIC){  str += "[Numeric]"; }
            if(this.type==TYPE_NUMERIC){  str += ""; }
            if(this.type==TYPE_TEXT){  str += "[TEXT]"; }
            return str;
//            return "TEST";
        }

        @Override
        public int compareTo(ColumnWithType o) {

            int comparisonType = Integer.compare(this.type,o.type);

            if(comparisonType!=0){
                return comparisonType;
            }
            int comparisonCol = Integer.compare(this.column,o.column);

            return comparisonCol;
        }
    }


    // Transfer Handler for JLists to export type together with column number.. :
    public static class MyTransferHandlerForTypeData extends TransferHandler{
        //int mType = -1;

        public MyTransferHandlerForTypeData(){}

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
            ColumnWithType cwt = (ColumnWithType) ( myList.getModel().getElementAt( myList.getSelectedIndex() ) );
            return new java.awt.datatransfer.StringSelection( cwt.column + "/" + cwt.type );
        }
    }

//    private static JPanel getLeftAlignedHorizontalBoxLayoutPanel(){
//        JPanel p = new JPanel();
//        p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));
//        p.setAlignmentX(Component.LEFT_ALIGNMENT);
//        p.setAlignmentY(Component.TOP_ALIGNMENT);
//        return p;
//    }
//    private static JPanel getTopAlignedVerticalBoxLayoutPanel(){
//        JPanel p = new JPanel();
//        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
//        p.setAlignmentX(Component.LEFT_ALIGNMENT);
//        p.setAlignmentY(Component.TOP_ALIGNMENT);
//        return p;
//    }



    // Transfer Handler for JList with columsn to show:
    public class MyTransferHandler extends TransferHandler{

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

            return new java.awt.datatransfer.StringSelection( ((JCardWizard2.ColumnWithType) myList.getModel().getElementAt( this.mIndexDraggedInternally)).getEncoded() ) ;

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
                JCardWizard2.ColumnWithType data =  new JCardWizard2.ColumnWithType( mModel, (String) t.getTransferData(stringFlavor) );

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

                // sort types..
                boolean sortTypes = true;
                if(sortTypes){
                    List<ColumnWithType> typeA = new ArrayList<>();
                    List<ColumnWithType> typeB = new ArrayList<>();
                    List<ColumnWithType> typeC = new ArrayList<>();
                    List<ColumnWithType> typeD = new ArrayList<>();
                    for(int zi=0;zi<listModel.getSize();zi++){
                        switch(((ColumnWithType) listModel.get(zi) ).type){
                            case ColumnWithType.TYPE_CHEM_STRUCTURE: { typeA.add( (ColumnWithType) listModel.get(zi)); } break;
                            case ColumnWithType.TYPE_CHEM_REACTION: {  typeB.add( (ColumnWithType) listModel.get(zi)); } break;
                            case ColumnWithType.TYPE_NUMERIC: {  typeC.add( (ColumnWithType) listModel.get(zi)); } break;
                            case ColumnWithType.TYPE_TEXT: {  typeD.add( (ColumnWithType) listModel.get(zi)); } break;
                        }
                    }
                    listModel.removeAllElements();
                    typeD.stream().forEach( cwti -> listModel.addElement(cwti) );
                    typeA.stream().forEach( cwti -> listModel.addElement(cwti) );
                    typeB.stream().forEach( cwti -> listModel.addElement(cwti) );
                    typeC.stream().forEach( cwti -> listModel.addElement(cwti) );
                }
            }
            catch(Exception e){
                System.out.println("something went wrong: "+e.toString());
            }

            // sync GUI to config.
            // for some reason the ListModelEvents dont fire, not matter what I do?!..
            updateGlobalConfig();

            return true;
        }
    }



    class MyChangeListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            if( e.getSource() instanceof JSlider ){
                JSlider es = (JSlider) e.getSource();
                if(! es.getValueIsAdjusting() ){
                    //reininit card..
                    //syncConfigToEverything();
                }
            }
            //if( e.getSource() )
        }
    }

    class MyActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if(e.getActionCommand().equals( CMD_APPLYBUTTON )){
                // apply changes..
//                mCardPane.getCardDrawer().setCardDrawingConfig(mCDC);
//                mCardPane.getStackDrawer().setStackDrawingConfig(mSDC);

                syncConfigToEverything(true);

                mCDC.fireConfigChangeEvent();
                mSDC.fireConfigChangeEvent();
            }
        }
    }

    private void updateGlobalConfig() {
        // update the global config!
        List<JCardWizard2.ColumnWithType> listColumns = new ArrayList<>();
        for(int zi=0; zi<mListColumnsToShow.getModel().getSize(); zi++){
            listColumns.add( mListColumnsToShow.getModel().getElementAt(zi) );
        }
        mCWGC = new CardWizardGlobalConfig(listColumns);

        invalidate();
        syncConfigToEverything(false);
    }

    class MyListDataListener implements ListDataListener {
        @Override
        public void intervalAdded(ListDataEvent e) {
            updateGlobalConfig();
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            updateGlobalConfig();
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            updateGlobalConfig();
        }



    }

    class MyDeleteKeyListener implements KeyListener {
        @Override
        public void keyTyped(KeyEvent e) {

        }

        @Override
        public void keyPressed(KeyEvent e) {
            if(e.getKeyCode() == KeyEvent.VK_DELETE){
                ((DefaultListModel<ColumnWithType>) mListColumnsToShow.getModel() ).remove(  mListColumnsToShow.getSelectedIndex() );
                updateGlobalConfig();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {

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



    public static final String PROPERTYNAME_CARD_COLUMNS = "CardConfigColumns";
    public static final String PROPERTYNAME_STACK_COLUMNS = "CardConfigColumns";


    public static final String PROPERTYNAME_CARD_SIZE_CHEMSTRUCTURE  = "CardSizeChemStructures";
    public static final String PROPERTYNAME_CARD_SIZE_CHEMREACTION   = "CardSizeChemReactions";
    public static final String PROPERTYNAME_CARD_SIZE_NUMERIC        = "CardSizeChemNumeric";

    public static final String PROPERTYNAME_CARD_DATAPANEL_DIV_A     = "CardDatapanelDivA";
    public static final String PROPERTYNAME_CARD_DATAPANEL_DIV_B     = "CardDatapanelDivB";
    public static final String PROPERTYNAME_CARD_DATAPANEL_MODE      = "CardDatapanelMode";

    public static final String PROPERTYNAME_STACK_SIZE_CHEMSTRUCTURE  = "StackSizeChemStructures";
    public static final String PROPERTYNAME_STACK_SIZE_CHEMREACTION   = "StackSizeChemReactions";
    public static final String PROPERTYNAME_STACK_SIZE_NUMERIC        = "StackSizeChemNumeric";

    public static final String PROPERTYNAME_STACK_DATAPANEL_DIV_A     = "StackDatapanelDivA";
    public static final String PROPERTYNAME_STACK_DATAPANEL_DIV_B     = "StackDatapanelDivB";
    public static final String PROPERTYNAME_STACK_DATAPANEL_MODE      = "StackDatapanelMode";



    public JCardWizard2 getThis() { return this; }


    class JCardConfigPanel extends JPanel implements JMyOptionsTable.OptionTableListener {

        JMyOptionsTable mDataPanelOptionsTable;

        public JCardConfigPanel(){
            this.init();
            this.setLayout(new BorderLayout());
            this.add(mDataPanelOptionsTable,BorderLayout.CENTER);
            this.revalidate();
        }

        public void init(){
            //this.mDataPanelOptionsTable = new JMyOptionsTable( 2 , 3 );
            this.mDataPanelOptionsTable = new JMyOptionsTable();

            this.mDataPanelOptionsTable.setSlider(0,0, PROPERTYNAME_CARD_SIZE_CHEMSTRUCTURE , "Size Structure", 0,1 , 0.5);
            this.mDataPanelOptionsTable.setSlider(0,1, PROPERTYNAME_CARD_SIZE_CHEMREACTION  , "Size Reaction", 0,1 , 0.5);
            this.mDataPanelOptionsTable.setSlider(0,2, PROPERTYNAME_CARD_SIZE_NUMERIC       , "Size Numeric", 0,1 , 0.5);


            this.mDataPanelOptionsTable.setSlider(1,0, PROPERTYNAME_CARD_DATAPANEL_DIV_A, "Div. A", 0, 1 , 0.75);
            this.mDataPanelOptionsTable.setSlider(1,1, PROPERTYNAME_CARD_DATAPANEL_DIV_B, "Div. B", 0, 1 , 0.0);
            //List<String> dataPanelModes = new ArrayList<>(); dataPanelModes.add( "Name+Value" ); dataPanelModes.add( "Name+Bar" ); dataPanelModes.add( "Name+Value+Bar" ); dataPanelModes.add( "Bar" );
            List<String> dataPanelModes = new ArrayList<>();
            dataPanelModes.add(CardWizardCardConfig.MODE_DATA_SHOW[0]); dataPanelModes.add(CardWizardCardConfig.MODE_DATA_SHOW[1]); dataPanelModes.add(CardWizardCardConfig.MODE_DATA_SHOW[2]); dataPanelModes.add(CardWizardCardConfig.MODE_DATA_SHOW[3]);
            this.mDataPanelOptionsTable.setComboBox(1,2, PROPERTYNAME_CARD_DATAPANEL_MODE,"DataPanelMode",dataPanelModes,"Name+Value");

            this.mDataPanelOptionsTable.reinit();
            this.mDataPanelOptionsTable.registerOptionTableListener(this );
        }

        @Override
        public void optionTableEventHappened(JMyOptionsTable.OptionTableEvent event) {
            if(event.isAdjusting()){return;}

            //mDataPanelOptionsTable.collectValues() ;
            synchGUItoConfig();
            setFullConfig( new FullCardWizardConfig( mCWGC,mCWCC, mCWSC) );
        }

        /**
         * Inits the gui with the current config values
         */
        public void synchConfigToGUI(){

            this.mDataPanelOptionsTable.setValue(PROPERTYNAME_CARD_SIZE_CHEMSTRUCTURE , mCWCC.mSizeChemistryStructure );
            this.mDataPanelOptionsTable.setValue(PROPERTYNAME_CARD_SIZE_CHEMREACTION  , mCWCC.mSizeChemistryRxn );
            this.mDataPanelOptionsTable.setValue(PROPERTYNAME_CARD_SIZE_NUMERIC       , mCWCC.mSizeNumeric );

            this.mDataPanelOptionsTable.setValue(PROPERTYNAME_CARD_DATAPANEL_DIV_A    , mCWCC.mDivA );
            this.mDataPanelOptionsTable.setValue(PROPERTYNAME_CARD_DATAPANEL_DIV_B    , mCWCC.mDivB );
            this.mDataPanelOptionsTable.setValue(PROPERTYNAME_CARD_DATAPANEL_MODE     , mCWCC.mMode );

            // and init the preview panels:
            createCardConfiguration( mCWGC , mCWCC );
            createStackConfiguration( mCWGC , mCWSC );

            mPanelPreviewCard.setCardDrawingConfig(mCDC);
            mPanelPreviewStack.setStackDrawingConfig(mSDC);
        }

        /**
         * Initializes the CardWizard2.mCWCC according to the GUI settings, i.e. it:
         *
         * 1. Collects selected columns from the mDataPanelOptionsTable (i.e. columns and rel. panel sizes)
         * 2. Creates the CardWizardStackConfig object, based on the columns, relative sizes, and division config.
         * 3. sets JCardWizard2.mCWCC to the created CardWizardStackConfig object.
         *
         */
        public void synchGUItoConfig(){

//            List<JCardWizard2.ColumnWithType> listColumns = new ArrayList<>();
//            for(int zi=0; zi<mListColumnsToShow.getModel().getSize(); zi++){
//                listColumns.add( mListColumnsToShow.getModel().getElementAt(zi) );
//            }

            // CARD CONFIG..
            Properties p = this.mDataPanelOptionsTable.collectValues();
            double cCS = Double.parseDouble( p.getProperty( PROPERTYNAME_CARD_SIZE_CHEMSTRUCTURE ) );
            double cCR = Double.parseDouble( p.getProperty( PROPERTYNAME_CARD_SIZE_CHEMREACTION ) );
            double cNu = Double.parseDouble( p.getProperty( PROPERTYNAME_CARD_SIZE_NUMERIC ) );

            double cDivA = Double.parseDouble( p.getProperty( PROPERTYNAME_CARD_DATAPANEL_DIV_A ) );
            double cDivB = Double.parseDouble( p.getProperty( PROPERTYNAME_CARD_DATAPANEL_DIV_B ) );
            String cMode = p.getProperty( PROPERTYNAME_CARD_DATAPANEL_MODE );

            //JCardWizard2.CardWizardCardConfig cwcc = new JCardWizard2.CardWizardCardConfig( listColumns , cCS , cCR , cNu , cDivA,cDivB,cMode);
            JCardWizard2.CardWizardCardConfig cwcc = new JCardWizard2.CardWizardCardConfig( cCS , cCR , cNu , cDivA,cDivB,cMode);
            mCWCC = cwcc;

            // now create actual card configuration:
            createCardConfiguration( mCWGC , mCWCC );

            //mPanelPreviewCard.setCardDrawingConfig(mCDC);
            //mPanelPreviewStack.setStackDrawingConfig(mSDC);


        }
    }



    class JStackConfigPanel extends JPanel implements JMyOptionsTable.OptionTableListener {

        JMyOptionsTable mDataPanelOptionsTable;

        public JStackConfigPanel() {
            this.init();
            this.setLayout(new BorderLayout());
            this.add(mDataPanelOptionsTable, BorderLayout.CENTER);
            this.revalidate();
        }

        public void init() {
            //this.mDataPanelOptionsTable = new JMyOptionsTable(2, 3);
            this.mDataPanelOptionsTable = new JMyOptionsTable();

            this.mDataPanelOptionsTable.setSlider(0, 0, PROPERTYNAME_STACK_SIZE_CHEMSTRUCTURE, "Size Structure", 0, 1, 0.5);
            this.mDataPanelOptionsTable.setSlider(0, 1, PROPERTYNAME_STACK_SIZE_CHEMREACTION, "Size Reaction", 0, 1, 0.5);
            this.mDataPanelOptionsTable.setSlider(0, 2, PROPERTYNAME_STACK_SIZE_NUMERIC, "Size Numeric", 0, 1, 0.5);


            this.mDataPanelOptionsTable.setSlider(1, 0, PROPERTYNAME_STACK_DATAPANEL_DIV_A, "Div. A", 0, 1, 0.75);
            this.mDataPanelOptionsTable.setSlider(1, 1, PROPERTYNAME_STACK_DATAPANEL_DIV_B, "Div. B", 0, 1, 0.25);
//            List<String> dataPanelModes = new ArrayList<>();
//            dataPanelModes.add("Name+Value");
//            dataPanelModes.add("Name+Bar");
//            dataPanelModes.add("Name+Value+Bar");
//            this.mDataPanelOptionsTable.setComboBox(1, 2, PROPERTYNAME_CARD_DATAPANEL_MODE, "DataPanelMode", dataPanelModes, "Name+Value");
            this.mDataPanelOptionsTable.reinit();
            this.mDataPanelOptionsTable.registerOptionTableListener(this);
        }

        @Override
        public void optionTableEventHappened(JMyOptionsTable.OptionTableEvent event) {
            if(event.isAdjusting()){return;}

            //mDataPanelOptionsTable.collectValues() ;
            synchGUItoConfig();
            setFullConfig( new FullCardWizardConfig( mCWGC ,mCWCC, mCWSC) );
        }

        /**
         *
         * Inits the JCardWizard2.mCWSC according to the GUI settings, i.e. it:
         *
         * 1. Collects selected columns from the mDataPanelOptionsTable (i.e. columns and rel. panel sizes)
         * 2. Sorts out the non-stack-relevant columns (text..)
         * 3. Creates the CardWizardStackConfig object, based on the columns, relative sizes, and division config.
         * 4. sets JCardWizard2.mCWSC to the created CardWizardStackConfig object.
         *
         */
        public void synchGUItoConfig() {

            Properties p = this.mDataPanelOptionsTable.collectValues();

//            List<JCardWizard2.ColumnWithType> listColumns = new ArrayList<>();
//            for(int zi=0; zi<mListColumnsToShow.getModel().getSize(); zi++){
//                // check if we have to sort out the column, e.g. for text
//                if( mListColumnsToShow.getModel().getElementAt(zi).type==ColumnWithType.TYPE_TEXT ){
//                    continue;
//                }
//                listColumns.add( mListColumnsToShow.getModel().getElementAt(zi) );
//            }

            // STACK CONFIG..
            double ssCS = Double.parseDouble( p.getProperty( PROPERTYNAME_STACK_SIZE_CHEMSTRUCTURE ) );
            double ssCR = Double.parseDouble( p.getProperty( PROPERTYNAME_STACK_SIZE_CHEMREACTION ) );
            double ssNu = Double.parseDouble( p.getProperty( PROPERTYNAME_STACK_SIZE_NUMERIC ) );

            double ssDivA = Double.parseDouble( p.getProperty(PROPERTYNAME_STACK_DATAPANEL_DIV_A) );
            double ssDivB = Double.parseDouble( p.getProperty(PROPERTYNAME_STACK_DATAPANEL_DIV_B) );

            //JCardWizard2.CardWizardStackConfig cwsc = new JCardWizard2.CardWizardStackConfig( listColumns , ssCS , ssCR , ssNu, ssDivA , ssDivB );
            JCardWizard2.CardWizardStackConfig cwsc = new JCardWizard2.CardWizardStackConfig( ssCS , ssCR , ssNu, ssDivA , ssDivB );
            mCWSC = cwsc;

            createStackConfiguration(mCWGC ,mCWSC);
        }
        /**
         * Inits the gui with the current config values
         */
        public void synchConfigToGUI(){
            mDataPanelOptionsTable.setValue(PROPERTYNAME_STACK_SIZE_CHEMSTRUCTURE , mCWSC.mSizeChemistryStructure );
            mDataPanelOptionsTable.setValue(PROPERTYNAME_STACK_SIZE_CHEMREACTION  , mCWSC.mSizeChemistryRxn );
            mDataPanelOptionsTable.setValue(PROPERTYNAME_STACK_SIZE_NUMERIC       , mCWSC.mSizeNumeric );
        }

    }






    public static class CardWizardGlobalConfig {
        private List<JCardWizard2.ColumnWithType> mColumns;

        public CardWizardGlobalConfig(List<JCardWizard2.ColumnWithType> cols){
            this.mColumns = cols;
        }

        public List<JCardWizard2.ColumnWithType> getColumns() {
            return mColumns;
        }
        public void setColumns(List<JCardWizard2.ColumnWithType> mColumns) {
            this.mColumns = mColumns;
        }
    }


    public static class CardWizardCardConfig {

        //public final static String MODE_NUMERIC_LABEL_AND_VALUE = "Label+Value";

        public final static String MODE_DATA_SHOW[] = new String[] { "Label+Value" , "Label+Value+Bar" , "Label+Bar" , "Bar" };
        public final static Map<String,SingleDataPanel.DataShowMode> MAP_DATA_SHOW_MODES;

        static {
            MAP_DATA_SHOW_MODES = new HashMap<>();
            MAP_DATA_SHOW_MODES.put(MODE_DATA_SHOW[0], SingleDataPanel.DataShowMode.NameAndValue);
            MAP_DATA_SHOW_MODES.put(MODE_DATA_SHOW[1], SingleDataPanel.DataShowMode.NameAndValueAndBar);
            MAP_DATA_SHOW_MODES.put(MODE_DATA_SHOW[2], SingleDataPanel.DataShowMode.NameAndBar);
            MAP_DATA_SHOW_MODES.put(MODE_DATA_SHOW[3], SingleDataPanel.DataShowMode.Bar);
        }


//        public List<JCardWizard2.ColumnWithType> getColumns() {
//            return mColumns;
//        }
//
//        public void setColumns(List<JCardWizard2.ColumnWithType> mColumns) {
//            this.mColumns = mColumns;
//        }

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

        //private List<JCardWizard2.ColumnWithType> mColumns;

        private double mSizeChemistryStructure;
        private double mSizeChemistryRxn;
        private double mSizeNumeric;

        private double mDivA;
        private double mDivB;
        private String mMode;

        public double getDivA(){ return mDivA;}
        public double getDivB(){ return mDivB;}
        public String getMode(){ return mMode;}


        //public CardWizardCardConfig(List<JCardWizard2.ColumnWithType> columns , double sizeChemistryStructure , double sizeChemistryRxn, double sizeNumeric , double divA , double divB , String mode){
        public CardWizardCardConfig( double sizeChemistryStructure , double sizeChemistryRxn, double sizeNumeric , double divA , double divB , String mode){
            //this.mColumns = columns;

            double totalSize = sizeChemistryStructure + sizeChemistryRxn + sizeNumeric;

            this.mSizeChemistryStructure = sizeChemistryStructure / totalSize;
            this.mSizeChemistryRxn       = sizeChemistryRxn / totalSize;
            this.mSizeNumeric            = sizeNumeric / totalSize;

            this.mDivA = divA;
            this.mDivB = divB;
            this.mMode = mode;
        }
    }


    public static class CardWizardStackConfig{

//        public List<JCardWizard2.ColumnWithType> getColumns(){ return mColumns; }
//
//        public void setColumns(List<JCardWizard2.ColumnWithType> mColumns) {
//            this.mColumns = mColumns;
//        }

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

        private List<JCardWizard2.ColumnWithType> mColumns;

        private double mSizeChemistryStructure;
        private double mSizeChemistryRxn;
        private double mSizeNumeric;

        private double mDivA;
        private double mDivB;
        public double getDivA(){ return mDivA;}
        public double getDivB(){ return mDivB;}


//        public CardWizardStackConfig(List<JCardWizard2.ColumnWithType> columns , double sizeChemistryStructure , double sizeChemistryRxn, double sizeNumeric , double divA , double divB ){
//            this.mColumns = columns;
        public CardWizardStackConfig( double sizeChemistryStructure , double sizeChemistryRxn, double sizeNumeric , double divA , double divB ){

            double totalSize = sizeChemistryStructure + sizeChemistryRxn + sizeNumeric;

            this.mSizeChemistryStructure = sizeChemistryStructure / totalSize;
            this.mSizeChemistryRxn       = sizeChemistryRxn / totalSize;
            this.mSizeNumeric            = sizeNumeric / totalSize;
            this.mDivA = divA;
            this.mDivB = divB;

        }
    }



    /**
     *
     * Uses the current config and sets if as current config in the CardView, and sets the corresponding config in
     * the JCardPane.
     *
     * @param overwriteCardViewConfig  : should the CardViewConfig (i.e. the one which indicates the currently active
     *                                   on be overwritten?)
     */
    public void syncConfigToEverything(boolean overwriteCardViewConfig){
        this.syncConfigToGUI();
        this.createCardConfiguration(this.mCWGC,this.mCWCC);
        this.createStackConfiguration(this.mCWGC,this.mCWSC);

        this.setCardDrawingConfig(mCDC);
        this.setStackDrawingConfig(mSDC);

        if(overwriteCardViewConfig){
            this.mCardPane.getCardDrawer().setCardDrawingConfig(this.mCDC);
            this.mCardPane.getStackDrawer().setStackDrawingConfig(this.mSDC);

            //this.mCardView.setCardWizardConfig(new FullCardWizardConfig(this.mCWGC,mCWCC,mCWSC));
            // @TODO probably fire event / somehow make the card pane redraw / update the cards..
        }
    }


    /**
     * Creates the actual card config from the wizard configuration.
     *
     * @param config
     */
    public void createCardConfiguration( JCardWizard2.CardWizardGlobalConfig gconf , JCardWizard2.CardWizardCardConfig config )
    {

        //  1. count the number of columns / types that we have:
        List<JCardWizard2.ColumnWithType> listCS = gconf.getColumns().stream().filter(ci -> ci.type == JCardWizard2.ColumnWithType.TYPE_CHEM_STRUCTURE ).collect(Collectors.toList());
        List<JCardWizard2.ColumnWithType> listCR = gconf.getColumns().stream().filter(ci -> ci.type == JCardWizard2.ColumnWithType.TYPE_CHEM_REACTION ).collect(Collectors.toList());
        List<JCardWizard2.ColumnWithType> listNu = gconf.getColumns().stream().filter(ci -> ci.type == JCardWizard2.ColumnWithType.TYPE_NUMERIC ).collect(Collectors.toList());
        //List<JCardWizard2.ColumnWithType> listNu = config.getColumns().stream().filter(ci -> ci.type == JCardWizard2.ColumnWithType.TYPE_NUMERIC || ci.type == JCardWizard2.ColumnWithType.TYPE_TEXT ).collect(Collectors.toList());
        List<JCardWizard2.ColumnWithType> listTx = gconf.getColumns().stream().filter(ci -> ci.type == JCardWizard2.ColumnWithType.TYPE_TEXT ).collect(Collectors.toList());


        int total = gconf.getColumns().size();
        int nCS   = listCS.size();
        int nCR   = listCR.size();
        int nNu   = listNu.size();
        int nTx   = listTx.size();

        // magic formula to compute percentage for different parts:
        double relCS = config.mSizeChemistryStructure;
        double relCR = config.mSizeChemistryRxn;
        double relNu = config.mSizeNumeric;  // scales also the txt line(s) on top..

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
        double relPerNu = preNu / (nNu + nTx);

        // put thems panels
        // We just use the order chem stuff / then all numeric stuff.. this is for simplicity, could be extended to mix stuff, but this requires quite some additional code..
        CardDrawingConfig cdc = new CardDrawingConfig(mColorHandler);
        // transfer all config change listeners:
        this.mCDC.getConfigChangeListeners().forEach( ccl -> cdc.registerConfigChangeListener(ccl) );

        List<JCardWizard2.ColumnWithType> txtColumns       = new ArrayList<>();
        List<JCardWizard2.ColumnWithType> chemicalColumns  = new ArrayList<>();
        List<JCardWizard2.ColumnWithType> numericalColumns = new ArrayList<>();

        for(JCardWizard2.ColumnWithType cwt : gconf.getColumns()){
            if( cwt.isChemical() ){
                chemicalColumns.add(cwt);
            }
            else{
                if( cwt.type == ColumnWithType.TYPE_NUMERIC) {
                    numericalColumns.add(cwt);
                }
                if( cwt.type == ColumnWithType.TYPE_TEXT){
                    txtColumns.add(cwt);
                }
            }
        }

        // add themz:
        double currentPosition = 0.0;

        for ( JCardWizard2.ColumnWithType cc : txtColumns ) {
            SingleDataPanel txP = new SingleDataPanel();
            txP.setColumns(new int[]{cc.column});

            cdc.getSlots().add( new CardDrawingConfig.CardSurfaceSlot( cdc , txP, currentPosition , 1 * relPerNu ) );
            currentPosition += relPerNu;
        }

        for( JCardWizard2.ColumnWithType cc : chemicalColumns ){
            if(cc.type== JCardWizard2.ColumnWithType.TYPE_CHEM_STRUCTURE) {
                StructurePanel sp = new StructurePanel();
                sp.setColumns(new int[]{cc.column});
                cdc.getSlots().add(new CardDrawingConfig.CardSurfaceSlot(cdc, sp, currentPosition, relPerCS));
                currentPosition += relPerCS;
            }
            if(cc.type== JCardWizard2.ColumnWithType.TYPE_CHEM_REACTION) {
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
        mdp.setDivA( config.mDivA ); mdp.setDivB( config.mDivB );

        mdp.setDataShowMode( CardWizardCardConfig.MAP_DATA_SHOW_MODES.get( config.mMode ) );


        cdc.getSlots().add( new CardDrawingConfig.CardSurfaceSlot( cdc , mdp, currentPosition , numericalColumns.size() * relPerNu ) );
        cdc.registerConfigChangeListener(this.mPanelPreviewCard);
        cdc.registerConfigChangeListener(this.mPanelPreviewStack);

        this.setCardDrawingConfig(cdc);
    }




    public void createStackConfiguration( JCardWizard2.CardWizardGlobalConfig gconf , JCardWizard2.CardWizardStackConfig config ){
        //  1. count the number of columns / types that we have:
        List<JCardWizard2.ColumnWithType> listCS = gconf.getColumns().stream().filter(ci -> ci.type == JCardWizard2.ColumnWithType.TYPE_CHEM_STRUCTURE ).collect(Collectors.toList());
        List<JCardWizard2.ColumnWithType> listCR = gconf.getColumns().stream().filter(ci -> ci.type == JCardWizard2.ColumnWithType.TYPE_CHEM_REACTION ).collect(Collectors.toList());
        List<JCardWizard2.ColumnWithType> listNu = gconf.getColumns().stream().filter(ci -> ci.type == JCardWizard2.ColumnWithType.TYPE_NUMERIC ).collect(Collectors.toList());

        int total = gconf.getColumns().size();
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

        // reserve space for Stack
        double confRelStackSizeIndicator = 0.1;
        preCS = preCS * (1-confRelStackSizeIndicator);
        preCR = preCR * (1-confRelStackSizeIndicator);
        preNu = preNu * (1-confRelStackSizeIndicator);


        double relPerCS = preCS / nCS;
        double relPerCR = preCR / nCR;
        double relPerNu = preNu / nNu;

        // put thems panels
        // We just use the order chem stuff / then all numeric stuff.. this is for simplicity, could be extended to mix stuff, but this requires quite some additional code..
        StackDrawingConfig sdc = new StackDrawingConfig(mColorHandler);
        // transfer all config change listeners:
        this.mSDC.getConfigChangeListeners().forEach( ccl -> sdc.registerConfigChangeListener(ccl) );

        List<JCardWizard2.ColumnWithType> chemicalColumns  = new ArrayList<>();
        List<JCardWizard2.ColumnWithType> numericalColumns = new ArrayList<>();

        for(JCardWizard2.ColumnWithType cwt : gconf.getColumns()){
            if( cwt.isChemical() ){
                chemicalColumns.add(cwt);
            }
            else{
                if( cwt.isNumeric()) {
                    numericalColumns.add(cwt);
                }
            }
        }

        // add themz:
        double currentPosition = 0.0;

        NumCardsIndicatorPanel ssip = new NumCardsIndicatorPanel();
        sdc.getSlots().add(new StackDrawingConfig.StackSurfaceSlot(ssip,currentPosition,confRelStackSizeIndicator));
        currentPosition += confRelStackSizeIndicator;

        for( JCardWizard2.ColumnWithType cc : chemicalColumns ){
            if(cc.type== JCardWizard2.ColumnWithType.TYPE_CHEM_STRUCTURE) {
                StackChemStructurePanel sp = new StackChemStructurePanel();
                sp.setColumns(new int[]{cc.column});
                sdc.getSlots().add(new StackDrawingConfig.StackSurfaceSlot( sp, currentPosition, relPerCS));
                currentPosition += relPerCS;
            }
            if(cc.type== JCardWizard2.ColumnWithType.TYPE_CHEM_REACTION) {
                // @TODO add reaction panel (i.e. figure out what has to be changed..)
                StackChemStructurePanel sp = new StackChemStructurePanel();
                sp.setColumns(new int[]{cc.column});
                sdc.getSlots().add(new StackDrawingConfig.StackSurfaceSlot( sp,  currentPosition, relPerCR));
                currentPosition += relPerCR;
            }
        }

        // add multipanel for numerical values below:
        StackMultiDataPanel mdp = new StackMultiDataPanel();

        mdp.setDivA(config.getDivA()); mdp.setDivB(config.getDivB());
        mdp.setNumberOfColumns(numericalColumns.size());
        mdp.setColumns( numericalColumns.stream().mapToInt(  nci -> nci.column).toArray() );


        sdc.getSlots().add( new StackDrawingConfig.StackSurfaceSlot( mdp, currentPosition , numericalColumns.size() * relPerNu ) );
        sdc.registerConfigChangeListener(this.mPanelPreviewCard);
        sdc.registerConfigChangeListener(this.mPanelPreviewStack);

        this.setStackDrawingConfig(sdc);
    }


    public void setCardDrawingConfig(CardDrawingConfig cdc){
        // transfer all listeners to new cdc..
        CardDrawingConfig oldCDC = this.mCDC;
        this.mCDC = cdc;

        for( CardDrawingConfig.CardDrawingConfigChangeListener cdccl : oldCDC.getConfigChangeListeners() ){
            this.mCDC.registerConfigChangeListener( cdccl );
        }

        this.mPanelPreviewCard.setCardDrawingConfig(this.mCDC);
        // redraw..
        this.initCardPreviewPanel();
        this.mCDC.fireConfigChangeEvent();
        this.revalidate();
    }

    public void setStackDrawingConfig(StackDrawingConfig sdc){
        // transfer all listeners to the new scd..
        StackDrawingConfig oldSDC = this.mSDC;
        this.mSDC = sdc;
        for( StackDrawingConfig.StackDrawingConfigChangeListener sdccl : oldSDC.getConfigChangeListeners() ){
            this.mSDC.registerConfigChangeListener( sdccl );
        }

        this.mPanelPreviewStack.setStackDrawingConfig(this.mSDC);
        // redraw..
        this.initCardPreviewPanel();
        this.mSDC.fireConfigChangeEvent();
        this.revalidate();
    }




    //public static Pattern[] mConfInit_Regexps_HighestPriority        = new Pattern[]{ Pattern.compile("Idorsia No") };
    public static Pattern[] mConfInit_Regexps_HighestPriority        = new Pattern[]{ };
    public static Pattern[] mConfInit_Regexps_SecondHighestPriority  = new Pattern[]{ Pattern.compile(".*IC 50.*") };


    static{

    }

    /**
     * We use this function to initialize the card layout.
     * It picks:
     * 1. first chemical structure, and then:
     * 2. string / data columns which match regexps in mConfInit_Regexps_HighestPriority
     * 3. data columns which match regexps in mConfInit_Regexps_SecondHighestPriority
     * 4. if less than six columns, then additional columns, (@TODO: starting from the left side of the table..)
     *
     * @param ctm
     * @param colorHandler
     * @return
     */
    public FullCardWizardConfig createDefaultConfig(CompoundTableModel ctm, CompoundTableColorHandler colorHandler){

        List<Integer> idxStructures = new ArrayList<>();
        List<Integer> idxText       = new ArrayList<>();
        List<Integer> idxNumeric    = new ArrayList<>();

        for(int zi=0;zi<ctm.getColumnCount();zi++){
            if(ctm.isColumnTypeStructure(zi)){ idxStructures.add(zi); }
            if(ctm.isColumnTypeDouble(zi)){ idxNumeric.add(zi); }
            if(ctm.isColumnTypeString(zi)){ idxText.add(zi); }
        }


        List<ColumnWithType> columns    = new ArrayList<>();

        if(!idxStructures.isEmpty()){
            columns.add( new ColumnWithType(ctm, idxStructures.get(0) , ColumnWithType.TYPE_CHEM_STRUCTURE ) );
        }

        for(int zi1=0;zi1<idxText.size();zi1++){
            int zi = zi1;
            if( Arrays.stream( mConfInit_Regexps_HighestPriority ).filter( ri -> ri.matcher( ctm.getColumnTitle(idxText.get(zi)) ).find() ).count() > 0){
                columns.add( new ColumnWithType(ctm, idxText.get(zi) , ColumnWithType.TYPE_TEXT ) );
            }
            else if( Arrays.stream( mConfInit_Regexps_SecondHighestPriority ).filter( ri -> ri.matcher( ctm.getColumnTitle(idxText.get(zi)) ).find() ).count() > 0 ){
                columns.add( new ColumnWithType(ctm, idxText.get(zi) , ColumnWithType.TYPE_TEXT ) );
            }
        }

        for(int zi1=0;zi1<idxNumeric.size();zi1++){
            int zi = zi1;
            if( Arrays.stream( mConfInit_Regexps_HighestPriority ).filter( ri -> ri.matcher( ctm.getColumnTitle(idxNumeric.get(zi)) ).find() ).count() > 0){
                columns.add( new ColumnWithType(ctm, idxText.get(zi) , ColumnWithType.TYPE_NUMERIC ) );
            }
            else if( Arrays.stream( mConfInit_Regexps_SecondHighestPriority ).filter( ri -> ri.matcher( ctm.getColumnTitle(idxNumeric.get(zi)) ).find() ).count() > 0 ){
                columns.add( new ColumnWithType(ctm, idxText.get(zi) , ColumnWithType.TYPE_NUMERIC ) );
            }
        }

        Set<Integer> column_indeces = columns.stream().mapToInt(ci -> ci.column ).boxed().collect(Collectors.toSet());

        List<Integer> remaniningNumericColumns = idxNumeric.stream().filter( idi -> ! columns.contains(idi) ).collect(Collectors.toList());

        for(int zi=0;zi<Math.min( 6-remaniningNumericColumns.size() , remaniningNumericColumns.size());zi++){
            columns.add( new ColumnWithType(ctm, remaniningNumericColumns.get(zi) , ColumnWithType.TYPE_NUMERIC ) );
        }

        // @TODO: would be maybe nice to sort columns according to priority etc.. (implement via extending ColumnWithType, i.e. put priority in there)

        // CardWizardCardConfig  cwcc = new CardWizardCardConfig(columns,0.5,0.5,0.5,0.75,0.0, CardWizardCardConfig.MODE_NUMERIC_LABEL_AND_VALUE );
        CardWizardCardConfig  cwcc = new CardWizardCardConfig(0.5,0.5,0.5,0.75,0.0, CardWizardCardConfig.MODE_DATA_SHOW[0] );

        // remove text..
        List<ColumnWithType> columns_stacks = columns.stream().filter( ci -> ci.type != ColumnWithType.TYPE_TEXT ).collect(Collectors.toList());

        //CardWizardStackConfig cwsc = new CardWizardStackConfig( columns_stacks ,0.5,0.5,0.5 , 0.75 , 0.25 );
        CardWizardStackConfig cwsc = new CardWizardStackConfig( 0.5,0.5,0.5 , 0.75 , 0.25 );

        CardWizardGlobalConfig cwgc = new CardWizardGlobalConfig(columns);


        return new FullCardWizardConfig(cwgc,cwcc,cwsc);
    }

    /**
     * We use this function to initialize the card layout.
     * It picks:
     * 1. first chemical structure, and then:
     * 2. string / data columns which match regexps in mConfInit_Regexps_HighestPriority
     * 3. data columns which match regexps in mConfInit_Regexps_SecondHighestPriority
     * 4. if less than six columns, then additional columns, (@TODO: starting from the left side of the table..)
     *
     * @param ctm
     * @return
     */
    public static List<ColumnWithType> createDefaultConfig_2(CompoundTableModel ctm){

        List<Integer> idxStructures = new ArrayList<>();
        List<Integer> idxText       = new ArrayList<>();
        List<Integer> idxNumeric    = new ArrayList<>();

        for(int zi=0;zi<ctm.getColumnCount();zi++){
            if(ctm.isColumnTypeStructure(zi)){ idxStructures.add(zi); }
            if(ctm.isColumnTypeDouble(zi)){ idxNumeric.add(zi); }
            if(ctm.isColumnTypeString(zi)){ idxText.add(zi); }
        }


        List<ColumnWithType> columns    = new ArrayList<>();

        if(!idxStructures.isEmpty()){
            columns.add( new ColumnWithType(ctm, idxStructures.get(0) , ColumnWithType.TYPE_CHEM_STRUCTURE ) );
        }

        for(int zi1=0;zi1<idxText.size();zi1++){
            int zi = zi1;
            if( Arrays.stream( mConfInit_Regexps_HighestPriority ).filter( ri -> ri.matcher( ctm.getColumnTitle(idxText.get(zi)) ).find() ).count() > 0){
                columns.add( new ColumnWithType(ctm, idxText.get(zi) , ColumnWithType.TYPE_TEXT ) );
            }
            else if( Arrays.stream( mConfInit_Regexps_SecondHighestPriority ).filter( ri -> ri.matcher( ctm.getColumnTitle(idxText.get(zi)) ).find() ).count() > 0 ){
                columns.add( new ColumnWithType(ctm, idxText.get(zi) , ColumnWithType.TYPE_TEXT ) );
            }
        }

        for(int zi1=0;zi1<idxNumeric.size();zi1++){
            int zi = zi1;
            if( Arrays.stream( mConfInit_Regexps_HighestPriority ).filter( ri -> ri.matcher( ctm.getColumnTitle(idxNumeric.get(zi)) ).find() ).count() > 0){
                columns.add( new ColumnWithType(ctm, idxText.get(zi) , ColumnWithType.TYPE_NUMERIC ) );
            }
            else if( Arrays.stream( mConfInit_Regexps_SecondHighestPriority ).filter( ri -> ri.matcher( ctm.getColumnTitle(idxNumeric.get(zi)) ).find() ).count() > 0 ){
                columns.add( new ColumnWithType(ctm, idxText.get(zi) , ColumnWithType.TYPE_NUMERIC ) );
            }
        }

        Set<Integer> column_indeces = columns.stream().mapToInt(ci -> ci.column ).boxed().collect(Collectors.toSet());

        List<Integer> remaniningNumericColumns = idxNumeric.stream().filter( idi -> ! columns.contains(idi) ).collect(Collectors.toList());

        for(int zi=0;zi<Math.min( 6-columns.size() , remaniningNumericColumns.size());zi++){
            columns.add( new ColumnWithType(ctm, remaniningNumericColumns.get(zi) , ColumnWithType.TYPE_NUMERIC ) );
        }

        return columns;
    }




    /**
     *
     * Serialization of FCWC to Properties object.
     * We have the following properties:
     *
     * "global"
     * ListOfColumnWithTypes : comma separated string of "serialized cwts". For their (de-)serialization one can just
     *                         use the ".toString()" function and the constructor ColumnWithType(CMT,String)
     *
     * "card config":
     * has the 6 properties from the corresponding JMyOptionsTable
     *
     * "stack config":
     * also has the 6 properties form the corresponding JMyOptionsTable
     *
     */
    public static class FullCardWizardConfig{
        public static String PROPERTYNAME_LIST_OF_COLUMNS_WITH_TYPE = "ListOfColumnsWithType";

        public CardWizardGlobalConfig  mGlobalConfig  = null;
        public CardWizardCardConfig    mCardConfig    = null;
        public CardWizardStackConfig   mStackConfig   = null;

        public FullCardWizardConfig( CardWizardGlobalConfig globalConfig, CardWizardCardConfig cardConfig , CardWizardStackConfig stackConfig ){
            this.mGlobalConfig = globalConfig;
            this.mCardConfig   = cardConfig;
            this.mStackConfig  = stackConfig;
        }

        public Properties toProperties() {

            Properties p = new Properties();

            // serialize the columns from the global config:
            StringBuilder sb_cols = new StringBuilder();
            for(int zi = 0; zi<this.mGlobalConfig.mColumns.size();zi++){
                sb_cols.append( this.mGlobalConfig.mColumns.get(zi) );
                if(zi<this.mGlobalConfig.mColumns.size()-1){
                    sb_cols.append(",");
                }
            }
            p.put(PROPERTYNAME_LIST_OF_COLUMNS_WITH_TYPE,sb_cols.toString());

            // serialize the cards config: just copy the properties..
            p.put(PROPERTYNAME_CARD_DATAPANEL_DIV_A , mCardConfig.getDivA() );
            p.put(PROPERTYNAME_CARD_DATAPANEL_DIV_B , mCardConfig.getDivB() );
            p.put(PROPERTYNAME_CARD_DATAPANEL_MODE , mCardConfig.getMode() );
            p.put(PROPERTYNAME_CARD_SIZE_CHEMSTRUCTURE , mCardConfig.getSizeChemistryStructure() );
            p.put(PROPERTYNAME_CARD_SIZE_CHEMREACTION , mCardConfig.getSizeChemistryRxn() );
            p.put(PROPERTYNAME_CARD_SIZE_NUMERIC , mCardConfig.getSizeNumeric() );

            // serialize the stacks config: just copy the properties..
            p.put(PROPERTYNAME_STACK_DATAPANEL_DIV_A , mStackConfig.getDivA() );
            p.put(PROPERTYNAME_STACK_DATAPANEL_DIV_B , mStackConfig.getDivB() );
            p.put(PROPERTYNAME_STACK_SIZE_CHEMSTRUCTURE , mStackConfig.getSizeChemistryStructure() );
            p.put(PROPERTYNAME_STACK_SIZE_CHEMREACTION , mStackConfig.getSizeChemistryRxn() );
            p.put(PROPERTYNAME_STACK_SIZE_NUMERIC , mStackConfig.getSizeNumeric() );

            return p;
        }

        public FullCardWizardConfig(CompoundTableModel ctm, Properties p) {

            // 1. deserialize the list of columns
            List<ColumnWithType> cols = new ArrayList<>();
            String col_data           = (String) p.get(PROPERTYNAME_LIST_OF_COLUMNS_WITH_TYPE);
            String splits[] = col_data.split(",");
            for(int zi=0; zi<splits.length; zi++) {
                cols.add( new JCardWizard2.ColumnWithType( ctm , splits[zi] ) );
            }
            this.mGlobalConfig = new CardWizardGlobalConfig(cols);

            // 2. deserialize the cards config..
            double c_size_chem_structure = Double.parseDouble( (String) p.get(PROPERTYNAME_CARD_SIZE_CHEMSTRUCTURE) );
            double c_size_chem_rxn       = Double.parseDouble( (String) p.get(PROPERTYNAME_CARD_SIZE_CHEMREACTION) );
            double c_size_numeric        = Double.parseDouble( (String) p.get(PROPERTYNAME_CARD_SIZE_NUMERIC) );
            double c_div_a               = Double.parseDouble( (String) p.get(PROPERTYNAME_CARD_DATAPANEL_DIV_A) );
            double c_div_b               = Double.parseDouble( (String) p.get(PROPERTYNAME_CARD_DATAPANEL_DIV_B) );
            String c_mode                = (String) p.get(PROPERTYNAME_CARD_DATAPANEL_MODE);

            this.mCardConfig = new CardWizardCardConfig( c_size_chem_structure,c_size_chem_rxn,c_size_numeric,c_div_a,
                                                            c_div_b, c_mode);

            // 3. deserialize the stacks config:
            double s_size_chem_structure = Double.parseDouble( (String) p.get(PROPERTYNAME_STACK_SIZE_CHEMSTRUCTURE) );
            double s_size_chem_rxn       = Double.parseDouble( (String) p.get(PROPERTYNAME_STACK_SIZE_CHEMREACTION) );
            double s_size_numeric        = Double.parseDouble( (String) p.get(PROPERTYNAME_STACK_SIZE_NUMERIC) );
            double s_div_a               = Double.parseDouble( (String) p.get(PROPERTYNAME_STACK_DATAPANEL_DIV_A) );
            double s_div_b               = Double.parseDouble( (String) p.get(PROPERTYNAME_STACK_DATAPANEL_DIV_B) );

            this.mStackConfig = new CardWizardStackConfig( s_size_chem_structure,s_size_chem_rxn,s_size_numeric,
                                                           s_div_a, s_div_b);

        }
    }


//    public static void main(String args[]){
//        JFrame testFrame = new JFrame();
//
//        testFrame.getContentPane().setLayout( new BorderLayout() );
//        testFrame.getContentPane().add( new JCardWizard2(null,null,null,null,null , false  , null ) , BorderLayout.CENTER );
//
//        testFrame.setVisible(true);
//        testFrame.setSize( testFrame.getContentPane().getPreferredSize() );
//    }

}
