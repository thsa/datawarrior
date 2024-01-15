package com.actelion.research.table.view;

import com.actelion.research.util.DoubleFormat;

import java.awt.*;

public abstract class AbstractBarOrPieChart extends AbstractCategoryChart {
	private static final float cMaxBarReduction = 0.66f;
	protected static final float STATISTIC_LABEL_FONT_FACTOR = 0.8f;

	public static final float cAnchoredBarSpacing = 0.08f;  // for bars that touch one end of bar area this spacing is added to the other end
	public static final float cCenteredBarSpacing = 0.05f;  // for centered bars we add this spacing on both ends of bar area
	protected static final float cLogBarMinSizeFactor = 0.10f;    // bars on logarithic data have this as base height

	private int mChartMode;

	public AbstractBarOrPieChart(JVisualization visualization, int hvCount, int mode) {
		super(visualization, hvCount, -1);
		mChartMode = mode;
	}

	@Override
	public boolean isChartWithMarkers() {
		// neither NaN markers nor outliers
		return false;
	}

	@Override
	public boolean paintMarker(VisualizationPoint vp) {
		return false;
	}

	/**
	 * Based on axis column assignments and on hvIndices of VisualizationPoints
	 * this method assigns all visible VisualizationPoints to bars/pies and to color categories
	 * within these bars/pies. It also calculates relative bar/pie sizes.
	 */
	@Override
	public void calculate() {
		mPointsInCategory = new int[mHVCount][mCatCount];
		mPointsInColorCategory = new int[mHVCount][mCatCount][mColor.length];
		mBarValue = new float[mHVCount][mCatCount];
		if (mChartMode != JVisualization.cChartModeCount
		 && mChartMode != JVisualization.cChartModePercent) {
			mMean = new float[mHVCount][mCatCount];
			mAbsValueSum = new float[mHVCount][mCatCount];
			mAbsColorValueSum = new float[mHVCount][mCatCount][mColor.length];
		}

		if (mVisualization.getMarkerSizeColumn() != JVisualization.cColumnUnassigned) // variable width
			mAbsColorWidthSum = new float[mHVCount][mCatCount][mColor.length];

		double widthHeightRatio = 1.0;
		if (mVisualization.getDimensionCount() == 2) {
			Rectangle bounds = ((JVisualization2D)mVisualization).getAllBoundsWithoutLegend();
			if (bounds.height > 0)
				widthHeightRatio = (double)bounds.width / (double)bounds.height;
			VisualizationSplitter splitter = mVisualization.getSplitter();
			if (splitter != null)
				widthHeightRatio *= (double)splitter.getVCount() / (double)splitter.getHCount();
		}

		int dimensions = mVisualization.getDimensionCount();
		int assignedAxisCount = 0;
		for (int axis=0; axis<dimensions; axis++)
			if (mVisualization.getColumnIndex(axis) != JVisualization.cColumnUnassigned)
				assignedAxisCount++;

		// determine, which of the axis the bar is parallel to
		if (assignedAxisCount == 0 || assignedAxisCount == dimensions) {
			// we take the axis with the maximum space available
			double maxSpace = 0;
			for (int axis=0; axis<dimensions; axis++) {
				double relSpace = (axis == 0 ? widthHeightRatio : 1.0) / mVisualization.getCategoryVisCount(axis);
				if (maxSpace < relSpace) {
					maxSpace = relSpace;
					mDoubleAxis = axis;
				}
			}
		}
		else {
			mDoubleAxis = 0;
			for (int axis=1; axis<dimensions; axis++)
				if (mVisualization.getColumnIndex(axis) == JVisualization.cColumnUnassigned)
					mDoubleAxis = axis;
				else if (mVisualization.getColumnIndex(axis) != JVisualization.cColumnUnassigned
						&& mVisualization.getCategoryVisCount(axis) <= mVisualization.getCategoryVisCount(mDoubleAxis))
					mDoubleAxis = axis;
		}

		VisualizationPoint[] point = mVisualization.getDataPoints();
		int chartColumn = mVisualization.getChartColumn();
		int markerSizeColumn = mVisualization.getMarkerSizeColumn();

		for (VisualizationPoint vp:point) {
			if (mVisualization.isVisibleInBarsOrPies(vp)) {
				int cat = mVisualization.getChartCategoryIndex(vp);
				int colorIndex = mVisualization.getColorIndex(vp, mBaseColorCount, mFocusFlagNo);
				float chartValue = (chartColumn == -1) ? Float.NaN : vp.record.getDouble(chartColumn);
				float widthValue = (markerSizeColumn == -1) ? Float.NaN : vp.record.getDouble(markerSizeColumn);
				addValue(vp.hvIndex, cat, colorIndex, chartValue, widthValue);
			}
		}

		if (mMean != null)
			for (int i = 0; i<mHVCount; i++)
				for (int j = 0; j <mCatCount; j++)
					if (mPointsInCategory[i][j] != 0)
						mMean[i][j] /= mPointsInCategory[i][j];

		if (mMean != null)
			// calculate standard deviation and error margin using the values in mChartColumn
			calculateStdDevAndErrorMargin(mVisualization, mCatCount, mPointsInCategory, -1, chartColumn);

		int[][][] count = new int[mHVCount][mCatCount][mColor.length];
		for (int hv = 0; hv<mHVCount; hv++)
			for (int cat = 0; cat<mCatCount; cat++)
				for (int color = 1; color<mColor.length; color++)
					count[hv][cat][color] = count[hv][cat][color-1]+ mPointsInColorCategory[hv][cat][color-1];
		for (VisualizationPoint vp:point) {
			if (mVisualization.isVisibleInBarsOrPies(vp)) {
				int hv = vp.hvIndex;
				int cat = mVisualization.getChartCategoryIndex(vp);
				int colorIndex = mVisualization.getColorIndex(vp, mBaseColorCount, mFocusFlagNo);
				vp.chartGroupIndex = count[hv][cat][colorIndex];
				count[hv][cat][colorIndex]++;
			}
		}

		// generate all category bar values
		float[] dataMinAndMax = calculateBarDimensions(mBarValue, mPointsInCategory, true);

		// For a static scale, we need to calculate the absolute bar minimum and maximum of all bars
		// assuming all rows to be visible.
		if (!mVisualization.isDynamicScale()) {
			int allCatCount = mVisualization.getCaseSeparationCategoryCount();
			for (int axis=0; axis<dimensions; axis++)
				if (mVisualization.getColumnIndex(axis) != JVisualization.cColumnUnassigned)
					allCatCount *=  mVisualization.getTableModel().getCategoryCount(mVisualization.getColumnIndex(axis));

			int[][] localPointsInCategory = new int[mHVCount][allCatCount];
			for (VisualizationPoint vp:point) {
				if (!mVisualization.isNaN(vp) && vp.hvIndex != -1) {
					int cat = mVisualization.getFullCategoryIndex(vp);
					localPointsInCategory[vp.hvIndex][cat]++;
				}
			}

			float[] visOnlyMinAndMax = dataMinAndMax;

			dataMinAndMax = calculateBarDimensions(new float[mHVCount][allCatCount], localPointsInCategory, false);

			if (dataMinAndMax[0] > visOnlyMinAndMax[0])
				dataMinAndMax[0] = visOnlyMinAndMax[0];
			if (dataMinAndMax[1] < visOnlyMinAndMax[1])
				dataMinAndMax[1] = visOnlyMinAndMax[1];
		}

		mBarOrPieDataAvailable = (dataMinAndMax[0] != Float.POSITIVE_INFINITY);

		if (mBarOrPieDataAvailable) {
			switch (mChartMode) {
				case JVisualization.cChartModeCount:
				case JVisualization.cChartModePercent:
					mAxisMin = 0.0f;
					mAxisMax = dataMinAndMax[1] * (1f + cAnchoredBarSpacing);
					mBarBase = 0.0f;
					break;
				default:
					float maxMinusMin = dataMinAndMax[1] - dataMinAndMax[0];
					if (mVisualization.getTableModel().isLogarithmicViewMode(chartColumn)) {
						float spacing = cAnchoredBarSpacing * maxMinusMin;
						float minSize = cLogBarMinSizeFactor * maxMinusMin;
						mAxisMin = dataMinAndMax[0] - minSize;
						mAxisMax = dataMinAndMax[1] + spacing;
						mBarBase = mAxisMin;
					}
					else {
						if (dataMinAndMax[0] >= 0.0) {
							mAxisMin = 0.0f;
							mAxisMax = dataMinAndMax[1] * (1f + cAnchoredBarSpacing);
						}
						else if (dataMinAndMax[1] <= 0.0) {
							mAxisMin = dataMinAndMax[0] * (1f + cAnchoredBarSpacing);
							mAxisMax = 0.0f;
						}
						else {
							float spacing = cCenteredBarSpacing * maxMinusMin;
							mAxisMax = dataMinAndMax[1] + spacing;
							mAxisMin = dataMinAndMax[0] - spacing;
						}
						mBarBase = 0.0f;
					}
					break;
			}
		}
		else {
			mAxisMin = 0.0f;
			mAxisMax = 1.0f + cAnchoredBarSpacing;
			mBarBase = 0.0f;
		}
//System.out.println("calculateBarsOrPies() dataMin:"+dataMinAndMax[0]+" dataMax:"+dataMinAndMax[1]+" axisMin:"+axisMin+" axisMax:"+axisMax+" barBase:"+barBase);
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
		mPointsInCategory[hvIndex][cat]++;
		mPointsInColorCategory[hvIndex][cat][colorIndex]++;
		if (mAbsValueSum != null)
			mAbsValueSum[hvIndex][cat] += Math.abs(chartValue);
		if (mAbsColorValueSum != null)
			mAbsColorValueSum[hvIndex][cat][colorIndex] += Math.abs(chartValue);
		if (mMean != null)
			mMean[hvIndex][cat] += chartValue;
		if (mAbsColorWidthSum != null) {
			if (!Float.isNaN(sizeValue)) {
				float absSizeValue = Math.abs(sizeValue);
				mAbsColorWidthSum[hvIndex][cat][colorIndex] += absSizeValue;
				if (mMaxWidth< absSizeValue)
					mMaxWidth = absSizeValue;
			}
		}
	}

	/**
	 * Calculates the barValue of all bars, which is the basis for the complete bar or pie size.
	 * @param visibleOnly
	 * @return
	 */
	private float[] calculateBarDimensions(float[][] barValue, int[][] pointsInCategory, boolean visibleOnly) {
		VisualizationPoint[] point = mVisualization.getDataPoints();
		int chartColumn = mVisualization.getChartColumn();

		int count = 0;
		for (VisualizationPoint vp:point) {
			if ((visibleOnly && mVisualization.isVisibleInBarsOrPies(vp)) || (!visibleOnly && !mVisualization.isNaNInBarsOrPies(vp) && vp.hvIndex != -1)) {
				int cat = visibleOnly ? mVisualization.getChartCategoryIndex(vp) : mVisualization.getFullCategoryIndex(vp);
				count++;
				switch (mChartMode) {
					case JVisualization.cChartModeCount:
					case JVisualization.cChartModePercent:
						barValue[vp.hvIndex][cat]++;
						break;
					case JVisualization.cChartModeMin:
					case JVisualization.cChartModeMax:
						float value = vp.record.getDouble(chartColumn);
						if (pointsInCategory[vp.hvIndex][cat] == 1)
							barValue[vp.hvIndex][cat] = value;
						else if (mChartMode == JVisualization.cChartModeMin)
							barValue[vp.hvIndex][cat] = Math.min(barValue[vp.hvIndex][cat], value);
						else
							barValue[vp.hvIndex][cat] = Math.max(barValue[vp.hvIndex][cat], value);
						break;
					case JVisualization.cChartModeMean:
						barValue[vp.hvIndex][cat] += vp.record.getDouble(chartColumn);
						break;
					case JVisualization.cChartModeSum:
						if (mVisualization.getTableModel().isLogarithmicViewMode(chartColumn))
							barValue[vp.hvIndex][cat] += Math.pow(10, vp.record.getDouble(chartColumn));
						else
							barValue[vp.hvIndex][cat] += vp.record.getDouble(chartColumn);
						break;
				}
			}
		}

		if (mChartMode == JVisualization.cChartModePercent)
			for (int i = 0; i<mHVCount; i++)
				for (int j=0; j<barValue[i].length; j++)
					barValue[i][j] *= 100f / count;
		if (mChartMode == JVisualization.cChartModeMean)
			for (int i = 0; i<mHVCount; i++)
				for (int j=0; j<barValue[i].length; j++)
					barValue[i][j] /= pointsInCategory[i][j];
		if (mChartMode == JVisualization.cChartModeSum && mVisualization.getTableModel().isLogarithmicViewMode(chartColumn))
			for (int i = 0; i<mHVCount; i++)
				for (int j=0; j<barValue[i].length; j++)
					barValue[i][j] = (float)Math.log10(barValue[i][j]);

		float[] dataMinAndMax = new float[2];
		dataMinAndMax[0] = Float.POSITIVE_INFINITY;
		dataMinAndMax[1] = Float.NEGATIVE_INFINITY;
		for (int i = 0; i<mHVCount; i++) {
			for (int j=0; j<pointsInCategory[i].length; j++) {
				if (pointsInCategory[i][j] != 0) {
					if (dataMinAndMax[0] > barValue[i][j])
						dataMinAndMax[0] = barValue[i][j];
					if (dataMinAndMax[1] < barValue[i][j])
						dataMinAndMax[1] = barValue[i][j];
				}
			}
		}

		return dataMinAndMax;
	}


	private int getBarOrPieSizeValueDigits() {
		if (mChartMode == JVisualization.cChartModeCount)
			return 1 + (int)Math.log10(mVisualization.getTableModel().getTotalRowCount());
		if (mChartMode == JVisualization.cChartModePercent)
			return FLOAT_DIGITS;

		int chartColumn = mVisualization.getChartColumn();
		if (mVisualization.getTableModel().isColumnTypeInteger(chartColumn) && !mVisualization.getTableModel().isLogarithmicViewMode(chartColumn))
			return INT_DIGITS;

		return FLOAT_DIGITS;
	}

	protected int compileStatisticsLines(int hv, int cat, String[] lineText) {
		boolean usesCounts = (mChartMode == JVisualization.cChartModeCount || mChartMode == JVisualization.cChartModePercent);
		boolean isLogarithmic = usesCounts ? false : mVisualization.getTableModel().isLogarithmicViewMode(mVisualization.getChartColumn());

		int lineCount = 0;
		if (mVisualization.isShowBarOrPieSizeValue()) {
			double value = mChartMode == JVisualization.cChartModeCount
					|| mChartMode == JVisualization.cChartModePercent
					|| !mVisualization.getTableModel().isLogarithmicViewMode(mVisualization.getChartColumn()) ?
					mBarValue[hv][cat] : Math.pow(10.0, mBarValue[hv][cat]);
			lineText[lineCount++] = DoubleFormat.toString(value, getBarOrPieSizeValueDigits());
		}
		if (mVisualization.isShowMeanAndMedianValues()) {
			float meanValue = isLogarithmic ? (float) Math.pow(10, mMean[hv][cat]) : mMean[hv][cat];
			lineText[lineCount++] = "mean=" + DoubleFormat.toString(meanValue, FLOAT_DIGITS);
		}
		if (mVisualization.isShowStandardDeviation()) {
			if (Float.isInfinite(mStdDev[hv][cat])) {
				lineText[lineCount++] = "\u03C3=Infinity";
			}
			else {
				double sdev = isLogarithmic ? Math.pow(10, mStdDev[hv][cat]) : mStdDev[hv][cat];
				lineText[lineCount++] = "\u03C3=".concat(DoubleFormat.toString(sdev, FLOAT_DIGITS));
			}
		}
		if (mVisualization.isShowConfidenceInterval()) {
			if (Float.isInfinite(mErrorMargin[hv][cat])) {
				lineText[lineCount++] = "CI95: Infinity";
			}
			else {
				double ll = mBarValue[hv][cat] - mErrorMargin[hv][cat];
				double hl = mBarValue[hv][cat] + mErrorMargin[hv][cat];
				if (isLogarithmic) {
					ll = Math.pow(10, ll);
					hl = Math.pow(10, hl);
				}
				lineText[lineCount++] = "CI95: ".concat(DoubleFormat.toString(ll, FLOAT_DIGITS)).concat("-").concat(DoubleFormat.toString(hl, FLOAT_DIGITS));
			}
		}
		if (mVisualization.isShowValueCount()) {
			lineText[lineCount++] = "N=" + mPointsInCategory[hv][cat];
		}
		return lineCount;
	}

	/**
	 * If we need space for statistics labels, then reduce the area that we have for the bar.
	 * If we show a scale reflecting the bar values, then we need to transform scale labels
	 * accordingly.
	 * @param baseRect
	 */
	protected void adaptDoubleScalesForStatisticalLabels(final Rectangle baseRect) {
		if (mVisualization.getChartType() == JVisualization.cChartTypeBars) {
			for (int axis=0; axis<2; axis++) {
				if (axis == mDoubleAxis) {
					float cellSize = (axis == 0 ? baseRect.width : baseRect.height) / (float)mVisualization.getCategoryVisCount(axis);
					float spacing = cellSize * getBarChartEmptyLabelAreaSpacing();
					float labelSize = calculateStatisticsLabelSize(axis == 0, spacing);
					if (labelSize != 0f) {
						float reduction = Math.min(cMaxBarReduction * cellSize, labelSize - spacing);
						float appliedHeight = (cellSize - reduction);

						// if the axis is not assigned to a category column, then we have a double value scale
						if (mVisualization.getColumnIndex(axis) == JVisualization.cColumnUnassigned) {
							float shift = isRightBarChart() ? reduction / cellSize : 0f;
							((JVisualization2D)mVisualization).transformScaleLinePositions(axis, shift, appliedHeight / cellSize);
						}
						// if the bar axis shows category values, then we correct centered bars only: label must be at bar base
						else if (isCenteredBarChart()) {
							float lowFraction = mAxisMin / (mAxisMin - mAxisMax);
							float shift = (lowFraction * appliedHeight / cellSize - lowFraction) / (float)mVisualization.getCategoryVisCount(axis);
							((JVisualization2D)mVisualization).transformScaleLinePositions(axis, shift, 1f);
						}

						float originalRange = mAxisMax - mAxisMin;
						if (!isRightBarChart())
							mAxisMax = mAxisMin + originalRange * cellSize / appliedHeight;
						else
							mAxisMin = mAxisMax - originalRange * cellSize / appliedHeight;
					}
				}
			}
		}
		else if (mVisualization.getChartType() == JVisualization.cChartTypePies) {
			if (mVisualization.getColumnIndex(1) != JVisualization.cColumnUnassigned) {
				float cellHeight = baseRect.height / (float)mVisualization.getCategoryVisCount(1);
				float labelSize = Math.min(cellHeight / 2f, calculateStatisticsLabelSize(false, 0f));
				if (labelSize != 0f) {
					float shift = labelSize / (2f * baseRect.height);
					((JVisualization2D)mVisualization).transformScaleLinePositions(1, shift, 1f);
				}
			}
		}
	}

	/**
	 * Considering the current font this method calculates the space needed to paint the
	 * selected statistical values into bar/pie charts including padding.
	 * If the needed space doesn't exceed the given threshold, then 0 is returned.
	 * @param isWidth true in case of horizontal bars, false otherwise
	 * @param threshold minimum size value that will be returned as non-zero value
	 * @return size needed to display statistics labels: height or width
	 */
	protected float calculateStatisticsLabelSize(boolean isWidth, float threshold) {
		int labelCount = getLabelCount();
		if (labelCount == 0)
			return 0f;

		float scaledFontHeight = ((JVisualization2D)mVisualization).getFontHeightScaledToSplitView();
		float size = isWidth ? scaledFontHeight + calculateStatisticalLabelWidth(labelCount) : scaledFontHeight * (0.5f + labelCount);
		return size > threshold ? size : 0f;
	}

	/**
	 * Returns the string width of the longest statistical label shown in bar/box/whisker/pie chart
	 * @param labelCount
	 * @return
	 */
	private float calculateStatisticalLabelWidth(int labelCount) {
		((JVisualization2D)mVisualization).setRelativeFontHeightAndScaleToSplitView(STATISTIC_LABEL_FONT_FACTOR);
		int width = 0;
		String[] lineText = new String[labelCount];
		for (int hv = 0; hv<mHVCount; hv++) {
			for (int i = 0; i<mVisualization.getCategoryVisCount(0); i++) {
				for (int j = 0; j<mVisualization.getCategoryVisCount(1); j++) {
					for (int k = 0; k<mVisualization.getCaseSeparationCategoryCount(); k++) {
						int cat = (i+j* mVisualization.getCategoryVisCount(0))* mVisualization.getCaseSeparationCategoryCount()+k;
						int count = compileStatisticsLines(hv, cat, lineText);
						for (int l=0; l<count; l++)
							width = Math.max(width, mVisualization.getStringWidth(lineText[l]));
					}
				}
			}
		}
		return width;
	}

	/**
	 * @return true if bars of a bar chart are based on the left/bottom edge
	 *
	protected boolean isLeftBarChart() {
		if (axisMin == 0)
			return true;
		int chartColumn = visualization.getChartColumn();
		return (chartColumn != -1 && visualization.getTableModel().isLogarithmicViewMode(chartColumn));
	}   */

	/**
	 * @return true if bars of a bar chart are based on the right/top edge
	 */
	protected boolean isRightBarChart() {
		return mAxisMax == 0;
	}

	/**
	 * @return true if bars of a bar chart are centered in view
	 */
	protected boolean isCenteredBarChart() {
		if (mAxisMin == 0 || mAxisMax == 0)
			return false;
		int chartColumn = mVisualization.getChartColumn();
		return (chartColumn != -1 && !mVisualization.getTableModel().isLogarithmicViewMode(chartColumn));
	}

	/**
	 * @return true if bars of a bar chart are based on the left/bottom edge
	 */
	protected boolean isLogarithmicBarOrPieChart() {
		int chartColumn = mVisualization.getChartColumn();
		return (chartColumn != -1 && mVisualization.getTableModel().isLogarithmicViewMode(chartColumn));
	}

	/**
	 * Calculates depending on the bar chart type (logarithmic, centered, left- or right-anchored)
	 * the fraction of the default spacing within a graph cell, which multiplied with the cell size
	 * would give the size of the empty spacing in graph coordinates as long as no labels are shown.
	 * If labels are shown, they use this area and enlarge it on demand while reducing the
	 * actual bar area accordingly.
	 * @return fraction of cell that is used as empty space on that size of the bar, where labels may be shown
	 */
	protected float getBarChartEmptyLabelAreaSpacing() {
		if (isLogarithmicBarOrPieChart())
			return cAnchoredBarSpacing / (1f + cLogBarMinSizeFactor + cAnchoredBarSpacing);
		if (isCenteredBarChart())
			return cCenteredBarSpacing / (1f + 2f * cCenteredBarSpacing);
		else
			return cAnchoredBarSpacing / (1f + cAnchoredBarSpacing);
	}

	/**
	 * Calculates depending on the bar chart type (logarithmic, centered, left- or right-anchored)
	 * the fraction of the total spacing within a graph cell. This is the sum of left/top and
	 * right/bottom spacing fractions of which one of them is 0.0 for anchored bars. In case of
	 * logarithmic bars the drawn base part of the bar is considered spacing and included in this
	 * fraction.
	 * @return fraction of cell that is not occupied by the bar area (or base part of bar in case of logarithmic bars)
	 */
	protected float getBarChartTotalSpacing() {
		if (isLogarithmicBarOrPieChart())
			return (cAnchoredBarSpacing + cLogBarMinSizeFactor) / (1f + cLogBarMinSizeFactor + cAnchoredBarSpacing);
		if (isCenteredBarChart())
			return 2f * cCenteredBarSpacing / (1f + 2f * cCenteredBarSpacing);
		else
			return cAnchoredBarSpacing / (1f + cAnchoredBarSpacing);
	}

	/**
	 * @return number of labels, i.e. label line count in bar or pie charts
	 */
	protected int getLabelCount() {
		int count = 0;
		if (mVisualization.isShowValueCount())
			count++;
		if (mVisualization.isShowBarOrPieSizeValue())
			count++;
		if (mVisualization.isShowConfidenceInterval())
			count++;
		if (mVisualization.isShowMeanAndMedianValues())
			count++;
		if (mVisualization.isShowStandardDeviation())
			count++;
		return count;
	}
}
