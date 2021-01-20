package com.actelion.research.table.view.card.cardsurface.gui;

import com.actelion.research.table.view.card.cardsurface.AbstractCardSurfacePanel;
import com.actelion.research.table.view.card.cardsurface.CardDrawingConfig;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JPanelArrangerTable extends JTable implements TableModelListener , ListSelectionListener {

    /**
     *
     *
     */


    JCardConfigurationPanel mCCP     = null;

    MyTableModel            mModel   = null;

    MyTransferHandler       mTransferHandler = null;

    TableEditorUpdater      mTableEditorUpdater = null;

    //JPanelArrangerList.MyCellRendererComponent mCellRenderer = new JPanelArrangerList.MyCellRendererComponent();

    //JPanelArrangerList.MyTransferHandler mTransferHandler = new JPanelArrangerList.MyTransferHandler();


    public JPanelArrangerTable(JCardConfigurationPanel ccp){
        this.mCCP = ccp;

        //this.mModel.addRow(new CardDrawingConfig.CardSurfaceSlot( new StructurePanel() ,2,0.0,0.75 ));
        //this.mModel.addRow(new CardDrawingConfig.CardSurfaceSlot( new SingleDataPanel(),4,0.75,0.1 ));
        //this.mListModel.addElement("TEST 3");

        mModel = new MyTableModel();

        this.setModel(mModel);

        //this.setMinimumSize(new Dimension(400,600));
        this.setPreferredSize(new Dimension(400,600));
        this.setMaximumSize(new Dimension(4000,6000));
        this.setBackground(Color.lightGray);

        //this.setCellRenderer(mCellRenderer);
        this.getTableHeader().setReorderingAllowed(false);


        mTransferHandler = new MyTransferHandler();
        this.setTransferHandler(mTransferHandler);
        this.setDragEnabled(true);
        this.setDropMode(DropMode.INSERT_ROWS);

        //this.setSelectionMode(TableSelectionModeS);
        this.setRowSelectionAllowed(true);
        this.setColumnSelectionAllowed(false);

        mTableEditorUpdater = new TableEditorUpdater();

        //this.setDefaultRenderer(String.class,new DefaultRenderer());

        this.mModel.addTableModelListener(mTableEditorUpdater);
        this.mModel.addTableModelListener(this);

        this.setVisible(true);
        this.updateUI();

        mModel.fireTableStructureChanged();
    }

    /**
     * Call this to make sure table resynchronizes with the current CardDrawingConfiguration
     */
    public void recomputeTable(){
        this.mModel.fireTableStructureChanged();
    }

    public void tableChanged(TableModelEvent e){
        super.tableChanged(e);

        // init column renderers:
        if(this.mModel!=null) {
            for (int zi = 2; zi < this.mModel.getColumnCount(); zi++) {
                this.getColumnModel().getColumn(zi).setCellRenderer(new MyColumnNameCellRenderer());
            }
        }

        this.repaint();
        if(mCCP!=null) {
            mCCP.repaint();
        }

    }


    /**
     * This is the crucial function that can always be called to "repack" the panels, and correct the sizes of all panels.
     *
     * What it does:
     * 1. It learns the order of panels from the order of the "relativeY" values.
     * 2. It "linearizes" the intervals, in the order learnt in step 1. I.e. it shifts intervals towards larger numbers
     *    until there are no overlaps.
     * 3. The sizes of all panels are divided by the total length of the "linearized" intervals.
     *
     * @TODO figure out way how this also "sorts the TableModel"..
     *
     */
    public void packPanelsAndResize(){

        // sort panels:
        List<CardDrawingConfig.CardSurfaceSlot> sortedPanels = new ArrayList<>();
        for(int zi=0;zi<this.mModel.getRowCount();zi++){
            sortedPanels.add( this.mModel.getRow(zi));
        }
        sortedPanels.sort( (x,y) -> Double.compare( x.getRelativePosY() , y.getRelativePosY() ) );

        // "linearize"
        double ypos = 0;
        for(CardDrawingConfig.CardSurfaceSlot slot : sortedPanels){
            slot.setRelativePosY(ypos);
            ypos+=slot.getRelativeHeight();
            System.out.println("Linearization: YPos="+ypos);
        }

        // pack into 0-1 interval:
        //double newpos = 0
        System.out.println("Recomputed positions and sizes:");
        double ynewpos = 0;
        for(CardDrawingConfig.CardSurfaceSlot slot : sortedPanels){
            slot.setRelativePosY( ynewpos );
            double height_i = slot.getRelativeHeight() / ypos;
            slot.setRelativeHeight( height_i );
            ynewpos += height_i;
            System.out.println("YPos="+slot.getRelativePosY()+" Height="+slot.getRelativeHeight());
        }

        // sort the panels in the mCDC of the TableModel accordingly:
        mCCP.getConfig().getSlots().sort( (x,y) -> Double.compare( x.getRelativePosY() , y.getRelativePosY() ) );
        mModel.fireTableStructureChanged();
    }




    public void valueChanged(ListSelectionEvent e) {
        super.valueChanged(e);
        this.mCCP.setConfigurationPanel( this.mModel.getRow( e.getFirstIndex() ).getSurfacePanel() );
    }



//    /**
//     * Call this to inform the gui that the card configuration has changed. It will reconstruct the CardDrawingConfig object
//     * and reininit all the necessary parts.
//     */
//    public void fireCardConfigurationChanged(){
//    }



    class MyColumnNameCellRenderer implements TableCellRenderer{
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if(mModel==null){
                return new JLabel();
            }
            if(mModel.getValueAt( row, column)==null){
                return new JLabel();
            }
            int linkedColumn = ((Integer) mModel.getValueAt( row, column));
            String columnTitle = mCCP.getTableModel().getColumnTitle(linkedColumn);
            JLabel label = new JLabel( columnTitle );

            return label;
        }
    }

    class MyTableModel extends AbstractTableModel {

        //CardDrawingConfig mCDC = null;

        public MyTableModel() {
        }

        @Override
        public int getRowCount() {
            return mCCP.getConfig().getSlots().size();
        }

        @Override
        public int getColumnCount() {

            int nSlots = mCCP.getConfig().getMaxNumberOfColumnRequired() + 2; // first is name, second is size
            System.out.println("getColumnCount: "+nSlots+" columns");
            return nSlots;
        }

        @Override
        public String getColumnName(int columnIndex) {
            if(columnIndex==0){return "Name";}
            if(columnIndex==1){return "Size";}
            return "DataColumn "+(columnIndex-2);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if(columnIndex==0){return String.class;}
            if(columnIndex==1){return Double.class;}
            return Integer.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if(columnIndex > 0){return true;}
            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            CardDrawingConfig.CardSurfaceSlot slot = mCCP.getConfig().getSlots().get(rowIndex);
            if(columnIndex==0){return slot.getSurfacePanel().getName();}
            if(columnIndex==1){return slot.getRelativeHeight();}
            if( slot.getSurfacePanel().getNumberOfDataColumnsRequired() < columnIndex-2 ){
                return null;
            }
            else {
                if(slot.getColumns().length<columnIndex-2+1){
                    return null;
                }
                return slot.getColumns()[columnIndex - 2];
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if(columnIndex==1){
                Double value = (Double) aValue;
                mCCP.getConfig().getSlots().get(rowIndex).setRelativeHeight(value.doubleValue());
            }
            if(columnIndex>=2){
                Integer value = (Integer) aValue;
                int columnsOld[] = mCCP.getConfig().getSlots().get(rowIndex).getColumns();

                if(columnsOld.length<mCCP.getConfig().getSlots().get(rowIndex).getSurfacePanel().getNumberOfDataColumnsRequired()){
                    int columnsOldUpdated[] = new int[mCCP.getConfig().getSlots().get(rowIndex).getSurfacePanel().getNumberOfDataColumnsRequired()];
                    Arrays.fill(columnsOldUpdated,-1);
                    for(int zi=0;zi<columnsOld.length;zi++){ columnsOldUpdated[zi] = columnsOld[zi]; }
                    mCCP.getConfig().getSlots().get(rowIndex).setColumns(columnsOldUpdated);
                    columnsOld = mCCP.getConfig().getSlots().get(rowIndex).getColumns();
                }

                columnsOld[columnIndex-2] = (value==null)?-1:value;
                mCCP.getConfig().getSlots().get(rowIndex).setColumns(columnsOld);
            }
            // update will be handled by TableModelListener..
            packPanelsAndResize();
            fireTableDataChanged();
        }

        public void addRow(int index, CardDrawingConfig.CardSurfaceSlot slot){
            mCCP.getConfig().getSlots().add(index, slot);
            packPanelsAndResize();
            fireTableDataChanged();
        }
        public void setRow(int index, CardDrawingConfig.CardSurfaceSlot slot){
            mCCP.getConfig().getSlots().set(index, slot);
            packPanelsAndResize();
            fireTableDataChanged();
        }
        public void removeRow(int index){
            mCCP.getConfig().getSlots().remove(index);
            packPanelsAndResize();
            fireTableDataChanged();
        }
        public CardDrawingConfig.CardSurfaceSlot getRow(int index){
            return mCCP.getConfig().getSlots().get(index);
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
            System.out.println("Create transferable");
            JPanelArrangerTable table = (JPanelArrangerTable)c;
            index  = table.getSelectedRow();
            return mModel.getRow(index);
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
            System.out.println( "Import Data:" );


            if (!info.isDrop()) {
                return false;
            }

            JTable table = (JTable)info.getComponent();
            //DefaultListModel listModel = (DefaultListModel)list.getModel();

            JTable.DropLocation dl = (JTable.DropLocation) info.getDropLocation();
            int dropIndex = dl.getRow();
            boolean insert = dl.isInsertRow();

            System.out.println( "DropLocation: "+dropIndex + " insert="+insert );

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

                        data = new CardDrawingConfig.CardSurfaceSlot(mCCP.getConfig() , panel , 0.9 , 0.1 );
                        data.getSurfacePanel().initializePanelConfiguration(mCCP.getTableModel(), slotConfig);
                    } catch (Exception e) {
                        return false;
                    }
                }
            }
            // Perform the actual import.
            if(insert){
                //mModel.insertRow(dropIndex,data);
                mModel.addRow(dropIndex,data);
            }
            else{
                mModel.addRow(dropIndex+1,data);
                if(dropIndex<mModel.getRowCount()) {
                    mModel.setRow(dropIndex+1, data);
                }
                else {
                    mModel.addRow(dropIndex+1, data);
                }
            }
            //updateUI();
            //mModel.fireTableDataChanged();
            mModel.fireTableStructureChanged();
            return true;
        }


        /**
         * Remove the items moved from the list.
         */
        @Override
        protected void exportDone(JComponent c, Transferable data, int action) {
            System.out.println( "Import Data:" );

            JPanelArrangerTable source = (JPanelArrangerTable)c;
            //DefaultListModel listModel  = (DefaultListModel)source.getModel();


            if ( action == TransferHandler.MOVE ) {
                ((MyTableModel)source.getModel()).removeRow(index);
            }
            index = -1;
            updateUI();
        }
    }


    class TableEditorUpdater implements TableModelListener{
        @Override
        public void tableChanged(TableModelEvent e) {
            // reinint the editors..
            //System.out.println("TABLE CHANGED!");
        }
    }

    @Override
    public TableCellEditor getCellEditor(int row, int column){
        if(column==1){
            return new SpinnerEditor( ((Double) this.mModel.getValueAt(row,column) ).doubleValue() );
        }
        if(column>1){
            int nDCR = mModel.getRow(row).getSurfacePanel().getNumberOfDataColumnsRequired();
            if( column - 2 < nDCR ){
                return new DefaultCellEditor( JComboBoxForTableColumns.createComboBoxForSurfacePanelDataLink(mCCP.getTableModel(), mModel.getRow(row).getSurfacePanel(),column - 2 ) );
                // @TODO collect all possible columns for the (column-2) data link of the surface panel..
                //List<Integer> availableOptions = CardDrawingConfig.findAllMatchingColumnsForDataLink(mCCP.getTableModel(),mModel.getRow(row).getSurfacePanel(),column-2 );
                //return new DefaultCellEditor(new JComboBox(  new Vector(availableOptions) ));
            }
        }
        return null;
    }


    static class DefaultRenderer extends DefaultTableCellRenderer {
        public DefaultRenderer() { super(); }

        public void setValue(Object value) {
            this.setText("XXX");
        }
    }



}


class SpinnerEditor extends DefaultCellEditor
{
    private JSpinner spinner;

    public SpinnerEditor(double value)
    {
        super( new JTextField() );
        spinner = new JSpinner(new SpinnerNumberModel(value, 0, 1, 0.02));
        spinner.setBorder( null );
    }

    public Component getTableCellEditorComponent(
            JTable table, Object value, boolean isSelected, int row, int column)
    {
        spinner.setValue( value );
        return spinner;
    }

    public Object getCellEditorValue()
    {
        return spinner.getValue();
    }
}





