package com.actelion.research.table.view.config;

import com.actelion.research.table.model.CompoundTableModel;

public class VPanel2DConfiguration extends VPanelConfiguration {
	public static final String VIEW_TYPE = "2D-view";

	public VPanel2DConfiguration(CompoundTableModel tableModel) {
		super(tableModel);
		}

	@Override
	public String getViewType() {
		return VIEW_TYPE;
	}
}
