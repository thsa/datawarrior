package com.actelion.research.table.view;

import com.actelion.research.util.ColorHelper;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class BarChart extends AbstractBarOrPieChart {
	public BarChart(JVisualization visualization, int hvCount, int mode) {
		super(visualization, hvCount, mode);
	}

	@Override
	public void paint(Graphics2D g, Rectangle baseBounds, Rectangle baseGraphRect) {
		if (!mBarOrPieDataAvailable)
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

		float[][] barPosition = new float[mHVCount][mCatCount];
		float[][][] barColorEdge = new float[mHVCount][mCatCount][mColor.length+1];
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
							barPosition[hv][cat] = (mDoubleAxis == 1) ?
									baseGraphRect.x + hOffset + i*cellWidth + cellWidth/2
									: baseGraphRect.y + vOffset + baseGraphRect.height - j*cellWidth - cellWidth/2;

							if (caseSeparationCategoryCount != 1)
								barPosition[hv][cat] += csOffset + k*csWidth;

							// right bound bars have negative values and the barBase set to 0f (==axisMax).
							// Move them left by one bar length and extend them to the right (done by positive inner distance).
							float barOffset = isRightBarChart() || (isCenteredBarChart() && mBarValue[hv][cat] < 0f) ?
									cellHeight * (mBarValue[hv][cat] - mBarBase) / axisRange : 0f;
							barColorEdge[hv][cat][0] = (mDoubleAxis == 1) ?
									baseGraphRect.y + vOffset - barBaseOffset - barOffset + baseGraphRect.height - cellHeight * j
									: baseGraphRect.x + hOffset + barBaseOffset + barOffset + cellHeight * i;

							if (useProportionalFractions()) {
								for (int l=0; l<mColor.length; l++) {
									float size = (mAbsValueSum[hv][cat] == 0f) ? 0f : mAbsColorValueSum[hv][cat][l]
											* barHeight / mAbsValueSum[hv][cat];
									barColorEdge[hv][cat][l+1] = (mDoubleAxis == 1) ?
											barColorEdge[hv][cat][l] - size : barColorEdge[hv][cat][l] + size;
								}
							}
							else {  // calculate color edge position based on color category counts
								for (int l=0; l<mColor.length; l++) {
									float size = barHeight * mPointsInColorCategory[hv][cat][l]
											/ mPointsInCategory[hv][cat];
									barColorEdge[hv][cat][l + 1] = (mDoubleAxis == 1) ?
											barColorEdge[hv][cat][l] - size : barColorEdge[hv][cat][l] + size;
								}
							}
						}
					}
				}
			}
		}

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
								g.fillRect(Math.round(barPosition[hv][cat]-width/2),
										Math.round(barColorEdge[hv][cat][k+1]),
										Math.round(width),
										Math.round(barColorEdge[hv][cat][k])-Math.round(barColorEdge[hv][cat][k+1]));
							else
								g.fillRect(Math.round(barColorEdge[hv][cat][k]),
										Math.round(barPosition[hv][cat]-width/2),
										Math.round(barColorEdge[hv][cat][k+1]-Math.round(barColorEdge[hv][cat][k])),
										Math.round(width));
						}
					}
					if (drawOutline && mPointsInCategory[hv][cat] > 0) {
						g.setColor(outlineGray);
						if (mDoubleAxis == 1)
							g.drawRect(Math.round(barPosition[hv][cat]- mBarWidth /2),
									Math.round(barColorEdge[hv][cat][mColor.length]),
									Math.round(mBarWidth),
									Math.round(barColorEdge[hv][cat][0])-Math.round(barColorEdge[hv][cat][mColor.length]));
						else
							g.drawRect(Math.round(barColorEdge[hv][cat][0]),
									Math.round(barPosition[hv][cat]- mBarWidth /2),
									Math.round(barColorEdge[hv][cat][mColor.length])-Math.round(barColorEdge[hv][cat][0]),
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
							float x0 = barPosition[hv][cat];
							float y0 = labelIsLeftOrBelow ?
									barColorEdge[hv][cat][0] + scaledFontHeight
									: barColorEdge[hv][cat][mColor.length] - ((float)lineCount - 0.7f) * scaledFontHeight;
							for (int line=0; line<lineCount; line++) {
								float x = x0 - g.getFontMetrics().stringWidth(lineText[line])/2f;
								float y = y0 + line*scaledFontHeight;
								g.drawString(lineText[line], Math.round(x), Math.round(y));
							}
						}
						else {
							float x0 = labelIsLeftOrBelow ?
									barColorEdge[hv][cat][0] - scaledFontHeight/2f
									: barColorEdge[hv][cat][mColor.length] + scaledFontHeight/2f;
							float y0 = barPosition[hv][cat] - ((lineCount-1)*scaledFontHeight)/2f + g.getFontMetrics().getAscent()/2f;
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
		int chartColumn = mVisualization.getChartColumn();
		int sizeColumn = mVisualization.getMarkerSizeColumn();

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
						vp.screenX = barPosition[hv][cat];
						vp.widthOrAngle1 = width;
						float fractionHeight = Math.abs(vp.record.getDouble(chartColumn))
								* mAbsValueFactor[hv][cat];
						vp.screenY = barColorEdge[hv][cat][colorIndex] - 0.5f * fractionHeight;
						barColorEdge[hv][cat][colorIndex] -= fractionHeight;
						vp.heightOrAngle2 = fractionHeight;
					}
					else {
						float fractionHeight = Math.abs(vp.record.getDouble(chartColumn))
								* mAbsValueFactor[hv][cat];
						vp.screenX = barColorEdge[hv][cat][colorIndex] + 0.5f * fractionHeight;
						barColorEdge[hv][cat][colorIndex] += fractionHeight;
						vp.widthOrAngle1 = fractionHeight;
						vp.screenY = barPosition[hv][cat];
						vp.heightOrAngle2 = width;
					}
				}
				else {
					if (mDoubleAxis == 1) {
						vp.screenX = barPosition[hv][cat];
						vp.widthOrAngle1 = width;
						vp.screenY = barColorEdge[hv][cat][0] - mInnerDistance[hv][cat] * (0.5f + vp.chartGroupIndex);
						vp.heightOrAngle2 = mInnerDistance[hv][cat];
					}
					else {
						vp.screenX = barColorEdge[hv][cat][0] + mInnerDistance[hv][cat] * (0.5f + vp.chartGroupIndex);
						vp.widthOrAngle1 = mInnerDistance[hv][cat];
						vp.screenY = barPosition[hv][cat];
						vp.heightOrAngle2 = width;
					}
				}

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
}
