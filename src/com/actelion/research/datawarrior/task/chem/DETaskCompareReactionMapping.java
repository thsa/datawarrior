/*
 * Copyright 2021 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91, CH-4123 Allschwil, Switzerland
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

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.ByteArrayComparator;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.util.Arrays;
import java.util.Properties;

public class DETaskCompareReactionMapping extends DETaskAbstractFromReaction {
	public static final String TASK_NAME = "Compare Reaction Mapping";

	private static final String PROPERTY_REACTION_COLUMN = "reactionColumn";

	private JComboBox mComboBoxReactionColumn;
	private int mReactionColumn1,mReactionColumn2,mMappingColumn1,mMappingColumn2;

	public DETaskCompareReactionMapping(DEFrame parent) {
		super(parent, DESCRIPTOR_NONE, false, true);
	}

	@Override
	protected int getNewColumnCount() {
		return 1;
		}

	@Override
	protected String getNewColumnName(int column) {
		return "Reaction Mapping Comparison";
		}

	@Override
	public boolean hasExtendedDialogContent() {
		return true;
	}

	@Override
	public JPanel getExtendedDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {TableLayout.PREFERRED, gap, TableLayout.PREFERRED},
				{TableLayout.PREFERRED} };

		int[] reactionColumn = getCompatibleChemistryColumnList();

		mComboBoxReactionColumn = new JComboBox();
		if (reactionColumn != null)
			for (int i=0; i<reactionColumn.length; i++)
				mComboBoxReactionColumn.addItem(getTableModel().getColumnTitle(reactionColumn[i]));
		mComboBoxReactionColumn.setEditable(!isInteractive());

		JPanel ep = new JPanel();
		ep.setLayout(new TableLayout(size));
		ep.add(new JLabel("Compare to:"), "0,0");
		ep.add(mComboBoxReactionColumn, "2,0");
		return ep;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		String reactionColumn = (String)mComboBoxReactionColumn.getSelectedItem();
		if (reactionColumn != null)
			configuration.setProperty(PROPERTY_REACTION_COLUMN, reactionColumn);
		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		String value = configuration.getProperty(PROPERTY_REACTION_COLUMN, "");
		if (value.length() != 0) {
			int column = getTableModel().findColumn(value);
			if (column != -1) {
				mComboBoxReactionColumn.setSelectedItem(getTableModel().getColumnTitle(column));
				}
			else if (!isInteractive()) {
				mComboBoxReactionColumn.setSelectedItem(value);
				}
			}
		else if (!isInteractive()) {
			mComboBoxReactionColumn.setSelectedItem(getTypeName());
			}
		else if (mComboBoxReactionColumn.getItemCount() != 0) {
			mComboBoxReactionColumn.setSelectedIndex(mComboBoxReactionColumn.getItemCount()-1);
			}
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		if (mComboBoxReactionColumn.getItemCount() != 0)
			mComboBoxReactionColumn.setSelectedIndex(mComboBoxReactionColumn.getItemCount()-1);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isConfigurable() {
		int[] columnList = getCompatibleChemistryColumnList();
		if (columnList == null || columnList.length < 2) {
			showErrorMessage("The comparison of two reaction mapping columns needs at least two existing reaction columns.");
			return false;
			}
		return true;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (!super.isConfigurationValid(configuration, isLive))
			return false;

		if (isLive) {
			int reactionColumn1 = getChemistryColumn(configuration);
			int mappingColumn1 = getTableModel().getChildColumn(reactionColumn1, CompoundTableConstants.cColumnTypeReactionMapping);
			if (mappingColumn1 == -1) {
				showErrorMessage("Mapping of reaction column '"+configuration.getProperty(PROPERTY_CHEMISTRY_COLUMN)+"' not found.");
				return false;
				}
			int reactionColumn2 = getTableModel().findColumn(configuration.getProperty(PROPERTY_REACTION_COLUMN));
			if (reactionColumn2 == -1) {
				showErrorMessage("Reaction column '"+configuration.getProperty(PROPERTY_REACTION_COLUMN)+"' not found.");
				return false;
				}
			int mappingColumn2 = getTableModel().getChildColumn(reactionColumn2, CompoundTableConstants.cColumnTypeReactionMapping);
			if (mappingColumn2 == -1) {
				showErrorMessage("Mapping of reaction column '"+configuration.getProperty(PROPERTY_REACTION_COLUMN)+"' not found.");
				return false;
				}
			}
		return true;
		}

	@Override
	protected boolean preprocessRows(Properties configuration) {
		mReactionColumn1 = getChemistryColumn(configuration);
		mMappingColumn1 = getTableModel().getChildColumn(mReactionColumn1, CompoundTableConstants.cColumnTypeReactionMapping);
		mReactionColumn2 = getTableModel().findColumn(configuration.getProperty(PROPERTY_REACTION_COLUMN));
		mMappingColumn2 = getTableModel().getChildColumn(mReactionColumn2, CompoundTableConstants.cColumnTypeReactionMapping);
		return super.preprocessRows(configuration);
		}

		@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol) {
		CompoundRecord record = getTableModel().getTotalRecord(row);

		byte[] rxncode1 = (byte[])record.getData(mReactionColumn1);
		byte[] rxncode2 = (byte[])record.getData(mReactionColumn2);
		String result = "reaction mismatch";
		if (new ByteArrayComparator().compare(rxncode1, rxncode2) == 0) {
			byte[] mapping1 = (byte[])record.getData(mMappingColumn1);
			byte[] mapping2 = (byte[])record.getData(mMappingColumn2);
			result = (mapping1 == null && mapping2 == null) ? "both null"
					: mapping1 == null ? "rxn1 not mapped"
					: mapping2 == null ? "rxn2 not mapped"
					: compareMappings(record, mapping1, mapping2);
			}

		getTableModel().setTotalDataAt(result.getBytes(), row, firstNewColumn);
		}

	private String compareMappings(CompoundRecord record, byte[] mapping1, byte[] mapping2) {
		if (new ByteArrayComparator().compare(mapping1, mapping2) == 0)
			return "exact";

		// In the following we assume both reactions to be equal but not necessarily canonical

		Reaction rxn1 = getTableModel().getChemicalReaction(record, mReactionColumn1, CompoundTableModel.ATOM_COLOR_MODE_NONE);
		Reaction rxn2 = getTableModel().getChemicalReaction(record, mReactionColumn2, CompoundTableModel.ATOM_COLOR_MODE_NONE);

		try { rxn1.validateMapping(); } catch (Exception e) { return "duplicate mapNos in rxn1"; }
		try { rxn2.validateMapping(); } catch (Exception e) { return "duplicate mapNos in rxn2"; }

		StereoMolecule[] mol1 = new StereoMolecule[rxn1.getMolecules()];
		Canonizer[] can1 = new Canonizer[rxn1.getMolecules()];
		for (int i=0; i<rxn1.getMolecules(); i++) {
			mol1[i] = rxn1.getMolecule(i);
			can1[i] = new Canonizer(mol1[i], Canonizer.CREATE_SYMMETRY_RANK);
			}

		StereoMolecule[] mol2 = new StereoMolecule[rxn2.getMolecules()];
		Canonizer[] can2 = new Canonizer[rxn2.getMolecules()];
		for (int i=0; i<rxn2.getMolecules(); i++) {
			mol2[i] = rxn2.getMolecule(i);
			can2[i] = new Canonizer(mol2[i], Canonizer.CREATE_SYMMETRY_RANK);
			}

		long[] link1 = createSortedAtomLinks(rxn1, can1);
		long[] link2 = createSortedAtomLinks(rxn2, can2);
		if (link1.length != link2.length)
			return "different";

		for (int i=0; i<link1.length; i++)
			if (link1[i] != link2[i])
				return "different";

		return "match";
		}

	private long[] createSortedAtomLinks(Reaction rxn, Canonizer[] can) {
		int maxMapNo = 0;  // maximum number of atom links
		for (int i=0; i<rxn.getMolecules(); i++) {
			StereoMolecule mol = rxn.getMolecule(i);
			for (int atom=0; atom<mol.getAllAtoms(); atom++) {
				int mapNo = mol.getAtomMapNo(atom);
				if (maxMapNo < mapNo)
					maxMapNo = mapNo;
				}
			}

		int[] mapNoToProduct = new int[maxMapNo];
		int[] mapNoToPAtom = new int[maxMapNo];
		for (int i=0; i<rxn.getProducts(); i++) {
			StereoMolecule mol = rxn.getProduct(i);
			for (int atom=0; atom<mol.getAllAtoms(); atom++) {
				int mapNo = mol.getAtomMapNo(atom) - 1;
				if (mapNo != -1) {
					mapNoToProduct[mapNo] = i;
					mapNoToPAtom[mapNo] = atom;
					}
				}
			}

		long[] link = new long[maxMapNo];
		for (int i=0; i<rxn.getReactants(); i++) {
			StereoMolecule mol = rxn.getReactant(i);
			for (int atom=0; atom<mol.getAllAtoms(); atom++) {
				int mapNo = mol.getAtomMapNo(atom)-1;
				if (mapNo != -1) {
					int productIndex = mapNoToProduct[mapNo];
					int productRank = can[rxn.getReactants()+productIndex].getSymmetryRank(mapNoToPAtom[mapNo]);
					link[mapNo] = ((long)i << 56) + ((long)can[i].getSymmetryRank(atom) << 32) + (productIndex << 24) + productRank;
					}
				}
			}

		Arrays.sort(link);
		return link;
		}
	}
