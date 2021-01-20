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

public class BoxPlotViewInfo extends CategoryViewInfo {
	float[][] boxQ1;	// 1st quartile
	float[][] median;	// 2nd quartile = median
	float[][] boxQ3;	// 3rd quartile
	float[][] boxUAV;	// upper adjacent value
	float[][] boxLAV;	// lower adjacent value
	float[][] pValue;	// t-test based p-value
	float[][] foldChange;// fold change compared to reference category
	int[][] outlierCount;// count of rows outside of LAV and UAV

    public BoxPlotViewInfo(int hvCount, int catCount, int colorCount) {
    	super(hvCount, catCount, colorCount, JVisualization.cChartModeCount, false);
    	}

    public boolean isOutsideValue(int hv, int cat, float value) {
    	return (value < boxLAV[hv][cat]) || (value > boxUAV[hv][cat]);
    	}
	}
