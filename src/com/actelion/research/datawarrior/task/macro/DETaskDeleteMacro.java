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
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Properties;

public class DETaskDeleteMacro extends ConfigurableTask {
	public static final String TASK_NAME = "Delete Macro";

	private static final String PROPERTY_MACRO_NAME = "macroName";

	private final DEFrame mParentFrame;
	private final String mMacroName;
	private final CompoundTableModel mTableModel;
	private JComboBox<String> mComboBoxMacroName;

	/**
	 * @param parentFrame
	 * @param macroName null if non-interactive; otherwise name of macro to be run
	 */
	public DETaskDeleteMacro(DEFrame parentFrame, String macroName) {
		super(parentFrame, false);
		mParentFrame = parentFrame;
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
		int gap = HiDPIHelper.scale(8);
		JPanel p = new JPanel();
		double[][] size = { {gap, TableLayout.PREFERRED, gap>>1, TableLayout.PREFERRED, gap},
				{gap, TableLayout.PREFERRED, gap} };
		p.setLayout(new TableLayout(size));

		@SuppressWarnings("unchecked")
		ArrayList<DEMacro> macroList = (ArrayList<DEMacro>)mTableModel.getExtensionData(CompoundTableModel.cExtensionNameMacroList);
		p.add(new JLabel("Macro name:"), "1,1");
		mComboBoxMacroName = new JComboBox<>();
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
		if (macroList == null) {
			showErrorMessage("Macro '"+macroName+"' couldn't be deleted, because the current window doesn't contain any macros.");
			return;
		}

		DEMacro theMacro = null;
		for (DEMacro macro:macroList) {
			if (macro.getName().equals(macroName)) {
				theMacro = macro;
				break;
				}
			}
		if (theMacro == null) {
			showErrorMessage("Macro '"+macroName+"' was not be deleted, because it couldn't be found.");
			return;
			}

		macroList.remove(theMacro);

		// to trigger change events and update the macro lists in the menu
		mParentFrame.getTableModel().setExtensionData(CompoundTableConstants.cExtensionNameMacroList, macroList);
		mParentFrame.setDirty(true);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
	}
}
