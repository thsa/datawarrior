package com.actelion.research.table.view.card.cardsurface;

import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class AbstractCardElementBackgroundDrawer {

    /**
     * Draws background, assuming that the topleft corner of the card is at position 0/0.
     *
     * @param g
     * @param cw
     * @param ch
     * @param nCards
     * @param shadow
     * @param randSeed
     */
    public abstract void drawBackground(Graphics g, int cw, int ch, int nCards, boolean shadow, int randSeed);

     /*
      */

    /**
     * Draws background, assuming that the topleft corner of the card is at position 0/0.
     *
     * @param g
     * @param cw
     * @param ch
     * @param nCards
     * @param shadow
     * @param randSeed
     */
    public abstract void drawBackground_hq(Graphics g, int cw, int ch, int nCards, boolean shadow, int randSeed);


//    public BufferedBackgroundImage createBufferedBackgroundImage( Graphics g, int cw, int ch, int padding_x, int padding_y , int resolution_x, int resolution_y , int nCards, boolean shadow, int randSeed ){
//
//        BufferedImage bim = new BufferedImage(resolution_x, resolution_y, BufferedImage.TYPE_INT_ARGB);
//
//        Graphics2D g2 = (Graphics2D) bim.createGraphics();
//
//
//        double ratio_x =  ((cw + 2*padding_x)*1.0) / resolution_x;
//        double ratio_y =  ((ch + 2*padding_y)*1.0) / resolution_y;
//
//        AffineTransform at = AffineTransform.getScaleInstance( 1.0/ratio_x , 1.0/ratio_y );
//        //AffineTransform at = AffineTransform.getScaleInstance( ratio_x , ratio_y );
//        at.translate( +padding_x , +padding_y );
//        g2.transform(at);
//
//        this.drawBackground(g,cw,ch,nCards,shadow,randSeed);
//        g2.dispose();
//
//
//        int anchor_x = (int)  ( resolution_x *( (1.0*padding_x)/(2.0*padding_x+cw) )  );
//        int anchor_y = (int)  ( resolution_y *( (1.0*padding_y)/(2.0*padding_y+ch) )  );
//
//        return new BufferedBackgroundImage(bim,anchor_x,anchor_y);
//    }

    public static class BufferedBackgroundImage {

        BufferedImage mBIM = null;

        double mPaddingXL;
        double mPaddingXR;
        double mPaddingYB;
        double mPaddingYT;

        /**
         * padding is in "cardpane pixels".
         *
         * @param bim
         * @param padding_xl
         * @param padding_xr
         * @param padding_yb
         * @param padding_yt
         */
        public BufferedBackgroundImage(BufferedImage bim, double padding_xl, double padding_xr, double padding_yb, double padding_yt) {
            mBIM = bim;

            mPaddingXL = padding_xl;
            mPaddingXR = padding_xr;
            mPaddingYB = padding_yb;
            mPaddingYT = padding_yt;
        }

        public BufferedImage getBIM() {
            return mBIM;
        }

        public double getPaddingXL() {
            return mPaddingXL;
        }

        public double getPaddingXR() {
            return mPaddingXR;
        }

        public double getPaddingYB() {
            return mPaddingYB;
        }

        public double getPaddingYT() {
            return mPaddingYT;
        }
    }

//        /**
//         * Draws the background, assuming the topleft corner of the card is at position 0/0.
//         *
//         * @param g
//         * @param cardCenterX
//         * @param cardCenterY
//         * @param cw
//         * @param ch
//         */
//        public void draw(Graphics g, int cardCenterX, int cardCenterY, int cw, int ch){
//
//            double card_res_x = mBIM.getWidth() - 2*mAnchorPointX;
//            double card_res_y = mBIM.getHeight() - 2*mAnchorPointY;
//
//            double padding_relative_x  = (1.0*mAnchorPointX) / mBIM.getWidth();
//            double padding_relative_y  = (1.0*mAnchorPointY) / mBIM.getHeight();
//
//            double padding_screen_x = padding_relative_x * cw;
//            double padding_screen_y = padding_relative_y * ch;
//
//            g.drawImage(mBIM, (int) -padding_screen_x, (int) -padding_screen_y, (int) (cw+2*padding_screen_x) , (int) (ch+2*padding_screen_y) , null );
//        }
//
//    }

}
