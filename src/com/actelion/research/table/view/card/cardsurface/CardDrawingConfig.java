package com.actelion.research.table.view.card.cardsurface;


import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.model.CompoundTableModel;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * This class contains the configuration for the drawing of cards.
 *
 * This class also contains the set of all available card drawing panels
 *
 *
 */
public class CardDrawingConfig {


    CompoundTableColorHandler mColorHandler = null;

    public CardDrawingConfig(CompoundTableColorHandler colorHandler){
        // for debugging:
        //mSlots.add( new CardSurfaceSlot(new StructurePanel(), new int[]{2}, 0, 0.75));
        //mSlots.add( new CardSurfaceSlot(new SingleDataPanel(), new int[]{2}, 0.75, 0.1));
        this.mColorHandler = colorHandler;
    }

    /**
     * Copy constructor
     */
    public CardDrawingConfig(CardDrawingConfig cdc){
        this.mColorHandler = cdc.getColorHandler();
        this.mConfigChangeListeners = new ArrayList<>(cdc.getConfigChangeListeners());
        this.mDefaultFont = cdc.getDefaultFont();
        this.mSlots = new ArrayList<>();
        for( CardSurfaceSlot si : cdc.getSlots() ){ this.mSlots.add( (CardSurfaceSlot) si.clone() ); }
    }

    List<CardSurfaceSlot> mSlots = new ArrayList<>();

    Font mDefaultFont = new Font("Verdana", Font.BOLD, 10);

    public Font getDefaultFont() {
        return mDefaultFont;
    }

    public List<CardSurfaceSlot> getSlots(){
        return this.mSlots;
    }

    public double getCardHeight(){
        return 200;
    }

    public double getCardWidth(){
        return 140;
    }

    public int getMaxNumberOfColumnRequired(){
        int nSlots = mSlots.stream().map((a) -> a.getSurfacePanel().getNumberOfDataColumnsRequired() ).reduce( 0, (a,b) -> Math.max(a,b));
        return nSlots;
    }



    public void setColorHandler(CompoundTableColorHandler colorHandler){
        this.mColorHandler = colorHandler;
        fireConfigChangeEvent();
    }


    public CompoundTableColorHandler getColorHandler(){
        return this.mColorHandler;
    }

    private List<CardDrawingConfigChangeListener> mConfigChangeListeners = new ArrayList<>();

    public List<CardDrawingConfigChangeListener> getConfigChangeListeners(){
        return this.mConfigChangeListeners;
    }

    public void registerConfigChangeListener(CardDrawingConfigChangeListener listener){
        if(!this.mConfigChangeListeners.contains(listener)) {
            this.mConfigChangeListeners.add(listener);
        }
    }

    public void fireConfigChangeEvent(){
        for(CardDrawingConfigChangeListener cdccl : mConfigChangeListeners){
            if(cdccl!=null) {
                cdccl.configChanged();
            }
        }
    }

    public static interface CardDrawingConfigChangeListener{
        public void configChanged();
    }


    public static class CardSurfaceSlot implements Transferable , Cloneable {


        double mRelativePosY;
        double mRelativeHeight;

        //int mColumns[];

        CardDrawingConfig        mCDC;
        AbstractCardSurfacePanel mSurfacePanel;


        public CardSurfaceSlot(CardDrawingConfig cdc, AbstractCardSurfacePanel panel, double relativePosY, double relativeHeight){
            mCDC = cdc;
            mSurfacePanel = panel;
            //mColumns = columns;
            this.mRelativePosY = relativePosY;
            this.mRelativeHeight = relativeHeight;
            mCDC.fireConfigChangeEvent();
        }



        public double getRelativePosY() {
            return this.mRelativePosY;
        }

        public double getRelativeHeight() {
            return mRelativeHeight;
        }

        public AbstractCardSurfacePanel getSurfacePanel() {
            return mSurfacePanel;
        }

        public void setRelativePosY(double relativePosY) {
            this.mRelativePosY = relativePosY;
            mCDC.fireConfigChangeEvent();
        }

        public void setRelativeHeight(double relativeHeight) {
            this.mRelativeHeight = relativeHeight;
            mCDC.fireConfigChangeEvent();
        }

        public void setSurfacePanel(AbstractCardSurfacePanel surfacePanel) {
            this.mSurfacePanel = surfacePanel;
        }

        public int[] getColumns() {
            // ensure that columns array is big enough..
            return this.mSurfacePanel.getColumns();
        }

        public void setColumns(int columns[]){
            this.mSurfacePanel.setColumns(columns);
            this.mCDC.fireConfigChangeEvent();
//            if(this.mSurfacePanel.getNumberOfDataColumnsRequired() != columns.length){
//                System.out.println("CardDrawingConfig : Wrong number of data columns provided! "+columns.length+" instead of "+this.mSurfacePanel.getNumberOfDataColumnsRequired());
//                if(columns.length < this.mSurfacePanel.getNumberOfDataColumnsRequired()){
//                    this.mColumns = new int[this.mSurfacePanel.getNumberOfDataColumnsRequired()];
//                    Arrays.fill(this.mColumns,-1);
//                    for(int zi=0;zi<columns.length;zi++){ this.mColumns[zi] = columns[zi];}
//                }
//            }
//            else {
//                this.mColumns = columns;
//            }
        }

        @Override
        public Object clone(){
            return new CardSurfaceSlot(this.mCDC,this.mSurfacePanel,this.mRelativePosY,this.mRelativeHeight);
        }



        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{ cardSurfacePanelConfigFlavor , DataFlavor.stringFlavor };
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            if(flavor.equals(cardSurfacePanelConfigFlavor)){ return true; }
            if(flavor.equals(DataFlavor.stringFlavor)){ return true; }
            return false;
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if(flavor.equals(DataFlavor.stringFlavor)){ return new String(this.mSurfacePanel.getName());}
            if(flavor.equals(DataFlavor.stringFlavor)){
                return this.clone();
            }
            return null;
        }



    }


    /**
     * This function initializes a structure panel and a multi data panel and tries to find some reasonable data columns
     * that it will link to the panels.
     *
     * @param ctm
     * @return
     */
    public static CardDrawingConfig createDefaultCardDrawingConfig(CompoundTableModel ctm, CompoundTableColorHandler colorHandler){
        CardDrawingConfig cdConfig = new CardDrawingConfig(colorHandler);

        StructurePanel  sp   = new StructurePanel();
        sp.initializePanelConfiguration(ctm);

        if(sp.getColumns()[0]<0){

            MultiDataPanel dp = new MultiDataPanel();
            //dp.setNumberOfColumns();
            dp.initializePanelConfiguration(ctm);

            CardSurfaceSlot cssB = new CardSurfaceSlot(cdConfig, dp, 0.0, 1.0);

            cdConfig.getSlots().add(cssB);

            return cdConfig;

        }
        else {
            MultiDataPanel dp = new MultiDataPanel();
            dp.initializePanelConfiguration(ctm);

            CardSurfaceSlot cssA = new CardSurfaceSlot(cdConfig, sp, 0, 0.75);
            CardSurfaceSlot cssB = new CardSurfaceSlot(cdConfig, dp, 0.75, 0.25);

            cdConfig.getSlots().add(cssA);
            cdConfig.getSlots().add(cssB);

            return cdConfig;
        }
    }



    public static AbstractCardSurfacePanel getCardPanel(String name){

        if(name.equals( StructurePanel.NAME )){  return new StructurePanel(); }
        if(name.equals( SingleDataPanel.NAME )){ return new SingleDataPanel(); }
        if(name.equals( MultiDataPanel.NAME )){  return new MultiDataPanel(); }

        // return null if name could not be resolved
        return null;
    }

    public static class CardSurfacePanelConfigFlavor extends DataFlavor{}

    public static CardSurfacePanelConfigFlavor cardSurfacePanelConfigFlavor = new CardSurfacePanelConfigFlavor();


    public static List<Integer> findAllMatchingColumnsForDataLink(CompoundTableModel model, AbstractCardSurfacePanel panel, int slot){
        ArrayList<Integer> okSlots = new ArrayList<>();
        for(int zi=0;zi<model.getTotalColumnCount();zi++) {
            if( panel.canHandleColumnForSlot(model,slot,zi) ){
                okSlots.add(zi);
            };
        }
        return okSlots;
    }
}
