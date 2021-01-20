/*
 * Copyright 2020 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91, CH-4123 Allschwil, Switzerland
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

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DETableView;
import com.actelion.research.datawarrior.task.AbstractMultiColumnTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskAddColumnsToGroup extends AbstractMultiColumnTask {
	public static final String TASK_NAME = "Add Columns To Group";

	private static final String PROPERTY_GROUP_NAME = "groupName";

	private DETableView mTableView;
	private JTextField mTextFieldGroupName;
	private String mGroupName;

	public DETaskAddColumnsToGroup(Frame owner, DETableView tableView, CompoundTableModel tableModel) {
		super(owner, tableModel, false);
		mTableView = tableView;
		}

	/**
     * Instantiates this task interactively with a predefined configuration (add selected to group)
	 * or a half-predefined configuration (new group from selected). In the second case the group name
	 * is unknown and must be asked for in the configuration dialog.
     * @param owner
     * @param tableView
     * @param columnList
	 * @param groupName if null, then
     */
	public DETaskAddColumnsToGroup(Frame owner, DETableView tableView, CompoundTableModel tableModel, int[] columnList, String groupName) {
		super(owner, tableModel, false, columnList);
		mTableView = tableView;
		mGroupName = groupName;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		if (mGroupName == null)
			return null;

		Properties configuration = super.getPredefinedConfiguration();
		configuration.setProperty(PROPERTY_GROUP_NAME, mGroupName);
		return configuration;
		}

	@Override
	public JPanel createDialogContent() {
		return getColumnList() == null ? super.createDialogContent() : createInnerDialogContent();
		}

	@Override
	public JPanel createInnerDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap} };
		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		mTextFieldGroupName = new JTextField(12);
		content.add(new JLabel("Group name:"), "1,1");
		content.add(mTextFieldGroupName, "3,1");

		return content;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = (getColumnList() == null) ?
				super.getDialogConfiguration() : createConfigurationFromColumnList();
		String groupName = isInteractive() ? uniqueGroupName(mTextFieldGroupName.getText()) : mTextFieldGroupName.getText();
		configuration.setProperty(PROPERTY_GROUP_NAME, groupName);
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		if (getColumnList() == null)
			super.setDialogConfiguration(configuration);

		mTextFieldGroupName.setText(isInteractive() ?
				uniqueGroupName("Group 1") : configuration.getProperty(PROPERTY_GROUP_NAME, ""));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (getColumnList() == null)
			super.setDialogConfigurationToDefault();

		mTextFieldGroupName.setText(mGroupName != null ? mGroupName : uniqueGroupName("Group 1"));
		}

	private String uniqueGroupName(String name) {
		String[] groupNames = DETaskAbstractTableColumnGroup.getAvailableGroupNames(mTableView.getTableModel());
		while (true) {
			boolean isUnique = true;
			for (String groupName:groupNames) {
				if (groupName.equals(name)) {
					isUnique = false;
					break;
					}
				}

			if (isUnique)
				break;

			int index = name.lastIndexOf(' ');
			if (index == -1)
				name = name + " 2";
			else {
				try {
					int suffix = Integer.parseInt(name.substring(index+1));
					name = name.substring(0, index+1) + (suffix+1);
					}
				catch (NumberFormatException nfe) {
					name = name + " 2";
					}
				}
			}

		return name;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String groupName = configuration.getProperty(PROPERTY_GROUP_NAME, "");
		if (groupName.length() == 0) {
			showErrorMessage("No column group name defined.");
			return false;
			}
		if (groupName.indexOf(';') != -1) {
			showErrorMessage("Column group names may not contain ';'.");
			return false;
			}

		return super.isConfigurationValid(configuration, isLive);
		}

		@Override
	public boolean isCompatibleColumn(int column) {
		return getTableModel().isColumnDisplayable(column);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public void runTask(Properties configuration) {
		String groupName = configuration.getProperty(PROPERTY_GROUP_NAME);
		int[] columnList = getColumnList(configuration);

		for (int column:columnList) {
			String oldGroupNames = getTableModel().getColumnProperty(column, CompoundTableConstants.cColumnPropertyDisplayGroup);
			if (oldGroupNames == null)
				getTableModel().setColumnProperty(column, CompoundTableConstants.cColumnPropertyDisplayGroup, groupName);
			else if (!oldGroupNames.equals(groupName)
				  && !oldGroupNames.startsWith(groupName+";")
				  && !oldGroupNames.contains(";"+groupName+";")
				  && !oldGroupNames.endsWith(";"+groupName))
				getTableModel().setColumnProperty(column, CompoundTableConstants.cColumnPropertyDisplayGroup, oldGroupNames+";"+groupName);
			}
		mTableView.getTable().getTableHeader().repaint();
		}
	}
