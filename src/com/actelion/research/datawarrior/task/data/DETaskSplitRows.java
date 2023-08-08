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

package com.actelion.research.datawarrior.task.data;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.task.AbstractSingleColumnTask;
import com.actelion.research.table.model.CompoundTableModel;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskSplitRows extends AbstractSingleColumnTask {
	public static final String TASK_NAME = "Split Rows With Multiple Values";

	private static final String cNLSeparatorRegex = CompoundTableConstants.cLineSeparator.replace("\n", "\\n");
	private static final String cSeparatorRegex = CompoundTableConstants.cEntrySeparator+"|"+cNLSeparatorRegex;

	public DETaskSplitRows(Frame owner, CompoundTableModel tableModel) {
		super(owner, tableModel, true);
	}

	@Override
	public boolean isCompatibleColumn(int column) {
		return getTableModel().isMultiEntryColumn(column)
			|| getTableModel().isColumnTypeStructure(column);
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

//	@Override
//	public String getHelpURL() {
//		return "/html/help/data.html#SplittingRows";
//	}

	@Override
	public void runTask(Properties configuration) {
		int column = getColumn(configuration);

		boolean isStructure = getTableModel().isColumnTypeStructure(column);
		StereoMolecule mol = null;
		int coords2DColumn = -1;
		int[] coords3DColumn = null;
		String[][][] idcodesWithCoords = null;

		String[][] entries = null;
		String regex = null;

		if (isStructure) {
			mol = new StereoMolecule();
			coords2DColumn = getTableModel().getChildColumn(column, CompoundTableConstants.cColumnType2DCoordinates);
			int count = 0;
			for (int i=0; i<getTableModel().getTotalColumnCount(); i++)
				if (getTableModel().getParentColumn(i) == column
				 && getTableModel().getColumnSpecialType(i).equals(CompoundTableConstants.cColumnType3DCoordinates))
					count++;
			if (count != 0) {
				int index = 0;
				coords3DColumn = new int[count];
				for (int i=0; i<getTableModel().getTotalColumnCount(); i++)
					if (getTableModel().getParentColumn(i) == column
					 && getTableModel().getColumnSpecialType(i).equals(CompoundTableConstants.cColumnType3DCoordinates))
						coords3DColumn[index++] = i;
				}
			idcodesWithCoords = new String[getTableModel().getTotalRowCount()][][];
			}
		else {
			entries = new String[getTableModel().getTotalRowCount()][];
			regex = getTableModel().isMultiLineColumn(column) ? "\\n" : "; ";
			}

		int newRowCount = 0;
		startProgress("Analysing column...", 0, getTableModel().getTotalRowCount());
		for (int row=0; row<getTableModel().getTotalRowCount(); row++) {
			if (isStructure) {
				if (getTableModel().getChemicalStructure(getTableModel().getTotalRecord(row), column, CompoundTableModel.ATOM_COLOR_MODE_NONE, mol) != null
				 && mol.getAllAtoms() != 0) {
					int[] fragmentNo = new int[mol.getAllAtoms()];
					int fragmentCount = mol.getFragmentNumbers(fragmentNo, false, false);
					if (fragmentCount > 1) {
						StereoMolecule[] fragment = mol.getFragments(fragmentNo, fragmentCount);
						idcodesWithCoords[row] = new String[fragmentCount][1 + (coords2DColumn != -1 ? 1 : 0) + (coords3DColumn == null ? 0 : coords3DColumn.length)];
						for (int f=0; f<fragmentCount; f++) {
							Canonizer canonizer = new Canonizer(fragment[f]);
							idcodesWithCoords[row][f][0] = canonizer.getIDCode();
							if (coords2DColumn != -1)
								idcodesWithCoords[row][f][1] = canonizer.getEncodedCoordinates();
							}

						if (coords3DColumn != null) {
							// for 3D coordinates we need to split fragments again, because we may have hydrogen atoms in addition
							for (int i=0; i<coords3DColumn.length; i++) {
								getTableModel().getChemicalStructure(getTableModel().getTotalRecord(row), coords3DColumn[i], CompoundTableModel.ATOM_COLOR_MODE_NONE, mol);
								fragmentNo = new int[mol.getAllAtoms()];
								fragmentCount = mol.getFragmentNumbers(fragmentNo, false, false);
								fragment = mol.getFragments(fragmentNo, fragmentCount);
								for (int f=0; f<fragmentCount; f++) {
									Canonizer canonizer = new Canonizer(fragment[f]);
									idcodesWithCoords[row][f][1 + (coords2DColumn != -1 ? 1 : 0) + i] = canonizer.getEncodedCoordinates();
									}
								}
							}
						newRowCount += fragmentCount - 1;
						}
					}
				}
			else {
				entries[row] = separateEntries(getTableModel().getTotalValueAt(row, column), regex);
				newRowCount += entries[row].length - 1;
				}
			}

		if (newRowCount == 0) {
			String object = isStructure ? "fragment" : "value";
			showMessage("No rows were split, because column '"+getTableModel().getColumnTitle(column)+"' doesn't contain multiple "+object+"s.", JOptionPane.INFORMATION_MESSAGE);
			return;
			}

		int[] groupColumn = null;
		if (!isStructure) {
			String columnGroupName = getTableModel().getColumnProperty(column, CompoundTableConstants.cColumnPropertyGroupName);
			if (columnGroupName == null) {
				groupColumn = new int[1];
				groupColumn[0] = column;
				}
			else {
				int count = 0;
				for (int c=0; c<getTableModel().getTotalColumnCount(); c++)
					if (columnGroupName.equals(getTableModel().getColumnProperty(c, CompoundTableConstants.cColumnPropertyGroupName)))
						count++;
				if (count != 0) {
					groupColumn = new int[count];
					count = 0;
					for (int c=0; c<getTableModel().getTotalColumnCount(); c++)
						if (columnGroupName.equals(getTableModel().getColumnProperty(c, CompoundTableConstants.cColumnPropertyGroupName)))
							groupColumn[count++] = c;
					}
				}
			}

		int oldRowCount = getTableModel().getTotalRowCount();
		getTableModel().addNewRows(newRowCount, false);
		int newRow = oldRowCount + newRowCount - 1;
		int rowID = oldRowCount;
		for (int oldRow=oldRowCount-1; oldRow>=0; oldRow--) {
			getTableModel().moveRow(oldRow, newRow--);

			if (isStructure) {
				String[][] idcodeWithCoords = idcodesWithCoords[oldRow];
				if (idcodeWithCoords != null) {
					for (int i=1; i<idcodeWithCoords.length; i++)
						getTableModel().cloneRow(oldRow, newRow--, rowID++);

					for (int f=0; f<idcodeWithCoords.length; f++) {
						int row = newRow+f+1;
						getTableModel().removeChildDescriptorsAndCoordinates(row, column);
						int index = 0;
						getTableModel().setTotalValueAt(idcodeWithCoords[f][index++], row, column);
						if (coords2DColumn != -1)
							getTableModel().setTotalValueAt(idcodeWithCoords[f][index++], row, coords2DColumn);
						if (coords3DColumn != null)
							for (int c:coords3DColumn)
								getTableModel().setTotalValueAt(idcodeWithCoords[f][index++], row, c);
						}
					}
				}
			else {
				String[] entry = entries[oldRow];
				if (entry.length > 1) {
					for (int i=1; i<entry.length; i++)
						getTableModel().cloneRow(oldRow, newRow--, rowID++);

					for (int i=0; i<entry.length; i++)
						getTableModel().setTotalValueAt(entry[i], newRow+i+1, column);

					for (int gc:groupColumn) {
						if (gc != column) {
							String[] ge = separateEntries(getTableModel().getTotalValueAt(oldRow, gc), regex);
							if (ge.length == entry.length) {
								for (int i=0; i<ge.length; i++)
									getTableModel().setTotalValueAt(ge[i], newRow+i+1, gc);
								}
							}
						}
					}
				}
			}

		getTableModel().finalizeNewRows(0, this);
		}

	private String[] separateEntries(String data, String regex) {
		if (data == null || data.length() == 0) {
			String[] entry = { "" };
			return entry;
			}

		return data.split(regex, -1);
	}
}
