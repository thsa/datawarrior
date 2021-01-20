package com.actelion.research.table.view.card.cardsurface;

import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.card.CardElement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class StackMultiDataPanel extends AbstractStackSurfacePanel {

    public final static String NAME = "Multi Data [Stack]";



    private int mColumns[] = new int[]{-1,-1,-1,-1};

    private int mNumberOfColumns = 4;

    public void setNumberOfColumns(int numberOfColumns){
        mNumberOfColumns = numberOfColumns;
    }


    private double mDivA = 0.25;
    private double mDivB = 0.75;


    private List<StackSingleDataPanel> mSingleDataPanels = new ArrayList<>();


    @Override
    public synchronized void drawPanel(Graphics g, StackDrawingConfig conf, CompoundTableModel model, List<CompoundRecord> rec, CardElement ce, int[] columns, int w, int h) {
        Graphics2D g2 = (Graphics2D) g;

        AffineTransform atOriginal = g2.getTransform();

        int panelsToPlot = Math.min( mSingleDataPanels.size() , Math.min( columns.length, this.getNumberOfDataColumnsRequired() ) );
        for(int zi=0;zi<panelsToPlot  ;zi++){

            if(zi%2==0){g2.setColor(Color.gray.brighter());}else{g2.setColor(Color.gray.brighter().brighter());}


            g2.setTransform(atOriginal);
            g.translate(0,(h*zi)/ getNumberOfDataColumnsRequired() );

            g2.fillRect(0,0,w,h/ getNumberOfDataColumnsRequired()  );

            if( this.mSingleDataPanels.size() > zi ) {
                this.mSingleDataPanels.get(zi).drawPanel(g, conf, model, rec, ce, new int[]{columns[zi]}, w, h / getNumberOfDataColumnsRequired());
            }
            else{
                System.out.println("Hmm.. How did we end up here? PROBLEM??");
            }
        }
        g2.setTransform(atOriginal);
    }



    public void setDivA(double divA){this.mDivA = divA;}

    public void setDivB(double divB){this.mDivB = divB;}


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
        return(model.isColumnTypeDouble(column));
    }

    @Override
    public void setColumns(int[] dataLinks) {
        this.mColumns = dataLinks;
    }


    // CACHING OF StackSingleDataPanel (i.e. they are only reinitialized if the records change..)
    private Map<Integer,StackSingleDataPanel> mCacheSingleDataPanel_Panels = new HashMap<>();

    // contains information about the currently cached panels
    private Set<Integer> mCacheSingleDataPanel_CachedRecordConfiguration = new HashSet<>();
    private Set<Integer> mCacheSingleDataPanel_CachedVisibleRecordConfiguration = new HashSet<>();
    private List<Integer> mCacheSingleDataPanel_CachedColumns            = new ArrayList<>();


    @Override
    public synchronized void initalizeStackPanel(Graphics g, StackDrawingConfig conf, CompoundTableModel model, int focusListFlag , List<CompoundRecord> records, int[] columns, int w, int h) {

        // create records id set:
        Set<Integer> recordSet = records.stream().mapToInt( ri -> ri.getID() ).boxed().collect(Collectors.toSet());

        // create nonexcluded records id set:
        Set<Integer> visibleRecordSet = records.stream().filter( ri -> model.isVisible(ri) ).mapToInt( ri -> ri.getID() ).boxed().collect(Collectors.toSet());


        // create columns list:
        List<Integer> columnSet = Arrays.stream(columns).boxed().collect(Collectors.toList());

        // check if we need recreation of the sdps:
        boolean requiresRecreation = (! mCacheSingleDataPanel_CachedColumns.equals(columnSet) ) ||
                                     (! mCacheSingleDataPanel_CachedRecordConfiguration.equals(recordSet) ) ||
                                     (! mCacheSingleDataPanel_CachedVisibleRecordConfiguration.equals(visibleRecordSet));
//        boolean requiresRecreation = true;

        if(requiresRecreation){
            this.mSingleDataPanels.clear();
            this.mCacheSingleDataPanel_CachedRecordConfiguration        = recordSet;
            this.mCacheSingleDataPanel_CachedVisibleRecordConfiguration = visibleRecordSet;
            this.mCacheSingleDataPanel_CachedColumns                    = columnSet;
        }

        //this.mSingleDataPanels.clear();
        for (int zi = 0; zi < this.getNumberOfDataColumnsRequired(); zi++) {

            if(requiresRecreation) {
                //this.mSingleDataPanels.set(zi,new StackSingleDataPanel());
                this.mSingleDataPanels.add(new StackSingleDataPanel());
            }
            StackSingleDataPanel sdp = this.mSingleDataPanels.get(zi);

            // we cannot really avoid this reininit. Could be that just the layout changed..
            sdp.setDivA(mDivA);
            sdp.setDivB(mDivB);
            sdp.initalizeStackPanel(g, conf, model, focusListFlag, records, new int[]{columns[zi]}, w, h / this.getNumberOfDataColumnsRequired());

            //System.out.println("INIT PANEL WITH " + records.size() + "CRs");
        }
    }

    @Override
    public int[] getColumns() {
        return this.mColumns;
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
