package com.actelion.research.datawarrior.task;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.util.ByteArrayComparator;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.util.Arrays;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

public class DETaskExtendRowSelection extends AbstractSingleColumnTask {
	public static final String TASK_NAME = "Extend Row Selection";

	// Currently only supported mode is MODE_CONNECTED. One could think of MODE_BY_CATEGORY
	private static final String PROPERTY_MODE = "mode";

	private static final String[] MODE_CODE = {"equal", "connected", "parents", "children"};
	private static final String[] MODE_TEXT = {"rows with equal data", "connected rows", "connected parents", "connected children"};
	private static final int MODE_EQUAL_DATA = 0;
	private static final int MODE_CONNECTED_ALL = 1;
	private static final int MODE_CONNECTED_PARENTS = 2;
	private static final int MODE_CONNECTED_CHILDREN = 3;

	private JComboBox mComboBoxMode;
	private int mMode;

	public DETaskExtendRowSelection(DEFrame parent, int column, int mode) {
		super(parent, parent.getTableModel(), true, column);
		mMode = mode;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		Properties configuration = super.getPredefinedConfiguration();
		if (configuration != null)
			configuration.setProperty(PROPERTY_MODE, MODE_CODE[mMode]);

		return configuration;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		if (configuration != null)
			configuration.setProperty(PROPERTY_MODE, MODE_CODE[mComboBoxMode.getSelectedIndex()]);

		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		int mode = findListIndex(configuration.getProperty(PROPERTY_MODE), MODE_CODE, 0);
		mComboBoxMode.setSelectedIndex(mode);
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mComboBoxMode.setSelectedIndex(0);
		}

	@Override
	public JPanel createInnerDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED},
				{gap, TableLayout.PREFERRED, gap} };
		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		content.add(new JLabel("Extend selection to all", JLabel.RIGHT), "0,1");
		mComboBoxMode = new JComboBox(MODE_TEXT);
		content.add(mComboBoxMode, "2,1");

		return content;
		}

	@Override
	public boolean isCompatibleColumn(int column) {
		return getTableModel().isColumnDisplayable(column);

//		String refColumn = getTableModel().getColumnProperty(column, CompoundTableConstants.cColumnPropertyReferencedColumn);
//		if (refColumn != null && getTableModel().findColumn(refColumn) != -1)
//			return true;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public String getColumnLabelText() {
		return "data/connection column:";
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (!super.isConfigurationValid(configuration, isLive))
			return false;

		if (isLive) {
			int mode = findListIndex(configuration.getProperty(PROPERTY_MODE), MODE_CODE, MODE_CONNECTED_ALL);
			if (mode != MODE_EQUAL_DATA) {
				int column = getColumn(configuration);
				String refColumn = getTableModel().getColumnProperty(column, CompoundTableConstants.cColumnPropertyReferencedColumn);
				if (refColumn == null) {
					showErrorMessage("Defined mode '"+MODE_CODE[mode]+" requires chosen column to reference another column.");
					return false;
					}
				if (getTableModel().findColumn(refColumn) == -1) {
					showErrorMessage("References column '"+refColumn+"' not found.");
					return false;
					}
				}
			}
		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		int mode = findListIndex(configuration.getProperty(PROPERTY_MODE), MODE_CODE, MODE_CONNECTED_ALL);
		if (mode == MODE_CONNECTED_ALL || mode == MODE_CONNECTED_PARENTS || mode == MODE_CONNECTED_CHILDREN)
			selectReferenced(configuration, mode);
		else
			selectEqual(configuration);
		}

	private void selectEqual(Properties configuration) {
		TreeSet<byte[]> selectedSet = new TreeSet<>(new ByteArrayComparator());
		int column = getColumn(configuration);
		for (int row=0; row<getTableModel().getTotalRowCount() && !threadMustDie(); row++) {
			CompoundRecord record = getTableModel().getTotalRecord(row);
			if (record.isSelected()) {
				byte[] bytes = (byte[])record.getData(column);
				if (bytes != null)
					selectedSet.add(bytes);
				}
			}

		int count = 0;
		for (int row=0; row<getTableModel().getTotalRowCount() && !threadMustDie(); row++) {
			CompoundRecord record = getTableModel().getTotalRecord(row);
			if (!record.isSelected()) {
				byte[] bytes = (byte[])record.getData(column);
				if (bytes != null && selectedSet.contains(bytes)) {
					record.setSelection(true);
					count++;
					}
				}
			}

		if (count != 0)
			getTableModel().invalidateSelectionModel();
		}

	private void selectReferenced(Properties configuration, int mode) {
		int referencingColumn = getColumn(configuration);
		int referencedColumn = getTableModel().findColumn(getTableModel().getColumnProperty(referencingColumn, CompoundTableConstants.cColumnPropertyReferencedColumn));
		boolean columnIsBidirectional = CompoundTableConstants.cColumnPropertyReferenceTypeRedundant.equals(
				getTableModel().getColumnProperty(referencingColumn, CompoundTableConstants.cColumnPropertyReferenceType));

		TreeMap<String,Integer> idToRowMap = new TreeMap<>();
		for (int row=0; row<getTableModel().getTotalRowCount() && !threadMustDie(); row++) {
			String id = getTableModel().getTotalValueAt(row, referencedColumn);
			if (id != null)
				idToRowMap.put(id, row);
			}

		boolean selectParents = (mode == MODE_CONNECTED_PARENTS);
		boolean selectChildren = (mode == MODE_CONNECTED_CHILDREN);
		boolean selectAllConnected = false;
		if (columnIsBidirectional) {
			selectParents = false;
			selectChildren = true;  // sufficient, because column contains children and(!) parent links
			}
		else if (mode == MODE_CONNECTED_ALL) {
			selectAllConnected = true;
			selectParents = false;
			selectChildren = false;
			}

		int count = 0;
		if (selectParents || selectAllConnected) {
			int[][] rowToParents = new int[getTableModel().getTotalRowCount()][];
			for (int row=0; row<getTableModel().getTotalRowCount(); row++) {
				String[] refIDs = getTableModel().separateEntries(getTableModel().getTotalValueAt(row, referencingColumn));
				for (String refID:refIDs) {
					if (refID.length() != 0) {
						int childRow = idToRowMap.get(refID);
						addRowReference(rowToParents, childRow, row);
						if (selectAllConnected)
							addRowReference(rowToParents, row, childRow);
						}
					}
				}

			for (int row=0; row<getTableModel().getTotalRowCount() && !threadMustDie(); row++)
				if (getTableModel().getTotalRecord(row).isSelected())
					count += selectReferencedParents(row, rowToParents);
			}

		if (selectChildren) {
			for (int row = 0; row<getTableModel().getTotalRowCount() && !threadMustDie(); row++)
				if (getTableModel().getTotalRecord(row).isSelected())
					count += selectReferencedChildren(idToRowMap, row, referencingColumn);
			}

		if (count != 0)
			getTableModel().invalidateSelectionModel();
		}

	private void addRowReference(int[][] rowToParents, int sourceRow, int destRow) {
		if (rowToParents[sourceRow] == null) {
			rowToParents[sourceRow] = new int[1];
			rowToParents[sourceRow][0] = destRow;
			}
		else {
			int len = rowToParents[sourceRow].length;
			rowToParents[sourceRow] = Arrays.copyOf(rowToParents[sourceRow], len+1);
			rowToParents[sourceRow][len] = destRow;
			}
		}

	private int selectReferencedParents(int row, int[][] rowToParents) {
		int count = 0;
		int[] parentRows = rowToParents[row];
		if (parentRows != null) {
			for (int parentRow:parentRows) {
				if (!getTableModel().getTotalRecord(parentRow).isSelected()) {
					getTableModel().getTotalRecord(parentRow).setSelection(true);
					count++;
					count += selectReferencedParents(row, rowToParents);
					}
				}
			}
		return count;
		}

	private int selectReferencedChildren(TreeMap<String,Integer> idToRowMap, int row, int referencingColumn) {
		int count = 0;
		String[] refIDs = getTableModel().separateEntries(getTableModel().getTotalValueAt(row, referencingColumn));
		for (String refID:refIDs) {
			if (refID.length() != 0) {
				int referencedRow = idToRowMap.get(refID);
				if (!getTableModel().getTotalRecord(referencedRow).isSelected()) {
					getTableModel().getTotalRecord(referencedRow).setSelection(true);
					count++;
					count += selectReferencedChildren(idToRowMap, referencedRow, referencingColumn);
					}
				}
			}
		return count;
		}
	}
