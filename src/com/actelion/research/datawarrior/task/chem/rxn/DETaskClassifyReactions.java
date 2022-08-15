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

package com.actelion.research.datawarrior.task.chem.rxn;

import com.actelion.research.chem.reaction.Classification;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.chem.reaction.ReactionClassifier;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class DETaskClassifyReactions extends ConfigurableTask {
	public static final String TASK_NAME = "Classify Reactions";

	private static final String PROPERTY_REACTION_COLUMN = "structureColumn";

	private JComboBox			mComboBoxReactionColumn;

	private volatile CompoundTableModel	mTableModel;
	private volatile int				mReactionColumn;
	private AtomicInteger				mSMPRecordIndex,mSMPWorkingThreads,mSMPErrorCount;

	public DETaskClassifyReactions(DEFrame parent) {
		super(parent, true);
		mTableModel = parent.getTableModel();
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
	public boolean isConfigurable() {
		if (getCompatibleReactionColumnList() == null) {
			showErrorMessage("Running '"+getTaskName()+"' requires the presence of chemical structures.");
			return false;
			}

		return true;
		}

	@Override
	public JPanel createDialogContent() {
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8}, {8, TableLayout.PREFERRED, 8} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		int[] reactionColumn = getCompatibleReactionColumnList();

		// create components
		mComboBoxReactionColumn = new JComboBox();
		if (reactionColumn != null)
			for (int i=0; i<reactionColumn.length; i++)
				mComboBoxReactionColumn.addItem(mTableModel.getColumnTitle(reactionColumn[i]));
		mComboBoxReactionColumn.setEditable(!isInteractive());
		content.add(new JLabel("Reaction column:"), "1,1");
		content.add(mComboBoxReactionColumn, "3,1");

		return content;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (isLive) {
			int idcodeColumn = selectStructureColumn(configuration);
			if (idcodeColumn == -1) {
				showErrorMessage("Reaction column not found.");
				return false;
				}
			}
		return true;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String value = configuration.getProperty(PROPERTY_REACTION_COLUMN, "");
		if (value.length() != 0) {
			int column = mTableModel.findColumn(value);
			if (column != -1)
				mComboBoxReactionColumn.setSelectedItem(mTableModel.getColumnTitle(column));
			else if (!isInteractive())
				mComboBoxReactionColumn.setSelectedItem(value);
			else if (mComboBoxReactionColumn.getItemCount() != 0)
				mComboBoxReactionColumn.setSelectedIndex(0);
			}
		else if (!isInteractive()) {
			mComboBoxReactionColumn.setSelectedItem("Reaction");
			}
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mComboBoxReactionColumn.getItemCount() != 0)
			mComboBoxReactionColumn.setSelectedIndex(0);
		else if (!isInteractive())
			mComboBoxReactionColumn.setSelectedItem("Reaction");
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		if (mComboBoxReactionColumn.getItemCount() != 0)
			configuration.setProperty(PROPERTY_REACTION_COLUMN, (String)mComboBoxReactionColumn.getSelectedItem());

		return configuration;
		}

	@Override
	public void runTask(Properties configuration) {
		mReactionColumn = selectStructureColumn(configuration);

		startProgress("Running '"+getTaskName()+"'...", 0, mTableModel.getTotalRowCount());

		final String[] columnName = { "Reaction class", "Reaction sub-class", "Classification Error" };
		final int firstNewColumn = mTableModel.addNewColumns(columnName);

		int threadCount = Runtime.getRuntime().availableProcessors();
		mSMPRecordIndex = new AtomicInteger(mTableModel.getTotalRowCount());
		mSMPWorkingThreads = new AtomicInteger(threadCount);
		mSMPErrorCount = new AtomicInteger(0);

		Thread[] t = new Thread[threadCount];
		for (int i=0; i<threadCount; i++) {
			t[i] = new Thread("ReactionClassifier "+(i+1)) {
				public void run() {
					ReactionClassifier classifier = new ReactionClassifier();
					int recordIndex = mSMPRecordIndex.decrementAndGet();
					while (recordIndex >= 0 && !threadMustDie()) {
						try {
							Reaction reaction = mTableModel.getChemicalReaction(mTableModel.getTotalRecord(recordIndex), mReactionColumn, CompoundTableModel.ATOM_COLOR_MODE_NONE);
							int error = classifier.classify(reaction);
							if (error == ReactionClassifier.cErrorNoError) {
								Classification result = classifier.getClassificationResult();
								mTableModel.setTotalValueAt(result.mClassName, recordIndex, firstNewColumn);
								String subclass = result.mUnitName[0];
								for (int i=1; i<result.mUnitRxns; i++)
									subclass += "; "+result.mUnitName[i];
								mTableModel.setTotalValueAt(subclass, recordIndex, firstNewColumn+1);
								}
							else {
								mTableModel.setTotalValueAt("Error "+error, recordIndex, firstNewColumn+2);
								}
							}
						catch (Exception e) {
							e.printStackTrace();
							mSMPErrorCount.incrementAndGet();
							}

						updateProgress(-1);
						recordIndex = mSMPRecordIndex.decrementAndGet();
						}

					if (mSMPWorkingThreads.decrementAndGet() == 0) {
						if (!threadMustDie() && mSMPErrorCount.get() != 0)
							showErrorCount(mSMPErrorCount.get());

   						mTableModel.finalizeNewColumns(firstNewColumn, DETaskClassifyReactions.this);
						}
					}
				};
			t[i].setPriority(Thread.MIN_PRIORITY);
			t[i].start();
			}

		// the controller thread must wait until all others are finished
		// before the next task can begin or the dialog is closed
		for (int i=0; i<threadCount; i++)
			try { t[i].join(); } catch (InterruptedException e) {}
		}

	private void showErrorCount(int errorCount) {
		showErrorMessage("The task '"+TASK_NAME+"' failed on "+errorCount+" reactions.");
		}

	private int selectStructureColumn(Properties configuration) {
		int[] idcodeColumn = getCompatibleReactionColumnList();
		if (idcodeColumn.length == 1)
			return idcodeColumn[0];	// there is no choice
		int column = mTableModel.findColumn(configuration.getProperty(PROPERTY_REACTION_COLUMN));
		for (int i=0; i<idcodeColumn.length; i++)
			if (column == idcodeColumn[i])
				return column;
		return -1;
		}

	private int[] getCompatibleReactionColumnList() {
		return mTableModel.getSpecialColumnList(CompoundTableModel.cColumnTypeRXNCode);
		}
	}
