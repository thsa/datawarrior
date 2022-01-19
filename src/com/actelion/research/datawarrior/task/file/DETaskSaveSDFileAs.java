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

package com.actelion.research.datawarrior.task.file;

import com.actelion.research.chem.IDCodeParserWithoutCoordinateInvention;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.CompoundFileHelper;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.table.CompoundTableSaver;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

public class DETaskSaveSDFileAs extends DETaskAbstractSaveFile {
    public static final String TASK_NAME = "Save SD-File";

	private static final String PROPERTY_SD_VERSION = "version";
	private static final String PROPERTY_STRUCTURE_COLUMN = "structureColumn";
	private static final String PROPERTY_NAME_COLUMN = "idColumn";
	private static final String PROPERTY_COORDINATE_MODE = "coordinates";   // may also contain a 3D-coords column name
	private static final String PROPERTY_INCLUDE_REFERENCE_COMPOUND = "includeRefCompound";
	private static final String[] SD_VERSION_OPTIONS = { "Version 2", "Version 3" };
	private static final String[] SD_VERSION_CODE = { "v2", "v3" };
	private static final String[] COMPOUND_NAME_OPTIONS = { "<Use row number>", "<Automatic>" };
	private static final String[] COMPOUND_NAME_CODE = { "<rowNo>", "<idColumn>" };
	private static final String[] COORDINATE_OPTIONS = { "2D", "3D (1st of multiple)" };
	private static final String[] COORDINATE_CODE = { "2D", "prefer3D" };
	private static final int INDEX_PREFER_2D = 0;
	private static final int INDEX_PREFER_3D = 1;
	private static final int INDEX_VERSION_3 = 1;
	private static final String OPTION_NO_STRUCTURE = "<none>";
	private static final int COMPOUND_NAME_OPTION_ROW_NUMBER = 0;
	private static final int COMPOUND_NAME_OPTION_COLUMN_PROPERTY = 1;

	private Properties mPredefinedConfiguration;
	private JComboBox mComboBoxVersion,mComboBoxStructureColumn,mComboBoxCompoundName,mComboBoxCoordinateMode;
	private JCheckBox mCheckBoxIncludeRefMol;

	/**
	 * The logic of this task is different from its parent class DETaskAbstractSaveFile:<br>
	 * - If invoked interactively:<br>
	 * super.getPredefinedConfiguration() is called to show a file selection dialog and to
	 * create a configuration from it. <b>null</b> is returned to also show a configuration dialog.
	 * However, createDialogContent() is overridden to only show the inner dialog content.
	 * getDialogConfiguration() returns inner dialog settings plus the predefined path.<br>
	 * - If invoked as part of a macro:<br>
	 * The behaviour is standard, i.e. the dialog shows outer and inner dialog to configure
	 * the entire task.
	 * @param parent
	 */
	public DETaskSaveSDFileAs(DEFrame parent) {
		super(parent, "");
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public int getFileType() {
		return CompoundFileHelper.cFileTypeSD;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		if (isInteractive()) {
			// this requests interactively the file & path from the user and puts it into a properties object
			mPredefinedConfiguration = super.getPredefinedConfiguration();
			if (mPredefinedConfiguration.getProperty(PROPERTY_FILENAME) == null)
				return mPredefinedConfiguration;	// suppress showing follow-up dialog if the file dialog was cancelled
			}

		return null;	// show a configuration dialog
		}

	@Override
	public JPanel createDialogContent() {
		// special handling with SD-files: 
		if (isInteractive()) {
			return createInnerDialogContent();
			}
		else {
			return super.createDialogContent();
			}
		}

	@Override
	public JPanel createInnerDialogContent() {
		JPanel p = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8} };
		p.setLayout(new TableLayout(size));

		int[] columnList = getTableModel().getSpecialColumnList(CompoundTableConstants.cColumnTypeIDCode);
		p.add(new JLabel("Structure column:"), "1,1");
		mComboBoxStructureColumn = new JComboBox();
		if (columnList != null)
			for (int column:columnList)
				mComboBoxStructureColumn.addItem(getTableModel().getColumnTitle(column));
		mComboBoxStructureColumn.addItem(OPTION_NO_STRUCTURE);
		mComboBoxStructureColumn.setEditable(!isInteractive());
		mComboBoxStructureColumn.addActionListener(this);
		p.add(mComboBoxStructureColumn, "3,1");

		p.add(new JLabel("SD-file version:"), "1,3");
		mComboBoxVersion = new JComboBox(SD_VERSION_OPTIONS);
		p.add(mComboBoxVersion, "3,3");

		p.add(new JLabel("Atom coordinates:"), "1,5");
		mComboBoxCoordinateMode = new JComboBox(COORDINATE_OPTIONS);
		mComboBoxCoordinateMode.setEditable(!isInteractive());
		mComboBoxCoordinateMode.addActionListener(this);
		p.add(mComboBoxCoordinateMode, "3,5");

		mCheckBoxIncludeRefMol = new JCheckBox("Include Reference Molecule");
		mCheckBoxIncludeRefMol.setEnabled(false);
		p.add(mCheckBoxIncludeRefMol, "1,7,3,7");

		p.add(new JLabel("Compound name column:"), "1,9");
		mComboBoxCompoundName = new JComboBox(COMPOUND_NAME_OPTIONS);
		mComboBoxCompoundName.setEditable(!isInteractive());
		for (int column=0; column<getTableModel().getTotalColumnCount(); column++)
			if (!getTableModel().isMultiCategoryColumn(column)
			 && getTableModel().getColumnSpecialType(column) == null)
				mComboBoxCompoundName.addItem(getTableModel().getColumnTitle(column));
		p.add(mComboBoxCompoundName, "3,9");

		return p;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mComboBoxStructureColumn)
			update3DCoordinateOptions();
		else if (e.getSource() == mComboBoxCoordinateMode) {
			boolean hasRefCompound = false;
			if (mComboBoxCoordinateMode.getSelectedIndex() != INDEX_PREFER_2D) {
				int column3D = mComboBoxCoordinateMode.getSelectedIndex() == INDEX_PREFER_3D ? -1
							: getTableModel().findColumn((String)mComboBoxCoordinateMode.getSelectedItem());
				if (column3D == -1) {
					int structureColumn = getTableModel().findColumn((String)mComboBoxStructureColumn.getSelectedItem());
					if (structureColumn != -1) {
						for (int column=0; column<getTableModel().getTotalColumnCount(); column++) {
							String specialType = getTableModel().getColumnSpecialType(column);
							if (CompoundTableModel.cColumnType3DCoordinates.equals(specialType)
							 && getTableModel().getParentColumn(column) == structureColumn) {
								column3D = column;
								break;
								}
							}
						}
					}
				if (column3D != -1) {
					if (getTableModel().getColumnProperty(column3D, CompoundTableConstants.cColumnPropertySuperposeMolecule) != null) {
						mCheckBoxIncludeRefMol.setText("Include Superposed Molecule");
						hasRefCompound = true;
						}
					if (getTableModel().getColumnProperty(column3D, CompoundTableConstants.cColumnPropertyNaturalLigand) != null
					 || getTableModel().getColumnProperty(column3D, CompoundTableConstants.cColumnPropertyProteinCavity) != null) {
						mCheckBoxIncludeRefMol.setText("Include Cavity & Natural Ligand");
						hasRefCompound = true;
						}
					}

				}
			mCheckBoxIncludeRefMol.setEnabled(hasRefCompound);
			}
		else
			super.actionPerformed(e);
		}

	/*	private void enableCoordinateMenu() {
	    for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
	        String specialType = mTableModel.getColumnSpecialType(column);
	        if (specialType != null
	         && structureColumn.equals(mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyParentColumn))) {
	            if (specialType.equals(CompoundTableModel.cColumnType2DCoordinates))
	                mSDColumn2DCoordinates = column;
	            else if (specialType.equals(CompoundTableModel.cColumnType3DCoordinates))
	                mSDColumn3DCoordinates = column;
	            }
	        }
	    if (mSDColumn3DCoordinates != -1) {
	        int option = JOptionPane.showOptionDialog(mParentFrame, "Where applicable, what do you prefer?",
	                "Coordinate Selection", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
	                null, new Object[] { "2D-Coordinates", "3D-Coordinates"}, "2D-Coordinates");
	        if (option == JOptionPane.CLOSED_OPTION) {
				finalStatus(null);
				return;
	        	}
	        mPrefer3D = (option == 1);
	        }
		}*/

	private void update3DCoordinateOptions() {
		mComboBoxCoordinateMode.removeAllItems();
		for (String item:COORDINATE_OPTIONS)
			mComboBoxCoordinateMode.addItem(item);

		int structureColumn = getTableModel().findColumn((String)mComboBoxStructureColumn.getSelectedItem());
		int[] coordinateColumn = getTableModel().getSpecialColumnList(CompoundTableConstants.cColumnType3DCoordinates);
		if (coordinateColumn != null)
			for (int column:coordinateColumn)
				if (getTableModel().getParentColumn(column) == structureColumn)
					mComboBoxCoordinateMode.addItem(getTableModel().getColumnTitle(column));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (!isInteractive())
			super.setDialogConfigurationToDefault();
		mComboBoxStructureColumn.setSelectedIndex(0);
		mComboBoxVersion.setSelectedIndex(1);
		update3DCoordinateOptions();
		mComboBoxCoordinateMode.setSelectedIndex(0);
		mCheckBoxIncludeRefMol.setSelected(false);

		int compoundOption = COMPOUND_NAME_OPTION_ROW_NUMBER;
		if (isInteractive()) {
			int structureColumn = getTableModel().findColumn((String)mComboBoxStructureColumn.getItemAt(0));
			if (getTableModel().findColumn(getTableModel().getColumnProperty(structureColumn, CompoundTableModel.cColumnPropertyRelatedIdentifierColumn)) != -1)
				compoundOption = COMPOUND_NAME_OPTION_COLUMN_PROPERTY;
			}
		mComboBoxCompoundName.setSelectedIndex(compoundOption);
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		if (!isInteractive())
			super.setDialogConfiguration(configuration);
		mComboBoxStructureColumn.setSelectedItem(configuration.getProperty(PROPERTY_STRUCTURE_COLUMN, (String)mComboBoxStructureColumn.getItemAt(0)));
		mComboBoxVersion.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_SD_VERSION), SD_VERSION_CODE, 1));

		update3DCoordinateOptions();
		String coordsCode = configuration.getProperty(PROPERTY_COORDINATE_MODE, COORDINATE_CODE[INDEX_PREFER_2D]);
		int index = findListIndex(coordsCode, COORDINATE_CODE, -1);
		if (index == -1)
			mComboBoxCoordinateMode.setSelectedItem(coordsCode);
		else
			mComboBoxCoordinateMode.setSelectedIndex(index);

		mCheckBoxIncludeRefMol.setSelected("true".equals(configuration.getProperty(PROPERTY_INCLUDE_REFERENCE_COMPOUND)));

		String nameCode = configuration.getProperty(PROPERTY_NAME_COLUMN, COMPOUND_NAME_CODE[COMPOUND_NAME_OPTION_COLUMN_PROPERTY]);
		index = findListIndex(nameCode, COMPOUND_NAME_CODE, -1);
		if (index == -1)
			mComboBoxCompoundName.setSelectedItem(nameCode);
		else
			mComboBoxCompoundName.setSelectedIndex(index);
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = isInteractive() ? mPredefinedConfiguration : super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_STRUCTURE_COLUMN, (String)mComboBoxStructureColumn.getSelectedItem());
		configuration.setProperty(PROPERTY_SD_VERSION, SD_VERSION_CODE[mComboBoxVersion.getSelectedIndex()]);
		int index = mComboBoxCoordinateMode.getSelectedIndex();
		configuration.setProperty(PROPERTY_COORDINATE_MODE, index < COORDINATE_CODE.length ?
				COORDINATE_CODE[index] : getTableModel().getColumnTitleNoAlias((String)mComboBoxCoordinateMode.getSelectedItem()));
		configuration.setProperty(PROPERTY_INCLUDE_REFERENCE_COMPOUND, mCheckBoxIncludeRefMol.isSelected() ? "true" : "false");
		index = mComboBoxCompoundName.getSelectedIndex();
		configuration.setProperty(PROPERTY_NAME_COLUMN, index < COMPOUND_NAME_CODE.length ?
				COMPOUND_NAME_CODE[index] : getTableModel().getColumnTitleNoAlias((String)mComboBoxCompoundName.getSelectedItem()));
		return configuration;
		}

	@Override
	public boolean isConfigurable() {
		if (getTableModel().getTotalRowCount() == 0
		 || getTableModel().getTotalColumnCount() == 0) {
			showErrorMessage("Empty documents cannot be saved.");
			return false;
			}
		return true;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String columnTitle = configuration.getProperty(PROPERTY_STRUCTURE_COLUMN, OPTION_NO_STRUCTURE);
		if (isLive && !columnTitle.equals(OPTION_NO_STRUCTURE)) {
			int column = getTableModel().findColumn(columnTitle);
			if (column == -1) {
				showErrorMessage("Column '"+columnTitle+"' not found.");
				return false;
				}
			if (!getTableModel().isColumnTypeStructure(column)) {
				showErrorMessage("Column '"+columnTitle+"' doesn't contain chemical structures.");
				return false;
				}
			}

		return super.isConfigurationValid(configuration, isLive);
		}

	@Override
	public void saveFile(File file, Properties configuration) {
		String structureTitle = configuration.getProperty(PROPERTY_STRUCTURE_COLUMN, OPTION_NO_STRUCTURE);
		int structureColumn = structureTitle.equals(OPTION_NO_STRUCTURE) ? -1 : getTableModel().findColumn(structureTitle);
		String coordinateMode = configuration.getProperty(PROPERTY_COORDINATE_MODE);
		boolean prefer2D = COORDINATE_CODE[INDEX_PREFER_2D].equals(coordinateMode);
		boolean prefer3D = COORDINATE_CODE[INDEX_PREFER_3D].equals(coordinateMode);
		boolean version3 = SD_VERSION_CODE[INDEX_VERSION_3].equals(configuration.getProperty(PROPERTY_SD_VERSION));
		int fileType = version3 ? CompoundFileHelper.cFileTypeSDV3 : CompoundFileHelper.cFileTypeSDV2;

		String name = configuration.getProperty(PROPERTY_NAME_COLUMN, COMPOUND_NAME_CODE[COMPOUND_NAME_OPTION_COLUMN_PROPERTY]);
		int index = findListIndex(name, COMPOUND_NAME_CODE, -1);
		int nameColumn = (index == COMPOUND_NAME_OPTION_ROW_NUMBER) ? CompoundTableSaver.ID_BUILD_ONE
					   : (index == COMPOUND_NAME_OPTION_COLUMN_PROPERTY) ? CompoundTableSaver.ID_USE_PROPERTY
					   : getTableModel().findColumn(name);

		CompoundTableModel tableModel = ((DEFrame)getParentFrame()).getMainFrame().getTableModel();
		JTable table = ((DEFrame)getParentFrame()).getMainFrame().getMainPane().getTable();

		int coordsColumn = -1;
		if (structureColumn != -1) {
			if (nameColumn == CompoundTableSaver.ID_USE_PROPERTY)
				nameColumn = tableModel.findColumn(tableModel.getColumnProperty(structureColumn, CompoundTableModel.cColumnPropertyRelatedIdentifierColumn));
			if (prefer2D || prefer3D) {
				for (int column=0; column<tableModel.getTotalColumnCount(); column++) {
					String specialType = tableModel.getColumnSpecialType(column);
					if (specialType != null && tableModel.getParentColumn(column) == structureColumn) {
						if (prefer2D && specialType.equals(CompoundTableModel.cColumnType2DCoordinates)) {
							coordsColumn = column;
							break;
							}
						if (prefer3D && specialType.equals(CompoundTableModel.cColumnType3DCoordinates)) {
							coordsColumn = column;
							break;
							}
						}
					}
				}
			else {
				coordsColumn = tableModel.findColumn(coordinateMode);
				}
			}

		StereoMolecule[] refMol = null;
		if ("true".equals(configuration.getProperty(PROPERTY_INCLUDE_REFERENCE_COMPOUND))
		 && CompoundTableModel.cColumnType3DCoordinates.equals(tableModel.getColumnSpecialType(coordsColumn))) {
			ArrayList<StereoMolecule> molList = new ArrayList<>();
			String superposeIDCode = getTableModel().getColumnProperty(coordsColumn, CompoundTableConstants.cColumnPropertySuperposeMolecule);
			if (superposeIDCode != null) {
				StereoMolecule superposeMol = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(superposeIDCode);
				if (superposeMol != null && superposeMol.getAllAtoms() != 0) {
					superposeMol.setName("Superposed Molecule");
					molList.add(superposeMol);
					}
				}
			String cavityIDCode = getTableModel().getColumnProperty(coordsColumn, CompoundTableConstants.cColumnPropertyProteinCavity);
			if (cavityIDCode != null) {
				StereoMolecule cavityMol = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(cavityIDCode);
				if (cavityMol != null && cavityMol.getAllAtoms() != 0) {
					cavityMol.setName("Protein Cavity");
					molList.add(cavityMol);
					}
				}
			String ligandIDCode = getTableModel().getColumnProperty(coordsColumn, CompoundTableConstants.cColumnPropertyNaturalLigand);
			if (ligandIDCode != null) {
				StereoMolecule ligandMol = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(ligandIDCode);
				if (ligandMol != null && ligandMol.getAllAtoms() != 0) {
					ligandMol.setName("Natural Ligand");
					molList.add(ligandMol);
					}
				}
			if (molList.size() != 0)
				refMol = molList.toArray(new StereoMolecule[0]);
			}

		new CompoundTableSaver(getParentFrame(), tableModel, table).saveSDFile(file,  fileType, structureColumn, nameColumn, coordsColumn, refMol);
		}
	}
