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

package com.actelion.research.datawarrior.task.file;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.AbstractWindowTask;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskCloseWindow extends AbstractWindowTask {
	public static final String TASK_NAME = "Close Window";

	private static final String PROPERTY_SAVE_CHANGES = "saveChanges";
	private static final String[] MODE_TEXT = { "Ask before closing", "Save without asking", "Close without saving" };
	private static final String[] MODE_CODE = { "ask", "yes", "no" };
	private static final int SAVE_CHANGES_ASK = 0;
	private static final int SAVE_CHANGES_YES = 1;
	private static final int SAVE_CHANGES_NO = 2;

	private JComboBox mComboBoxMode;

	public DETaskCloseWindow(Frame parent, DataWarrior application, DEFrame window) {
		super(parent, application, window);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public JPanel createInnerDialogContent() {
		double[][] size = { {TableLayout.PREFERRED, 8, TableLayout.PREFERRED}, {TableLayout.PREFERRED} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		mComboBoxMode = new JComboBox(MODE_TEXT);
		content.add(new JLabel("Unsaved changes:"), "0,0");
		content.add(mComboBoxMode, "2,0");

		return content;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_SAVE_CHANGES, MODE_CODE[mComboBoxMode.getSelectedIndex()]);
		return configuration;
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mComboBoxMode.setSelectedIndex(SAVE_CHANGES_ASK);
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mComboBoxMode.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_SAVE_CHANGES), MODE_CODE, SAVE_CHANGES_ASK));
		}

	@Override
	public void runTask(Properties configuration) {
		int saveChanges = findListIndex(configuration.getProperty(PROPERTY_SAVE_CHANGES), MODE_CODE, SAVE_CHANGES_ASK);
		if (saveChanges != SAVE_CHANGES_ASK)
			getApplication().closeFrameSilently(getConfiguredWindow(configuration), saveChanges == SAVE_CHANGES_YES);
		else {
			int result = getApplication().closeFrameSafely(getConfiguredWindow(configuration), isInteractive());
			if (result != 0)
				configuration.setProperty(PROPERTY_SAVE_CHANGES, MODE_CODE[result == 1 ? SAVE_CHANGES_NO : SAVE_CHANGES_YES]);
			}
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return getApplication().getNewFrontFrameAfterClosing();
		}
	}
