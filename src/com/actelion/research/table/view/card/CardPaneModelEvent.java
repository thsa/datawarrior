package com.actelion.research.table.view.card;

import com.actelion.research.table.model.CompoundRecord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CardPaneModelEvent {

    public static enum EventType { NEW_CARD_ELEMENT , DELETED_CARD_ELEMENT , CARD_ELEMENT_CHANGED };

    private CardPaneModelEvent.EventType mEventType;
    private CardPaneModel.CardElementInterface mCardElement;

    private List<CompoundRecord> mCompoundRecords = null;

    public CardPaneModelEvent(EventType et, CardPaneModel.CardElementInterface ce){
        this.mEventType = et;
        this.mCardElement = ce;
        this.mCompoundRecords = new ArrayList<>();
    }

    public CardPaneModelEvent(EventType et, CardPaneModel.CardElementInterface ce, Collection<CompoundRecord> crs){
        this.mEventType = et;
        this.mCardElement = ce;
        this.mCompoundRecords = new ArrayList<>(crs);
    }


    public List<CardPaneModel.CardElementInterface> getCards(){
        if(this.mCardElement != null){
            List<CardPaneModel.CardElementInterface> ceL = new ArrayList<>();
            ceL.add(this.mCardElement);
            return ceL;
        }
        return new ArrayList<CardPaneModel.CardElementInterface>();
    }

    public EventType getType(){return this.mEventType;}

    /**
     * For DELETED_CARD_ELEMENT, this contains the compound records that the card contained.
     *
     * @return
     */
    public List<CompoundRecord> getCompoundRecords(){ return this.mCompoundRecords; }

}
