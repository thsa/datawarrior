package com.actelion.research.table.view.card.cardsurface;

import java.awt.*;

public class FontAndTextHelper {



    /**
     * Shrinks the baseFont until a charactersToShow long word fits into "width"
     *
     * @param baseFont
     * @param charactersToShow
     * @param width
     * @return
     */
    public static Font determineFittingFontByShrinking(Graphics g, Font baseFont , int charactersToShow , float width ){
        return determineFittingFontByShrinking(g, baseFont, charactersToShow, width , Float.POSITIVE_INFINITY);
    }

    /**
     * Shrinks the baseFont until a charactersToShow long word fits into "width"
     *
     * @param baseFont
     * @param charactersToShow
     * @param width
     * @return
     */
    public static Font determineFittingFontByShrinking(Graphics g, Font baseFont , int charactersToShow , float width , float height){
        Font finalFont = null;
        g.setFont(baseFont);
        //float fontSize = ( (1.0f*width) / g.getFontMetrics().getHeight()) * 10; //* 10;
        float fontSize = baseFont.getSize();
        g.setFont( baseFont.deriveFont(Font.PLAIN, fontSize) );
        //determine if we have to shrink more to fit the width.. (assuming ca. 20 characters col. name)

        float fontSizeSmaller = fontSize;
        while( g.getFontMetrics().getWidths()[0] * 1.0 * charactersToShow  > width  || g.getFontMetrics().getHeight() > height ){
            //System.out.println("warning: shrink font more.. "+ g.getFontMetrics().getWidths()[0] * 1.0 * charactersToShow + " target: < "+width + " (w) and "+ g.getFontMetrics().getHeight() +" target: <"+height+ " (h)" );
            // shrink more..
            fontSizeSmaller = fontSizeSmaller * 0.85f;  //( width / (g.getFontMetrics().getWidths()[0] * (1.0f * width) ) ) * g.getFont().getSize();
            g.setFont( g.getFont().deriveFont(Font.PLAIN, fontSizeSmaller) );
            finalFont = g.getFont();
        }
//        if( g.getFontMetrics().getWidths()[0] * 20 > width){
//            System.out.println("warning: shrink font more..");
//            // shrink more..
//            float fontSizeSmaller = ( width / (g.getFontMetrics().getWidths()[0]*20.0f) ) * g.getFont().getSize();
//            g.setFont( g.getFont().deriveFont(Font.PLAIN, fontSizeSmaller) );
//            finalFont = g.getFont();
//        }
        return finalFont;
    }


    /**
     * Shrinks a text such that it fits into "width", given the current Font set in the Graphics object.
     *
     * @param g
     * @param text
     * @param width
     * @return
     */
    public static String shrinkString( Graphics g, String text, int width ){
        String finalText = text;
        if( g.getFontMetrics().stringWidth(text) > width ){
            // shrink:
            String shrinkString = text;
            while( g.getFontMetrics().stringWidth( (shrinkString+"..") ) > width  && shrinkString.length()>=2 ){
                shrinkString = new String( shrinkString.substring(0,shrinkString.length()-2));
            }
            finalText = shrinkString+"..";
        }
        return finalText;
    }


    /**
     * Shrinks a number to fit into "width"
     *
     * @param g
     * @param width
     * @param value
     * @return
     */
    public static String shrinkNumberToWidth(Graphics g, int width, double value){

        int      decimal = 7; // one more than we maximally wanna try..
        boolean  lengthOk = false;
        while( !lengthOk && decimal>0 ){
            lengthOk = g.getFontMetrics().stringWidth(  String.format( "%."+decimal+"f" , value )  ) <= width;
            decimal--;
        }

        if(!lengthOk){
            //hmm.. maybe try to return exp version
            return String.format("%.1e",value);
        }
        else{
            return String.format( "%."+decimal+"f" , value );
        }
    }




}
