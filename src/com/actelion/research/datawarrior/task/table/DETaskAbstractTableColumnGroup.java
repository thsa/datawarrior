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

import com.actelion.research.chem.SortedStringList;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DETableView;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public abstract class DETaskAbstractTableColumnGroup extends ConfigurableTask {

	private static final String PROPERTY_GROUP_NAME = "groupName";

	private DETableView mTableView;
	private CompoundTableModel mTableModel;
	private String mColumnGroupName;
	private JComboBox mComboBox;

	public DETaskAbstractTableColumnGroup(Frame owner, DETableView tableView, CompoundTableModel tableModel) {
		super(owner, false);
		mTableView = tableView;
		mTableModel = tableModel;
		}

	/**
	 * Instantiates this task interactively with a pre-defined configuration.
	 * @param owner
	 * @param tableView
	 * @param groupName
	 */
	public DETaskAbstractTableColumnGroup(Frame owner, DETableView tableView, CompoundTableModel tableModel, String groupName) {
		super(owner, false);
		mTableView = tableView;
		mTableModel = tableModel;
		mColumnGroupName = groupName;
		}

	@Override
	public final JComponent createDialogContent() {
		JPanel p = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap},
				{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };
		p.setLayout(new TableLayout(size));

		p.add(new JLabel("Column group:"), "1,1");
		mComboBox = new JComboBox(getAvailableGroupNames(mTableModel));
		mComboBox.setEditable(true);
		p.add(mComboBox, "3,1");

		JComponent innerContent = createInnerDialogContent();
		if (innerContent != null)
			p.add(innerContent, "1,3,3,3");

		return p;
		}

	public JComponent createInnerDialogContent() {
		return null;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		if (mColumnGroupName == null)
			return null;

		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_GROUP_NAME, mColumnGroupName);
		return configuration;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		String item = (String)mComboBox.getSelectedItem();
		if (item != null && item.length() != 0)
			configuration.setProperty(PROPERTY_GROUP_NAME, (String)mComboBox.getSelectedItem());
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mComboBox.setSelectedItem(configuration.getProperty(PROPERTY_GROUP_NAME, ""));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		}

	@Override
	public boolean isConfigurable() {
		return getAvailableGroupNames(mTableModel).length != 0;
	}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String groupName = configuration.getProperty(PROPERTY_GROUP_NAME, "");
		if (groupName.length() == 0) {
			showErrorMessage("No column group name defined.");
			return false;
			}

		if (isLive) {
			String[] groupNames = getAvailableGroupNames(mTableModel);
			boolean found = false;
			for (String name:groupNames) {
				if (name.equals(groupName)) {
					found = true;
					break;
					}
				}
			if (!found) {
				showErrorMessage("Column group '"+groupName+"' not found.");
				return false;
				}
			}

		return true;
		}

	public static String[] getAvailableGroupNames(CompoundTableModel tableModel) {
		SortedStringList groupNameList = new SortedStringList();
		for (int column=0; column<tableModel.getTotalColumnCount(); column++) {
			if (tableModel.isColumnDisplayable(column)) {
				String groupNames = tableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyDisplayGroup);
				if (groupNames != null) {
					String[] groupName = groupNames.split(";");
					for (String name:groupName)
						groupNameList.addString(name);
					}
				}
			}
		return groupNameList.toArray();
		}

	public DETableView getTableView() {
		return mTableView;
		}

 	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
	public void runTask(Properties configuration) {
		prepareColumnGroupActions(configuration);
		String groupName = configuration.getProperty(PROPERTY_GROUP_NAME);
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
			if (mTableModel.isColumnDisplayable(column)) {
				String groupNames = mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyDisplayGroup);
				boolean isGroupMember = groupNames != null
						&& (groupNames.equals(groupName)
						|| groupNames.startsWith(groupName+";")
						|| groupNames.endsWith(";"+groupName)
						|| groupNames.contains(";"+groupName+";"));
				doColumnGroupAction(column, isGroupMember, groupName);
				}
			}
		mTableView.getTable().getTableHeader().repaint();
		}

	public abstract void prepareColumnGroupActions(Properties configuration);
	public abstract void doColumnGroupAction(int column, boolean isGroupMember, String groupName);
	}
