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

import com.actelion.research.chem.SortedStringList;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DERuntimeProperties;
import com.actelion.research.datawarrior.task.AbstractTask;
import com.actelion.research.datawarrior.task.TaskUIDelegate;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.table.CompoundTableLoader;
import com.actelion.research.table.model.CompoundTableModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

public class DETaskMergeFile extends AbstractTask implements TaskConstantsMergeFile {
	public static final String TASK_NAME_FILE = "Merge File";
	public static final String TASK_NAME_CLIPBOARD = "Merge Clipboard Content";

	private static final int IS_NOT_DISPLAYABLE = -1;
	private static final int IS_NORMAL_DISPLAYABLE = 0;

	private CompoundTableModel	mTableModel;
	private UIDelegateMergeFile	mUIDelegate;
	private CompoundTableLoader	mLoader;
	private boolean             mIsClipboard;

	public DETaskMergeFile(DEFrame parent, boolean isClipboard) {
		super(parent, true);
		// All tasks that use CompoundTableLoader need to run in an own thread if they run in a macro
		// to prevent the CompoundTableLoader to run processData() in a new thread without waiting
		// in the EDT
		mTableModel = parent.getTableModel();
		mIsClipboard = isClipboard;
		}

	@Override
	public String getTaskName() {
		return mIsClipboard ? TASK_NAME_CLIPBOARD : TASK_NAME_FILE;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/import.html#MergeData";
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
	public TaskUIDelegate createUIDelegate() {
		mUIDelegate = new UIDelegateMergeFile((DEFrame)getParentFrame(), this, isInteractive(), mIsClipboard);
		return mUIDelegate;
		}

	@Override
	public boolean isConfigurable() {
		if (mTableModel.getTotalColumnCount() == 0) {
			showErrorMessage("You cannot merge a file with an empty table. Use 'Open File...' instead.");
			return false;
			}

		return true;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String fileName = configuration.getProperty(PROPERTY_FILENAME);
		if (!mIsClipboard && !isFileAndPathValid(fileName, false, false))
			return false;

		int columnCount = 0;
		try { columnCount = Integer.parseInt(configuration.getProperty(PROPERTY_COLUMN_COUNT, "0")); } catch (NumberFormatException nfe) {}
		if (columnCount == 0) {
			showErrorMessage("No source columns defined for merging.");
			return false;
			}

		boolean mergeColumnFound = false;
		SortedStringList usedColumnList = new SortedStringList();
		for (int i=0; i<columnCount; i++) {
			String sourceColumn = configuration.getProperty(PROPERTY_SOURCE_COLUMN+i, "");
			if (sourceColumn.length() == 0) {
				showErrorMessage("Source column not defined.");
				return false;
				}

			String destColumn = configuration.getProperty(PROPERTY_DEST_COLUMN+i, "");
			if (destColumn.length() == 0) {
				showErrorMessage("Destination column not defined.");
				return false;
				}

			String option = configuration.getProperty(PROPERTY_OPTION+i);

			if (!destColumn.equals(DEST_COLUMN_ADD)
			 && !destColumn.equals(DEST_COLUMN_TRASH)) {
				if (isLive) {
					int column = mTableModel.findColumn(destColumn);
					if (column == -1) {
						showErrorMessage("Destination column '"+destColumn+"' not found.");
						return false;
						}
					if (OPTION_CODE[CompoundTableLoader.MERGE_MODE_IS_KEY_NO_CASE].equals(option) && mTableModel.getColumnSpecialType(column) != null) {
						showErrorMessage("You cannot use 'ignore case' when merging equal structures or reactions.");
						return false;
						}
					}
				if (usedColumnList.addString(destColumn) == -1) {
					showErrorMessage("Column '"+destColumn+"' is used twice.");
					return false;
					}
				if (OPTION_CODE[CompoundTableLoader.MERGE_MODE_IS_KEY].equals(option)
				 || OPTION_CODE[CompoundTableLoader.MERGE_MODE_IS_KEY_NO_CASE].equals(option)
				 || OPTION_CODE[CompoundTableLoader.MERGE_MODE_IS_KEY_WORD_SEARCH].equals(option))
					mergeColumnFound = true;
				}
			}
		if (!mergeColumnFound) {
			showErrorMessage("You need to define at least one column to contain merge keys.");
			return false;
			}

		if (mUIDelegate != null) {
			mLoader = mUIDelegate.getCompoundTableLoader();
			}
		else {
			if (mIsClipboard) {
				DEFrame parent = (DEFrame)getParentFrame();
				mLoader = new CompoundTableLoader(parent, parent.getTableModel(), getProgressController());
				mLoader.paste(1, true);
				}
			else {
				fileName = resolvePathVariables(fileName);
				DEFrame parent = (DEFrame)getParentFrame();
				mLoader = new CompoundTableLoader(parent, parent.getTableModel(), getProgressController());
				mLoader.readFile(new File(fileName), new DERuntimeProperties(parent.getMainFrame()), FileHelper.getFileType(fileName), CompoundTableLoader.READ_DATA);
				}
			}

		int keyColumnCount = 0;
		int keyWordMatchColumnCount = 0;
		for (int i=0; i<columnCount; i++) {
			String option = configuration.getProperty(PROPERTY_OPTION + i);
			String destColumn = configuration.getProperty(PROPERTY_DEST_COLUMN+i, "");
			if (!destColumn.equals(DEST_COLUMN_ADD)
			 && !destColumn.equals(DEST_COLUMN_TRASH)
			 && (OPTION_CODE[CompoundTableLoader.MERGE_MODE_IS_KEY].equals(option)
			  || OPTION_CODE[CompoundTableLoader.MERGE_MODE_IS_KEY_NO_CASE].equals(option)
			  || OPTION_CODE[CompoundTableLoader.MERGE_MODE_IS_KEY_WORD_SEARCH].equals(option))) {
				keyColumnCount++;
				if (OPTION_CODE[CompoundTableLoader.MERGE_MODE_IS_KEY_WORD_SEARCH].equals(option))
					keyWordMatchColumnCount++;
				}
			}
		if (keyWordMatchColumnCount > 1) {
			showErrorMessage("The option '"+OPTION_TEXT[CompoundTableLoader.MERGE_MODE_IS_KEY_WORD_SEARCH]+"' can be used for one column only.");
			return false;
			}
		if (keyWordMatchColumnCount == 1 && keyColumnCount > 1) {
			showErrorMessage("The option '"+OPTION_TEXT[CompoundTableLoader.MERGE_MODE_IS_KEY_WORD_SEARCH]+"' can be combined with merge keys on other columns.");
			return false;
			}
		String[] keyColumnName = new String[keyColumnCount];
		boolean[] isIgnoreCase = new boolean[keyColumnCount];
		keyColumnCount = 0;
		for (int i=0; i<columnCount; i++) {
			String option = configuration.getProperty(PROPERTY_OPTION+i);
			String destColumn = configuration.getProperty(PROPERTY_DEST_COLUMN+i, "");
			if (!destColumn.equals(DEST_COLUMN_ADD)
			 && !destColumn.equals(DEST_COLUMN_TRASH)
			 && (OPTION_CODE[CompoundTableLoader.MERGE_MODE_IS_KEY].equals(option)
			  || OPTION_CODE[CompoundTableLoader.MERGE_MODE_IS_KEY_NO_CASE].equals(option)
			  || OPTION_CODE[CompoundTableLoader.MERGE_MODE_IS_KEY_WORD_SEARCH].equals(option))) {
				if (!OPTION_CODE[CompoundTableLoader.MERGE_MODE_IS_KEY].equals(option))
					isIgnoreCase[keyColumnCount] = true;
				keyColumnName[keyColumnCount++] = configuration.getProperty(PROPERTY_SOURCE_COLUMN + i);
				}
			}
		if (!mLoader.areMergeKeysUnique(keyColumnName, isIgnoreCase, null)) {
			showErrorMessage("The defined key column(s) contain duplicate data in some rows and cannot uniquely identify each row.");
			return false;
			}

		return true;
		}

	private int getDisplayableType(String specialType) {
		if (specialType == null)
			return IS_NORMAL_DISPLAYABLE;

		for (int i=0; i<CompoundTableModel.cParentSpecialColumnTypes.length; i++)
			if (specialType.equals(CompoundTableModel.cParentSpecialColumnTypes[i]))
				return i+1;

		return IS_NOT_DISPLAYABLE;
		}

	private String[] getVisibleFieldNames() {
		String[] totalFieldName = mLoader.getFieldNames();
		ArrayList<String> visibleFieldList = new ArrayList<String>();
		for (int i=0; i<totalFieldName.length; i++) {
			int displayableType = getDisplayableType(mLoader.getColumnSpecialType(totalFieldName[i]));
			if (displayableType != IS_NOT_DISPLAYABLE)
				visibleFieldList.add(totalFieldName[i]);
			}
		return visibleFieldList.toArray(new String[0]);
		}

	@Override
	public void runTask(Properties configuration) {
		int columnCount = Integer.parseInt(configuration.getProperty(PROPERTY_COLUMN_COUNT));

		String[] totalFieldName = mLoader.getFieldNames();
		String[] visibleFieldName = getVisibleFieldNames();

		int[] visibleDestColumn = new int[visibleFieldName.length];
		int[] visibleMergeMode = new int[visibleFieldName.length];

		boolean appendColumns = "true".equals(configuration.getProperty(PROPERTY_APPEND_COLUMNS));

		// the default is to trash all not defined columns unless PROPERTY_APPEND_COLUMNS is "true"
		for (int i=0; i<visibleFieldName.length; i++) {
			visibleDestColumn[i] = appendColumns ? CompoundTableLoader.NEW_COLUMN : CompoundTableLoader.NO_COLUMN;
			visibleMergeMode[i] = CompoundTableLoader.MERGE_MODE_APPEND;
			}

		for (int j=0; j<columnCount; j++) {
			String sourceColumn = configuration.getProperty(PROPERTY_SOURCE_COLUMN+j);
			for (int i=0; i<visibleFieldName.length; i++) {
				if (visibleFieldName[i].equals(sourceColumn)) {
					String destColumn = configuration.getProperty(PROPERTY_DEST_COLUMN+j);
					if (destColumn.equals(DEST_COLUMN_ADD))
						visibleDestColumn[i] = CompoundTableLoader.NEW_COLUMN;
					else if (destColumn.equals(DEST_COLUMN_TRASH))
						visibleDestColumn[i] = CompoundTableLoader.NO_COLUMN;
					else
						visibleDestColumn[i] = mTableModel.findColumn(destColumn);

					visibleMergeMode[i] = findListIndex(configuration.getProperty(PROPERTY_OPTION+j),
							OPTION_CODE, CompoundTableLoader.MERGE_MODE_APPEND);

					if ((visibleMergeMode[i] == CompoundTableLoader.MERGE_MODE_IS_KEY_NO_CASE
					  || visibleMergeMode[i] == CompoundTableLoader.MERGE_MODE_IS_KEY_WORD_SEARCH)
					 && mTableModel.getColumnSpecialType(visibleDestColumn[i]) != null)
						visibleMergeMode[i] = CompoundTableLoader.MERGE_MODE_IS_KEY;

					break;
					}
				}
			}

		int visIndex = 0;
		int[] destColumn = new int[totalFieldName.length];
		int[] mergeMode = new int[totalFieldName.length];
		for (int i=0; i<totalFieldName.length; i++) {
			String specialType = mLoader.getColumnSpecialType(totalFieldName[i]);
			int displayableType = getDisplayableType(specialType);
			if (displayableType == IS_NOT_DISPLAYABLE) {
				String parentColumn = mLoader.getParentColumnName(totalFieldName[i]);
				for (int j=0; j<visibleFieldName.length; j++) {
					if (visibleFieldName[j].equals(parentColumn)) {
						if (visibleDestColumn[j] == CompoundTableLoader.NEW_COLUMN
						 || visibleDestColumn[j] == CompoundTableLoader.NO_COLUMN) {
							destColumn[i] = visibleDestColumn[j];
							mergeMode[i] = CompoundTableLoader.MERGE_MODE_REPLACE;
							}
						else {
							int column = mTableModel.getChildColumn(visibleDestColumn[j], specialType);
							if (column != -1)
								destColumn[i] = column;
							else
								destColumn[i] = CompoundTableLoader.NEW_COLUMN;

							mergeMode[i] = visibleMergeMode[j];	// parent merge mode
							if (mergeMode[i] == CompoundTableLoader.MERGE_MODE_USE_IF_EMPTY)
								mergeMode[i] = CompoundTableLoader.MERGE_MODE_AS_PARENT;
							if (mergeMode[i] == CompoundTableLoader.MERGE_MODE_IS_KEY)
								mergeMode[i] = CompoundTableLoader.MERGE_MODE_USE_IF_EMPTY;
							}
						break;
						}
					}
				}
			else {
				mergeMode[i] = visibleMergeMode[visIndex];
				destColumn[i] = visibleDestColumn[visIndex++];
				}
			}

		boolean appendRows = "true".equals(configuration.getProperty(PROPERTY_APPEND_ROWS, "true"));
		mLoader.mergeFile(destColumn, mergeMode, appendRows);
		}
	}
