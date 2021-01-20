package com.actelion.research.table.view.card.cardsurface.gui;

import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.card.tools.ColWithType;
import com.actelion.research.table.view.card.tools.IntOrderedPair;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.List;

public class JMyOptionsTable extends JComponent {


    // @TODO: add color etc..
    public static enum Option { SLIDER , COMBOBOX , CHECKBOX };

//    int mSizeX = 0;
//    int mSizeY = 0;

//    public JMyOptionsTable(int sizeX , int sizeY){
//        mSizeX = sizeX;
//                mSizeY = sizeY;

    public JMyOptionsTable(){
    }


    //private boolean mMaxSizeSetToPreferredSize = true;

    /**
     *
     * It is debatalble whether this override is a good idea..
     *
     * @return
     */
    @Override
    public Dimension getMaximumSize(){
        return this.getPreferredSize();
    }


    /**
     * Resets everything..
     */
    public void reset(){
        this.mTableListeners.clear();
        this.removeAll();
        this.mOptions.clear();
    }

    /**
     * Returns max x coordinate of component..
     */
    public int getSizeX(){
        if(mOptions.keySet().size()==0){return 0;}
        return mOptions.keySet().stream().mapToInt( ki -> ki.A ).max().getAsInt() + 1;
    }

    /**
     * Returns max y coordinate of component..
     */
    public int getSizeY(){
        if(mOptions.keySet().size()==0){return 0;}
        return mOptions.keySet().stream().mapToInt( ki -> ki.B ).max().getAsInt() + 1;
    }

    /**
     * Checks if option is activated
     *
     * @param x
     * @param y
     * @return
     */
    public boolean isActivated(int x, int y){
        return mOptions.get(new IntOrderedPair(x,y)).isActivated();
    }


    TableLayout mLayout = null;

    int mPaddingValue = HiDPIHelper.scale(8);

    /**
     * Reinitializes everything
     */
    public void reinit(){

        List<Double> columns = new ArrayList<>();
        List<Double> rows    = new ArrayList<>();

        columns.add( (double) mPaddingValue );
        for(int zi=0;zi<getSizeX();zi++){
            columns.add( TableLayout.PREFERRED );
            columns.add((double) mPaddingValue );
        }

        rows.add( (double) mPaddingValue );
        for(int zi=0;zi<getSizeY();zi++){
            rows.add( TableLayout.PREFERRED );
            rows.add((double) mPaddingValue );
        }

        double layoutArray[][] = new double[2][];
        layoutArray[0] =  columns.stream().mapToDouble( di -> di.doubleValue() ).toArray();
        layoutArray[1] =  rows.stream().mapToDouble( di -> di.doubleValue() ).toArray();

        mLayout = new TableLayout(layoutArray);
        this.setLayout(mLayout);

        // init all elements..
        for( int zx=0;zx<getSizeX();zx++ ){
            for( int zy=0;zy<getSizeY();zy++ ){
                if(this.mOptions.containsKey( new IntOrderedPair(zx,zy) )){
                    JComponent newComponent = mOptions.get(new IntOrderedPair(zx,zy)).getPanel();
                    this.add( newComponent , ""+(1+2*zx) + " , "+(1+2*zy) );
                }
            }
        }
    }

    public void setSlider(int x, int y, String propertyName , String name , double a , double b , double value){
        this.mOptions.put( new IntOrderedPair(x,y) , new MySlider( Option.SLIDER , name ,x ,y, a , b , value) );
        this.setPropertyName(new IntOrderedPair(x,y) , propertyName);
        //this.mPropertyNames.put( new IntOrderedPair(x,y) , propertyName );
    }

    public void setComboBox(int x, int y, String propertyName ,  String name, List<String> strings , String selectedValue){
        this.mOptions.put( new IntOrderedPair(x,y) , new MyComboBox( Option.COMBOBOX , name , x,y, strings , selectedValue ) );
        this.setPropertyName( new IntOrderedPair(x,y) , propertyName );
        //this.mPropertyNames.put( new IntOrderedPair(x,y) , propertyName );
    }

    public void setCheckBox(int x, int y, String propertyName, String name, boolean defaultValue){
        this.mOptions.put( new IntOrderedPair(x,y) , new MyCheckBox( Option.CHECKBOX , name , x,y, defaultValue ) );
        this.setPropertyName( new IntOrderedPair(x,y) , propertyName );
    }

    /**
     * Automatically creates a combo box for the double and categorical columns.
     *
     * @param x
     * @param y
     * @param propertyName
     * @param name
     * @param model
     */
    public void setComboBoxDoubleAndCategoricalColumns(int x, int y, String propertyName, String name , CompoundTableModel model){
        //JComboBoxForTableColumns tableColumns = new MyComboBox()  JComboBoxForTableColumns(labelMap);
        this.mOptions.put( new IntOrderedPair(x,y) , new MyComboBoxForTableColumns( Option.COMBOBOX , name , x,y, model ) );
        this.setPropertyName( new IntOrderedPair(x,y) , propertyName );
    }

    /**
     * Automatically creates a combo box for the double and categorical columns.
     * Difference to other variant is, that one can pick the interpretation of the columns.
     *
     * @param x
     * @param y
     * @param propertyName
     * @param name
     * @param model
     */
    public void setComboBoxDoubleAndCategoricalColumns2(int x, int y, String propertyName, String name , CompoundTableModel model){
        //JComboBoxForTableColumns tableColumns = new MyComboBox()  JComboBoxForTableColumns(labelMap);
        this.mOptions.put( new IntOrderedPair(x,y) , new MyComboBoxForTableColumns2( Option.COMBOBOX , name , x,y, model ) );
        this.setPropertyName( new IntOrderedPair(x,y) , propertyName );
    }


    private HashMap<IntOrderedPair,MyOption> mOptions        = new HashMap<>();

    private HashMap<IntOrderedPair,String>   mPropertyNames     = new HashMap<>();
    private HashMap<String,IntOrderedPair>   mPropertyNamesInv  = new HashMap<>();

    public void setPropertyName(IntOrderedPair pos, String name){
        this.mPropertyNames.put(pos,name);
        this.mPropertyNamesInv.put(name,pos);
    }

    //private HashMap<String,MyOption>         mOptionNames  = new HashMap<>();
    public void deactivate(int x, int y){ mOptions.get(new IntOrderedPair(x, y)).deactivate(); }
    public void activate(int x, int y){ mOptions.get(new IntOrderedPair(x, y)).activate(); }

    public void setValue(int x, int y, Object value){
        MyOption opt      = mOptions.get( new IntOrderedPair(x, y) );
        JComponent opt_c  = mOptions.get( new IntOrderedPair(x, y) ).getPanel();
        opt.setValue( value );
    }

    public void setValue( String propertyName , Object value ){
        IntOrderedPair pos = this.mPropertyNamesInv.get(propertyName);
        this.setValue(pos.A,pos.B,value);
    }

    public abstract static class MyOption {
        Option mOption;
        String mLabel;

        boolean mActivated;

        int mX; int mY;

        /**
         * The value to which this option is set.
         */
        String mValue = "";

        public MyOption(Option option, String label , int x, int y){
            this.mOption = option;
            this.mLabel  = label;

            this.mX = x;
            this.mY = y;

            this.mActivated = true;
        }

        protected void setOptionValue(String newValue){
            this.mValue = newValue;
        }

        public String getOptionValue(){
            return this.mValue;
        }

        public abstract JPanel getPanel();

        /**
         * Sets the control governed by this MyOption object to the given value.
         *
         * @param value
         */
        public abstract void setValue( Object value );

        /**
         * Sets the component to the given value
         *
         * @param c A JComponent that was created by the given MyOption object.
         * @param value The value to set.
         */
        public abstract void setValue( JComponent c , Object value );

        public abstract boolean isActivated();
        public abstract void activate();
        public abstract void deactivate();

    }

    public class MySlider extends MyOption {
        double     mA, mB;
        int        mTicks;

        JPanel     mPanel  = null;
        JSlider    mSlider = null;


        public MySlider(Option option, String label, int x, int y, double a, double b, double value) {
            this(option,label,x,y,a,b,value,100);
        }

        public MySlider(Option option, String label, int x, int y, double a, double b, double value , int ticks)
        {
            super(option,label,x,y);
            mA = a;
            mB = b;
            mTicks = ticks;
            this.initPanel();
            this.setValue(mSlider,value);
        }

        private void initPanel(){
            mSlider = new JSlider(0, mTicks + 1);
            if (!this.mActivated) {
                mSlider.setEnabled(false);
            }
            mSlider.addChangeListener(new MyChangeListener_JSlider());

            mPanel = new JPanel();
            mPanel.setLayout(new BorderLayout());
            mPanel.add(new JLabel(this.mLabel), BorderLayout.WEST);
            mPanel.add(mSlider, BorderLayout.EAST);
        }

        public JPanel getPanel(){
            return mPanel;
        }

        public MySlider getThis(){ return this; }

        @Override
        public void setValue(Object value) {
            this.setValue( this.mSlider , value );
        }

        @Override
        public void setValue(JComponent c, Object value) {
            JSlider slider = (JSlider) c;
            double  vd     = ((Double)value).doubleValue();
            // determine tick:
            int tick = (int)( (vd-mA) / (mB-mA) * mTicks );
            slider.setValue(tick);
            this.setOptionValue(""+value);
        }

        public boolean isActivated(){return mActivated;}
        public void activate(){this.mActivated=true;this.mSlider.setEnabled(true);}
        public void deactivate(){this.mActivated=false;;this.mSlider.setEnabled(false);}

        class MyChangeListener_JSlider implements ChangeListener {

            public MyChangeListener_JSlider(){
                //this.mX = x; this.mY = y,
            }

            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider slider = (JSlider) e.getSource();
                //MySlider sliderOption = (MySlider) mOptions.get(new IntOrderedPair(mX, mY));

                //double value = sliderOption.mA + (sliderOption.mB-sliderOption.mA) * (slider.getValue() / (sliderOption.mTicks) );
                double value = mA + (mB-mA) * ( (1.0*slider.getValue()) / (mTicks) );
                setOptionValue( ""+value );

                if( !( slider ).getValueIsAdjusting() ){
                    fireOptionTableEvent( new OptionTableEvent( getThis() , slider , e , false ) );
                }
                else{
                    fireOptionTableEvent( new OptionTableEvent( getThis() , slider , e , true ) );
                }
            }
        }
    }


    public class MyComboBoxForTableColumns extends MyOption {

        JPanel     mPanel                    = null;
        JComboBoxForTableColumns  mComboBox  = null;

        CompoundTableModel mModel = null;

        public MyComboBoxForTableColumns(Option option, String label, int x, int y, CompoundTableModel model)
        {
            super(option,label,x,y);
            mModel = model;

            initPanel();
            this.setValue(mComboBox, mComboBox.getItemAt(0) );
        }

        public JPanel getPanel(){ return mPanel; }

        public void initPanel(){
            //JComboBox<String> combobox = new JComboBox<>();
            mComboBox = JComboBoxForTableColumns.createComboBoxDoubleAndCategorical(mModel);
            //mStrings.stream().forEachOrdered( si -> mComboBox.addItem(si) );

            mPanel = new JPanel();
            mPanel.setLayout(new BorderLayout());
            //panel.add( new JLabel(this.mLabel) , BorderLayout.WEST );
            mPanel.add( new JLabel(this.mLabel) , BorderLayout.WEST );
            mPanel.add( mComboBox , BorderLayout.CENTER );
            mComboBox.addItemListener( new MyComboBoxForTableColumns.MyItemListener_JComboBox() );
            if(!this.mActivated){
                mComboBox.setEnabled(false);
            }
        }

        @Override
        public void setValue(Object value) {
            this.setValue( this.mComboBox , value );
        }

        @Override
        public void setValue(JComponent c, Object value) {
            JComboBox cb  = (JComboBox) c;
            //String    vs  = (String) value;
            cb.setSelectedItem(value);
            //this.setValue(value.toString());
            this.setOptionValue(value.toString());
        }

        public boolean isActivated(){return mActivated;}
        public void activate(){this.mActivated=true;this.mComboBox.setEnabled(true);}
        public void deactivate(){this.mActivated=false;;this.mComboBox.setEnabled(false);}

        public MyComboBoxForTableColumns getThis(){ return this; }

        class MyItemListener_JComboBox implements ItemListener {
            int mX; int mY;

            public MyItemListener_JComboBox(){
            }

            @Override
            public void itemStateChanged(ItemEvent e) {
                JComboBoxForTableColumns combobox = (JComboBoxForTableColumns) e.getSource();

                Integer item = (Integer) combobox.getSelectedItem();
                setOptionValue(item.toString());
                fireOptionTableEvent(  new OptionTableEvent( getThis() , combobox, e , false ) );
            }
        }

    }


    public class MyComboBoxForTableColumns2 extends MyOption {

        JPanel     mPanel                     = null;
        JComboBoxForTableColumns2  mComboBox  = null;

        CompoundTableModel mModel = null;

        public MyComboBoxForTableColumns2(Option option, String label, int x, int y, CompoundTableModel model)
        {
            super(option,label,x,y);
            mModel = model;

            initPanel();
            this.setValue(mComboBox, mComboBox.getItemAt(0) );
        }

        public JPanel getPanel(){ return mPanel; }

        public void initPanel(){
            //JComboBox<String> combobox = new JComboBox<>();
            //mComboBox = JComboBoxForTableColumns2.createComboBoxDoubleAndCategorical(mModel);
            mComboBox = JComboBoxForTableColumns2.createComboBoxForAllClasses(mModel);
            //mStrings.stream().forEachOrdered( si -> mComboBox.addItem(si) );

            mPanel = new JPanel();
            mPanel.setLayout(new BorderLayout());
            //panel.add( new JLabel(this.mLabel) , BorderLayout.WEST );
            mPanel.add( new JLabel(this.mLabel) , BorderLayout.WEST );
            mPanel.add( mComboBox , BorderLayout.CENTER );
            mComboBox.addItemListener( new MyComboBoxForTableColumns2.MyItemListener_JComboBox() );
            if(!this.mActivated){
                mComboBox.setEnabled(false);
            }
        }

        @Override
        public void setValue(Object value) {
            this.setValue( this.mComboBox , value );
        }

        @Override
        public void setValue(JComponent c, Object value) {
            JComboBox cb  = (JComboBox) c;
            //String    vs  = (String) value;
            cb.setSelectedItem(value);
            //this.setValue(value.toString());
            this.setOptionValue( ((ColWithType)value).serializeToString());
        }

        public boolean isActivated(){return mActivated;}
        public void activate(){this.mActivated=true;this.mComboBox.setEnabled(true);}
        public void deactivate(){this.mActivated=false;;this.mComboBox.setEnabled(false);}

        public MyComboBoxForTableColumns2 getThis(){ return this; }

        class MyItemListener_JComboBox implements ItemListener {
            int mX; int mY;

            public MyItemListener_JComboBox(){
            }

            @Override
            public void itemStateChanged(ItemEvent e) {
                JComboBoxForTableColumns2 combobox = (JComboBoxForTableColumns2) e.getSource();

                //Integer item = (Integer) combobox.getSelectedItem();
                ColWithType item = (ColWithType) combobox.getSelectedItem();
                setOptionValue(item.serializeToString());
                fireOptionTableEvent(  new OptionTableEvent( getThis() , combobox, e , false ) );
            }
        }

    }



    public class MyComboBox extends MyOption {

        List<String> mStrings;

        JPanel     mPanel     = null;
        JComboBox  mComboBox  = null;

        public MyComboBox(Option option, String label, int x, int y, List<String> strings, String value)
        {
            super(option,label,x,y);
            mStrings = strings;
            initPanel();
            this.setValue(mComboBox,value);
        }

        public JPanel getPanel(){ return mPanel; }

        public void initPanel(){
            //JComboBox<String> combobox = new JComboBox<>();
            mComboBox = new JComboBox<>();
            mStrings.stream().forEachOrdered( si -> mComboBox.addItem(si) );
            mPanel = new JPanel();
            mPanel.setLayout(new BorderLayout());

            mPanel.add( new JLabel(this.mLabel) , BorderLayout.WEST );
            mPanel.add( mComboBox , BorderLayout.CENTER );
            mComboBox.addItemListener( new MyItemListener_JComboBox() );
            if(!this.mActivated){
                mComboBox.setEnabled(false);
            }
        }

        @Override
        public void setValue(Object value) {
            this.setValue( this.mComboBox , value );
        }

        @Override
        public void setValue(JComponent c, Object value) {
            JComboBox cb  = (JComboBox) c;
            String    vs  = (String) value;
            cb.setSelectedItem(vs);
            this.setOptionValue(vs);
        }

        public boolean isActivated(){return mActivated;}
        public void activate(){this.mActivated=true;this.mComboBox.setEnabled(true);}
        public void deactivate(){this.mActivated=false;;this.mComboBox.setEnabled(false);}

        public MyComboBox getThis(){ return this; }

        class MyItemListener_JComboBox implements ItemListener {

            public MyItemListener_JComboBox(){
            }

            @Override
            public void itemStateChanged(ItemEvent e) {
                JComboBox combobox = (JComboBox) e.getSource();

                String item = (String) e.getItem();
                setOptionValue(item);

                fireOptionTableEvent(  new OptionTableEvent( getThis() , combobox, e , false ) );
            }
        }
    }


    public class MyCheckBox extends MyOption{
        JPanel     mPanel     = null;
        JCheckBox  mCheckBox  = null;

        public MyCheckBox(Option option, String label, int x, int y, boolean value)
        {
            super(option,label,x,y);
            initPanel();
            this.setValue(mCheckBox,value);
        }

        public JPanel getPanel(){ return mPanel; }

        public void initPanel(){
            //JComboBox<String> combobox = new JComboBox<>();
            mCheckBox = new JCheckBox();
            mPanel = new JPanel();
            mPanel.setLayout(new BorderLayout());

            mPanel.add( new JLabel(this.mLabel) , BorderLayout.WEST );
            mPanel.add( mCheckBox , BorderLayout.CENTER );
            mCheckBox.addItemListener( new MyCheckBox.MyItemListener_JCheckBox() );
            if(!this.mActivated){
                mCheckBox.setEnabled(false);
            }
        }

        @Override
        public void setValue(Object value) {
            this.setValue( this.mCheckBox , value );
        }

        @Override
        public void setValue(JComponent c, Object value) {
            JCheckBox cb  = (JCheckBox) c;
            Boolean   vs  = (Boolean) value;
            cb.setSelected(vs.booleanValue());
            this.setOptionValue(vs.toString());
        }

        public boolean isActivated(){return mActivated;}
        public void activate(){this.mActivated=true;this.mCheckBox.setEnabled(true);}
        public void deactivate(){this.mActivated=false;;this.mCheckBox.setEnabled(false);}

        public MyCheckBox getThis(){ return this; }

        class MyItemListener_JCheckBox implements ItemListener {

            public MyItemListener_JCheckBox(){
            }

            @Override
            public void itemStateChanged(ItemEvent e) {
                JCheckBox checkbox = (JCheckBox) e.getSource();

                boolean item = checkbox.isSelected();//(String) e.get();
                setOptionValue( Boolean.toString(item) );

                fireOptionTableEvent(  new OptionTableEvent( getThis() , checkbox, e , false ) );
            }
        }

    }

//    public static class MySlider extends MyOption{
//
//    }



    public Properties collectValues() {

        Properties p = new Properties();

        for(  IntOrderedPair ip : this.mOptions.keySet()  ){
             MyOption opt = mOptions.get(ip);
             if(opt != null){
                 if(opt.isActivated()){
                     p.put( mPropertyNames.get(ip) , mOptions.get(ip).getOptionValue() );
                 }
             }
        }
        System.out.println("JMyOptionsTable::collectValues() -> "+p.toString());
        return p;
    }



//    public static final class IntOrderedPair{
//        public final int A;
//        public final int B;
//        private final int mHash;
//        public IntOrderedPair(int a, int b){
//            this.A = a;
//            this.B = b;
//            mHash  = Long.hashCode( ((((long)this.A) <<32) | ((long)this.B)) );
//        }
//        @Override
//        public boolean equals(Object o){
//            if(!(o instanceof IntOrderedPair)){return false;}
//            IntOrderedPair iop = (IntOrderedPair) o;
//            return (iop.A == this.A) && (iop.B == this.B);
//        }
//        @Override
//        public int hashCode(){
//            return this.mHash;
//        }
//    }




    List<OptionTableListener> mTableListeners = new ArrayList<>();

    public void registerOptionTableListener(OptionTableListener listener){
        mTableListeners.add(listener);
    }

    public void fireOptionTableEvent( OptionTableEvent event ){
        for(OptionTableListener otl : mTableListeners){
            otl.optionTableEventHappened(event);
        }
    }

    public static interface OptionTableListener{
        public void optionTableEventHappened(OptionTableEvent event);
    }

    public static class OptionTableEvent{

        private MyOption     mOption;
        private JComponent   mSource;

        private EventObject mOriginalEvent;
        private boolean     mIsAdjusting;

        public OptionTableEvent( MyOption option, JComponent source , EventObject originalEvent , boolean isAdjusting){
            this.mOption = option;
            this.mSource = source;
            this.mOriginalEvent = originalEvent;
            this.mIsAdjusting   = isAdjusting;
        }

        public MyOption    getOption(){ return mOption; }
        public JComponent  getSource(){ return mSource; }
        public EventObject getOriginalEvent(){ return mOriginalEvent; }
        public boolean     isAdjusting(){ return mIsAdjusting; }
    }


    public static void main(String args[]){
        JFrame f = new JFrame();
        f.setVisible(true);
        f.setSize(600,600);

        f.getContentPane().setLayout(new BorderLayout());

        //JMyOptionsTable mTable = new JMyOptionsTable(2,2);
        JMyOptionsTable mTable = new JMyOptionsTable();

        mTable.setSlider( 0,0, "lenghtA" ,"Length:", 0,400 , 130);
        mTable.setSlider( 0,1,"myAngle","Angle:", 0,2*Math.PI  , 5.4);
        List<String> mModes = new ArrayList<String>(); mModes.add("Fast");mModes.add("Insane");mModes.add("Smooth");
        mTable.setComboBox( 1,0,"Mode","Mode",mModes, "Smooth");
        mTable.setSlider(1,1,"optionVerySpecial","Unneccess. Option",0,100 , 33 );
        mTable.deactivate(1,1);
        mTable.reinit();

        f.getContentPane().add( mTable , BorderLayout.CENTER);
        f.pack();
    }

    public static void main_01(String args[]){
        JFrame f = new JFrame();
        f.setVisible(true);
        f.setSize(600,600);

        f.getContentPane().setLayout(new BorderLayout());


        JPanel parent = new JPanel();
        double layout[][] = new double[][]{ {16,TableLayout.PREFERRED,16,TableLayout.PREFERRED,16}  ,{16,TableLayout.PREFERRED,16} };
        parent.setLayout(new TableLayout(layout));

        //JMyOptionsTable mTable = new JMyOptionsTable(4, 4);
        JMyOptionsTable mTable = new JMyOptionsTable();

        mTable.setSlider( 0,0, "lenghtA" ,"Length:", 0,400 , 130);
        mTable.setSlider( 0,1,"myAngle","Angle:", 0,2*Math.PI  , 5.4);
        List<String> mModes = new ArrayList<String>(); mModes.add("Fast");mModes.add("Insane");mModes.add("Smooth");
        mTable.setComboBox( 1,0,"Mode","Mode",mModes, "Smooth");
        mTable.setSlider(1,1,"optionVerySpecial","Unneccess. Option",0,100 , 33 );
        mTable.deactivate(1,1);
        mTable.reinit();


        JTabbedPane tp = new JTabbedPane();
        tp.addTab("XTab",mTable);
        parent.add( tp , " 1 , 1 " );

//        JPanel pNested = new JPanel();
//        JMyOptionsTable mTable2 = new JMyOptionsTable(2, 2);
//        mTable2.setSlider(0,0,"otherLength","Length2:",0,200);


        f.getContentPane().add( parent , BorderLayout.CENTER);
        f.pack();
    }


}
