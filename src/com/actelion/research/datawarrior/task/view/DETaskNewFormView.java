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

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFormView;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.DETable;
import com.actelion.research.datawarrior.task.table.DETaskAbstractTableColumnGroup;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Properties;

public class DETaskNewFormView extends DETaskAbstractNewView {
	public static final String TASK_NAME = "New Form View";

	private static final String PROPERTY_COLUMN_MODE = "columnMode";
	private static final String PROPERTY_FORM_COLUMN_COUNT = "formColumnCount";

	private static final int MODE_ALL_COLUMNS = 0;
	private static final int MODE_SELECTED_COLUMNS = 1;
	private static final int MODE_VISIBLE_COLUMNS = 2;
	private static final int MODE_NO_COLUMNS = 3;
	private static final String MODE_GROUP = "Group: ";

	private static final String[] TEXT_MODE = { "All columns",  "Selected columns", "Visible Columns", "No Columns" };
	private static final String[] CODE_MODE = { "all", "selected", "visible", "none" };
	private static final String[] CODE_COUNT = { "automatic", "1",  "2", "3", "4", "5", "6" };
	private static final int DEFAULT_COLUMN_MODE = 0;
	private static final String DEFAULT_COLUMN_COUNT = "automatic";

	private JComboBox mComboBoxColumnMode,mComboBoxColumnCount;

	public DETaskNewFormView(Frame parent, DEMainPane mainPane, String whereViewName) {
		super(parent, mainPane, whereViewName);
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public String getDefaultViewName() {
		return getMainPane().getDefaultViewName(DEMainPane.VIEW_TYPE_FORM, -1);
	}

	@Override
	public void addInnerDialogContent(JPanel content) {
		int gap = HiDPIHelper.scale(8);
		JPanel p = new JPanel();
		double[][] size = { {TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED},
				{TableLayout.PREFERRED, gap, TableLayout.PREFERRED} };
		p.setLayout(new TableLayout(size));

		p.add(new JLabel("Create form fields for:"), "0,0");
		mComboBoxColumnMode = new JComboBox(TEXT_MODE);
		String[] groupNames = DETaskAbstractTableColumnGroup.getAvailableGroupNames(getMainPane().getTableModel());
		if (groupNames != null)
			for (String gn:groupNames)
				mComboBoxColumnMode.addItem(MODE_GROUP+gn);
		mComboBoxColumnMode.setEditable(!isInteractive());
		p.add(mComboBoxColumnMode, "2,0");

		p.add(new JLabel("Form column count:"), "0,2");
		mComboBoxColumnCount = new JComboBox(CODE_COUNT);
		p.add(mComboBoxColumnCount, "2,2");

		content.add(p, "1,7,3,7");
	}

	@Override
	public Properties getPredefinedConfiguration() {
		return null;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		int mode = mComboBoxColumnMode.getSelectedIndex();
		if (mode < CODE_MODE.length)
			configuration.setProperty(PROPERTY_COLUMN_MODE, CODE_MODE[mComboBoxColumnMode.getSelectedIndex()]);
		else
			configuration.setProperty(PROPERTY_COLUMN_MODE, (String)mComboBoxColumnMode.getSelectedItem());
		configuration.setProperty(PROPERTY_FORM_COLUMN_COUNT, CODE_COUNT[mComboBoxColumnCount.getSelectedIndex()]);
		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		String modeCode = configuration.getProperty(PROPERTY_COLUMN_MODE);
		int mode = findListIndex(modeCode, CODE_MODE, -1);
		if (mode != -1)
			mComboBoxColumnMode.setSelectedIndex(mode);
		else if (modeCode != null && modeCode.startsWith(MODE_GROUP))
			mComboBoxColumnMode.setSelectedItem(modeCode);
		String count = configuration.getProperty(PROPERTY_FORM_COLUMN_COUNT, DEFAULT_COLUMN_COUNT);
		mComboBoxColumnCount.setSelectedItem(count);
	}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mComboBoxColumnMode.setSelectedIndex(DEFAULT_COLUMN_MODE);
		mComboBoxColumnCount.setSelectedItem(DEFAULT_COLUMN_COUNT);
	}

	@Override
	public boolean isConfigurable() {
		return true;
	}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (!super.isConfigurationValid(configuration, isLive))
			return false;

		String modeCode = configuration.getProperty(PROPERTY_COLUMN_MODE);
		if (findListIndex(modeCode, CODE_MODE, -1) == -1
		 && !modeCode.startsWith(MODE_GROUP)) {
			showErrorMessage("Column definition is invalid.");
			return false;
		}

		return true;
	}

	@Override
	public void createNewView(String viewName, String whereView, String where, Properties configuration) {
		String modeCode = configuration.getProperty(PROPERTY_COLUMN_MODE);
		int columnMode = findListIndex(modeCode, CODE_MODE, -1);

		int formColumnCount = -1;   // automatic
		try {
			formColumnCount = Integer.parseInt(configuration.getProperty(PROPERTY_FORM_COLUMN_COUNT));
			}
		catch (NumberFormatException nfe) {}

		CompoundTableModel tableModel = getMainPane().getTableModel();
		DETable table = getMainPane().getTable();
		boolean[] includeTableColumn = new boolean[tableModel.getTotalColumnCount()];
		switch (columnMode) {
		case MODE_ALL_COLUMNS:
			Arrays.fill(includeTableColumn, true);	// non-displayable columns
			break;
		case MODE_SELECTED_COLUMNS:
			for (int column=0; column<includeTableColumn.length; column++)
				includeTableColumn[column] = table.isColumnSelected(table.convertTotalColumnIndexToView(column));
			break;
		case MODE_VISIBLE_COLUMNS:
			for (int column=0; column<includeTableColumn.length; column++)
				if (table.convertTotalColumnIndexToView(column) != -1)
					includeTableColumn[column] = true;
			break;
		case MODE_NO_COLUMNS:
			break;
		default:
			if (modeCode.startsWith(MODE_GROUP)) {
				String groupName = modeCode.substring(MODE_GROUP.length());
				for (int column=0; column<includeTableColumn.length; column++) {
					String groups = tableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyDisplayGroup);
					if (groups != null) {
						for (String group:groups.split(";")) {
							if (groupName.equals(group)) {
								includeTableColumn[column] = true;
								break;
								}
							}
						}
					}
				}
			break;
		}

		DEFormView view = (DEFormView)getMainPane().createNewView(viewName, DEMainPane.VIEW_TYPE_FORM, whereView, where, -1);
		view.createLayout(includeTableColumn, true, true, formColumnCount);
	}
}
