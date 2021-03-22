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

import com.actelion.research.chem.io.CompoundTableConstants;

public class CompoundRecord {
    public static final int cFlagSelected = 0;
    private static final int cFlagDeleted = 1;
	protected static final int cFlagFirstUnusedFlagNo = 2;
	protected static final int cFlagLastUnusedFlagNo = 63;

    public static final long cFlagMaskSelected = (1L << cFlagSelected);
    public static final long cFlagMaskDeleted = (1L << cFlagDeleted);

	private Object[]		mData;
	protected String[][][]	mDetailReference;	// [column][detailIndex][count]
	protected float[]		mFloat;
	protected long			mFlags;
	protected int			mOriginalIndex;

/*	protected CompoundRecord(int index, String[] data) {
		mData = new Object[data.length];
		for (int i=0; i<data.length; i++)
		    if (data[i] != null && data[i].length() != 0)
		        mData[i] = data[i].getBytes();
		mOriginalIndex = index;
		mDouble = new float[data.length];
		}
*/
/*	protected CompoundRecord(int index, Object[] data) {
		mData = data;
		mOriginalIndex = index;
		mFloat = new float[data.length];
		}*/

	protected CompoundRecord(int index, int columns) {
		mOriginalIndex = index;
        if (columns != 0) {
            mData = new Object[columns];
            mFloat = new float[columns];
            }
		}

	protected CompoundRecord(CompoundRecord record, int index) {
		mOriginalIndex = index;
		mFlags = record.mFlags;
		int columns = record.mData == null ? 0 : record.mData.length;
		if (columns != 0) {
			mData = new Object[columns];
			if (record.mDetailReference != null)
				mDetailReference = new String[record.mDetailReference.length][][];
			for (int c=0; c<columns; c++) {
				mData[c] = (record.mData[c] instanceof byte[]) ? ((byte[]) record.mData[c]).clone()
						: (record.mData[c] instanceof int[]) ? ((int[]) record.mData[c]).clone()
						: (record.mData[c] instanceof long[]) ? ((long[]) record.mData[c]).clone() : null;

				if (record.mDetailReference != null && record.mDetailReference[c] != null) {
					mDetailReference[c] = new String[record.mDetailReference[c].length][];
					for (int i=0; i<record.mDetailReference[c].length; i++) {
						if (record.mDetailReference[c][i] != null) {
							mDetailReference[c][i] = new String[record.mDetailReference[c][i].length];
							for (int j=0; j<record.mDetailReference[c][i].length; j++)
								mDetailReference[c][i][j] = record.mDetailReference[c][i][j];
							}
						}
					}
				}

			mFloat = new float[columns];
			}
		}

		/**
		 * Returns a uniquely identifying integer of this record, which is immune to sorting.
		 * IDs are reassigned whenever some records are deleted. The CompoundTableEvent sent
		 * after deletion contains the mapping from old to new IDs. It is guaranteed that all
		 * integers from 0 to the totalRecordCount-1 are used exactly once.
		 * @return
		 */
	public int getID() {
		return mOriginalIndex;
		}

	public Object getData(int column) {
		return (column == -1) ? null : mData[column];
		}

    /**
     * Requires a call to the respective CompoundTableModel.finalizeDeletion()
     * after all records are marked.
     */
    public void markForDeletion() {
        mFlags |= cFlagMaskDeleted;
        }

    public void setSelection(boolean value) {
        if (value)
            mFlags |= CompoundRecord.cFlagMaskSelected;
        else
            mFlags &= ~CompoundRecord.cFlagMaskSelected;
        }

    public boolean isSelected() {
        return (mFlags & CompoundRecord.cFlagMaskSelected) != 0;
        }

    /**
     * @param flagNo >= 0
     * @return whether the specified flag is set or not
     */
    public boolean isFlagSet(int flagNo) {
        return (mFlags & (1L << flagNo)) != 0;
        }

    /**
     * @return all set flags as a mask
     */
    public long getFlags() {
        return mFlags;
		}
	
	public String[][] getDetailReferences(int column) {
		return (mDetailReference == null) ? null : mDetailReference[column];
		}

	/**
	 * Get the pre-processed double representation of this cell.
	 * If the column is not numerical, the return value is undefined.
	 * @param column
	 * @return valid double value or NaN if cell is empty
	 */
	public float getDouble(int column) {
		return mFloat[column];
		}

	public void setDetailReferences(int column, String[][] detailReferences) {
		if (mDetailReference == null && detailReferences != null)
			mDetailReference = new String[mData.length][][];
		if (mDetailReference != null)
			mDetailReference[column] = detailReferences;
		}

	public void allocateColumn() {
        if (mData == null)
            mData = new Object[1];
        else {
            Object[] newData = new Object[mData.length+1];
            for (int i=0; i<mData.length; i++)
                newData[i] = mData[i];
            mData = newData;
            }
        }

	public void setData(Object value, int column) {
	    setData(value, column, false);
		}

    public void setData(Object value, int column, boolean keepDetailReferences) {
        mData[column] = value;
        if (mDetailReference != null && !keepDetailReferences)
            mDetailReference[column] = null;
        }

    /**
     * Appends new cell content to an existing cell. If the value contains
     * detail data at its end, then separateDetail() should be called later.
     * @param value
     * @param column
     */
    public void appendData(byte[] value, int column) {
    	if (mData[column] == null)
    		mData[column] = value;
    	else if (value != null) {
    		byte[] old = (byte[])mData[column];
    		byte[] merged = new byte[old.length+1+value.length];
    		int i = 0;
    		for (byte b:old)
    			merged[i++] = b;
			merged[i++] = CompoundTableConstants.cLineSeparatorByte;
    		for (byte b:value)
    			merged[i++] = b;
    		mData[column] = merged;
    		}
        }

    /**
     * Moves detail references from the end of the data part of this column's cells
     * to the respective cell's detail part, while retaining existing detail references.
     * A data byte sequence with detail may look like: "blabla|#|0:22|#|1:-139|#|3:image4.jpg".
     * For this case three detail references are appended to the existing detail list.
     * The integers 22 and -139 refer to embedded details (details with negative references are
     * zipped) while image4.jpg refers to en external file. The corresponding detail section
     * in the DataWarrior file may look like this:
		<columnProperty="detailCount	4">
		<columnProperty="detailSource0	embedded">
		<columnProperty="detailType0	image/jpeg">
		<columnProperty="detailName0	Picture">
		<columnProperty="detailSource1	embedded">
		<columnProperty="detailType1	text/html">
		<columnProperty="detailName1	Description">
		...
		<columnProperty="detailSource3	relPath:images/">
		<columnProperty="detailType3	image/jpeg">
		<columnProperty="detailName3	Picture">
     * @param separator cDefaultDetailSeparator ("|#|") if not otherwise defined with cColumnPropertyDetailSeparator
     * @param column 
     */
	public void separateDetail(String separator, int column) {
	    if (mData[column] == null)
	        return;

	    String data = new String((byte[])mData[column]);
		int firstSeparatorIndex = data.indexOf(separator);
		if (firstSeparatorIndex == -1)
		    return;

		int maxIndex = -1;
		int separatorIndex = firstSeparatorIndex;
		while (separatorIndex != -1) {
			int colonIndex = data.indexOf(CompoundTableModel.cDetailIndexSeparator, separatorIndex+separator.length());
			try {
				int position = Integer.parseInt(data.substring(separatorIndex+separator.length(), colonIndex));
				if (maxIndex < position)
					maxIndex = position;
				}
			catch (Exception e) {}
			separatorIndex = data.indexOf(separator, separatorIndex+separator.length());
			}

		if (maxIndex != -1) {
			if (mDetailReference == null)
				mDetailReference = new String[mData.length][][];
			if (mDetailReference[column] == null)
				mDetailReference[column] = new String[maxIndex+1][];
			else if (mDetailReference[column].length <= maxIndex) {
				String[][] oldDetail = mDetailReference[column];
				mDetailReference[column] = new String[maxIndex+1][];
				for (int i=0; i<oldDetail.length; i++)
					mDetailReference[column][i] = oldDetail[i];
				}

			separatorIndex = firstSeparatorIndex;
			while (separatorIndex != -1) {
				int colonIndex = data.indexOf(CompoundTableModel.cDetailIndexSeparator,
													   separatorIndex+separator.length());
				int nextSeparatorIndex = data.indexOf(separator, separatorIndex+separator.length());
				try {
					int position = Integer.parseInt(data.substring(separatorIndex+separator.length(), colonIndex));
					int referenceStart = colonIndex+CompoundTableModel.cDetailIndexSeparator.length();
					String reference = (nextSeparatorIndex == -1) ?
					        				data.substring(referenceStart)
										  : data.substring(referenceStart, nextSeparatorIndex);
					if (mDetailReference[column][position] == null) {
						mDetailReference[column][position] = new String[1];
						mDetailReference[column][position][0] = reference;
						}
					else {
						String[] oldReferenceList = mDetailReference[column][position];
						String[] newReferenceList = new String[oldReferenceList.length+1];
						for (int i=0; i<oldReferenceList.length; i++)
							newReferenceList[i] = oldReferenceList[i];
						newReferenceList[oldReferenceList.length] = reference;
						mDetailReference[column][position] = newReferenceList;
						}
					}
				catch (NumberFormatException nfe) {}
				separatorIndex = nextSeparatorIndex;
				}
			}

		mData[column] = data.substring(0, firstSeparatorIndex).getBytes();
		}

	public void addColumns(int no) {
		int currentColumnCount = mData.length;
		Object[] newData = new Object[currentColumnCount+no];
		float[] newFloat = new float[currentColumnCount+no];
		String[][][] newDetailReference = (mDetailReference == null) ? null : new String[currentColumnCount+no][][];
		for (int i=0; i<currentColumnCount; i++) {
			newData[i] = mData[i];
			newFloat[i] = mFloat[i];
			if (newDetailReference != null)
				newDetailReference[i] = mDetailReference[i];
			}
		mData = newData;
		mFloat = newFloat;
		mDetailReference = newDetailReference;
		}

	public void removeColumns(boolean[] removeColumn, int removalCount) {
		int currentColumnCount = mData.length;
		int newColumnCount = mData.length - removalCount;
		Object[] newData = new Object[newColumnCount];
		float[] newFloat = new float[newColumnCount];
		String[][][] newDetailReference = (mDetailReference == null) ? null : new String[newColumnCount][][];
		int newIndex = 0;
		boolean detailFound = false;
		for (int i=0; i<currentColumnCount; i++) {
			if (!removeColumn[i]) {
				newData[newIndex] = mData[i];
				newFloat[newIndex] = mFloat[i];
				if (newDetailReference != null && mDetailReference[i] != null) {
					newDetailReference[newIndex] = mDetailReference[i];
					detailFound = true;
					}
				newIndex++;
				}
			}
		mData = newData;
		mFloat = newFloat;
		mDetailReference = (detailFound) ? newDetailReference : null;
		}

    /**
     * sets the flag specified by the flag number (>= 0)
     * @param flagNo
     */
    public void setFlag(int flagNo) {
        mFlags |= (1L << flagNo);
        }

    /**
     * sets the flag specified by the flag number (>= 0)
     * @param flagNo
     */
    public void clearFlag(int flagNo) {
        mFlags &= ~(1L << flagNo);
        }

    /**
     * sets all flags contained in the mask
     * @param mask
     */
	public void setFlags(long mask) {
		mFlags |= mask;
		}

	/**
     * clears all flags contained in the mask
     * @param mask
     */
	public void clearFlags(long mask) {
		mFlags &= ~mask;
		}
	}
