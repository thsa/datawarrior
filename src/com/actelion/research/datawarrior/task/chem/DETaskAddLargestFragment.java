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
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.MoleculeNeutralizer;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.util.Properties;

public class DETaskAddLargestFragment extends DETaskAbstractFromStructure {
	public static final String TASK_NAME = "Add Largest Fragment";

	private static final String PROPERTY_NEUTRALIZE = "neutralize";
	private static final String PROPERTY_AS_SUBSTRUCTURE = "asSubstructure";

	private JCheckBox mCheckBoxNeutralize,mCheckBoxAsSubstructure;
	private boolean mNeutralizeFragment,mAsSubstructure;

    public DETaskAddLargestFragment(DEFrame parent) {
		super(parent, DESCRIPTOR_NONE, false, true);
		}

	@Override
	protected int getNewColumnCount() {
		int count = 1;
		for (int column=0; column<getTableModel().getTotalColumnCount(); column++)
			if (getTableModel().getParentColumn(column) == getChemistryColumn() && isCoordinateColumn(column))
			  count++;
		return count;
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
		double[][] size = { {TableLayout.PREFERRED}, {TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED} };

		mCheckBoxNeutralize = new JCheckBox("Neutralize charges");
		mCheckBoxAsSubstructure = new JCheckBox("Convert to sub-structure");

		JPanel ep = new JPanel();
		ep.setLayout(new TableLayout(size));
		ep.add(mCheckBoxNeutralize, "0,0");
		ep.add(mCheckBoxAsSubstructure, "0,2");
		return ep;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/chemistry.html#AddLargestFragment";
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_NEUTRALIZE, mCheckBoxNeutralize.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_AS_SUBSTRUCTURE, mCheckBoxAsSubstructure.isSelected() ? "true" : "false");
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mCheckBoxNeutralize.setSelected("true".equals(configuration.getProperty(PROPERTY_NEUTRALIZE)));
		mCheckBoxAsSubstructure.setSelected("true".equals(configuration.getProperty(PROPERTY_AS_SUBSTRUCTURE)));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mCheckBoxNeutralize.setSelected(true);
		mCheckBoxAsSubstructure.setSelected(false);
		}

	@Override
	protected void setNewColumnProperties(int firstNewColumn) {
		String sourceColumnName = getTableModel().getColumnTitle(getChemistryColumn());

		getTableModel().setColumnName("Largest Fragment of " + sourceColumnName, firstNewColumn);
		getTableModel().setColumnProperty(firstNewColumn, CompoundTableConstants.cColumnPropertySpecialType,
				CompoundTableConstants.cColumnTypeIDCode);
		if (mAsSubstructure)
			getTableModel().setColumnProperty(firstNewColumn, CompoundTableConstants.cColumnPropertyIsFragment, "true");

		int count = 1;
		for (int column=0; column<getTableModel().getTotalColumnCount(); column++) {
			if (getTableModel().getParentColumn(column) == getChemistryColumn() && isCoordinateColumn(column)) {
				getTableModel().setColumnName("fragmentCoordinates"+count, firstNewColumn+count);
				getTableModel().setColumnProperty(firstNewColumn+count,
						CompoundTableConstants.cColumnPropertySpecialType, getTableModel().getColumnSpecialType(column));
				getTableModel().setColumnProperty(firstNewColumn+count,
						CompoundTableConstants.cColumnPropertyParentColumn, getTableModel().getColumnTitleNoAlias(firstNewColumn));
				count++;
				}
			}
		}

	private boolean isCoordinateColumn(int column) {
		return CompoundTableConstants.cColumnType2DCoordinates.equals(getTableModel().getColumnSpecialType(column))
			|| CompoundTableConstants.cColumnType3DCoordinates.equals(getTableModel().getColumnSpecialType(column));
		}

	@Override
	protected boolean preprocessRows(Properties configuration) {
		mNeutralizeFragment = "true".equals(configuration.getProperty(PROPERTY_NEUTRALIZE));
		mAsSubstructure = "true".equals(configuration.getProperty(PROPERTY_AS_SUBSTRUCTURE));
		return super.preprocessRows(configuration);
    	}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol, int threadIndex) {
		CompoundRecord record = getTableModel().getTotalRecord(row);
		byte[] idcode = (byte[])record.getData(getChemistryColumn());
		if (idcode != null) {
			int count = 0;
			for (int column=0; column<getTableModel().getTotalColumnCount(); column++) {
				if (getTableModel().getParentColumn(column) == getChemistryColumn()) {
					if (record.getData(column) != null && isCoordinateColumn(column)) {
						count++;
						boolean is2D = CompoundTableConstants.cColumnType2DCoordinates.equals(getTableModel().getColumnSpecialType(column));
						StereoMolecule mol = new IDCodeParser(is2D).getCompactMolecule(idcode, (byte[])record.getData(column));
						handleMolecule(mol);
						Canonizer canonizer = new Canonizer(mol);
						getTableModel().setTotalValueAt(canonizer.getIDCode(), row, firstNewColumn);
						getTableModel().setTotalValueAt(canonizer.getEncodedCoordinates(), row, firstNewColumn+count);
						}
					}
				}
			if (count == 0) {
				StereoMolecule mol = getChemicalStructure(row, containerMol);
				if (mol != null) {
					handleMolecule(mol);
					Canonizer canonizer = new Canonizer(mol);
					getTableModel().setTotalValueAt(canonizer.getIDCode(), row, firstNewColumn);
					}
				}
			}
		}

	private void handleMolecule(StereoMolecule mol) {
		mol.stripSmallFragments();
		if (mNeutralizeFragment)
			MoleculeNeutralizer.neutralizeChargedMolecule(mol);
		if (mAsSubstructure)
			mol.setFragment(true);
		}
	}
