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

import com.actelion.research.datawarrior.task.AbstractMultiColumnTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Properties;

public class DETaskCalculateSelectivityScore extends AbstractMultiColumnTask {
	public static final String TASK_NAME = "Calculate Selectivity Score";

	private static final String PROPERTY_PREPROCESS = "preprocess";

	private static final String[] PREPROCESS_TEXT = { "Take values as they are", "Invert scale (new = 100 - old)" };
	private static final String[] PREPROCESS_CODE = { "none", "100-x" };
	private static final int PREPROCESS_INVERT = 1;

	private JComboBox mComboBoxPreprocess;

	public DETaskCalculateSelectivityScore(Frame owner, CompoundTableModel tableModel) {
		super(owner, tableModel, true);
		}

    /*
     * Instantiates this task interactively with a pre-defined configuration.
     * @param owner
     * @param tableModel
     * @param column
     *
	public DETaskCalculateSelectivityScore(Frame owner, CompoundTableModel tableModel, int[] column) {
		super(owner, tableModel, true, column);
		}*/

	@Override
	public boolean isCompatibleColumn(int column) {
		return getTableModel().isColumnTypeDouble(column) && !getTableModel().isColumnTypeDate(column);
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.put(PROPERTY_PREPROCESS, PREPROCESS_CODE[mComboBoxPreprocess.getSelectedIndex()]);
		return configuration;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public String getHelpURL() {
		return "/html/help/data.html#Gini";
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mComboBoxPreprocess.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_PREPROCESS), PREPROCESS_CODE, 0));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mComboBoxPreprocess.setSelectedIndex(0);
	}

	@Override
	public JPanel createInnerDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap} };

		mComboBoxPreprocess = new JComboBox(PREPROCESS_TEXT);

		JPanel ip = new JPanel();
		ip.setLayout(new TableLayout(size));
		ip.add(new JLabel("Value preprocessing:"), "1,1");
		ip.add(mComboBoxPreprocess, "3,1");
		return ip;
		}

	@Override
	public void runTask(Properties configuration) {
		int[] columnList = getColumnList(configuration);
		float[] value = new float[columnList.length];

		int preprocessing = findListIndex(configuration.getProperty(PROPERTY_PREPROCESS), PREPROCESS_CODE, 0);

		String[] columnName = new String[1];
		columnName[0] = "Gini Selectivity Score";

		int scoreColumn = getTableModel().addNewColumns(columnName);
		startProgress("Calculating Gini scores...", 0, getTableModel().getTotalRowCount());

		for (int row=0; row<getTableModel().getTotalRowCount(); row++) {
			if (threadMustDie())
				break;
			updateProgress(row);

			CompoundRecord record = getTableModel().getTotalRecord(row);
			double score = calculateGiniScore(record, columnList, value, preprocessing);
			record.setData(DoubleFormat.toString(score).getBytes(), scoreColumn);
			}

		getTableModel().finalizeNewColumns(scoreColumn, null);
		}

	private double calculateGiniScore(CompoundRecord record, int[] columnList, float[] value, int preprocessing) {
		for (int i=0; i<columnList.length; i++) {
			value[i] = record.getDouble(columnList[i]);
			if (getTableModel().isLogarithmicViewMode(columnList[i]) && !Float.isNaN(value[i]))
				value[i] = (float)Math.pow(10, value[i]);
			if (preprocessing == PREPROCESS_INVERT)
				value[i] = 100 - value[i];
			}

		Arrays.sort(value);
		int count = 0;
		float area = 0;
		float sum = 0;

		for (int i=0; i<columnList.length; i++) {
			if (!Float.isNaN(value[i])) {
				sum += value[i];
				area += sum;
				count++;
				}
			}
		area -= sum / 2;	// trapez method;

		return (count <= 1) ? Double.NaN : 1.0 - 2.0 * area / (sum * count);
		}
	}
