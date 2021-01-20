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

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.ArrayUtils;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

public abstract class AbstractMultiColumnTask extends ConfigurableTask implements ActionListener {
	private static final String PROPERTY_COLUMN_LIST = "columnList";
	private static final String PROPERTY_MODE = "columnDefMode";
	private static final String PROPERTY_CRITERION = "defCriterion";

	private static final String[] MODE_OPTIONS = {"contains", "starts with", "matches regex", "is one of these"};
	private static final String[] MODE_CODE = {"contains", "startsWith", "regex", "list" };
	private static final int MODE_CONTAINS = 0;
	private static final int MODE_STARTS_WITH = 1;
	private static final int MODE_REGEX = 2;
	private static final int MODE_LIST = 3;

	private CompoundTableModel	mTableModel;
	private JComboBox           mComboBoxMode;
	private JList				mListColumns;
	private JTextField          mTextFieldCriterion;
	private JTextArea			mTextArea;
	private JLabel              mLabelMessage;
	private int[]				mColumnList;

	public AbstractMultiColumnTask(Frame owner, CompoundTableModel tableModel, boolean useOwnThread) {
		super(owner, useOwnThread);
		mTableModel = tableModel;
		mColumnList = null;	// if interactive, then show dialog
		}

    /**
     * Instantiates this task interactively with a pre-defined configuration.
     * @param owner
     * @param tableModel
     * @param useOwnThread
     * @param columnList
     */
	public AbstractMultiColumnTask(Frame owner, CompoundTableModel tableModel, boolean useOwnThread, int[] columnList) {
		super(owner, useOwnThread);
		mTableModel = tableModel;
		mColumnList = columnList;
		}

	public CompoundTableModel getTableModel() {
		return mTableModel;
		}

	public int[] getColumnList() {
		return mColumnList;
	}

	/**
	 * @param column total column index
	 * @return true if the column should appear in list for selection or shall be matched with condition
	 */
	public abstract boolean isCompatibleColumn(int column);

	@Override
	public Properties getPredefinedConfiguration() {
		return mColumnList == null ? null : createConfigurationFromColumnList();
		}

	public Properties createConfigurationFromColumnList() {
		Properties configuration = new Properties();
		StringBuilder sb = new StringBuilder(mTableModel.getColumnTitleNoAlias(mColumnList[0]));
		for (int i=1; i<mColumnList.length; i++)
			sb.append("\t").append(mTableModel.getColumnTitleNoAlias(mColumnList[i]));
		configuration.setProperty(PROPERTY_COLUMN_LIST, sb.toString());
		configuration.setProperty(PROPERTY_MODE, MODE_CODE[MODE_LIST]);
		return configuration;
		}

	@Override
	public JPanel createDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.FILL, gap},
							{TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap/2, TableLayout.FILL, gap} };
		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		JPanel innerPanel = createInnerDialogContent();
		if (innerPanel != null)
			content.add(innerPanel, "0,0,6,0");

		content.add(new JLabel("Column name"), "1,2");
		mComboBoxMode = new JComboBox(MODE_OPTIONS);
		mComboBoxMode.setSelectedIndex(MODE_LIST);
		mComboBoxMode.addActionListener(this);
		content.add(mComboBoxMode, "3,2");

		mTextFieldCriterion = new JTextField(8);
		content.add(mTextFieldCriterion, "5,2");

		JScrollPane scrollPane;

		if (isInteractive()) {
			mLabelMessage = new JLabel("Select one or multiple column names!");
			ArrayList<String> columnList = new ArrayList<String>();
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
				if (isCompatibleColumn(column))
					columnList.add(mTableModel.getColumnTitle(column));
			String[] itemList = columnList.toArray(new String[0]);
			Arrays.sort(itemList, (s1, s2) -> s1.compareToIgnoreCase(s2) );
			mListColumns = new JList(itemList);
			scrollPane = new JScrollPane(mListColumns);
	//		scrollPane.setPreferredSize(new Dimension(240,240));	de-facto limits width when long column names need more space
			}
		else {
			int wh = HiDPIHelper.scale(240);
			mLabelMessage = new JLabel("Type one or multiple column names!");
			mTextArea = new JTextArea();
			scrollPane = new JScrollPane(mTextArea);
			scrollPane.setPreferredSize(new Dimension(wh,wh));
			}

		content.add(mLabelMessage, "1,4,5,4");
		content.add(scrollPane, "1,6,5,6");

		return content;
		}

	/**
	 * Override this if your subclass needs more properties to be defined.
	 * This panel is expected to have some whitespace at its bottom.
	 * @return
	 */
	public JPanel createInnerDialogContent() {
		return null;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mComboBoxMode) {
			enableItems();
			return;
			}
		}

	private void enableItems() {
		boolean isListMode = (mComboBoxMode.getSelectedIndex() == MODE_LIST);
		mTextFieldCriterion.setEnabled(!isListMode);
		mLabelMessage.setEnabled(isListMode);
		if (mTextArea != null)
			mTextArea.setEnabled(isListMode);
		if (mListColumns != null)
			mListColumns.setEnabled(isListMode);
		}

	@Override
	public boolean isConfigurable() {
		for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
			if (isCompatibleColumn(i))
				return true;

		showErrorMessage("No compatible columns found.");
		return false;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_MODE, MODE_CODE[mComboBoxMode.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_CRITERION, mTextFieldCriterion.getText());
		String columnNames = isInteractive() ?
				  getSelectedColumnsFromList(mListColumns, mTableModel)
				: mTextArea.getText().replace('\n', '\t');
		if (columnNames != null && columnNames.length() != 0)
			configuration.setProperty(PROPERTY_COLUMN_LIST, columnNames);
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mComboBoxMode.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_MODE), MODE_CODE, MODE_LIST));
		mTextFieldCriterion.setText(configuration.getProperty(PROPERTY_CRITERION, ""));
		String columnNames = configuration.getProperty(PROPERTY_COLUMN_LIST, "");
		if (isInteractive())
			selectColumnsInList(mListColumns, columnNames, mTableModel);
		else
			mTextArea.setText(columnNames.replace('\t', '\n'));
		enableItems();
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mTextFieldCriterion.setText("");
		if (isInteractive()) {
			mComboBoxMode.setSelectedIndex(MODE_LIST);
			mListColumns.clearSelection();
			}
		else {
			mComboBoxMode.setSelectedIndex(MODE_CONTAINS);
			mTextArea.setText("");
			}
		enableItems();
		}

/*	@Override
	public boolean isRedundant(Properties previousConfiguration, Properties currentConfiguration) {
		int mode1 = findListIndex(previousConfiguration.getProperty(PROPERTY_MODE), MODE_CODE, MODE_LIST);
		int mode2 = findListIndex(currentConfiguration.getProperty(PROPERTY_MODE), MODE_CODE, MODE_LIST);
		if (mode1 != mode2)
			return false;

		if (mode1 == MODE_LIST) {
			String list1 = previousConfiguration.getProperty(PROPERTY_COLUMN_LIST, "");
			String list2 = currentConfiguration.getProperty(PROPERTY_COLUMN_LIST, "");
			return list1.equals(list2);
			}

		String crit1 = previousConfiguration.getProperty(PROPERTY_CRITERION, "");
		String crit2 = currentConfiguration.getProperty(PROPERTY_CRITERION, "");
		return crit1.equals(crit2);
		}*/

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		int mode = findListIndex(configuration.getProperty(PROPERTY_MODE), MODE_CODE, MODE_LIST);
		if (mode == MODE_LIST) {
			String columnList = configuration.getProperty(PROPERTY_COLUMN_LIST);
			if (columnList == null) {
				showErrorMessage("No columns defined.");
				return false;
				}

			if (isLive) {
				String[] columnName = columnList.split("\\t");
				for (int i = 0; i < columnName.length; i++) {
					int column = mTableModel.findColumn(columnName[i]);
					if (column == -1) {
						showErrorMessage("Column '" + columnName[i] + "' not found.");
						return false;
						}
					}
				}
			}
		else {
			String criterion = configuration.getProperty(PROPERTY_CRITERION);
			if (criterion == null) {
				showErrorMessage("No column name criterion defined.");
				return false;
				}

			if (isLive) {
				int[] column = compileColumnList(mode, criterion);
				if (column == null) {
					showErrorMessage("No column found that "+MODE_OPTIONS[mode]+" '"+criterion+"'.");
					return false;
					}
				}
			}

		return true;
		}

	private int[] compileColumnList(int mode, String criterion) {
		int count = 0;
		int[] column = new int[mTableModel.getTotalColumnCount()];
		for (int i=0; i<mTableModel.getTotalColumnCount(); i++) {
			if (isCompatibleColumn(i)) {
				boolean isMatch = false;
				switch (mode) {
					case MODE_CONTAINS:
						if (mTableModel.getColumnTitle(i).contains(criterion))
							isMatch = true;
						break;
					case MODE_STARTS_WITH:
						if (mTableModel.getColumnTitle(i).startsWith(criterion))
							isMatch = true;
						break;
					case MODE_REGEX:
						if (mTableModel.getColumnTitle(i).matches(criterion))
							isMatch = true;
						break;
					}

				if (isMatch) {
					column[count++] = i;
					}
				}
			}

		return (count == 0) ? null : (count == column.length) ? column : (int[])ArrayUtils.resize(column, count);
		}

	public int[] getColumnList(Properties configuration) {
		int mode = findListIndex(configuration.getProperty(PROPERTY_MODE), MODE_CODE, MODE_LIST);
		if (mode == MODE_LIST) {
			String[] columnName = configuration.getProperty(PROPERTY_COLUMN_LIST).split("\\t");
			int count = 0;
			int[] column = new int[columnName.length];
			for (int i = 0; i < columnName.length; i++) {
				column[count] = getTableModel().findColumn(columnName[i]);
				if (column[count] != -1 && isCompatibleColumn(column[count]))
					count++;
				}
			return (count == column.length) ? column : (int[]) ArrayUtils.resize(column, count);
			}

		return compileColumnList(mode, configuration.getProperty(PROPERTY_CRITERION));
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
