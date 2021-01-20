package com.actelion.research.table.view.card.cardsurface;

import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.card.CardElement;

import java.awt.*;
import java.util.List;

public abstract class AbstractStackSurfacePanel extends  AbstractCardElementSurfacePanel {


    public abstract void drawPanel(Graphics g, StackDrawingConfig conf, CompoundTableModel model, List<CompoundRecord> rec , CardElement ce , int columns[], int w, int h);

    public boolean isSingleCardSurfacePanel() { return false;}

    /**
     * This will be called once before the cards are drawn, always after a change of  configuration or
     * width / size of the panel..
     *
     * @param g
     * @param conf
     * @param model
     * @param records
     * @param columns
     * @param w
     * @param h
     */
    public abstract void initalizeStackPanel(Graphics g, StackDrawingConfig conf, CompoundTableModel model, int focusListFlag , List<CompoundRecord> records, int columns[], int w, int h);


}
