package com.actelion.research.table.view.card.cardsurface;

import com.actelion.research.table.model.CompoundRecord;

import java.awt.*;

public interface CardDrawingInterface {

    public void drawPanel(Graphics g, CardDrawingConfig conf, CompoundRecord rec , int column, int w, int h);



}
