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
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskShowMessage extends ConfigurableTask {
    public static final String TASK_NAME = "Show Message";

    private static final String PROPERTY_MESSAGE = "message";

    private JTextArea mTextAreaMessage;

	public DETaskShowMessage(Frame parent) {
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
							{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));
		content.add(new JLabel("Message:"), "1,1");
		mTextAreaMessage = new JTextArea(6, 40);
		mTextAreaMessage.setLineWrap(true);
		JScrollPane sp = new JScrollPane(mTextAreaMessage,
										JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
										JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		content.add(sp, "1,3");
		return content;
		}

	@Override
    public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_MESSAGE, mTextAreaMessage.getText());
		return configuration;
		}

	@Override
    public void setDialogConfiguration(Properties configuration) {
		mTextAreaMessage.setText(configuration.getProperty(PROPERTY_MESSAGE, ""));
		}

	@Override
    public void setDialogConfigurationToDefault() {
		mTextAreaMessage.setText("");
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String message = configuration.getProperty(PROPERTY_MESSAGE, "");
		if (message.length() == 0) {
			showErrorMessage("No message defined.");
			return false;
			}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		JOptionPane.showMessageDialog(getParentFrame(), resolveVariables(configuration.getProperty(PROPERTY_MESSAGE, "")),
									  "Show Message Task", JOptionPane.INFORMATION_MESSAGE);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
