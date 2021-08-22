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
	 * Call this to get the column index of a structure column of the current front window.
	 * If multiple structure columns exist, then the user is asked by this method to select one.
	 * If no column of the active window contains chemical structures, then -1 is returned.
	 * This method is useful, if your plugin derives new information from chemical structures,
	 * e.g. calculates properties, send structures to a server for value retrieval or remote
	 * property calculations.
	 * @return valid structure column index or -1
	 */
	public int getStructureColumn();

	/**
	 * If you know a column's title, use this method to get the column's index.
	 * This method is useful, if a column contains identifiers, which can be use
	 * to retrieve related information from a database.
	 * @return column index or -1 if a column with the given name was not found
	 */
	public int findColumn(String columnTitle);

	/**
	 * @return total row count of DataWarrior's current front window
	 */
	public int getTotalRowCount();

	/**
	 * Alphanumerical cells may contain multiple values, which are either separated by
	 * '; ' or in distinct lines. This method creates an array of all individual cell entries.
	 * Empty cells are returned as an array with size=1 containing one empty String.
	 * @param row total row index, which includes invisible rows
	 * @param column total column index, which includes invisible columns
	 * @return array of separated cell entries of active DataWarrior window
	 */
	public String[] getCellData(int row, int column);

	/**
	 * Assuming that the given column contains chemical structures, the cell's structure
	 * is converted into molfile version 2, which is then returned.
	 * @param row
	 * @param column
	 * @return valid molfile V2 or null, if the cell is empty
	 */
	public String getCellDataAsMolfileV2(int row, int column);

	/**
	 * Assuming that the given column contains chemical structures, the cell's structure
	 * is converted into molfile version 3, which is then returned.
	 * @param row
	 * @param column
	 * @return valid molfile V3 or null, if the cell is empty
	 */
	public String getCellDataAsMolfileV3(int row, int column);

	/**
	 * Assuming that the given column contains chemical structures, the cell's structure
	 * is converted into an isomeric, canonical SMILES, which is then returned.
	 * @param row
	 * @param column
	 * @return valid SMILES or null, if the cell is empty
	 */
	public String getCellDataAsSmiles(int row, int column);

	/**
	 * Call this if you need to append one or more new columns to DataWarrior's active window.
	 *
	 * @param columnTitle
	 * @return column index of the first of the new columns; -1 task was cancelled
	 */
	public int initializeNewColumns(String[] columnTitle);

	/**
	 * Call this if you need a new window to be populated with your data.
	 * After this method define new column types and titles. Then add the cell data.
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
	 * If you have called initializeData() and, thus, created a new windows, then
	 * call this methof after all column types and titles are defined and after
	 * all cell data was set.
	 * @param template null or a valid template String as it is found in a .dwat file
	 */
	void finalizeData(String template);

	/**
	 * If you have called initializeNewColumns() and, thus, appended a few columns
	 * to the currently active window, then you need to call this method after
	 * setting supplying the content for the new column's cells.
	 * @param firstColumn
	 */
	public void finalizeNewColumns(int firstColumn);

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
