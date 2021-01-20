package com.actelion.research.table.view.card.tools;

import com.actelion.research.table.model.CompoundTableModel;

import java.util.ArrayList;
import java.util.List;

public class ColWithType {

    public enum ColType { NONE , NUMERICAL , CATEGORICAL , TEXT , STRUCTURE , REACTION };

    private ColType mType = ColType.NONE;

    private CompoundTableModel mCTM = null;
    private int mCol = -1;


    public ColWithType(CompoundTableModel ctm, int col, ColType type) {
        this.mCTM = ctm;
        this.mCol = col;
        this.mType = type;
    }

    public ColType getType() {
        return mType;
    }

    public CompoundTableModel getCompoundTableModel() {
        return mCTM;
    }

    public int getCol() {
        return mCol;
    }


    public String toString(){
        switch(mType){
            case NONE: return mCTM.getColumnTitle(mCol)+"[None]";
            case STRUCTURE: return mCTM.getColumnTitle(mCol)+"[Structure]";
            case CATEGORICAL: return mCTM.getColumnTitle(mCol)+"[Categorical]";
            case NUMERICAL: return mCTM.getColumnTitle(mCol)+"[Numerical]";
            case TEXT: return mCTM.getColumnTitle(mCol)+"[Text]";
        }
        // cannot end up here..
        return mCTM.getColumnTitle(mCol)+"[SomethingSeriouslyWrong..]";
    }

    public String serializeToString() {
        switch(mType){
            case NONE: return mCol+"/None";
            case STRUCTURE: return mCol+"/Structure";
            case CATEGORICAL: return mCol+"/Categorical";
            case NUMERICAL: return mCol+"/Numerical";
            case TEXT: return mCol+"/Text";
        }
        // cannot end up here..
        return mCTM.getColumnTitle(mCol)+"[SomethingSeriouslyWrong..]";
    }


//    /**
//     * Todo: find out what is the most efficient way to do this
//     *
//     * @param col
//     */
//    public static int columnToTotalColumn(CompoundTableModel ctm, int col){
//        ctm. ctm.getColumnTitle(col)
//    }

    /**
     * Works together with serializeToString()
     *
     * @param s
     * @return
     */
    public static ColWithType deserializeFromString(CompoundTableModel ctm, String s){
        String split[] = s.split("/");
        if(split.length!=2){
            System.out.println("ERROR! ColWithType::deserializeFromString failed..");
        }

        int col = Integer.parseInt(split[0]);

        if(split[1].equals("None")){return new ColWithType(ctm,col,ColType.NONE);}
        if(split[1].equals("Structure")){return new ColWithType(ctm,col,ColType.STRUCTURE);}
        if(split[1].equals("Categorical")){return new ColWithType(ctm,col,ColType.CATEGORICAL);}
        if(split[1].equals("Numerical")){return new ColWithType(ctm,col,ColType.NUMERICAL);}
        if(split[1].equals("Text")){return new ColWithType(ctm,col,ColType.TEXT);}

        System.out.println("ERROR! ColWithType::deserializeFromString failed..");
        return null;
    }

    public static List<ColWithType> getAllColumnsFromCTM(CompoundTableModel ctm) {
        List<ColWithType> list = new ArrayList<>();

        // 1. add structure rows:
        for( int zi = 0;zi<ctm.getColumnCount(); zi++ ){
            if(ctm.isColumnTypeStructure(zi)){ list.add(new ColWithType(ctm,zi,ColType.STRUCTURE) ); }
        }

        // 2. add category< rows:
        for( int zi = 0;zi<ctm.getColumnCount(); zi++ ){
            if(ctm.isColumnTypeCategory(zi)){ list.add(new ColWithType(ctm,zi,ColType.CATEGORICAL) ); }
        }

        // 3. add numeric rows:
        for( int zi = 0;zi<ctm.getColumnCount(); zi++ ){
            if(ctm.isColumnTypeDouble(zi)){ list.add(new ColWithType(ctm,zi,ColType.NUMERICAL) ); }
        }

        return list;
    }

    public static List<ColWithType> getAllNumericColumns(CompoundTableModel ctm) {
        List<ColWithType> columns = new ArrayList<>();
        for(int zi=0;zi<ctm.getTotalColumnCount();zi++) {
            if(ctm.isColumnTypeDouble(zi)){
                columns.add( new ColWithType(ctm,zi, ColType.NUMERICAL));
            }
        }
        return columns;
    }

    public static List<ColWithType> getAllStructureColumns(CompoundTableModel ctm) {
        List<ColWithType> columns = new ArrayList<>();
        for(int zi=0;zi<ctm.getTotalColumnCount();zi++) {
            if(ctm.isColumnTypeStructure(zi)){
                columns.add( new ColWithType(ctm,zi, ColType.STRUCTURE));
            }
        }
        return columns;
    }



}
