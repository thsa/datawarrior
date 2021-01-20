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

import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorHelper;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.util.Properties;

import static com.actelion.research.chem.descriptor.DescriptorConstants.DESCRIPTOR_ShapeAlign;
import static com.actelion.research.chem.descriptor.DescriptorConstants.DESCRIPTOR_TYPE_MOLECULE;

public class DETaskCalculateDescriptor extends ConfigurableTask implements CompoundTableConstants {
	public static final String TASK_NAME = "Calculate Descriptor";

	private static final String PROPERTY_COLUMN = "column";
	private static final String PROPERTY_DESCRIPTOR = "descriptor";
	private static final String PROPERTY_REACTION_PART = "reactionPart";

	private static final String [] CHEM_TYPE_CODE = { cReactionPartReaction, cReactionPartReactants, cReactionPartProducts };
	private static final String [] CHEM_TYPE_TEXT = { "Structure / Entire reaction", "Starting materials", "Reaction products" };

	private JComboBox	mComboBoxDescriptor,mComboBoxColumn,mComboBoxChemType;
	private String		mDescriptor;
	private String		mReactionPart;
	private CompoundTableModel	mTableModel;

	/**
	 *
	 * @param parent
	 * @param descriptor a molecule or reaction descriptor short name
	 * @param reactionPart null or one of the CompoundTableConstants.cReactionPartXXX options (if molecule descriptor referring to reaction)
	 */
	public DETaskCalculateDescriptor(DEFrame parent, String descriptor, String reactionPart) {
		super(parent, descriptor == null);
		mTableModel = parent.getTableModel();
		mDescriptor = descriptor;
		mReactionPart = reactionPart;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		if (mDescriptor == null)
			return null;
		int type = DescriptorHelper.getDescriptorType(mDescriptor);
		int[] column = (type == DescriptorConstants.DESCRIPTOR_TYPE_MOLECULE && mReactionPart == null) ?
				mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeIDCode)
					 : (type == DescriptorConstants.DESCRIPTOR_TYPE_REACTION || mReactionPart != null) ?
				mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeRXNCode)
					 : null;
		if (column != null && column.length == 1) {
			Properties configuration = new Properties();
			configuration.setProperty(PROPERTY_COLUMN, mTableModel.getColumnTitleNoAlias(column[0]));
			configuration.setProperty(PROPERTY_DESCRIPTOR, mDescriptor);
			if (mReactionPart != null)
				configuration.setProperty(PROPERTY_REACTION_PART, mReactionPart);
			return configuration;
			}

		return null;
		}

	@Override
	public JPanel createDialogContent() {
		JPanel p = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8} };

		p.setLayout(new TableLayout(size));
		p.add(new JLabel("Descriptor:"), "1,1");
		mComboBoxDescriptor = new JComboBox();
		for (int i=0; i<DescriptorConstants.DESCRIPTOR_LIST.length; i++)
			mComboBoxDescriptor.addItem(DescriptorConstants.DESCRIPTOR_LIST[i].shortName);
		if (System.getProperty("development") != null)
			mComboBoxDescriptor.addItem(DESCRIPTOR_ShapeAlign.shortName);
		p.add(mComboBoxDescriptor, "3,1");

		if (mDescriptor != null) {
			mComboBoxDescriptor.setSelectedItem(mDescriptor);	
			mComboBoxDescriptor.setEnabled(false);
			}

		p.add(new JLabel("Chemistry column:"), "1,3");
		mComboBoxColumn = new JComboBox();
		int[] column = mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeIDCode);
		if (column != null)
			for (int i=0; i<column.length; i++)
				mComboBoxColumn.addItem(mTableModel.getColumnTitleNoAlias(column[i]));
		column = mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeRXNCode);
		if (column != null)
			for (int i=0; i<column.length; i++)
				mComboBoxColumn.addItem(mTableModel.getColumnTitleNoAlias(column[i]));
		mComboBoxColumn.setEditable(mDescriptor == null);
		p.add(mComboBoxColumn, "3,3");

		p.add(new JLabel("Reaction part:"), "1,5");
		mComboBoxChemType = new JComboBox(CHEM_TYPE_TEXT);
		p.add(mComboBoxChemType, "3,5");

		if (mDescriptor != null) {
			if (mReactionPart == null) {
				mComboBoxChemType.setSelectedIndex(0);
				}
			else {
				int index = findListIndex(mReactionPart, CHEM_TYPE_CODE, 0);
				mComboBoxChemType.setSelectedIndex(index);
				}
			mComboBoxChemType.setEnabled(false);
			}

		return p;
		}

	@Override
	public boolean isConfigurable() {
		return mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeIDCode) != null
			|| mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeRXNCode) != null;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_DESCRIPTOR, (String)mComboBoxDescriptor.getSelectedItem());
		String item = (String)mComboBoxColumn.getSelectedItem();
		if (item != null) {
			configuration.setProperty(PROPERTY_COLUMN, item);
			configuration.setProperty(PROPERTY_REACTION_PART, CHEM_TYPE_CODE[mComboBoxChemType.getSelectedIndex()]);
			}
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		if (mDescriptor == null)
			mComboBoxDescriptor.setSelectedItem(configuration.getProperty(PROPERTY_DESCRIPTOR));
		mComboBoxColumn.setSelectedItem(configuration.getProperty(PROPERTY_COLUMN, ""));
		mComboBoxChemType.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_REACTION_PART), CHEM_TYPE_CODE, 0));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mDescriptor == null)
			mComboBoxDescriptor.setSelectedItem(DescriptorConstants.DESCRIPTOR_SkeletonSpheres.shortName);
		mComboBoxColumn.setSelectedItem("Structure");
		mComboBoxChemType.setSelectedIndex(0);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (isLive) {
			String descriptor = configuration.getProperty(PROPERTY_DESCRIPTOR);
			String columnName = configuration.getProperty(PROPERTY_COLUMN);
			int type = DescriptorHelper.getDescriptorType(descriptor);
			int column = mTableModel.findColumn(columnName);

			String reactionPart = (CompoundTableConstants.cColumnTypeRXNCode.equals(mTableModel.getColumnSpecialType(column))
					&& DescriptorHelper.getDescriptorType(descriptor) == DescriptorConstants.DESCRIPTOR_TYPE_MOLECULE) ?
					configuration.getProperty(PROPERTY_REACTION_PART, cReactionPartProducts) : null;

			if (column == -1) {
				int[] columnList = (type == DescriptorConstants.DESCRIPTOR_TYPE_MOLECULE && reactionPart == null) ?
						mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeIDCode)
							 : (type == DescriptorConstants.DESCRIPTOR_TYPE_REACTION || reactionPart != null) ?
						mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeRXNCode)
							 : null;
				if (columnList != null)
					column = columnList[0];
				}
			if (column == -1) {
				showErrorMessage("Column '"+columnName+"' nor any alternative found.");
				return false;
				}
			if (type == DescriptorConstants.DESCRIPTOR_TYPE_MOLECULE) {
				if (!mTableModel.isColumnTypeStructure(column)
				 && !mTableModel.isColumnTypeReaction(column)) {
					showErrorMessage("Column '" + columnName + "' doesn't contain molecules or reactions.");
					return false;
					}
				}
			if (type == DescriptorConstants.DESCRIPTOR_TYPE_REACTION) {
				if (!mTableModel.isColumnTypeReaction(column)) {
					showErrorMessage("Column '"+columnName+"' doesn't contain reactions.");
					return false;
					}
				}

			if (mTableModel.getChildColumn(column, descriptor, reactionPart) != -1) {
				showErrorMessage("Column '"+columnName+"' has already the descriptor '"+descriptor+"'.");
				return false;
				}
			}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		final String descriptor = configuration.getProperty(PROPERTY_DESCRIPTOR);
		String columnName = configuration.getProperty(PROPERTY_COLUMN);
		int column = mTableModel.findColumn(columnName);

		// Only use the discriminating reactionPart, if we have a molecule descriptor on a reaction column!
		String reactionPart = (CompoundTableConstants.cColumnTypeRXNCode.equals(mTableModel.getColumnSpecialType(column))
				&& DescriptorHelper.getDescriptorType(descriptor) == DescriptorConstants.DESCRIPTOR_TYPE_MOLECULE) ?
				configuration.getProperty(PROPERTY_REACTION_PART, cReactionPartProducts) : null;

		if (column == -1) {
			int type = DescriptorHelper.getDescriptorType(descriptor);
			int[] columnList = (type == DescriptorConstants.DESCRIPTOR_TYPE_MOLECULE && reactionPart == null) ?
					mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeIDCode)
					: (type == DescriptorConstants.DESCRIPTOR_TYPE_REACTION || reactionPart != null) ?
					mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeRXNCode)
					: null;
			if (columnList != null)
				column = columnList[0];
			}
		if (column != -1) {
			if (mTableModel.getChildColumn(column, descriptor, reactionPart) == -1) {
				if (SwingUtilities.isEventDispatchThread()) {	// is interactive
					mTableModel.addDescriptorColumn(column, descriptor, reactionPart);
					}
				else {
					try {
						final int _column = column;
						SwingUtilities.invokeAndWait(() -> mTableModel.addDescriptorColumn(_column, descriptor, reactionPart));
						}
					catch (Exception e) {}

					waitForDescriptor(mTableModel, mTableModel.getTotalColumnCount()-1);
					if (threadMustDie())
						return;
					}
				}
			}
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
