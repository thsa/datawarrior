package com.actelion.research.table.view.card.cardsurface;

import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public interface CardElementSurfacePanelInterface {

    public String getName();
    public JComponent getLabel();
    public JPanel getConfigDialog();





    /**
     * @return true if surface panel for single card
     */
    public boolean isSingleCardSurfacePanel();

    /**
     *
     * @return number of data columns that are required to show data
     */
    public int getNumberOfDataColumnsRequired();


    /**
     * Checks if the data in column from model is ok for the given slot.
     *
     * @param model
     * @param slot
     * @param column
     * @return
     */
    public boolean canHandleColumnForSlot(CompoundTableModel model, int slot, int column);


    /**
     * Sets the data columns linked to this draw panel
     * @param i
     */
    public void setColumns(int[] dataLinks);


    /**
     * Gets the data columns linked to this draw panel
     * @return data columns linked to this draw panel
     */
    public int[] getColumns();

    /**
     * Should figure out a valid initial configuration for the panel.
     *
     * @param model
     * @param slot
     * @param column If (-1) or if not a matching column, this function should automatically select a matching column
     */
    public void initializePanelConfiguration(CompoundTableModel model, int columns[]);




}
