/*
 * Copyright 2017 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91, CH-4123 Allschwil, Switzerland
 *
 * This file is part of DataWarrior.
 * 
 * DataWarrior is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * DataWarrior is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with DataWarrior.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package com.actelion.research.datawarrior.task.view;

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.VisualizationColor;
import com.actelion.research.table.view.VisualizationPanel;

import java.awt.*;
import java.util.Properties;

public class DETaskSetMarkerLabelBackgroundColor extends DETaskAbstractSetColor {
	public static final String TASK_NAME = "Set Marker Label Background Color";

	public DETaskSetMarkerLabelBackgroundColor(Frame owner,
	                                           DEMainPane mainPane,
	                                           VisualizationPanel view) {
		super(owner, mainPane, view, "Set Marker Label Background Color");
		}

	@Override
	public OTHER_VIEWS getOtherViewMode() {
		return OTHER_VIEWS.GRAPHICAL;
		}

	@Override
	public VisualizationColor getVisualizationColor(CompoundTableView view) {
		return ((VisualizationPanel)view).getVisualization().getLabelBackgroundColor();
		}

	@Override
	public VisualizationColor getVisualizationColor(CompoundTableView view, Properties configuration) {
		return getVisualizationColor(view);
		}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof VisualizationPanel) ? null : "Marker label background colors can be used in 2D/3D-Views only.";
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		if (view instanceof VisualizationPanel)
			super.applyConfiguration(view, configuration, isAdjusting);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}
	}
