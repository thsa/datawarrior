package com.actelion.research.datawarrior;

import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.config.ViewConfiguration;

public class DEFormConfiguration extends ViewConfiguration<DEFormView> {
	public static final String VIEW_TYPE = "formView";

	private CompoundTableModel mTableModel;

	public DEFormConfiguration(CompoundTableModel tableModel) {
		super(tableModel);
		mTableModel = tableModel;
	}

	@Override
	public String getViewType() {
		return VIEW_TYPE;
	}

	@Override
	public void apply(DEFormView formView) {

	}

	@Override
	public void learn(DEFormView formView) {

	}
}
