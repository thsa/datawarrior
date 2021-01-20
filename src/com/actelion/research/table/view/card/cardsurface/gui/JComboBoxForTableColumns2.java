package com.actelion.research.table.view.card.cardsurface.gui;


import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.card.cardsurface.CardElementSurfacePanelInterface;
import com.actelion.research.table.view.card.tools.ColWithType;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Difference to JComboBoxForTableColumn: The JComboBoxForTableColumns is only interested in getting the column number.
 * With the ..2, we list all colums with all their possible interpretations.
 *
 */
public class JComboBoxForTableColumns2 extends JComboBox<ColWithType> {

    java.util.List<ColWithType> mColumns = null;

    public JComboBoxForTableColumns2(List<ColWithType> entries ){

        this.mColumns  = new ArrayList<>( entries );

        for(ColWithType column : entries) {
            this.addItem(column);
        }
        this.setRenderer(new JComboBoxForTableColumns2.MyCellRenderer());

    }

    class MyCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            //JLabel label = new JLabel( mLabels.get(value) );
            // if(isSelected){label.setBorder(new LineBorder(Color.blue.darker(),2));}
            System.out.println("create Cell for String: "+ value.toString() );
            Component c = super.getListCellRendererComponent(list, value.toString() , index, isSelected, cellHasFocus);

            return c;
        }
    }


    /**
     * Create combo box for all types supported by ColWithType..
     *
     * @param model
     * @return
     */
    public static JComboBoxForTableColumns2 createComboBoxForAllClasses(CompoundTableModel model){
        List<ColWithType> columns = ColWithType.getAllColumnsFromCTM(model);
        return new JComboBoxForTableColumns2(columns);
    }

    public static JComboBoxForTableColumns createComboBoxDoubleAndCategorical(CompoundTableModel model){
        System.out.println("Not yet implemented..");
        return null;
    }



}
