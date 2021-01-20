package com.actelion.research.datawarrior.task.chem;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;

import javax.swing.*;

public class DETaskSortStructuresBySimilarity extends DETaskAbstractSortChemistryBySimilarity {
	public static final String TASK_NAME = "Sort Structures By Similarity";

	public DETaskSortStructuresBySimilarity(DEFrame parent, int descriptorColumn, CompoundRecord record) {
		super(parent, descriptorColumn, record);
	}

	@Override
	protected String getChemistryName() { return "Structure"; }

	@Override
	protected String getParentColumnType() { return CompoundTableModel.cColumnTypeIDCode; }

	@Override
	protected JComboBox getComboBoxReactionPart() {
		return null;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}
}
