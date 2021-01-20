package com.actelion.research.table.view.config;

import com.actelion.research.table.model.CompoundTableModel;

public class VPanel3DConfiguration extends VPanelConfiguration {
	public static final String VIEW_TYPE = "3D-view";

	public VPanel3DConfiguration(CompoundTableModel tableModel) {
		super(tableModel);
	}

	@Override
	public String getViewType() {
		return VIEW_TYPE;
	}
}
