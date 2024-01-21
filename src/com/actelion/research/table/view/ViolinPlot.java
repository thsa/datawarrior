package com.actelion.research.table.view;

import com.actelion.research.util.ColorHelper;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

public class ViolinPlot extends AbstractDistributionPlot {
	private static final int FRACTIONS = 120;
	private static final int SCREEN_WIDTH_FRACTIONS = 7;
	private static final float VIOLIN_WIDTH_FACTOR = 0.95f;  // We devide space such that violins never overlap with the default marker size == 1
	public static final float DEFAULT_MARGIN = 2.5f * SCREEN_WIDTH_FRACTIONS / FRACTIONS;   // 2.0 is exact fit
	private float[][][][] mViolinWidth;
	private int[][][][] mPointsInColorFraction;
	private float mMaxViolinWidth,mTranslatedMaxWidth;

	private int[] mSplittingOffset;

	public ViolinPlot(JVisualization visualization, int hvCount, int doubleAxis) {
		super(visualization, hvCount, doubleAxis);
	}

	@Override
	public boolean paintMarker(VisualizationPoint vp) {
		return vp.chartGroupIndex == -1;    // visible NaN
	}

	@Override
	public void calculate() {
		super.calculate(JVisualization.cChartTypeViolins);

		int fractions = Math.round(FRACTIONS * mVisualization.getRelativeVisibleRange(mDoubleAxis));
		if (fractions == 0)
			return;

		float axisVisMin = mVisualization.getVisibleMin(mDoubleAxis);
		float axisVisMax = mVisualization.getVisibleMax(mDoubleAxis);

		// Assign visualization point contributions to violin segments(i.e. fractions).
		// Depending on exact position split VP's contribution (1.0) and assign to two adjacent fractions.
		float[][][][] count = new float[mHVCount][mCatCount][mColor.length][fractions+1];
		mPointsInColorFraction = new int[mHVCount][mCatCount][mColor.length][fractions];
		for (VisualizationPoint vp:mVisualization.getDataPoints()) {
			if (mVisualization.isVisible(vp)) {
				if (mVisualization.isNaN(vp)) {
					vp.chartGroupIndex = -1;     // visible but NaN
				}
				else {
					vp.chartGroupIndex = 1;     // visible and not NaN
					float v = mVisualization.getAxisValue(vp.record, mDoubleAxis);
					if (!Float.isNaN(v)) {
						v = (v-axisVisMin) / (axisVisMax-axisVisMin);
						int hv = vp.hvIndex;
						int cat = mVisualization.getChartCategoryIndex(vp);
						int colorIndex = mVisualization.getColorIndex(vp, mBaseColorCount, mFocusFlagNo);

						// We split the bar area into <fractions> equally sized fractions.
						// Depending on the exact location of the axis value we split the
						// count value of 1.0 into two parts and add them two adjacent
						// mViolinWidth cells, where we currently just add count contributions.
						v *= fractions;
						int index = Math.round(v);
						float dif = v - index;
						if (v > index) {
							count[hv][cat][colorIndex][index] += 1f - dif;
							if (index < fractions)
								count[hv][cat][colorIndex][index+1] += dif;
						}
						else {
							count[hv][cat][colorIndex][index] += 1f + dif;
							if (index > 0)
								count[hv][cat][colorIndex][index-1] -= dif;
						}

						mPointsInColorFraction[hv][cat][colorIndex][Math.min(fractions-1, (int)v)]++;
					}
				}
			}
			else {
				vp.chartGroupIndex = 0; // not visible
			}
		}

		// Calculate neighbour incluence factors using a Gaussean function
		int influences = FRACTIONS / 10;
		float[] influence = new float[influences];
		float factor = 4f/((influences-1)*(influences-1));  // lowest used influence factor is e^-4 (0.0183)
		for (int i=0; i<influences; i++)
			influence[i] = (float)Math.exp(-factor*i*i);

		// Smoothen the count values by applying the Gaussean influence zone
		mViolinWidth = new float[mHVCount][mCatCount][mColor.length][fractions+1];
		for (int hv = 0; hv<mHVCount; hv++) {
			for (int cat = 0; cat<mCatCount; cat++) {
				for (int colorIndex = 0; colorIndex<mColor.length; colorIndex++) {
					for (int fraction=0; fraction<=fractions; fraction++) {
						mViolinWidth[hv][cat][colorIndex][fraction] += count[hv][cat][colorIndex][fraction];
						for (int j=1; j<influence.length; j++) {
							if (fraction >= j)
								mViolinWidth[hv][cat][colorIndex][fraction-j] += influence[j] * count[hv][cat][colorIndex][fraction];
							if (fraction + j <= fractions)
								mViolinWidth[hv][cat][colorIndex][fraction+j] += influence[j] * count[hv][cat][colorIndex][fraction];
						}
					}
				}
			}
		}

		// accumulate values over colors
		for (int hv = 0; hv<mHVCount; hv++)
			for (int cat = 0; cat<mCatCount; cat++)
				for (int colorIndex = 1; colorIndex<mColor.length; colorIndex++)
					for (int fraction=0; fraction<=fractions; fraction++)
						mViolinWidth[hv][cat][colorIndex][fraction] += mViolinWidth[hv][cat][colorIndex-1][fraction];

		// determine largest accumulated value
		mMaxViolinWidth = 0f;
		for (int hv = 0; hv<mHVCount; hv++)
			for (int cat = 0; cat<mCatCount; cat++)
				for (int fraction=0; fraction<=fractions; fraction++)
					if (mMaxViolinWidth < mViolinWidth[hv][cat][mColor.length-1][fraction])
						mMaxViolinWidth = mViolinWidth[hv][cat][mColor.length-1][fraction];
	}

	@Override
	public void paint(Graphics2D g, Rectangle baseBounds, Rectangle baseGraphRect) {
		if (mViolinWidth == null || mViolinWidth.length == 0 || mViolinWidth[0].length == 0)
			return;

		float[][] position = new float[mHVCount][mCatCount];
		float[][] mean = new float[mHVCount][mCatCount];
		float[][] median = new float[mHVCount][mCatCount];
		float[][] lav = new float[mHVCount][mCatCount];
		float[][] uav = new float[mHVCount][mCatCount];
		float[][] q1 = new float[mHVCount][mCatCount];
		float[][] q3 = new float[mHVCount][mCatCount];

		calculateCoordinates(baseGraphRect, position, mean, median, lav, uav, q1, q3, null);

		Composite original = null;
		float markerTransparency = ((JVisualization2D)mVisualization).getMarkerTransparency();
		if (markerTransparency != 0.0) {
			original = g.getComposite();
			Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(1.0-markerTransparency));
			g.setComposite(composite);
		}

		drawConnectionLines(g, position, mean, median);

		float lineWidth = Math.min(((JVisualization2D)mVisualization).getFontHeightScaledToSplitView()/6f, mBarWidth/8f);
		Stroke thinLineStroke = new BasicStroke(0.5f*lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		Stroke lineStroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		Stroke fatStroke = new BasicStroke(3*lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

		Color strongGray = mVisualization.getContrastGrey(JVisualization.SCALE_STRONG);
		Color mediumGray = mVisualization.getContrastGrey(JVisualization.SCALE_MEDIUM);
		int fractions = mViolinWidth[0][0][0].length-1;
		mTranslatedMaxWidth = VIOLIN_WIDTH_FACTOR * mBarWidth / mMaxViolinWidth * mVisualization.getMarkerSize();

		Path2D.Float path = new Path2D.Float(Path2D.WIND_NON_ZERO, 2*mViolinWidth[0][0].length);

		float[] fc = new float[fractions+1];
		if (mDoubleAxis == 1)
			for (int f=0; f<=fractions; f++)
				fc[f] = baseGraphRect.y + (fractions-f) * (float)baseGraphRect.height / fractions;
		else
			for (int f=0; f<=fractions; f++)
				fc[f] = baseGraphRect.x + f * (float)baseGraphRect.width / fractions;

		// Calculate splitting offset in double axis direction (the other splitting axis offset is already in position[])
		mSplittingOffset = new int[mHVCount];
		for (int hv = 0; hv<mHVCount; hv++) {
			if (mVisualization.isSplitView()) {
				VisualizationSplitter splitter = mVisualization.getSplitter();
				if (mDoubleAxis == 0)
					mSplittingOffset[hv] = splitter.getHIndex(hv) * splitter.getGridWidth();
				else
					mSplittingOffset[hv] = splitter.getVIndex(hv) * splitter.getGridHeight();
			}
		}

		for (int hv = 0; hv<mHVCount; hv++) {
			for (int cat = 0; cat<mCatCount; cat++) {
				for (int colorIndex = 0; colorIndex<mColor.length; colorIndex++) {
					int startFraction = 0;
					while (startFraction >= 0 && startFraction < fractions) {
						startFraction = buildViolinPath(position, startFraction, hv, cat, colorIndex-1, colorIndex, fc, path);
						if (startFraction != -1) {
							g.setColor(mColor[colorIndex]);
							g.fill(path);
						}
					}
				}

				if (((JVisualization2D)mVisualization).isDrawBarPieBoxOutline()) {
					int startFraction = 0;
					while (startFraction >= 0 && startFraction < fractions) {
						startFraction = buildViolinPath(position, startFraction, hv, cat, -1, mColor.length-1, fc, path);

						if (startFraction != -1) {
							g.setColor(strongGray);
							g.setStroke(lineStroke);
							g.draw(path);
						}
					}
				}

				g.setColor(mediumGray);
				if (mDoubleAxis == 1) {
					g.setStroke(fatStroke);
					g.draw(new Line2D.Float(position[hv][cat], q1[hv][cat], position[hv][cat], q3[hv][cat]));
					g.setStroke(lineStroke);
					g.draw(new Line2D.Float(position[hv][cat], uav[hv][cat], position[hv][cat], lav[hv][cat]));
				}
				else {
					g.setStroke(fatStroke);
					g.draw(new Line2D.Float(q1[hv][cat], position[hv][cat], q3[hv][cat], position[hv][cat]));
					g.setStroke(lineStroke);
					g.draw(new Line2D.Float(uav[hv][cat], position[hv][cat], lav[hv][cat], position[hv][cat]));
				}

				g.setStroke(thinLineStroke);
				drawMeanIndicators(g, median[hv][cat], mean[hv][cat], position[hv][cat], lineWidth);

				float offset = 1.5f * (mDoubleAxis == 1 ? baseGraphRect.height : baseGraphRect.width) * SCREEN_WIDTH_FRACTIONS / (float)fractions;
				paintStatisticsInfo(g, baseGraphRect, position, lav, uav, offset);
			}
		}

		if (original != null)
			g.setComposite(original);

		// Assign screen positions, width, and height to individual violin contributing visualization points
		float axisVisMin = mVisualization.getVisibleMin(mDoubleAxis);
		float axisVisMax = mVisualization.getVisibleMax(mDoubleAxis);
		float axisRange = axisVisMax - axisVisMin;
		int[][][][] count = new int[mHVCount][mCatCount][mColor.length][fractions+1];
		for (VisualizationPoint vp:mVisualization.getDataPoints()) {
			if (vp.chartGroupIndex == 1) {  // visible and not NaN
				float v = mVisualization.getAxisValue(vp.record, mDoubleAxis);
				if (!Float.isNaN(v)) {
					int hv = vp.hvIndex;
					int cat = mVisualization.getChartCategoryIndex(vp);
					int colorIndex = mVisualization.getColorIndex(vp, mBaseColorCount, mFocusFlagNo);
					int fraction = Math.min(fractions-1, (int)(fractions * (v-axisVisMin) / (axisVisMax-axisVisMin)));
					float dist = (mViolinWidth[hv][cat][colorIndex][fraction] - (colorIndex == 0 ? 0f : mViolinWidth[hv][cat][colorIndex-1][fraction]))
								/ Math.max(1, mPointsInColorFraction[hv][cat][colorIndex][fraction]);   // should never be 0, though!
					float markerSize = 2f * mTranslatedMaxWidth * dist;
					float screenPos = translate(hv, cat, colorIndex-1, fraction, position)
									+ (0.5f + count[hv][cat][colorIndex][fraction]) * markerSize;

					if (mDoubleAxis == 1) {
						vp.screenX = screenPos;
						vp.widthOrAngle1 = markerSize;
						vp.screenY = mSplittingOffset[hv] + baseGraphRect.y + baseGraphRect.height + (axisVisMin-v)*baseGraphRect.height / axisRange;
						vp.heightOrAngle2 = SCREEN_WIDTH_FRACTIONS * baseGraphRect.height / (float)fractions;
					}
					else {
						vp.screenX = mSplittingOffset[hv] + baseGraphRect.x + (v-axisVisMin)*baseGraphRect.width / axisRange;
						vp.widthOrAngle1 = SCREEN_WIDTH_FRACTIONS * baseGraphRect.width / (float)fractions;
						vp.screenY = screenPos;
						vp.heightOrAngle2 = markerSize;
					}
					count[hv][cat][colorIndex][fraction]++;
				}
			}
		}

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

			for (VisualizationPoint vp:mVisualization.getDataPoints()) {
				if (vp.chartGroupIndex != 0 && labelHelper.hasLabels(vp)) {
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

	@Override
	protected void drawMeanIndicators(Graphics2D g, float median, float mean, float bar, float lineWidth) {
		switch (mVisualization.getBoxplotMeanMode()) {
			case cBoxplotMeanModeMedian:
				drawIndicatorDot(g, median, bar, lineWidth * 3.5f, mVisualization.getContrastGrey(JVisualization.SCALE_STRONG));
				break;
			case cBoxplotMeanModeMean:
				drawIndicatorDot(g, mean, bar, lineWidth * 3.5f, Color.RED.darker());
				break;
			case cBoxplotMeanModeLines:
				drawIndicatorDot(g, mean, bar, lineWidth * 3.5f, Color.RED.darker());
				drawIndicatorDot(g, median, bar, lineWidth * 3.5f, mVisualization.getContrastGrey(JVisualization.SCALE_STRONG));
				break;
			case cBoxplotMeanModeTriangles:
				float width = 2f * lineWidth;
				float space = 2f * lineWidth;
				float tip = space + 1.5f * width;

				if (mDoubleAxis == 1) {
					drawIndicatorTriangle(g, bar+tip, median, bar+space, median-width, bar+space, median+width, Color.BLACK);
					drawIndicatorTriangle(g, bar-tip, mean, bar-space, mean-width, bar-space, mean+width, Color.RED);
				}
				else {
					drawIndicatorTriangle(g, median, bar+tip, median-width, bar+space, median+width, bar+space, Color.BLACK);
					drawIndicatorTriangle(g, mean, bar-tip, mean-width, bar-space, mean+width, bar-space, Color.RED);
				}
				break;
		}
	}

	private void drawIndicatorDot(Graphics2D g, float median, float bar, float size, Color color) {
		Color veryLightGray = mVisualization.getContrastGrey(0f);
		g.setColor(veryLightGray);
		float innerSize = 0.6f * size;
		if (mDoubleAxis == 1) {
			g.fill(new Ellipse2D.Float(bar - 0.5f*size, median - 0.5f*size, size, size));
			g.setColor(color);
			g.fill(new Ellipse2D.Float(bar - 0.5f*innerSize, median - 0.5f*innerSize, innerSize, innerSize));
		}
		else {
			g.fill(new Ellipse2D.Float(median - 0.5f*size, bar - 0.5f*size, size, size));
			g.setColor(color);
			g.fill(new Ellipse2D.Float(median - 0.5f*innerSize, bar - 0.5f*innerSize, innerSize, innerSize));
		}
	}

	/**
	 * Starting from fraction f1, build a path of the next violin fragment between the given color indexes.
	 * If at the given fraction the violin width is 0.0, then increase f1 until populated width is found.
	 * Increases fractions and adds them to the violin path until either width gets 0.0 again or f1 reaches
	 * fc.length.
	 * @param position
	 * @param f1
	 * @param hv
	 * @param cat
	 * @param colorIndex1 -1 ... mColor.length-1; -1 is a virtual index that implies a violin width of 0.0
	 * @param colorIndex2 0 ... mColor.length-1
	 * @param fc
	 * @param path
	 * @return -1 if no path was built
	 */
	private int buildViolinPath(float[][] position, int f1, int hv, int cat, int colorIndex1, int colorIndex2, float[] fc, Path2D.Float path) {
		int fractions = fc.length-1;
		path.reset();

		if (violinBetweenColorsIsEmpty(hv, cat, colorIndex1, colorIndex2, f1))
			while (f1 < fractions && violinBetweenColorsIsEmpty(hv, cat, colorIndex1, colorIndex2, f1+1))
				f1++;

		if (f1 == fractions)
			return -1;

		// Lowest point of violin. May require a second symmetrical point
		// if f1==0 and mViolinWidth[hv][cat][f1] != 0.
		if (mDoubleAxis == 1)
			path.moveTo(translate(hv, cat, colorIndex1, f1, position), mSplittingOffset[hv] + fc[f1]);
		else
			path.moveTo(mSplittingOffset[hv] + fc[f1], translate(hv, cat, colorIndex1, f1, position));

		int f2 = f1+1;
		while (f2 < fractions && !violinBetweenColorsIsEmpty(hv, cat, colorIndex1, colorIndex2, f2)) {
			addToViolinPath(mSplittingOffset[hv] + fc[f2], translate(hv, cat, colorIndex1, f2, position), path);
			f2++;
		}
		addToViolinPath(mSplittingOffset[hv] + fc[f2], translate(hv, cat, colorIndex1, f2, position), path);
		if (!violinBetweenColorsIsEmpty(hv, cat, colorIndex1, colorIndex2, f2))
			addToViolinPath(mSplittingOffset[hv] + fc[f2], translate(hv, cat, colorIndex2, f2, position), path);

		for (int f=f2-1; f>f1; f--)
			addToViolinPath(mSplittingOffset[hv] + fc[f], translate(hv, cat, colorIndex2, f, position), path);

		if (!violinBetweenColorsIsEmpty(hv, cat, colorIndex1, colorIndex2, f1))
			addToViolinPath(mSplittingOffset[hv] + fc[f1], translate(hv, cat, colorIndex2, f1, position), path);

		path.closePath();

		return f2 + 1;
	}

	private boolean violinBetweenColorsIsEmpty(int hv, int cat, int colorIndex1, int colorIndex2, int fraction) {
		if (colorIndex1 == -1)
			return mViolinWidth[hv][cat][colorIndex2][fraction] == 0f;

		return mViolinWidth[hv][cat][colorIndex1][fraction] == mViolinWidth[hv][cat][colorIndex2][fraction];
	}

	private float translate(int hv, int cat, int colorIndex, int fraction, float[][] position) {
		float leftEdge = position[hv][cat] - mTranslatedMaxWidth * mViolinWidth[hv][cat][mColor.length-1][fraction];
		return colorIndex == -1 ? leftEdge : leftEdge + 2f * mTranslatedMaxWidth * mViolinWidth[hv][cat][colorIndex][fraction];
	}

	private void addToViolinPath(float fractionCoord, float densityCoord, Path2D.Float path) {
		if (mDoubleAxis == 1)
			path.lineTo(densityCoord, fractionCoord);
		else
			path.lineTo(fractionCoord, densityCoord);
	}
}
