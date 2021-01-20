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

import java.util.Properties;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.table.model.CompoundTableModel;


public class DETaskAddEmptyRows extends ConfigurableTask {
    public static final String TASK_NAME = "Add Empty Rows";

    private static final String ROW_COUNT = "rowCount";
	private CompoundTableModel mTableModel;
    private JTextField mTextFieldRowCount;

	public DETaskAddEmptyRows(DEFrame parent) {
		super(parent, false);
		mTableModel = parent.getTableModel();
		}

	@Override
	public boolean isConfigurable() {
		return (mTableModel.getTotalColumnCount() != 0);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
    public JPanel createDialogContent() {
		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));
		content.add(new JLabel("Count of new rows:"), "1,1");
		mTextFieldRowCount = new JTextField(6);
		content.add(mTextFieldRowCount, "3,1");

		return content;
		}

	@Override
    public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		try {
			int rowCount = Integer.parseInt(configuration.getProperty(ROW_COUNT, "1"));
			if (rowCount <= 0) {
				showErrorMessage("Row count is lower or equal zero.");
				return false;
				}
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("Row count is not a number.");
			return false;
			}

		return true;
		}

	@Override
    public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(ROW_COUNT, mTextFieldRowCount.getText());
		return configuration;
		}

	@Override
    public void setDialogConfiguration(Properties configuration) {
		mTextFieldRowCount.setText(configuration.getProperty(ROW_COUNT, "1"));
		}

	@Override
    public void setDialogConfigurationToDefault() {
		mTextFieldRowCount.setText("1");
		}

	@Override
	public void runTask(Properties configuration) {
		int rowCount = 1;
		try {
			rowCount = Integer.parseInt(configuration.getProperty("rowCount"));
			}
		catch (NumberFormatException nfe) {
			return;
			}

		int firstNewRow = mTableModel.getTotalRowCount();
		mTableModel.addNewRows(rowCount, true);
	    mTableModel.finalizeNewRows(firstNewRow, null);
		}
	}
