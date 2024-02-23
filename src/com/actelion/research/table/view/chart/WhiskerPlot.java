package com.actelion.research.table.view.chart;

import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.JVisualization2D;
import com.actelion.research.table.view.VisualizationColor;
import com.actelion.research.table.view.VisualizationPoint;

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
		calculateStatistics();
	}

	@Override
	public void calculateCoordinates(Rectangle baseGraphRect) {
		// initialize arrays to be filled
		mSCenter = new float[mHVCount][mCatCount];
		mSMean = new float[mHVCount][mCatCount];
		mSMedian = new float[mHVCount][mCatCount];
		mSLAV = new float[mHVCount][mCatCount];
		mSUAV = new float[mHVCount][mCatCount];
		mSQ1 = new float[mHVCount][mCatCount];
		mSQ3 = new float[mHVCount][mCatCount];
		calculateScreenCoordinates(baseGraphRect);
	}

	@Override
	public void paint(Graphics2D g, Rectangle baseBounds, Rectangle baseGraphRect) {
		drawConnectionLines(g);

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
						if (mSLAV[hv][cat] > mSQ1[hv][cat])
							g.drawLine(Math.round(mSCenter[hv][cat]-lineLengthAV),
									Math.round(mSLAV[hv][cat]),
									Math.round(mSCenter[hv][cat]+lineLengthAV),
									Math.round(mSLAV[hv][cat]));
						if (mSUAV[hv][cat] < mSQ3[hv][cat])
							g.drawLine(Math.round(mSCenter[hv][cat]-lineLengthAV),
									Math.round(mSUAV[hv][cat]),
									Math.round(mSCenter[hv][cat]+lineLengthAV),
									Math.round(mSUAV[hv][cat]));

						g.setStroke(dashedStroke);
						g.drawLine(Math.round(mSCenter[hv][cat]),
								Math.round(mSUAV[hv][cat]),
								Math.round(mSCenter[hv][cat]),
								Math.round(mSLAV[hv][cat]));
					}
					else {
						g.setStroke(lineStroke);
						g.drawLine(Math.round(mSLAV[hv][cat]),
								Math.round(mSCenter[hv][cat]-lineLengthAV),
								Math.round(mSLAV[hv][cat]),
								Math.round(mSCenter[hv][cat]+lineLengthAV));
						g.drawLine(Math.round(mSUAV[hv][cat]),
								Math.round(mSCenter[hv][cat]-lineLengthAV),
								Math.round(mSUAV[hv][cat]),
								Math.round(mSCenter[hv][cat]+lineLengthAV));

						g.setStroke(dashedStroke);
						g.drawLine(Math.round(mSLAV[hv][cat]),
								Math.round(mSCenter[hv][cat]),
								Math.round(mSUAV[hv][cat]),
								Math.round(mSCenter[hv][cat]));
					}

					g.setStroke(lineStroke);
					drawMeanIndicators(g, mSMedian[hv][cat], mSMean[hv][cat], mSCenter[hv][cat], 2*lineWidth);
				}
			}
		}

		paintStatisticsInfo(g, baseGraphRect, 0f);
	}
}
