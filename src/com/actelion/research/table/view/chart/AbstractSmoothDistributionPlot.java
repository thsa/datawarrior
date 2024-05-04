package com.actelion.research.table.view.chart;

import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.JVisualization2D;
import com.actelion.research.table.view.VisualizationPoint;
import com.actelion.research.util.ColorHelper;

import java.awt.*;
import java.awt.geom.Path2D;

public abstract class AbstractSmoothDistributionPlot extends AbstractDistributionPlot {
	protected static final int FRACTIONS = 120; // number of fractions for full data range and default smoothing=0.5
	protected static final int SMOOTHING_FRACTIONS = 12; // absolute number of fractions used for smoothing for one side of Gaussian curve
	private static final float SMOOTHING_FACTOR = 2; // min and max total range fractions is FRACTIONS '/' and '*' exp(SMOOTHING_FACTOR), respectively
	protected static final int SCREEN_WIDTH_FRACTIONS = 7;
	protected static final float VIOLIN_WIDTH_FACTOR = 0.95f;  // We devide space such that violins never overlap with the default marker size == 1
	public static final float DEFAULT_MARGIN = 2.5f * SCREEN_WIDTH_FRACTIONS / FRACTIONS;   // 2.0 is exact fit
	protected float[][][][] mViolinWidth;
	protected int[][][][] mPointsInColorFraction;
	protected float mMaxViolinWidth,mTranslatedMaxWidth;

	public AbstractSmoothDistributionPlot(JVisualization visualization, int hvCount, int doubleAxis) {
		super(visualization, hvCount, doubleAxis);
	}

	@Override
	public boolean paintMarker(VisualizationPoint vp) {
		return vp.chartGroupIndex == -1;    // visible NaN
	}

	@Override
	public void calculate() {
		super.calculateStatistics();

		int fullRangeFractions = (int)Math.round(FRACTIONS * Math.exp(0.5*SMOOTHING_FACTOR * (((JVisualization2D)mVisualization).getEdgeSmoothing() - 0.5)));
		int fractions = Math.round(fullRangeFractions * (float)mVisualization.getRelativeVisibleRange(mDoubleAxis));
		if (fractions == 0)
			return;

		double axisVisMin = mVisualization.getVisibleMin(mDoubleAxis);
		double axisVisMax = mVisualization.getVisibleMax(mDoubleAxis);

		// Assign visualization point contributions to violin segments(i.e. fractions).
		// Depending on exact position split VP's contribution (1.0) and assign to two adjacent fractions.
		float[][][][] count = new float[mHVCount][mCatCount][mColor.length][fractions+1];
		mPointsInColorFraction = new int[mHVCount][mCatCount][mColor.length][fractions];
		for (VisualizationPoint vp:mVisualization.getDataPoints()) {
			if (mVisualization.isVisible(vp)) {
				if (mVisualization.isNaNOnAxis(vp)) {
					vp.chartGroupIndex = -1;     // visible but NaN
				}
				else {
					vp.chartGroupIndex = 1;     // visible and not NaN
					float v = mVisualization.getAxisValue(vp.record, mDoubleAxis);
					if (!Float.isNaN(v)) {
						v = (float)((v-axisVisMin) / (axisVisMax-axisVisMin));
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
		int influences = SMOOTHING_FRACTIONS;
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
	public void calculateCoordinates(Rectangle baseGraphRect) {
		if (mViolinWidth == null || mViolinWidth.length == 0 || mViolinWidth[0].length == 0)
			return;

		// initialize arrays to be filled
		mSCenter = new float[mHVCount][mCatCount];
		mSMean = new float[mHVCount][mCatCount];
		mSMedian = new float[mHVCount][mCatCount];
		mSLAV = new float[mHVCount][mCatCount];
		mSUAV = new float[mHVCount][mCatCount];
		mSQ1 = new float[mHVCount][mCatCount];
		mSQ3 = new float[mHVCount][mCatCount];
		calculateScreenCoordinates(baseGraphRect);

		mTranslatedMaxWidth = VIOLIN_WIDTH_FACTOR * mBarWidth / mMaxViolinWidth * mVisualization.getMarkerSize();
		calculatePseudoMarkerPositions(baseGraphRect);
	}

	@Override
	public void paint(Graphics2D g, Rectangle baseBounds, Rectangle baseGraphRect) {
		if (mViolinWidth == null || mViolinWidth.length == 0 || mViolinWidth[0].length == 0)
			return;

		Composite original = null;
		float markerTransparency = ((JVisualization2D)mVisualization).getMarkerTransparency();
		if (markerTransparency != 0.0) {
			original = g.getComposite();
			Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(1.0-markerTransparency));
			g.setComposite(composite);
		}

		drawConnectionLines(g);

		float lineWidth = Math.min(((JVisualization2D)mVisualization).getFontHeightScaledToSplitView()/6f, mBarWidth/8f);
		Stroke outlineStroke = new BasicStroke(0.5f*lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

		Color strongGray = mVisualization.getContrastGrey(JVisualization.SCALE_STRONG);
		Color mediumGray = mVisualization.getContrastGrey(JVisualization.SCALE_MEDIUM);
		int fractions = mViolinWidth[0][0][0].length-1;

		Path2D.Float path = new Path2D.Float(Path2D.WIND_NON_ZERO, 2*mViolinWidth[0][0].length);

		float[] fc = new float[fractions+1];
		if (mDoubleAxis == 1)
			for (int f=0; f<=fractions; f++)
				fc[f] = baseGraphRect.y + (fractions-f) * (float)baseGraphRect.height / fractions;
		else
			for (int f=0; f<=fractions; f++)
				fc[f] = baseGraphRect.x + f * (float)baseGraphRect.width / fractions;

		for (int hv = 0; hv<mHVCount; hv++) {
			for (int cat=mCatCount-1; cat>=0; cat--) {
				for (int colorIndex = 0; colorIndex<mColor.length; colorIndex++) {
					int startFraction = 0;
					while (startFraction >= 0 && startFraction < fractions) {
						startFraction = buildViolinPath(startFraction, hv, cat, colorIndex-1, colorIndex, fc, path);
						if (startFraction != -1) {
							g.setColor(mColor[colorIndex]);
							g.fill(path);
						}
					}
				}

				if (((JVisualization2D)mVisualization).isDrawBarPieBoxOutline()) {
					int startFraction = 0;
					while (startFraction >= 0 && startFraction < fractions) {
						startFraction = buildViolinPath(startFraction, hv, cat, -1, mColor.length-1, fc, path);

						if (startFraction != -1) {
							g.setColor(strongGray);
							g.setStroke(outlineStroke);
							g.draw(path);
						}
					}
				}

				if (mVisualization.getBoxplotMeanMode() != cBoxplotMeanModeNoIndicator) {
					drawAVIndicators(g, mSLAV[hv][cat], mSUAV[hv][cat], mSCenter[hv][cat], lineWidth, mediumGray);
					drawQIndicators(g, mSQ1[hv][cat], mSQ3[hv][cat], mSCenter[hv][cat], lineWidth, mediumGray);
				}

				drawMeanIndicators(g, mSMedian[hv][cat], mSMean[hv][cat], mSCenter[hv][cat], lineWidth);

				float offset = 1.5f * (mDoubleAxis == 1 ? baseGraphRect.height : baseGraphRect.width) * SCREEN_WIDTH_FRACTIONS / (float)fractions;
				paintStatisticsInfo(g, baseGraphRect, offset);
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

	private void calculatePseudoMarkerPositions(Rectangle baseGraphRect) {
		// Assign screen positions, width, and height to individual violin contributing visualization points
		int fractions = mViolinWidth[0][0][0].length-1;
		double axisVisMin = mVisualization.getVisibleMin(mDoubleAxis);
		double axisVisMax = mVisualization.getVisibleMax(mDoubleAxis);
		double axisRange = axisVisMax - axisVisMin;
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
					float screenPos = translate(hv, cat, colorIndex-1, fraction)
							+ addDirection((0.5f + count[hv][cat][colorIndex][fraction]) * markerSize);

					if (mDoubleAxis == 1) {
						vp.screenX = screenPos;
						vp.widthOrAngle1 = markerSize;
						vp.screenY = mHVOffset[1][hv] + baseGraphRect.y + baseGraphRect.height + (float)((axisVisMin-v)*baseGraphRect.height / axisRange);
						vp.heightOrAngle2 = SCREEN_WIDTH_FRACTIONS * baseGraphRect.height / (float)fractions;
					}
					else {
						vp.screenX = mHVOffset[0][hv] + baseGraphRect.x + (float)((v-axisVisMin)*baseGraphRect.width / axisRange);
						vp.widthOrAngle1 = SCREEN_WIDTH_FRACTIONS * baseGraphRect.width / (float)fractions;
						vp.screenY = screenPos;
						vp.heightOrAngle2 = markerSize;
					}
					count[hv][cat][colorIndex][fraction]++;
				}
			}
		}
	}

	/**
	 * Starting from fraction f1, build a path of the next violin fragment between the given color indexes.
	 * If at the given fraction the violin width is 0.0, then increase f1 until populated width is found.
	 * Increases fractions and adds them to the violin path until either width gets 0.0 again or f1 reaches
	 * fc.length.
	 * @param f1
	 * @param hv
	 * @param cat
	 * @param colorIndex1 -1 ... mColor.length-1; -1 is a virtual index that implies a violin width of 0.0
	 * @param colorIndex2 0 ... mColor.length-1
	 * @param fc
	 * @param path
	 * @return -1 if no path was built
	 */
	private int buildViolinPath(int f1, int hv, int cat, int colorIndex1, int colorIndex2, float[] fc, Path2D.Float path) {
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
			path.moveTo(translate(hv, cat, colorIndex1, f1), mHVOffset[mDoubleAxis][hv] + fc[f1]);
		else
			path.moveTo(mHVOffset[mDoubleAxis][hv] + fc[f1], translate(hv, cat, colorIndex1, f1));

		int f2 = f1+1;
		while (f2 < fractions && !violinBetweenColorsIsEmpty(hv, cat, colorIndex1, colorIndex2, f2)) {
			addToViolinPath(mHVOffset[mDoubleAxis][hv] + fc[f2], translate(hv, cat, colorIndex1, f2), path);
			f2++;
		}
		addToViolinPath(mHVOffset[mDoubleAxis][hv] + fc[f2], translate(hv, cat, colorIndex1, f2), path);
		if (!violinBetweenColorsIsEmpty(hv, cat, colorIndex1, colorIndex2, f2))
			addToViolinPath(mHVOffset[mDoubleAxis][hv] + fc[f2], translate(hv, cat, colorIndex2, f2), path);

		for (int f=f2-1; f>f1; f--)
			addToViolinPath(mHVOffset[mDoubleAxis][hv] + fc[f], translate(hv, cat, colorIndex2, f), path);

		if (!violinBetweenColorsIsEmpty(hv, cat, colorIndex1, colorIndex2, f1))
			addToViolinPath(mHVOffset[mDoubleAxis][hv] + fc[f1], translate(hv, cat, colorIndex2, f1), path);

		path.closePath();

		return f2 + 1;
	}

	private boolean violinBetweenColorsIsEmpty(int hv, int cat, int colorIndex1, int colorIndex2, int fraction) {
		if (colorIndex1 == -1)
			return mViolinWidth[hv][cat][colorIndex2][fraction] == 0f;

		return mViolinWidth[hv][cat][colorIndex1][fraction] == mViolinWidth[hv][cat][colorIndex2][fraction];
	}

	protected float addDirection(float screenDelta) {
		return screenDelta;
	}

	protected abstract float translate(int hv, int cat, int colorIndex, int fraction);

	protected abstract void drawQIndicators(Graphics2D g, float q1, float q3, float bar, float lineWidth, Color color);
	protected abstract void drawAVIndicators(Graphics2D g, float lav, float uav, float bar, float lineWidth, Color color);


	private void addToViolinPath(float fractionCoord, float densityCoord, Path2D.Float path) {
		if (mDoubleAxis == 1)
			path.lineTo(densityCoord, fractionCoord);
		else
			path.lineTo(fractionCoord, densityCoord);
	}
}
