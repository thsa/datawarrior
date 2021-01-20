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

import java.awt.Color;

public class CategoryViewInfo {
    int[][] pointsInCategory;   // in case of box plot this excludes outliers
    int[][][] pointsInColorCategory;
    Color[] color;
    float[][] barValue;         // in case of sum mode: sum of individual values; in case of mean mode: sum/count
    float[][] absValueSum;      // only used in case of mean or sum. As barValue, but sum is built from abs() of individual values
    float[][] absValueFactor;   // factor from abs(record.getDouble()) to screen bar fraction height
    float[][][] absColorValueSum;   // same as absValueSum, but split into the color category sections
    float[][][] absColorWidthSum;   // only used in case of variable widths, i.e. if a marker size column is selected
    float[][] mean;
    float[][] stdDev;	// standard deviation (sigma-1)
    float[][] errorMargin;// (95 % confidence)
    float[][] innerDistance;   // distance of two adjacent sub-bar areas in bar
    float[][] pieX;
    float[][] pieY;
    float[][] pieSize;         // pie size in pixel
    float axisMin,axisMax,barBase;
    float barWidth,maxWidth;
    int barAxis;
    boolean barOrPieDataAvailable;

    public CategoryViewInfo(int hvCount, int catCount, int colorCount, int mode, boolean variableWidth) {
        pointsInCategory = new int[hvCount][catCount];
        pointsInColorCategory = new int[hvCount][catCount][colorCount];
        barValue = new float[hvCount][catCount];
        color = new Color[colorCount];
        if (mode != JVisualization.cChartModeCount
         && mode != JVisualization.cChartModePercent) {
            mean = new float[hvCount][catCount];
            absValueSum = new float[hvCount][catCount];
            absColorValueSum = new float[hvCount][catCount][colorCount];
            }
        if (variableWidth) {
            absColorWidthSum = new float[hvCount][catCount][colorCount];
            }
        }

    /**
     * During the chart calculation phase adds a contributing (i.e. visible) row to the chart
     * @param hvIndex
     * @param cat
     * @param colorIndex
     * @param chartValue may be NaN if the chart does not use a chart value, e.g. bar chart showing counts or percent
     * @param sizeValue (barcharts only) may be NaN if the bar chart does not use a width value, e.g. marker size is unassigned
     */
    public void addValue(int hvIndex, int cat, int colorIndex, float chartValue, float sizeValue) {
        pointsInCategory[hvIndex][cat]++;
        pointsInColorCategory[hvIndex][cat][colorIndex]++;
        if (absValueSum != null)
            absValueSum[hvIndex][cat] += Math.abs(chartValue);
        if (absColorValueSum != null)
            absColorValueSum[hvIndex][cat][colorIndex] += Math.abs(chartValue);
        if (mean != null)
            mean[hvIndex][cat] += chartValue;
        if (absColorWidthSum != null) {
            if (!Float.isNaN(sizeValue)) {
                float absSizeValue = Math.abs(sizeValue);
                absColorWidthSum[hvIndex][cat][colorIndex] += absSizeValue;
                if (maxWidth < absSizeValue)
                    maxWidth = absSizeValue;
                }
            }
        }

    public float getBarWidthFactor(float sizeValue) {
        return Float.isNaN(sizeValue) ? 0f : 2f * Math.abs(sizeValue) / maxWidth;
        }

    public boolean useProportionalFractions() {
        return absValueSum != null;
        }
    public boolean useProportionalWidths() {
        return absColorWidthSum != null;
    }
 	}
