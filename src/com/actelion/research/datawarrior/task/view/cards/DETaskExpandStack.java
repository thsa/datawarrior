package com.actelion.research.datawarrior.task.view.cards;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.AbstractTaskWithoutConfiguration;
import com.actelion.research.table.view.card.CardElement;
import com.actelion.research.table.view.card.CardPaneEvent;
import com.actelion.research.table.view.card.JCardPane;
import com.actelion.research.table.model.CompoundRecord;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Properties;
import java.util.function.BiFunction;

public class DETaskExpandStack extends AbstractTaskWithoutConfiguration {

    public static final String TASK_NAME = "Create Stack from Selection";

    //private CompoundListSelectionModel   mSelectionModel;
    private JCardPane mCardPane;

    private CardElement mStack;

    public DETaskExpandStack(Frame owner, JCardPane cardPane, CardElement stack ){
        super(owner,false);

        //this.mSelectionModel = selectionModel;
        this.mCardPane       = cardPane;
        this.mStack          = stack;
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public void runTask(Properties configuration) {

        //mCardPane.getCardPaneModel().createStack( mCardPane.getSelection() , mStackPos.getX(),mStackPos.getY() , true );

        // create new stack elements..
        Point2D pos = mStack.getPos();

        int nx = (int) Math.ceil( Math.sqrt( mStack.getNonexcludedRecords().size() ) );
        int ny = nx;

        // width / height:
        double cw = mCardPane.getCardDrawer().getCardDrawingConfig().getCardWidth();
        double ch = mCardPane.getCardDrawer().getCardDrawingConfig().getCardHeight();

        double gridw = 1.1 * cw * (nx);
        double gridh = 1.1 * ch * (ny);

        double x0    = pos.getX() - 0.5*gridw;
        double y0    = pos.getY() - 0.5*gridh;

        BiFunction<Integer,Integer,Point2D> fPos = (cxi,cyi) -> new Point2D.Double( x0 + cxi * (1.1) * cw  , y0 + cyi * (1.1) * ch );


        // put cards:

        int cx = 0; int cy = 0;
        for( CompoundRecord cr : mStack.getNonexcludedRecords() ){


            CardElement ce_new = mCardPane.getCardPaneModel().addCE( cr );
            Point2D ce_pos     = fPos.apply(cx,cy);
            ce_new.setPosX( ce_pos.getX()); ce_new.setPosY(ce_pos.getY());

            mCardPane.setCardElementToFront(ce_new);
            mCardPane.fireCardElementChanged(ce_new);

            cx++;
            if(cx==nx){
                cx=0;
                cy++;
            }

        }

        // remove stack:
        mCardPane.getCardPaneModel().removeRecordsFromCE(mStack,mStack.getNonexcludedRecords() );
        mCardPane.fireCardPaneEvent(new CardPaneEvent(CardPaneEvent.EventType.VISIBLE_ELEMENTS_CHANGED));

        mCardPane.repaint();
    }


    @Override
    public boolean isConfigurable(){
        return true;
    }

    @Override
    public DEFrame getNewFrontFrame() {
        return null;
    }

}
