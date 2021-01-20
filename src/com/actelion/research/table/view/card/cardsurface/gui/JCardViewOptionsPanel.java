package com.actelion.research.table.view.card.cardsurface.gui;

import com.actelion.research.table.view.card.JCardPane;
import com.actelion.research.table.view.card.JFastCardPane;
import com.actelion.research.table.model.CompoundTableModel;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class JCardViewOptionsPanel extends JPanel {


    private static final String HIDE_ROWS_OPTION = "HideRowsInOtherViews";

    private static final String RENDERING_NUM_CARDS_FULL   = "RenderingNumCardsNoBuffering";
    private static final String RENDERING_NUM_CARDS_NORMAL = "RenderingNumCardsNoBuffering";

    private static final String RENDERING_DISABLE_SHADOWS  = "DisableShadows";

    private static final String RENDERING_PIXEL_PERFORMANCNE     = "RenderingPixelPerformance";

    private static final String RENDERING_COMPUTE_HIDDEN_CARDS   = "RenderingComputeHiddenCards";

    //private static final String RENDERING_REDRAW_THREADS         =
    private static final String RENDERING_FAST_PANE_UPDATE_RATE  =  "RenderingFastPaneUpdateRate"    ; // in ms

    private JCardPane mCardPane;

    private JFastCardPane mFastCardPane;

    private CompoundTableModel mTableModel;


    // GUI:
    JTabbedPane mTabbedPane;


    JComponent mPaneRowHiding;
    JMyOptionsTable mOptionsTableRowHiding;

    JComponent mPaneRendering;
    JMyOptionsTable mOptionsTableRendering;


    public JCardViewOptionsPanel(CompoundTableModel model , JCardPane cardpane , JFastCardPane fastCardpane){
        this.mCardPane = cardpane;
        this.mFastCardPane = fastCardpane;
        this.mTableModel = model;

        initOptionsPanel();
    }

    private void initOptionsPanel(){

        this.setLayout(new BorderLayout());

        mTabbedPane = new JTabbedPane();
        this.add(mTabbedPane,BorderLayout.CENTER);

        initRowHidingPane();
        initRenderingPane();

        mTabbedPane.addTab("Row hiding",mPaneRowHiding);
        mTabbedPane.addTab("Rendering",mPaneRendering);
    }


    private void initRowHidingPane(){
        mPaneRowHiding = new JPanel();
        mPaneRowHiding.setLayout(new BorderLayout());

        //mOptionsTableRowHiding = new JMyOptionsTable(1, 1);
        mOptionsTableRowHiding = new JMyOptionsTable();

        mOptionsTableRowHiding.setCheckBox(0,0,HIDE_ROWS_OPTION,"Hide invisible rows in other views",  mCardPane.getCardPaneModel().isRowHiding());
        //mOptionsTableRowHiding.set
        mOptionsTableRowHiding.reinit();

        mPaneRowHiding.add(mOptionsTableRowHiding,BorderLayout.CENTER);

        mOptionsTableRowHiding.registerOptionTableListener( new MyOptionTableListener_RowHiding() );
        mOptionsTableRowHiding.setValue(0,0, mCardPane.getCardPaneModel().isRowHiding() );
    }


    private void initRenderingPane(){
        mPaneRendering = new JPanel();
        mPaneRendering.setLayout(new BorderLayout());

        //mOptionsTableRendering = new JMyOptionsTable(1,5);
        mOptionsTableRendering = new JMyOptionsTable();

        int numCardsHQ       = this.mCardPane.getNumCards_HQ();
        int numCardsFast     = this.mCardPane.getNumCards_SUPERFAST();

        boolean disableShadows = this.mCardPane.isDisableShadowsEnabled();

        int numPixelsToDraw    = this.mCardPane.getPixelDrawingPerformance();

        mOptionsTableRendering.setSlider(0,0,RENDERING_NUM_CARDS_FULL,"Switch to hq rendering", 0 , 1000 , numCardsHQ);
        mOptionsTableRendering.setSlider(0,1,RENDERING_NUM_CARDS_NORMAL,"Switch to normal rendering", 0 , 2000 , numCardsFast);
        mOptionsTableRendering.setCheckBox(0,2,RENDERING_DISABLE_SHADOWS,"Disable shadows", disableShadows);

        //mOptionsTableRendering.setSlider(0,0,RENDERING_NUM_CARDS_SUPERFAST,"Switch to fast rendering", 0  , 4000 , 8);
        mOptionsTableRendering.setSlider(0,3,RENDERING_PIXEL_PERFORMANCNE,"Buffered images resolution", 200000 , 24000000 , numPixelsToDraw);
        mOptionsTableRendering.setCheckBox(0,4,RENDERING_COMPUTE_HIDDEN_CARDS,"Compute hidden cards", mCardPane.isComputeHiddenCardsEnabled() );
        mOptionsTableRendering.setSlider(0,5,RENDERING_FAST_PANE_UPDATE_RATE,"Small Pane Update Rate", 0 , 40 , 1000.0 / mFastCardPane.getUpdateRate() );

        mOptionsTableRendering.reinit();

        mPaneRendering.add(mOptionsTableRendering,BorderLayout.CENTER);

        mOptionsTableRendering.registerOptionTableListener( new MyOptionTableListener_Rendering() );
    }


    class MyOptionTableListener_RowHiding implements JMyOptionsTable.OptionTableListener {
        @Override
        public void optionTableEventHappened(JMyOptionsTable.OptionTableEvent event) {

            boolean rowHiding = Boolean.parseBoolean( mOptionsTableRowHiding.collectValues().getProperty( HIDE_ROWS_OPTION ) );
            mCardPane.getCardPaneModel().setRowHiding( rowHiding );

        }
    }

    class MyOptionTableListener_Rendering implements JMyOptionsTable.OptionTableListener {
        @Override
        public void optionTableEventHappened(JMyOptionsTable.OptionTableEvent event) {

            Properties renderProperties = mOptionsTableRendering.collectValues();
            double numCardsFull         = Double.parseDouble( renderProperties.getProperty( RENDERING_NUM_CARDS_FULL   ) );
            double numCardsNormal       = Double.parseDouble( renderProperties.getProperty( RENDERING_NUM_CARDS_NORMAL ) );

            boolean disableShadows      = Boolean.parseBoolean( renderProperties.getProperty( RENDERING_DISABLE_SHADOWS ) );

            double renderPixels         = Double.parseDouble( renderProperties.getProperty( RENDERING_PIXEL_PERFORMANCNE ) );

            boolean computeHiddenCards  = Boolean.parseBoolean( renderProperties.getProperty( RENDERING_COMPUTE_HIDDEN_CARDS ));

            int  fastPaneMillisec       = (int) (1000.0 / Double.parseDouble( renderProperties.getProperty( RENDERING_FAST_PANE_UPDATE_RATE ) ));

            mCardPane.setNumCards_HQ((int)   numCardsFull);
            mCardPane.setNumCards_SUPERFAST((int) numCardsNormal);
            mCardPane.setDisableShadowsEnabled(disableShadows);
            mCardPane.setPixelDrawingPerformance((int) renderPixels);
            mCardPane.setComputeHiddenCardElementsEnabled(computeHiddenCards);
            mFastCardPane.setUpdateRate( fastPaneMillisec );
            //mCardPane.getCardPaneModel().setRowHiding( rowHiding );

        }
    }


}
