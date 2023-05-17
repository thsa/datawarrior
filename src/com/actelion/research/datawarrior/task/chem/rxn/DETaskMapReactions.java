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
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.util.ByteArrayComparator;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public class DETaskMapReactions extends DETaskAbstractFromReaction {
	public static final String TASK_NAME = "Map Reaction Atoms";

	private static final String PROPERTY_NEW_COLUMN = "newColumn";
	private static final String PROPERTY_RULE_AND_SCORE = "ruleAndScore";

	private JCheckBox mCheckBoxCreateNewReaction, mCheckBoxAddRuleAndScore;
	private int mMappingColumn;
	private boolean mCreateReactionColumns,mAddRuleAndScore,mAddMappingColumn;

	public DETaskMapReactions(DEFrame parent) {
		super(parent, DESCRIPTOR_NONE, false, true);
	}

	@Override
	protected int getNewColumnCount() {
		return (mAddRuleAndScore ? 3 : 0)
			 + (mCreateReactionColumns ? 4 : mAddMappingColumn ? 1 : 0);
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
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {TableLayout.PREFERRED},
							{TableLayout.PREFERRED, gap, TableLayout.PREFERRED} };

		mCheckBoxCreateNewReaction = new JCheckBox("Create new reaction column");
		mCheckBoxAddRuleAndScore = new JCheckBox("Add columns with rule and score");

		JPanel ep = new JPanel();
		ep.setLayout(new TableLayout(size));
		ep.add(mCheckBoxCreateNewReaction, "0,0");
		ep.add(mCheckBoxAddRuleAndScore, "0,2");
		return ep;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_NEW_COLUMN, mCheckBoxCreateNewReaction.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_RULE_AND_SCORE, mCheckBoxAddRuleAndScore.isSelected() ? "true" : "false");
		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mCheckBoxCreateNewReaction.setSelected("true".equals(configuration.getProperty(PROPERTY_NEW_COLUMN)));
		mCheckBoxAddRuleAndScore.setSelected("true".equals(configuration.getProperty(PROPERTY_RULE_AND_SCORE)));
	}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mCheckBoxCreateNewReaction.setSelected(false);
		mCheckBoxAddRuleAndScore.setSelected(false);
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	protected void setNewColumnProperties(int firstNewColumn) {
		String sourceColumnName = getTableModel().getColumnTitle(getChemistryColumn());
		int firstRuleColumn = firstNewColumn;
		if (mCreateReactionColumns) {
			getTableModel().prepareReactionColumns(firstNewColumn, "Mapped " + sourceColumnName, false,
					true, true, false, true, false, false, false);
			firstRuleColumn += 4;
		}
		else if (mAddMappingColumn) {
			getTableModel().setColumnName(CompoundTableConstants.cColumnTypeReactionMapping, firstNewColumn);
			getTableModel().setColumnProperty(firstNewColumn, CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnTypeReactionMapping);
			getTableModel().setColumnProperty(firstNewColumn, CompoundTableConstants.cColumnPropertyParentColumn, sourceColumnName);
			firstRuleColumn++;
		}

		if (mAddRuleAndScore) {
			getTableModel().setColumnName("Mapping Score", firstRuleColumn++);
			getTableModel().setColumnName("Chemical Rule", firstRuleColumn++);
			getTableModel().setColumnName("Rule History", firstRuleColumn++);
		}
	}

	@Override
	protected boolean preprocessRows(Properties configuration) {
		mCreateReactionColumns = "true".equals(configuration.getProperty(PROPERTY_NEW_COLUMN));
		mAddRuleAndScore = "true".equals(configuration.getProperty(PROPERTY_RULE_AND_SCORE));
		mMappingColumn = getTableModel().getChildColumn(getChemistryColumn(), CompoundTableConstants.cColumnTypeReactionMapping);
		mAddMappingColumn = !mCreateReactionColumns && mMappingColumn == -1;
		return super.preprocessRows(configuration);
	}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol, int threadIndex) {
		Reaction rxn = getChemicalReaction(row);
		if (rxn == null)
			return;

		rxn.removeAtomMapping(false);
		try {
AtomicBoolean b = new AtomicBoolean();
new Thread(() -> {
 try { Thread.sleep(5000); } catch (InterruptedException ie) {}
 if (!b.get())
  System.out.println("sleepy row:"+row);
}).start();
			ChemicalRuleEnhancedReactionMapper mapper = new ChemicalRuleEnhancedReactionMapper();
			mapper.map(rxn);
b.set(true);

			String[] encoding = ReactionEncoder.encode(rxn, false);
			if (encoding != null) {
				int firstRuleColumn = firstNewColumn;
				if (mCreateReactionColumns) {
					getTableModel().setTotalValueAt(encoding[0], row, firstNewColumn);
					getTableModel().setTotalValueAt(encoding[1], row, firstNewColumn + 1);
					getTableModel().setTotalValueAt(encoding[2], row, firstNewColumn + 2);
					firstRuleColumn += 4;
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

				if (mAddRuleAndScore) {
					String ruleName = mapper.getAppliedRule() == null ? "" : mapper.getAppliedRule().getName();
					getTableModel().setTotalValueAt(Float.toString(mapper.getScore()), row, firstRuleColumn++);
					getTableModel().setTotalValueAt(ruleName, row, firstRuleColumn++);
					getTableModel().setTotalValueAt(mapper.getHistory(), row, firstRuleColumn++);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void postprocess(int firstNewColumn) {
		if (!mCreateReactionColumns) {
			int reactionColumn = getTableModel().getParentColumn(mAddMappingColumn ? firstNewColumn : mMappingColumn);
			getTableModel().setHiliteMode(reactionColumn, CompoundTableConstants.cReactionHiliteModeReactionCenter);
			}
		}
	}
