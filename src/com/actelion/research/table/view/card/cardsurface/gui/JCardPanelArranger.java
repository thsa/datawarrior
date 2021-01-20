package com.actelion.research.table.view.card.cardsurface.gui;

import com.actelion.research.table.view.card.cardsurface.AbstractCardSurfacePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.util.ArrayList;
import java.util.List;

public class JCardPanelArranger extends JPanel {

    private JCardConfigurationPanel mCardConfigurationPanel = null;

    private JAvailablePanelsPanel mAvailablePanelsPanel = null;
    //private JArrangerTable mArrangerTable = null;
    //private JPanelArrangerList mArrangerList = null;
    private JPanelArrangerTable mArrangerTable = null;

    private JScrollPane mListScrollPane = null;

    private LayoutManager mLayout = null;


    public JCardPanelArranger( JCardConfigurationPanel ccp ) {
        super();
        this.mCardConfigurationPanel = ccp;

        mLayout = new BoxLayout(this, BoxLayout.Y_AXIS);
        //mLayout = new FlowLayout(FlowLayout.LEFT);
        this.setLayout(mLayout);

        this.mAvailablePanelsPanel = new JAvailablePanelsPanel();
        this.add(mAvailablePanelsPanel);

        //this.mArrangerTable = new JCardPanelArranger.JArrangerTable();
        //this.mArrangerList = new JPanelArrangerList(this.mCardConfigurationPanel);
        this.mArrangerTable = new JPanelArrangerTable(this.mCardConfigurationPanel);

        mListScrollPane = new JScrollPane();
        mListScrollPane.setViewportView(mArrangerTable);
        mListScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        mListScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.add(mListScrollPane);
    }

    public JPanelArrangerTable getArrangerTable(){
        return mArrangerTable;
    }



    class JAvailablePanelsPanel extends JList<String> {

        DefaultListModel mModel = new DefaultListModel();

        public JAvailablePanelsPanel() {

            this.setMinimumSize(new Dimension(200, 600));
            this.setMaximumSize(new Dimension(1000, 600));
            //this.setBackground(Color.black);

            this.setModel(mModel);
            this.setLayout(new FlowLayout(FlowLayout.LEFT));
            this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            reininit();


            // enable drag start
            this.setDragEnabled(true);
        }

        public void reininit(){
            int cnt = 0;
            for( AbstractCardSurfacePanel pi : mCardConfigurationPanel.getAllPanels() ) {
                mModel.add(cnt,pi.getName());
                cnt++;
            }
        }

    }









    /**
     * Allows for drop into "dropzones"
     *
      */
    class JArrangerTable extends JPanel {

        private static final int SIZE_Y_FIRST_DROP = 200;
        private static final int SIZE_Y_DROP       = 20;

        private JCardPanelArranger mCPA = null;

        private LayoutManager      mLayout = new BoxLayout(this,BoxLayout.Y_AXIS);


        private List<JComponent>   mPanels      = new ArrayList<>();
        private List<JComponent>   mDropZones   = new ArrayList<>();

        public JArrangerTable() {

            this.setMinimumSize(new Dimension(200, 400));
            this.setBackground(Color.blue);
            this.setLayout(mLayout);

            recomputeDropZones();
        }

        /**
         * Based on mPanels, adds Drop Zones inbetween..
         */
        public void recomputeDropZones(){
            this.mDropZones.clear();
            for( Component c : this.getComponents()){
                if(c instanceof JDropZone){ this.remove(c);}
            }

            if(this.mPanels.size()==0){
                JDropZone newDrop = new JDropZone(this, SIZE_Y_FIRST_DROP);
                this.add(newDrop);
                //this.add(new JLabel("BLUBB!!"));

            }
            else{
                JDropZone newDrop = new JDropZone(this, SIZE_Y_DROP);
                this.add(newDrop,0);
                int cpos= 2;
                for(int zi=0;zi<this.mPanels.size();zi++){
                    if(zi!=this.mPanels.size()) {
                        newDrop = new JDropZone(this, SIZE_Y_DROP);
                    }
                    else{
                        newDrop = new JDropZone(this, SIZE_Y_FIRST_DROP);
                    }
                    this.add(newDrop,cpos);
                    cpos+=2;
                }
            }
            this.add(new Box.Filler(new Dimension(0,0),new Dimension(200,500),new Dimension(10000 , 10000) ));
            this.validate();
        }

        public void proposeDrop(JComponent dropZone){

        }

        /**
         * Fills the table with all the regular components (i.e. the elements of mPanels).
         */
        public void repopulateTable(){
            this.removeAll();
            for(JComponent c : this.mPanels){
                this.add(c);
            }
        }

        public void doDrop(JComponent dropZone, String dropString){
            System.out.println("DROP! "+dropString);
            //this.mPanels.add( new JLabel( dropString ) );
            this.mPanels.add( new JDataLinkPanel("dropString",3) ); // @TODO: add at right position, i.e. add drop pos property to dropzones..
            this.repopulateTable();
            this.recomputeDropZones();
            this.revalidate();
            this.repaint();
        }

    }

    class ToTransferHandler extends TransferHandler{

        private JDropZone mDropZone;

        public ToTransferHandler(JDropZone dropZone){
            this.mDropZone = dropZone;
        }

        public boolean canImport(TransferHandler.TransferSupport support) {
            // for the demo, we will only support drops (not clipboard paste)
            if (!support.isDrop()) {
                return false;
            }

            return true;
            // we only import Strings
            //if (!support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            //    return false;
            //}

            // check if the source actions contain the desired action -
            // either copy or move, depending on what was specified when
            // this instance was created
            //boolean actionSupported = (action & support.getSourceDropActions()) == action;
            //if (actionSupported) {
            //    support.setDropAction(action);
            //    return true;
            //}

            // the desired action is not supported, so reject the transfer
            //return false;
        }

        public boolean importData(TransferHandler.TransferSupport support) {
            try{
                String dropString = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                this.mDropZone.doDrop(dropString);
                return true;
            }
            catch(Exception e){
                return false;
            }
        }

    }

    class JDropZone extends JPanel{
        private JArrangerTable     mArrangerTable = null;
        private ToTransferHandler  mTT  = new ToTransferHandler(this);

        public JDropZone(JArrangerTable table, int height){
            mArrangerTable = table;

            this.setMinimumSize(new Dimension(200,height));
            this.setPreferredSize(new Dimension(200,height));
            this.setMaximumSize(new Dimension(2000000,height));
            this.setTransferHandler(mTT);
            this.setBackground(Color.red.brighter());
        }

        public void doDrop(String dropString){
            this.mArrangerTable.doDrop(this,dropString);
        }
    }


    class JDataLinkPanel extends JPanel{
        private static final int SIZE_Y         = 20;
        private static final int SIZE_X_SPINNER = 40;


        private LayoutManager mLayout       = new BoxLayout(this,BoxLayout.X_AXIS);
        private int           mNumDataLinks = 0;

        private JSpinner      mSpinner      = null;


        public JDataLinkPanel(String name, int dataLinks){
            this.setLayout(mLayout);
            this.mNumDataLinks = dataLinks;

            for(int zi=0;zi<dataLinks;zi++){
                this.add(new JComboBox<>());
            }
            mSpinner = new JSpinner();
            //mSpinner.getModel().
            mSpinner.setMaximumSize(new Dimension(SIZE_X_SPINNER,SIZE_Y));
            this.add(mSpinner);

            this.setMaximumSize(new Dimension(1000000,SIZE_Y));
        }



    }

}