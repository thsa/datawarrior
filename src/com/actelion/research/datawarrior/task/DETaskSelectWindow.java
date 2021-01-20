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

import java.awt.Frame;
import java.util.Properties;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;

public class DETaskSelectWindow extends AbstractWindowTask {
	public static final String TASK_NAME = "Select Window";

    private DEFrame mNewFrontFrame;

    public DETaskSelectWindow(Frame parent, DataWarrior application, DEFrame window) {
		super(parent, application, window);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isConfigurable() {
		return (getApplication().getFrameList().size() > 1);
		}

	@Override
	public void runTask(Properties configuration) {
		mNewFrontFrame = getConfiguredWindow(configuration);
		if (mNewFrontFrame != null)
			getApplication().setActiveFrame(mNewFrontFrame);
		}

	@Override
	public boolean isRedundant(Properties previousConfiguration, Properties currentConfiguration) {
		return true;
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return mNewFrontFrame;
		}
	}
