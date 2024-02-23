package com.actelion.research.table.view.chart;

import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.JVisualization2D;
import com.actelion.research.table.view.VisualizationPoint;
import com.actelion.research.table.view.VisualizationSplitter;

import java.awt.*;

public class PieChart extends AbstractBarOrPieChart {
	private static final float cMaxPieSize = 1.0f;

	private float[][] mPieX;
	private float[][] mPieY;
	private float[][] mPieSize;         // pie size in pixel
	private float[][][] mPieColorEdge;


	public PieChart(JVisualization visualization, int hvCount, int mode) {
		super(visualization, hvCount, mode);
	}

	public float getPieX(int hv, int cat) {
		return mPieX[hv][cat];
	}

	public float getPieY(int hv, int cat) {
		return mPieY[hv][cat];
	}

	public float getPieSize(int hv, int cat) {
		return mPieSize[hv][cat];
	}

	@Override
	public void calculateCoordinates(Rectangle baseGraphRect) {
		if (!isBarOrPieDataAvailable())
			return;

		int categoryVisCount0 = mVisualization.getCategoryVisCount(0);;
		int categoryVisCount1 = mVisualization.getCategoryVisCount(1);
		int caseSeparationCategoryCount = mVisualization.getCaseSeparationCategoryCount();

		float cellHeight = baseGraphRect.height / (float)categoryVisCount1;
		float labelHeight = Math.min(cellHeight / 2f, calculateStatisticsLabelSize(false, 0f));  // we need the height of the label set
		float cellWidth = (float)baseGraphRect.width / (float)categoryVisCount0;
		float cellSize = Math.min(cellWidth, cellHeight);

		mPieSize = new float[mHVCount][mCatCount];
		mPieX = new float[mHVCount][mCatCount];
		mPieY = new float[mHVCount][mCatCount];
		mPieColorEdge = new float[mHVCount][mCatCount][mColor.length+1];
		int preferredCSAxis = (cellWidth > cellHeight) ? 0 : 1;
		float csWidth = (preferredCSAxis == 0 ? cellWidth : -cellHeight)
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

			for (int i = 0; i<categoryVisCount0; i++) {
				for (int j = 0; j<categoryVisCount1; j++) {
					for (int k=0; k<caseSeparationCategoryCount; k++) {
						int cat = (i+j* categoryVisCount0)*caseSeparationCategoryCount+k;
						if (mPointsInCategory[hv][cat] > 0) {
							float relSize = Math.abs(mBarValue[hv][cat] - mBarBase)
									/ (mAxisMax - mAxisMin);
							mPieSize[hv][cat] = cMaxPieSize * cellSize * mVisualization.getMarkerSize()
									* (float)Math.sqrt(relSize);
							mPieX[hv][cat] = baseGraphRect.x + hOffset + i*cellWidth + cellWidth/2;
							mPieY[hv][cat] = baseGraphRect.y + vOffset + baseGraphRect.height
									- labelHeight / 2f - j*cellHeight - cellHeight/2;

							if (caseSeparationCategoryCount != 1) {
								if (preferredCSAxis == 0)
									mPieX[hv][cat] += csOffset + k*csWidth;
								else
									mPieY[hv][cat] += csOffset + k*csWidth;
							}

							if (useProportionalFractions())
								for (int l=0; l<mColor.length; l++)
									mPieColorEdge[hv][cat][l+1] = mPieColorEdge[hv][cat][l] + 360.0f
											* mAbsColorValueSum[hv][cat][l]
											/ mAbsValueSum[hv][cat];
							else
								for (int l=0; l<mColor.length; l++)
									mPieColorEdge[hv][cat][l+1] = mPieColorEdge[hv][cat][l] + 360.0f
											* (float)mPointsInColorCategory[hv][cat][l]
											/ (float)mPointsInCategory[hv][cat];
						}
					}
				}
			}
		}

		VisualizationPoint[] point = mVisualization.getDataPoints();
		int chartColumn = mVisualization.getChartType().getColumn();

		// calculate coordinates for selection
		for (VisualizationPoint vp:point) {
			if (mVisualization.isVisibleInBarsOrPies(vp)) {
				int hv = vp.hvIndex;
				int cat = mVisualization.getChartCategoryIndex(vp);
				float angle = 0f;
				if (useProportionalFractions()) {
					int colorIndex = mVisualization.getColorIndex(vp, mBaseColorCount, mFocusFlagNo);
					float fractionAngle = 360f * Math.abs(vp.record.getDouble(chartColumn))
							/ mAbsValueSum[hv][cat];
					vp.widthOrAngle1 = mPieColorEdge[hv][cat][colorIndex];
					angle = (mPieColorEdge[hv][cat][colorIndex] + 0.5f * fractionAngle) * (float)Math.PI / 180f;
					mPieColorEdge[hv][cat][colorIndex] += fractionAngle;
					vp.heightOrAngle2 = mPieColorEdge[hv][cat][colorIndex];
				}
				else {
					angle = (0.5f + vp.chartGroupIndex) * 2.0f * (float)Math.PI / mPointsInCategory[hv][cat];
				}
				vp.screenX = Math.round(mPieX[hv][cat]+ mPieSize[hv][cat]/2.0f*(float)Math.cos(angle));
				vp.screenY = Math.round(mPieY[hv][cat]- mPieSize[hv][cat]/2.0f*(float)Math.sin(angle));
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

		for (int hv = 0; hv<mHVCount; hv++) {
			for (int cat=0; cat<mCatCount; cat++) {
				if (mPointsInCategory[hv][cat] > 0) {
					int r = Math.round(mPieSize[hv][cat]/2);
					int x = Math.round(mPieX[hv][cat]);
					int y = Math.round(mPieY[hv][cat]);
					if (mPointsInCategory[hv][cat] == 1) {
						for (int k=0; k<mColor.length; k++) {
							if (mPointsInColorCategory[hv][cat][k] > 0) {
								g.setColor(mColor[k]);
								break;
							}
						}
						g.fillOval(x-r, y-r, 2*r, 2*r);
					}
					else {
						for (int k=0; k<mColor.length; k++) {
							if (mPointsInColorCategory[hv][cat][k] > 0) {
								g.setColor(mColor[k]);
								g.fillArc(x-r, y-r, 2*r, 2*r,
										Math.round(mPieColorEdge[hv][cat][k]),
										Math.round(mPieColorEdge[hv][cat][k+1])-Math.round(mPieColorEdge[hv][cat][k]));
							}
						}
					}
					if (drawOutline) {
						g.setColor(outlineGray);
						g.drawOval(x - r, y - r, 2 * r, 2 * r);
					}
				}
			}
		}

		if (getLabelCount() != 0) {
			String[] lineText = new String[getLabelCount()];
			int scaledFontHeight = ((JVisualization2D)mVisualization).setRelativeFontHeightAndScaleToSplitView(STATISTIC_LABEL_FONT_FACTOR);
			g.setColor(mVisualization.getContrastGrey(JVisualization.SCALE_STRONG));
			for (int hv = 0; hv<mHVCount; hv++) {
				for (int cat=0; cat<mCatCount; cat++) {
					if (mPointsInCategory[hv][cat] > 0) {
						int lineCount = compileStatisticsLines(hv, cat, lineText);

						float r = mPieSize[hv][cat]/2;
						float x0 = mPieX[hv][cat];
						float y0 = mPieY[hv][cat] + r + g.getFontMetrics().getAscent() + scaledFontHeight / 4f;

						for (int line=0; line<lineCount; line++) {
							float x = Math.round(x0 - g.getFontMetrics().stringWidth(lineText[line])/2f);
							float y = y0 + line * scaledFontHeight;
							g.drawString(lineText[line], Math.round(x), Math.round(y));
						}
					}
				}
			}
		}

		if (original != null)
			g.setComposite(original);
	}

	/**
	 * If we need space for statistics labels, then reduce the area that we have for the pie.
	 * If we show a scale reflecting the pie values, then we need to transform scale labels
	 * accordingly.
	 * @param baseRect
	 */
	@Override
	public void adaptDoubleScalesForStatisticalLabels(final Rectangle baseRect) {
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
