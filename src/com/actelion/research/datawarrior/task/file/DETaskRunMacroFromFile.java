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

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.DEMacro;
import com.actelion.research.datawarrior.task.DEMacroRecorder;
import com.actelion.research.datawarrior.task.macro.GenericTaskRunMacro;
import com.actelion.research.gui.FileHelper;

public class DETaskRunMacroFromFile extends DETaskAbstractOpenFile implements GenericTaskRunMacro {
	public static final String TASK_NAME = "Open And Run Macro";

    public DETaskRunMacroFromFile(DataWarrior application) {
		super(application, "Open And Execute Macro",
				FileHelper.cFileTypeDataWarriorMacro);
		}

    public DETaskRunMacroFromFile(DataWarrior application, String filePath) {
		super(application, "Open And Execute Macro",
				FileHelper.cFileTypeDataWarriorMacro, filePath);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public DEMacro getMacro(Properties configuration) {
		if (!isConfigurationValid(configuration, true))
			return null;

		String fileName = configuration.getProperty(PROPERTY_FILENAME);

		File file = ASK_FOR_FILE.equals(fileName) ? askForFile(null) : new File(resolvePathVariables(fileName));
		if (file == null) {
			showErrorMessage("No file was chosen.");
			return null;
			}

		try {
			return new DEMacro(file, null);
			}
		catch (IOException ioe) {
			return null;
			}
		}

	@Override
	public DEFrame openFile(File file, Properties configuration) {
		try {
			DEMacro macro = new DEMacro(file, null);
			DEMacroRecorder.getInstance().runMacro(macro, getApplication().getActiveFrame());
			}
		catch (IOException ioe) {}
		return null;
		}
	}
