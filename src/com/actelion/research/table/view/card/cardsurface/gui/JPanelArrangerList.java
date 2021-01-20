package com.actelion.research.table.view.card.cardsurface.gui;

import com.actelion.research.table.view.card.cardsurface.AbstractCardSurfacePanel;
import com.actelion.research.table.view.card.cardsurface.CardDrawingConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JPanelArrangerList extends JList<CardDrawingConfig.CardSurfaceSlot> {

    JCardConfigurationPanel mCCP;

    DefaultListModel<CardDrawingConfig.CardSurfaceSlot>       mListModel    = new DefaultListModel<>();

    MyCellRendererComponent        mCellRenderer = new MyCellRendererComponent();

    MyTransferHandler              mTransferHandler = new MyTransferHandler();


    public JPanelArrangerList(JCardConfigurationPanel ccp){
        this.mCCP = ccp;

        //this.mListModel.addElement(new CardDrawingConfig.CardSurfaceSlot( new StructurePanel() ,new int[]{2},0.0 ,0.75 ));
        //this.mListModel.addElement(new CardDrawingConfig.CardSurfaceSlot( new SingleDataPanel(),new int[]{4},0.75,0.1 ));
        //this.mListModel.addElement("TEST 3");


        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.setLayoutOrientation(JList.VERTICAL);
        this.setModel(mListModel);
        //this.setVisibleRow

        //this.setMinimumSize(new Dimension(400,600));
        this.setPreferredSize(new Dimension(400,600));
        this.setMaximumSize(new Dimension(4000,6000));
        this.setBackground(Color.black);

        this.setCellRenderer(mCellRenderer);

        this.setTransferHandler(mTransferHandler);
        this.setDragEnabled(false);
        //this.setDragEnabled(true);

        this.updateUI();
    }


    class MyCellRendererComponent extends JPanel implements ListCellRenderer<CardDrawingConfig.CardSurfaceSlot>{

        private LayoutManager mLayout = new BoxLayout(this,BoxLayout.X_AXIS);

        @Override
        public Component getListCellRendererComponent(JList<? extends CardDrawingConfig.CardSurfaceSlot> list, CardDrawingConfig.CardSurfaceSlot value, int index, boolean isSelected, boolean cellHasFocus) {
            //this.setBackground(Color.red);

            // resolve the actual surface panel:
            this.removeAll();
            this.setLayout(mLayout);
            this.add(new JLabel(value.getSurfacePanel().getName()));
            //this.add(new JComboBox<String>());

            if(isSelected){
                this.setBorder( BorderFactory.createLineBorder(Color.orange.darker().darker(),2));
            }
            else{
                this.setBorder( BorderFactory.createLineBorder(Color.black,1));
            }

            this.setBackground( (index%2==0)?Color.gray:Color.lightGray );
            this.setMinimumSize( new Dimension(200,400) );


            for(int zi=0;zi<value.getSurfacePanel().getNumberOfDataColumnsRequired();zi++){
                JComboBox<String> options = new JComboBox<>();
                options.setSize(new Dimension(1200,100));
                options.addItem("A");options.addItem("B");options.addItem("X");
                this.add(options);
            }


            return this;
        }
    }

    class MyTransferHandler extends TransferHandler{
        private int index = -1;

        /**
         * We only support importing strings.
         */
        @Override
        public boolean canImport(TransferHandler.TransferSupport info) {
            // Check for String flavor
            if (info.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return true;
            }
            if (info.isDataFlavorSupported(CardDrawingConfig.cardSurfacePanelConfigFlavor)){
                return true;
            }
            return false;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            JList list = (JList)c;
            index  = list.getSelectedIndices()[0];
            return mListModel.get(index);
        }

        /**
         * We support both copy and move actions.
         */
        @Override
        public int getSourceActions(JComponent c) {
            return TransferHandler.COPY_OR_MOVE;
        }


        @Override
        public boolean importData(TransferHandler.TransferSupport info){
            if (!info.isDrop()) {
                return false;
            }

            JList list = (JList)info.getComponent();
            DefaultListModel listModel = (DefaultListModel)list.getModel();
            JList.DropLocation dl = (JList.DropLocation)info.getDropLocation();
            int dropIndex = dl.getIndex();
            boolean insert = dl.isInsert();

            // Get the string that is being dropped.
            Transferable t = info.getTransferable();


            CardDrawingConfig.CardSurfaceSlot data = null;
            List<DataFlavor> dataFlavors      = Arrays.stream( t.getTransferDataFlavors() ).collect(Collectors.toList());
            if( dataFlavors.contains(CardDrawingConfig.cardSurfacePanelConfigFlavor) ) {
                try {
                    data = (CardDrawingConfig.CardSurfaceSlot) t.getTransferData(CardDrawingConfig.cardSurfacePanelConfigFlavor);
                } catch (Exception e) {
                    return false;
                }
            }
            else{
                if( dataFlavors.contains(DataFlavor.stringFlavor) ) {
                    try {
                        String name                    = (String) t.getTransferData(DataFlavor.stringFlavor);
                        AbstractCardSurfacePanel panel = CardDrawingConfig.getCardPanel(name);
                        // @ TODO find fitting column..
                        // @ TODO find space..

                        int slotConfig[] = new int[panel.getNumberOfDataColumnsRequired()];
                        Arrays.fill( slotConfig, -1);
                        data = new CardDrawingConfig.CardSurfaceSlot( mCCP.getConfig() ,panel,0.9 , 0.1 );
                    } catch (Exception e) {
                        return false;
                    }
                }
            }
            // Perform the actual import.
            if(insert){
                mListModel.add(dropIndex,data);
            }
            else{
                mListModel.add(dropIndex+1,data);
                if(dropIndex<mListModel.getSize()) {
                    mListModel.set(dropIndex+1, data);
                }
                else {
                    mListModel.add(dropIndex+1, data);
                }
            }
            return true;
        }


        /**
         * Remove the items moved from the list.
         */
        @Override
        protected void exportDone(JComponent c, Transferable data, int action) {
            JList source = (JList)c;
            DefaultListModel listModel  = (DefaultListModel)source.getModel();

            if (action == TransferHandler.MOVE) {
                mListModel.remove(index);
            }
            index = -1;
        }
    }


}
