package com.actelion.research.datawarrior.task.view.cards;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.AbstractTaskWithoutConfiguration;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.view.JCardView;
import com.actelion.research.table.view.card.CardElement;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * This class provides the task to create substacks from card elements.
 *
 * The SubstackCriterion defines the splitting criterion. The parameter mCriterionParameter specifies the
 * input to the criterion. The following criteria are supported:
 *
 * Category: Then the parameter is an Integer object and indicates the column ID of the category
 * Hitlist:  Then the parameter is an Integer object and indicates the hitlist ID of the hitlist
 *
 *
 */
public class DETaskCreateSubstacks extends AbstractTaskWithoutConfiguration {

    public static final String TASK_NAME = "Create Substacks";

    public enum SubstackCriterion { CATEGORY , HITLIST };

    private DECardsViewHelper mCardsViewHelper;

    private DECardsViewHelper.Applicability mApplicability;

    private Point2D mMouseCardPanePos;

    private SubstackCriterion mCriterion;

    private Object mCriterionParameter;

    public DETaskCreateSubstacks(Frame owner, JCardView cardView, Point2D mouseCardPanePos , DECardsViewHelper.Applicability applicability , SubstackCriterion criterion , Object criterionParameter) {
        super(owner,false);

        mCardsViewHelper = new DECardsViewHelper(cardView);
        mApplicability   = applicability;

        mMouseCardPanePos = mouseCardPanePos;

        mCriterion = criterion;
        mCriterionParameter = criterionParameter;
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


        List<CompoundRecord> crList = null;

        switch(mApplicability){
            case ALL:
                crList = mCardsViewHelper.getNonexclucdedCompoundRecords();
                // !! And later we have to select all card elements to make the function
                //    JCardPane::arrangeSelectedCardElements(..) arrange them!
                break;
            case SELECTED:
                crList = mCardsViewHelper.getSelectedCompoundRecords();
                break;
        }

        if(crList.size()==0) {
            System.out.println("[INFO] DETaskCrreateSubstacks : no cards selected --> return");
            return;
        }

        double cw = mCardsViewHelper.getCardWidth(); double ch = mCardsViewHelper.getCardHeight();

        switch(mCriterion) {
            case CATEGORY:
                Integer column = (Integer) mCriterionParameter;

                List<Integer> categoryList = crList.stream().mapToInt( cr -> (int) cr.getDouble(column) ).distinct().boxed().collect(Collectors.toList()) ;

                // remove old ones:
                mCardsViewHelper.getCardPane().getCardPaneModel().removeRecords(crList);

                // position:
                int py  = (int) mMouseCardPanePos.getY();
                int px  = (int) mMouseCardPanePos.getX();
                int pdx = (int) (1.2 * mCardsViewHelper.getCardPane().getStackDrawer().getStackDrawingConfig().getStackWidth() );


                // determine the x position (take the most right of ALL not removed cards):
                double max_x = 0;
                if(mCardsViewHelper.getCardPane().getCardPaneModel().getAllElements().size()==0) {
                    // then we don't have to do anything..
                }
                else {
                    max_x = mCardsViewHelper.getCardPane().getCardPaneModel().getAllElements().stream().mapToDouble(ci -> ci.getPosX()).max().getAsDouble();
                }

                double start_x = max_x + 4*cw;

                // sort categories:
                categoryList.sort(Integer::compare);

                // add new stacks
                List<CardElement> new_stacks = new ArrayList<>(); // we collect them to set the viewport afterwards
                int cnt = 0;
                for( Integer cat : categoryList ){
                    List<CompoundRecord> oci = crList.stream().filter( cr -> ((int) cr.getDouble(column))==cat.intValue() ).collect(Collectors.toList());
                    //mCardsViewHelper.getCardPane().getCardPaneModel().addCE(oci,px+pdx*cnt,py);
                    CardElement ce_new = mCardsViewHelper.getCardPane().getCardPaneModel().addCE(oci,start_x+pdx*cnt,py);
                    new_stacks.add(ce_new);
                    cnt++;
                }

                // select the newly created card elements!
                mCardsViewHelper.getCardPane().setSelection(new_stacks);

                mCardsViewHelper.getCardPane().arrangeSelectedCardElements(false,1,true);
                mCardsViewHelper.getCardPane().repaint();
//                mCardsViewHelper.getCardPane().setViewportAroundSpecificCardElements(new_stacks,0.2);

                break;

            case HITLIST:

                // two possible modes how this can work (see DWC-40)
                boolean justCreateTwoStacks = true; // true, simpler mode, false, more complicated..

                Integer flagIdx = (Integer) mCriterionParameter;

                // loop over all CEs and sort them into two stacks..
                List<CompoundRecord> crInHitlist  = new ArrayList<>();
                List<CompoundRecord> crNotInHitlist = new ArrayList<>();

                for(CompoundRecord cr : crList) {
                    if(cr.isFlagSet(flagIdx)){crInHitlist.add(cr);}
                    else{crNotInHitlist.add(cr);}
                }


                if(justCreateTwoStacks){
                    // now just create two stacks..
                    mCardsViewHelper.getCardPane().getCardPaneModel().removeRecords(crInHitlist);
                    mCardsViewHelper.getCardPane().getCardPaneModel().removeRecords(crNotInHitlist);
                    mCardsViewHelper.getCardPane().getCardPaneModel().addCE(crInHitlist, mMouseCardPanePos.getX() - 0.6 * cw, mMouseCardPanePos.getY() );
                    mCardsViewHelper.getCardPane().getCardPaneModel().addCE(crNotInHitlist, mMouseCardPanePos.getX() + 0.6 * cw, mMouseCardPanePos.getY() );
                }
                else{
                    // create one new stack..
                    mCardsViewHelper.getCardPane().getCardPaneModel().removeRecords(crInHitlist);
                    mCardsViewHelper.getCardPane().getCardPaneModel().addCE(crNotInHitlist, mMouseCardPanePos.getX() + 0.6 * cw, mMouseCardPanePos.getY() );
                }

                break;


        }


        //System.out.println("NOT YET SUPPORTED!");
    }

    @Override
    public DEFrame getNewFrontFrame() {
        return null;
    }

}
