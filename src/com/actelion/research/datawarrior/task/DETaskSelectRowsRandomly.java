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
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;
import java.util.Random;

public class DETaskSelectRowsRandomly extends ConfigurableTask {
	public static final String TASK_NAME = "Select Rows Randomly";

	private static final String PROPERTY_PERCENTAGE = "percent";
	private static final String PROPERTY_VISIBLE_ONLY = "visibleOnly";

	private JTextField mTextFieldPercentage;
	private JCheckBox mCheckBoxVisibleRowsOnly;
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
				{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));
		content.add(new JLabel("Select"), "1,1");
		mTextFieldPercentage = new JTextField(3);
		content.add(mTextFieldPercentage, "3,1");
		content.add(new JLabel("percent of rows."), "5,1");

		mCheckBoxVisibleRowsOnly = new JCheckBox("Visible rows only");
		content.add(mCheckBoxVisibleRowsOnly, "1,3,5,3");

		return content;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_PERCENTAGE, mTextFieldPercentage.getText());
		configuration.setProperty(PROPERTY_VISIBLE_ONLY, mCheckBoxVisibleRowsOnly.isSelected() ? "true" : "false");
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mTextFieldPercentage.setText(configuration.getProperty(PROPERTY_PERCENTAGE, ""));
		mCheckBoxVisibleRowsOnly.setSelected(!"false".equals(configuration.getProperty(PROPERTY_VISIBLE_ONLY)));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mTextFieldPercentage.setText("50");
		mCheckBoxVisibleRowsOnly.setSelected(true);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String value = configuration.getProperty(PROPERTY_PERCENTAGE);
		if (value != null) {
			try {
				float percent = Float.parseFloat(value);
				if (percent <= 0 || percent >= 100) {
					showErrorMessage("Percentage value must be in range: 0 < value < 100.");
					return false;
					}
				}
			catch (NumberFormatException nfe) {
				showErrorMessage("Percentage is not numerical.");
				return false;
				}
			}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		float percent = Float.parseFloat(configuration.getProperty(PROPERTY_PERCENTAGE));
		boolean visibleOnly = !"false".equals(configuration.getProperty(PROPERTY_VISIBLE_ONLY));
		int totalCount = visibleOnly ? mTableModel.getRowCount() : mTableModel.getTotalRowCount();
		int selectionCount = Math.round(totalCount * percent / 100);

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

		for (int row=0; row<mTableModel.getTotalRowCount(); row++)
			mTableModel.getTotalRecord(row).clearFlags(CompoundRecord.cFlagMaskSelected);

		for (int i=0; i<selectionCount; i++)
			if (visibleOnly)
				mTableModel.setSelected(rowNo[i]);
			else
				mTableModel.getTotalRecord(rowNo[i]).setFlags(CompoundRecord.cFlagMaskSelected);

		mTableModel.invalidateSelectionModel();
		}

	public DEFrame getNewFrontFrame() { return null; }
	}
