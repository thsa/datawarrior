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

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DERuntimeProperties;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.DEMacro;
import com.actelion.research.datawarrior.task.DEMacroRecorder;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.CompoundTableLoader;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

public class DETaskOpenFile extends DETaskAbstractOpenFile {
	public static final String TASK_NAME = "Open File";

	private static final String PROPERTY_ASSUME_CHIRAL = "assumeChiral";
	private static final String PROPERTY_ADD_MAPPING = "addMapping";
	private static final String PROPERTY_CSV_DELIMITER = "csvDelimiter";

	private static final String[] DELIMITER_TEXT = { "Comma (default)", "Semicolon", "Vertical line" };
	private static final int[] DELIMITER_FILETYPE = { FileHelper.cFileTypeTextCommaSeparated, FileHelper.cFileTypeTextSemicolonSeparated, FileHelper.cFileTypeTextVLineSeparated };

	private DataWarrior mApplication;
	private JComboBox mComboBoxCSVDelimiter;
    private JCheckBox mCheckBoxAssumeChiralTrue,mCheckBoxAddMapping;

    public DETaskOpenFile(DataWarrior application) {
		super(application, "Open DataWarrior-, SD-, gzipped SD- or Text-File", FileHelper.cFileTypeDataWarriorCompatibleData);
		mApplication = application;
		}

    public DETaskOpenFile(DataWarrior application, String filePath) {
		super(application, "Open DataWarrior-, SD-, gzipped SD-, or Text-File", FileHelper.cFileTypeDataWarriorCompatibleData, filePath);
		mApplication = application;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public JPanel createInnerDialogContent() {
		JPanel p = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {TableLayout.PREFERRED, gap, TableLayout.PREFERRED},
							{TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };
		p.setLayout(new TableLayout(size));

		mComboBoxCSVDelimiter = new JComboBox(DELIMITER_TEXT);
		mComboBoxCSVDelimiter.setEnabled(false);
		p.add(new JLabel("CSV-File delimiter:", JLabel.RIGHT), "0,0");
		p.add(mComboBoxCSVDelimiter, "2,0");

		mCheckBoxAssumeChiralTrue = new JCheckBox("For V2000 SD-files assume molecules to be pure enantiomers");
		mCheckBoxAssumeChiralTrue.setEnabled(false);
		p.add(mCheckBoxAssumeChiralTrue, "0,2,2,2");

		mCheckBoxAddMapping = new JCheckBox("Create atom mapping for unmapped reactions (SMILES, RD-files)");
		mCheckBoxAddMapping.setEnabled(false);
		p.add(mCheckBoxAddMapping, "0,4,2,4");

		return p;
		}

	@Override
	protected void fileChanged(File file) {
		enableLocalItems();
		}

	@Override
	protected void enableItems() {
    	super.enableItems();
		enableLocalItems();
		}

	private void enableLocalItems() {
		int fileType = getFilePath() == null ? 0 : FileHelper.getFileType(getFilePath());
		mComboBoxCSVDelimiter.setEnabled((isChooseFileDuringMacro() || (fileType & FileHelper.cFileTypeTextCommaSeparated) != 0));
		mCheckBoxAssumeChiralTrue.setEnabled(isChooseFileDuringMacro() || (fileType & FileHelper.cFileTypeSD) != 0);
		mCheckBoxAddMapping.setEnabled(isChooseFileDuringMacro() || (fileType & (FileHelper.cFileTypeRD | FileHelper.cFileTypeTextAny)) != 0);
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		if (mComboBoxCSVDelimiter.isEnabled())
			configuration.setProperty(PROPERTY_CSV_DELIMITER, CompoundTableLoader.DELIMITER_STRING[mComboBoxCSVDelimiter.getSelectedIndex()]);
		if (mCheckBoxAssumeChiralTrue.isEnabled())
			configuration.setProperty(PROPERTY_ASSUME_CHIRAL, mCheckBoxAssumeChiralTrue.isSelected() ? "true" : "false");
		if (mCheckBoxAddMapping.isEnabled())
			configuration.setProperty(PROPERTY_ADD_MAPPING, mCheckBoxAddMapping.isSelected() ? "true" : "false");
		return configuration;
        }

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mComboBoxCSVDelimiter.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_CSV_DELIMITER), CompoundTableLoader.DELIMITER_STRING, 0));
		mCheckBoxAssumeChiralTrue.setSelected("true".equals(configuration.getProperty(PROPERTY_ASSUME_CHIRAL)));
		mCheckBoxAddMapping.setSelected("true".equals(configuration.getProperty(PROPERTY_ADD_MAPPING)));
		enableLocalItems();
		}

	@Override
	public void setDialogConfigurationToDefault() {
    	super.setDialogConfigurationToDefault();
    	mComboBoxCSVDelimiter.setSelectedIndex(0);
		mCheckBoxAssumeChiralTrue.setSelected(false);
		mCheckBoxAddMapping.setSelected(false);
		enableLocalItems();
		}

	@Override
	public boolean qualifiesForRecentFileMenu() {
		return true;
		}

	@Override
	public DEFrame openFile(File file, Properties configuration) {
		int type = FileHelper.getFileType(file.getName());
		if ((type & FileHelper.cFileTypeTextCommaSeparated) != 0) {
			int delimiter = findListIndex(configuration.getProperty(PROPERTY_CSV_DELIMITER), CompoundTableLoader.DELIMITER_STRING, -1);
			if (delimiter != -1)
				type = DELIMITER_FILETYPE[delimiter];
			}
		final int filetype = type;
		final DEFrame emptyFrame = mApplication.getEmptyFrame(file.getName());
		CompoundTableLoader loader = new CompoundTableLoader(emptyFrame, emptyFrame.getTableModel(), this) {
			public void finalStatus(boolean success) {
				if (success && filetype == FileHelper.cFileTypeDataWarrior) {
					emptyFrame.setDirty(false);
					SwingUtilities.invokeLater(() -> runAutoStartMacros(emptyFrame));
					}
				}
			};
		loader.addDataDependentPropertyReader(CustomLabelPositionWriter.PROPERTY_NAME, new CustomLabelPositionReader(emptyFrame));
		loader.addDataDependentPropertyReader(CardViewPositionWriter.PROPERTY_NAME, new CardViewPositionReader(emptyFrame));
		loader.setAssumeChiralFlag("true".equals(configuration.getProperty(PROPERTY_ASSUME_CHIRAL)));
		loader.setAddAtomMapping("true".equals(configuration.getProperty(PROPERTY_ADD_MAPPING)));
		loader.readFile(file, new DERuntimeProperties(emptyFrame.getMainFrame()), filetype);
		if (loader.isAssumeChiralFlag())
			configuration.setProperty(PROPERTY_ASSUME_CHIRAL, "true");
		if (loader.isAddAtomMapping())
			configuration.setProperty(PROPERTY_ADD_MAPPING, "true");
		if ((filetype & FileHelper.cFileTypeTextAnyCSV) != 0)
			configuration.setProperty(PROPERTY_CSV_DELIMITER, CompoundTableLoader.DELIMITER_STRING[loader.getCSVDelimiter()]);
		return emptyFrame;
		}

	private void runAutoStartMacros(DEFrame frame) {
		ArrayList<DEMacro> macroList = (ArrayList<DEMacro>)frame.getTableModel().getExtensionData(CompoundTableConstants.cExtensionNameMacroList);
		if (macroList != null) {
			for (DEMacro macro:macroList) {
				if (macro.isAutoStarting()) {
					DEMacroRecorder.getInstance().runMacro(macro, frame);
					}
				}
			}
		}
	}
