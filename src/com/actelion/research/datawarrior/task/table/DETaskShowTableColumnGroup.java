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

import com.actelion.research.datawarrior.DETableView;
import com.actelion.research.table.model.CompoundTableModel;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskShowTableColumnGroup extends DETaskAbstractTableColumnGroup {
	public static final String TASK_NAME = "Show Table Column Group";

	private static final String PROPERTY_HIDE_OTHERS = "hideOthers";

	private JCheckBox mCheckBoxHideOthers;
	private boolean mHideOthers;

	public DETaskShowTableColumnGroup(Frame owner, DETableView tableView, CompoundTableModel tableModel) {
		super(owner, tableView, tableModel);
		}

    /**
     * Instantiates this task interactively with a pre-defined configuration.
     * @param owner
     * @param tableView
     * @param groupName
     */
	public DETaskShowTableColumnGroup(Frame owner, DETableView tableView, CompoundTableModel tableModel, String groupName, boolean hideOthers) {
		super(owner, tableView, tableModel, groupName);
		mHideOthers = hideOthers;
		}

	@Override
	public JComponent createInnerDialogContent() {
		mCheckBoxHideOthers = new JCheckBox("Hide All Other Columns");
		return mCheckBoxHideOthers;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		Properties configuration = super.getPredefinedConfiguration();
		if (configuration == null)
			return null;

		configuration.setProperty(PROPERTY_HIDE_OTHERS, mHideOthers ? "true" : "false");
		return configuration;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_HIDE_OTHERS, mCheckBoxHideOthers.isSelected() ? "true" : "false");
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mCheckBoxHideOthers.setSelected(!"false".equals(configuration.getProperty(PROPERTY_HIDE_OTHERS)));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mCheckBoxHideOthers.setSelected(true);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public void prepareColumnGroupActions(Properties configuration) {
		mHideOthers = !"false".equals(configuration.getProperty(PROPERTY_HIDE_OTHERS));
		}

	@Override
	public void doColumnGroupAction(int column, boolean isGroupMember, String groupName) {
		boolean isShown = getTableView().getTable().convertTotalColumnIndexToView(column) != -1;
		if ((!isShown || mHideOthers) && (isShown != isGroupMember))
			getTableView().setColumnVisibility(column, isGroupMember);
		}
	}
