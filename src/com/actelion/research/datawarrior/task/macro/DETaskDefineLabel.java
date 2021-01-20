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

import com.actelion.research.datawarrior.task.ConfigurableTask;
import info.clearthought.layout.TableLayout;

import java.awt.Frame;
import java.util.Properties;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.actelion.research.datawarrior.DEFrame;

public class DETaskDefineLabel extends ConfigurableTask {
    public static final String TASK_NAME = "Define Label";

	public static final String PROPERTY_LABEL = "label";

    private JTextField mTextFieldName;

	public DETaskDefineLabel(Frame parent) {
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
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));
		content.add(new JLabel("Label name:"), "1,1");
		mTextFieldName = new JTextField(6);
		content.add(mTextFieldName, "3,1");

		return content;
		}

	@Override
    public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_LABEL, mTextFieldName.getText().trim());
		return configuration;
		}

	@Override
    public void setDialogConfiguration(Properties configuration) {
		mTextFieldName.setText(configuration.getProperty(PROPERTY_LABEL, "Label1"));
		}

	@Override
    public void setDialogConfigurationToDefault() {
		mTextFieldName.setText("Label1");
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String name = configuration.getProperty(PROPERTY_LABEL);
		if (name == null || name.length() == 0) {
			showErrorMessage("No label name defined.");
			return false;
			}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		// don't do anything
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
