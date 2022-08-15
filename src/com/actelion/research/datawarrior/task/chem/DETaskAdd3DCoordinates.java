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

import com.actelion.research.chem.*;
import com.actelion.research.chem.conf.TorsionDB;
import com.actelion.research.chem.conf.TorsionDescriptor;
import com.actelion.research.chem.conf.TorsionDescriptorHelper;
import com.actelion.research.chem.forcefield.mmff.ForceFieldMMFF94;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.FXConformerDialog;
import com.actelion.research.datawarrior.task.file.JFilePathLabel;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;
import org.openmolecules.chem.conf.gen.ConformerGenerator;
import org.openmolecules.chem.conf.gen.RigidFragmentCache;
import org.openmolecules.chem.conf.so.ConformationSelfOrganizer;
import org.openmolecules.chem.conf.so.SelfOrganizedConformer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.concurrent.SynchronousQueue;


public class DETaskAdd3DCoordinates extends DETaskAbstractFromStructure {
	public static final String TASK_NAME = "Generate Conformers";

	private static final String PROPERTY_ALGORITHM = "algorithm";
	private static final String PROPERTY_TORSION_SOURCE = "torsionSource";
	private static final String PROPERTY_MINIMIZE = "minimize";
	private static final String PROPERTY_FILE_NAME = "fileName";
	private static final String PROPERTY_FILE_TYPE = "fileType";
	private static final String PROPERTY_POOL_CONFORMERS = "poolConformers";
	private static final String PROPERTY_MAX_CONFORMERS = "maxConformers";
	private static final String PROPERTY_LARGEST_FRAGMENT = "largestFragment";
	private static final String PROPERTY_NEUTRALIZE_FRAGMENT = "neutralize";
	private static final String PROPERTY_PROTONATION_PH = "protonationPH";
	private static final String PROPERTY_PROTONATION_SPAN = "protonationSpan";
	private static final String PROPERTY_STEREO_ISOMER_LIMIT = "stereoIsomerLimit";

	private static final String[] TORSION_SOURCE_TEXT = { "From crystallographic database", "Use 60 degree steps" };
	private static final String[] TORSION_SOURCE_CODE = { "crystallDB", "6steps" };
	private static final int TORSION_SOURCE_CRYSTAL_DATA = 0;
	private static final int TORSION_SOURCE_6_STEPS = 1;
	private static final int DEFAULT_TORSION_SOURCE = TORSION_SOURCE_CRYSTAL_DATA;

	private static final String[] MINIMIZE_TEXT = { "MMFF94s+ forcefield", "MMFF94s forcefield", "Don't minimize" };
	private static final String[] MINIMIZE_CODE = { "mmff94+", "mmff94", "none" };
	private static final String[] MINIMIZE_TITLE = { "mmff94s+", "mmff94s", "not minimized" };
	private static final int MINIMIZE_MMFF94sPlus = 0;
	private static final int MINIMIZE_MMFF94s = 1;
	private static final int MINIMIZE_NONE = 2;
	private static final int DEFAULT_MINIMIZATION = MINIMIZE_MMFF94sPlus;
	private static final String DEFAULT_MAX_CONFORMERS_EXPORT = "16";
	private static final String DEFAULT_MAX_CONFORMERS_IN_TABLE = "1";
	private static final int MAX_CONFORMERS = 1024;
	private static final String DEFAULT_MAX_STEREO_ISOMERS = "16";

	private static final int LOW_ENERGY_RANDOM = 0;
	private static final int PURE_RANDOM = 1;
	private static final int ADAPTIVE_RANDOM = 2;
	private static final int SYSTEMATIC = 3;
	private static final int SELF_ORGANIZED = 4;
	private static final int DEFAULT_ALGORITHM = LOW_ENERGY_RANDOM;
	private static final int FILE_TYPE_NONE = -1;

	private static final String[] ALGORITHM_TEXT = { "Random, low energy bias", "Pure random", "Adaptive collision avoidance, low energy bias", "Systematic, low energy bias", "Self-organized" };
	private static final String[] ALGORITHM_CODE = { "lowEnergyRandom", "pureRandom", "adaptiveRandom", "systematic", "selfOrganized" };
	private static final boolean[] ALGORITHM_NEEDS_TORSIONS = { true, true, true, true, false };

	private static final String[] FILE_TYPE_TEXT = { "DataWarrior", "SD-File Version 2", "SD-File Version 3" };
	private static final String[] FILE_TYPE_CODE = { "dwar", "sdf2", "sdf3" };
	private static final int[] FILE_TYPE = { FileHelper.cFileTypeDataWarrior, FileHelper.cFileTypeSDV2, FileHelper.cFileTypeSDV3 };

	private JComboBox			mComboBoxAlgorithm,mComboBoxTorsionSource,mComboBoxMinimize,mComboBoxFileType;
	private JCheckBox			mCheckBoxExportFile,mCheckBoxPoolConformers,mCheckBoxLargestFragment,mCheckBoxNeutralize,mCheckBoxSkip,mCheckBoxProtonate;
	private JFilePathLabel		mLabelFileName;
	private JTextField			mTextFieldMaxCount,mTextFieldSkip,mTextFieldPH,mTextFieldPHSpan;
	private JButton				mButtonEdit;
	private int                 mCacheSizeAtStart;
	private long                mStartMillis;
	private volatile boolean	mCheckOverwrite,mPoolConformers;
	private volatile boolean	mLargestFragmentOnly,mNeutralizeLargestFragment;
	private volatile float		mPH1,mPH2;
	private volatile int		mAlgorithm,mMinimization,mTorsionSource,mMinimizationErrors,mFileType,mMaxConformers,
								mStereoIsomerLimit,mIdentifierColumn;
	private volatile BufferedWriter mFileWriter;
	private volatile Map<String,Object> mMMFFOptions;
	private volatile SynchronousQueue<String> mRowQueue;

	public DETaskAdd3DCoordinates(DEFrame parent) {
		super(parent, DESCRIPTOR_NONE, false, true);
		mCheckOverwrite = true;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/conformers.html#Generate";
		}

	@Override
	public boolean hasExtendedDialogContent() {
		return true;
		}

	@Override
	public JPanel getExtendedDialogContent() {
		JPanel ep = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, TableLayout.FILL, TableLayout.PREFERRED},
							{TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED,
							3*gap, TableLayout.PREFERRED,
							gap/2, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED,
							gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED} };
		ep.setLayout(new TableLayout(size));
		ep.add(new JLabel("Algorithm:"), "0,0");
		mComboBoxAlgorithm = new JComboBox(ALGORITHM_TEXT);
		mComboBoxAlgorithm.addActionListener(this);
		ep.add(mComboBoxAlgorithm, "2,0,4,0");
		ep.add(new JLabel("Initial torsions:"), "0,2");
		mComboBoxTorsionSource = new JComboBox(TORSION_SOURCE_TEXT);
		ep.add(mComboBoxTorsionSource, "2,2,4,2");
		ep.add(new JLabel("Minimize energy:"), "0,4");
		mComboBoxMinimize = new JComboBox(MINIMIZE_TEXT);
		ep.add(mComboBoxMinimize, "2,4,4,4");

		ep.add(new JLabel("Max. conformer count:"), "0,6");
		mTextFieldMaxCount = new JTextField();
		ep.add(mTextFieldMaxCount, "2,6");
		ep.add(new JLabel(" per stereo isomer"), "3,6,4,6");

		mCheckBoxExportFile = new JCheckBox("Write into file:");
		ep.add(mCheckBoxExportFile, "0,8");
		mCheckBoxExportFile.addActionListener(this);

		mLabelFileName = new JFilePathLabel(!isInteractive());
		ep.add(mLabelFileName, "2,8,3,8");

		mButtonEdit = new JButton("Edit");
		mButtonEdit.addActionListener(this);
		ep.add(mButtonEdit, "4,8");

		ep.add(new JLabel("File type:"), "0,10");
		mComboBoxFileType = new JComboBox(FILE_TYPE_TEXT);
		mComboBoxFileType.addActionListener(this);
		ep.add(mComboBoxFileType, "2,10");

		mCheckBoxPoolConformers = new JCheckBox("Pool conformers of same compound");
		mCheckBoxPoolConformers.addActionListener(this);
		ep.add(mCheckBoxPoolConformers, "2,12,4,12");

		mCheckBoxLargestFragment = new JCheckBox("Remove small fragments");
		mCheckBoxLargestFragment.addActionListener(this);
		ep.add(mCheckBoxLargestFragment, "2,14,4,14");

		mCheckBoxNeutralize = new JCheckBox("Neutralize remaining fragment");
		mCheckBoxNeutralize.addActionListener(this);
		ep.add(mCheckBoxNeutralize, "2,16,4,16");

		mCheckBoxSkip = new JCheckBox("Skip compounds with more than ");
		mCheckBoxSkip.addActionListener(this);
		mTextFieldSkip = new JTextField(2);
		JPanel skipPanel = new JPanel();
		skipPanel.add(mCheckBoxSkip);
		skipPanel.add(mTextFieldSkip);
		skipPanel.add(new JLabel(" stereo isomers"));
		ep.add(skipPanel, "0,18,4,18");

		mCheckBoxProtonate = new JCheckBox("Create proper protonation state(s) for pH=");
		mCheckBoxProtonate.addActionListener(this);
		mTextFieldPH = new JTextField(2);
		mTextFieldPHSpan = new JTextField(2);
		JPanel protonationPanel = new JPanel();
		protonationPanel.add(mCheckBoxProtonate);
		protonationPanel.add(mTextFieldPH);
		protonationPanel.add(new JLabel(" +-"));
		protonationPanel.add(mTextFieldPHSpan);
		ep.add(protonationPanel, "0,20,4,20");

		return ep;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mComboBoxAlgorithm) {
			mComboBoxTorsionSource.setEnabled(ALGORITHM_NEEDS_TORSIONS[mComboBoxAlgorithm.getSelectedIndex()]);
			return;
			}
		if (e.getSource() == mButtonEdit) {
			String filename = new FileHelper(getParentFrame()).selectFileToSave(
					"Write Conformers To File", FILE_TYPE[mComboBoxFileType.getSelectedIndex()], "conformers");
			if (filename != null) {
				mLabelFileName.setPath(filename);
				mCheckOverwrite = false;
				}
			}
		if (e.getSource() == mCheckBoxExportFile) {
			if (mCheckBoxExportFile.isSelected()) {
				if (mLabelFileName.getPath() == null) {
					String filename = new FileHelper(getParentFrame()).selectFileToSave(
							"Write Conformers To File", FILE_TYPE[mComboBoxFileType.getSelectedIndex()], "conformers");
					if (filename != null) {
						mLabelFileName.setPath(filename);
						mCheckOverwrite = false;
						}
					else {
						mCheckBoxExportFile.setSelected(false);
						mLabelFileName.setPath(null);
						}
					}
				}
			mTextFieldMaxCount.setText(mCheckBoxExportFile.isSelected() ? DEFAULT_MAX_CONFORMERS_EXPORT : DEFAULT_MAX_CONFORMERS_IN_TABLE);
			enableItems();
			return;
			}
		if (e.getSource() == mCheckBoxLargestFragment) {
			enableItems();
			}
		if (e.getSource() == mCheckBoxNeutralize) {
			enableItems();
			}
		if (e.getSource() == mCheckBoxSkip) {
			enableItems();
			}
		if (e.getSource() == mCheckBoxProtonate) {
			enableItems();
			}
		if (e.getSource() == mComboBoxFileType) {
			String filePath = mLabelFileName.getPath();
			if (filePath != null) {
				mLabelFileName.setPath(FileHelper.removeExtension(filePath)
						+ FileHelper.getExtension(FILE_TYPE[mComboBoxFileType.getSelectedIndex()]));
				}
			enableItems();
			return;
			}

		super.actionPerformed(e);
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_ALGORITHM, ALGORITHM_CODE[mComboBoxAlgorithm.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_TORSION_SOURCE, TORSION_SOURCE_CODE[mComboBoxTorsionSource.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_MINIMIZE, MINIMIZE_CODE[mComboBoxMinimize.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_MAX_CONFORMERS, mTextFieldMaxCount.getText());

		if (mCheckBoxExportFile.isSelected()) {
			configuration.setProperty(PROPERTY_FILE_NAME, mLabelFileName.getPath());
			configuration.setProperty(PROPERTY_FILE_TYPE, FILE_TYPE_CODE[mComboBoxFileType.getSelectedIndex()]);
			configuration.setProperty(PROPERTY_LARGEST_FRAGMENT, mCheckBoxLargestFragment.isSelected()?"true":"false");
			configuration.setProperty(PROPERTY_NEUTRALIZE_FRAGMENT, mCheckBoxNeutralize.isSelected()?"true":"false");
			if (mComboBoxFileType.getSelectedIndex() == 0)
				configuration.setProperty(PROPERTY_POOL_CONFORMERS, mCheckBoxPoolConformers.isSelected()?"true":"false");
			if (mCheckBoxSkip.isSelected()) {
				configuration.setProperty(PROPERTY_STEREO_ISOMER_LIMIT, mTextFieldSkip.getText());
				}
			if (mCheckBoxProtonate.isSelected()) {
				configuration.setProperty(PROPERTY_PROTONATION_PH, mTextFieldPH.getText());
				if (mTextFieldPHSpan.getText().length() != 0 && !mTextFieldPHSpan.getText().equals("0"))
					configuration.setProperty(PROPERTY_PROTONATION_SPAN, mTextFieldPHSpan.getText());
				}
			}

		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mComboBoxAlgorithm.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_ALGORITHM), ALGORITHM_CODE, DEFAULT_ALGORITHM));
		mComboBoxTorsionSource.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_TORSION_SOURCE), TORSION_SOURCE_CODE, DEFAULT_TORSION_SOURCE));
		mComboBoxMinimize.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_MINIMIZE), MINIMIZE_CODE, DEFAULT_MINIMIZATION));

		String value = configuration.getProperty(PROPERTY_FILE_NAME);
		mCheckBoxExportFile.setSelected(value != null);
		mLabelFileName.setPath(value == null ? null : isFileAndPathValid(value, true, false) ? value : null);

		mComboBoxFileType.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_FILE_TYPE), FILE_TYPE_CODE, 0));
		mCheckBoxPoolConformers.setSelected("true".equals(configuration.getProperty(PROPERTY_POOL_CONFORMERS)));
		mTextFieldMaxCount.setText(configuration.getProperty(PROPERTY_MAX_CONFORMERS, value != null ? DEFAULT_MAX_CONFORMERS_EXPORT : DEFAULT_MAX_CONFORMERS_IN_TABLE));
		mCheckBoxLargestFragment.setSelected(!"false".equals(configuration.getProperty(PROPERTY_LARGEST_FRAGMENT)));
		mCheckBoxNeutralize.setSelected("true".equals(configuration.getProperty(PROPERTY_NEUTRALIZE_FRAGMENT)));

		value = configuration.getProperty(PROPERTY_STEREO_ISOMER_LIMIT);
		mCheckBoxSkip.setSelected(value != null);
		mTextFieldSkip.setText(value != null ? value : DEFAULT_MAX_STEREO_ISOMERS);

		mCheckBoxProtonate.setSelected(configuration.getProperty(PROPERTY_PROTONATION_PH) != null);
		mTextFieldPH.setText(configuration.getProperty(PROPERTY_PROTONATION_PH, "7.4"));
		mTextFieldPHSpan.setText(configuration.getProperty(PROPERTY_PROTONATION_SPAN, "0"));

		enableItems();
		mComboBoxTorsionSource.setEnabled(ALGORITHM_NEEDS_TORSIONS[mComboBoxAlgorithm.getSelectedIndex()]);
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mComboBoxAlgorithm.setSelectedIndex(DEFAULT_ALGORITHM);
		mComboBoxTorsionSource.setSelectedIndex(DEFAULT_TORSION_SOURCE);
		mComboBoxMinimize.setSelectedIndex(DEFAULT_MINIMIZATION);
		mCheckBoxExportFile.setSelected(false);
		mCheckBoxPoolConformers.setSelected(false);
		mTextFieldMaxCount.setText(DEFAULT_MAX_CONFORMERS_IN_TABLE);
		mCheckBoxLargestFragment.setSelected(true);
		mCheckBoxNeutralize.setSelected(false);
		mCheckBoxSkip.setSelected(true);
		mTextFieldSkip.setText(DEFAULT_MAX_STEREO_ISOMERS);
		mCheckBoxProtonate.setSelected(false);
		mTextFieldPH.setText("7.4");
		mTextFieldPHSpan.setText("0");

		enableItems();
		mComboBoxTorsionSource.setEnabled(ALGORITHM_NEEDS_TORSIONS[mComboBoxAlgorithm.getSelectedIndex()]);
		}

	private void enableItems() {
		boolean isEnabled = mCheckBoxExportFile.isSelected();
		mLabelFileName.setEnabled(isEnabled);
		mComboBoxFileType.setEnabled(isEnabled);
		mButtonEdit.setEnabled(isEnabled);
		mCheckBoxPoolConformers.setEnabled(isEnabled && mComboBoxFileType.getSelectedIndex() == 0);
		mCheckBoxLargestFragment.setEnabled(isEnabled);
		mCheckBoxNeutralize.setEnabled(isEnabled && mCheckBoxLargestFragment.isSelected() && (!PKaPredictor.isAvailable() || !mCheckBoxProtonate.isSelected()));
		mCheckBoxSkip.setEnabled(isEnabled);
		mTextFieldSkip.setEnabled(mCheckBoxSkip.isEnabled() && mCheckBoxSkip.isSelected());
		mCheckBoxProtonate.setEnabled(isEnabled && PKaPredictor.isAvailable() && (!mCheckBoxLargestFragment.isSelected() || !mCheckBoxNeutralize.isSelected()));
		mTextFieldPH.setEnabled(mCheckBoxProtonate.isEnabled() && mCheckBoxProtonate.isSelected());
		mTextFieldPHSpan.setEnabled(mCheckBoxProtonate.isEnabled() && mCheckBoxProtonate.isSelected());
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (!super.isConfigurationValid(configuration, isLive))
			return false;

		try {
			int count = Integer.parseInt(configuration.getProperty(PROPERTY_MAX_CONFORMERS));
			if (count < 1 || count > MAX_CONFORMERS) {
				showErrorMessage("The maximum conformer count must be between 1 and "+MAX_CONFORMERS);
				return false;
				}
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("The maximum conformer count is not numerical");
			return false;
			}

		String fileName = configuration.getProperty(PROPERTY_FILE_NAME);
		if (fileName != null) {
			if (!isFileAndPathValid(fileName, true, mCheckOverwrite))
				return false;
			int fileType = FILE_TYPE[findListIndex(configuration.getProperty(PROPERTY_FILE_TYPE), FILE_TYPE_CODE, 0)];
			String extension = FileHelper.getExtension(fileType);
			if (!fileName.endsWith(extension)) {
				showErrorMessage("Wrong file extension for file type '"+FILE_TYPE_TEXT[fileType]+"'.");
				return false;
				}
			String stereoIsomerLimit = configuration.getProperty(PROPERTY_STEREO_ISOMER_LIMIT);
			if (stereoIsomerLimit != null) {
				try {
					int count = Integer.parseInt(configuration.getProperty(PROPERTY_STEREO_ISOMER_LIMIT));
					if (count < 1) {
						showErrorMessage("The stereo isomer limit must not be lower than 1");
						return false;
						}
					}
				catch (NumberFormatException nfe) {
					showErrorMessage("The stereo isomer limit is not numerical");
					return false;
					}
				}
			String pH = configuration.getProperty(PROPERTY_PROTONATION_PH);
			if (pH != null) {
				if (pH.length() == 0) {
					showErrorMessage("No pH-value given.");
					return false;
					}
				try {
					float value = Float.parseFloat(pH);
					if (value < 0 || value > 14) {
						showErrorMessage("The pH value must be between 0 and 14.");
						return false;
						}
					String span = configuration.getProperty(PROPERTY_PROTONATION_SPAN);
					if (span != null) {
						try {
							float d = Float.parseFloat(span);
							if (value-d < 0 || value+d > 14) {
								showErrorMessage("The pH range must be between 0 and 14.");
								return false;
								}
							}
						catch (NumberFormatException nfe) {
							showErrorMessage("The pH range value is neither empty not numerical");
							return false;
							}
						}
					}
				catch (NumberFormatException nfe) {
					showErrorMessage("The pH value is not numerical");
					return false;
					}
				}
			}

		return true;
		}

	@Override
	protected int getNewColumnCount() {
		return (mFileType != FILE_TYPE_NONE) ? 0 : (mMinimization == MINIMIZE_MMFF94s || mMinimization == MINIMIZE_MMFF94sPlus) ? 2 : 1;
		}

	@Override
	protected void setNewColumnProperties(int firstNewColumn) {
		if (mFileType == FILE_TYPE_NONE) {
			getTableModel().setColumnProperty(firstNewColumn,
					CompoundTableModel.cColumnPropertySpecialType,
					CompoundTableModel.cColumnType3DCoordinates);
			getTableModel().setColumnProperty(firstNewColumn,
					CompoundTableModel.cColumnPropertyParentColumn,
					getTableModel().getColumnTitleNoAlias(getChemistryColumn()));
			}
		}

	@Override
	protected String getNewColumnName(int column) {
		switch (column) {
		case 0:
			String title = "3D-"+getTableModel().getColumnTitle(getChemistryColumn());
			switch (mAlgorithm) {
			case ADAPTIVE_RANDOM:
				return title+" (adaptive torsions, "+MINIMIZE_TITLE[mMinimization]+")";
			case SYSTEMATIC:
				return title+" (systematic torsions, "+MINIMIZE_TITLE[mMinimization]+")";
			case LOW_ENERGY_RANDOM:
				return title+" (low-energy random, "+MINIMIZE_TITLE[mMinimization]+")";
			case PURE_RANDOM:
				return title+" (pure random, "+MINIMIZE_TITLE[mMinimization]+")";
			case SELF_ORGANIZED:
				return title+" (self-organized, "+MINIMIZE_TITLE[mMinimization]+")";
			default:	// should not happen
				return CompoundTableModel.cColumnType3DCoordinates;
				}
		case 1:
			return "MMFF94 Energy";
		default:
			return "Unknown";
			}
		}

	@Override
	protected boolean preprocessRows(Properties configuration) {
		mAlgorithm = findListIndex(configuration.getProperty(PROPERTY_ALGORITHM), ALGORITHM_CODE, DEFAULT_ALGORITHM);
		mTorsionSource = findListIndex(configuration.getProperty(PROPERTY_TORSION_SOURCE), TORSION_SOURCE_CODE, DEFAULT_TORSION_SOURCE);
		mMinimization = findListIndex(configuration.getProperty(PROPERTY_MINIMIZE), MINIMIZE_CODE, DEFAULT_MINIMIZATION);
		if (mMinimization == MINIMIZE_MMFF94s) {
			ForceFieldMMFF94.initialize(ForceFieldMMFF94.MMFF94S);
			mMMFFOptions = new HashMap<>();
			}
		else if (mMinimization == MINIMIZE_MMFF94sPlus) {
			ForceFieldMMFF94.initialize(ForceFieldMMFF94.MMFF94SPLUS);
			mMMFFOptions = new HashMap<>();
			}
		mMinimizationErrors = 0;

		mStereoIsomerLimit = Integer.MAX_VALUE;
		String limit = configuration.getProperty(PROPERTY_STEREO_ISOMER_LIMIT);
		if (limit != null)
			mStereoIsomerLimit = Integer.parseInt(limit);

		mPH1 = Float.NaN;
		String ph = PKaPredictor.isAvailable() ? configuration.getProperty(PROPERTY_PROTONATION_PH) : null;
		if (ph != null) {
			mPH1 = Float.parseFloat(ph);
			mPH2 = mPH1;
			String span = configuration.getProperty(PROPERTY_PROTONATION_SPAN);
			if (span != null) {
				float d = Float.parseFloat(span);
				mPH2 = mPH1 + d;
				mPH1 -= d;
				}
			}

		mMaxConformers = Integer.parseInt(configuration.getProperty(PROPERTY_MAX_CONFORMERS));

		mStartMillis = System.currentTimeMillis();
		if (mAlgorithm != SELF_ORGANIZED && getTableModel().getTotalRowCount() > 99) {
			RigidFragmentCache.getDefaultInstance().loadDefaultCache();
			RigidFragmentCache.getDefaultInstance().resetAllCounters();
			mCacheSizeAtStart = RigidFragmentCache.getDefaultInstance().size();
			}

		mFileType = FILE_TYPE_NONE;	// default
		String fileName = configuration.getProperty(PROPERTY_FILE_NAME);
		if (fileName != null) {
			mFileType = FILE_TYPE[findListIndex(configuration.getProperty(PROPERTY_FILE_TYPE), FILE_TYPE_CODE, 0)];
			mLargestFragmentOnly = !"false".equals(configuration.getProperty(PROPERTY_LARGEST_FRAGMENT));
			mNeutralizeLargestFragment = "true".equals(configuration.getProperty(PROPERTY_NEUTRALIZE_FRAGMENT));
			mPoolConformers = "true".equals(configuration.getProperty(PROPERTY_POOL_CONFORMERS));

			String columnName = getTableModel().getColumnProperty(getChemistryColumn(), CompoundTableConstants.cColumnPropertyRelatedIdentifierColumn);
			mIdentifierColumn = (columnName == null) ? -1 : getTableModel().findColumn(columnName);

			try {
				mFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resolvePathVariables(fileName)), "UTF-8"));

				if (mFileType == FileHelper.cFileTypeDataWarrior)
					writeDataWarriorHeader();
				}
			catch (IOException ioe) {
				showErrorMessage(ioe.toString());
				return false;
				}

			mRowQueue = new SynchronousQueue<>();
			new Thread(() -> consumeRows()).start();
			}

		return true;
		}

	private void consumeRows() {
		String row;
		try {
			while ((row = mRowQueue.take()).length() != 0) {
				try {
					mFileWriter.write(row);
					} catch (IOException ioe) { break; }
				}
			} catch (InterruptedException ie) {}

		try {
			if (mFileType == FileHelper.cFileTypeDataWarrior)
				writeDataWarriorFooter();

			mFileWriter.close();
			}
		catch (IOException ioe) {
			showErrorMessage(ioe.toString());
			}
		}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule mol, int threadIndex) throws Exception {
		if (mFileType != FILE_TYPE_NONE) {
			addConformersToQueue(row, mol);
			}
		else {
			addConformerToTable(row, firstNewColumn, mol);
			}
		}

	@Override
	protected void postprocess(int firstNewColumn) {
		if (mRowQueue != null)
			try { mRowQueue.put(""); } catch (InterruptedException ie) {}  // to release consuming thread

		long seconds = (System.currentTimeMillis()-mStartMillis)/1000;
		System.out.println("Up to "+mMaxConformers+" conformers generated from "+getTableModel().getTotalRowCount()+" molecules in "+seconds+" seconds.");
		System.out.println("Algorithm:"+ALGORITHM_CODE[mAlgorithm]+". Minimization:"+MINIMIZE_CODE[mMinimization]+".");
		if (mAlgorithm != SELF_ORGANIZED) {
			int requests = RigidFragmentCache.getDefaultInstance().getRequestCount();
			int hits = RigidFragmentCache.getDefaultInstance().getHitCount();
			double quote = 100.0*(double)hits/(double)requests;
			int nonCachable = RigidFragmentCache.getDefaultInstance().getNonCachableCount();
			System.out.println("RigidFragmentCache size before:"+mCacheSizeAtStart+"; after:"+RigidFragmentCache.getDefaultInstance().size()+"; requests:"+requests+"; hits:"+hits+"("+DoubleFormat.toString(quote,3)+"%); non-cachable:"+nonCachable);
			}

		if (mMinimizationErrors != 0 && isInteractive())
			showInteractiveTaskMessage("Forcefield minimization failed in "+mMinimizationErrors+" cases.", JOptionPane.INFORMATION_MESSAGE);
		}

	private void addConformerToTable(int row, int firstNewColumn, StereoMolecule mol) {
		mol = getChemicalStructure(row, mol);
		if (mol == null || mol.getAllAtoms() == 0)
			return;

		boolean isOneStereoIsomer = !hasMultipleStereoIsomers(mol);
		ConformerGenerator cg = null;
		ConformationSelfOrganizer cs = null;

		Coordinates refCOG = null;
		Coordinates[] refCoords = null;
		Coordinates[] coords = null;
		int[] superposeAtom = null;

		TorsionDescriptorHelper torsionHelper = null;
		ArrayList<TorsionDescriptor> torsionDescriptorList = null;
		if (mMaxConformers > 1 && mMinimization != MINIMIZE_NONE) {
			torsionHelper = new TorsionDescriptorHelper(mol);
			torsionDescriptorList = new ArrayList<>();
			}

		StringBuilder coordsBuilder = new StringBuilder();
		StringBuilder energyBuilder = new StringBuilder();

		for (int i=0; i<mMaxConformers; i++) {
			switch (mAlgorithm) {
			case ADAPTIVE_RANDOM:
				if (cg == null) {
					cg = new ConformerGenerator(mMinimization == MINIMIZE_MMFF94sPlus);
					cg.initializeConformers(mol, ConformerGenerator.STRATEGY_ADAPTIVE_RANDOM, 1000, mTorsionSource == TORSION_SOURCE_6_STEPS);
					}
				mol = cg.getNextConformerAsMolecule(mol);
				break;
			case SYSTEMATIC:
				if (cg == null) {
					cg = new ConformerGenerator(mMinimization == MINIMIZE_MMFF94sPlus);
					cg.initializeConformers(mol, ConformerGenerator.STRATEGY_LIKELY_SYSTEMATIC, 1000, mTorsionSource == TORSION_SOURCE_6_STEPS);
					}
				mol = cg.getNextConformerAsMolecule(mol);
				break;
			case LOW_ENERGY_RANDOM:
				if (cg == null) {
					cg = new ConformerGenerator(mMinimization == MINIMIZE_MMFF94sPlus);
					cg.initializeConformers(mol, ConformerGenerator.STRATEGY_LIKELY_RANDOM, 1000, mTorsionSource == TORSION_SOURCE_6_STEPS);
					}
				mol = cg.getNextConformerAsMolecule(mol);
				break;
			case PURE_RANDOM:
				if (cg == null) {
					cg = new ConformerGenerator(mMinimization == MINIMIZE_MMFF94sPlus);
					cg.initializeConformers(mol, ConformerGenerator.STRATEGY_PURE_RANDOM, 1000, mTorsionSource == TORSION_SOURCE_6_STEPS);
					}
				mol = cg.getNextConformerAsMolecule(mol);
				break;
			case SELF_ORGANIZED:
				if (cs == null) {
					ConformerGenerator.addHydrogenAtoms(mol);
					cs = new ConformationSelfOrganizer(mol, true);
					cs.initializeConformers(0, mMaxConformers);
					}
				SelfOrganizedConformer soc = cs.getNextConformer();
				if (soc == null)
					mol = null;
				else
					soc.toMolecule(mol);
				break;
				}

			if (mol != null && mol.getAllAtoms() != 0) {
	//			String rawCoords = (mMinimization != MINIMIZE_MMFF94) ? null : new Canonizer(mol).getEncodedCoordinates(true);

				MinimizationResult result = new MinimizationResult();
				minimize(mol, result);

				// if we minimize, we check again, whether the minimized conformer is a very similar sibling in the list
				boolean isRedundantConformer = false;
				if (mMaxConformers > 1 && mMinimization != MINIMIZE_NONE)
					isRedundantConformer = isRedundantConformer(torsionHelper, torsionDescriptorList);

				if (!isRedundantConformer) {
					// If is first conformer, then prepare reference coords for superpositioning
					if (refCoords == null) {
						centerConformer(mol);
						superposeAtom = suggestSuperposeAtoms(mol);
						coords = new Coordinates[superposeAtom.length];
						refCoords = new Coordinates[superposeAtom.length];
						for (int j=0; j<superposeAtom.length; j++)
							refCoords[j] = new Coordinates(mol.getCoordinates(superposeAtom[j]));
						refCOG = FXConformerDialog.kabschCOG(refCoords);
						}
					else {	// superpose onto first conformer
						for (int j=0; j<superposeAtom.length; j++)
							coords[j] = new Coordinates(mol.getCoordinates(superposeAtom[j]));
						superpose(mol, coords, refCoords, refCOG);
						}

					Canonizer canonizer = new Canonizer(mol);
					if (isOneStereoIsomer	// a final conformer is one stereo isomer
					 && !canonizer.getIDCode().equals(getTableModel().getTotalValueAt(row, getChemistryColumn()))) {
						System.out.println("WARNING: idcodes after 3D-coordinate generation differ!!!");
						System.out.println("old: "+getTableModel().getTotalValueAt(row, getChemistryColumn()));
						System.out.println("new: "+canonizer.getIDCode());
						}

					if (coordsBuilder.length() != 0)
						coordsBuilder.append(' ');
					coordsBuilder.append(canonizer.getEncodedCoordinates(true));

					if (mMinimization == MINIMIZE_MMFF94sPlus || mMinimization == MINIMIZE_MMFF94s) {
						String energyText = (result.errorMessage != null) ? result.errorMessage : DoubleFormat.toString(result.energy);
						if (energyBuilder.length() != 0)
							energyBuilder.append("; ");
						energyBuilder.append(energyText);
						}
					}
				}
			}

		if (coordsBuilder.length() != 0) {
			getTableModel().setTotalValueAt(coordsBuilder.toString(), row, firstNewColumn);
			if (mMinimization == MINIMIZE_MMFF94sPlus || mMinimization == MINIMIZE_MMFF94s) {
				getTableModel().setTotalValueAt(energyBuilder.toString(), row, firstNewColumn+1);
				}
			}
		else{
			getTableModel().setTotalValueAt(null, row, firstNewColumn);
			if (mMinimization == MINIMIZE_MMFF94sPlus || mMinimization == MINIMIZE_MMFF94s) {
				getTableModel().setTotalValueAt(null, row, firstNewColumn + 1);
				}
			}
		}

	private void superpose(StereoMolecule mol, Coordinates[] coords, Coordinates[] refCoords, Coordinates refCOG) {
		Coordinates cog = FXConformerDialog.kabschCOG(coords);
		double[][] matrix = FXConformerDialog.kabschAlign(refCoords, coords, refCOG, cog);
		for (int atom=0; atom<mol.getAllAtoms(); atom++) {
			Coordinates c = mol.getCoordinates(atom);
			c.sub(cog);
			c.rotate(matrix);
			c.add(refCOG);
			}
		}

	private void addConformersToQueue(int row, StereoMolecule mol) throws Exception {
		mol = getChemicalStructure(row, mol);
		if (mol == null || mol.getAllAtoms() == 0)
			return;

		if (mLargestFragmentOnly) {
			mol.stripSmallFragments();
			if (mNeutralizeLargestFragment)
				new MoleculeNeutralizer().neutralizeChargedMolecule(mol);
			}

		// don't create any protonation states
		if (Float.isNaN(mPH1)) {
			StringBuilder builder = new StringBuilder();
			addConformersToQueue2(row, -1, mol, builder);
			if (builder.length() != 0)
				mRowQueue.put(builder.toString());

			return;
			}

		double[] basicpKa = new double[3];
		double[] acidicpKa = new double[3];
		int[] basicAtom = new int[3];
		int[] acidicAtom = new int[3];

		for (int i=0; i<3; i++) {
			basicpKa[i] = Double.NaN;
			acidicpKa[i] = Double.NaN;
			}

		new PKaPredictor().getProtonationStates(mol, basicpKa, acidicpKa, basicAtom, acidicAtom);

		ArrayList<PKa> pKaList = new ArrayList<PKa>();
		for (int i=0; i<3 && !Double.isNaN(basicpKa[i]); i++)
			pKaList.add(new PKa(basicAtom[i], basicpKa[i], true));
		for (int i=0; i<3 && !Double.isNaN(acidicpKa[i]); i++)
			pKaList.add(new PKa(acidicAtom[i], acidicpKa[i], false));

		PKa[] pKa = pKaList.toArray(new PKa[0]);
		Arrays.sort(pKa);

		// determine indexes of pKa values that are within the pH-range
		int i1 = 0;
		while (i1 < pKa.length && pKa[i1].pKa < mPH1)
			i1++;
		int i2 = i1;
		while (i2 < pKa.length && pKa[i2].pKa <= mPH2)
			i2++;

		for (int i=0; i<i1; i++)
			mol.setAtomCharge(pKa[i].atom, pKa[i].isBasic ? 0 : -1);
		for (int i=i1; i<pKa.length; i++)
			mol.setAtomCharge(pKa[i].atom, pKa[i].isBasic ? 1 : 0);

		StringBuilder builder = new StringBuilder();
		addConformersToQueue2(row, 0, new StereoMolecule(mol), builder);
		for (int i=i1; i<i2; i++) {
			mol.setAtomCharge(pKa[i].atom, pKa[i].isBasic ? 0 : -1);
			addConformersToQueue2(row, i-i1+1, new StereoMolecule(mol), builder);
			}
		if (builder.length() != 0)
			mRowQueue.put(builder.toString());
		}

	public static int[] suggestSuperposeAtoms(StereoMolecule mol) {
		boolean[] isRotatableBond = new boolean[mol.getAllBonds()];
		int count = TorsionDB.findRotatableBonds(mol, true, isRotatableBond);
		if (count == 0) {
			int[] coreAtom = new int[mol.getAllAtoms()];
			for (int atom=0; atom<mol.getAllAtoms(); atom++)
				coreAtom[atom] = atom;
			return coreAtom;
			}

		int[] fragmentNo = new int[mol.getAllAtoms()];
		int fragmentCount = mol.getFragmentNumbers(fragmentNo, isRotatableBond, true);
		int[] fragmentSize = new int[fragmentCount];
		float[] atad = mol.getAverageTopologicalAtomDistance();
		float[] fragmentATAD = new float[fragmentCount];
		for (int atom=0; atom<mol.getAtoms(); atom++) {
			fragmentATAD[fragmentNo[atom]] += atad[atom];
			fragmentSize[fragmentNo[atom]]++;
			}
		int bestFragment = -1;
		float bestATAD = Float.MAX_VALUE;
		for (int i=0; i<fragmentCount; i++) {
			fragmentATAD[i] /= fragmentSize[i];
			if (bestATAD > fragmentATAD[i]) {
				bestATAD = fragmentATAD[i];
				bestFragment = i;
				}
			}
		int fragmentSizeWithNeighbours = fragmentSize[bestFragment];
		for (int atom=0; atom<mol.getAtoms(); atom++) {
			if (fragmentNo[atom] == bestFragment) {
				for (int i = 0; i < mol.getConnAtoms(atom); i++) {
					int connAtom = mol.getConnAtom(atom, i);
					if (fragmentNo[connAtom] != bestFragment
							&& fragmentNo[connAtom] != fragmentCount) {
						fragmentNo[connAtom] = fragmentCount;
						fragmentSizeWithNeighbours++;
						}
					}
				}
			}
		int[] coreAtom = new int[fragmentSizeWithNeighbours];
		int index = 0;
		for (int atom=0; atom<mol.getAtoms(); atom++)
			if (fragmentNo[atom] == bestFragment
					|| fragmentNo[atom] == fragmentCount)
				coreAtom[index++] = atom;
		return coreAtom;
		}

	private class PKa implements Comparable<PKa> {
		int atom;
		double pKa;
		boolean isBasic;

		public PKa(int atom, double pKa, boolean isBasic) {
			this.atom = atom;
			this.pKa = pKa;
			this.isBasic = isBasic;
			}

		@Override public int compareTo(PKa o) {
			return this.pKa > o.pKa ? 1 : this.pKa == o.pKa ? 0 : -1;
			}
		}

	private void addConformersToQueue2(int row, int protonationState, StereoMolecule mol, StringBuilder builder) throws Exception {
		StereoIsomerEnumerator sie = new StereoIsomerEnumerator(mol, true);
		int isomerCount = sie.getStereoIsomerCount();
		boolean createEnantiomers = sie.isSkippingEnantiomers();

		if (isomerCount <= mStereoIsomerLimit)
			for (int i=0; i<isomerCount; i++)
				addConformersToQueue3(row, protonationState, i, sie, createEnantiomers, builder);
		}

	private void addConformersToQueue3(int row, int protonationState, int stereoIsomer, StereoIsomerEnumerator stereoIsomerEnumerator, boolean createEnantiomers, StringBuilder builder) throws Exception {
		ConformerGenerator cg = null;
		ConformationSelfOrganizer cs = null;

		int maxTorsionSets = (int)Math.max(2 * mMaxConformers, (1000 * Math.sqrt(mMaxConformers)));

		StereoMolecule mol = stereoIsomerEnumerator.getStereoIsomer(stereoIsomer);

		TorsionDescriptorHelper torsionHelper = null;
		ArrayList<TorsionDescriptor> torsionDescriptorList = null;
		if (mMaxConformers > 1 && mMinimization != MINIMIZE_NONE) {
			torsionHelper = new TorsionDescriptorHelper(mol);
			torsionDescriptorList = new ArrayList<TorsionDescriptor>();
			}

		Canonizer canonizer1 = null;
		Canonizer canonizer2 = null;
		StringBuilder coordsBuilder1 = mPoolConformers ? new StringBuilder() : null;
		StringBuilder coordsBuilder2 = mPoolConformers ? new StringBuilder() : null;
		StringBuilder energyBuilder1 = mPoolConformers && mMinimization != MINIMIZE_NONE ? new StringBuilder() : null;
		StringBuilder energyBuilder2 = mPoolConformers && mMinimization != MINIMIZE_NONE ? new StringBuilder() : null;
		StringBuilder errorBuilder1 = mPoolConformers && mMinimization != MINIMIZE_NONE ? new StringBuilder() : null;
		StringBuilder errorBuilder2 = mPoolConformers && mMinimization != MINIMIZE_NONE ? new StringBuilder() : null;

		Coordinates refCOG = null;
		Coordinates[] refCoords = null;
		Coordinates[] coords = null;
		int[] superposeAtom = null;

		String id = (mIdentifierColumn == -1) ? Integer.toString(row+1) : getTableModel().getTotalValueAt(row, mIdentifierColumn);
		int realStereoIsomer = stereoIsomer * (createEnantiomers ? 2 : 1);

		for (int i=0; i<mMaxConformers; i++) {
			switch (mAlgorithm) {
			case ADAPTIVE_RANDOM:
				if (cg == null) {
					cg = new ConformerGenerator(mMinimization == MINIMIZE_MMFF94sPlus);
					cg.initializeConformers(mol, ConformerGenerator.STRATEGY_ADAPTIVE_RANDOM, maxTorsionSets, mTorsionSource == TORSION_SOURCE_6_STEPS);
					}
				mol = cg.getNextConformerAsMolecule(mol);
				break;
			case SYSTEMATIC:
				if (cg == null) {
					cg = new ConformerGenerator(mMinimization == MINIMIZE_MMFF94sPlus);
					cg.initializeConformers(mol, ConformerGenerator.STRATEGY_LIKELY_SYSTEMATIC, maxTorsionSets, mTorsionSource == TORSION_SOURCE_6_STEPS);
					}
				mol = cg.getNextConformerAsMolecule(mol);
				break;
			case LOW_ENERGY_RANDOM:
				if (cg == null) {
					cg = new ConformerGenerator(mMinimization == MINIMIZE_MMFF94sPlus);
					cg.initializeConformers(mol, ConformerGenerator.STRATEGY_LIKELY_RANDOM, maxTorsionSets, mTorsionSource == TORSION_SOURCE_6_STEPS);
					}
				mol = cg.getNextConformerAsMolecule(mol);
				break;
			case PURE_RANDOM:
				if (cg == null) {
					cg = new ConformerGenerator(mMinimization == MINIMIZE_MMFF94sPlus);
					cg.initializeConformers(mol, ConformerGenerator.STRATEGY_PURE_RANDOM, maxTorsionSets, mTorsionSource == TORSION_SOURCE_6_STEPS);
					}
				mol = cg.getNextConformerAsMolecule(mol);
				break;
			case SELF_ORGANIZED:
				if (cs == null) {
					ConformerGenerator.addHydrogenAtoms(mol);
					cs = new ConformationSelfOrganizer(mol, true);
					cs.initializeConformers(0, mMaxConformers);
					}
				SelfOrganizedConformer soc = cs.getNextConformer();
				if (soc == null)
					mol = null;
				else
					soc.toMolecule(mol);
				break;
				}

			if (mol == null)
				break;

			MinimizationResult result = new MinimizationResult();
			minimize(mol, result);

			if (!stereoIsomerEnumerator.isCorrectStereoIsomer(mol, stereoIsomer))
				continue;

			// if we minimize, we check again, whether the minimized conformer is a very similar sibling in the list
			boolean isRedundantConformer = false;
			if (mMaxConformers > 1 && mMinimization != MINIMIZE_NONE)
				isRedundantConformer = isRedundantConformer(torsionHelper, torsionDescriptorList);

			if (!isRedundantConformer) {
				// If is first conformer, then prepare reference coords for superpositioning
				if (refCoords == null) {
					centerConformer(mol);
					superposeAtom = suggestSuperposeAtoms(mol);
					coords = new Coordinates[superposeAtom.length];
					refCoords = new Coordinates[superposeAtom.length];
					for (int j=0; j<superposeAtom.length; j++)
						refCoords[j] = new Coordinates(mol.getCoordinates(superposeAtom[j]));
					refCOG = FXConformerDialog.kabschCOG(refCoords);
					}
				else {	// superpose onto first conformer
					for (int j=0; j<superposeAtom.length; j++)
						coords[j] = new Coordinates(mol.getCoordinates(superposeAtom[j]));
					superpose(mol, coords, refCoords, refCOG);
					}

				if (mFileType == FileHelper.cFileTypeDataWarrior) {
					if (canonizer1 == null)
						canonizer1 = new Canonizer(mol, Canonizer.COORDS_ARE_3D);
					else
						canonizer1.invalidateCoordinates();

					if (mPoolConformers) {
						if (coordsBuilder1.length() != 0)
							coordsBuilder1.append(' ');
						coordsBuilder1.append(canonizer1.getEncodedCoordinates(true));
						if (mMinimization != MINIMIZE_NONE) {
							if (energyBuilder1.length() != 0)
								energyBuilder1.append("; ");
							energyBuilder1.append(result.energy());
							if (errorBuilder1.length() != 0)
								errorBuilder1.append("; ");
							errorBuilder1.append(result.error());
							}
						}
					else {
						buildDataWarriorRecord(builder, canonizer1.getIDCode(), canonizer1.getEncodedCoordinates(),
								id, protonationState, realStereoIsomer, result.energy(), result.error());
						}
					}
				else {
					buildSDFRecord(builder, mol, id, protonationState, realStereoIsomer, result);
					}

				if (createEnantiomers) {
					for (int atom=0; atom<mol.getAllAtoms(); atom++)
						mol.setAtomZ(atom, -mol.getAtomZ(atom));

					if (mFileType == FileHelper.cFileTypeDataWarrior) {
						if (canonizer2 == null)
							canonizer2 = new Canonizer(mol, Canonizer.COORDS_ARE_3D);
						else
							canonizer2.invalidateCoordinates();

						if (mPoolConformers) {
							if (coordsBuilder2.length() != 0)
								coordsBuilder2.append(' ');
							coordsBuilder2.append(canonizer2.getEncodedCoordinates(true));
							if (mMinimization != MINIMIZE_NONE) {
								if (energyBuilder2.length() != 0)
									energyBuilder2.append("; ");
								energyBuilder2.append(result.energy());
								if (errorBuilder2.length() != 0)
									errorBuilder2.append("; ");
								errorBuilder2.append(result.error());
								}
							}
						else {
							buildDataWarriorRecord(builder, canonizer2.getIDCode(), canonizer2.getEncodedCoordinates(),
									id, protonationState, realStereoIsomer+1, result.energy(), result.error());
							}
						}
					else {
						buildSDFRecord(builder, mol, id, protonationState, realStereoIsomer+1, result);
						}
					}
				}
			}

		if (mPoolConformers && canonizer1 != null) {
			buildDataWarriorRecord(builder, canonizer1.getIDCode(), coordsBuilder1.toString(), id,
					protonationState, realStereoIsomer,
					energyBuilder1 == null ? null : energyBuilder1.toString(),
					errorBuilder1 == null ? null : errorBuilder1.toString());

			if (createEnantiomers && canonizer2 != null)
				buildDataWarriorRecord(builder, canonizer2.getIDCode(), coordsBuilder2.toString(), id,
						protonationState, realStereoIsomer+1,
						energyBuilder2 == null ? null : energyBuilder2.toString(),
						errorBuilder2 == null ? null : errorBuilder2.toString());
			}
		}

	/**
	 * Minimizes the molecule with the method defined in mMinimization.
	 * The starting conformer is taken from mol.
	 * When this method finishes, then the minimized atom coordinates are in mol,
	 * even if mMinimization == MINIMIZE_NONE.
	 * @param mol receives minimized coodinates; taken as start conformer
	 * @param result receives energy and possibly error message
	 */
	private void minimize(StereoMolecule mol, MinimizationResult result) {
		try {
			if (mMinimization == MINIMIZE_MMFF94sPlus || mMinimization == MINIMIZE_MMFF94s) {
				String tableSet = mMinimization == MINIMIZE_MMFF94sPlus ? ForceFieldMMFF94.MMFF94SPLUS : ForceFieldMMFF94.MMFF94S;
				int[] fragmentNo = new int[mol.getAllAtoms()];
				int fragmentCount = mol.getFragmentNumbers(fragmentNo, false, true);
				if (fragmentCount == 1) {
					ForceFieldMMFF94 ff = new ForceFieldMMFF94(mol, tableSet, mMMFFOptions);
					int error = ff.minimise(10000, 0.0001, 1.0e-6);
					if (error != 0)
						throw new Exception("MMFF94 error code "+error);
					result.energy = (float)ff.getTotalEnergy();
					}
				else {
					int maxAtoms = 0;
	
					StereoMolecule[] fragment = mol.getFragments(fragmentNo, fragmentCount);
					for (StereoMolecule f:fragment) {
						if (f.getAllAtoms() > 2) {
							ForceFieldMMFF94 ff = new ForceFieldMMFF94(f, tableSet, mMMFFOptions);
							int error = ff.minimise(10000, 0.0001, 1.0e-6);
							if (error != 0)
								throw new Exception("MMFF94 error code "+error);
	
							if (maxAtoms < f.getAllAtoms()) {	// we take the energy value from the largest fragment
								maxAtoms = f.getAllAtoms();
								result.energy = (float)ff.getTotalEnergy();
								}
							}
						}
					int[] atom = new int[fragmentCount];
					for (int i=0; i<fragmentNo.length; i++) {
						int f = fragmentNo[i];
						mol.setAtomX(i, fragment[f].getAtomX(atom[f]));
						mol.setAtomY(i, fragment[f].getAtomY(atom[f]));
						mol.setAtomZ(i, fragment[f].getAtomZ(atom[f]));
						atom[f]++;
						}
					}
				}
			}
		catch (Exception e) {
			result.energy = Double.NaN;
			result.errorMessage = e.getLocalizedMessage();

			if (mMinimizationErrors == 0)
				e.printStackTrace();
			mMinimizationErrors++;
			}
		}

	private boolean isRedundantConformer(TorsionDescriptorHelper torsionHelper, ArrayList<TorsionDescriptor> torsionDescriptorList) {
		TorsionDescriptor ntd = torsionHelper.getTorsionDescriptor();
		for (TorsionDescriptor td:torsionDescriptorList)
			if (td.equals(ntd))
				return true;

		torsionDescriptorList.add(ntd);
		return false;
		}

	private boolean hasMultipleStereoIsomers(StereoMolecule mol) {
		for (int atom=0; atom<mol.getAtoms(); atom++)
			if (mol.getAtomParity(atom) == Molecule.cAtomParityUnknown
			 || (mol.isAtomStereoCenter(atom) && mol.getAtomESRType(atom) != Molecule.cESRTypeAbs))
				return true;
		for (int bond=0; bond<mol.getBonds(); bond++)
			if (mol.getBondParity(bond) == Molecule.cBondParityUnknown)
				return true;

		return false;
		}

	private void centerConformer(StereoMolecule mol) {
		double x = 0;
		double y = 0;
		double z = 0;
		for (int atom=0; atom<mol.getAllAtoms(); atom++) {
			x += mol.getAtomX(atom);
			y += mol.getAtomY(atom);
			z += mol.getAtomZ(atom);
			}
		x /= mol.getAllAtoms();
		y /= mol.getAllAtoms();
		z /= mol.getAllAtoms();
		for (int atom=0; atom<mol.getAllAtoms(); atom++) {
			mol.setAtomX(atom, mol.getAtomX(atom) - x);
			mol.setAtomY(atom, mol.getAtomY(atom) - y);
			mol.setAtomZ(atom, mol.getAtomZ(atom) - z);
			}
		}

	private void writeDataWarriorHeader() throws IOException {
		mFileWriter.write("<datawarrior-fileinfo>\n");
		mFileWriter.write("<version=\"3.1\">\n");
		mFileWriter.write("</datawarrior-fileinfo>\n");
		mFileWriter.write("<column properties>\n");
		mFileWriter.write("<columnName=\"Structure\">\n");
		mFileWriter.write("<columnProperty=\"specialType\tidcode\">\n");
		mFileWriter.write("<columnName=\"idcoordinates3D\">\n");
		mFileWriter.write("<columnProperty=\"specialType\tidcoordinates3D\">\n");
		mFileWriter.write("<columnProperty=\"parent\tStructure\">\n");
		mFileWriter.write("</column properties>\n");
		mFileWriter.write("Structure\tidcoordinates3D\tID");
		if (!Float.isNaN(mPH1))
			mFileWriter.write("\tProtonation State");
		mFileWriter.write("\tStereo Isomer");
		if (mMinimization != MINIMIZE_NONE)
			mFileWriter.write("\tEnergy\tMinimization Error");	// Don't change. These names are used in CompoundTableSaver!!!
		mFileWriter.write("\n");
		}

	private void writeDataWarriorFooter() throws IOException {
		mFileWriter.write("<datawarrior properties>\n");
		mFileWriter.write("<columnWidth_Table_Energy=\"75\">\n");
		mFileWriter.write("<columnWidth_Table_ID=\"75\">\n");
		mFileWriter.write("<columnWidth_Table_Minimization Error=\"75\">\n");
		mFileWriter.write("<columnWidth_Table_Structure=\"132\">\n");
		mFileWriter.write("<detailView=\"height[Data]=0.22;height[Structure]=0.30;height[3D-Structure]=0.48\">\n");
		if (mMinimizationErrors != 0)
			mFileWriter.write("<filter0=\"#double#\tEnergy\t#disabled#\">\n");
		else
			mFileWriter.write("<filter0=\"#double#\tEnergy\">\n");
		mFileWriter.write("<mainSplitting=\"0.72\">\n");
		mFileWriter.write("<mainView=\"Structures\">\n");
		mFileWriter.write("<mainViewCount=\"2\">\n");
		mFileWriter.write("<mainViewDockInfo0=\"root\">\n");
		mFileWriter.write("<mainViewDockInfo1=\"Table\tright\t0.50\">\n");
		mFileWriter.write("<mainViewName0=\"Table\">\n");
		mFileWriter.write("<mainViewName1=\"Structures\">\n");
		mFileWriter.write("<mainViewType0=\"tableView\">\n");
		mFileWriter.write("<mainViewType1=\"structureView\">\n");
		mFileWriter.write("<rightSplitting=\"0.16\">\n");
		mFileWriter.write("<rowHeight_Table=\"80\">\n");
		mFileWriter.write("<structureGridColumn_Structures=\"Structure\">\n");
		mFileWriter.write("<structureGridColumns_Structures=\"6\">\n");
		mFileWriter.write("</datawarrior properties>\n");
		}

	private void buildDataWarriorRecord(StringBuilder builder, String idcode, String coords, String id,
										int protonationState, int stereoIsomer, String energy, String error) {
		builder.append(idcode+"\t"+coords+"\t"+id);
		if (!Float.isNaN(mPH1)) {
			builder.append('\t');
			builder.append(protonationState+1);
			}
		builder.append('\t');
		builder.append(stereoIsomer+1);
		if (mMinimization != MINIMIZE_NONE) {
			builder.append('\t');
			builder.append(energy);
			builder.append('\t');
			builder.append(error);
			}
		builder.append('\n');
		}

	private void buildSDFRecord(StringBuilder builder, StereoMolecule mol, String id,
								int protonationState, int stereoIsomer, MinimizationResult result) {
		if (mFileType == FileHelper.cFileTypeSDV2)
			new MolfileCreator(mol, true, builder);
		else
			new MolfileV3Creator(mol, true, builder);

		builder.append(">  <ID>\n");
		builder.append(id);
		builder.append("\n\n");

		if (protonationState != -1) {
			builder.append(">  <Protonation State>\n");
			builder.append(protonationState+1);
			builder.append("\n\n");
			}

		if (stereoIsomer != -1) {
			builder.append(">  <Stereo Isomer>\n");
			builder.append(stereoIsomer+1);
			builder.append("\n\n");
			}

		if (mMinimization != MINIMIZE_NONE) {
			builder.append(">  <Energy>\n");
			builder.append(result.energy());
			builder.append("\n\n");

			builder.append(">  <Error>\n");
			builder.append(result.error());
			builder.append("\n\n");
			}

		builder.append("$$$$\n");
		}

	private class MinimizationResult {
		double energy;
		String errorMessage;

		public MinimizationResult() {
			energy = Double.NaN;
			errorMessage = null;
			}

		public String energy() {
			return Double.isNaN(energy) ? "" : DoubleFormat.toString(energy);
			}

		public String error() {
			return errorMessage == null ? "" : errorMessage;
			}
		}
	}
