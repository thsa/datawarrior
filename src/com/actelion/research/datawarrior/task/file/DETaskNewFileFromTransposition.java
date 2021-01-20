/*
 * Copyright 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91, CH-4123 Allschwil, Switzerland
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

package com.actelion.research.datawarrior.task.file;

import java.util.Properties;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.AbstractSingleColumnTask;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;


public class DETaskNewFileFromTransposition extends AbstractSingleColumnTask {
	public static final String TASK_NAME = "New File From Transposition";

	private DEFrame mSourceFrame,mTargetFrame;
	private DataWarrior mApplication;

	public DETaskNewFileFromTransposition(DEFrame sourceFrame, DataWarrior application) {
		super(sourceFrame, sourceFrame.getTableModel(), false);
		mSourceFrame = sourceFrame;
		mApplication = application;
	}

	@Override
	public boolean allowColumnNoneItem() {
		return true;
	}

	@Override
	public boolean isCompatibleColumn(int column) {
		return getTableModel().getColumnSpecialType(column) == null
			&& getTableModel().isColumnDataComplete(column)
			&& getTableModel().isColumnDataUnique(column);
		}

	@Override
	public String getColumnLabelText() {
		return "Take new column title from:";
	}

	@Override
	public boolean isConfigurable() {
		CompoundTableModel tableModel = mSourceFrame.getTableModel();

		if (tableModel.getTotalRowCount() == 0 || tableModel.getTotalColumnCount() == 0) {
			showErrorMessage("No columns or rows found.");
			return false;
		}

		return true;
	}

	@Override
	public DEFrame getNewFrontFrame() {
		return mTargetFrame;
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public void runTask(Properties configuration) {
		CompoundTableModel sourceTableModel = mSourceFrame.getTableModel();

		int headerColumn = super.getColumn(configuration);
		int columnCount = sourceTableModel.getTotalRowCount() + 1;	// every row including the header will give a new column
		int rowCount = sourceTableModel.getTotalColumnCount() - (headerColumn == NO_COLUMN ? 0 : 1);
		for (int i=0; i<sourceTableModel.getTotalColumnCount(); i++)
			if (sourceTableModel.getColumnSpecialType(i) != null)
				rowCount--;

		mTargetFrame = mApplication.getEmptyFrame("Transposition of "+mSourceFrame.getTitle());
		CompoundTableModel targetTableModel = mTargetFrame.getTableModel();
		targetTableModel.initializeTable(rowCount, columnCount);

		targetTableModel.setColumnName("Column Name", 0);
		for (int column=1; column<columnCount; column++)
			targetTableModel.setColumnName(headerColumn == NO_COLUMN ?
					"Row "+column : sourceTableModel.getTotalValueAt(column-1, headerColumn), column);

		int row = 0;
		for (int sourceColumn=0; sourceColumn<sourceTableModel.getTotalColumnCount(); sourceColumn++) {
			if (sourceColumn != headerColumn && sourceTableModel.getColumnSpecialType(sourceColumn) == null) {
				targetTableModel.setTotalValueAt(sourceTableModel.getColumnTitle(sourceColumn), row, 0);
				for (int column=1; column<columnCount; column++)
					targetTableModel.setTotalValueAt(sourceTableModel.getTotalValueAt(column-1, sourceColumn), row, column);

				row++;
			}
		}

		targetTableModel.finalizeTable(CompoundTableEvent.cSpecifierDefaultFiltersAndViews, getProgressController());
	}
}
