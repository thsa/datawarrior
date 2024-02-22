package com.actelion.research.table.view.chart;

import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationPoint;

public class ChartType {
	public static final int cTypeScatterPlot = 0;
	public static final int cTypeWhiskerPlot = 1;
	public static final int cTypeBoxPlot = 2;
	public static final int cTypeViolins = 3;
	public static final int cTypeRidgeLines = 4;
	public static final int cTypeBars = 5;
	public static final int cTypePies = 6;

	public static final String[] TYPE_NAME = { "Scatter Plot", "Whisker Plot", "Box Plot", "Violin Plot", "Ridge Line Plot", "Bar Chart", "Pie Chart" };
	public static final String[] TYPE_CODE = { "scatter", "whiskers", "boxes", "violins", "ridges", "bars", "pies" };

	public static final int cModeCount = 0;
	public static final int cModePercent = 1;
	public static final int cModeMean = 2;
	public static final int cModeMin = 3;
	public static final int cModeMax = 4;
	public static final int cModeSum = 5;

	public static final String[] MODE_NAME = { "Row Count", "Row Percentage", "Mean Value", "Minimum Value", "Maximum Value", "Sum of Values" };
	public static final String[] MODE_CODE = { "count", "percent", "mean", "min", "max", "sum" };

	private final int mDimensions;
	private int mType;
	private int mMode;
	private int mColumn;

	public ChartType(int dimensions) {
		mDimensions = dimensions;
		mType = cTypeScatterPlot;
		mMode = cModeCount;
		mColumn = JVisualization.cColumnUnassigned;
	}

	public int getType() {
		return mType;
	}

	public void setType(int type) {
		mType = type;
	}

	public int getMode() {
		return mMode;
	}

	public void setMode(int mode) {
		mMode = mode;
	}

	public int getColumn() {
		return mColumn;
	}

	public void setColumn(int column) {
		mColumn = column;
	}

	public boolean isShownAsMarker(VisualizationPoint vp) {
		return mType == cTypeScatterPlot
			|| mType == cTypeWhiskerPlot
			|| (mType == cTypeBoxPlot && vp.chartGroupIndex == -1)
			|| (mType == cTypeRidgeLines && vp.chartGroupIndex == -1)
			|| (mType == cTypeViolins && vp.chartGroupIndex == -1);
	}

	public boolean isShownAsBarFraction(VisualizationPoint vp) {
		return mType == cTypeBars
			|| (mType == cTypeBoxPlot && vp.chartGroupIndex == -1);
	}

	public boolean isShownAsOvalFraction(VisualizationPoint vp) {
		return (mType == cTypeRidgeLines && vp.chartGroupIndex == -1)
			|| (mType == cTypeViolins && vp.chartGroupIndex == -1);
	}

	public boolean isScatterPlot() {
		// dedicated check for scatter plot, which is different from all other plots, because it has no associated ChartInfo object
		return mType == cTypeScatterPlot;
	}
	/**
	 * @return whether chart mode is proportional to count (i.e. mode is count or percent)
	 */
	public boolean isSimpleMode() {
		return mMode == cModeCount
			|| mMode == cModePercent;
	}

	/**
	 * @return plot specific margin or -1f to use the view's mScatterPlotMargin
	 */
	public float getMargin() {
		return mType == cTypeViolins ? ViolinPlot.DEFAULT_MARGIN : -1f;
	}

	public AbstractChart createChartInfo(JVisualization visualization, int hvCount) {
		return
				mType == cTypeBoxPlot ? new BoxPlot(visualization, hvCount, visualization.determineBoxPlotDoubleAxis())
			  : mType == cTypeWhiskerPlot ? new WhiskerPlot(visualization, hvCount, visualization.determineBoxPlotDoubleAxis())
			  : mType == cTypeViolins ? new ViolinPlot(visualization, hvCount, visualization.determineBoxPlotDoubleAxis())
			  : mType == cTypeRidgeLines ? new RidgeLinePlot(visualization, hvCount, visualization.determineBoxPlotDoubleAxis())
			  : mType == cTypeBars ? new BarChart(visualization, hvCount, mMode)
			  : mType == cTypePies ? new PieChart(visualization, hvCount, mMode) : null;
	}

	public boolean displaysMarkers() {
		// all distribution plots may show markers, even if violin and ridge line plots show them in the NaN area only
		return mType != cTypeBars
			&& mType != cTypePies;
	}

	public boolean supportsOutliers() {
		return mType == cTypeBoxPlot;
	}

	public boolean supportsLabels() {
		return mDimensions == 2
			|| mType == cTypeScatterPlot;
	}

	public boolean supportsBackgroundColor() {
		return mDimensions == 2
			&& mType != cTypeBars
			&& mType != cTypeBoxPlot;
	}

	public boolean supportsTransparency() {
		return mDimensions == 2;
	}

	public boolean supportsConnectionLines() {
		return mType != cTypeBars
			&& mType != cTypePies;
	}

	public boolean supportsStatistics() {
		return mDimensions == 2;
	}

	public boolean supportsShowMeanAndMedian() {
		if (isBarOrPieChart(mType))
			return mMode != cModePercent
				&& mMode != cModeCount;
		return isDistributionPlot(mType);
	}

	public boolean supportsShowStdDevAndErrorMergin() {
		if (mType == cTypeBars
		 || mType == cTypePies)
			return mMode != cModePercent
				&& mMode != cModeCount;
		return isDistributionPlot();
	}

	public boolean supportsShowValueCount() {
		return isBarOrPieChart(mType)
			|| isDistributionPlot(mType);
	}

	public boolean supportsPValues() {
		return isDistributionPlot();
	}

	public boolean supportsShowBarOrPieSizeValues() {
		return isBarOrPieChart(mType);
	}

	public boolean isDistributionPlot() {
		return isDistributionPlot(mType);
	}

	public boolean isBarOrPieChart() {
		return isBarOrPieChart(mType);
	}

	public static boolean isBarOrPieChart(int chartType) {
		return chartType == cTypeBars
			|| chartType == cTypePies;
	}

	public static boolean isDistributionPlot(int chartType) {
		return chartType == cTypeBoxPlot
				|| chartType == cTypeWhiskerPlot
				|| chartType == cTypeRidgeLines
				|| chartType == cTypeViolins;
	}

	public static String getName(int chartType) {
		return chartType == cTypeBars ? "bar chart"
			 : chartType == cTypePies ? "pie chart"
			 : chartType == cTypeBoxPlot ? "box plot"
			 : chartType == cTypeWhiskerPlot ? "whisker plot"
			 : chartType == cTypeViolins ? "violin plot"
			 : chartType == cTypeRidgeLines ? "ridge line plot" : "scatter plot";
	}

	public String getModeText(String columnName) {
		return (mMode == cModePercent) ? "\tPercent of Rows"
			 : (mMode == cModeMean) ? "\tMean of "+columnName
			 : (mMode == cModeSum) ? "\tSum of "+columnName
			 : (mMode == cModeMin) ? "\tMinimum of "+columnName
			 : (mMode == cModeMax) ? "\tMaximum of "+columnName : "";
	}

	public boolean updateCoordsOnFocusOrSelectionChange() {
		return mType != cTypeScatterPlot
			&& mType != cTypeWhiskerPlot;
	}
}
