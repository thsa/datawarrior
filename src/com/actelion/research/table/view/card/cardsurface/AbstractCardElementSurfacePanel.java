package com.actelion.research.table.view.card.cardsurface;

import com.actelion.research.table.model.CompoundTableModel;

import java.util.*;
import java.util.List;

public abstract class AbstractCardElementSurfacePanel implements CardElementSurfacePanelInterface {




    @Override
    public void initializePanelConfiguration(CompoundTableModel model, int columns[]){

        if( columns==null ){
            columns = new int[this.getNumberOfDataColumnsRequired()];
            Arrays.fill(columns,-1);
        }

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





}
