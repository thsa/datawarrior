package com.actelion.research.table.view.card.cardsurface;

import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public abstract class AbstractCardSurfacePanel implements CardElementSurfacePanelInterface {

    public abstract void drawPanel(Graphics g, CardDrawingConfig conf, CompoundTableModel model, CompoundRecord rec , int columns[], int w, int h);

    public boolean isSingleCardSurfacePanel() { return true;}

    public abstract String getName();
    public abstract JComponent getLabel();
    public abstract JPanel getConfigDialog();


    /**
     * This will be called once before the cards are drawn, always after a change of  configuration or
     * width / size of the panel..
     *
     * @param g
     * @param conf
     * @param model
     * @param records
     * @param columns
     * @param w
     * @param h
     */
    public abstract void initalizeCardPanel(Graphics g, CardDrawingConfig conf, CompoundTableModel model, List<CompoundRecord> records, int columns[], int w, int h);



    /**
     * Initializes the panel configuration and tries to find some reasonable entries for the data columns.
     *
     * @param model
     */
    public void initializePanelConfiguration(CompoundTableModel model){
        int cols[] = new int[this.getNumberOfDataColumnsRequired()];
        Arrays.fill(cols,-1);
        initializePanelConfiguration(model,cols);
    }

    @Override
    public void initializePanelConfiguration(CompoundTableModel model, int columns[]){

        if(this.getNumberOfDataColumnsRequired() > 0){

            Map<Integer, List<Integer>> matchingColumns = findAllMatchingColumns(model);
            Set<Integer> shownColumns = new HashSet<>();

            for(int zi=0;zi<this.getNumberOfDataColumnsRequired();zi++){

                boolean needsColumn = false;
                int column = columns[zi];
                if(column<0){
                    needsColumn = true;
                }
                if(!needsColumn) {
                    if (!this.canHandleColumnForSlot(model, zi, column)) {
                        needsColumn = true;
                    }
                }
                if(needsColumn){
                    // find all handled columns
                    List<Integer> okCols = matchingColumns.get(zi);
                    List<Integer> nonShownCols = new ArrayList<>(okCols);
                    nonShownCols.removeAll(shownColumns);
                    if(!nonShownCols.isEmpty()){
                        int col = nonShownCols.get(0);
                        columns[zi] = col;
                        shownColumns.add(col);
                    }
                    else{
                        if(!okCols.isEmpty()){
                            int col = okCols.get(0);
                            columns[zi] = col;
                            shownColumns.add(col);
                        }
                        else{
                            columns[zi] = -1;
                        }
                    }
                }
            }
            this.setColumns(columns);
        }
    }

    public Map<Integer, List<Integer>> findAllMatchingColumns(CompoundTableModel model  ){
        Map<Integer, List<Integer>> map = new HashMap<>();
        for(int zi=0;zi<this.getNumberOfDataColumnsRequired();zi++){
            List<Integer> matchingSlots = new ArrayList<>();
            for(int zj=0;zj<model.getTotalColumnCount();zj++){
                if(this.canHandleColumnForSlot(model, zi, zj)){
                    matchingSlots.add(zj);
                }
            }
            map.put(zi,matchingSlots);
        }
        return map;
    }


    /**
     * Helper function to automatically create the stack surface configuration from the card surface configuration.
     * Can return an empty list if there is not matching proposal or if the proposal mechanism is not supported.
     *
     * Returns a list because it can make sense to split SingleCard "MultiPanels" into separate stack panels..
     *
     * @return a list containing matching stack surface panels. Returns an empty list if it fails to make a proposal.
     */
    public abstract List<StackSurfacePanelAndColumnsAndPosition> proposeCorrespondingStackSurfacePanel( int columns[] , double relPos, double relHeight );


    public static class StackSurfacePanelAndColumnsAndPosition{
        public AbstractStackSurfacePanel panel;
        public int[]                     columns;
        public double                    relativePosition;
        public double                    relativeHeight;

        public StackSurfacePanelAndColumnsAndPosition(AbstractStackSurfacePanel p, int[] cols, double relPos, double relHeight){
            panel = p;
            columns = Arrays.copyOf(cols,cols.length);
            relativePosition = relPos;
            relativeHeight   = relHeight;
        }
    }

}
