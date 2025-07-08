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
import com.actelion.research.chem.ExtendedDepictor;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.chem.reaction.ReactionEncoder;
import com.actelion.research.chem.reaction.Reactor;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DETable;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.JEditableChemistryView;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class DETaskPerformReaction extends ConfigurableTask {
	public static final String TASK_NAME = "Perform Reaction";

	private static final String PROPERTY_COLUMN_LIST = "columnList";
	private static final String PROPERTY_TRANSFORMATION = "transformation";

	private final CompoundTableModel mTableModel;
	private final DETable mTable;
	private JList<String> mListColumns;
	private JTextArea mTextArea;
	private JEditableChemistryView mTransformationView;

	public DETaskPerformReaction(DEFrame owner) {
		super(owner, true);
		mTableModel = owner.getTableModel();
		mTable = owner.getMainFrame().getMainPane().getTable();
	}

	@Override
	public JPanel createDialogContent() {
		JPanel content = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap, HiDPIHelper.scale(96), gap } };
		content.setLayout(new TableLayout(size));

		content.add(new JLabel("Select reactant column(s):"), "1,1");

		int[] idcodeColumn = mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeIDCode);
		String[] itemList = new String[idcodeColumn.length];
		for (int i=0; i<itemList.length; i++)
			itemList[i] = mTableModel.getColumnTitle(idcodeColumn[i]);
		JScrollPane scrollPane = null;
		if (isInteractive()) {
			mListColumns = new JList<>(itemList);
			scrollPane = new JScrollPane(mListColumns);
		}
		else {
			mTextArea = new JTextArea();
			scrollPane = new JScrollPane(mTextArea);
		}
		scrollPane.setPreferredSize(new Dimension(HiDPIHelper.scale(360),HiDPIHelper.scale(80)));
		content.add(scrollPane, "1,3");

		mTransformationView = new JEditableChemistryView(ExtendedDepictor.TYPE_REACTION);
		mTransformationView.getReaction().setFragment(true);
		content.add(mTransformationView, "1,5");

		return content;
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public String getHelpURL() {
		return "/html/help/chemistry.html#ApplyReaction";
	}

	@Override
	public boolean isConfigurable() {
		return mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeIDCode) != null;
	}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String columnList = configuration.getProperty(PROPERTY_COLUMN_LIST);
		if (columnList == null) {
			showErrorMessage("No reactant columns defined.");
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
				if (!mTableModel.isColumnTypeStructure(column[i])) {
					showErrorMessage("Column '"+columnName[i]+"' doesn't contains chemical structures.");
					return false;
				}
			}

			String transformation = configuration.getProperty(PROPERTY_TRANSFORMATION, "");
			if (transformation.isEmpty()) {
				showErrorMessage("No tranformation reaction defined.");
				return false;
			}
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

		return true;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties p = new Properties();

		String columnNames = isInteractive() ?
				getSelectedColumnsFromList(mListColumns, mTableModel)
				: mTextArea.getText().replace('\n', '\t');
		if (columnNames != null && !columnNames.isEmpty())
			p.setProperty(PROPERTY_COLUMN_LIST, columnNames);

		Reaction transformation = mTransformationView.getReaction();
		if (!transformation.isEmpty())
			p.setProperty(PROPERTY_TRANSFORMATION, ReactionEncoder.encode(transformation, false,
					ReactionEncoder.INCLUDE_DEFAULT | ReactionEncoder.RETAIN_REACTANT_AND_PRODUCT_ORDER));

		return p;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String columnNames = configuration.getProperty(PROPERTY_COLUMN_LIST, "");
		if (isInteractive())
			selectColumnsInList(mListColumns, columnNames, mTableModel);
		else
			mTextArea.setText(columnNames.replace('\t', '\n'));

		String transformation = configuration.getProperty(PROPERTY_TRANSFORMATION, "");
		if (!transformation.isEmpty())
			mTransformationView.setContent(ReactionEncoder.decode(transformation, true));
	}

	@Override
	public void setDialogConfigurationToDefault() {
		if (isInteractive())
			mListColumns.clearSelection();
		else
			mTextArea.setText("");
	}

	@Override
	public void runTask(Properties configuration) {
		String[] columnName = configuration.getProperty(PROPERTY_COLUMN_LIST).split("\\t");
		String transformation = configuration.getProperty(PROPERTY_TRANSFORMATION, "");
		Reaction rxn = ReactionEncoder.decode(transformation, true);

		int[] column = new int[columnName.length];
		for (int i=0; i<columnName.length; i++)
			column[i] = mTableModel.findColumn(columnName[i]);

		sortByVisibleOrder(column);

		String[] title = new String[2 * rxn.getProducts()];
		for (int i=0; i<rxn.getProducts(); i++) {
			title[2 * i] = (rxn.getProducts() == 1) ? "Product" : "Product " + (i + 1);
			title[2 * i + 1] = CompoundTableConstants.cColumnType2DCoordinates + "_p" + (i + 1);
		}
		int firstNewColumn = mTableModel.addNewColumns(title);

		startProgress("Performing Reaction...", 0, mTableModel.getTotalRowCount());

		int threadCount = Runtime.getRuntime().availableProcessors();
		AtomicInteger mSMPRecordIndex = new AtomicInteger(mTableModel.getTotalRowCount());
		AtomicInteger mSMPWorkingThreads = new AtomicInteger(threadCount);

		Thread[] t = new Thread[threadCount];
		for (int i=0; i<threadCount; i++) {
			t[i] = new Thread("Reaction Performer "+(i+1)) {
				public void run() {
					Reactor reactor = new Reactor(ReactionEncoder.decode(transformation, true));
					int recordIndex = mSMPRecordIndex.decrementAndGet();
					while (recordIndex >= 0 && !threadMustDie()) {
						try {
							processRow(reactor, firstNewColumn, recordIndex, column);
						}
						catch (Exception e) {
							System.out.println("Exception in row "+recordIndex);
							e.printStackTrace();
						}

						updateProgress(-1);
						recordIndex = mSMPRecordIndex.decrementAndGet();
					}

					if (mSMPWorkingThreads.decrementAndGet() == 0) {
						for (int i=0; i<rxn.getProducts(); i++) {
							mTableModel.setColumnProperty(firstNewColumn+2*i, CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnTypeIDCode);
							mTableModel.setColumnProperty(firstNewColumn+2*i+1, CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnType2DCoordinates);
							mTableModel.setColumnProperty(firstNewColumn+2*i+1, CompoundTableConstants.cColumnPropertyParentColumn, mTableModel.getColumnTitleNoAlias(firstNewColumn+2*i));
						}

						mTableModel.finalizeNewColumns(firstNewColumn, DETaskPerformReaction.this);
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

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
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

	private void processRow(Reactor reactor, int firstNewColumn, int row , int[] sourceColumn) {
		StereoMolecule[] mol = new StereoMolecule[sourceColumn.length];
		CompoundRecord record = mTableModel.getTotalRecord(row);
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
			for (int i=0; i<products[0].length; i++) {
				Canonizer canonizer = new Canonizer(products[0][i]);
				record.setData(canonizer.getIDCode().getBytes(), firstNewColumn+2*i);
				record.setData(canonizer.getEncodedCoordinates().getBytes(), firstNewColumn+2*i+1);
			}
		}
	}
}
