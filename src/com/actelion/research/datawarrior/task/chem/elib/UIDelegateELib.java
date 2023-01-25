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

package com.actelion.research.datawarrior.task.chem.elib;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.CompoundFileHelper;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.prediction.MolecularPropertyHelper;
import com.actelion.research.datawarrior.task.AbstractTask;
import com.actelion.research.datawarrior.task.TaskUIDelegate;
import com.actelion.research.datawarrior.task.chem.DECompoundProvider;
import com.actelion.research.datawarrior.task.file.JFilePathLabel;
import com.actelion.research.gui.*;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableListHandler;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

public class UIDelegateELib implements ActionListener,TaskConstantsELib,TaskUIDelegate {
	private static final int MIN_DEFERRED_OPTION = RANDOM_OPTION;
	private static final int MIN_DEFERRED_DETAIL_OPTION = FILE_OPTION;

	//	private static final String LSD_IDCODE = "fa{q@@DZjCHhhhddXeEhmEjt\\e[WkUUSUADPtRLJr@@";
	private static final String[] DEFAULT_SET = {
			"fb}I@@DRTXFQQJJJIJIZsbm^mENVh@HBHjB@@",
			"ffka`@B`xXhaHRDaDaHQDRDGBTeztPirV`jBjbjjFjHUDrJbX@",
			"fj}qb@LNKAFPAFRRFJIJIIQVVb{rRkUAPE@PP@@",
			"fc\u007FPR@L^`apPAFRREQIJKQPyJJEJcEV}E^uiBBQjjh@HbDR@@",
			"fa{q@@DZjCHhhhddXeEhmEjt\\e[WkUUSUADPtRLJr@@",
			"fj}`c@I^BCDDDEbLbbRabTtRbJhqQdy`@jjA`HbGJ@@",
			"fnkq@@L\\jCHeEDehdhddlZ\\TZI\\uZZjfJBbdH@@",
			"fbmi`@DTxIPDyrJIQQJjJKILyIW`mLtuSUUVMKtC@@" };

	private Frame				mParentFrame;
	private CompoundTableModel	mTableModel;
	private DETaskBuildEvolutionaryLibrary mTask;
	private boolean             mDiableEvents;
	private JComboBox			mComboBoxStartCompounds,mComboBoxGenerations,mComboBoxCompounds,mComboBoxRuns,
								mComboBoxSurvivalCount,mComboBoxCreateLike,mComboBoxFitnessOption;
	private JCheckBox           mCheckBoxDeferred;
	private JButton             mButtonDeferredDetails;
	private String                mFirstGenerationFile;
	private String              mFirstGenerationColumn,mFirstGenerationList;
	private JPanel				mFitnessPanel;
	private JScrollPane			mFitnessScrollpane;
	private CompoundCollectionPane<StereoMolecule> mFirstGenerationPane;
	private DefaultCompoundCollectionModel.Molecule mFirstGeneration;
	private DECompoundProvider mCompoundCollectionHelper;

	public UIDelegateELib(Frame parent, CompoundTableModel tableModel, DETaskBuildEvolutionaryLibrary task) {
		mParentFrame = parent;
		mTableModel = tableModel;
		mTask = task;
		}

	public CompoundTableModel getTableModel() {
		return mTableModel;
		}

	@Override
	public JComponent createDialogContent() {
		JPanel p1 = new JPanel();
		final int scaled4 = HiDPIHelper.scale(4);
		final int scaled8 = HiDPIHelper.scale(8);
		double[][] size1 = { {scaled8, TableLayout.PREFERRED, scaled8, TableLayout.PREFERRED, 2*scaled8, TableLayout.FILL,
				TableLayout.PREFERRED, scaled8, TableLayout.PREFERRED, scaled8},
							 {scaled8, TableLayout.PREFERRED, scaled8, HiDPIHelper.scale(104),
									 scaled4, TableLayout.PREFERRED, 2*scaled8,
								TableLayout.PREFERRED, scaled4, TableLayout.PREFERRED, scaled4, TableLayout.PREFERRED,
								3*scaled8, TableLayout.PREFERRED, scaled8, TableLayout.FILL, scaled8} };
		p1.setLayout(new TableLayout(size1));

		p1.add(new JLabel("1st generation compounds:", JLabel.RIGHT), "1,1");

		mComboBoxStartCompounds = new JComboBox(START_COMPOUND_TEXT);
		mComboBoxStartCompounds.addActionListener(this);
		p1.add(mComboBoxStartCompounds, "3,1");

		mCheckBoxDeferred = new JCheckBox("Build at task execution time");
		mCheckBoxDeferred.addActionListener(this);

		mButtonDeferredDetails = new JButton("Details...");
		mButtonDeferredDetails.addActionListener(this);

		JPanel deferredPanel = new JPanel();
		deferredPanel.setLayout(new BorderLayout());
		deferredPanel.add(mCheckBoxDeferred, BorderLayout.WEST);
		deferredPanel.add(mButtonDeferredDetails, BorderLayout.EAST);
		p1.add(deferredPanel, "5,1,8,1");

		mFirstGeneration = new DefaultCompoundCollectionModel.Molecule();
		mFirstGenerationPane = new CompoundCollectionPane<StereoMolecule>(mFirstGeneration, false);
		mFirstGenerationPane.setEditable(true);
		mFirstGenerationPane.setClipboardHandler(new ClipboardHandler());
		mFirstGenerationPane.setShowValidationError(true);
		p1.add(mFirstGenerationPane, "1,3,8,3");

		p1.add(new JLabel("(Select sub-structures to protect them from being changed)", JLabel.CENTER), "1,5,8,5");

		mCompoundCollectionHelper = new DECompoundProvider(mFirstGenerationPane);
		mCompoundCollectionHelper.addPopupItems(mParentFrame);

		mComboBoxGenerations = new JComboBox(GENERATION_OPTIONS);
		mComboBoxCompounds = new JComboBox(COMPOUND_OPTIONS);
		mComboBoxCompounds.setSelectedItem(DEFAULT_COMPOUNDS);
		mComboBoxCompounds.setEditable(true);
		mComboBoxSurvivalCount = new JComboBox(SURVIVAL_OPTIONS);
		mComboBoxSurvivalCount.setSelectedItem(DEFAULT_SURVIVALS);
		mComboBoxSurvivalCount.setEditable(true);
		p1.add(mComboBoxGenerations, "1,7");
		p1.add(mComboBoxCompounds, "1,9");
		p1.add(mComboBoxSurvivalCount, "1,11");
		p1.add(new JLabel("Generations"), "3,7");
		p1.add(new JLabel("Compounds per generation"), "3,9");
		p1.add(new JLabel("Compounds survive a generation"), "3,11");

		p1.add(new JLabel("Create compounds like"), "6,7");
		mComboBoxCreateLike = new JComboBox(COMPOUND_KIND_TEXT);
		p1.add(mComboBoxCreateLike, "8,7");

		p1.add(new JLabel("Total run count:"), "6,11");
		mComboBoxRuns = new JComboBox(RUN_OPTIONS);
		mComboBoxRuns.setEditable(true);
		p1.add(mComboBoxRuns, "8,11");

		p1.add(new JLabel("Fitness Criteria"), "1,13");
		JButton bAdd = new JButton("Add Criterion ->");
		bAdd.setActionCommand("addOption");
		bAdd.addActionListener(this);
		p1.add(bAdd, "6,13");
		mComboBoxFitnessOption = new JComboBox();
		mComboBoxFitnessOption.addItem(FitnessPanel.STRUCTURE_OPTION_TEXT);
		mComboBoxFitnessOption.addItem(FitnessPanel.CONFORMER_OPTION_TEXT);
		mComboBoxFitnessOption.addItem(FitnessPanel.DOCKING_OPTION_TEXT);
		for (int i = 0; i< MolecularPropertyHelper.getPropertyCount(); i++)
			mComboBoxFitnessOption.addItem(MolecularPropertyHelper.getPropertyName(i));
		mComboBoxFitnessOption.setSelectedIndex(0); // structure option
		p1.add(mComboBoxFitnessOption, "8,13");

		mFitnessPanel = new JPanel() {
		    private static final long serialVersionUID = 0x20140724;

		    @Override
		    public void paintComponent(Graphics g) {
		    	super.paintComponent(g);

		    	if (getComponentCount() == 0) {
		            Dimension theSize = getSize();
		            Insets insets = getInsets();
		            theSize.width -= insets.left + insets.right;
		            theSize.height -= insets.top + insets.bottom;

	    	        g.setColor(Color.GRAY);
	    	        FontMetrics metrics = g.getFontMetrics();
	    	        final String message = "<To add fitness criteria select type and click 'Add Criterion'>";
	    	        Rectangle2D bounds = metrics.getStringBounds(message, g);
	    	        g.drawString(message, (int)(insets.left+theSize.width-bounds.getWidth())/2,
	    	                                   (insets.top+theSize.height-metrics.getHeight())/2+metrics.getAscent());
		    		}

		    	Rectangle r = new Rectangle();
		    	g.setColor(Color.GRAY);
		    	for (int i=1; i<getComponentCount(); i++) {
		    		getComponent(i).getBounds(r);
					g.drawLine(r.x+2, r.y-3, r.x+r.width-3, r.y-3);
					g.drawLine(r.x+2, r.y-2, r.x+r.width-3, r.y-2);
		    		}
		    	}
			};
		Dimension fitnessPanelSize = new Dimension(HiDPIHelper.scale(512), HiDPIHelper.scale(256));
		mFitnessPanel.setLayout(new VerticalFlowLayout());
		mFitnessScrollpane = new JScrollPane(mFitnessPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		mFitnessScrollpane.setMinimumSize(fitnessPanelSize);
		mFitnessScrollpane.setPreferredSize(fitnessPanelSize);
		p1.add(mFitnessScrollpane, "1,15,8,15");

		return p1;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (mDiableEvents)
			return;

		if (e.getSource() == mComboBoxStartCompounds) {
			int startSetOption = mComboBoxStartCompounds.getSelectedIndex();

			if (startSetOption < MIN_DEFERRED_OPTION && mCheckBoxDeferred.isSelected())
				mCheckBoxDeferred.setSelected(false);

			if (startSetOption == FILE_OPTION) {
				File file = new FileHelper(mParentFrame).selectFileToOpen("Open Compound File", CompoundFileHelper.cFileTypeCompoundFiles);
				mFirstGenerationFile = (file == null) ? null : file.getPath();
				}
			else if (startSetOption == COLUMN_OPTION)
				mFirstGenerationColumn = inquireColumnNameAndListName(true)[0];
			else if (startSetOption == SELECTED_OPTION)
				mFirstGenerationColumn = inquireColumnNameAndListName(true)[0];
			else if (startSetOption == LIST_OPTION) {
				String[] columnAndList = inquireColumnNameAndListName(false);
				mFirstGenerationColumn = columnAndList[0];
				mFirstGenerationList = columnAndList[1];
				}

			enableItems();
			updateStartGenerationPane();
			return;
			}

		if (e.getSource() == mCheckBoxDeferred) {
			enableItems();
			updateStartGenerationPane();
			return;
			}

		if (e.getSource() == mButtonDeferredDetails) {
			int startSetOption = mComboBoxStartCompounds.getSelectedIndex();
			if (startSetOption == COLUMN_OPTION
			 || startSetOption == SELECTED_OPTION
			 || startSetOption == LIST_OPTION)
				inquireColumnNameAndListName(startSetOption != LIST_OPTION);
			else if (startSetOption == FILE_OPTION)
				editDeferredFile();
			}

		if (e.getActionCommand().equals("addOption")) {
			int type = mComboBoxFitnessOption.getSelectedIndex() - FitnessPanel.SPECIAL_OPTION_COUNT;
			mFitnessPanel.add(FitnessPanel.createFitnessPanel(mParentFrame, this, type));
			mFitnessScrollpane.validate();
			mFitnessScrollpane.repaint();
			return;
			}
		}

	private void enableItems() {
		if (!mCheckBoxDeferred.isSelected()) {
			mFirstGenerationPane.setEnabled(true);
			mFirstGenerationPane.setMessage(null);
			}
		else {
			mFirstGenerationPane.setEnabled(false);
			mFirstGenerationPane.setMessage("1st generation compounds will be created when task is executed");
			}
		int startSetOption = mComboBoxStartCompounds.getSelectedIndex();
		mCheckBoxDeferred.setEnabled(startSetOption >= MIN_DEFERRED_OPTION);
		mButtonDeferredDetails.setEnabled(mCheckBoxDeferred.isSelected() && startSetOption >= MIN_DEFERRED_DETAIL_OPTION);
		}

	private void populateStartGeneration(int option) {
		mFirstGeneration.clear();
		if (option == DEFAULT_OPTION) {
			for (String idcode:DEFAULT_SET)
				mFirstGeneration.addCompound(new IDCodeParser(true).getCompactMolecule(idcode));
		}
		else if (option == RANDOM_OPTION) {
			int count = 4 * Integer.parseInt((String)mComboBoxSurvivalCount.getSelectedItem());
			ConcurrentLinkedQueue<StereoMolecule> compounds = createRandomStartSet(count, mComboBoxCreateLike.getSelectedIndex());
			while (!compounds.isEmpty())
				mFirstGeneration.addCompound(compounds.poll());
		}
		else if (option == FILE_OPTION) {
			if (mFirstGenerationFile != null) {
				File file = new File(mFirstGenerationFile);
				if (file.exists() && file.canRead()) {
					ArrayList<StereoMolecule> compounds = new FileHelper(mParentFrame).readStructuresFromFile(file, true);
					if (compounds != null)
						for (StereoMolecule mol:compounds)
							mFirstGeneration.addCompound(mol);
				}
			}
		}
		else if (option == CLIPBOARD_OPTION) {
			ArrayList<StereoMolecule> compounds = new ClipboardHandler().pasteMolecules();
			if (compounds != null)
				for (StereoMolecule mol:compounds)
					mFirstGeneration.addCompound(mol);
		}
		else if (option != CUSTOM_OPTION) {
			ArrayList<MoleculeWithDescriptor> mwdl = getMoleculesFromColumn(option, null, mFirstGenerationColumn, mFirstGenerationList);
			if (mwdl != null)
				for (MoleculeWithDescriptor mwd:mwdl)
					mFirstGeneration.addCompound(mwd.mMol);
			}
		}

	public void editDeferredFile() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, TableLayout.FILL, TableLayout.PREFERRED, gap},
				{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap } };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		JDialog dialog = new JDialog(mParentFrame, "Select 1st-Generation File", true);
		JFilePathLabel filePathLabel = new JFilePathLabel(true);
		JButton buttonEdit = new JButton(JFilePathLabel.BUTTON_TEXT);
		JCheckBox checkBoxChooseDuringMacro = new JCheckBox("Choose file during macro execution");

		ActionListener listener = e -> {
			if (e.getActionCommand().equals("OK")) {
				mFirstGenerationFile = checkBoxChooseDuringMacro.isSelected() ? null : filePathLabel.getPath();
				dialog.dispose();
				return;
				}

			if (e.getSource() == buttonEdit) {
				File file = new FileHelper(mParentFrame).selectFileToOpen("Select 1st-Generation File",
						CompoundFileHelper.cFileTypeCompoundFiles, mFirstGenerationFile);
				if (file != null) {
					filePathLabel.setPath(file.getAbsolutePath());
					checkBoxChooseDuringMacro.setSelected(false);
					}
				}

			if (e.getSource() == checkBoxChooseDuringMacro) {
				buttonEdit.setEnabled(!checkBoxChooseDuringMacro.isSelected());
				filePathLabel.setEnabled(!checkBoxChooseDuringMacro.isSelected());
				}

			if (e.getSource() == filePathLabel) {
				checkBoxChooseDuringMacro.setSelected(filePathLabel.getPath() == null);
				}

			dialog.getRootPane().getDefaultButton().setEnabled(checkBoxChooseDuringMacro.isSelected() || filePathLabel.getPath() != null);
			};

		filePathLabel.setPath(mFirstGenerationFile);
		filePathLabel.setListener(listener);
		content.add(new JLabel("File:"), "1,1");
		content.add(filePathLabel, "3,1");

		buttonEdit.addActionListener(listener);
		content.add(buttonEdit, "5,1");

		checkBoxChooseDuringMacro.setSelected(filePathLabel.getPath() == null);
		checkBoxChooseDuringMacro.addActionListener(listener);
		content.add(checkBoxChooseDuringMacro, "1,3,5,3");

		dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
		dialog.getContentPane().setLayout(new BorderLayout());
		dialog.getContentPane().add(content, BorderLayout.CENTER);
		dialog.getContentPane().add(createButtonPanel(dialog, listener), BorderLayout.SOUTH);

		dialog.pack();
		dialog.setLocationRelativeTo(mParentFrame);
		dialog.setVisible(true);
	}

	public String[] inquireColumnNameAndListName(boolean columnNameOnly) {
		String[] columnNameAndListName = new String[2];

		int[] structureColumns = mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeIDCode);

		CompoundTableListHandler hh = mTableModel.getListHandler();

		if (mTask.isInteractive() || !mCheckBoxDeferred.isSelected()) {
			if (structureColumns == null) {
				mTask.showInteractiveTaskMessage("No column with chemical structures found.", AbstractTask.WARNING_MESSAGE);
				return columnNameAndListName;
				}

			if (!columnNameOnly && hh.getListCount() == 0) {
				mTask.showInteractiveTaskMessage("Your DataWarrior document doesn't contain any row list.", AbstractTask.WARNING_MESSAGE);
				return columnNameAndListName;
				}

			if (structureColumns.length == 1) {
				columnNameAndListName[0] = mTableModel.getColumnTitleNoAlias(structureColumns[0]);
				if (columnNameOnly)
					return columnNameAndListName;
				}
			}

		final int scaled8 = HiDPIHelper.scale(8);
		double[][] size = { { scaled8, TableLayout.PREFERRED, scaled8, TableLayout.PREFERRED, scaled8 },
							{ scaled8, TableLayout.PREFERRED, scaled8, TableLayout.PREFERRED, scaled8 } };
		JPanel content  = new JPanel();
		content.setLayout(new TableLayout(size));

		JComboBox<String> comboBox1 = (columnNameAndListName[0] == null) ? new JComboBox<>() : null;
		if (comboBox1 != null) {
			comboBox1.setEditable(true);
			for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
				if (mTableModel.isColumnTypeStructure(i))
					comboBox1.addItem(mTableModel.getColumnTitle(i));
			if (mFirstGenerationColumn != null) {
				int column = mTableModel.findColumn(mFirstGenerationColumn);
				if (mTableModel.isColumnTypeStructure(column))
					comboBox1.setSelectedItem(mTableModel.getColumnTitle(column));
				}

			content.add(new JLabel("Structure column:"), "1,1");
			content.add(comboBox1, "3,1");
			}

		JComboBox<String> comboBox2 = (!columnNameOnly && hh.getListCount() != 0) ? new JComboBox<>() : null;
		if (!columnNameOnly && hh.getListCount() != 0) {
			comboBox2.setEditable(true);
			for (int i = 0; i<hh.getListCount(); i++)
				comboBox2.addItem(hh.getListName(i));
			if (mFirstGenerationList != null)
				comboBox2.setSelectedItem(mFirstGenerationList);

			content.add(new JLabel("Row list:"), "1,3");
			content.add(comboBox2, "3,3");
			}

		JDialog dialog = new JDialog(mParentFrame, "Select Structure Column", true);

		ActionListener listener = e -> {
			if (e.getActionCommand().equals("OK")) {
				if (comboBox1 != null)
					columnNameAndListName[0] = (String)comboBox1.getSelectedItem();
				if (columnNameAndListName[0] != null && columnNameAndListName[0].length() == 0)
					columnNameAndListName[0] = null;
				if (columnNameAndListName[0] != null) {
					int column = mTableModel.findColumn(columnNameAndListName[0]);
					if (column != -1)
						columnNameAndListName[0] = mTableModel.getColumnTitleNoAlias(column);
					if (comboBox2 != null) {
						columnNameAndListName[1] = (String)comboBox2.getSelectedItem();
						if (columnNameAndListName[1] != null && columnNameAndListName[1].length() == 0)
							columnNameAndListName[1] = null;
						}
					}
				dialog.dispose();
				}
			};

		dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
		dialog.getContentPane().setLayout(new BorderLayout());
		dialog.getContentPane().add(content, BorderLayout.CENTER);
		dialog.getContentPane().add(createButtonPanel(dialog, listener), BorderLayout.SOUTH);

		dialog.pack();
		dialog.setLocationRelativeTo(mParentFrame);
		dialog.setVisible(true);

		return columnNameAndListName;
		}

	private JPanel createButtonPanel(JDialog dialog, ActionListener al) {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {TableLayout.FILL, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap},
				{gap, TableLayout.PREFERRED, gap} };

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new TableLayout(size));
		JButton bok = new JButton("OK");
		bok.addActionListener(al);
		buttonPanel.add(bok, "3,1");
		dialog.getRootPane().setDefaultButton(bok);
		return buttonPanel;
		}

	protected ArrayList<MoleculeWithDescriptor> getMoleculesFromColumn(int startSetOption, String descriptorType,
	                                                                   String columnName, String listName) {
		long mask = 0;
		int idcodeColumn = mTableModel.findColumn(columnName);
		if (startSetOption != COLUMN_OPTION) {
			CompoundTableListHandler hh = mTableModel.getListHandler();
			int listIndex = (startSetOption == SELECTED_OPTION) ? CompoundTableListHandler.LISTINDEX_SELECTION : hh.getListIndex(listName);
			mask = hh.getListMask(listIndex);
			}
		int descriptorColumn = (descriptorType == null) ? -1 : mTableModel.getChildColumn(idcodeColumn, descriptorType);
		ArrayList<MoleculeWithDescriptor> moleculeList = new ArrayList<MoleculeWithDescriptor>();
		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			CompoundRecord record = mTableModel.getTotalRecord(row);
			if (mask == 0 || (record.getFlags() & mask) != 0) {
				StereoMolecule mol = mTableModel.getChemicalStructure(record, idcodeColumn, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
				if (mol != null) {
					Object descriptor = (descriptorColumn == -1) ? null : record.getData(descriptorColumn);
					moleculeList.add(new MoleculeWithDescriptor(mol, descriptor));
					}
				}
			}
		return (moleculeList.size() == 0) ? null : moleculeList;
		}

	/**
	 * @param molCount
	 * @param kind
	 * @return
	 */
	private ConcurrentLinkedQueue<StereoMolecule> createRandomStartSet(int molCount, int kind) {
		ConcurrentLinkedQueue<StereoMolecule> moleculeQueue = new ConcurrentLinkedQueue<>();
		JProgressDialog pd = new JProgressDialog(mParentFrame);

		new Thread(() -> {
			DECompoundProvider.createRandomCompounds(pd, molCount, kind, moleculeQueue);

			SwingUtilities.invokeLater(() -> {
				pd.setVisible(false);
				pd.dispose();
				} );
			} ).start();

		pd.setVisible(true);

		return moleculeQueue;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		if (mCheckBoxDeferred.isSelected()) {
			configuration.setProperty(PROPERTY_START_SET_DEFERRED, "true");
			int startSetOption = mComboBoxStartCompounds.getSelectedIndex();
			configuration.setProperty(PROPERTY_START_SET_OPTION, START_COMPOUND_CODE[startSetOption]);
			if (startSetOption == FILE_OPTION && mFirstGenerationFile != null) {
				configuration.setProperty(PROPERTY_START_SET_FILE, mFirstGenerationFile);
				}
			else if (startSetOption == COLUMN_OPTION
				  || startSetOption == SELECTED_OPTION
				  || startSetOption == LIST_OPTION) {
				configuration.setProperty(PROPERTY_START_SET_COLUMN, mFirstGenerationColumn);

				if (startSetOption == LIST_OPTION) {
					configuration.setProperty(PROPERTY_START_SET_LIST, mFirstGenerationList);
					}
				}
			}
		else if (mFirstGeneration.getSize() != 0) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i<mFirstGeneration.getSize(); i++) {
				if (sb.length() != 0)
					sb.append('\t');
				sb.append(new Canonizer(mFirstGeneration.getMolecule(i), Canonizer.ENCODE_ATOM_SELECTION).getIDCode());
				}
			configuration.setProperty(PROPERTY_START_COMPOUNDS, sb.toString());
			}

		configuration.setProperty(PROPERTY_SURVIVAL_COUNT, (String)mComboBoxSurvivalCount.getSelectedItem());
		configuration.setProperty(PROPERTY_GENERATION_COUNT, (String)mComboBoxGenerations.getSelectedItem());
		configuration.setProperty(PROPERTY_GENERATION_SIZE, (String)mComboBoxCompounds.getSelectedItem());
		configuration.setProperty(PROPERTY_RUN_COUNT, (String)mComboBoxRuns.getSelectedItem());

		configuration.setProperty(PROPERTY_COMPOUND_KIND, COMPOUND_KIND_CODE[mComboBoxCreateLike.getSelectedIndex()]);

		int fitnessOptionCount = mFitnessPanel.getComponentCount();
		configuration.setProperty(PROPERTY_FITNESS_PARAM_COUNT, Integer.toString(fitnessOptionCount));
		for (int i=0; i<fitnessOptionCount; i++) {
			FitnessPanel fp = (FitnessPanel)mFitnessPanel.getComponent(i);
			configuration.setProperty(PROPERTY_FITNESS_PARAM_CONFIG+i, fp.getConfiguration());
			}

		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mDiableEvents = true;

		int option = AbstractTask.findListIndex(configuration.getProperty(PROPERTY_START_SET_OPTION), START_COMPOUND_CODE, DEFAULT_OPTION);
		boolean isDeferred = "true".equals(configuration.getProperty(PROPERTY_START_SET_DEFERRED));

		// for legacy compatibility:
		if ("onthefly".equals(configuration.getProperty(PROPERTY_START_SET_OPTION))) {
			option = RANDOM_OPTION;
			isDeferred = true;
			}

		mComboBoxStartCompounds.setSelectedIndex(option);
		if (option == COLUMN_OPTION
		 || option == SELECTED_OPTION
		 || option == LIST_OPTION) {
			mFirstGenerationColumn = configuration.getProperty(PROPERTY_START_SET_COLUMN);

			if (option == LIST_OPTION)
				mFirstGenerationList = configuration.getProperty(PROPERTY_START_SET_LIST);
			}

		if (option == FILE_OPTION) {
			mFirstGenerationFile = configuration.getProperty(PROPERTY_START_SET_FILE);
			}

		mCheckBoxDeferred.setSelected(isDeferred);

		String startSet = configuration.getProperty(PROPERTY_START_COMPOUNDS, "");
		if (startSet.length() != 0)
			for (String idcode:configuration.getProperty(PROPERTY_START_COMPOUNDS, "").split("\\t"))
				mFirstGeneration.addCompound(new IDCodeParser(true).getCompactMolecule(idcode));

		mComboBoxSurvivalCount.setSelectedItem(configuration.getProperty(PROPERTY_SURVIVAL_COUNT, DEFAULT_SURVIVALS));
		mComboBoxGenerations.setSelectedItem(configuration.getProperty(PROPERTY_GENERATION_COUNT, DEFAULT_GENERATIONS));
		mComboBoxCompounds.setSelectedItem(configuration.getProperty(PROPERTY_GENERATION_SIZE, DEFAULT_COMPOUNDS));
		mComboBoxRuns.setSelectedItem(configuration.getProperty(PROPERTY_RUN_COUNT, DEFAULT_RUNS));

		mComboBoxCreateLike.setSelectedIndex(AbstractTask.findListIndex(configuration.getProperty(PROPERTY_COMPOUND_KIND), COMPOUND_KIND_CODE, 0));

		int fitnessOptionCount = Integer.parseInt(configuration.getProperty(PROPERTY_FITNESS_PARAM_COUNT));
		for (int i=0; i<fitnessOptionCount; i++) {
			String config = configuration.getProperty(PROPERTY_FITNESS_PARAM_CONFIG+i);
			if (config != null)
				mFitnessPanel.add(FitnessPanel.createFitnessPanel(mParentFrame, this, config));
			}

		enableItems();

		mDiableEvents = false;
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mDiableEvents = true;
		mCheckBoxDeferred.setSelected(false);
		mComboBoxSurvivalCount.setSelectedItem(DEFAULT_SURVIVALS);
		mComboBoxGenerations.setSelectedItem(DEFAULT_GENERATIONS);
		mComboBoxCompounds.setSelectedItem(DEFAULT_COMPOUNDS);
		mComboBoxRuns.setSelectedItem(DEFAULT_RUNS);

		mComboBoxCreateLike.setSelectedIndex(0);

		enableItems();
		updateStartGenerationPane();

		mDiableEvents = false;
		}

	private void updateStartGenerationPane() {
		if (mCheckBoxDeferred.isSelected())
			mFirstGenerationPane.getModel().clear();
		else
			populateStartGeneration(mComboBoxStartCompounds.getSelectedIndex());
		}
	}
