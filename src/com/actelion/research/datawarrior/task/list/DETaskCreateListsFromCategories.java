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

import com.actelion.research.table.model.CompoundTableListHandler;
import info.clearthought.layout.TableLayout;

import java.util.Properties;
import java.util.TreeMap;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;

public class DETaskCreateListsFromCategories extends ConfigurableTask {
	public static final String TASK_NAME = "Create Row Lists From Category Column";

	private static final String DEFAULT_PREFIX = "";
	private static final String DEFAULT_POSTFIX = "";

	private static final String PROPERTY_COLUMN = "column";
	private static final String PROPERTY_PREFIX = "prefix";
	private static final String PROPERTY_POSTFIX = "postfix";

	private CompoundTableModel	mTableModel;
	private JComboBox			mComboBoxColumn;
    private JTextField          mTextFieldPrefix,mTextFieldPostfix;

    public DETaskCreateListsFromCategories(DEFrame parent) {
        super(parent, true);
        mTableModel = parent.getTableModel();
    	}

	@Override
	public JComponent createDialogContent() {
		JPanel mp = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8} };
		mp.setLayout(new TableLayout(size));

		mp.add(new JLabel("Category column:"), "1,1");
		mComboBoxColumn = new JComboBox();
		for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
			if (columnQualifies(i))
				mComboBoxColumn.addItem(mTableModel.getColumnTitle(i));
		mComboBoxColumn.setEditable(!isInteractive());
		mp.add(mComboBoxColumn, "3,1");

		mp.add(new JLabel("Row list name prefix:"), "1,3");
        mTextFieldPrefix = new JTextField(12);
        mp.add(mTextFieldPrefix, "3,3");

        mp.add(new JLabel("Row list name postfix:"), "1,5");
        mTextFieldPostfix = new JTextField(12);
        mp.add(mTextFieldPostfix, "3,5");

        return mp;
        }

	private boolean columnQualifies(int column) {
		return mTableModel.isColumnTypeCategory(column)
			&& mTableModel.getCategoryCount(column) <= 60;
		}

	@Override
	public boolean isConfigurable() {
		for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
			if (columnQualifies(i))
				return true;

		showErrorMessage("No category column found.");
		return false;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/lists.html#Columns";
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		configuration.setProperty(PROPERTY_COLUMN, mTableModel.getColumnTitleNoAlias((String)mComboBoxColumn.getSelectedItem()));

		String prefix = mTextFieldPrefix.getText();
		if (prefix.length() != 0 && !prefix.equals(DEFAULT_PREFIX))
			configuration.put(PROPERTY_PREFIX, mTextFieldPrefix.getText());

		String postfix = mTextFieldPostfix.getText();
		if (postfix.length() != 0 && !postfix.equals(DEFAULT_POSTFIX))
			configuration.put(PROPERTY_POSTFIX, mTextFieldPostfix.getText());

		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mComboBoxColumn.setSelectedItem(configuration.getProperty(PROPERTY_COLUMN, ""));
		mTextFieldPrefix.setText(configuration.getProperty(PROPERTY_PREFIX, DEFAULT_PREFIX));
		mTextFieldPostfix.setText(configuration.getProperty(PROPERTY_POSTFIX, DEFAULT_POSTFIX));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mComboBoxColumn.getItemCount() != 0)
			mComboBoxColumn.setSelectedIndex(0);
		mTextFieldPrefix.setText(DEFAULT_PREFIX);
		mTextFieldPostfix.setText(DEFAULT_POSTFIX);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (isLive) {
			String columnName = configuration.getProperty(PROPERTY_COLUMN, "");
			int column = mTableModel.findColumn(columnName);
			if (column == -1) {
				showErrorMessage("Column '"+columnName+"' not found.");
				return false;
				}
			if (!columnQualifies(column)) {
				showErrorMessage("Column '"+columnName+"' contains too many or no categories.");
				return false;
				}
			if (mTableModel.getUnusedRowFlagCount() < mTableModel.getCategoryCount(column)) {
				showErrorMessage("Column '"+columnName+"' contains more categories than available row flags.\nClose filters or delete row lists to free row flags.");
				return false;
				}
			}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		int column = mTableModel.findColumn(configuration.getProperty(PROPERTY_COLUMN, ""));
		String prefix = configuration.getProperty(PROPERTY_PREFIX, DEFAULT_PREFIX);
		String postfix = configuration.getProperty(PROPERTY_POSTFIX, DEFAULT_POSTFIX);

		CompoundTableListHandler hh = mTableModel.getListHandler();
		String[] category = mTableModel.getCategoryList(column);
		int categoryCount = mTableModel.isMultiCategoryColumn(column) ? category.length-1 : category.length;

		TreeMap<String,Long> map = new TreeMap<String,Long>();
		for (int i=0; i<categoryCount; i++) {
			String name = prefix+(category[i].length() == 0 ? "<empty>" : category[i])+postfix;
			name = hh.createList(name, -1, CompoundTableListHandler.EMPTY_LIST, -1, null, false);
			map.put(category[i], hh.getListMask(hh.getListIndex(name)));
			}

        startProgress("Populating row lists...", 0, mTableModel.getTotalRowCount());

		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
            if (threadMustDie())
                break;

            if ((row % 16) == 15)
            	updateProgress(row);

            CompoundRecord record = mTableModel.getTotalRecord(row);
			if (record.getData(column) != null) {
				String[] entries = mTableModel.separateEntries(mTableModel.getTotalValueAt(row, column));
				for (String entry:entries)
					record.setFlags(map.get(mTableModel.normalizeCategoryEntry(entry, column)));
				}
			}
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
    }
