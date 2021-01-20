package com.actelion.research.table.view.card.positioning;

import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.card.CardElement;
import com.actelion.research.table.view.card.CardPaneModel;
import com.actelion.research.table.view.card.tools.IdentityHashedObject;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class SpiralOutPositioner extends AbstractCardPositioner {


    public static final String MODE_SPIRAL_OUT = "SPIRAL_OUT";

    private int mColumn = -1;

    /**
     * We compute an "optimal distance between cards", and this value scales the actual distance.
     */
    private double mCardSpread = 1.0;


    /**
     * We compute an "optimal spiraling out value", and this value scales the actual spiraling out.
     */
    private double mSpiralSpread = 1.0;


    public void setCardSpread( double cardSpread ) { this.mCardSpread = cardSpread; }

    public void setSpiralSpread( double spiralSpread ) { this.mSpiralSpread = spiralSpread ; }

    public SpiralOutPositioner(){


    }

    public void setColumn(int col){
        this.mColumn = col;
    }


    @Override
    public List<Point2D> positionAllCards(CompoundTableModel tableModel, List<CardElement> cards) throws InterruptedException {


        Map<IdentityHashedObject<CardElement>,Double> values = AbstractCardPositioner.extractValues(tableModel,mColumn,cards, ValueExtractionMode.ORDER,NaNHandlingInStacks.DONT_CONSIDER);

        int maxIndex = values.entrySet().stream().mapToInt( ei -> ei.getValue().intValue() ).max().getAsInt();

        CardElement ces_sorted[] = new CardElement[ maxIndex+1 ];

        for( IdentityHashedObject<CardElement> ki  : values.keySet() ){
            //System.out.println("Add at pos "+  values.get(ki));
            ces_sorted[ (int)  values.get(ki).doubleValue()] = ki.get();
        }

        //AbstractCardPositioner.

        CardElement dummy = cards.get(0);
        double cw = dummy.getRectangle().getWidth();
        double ch = dummy.getRectangle().getHeight();

        // Optimal spiraling out we say is at 1.25 times Max(cw,ch):
        double optSpirOut = 1.25 * Math.max(cw,ch);

        // Optimal "spread" is at 1.5 times the distance from the center to the corner of the card (this should fit roughtly..)
        double optSpread  = (Math.sqrt( cw*cw + ch*ch )*0.5) * 1.5;


        // and add "tweaking" via parameters:
        double finalSpread  = this.mCardSpread   * optSpread;
        double finalSpirOut = this.mSpiralSpread * optSpirOut;

        // spiral them..
        // we always compute the rad to rotate based on OptSpread and the currentRadius.
        // then we cotinuously increase the currentRadius.

        // we need some kind of reasonable initial radius
        double currentRadius   = 0.5*Math.max(cw,ch);
        double currentRotation = 0.0;

        //double px = 0; double py = 0;
        List<Point2D> positions = new ArrayList<>();

        for(int zi=0;zi<ces_sorted.length;zi++){
            double radToRotate = finalSpread / currentRadius; // 2 Pi cancel out

            currentRotation += radToRotate;
            currentRotation = currentRotation % ( 2 * Math.PI ) ; // this does work(?)
            positions.add( new Point2D.Double( Math.sin(currentRotation) * currentRadius , Math.cos(currentRotation) * currentRadius ) );

            currentRadius +=  (radToRotate / (2*Math.PI) ) * finalSpirOut ;
        }

        // quick sanity check lol!
        if(positions.size() != cards.size()){
            throw new Error("SpiralOutPositioner -> Something went very wrong.. number of positions is wrong");
        }

        System.out.println("Done computing spiral positions!");

        return positions;
    }




    @Override
    public Point2D positionSingleCard(CardPaneModel.CardElementInterface ce, CompoundRecord cr) {
        return null;
    }

    @Override
    public boolean requiresTableModel() {
        return true;
    }

    @Override
    public boolean supportPositionSingleCard() {
        return false;
    }

    @Override
    public boolean shouldCreateStacks() {
        return false;
    }

    @Override
    public void configure(Properties properties) {

    }

    @Override
    public Properties getConfiguration() {
        return null;
    }

}
