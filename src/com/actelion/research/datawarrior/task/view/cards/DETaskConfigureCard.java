package com.actelion.research.datawarrior.task.view.cards;

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.task.view.DETaskAbstractSetViewOptions;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JCardView;
import com.actelion.research.table.view.card.JCardPane;
import com.actelion.research.table.view.card.cardsurface.CardElementLayoutLogic;
import com.actelion.research.table.view.card.cardsurface.gui.JCardWizard2;
import com.actelion.research.table.view.card.tools.DataWarriorLink;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.actelion.research.datawarrior.task.view.cards.MyProperties.stringListToString_inverse;
import static java.awt.datatransfer.DataFlavor.stringFlavor;

//public class DETaskConfigureCard extends ConfigurableTask {
public class DETaskConfigureCard extends DETaskAbstractSetViewOptions {

    private static final String TASK_NAME = "Configure Card";

    public static final String KEY_SELECTED_COLUMNS_STRUCTURES        = "SelectedColumnsStructures";

    public static final String KEY_SELECTED_COLUMNS_NUMERICAL_VALUES  = "SelectedColumnsNumericalValues";


    private DataWarriorLink mLink;

    private CompoundTableModel mCTM;

    //private JCardPane mCardPane;


    //public DETaskConfigureCard(Frame owner, DataWarriorLink link , JCardPane cardPane) {
    public DETaskConfigureCard(Frame owner, DEMainPane mainPane, JCardPane cardPane, DataWarriorLink dwlink) {
        super(owner,mainPane,cardPane.getCardView());

        this.mLink = dwlink;
        this.mCTM = dwlink.getCTM();
        //this.mCardPane = cardPane;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public void addViewConfiguration(CompoundTableView view, Properties configuration) {

        JCardView cardView = (JCardView)view;
        JCardPane cardPane = cardView.getCardPane();

        //JCardWizard2.FullCardWizardConfig config = cardView.getCardWizardConfig();

//        List<String> column_names_structure = cardView.getCardWizardConfig().mGlobalConfig.getColumns().stream().filter( si -> si.isChemical() ).map( si -> si.getColumnName() ).collect(Collectors.toList());
//        List<String> column_names_numeric   = cardView.getCardWizardConfig().mGlobalConfig.getColumns().stream().filter( si -> si.isNumeric() ).map( si -> si.getColumnName() ).collect(Collectors.toList());

        List<String> column_names_structure = cardView.getGlobalConfig().stream().filter( si -> si.isChemical() ).map( si -> si.getColumnName() ).collect(Collectors.toList());
        List<String> column_names_numeric   = cardView.getGlobalConfig().stream().filter( si -> si.isNumeric() ).map( si -> si.getColumnName() ).collect(Collectors.toList());

        try {
            configuration.setProperty(KEY_SELECTED_COLUMNS_STRUCTURES, MyProperties.stringListToString( column_names_structure ) );
            configuration.setProperty(KEY_SELECTED_COLUMNS_NUMERICAL_VALUES, MyProperties.stringListToString( column_names_numeric ));
        }
        catch(Exception e){
            System.out.println(e);
        }
    }

    @Override
    public void addDialogConfiguration(Properties configuration) {

        try {
            configuration.setProperty(KEY_SELECTED_COLUMNS_STRUCTURES, MyProperties.stringListToString( mView.getSelectedStructureColumns() ) );
            configuration.setProperty(KEY_SELECTED_COLUMNS_NUMERICAL_VALUES, MyProperties.stringListToString( mView.getSelectedNumericalColumns()));
        }
        catch(Exception e){
            System.out.println(e);
        }
    }


    @Override
    public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
        return true;
    }

    @Override
    public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
        if (!(view instanceof JCardView))
            return;

        JCardPane cardPane = ((JCardView)view).getCardPane();
        MyProperties p = new MyProperties(configuration);

        String s_structure_columns  = p.getString(KEY_SELECTED_COLUMNS_STRUCTURES);
        String s_numeric_columns    = p.getString(KEY_SELECTED_COLUMNS_NUMERICAL_VALUES);

        List<String> structure_columns  = stringListToString_inverse(s_structure_columns );
        List<String> numeric_columns    = stringListToString_inverse(s_numeric_columns );

        // init list of columns:
        List<JCardWizard2.ColumnWithType> columns = new ArrayList<>();

        for(String si : structure_columns){
            columns.add( new JCardWizard2.ColumnWithType(mCTM, mCTM.findColumn(si)+"/"+ JCardWizard2.ColumnWithType.TYPE_CHEM_STRUCTURE));
        }
        for(String si : numeric_columns){
            columns.add( new JCardWizard2.ColumnWithType(mCTM, mCTM.findColumn(si)+"/"+JCardWizard2.ColumnWithType.TYPE_NUMERIC));
        }

        CardElementLayoutLogic layout_logic = ((JCardView)view).getCardElementLayoutLogic();
        CardElementLayoutLogic.CardAndStackConfig config = layout_logic.createFullCardWizardConfig(columns);

        cardPane.getCardView().setGlobalConfig( columns );
        cardPane.getCardDrawer().setCardDrawingConfig(config.CDC);
        cardPane.getStackDrawer().setStackDrawingConfig(config.SDC);

        cardPane.reinitAllCards();
    }

    @Override
    public void enableItems() {

    }

    @Override
    public void setDialogToDefault() {

    }

    @Override
    public boolean isViewTaskWithoutConfiguration() {
        return false;
    }

    @Override
    public JComponent createViewOptionContent() {
        Function<Void,Void> callback = (Void v) ->  {applyConfiguration( getInteractiveView() , getDialogConfiguration(), false ) ; return null;};
        mView = new JNewCardWizard( callback );
        return mView;
    }

//    @Override
//    public boolean isConfigurationValid(Properties configuration, boolean isLive) {
//        return true;
//    }

//    //@Override
//    public void runTask_(Properties configuration) {
//
//        MyProperties p = new MyProperties(configuration);
//
//        String s_structure_columns  = p.getString(KEY_SELECTED_COLUMNS_STRUCTURES);
//        String s_numeric_columns    = p.getString(KEY_SELECTED_COLUMNS_NUMERICAL_VALUES);
//
//        List<String> structure_columns  = stringListToString_inverse(s_structure_columns );
//        List<String> numeric_columns    = stringListToString_inverse(s_numeric_columns );
//
//        // init list of columns:
//        List<JCardWizard2.ColumnWithType> columns = new ArrayList<>();
//
//        for(String si : structure_columns){
//            columns.add( new JCardWizard2.ColumnWithType(mCTM, mCTM.findColumn(si)+"/"+ JCardWizard2.ColumnWithType.TYPE_CHEM_STRUCTURE));
//        }
//        for(String si : numeric_columns){
//            columns.add( new JCardWizard2.ColumnWithType(mCTM, mCTM.findColumn(si)+"/"+JCardWizard2.ColumnWithType.TYPE_NUMERIC));
//        }
//
//        CardAndStackConfig config = createFullCardWizardConfig(columns);
//
//        mCardPane.getCardDrawer().setCardDrawingConfig(config.CDC);
//        mCardPane.getStackDrawer().setStackDrawingConfig(config.SDC);
//
//    }

//    @Override
//    public DEFrame getNewFrontFrame() {
//        return null;
//    }


//----------------------Implementation Card Layout Logic --------------------------------------------------------------
                        // This is needed both by the GUI and by the Task without the GUI

//    static class CardAndStackConfig {
//        public final CardDrawingConfig CDC;
//        public final StackDrawingConfig SDC;
//
//        public CardAndStackConfig(CardDrawingConfig cdc, StackDrawingConfig sdc) {
//            CDC = cdc;
//            SDC = sdc;
//        }
//    }
//
//
//    public CardAndStackConfig createFullCardWizardConfig(List<JCardWizard2.ColumnWithType> columns) {
//
//        JCardWizard2.CardWizardGlobalConfig cwgc = new JCardWizard2.CardWizardGlobalConfig(columns);
//
//        JCardWizard2.CardWizardCardConfig  cwcc = new JCardWizard2.CardWizardCardConfig(Double.NaN,Double.NaN ,Double.NaN  , 0.75 , 0.0 , JCardWizard2.CardWizardCardConfig.MODE_DATA_SHOW[0]  );
//        JCardWizard2.CardWizardStackConfig cwsc = new JCardWizard2.CardWizardStackConfig(Double.NaN,Double.NaN ,Double.NaN , 0.75 , 0.25 );
//
//        //JCardWizard2.FullCardWizardConfig conf = new JCardWizard2;
//        CardDrawingConfig   cdc = createCardConfiguration(cwgc, cwcc);
//        StackDrawingConfig  sdc = createStackConfiguration(cwgc, cwsc);
//
//        return new CardAndStackConfig(cdc,sdc);
//    }
//
//
//    public CardDrawingConfig createCardConfiguration(JCardWizard2.CardWizardGlobalConfig gconf , JCardWizard2.CardWizardCardConfig config ) {
//
//        CardDrawingConfig cdc = new CardDrawingConfig( mLink.getTableColorHandler() );
//
//        // compute layout:
//        List<JCardWizard2.ColumnWithType> listCS = gconf.getColumns().stream().filter(ci -> ci.type == JCardWizard2.ColumnWithType.TYPE_CHEM_STRUCTURE ).collect(Collectors.toList());
//        List<JCardWizard2.ColumnWithType> listNu = gconf.getColumns().stream().filter(ci -> ci.type == JCardWizard2.ColumnWithType.TYPE_NUMERIC ).collect(Collectors.toList());
//
//        double layout[] = computeLayout_Card( listCS.size() , listNu.size() );
//        double h_structure  = layout[0];
//        double h_numeric    = layout[1];
//
//
//        // Add structure panel
//        double currentPosition = 0.0;
//        for( JCardWizard2.ColumnWithType cc : listCS ) {
//            StructurePanel sp = new StructurePanel();
//            sp.setColumns(new int[]{cc.column});
//            cdc.getSlots().add(new CardDrawingConfig.CardSurfaceSlot(cdc, sp, currentPosition, h_structure));
//            currentPosition += h_structure;
//        }
//
//        // Create MultiDataPanel to show numeric stuff
//
//        MultiDataPanel mdp = new MultiDataPanel();
//        mdp.setNumberOfColumns(listNu.size());
//        mdp.setColumns( listNu.stream().mapToInt(  nci -> nci.column).toArray() );
//        mdp.setDivA( config.getDivA() ); mdp.setDivB( config.getDivB() );
//
//        mdp.setDataShowMode( JCardWizard2.CardWizardCardConfig.MAP_DATA_SHOW_MODES.get( config.getMode() ) );
//        cdc.getSlots().add( new CardDrawingConfig.CardSurfaceSlot( cdc , mdp, currentPosition , listNu.size() * h_numeric ) );
//
//        return cdc;
//    }
//
//    public StackDrawingConfig createStackConfiguration(JCardWizard2.CardWizardGlobalConfig gconf , JCardWizard2.CardWizardStackConfig config ) {
//
//        CardDrawingConfig cdc = new CardDrawingConfig( mLink.getTableColorHandler() );
//
//        // compute layout:
//        List<JCardWizard2.ColumnWithType> listCS = gconf.getColumns().stream().filter(ci -> ci.type == JCardWizard2.ColumnWithType.TYPE_CHEM_STRUCTURE ).collect(Collectors.toList());
//        List<JCardWizard2.ColumnWithType> listNu = gconf.getColumns().stream().filter(ci -> ci.type == JCardWizard2.ColumnWithType.TYPE_NUMERIC ).collect(Collectors.toList());
//
//        double layout[] = computeLayout_Stack( listCS.size() , listNu.size() );
//        double h_structure  = layout[0];
//        double h_numeric    = layout[1];
//
//        StackDrawingConfig sdc = new StackDrawingConfig(mLink.getTableColorHandler());
//
//        // add themz:
//        double currentPosition = 0.0;
//
//        NumCardsIndicatorPanel ssip = new NumCardsIndicatorPanel();
//        sdc.getSlots().add(new StackDrawingConfig.StackSurfaceSlot(ssip,currentPosition,conf_stack_card_inicator_size));
//        currentPosition += conf_stack_card_inicator_size;
//
//        for( JCardWizard2.ColumnWithType cc : listCS ){
//            if(cc.type== JCardWizard2.ColumnWithType.TYPE_CHEM_STRUCTURE) {
//                StackChemStructurePanel sp = new StackChemStructurePanel();
//                sp.setColumns(new int[]{cc.column});
//                sdc.getSlots().add(new StackDrawingConfig.StackSurfaceSlot( sp, currentPosition, h_structure));
//                currentPosition += h_structure;
//            }
//        }
//
//        // add multipanel for numerical values below:
//        StackMultiDataPanel mdp = new StackMultiDataPanel();
//
//        mdp.setDivA(config.getDivA()); mdp.setDivB(config.getDivB());
//        mdp.setNumberOfColumns(listNu.size());
//        mdp.setColumns( listNu.stream().mapToInt(  nci -> nci.column).toArray() );
//
//        sdc.getSlots().add( new StackDrawingConfig.StackSurfaceSlot( mdp, currentPosition , listNu.size() * h_numeric ) );
//
//        return sdc;
//    }
//
//
//
//
//    // values for card, not for stack obviously
//    private final double conf_max_numerical_height     = 0.12;
//    // values for card, not for stack obviously
//    private final double conf_optimal_structure_height = 0.5;
//
//    // additional conf values for stacks. Here, obviously we want numerical values to be as big as possible,
//    // to show the density stuff
//    private final double conf_stack_card_inicator_size       = 0.1;
//    private final double conf_stack_optimal_numerical_height = 0.3;
//    private final double conf_stack_optimal_structure_height = 0.25;
//
//
//    /**
//     * Computes the y layout for the card.
//     *
//     *
//     * @param countStructures and countNumerical
//     * @return double array, where first value is structure height, second value is numerical height
//     */
//    public double[] computeLayout_Card( int countStructures , int countNumerical ) {
//
////        int countStructures = (int) slots.stream().filter( si -> si.intValue() == 1 ).count();
////        int countNumerical  = (int) slots.stream().filter( si -> si.intValue() == 2 ).count();
//
//
//        // this is what we want to compute..
//        double final_height_structure  = Double.NaN;
//        double final_height_numerical  = Double.NaN;
//
//        // check if optimal layout is possible
//        double optimal_space = countStructures * conf_optimal_structure_height + countNumerical * conf_max_numerical_height;
//
//        // check what to do if we have space left.. that's pretty simple
//        if(optimal_space <= 1.0){
//            final_height_numerical = conf_max_numerical_height;
//            if( countStructures >= 1 ){
//                final_height_structure = ( 1.0 - ( countNumerical * conf_max_numerical_height ) ) / countStructures;
//            }
//        }
//
//        // check what to do if we need too much space
//        if(optimal_space > 1.0){
//
//            double total_space = countStructures * conf_optimal_structure_height + countNumerical * conf_max_numerical_height;
//
//            // divide "optimal" values by total_space..
//            final_height_numerical = conf_max_numerical_height / total_space;
//            final_height_structure = conf_optimal_structure_height / total_space;
//        }
//
//        return new double[]{ final_height_structure , final_height_numerical };
//    }
//
//    /**
//     * Computes the y layout for the stack.
//     *
//     * @param countStructures , countNumerical
//     * @return double array, where first value is structure height, second value is numerical height
//     */
//    public double[] computeLayout_Stack( int countStructures , int countNumerical ) {
//
////        int countStructures = (int) slots.stream().filter( si -> si.intValue() == 1 ).count();
////        int countNumerical  = (int) slots.stream().filter( si -> si.intValue() == 2 ).count();
//
//        // this is what we want to compute..
//        double final_height_structure  = Double.NaN;
//        double final_height_numerical  = Double.NaN;
//
//        // check if optimal layout is possible
//        double available_space  = 1.0 - conf_stack_card_inicator_size;
//        double optimal_space    = countStructures * conf_stack_optimal_structure_height + countNumerical * conf_stack_optimal_numerical_height;
//
//        // check what to do if we have space left.. that's pretty simple
//        if(optimal_space <= available_space){
//            final_height_numerical = conf_stack_optimal_numerical_height * ( available_space / optimal_space )  ;
//            final_height_structure = conf_stack_optimal_structure_height * ( available_space / optimal_space )  ;
//        }
//
//        // check what to do if we need too much space.. we actually do the same..
//        if(optimal_space > 1.0){
//            final_height_numerical = conf_stack_optimal_numerical_height * ( available_space / optimal_space )  ;
//            final_height_structure = conf_stack_optimal_structure_height * ( available_space / optimal_space )  ;
////            double total_space = countStructures * conf_optimal_structure_height + countNumerical * conf_max_numerical_height;
////
////            // divide "optimal" values by total_space..
////            final_height_numerical = conf_max_numerical_height / total_space;
////            final_height_structure = conf_optimal_structure_height / total_space;
//        }
//
//        return new double[]{ final_height_structure , final_height_numerical };
//    }
//
//
//
//
//
//





//------------------------------ GUI -----------------------------------------------------------------------------------

    private JNewCardWizard mView;

//    @Override
//    public JComponent createDialogContent() {
//        mView = new JNewCardWizard();
//        return mView;
//    }

    //@Override
    public Properties getDialogConfiguration_() {

        Properties p = new Properties();

        try {
            p.setProperty(KEY_SELECTED_COLUMNS_STRUCTURES, MyProperties.stringListToString(mView.getSelectedStructureColumns()));
            p.setProperty(KEY_SELECTED_COLUMNS_NUMERICAL_VALUES, MyProperties.stringListToString(mView.getSelectedNumericalColumns()));
        }
        catch(Exception e){
            System.out.println(e);
        }

        return p;
    }

    @Override
    public void setDialogToConfiguration(Properties configuration) {

        MyProperties p = new MyProperties(configuration);



        List<String> structureColumns   =  MyProperties.stringListToString_inverse( p.getString(KEY_SELECTED_COLUMNS_STRUCTURES) );
        List<String> numericColumns     =  MyProperties.stringListToString_inverse( p.getString(KEY_SELECTED_COLUMNS_NUMERICAL_VALUES) );

        if(structureColumns==null){structureColumns = new ArrayList<>();}
        if(numericColumns==null){numericColumns = new ArrayList<>();}

        for(String ci : structureColumns) {
            int idx = mCTM.findColumn(ci);
            if(idx>=0){
                if(mCTM.isColumnTypeStructure(idx)) {
                    ((DefaultListModel<JCardWizard2.ColumnWithType>) this.mView.mListColumnsToShow.getModel()).addElement(new JCardWizard2.ColumnWithType(mCTM, idx, JCardWizard2.ColumnWithType.TYPE_CHEM_STRUCTURE));
                }
            }
        }

        for(String ci : numericColumns) {
            int idx = mCTM.findColumn(ci);
            if(idx>=0){
                if(mCTM.isColumnTypeDouble(idx)) {
                    ((DefaultListModel<JCardWizard2.ColumnWithType>) this.mView.mListColumnsToShow.getModel()).addElement(new JCardWizard2.ColumnWithType(mCTM, idx, JCardWizard2.ColumnWithType.TYPE_NUMERIC));
                }
            }
        }



    }

    //@Override
    public void setDialogConfigurationToDefault_() {

        // TODO: fill..
        String importantColNames[] = new String[] {};

        // 1. check for structure..
        String nameColStructure = null;
        for(int zi=0;zi<mCTM.getColumnCount();zi++){
            if(mCTM.isColumnTypeStructure(zi)){
                nameColStructure = mCTM.getColumnTitle(zi);
                break;
            }
        }

        // 2. search for other important things..
        List<String> numericCols = new ArrayList<>();

        for(int zi=0;zi<mCTM.getColumnCount();zi++){
            if(mCTM.isColumnTypeDouble(zi)){
                String name = mCTM.getColumnTitle(zi);
                if( Arrays.stream(importantColNames).anyMatch(  ai -> name.matches(ai) ) ){
                    numericCols.add(name);
                }

            }
        }

        return;

    }


//    static class ColumnWithType {
//        public final String Name;
//        public final booelan isStructure;
//        public final booelan isNumerical;
//        public final booelan isCategorical;
//        public final booelan isText;
//    }


    class JNewCardWizard extends JPanel implements ListDataListener, KeyListener {

        private JScrollPane mScrollPaneAvailableColumns_Combined;
        //private JScrollPane mScrollPaneAvailableColumnsStructure;
        //private JScrollPane mScrollPaneAvailableColumnsNumeric;

        private JScrollPane mScrollPaneColumnsToShow;

        private JList<JCardWizard2.ColumnWithType> mListAvailableColumns_Combined   = new JList<>( new DefaultListModel<>() );
//        private JList<JCardWizard2.ColumnWithType> mListAvailableColumnsStructure   = new JList<>( new DefaultListModel<>() );
//        private JList<JCardWizard2.ColumnWithType> mListAvailableColumnsNumeric     = new JList<>( new DefaultListModel<>() );


        private JList<JCardWizard2.ColumnWithType> mListColumnsToShow = new JList<>( new DefaultListModel<>() );


        // callback when something changed..
        private Function<Void,Void> mCallback;

        public List<String> getSelectedStructureColumns() {
            List<String> list = new ArrayList<>();
            for(int zi=0;zi<mListColumnsToShow.getModel().getSize();zi++){
                if(mListColumnsToShow.getModel().getElementAt(zi).type==JCardWizard2.ColumnWithType.TYPE_CHEM_STRUCTURE){ list.add(mListColumnsToShow.getModel().getElementAt(zi).getColumnName()); }
            }
            return list;
        }
        public List<String> getSelectedNumericalColumns() {
            List<String> list = new ArrayList<>();
            for(int zi=0;zi<mListColumnsToShow.getModel().getSize();zi++){
                if(mListColumnsToShow.getModel().getElementAt(zi).type==JCardWizard2.ColumnWithType.TYPE_NUMERIC){ list.add(mListColumnsToShow.getModel().getElementAt(zi).getColumnName()); }
            }
            return list;
        }


        public JNewCardWizard(Function<Void,Void> callback) {
            this.mCallback = callback;

            initGUI();

            initListeners();

            // finds the columns and fills the JLists
            initColumns();
        }

        public void initColumns() {

            for(int zi=0;zi<mCTM.getTotalColumnCount();zi++){
                if(mCTM.isColumnTypeStructure(zi)){ ((DefaultListModel<JCardWizard2.ColumnWithType>) mListAvailableColumns_Combined.getModel() ).addElement(new JCardWizard2.ColumnWithType(mCTM, zi, JCardWizard2.ColumnWithType.TYPE_CHEM_STRUCTURE)); }
                else if(mCTM.isColumnTypeDouble(zi)){ ((DefaultListModel<JCardWizard2.ColumnWithType>) mListAvailableColumns_Combined.getModel() ).addElement(new JCardWizard2.ColumnWithType(mCTM, zi, JCardWizard2.ColumnWithType.TYPE_NUMERIC)); }
                else if(mCTM.isColumnTypeCategory(zi)) { ((DefaultListModel<JCardWizard2.ColumnWithType>) mListAvailableColumns_Combined.getModel() ).addElement(new JCardWizard2.ColumnWithType(mCTM, zi, JCardWizard2.ColumnWithType.TYPE_NUMERIC));  }

                //if(mCTM.isColumnTypeDouble(zi)){ ((DefaultListModel<JCardWizard2.ColumnWithType>) mListAvailableColumnsNumeric.getModel() ).addElement(new JCardWizard2.ColumnWithType(mCTM, zi, JCardWizard2.ColumnWithType.TYPE_NUMERIC)); }
                //if(mCTM.isColumnTypeStructure(zi)){ ((DefaultListModel<JCardWizard2.ColumnWithType>) mListAvailableColumnsStructure.getModel() ).addElement(new JCardWizard2.ColumnWithType(mCTM, zi, JCardWizard2.ColumnWithType.TYPE_CHEM_STRUCTURE)); }
            }
        }

        private void initListeners() {
            this.mListColumnsToShow.addKeyListener(this);
            this.mListColumnsToShow.getModel().addListDataListener(this);
        }

        @Override
        public void keyTyped(KeyEvent e) {

        }

        @Override
        public void keyPressed(KeyEvent e) {
            if(e.getKeyCode()==KeyEvent.VK_DELETE){
                if( this.mListColumnsToShow.getSelectedValue() != null ){
                    ((DefaultListModel<JCardWizard2.ColumnWithType>) this.mListColumnsToShow.getModel() ).remove( this.mListColumnsToShow.getSelectedIndex() );
                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {

        }

        @Override
        public void intervalAdded(ListDataEvent e) {
            mCallback.apply(null) ;
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            mCallback.apply(null) ;
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            mCallback.apply(null) ;
        }

        private void initGUI(){
            double p = TableLayout.PREFERRED;

            double p4 = HiDPIHelper.scale(4);
            double p8 = HiDPIHelper.scale(8);


            double layout[][] = new double[][] { { p4 , p , p8  , p  , p8 , 0.5 , p4 } , {  p4 , p , p8 , p , p4 } };
            this.setLayout( new TableLayout(layout) );

            //this.mScrollPaneAvailableColumnsStructure  = new JScrollPane( mListAvailableColumnsStructure );
            //this.mScrollPaneAvailableColumnsNumeric    = new JScrollPane( mListAvailableColumnsNumeric );
            this.mScrollPaneAvailableColumns_Combined = new JScrollPane( mListAvailableColumns_Combined);

            this.mScrollPaneColumnsToShow     = new JScrollPane( mListColumnsToShow );

//            JPanel pS = new JPanel(); pS.setLayout(new BorderLayout());
//            pS.add(new JLabel("Chemical Structures"),BorderLayout.NORTH);
//            pS.add(mScrollPaneAvailableColumnsStructure,BorderLayout.CENTER);
//            this.add(pS , "1,1");
//
//            JPanel pN = new JPanel(); pN.setLayout(new BorderLayout());
//            pN.add(new JLabel("Numeric / categorical values"),BorderLayout.NORTH);
//            pN.add(mScrollPaneAvailableColumnsNumeric,BorderLayout.CENTER);
//            this.add(pN , "1,3");

            JPanel pS = new JPanel(); pS.setLayout(new BorderLayout());
            pS.add(new JLabel("Available columns"),BorderLayout.NORTH);
            pS.add(mScrollPaneAvailableColumns_Combined,BorderLayout.CENTER);
            this.add(pS , "1,1");

            JPanel pSelection = new JPanel(); pSelection.setLayout(new BorderLayout());
            pSelection.add(new JLabel("Selected columns"),BorderLayout.NORTH);
            pSelection.add(mScrollPaneColumnsToShow,BorderLayout.CENTER);
            this.add(pSelection,"3,1");

            mListAvailableColumns_Combined.setTransferHandler(new JCardWizard2.MyTransferHandlerForTypeData());
            mListAvailableColumns_Combined.setDragEnabled(true);
//            mListAvailableColumnsStructure.setTransferHandler( new JCardWizard2.MyTransferHandlerForTypeData() );
//            mListAvailableColumnsNumeric.setTransferHandler( new JCardWizard2.MyTransferHandlerForTypeData() );
//            mListAvailableColumnsStructure.setDragEnabled(true);
//            mListAvailableColumnsNumeric.setDragEnabled(true);

            mListColumnsToShow.setTransferHandler( new MyTransferHandlerImportAndSort());
            mListColumnsToShow.setDragEnabled(true);
        }


        // Transfer Handler for JList with columsn to show:
        public class MyTransferHandlerImportAndSort extends TransferHandler {

            /**
             * Indicates the start position of an internal dnd.
             */
            private int mIndexDraggedInternally = -1;

            /**
             * Indicates the index where the drop was inserted.
             */
            private int mIndexDroppedTo = -1;

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
             *
             */
            protected Transferable createTransferable(JComponent c) {

                JList myList = (JList) c;
                this.mIndexDraggedInternally = myList.getSelectedIndex();

                return new java.awt.datatransfer.StringSelection(((JCardWizard2.ColumnWithType) myList.getModel().getElementAt(this.mIndexDraggedInternally)).getEncoded());

                //return new java.awt.datatransfer.StringSelection( (String) myList.getModel().getElementAt( this.mIndexDraggedInternally ));
            }


            protected void exportDone(JComponent c, Transferable t, int action) {
                if (action == MOVE) {
                    //@TODO..
                    //c.remove removeSelection();
                    if (c.equals(mListColumnsToShow)) {
                        // internal dnd, remove the drag start index
                        try {
                            int indexToRemove = this.mIndexDraggedInternally + (this.mIndexDroppedTo < this.mIndexDraggedInternally ? 1 : 0);
                            (((DefaultListModel) (((JList) c).getModel()))).remove(indexToRemove);
                        } catch (Exception e) {
                        }

                    } else {
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
                    //System.out.println("Do import!");

                    JList list = (JList) (supp.getComponent());
                    DefaultListModel listModel = (DefaultListModel) (list.getModel());

                    // Fetch the Transferable and its data
                    Transferable t = supp.getTransferable();
                    //String data = (String) t.getTransferData(stringFlavor);
                    JCardWizard2.ColumnWithType data = new JCardWizard2.ColumnWithType(mCTM, (String) t.getTransferData(stringFlavor));

                    // Fetch the drop location
                    int loc = ((JList.DropLocation) supp.getDropLocation()).getIndex();

                    // Insert the data at this location
                    if (loc < 0) {
                        if (listModel.size() == 0) {
                            loc = 0;
                        } else {
                            loc = listModel.size();
                        }
                    }
                    //System.out.println("ADD NEW: " + data + " at: " + loc);
                    mIndexDroppedTo = loc;
                    listModel.add(loc, data);
                    //listModel.add(loc, new ColumnWithType(mModel,Integer.parseInt(data),1) );

                    // sort types..
                    boolean sortTypes = true;
                    if (sortTypes) {
                        List<JCardWizard2.ColumnWithType> typeA = new ArrayList<>();
                        List<JCardWizard2.ColumnWithType> typeB = new ArrayList<>();
                        List<JCardWizard2.ColumnWithType> typeC = new ArrayList<>();
                        List<JCardWizard2.ColumnWithType> typeD = new ArrayList<>();
                        for (int zi = 0; zi < listModel.getSize(); zi++) {
                            switch (((JCardWizard2.ColumnWithType) listModel.get(zi)).type) {
                                case JCardWizard2.ColumnWithType.TYPE_CHEM_STRUCTURE: {
                                    typeA.add((JCardWizard2.ColumnWithType) listModel.get(zi));
                                }
                                break;
                                case JCardWizard2.ColumnWithType.TYPE_CHEM_REACTION: {
                                    typeB.add((JCardWizard2.ColumnWithType) listModel.get(zi));
                                }
                                break;
                                case JCardWizard2.ColumnWithType.TYPE_NUMERIC: {
                                    typeC.add((JCardWizard2.ColumnWithType) listModel.get(zi));
                                }
                                break;
                                case JCardWizard2.ColumnWithType.TYPE_TEXT: {
                                    typeD.add((JCardWizard2.ColumnWithType) listModel.get(zi));
                                }
                                break;
                            }
                        }
                        listModel.removeAllElements();
                        typeD.stream().forEach(cwti -> listModel.addElement(cwti));
                        typeA.stream().forEach(cwti -> listModel.addElement(cwti));
                        typeB.stream().forEach(cwti -> listModel.addElement(cwti));
                        typeC.stream().forEach(cwti -> listModel.addElement(cwti));
                    }
                } catch (Exception e) {
                    System.out.println("something went wrong: " + e.toString());
                }

                // sync GUI to config.
                // for some reason the ListModelEvents dont fire, not matter what I do?!..
                //updateGlobalConfig();

                return true;
            }
        }
    }



























}
