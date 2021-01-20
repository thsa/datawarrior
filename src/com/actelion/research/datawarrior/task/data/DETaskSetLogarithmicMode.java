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

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DETableView;
import com.actelion.research.datawarrior.task.AbstractMultiColumnTask;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

import static com.actelion.research.chem.io.CompoundTableConstants.cAllowLogModeForNegativeOrZeroValues;


public class DETaskSetLogarithmicMode extends AbstractMultiColumnTask {
	public static final String TASK_NAME = "Set Logarithmic Column Mode";

	private static final String PROPERTY_LOGARITHMIC = "isLogarithmic";

	private DETableView             mTableView;
    private JCheckBox				mCheckBoxIsLogarithmic;
	private boolean					mIsLogarithmic;

	public DETaskSetLogarithmicMode(Frame owner, CompoundTableModel tableModel) {
		this(owner, tableModel, null, false);
	}

	public DETaskSetLogarithmicMode(Frame owner, CompoundTableModel tableModel, int[] columnList, boolean isLogarithmic) {
		super(owner, tableModel, false, columnList);
		mIsLogarithmic = isLogarithmic;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		Properties configuration = super.getPredefinedConfiguration();
		if (configuration != null)
			configuration.setProperty(PROPERTY_LOGARITHMIC, mIsLogarithmic ? "true" : "false");
		return configuration;
		}

	@Override
	public JPanel createInnerDialogContent() {
		double[][] size = { {TableLayout.PREFERRED}, {TableLayout.PREFERRED, 16} };

		mCheckBoxIsLogarithmic = new JCheckBox("Treat column data logarithmically");

		JPanel ip = new JPanel();
		ip.setLayout(new TableLayout(size));
		ip.add(mCheckBoxIsLogarithmic, "0,0");
		return ip;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.put(PROPERTY_LOGARITHMIC, mCheckBoxIsLogarithmic.isSelected() ? "true" : "false");
		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mCheckBoxIsLogarithmic.setSelected("true".equals(configuration.getProperty(PROPERTY_LOGARITHMIC)));
	}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mCheckBoxIsLogarithmic.setSelected(false);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isCompatibleColumn(int column) {
		return getTableModel().isColumnTypeDouble(column)
			&& !getTableModel().isColumnTypeDate(column)
			&& (cAllowLogModeForNegativeOrZeroValues
			 || getTableModel().getMinimumValue(column) > 0
			 || getTableModel().isLogarithmicViewMode(column));
		}

	@Override
	public void runTask(Properties configuration) {
		int[] columnList = getColumnList(configuration);
		boolean isLogarithmic = "true".equals(configuration.getProperty(PROPERTY_LOGARITHMIC));
		for (int column:columnList)
			getTableModel().setLogarithmicViewMode(column, isLogarithmic);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
