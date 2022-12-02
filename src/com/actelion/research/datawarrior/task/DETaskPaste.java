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

package com.actelion.research.datawarrior.task;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.table.CompoundTableLoader;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.util.Properties;

public class DETaskPaste extends ConfigurableTask {
    public static final String TASK_NAME = "Paste";

	private static final String PROPERTY_HEADER_HANDLING = "header";
	private static final String[] CODE_HEADER_HANDLING = {"no", "yes", "infer"};
	private static final String[] TEXT_HEADER_HANDLING = {"No", "Yes", "Analyze"};
	public static final int HEADER_WITHOUT = 0;
	public static final int HEADER_WITH = 1;
	public static final int HEADER_ANALYZE = 2;
	private static final int DEFAULT_HEADER_HANDLING = HEADER_ANALYZE;

	private DataWarrior	mApplication;
    private DEFrame		mNewFrame;
	private int         mHeaderHandling;
	private JComboBox   mComboBoxHeaderHandling;

    public DETaskPaste(DEFrame parent, DataWarrior application, int headerHandling) {
		super(parent, false);
		mApplication = application;
	    mHeaderHandling = headerHandling;
		}

	public Properties getPredefinedConfiguration() {
		if (!isInteractive())
			return null;

		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_HEADER_HANDLING, CODE_HEADER_HANDLING[mHeaderHandling]);
		return configuration;
		}

	@Override
	public JPanel createDialogContent() {
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
				{8, TableLayout.PREFERRED, 8} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));
		content.add(new JLabel("Includes header row:"), "1,1");
		mComboBoxHeaderHandling = new JComboBox(TEXT_HEADER_HANDLING);
		content.add(mComboBoxHeaderHandling, "3,1");

		return content;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_HEADER_HANDLING,
				CODE_HEADER_HANDLING[mComboBoxHeaderHandling.getSelectedIndex()]);
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mComboBoxHeaderHandling.setSelectedIndex(
				findListIndex(configuration.getProperty(PROPERTY_HEADER_HANDLING),
						CODE_HEADER_HANDLING, DEFAULT_HEADER_HANDLING));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mComboBoxHeaderHandling.setSelectedIndex(DEFAULT_HEADER_HANDLING);
	}


	@Override
	public boolean isConfigurable() {
		return true;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		return true;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public void runTask(Properties configuration) {
		mNewFrame = mApplication.getEmptyFrame(null);
		int headerHandling = findListIndex(configuration.getProperty(PROPERTY_HEADER_HANDLING),
				CODE_HEADER_HANDLING, DEFAULT_HEADER_HANDLING);
		new CompoundTableLoader(mNewFrame, mNewFrame.getTableModel(), null).paste(headerHandling, false);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return mNewFrame;
		}
}
