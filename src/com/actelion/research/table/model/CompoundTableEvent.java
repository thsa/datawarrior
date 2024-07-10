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

package com.actelion.research.table.model;

import java.util.EventObject;

public class CompoundTableEvent extends EventObject {
    private static final long serialVersionUID = 0x20060831;

	public static final int cNewTable = 1;
	public static final int cChangeColumnData = 2;	// change of column values, column type or log mode may also have changed, specifier may be row-ID
	public static final int cAddRows = 3;			// specifier is first new row
    public static final int cDeleteRows = 4;		// mapping is row mapping

	public static final int cAddColumns = 5;		// column index is index of first new column
	public static final int cRemoveColumns = 6;

	public static final int cChangeColumnName = 7;
	public static final int cAddColumnDetails = 8;	// mapping is detail mapping
	public static final int cRemoveColumnDetails = 9;	// mapping is detail mapping
	public static final int cChangeColumnDetailSource = 10;	// mapping[0] detail

	public static final int cChangeExcluded = 11;	// specifier is exclusionMask (exclusionMask evidently not used and removed TLS 28Jul2020)
	public static final int cChangeSelection = 12;	// This event is for ListSelectionModel only. Other components listen there
	public static final int cChangeSortOrder = 13;
	public static final int cChangeActiveRow = 14;

	public static final int cChangeColumnReference = 15;

	public static final int cChangeExtensionData = 21;	// the content data of one of the registered file extensions changed

	public static final int cSpecifierNoRuntimeProperties = 1;		// used as specifier if type = cNewTable
	public static final int cSpecifierDefaultFiltersAndViews = 2;	// used as specifier if type = cNewTable
	public static final int cSpecifierDefaultFilters = 3;			// used as specifier if type = cNewTable
	public static final int cSpecifierDefaultViews = 4; 			// used as specifier if type = cNewTable

	private int		mType,mColumn,mSpecifier,mOldCategoryCount;
	private int[]	mMapping;    // maps new to original columns/rows after column/row removal
	private int[]   mOldCategoryCounts;
	private boolean	mIsAdjusting;

	/**
	 * @param source
	 * @param type
	 * @param column absolute column index, or index of first column if multiple columns are concerned
	 */
    public CompoundTableEvent(Object source, int type, int column) {
		this(source, type, column, -1, -1, null, null);
	    }

	/**
	 * @param source
	 * @param type
	 * @param column absolute column index, or index of first column if multiple columns are concerned
	 * @param specifier special meaning in case of cNewTable,cAddRows,cChangeColumnData,cChangeExcluded,cChangeExtensionData
	 */
    public CompoundTableEvent(Object source, int type, int column, int specifier) {
		this(source, type, column, specifier, -1, null, null);
	    }

	/**
	 * @param source
	 * @param type
	 * @param column absolute column index, or index of first column if multiple columns are concerned
	 * @param specifier special meaning in case of cNewTable,cAddRows,cChangeColumnData,cChangeExcluded,cChangeExtensionData
	 * @param oldCategoryCount category count of column before the change happened; -1 if column didn't contain categories
	 */
	public CompoundTableEvent(Object source, int type, int column, int specifier, int oldCategoryCount) {
		this(source, type, column, specifier, oldCategoryCount, null, null);
		}

	public CompoundTableEvent(Object source, int type, int column, int specifier, int oldCategoryCount, int[] mapping, int[] oldCategoryCounts) {
		super(source);
		mType = type;
		mColumn = column;
		mSpecifier = specifier;
		mOldCategoryCount = oldCategoryCount;
		mMapping = mapping;
		mOldCategoryCounts = oldCategoryCounts;
	    }

	public CompoundTableEvent(Object source, int type, int specifier, boolean isAdjusting) {
		super(source);
		mType = type;
		mSpecifier = specifier;
		mIsAdjusting = isAdjusting;
	    }

	public int getType() {
		return mType;
		}

	public int getColumn() {
		return mColumn;
		}

	public int getSpecifier() {
		return mSpecifier;
		}

	public int[] getMapping() {
		return mMapping;
		}

	public int getOldCategoryCount(int column) {
		return mOldCategoryCounts == null ? mOldCategoryCount : mOldCategoryCounts[column];
	}

	public boolean isAdjusting() {
		return mIsAdjusting;
		}
	}