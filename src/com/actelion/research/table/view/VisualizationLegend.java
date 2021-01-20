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

import com.actelion.research.chem.AbstractDepictor;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableListHandler;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.DoubleFormat;
import com.actelion.research.util.ScaleLabel;
import com.actelion.research.util.ScaleLabelCreator;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class VisualizationLegend {
    public static final int cScaleLabelCount = 4;

	public static final int cLegendTypeNoLegend = 0;
	public static final int cLegendTypeSize = 1;
	public static final int cLegendTypeColorDouble = 2;
	public static final int cLegendTypeColorCategory = 3;
	public static final int cLegendTypeShapeCategory = 4;
	public static final int cLegendTypeBackgroundColorDouble = 5;
	public static final int cLegendTypeBackgroundColorCategory = 6;
	public static final int cLegendTypeMultiValueMarker = 7;

	private CompoundTableModel	mTableModel;
	private VisualizationColor	mVisualizationColor;
	private int					mHeight,mType,mX,mY,mTitleCells,
								mColumn,mFontHeight,mCellsPerLine,mCellWidth,mCellHeight;
	private String				mColumnName;
	private String[]			mCategoryList;
	private int[]				mMarkerSize;
	private boolean				mCategoryListContainsIDCodes,mColumnNameOnTop;
	private JVisualization		mVisualization;

	public VisualizationLegend(JVisualization visualization,
	                           CompoundTableModel tableModel,
	                           int column,
	                           VisualizationColor visualizationColor,
	                           int type) {
		mVisualization = visualization;
	    mTableModel = tableModel;
		mColumn = column;
		mVisualizationColor = visualizationColor;
		mType = type;
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cRemoveColumns
		 && mColumn >= 0)
			mColumn = e.getMapping()[mColumn];
		}

	public void paint(Rectangle bounds, boolean transparentBG) {
		mVisualization.setFontHeight(mFontHeight);
		switch (mType) {
		case cLegendTypeColorDouble:
		case cLegendTypeBackgroundColorDouble:
			String columnName = mTableModel.getColumnTitleExtended(mColumn);
			int columnNameWidth = mVisualization.getStringWidth(columnName);
			int x = bounds.x + (mColumnNameOnTop ? bounds.width/4
					: Math.min(bounds.width*9/20, Math.max(bounds.width/4, 2*mFontHeight + columnNameWidth)));
			int y = mColumnNameOnTop ? mY + mFontHeight + mHeight/8 : mY + mHeight/8;

			int width = bounds.width / 2;
			int height = mHeight/4;
			int noOfColors = mVisualizationColor.getColorListSizeWithoutDefaults();

			if (mType == cLegendTypeColorDouble) {
				for (int i=0; i<noOfColors; i++) {
				    mVisualization.setColor(mVisualizationColor.getColor(i));
				    mVisualization.fillRect(x+i*width/noOfColors, y, width/noOfColors+1, height);
					}
				mVisualization.setColor(mVisualization.getContrastGrey(0.80f));
				mVisualization.drawRect(x, y, width, height);
				}
			else {
				int centerY = y + height/2;
				Color neutralColor = mVisualization.getViewBackground();
				for (int i=0; i<noOfColors; i++) {
					Color baseColor = mVisualizationColor.getColor(i);
					for (int j=height-1; j>0; j--) {
						Color bgColor = transparentBG ?
								new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 255/height)
								: new Color(baseColor.getRed()+j*(neutralColor.getRed()-baseColor.getRed())/height,
											baseColor.getGreen()+j*(neutralColor.getGreen()-baseColor.getGreen())/height,
											baseColor.getBlue()+j*(neutralColor.getBlue()-baseColor.getBlue())/height);
					    mVisualization.setColor(bgColor);
						int x1 = x+i*width/noOfColors;
						int x2 = x+(i+1)*width/noOfColors;
					    mVisualization.fillRect(x1, centerY-j, x2-x1, j*2);
						}
					}
				mVisualization.setColor(mVisualization.getContrastGrey(0.80f));
				}

            float min,max;
            if (mTableModel.isDescriptorColumn(mColumn)) {
                min = mVisualization.getMarkerColor().getColorMin();
                max = mVisualization.getMarkerColor().getColorMax();
                if (Float.isNaN(min))
                    min = 0.0f;
                if (Float.isNaN(max))
                    max = 1.0f;
                if (min >= max) {
                    min = 0.0f;
                    max = 1.0f;
                    }
                }
            else {
                if (mType == cLegendTypeColorDouble) {
                    min = mVisualization.getMarkerColor().getColorMin();
                    max = mVisualization.getMarkerColor().getColorMax();
                    }
                else {  // mType == cLegendTypeBackgroundColorDouble
                    min = ((JVisualization2D)mVisualization).getBackgroundColor().getColorMin();
                    max = ((JVisualization2D)mVisualization).getBackgroundColor().getColorMax();
                    }

                if (Float.isNaN(min))
                    min = mTableModel.getMinimumValue(mColumn);
                if (Float.isNaN(max))
                    max = mTableModel.getMaximumValue(mColumn);
                }

			int scaleX = 0;
			int scaleY = y+height+mFontHeight;

			if (mTableModel.isDescriptorColumn(mColumn) || mTableModel.isColumnTypeDate(mColumn)) {
				for (int i=cScaleLabelCount; i>=0; i--) {
					scaleX = x+i*width/cScaleLabelCount;
					mVisualization.drawLine(scaleX, y+height+1, scaleX, y+height+height/8);
					float value = min + i*(max - min)/(float)cScaleLabelCount;
	                if (mTableModel.isLogarithmicViewMode(mColumn))
	                    value = (float)Math.pow(10.0, value);
					String label = (!mTableModel.isDescriptorColumn(mColumn) && mTableModel.isColumnTypeDate(mColumn)) ?
							DateFormat.getDateInstance().format(new Date(86400000*(long)(value)))
						  : DoubleFormat.toString(value, 3, true);
					scaleX -= mVisualization.getStringWidth(label)/2;
					mVisualization.drawString(label, scaleX, scaleY);
					}
				}
			else {
				VisualizationColor vc = (mType == cLegendTypeColorDouble) ? mVisualization.getMarkerColor()
										: ((JVisualization2D)mVisualization).getBackgroundColor();
				float rangeLow = Float.isNaN(vc.getColorMin()) ? mTableModel.getMinimumValue(mColumn) : mTableModel.isLogarithmicViewMode(mColumn) ? (float)Math.log10(vc.getColorMin()) : vc.getColorMin();
				float rangeHigh = Float.isNaN(vc.getColorMax()) ? mTableModel.getMaximumValue(mColumn) : mTableModel.isLogarithmicViewMode(mColumn) ? (float)Math.log10(vc.getColorMax()) : vc.getColorMax();
				ArrayList<ScaleLabel> labelList = (mTableModel.isLogarithmicViewMode(mColumn)) ?
						ScaleLabelCreator.createLogarithmicLabelList(rangeLow, rangeHigh)
					  : ScaleLabelCreator.createLinearLabelList(rangeLow, rangeHigh);
				int labelCount = (labelList == null) ? 0 : labelList.size();

				// set individual labels to null, if labels overlap
				if (labelCount > 2) {
					float gap = (float)(labelList.get(1).position - labelList.get(0).position) * width;

					// try using all labels
					boolean labelsOverlap = false;
					for (int i=1; i<labelCount; i++) {
						float needed = (mVisualization.getFontSize()
									  + mVisualization.getStringWidth(labelList.get(i-1).label)
									  + mVisualization.getStringWidth(labelList.get(i).label)) / 2f;
						if (needed > gap) {
							labelsOverlap = true;
							break;
							}
						}

					if (labelsOverlap) {
						// try using every second label
						labelsOverlap = false;
						for (int i=2; i<labelCount; i+=2) {
							float needed = (mVisualization.getFontSize()
										  + mVisualization.getStringWidth(labelList.get(i-2).label)
										  + mVisualization.getStringWidth(labelList.get(i).label)) / 2f;
							if (needed > 2f * gap) {
								labelsOverlap = true;
								break;
								}
							}

						// if every second doesn't work either use 1st and last label
						if (labelsOverlap) {
							for (int i=1; i<labelCount-1; i++)
								labelList.get(i).label = null;
							}
						// else remove every second label
						else {
							for (int i=1; i<labelCount; i+=2)
								labelList.get(i).label = null;
							}
						}
					}
				
				for (int i=labelCount-1; i>=0; i--) {
					ScaleLabel sl = labelList.get(i);
					scaleX = x+Math.round((float)sl.position*width);
					mVisualization.drawLine(scaleX, y+height+1, scaleX, y+height+height/8);
					if (sl.label != null) {
						scaleX -= mVisualization.getStringWidth(sl.label)/2;
						mVisualization.drawString(sl.label, scaleX, scaleY);
						}
					}
				}

			if (mColumnNameOnTop) {
				mVisualization.drawString(columnName, mX + (bounds.width-columnNameWidth) / 2, mY + mFontHeight);
				}
			else {
				if (scaleX > x)
					scaleX = x;
				mVisualization.drawString(columnName, scaleX-mFontHeight-columnNameWidth, scaleY);
				}
			break;
		case cLegendTypeSize:
			x = mX;
			y = mY + mCellHeight/2;
			int cell = mTitleCells;

			Color textColor = mVisualization.getContrastGrey(0.80f);
			mVisualization.setColor(textColor);
			if (!mColumnNameOnTop && mTitleCells == 0)
			    mVisualization.drawString(mColumnName, x - mVisualization.getStringWidth(mColumnName) - mFontHeight, y+mFontHeight*2/5);
			else
				mVisualization.drawString(mColumnName, x, y+mFontHeight*2/5);

			if (mColumnNameOnTop)
				y += mCellHeight;
			else
				x += mCellWidth * mTitleCells;

			for (int i=0; i<mCategoryList.length; i++) {
				mVisualization.drawMarker(Color.lightGray, 0, mMarkerSize[i], x+mFontHeight/3+mMarkerSize[i]/2, y);
				mVisualization.setColor(textColor);
			    mVisualization.drawString(mCategoryList[i], x+mFontHeight*2/3+mMarkerSize[i], y+mFontHeight*2/5);

				if (cell%mCellsPerLine == mCellsPerLine-1) {
					x = mX;
					y += mCellHeight;
					}
				else {
					x += mCellWidth;
					}
				cell++;
				}
			break;
		case cLegendTypeColorCategory:
		case cLegendTypeBackgroundColorCategory:
		case cLegendTypeShapeCategory:
		case cLegendTypeMultiValueMarker:
			// three options to position column title:
			// -top: starting just above first marker; mColumnNameOnTop=true, mTitleCells=0
			// -left: ending just left of first marker, markers start new line at mX; mColumnNameOnTop=false, mTitleCells=0
			// -embedded: starting at mX, first marker starts mTitleCells further right; mColumnNameOnTop=false, mTitleCells!=0
			x = mX;
			y = mY + mCellHeight/2;
			cell = mTitleCells;

			mVisualization.setColor(mVisualization.getContrastGrey(0.80f));
			if (!mColumnNameOnTop && mTitleCells == 0)
				mVisualization.drawString(mColumnName, x - mVisualization.getStringWidth(mColumnName) - mFontHeight, y+mFontHeight*2/5);
			else
				mVisualization.drawString(mColumnName, x, y+mFontHeight*2/5);

			if (mColumnNameOnTop)
				y += mCellHeight;
			else
				x += mCellWidth * mTitleCells;

			for (int i=0; i<mCategoryList.length; i++) {
				if (mType == cLegendTypeColorCategory || mType == cLegendTypeShapeCategory) {
					int shape = (mColumn == mVisualization.getMarkerShapeColumn()) ? i : 0;
					Color color = (mType == cLegendTypeColorCategory && i<mVisualizationColor.getColorCount()) ?
							mVisualizationColor.getColor(i) : Color.lightGray;
					mVisualization.drawMarker(color, shape, mFontHeight*8/10, x+mFontHeight*2/3-1, y);
					}
				else if (mType == cLegendTypeMultiValueMarker) {
					if (mVisualization.getMarkerColor().getColorColumn() == JVisualization.cColumnUnassigned) {
					    int size = mFontHeight*8/10+1;
					    mVisualization.setColor(((JVisualization2D)mVisualization).getMultiValueMarkerColor(i));
					    mVisualization.fillRect(x+mFontHeight-size, y-size/2, size, size);
					    mVisualization.setColor(mVisualization.getContrastGrey(0.75f));
					    mVisualization.drawRect(x+mFontHeight-size, y-size/2, size, size);
						}
					else {
						String no = ""+(i+1)+":";
					    mVisualization.drawString(no, x+mFontHeight-mVisualization.getStringWidth(no), y+mFontHeight*2/5);
						}
					}
				else if (mType == cLegendTypeBackgroundColorCategory) {
					int radius = 1 + mFontHeight*5/10;
					int centerX = x + mFontHeight*2/3;
					int centerY = y;
					Color baseColor = mVisualizationColor.getColor(i);
					Color neutralColor = mVisualization.getViewBackground();
					for (int j=radius-1; j>0; j--) {
					    mVisualization.setColor(new Color(baseColor.getRed()+j*(neutralColor.getRed()-baseColor.getRed())/radius,
											 baseColor.getGreen()+j*(neutralColor.getGreen()-baseColor.getGreen())/radius,
											 baseColor.getBlue()+j*(neutralColor.getBlue()-baseColor.getBlue())/radius));
					    mVisualization.fillRect(centerX-j, centerY-j, j*2, j*2);
						}
					mVisualization.setColor(mVisualization.getContrastGrey(0.80f));
					}

				if (mCategoryListContainsIDCodes) {
					String idcode = mCategoryList[i];
					if (idcode.length() != 0) {
						int index = idcode.indexOf(' ');
						StereoMolecule mol = (index == -1) ?
									new IDCodeParser(true).getCompactMolecule(idcode)
								  : new IDCodeParser(true).getCompactMolecule(idcode.substring(0, index),
																		  idcode.substring(index+1));
						mVisualization.drawMolecule(mol, mVisualization.getContrastGrey(0.80f),
										   new Rectangle2D.Double(x+mFontHeight*4/3,
			                			   y-mCellHeight/2,
			                			   mCellWidth*9/10-mFontHeight*4/3,
			                			   mCellHeight),
			                			   AbstractDepictor.cModeInflateToMaxAVBL, mCellWidth/6);
						}
					}
				else {
				    mVisualization.drawString(mCategoryList[i], x+mFontHeight*4/3, y+mFontHeight*2/5);
					}

				if (cell%mCellsPerLine == mCellsPerLine-1) {
					x = mX;
					y += mCellHeight;
					}
				else {
					x += mCellWidth;
					}
				cell++;
				}
			break;
			}
		}

	protected boolean layoutIsValid(boolean isCategory, int categoryCount) {
		switch (mType) {
		case cLegendTypeColorDouble:
		case cLegendTypeBackgroundColorDouble:
			return !isCategory;
		case cLegendTypeColorCategory:
		case cLegendTypeShapeCategory:
		case cLegendTypeBackgroundColorCategory:
		case cLegendTypeMultiValueMarker:
			return mCategoryList.length == categoryCount;
		default:
			return false;
			}
		}

	protected void calculate(Rectangle bounds, int fontHeight) {
		mVisualization.setFontHeight(fontHeight);
	    mFontHeight = fontHeight;
	    mCategoryListContainsIDCodes = false;
	    mColumnNameOnTop = false;

		mColumnName = (mType == cLegendTypeMultiValueMarker) ? "Value Name"
			  : CompoundTableListHandler.isListColumn(mColumn) ?
				"Member of '" + mTableModel.getListHandler().getListName(CompoundTableListHandler.convertToListIndex(mColumn)) + "'"
			  : mTableModel.getColumnTitleExtended(mColumn);

		int columnNameWidth = mFontHeight + mVisualization.getStringWidth(mColumnName);
		int legendSpacing = fontHeight / 2;

	    switch (mType) {
		case cLegendTypeColorDouble:
		case cLegendTypeBackgroundColorDouble:
			mHeight = 2 * mFontHeight + legendSpacing;
			if (columnNameWidth > bounds.width/2 - 2*mFontHeight) {
				mColumnNameOnTop = true;
				mHeight += mFontHeight;
				}
			mX = bounds.x;
			mY = bounds.y + bounds.height - mHeight;
			break;
		case cLegendTypeSize:
			if (CompoundTableListHandler.isListColumn(mColumn)) {
				mCategoryList = new String[2];
				mCategoryList[0] = "yes";
				mCategoryList[1] = "no";
				mMarkerSize = new int[2];
				mMarkerSize[0] = Math.round(mVisualization.getMarkerSizeFromHitlistMembership(true));
				mMarkerSize[1] = Math.round(mVisualization.getMarkerSizeFromHitlistMembership(false));
				}
			else {
				float rangeLow = mTableModel.getMinimumValue(mColumn);
				float rangeHigh = mTableModel.getMaximumValue(mColumn);
				ArrayList<ScaleLabel> labelList = (mTableModel.isLogarithmicViewMode(mColumn)) ?
						ScaleLabelCreator.createLogarithmicLabelList(rangeLow, rangeHigh)
					  : ScaleLabelCreator.createLinearLabelList(rangeLow, rangeHigh);
				int labelCount = (labelList == null) ? 0 : labelList.size();

				// limit markers shown to 4 times font size
				for (int i=0; i<labelCount; i++) {
					if (Math.round(mVisualization.getMarkerSizeFromValue((float)labelList.get(i).value)) > mFontHeight*4) {
						while (labelList.size() > i)
							labelList.remove(i);
						labelCount = i;
						break;
						}
					}

				// if we have too many labels, then remove every second one
				if (labelCount > 7) {
					for (int i = labelCount - ((labelCount & 1) == 0 ? 1 : 2); i > 0; i -= 2)
						labelList.remove(i);
					labelCount = labelList.size();
					}

				mCategoryList = new String[labelCount];
				mMarkerSize = new int[labelCount];
				for (int i=0; i<labelCount; i++) {
					mCategoryList[i] = labelList.get(i).label;
					mMarkerSize[i] = Math.round(mVisualization.getMarkerSizeFromValue((float)labelList.get(i).value));
					}
				}

			validateCategoryLayout(bounds, columnNameWidth, legendSpacing);
			break;
		case cLegendTypeColorCategory:
		case cLegendTypeShapeCategory:
		case cLegendTypeBackgroundColorCategory:
			if (CompoundTableListHandler.isListColumn(mColumn)) {
				mCategoryList = new String[2];
				mCategoryList[0] = "yes";
				mCategoryList[1] = "no";
				}
			else {
				mCategoryList = mTableModel.getCategoryList(mColumn);
                String specialType = mTableModel.getColumnSpecialType(mColumn);
                mCategoryListContainsIDCodes = (specialType != null && specialType.equals(CompoundTableModel.cColumnTypeIDCode));
				}

			validateCategoryLayout(bounds, columnNameWidth, legendSpacing);
			break;
		case cLegendTypeMultiValueMarker:
			int[] columns = ((JVisualization2D)mVisualization).getMultiValueMarkerColumns();
			mCategoryList = new String[columns.length];
			for (int i=0; i<columns.length; i++)
				mCategoryList[i] = mTableModel.getColumnTitle(columns[i]);
			validateCategoryLayout(bounds, columnNameWidth, legendSpacing);
			break;
			}
		}

	private void validateCategoryLayout(Rectangle bounds, int columnNameWidth, int legendSpacing) {
		int categories = mCategoryList.length;

		if (mCategoryListContainsIDCodes) {
			mCellWidth = bounds.width / 8;
			mCellHeight = mCellWidth / 2;
			}
		else {
			int maxTextLen = 0;
			for (int i=0; i<categories; i++) {
				int len = mVisualization.getStringWidth(mCategoryList[i]);
				if (maxTextLen < len)
					maxTextLen = len;
				}
			mCellHeight = mFontHeight*4/3;
			mCellWidth = mCellHeight + maxTextLen + mFontHeight;
			if (mType == cLegendTypeSize) {
				int maxSize = 0;
				for (int i=0; i<mMarkerSize.length; i++)
					if (maxSize < mMarkerSize[i])
						maxSize = mMarkerSize[i];
				if (mCellHeight < maxSize) {
					mCellWidth += maxSize - mCellHeight;
					mCellHeight = maxSize;
					}
				}
			}

		int lineWidth = bounds.width - mFontHeight/2;
		if (mCellWidth > lineWidth)
			mCellWidth = lineWidth;
		int maxCellsPerLine = Math.max(1, mCellWidth == 0 ? categories : lineWidth / mCellWidth);

		mColumnNameOnTop = false;
		mTitleCells = 0;

		int noOfLines = 1 + (categories-1) / maxCellsPerLine;
		mCellsPerLine = (noOfLines == 0) ? 1 : 1 + (categories-1) / noOfLines;

		// if column title at the left reduces number of categories per line, then we check, whether top or embedded are better options
		if (lineWidth - mCellWidth * mCellsPerLine - mFontHeight < columnNameWidth) {
			int titleCells = 1 + (columnNameWidth - 1) / Math.max(1, mCellWidth);
			int emptyCells = noOfLines * mCellsPerLine - categories;
			if (titleCells > emptyCells) {
				mColumnNameOnTop = true;
				noOfLines++;
				}
			else {
				mTitleCells = titleCells;
				}
			}

		mHeight = mCellHeight * noOfLines + legendSpacing;
		mX = bounds.x + (bounds.width - mCellsPerLine * mCellWidth) / 2;
		mY = bounds.y + bounds.height - mHeight;

		if (!mColumnNameOnTop && mTitleCells == 0)
			mX += columnNameWidth / 2;
		}

	protected int getColumn() {
		return mColumn;
		}

	protected int getHeight() {
		return mHeight;
		}

	protected void moveVertically(int distance) {
		mY += distance;
		}
	}
