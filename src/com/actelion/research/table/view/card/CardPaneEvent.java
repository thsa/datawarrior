package com.actelion.research.table.view.card;

import java.awt.geom.Rectangle2D;

public class CardPaneEvent {

    public static enum EventType { NEW_CARD_POSITIONS , VISIBLE_ELEMENTS_CHANGED , PROPOSAL_SNAP_EVENT , FINAL_SNAP_EVENT , RESET_SNAP_EVENT , DRAG_RELEASED };

    private EventType mEventType;

    private Rectangle2D mViewport = null;

    public CardPaneEvent(EventType event){
        this.mEventType = event;
    }

    public CardPaneEvent(EventType event, Rectangle2D viewport){
        this.mEventType = event;
        this.mViewport = viewport;
    }



    private CardElement mDraggedCardElement;
    private JCardPane.SnapArea mSnapArea;

    /**
     * Constructor for dragged card snaps to SnapArea event:
     */
    public CardPaneEvent( EventType event , CardElement draggedElement , JCardPane.SnapArea snapArea){
        this.mEventType = event;
        mDraggedCardElement = draggedElement;
        mSnapArea           = snapArea;
    }

    public EventType getType(){ return mEventType;}

    public Rectangle2D getViewport(){ return mViewport;}

    public CardElement getDraggedCardElement(){return mDraggedCardElement;}

    public JCardPane.SnapArea getSnapArea(){return mSnapArea;}

}
