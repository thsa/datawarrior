package com.actelion.research.datawarrior.task.view.cards;

import com.actelion.research.datawarrior.task.AbstractTaskWithoutConfiguration;
import com.actelion.research.table.view.card.JCardPane;

import java.awt.*;

public abstract class DETaskAbstractAlterCardElements extends AbstractTaskWithoutConfiguration {


    protected JCardPane mCardPane;

    public DETaskAbstractAlterCardElements(Frame frame, JCardPane cardPane){
        super(frame,false);
        mCardPane = cardPane;
    }
}
