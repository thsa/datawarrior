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

package com.actelion.research.datawarrior.task.table;

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.DETableView;
import com.actelion.research.datawarrior.task.AbstractViewTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.view.CompoundTableView;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskSetHeaderLineCount extends AbstractViewTask {
    public static final String TASK_NAME = "Set Header Line Count";

	public static final String[] OPTIONS = {"1", "2", "3", "4", "5", "6"};

	private static final String PROPERTY_LINE_COUNT = "lineCount";

    private JComboBox mComboBoxLineCount;
    private int mLineCount;

	public DETaskSetHeaderLineCount(Frame owner, DEMainPane mainPane, CompoundTableView view, int lineCount) {
		super(owner, mainPane, view);
		mLineCount = lineCount;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof DETableView) ? null : "The 'Set Header Line Count' task applies to the table view only.";
		}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return mLineCount != -1;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		Properties configuration = super.getPredefinedConfiguration();
		if (configuration == null || mLineCount == -1)
			return null;

		configuration.put(PROPERTY_LINE_COUNT, Integer.toString(mLineCount));
		return configuration;
		}

	@Override
	public JComponent createInnerDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));
		content.add(new JLabel("Table header row count:"), "1,1");
		mComboBoxLineCount = new JComboBox(OPTIONS);
		content.add(mComboBoxLineCount, "3,1");

		return content;
		}

	@Override
    public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_LINE_COUNT, (String)mComboBoxLineCount.getSelectedItem());
		return configuration;
		}

	@Override
    public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mComboBoxLineCount.setSelectedItem(configuration.getProperty(PROPERTY_LINE_COUNT, "1"));
		}

	@Override
    public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mComboBoxLineCount.setSelectedItem(OPTIONS[0]);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		return super.isConfigurationValid(configuration, isLive);
		}

	@Override
	public void runTask(Properties configuration) {
		CompoundTableView view = getConfiguredView(configuration);
		if (view instanceof DETableView) {
			int rowCount = Integer.parseInt(configuration.getProperty(PROPERTY_LINE_COUNT, OPTIONS[0]));
			((DETableView)view).setHeaderLineCount(rowCount);
			}
		}
	}
