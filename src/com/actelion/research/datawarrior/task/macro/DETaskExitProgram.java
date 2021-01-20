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

package com.actelion.research.datawarrior.task.macro;


import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.util.Properties;

public class DETaskExitProgram extends ConfigurableTask {
	public static final String TASK_NAME = "Exit Program";

	private static final String PROPERTY_SAVE_CHANGES = "saveChanges";
	private static final String[] MODE_TEXT = { "Ask before closing", "Save without asking", "Close without saving" };
	private static final String[] MODE_CODE = { "ask", "yes", "no" };
	private static final int SAVE_CHANGES_ASK = 0;
	private static final int SAVE_CHANGES_YES = 1;
	private static final int SAVE_CHANGES_NO = 2;

	private DataWarrior mApplication;
	private JComboBox mComboBoxMode;

	public DETaskExitProgram(DEFrame parent, DataWarrior application) {
		super(parent, false);
		mApplication = application;
	}

	@Override
	public boolean isConfigurable() {
		return true;
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public Properties getPredefinedConfiguration() {
		if (isInteractive()) {
			Properties configuration = new Properties();
			configuration.setProperty(PROPERTY_SAVE_CHANGES, MODE_CODE[SAVE_CHANGES_ASK]);
			return configuration;
			}

		return null;
	}

	@Override
	public JPanel createDialogContent() {
		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		mComboBoxMode = new JComboBox(MODE_TEXT);
		content.add(new JLabel("Unsaved changes:"), "1,1");
		content.add(mComboBoxMode, "3,1");

		return content;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		configuration.setProperty(PROPERTY_SAVE_CHANGES, MODE_CODE[mComboBoxMode.getSelectedIndex()]);

		return configuration;
	}

	@Override
	public void setDialogConfigurationToDefault() {
		mComboBoxMode.setSelectedIndex(SAVE_CHANGES_YES);
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mComboBoxMode.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_SAVE_CHANGES), MODE_CODE, SAVE_CHANGES_YES));
	}

	@Override
	public String getHelpURL() {
		return "/html/help/macros.html#CommandLine";
	}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		return true;
	}

	@Override
	public void runTask(Properties configuration) {
		int saveChanges = findListIndex(configuration.getProperty(PROPERTY_SAVE_CHANGES), MODE_CODE, SAVE_CHANGES_YES);

		if (saveChanges != SAVE_CHANGES_ASK) {	// is running a macro
			mApplication.closeAllFramesSilentlyAndExit(saveChanges == SAVE_CHANGES_YES);
			return;
		}

		mApplication.closeApplication(isInteractive());
	}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
	}
}
