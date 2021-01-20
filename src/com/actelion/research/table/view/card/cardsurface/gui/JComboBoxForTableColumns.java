package com.actelion.research.table.view.card.cardsurface.gui;

import com.actelion.research.table.view.card.cardsurface.CardElementSurfacePanelInterface;
import com.actelion.research.table.model.CompoundTableModel;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class JComboBoxForTableColumns extends JComboBox<Integer> {

    private Map<Integer,String> mLabels;


    public JComboBoxForTableColumns(Map<Integer,String> labelsToColumns ){
        this.mLabels  = new HashMap<Integer,String>( labelsToColumns );

        for(Integer column : labelsToColumns.keySet()){
            this.addItem(column);
        }
        this.setRenderer(new MyCellRenderer());

    }

    class MyCellRenderer extends DefaultListCellRenderer{

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            //JLabel label = new JLabel( mLabels.get(value) );
            // if(isSelected){label.setBorder(new LineBorder(Color.blue.darker(),2));}
            System.out.println("create Cell for String: "+mLabels.get(value));
            Component c = super.getListCellRendererComponent(list, mLabels.get(value) , index, isSelected, cellHasFocus);

            return c;
        }
    }


    public static JComboBoxForTableColumns createComboBoxForSurfacePanelDataLink(CompoundTableModel model, CardElementSurfacePanelInterface ce, int slot){

        Map labelMap  = new HashMap<Integer,String>();

        for(int zi=0;zi<model.getColumnCount();zi++) {
            if( ce.canHandleColumnForSlot(model, slot, zi) ){
                labelMap.put(zi,model.getColumnTitle(zi));
            }
        }

        return new JComboBoxForTableColumns(labelMap);
    }

    public static JComboBoxForTableColumns createComboBoxDoubleAndCategorical(CompoundTableModel model){
        Map labelMap  = new HashMap<Integer,String>();

        for(int zi=0;zi<model.getColumnCount();zi++) {
            if( model.isColumnTypeDouble(zi) || model.isColumnTypeCategory(zi) ){
                labelMap.put(zi,model.getColumnTitle(zi));
            }
        }

        return new JComboBoxForTableColumns(labelMap);
    }

}
