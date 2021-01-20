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
import java.util.Properties;

import javax.swing.JComponent;
import javax.swing.JTable;

import com.actelion.research.chem.io.CompoundFileHelper;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DERuntimeProperties;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.CompoundTableSaver;

public class DETaskSaveTemplateFileAs extends DETaskAbstractSaveFile {
    public static final String TASK_NAME = "Export Template";

	public DETaskSaveTemplateFileAs(DEFrame parent) {
		super(parent, "");
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public int getFileType() {
		return CompoundFileHelper.cFileTypeDataWarriorTemplate;
		}

	@Override
	public JComponent createInnerDialogContent() {
		return null;
		}

	@Override
	public boolean isConfigurable() {
		if (getTableModel().isEmpty()) {
			showErrorMessage("Templates of empty documents cannot be saved.");
			return false;
			}
		return true;
		}

	@Override
	public void saveFile(File file, Properties configuration) {
		CompoundTableModel tableModel = ((DEFrame)getParentFrame()).getMainFrame().getTableModel();
		JTable table = ((DEFrame)getParentFrame()).getMainFrame().getMainPane().getTable();
		DERuntimeProperties rtp = new DERuntimeProperties(((DEFrame)getParentFrame()).getMainFrame());
		new CompoundTableSaver(getParentFrame(), tableModel, table).saveTemplate(rtp, file);
		}
	}
