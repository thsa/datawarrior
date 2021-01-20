package com.actelion.research.table.view.card.cardsurface;

import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractCardDrawer {


    /**
     * NOTE! Only draws the background of the card SURFACE! Shadow, stacks etc.. are handled by the
     * CardElementBackgroundDrawer!
     *
     * @param g
     * @param model
     * @param rec
     */
    public abstract void drawCardBackground(Graphics g, CompoundTableModel model, CompoundRecord rec);

    public abstract void drawCardSurface(Graphics g, CompoundTableModel model, CompoundRecord rec);


    public abstract void drawCardBorder(Graphics g, CompoundTableModel model, CompoundRecord rec);

    /**
     * Draw card, assuming that the left-upper corner of the card is at 0/0 coordinate.
     *
     * @param g
     * @param model
     * @param rec
     */
    public void drawCard(Graphics g, CompoundTableModel model, CompoundRecord rec){
        drawCardBackground(g,model,rec);
        drawCardSurface(g,model,rec);
        drawCardBorder(g,model,rec);
    }

    public abstract CardDrawingConfig getCardDrawingConfig();
    public abstract void setCardDrawingConfig(CardDrawingConfig cardDrawingConfig);

    public void initializeAllPanels(Graphics g, CardDrawingConfig conf, CompoundTableModel model, List<CompoundRecord> records){
        for(CardDrawingConfig.CardSurfaceSlot css : this.getCardDrawingConfig().getSlots() ){
            // only initialize with valid datalink config:
            boolean configOk = false;
            if( css.getColumns().length >= css.getSurfacePanel().getNumberOfDataColumnsRequired() ){
                if( Arrays.stream(css.getColumns()).reduce(Integer::min).orElseGet( () -> 0 ) >= 0 ){
                    configOk = true;
                }
            }
            if(configOk) {
                css.getSurfacePanel().initalizeCardPanel(g, conf, model, records, css.getColumns(), (int) conf.getCardWidth(), (int) conf.getCardHeight());
            }
        }
    }

    public abstract AbstractCardDrawer clone();

}
