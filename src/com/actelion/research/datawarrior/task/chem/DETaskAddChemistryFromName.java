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

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.SmilesParser;
import com.actelion.research.chem.SortedStringList;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.name.StructureNameResolver;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.chem.reaction.ReactionEncoder;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.AbstractSingleColumnTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;
import uk.ac.cam.ch.wwmm.opsin.NameToStructure;

import javax.swing.*;
import java.util.Properties;

public class DETaskAddChemistryFromName extends AbstractSingleColumnTask {
	public static final String TASK_NAME = "Add Structures From Name";

//  Cactus is not used anymore.
//	private static final String CACTUS_URL = "https://cactus.nci.nih.gov/chemical/structure/";
//	private static final String CACTUS_FORMAT_SDF = "/sdf";	// alternatives e.g.: smiles,stdinchikey
//	private static final String CACTUS_FORMAT_SMILES = "/smiles";

	private static final String PROPERTY_USE_SERVER = "useServer";
	private static final String PROPERTY_IS_SMARTS = "isSmarts";
	private static final String PROPERTY_USE_DOUBLE_DOT = "useDoubleDot";

	private static final int MAX_REACTION_SMILES_CHECKS = 10;
	private static final int MAX_REACTION_SMILES_ERRORS = 5;

	private static final String[] cSourceColumnName = { "substance name", "compound name", "iupac name", "smiles", "smarts", "smirks" };

	private JCheckBox mCheckBoxIsSmarts,mCheckBoxUseServer,mCheckBoxUseDoubleDot;

	public DETaskAddChemistryFromName(DEFrame parentFrame) {
    	super(parentFrame, parentFrame.getTableModel(), true);
    	}

    @Override
	public boolean isCompatibleColumn(int column) {
		return getTableModel().isColumnTypeString(column) && getTableModel().getColumnSpecialType(column) == null;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public JPanel createInnerDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {TableLayout.PREFERRED},
							{gap>>1, TableLayout.PREFERRED, gap>>1, TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED, gap>>1, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };
		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		mCheckBoxIsSmarts = new JCheckBox("Interpret SMILES as SMARTS and reaction SMILES as SMIRKS");
		content.add(mCheckBoxIsSmarts, "0,1");

		mCheckBoxUseDoubleDot = new JCheckBox("Consider '..' (not '.') as molecule separator (reaction SMILES / SMIRKS)");
		content.add(mCheckBoxUseDoubleDot, "0,3");

		content.add(new JLabel("If DataWarrior cannot interpret a name as IUPAC-name nor as SMILES,"), "0,5");
		content.add(new JLabel("then DataWarrior may connect to openmolecules.org to resolve names."), "0,7");

		mCheckBoxUseServer = new JCheckBox("Allow openmolecules.org name-to-structure service");
		content.add(mCheckBoxUseServer, "0,9");

		return content;
		}

	@Override
	public void columnChanged(int column) {
		super.columnChanged(column);
		if (mCheckBoxUseDoubleDot != null && isInteractive())
			mCheckBoxUseDoubleDot.setEnabled(isReactionSmilesOrSmirks(column, mCheckBoxIsSmarts.isSelected(), new boolean[1]));
		}

	@Override
	public String getColumnLabelText() {
		return "Structure name or SMILES column:";
		}

	@Override
	public void selectDefaultColumn(JComboBox<String> comboBox) {
		for (String name:cSourceColumnName) {
			for (int i=0; i<comboBox.getItemCount(); i++) {
				if (name.equals((comboBox.getItemAt(i)).toLowerCase())) {
					comboBox.setSelectedIndex(i);
					return;
					}
				}
			}

		// default handling, if no obvious name column was found
		super.selectDefaultColumn(comboBox);
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		String isSmarts = (mCheckBoxIsSmarts == null) ? "false" : mCheckBoxIsSmarts.isSelected()? "true" : "false";
		configuration.setProperty(PROPERTY_IS_SMARTS, isSmarts);
		String useDoubleDot = (mCheckBoxUseDoubleDot == null) ? "false" : mCheckBoxUseDoubleDot.isSelected()? "true" : "false";
		configuration.setProperty(PROPERTY_USE_DOUBLE_DOT, useDoubleDot);
		String useServer = (mCheckBoxUseServer == null) ? "true" : mCheckBoxUseServer.isSelected()? "true" : "false";
		configuration.setProperty(PROPERTY_USE_SERVER, useServer);
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		if (mCheckBoxIsSmarts != null)
			mCheckBoxIsSmarts.setSelected("true".equals(configuration.getProperty(PROPERTY_IS_SMARTS, "false")));
		if (mCheckBoxUseDoubleDot != null)
			mCheckBoxUseDoubleDot.setSelected("true".equals(configuration.getProperty(PROPERTY_USE_DOUBLE_DOT, "false")));
		if (mCheckBoxUseServer != null)
			mCheckBoxUseServer.setSelected("true".equals(configuration.getProperty(PROPERTY_USE_SERVER, "true")));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		if (mCheckBoxIsSmarts != null)
			mCheckBoxIsSmarts.setSelected(false);
		if (mCheckBoxUseDoubleDot != null)
			mCheckBoxUseDoubleDot.setSelected(false);
		if (mCheckBoxUseServer != null)
			mCheckBoxUseServer.setSelected(true);
		}

	@Override
	public void runTask(Properties configuration) {
		int sourceColumn = getColumn(configuration);
		boolean[] catalystsFound = new boolean[1];
		boolean isSmarts = "true".equals(configuration.getProperty(PROPERTY_IS_SMARTS, "false"));
		boolean isReactionSmiles = isReactionSmilesOrSmirks(sourceColumn, isSmarts, catalystsFound);
		int smilesMode = isSmarts ? SmilesParser.SMARTS_MODE_IS_SMARTS : SmilesParser.SMARTS_MODE_GUESS;
		if (!"true".equals(configuration.getProperty(PROPERTY_USE_DOUBLE_DOT, "false")))
			smilesMode += SmilesParser.MODE_SINGLE_DOT_SEPARATOR;

		int idcodeColumn = getTableModel().addNewColumns(!isReactionSmiles ? 3 : catalystsFound[0] ? 8 : 6);
        int coordsColumn = idcodeColumn + (isReactionSmiles ? 2 : 1);
		if (isReactionSmiles)
			getTableModel().prepareReactionColumns(idcodeColumn, "Reaction", isSmarts, true, true, catalystsFound[0], true, true, true, catalystsFound[0]);
		else
			getTableModel().prepareStructureColumns(idcodeColumn, "Structure", true, true);

		NameToStructure opsinN2S = NameToStructure.getInstance();
		StereoMolecule mol = new StereoMolecule();
		Reaction rxn = null;

		boolean useServer = "true".equals(configuration.getProperty(PROPERTY_USE_SERVER, "true"))
							&& StructureNameResolver.getInstance() != null;
		SortedStringList unresolvedList = useServer ? new SortedStringList() : null;

		startProgress("Generating Structures...", 0, getTableModel().getTotalRowCount());
		for (int row=0; row<getTableModel().getTotalRowCount() && !threadMustDie(); row++) {
			updateProgress(row);

			String name = getTableModel().getTotalValueAt(row, sourceColumn).trim();
			if (!name.isEmpty()) {
				try {
					if (isReactionSmiles)
						rxn = new SmilesParser(smilesMode).parseReaction(name);
					else
						new SmilesParser(smilesMode).parse(mol, name);
					}
				catch (Exception e) {
					mol.clear();
					rxn = null;
					}

				if (isReactionSmiles) {
					if (rxn != null) {
						String[] rxnData = ReactionEncoder.encode(rxn, false, false);
						if (rxnData != null && rxnData[0] != null) {
							getTableModel().setTotalValueAt(rxnData[0], row, idcodeColumn);
							if (rxnData[1] != null && !rxnData[1].isEmpty())
								getTableModel().setTotalValueAt(rxnData[1], row, idcodeColumn+1);
							if (rxnData[2] != null && !rxnData[2].isEmpty())
								getTableModel().setTotalValueAt(rxnData[2], row, idcodeColumn+2);
							if (rxnData[4] != null && !rxnData[4].isEmpty() && catalystsFound[0])
								getTableModel().setTotalValueAt(rxnData[4], row, idcodeColumn+3);
							}
						}
					}
				else {
					if (mol.getAllAtoms() == 0) {
						String smiles = opsinN2S.parseToSmiles(name);
						if (smiles != null) {
							try {
								new SmilesParser(SmilesParser.SMARTS_MODE_IS_SMILES).parse(mol, smiles);
								}
							catch (Exception e) {
								mol.clear();
								}
							}
						}

					if (mol.getAllAtoms() != 0) {
						try {
							mol.normalizeAmbiguousBonds();
							mol.canonizeCharge(true);
							Canonizer canonizer = new Canonizer(mol);
							canonizer.setSingleUnknownAsRacemicParity();
							getTableModel().setTotalValueAt(canonizer.getIDCode(), row, idcodeColumn);
							getTableModel().setTotalValueAt(canonizer.getEncodedCoordinates(), row, coordsColumn);
							}
						catch (Exception e) {
							e.printStackTrace();
							}
						}
					else if (useServer) {
						unresolvedList.addString(name);
						}
					}
				}
			}

		if (useServer && unresolvedList.getSize() != 0) {
			startProgress("Sending "+unresolvedList.getSize()+" Names...", 0, 0);
			String[] idcodes = StructureNameResolver.getInstance().resolveRemote(unresolvedList.toArray());
			for (int row=0; row<getTableModel().getTotalRowCount() && !threadMustDie(); row++) {
				String name = getTableModel().getTotalValueAt(row, sourceColumn).trim();
				if (!name.isEmpty() && getTableModel().getTotalRecord(row).getData(idcodeColumn) == null) {
					String idcode = idcodes[unresolvedList.getListIndex(name)];
					if (idcode != null && !idcode.isEmpty()) {
						int index = idcode.indexOf(' ');
						if (index == -1) {
							getTableModel().setTotalValueAt(idcode, row, idcodeColumn);
							}
						else {
							getTableModel().setTotalValueAt(idcode.substring(0, index), row, idcodeColumn);
							getTableModel().setTotalValueAt(idcode.substring(index+1), row, coordsColumn);
							}
						}
					}
				}
			}

		getTableModel().finalizeNewColumns(idcodeColumn, this);
		}

	private boolean isReactionSmilesOrSmirks(int column, boolean isSmarts, boolean[] catalystsFound) {
		CompoundTableModel tableModel = getTableModel();
		int smilesMode = isSmarts ? SmilesParser.SMARTS_MODE_IS_SMARTS : SmilesParser.SMARTS_MODE_GUESS;
		int count = 0;
		int errorCount = 0;
		int[] catalystCountHolder = new int[1];
		for (int row=0; row<getTableModel().getTotalRowCount(); row++) {
			byte[] value = (byte[])tableModel.getTotalRecord(row).getData(column);
			if (value != null) {
				if (!SmilesParser.isReactionSmiles(value, catalystCountHolder))
					return false;
				try {
					Reaction reaction = new SmilesParser(smilesMode).parseReaction(value);
					if (catalystCountHolder[0] != 0)
						catalystsFound[0] |= (reaction.getCatalysts() != 0);
					}
				catch (Exception e) {
					errorCount++;
					if (errorCount == MAX_REACTION_SMILES_ERRORS)
						return false;
					}
				if (++count == MAX_REACTION_SMILES_CHECKS)
					return true;
				}
			}

		return count != 0;
		}
	}
