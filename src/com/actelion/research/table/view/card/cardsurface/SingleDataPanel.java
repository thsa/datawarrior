package com.actelion.research.table.view.card.cardsurface;

import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.VisualizationColor;
import com.actelion.research.table.view.VisualizationColorListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class SingleDataPanel extends AbstractCardSurfacePanel {

    public final static String NAME = "Single Data";


    /**
     * Margins
     */
    private static final int CONF_MARGIN_LEFT = 4;



    /**
     * Space used for the column name
     */
    private double mDividerPosA = 0.75;

    /**
     * Rel. size (compared to TOTAL CARD WIDTH) of second panel.
     */
    private double mDividerPosB = 0.0;




    /**
     * This is the font which is used to initialize the "shrinking procedure". The final font which will be used in
     * this panel is mFinalFont
     */
    private Font mBaseFont = new Font("LucidaSans", Font.BOLD, 10);

    private int mColumn = -1;


    // CONFIG VARIABLES
    private boolean mShowName  = true;
    private boolean mShowValue = true;
    private boolean mShowBar   = false;


    /**
     * Modes that can be used for showing the card..
     */
    public enum DataShowMode { NameAndValue , NameAndValueAndBar , NameAndBar , Bar }

    public void setDataShowMode(DataShowMode mode){
        if(mode==DataShowMode.NameAndValue){ mShowName = true; mShowValue = true ;mShowBar = false; }
        if(mode==DataShowMode.NameAndValueAndBar){ mShowName = true; mShowValue = true ;mShowBar = true; }
        if(mode==DataShowMode.NameAndBar){ mShowName = true; mShowValue = false ;mShowBar = true; }
        if(mode==DataShowMode.Bar){ mShowName = false; mShowValue = false ;mShowBar = true; }
    }


    /**
     * Final font that will be used for rendering the panel.
     */
    private Font mFinalFont = null;


    /**
     * Computed interval for drawing bars..
     */
    private double mFinalMinValue = 0.0;
    private double mFinalMaxValue = 1.0;

    /**
     * Final name that is used to indicate the name of the column
     */
    private String mFinalColumnName = "";



    private boolean mIsLogarithmic = false;

    @Override
    public void initalizeCardPanel(Graphics g, CardDrawingConfig conf, CompoundTableModel model, List<CompoundRecord> records, int columns[], int w, int h){


        // determine font size:
        g.setFont(mBaseFont);
        float fontSize = ( (1.0f*w) / g.getFontMetrics().getHeight()) * 10;
        g.setFont( mBaseFont.deriveFont(Font.PLAIN, fontSize) );
        //determine if we have to shrink more to fit the width..
        if( g.getFontMetrics().charWidth('w') * 20 > w){
            //System.out.println("warning: shrink font more..");
            // shrink more..
            float fontSizeSmaller = ( w / (g.getFontMetrics().charWidth('w')*20.0f) ) * g.getFont().getSize();
            g.setFont( g.getFont().deriveFont(Font.PLAIN, fontSizeSmaller) );
            this.mFinalFont = g.getFont();
        }

        String columnTitle = "<UNKNOWN NAME>";
        try {
            columnTitle = model.getColumnTitle(columns[0]); //model.getColumnTitle(columns[0]);

            // no log.. actually, with getValue we get the non-logarithmic value independent of the logarithmic view mode
//            if(model.isLogarithmicViewMode(columns[0])){
//                columnTitle = "log("+columnTitle+")";
//            }
        }
        catch(Exception e){
        }
        if(columnTitle==null){columnTitle = "<NULL>";}

        mFinalColumnName = columnTitle;

        if( g.getFontMetrics().stringWidth(columnTitle) > mDividerPosA * w ){
            // shrink:
            String shrinkString = columnTitle;
            while( g.getFontMetrics().stringWidth( (shrinkString+"..") ) > mDividerPosA * w && shrinkString.length()>=2 ){
                shrinkString = new String( shrinkString.substring(0,shrinkString.length()-2));
            }
            mFinalColumnName = shrinkString+"..";
        }

        // compute min ( max
        try {
            this.mFinalMinValue = records.stream().mapToDouble(r -> r.getDouble(columns[0])).min().orElse(Double.NaN);
            this.mFinalMaxValue = records.stream().mapToDouble(r -> r.getDouble(columns[0])).max().orElse(Double.NaN);
        }
        catch(Exception e){
            System.out.println("SingleDataPanel : Failed to compute min/max");
        }

        this.mColumn = columns[0];
    }


    public void setDivA(double divA){this.mDividerPosA = divA;}

    public void setDivB(double divB){this.mDividerPosB = divB;}


    @Override
    public void drawPanel(Graphics g, CardDrawingConfig conf, CompoundTableModel model, CompoundRecord rec, int columns[], int w, int h) {

//        g.setColor(Color.lightGray);
//        g.fillRect((int)(0.2*w),(int)(0.2*h),(int)(0.6*w),(int)(0.6*h));
//        g.setColor(Color.RED);
        //g.drawString("<<!!!TESTDATA!!!>>",(int)(0.1*w),(int)(0.4*h));


        // Color the background:
        //model.get
        if(conf.getColorHandler().hasColorAssigned(columns[0], CompoundTableColorHandler.BACKGROUND)){
            //Color bgColor = conf.getColorHandler().getVisualizationColor(columns[0],CompoundTableColorHandler.BACKGROUND).getColor(rec);
            Color bgColor = conf.getColorHandler().getVisualizationColor(columns[0],CompoundTableColorHandler.BACKGROUND).getColorForBackground(rec);
            g.setColor( bgColor );
            g.fillRect( 0,0,w,h );
        }


        // get the value:
        String varName  = "<nothing>";

        boolean  isText = model.isColumnTypeString(columns[0]);
        double  value    = 0;
        String  stringValue = "";

        try {
            if(isText){
                //stringValue = model.getValue(rec, columns[0]);
                //stringValue = model.getTotalValueAt(rec.getID(), columns[0]);
                //stringValue = model.getValue(rec,columns[0]);  //(String) rec.getData(columns[0]);
                stringValue = model.getTotalValueAt(rec.getID(),columns[0]);  //(String) rec.getData(columns[0]);
            }
            else {
                //value = model.getDoubleAt(rec.getID(), columns[0]);
                //value = model.getTotalDoubleAt(rec.getID(), columns[0]);
                //value = rec.getDouble(columns[0]);
                //value = model.getTotalDoubleAt(rec.getID(),columns[0]);
                isText = true;
                stringValue = model.getTotalValueAt(rec.getID(),columns[0]);
            }
        }
        catch(Exception e){
            System.out.println("[SingleDataPanel] --> Error fetching value from model..");
            System.out.println(e);
        }

        //if( mShowName && mShowValue && !mShowBar ){
        g.setFont(mFinalFont);
        g.setColor(Color.black);
        //int myFontHeight = g.getFontMetrics().getAscent()+g.getFontMetrics().getDescent();

        double posTextY = ( h- (h-g.getFontMetrics().getHeight())/2 ) - g.getFontMetrics().getDescent();

        if(isText) {
            String shrunkString = FontAndTextHelper.shrinkString( g , stringValue , (int) ((1.0 - mDividerPosA) * w) );
            g.drawString(mFinalColumnName + ":  "+stringValue, (int) (0.00 * w) + CONF_MARGIN_LEFT, (int) (posTextY));
        }
        if(!isText) {
            if (mShowName && mShowValue && !mShowBar) {

                g.drawString(mFinalColumnName + ":", (int) (0.00 * w) + CONF_MARGIN_LEFT, (int) (posTextY));

                String s = "";

                // shrink number to good size
                s = shrinkNumberToWidth(g, (int) ((1.0 - mDividerPosA) * w), value);
                g.drawString(s, (int) ((mDividerPosA) * w) + CONF_MARGIN_LEFT, (int) posTextY);
            }

            if (mShowName && !mShowValue && mShowBar) {
                g.drawString(mFinalColumnName + ":", (int) (0.00 * w) + CONF_MARGIN_LEFT, (int) (posTextY));
                // draw bar:
                int posXA = (int) (mDividerPosA * w) + CONF_MARGIN_LEFT;
                double lengthFull = w - posXA;
                int lengthBar = (int) (lengthFull * ((value - mFinalMinValue) / (mFinalMaxValue - mFinalMinValue)));
                g.setColor(Color.red.darker());
                g.fillRect(posXA, 4, lengthBar, h - 8);

            }

            if (mShowName && mShowValue && mShowBar) {
                g.drawString(mFinalColumnName + ":", (int) (0.00 * w) + CONF_MARGIN_LEFT, (int) (posTextY));

                int posXA = (int) (mDividerPosA * w) + CONF_MARGIN_LEFT;
                int posXB = posXA + (int) (((mDividerPosA + mDividerPosB) * mDividerPosA * w) + CONF_MARGIN_LEFT);
                int lengthNumber = posXB - posXA;

                String s = "";
                if (!isText) {
                    // shrink number to good size
                    s = shrinkNumberToWidth(g, (int) lengthNumber, value);
                }
                {
                    s = stringValue;
                }
                g.drawString(s, (int) posXA, (int) posTextY);
                // draw bar:
                double lengthFull = w - posXB;
                int lengthBar = (int) (lengthFull * ((value - mFinalMinValue) / (mFinalMaxValue - mFinalMinValue)));
                g.setColor(Color.red.darker());
                g.fillRect(posXB, 4, lengthBar, h - 8);
            }
        }

    }





    @Override
    public String getName() {
        return this.NAME;
    }

    @Override
    public JComponent getLabel() {
        return new JLabel("SIMPLE!");
    }

    @Override
    public JPanel getConfigDialog() {
        return new JSDPConfigPanel();
    }

    @Override
    public int getNumberOfDataColumnsRequired() {
        return 1;
    }

    @Override
    public boolean canHandleColumnForSlot(CompoundTableModel model, int slot, int column) {
        return( model.isColumnTypeDouble(column) || model.isColumnTypeString(column) );
    }

    @Override
    public void setColumns(int columns[]){
        if(columns.length!=1){
            System.out.println("SingleDataPanel : Wrong number of data columns supplied! "+columns.length+" instead of "+1);
        }
        this.mColumn = columns[0];
    }


    public static String shrinkNumberToWidth(Graphics g, int width, double value){

        int      decimal = 7; // one more than we maximally wanna try..
        boolean  lengthOk = false;
        while( !lengthOk && decimal>0 ){
            lengthOk = g.getFontMetrics().stringWidth(  String.format( "%."+decimal+"f" , value )  ) <= width;
            decimal--;
        }

        if(!lengthOk){
            //hmm.. maybe try to return exp version
            return String.format("%.1e",value);
        }
        else{
            return String.format( "%."+decimal+"f" , value );
        }
    }

    @Override
    public int[] getColumns() {
        return new int[]{ this.mColumn };
    }


    //@TODO implement..proposeCorrespondingStackSurfacePanel
    @Override
    public List<StackSurfacePanelAndColumnsAndPosition> proposeCorrespondingStackSurfacePanel( int columns[] , double relPos, double relHeight ) {
        StackSingleDataPanel ssdp = new StackSingleDataPanel();
        //ssdp.setColumns( new int[]{this.mColumn} );
        List<StackSurfacePanelAndColumnsAndPosition> plist = new ArrayList<>();
        plist.add( new StackSurfacePanelAndColumnsAndPosition(ssdp, new int[]{columns[0]} , relPos, relHeight ) );
        return plist;
    }

    class JSDPConfigPanel extends JPanel implements ActionListener, ChangeListener {

        private JPanel mPanelA = new JPanel();
        private JPanel mPanelB = new JPanel();
        private JPanel mPanelC = new JPanel();

        private String[] mModeNames = new String[]{"Name+Value","Name+Bar","Name+Value+Bar"};

        private JComboBox mComboBoxMode = new JComboBox( mModeNames );

        private JSlider mSliderA = new JSlider();
        private JSlider mSliderB = new JSlider();


        public JSDPConfigPanel(){
            super();

            this.init();
            this.synchConfigToGUI();

            mSliderA.addChangeListener(this);
            mComboBoxMode.addActionListener(this);
        }

        public void init(){
            this.setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
            this.add(mPanelA);
            this.add(mPanelB);
            this.add(mPanelC);
            this.setAlignmentX(Component.LEFT_ALIGNMENT);

            mPanelA.setLayout(new FlowLayout(FlowLayout.LEFT));
            mPanelA.add(new JLabel("MODE:"));
            mPanelA.add(mComboBoxMode);
            mPanelA.setMaximumSize(new Dimension(100000,50));
            //mPanelA.setAlignmentX(Component.LEFT_ALIGNMENT);

            mPanelB.setLayout(new FlowLayout(FlowLayout.LEFT));
            mPanelB.add(new JLabel("Divider A:"));
            mPanelB.add(mSliderA);
            mPanelB.setMaximumSize(new Dimension(100000,50));
            //mPanelB.setAlignmentX(Component.LEFT_ALIGNMENT);

            mSliderA.setMinimumSize(new Dimension(100,45));
            mSliderA.setMinimum(0);
            mSliderA.setMaximum(100);

            mPanelC.setLayout(new FlowLayout(FlowLayout.LEFT));
            mPanelC.add(new JLabel("Divider B:"));
            mPanelC.add(mSliderB);
            mPanelC.setMaximumSize(new Dimension(100000,40));
            //mPanelC.setAlignmentX(Component.LEFT_ALIGNMENT);

            mSliderB.setMinimumSize(new Dimension(100,45));
            mSliderB.setMinimum(0);
            mSliderB.setMaximum(100);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            this.synchGUItoConfig();
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            this.synchGUItoConfig();
        }

        /**
         * Inits the gui with the current config values
         */
        public void synchConfigToGUI(){

            if(mShowName && mShowValue &&  mShowBar ){
                mComboBoxMode.setSelectedIndex(2);
                mSliderA.setEnabled(true);
                mSliderB.setEnabled(true);
                mSliderA.setValue( (int) (100*mDividerPosA) );
                // slider goes from 0 to (1-mDividerPosA)
                mSliderB.setValue( (int) (100*mDividerPosB/(1-mDividerPosA))  );
            }

            if(mShowName && mShowValue &&  !mShowBar ){
                mComboBoxMode.setSelectedIndex(0);
                mSliderA.setEnabled(true);
                mSliderB.setEnabled(false);
                mSliderA.setValue( (int) (100*mDividerPosA) );
            }

            if(mShowName && !mShowValue &&  mShowBar ){
                mComboBoxMode.setSelectedIndex(1);
                mSliderA.setEnabled(true);
                mSliderB.setEnabled(false);
                mSliderA.setValue( (int) (100*mDividerPosA) );
            }
        }

        /**
         * Inits the gui with the current config values
         */
        public void synchGUItoConfig(){
            if(this.mComboBoxMode.getSelectedIndex()==0){
                mShowName = true;
                mShowValue = true;
                mShowBar  = false;
                mDividerPosA = 0.01 * this.mSliderA.getValue();
                mSliderB.setEnabled(false);
            }
            if(this.mComboBoxMode.getSelectedIndex()==1) {
                mShowName = true;
                mShowValue = false;
                mShowBar  = true;
                mDividerPosA = 0.01 * this.mSliderA.getValue();
                mSliderB.setEnabled(false);
            }
            if(this.mComboBoxMode.getSelectedIndex()==2) {
                mShowName = true;
                mShowValue = true;
                mShowBar  = true;
                mDividerPosA = 0.01 * this.mSliderA.getValue();
                mDividerPosB =   (1-mDividerPosA) * ( 0.01 * this.mSliderB.getValue() );
                mSliderB.setEnabled(true);
            }
        }

    }



}
