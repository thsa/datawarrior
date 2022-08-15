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

import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.datawarrior.DEFrame;


public class DETaskAddCIPInfo extends DETaskAbstractFromStructure {
	public static final String TASK_NAME = "Add CIP Info";

	public DETaskAddCIPInfo(DEFrame parent) {
		super(parent, DESCRIPTOR_NONE, true, true);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean hasExtendedDialogContent() {
		return false;
		}

	@Override
    protected int getNewColumnCount() {
		return 3;
		}

	@Override
    protected String getNewColumnName(int column) {
		String[] columnName = { "CIP Info R/S", "CIP Info E/Z", "CIP Info Text" };
		return columnName[column];
		}

	@Override
	protected String getNewColumnValue(StereoMolecule mol, Object descriptor, int column) {
		if (mol.getAllAtoms() == 0)
			return "";

		mol.ensureHelperArrays(Molecule.cHelperParities);
		if (mol.getStereoCenterCount() == 0)
			return "";

		StringBuilder info = new StringBuilder();

		if (column == 0) {
			for (int atom = 0; atom < mol.getAtoms(); atom++) {
				if (mol.isAtomStereoCenter(atom)) {
					if (info.length() != 0)
						info.append(" ");
					info.append(Integer.toString(atom)
							+ (mol.getAtomESRType(atom) == Molecule.cESRTypeAnd ? "&" + (mol.getAtomESRGroup(atom) + 1)
							 : mol.getAtomESRType(atom) == Molecule.cESRTypeOr ? "or" + (mol.getAtomESRGroup(atom) + 1)
							 : mol.getAtomParity(atom) == Molecule.cAtomParityUnknown ? "?"
							 : mol.getAtomCIPParity(atom) == Molecule.cAtomCIPParityRorM ? "R"
							 : mol.getAtomCIPParity(atom) == Molecule.cAtomCIPParitySorP ? "S" : "???"));
					}
				}
			}
		else if (column == 1) {
			for (int bond=0; bond<mol.getBonds(); bond++) {
				if (mol.getBondOrder(bond) == 2) {
					if (mol.getBondParity(bond) != Molecule.cBondParityNone && !mol.isSmallRingBond(bond)) {
						if (info.length() != 0)
							info.append(" ");
						info.append(Integer.toString(bond)
								+ (mol.getBondParity(bond) == Molecule.cBondParityUnknown ? "?"
								 : mol.getBondCIPParity(bond) == Molecule.cBondCIPParityZorM ? "Z"
								 : mol.getBondCIPParity(bond) == Molecule.cBondCIPParityEorP ? "E" : "???"));
						}
					}
				}
			}
		else {
			return mol.getChiralText();
			}
		return info.toString();
		}
	}
