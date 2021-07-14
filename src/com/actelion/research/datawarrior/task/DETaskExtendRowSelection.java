package com.actelion.research.datawarrior.task;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;

import javax.swing.*;
import java.util.Properties;
import java.util.TreeMap;

public class DETaskExtendRowSelection extends AbstractSingleColumnTask {
	public static final String TASK_NAME = "Extend Row Selection";

	// Currently only supported mode is MODE_CONNECTED. One could think of MODE_BY_CATEGORY
	private static final String PROPERTY_MODE = "mode";
	private static final String MODE_CONNECTED = "CONNECTED";

	public DETaskExtendRowSelection(DEFrame parent, int column) {
		super(parent, parent.getTableModel(), true, column);
		}

	@Override
	public boolean isCompatibleColumn(int column) {
		String refColumn = getTableModel().getColumnProperty(column, CompoundTableConstants.cColumnPropertyReferencedColumn);
		return refColumn != null && getTableModel().findColumn(refColumn) != -1;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public JPanel createInnerDialogContent() {
		return null;
		}

	@Override
	public String getColumnLabelText() {
		return "Connection column:";
	}

	@Override
	public void runTask(Properties configuration) {
		int referencingColumn = getColumn(configuration);
		int referencedColumn = getTableModel().findColumn(getTableModel().getColumnProperty(referencingColumn, CompoundTableConstants.cColumnPropertyReferencedColumn));

		TreeMap<String,Integer> idToRowMap = new TreeMap<>();
		for (int row=0; row<getTableModel().getTotalRowCount() && !threadMustDie(); row++) {
			String id = getTableModel().getTotalValueAt(row, referencedColumn);
			if (id != null)
				idToRowMap.put(id, row);
			}

		boolean[] rowHandled = new boolean[getTableModel().getTotalRowCount()];
		int count = 0;
		for (int row=0; row<getTableModel().getTotalRowCount() && !threadMustDie(); row++)
			if (!rowHandled[row] && getTableModel().getTotalRecord(row).isSelected())
				count += selectReferenced(idToRowMap, rowHandled, row, referencingColumn);

		if (count != 0)
			getTableModel().invalidateSelectionModel();
		}

	private int selectReferenced(TreeMap<String,Integer> idToRowMap, boolean[] rowHandled, int referencingRow, int referencingColumn) {
		int count = 0;
		String[] refIDs = getTableModel().separateEntries(getTableModel().getTotalValueAt(referencingRow, referencingColumn));
		for (String refID:refIDs) {
			if (refID.length() != 0) {
				int row = idToRowMap.get(refID);
				if (!rowHandled[row] && !getTableModel().getTotalRecord(row).isSelected()) {
					getTableModel().getTotalRecord(row).setSelection(true);
					count++;
					count += selectReferenced(idToRowMap, rowHandled, row, referencingColumn);
					}
				}
			}
		return count;
		}
	}
