package com.actelion.research.table.view.card.positioning;

import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.card.CardElement;
import com.actelion.research.table.view.card.tools.IdentityHashedObject;

import java.awt.geom.Point2D;
import java.util.*;

public abstract class AbstractCardPositioner implements CardPositionerInterface{

    /**
     * First sets the table model, then positions all supplied cards.
     *
     * @param tableModel
     * @param cards
     * @return
     */
    public List<Point2D> positionAllCards(CompoundTableModel tableModel, List<CardElement> cards )  throws InterruptedException {
        this.setTableModel(tableModel);
        List<Point2D> pos = new ArrayList<>(cards.size());
        int zi=0;
        for(CardElement cei : cards) {
            if(Thread.currentThread().isInterrupted()){
                throw new InterruptedException();
            }

            pos.add(positionSingleCard(cei,null));
            zi++;
        }
        return pos;
    }


    CompoundTableModel mCTM = null;


    /**
     * If this card positioner requires a table model, then calling this function will update all internal data
     * to use the newly supplied table model. I.e. after calling this function, the psotionCard(ce,cr) function must
     * work according to the newly supplied table model.
     *
     * @param tableModel
     */
    public void setTableModel( CompoundTableModel tableModel ){
        this.mCTM = tableModel;
    }

    public CompoundTableModel getTableModel(){
        return this.mCTM;
    }


    /**
     * If column is numeric, return double value.
     * If column is categorical, return int value indicating the position of the catetory
     *
     * @param model
     * @param rec
     * @param column
     * @return
     */
    public static double myGetDouble(CompoundTableModel model, CompoundRecord rec, int column){
        if(model.isColumnTypeCategory(column)){
            int categoryIndex = model.getCategoryIndex( column , rec );
            return categoryIndex;
        }
        else{
            return rec.getDouble(column);
        }
    }


    /**
     *
     *
     * @param model
     * @param ce
     * @param column
     * @return
     */
    public static double myGetDoubleAndAverageForStack( CompoundTableModel model , CardElement ce , int column ){
        return ce.getNonexcludedRecords().stream().mapToDouble( ri -> myGetDouble(model,ri,column) ).average().getAsDouble();
    }


    /**
     * Explanation:
     * ORDER : elments with same numerical value will get ( arbitrary, or due to additional config) order
     *         inbetween them. I.e. every element will have a unique number.
     *
     * ORDER_COLLAPSED : elments with same numerical value will get the same order value.
     */
    public static enum ValueExtractionMode { NUMERICAL , ORDER , ORDER_COLLAPSED };

    /**
     * Explanation:
     * DONT_CONSIDER: NaN values will be just not considered. If no elements in a stack are considered value is NaN.
     * ZERO: NaN values will be counted as zero.
     * MAKE_STACK_NAN: Then, a single NaN value will make the whole stack have value NaN.
     */
    public static enum NaNHandlingInStacks { DONT_CONSIDER , ZERO , MAKE_STACK_NAN }

    public static double extractCardElementValue(CompoundTableModel model, int column, CardElement ce , NaNHandlingInStacks stack_mode) {

        // hmm..
        if (ce.isEmptyAfterExclusion()) {
            return Double.NaN;
        }

        if (ce.isCardAfterExclusion()) {
            return myGetDouble(model, ce.getNonexcludedRecords().get(0), column);
        }
        if (ce.isStackAfterExclusion()) {

            double sum = 0.0;
            int n = 0;

            for (CompoundRecord cr : ce.getAllNonexcludedRecords()) {
                double vi = myGetDouble(model, cr, column);
                if (Double.isNaN(vi)) {
                    if (stack_mode == NaNHandlingInStacks.DONT_CONSIDER) {

                    } else {
                        if (stack_mode == NaNHandlingInStacks.MAKE_STACK_NAN) {
                            sum = Double.NaN;
                            break;
                        }
                        if (stack_mode == NaNHandlingInStacks.ZERO) {
                            sum += 0.0;
                            n++;
                        }
                    }
                } else {
                    sum += vi;
                    n++;
                }
            }
            return (sum) / (1.0 * n);
        }

        System.out.println("extractCardElementValue : Something went very wrong..");
        // we cannot end up here..
        return Double.NaN;
    }
    /**
     * This is the default way to extract numerical values from (numerical / categorical) columns.
     * Extracts all values for non-excluded CardElements / compound records.
     *
     * @return
     */
    public static Map<IdentityHashedObject<CardElement>,Double> extractValues( CompoundTableModel model , int column , List<CardElement> ces , ValueExtractionMode mode , NaNHandlingInStacks stack_mode ) throws InterruptedException {

        long timestamp = System.currentTimeMillis();

        double values[] = new double[ces.size()];

        // 1. extract all numerical values:
        int zi=0;
        for(CardElement ce : ces){
            if(Thread.currentThread().isInterrupted()){throw new InterruptedException();}

            values[zi] = extractCardElementValue(model,column,ce,stack_mode);
            zi++;
        }

        // 2. consider extraction mode:

        // 2.1
        if(mode == ValueExtractionMode.NUMERICAL){
            Map<IdentityHashedObject<CardElement>,Double> result = new HashMap<>();
            for(CardElement ce : ces){
                if(Thread.currentThread().isInterrupted()){throw new InterruptedException();}
                result.put(new IdentityHashedObject<>(ce),values[zi]);
                zi++;
            }
            return result;
        }

        // 2.2
        if(mode == ValueExtractionMode.ORDER || mode == ValueExtractionMode.ORDER_COLLAPSED ){
            Integer[] order = getNumericalOrder(values);

            if(mode == ValueExtractionMode.ORDER ){
                Map<IdentityHashedObject<CardElement>,Double> result = new HashMap<>();
                zi=0;
                for(CardElement ce : ces){
                    if(Thread.currentThread().isInterrupted()){throw new InterruptedException();}
                    result.put(new IdentityHashedObject<>(ce), new Double(order[zi].intValue() ));
                    zi++;
                }
                System.out.println("extractValues finished: time= "+ (System.currentTimeMillis()-timestamp)+" ms");
                return result;
            }

            if(mode == ValueExtractionMode.ORDER_COLLAPSED){
                Map<IdentityHashedObject<CardElement>,Double> result      = new HashMap<>();
                Map<Integer,IdentityHashedObject<CardElement>> result_inv = new HashMap<>();
                zi=0;
                for(CardElement ce : ces){
                    if(Thread.currentThread().isInterrupted()){throw new InterruptedException();}
                    result.put(new IdentityHashedObject<>(ce), new Double(order[zi].intValue() ));
                    result_inv.put( order[zi], new IdentityHashedObject<>(ce) );
                    zi++;
                }
                // loop over values and collapse equal ones..
                List<Integer> sorted_indeces = new ArrayList<>(result_inv.keySet());
                sorted_indeces.sort(Integer::compare);
                double last_value  = Double.NaN;
                boolean first = true;
                int     last_index = -1;
                for( Integer oi : sorted_indeces ){
                    if(Thread.currentThread().isInterrupted()){throw new InterruptedException();}
                    if(first){
                        first=false;
                        last_value = result.get( result_inv.get(oi) ).doubleValue();
                        last_index = oi.intValue();
                    }
                    else{
                        double next_value = result.get( result_inv.get(oi) ).doubleValue();
                        if(last_value==next_value){
                            // overwrite entry in result..
                            result.put( result_inv.get(oi) , new Double(last_index) );
                        }
                    }
                }
                return result;
            }
        }
        //model.is
        return null;
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


}
