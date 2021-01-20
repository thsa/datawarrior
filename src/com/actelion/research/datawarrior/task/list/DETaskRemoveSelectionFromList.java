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

package com.actelion.research.datawarrior.task.list;

import java.util.Properties;

import com.actelion.research.datawarrior.DEFrame;


public class DETaskRemoveSelectionFromList extends DETaskAbstractListTask {
	public static final String TASK_NAME = "Remove Selection From List";

    /**
     * The listIndex parameter may be used to override the configuration's list name.
     * If listIndex is preconfigured (i.e. != -1) and defineAndRun() is called, then
     * this task will immediately run without showing a configuration dialog.
     * @param parent
     * @param listIndex -1 or valid list index
     */
    public DETaskRemoveSelectionFromList(DEFrame parent, int listIndex) {
		super(parent, listIndex);
        }

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/lists.html#Selection";
		}

	@Override
	public void runTask(Properties configuration) {
		getTableModel().getListHandler().removeSelected(getListIndex(configuration));
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
    }
