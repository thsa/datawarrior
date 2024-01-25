package com.actelion.research.datawarrior.task.chem.dev;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.chem.DETaskAbstractFromChemistry;
import com.actelion.research.datawarrior.task.file.JFilePathLabel;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Properties;

public class DevTaskValidateIDCodes extends DETaskAbstractFromChemistry {
	public static final String TASK_NAME = "Validate ID-Codes";

	private static final String PROPERTY_IDCODE_FILE = "idcodeFile";
	private static final String PROPERTY_CHECK_ONLY = "checkOnly";
	private static final String PROPERTY_CREATE_COLUMN = "createColumn";

	private String mChemistryType;
	private JCheckBox mCheckBoxCreateColumn,mCheckBoxIDCodeFile,mCheckBoxCheckOnly;
	private JFilePathLabel mLabelIDCodeFile;
	private boolean mCreateColumn,mCheckOnly,mCheckOverwrite;
	private BufferedWriter mIDCodeWriterWriter;
	private int mMismatchCount;

	/**
	 * @param parent
	 * @param chemistryType CompoundTableModel.cColumnTypeIDCode or cColumnTypeRXNCode
	 */
	public DevTaskValidateIDCodes(DEFrame parent, String chemistryType) {
		super(parent, DESCRIPTOR_NONE, false, true);
		mChemistryType = chemistryType;
		mCheckOverwrite = true;
		}

	@Override
	protected String getColumnType() {
		return mChemistryType;
	}

	@Override
	protected String getTypeName() {
		return  CompoundTableModel.cColumnTypeRXNCode.equals(mChemistryType) ? "Reaction" : "Structure";
	}

	@Override
	protected int getDescriptorType() {
		return CompoundTableModel.cColumnTypeRXNCode.equals(mChemistryType) ?
				DescriptorConstants.DESCRIPTOR_TYPE_REACTION : DescriptorConstants.DESCRIPTOR_TYPE_MOLECULE;
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	protected int getNewColumnCount() {
		return mCreateColumn ? 1 : 0;
		}

	@Override
	public boolean hasExtendedDialogContent() {
		return true;
	}

	@Override
	public JPanel getExtendedDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {HiDPIHelper.scale(80), gap/2, TableLayout.PREFERRED},
							{TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED} };

		mCheckBoxCheckOnly = new JCheckBox("Don't update "+getTypeName()+"; check only.");
		mCheckBoxCreateColumn = new JCheckBox("Create new column with validation result");

		mCheckBoxIDCodeFile = new JCheckBox("Save non-matching ID-Codes to file:");
		mCheckBoxIDCodeFile.addActionListener(this);
		mLabelIDCodeFile = new JFilePathLabel(!isInteractive());

		JPanel ep = new JPanel();
		ep.setLayout(new TableLayout(size));
		ep.add(mCheckBoxCheckOnly, "0,0,2,0");
		ep.add(mCheckBoxCreateColumn, "0,2,2,2");
		ep.add(mCheckBoxIDCodeFile, "0,4,2,4");
		ep.add(mLabelIDCodeFile, "2,6");
		return ep;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mCheckBoxIDCodeFile) {
			if (mCheckBoxIDCodeFile.isSelected()) {
				int filetype = FileHelper.cFileTypeTextTabDelimited;
				String filename = new FileHelper(getParentFrame()).selectFileToSave(
						"Save Original And Updated ID-Codes To File", filetype, "idcode comparison");
				if (filename != null) {
					mLabelIDCodeFile.setPath(filename);
					mLabelIDCodeFile.setEnabled(true);
					mCheckOverwrite = false;
					}
				else {
					mCheckBoxIDCodeFile.setSelected(false);
					mLabelIDCodeFile.setPath(null);
					mLabelIDCodeFile.setEnabled(false);
					}
				}
			else {
				mLabelIDCodeFile.setEnabled(false);
				}
			return;
			}
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);

		mCheckBoxCheckOnly.setSelected("true".equals(configuration.getProperty(PROPERTY_CHECK_ONLY)));
		mCheckBoxCreateColumn.setSelected("true".equals(configuration.getProperty(PROPERTY_CREATE_COLUMN)));

		String value = configuration.getProperty(PROPERTY_IDCODE_FILE);
		mCheckBoxIDCodeFile.setSelected(value != null);
		mLabelIDCodeFile.setPath(value == null ? null : isFileAndPathValid(value, true, false) ? value : null);
		mLabelIDCodeFile.setEnabled(value != null);
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mCheckBoxCheckOnly.setSelected(true);
		mCheckBoxCreateColumn.setSelected(true);
		mCheckBoxIDCodeFile.setSelected(false);
		mLabelIDCodeFile.setPath("");
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();

		configuration.setProperty(PROPERTY_CHECK_ONLY, mCheckBoxCheckOnly.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_CREATE_COLUMN, mCheckBoxCreateColumn.isSelected() ? "true" : "false");

		if (mCheckBoxIDCodeFile.isSelected())
			configuration.setProperty(PROPERTY_IDCODE_FILE, mLabelIDCodeFile.getPath());

		return configuration;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String fileName = configuration.getProperty(PROPERTY_IDCODE_FILE);
		if (isLive) {
			if (fileName != null
			 && !isFileAndPathValid(fileName, true, mCheckOverwrite))
				return false;
			}

		if (fileName == null
		 && "true".equals(configuration.getProperty(PROPERTY_CHECK_ONLY))
		 && !"true".equals(configuration.getProperty(PROPERTY_CREATE_COLUMN))) {
			showErrorMessage("You either need to update ID-Code, create a result column, or create a result file.");
			return false;
			}

		return true;
		}

	@Override
	protected String getNewColumnName(int column) {
		return "IDCode Validation Result";
		}

	@Override
	protected boolean preprocessRows(Properties configuration) {
		mMismatchCount = 0;
		mCreateColumn = "true".equals(configuration.getProperty(PROPERTY_CREATE_COLUMN));
		mCheckOnly = "true".equals(configuration.getProperty(PROPERTY_CHECK_ONLY));

		String fileName = configuration.getProperty(PROPERTY_IDCODE_FILE);
		mIDCodeWriterWriter = null;
		if (fileName != null) {
			try {
				mIDCodeWriterWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resolvePathVariables(fileName)), "UTF-8"));
				mIDCodeWriterWriter.write("Original ID-Code [idcode]\tUpdated ID-Code [idcode]");
				mIDCodeWriterWriter.newLine();
				}
			catch (IOException ioe) {}
			}

		return true;
		}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol, int threadIndex) throws Exception {
		StereoMolecule mol = getChemicalStructure(row, containerMol);
		if (mol != null && mol.getAllAtoms() != 0) {
			Canonizer canonizer = new Canonizer(mol);
			String idcode = canonizer.getIDCode();
			String original = getTableModel().getTotalValueAt(row, getChemistryColumn());
			if (original.equals(idcode)) {
				if (mCreateColumn)
					getTableModel().setTotalValueAt("match", row, firstNewColumn);
				}
			else {
				if (!mCheckOnly)
					getTableModel().setTotalValueAt(idcode, row, getChemistryColumn());

				if (mCreateColumn)
					getTableModel().setTotalValueAt("mismatch", row, firstNewColumn);

				if (mIDCodeWriterWriter != null) {
					mIDCodeWriterWriter.write(original);
					mIDCodeWriterWriter.write('\t');
					mIDCodeWriterWriter.write(idcode);
					mIDCodeWriterWriter.newLine();
					}

				mMismatchCount++;
				}
			}
		else {
			if (mCreateColumn)
				getTableModel().setTotalValueAt("empty", row, firstNewColumn);
			}
		}

	@Override
	protected void postprocess(int firstNewColumn) {
		if (mIDCodeWriterWriter == null && !mCreateColumn) {
			String message = (mMismatchCount == 0) ? "No ID-Code mismatches found."
						   : mCheckOnly ? Integer.toString(mMismatchCount)+" non-matching ID-Codes found."
						   : Integer.toString(mMismatchCount)+" ID-Codes have been updated.";
			showInteractiveTaskMessage(message, JOptionPane.INFORMATION_MESSAGE);
			}

		if (!mCheckOnly)
			getTableModel().finalizeChangeChemistryColumn(getChemistryColumn(), 0, getTableModel().getTotalRowCount(), false);

		if (mIDCodeWriterWriter != null)
			try { mIDCodeWriterWriter.close(); } catch (IOException ioe) {}
		}
	}
