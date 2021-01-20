package com.actelion.research.datawarrior.task.view.cards;

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.task.view.DETaskAbstractSetViewOptions;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JCardView;
import com.actelion.research.table.view.card.JCardPane;
import com.actelion.research.table.view.card.tools.DataWarriorLink;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Properties;

/**
 *
 * Performance settings:
 *
 * We show two options:
 *
 * Render Hi Quality : means that we can switch to full quality if number of cards to render
 *                     is below RENDERING_NUM_CARDS_FULL. RENDERING_NUM_CARDS_FULL is by default
 *                     set to hundred.
 *
 * Max Cards to render : is the value RENDERING_NUM_CARDS_NORMAL , if we have to draw more than this number
 *                       of cards, then we just draw rectangles and no buffered images.
 *
 *
 */


public class DETaskSetCardViewOptions extends DETaskAbstractSetViewOptions {

    private static final String TASK_NAME = "Set Card View Options..";

    //public static final String KEY_;

    private static final String KEY_HIDE_INVISIBLE_ROWS_IN_OTHER_VIEWS = "HideRowsInOtherViews";

    private static final String KEY_RENDER_HQ          = "RenderHQ";

    private static final String KEY_RENDER_HQ_UB       = "RenderHqUB";
    private static final String KEY_RENDER_NORMAL_UB   = "RenderNormalUB";

    private static final String KEY_RENDER_SHADOW      = "RenderShadow";

    private static final String KEY_RENDER_BIM_RES     = "RenderBIMResolution";

    private static final String KEY_RENDER_COMPUTE_HIDDEN_CARDS  = "RenderComputeHiddenCards";

    private static final String KEY_RENDER_SMALL_PANE_UPDATE_RATE  = "RenderSmallPaneUpdateRate";

    private static final String KEY_RENDER_NUM_CARD_CACHE_THREADS  = "RenderCardCacheThreads";



    private DataWarriorLink mLink;
    private CompoundTableModel mCTM;
    private JCardPane mCardPane;
    private JCardView mCardView;


    private JCardViewOptionsPane mView;

    public DETaskSetCardViewOptions(Frame owner, DEMainPane mainPane , DataWarriorLink link , JCardPane cardPane) {
        super(owner,mainPane,cardPane.getCardView());

        this.mLink = link;
        this.mCTM = link.getCTM();
        //this.mCardPane = cardPane;

        this.mView = new JCardViewOptionsPane();
    }


    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public String getTaskName() {
        return this.TASK_NAME;
    }

//    @Override
//    public boolean isConfigurationValid(Properties configuration, boolean isLive) {
//        return true;
//    }

//    @Override
//    public void runTask(Properties configuration) {

    @Override
    public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {

        if (view == null || !(view instanceof JCardView))
            return;

        JCardPane cardPaneView = ((JCardView) view).getCardPane();

        MyProperties p = new MyProperties(configuration);

        boolean hideCardsInOtherViews = Boolean.parseBoolean( p.getProperty( KEY_HIDE_INVISIBLE_ROWS_IN_OTHER_VIEWS ) );

        boolean renderHQ            = Boolean.parseBoolean( p.getProperty( KEY_RENDER_HQ ) );
        double numCardsFull         = Double.parseDouble( p.getProperty( KEY_RENDER_HQ_UB   ) );
        double numCardsNormal       = Double.parseDouble( p.getProperty( KEY_RENDER_NORMAL_UB) );
        boolean enableShadows       = Boolean.parseBoolean( p.getProperty( KEY_RENDER_SHADOW) );
        double renderPixels         = Double.parseDouble( p.getProperty( KEY_RENDER_BIM_RES ) );
        boolean computeHiddenCards  = Boolean.parseBoolean( p.getProperty( KEY_RENDER_COMPUTE_HIDDEN_CARDS ));
        //int  fastPaneMillisec       = (int) (1000.0 / Double.parseDouble( p.getProperty( KEY_RENDER_SMALL_PANE_UPDATE_RATE ) ));
        int numCardCacheThreads    = p.getInt(KEY_RENDER_NUM_CARD_CACHE_THREADS);

        if(false) {
            System.out.println("HiQuality: " + renderHQ);
            System.out.println("Resolution: " + renderPixels);
            System.out.println("Set compute hidden cards to: " + computeHiddenCards);
        }

        cardPaneView.setSwitchToHiQuality(renderHQ);
        cardPaneView.setNumCards_HQ((int)   numCardsFull);
        cardPaneView.setNumCards_SUPERFAST((int) numCardsNormal);
        cardPaneView.setDisableShadowsEnabled(!enableShadows);
        cardPaneView.setPixelDrawingPerformance((int) renderPixels);
        cardPaneView.setComputeHiddenCardElementsEnabled(computeHiddenCards);
        //cardPaneView.getCardView().getFastCardPane().setUpdateRate( fastPaneMillisec );
        cardPaneView.setCardCacheThreadCount(numCardCacheThreads);

        cardPaneView.getCardPaneModel().setActiveExclusion(hideCardsInOtherViews);

        if(!isAdjusting){
            cardPaneView.reinitAllCards();
        }
    }




    @Override
    public void addViewConfiguration(CompoundTableView view, Properties configuration) {

        JCardView cardView = (JCardView)view;
        JCardPane cardPane = cardView.getCardPane();


        boolean hideRowsInOtherViews = cardPane.getCardPaneModel().isRowHiding();

        boolean render_hq = cardPane.isSwitchToHiQuality();

        int numCardsFull    = cardPane.getNumCards_HQ();
        int numCardsNormal  = cardPane.getNumCards_SUPERFAST();

        boolean enableShadows = !cardPane.isDisableShadowsEnabled();

        double renderPixels = cardPane.getCachedCardProviderNumberOfPixelsToDraw();

        boolean computeHiddenCards = cardPane.isComputeHiddenCardsEnabled();

        //int fastPaneUpdateRate = cardView.getFastCardPane().getUpdateRate();

        int numCardCacheThreads = cardPane.getCardCacheThreadCount();


        Properties p = configuration;

        p.setProperty( KEY_HIDE_INVISIBLE_ROWS_IN_OTHER_VIEWS , ""+hideRowsInOtherViews );

        p.setProperty( KEY_RENDER_HQ , ""+render_hq);

        p.setProperty( KEY_RENDER_HQ_UB       , ""+numCardsFull );
        p.setProperty( KEY_RENDER_NORMAL_UB   , ""+numCardsNormal );


        //int resolution = (int)(   200000 + 4000000.0 * Math.exp( (this.jSliderRenderResolution.getValue() / 100.0 ) ) );
        p.setProperty( KEY_RENDER_BIM_RES     , ""+renderPixels );


        p.setProperty( KEY_RENDER_COMPUTE_HIDDEN_CARDS , ""+computeHiddenCards );
        p.setProperty( KEY_RENDER_SHADOW , ""+enableShadows );

        //p.setProperty( KEY_RENDER_SMALL_PANE_UPDATE_RATE , ""+fastPaneUpdateRate );
        p.setProperty( KEY_RENDER_NUM_CARD_CACHE_THREADS , ""+numCardCacheThreads );

    }

    @Override
    public void addDialogConfiguration(Properties configuration) {
        mView.addDialogConfiguration(configuration);
    }

    @Override
    public void setDialogToConfiguration(Properties configuration) {
        mView.setDialogConfiguration(configuration);
    }

    @Override
    public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {

        return true;
    }



    @Override
    public void enableItems() {
        mView.jCheckBoxShadow.setEnabled(false);
    }

    @Override
    public void setDialogToDefault() {
        mView.setDialogConfigurationToDefault();
    }

    @Override
    public boolean isViewTaskWithoutConfiguration() {
        return false;
    }

    @Override
    public JComponent createViewOptionContent() {
        return mView;
    }

    class JCardViewOptionsPane extends JPanel implements ChangeListener, ItemListener {

        private JTabbedPane mTabbedPane = new JTabbedPane();

        private JPanel      pRowHiding  = new JPanel();
        private JPanel      pRendering  = new JPanel();


        // row hiding:
        private JCheckBox jCheckBoxRowHiding = new JCheckBox();

        // render config:
        private JCheckBox jCheckBoxHiQuality          = new JCheckBox();
        //private JSlider   jSliderRenderHQ             = new JSlider(0,400);
        private JSlider   jSliderRenderNormal         = new JSlider(400,4000);
        //private JSlider   jSliderRenderResolution     = new JSlider(0,100);

        private JCheckBox jCheckBoxComputeHiddenCards = new JCheckBox();
        private JCheckBox jCheckBoxShadow             = new JCheckBox();

        //private JSlider jSliderRenderSmallPaneUpdateRate  = new JSlider(1,40);
        //private JSlider jSliderRenderCardCacheThreads     = new JSlider(1,6);



        public JCardViewOptionsPane() {

            double p = TableLayout.PREFERRED;

            this.setLayout(new BorderLayout());
            this.add(mTabbedPane,BorderLayout.CENTER);

            mTabbedPane.addTab("Row Hiding",pRowHiding);
            mTabbedPane.addTab("Rendering",pRendering);

            //-- init row hiding tab---------------
            double layout_rh[][] = new double[][] { {4,p,8,p,4} , {4,p,4}  };
            pRowHiding.setLayout( new TableLayout(layout_rh) );

            pRowHiding.add( new JLabel( "Hide invisible rows in other plots" )  , "1,1" );
            pRowHiding.add( jCheckBoxRowHiding  , "3,1" );

            // init render config
            double layout_render[][] = new double[][] { { 4,p,8,p,4 } , {4,p,8,p,8,p,8,p,8,p,8,p,8,p,4}  };

            pRendering.setLayout(new TableLayout( layout_render ));

//            pRendering.add( new JLabel("Threshold HQ Rendering") ,"1,1");
//            pRendering.add( jSliderRenderHQ ,"3,1");
            pRendering.add( new JLabel("HiQuality Rendering") ,"1,1");
            pRendering.add( jCheckBoxHiQuality ,"3,1");

            //pRendering.add( new JLabel("Threshold Normal Rendering") ,"1,3");
            pRendering.add( new JLabel("Max Cards to Draw") ,"1,3");
            pRendering.add( jSliderRenderNormal ,"3,3");

            //pRendering.add( new JLabel("Render Resolution") ,"1,5");
            //pRendering.add( jSliderRenderResolution ,"3,5");

            pRendering.add( new JLabel("Compute Hidden Cards") , "1,7" );
            pRendering.add( jCheckBoxComputeHiddenCards , "3,7" );

            pRendering.add( new JLabel("Render Shadows") , "1,9" );
            pRendering.add( jCheckBoxShadow , "3,9" );

//            pRendering.add( new JLabel("Small Pane Update Rate") , "1,11");
//            pRendering.add( jSliderRenderSmallPaneUpdateRate , "3,11" );

//            pRendering.add( new JLabel("Card Caching Threads") , "1,13");
//            pRendering.add( jSliderRenderCardCacheThreads , "3,13" );


            this.addListeners();
        }

        private void addListeners() {

            jCheckBoxComputeHiddenCards.addItemListener(this);
            jCheckBoxShadow.addItemListener(this);
            jCheckBoxRowHiding.addItemListener(this);

            jCheckBoxHiQuality.addItemListener(this);
//            jSliderRenderHQ.addChangeListener(this);
//            jSliderRenderResolution.addChangeListener(this);
//            jSliderRenderCardCacheThreads.addChangeListener(this);
            jSliderRenderNormal.addChangeListener(this);

        }


        @Override
        public void stateChanged(ChangeEvent e) {
            //applyConfiguration(, getDialogConfiguration(), false);
            applyConfiguration( getInteractiveView() , getDialogConfiguration(), false);
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            applyConfiguration( getInteractiveView() , getDialogConfiguration(), false);
        }

        public void addDialogConfiguration(Properties p) {
            //Properties p = new Properties();

            p.setProperty( KEY_HIDE_INVISIBLE_ROWS_IN_OTHER_VIEWS , ""+this.jCheckBoxRowHiding.isSelected() );

            //p.setProperty( KEY_RENDER_HQ_UB       , ""+this.jSliderRenderHQ.getValue() );

            p.setProperty( KEY_RENDER_HQ , ""+this.jCheckBoxHiQuality.isSelected() );
            p.setProperty( KEY_RENDER_HQ_UB       , ""+100 );
            p.setProperty( KEY_RENDER_NORMAL_UB   , ""+this.jSliderRenderNormal.getValue() );


            //int resolution = (int)(   200000 + 4000000.0 * Math.exp( (this.jSliderRenderResolution.getValue() / 100.0 ) ) );
            int resolution = 2000000;

            p.setProperty( KEY_RENDER_BIM_RES     , ""+resolution );


            //p.setProperty( KEY_RENDER_COMPUTE_HIDDEN_CARDS , ""+this.jCheckBoxComputeHiddenCards.isSelected() );
            p.setProperty( KEY_RENDER_COMPUTE_HIDDEN_CARDS , ""+false );

            //p.setProperty( KEY_RENDER_SHADOW , ""+this.jCheckBoxShadow.isSelected() );
            p.setProperty( KEY_RENDER_SHADOW , ""+false );


            //p.setProperty( KEY_RENDER_SMALL_PANE_UPDATE_RATE , ""+this.jSliderRenderSmallPaneUpdateRate.getValue() );
            //p.setProperty( KEY_RENDER_NUM_CARD_CACHE_THREADS , ""+this.jSliderRenderCardCacheThreads.getValue() );
            p.setProperty( KEY_RENDER_NUM_CARD_CACHE_THREADS , ""+1 );
            //return p;
        }

        public void setDialogConfiguration(Properties configuration) {
            MyProperties p = new MyProperties(configuration);


            this.jCheckBoxRowHiding.setSelected( p.getBool(KEY_HIDE_INVISIBLE_ROWS_IN_OTHER_VIEWS) );

            //this.jCheckBoxShadow.setSelected( p.getBool(KEY_RENDER_SHADOW) );
            //this.jCheckBoxComputeHiddenCards.setSelected( p.getBool(KEY_RENDER_COMPUTE_HIDDEN_CARDS) );
            this.jCheckBoxShadow.setSelected( false );
            this.jCheckBoxComputeHiddenCards.setSelected( false );


            //this.jSliderRenderCardCacheThreads.setValue( p.getInt(KEY_RENDER_NUM_CARD_CACHE_THREADS) );
            //this.jSliderRenderSmallPaneUpdateRate.setValue( p.getInt(KEY_RENDER_SMALL_PANE_UPDATE_RATE) );

            this.jCheckBoxHiQuality.setSelected( p.getBool(KEY_RENDER_HQ) );

            //this.jSliderRenderHQ.setValue( p.getInt(KEY_RENDER_HQ_UB) );
            this.jSliderRenderNormal.setValue( p.getInt(KEY_RENDER_NORMAL_UB) );

//            double res_value_x = (double) p.getDouble(KEY_RENDER_BIM_RES);
//            double res_value   = 100 * ( Math.log( (res_value_x / 4000000.0)) );
//            this.jSliderRenderResolution.setValue( (int) res_value );
//            this.jSliderRenderCardCacheThreads.setValue( p.getInt(KEY_RENDER_NUM_CARD_CACHE_THREADS) );

        }

        public void setDialogConfigurationToDefault() {
            this.jCheckBoxHiQuality.setSelected(true);
            this.jCheckBoxComputeHiddenCards.setSelected(false);
//            this.jSliderRenderCardCacheThreads.setValue(2);
//            this.jSliderRenderResolution.setValue(15);
//            this.jSliderRenderHQ.setValue(20);
            this.jSliderRenderNormal.setValue(600);
            //this.jSliderRenderSmallPaneUpdateRate.setValue(10);
            this.jCheckBoxShadow.setSelected(false);
            this.jCheckBoxRowHiding.setSelected(false);
        }







    }

}