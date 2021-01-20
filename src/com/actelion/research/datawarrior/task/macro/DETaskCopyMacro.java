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
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.datawarrior.task.DEMacro;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Properties;

public class DETaskCopyMacro extends ConfigurableTask {
	public static final String TASK_NAME = "Copy Macro";

	private static final String PROPERTY_MACRO_NAME = "macroName";

	private String mMacroName;
	private CompoundTableModel mTableModel;
	private JComboBox mComboBoxMacroName;

	/**
	 * @param parentFrame
	 * @param macroName null if non-interactive; otherwise name of macro to be run
	 */
	public DETaskCopyMacro(DEFrame parentFrame, String macroName) {
		super(parentFrame, false);
		mTableModel = parentFrame.getTableModel();
		mMacroName = macroName;
		}

	@Override
	public boolean isConfigurable() {
		if (mTableModel.getExtensionData(CompoundTableModel.cExtensionNameMacroList) == null) {
			showErrorMessage("No macro found.");
			return false;
		}
		return true;
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		if (mMacroName == null)
			return null;

		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_MACRO_NAME, mMacroName);
		return configuration;
		}

	@Override
	public JComponent createDialogContent() {
		JPanel p = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
				{8, TableLayout.PREFERRED, 8} };
		p.setLayout(new TableLayout(size));

		@SuppressWarnings("unchecked")
		ArrayList<DEMacro> macroList = (ArrayList<DEMacro>)mTableModel.getExtensionData(CompoundTableModel.cExtensionNameMacroList);
		p.add(new JLabel("Macro name:"), "1,1");
		mComboBoxMacroName = new JComboBox();
		for (DEMacro macro:macroList)
			mComboBoxMacroName.addItem(macro.getName());
		mComboBoxMacroName.setEditable(true);
		p.add(mComboBoxMacroName, "3,1");

		return p;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_MACRO_NAME, (String)mComboBoxMacroName.getSelectedItem());
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mComboBoxMacroName.setSelectedItem(configuration.getProperty(PROPERTY_MACRO_NAME, ""));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mComboBoxMacroName.getItemCount() != 0)
			mComboBoxMacroName.setSelectedIndex(0);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String macroName = configuration.getProperty(PROPERTY_MACRO_NAME);
		if (macroName == null) {
			showErrorMessage("No macro name defined.");
			return false;
			}

		if (isLive) {
			@SuppressWarnings("unchecked")
			ArrayList<DEMacro> macroList = (ArrayList<DEMacro>)mTableModel.getExtensionData(CompoundTableConstants.cExtensionNameMacroList);
			DEMacro macro = null;
			if (macroList != null) {
				for (DEMacro m:macroList) {
					if (m.getName().equals(macroName)) {
						macro = m;
						break;
						}
					}
				}
			if (macro == null) {
				showErrorMessage("Macro '"+macroName+"' not found.");
				return false;
				}
			}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		String macroName = configuration.getProperty(PROPERTY_MACRO_NAME);
		@SuppressWarnings("unchecked")
		ArrayList<DEMacro> macroList = (ArrayList<DEMacro>)mTableModel.getExtensionData(CompoundTableModel.cExtensionNameMacroList);
		for (DEMacro macro:macroList) {
			if (macro.getName().equals(macroName)) {
				try {
					StringWriter sw = new StringWriter();
					BufferedWriter bw = new BufferedWriter(sw);
					macro.writeMacro(bw);
					bw.flush();
					StringSelection theData = new StringSelection(sw.toString());
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(theData, theData);
					bw.close();
					}
				catch (IOException ioe) {
					showErrorMessage("Macro '"+macroName+"' could not be written:\n"+ioe.toString());
					}
				break;
				}
			}
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
