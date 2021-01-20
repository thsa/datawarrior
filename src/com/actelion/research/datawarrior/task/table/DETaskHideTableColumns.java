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
import com.actelion.research.datawarrior.task.AbstractMultiColumnTask;
import com.actelion.research.table.model.CompoundTableModel;

import java.awt.*;
import java.util.Properties;

public class DETaskHideTableColumns extends AbstractMultiColumnTask {
	public static final String TASK_NAME = "Hide Table Columns";

	private DETableView mTableView;

	public DETaskHideTableColumns(Frame owner, DETableView tableView, CompoundTableModel tableModel) {
		super(owner, tableModel, false);
		mTableView = tableView;
		}

    /**
     * Instantiates this task interactively with a pre-defined configuration.
     * @param owner
     * @param tableView
     * @param columnList
     */
	public DETaskHideTableColumns(Frame owner, DETableView tableView, CompoundTableModel tableModel, int[] columnList) {
		super(owner, tableModel, false, columnList);
		mTableView = tableView;
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
		int[] columnList = getColumnList(configuration);

		for (int column:columnList)
            mTableView.setColumnVisibility(column, false);
		}
	}
