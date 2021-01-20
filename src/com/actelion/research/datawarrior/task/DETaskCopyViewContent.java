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
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.table.view.CompoundTableView;

import java.util.Properties;

public class DETaskCopyViewContent extends AbstractTaskWithoutConfiguration {
    public static final String TASK_NAME = "Copy View Content";

    DEFrame mParentFrame;

    public DETaskCopyViewContent(DEFrame parent) {
		super(parent, false);
		mParentFrame = parent;
		}

	@Override
	public boolean isConfigurable() {
		if (((DEFrame)getParentFrame()).getMainFrame().getMainPane().getSelectedView() == null) {
			showErrorMessage("There is no view. Thus, no view content can be copied.");
			return false;
			}
    	return true;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public void runTask(Properties configuration) {
		DEMainPane mainPane = ((DEFrame)getParentFrame()).getMainFrame().getMainPane();
		CompoundTableView view = mainPane.getSelectedView();
		if (view != null && !view.copyViewContent())
			showErrorMessage("The active view '"+mainPane.getSelectedViewTitle()+"' doesn't support copying.");
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
