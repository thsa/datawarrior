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

import javax.swing.*;
import java.util.ArrayList;
import java.util.TreeSet;

public class CompoundTableListHandler {
	public static final int		EMPTY_LIST = 0;
	public static final int		ALL_IN_LIST = 1;
	public static final int		FROM_VISIBLE = 2;
	public static final int		FROM_HIDDEN = 3;
	public static final int		FROM_SELECTED = 4;
	public static final int		FROM_KEY_SET = 5;

	public static final int		OPERATION_AND = 0;
	public static final int		OPERATION_OR = 1;
	public static final int		OPERATION_XOR = 2;
	public static final int		OPERATION_NOT = 3;

    public static final int		LISTINDEX_NONE = -1;
    public static final int 	LISTINDEX_ANY = -2;
	public static final int 	LISTINDEX_SELECTION = -3;

    public static final String	LISTNAME_NONE = "<none>";
    public static final String	LISTNAME_ANY = "<any>";
	public static final String	LIST_CODE_SELECTION = "<selection>";	// list code used to store the selection in files
	public static final String	LIST_NAME_SELECTION = "<Row Selection>";// pseudo list name shown to the user

	public static final int		PSEUDO_COLUMN_SELECTION = -3;
	private static final int	PSEUDO_COLUMN_FIRST_LIST = -4;
	// a pseudo columnIndex = PSEUDO_COLUMN_FIRST_LIST - listIndex;

	private CompoundTableModel	mTableModel;
	private ArrayList<ListInfo> mListInfoList;
	private ArrayList<CompoundTableListListener> mListener;

	/**
	 * Negative column indexes, e.g. PSEUDO_COLUMN_SELECTION may refer to the selection, to list indexes,
	 * or to no column, rather than a real compound table column.
	 * @param column (pseudo) column index that refers to a column, selection, a list or nothing at all
	 * @return true if the column index refers to a list or to the selection
	 */
	public static boolean isListOrSelectionColumn(int column) {
			// certain negative column numbers actually refer to a list rather than a column
		return column == PSEUDO_COLUMN_SELECTION || isListColumn(column);
		}

	/**
	 * Negative column indexes, e.g. PSEUDO_COLUMN_SELECTION may refer to the selection, to list indexes,
	 * or to no column, rather than a real compound table column.
	 * @param column (pseudo) column index that refers to a column, selection, a list or nothing at all
	 * @return true if the column index refers to a list
	 */
	public static boolean isListColumn(int column) {
		// certain negative column numbers actually refer to a list rather than a column
		return (column <= PSEUDO_COLUMN_FIRST_LIST);
		}

	public static int convertToListIndex(int column) {
		return (column == PSEUDO_COLUMN_SELECTION) ? LISTINDEX_SELECTION : isListColumn(column) ? PSEUDO_COLUMN_FIRST_LIST - column : LISTINDEX_NONE;
		}

	public static int getColumnFromList(int list) {
		return (list == LISTINDEX_NONE) ? -1 : PSEUDO_COLUMN_FIRST_LIST - list;
		}

	public CompoundTableListHandler(CompoundTableModel tableModel) {
		mTableModel = tableModel;
		mListInfoList = new ArrayList<ListInfo>();
		mListener = new ArrayList<CompoundTableListListener>();
		}

	public void addCompoundTableListListener(CompoundTableListListener l) {
		mListener.add(l);
		}

	public void removeCompoundTableListListener(CompoundTableListListener l) {
		mListener.remove(l);
		}

	public int getListCount() {
		return mListInfoList.size();
		}

	/**
	 * @param listIndex valid list index, LISTNAME_NONE, LISTNAME_ANY, or LIST_NAME_SELECTION
	 * @return list name from listIndex inlcuding pseudo list names
	 */
	public String getListName(int listIndex) {
		return listIndex == LISTINDEX_NONE ? LISTNAME_NONE
		 	 : listIndex == LISTINDEX_ANY ? LISTNAME_ANY
		 	 : listIndex == LISTINDEX_SELECTION ? LIST_NAME_SELECTION
			 : mListInfoList.get(listIndex).name;
		}

	/**
	 * @param column pseudo column no
	 * @return list index from pseudo column, LISTINDEX_SELECTION, or LISTINDEX_NONE, if list doesn't exist
	 */
	public int getListIndex(int column) {
		int index = convertToListIndex(column);
		return index != LISTINDEX_SELECTION && (index < 0 || index >= mListInfoList.size()) ? LISTINDEX_NONE : index;
		}

	public int getListIndex(String name) {
		for (int i = 0; i< mListInfoList.size(); i++)
			if (mListInfoList.get(i).name.equals(name))
				return i;

		if (LIST_NAME_SELECTION.equals(name))
			return LISTINDEX_SELECTION;

		return LISTINDEX_NONE;
		}

    public int getListFlagNo(String name) {
        for (ListInfo info: mListInfoList)
            if (info.name.equals(name))
                return info.flagNo;

        if (LIST_NAME_SELECTION.equals(name))
        	return CompoundRecord.cFlagSelected;

        return 0;
        }

    /**
     * Returns the compound record flag that is associated with this list.
     * @param index valid list index (>= 0) or LISTINDEX_SELECTION
     * @return flagNo
     */
    public int getListFlagNo(int index) {
        return (index == LISTINDEX_SELECTION) ? CompoundRecord.cFlagSelected : index < 0 ? -1 : mListInfoList.get(index).flagNo;
        }

    /**
     * Creates a mask containing one all flags of those lists specified in index
     * @param index list index or LISTINDEX_NONE, LISTINDEX_SELECTION or LISTINDEX_ANY
     * @return mask
     */
    public long getListMask(int index) {
        if (index == LISTINDEX_NONE)
            return 0;
		if (index == LISTINDEX_SELECTION)
			return CompoundRecord.cFlagMaskSelected;
        if (index == LISTINDEX_ANY) {
            long mask = 0;
            for (int i = 0; i< mListInfoList.size(); i++)
                mask |= (1L << mListInfoList.get(i).flagNo);
            return mask;
            }
		return (1L << mListInfoList.get(index).flagNo);
		}


	/**
	 * Creates an empty list with the given name. If the name is already taken, a modified name is used and returned
	 * @param name
	 * @return unique list name which may differ from the intended 'name'
	 */
	public String createList(String name) {
		return createList(name, -1, EMPTY_LIST, -1, null,false);
		}

	public String createList(String name, int extentionColumn, int source, int keyColumn, TreeSet<String> keySet) {
		return createList(name,extentionColumn,source,keyColumn,keySet,false);
		}

    /**
     * Create a new list that is either empty or contains rows defined in source.
     * If source is FROM_KEY_SET, then keyColumn and keySet define which column contains
     * the keys that need to match those in keySet for a row being considered a list member.
     * One may specify a category column to enlarge the initial list such that
     * if at least one record of a category belongs to the initial list, then
     * all records of this category will added to the list.
     * @param name intended name for new list
     * @param extentionColumn -1 or category column for list completion
     * @param source EMPTY_LIST,FROM_VISIBLE,FROM_HIDDEN, FROM_SELECTED or FROM_KEY_SET
     * @param keyColumn the column in which to look for keys that make a row a list member
     * @param keySet the set of keys, which if present makes a row a list member
	 * @param keySetIsLowerCase if true, then keySet entries were converted to lower case for case insensitive matching
     * @return unique list name which may differ from the intended 'name'
     */
	public String createList(String name, int extentionColumn, int source, int keyColumn, TreeSet<String> keySet, boolean keySetIsLowerCase) {
		int flagNo = mTableModel.getUnusedRowFlag(false);
		if (flagNo == -1)
			return null;

		name = getUniqueName(name);

		mListInfoList.add(new ListInfo(name, flagNo));

		if (source == ALL_IN_LIST) {
            for (int row=0; row<mTableModel.getTotalRowCount(); row++)
				mTableModel.getTotalRecord(row).setFlag(flagNo);
			}
		else if (source != EMPTY_LIST) {
            if (extentionColumn == -1) {
                for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
    				CompoundRecord record = mTableModel.getTotalRecord(row);
    				switch (source) {
    				case FROM_VISIBLE:
    					if (mTableModel.isVisible(record))
    						record.setFlag(flagNo);
    					break;
    				case FROM_HIDDEN:
                        if (!mTableModel.isVisible(record))
                            record.setFlag(flagNo);
    					break;
    				case FROM_SELECTED:
                        if (mTableModel.isVisibleAndSelected(record))
                            record.setFlag(flagNo);
    					break;
    				case FROM_KEY_SET:
                        String[] items = mTableModel.separateEntries(mTableModel.encodeData(record, keyColumn));
                        for (String item:items) {
                        	if (keySetIsLowerCase)
                        		item = item.toLowerCase();
                        	if (keySet.contains(item)) {
	                            record.setFlag(flagNo);
	                            break;
                        		}
                        	}
    					break;
    					}
    				}
                }
            else {
                TreeSet<String> categorySet = new TreeSet<String>();
                for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
                    CompoundRecord record = mTableModel.getTotalRecord(row);
                    String[] entry = null; 
                    switch (source) {
                    case FROM_VISIBLE:
                        if (mTableModel.isVisible(record))
                            entry = mTableModel.separateEntries(mTableModel.encodeData(record, extentionColumn));
                        break;
                    case FROM_HIDDEN:
                        if (mTableModel.isVisible(record))
                            entry = mTableModel.separateEntries(mTableModel.encodeData(record, extentionColumn));
                        break;
                    case FROM_SELECTED:
                        if (mTableModel.isVisibleAndSelected(record))
                            entry = mTableModel.separateEntries(mTableModel.encodeData(record, extentionColumn));
                        break;
    				case FROM_KEY_SET:
                        String[] items = mTableModel.separateEntries(mTableModel.encodeData(record, keyColumn));
                        for (String item:items) {
							if (keySetIsLowerCase)
								item = item.toLowerCase();
                        	if (keySet.contains(item)) {
                                entry = mTableModel.separateEntries(mTableModel.encodeData(record, extentionColumn));
	                            break;
                        		}
                        	}
    					break;
                        }
                    if (entry != null)
                        for (int i=0; i<entry.length; i++)
                            categorySet.add(entry[i]);
                    }
                for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
                    CompoundRecord record = mTableModel.getTotalRecord(row);
                    String[] entry = mTableModel.separateEntries(mTableModel.encodeData(record, extentionColumn));
                    for (int i=0; i<entry.length; i++)
                        if (categorySet.contains(entry[i]))
                            record.setFlag(flagNo);
                    }
                }
			}

        mTableModel.setRowFlagToDirty(flagNo);

        fireEvents(new CompoundTableListEvent(this, CompoundTableListEvent.cAdd, mListInfoList.size()-1));
		return name;
		}

    /**
     * Creates a new list based on a boolean operation on existing lists.
     * @param name intended name for new list
     * @param list1
     * @param list2
     * @param operation OPERATION_AND,OPERATION_OR,OPERATION_XOR or OPERATION_NOT
     * @return unique list name which may differ from the intended 'name'
     */
	public String createList(String name, int list1, int list2, int operation) {
		int flagNo = mTableModel.getUnusedRowFlag(false);
		if (flagNo == -1)
			return null;

		name = getUniqueName(name);

		mListInfoList.add(new ListInfo(name, flagNo));

		long mask1 = getListMask(list1);
        long mask2 = getListMask(list2);
        long maskBoth = (mask1 | mask2);

		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			CompoundRecord record = mTableModel.getTotalRecord(row);
			long availableFlags = (record.mFlags & maskBoth);
			switch (operation) {
			case OPERATION_AND:
				if (availableFlags == maskBoth)
					record.setFlag(flagNo);
				break;
			case OPERATION_OR:
				if (availableFlags != 0)
                    record.setFlag(flagNo);
				break;
			case OPERATION_XOR:
				if (availableFlags == mask1 || availableFlags == mask2)
                    record.setFlag(flagNo);
				break;
			case OPERATION_NOT:
				if (availableFlags == mask1)
                    record.setFlag(flagNo);
				break;
				}
			}

        mTableModel.setRowFlagToDirty(flagNo);
        
		fireEvents(new CompoundTableListEvent(this, CompoundTableListEvent.cAdd, mListInfoList.size()-1));
		return name;
		}

    /**
     * Add one individual record to a list without firing any events
     * @param record
     * @param flagNo
     */
    public void addRecordSilent(CompoundRecord record, int flagNo) {
        record.setFlag(flagNo);
        }

    /**
     * Add one individual record to a list and fire change events
     * @param record
     * @param list
     */
    public void addRecord(CompoundRecord record, int list) {
	    int flagNo = getListFlagNo(list);
	    if (!record.isFlagSet(flagNo)) {
	    	record.setFlag(flagNo);

	    	mTableModel.setRowFlagToDirty(flagNo);

	        fireEvents(new CompoundTableListEvent(this, CompoundTableListEvent.cChange, list));
			}
		}
	
    /**
     * Add one individual record to a list and fire change events
     * @param record
     * @param list
     */
    public void removeRecord(CompoundRecord record, int list) {
	    int flagNo = getListFlagNo(list);
	    if (record.isFlagSet(flagNo)) {
	    	record.clearFlag(flagNo);

	    	mTableModel.setRowFlagToDirty(flagNo);

	    	fireEvents(new CompoundTableListEvent(this, CompoundTableListEvent.cChange, list));
			}
		}
	
    /**
     * add all selected and visible records to the given list
     * @param list
     */
    public void addSelected(int list) {
	    int flagNo = getListFlagNo(list);
		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			CompoundRecord record = mTableModel.getTotalRecord(row);
			if (mTableModel.isVisibleAndSelected(record))
				record.setFlag(flagNo);
			}

        mTableModel.setRowFlagToDirty(flagNo);

        fireEvents(new CompoundTableListEvent(this, CompoundTableListEvent.cChange, list));
		}
	
	public void removeSelected(int list) {
	    int flagNo = getListFlagNo(list);
		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			CompoundRecord record = mTableModel.getTotalRecord(row);
            if (mTableModel.isVisibleAndSelected(record))
				record.clearFlag(flagNo);
			}

        mTableModel.setRowFlagToDirty(flagNo);

        fireEvents(new CompoundTableListEvent(this, CompoundTableListEvent.cChange, list));
		}

	public void deleteList(String name) {
		int index = indexOf(name);
		if (index != -1) {
			ListInfo info = mListInfoList.remove(index);
			mTableModel.freeRowFlag(info.flagNo);

			fireEvents(new CompoundTableListEvent(this, CompoundTableListEvent.cDelete, index));
			}
		}

	public String[] getListNames() {
		if (mListInfoList.isEmpty())
			return null;

		String[] name = new String[mListInfoList.size()];
		for (int i = 0; i< mListInfoList.size(); i++)
			name[i] = getListName(i);

		return name;
		}

	protected void clearListData() {
			// only to be called from CompoundTableModel on initializeTable()
		mListInfoList.clear();
		}

	public String getUniqueName(String name) {
		while (name.equals(LISTNAME_NONE)
            || name.equals(LISTNAME_ANY)
			|| name.equals(LIST_NAME_SELECTION)
            || indexOf(name) != -1) {
			int i = name.lastIndexOf('_');
			if (i == -1)
				name = name.concat("_2");
			else {
				try {
					int no = Integer.parseInt(name.substring(i+1));
					name = name.substring(0, i).concat("_"+(no+1));
					}
				catch (NumberFormatException e) {
					name = name.concat("_2");
					}
				}
			}

		return name;
		}

    private void fireEvents(final CompoundTableListEvent e) {
        if (SwingUtilities.isEventDispatchThread()) {
			for (CompoundTableListListener l:mListener)
				l.listChanged(e);
        	}
        else {
        	try {
	        	SwingUtilities.invokeAndWait(new Runnable() {
	        		public void run() {
	        			for (CompoundTableListListener l:mListener)
	        				l.listChanged(e);
	        			}
	        		} );
        		}
            catch (Exception ee) {
                ee.printStackTrace();
                }
        	}
		}

	private int indexOf(String name) {
		for (int i = 0; i< mListInfoList.size(); i++) {
			if (mListInfoList.get(i).name.equals(name))
				return i;
			}
		return -1;
		}
	}


class ListInfo {
	public String	name;
	public int 		flagNo;

    public ListInfo(String name, int flagNo) {
		this.name = name;
		this.flagNo = flagNo;
    	}
	}
