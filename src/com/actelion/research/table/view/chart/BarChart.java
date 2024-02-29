package com.actelion.research.table.view.chart;

import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.JVisualization2D;
import com.actelion.research.table.view.VisualizationPoint;
import com.actelion.research.table.view.VisualizationSplitter;
import com.actelion.research.util.ColorHelper;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class BarChart extends AbstractBarOrPieChart {
	private static final float cMaxBarReduction = 0.66f;

	private float[][] mBarPosition = new float[mHVCount][mCatCount];
	private float[][][] mBarColorEdge = new float[mHVCount][mCatCount][mColor.length+1];


	public BarChart(JVisualization visualization, int hvCount, int mode) {
		super(visualization, hvCount, mode);
	}

	@Override
	public float getScaleLinePosition(int axis) {
		return axis == mDoubleAxis ? (mBarBase - mAxisMin) / (mAxisMax - mAxisMin) : super.getScaleLinePosition(axis);
	}

	@Override
	public void calculateCoordinates(Rectangle baseGraphRect) {
		if (!isBarOrPieDataAvailable())
			return;

		float axisRange = mAxisMax - mAxisMin;
		int categoryVisCount0 = mVisualization.getCategoryVisCount(0);;
		int categoryVisCount1 = mVisualization.getCategoryVisCount(1);

		float cellWidth = (mDoubleAxis == 1) ?
				(float)baseGraphRect.width / (float)categoryVisCount0
				: (float)baseGraphRect.height / (float)categoryVisCount1;
		float cellHeight = (mDoubleAxis == 1) ?
				(float)baseGraphRect.height / (float)categoryVisCount1
				: (float)baseGraphRect.width / (float)categoryVisCount0;

		// if we need space for statistics labels, then reduce the area that we have for the bar
		float spacing = cellHeight * getBarChartEmptyLabelAreaSpacing();
		float labelSize = calculateStatisticsLabelSize(mDoubleAxis == 0, spacing);

		int caseSeparationCategoryCount = mVisualization.getCaseSeparationCategoryCount();

		mBarWidth = Math.max(1, Math.max(0.05f * cellWidth / caseSeparationCategoryCount,
				mVisualization.getMarkerSize() * Math.min(0.2f * cellHeight, (caseSeparationCategoryCount == 1) ?
						0.6f * cellWidth : 0.5f * cellWidth / caseSeparationCategoryCount)));

		float barBaseOffset = (mBarBase - mAxisMin) * cellHeight / axisRange;

		if (labelSize != 0f && isRightBarChart())
			barBaseOffset = cellHeight;

		if (useProportionalFractions())
			mAbsValueFactor = new float[mHVCount][mCatCount];
		else
			mInnerDistance = new float[mHVCount][mCatCount];

		mBarPosition = new float[mHVCount][mCatCount];
		mBarColorEdge = new float[mHVCount][mCatCount][mColor.length+1];
		float csWidth = (mDoubleAxis == 1 ? cellWidth : -cellWidth)
				* mVisualization.getCaseSeparationValue() / caseSeparationCategoryCount;
		float csOffset = csWidth * (1 - caseSeparationCategoryCount) / 2.0f;
		for (int hv = 0; hv<mHVCount; hv++) {
			int hOffset = 0;
			int vOffset = 0;
			if (mVisualization.isSplitView()) {
				VisualizationSplitter splitter = mVisualization.getSplitter();
				hOffset = splitter.getHIndex(hv) * splitter.getGridWidth();
				vOffset = splitter.getVIndex(hv) * splitter.getGridHeight();
			}

			for (int i = 0; i<mVisualization.getCategoryVisCount(0); i++) {
				for (int j = 0; j<mVisualization.getCategoryVisCount(1); j++) {
					for (int k=0; k<caseSeparationCategoryCount; k++) {
						int cat = (i+j* mVisualization.getCategoryVisCount(0))*caseSeparationCategoryCount+k;
						if (mPointsInCategory[hv][cat] > 0) {
							float barHeight = cellHeight * Math.abs(mBarValue[hv][cat] - mBarBase) / axisRange;
							if (useProportionalFractions())
								mAbsValueFactor[hv][cat] = (mAbsValueSum[hv][cat] == 0f) ? 0f : barHeight / mAbsValueSum[hv][cat];
							else
								mInnerDistance[hv][cat] = barHeight / (float)mPointsInCategory[hv][cat];
							mBarPosition[hv][cat] = (mDoubleAxis == 1) ?
									baseGraphRect.x + hOffset + i*cellWidth + cellWidth/2
									: baseGraphRect.y + vOffset + baseGraphRect.height - j*cellWidth - cellWidth/2;

							if (caseSeparationCategoryCount != 1)
								mBarPosition[hv][cat] += csOffset + k*csWidth;

							// right bound bars have negative values and the barBase set to 0f (==axisMax).
							// Move them left by one bar length and extend them to the right (done by positive inner distance).
							float barOffset = isRightBarChart() || (isCenteredBarChart() && mBarValue[hv][cat] < 0f) ?
									cellHeight * (mBarValue[hv][cat] - mBarBase) / axisRange : 0f;
							mBarColorEdge[hv][cat][0] = (mDoubleAxis == 1) ?
									baseGraphRect.y + vOffset - barBaseOffset - barOffset + baseGraphRect.height - cellHeight * j
									: baseGraphRect.x + hOffset + barBaseOffset + barOffset + cellHeight * i;

							if (useProportionalFractions()) {
								for (int l=0; l<mColor.length; l++) {
									float size = (mAbsValueSum[hv][cat] == 0f) ? 0f : mAbsColorValueSum[hv][cat][l]
											* barHeight / mAbsValueSum[hv][cat];
									mBarColorEdge[hv][cat][l+1] = (mDoubleAxis == 1) ?
											mBarColorEdge[hv][cat][l] - size : mBarColorEdge[hv][cat][l] + size;
								}
							}
							else {  // calculate color edge position based on color category counts
								for (int l=0; l<mColor.length; l++) {
									float size = barHeight * mPointsInColorCategory[hv][cat][l]
											/ mPointsInCategory[hv][cat];
									mBarColorEdge[hv][cat][l + 1] = (mDoubleAxis == 1) ?
											mBarColorEdge[hv][cat][l] - size : mBarColorEdge[hv][cat][l] + size;
								}
							}
						}
					}
				}
			}
		}

		VisualizationPoint[] point = mVisualization.getDataPoints();
		int chartColumn = mVisualization.getChartType().getColumn();
		int sizeColumn = mVisualization.getMarkerSizeColumn();

		float[][][] colorFractionEdge = null;
		if (useProportionalFractions()) {
			colorFractionEdge = new float[mBarColorEdge.length][][];
			for (int i=0; i<mBarColorEdge.length; i++) {
				colorFractionEdge[i] = new float[mBarColorEdge[i].length][];
				for (int j=0; j<mBarColorEdge[i].length; j++)
					colorFractionEdge[i][j] = mBarColorEdge[i][j].clone();
			}
		}

		for (VisualizationPoint vp:point) {
			if (mVisualization.isVisibleInBarsOrPies(vp)) {
				int hv = vp.hvIndex;
				int cat = mVisualization.getChartCategoryIndex(vp);
				int colorIndex = mVisualization.getColorIndex(vp, mBaseColorCount, mFocusFlagNo);

				float width = mBarWidth;
				if (useProportionalWidths())
					width *= getBarWidthFactor(vp.record.getDouble(sizeColumn));

				if (useProportionalFractions()) {
					if (mDoubleAxis == 1) {
						vp.screenX = mBarPosition[hv][cat];
						vp.widthOrAngle1 = width;
						float fractionHeight = Math.abs(vp.record.getDouble(chartColumn))
								* mAbsValueFactor[hv][cat];
						vp.screenY = colorFractionEdge[hv][cat][colorIndex] - 0.5f * fractionHeight;
						colorFractionEdge[hv][cat][colorIndex] -= fractionHeight;
						vp.heightOrAngle2 = fractionHeight;
					}
					else {
						float fractionHeight = Math.abs(vp.record.getDouble(chartColumn))
								* mAbsValueFactor[hv][cat];
						vp.screenX = colorFractionEdge[hv][cat][colorIndex] + 0.5f * fractionHeight;
						colorFractionEdge[hv][cat][colorIndex] += fractionHeight;
						vp.widthOrAngle1 = fractionHeight;
						vp.screenY = mBarPosition[hv][cat];
						vp.heightOrAngle2 = width;
					}
				}
				else {
					if (mDoubleAxis == 1) {
						vp.screenX = mBarPosition[hv][cat];
						vp.widthOrAngle1 = width;
						vp.screenY = mBarColorEdge[hv][cat][0] - mInnerDistance[hv][cat] * (0.5f + vp.chartGroupIndex);
						vp.heightOrAngle2 = mInnerDistance[hv][cat];
					}
					else {
						vp.screenX = mBarColorEdge[hv][cat][0] + mInnerDistance[hv][cat] * (0.5f + vp.chartGroupIndex);
						vp.widthOrAngle1 = mInnerDistance[hv][cat];
						vp.screenY = mBarPosition[hv][cat];
						vp.heightOrAngle2 = width;
					}
				}
			}
		}
	}

	@Override
	public void paint(Graphics2D g, Rectangle baseBounds, Rectangle baseGraphRect) {
		if (!isBarOrPieDataAvailable())
			return;

		Composite original = null;
		float markerTransparency = ((JVisualization2D)mVisualization).getMarkerTransparency();
		if (markerTransparency != 0.0) {
			original = g.getComposite();
			Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(1.0-markerTransparency));
			g.setComposite(composite);
		}

		boolean drawOutline = ((JVisualization2D)mVisualization).isDrawBarPieBoxOutline();
		Color outlineGray = mVisualization.getContrastGrey(JVisualization2D.MARKER_OUTLINE);

		if (!useProportionalWidths()) {
			for (int hv = 0; hv<mHVCount; hv++) {
				for (int cat=0; cat<mCatCount; cat++) {
					for (int k=0; k<mColor.length; k++) {
						if (mPointsInColorCategory[hv][cat][k] > 0) {
							float width = mBarWidth;
							g.setColor(mColor[k]);
							if (mDoubleAxis == 1)
								g.fillRect(Math.round(mBarPosition[hv][cat]-width/2),
										Math.round(mBarColorEdge[hv][cat][k+1]),
										Math.round(width),
										Math.round(mBarColorEdge[hv][cat][k])-Math.round(mBarColorEdge[hv][cat][k+1]));
							else
								g.fillRect(Math.round(mBarColorEdge[hv][cat][k]),
										Math.round(mBarPosition[hv][cat]-width/2),
										Math.round(mBarColorEdge[hv][cat][k+1]-Math.round(mBarColorEdge[hv][cat][k])),
										Math.round(width));
						}
					}
					if (drawOutline && mPointsInCategory[hv][cat] > 0) {
						g.setColor(outlineGray);
						if (mDoubleAxis == 1)
							g.drawRect(Math.round(mBarPosition[hv][cat]- mBarWidth /2),
									Math.round(mBarColorEdge[hv][cat][mColor.length]),
									Math.round(mBarWidth),
									Math.round(mBarColorEdge[hv][cat][0])-Math.round(mBarColorEdge[hv][cat][mColor.length]));
						else
							g.drawRect(Math.round(mBarColorEdge[hv][cat][0]),
									Math.round(mBarPosition[hv][cat]- mBarWidth /2),
									Math.round(mBarColorEdge[hv][cat][mColor.length])-Math.round(mBarColorEdge[hv][cat][0]),
									Math.round(mBarWidth));
					}
				}
			}
		}

		if (getLabelCount() != 0) {
			boolean labelIsLeftOrBelow = isRightBarChart();
			String[] lineText = new String[5];

			int scaledFontHeight = ((JVisualization2D)mVisualization).setRelativeFontHeightAndScaleToSplitView(STATISTIC_LABEL_FONT_FACTOR);
			g.setColor(mVisualization.getContrastGrey(JVisualization.SCALE_STRONG));
			for (int hv = 0; hv<mHVCount; hv++) {
				for (int cat=0; cat<mCatCount; cat++) {
					if (mPointsInCategory[hv][cat] > 0) {
						int lineCount = compileStatisticsLines(hv, cat, lineText);

						if (mDoubleAxis == 1) {
							float x0 = mBarPosition[hv][cat];
							float y0 = labelIsLeftOrBelow ?
									mBarColorEdge[hv][cat][0] + scaledFontHeight
									: mBarColorEdge[hv][cat][mColor.length] - ((float)lineCount - 0.7f) * scaledFontHeight;
							for (int line=0; line<lineCount; line++) {
								float x = x0 - g.getFontMetrics().stringWidth(lineText[line])/2f;
								float y = y0 + line*scaledFontHeight;
								g.drawString(lineText[line], Math.round(x), Math.round(y));
							}
						}
						else {
							float x0 = labelIsLeftOrBelow ?
									mBarColorEdge[hv][cat][0] - scaledFontHeight/2f
									: mBarColorEdge[hv][cat][mColor.length] + scaledFontHeight/2f;
							float y0 = mBarPosition[hv][cat] - ((lineCount-1)*scaledFontHeight)/2f + g.getFontMetrics().getAscent()/2f;
							for (int line=0; line<lineCount; line++) {
								float x = x0 - (labelIsLeftOrBelow ? g.getFontMetrics().stringWidth(lineText[line]) : 0f);
								float y = y0 + line*scaledFontHeight;
								g.drawString(lineText[line], Math.round(x), Math.round(y));
							}
						}
					}
				}
			}
		}

		VisualizationPoint[] point = mVisualization.getDataPoints();

		for (VisualizationPoint vp:point) {
			if (mVisualization.isVisibleInBarsOrPies(vp)) {
				int colorIndex = mVisualization.getColorIndex(vp, mBaseColorCount, mFocusFlagNo);
				if (useProportionalWidths()) {
					g.setColor(mColor[colorIndex]);
					g.fill(new Rectangle2D.Float(vp.screenX-vp.widthOrAngle1/2f, vp.screenY-vp.heightOrAngle2/2f, vp.widthOrAngle1, vp.heightOrAngle2));
					if (drawOutline) {
						g.setColor(outlineGray);
						g.draw(new Rectangle2D.Float(vp.screenX-vp.widthOrAngle1/2f, vp.screenY-vp.heightOrAngle2/2f, vp.widthOrAngle1, vp.heightOrAngle2));
					}
				}
			}
		}

		if (original != null)
			g.setComposite(original);

		if (mVisualization.showAnyLabels()) {
			JVisualization2D.LabelHelper labelHelper = ((JVisualization2D)mVisualization).createLabelHelper(baseBounds, baseGraphRect);
			labelHelper.calculateLabels();

			if (mVisualization.isOptimizeLabelPositions())
				labelHelper.optimizeLabels();

			float labelTransparency = ((JVisualization2D)mVisualization).getMarkerLabelTransparency();

			Composite labelComposite = null;
			if (labelTransparency != 0.0) {
				original = g.getComposite();
				labelComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(1.0 - labelTransparency));
				g.setComposite(labelComposite);
			}

			boolean isDarkBackground = (ColorHelper.perceivedBrightness(mVisualization.getViewBackground()) <= 0.5);

			for (VisualizationPoint vp:point) {
				if (mVisualization.isVisibleInBarsOrPies(vp) && labelHelper.hasLabels(vp)) {
					Color fg = mColor[mVisualization.getColorIndex(vp, mBaseColorCount, mFocusFlagNo)];
					Color bg = mVisualization.getLabelBackgroundColor().getColorForBackground(vp.record, isDarkBackground);
					if (bg == null)
						bg = mVisualization.getLabelBackgroundColor().getDefaultDataColor();
					labelHelper.prepareLabels(vp);
					labelHelper.drawLabelLines(vp, fg, labelComposite);
					((JVisualization2D)mVisualization).drawMarkerLabels(labelHelper.getLabelInfo(), fg, bg, fg, false, labelComposite);
				}
			}

			if (labelComposite != null)
				g.setComposite(original);
		}
	}

	/**
	 * If we need space for statistics labels, then reduce the area that we have for the bar.
	 * If we show a scale reflecting the bar values, then we need to transform scale labels
	 * accordingly.
	 * @param baseRect
	 */
	@Override
	public void adaptDoubleScalesForStatisticalLabels(final Rectangle baseRect) {
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
}
