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

package com.actelion.research.datawarrior.task.filter;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEPruningPanel;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.filter.JFilterPanel;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2013
 * Company:
 * @author
 * @version 1.0
 */

public class DETaskCloseFilter extends ConfigurableTask implements ActionListener {
	public static final String TASK_NAME = "Close Filter";

	private static final String PROPERTY_TYPE = "type";
	private static final String PROPERTY_COLUMN = "column";
	private static final String PROPERTY_DUPLICATE = "duplicate";
    
	private CompoundTableModel  mTableModel;
	private DEPruningPanel      mPruningPanel;
	private JComboBox			mComboBoxType;
	private JTextField			mTextFieldColumn,mTextFieldIndex;
	private JFilterPanel		mFilter;

    /**
     * Instantiates this task interactively with a pre-defined configuration.
     * @param parent
     * @param pruningPanel
     * @param filter null, if not interactive
     */
    public DETaskCloseFilter(Frame parent, DEPruningPanel pruningPanel, JFilterPanel filter) {
		super(parent, false);
    	
		mPruningPanel = pruningPanel;
		mTableModel = pruningPanel.getTableModel();
		mFilter = filter;
    	}

	@Override
	public Properties getPredefinedConfiguration() {
		if (mFilter == null)
			return null;

		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_TYPE, DETaskAddNewFilter.FILTER_CODE[mFilter.getFilterType()]);
		if (DETaskAddNewFilter.FILTER_NEEDS_COLUMN[mFilter.getFilterType()])
			configuration.setProperty(PROPERTY_COLUMN, mTableModel.getColumnTitleNoAlias(mFilter.getColumnIndex()));
		configuration.setProperty(PROPERTY_DUPLICATE, Integer.toString(1+mPruningPanel.getFilterDuplicateIndex(mFilter, mFilter.getColumnIndex())));
		return configuration;
		}

	@Override
	public boolean isConfigurable() {
		return mPruningPanel.getFilterCount() != 0;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public JComponent createDialogContent() {
		mComboBoxType = new JComboBox(DETaskAddNewFilter.FILTER_NAME);
		mComboBoxType.addActionListener(this);
		mTextFieldColumn = new JTextField();
		mTextFieldIndex = new JTextField();

		JPanel content = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };
		content.setLayout(new TableLayout(size));

		content.add(new JLabel("Filter type:"), "1,1");
		content.add(mComboBoxType, "3,1,5,1");

		content.add(new JLabel("Column:"), "1,3");
		content.add(mTextFieldColumn, "3,3,5,3");

		content.add(new JLabel("Duplicate Filter No:"), "1,5");
        mTextFieldIndex = new JTextField(1);
        content.add(mTextFieldIndex, "3,5");
        content.add(new JLabel("(usually '1')"), "5,5");

		return content;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		enableTextField();
		}

	private void enableTextField() {
		int type = findListIndex((String)mComboBoxType.getSelectedItem(), DETaskAddNewFilter.FILTER_NAME, -1);
		mTextFieldColumn.setEnabled(DETaskAddNewFilter.FILTER_NEEDS_COLUMN[type]);
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		int type = findListIndex((String)mComboBoxType.getSelectedItem(), DETaskAddNewFilter.FILTER_NAME, -1);
		configuration.setProperty(PROPERTY_TYPE, DETaskAddNewFilter.FILTER_CODE[type]);

		if (DETaskAddNewFilter.FILTER_NEEDS_COLUMN[type])
			configuration.setProperty(PROPERTY_COLUMN, mTableModel.getColumnTitleNoAlias(mTextFieldColumn.getText()));

		configuration.setProperty(PROPERTY_DUPLICATE, mTextFieldIndex.getText());

		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		int type = findListIndex(configuration.getProperty(PROPERTY_TYPE), DETaskAddNewFilter.FILTER_CODE,0);
		mComboBoxType.setSelectedIndex(type);
		mTextFieldColumn.setText(configuration.getProperty(PROPERTY_COLUMN, ""));
		mTextFieldIndex.setText(configuration.getProperty(PROPERTY_DUPLICATE, "1"));
		enableTextField();
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mComboBoxType.setSelectedIndex(JFilterPanel.FILTER_TYPE_CATEGORY_BROWSER);
		mTextFieldColumn.setText("");
		mTextFieldIndex.setText("1");
		enableTextField();
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		int type = findListIndex(configuration.getProperty(PROPERTY_TYPE), DETaskAddNewFilter.FILTER_CODE, -1);

		int column = -1;
		if (DETaskAddNewFilter.FILTER_NEEDS_COLUMN[type]) {
			String columnName = configuration.getProperty(PROPERTY_COLUMN, "");
			if (columnName.length() == 0) {
				showErrorMessage("Column name missing. "+DETaskAddNewFilter.FILTER_NAME[type]+" filters  require a defined associated column.");
				return false;
				}
			if (isLive) {
				column = mTableModel.findColumn(columnName);
				if (column == -1) {
					showErrorMessage("Column '"+columnName+"' not found.");
					return false;
					}
				}
			}

		try {
			int index = Integer.parseInt(configuration.getProperty(PROPERTY_DUPLICATE, "1")) - 1;
			if (index < 0 || index > 31) {
				showErrorMessage("Duplicate filter number must be a small positive integer.");
				return false;
				}
			if (isLive) {
				JFilterPanel filter = mPruningPanel.getFilter(type, column, index);
				if (filter == null) {
					showErrorMessage("Filter not found.");
					return false;
					}
				}
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("Duplicate filter number is not numerical");
			return false;
			}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		int type = findListIndex(configuration.getProperty(PROPERTY_TYPE), DETaskAddNewFilter.FILTER_CODE, -1);
		int column = DETaskAddNewFilter.FILTER_NEEDS_COLUMN[type] ? mTableModel.findColumn(configuration.getProperty(PROPERTY_COLUMN)) : -1;
		int duplicate = Integer.parseInt(configuration.getProperty(PROPERTY_DUPLICATE, "1")) - 1;
		mPruningPanel.getFilter(type, column, duplicate).removePanel();
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
