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
import com.actelion.research.chem.ExtendedDepictor;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.coords.CoordinateInventor;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.chem.reaction.ReactionEncoder;
import com.actelion.research.chem.reaction.Reactor;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DETable;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.datawarrior.task.chem.DETaskCoreBasedSAR;
import com.actelion.research.gui.JEditableChemistryView;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;

public class DETaskMergeColumns extends ConfigurableTask implements ActionListener,ListSelectionListener {
	private static final String PROPERTY_TARGET_COLUMN = "newColumn";
	private static final String PROPERTY_REMOVE_SOURCE_COLUMNS = "remove";
	private static final String PROPERTY_COLUMN_LIST = "columnList";
	private static final String PROPERTY_TRANSFORMATION = "transformation";
	private static final String PROPERTY_KEEP_COORDS = "keepCoords";

	private static final int WARNING_CYCLIC_REFERENCE = 1;
	private static final int WARNING_SELF_REFERENCE = 2;
	private static final String[] WARNING_TEXT = {
		"Couldn't resolve R-groups within R-groups\nbecause of cyclic references",
		"Self referencing R-group(s) found",
		};

	public static final String TASK_NAME = "Merge Columns";

	private CompoundTableModel	mTableModel;
	private JList<String>		mListColumns;
	private JComboBox			mComboBoxTargetColumn;
	private JTextArea			mTextArea;
	private JCheckBox			mCheckBoxRemove,mCheckBoxKeepCoords,mCheckBoxUseTransformation;
	private DETable				mTable;
	private JEditableChemistryView mTransformationView;
	private int                 mWarnings;

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
								  TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap,
								  TableLayout.PREFERRED, TableLayout.PREFERRED,
							      TableLayout.PREFERRED, gap/2, HiDPIHelper.scale(72), gap } };
		content.setLayout(new TableLayout(size));

		content.add(new JLabel("Name of target column (new or existing):"), "1,1");
		mComboBoxTargetColumn = new JComboBox();
		mComboBoxTargetColumn.setEditable(true);
		for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
			if (mTableModel.getColumnSpecialType(i) == null
			 || mTableModel.getColumnSpecialType(i).equals(CompoundTableConstants.cColumnTypeIDCode))
				mComboBoxTargetColumn.addItem(mTableModel.getColumnTitle(i));
		content.add(mComboBoxTargetColumn, "1,3");

		content.add(new JLabel("Select source columns to be merged:"), "1,5");

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
			mListColumns.addListSelectionListener(this);
			scrollPane = new JScrollPane(mListColumns);
			}
		else {
			mTextArea = new JTextArea();
			scrollPane = new JScrollPane(mTextArea);
			}
		scrollPane.setPreferredSize(new Dimension(HiDPIHelper.scale(240),HiDPIHelper.scale(160)));
		content.add(scrollPane, "1,7");

		mCheckBoxRemove = new JCheckBox("Remove source columns after merging");
		content.add(mCheckBoxRemove, "1,9");

		mCheckBoxKeepCoords = new JCheckBox("Keep atom coords of largest molecule");
		content.add(mCheckBoxKeepCoords, "1,10");

		mCheckBoxUseTransformation = new JCheckBox("Use reaction for merging structures:");
		mCheckBoxUseTransformation.addActionListener(this);
		content.add(mCheckBoxUseTransformation, "1,11");

		mTransformationView = new JEditableChemistryView(ExtendedDepictor.TYPE_REACTION);
		mTransformationView.getReaction().setFragment(true);
		content.add(mTransformationView, "1,13,3,13");

		return content;
		}


	@Override
	public void valueChanged(ListSelectionEvent e) {
		enableItems();
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		enableItems();
		}

	private void enableItems() {
		boolean structureColumnsOnly = structureColumnsOnly();
		mCheckBoxUseTransformation.setEnabled(structureColumnsOnly);
		mCheckBoxKeepCoords.setEnabled(structureColumnsOnly & !mCheckBoxUseTransformation.isSelected());
		mTransformationView.setEnabled(structureColumnsOnly && mCheckBoxUseTransformation.isSelected());
		}

	private boolean structureColumnsOnly() {
		if (mListColumns == null)
			return true;

		List<String> selection = mListColumns.getSelectedValuesList();
		if (selection.isEmpty())
			return false;

		for (String item:selection)
			if (!mTableModel.isColumnTypeStructure(mTableModel.findColumn(item)))
				return false;

		return true;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/data.html#MergeColumns";
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties p = new Properties();

		p.setProperty(PROPERTY_TARGET_COLUMN, (String)mComboBoxTargetColumn.getSelectedItem());

		String columnNames = isInteractive() ?
				  getSelectedColumnsFromList(mListColumns, mTableModel)
				: mTextArea.getText().replace('\n', '\t');
		if (columnNames != null && columnNames.length() != 0)
			p.setProperty(PROPERTY_COLUMN_LIST, columnNames);

		p.setProperty(PROPERTY_REMOVE_SOURCE_COLUMNS, mCheckBoxRemove.isSelected() ? "true" : "false");

		p.setProperty(PROPERTY_KEEP_COORDS, mCheckBoxKeepCoords.isSelected() ? "true" : "false");

		if (mCheckBoxUseTransformation.isSelected()) {
			Reaction transformation = mTransformationView.getReaction();
			if (!transformation.isEmpty())
				p.setProperty(PROPERTY_TRANSFORMATION, ReactionEncoder.encode(transformation, false,
						ReactionEncoder.INCLUDE_DEFAULT | ReactionEncoder.RETAIN_REACTANT_AND_PRODUCT_ORDER));
		}

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
		int textColumnCount = 0;
		int structureColumnCount = 0;
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
			if (mTableModel.getColumnSpecialType(column) == null)
				textColumnCount++;
			else if (mTableModel.getColumnSpecialType(column).equals(CompoundTableConstants.cColumnTypeIDCode))
				structureColumnCount++;
			}
		return textColumnCount >= 2 || structureColumnCount >= 2;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String targetColumn = configuration.getProperty(PROPERTY_TARGET_COLUMN);
		// targetColumn may be null for compatibility reasons: null means that first source column is target column
		if (targetColumn != null && targetColumn.length() == 0) {
			showErrorMessage("No target column defined.");
			return false;
			}

		String columnList = configuration.getProperty(PROPERTY_COLUMN_LIST);
		if (columnList == null) {
			showErrorMessage("No source columns defined.");
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
			if (idcodeFound) {
				String transformation = configuration.getProperty(PROPERTY_TRANSFORMATION, "");
				if (transformation.length() != 0) {
					Reaction rxn = ReactionEncoder.decode(transformation, false);
					if (rxn.isEmpty()) {
						showErrorMessage("No tranformation reaction defined.");
						return false;
						}
					if (rxn.getReactants() != column.length) {
						showErrorMessage("The number of reactants in transformation reaction\ndoes not match number of structure columns.");
						return false;
						}
					if (!rxn.isMapped()) {
						showErrorMessage("The transformation reaction seems to be unmapped or improperly mapped.");
						return false;
						}
					}
				}
			if (targetColumn != null) {
				int tc = mTableModel.findColumn(targetColumn);
				if (tc != -1) {
					if (idcodeFound && !CompoundTableConstants.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(tc))) {
						showErrorMessage("When merging chemical structures,\n then the target column must be a structure column.");
						return false;
						}
					if (alphaNumFound && mTableModel.getColumnSpecialType(tc) != null) {
						showErrorMessage("When merging alphanumerical data,\n then the target column cannot be a chemistry column.");
						return false;
						}
					}
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
		String targetColumnName = configuration.getProperty(PROPERTY_TARGET_COLUMN);
		String[] columnName = configuration.getProperty(PROPERTY_COLUMN_LIST).split("\\t");
		boolean removeSourceColumns = "true".equals(configuration.getProperty(PROPERTY_REMOVE_SOURCE_COLUMNS, "true"));
		boolean keepCoords = "true".equals(configuration.getProperty(PROPERTY_KEEP_COORDS, "true"));

		int[] column = new int[columnName.length];
		for (int i=0; i<columnName.length; i++)
			column[i] = mTableModel.findColumn(columnName[i]);

		sortByVisibleOrder(column);

		boolean isStructureMerge = CompoundTableConstants.cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(column[0]));

		int targetColumn = (targetColumnName == null) ? column[0] : mTableModel.findColumn(targetColumnName);
		int targetCoordsColumn = -1;
		boolean createTargetColumn = (targetColumn == -1);

		if (createTargetColumn) {
			String[] title = new String[isStructureMerge ? 2 : 1];
			title[0] = targetColumnName;
			if (isStructureMerge)
				title[1] = CompoundTableConstants.cColumnType2DCoordinates;
			targetColumn = mTableModel.addNewColumns(title);
			if (isStructureMerge)
				targetCoordsColumn = targetColumn + 1;
			}

		if (isStructureMerge && !createTargetColumn) {
			targetCoordsColumn = mTableModel.getChildColumn(targetColumn, CompoundTableConstants.cColumnType2DCoordinates);
			}

		// either idcode & coords or text & detail
		Object[][] result = new Object[mTableModel.getTotalRowCount()][2];

		if (isStructureMerge) {
			String transformation = configuration.getProperty(PROPERTY_TRANSFORMATION, "");
			Reaction rxn = (transformation.length() == 0) ? null : ReactionEncoder.decode(transformation, true);

			startProgress("Merging structures...", 0, mTableModel.getTotalRowCount());
			for (int row = 0; row<mTableModel.getTotalRowCount(); row++) {
				if ((row & 255) == 255)
					updateProgress(row);

				if (rxn == null)
					mergeStructureCells(mTableModel.getTotalRecord(row), column, result[row], keepCoords);
				else
					mergeCellsByTransformation(mTableModel.getTotalRecord(row), column, result[row], new Reactor(rxn));
				}

			for (int i=0; i<WARNING_TEXT.length; i++)
				if ((mWarnings & (1 << i)) != 0)
					showErrorMessage(WARNING_TEXT[i]+" in some row(s).");
			}
		else {
			for (int row = 0; row<mTableModel.getTotalRowCount(); row++)
				mergeTextCells(mTableModel.getTotalRecord(row), column, result[row]);
			}

		if (!isStructureMerge) {
			mTableModel.setColumnProperties(targetColumn, mergeColumnProperties(column, null));
			}
		else if (createTargetColumn) {
			mTableModel.setColumnProperty(targetColumn, CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnTypeIDCode);
			mTableModel.setColumnProperty(targetCoordsColumn, CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnType2DCoordinates);
			mTableModel.setColumnProperty(targetCoordsColumn, CompoundTableConstants.cColumnPropertyParentColumn, mTableModel.getColumnTitleNoAlias(targetColumn));
			}

		final int tc1 = targetColumn;
		final int tc2 = targetCoordsColumn;
		SwingUtilities.invokeLater(() -> {
			for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
				CompoundRecord record = mTableModel.getTotalRecord(row);
				record.setData(result[row][0], tc1);
				if (!isStructureMerge)
					record.setDetailReferences(tc1, (String[][])result[row][1]);
				else if (tc2 != -1)
					record.setData(result[row][1], tc2);
				}
			if (createTargetColumn)
				mTableModel.finalizeNewColumns(tc1, this);
			else if (isStructureMerge)
				mTableModel.finalizeChangeChemistryColumn(column[0], 0, mTableModel.getTotalRowCount(), true);
			else
				mTableModel.finalizeChangeAlphaNumericalColumn(column[0], 0, mTableModel.getTotalRowCount());
			} );

		if (removeSourceColumns) {
			SwingUtilities.invokeLater(() -> {
				boolean[] removeColumn = new boolean[mTableModel.getTotalColumnCount()];
				int removalCount = 0;
				for (int i=0; i<column.length; i++) {
					if (column[i] != tc1 && column[i] != tc2) {
						removeColumn[column[i]] = true;
						removalCount++;
						}
					}
				mTableModel.removeColumns(removeColumn, removalCount);
				} );
			}
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String columnNames = configuration.getProperty(PROPERTY_COLUMN_LIST, "");
		if (isInteractive())
			selectColumnsInList(mListColumns, columnNames, mTableModel);
		else
			mTextArea.setText(columnNames.replace('\t', '\n'));

		String targetColumnName = configuration.getProperty(PROPERTY_TARGET_COLUMN);
		// historically null was used for: target column is first of source columns
		if (targetColumnName == null) {
			int index = columnNames.indexOf('\t');
			targetColumnName = (index == -1) ? columnNames : columnNames.substring(0, index);
			}
		mComboBoxTargetColumn.setSelectedItem(targetColumnName);

		mCheckBoxRemove.setSelected("true".equals(configuration.getProperty(PROPERTY_REMOVE_SOURCE_COLUMNS, "true")));

		mCheckBoxKeepCoords.setSelected("true".equals(configuration.getProperty(PROPERTY_KEEP_COORDS, "true")));

		String transformation = configuration.getProperty(PROPERTY_TRANSFORMATION, "");
		mCheckBoxUseTransformation.setSelected(transformation.length() != 0);
		if (transformation.length() != 0)
			mTransformationView.setContent(ReactionEncoder.decode(transformation, true));

		enableItems();
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mComboBoxTargetColumn.setSelectedItem("Merged Data");

		if (isInteractive())
			mListColumns.clearSelection();
		else
			mTextArea.setText("");

		mCheckBoxRemove.setSelected(false);

		mCheckBoxKeepCoords.setSelected(true);

		mCheckBoxUseTransformation.setSelected(false);

		enableItems();
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

	private void mergeTextCells(CompoundRecord record, int[] column, Object[] result) {
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

		result[0] = (buf.length() == 0) ? null : buf.toString().getBytes();
		result[1] = detail;
		}

	private void mergeStructureCells(CompoundRecord record, int[] sourceColumn, Object[] result, boolean keepCoords) {
		boolean[] isRGroup = new boolean[sourceColumn.length];
		boolean[] wasAdded = new boolean[sourceColumn.length];
		int[] rGroupIndex = getRGroupIndexes(sourceColumn, isRGroup);

		int largestMoleculeIndex = -1;
		int largestMoleculeSize = 0;
		int rGroupCount = 0;

		StereoMolecule[] mol = new StereoMolecule[sourceColumn.length];
		StereoMolecule[] rGroup = new StereoMolecule[sourceColumn.length];    // we need to cache R-groups, before starting to replace Rn atoms
		for (int i=0; i<sourceColumn.length; i++) {
			mol[i] = mTableModel.getChemicalStructure(record, sourceColumn[i], CompoundTableModel.ATOM_COLOR_MODE_NONE, mol[i]);
			if (mol[i] != null) {
				mol[i].ensureHelperArrays(Molecule.cHelperNeighbours);
				if (isRGroup[i]) {
					rGroup[i] = mol[i].getCompactCopy();
					rGroupCount++;
					}
				else if (largestMoleculeSize < mol[i].getAtoms()) {
					largestMoleculeSize = mol[i].getAtoms();
					largestMoleculeIndex = i;
					}
				}
			}

		/* We check for all R-groups, which of them contain other R-groups that we can resolve, because they are known.
		 * Then we repeatedly go through all R-group structures and replace all Rn-atoms, of which we know the structure
		 * and where the structure doesn't contain other known R-groups, with the structure.
		 * This, of course, implies that we don't have cyclic R-group inclusion, but it allows recursive R-group inclusion:
		 * R1 contains R2 and R2 contains R3 is allowed, while R1 contains R2 and R2 contains R1 is not allowed.
		 */
		if (rGroupCount != 0) {
			int notFullyResolvedRGroupCount = 0;
			int[] referencedKnownRGroups = new int[sourceColumn.length];
			for (int i=0; i<sourceColumn.length; i++) {
				if (mol[i] != null && isRGroup[i]) {
					for (int atom=0; atom<mol[i].getAllAtoms(); atom++) {
						int atomicNo = mol[i].getAtomicNo(atom);
						if (atomicNo >= 129 && atomicNo <= 144) {
							int rgi = rGroupIndex[(atomicNo>=142) ? atomicNo - 141 : atomicNo - 125];
							if (rgi == i) {
								mWarnings |= WARNING_SELF_REFERENCE;
								return;
								}
							if (rgi != -1)
								referencedKnownRGroups[i] |= (1 << rgi);
							}
						}
					if (referencedKnownRGroups[i] != 0)
						notFullyResolvedRGroupCount++;
					}
				}
			while (notFullyResolvedRGroupCount != 0) {
				boolean hasResolvedRGroups = false;
				for (int i=0; i<sourceColumn.length; i++) {
					// For every R-group resolve those R-groups, which don't include other R-groups
					if (referencedKnownRGroups[i] != 0) {
						int resolvedRGroupIndexes = resolveRGroups(mol[i], rGroup, rGroupIndex, wasAdded, referencedKnownRGroups, keepCoords);
						if (resolvedRGroupIndexes != 0) {
							rGroup[i] = mol[i].getCompactCopy();
							referencedKnownRGroups[i] &= ~resolvedRGroupIndexes;
							hasResolvedRGroups = true;
							}
						if (referencedKnownRGroups[i] == 0)
							notFullyResolvedRGroupCount--;
						}
					}
				if (!hasResolvedRGroups) {
					mWarnings |= WARNING_CYCLIC_REFERENCE;
					return;
					}
				}
			}

		StereoMolecule merged;

		if (largestMoleculeSize != 0) {
			merged = mol[largestMoleculeIndex];
			wasAdded[largestMoleculeIndex] = true;
			}
		else {
			merged = new StereoMolecule();
			}

		if (keepCoords)
			for (int atom=0; atom<merged.getAllAtoms(); atom++)
				merged.setAtomMarker(atom, true);  // marker to keep this atom's coordinates

		boolean needsCoordinateUpdate = (rGroupCount != 0)
				&& (resolveRGroups(merged, rGroup, rGroupIndex, wasAdded, null, keepCoords) != 0);

		for (int i=0; i<sourceColumn.length; i++) {
			if (mol[i] != null && !wasAdded[i]) {
				merged.addMolecule(mol[i]);
				needsCoordinateUpdate = true;
				}
			}

		if (needsCoordinateUpdate)
			new CoordinateInventor(keepCoords ? CoordinateInventor.MODE_KEEP_MARKED_ATOM_COORDS : 0).invent(merged);

		Canonizer canonizer = new Canonizer(merged);
		result[0] = canonizer.getIDCode().getBytes();
		result[1] = canonizer.getEncodedCoordinates().getBytes();
		}

	/**
	 * If the molecule mol contains Rn atoms and if the respective Rn group exists (rGroupIndex[n] != -1),
	 * then the Rn-atom is replaced by respective R-group structure taken from rGroup[rGroupIndex[n]].
	 * If that R-group structure contains an atom labelled '1','2',... ('x') indicating a ring closure
	 * connection back to the molecule and if mol contains a Rx atom, then the Rx atom and the labelled
	 * atom are replaced by a bond connecting their neighbours and closing the ring.
	 * If referencedKnownRGroups is given, then only those R-groups may be resolved, which don't reference
	 * other R-groups.
	 * @param mol
	 * @param rGroup
	 * @param rGroupIndex
	 * @param wasAdded
	 * @param referencedKnownRGroups if not null, then
	 * @param keepCoords of largest fragment
	 * @return bits representing all in mol resolved R-group indexes
	 */
	private int resolveRGroups(StereoMolecule mol, StereoMolecule[] rGroup, int[] rGroupIndex, boolean[] wasAdded, int[] referencedKnownRGroups, boolean keepCoords) {
		int rGroupIndexesResolved = 0;

		int originalAtomCount = mol.getAllAtoms();
		boolean needsDeletion = false;
		boolean needsArrangement = false;
		for (int atom1=0; atom1<originalAtomCount; atom1++) {
			int atomicNo1 = mol.getAtomicNo(atom1);

			// The Rn atom should exactly have one neighbour, but mol may have been edited by an evil user
			if (atomicNo1 >= 129 && atomicNo1 <= 144 && mol.getConnAtoms(atom1) >= 1) {
				int coreAtom = mol.getConnAtom(atom1, 0);
				int rGroupIndex1 = rGroupIndex[atomicNo1 >= 142 ? atomicNo1 - 141 : atomicNo1 - 125];
				if (rGroupIndex1 != -1
				 && (referencedKnownRGroups == null
				  || referencedKnownRGroups[rGroupIndex1] == 0)) {  // if the R-group does not include other known (=resolvable) R-groups
					if (rGroup[rGroupIndex1] == null) {
						mol.setAtomicNo(atom1, 1);
						}
					else {
						int atomStart = mol.getAllAtoms();
						int bondStart = mol.getAllBonds();
						wasAdded[rGroupIndex1] = true;
						needsArrangement = true;
						for (int atom2=atomStart; atom2<mol.getAllAtoms(); atom2++) {
							if (mol.getAtomicNo(atom2) == 0) {
								if (mol.getAtomCustomLabel(atom2) == null) {
									// Primary attachment points are atomicNo==0 with no custom label.
									double angle = mol.getBondAngle(coreAtom, atom1);
									int bondType = mol.getBondType(mol.getConnBond(atom1, 0));
									mol.addSubstituent(rGroup[rGroupIndex1], coreAtom, angle, bondType);
									mol.markAtomForDeletion(atom1);
									needsDeletion = true;
									}
								else {
									// Atoms with atomicNo==0 and customs labels '1','2',etc encode links back to core,
									// when R-groups have multiple attachments to the core fragment.
									try {
										// TODO improve stereo chemistry handling
										int backLinkRGroupIndex = Integer.parseInt(mol.getAtomCustomLabel(atom2));
										if (backLinkRGroupIndex >= 1 && backLinkRGroupIndex <= 16) {
											for (int atom3=0; atom3<atomStart; atom3++) {
												int atomicNo3 = mol.getAtomicNo(atom3);
												if (atomicNo3 >= 129 && atomicNo3 <= 144 && mol.getConnAtoms(atom3) >= 1) {
													int rGroupNo3 = (atomicNo3 >= 142) ? atomicNo3 - 141 : atomicNo3 - 125;
													if (rGroupNo3 == backLinkRGroupIndex) {
														for (int bond2=bondStart; bond2<mol.getAllBonds(); bond2++) {
															for (int j=0; j<2; j++) {
																if (mol.getBondAtom(j, bond2) == atom2) {
																	mol.setBondAtom(j, bond2, mol.getConnAtom(atom3, 0));
																	}
																}
															}
														mol.markAtomForDeletion(atom2);
														mol.markAtomForDeletion(atom3);
														needsDeletion = true;
														if (rGroupIndex[rGroupNo3] != -1)
															wasAdded[rGroupIndex[rGroupNo3]] = true;
														break;
														}
													}
												}
											}
										}
									catch (NumberFormatException nfe) {
										nfe.printStackTrace();
										}
									}
								}
							}
						}
					rGroupIndexesResolved |= (1 << rGroupIndex1);
					}
				}
			}

		if (needsDeletion)
			mol.deleteMarkedAtomsAndBonds();

		if (needsArrangement)
			new CoordinateInventor(keepCoords ? CoordinateInventor.MODE_KEEP_MARKED_ATOM_COORDS : 0).invent(mol);

		return rGroupIndexesResolved;
		}

	private int[] getRGroupIndexes(int[] sourceColumn, boolean[] isRGroup) {
		int[] rGroupIndex = new int[17];
		Arrays.fill(rGroupIndex, -1);

		for (int i=0; i<sourceColumn.length; i++) {
			int rGroup = DETaskCoreBasedSAR.getRGoupNoFromColumnName(mTableModel, sourceColumn[i]);
			if (rGroup != -1 && rGroup <= 16 && rGroupIndex[rGroup] == -1) {
				rGroupIndex[rGroup] = i;
				isRGroup[i] = true;
				}
			}

		return rGroupIndex;
		}

	private void mergeCellsByTransformation(CompoundRecord record, int[] sourceColumn, Object[] result, Reactor reactor) {
		StereoMolecule[] mol = new StereoMolecule[sourceColumn.length];
		for (int i=0; i<sourceColumn.length; i++) {
			mol[i] = mTableModel.getChemicalStructure(record, sourceColumn[i], CompoundTableModel.ATOM_COLOR_MODE_NONE, mol[i]);
			if (mol[i] == null || mol[i].getAllAtoms() == 0)
				return;

			mol[i].ensureHelperArrays(Molecule.cHelperNeighbours);
			}

		for (int i=0; i<mol.length; i++)
			reactor.setReactant(i, mol[i]);

		StereoMolecule[][] products = reactor.getProducts();

		if (products.length != 0 && products[0].length != 0) {
			Canonizer canonizer = new Canonizer(products[0][0]);
			result[0] = canonizer.getIDCode().getBytes();
			result[1] = canonizer.getEncodedCoordinates().getBytes();
			}
		}
	}
