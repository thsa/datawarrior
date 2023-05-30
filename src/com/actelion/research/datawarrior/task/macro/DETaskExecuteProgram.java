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
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Properties;

public class DETaskExecuteProgram extends ConfigurableTask {
	public static final String TASK_NAME = "Execute Program";

	private static final String PROPERTY_EXECUTABLE = "exe";
	private static final String PROPERTY_PARAMETERS = "params";
	private static final String PROPERTY_WAIT_TO_FINISH = "wait";

	private JTextField mTextFieldExecutable,mTextFieldParameters;
	private JCheckBox mCheckBoxWaitToFinish;

	public DETaskExecuteProgram(Frame parent) {
		super(parent, false);
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
	public JPanel createDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		content.add(new JLabel("Path to executable:"), "1,1");
		mTextFieldExecutable = new JTextField(32);
		content.add(mTextFieldExecutable, "3,1");

		content.add(new JLabel("Parameters:"), "1,3");
		mTextFieldParameters = new JTextField(32);
		content.add(mTextFieldParameters, "3,3");

		mCheckBoxWaitToFinish = new JCheckBox("Wait for the program to finish");
		content.add(mCheckBoxWaitToFinish, "1,5,3,5");

		return content;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_EXECUTABLE, mTextFieldExecutable.getText().trim());
		configuration.setProperty(PROPERTY_PARAMETERS, mTextFieldParameters.getText().trim());
		configuration.setProperty(PROPERTY_WAIT_TO_FINISH, mCheckBoxWaitToFinish.isSelected() ? "true" : "false");
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mTextFieldExecutable.setText(configuration.getProperty(PROPERTY_EXECUTABLE, ""));
		mTextFieldParameters.setText(configuration.getProperty(PROPERTY_PARAMETERS, ""));
		mCheckBoxWaitToFinish.setSelected("true".equals(configuration.getProperty(PROPERTY_WAIT_TO_FINISH)));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mTextFieldExecutable.setText("");
		mTextFieldParameters.setText("");
		mCheckBoxWaitToFinish.setSelected(true);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String executable = configuration.getProperty(PROPERTY_EXECUTABLE);
		if (executable == null || executable.length() == 0) {
			showErrorMessage("No path/name defined for executable program defined.");
			return false;
			}
		if (!new File(resolvePathVariables(executable)).exists()) {
			showErrorMessage("Program path '"+executable+"' doesn't exist.");
			return false;
			}
		if (new File(resolvePathVariables(executable)).isDirectory()) {
			showErrorMessage("Path '"+executable+"' is a directory.");
			return false;
			}
		if (!new File(executable).canExecute()) {
			showErrorMessage("Cannot execute program '"+executable+"'.");
			return false;
			}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		String executable = resolvePathVariables(configuration.getProperty(PROPERTY_EXECUTABLE));
		String parameters = configuration.getProperty(PROPERTY_PARAMETERS, "");
		if (parameters.length() != 0)
			executable = executable.concat(" ").concat(resolvePathVariables(parameters));

		boolean wait = "true".equals(configuration.getProperty(PROPERTY_WAIT_TO_FINISH));
		try {
			Process p = Runtime.getRuntime().exec(executable);
			if (wait)
				p.waitFor();
			}
		catch (Exception e) {
			e.printStackTrace();
			}
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
	}
}
