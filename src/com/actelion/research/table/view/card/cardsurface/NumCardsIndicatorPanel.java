package com.actelion.research.table.view.card.cardsurface;

import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.card.CardElement;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class NumCardsIndicatorPanel extends AbstractStackSurfacePanel {

    public static final String NAME = "Number of Cards";

    private static final int CONF_MAX_PER_LINE = 20;
    private static final int CONF_MAX_LINES    = 2;

    private static final int CONF_GAP_PX       = 2;


    private int mFocusListFlag                 = -1;

    private int mConfPxNumberPos               = 6;
    private int mConfPxNumberEnd               = 40;


    private Paint mPaintBackground       = Color.gray;
    private Paint mPaintBackgroundNumber = Color.lightGray;

    private Color mColSelected   = Color.CYAN.darker().darker();
    private Color mColUnselected = Color.yellow;
    private Color mColGreyedOut  = Color.darkGray;


    private Font mFinalFont      = null;

    private static final Font mBaseFont = new Font("LucidaSans", Font.BOLD, 20);

    @Override
    public void drawPanel(Graphics g, StackDrawingConfig conf, CompoundTableModel model, List<CompoundRecord> rec, CardElement ce , int columns[], int w, int h) {

        int n = rec.size();

        Graphics2D g2 = (Graphics2D) g;

        // draw background:
        g2.setPaint(mPaintBackground);
        g2.fillRect(0,0,w,h);

        // draw number:
        g2.setColor(Color.black);
        g2.setFont(mFinalFont);

        String numStr = ""+rec.size();

        int pxEndStr = g.getFontMetrics().stringWidth(numStr);

        // layout:
        int pxStartIndicator = mConfPxNumberPos+pxEndStr+4*CONF_GAP_PX;
        int pxWidthIndicator = w - CONF_GAP_PX - (mConfPxNumberPos+pxEndStr+CONF_GAP_PX+CONF_GAP_PX);

        g2.setPaint(mPaintBackgroundNumber);
        g2.fillRect(mConfPxNumberPos-CONF_GAP_PX,CONF_GAP_PX,  mConfPxNumberPos+pxEndStr+CONF_GAP_PX, h-2*CONF_GAP_PX);
        g2.setPaint(Color.black);
        g2.drawRect(mConfPxNumberPos-CONF_GAP_PX,CONF_GAP_PX,  mConfPxNumberPos+pxEndStr+CONF_GAP_PX, h-2*CONF_GAP_PX);

        double posTextY = ( h- (h-g.getFontMetrics().getHeight())/2 ) - g.getFontMetrics().getDescent();

        g2.drawString( numStr,mConfPxNumberPos,(int) (CONF_GAP_PX+posTextY) );




        // figure out mode:
        if( n > CONF_MAX_PER_LINE * CONF_MAX_LINES ){


            // draw ratios, i.e. :   greyed-out vs. ngo-selected vs. ngro-nonselected..
//          int n_selected = (int) rec.stream().filter( ri -> ri.isSelected() ).count();

            int n_greyed_out       = 0;
            int n_ngro_selected    = 0;
            int n_ngro_unselected  = 0;

            if(mFocusListFlag>=0) {
                for (CompoundRecord cri : rec) {
                    if (!cri.isFlagSet(mFocusListFlag)){n_greyed_out++;}
                    else{
                        if(cri.isSelected()){n_ngro_selected++;} else { n_ngro_unselected++;}
                    }
                }
            }
            else{
                for (CompoundRecord cri : rec) {
                    if(cri.isSelected()){n_ngro_selected++;} else { n_ngro_unselected++; }
                }
            }



            //double ratio = (1.0*n_selected) / rec.size();

            double ratio_g         = (1.0*n_greyed_out) / rec.size();
            double ratio_ng_sel    = (1.0*n_ngro_selected) / rec.size();
            double ratio_ng_unsel  = (1.0*n_ngro_unselected) / rec.size();



            //int pxA      = (int) ( ratio * pxWidthIndicator );

            int pxA = (int) (  ratio_g      * pxWidthIndicator );
            int pxB = (int) ( (ratio_g + ratio_ng_sel) * pxWidthIndicator );

            g2.setColor(mColGreyedOut);
            g2.fillRect(pxStartIndicator, CONF_GAP_PX , pxA , h - 2*CONF_GAP_PX );

            g2.setColor(mColSelected);
            g2.fillRect(pxStartIndicator+pxA, CONF_GAP_PX , pxB-pxA , h - 2*CONF_GAP_PX );

            g2.setColor(mColUnselected);
            g2.fillRect(pxStartIndicator + pxB , CONF_GAP_PX , pxWidthIndicator-pxB , h - 2*CONF_GAP_PX );
        }
        else{
            // figure out number lines:
            int n_lines = 1 + rec.size() / CONF_MAX_PER_LINE;

            int pxPerLine = (h-(n_lines+1)*CONF_GAP_PX) / n_lines;
            double pxPerBarTot = (w-mConfPxNumberEnd) / CONF_MAX_PER_LINE;
            double pxPerBar    = pxPerBarTot - 1.25;

            int remaining = rec.size();

            for(int zi=0;zi<n_lines;zi++){
                int py = (zi+1) * CONF_GAP_PX + zi * pxPerLine;

                for(int zj=0; zj < Math.min(remaining,CONF_MAX_PER_LINE); zj++){

                    if(rec.get(zj).isSelected() ){
                        g.setColor(mColSelected);
                    }
                    else{
                        g.setColor(mColUnselected);
                    }

                    if(mFocusListFlag>=0) {
                        if (!rec.get(zj).isFlagSet(mFocusListFlag)) {
                            g.setColor(mColGreyedOut);
                        }
                    }

                    int pxi = (int) Math.floor( zj*pxPerBarTot );
                    g.fillRect( mConfPxNumberEnd + pxi , py , (int)Math.ceil(pxPerBar) , pxPerLine);
                }
                remaining -= CONF_MAX_PER_LINE;
            }
        }

        g2.setColor(Color.BLACK);
        g2.drawRect(pxStartIndicator,CONF_GAP_PX,pxWidthIndicator,h-2*CONF_GAP_PX);

        
        // and now we draw the stack name
        g2.setFont(mFinalFont);
        g2.setColor(Color.red.darker());
        g2.drawString(ce.getStackName(),20, (int) (-0.6*h) );
    }



    public void drawPanel_old(Graphics g, StackDrawingConfig conf, CompoundTableModel model, List<CompoundRecord> rec, int columns[], int w, int h) {

        int maxSizeX = 8;
        int sizeY    = 8;
        int n           = rec.size();
        double gap      = 1.0;
        double sizeX    = Math.min( maxSizeX, (1.0*w) / n );
        g.setColor(Color.darkGray.darker());
        g.fillRect(0,0,w,h);
        for( int zi=0;zi<rec.size();zi++){
            if(rec.get(zi).isSelected()){g.setColor(Color.red.darker());} else{g.setColor(Color.orange);}

            int x_pos = (int) ((zi*sizeX)+(zi+1)*gap);
            int y_pos = 0;
            while(x_pos>w){ x_pos -= w; y_pos += sizeY+2; }

            g.fillRect( x_pos , 2+y_pos , (int) sizeX, sizeY );
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public JComponent getLabel() {
        return null;
    }

    @Override
    public JPanel getConfigDialog() {
        return null;
    }

    @Override
    public int getNumberOfDataColumnsRequired() {
        return 0;
    }

    @Override
    public boolean canHandleColumnForSlot(CompoundTableModel model, int slot, int column) {
        System.out.println("Warning: NumCardsIndicatorPanel::canHandleColumnForSlot called..");
        return false;
    }

    @Override
    public void setColumns(int[] dataLinks) {
        System.out.println("Warning: NumCardsIndicatorPanel::setColumns called..");
        // nothing
    }

    @Override
    public int[] getColumns() {
        return new int[0];
    }

    @Override
    public void initalizeStackPanel(Graphics g, StackDrawingConfig conf, CompoundTableModel model, int focusListFlag, List<CompoundRecord> records, int[] columns, int w, int h) {
        this.mFinalFont = FontAndTextHelper.determineFittingFontByShrinking(g, mBaseFont, 3, (int)( mConfPxNumberEnd-mConfPxNumberPos ) );
        mFocusListFlag = focusListFlag;
    }

}
