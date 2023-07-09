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

import java.awt.*;
import java.util.HashMap;

/**
 * This is a callback interface of an object passed to a PluginTask's run() method
 * providing methods for plugin actions, e.g. to open a new DataWarrior window
 * and to populate the table model with data from a remote database.
 */
public interface IPluginHelper {
	int COLUMN_TYPE_ALPHANUMERICAL = 0;   // this is the default
	int COLUMN_TYPE_STRUCTURE_FROM_SMILES = 1;
	int COLUMN_TYPE_STRUCTURE_FROM_MOLFILE = 2;
	int COLUMN_TYPE_STRUCTURE_FROM_IDCODE = 3;

	String TEMPLATE_DEFAULT_FILTERS = "Filters";
	String TEMPLATE_DEFAULT_FILTERS_AND_VIEWS = "FiltersAndViews";
	String TEMPLATE_NONE = "None";

	/**
	 * @return Frame of front window
	 */
	Frame getParentFrame();

	/**
	 * Call this to get the column index of a structure column of the current front window.
	 * If multiple structure columns exist, then the user is asked by this method to select one.
	 * If no column of the active window contains chemical structures, then -1 is returned.
	 * This method is useful, if your plugin derives new information from chemical structures,
	 * e.g. calculates properties, send structures to a server for value retrieval or remote
	 * property calculations.
	 * @return valid structure column index or -1
	 */
	int getStructureColumn();

	/**
	 * If you know a column's title, use this method to get the column's index.
	 * This method is useful, if a column contains identifiers, which can be use
	 * to retrieve related information from a database.
	 * @return column index or -1 if a column with the given name was not found
	 */
	int findColumn(String columnTitle);

	/**
	 * Use this method whenever you need to display a column title.
	 * For displayable columns this method returns the alias (if existing) or the
	 * original column name. For non-displayable columns a title is constructed from type and
	 * parent title like <i>Structure [2D-Coordinates]</i> or <i>Reaction [ReactionFp]</i>.
	 * findColumn(String columnName) is guaranteed to correctly return the column index from this title.
	 * @return
	 */
	String getColumnTitle(int column);

	/**
	 * @param column
	 * @return all column properties of the specified column
	 */
	HashMap<String, String> getColumnProperties(int column);

	/**
	 * @return total column count of DataWarrior's current front window
	 */
	int getTotalColumnCount();

	/**
	 * @return total row count of DataWarrior's current front window
	 */
	int getTotalRowCount();

	/**
	 * @param visibleOnly if true, then all selected and currently visible rows are returned
	 * @return row indexes of all (or just currently visible) selected rows
	 */
	int[] getSelectedRows(boolean visibleOnly);

	/**
	 * Alphanumerical cells may contain multiple values, which are either separated by
	 * '; ' or in distinct lines. This method creates an array of all individual cell entries.
	 * Empty cells are returned as an array with size=1 containing one empty String.
	 * @param row total row index, which includes invisible rows
	 * @param column total column index, which includes invisible columns
	 * @return array of separated cell entries of active DataWarrior window
	 */
	String[] getCellData(int row, int column);

	/**
	 * If all cells of one column can be numerically interpreted or if the columns is defined
	 * to contain numerical values, then this method gets the numerical interpretation of a
	 * cell from this column. If that cell contrains multiple values, then this method returns
	 * a mean,median,sum,min,max value depending on the summary mode defined for that column.
	 * If a column is defined to treat values logarithmically, the the logarithm of the (summarized)
	 * value is returned. For empty cells and cells with non numerical content NaN is returned.
	 * @param row total row index, which includes invisible rows
	 * @param column total column index, which includes invisible columns
	 * @return numerical cell content interpretation of active DataWarrior window
	 */
	double getCellDataNumerical(int row, int column);

	/**
	 * Assuming that the given column contains chemical structures, the cell's structure
	 * is converted into molfile version 2, which is then returned.
	 * @param row
	 * @param column
	 * @return valid molfile V2 or null, if the cell is empty
	 */
	String getCellDataAsMolfileV2(int row, int column);

	/**
	 * Assuming that the given column contains chemical structures, the cell's structure
	 * is converted into molfile version 3, which is then returned.
	 * @param row
	 * @param column
	 * @return valid molfile V3 or null, if the cell is empty
	 */
	String getCellDataAsMolfileV3(int row, int column);

	/**
	 * Assuming that the given column contains chemical structures, the cell's structure
	 * is converted into an isomeric, canonical SMILES, which is then returned.
	 * @param row
	 * @param column
	 * @return valid SMILES or null, if the cell is empty
	 */
	String getCellDataAsSmiles(int row, int column);

	/**
	 * Call this if you need to append one or more new columns to DataWarrior's active window.
	 *
	 * @param columnTitle
	 * @return column index of the first of the new columns; -1 task was cancelled
	 */
	int initializeNewColumns(String[] columnTitle);

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
	 * Call this for columns that contain chemical structures
	 * @param column
	 * @param type COLUMN_TYPE_...
	 */
	void setColumnType(int column, int type);

	/**
	 * If need to change your column's behaviour, you may supply column properties
	 * @param column
	 * @param key
	 * @param value
	 */
	void setColumnProperty(int column, String key, String value);

	/**
	 * If you have used setColumnType() to define the nature of new columns, then use
	 * this method to populate table data.
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
	 * This method directly stores the passed object in the native DataWarrior table
	 * skipping any structure format adaption. If you have defined columns natively using
	 * setColumnProperty() rather setColumnType(), then you may use this method to populate
	 * table model calls with idcodes, id-coords, descriptors and alphanumerical content.
	 * @param column
	 * @param row
	 * @param value byte[] for alphanumerical columns and native object for descriptors
	 */
	void setCellDataNative(int column, int row, Object value);

	/**
	 * If you have called initializeData() and, thus, created a new windows, then
	 * call this methof after all column types and titles are defined and after
	 * all cell data was set.
	 * @param template null or TEMPLATE... or a valid custom template String as it is found in a .dwat file
	 */
	void finalizeData(String template);

	/**
	 * If you have called initializeNewColumns() and, thus, appended a few columns
	 * to the currently active window, then you need to call this method after
	 * setting supplying the content for the new column's cells.
	 * @param firstColumn
	 */
	void finalizeNewColumns(int firstColumn);

	/**
	 * If a variable was defined earlier in the running macro, then this method
	 * returns the value of the valiable with the given name.
	 * @param name
	 * @return null if variable doesn't exist
	 */
	String getVariable(String name);

	/**
	 * Runs the given macro in text form on the current front window.
	 * @param macro
	 */
	void runMacro(String macro);

	/**
	 * Applies the given runtime properties in text format.
	 * The template needs to be a multi line String as it appears in a dwat or dwar file
	 * starting with a '<column properties>' line and ending with a '</column properties>' line.
	 * @param template
	 * @param clearFirst if true, then all views and filters are removed before applying the template
	 */
	void setRuntimeProperties(String template, boolean clearFirst);

	/**
	 * If errors happen during the execution of the task (e.g. database is down)
	 * then call this method to show a user interpretable error to the user and return
	 * from the task's run() method.
	 * @param message
	 */
	void showErrorMessage(String message);

	/**
	 * To show a success messages, e.g. after a performed database transaction,
	 * call this method from the task's run() method after successful task completion.
	 * @param message
	 */
	void showSuccessMessage(String message);

	/**
	 * Shows a warning messages, when called in the task's run() method.
	 * @param message
	 */
	void showWarningMessage(String message);

	/**
	 * During task execution DataWarrior shows a progress dialog with a cancel button.
	 * For lengthy tasks you should frequently call this method and stop the task
	 * execution if this method returns true.
	 * @return
	 */
	boolean isCancelled();  // true, if the user has pressed the stop/cancel button of a progress dialog
	}
