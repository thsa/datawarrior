package com.actelion.research.table.view.card.tools;

import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.DetailPopupProvider;
import com.actelion.research.table.model.CompoundListSelectionModel;
import com.actelion.research.table.model.CompoundTableModel;

import java.awt.*;

public class DataWarriorLink {

    private Frame mParentFrame;
    private CompoundTableModel mTableModel;
    private CompoundListSelectionModel mListSelectionModel;
    private CompoundTableColorHandler mColorHandler;
    private DetailPopupProvider mDetailPopupProvider;


    public DataWarriorLink(Frame parentFrame, CompoundTableModel tableModel, CompoundListSelectionModel listSelectionModel, CompoundTableColorHandler ctch , DetailPopupProvider dpp ) {
        this.mParentFrame          = parentFrame;
        this.mTableModel           = tableModel;
        this.mListSelectionModel   = listSelectionModel;
        this.mColorHandler         = ctch;
        this.mDetailPopupProvider  = dpp;
    }


    public Frame getParentFrame() {return mParentFrame;}

    public CompoundTableModel getCTM() {return mTableModel;}

    public CompoundListSelectionModel getListSelectionModel() { return mListSelectionModel; }

    public CompoundTableColorHandler getTableColorHandler() { return mColorHandler; }

    public DetailPopupProvider getDetailPopupProvider() { return mDetailPopupProvider; }


}
