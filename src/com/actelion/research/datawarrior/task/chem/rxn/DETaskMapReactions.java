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

package com.actelion.research.datawarrior.task.chem.rxn;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.chem.reaction.ReactionEncoder;
import com.actelion.research.chem.reaction.mapping.ChemicalRuleEnhancedReactionMapper;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.chem.DETaskAbstractFromReaction;
import com.actelion.research.util.ByteArrayComparator;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.util.Properties;

public class DETaskMapReactions extends DETaskAbstractFromReaction {
	public static final String TASK_NAME = "Map Reactions";

	private static final boolean CREATE_SCORE_AND_RULE_COLUMNS = true;

	private static final String PROPERTY_NEW_COLUMN = "newColumn";

	private JCheckBox mCheckBoxCreateNewReaction;
	private int mMappingColumn;
	private boolean mCreateNewColumn;

	public DETaskMapReactions(DEFrame parent) {
		super(parent, DESCRIPTOR_NONE, false, true);
	}

	@Override
	protected int getNewColumnCount() {
		return mCreateNewColumn ? (CREATE_SCORE_AND_RULE_COLUMNS ? 6 : 3)
				: getTableModel().getChildColumn(getChemistryColumn(),
				CompoundTableConstants.cColumnTypeReactionMapping) == -1 ? 1 : 0;
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
//		int gap = HiDPIHelper.scale(8);
		double[][] size = { {TableLayout.PREFERRED},
							{TableLayout.PREFERRED} };

		mCheckBoxCreateNewReaction = new JCheckBox("Create new reaction column");

		JPanel ep = new JPanel();
		ep.setLayout(new TableLayout(size));
		ep.add(mCheckBoxCreateNewReaction, "0,0");
		return ep;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_NEW_COLUMN, mCheckBoxCreateNewReaction.isSelected() ? "true" : "false");
		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mCheckBoxCreateNewReaction.setSelected("true".equals(configuration.getProperty(PROPERTY_NEW_COLUMN)));
	}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mCheckBoxCreateNewReaction.setSelected(true);
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	protected void setNewColumnProperties(int firstNewColumn) {
		String sourceColumnName = getTableModel().getColumnTitle(getChemistryColumn());
		if (mCreateNewColumn) {
			getTableModel().prepareReactionColumns(firstNewColumn, "Mapped " + sourceColumnName, false,
					true, false, false, true, false, false, false);
			if (CREATE_SCORE_AND_RULE_COLUMNS) {
				getTableModel().setColumnName("Mapping Score", firstNewColumn+3);
				getTableModel().setColumnName("Chemical Rule", firstNewColumn+4);
				getTableModel().setColumnName("Rule History", firstNewColumn+5);
				}
			}
		else  {
			getTableModel().setColumnName(CompoundTableConstants.cColumnTypeReactionMapping, firstNewColumn);
			getTableModel().setColumnProperty(firstNewColumn, CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnTypeReactionMapping);
			getTableModel().setColumnProperty(firstNewColumn, CompoundTableConstants.cColumnPropertyParentColumn, sourceColumnName);
		}
	}

	@Override
	protected boolean preprocessRows(Properties configuration) {
		mCreateNewColumn = "true".equals(configuration.getProperty(PROPERTY_NEW_COLUMN));
		mMappingColumn = getTableModel().getChildColumn(getChemistryColumn(), CompoundTableConstants.cColumnTypeReactionMapping);
		return super.preprocessRows(configuration);
	}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol) {
		Reaction rxn = getChemicalReaction(row);
		if (rxn == null)
			return;

		rxn.removeAtomMapping(false);
		try {
			ChemicalRuleEnhancedReactionMapper mapper = new ChemicalRuleEnhancedReactionMapper();
			mapper.map(rxn);

			String[] encoding = ReactionEncoder.encode(rxn, false);
			if (encoding != null) {
				if (mCreateNewColumn) {
					getTableModel().setTotalValueAt(encoding[0], row, firstNewColumn);
					getTableModel().setTotalValueAt(encoding[1], row, firstNewColumn + 1);
					if (CREATE_SCORE_AND_RULE_COLUMNS) {
						String ruleName = mapper.getAppliedRule() == null ? "" : mapper.getAppliedRule().getName();
						getTableModel().setTotalValueAt(Float.toString(mapper.getScore()), row, firstNewColumn + 3);
						getTableModel().setTotalValueAt(ruleName, row, firstNewColumn + 4);
						getTableModel().setTotalValueAt(mapper.getHistory(), row, firstNewColumn + 5);
						}
					}
				else {
					byte[] rxnCode = (byte[])getTableModel().getTotalRecord(row).getData(getChemistryColumn());
					if (new ByteArrayComparator().compare(encoding[0].getBytes(), rxnCode) == 0) {
						getTableModel().setTotalValueAt(encoding[1], row, mMappingColumn == -1 ? firstNewColumn : mMappingColumn);
					}
					else {
						System.out.println("rxnCode mismatch:");
						System.out.println("old:"+new String(rxnCode));
						System.out.println("new:"+encoding[0]);
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void postprocess(int firstNewColumn) {
		if (!mCreateNewColumn)
			getTableModel().setHiliteMode(getTableModel().getParentColumn(mMappingColumn), CompoundTableConstants.cReactionHiliteModeReactionCenter);
		}
	}
