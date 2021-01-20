package com.actelion.research.datawarrior.task.view.cards;

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.task.AbstractViewTask;
import com.actelion.research.table.view.card.CardElement;
import com.actelion.research.table.view.card.JCardPane;
import com.actelion.research.table.view.card.animators.PathAnimator;
import com.actelion.research.table.view.card.positioning.CardPositionerInterface;
import com.actelion.research.table.view.JCardView;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public abstract class DETaskAbstractPositioningTask extends AbstractViewTask {

    protected JCardPane mCardPane;

    protected CardPositionerInterface mPositioner = null;

    protected java.util.List<CardElement> mElements = new ArrayList<>();

    protected double mTimeAnimation = 0.0;


    public DETaskAbstractPositioningTask(Frame frame, DEMainPane mainPane, JCardView cardView, List<CardElement> cardElements , CardPositionerInterface positioner, double timeAnimation){
        super(frame,mainPane,cardView);

        this.mCardPane = cardView.getCardPane();
        this.mElements = cardElements;
        this.mPositioner = positioner;
        this.mTimeAnimation = timeAnimation;
    }

    public void runPositioning(){
        PathAnimator pa = PathAnimator.createToPositionPathAnimator(mElements,mPositioner,mTimeAnimation);
        //mCardPane.startAnimation(pa);
    }

}
