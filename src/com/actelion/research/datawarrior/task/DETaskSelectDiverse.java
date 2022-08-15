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

package com.actelion.research.datawarrior.task;

import com.actelion.research.chem.DiversitySelector;
import com.actelion.research.chem.SortedStringList;
import com.actelion.research.chem.descriptor.DescriptorHelper;
import com.actelion.research.chem.io.CompoundFileParser;
import com.actelion.research.chem.io.DWARFileParser;
import com.actelion.research.chem.io.SDFileParser;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TreeSet;


public class DETaskSelectDiverse extends ConfigurableTask implements ActionListener {
	static final long serialVersionUID = 0x20120309;

	private static final String PROPERTY_DESCRIPTOR_COLUMN = "descriptorColumn";
	private static final String PROPERTY_SELECTION_COUNT = "selectionCount";
	private static final String PROPERTY_FILE_NAME = "fileName";

	private static final String[] sColumnName = {"Diversity Selection Rank"};

	public static final String TASK_NAME = "Select Diverse Compounds";

	private DEFrame			 mSourceFrame;
	private CompoundTableModel  mTableModel;
	private JComboBox		   mComboBoxDescriptorColumn;
	private JTextField		  mTextFieldCount;
	private JCheckBox		   mCheckBoxAddFile;
	private File				mFile;
	private JLabel			  mLabelFileName;

	public DETaskSelectDiverse(DEFrame parent) {
		super(parent, true);
		mSourceFrame = parent;
		mTableModel = mSourceFrame.getTableModel();
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
	public boolean isConfigurable() {
		boolean descriptorFound = false;
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
			if (qualifiesAsDescriptorColumn(column)) {
				descriptorFound = true;
				break;
				}
			}

		if (!descriptorFound) {
			showErrorMessage("No chemical descriptor found.");
			return false;
			}

		return true;
		}

	@Override
	public JPanel createDialogContent() {
		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8,
								TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		content.add(new JLabel("Descriptor:"), "1,1");
		mComboBoxDescriptorColumn = new JComboBox();
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (qualifiesAsDescriptorColumn(column))
				mComboBoxDescriptorColumn.addItem(mTableModel.getColumnTitle(column));
		mComboBoxDescriptorColumn.setEditable(!isInteractive());
		content.add(mComboBoxDescriptorColumn, "3,1");

		content.add(new JLabel("No of compounds:"), "1,3");
		mTextFieldCount = new JTextField(4);
		content.add(mTextFieldCount, "3,3");

		mCheckBoxAddFile = new JCheckBox("Avoid compounds from file:");
		content.add(mCheckBoxAddFile, "1,5");
		mCheckBoxAddFile.addActionListener(this);

		mLabelFileName = new JLabel();
		content.add(mLabelFileName, "3,5");

		return content;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/chemistry.html#SelectDiverse";
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String descriptorName = configuration.getProperty(PROPERTY_DESCRIPTOR_COLUMN);
		int index1 = (descriptorName == null) ? -1 : descriptorName.lastIndexOf('[');
		int index2 = (index1 == -1) ? -1 : descriptorName.indexOf(']', index1);
		if (index1 == -1 || index2 == -1) {
			showErrorMessage("Descriptor '"+descriptorName+"' must end with '[<descriptor name>]'.");
			return false;
			}
		String shortName = descriptorName.substring(index1+1, index2);
		if (!DescriptorHelper.isBinaryFingerprint(shortName)) {
			showErrorMessage("Descriptor '"+descriptorName+"' is not a binary fingerprint.");
			return false;
			}

		if (isLive) {
			int descriptorColumn = mTableModel.findColumn(descriptorName);
			if (descriptorColumn == -1) {
				showErrorMessage("Descriptor '"+descriptorName+"' not found.");
				return false;
				}
			}

		String selectionCount = configuration.getProperty(PROPERTY_SELECTION_COUNT);
		if (selectionCount != null) {
			try {
				int selCount = Integer.parseInt(selectionCount);
				if (selCount < 1) {
					showErrorMessage("The selection count must be a positive integer");
					return false;
					}
				}
			catch (NumberFormatException nfe) {
				showErrorMessage("The selection count is not numerical");
				return false;
				}
			}

		return true;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mFile = null;
		String value = configuration.getProperty(PROPERTY_FILE_NAME);
		if (value != null) {
			mFile = new File(value);
			if (!mFile.exists())
				mFile = null;
			}
		if (mFile == null) {
			mCheckBoxAddFile.setSelected(false);
			mLabelFileName.setText("");
			}
		else {
			mCheckBoxAddFile.setSelected(true);
			mLabelFileName.setText(mFile.getName());
			}

		value = configuration.getProperty(PROPERTY_DESCRIPTOR_COLUMN, "");
		if (value.length() != 0) {
			int column = mTableModel.findColumn(value);
			if (column != -1 && qualifiesAsDescriptorColumn(column))
				mComboBoxDescriptorColumn.setSelectedItem(mTableModel.getColumnTitle(column));
			else if (!isInteractive())
				mComboBoxDescriptorColumn.setSelectedItem(value);
			else if (mComboBoxDescriptorColumn.getItemCount() != 0)
				mComboBoxDescriptorColumn.setSelectedIndex(0);
			}
		else if (!isInteractive()) {
			mComboBoxDescriptorColumn.setSelectedItem("Structure [FragFp]");
			}

		mTextFieldCount.setText(configuration.getProperty(PROPERTY_SELECTION_COUNT, "1000"));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mComboBoxDescriptorColumn.getItemCount() != 0)
			mComboBoxDescriptorColumn.setSelectedIndex(0);
		else if (!isInteractive())
			mComboBoxDescriptorColumn.setSelectedItem("Structure [FragFp]");
		mTextFieldCount.setText("1000");
		mCheckBoxAddFile.setSelected(false);
		mLabelFileName.setText("");
		mFile = null;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		if (mComboBoxDescriptorColumn.getItemCount() != 0)
			configuration.setProperty(PROPERTY_DESCRIPTOR_COLUMN, (String)mComboBoxDescriptorColumn.getSelectedItem());

		if (mTextFieldCount.getText().length() != 0)
			configuration.setProperty(PROPERTY_SELECTION_COUNT, mTextFieldCount.getText());

		if (mCheckBoxAddFile.isSelected())
			configuration.setProperty(PROPERTY_FILE_NAME, mFile.getAbsolutePath());

		return configuration;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	private boolean qualifiesAsDescriptorColumn(int column) {
		return DescriptorHelper.isBinaryFingerprint(mTableModel.getColumnSpecialType(column));
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mCheckBoxAddFile) {
			if (mCheckBoxAddFile.isSelected()) {
				mFile = FileHelper.getFile(mSourceFrame, "Select DataWarrior- or SD-File", FileHelper.cFileTypeDataWarrior | FileHelper.cFileTypeSD);
				if (mFile != null) {
					mLabelFileName.setText(mFile.getName());
					}
				else {
					mCheckBoxAddFile.setSelected(false);
					mLabelFileName.setText("");
					mFile = null;
					}
				}
			else {
				mLabelFileName.setEnabled(false);
				}
			return;
			}
		}

	private int getUniqueStructureCount(int descriptorColumn) {
		int count = 0;
		int idcodeColumn = mTableModel.getParentColumn(descriptorColumn);
		SortedStringList uniqueCompoundList = new SortedStringList();
		for (int i=0; i<mTableModel.getTotalRowCount(); i++) {
			String idcode = mTableModel.getTotalValueAt(i, idcodeColumn);
			if (idcode != null
			 && mTableModel.getTotalRecord(i).getData(descriptorColumn) != null
			 && uniqueCompoundList.addString(idcode) != -1)
				count++;
			}

		return count;
		}

	@Override
	public void runTask(Properties configuration) {
		int descriptorColumn = mTableModel.findColumn(configuration.getProperty(PROPERTY_DESCRIPTOR_COLUMN));
		int idcodeColumn = mTableModel.getParentColumn(descriptorColumn);

		int selectionCount = -1;
		try {
			selectionCount = Integer.parseInt(configuration.getProperty(PROPERTY_SELECTION_COUNT, "-1"));
			}
		catch (NumberFormatException nfe) {}		

		String fileName = configuration.getProperty(PROPERTY_FILE_NAME);

		DiversitySelector selector = new DiversitySelector();
		selector.addProgressListener(getProgressController());
		selector.setThreadMaster(getProgressController());

		TreeSet<String> uniqueCompoundList = new TreeSet<String>();

		if (fileName != null) {
			if (!new File(fileName).exists()) {
				showErrorMessage("File '"+fileName+"' not found.");
				}
			else {
				int index = fileName.lastIndexOf('.');
				String extention = (index == -1) ? "" : fileName.substring(index).toLowerCase();
				CompoundFileParser parser = (extention.equals(".sdf")) ?
													   new SDFileParser(fileName)
										  : (extention.equals(".ode") || extention.equals(".dwar")) ?
													   new DWARFileParser(fileName) : null;
	
				int records = 0;
				int errors = 0;
				String descriptorType = mTableModel.getColumnSpecialType(descriptorColumn);
	
				// TODO this should be descriptor dependent, although currently all binary descriptors have 512 bits
				selector.initializeExistingSet(512);

				if (parser != null) {
					parser.setDescriptorHandlerFactory(CompoundTableModel.getDefaultDescriptorHandlerFactory());

					int rowCount = parser.getRowCount();
					startProgress("Processing Compounds From File...", 0, (rowCount == -1) ? 0 : rowCount);
					while (parser.next()) {
						if (threadMustDie())
							break;
	
						records++;
						String idcode = parser.getIDCode();
						if (idcode != null && !uniqueCompoundList.contains(idcode)) {
							uniqueCompoundList.add(idcode);
							selector.addToExistingSet((long[])parser.getDescriptor(descriptorType));
							}
						else {
							errors++;
							}
	
						if (rowCount != -1)
							updateProgress(records);
						}
	
					if (errors != 0)
						showErrorMessage(""+errors+" of "+records+" file records could not be read and were skipped.");
					}
				}
			}

		int[] originalIndex = null;
		int[] selected = null;
		if (!threadMustDie()) {
			ArrayList<long[]> indexList = new ArrayList<long[]>();
			int uniqueStructureCount = getUniqueStructureCount(descriptorColumn);
			originalIndex = new int[uniqueStructureCount];
			for (int i=0; i<mTableModel.getTotalRowCount(); i++) {
				String idcode = mTableModel.getTotalValueAt(i, idcodeColumn);
				if (idcode != null && !uniqueCompoundList.contains(idcode)) {
					long[] features = (long[])mTableModel.getTotalRecord(i).getData(descriptorColumn);
					if (features != null) {
						uniqueCompoundList.add(idcode);
						originalIndex[indexList.size()] = i;
						indexList.add(features);
						}
					}
				}

			if (indexList.size() == 0) {
				showErrorMessage("Descriptor column does not contains descriptors.");
				}
			else {
				long[][] featureList = new long[indexList.size()][];
				for (int i=0; i<indexList.size(); i++)
					featureList[i] = indexList.get(i);
	
				int compoundsToSelect = (selectionCount == -1) ? uniqueStructureCount : selectionCount;
				selected = selector.select(indexList.toArray(new long[0][]), compoundsToSelect);
				}
			}

		if (!threadMustDie()) {
			int firstNewColumn = mTableModel.addNewColumns(sColumnName);
			if (selected != null)
				for (int i=0; i<selected.length; i++)
					mTableModel.setTotalValueAt(""+(i+1), originalIndex[selected[i]], firstNewColumn);

			mTableModel.finalizeNewColumns(firstNewColumn, getProgressController());
			}
		}
	}
