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

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;

public class DETaskNewColumnWithListNames extends ConfigurableTask {
	public static final String TASK_NAME = "New Column With List Names";

	private static final String DEFAULT_COLUMN_NAME = "List Membership";

	private static final String PROPERTY_COLUMN_NAME = "columnName";

	private CompoundTableModel	mTableModel;
    private JTextField          mTextFieldColumnName;

    public DETaskNewColumnWithListNames(DEFrame parent) {
        super(parent, true);
        mTableModel = parent.getTableModel();
    	}

	@Override
	public JComponent createDialogContent() {
		JPanel mp = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8} };
		mp.setLayout(new TableLayout(size));
        mp.add(new JLabel("New column name:"), "1,1");
        mTextFieldColumnName = new JTextField(12);
        mp.add(mTextFieldColumnName, "3,1");
        return mp;
        }

	@Override
	public boolean isConfigurable() {
		if (mTableModel.getListHandler().getListCount() == 0) {
			showErrorMessage("No row lists found.");
			return false;
			}
		return true;
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
		String columnName = mTextFieldColumnName.getText();
		if (columnName.length() != 0 && !columnName.equals(DEFAULT_COLUMN_NAME))
			configuration.put(PROPERTY_COLUMN_NAME, mTextFieldColumnName.getText());
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mTextFieldColumnName.setText(configuration.getProperty(PROPERTY_COLUMN_NAME, DEFAULT_COLUMN_NAME));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mTextFieldColumnName.setText(DEFAULT_COLUMN_NAME);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		return true;
		}

	@Override
	public void runTask(Properties configuration) {
        String[] columnName = new String[1];
		columnName[0] = configuration.getProperty(PROPERTY_COLUMN_NAME, DEFAULT_COLUMN_NAME);
        int column = mTableModel.addNewColumns(columnName);

        CompoundTableListHandler hitlistHandler = mTableModel.getListHandler();

        startProgress("Compiling List Names...", 0, mTableModel.getTotalRowCount());

        for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
            if (threadMustDie())
                break;

            if ((row % 16) == 15)
            	updateProgress(row);

            StringBuilder buf = null;
            CompoundRecord record = mTableModel.getTotalRecord(row);
            for (int i = 0; i<hitlistHandler.getListCount(); i++) {
                if (record.isFlagSet(hitlistHandler.getListFlagNo(i))) {
                    if (buf == null)
                        buf = new StringBuilder();
                    else
                        buf.append(CompoundTableModel.cEntrySeparator);
                    buf.append(hitlistHandler.getListName(i));
                    }
                }

            mTableModel.setTotalValueAt(buf==null ? "" : buf.toString(), row, column);
            }

        mTableModel.finalizeNewColumns(column, this);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
    }
