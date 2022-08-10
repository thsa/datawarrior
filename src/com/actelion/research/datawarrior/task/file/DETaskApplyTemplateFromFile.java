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
import com.actelion.research.datawarrior.DERuntimeProperties;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.table.CompoundTableLoader;

import java.io.File;
import java.util.Properties;

public class DETaskApplyTemplateFromFile extends DETaskAbstractOpenFile {
	public static final String TASK_NAME = "Open And Apply Template";

    public DETaskApplyTemplateFromFile(DataWarrior application) {
		super(application, "Open & Apply DataWarrior Template",
				FileHelper.cFileTypeDataWarriorTemplateContaining);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public DEFrame openFile(File file, Properties configuration) {
		DEFrame activeFrame = getApplication().getActiveFrame();
        CompoundTableLoader loader = new CompoundTableLoader(activeFrame, activeFrame.getTableModel(), getProgressController());
	    loader.readTemplate(file, new DERuntimeProperties(activeFrame.getMainFrame()));
	    activeFrame.setDirty(true);	// has to be done explicitly, because no CompoundTableModel events are triggered
		return null;
		}
	}
