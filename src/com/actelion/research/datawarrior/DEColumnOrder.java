/*
 * Copyright 2019 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91, CH-4123 Allschwil, Switzerland
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

package com.actelion.research.datawarrior;

import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableListener;
import com.actelion.research.table.model.CompoundTableModel;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumnModel;
import java.util.ArrayList;

/**
 * This class keeps track of the visible column order in relation to the CompoundTableModel's native order.
 * In DataWarrior column can be hidden manually or by applying the column name filter. When columns are un-hidden,
 * they should appear at the correct position reflecting the tracked column order.
 */
public class DEColumnOrder extends ArrayList<Integer> implements TableColumnModelListener,CompoundTableListener {
	// maintains the list of all displayable CompoundTableModel column indexes in display order
	private CompoundTableModel mTableModel;
	private TableColumnModel mColumnModel;
	private boolean mNeglectMoveColumnEvents;

	public DEColumnOrder(CompoundTableModel tableModel, TableColumnModel columnModel) {
		mTableModel = tableModel;
		mColumnModel = columnModel;

		tableModel.addCompoundTableListener(this);
		columnModel.addColumnModelListener(this);

		// Start with the native oreder of displayable columns.
		initialize();
		}

	public void setNeglectMoveColumnEvents(boolean b) {
		mNeglectMoveColumnEvents = b;
		}

	private void initialize() {
		clear();
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.isColumnDisplayable(column))
				add(Integer.valueOf(column));
		}

	/**
	 * @return whether this order reflects the native CompoundTableModel order of columns
	 */
	public boolean isNative() {
		int recent = -1;
		for (int column:this) {
			if (column < recent)
				return false;
			recent = column;
			}
		return true;
		}

	@Override
	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cNewTable) {
			initialize();
			}
		else if (e.getType() == CompoundTableEvent.cAddColumns) {
			for (int column=e.getColumn(); column<mTableModel.getTotalColumnCount(); column++)
				if (mTableModel.isColumnDisplayable(column))
					add(Integer.valueOf(column));
			}
		else if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			int[] map = e.getMapping();
			for (int i=size()-1; i>=0; i--) {
				int newIndex = map[get(i).intValue()];
				if (newIndex == -1)
					remove(i);
				else
					set(i, Integer.valueOf(newIndex));
				}
			}
		}

	/** Tells listeners that a column was added to the model. */
	@Override
	public void columnAdded(TableColumnModelEvent e) {}

	/** Tells listeners that a column was removed from the model. */
	@Override
	public void columnRemoved(TableColumnModelEvent e) {}

	/** Tells listeners that a column was repositioned. */
	@Override
	public void columnMoved(TableColumnModelEvent e) {
		if (!mNeglectMoveColumnEvents) {
			int fromIndex = e.getFromIndex();
			int toIndex = e.getToIndex();

			if (fromIndex == toIndex)
				return;

			int movedModelColumn = mTableModel.convertFromDisplayableColumnIndex(mColumnModel.getColumn(toIndex).getModelIndex());
			remove(Integer.valueOf(movedModelColumn));

			// If we move to the very left of the table, then add the removed model column index at the start of the list
			int insertIndex = 0;

			// Otherwise find the list position of the replaced column's model column index and insert at that position
			if (toIndex != 0) {
				int leftModelColumn = mTableModel.convertFromDisplayableColumnIndex(mColumnModel.getColumn(toIndex - 1).getModelIndex());
				insertIndex = indexOf(leftModelColumn) + 1;
				}

			add(insertIndex, Integer.valueOf(movedModelColumn));

//printOrder("moved: ");
			}
		}

//	public void printOrder(String msg) {
//		System.out.println(msg); for (int column:mOrderedColumnList) System.out.println(Integer.toString(column)+" "+mTableModel.getColumnTitle(column)); System.out.println();
//		}

	/** Tells listeners that a column was moved due to a margin change. */
	@Override
	public void columnMarginChanged(ChangeEvent e) {}

	@Override
	public void columnSelectionChanged(ListSelectionEvent e) {}

	protected int getColumnIndexForInsertion(int modelColumn) {
		int listIndex = indexOf(Integer.valueOf(modelColumn));	// index in ordered displayable column list
		int insertIndex = mColumnModel.getColumnCount(); // default is after the last entry
		for (int i=0; i<mColumnModel.getColumnCount(); i++) {
			int mc = mTableModel.convertFromDisplayableColumnIndex(mColumnModel.getColumn(i).getModelIndex());
			int li = indexOf(Integer.valueOf(mc));	// index in ordered displayable column list
			if (listIndex < li) {
				insertIndex = i;
				break;
				}
			}
//print("insert: tableModelColumn="+modelColumn+" insertIndex="+insertIndex);
		return insertIndex;
		}
	}
