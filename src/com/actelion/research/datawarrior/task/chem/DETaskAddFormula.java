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

import com.actelion.research.chem.MolecularFormula;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.datawarrior.DEFrame;

import java.util.Arrays;
import java.util.Comparator;


public class DETaskAddFormula extends DETaskAbstractFromStructure {
	public static final String TASK_NAME = "Add Molecular Formula";

	public DETaskAddFormula(DEFrame parent) {
		super(parent, DESCRIPTOR_NONE, true, true);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/chemistry.html#AddFormula";
		}

	@Override
    protected int getNewColumnCount() {
		return 1;
		}

	@Override
    protected String getNewColumnName(int column) {
		return "Molecular Formula";
		}

	@Override
	public boolean hasExtendedDialogContent() {
		return false;
		}

	@Override
	protected String getNewColumnValue(StereoMolecule mol, Object descriptor, int column) {
		if (mol.getAllAtoms() == 0)
			return "";

        int[] fragmentNo = new int[mol.getAllAtoms()];
        int fragments = mol.getFragmentNumbers(fragmentNo, false,true);
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
