package com.actelion.research.datawarrior.task.view.cards;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.datawarrior.task.TaskUIDelegate;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.card.CardElement;
import com.actelion.research.table.view.card.CardPaneModel;
import com.actelion.research.table.view.card.JCardPane;
import com.actelion.research.table.view.card.JFastCardPaneForSorting;
import com.actelion.research.table.view.card.tools.DataWarriorLink;
import com.actelion.research.table.view.card.tools.IntOrderedPair;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Properties object:
 *
 *
 *
 *
 *
 */
public class DETaskPositionCards extends ConfigurableTask {


    private static final String TASK_NAME = "Position Cards";

    public static final String VALUE_NO_COL_SELECTED = "<none>";


    public static final String PROPERTY_COLUMN_X = "colX";
    public static final String PROPERTY_COLUMN_Y = "colY";
    public static final String PROPERTY_GROUP_CATEGORIES_X = "groupCatX";
    public static final String PROPERTY_GROUP_CATEGORIES_Y = "groupCatY";
    public static final String PROPERTY_INVERT_X = "invertX";
    public static final String PROPERTY_INVERT_Y = "invertY";

    public static final String PROPERTY_STRETCH        = "stretch";
    public static final String PROPERTY_ASPECT_RATIO   = "aspectRatio";

    public static final String PROPERTY_SELECTED_ONLY  = "selectedOnly";

    public static final String PROPERTY_SHAPE = "shape";

    public static final String PROPERTY_SPLIT_BY = "splitBy";


    public static final String VALUES_SHAPE[] = new String[] { "Grid" , "Linear" , "Spiral" , "Circle" };


    private DataWarriorLink mLink;

    private CompoundTableModel mCTM;

    private DETaskPositionCardsView mView;

    private JCardPane mCardPane;


    private DETaskPositionCardsView.MyMultiChangeListener mMyChangeListener;


    public DETaskPositionCards(Frame owner, DataWarriorLink link , JCardPane cardPane) {
        super(owner,false);

        this.mLink = link;
        this.mCTM = link.getCTM();
        this.mCardPane = cardPane;

        initColumns();


        mView = new DETaskPositionCardsView();
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
    public boolean isConfigurationValid(Properties configuration, boolean isLive) {
        try {
            MyProperties p = new MyProperties(configuration);
            if(isLive) {
                if(!mColumns_X.stream().map(xi -> xi.Name ).collect(Collectors.toList()).contains(p.getString(PROPERTY_COLUMN_X))){return false;}
                if(!mColumns_Y.stream().map(xi -> xi.Name ).collect(Collectors.toList()).contains(p.getString(PROPERTY_COLUMN_Y))){return false;}
                if(!mColumns_Split.stream().map(xi -> xi.Name ).collect(Collectors.toList()).contains(p.getString(PROPERTY_SPLIT_BY))){return false;}
            }

            List<String> shapes = Arrays.stream(VALUES_SHAPE).collect(Collectors.toList());
            if(!shapes.contains(p.getString(PROPERTY_SHAPE))){return false;}

            int stretch      = p.getInt(PROPERTY_STRETCH);
            int aspect_ratio = p.getInt(PROPERTY_ASPECT_RATIO);
            if( stretch > 100 || stretch < 0 || aspect_ratio > 100 || aspect_ratio < 0){ return false; }

        }
        catch(Exception e){
            return false;
        }

        return true;
    }

    @Override
    public void runTask(Properties configuration) {
        List<CardPaneModel.CardElementData> positions = mView.positionCards(configuration);

        // important, we have to deactivate computation of hidden card elements for performance reasons, tons of
        // events will be fired..
        boolean isComputingHiddenCards = mCardPane.isComputeHiddenCardsEnabled();
        mCardPane.setComputeHiddenCardElementsEnabled(false);
        mCardPane.getCardPaneModel().loadSerializedCardElementDataPositions(positions);
        mCardPane.setComputeHiddenCardElementsEnabled(isComputingHiddenCards);

        mCardPane.startAnimateViewportToShowAllCards();

    }

    @Override
    public DEFrame getNewFrontFrame() {
        return null;
    }


    public void initColumns() {

        mColumns_Y.add( new Col(VALUE_NO_COL_SELECTED,-1,false,false));
        mColumns_Split.add( new Col(VALUE_NO_COL_SELECTED,-1,false,false));

        for(int zi=0;zi<mCTM.getColumnCount();zi++){
            String name = mCTM.getColumnTitle(zi);
            int    idx  = zi;
            boolean isnumeric     = mCTM.isColumnTypeDouble(zi);
            boolean iscategorical = mCTM.isColumnTypeCategory(zi);

            if(isnumeric || iscategorical){
                mColumns_X.add(new Col(name,idx,isnumeric,iscategorical));
                mColumns_Y.add(new Col(name,idx,isnumeric,iscategorical));
            }

            if(iscategorical){
                mColumns_Split.add(new Col(name,idx,false,iscategorical)); // init it this way that it shows the right thing
            }
        }
    }


    /**
     * Idx==-1 means it is "none"
     */
    class Col {
        public final String Name;
        public final int Idx;
        public final boolean isNumeric;
        public final boolean isCategorical;
        public Col(String name, int idx, boolean isnumeric, boolean iscategorical){
            this.Name = name;
            this.Idx  = idx;
            this.isNumeric = isnumeric;
            this.isCategorical = iscategorical;
        }

        @Override
        public String toString() {
            if(Idx<0){
                return VALUE_NO_COL_SELECTED;
            }
            return Name+ ((isNumeric)?"[Numeric]":"") + ((isCategorical)?"[Categorical]":"");
        }
    }


    // All numerical / categorical
    List<Col> mColumns_X = new ArrayList<>();


    // <none> + All numerical / categorical
    List<Col> mColumns_Y = new ArrayList<>();

    // <none> + All categorical
    List<Col> mColumns_Split = new ArrayList<>();



    @Override
    public JComponent createDialogContent() {
        return mView;
    }

    @Override
    public Properties getDialogConfiguration() {
        return mView.getDialogConfiguration();
    }

    @Override
    public void setDialogConfiguration(Properties configuration) {
        mView.setDialogConfiguration(configuration);
    }

    @Override
    public void setDialogConfigurationToDefault() {
        mView.setDialogConfigurationToDefault();
    }


//------------------------ VIEW ----------------------------------------------------------------------------------



    public class DETaskPositionCardsView extends JPanel implements TaskUIDelegate {

        private JLabel labelColX = new JLabel("Column X ");
        private JLabel labelColY = new JLabel("Column Y ");

        private JLabel labellSize        = new JLabel("Size ");
        private JLabel labellAspectRatio = new JLabel("Aspect Ratio ");

        private JLabel labelShape     = new JLabel("Shape ");

        private JLabel labelSelectedOnly   = new JLabel("Selected only ");

        private JLabel labelSplitBy        = new JLabel("Split by ");


        private JComboBox<Col> mCB_X;
        private JComboBox<Col> mCB_Y;


        private JSlider mSlider_Size;
        private JSlider mSlider_AspectRatio;

        private JComboBox<String> mCB_Shape;

        private JComboBox<Col> mCB_SplitBy;


        private JLabel labelX_gcat    = new JLabel("Group Categories ");
        private JLabel labelX_desc    = new JLabel("Descending ");

        private JLabel labelY_gcat    = new JLabel("Group Categories ");
        private JLabel labelY_desc    = new JLabel("Descending ");


        private JCheckBox mCBox_X_GroupCat = new JCheckBox();
        private JCheckBox mCBox_X_Invert   = new JCheckBox();

        private JCheckBox mCBox_Y_GroupCat = new JCheckBox();
        private JCheckBox mCBox_Y_Invert   = new JCheckBox();


        private JCheckBox mCBox_selectedOnly = new JCheckBox();

        private JPanel mPreview     = new JPanel();
        private JFastCardPaneForSorting mFastCardPane;

        public DETaskPositionCardsView() {

            initLayout();

            addListeners();

            // init FastCardPane:
            mFastCardPane = new JFastCardPaneForSorting(mCardPane.getCardPaneModel().cloneModel());
            mFastCardPane.setOpaque(true);
            mFastCardPane.setInternalBufferingEnabled(false);
            mFastCardPane.setOpaque(false);
            mFastCardPane.setViewportMovement(false);
            this.mPreview.setLayout(new BorderLayout());
            this.mPreview.add( mFastCardPane, BorderLayout.CENTER );
            this.mPreview.setOpaque(true);
            this.mPreview.setDoubleBuffered(false);


            this.updateUIEnabledStatus();
        }


        private void addListeners() {

            mMyChangeListener = new MyMultiChangeListener();
            mSlider_Size.addChangeListener(mMyChangeListener);
            mSlider_AspectRatio.addChangeListener(mMyChangeListener);

//            mCB_SplitBy.addItemListener(listener);
//            mCB_X.addItemListener(listener);
//            mCB_Y.addItemListener(listener);

            mCB_X.addActionListener(mMyChangeListener);
            mCB_Y.addActionListener(mMyChangeListener);
            mCB_SplitBy.addActionListener(mMyChangeListener);

            mCB_Shape.addActionListener(mMyChangeListener);

            mCBox_X_Invert.addChangeListener(mMyChangeListener);
            mCBox_Y_Invert.addChangeListener(mMyChangeListener);
            mCBox_X_GroupCat.addChangeListener(mMyChangeListener);
            mCBox_Y_GroupCat.addChangeListener(mMyChangeListener);

            mCBox_selectedOnly.addChangeListener(mMyChangeListener);
        }

        private void initLayout() {
            double p = TableLayout.PREFERRED;


            // TODO: figure out nice way to give reasonable width to the preview panel.. (i.e. first value in x config.)

            double layout[][] = new double[][] { {4,600,8,p,4} ,  {4,p,8,p,p,8,p,8,p,p,8,p,8,p,8,p,8,p,8,p,8} };

            this.setLayout(new TableLayout(layout));


            mCB_X = new JComboBox<Col>( new Vector(mColumns_X) );
            mCB_Y = new JComboBox<Col>( new Vector(mColumns_Y) );

            mCB_SplitBy  = new JComboBox<Col>( new Vector(mColumns_Split) );

            this.add( mPreview , "1,1,1,17" );
            mPreview.setBackground(Color.green.darker());

            JPanel p1 = new JPanel(); p1.setLayout(new BorderLayout()); p1.add(labelColX,BorderLayout.WEST); p1.add( mCB_X ,BorderLayout.CENTER);

            JPanel p2 = new JPanel(); p2.setLayout(new BorderLayout()); p2.add(labelColY,BorderLayout.WEST); p2.add( mCB_Y ,BorderLayout.CENTER);

            this.add(p1,"3,1");
            this.add(p2,"3,6");

            double layout_buttons[][] = new double[][] { { 40, p} , {4,0.999} };

            JPanel pb_x1 = new JPanel();pb_x1.setLayout(new TableLayout(layout_buttons));
            JPanel pb_x2 = new JPanel();pb_x2.setLayout(new TableLayout(layout_buttons));

            JPanel pb_y1 = new JPanel();pb_y1.setLayout(new TableLayout(layout_buttons));
            JPanel pb_y2 = new JPanel();pb_y2.setLayout(new TableLayout(layout_buttons));

            JPanel pb_x1_a = new JPanel(); pb_x1_a.setLayout(new BoxLayout(pb_x1_a,BoxLayout.X_AXIS)); pb_x1_a.add(mCBox_X_GroupCat); pb_x1_a.add(labelX_gcat);
            pb_x1.add(pb_x1_a,"1,1");

            JPanel pb_x1_b = new JPanel(); pb_x1_b.setLayout(new BoxLayout(pb_x1_b,BoxLayout.X_AXIS)); pb_x1_b.add(mCBox_X_Invert); pb_x1_b.add(labelX_desc);
            pb_x2.add(pb_x1_b,"1,1");


            JPanel pb_y1_a = new JPanel(); pb_y1_a.setLayout(new BoxLayout(pb_y1_a,BoxLayout.X_AXIS)); pb_y1_a.add(mCBox_Y_GroupCat); pb_y1_a.add(labelY_gcat);
            pb_y1.add(pb_y1_a,"1,1");

            JPanel pb_y1_b = new JPanel(); pb_y1_b.setLayout(new BoxLayout(pb_y1_b,BoxLayout.X_AXIS)); pb_y1_b.add(mCBox_Y_Invert); pb_y1_b.add(labelY_desc);
            pb_y2.add(pb_y1_b,"1,1");



            this.add(pb_x1,"3,3");
            this.add(pb_x2,"3,4");

            this.add(pb_y1,"3,8");
            this.add(pb_y2,"3,9");


            // add the shape combo box:
            mCB_Shape = new JComboBox<>((VALUES_SHAPE));
            JPanel pb_s = new JPanel(); pb_s.setLayout(new BoxLayout(pb_s,BoxLayout.X_AXIS));
            pb_s.add(labelShape); pb_s.add( mCB_Shape );
            this.add(pb_s,"3,11");


            // add the size and aspect ratio sliders:
            mSlider_Size         = new JSlider(0,100,50);
            mSlider_AspectRatio  = new JSlider(0,100,50);

            double layout_stretch[][] = new double[][] { {4,p,8,p,TableLayout.FILL} , {4,p,8,p,4} };
            JPanel p_stretch = new JPanel(); p_stretch.setLayout(new TableLayout(layout_stretch));

            p_stretch.add(labellSize,"1,1");
            p_stretch.add(mSlider_Size,"3,1");
            p_stretch.add(labellAspectRatio,"1,3");
            p_stretch.add(mSlider_AspectRatio,"3,3");

            this.add(p_stretch,"3,13");

            // Last two columns: selected only checkbox and split by column choice

            JPanel p_so = new JPanel(); p_so.setLayout( new BoxLayout(p_so,BoxLayout.X_AXIS) );
            p_so.add(mCBox_selectedOnly); p_so.add(labelSelectedOnly);

            this.add(p_so,"3,15");

            JPanel p_split = new JPanel(); p_split.setLayout( new BoxLayout(p_split,BoxLayout.X_AXIS) );
            p_split.add(labelSplitBy); p_split.add(mCB_SplitBy);

            this.add(p_split,"3,17");
        }



        @Override
        public JComponent createDialogContent() {
            return null;
        }

        @Override
        public Properties getDialogConfiguration() {
            Properties p = new Properties();
            p.setProperty( PROPERTY_COLUMN_X , ((Col)mCB_X.getSelectedItem()).Name );
            p.setProperty( PROPERTY_COLUMN_Y , ((Col)mCB_Y.getSelectedItem()).Name );

            p.setProperty( PROPERTY_GROUP_CATEGORIES_X , Boolean.toString( mCBox_X_GroupCat.isSelected() ) );
            p.setProperty( PROPERTY_GROUP_CATEGORIES_Y , Boolean.toString( mCBox_Y_GroupCat.isSelected() ) );

            p.setProperty( PROPERTY_INVERT_X , Boolean.toString( mCBox_X_Invert.isSelected() ) );
            p.setProperty( PROPERTY_INVERT_Y, Boolean.toString( mCBox_Y_Invert.isSelected() ) );

            p.setProperty(PROPERTY_SHAPE,(String) mCB_Shape.getSelectedItem());

            p.setProperty(PROPERTY_STRETCH, ""+mSlider_Size.getValue() );
            p.setProperty(PROPERTY_ASPECT_RATIO, ""+mSlider_AspectRatio.getValue() );

            p.setProperty(PROPERTY_SELECTED_ONLY,""+mCBox_selectedOnly.isSelected());

            p.setProperty(PROPERTY_SPLIT_BY,""+((Col)mCB_SplitBy.getSelectedItem()).Name);

            return p;
        }


        /**
         * Sets checkboxes, sliders etc. enabled / disabled according to the current config.
         *
         */
        private void updateUIEnabledStatus() {
            int col_x = this.mCB_X.getModel().getElementAt( this.mCB_X.getSelectedIndex() ).Idx;
            boolean enable_cb_x_group =  mCTM.isColumnTypeCategory(col_x);
            this.mCBox_X_GroupCat.setEnabled(enable_cb_x_group);

            boolean enable_combobox_shape = mCB_Y.getSelectedIndex()==0;
            this.mCB_Shape.setEnabled(enable_combobox_shape);

            boolean is_2d_mode = this.mCB_Y.getSelectedIndex()!=0;
            if(is_2d_mode) {
                mCBox_Y_GroupCat.setSelected(true);
                mCBox_Y_GroupCat.setEnabled(false);
                mCBox_X_GroupCat.setSelected(true);
                mCBox_X_GroupCat.setEnabled(false);
            }
            else{
                mCBox_Y_GroupCat.setSelected(false);
                mCBox_Y_GroupCat.setEnabled(false);
                mCBox_Y_Invert.setEnabled(false);
            }
        }

        @Override
        public void setDialogConfiguration(Properties pp) {

            MyProperties p = new MyProperties(pp);

            int idx_x = mColumns_X.stream().map(xi -> xi.Name ).collect(Collectors.toList()).indexOf(p.getString(PROPERTY_COLUMN_X));
            int idx_y = mColumns_Y.stream().map(xi -> xi.Name ).collect(Collectors.toList()).indexOf(p.getString(PROPERTY_COLUMN_Y));
            int idx_s = mColumns_Split.stream().map(xi -> xi.Name ).collect(Collectors.toList()).indexOf(p.getString(PROPERTY_SPLIT_BY));

            mCB_X.setSelectedIndex(idx_x);
            mCB_Y.setSelectedIndex(idx_y);
            mCB_SplitBy.setSelectedIndex(idx_s);

            this.mCB_Shape.setSelectedItem( p.getProperty(PROPERTY_SHAPE) );

            this.mCBox_X_Invert.setSelected( Boolean.parseBoolean( p.getProperty(PROPERTY_INVERT_X) ) );
            this.mCBox_Y_Invert.setSelected( Boolean.parseBoolean( p.getProperty(PROPERTY_INVERT_Y) ) );

            this.mCBox_X_GroupCat.setSelected( Boolean.parseBoolean( p.getProperty(PROPERTY_GROUP_CATEGORIES_X) ) );
            this.mCBox_Y_GroupCat.setSelected( Boolean.parseBoolean( p.getProperty(PROPERTY_GROUP_CATEGORIES_Y) ) );

            this.mSlider_Size.setValue( Integer.parseInt( p.getProperty( PROPERTY_STRETCH ) ) );
            this.mSlider_AspectRatio.setValue( Integer.parseInt( p.getProperty( PROPERTY_ASPECT_RATIO ) ) );

            this.updateUIEnabledStatus();
//            if( p.getProperty(PROPERTY_COLUMN_Y).equals( VALUE_NO_COL_SELECTED ) ) {
//                mCB_Shape.setEnabled(true);
//            }
//            else{
//                mCB_Shape.setEnabled(false);
//            }
//            if(  ( ! mColumns_X.get( this.mCB_X.getSelectedIndex() ).isNumeric ) &&  (! mColumns_X.get( this.mCB_X.getSelectedIndex() ).isNumeric ) ){
//                this.mSlider_Size.setEnabled(false);
//                this.mSlider_AspectRatio.setEnabled(false);
//            }

        }


        @Override
        public void setDialogConfigurationToDefault() {

            this.mCB_X.setSelectedIndex(0);
            this.mCB_Y.setSelectedIndex(0);

            this.mCB_SplitBy.setSelectedIndex(0);

            this.mCB_Shape.setSelectedIndex(0);


            this.mSlider_Size.setValue(50);
            this.mSlider_AspectRatio.setValue(50);
        }


        /**
         * Sets the right things activated / deactivated
         *
         */
        public void validateDialogElements() {

            // to avoid stack overflows when we adjust the checkboxes that we listen to..
            mMyChangeListener.disableTemporariliy();

            if(this.mCB_Y.getSelectedIndex()!=0){
                this.mCB_Shape.setEnabled(false);
            }
            else{
                this.mCB_Shape.setEnabled(true);
            }
            updateUIEnabledStatus();

            mMyChangeListener.enable();
        }


        public class MyMultiChangeListener implements ActionListener , ChangeListener, ItemListener {

            private boolean mEnabled = true;

            public void disableTemporariliy(){
                this.mEnabled = false;
            }

            public void enable(){
                this.mEnabled = true;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if(!this.mEnabled){return;}

                validateDialogElements();
                positionCards(getDialogConfiguration());
            }
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(!this.mEnabled){return;}

                validateDialogElements();
                positionCards(getDialogConfiguration());
            }
            @Override
            public void stateChanged(ChangeEvent e) {
                if(!this.mEnabled){return;}

                validateDialogElements();
                positionCards(getDialogConfiguration());
            }
        }



//------------------------------ Implementation of logic ---------------------------------------------------------------


        public List<CardPaneModel.CardElementData> positionCards( Properties pp ) {

            MyProperties p = new MyProperties(pp);

            List<CardPaneModel.CardElementData> positions = new ArrayList<>();

            if(  p.getString(PROPERTY_COLUMN_Y).equals( VALUE_NO_COL_SELECTED )   ) {
                // 1d
                positions = positionCards_1D(p);
            }
            else{
                // 2d
                positions = positionCards_2D(p);
            }

            mFastCardPane.repaint();
            //mPreview.repaint();
            return positions;
        }

        public List<CardPaneModel.CardElementData> positionCards_1D(MyProperties p) {
            int col_split = mCTM.findColumn(p.getString(PROPERTY_SPLIT_BY));

            int col_x = mCTM.findColumn(p.getString(PROPERTY_COLUMN_X));
            //int col_y = mCTM.findColumn(p.getString(PROPERTY_COLUMN_Y));

            // 1. split by:
            List<Integer> all_records = new ArrayList<>();
            if(!p.getBool(PROPERTY_SELECTED_ONLY)) {
                for (int zi = 0; zi < mCTM.getTotalRowCount(); zi++) {
                    if (mCTM.isVisible(mCTM.getTotalRecord(zi))) {
                        all_records.add(zi);
                    }
                }
            }
            else{
                for (int zi = 0; zi < mCTM.getTotalRowCount(); zi++) {
                    if (mCTM.isSelected(mCTM.getTotalRecord(zi))) {
                        all_records.add(zi);
                    }
                }
            }


            List<List<Integer>> split_records = new ArrayList<>();
            if( ! p.getString( PROPERTY_SPLIT_BY ).equals(VALUE_NO_COL_SELECTED) ){
                Map<Integer,List<Integer>> sorted = sortIntoCategories(mCTM,col_split, all_records);
                for( int cati : sorted.keySet().stream().sorted().mapToInt( ki -> ki.intValue() ).toArray() ){
                    //for( sorted.get(cati) )
                    split_records.add( sorted.get(cati) );
                }
            }
            else
            {
                split_records.add( new ArrayList<>(all_records) );
            }


            // 2. determine "starting pos x" and "anchor y":
            // for x we also consider invisible cards
            // for y we take the mean of visible cards

            double start_x = mCardPane.getCardPaneModel().getAllElements().stream().mapToDouble( ei -> ei.getPosX() ).max().getAsDouble();
            double pos_y   = mCardPane.getCardPaneModel().getReducedAllElements().stream().mapToDouble( ei -> ei.getPosY() ).average().getAsDouble();


            // 3. Compute some basic alignment values
            // maybe make relative to card width..
            double cw = mCardPane.getCardDrawer().getCardDrawingConfig().getCardWidth();
            double ch = mCardPane.getCardDrawer().getCardDrawingConfig().getCardHeight();

            double gap_x = 6*cw;

            double pos_x = start_x + gap_x;

            // loop over splits:

            // before that, clear all visible cards from the card pane model..
            List<CardElement> ces_to_remove = mFastCardPane.getCardPaneModel().getReducedAllElements();

            //for( CardElement cei : ces_to_remove ){ mFastCardPane.getCardPaneModel().removeCE(cei); }
            for( CardElement cei : ces_to_remove ){
                mFastCardPane.getCardPaneModel().removeRecordsFromCE(cei,cei.getNonexcludedRecords());
            }

            for( List<Integer> list : split_records ) {


                boolean invert = p.getBool(PROPERTY_INVERT_X);
                list.sort( (Integer a , Integer b) -> (invert?-1:1) * Double.compare(  mCTM.getTotalDoubleAt(a,col_x) , mCTM.getTotalDoubleAt(b,col_x) )  );

                double shift_x_afterwards = 0.0;

                // !!! Can be empty, in case that cards are hidden such that this category is empty !!!
                if (! list.isEmpty()) {



                    // 1. Compute positions:

                    Map<Integer, Point2D> pos = null;

                    // number of pos depends on grouping or not:
                    // !! THIS DOES NOT WORK, AS WE MAY NOT HAVE ALL CATEGORIES WITH HIDDEN CARDS.. !!
                    // --> WE HAVE TO REALLY COUNT NUMBER OF CATEGORIES IN THAT CASE
                    //int nPos = (p.getBool(PROPERTY_GROUP_CATEGORIES_X) ? mCTM.getCategoryCount(col_x) : list.size());
                    int nPos = -1;

                    if(p.getBool(PROPERTY_GROUP_CATEGORIES_X)){
                        nPos = (int) list.stream().map( ci -> mCTM.getTotalValueAt( ci , col_x) ).distinct().count();
                    }
                    else{
                        nPos = list.size();
                    }

                    if (p.getString(PROPERTY_SHAPE).equals(VALUES_SHAPE[0])) {
                        // grid:
                        pos = computeGrid(nPos, cw, ch);
                    }
                    if (p.getString(PROPERTY_SHAPE).equals(VALUES_SHAPE[1])) {
                        // linear:
                        pos = computeLinear(nPos, cw, ch);
                    }
                    if (p.getString(PROPERTY_SHAPE).equals(VALUES_SHAPE[2])) {
                        // spiral:
                        pos = computeSpiral(nPos, cw, ch);
                    }
                    if (p.getString(PROPERTY_SHAPE).equals(VALUES_SHAPE[3])) {
                        // circle:
                        pos = computeCircle(nPos, cw, ch);
                    }


                    // 2. stretch / change aspect ratio..
                    // we can assume zero/zero is center
                    double stretch = Math.exp( (p.getInt(PROPERTY_STRETCH)-50.0)/25.0 );
                    double aspectratio = Math.exp( (p.getInt(PROPERTY_ASPECT_RATIO)-50.0)/25.0 );
                    double stretchX = stretch * aspectratio; double stretchY = stretch / aspectratio;

                    Map<Integer,Point2D> new_pos = new HashMap<>();
                    for( Integer ki : pos.keySet() ){
                        Point2D p_old = pos.get(ki);
                        new_pos.put( ki , new Point2D.Double( p_old.getX() * stretchX , p_old.getY() * stretchY ) );
                    }
                    pos = new_pos;




                    // 3. put right to pos_x
                    double x_pos_min = pos.values().stream().mapToDouble(pi -> pi.getX()).min().getAsDouble();
                    double x_pos_max = pos.values().stream().mapToDouble(pi -> pi.getX()).max().getAsDouble();

                    double shift_this = -x_pos_min;
                    shift_x_afterwards = x_pos_max - x_pos_min;

                    if (p.getBool(PROPERTY_GROUP_CATEGORIES_X)) {
                        // AFTER SORTINTOCATEGORIES WE HAVE TO REESTABLISH THE INVERT!!
                        Map<Integer, List<Integer>> sorted = sortIntoCategories(mCTM, col_x, list);

                        if(p.getBool(PROPERTY_INVERT_X)){
                            for (int zi = 0; zi < nPos; zi++) {
                                mFastCardPane.getCardPaneModel().addCE(getCRs(mCTM, sorted.get(nPos-1-zi)), pos_x + shift_this + pos.get(zi).getX(), pos.get(zi).getY());
                            }
                        }
                        else {
                            for (int zi = 0; zi < nPos; zi++) {
                                mFastCardPane.getCardPaneModel().addCE(getCRs(mCTM, sorted.get(zi)), pos_x + shift_this + pos.get(zi).getX(), pos.get(zi).getY());
                            }
                        }
                    } else {
                        for (int zi = 0; zi < nPos; zi++) {
                            ArrayList<CompoundRecord> crlist = new ArrayList<>();
                            crlist.add(mCTM.getTotalRecord(list.get(zi)));
                            // NOTE: position is INDEED at zi, not at list.get(zi) !
                            mFastCardPane.getCardPaneModel().addCE(crlist, pos_x + shift_this + pos.get(zi).getX(), pos.get(zi).getY());
                        }
                    }
                }

                // 4. shift
                pos_x += shift_x_afterwards + gap_x;
                System.out.println("PosX: "+pos_x+" added card elements");
            }

            return mFastCardPane.getCardPaneModel().serializeToCardElementData();

        }


        public Map<Integer,Point2D> computeCircle( int n , double  cw , double  ch ) {

            Map<Integer,Point2D> circle_points = new HashMap<>();
            double defaultDistance = ch;
            double radius = (defaultDistance * n) / (2* Math.PI) ;
            for(int zi=0;zi<n;zi++) {
                // we start at top, i.e. at 90 degree into the unit circle, then we go clockwise, i.e. we subtract rad
                double rad = (Math.PI / 2 ) -  (1.0*zi/(1.0*n)) * 2 * Math.PI ;
                 circle_points.put( zi , new Point2D.Double( radius * Math.cos(rad) , radius * Math.sin(rad) ) );
            }

            return circle_points;
        }

        public Map<Integer,Point2D> computeGrid( int n , double cw , double ch ) {

            // define gap
            double gap = 0.2 * cw;

            Map<Integer,Point2D> grid_points = new HashMap<>();

            // we loop over all possible rectangles and take the one closest to 4/3:
            double target_ratio = 4.0/3.0;

            int n_best = 100; double ratio_best=Double.MAX_VALUE;
            for(int zi=1;zi<n;zi++){
                int y = n / zi;
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

        public Map<Integer,Point2D> computeLinear( int n , double cw , double ch ) {

            // define gap
            double gap = 0.2 * cw;

            Map<Integer,Point2D> grid_points = new HashMap<>();

            // position cards starting at zero.. first place just place the square, afterward make it zero centered..
            double x = 0; double y = 0;
            for( int zi=0;zi<n;zi++ ){
                grid_points.put( zi , new Point2D.Double( x , y ) );
                x += cw + gap;
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

        public Map<Integer,Point2D> computeSpiral( int n , double cw , double ch ) {

            // Optimal spiraling out we say is at 1.25 times Max(cw,ch):
            double optSpirOut = 1.25 * Math.max(cw,ch);

            // Optimal "spread" is at 1.5 times the distance from the center to the corner of the card (this should fit roughtly..)
            double optSpread  = (Math.sqrt( cw*cw + ch*ch )*0.5) * 1.5;

//            // and add "tweaking" via parameters:
//            double finalSpread  = this.mCardSpread   * optSpread;
//            double finalSpirOut = this.mSpiralSpread * optSpirOut;
            double finalSpirOut = optSpirOut;
            double finalSpread  = optSpread;

            // spiral them..
            // we always compute the rad to rotate based on OptSpread and the currentRadius.
            // then we cotinuously increase the currentRadius.

            // we need some kind of reasonable initial radius
            double currentRadius   = 0.5*Math.max(cw,ch);
            double currentRotation = 0.0;

            //double px = 0; double py = 0;
            //List<Point2D> positions = new ArrayList<>();
            Map<Integer,Point2D> spiral_points = new HashMap<>();

            for(int zi=0;zi<n;zi++){
                double radToRotate = finalSpread / currentRadius; // 2 Pi cancel out

                currentRotation += radToRotate;
                currentRotation = currentRotation % ( 2 * Math.PI ) ; // this does work(?)
                spiral_points.put( zi , new Point2D.Double( Math.sin(currentRotation) * currentRadius , Math.cos(currentRotation) * currentRadius ) );

                currentRadius +=  (radToRotate / (2*Math.PI) ) * finalSpirOut ;
            }

            return spiral_points;
        }



        public List<CardPaneModel.CardElementData> positionCards_2D(MyProperties p) {

            int col_split = mCTM.findColumn(p.getString(PROPERTY_SPLIT_BY));

            int col_x = mCTM.findColumn(p.getString(PROPERTY_COLUMN_X));
            int col_y = mCTM.findColumn(p.getString(PROPERTY_COLUMN_Y));

            // 1. split by:

            List<Integer> all_records = new ArrayList<>();
            if(!p.getBool(PROPERTY_SELECTED_ONLY)) {
                for (int zi = 0; zi < mCTM.getTotalRowCount(); zi++) {
                    if (mCTM.isVisible(mCTM.getTotalRecord(zi))) {
                        all_records.add(zi);
                    }
                }
            }
            else{
                for (int zi = 0; zi < mCTM.getTotalRowCount(); zi++) {
                    if (mCTM.isSelected(mCTM.getTotalRecord(zi))) {
                        all_records.add(zi);
                    }
                }
            }


            List<List<Integer>> split_records = new ArrayList<>();
            if( ! p.getString( PROPERTY_SPLIT_BY ).equals(VALUE_NO_COL_SELECTED) ){
                Map<Integer,List<Integer>> sorted = sortIntoCategories(mCTM,col_split, all_records);
                for( int cati : sorted.keySet().stream().sorted().mapToInt( ki -> ki.intValue() ).toArray() ){
                    //for( sorted.get(cati) )
                    split_records.add( sorted.get(cati) );
                }
            }
            else
            {
                split_records.add( new ArrayList<>(all_records) );
            }


            // 2.1 determine "starting pos x" and "anchor y":
            // for x we also consider invisible cards
            // for y we take the mean of visible cards

            double start_x = mCardPane.getCardPaneModel().getAllElements().stream().mapToDouble( ei -> ei.getPosX() ).max().getAsDouble();
            double pos_y   = mCardPane.getCardPaneModel().getReducedAllElements().stream().mapToDouble( ei -> ei.getPosY() ).average().getAsDouble();



            // 2.2 stretch / change aspect ratio..
            double stretch = Math.exp( (p.getInt(PROPERTY_STRETCH)-50.0)/25.0 );
            double aspectratio = Math.exp( (p.getInt(PROPERTY_ASPECT_RATIO)-50.0)/25.0 );
            double stretchX = stretch * aspectratio; double stretchY = stretch / aspectratio;



            // 3. Compute some basic alignment values
            // maybe make relative to card width..
            double cw = mCardPane.getCardDrawer().getCardDrawingConfig().getCardWidth();
            double ch = mCardPane.getCardDrawer().getCardDrawingConfig().getCardHeight();

            // Here we should scale the gap also with the stretching options (is done when it is applied..)
            double gap_x = ( 6*cw ) * stretch;

            double pos_x = start_x + gap_x;

            // loop over splits:

            // before that, clear all visible cards from the card pane model..
            // NOTE! This list of card elements is only a proxy for the actual compound records that we
            // have to remove, as we remove only the non-excluded (i.e. visible) ones.
            List<CardElement> ces_to_remove = mFastCardPane.getCardPaneModel().getReducedAllElements();
            //for( CardElement cei : ces_to_remove ){ mFastCardPane.getCardPaneModel().removeCE(cei); }

            List<CompoundRecord> crs_to_add = new ArrayList<>();
            for( CardElement cei : ces_to_remove ){
                //mFastCardPane.getCardPaneModel().removeCE(cei);
                //crs_to_add.addAll(cei.getAllNonexcludedRecords());
                mFastCardPane.getCardPaneModel().removeRecordsFromCE(cei,cei.getAllNonexcludedRecords());
            }

            for( List<Integer> list : split_records ) {

                double shift_x_afterwards = 0.0;

                // Two special cases that we have to cover:
                // 1. !!! Can be empty, in case that cards are hidden such that this category is empty !!!
                // 2. Elements can have all the same x (or y) values, then the scattering does not work.
                if (!list.isEmpty()) {

                    // 2. default size is:
                    int ncards = list.size();
                    double sizeDefault = Math.sqrt(ncards) * ch;


                    double size_x = sizeDefault * stretchX;
                    double size_y = sizeDefault * stretchY;


                    if (  ! p.getBool(PROPERTY_GROUP_CATEGORIES_X)  ) {

                        double values_x[] = list.stream().mapToDouble( li -> mCTM.getTotalDoubleAt(li,col_x) ).toArray();
                        double values_y[] = list.stream().mapToDouble( li -> mCTM.getTotalDoubleAt(li,col_y) ).toArray();

                        // replace missing values by zero :
                        for(int zi=0;zi<values_x.length;zi++){ if( Double.isNaN(values_x[zi])){values_x[zi]=0;} }
                        for(int zi=0;zi<values_y.length;zi++){ if( Double.isNaN(values_y[zi])){values_y[zi]=0;} }


                        DoubleSummaryStatistics dss_x = Arrays.stream(values_x).summaryStatistics();
                        DoubleSummaryStatistics dss_y = Arrays.stream(values_y).summaryStatistics();

                        double xmin = dss_x.getMin(); double xmax = dss_x.getMax();
                        double ymin = dss_x.getMin(); double ymax = dss_y.getMax();

                        // cover the case that all are the same value. Here we can just pick any range around the value..
                        if(xmin==xmax){ xmin-=0.2 ; xmax+= 0.2; }
                        if(ymin==ymax){ ymin-=0.2 ; ymax+= 0.2; }

                        Map<Integer,Point2D.Double> pos = new HashMap<>();
                        for( int zi=0;zi<values_x.length;zi++){
                            double px = -0.5*size_x + size_x * ( (values_x[zi]-xmin)  / ( xmax-xmin )  );
                            double py = -0.5*size_y + size_y * ( (values_y[zi]-ymin)  / ( ymax-ymin )  );
                            pos.put( zi , new Point2D.Double( px,py ) );
                        }


                        // 3. put right to pos_x
                        double x_pos_min = pos.values().stream().mapToDouble(pi -> pi.getX()).min().getAsDouble();
                        double x_pos_max = pos.values().stream().mapToDouble(pi -> pi.getX()).max().getAsDouble();

                        double shift_this = -x_pos_min;
                        shift_x_afterwards = x_pos_max - x_pos_min;

                        for (int zi = 0; zi < values_x.length; zi++) {
                            List<Integer> el_i = new ArrayList<>(); el_i.add(list.get(zi));
                            mFastCardPane.getCardPaneModel().addCE(getCRs(mCTM,el_i), pos_x + shift_this + pos.get( zi ).getX(), pos.get(zi).getY());
                        }
                    }
                    else {


                        Map<List<Double>,List<Integer>> sorted_cards = new HashMap<>();

                        double values_x[] = list.stream().mapToDouble( li ->  mCTM.getTotalDoubleAt(li,col_x) ).toArray();
                        double values_y[] = list.stream().mapToDouble( li ->  mCTM.getTotalDoubleAt(li,col_y) ).toArray();

                        for( int zi=0;zi<values_x.length;zi++ ){
                            //IntOrderedPair iop = new IntOrderedPair( values_x[zi] , values_y[zi] );
                            List<Double> two_doubles = new ArrayList<>();
                            two_doubles.add(values_x[zi]); two_doubles.add(values_y[zi]);
                            if(!sorted_cards.containsKey(two_doubles)){ sorted_cards.put( two_doubles,new ArrayList<Integer>() ); }
                            sorted_cards.get(two_doubles).add( list.get(zi) );
                        }

                        DoubleSummaryStatistics iss_x = Arrays.stream(values_x).summaryStatistics();
                        DoubleSummaryStatistics iss_y = Arrays.stream(values_y).summaryStatistics();

                        double xmin = iss_x.getMin(); double xmax = iss_x.getMax();
                        double ymin = iss_x.getMin(); double ymax = iss_y.getMax();

                        // cover the case that all are the same value. Here we can just pick any range around the value..
                        if(xmin>=xmax){ xmin-=0.2 ; xmax+= 0.2; }
                        if(ymin>=ymax){ ymin-=0.2 ; ymax+= 0.2; }

                        Map<List<Double>,Point2D.Double> pos = new HashMap<>();
                        for( List<Double> iopi : sorted_cards.keySet() ){
                            double px = -0.5*size_x + size_x * ( (iopi.get(0)-xmin)  / ( xmax-xmin )  );
                            double py = -0.5*size_y + size_y * ( (iopi.get(1)-ymin)  / ( ymax-ymin )  );
                            pos.put( iopi , new Point2D.Double( px,py ) );
                        }

                        // 3. put right to pos_x
                        double x_pos_min = pos.values().stream().mapToDouble(pi -> pi.getX()).min().getAsDouble();
                        double x_pos_max = pos.values().stream().mapToDouble(pi -> pi.getX()).max().getAsDouble();

                        double shift_this = -x_pos_min;
                        shift_x_afterwards = x_pos_max - x_pos_min;

                        for( List<Double> iopi : sorted_cards.keySet() ){
                            List<Integer> el_i = sorted_cards.get(iopi);
                            mFastCardPane.getCardPaneModel().addCE(getCRs(mCTM,el_i), pos_x + shift_this + pos.get( iopi ).getX(), pos.get(iopi).getY());
                        }



//                        Map<IntOrderedPair,List<Integer>> sorted_cards = new HashMap<>();
//
//                        int values_x[] = list.stream().mapToInt( li -> (int) mCTM.getTotalDoubleAt(li,col_x) ).toArray();
//                        int values_y[] = list.stream().mapToInt( li -> (int) mCTM.getTotalDoubleAt(li,col_y) ).toArray();
//
//                        for( int zi=0;zi<values_x.length;zi++ ){
//                            IntOrderedPair iop = new IntOrderedPair( values_x[zi] , values_y[zi] );
//                            if(!sorted_cards.containsKey(iop)){ sorted_cards.put( iop,new ArrayList<Integer>() ); }
//                            sorted_cards.get(iop).add(zi);
//                        }
//
//                        IntSummaryStatistics iss_x = Arrays.stream(values_x).summaryStatistics();
//                        IntSummaryStatistics iss_y = Arrays.stream(values_y).summaryStatistics();
//
//                        double xmin = iss_x.getMin(); double xmax = iss_x.getMax();
//                        double ymin = iss_x.getMin(); double ymax = iss_y.getMax();
//
//                        // cover the case that all are the same value. Here we can just pick any range around the value..
//                        if(xmin==xmax){ xmin-=0.2 ; xmax+= 0.2; }
//                        if(ymin==ymax){ ymin-=0.2 ; ymax+= 0.2; }
//
//                        Map<IntOrderedPair,Point2D.Double> pos = new HashMap<>();
//                        for( IntOrderedPair iopi : sorted_cards.keySet() ){
//                            double px = -0.5*size_x + size_x * ( (iopi.A-xmin)  / ( xmax-xmin )  );
//                            double py = -0.5*size_y + size_y * ( (iopi.B-ymin)  / ( ymax-ymin )  );
//                            pos.put( iopi , new Point2D.Double( px,py ) );
//                        }
//
//
//                        // 3. put right to pos_x
//                        double x_pos_min = pos.values().stream().mapToDouble(pi -> pi.getX()).min().getAsDouble();
//                        double x_pos_max = pos.values().stream().mapToDouble(pi -> pi.getX()).max().getAsDouble();
//
//                        double shift_this = -x_pos_min;
//                        shift_x_afterwards = x_pos_max - x_pos_min;
//
//                        for( IntOrderedPair iopi : sorted_cards.keySet() ){
//                            List<Integer> el_i = sorted_cards.get(iopi);
//                            mFastCardPane.getCardPaneModel().addCE(getCRs(mCTM,el_i), pos_x + shift_this + pos.get( iopi ).getX(), pos.get(iopi).getY());
//                        }
                    }
                }

                // 4. shift
                pos_x += shift_x_afterwards + gap_x;
                System.out.println("PosX: "+pos_x+" added card elements");

            }

            return mFastCardPane.getCardPaneModel().serializeToCardElementData();
        }






    }

    public static List<CompoundRecord> getCRs(CompoundTableModel ctm, List<Integer> ids) {
        return ids.stream().map( ri -> ctm.getTotalRecord(ri) ).collect(Collectors.toList());
    }


    public static Map<Integer,List<Integer>> sortIntoCategories(CompoundTableModel ctm, int col, List<Integer> ids ) {
        Map<Integer,List<Integer>> sorted = new HashMap<>();
        for( int zi=0; zi<ctm.getCategoryCount(col);zi++ ){ sorted.put(zi,new ArrayList<Integer>()); }
        for(int ri : ids) {
            sorted.get( ctm.getCategoryIndex(col, ctm.getTotalRecord(ri))).add(ri);
        }

        // !! SHRINK FOR THE CASE THAT WE DO NOT FIND ALL CATEGORIES!!!
        Map<Integer,List<Integer>> sorted_compact = new HashMap<>();
        int compact_counter = 0;
        for( int zi=0;zi<ctm.getCategoryCount(col);zi++) {
            if(sorted.get(zi).isEmpty()){

            }
            else{
                sorted_compact.put(compact_counter,sorted.get(zi));
                compact_counter++;
            }
        }

        return sorted_compact;
    }




































}
