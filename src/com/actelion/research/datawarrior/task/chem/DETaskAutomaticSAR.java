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
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.JVisualization2D;
import com.actelion.research.table.view.VisualizationPanel2D;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;
import java.util.TreeMap;


public class DETaskAutomaticSAR extends DETaskAbstractFromStructure {
	public static final String TASK_NAME = "Automatic SAR Analysis";
	private static final String SUBSTITUENT_NONE = "#";
	private static final String SUBSTITUENT_VARIES = "*";

	private static final String PROPERTY_SCAFFOLD_MODE = "scaffoldMode";

	private static final int SCAFFOLD_CENTRAL_RING = 0;
	private static final int SCAFFOLD_MURCKO = 1;

	private static final String[] SCAFFOLD_TEXT = { "Most central ring system", "Murcko scaffolds" };
	private static final String[] SCAFFOLD_CODE = { "centralRing", "murcko" };

	private DEFrame		mFrame;
	private JComboBox	mComboBoxScaffoldMode;
	private String[]	mCoreIDCode;
	private int[][]		mCoreAtom;
	private int			mSubstituentCount;
	private TreeMap<String,String[]> mSubstitutionMap;

	public DETaskAutomaticSAR(DEFrame parent) {
		super(parent, DESCRIPTOR_NONE, false, false);
		mFrame = parent;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean hasExtendedDialogContent() {
		return true;
		}

	@Override
	public JPanel getExtendedDialogContent() {
		JPanel ep = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {TableLayout.PREFERRED, gap, TableLayout.PREFERRED},
							{TableLayout.PREFERRED} };
		ep.setLayout(new TableLayout(size));
		ep.add(new JLabel("Scaffold type:"), "0,0");
		mComboBoxScaffoldMode = new JComboBox(SCAFFOLD_TEXT);
		ep.add(mComboBoxScaffoldMode, "2,0");
		return ep;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/chemistry.html#SARTables";
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_SCAFFOLD_MODE, SCAFFOLD_CODE[mComboBoxScaffoldMode.getSelectedIndex()]);
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mComboBoxScaffoldMode.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_SCAFFOLD_MODE), SCAFFOLD_CODE, SCAFFOLD_CENTRAL_RING));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mComboBoxScaffoldMode.setSelectedIndex(SCAFFOLD_CENTRAL_RING);
		}

	@Override
	protected int getNewColumnCount() {
		return (mCoreAtom == null) ? 0 : 1+mSubstituentCount;
		}

	@Override
	protected String getNewColumnName(int column) {
		return (column == 0) ? "Core Fragment" : "R"+column;
		}

	@Override
	protected boolean preprocessRows(Properties configuration) {
		int rowCount = getTableModel().getTotalRowCount();
		startProgress("Analyzing scaffolds...", 0, rowCount);

		int scaffoldMode = findListIndex(configuration.getProperty(PROPERTY_SCAFFOLD_MODE), SCAFFOLD_CODE, SCAFFOLD_CENTRAL_RING);
		mCoreIDCode = new String[rowCount];
		mCoreAtom = new int[rowCount][];
		mSubstitutionMap = new TreeMap<>();
		StereoMolecule core = new StereoMolecule();
		StereoMolecule container = new StereoMolecule();
		for (int row=0; row<rowCount; row++) {
			if ((row % 16) == 15) {
				if (threadMustDie())
					break;
				updateProgress(row);
				}

			StereoMolecule mol = getChemicalStructure(row, container);
			if (mol != null) {
				mol.stripSmallFragments();
				boolean[] isCoreAtom = (scaffoldMode == SCAFFOLD_MURCKO) ?
						ScaffoldHelper.findMurckoScaffold(mol) : ScaffoldHelper.findMostCentralRingSystem(mol);
				if (isCoreAtom != null) {
					int[] molToCoreAtom = new int[mol.getAllAtoms()];
					mol.copyMoleculeByAtoms(core, isCoreAtom, true, molToCoreAtom);
					core.setFragment(true);
					Canonizer canonizer = new Canonizer(core);
					int[] graphIndex = canonizer.getGraphIndexes();

					// build atom index map from canonized core to fragment stripped molecule of this row
					mCoreAtom[row] = new int[core.getAtoms()];
					for (int atom=0; atom<molToCoreAtom.length; atom++)
						if (molToCoreAtom[atom] != -1)
							mCoreAtom[row][graphIndex[molToCoreAtom[atom]]] = atom;

					mCoreIDCode[row] = canonizer.getIDCode();

					String[] sharedSubstituentCode = mSubstitutionMap.get(mCoreIDCode[row]);
					if (sharedSubstituentCode == null) {
						sharedSubstituentCode = new String[core.getAtoms()];
						mSubstitutionMap.put(mCoreIDCode[row], sharedSubstituentCode);
						}
					for (int atom=0; atom<molToCoreAtom.length; atom++) {
						if (molToCoreAtom[atom] != -1) {
							int canonicalCoreAtom = graphIndex[molToCoreAtom[atom]];
							if (!SUBSTITUENT_VARIES.equals(sharedSubstituentCode[canonicalCoreAtom])) {
								String idcode = (mol.getConnAtoms(atom) > core.getConnAtoms(molToCoreAtom[atom])) ?
										getSubstituentIDCode(mol, atom, isCoreAtom) : SUBSTITUENT_NONE;
								if (sharedSubstituentCode[canonicalCoreAtom] == null)
									sharedSubstituentCode[canonicalCoreAtom] = idcode;
								else if (!sharedSubstituentCode[canonicalCoreAtom].equals(idcode))
									sharedSubstituentCode[canonicalCoreAtom] = SUBSTITUENT_VARIES;
							 	}
							}
						}
					}
				}
			}

		if (!threadMustDie()) {
			mSubstituentCount = 0;
			for (String[] sharedSubstituentCode:mSubstitutionMap.values()) {
				int count = 0;
				for (String s:sharedSubstituentCode)
					if (SUBSTITUENT_VARIES.equals(s))
						count++;
				if (mSubstituentCount < count)
					mSubstituentCount = count;
				}
			}

		return true;
		}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol, int threadIndex) {
		StereoMolecule mol = getChemicalStructure(row, containerMol);
		if (mol != null) {
			mol.stripSmallFragments();
			mol.ensureHelperArrays(Molecule.cHelperParities);

			String[] sharedSubstituentCode = mSubstitutionMap.get(mCoreIDCode[row]);

			boolean[] isCoreAtom = new boolean[mol.getAtoms()+sharedSubstituentCode.length];
			for (int i=0; i<mCoreAtom[row].length; i++)
				isCoreAtom[mCoreAtom[row][i]] = true;

			for (int i=0; i<sharedSubstituentCode.length; i++) {
				if (sharedSubstituentCode[i] != null
				 && sharedSubstituentCode[i] != SUBSTITUENT_VARIES) {
					addSubstituent(mol, mCoreAtom[row][i], isCoreAtom);
					}
				}

			int substituentNo = 1;
			for (int i=0; i<sharedSubstituentCode.length; i++) {
				if (sharedSubstituentCode[i] == SUBSTITUENT_VARIES) {
					String idcode = getSubstituentIDCode(mol, mCoreAtom[row][i], isCoreAtom);
					if (idcode != null)
						getTableModel().setTotalValueAt(idcode, row, firstNewColumn+substituentNo);
					substituentNo++;
					}
				}

			// attach rest atoms (R1...Rn) to core where we have substitution
			substituentNo = 1;
			for (int i=0; i<sharedSubstituentCode.length; i++) {
				if (sharedSubstituentCode[i] == SUBSTITUENT_VARIES) {
					int restAtom = mol.addAtom((substituentNo <= 3) ? 141+substituentNo : 125+substituentNo);
					mol.addBond(mCoreAtom[row][i], restAtom, Molecule.cBondTypeSingle);
//					mol.setAtomCustomLabel(restAtom, "R"+substituentNo);
					isCoreAtom[restAtom] = true;
					substituentNo++;
					}
				}
			StereoMolecule core = new StereoMolecule();
			mol.copyMoleculeByAtoms(core, isCoreAtom, false, null);
			core.setFragment(false);
			String coreIDCode = new Canonizer(core).getIDCode();

			getTableModel().setTotalValueAt(coreIDCode, row, firstNewColumn);
			}
		}

	@Override
	public void postprocess(final int firstNewColumn) {
		SwingUtilities.invokeLater(() -> {
			int coreCount = getTableModel().isColumnTypeCategory(firstNewColumn) ? getTableModel().getCategoryCount(firstNewColumn) : 1;
			int rCount = getTableModel().getTotalColumnCount() - firstNewColumn - 1;
			if (coreCount <= 64 && (rCount >= 2 || (rCount == 1 && coreCount != 1))) {
				VisualizationPanel2D vpanel = mFrame.getMainFrame().getMainPane().add2DView("Core Structures", null);
				int firstAxisColumn = (rCount >= 2) ? firstNewColumn+1 : firstNewColumn;
				vpanel.setAxisColumnName(0, getTableModel().getColumnTitleNoAlias(firstAxisColumn));
				vpanel.setAxisColumnName(1, getTableModel().getColumnTitleNoAlias(firstAxisColumn+1));

				JVisualization2D visualization = (JVisualization2D)vpanel.getVisualization();
				visualization.setMarkerSize(0.6f, false);
				visualization.setPreferredChartType(JVisualization.cChartTypeScatterPlot, -1, -1);
				if (coreCount != 1 && firstAxisColumn != firstNewColumn)
					visualization.setSplittingColumns(firstNewColumn, JVisualization.cColumnUnassigned, 1.0f, false);
				}
			});
		}

	private void addSubstituent(StereoMolecule mol, int startAtom, boolean[] isCoreAtom) {
		int[] graphAtom = new int[mol.getAllAtoms()];
		graphAtom[0] = startAtom;
		int current = 0;
		int highest = 0;
		while (current <= highest) {
			for (int i=0; i<mol.getConnAtoms(graphAtom[current]); i++) {
				int candidate = mol.getConnAtom(graphAtom[current], i);
				if (!isCoreAtom[candidate]) {
					isCoreAtom[candidate] = true;
					graphAtom[++highest] = candidate;
					}
				}
			current++;
			}
		}

	private String getSubstituentIDCode(StereoMolecule mol, int startAtom, boolean[] isCoreAtom) {
		boolean[] isSubstituentAtom = new boolean[mol.getAtoms()];
		int[] graphAtom = new int[mol.getAtoms()];
		isSubstituentAtom[startAtom] = true;
		graphAtom[0] = startAtom;
		int current = 0;
		int highest = 0;
		boolean isRingClosure = false;
		while (current <= highest) {
			if (current == 0 || !isCoreAtom[graphAtom[current]]) {
				for (int i=0; i<mol.getConnAtoms(graphAtom[current]); i++) {
					int candidate = mol.getConnAtom(graphAtom[current], i);
					if (!isSubstituentAtom[candidate]
					 && (current != 0 || !isCoreAtom[candidate])) {
						isSubstituentAtom[candidate] = true;
						graphAtom[++highest] = candidate;
						if (isCoreAtom[candidate])
							isRingClosure = true;
						}
					}
				}
			current++;
			}

		if (highest == 0)
			return null;

		StereoMolecule fragment = new StereoMolecule();
		int[] substituentAtom = new int[mol.getAtoms()];
		mol.copyMoleculeByAtoms(fragment, isSubstituentAtom, false, substituentAtom);
		fragment.setFragment(false);
//		fragment.ensureHelperArrays(Molecule.cHelperRings);

		// set atomicNo of startAtom to 0 to indicate attachment point
		fragment.setAtomicNo(substituentAtom[startAtom], 0);

		if (isRingClosure) {
			// set atomicNo of ring closure atoms to 0 to indicate attachment points
			for (int atom=0; atom<mol.getAllAtoms(); atom++)
				if (isSubstituentAtom[atom] && isCoreAtom[atom] && atom != startAtom)
					fragment.setAtomicNo(substituentAtom[atom], 0);

			for (int bond=fragment.getAllBonds()-1; bond>=0; bond--)
				if (fragment.getAtomicNo(fragment.getBondAtom(0, bond)) == 0
				 && fragment.getAtomicNo(fragment.getBondAtom(1, bond)) == 0)
					fragment.deleteBond(bond);
			}

		return new Canonizer(fragment).getIDCode();
		}

	@Override
	protected void setNewColumnProperties(int firstNewColumn) {
		for (int i=0; i<=mSubstituentCount; i++)
			getTableModel().setColumnProperty(firstNewColumn+i, CompoundTableModel.cColumnPropertySpecialType,
																CompoundTableModel.cColumnTypeIDCode);

/*		tableModel.prepareStructureColumns(firstNewColumn, "Core Fragment", false, false);
		for (int i=1; i<=mSubstituentCount; i++)
			tableModel.prepareStructureColumns(firstNewColumn+i, "R"+i, false, false);*/
		}

	@Override
	protected String getNewColumnValue(StereoMolecule mol, Object descriptor, int column) {
		if (mol.getAllAtoms() == 0)
			return "";

		int[] fragmentNo = new int[mol.getAllAtoms()];
		int fragments = mol.getFragmentNumbers(fragmentNo, false, true);
		if (fragments == 1)
			return new MolecularFormula(mol).getFormula();

		StereoMolecule[] fragment = mol.getFragments(fragmentNo, fragments);
		Arrays.sort(fragment, new Comparator<StereoMolecule>() {
			public int compare(StereoMolecule mol1, StereoMolecule mol2) {
				return mol1.getAllAtoms() > mol2.getAllAtoms() ? 0
					 : mol1.getAllAtoms() == mol2.getAllAtoms() ? 1 : 2;
				}
			});

		StringBuilder formula = new StringBuilder(new MolecularFormula(fragment[0]).getFormula());
		for (int i=1; i<fragment.length; i++)
			formula.append("."+new MolecularFormula(fragment[i]).getFormula());
		return formula.toString();
		}
	}
