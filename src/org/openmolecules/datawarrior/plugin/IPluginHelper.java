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

package org.openmolecules.datawarrior.plugin;

/**
 * This is a callback interface of an object passed to a PluginTask's run() method
 * providing methods for plugin actions, e.g. to open a new DataWarrior window
 * and to populate the table model with data from a remote database.
 */
public interface IPluginHelper {
	public int COLUMN_TYPE_ALPHANUMERICAL = 0;   // this is the default
	public int COLUMN_TYPE_STRUCTURE_FROM_SMILES = 1;
	public int COLUMN_TYPE_STRUCTURE_FROM_MOLFILE = 2;
	public int COLUMN_TYPE_STRUCTURE_FROM_IDCODE = 3;

	/**
	 * Call this before setting column type/title and cell data.
	 * @param columnCount visible column count
	 * @param rowCount data rows not including the title line
	 * @param newWindowName
	 */
	void initializeData(int columnCount, int rowCount, String newWindowName);

	/**
	 * Call this once per column to overwrite the default titles 'Column n'
	 * @param column
	 * @param title
	 */
	void setColumnTitle(int column, String title);

	/**
	 * Call this for columns that contains chemical structures
	 * @param column
	 * @param type COLUMN_TYPE_...
	 */
	void setColumnType(int column, int type);

	/**
	 * If the column's type is COLUMN_TYPE_STRUCTURE_FROM_SMILES or
	 * COLUMN_TYPE_STRUCTURE_FROM_MOLFILE, then a SMILES string or molfile (v2000 or v3000)
	 * should be passed. if the column's type is COLUMN_TYPE_STRUCTURE_FROM_IDCODE,
	 * then you may either pass an idcode or an idcode+SPACE+id-coordinates.
	 * In either case DataWarrior creates a chemical structure from the input,
	 * stores a canonical interpretation (idcode) and maintains the 2D-atoms coordinates
	 * in a second hidden column.
	 * @param column
	 * @param row
	 * @param value may be multiline content separated with \n
	 */
	void setCellData(int column, int row, String value);

	/**
	 * Call this after all column types and titles are defined and all cell data
	 * is set.
	 * @param template null or a valid template String as it is found in a .dwat file
	 */
	void finalizeData(String template);

	/**
	 * If errors happen during the execution of the task (e.g. database is down)
	 * then call this method to show a user interpretable error to the user and return
	 * from the task's run() method.
	 * @param message
	 */
	void showErrorMessage(String message);

	/**
	 * During task execution DataWarrior shows a progress dialog with a cancel button.
	 * For lengthy tasks you should frequently call this method and stop the task
	 * execution if this method returns true.
	 * @return
	 */
	boolean isCancelled();  // true, if the user has pressed the stop/cancel button of a progress dialog
	}
