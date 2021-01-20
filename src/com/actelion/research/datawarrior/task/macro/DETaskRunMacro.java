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

import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.datawarrior.task.DEMacro;
import com.actelion.research.datawarrior.task.DEMacroRecorder;
import info.clearthought.layout.TableLayout;

import java.util.ArrayList;
import java.util.Properties;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEMacroEditor;
import com.actelion.research.table.view.CompoundTableView;

public class DETaskRunMacro extends ConfigurableTask implements GenericTaskRunMacro {
	public static final String TASK_NAME = "Run Macro";

	private static final String PROPERTY_MACRONAME = "macroName";

	private DEFrame		mParentFrame;
	private String		mMacroName;
	private JComboBox	mComboBox;

	/**
	 * @param parentFrame
	 * @param macroName null if non-interactive; otherwise name of macro to be run
	 */
	public DETaskRunMacro(DEFrame parentFrame, String macroName) {
		super(parentFrame, false);
		mParentFrame = parentFrame;
		mMacroName = macroName;
		}

	@Override
	public boolean isConfigurable() {
		@SuppressWarnings("unchecked")
		ArrayList<DEMacro> macroList = (ArrayList<DEMacro>)mParentFrame.getTableModel().getExtensionData(CompoundTableConstants.cExtensionNameMacroList);
		if (macroList == null || macroList.size() == 0) {
			showErrorMessage("This DataWarrior document does not contain any macros.");
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
		if (mMacroName != null) {
			Properties configuration = new Properties();
			configuration.setProperty(PROPERTY_MACRONAME, mMacroName);
			return configuration;
			}
		return null;
		}

	@Override
	public JComponent createDialogContent() {
		JPanel p = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
		        			{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED} };
        p.setLayout(new TableLayout(size));

        DEMacro parentMacro = null;
		CompoundTableView view = mParentFrame.getMainFrame().getMainPane().getSelectedView();
		if (view != null && view instanceof DEMacroEditor)
			parentMacro = ((DEMacroEditor)view).getCurrentMacro();

        p.add(new JLabel("Macro name:"), "1,1");
        mComboBox = new JComboBox();
		@SuppressWarnings("unchecked")
		ArrayList<DEMacro> macroList = (ArrayList<DEMacro>)mParentFrame.getTableModel().getExtensionData(CompoundTableConstants.cExtensionNameMacroList);
		if (macroList != null)
			for (DEMacro macro:macroList)
				if (macro != parentMacro)
					mComboBox.addItem(macro.getName());

        mComboBox.setEditable(true);
        p.add(mComboBox, "3,1");

        return p;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_MACRONAME, (String)mComboBox.getSelectedItem());
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mComboBox.setSelectedItem(configuration.getProperty(PROPERTY_MACRONAME, ""));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mComboBox.getItemCount() != 0)
			mComboBox.setSelectedIndex(0);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String macroName = configuration.getProperty(PROPERTY_MACRONAME);
		if (macroName == null) {
			showErrorMessage("No macro name defined.");
			return false;
			}

		if (isLive) {
			@SuppressWarnings("unchecked")
			ArrayList<DEMacro> macroList = (ArrayList<DEMacro>)mParentFrame.getTableModel().getExtensionData(CompoundTableConstants.cExtensionNameMacroList);
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
			if (macro.isEmpty()) {
				showErrorMessage("Macro '"+macroName+"' does not have any tasks.");
				return false;
				}
			if (DEMacroRecorder.getInstance().isRecording(macro)) {
				showErrorMessage("Macro '"+macroName+"' is currently recording tasks.");
				return false;
				}
			}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		DEMacroRecorder.getInstance().runMacro(getMacro(configuration), mParentFrame);
		}

	@Override
	public DEMacro getMacro(Properties configuration) {
		if (!isConfigurationValid(configuration, true))
			return null;

		String macroName = configuration.getProperty(PROPERTY_MACRONAME);
		@SuppressWarnings("unchecked")
		ArrayList<DEMacro> macroList = (ArrayList<DEMacro>)mParentFrame.getTableModel().getExtensionData(CompoundTableConstants.cExtensionNameMacroList);
		for (DEMacro macro:macroList)
			if (macro.getName().equals(macroName))
				return macro;

		return null;
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
