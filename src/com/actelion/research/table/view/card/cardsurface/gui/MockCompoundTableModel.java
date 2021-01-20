package com.actelion.research.table.view.card.cardsurface.gui;

import com.actelion.research.table.model.CompoundTableModel;

import java.util.ArrayList;

public class MockCompoundTableModel extends CompoundTableModel {

    private ArrayList<Integer> mListChemStructureCols = new ArrayList<>();
    private ArrayList<Integer> mListChemRxnCols = new ArrayList<>();
    private ArrayList<Integer> mListNumericCols = new ArrayList<>();


    public MockCompoundTableModel(){
        this.mListChemStructureCols.add(0); this.mListChemStructureCols.add(1);
        this.mListChemRxnCols.add(2); this.mListChemRxnCols.add(3);
        for(int zi=3;zi<13;zi++){this.mListNumericCols.add(zi);}
    }

    @Override
    public boolean isColumnTypeDouble(int col){
        return mListNumericCols.contains(col);
    }

    @Override
    public boolean isColumnTypeStructure(int col){
        return mListChemStructureCols.contains(col);
    }

    @Override
    public boolean isColumnTypeReaction(int col){
        return mListChemRxnCols.contains(col);
    }

    @Override
    public int getColumnCount(){
        return 14;
    }

    @Override
    public String getColumnTitle(int column) {
        return "MockColumn"+column;
    }
}

