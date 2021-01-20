package com.actelion.research.table.view.config;

import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.JStructureGrid;

public class StructureGridConfiguration extends ViewConfiguration<JStructureGrid> {
	public static final String VIEW_TYPE = "structureGrid";

	public StructureGridConfiguration(CompoundTableModel tableModel) {
		super(tableModel);
	}

	@Override
	public String getViewType() {
		return VIEW_TYPE;
	}

	@Override
	public void apply(JStructureGrid view) {
	}

	@Override
	public void learn(JStructureGrid view) {
	}
}
