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

package com.actelion.research.datawarrior.task.list;

import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableListHandler;
import info.clearthought.layout.TableLayout;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;

import javax.swing.*;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.table.model.CompoundTableModel;


public class DETaskNewRowList extends ConfigurableTask implements ActionListener {
	public static final String TASK_NAME = "New Row List";

	public static final int MODE_SELECTED = 0;
	public static final int MODE_VISIBLE = 1;
	public static final int MODE_HIDDEN = 2;
	public static final int MODE_EMPTY = 3;
	public static final int MODE_ALL = 4;
	public static final int MODE_CLIPBOARD = 5;
	public static final int MODE_DUPLICATE = 6;	// this mode and beyond require selected columns as criteria
	public static final int MODE_UNIQUE = 7;
	public static final int MODE_DISTINCT = 8;

	private static final String PROPERTY_ID_COLUMN = "idColumn";
	private static final String PROPERTY_EXTENSION_COLUMN = "extensionColumn";
	private static final String PROPERTY_MODE = "mode";
	private static final String PROPERTY_HITLIST_NAME = "listName";
	private static final String PROPERTY_COLUMN_LIST = "columnList";
	private static final String PROPERTY_CASE_SENSITIVE = "caseSensitive";

	private static final String DEFAULT_LIST_NAME = "Unnamed Rowlist";

	private static final String[] MODE_TEXT = { "selected rows", "visible rows", "hidden rows", "no rows", "all rows", "IDs in clipboard", "duplicate rows", "unique rows", "distinct rows" };
	private static final String[] MODE_CODE = { "selected", "visible", "hidden", "empty", "all", "clipboard", "duplicate", "unique", "distinct" };

	private CompoundTableModel	mTableModel;
	private JPanel				mDialogPanel,mActiveOptionPanel,mClipboardPanel, mColumnSelectionPanel;
	private JTextField			mTextFieldHitlistName;
	private JCheckBox			mCheckBoxExtendList,mCheckBoxCaseSensitive;
	private JComboBox			mComboBoxExtentionColumn,mComboBoxMode,mComboBoxIDColumn;
	private int					mFixedMode,mSuggestedIDColumn;
	private JList				mListColumns;
	private JTextArea			mTextArea;

	/**
	 * The fixedMode parameter may be used to predefine the configuration's mode setting.
	 * If a fixedMode is given then the dialog will not show the mode selection combobox.
	 * @param parent
	 * @param fixedMode -1 or initial mode in dialog
	 */
	public DETaskNewRowList(DEFrame parent, int fixedMode) {
		super(parent, true);
		mTableModel = parent.getTableModel();
		mFixedMode = fixedMode;
		mSuggestedIDColumn = -1;
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mComboBoxMode) {
			mCheckBoxExtendList.setEnabled(mComboBoxMode.getSelectedIndex() != MODE_EMPTY && mComboBoxMode.getSelectedIndex() != MODE_ALL);
			mComboBoxExtentionColumn.setEnabled(mCheckBoxExtendList.isEnabled() && mCheckBoxExtendList.isSelected());

			int mode = mComboBoxMode.getSelectedIndex();

			mCheckBoxCaseSensitive.setEnabled(mode >= MODE_CLIPBOARD);

			JPanel optionPanel = (mode == MODE_CLIPBOARD) ? mClipboardPanel
					: (mode >= MODE_DUPLICATE) ? mColumnSelectionPanel : null;

			if (mActiveOptionPanel == optionPanel)
				return;

			if (mActiveOptionPanel != null)
				mDialogPanel.remove(mActiveOptionPanel);

			if (optionPanel != null)
				mDialogPanel.add(optionPanel, "1,5,3,5");

			mActiveOptionPanel = optionPanel;

			getDialog().pack();

			return;
			}

		if (e.getSource() == mCheckBoxExtendList) {
			mComboBoxExtentionColumn.setEnabled(mCheckBoxExtendList.isSelected());
			return;
			}
		}

	@Override
	public boolean isConfigurable() {
		if (mTableModel.getTotalRowCount() == 0) {
			showErrorMessage("You cannot create a row list without any data.");
			return false;
			}
		if (mTableModel.getUnusedRowFlagCount() == 0) {
			showErrorMessage("Cannot create a new row list, because the\n"
					+"maximum number of filters/lists is reached.");
			}
		return true;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/lists.html#Creation";
		}

	@Override
	public JComponent createDialogContent() {
		int gap = HiDPIHelper.scale(8);
		mDialogPanel = new JPanel();
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED, gap,
									TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap} };
		mDialogPanel.setLayout(new TableLayout(size));

		mDialogPanel.add(new JLabel("Row list name:"), "1,1");
		mTextFieldHitlistName = new JTextField(16);
		mDialogPanel.add(mTextFieldHitlistName, "3,1");

		if (mFixedMode == -1) {
			mDialogPanel.add(new JLabel("Create row list from:"), "1,3");
			mComboBoxMode = new JComboBox(MODE_TEXT);
			mComboBoxMode.addActionListener(this);
			mDialogPanel.add(mComboBoxMode, "3,3");

			mClipboardPanel = createClipboardPanel();
			mColumnSelectionPanel = createColumnSelectionPanel();
			}
		else if (mFixedMode == MODE_CLIPBOARD) {
			mClipboardPanel = createClipboardPanel();
			mDialogPanel.add(mClipboardPanel, "1,5,3,5");
			}
		else if (mFixedMode >= MODE_DUPLICATE) {
			mColumnSelectionPanel = createColumnSelectionPanel();
			mDialogPanel.add(mColumnSelectionPanel, "1,5,3,5");
			}

		if (mFixedMode == -1 || mFixedMode >= MODE_CLIPBOARD) {
			mCheckBoxCaseSensitive = new JCheckBox("Case sensitive");
			mDialogPanel.add(mCheckBoxCaseSensitive, "1,7");
			}

		mCheckBoxExtendList = new JCheckBox("Extend list to all rows of same category", false);
		mCheckBoxExtendList.addActionListener(this);
		mDialogPanel.add(mCheckBoxExtendList, "1,9,3,9");

		mDialogPanel.add(new JLabel("Category column:", JLabel.RIGHT), "1,11");
		ArrayList<String> categoryColumnList = new ArrayList<String>();
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.isColumnTypeCategory(column))
				categoryColumnList.add(mTableModel.getColumnTitle(column));
		mComboBoxExtentionColumn = new JComboBox(categoryColumnList.toArray(new String[0]));
		mComboBoxExtentionColumn.setEnabled(false);
		mComboBoxExtentionColumn.setEditable(mFixedMode == -1);
		mDialogPanel.add(mComboBoxExtentionColumn, "3,11");

		return mDialogPanel;
		}

	private JPanel createClipboardPanel() {
		double[][] size = { {TableLayout.PREFERRED, HiDPIHelper.scale(8), TableLayout.PREFERRED}, {TableLayout.PREFERRED}};
		JPanel p = new JPanel();
		p.setLayout(new TableLayout(size));
		p.add(new JLabel("Column containing IDs:"), "0,0");
		ArrayList<String> idColumnList = new ArrayList<String>();
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (qualifiesAsIDColumn(column))
				idColumnList.add(mTableModel.getColumnTitle(column));
		mComboBoxIDColumn = new JComboBox(idColumnList.toArray(new String[0]));
		mComboBoxIDColumn.setEditable(mFixedMode == -1);
		p.add(mComboBoxIDColumn, "2,0");
		return p;
		}

	private JPanel createColumnSelectionPanel() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {TableLayout.PREFERRED},
				{TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap, TableLayout.PREFERRED} };
		JPanel p = new JPanel();
		p.setLayout(new TableLayout(size));

		p.add(new JLabel("Select column(s) to consider for equivalence check!", SwingConstants.CENTER), "0,0");
		p.add(new JLabel("(use <CTRL> for multiple selections)", SwingConstants.CENTER), "0,2");

		ArrayList<String> columnNameList = new ArrayList<String>();
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
			String specialType = mTableModel.getColumnSpecialType(column);
			if (specialType == null
					|| specialType.equals(CompoundTableModel.cColumnTypeIDCode)
					|| specialType.equals(CompoundTableModel.cColumnTypeRXNCode))
				columnNameList.add(mTableModel.getColumnTitle(column));
			}
		JScrollPane scrollPane = null;
		if (isInteractive()) {
			mListColumns = new JList(columnNameList.toArray());
			scrollPane = new JScrollPane(mListColumns);
			}
		else {
			mTextArea = new JTextArea();
			scrollPane = new JScrollPane(mTextArea);
			}
		scrollPane.setPreferredSize(new Dimension(220,160));
		p.add(scrollPane, "0,4");

		return p;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_HITLIST_NAME, mTextFieldHitlistName.getText());
		int mode = (mFixedMode != -1) ? mFixedMode : mComboBoxMode.getSelectedIndex();
		configuration.setProperty(PROPERTY_MODE, MODE_CODE[mode]);
		if (mode == MODE_CLIPBOARD && mComboBoxIDColumn.getSelectedItem() != null)
			configuration.setProperty(PROPERTY_ID_COLUMN, mTableModel.getColumnTitleNoAlias(
					(String)mComboBoxIDColumn.getSelectedItem()));
		if (mCheckBoxExtendList.isSelected())
			configuration.setProperty(PROPERTY_EXTENSION_COLUMN, mTableModel.getColumnTitleNoAlias(
					(String)mComboBoxExtentionColumn.getSelectedItem()));

		if (mode >= MODE_CLIPBOARD)
			configuration.setProperty(PROPERTY_CASE_SENSITIVE, mCheckBoxCaseSensitive.isSelected() ? "true" : "false");

		if (mode >= MODE_DUPLICATE) {
			String columnNames = isInteractive() ?
					getSelectedColumnsFromList(mListColumns, mTableModel)
					: mTextArea.getText().replace('\n', '\t');
			if (columnNames != null && columnNames.length() != 0)
				configuration.setProperty(PROPERTY_COLUMN_LIST, columnNames);
			}

		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		int mode = findListIndex(configuration.getProperty(PROPERTY_MODE), MODE_CODE, MODE_SELECTED);
		if (mFixedMode == -1) {	// no fixed mode -> we edit a task as part of a sequence
			mComboBoxMode.setSelectedIndex(mode);
			mTextFieldHitlistName.setText(configuration.getProperty(PROPERTY_HITLIST_NAME, DEFAULT_LIST_NAME));
			}
		else {
			String listName = (mode != mFixedMode) ? MODE_TEXT[mFixedMode] : configuration.getProperty(PROPERTY_HITLIST_NAME, DEFAULT_LIST_NAME);
			mTextFieldHitlistName.setText(mTableModel.getListHandler().getUniqueName(listName));
			}

		String extensionColumn = configuration.getProperty(PROPERTY_EXTENSION_COLUMN, "");
		if (extensionColumn.length() == 0) {
			mCheckBoxExtendList.setSelected(false);
			mComboBoxExtentionColumn.setEnabled(false);
			}
		else {
			int column = mTableModel.findColumn(configuration.getProperty(PROPERTY_EXTENSION_COLUMN));
			if (column != -1 && mTableModel.isColumnTypeCategory(column)) {
				mCheckBoxExtendList.setSelected(true);
				mComboBoxExtentionColumn.setSelectedItem(mTableModel.getColumnTitle(column));
				mComboBoxExtentionColumn.setEnabled(true);

				}
			else if (mFixedMode == -1) {
				mCheckBoxExtendList.setSelected(true);
				mComboBoxExtentionColumn.setSelectedItem(extensionColumn);
				mComboBoxExtentionColumn.setEnabled(true);
				}
			}

		if (mFixedMode == -1 || mFixedMode >= MODE_CLIPBOARD)
			mCheckBoxCaseSensitive.setSelected("true".equals(configuration.getProperty(PROPERTY_CASE_SENSITIVE, "true")));

		if (mFixedMode == -1 || mFixedMode >= MODE_DUPLICATE) {
			String columnNames = configuration.getProperty(PROPERTY_COLUMN_LIST, "");
			if (isInteractive())
				selectColumnsInList(mListColumns, columnNames, mTableModel);
			else
				mTextArea.setText(columnNames.replace('\t', '\n'));
			}

		if (mComboBoxIDColumn != null) {
			String idColumn = configuration.getProperty(PROPERTY_ID_COLUMN, "");
			if (idColumn.length() != 0) {
				int column = mTableModel.findColumn(idColumn);
				if (column != -1 && qualifiesAsIDColumn(column))
					mComboBoxIDColumn.setSelectedItem(mTableModel.getColumnTitle(column));
				else if (mFixedMode == -1)
					mComboBoxIDColumn.setSelectedItem(idColumn);
				else
					selectSuggestedItemOfComboBoxIDColumn();
				}
			}
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mFixedMode == -1)
			mComboBoxMode.setSelectedIndex(MODE_SELECTED);
		mTextFieldHitlistName.setText((mFixedMode == -1) ? DEFAULT_LIST_NAME : MODE_TEXT[mFixedMode]);

		if (mFixedMode == -1 || mFixedMode >= MODE_DUPLICATE) {
			if (isInteractive())
				mListColumns.clearSelection();
			else
				mTextArea.setText("");
			}

		if (mCheckBoxCaseSensitive != null)
			mCheckBoxCaseSensitive.setSelected(true);

		if (mFixedMode == MODE_CLIPBOARD)
			selectSuggestedItemOfComboBoxIDColumn();
		else if (mFixedMode == -1 && mComboBoxIDColumn.getItemCount() != 0)
			mComboBoxIDColumn.setSelectedIndex(0);

		mCheckBoxExtendList.setSelected(false);
		mComboBoxExtentionColumn.setEnabled(false);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (configuration.getProperty(PROPERTY_HITLIST_NAME, "").length() == 0) {
			showErrorMessage("No list name specified.");
			return false;
			}

		int mode = findListIndex(configuration.getProperty(PROPERTY_MODE), MODE_CODE, -1);
		if (mode == -1) {
			showErrorMessage("No mode specified.");
			return false;
			}


		if (mode >= MODE_DUPLICATE) {
			String columnList = configuration.getProperty(PROPERTY_COLUMN_LIST);
			if (columnList == null) {
				showErrorMessage("Columns for uniqueness not defined.");
				return false;
				}

			if (isLive) {
				String[] columnName = columnList.split("\\t");
				int[] column = new int[columnName.length];
				for (int i=0; i<columnName.length; i++) {
					column[i] = mTableModel.findColumn(columnName[i]);
					if (column[i] == -1) {
						showErrorMessage("Column '"+columnName[i]+"' not found.");
						return false;
						}
					}
				for (int i=0; i<column.length; i++) {
					if (!columnQualifiesForUniqueness(column[i])) {
						showErrorMessage("Column '"+columnName[i]+"' cannot be used for a redundency check.");
						return false;
						}
					}
				}
			}

		if (isLive) {
			String columnName = configuration.getProperty(PROPERTY_EXTENSION_COLUMN);
			if (columnName != null) {
				if (mTableModel.findColumn(columnName) == -1) {
					showErrorMessage("Column '"+columnName+"' not found.");
					return false;
					}
				}

			if (mode == MODE_CLIPBOARD) {
				TreeSet<String> keySet = analyzeClipboard(true);
				if (keySet == null) {
					showErrorMessage("The clipboard is empty.");
					return false;
					}
				String idColumnName = configuration.getProperty(PROPERTY_ID_COLUMN);
				if (mTableModel.findColumn(idColumnName) == -1 && suggestIDColumn() == -1) {
					showErrorMessage("A column containing IDs was not defined nor found\n"
							+"and no column exists that matches any clipboard content.");
					return false;
					}
				}
			}
		
		return true;
		}

	private boolean columnQualifiesForUniqueness(int column) {
		String specialType = mTableModel.getColumnSpecialType(column);
		return (specialType == null
				|| specialType.equals(CompoundTableModel.cColumnTypeIDCode)
				|| specialType.equals(CompoundTableModel.cColumnTypeRXNCode));
		}

	private void selectSuggestedItemOfComboBoxIDColumn() {
		int column = suggestIDColumn();
		if (column != -1)
			mComboBoxIDColumn.setSelectedItem(mTableModel.getColumnTitle(column));
		}

	private boolean qualifiesAsIDColumn(int column) {
		return mTableModel.getColumnSpecialType(column) == null;
// no reason to exclude numerical columns, TLS 25-Apr-2019
//		return mTableModel.isColumnTypeString(column) && mTableModel.getColumnSpecialType(column) == null;
		}

	/**
	 * Finds the column that has the highest number of clipboard content matches.
	 * @return -1 or valid column index
	 */
	private int suggestIDColumn() {
		if (mSuggestedIDColumn == -1) {
			TreeSet<String> keySet = analyzeClipboard(true);
			if (keySet != null) {
				int maxMatchCount = 0;
				for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
					if (qualifiesAsIDColumn(column)) {
						int matchCount = 0;
						for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
							String[] entry = mTableModel.separateEntries(mTableModel.getTotalValueAt(row, column));
							for (int i=0; i<entry.length; i++) {
								if (entry[i].length() > 0 && keySet.contains(entry[i])) {
									matchCount++;
									break;
									}
								}
							}
						if (matchCount != 0) {
							if (maxMatchCount < matchCount) {
								maxMatchCount = matchCount;
								mSuggestedIDColumn = column;
								}
							}
						}
					}
				if (mSuggestedIDColumn == -1)
					mSuggestedIDColumn = -2;	// mark that analysis has been done
				}
			}
		return (mSuggestedIDColumn == -2) ? -1 : mSuggestedIDColumn;
		}

	/**
	 * Analyzes the clipboard content and sets mKeySet, mListName, mKeyColumn
	 * @return set of unique IDs found in clipboard or null, if clipboard is empty
	 */
	private TreeSet<String> analyzeClipboard(boolean caseSensitive) {
		TreeSet<String> keySet = null;

		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable theData = clipboard.getContents(this);
		String s;
		try {
			s = (String)(theData.getTransferData(DataFlavor.stringFlavor));
			BufferedReader theReader = new BufferedReader(new StringReader(s));

			keySet = new TreeSet<String>();
			String key = theReader.readLine();
			while (key != null) {
				if (!caseSensitive)
					key.toLowerCase();
				keySet.add(key);
				key = theReader.readLine();
				}

			theReader.close();
			return keySet.isEmpty() ? null : keySet;
			}
   		catch (Exception e) {
   			return null;
   			}
		}

	@Override
	public void runTask(Properties configuration) {
		int mode = findListIndex(configuration.getProperty(PROPERTY_MODE), MODE_CODE, MODE_SELECTED);
		int hitlistMode = (mode == MODE_SELECTED)	? CompoundTableListHandler.FROM_SELECTED
						: (mode == MODE_VISIBLE)	? CompoundTableListHandler.FROM_VISIBLE
						: (mode == MODE_HIDDEN)		? CompoundTableListHandler.FROM_HIDDEN
						: (mode == MODE_CLIPBOARD)	? CompoundTableListHandler.FROM_KEY_SET
						: (mode == MODE_DUPLICATE)	? CompoundTableListHandler.EMPTY_LIST
						: (mode == MODE_UNIQUE)		? CompoundTableListHandler.EMPTY_LIST
						: (mode == MODE_DISTINCT)	? CompoundTableListHandler.EMPTY_LIST
						: (mode == MODE_EMPTY)		? CompoundTableListHandler.EMPTY_LIST
						:							  CompoundTableListHandler.ALL_IN_LIST;
						
		String name = configuration.getProperty(PROPERTY_HITLIST_NAME);
		int extensionColumn = mTableModel.findColumn(configuration.getProperty(PROPERTY_EXTENSION_COLUMN));

		boolean caseSensitive = "true".equals(configuration.getProperty(PROPERTY_CASE_SENSITIVE, "true"));

		TreeSet<String> keySet = null;
		int keyColumn = -1;
		if (mode == MODE_CLIPBOARD) {
			keySet = analyzeClipboard(caseSensitive);
			keyColumn = mTableModel.findColumn(configuration.getProperty(PROPERTY_ID_COLUMN));
			if (keyColumn == -1)
				keyColumn = suggestIDColumn();
			}

		name = mTableModel.getListHandler().createList(name, extensionColumn, hitlistMode, keyColumn, keySet, !caseSensitive);
		if (name == null)
			showErrorMessage("The maximum number of filters/lists is reached.");
		else if (mode >= MODE_DUPLICATE)
			setHitlistFlags(configuration, mode, name, caseSensitive);
		}

	private void setHitlistFlags(Properties configuration, int mode, String listName, boolean caseSensitive) {
		String[] columnName = configuration.getProperty(PROPERTY_COLUMN_LIST).split("\\t");
		int[] columnList = new int[columnName.length];
		for (int i=0; i<columnList.length; i++)
			columnList[i] = mTableModel.findColumn(columnName[i]);

		CompoundRecord[] record = new CompoundRecord[mTableModel.getTotalRowCount()];
		for (int row=0; row<mTableModel.getTotalRowCount(); row++)
			record[row] = mTableModel.getTotalRecord(row);

		RedundancyComparator comparator = new RedundancyComparator(mTableModel, columnList, caseSensitive);

		Arrays.sort(record, comparator);

		CompoundTableListHandler listHandler = mTableModel.getListHandler();
		int hitlistFlagNo = listHandler.getListFlagNo(listName);

		if (mode == MODE_UNIQUE) {
			boolean isFirstInSet = true;
			for (int row=0; row<record.length; row++) {
				boolean isLastInSet = (row + 1 == record.length || comparator.compare(record[row], record[row + 1]) != 0);
				if (isFirstInSet && isLastInSet)
					listHandler.addRecordSilent(record[row], hitlistFlagNo);
				isFirstInSet = isLastInSet;
				}
			}
		else if (mode == MODE_DUPLICATE) {
			for (int row=1; row<record.length; row++)
				if (comparator.compare(record[row - 1], record[row]) == 0)
					listHandler.addRecordSilent(record[row], hitlistFlagNo);
			}
		else if (mode == MODE_DISTINCT) {
			listHandler.addRecordSilent(record[0], hitlistFlagNo);
			for (int row=1; row<record.length; row++)
				if (comparator.compare(record[row - 1], record[row]) != 0)
					listHandler.addRecordSilent(record[row], hitlistFlagNo);
			}
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}

class RedundancyComparator implements Comparator<CompoundRecord> {
	private CompoundTableModel mTableModel;
	private int[] mColumnList;
	private boolean[] mIsCaseSensitive;

	public RedundancyComparator(CompoundTableModel tableModel, int[] columnList, boolean caseSensitive) {
		mTableModel = tableModel;
		mColumnList = columnList;
		mIsCaseSensitive = new boolean[columnList.length];
		for (int i=0; i<columnList.length; i++)
			mIsCaseSensitive[i] = caseSensitive || mTableModel.getColumnSpecialType(columnList[i]) != null;
	}

	public int compare(CompoundRecord o1, CompoundRecord o2) {
		int comparison = 0;
		for (int i=0; i<mColumnList.length; i++) {
			String s1 = mTableModel.getValue(o1, mColumnList[i]);
			String s2 = mTableModel.getValue(o2, mColumnList[i]);
			if (!mIsCaseSensitive[i]) {
				if (s1 != null)
					s1 = s1.toLowerCase();
				if (s2 != null)
					s2 = s2.toLowerCase();
				}
			comparison = (s1 == null) ? ((s2 == null) ? 0 : 1)
					: (s2 == null) ? -1
					: s1.compareTo(s2);
			if (comparison != 0)
				return comparison;
		}
		return comparison;
	}
}
