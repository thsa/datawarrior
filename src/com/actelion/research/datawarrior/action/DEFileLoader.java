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

package com.actelion.research.datawarrior.action;

import java.io.File;

import com.actelion.research.calc.ProgressController;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DERuntimeProperties;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.table.CompoundTableLoader;

public class DEFileLoader {
	private DEFrame mActiveFrame;
	private ProgressController mProgressController;

	public DEFileLoader(DEFrame activeFrame, ProgressController pc) {
		mActiveFrame = activeFrame;
		mProgressController = pc;
		}

	/**
	 * Opens file extracts the template and applies it to the current document.
	 * Allowed file types are those defined in FileHelper.cFileTypeDataWarriorTemplateContaining.
	 * @param file
	 */
/*	public void applyTemplate(File file) {
        CompoundTableLoader loader = new CompoundTableLoader(mActiveFrame, mActiveFrame.getTableModel(), mProgressController);
	    loader.readTemplate(file, new DERuntimeProperties(mActiveFrame.getMainFrame()));
	    mActiveFrame.setDirty(true);	// has to be done explicitly, because no CompoundTableModel events are triggered
		}

	public DEFrame openAndRunQuery(File file) {
		return new DEOsirisQueryHandler(mActiveFrame).readQuery(file, mApplication);
		}

	public void openAndRunMacro(File file) {
		try {
			DEMacroRecorder.getInstance().runMacro(new DEMacro(file, null), mActiveFrame);
			}
		catch (IOException ioe) {
			JOptionPane.showMessageDialog(mActiveFrame, ioe.toString());
			}
		}*/

	/**
	 * Analyzes file, displays append dialog and appends data to current document according to specified append options.
	 * Allowed file types are those defined in cFileTypeDataWarriorCompatibleData.
	 * @param file
	 */
	public void appendFile(File file) {
		int fileType = FileHelper.getFileType(file.getName());
		CompoundTableLoader loader = new CompoundTableLoader(mActiveFrame, mActiveFrame.getTableModel(), mProgressController);
		loader.readFile(file, new DERuntimeProperties(mActiveFrame.getMainFrame()), fileType, CompoundTableLoader.READ_DATA);
		new DEAppendFileDialog(mActiveFrame, mActiveFrame.getTableModel(), file.getName(), loader);
		}
	}
