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
import com.actelion.research.datawarrior.task.DEMacro;
import com.actelion.research.datawarrior.task.DEMacroRecorder;
import info.clearthought.layout.TableLayout;

import java.util.ArrayList;
import java.util.Properties;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEMacroEditor;
import com.actelion.research.table.view.CompoundTableView;

public class DETaskSetMessageMode extends ConfigurableTask {
	public static final String TASK_NAME = "Set Message Mode";

	private static final String PROPERTY_MODE = "mode";

	// these Strings map on the DEMacroRecorder's MESSAGE_MODE_XXX options
	private static final String[] MODE_TEXT = { "Show all error messages", "Show first error only", "Suppress all error messages" };
	private static final String[] MODE_CODE = { "showErrors", "showOneError", "skipErrors" };


	private DEFrame		mParentFrame;
	private JComboBox	mComboBox;

	/**
	 * @param parentFrame
	 */
	public DETaskSetMessageMode(DEFrame parentFrame) {
		super(parentFrame, false);
		mParentFrame = parentFrame;
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
	public JComponent createDialogContent() {
		JPanel p = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED} };
		p.setLayout(new TableLayout(size));

		p.add(new JLabel("Message mode:"), "1,1");
		mComboBox = new JComboBox(MODE_TEXT);
		p.add(mComboBox, "3,1");

		return p;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_MODE, MODE_CODE[mComboBox.getSelectedIndex()]);
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mComboBox.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_MODE), MODE_CODE, DEMacroRecorder.DEFAULT_MESSAGE_MODE));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mComboBox.setSelectedIndex(DEMacroRecorder.DEFAULT_MESSAGE_MODE);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		if (!isInteractive())
			DEMacroRecorder.getInstance().setMessageMode(findListIndex(configuration.getProperty(PROPERTY_MODE), MODE_CODE, DEMacroRecorder.DEFAULT_MESSAGE_MODE));
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
	}
}
