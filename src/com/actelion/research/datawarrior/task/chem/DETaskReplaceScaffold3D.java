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

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.IDCodeParserWithoutCoordinateInvention;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.forcefield.mmff.ForceFieldMMFF94;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.io.DWARFileParser;
import com.actelion.research.chem.shredder.FragmentGeometry3D;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.fx.EditableSmallMolMenuController;
import com.actelion.research.datawarrior.fx.JFXMolViewerPanel;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.datawarrior.task.file.JFilePathLabel;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.VerticalFlowLayout;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;
import org.openmolecules.fx.viewer3d.V3DScene;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;
import java.util.concurrent.SynchronousQueue;

public class DETaskReplaceScaffold3D extends ConfigurableTask implements ActionListener {
	static final long serialVersionUID = 0x20140205;

	private static final String PROPERTY_QUERY = "query";
	private static final String PROPERTY_SIMILARITY = "similarity";
	private static final String PROPERTY_IN_FILE_NAME = "inFile";
	private static final String PROPERTY_MINIMIZE = "minimize";


	public static final String TASK_NAME = "Replace Scaffold By 3D-Fragment";
	private static final int MIN_SIMILARITY = 60;
	private static final int DEFAULT_SIMILARITY = 80;


	private DEFrame mSourceFrame,mTargetFrame;
	private CompoundTableModel mTableModel;
	private JFXMolViewerPanel mConformerPanel;
	private JCheckBox mCheckBoxMinimize;
	private JSlider mSimilaritySlider;
	private JFilePathLabel mLabelInFileName;
	private volatile SynchronousQueue<StereoMolecule> mFragmentQueue;
	private volatile SynchronousQueue<byte[][]> mResultTableQueue;
	private volatile int mThreadCount;
	private volatile StereoMolecule mQueryMol;
	private volatile FragmentGeometry3D mQueryGeometry;


	public DETaskReplaceScaffold3D(DEFrame parent) {
		super(parent, true);
		mSourceFrame = parent;
		mTableModel = mSourceFrame.getTableModel();
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return mTargetFrame;
		}

	@Override
	public boolean isConfigurable() {
		return true;
		}

	@Override
	public JPanel createDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.FILL, gap},
							{gap, TableLayout.PREFERRED, gap>>1, TableLayout.PREFERRED, gap>>1, TableLayout.PREFERRED,
									2*gap, TableLayout.PREFERRED, gap>>1, TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED, gap} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		EnumSet<V3DScene.ViewerSettings> settings = V3DScene.CONFORMER_VIEW_MODE;
		settings.add(V3DScene.ViewerSettings.EDITING);
		mConformerPanel = new JFXMolViewerPanel(false, settings);
		mConformerPanel.adaptToLookAndFeelChanges();
//		mConformerPanel.setBackground(new java.awt.Color(24, 24, 96));
		mConformerPanel.setPreferredSize(new Dimension(HiDPIHelper.scale(400), HiDPIHelper.scale(200)));
		mConformerPanel.setPopupMenuController(new EditableSmallMolMenuController(getParentFrame(), mConformerPanel));

		content.add(mConformerPanel, "1,1,3,1");
		content.add(new JLabel("First, define bio-active molecule.", JLabel.CENTER), "1,3,3,3");
		content.add(new JLabel("Then, select scaffold atoms to be replaced.", JLabel.CENTER), "1,5,3,5");

		content.add(new JLabel("3D-Fragment File:"), "1,7");
		mLabelInFileName = new JFilePathLabel(!isInteractive());
		content.add(mLabelInFileName, "3,7");

//		JPanel inFilePanel = new JPanel();
//		inFilePanel.setLayout(new BorderLayout());
//		inFilePanel.add(new JLabel("3D-Fragment File:  "), BorderLayout.WEST);
//		mLabelInFileName = new JFilePathLabel(!isInteractive());
//		inFilePanel.add(mLabelInFileName, BorderLayout.CENTER);
//		content.add(inFilePanel, "1,6");

		JButton buttonEdit = new JButton(JFilePathLabel.BUTTON_TEXT);
		buttonEdit.addActionListener(this);
		content.add(buttonEdit, "1,9");

		createSimilaritySlider();

		mCheckBoxMinimize = new JCheckBox("Energy-minimize structures after scaffold replacement");
		content.add(mCheckBoxMinimize, "1,11,3,11");

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

//	@Override
//	public String getHelpURL() {
//		return "/html/help/chemistry.html#ReplaceScaffold";
//		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String idcode = configuration.getProperty(PROPERTY_QUERY);
		if (idcode == null) {
			showErrorMessage("You didn't define a bio-active conformer.");
			return false;
			}
		StereoMolecule query = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(idcode);
		if (query.getAllAtoms() == 0) {
			showErrorMessage("Your query molecule doesn't contain atoms.");
			return false;
			}
		int selectedAtomCount = 0;
		query.ensureHelperArrays(Molecule.cHelperNeighbours);
		for (int atom=0; atom<query.getAllAtoms(); atom++) {
			if (query.isSelectedAtom(atom))
				selectedAtomCount++;
			else {
				int selectedNeighbourCount = 0;
				for (int i=0; i<query.getAllConnAtoms(atom); i++)
					if (query.isSelectedAtom(query.getConnAtom(atom, i)))
						selectedNeighbourCount++;
				if (selectedNeighbourCount > 1) {
					showErrorMessage("An exit vector atom must not connect to more than one core atoms.");
					return false;
				}
			}
		}
		if (selectedAtomCount < 3) {
			showErrorMessage("Your query molecule doesn't contain sufficient selected atoms.");
			return false;
		}
		if (query.getAllAtoms() > selectedAtomCount + 2) {
			showErrorMessage("Your query molecule needs at least two non-selected atoms.");
			return false;
		}

		for (int atom=0; atom<query.getAllAtoms(); atom++)
			if (query.isSelectedAtom(atom))
				query.setAtomMarker(atom, true);
		if (query.getFragmentNumbers(new int[query.getAllAtoms()], true, true) > 1) {
			showErrorMessage("Your atom selection covers multiple disconnected core structures.");
			return false;
		}

		String inFileName = configuration.getProperty(PROPERTY_IN_FILE_NAME);
		if (isLive && !isFileAndPathValid(inFileName, false, false))
			return false;

		int index = inFileName.lastIndexOf('.');
		String extension = (index == -1) ? "" : inFileName.substring(index+1).toLowerCase();
		if (!extension.equals("dwar")) {
			showErrorMessage("Input file is not a native DataWarrior file.");
			return false;
			}

		if (isLive) {
			DWARFileParser parser = new DWARFileParser(inFileName);
			boolean has3DStructures = parser.hasStructureCoordinates3D();
			parser.close();
			if (!has3DStructures) {
				showErrorMessage("The DataWarrior input-file doesn't contain 3-dimensional structures.");
				return false;
				}
			}

		return true;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String value = configuration.getProperty(PROPERTY_QUERY);
		if (value != null) {
			StereoMolecule query = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(value);
			mConformerPanel.addMolecule(query, null, null);
			mConformerPanel.optimizeView();
			}

		value = configuration.getProperty(PROPERTY_IN_FILE_NAME);
		mLabelInFileName.setPath(value == null ? null : isFileAndPathValid(value, false, false) ? value : null);

		value = configuration.getProperty(PROPERTY_SIMILARITY, Integer.toString(DEFAULT_SIMILARITY));
		try { mSimilaritySlider.setValue(Math.min(100, Math.max(MIN_SIMILARITY, Integer.parseInt(value)))); }
		catch (NumberFormatException nfe) { mSimilaritySlider.setValue(DEFAULT_SIMILARITY); }

		mCheckBoxMinimize.setSelected("true".equals(configuration.getProperty(PROPERTY_MINIMIZE, "true")));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mSimilaritySlider.setValue(DEFAULT_SIMILARITY);
		mCheckBoxMinimize.setSelected(true);
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		ArrayList<StereoMolecule> mols = mConformerPanel.getMolecules(null);
		if (!mols.isEmpty() && mols.get(0).getAllAtoms() != 0) {
			Canonizer canonizer = new Canonizer(mols.get(0), Canonizer.ENCODE_ATOM_SELECTION);
			String idcode = canonizer.getIDCode() + " " + canonizer.getEncodedCoordinates();
			configuration.setProperty(PROPERTY_QUERY, idcode);
			}

		String fileName = mLabelInFileName.getPath();
		if (fileName != null)
			configuration.setProperty(PROPERTY_IN_FILE_NAME, fileName);

		configuration.setProperty(PROPERTY_SIMILARITY, ""+mSimilaritySlider.getValue());

		configuration.setProperty(PROPERTY_MINIMIZE, mCheckBoxMinimize.isSelected() ? "true" : "false");

		return configuration;
		}

	private String askForCompoundFile(String selectedFile) {
		File file = new FileHelper(getParentFrame()).selectFileToOpen(
				"Open Compound File", FileHelper.cFileTypeSD | FileHelper.cFileTypeDataWarrior, selectedFile);
		return (file == null) ? null : file.getPath();
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(JFilePathLabel.BUTTON_TEXT)) {
			String path = askForCompoundFile(resolvePathVariables(mLabelInFileName.getPath()));
			if (path != null) {
				mLabelInFileName.setPath(path);
				}
			}
		}

	@Override
	public void runTask(Properties configuration) {
		final String fileName = resolvePathVariables(configuration.getProperty(PROPERTY_IN_FILE_NAME));
		final boolean minimize = "true".equals(configuration.getProperty(PROPERTY_MINIMIZE, "true"));

		mQueryMol = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(configuration.getProperty(PROPERTY_QUERY));
		mQueryMol.ensureHelperArrays(Molecule.cHelperParities);
		mQueryGeometry = new FragmentGeometry3D(mQueryMol, FragmentGeometry3D.MODE_SELECTED_ATOMS);

		mThreadCount = Runtime.getRuntime().availableProcessors();
		mFragmentQueue = new SynchronousQueue<>();
		for (int i=0; i<mThreadCount; i++) {
			Thread t = new Thread(() -> consumeFragments(minimize), "Scaffold Replacement Calculator " + (i + 1));
			t.setPriority(Thread.MIN_PRIORITY);
			t.start();
			}

		ForceFieldMMFF94.initialize(ForceFieldMMFF94.MMFF94SPLUS);

		mResultTableQueue = new SynchronousQueue<>();
		Thread collectionThread = new Thread(() -> consumeResultRows());
		collectionThread.start();

		DWARFileParser parser = new DWARFileParser(fileName);
		while (parser.next()) {
			String idcode = parser.getIDCode();
			String coords = parser.getCoordinates3D();
			StereoMolecule mol = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(idcode, coords);
			if (mol != null && mol.getAllAtoms() != 0)
				mFragmentQueue.add(mol);
			}

		StereoMolecule empty = new StereoMolecule();
		for (int i=0; i<mThreadCount; i++)
			mFragmentQueue.add(empty);

		// the controller thread must wait until all others are finished
		// before the next task can begin or the dialog is closed
		try { collectionThread.join(); } catch (InterruptedException e) {}
		}

	private void consumeFragments(final boolean minimize) {
		StereoMolecule fragment;
		try {
			while ((fragment = mFragmentQueue.take()).getAllAtoms() != 0)
				generateReplacements(fragment, minimize);
		} catch (InterruptedException ie) {}
		mResultTableQueue.add(new byte[0][]);
	}

	private void generateReplacements(StereoMolecule fragment, boolean minimize) {
		FragmentGeometry3D fragmentGeometry = new FragmentGeometry3D(fragment, FragmentGeometry3D.MODE_FRAGMENT_WITH_EXIT_VECTORS);
		if (!mQueryGeometry.equals(fragmentGeometry))
			return;

		for (int[] permutation : mQueryGeometry.getPermutations()) {
			float score = mQueryGeometry.align(fragmentGeometry, permutation);
/* TODO			if (score < LIMIT) {
				StereoMolecule queryWithNewCore = construct();
				minimize(queryWithNewCore);
				float rmsd = rmsd(mQuery, queryWithNewCore);
				if (rmsd < limit) {
					byte[][] row = constructRow(queryWithNewCore, score, rmsd);
					mResultTableQueue.addAll(row);
				}
			}
*/		}
	}

	private float minimize(StereoMolecule mol) {
		String tableSet = ForceFieldMMFF94.MMFF94SPLUS;
		ForceFieldMMFF94 ff = new ForceFieldMMFF94(mol, tableSet, new HashMap<>());
		int error = ff.minimise(10000, 0.0001, 1.0e-6);
		return error != 0 ? Float.NaN : (float)ff.getTotalEnergy();
		}

	private void consumeResultRows() {
		ArrayList<byte[][]> rowList = new ArrayList<>();
		try {
			for (int emptyCount=0; emptyCount<mThreadCount;) {
				byte[][] row = mResultTableQueue.take();
				if (row.length != 0)
					rowList.add(row);
				else
					emptyCount++;
				}
			} catch (InterruptedException ie) {}

		if (!rowList.isEmpty()) {
			mTargetFrame = DataWarrior.getApplication().getEmptyFrame("Scaffold Replacements");

			CompoundTableModel tableModel = mTargetFrame.getTableModel();
			tableModel.initializeTable(rowList.size(), 4);
			tableModel.setColumnName("Structure", 0);
			tableModel.setColumnProperty(0, CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnTypeIDCode);
			tableModel.setColumnName(CompoundTableConstants.cColumnType3DCoordinates, 1);
			tableModel.setColumnProperty(1, CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnType2DCoordinates);
			tableModel.setColumnProperty(1, CompoundTableConstants.cColumnPropertyParentColumn, "Structure");
			tableModel.prepareDescriptorColumn(2, 0, null, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
			tableModel.setColumnName("Scaffold RMSD", 3);

			for (int row=0; row<rowList.size(); row++) {
				tableModel.setTotalDataAt(rowList.get(row)[0], row, 0);
				tableModel.setTotalDataAt(rowList.get(row)[1], row, 0);
				}

			tableModel.finalizeTable(CompoundTableEvent.cSpecifierNoRuntimeProperties, this);
			}
		}
	}