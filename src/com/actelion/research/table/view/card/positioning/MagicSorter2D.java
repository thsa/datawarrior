package com.actelion.research.table.view.card.positioning;

import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.card.CardElement;
import com.actelion.research.table.view.card.CardPaneModel;
import com.actelion.research.table.view.card.cardsurface.gui.JMyOptionsTable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class MagicSorter2D extends CardPositionerWithGUI{


    //private CompoundTableModel mCTM = null;

    private int mColX = -1;
    private int mColY = -1;

    private List<CardElement>   mElements = null;


    public MagicSorter2D(CompoundTableModel ctm, List<CardElement> elements){
        //this.mCTM = ctm;

        this.setTableModel(ctm);
        this.mElements = new ArrayList<>(elements);

        ArrayList<Integer> numericalRows = new ArrayList<>();
        for(int zi=0;zi<getTableModel().getColumnCount();zi++){
            if(getTableModel().isColumnTypeDouble( zi )){ numericalRows.add(zi); }
        }
        int colX = -1; int colY = -1;
        if(numericalRows.size()==1){ colX = numericalRows.get(0); colY = numericalRows.get(0); }
        if(numericalRows.size()>1){  colX = numericalRows.get(0); colY = numericalRows.get(1); }
        mColX = colX; mColY = colY;

    }


    @Override
    public boolean requiresTableModel() {
        return true;
    }

    @Override
    public JPanel getGUI() {
        return new MagicSorter2DConfigPanel();
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void configure(Properties p) {
    }

    @Override
    public Properties getConfiguration() {
        return null;
    }

    @Override
    public Point2D positionSingleCard(CardPaneModel.CardElementInterface ce, CompoundRecord cr) {
        return null;
    }

    @Override
    public List<Point2D> positionAllCards(CompoundTableModel tableModel, List<CardElement> cards ){

        // extract values:
        double[] vx = cards.stream().mapToDouble( ci -> AbstractCardPositioner.myGetDoubleAndAverageForStack(tableModel,ci,mColX) ).toArray();
        double[] vy = cards.stream().mapToDouble( ci -> AbstractCardPositioner.myGetDoubleAndAverageForStack(tableModel,ci,mColY) ).toArray();

        int gp[][] = computeExpandedScatterGrid(vx,vy);

        double cardWidth   = cards.get(0).getRectangle().getWidth();
        double cardHeight  = cards.get(0).getRectangle().getHeight();

        double width_scaled  = (cardWidth+(1+40*Math.sqrt(mStretchX)) ) * cards.size() ;
        double height_scaled = (cardHeight+(1+40*Math.sqrt(mStretchY)) ) * cards.size() ;

        double x0 = -0.5*width_scaled; double y0 = -0.5*height_scaled;
        double dx = width_scaled / cards.size(); double dy = height_scaled / cards.size();


        List<Point2D> pos = new ArrayList<>();
        for(int zi=0; zi<gp[0].length;zi++){
            pos.add( new Point2D.Double( x0 + dx * gp[0][zi] , y0 + dy * gp[1][zi] ) );
        }

        return pos;
    }


    public void setColumns(int cx, int cy){
        mColX = cx;
        mColY = cy;
    }

    private double mStretchX = 0.0;
    private double mStretchY = 0.0;

    public void setStretchX(double sx){this.mStretchX=sx;}
    public void setStretchY(double sy){this.mStretchY=sy;}


    @Override
    public boolean supportPositionSingleCard() {
        return false;
    }

    @Override
    public boolean shouldCreateStacks() {
        return false;
    }

    /**
     *
     *
     * @param pX
     * @param pY
     * @return ret[0] : x-indices, ret[1] : y-indices
     */
    public static int[][] computeExpandedScatterGrid(double pX[], double pY[]){

        double x[] = Arrays.copyOf(pX,pX.length);
        double y[] = Arrays.copyOf(pY,pY.length);


        // 1. find numerical order of both arrays
        Integer order_x[] = getNumericalOrder(pX);
        Integer order_y[] = getNumericalOrder(pY);

        // 2. done, package the result..
        int result[][] = new int[2][pX.length];

        for(int zi=0;zi<pX.length;zi++){
            result[0][zi] = order_x[zi];
            result[1][zi] = order_y[zi];
        }
        return result;
    }

    public static Integer[] getNumericalOrder(double x[]){
        Integer xi[] = new Integer[x.length];
        for(int zi=0;zi<xi.length;zi++){ xi[zi] = zi; }

        Arrays.sort(xi, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return Double.compare(x[o1],x[o2]);
            }
        });
        return xi;
    }



    public MagicSorter2D getThis(){return this;}

    class MagicSorter2DConfigPanel extends JPanel implements JMyOptionsTable.OptionTableListener {

        private JMyOptionsTable mOptionsTable;

        public MagicSorter2DConfigPanel() {
            initOptionsTable();
            this.setLayout(new BorderLayout());
            this.add(mOptionsTable,BorderLayout.CENTER);
            this.setVisible(true);
            //this.setBackground(Color.blue);
        }

        @Override
        public void optionTableEventHappened(JMyOptionsTable.OptionTableEvent event) {
            if(event.isAdjusting()){return;}

            Properties p = mOptionsTable.collectValues();

            int col_x = Integer.parseInt( (String) p.get(PROPERTY_NAME_COLUMN_X) );
            int col_y = Integer.parseInt( (String) p.get(PROPERTY_NAME_COLUMN_Y) );

            getThis().setColumns(col_x, col_y);

            getThis().setStretchX( Double.parseDouble((String) p.get(PROPERTY_NAME_STRETCH_X)) );
            getThis().setStretchY( Double.parseDouble((String) p.get(PROPERTY_NAME_STRETCH_Y)) );

            getThis().fireSortingConfigEvent(getThis(),"Something changed",true);
        }


        public static final String PROPERTY_NAME_COLUMN_X  = "ColumnX";
        public static final String PROPERTY_NAME_COLUMN_Y  = "ColumnY";
        public static final String PROPERTY_NAME_STRETCH_X = "StretchX";
        public static final String PROPERTY_NAME_STRETCH_Y = "StretchY";
        public static final String PROPERTY_NAME_JITTER_X  = "JitterX";
        public static final String PROPERTY_NAME_JITTER_Y  = "JitterY";

        public void initOptionsTable(){
            //mOptionsTable = new JMyOptionsTable(1,6);
            mOptionsTable = new JMyOptionsTable();

            //mOptionsTable.setComboBoxDoubleAndCategoricalColumns(0,0,PROPERTY_NAME_COLUMN, mModel);
            mOptionsTable.setComboBoxDoubleAndCategoricalColumns(0,0,PROPERTY_NAME_COLUMN_X,"Column X:", getThis().getTableModel() );
            mOptionsTable.setComboBoxDoubleAndCategoricalColumns(0,1,PROPERTY_NAME_COLUMN_Y,"Column Y:", getThis().getTableModel() );

            mOptionsTable.setSlider(0,2,PROPERTY_NAME_STRETCH_X, "Stretch X:",0,1,0.25);
            mOptionsTable.setSlider(0,3,PROPERTY_NAME_STRETCH_Y, "Stretch Y:",0,1,0.25);

            mOptionsTable.reinit();

            mOptionsTable.registerOptionTableListener(this);
        }

    }








}
