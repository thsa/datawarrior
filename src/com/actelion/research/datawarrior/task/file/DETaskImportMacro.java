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
import java.util.ArrayList;
import java.util.Properties;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEMacroEditor;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.DEMacro;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.dock.Dockable;
import com.actelion.research.table.model.CompoundTableModel;

import javax.swing.*;

public class DETaskImportMacro extends DETaskAbstractOpenFile {
	public static final String TASK_NAME = "Import Macro";

	private CompoundTableModel mTableModel;

	public DETaskImportMacro(DataWarrior application) {
		super(application, "Import DataWarrior Macro", FileHelper.cFileTypeDataWarriorMacro);
		mTableModel = application.getActiveFrame().getTableModel();
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public DEFrame openFile(File file, Properties configuration) {
		try {
			@SuppressWarnings("unchecked")
			ArrayList<DEMacro> macroList = (ArrayList<DEMacro>) mTableModel.getExtensionData(CompoundTableConstants.cExtensionNameMacroList);
			DEMacro macro = new DEMacro(file, macroList);
			if (macroList == null)
				macroList = new ArrayList<>();
			macroList.add(macro);
			mTableModel.setExtensionData(CompoundTableConstants.cExtensionNameMacroList, macroList);

			for (Dockable d : ((DEFrame) getParentFrame()).getMainFrame().getMainPane().getDockables())
				if (d.getContent() instanceof DEMacroEditor)
					selectMacroInEDT((DEMacroEditor) d.getContent(), macro);
		} catch (IOException ioe) {
			showErrorMessage(ioe.toString());
		}
		return null;
	}

	private void selectMacroInEDT(final DEMacroEditor editor, final DEMacro macro) {
		try {
			SwingUtilities.invokeAndWait(() -> editor.selectMacro(macro) );
		} catch (Exception e) {}
	}
}
