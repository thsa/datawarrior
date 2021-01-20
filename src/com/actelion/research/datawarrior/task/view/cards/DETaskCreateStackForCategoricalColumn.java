package com.actelion.research.datawarrior.task.view.cards;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.AbstractTaskWithoutConfiguration;
import com.actelion.research.table.view.card.JCardPane;
import com.actelion.research.table.model.CompoundRecord;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class DETaskCreateStackForCategoricalColumn extends AbstractTaskWithoutConfiguration {

    public static final String TASK_NAME = "Create Stack from Selection";

    //private CompoundListSelectionModel   mSelectionModel;
    private JCardPane                    mCardPane;
    private Point2D                      mStackPos; // position of new stack

    private int                          mColumn;


    public DETaskCreateStackForCategoricalColumn(Frame owner, JCardPane cardPane, Point2D stackPos, int column ){
        super(owner,false);

        //this.mSelectionModel = selectionModel;
        this.mCardPane       = cardPane;
        this.mStackPos       = stackPos;
        this.mColumn         = column;
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

        List<CompoundRecord> crList = mCardPane.getCardPaneModel().getAllElements().stream().flatMap(ei -> ei.getAllRecords().stream() ).collect(Collectors.toList() );

        mCardPane.getCardPaneModel().clearAllCEs();

        List<Integer> categoryList = crList.stream().mapToInt( cr -> (int) cr.getDouble(mColumn) ).distinct().boxed().collect(Collectors.toList()) ;

        // position:
        int py  = (int) mStackPos.getY();
        int px  = (int) mStackPos.getX();
        int pdx = (int) (1.2 * mCardPane.getStackDrawer().getStackDrawingConfig().getStackWidth() );


        int cnt = 0;
        for( Integer cat : categoryList ){
            List<CompoundRecord> oci = crList.stream().filter( cr -> ((int) cr.getDouble(mColumn))==cat.intValue() ).collect(Collectors.toList());
            mCardPane.getCardPaneModel().addCE(oci,px+pdx*cnt,py);
            cnt++;
        }
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