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
import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.MolfileParser;
import com.actelion.research.chem.SmilesParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DERuntimeProperties;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;
import org.openmolecules.datawarrior.plugin.IPluginHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class PluginHelper implements IPluginHelper {
	private DataWarrior mApplication;
	private DEFrame mParentFrame;
	private CompoundTableModel  mTableModel;
	private ProgressController  mProgressController;
	private int[]               mColumnType,mCoordinateColumn;
	private StereoMolecule      mMol;

	public PluginHelper(DataWarrior application, ProgressController pl) {
		mApplication = application;
		mProgressController = pl;
		}

	public DEFrame getNewFrame() {
		return mParentFrame;
		}

	@Override
	public void initializeData(int columnCount, int rowCount, String newWindowName) {
		if (mProgressController.threadMustDie())
			return;

		mParentFrame = mApplication.getEmptyFrame(newWindowName);
		mTableModel = mParentFrame.getTableModel();
		mTableModel.initializeTable(rowCount, columnCount);
		mColumnType = new int[columnCount];
		mCoordinateColumn = new int[columnCount];
		mProgressController.startProgress("Populating table...", 0, rowCount);
	}

	@Override
	public void setColumnTitle(int column, String title) {
		if (mProgressController.threadMustDie())
			return;

		mTableModel.setColumnName(title, column);
	}

	@Override
	public void setColumnType(int column, int type) {
		if (mProgressController.threadMustDie())
			return;

		mColumnType[column] = type;
		if (type == COLUMN_TYPE_STRUCTURE_FROM_SMILES
		 || type == COLUMN_TYPE_STRUCTURE_FROM_MOLFILE
		 || type == COLUMN_TYPE_STRUCTURE_FROM_IDCODE) {
			if (mMol == null)
				mMol = new StereoMolecule();
			mCoordinateColumn[column] = mTableModel.addNewColumns(1);
			mTableModel.setColumnProperty(column, CompoundTableConstants.cColumnPropertySpecialType,
					CompoundTableConstants.cColumnTypeIDCode);
			mTableModel.setColumnProperty(mCoordinateColumn[column], CompoundTableConstants.cColumnPropertySpecialType,
					CompoundTableConstants.cColumnType2DCoordinates);
		}
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
		else if (mColumnType[column] == COLUMN_TYPE_STRUCTURE_FROM_MOLFILE) {
			try {
				new MolfileParser().parse(mMol, value);
				Canonizer canonizer = new Canonizer(mMol);
				value = canonizer.getIDCode();
				coordinates = canonizer.getEncodedCoordinates();
			} catch (Exception e) {
				value = null;
			}
		}
		else if (mColumnType[column] == COLUMN_TYPE_STRUCTURE_FROM_IDCODE) {
			int index = value.indexOf(' ');
			if (index != -1) {
				coordinates = value.substring(index+1);
				value = value.substring(0, index);
			}
		}

		mTableModel.setTotalValueAt(value, row, column);
		if (coordinates != null && coordinates.length() != 0)
			mTableModel.setTotalValueAt(coordinates, row, mCoordinateColumn[column]);
	}

	@Override
	public void finalizeData(String template) {
		if (mProgressController.threadMustDie())
			return;

		// we need to do this, when all column titles are reliably set
		for (int column=0; column<mCoordinateColumn.length; column++)
			if (mCoordinateColumn[column] != 0)
				mTableModel.setColumnProperty(mCoordinateColumn[column],
						CompoundTableConstants.cColumnPropertyParentColumn,
						mTableModel.getColumnTitleNoAlias(column));

		if (template == null) {
			mTableModel.finalizeTable(CompoundTableEvent.cSpecifierDefaultFiltersAndViews, mProgressController);
			}
		else {
			mTableModel.finalizeTable(CompoundTableEvent.cSpecifierNoRuntimeProperties, mProgressController);
			DERuntimeProperties rtp = new DERuntimeProperties(mParentFrame.getMainFrame());
			try {
				rtp.read(new BufferedReader(new StringReader(template)));
				rtp.apply();
			} catch (IOException ioe) {}
		}
	}

	@Override
	public void showErrorMessage(String message) {
		mProgressController.showErrorMessage(message);
		}

	@Override
	public boolean isCancelled() {
		return mProgressController.threadMustDie();
	}
}
