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

package com.actelion.research.datawarrior.task.macro;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEMacroEditor;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.AbstractTaskWithoutConfiguration;
import com.actelion.research.datawarrior.task.DEMacro;
import com.actelion.research.gui.dock.Dockable;
import com.actelion.research.table.model.CompoundTableModel;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Properties;

public class DETaskPasteMacro extends AbstractTaskWithoutConfiguration {
	public static final String TASK_NAME = "Paste Macro";

	private CompoundTableModel mTableModel;

	/**
	 * @param application
	 */
	public DETaskPasteMacro(DataWarrior application) {
		super(application.getActiveFrame(), false);
		mTableModel = application.getActiveFrame().getTableModel();
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isConfigurable() {
		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		try {
			@SuppressWarnings("unchecked")
			Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
			String text = (String)t.getTransferData(DataFlavor.stringFlavor);
			if (text != null) {
				if (!text.startsWith(DEMacro.MACRO_START)) {
					showErrorMessage("The clipboard is doesn't contain a DataWarrior macro.");
					}
				else {
					ArrayList<DEMacro> macroList = (ArrayList<DEMacro>) mTableModel.getExtensionData(CompoundTableConstants.cExtensionNameMacroList);
					BufferedReader reader = new BufferedReader(new StringReader(text));
					DEMacro macro = new DEMacro(reader, macroList);
					if (macroList == null)
						macroList = new ArrayList<>();
					macroList.add(macro);
					mTableModel.setExtensionData(CompoundTableConstants.cExtensionNameMacroList, macroList);

					for (Dockable d : ((DEFrame)getParentFrame()).getMainFrame().getMainPane().getDockables())
						if (d.getContent() instanceof DEMacroEditor)
							selectMacroInEDT((DEMacroEditor)d.getContent(), macro);
					}
				}
			}
		catch (UnsupportedFlavorException ufe) {
			showErrorMessage("The clipboard is doesn't contain text data.");
			}
		catch (IOException ioe) {
			showErrorMessage(ioe.toString());
			}
		}

	private void selectMacroInEDT(final DEMacroEditor editor, final DEMacro macro) {
		if (SwingUtilities.isEventDispatchThread()) {
			editor.selectMacro(macro);
			}
		else {  // currently not used:
			try {
				SwingUtilities.invokeAndWait(() -> editor.selectMacro(macro) );
				} catch (Exception e) {}
			}
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
