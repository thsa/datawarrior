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

package com.actelion.research.table;

import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.model.DetailTableModel;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.TreeMap;

public class JDetailTable extends JTable implements TableModelListener {
    private static final long serialVersionUID = 0x20061009;

    private static final int cTextRowHeight = 16;
	private static final int cMultiLineRowHeight = 32;
	private static final int cSpecialRowHeight = 64;

	private DetailTableModel mDetailModel;
	private TreeMap<String,Integer> mRowHeightMap;

	public JDetailTable(DetailTableModel detailTableModel) {
		super(detailTableModel);

		mDetailModel = detailTableModel;
		mRowHeightMap = new TreeMap<String,Integer>();

		for (int column=0; column<2; column++)
			getColumnModel().getColumn(column).setCellRenderer(
						new DetailTableCellRenderer(detailTableModel));

		setColumnSelectionAllowed(false);
		setRowSelectionAllowed(false);

		new TableRowHeightAdjuster(this);
		}

    public void setColorHandler(CompoundTableColorHandler colorHandler) {
	    for (int column=0; column<2; column++)
	    	((DetailTableCellRenderer)getColumnModel().getColumn(column).getCellRenderer()).setColorHandler(colorHandler);
    	}

	@Override
	public void setRowHeight(int row, int rowHeight) {
		super.setRowHeight(row, rowHeight);

		CompoundTableModel tableModel = mDetailModel.getParentModel();
		mRowHeightMap.put(tableModel.getColumnTitleNoAlias(mDetailModel.getParentColumn(row)), rowHeight);
		}

	public void tableChanged(TableModelEvent e) {
		super.tableChanged(e);

		if (mDetailModel != null && e.getColumn() == TableModelEvent.ALL_COLUMNS) {
			CompoundTableModel tableModel = mDetailModel.getParentModel();
			for (int row=0; row<mDetailModel.getRowCount(); row++) {
                int column = mDetailModel.getParentColumn(row);
				Integer value = mRowHeightMap.get(tableModel.getColumnTitleNoAlias(column));
				if (value != null) {
					setRowHeight(row, value.intValue());
					}
				else {
					String specialType = tableModel.getColumnSpecialType(column);
					if (specialType != null)
						setRowHeight(row, HiDPIHelper.scale(cSpecialRowHeight));
					else if (tableModel.isMultiLineColumn(row))
						setRowHeight(row, HiDPIHelper.scale(cMultiLineRowHeight));
					else
						setRowHeight(row, HiDPIHelper.scale(cTextRowHeight));
					}
				}
			}
		}
	}
