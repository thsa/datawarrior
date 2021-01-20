package com.actelion.research.table.view.card.cardsurface.gui;


import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.view.card.CardElement;
import com.actelion.research.table.view.card.JCardPane;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.card.cardsurface.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class JCardPreviewPanel extends JPanel implements CardDrawingConfig.CardDrawingConfigChangeListener, StackDrawingConfig.StackDrawingConfigChangeListener {

    private static final int CARD_MARGIN = 20;

    private AbstractCardDrawer mCardDrawer    = null;

    //private CardDrawingConfig mCardConfig     = null;

    private AbstractStackDrawer mStackDrawer  = null;


    /**
     * we can inint the cardPane to null, as we don't use the buffering mechanisms here. I.e. we draw everything with
     * drawBackground_hq(..).
      */
    AbstractCardElementBackgroundDrawer mBackgroundDrawer = new DefaultCardElementBackgroundDrawer(null);


    //private StackDrawingConfig mStackConfig   = null;

    private CompoundTableModel mTableModel = null;

    private CardElement mCardElementToShow = null;

    private JCardPane mCardPane           = null;


    public CardDrawingConfig getCardDrawingConfig(){ return mCardDrawer.getCardDrawingConfig(); }

    public StackDrawingConfig getStackDrawingConfig(){ return mStackDrawer.getStackDrawingConfig(); }


    /**
     * There are two ways of constructing the card preview panel:
     *
     * (i)  you have a JCardPane to supply, then use the other constructor,
     *
     * (ii) you have the drawer / configurations, then use this constructor.
     *
     * @param tableModel
     * @param cardDrawer
     * @param config
     * @param stackDrawer
     * @param stackConfig
     */
    public JCardPreviewPanel(CompoundTableModel tableModel , CompoundTableColorHandler ctch , CardDrawingConfig config , StackDrawingConfig stackConfig ) {
        this.mTableModel = tableModel;
        //this.mCardDrawer = cardDrawer;
        this.mCardDrawer = new DefaultCardDrawer(tableModel,ctch);
        this.mCardDrawer.setCardDrawingConfig(config);
        //this.mCardConfig = config;

        this.mStackDrawer  = new DefaultStackDrawer(tableModel,ctch);
        this.mStackDrawer.setStackDrawingConfig(stackConfig);
        //this.mStackConfig  = stackConfig;

        this.setMinimumSize(new Dimension(400, 800));
        this.setBackground(Color.green.darker());

        if(getCardDrawingConfig()!=null) {
            getCardDrawingConfig().registerConfigChangeListener(this);
            this.mCardElementToShow = new CardElement( this.getCardDrawingConfig() , this.getStackDrawingConfig() , mTableModel.getRecord(0) , new Point2D.Double(0,0) , 1 );
            //this.mCardElementToShow = new CardElement( this.mCardPane, mTableModel.getRecord(0) , new Point2D.Double(0,0) , 1 ); // rectangle and zpos dont matter here, only the record is important
        }
        if(getStackDrawingConfig()!=null) {
            //mStackConfig.registerConfigChangeListener(this);
            int startX = 0; int endX = Math.min( 20 , mTableModel.getTotalRowCount() );
            if(endX < 1){
                // hmm do something, in this case things will go bad if only the StackDrawer is initalized..
            }

            List<CompoundRecord> listCR = new ArrayList<>();
            for(int zi = startX;zi<endX; zi++){ listCR.add( mTableModel.getTotalRecord(zi) ); }
            this.mCardElementToShow = new CardElement( this.getCardDrawingConfig() , this.getStackDrawingConfig() , listCR , new Point2D.Double(0,0) , 1 );
            //this.mCardElementToShow = new CardElement( this.mCardPane , listCR , new Point2D.Double(0,0) , 1 ); // rectangle and zpos dont matter here, only the record is important
        }
    }

//    /**
//     * There are two ways of constructing the card preview panel:
//     *
//     * (i)  you have a JCardPane to supply, then use this constructor,
//     *
//     * (ii) you have the drawer / configurations, then use the other constructor.
//     *
//     *
//     * @param tableModel
//     * @param cardPane
//     */
//    public JCardPreviewPanel( CompoundTableModel tableModel , JCardPane cardPane ) {
//        this(tableModel, cardPane.getCardDrawer() , cardPane.getCardDrawer().getCardDrawingConfig() , cardPane.getStackDrawer() , cardPane.getStackDrawer().getStackDrawingConfig() );
//        this.mTableModel = tableModel;
//        this.mCardPane = cardPane;
//    }

    public void setCardElementToShow(List<CompoundRecord> records){
        this.setCardElementToShow( createCardElement(records) );
    }
    public void setCardElementToShow(CardElement ce){
        this.mCardElementToShow = ce;
    }


    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);

        // @TODO do this more carefully, i.e. show "standby" status..
        if( this.mCardElementToShow.isCard() ) {
            // draw Card:

            if (this.getCardDrawingConfig() == null || this.mCardDrawer == null) {
                g.setColor(Color.darkGray);
                g.fillRect(0, 0, this.getWidth(), this.getHeight());
                return;
            }

            //mCardDrawer.setCardDrawingConfig(mCardConfig);
            mCardDrawer.initializeAllPanels(g, getCardDrawingConfig(), mTableModel, JCardConfigurationPanel.getAllCompoundRecordsFromTable(mTableModel));

            Graphics2D g2 = (Graphics2D) g;

            double aspectRatio = (1.0 * this.getWidth() - (2 * CARD_MARGIN)) / (1.0 * this.getHeight() - (2 * CARD_MARGIN));
            double cardRatio = getCardDrawingConfig().getCardWidth() / getCardDrawingConfig().getCardHeight();

            int cardX, cardY, cardW, cardH;
            double transform = 1.0;

            g2.setPaint(new GradientPaint(0, 0, Color.darkGray, 0, this.getHeight(), Color.darkGray.darker().darker()));
            g2.fillRect(0, 0, this.getWidth(), this.getHeight());

            paintTestBackground(g, new int[]{-1000, 1000}, new int[]{-1000, 1000});

            //System.out.println("Aspect ratio:" + (aspectRatio));

            if (aspectRatio > cardRatio) {
                //height limited
                cardY = CARD_MARGIN;
                cardH = this.getHeight() - 2 * CARD_MARGIN;
                cardW = (int) (cardH * cardRatio);
                cardX = (this.getWidth() - cardW) / 2;
                transform = 1.0 * cardH / getCardDrawingConfig().getCardHeight();
            } else {
                //width limited
                cardX = CARD_MARGIN;
                cardW = this.getWidth() - 2 * CARD_MARGIN;
                cardH = (int) (cardW / cardRatio);
                cardY = (this.getHeight() - cardH) / 2;
                transform = 1.0 * cardW / getCardDrawingConfig().getCardWidth();
            }

            // transform
            //g2.transform(AffineTransform.getTranslateInstance(cardX, cardY);
            //g2.transform(AffineTransform.getScaleInstance(transform, transform);

            if (mTableModel != null) {
                AffineTransform oldTransform = g2.getTransform();
                AffineTransform atCard = new AffineTransform();
                atCard.translate(cardX, cardY);
                atCard.scale(transform, transform);
                g2.transform(atCard);
                //mCardDrawer.drawCard(g, mTableModel, mTableModel.getTotalRecord(0));
                //mCardDrawer.drawCardBorder(g, mTableModel, mTableModel.getTotalRecord(0));
                mBackgroundDrawer.drawBackground_hq(g, (int) mStackDrawer.getStackDrawingConfig().getStackWidth(), (int) mStackDrawer.getStackDrawingConfig().getStackHeight(),this.mCardElementToShow.getAllRecords().size(),true,1);
                mCardDrawer.drawCard(g, mTableModel, this.mCardElementToShow.getAllRecords().get(0) );

                g2.setTransform(oldTransform);

            } else {
                if (false) {
                    g2.setPaint(new GradientPaint(0, 0, Color.white.darker(), this.getWidth(), this.getHeight(), Color.lightGray));
                    g2.fillRoundRect(cardX, cardY, cardW, cardH, (int) (0.05 * cardW), (int) (0.05 * cardW));
                } else {
                    AffineTransform oldTransform = g2.getTransform();
                    AffineTransform atCard = new AffineTransform();
                    atCard.translate(cardX, cardY);
                    atCard.scale(transform, transform);
                    g2.transform(atCard);
                    mCardDrawer.drawCardBackground(g, null, null);
                    g2.setTransform(oldTransform);
                }
            }
        }
        else
        {
            // draw stack..
            if (this.getStackDrawingConfig() == null || this.mStackDrawer == null) {
                g.setColor(Color.darkGray);
                g.fillRect(0, 0, this.getWidth(), this.getHeight());
                return;
            }

            mStackDrawer.setStackDrawingConfig(getStackDrawingConfig());
            //mStackDrawer.initializeAllPanels(g, mStackConfig, mTableModel, getAllCompoundRecordsFromTable(mTableModel));

            Graphics2D g2 = (Graphics2D) g;

            double aspectRatio = (1.0 * this.getWidth() - (2 * CARD_MARGIN)) / (1.0 * this.getHeight() - (2 * CARD_MARGIN));
            double cardRatio = getStackDrawingConfig().getStackWidth() / getStackDrawingConfig().getStackHeight();

            int cardX, cardY, cardW, cardH;
            double transform = 1.0;

            g2.setPaint(new GradientPaint(0, 0, Color.darkGray, 0, this.getHeight(), Color.darkGray.darker().darker()));
            g2.fillRect(0, 0, this.getWidth(), this.getHeight());

            paintTestBackground(g, new int[]{-1000, 1000}, new int[]{-1000, 1000});

            //System.out.println("Aspect ratio:" + (aspectRatio));

            if (aspectRatio > cardRatio) {
                //height limited
                cardY = CARD_MARGIN;
                cardH = this.getHeight() - 2 * CARD_MARGIN;
                cardW = (int) (cardH * cardRatio);
                cardX = (this.getWidth() - cardW) / 2;
                transform = 1.0 * cardH / getStackDrawingConfig().getStackHeight();
            } else {
                //width limited
                cardX = CARD_MARGIN;
                cardW = this.getWidth() - 2 * CARD_MARGIN;
                cardH = (int) (cardW / cardRatio);
                cardY = (this.getHeight() - cardH) / 2;
                transform = 1.0 * cardW / getStackDrawingConfig().getStackWidth();
            }

            // transform
            //g2.transform(AffineTransform.getTranslateInstance(cardX, cardY);
            //g2.transform(AffineTransform.getScaleInstance(transform, transform);

            if (mTableModel != null) {
                AffineTransform oldTransform = g2.getTransform();
                AffineTransform atCard = new AffineTransform();
                atCard.translate(cardX, cardY);
                atCard.scale(transform, transform);
                g2.transform(atCard);
                mBackgroundDrawer.drawBackground_hq(g, (int) mStackDrawer.getStackDrawingConfig().getStackWidth(), (int) mStackDrawer.getStackDrawingConfig().getStackHeight(),this.mCardElementToShow.getAllRecords().size(),true,1);
                mStackDrawer.drawStack(g, mTableModel, -1 , this.mCardElementToShow.getAllRecords() , mCardElementToShow );


                g2.setTransform(oldTransform);

            } else {
                if (false) {
                    g2.setPaint(new GradientPaint(0, 0, Color.white.darker(), this.getWidth(), this.getHeight(), Color.lightGray));
                    g2.fillRoundRect(cardX, cardY, cardW, cardH, (int) (0.05 * cardW), (int) (0.05 * cardW));
                } else {
                    AffineTransform oldTransform = g2.getTransform();
                    AffineTransform atCard = new AffineTransform();
                    atCard.translate(cardX, cardY);
                    atCard.scale(transform, transform);
                    g2.transform(atCard);
                    //mStackDrawer.drawStackBackground(g, null, null);
                    //mBackgroundDrawer.drawBackground(g,cardW,cardH,this.mCardElementToShow.getAllRecords().size(),true,1);
                    g2.setTransform(oldTransform);
                }
            }


        }
    }


    private Random mRandom = new Random();

    public void setShowRandomCard(){
        this.setCardElementToShow( createCardElement( mRandom.nextInt( mTableModel.getTotalRowCount() ) ) );
    }

    public void setShowRandomStack( int maxSize ){
        List<Integer> stacka = new ArrayList<>();
        for(int zi=0;zi<mTableModel.getTotalRowCount();zi++){ stacka.add(zi); }
        Collections.shuffle( stacka );

        stacka = stacka.subList(0,Math.min( stacka.size()-1 , maxSize ));
        this.setCardElementToShow( createCardElement( stacka.stream().map( si -> mTableModel.getTotalRecord( si  ) ).collect(Collectors.toList() ) ) );
    }


    private CardElement createCardElement(int rec){
        List<Integer> lr = new ArrayList<>();
        lr.add(rec);
        return createCardElement( lr.stream().map( si -> mTableModel.getTotalRecord( si ) ).collect( Collectors.toList() ) );
    }

//    private CardElement createCardElement(List<Integer> rec){
//        List<CompoundRecord> records = rec.stream().map( ii -> this.mTableModel.getRecord(ii) ).collect(Collectors.toList());
//        CardElement ce = createCardElement(records);
//        return ce;
//    }

    private CardElement createCardElement(List<CompoundRecord> records){
        //CardElement ce = new CardElement( this.mCardPane, records, new Point2D.Double(0,0) , 1 ); // rectangle and zpos dont matter here, only the record is important
        CardElement ce = new CardElement( this.mCardDrawer.getCardDrawingConfig() , this.mStackDrawer.getStackDrawingConfig() , records, new Point2D.Double(0,0) , 1 ); // rectangle and zpos dont matter here, only the record is important
        return ce;
    }

    public void setCardDrawingConfig( CardDrawingConfig cdc ){
        //this.mCardConfig = cdc;
        this.mCardDrawer.setCardDrawingConfig(cdc);
        this.repaint();
    }

    public void setStackDrawingConfig( StackDrawingConfig sdc){
        this.mStackDrawer.setStackDrawingConfig(sdc);
        this.repaint();
        //this.mStackDrawer.
        //this.mStackDrawer.set
    }

    public void paintTestBackground(Graphics g, int x[], int y[]){
        int fcnt = 0;
        for(int xi=x[0];xi<=x[1];xi+=100){
            for(int yi=x[0];yi<=y[1];yi+=100){
                if(fcnt%2==0){g.setColor(Color.DARK_GRAY.darker());}else{g.setColor(Color.DARK_GRAY);}
                g.fillRect(xi,yi,100, 100);
                g.setColor(Color.green.darker().darker());
                g.drawString(""+xi+","+yi,xi,yi);
                fcnt++;
            }
        }
    }

    @Override
    public void configChanged() {
        this.repaint();
    }

}
