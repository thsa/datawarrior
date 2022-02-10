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

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DETableView;
import com.actelion.research.table.model.CompoundTableModel;

import java.awt.*;
import java.util.Properties;

public class DETaskRemoveColumnGroup extends DETaskAbstractTableColumnGroup {
	public static final String TASK_NAME = "Remove Column Group";

	public DETaskRemoveColumnGroup(Frame owner, DETableView tableView, CompoundTableModel tableModel) {
		super(owner, tableView, tableModel);
		}

    /**
     * Instantiates this task interactively with a pre-defined configuration.
     * @param owner
     * @param tableView
     * @param groupName
     */
	public DETaskRemoveColumnGroup(Frame owner, DETableView tableView, CompoundTableModel tableModel, String groupName) {
		super(owner, tableView, tableModel, groupName);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public void prepareColumnGroupActions(Properties configuration) {}

	@Override
	public void doColumnGroupAction(int column, boolean isGroupMember, String groupName) {
		if (isGroupMember) {
			CompoundTableModel tableModel = getTableView().getTableModel();
			String oldGroupNames = tableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyDisplayGroup);
			if (oldGroupNames.equals(groupName))
				tableModel.setColumnProperty(column, CompoundTableConstants.cColumnPropertyDisplayGroup, null);
			else if (oldGroupNames.startsWith(groupName+";"))
				tableModel.setColumnProperty(column, CompoundTableConstants.cColumnPropertyDisplayGroup, oldGroupNames.substring(groupName.length()+1));
			else if (oldGroupNames.endsWith(";"+groupName))
				tableModel.setColumnProperty(column, CompoundTableConstants.cColumnPropertyDisplayGroup, oldGroupNames.substring(0, oldGroupNames.length()-groupName.length()-1));
			else if (oldGroupNames.contains(";"+groupName+";")) {
				int index = oldGroupNames.indexOf(";"+groupName+";");
				String newGroupNames = oldGroupNames.substring(0, index) + oldGroupNames.substring(index+groupName.length()+1);
				tableModel.setColumnProperty(column, CompoundTableConstants.cColumnPropertyDisplayGroup, newGroupNames);
				}
			}
		}
	}
