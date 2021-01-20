package com.actelion.research.table.view.card.positioning;

import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.card.CardElement;
import com.actelion.research.table.view.card.CardPaneModel;
import com.actelion.research.table.model.CompoundRecord;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.Properties;

public interface CardPositionerInterface {


    /**
     * Provides two functionalities: (1) it can place existing cards, via the first parameter. (2) it can be used to
     * position newly created cards, based on the second parameter.
     *
     * @param ce
     * @param cr
     * @return
     */
    public Point2D positionSingleCard(CardPaneModel.CardElementInterface ce, CompoundRecord cr);


    /**
     * If this card positioner requires a table model, then calling this function will update all internal data
     * to use the newly supplied table model. I.e. after calling this function, the psotionCard(ce,cr) function must
     * work according to the newly supplied table model.
     *
     * @param tableModel
     */
    public void setTableModel( CompoundTableModel tableModel );

    public List<Point2D> positionAllCards(CompoundTableModel tableModel , List<CardElement> ce) throws InterruptedException;

    /**
     * If this returns true, then the calls to positionSingleCard can only work if a table model was set before.
     *
     * @return
     */
    public boolean requiresTableModel();


    /**
     * If this returns true, it indicates that CardElements which are positioned to exactly the same position should
     * be combined into stacks.
     *
     * @return
     */
    public abstract boolean shouldCreateStacks();



    /**
     * If this returns false, then the positionSingleCard cannot be used.
     *
     * @return
     */
    public boolean supportPositionSingleCard();

    public void configure(Properties properties);
    public Properties getConfiguration();

}
