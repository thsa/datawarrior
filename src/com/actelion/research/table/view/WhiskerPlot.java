package com.actelion.research.table.view;

import java.awt.*;

public class WhiskerPlot extends AbstractDistributionPlot {
	public WhiskerPlot(JVisualization visualization, int hvCount, int doubleAxis) {
		super(visualization, hvCount, doubleAxis);
	}
	@Override
	public boolean paintMarker(VisualizationPoint vp) {
		return true;    // we paint all visible markers
	}

	@Override
	public void calculate() {
		super.calculate(JVisualization.cChartTypeWhiskerPlot);
	}

	@Override
	public void paint(Graphics2D g, Rectangle baseBounds, Rectangle baseGraphRect) {
		float[][] position = new float[mHVCount][mCatCount];
		float[][] mean = new float[mHVCount][mCatCount];
		float[][] median = new float[mHVCount][mCatCount];
		float[][] lav = new float[mHVCount][mCatCount];
		float[][] uav = new float[mHVCount][mCatCount];
		float[][] q1 = new float[mHVCount][mCatCount];
		float[][] q3 = new float[mHVCount][mCatCount];

		calculateCoordinates(baseGraphRect, position, mean, median, lav, uav, q1, q3, null);
		drawConnectionLines(g, position, mean, median);

		float lineLengthAV = mBarWidth / 3;
		float lineWidth = Math.min(((JVisualization2D)mVisualization).getFontHeightScaledToSplitView()/8f, mBarWidth /8f);

		Stroke lineStroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		Stroke dashedStroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
				lineWidth, new float[] {3*lineWidth}, 0f);

		Color strongGray = mVisualization.getContrastGrey(JVisualization.SCALE_STRONG);
		VisualizationColor markerColor = mVisualization.getMarkerColor();
		int caseSeparationCategoryCount = mVisualization.getCaseSeparationCategoryCount();
		int caseSeparationColumn = mVisualization.getCaseSeparationColumn();

		for (int hv = 0; hv<mHVCount; hv++) {
			for (int cat=0; cat<mCatCount; cat++) {
				if (mPointsInCategory[hv][cat] > 0) {
					// If we show no markers in a whisker plot, and if every whisker belongs to one category
					// of that column that is assigned for marker coloring, then we draw the whisker itself
					// in the color assigned to that category.
					if (mVisualization.getMarkerSize() == 0.0) {
						if (markerColor.getColorColumn() == mVisualization.getColumnIndex(mDoubleAxis == 1 ? 0 : 1))
							g.setColor(markerColor.getColor(cat / caseSeparationCategoryCount));
						else if (caseSeparationCategoryCount != 1
								&& markerColor.getColorColumn() == caseSeparationColumn)
							g.setColor(markerColor.getColor(cat % caseSeparationCategoryCount));
						else
							g.setColor(strongGray);
					}
					else {
						g.setColor(strongGray);
					}

					if (mDoubleAxis == 1) {
						g.setStroke(lineStroke);
						if (lav[hv][cat] > q1[hv][cat])
							g.drawLine(Math.round(position[hv][cat]-lineLengthAV),
									Math.round(lav[hv][cat]),
									Math.round(position[hv][cat]+lineLengthAV),
									Math.round(lav[hv][cat]));
						if (uav[hv][cat] < q3[hv][cat])
							g.drawLine(Math.round(position[hv][cat]-lineLengthAV),
									Math.round(uav[hv][cat]),
									Math.round(position[hv][cat]+lineLengthAV),
									Math.round(uav[hv][cat]));

						g.setStroke(dashedStroke);
						g.drawLine(Math.round(position[hv][cat]),
								Math.round(uav[hv][cat]),
								Math.round(position[hv][cat]),
								Math.round(lav[hv][cat]));
					}
					else {
						g.setStroke(lineStroke);
						g.drawLine(Math.round(lav[hv][cat]),
								Math.round(position[hv][cat]-lineLengthAV),
								Math.round(lav[hv][cat]),
								Math.round(position[hv][cat]+lineLengthAV));
						g.drawLine(Math.round(uav[hv][cat]),
								Math.round(position[hv][cat]-lineLengthAV),
								Math.round(uav[hv][cat]),
								Math.round(position[hv][cat]+lineLengthAV));

						g.setStroke(dashedStroke);
						g.drawLine(Math.round(lav[hv][cat]),
								Math.round(position[hv][cat]),
								Math.round(uav[hv][cat]),
								Math.round(position[hv][cat]));
					}

					g.setStroke(lineStroke);
					drawMeanIndicators(g, median[hv][cat], mean[hv][cat], position[hv][cat], 2*lineWidth);
				}
			}
		}

		paintStatisticsInfo(g, baseGraphRect, position, lav, uav, 0f);
	}
}
