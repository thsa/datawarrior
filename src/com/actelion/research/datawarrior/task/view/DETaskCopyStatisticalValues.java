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
import com.actelion.research.datawarrior.DETableView;
import com.actelion.research.datawarrior.task.AbstractViewTask;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.VisualizationPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.HashMap;
import java.util.Properties;

public class DETaskCopyStatisticalValues extends AbstractViewTask {
	public static final String TASK_NAME = "Copy Statistical Values";

	public DETaskCopyStatisticalValues(Frame parent, DEMainPane mainPane, CompoundTableView view) {
		super(parent, mainPane, view);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return true;
		}

	@Override
	public JComponent createInnerDialogContent() {
		return null;
		}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof VisualizationPanel || view instanceof DETableView) ?
				null : "Only 2D- and 3D-view support copying statistical values.";
		}

	@Override
	public void runTask(Properties configuration) {
        final CompoundTableView view = getConfiguredView(configuration);
		if (SwingUtilities.isEventDispatchThread())
			doTheWork(view);
		else
			SwingUtilities.invokeLater(() -> doTheWork(view) );
		}

	private void doTheWork(CompoundTableView view) {
		String content = (view instanceof VisualizationPanel) ?
				((VisualizationPanel)view).getVisualization().getStatisticalValues() : getStatisticalValuesFromTable();
		StringSelection theData = new StringSelection(content);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(theData, theData);
		}

	private String getStatisticalValuesFromTable() {
		StringBuilder sb = new StringBuilder();
		sb.append("Column\t");
		sb.append("Data type\t");
		sb.append("Category count\t");
		sb.append("Empty value count\t");
		sb.append("Uniqueness\t");
		sb.append("Has multi value rows\t");
		sb.append("Properties\n");
		for (int column=0; column<getTableModel().getTotalColumnCount(); column++) {
			sb.append(getTableModel().getColumnTitle(column));
			sb.append("\t");
			sb.append(getDataType(column));
			sb.append("\t");
			sb.append(getCategoryCount(column));
			sb.append("\t");
			sb.append(getEmptyValueCount(column));
			sb.append("\t");
			sb.append(getUniqueness(column));
			sb.append("\t");
			sb.append(getMultipleEntries(column));
			sb.append("\t");
			sb.append(getColumnProperties(column));
			sb.append("\n");
			}
		return sb.toString();
		}

	private String getDataType(int column) {
		if (getTableModel().isColumnTypeStructure(column))
			return "chemical structure";
		if (getTableModel().isColumnTypeReaction(column))
			return "chemical reaction";
		if (getTableModel().getColumnSpecialType(column) != null)
			return getTableModel().getColumnSpecialType(column);
		if (getTableModel().isColumnTypeRangeCategory(column))
			return "range categories";
		if (getTableModel().isColumnTypeDate(column))
			return "date values";
		if (getTableModel().isColumnTypeDouble(column))
			return "numerical";
		if (getTableModel().isColumnTypeString(column))
			return "text";
		else
			return "unknown";
		}

	private String getCategoryCount(int column) {
		if (getTableModel().isColumnTypeCategory(column))
			return Integer.toString(getTableModel().getCategoryCount(column));
		else
			return "";
		}

	private String getEmptyValueCount(int column) {
		int count = 0;

		if (!getTableModel().isColumnDataComplete(column))
			for (int row=0; row<getTableModel().getTotalRowCount(); row++)
				if (getTableModel().getTotalRecord(row).getData(column) == null)
					count++;

		return Integer.toString(count);
		}

	private String getUniqueness(int column) {
		return getTableModel().isEqualValueColumn(column) ? "all values equal"
			 : getTableModel().isColumnDataUnique(column) ? "all values unique" : "some values equal";
		}

	private String getMultipleEntries(int column) {
		return getTableModel().isMultiEntryColumn(column) ? "yes" : "no";
		}

	private String getColumnProperties(int column) {
		HashMap<String,String> props = getTableModel().getColumnProperties(column);
		if (props == null || props.isEmpty())
			return "";
		StringBuilder sb = new StringBuilder();
		for (String key:props.keySet()) {
			if (sb.length() != 0)
				sb.append("; ");
			sb.append(key);
			sb.append(":");
			sb.append(props.get(key));
			}
		return sb.toString();
		}
	}
