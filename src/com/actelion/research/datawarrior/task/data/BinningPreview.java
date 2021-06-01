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

package com.actelion.research.datawarrior.task.data;

import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.BinGenerator;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.DoubleFormat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.math.BigDecimal;

public class BinningPreview extends JPanel implements MouseMotionListener {
    private static final long serialVersionUID = 0x20120808;

    private static final int cPreviewWidth = HiDPIHelper.scale(240);
	private static final int cPreviewHeight = HiDPIHelper.scale(180);
	private static final int cBorder = HiDPIHelper.scale(6);

	private CompoundTableModel  mTableModel;
	private Image               mOffImage;
	private int[]				mMemberCount;
	private boolean				mPreviewValid,mOffImageValid,mIsLogarithmic,mIsDate;
	private BinGenerator		mLimits;
	private double[]            mCustomBinThreshold;
	private String[]            mCustomBinName;
	private int					mColumn,mMaxMemberCount,mMouseX,mMouseY;
	private double				mBinSize,mBinStart;
	private String				mMouseText;
	private Rectangle			mHighlightedRect;

    public BinningPreview(CompoundTableModel tableModel) {
		mTableModel = tableModel;
		mColumn = -1;
		addMouseMotionListener(this);
	    }

	public Dimension getPreferredSize() {
		return new Dimension(cPreviewWidth, cPreviewHeight);
		}

	public void paintComponent(Graphics g) {
		Dimension theSize = getSize();
		if (theSize.width == 0 || theSize.height == 0)
			return;

		if (mOffImage == null
		 || mOffImage.getWidth(null) != theSize.width
		 || mOffImage.getHeight(null) != theSize.height) {
			mOffImage = createImage(theSize.width,theSize.height);
			mOffImageValid = false;
		    }

		if (!mPreviewValid) {
			if (mColumn != -1) {
				if (mCustomBinThreshold != null) {
					mMemberCount = new int[mCustomBinThreshold.length + 1];
					for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
						double value = mTableModel.getDoubleAt(row, mColumn);
						if (mTableModel.isLogarithmicViewMode(mColumn))
							value = Math.pow(10.0, value);
						if (!Double.isNaN(value)) {
							int index = mCustomBinThreshold.length;
							for (int i=0; i<mCustomBinThreshold.length; i++) {
								if (value < mCustomBinThreshold[i]) {
									index = i;
									break;
									}
								}
							mMemberCount[index]++;
							}
						}
					mPreviewValid = true;
					}
				else {
					if (!Double.isNaN(mBinStart) && !Double.isNaN(mBinSize)) {
						mLimits = new BinGenerator(mTableModel, mColumn, new BigDecimal(mBinStart),
								new BigDecimal(mBinSize), mIsLogarithmic, mIsDate);
						int binCount = mLimits.getBinCount();

						mMemberCount = new int[binCount];
						for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
							int index = mLimits.getIndex(mTableModel.getDoubleAt(row, mColumn));
							if (index != binCount)
								mMemberCount[index]++;
							}

						mPreviewValid = true;
						}
					}

				if (mPreviewValid) {
					mMaxMemberCount = 0;
					for (int i=0; i<mMemberCount.length; i++)
						if (mMaxMemberCount < mMemberCount[i])
							mMaxMemberCount = mMemberCount[i];
					}
				}

			mMouseText = null;
			mOffImageValid = false;
			}

		if (!mOffImageValid) {
			Graphics offG = mOffImage.getGraphics();
			offG.setColor(Color.white);
			offG.fillRect(0, 0, theSize.width, theSize.height);

			if (mPreviewValid && mMaxMemberCount != 0) {
				int barSpacing = Math.max((cPreviewWidth-cBorder)/mMemberCount.length, 1);
				int barWidth = Math.max(barSpacing*2/3, 1);
				offG.setColor(new Color(121, 232, 144));
				for (int i=0; i<mMemberCount.length; i++) {
					if (mMemberCount[i] != 0) {
						int barHeight = (cPreviewHeight - 2 * cBorder) * mMemberCount[i] / mMaxMemberCount;
						int x = cBorder + barSpacing/6 + i * barSpacing;
						int y = cPreviewHeight - cBorder - barHeight;
						offG.fillRect(x, y, barWidth, barHeight);
						}
					}
				}

			offG.setColor(Color.gray);
			offG.draw3DRect(0, 0, theSize.width-1, theSize.height-1, false);

		    mOffImageValid = true;
			}

		g.drawImage(mOffImage, 0, 0, this);
		if (mMouseText != null) {
			g.setColor(new Color(85, 163, 101));
			g.fillRect(mHighlightedRect.x, mHighlightedRect.y, mHighlightedRect.width, mHighlightedRect.height);

			g.setColor(Color.black);
			int stringWidth = g.getFontMetrics().stringWidth(mMouseText);
			int x = Math.max(cBorder, Math.min(theSize.width-cBorder-stringWidth, mHighlightedRect.x + (mHighlightedRect.width - stringWidth)/2));
			g.drawString(mMouseText, x, g.getFontMetrics().getHeight());
			}
		}

	public void update(int column, double[] customBinThreshold, String[] customBinName, boolean isLogarithmic, boolean isDate) {
		mColumn = column;
		mCustomBinThreshold = customBinThreshold == null ? new double[0] : customBinThreshold;
		mCustomBinName = customBinName;
		mIsLogarithmic = isLogarithmic;
		mIsDate = isDate;
		mPreviewValid = false;
		repaint();
		}

	public void update(int column, double binSize, double binStart, boolean isLogarithmic, boolean isDate) {
		if (mColumn != column || mCustomBinThreshold != null || mBinSize != binSize || mBinStart != binStart || mIsLogarithmic != isLogarithmic) {
			mColumn = column;
			mBinSize = binSize;
			mBinStart = binStart;
			mIsLogarithmic = isLogarithmic;
			mIsDate = isDate;
			mPreviewValid = false;
			mCustomBinThreshold = null;
			mCustomBinName = null;
			repaint();
			}
		}

	public void mouseDragged(MouseEvent e) {}

	public void mouseMoved(MouseEvent e) {
		if (!mPreviewValid || mMemberCount.length == 0)
			return;

		boolean doRepaint = (mMouseText != null);
		mMouseX = e.getX();
		mMouseY = e.getY();
		mMouseText = null;
		int barSpacing = Math.max((cPreviewWidth-cBorder)/mMemberCount.length, 1);
		int barWidth = Math.max(barSpacing*2/3, 1);
		for (int i=0; i<mMemberCount.length; i++) {
			int barHeight = (cPreviewHeight - 2*cBorder) * mMemberCount[i] / mMaxMemberCount;
			int x = cBorder + barSpacing/6 + i*barSpacing;
			int y = cPreviewHeight - cBorder - barHeight;
			mHighlightedRect = new Rectangle(x, y, barWidth, barHeight);
			if (mHighlightedRect.contains(mMouseX, mMouseY)) {
				mMouseText = getBinName(i);
				repaint();
				return;
				}			
			}

		if (doRepaint)
			repaint();
		}

	private String getBinName(int index) {
    	if (mCustomBinThreshold == null)
    		return mLimits.getRangeString(index);
    	if (mCustomBinName != null)
    		return mCustomBinName[index];
    	if (index == 0)
			return mCustomBinThreshold.length == 0 ? "" : "x < " + DoubleFormat.toString(mCustomBinThreshold[0]);
    	if (index == mCustomBinThreshold.length)
			return DoubleFormat.toString(mCustomBinThreshold[mCustomBinThreshold.length-1]) + " <= x";
		return DoubleFormat.toString(mCustomBinThreshold[index-1]) + CompoundTableModel.cRangeSeparation + DoubleFormat.toString(mCustomBinThreshold[index]);
		}
	}
