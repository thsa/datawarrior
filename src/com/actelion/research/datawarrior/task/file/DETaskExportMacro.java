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

import info.clearthought.layout.TableLayout;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.chem.io.CompoundFileHelper;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.DEMacro;
import com.actelion.research.table.model.CompoundTableModel;

public class DETaskExportMacro extends DETaskAbstractSaveFile {
    public static final String TASK_NAME = "Export Macro";

    private static final String PROPERTY_MACRO_NAME = "macroName";

	private String mMacroName;
	private JComboBox mComboBoxMacroName;

	public DETaskExportMacro(DEFrame parent, String macroName) {
		super(parent, "");
		mMacroName = macroName;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public String getSuggestedFileName() {
		return (mMacroName != null) ? mMacroName : "Untitled Macro";
		}

	@Override
	public int getFileType() {
		return CompoundFileHelper.cFileTypeDataWarriorMacro;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		Properties configuration = super.getPredefinedConfiguration();
		if (configuration != null) {	// interactive
			configuration.setProperty(PROPERTY_MACRO_NAME, mMacroName);
			}
		return configuration;
		}

	@Override
	public JPanel createInnerDialogContent() {
		JPanel p = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8} };
		p.setLayout(new TableLayout(size));

		@SuppressWarnings("unchecked")
		ArrayList<DEMacro> macroList = (ArrayList<DEMacro>)getTableModel().getExtensionData(CompoundTableModel.cExtensionNameMacroList);
		p.add(new JLabel("Macro name:"), "1,1");
		mComboBoxMacroName = new JComboBox();
		for (DEMacro macro:macroList)
			mComboBoxMacroName.addItem(macro.getName());
		mComboBoxMacroName.setEditable(true);
		p.add(mComboBoxMacroName, "3,1");

		return p;
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mComboBoxMacroName.setSelectedIndex(0);
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mComboBoxMacroName.setSelectedItem(configuration.getProperty(PROPERTY_MACRO_NAME));
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_MACRO_NAME, (String)mComboBoxMacroName.getSelectedItem());
		return configuration;
		}

	@Override
	public boolean isConfigurable() {
		if (getTableModel().getExtensionData(CompoundTableModel.cExtensionNameMacroList) == null) {
			showErrorMessage("No macro found.");
			return false;
			}
		return true;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String macroName = configuration.getProperty(PROPERTY_MACRO_NAME);
		if (macroName == null) {
			showErrorMessage("No macro defined.");
			return false;
			}

		if (isLive) {
			@SuppressWarnings("unchecked")
			ArrayList<DEMacro> macroList = (ArrayList<DEMacro>)getTableModel().getExtensionData(CompoundTableModel.cExtensionNameMacroList);
			boolean found = false;
			for (DEMacro macro:macroList) {
				if (macro.getName().equals(macroName)) {
					found = true;
					break;
					}
				}
			if (!found) {
				showErrorMessage("Macro '"+macroName+"' not found.");
				return false;
				}
			}
		
		return super.isConfigurationValid(configuration, isLive);
		}

	@Override
	public void saveFile(File file, Properties configuration) {
		String macroName = configuration.getProperty(PROPERTY_MACRO_NAME);
		@SuppressWarnings("unchecked")
		ArrayList<DEMacro> macroList = (ArrayList<DEMacro>)getTableModel().getExtensionData(CompoundTableModel.cExtensionNameMacroList);
		for (DEMacro macro:macroList) {
			if (macro.getName().equals(macroName)) {
				try {
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),"UTF-8"));
					macro.writeMacro(writer);
					writer.close();
					}
				catch (IOException ioe) {
					showErrorMessage("Macro '"+macroName+"' could not be written:\n"+ioe.toString());
					}
				break;
				}
			}
		}
	}
