package com.actelion.research.datawarrior.task.view.cards;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.AbstractTaskWithoutConfiguration;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.view.JCardView;
import com.actelion.research.table.view.card.CardElement;
import com.actelion.research.table.view.card.JCardPane;
import com.actelion.research.table.view.card.positioning.SpiralOutPositioner;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class DETaskExpandStacks extends AbstractTaskWithoutConfiguration {

    public static final String TASK_NAME = "Expand Stacks";

    private DECardsViewHelper mCardsViewHelper;

    private DECardsViewHelper.Applicability mApplicability;

    private Point2D mMouseCardPanePos;

    public DETaskExpandStacks(Frame owner, JCardView cardView, Point2D mouseCardPanePos , DECardsViewHelper.Applicability applicability) {
        super(owner,false);

        mCardsViewHelper = new DECardsViewHelper(cardView);
        mMouseCardPanePos = mouseCardPanePos;
        mApplicability   = applicability;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public void runTask(Properties configuration) {

        List<CardElement> ceList = null;

        switch(mApplicability){
            case ALL:
                ceList = mCardsViewHelper.getNonexclucdedCardElements();
                break;
            case SELECTED:
                ceList = mCardsViewHelper.getSelectedCardElements();
                break;
        }

       //Loop over stacks:

        //List<CompoundRecord> crList = ceList.stream().filter(cxi -> cxi.isStackAfterExclusion() ).flatMap( cei -> cei.getSelectedRecords().stream() ).collect(Collectors.toList());

        for(CardElement cei : ceList){
            if(cei.isStackAfterExclusion()){

                Point2D pi = cei.getPos();
                List<CompoundRecord> crList = cei.getNonexcludedRecords();
                List<CompoundRecord> crList2 = new ArrayList<>(crList);

                if(false) {
                    // remove
                    mCardsViewHelper.getCardPane().getCardPaneModel().removeRecords(crList);
                    // add new:
                    mCardsViewHelper.addNewCardElements(pi, crList2);
                }

                mCardsViewHelper.getCardPane().arrangeSelectedCardElements(true, 1, true);

            }
        }
    }

    @Override
    public DEFrame getNewFrontFrame() {
        return null;
    }
}
