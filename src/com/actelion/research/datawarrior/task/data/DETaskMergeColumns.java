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
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.coords.CoordinateInventor;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DETable;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class DETaskMergeColumns extends ConfigurableTask {
	private static final String PROPERTY_NEW_COLUMN = "newColumn";
	private static final String PROPERTY_REMOVE_SOURCE_COLUMNS = "remove";
	private static final String PROPERTY_COLUMN_LIST = "columnList";

	public static final String TASK_NAME = "Merge Columns";

	private CompoundTableModel	mTableModel;
	private JList<String>		mListColumns;
	private JTextField			mTextFieldNewColumn;
	private JTextArea			mTextArea;
	private JCheckBox			mCheckBoxRemove;
	private DETable				mTable;

	public DETaskMergeColumns(DEFrame owner) {
		super(owner, true);
		mTableModel = owner.getTableModel();
		mTable = owner.getMainFrame().getMainPane().getTable();
		}

	@Override
	public JPanel createDialogContent() {
		JPanel content = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, HiDPIHelper.scale(20),
								  TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap } };
		content.setLayout(new TableLayout(size));

		content.add(new JLabel("New column name:"), "1,1");
		mTextFieldNewColumn = new JTextField(10);
		content.add(mTextFieldNewColumn, "3,1");
		content.add(new JLabel("(keep empty to merge all into first selected column)"), "1,3,3,3");

		content.add(new JLabel("Select columns to be merged:"), "1,5,3,5");

		ArrayList<String> columnList = new ArrayList<>();
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.getColumnSpecialType(column) == null
			 || mTableModel.getColumnSpecialType(column).equals(CompoundTableConstants.cColumnTypeIDCode))
				columnList.add(mTableModel.getColumnTitle(column));
		String[] itemList = columnList.toArray(new String[0]);
		Arrays.sort(itemList, (s1, s2) -> s1.compareToIgnoreCase(s2));
		JScrollPane scrollPane = null;
		if (isInteractive()) {
			mListColumns = new JList<>(itemList);
			scrollPane = new JScrollPane(mListColumns);
			}
		else {
			mTextArea = new JTextArea();
			scrollPane = new JScrollPane(mTextArea);
			}
		scrollPane.setPreferredSize(new Dimension(HiDPIHelper.scale(240),HiDPIHelper.scale(160)));
		content.add(scrollPane, "1,7,3,7");

		mCheckBoxRemove = new JCheckBox("Remove source columns after merging");
		content.add(mCheckBoxRemove, "1,9,3,9");

		return content;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties p = new Properties();

		if (mTextFieldNewColumn.getText().length() != 0)
			p.setProperty(PROPERTY_NEW_COLUMN, mTextFieldNewColumn.getText());

		String columnNames = isInteractive() ?
				  getSelectedColumnsFromList(mListColumns, mTableModel)
				: mTextArea.getText().replace('\n', '\t');
		if (columnNames != null && columnNames.length() != 0)
			p.setProperty(PROPERTY_COLUMN_LIST, columnNames);

		p.setProperty(PROPERTY_REMOVE_SOURCE_COLUMNS, mCheckBoxRemove.isSelected() ? "true" : "false");

		return p;
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
		int columnCount = 0;
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.getColumnSpecialType(column) == null)
				columnCount++;
		return columnCount >= 2;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String columnList = configuration.getProperty(PROPERTY_COLUMN_LIST);
		if (columnList == null) {
			showErrorMessage("No columns defined.");
			return false;
			}

		if (isLive) {
			String[] columnName = columnList.split("\\t");
			int[] column = new int[columnName.length];
			for (int i=0; i<columnName.length; i++) {
				column[i] = mTableModel.findColumn(columnName[i]);
				if (column[i] == -1) {
					showErrorMessage("Column '"+columnName[i]+"' not found.");
					return false;
					}
				}
			boolean alphaNumFound = false;
			boolean idcodeFound = false;
			for (int i=0; i<column.length; i++) {
				if (mTableModel.getColumnSpecialType(column[i]) == null)
					alphaNumFound = true;
				else if (mTableModel.getColumnSpecialType(column[i]).equals(CompoundTableConstants.cColumnTypeIDCode))
					idcodeFound = true;
				else {
					showErrorMessage("Column '"+columnName[i]+"' has a special type that cannot be merged.");
					return false;
					}
				}
			if (alphaNumFound && idcodeFound) {
				showErrorMessage("Structure columns cannot be merged with alphanumerical columns.");
				return false;
				}
			if (!isExecuting()) {
				StringBuilder conflictingProperties = new StringBuilder();
				mergeColumnProperties(column, conflictingProperties);
				if (conflictingProperties.length() != 0 && JOptionPane.showConfirmDialog(getParentFrame(),
							"Some column properties cannot be merged, because their values don't match:\n"
									+conflictingProperties.toString()+"Do you want to merge anyway?",
							"Warning", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.CANCEL_OPTION)
					return false;
				}
			}

		return true;
		}

	private HashMap<String,String> mergeColumnProperties(int[] columns, StringBuilder conflictingPropertiesMessage) {
		HashMap<String,String> mergedProperties = new HashMap<String,String>();
		TreeMap<String,TreeSet<String>> conflictingProperties = new TreeMap<>();
		TreeSet<String> keysToBeDeleted = (conflictingPropertiesMessage != null) ? null : new TreeSet<>();
		for (int column:columns) {
			HashMap<String,String> properties = mTableModel.getColumnProperties(column);
			for (String key:properties.keySet()) {
				if (mergedProperties.containsKey(key)) {
					if (!mergedProperties.get(key).equals(properties.get(key))) {
						if (conflictingPropertiesMessage == null) {	// we merge anyway and just remove conflicting properties
							keysToBeDeleted.add(key);
							}
						else {
							if (!conflictingProperties.containsKey(key)) {
								TreeSet<String> conflictingValues = new TreeSet<>();
								conflictingValues.add(mergedProperties.get(key));
								conflictingProperties.put(key, conflictingValues);
								}
							conflictingProperties.get(key).add(properties.get(key));
							}
						}
					}
				else {
					mergedProperties.put(key, properties.get(key));
					}
				}
			}

		if (keysToBeDeleted != null)
			for (String key:keysToBeDeleted)
				mergedProperties.remove(key);

		if (conflictingPropertiesMessage != null) {
			for (String key:conflictingProperties.keySet()) {
				conflictingPropertiesMessage.append(key);
				TreeSet<String> conflictingValues = conflictingProperties.get(key);
				boolean isFirst = true;
				for (String value:conflictingValues) {
					conflictingPropertiesMessage.append(isFirst ? ": " : ", ");
					conflictingPropertiesMessage.append(value);
					isFirst = false;
					}
				conflictingPropertiesMessage.append("\n");
				}
			}

		return mergedProperties;
		}

	@Override
	public void runTask(Properties configuration) {
		String targetColumnName = configuration.getProperty(PROPERTY_NEW_COLUMN, "");
		String[] columnName = configuration.getProperty(PROPERTY_COLUMN_LIST).split("\\t");
		boolean removeSourceColumns = "true".equals(configuration.getProperty(PROPERTY_REMOVE_SOURCE_COLUMNS, "true"));

		int[] column = new int[columnName.length];
		for (int i=0; i<columnName.length; i++)
			column[i] = mTableModel.findColumn(columnName[i]);

		sortByVisibleOrder(column);

		boolean isStructureMerge = CompoundTableConstants.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(column[0]));

		int targetColumn = column[0];
		if (targetColumnName.length() != 0) {
			String[] title = new String[isStructureMerge ? 2 : 1];
			title[0] = targetColumnName;
			if (isStructureMerge)
				title[1] = CompoundTableConstants.cColumnType2DCoordinates;
			targetColumn = mTableModel.addNewColumns(title);
			}

		if (isStructureMerge) {
			StereoMolecule[] mol = new StereoMolecule[column.length];
			startProgress("Merging structures...", 0, mTableModel.getTotalRowCount());
			for (int row = 0; row<mTableModel.getTotalRowCount(); row++) {
				if ((row & 255) == 255)
					updateProgress(row);
				mergeStructureCells(mTableModel.getTotalRecord(row), column, targetColumn, mol);
				}
			}
		else {
			for (int row = 0; row<mTableModel.getTotalRowCount(); row++)
				mergeTextCells(mTableModel.getTotalRecord(row), column, targetColumn);
			}

		if (isStructureMerge)
			mTableModel.prepareStructureColumns(targetColumn, mTableModel.getColumnTitleNoAlias(targetColumn), true, false);
		else
			mTableModel.setColumnProperties(targetColumn, mergeColumnProperties(column, null));

		if (targetColumnName.length() != 0)
			mTableModel.finalizeNewColumns(targetColumn, this);
		else
			mTableModel.finalizeChangeAlphaNumericalColumn(column[0], 0, mTableModel.getTotalRowCount());

		if (removeSourceColumns) {
			SwingUtilities.invokeLater(() -> {
				boolean[] removeColumn = new boolean[mTableModel.getTotalColumnCount()];
				int firstColumnIndex = (targetColumnName.length() != 0) ? 0 : 1;
				int removalCount = column.length - firstColumnIndex;
				for (int i=firstColumnIndex; i<column.length; i++)
					removeColumn[column[i]] = true;
				mTableModel.removeColumns(removeColumn, removalCount);
				} );
			}
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mTextFieldNewColumn.setText(configuration.getProperty(PROPERTY_NEW_COLUMN, ""));

		String columnNames = configuration.getProperty(PROPERTY_COLUMN_LIST, "");
		if (isInteractive())
			selectColumnsInList(mListColumns, columnNames, mTableModel);
		else
			mTextArea.setText(columnNames.replace('\t', '\n'));

		mCheckBoxRemove.setSelected("true".equals(configuration.getProperty(PROPERTY_REMOVE_SOURCE_COLUMNS, "true")));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mTextFieldNewColumn.setText("Merged Data");

		if (isInteractive())
			mListColumns.clearSelection();
		else
			mTextArea.setText("");

		mCheckBoxRemove.setSelected(true);
		}

	private void sortByVisibleOrder(int[] column) {
		for (int i=0; i<column.length; i++) {
			int viewIndex = mTable.convertTotalColumnIndexToView(column[i]);
			if (viewIndex != -1)
				column[i] |= (viewIndex << 16);
			}
		Arrays.sort(column);
		for (int i=0; i<column.length; i++)
			column[i] &= 0x0000FFFF;
		}

	private void mergeTextCells(CompoundRecord record, int[] column, int targetColumn) {
		StringBuffer buf = new StringBuffer();
		String separator = mTableModel.isMultiLineColumn(column[0]) ?
				CompoundTableModel.cLineSeparator : CompoundTableModel.cEntrySeparator;

		for (int i=0; i<column.length; i++) {
			String value = mTableModel.encodeData(record, column[i]);
			if (value.length() != 0) {
				if (buf.length() != 0)
					buf.append(separator);
				buf.append(value);
				}
			}

		int[] detailCount = null;
		for (int i=0; i<column.length; i++) {
			String[][] d = record.getDetailReferences(column[i]);
			if (d != null) {
				if (detailCount == null)
					detailCount = new int[d.length];
				for (int j=0; j<d.length; j++)
					if (d[j] != null)
						detailCount[j] += d[j].length;
				}
			}
		String[][] detail = null;
		if (detailCount != null) {
			detail = new String[detailCount.length][];
			int[] index = new int[detailCount.length];
			for (int i=0; i<detailCount.length; i++)
				detail[i] = new String[detailCount[i]];

			for (int i=0; i<column.length; i++) {
				String[][] d = record.getDetailReferences(column[i]);
				if (d != null) {
					for (int j=0; j<d.length; j++)
						if (d[j] != null)
							for (int k=0; k<d[j].length; k++)
								detail[j][index[j]++] = d[j][k];
					}
				}
			}

		record.setData(mTableModel.decodeData(buf.toString(), targetColumn), targetColumn);
		if (detail != null)
			record.setDetailReferences(targetColumn, detail);
		}

	private void mergeStructureCells(CompoundRecord record, int[] sourceColumn, int targetColumn, StereoMolecule[] mol) {
		boolean[] isRGroup = new boolean[sourceColumn.length];
		boolean[] wasAdded = new boolean[sourceColumn.length];
		int[] rGroupIndex = getRGroupIndexes(sourceColumn, isRGroup);

		int largestMoleculeIndex = -1;
		int largestMoleculeSize = 0;

		StereoMolecule[] rGroup = new StereoMolecule[sourceColumn.length];    // we need to cache R-groups, before starting to replace Rn atoms
		for (int i=0; i<sourceColumn.length; i++) {
			mol[i] = mTableModel.getChemicalStructure(record, sourceColumn[i], CompoundTableModel.ATOM_COLOR_MODE_NONE, mol[i]);
			if (mol[i] != null) {
				mol[i].ensureHelperArrays(Molecule.cHelperNeighbours);
				if (isRGroup[i])
					rGroup[i] = mol[i].getCompactCopy();
				else if (largestMoleculeSize < mol[i].getAtoms()) {
					largestMoleculeSize = mol[i].getAtoms();
					largestMoleculeIndex = i;
					}
				}
			}

		// If some columns contain R-groups, then merge the R-groups into any molecule, which has a respective Rn substituent!
		// Mark R-groups that have been merge one or more times. All other R-groups have to be added as unconnected molecules later.
		// We don't resolve multiple layers of R-grouping, e.g. R1 contains R2 and R2 contains R1.
		for (int i=0; i<sourceColumn.length; i++) {
			if (mol[i] != null) {
				int originalAtomCount = mol[i].getAllAtoms();
				boolean needsDeletion = false;
				boolean needsArrangement = false;
				for (int atom1=0; atom1<originalAtomCount; atom1++) {
					int atomicNo = mol[i].getAtomicNo(atom1);
					mol[i].setAtomMarker(atom1, true);  // marker to keep this atom's coordinates
					// The Rn atom should exactly have one neighbour, but mol may have been edited by an evil user
					if (atomicNo >= 129 && atomicNo <= 144 && mol[i].getConnAtoms(atom1) >= 1) {
						int coreAtom = mol[i].getConnAtom(atom1, 0);
						int rGroupNo = (atomicNo >= 142) ? atomicNo - 141 : atomicNo - 125;
						if (rGroupIndex[rGroupNo] != -1 && rGroupIndex[rGroupNo] != i) {
							if (rGroup[rGroupIndex[rGroupNo]] == null) {
								mol[i].markAtomForDeletion(atom1);
								needsDeletion = true;
								}
							else {
								int atomStart = mol[i].getAllAtoms();
								int bondStart = mol[i].getAllBonds();
								mol[i].addMolecule(rGroup[rGroupIndex[rGroupNo]]);
								wasAdded[rGroupIndex[rGroupNo]] = true;
								needsArrangement = true;
								for (int atom2=atomStart; atom2<mol[i].getAllAtoms(); atom2++) {
									if (mol[i].getAtomicNo(atom2) == 0) {
										for (int bond2=bondStart; bond2<mol[i].getAllBonds(); bond2++) {
											for (int j=0; j<2; j++) {
												if (mol[i].getBondAtom(j, bond2) == atom2) {
													mol[i].setBondAtom(j, bond2, coreAtom);
													}
												}
											}
										mol[i].markAtomForDeletion(atom1);
										mol[i].markAtomForDeletion(atom2);
										needsDeletion = true;
										}
									}
								}
							}
						}
					}

				if (needsDeletion)
					mol[i].deleteMarkedAtomsAndBonds();

				if (needsArrangement)
					new CoordinateInventor(CoordinateInventor.MODE_KEEP_MARKED_ATOM_COORDS).invent(mol[i]);
				}
			}

		if (largestMoleculeSize != 0) {
			StereoMolecule merged = mol[largestMoleculeIndex];
			wasAdded[largestMoleculeIndex] = true;

			boolean needsCoordinateUpdate = false;
			for (int i=0; i<sourceColumn.length; i++) {
				if (mol[i] != null && !wasAdded[i]) {
					merged.addMolecule(mol[i]);
					needsCoordinateUpdate = true;
					}
				}

			if (needsCoordinateUpdate) {
				for (int atom=0; atom<merged.getAllAtoms(); atom++)
					merged.setAtomMarker(atom, atom<largestMoleculeSize);
				new CoordinateInventor(CoordinateInventor.MODE_KEEP_MARKED_ATOM_COORDS).invent(merged);
				}

			Canonizer canonizer = new Canonizer(merged);
			record.setData(canonizer.getIDCode().getBytes(), targetColumn);
			record.setData(canonizer.getEncodedCoordinates().getBytes(), targetColumn + 1);
			}
		}

	private int[] getRGroupIndexes(int[] sourceColumn, boolean[] isRGroup) {
		int[] rGroupIndex = new int[17];
		Arrays.fill(rGroupIndex, -1);

		for (int i=0; i<sourceColumn.length; i++) {
			int rGroup = getRGoupNo(sourceColumn[i]);
			if (rGroup != -1 && rGroup <= 16 && rGroupIndex[rGroup] == -1) {
				rGroupIndex[rGroup] = i;
				isRGroup[i] = true;
				}
			}

		return rGroupIndex;
		}

	private int getRGoupNo(int column) {
		String columnName = mTableModel.getColumnTitle(column);
		if (columnName.length() >= 2 && columnName.charAt(0) == 'R' && Character.isDigit(columnName.charAt(1))) {
			if (columnName.length() == 2 || !Character.isDigit(columnName.charAt(2)))
				return columnName.charAt(1) - '0';
			if (columnName.length() > 3 && Character.isDigit(columnName.charAt(2))
			 && (columnName.length() == 3 || !Character.isDigit(columnName.charAt(3))))
				return Integer.parseInt(columnName.substring(1, 3));
			}

		return -1;
		}
	}
