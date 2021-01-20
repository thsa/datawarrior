package com.actelion.research.table.view.card.cardsurface;

import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DefaultCardDrawer extends AbstractCardDrawer {

    CardDrawingConfig mConfig = null;//new CardDrawingConfig();

    private volatile boolean mReinitRequired = true;

    /**
     * Copy constructor. It constructs a new DefaultCardDrawer with a NEW CardDrawingConfig.
     *
     * @param dcd
     */
    public DefaultCardDrawer( DefaultCardDrawer dcd ){
        this.setCardDrawingConfig( new CardDrawingConfig( dcd.getCardDrawingConfig() ) );
    }

    @Override
    public void drawCardSurface(Graphics g, CompoundTableModel tableModel, CompoundRecord rec) {
        Graphics2D g2 = (Graphics2D) g;

        if(mReinitRequired){
            for( CardDrawingConfig.CardSurfaceSlot slot  : mConfig.getSlots() ){
                slot.getSurfacePanel().initalizeCardPanel(g, mConfig, tableModel, getNonexcludedCompoundRecordsFromTable(tableModel), slot.getColumns(), (int)  mConfig.getCardWidth(), (int) (mConfig.getCardHeight() * slot.getRelativeHeight()) );
            }
            mReinitRequired = false;
        }

        //System.out.println("CardDrawer:: draw "+mConfig.getSlots()+" slots");
        for( CardDrawingConfig.CardSurfaceSlot slot  : mConfig.getSlots() ){
             double height = slot.getRelativeHeight() * mConfig.getCardHeight();
             double width  = mConfig.getCardWidth();
             int    ypos   = (int) ( slot.getRelativePosY()*mConfig.getCardHeight() );
             g.translate(0,  ypos );
             slot.getSurfacePanel().drawPanel(g,mConfig,tableModel,rec,slot.getColumns(),(int)width,(int)height);
             g.translate(0, -ypos );
        }

        //drawCardBorder(g);

        // move to CardPane::paintComponent
//        if(rec.isSelected()){
//            System.out.println("DRAW SELECTED!");
//            g2.setColor(Color.orange.darker());
//            g2.setStroke(new BasicStroke(20));
//            g2.drawRect(0,0,(int) mConfig.getCardWidth(),(int) mConfig.getCardHeight());
//        }

    }

    @Override
    public void drawCardBackground(Graphics g, CompoundTableModel model, CompoundRecord rec) {
        Graphics2D g2 = (Graphics2D) g;
        int w = (int) mConfig.getCardWidth();
        int h = (int) mConfig.getCardHeight();

        GradientPaint gradient = new GradientPaint(0, 0, Color.white.darker(), w/2, h, Color.lightGray.brighter());
        g2.setPaint(gradient);
        g2.fillRoundRect(0,0,w,h,4,10);
        g2.setColor(Color.black);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(0,0,w,h,4,10);
    }


    public void drawCardBorder(Graphics g, CompoundTableModel model, CompoundRecord rec){
        int w = (int) mConfig.getCardWidth();
        int h = (int) mConfig.getCardHeight();
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.black);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(0,0,w,h,4,10);
    }


    @Override
    public CardDrawingConfig getCardDrawingConfig() {
        return mConfig;
    }

    public void setCardDrawingConfig(CardDrawingConfig cardDrawingConfig) {
        mConfig = cardDrawingConfig;
        if(cardDrawingConfig.getSlots().size()==0){
            System.out.println("initialized with empty card drawing config..");
        }
        mReinitRequired = true;
    }

//    public double getCardWidth(){
//        return mConfig.getWidth();
//    }
//    public double getCardHeight(){
//        return mConfig.getHeight();
//    }


    public DefaultCardDrawer(CompoundTableModel ctm, CompoundTableColorHandler ctch){
        mConfig = new CardDrawingConfig(ctch);
        //mConfig = CardDrawingConfig.createDefaultCardDrawingConfig(ctm,ctch);
        //mConfig.fireConfigChangeEvent();
    }


    //public DefaultCardDrawer(){
    //}


    public static List<CompoundRecord> getNonexcludedCompoundRecordsFromTable(CompoundTableModel model){
        ArrayList<CompoundRecord> records = new ArrayList<>(model.getRowCount());
        for(int zi=0;zi<model.getRowCount();zi++){
            //records.set(zi,model.getTotalRecord(zi));
            records.add(model.getRecord(zi));
        }
        return records;
    }

    public static List<CompoundRecord> getAllCompoundRecordsFromTable(CompoundTableModel model){
        ArrayList<CompoundRecord> records = new ArrayList<>(model.getTotalRowCount());
        for(int zi=0;zi<model.getTotalRowCount();zi++){
            //records.set(zi,model.getTotalRecord(zi));
            records.add(model.getTotalRecord(zi));
        }
        return records;
    }

    /**
     * Clones both the drawer and the config
     *
     * @return
     */
    public DefaultCardDrawer clone() {
        return new DefaultCardDrawer(this);
    }
}

