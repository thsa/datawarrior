package com.actelion.research.table.view.card.cardsurface;

import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.model.CompoundTableModel;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class StackDrawingConfig {

    private CompoundTableColorHandler mColorHandler = null;

    public StackDrawingConfig(CompoundTableColorHandler colorHandler){
        mColorHandler = colorHandler;
    }

    /**
     * Copy constructor
     */
    public StackDrawingConfig(StackDrawingConfig cdc){
        this.mColorHandler = cdc.getColorHandler();
        this.mConfigChangeListeners = new ArrayList<>(cdc.getConfigChangeListeners());
        this.mDefaultFont = cdc.getDefaultFont();
        this.mSlots = new ArrayList<>();
        for( StackDrawingConfig.StackSurfaceSlot si : cdc.getSlots() ){ this.mSlots.add( (StackDrawingConfig.StackSurfaceSlot) si.clone() ); }
    }



    List<StackDrawingConfig.StackSurfaceSlot> mSlots = new ArrayList<>();

    Font mDefaultFont = new Font("Verdana", Font.BOLD, 10);

    public Font getDefaultFont() {
        return mDefaultFont;
    }

    public List<StackDrawingConfig.StackSurfaceSlot> getSlots(){
        return this.mSlots;
    }

    public double getStackHeight(){
        return 200;
    }

    public double getStackWidth(){
        return 140;
    }


    /**
     * This function initializes automatically intializes the "default card configuration" and then lets the panels
     * propose the corresponding stack configuration.
     *
     * @param ctm
     * @return
     */
    public static StackDrawingConfig createDefaultStackDrawingConfig(CompoundTableModel ctm, CompoundTableColorHandler colorHandler){
        CardDrawingConfig cdConfig = CardDrawingConfig.createDefaultCardDrawingConfig(ctm, colorHandler);

        // we have to draw a card, to make sure that the card is initialized..
        DefaultCardDrawer dcd = new DefaultCardDrawer(ctm,colorHandler);
        dcd.setCardDrawingConfig(cdConfig);
        BufferedImage bimDummy = new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);
        dcd.drawCard(bimDummy.createGraphics(),ctm, ctm.getRecord(0) );

        DefaultStackConfigurationProposer dscp = new DefaultStackConfigurationProposer();
        StackDrawingConfig sdc = dscp.proposeStackConfigurationFromCardConfiguration(cdConfig);

        return sdc;
    }





    public CompoundTableColorHandler getColorHandler(){
        return this.mColorHandler;
    }

    private List<StackDrawingConfig.StackDrawingConfigChangeListener> mConfigChangeListeners = new ArrayList<>();

    public List<StackDrawingConfig.StackDrawingConfigChangeListener> getConfigChangeListeners(){
        return this.mConfigChangeListeners;
    }

    public void registerConfigChangeListener(StackDrawingConfig.StackDrawingConfigChangeListener listener){
        if(!this.mConfigChangeListeners.contains(listener)) {
            this.mConfigChangeListeners.add(listener);
        }
    }

    public void fireConfigChangeEvent(){
        for(StackDrawingConfig.StackDrawingConfigChangeListener cdccl : mConfigChangeListeners){
            if(cdccl!=null) {
                cdccl.configChanged();
            }
        }
    }

    public static interface StackDrawingConfigChangeListener{
        public void configChanged();
    }




    public static class StackSurfaceSlot implements Cloneable {

        double mRelativePosY;
        double mRelativeHeight;

        //int mColumns[];

        AbstractStackSurfacePanel mSurfacePanel;

        public StackSurfaceSlot(AbstractStackSurfacePanel panel, double relativePosY, double relativeHeight) {
            this.mSurfacePanel = panel;
            //this.mColumns = columns;
            this.mRelativePosY = relativePosY;
            this.mRelativeHeight = relativeHeight;
        }

        public double getRelativePosY() {
            return mRelativePosY;
        }

        public double getRelativeHeight() {
            return mRelativeHeight;
        }

        public AbstractStackSurfacePanel getSurfacePanel() {
            return mSurfacePanel;
        }

        public void setRelativePosY(double relativePosY) {
            this.mRelativePosY = mRelativePosY;
        }

        public void setRelativeHeight(double relativeHeight) {
            this.mRelativeHeight = mRelativeHeight;
        }

        public void setSurfacePanel(AbstractStackSurfacePanel surfacePanel) {
            this.mSurfacePanel = mSurfacePanel;
        }

        public int[] getColumns() {
            return this.mSurfacePanel.getColumns();
        }
//        public void setColumn(int columns[]) {
//            this.mColumns = columns;
//        }

        @Override
        public Object clone(){
            StackSurfaceSlot s = new StackSurfaceSlot(this.mSurfacePanel , this.mRelativePosY , this.mRelativeHeight);
            return s;
        }

    }
}
