package com.actelion.research.table.view.card.cardsurface.gui;

import com.actelion.research.table.view.card.JCardPane;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.card.cardsurface.*;
import com.actelion.research.table.view.card.tools.DataWarriorLink;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class JCardConfigurationPanel extends JPanel {


    private CompoundTableModel mTableModel;


    private static final int SIZE_X_LEFT = 400;
    private static final int SIZE_X_MIDDLE = 400;
    private static final int SIZE_X_RIGHT = 400;


    private JCardPanelArranger mCardPanelArranger;

    private JSurfacePanelConfigPanel mSurfacePanelConfigPanel;

    private JCardPreviewPanel mCardPreviewPanel;

    private JCardPane mCardPane;


    /**
     * This is the config object
     */
    private CardDrawingConfig mConfig = null;//new CardDrawingConfig();

    /**
     * This is the drawer
     */
    private AbstractCardDrawer mCardDrawer = null;


    //private StructurePanel     mPanel_Structure   = new StructurePanel();
    //private SingleDataPanel    mPanel_SingleData  = new SingleDataPanel();



    public CardDrawingConfig getConfig(){
        return this.mConfig;
    }

    public void setConfig(CardDrawingConfig config){
        this.mConfig = config;
        if(this.mCardPanelArranger!=null) {
            this.mCardPanelArranger.getArrangerTable().recomputeTable();
        }
    }


    public AbstractCardSurfacePanel[] getAllPanels(){
        return new AbstractCardSurfacePanel[] { new StructurePanel(), new SingleDataPanel() , new MultiDataPanel() };
    }



    public CompoundTableModel getTableModel(){
        return mTableModel;
    }

    /**
     * Splits the two config views on the left from the card view on the right
     */
    private JSplitPane mSplitPane_All = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

    /**
     * Splits the two config views
     */
    private JSplitPane mSplitPane_Config = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    ;

    /**
     * Splits the single card from the multiple card view
     */
    private JSplitPane mSplitPane_CardPreview = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    ;


    /**
     * Data Warrior LInk
     */
    private DataWarriorLink mDWL = null;

    //public JCardConfigurationPanel(CompoundTableModel tableModel, JCardPane cardPane , CardDrawingConfig config) {
    public JCardConfigurationPanel( DataWarriorLink dwl, JCardPane cardPane , CardDrawingConfig config) {

        this.mCardPane = cardPane;

        this.mDWL = dwl;

        this.mTableModel = dwl.getCTM();
        this.setConfig(config);

        // init layout
        initLayout();

        this.setVisible(true);
    }

    public void initLayout() {


        this.setLayout(new BorderLayout());
        this.add(mSplitPane_All, BorderLayout.CENTER);
        mSplitPane_All.setDividerSize(6);


        mCardPanelArranger = new JCardPanelArranger(this);
        //mCardPreviewPanel = new JCardPreviewPanel(mTableModel , mCardDrawer,mConfig,null,null);
        //mCardPreviewPanel = new JCardPreviewPanel(mTableModel , mCardPane );
        mCardPreviewPanel = new JCardPreviewPanel(mTableModel , mDWL.getTableColorHandler(),  mCardPane.getCardDrawer().getCardDrawingConfig() ,  mCardPane.getStackDrawer().getStackDrawingConfig() );
        mSurfacePanelConfigPanel = new JSurfacePanelConfigPanel();


        mSplitPane_Config.setLeftComponent(mCardPanelArranger);
        mSplitPane_Config.setRightComponent(mSurfacePanelConfigPanel);

        // "top component" rather than left component
        mSplitPane_CardPreview.setLeftComponent(mCardPreviewPanel);


        mSplitPane_All.setLeftComponent(mSplitPane_Config);
        mSplitPane_All.setRightComponent(mSplitPane_CardPreview);
        this.validate();

        this.mSplitPane_All.setDividerLocation(0.67);
        this.mSplitPane_Config.setDividerLocation(400);


        this.validate();
        this.mCardPreviewPanel.repaint();
    }

    /**
     * Sets the middle panel
     *
     * @param jccp
     */
    public void setConfigurationPanel(AbstractCardSurfacePanel jccp){
        this.mSurfacePanelConfigPanel.setPanelConfig( jccp );
    }


//
//
//    class JCardPreviewPanel extends JPanel implements CardDrawingConfig.CardDrawingConfigChangeListener {
//        private static final int CARD_MARGIN = 20;
//
//        public JCardPreviewPanel() {
//            this.setMinimumSize(new Dimension(400, 800));
//            this.setBackground(Color.green.darker());
//
//            mConfig.registerConfigChangeListener(this);
//        }
//
//
//        @Override
//        public void paintComponent(Graphics g){
//            super.paintComponent(g);
//
//            mCardDrawer.setCardDrawingConfig(mConfig);
//            mCardDrawer.initializeAllPanels(g, mConfig, mTableModel, getAllCompoundRecordsFromTable(mTableModel) );
//
//            Graphics2D g2 = (Graphics2D) g;
//
//
//            double aspectRatio = (1.0*this.getWidth()-(2*CARD_MARGIN) ) / (1.0*this.getHeight() -(2*CARD_MARGIN) );
//            double cardRatio   = mConfig.getCardWidth() / mConfig.getCardHeight();
//
//            int cardX, cardY, cardW, cardH;
//            double transform = 1.0;
//
//            g2.setPaint(new GradientPaint(0,0,Color.darkGray,0,this.getHeight(),Color.darkGray.darker().darker()));
//            g2.fillRect(0,0,this.getWidth(),this.getHeight());
//
//            paintTestBackground(g,new int[]{-1000,1000},new int[]{-1000,1000});
//
//            //System.out.println("Aspect ratio:" + (aspectRatio));
//
//            if(aspectRatio > cardRatio){
//                //height limited
//                cardY = CARD_MARGIN;
//                cardH = this.getHeight()-2*CARD_MARGIN;
//                cardW = (int)( cardH * cardRatio );
//                cardX = (this.getWidth()-cardW)/2;
//                transform = 1.0*cardH / mConfig.getCardHeight();
//            }
//            else{
//                //width limited
//                cardX = CARD_MARGIN;
//                cardW = this.getWidth()-2*CARD_MARGIN;
//                cardH = (int)( cardW / cardRatio );
//                cardY = (this.getHeight()-cardH)/2;
//                transform = 1.0*cardW / mConfig.getCardWidth();
//            }
//
//            // transform
//            //g2.transform(AffineTransform.getTranslateInstance(cardX, cardY);
//            //g2.transform(AffineTransform.getScaleInstance(transform, transform);
//
//            if(mTableModel!=null) {
//                AffineTransform oldTransform = g2.getTransform();
//                AffineTransform atCard = new AffineTransform();
//                atCard.translate(cardX, cardY);
//                atCard.scale(transform,transform);
//                g2.transform(atCard);
//                mCardDrawer.drawCard(g, mTableModel, mTableModel.getTotalRecord(0));
//                mCardDrawer.drawCardBorder(g, mTableModel, mTableModel.getTotalRecord(0) );
//                g2.setTransform(oldTransform);
//            }
//            else{
//                if(false) {
//                    g2.setPaint(new GradientPaint(0, 0, Color.white.darker(), this.getWidth(), this.getHeight(), Color.lightGray));
//                    g2.fillRoundRect(cardX, cardY, cardW, cardH, (int) (0.05 * cardW), (int) (0.05 * cardW));
//                }
//                else{
//                    AffineTransform oldTransform = g2.getTransform();
//                    AffineTransform atCard = new AffineTransform();
//                    atCard.translate(cardX, cardY);
//                    atCard.scale(transform,transform);
//                    g2.transform(atCard);
//                    mCardDrawer.drawCardBackground(g,null,null );
//                    g2.setTransform(oldTransform);
//                }
//            }
//        }
//
//        public void paintTestBackground(Graphics g, int x[], int y[]){
//            int fcnt = 0;
//            for(int xi=x[0];xi<=x[1];xi+=100){
//                for(int yi=x[0];yi<=y[1];yi+=100){
//                    if(fcnt%2==0){g.setColor(Color.DARK_GRAY.darker());}else{g.setColor(Color.DARK_GRAY);}
//                    g.fillRect(xi,yi,100, 100);
//                    g.setColor(Color.green.darker().darker());
//                    g.drawString(""+xi+","+yi,xi,yi);
//                    fcnt++;
//                }
//            }
//        }
//
//        @Override
//        public void configChanged() {
//            this.repaint();
//        }
//    }

    class JSurfacePanelConfigPanel extends JPanel {
        public JSurfacePanelConfigPanel() {
            this.setMinimumSize(new Dimension(200, 800));
            this.setBackground(Color.darkGray);
        }

        public void setPanelConfig(AbstractCardSurfacePanel panel){
            this.removeAll();
            this.setLayout(new BorderLayout());
            if(panel.getConfigDialog()!=null) {
                this.add(panel.getConfigDialog(), BorderLayout.CENTER);
            }
            revalidate();
        }

    }







    public static class AvailablePanels{


    }



    public static void main(String args[]) {
        JFrame f = new JFrame();
        f.getContentPane().setLayout(new BorderLayout());
        //JCardConfigurationPanel jp = new JCardConfigurationPanel(null,null);
        //f.getContentPane().add(jp, BorderLayout.CENTER);
        f.setSize(800, 400);
        f.getContentPane().validate();
        f.setVisible(true);
    }



    public static List<CompoundRecord> getAllCompoundRecordsFromTable(CompoundTableModel model){
        ArrayList<CompoundRecord> records = new ArrayList<>(model.getRowCount());
        for(int zi=0;zi<model.getRowCount();zi++){
            //records.set(zi,model.getTotalRecord(zi));
            records.add(model.getTotalRecord(zi));
        }
        return records;
    }

}