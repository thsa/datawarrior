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

public abstract class AbstractCategoryChart {
    protected static final int FLOAT_DIGITS = 4;  // significant digits for in-view floating point values
    protected static final int INT_DIGITS = 8;    // significant digits for in-view integer values

    JVisualization mVisualization;
    int[][] mPointsInCategory;   // in case of box plot this excludes outliers
    int[][][] mPointsInColorCategory;
    Color[] mColor;
    float[][] mBarValue;         // in case of sum mode: sum of individual values; in case of mean mode: sum/count
    float[][] mAbsValueSum;      // only used in case of mean or sum. As barValue, but sum is built from abs() of individual values
    float[][] mAbsValueFactor;   // factor from abs(record.getDouble()) to screen bar fraction height
    float[][][] mAbsColorValueSum;   // same as absValueSum, but split into the color category sections
    float[][][] mAbsColorWidthSum;   // only used in case of variable widths, i.e. if a marker size column is selected
    float[][] mMean;
    float[][] mStdDev;	// standard deviation (sigma-1)
    float[][] mErrorMargin;// (95 % confidence)
    float[][] mInnerDistance;   // distance of two adjacent sub-bar areas in bar
    float[][] mPieX;
    float[][] mPieY;
    float[][] mPieSize;         // pie size in pixel
    float mAxisMin,mAxisMax, mBarBase;
    float mBarWidth, mMaxWidth;
    int mDoubleAxis,mHVCount,mCatCount,mFocusFlagNo,mBaseColorCount;
    boolean mBarOrPieDataAvailable;

    public AbstractCategoryChart(JVisualization visualization, int hvCount, int doubleAxis) {
        mVisualization = visualization;
        mHVCount = hvCount;
        mDoubleAxis = doubleAxis;

        mVisualization.calculateCategoryCounts(mDoubleAxis);

        // determine the number of visible bars/pies/violins... within the view, or within one view in case we have split views
        int dimensions = mVisualization.getDimensionCount();
        mCatCount = mVisualization.getCaseSeparationCategoryCount();
        for (int i=0; i<dimensions; i++)
            if (i != mDoubleAxis)
                mCatCount *= mVisualization.getCategoryVisCount(i);

        calculateColors();
    }

    private void calculateColors() {
        Color[] colorList = mVisualization.getMarkerColor().getColorList();
        mBaseColorCount = colorList.length;
        mFocusFlagNo = mVisualization.getFocusFlag();
        int extendedColorCount = colorList.length + 2;

        // create extended color list
        mColor = new Color[extendedColorCount * ((mFocusFlagNo == -1) ? 1 : 2)];
        for (int i=0; i<colorList.length; i++)
            mColor[i] = colorList[i];
        mColor[colorList.length] = VisualizationColor.cSelectedColor;
        mColor[colorList.length+1] = VisualizationColor.cUseAsFilterColor;
        if (mFocusFlagNo != -1) {
            for (int i=0; i<colorList.length; i++)
                mColor[i+extendedColorCount] = VisualizationColor.grayOutColor(colorList[i]);
            mColor[colorList.length+extendedColorCount] = VisualizationColor.grayOutColor(VisualizationColor.cSelectedColor);
            mColor[colorList.length+1+extendedColorCount] = VisualizationColor.grayOutColor(VisualizationColor.cUseAsFilterColor);
        }
    }

    public float getBarWidthFactor(float sizeValue) {
        return Float.isNaN(sizeValue) ? 0f : 2f * Math.abs(sizeValue) / mMaxWidth;
        }

    protected void calculateStdDevAndErrorMargin(JVisualization visualization, int[][] vCount, int axis, int column) {
        mStdDev = new float[mHVCount][mCatCount];
        mErrorMargin = new float[mHVCount][mCatCount];
        VisualizationPoint[] point = visualization.getDataPoints();
        int chartType = visualization.getChartType();
        for (VisualizationPoint vp:point) {
            if (((chartType == JVisualization.cChartTypeBars || chartType == JVisualization.cChartTypePies)
                    && visualization.isVisibleInBarsOrPies(vp))
             || ((chartType == JVisualization.cChartTypeBoxPlot || chartType == JVisualization.cChartTypeWhiskerPlot || chartType == JVisualization.cChartTypeViolins)
                    && visualization.isVisibleExcludeNaN(vp))) {
                int hv = vp.hvIndex;
                int cat = visualization.getChartCategoryIndex(vp);
                float d = visualization.getValue(vp.record, axis, column) - mMean[hv][cat];
                mStdDev[hv][cat] += d*d;
            }
        }
        for (int hv = 0; hv<mHVCount; hv++) {
            for (int cat=0; cat<mCatCount; cat++) {
                if (vCount[hv][cat] <= 1) {
                    mStdDev[hv][cat] = Float.POSITIVE_INFINITY;
                    mErrorMargin[hv][cat] = Float.POSITIVE_INFINITY;
                }
                else {
                    mStdDev[hv][cat] = (float)Math.sqrt(mStdDev[hv][cat] /= (vCount[hv][cat] - 1));
                    mErrorMargin[hv][cat] = 1.96f * mStdDev[hv][cat] / (float)Math.sqrt(vCount[hv][cat]);
                }
            }
        }
    }

    public boolean useProportionalFractions() {
        return mAbsValueSum != null;
        }
    public boolean useProportionalWidths() {
        return mAbsColorWidthSum != null;
    }

    public abstract boolean isChartWithMarkers();
    public abstract boolean paintMarker(VisualizationPoint vp);
    public abstract void calculate();

    public abstract void paint(Graphics2D g, Rectangle baseBounds, Rectangle baseGraphRect);

    /**
     * @return rough estimate of how many characters the longest label will be
     *
    private int getLabelWidth() {
    int width = FLOAT_DIGITS;
    if (isShowConfidenceInterval())
    width = Math.max(width, 8+2*FLOAT_DIGITS);
    if (isShowMeanAndMedianValues())
    width = Math.max(width, 7+FLOAT_DIGITS);
    if (isShowStandardDeviation())
    width = Math.max(width, 4+FLOAT_DIGITS);
    if (isShowValueCount())
    width = Math.max(width, 3+(int)Math.log10(mTableModel.getTotalRowCount()));  // N=nnn
    if (isShowBarOrPieSizeValue())
    width = Math.max(width, getBarOrPieSizeValueDigits());

    return width;
    }*/
 	}
