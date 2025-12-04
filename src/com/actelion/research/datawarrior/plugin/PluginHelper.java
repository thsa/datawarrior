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

package com.actelion.research.datawarrior.plugin;

import com.actelion.research.calc.ProgressController;
import com.actelion.research.chem.*;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DERuntimeProperties;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.AbstractTask;
import com.actelion.research.datawarrior.task.DEMacro;
import com.actelion.research.datawarrior.task.DEMacroRecorder;
import com.actelion.research.datawarrior.task.db.DETaskPluginTask;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;
import org.openmolecules.datawarrior.plugin.IPluginHelper;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Properties;
import java.util.TreeMap;

public class PluginHelper implements IPluginHelper {
	private final DataWarrior mApplication;
	private final DETaskPluginTask mPluginTask;
	private DEFrame mNewFrame;
	private final CompoundTableModel mSourceTableModel;
	private CompoundTableModel mTargetTableModel;
	private final ProgressController mProgressController;
	private int[] mColumnType;
	private StereoMolecule mMol;
	private TreeMap<Integer,Integer> mCoordinateColumnMap;
	private Properties mAnyPurposeProperties;

	public PluginHelper(DataWarrior application, DETaskPluginTask task, ProgressController pl) {
		mApplication = application;
		mPluginTask = task;
		mSourceTableModel = application.getActiveFrame().getTableModel();
		mTargetTableModel = mSourceTableModel;  // unless a new window is created
		mProgressController = pl;
	}

	@Override
	public int findColumn(String columnTitle) {
		return mSourceTableModel.findColumn(columnTitle);
	}

	@Override
	public int getCoordinateColumn(int column, boolean is3D) {
		if (mCoordinateColumnMap.containsKey(column))
			return mCoordinateColumnMap.get(column);

		return mSourceTableModel.getChildColumn(column, is3D ?
				CompoundTableConstants.cColumnType3DCoordinates : CompoundTableConstants.cColumnType2DCoordinates);
	}

	@Override
	public int getStructureColumn() {
		int[] structureColumn = mSourceTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeIDCode);
		if (structureColumn == null)
			return -1;
		if (structureColumn.length == 1)
			return structureColumn[0];

		String[] columnNameList = new String[structureColumn.length];
		for (int i=0; i<structureColumn.length; i++)
			columnNameList[i] = mSourceTableModel.getColumnTitle(structureColumn[i]);

		try {
			int[] column = new int[1];
			column[0] = -1;
			SwingUtilities.invokeAndWait(() -> {
				column[0] = mSourceTableModel.findColumn(
					(String)JOptionPane.showInputDialog(mApplication.getActiveFrame(),
						"Please select a column with chemical structures!",
						"Select Structure Column",
						JOptionPane.QUESTION_MESSAGE,
						null,
						columnNameList,
						columnNameList[0]));
				});
			return column[0];
			}
		catch (Exception ie) {
			return -1;
			}
		}

	@Override
	public int[] getSelectedRows(boolean visibleOnly) {
		int count = 0;
		for (int row=0; row<mSourceTableModel.getTotalRowCount(); row++) {
			CompoundRecord record = mSourceTableModel.getTotalRecord(row);
			if (visibleOnly) {
				if (mSourceTableModel.isVisibleAndSelected(record))
					count++;
				}
			else {
				if (mSourceTableModel.isSelected(record))
					count++;
				}
			}

		int[] selectedRow = new int[count];
		count = 0;
		for (int row=0; row<mSourceTableModel.getTotalRowCount(); row++) {
			CompoundRecord record = mSourceTableModel.getTotalRecord(row);
			if (visibleOnly) {
				if (mSourceTableModel.isVisibleAndSelected(record))
					selectedRow[count++] = row;
			}
			else {
				if (mSourceTableModel.isSelected(record))
					selectedRow[count++] = row;
				}
			}

		return selectedRow;
		}

	@Override
	public int getTotalRowCount() {
		return mSourceTableModel.getTotalRowCount();
	}

	@Override
	public String getColumnTitle(int column) {
		return mSourceTableModel.getColumnTitle(column);
	}

	@Override
	public HashMap<String, String> getColumnProperties(int column) {
		return mSourceTableModel.getColumnProperties(column);
	}

	@Override
	public int getTotalColumnCount() {
		return mSourceTableModel.getTotalColumnCount();
	}

	@Override
	public String[] getCellData(int row, int column) {
		return mSourceTableModel.separateEntries(mSourceTableModel.getTotalValueAt(row, column));
	}

	@Override
	public double getCellDataNumerical(int row, int column) {
		double value = mSourceTableModel.getTotalDoubleAt(row, column);
		if (!Double.isNaN(value) && mSourceTableModel.isLogarithmicViewMode(column))
			value = Math.pow(10, value);
		return value;
	}

	@Override
	public String getCellDataAsSmiles(int row, int column) {
		if (!mSourceTableModel.isColumnTypeStructure(column))
			return null;

		StereoMolecule mol = mSourceTableModel.getChemicalStructure(mSourceTableModel.getTotalRecord(row), column, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
		return mol == null || mol.getAllAtoms()==0 ? null : new IsomericSmilesCreator(mol).getSmiles();
	}

	@Override
	public String getCellDataAsMolfileV2(int row, int column) {
		if (!mSourceTableModel.isColumnTypeStructure(column))
			return null;

		StereoMolecule mol = mSourceTableModel.getChemicalStructure(mSourceTableModel.getTotalRecord(row), column, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
		return mol == null || mol.getAllAtoms()==0 ? null : new MolfileCreator(mol).getMolfile();
	}

	@Override
	public String getCellDataAsMolfileV3(int row, int column) {
		if (!mSourceTableModel.isColumnTypeStructure(column))
			return null;

		StereoMolecule mol = mSourceTableModel.getChemicalStructure(mSourceTableModel.getTotalRecord(row), column, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
		return mol == null || mol.getAllAtoms()==0 ? null : new MolfileV3Creator(mol).getMolfile();
	}

	@Override
	public String getCellDataAsIDCode(int row, int column) {
		int coordsColumn = -1;
		boolean is3D = CompoundTableConstants.cColumnType3DCoordinates.equals(mSourceTableModel.getColumnSpecialType(column));
		if (is3D) {
			coordsColumn = column;
			column = mSourceTableModel.getParentColumn(column);
			}
		else if (!mSourceTableModel.isColumnTypeStructure(column))
			return null;

		byte[] idcode = (byte[])mSourceTableModel.getTotalRecord(row).getData(column);
		if (idcode == null)
			return null;

		if (!is3D)
			coordsColumn = mSourceTableModel.getChildColumn(column, CompoundTableConstants.cColumnType2DCoordinates);

		byte[] coords = (coordsColumn == -1) ? null : (byte[])mSourceTableModel.getTotalRecord(row).getData(coordsColumn);
		return (coords == null) ? new String(idcode) : new String(idcode).concat(" ").concat(new String(coords));
	}

	@Override
	public Object getCellDataNative(int row, int column) {
		return mSourceTableModel.getTotalRecord(row).getData(column);
	}

	@Override
	public String getVariable(String name) {
		if (!(mProgressController instanceof DEMacroRecorder))
			return null;
		return ((DEMacroRecorder)mProgressController).getVariable(name);
		}

	@Override
	public Frame getParentFrame() {
		return mApplication.getActiveFrame();
	}

	public DEFrame getNewFrame() {
		return mNewFrame;
		}

	@Override
	public int initializeNewColumns(String[] columnTitle) {
		if (mProgressController.threadMustDie())
			return -1;

		mColumnType = new int[mSourceTableModel.getTotalColumnCount()+columnTitle.length];
		mCoordinateColumnMap = new TreeMap<>();
		int firstColumn = mSourceTableModel.addNewColumns(columnTitle);
		mProgressController.startProgress("Populating table...", 0, 0);
		return firstColumn;
	}

	@Override
	public void initializeData(int columnCount, int rowCount, String newWindowName) {
		if (mProgressController.threadMustDie())
			return;

		mNewFrame = mApplication.getEmptyFrame(newWindowName);
		mTargetTableModel = mNewFrame.getTableModel();
		mTargetTableModel.initializeTable(rowCount, columnCount);
		mColumnType = new int[columnCount];
		mCoordinateColumnMap = new TreeMap<>();
		mProgressController.startProgress("Populating table...", 0, rowCount);
	}

	@Override
	public void setColumnTitle(int column, String title) {
		if (mProgressController.threadMustDie())
			return;

		mTargetTableModel.setColumnName(title, column);
	}

	@Override
	public void setColumnType(int column, int type) {
		if (mProgressController.threadMustDie())
			return;

		mColumnType[column] = type;
		if (type == COLUMN_TYPE_STRUCTURE_FROM_SMILES
		 || type == COLUMN_TYPE_STRUCTURE_FROM_MOLFILE
		 || type == COLUMN_TYPE_STRUCTURE_FROM_IDCODE
		 || type == COLUMN_TYPE_3D_STRUCTURE_FROM_MOLFILE
		 || type == COLUMN_TYPE_3D_STRUCTURE_FROM_IDCODE) {
			if (mMol == null)
				mMol = new StereoMolecule();
			boolean is3D = type == COLUMN_TYPE_3D_STRUCTURE_FROM_MOLFILE || type == COLUMN_TYPE_3D_STRUCTURE_FROM_IDCODE;
			int coordsColumn = mTargetTableModel.addNewColumns(1);
			mCoordinateColumnMap.put(column, coordsColumn);
			mTargetTableModel.setColumnName(is3D ? "Conformer" : "atomCoordinates2D", coordsColumn);
			mTargetTableModel.setColumnProperty(column, CompoundTableConstants.cColumnPropertySpecialType,
					CompoundTableConstants.cColumnTypeIDCode);
			mTargetTableModel.setColumnProperty(coordsColumn, CompoundTableConstants.cColumnPropertySpecialType,
					is3D ? CompoundTableConstants.cColumnType3DCoordinates : CompoundTableConstants.cColumnType2DCoordinates);
			mTargetTableModel.setColumnProperty(coordsColumn, CompoundTableConstants.cColumnPropertyParentColumn,
					mTargetTableModel.getColumnTitleNoAlias(column));
		}
	}

	@Override
	public void setColumnProperty(int column, String key, String value) {
		mTargetTableModel.setColumnProperty(column, key, value);
		}

	@Override
	public void setRuntimeProperties(String template, boolean clearFirst) {
		DEFrame frame = mNewFrame != null ? mNewFrame : mApplication.getActiveFrame();
		DERuntimeProperties rtp = new DERuntimeProperties(frame.getMainFrame());
		try {
			rtp.read(new BufferedReader(new StringReader(template)));
			rtp.apply(clearFirst);
			}
		catch (IOException ioe) {}
		}

	@Override
	public void setCellData(int column, int row, String value) {
		if (mProgressController.threadMustDie())
			return;
		if (column == 0)
			mProgressController.updateProgress(row);

		String coordinates = null;
		if (mColumnType[column] == COLUMN_TYPE_STRUCTURE_FROM_SMILES) {
			try {
				new SmilesParser().parse(mMol, value);
				Canonizer canonizer = new Canonizer(mMol);
				value = canonizer.getIDCode();
				coordinates = canonizer.getEncodedCoordinates();
			} catch (Exception e) {
				value = null;
			}
		}
		else if (mColumnType[column] == COLUMN_TYPE_STRUCTURE_FROM_MOLFILE
			  || mColumnType[column] == COLUMN_TYPE_3D_STRUCTURE_FROM_MOLFILE) {
			try {
				new MolfileParser().parse(mMol, value);
				Canonizer canonizer = new Canonizer(mMol);
				value = canonizer.getIDCode();
				coordinates = canonizer.getEncodedCoordinates();
			} catch (Exception e) {
				value = null;
			}
		}
		else if (mColumnType[column] == COLUMN_TYPE_STRUCTURE_FROM_IDCODE
			  || mColumnType[column] == COLUMN_TYPE_3D_STRUCTURE_FROM_IDCODE) {
			int index = value.indexOf(' ');
			if (index != -1) {
				coordinates = value.substring(index+1);
				value = value.substring(0, index);
			}
		}

		mTargetTableModel.setTotalValueAt(value, row, column);
		if (coordinates != null && !coordinates.isEmpty())
			mTargetTableModel.setTotalValueAt(coordinates, row, mCoordinateColumnMap.get(column));
	}

	@Override
	public void setCellDataNative(int column, int row, Object value) {
		mTargetTableModel.getTotalRecord(row).setData(value, column);
	}

	@Override
	public void finalizeData(String template) {
		if (mProgressController.threadMustDie())
			return;

		// we need to do this, when all column titles are reliably set
		for (int structureColumn:mCoordinateColumnMap.keySet())
			if (mCoordinateColumnMap.get(structureColumn) != null)
				mTargetTableModel.setColumnProperty(mCoordinateColumnMap.get(structureColumn),
						CompoundTableConstants.cColumnPropertyParentColumn,
						mTargetTableModel.getColumnTitleNoAlias(structureColumn));

		if (template == null || template.equals(TEMPLATE_DEFAULT_FILTERS_AND_VIEWS)) {
			mTargetTableModel.finalizeTable(CompoundTableEvent.cSpecifierDefaultFiltersAndViews, mProgressController);
		}
		else if (template.equals(TEMPLATE_DEFAULT_FILTERS)) {
			mTargetTableModel.finalizeTable(CompoundTableEvent.cSpecifierDefaultFilters, mProgressController);
		}
		else if (template.equals(TEMPLATE_NONE)) {
			mTargetTableModel.finalizeTable(CompoundTableEvent.cSpecifierNoRuntimeProperties, mProgressController);
		}
		else {
			mTargetTableModel.finalizeTable(CompoundTableEvent.cSpecifierNoRuntimeProperties, mProgressController);
			DERuntimeProperties rtp = new DERuntimeProperties(mNewFrame.getMainFrame());
			try {
				rtp.read(new BufferedReader(new StringReader(template)));
				rtp.apply();
			} catch (IOException ioe) {}
		}
	}

	@Override
	public void finalizeNewColumns(int firstColumn) {
		if (mProgressController.threadMustDie())
			return;

		mTargetTableModel.finalizeNewColumns(firstColumn, mProgressController);
	}

	@Override
	public void runMacro(String macro) {
		if (macro.startsWith(DEMacro.MACRO_START)) {
			try {
				BufferedReader reader = new BufferedReader(new StringReader(macro));
				DEMacroRecorder.getInstance().runMacro(new DEMacro(reader, null),
						mNewFrame != null ? mNewFrame : mApplication.getActiveFrame());
			}
			catch (IOException ioe) {}
		}
	}

	@Override
	public void showErrorMessage(String message) {
		mProgressController.showErrorMessage(message);
		}

	@Override
	public void showSuccessMessage(String message) {
		mPluginTask.showInteractiveTaskMessage(message, AbstractTask.INFORMATION_MESSAGE);
		}

	@Override
	public void showWarningMessage(String message) {
		mPluginTask.showInteractiveTaskMessage(message, AbstractTask.WARNING_MESSAGE);
		}

	@Override
	public void startProgress(String message, int steps) {
		if (mPluginTask.getProgressController() != null)
			mPluginTask.getProgressController().startProgress(message, 0, steps);
	}

	@Override
	public void updateProgress(int step) {
		if (mPluginTask.getProgressController() != null)
			mPluginTask.getProgressController().updateProgress(step);
	}

	@Override
	public boolean isCancelled() {
		return mProgressController.threadMustDie();
	}

	@Override
	public String getAnyPurposeProperty(String key) {
		return mAnyPurposeProperties == null ? null : mAnyPurposeProperties.getProperty(key);
	}

	@Override
	public void setAnyPurposeProperty(String key, String value) {
		if (mAnyPurposeProperties == null)
			mAnyPurposeProperties = new Properties();

		mAnyPurposeProperties.setProperty(key, value);
	}
}
