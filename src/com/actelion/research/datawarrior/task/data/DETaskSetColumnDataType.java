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

package com.actelion.research.datawarrior.task.data;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.task.AbstractMultiColumnTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskSetColumnDataType extends AbstractMultiColumnTask {
	public static final String TASK_NAME = "Set Column Data Type";

	private static final String PROPERTY_DATA_TYPE = "type";
	private static final String PROPERTY_FORCE_CATEGORIES = "forceCategories";

	private JComboBox mComboBoxDataType;
	private JCheckBox mCheckBoxForceCategories;
	private int mDataType;
	private boolean mForceCategories;

	public DETaskSetColumnDataType(Frame owner, CompoundTableModel tableModel) {
		super(owner, tableModel, false);
		}

	/**
	 * Constructor for interactively define and run the task without showing a configuration dialog.
	 * @param owner
	 * @param tableModel
	 * @param column valid column
	 * @param dataType valid summaryMode
	 * @param forceCategories whether categories are enforced and the number of categories is unlimited
	 */
	public DETaskSetColumnDataType(Frame owner, CompoundTableModel tableModel, int column, int dataType, boolean forceCategories) {
		this(owner, tableModel, createColumnList(column), dataType, forceCategories);
		}

	/**
	 * Constructor for interactively define and run the task without showing a configuration dialog.
	 * @param owner
	 * @param tableModel
	 * @param columnList valid column list
	 * @param dataType valid summaryMode
	 * @param forceCategories whether categories are enforced and the number of categories is unlimited
	 */
	public DETaskSetColumnDataType(Frame owner, CompoundTableModel tableModel, int[] columnList, int dataType, boolean forceCategories) {
		super(owner, tableModel, false, columnList);
		mDataType = dataType;
		mForceCategories = forceCategories;
		}

	private static int[] createColumnList(int column) {
		int[] columnList = new int[1];
		columnList[0] = column;
		return columnList;
		}

	@Override
	public boolean isCompatibleColumn(int column) {
		return getTableModel().getColumnSpecialType(column) == null;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		Properties configuration = super.getPredefinedConfiguration();
		if (configuration != null) {
			configuration.put(PROPERTY_DATA_TYPE, CompoundTableConstants.cDataTypeCode[mDataType]);
			if (mForceCategories)
				configuration.put(PROPERTY_FORCE_CATEGORIES, "true");
			}

		return configuration;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.put(PROPERTY_DATA_TYPE, CompoundTableConstants.cDataTypeCode[mComboBoxDataType.getSelectedIndex()]);
		if (mCheckBoxForceCategories.isSelected())
			configuration.put(PROPERTY_FORCE_CATEGORIES, "true");
		return configuration;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mComboBoxDataType.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_DATA_TYPE),
				CompoundTableConstants.cDataTypeCode, CompoundTableConstants.cDataTypeAutomatic));
		mCheckBoxForceCategories.setSelected("true".equals(configuration.getProperty(PROPERTY_FORCE_CATEGORIES)));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mComboBoxDataType.setSelectedIndex(CompoundTableConstants.cDataTypeAutomatic);
		mCheckBoxForceCategories.setSelected(false);
		}

	@Override
	public JPanel createInnerDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };

		mComboBoxDataType = new JComboBox(CompoundTableConstants.cDataTypeText);

		JPanel ip = new JPanel();
		ip.setLayout(new TableLayout(size));
		ip.add(new JLabel("Data type:"), "1,1");
		ip.add(mComboBoxDataType, "3,1");

		mCheckBoxForceCategories = new JCheckBox("Force Categories");
		ip.add(mCheckBoxForceCategories, "1,3,3,3");

		return ip;
		}

	@Override
	public void runTask(Properties configuration) {
		int dataType = findListIndex(configuration.getProperty(PROPERTY_DATA_TYPE),
				CompoundTableConstants.cDataTypeCode, CompoundTableConstants.cDataTypeAutomatic);
		boolean forceCategories = "true".equals(configuration.getProperty(PROPERTY_FORCE_CATEGORIES));

		int[] columnList = getColumnList(configuration);

		for (int column:columnList)
			getTableModel().setExplicitDataType(column, dataType, forceCategories);
		}
	}
