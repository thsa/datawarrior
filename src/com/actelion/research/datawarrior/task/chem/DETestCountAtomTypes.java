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

import java.util.Properties;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.prediction.TotalSurfaceAreaPredictor;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.AbstractTaskWithoutConfiguration;
import com.actelion.research.table.model.CompoundTableModel;


public class DETestCountAtomTypes extends AbstractTaskWithoutConfiguration {
    public static final long serialVersionUID = 0x20150812;

    public static final String TASK_NAME = "Count Atom Types";

	private CompoundTableModel  mTableModel;

	public DETestCountAtomTypes(DEFrame parent) {
		super(parent, true);
		mTableModel = parent.getTableModel();
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
    public String getTaskName() {
    	return TASK_NAME;
    	}

	@Override
	public boolean isConfigurable() {
		if (mTableModel.getSpecialColumnList(CompoundTableModel.cColumnTypeIDCode) == null) {
			showErrorMessage("No chemical structure column found.");
			return false;
			}

        return true;
		}

	@Override
	public void runTask(Properties configuration) {
		int idcodeColumn = mTableModel.getSpecialColumnList(CompoundTableModel.cColumnTypeIDCode)[0];

		int rowCount = mTableModel.getTotalRowCount();
		int polarAtomTypeCount = TotalSurfaceAreaPredictor.getPolarAtomTypeCount();
		int nonPolarAtomTypeCount = TotalSurfaceAreaPredictor.getNonPolarAtomTypeCount();

		int[][] atomTypeCount = new int[rowCount][polarAtomTypeCount+nonPolarAtomTypeCount];

		StereoMolecule mol = new StereoMolecule();
		TotalSurfaceAreaPredictor sap = new TotalSurfaceAreaPredictor();

		startProgress("Counting atom types...", 0, rowCount);

		for (int r=0; r<rowCount; r++) {
			if (threadMustDie())
				break;
			if ((r % 256) == 255)
			updateProgress(r);

			mTableModel.getChemicalStructure(mTableModel.getTotalRecord(r), idcodeColumn, CompoundTableModel.ATOM_COLOR_MODE_NONE, mol);
			if (mol != null && mol.getAllAtoms() != 0) {
				mol.stripSmallFragments();

				int[] count = sap.getPolarAtomTypeCounts(mol);
				for (int i=0; i<polarAtomTypeCount; i++)
					atomTypeCount[r][i] = count[i];
				
				count = sap.getNonPolarAtomTypeCounts(mol);
				for (int i=0; i<nonPolarAtomTypeCount; i++)
					atomTypeCount[r][polarAtomTypeCount+i] = count[i];
				}
			}

		if (!threadMustDie()) {
			startProgress("Extending table...", 0, rowCount);

			String[] columnTitle = new String[polarAtomTypeCount+nonPolarAtomTypeCount];
			for (int i=0; i<columnTitle.length; i++)
				columnTitle[i] = (i < polarAtomTypeCount) ? "pc"+(i+1) : "npc"+(i-polarAtomTypeCount+1);
			final int firstNewColumn = mTableModel.addNewColumns(columnTitle);
			for (int i=0; i<polarAtomTypeCount+nonPolarAtomTypeCount; i++)
				for (int r=0; r<rowCount; r++)
					mTableModel.setTotalValueAt(""+atomTypeCount[r][i], r, firstNewColumn+i);

			mTableModel.finalizeNewColumns(firstNewColumn, this);
			}
		}
	}
