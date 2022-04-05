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

package com.actelion.research.datawarrior.task.chem;

import com.actelion.research.chem.CanonizerUtil;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorHandler;
import com.actelion.research.chem.descriptor.DescriptorHelper;
import com.actelion.research.chem.io.*;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.datawarrior.task.file.JFilePathLabel;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.VerticalFlowLayout;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.CompoundTableSaver;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.ByteArrayComparator;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DETaskFindSimilarCompoundsInFile extends ConfigurableTask implements ActionListener,Runnable {
	static final long serialVersionUID = 0x20140205;

	private static final String PROPERTY_DESCRIPTOR_COLUMN = "descriptorColumn";
	private static final String PROPERTY_SIMILARITY = "similarity";
	private static final String PROPERTY_COLUMN_LIST = "columnList";
	private static final String PROPERTY_IN_FILE_NAME = "inFile";
	private static final String PROPERTY_SIMILAR_FILE_NAME = "similarFile";
	private static final String PROPERTY_DISSIMILAR_FILE_NAME = "dissimilarFile";
	private static final String PROPERTY_PAIR_FILE_NAME = "pairFile";
	private static final String PROPERTY_PAIR_ID1 = "pairID1";
	private static final String PROPERTY_PAIR_ID2 = "pairID2";
	private static final String PROPERTY_HALF_MATRIX = "halfMatrix";
	private static final String PROPERTY_ANY_STEREO = "anyStereo";
	private static final String PROPERTY_ANY_TAUTOMER = "anyTautomer";
	private static final String PROPERTY_ANY_SALT = "anySalt";

	private static final String EXACT_STRUCTURE_COLUMN_NAME = "First Matching Structure";
	private static final String EXACT_MATCHES_COLUMN_NAME = "Structure Matches";
	private static final String SIMILAR_STRUCTURE_COLUMN_NAME = "Most Similar Structure";
	private static final String SIMILARITY_COLUMN_NAME = "Similarity";

	private static final String[] COMPOUND_ID_OPTIONS = { "<Use row number>", "<Automatic>" };
	private static final String[] COMPOUND_ID_CODE = { "<rowNo>", "<idColumn>" };
	private static final int COMPOUND_ID_OPTION_ROW_NUMBER = 0;
	private static final int COMPOUND_ID_OPTION_AUTOMATIC = 1;

	private static final int MIN_SIMILARITY = 0;
	private static final int DEFAULT_SIMILARITY = 85;
	private static final int MAX_DESCRIPTOR_CACHE_SIZE = 100000;

	public static final String TASK_NAME = "Find Similar Compounds In Other File";

	private static final String EXACT_TEXT = " [Exact]";

	private DEFrame				mSourceFrame;
	private CompoundTableModel	mTableModel;
	private JPanel				mCardPanel;
	private JComboBox			mComboBoxDescriptorColumn,mComboBoxPairID1,mComboBoxPairID2;
	private JSlider				mSimilaritySlider;
	private JList				mListColumns;
	private JCheckBox			mCheckBoxSimilarFile,mCheckBoxDissimilarFile,mCheckBoxPairFile,mCheckBoxHalfMatrix,
			mCheckBoxAnyStereo,mCheckBoxAnyTautomer,mCheckBoxAnySalt;
	private JFilePathLabel		mLabelInFileName,mLabelSimilarFileName,mLabelDissimilarFileName,mLabelPairFileName;
	private boolean				mCheckOverwriteSim,mCheckOverwriteDissim,mCheckOverwritePairs,mBothFilesAreTheSame;

	public DETaskFindSimilarCompoundsInFile(DEFrame parent) {
		super(parent, true);
		mSourceFrame = parent;
		mTableModel = mSourceFrame.getTableModel();
		mCheckOverwriteSim = true;
		mCheckOverwriteDissim = true;
		mCheckOverwritePairs = true;
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
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap,
								TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED, TableLayout.PREFERRED, 2*gap,
								TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		JPanel inFilePanel = new JPanel();
		inFilePanel.setLayout(new BorderLayout());
		inFilePanel.add(new JLabel("File:  "), BorderLayout.WEST);
		mLabelInFileName = new JFilePathLabel(!isInteractive());
		inFilePanel.add(mLabelInFileName, BorderLayout.CENTER);
		content.add(inFilePanel, "1,1,3,1");

		JButton buttonEdit = new JButton(JFilePathLabel.BUTTON_TEXT);
		buttonEdit.addActionListener(this);
		content.add(buttonEdit, "5,1");

		content.add(new JLabel("Structure column [comparison method]:", JLabel.RIGHT), "1,3");
		mComboBoxDescriptorColumn = new JComboBox();
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (qualifiesAsDescriptorColumn(column))
				mComboBoxDescriptorColumn.addItem(mTableModel.getColumnTitle(column));
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.isColumnTypeStructure(column))
				mComboBoxDescriptorColumn.addItem(mTableModel.getColumnTitle(column)+EXACT_TEXT);
		mComboBoxDescriptorColumn.setEditable(!isInteractive());
		mComboBoxDescriptorColumn.addActionListener(this);
		content.add(mComboBoxDescriptorColumn, "3,3,5,3");

//		content.add(new JLabel("Similarity limit:", JLabel.RIGHT), "1,5");
//		content.add(createSimilaritySlider(), "3,5,5,5");

		mCheckBoxAnyStereo = new JCheckBox("Neglect stereo features");
		mCheckBoxAnyTautomer = new JCheckBox("Consider tautomers equal");
		mCheckBoxAnySalt = new JCheckBox("Consider largest fragment only");
		double[][] size1 = { {TableLayout.PREFERRED}, {TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED} };
		JPanel optionPanel = new JPanel(new TableLayout(size1));
		optionPanel.add(mCheckBoxAnyStereo, "0,0");
		optionPanel.add(mCheckBoxAnyTautomer, "0,1");
		optionPanel.add(mCheckBoxAnySalt, "0,2");

		mCardPanel = new JPanel(new CardLayout());
		mCardPanel.add(optionPanel, "options");
		mCardPanel.add(createSimilaritySlider(), "slider");
		content.add(mCardPanel, "3,5,5,5");

		JLabel listLabel = new JLabel("<html>Select columns of other file<br>to be copied into this file<br>when structures are similar:<br><br>(Press Ctrl for multiple selection)</html>");
		listLabel.setVerticalAlignment(SwingConstants.TOP);
		content.add(listLabel, "1,7");

		mListColumns = new JList();
		JScrollPane scrollPane = new JScrollPane(mListColumns);
		content.add(scrollPane, "3,7,5,7");

		mCheckBoxSimilarFile = new JCheckBox("Save similar compounds to file:");
		content.add(mCheckBoxSimilarFile, "1,9");
		mCheckBoxSimilarFile.addActionListener(this);

		mLabelSimilarFileName = new JFilePathLabel(!isInteractive());
		content.add(mLabelSimilarFileName, "3,9,5,9");

		mCheckBoxDissimilarFile = new JCheckBox("Save dissimilar compounds to file:");
		content.add(mCheckBoxDissimilarFile, "1,10");
		mCheckBoxDissimilarFile.addActionListener(this);

		mLabelDissimilarFileName = new JFilePathLabel(!isInteractive());
		content.add(mLabelDissimilarFileName, "3,10,5,10");

		mCheckBoxPairFile = new JCheckBox("Save similar compound pairs to file:");
		content.add(mCheckBoxPairFile, "1,12");
		mCheckBoxPairFile.addActionListener(this);

		mLabelPairFileName = new JFilePathLabel(!isInteractive());
		content.add(mLabelPairFileName, "3,12,5,12");

		content.add(new JLabel("Compound-ID of this dataset:", JLabel.RIGHT), "1,14");
		mComboBoxPairID1 = new JComboBox(COMPOUND_ID_OPTIONS);
		mComboBoxPairID1.setEditable(!isInteractive());
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (!mTableModel.isMultiCategoryColumn(column)
			 && mTableModel.getColumnSpecialType(column) == null)
				mComboBoxPairID1.addItem(mTableModel.getColumnTitle(column));
		content.add(mComboBoxPairID1, "3,14,5,14");

		content.add(new JLabel("Compound-ID of external file:", JLabel.RIGHT), "1,16");
		mComboBoxPairID2 = new JComboBox(COMPOUND_ID_OPTIONS);
		mComboBoxPairID2.setEditable(!isInteractive());
		content.add(mComboBoxPairID2, "3,16,5,16");

		mCheckBoxHalfMatrix = new JCheckBox("Create half matrix only");
		content.add(mCheckBoxHalfMatrix, "3,18,5,18");

		return content;
		}

	private JComponent createSimilaritySlider() {
		Hashtable<Integer,JLabel> labels = new Hashtable<Integer,JLabel>();
		labels.put(MIN_SIMILARITY, new JLabel(Integer.toString(MIN_SIMILARITY)+"%"));
		labels.put(50+MIN_SIMILARITY/2, new JLabel(Integer.toString(50+MIN_SIMILARITY/2)+"%"));
		labels.put(100, new JLabel("100%"));
		mSimilaritySlider = new JSlider(JSlider.HORIZONTAL, MIN_SIMILARITY, 100, DEFAULT_SIMILARITY);
		mSimilaritySlider.setMinorTickSpacing(5);
		mSimilaritySlider.setMajorTickSpacing(10);
		mSimilaritySlider.setLabelTable(labels);
		mSimilaritySlider.setPaintLabels(true);
		mSimilaritySlider.setPaintTicks(true);
//		mSimilaritySlider.setPreferredSize(new Dimension(120, mSimilaritySlider.getPreferredSize().height));
		JPanel spanel = new JPanel(new VerticalFlowLayout());
		spanel.add(new JLabel("Similarity limit:"));
		spanel.add(mSimilaritySlider);
		return spanel;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/chemistry.html#CompareFiles";
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String inFileName = configuration.getProperty(PROPERTY_IN_FILE_NAME);
		if (isLive && !isFileAndPathValid(inFileName, false, false))
			return false;

		int index = inFileName.lastIndexOf('.');
		String extension = (index == -1) ? "" : inFileName.substring(index+1).toLowerCase();
		if (!extension.equals("dwar") && !extension.equals("sdf")) {
			showErrorMessage("Input file is neither a DataWarrior file nor an SD-file.");
			return false;
			}

		String descriptorName = configuration.getProperty(PROPERTY_DESCRIPTOR_COLUMN, "");
		if (descriptorName.length() == 0) {
			showErrorMessage("Descriptor column not defined.");
			return false;
			}

		if (isLive) {
			if (extension.equals("dwar")) {
				DWARFileParser parser = new DWARFileParser(inFileName);
				boolean hasStructures = parser.hasStructures();
				parser.close();
				if (!hasStructures) {
					showErrorMessage("The DataWarrior input-file doesn't contain chemical structures.");
					return false;
					}
				}
	
			String similarFileName = configuration.getProperty(PROPERTY_SIMILAR_FILE_NAME);
			if (similarFileName != null) {
				if (!isFileAndPathValid(similarFileName, true, mCheckOverwriteSim))
					return false;
				index = similarFileName.lastIndexOf('.');
				if (index == -1 || !similarFileName.substring(index+1).toLowerCase().equals(extension)) {
					showErrorMessage("The file types of input file and similar compounds output file don't match.");
					return false;
					}
				}
	
			String dissimilarFileName = configuration.getProperty(PROPERTY_DISSIMILAR_FILE_NAME);
			if (dissimilarFileName != null) {
				if (!isFileAndPathValid(dissimilarFileName, true, mCheckOverwriteDissim))
					return false;
				index = dissimilarFileName.lastIndexOf('.');
				if (index == -1 || !dissimilarFileName.substring(index+1).toLowerCase().equals(extension)) {
					showErrorMessage("The file types of input file and dissimilar compounds output file don't match.");
					return false;
				}
				}

			String pairFileName = configuration.getProperty(PROPERTY_PAIR_FILE_NAME);
			if (pairFileName != null && !isFileAndPathValid(pairFileName, true, mCheckOverwritePairs))
				return false;

			if (descriptorName.endsWith(EXACT_TEXT)) {
				int structureColumn = mTableModel.findColumn(descriptorName.substring(0, descriptorName.length() - EXACT_TEXT.length()));
				if (structureColumn == -1) {
					showErrorMessage("Structure column '"+descriptorName+"' not found.");
					return false;
					}
				}
			else {
				int descriptorColumn = mTableModel.findColumn(descriptorName);
				if (descriptorColumn == -1) {
					showErrorMessage("Descriptor column '"+descriptorName+"' not found.");
					return false;
					}
				}
			}

		return true;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String value = configuration.getProperty(PROPERTY_IN_FILE_NAME);
		mLabelInFileName.setPath(value == null ? null : isFileAndPathValid(value, false, false) ? value : null);
		updateDialogFromFile(mLabelInFileName.getPath());

		value = configuration.getProperty(PROPERTY_DESCRIPTOR_COLUMN);
		if (value != null) {
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

		if (value != null && value.endsWith(EXACT_TEXT)) {
			mCheckBoxAnyStereo.setSelected(configuration.getProperty(PROPERTY_ANY_STEREO, "false").equals("true"));
			mCheckBoxAnyTautomer.setSelected(configuration.getProperty(PROPERTY_ANY_TAUTOMER, "false").equals("true"));
			mCheckBoxAnySalt.setSelected(configuration.getProperty(PROPERTY_ANY_SALT, "false").equals("true"));
			mSimilaritySlider.setValue(80);
			}
		else {
			mCheckBoxAnyStereo.setSelected(false);
			mCheckBoxAnyTautomer.setSelected(false);
			mCheckBoxAnySalt.setSelected(false);
			int similarity = DEFAULT_SIMILARITY;
			value = configuration.getProperty(PROPERTY_SIMILARITY);
			if (value != null)
				try { similarity = Math.min(100, Math.max(MIN_SIMILARITY, Integer.parseInt(value))); } catch (NumberFormatException nfe) {}
			mSimilaritySlider.setValue(similarity);
			}

		selectColumnsInList(mListColumns, configuration.getProperty(PROPERTY_COLUMN_LIST), mTableModel);
		mListColumns.clearSelection();
		String columnList = configuration.getProperty(PROPERTY_COLUMN_LIST);
		if (columnList != null) {
			for (String columnName:columnList.split("\\t")) {
				for (int i=0; i<mListColumns.getModel().getSize(); i++) {
					if (columnName.equals(mListColumns.getModel().getElementAt(i))) {
						mListColumns.addSelectionInterval(i, i);
						break;
						}
					}
				}
			}

		value = configuration.getProperty(PROPERTY_SIMILAR_FILE_NAME);
		mCheckBoxSimilarFile.setSelected(value != null);
		mLabelSimilarFileName.setPath(value == null ? null : isFileAndPathValid(value, true, false) ? value : null);
		mLabelSimilarFileName.setEnabled(value != null);

		value = configuration.getProperty(PROPERTY_DISSIMILAR_FILE_NAME);
		mCheckBoxDissimilarFile.setSelected(value != null);
		mLabelDissimilarFileName.setPath(value == null ? null : isFileAndPathValid(value, true, false) ? value : null);
		mLabelDissimilarFileName.setEnabled(value != null);

		value = configuration.getProperty(PROPERTY_PAIR_FILE_NAME);
		mCheckBoxPairFile.setSelected(value != null);
		mLabelPairFileName.setPath(value == null ? null : isFileAndPathValid(value, true, false) ? value : null);
		mLabelPairFileName.setEnabled(value != null);
		mComboBoxPairID1.setEnabled(value != null);
		mComboBoxPairID2.setEnabled(value != null);

		String idColumn1 = configuration.getProperty(PROPERTY_PAIR_ID1, COMPOUND_ID_CODE[COMPOUND_ID_OPTION_AUTOMATIC]);
		int index1 = findListIndex(idColumn1, COMPOUND_ID_CODE, -1);
		if (index1 == -1)
			mComboBoxPairID1.setSelectedItem(idColumn1);
		else
			mComboBoxPairID1.setSelectedIndex(index1);

		String idColumn2 = configuration.getProperty(PROPERTY_PAIR_ID2, COMPOUND_ID_CODE[COMPOUND_ID_OPTION_AUTOMATIC]);
		int index2 = findListIndex(idColumn2, COMPOUND_ID_CODE, -1);
		if (index2 == -1)
			mComboBoxPairID2.setSelectedItem(idColumn2);
		else
			mComboBoxPairID2.setSelectedIndex(index2);

		mCheckBoxHalfMatrix.setSelected(configuration.getProperty(PROPERTY_HALF_MATRIX, "true").equals("true"));

		showCardPanelItem();
		}

	@Override
	public void setDialogConfigurationToDefault() {
		String path = askForCompoundFile(null);
		mLabelInFileName.setPath(path);
		updateDialogFromFile(path);

		if (mComboBoxDescriptorColumn.getItemCount() != 0)
			mComboBoxDescriptorColumn.setSelectedIndex(0);
		else if (!isInteractive())
			mComboBoxDescriptorColumn.setSelectedItem("Structure [FragFp]");

		mCheckBoxAnyStereo.setSelected(false);
		mCheckBoxAnyTautomer.setSelected(false);
		mCheckBoxAnySalt.setSelected(false);
		mSimilaritySlider.setValue(DEFAULT_SIMILARITY);

		mListColumns.clearSelection();

		mCheckBoxSimilarFile.setSelected(false);
		mLabelSimilarFileName.setPath("");

		mCheckBoxDissimilarFile.setSelected(false);
		mLabelDissimilarFileName.setPath("");

		mCheckBoxPairFile.setSelected(false);
		mLabelPairFileName.setPath("");
		mLabelPairFileName.setEnabled(false);
		mComboBoxPairID1.setEnabled(false);
		mComboBoxPairID2.setEnabled(false);
		mCheckBoxHalfMatrix.setSelected(false);

		showCardPanelItem();
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		String fileName = mLabelInFileName.getPath();
		if (fileName != null)
			configuration.setProperty(PROPERTY_IN_FILE_NAME, fileName);

		String descriptorColumn = (String)mComboBoxDescriptorColumn.getSelectedItem();
		if (descriptorColumn != null)
			configuration.setProperty(PROPERTY_DESCRIPTOR_COLUMN, descriptorColumn);

		if (descriptorColumn != null && descriptorColumn.endsWith(EXACT_TEXT)) {
			configuration.setProperty(PROPERTY_ANY_STEREO, mCheckBoxAnyStereo.isSelected() ? "true" : "false");
			configuration.setProperty(PROPERTY_ANY_TAUTOMER, mCheckBoxAnyTautomer.isSelected() ? "true" : "false");
			configuration.setProperty(PROPERTY_ANY_SALT, mCheckBoxAnySalt.isSelected() ? "true" : "false");
			}
		else {
			configuration.setProperty(PROPERTY_SIMILARITY, ""+mSimilaritySlider.getValue());
			}

		List selectedColumnList = mListColumns.getSelectedValuesList();
		if (selectedColumnList.size() != 0) {
			StringBuilder sb = new StringBuilder((String)selectedColumnList.get(0));
			for (int i=1; i<selectedColumnList.size(); i++)
				sb.append('\t').append((String)selectedColumnList.get(i));
			configuration.setProperty(PROPERTY_COLUMN_LIST, sb.toString());
			}

		if (mCheckBoxSimilarFile.isSelected())
			configuration.setProperty(PROPERTY_SIMILAR_FILE_NAME, mLabelSimilarFileName.getPath());

		if (mCheckBoxDissimilarFile.isSelected())
			configuration.setProperty(PROPERTY_DISSIMILAR_FILE_NAME, mLabelDissimilarFileName.getPath());

		if (mCheckBoxPairFile.isSelected())
			configuration.setProperty(PROPERTY_PAIR_FILE_NAME, mLabelPairFileName.getPath());

		int index1 = mComboBoxPairID1.getSelectedIndex();
		configuration.setProperty(PROPERTY_PAIR_ID1, index1 < COMPOUND_ID_CODE.length ?
				COMPOUND_ID_CODE[index1] : mTableModel.getColumnTitleNoAlias((String)mComboBoxPairID1.getSelectedItem()));

		int index2 = mComboBoxPairID2.getSelectedIndex();
		configuration.setProperty(PROPERTY_PAIR_ID2, index2 < COMPOUND_ID_CODE.length ?
				COMPOUND_ID_CODE[index2] : mTableModel.getColumnTitleNoAlias((String)mComboBoxPairID2.getSelectedItem()));

		configuration.setProperty(PROPERTY_HALF_MATRIX, mCheckBoxHalfMatrix.isEnabled() && mCheckBoxHalfMatrix.isSelected() ? "true" : "false");

		return configuration;
		}

	private String askForCompoundFile(String selectedFile) {
		File file = new FileHelper(getParentFrame()).selectFileToOpen(
				"Open Compound File", FileHelper.cFileTypeSD | FileHelper.cFileTypeDataWarrior, selectedFile);
		return (file == null) ? null : file.getPath();
		}

	/**
	 * If the file is valid, show the file name in the dialog,
	 * extract the list of visible columns and update mColumnList.
	 * @param filePath or null
	 */
	private void updateDialogFromFile(String filePath) {
		boolean fileIsValid = false;

		if (filePath != null && new File(filePath).exists()) {
			String error = updateColumnList(filePath);
			if (error != null)
				showErrorMessage(error);
			else
				fileIsValid = true;
			}

		mLabelInFileName.setPath(fileIsValid ? filePath : null);

		mBothFilesAreTheSame = (filePath != null && (!isInteractive()
				|| (mTableModel.getFile() != null && mTableModel.getFile().getPath().equals(filePath))));
		mCheckBoxHalfMatrix.setEnabled(mCheckBoxPairFile.isSelected() && mBothFilesAreTheSame);
		mCheckBoxHalfMatrix.setSelected(mCheckBoxPairFile.isSelected() && mBothFilesAreTheSame);

		setOKButtonEnabled(fileIsValid);
		}

	/**
	 * Read and analyzes a compound file and updates the mColumnList
	 * @param fileName
	 * @return error message or null
	 */
	private String updateColumnList(String fileName) {
		int index = fileName.lastIndexOf('.');
		String extention = (index == -1) ? "" : fileName.substring(index).toLowerCase();

		ArrayList<String> columnList = new ArrayList<String>();

		if (extention.equals(".sdf")) {
			SDFileParser parser = new SDFileParser(fileName);
			for (String fieldName:parser.getFieldNames())
				columnList.add(fieldName);
			parser.close();
			}
		else if (extention.equals(".ode") || extention.equals(".dwar")) {
			DWARFileParser parser = new DWARFileParser(fileName);
			if (!parser.hasStructures()) {
				parser.close();
				return new File("'"+fileName).getName()+"' does not contain chemical structures.";
				}
			if (parser.getFieldNames() != null)
				for (String fieldName:parser.getFieldNames())
					columnList.add(fieldName);
			parser.close();
			}
		else {
			return new File("'"+fileName).getName()+"' is neither a DataWarrior file nor an SD-file.";
			}

		String[] itemList = columnList.toArray(new String[0]);
		Arrays.sort(itemList, new Comparator<String>() {
			public int compare(String s1, String s2) {
				return s1.compareToIgnoreCase(s2);
				}
			} );
		mListColumns.removeAll();
		mListColumns.setListData(itemList);
		mComboBoxPairID2.removeAllItems();
		for (String item: COMPOUND_ID_OPTIONS)
			mComboBoxPairID2.addItem(item);
		for (String item:itemList)
			mComboBoxPairID2.addItem(item);
		getDialog().pack();
		return null;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	private boolean qualifiesAsDescriptorColumn(int column) {
		return DescriptorHelper.isDescriptorShortName(mTableModel.getColumnSpecialType(column));
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(JFilePathLabel.BUTTON_TEXT)) {
			String path = askForCompoundFile(resolvePathVariables(mLabelInFileName.getPath()));
			if (path != null) {
				mLabelInFileName.setPath(path);
				updateDialogFromFile(path);
				}
			return;
			}

		if (e.getSource() == mComboBoxDescriptorColumn) {
			showCardPanelItem();
			return;
			}

		if (e.getSource() == mCheckBoxSimilarFile) {
			if (mCheckBoxSimilarFile.isSelected()) {
				int filetype = FileHelper.getFileType(mLabelInFileName.getPath());
				String filename = new FileHelper(getParentFrame()).selectFileToSave(
						"Save Similar Compounds To File", filetype, "Similar Compounds");
				if (filename != null) {
					mLabelSimilarFileName.setPath(filename);
					mLabelSimilarFileName.setEnabled(true);
					mCheckOverwriteSim = false;
					}
				else {
					mCheckBoxSimilarFile.setSelected(false);
					mLabelSimilarFileName.setPath(null);
					mLabelSimilarFileName.setEnabled(false);
					}
				}
			else {
				mLabelSimilarFileName.setEnabled(false);
				}
			return;
			}

		if (e.getSource() == mCheckBoxDissimilarFile) {
			if (mCheckBoxDissimilarFile.isSelected()) {
				int filetype = FileHelper.getFileType(mLabelInFileName.getPath());
				String filename = new FileHelper(getParentFrame()).selectFileToSave(
						"Save Dissimilar Compounds To File", filetype, "Dissimilar Compounds");
				if (filename != null) {
					mLabelDissimilarFileName.setPath(filename);
					mLabelDissimilarFileName.setEnabled(true);
					mCheckOverwriteDissim = false;
					}
				else {
					mCheckBoxDissimilarFile.setSelected(false);
					mLabelDissimilarFileName.setPath(null);
					mLabelDissimilarFileName.setEnabled(false);
					}
				}
			else {
				mLabelDissimilarFileName.setEnabled(false);
				}
			return;
			}

		if (e.getSource() == mCheckBoxPairFile) {
			boolean writePairFile = mCheckBoxPairFile.isSelected();
			if (writePairFile) {
				String filename = new FileHelper(getParentFrame()).selectFileToSave(
						"Save Compound Pairs To File", FileHelper.cFileTypeDataWarrior, "Compound Pairs");
				if (filename != null) {
					mLabelPairFileName.setPath(filename);
					mCheckOverwritePairs = false;
					}
				else {
					writePairFile = false;
					mCheckBoxPairFile.setSelected(false);
					mLabelPairFileName.setPath(null);
					}
				}
			mLabelPairFileName.setEnabled(writePairFile);
			mComboBoxPairID1.setEnabled(writePairFile);
			mComboBoxPairID2.setEnabled(writePairFile);
			mCheckBoxHalfMatrix.setEnabled(mCheckBoxPairFile.isSelected() && mBothFilesAreTheSame);
			mCheckBoxHalfMatrix.setSelected(mCheckBoxPairFile.isSelected() && mBothFilesAreTheSame);
			return;
			}
		}

	private void showCardPanelItem() {
		boolean isExact = ((String)mComboBoxDescriptorColumn.getSelectedItem()).endsWith(EXACT_TEXT);
		CardLayout cl = (CardLayout)(mCardPanel.getLayout());
		cl.show(mCardPanel, isExact ? "options" : "slider");
		}

	@Override
	public void runTask(Properties configuration) {
		String fileName = resolvePathVariables(configuration.getProperty(PROPERTY_IN_FILE_NAME));
		String value = configuration.getProperty(PROPERTY_DESCRIPTOR_COLUMN);
		final boolean isDescriptor = !value.endsWith(EXACT_TEXT);
		final boolean anyStereo = configuration.getProperty(PROPERTY_ANY_STEREO, "false").equals("true");
		final boolean anyTautomer = configuration.getProperty(PROPERTY_ANY_TAUTOMER, "false").equals("true");
		final boolean anySalt = configuration.getProperty(PROPERTY_ANY_SALT, "false").equals("true");
		final boolean needsHash = !isDescriptor && (anyStereo | anyTautomer | anySalt);
		final CanonizerUtil.IDCODE_TYPE hashType = anyStereo ?
				(anyTautomer ? CanonizerUtil.IDCODE_TYPE.NOSTEREO_TAUTOMER : CanonizerUtil.IDCODE_TYPE.NOSTEREO)
			  : (anyTautomer ? CanonizerUtil.IDCODE_TYPE.TAUTOMER : CanonizerUtil.IDCODE_TYPE.NORMAL);

		if (!isDescriptor)
			value = value.substring(0, value.length() - EXACT_TEXT.length());
		final int chemColumn = mTableModel.findColumn(value);
		if (isDescriptor)
			waitForDescriptor(mTableModel, chemColumn);

		if (threadMustDie())
			return;

		final long[] hash = needsHash ? new long[mTableModel.getTotalRowCount()] : null;
		if (needsHash) {
			int threadCount = Runtime.getRuntime().availableProcessors();
			final AtomicInteger smtIndex = new AtomicInteger(mTableModel.getTotalRowCount());

			startProgress("Calculating structure hashes...", 0, mTableModel.getTotalRowCount());
			Thread[] t = new Thread[threadCount];
			for (int i=0; i<threadCount; i++) {
				t[i] = new Thread("Structure hash calculator "+(i+1)) {
					public void run() {
						StereoMolecule mol = new StereoMolecule();
						int index;
						while ((index = smtIndex.decrementAndGet()) >= 0) {
							if ((index & 255) == 255)
								updateProgress(mTableModel.getTotalRowCount() - index);
							CompoundRecord record = mTableModel.getTotalRecord(index);
							hash[index] = CanonizerUtil.getHash(mTableModel.getChemicalStructure(record, chemColumn, 0, mol), hashType, anySalt);
						}
					}
				};
				t[i].start();
			}
			for (int i=0; i<threadCount; i++)
				try { t[i].join(); } catch (InterruptedException ie) {}
		}

		int intSim = DEFAULT_SIMILARITY;
		if (isDescriptor) {
			value = configuration.getProperty(PROPERTY_SIMILARITY);
			if (value != null)
				try { intSim = Math.min(100, Math.max(MIN_SIMILARITY, Integer.parseInt(value))); } catch (NumberFormatException nfe) {}
			}
		float similarityLimit = (float)intSim / 100f;

		String simFileName = configuration.getProperty(PROPERTY_SIMILAR_FILE_NAME);
		BufferedWriter simWriter = null;
		if (simFileName != null) {
			try {
				simWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resolvePathVariables(simFileName)), "UTF-8"));
				}
			catch (IOException ioe) {}
			}

		String dissimFileName = configuration.getProperty(PROPERTY_DISSIMILAR_FILE_NAME);
		BufferedWriter dissimWriter = null;
		if (dissimFileName != null) {
			try {
				dissimWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resolvePathVariables(dissimFileName)), "UTF-8"));
				}
			catch (IOException ioe) {}
			}

		String pairFileName = configuration.getProperty(PROPERTY_PAIR_FILE_NAME);
		BufferedWriter pairWriter = null;
		if (pairFileName != null) {
			try {
				pairWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resolvePathVariables(pairFileName)), "UTF-8"));
				}
			catch (IOException ioe) {}
			}

		String sourceColumnNames = configuration.getProperty(PROPERTY_COLUMN_LIST);
		String id2ColumnName = configuration.getProperty(PROPERTY_PAIR_ID2);
		int id2ColumnNameIndex = findListIndex(id2ColumnName, COMPOUND_ID_CODE, -1);

		boolean isSDF = fileName.substring(fileName.length()-4).toLowerCase().equals(".sdf");
		int dwarMode = DWARFileParser.MODE_COORDINATES_REQUIRE_2D | DWARFileParser.MODE_EXTRACT_DETAILS;
		if (simWriter != null || dissimWriter != null)
			dwarMode |= DWARFileParser.MODE_BUFFER_HEAD_AND_TAIL;
		CompoundFileParser parser = isSDF ? new SDFileParser(fileName) : new DWARFileParser(fileName, dwarMode);
		if (isSDF && (id2ColumnNameIndex == -1 || sourceColumnNames != null))
			parser = new SDFileParser(fileName, parser.getFieldNames());
		parser.setDescriptorHandlerFactory(CompoundTableModel.getDefaultDescriptorHandlerFactory());
		boolean coordsAvailable = (isSDF || ((DWARFileParser)parser).hasStructureCoordinates());

		if (!isSDF) {
			if (simFileName != null)
				writeHeadOrTail((DWARFileParser)parser, simWriter);
			if (dissimFileName != null)
				writeHeadOrTail((DWARFileParser)parser, dissimWriter);
			}

		String descriptorType = mTableModel.getColumnSpecialType(chemColumn);
		@SuppressWarnings("unchecked")
		final DescriptorHandler<Object,Object> dh = isDescriptor ? mTableModel.getDescriptorHandler(chemColumn).getThreadSafeCopy() : null;

		DWARFileCreator dwarCreator = null;
		ArrayList<PairMapEntry> pairMap = null;
		boolean halfMatrix = configuration.getProperty(PROPERTY_HALF_MATRIX, "false").equals("true");
		if (pairWriter != null) {
			pairMap = new ArrayList<>();
			dwarCreator = new DWARFileCreator(pairWriter);

			int structure1SourceColumn = isDescriptor ? mTableModel.getParentColumn(chemColumn) : chemColumn;
			int structure1DestColumn = dwarCreator.addStructureColumn("Structure 1", "ID 1");
			pairMap.add(new PairMapEntry(structure1SourceColumn, structure1DestColumn, false));

			for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
				if (mTableModel.getParentColumn(column) == structure1SourceColumn) {
					String type = mTableModel.getColumnSpecialType(column);
					if (CompoundTableConstants.cColumnType2DCoordinates.equals(type))
						pairMap.add(new PairMapEntry(column,
								dwarCreator.add2DCoordinatesColumn(structure1DestColumn), false));
					else if (CompoundTableConstants.cColumnType3DCoordinates.equals(type))
						pairMap.add(new PairMapEntry(column,
								dwarCreator.add3DCoordinatesColumn(mTableModel.getColumnTitle(column), structure1DestColumn), false));
					else if (mTableModel.isDescriptorColumn(column))
						pairMap.add(new PairMapEntry(column,
								dwarCreator.addDescriptorColumn(type, mTableModel.getDescriptorHandler(column).getVersion(), structure1DestColumn), false));
					}
				}

			String id1ColumnName = configuration.getProperty(PROPERTY_PAIR_ID1, COMPOUND_ID_CODE[COMPOUND_ID_OPTION_AUTOMATIC]);
			int index = findListIndex(id1ColumnName, COMPOUND_ID_CODE, -1);
			int id1Column = (index == COMPOUND_ID_OPTION_ROW_NUMBER) ? -1
						  : (index == COMPOUND_ID_OPTION_AUTOMATIC) ?
					mTableModel.findColumn(mTableModel.getColumnProperty(structure1SourceColumn, CompoundTableModel.cColumnPropertyRelatedIdentifierColumn))
						  : mTableModel.findColumn(id1ColumnName);
			if (id1Column == -1)
				id1Column = PairMapEntry.CONSTRUCT_ROW_NUMBER;
			pairMap.add(new PairMapEntry(id1Column,
						dwarCreator.addAlphanumericalColumn("ID 1"), false));

			int structure2DestColumn = dwarCreator.addStructureColumn("Structure 2", "ID 2");
			pairMap.add(new PairMapEntry(PairMapEntry.STRUCTURE, structure2DestColumn, true));

			if (parser instanceof DWARFileParser) {
				if (((DWARFileParser)parser).hasStructureCoordinates2D())
					pairMap.add(new PairMapEntry(PairMapEntry.COORDINATES_2D,
							dwarCreator.add2DCoordinatesColumn(structure2DestColumn), true));
				if (((DWARFileParser)parser).hasStructureCoordinates3D())
					pairMap.add(new PairMapEntry(PairMapEntry.COORDINATES_3D,
							dwarCreator.add3DCoordinatesColumn(((DWARFileParser)parser).getStructureCoordinates3DColumnName(),
									structure2DestColumn), true));
			}

			if (isDescriptor)
				pairMap.add(new PairMapEntry(PairMapEntry.DESCRIPTOR,
						dwarCreator.addDescriptorColumn(descriptorType, dh.getVersion(), structure2DestColumn), true));

			int id2Column = PairMapEntry.CONSTRUCT_ROW_NUMBER;	// default
			if (id2ColumnNameIndex == COMPOUND_ID_OPTION_ROW_NUMBER) {
				id2Column = PairMapEntry.CONSTRUCT_ROW_NUMBER;
				}
			else if (id2ColumnNameIndex == COMPOUND_ID_OPTION_AUTOMATIC) {
				if (parser instanceof DWARFileParser) {
					id2Column = PairMapEntry.AUTOMATIC_COMPOUND_ID;
					}
				}
			else {
				String[] fieldName = parser.getFieldNames();
				for (int i=0; i<fieldName.length; i++) {
					if (fieldName[i].equals(id2ColumnName)) {
						id2Column = i;
						break;
						}
					}
				}
			pairMap.add(new PairMapEntry(id2Column, dwarCreator.addAlphanumericalColumn("ID 2"), true));

			if (isDescriptor)
				dwarCreator.addAlphanumericalColumn("Similarity ("+descriptorType+")");

			try {
				dwarCreator.writeHeader(-1);
				}
			catch (IOException ioe) {
				ioe.printStackTrace();
				}
			}

		int records = 0;
		int errors = 0;

		TreeMap<String,Object> descriptorCache = null;
		if (isDescriptor || needsHash)
			descriptorCache = new TreeMap<>();

		int alphaNumColumnCount = 0;
		int[] sourceColumn = null;
		if (sourceColumnNames != null) {
			String[] sourceColumnName = sourceColumnNames.split("\\t");
			String[] parserColumnName = parser.getFieldNames();
			sourceColumn = new int[sourceColumnName.length];
			for (int i=0; i<sourceColumnName.length; i++) {
				for (int j=0; j<parserColumnName.length; j++) {
					if (sourceColumnName[i].equals(parserColumnName[j])) {
						sourceColumn[alphaNumColumnCount++] = j;
						break;
						}
					}
				}
			}

		int resultColumnCount = isDescriptor ? 2 : 1;
		int structureColumnCount = coordsAvailable ? 2 : 1;
		int firstNewColumn = mTableModel.addNewColumns(resultColumnCount+structureColumnCount+alphaNumColumnCount);
		int matchCountColumn = firstNewColumn;
		int maxSimilarityColumn = isDescriptor ? firstNewColumn + 1 : -1;
		int structureColumn = firstNewColumn + resultColumnCount;
		int firstNewAlphaNumColumn = structureColumn + structureColumnCount;

		TreeSet<String> detailReferences = new TreeSet<>();

		String matchCountColumnName = isDescriptor ? "Matches ["+intSim+"%; "+descriptorType+"]" : EXACT_MATCHES_COLUMN_NAME;
		String maxSimilarityColumnName = "Highest Similarity ["+descriptorType+"]";
		String structureColumnName = isDescriptor ? SIMILAR_STRUCTURE_COLUMN_NAME : EXACT_STRUCTURE_COLUMN_NAME
				+" S"+(anyStereo?"n":"y")+" T:"+(anyTautomer?"n":"y")+" LF:"+(anySalt?"n":"y");
		mTableModel.setColumnName(matchCountColumnName, matchCountColumn);
		if (maxSimilarityColumn != -1)
			mTableModel.setColumnName(maxSimilarityColumnName, maxSimilarityColumn);
		mTableModel.prepareStructureColumns(structureColumn, structureColumnName, coordsAvailable, false);
		for (int i=0; i<alphaNumColumnCount; i++) {
			String columnName = parser.getFieldNames()[sourceColumn[i]];
			mTableModel.setColumnName(columnName, firstNewAlphaNumColumn+i);
			if (!isSDF) {
				Properties properties = ((DWARFileParser)parser).getColumnProperties(columnName);
				if (properties != null)
					for (Object key:properties.keySet())
						mTableModel.setColumnProperty(firstNewAlphaNumColumn+i, (String)key, (String)properties.get(key));
				}
			}

		int[] matchCount = new int[mTableModel.getTotalRowCount()];
		float[] maxSimilarity = isDescriptor ? new float[mTableModel.getTotalRowCount()] : null;

		int threadCount = Runtime.getRuntime().availableProcessors();
		final float[] similarityList = (threadCount == 1) ? null : new float[mTableModel.getTotalRowCount()];

		//try {
		int rowCount = parser.getRowCount();
		startProgress("Processing Compounds From File...", 0, (rowCount == -1) ? 0 : rowCount);
		while (parser.next()) {
			if (threadMustDie())
				break;

			records++;

			String idcode = parser.getIDCode();
			if (idcode == null) {
				errors++;
				continue;
				}

			Object descriptor = null;
			if (isDescriptor || needsHash) {
				descriptorCache.get(idcode);
				if (descriptor == null) {
					descriptor = isDescriptor ? parser.getDescriptor(descriptorType)
											  : CanonizerUtil.getHash(parser.getMolecule(), hashType, anySalt);

					if (descriptor == null) {
						errors++;
						continue;
						}

					if (descriptorCache.size() < MAX_DESCRIPTOR_CACHE_SIZE)
						descriptorCache.put(idcode, descriptor);
					}
				}

			// We pre-calculate similarities on multiple threads...
			final AtomicInteger smtIndex = new AtomicInteger(mTableModel.getTotalRowCount());
			final Object _descriptor = descriptor;

			if (isDescriptor && similarityList != null) {
				Thread[] t = new Thread[threadCount];
				for (int i=0; i<threadCount; i++) {
					t[i] = new Thread(TASK_NAME+" "+(i+1)) {
						public void run() {
							int index;
							while ((index = smtIndex.decrementAndGet()) >= 0) {
								CompoundRecord record = mTableModel.getTotalRecord(index);
								similarityList[index] = 0;
								similarityList[index] = dh.getSimilarity(_descriptor, record.getData(chemColumn));
								}
							}
						};
					t[i].start();
					}
				for (int i=0; i<threadCount; i++)
					try { t[i].join(); } catch (InterruptedException ie) {}
				}

			boolean isSimilar = false;
			byte[] idcodeBytes = (isDescriptor || needsHash) ? null : idcode.getBytes();

			for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
				CompoundRecord record = mTableModel.getTotalRecord(row);

				float similarity = 0;
				if (isDescriptor) {
					similarity = (similarityList != null) ? similarityList[row]
							: dh.getSimilarity(descriptor, record.getData(chemColumn));
					}
				else if (needsHash) {
					if (hash[row] == (long)descriptor)
						similarity = 1.0f;
					}
				else {
					if ((new ByteArrayComparator()).compare(idcodeBytes, (byte[])record.getData(chemColumn)) == 0)
						similarity = 1.0f;
					}

				if (similarity >= similarityLimit) {
					if (pairWriter != null && (!halfMatrix || row+1 < records)) {
						try {
							for (PairMapEntry pme : pairMap) {
								if (pme.isFromExternalFile) {
									String cellValue = null;
									switch (pme.sourceColumn) {
									case PairMapEntry.STRUCTURE:
										cellValue = parser.getIDCode();
										break;
									case PairMapEntry.COORDINATES_2D:
										cellValue = ((DWARFileParser)parser).getCoordinates2D();
										break;
									case PairMapEntry.COORDINATES_3D:
										cellValue = ((DWARFileParser)parser).getCoordinates3D();
										break;
									case PairMapEntry.DESCRIPTOR:
										cellValue = dh.encode(descriptor);
										break;
									case PairMapEntry.CONSTRUCT_ROW_NUMBER:
										cellValue = Integer.toString(records);
										break;
									case PairMapEntry.AUTOMATIC_COMPOUND_ID:
										cellValue = parser.getMoleculeName();
										break;
									default:
										cellValue = CompoundTableSaver.convertNewlines(parser.getFieldData(pme.sourceColumn));
										break;
										}
									if (cellValue != null)
										pairWriter.write(cellValue);
									}
								else {
									switch (pme.sourceColumn) {
									case PairMapEntry.CONSTRUCT_ROW_NUMBER:
										pairWriter.write(Integer.toString(row + 1));
										break;
									default:
										pairWriter.write(mTableModel.encodeData(record, pme.sourceColumn));
										break;
										}
									}
								pairWriter.write("\t");
								}
							pairWriter.write(DoubleFormat.toString(similarity));
							pairWriter.newLine();
							}
						catch (IOException ioe) {}
						}

					boolean replace = matchCount[row] == 0 || (isDescriptor && similarity > maxSimilarity[row]);

					matchCount[row]++;

					if (maxSimilarityColumn != -1
					 && maxSimilarity[row] < similarity)
						maxSimilarity[row] = similarity;

					if (replace) {
						String _idcode = parser.getIDCode();
						if (_idcode != null) {
							record.setData(_idcode.getBytes(), structureColumn);
							if (coordsAvailable) {
								String coords = parser.getCoordinates();
								if (coords != null)
									record.setData(coords.getBytes(), structureColumn+1);
								}
							}
						for (int i=0; i<alphaNumColumnCount; i++) {
							String fieldData = parser.getFieldData(sourceColumn[i]);
							int destColumn = firstNewAlphaNumColumn+i;
							record.setData(fieldData == null ? null : fieldData.getBytes(), destColumn);
							if (!isSDF)
								mTableModel.getDetailHandler().extractEmbeddedDetailReferences(destColumn, fieldData, detailReferences);
							}
						}

					isSimilar = true;
					}
				}

			if (simWriter != null && isSimilar)
				writeRecord(isSDF, parser, simWriter);
			if (dissimWriter != null && !isSimilar)
				writeRecord(isSDF, parser, dissimWriter);

			updateProgress(records);
			}
//} catch (Exception e) { e.printStackTrace(); }

		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			CompoundRecord record = mTableModel.getRecord(row);
			record.setData(Integer.toString(matchCount[row]).getBytes(), matchCountColumn);
			if (maxSimilarityColumn != -1 && maxSimilarity[row] != 0)
				record.setData(DoubleFormat.toString(maxSimilarity[row]).getBytes(), maxSimilarityColumn);
			}

		if (!threadMustDie() && !isSDF && detailReferences.size() != 0) {
			resolveDetailIDCollisions(detailReferences);
			HashMap<String,byte[]> details = ((DWARFileParser)parser).getDetails();
			for (String key:detailReferences)
				mTableModel.getDetailHandler().setEmbeddedDetail(key, details.get(key));
			}

		if (!threadMustDie()) {
			if (!isSDF) {
				if (simFileName != null)
					writeHeadOrTail((DWARFileParser)parser, simWriter);
				if (dissimFileName != null)
					writeHeadOrTail((DWARFileParser)parser, dissimWriter);
				}
			}

		if (simWriter != null)
			try { simWriter.close(); } catch (IOException ioe) {}
		if (dissimWriter != null)
			try { dissimWriter.close(); } catch (IOException ioe) {}
		if (pairWriter != null)
			try { pairWriter.close(); } catch (IOException ioe) {}

		if (errors != 0)
			showErrorMessage(""+errors+" of "+records+" file records could not be processed and were skipped.");

		mTableModel.finalizeNewColumns(firstNewColumn, getProgressController());
		}

	private void resolveDetailIDCollisions(TreeSet<String> detailReferences) {
		if (mTableModel.getDetailHandler().getEmbeddedDetailCount() != 0) {
						// Existing data as well a new data have embedded details.
						// Adding an offset to the IDs of existing details ensures collision-free merging/appending.
			int highID = 0;
			for (String key:detailReferences) {
				try {
					int id = Math.abs(Integer.parseInt(key));
					if (highID < id)
						highID = id;
					}
				catch (NumberFormatException nfe) {}
				}

			if (highID != 0)
				mTableModel.addOffsetToEmbeddedDetailIDs(highID);
			}
		}

	private void insertBytes(CompoundRecord record, int column, byte[] bytes, int index) {
		// convert cLineSeparators into cEntrySeparators
		int lineCount = 0;
		for (byte b:bytes)
			if (b == CompoundTableConstants.cLineSeparatorByte)
				lineCount++;
		if (lineCount != 0) {
			byte[] old = bytes;
			bytes = new byte[old.length+(lineCount*(CompoundTableConstants.cEntrySeparatorBytes.length-1))];
			int i = 0;
			for (byte b:old) {
				if (b == CompoundTableConstants.cLineSeparatorByte)
					for (byte sb:CompoundTableConstants.cEntrySeparatorBytes)
						bytes[i++] = sb;
				else
					bytes[i++] = b;
				}
			}

		if (record.getData(column) == null) {
			record.setData(bytes, column);
			return;
			}

		byte[] detailSeparator = mTableModel.getDetailSeparator(column).getBytes();

		byte[] oldBytes = (byte[])record.getData(column);
		byte[] newBytes = new byte[oldBytes.length+1+bytes.length];
		int oldLength = getLengthWithoutDetail(oldBytes, detailSeparator);
		int length = getLengthWithoutDetail(bytes, detailSeparator);
		int i = 0;
		int entryIndex = 0;
		for (int j=0; j<oldLength; j++) {
			if (entryIndex == index) {	// we need to insert
				for (int k=0; k<length; k++)
					newBytes[i++] = bytes[k];
				newBytes[i++] = CompoundTableConstants.cLineSeparatorByte;
				entryIndex++;
				}
			newBytes[i++] = oldBytes[j];
			if (oldBytes[j] == CompoundTableConstants.cLineSeparatorByte)
				entryIndex++;
			}
		if (entryIndex < index) {	// we need to append
			newBytes[i++] = CompoundTableConstants.cLineSeparatorByte;
			for (int k=0; k<length; k++)
				newBytes[i++] = bytes[k];
			}

		// attach detail references to the end
		for (int j=oldLength; j<oldBytes.length; j++)
			newBytes[i++] = oldBytes[j];
		for (int k=length; k<bytes.length; k++)
			newBytes[i++] = bytes[k];

		record.setData(newBytes, column);
		}

	private int getLengthWithoutDetail(byte[] data, byte[] separator) {
		for (int i=0; i<=data.length-separator.length; i++) {
			int j=0;
			while (j<separator.length && data[i+j] == separator[j])
				j++;
			if (j == separator.length)
				return i;
			}
		return data.length;
		}

	private void writeHeadOrTail(DWARFileParser parser, BufferedWriter writer) {
		try {
			for (String line:parser.getHeadOrTail()) {
				if (!line.startsWith("<"+CompoundTableConstants.cNativeFileRowCount+"=")) {
					writer.write(line);
					writer.newLine();
					}
				}
			}
		catch (IOException ioe) {}
		}

	private void writeRecord(boolean isSDF, CompoundFileParser parser, BufferedWriter writer) {
		if (isSDF) {
			try {
				writer.write(((SDFileParser)parser).getNextMolFile());
				writer.write(((SDFileParser)parser).getNextFieldData());
				}
			catch (IOException ioe) {}
			}
		else {
			try {
				writer.write(((DWARFileParser)parser).getRow());
				writer.newLine();
				}
			catch (IOException ioe) {}
			}
		}
	}

class PairMapEntry {
	public static final int STRUCTURE = -1;
	public static final int COORDINATES_2D = -2;
	public static final int COORDINATES_3D = -3;
	public static final int DESCRIPTOR = -4;
	public static final int CONSTRUCT_ROW_NUMBER = -5;
	public static final int AUTOMATIC_COMPOUND_ID = -6;

	int sourceColumn,destColumn;
	boolean isFromExternalFile;

	public PairMapEntry(int sourceColumn, int destColumn, boolean isFromExternalFile) {
		this.sourceColumn = sourceColumn;
		this.destColumn = destColumn;
		this.isFromExternalFile = isFromExternalFile;
	}
}