package com.actelion.research.table.view.chart;

import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.JVisualization2D;
import com.actelion.research.table.view.VisualizationPoint;

import java.awt.*;
import java.awt.geom.Line2D;

public class BoxPlot extends AbstractDistributionPlot {
	public BoxPlot(JVisualization visualization, int hvCount, int doubleAxis) {
		super(visualization, hvCount, doubleAxis);
		}

	@Override
	public boolean paintMarker(VisualizationPoint vp) {
		return vp.chartGroupIndex == -1;    // is outlier
		}

	@Override
	public void calculateCoordinates(Rectangle baseGraphRect) {
		// initialize arrays to be filled
		mSCenter = new float[mHVCount][mCatCount];
		mSMean = new float[mHVCount][mCatCount];
		mSMedian = new float[mHVCount][mCatCount];
		mSLAV = new float[mHVCount][mCatCount];
		mSUAV = new float[mHVCount][mCatCount];
		mSBarColorEdge = new float[mHVCount][mCatCount][mColor.length+1];
		calculateScreenCoordinates(baseGraphRect);
	}

	@Override
	public void calculate() {
		calculateStatistics();

		VisualizationPoint[] point = mVisualization.getDataPoints();

		mOutlierCount = new int[mHVCount][mCatCount];

		mPointsInCategory = new int[mHVCount][mCatCount];
		mPointsInColorCategory = new int[mHVCount][mCatCount][mColor.length];
		for (int i=0; i<point.length; i++) {
			VisualizationPoint vp = point[i];
			if (mVisualization.isVisibleExcludeNaN(vp)) {
				int cat = mVisualization.getChartCategoryIndex(vp);
				int hv = vp.hvIndex;
				if (isOutsideValue(hv, cat, mVisualization.getAxisValue(vp.record, mDoubleAxis))) {
					mOutlierCount[hv][cat]++;
				}
				else {
					int colorIndex = mVisualization.getColorIndex(vp, mBaseColorCount, mFocusFlagNo);
					mPointsInCategory[hv][cat]++;
					mPointsInColorCategory[hv][cat][colorIndex]++;
				}
			}
		}

		int[][][] count = new int[mHVCount][mCatCount][mColor.length];
		for (int hv = 0; hv<mHVCount; hv++)
			for (int cat = 0; cat<mCatCount; cat++)
				for (int color = 1; color<mColor.length; color++)
					count[hv][cat][color] = count[hv][cat][color-1] + mPointsInColorCategory[hv][cat][color-1];

		for (int i=0; i<point.length; i++) {
			VisualizationPoint vp = point[i];
			if (mVisualization.isVisible(vp)) {
				float v = mVisualization.getAxisValue(vp.record, mDoubleAxis);
				if (Float.isNaN(v)) {
					vp.chartGroupIndex = -1;
				}
				else {
					int hv = vp.hvIndex;
					int cat = mVisualization.getChartCategoryIndex(vp);
					if (isOutsideValue(hv, cat, v)) {
						vp.chartGroupIndex = -1;
					}
					else {
						int colorIndex = mVisualization.getColorIndex(vp, mBaseColorCount, mFocusFlagNo);
						vp.chartGroupIndex = count[hv][cat][colorIndex];
						count[hv][cat][colorIndex]++;
					}
				}
			}
		}
	}

	@Override
	public void paint(Graphics2D g, Rectangle baseBounds, Rectangle baseGraphRect) {
		drawConnectionLines(g);

		Composite original = null;
		Composite composite = null;
		float markerTransparency = ((JVisualization2D)mVisualization).getMarkerTransparency();
		if (markerTransparency != 0.0) {
			original = g.getComposite();
			composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(1.0-markerTransparency));
		}

		float lineLengthAV = mBarWidth / 3;

		float lineWidth = Math.min(((JVisualization2D)mVisualization).getFontHeightScaledToSplitView()/8f, mBarWidth /8f);

		Stroke lineStroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		Stroke dashedStroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
				lineWidth, new float[] {3*lineWidth}, 0f);

		Color strongGray = mVisualization.getContrastGrey(JVisualization.SCALE_STRONG);

		for (int hv = 0; hv<mHVCount; hv++) {
			for (int cat=0; cat<mCatCount; cat++) {
				if (mPointsInCategory[hv][cat] > 0) {
					if (composite != null)
						g.setComposite(composite);

					for (int k=0; k<mColor.length; k++) {
						if (mPointsInColorCategory[hv][cat][k] > 0) {
							g.setColor(mColor[k]);
							if (mDoubleAxis == 1)
								g.fillRect(Math.round(mSCenter[hv][cat]- mBarWidth /2),
										Math.round(mSBarColorEdge[hv][cat][k+1]),
										Math.round(mBarWidth),
										Math.round(mSBarColorEdge[hv][cat][k])-Math.round(mSBarColorEdge[hv][cat][k+1]));
							else
								g.fillRect(Math.round(mSBarColorEdge[hv][cat][k]),
										Math.round(mSCenter[hv][cat]- mBarWidth /2),
										Math.round(mSBarColorEdge[hv][cat][k+1]-Math.round(mSBarColorEdge[hv][cat][k])),
										Math.round(mBarWidth));
						}
					}
					if (original != null)
						g.setComposite(original);

					g.setColor(strongGray);
					g.setStroke(lineStroke);

					if (((JVisualization2D)mVisualization).isDrawBarPieBoxOutline()) {
						if (mDoubleAxis == 1)
							g.drawRect(Math.round(mSCenter[hv][cat]- mBarWidth /2),
									Math.round(mSBarColorEdge[hv][cat][mColor.length]),
									Math.round(mBarWidth),
									Math.round(mSBarColorEdge[hv][cat][0])-Math.round(mSBarColorEdge[hv][cat][mColor.length]));
						else
							g.drawRect(Math.round(mSBarColorEdge[hv][cat][0]),
									Math.round(mSCenter[hv][cat]- mBarWidth /2),
									Math.round(mSBarColorEdge[hv][cat][mColor.length])-Math.round(mSBarColorEdge[hv][cat][0]),
									Math.round(mBarWidth));
					}

					if (mSLAV[hv][cat] < mSBarColorEdge[hv][cat][0] ^ mDoubleAxis == 1)
						drawLine(g, Math.round(mSCenter[hv][cat]-lineLengthAV),
								Math.round(mSLAV[hv][cat]),
								Math.round(mSCenter[hv][cat]+lineLengthAV),
								Math.round(mSLAV[hv][cat]));
					if (mSUAV[hv][cat] > mSBarColorEdge[hv][cat][mColor.length] ^ mDoubleAxis == 1)
						drawLine(g, Math.round(mSCenter[hv][cat]-lineLengthAV),
								Math.round(mSUAV[hv][cat]),
								Math.round(mSCenter[hv][cat]+lineLengthAV),
								Math.round(mSUAV[hv][cat]));

					g.setStroke(dashedStroke);
					if (mSLAV[hv][cat] < mSBarColorEdge[hv][cat][0] ^ mDoubleAxis == 1)
						drawLine(g, Math.round(mSCenter[hv][cat]),
								Math.round(mSLAV[hv][cat]),
								Math.round(mSCenter[hv][cat]),
								Math.round(mSBarColorEdge[hv][cat][0]));
					if (mSUAV[hv][cat] > mSBarColorEdge[hv][cat][mColor.length] ^ mDoubleAxis == 1)
						drawLine(g, Math.round(mSCenter[hv][cat]),
								Math.round(mSUAV[hv][cat]),
								Math.round(mSCenter[hv][cat]),
								Math.round(mSBarColorEdge[hv][cat][mColor.length]));

					g.setStroke(lineStroke);
					drawMeanIndicators(g, mSMedian[hv][cat], mSMean[hv][cat], mSCenter[hv][cat], 2*lineWidth);
				}
			}
		}

		paintStatisticsInfo(g, baseGraphRect, 0f);

		// in case of box-plot calculate screen positions of all non-outliers
		for (VisualizationPoint vp:mVisualization.getDataPoints()) {
			if (mVisualization.isVisibleExcludeNaN(vp)) {
				if (vp.chartGroupIndex != -1) {
					int hv = vp.hvIndex;
					int cat = mVisualization.getChartCategoryIndex(vp);
					if (mDoubleAxis == 1) {
						vp.screenX = mSCenter[hv][cat];
						vp.screenY = mSBarColorEdge[hv][cat][0]- mInnerDistance[hv][cat]*(1+vp.chartGroupIndex)
								+ mInnerDistance[hv][cat]/2;
						vp.widthOrAngle1 = mBarWidth;
						vp.heightOrAngle2 = mInnerDistance[hv][cat];
					}
					else {
						vp.screenX = mSBarColorEdge[hv][cat][0]+ mInnerDistance[hv][cat]*vp.chartGroupIndex
								+ mInnerDistance[hv][cat]/2;
						vp.screenY = mSCenter[hv][cat]- mBarWidth /2+ mBarWidth /2;
						vp.widthOrAngle1 = mInnerDistance[hv][cat];
						vp.heightOrAngle2 = mBarWidth;
					}
				}
			}
		}
	}

	private void drawLine(Graphics2D g, float a1, float b1, float a2, float b2) {
		if (mDoubleAxis == 1)
			g.draw(new Line2D.Float(a1, b1, a2, b2));
		else
			g.draw(new Line2D.Float(b1, a1, b2, a2));
	}
}
