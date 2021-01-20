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

import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.datawarrior.task.DEMacroRecorder;
import com.actelion.research.util.BrowserControl;
import info.clearthought.layout.TableLayout;

import java.awt.Frame;
import java.util.Properties;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.actelion.research.datawarrior.DEFrame;

public class DETaskOpenWebBrowser extends ConfigurableTask {
    public static final String TASK_NAME = "OpenWebBrowser";

	private static final String PROPERTY_URL = "url";
	private static final String DEFAULT_URL = "openmolecules.org/datawarrior/";

    private JTextField mTextFieldURL;

	public DETaskOpenWebBrowser(Frame parent) {
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
		double[][] size = { {8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));
		content.add(new JLabel("URL:"), "1,1");
		mTextFieldURL = new JTextField(64);
		content.add(mTextFieldURL, "1,3");

		return content;
		}

	@Override
    public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_URL, mTextFieldURL.getText().trim());
		return configuration;
		}

	@Override
    public void setDialogConfiguration(Properties configuration) {
		mTextFieldURL.setText(configuration.getProperty(PROPERTY_URL, DEFAULT_URL));
		}

	@Override
    public void setDialogConfigurationToDefault() {
		mTextFieldURL.setText(DEFAULT_URL);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String name = configuration.getProperty(PROPERTY_URL);
		if (name == null || name.length() == 0) {
			showErrorMessage("No URL defined.");
			return false;
			}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		String url = configuration.getProperty(PROPERTY_URL, "");
		if (url.length() != 0) {
			url = DataWarrior.getApplication().resolveURLVariables(((DEMacroRecorder)getProgressController()).resolveVariables(url));
			BrowserControl.displayURL(url);
			}
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
