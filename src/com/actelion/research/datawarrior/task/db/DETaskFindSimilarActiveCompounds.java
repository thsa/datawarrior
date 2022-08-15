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

package com.actelion.research.datawarrior.task.db;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.chem.DETaskAbstractFromStructure;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.ByteArrayComparator;
import org.openmolecules.chembl.ChemblServerConstants;

import java.util.*;


public class DETaskFindSimilarActiveCompounds extends DETaskAbstractFromStructure implements ChemblServerConstants {
	public static final String TASK_NAME = "Find Similar Compounds In ChEMBL Actives";

	private static final int MAX_COMPOUNDS = 10000;
    private TreeMap<byte[], byte[][][]> mResultMap;

	public DETaskFindSimilarActiveCompounds(DEFrame parent) {
		super(parent, DESCRIPTOR_NONE, false, false);
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
		// we don't want the first column, which contains the query idcode
		// that is identical to the structure column used for the query.
		return COLUMN_NAME_FIND_ACTIVES_SKELSPHERES.length-1;
		}

	@Override
    protected String getNewColumnName(int column) {
		// we don't want the first column, which contains the query idcode
		// that is identical to the structure column used for the query.
		return COLUMN_NAME_FIND_ACTIVES_SKELSPHERES[column+1];
		}

	@Override
	protected void setNewColumnProperties(int firstNewColumn) {
		getTableModel().setColumnProperty(firstNewColumn+FIND_ACTIVES_RESULT_COLUMN_ACTIVE_IDCODE-1,
				CompoundTableModel.cColumnPropertySpecialType, CompoundTableModel.cColumnTypeIDCode);
		}

	@Override
    protected boolean preprocessRows(Properties configuration) {
		int idcodeColumn = getChemistryColumn();
		TreeSet<byte[]> idcodeSet = new TreeSet<byte[]>(new ByteArrayComparator());
		for (int row=0; row<getTableModel().getTotalRowCount(); row++) {
			byte[] idcode = (byte[])getTableModel().getTotalRecord(row).getData(idcodeColumn);
			if (idcode != null) {
				idcodeSet.add(idcode);
				if (idcodeSet.size() > MAX_COMPOUNDS) {
					showErrorMessage("Limit of "+MAX_COMPOUNDS+" unique compound exceeded.");
					return false;
					}
				}
			}

		if (idcodeSet.size() == 0) {
			showErrorMessage("No molecule structures found in current data set.");
			return false;
			}
		
		int blockCount = idcodeSet.size() / 64;
		startProgress("Analysing "+getTableModel().getTotalRowCount()+" molecules on server...", 0, blockCount);
		byte[][] idcodeList = idcodeSet.toArray(new byte[0][]);

		mResultMap = new TreeMap<byte[], byte[][][]>(new ByteArrayComparator());
		byte[][] sublist = null;
		for (int block=0; block<blockCount; block++) {
			updateProgress(block);

			int sublistIndex = block*64;
			int sublistLength = Math.min(idcodeList.length-sublistIndex, 64);
			if (sublist == null || sublist.length != sublistLength)
				sublist = new byte[sublistLength][];
			for (int i=0; i<sublistLength; i++)
				sublist[i] = idcodeList[sublistIndex+i];
			
		    byte[][][] resultTable = new ChemblCommunicator(this, "datawarrior").findActiveCompoundsSkelSpheres(sublist);
			if (resultTable != null && resultTable.length != 0) {
				for (byte[][] row:resultTable) {
					byte[] idcode = row[FIND_ACTIVES_RESULT_COLUMN_QUERY_IDCODE];
					if (mResultMap.containsKey(idcode)) {
						byte[][][] oldTable = mResultMap.get(idcode);
						byte[][][] newTable = new byte[oldTable.length+1][][];
						for (int i=0; i<oldTable.length; i++)
							newTable[i] = oldTable[i];
						newTable[oldTable.length] = row;
						mResultMap.put(idcode, newTable);
						}
					else {
						byte[][][] newTable = new byte[1][][];
						newTable[0] = row;
						mResultMap.put(idcode, newTable);
						}
					}
				}
			}

		if (mResultMap.size() == 0)
			mResultMap = null;

		if (mResultMap == null) {
			showErrorMessage("The server didn't find similar compounds among the 'Reliable ChEMBL Actives'.");
			return false;
			}

		return true;
		}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol, int threadIndex) throws Exception {
		byte[] idcode = (byte[])getTableModel().getTotalRecord(row).getData(getChemistryColumn());
		if (idcode != null) {
			byte[][][] resultList = mResultMap.get(idcode);
			if (resultList != null) {
				Arrays.sort(resultList, new ResultComparator());
				boolean otherCompoundFound = false;
				for (int i=0; i<resultList.length; i++) {
					if (!otherCompoundFound && i != 0
					 && new ByteArrayComparator().compare(resultList[0][FIND_ACTIVES_RESULT_COLUMN_ACTIVE_IDCODE],
														  resultList[i][FIND_ACTIVES_RESULT_COLUMN_ACTIVE_IDCODE]) != 0) {
						otherCompoundFound = true;
						for (int j=0; j<getNewColumnCount(); j++) {
							if (j != FIND_ACTIVES_RESULT_COLUMN_ACTIVE_IDCODE-1
							 && j != FIND_ACTIVES_RESULT_COLUMN_SKELSPHERES-1) {
								if (j == FIND_ACTIVES_RESULT_COLUMN_TARGET-1)
									appendToCell("<< other active compounds >>".getBytes(), row, firstNewColumn+j);
								else
									appendToCell(new byte[0], row, firstNewColumn+j);
								}
							}
						}
					for (int j=0; j<getNewColumnCount(); j++) {
						if (i == 0
						 || (j != FIND_ACTIVES_RESULT_COLUMN_ACTIVE_IDCODE-1
						  && j != FIND_ACTIVES_RESULT_COLUMN_SKELSPHERES-1))	// we only want the first idcode in the cell
							appendToCell(resultList[i][j+1], row, firstNewColumn+j);
						}
					}
				}
			}
		}

	private void appendToCell(byte[] value, int row, int column) {
		CompoundRecord record = getTableModel().getTotalRecord(row);
    	if (record.getData(column) == null)
    		record.setData(value, column);
    	else if (value != null)
    		record.setData((new String((byte[])record.getData(column))
    					  + CompoundTableConstants.cLineSeparator
    					  + new String(value)).getBytes(), column);;
		}

	/**
	 * Compares two result rows based on skeletonSpheres similarities.
	 * The result with the higher similarity is considered smaller
	 * to get highest similarity records first in list after sorting.
	 */
	private class ResultComparator implements Comparator<byte[][]> {

		@Override
		public int compare(byte[][] result1, byte[][] result2) {
			try {
				float skel1 = Float.parseFloat(new String(result1[FIND_ACTIVES_RESULT_COLUMN_SKELSPHERES]));
				float skel2 = Float.parseFloat(new String(result2[FIND_ACTIVES_RESULT_COLUMN_SKELSPHERES]));
				return (skel1 < skel2) ? 1 : (skel1 == skel2) ? 0 : -1;
				}
			catch (NumberFormatException nfe) {
				return 0;	// should never happen
				}
			}
		}
	}
