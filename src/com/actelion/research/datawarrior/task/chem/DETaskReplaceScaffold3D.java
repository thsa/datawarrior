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
import com.actelion.research.gui.VerticalFlowLayout;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.DoubleFormat;
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

	private static final double MAX_ROOT_RMSD = 0.5;
	private static final double MAX_EXIT_VECTOR_DIVERSION = 30 * Math.PI / 180;
	private static final double MIN_MOLECULE_PHESA = 0.6;

	private static final String PROPERTY_QUERY = "query";
	private static final String PROPERTY_SIMILARITY = "similarity";
	private static final String PROPERTY_IN_FILE_NAME = "inFile";
	private static final String PROPERTY_ATOMS_LESS = "atomsLess";
	private static final String PROPERTY_ATOMS_MORE = "atomsMore";
	private static final String PROPERTY_MINIMIZE = "minimize";

	private static final String[] NEW_COLUMN_NAME = {
			"Structure", "flexibly superposed", DescriptorConstants.DESCRIPTOR_FFP512.shortName, "PheSA Flex Score",
			"Scaffold Similarity", "Energy Dif", "PheSA Rigid Score", "minimized and rigid superposition"
			};

	public static final String TASK_NAME = "Replace Scaffold By 3D-Fragment";
	private static final int MIN_SIMILARITY = 60;
	private static final int DEFAULT_SIMILARITY = 80;


	private DEFrame mTargetFrame;
	private JFXMolViewerPanel mConformerPanel;
	private JCheckBox mCheckBoxMinimize;
	private JTextField mTextFieldAtomsLess,mTextFieldAtomsMore;
	private JSlider mSimilaritySlider;
	private JFilePathLabel mLabelInFileName;
	private volatile SynchronousQueue<StereoMolecule> mFragmentQueue;
	private volatile SynchronousQueue<byte[][]> mResultTableQueue;
	private volatile int mThreadCount;
	private volatile StereoMolecule mQueryMol;
	private volatile FragmentGeometry3D mQueryGeometry;


	public DETaskReplaceScaffold3D(DEFrame parent) {
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
									2*gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		EnumSet<V3DScene.ViewerSettings> settings = V3DScene.CONFORMER_VIEW_MODE;
		settings.add(V3DScene.ViewerSettings.EDITING);
		mConformerPanel = new JFXMolViewerPanel(false, settings);
		mConformerPanel.adaptToLookAndFeelChanges();
		mConformerPanel.setPreferredSize(new Dimension(HiDPIHelper.scale(400), HiDPIHelper.scale(200)));
		mConformerPanel.setPopupMenuController(new EditableSmallMolMenuController(getParentFrame(), mConformerPanel));

		content.add(mConformerPanel, "1,1,3,1");
		content.add(new JLabel("First, define a bio-active 3D-structure.", JLabel.CENTER), "1,3,3,3");
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

		mTextFieldAtomsLess = new JTextField(2);
		mTextFieldAtomsMore = new JTextField(2);
		JPanel atomPanel = new JPanel();
		atomPanel.add(new JLabel("Allow "));
		atomPanel.add(mTextFieldAtomsLess);
		atomPanel.add(new JLabel(" less to "));
		atomPanel.add(mTextFieldAtomsMore);
		atomPanel.add(new JLabel(" more non-H atoms than selected atoms"));
		content.add(atomPanel, "1,11,3,11");

		mCheckBoxMinimize = new JCheckBox("Energy-minimize structures after scaffold replacement");
		content.add(mCheckBoxMinimize, "1,13,3,13");

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
		if (query.getAllAtoms() < selectedAtomCount + 2) {
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

		try {
			Integer.parseInt(configuration.getProperty(PROPERTY_ATOMS_LESS));
			Integer.parseInt(configuration.getProperty(PROPERTY_ATOMS_MORE));
		} catch (NumberFormatException nfe) {
			showErrorMessage("The setting for the allowed non-hydrogen atom span is not numerical.");
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

		mTextFieldAtomsLess.setText(configuration.getProperty(PROPERTY_ATOMS_LESS, ""));
		mTextFieldAtomsMore.setText(configuration.getProperty(PROPERTY_ATOMS_MORE, ""));

		mCheckBoxMinimize.setSelected("true".equals(configuration.getProperty(PROPERTY_MINIMIZE, "true")));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mSimilaritySlider.setValue(DEFAULT_SIMILARITY);
		mTextFieldAtomsLess.setText("3");
		mTextFieldAtomsMore.setText("5");
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

		configuration.setProperty(PROPERTY_ATOMS_LESS, mTextFieldAtomsLess.getText());
		configuration.setProperty(PROPERTY_ATOMS_MORE, mTextFieldAtomsMore.getText());

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

		String queryIDCode = configuration.getProperty(PROPERTY_QUERY);
		mQueryMol = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(queryIDCode);
		mQueryMol.ensureHelperArrays(Molecule.cHelperParities);
		mQueryGeometry = new FragmentGeometry3D(mQueryMol, FragmentGeometry3D.MODE_SELECTED_ATOMS);

		boolean[] includeAtom = new boolean[mQueryMol.getAllAtoms()];
		for (int atom=0; atom<mQueryMol.getAllAtoms(); atom++) {
			if (mQueryMol.isSelectedAtom(atom)) {
				includeAtom[atom] = true;
				for (int i=0; i<mQueryMol.getAllConnAtoms(atom); i++)
					includeAtom[mQueryMol.getConnAtom(atom, i)] = true;
			}
		}
		StereoMolecule origScaffold = new StereoMolecule();
		mQueryMol.copyMoleculeByAtoms(origScaffold, includeAtom, false, null);
		origScaffold.ensureHelperArrays(Molecule.cHelperNeighbours);
		int minAtoms = origScaffold.getAtoms() - Integer.parseInt(configuration.getProperty(PROPERTY_ATOMS_LESS));
		int maxAtoms = origScaffold.getAtoms() + Integer.parseInt(configuration.getProperty(PROPERTY_ATOMS_MORE));
		DescriptorHandlerSkeletonSpheres dhSkelSpheres = DescriptorHandlerSkeletonSpheres.getDefaultInstance();
		byte[] origScaffoldSkelSpheres = dhSkelSpheres.createDescriptor(origScaffold);

		mThreadCount = Runtime.getRuntime().availableProcessors();
		mFragmentQueue = new SynchronousQueue<>();
		for (int i=0; i<mThreadCount; i++) {
			Thread t = new Thread(() -> consumeFragments(minimize, origScaffoldSkelSpheres), "Scaffold Replacer " + (i + 1));
			t.setPriority(Thread.MIN_PRIORITY);
			t.start();
			}

		ForceFieldMMFF94.initialize(ForceFieldMMFF94.MMFF94SPLUS);

		mResultTableQueue = new SynchronousQueue<>();
		Thread collectionThread = new Thread(() -> consumeResultRows(queryIDCode));
		collectionThread.start();

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
						if (rowCount != -1)
							updateProgress(++row);
					} catch (InterruptedException ie) {}
				}
			}
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

	private void consumeFragments(final boolean minimize, final byte[] origScaffoldSkelSpheres) {
		StereoMolecule fragment;
		try {
			while ((fragment = mFragmentQueue.take()).getAllAtoms() != 0)
				generateReplacements(fragment, minimize, origScaffoldSkelSpheres);
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		try { mResultTableQueue.put(new byte[0][]); } catch (InterruptedException ie) { ie.printStackTrace(); }
	}

	private void generateReplacements(StereoMolecule fragment, boolean minimize, byte[] origScaffoldSkelSpheres) {
		FragmentGeometry3D fragmentGeometry = new FragmentGeometry3D(fragment, FragmentGeometry3D.MODE_FRAGMENT_WITH_EXIT_VECTORS);
		if (!mQueryGeometry.equals(fragmentGeometry))
			return;

		for (int p=0; p<mQueryGeometry.getPermutationCount(); p++) {
			double[][] matrix = mQueryGeometry.alignRootAndExitAtoms(fragmentGeometry, p, MAX_ROOT_RMSD);
			if (matrix != null) {	// TODO should we also check how well COG of scaffold and replacement match???

				// Create new atom coordinates for complete aligned fragment
				Coordinates[] fragCoords = new Coordinates[fragment.getAllAtoms()];
				for (int atom=0; atom<fragment.getAllAtoms(); atom++) {
					fragCoords[atom] = new Coordinates(fragment.getCoordinates(atom));
					fragCoords[atom].sub(fragmentGeometry.getAlignmentCOG());
					fragCoords[atom].rotate(matrix);
					fragCoords[atom].add(mQueryGeometry.getAlignmentCOG());
					}

				// Check, whether exit vector directions are compatible
				if (mQueryGeometry.hasMatchingExitVectors(fragmentGeometry, fragCoords, p, MAX_EXIT_VECTOR_DIVERSION)) {
					// Copy query molecule and remove selected part
					StereoMolecule flexQuery = new StereoMolecule(mQueryMol);
					for (int atom=0; atom<flexQuery.getAllAtoms(); atom++)
						if (flexQuery.isSelectedAtom(atom))
							flexQuery.markAtomForDeletion(atom);
					int[] origToNewAtomInQuery = flexQuery.deleteMarkedAtomsAndBonds();

					// add aligned fragment
					int queryAtoms = flexQuery.getAllAtoms();
					int queryBonds = flexQuery.getAllBonds();
					int[] origToNewAtomInFrag = flexQuery.addMolecule(fragment);
					for (int i=0; i<fragment.getAllAtoms(); i++) {
						flexQuery.setAtomX(queryAtoms + i, fragCoords[i].x);
						flexQuery.setAtomY(queryAtoms + i, fragCoords[i].y);
						flexQuery.setAtomZ(queryAtoms + i, fragCoords[i].z);
					}

					for (int i=0; i<mQueryGeometry.getExitVectorCount(); i++) {
						int exitAtomQery = origToNewAtomInQuery[mQueryGeometry.getExitAtom(i)];
						int rootAtomFrag = origToNewAtomInFrag[fragmentGeometry.getRootAtom(mQueryGeometry.getPermutedExitVectorIndex(p, i))];
						int exitAtomFrag = origToNewAtomInFrag[fragmentGeometry.getExitAtom(mQueryGeometry.getPermutedExitVectorIndex(p, i))];
						for (int bond=queryBonds; bond<flexQuery.getAllBonds(); bond++) {
							if (flexQuery.getBondAtom(0, bond) == rootAtomFrag
							 && flexQuery.getBondAtom(1, bond) == exitAtomFrag) {
								flexQuery.setBondAtom(1, bond, exitAtomQery);
								flexQuery.markAtomForDeletion(exitAtomFrag);
								break;
							}
							else if (flexQuery.getBondAtom(1, bond) == rootAtomFrag
								  && flexQuery.getBondAtom(0, bond) == exitAtomFrag) {
								flexQuery.setBondAtom(0, bond, exitAtomQery);
								flexQuery.markAtomForDeletion(exitAtomFrag);
								break;
							}
						}
					}
					flexQuery.deleteMarkedAtomsAndBonds();

//					double phesaScore = PheSAAlignmentOptimizer.alignTwoMolsInPlace(mQueryMol, queryWithNewCore, 0.5);
					FlexibleShapeAlignment fsa = new FlexibleShapeAlignment(mQueryMol, flexQuery);
					double phesaScore1 = fsa.align()[0];

					Canonizer canonizer = new Canonizer(flexQuery);
					String idcode = canonizer.getIDCode();
					String flexCoords = canonizer.getEncodedCoordinates();

					double energy2 = getTotalEnergy(flexQuery);

					double energy3 = minimize(flexQuery);
					double phesaScore2 = PheSAAlignmentOptimizer.alignTwoMolsInPlace(mQueryMol, flexQuery, 0.5);

					String rigidCoords = new Canonizer(flexQuery).getEncodedCoordinates();

					if (phesaScore1 > MIN_MOLECULE_PHESA) {
						try {
							DescriptorHandlerSkeletonSpheres dh = DescriptorHandlerSkeletonSpheres.getDefaultInstance();
							double similarity = dh.getSimilarity(origScaffoldSkelSpheres, dh.createDescriptor(fragment));
							mResultTableQueue.put(constructRow(idcode, flexCoords, rigidCoords, phesaScore1, similarity, energy2-energy3, phesaScore2));
						}
					catch (InterruptedException ie) {
							ie.printStackTrace();
						}
					}
				}
			}
		}
	}

	private double getTotalEnergy(StereoMolecule mol) {
		String tableSet = ForceFieldMMFF94.MMFF94SPLUS;
		ForceFieldMMFF94 ff = new ForceFieldMMFF94(mol, tableSet, new HashMap<>());

		double[] pos = new double[3*mol.getAllAtoms()];
		for (int i=0; i<mol.getAllAtoms(); i++) {
			pos[3*i    ] = mol.getAtomX(i); //+ delta[0];
			pos[3*i + 1] = mol.getAtomY(i); //+ delta[1];
			pos[3*i + 2] = mol.getAtomZ(i); //+ delta[2];
		}
		return ff.getTotalEnergy(pos);
	}

	private double minimize(StereoMolecule mol) {
		String tableSet = ForceFieldMMFF94.MMFF94SPLUS;
		ForceFieldMMFF94 ff = new ForceFieldMMFF94(mol, tableSet, new HashMap<>());
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

		if (rowList.isEmpty()) {
			showErrorMessage("No matching fragment could be found.");
		}

		mTargetFrame = DataWarrior.getApplication().getEmptyFrame("Scaffold Replacement");

		CompoundTableModel tableModel = mTargetFrame.getTableModel();
		tableModel.initializeTable(rowList.size(), 8);
		for (int i=0; i<NEW_COLUMN_NAME.length; i++)
			tableModel.setColumnName(NEW_COLUMN_NAME[i], i);

		tableModel.setColumnProperty(0, CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnTypeIDCode);
		tableModel.setColumnProperty(1, CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnType3DCoordinates);
		tableModel.setColumnProperty(1, CompoundTableConstants.cColumnPropertyParentColumn, NEW_COLUMN_NAME[0]);
		tableModel.setColumnProperty(1, CompoundTableConstants.cColumnPropertySuperposeMolecule, queryIDCode);
		tableModel.prepareDescriptorColumn(2, 0, null, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
		tableModel.setColumnProperty(7, CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnType3DCoordinates);
		tableModel.setColumnProperty(7, CompoundTableConstants.cColumnPropertyParentColumn, NEW_COLUMN_NAME[0]);
		tableModel.setColumnProperty(7, CompoundTableConstants.cColumnPropertySuperposeMolecule, queryIDCode);

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
		}

	private byte[][] constructRow(String idcode, String flexCoords, String rigidCoords, double phesaScore1, double scaffoldSimilarity, double energyDif, double phesaScore2) {
		byte[][] row = new byte[8][];
		row[0] = idcode.getBytes();
		row[1] = flexCoords.getBytes();
		row[3] = DoubleFormat.toString(phesaScore1).getBytes();
		row[4] = DoubleFormat.toString(scaffoldSimilarity).getBytes();
		row[5] = DoubleFormat.toString(energyDif).getBytes();
		row[6] = DoubleFormat.toString(phesaScore2).getBytes();
		row[7] = rigidCoords.getBytes();
		return row;
		}
	}