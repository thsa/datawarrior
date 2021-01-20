package com.actelion.research.table.view.card.positioning;

import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.card.CardPaneModel;
import com.actelion.research.table.view.card.JCardPane;
import com.actelion.research.table.model.CompoundRecord;

import java.awt.geom.Point2D;
import java.util.Properties;



public class RandomCardPositioner extends AbstractCardPositioner {

    Point2D   mCenter;
    double    mRadius;

    JCardPane mCardPane = null;

    public RandomCardPositioner(JCardPane cardPane){
        mCenter = new Point2D.Double(200,200);
        mRadius = 400;
        mCardPane = cardPane;
    }


    public void setTableModel(CompoundTableModel tableModel){
        super.setTableModel(tableModel);

        this.mRadius = 100 * Math.sqrt( tableModel.getRowCount() );
    }

    @Override
    public boolean requiresTableModel() {
        return true;
    }

    @Override
    public boolean shouldCreateStacks() {
        return false;
    }

    @Override
    public boolean supportPositionSingleCard() {
        return true;
    }

    @Override
//    public Point2D positionCard(CardElement ce, CompoundRecord cr) {
    public Point2D positionSingleCard(CardPaneModel.CardElementInterface ce, CompoundRecord cr) {


        Point2D center = mCenter;
        double randRadius = Math.random() * mRadius;
        double randAngle = Math.random() * 2 * Math.PI;

        if(mCardPane!=null) {
            center = new Point2D.Double( mCardPane.getViewport().getCenterX() , mCardPane.getViewport().getCenterY() );
        }
        else{

        }

        return new Point2D.Double( (randRadius * Math.cos(randAngle)) + center.getX() , (randRadius * Math.sin(randAngle) ) + center.getY() );
    }

    @Override
    public void configure(Properties properties) {
        this.mCenter = new Point2D.Double( Double.parseDouble(  properties.getProperty("CenterX") ), Double.parseDouble( properties.getProperty("CenterY") ));
        this.mRadius = (double) properties.get("Radius");
    }

    @Override
    public Properties getConfiguration() {
        Properties p = new Properties();

        p.setProperty("CenterX",""+this.mCenter.getX());
        p.setProperty("CenterY",""+this.mCenter.getY());
        p.setProperty("Radius",""+this.mRadius);

        return p;
    }
}
