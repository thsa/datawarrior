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

package com.actelion.research.table.model;

import com.actelion.research.calc.INumericalDataColumn;

public class NumericalCompoundTableColumn implements INumericalDataColumn {
	public static final int MODE_ALL_ROWS = 0;
	public static final int MODE_CUSTOM_ROWS = 1;
	public static final int MODE_VISIBLE_ROWS = 2;

	private CompoundTableModel	mTableModel;
	private int					mColumn,mMode;
	private int[]               mCustomRow;

	public NumericalCompoundTableColumn(CompoundTableModel tableModel, int column) {
		this(tableModel, column, MODE_ALL_ROWS, null);
		}

	public NumericalCompoundTableColumn(CompoundTableModel tableModel, int column, int mode, int[] customRows) {
		mTableModel = tableModel;
		mColumn = column;
		mMode = mode;
		mCustomRow = customRows;
		}

	@Override
	public double getValueAt(int row) {
        return mMode == MODE_ALL_ROWS ?
		        mTableModel.getTotalDoubleAt(row, mColumn)
		     : mMode == MODE_VISIBLE_ROWS ?
		        mTableModel.getDoubleAt(row, mColumn)
		     : mTableModel.getTotalDoubleAt(mCustomRow[row], mColumn);
		}

	@Override
	public int getValueCount() {
        return mMode == MODE_ALL_ROWS ?
		        mTableModel.getTotalRowCount()
		     : mMode == MODE_VISIBLE_ROWS ?
		        mTableModel.getRowCount()
		     : mCustomRow.length;
		}

	public int getColumn() {
		return mColumn;
		}

	public static int[] compileCustomRows(int listIndex, CompoundTableModel tableModel) {
		CompoundTableListHandler listHandler = tableModel.getListHandler();
		long flag = listHandler.getListMask(listIndex);
		int count = 0;
		for (int row=0; row<tableModel.getTotalRowCount(); row++)
			if ((tableModel.getTotalRecord(row).getFlags() & flag) != 0)
				count++;
		int[] customRows = new int[count];
		count = 0;
		for (int row=0; row<tableModel.getTotalRowCount(); row++)
			if ((tableModel.getTotalRecord(row).getFlags() & flag) != 0)
				customRows[count++] = row;

		return customRows;
		}
	}
