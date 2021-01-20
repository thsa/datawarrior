package com.actelion.research.table.view.card.cardsurface;

import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

public class MultiDataPanel extends AbstractCardSurfacePanel {

    public final static String NAME = "Multi Data";



    private int mColumns[] = new int[]{-1,-1,-1,-1};

    private int mNumberOfColumns = 4;

    public void setNumberOfColumns(int numberOfColumns){
        mNumberOfColumns = numberOfColumns;
    }



    private double mDivA = 0.75;
    private double mDivB = 0.0;

    public void setDivA(double divA){this.mDivA = divA;}
    public void setDivB(double divB){this.mDivB = divB;}





    private List<SingleDataPanel> mSingleDataPanels = new ArrayList<>();

    @Override
    public void drawPanel(Graphics g, CardDrawingConfig conf, CompoundTableModel model, CompoundRecord rec, int[] columns, int w, int h) {
        Graphics2D g2 = (Graphics2D) g;

        AffineTransform atOriginal = g2.getTransform();

        int panelsToPlot = Math.min( mSingleDataPanels.size() , Math.min( columns.length, this.getNumberOfDataColumnsRequired() ) );
        for(int zi=0;zi<panelsToPlot  ;zi++){

            if(zi%2==0){g2.setColor(Color.gray.brighter());}else{g2.setColor(Color.gray.brighter().brighter());}


            g2.setTransform(atOriginal);
            g.translate(0,(h*zi)/ getNumberOfDataColumnsRequired() );

            g2.fillRect(0,0,w,h/ getNumberOfDataColumnsRequired()  );

            try {
                this.mSingleDataPanels.get(zi).drawPanel(g, conf, model, rec, new int[]{columns[zi]}, w, h / getNumberOfDataColumnsRequired());
            }
            catch(Exception e){
                System.out.println("[MultiDataPanel] Something went wrong while drawing the panel.. \n"+e.toString());
            }
        }
        g2.setTransform(atOriginal);
    }

    @Override
    public String getName() {
        return this.NAME;
    }

    @Override
    public JComponent getLabel() {
        return new JLabel("MultiDataPanel!");
    }

    @Override
    public JPanel getConfigDialog() {
        return new JMDPConfigPanel();
    }

    @Override
    public int getNumberOfDataColumnsRequired() {
        return mNumberOfColumns;
    }

    @Override
    public boolean canHandleColumnForSlot(CompoundTableModel model, int slot, int column) {
        return(model.isColumnTypeDouble(column) || model.isColumnTypeString(column) );
    }

    @Override
    public void setColumns(int[] dataLinks) {
        this.mColumns = dataLinks;
    }

    @Override
    public synchronized void initalizeCardPanel(Graphics g, CardDrawingConfig conf, CompoundTableModel model, List<CompoundRecord> records, int[] columns, int w, int h) {
        this.mSingleDataPanels.clear();
        for(int zi=0;zi<this.getNumberOfDataColumnsRequired();zi++){
            SingleDataPanel sdp = new SingleDataPanel();
            sdp.initalizeCardPanel(g, conf, model, records, new int[]{columns[zi]},w,h/this.getNumberOfDataColumnsRequired() );
            sdp.setDivA(mDivA); sdp.setDivB(mDivB);
            if(this.mDataShowMode!=null){sdp.setDataShowMode(mDataShowMode);}
            this.mSingleDataPanels.add(sdp);
        }
    }

    @Override
    public int[] getColumns() {
        return this.mColumns;
    }


    private SingleDataPanel.DataShowMode mDataShowMode = null;

    public void setDataShowMode(SingleDataPanel.DataShowMode mode) {
        this.mSingleDataPanels.stream().forEach( sdpi -> sdpi.setDataShowMode(mode) );
        this.mDataShowMode = mode;
    }

    //@TODO implement..proposeCorrespondingStackSurfacePanel
    @Override
    public List<StackSurfacePanelAndColumnsAndPosition> proposeCorrespondingStackSurfacePanel( int columns[] , double relPos, double relHeight ) {
        List<StackSurfacePanelAndColumnsAndPosition> panels = new ArrayList<>();

        // NOTE: for this it has to be actually initialized..
        int cntSlot = 0;
        for(SingleDataPanel sdp :  this.mSingleDataPanels){
            panels.addAll( sdp.proposeCorrespondingStackSurfacePanel( new int[]{ columns[cntSlot]} , relPos + cntSlot * relHeight/this.mSingleDataPanels.size() , relHeight/this.mSingleDataPanels.size() ) );
            cntSlot++;
        }
        return panels;
    }




    class JMDPConfigPanel extends JPanel implements ActionListener {

        private JComboBox<Integer> mComboBoxNumberOfSlots = new JComboBox<>();

        public JMDPConfigPanel(){
            super();

            init();
            syncConfigToGUI();
            mComboBoxNumberOfSlots.addActionListener(this);
            mComboBoxNumberOfSlots.setMaximumSize(new Dimension(10000,20));

            this.setBackground(Color.white);
            this.setVisible(true);
        }

        public void init(){
            for(int zi=0;zi<16;zi++){ this.mComboBoxNumberOfSlots.addItem(zi); }

            this.setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
            this.mComboBoxNumberOfSlots.setSelectedIndex(4);
            this.add(mComboBoxNumberOfSlots);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            syncGUIToConfig();
        }

        public void syncGUIToConfig(){
            setNumberOfColumns( (int) this.mComboBoxNumberOfSlots.getSelectedItem() );
        }

        // Reinits the slot panels..
        public void syncConfigToGUI() {

        }
    }


}
