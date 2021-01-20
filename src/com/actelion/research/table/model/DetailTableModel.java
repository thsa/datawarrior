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

import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.reaction.ReactionEncoder;
import com.actelion.research.table.CompoundTableChemistryCellRenderer;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

public class DetailTableModel extends DefaultTableModel
            implements CompoundTableListener,TableModelListener {
    private static final long serialVersionUID = 0x20060929;

    private static final String[] cColumnName = {"Column Name", "Value"};

	private CompoundTableModel  mParentModel;
	private CompoundRecord      mParentRecord;

    public DetailTableModel(CompoundTableModel parentModel) {
		super(cColumnName, 0);
		mParentModel = parentModel;
        parentModel.addTableModelListener(this);
		parentModel.addCompoundTableListener(this);
		initialize();
		}

	public CompoundRecord getCompoundRecord() {
		return mParentRecord;
		}

	public CompoundTableModel getParentModel() {
		return mParentModel;
		}

	private void initialize() {
		mParentRecord = mParentModel.getHighlightedRow();
		int rowCount = mParentModel.getColumnCount();
		setRowCount(rowCount);
	    for (int row=0; row<rowCount; row++) {
	    	int column = mParentModel.convertFromDisplayableColumnIndex(row);

	    	// we show a title without summary mode indication, i.e. 'xxxx' instead of 'mean of xxxx'
	    	String alias = mParentModel.getColumnAlias(column);
			setValueAt(alias != null ? alias : mParentModel.getColumnTitleNoAlias(column), row, 0);

			setValueAt(getSecondColumnValue(mParentRecord, row), row, 1);
			}
		}

	public boolean isCellEditable(int row, int col) {
		return false;
		}

    public void tableChanged(TableModelEvent e) {
        if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
            initialize();
            fireTableDataChanged();
            }
        }

    public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cAddColumns
		 || e.getType() == CompoundTableEvent.cRemoveColumns) {
			initialize();
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnData) {
            int column = e.getColumn();
            int row = mParentModel.convertToDisplayableColumnIndex(column);
			if (row != -1 && row < getRowCount())
	    		setValueAt(getSecondColumnValue(mParentRecord, row), row, 1);
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnName) {
            int column = e.getColumn();
            int row = mParentModel.convertToDisplayableColumnIndex(column);
			if (row != -1 && mParentRecord != null)
			    setValueAt(mParentModel.getColumnTitle(column), row, 0);
			}
		}

	public void detailChanged(CompoundRecord record) {
		mParentRecord = record;
		for (int row=0; row<getRowCount(); row++)
			setValueAt(getSecondColumnValue(record, row), row, 1);

		fireTableDataChanged();
		}

	private Object getSecondColumnValue(CompoundRecord record, int row) {
		if (record == null)
			return "";

		int column = mParentModel.convertFromDisplayableColumnIndex(row);
		String type = mParentModel.getColumnSpecialType(column);
		if (CompoundTableModel.cColumnTypeIDCode.equals(type)) {
			int coordinateColumn = (column == -1) ? -1
					: mParentModel.getChildColumn(column, CompoundTableModel.cColumnType2DCoordinates);
			byte[] idcode = (byte[])record.getData(column);
			if (idcode != null
					&& (coordinateColumn == -1 || record.getData(coordinateColumn) == null)
					&& new IDCodeParser().getAtomCount(idcode, 0) > CompoundTableChemistryCellRenderer.ON_THE_FLY_COORD_MAX_ATOMS) {
				return "Calc. 2D-atom coords for large molecules!";
				}

			return mParentModel.getChemicalStructure(record, column, CompoundTableModel.ATOM_COLOR_MODE_ALL, null);
			}

		if (CompoundTableModel.cColumnTypeRXNCode.equals(type)) {
			byte[] rxncode = (byte[])record.getData(column);
			if (rxncode != null)
				return ReactionEncoder.decode(new String(rxncode), true);
			}

		// we show the data also as it is, without calculation of a summary value etc.
		return mParentModel.encodeData(record, column);
		}
	}