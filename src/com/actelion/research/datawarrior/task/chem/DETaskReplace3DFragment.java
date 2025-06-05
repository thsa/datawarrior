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
import com.actelion.research.chem.alignment3d.PheSAAlignmentOptimizer;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.conf.TorsionDescriptor;
import com.actelion.research.chem.conf.TorsionDescriptorHelper;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorHandlerSkeletonSpheres;
import com.actelion.research.chem.forcefield.mmff.ForceFieldMMFF94;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.io.DWARFileParser;
import com.actelion.research.chem.phesaflex.FlexibleShapeAlignment;
import com.actelion.research.chem.shredder.FragmentGeometry3D;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.fx.EditableSmallMolMenuController;
import com.actelion.research.datawarrior.fx.JFXMolViewerPanel;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.datawarrior.task.file.JFilePathLabel;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationColor;
import com.actelion.research.table.view.VisualizationPanel2D;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;
import org.openmolecules.chem.conf.gen.ConformerGenerator;
import org.openmolecules.fx.viewer3d.V3DScene;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DETaskReplace3DFragment extends ConfigurableTask implements ActionListener {
	private static final int DEFAULT_ATOMS_LESS = 5;
	private static final int DEFAULT_ATOMS_MORE = 10;
	private static final double DEFAULT_MAX_EV_RMSD = 0.6;
	private static final double DEFAULT_MAX_EV_DIVERGENCE = 30;
	private static final double DEFAULT_MIN_PHESA_FLEX = 0.5;

	private static final String PROPERTY_QUERY = "query";
	private static final String PROPERTY_IN_FILE_NAME = "inFile";
	private static final String PROPERTY_ATOMS_LESS = "atomsLess";
	private static final String PROPERTY_ATOMS_MORE = "atomsMore";
	private static final String PROPERTY_MAX_EV_RMSD = "maxEVRMSD";
	private static final String PROPERTY_MAX_EV_DIV = "maxEVDivergence";
	private static final String PROPERTY_MIN_PHESA_FLEX = "minPhesaFlex";

	private static final String[] NEW_COLUMN_NAME = {
			"Structure", "MMFF94+ minimized & retained atoms aligned", "MMFF94+ minimized & PheSA aligned", "PheSA-flex aligned", DescriptorConstants.DESCRIPTOR_FFP512.shortName,
			"New Fragment", "New Fragment 3D", "Fragment RMSD", "Angle Divergence", "Retained Atom RMSD",
			"MMFF94+ PheSA Rigid Score", "PheSA Flex Score", "Scaffold Similarity", "Energy Dif Flex Rigid", "Conformer Percentage", "Conformer Energy Dif" };
	private static final int STRUCTURE_COLUMN = 0;
	private static final int COORDS3D_MINIMIZED_COLUMN = 1;
	private static final int COORDS3D_RIGID_COLUMN = 2;
	private static final int COORDS3D_FLEX_COLUMN = 3;
	private static final int FFP_COLUMN = 4;
	private static final int FRAGMENT_COLUMN = 5;
	private static final int FRAGMENT_COORDS_COLUMN = 6;
	private static final int FRAGMENT_RMSD_COLUMN = 7;
	private static final int FRAGMENT_ANGLE_COLUMN = 8;
	private static final int QUERY_RMSD_COLUMN = 9;
	private static final int PHESA_RIGID_COLUMN = 10;
	private static final int PHESA_FLEX_COLUMN = 11;
	private static final int SCAFFOLD_SIM_COLUMN = 12;
	private static final int ENERGY_DIF_PHESA_COLUMN = 13;
	private static final int CONFORMER_PERCENTAGE_COLUMN = 14;
	private static final int CONFORMER_ENERGY_DIF_COLUMN = 15;

	public static final String TASK_NAME = "Replace 3D-Fragment";

	private DEFrame mTargetFrame;
	private JFXMolViewerPanel mConformerPanel;
	private JTextField mTextFieldAtomsLess,mTextFieldAtomsMore,mTextFieldMaxEVRMSD,mTextFieldMaxEVDivergence,mTextFieldMinPheSAFlex;
	private JFilePathLabel mLabelInFileName;
	private volatile SynchronousQueue<StereoMolecule> mFragmentQueue;
	private volatile SynchronousQueue<byte[][]> mResultTableQueue;
	private volatile int mThreadCount;
	private volatile boolean mSeekLinker;
	private volatile double mMaxEVRMSD,mMaxEVDivergence,mMinPheSAFlex;
	private volatile String mOrigScaffoldIDCodeWithCoords;
	private volatile StereoMolecule mQueryMol;
	private volatile FragmentGeometry3D mQueryGeometry;
	private volatile AtomicInteger mPheSAFlexMatchCount,mEVTypeMatchCount,mEVAngleMatchCount,mEVPermutationCount,mEVRMSDMatchCount;
	private volatile Coordinates[] mQueryStaticAtomCoords;


	public DETaskReplace3DFragment(DEFrame parent) {
		super(parent, true);
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
									2*gap, TableLayout.PREFERRED, gap>>1, TableLayout.PREFERRED,
									2*gap, TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED, gap} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		EnumSet<V3DScene.ViewerSettings> settings = V3DScene.CONFORMER_VIEW_MODE;
		settings.add(V3DScene.ViewerSettings.EDITING);
		settings.add(V3DScene.ViewerSettings.ATOM_LEVEL_SELECTION);

		mConformerPanel = new JFXMolViewerPanel(false, settings);
		mConformerPanel.adaptToLookAndFeelChanges();
		mConformerPanel.setPreferredSize(new Dimension(HiDPIHelper.scale(400), HiDPIHelper.scale(200)));
		EditableSmallMolMenuController controller = new EditableSmallMolMenuController(getParentFrame(), mConformerPanel);
		controller.setMoleculeColor(javafx.scene.paint.Color.gray(0.7));
		mConformerPanel.setPopupMenuController(controller);

		content.add(mConformerPanel, "1,1,3,1");
		content.add(new JLabel("First, define a bio-active 3D-structure.", JLabel.CENTER), "1,3,3,3");
		content.add(new JLabel("Then, select atoms to be replaced by fragment.", JLabel.CENTER), "1,5,3,5");

		content.add(new JLabel("3D-fragment file:"), "1,7");
		mLabelInFileName = new JFilePathLabel(!isInteractive());
		content.add(mLabelInFileName, "3,7");

		JButton buttonEdit = new JButton(JFilePathLabel.BUTTON_TEXT);
		buttonEdit.addActionListener(this);
		content.add(buttonEdit, "1,9");

		mTextFieldAtomsLess = new JTextField(2);
		mTextFieldAtomsMore = new JTextField(2);
		JPanel atomPanel = new JPanel();
		atomPanel.add(new JLabel("Allow "));
		atomPanel.add(mTextFieldAtomsLess);
		atomPanel.add(new JLabel(" less and "));
		atomPanel.add(mTextFieldAtomsMore);
		atomPanel.add(new JLabel(" more non-H atoms than original"));
		content.add(atomPanel, "1,11,3,11");

		double[][] tpSize = { {TableLayout.FILL, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, TableLayout.FILL},
							  {TableLayout.PREFERRED, gap>>1, TableLayout.PREFERRED, gap>>1, TableLayout.PREFERRED, gap>>1, TableLayout.PREFERRED} };
		JPanel thresholdPanel = new JPanel();
		thresholdPanel.setLayout(new TableLayout(tpSize));

		mTextFieldMaxEVRMSD = new JTextField(2);
		mTextFieldMaxEVDivergence = new JTextField(2);
		mTextFieldMinPheSAFlex = new JTextField(2);

		thresholdPanel.add(new JLabel("Geometric constraints for 3D-fragments:"), "1,0,5,0");

		thresholdPanel.add(new JLabel("Maximum exit vector RMSD:"), "1,2");
		thresholdPanel.add(mTextFieldMaxEVRMSD, "3,2");

		thresholdPanel.add(new JLabel("Maximum exit vector angle divergence:"), "1,4");
		thresholdPanel.add(mTextFieldMaxEVDivergence, "3,4");
		thresholdPanel.add(new JLabel("degrees"), "5,4");

		thresholdPanel.add(new JLabel("Minimum PheSA-flex score"), "1,6");
		thresholdPanel.add(mTextFieldMinPheSAFlex, "3,6");

		content.add(thresholdPanel, "1,13,3,13");

		return content;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/conformers.html#ReplaceAndLink";
		}

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
		int selectedLonelyHydrogenCount = 0;
		query.ensureHelperArrays(Molecule.cHelperRings);
		for (int atom=0; atom<query.getAllAtoms(); atom++) {
			if (isLonelySelectedHydrogen(query, atom))
				selectedLonelyHydrogenCount++;
			else if (query.isSelectedAtom(atom))
				selectedAtomCount++;
			else {
				int selectedNeighbourCount = 0;
				for (int i=0; i<query.getAllConnAtoms(atom); i++)
					if (query.isSelectedAtom(query.getConnAtom(atom, i)))
						selectedNeighbourCount++;
				if (selectedNeighbourCount > 1) {
					showErrorMessage("Non-selected atoms must not connect to more than one selected atoms,\n"
									+"which means that none of the remaining query atoms is allowed to\n"
									+"connect to two different atoms of the new fragment at the same time.");
					return false;
				}
			}
		}

		if (selectedLonelyHydrogenCount < 2
		 && selectedAtomCount < 3) {
			showErrorMessage("Your query molecule doesn't contain sufficient selected atoms.");
			return false;
		}
		if (query.getAllAtoms() < selectedAtomCount + selectedLonelyHydrogenCount + 2) {
			showErrorMessage("Your query molecule needs at least two non-selected atoms.");
			return false;
		}

		int bondCount = 0;
		for (int bond=0; bond<query.getAllBonds(); bond++) {
			if (query.isSelectedAtom(query.getBondAtom(0, bond)) != query.isSelectedAtom(query.getBondAtom(1, bond))) {
				if (query.getBondOrder(bond) != 1) {
					showErrorMessage("You may not cut double or triple bonds.");
					return false;
				}
				if (query.isAromaticBond(bond)) {
					showErrorMessage("You may not cut delocalized bonds.");
					return false;
				}
				bondCount++;
			}
		}
		if (bondCount > DETaskBuild3DFragmentLibrary.MAX_EXIT_VECTORS) {
			showErrorMessage("You may not cut more than "+DETaskBuild3DFragmentLibrary.MAX_EXIT_VECTORS+" bonds.");
			return false;
		}

//		for (int atom=0; atom<query.getAllAtoms(); atom++)
//			if (query.isSelectedAtom(atom) && !isLonelySelectedHydrogen(query, atom))
//				query.setAtomMarker(atom, true);
//		if (query.getFragmentNumbers(new int[query.getAllAtoms()], true, true) > 1) {
//			showErrorMessage("Your atom selection covers multiple disconnected core structures.");
//			return false;
//		}

		try {
			int less = Integer.parseInt(configuration.getProperty(PROPERTY_ATOMS_LESS));
			int more = Integer.parseInt(configuration.getProperty(PROPERTY_ATOMS_MORE));
			if (less + more < 0) {
				showErrorMessage("While negative values are allowed,\nyour definition of the allowed atom span is excludes everything.");
				return false;
			}
		} catch (NumberFormatException nfe) {
			showErrorMessage("The setting for the allowed non-hydrogen atom span is not numerical.");
			return false;
		}

		try {
			double maxRMSD = Double.parseDouble(configuration.getProperty(PROPERTY_MAX_EV_RMSD));
			if (maxRMSD < 0 || maxRMSD > 10) {
				showErrorMessage("The maximum exit vector RMSD must be between 0 and 10.");
				return false;
			}
			double maxDiv = Double.parseDouble(configuration.getProperty(PROPERTY_MAX_EV_DIV));
			if (maxDiv < 0 || maxDiv > 90) {
				showErrorMessage("The maximum exit vector angle divergence must be between 0 and 90 degrees.");
				return false;
			}
			double minPhesa = Double.parseDouble(configuration.getProperty(PROPERTY_MIN_PHESA_FLEX));
			if (minPhesa < 0 || minPhesa > 1) {
				showErrorMessage("The minimum PheSA-flex cutoff value must be between 0.0 and 1.0.");
				return false;
			}
		} catch (NumberFormatException nfe) {
			showErrorMessage("At least one of your geometry constraints is not numerical.");
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
			DWARFileParser parser = new DWARFileParser(inFileName, DWARFileParser.MODE_COORDINATES_PREFER_3D);
			boolean has3DStructures = parser.hasStructureCoordinates3D();
			if (!parser.next()) {
				showErrorMessage("The supplied fragment file doesn't contain any rows.");
				return false;
				}
			StereoMolecule fragment = parser.getMolecule();
			parser.close();

			if (!has3DStructures) {
				showErrorMessage("The supplied fragment file doesn't contain 3-dimensional structures.");
				return false;
			}

			boolean attachmentPointFound = false;
			for (int atom=0; atom<fragment.getAllAtoms(); atom++) {
				if (fragment.getAtomicNo(atom) == 0 || "*".equals(fragment.getAtomCustomLabel(atom))) {
					attachmentPointFound = true;
					break;
					}
				}
			if (!attachmentPointFound) {
				showErrorMessage("The supplied fragment file's 3D-structures don't contains attachment points.");
				return false;
				}
			}

		return true;
		}

	private boolean isLonelySelectedHydrogen(StereoMolecule query, int atom) {
		return query.isSelectedAtom(atom)
			&& query.getAtomicNo(atom) == 1
			&& query.getConnAtoms(atom) == 1
			&& !query.isSelectedAtom(query.getConnAtom(atom, 0));
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

		mTextFieldAtomsLess.setText(configuration.getProperty(PROPERTY_ATOMS_LESS, Integer.toString(DEFAULT_ATOMS_LESS)));
		mTextFieldAtomsMore.setText(configuration.getProperty(PROPERTY_ATOMS_MORE, Integer.toString(DEFAULT_ATOMS_MORE)));
		mTextFieldMaxEVRMSD.setText(configuration.getProperty(PROPERTY_MAX_EV_RMSD, Double.toString(DEFAULT_MAX_EV_RMSD)));
		mTextFieldMaxEVDivergence.setText(configuration.getProperty(PROPERTY_MAX_EV_DIV, Double.toString(DEFAULT_MAX_EV_DIVERGENCE)));
		mTextFieldMinPheSAFlex.setText(configuration.getProperty(PROPERTY_MIN_PHESA_FLEX, Double.toString(DEFAULT_MIN_PHESA_FLEX)));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mTextFieldAtomsLess.setText(Integer.toString(DEFAULT_ATOMS_LESS));
		mTextFieldAtomsMore.setText(Integer.toString(DEFAULT_ATOMS_MORE));
		mTextFieldMaxEVRMSD.setText(Double.toString(DEFAULT_MAX_EV_RMSD));
		mTextFieldMaxEVDivergence.setText(Double.toString(DEFAULT_MAX_EV_DIVERGENCE));
		mTextFieldMinPheSAFlex.setText(Double.toString(DEFAULT_MIN_PHESA_FLEX));
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		ArrayList<StereoMolecule> mols = mConformerPanel.getMolecules(null);
		if (!mols.isEmpty() && mols.get(0).getAllAtoms() != 0) {
			mols.get(0).center(); // We need to center because of PheSA-flex scoring, which doesn't work with non-centered molecules.
			Canonizer canonizer = new Canonizer(mols.get(0), Canonizer.ENCODE_ATOM_SELECTION);
			String idcode = canonizer.getIDCode() + " " + canonizer.getEncodedCoordinates();
			configuration.setProperty(PROPERTY_QUERY, idcode);
			}

		String fileName = mLabelInFileName.getPath();
		if (fileName != null)
			configuration.setProperty(PROPERTY_IN_FILE_NAME, fileName);

		configuration.setProperty(PROPERTY_ATOMS_LESS, mTextFieldAtomsLess.getText());
		configuration.setProperty(PROPERTY_ATOMS_MORE, mTextFieldAtomsMore.getText());
		configuration.setProperty(PROPERTY_MAX_EV_RMSD, mTextFieldMaxEVRMSD.getText());
		configuration.setProperty(PROPERTY_MAX_EV_DIV, mTextFieldMaxEVDivergence.getText());
		configuration.setProperty(PROPERTY_MIN_PHESA_FLEX, mTextFieldMinPheSAFlex.getText());

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

		mMaxEVRMSD = Double.parseDouble(configuration.getProperty(PROPERTY_MAX_EV_RMSD));
		mMaxEVDivergence = Double.parseDouble(configuration.getProperty(PROPERTY_MAX_EV_DIV));
		mMinPheSAFlex = Double.parseDouble(configuration.getProperty(PROPERTY_MIN_PHESA_FLEX));

		String queryIDCode = configuration.getProperty(PROPERTY_QUERY);
		mQueryMol = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(queryIDCode);
		mQueryMol.ensureHelperArrays(Molecule.cHelperParities);
		mQueryGeometry = new FragmentGeometry3D(mQueryMol, FragmentGeometry3D.MODE_SELECTED_ATOMS);

		int selectedAtomCount = 0;
		boolean[] includeAtom = new boolean[mQueryMol.getAllAtoms()];
		for (int atom=0; atom<mQueryMol.getAllAtoms(); atom++) {
			if (mQueryMol.isSelectedAtom(atom)) {
				includeAtom[atom] = true;
				selectedAtomCount++;
			}
		}

		// Copy selected part and determine, whether we have multiple disconnected fragments, i.e. whether we look for a linker!
		StereoMolecule testMol = new StereoMolecule();
		mQueryMol.copyMoleculeByAtoms(testMol, includeAtom, false, null);
		mSeekLinker = (testMol.getFragmentNumbers(new int[testMol.getAllAtoms()], false, true) > 1);

		// Extend selection by one atom to include exit atoms (and possibly missed hydrogens)
		for (int atom=0; atom<mQueryMol.getAllAtoms(); atom++) {
			if (mQueryMol.isSelectedAtom(atom)) {
				for (int i = 0; i<mQueryMol.getAllConnAtoms(atom); i++) {
					int connAtom = mQueryMol.getConnAtom(atom, i);
					if (!mQueryMol.isSelectedAtom(connAtom)) {
						includeAtom[connAtom] = true;
						if (connAtom >=mQueryMol.getAtoms()) {	// select attached hydrogens, if they were forgotten to select
							mQueryMol.setAtomSelection(connAtom, true);
							selectedAtomCount++;
						}
					}
				}
			}
		}

		mQueryStaticAtomCoords = new Coordinates[mQueryMol.getAllAtoms()-selectedAtomCount];
		int index = 0;
		for (int atom=0; atom<mQueryMol.getAllAtoms(); atom++)
			if (!mQueryMol.isSelectedAtom(atom))
				mQueryStaticAtomCoords[index++] = mQueryMol.getCoordinates(atom);

		// Copy selected part of query molecule including exit atoms and convert them to attachment points (add label "*")
		StereoMolecule origScaffold = new StereoMolecule();
		int[] atomMap = new int[mQueryMol.getAllAtoms()];
		mQueryMol.copyMoleculeByAtoms(origScaffold, includeAtom, false, atomMap);
		for (int atom=0; atom<mQueryMol.getAllAtoms(); atom++)
			if (mQueryMol.isSelectedAtom(atom))
				for (int i=0; i<mQueryMol.getAllConnAtoms(atom); i++)
					if (!mQueryMol.isSelectedAtom(mQueryMol.getConnAtom(atom, i)))
						origScaffold.setAtomCustomLabel(atomMap[mQueryMol.getConnAtom(atom, i)], "*");

		// Create idcode and skelSpheres from original scaffold with exit atoms
		origScaffold.setFragment(false);
		Canonizer canonizer = new Canonizer(origScaffold, Canonizer.ENCODE_ATOM_CUSTOM_LABELS);
		mOrigScaffoldIDCodeWithCoords = canonizer.getIDCode() + " " + canonizer.getEncodedCoordinates();
		DescriptorHandlerSkeletonSpheres dhSkelSpheres = DescriptorHandlerSkeletonSpheres.getDefaultInstance();
		byte[] origScaffoldSkelSpheres = dhSkelSpheres.createDescriptor(origScaffold);

		int minAtoms = origScaffold.getAtoms() - Integer.parseInt(configuration.getProperty(PROPERTY_ATOMS_LESS));
		int maxAtoms = origScaffold.getAtoms() + Integer.parseInt(configuration.getProperty(PROPERTY_ATOMS_MORE));

		ForceFieldMMFF94.initialize(ForceFieldMMFF94.MMFF94SPLUS);

		mEVTypeMatchCount = new AtomicInteger();
		mEVPermutationCount = new AtomicInteger();
		mEVRMSDMatchCount = new AtomicInteger();
		mEVAngleMatchCount = new AtomicInteger();
		mPheSAFlexMatchCount = new AtomicInteger();

		// Start worker threads that process all scaffolds/linkers from the fragment file
		mThreadCount = Runtime.getRuntime().availableProcessors();
		mFragmentQueue = new SynchronousQueue<>();
		for (int i=0; i<mThreadCount; i++) {
			Thread t = new Thread(() -> consumeFragments(origScaffoldSkelSpheres), "Scaffold Replacer " + (i + 1));
			t.setPriority(Thread.MIN_PRIORITY);
			t.start();
		}

		// Start worker thread that collects all qualifying results
		mResultTableQueue = new SynchronousQueue<>();
		Thread collectionThread = new Thread(() -> consumeResultRows(queryIDCode));
		collectionThread.start();

		// Read and construct all candidate 3D-fragments from the input file and put into the worker queue
		DWARFileParser parser = new DWARFileParser(fileName);
		int rowCount = parser.getRowCount();
		int row = 0;
		startProgress("Replacing scaffolds...", 0, rowCount == -1 ? 0 : rowCount);
		while (parser.next()) {
			String idcode = parser.getIDCode();
			String coords = parser.getCoordinates3D();
			StereoMolecule mol = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(idcode, coords);
			if (mol != null && mol.getAllAtoms() != 0) {
				mol.ensureHelperArrays(Molecule.cHelperNeighbours);
				if (mol.getAtoms() >= minAtoms && mol.getAtoms() <= maxAtoms) {
					try {
						mFragmentQueue.put(mol);
					} catch (InterruptedException ie) {}
				}
			}
			if (rowCount != -1)
				updateProgress(++row);
		}

		StereoMolecule empty = new StereoMolecule();
		for (int i=0; i<mThreadCount; i++)
			try {
				mFragmentQueue.put(empty);
			} catch (InterruptedException ie) {}

		// the controller thread must wait until all others are finished
		// before the next task can begin or the dialog is closed
		try { collectionThread.join(); } catch (InterruptedException e) {}
		}

	private void consumeFragments(final byte[] origScaffoldSkelSpheres) {
		StereoMolecule fragment;
		try {
			while ((fragment = mFragmentQueue.take()).getAllAtoms() != 0)
				generateReplacements(fragment, origScaffoldSkelSpheres);
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		try { mResultTableQueue.put(new byte[0][]); } catch (InterruptedException ie) { ie.printStackTrace(); }
	}

	private void generateReplacements(StereoMolecule fragment, byte[] origScaffoldSkelSpheres) {
		FragmentGeometry3D fragmentGeometry = new FragmentGeometry3D(fragment, FragmentGeometry3D.MODE_FRAGMENT_WITH_EXIT_VECTORS);
		if (!mQueryGeometry.equals(fragmentGeometry))
			return;

		mEVTypeMatchCount.incrementAndGet();

		fragment.ensureHelperArrays(Molecule.cHelperNeighbours);
		for (int i=0; i<fragment.getAtoms(); i++) {
			if (fragment.getImplicitHydrogens(i) != 0) {
				System.out.println("WARNING: Implicit hydrogens found in 3D-fragment.");
				return;
			}
		}

		for (int p=0; p<mQueryGeometry.getPermutationCount(); p++) {
			mEVPermutationCount.incrementAndGet();

			double[] rmsdHolder = new double[1];
			double[][] matrix = mQueryGeometry.alignRootAndExitAtoms(fragmentGeometry, p, rmsdHolder, mMaxEVRMSD);
			if (matrix != null) {	// TODO should we also check how well COG of scaffold and replacement match???
				mEVRMSDMatchCount.incrementAndGet();

				// Create new atom coordinates for complete aligned fragment
				Coordinates[] fragCoords = new Coordinates[fragment.getAllAtoms()];
				for (int atom=0; atom<fragment.getAllAtoms(); atom++) {
					fragCoords[atom] = new Coordinates(fragment.getCoordinates(atom));
					fragCoords[atom].sub(fragmentGeometry.getAlignmentCOG());
					fragCoords[atom].rotate(matrix);
					fragCoords[atom].add(mQueryGeometry.getAlignmentCOG());
					}

				// Check, whether exit vector directions are compatible
				double[][] angleHolder = new double[1][];
				if (mQueryGeometry.hasMatchingExitVectors(fragmentGeometry, fragCoords, p, angleHolder, mMaxEVDivergence)) {
					// Copy query molecule and remove selected part
					mEVAngleMatchCount.incrementAndGet();

					StereoMolecule modifiedQuery = new StereoMolecule(mQueryMol);
					for (int atom=0; atom<modifiedQuery.getAllAtoms(); atom++)
						if (modifiedQuery.isSelectedAtom(atom))
							modifiedQuery.markAtomForDeletion(atom);
					int[] origToNewAtomInQuery = modifiedQuery.deleteMarkedAtomsAndBonds();

					// Build array of unselected atom coordinate references for aligning these atoms later
					Coordinates[] minimizedStaticAtomCoords = new Coordinates[modifiedQuery.getAllAtoms()];
					for (int atom=0; atom<modifiedQuery.getAllAtoms(); atom++)
						minimizedStaticAtomCoords[atom] = modifiedQuery.getCoordinates(atom);

					// Then add the aligned fragment
					int queryAtoms = modifiedQuery.getAllAtoms();
					int queryBonds = modifiedQuery.getAllBonds();
					int[] origToNewAtomInFrag = modifiedQuery.addMolecule(fragment);
					for (int i=0; i<fragment.getAllAtoms(); i++) {
						modifiedQuery.setAtomX(queryAtoms + i, fragCoords[i].x);
						modifiedQuery.setAtomY(queryAtoms + i, fragCoords[i].y);
						modifiedQuery.setAtomZ(queryAtoms + i, fragCoords[i].z);
					}

					// Remove exit atoms of fragment and re-connect fragment root atoms with corresponding partners in query molecule
					for (int i=0; i<mQueryGeometry.getExitVectorCount(); i++) {
						int exitAtomQuery = origToNewAtomInQuery[mQueryGeometry.getExitAtom(i)];
						int rootAtomFrag = origToNewAtomInFrag[fragmentGeometry.getRootAtom(mQueryGeometry.getPermutedExitVectorIndex(p, i))];
						int exitAtomFrag = origToNewAtomInFrag[fragmentGeometry.getExitAtom(mQueryGeometry.getPermutedExitVectorIndex(p, i))];
						for (int bond=queryBonds; bond<modifiedQuery.getAllBonds(); bond++) {
							if (modifiedQuery.getBondAtom(0, bond) == rootAtomFrag
							 && modifiedQuery.getBondAtom(1, bond) == exitAtomFrag) {
								modifiedQuery.setBondAtom(1, bond, exitAtomQuery);
								modifiedQuery.markAtomForDeletion(exitAtomFrag);
								break;
							}
							else if (modifiedQuery.getBondAtom(1, bond) == rootAtomFrag
								  && modifiedQuery.getBondAtom(0, bond) == exitAtomFrag) {
								modifiedQuery.setBondAtom(0, bond, exitAtomQuery);
								modifiedQuery.markAtomForDeletion(exitAtomFrag);
								break;
							}
						}
					}
					modifiedQuery.deleteMarkedAtomsAndBonds();

					// optimize changed structure using MMFF94
					ForceFieldMMFF94 ff = null;
					double mmffEnergy = Double.NaN;
					try {
						ff = new ForceFieldMMFF94(modifiedQuery, ForceFieldMMFF94.MMFF94SPLUS, new HashMap<>());
						mmffEnergy = minimize(ff, modifiedQuery);
					}
					catch (RuntimeException rte) {}

					// Kabsch-align minimized structure to query using non-changing atoms.
					// Then, determine RSMD of non-changing atoms.
					double queryRMSD = alignAndGetRMSD(mQueryStaticAtomCoords, minimizedStaticAtomCoords, modifiedQuery);

					Canonizer modifiedQueryCanonizer = new Canonizer(modifiedQuery);
					String idcode = modifiedQueryCanonizer.getIDCode();
					String idcoordsMinimized = modifiedQueryCanonizer.getEncodedCoordinates();

					double phesaRigidScore = PheSAAlignmentOptimizer.alignTwoMolsInPlace(mQueryMol, modifiedQuery, 0.5);
					String idcoordsPheSARigid = modifiedQueryCanonizer.getEncodedCoordinates();

					FlexibleShapeAlignment fsa = new FlexibleShapeAlignment(mQueryMol, modifiedQuery);
					double phesaFlexScore = fsa.align()[0];	// returns 0 if it cannot align, e.g. because of inexistant forcefield parameters

					if (phesaFlexScore == 0)
						phesaFlexScore = Double.NaN;
					modifiedQueryCanonizer.invalidateCoordinates();
					String idcoordsPheSAFlex = modifiedQueryCanonizer.getEncodedCoordinates();

					double phESAEnergyDif = Double.isNaN(mmffEnergy) ? Double.NaN : getTotalEnergy(ff, modifiedQuery) - mmffEnergy;

					// When finding linkers for marked hydrogen atoms,
					// phesaFlexScore should not be a cut-off criterion
					if (mSeekLinker || Double.isNaN(phesaFlexScore) || phesaFlexScore >= mMinPheSAFlex) {
						mPheSAFlexMatchCount.incrementAndGet();

						Canonizer fragCanonizer = new Canonizer(fragment, Canonizer.ENCODE_ATOM_CUSTOM_LABELS);
						String fragmentIDCode = fragCanonizer.getIDCode();
						String fragmentCoords = fragCanonizer.getEncodedCoordinates(true, fragCoords);

						double[] confEnergyDifAndPercentage = conformerEnergyDifAndPercentage(modifiedQuery, mmffEnergy);

						try {
							DescriptorHandlerSkeletonSpheres dh = DescriptorHandlerSkeletonSpheres.getDefaultInstance();
							double similarity = dh.getSimilarity(origScaffoldSkelSpheres, dh.createDescriptor(fragment));
							mResultTableQueue.put(constructRow(idcode, idcoordsMinimized, idcoordsPheSARigid, idcoordsPheSAFlex, fragmentIDCode, fragmentCoords,
									rmsdHolder[0], angleHolder[0], queryRMSD, phesaRigidScore, phesaFlexScore, similarity, phESAEnergyDif,
									confEnergyDifAndPercentage[0], confEnergyDifAndPercentage[1]));
						}
						catch (InterruptedException ie) {
							ie.printStackTrace();
						}
					}
				}
			}
		}
	}

	public double[] conformerEnergyDifAndPercentage(StereoMolecule originalMol, double originalEnergy) {
		double[] energyDifAndPercentage = new double[2];

		if (Double.isNaN(originalEnergy)) {
			energyDifAndPercentage[0] = Double.NaN;
			energyDifAndPercentage[1] = Double.NaN;
			return energyDifAndPercentage;
		}

		final int MAX_CONFORMERS = 64;

		ConformerGenerator cg = null;

		ArrayList<Double> energyList = new ArrayList<>();
		double lowestEnergy = originalEnergy;

		int maxTorsionSets = (int) Math.max(2 * MAX_CONFORMERS, (1000 * Math.sqrt(MAX_CONFORMERS)));

		ForceFieldMMFF94.initialize(ForceFieldMMFF94.MMFF94SPLUS);

		StereoMolecule mol = new StereoMolecule(originalMol);
		TorsionDescriptorHelper torsionHelper = new TorsionDescriptorHelper(mol);
		ArrayList<TorsionDescriptor> torsionDescriptorList = new ArrayList<>();

		// add original conformer
		torsionDescriptorList.add(torsionHelper.getTorsionDescriptor());
		energyList.add(originalEnergy);

		for (int i=0; i<MAX_CONFORMERS; i++) {
			if (cg == null) {
				cg = new ConformerGenerator();
				cg.initializeConformers(mol, ConformerGenerator.STRATEGY_ADAPTIVE_RANDOM, maxTorsionSets, false);
			}

			Conformer conformer = cg.getNextConformer();
			if (conformer == null)
				break;

			conformer.copyTo(mol);
			ForceFieldMMFF94 ff = new ForceFieldMMFF94(mol, ForceFieldMMFF94.MMFF94SPLUS, new HashMap<>());
			double energy = minimize(ff, mol);

			// check for redundancy again, because we have minimized and changed the conformer
			if (!isRedundantConformer(torsionHelper, torsionDescriptorList)) {
				energyList.add(energy);
				if (lowestEnergy > energy)
					lowestEnergy = energy;
			}
		}

		double relPercentageSum = 0;
		for (double energy : energyList) {
			double energyDif = 4180 * (energy - lowestEnergy);	// value in Joule/mol
			double K = Math.exp(-energyDif/(8.314*298));	// equilibrium constant between lowest energy conformer and given conformer
			relPercentageSum += K;
		}

		energyDifAndPercentage[0] = originalEnergy - lowestEnergy;
		energyDifAndPercentage[1] = 100.0 * Math.exp(-4180*(originalEnergy-lowestEnergy)/(8.314*298)) / relPercentageSum;
		return energyDifAndPercentage;
	}

	private boolean isRedundantConformer(TorsionDescriptorHelper torsionHelper, ArrayList<TorsionDescriptor> torsionDescriptorList) {
		TorsionDescriptor ntd = torsionHelper.getTorsionDescriptor();
		for (TorsionDescriptor td:torsionDescriptorList)
			if (td.equals(ntd))
				return true;

		torsionDescriptorList.add(ntd);
		return false;
	}

	private double getTotalEnergy(ForceFieldMMFF94 ff, StereoMolecule mol) {
		double[] pos = new double[3*mol.getAllAtoms()];
		for (int i=0; i<mol.getAllAtoms(); i++) {
			pos[3*i]   = mol.getAtomX(i);
			pos[3*i+1] = mol.getAtomY(i);
			pos[3*i+2] = mol.getAtomZ(i);
		}
		return ff.getTotalEnergy(pos);
	}

	private double minimize(ForceFieldMMFF94 ff, StereoMolecule mol) {
		int error = ff.minimise(10000, 0.0001, 1.0e-6);
		return error != 0 ? Float.NaN : ff.getTotalEnergy();
		}

	private void consumeResultRows(String queryIDCode) {
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

		StringBuilder message = new StringBuilder("The task '"+TASK_NAME+"' has found:\n");
		message.append(mEVTypeMatchCount.get() + " fragments with matching exit vector atom types\n");
		if (mEVTypeMatchCount.get() != mEVPermutationCount.get())
			message.append(mEVPermutationCount + " fragment-to-query atom type matches\n");
		message.append(mEVRMSDMatchCount + " matches with exit vector RMSD < "+ mMaxEVRMSD +" (root and exit atoms)\n");
		message.append(mEVAngleMatchCount + " matches with exit vector angle divergence < "+ mMaxEVDivergence +" degrees\n");
		if (!mSeekLinker)
			message.append(mPheSAFlexMatchCount + " built molecules with a PheSA-flex-overlap > "+mMinPheSAFlex+"\n");

		if (rowList.isEmpty()) {
			showErrorMessage(message.toString());
			return;
		}

		mTargetFrame = DataWarrior.getApplication().getEmptyFrame("Scaffold Replacement");

		CompoundTableModel tableModel = mTargetFrame.getTableModel();
		tableModel.initializeTable(rowList.size(), NEW_COLUMN_NAME.length);
		for (int i=0; i<NEW_COLUMN_NAME.length; i++)
			tableModel.setColumnName(NEW_COLUMN_NAME[i], i);

		tableModel.setColumnProperty(STRUCTURE_COLUMN, CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnTypeIDCode);
		tableModel.setColumnProperty(COORDS3D_MINIMIZED_COLUMN, CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnType3DCoordinates);
		tableModel.setColumnProperty(COORDS3D_MINIMIZED_COLUMN, CompoundTableConstants.cColumnPropertyParentColumn, NEW_COLUMN_NAME[STRUCTURE_COLUMN]);
		tableModel.setColumnProperty(COORDS3D_MINIMIZED_COLUMN, CompoundTableConstants.cColumnPropertySuperposeMolecule, queryIDCode);
		tableModel.setColumnProperty(COORDS3D_FLEX_COLUMN, CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnType3DCoordinates);
		tableModel.setColumnProperty(COORDS3D_FLEX_COLUMN, CompoundTableConstants.cColumnPropertyParentColumn, NEW_COLUMN_NAME[STRUCTURE_COLUMN]);
		tableModel.setColumnProperty(COORDS3D_FLEX_COLUMN, CompoundTableConstants.cColumnPropertySuperposeMolecule, queryIDCode);
		tableModel.setColumnProperty(COORDS3D_RIGID_COLUMN, CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnType3DCoordinates);
		tableModel.setColumnProperty(COORDS3D_RIGID_COLUMN, CompoundTableConstants.cColumnPropertyParentColumn, NEW_COLUMN_NAME[STRUCTURE_COLUMN]);
		tableModel.setColumnProperty(COORDS3D_RIGID_COLUMN, CompoundTableConstants.cColumnPropertySuperposeMolecule, queryIDCode);
		tableModel.prepareDescriptorColumn(FFP_COLUMN, STRUCTURE_COLUMN, null, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
		tableModel.setColumnProperty(FRAGMENT_COLUMN, CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnTypeIDCode);
		tableModel.setColumnProperty(FRAGMENT_COORDS_COLUMN, CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnType3DCoordinates);
		tableModel.setColumnProperty(FRAGMENT_COORDS_COLUMN, CompoundTableConstants.cColumnPropertyParentColumn, NEW_COLUMN_NAME[FRAGMENT_COLUMN]);
		tableModel.setColumnProperty(FRAGMENT_COORDS_COLUMN, CompoundTableConstants.cColumnPropertySuperposeMolecule, mOrigScaffoldIDCodeWithCoords);

		int row = 0;
		for (byte[][] line : rowList) {
			int column = 0;
			for (byte[] data : line) {
				if (data != null)
					tableModel.setTotalDataAt(data, row, column);
				column++;
			}
			row++;
			}

		tableModel.finalizeTable(CompoundTableEvent.cSpecifierNoRuntimeProperties, this);

		try {
			SwingUtilities.invokeAndWait(() -> {
				mTargetFrame.getMainFrame().setMainSplitting(0.7);
				mTargetFrame.getMainFrame().setRightSplitting(0);
				mTargetFrame.getMainFrame().getDetailPane().setProperties("height[Data]=0.15;height["+NEW_COLUMN_NAME[STRUCTURE_COLUMN]
						+"]=0.16;height["+NEW_COLUMN_NAME[mSeekLinker ? COORDS3D_MINIMIZED_COLUMN : COORDS3D_FLEX_COLUMN]
						+"]=0.23;height["+NEW_COLUMN_NAME[COORDS3D_RIGID_COLUMN]
						+"]=0.23;height["+NEW_COLUMN_NAME[FRAGMENT_COORDS_COLUMN]+"]=0.23");

				String title1 = "Fragment RMSD & Angle Divergence";
				VisualizationPanel2D vpanel1 = mTargetFrame.getMainFrame().getMainPane().add2DView(title1, "Table\tbottom\t0.5");
				vpanel1.setAxisColumnName(0, NEW_COLUMN_NAME[FRAGMENT_ANGLE_COLUMN]);
				vpanel1.setAxisColumnName(1, NEW_COLUMN_NAME[FRAGMENT_RMSD_COLUMN]);
				int colorListMode1 = VisualizationColor.cColorListModeHSBLong;
				Color[] colorList1 = VisualizationColor.createColorWedge(Color.red, Color.blue, colorListMode1, null);
				vpanel1.getVisualization().getMarkerColor().setColor(SCAFFOLD_SIM_COLUMN, colorList1, colorListMode1);
				vpanel1.getVisualization().setFontSize(2.0f, JVisualization.cFontSizeModeRelative, false);

				String title2 = mSeekLinker ? "RMDS" : "RMSD & PheSA-flex";
				VisualizationPanel2D vpanel2 = mTargetFrame.getMainFrame().getMainPane().add2DView(title2, title1 + "\tright\t0.5");
				vpanel2.setAxisColumnName(0, NEW_COLUMN_NAME[QUERY_RMSD_COLUMN]);
				vpanel2.setAxisColumnName(1, NEW_COLUMN_NAME[mSeekLinker ? FRAGMENT_RMSD_COLUMN : PHESA_FLEX_COLUMN]);
				int colorListMode2 = VisualizationColor.cColorListModeHSBLong;
				Color[] colorList2 = VisualizationColor.createColorWedge(Color.red, Color.blue, colorListMode2, null);
				vpanel2.getVisualization().getMarkerColor().setColor(SCAFFOLD_SIM_COLUMN, colorList2, colorListMode2);
				vpanel2.getVisualization().setFontSize(2.0f, JVisualization.cFontSizeModeRelative, false);
			});
		}
		catch (Exception e) {}

		showMessage(message.toString(), INFORMATION_MESSAGE);
	}

	private byte[][] constructRow(String idcode, String minimizedCoords, String rigidCoords, String flexCoords, String fragmentIDCode, String fragmentCoords,
								  double fragmentRMSD, double[] fragmentAngles, double queryRMSD,
								  double phesaRigidScore, double phesaFlexScore, double scaffoldSimilarity,
								  double phESAEnergyDif, double conformerPercentage, double conformerEnergyDif) {
		byte[][] row = new byte[NEW_COLUMN_NAME.length][];
		row[STRUCTURE_COLUMN] = idcode.getBytes();
		row[COORDS3D_MINIMIZED_COLUMN] = minimizedCoords.getBytes();
		row[COORDS3D_RIGID_COLUMN] = rigidCoords.getBytes();
		row[COORDS3D_FLEX_COLUMN] = flexCoords.getBytes();
		row[PHESA_RIGID_COLUMN] = DoubleFormat.toString(phesaRigidScore).getBytes();
		row[PHESA_FLEX_COLUMN] = DoubleFormat.toString(phesaFlexScore).getBytes();
		row[FRAGMENT_COLUMN] = fragmentIDCode.getBytes();
		row[FRAGMENT_COORDS_COLUMN] = fragmentCoords.getBytes();
		row[FRAGMENT_RMSD_COLUMN] = DoubleFormat.toString(fragmentRMSD).getBytes();
		StringBuilder angles = new StringBuilder();
		for (double angle : fragmentAngles) {
			if (!angles.isEmpty())
				angles.append("; ");
			angles.append(DoubleFormat.toString(angle, 2));
		}
		row[FRAGMENT_ANGLE_COLUMN] = angles.toString().getBytes();
		row[QUERY_RMSD_COLUMN] = DoubleFormat.toString(queryRMSD).getBytes();
		row[SCAFFOLD_SIM_COLUMN] = DoubleFormat.toString(scaffoldSimilarity).getBytes();
		row[ENERGY_DIF_PHESA_COLUMN] = DoubleFormat.toString(phESAEnergyDif).getBytes();
		row[CONFORMER_PERCENTAGE_COLUMN] = DoubleFormat.toString(conformerPercentage).getBytes();
		row[CONFORMER_ENERGY_DIF_COLUMN] = DoubleFormat.toString(conformerEnergyDif).getBytes();
		return row;
		}

	private double alignAndGetRMSD(Coordinates[] queryCoords, Coordinates[] minimizedCoords, StereoMolecule modifiedQuery) {
		Coordinates queryCOG = FragmentGeometry3D.centerOfGravity(queryCoords);
		Coordinates minimizedCOG = FragmentGeometry3D.centerOfGravity(minimizedCoords);
		double[][] matrix = FragmentGeometry3D.kabschAlign(queryCoords, minimizedCoords, queryCOG, minimizedCOG);

		for (Coordinates c : modifiedQuery.getAtomCoordinates()) {
			c.sub(minimizedCOG);
			c.rotate(matrix);
			c.add(queryCOG);
		}

		return Coordinates.getRmsd(queryCoords, minimizedCoords);
	}
}