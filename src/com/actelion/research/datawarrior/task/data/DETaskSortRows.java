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

package com.actelion.research.datawarrior.task.data;

import info.clearthought.layout.TableLayout;

import java.awt.Frame;
import java.util.Properties;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.table.model.CompoundTableModel;

public class DETaskSortRows extends ConfigurableTask {
	public static final String TASK_NAME = "Sort Rows";

	private static final String PROPERTY_COLUMN = "column";
	private static final String PROPERTY_DESCENDING = "descending";
	private static final String PROPERTY_SELECTED_FIRST = "selectedFirst";

    private JComboBox			mComboBoxColumn;
    private JRadioButton		mRadioButton,mRadioButtonSelectedFirst;
    private CompoundTableModel	mTableModel;
	private int					mDefaultColumn;
	private boolean				mSelectedFirst;

	public DETaskSortRows(Frame parent, CompoundTableModel	tableModel, int defaultColumn, boolean selectedFirst) {
		super(parent, false);
		mTableModel = tableModel;
		mDefaultColumn = defaultColumn;
		mSelectedFirst = selectedFirst;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isRedundant(Properties previousConfiguration, Properties currentConfiguration) {
		return currentConfiguration.getProperty(PROPERTY_COLUMN).equals(previousConfiguration.getProperty(PROPERTY_COLUMN));
		}

	@Override
	public Properties getPredefinedConfiguration() {
		if (mDefaultColumn == -1)
			return null;

		Properties configuration = new Properties();
		configuration.put(PROPERTY_COLUMN, mTableModel.getColumnTitleNoAlias(mDefaultColumn));
		configuration.put(PROPERTY_DESCENDING, (mTableModel.getLastSortColumn() == mDefaultColumn) ? "true" : "false");
		configuration.put(PROPERTY_SELECTED_FIRST, mSelectedFirst ? "true" : "false");
		return configuration;
		}

	@Override
	public JComponent createDialogContent() {
		JPanel p = new JPanel();
        double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
                            {8, TableLayout.PREFERRED, 16, TableLayout.PREFERRED, TableLayout.PREFERRED, 8} };
        p.setLayout(new TableLayout(size));

		mComboBoxColumn = new JComboBox();
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.isColumnDisplayable(column))
				mComboBoxColumn.addItem(mTableModel.getColumnTitle(column));
		mComboBoxColumn.setEditable(true);
		p.add(new JLabel("Column:"), "1,1");
		p.add(mComboBoxColumn, "3,1");

		mRadioButton = new JRadioButton("Descending order");
		p.add(mRadioButton, "1,3,3,3");

		mRadioButtonSelectedFirst = new JRadioButton("Move selected rows to top.");
		p.add(mRadioButtonSelectedFirst, "1,4,3,4");

        return p;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.put(PROPERTY_COLUMN, mTableModel.getColumnTitleNoAlias((String)mComboBoxColumn.getSelectedItem()));
		configuration.put(PROPERTY_DESCENDING, mRadioButton.isSelected() ? "true" : "false");
		configuration.put(PROPERTY_SELECTED_FIRST, mRadioButtonSelectedFirst.isSelected() ? "true" : "false");
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String columnName = configuration.getProperty(PROPERTY_COLUMN);
		int column = mTableModel.findColumn(columnName);
		if (column != -1)
			mComboBoxColumn.setSelectedItem(mTableModel.getColumnTitle(column));
		else
			mComboBoxColumn.setSelectedItem(columnName);

		mRadioButton.setSelected("true".equals(configuration.getProperty(PROPERTY_DESCENDING)));
		mRadioButtonSelectedFirst.setSelected("true".equals(configuration.getProperty(PROPERTY_SELECTED_FIRST)));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mComboBoxColumn.getItemCount() != 0)
			mComboBoxColumn.setSelectedIndex(0);

		mRadioButton.setSelected(false);
		mRadioButtonSelectedFirst.setSelected(false);
		}

	@Override
	public boolean isConfigurable() {
		return mTableModel.getColumnCount() != 0;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (isLive) {
			int column = mTableModel.findColumn(configuration.getProperty(PROPERTY_COLUMN, ""));
			if (column == -1 || !mTableModel.isColumnDisplayable(column)) {
				showErrorMessage("Column '"+configuration.getProperty(PROPERTY_COLUMN)+"' not found.");
		        return false;
				}
			}
		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		int column = mTableModel.findColumn(configuration.getProperty(PROPERTY_COLUMN, ""));
		boolean descending = "true".equals(configuration.getProperty(PROPERTY_DESCENDING));
		boolean selectedFirst = "true".equals(configuration.getProperty(PROPERTY_SELECTED_FIRST));
		mTableModel.sort(column, descending, selectedFirst);
		}
	
	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
