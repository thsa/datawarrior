package com.actelion.research.table.view.card.cardsurface;

import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.card.CardElement;

import java.awt.*;
import java.util.List;

public abstract class AbstractStackDrawer {


    /**
     *
     * NOTE! Only draws the background of the card SURFACE! Shadow, stacks etc.. are handled by the
     * CardElementBackgroundDrawer!
     *
     * @param g
     * @param model
     * @param rec
     */
    public abstract void drawStackBackground(Graphics g, CompoundTableModel model, List<CompoundRecord> rec);
    public abstract void drawStackSurface(Graphics g, CompoundTableModel model, int focusListFlag, List<CompoundRecord> rec, CardElement ce);
    public abstract void drawStackBorder(Graphics g, CompoundTableModel model, List<CompoundRecord> rec);

    /**
     * Supposed to draw an indicator for the number of cards in the stack, and potentially to show selection status.
     */
    public abstract void drawStackElementsIndicator(Graphics g, CompoundTableModel model, List<CompoundRecord> rec);

    //public abstract void drawStackBorder(Graphics g, CompoundTableModel model, List<CompoundRecord> rec);

    public abstract StackDrawingConfig getStackDrawingConfig();
    public abstract void setStackDrawingConfig(StackDrawingConfig stackDrawingConfig);

    /**
     * Draw stack, assuming that the left upper corner of the card is at 0/0 coordinate.
     *
     * @param g
     * @param model
     * @param rec
     */
    public void drawStack(Graphics g, CompoundTableModel model, int focusListFlag, List<CompoundRecord> rec, CardElement ce){
        drawStackBackground(g,model,rec);
        drawStackSurface(g,model,focusListFlag,rec,ce);
        drawStackElementsIndicator(g,model,rec);
        drawStackBorder(g,model,rec);
    }

    public abstract AbstractStackDrawer clone();

}
