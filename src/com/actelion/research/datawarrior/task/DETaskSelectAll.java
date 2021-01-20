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

package com.actelion.research.datawarrior.task;

import java.util.Properties;

import javax.swing.JTable;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.table.model.CompoundTableModel;

public class DETaskSelectAll extends AbstractTaskWithoutConfiguration {
    public static final String TASK_NAME = "Select All";

    JTable				mTable;
    CompoundTableModel	mTableModel;

	public DETaskSelectAll(DEFrame parent) {
		super(parent, false);
		mTable = parent.getMainFrame().getMainPane().getTable();
		mTableModel = parent.getTableModel();
		}

	@Override
	public boolean isConfigurable() {
		return true;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public void runTask(Properties configuration) {
		int rowCount = mTableModel.getRowCount();
		int columnCount = mTable.getColumnModel().getColumnCount();
		if (rowCount > 0 && columnCount > 0) {
			mTable.getSelectionModel().setSelectionInterval(0, rowCount-1);
			mTable.getColumnModel().getSelectionModel().setSelectionInterval(0, columnCount-1);
			}
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
