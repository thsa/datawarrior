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

package com.actelion.research.datawarrior.task.view;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

/**
  * ListTransferHandler is based on 1.4 ExtendedDnDDemo.java example
  * and is modified to support drop mode INSERT rather than SELECTION.
  * The underlying JList may have any selection mode.
  * When dragging single/multiple list entries within the list, they are <b>moved</b>.
  * When dragging single/multiple Strings from an external source onto a JList, they are <b>inserted</b>.
  * Multiple String entries from an external source are expected to be passed as one /n-delimited String.
  * After the d&d operation the dragged items are selected.
  */
public class ListTransferHandler extends StringTransferHandler {
	private static final long serialVersionUID = 0x20130318;

	private int[] mSourceIndex = null;
	private int mAddIndex = -1; // Location where items were added
	private int mAddCount = 0;	// Number of items added.

	/**
	 * This is called, if the JList is the d&d source. If the remove argument is true,
	 * Bundle up the selected items in the list as a single string, for export.
	 */
	@Override
	protected String exportString(JList list) {
		mSourceIndex = list.getSelectedIndices();
		List<String> values = list.getSelectedValuesList();

		StringBuilder buff = new StringBuilder();

		for (int i = 0; i < values.size(); i++) {
			String val = values.get(i);
			buff.append(val == null ? "" : val);
			if (i != values.size() - 1) {
				buff.append("\n");
				}
			}

		return buff.toString();
		}

	/**
	 * This is called, if the JList is the d&d target.
	 * Individual items are expected to be delimited by a '\n'.
	 */
	@Override
	protected void importString(JList list, String str) {
		DefaultListModel model = (DefaultListModel)list.getModel();
		int index = (list.getDropLocation() == null) ? model.getSize(): list.getDropLocation().getIndex();

		mAddIndex = index;
		String[] values = str.split("\\n");
		mAddCount = values.length;
		for (int i = 0; i < values.length; i++) {
			model.add(index++, values[i]);
			}
		list.setSelectionInterval(mAddIndex, mAddIndex+mAddCount-1);

		if (mSourceIndex == null) {	// we have dropped something from an external source
			int[] map = new int[model.getSize()];
			for (int i=0; i<map.length; i++)
				map[i] = (i<mAddIndex) ? i : (i<mAddIndex+values.length) ? -1 : i-values.length;
			updateListIndexes(map);
			}
		}

	/**
	 * This is called, if the JList is the d&d source. If the remove argument is true,
	 * the drop has been successful and it's time to remove the selected items
	 * from the list. If the remove argument is false, it was a COPY operation
	 * and the original list is left intact.
	 */
	@Override
	protected void cleanup(JList list, boolean remove) {
		DefaultListModel model = (DefaultListModel)list.getModel();
		if (remove && mSourceIndex != null) {
			//If we are moving items around in the same list.

			// create map of old list indexes
			int[] map = new int[model.getSize() - mAddCount];
			boolean[] used = new boolean[map.length];
			int addIndex = mAddIndex;	// new addIndex after removing source objects 
			for (int i=0; i<mSourceIndex.length; i++) {
				if (mSourceIndex[i] < mAddIndex)
					addIndex--;
				}
			for (int i=0; i<mSourceIndex.length; i++) {
				map[addIndex+i] = mSourceIndex[i];
				used[mSourceIndex[i]] = true;
				}
			int index = 0;
			for (int i=0; i<map.length; i++) {
				if (!used[i]) {
					if (index == addIndex)
						index += mAddCount;
					map[index++] = i;
					}
				}

			//We need to adjust mSourceIndex accordingly, since those
			//after the insertion point have moved.
			if (mAddCount > 0) {	// JList is both, d&d source and target
				for (int i = 0; i < mSourceIndex.length; i++) {
					if (mSourceIndex[i] >= mAddIndex) {
						mSourceIndex[i] += mAddCount;
						}
					}
				}
			for (int i = mSourceIndex.length - 1; i >= 0; i--) {
				model.remove(mSourceIndex[i]);
				}

			updateListIndexes(map);
			}

		mSourceIndex = null;
		mAddCount = 0;
		mAddIndex = -1;
		}

	/**
	 * Override this method if you need to track list item positions
	 * when the list is changing.
	 * @param newToOldListIndexMap item index before list change, -1: new item
	 */
	public void updateListIndexes(int[] newToOldListIndexMap) {
		}
	}

abstract class StringTransferHandler extends TransferHandler {
	private static final long serialVersionUID = 0x20130315;

	protected abstract String exportString(JList list);
	protected abstract void importString(JList list, String str);
	protected abstract void cleanup(JList list, boolean remove);

	protected Transferable createTransferable(JComponent c) {
		return new StringSelection(exportString((JList)c));
		}

	public int getSourceActions(JComponent c) {
		return COPY_OR_MOVE;
		}

	public boolean importData(TransferSupport support) {
		if (canImport(support)) {
			try {
				String str = getStringData(support);
				importString((JList)support.getComponent(), str);
				return true;
				}
			catch (UnsupportedFlavorException ufe) {}
			catch (IOException ioe) {}
			}
		return false;
		}

	/**
	 * Returns the String provided by the support. 
	 * Tries for predefined DataFlavor.stringFlavor
	 * and flavors with a representationClass of String.			
	 */
	public String getStringData(TransferSupport support) throws UnsupportedFlavorException, IOException {
		if (support.isDataFlavorSupported(DataFlavor.stringFlavor))
			return (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);

		DataFlavor[] flavors = support.getDataFlavors();
		for (DataFlavor dataFlavor : flavors)
			if (dataFlavor.getRepresentationClass() == String.class)
				return (String)support.getTransferable().getTransferData(dataFlavor);

		return "";
		}

	protected void exportDone(JComponent c, Transferable data, int action) {
		cleanup((JList)c, action == MOVE);
		}

	@Override
	public boolean canImport(TransferSupport support) {
		if (!support.isDrop())
			return false;

		return isStringDataSupported(support);
		}

	/**
	 * Returns a boolean indicating whether or not the support can
	 * provide a string value. Checks for predefined DataFlavor.stringFlavor
	 * and flavors with a representationClass of String.
	 */
	protected boolean isStringDataSupported(TransferSupport support) {
		if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) return true;
		DataFlavor[] flavors = support.getDataFlavors();
		for (DataFlavor dataFlavor : flavors)
			if (dataFlavor.getRepresentationClass() == String.class) return true;

		return false;
		}
	}
