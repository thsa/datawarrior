package com.actelion.research.datawarrior.task.view.cards;

import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.view.JCardView;
import com.actelion.research.table.view.card.CardElement;
import com.actelion.research.table.view.card.CardPaneModel;
import com.actelion.research.table.view.card.JCardPane;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class DECardsViewHelper {

    JCardView mCardView;



    public enum Applicability { ALL , SELECTED , MOUSE_OVER }

    public DECardsViewHelper(JCardView cardView) {
        mCardView = cardView;
    }

    public JCardPane getCardPane() { return mCardView.getCardPane(); }

    public CardPaneModel<CardElement, CardElement.CardElementFactory> getCardPaneModel() {return this.mCardView.getCardPane().getCardPaneModel(); }

    public List<CardElement> getNonexclucdedCardElements(){
        return mCardView.getCardPane().getCardPaneModel().getReducedAllElements();
    }

    public List<CompoundRecord> getNonexclucdedCompoundRecords() {
        return mCardView.getCardPane().getCardPaneModel().getReducedAllElements().stream().flatMap( cei -> cei.getAllNonexcludedRecords().stream() ).collect(Collectors.toList());
    }

    public List<CompoundRecord> getSelectedCompoundRecords(){
        return mCardView.getCardPane().getSelection().stream().flatMap( ce -> ce.getAllNonexcludedRecords().stream() ).collect(Collectors.toList() );
    }

    public List<CardElement> getSelectedCardElements() {
        return mCardView.getCardPane().getSelection();
    }


    public void addNewCardElements(Point2D pos, List<CompoundRecord> crList) {

        Random r01 = new Random(7643);
        double cw  = getCardPane().getCardDrawer().getCardDrawingConfig().getCardWidth();

        int nDrops = crList.size();

        for( CompoundRecord cri : crList ){
            //sList<CompoundRecord> lcr = licr.getCRs();
            Point2D dropPoint     = new Point2D.Double( pos.getX(),pos.getY() );
            dropPoint.setLocation( dropPoint.getX() + (0.5-r01.nextDouble()) * cw * Math.sqrt( nDrops ) , dropPoint.getY() + (0.5-r01.nextDouble()) * cw * Math.sqrt( nDrops ) );
            getCardPane().addCardElement( cri , dropPoint.getX() , dropPoint.getY() );
        }
    }


    public double getCardWidth(){
        return getCardPane().getCardDrawer().getCardDrawingConfig().getCardWidth();
    }

    public double getCardHeight(){
        return getCardPane().getCardDrawer().getCardDrawingConfig().getCardHeight();
    }



}
