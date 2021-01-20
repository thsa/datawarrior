package com.actelion.research.datawarrior.task.view.cards;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.AbstractTaskWithoutConfiguration;
import com.actelion.research.table.view.card.CardPaneEvent;
import com.actelion.research.table.view.card.JCardPane;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Properties;

public class DETaskCreateStackFromSelection extends AbstractTaskWithoutConfiguration {

    public static final String TASK_NAME = "Create Stack from Selection";

    //private CompoundListSelectionModel   mSelectionModel;
    private JCardPane                    mCardPane;
    private Point2D                      mStackPos; // position of new stack


    public DETaskCreateStackFromSelection(Frame owner, JCardPane cardPane, Point2D stackPos){
        super(owner,false);

        //this.mSelectionModel = selectionModel;
        this.mCardPane       = cardPane;
        this.mStackPos       = stackPos;

    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public void runTask(Properties configuration) {

//        // 1. create new stack
//        Map<CompoundRecord,CardElement> ceMap = mCardPane.getMap_CompoundRecord2CE();
//        ArrayList<CompoundRecord> crList = new ArrayList<>();
//        for(CompoundRecord cr : ceMap.keySet()){
//            if(cr.isSelected()){
//                crList.add(cr);
//            }
//        }
//
//        // 2. remove all elements
//        for(CompoundRecord cr : crList){
//            if( cr.isSelected() ){
//                CardElement ce = ceMap.get(cr);
//                if(ce.getAllRecords().size()==1){
//                    //mCardPane.getAllCardElements().remove(ce);
//                    mCardPane.getCardPaneModel().removeCE(ce );
//                }
//                else{
//                    //ce.getRecords().remove(cr);
//                    mCardPane.getCardPaneModel().removeRecordFromCE(ce,cr);
//                }
//            }
//        }
//
//        //CardElement newCardElement = mCardPane.createNewCardElement(mStackPos,crList);
//        mCardPane.addCardElement(crList,mStackPos.getX(),mStackPos.getY());

        mCardPane.getCardPaneModel().createStack( mCardPane.getSelection() , mStackPos.getX(),mStackPos.getY() , true );

        mCardPane.fireCardPaneEvent( new CardPaneEvent( CardPaneEvent.EventType.VISIBLE_ELEMENTS_CHANGED , mCardPane.getViewport() ) );

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