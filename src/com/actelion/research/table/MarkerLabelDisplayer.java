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

package com.actelion.research.table;

public interface MarkerLabelDisplayer extends MarkerLabelConstants {
	public static final int cLabelsOnAllRows = -1;
	public static final int cLabelsOnSelection = -2;

	public void setMarkerLabels(int[] columnAtPosition);
	public void setMarkerLabelList(int listNo);
	public void setMarkerLabelOnePerCategory(int categoryColumn, int valueColumn, int mode);
    public void setMarkerLabelSize(float size, boolean isAdjusting);
	public void setMarkerLabelsInTreeViewOnly(boolean inTreeViewOnly);
	public void setMarkerLabelsBlackOrWhite(boolean blackAndWhite);
	public void setOptimizeLabelPositions(boolean optimize);
	public void setShowColumnNameInTable(boolean showColumnName);
	public void setShowLabelBackground(boolean b);
	public void setLabelTransparency(float transparency, boolean isAdjusting);
	public float getMarkerLabelSize();
	public int getMarkerLabelColumn(int position);
	public int getMarkerLabelList();
	public int[] getMarkerLabelOnePerCategory();
//	public int getMarkerLabelTableEntryCount();
	public float getLabelTransparency();
	public boolean supportsMidPositionLabels();
	public boolean supportsMarkerLabelTable();
	public boolean supportsLabelsByList();
	public boolean supportsLabelBackground();
	public boolean supportsLabelBackgroundTransparency();
	public boolean supportsLabelPositionOptimization();
	public boolean isMarkerLabelsInTreeViewOnly();
	public boolean isMarkerLabelBlackOrWhite();
	public boolean isShowLabelBackground();
	public boolean isTreeViewModeEnabled();
	public boolean isShowColumnNameInTable();
	public boolean isOptimizeLabelPositions();
	}
