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

package com.actelion.research.table;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class TableRowHeightAdjuster implements MouseListener,MouseMotionListener {
	private static final int cResizeTolerance = 2;

	private JTable				mTable;
	private Cursor				mResizeCursor,mDefaultCursor;
	private boolean				mIsResizing;
	private int					mResizingRowHeight;
	private int					mDragStartY,mDragStartRow,mDragStartRowHeight;

	public TableRowHeightAdjuster(JTable table) {
		mTable = table;

		mTable.addMouseListener(this);
		mTable.addMouseMotionListener(this);

		mDefaultCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
		mResizeCursor = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
		}

	public void mouseClicked(MouseEvent e) {}

	public void mousePressed(MouseEvent e) {
		mIsResizing = (mTable.getCursor() == mResizeCursor);
		if (mIsResizing) {
			Point p = e.getPoint();
			p.y -= cResizeTolerance;
			int row = mTable.rowAtPoint(p);
//			mResizingRowY = mTable.getCellRect(row, 0, false).y-1;
			mDragStartY = e.getY();
			mDragStartRow = row;
			mDragStartRowHeight = mTable.getRowHeight(row);
			}
		}

	public void mouseReleased(MouseEvent e) {
		mIsResizing = false;
		}

	public void mouseEntered(MouseEvent e) {}

	public void mouseExited(MouseEvent e) {
		if (!mIsResizing)
			mTable.setCursor(mDefaultCursor);
		}

	public void mouseMoved(MouseEvent e) {
		int row = mTable.rowAtPoint(e.getPoint());
		Rectangle cellRect = mTable.getCellRect(row, 0, false);
		int y = e.getY() - cellRect.y;
		if ((y < cResizeTolerance && row != 0) || (y > cellRect.height-cResizeTolerance) && row != mTable.getRowCount()-1)
			mTable.setCursor(mResizeCursor);
		else
			mTable.setCursor(mDefaultCursor);
		}

	public void mouseDragged(MouseEvent e) {
		if (mIsResizing) {
			mResizingRowHeight = Math.max(16, mDragStartRowHeight + e.getY() - mDragStartY);

			mTable.setRowHeight(mDragStartRow, mResizingRowHeight);
			}
		}
	}
