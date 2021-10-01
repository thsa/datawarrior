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

package com.actelion.research.datawarrior.task.chem.rxn;

import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.chem.reaction.ReactionEncoder;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.chem.DETaskAbstractFromReaction;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.util.Properties;

public class DETaskExtractTransformation extends DETaskAbstractFromReaction {
	public static final String TASK_NAME = "Extract Reaction Transformation";

	private static final String PROPERTY_DETAIL = "detail";
	private static final String[] DETAIL_TEXT = { "One atom shell", "One shell with features", "Two atom shells", "Two shells with features" };
	private static final String[] DETAIL_CODE = { "oneShell", "oneShellEx", "twoShells", "twoShellsEx" };
	private static final int DETAIL_DEFAULT = 1;

	private JComboBox mComboBoxDetail;
	private int mDetail;

	public DETaskExtractTransformation(DEFrame parent) {
		super(parent, DESCRIPTOR_NONE, false, true);
		mDetail = 1;
		}

	@Override
	protected int getNewColumnCount() {
		return 3;
		}

	@Override
	protected String getNewColumnName(int column) {
		// is done by setNewColumnProperties()
		return "";
		}

	@Override
	public boolean hasExtendedDialogContent() {
		return true;
		}

	@Override
	public JPanel getExtendedDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {TableLayout.PREFERRED, gap, TableLayout.PREFERRED}, {TableLayout.PREFERRED} };

		mComboBoxDetail = new JComboBox(DETAIL_TEXT);

		JPanel ep = new JPanel();
		ep.setLayout(new TableLayout(size));
		ep.add(new JLabel("Extend reaction center by:"), "0,0");
		ep.add(mComboBoxDetail, "2,0");
		return ep;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_DETAIL, DETAIL_CODE[mComboBoxDetail.getSelectedIndex()]);
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		int index = findListIndex(configuration.getProperty(PROPERTY_DETAIL), DETAIL_CODE, DETAIL_DEFAULT);
		mComboBoxDetail.setSelectedIndex(index);
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mComboBoxDetail.setSelectedIndex(DETAIL_DEFAULT);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	protected void setNewColumnProperties(int firstNewColumn) {
		String sourceColumnName = getTableModel().getColumnTitle(getChemistryColumn());
		getTableModel().prepareReactionColumns(firstNewColumn, "Transformation of " + sourceColumnName, true,
				true, false, false, true, false, false, false);
		}

	@Override
	protected boolean preprocessRows(Properties configuration) {
		mDetail = findListIndex(configuration.getProperty(PROPERTY_DETAIL), DETAIL_CODE, DETAIL_DEFAULT);
		return super.preprocessRows(configuration);
		}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol, int threadIndex) {
		Reaction rxn = getChemicalReaction(row);
		if (rxn == null)
			return;

		// find reaction centers as those mapped atoms that change bonding or are connected to unmapped atoms
		boolean[] isReactionCenter = rxn.getReactionCenterMapNos();
		if (isReactionCenter == null)
			return;

		Reaction transformation = new Reaction();
		for (int i=0; i<rxn.getMolecules(); i++) {
			StereoMolecule mol = rxn.getMolecule(i);
			boolean[] includeAtom = new boolean[mol.getAllAtoms()];
			int atomCount = rxn.getReactionCenterAtoms(i, isReactionCenter, includeAtom, null);
			if (atomCount == 0)
				continue;

			atomCount = addOneAtomShell(mol, includeAtom, atomCount);	// extend to first atom shell

			if (mDetail >= 2)
				atomCount = addOneAtomShell(mol, includeAtom, atomCount);	// extend to second atom shell

			int[] old2NewAtom = new int[mol.getAllAtoms()];
			StereoMolecule newMol = new StereoMolecule(atomCount, atomCount+6);	// in most cases this should be large enough
			mol.copyMoleculeByAtoms(newMol, includeAtom, true, old2NewAtom);

			addNeighbourCounts(mol, newMol, old2NewAtom);

			if (mDetail == 1 || mDetail == 3) {
				// TODO add detail query features
				}

			if (i < rxn.getReactants())
				transformation.addReactant(newMol);
			else
				transformation.addProduct(newMol);
			}

		String[] encodedTransformation = ReactionEncoder.encode(transformation, false);
		if (encodedTransformation != null) {
			getTableModel().setTotalValueAt(encodedTransformation[0], row, firstNewColumn);
			getTableModel().setTotalValueAt(encodedTransformation[1], row, firstNewColumn+1);
			}
		}

	private int addOneAtomShell(StereoMolecule mol, boolean[] includeAtom, int count) {
		boolean[] oldIncludeAtom = includeAtom.clone();
		for (int bond=0; bond<mol.getAllBonds(); bond++) {
			int atom1 = mol.getBondAtom(0, bond);
			int atom2 = mol.getBondAtom(1, bond);
			if (!includeAtom[atom1] && oldIncludeAtom[atom2]) {
				includeAtom[atom1] = true;
				count++;
				}
			if (!includeAtom[atom2] && oldIncludeAtom[atom1]) {
				includeAtom[atom2] = true;
				count++;
				}
			}
		return count;
		}

	private void addNeighbourCounts(StereoMolecule mol, StereoMolecule fragment, int[] mol2fragAtom) {
		for (int atom=0; atom<mol.getAtoms(); atom++) {
			int fAtom = mol2fragAtom[atom];
			if (fAtom != -1) {
				if (mol.getConnAtoms(atom) == fragment.getConnAtoms(fAtom)) {	// if we have copied all atom neighbours
					if (fragment.getLowestFreeValence(fAtom) > 0)
						fragment.setAtomQueryFeature(fAtom, Molecule.cAtomQFNoMoreNeighbours, true);
					}
				else {	// if the fragment doesn't have all original neighbours, we set a query feature
					switch (mol.getConnAtoms(atom)) {
					case 1:
						fragment.setAtomQueryFeature(fAtom, Molecule.cAtomQFNeighbours & ~Molecule.cAtomQFNot1Neighbour, true);
						break;
					case 2:
						fragment.setAtomQueryFeature(fAtom, Molecule.cAtomQFNeighbours & ~Molecule.cAtomQFNot2Neighbours, true);
						break;
					case 3:
						fragment.setAtomQueryFeature(fAtom, Molecule.cAtomQFNeighbours & ~Molecule.cAtomQFNot3Neighbours, true);
						break;
					case 4:
						fragment.setAtomQueryFeature(fAtom, Molecule.cAtomQFNeighbours & ~Molecule.cAtomQFNot4Neighbours, true);
						break;
						}
					}
				}
			}
		}
	}
