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

import java.awt.*;


public class VisualizationSplitter {
	private int	 mHeaderHeight,mWidth,mHeight,mSpacing,mHalfSpacing,mHCount,mVCount;
	private int[]   mX,mY;

	/**
	 * constructs a VisualizationSplitter based on one or two categories
	 * (in the first case column2 must be JVisualization.cColumnUnassigned)
	 * @param bounds
	 * @param count1 number of items in 1st category
	 * @param count2 number of items in 2nd category (-1 if only one category used)
	 * @param fontHeight font height without view splitting
	 * @param largeHeader true if header needs more height, e.g. for depicting molecules
	 * @param aspect desired width/height ratio of sub-views
	 */
	protected VisualizationSplitter(Rectangle bounds, int count1, int count2, int fontHeight, boolean largeHeader, float aspect) {
		int count = Math.max(1, count2 == -1 ? count1 : count1*count2);
		mHeaderHeight = (largeHeader) ? fontHeight*5 : fontHeight*3/2;

		if (bounds.height <= 0) {
			mHeaderHeight = 0;
			mHCount = count;
			mVCount = 1;
			}
		else if (count2 == -1) {
			mHeaderHeight = (int)Math.min(mHeaderHeight, bounds.height/Math.max(1, 4*Math.sqrt(aspect*count)));
			int v = 1 + count;
			double minPanalty = Double.MAX_VALUE;
			for (int h=1; h<=count; h++) {
				int oldV = v;
				while (h*(v-1) >= count)
					v--;

				if (v == oldV)
					continue;

				double panalty = Math.abs(Math.log(((double)bounds.width/(double)h)/(aspect*((double)bounds.height/(double)v-(double)mHeaderHeight))));
				if (minPanalty > panalty) {
					minPanalty = panalty;
					mHCount = h;
					mVCount = v;
					}
				}
			}
		else {
			mHeaderHeight = Math.min(mHeaderHeight, bounds.height/(4*count2));
			mHCount = count1;
			mVCount = count2;
			}
		fontHeight = Math.min(fontHeight, mHeaderHeight);

		mSpacing = (bounds.width / mHCount < bounds.height / mVCount) ?
					bounds.width / (32 * mHCount)
				  : bounds.height / (32 * mVCount);
		mHalfSpacing = mSpacing - mSpacing/2;
		int remainingWidth = bounds.width - mSpacing * mHCount;
		int remainingHeight = bounds.height - mSpacing * mVCount;
		mWidth = (remainingWidth - 1) / mHCount;
		mHeight = (remainingHeight - 1) / mVCount;
		remainingWidth -= mHCount * mWidth;
		remainingHeight -= mVCount * mHeight;
		mX = new int[mHCount];
		mX[0] = bounds.x + remainingWidth / 2;
		for (int i=1; i<mHCount; i++)
			mX[i] = mX[i-1] + mWidth + mSpacing;
		mY = new int[mVCount];
		mY[0] = bounds.y + remainingHeight / 2;
		for (int i=1; i<mVCount; i++)
			mY[i] = mY[i-1] + mHeight + mSpacing;
		}

	/**
	 * @param hvIndex combined category index
	 * @return bounds of graph area of respective sub-view
	 */
	public Rectangle getGraphBounds(int hvIndex) {
		int hIndex = hvIndex % mHCount;
		int vIndex = hvIndex / mHCount;
		return new Rectangle(mX[hIndex]+mHalfSpacing,
							 mY[vIndex]+mHalfSpacing+mHeaderHeight,
							 mWidth,
							 mHeight-mHeaderHeight);
		}

	/**
	 * @param hvIndex combined category index
	 * @return bounds of title area of respective sub-view
	 */
	public Rectangle getTitleBounds(int hvIndex) {
		int hIndex = hvIndex % mHCount;
		int vIndex = hvIndex / mHCount;
		return new Rectangle(mX[hIndex],
							 mY[vIndex],
							 mWidth+mSpacing,
							 mHeaderHeight);
		}

	public int getGridWidth() {
		return mWidth+mSpacing;
		}

	public int getGridHeight() {
		return mHeight+mSpacing;
		}

	public int getHCount() {
		return mHCount;
		}

	public int getVCount() {
		return mVCount;
		}

	public int getHIndex(int hvIndex) {
		return hvIndex % mHCount;
		}

	public int getVIndex(int hvIndex) {
		return hvIndex / mHCount;
		}

	public void paintGrid(Graphics g, Color borderColor, Color titleBackground) {
		int x1 = mX[0];
		int x2 = x1 + mHCount * (mWidth + mSpacing);
		int y1 = mY[0];
		int y2 = y1 + mVCount * (mHeight + mSpacing);

		g.setColor(titleBackground);
		for (int y=y1; y<y2; y+=mHeight+mSpacing) {
			g.fillRect(x1+1, y+1, x2-x1-1, mHeaderHeight-1);
			}

		g.setColor(borderColor);
		for (int x=x1; x<=x2; x+=mWidth+mSpacing)
			g.drawLine(x, y1, x, y2);
		for (int y=y1; y<=y2; y+=mHeight+mSpacing)
			g.drawLine(x1, y, x2, y);
		for (int y=y1; y<y2; y+=mHeight+mSpacing)
			g.drawLine(x1, y+mHeaderHeight, x2, y+mHeaderHeight);
		}
	}
