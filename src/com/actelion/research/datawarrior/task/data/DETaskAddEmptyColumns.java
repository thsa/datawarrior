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

package com.actelion.research.datawarrior.task.data;

import com.actelion.research.datawarrior.DataWarrior;

import java.awt.event.ActionListener;
import java.util.Properties;

public class DETaskAddEmptyColumns extends DETaskAbstractNewColumns implements ActionListener {
	public static final String TASK_NAME = "Add Empty Columns";

    public DETaskAddEmptyColumns(DataWarrior application) {
		super(application);
    	}

    @Override
    public String getDialogTitle() {
    	return "Add Empty Columns";
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
	protected boolean createNewTable() {
		return false;
		}

	@Override
	public void runTask(Properties configuration) {
		super.addNewColumns(configuration);
		}
	}
