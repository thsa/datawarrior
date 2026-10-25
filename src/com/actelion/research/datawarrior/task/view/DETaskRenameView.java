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

package com.actelion.research.datawarrior.task.view;

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.task.AbstractViewTask;
import com.actelion.research.table.view.CompoundTableView;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskRenameView extends AbstractViewTask {
	public static final String TASK_NAME = "Rename View";

	private static final String PROPERTY_NEW_NAME = "newName";

	private final DEMainPane	mMainPane;
	private JTextField	mTextFieldNewName;

	public DETaskRenameView(Frame parent, DEMainPane mainPane, CompoundTableView view) {
		super(parent, mainPane, view);
		mMainPane = mainPane;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public JComponent createInnerDialogContent() {
		JPanel p = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
		        			{8, TableLayout.PREFERRED, 8} };
        p.setLayout(new TableLayout(size));

        p.add(new JLabel("Rename to:"), "1,1");
		mTextFieldNewName = new JTextField(16);
        p.add(mTextFieldNewName, "3,1");

		return p;
		}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return false;
		}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return null;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_NEW_NAME, mTextFieldNewName.getText());
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		if (getInteractiveView() == null) {
			mTextFieldNewName.setText(configuration.getProperty(PROPERTY_NEW_NAME));
			}
		else {
			mTextFieldNewName.setText(getInteractiveViewName());
			}
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		if (getInteractiveViewName() != null)
			mTextFieldNewName.setText(getInteractiveViewName());
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String newTitle = configuration.getProperty(PROPERTY_NEW_NAME, "");
		if (newTitle.isEmpty()) {
			showErrorMessage("New view name is not defined.");
			return false;
			}

		if (isLive && mMainPane.getDockableTitles().contains(newTitle)) {
			showErrorMessage("New view name is already used.");
			return false;
			}

		return super.isConfigurationValid(configuration, isLive);
		}

	@Override
	public void runTask(Properties configuration) {
		String newName = configuration.getProperty(PROPERTY_NEW_NAME);
		mMainPane.renameView(getConfiguredViewName(configuration), newName);
		}
	}
