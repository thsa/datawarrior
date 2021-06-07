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

import com.actelion.research.gui.LookAndFeelHelper;
import com.actelion.research.table.model.*;
import com.actelion.research.util.ColorHelper;
import com.actelion.research.util.DoubleFormat;

import java.awt.*;
import java.util.TreeMap;

public class VisualizationColor implements CompoundTableListener,CompoundTableListListener {
	public static final int cColorListModeHSBShort = 0;
	public static final int cColorListModeHSBLong = 1;
	public static final int cColorListModeStraight = 2;
	public static final int cColorListModeCategories = 3;

	public static final String[] COLOR_LIST_MODE_CODE = { "hsbShort", "hsbLong", "straight", "categories" };

	public static final int cWedgeColors = 128;

	public static final Color cSelectedColor = new Color(0, 102, 102);
	public static final Color cUseAsFilterColor = new Color(102, 0, 102);
	public static final Color cDefaultDataColor = new Color(202, 202, 0);
	public static final Color cMissingDataColor = Color.lightGray;

	protected static int cSpecialColorCount = 2;
	protected static final byte cDefaultDataColorIndex = 0;
	protected static final byte cMissingDataColorIndex = 1;

	public static final int cMaxColorCategories = 128;

	private static final Color[] cCategoryColor = {		 new Color( 73,   0, 255),
		new Color(255,   0,  42), new Color(  0, 173, 102), new Color(255, 255,   0),
		new Color( 79, 209, 248), new Color(221, 160, 246), new Color(255, 171,  25),
		new Color(192, 255, 208), new Color(255,   0, 216), new Color(170, 107,  88),
		new Color(188, 188, 188), new Color( 94, 107,  38), new Color(108, 255,   0),
		new Color(255, 188, 151), new Color(212, 231,  50), new Color(166,   0,   0)	} ;

	private static float[] sHSBBuffer = new float[3];

	private CompoundTableModel	mTableModel;
	private VisualizationColorListener mColorListener;
	private Color[] mColorList;
	private float	mColorMin,mColorMax;
	private float[] mColorThresholds;
	private int		mColorColumn,mColorListMode;
	private TreeMap<String,Color> mCategoryColorMap;

	public static Color[] createColorWedge(Color c1, Color c2, int mode, Color[] colorList) {
		int colorCount = (colorList == null) ? cWedgeColors : colorList.length;

		if (colorList == null)
			colorList = new Color[cWedgeColors];

		if (mode == cColorListModeStraight) {
			for (int i=0; i<colorCount; i++)
				colorList[i] = new Color(
					c1.getRed()+i*(c2.getRed()-c1.getRed())/(colorCount-1),
					c1.getGreen()+i*(c2.getGreen()-c1.getGreen())/(colorCount-1),
					c1.getBlue()+i*(c2.getBlue()-c1.getBlue())/(colorCount-1));
			return colorList;
			}

		float[] hsb1 = Color.RGBtoHSB(c1.getRed(), c1.getGreen(), c1.getBlue(), null);
		float[] hsb2 = Color.RGBtoHSB(c2.getRed(), c2.getGreen(), c2.getBlue(), null);

		if ((mode == cColorListModeHSBShort)
		  ^ (Math.abs(hsb1[0] - hsb2[0]) < 0.5)) {
			if (hsb1[0] < hsb2[0])
				hsb1[0] += 1.0;
			else
				hsb2[0] += 1.0;
			}

		for (int i=0; i<colorCount; i++)
			colorList[i] = Color.getHSBColor(
				hsb1[0]+(float)i*(hsb2[0]-hsb1[0])/(float)(colorCount-1),
				hsb1[1]+(float)i*(hsb2[1]-hsb1[1])/(float)(colorCount-1),
				hsb1[2]+(float)i*(hsb2[2]-hsb1[2])/(float)(colorCount-1));

		return colorList;
		}

	public static Color grayOutColor(Color color) {
		Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), sHSBBuffer);
		return Color.getHSBColor(sHSBBuffer[0], sHSBBuffer[1]/6, 1-(1-sHSBBuffer[2])/6);
		}

	public static Color lowContrastColor(Color c, Color background) {
		Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), sHSBBuffer);
		float perceivedBG = ColorHelper.perceivedBrightness(background);
		float min,max;
		if (perceivedBG > 0.5f) {
			min = perceivedBG - 0.2f;
			max = perceivedBG;
			}
		else {
			min = perceivedBG/2.0f + 0.25f;
			max = min + 0.2f;
			}
		return Color.getHSBColor(sHSBBuffer[0], sHSBBuffer[1]/6, min+sHSBBuffer[2]*(max-min));
		}

	public static float[] parseCustomThresholds(String thresholdString, CompoundTableModel tableModel, int column) {
		if (thresholdString == null || thresholdString.length() == 0)
			return null;

		boolean isLogarithmic = (column == -1) ? false : tableModel.isLogarithmicViewMode(column);
		String[] values = thresholdString.split(",");
		float[] thresholds = new float[values.length];
		for (int i=0; i<values.length; i++) {
			try {
				thresholds[i] = Float.parseFloat(values[i].trim());
				if (i != 0 && thresholds[i] <= thresholds[i-1]) {
					thresholds = null;
					break;
					}
				if (isLogarithmic && thresholds[i] <= 0) {
					thresholds = null;
					break;
					}
				}
			catch (NumberFormatException nfe) {
				thresholds = null;
				break;
				}
			}
		return thresholds;
		}

	/**
	 * Creates a VisualizationColor with unassigned column.
	 * The listener is not informed about this initial setting.
	 * @param tableModel
	 * @param colorListener
	 */
	public VisualizationColor(CompoundTableModel tableModel, VisualizationColorListener colorListener) {
		mTableModel = tableModel;
		mColorListener = colorListener;
		mColorColumn = JVisualization.cColumnUnassigned;
		mColorList = new Color[cSpecialColorCount];
		mColorList[cDefaultDataColorIndex] = cDefaultDataColor;
		mColorList[cMissingDataColorIndex] = cMissingDataColor;
		mColorListMode = cColorListModeHSBLong;
		mColorMin = Float.NaN;
		mColorMax = Float.NaN;
		}

	/**
	 * Sets the column to unassigned and list and mode to default.
	 * Fires colorChanged() if color column was not unassigned.
	 */
	public void initialize() {
		boolean fireChange = (mColorColumn != JVisualization.cColumnUnassigned);

		mColorColumn = JVisualization.cColumnUnassigned;

		Color[] oldColorList = mColorList;
		mColorList = new Color[cSpecialColorCount];
		for (int i=0; i<cSpecialColorCount; i++)
			mColorList[i] = oldColorList[i];

		mColorListMode = cColorListModeHSBLong;
		
		mCategoryColorMap = null;
		mColorThresholds = null;

		if (fireChange)
			mColorListener.colorChanged(this);
		}

	public Color getDefaultDataColor() {
		return mColorList[cDefaultDataColorIndex];
		}

	public void setDefaultDataColor(Color c) {
		mColorList[cDefaultDataColorIndex] = c;
		mColorListener.colorChanged(this);
		}

	public boolean isDefaultDefaultDataColor() {
		return cDefaultDataColor.equals(mColorList[cDefaultDataColorIndex]);
		}

	public Color getMissingDataColor() {
		return mColorList[cMissingDataColorIndex];
		}

	public void setMissingDataColor(Color c) {
		mColorList[cMissingDataColorIndex] = c;
		mColorListener.colorChanged(this);
		}

	public boolean isDefaultMissingDataColor() {
		return cMissingDataColor.equals(mColorList[cMissingDataColorIndex]);
		}

	public Color[] createDefaultCategoryColorList(int column) {
		int categories = (CompoundTableListHandler.isListColumn(column)) ?
							2 : mTableModel.getCategoryCount(column);
		return createDiverseColorList(categories);
		}

	public static Color[] createDiverseColorList(int colorCount) {
		Color[] colorList = new Color[colorCount];
		if (colorCount <= cCategoryColor.length) {
			for (int i=0; i<colorCount; i++)
				colorList[i] = cCategoryColor[i];
			}
		else {
			int divisor = ((colorCount & 1) == 0) ? colorCount : colorCount+1;
			for (int i=0; i<colorCount; i++) {
				boolean enlighten = ((i & 3) == 1);
				boolean darken = ((i & 3) == 3);
				colorList[i] = new Color(Color.HSBtoRGB((float)i/(float)divisor,
												enlighten ? (float)0.3 : (float)1.0,
												darken ? (float)0.6 : (float)1.0));
				}
			}
		return colorList;
		}

	/**
	 * @return color count without special colors
	 */
	public int getColorCount() {
		return mColorList.length - cSpecialColorCount;
		}

	public int getColorColumn() {
		return mColorColumn;
		}

	/**
	 * Returns the color of the current list at index position.
	 * @param index 0...n with n=color count without default colors
	 * @return
	 */
	public Color getColor(int index) {
		return mColorColumn == -1 ? null : mColorList[index+cSpecialColorCount];
		}

	/**
	 * Determines the color representing the value of <code>record</code> in
	 * the column that is associated with this VisualisationColor.
	 * If this VisualisationColor is associated with no column, then this method
	 * returns null;
	 */
	public Color getColor(CompoundRecord record) {
		return mColorColumn == -1 ? null : mColorList[getColorListIndex(record)];
		}

	/**
	 * Determines the color representing the value of <code>record</code> in
	 * the column that is associated with this VisualisationColor.
	 * If this VisualisationColor is associated with no column, then this method
	 * returns null;
	 * @param record
	 * @return null or record's color adapted to be used as background
	 */
	public Color getColorForBackground(CompoundRecord record) {
		Color c = getColor(record);
		return c == null ? null : LookAndFeelHelper.isDarkLookAndFeel() ?
				new Color(c.getRed()/3, c.getGreen()/3, c.getBlue()/3)
			  : new Color(c.getRed()/4+192, c.getGreen()/4+192, c.getBlue()/4+192);
		}

	/**
	 * Determines the color representing the value of <code>record</code> in
	 * the column that is associated with this VisualisationColor.
	 * If this VisualisationColor is associated with no column, then this method
	 * returns null;
	 * @param record
	 * @return null or record's color adapted to be used as foreground
	 */
	public Color getColorForForeground(CompoundRecord record) {
		Color c = getColor(record);
		return c == null ? null : LookAndFeelHelper.isDarkLookAndFeel() ?
				ColorHelper.createColor(c, 0.7f)
			  : c.darker();
		}

	/**
	 * Determines the color representing the value of <code>record</code> in
	 * the column that is associated with this VisualisationColor.
	 * If this VisualisationColor is associated with no column, then this method
	 * returns null;
	 * @param record
	 * @return null or record's color adapted to be used as background when printing
	 */
	public Color getColorForPrintBackground(CompoundRecord record) {
		Color c = getColor(record);
		return c == null ? null : new Color(c.getRed()/4+192, c.getGreen()/4+192, c.getBlue()/4+192);
		}

	/**
	 * Determines the color representing the value of <code>record</code> in
	 * the column that is associated with this VisualisationColor.
	 * If this VisualisationColor is associated with no column, then this method
	 * returns null;
	 * @param record
	 * @return null or record's color adapted to be used as foreground when printing
	 */
	public Color getColorForPrintForeground(CompoundRecord record) {
		Color c = getColor(record);
		return c == null ? null : c.darker();
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cChangeColumnData) {
			int column = e.getColumn();
			if (mColorColumn == column) {
				if (mColorListMode == cColorListModeCategories) {
					if (!mTableModel.isColumnTypeCategory(mColorColumn)
					 || mTableModel.getCategoryCount(column) > cMaxColorCategories) {
						initialize();
						}
					else {
						if (mCategoryColorMap != null)
							setColorList(createUpdatedCategoryColorList());
						else
							setColorList(createDefaultCategoryColorList(mColorColumn));

						mColorListener.colorChanged(this);
						}
					}
				else {
					if (!mTableModel.isColumnTypeDouble(mColorColumn))
						initialize();
					else
						mColorListener.colorChanged(this);
					}
				}
			}
		else if (e.getType() == CompoundTableEvent.cAddRows
			  || e.getType() == CompoundTableEvent.cDeleteRows) {
			if (mColorColumn != JVisualization.cColumnUnassigned
			 && !CompoundTableListHandler.isListColumn(mColorColumn)) {
				if (mColorListMode == VisualizationColor.cColorListModeCategories) {
					if (mTableModel.isColumnTypeCategory(mColorColumn)) {	// if still multiple categories
						setColorList(createUpdatedCategoryColorList());
						mColorListener.colorChanged(this);
						}
					else {
						initialize();
						}
					}
				else {
					mColorListener.colorChanged(this);
					}
				}
			}
		else if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			if (mColorColumn >= 0) {
				int[] columnMapping = e.getMapping();
				if (columnMapping[mColorColumn] == JVisualization.cColumnUnassigned)
					initialize();
				else
					mColorColumn = columnMapping[mColorColumn];
				}
			}
		else if (e.getType() == CompoundTableEvent.cChangeActiveRow) {
			if (mColorColumn >= 0 && mTableModel.isDescriptorColumn(mColorColumn))
				mColorListener.colorChanged(this);
			}
		}

	public void listChanged(CompoundTableListEvent e) {
		if (e.getType() == CompoundTableListEvent.cDelete) {
			if (CompoundTableListHandler.isListColumn(mColorColumn)) {
				int hitlistIndex = CompoundTableListHandler.convertToListIndex(mColorColumn);
				if (e.getListIndex() == hitlistIndex)
					initialize();
				else if (hitlistIndex > e.getListIndex())
					mColorColumn = CompoundTableListHandler.getColumnFromList(hitlistIndex-1);
				}
			}
		else if (e.getType() == CompoundTableListEvent.cChange) {
			if (CompoundTableListHandler.isListColumn(mColorColumn)) {
				int hitlistIndex = CompoundTableListHandler.convertToListIndex(mColorColumn);
				if (e.getListIndex() == hitlistIndex)
					mColorListener.colorChanged(this);
				}
			}
		}

	/**
	 * This returns the user definable part of the color list.
	 * @return
	 */
	public Color[] getColorListWithoutDefaults() {
		if (mColorColumn == -1 || mColorList.length == cSpecialColorCount)
			return null;

		// Determine the count of used colors; the size of mColorList may be larger and only partially used
		int colorCount = getColorListSizeWithoutDefaults();

		Color[] theList = new Color[colorCount];
		for (int i=0; i<colorCount; i++)
			theList[i] = mColorList[i+cSpecialColorCount];

		return theList;
		}

	public int getColorListSizeWithoutDefaults() {
		// Determine the count of used colors; the size of mColorList may be larger and only partially used
		return (mColorListMode != cColorListModeCategories) ? mColorList.length - cSpecialColorCount
				: mColorThresholds != null ? mColorThresholds.length + 1
				: (CompoundTableListHandler.isListColumn(mColorColumn)) ? 2
				: mTableModel.getCategoryCount(mColorColumn);
		}

	/**
	 * This returns the complete color list including selection color, etc.
	 * @return
	 */
	public Color[] getColorList() {
		return mColorList;
		}

	public int getColorListMode() {
		return mColorListMode;
		}

	private int getColorListIndex(CompoundRecord record) {
		if (mColorColumn == JVisualization.cColumnUnassigned)
			return cDefaultDataColorIndex;
		if (CompoundTableListHandler.isListColumn(mColorColumn)) {
			int hitlistIndex = CompoundTableListHandler.convertToListIndex(mColorColumn);
			int flagNo = mTableModel.getListHandler().getListFlagNo(hitlistIndex);
			return record.isFlagSet(flagNo) ? cSpecialColorCount : cSpecialColorCount + 1;
			}
		if (mTableModel.isDescriptorColumn(mColorColumn))
			return getSimilarityColorIndex(record);
		if (mColorListMode == cColorListModeCategories) {
			if (mColorThresholds != null) {
				double value = record.getDouble(mColorColumn);
				if (mTableModel.isLogarithmicViewMode(mColorColumn))
					value = Math.pow(10, value);
				for (int i=0; i<mColorThresholds.length; i++)
					if (value < mColorThresholds[i])
						return cSpecialColorCount+i;
				return cSpecialColorCount+mColorThresholds.length;
				}
			return cSpecialColorCount + mTableModel.getCategoryIndex(mColorColumn, record);
			}
		if (mTableModel.isColumnTypeDouble(mColorColumn)) {
			float min = Float.isNaN(mColorMin) ?
									mTableModel.getMinimumValue(mColorColumn)
					   : (mTableModel.isLogarithmicViewMode(mColorColumn)) ?
							   (float)Math.log(mColorMin)/(float)Math.log(10)
					   : mColorMin;
			float max = Float.isNaN(mColorMax) ?
									mTableModel.getMaximumValue(mColorColumn)
					   : (mTableModel.isLogarithmicViewMode(mColorColumn)) ?
							   (float)Math.log(mColorMax)/(float)Math.log(10)
					   : mColorMax;

			//	1. colorMin is explicitly set; max is real max, but lower than min
			// or 2. colorMax is explicitly set; min is real min, but larger than max
			// first case is OK, second needs adaption below to be handled as intended
			if (min >= max)
				if (!Float.isNaN(mColorMax))
					min = Float.MIN_VALUE;

			float value = record.getDouble(mColorColumn);
			if (Float.isNaN(value))
				return cMissingDataColorIndex;
			if (value <= min)
				return cSpecialColorCount;
			if (value >= max)
				return mColorList.length-1;
			return (int)(0.5 + cSpecialColorCount
					+ (float)(mColorList.length-cSpecialColorCount-1)
					* (value - min) / (max - min));
			}
		return cDefaultDataColorIndex;
		}

	private int getSimilarityColorIndex(CompoundRecord record) {
		CompoundRecord currentRecord = mTableModel.getActiveRow();
		if (currentRecord == null)
			return cDefaultDataColorIndex;

		float similarity = mTableModel.getDescriptorSimilarity(currentRecord, record, mColorColumn);
		if (Float.isNaN(similarity))
			return cMissingDataColorIndex;

		if (mColorThresholds != null) {
			for (int i=0; i<mColorThresholds.length; i++)
				if (similarity < mColorThresholds[i])
					return cSpecialColorCount + i;
			return cSpecialColorCount + mColorThresholds.length;
			}

		float min = Float.isNaN(mColorMin) ? 0.0f : mColorMin;
		float max = Float.isNaN(mColorMax) ? 1.0f : mColorMax;
		if (min >= max) {
			min = 0.0f;
			max = 1.0f;
			}

		if (similarity <= min)
			return cSpecialColorCount;
		if (similarity >= max)
			return mColorList.length-1;
		return (int)(0.5f + cSpecialColorCount
					+ (float)(mColorList.length - cSpecialColorCount - 1)
					* (similarity - min) / (max - min));
		}

	public float[] getColorThresholds() {
		return mColorThresholds;
		}

	public String getColorThresholdString() {
		if (mColorThresholds == null)
			return null;

		StringBuilder sb = new StringBuilder();
		for (float v:mColorThresholds) {
			if (sb.length() != 0)
				sb.append(",");
			sb.append(DoubleFormat.toString(v));
			}
		return sb.toString();
		}

	public String[] createCustomThresholdCategoryNames() {
		String[] categoryName = new String[mColorThresholds.length+1];
		categoryName[0] = "<"+ DoubleFormat.toString(mColorThresholds[0]);
		categoryName[mColorThresholds.length] = ">=" + DoubleFormat.toString(mColorThresholds[mColorThresholds.length-1]);
		for (int i=1; i<mColorThresholds.length; i++)
			categoryName[i] = DoubleFormat.toString(mColorThresholds[i-1]) + "-" + DoubleFormat.toString(mColorThresholds[i]);
		return categoryName;
		}

	/**
	 * Defines the min and max values that limit the color wedge.
	 * Rows representing values below min are drawn in the first
	 * color, those above max are drawn in the last color of the color
	 * sequence defined by setColor(... Color[] colorList ...).
	 * If min or max is Float.NaN then the lowest or highest existing
	 * value is used instead. min and max are not considered if the
	 * column type is a category or hitlist type.
	 * @param min NaN or value to be associated with colorList[0]
	 * @param max NaN or value to be associated with colorList[colorList.length-1]
	 */
	public void setColorRange(float min, float max) {
		if (mColorColumn >= 0
		 && (mTableModel.isColumnTypeDouble(mColorColumn)
		  || mTableModel.isDescriptorColumn(mColorColumn))
		 && (Float.isNaN(min) || Float.isNaN(max) || min < max)
		 && (Float.isNaN(min) || !mTableModel.isLogarithmicViewMode(mColorColumn) || min > 0.0)
		 && (Float.isNaN(max) || !mTableModel.isLogarithmicViewMode(mColorColumn) || max > 0.0)) {
			mColorMin = min;
			mColorMax = max;
			}

		mColorListener.colorChanged(this);
		}

	/**
	 * If a minimum value was set to define the lower end of the color wedge,
	 * then this value is returned.
	 * @return NaN if no minimum value was set
	 */
	public float getColorMin() {
		if (mColorColumn >= 0
		 && (mTableModel.isColumnTypeDouble(mColorColumn)
		  || mTableModel.isDescriptorColumn(mColorColumn)))
			return mColorMin;

		return Float.NaN;
		}

	/**
	 * If a maximum value was set to define the higher end of the color wedge,
	 * then this value is returned.
	 * @return NaN if no maximum value was set
	 */
	public float getColorMax() {
		if (mColorColumn >= 0
		 && (mTableModel.isColumnTypeDouble(mColorColumn)
		  || mTableModel.isDescriptorColumn(mColorColumn)))
			return mColorMax;
		
		return Float.NaN;
		}

	/**
	 * (Re-)defines the reference column of this VisualizationColor and sets core attributes to default values.
	 * @param column CompoundTableModel column to deliver the values
	 */
	public void setColor(int column) {
		if (column != mColorColumn) {
			if (CompoundTableListHandler.isListColumn(column))
   				setColor(column, createDefaultCategoryColorList(column), cColorListModeCategories);
			else if (column == JVisualization.cColumnUnassigned
			 || mTableModel.isColumnTypeDouble(column)
			 || mTableModel.isDescriptorColumn(column))
				setColor(column, createColorWedge(Color.red, Color.blue, cColorListModeHSBLong, null), cColorListModeHSBLong);
			else if (mTableModel.isColumnTypeCategory(column)
				  && mTableModel.getCategoryCount(column) <= cMaxColorCategories)
				setColor(column, createDefaultCategoryColorList(column), cColorListModeCategories);
			}
		}

	/**
	 * (Re-)defines reference column and/or the color list mode and/or one or more colors of the list.
	 * @param column existing column, cColumnUnassigned, or pseudo column referring to a hitlist
	 * @param colorList user defined color list (color wedge or category colors) without special colors
	 * @param mode one of the cColorListMode??? options
	 */
	public void setColor(int column, Color[] colorList, int mode) {
		setColor(column, colorList, mode, null);
		}

	/**
	 * (Re-)defines reference column and/or the color list mode and/or one or more colors of the list.
	 * @param column existing column, cColumnUnassigned, or pseudo column referring to a hitlist
	 * @param colorList user defined color list (color wedge or category colors) without special colors
	 * @param mode one of the cColorListMode??? options
	 * @param thresholds null or custom thresholds for numerical columns
	 */
	public void setColor(int column, Color[] colorList, int mode, float[] thresholds) {
		if (thresholds != null
		 && column >= 0
		 && mTableModel.isLogarithmicViewMode(column)) {
			for (float t:thresholds) {
				if (t <= 0f) {
					thresholds = null;
					mode = cColorListModeHSBLong;
					break;
					}
				}
			}

		if (CompoundTableListHandler.isListColumn(column)) {
			mode = cColorListModeCategories;
			}
		else if (mTableModel.isDescriptorColumn(column)) {
			if (mode == cColorListModeCategories && thresholds == null)
				mode = cColorListModeHSBLong;
			}
		else {
			if (!((mTableModel.isColumnTypeCategory(column)
				&& mTableModel.getCategoryCount(column) <= VisualizationColor.cMaxColorCategories)
			   || mTableModel.isColumnTypeDouble(column))) {
				column = JVisualization.cColumnUnassigned;
				thresholds = null;
				}
			else if (mode == cColorListModeCategories
				  && thresholds == null
				  && !mTableModel.isColumnTypeCategory(column)) {
				mode = cColorListModeHSBLong;
				}
			}

		if (colorList == null)
			colorList = new Color[0];

		if (mColorColumn == column && mColorListMode == mode && mColorList.length == colorList.length) {
			boolean isDifferent = false;
			for (int i=0; i<colorList.length; i++) {
				if (!colorList[i].equals(mColorList[i])) {
					isDifferent = true;
					break;
					}
				}

			if (thresholds != null) {
				if (mColorThresholds == null || mColorThresholds.length != thresholds.length) {
					isDifferent = true;
					}
				else {
					for (int i=0; i<thresholds.length; i++) {
						if (mColorThresholds[i] != thresholds[i]) {
							isDifferent = true;
							break;
							}
						}
					}
				}
			else if (mColorThresholds != null) {
				isDifferent = true;
				}

			if (!isDifferent)
				return;
			}

		if (mColorColumn != column && mColorListMode == cColorListModeCategories)
			mCategoryColorMap = null;

		if (mode == cColorListModeCategories) {
			int categoryCount = (thresholds != null) ? thresholds.length + 1 : mTableModel.getCategoryCount(column);
			if (categoryCount>colorList.length)
				colorList = extendCategoryColorList(colorList, categoryCount);
			}

		if (column != mColorColumn) {
			mColorMin = Float.NaN;
			mColorMax = Float.NaN;
			}

		mColorColumn = column;
		mColorListMode = mode;
		mColorThresholds = thresholds;
		setColorList(colorList);

		if (mode == cColorListModeCategories
		 && thresholds == null
		 && !CompoundTableListHandler.isListColumn(column))
			createCategoryColorMap(colorList);

		mColorListener.colorChanged(this);
		}

	/**
	 * Creates a mapping from category values to colors in order to
	 * retain color-category assignments when changing column data.
	 * @param colorList
	 */
	private void createCategoryColorMap(Color[] colorList) {
		mCategoryColorMap = new TreeMap<String,Color>();
		String[] categoryList = mTableModel.getCategoryList(mColorColumn);
		for (int i=0; i<categoryList.length; i++)
			mCategoryColorMap.put(categoryList[i], colorList[i]);
		}

	/**
	 * For category columns that contain more categories than a color list has colors,
	 * this method extends a color list to be used for the category column.
	 */
	private Color[] extendCategoryColorList(Color[] shortColorList, int newCategoryCount) {
		Color[] colorList = createDiverseColorList(newCategoryCount);
		for (int i=0; i<shortColorList.length; i++) {
			Color origColor = colorList[i];
   			colorList[i] = shortColorList[i];
			for (int j=shortColorList.length; j<colorList.length; j++) {
				if (colorList[j].equals(colorList[i])) {
					colorList[j] = origColor;
					}
				}
			}
		return colorList;
		}

	/**
	 * Create a new color list that reflects recent category-color assignments
	 * and matches the tableModel's current category list.
	 */
	private Color[] createUpdatedCategoryColorList() {
		String[] category = mTableModel.getCategoryList(mColorColumn);

		Color[] newColor = createDefaultCategoryColorList(mColorColumn);

		Color[] colorList = new Color[category.length];
		for (int i=0; i<colorList.length; i++) {
			colorList[i] = mCategoryColorMap.get(category[i]);
			if (colorList[i] != null) {
				for (int j=0; j<newColor.length; j++) {
					if (colorList[i].equals(newColor[j])) {
						newColor[j] = null;
						break;
						}
					}
				}
			}

		int newColorIndex = 0;
		for (int i=0; i<colorList.length; i++) {
			if (colorList[i] == null) {
				while (newColor[newColorIndex] == null)
					newColorIndex++;
				colorList[i] = newColor[newColorIndex++];
				}
			}

		createCategoryColorMap(colorList);
		return colorList;
		}

	/**
	 * Takes colorList plus the special colors (selection color, etc) as current list.
	 * @param colorList
	 */
	private void setColorList(Color[] colorList) {
		Color[] oldColorList = mColorList;
		mColorList = new Color[((colorList == null) ? 0 : colorList.length)+cSpecialColorCount];
		for (int i=0; i<cSpecialColorCount; i++)
			mColorList[i] = oldColorList[i];
		if (colorList != null)
			for (int i=0; i<colorList.length; i++)
				mColorList[i+cSpecialColorCount] = colorList[i];
		}
	}
