package com.actelion.research.table.view.card.positioning;

import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.card.CardElement;
import com.actelion.research.table.view.card.CardPaneModel;
import com.actelion.research.table.model.CompoundRecord;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.stream.Collectors;

public class GridPositioner extends AbstractCardPositioner {

    private List<List<CompoundRecord>> mRecords = new ArrayList<>();

    private Map<List<CompoundRecord>,Point2D> mGridPositions = new HashMap<>();

    public GridPositioner( List<CardElement> ce, double center_x, double center_y, double cardWidth, double cardHeight ){
        //this.initGrid(ce);
    }

//    private void initGrid(List<CardElement> ce){
//
//        // just init with the first record of the card elements..
//        this.mRecords = ce.stream().map( cei -> cei.getAllRecords() ).collect(Collectors.toList());  //new ArrayList<>( records );
//
//        List<List<CompoundRecord>> records = mRecords;
//
//        double cardWidth = ce.get(0).getRectangle().getWidth();
//        double cardHeight = ce.get(0).getRectangle().getHeight();
//
//        double center_x = 400; double center_y = 400;
//
//
//        double width_scaled  = (cardWidth * 1.1 ) * Math.ceil( Math.sqrt(records.size()));
//        double height_scaled = (cardHeight * 1.1 ) * Math.ceil( Math.sqrt(records.size()));
//
//        double x0 = center_x -0.5*width_scaled; double y0 = center_y - 0.5*height_scaled;
//        double dx = cardWidth * 1.1; double dy = cardHeight*1.1;
//
//
//        //List<CardElement> sortedCEs = new ArrayList<>(ces);
//        //sortedCEs.sort( (x,y) -> Double.compare( CardNumericalSorter.myGetDoubleAndAverageForStack(mModel, x, mColumn) , CardNumericalSorter.myGetDoubleAndAverageForStack(mModel, y, mColumn) ) );
//
//        //sortedCEs.stream().forEachOrdered( (x) -> System.out.println("V="+x.getAllRecords().get(0).getDouble(mColumn)) );
//
//        int cnt = 0;
//        for(int cy = 0;cy< (int) Math.ceil( Math.sqrt(records.size()) );cy++ ){
//            for(int cx = 0;cx< (int) Math.ceil( Math.sqrt(records.size()) );cx++ ){
//                if(cnt>=records.size()){break;}
//                mGridPositions.put( records.get(cnt) , new Point2D.Double( x0 + cx * dx , y0 + cy * dy ) );
//                cnt++;
//            }
//        }
//    }
//
//    @Override
//    public List<Point2D> positionAllCards(CompoundTableModel tableModel, List<CardElement> ce){
//        this.setTableModel(tableModel);
//        this.initGrid( ce );
//
//        List<Point2D> pos = new ArrayList<Point2D>();
//        int zi=0;
//        for(CardElement cei : ce) {
//            pos.add(positionSingleCard(cei,null));
//            zi++;
//        }
//        return pos;
//    }

    private int[] mLastDimensions = null;

    /**
     *
     * @return number of cards in x / y dimension of last position operation.
     */
    public int[] getLastDimensions() {
        return mLastDimensions;
    }

    @Override
    public List<Point2D> positionAllCards(CompoundTableModel tableModel, List<CardElement> ce){

        if(ce.size()==0) { return new ArrayList<>(); }

        double cardWidth = ce.get(0).getRectangle().getWidth();
        double cardHeight = ce.get(0).getRectangle().getHeight();

        double center_x = 400; double center_y = 400;

        int nCE = ce.size();

        double width_scaled  = (cardWidth * 1.1 ) * Math.ceil( Math.sqrt(nCE));
        double height_scaled = (cardHeight * 1.1 ) * Math.ceil( Math.sqrt(nCE));

        double x0 = center_x -0.5*width_scaled; double y0 = center_y - 0.5*height_scaled;
        double dx = cardWidth * 1.1; double dy = cardHeight*1.1;


        List<Point2D> positions = new ArrayList<>();


        int cnt = 0; int max_x= 0; int max_y= 0;
        for(int cy = 0;cy< (int) Math.ceil( Math.sqrt(nCE) );cy++ ){
            for(int cx = 0;cx< (int) Math.ceil( Math.sqrt(nCE) );cx++ ){
                if(cnt>=nCE){break;}
                max_x = Math.max(cx+1,max_x);
                max_y = cy+1;
                //mGridPositions.put( records.get(cnt) , new Point2D.Double( x0 + cx * dx , y0 + cy * dy ) );
                positions.add(new Point2D.Double( x0 + cx * dx , y0 + cy * dy ));
                cnt++;
            }
        }

        this.mLastDimensions = new int[]{max_x,max_y};

        return positions;
    }




    @Override
    public boolean requiresTableModel() {
        return false;
    }

    @Override
    public boolean supportPositionSingleCard() {
        return false;
    }

    @Override
    public Point2D positionSingleCard(CardPaneModel.CardElementInterface ce, CompoundRecord cr) {
        List<CompoundRecord> record = null;
        if(cr!=null){
            record = new ArrayList<>(); record.add(cr);
        }
        else{
            record = ce.getAllRecords();
        }
        return mGridPositions.get(record);
    }

    @Override
    public void configure(Properties properties) {

    }

    @Override
    public boolean shouldCreateStacks() {
        return false;
    }

    @Override
    public Properties getConfiguration() {
        return null;
    }
}
