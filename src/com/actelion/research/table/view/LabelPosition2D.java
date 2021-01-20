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

package com.actelion.research.table.view;

import com.actelion.research.util.DoubleFormat;

import java.io.BufferedWriter;
import java.io.IOException;

public class LabelPosition2D {
	private float mX,mY;
	private int mColumn,mScreenX1,mScreenX2,mScreenY1,mScreenY2;    // screen coords include the retina factor
	private LabelPosition2D mNextInChain;
	private boolean mIsCustom;

	/**
	 * Creates a new non-custom VisualizationLabelPosition
	 * @param column
	 * @param nextInChain
	 */
	public LabelPosition2D(int column, LabelPosition2D nextInChain) {
		mColumn = column;
		mNextInChain = nextInChain;
	}

	public LabelPosition2D getNext() {
		return mNextInChain;
	}

	public void skipNext() {
		mNextInChain = mNextInChain.mNextInChain;
	}

	public boolean isCustom() {
		return mIsCustom;
	}

	public int getColumn() {
		return mColumn;
	}

	public void setColumn(int column) {
		mColumn = column;
	}

	public float getX() {
		return mX;
	}

	public float getY() {
		return mY;
	}

	/**
	 * This sets a new position for this label.
	 * x,y are data values in the column data range assigned to the axes.
	 * If you also setCustom(true), then the label position is locked.
	 * @param x relative x (0...1) in unzoomed view
	 * @param y relative y (0...1) in unzoomed view
	 */
	public void setXY(float x, float y) {
		mX = x;
		mY = y;
	}

	/**
	 * @return screen x-coord of label center without retina and AA factors
	 */
	public int getLabelCenterOnScreenX() {
		return (mScreenX1 + mScreenX2) / 2;
	}

	/**
	 * @return screen y-coord of label center without retina and AA factors
	 */
	public int getLabelCenterOnScreenY() {
		return (mScreenY1 + mScreenY2) / 2;
	}

	/**
	 * @return screen x-coord of left edge of label without retina and AA factors
	 */
	public int getScreenX1() {
		return mScreenX1;
		}

	/**
	 * @return screen y-coord of top edge of label without retina and AA factors
	 */
	public int getScreenY1() {
		return mScreenY1;
		}

	/**
	 * @return screen x-coord of right edge of label without retina and AA factors
	 */
	public int getScreenX2() {
		return mScreenX2;
		}

	/**
	 * @return screen y-coord of bottom edge of label without retina and AA factors
	 */
	public int getScreenY2() {
		return mScreenY2;
		}

	/**
	 * @return screen width of label without retina and AA factors
	 */
	public int getScreenWidth() {
		return mScreenX2 - mScreenX1;
		}

	/**
	 * @return screen height label without retina and AA factors
	 */
	public int getScreenHeight() {
		return mScreenY2 - mScreenY1;
		}

	/**
	 * @param isCustom
	 */
	public void setCustom(boolean isCustom) {
		mIsCustom = isCustom;
	}

	/**
	 * Translates this position by dx and dy on a scale without retina and AA factors
	 * @param dx
	 * @param dy
	 */
	public void translate(int dx, int dy) {
		mScreenX1 += dx;
		mScreenY1 += dy;
		mScreenX2 += dx;
		mScreenY2 += dy;
	}

	/**
	 * Translates the x-position by dx on a scale without retina and AA factors
	 * @param dx
	 */
	public void translateX(int dx) {
		mScreenX1 += dx;
		mScreenX2 += dx;
	}

	/**
	 * Translates the x-position by dx on a scale without retina and AA factors
	 * @param dy
	 */
	public void translateY(int dy) {
		mScreenY1 += dy;
		mScreenY2 += dy;
	}

	/**
	 * Set screen coordinates without retina and AA factors
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 */
	public void setScreenLocation(int x1, int y1, int x2, int y2) {
		mScreenX1 = x1;
		mScreenY1 = y1;
		mScreenX2 = x2;
		mScreenY2 = y2;
	}

	/**
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean containsOnScreen(int x, int y) {
		return x >= mScreenX1 && x <= mScreenX2 && y >= mScreenY1 && y <= mScreenY2;
	}

	public void writePosition(BufferedWriter writer, String rowID, String columnTitle) throws IOException {
		writer.write(rowID);
		writer.write('\t');
		writer.write(columnTitle);
		writer.write('\t');
		writer.write(DoubleFormat.toString(mX));
		writer.write('\t');
		writer.write(DoubleFormat.toString(mY));
	}
}
