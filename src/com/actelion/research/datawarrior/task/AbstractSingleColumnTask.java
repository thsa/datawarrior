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
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Properties;

public abstract class AbstractSingleColumnTask extends ConfigurableTask implements ItemListener {
	private static final String PROPERTY_COLUMN = "column";
	private static final String TEXT_NO_COLUMN = "<none>";
	private static final String CODE_NO_COLUMN = "_none_";
	public static final int NO_COLUMN = -2;

	private CompoundTableModel	mTableModel;
	private JComboBox           mComboBoxColumn;
	private int				    mColumn;

	public AbstractSingleColumnTask(Frame owner, CompoundTableModel tableModel, boolean useOwnThread) {
		super(owner, useOwnThread);
		mTableModel = tableModel;
		mColumn = -1;	// if interactive, then show dialog
		}

	/**
	 * Instantiates this task interactively with a pre-defined configuration.
	 * @param owner
	 * @param tableModel
	 * @param useOwnThread
	 * @param column use NO_COLUMN for valid preselection of none of the columns
	 */
	public AbstractSingleColumnTask(Frame owner, CompoundTableModel tableModel, boolean useOwnThread, int column) {
		super(owner, useOwnThread);
		mTableModel = tableModel;
		mColumn = column;
		}

	public CompoundTableModel getTableModel() {
		return mTableModel;
	}

	/**
	 * @param column total column index
	 * @return true if the column should appear in list for selection or shall be matched with condition
	 */
	public abstract boolean isCompatibleColumn(int column);

	public int getPredefinedColumn() {
		return mColumn;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		if (mColumn == -1)
			return null;

		Properties configuration = new Properties();
		if (mColumn == NO_COLUMN)
			configuration.setProperty(PROPERTY_COLUMN, CODE_NO_COLUMN);
		else
			configuration.setProperty(PROPERTY_COLUMN, mTableModel.getColumnTitleNoAlias(mColumn));
		return configuration;
		}

	@Override
	public JPanel createDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, TableLayout.FILL, gap},
				{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };
		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		content.add(new JLabel(getColumnLabelText()), "1,1");
		mComboBoxColumn = new JComboBox();
		mComboBoxColumn.addItemListener(this);
		if (allowColumnNoneItem())
			mComboBoxColumn.addItem(TEXT_NO_COLUMN);
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (isCompatibleColumn(column))
				mComboBoxColumn.addItem(mTableModel.getColumnTitle(column));
		mComboBoxColumn.setEditable(!isInteractive());
		if (mColumn != -1)
			mComboBoxColumn.setEnabled(false);	// in case getPredefinedConfiguration() is overriden and dialog is shown with defined column
		content.add(mComboBoxColumn, "3,1");

		JPanel innerPanel = createInnerDialogContent();
		if (innerPanel != null)
			content.add(innerPanel, "1,3,4,3");

		return content;
		}

	/**
	 * Override this if you allow selecting none of the columns.
	 * @return
	 */
	public boolean allowColumnNoneItem() {
		return false;
		}

	public String getColumnLabelText() {
		return "Column Name:";
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
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == mComboBoxColumn && e.getStateChange() == ItemEvent.SELECTED)
			columnChanged(getSelectedColumn());
		}

	public int getSelectedColumn() {
		return getTableModel().findColumn((String)mComboBoxColumn.getSelectedItem());
		}

	public String getSelectedColumnName() {
		return (String)mComboBoxColumn.getSelectedItem();
		}

	/**
	 * Override this if you need to update dialog items, when the column popup state changes
	 */
	public void columnChanged(int column) {}

	@Override
	public boolean isConfigurable() {
		if (allowColumnNoneItem())
			return true;

		for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
			if (isCompatibleColumn(i))
				return true;

		showErrorMessage("No compatible columns found.");
		return false;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		String item = (String)mComboBoxColumn.getSelectedItem();
		if (item == null || item.length() == 0)
			item = TEXT_NO_COLUMN;

		if (TEXT_NO_COLUMN.equals(item))
			configuration.setProperty(PROPERTY_COLUMN, CODE_NO_COLUMN);
		else
			configuration.setProperty(PROPERTY_COLUMN, mTableModel.getColumnTitleNoAlias(item));
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String name = configuration.getProperty(PROPERTY_COLUMN);
		int column = (mColumn != -1) ? mColumn : CODE_NO_COLUMN.equals(name) ? NO_COLUMN : mTableModel.findColumn(name);
		mComboBoxColumn.setSelectedItem(column == -1 ? configuration.getProperty(PROPERTY_COLUMN)
					: (column == NO_COLUMN) ? TEXT_NO_COLUMN : mTableModel.getColumnTitle(column));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mColumn != -1)
			mComboBoxColumn.setSelectedItem(mColumn == NO_COLUMN ? TEXT_NO_COLUMN : mTableModel.getColumnTitle(mColumn));
		else
			selectDefaultColumn(mComboBoxColumn);
		}

	/**
	 * If there is no preselected column then this function is called and selects the first compatible column.
	 * Override this for different handling.
	 */
	public void selectDefaultColumn(JComboBox comboBox) {
		if (allowColumnNoneItem()) {
			comboBox.setSelectedItem(TEXT_NO_COLUMN);
			}
		else {
			for (int i=0; i<mTableModel.getTotalColumnCount(); i++) {
				if (isCompatibleColumn(i)) {
					comboBox.setSelectedItem(mTableModel.getColumnTitle(i));
					break;
					}
				}
			}
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String columnName = configuration.getProperty(PROPERTY_COLUMN, CODE_NO_COLUMN);
		if (CODE_NO_COLUMN.equals(columnName) && !allowColumnNoneItem()) {
			showErrorMessage("No column defined.");
			return false;
			}
		if (isLive) {
			if (!CODE_NO_COLUMN.equals(columnName)) {
				int column = mTableModel.findColumn(columnName);
				if (column == -1) {
					showErrorMessage("Column '" + columnName + "' not found.");
					return false;
					}
				if (!isCompatibleColumn(column)) {
					showErrorMessage("Column '" + columnName + "' is not compatible.");
					return false;
					}
				}
			}
		return true;
		}

	public int getColumn(Properties configuration) {
		String name = configuration.getProperty(PROPERTY_COLUMN);

		if (CODE_NO_COLUMN.equals(name))
			return NO_COLUMN;

		return mTableModel.findColumn(name);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}

