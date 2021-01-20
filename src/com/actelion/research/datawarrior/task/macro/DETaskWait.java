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

public class DETaskWait extends ConfigurableTask {
    public static final String TASK_NAME = "Wait Some Seconds";

    private static final String PROPERTY_SECONDS = "seconds";

    private JTextField mTextFieldSeconds;

	public DETaskWait(Frame parent) {
		super(parent, true);
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
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));
		content.add(new JLabel("Wait for"), "1,1");
		mTextFieldSeconds = new JTextField(6);
		content.add(mTextFieldSeconds, "3,1");
		content.add(new JLabel("seconds"), "5,1");

		return content;
		}

	@Override
    public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_SECONDS, mTextFieldSeconds.getText());
		return configuration;
		}

	@Override
    public void setDialogConfiguration(Properties configuration) {
		mTextFieldSeconds.setText(configuration.getProperty(PROPERTY_SECONDS, "5"));
		}

	@Override
    public void setDialogConfigurationToDefault() {
		mTextFieldSeconds.setText("5");
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		try {
			float seconds = Float.parseFloat(configuration.getProperty(PROPERTY_SECONDS, "5"));
			if (seconds <= 0) {
				showErrorMessage("Time to wait is 0s or less.");
				return false;
				}
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("Value is not a number.");
			return false;
			}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		final float seconds = Float.parseFloat(configuration.getProperty(PROPERTY_SECONDS, "5"));
		try {
			Thread.sleep((long)(seconds * 1000));
			}
		catch (InterruptedException ie) {}
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
