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

package com.actelion.research.datawarrior.task.table;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DETable;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.CompoundTableSaver;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskCopyTableCells extends ConfigurableTask {
	public static final String TASK_NAME = "Copy Table Cells";

	private static final String PROPERTY_WITH_HEADER = "withHeader";

	private DETable mTable;
	private CompoundTableModel mTableModel;
	private JCheckBox mCheckBoxWithHeader;
	private boolean mWithHeader,mIsPredefined;

	/**
	 * Constructor for showing the dialog to configure whether copying with or without header.
	 * @param owner
	 * @param tableModel
	 * @param table
	 */
	public DETaskCopyTableCells(Frame owner, CompoundTableModel tableModel, DETable table) {
		super(owner, false);
		mTableModel = tableModel;
		mTable = table;
		mIsPredefined = false;
	}

	/**
	 * Constructor for showing the dialog to configure whether copying with or without header.
	 * @param owner
	 * @param tableModel
	 * @param table
	 */
	public DETaskCopyTableCells(Frame owner, CompoundTableModel tableModel, DETable table, boolean withHeader) {
		super(owner, false);
		mTableModel = tableModel;
		mTable = table;
		mIsPredefined = true;
		mWithHeader = withHeader;
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public Properties getPredefinedConfiguration() {
		Properties configuration = null;

		if (mIsPredefined) {
			configuration = new Properties();
			configuration.setProperty(PROPERTY_WITH_HEADER, mWithHeader ? "true" : "false");
		}

		return configuration;
	}

	@Override
	public JComponent createDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap}, {gap, TableLayout.PREFERRED, gap} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));
		mCheckBoxWithHeader = new JCheckBox("Include table header");
		content.add(mCheckBoxWithHeader, "1,1");

		return content;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_WITH_HEADER, mCheckBoxWithHeader.isSelected() ? "true" : "false");
		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mCheckBoxWithHeader.setSelected("true".equals(configuration.getProperty(PROPERTY_WITH_HEADER, "true")));
	}

	@Override
	public void setDialogConfigurationToDefault() {
		mCheckBoxWithHeader.setSelected(false);
	}

	@Override
	public boolean isConfigurable() {
		if (!mTableModel.hasVisibleSelectedRows() || mTable.getSelectedColumns().length == 0) {
			showErrorMessage("No selected cells found.");
			return false;
		}
		return true;
	}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		return true;
	}

	@Override
	public void runTask(Properties configuration) {
		boolean withHeader = "true".equals(configuration.getProperty(PROPERTY_WITH_HEADER, "true"));
		new CompoundTableSaver(getParentFrame(), mTableModel, mTable).copy(!withHeader);
	}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
	}
}
