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
package com.actelion.research.datawarrior.task.db;

import com.actelion.research.calc.ProgressController;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.plugin.PluginHelper;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.datawarrior.plugin.PluginGUIHelper;
import org.openmolecules.datawarrior.plugin.IPluginTask;

import javax.swing.*;
import java.util.Properties;

/**
 * This is a facade for any database plugin task
 */
public class DETaskPluginTask extends ConfigurableTask {
	private IPluginTask mDelegate;
	private PluginHelper mPluginHelper;
	private DataWarrior mApplication;

	public DETaskPluginTask(DEFrame owner, IPluginTask delegate) {
		super(owner, true);
		mApplication = owner.getApplication();
		mDelegate = delegate;
		}

	@Override
	public boolean isConfigurable() {
		return true;
		}

	@Override
	public String getTaskName() {
		return mDelegate.getTaskName();
	}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String error = mDelegate.checkConfiguration(configuration);
		if (error == null)
			return true;

		showErrorMessage(error);
		return false;
		}

	@Override
	public void runTask(Properties configuration) {
		ProgressController pc = getProgressController();
		mPluginHelper = new PluginHelper(mApplication, pc);
		pc.startProgress("Retrieving data from plugin...", 0, 0);
		mDelegate.run(configuration, mPluginHelper);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return mPluginHelper.getNewFrame();
		}

	@Override
	public JComponent createDialogContent() {
		return mDelegate.createDialogContent(new PluginGUIHelper());
		}

	@Override
	public Properties getDialogConfiguration() {
		return mDelegate.getDialogConfiguration();
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mDelegate.setDialogConfiguration(configuration);
		}

	@Override
	public void setDialogConfigurationToDefault() {
		//
		}
	}
