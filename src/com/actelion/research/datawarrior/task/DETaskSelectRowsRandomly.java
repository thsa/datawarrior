/*
 * Copyright 2019 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91, CH-4123 Allschwil, Switzerland
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

import com.actelion.research.datawarrior.*;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;
import java.util.Random;

public class DETaskSelectRowsRandomly extends ConfigurableTask {
	public static final String TASK_NAME = "Select Rows Randomly";

	private static final String PROPERTY_PERCENTAGE = "percent";

	private JTextField mTextFieldPercentage;
	private CompoundTableModel mTableModel;

	public DETaskSelectRowsRandomly(Frame owner, CompoundTableModel tableModel) {
		super(owner, false);
		mTableModel = tableModel;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public boolean isConfigurable() {
		if (mTableModel.getTotalRowCount() == 0) {
			showErrorMessage("There are no rows to be selected.");
			return false;
			}
		return true;
		}

	@Override
	public JComponent createDialogContent() {
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
				{8, TableLayout.PREFERRED, 8} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));
		content.add(new JLabel("Select"), "1,1");
		mTextFieldPercentage = new JTextField(3);
		content.add(mTextFieldPercentage, "3,1");
		content.add(new JLabel("percent of all rows."), "5,1");

		return content;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_PERCENTAGE, mTextFieldPercentage.getText());
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mTextFieldPercentage.setText(configuration.getProperty(PROPERTY_PERCENTAGE, ""));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mTextFieldPercentage.setText("50");
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String value = configuration.getProperty(PROPERTY_PERCENTAGE);
		if (value != null) {
			try {
				int percent = Integer.parseInt(value);
				if (percent <= 0 || percent >= 100) {
					showErrorMessage("Percentage must be between 1 and 99.");
					return false;
					}
				}
			catch (NumberFormatException nfe) {
				showErrorMessage("Percentage is not an integer.");
				return false;
				}
			}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		int percent = Integer.parseInt(configuration.getProperty(PROPERTY_PERCENTAGE));
		int selectionCount = mTableModel.getTotalRowCount() * percent / 100;
		int totalCount = mTableModel.getTotalRowCount();

		int[] rowNo = new int[totalCount];
		for (int i=0; i<rowNo.length; i++)
			rowNo[i] = i;

		Random random = new Random();
		for (int i=0; i<selectionCount; i++) {
			int selected = i + random.nextInt(totalCount - i);
			int temp = rowNo[i];
			rowNo[i] = rowNo[selected];
			rowNo[selected] = temp;
			}

		for (int i=0; i<selectionCount; i++)
			mTableModel.setSelected(rowNo[i]);
		for (int i=selectionCount; i<totalCount; i++)
			mTableModel.clearSelected(rowNo[i]);

		mTableModel.invalidateSelectionModel();
		}

	public DEFrame getNewFrontFrame() { return null; }
	}
