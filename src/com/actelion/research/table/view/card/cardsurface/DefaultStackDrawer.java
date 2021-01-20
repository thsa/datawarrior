package com.actelion.research.table.view.card.cardsurface;

import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.model.*;
import com.actelion.research.table.view.card.CardElement;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 *
 * .clone will init a new object with a new (cloned) StackDrawingConfig object. All the buffered stack shapes and
 * background images are NOT cloned.
 *
 */

public class DefaultStackDrawer extends AbstractStackDrawer implements CompoundTableListener , CompoundTableListListener, Cloneable {

    /**
     * Max cards that are considered for drawing the stack (unneccesary performance issue if your stack has 1000+ cards)
     */
    private static final int CONF_MAX_STACK_CARDS_DRAWN = 12;//32;


    StackDrawingConfig mConfig;

    /**
     * focus list
     */
    private int mFocusListIndex = -1;

    private volatile boolean mReinitRequired = true;




    private List<Shape> mStackShapes = new ArrayList<>();



    /**
     * Copy constructor. It constructs a new DefaultStackDrawer with a NEW StackDrawingConfig.
     *
     * @param dsd
     */
    public DefaultStackDrawer( DefaultStackDrawer dsd ){

        this.setStackDrawingConfig( new StackDrawingConfig( dsd.getStackDrawingConfig() ) );
    }


    /**
     * Note: this does not take care for CTM::isVisible(..), i.e. it expects that the function which calls
     * this already sorted out all the non-visibie cards from the stack!
     *
     *
     * @param g
     * @param tableModel
     * @param rec
     */
    @Override
    public void drawStackSurface(Graphics g, CompoundTableModel tableModel, int focusListFlag, List<CompoundRecord> rec, CardElement ce) {

        Graphics2D g2 = (Graphics2D) g;

        if(mReinitRequired){
            for( StackDrawingConfig.StackSurfaceSlot slot  : mConfig.getSlots() ){
                slot.getSurfacePanel().initalizeStackPanel(g, mConfig, tableModel, focusListFlag, DefaultCardDrawer.getNonexcludedCompoundRecordsFromTable(tableModel), slot.getColumns(), (int)  mConfig.getStackWidth(),  (int) (mConfig.getStackHeight() * slot.getRelativeHeight()) );
            }
            mReinitRequired = false;
        }



        for( StackDrawingConfig.StackSurfaceSlot slot  : mConfig.getSlots() ){
            double height = slot.getRelativeHeight() * mConfig.getStackHeight();
            double width  = mConfig.getStackWidth();
            int    ypos   = (int) ( slot.getRelativePosY()*mConfig.getStackHeight() );
            g.translate(0,  ypos );
            slot.getSurfacePanel().drawPanel(g,mConfig,tableModel,rec,ce,slot.getColumns(),(int)width,(int)height);
            g.translate(0, -ypos );
        }


//        if( rec.stream().anyMatch( r -> r.isSelected() ) ){
//            if( rec.stream().allMatch( r -> r.isSelected() ) ){
//                // all selected..
//                //System.out.println("DRAW SELECTED!");
//                g2.setColor(Color.orange.darker());
//                g2.setStroke(new BasicStroke(20));
//                g2.drawRect(0,0,(int) mConfig.getStackWidth(),(int) mConfig.getStackHeight());
//            }
//            else{
//                // some selected..
//            }
//
//        }
    }


    /**
     *
      */

    /**
     *
     * NOTE! Only draws the background of the SURFACE of the card element. Stacked cards / shadow etc. is handled by
     * the CardElementBackgroundDrawer.
     *
     * @param g
     * @param model
     * @param rec
     */
    @Override
    public void drawStackBackground(Graphics g, CompoundTableModel model, List<CompoundRecord> rec) {
        Graphics2D g2 = (Graphics2D) g;
        int w = (int) mConfig.getStackWidth();
        int h = (int) mConfig.getStackHeight();

        GradientPaint gradient = new GradientPaint(0, 0, Color.white.darker(), w/2, h, Color.lightGray.brighter());
        g2.setPaint(gradient);
        g2.fillRoundRect(0,0,w,h,4,10);
        g2.setColor(Color.black);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(0,0,w,h,4,10);
    }


    /**
     * Note: this does not take care for CTM::isVisible(..), i.e. it expects that the function which calls
     * this already sorted out all the non-visibie cards from the stack!
     *
     * @param g
     * @param tableModel
     * @param rec
     */
    public void drawStackBackground_old(Graphics g, CompoundTableModel tableModel, List<CompoundRecord> rec){

        if(rec.size()<=1){
            return;
        }

        //Random r = new Random(12345);
        Random r = new Random(12345 + rec.get(0).getID() );

        Graphics2D g2 = (Graphics2D) g;
        int w = (int) mConfig.getStackWidth();
        int h = (int) mConfig.getStackHeight();

        double max_rot = 0.15;
        double max_shift = 0.08;


        RoundRectangle2D rr = new RoundRectangle2D.Double(0,0,w,h,10,10);

        int numStackCards = Math.min( rec.size() , CONF_MAX_STACK_CARDS_DRAWN );

        if(this.mStackShapes.size() < numStackCards ){
            // init stack shapes:
            this.mStackShapes.clear();
            for(int zi=0;zi<numStackCards-1;zi++) {

                double rot = r.nextDouble() * 2 * max_rot - max_rot;

                double shift_x = (0.5 - r.nextDouble()) * max_shift * w;
                double shift_y = (0.5 - r.nextDouble()) * max_shift * h;

                //Shape rr_rot = AffineTransform.getRotateInstance(rot,w/2,h/2).createTransformedShape(rr);
                AffineTransform rotateAndShift = AffineTransform.getRotateInstance(rot, w / 2, h / 2);
                rotateAndShift.translate(shift_x, shift_y);
                Shape rr_rot = rotateAndShift.createTransformedShape(rr);
                mStackShapes.add(rr_rot);
            }
        }

        for(int zi=0;zi<numStackCards-1;zi++){
            // this try/catch is just becuase of the weird "IndexOutOfBoundsException" with index smaller than size that occurs occasionally. (I dont understand how/why..)
            try {
                Shape stackedCard = mStackShapes.get(zi);
                int cardColor = (int) (40 * ((1.0 * zi) / (numStackCards - 1)));
                g2.setColor(new Color(cardColor + 120, cardColor + 120, cardColor + 120));
                g2.fill(stackedCard);
                g2.setColor(Color.black);
                g2.draw(stackedCard);
            }
            catch(Exception e){
                System.out.println("Exception while drawing stack background..");
            }
        }


        GradientPaint gradient = new GradientPaint(0, 0, Color.white.darker(), w/2, h, Color.lightGray.brighter());
        g2.setPaint(gradient);
        g2.fillRoundRect(0,0,w,h,10,10);
        g2.setColor(Color.black);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(0,0,w,h,10,10);


    }

    public void drawStackBorder(Graphics g, CompoundTableModel model, List<CompoundRecord> rec){
        int w = (int) mConfig.getStackWidth();
        int h = (int) mConfig.getStackHeight();
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.black);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(0,0,w,h,4,10);
    }


    @Override
    public void drawStackElementsIndicator(Graphics g, CompoundTableModel model, List<CompoundRecord> rec) {
        // figure out font size:
//        Graphics2D g2 = (Graphics2D) g;
//        g2.setColor(Color.red.darker());
//        g2.setFont( new Font("Verdana",Font.PLAIN,20) );
//        g2.drawString(""+rec.size(),0,0);
    }

    public void setStackDrawingConfig(StackDrawingConfig sdc) {
        this.mReinitRequired = true;
        this.mConfig = sdc;
    }

    public StackDrawingConfig getStackDrawingConfig(){ return this.mConfig; }


    @Override
    public void compoundTableChanged(CompoundTableEvent e) {
        if(e.isAdjusting()){return;}

        if(e.getType()==CompoundTableEvent.cChangeExcluded){
            this.mReinitRequired = true;
        }
    }

    @Override
    public void listChanged(CompoundTableListEvent e) {
        this.mFocusListIndex = e.getListIndex();
        this.mReinitRequired = true;
    }

    public DefaultStackDrawer(CompoundTableModel ctm, CompoundTableColorHandler ctch){
        // @TODO add real init

        //mConfig = new StackDrawingConfig();

        // funny way to initialize default:
        // 1. create card config automatically, then let the StackConfigurationProposer propose the stack config
        //StackDrawingConfig sdc = StackDrawingConfig.createDefaultStackDrawingConfig(ctm, ctch);

        StackDrawingConfig sdc = new StackDrawingConfig(ctch);
        this.setStackDrawingConfig(sdc);

        //mConfig.getSlots().add(new CardDrawingConfig.CardSurfaceSlot(new StructurePanel(),4,0.0,0.25));
        //mConfig.getSlots().add(new StackDrawingConfig.StackSurfaceSlot(new NumCardsIndicatorPanel(),new int[]{},0.0,0.1));
        //mConfig.getSlots().add(new StackDrawingConfig.StackSurfaceSlot(new StackChemStructurePanel(),new int[]{13},0.1,0.5));

        // @TODO: check why this does not work..
        mConfig.getSlots().stream().forEach( si ->  si.getSurfacePanel().initializePanelConfiguration(ctm,null));
        //mConfig.getSlots().add(new CardDrawingConfig.CardSurfaceSlot(new StructurePanel(),4,0.5,0.25));
        //mConfig.getSlots().add(new CardDrawingConfig.CardSurfaceSlot(new StructurePanel(),4,0.75,0.25));


        // REGISTER AS LISTENER TO COMPOUND TABLE MODEL:
        ctm.addCompoundTableListener(this);

    }

    /**
     * Clones both the drawer and the config
     *
     * @return
     */
    public DefaultStackDrawer clone() {
        return new DefaultStackDrawer(this);
    }


}
