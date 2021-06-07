/*
 * Copyright 2017 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91, CH-4123 Allschwil, Switzerland
 *
 * This file is part of DataWarrior.
 * 
 * DataWarrior is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * DataWarrior is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with DataWarrior.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package com.actelion.research.table.view;

import com.actelion.research.calc.CorrelationCalculator;
import com.actelion.research.calc.INumericalDataColumn;
import com.actelion.research.chem.Depictor2D;
import com.actelion.research.chem.DepictorTransformation;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.MarkerLabelDisplayer;
import com.actelion.research.table.category.CategoryList;
import com.actelion.research.table.category.CategoryMolecule;
import com.actelion.research.table.model.*;
import com.actelion.research.table.view.graph.RadialGraphOptimizer;
import com.actelion.research.table.view.graph.TreeGraphOptimizer;
import com.actelion.research.table.view.graph.VisualizationNode;
import com.actelion.research.util.ColorHelper;
import com.actelion.research.util.DoubleFormat;
import org.nfunk.jep.JEP;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.awt.print.PageFormat;
import java.io.*;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.*;

import static com.actelion.research.table.view.VisualizationColor.cUseAsFilterColor;

public class JVisualization2D extends JVisualization {
	private static final long serialVersionUID = 0x00000001;

	private static final int cSimpleShapeCount = 7;
	private static final float[][] cExtendedShapeCoords = {
			{0.28f,-0.49f,0.56f,0f,0.28f,0.49f,-0.28f,0.49f,-0.56f,0f,-0.28f,-0.49f}, // hexagon
			{0f,-0.70f,0.19f,-0.26f,0.66f,-0.22f,0.3f,0.1f,0.41f,0.57f,0f,0.32f,-0.41f,0.57f,-0.3f,0.1f,-0.66f,-0.22f,-0.19f,-0.26f}, // 5-star
			{0.8f,0f,-0.8f,0.6f,-0.2f,0f,-0.8f,-0.6f}, // arrow triangle right
			{0.42f,-0.60f,0.18f,-0.18f,0.60f,-0.42f,0.60f,0.42f,0.18f,0.18f,0.42f,0.60f,-0.42f,0.60f,-0.18f,0.18f,-0.60f,0.42f,-0.60f,-0.42f,-0.18f,-0.18f,-0.42f,-0.60f}, // quadrofoglio
			{0f,-0.2f,0.6f,-0.6f,0.6f,0.6f,0f,0.2f,-0.6f,0.6f,-0.6f,-0.6f}, // double triangle
			{0.8f,-0.6f,0.2f,0f,0.8f,0.6f,-0.8f,0f}, // arrow triange left
			{0f,-0.32f,0.35f,-0.61f,0.28f,-0.16f,0.70f,0f,0.28f,0.16f,0.35f,0.61f,0f,0.32f,-0.35f,0.61f,-0.28f,0.16f,-0.70f,0f,-0.28f,-0.16f,-0.35f,-0.61f}, // 6-star
			{-0.22f,-0.66f,0.22f,-0.66f,0.22f,-0.22f,0.66f,-0.22f,0.66f,0.22f,0.22f,0.22f,0.22f,0.66f,-0.22f,0.66f,-0.22f,0.22f,-0.66f,0.22f,-0.66f,-0.22f,-0.22f,-0.22f}, // swiss ross
			{0f,-0.31f,0.31f,-0.62f,0.62f,-0.31f,0.31f,0f,0.62f,0.31f,0.31f,0.62f,0f,0.31f,-0.31f,0.62f,-0.62f,0.31f,-0.31f,0f,-0.62f,-0.31f,-0.31f,-0.62f}, // rotated swiss cross
			{0f,-0.75f,0.75f,-0.25f,0.25f,-0.25f,0.25f,0.25f,0.75f,0.25f,0f,0.75f,-0.75f,0.25f,-0.25f,0.25f,-0.25f,-0.25f,-0.75f,-0.25f}, // double arrow
			{0.38f,-0.66f,0.21f,-0.12f,0.76f,0f,0.38f,0.66f,0f,0.24f,-0.38f,0.66f,-0.76f,0f,-0.21f,-0.12f,-0.38f,-0.66f}, // trifoglio
			{0.19f,-0.19f,0.19f,-0.57f,0.57f,-0.57f,0.57f,0.57f,0.19f,0.57f,0.19f,0.19f,-0.19f,0.19f,-0.19f,0.57f,-0.57f,0.57f,-0.57f,-0.57f,-0.19f,-0.57f,-0.19f,-0.19f}, // H
	};
	public static final int cAvailableShapeCount = cSimpleShapeCount + cExtendedShapeCoords.length;

	public static final int BACKGROUND_VISIBLE_RECORDS = -1;
	public static final int BACKGROUND_ALL_RECORDS = -2;

	private static final float cMarkerSize = 0.028f;
	private static final float cConnectionLineWidth = 0.005f;
	private static final float cMaxPieSize = 1.0f;
	private static final float cMaxBarReduction = 0.66f;
	private static final float cMinTextLabelSpace = 1.3f;	// multiplied with text height is minimum distance between category text labels
	private static final float cMinStructureLabelSpace = 2.0f;	// multiplied with text height is minimum distance between structure labels
	private static final float ARROW_TIP_SIZE = 0.6f;
	private static final float OUTLINE_LIMIT = 3;

	private static final int cPrintScaling = 16;

	private static final float MARKER_OUTLINE = 0.7f;
	private static final float NAN_WIDTH = 2.0f;
	private static final float NAN_SPACING = 0.5f;
	private static final float AXIS_TEXT_PADDING = 0.5f;
	private static final int FLOAT_DIGITS = 4;  // significant digits for in-view floating point values
	private static final int INT_DIGITS = 8;    // significant digits for in-view integer values

	private static final float STATISTIC_LABEL_FONT_FACTOR = 0.8f;
	private static final float CROSSHAIR_LABEL_BORDER = 0.15f;
	private static final float CROSSHAIR_ALPHA = 0.8f;
	private static final int DEFAULT_STRUCTURE_SCALE_SIZE = 4; // is multiplied by font size

	// if the delay between recent repaint() and paintComponent() is larger than this, we assume a busy EDT and paint with low detail
	private static final long MAX_REPAINT_DELAY_FOR_FULL_DETAILS = 100;

	// if paintComponent() with full detail takes more than this, the next paintComponent() will skip detail if EDT is busy or paintComponent()s are believed to be adjusting
	private static final long MAX_FULL_DETAIL_PAINT_TIME = 80;

	// delay between finishing last paintComponent() till start of next one. Delays smaller than this indicate adjusting=true, i.e. more paints to expect
	private static final long MAX_MILLIS_BETWEEN_LOW_DETAIL_PAINTS = 500;

	// delay between low detail paint and next automatic full detail paint, unless another low detail paint comes in between
	private static final long SLEEP_MILLIS_UNTIL_FULL_DETAIL_PAINT = 500;

	public static final String[] SCALE_MODE_TEXT = { "On both axes", "Hide all scales", "On X-axis only", "On Y-axis only" };
	public static final String[] GRID_MODE_TEXT = { "Show full grid", "Hide any grid", "Vertical lines only", "Horizontal lines only" };
	public static final String[] CROSSHAIR_MODE_TEXT = { "Automatic", "On both axes", "On X-axis only", "On Y-axis only", "Never ever" };
	public static final String[] CROSSHAIR_MODE_CODE = { "automatic", "both", "x", "y", "none" };
	public static final int CROSSHAIR_MODE_AUTOMATIC = 0;
	private static final int CROSSHAIR_MODE_BOTH = 1;
	private static final int CROSSHAIR_MODE_X = 2;
	private static final int CROSSHAIR_MODE_Y = 3;
	private static final int CROSSHAIR_MODE_NONE = 4;

	public static final String[] CURVE_MODE_TEXT = { "<none>", "Vertical Line", "Horizontal Line", "Fitted Line", "Smooth Curve", "Use Formula" };
	public static final String[] CURVE_MODE_CODE = { "none", "abscissa", "ordinate", "fitted", "smooth", "formula" };
	private static final int cCurveModeNone = 0;
	private static final int cCurveModeMask = 7;
	public static final int cCurveModeVertical = 1;
	public static final int cCurveModHorizontal = 2;
	public static final int cCurveModeFitted = 3;
	public static final int cCurveModeSmooth = 4;
	public static final int cCurveModeExpression = 5;
	private static final int cCurveStandardDeviation = 8;
	private static final int cCurveSplitByCategory = 16;

	public static final float DEFAULT_CURVE_LINE_WIDTH = 1.5f;
	public static final float DEFAULT_CURVE_SMOOTHING = 0.5f;

	private static final int[] SUPPORTED_CHART_TYPE = { cChartTypeScatterPlot, cChartTypeWhiskerPlot, cChartTypeBoxPlot, cChartTypeBars, cChartTypePies };

	private static final int cDefaultScaleStyle = cScaleStyleFrame;

	private static final int cScaleTextNormal = 1;
	private static final int cScaleTextAlternating = 2;
	private static final int cScaleTextInclined = 3;
	private static final int cScaleTextVertical = 4;

	public static final int cMultiValueMarkerModeNone = 0;
	private static final int cMultiValueMarkerModePies = 1;
	private static final int cMultiValueMarkerModeBars = 2;
	public static final String[] MULTI_VALUE_MARKER_MODE_TEXT = { "<none>", "Pie Pieces", "Bars" };
	public static final String[] MULTI_VALUE_MARKER_MODE_CODE = { "none", "pies", "bars" };

	private static final String[] SPLIT_VIEW_EXCEEDED_MESSAGE = {"Split view count exceeded!", "Use filters to hide rows or", "don't configure view splitting."};

	private volatile boolean mSkipPaintDetails;
	private volatile Thread	mFullDetailPaintThread;

	private Graphics2D		mG;
	private Composite		mLabelBackgroundComposite;
	private Stroke          mThinLineStroke,mNormalLineStroke,mFatLineStroke,mVeryFatLineStroke,mConnectionStroke;
	private float[]			mCorrelationCoefficient;
	private float			mFontScaling,mMarkerTransparency,mCurveLineWidth,mCurveSmoothing;
	private int				mBorder,mCurveInfo,mBackgroundHCount,mBackgroundVCount,mCrossHairMode,
							mBackgroundColorRadius,mBackgroundColorFading,mBackgroundColorConsidered,
							mConnectionFromIndex1,mConnectionFromIndex2,mShownCorrelationType,mMultiValueMarkerMode;
	private long			mPreviousPaintEnd,mPreviousFullDetailPaintMillis,mMostRecentRepaintMillis;
	private boolean			mBackgroundValid,mIsHighResolution,mScaleTitleCentered,
							mDrawMarkerOutline,mDrawBarPieBoxOutline;
	private int[]			mScaleTextMode,mScaleDepictorOffset,mSplittingMolIndex,mMultiValueMarkerColumns;
	private int[]           mScaleSize; // 0: label area height beneath X-axis; 1: label area width left of y-axis
	private int[]           mNaNSize;   // 0: NaN area width left of Y-axis; 1: NaN area height beneath X-axis
	protected VisualizationColor	mBackgroundColor;
	private LabelHelper     mLabelHelper;
	private Color[]			mMultiValueMarkerColor;
	private Color[][][]		mBackground;
	private Depictor2D[][]	mScaleDepictor,mSplittingDepictor;
	private VolatileImage	mOffImage;
	private BufferedImage   mBackgroundImage;		// primary data
	private byte[]			mBackgroundImageData;	// cached if delivered or constructed from mBackgroundImage if needed
//	private byte[]			mSVGBackgroundData;		// alternative to mBackgroundImage
	private Graphics2D		mOffG;
	private ArrayList<ScaleLine>[]	mScaleLineList;
	private float[][]       mCurveXMin,mCurveXInc,mCurveStdDev;
	private float[][][]     mCurveY;
	private String          mCurveExpression;

	@SuppressWarnings("unchecked")
	public JVisualization2D(CompoundTableModel tableModel,
							CompoundListSelectionModel selectionModel) {
		super(tableModel, selectionModel, 2);
		mPoint = new VisualizationPoint2D[0];
		mNaNSize = new int[2];
		mScaleSize = new int[2];
		mScaleTextMode = new int[2];
		mScaleDepictor = new Depictor2D[2][];
		mScaleDepictorOffset = new int[2];
		mScaleStyle = cDefaultScaleStyle;
		mScaleLineList = new ArrayList[2];
		mScaleLineList[0] = new ArrayList<>();
		mScaleLineList[1] = new ArrayList<>();
		mSplittingDepictor = new Depictor2D[2][];
		mSplittingMolIndex = new int[2];
		mSplittingMolIndex[0] = cColumnUnassigned;
		mSplittingMolIndex[1] = cColumnUnassigned;

		mBackgroundColor = new VisualizationColor(mTableModel, this);

		initialize();
		}

	protected void initialize() {
		super.initialize();
		mBackgroundColorConsidered = BACKGROUND_VISIBLE_RECORDS;
		mMarkerShapeColumn = cColumnUnassigned;
		mChartColumn = cColumnUnassigned;
		mChartMode = cChartModeCount;
		mBackgroundColorRadius = 10;
		mBackgroundColorFading = 10;
		mBackgroundImage = null;
		mBackgroundImageData = null;
		mMultiValueMarkerColumns = null;
		mMultiValueMarkerMode = cMultiValueMarkerModePies;
		mShownCorrelationType = CorrelationCalculator.TYPE_NONE;
		mScaleTitleCentered = true;
		mDrawMarkerOutline = true;
		mDrawBarPieBoxOutline = false;
		mCurveLineWidth = DEFAULT_CURVE_LINE_WIDTH;
		mCurveSmoothing = DEFAULT_CURVE_SMOOTHING;
		mCrossHairMode = CROSSHAIR_MODE_AUTOMATIC;
		}

	@Override
	public boolean supportsLabelPositionOptimization() {
		return true;
		}

	@Override
	protected void determineWarningMessage() {
		super.determineWarningMessage();
		if (mWarningMessage == null
		 && (mCurveInfo & cCurveSplitByCategory) != 0
		 && mMarkerColor.getColorColumn() != cColumnUnassigned
		 && mMarkerColor.getColorListMode() != VisualizationColor.cColorListModeCategories)
			mWarningMessage = "Choose 'Color by categories' in marker color dialog to split lines/curves by color category!";
		}

	@Override
	public void repaint() {
		super.repaint();
		mMostRecentRepaintMillis = System.currentTimeMillis();
		}

	@Override
	public synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (!mIsFastRendering)
			((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		mIsHighResolution = false;

		mWarningMessage = null;
		determineWarningMessage();

		int width = getWidth();
		int height = getHeight();

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();

		float retinaFactor = HiDPIHelper.getRetinaScaleFactor();
		if (mOffImage == null
		 || mOffImage.getWidth(null) != width*retinaFactor
		 || mOffImage.getHeight(null) != height*retinaFactor) {
			mOffImage = gc.createCompatibleVolatileImage(Math.round(width*retinaFactor), Math.round(height*retinaFactor), Transparency.OPAQUE);
			mCoordinatesValid = false;
			}

		if (!mCoordinatesValid)
			mOffImageValid = false;

		if (!mOffImageValid) {
			do  {
				if (mOffImage.validate(gc) == VolatileImage.IMAGE_INCOMPATIBLE)
					mOffImage = gc.createCompatibleVolatileImage(Math.round(width*retinaFactor), Math.round(height*retinaFactor), Transparency.OPAQUE);

				mOffG = null;
				try {
					mOffG = mOffImage.createGraphics();

					final long start = System.currentTimeMillis();
					if (!mSkipPaintDetails) {	// if we are not already in skip detail mode
						if (start - mMostRecentRepaintMillis > MAX_REPAINT_DELAY_FOR_FULL_DETAILS
						  || (start - mPreviousPaintEnd < MAX_MILLIS_BETWEEN_LOW_DETAIL_PAINTS
						   && mPreviousFullDetailPaintMillis > MAX_FULL_DETAIL_PAINT_TIME)) {
							mSkipPaintDetails = true;
							mFullDetailPaintThread = null;
							}
//						System.out.println(this.toString()+"; millis since repaint:"+(start - mMostRecentRepaintMillis)+" since recent paint end:"
//								+(start - mPreviousPaintEnd)+" recent full paint millis:"+mPreviousFullDetailPaintMillis+" skipping details:"+mSkipPaintDetails);
						}

					if (retinaFactor != 1f)
						mOffG.scale(retinaFactor, retinaFactor);
					if (!mIsFastRendering && !mSkipPaintDetails)
						mOffG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//					mOffG.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);	// no sub-pixel accuracy looks cleaner
		
					mOffG.setColor(getViewBackground());
					mOffG.fillRect(0, 0, width, height);
					Insets insets = getInsets();
					Rectangle bounds = new Rectangle(insets.left, insets.top, width-insets.left-insets.right, height-insets.top-insets.bottom);
		
					mCorrelationCoefficient = null;

					mFontHeight = calculateFontSize(bounds.width, bounds.height, 1f, retinaFactor, true);

					mG = mOffG;

					paintContent(bounds, false);

					if (mWarningMessage != null) {
						setColor(Color.RED);
						setFontHeight(mFontHeight);
						drawString(mWarningMessage, width/2 - getStringWidth(mWarningMessage)/2, mFontHeight);
						}

					if (mSkipPaintDetails) {
						mFullDetailPaintThread = new Thread(() -> {
							try { Thread.sleep(SLEEP_MILLIS_UNTIL_FULL_DETAIL_PAINT); } catch (InterruptedException ie) {}
							if (Thread.currentThread() == mFullDetailPaintThread) {
								mSkipPaintDetails = false;
								mOffImageValid = false;
								repaint();
								}
							});
						mFullDetailPaintThread.start();
						}

					mPreviousPaintEnd = System.currentTimeMillis();
					if (!mSkipPaintDetails)
						mPreviousFullDetailPaintMillis = System.currentTimeMillis() - start;
					}
				finally {	// It's always best to dispose of your Graphics objects.
					mOffG.dispose();
					}
				} while (mOffImage.contentsLost());

			mOffImageValid = true;
			}

		g.drawImage(mOffImage, 0, 0, width, height, this);

		if (mActivePoint != null && isVisible(mActivePoint))
			markReference((Graphics2D)g);

		if (mHighlightedPoint != null)
			markHighlighted((Graphics2D)g);

		if (showCrossHair())
			drawCrossHair((Graphics2D)g);

		drawSelectionOutline(g);

		if (mSplittingColumn[0] != cColumnUnassigned && !isSplitView()) {
			g.setColor(Color.RED.darker());
			int fontSize = 3*mFontHeight;
			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, fontSize));
			for (int i=0; i<SPLIT_VIEW_EXCEEDED_MESSAGE.length; i++) {
				int w = (int)g.getFontMetrics().getStringBounds(SPLIT_VIEW_EXCEEDED_MESSAGE[i], g).getWidth();
				g.drawString(SPLIT_VIEW_EXCEEDED_MESSAGE[i], (width-w)/2, height/2+(i*2-SPLIT_VIEW_EXCEEDED_MESSAGE.length-1)*fontSize);
				}
			}
		}

	@Override
	public int print(Graphics g, PageFormat f, int pageIndex) {
		if (pageIndex != 0)
			return NO_SUCH_PAGE;

		Rectangle bounds = new Rectangle((int)(cPrintScaling * f.getImageableX()),
										 (int)(cPrintScaling * f.getImageableY()),
										 (int)(cPrintScaling * f.getImageableWidth()),
										 (int)(cPrintScaling * f.getImageableHeight()));

		paintHighResolution((Graphics2D)g, bounds, cPrintScaling, false, true);

		return PAGE_EXISTS;
		}

	@Override
	public synchronized void paintHighResolution(Graphics2D g, Rectangle bounds, float fontScaling, boolean transparentBG, boolean isPrinting) {
			// font sizes are optimized for screen resolution are need to be scaled by fontScaling
		mIsHighResolution = true;
		mFontScaling = fontScaling;

		mCoordinatesValid = false;
		mBackgroundValid = false;

		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);	no sub-pixel accuracy looks cleaner

		mFontHeight = calculateFontSize(bounds.width, bounds.height, fontScaling, 1f, false);

		mG = g;

		if (isPrinting)
				// fontScaling was also used to inflate bounds to gain resolution
				// and has to be compensated by inverted scaling of the g2D
			g.scale(1.0/fontScaling, 1.0/fontScaling);

		if (!transparentBG) {
			setColor(getViewBackground());
			fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
			}

		if (bounds.width > 0 && bounds.height > 0)
			paintContent(bounds, transparentBG);

		mCoordinatesValid = false;
		mBackgroundValid = false;
		}

	/**
	 * If we need space for statistics labels, then reduce the area that we have for the bar.
	 * If we show a scale reflecting the bar values, then we need to transform scale labels
	 * accordingly.
	 * @param baseRect
	 */
	private void adaptDoubleScalesForStatisticalLabels(final Rectangle baseRect) {
		if (mChartType == cChartTypeBars) {
			for (int axis=0; axis<2; axis++) {
				if (axis == mChartInfo.barAxis) {
					float cellSize = (axis == 0 ? baseRect.width : baseRect.height) / (float)getCategoryVisCount(axis);
					float spacing = cellSize * getBarChartEmptyLabelAreaSpacing();
					float labelSize = calculateStatisticsLabelSize(axis == 0, spacing);
					if (labelSize != 0f) {
						float reduction = Math.min(cMaxBarReduction * cellSize, labelSize - spacing);
						float appliedHeight = (cellSize - reduction);

						// if the axis is not assigned to a category column, then we have a double value scale
						if (mAxisIndex[axis] == cColumnUnassigned) {
							float shift = isRightBarChart() ? reduction / cellSize : 0f;

							for (ScaleLine sl : mScaleLineList[axis])
								sl.position = shift + sl.position * appliedHeight / cellSize;
							}
						// if the bar axis shows category values, then we correct centered bars only: label must be at bar base
						else if (isCenteredBarChart()) {
							float lowFraction = mChartInfo.axisMin / (mChartInfo.axisMin - mChartInfo.axisMax);
							float shift = (lowFraction * appliedHeight / cellSize - lowFraction) / (float)getCategoryVisCount(axis);

							for (ScaleLine sl : mScaleLineList[axis])
								sl.position += shift;
							}

						float originalRange = mChartInfo.axisMax - mChartInfo.axisMin;
						if (!isRightBarChart())
							mChartInfo.axisMax = mChartInfo.axisMin + originalRange * cellSize / appliedHeight;
						else
							mChartInfo.axisMin = mChartInfo.axisMax - originalRange * cellSize / appliedHeight;
						}
					}
				}
			}
		else if (mChartType == cChartTypePies) {
			if (mAxisIndex[1] != cColumnUnassigned) {
				float cellHeight = baseRect.height / (float)getCategoryVisCount(1);
				float labelSize = Math.min(cellHeight / 2f, calculateStatisticsLabelSize(false, 0f));
				if (labelSize != 0f) {
					float shift = labelSize / (2f * baseRect.height);
					for (ScaleLine sl : mScaleLineList[1])
						sl.position += shift;
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
	private float calculateStatisticsLabelSize(boolean isWidth, float threshold) {
		int labelCount = getBarOrPieLabelCount();
		if (labelCount == 0)
			return 0f;

		float scaledFontHeight = scaleIfSplitView(mFontHeight);
		float size = isWidth ? scaledFontHeight + calculateStatisticalLabelWidth(labelCount) : scaledFontHeight * (0.5f + labelCount);
		return size > threshold ? size : 0f;
		}

	/**
	 * Returns the string width of the longest statistical label shown in bar/box/whisker/pie chart
	 * @param labelCount
	 * @return
	 */
	private float calculateStatisticalLabelWidth(int labelCount) {
		int scaledFontHeight = Math.round(scaleIfSplitView(STATISTIC_LABEL_FONT_FACTOR * mFontHeight));
		mG.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, scaledFontHeight));
		int width = 0;
		String[] lineText = new String[labelCount];
		for (int hv=0; hv<mHVCount; hv++) {
			for (int i = 0; i<getCategoryVisCount(0); i++) {
				for (int j = 0; j<getCategoryVisCount(1); j++) {
					for (int k=0; k<mCaseSeparationCategoryCount; k++) {
						int cat = (i+j* getCategoryVisCount(0))*mCaseSeparationCategoryCount+k;
						int count = compileBarAndPieStatisticsLines(hv, cat, lineText);
						for (int l=0; l<count; l++)
							width = Math.max(width, mG.getFontMetrics().stringWidth(lineText[l]));
						}
					}
				}
			}
		return width;
		}


	/**
	 * @return rough estimate of how many characters the longest label will be
	 *
	private int getLabelWidth() {
		int width = FLOAT_DIGITS;
		if (isShowConfidenceInterval())
			width = Math.max(width, 8+2*FLOAT_DIGITS);
		if (isShowMeanAndMedianValues())
			width = Math.max(width, 7+FLOAT_DIGITS);
		if (isShowStandardDeviation())
			width = Math.max(width, 4+FLOAT_DIGITS);
		if (isShowValueCount())
			width = Math.max(width, 3+(int)Math.log10(mTableModel.getTotalRowCount()));  // N=nnn
		if (isShowBarOrPieSizeValue())
			width = Math.max(width, getBarOrPieSizeValueDigits());

		return width;
		}*/

	/**
	 * @return number of labels, i.e. label line count in bar or pie charts
	 */
	private int getBarOrPieLabelCount() {
		int count = 0;
		if (isShowValueCount())
			count++;
		if (isShowBarOrPieSizeValue())
			count++;
		if (isShowConfidenceInterval())
			count++;
		if (isShowMeanAndMedianValues())
			count++;
		if (isShowStandardDeviation())
			count++;
		return count;
		}

	private void paintContent(final Rectangle bounds, boolean transparentBG) {
		if (validateSplittingIndices())
			mBackgroundValid = false;

		mChartInfo = null;
		mLabelHelper = null;

		calculateMarkerSize(bounds);    // marker sizes are needed for size legend
		calculateLegend(bounds, (int)scaleIfSplitView(mFontHeight));

		if (isSplitView()) {
			int scaledFontHeight = Math.round(scaleIfSplitView(mFontHeight));
			if (mLegendList.size() != 0)
				bounds.height -= scaledFontHeight / 2;
			compileSplittingHeaderMolecules();
			int count1 = mShowEmptyInSplitView ? mTableModel.getCategoryCount(mSplittingColumn[0]) : calculateVisibleCategoryCount(mSplittingColumn[0]);
			int count2 = (mSplittingColumn[1] == cColumnUnassigned) ? -1
					: mShowEmptyInSplitView ? mTableModel.getCategoryCount(mSplittingColumn[1]) : calculateVisibleCategoryCount(mSplittingColumn[1]);
			boolean largeHeader = (mSplittingDepictor[0] != null
					|| mSplittingDepictor[1] != null);
			mSplitter = new VisualizationSplitter(bounds, count1, count2, scaledFontHeight, largeHeader, mSplittingAspectRatio);
			}

		switch (mChartType) {
			case cChartTypeBoxPlot:
			case cChartTypeWhiskerPlot:
				calculateBoxPlot();
				break;
			case cChartTypeBars:
			case cChartTypePies:
				double widthHeightRatio = (bounds.height <= 0) ? 1.0 : (double)bounds.width / (double)bounds.height;
				if (mSplitter != null)
					widthHeightRatio *= (double)mSplitter.getVCount() / (double)mSplitter.getHCount();
				calculateBarsOrPies(widthHeightRatio);
				break;
			}

		// total bounds of first split (or total unsplit) graphical including scales
		Rectangle baseBounds = isSplitView() ? mSplitter.getSubViewBounds(0) : bounds;

		boolean hasFreshCoords = false;
		if (!mCoordinatesValid) {
			calculateCoordinates(mG, baseBounds);
			hasFreshCoords = true;
			}

		// bounds of first split (or total unsplit) graph area. This excluded scales, NaN area, border, etc.
		Rectangle baseGraphRect = getGraphBounds(baseBounds);

		// We may need to enlarge the visible scale range to create space for statistics labels
		if (hasFreshCoords)
			adaptDoubleScalesForStatisticalLabels(baseGraphRect);

		if (mOptimizeLabelPositions
		 && mChartType != cChartTypeBars
		 && mChartType != cChartTypePies) {
			mLabelHelper = new LabelHelper(baseBounds, baseGraphRect);
			mLabelHelper.calculateLabels();
			mLabelHelper.optimizeLabels();
			}

		if ((mCurveInfo & cCurveModeMask) == cCurveModeSmooth && mCurveY == null)
			calculateSmoothCurve(baseGraphRect.width);
		if ((mCurveInfo & cCurveModeMask) == cCurveModeExpression && mCurveY == null)
			calculateExpressionCurve(baseGraphRect);

		float thinLineWidth = scaleIfSplitView(mFontHeight)/16f;
		if (mIsFastRendering) {
			mThinLineStroke = new BasicStroke(1, BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER);
			mNormalLineStroke = new BasicStroke(1, BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER);
			mFatLineStroke = new BasicStroke(2f, BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER);
			mVeryFatLineStroke = new BasicStroke(3f, BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER);
			mConnectionStroke = new BasicStroke(mAbsoluteConnectionLineWidth, BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER);
			}
		else {
			mThinLineStroke = new BasicStroke(thinLineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
			mNormalLineStroke = new BasicStroke(1.4f * thinLineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
			mFatLineStroke = new BasicStroke(2f * thinLineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
			mVeryFatLineStroke = new BasicStroke(3f * thinLineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
			mConnectionStroke = new BasicStroke(mAbsoluteConnectionLineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
			}

		mLabelBackgroundComposite = (showAnyLabels() && mShowLabelBackground) ?
				AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f-mLabelBackgroundTransparency) : null;

		if (isSplitView()) {
			int scaledFontHeight = Math.round(scaleIfSplitView(mFontHeight));
			float titleBrightness = ColorHelper.perceivedBrightness(getTitleBackground());
			float backgroundBrightness = ColorHelper.perceivedBrightness(getViewBackground());
			Color borderColor = (backgroundBrightness > titleBrightness) ? getTitleBackground().darker().darker()
																		 : getTitleBackground().brighter().brighter();
			mSplitter.paintGrid(mG, borderColor, getTitleBackground());
			for (int hv=0; hv<mHVCount; hv++)
				paintGraph(getGraphBounds(mSplitter.getSubViewBounds(hv)), hv, transparentBG);

			mG.setColor(getContrastGrey(SCALE_STRONG, getTitleBackground()));
			mG.setFont(new Font(Font.SANS_SERIF, Font.BOLD, scaledFontHeight));

			String[][] splittingCategory = getSplitViewCategories();

			for (int hv=0; hv<mHVCount; hv++) {
				Rectangle titleArea = mSplitter.getTitleBounds(hv);
				mG.setClip(titleArea);

				int molWidth = Math.min(titleArea.width*2/5, titleArea.height*3/2);
				int cat1Index = (mSplittingColumn[1] == cColumnUnassigned) ? hv : mSplitter.getHIndex(hv);
				String shortTitle1 = splittingCategory[0][cat1Index];
				String title1 = (mSplittingColumn[0] == CompoundTableListHandler.PSEUDO_COLUMN_SELECTION) ?
						shortTitle1 : mTableModel.getColumnTitleExtended(mSplittingColumn[0])+": "+shortTitle1;
				int title1Width = mSplittingDepictor[0] == null ?
								  mG.getFontMetrics().stringWidth(title1)
								: molWidth;
				String shortTitle2 = null;
				String title2 = null;
				int title2Width = 0;
				int totalWidth = title1Width;
				if (mSplittingColumn[1] != cColumnUnassigned) {
					shortTitle2 = splittingCategory[1][mSplitter.getVIndex(hv)];
					title2 = (mSplittingColumn[1] == CompoundTableListHandler.PSEUDO_COLUMN_SELECTION) ?
							shortTitle2 :mTableModel.getColumnTitleExtended(mSplittingColumn[1])+": "+shortTitle2;
					title2Width = mSplittingDepictor[1] == null ?
								  mG.getFontMetrics().stringWidth(title2)
								: molWidth;
					totalWidth += title2Width + mG.getFontMetrics().stringWidth(" | ");
					}

				int textY = titleArea.y+(1+titleArea.height-scaledFontHeight)/2+mG.getFontMetrics().getAscent();

				if (totalWidth > titleArea.width) {
					title1 = shortTitle1;
					title1Width = mSplittingDepictor[0] == null ?
								  mG.getFontMetrics().stringWidth(shortTitle1)
								: molWidth;
					totalWidth = title1Width;
					if (mSplittingColumn[1] != cColumnUnassigned) {
						title2 = shortTitle2;
						title2Width = mSplittingDepictor[1] == null ?
									  mG.getFontMetrics().stringWidth(title2)
									: molWidth;
						totalWidth += title2Width + mG.getFontMetrics().stringWidth(" | ");
						}
					}

				int x1 = titleArea.x+(titleArea.width-totalWidth)/2;
				if (mSplittingDepictor[0] == null)
					mG.drawString(title1, x1, textY);
				else if (mSplittingDepictor[0][cat1Index] != null) {
					Rectangle.Double r = new Rectangle.Double(x1, titleArea.y, molWidth, titleArea.height);
					int maxAVBL = Depictor2D.cOptAvBondLen;
					if (mIsHighResolution)
						maxAVBL *= mFontScaling;
					mSplittingDepictor[0][cat1Index].validateView(mG, r, Depictor2D.cModeInflateToMaxAVBL+maxAVBL);
					mSplittingDepictor[0][cat1Index].paint(mG);
					}

				if (mSplittingColumn[1] != cColumnUnassigned) {
					mG.drawString(" | ", titleArea.x+(titleArea.width-totalWidth)/2+title1Width, textY);

					int x2 = titleArea.x+(totalWidth+titleArea.width)/2-title2Width;
					if (mSplittingDepictor[1] == null)
						mG.drawString(title2, x2, textY);
					else if (mSplittingDepictor[1][mSplitter.getVIndex(hv)] != null) {
						Rectangle.Double r = new Rectangle.Double(x2, titleArea.y, molWidth, titleArea.height);
						int maxAVBL = Depictor2D.cOptAvBondLen;
						if (mIsHighResolution)
							maxAVBL *= mFontScaling;
						mSplittingDepictor[1][mSplitter.getVIndex(hv)].validateView(mG, r, Depictor2D.cModeInflateToMaxAVBL+maxAVBL);
						mSplittingDepictor[1][mSplitter.getVIndex(hv)].paint(mG);
						}
					}
				}

			mG.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, scaledFontHeight)); // set font back to plain
			mG.setClip(null);
			}
		else {
			mSplitter = null;
			paintGraph(baseGraphRect, 0, transparentBG);
			}

		if (baseGraphRect.width <= 0 || baseGraphRect.height <= 0)
			return;

		switch (mChartType) {
		case cChartTypeBars:
			paintBarChart(mG, baseBounds, baseGraphRect);
			break;
		case cChartTypePies:
			paintPieChart(mG, baseGraphRect);
			break;
		case cChartTypeScatterPlot:
			paintMarkers(baseBounds, baseGraphRect);
			break;
		case cChartTypeBoxPlot:
		case cChartTypeWhiskerPlot:
			paintMarkers(baseBounds, baseGraphRect);
			paintBoxOrWhiskerPlot(mG, baseGraphRect);
			break;
			}

		paintLegend(bounds, transparentBG);

		if (!mIsHighResolution)
			paintMessages(mG, bounds.x, bounds.width);
		}

	/**
	 * Returns the bounds of the graph area, provided that the given point
	 * is part of it (or part of its scale area, if tolerant==true).
	 * If we have split views, then the graph area of that view
	 * is returned, which contains the the given point.
	 * If point(x,y) is outside of the graph area (and scale area if tolerant==true),
	 * then null is returned.
	 * Scale, legend and border area is not part of the returned graph bounds.
	 * @param screenX
	 * @param screenY
	 * @param tolerant if true, then graph bounds are also returned if the screen point is in the scale area
	 * @return graph bounds or null (does not include retina factor)
	 */
	public Rectangle getGraphBounds(int screenX, int screenY, boolean tolerant) {
		if (isSplitView()) {
			int hv = mSplitter.getHVIndex(screenX, screenY, false);
			if (hv != -1) {
				Rectangle viewBounds = mSplitter.getSubViewBounds(hv);
				Rectangle bounds = getGraphBounds(viewBounds);
				if (tolerant) {
					int gap = Math.min(mBorder, (int)scaleIfSplitView(mFontHeight));
					if (screenX > viewBounds.x + gap
					 && screenX < viewBounds.x + viewBounds.width - mBorder
					 && screenY > viewBounds.y + mBorder
					 && screenY < viewBounds.y + viewBounds.height - gap)
						return bounds;
					}
				else {
					Rectangle tempBounds = new Rectangle(bounds);
					tempBounds.grow(2,2);	// to get away with uncertaintees of rounded input coordinates
					if (tempBounds.contains(screenX, screenY))
						return bounds;
					}
				}
			}
		else {
			int width = getWidth();
			int height = getHeight();
			Insets insets = getInsets();
			Rectangle allBounds = new Rectangle(insets.left, insets.top, width-insets.left-insets.right, height-insets.top-insets.bottom);
			for (VisualizationLegend legend:mLegendList)
				allBounds.height -= legend.getHeight();
			Rectangle bounds = getGraphBounds(allBounds);
			if (tolerant) {
				int gap = Math.min(mBorder, mFontHeight);
				if (screenX > allBounds.x + gap
				 && screenX < allBounds.x + allBounds.width - mBorder
				 && screenY > allBounds.y + mBorder
				 && screenY < allBounds.y + allBounds.height - gap)
					return bounds;
				}
			else {
				Rectangle tempBounds = new Rectangle(bounds);
				tempBounds.grow(2, 2);    // to get away with uncertaintees of rounded input coordinates
				if (tempBounds.contains(screenX, screenY))
					return bounds;
				}
			}
		return null;
		}

	/**
	 * Removes border, scale and NaN area from view area to yield the rectangle that can be used for
	 * the graph itself.
	 * @param bounds bounds of total view area minus without legend, may be sub view area
	 * @return bounds for drawing the graph
	 */
	private Rectangle getGraphBounds(Rectangle bounds) {
		float scaledFontHeight = scaleIfSplitView(mFontHeight);
		Rectangle graphBounds = new Rectangle(
				bounds.x + mBorder + mNaNSize[0] + mScaleSize[1],
				bounds.y + mBorder,
				bounds.width - mNaNSize[0] - mScaleSize[1] - 2 * mBorder,
				bounds.height - mNaNSize[1] - mScaleSize[0] - 2 * mBorder);

		if (mTreeNodeList == null) {
			float arrowSize = (mScaleStyle == cScaleStyleArrows) ? ARROW_TIP_SIZE * scaledFontHeight : 0f;
			if (mScaleTitleCentered) {
				if (showScale(0)) {
					graphBounds.width -= arrowSize;    // arrow triangle on x-axis
					if (mAxisIndex[0] != cColumnUnassigned || (mChartType == cChartTypeBars && mChartInfo.barAxis == 0))
						graphBounds.height -= (AXIS_TEXT_PADDING+1.0) * scaledFontHeight;
					}
				if (showScale(1)) {
					if (mAxisIndex[1] != cColumnUnassigned || (mChartType == cChartTypeBars && mChartInfo.barAxis == 1)) {
						graphBounds.x += (AXIS_TEXT_PADDING+1.0) * scaledFontHeight;
						graphBounds.width -= (AXIS_TEXT_PADDING+1.0) * scaledFontHeight;
						}
					graphBounds.y += arrowSize;
					graphBounds.height -= arrowSize;
					}
				}
			else {
				if (showScale(0)) {
					graphBounds.width -= arrowSize;    // arrow triangle on x-axis
					graphBounds.height -= scaledFontHeight;
					}
				if (showScale(1)) {
					graphBounds.y += scaledFontHeight;
					graphBounds.height -= scaledFontHeight;
					}
				}
			}

		return graphBounds;
		}

	private void paintGraph(Rectangle graphRect, int hvIndex, boolean transparentBG) {
		setFontHeightAndScaleToSplitView(mFontHeight);

		if (hasColorBackground()) {
			if (mSplitter != null
			 && (mSplitter.getHCount() != mBackgroundHCount
			  || mSplitter.getVCount() != mBackgroundVCount))
				mBackgroundValid = false;

			if (!mBackgroundValid)
				calculateBackground(graphRect, transparentBG);
			}

		if (mShowNaNValues)
			drawNaNArea(mG, graphRect);

		if (mBackgroundImage != null
		 || hasColorBackground())
			drawBackground(mG, graphRect, hvIndex);

		if (mTreeNodeList == null) {
			if (mGridMode != cGridModeHidden || mScaleMode != cScaleModeHidden)
				drawGrid(mG, graphRect);	// draws grid and scale labels
			if ((mCurveInfo & cCurveStandardDeviation) == cCurveStandardDeviation) {
				if (getCurveMode() == cCurveModeSmooth)
					drawSmoothCurveArea(hvIndex, graphRect);
				else if (getCurveMode() == cCurveModeExpression)
					drawExpressionCurveArea(hvIndex, graphRect);
				}
			if (mScaleMode != cScaleModeHidden)
				drawAxes(mG, graphRect);
			}
		}

	private boolean hasColorBackground() {
		return mChartType != cChartTypeBars
	   		&& mChartType != cChartTypeBoxPlot
	   		&& mBackgroundColor.getColorColumn() != cColumnUnassigned;
		}

	private void paintMarkers(Rectangle baseBounds, Rectangle baseGraphRect) {
		if (mTreeNodeList != null && mTreeNodeList.length == 0)
			return;	// We have a detail graph view without root node (no active row chosen)

		Composite original = null;
		if (mMarkerTransparency != 0.0) {
			original = mG.getComposite();
			Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(1.0-mMarkerTransparency));
			mG.setComposite(composite);
			}

		if (mLabelHelper == null && showAnyLabels())
			mLabelHelper = new LabelHelper(baseBounds, baseGraphRect);
		boolean drawConnectionLinesInFocus = (mChartType == cChartTypeScatterPlot || mTreeNodeList != null) && drawConnectionLines();

		if (mConnectionColumn != cColumnUnassigned
		 && mRelativeMarkerSize == 0.0
		 && mLabelHelper == null) {
			// don't draw markers if we have connection lines and marker size is zero
			if (drawConnectionLinesInFocus)
				drawConnectionLines(true, true);
			}
		else {
			int focusFlagNo = getFocusFlag();
			int firstFocusIndex = 0;
			if (focusFlagNo != -1) {
				int index2 = mDataPoints-1;
				while (firstFocusIndex<index2) {
					if (mPoint[firstFocusIndex].record.isFlagSet(focusFlagNo)) {
						while (mPoint[index2].record.isFlagSet(focusFlagNo)
							&& index2 > firstFocusIndex)
							index2--;
						if (index2 == firstFocusIndex)
							break;
						VisualizationPoint temp = mPoint[firstFocusIndex];
						mPoint[firstFocusIndex] = mPoint[index2];
						mPoint[index2] = temp;
						}
					firstFocusIndex++;
					}
				}

			boolean isTreeView = isTreeViewGraph();
			boolean isDarkBackground = (ColorHelper.perceivedBrightness(getViewBackground()) <= 0.5);
			MultiValueBars mvbi = (mMultiValueMarkerMode == cMultiValueMarkerModeBars && mMultiValueMarkerColumns != null) ?
					new MultiValueBars() : null;

			boolean useSelectionColor = mFocusList != FocusableView.cFocusOnSelection
					&& mSplittingColumn[0] != CompoundTableListHandler.PSEUDO_COLUMN_SELECTION
					&& mSplittingColumn[1] != CompoundTableListHandler.PSEUDO_COLUMN_SELECTION;

			boolean isFilter = mUseAsFilterFlagNo != -1 && !mTableModel.isRowFlagSuspended(mUseAsFilterFlagNo);

			for (int i=0; i<mDataPoints; i++) {
				if (drawConnectionLinesInFocus && i == firstFocusIndex)
					drawConnectionLines(true, true);

				boolean drawLabels = false;
				if (isVisible(mPoint[i])
				 && (mChartType == cChartTypeScatterPlot
				  || mChartType == cChartTypeWhiskerPlot
				  || mPoint[i].chartGroupIndex == -1
				  || mTreeNodeList != null)) {
					VisualizationPoint vp = mPoint[i];
					vp.widthOrAngle1 = vp.heightOrAngle2 = (int)getMarkerSize(vp);
					boolean inFocus = (focusFlagNo == -1 || vp.record.isFlagSet(focusFlagNo));

					Color color = (isFilter && !vp.record.isFlagSet(mUseAsFilterFlagNo)) ? cUseAsFilterColor
							: (vp.record.isSelected() && useSelectionColor) ? VisualizationColor.cSelectedColor
							: mMarkerColor.getColorList()[vp.colorIndex];

					Color markerColor = inFocus ? color : VisualizationColor.lowContrastColor(color, getViewBackground());
					Color outlineColor = isDarkBackground ? markerColor.brighter() : markerColor.darker();

					drawLabels = mLabelHelper != null && mLabelHelper.hasLabels(vp);
					if (drawLabels) {
						mLabelHelper.prepareLabels(vp);
						mLabelHelper.drawLabelLines(vp, outlineColor);
						}

					if (vp.widthOrAngle1 != 0
					 && (mLabelColumn[MarkerLabelDisplayer.cMidCenter] == cColumnUnassigned || !drawLabels)) {
						if (mMultiValueMarkerMode != cMultiValueMarkerModeNone && mMultiValueMarkerColumns != null) {
							if (mMultiValueMarkerMode == cMultiValueMarkerModeBars)
								drawMultiValueBars(color, inFocus, isDarkBackground, vp.widthOrAngle1, mvbi, vp);
							else
								drawMultiValuePies(color, inFocus, isDarkBackground, vp.widthOrAngle1, vp);
							}
						else {
							int shape = (mMarkerShapeColumn != cColumnUnassigned) ? vp.shape : mIsFastRendering ? 1 : 0;
							drawMarker(markerColor, outlineColor, shape, vp.widthOrAngle1, vp.screenX, vp.screenY);
							}
						}

					if (drawLabels)
						drawMarkerLabels(mLabelHelper.getLabelInfo(), markerColor, outlineColor, isTreeView);
					}

				if (!drawLabels)
					mPoint[i].removeNonCustomLabelPositions();
				}
			}

		if (original != null)
			mG.setComposite(original);

		if (mChartType == cChartTypeScatterPlot && mTreeNodeList == null) {
			if (mCurveInfo != cCurveModeNone)
				drawCurves(baseGraphRect);

			if (mShownCorrelationType != CorrelationCalculator.TYPE_NONE)
				drawCorrelationCoefficient(baseGraphRect);
			}
		}

	private void drawMarkerLabels(MarkerLabelInfo[] labelInfo, Color markerColor, Color outlineColor, boolean isTreeView) {
		if (mMarkerLabelSize != 1.0)
			setFontHeightAndScaleToSplitView(mMarkerLabelSize * mFontHeight);

		boolean isDarkBackground = (ColorHelper.perceivedBrightness(mLabelBackground) <= 0.5);
		Color labelColor = mIsMarkerLabelsBlackAndWhite ? getContrastGrey(1f) : isDarkBackground ? markerColor : markerColor.darker();

		if (mLabelColumn[MarkerLabelDisplayer.cMidCenter] != cColumnUnassigned
				&& (!mLabelsInTreeViewOnly || isTreeView))
			drawMarkerLabel(labelInfo[MarkerLabelDisplayer.cMidCenter], labelColor, outlineColor);

		for (int i=0; i<labelInfo.length; i++)
			if (i != MarkerLabelDisplayer.cMidCenter
					&& mLabelColumn[i] != cColumnUnassigned
					&& (!mLabelsInTreeViewOnly || isTreeView))
				drawMarkerLabel(labelInfo[i], labelColor, outlineColor);

		if (mMarkerLabelSize != 1.0)
			setFontHeightAndScaleToSplitView(mFontHeight);
		}

	/**
	 * Most efficiently exact label dimensions and positions are calculated just before drawing the label.
	 * This is especially true for molecule labels, where the depictor is needed to determine labels bounds.
	 * However, in case of automatic label position optimization we need label bounds in advance.
	 * This method calculates/estimates label rectangles and creates a labelList for later position optimization.
	 *
	private void calculateAllMarkerLabels(Rectangle baseGraphRect, LabelPosition2D[][] labelPosition) {
		if (!showAnyLabels())
			return;

		mLabelHelper = new LabelHelper();
		MarkerLabelInfo mli = new MarkerLabelInfo();
		int labelFlagNo = getLabelFlag();
		TreeMap<byte[],VisualizationPoint> oneLabelPerCategoryMap = buildOnePerCategoryMap();
		boolean isTreeView = isTreeViewGraph();

		ArrayList<LabelPosition2D>[] lpList = null;
		if (labelPosition != null) {
			lpList = new ArrayList[mHVCount];
			for (int hv=0; hv<mHVCount; hv++)
				lpList[hv] = new ArrayList<>();
			}

		for (VisualizationPoint vp:mPoint) {
			if (isVisible(vp)
			 && (mChartType == cChartTypeScatterPlot
			  || mChartType == cChartTypeWhiskerPlot
			  || vp.chartGroupIndex == -1
			  || mTreeNodeList != null)) {
				if ((mLabelList == cLabelsOnAllRows || (labelFlagNo != -1 && vp.record.isFlagSet(labelFlagNo)))
				 && (oneLabelPerCategoryMap==null || vp==oneLabelPerCategoryMap.get(vp.record.getData(mOnePerCategoryLabelCategoryColumn)))) {
					for (int j = 0; j<mLabelColumn.length; j++) {
						if (mLabelColumn[j] != -1) {
							prepareMarkerLabelInfo(vp, j, isTreeView, mli);
							LabelPosition2D lp = calculateMarkerLabel(vp, j, baseGraphRect, mli);
							if (labelPosition != null && lp != null)
								lpList[vp.hvIndex].add(lp);
							}
						}
					}
				}
			}

		if (labelPosition != null)
			for (int hv=0; hv<mHVCount; hv++)
				labelPosition[hv] = lpList[hv].toArray(new LabelPosition2D[0]);
		}*/

	private void prepareMarkerLabelInfo(VisualizationPoint vp, int position, boolean isTreeView, MarkerLabelInfo mli) {
		int column = mLabelColumn[position];
		boolean isMolecule = mTableModel.isColumnTypeStructure(column);
		mli.fontSize = getLabelFontSize(vp, position, isTreeView);
		mli.border = mShowLabelBackground ? (int)(0.15f * mli.fontSize) : 0;
		mli.label = null;
		mli.depictor = null;
		if (isMolecule) {
			if (mLabelMolecule == null)
				mLabelMolecule = new StereoMolecule();
			StereoMolecule mol = mTableModel.getChemicalStructure(vp.record, column, CompoundTableModel.ATOM_COLOR_MODE_EXPLICIT, mLabelMolecule);
			if (mol != null) {
				float zoom = Float.isNaN(mMarkerSizeZoomAdaption) ? 1f : mMarkerSizeZoomAdaption;
				mli.depictor = new Depictor2D(mol, Depictor2D.cDModeSuppressChiralText);
				mli.depictor.validateView(mG, DEPICTOR_RECT,
						Depictor2D.cModeInflateToHighResAVBL + Math.max(1, (int)(256 * zoom * scaleIfSplitView(getLabelAVBL(vp, position, isTreeView)))));
				}
			}
		else {
			mli.label = mTableModel.getValue(vp.record, column);
			if (mli.label.length() == 0)
				mli.label = null;
			}
		}

	private void copyCoordsToMarkerLabelInfo(LabelPosition2D labelPosition, MarkerLabelInfo mli) {
		mli.x1 = labelPosition.getScreenX1();
		mli.y1 = labelPosition.getScreenY1();
		mli.x2 = labelPosition.getScreenX2();
		mli.y2 = labelPosition.getScreenY2();

		if (mli.depictor != null) {
			Rectangle2D.Double molRect = mli.depictor.getBoundingRect();
			mli.depictor.applyTransformation(new DepictorTransformation(1.0f,
					labelPosition.getScreenX1() + mli.border - molRect.x,
					labelPosition.getScreenY1() + mli.border - molRect.y));
			}
		else {
			mli.x = labelPosition.getScreenX1() + mli.border;
			mli.y = labelPosition.getScreenY1() + mli.border;
			}
		}

	/**
	 * Calculates the marker label specified by position considering vp.width and vp.height
	 * for exact label location. If position is midCenter and therefore replaces
	 * the original marker, then vp.width and vp.height are set to 0, such that findMarker()
	 * detects the label and not the marker anymore.
	 * @param vp
	 * @param position
	 * @param baseGraphRect
	 * @param mli prepared(!) MarkerLabelInfo
	 */
	private LabelPosition2D calculateMarkerLabel(VisualizationPoint vp, int position, Rectangle baseGraphRect, MarkerLabelInfo mli) {
		int x = Math.round(vp.screenX);
		int y = Math.round(vp.screenY);
		int w,h;

		// we have a label replacing the marker
		if (position == MarkerLabelDisplayer.cMidCenter) {
			vp.widthOrAngle1 = 0;
			vp.heightOrAngle2 = 0;
			}

		if (mli.depictor != null) {
			Rectangle2D.Double molRect = mli.depictor.getBoundingRect();
			w = (int)molRect.width;
			h = (int)molRect.height;
			}
		else if (mli.label != null) {
			setFontHeightAndScaleToSplitView(mli.fontSize);
			w = mG.getFontMetrics().stringWidth(mli.label);
			h = mG.getFontMetrics().getHeight();
			}
		else {
			return null;
			}

		LabelPosition2D labelPosition = vp.getOrCreateLabelPosition(mLabelColumn[position], false);
		if (labelPosition.isCustom()) {
			float dataX = labelPosition.getX();
			float dataY = labelPosition.getY();
			float relX = (mAxisVisMax[0] == mAxisVisMin[0]) ? 0.5f : (dataX - mAxisVisMin[0]) / (mAxisVisMax[0] - mAxisVisMin[0]);
			float relY = (mAxisVisMax[1] == mAxisVisMin[1]) ? 0.5f : (dataY - mAxisVisMin[1]) / (mAxisVisMax[1] - mAxisVisMin[1]);
			x = baseGraphRect.x + Math.round(relX * baseGraphRect.width);
			y = baseGraphRect.y + baseGraphRect.height - Math.round(relY * baseGraphRect.height);

			// when zooming show labels of visible markers that would be zoomed out of the view at the view edge
			if (x < baseGraphRect.x)
				x = baseGraphRect.x;
			else if (x > baseGraphRect.x + baseGraphRect.width)
				x = baseGraphRect.x + baseGraphRect.width;
			if (y < baseGraphRect.y)
				y = baseGraphRect.y;
			else if (y > baseGraphRect.y + baseGraphRect.height)
				y = baseGraphRect.x + baseGraphRect.height;

			x -= w/2;
			y -= h/2;

			if (mHVCount != 1) {
				x += mSplitter.getHIndex(vp.hvIndex) * mSplitter.getGridWidth();
				y += mSplitter.getVIndex(vp.hvIndex) * mSplitter.getGridHeight();
				}
			}
		else {
			switch (position) {
				case MarkerLabelDisplayer.cTopLeft:
					x -= vp.widthOrAngle1 /2 + w;
					y -= vp.heightOrAngle2 /2 + h;
					break;
				case MarkerLabelDisplayer.cTopCenter:
					x -= w/2;
					y -= vp.heightOrAngle2 /2 + h;
					break;
				case MarkerLabelDisplayer.cTopRight:
					x += vp.widthOrAngle1 /2;
					y -= vp.heightOrAngle2 /2 + h;
					break;
				case MarkerLabelDisplayer.cMidLeft:
					x -= vp.widthOrAngle1 *2/3 + w;
					y -= h/2;
					break;
				case MarkerLabelDisplayer.cMidCenter:
//					vp.widthOrAngle1 = w;
//					vp.heightOrAngle2 = h;
					x -= w/2;
					y -= h/2;
					break;
				case MarkerLabelDisplayer.cMidRight:
					x += vp.widthOrAngle1 *2/3;
					y -= h/2;
					break;
				case MarkerLabelDisplayer.cBottomLeft:
					x -= vp.widthOrAngle1 /2 + w;
					y += vp.heightOrAngle2 /2;
					break;
				case MarkerLabelDisplayer.cBottomCenter:
					x -= w/2;
					y += vp.heightOrAngle2 /2;
					break;
				case MarkerLabelDisplayer.cBottomRight:
					x += vp.widthOrAngle1 /2;
					y += vp.heightOrAngle2 /2;
					break;
				}
			}

		labelPosition.setScreenLocation(x - mli.border, y - mli.border, x + w + 2 * mli.border, y + h + 2 * mli.border);

		return labelPosition;
		}

	private void drawMarkerLabelLine(VisualizationPoint vp, MarkerLabelInfo mli, Color color) {
		if (mli.label != null || mli.depictor != null) {
			Point connectionPoint = getLabelConnectionPoint(vp.screenX, vp.screenY, mli.x1, mli.y1, mli.x2, mli.y2);
			if (connectionPoint != null) {
				mG.setColor(color);
				mG.setStroke(mNormalLineStroke);
				mG.drawLine(Math.round(vp.screenX), Math.round(vp.screenY), connectionPoint.x, connectionPoint.y);
				}
			}
		}

	private void drawMarkerLabel(MarkerLabelInfo mli, Color labelColor, Color outlineColor) {
		if (mli.label != null || mli.depictor != null) {
			if (mShowLabelBackground) {
				Composite original = null;
				if (mLabelBackgroundComposite != null) {
					original = mG.getComposite();
					mG.setComposite(mLabelBackgroundComposite);
					}

				mG.setColor(mLabelBackground);
				mG.fillRect(mli.x1, mli.y1, mli.x2-mli.x1, mli.y2-mli.y1);
				mG.setColor(outlineColor);
				mG.setStroke(mThinLineStroke);
				mG.drawRect(mli.x1, mli.y1, mli.x2-mli.x1, mli.y2-mli.y1);

				if (original != null)
					mG.setComposite(original);
				}

			// For custom located labels we may need to draw a line from marker to label edge

			if (mli.depictor != null) {
				mli.depictor.setOverruleColor(labelColor, getViewBackground());
				mli.depictor.paint(mG);
				}
			else {
				mG.setColor(labelColor);
				setFontHeightAndScaleToSplitView(mli.fontSize);
				mG.drawString(mli.label, mli.x, mli.y + mG.getFontMetrics().getAscent());
				}
			}
		}

	protected void drawMarker(Color color, int shape, int size, int x, int y) {
		drawMarker(color, getContrastGrey(MARKER_OUTLINE), shape, size, x, y);
		}

	private boolean renderFaster() {
		return mSkipPaintDetails;
		}

	private void drawMarker(Color color, Color outlineColor, int shape, float size, float x, float y) {
		float halfSize = size/2;
		float sx,sy;
		GeneralPath polygon;

		boolean drawOutline = mDrawMarkerOutline && !renderFaster() && size > OUTLINE_LIMIT;
		mG.setColor(size > OUTLINE_LIMIT ? color : outlineColor);
		mG.setStroke(mThinLineStroke);
		switch (shape) {
		case 0:
			mG.fill(new Ellipse2D.Float(x-halfSize, y-halfSize, size, size));
			if (drawOutline) {
				mG.setColor(outlineColor);
				mG.draw(new Ellipse2D.Float(x-halfSize, y-halfSize, size, size));
				}
			break;
		case 1:
			if (!mIsFastRendering) {
				mG.fill(new Rectangle2D.Float(x - halfSize, y - halfSize, size, size));
				if (drawOutline) {
					mG.setColor(outlineColor);
					mG.draw(new Rectangle2D.Float(x - halfSize, y - halfSize, size, size));
					}
				}
			else {
				int x1 = Math.round(x - halfSize);
				int y1 = Math.round(y - halfSize);
				int s = Math.round(size);
				mG.fillRect(x1, y1, s, s);
				if (drawOutline) {
					mG.setColor(outlineColor);
					mG.drawRect(x1, y1, s, s);
					}
				}
			break;
		case 2:
			polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 3);
			polygon.moveTo(x-halfSize, y+size/3);
			polygon.lineTo(x+halfSize, y+size/3);
			polygon.lineTo(x, y-2*size/3);
			polygon.closePath();
			mG.fill(polygon);
			if (drawOutline) {
				mG.setColor(outlineColor);
				mG.draw(polygon);
				}
			break;
		case 3:
			polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 4);
			polygon.moveTo(x-halfSize, y);
			polygon.lineTo(x, y+halfSize);
			polygon.lineTo(x+halfSize, y);
			polygon.lineTo(x, y-halfSize);
			polygon.closePath();
			mG.fill(polygon);
			if (drawOutline) {
				mG.setColor(outlineColor);
				mG.draw(polygon);
				}
			break;
		case 4:
			polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 3);
			polygon.moveTo(x-halfSize, y-size/3);
			polygon.lineTo(x+halfSize, y-size/3);
			polygon.lineTo(x, y+2*size/3);
			polygon.closePath();
			mG.fill(polygon);
			if (drawOutline) {
				mG.setColor(outlineColor);
				mG.draw(polygon);
			}
			break;
		case 5:
			sx = size/4;
			sy = sx+halfSize;
			mG.fill(new Rectangle2D.Float(x-sx, y-sy, 2*sx, 2*sy));
			if (drawOutline) {
				mG.setColor(outlineColor);
				mG.draw(new Rectangle2D.Float(x-sx, y-sy, 2*sx, 2*sy));
				}
			break;
		case 6:
			sy = size/4;
			sx = sy+halfSize;
			mG.fill(new Rectangle2D.Float(x-sx, y-sy, 2*sx, 2*sy));
			if (drawOutline) {
				mG.setColor(outlineColor);
				mG.draw(new Rectangle2D.Float(x-sx, y-sy, 2*sx, 2*sy));
				}
			break;
		default:
			float[] coords = cExtendedShapeCoords[shape - cSimpleShapeCount];
			polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, coords.length/2);
			polygon.moveTo(x+coords[0]*size, y+coords[1]*size);
			for (int i=2; i<coords.length; i+=2)
				polygon.lineTo(x+coords[i]*size, y+coords[i+1]*size);
			polygon.closePath();
			mG.fill(polygon);
			if (drawOutline) {
				mG.setColor(outlineColor);
				mG.draw(polygon);
				}
			break;
			}
		}

	private void drawMultiValueBars(Color color, boolean inFocus, boolean isDarkBackground, float size, MultiValueBars info, VisualizationPoint vp) {
		if (mMarkerColor.getColorColumn() == cColumnUnassigned
		 && color != VisualizationColor.cSelectedColor
		 && color != VisualizationColor.cUseAsFilterColor) {
			if (mMultiValueMarkerColor == null || mMultiValueMarkerColor.length != mMultiValueMarkerColumns.length)
				mMultiValueMarkerColor = VisualizationColor.createDiverseColorList(mMultiValueMarkerColumns.length);
			color = null;
			}

		info.calculate(size, vp);
		int x = info.firstBarX;
		mG.setColor(getContrastGrey(1f));
		mG.drawLine(x-info.barWidth/2, info.zeroY, x+mMultiValueMarkerColumns.length*info.barWidth+info.barWidth/2, info.zeroY);
		for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
			if (!Float.isNaN(info.relValue[i])) {
				Color barColor = (color != null) ? color : mMultiValueMarkerColor[i];
				Color fillColor = inFocus ? barColor : VisualizationColor.lowContrastColor(barColor, getViewBackground());
				mG.setColor(fillColor);
				mG.fillRect(x, info.barY[i], info.barWidth, info.barHeight[i]);
				}
			x += info.barWidth;
			}
		}

	private void drawMultiValuePies(Color color, boolean inFocus, boolean isDarkBackground, float size, VisualizationPoint vp) {
		if (mMarkerColor.getColorColumn() == cColumnUnassigned
		 && color != VisualizationColor.cSelectedColor
		 && color != VisualizationColor.cUseAsFilterColor) {
			if (mMultiValueMarkerColor == null || mMultiValueMarkerColor.length != mMultiValueMarkerColumns.length)
				mMultiValueMarkerColor = VisualizationColor.createDiverseColorList(mMultiValueMarkerColumns.length);
			color = null;
			}

		size *= 0.5f  * (float)Math.sqrt(Math.sqrt(mMultiValueMarkerColumns.length));
			// one sqrt because of area, 2nd sqrt to grow under-proportional with number of pie pieces

		float angleIncrement = 360f / mMultiValueMarkerColumns.length;
		float angle = 90f - angleIncrement;
		for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
			float r = size * getMarkerSizeVPFactor(vp.record.getDouble(mMultiValueMarkerColumns[i]), mMultiValueMarkerColumns[i]);
			Color piePieceColor = (color != null) ? color : mMultiValueMarkerColor[i];
			Color fillColor = inFocus ? piePieceColor : VisualizationColor.lowContrastColor(piePieceColor, getViewBackground());
			mG.setColor(fillColor);
			mG.fillArc(Math.round(vp.screenX-r), Math.round(vp.screenY-r), Math.round(2*r-1), Math.round(2*r-1), Math.round(angle), Math.round(angleIncrement));
			if (mDrawBarPieBoxOutline) {
				Color lineColor = isDarkBackground ? fillColor.brighter() : fillColor.darker();
				mG.setColor(lineColor);
				mG.drawArc(Math.round(vp.screenX - r), Math.round(vp.screenY - r), Math.round(2 * r - 1), Math.round(2 * r - 1), Math.round(angle), Math.round(angleIncrement));
				}
			angle -= angleIncrement;
			}
		}

	/**
	 * If no connection lines need to be drawn, then this method does nothing and returns false.
	 * Otherwise, if no focus is set, then this method draws all connection lines and returns false.
	 * Otherwise, this method draws those lines connecting markers, which are not in focus and
	 * returns true to indicate that connection line drawing is not completed yet. In this case
	 * drawConnectionLines(true, true) needs to be called after drawing those markers that are
	 * not in focus.
	 * @return true if drawConnectionLines(true, true) needs to be called later
	 */
	private boolean drawConnectionLines() {
		if (mPoint.length == 0)
			return false;

		if (mConnectionColumn == cColumnUnassigned
		 || mConnectionColumn == cConnectionColumnConnectCases)
			return false;

		if (!mIsHighResolution && mAbsoluteConnectionLineWidth < 0.5f)
			return false;

		String value = (mConnectionColumn < 0) ?
				null : mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferencedColumn);
		if (value == null)
			return drawCategoryConnectionLines();

		int referencedColumn = mTableModel.findColumn(value);
		if (referencedColumn != -1)
			return drawReferenceConnectionLines(referencedColumn);

		return false;
		}

	/**
	 * 
	 * @param considerFocus
	 * @param inFocus
	 */
	private void drawConnectionLines(boolean considerFocus, boolean inFocus) {
		String value = (mConnectionColumn < 0) ?
				null : mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferencedColumn);

		if (value == null)
			drawCategoryConnectionLines(considerFocus, inFocus);
		else
			drawReferenceConnectionLines(considerFocus, inFocus);
		}

	private boolean drawCategoryConnectionLines() {
		int connectionOrderColumn = (mConnectionOrderColumn == cColumnUnassigned) ?
										mAxisIndex[0] : mConnectionOrderColumn;
		if (connectionOrderColumn == cColumnUnassigned)
			return false;

		if (mConnectionLinePoint == null || mConnectionLinePoint.length != mPoint.length)
			mConnectionLinePoint = new VisualizationPoint[mPoint.length];
		for (int i=0; i<mPoint.length; i++)
			mConnectionLinePoint[i] = mPoint[i];

		Arrays.sort(mConnectionLinePoint, (p1, p2) -> compareConnectionLinePoints(p1, p2) );

		mConnectionFromIndex1 = 0;
		while (mConnectionFromIndex1<mConnectionLinePoint.length
			&& !isVisibleExcludeNaN(mConnectionLinePoint[mConnectionFromIndex1]))
			mConnectionFromIndex1++;

		if (mConnectionFromIndex1 == mConnectionLinePoint.length)
			return false;

		mConnectionFromIndex2 = getNextChangedConnectionLinePointIndex(mConnectionFromIndex1);
		if (mConnectionFromIndex2 == mConnectionLinePoint.length)
			return false;

		drawCategoryConnectionLines(mFocusList != FocusableView.cFocusNone, false);
		return (mFocusList != FocusableView.cFocusNone);
		}

	private void drawCategoryConnectionLines(boolean considerFocus, boolean inFocus) {
		long focusMask = (mFocusList == FocusableView.cFocusNone) ? 0
					   : (mFocusList == FocusableView.cFocusOnSelection) ? CompoundRecord.cFlagMaskSelected
					   : mTableModel.getListHandler().getListMask(mFocusList);

		int fromIndex1 = mConnectionFromIndex1;
		int fromIndex2 = mConnectionFromIndex2;

		mG.setStroke(mConnectionStroke);

		while (true) {
			int toIndex1 = fromIndex2;

			while (toIndex1<mConnectionLinePoint.length
				&& !isVisibleExcludeNaN(mConnectionLinePoint[toIndex1]))
				toIndex1++;

			if (toIndex1 == mConnectionLinePoint.length)
				return;

			int toIndex2 = getNextChangedConnectionLinePointIndex(toIndex1);

			if (isConnectionLinePossible(mConnectionLinePoint[fromIndex1], mConnectionLinePoint[toIndex1]))
				for (int i=fromIndex1; i<fromIndex2; i++)
					if (isVisibleExcludeNaN(mConnectionLinePoint[i]))
						for (int j=toIndex1; j<toIndex2; j++)
							if (isVisibleExcludeNaN(mConnectionLinePoint[j])
							 && (!considerFocus
							  || (inFocus
								^ (mConnectionLinePoint[j].record.getFlags() & focusMask) == 0)))
								drawConnectionLine(mConnectionLinePoint[i], mConnectionLinePoint[j], considerFocus && !inFocus, 0.0f, false);

			fromIndex1 = toIndex1;
			fromIndex2 = toIndex2;
			}
		}

	private boolean drawReferenceConnectionLines(int referencedColumn) {
		if (mConnectionLineMap == null)
			mConnectionLineMap = createReferenceMap(referencedColumn);

		drawReferenceConnectionLines(mFocusList != FocusableView.cFocusNone, false);

		return (mFocusList != FocusableView.cFocusNone);
		}

	private void drawReferenceConnectionLines(boolean considerFocus, boolean inFocus) {
		long focusMask = (mFocusList == FocusableView.cFocusNone) ? 0
					   : (mFocusList == FocusableView.cFocusOnSelection) ? CompoundRecord.cFlagMaskSelected
					   : mTableModel.getListHandler().getListMask(mFocusList);
		int strengthColumn = mTableModel.findColumn(mTableModel.getColumnProperty(mConnectionColumn,
				CompoundTableConstants.cColumnPropertyReferenceStrengthColumn));
		boolean isRedundant = CompoundTableConstants.cColumnPropertyReferenceTypeRedundant.equals(
				mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferenceType));

		mG.setStroke(mConnectionStroke);
		Composite original = (strengthColumn == -1) ? null : mG.getComposite();

		if (mTreeNodeList != null) {
			for (int layer=1; layer<mTreeNodeList.length; layer++) {
				for (VisualizationNode node:mTreeNodeList[layer]) {
					VisualizationPoint vp1 = node.getVisualizationPoint();
					VisualizationPoint vp2 = node.getParentNode().getVisualizationPoint();
					float strength = node.getStrength();
					if (isVisible(vp1)
					 && isVisible(vp2)
					 && (!considerFocus
					  || (inFocus
						^ (vp1.record.getFlags() & vp2.record.getFlags() & focusMask) == 0))) {
						if (strength > 0f)
							drawConnectionLine(vp1, vp2, considerFocus && !inFocus, 1f-strength, !isRedundant);
						}
					}
				}
			}
		else {
			float min = 0;
			float max = 0;
			float dif = 0;
			if (strengthColumn != -1) {
				min = mTableModel.getMinimumValue(strengthColumn);
				max = mTableModel.getMaximumValue(strengthColumn);
				if (max == min) {
					strengthColumn = -1;
					}
				else {
					min -= 0.2 * (max - min);
					dif = max - min;
					}
				}

			for (VisualizationPoint vp1:mPoint) {
				if (isVisible(vp1)) {
					byte[] data = (byte[])vp1.record.getData(mConnectionColumn);
					if (data != null) {
						String[] entry = mTableModel.separateEntries(new String(data));
	
						String[] strength = null;
						if (strengthColumn != -1) {
							byte[] strengthData = (byte[])vp1.record.getData(strengthColumn);
							if (strengthData != null) {
								strength = mTableModel.separateEntries(new String(strengthData));
								if (strength.length != entry.length)
									strength = null;
								}
							}
	
						int index = 0;
						for (String ref:entry) {
							VisualizationPoint vp2 = mConnectionLineMap.get(ref.getBytes());
							if (vp2 != null && isVisible(vp2)
							 && (!isRedundant || (vp1.record.getID() < vp2.record.getID()))
							 && (!considerFocus
							  || (inFocus
							   ^ (vp1.record.getFlags() & vp2.record.getFlags() & focusMask) == 0))) {
								float transparency = 0.0f;
								if (strength != null) {
									try {
										float value = Math.min(max, Math.max(min, mTableModel.tryParseEntry(strength[index++], strengthColumn)));
										transparency = Float.isNaN(value) ? 1.0f : (float)((max-value) / dif);
										}
									catch (NumberFormatException nfe) {}
									}
								if (transparency != 1.0f) {
									drawConnectionLine(vp1, vp2, considerFocus && !inFocus, transparency, !isRedundant);
									}
								}
							}
						}
					}
				}
			}

		if (original != null)
			mG.setComposite(original);
		}

	/**
	 * Draws a connection line between the given points. 
	 * If transparency is different from 0.0, then this method sets a Composite on mG. In this case the calling
	 * method needs to save and restore the old Composite before/after calling this method.
	 * If fast render mode is active, transparency is simulated by adapting the line color to the current background.
	 * @param p1
	 * @param p2
	 * @param outOfFocus
	 * @param transparency if 0.0 then the current composite is not touched and the line drawn with the current g2d transparency
	 */
	private void drawConnectionLine(VisualizationPoint p1, VisualizationPoint p2, boolean outOfFocus, float transparency, boolean showArrow) {
		if (isConnectionLineSuppressed(p1, p2))
			return;

		Color color = ColorHelper.intermediateColor(mMarkerColor.getColorList()[p1.colorIndex],
													mMarkerColor.getColorList()[p2.colorIndex], 0.5f);
		if (transparency != 0.0f) {
			if (!mIsFastRendering || mIsHighResolution)
				mG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f - transparency));
			else
				color = ColorHelper.intermediateColor(color, getViewBackground(), transparency);
			}
		if (outOfFocus)
			color = VisualizationColor.lowContrastColor(color, getViewBackground());
		mG.setColor(color);
		mG.draw(new Line2D.Float(p1.screenX, p1.screenY, p2.screenX, p2.screenY));
		if (showArrow) {
			float dx = p2.screenX - p1.screenX;
			float dy = p2.screenY - p1.screenY;
			float len = (float)Math.sqrt(dx*dx+dy*dy);
			float s = 2.5f * mAbsoluteConnectionLineWidth;
			if (len > 2*s) {
				float xm = p1.screenX + dx / 2;
				float ym = p1.screenY + dy / 2;
				float ndx = dx * s / len;
				float ndy = dy * s / len;

				if (mIsConnectionLineInverted) {
					GeneralPath polygon = new GeneralPath(GeneralPath.WIND_NON_ZERO, 3);
					polygon.moveTo(xm + ndx + ndy, ym + ndy - ndx);
					polygon.lineTo(Math.round(xm + ndx - ndy), ym + ndy + ndx);
					polygon.lineTo(Math.round(xm - ndx), ym - ndy);
					polygon.closePath();
					mG.fill(polygon);
					}
				else {
					GeneralPath polygon = new GeneralPath(GeneralPath.WIND_NON_ZERO, 3);
					polygon.moveTo(xm - ndx + ndy, ym - ndy - ndx);
					polygon.lineTo(Math.round(xm - ndx - ndy), ym - ndy + ndx);
					polygon.lineTo(Math.round(xm + ndx), ym + ndy);
					polygon.closePath();
					mG.fill(polygon);
					}
				}
			}
		}

	private void paintBarChart(Graphics2D g, Rectangle baseBounds, Rectangle baseGraphRect) {
		if (!mChartInfo.barOrPieDataAvailable)
			return;

		float axisRange = mChartInfo.axisMax - mChartInfo.axisMin;

		float cellWidth = (mChartInfo.barAxis == 1) ?
				(float)baseGraphRect.width / (float)getCategoryVisCount(0)
			  : (float)baseGraphRect.height / (float)getCategoryVisCount(1);
		float cellHeight = (mChartInfo.barAxis == 1) ?
				(float)baseGraphRect.height / (float)getCategoryVisCount(1)
			  : (float)baseGraphRect.width / (float)getCategoryVisCount(0);

		// if we need space for statistics labels, then reduce the area that we have for the bar
		float spacing = cellHeight * getBarChartEmptyLabelAreaSpacing();
		float labelSize = calculateStatisticsLabelSize(mChartInfo.barAxis == 0, spacing);

		mChartInfo.barWidth = mRelativeMarkerSize * Math.min(0.2f * cellHeight, (mCaseSeparationCategoryCount == 1) ?
				0.6f * cellWidth : 0.5f * cellWidth / mCaseSeparationCategoryCount);
		if (mChartInfo.barWidth < 1)
			mChartInfo.barWidth = 1;

		int focusFlagNo = getFocusFlag();
		int colorListLength = mMarkerColor.getColorList().length;
		int basicColorCount = colorListLength + 2;
		int colorCount = basicColorCount * ((focusFlagNo == -1) ? 1 : 2);
		int catCount = getCategoryVisCount(0)* getCategoryVisCount(1)*mCaseSeparationCategoryCount;

		float barBaseOffset = (mChartInfo.barBase - mChartInfo.axisMin) * cellHeight / axisRange;

		if (labelSize != 0f && isRightBarChart())
			barBaseOffset = cellHeight;

		if (mChartInfo.useProportionalFractions())
			mChartInfo.absValueFactor = new float[mHVCount][catCount];
		else
			mChartInfo.innerDistance = new float[mHVCount][catCount];

		float[][] barPosition = new float[mHVCount][catCount];
		float[][][] barColorEdge = new float[mHVCount][catCount][colorCount+1];
		float csWidth = (mChartInfo.barAxis == 1 ? cellWidth : -cellWidth)
					   * mCaseSeparationValue / mCaseSeparationCategoryCount;
		float csOffset = csWidth * (1 - mCaseSeparationCategoryCount) / 2.0f;
		for (int hv=0; hv<mHVCount; hv++) {
			int hOffset = 0;
			int vOffset = 0;
			if (isSplitView()) {
				hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
				vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
				}

			for (int i = 0; i<getCategoryVisCount(0); i++) {
				for (int j = 0; j<getCategoryVisCount(1); j++) {
					for (int k=0; k<mCaseSeparationCategoryCount; k++) {
						int cat = (i+j* getCategoryVisCount(0))*mCaseSeparationCategoryCount+k;
						if (mChartInfo.pointsInCategory[hv][cat] > 0) {
							float barHeight = cellHeight * Math.abs(mChartInfo.barValue[hv][cat] - mChartInfo.barBase) / axisRange;
							if (mChartInfo.useProportionalFractions())
								mChartInfo.absValueFactor[hv][cat] = (mChartInfo.absValueSum[hv][cat] == 0f) ? 0f : barHeight / mChartInfo.absValueSum[hv][cat];
							else
								mChartInfo.innerDistance[hv][cat] = barHeight / (float)mChartInfo.pointsInCategory[hv][cat];
							barPosition[hv][cat] = (mChartInfo.barAxis == 1) ?
									  baseGraphRect.x + hOffset + i*cellWidth + cellWidth/2
									: baseGraphRect.y + vOffset + baseGraphRect.height - j*cellWidth - cellWidth/2;
	
							if (mCaseSeparationCategoryCount != 1)
								barPosition[hv][cat] += csOffset + k*csWidth;

							// right bound bars have negative values and the barBase set to 0f (==axisMax).
							// Move them left by one bar length and extend them to the right (done by positive inner distance).
							float barOffset = isRightBarChart() || (isCenteredBarChart() && mChartInfo.barValue[hv][cat] < 0f) ?
									cellHeight * (mChartInfo.barValue[hv][cat] - mChartInfo.barBase) / axisRange : 0f;
							barColorEdge[hv][cat][0] = (mChartInfo.barAxis == 1) ?
									baseGraphRect.y + vOffset - barBaseOffset - barOffset + baseGraphRect.height - cellHeight * j
								  : baseGraphRect.x + hOffset + barBaseOffset + barOffset + cellHeight * i;

							if (mChartInfo.useProportionalFractions()) {
								for (int l=0; l<colorCount; l++) {
									float size = (mChartInfo.absValueSum[hv][cat] == 0f) ? 0f : mChartInfo.absColorValueSum[hv][cat][l]
											   * barHeight / mChartInfo.absValueSum[hv][cat];
									barColorEdge[hv][cat][l+1] = (mChartInfo.barAxis == 1) ?
											  barColorEdge[hv][cat][l] - size : barColorEdge[hv][cat][l] + size;
									}
								}
							else {  // calculate color edge position based on color category counts
								for (int l=0; l<colorCount; l++) {
									float size = barHeight * mChartInfo.pointsInColorCategory[hv][cat][l]
														   / mChartInfo.pointsInCategory[hv][cat];
									barColorEdge[hv][cat][l + 1] = (mChartInfo.barAxis == 1) ?
											barColorEdge[hv][cat][l] - size : barColorEdge[hv][cat][l] + size;
									}
								}
							}
						}
					}
				}
			}

		Composite original = null;
		if (mMarkerTransparency != 0.0) {
			original = g.getComposite();
			Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(1.0-mMarkerTransparency)); 
			g.setComposite(composite);
			}

		if (!mChartInfo.useProportionalWidths()) {
			for (int hv=0; hv<mHVCount; hv++) {
				for (int cat=0; cat<catCount; cat++) {
					for (int k=0; k<colorCount; k++) {
						if (mChartInfo.pointsInColorCategory[hv][cat][k] > 0) {
							float width = mChartInfo.barWidth;
							g.setColor(mChartInfo.color[k]);
							if (mChartInfo.barAxis == 1)
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
					if (mDrawBarPieBoxOutline && mChartInfo.pointsInCategory[hv][cat] > 0) {
						g.setColor(getContrastGrey(MARKER_OUTLINE));
						if (mChartInfo.barAxis == 1)
							g.drawRect(Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2),
	                                   Math.round(barColorEdge[hv][cat][colorCount]),
									   Math.round(mChartInfo.barWidth),
									   Math.round(barColorEdge[hv][cat][0])-Math.round(barColorEdge[hv][cat][colorCount]));
						else
							g.drawRect(Math.round(barColorEdge[hv][cat][0]),
	                                   Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2),
									   Math.round(barColorEdge[hv][cat][colorCount])-Math.round(barColorEdge[hv][cat][0]),
									   Math.round(mChartInfo.barWidth));
						}
					}
				}
			}

		if (getBarOrPieLabelCount() != 0) {
			boolean labelIsLeftOrBelow = isRightBarChart();
			String[] lineText = new String[5];
			int scaledFontHeight = Math.round(scaleIfSplitView(STATISTIC_LABEL_FONT_FACTOR * mFontHeight));
			g.setColor(getContrastGrey(SCALE_STRONG));
			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, scaledFontHeight)); // set font back to plain
			for (int hv=0; hv<mHVCount; hv++) {
				for (int cat=0; cat<catCount; cat++) {
					if (mChartInfo.pointsInCategory[hv][cat] > 0) {
						int lineCount = compileBarAndPieStatisticsLines(hv, cat, lineText);

						if (mChartInfo.barAxis == 1) {
							float x0 = barPosition[hv][cat];
							float y0 = labelIsLeftOrBelow ?
									   barColorEdge[hv][cat][0] + scaledFontHeight
									 : barColorEdge[hv][cat][colorCount] - ((float)lineCount - 0.7f) * scaledFontHeight;
							for (int line=0; line<lineCount; line++) {
								float x = x0 - g.getFontMetrics().stringWidth(lineText[line])/2;
								float y = y0 + line*scaledFontHeight;
								g.drawString(lineText[line], Math.round(x), Math.round(y));
								}
							}
						else {
							float x0 = labelIsLeftOrBelow ?
									   barColorEdge[hv][cat][0] - scaledFontHeight/2
									 : barColorEdge[hv][cat][colorCount] + scaledFontHeight/2;
							float y0 = barPosition[hv][cat] - ((lineCount-1)*scaledFontHeight)/2 + g.getFontMetrics().getAscent()/2;
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

		for (int i=0; i<mDataPoints; i++) {
			VisualizationPoint vp = mPoint[i];
			if (isVisibleInBarsOrPies(vp)) {
				int hv = vp.hvIndex;
				int cat = getChartCategoryIndex(vp);
				int colorIndex = getColorIndex(vp, colorListLength, focusFlagNo);

				float width = mChartInfo.barWidth;
				if (mChartInfo.useProportionalWidths())
					width *= mChartInfo.getBarWidthFactor(vp.record.getDouble(mMarkerSizeColumn));

				if (mChartInfo.useProportionalFractions()) {
					if (mChartInfo.barAxis == 1) {
						vp.screenX = barPosition[hv][cat];
						vp.widthOrAngle1 = width;
						float fractionHeight = Math.abs(vp.record.getDouble(mChartColumn))
								* mChartInfo.absValueFactor[hv][cat];
						vp.screenY = barColorEdge[hv][cat][colorIndex] - 0.5f * fractionHeight;
						barColorEdge[hv][cat][colorIndex] -= fractionHeight;
						vp.heightOrAngle2 = fractionHeight;
						}
					else {
						float fractionHeight = Math.abs(vp.record.getDouble(mChartColumn))
								* mChartInfo.absValueFactor[hv][cat];
						vp.screenX = barColorEdge[hv][cat][colorIndex] + 0.5f * fractionHeight;
						barColorEdge[hv][cat][colorIndex] += fractionHeight;
						vp.widthOrAngle1 = fractionHeight;
						vp.screenY = barPosition[hv][cat];
						vp.heightOrAngle2 = width;
						}
					}
				else {
					if (mChartInfo.barAxis == 1) {
						vp.screenX = barPosition[hv][cat];
						vp.widthOrAngle1 = width;
						vp.screenY = barColorEdge[hv][cat][0] - mChartInfo.innerDistance[hv][cat] * (0.5f + vp.chartGroupIndex);
						vp.heightOrAngle2 = mChartInfo.innerDistance[hv][cat];
						}
					else {
						vp.screenX = barColorEdge[hv][cat][0] + mChartInfo.innerDistance[hv][cat] * (0.5f + vp.chartGroupIndex);
						vp.widthOrAngle1 = mChartInfo.innerDistance[hv][cat];
						vp.screenY = barPosition[hv][cat];
						vp.heightOrAngle2 = width;
						}
					}

				if (mChartInfo.useProportionalWidths()) {
					g.setColor(mChartInfo.color[colorIndex]);
					g.fill(new Rectangle2D.Float(vp.screenX-vp.widthOrAngle1/2f, vp.screenY-vp.heightOrAngle2/2f, vp.widthOrAngle1, vp.heightOrAngle2));
					if (mDrawBarPieBoxOutline) {
						g.setColor(getContrastGrey(MARKER_OUTLINE));
						g.draw(new Rectangle2D.Float(vp.screenX-vp.widthOrAngle1/2f, vp.screenY-vp.heightOrAngle2/2f, vp.widthOrAngle1, vp.heightOrAngle2));
						}
					}
				}
			}

		if (original != null)
			g.setComposite(original);

		if (showAnyLabels()) {
			LabelHelper labelHelper = new LabelHelper(baseBounds, baseGraphRect);
			labelHelper.calculateLabels();

			if (mOptimizeLabelPositions)
				labelHelper.optimizeLabels();

			for (int i=0; i<mDataPoints; i++) {
				VisualizationPoint vp = mPoint[i];
				if (isVisibleInBarsOrPies(vp) && labelHelper.hasLabels(vp)) {
					Color color = mChartInfo.color[getColorIndex(vp, colorListLength, focusFlagNo)];
					labelHelper.prepareLabels(vp);
					labelHelper.drawLabelLines(vp, color);
					drawMarkerLabels(labelHelper.getLabelInfo(), color, color, false);
					}
				}
			}
		}

	private int getBarOrPieSizeValueDigits() {
		if (mChartMode == cChartModeCount)
			return 1 + (int)Math.log10(mTableModel.getTotalRowCount());
		if (mChartMode == cChartModePercent)
			return FLOAT_DIGITS;
		if (mTableModel.isColumnTypeInteger(mChartColumn) && !mTableModel.isLogarithmicViewMode(mChartColumn))
			return INT_DIGITS;

		return FLOAT_DIGITS;
		}

	private int compileBarAndPieStatisticsLines(int hv, int cat, String[] lineText) {
		boolean usesCounts = (mChartMode == cChartModeCount || mChartMode == cChartModePercent);
		boolean isLogarithmic = usesCounts ? false : mTableModel.isLogarithmicViewMode(mChartColumn);

		int lineCount = 0;
		if (isShowBarOrPieSizeValue()) {
			double value = mChartMode == cChartModeCount
						|| mChartMode == cChartModePercent
						|| !mTableModel.isLogarithmicViewMode(mChartColumn) ?
					mChartInfo.barValue[hv][cat] : Math.pow(10.0, mChartInfo.barValue[hv][cat]);
			lineText[lineCount++] = DoubleFormat.toString(value, getBarOrPieSizeValueDigits());
			}
		if (isShowMeanAndMedianValues()) {
			float meanValue = isLogarithmic ? (float) Math.pow(10, mChartInfo.mean[hv][cat]) : mChartInfo.mean[hv][cat];
			lineText[lineCount++] = "mean=" + DoubleFormat.toString(meanValue, FLOAT_DIGITS);
			}
		if (isShowStandardDeviation()) {
			if (Float.isInfinite(mChartInfo.stdDev[hv][cat])) {
				lineText[lineCount++] = "\u03C3=Infinity";
				}
			else {
				double stdDev = isLogarithmic ? Math.pow(10, mChartInfo.stdDev[hv][cat]) : mChartInfo.stdDev[hv][cat];
				lineText[lineCount++] = "\u03C3=".concat(DoubleFormat.toString(stdDev, FLOAT_DIGITS));
				}
			}
		if (isShowConfidenceInterval()) {
			if (Float.isInfinite(mChartInfo.errorMargin[hv][cat])) {
				lineText[lineCount++] = "CI95: Infinity";
				}
			else {
				double ll = mChartInfo.barValue[hv][cat] - mChartInfo.errorMargin[hv][cat];
				double hl = mChartInfo.barValue[hv][cat] + mChartInfo.errorMargin[hv][cat];
				if (isLogarithmic) {
					ll = Math.pow(10, ll);
					hl = Math.pow(10, hl);
					}
				lineText[lineCount++] = "CI95: ".concat(DoubleFormat.toString(ll, FLOAT_DIGITS)).concat("-").concat(DoubleFormat.toString(hl, FLOAT_DIGITS));
				}
			}
		if (isShowValueCount()) {
			lineText[lineCount++] = "N=" + mChartInfo.pointsInCategory[hv][cat];
			}
		return lineCount;
		}

	private void paintPieChart(Graphics2D g, Rectangle baseRect) {
		if (!mChartInfo.barOrPieDataAvailable)
			return;

		float cellHeight = baseRect.height / (float)getCategoryVisCount(1);
		float labelHeight = Math.min(cellHeight / 2f, calculateStatisticsLabelSize(false, 0f));  // we need the height of the label set
		float cellWidth = (float)baseRect.width / (float)getCategoryVisCount(0);
		float cellSize = Math.min(cellWidth, cellHeight);

		int focusFlagNo = getFocusFlag();
		int colorListLength = mMarkerColor.getColorList().length;
		int basicColorCount = mMarkerColor.getColorList().length + 2;
		int colorCount = basicColorCount * ((focusFlagNo == -1) ? 1 : 2);
		int catCount = mCaseSeparationCategoryCount* getCategoryVisCount(0)* getCategoryVisCount(1);

		mChartInfo.pieSize = new float[mHVCount][catCount];
		mChartInfo.pieX = new float[mHVCount][catCount];
		mChartInfo.pieY = new float[mHVCount][catCount];
		float[][][] pieColorEdge = new float[mHVCount][catCount][colorCount+1];
		int preferredCSAxis = (cellWidth > cellHeight) ? 0 : 1;
		float csWidth = (preferredCSAxis == 0 ? cellWidth : -cellHeight)
						* mCaseSeparationValue / mCaseSeparationCategoryCount;
		float csOffset = csWidth * (1 - mCaseSeparationCategoryCount) / 2.0f;
		for (int hv=0; hv<mHVCount; hv++) {
			int hOffset = 0;
			int vOffset = 0;
			if (isSplitView()) {
				hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
				vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
				}

			for (int i = 0; i<getCategoryVisCount(0); i++) {
				for (int j = 0; j<getCategoryVisCount(1); j++) {
					for (int k=0; k<mCaseSeparationCategoryCount; k++) {
						int cat = (i+j* getCategoryVisCount(0))*mCaseSeparationCategoryCount+k;
						if (mChartInfo.pointsInCategory[hv][cat] > 0) {
							float relSize = Math.abs(mChartInfo.barValue[hv][cat] - mChartInfo.barBase)
											 / (mChartInfo.axisMax - mChartInfo.axisMin);
							mChartInfo.pieSize[hv][cat] = cMaxPieSize * cellSize * mRelativeMarkerSize
													  * (float)Math.sqrt(relSize);
							mChartInfo.pieX[hv][cat] = baseRect.x + hOffset + i*cellWidth + cellWidth/2;
							mChartInfo.pieY[hv][cat] = baseRect.y + vOffset + baseRect.height
									- labelHeight / 2f - j*cellHeight - cellHeight/2;
	
							if (mCaseSeparationCategoryCount != 1) {
								if (preferredCSAxis == 0)
									mChartInfo.pieX[hv][cat] += csOffset + k*csWidth;
								else
									mChartInfo.pieY[hv][cat] += csOffset + k*csWidth;
								}

							if (mChartInfo.useProportionalFractions())
								for (int l=0; l<colorCount; l++)
									pieColorEdge[hv][cat][l+1] = pieColorEdge[hv][cat][l] + 360.0f
											* mChartInfo.absColorValueSum[hv][cat][l]
											/ mChartInfo.absValueSum[hv][cat];
							else
								for (int l=0; l<colorCount; l++)
									pieColorEdge[hv][cat][l+1] = pieColorEdge[hv][cat][l] + 360.0f
											  * (float)mChartInfo.pointsInColorCategory[hv][cat][l]
											  / (float)mChartInfo.pointsInCategory[hv][cat];
							}
						}
					}
				}
			}

		Composite original = null;
		if (mMarkerTransparency != 0.0) {
			original = mG.getComposite();
			Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(1.0-mMarkerTransparency)); 
			mG.setComposite(composite);
			}

		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (mChartInfo.pointsInCategory[hv][cat] > 0) {
					int r = Math.round(mChartInfo.pieSize[hv][cat]/2);
					int x = Math.round(mChartInfo.pieX[hv][cat]);
					int y = Math.round(mChartInfo.pieY[hv][cat]);
					if (mChartInfo.pointsInCategory[hv][cat] == 1) {
						for (int k=0; k<colorCount; k++) {
							if (mChartInfo.pointsInColorCategory[hv][cat][k] > 0) {
								g.setColor(mChartInfo.color[k]);
								break;
								}
							}
						g.fillOval(x-r, y-r, 2*r, 2*r);
						}
					else {
						for (int k=0; k<colorCount; k++) {
							if (mChartInfo.pointsInColorCategory[hv][cat][k] > 0) {
								g.setColor(mChartInfo.color[k]);
								g.fillArc(x-r, y-r, 2*r, 2*r,
										  Math.round(pieColorEdge[hv][cat][k]),
										  Math.round(pieColorEdge[hv][cat][k+1])-Math.round(pieColorEdge[hv][cat][k]));
								}
							}
						}
					if (mDrawBarPieBoxOutline) {
						g.setColor(getContrastGrey(MARKER_OUTLINE));
						g.drawOval(x - r, y - r, 2 * r, 2 * r);
						}
					}
				}
			}

		if (getBarOrPieLabelCount() != 0) {
			String[] lineText = new String[getBarOrPieLabelCount()];
			int scaledFontHeight = Math.round(scaleIfSplitView(STATISTIC_LABEL_FONT_FACTOR * mFontHeight));
			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, scaledFontHeight));
			g.setColor(getContrastGrey(SCALE_STRONG));
			for (int hv=0; hv<mHVCount; hv++) {
				for (int cat=0; cat<catCount; cat++) {
					if (mChartInfo.pointsInCategory[hv][cat] > 0) {
						int lineCount = compileBarAndPieStatisticsLines(hv, cat, lineText);

						float r = mChartInfo.pieSize[hv][cat]/2;
						float x0 = mChartInfo.pieX[hv][cat];
						float y0 = mChartInfo.pieY[hv][cat] + r + mG.getFontMetrics().getAscent() + scaledFontHeight / 4f;

						for (int line=0; line<lineCount; line++) {
							float x = Math.round(x0 - mG.getFontMetrics().stringWidth(lineText[line])/2);
							float y = y0 + line * Math.round(scaledFontHeight);
							mG.drawString(lineText[line], Math.round(x), Math.round(y));
							}
						}
					}
				}
			}

		if (original != null)
			mG.setComposite(original);

		// calculate coordinates for selection
		for (int i=0; i<mDataPoints; i++) {
			VisualizationPoint vp = mPoint[i];
			if (isVisibleInBarsOrPies(vp)) {
				int hv = vp.hvIndex;
				int cat = getChartCategoryIndex(vp);
				float angle = 0f;
				if (mChartInfo.useProportionalFractions()) {
					int colorIndex = getColorIndex(vp, colorListLength, focusFlagNo);
					float fractionAngle = 360f * Math.abs(vp.record.getDouble(mChartColumn))
										/ mChartInfo.absValueSum[hv][cat];
					vp.widthOrAngle1 = pieColorEdge[hv][cat][colorIndex];
					angle = (pieColorEdge[hv][cat][colorIndex] + 0.5f * fractionAngle) * (float)Math.PI / 180f;
					pieColorEdge[hv][cat][colorIndex] += fractionAngle;
					vp.heightOrAngle2 = pieColorEdge[hv][cat][colorIndex];
					}
				else {
					angle = (0.5f + vp.chartGroupIndex) * 2.0f * (float)Math.PI / mChartInfo.pointsInCategory[hv][cat];
					}
				vp.screenX = Math.round(mChartInfo.pieX[hv][cat]+mChartInfo.pieSize[hv][cat]/2.0f*(float)Math.cos(angle));
				vp.screenY = Math.round(mChartInfo.pieY[hv][cat]-mChartInfo.pieSize[hv][cat]/2.0f*(float)Math.sin(angle));
				}
			}
		}

	private void paintBoxOrWhiskerPlot(Graphics2D g, Rectangle baseRect) {
		BoxPlotViewInfo boxPlotInfo = (BoxPlotViewInfo)mChartInfo;

		float cellWidth = (boxPlotInfo.barAxis == 1) ?
				(float)baseRect.width / (float)getCategoryVisCount(0)
			  : (float)baseRect.height / (float)getCategoryVisCount(1);
		float cellHeight = (boxPlotInfo.barAxis == 1) ?
				(float)baseRect.height
			  : (float)baseRect.width;
		float valueRange = (boxPlotInfo.barAxis == 1) ?
				mAxisVisMax[1]-mAxisVisMin[1]
			  : mAxisVisMax[0]-mAxisVisMin[0];

		mChartInfo.barWidth = Math.min(0.2f * cellHeight, 0.5f * cellWidth / mCaseSeparationCategoryCount);

		int focusFlagNo = getFocusFlag();
		int basicColorCount = mMarkerColor.getColorList().length + 2;
		int colorCount = basicColorCount * ((focusFlagNo == -1) ? 1 : 2);
		int axisCatCount = getCategoryVisCount(boxPlotInfo.barAxis == 1 ? 0 : 1);
		int catCount = axisCatCount * mCaseSeparationCategoryCount;

		boxPlotInfo.innerDistance = new float[mHVCount][catCount];
		float[][] barPosition = new float[mHVCount][catCount];
		float[][][] barColorEdge = new float[mHVCount][catCount][colorCount+1];
		float[][] boxLAV = new float[mHVCount][catCount];
		float[][] boxUAV = new float[mHVCount][catCount];
		float[][] mean = new float[mHVCount][catCount];
		float[][] median = new float[mHVCount][catCount];
		float csWidth = (mChartInfo.barAxis == 1 ? cellWidth : -cellWidth)
					   * mCaseSeparationValue / mCaseSeparationCategoryCount;
		float csOffset = csWidth * (1 - mCaseSeparationCategoryCount) / 2.0f;
		for (int hv=0; hv<mHVCount; hv++) {
			int hOffset = 0;
			int vOffset = 0;
			if (isSplitView()) {
				hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
				vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
				}

			for (int i=0; i<axisCatCount; i++) {
				for (int j=0; j<mCaseSeparationCategoryCount; j++) {
					int cat = i*mCaseSeparationCategoryCount + j;
					if (boxPlotInfo.pointsInCategory[hv][cat] != 0) {
						boxPlotInfo.innerDistance[hv][cat] = (boxPlotInfo.boxQ3[hv][cat] - boxPlotInfo.boxQ1[hv][cat])
														  * cellHeight / valueRange / (float)boxPlotInfo.pointsInCategory[hv][cat];
	
						int offset = 0;
						float visMin = 0;
						float factor = 0;
						float innerDistance = boxPlotInfo.innerDistance[hv][cat];
						if (boxPlotInfo.barAxis == 1) {
							barPosition[hv][cat] = baseRect.x + hOffset + i*cellWidth + cellWidth/2;
	
							offset = baseRect.y + vOffset + baseRect.height;
							visMin = mAxisVisMin[1];
							factor =  - (float)baseRect.height / valueRange;
							innerDistance = -innerDistance;
							}
						else {
							barPosition[hv][cat] = baseRect.y + vOffset + baseRect.height - i*cellWidth - cellWidth/2;
	
							offset = baseRect.x + hOffset;
							visMin = mAxisVisMin[0];
							factor =  (float)baseRect.width / valueRange;
							}

						if (mCaseSeparationCategoryCount != 1)
							barPosition[hv][cat] += csOffset + j*csWidth;

						barColorEdge[hv][cat][0] = offset + factor * (boxPlotInfo.boxQ1[hv][cat] - visMin);
	
						for (int k=0; k<colorCount; k++)
							barColorEdge[hv][cat][k+1] = barColorEdge[hv][cat][k] + innerDistance
													   * (float)boxPlotInfo.pointsInColorCategory[hv][cat][k];

	
						boxLAV[hv][cat] = offset + factor * (boxPlotInfo.boxLAV[hv][cat] - visMin);
						boxUAV[hv][cat] = offset + factor * (boxPlotInfo.boxUAV[hv][cat] - visMin);
						mean[hv][cat] = offset + factor * (boxPlotInfo.mean[hv][cat] - visMin);
						median[hv][cat] = offset + factor * (boxPlotInfo.median[hv][cat] - visMin);
						}
					}
				}
			}

		// draw connection lines
		if (mConnectionColumn == cConnectionColumnConnectCases
		 || mConnectionColumn == mAxisIndex[1-boxPlotInfo.barAxis]) {
			g.setStroke(mConnectionStroke);
			g.setColor(mBoxplotMeanMode == cBoxplotMeanModeMean ? Color.RED.darker() : getContrastGrey(SCALE_STRONG));
			for (int hv=0; hv<mHVCount; hv++) {
				for (int j=0; j<mCaseSeparationCategoryCount; j++) {
					int oldX = Integer.MAX_VALUE;
					int oldY = Integer.MAX_VALUE;
					if (mCaseSeparationCategoryCount != 1
					 && mMarkerColor.getColorColumn() == mCaseSeparationColumn) {
						if (mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) {
							g.setColor(mMarkerColor.getColor(j));
							}
						else {
							for (int k=0; k<colorCount; k++) {
								if (boxPlotInfo.pointsInColorCategory[hv][j][k] != 0) {
									g.setColor(boxPlotInfo.color[k]);
									break;
									}
								}
							}
						}

					for (int i=0; i<axisCatCount; i++) {
						int cat = i*mCaseSeparationCategoryCount + j;
				
						if (boxPlotInfo.pointsInCategory[hv][cat] > 0) {
							int value = Math.round(mBoxplotMeanMode == cBoxplotMeanModeMean ? mean[hv][cat] : median[hv][cat]);
							int newX = Math.round(boxPlotInfo.barAxis == 1 ? barPosition[hv][cat] : value);
							int newY = Math.round(boxPlotInfo.barAxis == 1 ? value : barPosition[hv][cat]);
							if (oldX != Integer.MAX_VALUE) {
								g.drawLine(oldX, oldY, newX, newY);
								}
							oldX = newX;
							oldY = newY;
							}
						}
					}
				}
			}
		else if (mCaseSeparationCategoryCount != 1 && mConnectionColumn == mCaseSeparationColumn) {
			g.setStroke(mConnectionStroke);
			g.setColor(mBoxplotMeanMode == cBoxplotMeanModeMean ? Color.RED.darker() : getContrastGrey(SCALE_STRONG));
			for (int hv=0; hv<mHVCount; hv++) {
				for (int i=0; i<axisCatCount; i++) {
					int oldX = Integer.MAX_VALUE;
					int oldY = Integer.MAX_VALUE;
					if (mMarkerColor.getColorColumn() == mAxisIndex[1-boxPlotInfo.barAxis]) {
						if (mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) {
							g.setColor(mMarkerColor.getColor(i));
							}
						else {
							for (int k=0; k<colorCount; k++) {
								if (boxPlotInfo.pointsInColorCategory[hv][i*mCaseSeparationCategoryCount][k] != 0) {
									g.setColor(boxPlotInfo.color[k]);
									break;
									}
								}
							}
						}

					for (int j=0; j<mCaseSeparationCategoryCount; j++) {
						int cat = i*mCaseSeparationCategoryCount + j;
				
						if (boxPlotInfo.pointsInCategory[hv][cat] > 0) {
							int value = Math.round(mBoxplotMeanMode == cBoxplotMeanModeMean ? mean[hv][cat] : median[hv][cat]);
							int newX = Math.round(boxPlotInfo.barAxis == 1 ? barPosition[hv][cat] : value);
							int newY = Math.round(boxPlotInfo.barAxis == 1 ? value : barPosition[hv][cat]);
							if (oldX != Integer.MAX_VALUE) {
								g.drawLine(oldX, oldY, newX, newY);
								}
							oldX = newX;
							oldY = newY;
							}
						}
					}
				}
			}

		Composite original = null;
		Composite composite = null;
		if (mChartType == cChartTypeBoxPlot && mMarkerTransparency != 0.0) {
			original = mG.getComposite();
			composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(1.0-mMarkerTransparency)); 
			}

		float lineLengthAV = mChartInfo.barWidth / 3;

		float lineWidth = Math.min(scaleIfSplitView(mFontHeight)/8f, mChartInfo.barWidth/8f);

		Stroke lineStroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		Stroke dashedStroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
										   lineWidth, new float[] {3*lineWidth}, 0f);

		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (boxPlotInfo.pointsInCategory[hv][cat] > 0) {
					if (mChartType == cChartTypeBoxPlot) {
						if (composite != null)
							mG.setComposite(composite);
	
						for (int k=0; k<colorCount; k++) {
							if (boxPlotInfo.pointsInColorCategory[hv][cat][k] > 0) {
								g.setColor(boxPlotInfo.color[k]);
								if (boxPlotInfo.barAxis == 1)
									g.fillRect(Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2),
				  							   Math.round(barColorEdge[hv][cat][k+1]),
											   Math.round(mChartInfo.barWidth),
											   Math.round(barColorEdge[hv][cat][k])-Math.round(barColorEdge[hv][cat][k+1]));
								else
									g.fillRect(Math.round(barColorEdge[hv][cat][k]),
				  							   Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2),
											   Math.round(barColorEdge[hv][cat][k+1]-Math.round(barColorEdge[hv][cat][k])),
											   Math.round(mChartInfo.barWidth));
								}
							}
						if (original != null)
							mG.setComposite(original);
						}

					// If we show no markers in a whisker plot, and if every whisker belongs to one category
					// of that column that is assigned for marker coloring, then we draw the whisker itself
					// in the color assigned to that category.
					if (mChartType == cChartTypeWhiskerPlot
				   	 && mRelativeMarkerSize == 0.0) {
						if (mMarkerColor.getColorColumn() == mAxisIndex[boxPlotInfo.barAxis == 1 ? 0 : 1])
							g.setColor(mMarkerColor.getColor(cat / mCaseSeparationCategoryCount));
						else if (mCaseSeparationCategoryCount != 1
							  && mMarkerColor.getColorColumn() == mCaseSeparationColumn)
							g.setColor(mMarkerColor.getColor(cat % mCaseSeparationCategoryCount));
						else
							g.setColor(getContrastGrey(SCALE_STRONG));
						}
					else {
						g.setColor(getContrastGrey(SCALE_STRONG));
						}

					if (boxPlotInfo.barAxis == 1) {
						mG.setStroke(lineStroke);
						if (mDrawBarPieBoxOutline && mChartType == cChartTypeBoxPlot) {
							g.drawRect(Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2),
	  								   Math.round(barColorEdge[hv][cat][colorCount]),
									   Math.round(mChartInfo.barWidth),
									   Math.round(barColorEdge[hv][cat][0])-Math.round(barColorEdge[hv][cat][colorCount]));
							}
						if (boxLAV[hv][cat] > barColorEdge[hv][cat][0])
							g.drawLine(Math.round(barPosition[hv][cat]-lineLengthAV),
									   Math.round(boxLAV[hv][cat]),
									   Math.round(barPosition[hv][cat]+lineLengthAV),
									   Math.round(boxLAV[hv][cat]));
						if (boxUAV[hv][cat] < barColorEdge[hv][cat][colorCount])
							g.drawLine(Math.round(barPosition[hv][cat]-lineLengthAV),
									   Math.round(boxUAV[hv][cat]),
									   Math.round(barPosition[hv][cat]+lineLengthAV),
									   Math.round(boxUAV[hv][cat]));

						mG.setStroke(dashedStroke);
						if (mChartType == cChartTypeWhiskerPlot) {
							g.drawLine(Math.round(barPosition[hv][cat]),
									   Math.round(boxUAV[hv][cat]),
									   Math.round(barPosition[hv][cat]),
									   Math.round(boxLAV[hv][cat]));
							}
						else {
							if (boxLAV[hv][cat] > barColorEdge[hv][cat][0])
								g.drawLine(Math.round(barPosition[hv][cat]),
										   Math.round(boxLAV[hv][cat]),
										   Math.round(barPosition[hv][cat]),
										   Math.round(barColorEdge[hv][cat][0]));
							if (boxUAV[hv][cat] < barColorEdge[hv][cat][colorCount])
								g.drawLine(Math.round(barPosition[hv][cat]),
										   Math.round(boxUAV[hv][cat]),
										   Math.round(barPosition[hv][cat]),
										   Math.round(barColorEdge[hv][cat][colorCount]));
							}
						}
					else {
						mG.setStroke(lineStroke);
						if (mDrawBarPieBoxOutline && mChartType == cChartTypeBoxPlot) {
							g.drawRect(Math.round(barColorEdge[hv][cat][0]),
	  								   Math.round(barPosition[hv][cat]-mChartInfo.barWidth/2),
									   Math.round(barColorEdge[hv][cat][colorCount])-Math.round(barColorEdge[hv][cat][0]),
									   Math.round(mChartInfo.barWidth));
							}
						mG.setStroke(lineStroke);
						g.drawLine(Math.round(boxLAV[hv][cat]),
								   Math.round(barPosition[hv][cat]-lineLengthAV),
								   Math.round(boxLAV[hv][cat]),
								   Math.round(barPosition[hv][cat]+lineLengthAV));
						g.drawLine(Math.round(boxUAV[hv][cat]),
								   Math.round(barPosition[hv][cat]-lineLengthAV),
								   Math.round(boxUAV[hv][cat]),
								   Math.round(barPosition[hv][cat]+lineLengthAV));

						mG.setStroke(dashedStroke);
						if (mChartType == cChartTypeWhiskerPlot) {
							g.drawLine(Math.round(boxLAV[hv][cat]),
									   Math.round(barPosition[hv][cat]),
									   Math.round(boxUAV[hv][cat]),
									   Math.round(barPosition[hv][cat]));
							}
						else {
							mG.setStroke(dashedStroke);
							g.drawLine(Math.round(boxLAV[hv][cat]),
									   Math.round(barPosition[hv][cat]),
									   Math.round(barColorEdge[hv][cat][0]),
									   Math.round(barPosition[hv][cat]));
							g.drawLine(Math.round(boxUAV[hv][cat]),
									   Math.round(barPosition[hv][cat]),
									   Math.round(barColorEdge[hv][cat][colorCount]),
									   Math.round(barPosition[hv][cat]));
							}
						}

					mG.setStroke(lineStroke);
					drawBoxMeanIndicators(g, median[hv][cat], mean[hv][cat], barPosition[hv][cat], 2*lineWidth);
					}
				}
			}

		if (isShowMeanAndMedianValues()
		 || isShowStandardDeviation()
		 || isShowConfidenceInterval()
		 || isShowValueCount()
		 || boxPlotInfo.foldChange != null
		 || boxPlotInfo.pValue != null) {
			String[] lineText = new String[7];
			g.setColor(getContrastGrey(SCALE_STRONG));
			int scaledFontHeight = Math.round(scaleIfSplitView(mFontHeight));
			boolean isLogarithmic = mTableModel.isLogarithmicViewMode(mAxisIndex[boxPlotInfo.barAxis]);
			for (int hv=0; hv<mHVCount; hv++) {
				int hOffset = 0;
				int vOffset = 0;
				if (isSplitView()) {
					hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
					vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
					}
				for (int cat=0; cat<catCount; cat++) {
					if (boxPlotInfo.pointsInCategory[hv][cat] > 0) {
						int lineCount = 0;
						if (isShowMeanAndMedianValues()) {
							int digits = mTableModel.isColumnTypeInteger(mAxisIndex[boxPlotInfo.barAxis]) ? INT_DIGITS : FLOAT_DIGITS;
							float meanValue = isLogarithmic ? (float)Math.pow(10, boxPlotInfo.mean[hv][cat]) : boxPlotInfo.mean[hv][cat];
							float medianValue = isLogarithmic ? (float)Math.pow(10, boxPlotInfo.median[hv][cat]) : boxPlotInfo.median[hv][cat];
							switch (mBoxplotMeanMode) {
							case cBoxplotMeanModeMedian:
								lineText[lineCount++] = "median="+DoubleFormat.toString(medianValue, digits);
								break;
							case cBoxplotMeanModeMean:
								lineText[lineCount++] = "mean="+DoubleFormat.toString(meanValue, digits);
								break;
							case cBoxplotMeanModeLines:
							case cBoxplotMeanModeTriangles:
								lineText[lineCount++] = "mean="+DoubleFormat.toString(meanValue, digits);
								lineText[lineCount++] = "median="+DoubleFormat.toString(medianValue, digits);
								break;
								}
							}
						if (isShowStandardDeviation()) {
							if (Float.isInfinite(boxPlotInfo.stdDev[hv][cat])) {
								lineText[lineCount++] = "\u03C3=Infinity";
								}
							else {
								double stdDev = isLogarithmic ? Math.pow(10, boxPlotInfo.stdDev[hv][cat]) : boxPlotInfo.stdDev[hv][cat];
								lineText[lineCount++] = "\u03C3=" + DoubleFormat.toString(stdDev, FLOAT_DIGITS);
								}
							}
						if (isShowConfidenceInterval()) {
							if (Float.isInfinite(boxPlotInfo.errorMargin[hv][cat])) {
								lineText[lineCount++] = "CI95: Infinity";
								}
							else {
								double ll = boxPlotInfo.mean[hv][cat] - boxPlotInfo.errorMargin[hv][cat];
								double hl = boxPlotInfo.mean[hv][cat] + boxPlotInfo.errorMargin[hv][cat];
								if (isLogarithmic) {
									ll = Math.pow(10, ll);
									hl = Math.pow(10, hl);
									}
								lineText[lineCount++] = "CI95: ".concat(DoubleFormat.toString(ll, FLOAT_DIGITS)).concat("-").concat(DoubleFormat.toString(hl, FLOAT_DIGITS));
								}
							}
						if (isShowValueCount()) {
							int outliers = (boxPlotInfo.outlierCount == null) ? 0 : boxPlotInfo.outlierCount[hv][cat];
							lineText[lineCount++] = "N="+(boxPlotInfo.pointsInCategory[hv][cat]+outliers);
							}
						if (boxPlotInfo.foldChange != null && !Float.isNaN(boxPlotInfo.foldChange[hv][cat])) {
							String label = isLogarithmic ? "log2fc=" : "fc=";
							lineText[lineCount++] = label+new DecimalFormat("#.###").format(boxPlotInfo.foldChange[hv][cat]);
							}
						if (boxPlotInfo.pValue != null && !Float.isNaN(boxPlotInfo.pValue[hv][cat])) {
							lineText[lineCount++] = "p="+new DecimalFormat("#.####").format(boxPlotInfo.pValue[hv][cat]);
							}

						// calculate the needed space of the text area incl. border of scaledFontHeight/2
						int textWidth = 0;
						int textHeight = (1+lineCount) * scaledFontHeight;
						for (int line=0; line<lineCount; line++) {
							int textLineWidth = mG.getFontMetrics().stringWidth(lineText[line]);
							if (textWidth < textLineWidth)
								textWidth = textLineWidth;
							}
						textWidth += scaledFontHeight;

						for (int line=0; line<lineCount; line++) {
							int textLineWidth = mG.getFontMetrics().stringWidth(lineText[line]);
							float x,y;
							if (boxPlotInfo.barAxis == 1) {
								x = barPosition[hv][cat] - textLineWidth/2;
								if (baseRect.y+baseRect.height - boxLAV[hv][cat] < textHeight
								 && boxUAV[hv][cat] - baseRect.y > textHeight)
									y = boxUAV[hv][cat]-textHeight+scaledFontHeight*3/2+line*scaledFontHeight;
								else
									y = Math.min(boxLAV[hv][cat]+scaledFontHeight*3/2, baseRect.y+baseRect.height+vOffset+scaledFontHeight/2-lineCount*scaledFontHeight)+line*scaledFontHeight;
								}
							else {
								if (baseRect.x+baseRect.width - boxUAV[hv][cat] < textWidth
								 && boxLAV[hv][cat] - baseRect.x > textWidth)
									x = boxLAV[hv][cat] - textLineWidth - scaledFontHeight/2;
								else
									x = Math.min(boxUAV[hv][cat]+scaledFontHeight/2, baseRect.x+baseRect.width+hOffset-textLineWidth);
								y = barPosition[hv][cat]+mG.getFontMetrics().getAscent()/2-((lineCount-1)*scaledFontHeight)/2+line*scaledFontHeight;
								}
							mG.drawString(lineText[line], Math.round(x), Math.round(y));
							}
						}
					}
				}
			}

		if (original != null)
			mG.setComposite(original);

		// in case of box-plot calculate screen positions of all non-outliers
		if (mChartType != cChartTypeWhiskerPlot) {
			for (int i=0; i<mDataPoints; i++) {
				if (isVisibleExcludeNaN(mPoint[i])) {
					int chartGroupIndex = mPoint[i].chartGroupIndex;
					if (chartGroupIndex != -1) {
						int hv = mPoint[i].hvIndex;
						int cat = getChartCategoryIndex(mPoint[i]);
						if (boxPlotInfo.barAxis == 1) {
							mPoint[i].screenX = barPosition[hv][cat];
							mPoint[i].screenY = barColorEdge[hv][cat][0]-boxPlotInfo.innerDistance[hv][cat]*(1+chartGroupIndex)
											  + boxPlotInfo.innerDistance[hv][cat]/2;
							mPoint[i].widthOrAngle1 = boxPlotInfo.barWidth;
							mPoint[i].heightOrAngle2 = boxPlotInfo.innerDistance[hv][cat];
							}
						else {
							mPoint[i].screenX = barColorEdge[hv][cat][0]+boxPlotInfo.innerDistance[hv][cat]*chartGroupIndex
											  + boxPlotInfo.innerDistance[hv][cat]/2;
							mPoint[i].screenY = barPosition[hv][cat]-mChartInfo.barWidth/2+mChartInfo.barWidth/2;
							mPoint[i].widthOrAngle1 = boxPlotInfo.innerDistance[hv][cat];
							mPoint[i].heightOrAngle2 = boxPlotInfo.barWidth;
							}
						}
					}
				}
			}
		}

	private void drawBoxMeanIndicators(Graphics2D g, float median, float mean, float bar, float lineWidth) {
		switch (mBoxplotMeanMode) {
		case cBoxplotMeanModeMedian:
			drawIndicatorLine(g, median, bar, lineWidth, getContrastGrey(SCALE_STRONG));
			break;
		case cBoxplotMeanModeMean:
			drawIndicatorLine(g, mean, bar, lineWidth, Color.RED.darker());
			break;
		case cBoxplotMeanModeLines:
			drawIndicatorLine(g, mean, bar, lineWidth, Color.RED.darker());
			drawIndicatorLine(g, median, bar, lineWidth, getContrastGrey(SCALE_STRONG));
			break;
		case cBoxplotMeanModeTriangles:
			float width = mChartInfo.barWidth / 4;
			float space = width / 3;
			float tip = space + 1.5f * width;

			if (mChartInfo.barAxis == 1) {
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

	private void drawIndicatorLine(Graphics2D g, float position, float bar, float lineWidth, Color color) {
		g.setColor(color);
		if (mChartInfo.barAxis == 1) {
			g.fillRect(Math.round(bar-mChartInfo.barWidth/2),
					   Math.round(position-lineWidth/2),
					   Math.round(mChartInfo.barWidth),
					   Math.round(lineWidth));
			}
		else {
			g.fillRect(Math.round(position-lineWidth/2),
					   Math.round(bar-mChartInfo.barWidth/2),
					   Math.round(lineWidth),
					   Math.round(mChartInfo.barWidth));
			}
		}

	private void drawIndicatorTriangle(Graphics2D g, float x1, float y1, float x2, float y2, float x3, float y3, Color color) {
		GeneralPath polygon = new GeneralPath(GeneralPath.WIND_NON_ZERO, 3);

		polygon.moveTo(Math.round(x1), Math.round(y1));
		polygon.lineTo(Math.round(x2), Math.round(y2));
		polygon.lineTo(Math.round(x3), Math.round(y3));
		polygon.closePath();

		g.setColor(color);
		g.fill(polygon);
		g.setColor(getContrastGrey(SCALE_STRONG));
		g.draw(polygon);
		}

	private void markHighlighted(Graphics2D g) {
		if (isVisible(mHighlightedPoint)) {
			g.setColor(getContrastGrey(SCALE_STRONG));
			if (mLabelColumn[cMidCenter] == -1)
				markMarker(g, (VisualizationPoint2D)mHighlightedPoint, true);
			if (mHighlightedLabelPosition != null) {
				g.setStroke(mFatLineStroke);
				g.drawRect(mHighlightedLabelPosition.getScreenX1(),
						mHighlightedLabelPosition.getScreenY1(),
						mHighlightedLabelPosition.getScreenWidth(),
						mHighlightedLabelPosition.getScreenHeight());
				}
			}
		}

	@Override
	protected void updateHighlightedLabelPosition() {
		int newX = mHighlightedLabelPosition.getLabelCenterOnScreenX();
		int newY = mHighlightedLabelPosition.getLabelCenterOnScreenY();
		Rectangle bounds = getGraphBounds(Math.round(mHighlightedPoint.screenX), Math.round(mHighlightedPoint.screenY), false);
		if (bounds != null && bounds.contains(newX, newY)) { // don't allow dragging into another split view
			int sx1 = bounds.x;
			int sx2 = bounds.x + bounds.width;
			int sy1 = bounds.y;
			int sy2 = bounds.y + bounds.height;
			float rx = (float) (mHighlightedLabelPosition.getLabelCenterOnScreenX() - sx1) / (float) (sx2 - sx1);
			float x = mAxisVisMin[0] + rx * (mAxisVisMax[0] - mAxisVisMin[0]);
			float ry = (float) (mHighlightedLabelPosition.getLabelCenterOnScreenY() - sy1) / (float) (sy2 - sy1);
			float y = mAxisVisMin[1] + (1f - ry) * (mAxisVisMax[1] - mAxisVisMin[1]);
			mHighlightedLabelPosition.setCustom(true);
			mHighlightedLabelPosition.setXY(x, y);
			}
		}

	private void markReference(Graphics2D g) {
		g.setColor(Color.red);
		if (mLabelColumn[cMidCenter] != -1) {
			LabelPosition2D lp = mActivePoint.getLabelPosition(mLabelColumn[cMidCenter]);
			if (lp != null) {
				g.setStroke(mFatLineStroke);
				g.drawRect(lp.getScreenX1(), lp.getScreenY1(), lp.getScreenWidth(), lp.getScreenHeight());
				return;
				}
			}

		markMarker(g, (VisualizationPoint2D)mActivePoint, true);
		}

	@Override
	public boolean showCrossHair() {
		return !mMouseIsDown && mCrossHairMode != CROSSHAIR_MODE_NONE
			&& (mSplitter == null || mSplitter.getHVIndex(mMouseX1, mMouseY1, false) != -1);
		}

	private void drawCrossHair(Graphics2D g) {
		Rectangle bounds = getGraphBounds(mMouseX1, mMouseY1, true);
		if (bounds != null) {
			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (int)scaleIfSplitView(mFontHeight)));
			if (mMouseX1 >= bounds.x
			 && (mCrossHairMode == CROSSHAIR_MODE_BOTH
			  || mCrossHairMode == CROSSHAIR_MODE_X
			  || (mCrossHairMode == CROSSHAIR_MODE_AUTOMATIC
			   && (mScaleLineList[0].size() == 0
				|| (mAxisIndex[0] != -1 && !mIsCategoryAxis[0])
				|| (mAxisIndex[0] == -1 && mChartType == cChartTypeBars && mChartInfo.barAxis == 0))))) {
				float position = (float)(mMouseX1-bounds.x)/(float)bounds.width;
				Object label = calculateDynamicScaleLabel(0, position);
				if (label != null)
					drawScaleLine(g, bounds, 0, label, position, true, false);
				}
			if (mMouseY1 <= bounds.y+bounds.height
			 && (mCrossHairMode == CROSSHAIR_MODE_BOTH
			  || mCrossHairMode == CROSSHAIR_MODE_Y
			  || (mCrossHairMode == CROSSHAIR_MODE_AUTOMATIC
			   && (mScaleLineList[1].size() == 0
				|| (mAxisIndex[1] != -1 && !mIsCategoryAxis[1])
				|| (mAxisIndex[1] == -1 && mChartType == cChartTypeBars && mChartInfo.barAxis == 1))))) {
				float position = (float)(bounds.y+bounds.height-mMouseY1)/(float)bounds.height;
				Object label = calculateDynamicScaleLabel(1, position);
				if (label != null)
					drawScaleLine(g, bounds, 1, label, position, true, false);
				}
			}
		}

	private void drawCurves(Rectangle baseGraphRect) {
		float lineWidth = scaleIfSplitView(mFontHeight)/12f;
		Stroke curveStroke = new BasicStroke(lineWidth * mCurveLineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		mG.setColor(getContrastGrey(SCALE_STRONG));
		mG.setStroke(curveStroke);

		switch (mCurveInfo & cCurveModeMask) {
		case cCurveModeVertical:
			drawVerticalMeanLine(baseGraphRect);
			break;
		case cCurveModHorizontal:
			drawHorizontalMeanLine(baseGraphRect);
			break;
		case cCurveModeFitted:
			drawFittedMeanLine(baseGraphRect);
			break;
		case cCurveModeSmooth:
			drawSmoothCurve();
			break;
		case cCurveModeExpression:
			drawExpressionCurve(baseGraphRect);
			break;
			}
		}

	@Override
	public String getStatisticalValues() {
		if (mChartType != cChartTypeScatterPlot)
			return super.getStatisticalValues();

		StringWriter stringWriter = new StringWriter(1024);
		BufferedWriter writer = new BufferedWriter(stringWriter);

		try {
			if ((mCurveInfo & cCurveModeMask) == cCurveModeVertical)
				getMeanLineStatistics(writer, 0);
			if ((mCurveInfo & cCurveModeMask) == cCurveModHorizontal)
				getMeanLineStatistics(writer, 1);
			if ((mCurveInfo & cCurveModeMask) == cCurveModeFitted)
				getFittedLineStatistics(writer);

			if (mShownCorrelationType != CorrelationCalculator.TYPE_NONE && mCorrelationCoefficient != null) {
				writer.write("Correlation coefficient"+ " ("+CorrelationCalculator.TYPE_NAME[mShownCorrelationType]+"):");
				writer.newLine();
				if (mCorrelationCoefficient.length == 1) {
					writer.write(DoubleFormat.toString(mCorrelationCoefficient[0], 3));
					writer.newLine();
					}
				else {
					String[][] splittingCategory = getSplitViewCategories();
					if (isSplitView())
						writer.write(mTableModel.getColumnTitle(mSplittingColumn[0])+"\t");
					if (isSplitView() && mSplittingColumn[1] != cColumnUnassigned)
						writer.write(mTableModel.getColumnTitle(mSplittingColumn[1])+"\t");
					writer.write("r");
					writer.newLine();
					for (int hv=0; hv<mHVCount; hv++) {
						if (isSplitView())
							writer.write(splittingCategory[0][(mSplittingColumn[1] == cColumnUnassigned) ? hv : mSplitter.getHIndex(hv)]+"\t");
						if (isSplitView() && mSplittingColumn[1] != cColumnUnassigned)
							writer.write(splittingCategory[1][mSplitter.getVIndex(hv)]+"\t");
						writer.write(DoubleFormat.toString(mCorrelationCoefficient[hv], 3));
						writer.newLine();
						}
					}
				}

			writer.close();
			}
		catch (IOException ioe) {}

		return stringWriter.toString();
		}

	private void getMeanLineStatistics(BufferedWriter writer, int axis) throws IOException {
		int colorColumn = mMarkerColor.getColorColumn();
		int catCount = ((mCurveInfo & cCurveSplitByCategory) != 0
					 && colorColumn != cColumnUnassigned
					 && mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) ?
							 mTableModel.getCategoryCount(colorColumn) : 1;
		int[][] count = new int[mHVCount][catCount];
		float[][] xmean = new float[mHVCount][catCount];
		float[][] stdDev = new float[mHVCount][catCount];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
				xmean[mPoint[i].hvIndex][cat] += getAxisValue(mPoint[i].record, axis);
				count[mPoint[i].hvIndex][cat]++;
				}
			}

		for (int hv=0; hv<mHVCount; hv++)
			for (int cat=0; cat<catCount; cat++)
				if (count[hv][cat] != 0)
					xmean[hv][cat] /= count[hv][cat];

		if ((mCurveInfo & cCurveStandardDeviation) != 0) {
			for (int i=0; i<mDataPoints; i++) {
				if (isVisibleExcludeNaN(mPoint[i])) {
					int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
					stdDev[mPoint[i].hvIndex][cat] += (getAxisValue(mPoint[i].record, axis) - xmean[mPoint[i].hvIndex][cat])
											   		* (getAxisValue(mPoint[i].record, axis) - xmean[mPoint[i].hvIndex][cat]);
					}
				}
			}

		String[][] splittingCategory = getSplitViewCategories();
		String[] colorCategory = (catCount == 1) ? null : mTableModel.getCategoryList(colorColumn);

//		writer.write((axis == 0) ? "Vertical Mean Line:" : "Horizontal Mean Line:");	// without this line we can paste the data into DataWarrior
//		writer.newLine();

		// if we have distinct curve statistics by color categories and if we we split the view by the same column,
		// the we have lots of empty combinations that we should suppress in the output
		boolean isRedundantSplitting = catCount != 1 && isSplitView()
				&& (mSplittingColumn[0] == colorColumn
				|| (mSplittingColumn[1] != cColumnUnassigned && mSplittingColumn[1] == colorColumn));

		if (isSplitView())
			writer.write(mTableModel.getColumnTitle(mSplittingColumn[0])+"\t");
		if (isSplitView() && mSplittingColumn[1] != cColumnUnassigned)
			writer.write(mTableModel.getColumnTitle(mSplittingColumn[1])+"\t");
		if (catCount != 1 && !isRedundantSplitting)
			writer.write(mTableModel.getColumnTitle(colorColumn)+"\t");
		writer.write("Value Count\tMean Value");
		if ((mCurveInfo & cCurveStandardDeviation) != 0)
			writer.write("\tStandard Deviation");
		writer.newLine();

		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (count[hv][cat] == 0)
					continue;

				if (isSplitView())
					writer.write(splittingCategory[0][(mSplittingColumn[1] == cColumnUnassigned) ? hv : mSplitter.getHIndex(hv)]+"\t");
				if (isSplitView() && mSplittingColumn[1] != cColumnUnassigned)
					writer.write(splittingCategory[1][mSplitter.getVIndex(hv)]+"\t");
				if (catCount != 1 && !isRedundantSplitting)
					writer.write(colorCategory[cat]+"\t");
				writer.write(count[hv][cat]+"\t");
				writer.write(count[hv][cat] == 0 ? "" : formatValue(xmean[hv][cat], mAxisIndex[axis]));
				if ((mCurveInfo & cCurveStandardDeviation) != 0) {
					stdDev[hv][cat] /= (count[hv][cat]-1);
					stdDev[hv][cat] = (float)Math.sqrt(stdDev[hv][cat]);
					writer.write("\t"+formatValue(stdDev[hv][cat], mAxisIndex[axis]));
					}
				writer.newLine();
				}
			}
		writer.newLine();
		}

	private void getFittedLineStatistics(BufferedWriter writer) throws IOException {
		int colorColumn = mMarkerColor.getColorColumn();
		int catCount = ((mCurveInfo & cCurveSplitByCategory) != 0
					 && colorColumn != cColumnUnassigned
					 && mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) ?
							 mTableModel.getCategoryCount(colorColumn) : 1;
		int[][] count = new int[mHVCount][catCount];
		float[][] sx = new float[mHVCount][catCount];
		float[][] sy = new float[mHVCount][catCount];
		float[][] sx2 = new float[mHVCount][catCount];
		float[][] sxy = new float[mHVCount][catCount];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
				sx[mPoint[i].hvIndex][cat] += getAxisValue(mPoint[i].record, 0);
				sy[mPoint[i].hvIndex][cat] += getAxisValue(mPoint[i].record, 1);
				sx2[mPoint[i].hvIndex][cat] += getAxisValue(mPoint[i].record, 0) * getAxisValue(mPoint[i].record, 0);
				sxy[mPoint[i].hvIndex][cat] += getAxisValue(mPoint[i].record, 0) * getAxisValue(mPoint[i].record, 1);
				count[mPoint[i].hvIndex][cat]++;
				}
			}
		float[][] m = null;
		float[][] b = null;
		m = new float[mHVCount][catCount];
		b = new float[mHVCount][catCount];
		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				m[hv][cat] = (count[hv][cat]*sxy[hv][cat]-sx[hv][cat]*sy[hv][cat])/(count[hv][cat]*sx2[hv][cat]-sx[hv][cat]*sx[hv][cat]);
				b[hv][cat] = sy[hv][cat]/count[hv][cat]-m[hv][cat]*sx[hv][cat]/count[hv][cat];
				}
			}

		float[][] stdDev = null;
		if ((mCurveInfo & cCurveStandardDeviation) != 0) {
			stdDev = new float[mHVCount][catCount];
			for (int i=0; i<mDataPoints; i++) {
				if (isVisibleExcludeNaN(mPoint[i])) {
					int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
					float b2 = getAxisValue(mPoint[i].record, 1) + getAxisValue(mPoint[i].record, 0)/m[mPoint[i].hvIndex][cat];
					float xs = (b2-b[mPoint[i].hvIndex][cat])/(m[mPoint[i].hvIndex][cat]+1.0f/m[mPoint[i].hvIndex][cat]);
					float ys = -xs/m[mPoint[i].hvIndex][cat] + b2;
					stdDev[mPoint[i].hvIndex][cat] += (getAxisValue(mPoint[i].record, 0)-xs)*(getAxisValue(mPoint[i].record, 0)-xs);
					stdDev[mPoint[i].hvIndex][cat] += (getAxisValue(mPoint[i].record, 1)-ys)*(getAxisValue(mPoint[i].record, 1)-ys);
					}
				}
			}

		String[][] splittingCategory = getSplitViewCategories();
		String[] colorCategory = (catCount == 1) ? null : mTableModel.getCategoryList(colorColumn);

//		writer.write("Fitted Straight Line:");	// without this line we can paste the data into DataWarrior
//		writer.newLine();

		if (mTableModel.isLogarithmicViewMode(mAxisIndex[0]) || mTableModel.isLogarithmicViewMode(mAxisIndex[1])) {
			writer.write("Gradient m and standard deviation are based on logarithmic values.");
			writer.newLine();
			}

		// if we have distinct curve statistics by color categories and if we we split the view by the same column,
		// the we have lots of empty combinations that we should suppress in the output
		boolean isRedundantSplitting = catCount != 1 && isSplitView()
				&& (mSplittingColumn[0] == colorColumn
				|| (mSplittingColumn[1] != cColumnUnassigned && mSplittingColumn[1] == colorColumn));

		if (isSplitView())
			writer.write(mTableModel.getColumnTitle(mSplittingColumn[0])+"\t");
		if (isSplitView() && mSplittingColumn[1] != cColumnUnassigned)
			writer.write(mTableModel.getColumnTitle(mSplittingColumn[1])+"\t");
		if (catCount != 1 && !isRedundantSplitting)
			writer.write(mTableModel.getColumnTitle(colorColumn)+"\t");
		writer.write("Value Count\tGradient m\tIntercept b");
		if ((mCurveInfo & cCurveStandardDeviation) != 0)
			writer.write("\tStandard Deviation");
		writer.newLine();

		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (count[hv][cat] == 0)
					continue;

				if (isSplitView())
					writer.write(splittingCategory[0][(mSplittingColumn[1] == cColumnUnassigned) ? hv : mSplitter.getHIndex(hv)]+"\t");
				if (isSplitView() && mSplittingColumn[1] != cColumnUnassigned)
					writer.write(splittingCategory[1][mSplitter.getVIndex(hv)]+"\t");
				if (catCount != 1 && !isRedundantSplitting)
					writer.write(colorCategory[cat]+"\t");
				writer.write(count[hv][cat]+"\t");
				if (count[hv][cat] < 2) {
					writer.write("\t");
					if ((mCurveInfo & cCurveStandardDeviation) != 0)
						writer.write("\t");
					}
				else {
					if (count[hv][cat]*sx2[hv][cat] == sx[hv][cat]*sx[hv][cat])
						writer.write("Infinity\t-Infinity");
					else if (count[hv][cat]*sxy[hv][cat] == sx[hv][cat]*sy[hv][cat])
						writer.write("0.0\t"+formatValue(sy[hv][cat] / count[hv][cat], mAxisIndex[1]));
					else
						writer.write(DoubleFormat.toString(m[hv][cat])+"\t"+formatValue(b[hv][cat], mAxisIndex[1]));
					if ((mCurveInfo & cCurveStandardDeviation) != 0) {
						stdDev[hv][cat] /= (count[hv][cat]-1);
						stdDev[hv][cat] = (float)Math.sqrt(stdDev[hv][cat]);
						writer.write("\t"+DoubleFormat.toString(stdDev[hv][cat]));
						}
					}
				writer.newLine();
				}
			}
		writer.newLine();
		}

	private String[][] getSplitViewCategories() {
		String[][] splittingCategory = null;
		if (isSplitView()) {
			splittingCategory = new String[2][];
			splittingCategory[0] = mShowEmptyInSplitView ? mTableModel.getCategoryList(mSplittingColumn[0])
														 : getVisibleCategoryList(mSplittingColumn[0]);
			if (mSplittingColumn[1] != cColumnUnassigned)
				splittingCategory[1] = mShowEmptyInSplitView ? mTableModel.getCategoryList(mSplittingColumn[1])
															 : getVisibleCategoryList(mSplittingColumn[1]);
			}
		return splittingCategory;
		}

	private void drawVerticalMeanLine(Rectangle baseGraphRect) {
		int catCount = ((mCurveInfo & cCurveSplitByCategory) != 0
					 && mMarkerColor.getColorColumn() != cColumnUnassigned
					 && mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) ?
							 mTableModel.getCategoryCount(mMarkerColor.getColorColumn()) : 1;
		int[][] count = new int[mHVCount][catCount];
		float[][] xmean = new float[mHVCount][catCount];
		float[][] stdDev = new float[mHVCount][catCount];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i]) && mPoint[i].hvIndex != -1) {
				int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
				xmean[mPoint[i].hvIndex][cat] += mPoint[i].screenX;
				count[mPoint[i].hvIndex][cat]++;
				}
			}

		for (int hv=0; hv<mHVCount; hv++)
			for (int cat=0; cat<catCount; cat++)
				if (count[hv][cat] != 0)
					xmean[hv][cat] /= count[hv][cat];

		boolean showStdDev = ((mCurveInfo & cCurveStandardDeviation) != 0);
		if (showStdDev) {
			for (int i=0; i<mDataPoints; i++) {
				if (isVisibleExcludeNaN(mPoint[i]) && mPoint[i].hvIndex != -1) {
					int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
					stdDev[mPoint[i].hvIndex][cat] += (mPoint[i].screenX - xmean[mPoint[i].hvIndex][cat])
											   		* (mPoint[i].screenX - xmean[mPoint[i].hvIndex][cat]);
					}
				}
			}

		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (count[hv][cat] != 0) {
					int hOffset = 0;
					int vOffset = 0;
					if (isSplitView()) {
						hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
						vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
						}
					int ymin = baseGraphRect.y + vOffset;
					int ymax = ymin + baseGraphRect.height;
	
					if (catCount != 1)
						mG.setColor(mMarkerColor.getColor(cat));

					float dx = 0f;
					if (showStdDev) {
						stdDev[hv][cat] /= (count[hv][cat]-1);
						stdDev[hv][cat] = (float)Math.sqrt(stdDev[hv][cat]);
						dx = stdDev[hv][cat];
						}

					drawVerticalLine(xmean[hv][cat], ymin, ymax, dx);
					}
				}
			}
		}

	private void drawVerticalLine(float x, float ymin, float ymax, float stdDev) {
		if (stdDev != 0) {
			Composite original = mG.getComposite();
			Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f);
			mG.setComposite(composite);
			mG.fill(new Rectangle2D.Float(x-stdDev, ymin, 2*stdDev, ymax-ymin));
			mG.setComposite(original);
			}
		mG.draw(new Line2D.Float(x, ymin, x, ymax));
		}

	private void drawHorizontalMeanLine(Rectangle baseGraphRect) {
		int catCount = ((mCurveInfo & cCurveSplitByCategory) != 0
					 && mMarkerColor.getColorColumn() != cColumnUnassigned
					 && mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) ?
							 mTableModel.getCategoryCount(mMarkerColor.getColorColumn()) : 1;
		int[][] count = new int[mHVCount][catCount];
		float[][] ymean = new float[mHVCount][catCount];
		float[][] stdDev = new float[mHVCount][catCount];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i]) && mPoint[i].hvIndex != -1) {
				int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
				ymean[mPoint[i].hvIndex][cat] += mPoint[i].screenY;
				count[mPoint[i].hvIndex][cat]++;
				}
			}

		for (int hv=0; hv<mHVCount; hv++)
			for (int cat=0; cat<catCount; cat++)
				if (count[hv][cat] != 0)
					ymean[hv][cat] /= count[hv][cat];

		boolean showStdDev = ((mCurveInfo & cCurveStandardDeviation) != 0);
		if (showStdDev) {
			for (int i=0; i<mDataPoints; i++) {
				if (isVisibleExcludeNaN(mPoint[i]) && mPoint[i].hvIndex != -1) {
					int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
					stdDev[mPoint[i].hvIndex][cat] += (mPoint[i].screenY - ymean[mPoint[i].hvIndex][cat])
													* (mPoint[i].screenY - ymean[mPoint[i].hvIndex][cat]);
					}
				}
			}
		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (count[hv][cat] != 0) {
					int hOffset = 0;
					int vOffset = 0;
					if (isSplitView()) {
						hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
						vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
						}
					int xmin = baseGraphRect.x + hOffset;
					int xmax = xmin + baseGraphRect.width;
	
					if (catCount != 1)
						mG.setColor(mMarkerColor.getColor(cat));

					float dy = 0f;
					if (showStdDev) {
						stdDev[hv][cat] /= (count[hv][cat]-1);
						stdDev[hv][cat] = (float)Math.sqrt(stdDev[hv][cat]);
						dy = stdDev[hv][cat];
						}

					drawHorizontalLine(xmin, xmax, ymean[hv][cat], dy);
					}
				}
			}
		}

	private void drawHorizontalLine(float xmin, float xmax, float y, float stdDev) {
		if (stdDev != 0) {
			Composite original = mG.getComposite();
			Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f);
			mG.setComposite(composite);
			mG.fill(new Rectangle2D.Float(xmin, y-stdDev, xmax-xmin, 2*stdDev));
			mG.setComposite(original);
			}
		mG.draw(new Line2D.Float(xmin, y, xmax, y));
		}

	private void calculateExpressionCurve(Rectangle baseGraphRect) {
		final int EXPRESSION_CURVE_STEPS = 512; // steps used for full width curve

		if (mCurveExpression == null
		 || mAxisVisMin[0] == mAxisVisMax[0]
		 || mAxisVisMin[1] == mAxisVisMax[1])
			return;

		mCurveXMin = new float[1][1];
		mCurveXMin[0][0] = baseGraphRect.x;
		mCurveXInc = new float[1][1];
		mCurveXInc[0][0] = (float)baseGraphRect.width / EXPRESSION_CURVE_STEPS;
		mCurveY = new float[1][1][EXPRESSION_CURVE_STEPS + 1];

		JEP parser = new JEP();
		parser.addStandardFunctions();
		parser.addStandardConstants();
		parser.addVariable("x", 0);
		parser.parseExpression(mCurveExpression);
		if (parser.hasError())
			return;

		for (int i=0; i<=EXPRESSION_CURVE_STEPS; i++) {
			float x = mAxisVisMin[0] + i * (mAxisVisMax[0] - mAxisVisMin[0]) / EXPRESSION_CURVE_STEPS;
			if (mTableModel.isLogarithmicViewMode(mAxisIndex[0]))
				x = (float)Math.pow(10.0, x);
			parser.addVariable("x", x);
			Object o = parser.getValueAsObject();
			float y = (o != null && o instanceof Double) ? (float)((Double)o).doubleValue() : Float.NaN;
			if (mTableModel.isLogarithmicViewMode(mAxisIndex[1]))
				y = (float)Math.log10(y);
			mCurveY[0][0][i] = Float.isNaN(y) ? Float.NaN
					: baseGraphRect.y + baseGraphRect.height * (1 - (y - mAxisVisMin[1]) / (mAxisVisMax[1] - mAxisVisMin[1]));
			}

		if ((mCurveInfo & cCurveStandardDeviation) != 0) {
			mCurveStdDev = new float[mHVCount][1];
			int[][] stdDevCount = new int[mHVCount][1];
			for (int i=0; i<mDataPoints; i++) {
				if (isVisibleExcludeNaN(mPoint[i]) && mPoint[i].hvIndex != -1) {
					int hv = mPoint[i].hvIndex;
					int dx = mSplitter == null ? 0 : mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
					int dy = mSplitter == null ? 0 : mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
					float xrel = (mPoint[i].screenX - dx - mCurveXMin[0][0]) / mCurveXInc[0][0];
					float ydif;
					int index = (int)xrel;
					if (index+1 >= mCurveY[0][0].length) {  // rare but possible case
						ydif = mPoint[i].screenY - mCurveY[0][0][index];
						}
					else {
						float weight2 = xrel - index;
						float weight1 = 1f - weight2;
						ydif = mPoint[i].screenY - dy
								- (weight1 * mCurveY[0][0][index] + weight2 * mCurveY[0][0][index + 1]);
						}
					mCurveStdDev[hv][0] += ydif * ydif;
					stdDevCount[hv][0]++;
					}
				}
			for (int hv=0; hv<mHVCount; hv++)
				mCurveStdDev[hv][0] = (stdDevCount[hv][0] <= 1) ? 0f
						: (float)Math.sqrt(mCurveStdDev[hv][0] / (stdDevCount[hv][0] - 1));
			}
		}

	private void calculateSmoothCurve(int graphWidth) {
		final int SMOOTH_CURVE_STEPS = 128; // steps used for full width curve

		int catCount = ((mCurveInfo & cCurveSplitByCategory) != 0
				&& mMarkerColor.getColorColumn() != cColumnUnassigned
				&& mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) ?
				mTableModel.getCategoryCount(mMarkerColor.getColorColumn()) : 1;

		mCurveXMin = new float[mHVCount][catCount];
		float[][] xmax = new float[mHVCount][catCount];
		mCurveXInc = new float[mHVCount][catCount];
		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat = 0; cat<catCount; cat++) {
				mCurveXMin[hv][cat] = Float.MAX_VALUE;
				xmax[hv][cat] = Float.MIN_VALUE;
				}
			}
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i]) && mPoint[i].hvIndex != -1) {
				int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
				int hv = mPoint[i].hvIndex;
				if (mCurveXMin[hv][cat] > mPoint[i].screenX)
					mCurveXMin[hv][cat] = mPoint[i].screenX;
				if (xmax[hv][cat] < mPoint[i].screenX)
					xmax[hv][cat] = mPoint[i].screenX;
				}
			}

		mCurveY = new float[mHVCount][catCount][];
		float[][][] weight = new float[mHVCount][catCount][];

		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat = 0; cat<catCount; cat++) {
				if (mCurveXMin[hv][cat] != Float.MAX_VALUE) {
					float xdif = xmax[hv][cat] - mCurveXMin[hv][cat];
					if (xdif != 0) {
						int steps = Math.round(xdif / graphWidth * SMOOTH_CURVE_STEPS);
						if (steps != 0) {
							mCurveY[hv][cat] = new float[steps+1];
							weight[hv][cat] = new float[steps+1];
							mCurveXInc[hv][cat] = xdif / steps;
							}
						}
					}
				}
			}

		float weightFactor = mCurveSmoothing * mCurveSmoothing * 200f;

		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i]) && mPoint[i].hvIndex != -1) {
				int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
				int hv = mPoint[i].hvIndex;
				if (mCurveY[hv][cat] != null) {
					for (int s = 0; s<mCurveY[hv][cat].length; s++) {
						float xs = mCurveXMin[hv][cat] + mCurveXInc[hv][cat] * s;
						float d = Math.abs(mPoint[i].screenX - xs) / graphWidth;
						float w = (float)Math.exp(-d * weightFactor);
						mCurveY[hv][cat][s] += w * mPoint[i].screenY;
						weight[hv][cat][s] += w;
						}
					}
				}
			}
		for (int hv=0; hv<mHVCount; hv++)
			for (int cat=0; cat<catCount; cat++)
				if (mCurveY[hv][cat] != null)
					for (int i = 0; i<mCurveY[hv][cat].length; i++)
						mCurveY[hv][cat][i] /= weight[hv][cat][i];

		if ((mCurveInfo & cCurveStandardDeviation) != 0) {
			mCurveStdDev = new float[mHVCount][catCount];
			int[][] stdDevCount = new int[mHVCount][catCount];
			for (int i=0; i<mDataPoints; i++) {
				if (isVisibleExcludeNaN(mPoint[i]) && mPoint[i].hvIndex != -1) {
					int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
					int hv = mPoint[i].hvIndex;
					if (mCurveY[hv][cat] != null) {
						float xrel = (mPoint[i].screenX - mCurveXMin[hv][cat]) / mCurveXInc[hv][cat];
						float ydif;
						int index = (int)xrel;
						if (index+1 >= mCurveY[hv][cat].length) {  // rare but possible case
							ydif = mPoint[i].screenY - mCurveY[hv][cat][index];
							}
						else {
							float weight2 = xrel - index;
							float weight1 = 1f - weight2;
							ydif = mPoint[i].screenY
									- (weight1 * mCurveY[hv][cat][index] + weight2 * mCurveY[hv][cat][index + 1]);
							}
						mCurveStdDev[hv][cat] += ydif * ydif;
						stdDevCount[hv][cat]++;
						}
					}
				}
			for (int hv=0; hv<mHVCount; hv++)
				for (int cat=0; cat<catCount; cat++)
					if (mCurveY[hv][cat] != null)
						mCurveStdDev[hv][cat] = (stdDevCount[hv][cat] <= 1) ? 0f
											  : (float)Math.sqrt(mCurveStdDev[hv][cat] / (stdDevCount[hv][cat] - 1));
			}
		}

	private void drawSmoothCurveArea(int hv, Rectangle graphRect) {
		for (int cat = 0; cat<mCurveY[hv].length; cat++) {
			if (mCurveY[hv][cat] != null && mCurveStdDev != null && mCurveStdDev[hv][cat] != 0f) {
				Polygon polygon = new Polygon();
				float x = mCurveXMin[hv][cat];
				float ydif = mCurveStdDev[hv][cat];
				for (int i = 0; i<mCurveY[hv][cat].length; i++) {
					float y = Math.max(graphRect.y, mCurveY[hv][cat][i]-ydif);
					polygon.addPoint(Math.round(x), Math.round(y));
					x += mCurveXInc[hv][cat];
					}
				for (int i = mCurveY[hv][cat].length-1; i>=0; i--) {
					x -= mCurveXInc[hv][cat];
					float y = Math.min(graphRect.y+graphRect.height, mCurveY[hv][cat][i]+ydif);
					polygon.addPoint(Math.round(x), Math.round(y));
					}
				if (mCurveY[hv].length != 1)
					mG.setColor(mMarkerColor.getColor(cat));
				else
					mG.setColor(getContrastGrey(1.0f));
				Composite original = mG.getComposite();
				Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f);
				mG.setComposite(composite);
				mG.fill(polygon);
				mG.setComposite(original);
				}
			}
		}

	private void drawSmoothCurve() {
		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat = 0; cat<mCurveY[hv].length; cat++) {
				if (mCurveY[hv][cat] != null) {
					if (mCurveY[hv].length != 1)
						mG.setColor(mMarkerColor.getColor(cat));
					float x = mCurveXMin[hv][cat];
					for (int i=0; i<mCurveY[hv][cat].length-1; i++) {
						mG.draw(new Line2D.Float(x, mCurveY[hv][cat][i], x+mCurveXInc[hv][cat], mCurveY[hv][cat][i+1]));
						x += mCurveXInc[hv][cat];
						}
					}
				}
			}
		}

	private void drawExpressionCurveArea(int hv, Rectangle graphRect) {
		if (mCurveY != null && mCurveStdDev != null && mCurveStdDev[hv][0] != 0f) {
			int dx = mSplitter == null ? 0 : mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
			int dy = mSplitter == null ? 0 : mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
			float ydif = mCurveStdDev[hv][0];
			float ymin = graphRect.y - dy - ydif;
			float ymax = graphRect.y + graphRect.height - dy + ydif;
			int i1 = 0;
			while (i1 < mCurveY[0][0].length) {
				while (i1 < mCurveY[0][0].length
					&& (mCurveY[0][0][i1] < ymin || mCurveY[0][0][i1] > ymax))
					i1++;

				int i2 = i1;
				while (i2 < mCurveY[0][0].length
					&& (mCurveY[0][0][i2] >= ymin && mCurveY[0][0][i2] <= ymax))
					i2++;

				if (i2 - i1 >= 2) {
					Polygon polygon = new Polygon();
					float x = mCurveXMin[0][0] + dx + i1*mCurveXInc[0][0];
					for (int i=i1; i<i2; i++) {
						float y = Math.max(graphRect.y, mCurveY[0][0][i]+dy-ydif);
						polygon.addPoint(Math.round(x), Math.round(y));
						x += mCurveXInc[0][0];
						}
					for (int i=i2-1; i>=i1; i--) {
						float y = Math.min(graphRect.y+graphRect.height, mCurveY[0][0][i]+dy+ydif);
						x -= mCurveXInc[0][0];
						polygon.addPoint(Math.round(x), Math.round(y));
						}
					mG.setColor(getContrastGrey(1.0f));
					Composite original = mG.getComposite();
					Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f);
					mG.setComposite(composite);
					mG.fill(polygon);
					mG.setComposite(original);
					}

				i1 = i2 + 1;
				}
			}
		}

	private void drawExpressionCurve(Rectangle baseGraphRect) {
		if (mCurveY != null) {
			for (int hv=0; hv<mHVCount; hv++) {
				int dx = mSplitter == null ? 0 : mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
				int dy = mSplitter == null ? 0 : mSplitter.getVIndex(hv) * mSplitter.getGridHeight();

				for (int i=0; i<mCurveY[0][0].length-1; i++) {
					if (mCurveY[0][0][i+1] < baseGraphRect.y
					 || mCurveY[0][0][i+1] > baseGraphRect.y + baseGraphRect.height) {
						i++;    // skip this and next curve fraction
						}
					else if (mCurveY[0][0][i] >= baseGraphRect.y
						  && mCurveY[0][0][i] <= baseGraphRect.y + baseGraphRect.height) {
						float x = mCurveXMin[0][0] + dx + i*mCurveXInc[0][0];
						mG.draw(new Line2D.Float(x, mCurveY[0][0][i] + dy, x + mCurveXInc[0][0], mCurveY[0][0][i + 1] + dy));
						}
					}

				if (hv == mHVCount-1) {
					String formula = "f(x)="+mCurveExpression;
					int textWidth = mG.getFontMetrics().stringWidth(formula);
					int y = baseGraphRect.y+baseGraphRect.height+dy-mG.getFontMetrics().getDescent();
					if (mShownCorrelationType != CorrelationCalculator.TYPE_NONE)
						y -= mG.getFont().getSize();
					drawString(formula, baseGraphRect.x+baseGraphRect.width+dx-textWidth, y);
					}
				}
			}
		}

	private void drawFittedMeanLine(Rectangle baseGraphRect) {
		int catCount = ((mCurveInfo & cCurveSplitByCategory) != 0
					 && mMarkerColor.getColorColumn() != cColumnUnassigned
					 && mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) ?
							 mTableModel.getCategoryCount(mMarkerColor.getColorColumn()) : 1;
		int[][] count = new int[mHVCount][catCount];
		float[][] sx = new float[mHVCount][catCount];
		float[][] sy = new float[mHVCount][catCount];
		float[][] sx2 = new float[mHVCount][catCount];
		float[][] sxy = new float[mHVCount][catCount];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i]) && mPoint[i].hvIndex != -1) {
				int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
				sx[mPoint[i].hvIndex][cat] += mPoint[i].screenX;
				sy[mPoint[i].hvIndex][cat] += mPoint[i].screenY;
				sx2[mPoint[i].hvIndex][cat] += mPoint[i].screenX * mPoint[i].screenX;
				sxy[mPoint[i].hvIndex][cat] += mPoint[i].screenX * mPoint[i].screenY;
				count[mPoint[i].hvIndex][cat]++;
				}
			}
		float[][] m = new float[mHVCount][catCount];
		float[][] b = new float[mHVCount][catCount];
		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				m[hv][cat] = (count[hv][cat]*sxy[hv][cat]-sx[hv][cat]*sy[hv][cat])/(count[hv][cat]*sx2[hv][cat]-sx[hv][cat]*sx[hv][cat]);
				b[hv][cat] = sy[hv][cat]/count[hv][cat]-m[hv][cat]*sx[hv][cat]/count[hv][cat];
				}
			}

		boolean showStdDev = (mCurveInfo & cCurveStandardDeviation) != 0;
		float[][] stdDev = null;
		if (showStdDev) {
			stdDev = new float[mHVCount][catCount];
			for (int i=0; i<mDataPoints; i++) {
				if (isVisibleExcludeNaN(mPoint[i]) && mPoint[i].hvIndex != -1) {
					int cat = (catCount == 1) ? 0 : mPoint[i].colorIndex - VisualizationColor.cSpecialColorCount;
					float b2 = mPoint[i].screenY + mPoint[i].screenX/m[mPoint[i].hvIndex][cat];
					float xs = (b2-b[mPoint[i].hvIndex][cat])/(m[mPoint[i].hvIndex][cat]+1.0f/m[mPoint[i].hvIndex][cat]);
					float ys = -xs/m[mPoint[i].hvIndex][cat] + b2;
					stdDev[mPoint[i].hvIndex][cat] += (mPoint[i].screenX-xs)*(mPoint[i].screenX-xs);
					stdDev[mPoint[i].hvIndex][cat] += (mPoint[i].screenY-ys)*(mPoint[i].screenY-ys);
					}
				}
			}

		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (count[hv][cat] < 2)
					continue;

				float dxy = 0;
				if (showStdDev) {
					stdDev[hv][cat] /= (count[hv][cat]-1);
					stdDev[hv][cat] = (float)Math.sqrt(stdDev[hv][cat]);
					dxy = (float)Math.sqrt(stdDev[hv][cat]*stdDev[hv][cat]*(1+m[hv][cat]*m[hv][cat]));
					}

				if (count[hv][cat]*sx2[hv][cat] == sx[hv][cat]*sx[hv][cat]) {
					float x = sx[hv][cat] / count[hv][cat];
					float ymin = baseGraphRect.y;
					if (isSplitView())
						ymin += mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
					drawVerticalLine(x, ymin, ymin+baseGraphRect.height, dxy);
					continue;
					}
				if (count[hv][cat]*sxy[hv][cat] == sx[hv][cat]*sy[hv][cat]) {
					float y = sy[hv][cat] / count[hv][cat];
					float xmin = baseGraphRect.x;
					if (isSplitView())
						xmin += mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
					drawHorizontalLine(xmin, xmin+baseGraphRect.width, y, dxy);
					continue;
					}

				if (catCount != 1)
					mG.setColor(mMarkerColor.getColor(cat));

				drawInclinedLine(baseGraphRect, hv, m[hv][cat], b[hv][cat], dxy);
				}
			}
		}

	private void drawInclinedLine(Rectangle baseGraphRect, int hv, float m, float b, float stdDev) {
		int hOffset = 0;
		int vOffset = 0;
		if (isSplitView()) {
			hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
			vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
			}

		int xmin = baseGraphRect.x+hOffset;
		int xmax = xmin+baseGraphRect.width;
		int ymin = baseGraphRect.y+vOffset;
		int ymax = ymin+baseGraphRect.height;

		float sxtop = (ymin-b)/m;
		float sxbottom = (ymax-b)/m;
		float syleft = m*xmin+b;
		float syright = m*(xmax)+b;
		float[] x = new float[2];
		float[] y = new float[2];
		if (syleft >= ymin && syleft <= ymax) {
			x[0] = xmin;
			y[0] = syleft;
			}
		else if (m < 0) {
			if (sxbottom < xmin || sxbottom > xmax)
				return;
			x[0] = sxbottom;
			y[0] = ymax;
			}
		else {
			if (sxtop < xmin || sxtop > xmax)
				return;
			x[0] = sxtop;
			y[0] = ymin;
			}
		if (syright >= ymin && syright <= ymax) {
			x[1] = xmax;
			y[1] = syright;
			}
		else if (m < 0) {
			if (sxtop < xmin || sxtop > xmax)
				return;
			x[1] = sxtop;
			y[1] = ymin;
			}
		else {
			if (sxbottom < xmin || sxbottom > xmax)
				return;
			x[1] = sxbottom;
			y[1] = ymax;
			}

		if (stdDev != 0f) {
			Polygon polygon = new Polygon();
			polygon.addPoint(Math.round(x[0]), Math.round(y[0]-stdDev));
			polygon.addPoint(Math.round(x[1]), Math.round(y[1]-stdDev));
			polygon.addPoint(Math.round(x[1]), Math.round(y[1]+stdDev));
			polygon.addPoint(Math.round(x[0]), Math.round(y[0]+stdDev));
			Composite original = mG.getComposite();
			Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f);
			mG.setComposite(composite);
			mG.fill(polygon);
			mG.setComposite(original);
			}

		mG.draw(new Line2D.Float(x[0], y[0], x[1], y[1]));
		}

	private void drawCorrelationCoefficient(Rectangle baseGraphRect) {
		if (mAxisIndex[0] == cColumnUnassigned || mAxisIndex[1] == cColumnUnassigned)
			return;

		int scaledFontHeight = Math.round(scaleIfSplitView(mFontHeight));
		setFontHeight(scaledFontHeight);
		mG.setColor(getContrastGrey(SCALE_STRONG));

		mCorrelationCoefficient = new float[mHVCount];
		if (mHVCount == 1) {
			float r = (float)new CorrelationCalculator().calculateCorrelation(
					new INumericalDataColumn() {
						public int getValueCount() {
							return mDataPoints;
							}
						public double getValueAt(int row) {
							return isVisibleExcludeNaN(mPoint[row]) ? getAxisValue(mPoint[row].record, 0) : Float.NaN;
							}
						},
					new INumericalDataColumn() {
						public int getValueCount() {
							return mDataPoints;
							}
						public double getValueAt(int row) {
							return isVisibleExcludeNaN(mPoint[row]) ? getAxisValue(mPoint[row].record, 1) : Float.NaN;
							}
						},
					mShownCorrelationType);
			String s = "r="+DoubleFormat.toString(r, 3)
					 + " ("+CorrelationCalculator.TYPE_NAME[mShownCorrelationType]+")";
			mG.drawString(s, baseGraphRect.x+baseGraphRect.width-mG.getFontMetrics().stringWidth(s),
							 baseGraphRect.y+baseGraphRect.height-mG.getFontMetrics().getDescent());
			mCorrelationCoefficient[0] = r;
			}
		else {
			int[] count = new int[mHVCount];
			for (int i=0; i<mDataPoints; i++)
				if (isVisibleExcludeNaN(mPoint[i]))
					count[mPoint[i].hvIndex]++;
			float[][][] value = new float[mHVCount][2][];
			for (int hv=0; hv<mHVCount; hv++) {
				value[hv][0] = new float[count[hv]];
				value[hv][1] = new float[count[hv]];
				}
			count = new int[mHVCount];
			for (int i=0; i<mDataPoints; i++) {
				if (isVisibleExcludeNaN(mPoint[i])) {
					value[mPoint[i].hvIndex][0][count[mPoint[i].hvIndex]] = getAxisValue(mPoint[i].record, 0);
					value[mPoint[i].hvIndex][1][count[mPoint[i].hvIndex]] = getAxisValue(mPoint[i].record, 1);
					count[mPoint[i].hvIndex]++;
					}
				}

			for (int hv=0; hv<mHVCount; hv++) {
				if (count[hv] >= 2) {
					final float[][] _value = value[hv];
					float r = (float)new CorrelationCalculator().calculateCorrelation(
							new INumericalDataColumn() {
								public int getValueCount() {
									return _value[0].length;
									}
								public double getValueAt(int row) {
									return _value[0][row];
									}
								},
							new INumericalDataColumn() {
								public int getValueCount() {
									return _value[1].length;
									}
								public double getValueAt(int row) {
									return _value[1][row];
									}
								},
							mShownCorrelationType);

					int hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
					int vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
					String s = "r="+DoubleFormat.toString(r, 3)
							 + " ("+CorrelationCalculator.TYPE_NAME[mShownCorrelationType]+")";
					mG.drawString(s, hOffset+baseGraphRect.x+baseGraphRect.width-mG.getFontMetrics().stringWidth(s),
									 vOffset+baseGraphRect.y+baseGraphRect.height-mG.getFontMetrics().getDescent());
					mCorrelationCoefficient[hv] = r;
					}
				}
			}
		}

	@Override
	protected void setActivePoint(VisualizationPoint newReference) {
		super.setActivePoint(newReference);

		if (mBackgroundColor.getColorColumn() != cColumnUnassigned) {
			if (mTableModel.isDescriptorColumn(mBackgroundColor.getColorColumn())) {
				setBackgroundSimilarityColors();
				mBackgroundValid = false;
				mOffImageValid = false;
				}
			}
		}

	private void markMarker(Graphics2D g, VisualizationPoint2D vp, boolean boldLine) {
		final float GAP = Math.round(scaleIfSplitView(mFontHeight) / 8f);
		float sizeX,sizeY,hSizeX,hSizeY;
		GeneralPath polygon;

		g.setStroke(boldLine ? mVeryFatLineStroke : mFatLineStroke);

		if (mLabelColumn[MarkerLabelDisplayer.cMidCenter] != -1
		 && (!mLabelsInTreeViewOnly || isTreeViewGraph())) {
			LabelPosition2D lp = vp.getLabelPosition(mLabelColumn[cMidCenter]);
			g.draw(new Rectangle2D.Float(lp.getScreenX1(), lp.getScreenY1(), lp.getScreenWidth(), lp.getScreenHeight()));
//			sizeX = vp.widthOrAngle1 + 2f*GAP;
//			sizeY = vp.heightOrAngle2 + 2f*GAP;
//			hSizeX = sizeX/2;
//			hSizeY = sizeY/2;
//			g.draw(new Rectangle2D.Float(vp.screenX-hSizeX, vp.screenY-hSizeY, sizeX, sizeY));
			}
		else if (mChartType == cChartTypeBars
		 || (mChartType == cChartTypeBoxPlot && vp.chartGroupIndex != -1)) {
			int hv = vp.hvIndex;
			int cat = getChartCategoryIndex(vp);
			if (cat != -1) {
				if (mChartInfo.innerDistance != null || mChartInfo.absValueFactor != null) {
					float width = mChartInfo.barWidth;
					if (mChartInfo.useProportionalWidths())
						width *= mChartInfo.getBarWidthFactor(vp.record.getDouble(mMarkerSizeColumn));

					if (mChartInfo.barAxis == 1) {
						sizeX = width + 2f*GAP;
						sizeY = (mChartInfo.innerDistance != null ? mChartInfo.innerDistance[hv][cat]
								: Math.abs(vp.record.getDouble(mChartColumn)) * mChartInfo.absValueFactor[hv][cat])
								+ 2f*GAP;
						}
					else {
						sizeX = (mChartInfo.innerDistance != null ? mChartInfo.innerDistance[hv][cat]
								: Math.abs(vp.record.getDouble(mChartColumn)) * mChartInfo.absValueFactor[hv][cat])
								+ 2f*GAP;
						sizeY = width + 2f*GAP;
						}

					hSizeX = sizeX/2;
					hSizeY = sizeY/2;
					g.draw(new Rectangle2D.Float(vp.screenX-hSizeX, vp.screenY-hSizeY, sizeX, sizeY));
					}
				}
			}
		else if (mChartType == cChartTypePies) {
			if (!mChartInfo.barOrPieDataAvailable)
				return;

			int hv = vp.hvIndex;
			int cat = getChartCategoryIndex(vp);
			if (cat != -1) {
				float x = mChartInfo.pieX[hv][cat];
				float y = mChartInfo.pieY[hv][cat];
				float r = mChartInfo.pieSize[hv][cat]/2 + GAP;
				float dif,angle;
				if (mChartInfo.useProportionalFractions()) {
					angle = vp.widthOrAngle1;
					dif = vp.heightOrAngle2 - vp.widthOrAngle1;
					}
				else {
					dif = 360f / (float)mChartInfo.pointsInCategory[hv][cat];
					angle = dif * vp.chartGroupIndex;
					}
				if (mChartInfo.pointsInCategory[hv][cat] == 1)
					g.draw(new Ellipse2D.Float(x-r, y-r, 2*r, 2*r));
				else
					g.draw(new Arc2D.Float(x-r, y-r, 2*r, 2*r, angle, dif, Arc2D.PIE));
				}
			}
		else if (mMultiValueMarkerMode != cMultiValueMarkerModeNone && mMultiValueMarkerColumns != null) {
			if (mMultiValueMarkerMode == cMultiValueMarkerModeBars) {
				MultiValueBars mvbi = new MultiValueBars();
				mvbi.calculate(vp.widthOrAngle1, vp);
				final int z = mMultiValueMarkerColumns.length-1;
				float x1 = mvbi.firstBarX-GAP;
				float xn = mvbi.firstBarX+mMultiValueMarkerColumns.length*mvbi.barWidth+GAP/2;
				g.draw(new Line2D.Float(x1, mvbi.barY[0]-GAP, x1, mvbi.barY[0]+mvbi.barHeight[0]+GAP/2));
				g.draw(new Line2D.Float(xn, mvbi.barY[z]-GAP, xn, mvbi.barY[z]+mvbi.barHeight[z]+GAP/2));
				float x2 = x1;
				float y1 = mvbi.barY[0]-GAP;
				float y2 = mvbi.barY[0]+mvbi.barHeight[0]+GAP/2;
				for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
					float x3 = mvbi.firstBarX+(i+1)*mvbi.barWidth-GAP;
					float x4 = x3;
					if (i == z || mvbi.barY[i]<mvbi.barY[i+1])
						x3 += 1.5f*GAP;
					if (i == z || mvbi.barY[i]+mvbi.barHeight[i]>mvbi.barY[i+1]+mvbi.barHeight[i+1])
						x4 += 1.5f*GAP;
					g.draw(new Line2D.Float(x1, y1, x3, y1));
					g.draw(new Line2D.Float(x2, y2, x4, y2));
					if (i != z) {
						float y3 = mvbi.barY[i+1]-GAP;
						float y4 = mvbi.barY[i+1]+mvbi.barHeight[i+1]+GAP/2;
						g.draw(new Line2D.Float(x3, y1, x3, y3));
						g.draw(new Line2D.Float(x4, y2, x4, y4));
						y1 = y3;
						y2 = y4;
						}
					x1 = x3;
					x2 = x4;
					}
				}
			else {
				float x = vp.screenX;
				float y = vp.screenY;
				float size = 0.5f  * vp.widthOrAngle1 * (float)Math.sqrt(Math.sqrt(mMultiValueMarkerColumns.length));
				float[] r = new float[mMultiValueMarkerColumns.length];
				for (int i=0; i<mMultiValueMarkerColumns.length; i++)
					r[i] = size * getMarkerSizeVPFactor(vp.record.getDouble(mMultiValueMarkerColumns[i]), mMultiValueMarkerColumns[i]);
				float angleIncrement = 360f / mMultiValueMarkerColumns.length;
				float angle = 90f - angleIncrement;
				for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
					g.draw(new Arc2D.Float(x-r[i], y-r[i], 2*r[i], 2*r[i], angle, angleIncrement, Arc2D.OPEN));

					float radAngle = (float)Math.PI * angle / 180;
					int h = (i == mMultiValueMarkerColumns.length-1) ? 0 : i+1;
					g.draw(new Line2D.Float(x+(float)Math.cos(radAngle)*r[h], y-(float)Math.sin(radAngle)*r[h],
							x+(float)Math.cos(radAngle)*r[i], y-(float)Math.sin(radAngle)*r[i]));

					angle -= angleIncrement;
					}
				}
			}
		else {
			float size = vp.widthOrAngle1;
			float halfSize = size / 2;
			float sx,sy;

			int shape = (mMarkerShapeColumn != cColumnUnassigned) ? vp.shape : mIsFastRendering ? 1 : 0;
			switch (shape) {
			case 0:
				g.draw(new Ellipse2D.Float(vp.screenX-halfSize-GAP, vp.screenY-halfSize-GAP, size+2*GAP, size+2*GAP));
				break;
			case 1:
				g.draw(new Rectangle2D.Float(vp.screenX-halfSize-GAP, vp.screenY-halfSize-GAP, size+2*GAP, size+2*GAP));
				break;
			case 2:
				polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 3);
				polygon.moveTo(vp.screenX-halfSize-1.5f*GAP, vp.screenY+size/3+GAP);
				polygon.lineTo(vp.screenX+halfSize+1.5f*GAP, vp.screenY+size/3+GAP);
				polygon.lineTo(vp.screenX, vp.screenY-2*size/3-2*GAP);
				polygon.closePath();
				g.draw(polygon);
				break;
			case 3:
				polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 4);
				polygon.moveTo(vp.screenX-halfSize-1.4*GAP, vp.screenY);
				polygon.lineTo(vp.screenX, vp.screenY+halfSize+1.4*GAP);
				polygon.lineTo(vp.screenX+halfSize+1.4*GAP, vp.screenY);
				polygon.lineTo(vp.screenX, vp.screenY-halfSize-1.4*GAP);
				polygon.closePath();
				g.draw(polygon);
				break;
			case 4:
				polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, 3);
				polygon.moveTo(vp.screenX-halfSize-1.5f*GAP, vp.screenY-size/3-GAP);
				polygon.lineTo(vp.screenX+halfSize+1.5f*GAP, vp.screenY-size/3-GAP);
				polygon.lineTo(vp.screenX, vp.screenY+2*size/3+2*GAP);
				polygon.closePath();
				g.draw(polygon);
				break;
			case 5:
				sx = size/4+GAP;
				sy = sx+halfSize;
				g.draw(new Rectangle2D.Float(vp.screenX-sx, vp.screenY-sy, 2*sx, 2*sy));
				break;
			case 6:
				sy = size/4+GAP;
				sx = sy+halfSize;
				g.draw(new Rectangle2D.Float(vp.screenX-sx, vp.screenY-sy, 2*sx, 2*sy));
				break;
			default:
				float[] outline = calculateOutline(cExtendedShapeCoords[shape - cSimpleShapeCount], size, GAP);
				polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, outline.length/2);
				polygon.moveTo(vp.screenX+outline[0], vp.screenY+outline[1]);
				for (int i=2; i<outline.length; i+=2)
					polygon.lineTo(vp.screenX+outline[i], vp.screenY+outline[i+1]);
				polygon.closePath();
				g.draw(polygon);
				break;
				}
			}
		}

	private float[] calculateOutline(float[] coords, float size, float gap) {
		float[] outline = new float[coords.length];
		for (int index0=0; index0<coords.length; index0+=2) {
			int index1 = (index0 == 0) ? coords.length-2 : index0 - 2;
			int index2 = (index0 == coords.length-2) ? 0 : index0 + 2;
			double a1 = getAngle(coords[index0], coords[index0+1], coords[index1], coords[index1+1]);
			double a2 = getAngle(coords[index0], coords[index0+1], coords[index2], coords[index2+1]);
			double angleDif = (a2 - a1) / 2;
			double angle = a1 + angleDif;
			float distance = gap / (float)Math.sin(angleDif);
			outline[index0] = coords[index0] * size - distance * (float)Math.sin(angle);
			outline[index0+1] = coords[index0+1] * size - distance * (float)Math.cos(angle);
			}
		return outline;
		}

	private double getAngle(double x1, double y1, double x2, double y2) {
		double angle;
		double xdif = x2 - x1;
		double ydif = y2 - y1;

		if (ydif != 0) {
			angle = Math.atan(xdif/ydif);
			if (ydif < 0) {
				if (xdif < 0)
					angle -= Math.PI;
				else
					angle += Math.PI;
			}
		}
		else
			angle = (xdif >0) ? Math.PI/2 : -Math.PI/2;

		return angle;
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		super.compoundTableChanged(e);

		if (e.getType() == CompoundTableEvent.cChangeExcluded) {
			if (mChartType == cChartTypeBoxPlot
			 || mChartType == cChartTypeWhiskerPlot) {
				invalidateOffImage(true);
				}
			if (mBackgroundColorConsidered == BACKGROUND_VISIBLE_RECORDS)
				mBackgroundValid = false;
			mCurveY = null;
			}
		else if (e.getType() == CompoundTableEvent.cAddRows
			  || e.getType() == CompoundTableEvent.cDeleteRows) {
			for (int axis=0; axis<2; axis++) {
				mScaleDepictor[axis] = null;
				mBackgroundValid = false;
				}
			for (int i=0; i<2; i++)
				mSplittingDepictor[i] = null;
			invalidateOffImage(true);
			}
		else if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			int[] columnMapping = e.getMapping();
			if (mMultiValueMarkerColumns != null) {
				int count = 0;
				for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
					mMultiValueMarkerColumns[i] = columnMapping[mMultiValueMarkerColumns[i]];
					if (mMultiValueMarkerColumns[i] == cColumnUnassigned)
						count++;
					}
				if (count != 0) {
					if (count == mMultiValueMarkerColumns.length) {
						mMultiValueMarkerColumns = null;
						}
					else {
						int[] newColumns = new int[mMultiValueMarkerColumns.length-count];
						int index = 0;
						for (int i=0; i<mMultiValueMarkerColumns.length; i++)
							if (mMultiValueMarkerColumns[i] != cColumnUnassigned)
								newColumns[index++] = mMultiValueMarkerColumns[i];
						mMultiValueMarkerColumns = newColumns;
						}
					invalidateOffImage(false);
					}
				}

			if (mChartMode != cChartModeCount
			 && mChartMode != cChartModePercent
			 && mChartColumn != cColumnUnassigned) {
				mChartColumn = columnMapping[mChartColumn];
				if (mChartColumn == cColumnUnassigned) {
					mChartMode = cChartModeCount;
					invalidateOffImage(true);
					}
				}
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnData) {
			int column = e.getColumn();
			for (int axis=0; axis<2; axis++) {
				if (column == mAxisIndex[axis]) {
					mScaleDepictor[axis] = null;
					mBackgroundValid = false;
					}
				}
			for (int i=0; i<2; i++)
				if (column == mSplittingColumn[i])
					mSplittingDepictor[i] = null;
			if (mMultiValueMarkerColumns != null)
				for (int i=0; i<mMultiValueMarkerColumns.length; i++)
					if (column == mMultiValueMarkerColumns[i])
						invalidateOffImage(false);

			if (mChartMode != cChartModeCount
			 && mChartMode != cChartModePercent
			 && mChartColumn == column) {
				invalidateOffImage(true);
				}
			}

		mBackgroundColor.compoundTableChanged(e);
		}

	public void listChanged(CompoundTableListEvent e) {
		super.listChanged(e);

		if (e.getType() == CompoundTableListEvent.cDelete) {
			if (mBackgroundColorConsidered >= 0) {	// is a list index
				if (e.getListIndex() == mBackgroundColorConsidered) {
					mBackgroundColorConsidered = BACKGROUND_VISIBLE_RECORDS;
					mBackgroundValid = false;
					invalidateOffImage(false);
					}
				else if (mBackgroundColorConsidered > e.getListIndex()) {
					mBackgroundColorConsidered--;
					}
				}
			}
		else if (e.getType() == CompoundTableListEvent.cChange) {
			if (mBackgroundColorConsidered >= 0) {	// is a list index
				if (e.getListIndex() == mBackgroundColorConsidered) {
					mBackgroundValid = false;
					invalidateOffImage(false);
					}
				}
			}

		mBackgroundColor.listChanged(e);
		}

	@Override
	public void colorChanged(VisualizationColor source) {
		if (source == mBackgroundColor) {
			updateBackgroundColorIndices();
			return;
			}

		super.colorChanged(source);
		}

	public VisualizationColor getBackgroundColor() {
		return mBackgroundColor;
		}

	public int getBackgroundColorConsidered() {
		return mBackgroundColorConsidered;
		}

	public int getBackgroundColorRadius() {
		return mBackgroundColorRadius;
		}

	public int getBackgroundColorFading() {
		return mBackgroundColorFading;
		}

	public void setBackgroundColorConsidered(int considered) {
		if (mBackgroundColorConsidered != considered) {
			mBackgroundColorConsidered = considered;
			mBackgroundValid = false;
			invalidateOffImage(false);
			}
		}

	public void setBackgroundColorRadius(int radius) {
		if (mBackgroundColorRadius != radius) {
			mBackgroundColorRadius = radius;
			mBackgroundValid = false;
			invalidateOffImage(false);
			}
		}

	public void setBackgroundColorFading(int fading) {
		if (mBackgroundColorFading != fading) {
			mBackgroundColorFading = fading;
			mBackgroundValid = false;
			invalidateOffImage(false);
			}
		}

	@Override
	protected void addMarkerTooltips(VisualizationPoint vp, TreeSet<Integer> columnSet, StringBuilder sb) {
		if (mMultiValueMarkerColumns != null) {
			for (int i=0; i<mMultiValueMarkerColumns.length; i++)
		        addTooltipRow(vp.record, mMultiValueMarkerColumns[i], null, columnSet, sb);
	        addTooltipRow(vp.record, mMarkerColor.getColorColumn(), null, columnSet, sb);
	        addTooltipRow(vp.record, mMarkerSizeColumn, null, columnSet, sb);
			}
		else {
			super.addMarkerTooltips(vp, columnSet, sb);
			}
		addTooltipRow(vp.record, mBackgroundColor.getColorColumn(), null, columnSet, sb);
		}

	@Override
	public boolean setViewBackground(Color c) {
		if (super.setViewBackground(c)) {
			mBackgroundValid = false;
			return true;
			}
		return false;
		}

	public float getMarkerTransparency() {
		return mMarkerTransparency;
		}

	/**
	 * Changes the marker transparency for non-histogram views
	 * @param transparency value from 0.0 to 1.0
	 */
	public void setMarkerTransparency(float transparency) {
		if (mMarkerTransparency != transparency) {
			mMarkerTransparency = transparency;
			mBackgroundValid = false;
			invalidateOffImage(false);
			}
		}

	public boolean isDrawMarkerOutline() {
		return mDrawMarkerOutline;
		}

	public void setDrawMarkerOutline(boolean b) {
		if (mDrawMarkerOutline != b) {
			mDrawMarkerOutline = b;
			invalidateOffImage(false);
			}
		}

	public boolean isDrawBarPieBoxOutline() {
		return mDrawBarPieBoxOutline;
	}

	public void setDrawBoxOutline(boolean b) {
		if (mDrawBarPieBoxOutline != b) {
			mDrawBarPieBoxOutline = b;
			invalidateOffImage(false);
		}
	}

	private void updateBackgroundColorIndices() {
		if (mBackgroundColor.getColorColumn() == cColumnUnassigned)
			for (int i=0; i<mDataPoints; i++)
				((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = VisualizationColor.cDefaultDataColorIndex;
		else if (CompoundTableListHandler.isListColumn(mBackgroundColor.getColorColumn())) {
			int listIndex = CompoundTableListHandler.convertToListIndex(mBackgroundColor.getColorColumn());
			int flagNo = mTableModel.getListHandler().getListFlagNo(listIndex);
			for (int i=0; i<mDataPoints; i++)
				((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = mPoint[i].record.isFlagSet(flagNo) ?
						VisualizationColor.cSpecialColorCount : VisualizationColor.cSpecialColorCount + 1;
			}
		else if (mTableModel.isDescriptorColumn(mBackgroundColor.getColorColumn()))
			setBackgroundSimilarityColors();
		else if (mBackgroundColor.getColorListMode() == VisualizationColor.cColorListModeCategories) {
			float[] thresholds = mBackgroundColor.getColorThresholds();
			if (thresholds != null) {
				for (int i=0; i<mDataPoints; i++) {
					double value = mPoint[i].record.getDouble(mBackgroundColor.getColorColumn());
					if (mTableModel.isLogarithmicViewMode(mBackgroundColor.getColorColumn()))
						value = Math.pow(10, value);
					((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = (short)(VisualizationColor.cSpecialColorCount+thresholds.length);
					for (int j=0; j<thresholds.length; j++) {
						if (value<thresholds[j]) {
							((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = (short)(VisualizationColor.cSpecialColorCount + j);
							break;
							}
						}
					}
				}
			else {
				for (int i=0; i<mDataPoints; i++)
					((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = VisualizationColor.cSpecialColorCount
							+ mTableModel.getCategoryIndex(mBackgroundColor.getColorColumn(), mPoint[i].record);
				}
			}
		else if (mTableModel.isColumnTypeDouble(mBackgroundColor.getColorColumn())) {
			float min = Float.isNaN(mBackgroundColor.getColorMin()) ?
					mTableModel.getMinimumValue(mBackgroundColor.getColorColumn())
					   : (mTableModel.isLogarithmicViewMode(mBackgroundColor.getColorColumn())) ?
							   (float)Math.log10(mBackgroundColor.getColorMin()) : mBackgroundColor.getColorMin();
			float max = Float.isNaN(mBackgroundColor.getColorMax()) ?
					mTableModel.getMaximumValue(mBackgroundColor.getColorColumn())
					   : (mTableModel.isLogarithmicViewMode(mBackgroundColor.getColorColumn())) ?
							   (float)Math.log10(mBackgroundColor.getColorMax()) : mBackgroundColor.getColorMax();

			//	1. colorMin is explicitly set; max is real max, but lower than min
			// or 2. colorMax is explicitly set; min is real min, but larger than max
			// first case is OK, second needs adaption below to be handled as indented
			if (min >= max)
				if (!Float.isNaN(mBackgroundColor.getColorMax()))
					min = Float.MIN_VALUE;

			for (int i=0; i<mDataPoints; i++) {
				float value = mPoint[i].record.getDouble(mBackgroundColor.getColorColumn());
				if (Float.isNaN(value))
					((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = VisualizationColor.cMissingDataColorIndex;
				else if (value <= min)
					((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = VisualizationColor.cSpecialColorCount;
				else if (value >= max)
					((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = mBackgroundColor.getColorList().length-1;
				else
					((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = (int)(0.5 + VisualizationColor.cSpecialColorCount
						+ (float)(mBackgroundColor.getColorList().length-VisualizationColor.cSpecialColorCount-1)
						* (value - min) / (max - min));
				}
			}

		mBackgroundValid = false;
		invalidateOffImage(true);
		}

	private void setBackgroundSimilarityColors() {
		if (mActivePoint == null)
			for (int i=0; i<mDataPoints; i++)
				((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = VisualizationColor.cDefaultDataColorIndex;
		else {
			for (int i=0; i<mDataPoints; i++) {
				float similarity = mTableModel.getDescriptorSimilarity(
										mActivePoint.record, mPoint[i].record, mBackgroundColor.getColorColumn());
				if (Float.isNaN(similarity))
					((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = VisualizationColor.cMissingDataColorIndex;
				else if (mBackgroundColor.getColorThresholds() != null) {
					float[] thresholds = mBackgroundColor.getColorThresholds();
					((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = (short)(VisualizationColor.cSpecialColorCount + thresholds.length);
					for (int j=0; j<thresholds.length; j++) {
						if (similarity<thresholds[j]) {
							((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = (short)(VisualizationColor.cSpecialColorCount + j);
							break;
							}
						}
					}
				else
					((VisualizationPoint2D)mPoint[i]).backgroundColorIndex = (int)(0.5 + VisualizationColor.cSpecialColorCount
						+ (float)(mBackgroundColor.getColorList().length - VisualizationColor.cSpecialColorCount - 1)
						* similarity);
				}
			}
		}

	public byte[] getBackgroundImageData() {
		if (mBackgroundImageData == null
		 && mBackgroundImage != null) {
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(mBackgroundImage, "png", baos);
				return baos.toByteArray();
				}
			catch (IOException ioe) {
				return null;
				}
			}
		return mBackgroundImageData;
		}

	public BufferedImage getBackgroundImage() {
		return mBackgroundImage;
		}

	public void setBackgroundImageData(byte[] imageData) {
		if (imageData == null) {
			if (mBackgroundImage == null)
				return;

			mBackgroundImage = null;
			mBackgroundImageData = null;
			}
		else {
			try {
				mBackgroundImage = ImageIO.read(new ByteArrayInputStream(imageData));
				mBackgroundImageData = imageData;
				}
			catch (IOException e) {}
			}

		invalidateOffImage(false);
		}

	public void setBackgroundImage(BufferedImage image) {
		if (image == null) {
			if (mBackgroundImage == null)
				return;

			mBackgroundImage = null;
			}
		else {
			mBackgroundImage = image;
			}

		mBackgroundImageData = null;
		invalidateOffImage(false);
		}

	public VisualizationPoint findMarker(int x, int y) {
		if (mChartType == cChartTypePies) {
			if (mChartInfo != null && mChartInfo.barOrPieDataAvailable) {
				int catCount = getCategoryVisCount(0)* getCategoryVisCount(1)*mCaseSeparationCategoryCount;
				for (int hv=mHVCount-1; hv>=0; hv--) {
					for (int cat=catCount-1; cat>=0; cat--) {
						float dx = x - mChartInfo.pieX[hv][cat];
						float dy = mChartInfo.pieY[hv][cat] - y;
						float radius = Math.round(mChartInfo.pieSize[hv][cat]/2);
						if (Math.sqrt(dx*dx+dy*dy) < radius) {
							float angle = (dx==0) ? ((dy>0) ? 0.5f*(float)Math.PI : 1.5f*(float)Math.PI)
										 : (dx<0) ? (float)Math.PI + (float)Math.atan(dy/dx)
										 : (dy<0) ? 2*(float)Math.PI + (float)Math.atan(dy/dx) : (float)Math.atan(dy/dx);
							if (mChartInfo.useProportionalFractions()) {
								angle *= 180f / (float)Math.PI;
								for (int i=mDataPoints-1; i>=0; i--)
									if (mPoint[i].hvIndex == hv
											&& getChartCategoryIndex(mPoint[i]) == cat
											&& angle >= mPoint[i].widthOrAngle1
											&& angle < mPoint[i].heightOrAngle2
											&& isVisibleInBarsOrPies(mPoint[i]))
										return mPoint[i];
								return null;	// should never reach this
								}
							else {
								int index = (int)(mChartInfo.pointsInCategory[hv][cat] * angle/(2*Math.PI));
								if (index>=0 && index<mChartInfo.pointsInCategory[hv][cat]) {
									for (int i=mDataPoints-1; i>=0; i--)
										if (mPoint[i].hvIndex == hv
										 && getChartCategoryIndex(mPoint[i]) == cat
										 && mPoint[i].chartGroupIndex == index
										 && isVisibleInBarsOrPies(mPoint[i]))
											return mPoint[i];
									return null;	// should never reach this
									}
								}
							}
						}
					}
				}

			return null;
			}

		return super.findMarker(x, y);
		}

	@Override
    public float getDistanceToMarker(VisualizationPoint vp, int x, int y) {
		if (mMultiValueMarkerMode != cMultiValueMarkerModeNone && mMultiValueMarkerColumns != null
		 && (mChartType == cChartTypeScatterPlot
		  || mChartType == cChartTypeWhiskerPlot
		  || (mChartType == cChartTypeBoxPlot && vp.chartGroupIndex == -1))) {
			if (mMultiValueMarkerMode == cMultiValueMarkerModePies) {
				float dx = x - vp.screenX;
				float dy = y - vp.screenY;
				float a = (float)(Math.atan2(dy, dx) + Math.PI/2);	// 0 degrees is not in EAST, but in NORTH
				if (a < 0f)
					a += 2*Math.PI;
				int i = Math.min((int)(a * mMultiValueMarkerColumns.length / (2*Math.PI)), mMultiValueMarkerColumns.length-1);
				float distance = (float)Math.sqrt(dx*dx + dy*dy);
				float size = 0.5f  * vp.widthOrAngle1 * (float)Math.sqrt(Math.sqrt(mMultiValueMarkerColumns.length));
				float r = size * getMarkerSizeVPFactor(vp.record.getDouble(mMultiValueMarkerColumns[i]), mMultiValueMarkerColumns[i]);
				return Math.max(0f, distance-r);
				}
			else {
				float minDistance = Float.MAX_VALUE;
				float maxdx = (mMultiValueMarkerColumns.length*Math.max(2, Math.round(vp.widthOrAngle1 /(2f*(float)Math.sqrt(mMultiValueMarkerColumns.length))))+8)/2;
				float maxdy = Math.round(vp.heightOrAngle2 *2f)+4;
				if (Math.abs(x-vp.screenX) < maxdx && Math.abs(y-vp.screenY) < maxdy) {
					MultiValueBars mvbi = new MultiValueBars();
					mvbi.calculate(vp.widthOrAngle1, vp);
					for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
						float barX = mvbi.firstBarX+i*mvbi.barWidth;
						float dx = Math.max(0, (x < barX) ? barX-x : x-(barX+mvbi.barWidth));
						float dy = Math.max(0, (y < mvbi.barY[i]) ? mvbi.barY[i]-y : y-(mvbi.barY[i]+mvbi.barHeight[i]));
						float d = Math.max(dx, dy);
						if (minDistance > d)
							minDistance = d;
						}
					}
				return minDistance;
				}
			}

		return super.getDistanceToMarker(vp, x, y);
		}

	@Override
	protected float getMarkerWidth(VisualizationPoint p) {
		// Pie charts don't use this function because marker location is handled
		// by overwriting the findMarker() method.
		if (mChartType == cChartTypeBars
		 || (mChartType == cChartTypeBoxPlot && p.chartGroupIndex != -1))
			return getBarFractionSize(p, mChartInfo.barAxis == 0);
		else
			return getMarkerSize(p);
		}

	@Override
	protected float getMarkerHeight(VisualizationPoint p) {
		// Pie charts don't use this function because marker location is handled
		// by overwriting the findMarker() method.
		if (mChartType == cChartTypeBars
		 || (mChartType == cChartTypeBoxPlot && p.chartGroupIndex != -1))
			return getBarFractionSize(p, mChartInfo.barAxis == 1);
		else
			return getMarkerSize(p);
		}

	private float getBarFractionSize(VisualizationPoint p, boolean isInBarDirection) {
		if (mChartInfo == null) {
			return 0;
			}
		else if (isInBarDirection) {
			int cat = getChartCategoryIndex(p);
			return (cat == -1) ? 0 : mChartInfo.useProportionalFractions() ?
					  Math.abs(p.record.getDouble(mAxisIndex[mChartInfo.barAxis])) * mChartInfo.absValueFactor[p.hvIndex][cat]
					: mChartInfo.innerDistance[p.hvIndex][cat];
			}
		else {
			return mChartInfo.barWidth;
			}
		}

	public void initializeAxis(int axis) {
		super.initializeAxis(axis);

		mBackgroundValid = false;
		mScaleDepictor[axis] = null;
		}

	private void calculateNaNArea(int width, int height) {
		int size = Math.min((int)((NAN_WIDTH+NAN_SPACING) * mAbsoluteMarkerSize), Math.min(width, height) / 5);
		for (int axis=0; axis<mDimensions; axis++)
			mNaNSize[axis] = (!mShowNaNValues
							|| mAxisIndex[axis] == cColumnUnassigned
							|| mIsCategoryAxis[axis]
							|| mTableModel.isColumnDataComplete(mAxisIndex[axis])) ? 0 : size;
		}

	private void calculateScaleDimensions(Graphics2D g, int width, int height) {
		mScaleSize[0] = 0;
		mScaleSize[1] = 0;

		if (mScaleMode == cScaleModeHidden || mTreeNodeList != null)
			return;

		int[] minScaleSize = new int[2];	// minimum space needed near graph root to not truncate labels of other axis
		int[] usedScaleSize = new int[2];	// space needed for labels on this axis

		int scaledFontSize = (int)scaleIfSplitView(mFontHeight);
		for (int axis=1; axis>=0; axis--) {	// vertical axis first
			compileScaleLabels(axis, axis == 0 ? width : height);
			if (mScaleLineList[axis].isEmpty()) {	   // empty scale
				usedScaleSize[axis] = 0;

				// If we have an empty scale line because of too many categories,
				// then we need space for the crosshair label. We use the axis name area for it,
				// but text labels on the vertical axis and structure labels require more space:
/*		  		int column = mAxisIndex[axis];
				if (column != -1 && mTableModel.isColumnTypeCategory(column)) {
					if (mTableModel.isColumnTypeStructure(column)) {
						usedScaleSize[axis] = DEFAULT_STRUCTURE_SCALE_SIZE * scaledFontSize;
						}
					else if (axis == 1) {   crosshair labels on Y axis are now vertical and don't need additional space
						CategoryList list = mTableModel.getNativeCategoryList(column);
						if (mTableModel.isColumnTypeRangeCategory(column)) {
							String range = list.getString(list.getSize()-1);
							int index = range.indexOf(CompoundTableConstants.cRangeSeparation);
							if (index != -1) {
								usedScaleSize[axis] = Math.max(0, getStringWidth(range.substring(index
										+ CompoundTableConstants.cRangeSeparation.length())) - 2*scaledFontSize);
								}
							}
						else {
							for (int i=0; i<list.getSize(); i++) {
								String label = list.getString(i);
								int size = getStringWidth(label) - 2*scaledFontSize;
								if (usedScaleSize[axis] < size)
									usedScaleSize[axis] = size;
								}
							}
						}
					}*/
				}
			else if (mScaleDepictor[axis] != null) {	// molecules on scale
				int w = (axis == 0) ? width : height;
				int h = (axis == 0) ? height : width;
				int maxSize = (int)Math.min(0.70f*w/mScaleLineList[axis].size(), 0.25f*h);
				for (ScaleLine sl:mScaleLineList[axis]) {
					if (sl.label != null) {
						int maxAVBL = (int)(mRelativeFontSize * Depictor2D.cOptAvBondLen);
						if (mIsHighResolution)
							maxAVBL *= mFontScaling;

						Depictor2D d = (Depictor2D)sl.label;
						Font oldFont = g.getFont();
						d.validateView(g, new Rectangle2D.Double(0, 0, maxSize, maxSize), Depictor2D.cModeInflateToMaxAVBL + maxAVBL);
						int usedSize = (int)(axis==0 ? d.getBoundingRect().height: d.getBoundingRect().width);
						usedScaleSize[axis] = Math.max(usedScaleSize[axis], usedSize);
						g.setFont(oldFont);
						}
					}
				}
			else {
				int maxLabelSize = 0;
				for (int i=0; i<mScaleLineList[axis].size(); i++) {
					String label = (String)mScaleLineList[axis].get(i).label;
					int size = getStringWidth(label);
					if (maxLabelSize < size)
						maxLabelSize = size;
					}

				if (axis == 0) {
					// assume vertical scale to take 1/6 of total width
					int firstLabelWidth = (mScaleLineList[0].size() == 0) ? 0 : getStringWidth((String)mScaleLineList[0].get(0).label);
					int gridSize = (width - Math.max(mNaNSize[0] + usedScaleSize[1], firstLabelWidth / 2)) / mScaleLineList[0].size();
					int maxSizeWithPadding = maxLabelSize + scaledFontSize / 2;
					if (gridSize < 1.75f * scaledFontSize) {
						usedScaleSize[0] = maxLabelSize;
						mScaleTextMode[axis] = cScaleTextVertical;
						minScaleSize[1] = 0;
						}
					else if (maxSizeWithPadding > gridSize*2) {
						usedScaleSize[0] = (int)(0.71*(scaledFontSize+maxSizeWithPadding));
						mScaleTextMode[axis] = cScaleTextInclined;
						minScaleSize[1] = usedScaleSize[0]*4/5;
						}
					else if (maxSizeWithPadding > gridSize) {
						usedScaleSize[0] = 3*scaledFontSize;
						mScaleTextMode[axis] = cScaleTextAlternating;
						minScaleSize[1] = gridSize/4;
						}
					else {
						usedScaleSize[0] = scaledFontSize * 3/2;
						mScaleTextMode[axis] = cScaleTextNormal;
						minScaleSize[1] = 0;
						}
					}
				else {
					usedScaleSize[1] = Math.max(scaledFontSize, maxLabelSize);
					mScaleTextMode[1] = cScaleTextNormal;
					minScaleSize[0] = scaledFontSize / 2;
					}
				}
			}

		for (int axis=0; axis<2; axis++) {
			if (showScale(axis))
				mScaleSize[axis] = Math.max(minScaleSize[axis]-mNaNSize[1-axis], usedScaleSize[axis]);
			else
				mScaleSize[axis] = Math.max(minScaleSize[axis]-mNaNSize[1-axis], 0);

			int allowedMax = 2*((axis == 0 ? height : width)-mNaNSize[1-axis])/3;
			if (mScaleSize[axis] > allowedMax)
				mScaleSize[axis] = allowedMax;
			}
		}

	private void compileSplittingHeaderMolecules() {
		for (int i=0; i<2; i++) {
			if (mSplittingMolIndex[i] != mSplittingColumn[i])
				mSplittingDepictor[i] = null;

			mSplittingDepictor[i] = null;
			if (mSplittingColumn[i] >= 0) {
				if (mTableModel.isColumnTypeStructure(mSplittingColumn[i])
				 && mSplittingDepictor[i] == null) {
					String[] idcodeList = mShowEmptyInSplitView ? mTableModel.getCategoryList(mSplittingColumn[i])
																: getVisibleCategoryList(mSplittingColumn[i]);
					mSplittingDepictor[i] = new Depictor2D[idcodeList.length];
					mSplittingMolIndex[i] = mSplittingColumn[i];

					for (int j=0; j<idcodeList.length; j++) {
						String idcode = idcodeList[j];
						if (idcode.length() != 0) {
							int index = idcode.indexOf(' ');
							StereoMolecule mol = (index == -1) ?
										new IDCodeParser(true).getCompactMolecule(idcode)
									  : new IDCodeParser(true).getCompactMolecule(
																	idcode.substring(0, index),
																	idcode.substring(index+1));
							mSplittingDepictor[i][j] = new Depictor2D(mol, Depictor2D.cDModeSuppressChiralText);
							}
						}
					}
				}
			}
		}   

	private void updateScaleMolecules(int axis, int i1, int i2, int scaleSize) {
		String[] idcodeList = mTableModel.getCategoryList(mAxisIndex[axis]);
		if (mScaleDepictor[axis] == null) {
			mScaleDepictor[axis] = new Depictor2D[Math.min(maxDisplayedCategoryLabels(axis, scaleSize), idcodeList.length)];
			mScaleDepictorOffset[axis] = Math.min(i1, idcodeList.length-mScaleDepictor[axis].length);
			createScaleMolecules(axis, mScaleDepictorOffset[axis], mScaleDepictorOffset[axis]+mScaleDepictor[axis].length);
			}
		else if (i1 < mScaleDepictorOffset[axis]) {
			int shift = mScaleDepictorOffset[axis]-i1;
			for (int i=mScaleDepictor[axis].length-1; i>=shift; i--)
				mScaleDepictor[axis][i] = mScaleDepictor[axis][i-shift];
			mScaleDepictorOffset[axis] = i1;
			createScaleMolecules(axis, i1, i1+Math.min(shift, mScaleDepictor[axis].length));
			}
		else if (i2 > mScaleDepictorOffset[axis]+mScaleDepictor[axis].length) {
			int shift = i2-mScaleDepictorOffset[axis]-mScaleDepictor[axis].length;
			for (int i=0; i<mScaleDepictor[axis].length-shift; i++)
				mScaleDepictor[axis][i] = mScaleDepictor[axis][i+shift];
			mScaleDepictorOffset[axis] = i2-mScaleDepictor[axis].length;
			createScaleMolecules(axis, i2-Math.min(shift, mScaleDepictor[axis].length), i2);
			}
		}

	private void createScaleMolecules(int axis, int i1, int i2) {
		CategoryList<CategoryMolecule> list = (CategoryList<CategoryMolecule>)mTableModel.getNativeCategoryList(mAxisIndex[axis]);
		for (int i=i1; i<i2; i++) {
			StereoMolecule mol = list.get(i).getMolecule();
			if (mol != null)
				mScaleDepictor[axis][i-mScaleDepictorOffset[axis]] = new Depictor2D(mol, Depictor2D.cDModeSuppressChiralText);
			}
		}

	protected boolean isTextCategoryAxis(int axis) {
		return mAxisIndex[axis] != cColumnUnassigned
			&& !mTableModel.isColumnTypeDouble(mAxisIndex[axis])
			&& !mTableModel.isDescriptorColumn(mAxisIndex[axis]);
		}

	private void compileScaleLabels(int axis, int scaleSize) {
		mScaleLineList[axis].clear();
		if (mAxisIndex[axis] == cColumnUnassigned) {
			if (mChartType == cChartTypeBars && mChartInfo.barAxis == axis)
				compileDoubleScaleLabels(axis);
			}
		else {
			if (mIsCategoryAxis[axis])
				compileCategoryScaleLabels(axis, scaleSize);
			else
				compileDoubleScaleLabels(axis);
			}
		}

	private int maxDisplayedCategoryLabels(int axis, int scaleSize) {
		float minSpace = mTableModel.isColumnTypeStructure(mAxisIndex[axis]) ? cMinStructureLabelSpace : cMinTextLabelSpace;
		return Math.round(scaleSize / (minSpace * scaleIfSplitView(mFontHeight)));
		}

	private Object calculateDynamicScaleLabel(int axis, float position) {
		if (mAxisIndex[axis] == -1) {
			if (mChartType != cChartTypeBars || axis != mChartInfo.barAxis)
				return null;

			double v = mChartInfo.axisMin + position * (mChartInfo.axisMax - mChartInfo.axisMin);

			if (mChartMode != cChartModeCount
			 && mChartMode != cChartModePercent
			 && mTableModel.isLogarithmicViewMode(mChartColumn))
				v = Math.pow(10, v);
			else if (mChartMode == cChartModeCount
			 || mTableModel.isColumnTypeInteger(mChartColumn))
				return Long.toString(Math.round(v));

			return DoubleFormat.toString(v, 4, false);
			}

		double v = mAxisVisMin[axis] + position * (mAxisVisMax[axis] - mAxisVisMin[axis]);

		if (mIsCategoryAxis[axis]) {
			int index = Math.round((float)v);

			if (mScaleDepictor[axis] != null) {
				index -= mScaleDepictorOffset[axis];
				return index >= 0 && index < mScaleDepictor[axis].length ? mScaleDepictor[axis][index] : null;
				}
			else if (mTableModel.isColumnTypeStructure(mAxisIndex[axis])) {
				CategoryList<CategoryMolecule> list = (CategoryList<CategoryMolecule>)mTableModel.getNativeCategoryList(mAxisIndex[axis]);
				if (index < 0 || index >= list.getSize())
					return null;
				StereoMolecule mol = list.get(index).getMolecule();
				return mol == null ? null : new Depictor2D(mol, Depictor2D.cDModeSuppressChiralText);
				}

			String[] categoryList = mTableModel.getCategoryList(mAxisIndex[axis]);
			return index >= 0 && index < categoryList.length ? categoryList[index] : null;
			}

		if (mTableModel.isColumnTypeDate(mAxisIndex[axis]))
			return DateFormat.getDateInstance().format(new Date(86400000*Math.round(v)+43200000));

		if (mTableModel.isLogarithmicViewMode(mAxisIndex[axis]))
			v = Math.pow(10, v);

		if (mTableModel.isColumnTypeInteger(mAxisIndex[axis]))
			return Long.toString(Math.round(v));

		return DoubleFormat.toString(v, 4, false);
		}

	private void compileCategoryScaleLabels(int axis, int scaleSize) {
		if ((int)(mAxisVisMax[axis]-mAxisVisMin[axis]) > maxDisplayedCategoryLabels(axis, scaleSize))
			return;

		String[] categoryList = mTableModel.getCategoryList(mAxisIndex[axis]);
		int entireCategoryCount = categoryList.length;
		if (entireCategoryCount == 0)
			return;

		if (mTableModel.isColumnTypeRangeCategory(mAxisIndex[axis]) && !USE_FULL_RANGE_CATEGORY_SCALES) {
//		 && (mChartType != cChartTypeBars || axis != mChartInfo.barAxis)) {
			compileRangeCategoryScaleLabels(axis);
			return;
			}

		int min = Math.round(mAxisVisMin[axis] + 0.5001f);
		int max = Math.round(mAxisVisMax[axis] - 0.5001f);
		if (mTableModel.isColumnTypeStructure(mAxisIndex[axis]))
			updateScaleMolecules(axis, min, max+1, scaleSize);

		for (int i=min; i<=max; i++) {
			float scalePosition = (mChartType == cChartTypeBars && axis == mChartInfo.barAxis) ?
				(mChartInfo.barBase - mChartInfo.axisMin) / (mChartInfo.axisMax - mChartInfo.axisMin) - 0.5f + i : i;
			float position = (scalePosition - mAxisVisMin[axis]) / (mAxisVisMax[axis] - mAxisVisMin[axis]);
			if (mScaleDepictor[axis] == null)
				mScaleLineList[axis].add(new ScaleLine(position, categoryList[i]));
			else
				mScaleLineList[axis].add(new ScaleLine(position, mScaleDepictor[axis][i-mScaleDepictorOffset[axis]]));
			}
		}

	private void compileRangeCategoryScaleLabels(int axis) {
		String[] categoryList = mTableModel.getCategoryList(mAxisIndex[axis]);

		int min = Math.round(mAxisVisMin[axis] + 0.5001f);
		int max = Math.round(mAxisVisMax[axis] - 0.5001f);

		if (max >= min) {
			for (int i=min; i<=max+1; i++) {
				String category = categoryList[Math.min(i, max)];
				float position = ((float)i - 0.5f - mAxisVisMin[axis]) / (mAxisVisMax[axis] - mAxisVisMin[axis]);
				String label = "???";	// should not happen
				if (category == CompoundTableConstants.cRangeNotAvailable) {
					if (i == max+1)
						continue;
					label = "none";
					position = ((float)i - mAxisVisMin[axis]) / (mAxisVisMax[axis] - mAxisVisMin[axis]);
					}
				else {
					int index = category.indexOf(CompoundTableConstants.cRangeSeparation);
					if (index != -1)
						label = (i <= max) ? categoryList[i].substring(0, index)
								: categoryList[max].substring(index + CompoundTableConstants.cRangeSeparation.length());
					}
				mScaleLineList[axis].add(new ScaleLine(position, label));
				}
			}
		}

	private void compileDoubleScaleLabels(int axis) {
		float axisStart,axisLength,totalRange;

		if (mAxisIndex[axis] == -1) {	// bar axis of bar chart
			if (mChartMode != cChartModeCount
			 && mChartMode != cChartModePercent
			 && mTableModel.isLogarithmicViewMode(mChartColumn)) {
				compileLogarithmicScaleLabels(axis);
				return;
				}

			axisStart = mChartInfo.axisMin;
			axisLength = mChartInfo.axisMax - mChartInfo.axisMin;
			totalRange = axisLength;
			}
		else if (mTableModel.isDescriptorColumn(mAxisIndex[axis])) {
			axisStart = mAxisVisMin[axis];
			axisLength = mAxisVisMax[axis] - mAxisVisMin[axis];
			totalRange = 1.0f;
			}
		else {
			if (mTableModel.isLogarithmicViewMode(mAxisIndex[axis])) {
				compileLogarithmicScaleLabels(axis);
				return;
				}

			axisStart = mAxisVisMin[axis];
			axisLength = mAxisVisMax[axis] - mAxisVisMin[axis];
			totalRange = mTableModel.getMaximumValue(mAxisIndex[axis])
						- mTableModel.getMinimumValue(mAxisIndex[axis]);
			}

		if (axisLength == 0.0
		 || axisLength < totalRange/100000)
			return;

		int exponent = 0;
		while (axisLength >= 50.0) {
			axisStart /= 10;
			axisLength /= 10;
			exponent++;
			}
		while (axisLength < 5.0) {
			axisStart *= 10;
			axisLength *= 10.0;
			exponent--;
			}

		int gridSpacing = (int)(axisLength / 10);
		if (gridSpacing < 1)
			gridSpacing = 1;
		else if (gridSpacing < 2)
			gridSpacing = 2;
		else
			gridSpacing = 5;

		int theMarker = (axisStart < 0) ?
			  (int)(axisStart - 0.0000001 - (axisStart % gridSpacing))
			: (int)(axisStart + 0.0000001 + gridSpacing - (axisStart % gridSpacing));
		while ((float)theMarker < (axisStart + axisLength)) {
			float position = (theMarker-axisStart) / axisLength;

			if (mAxisIndex[axis] != -1 && mTableModel.isColumnTypeDate(mAxisIndex[axis])) {
				String label = createDateLabel(theMarker, exponent);
				if (label != null)
					mScaleLineList[axis].add(new ScaleLine(position, label));
				}
			else
				mScaleLineList[axis].add(new ScaleLine(position, DoubleFormat.toShortString(theMarker, exponent)));

			theMarker += gridSpacing;
			}
		}

	private void compileLogarithmicScaleLabels(int axis) {
		float axisStart,axisLength,totalRange;

		if (mAxisIndex[axis] == -1) {	// bar axis of bar chart
			axisStart = mChartInfo.axisMin;
			axisLength = mChartInfo.axisMax - mChartInfo.axisMin;
			totalRange = axisLength;
			}
		else {
			axisStart = mAxisVisMin[axis];
			axisLength = mAxisVisMax[axis] - axisStart;
			totalRange = mTableModel.getMaximumValue(mAxisIndex[axis])
						 - mTableModel.getMinimumValue(mAxisIndex[axis]);
			}

		if (axisLength == 0.0
		 || axisLength < totalRange/100000)
			return;

		int intMin = (int)Math.floor(axisStart);
		int intMax = (int)Math.floor(axisStart+axisLength);
		
		if (axisLength > 5.4) {
			int step = 1 + (int)axisLength/10;
			for (int i=intMin; i<=intMax; i+=step)
				addLogarithmicScaleLabel(axis, i);
			}
		else if (axisLength > 3.6) {
			for (int i=intMin; i<=intMax; i++) {
				addLogarithmicScaleLabel(axis, i);
				addLogarithmicScaleLabel(axis, i + 0.47712125472f);
				}
			}
		else if (axisLength > 1.8) {
			for (int i=intMin; i<=intMax; i++) {
				addLogarithmicScaleLabel(axis, i);
				addLogarithmicScaleLabel(axis, i + 0.301029996f);
				addLogarithmicScaleLabel(axis, i + 0.698970004f);
				}
			}
		else if (axisLength > 1.0) {
			for (int i=intMin; i<=intMax; i++) {
				addLogarithmicScaleLabel(axis, i);
				addLogarithmicScaleLabel(axis, i + 0.176091259f);
				addLogarithmicScaleLabel(axis, i + 0.301029996f);
				addLogarithmicScaleLabel(axis, i + 0.477121255f);
				addLogarithmicScaleLabel(axis, i + 0.698970004f);
				addLogarithmicScaleLabel(axis, i + 0.84509804f);
				}
			}
		else {
			float start = (float)Math.pow(10, axisStart);
			float length = (float)Math.pow(10, axisStart+axisLength) - start;

			int exponent = 0;
			while (length >= 50.0) {
				start /= 10;
				length /= 10;
				exponent++;
				}
			while (length < 5.0) {
				start *= 10;
				length *= 10.0;
				exponent--;
				}

			int gridSpacing = (int)(length / 10);
			if (gridSpacing < 1)
				gridSpacing = 1;
			else if (gridSpacing < 2)
				gridSpacing = 2;
			else
				gridSpacing = 5;

			int theMarker = (start < 0) ?
				  (int)(start - 0.0000001 - (start % gridSpacing))
				: (int)(start + 0.0000001 + gridSpacing - (start % gridSpacing));
			while ((float)theMarker < (start + length)) {
				float log = (float)Math.log10(theMarker) + exponent;
				float position = (log-axisStart) / axisLength;
				mScaleLineList[axis].add(new ScaleLine(position, DoubleFormat.toShortString(theMarker, exponent)));
				theMarker += gridSpacing;
				}
			}
		}

	private void addLogarithmicScaleLabel(int axis, float value) {
		float min = (mAxisIndex[axis] == -1) ? mChartInfo.axisMin : mAxisVisMin[axis];
		float max = (mAxisIndex[axis] == -1) ? mChartInfo.axisMax : mAxisVisMax[axis];
		if (value >= min && value <= max) {
			float position = (value-min) / (max - min);
			mScaleLineList[axis].add(new ScaleLine(position, DoubleFormat.toString(Math.pow(10, value), 3, true)));
			}
		}

	/**
	 * Needs to be called before validateLegend(), because the size legend depends on it.
	 */
	private void calculateMarkerSize(Rectangle bounds) {
		if (mChartType != cChartTypeBars && mChartType != cChartTypePies) {

			// With smaller views due to splitting we reduce size less than proportionally,
			// because individual views are much less crowded and relatively larger markers seem more natural.
			float splittingFactor = (float)Math.pow(mHVCount, 0.33);

			if (mChartType == cChartTypeBoxPlot || mChartType == cChartTypeWhiskerPlot) {
				float cellWidth = (mIsCategoryAxis[0]) ?
						Math.min((float)bounds.width / (float)getCategoryVisCount(0), (float)bounds.height / 5.0f)
					  : Math.min((float)bounds.height / (float)getCategoryVisCount(1), (float)bounds.width / 5.0f);
				mAbsoluteMarkerSize = mRelativeMarkerSize * cellWidth / (4.0f * splittingFactor * (float)Math.sqrt(mCaseSeparationCategoryCount));
				}
			else {
				mAbsoluteMarkerSize = mRelativeMarkerSize * cMarkerSize * (float)Math.sqrt(bounds.width * bounds.height) / splittingFactor;
				}

			mAbsoluteConnectionLineWidth = mRelativeConnectionLineWidth * cConnectionLineWidth
		 								 * (float)Math.sqrt(bounds.width * bounds.height) / splittingFactor;
			if (!Float.isNaN(mMarkerSizeZoomAdaption))
				mAbsoluteConnectionLineWidth *= mMarkerSizeZoomAdaption;
			}
		}

	private void calculateCoordinates(Graphics2D g, Rectangle bounds) {
		mBorder = (mScaleMode == cScaleModeHidden) ? 0 : Math.min(bounds.width, bounds.height)/40;

		// to ensure proper string width
		setFontHeightAndScaleToSplitView(mFontHeight);

		calculateNaNArea(bounds.width, bounds.height);
		calculateScaleDimensions(g, bounds.width, bounds.height);

		if (mChartType == cChartTypeScatterPlot
		 || mChartType == cChartTypeBoxPlot
		 || mChartType == cChartTypeWhiskerPlot
		 || mTreeNodeList != null) {
			Rectangle graphRect = getGraphBounds(bounds);

			float jitterMaxX = 0;
			float jitterMaxY = 0;
			if ((mMarkerJitteringAxes & 1) != 0)
				jitterMaxX = mMarkerJittering * graphRect.width / (mIsCategoryAxis[0] ? mAxisVisMax[0] - mAxisVisMin[0] : 5);
			if ((mMarkerJitteringAxes & 2) != 0)
				jitterMaxY = mMarkerJittering * graphRect.height / (mIsCategoryAxis[1] ? mAxisVisMax[1] - mAxisVisMin[1] : 5);

			if (mTreeNodeList != null) {
				if (mTreeNodeList.length != 0)
					calculateTreeCoordinates(graphRect);
				mBackgroundValid = false;

/*				if (mMarkerJittering > 0.0) {	// don't jitter trees
					for (int i=0; i<mDataPoints; i++) {
						mPoint[i].screenX += (mRandom.nextDouble() - 0.5) * jitterMaxX;
						mPoint[i].screenY += (mRandom.nextDouble() - 0.5) * jitterMaxY;
						}
					}*/
				}
			else {
				float csCategoryWidth = 0;
				float csOffset = 0;
				int csAxis = getCaseSeparationAxis();
				if (csAxis != -1) {
					float width = csAxis == 0 ? graphRect.width : graphRect.height;
					float categoryWidth = (mAxisIndex[csAxis] == cColumnUnassigned) ? width
										 : width / (mAxisVisMax[csAxis]-mAxisVisMin[csAxis]);	// mCategoryCount[csAxis]; 	mCategoryCount is undefined for scatter plots
					categoryWidth *= mCaseSeparationValue;
					float csCategoryCount = mTableModel.getCategoryCount(mCaseSeparationColumn);
					csCategoryWidth = categoryWidth / csCategoryCount;
					csOffset = (csCategoryWidth - categoryWidth) / 2;
					}

				int xNaN = Math.round(graphRect.x - mNaNSize[0] * (0.5f * NAN_WIDTH + NAN_SPACING) / (NAN_WIDTH + NAN_SPACING));
				int yNaN = Math.round(graphRect.y + graphRect.height + mNaNSize[1] * (0.5f * NAN_WIDTH + NAN_SPACING) / (NAN_WIDTH + NAN_SPACING));
				if (mChartType == cChartTypeScatterPlot) {
					for (int i=0; i<mDataPoints; i++) {
						// calculating coordinates for invisible records also allows to skip coordinate recalculation
						// when the visibility changes (JVisualization3D uses the inverse approach)
						float doubleX = (mAxisIndex[0] == cColumnUnassigned) ? 0.0f : getAxisValue(mPoint[i].record, 0);
						float doubleY = (mAxisIndex[1] == cColumnUnassigned) ? 0.0f : getAxisValue(mPoint[i].record, 1);
						mPoint[i].screenX = Float.isNaN(doubleX) ? xNaN : graphRect.x
										  + (doubleX-mAxisVisMin[0])*graphRect.width / (mAxisVisMax[0]-mAxisVisMin[0]);
						mPoint[i].screenY = Float.isNaN(doubleY) ? yNaN : graphRect.y + graphRect.height
										  + (mAxisVisMin[1]-doubleY)*graphRect.height / (mAxisVisMax[1]-mAxisVisMin[1]);
						if (jitterMaxX != 0)
							mPoint[i].screenX += (mRandom.nextDouble() - 0.5) * jitterMaxX;
						if (jitterMaxY != 0)
							mPoint[i].screenY += (mRandom.nextDouble() - 0.5) * jitterMaxY;

						if (csAxis != -1) {
							float csShift = csOffset + csCategoryWidth * mTableModel.getCategoryIndex(mCaseSeparationColumn, mPoint[i].record);
							if (csAxis == 0)
								mPoint[i].screenX += csShift;
							else
								mPoint[i].screenY -= csShift;
							}
						}
					}
				else {	// mChartType == cChartTypeBoxPlot or cChartTypeWhiskerPlot
					boolean xIsDoubleCategory = mChartInfo.barAxis == 1
											 && mAxisIndex[0] != cColumnUnassigned
											 && mTableModel.isColumnTypeDouble(mAxisIndex[0]);
					boolean yIsDoubleCategory = mChartInfo.barAxis == 0
											 && mAxisIndex[1] != cColumnUnassigned
											 && mTableModel.isColumnTypeDouble(mAxisIndex[1]);
					for (int i=0; i<mDataPoints; i++) {
						if (mChartType == cChartTypeWhiskerPlot
						 || mPoint[i].chartGroupIndex == -1) {
							if (mAxisIndex[0] == cColumnUnassigned)
								mPoint[i].screenX = graphRect.x + graphRect.width * 0.5f;
							else if (xIsDoubleCategory)
								mPoint[i].screenX = graphRect.x + graphRect.width
											* (0.5f + getCategoryIndex(0, mPoint[i])) / getCategoryVisCount(0);
							else {
								float doubleX = getAxisValue(mPoint[i].record, 0);
								mPoint[i].screenX = Float.isNaN(doubleX) ? xNaN : graphRect.x
										  + (doubleX-mAxisVisMin[0])*graphRect.width / (mAxisVisMax[0]-mAxisVisMin[0]);							}
							if (mAxisIndex[1] == cColumnUnassigned)
								mPoint[i].screenY = graphRect.y + graphRect.height * 0.5f;
							else if (yIsDoubleCategory)
								mPoint[i].screenY = graphRect.y + graphRect.height - graphRect.height
											* (0.5f + getCategoryIndex(1, mPoint[i])) / getCategoryVisCount(1);
							else {
								float doubleY = getAxisValue(mPoint[i].record, 1);
								mPoint[i].screenY = Float.isNaN(doubleY) ? yNaN : graphRect.y + graphRect.height
										  + (mAxisVisMin[1]-doubleY)*graphRect.height / (mAxisVisMax[1]-mAxisVisMin[1]);
								}

							if (jitterMaxX != 0)
								mPoint[i].screenX += (mRandom.nextDouble() - 0.5) * jitterMaxX;
							if (jitterMaxY != 0)
								mPoint[i].screenY += (mRandom.nextDouble() - 0.5) * jitterMaxY;

							if (csAxis != -1) {
								float csShift = csOffset + csCategoryWidth * mTableModel.getCategoryIndex(mCaseSeparationColumn, mPoint[i].record);
								if (csAxis == 0)
									mPoint[i].screenX += csShift;
								else
									mPoint[i].screenY -= csShift;
								}
							}
						}
					}
				}

			addSplittingOffset();
			}

		mCurveY = null;

	   	mCoordinatesValid = true;
		}

	private void calculateTreeCoordinates(Rectangle graphRect) {
		if (mTreeViewMode == cTreeViewModeRadial) {
			float zoomFactor = (!mTreeViewShowAll || Float.isNaN(mMarkerSizeZoomAdaption)) ? 1f : mMarkerSizeZoomAdaption;
			float preferredMarkerDistance = 4*mAbsoluteMarkerSize*zoomFactor;
			RadialGraphOptimizer.optimizeCoordinates(graphRect, mTreeNodeList, preferredMarkerDistance);
			return;
			}
		if (mTreeViewMode == cTreeViewModeTopRoot || mTreeViewMode == cTreeViewModeBottomRoot) {
			int maxLayerDistance = graphRect.height / 4;
			int maxNeighborDistance = graphRect.width / 8;
			TreeGraphOptimizer.optimizeCoordinates(graphRect, mTreeNodeList, false, mTreeViewMode == cTreeViewModeBottomRoot, maxLayerDistance, maxNeighborDistance);
			}
		if (mTreeViewMode == cTreeViewModeLeftRoot || mTreeViewMode == cTreeViewModeRightRoot) {
			int maxLayerDistance = graphRect.width / 4;
			int maxNeighborDistance = graphRect.height / 8;
			TreeGraphOptimizer.optimizeCoordinates(graphRect, mTreeNodeList, true, mTreeViewMode == cTreeViewModeRightRoot, maxLayerDistance, maxNeighborDistance);
			}
		}

	private void addSplittingOffset() {
		if (isSplitView()) {
			int gridWidth = mSplitter.getGridWidth();
			int gridHeight = mSplitter.getGridHeight();
			for (int i=0; i<mDataPoints; i++) {
				if (mChartType == cChartTypeScatterPlot
				 || mChartType == cChartTypeWhiskerPlot
				 || mPoint[i].chartGroupIndex == -1) {
					int hIndex = mSplitter.getHIndex((int)mPoint[i].hvIndex);
					int vIndex = mSplitter.getVIndex((int)mPoint[i].hvIndex);
					mPoint[i].screenX += hIndex * gridWidth;
					mPoint[i].screenY += vIndex * gridHeight;
					}
				}
			}
		}

	/**
	 * Calculates the background color array for all split views.
	 * @param graphBounds used in case of tree view only
	 */
	private void calculateBackground(Rectangle graphBounds, boolean transparentBG) {
		int backgroundSize = (int)(480.0 - 120.0 * Math.log(mBackgroundColorRadius));
		int backgroundColorRadius = 2*mBackgroundColorRadius;
		if (mIsHighResolution) {
			backgroundSize *= 2;
			backgroundColorRadius *= 2;
			}

		if (mSplitter == null) {
			mBackgroundHCount = 1;
			mBackgroundVCount = 1;
			}
		else {
			backgroundSize *= 2;
			backgroundColorRadius *= 2;
			mBackgroundHCount = mSplitter.getHCount();
			mBackgroundVCount = mSplitter.getVCount();
			}
		int backgroundWidth = backgroundSize / mBackgroundHCount;
		int backgroundHeight = backgroundSize / mBackgroundVCount;

			// add all points' RGB color components to respective grid cells
			// consider all points that are less than backgroundColorRadius away from visible area
		float[][][] backgroundR = new float[mHVCount][backgroundWidth][backgroundHeight];
		float[][][] backgroundG = new float[mHVCount][backgroundWidth][backgroundHeight];
		float[][][] backgroundB = new float[mHVCount][backgroundWidth][backgroundHeight];
		float[][][] backgroundC = new float[mHVCount][backgroundWidth][backgroundHeight];

		float xMin,xMax,yMin,yMax;
		if (mTreeNodeList != null) {
			xMin = graphBounds.x;
			yMin = graphBounds.y + graphBounds.height;
			xMax = graphBounds.x + graphBounds.width;
			yMax = graphBounds.y;
			}
		else {
			if (mAxisIndex[0] == cColumnUnassigned) {
				xMin = mAxisVisMin[0];
				xMax = mAxisVisMax[0];
				}
			else if (mIsCategoryAxis[0]) {
				xMin = -0.5f;
				xMax = -0.5f + mTableModel.getCategoryCount(mAxisIndex[0]);
				}
			else {
				float[] minAndMax = getDataMinAndMax(0);
				xMin = minAndMax[0];
				xMax = minAndMax[1];
//				xMin = mTableModel.getMinimumValue(mAxisIndex[0]);
//				xMax = mTableModel.getMaximumValue(mAxisIndex[0]);
				}
	
			if (mAxisIndex[1] == cColumnUnassigned) {
				yMin = mAxisVisMin[1];
				yMax = mAxisVisMax[1];
				}
			else if (mIsCategoryAxis[1]) {
				yMin = -0.5f;
				yMax = -0.5f + mTableModel.getCategoryCount(mAxisIndex[1]);
				}
			else {
				float[] minAndMax = getDataMinAndMax(1);
				yMin = minAndMax[0];
				yMax = minAndMax[1];
//				yMin = mTableModel.getMinimumValue(mAxisIndex[1]);
//				yMax = mTableModel.getMaximumValue(mAxisIndex[1]);
				}
			}

		Color neutralColor = transparentBG ? Color.BLACK : getViewBackground();
		int neutralR = neutralColor.getRed();
		int neutralG = neutralColor.getGreen();
		int neutralB = neutralColor.getBlue();

		float rangeX = xMax - xMin;
		float rangeY = yMax - yMin;
		boolean considerVisibleRecords = (mBackgroundColorConsidered == BACKGROUND_VISIBLE_RECORDS) || (mTreeNodeList != null);
		boolean considerAllRecords = (mBackgroundColorConsidered == BACKGROUND_ALL_RECORDS && !considerVisibleRecords);
		int listFlagNo = (considerVisibleRecords || considerAllRecords) ? -1
						: mTableModel.getListHandler().getListFlagNo(mBackgroundColorConsidered);
		for (int i=0; i<mDataPoints; i++) {
			if (considerAllRecords
			 || (considerVisibleRecords && isVisibleExcludeNaN(mPoint[i]))
			 || (!considerVisibleRecords && mPoint[i].record.isFlagSet(listFlagNo)))	{
				float valueX;
				float valueY;
				if (mTreeNodeList != null) {
					valueX = mPoint[i].screenX;
					valueY = mPoint[i].screenY;
					}
				else {
					valueX = (mAxisIndex[0] == cColumnUnassigned) ? (xMin + xMax) / 2 : getAxisValue(mPoint[i].record, 0);
					valueY = (mAxisIndex[1] == cColumnUnassigned) ? (yMin + yMax) / 2 : getAxisValue(mPoint[i].record, 1);
					}
							  
				if (Float.isNaN(valueX) || Float.isNaN(valueY))
					continue;

				int x = Math.min(backgroundWidth-1, (int)(backgroundWidth * (valueX - xMin) / rangeX));
				int y = Math.min(backgroundHeight-1, (int)(backgroundHeight * (valueY - yMin) / rangeY));

				Color c = mBackgroundColor.getColorList()[((VisualizationPoint2D)mPoint[i]).backgroundColorIndex];
				backgroundR[mPoint[i].hvIndex][x][y] += c.getRed() - neutralR;
				backgroundG[mPoint[i].hvIndex][x][y] += c.getGreen() - neutralG;
				backgroundB[mPoint[i].hvIndex][x][y] += c.getBlue() - neutralB;
				backgroundC[mPoint[i].hvIndex][x][y] += 1.0;	// simply counts individual colors added
				}
			}

			// propagate colors to grid neighbourhood via cosine function
		float[][] influence = new float[backgroundColorRadius][backgroundColorRadius];
		for (int x=0; x<backgroundColorRadius; x++) {
			for (int y=0; y<backgroundColorRadius; y++) {
				float distance = (float)Math.sqrt(x*x + y*y);
				if (distance < backgroundColorRadius)
					influence[x][y] = (float)(0.5 + Math.cos(Math.PI*distance/(float)backgroundColorRadius) / 2.0);
				}
			}
		float[][][] smoothR = new float[mHVCount][backgroundWidth][backgroundHeight];
		float[][][] smoothG = new float[mHVCount][backgroundWidth][backgroundHeight];
		float[][][] smoothB = new float[mHVCount][backgroundWidth][backgroundHeight];
		float[][][] smoothC = new float[mHVCount][backgroundWidth][backgroundHeight];
		boolean xIsCyclic = (mAxisIndex[0] == cColumnUnassigned) ? false
									: (mTableModel.getColumnProperty(mAxisIndex[0],
										CompoundTableModel.cColumnPropertyCyclicDataMax) != null);
		boolean yIsCyclic = (mAxisIndex[1] == cColumnUnassigned) ? false
									: (mTableModel.getColumnProperty(mAxisIndex[1],
										CompoundTableModel.cColumnPropertyCyclicDataMax) != null);
		for (int x=0; x<backgroundWidth; x++) {
			int xmin = x-backgroundColorRadius+1;
			if (xmin < 0 && !xIsCyclic)
				xmin = 0;
			int xmax = x+backgroundColorRadius-1;
			if (xmax >= backgroundWidth && !xIsCyclic)
				xmax = backgroundWidth-1;

			for (int y=0; y<backgroundHeight; y++) {
				int ymin = y-backgroundColorRadius+1;
				if (ymin < 0 && !yIsCyclic)
					ymin = 0;
				int ymax = y+backgroundColorRadius-1;
				if (ymax >= backgroundHeight && !yIsCyclic)
					ymax = backgroundHeight-1;
	
				for (int hv=0; hv<mHVCount; hv++) {
					if (backgroundC[hv][x][y] > (float)0.0) {
						for (int ix=xmin; ix<=xmax; ix++) {
							int dx = Math.abs(x-ix);
	
							int destX = ix;
							if (destX < 0)
								destX += backgroundWidth;
							else if (destX >= backgroundWidth)
								destX -= backgroundWidth;
	
							for (int iy=ymin; iy<=ymax; iy++) {
								int dy = Math.abs(y-iy);
	
								int destY = iy;
								if (destY < 0)
									destY += backgroundHeight;
								else if (destY >= backgroundHeight)
									destY -= backgroundHeight;
	
								if (influence[dx][dy] > (float)0.0) {
									smoothR[hv][destX][destY] += influence[dx][dy] * backgroundR[hv][x][y];
									smoothG[hv][destX][destY] += influence[dx][dy] * backgroundG[hv][x][y];
									smoothB[hv][destX][destY] += influence[dx][dy] * backgroundB[hv][x][y];
									smoothC[hv][destX][destY] += influence[dx][dy] * backgroundC[hv][x][y];
									}
								}
							}
						}
					}
				}
			}

			// find highest sum of RGB components
		float max = (float)0.0;
		for (int hv=0; hv<mHVCount; hv++)
			for (int x=0; x<backgroundWidth; x++)
				for (int y=0; y<backgroundHeight; y++)
					if (max < smoothC[hv][x][y])
						max = smoothC[hv][x][y];

		float fading = (float)Math.exp(Math.log(1.0)-(float)mBackgroundColorFading/20*(Math.log(1.0)-Math.log(0.1)));

		mBackground = new Color[mHVCount][backgroundWidth][backgroundHeight];
		for (int hv=0; hv<mHVCount; hv++) {
			for (int x=0; x<backgroundWidth; x++) {
				for (int y=0; y<backgroundHeight; y++) {
					if (smoothC[hv][x][y] == 0) {
						mBackground[hv][x][y] = transparentBG ? new Color(1f, 1f, 1f, 0f) : neutralColor;
						}
					else {
						float f = (float)Math.exp(fading*Math.log(smoothC[hv][x][y] / max));
						if (transparentBG) {
							mBackground[hv][x][y] = new Color((int) (smoothR[hv][x][y] / smoothC[hv][x][y]),
									(int) (smoothG[hv][x][y] / smoothC[hv][x][y]),
									(int) (smoothB[hv][x][y] / smoothC[hv][x][y]),
									(int) (f * 255));
							}
						else {
							f /= smoothC[hv][x][y];
							mBackground[hv][x][y] = new Color(neutralR + (int) (f * smoothR[hv][x][y]),
															  neutralG + (int) (f * smoothG[hv][x][y]),
															  neutralB + (int) (f * smoothB[hv][x][y]));
							}
						}
					}
				}
			}

		mBackgroundValid = true;
		}

	private void drawBackground(Graphics2D g, Rectangle graphRect, int hvIndex) {
		ViewPort port = new ViewPort();

		if (hasColorBackground()) {
			int backgroundWidth = mBackground[0].length;
			int backgroundHeight = mBackground[0][0].length;
	
			int[] x = new int[backgroundWidth+1];
			int[] y = new int[backgroundHeight+1];
	
			float factorX = (float)graphRect.width/port.getVisRangle(0);
			float factorY = (float)graphRect.height/port.getVisRangle(1);
	
			int minxi = 0;
			int maxxi = backgroundWidth;
			int minyi = 0;
			int maxyi = backgroundHeight;
			for (int i=0; i<=backgroundWidth; i++) {
				float axisX = port.min[0]+i*port.getRange(0)/backgroundWidth;
				x[i] = graphRect.x + (int)(factorX*(axisX-port.visMin[0]));
				if (x[i] <= graphRect.x) {
					x[i] = graphRect.x;
					minxi = i;
					}
				if (x[i] >= graphRect.x+graphRect.width) {
					x[i] = graphRect.x+graphRect.width;
					maxxi = i;
					break;
					}
				}
			for (int i=0; i<=backgroundHeight; i++) {
	 			float axisY = port.min[1]+i*port.getRange(1)/backgroundHeight;
				y[i] = graphRect.y+graphRect.height - (int)(factorY*(axisY-port.visMin[1]));
				if (y[i] >= graphRect.y+graphRect.height) {
					y[i] = graphRect.y+graphRect.height;
					minyi = i;
					}
				if (y[i] <= graphRect.y) {
					y[i] = graphRect.y;
					maxyi = i;
					break;
					}
				}
			for (int xi=minxi; xi<maxxi; xi++) {
				for (int yi=minyi; yi<maxyi; yi++) {
					g.setColor(mBackground[hvIndex][xi][yi]);
					g.fillRect(x[xi], y[yi+1], x[xi+1]-x[xi], y[yi]-y[yi+1]);
					}
				}
			}

		if (mBackgroundImage != null) {
			int sx1 = Math.round((float)mBackgroundImage.getWidth()*(port.visMin[0]-port.min[0])/port.getRange(0));
			int sx2 = Math.round((float)mBackgroundImage.getWidth()*(port.visMax[0]-port.min[0])/port.getRange(0));
			int sy1 = Math.round((float)mBackgroundImage.getHeight()*(port.max[1]-port.visMax[1])/port.getRange(1));
			int sy2 = Math.round((float)mBackgroundImage.getHeight()*(port.max[1]-port.visMin[1])/port.getRange(1));
			if (sx1 < sx2 && sy1 < sy2)
				g.drawImage(mBackgroundImage, graphRect.x, graphRect.y,
											  graphRect.x+graphRect.width, graphRect.y+graphRect.height,
											  sx1, sy1, sx2, sy2, null);
			}
		}

	private void drawNaNArea(Graphics2D g, Rectangle graphRect) {
		mG.setColor(getContrastGrey(0.1f));
		int xNaNSpace = Math.round(mNaNSize[0] * NAN_SPACING / (NAN_WIDTH + NAN_SPACING));
		int yNaNSpace = Math.round(mNaNSize[1] * NAN_SPACING / (NAN_WIDTH + NAN_SPACING));
		if (mNaNSize[0] != 0)
			mG.fillRect(graphRect.x - mNaNSize[0], graphRect.y, mNaNSize[0] - xNaNSpace, graphRect.height + mNaNSize[1]);
		if (mNaNSize[1] != 0)
			mG.fillRect(graphRect.x - mNaNSize[0], graphRect.y + graphRect.height + yNaNSpace, graphRect.width + mNaNSize[0], mNaNSize[1] - yNaNSpace);
		}

	private void drawAxes(Graphics2D g, Rectangle graphRect) {
		if (mScaleStyle == cScaleStyleArrows) {
			g.setStroke(mNormalLineStroke);
			g.setColor(getContrastGrey(SCALE_STRONG));
			}
		else {
			g.setStroke(mFatLineStroke);
			g.setColor(getContrastGrey(SCALE_MEDIUM));
			}

		int xmin = graphRect.x;
		int xmax = graphRect.x+graphRect.width;
		int ymin = graphRect.y;
		int ymax = graphRect.y+graphRect.height;

		int arrowSize = (mScaleStyle == cScaleStyleArrows) ? (int)(ARROW_TIP_SIZE*scaleIfSplitView(mFontHeight)) : 0;
		int[] px = new int[3];
		int[] py = new int[3];
		if (showScale(0)
		 && (mAxisIndex[0] != cColumnUnassigned
		  || (mChartType == cChartTypeBars && mChartInfo.barAxis == 0))) {
			g.drawLine(xmin, ymax, xmax, ymax);
			if (mScaleStyle == cScaleStyleFrame)
				g.drawLine(xmin, ymin, xmax, ymin);

			if (mScaleStyle == cScaleStyleArrows) {
				px[0] = xmax;
				py[0] = ymax - arrowSize / 3;
				px[1] = xmax;
				py[1] = ymax + arrowSize / 3;
				px[2] = xmax + arrowSize;
				py[2] = ymax;
				g.fillPolygon(px, py, 3);
				}

			String label = (mAxisIndex[0] != cColumnUnassigned) ? getAxisTitle(mAxisIndex[0])
					: mChartMode == cChartModeCount ? "Count"
					: mChartMode == cChartModePercent ? "Percent"
					: CHART_MODE_AXIS_TEXT[mChartMode]+"("+mTableModel.getColumnTitle(mChartColumn)+")";
			if (mScaleTitleCentered) {
				g.drawString(label,
						xmax-(xmax-xmin)/2-g.getFontMetrics().stringWidth(label)/2,
						ymax+mScaleSize[0]+mNaNSize[1]+AXIS_TEXT_PADDING*scaleIfSplitView(mFontHeight)+g.getFontMetrics().getAscent());
				}
			else {
				g.drawString(label,
						xmax-g.getFontMetrics().stringWidth(label),
						ymax+mScaleSize[0]+mNaNSize[1]+g.getFontMetrics().getAscent());
				}
			}

		if (showScale(1)
		 && (mAxisIndex[1] != cColumnUnassigned
		  || (mChartType == cChartTypeBars && mChartInfo.barAxis == 1))) {
			g.drawLine(xmin, ymax, xmin, ymin);
			if (mScaleStyle == cScaleStyleFrame)
				g.drawLine(xmax, ymax, xmax, ymin);

			if (mScaleStyle == cScaleStyleArrows) {
				px[0] = xmin - arrowSize / 3;
				py[0] = ymin;
				px[1] = xmin + arrowSize / 3;
				py[1] = ymin;
				px[2] = xmin;
				py[2] = ymin - arrowSize;
				g.fillPolygon(px, py, 3);
				}

			String label = (mAxisIndex[1] != cColumnUnassigned) ? getAxisTitle(mAxisIndex[1])
					: mChartMode == cChartModeCount ? "Count"
					: mChartMode == cChartModePercent ? "Percent"
					: CHART_MODE_AXIS_TEXT[mChartMode]+"("+mTableModel.getColumnTitle(mChartColumn)+")";
			if (mScaleTitleCentered) {
				double labelX = xmin - mScaleSize[1] - mNaNSize[0] - AXIS_TEXT_PADDING * scaleIfSplitView(mFontHeight) - g.getFontMetrics().getDescent();
				double labelY = ymin + (ymax - ymin) / 2;
				AffineTransform oldTransform = g.getTransform();
				g.rotate(-Math.PI / 2, labelX, labelY);
				g.drawString(label, (int)labelX - g.getFontMetrics().stringWidth(label)/2, (int)labelY);
				g.setTransform(oldTransform);
				}
			else {
				int labelX = xmin - arrowSize - g.getFontMetrics().stringWidth(label);
				if (labelX < xmin - mBorder * 2 / 3 - mScaleSize[1] - mNaNSize[0])
					labelX = xmin + arrowSize;
				g.drawString(label, labelX, ymin - g.getFontMetrics().getDescent());
				}
			}
		}

	private void drawGrid(Graphics2D g, Rectangle graphRect) {
		for (int axis=0; axis<2; axis++)
			for (int i=0; i<mScaleLineList[axis].size(); i++)
				drawScaleLine(g, graphRect, axis, i);
		}

	private void drawScaleLine(Graphics2D g, Rectangle graphRect, int axis, int index) {
		ScaleLine scaleLine = mScaleLineList[axis].get(index);
		drawScaleLine(g, graphRect, axis, scaleLine.label, scaleLine.position, false, (index & 1) == 1);
		}

	private void drawScaleLine(Graphics2D g, Rectangle graphRect, int axis, Object label, float position, boolean isCrossHair, boolean isShifted) {
		int scaledFontHeight = Math.round(scaleIfSplitView(mFontHeight));
		int crossHairBorder = Math.round(CROSSHAIR_LABEL_BORDER * scaledFontHeight);
		g.setStroke(isCrossHair ? mNormalLineStroke : mThinLineStroke);
		if (axis == 0) {	// X-axis
			int axisPosition = graphRect.x + Math.round(graphRect.width*position);
			int yBase = graphRect.y+graphRect.height+mNaNSize[1];

			if (isCrossHair || mGridMode == cGridModeShown || mGridMode == cGridModeShowVertical) {
				g.setColor(getContrastGrey(SCALE_LIGHT));
				g.drawLine(axisPosition, graphRect.y, axisPosition, yBase);
				}
			else if (showScale(axis)) {
				g.setColor(getContrastGrey(SCALE_LIGHT));
				g.drawLine(axisPosition, graphRect.y+graphRect.height, axisPosition, graphRect.y+graphRect.height+scaledFontHeight/6);
				}

			if (label != null) {
				if (showScale(axis)) {
					if (label instanceof String) {
						String text = ((String)label).trim();
						if (!isCrossHair && mScaleTextMode[axis] == cScaleTextVertical) {
							int textX = axisPosition+scaledFontHeight/3; // middle to assent
							int textY = yBase+scaledFontHeight/3; // pading
							g.rotate(-Math.PI/2, textX, textY);
							int labelWidth = g.getFontMetrics().stringWidth(text);  // stringWidth() is different after rotation!!!
							drawScaleLabel(g, text, textX-labelWidth, textY, isCrossHair);
							g.rotate(Math.PI/2, textX, textY);
							}
						else if (!isCrossHair && mScaleTextMode[axis] == cScaleTextInclined) {
							int labelWidth = g.getFontMetrics().stringWidth(text);
							int textX = axisPosition+(int)(scaledFontHeight/3 - 0.71*labelWidth);
							int textY = yBase+(int)(0.71*(scaledFontHeight+labelWidth));
							g.rotate(-Math.PI/4, textX, textY);
							drawScaleLabel(g, text, textX, textY, isCrossHair);
							g.rotate(Math.PI/4, textX, textY);
							}
						else {
							int labelWidth = g.getFontMetrics().stringWidth(text);
							int x = axisPosition - labelWidth/2;
							int yShift = ((mScaleTextMode[axis] == cScaleTextAlternating && isShifted)) ?
									scaledFontHeight * 11 / 4 : scaledFontHeight * 5/4;
							if (isCrossHair)
								x = Math.max(graphRect.x-mScaleSize[1]-mNaNSize[0]-mBorder-scaledFontHeight,
									Math.min(graphRect.x+graphRect.width+mBorder-labelWidth-crossHairBorder, x));
							drawScaleLabel(g, text, x, yBase+yShift, isCrossHair);
							}
						}
					else {
						drawScaleMolecule(g, graphRect, axis, position, (Depictor2D)label, isCrossHair);
						}
					}
				else if (isCrossHair) { // without a scale, we show the crosshair label within the graph area
					if (label instanceof String) {
						String text = (String)label;
						int labelWidth = g.getFontMetrics().stringWidth(text);
						int x = Math.max(graphRect.x+crossHairBorder,
								Math.min(graphRect.x+graphRect.width-labelWidth-crossHairBorder,
										 axisPosition-labelWidth/2));
						int y = mLegendList.size() == 0 ? yBase-scaledFontHeight/4-crossHairBorder : yBase+scaledFontHeight+crossHairBorder;
						drawScaleLabel(g, text, x, y, isCrossHair);
						}
					else {
						drawScaleMolecule(g, graphRect, axis, position, (Depictor2D)label, isCrossHair);
						}
					}
				}
			}
		else {  // Y-axis
			int axisPosition = graphRect.y+graphRect.height - Math.round(graphRect.height*position);
			int xBase = graphRect.x-mNaNSize[0];

			if (isCrossHair || mGridMode == cGridModeShown || mGridMode == cGridModeShowHorizontal) {
				g.setColor(getContrastGrey(SCALE_LIGHT));
				g.drawLine(xBase, axisPosition, graphRect.x+graphRect.width, axisPosition);
				}
			else if (showScale(axis)) {
				g.setColor(getContrastGrey(SCALE_LIGHT));
				g.drawLine(graphRect.x-scaledFontHeight/6, axisPosition, graphRect.x, axisPosition);
				}

			if (label != null && (showScale(axis) || isCrossHair)) {
				if (label instanceof String) {
					String text = (String)label;
					int textWidth = g.getFontMetrics().stringWidth(text);
					boolean rotateLabel = isCrossHair
							&& (showScale(axis) && (textWidth > mScaleSize[axis]))
							 || (!showScale(axis) && (textWidth > 8*scaledFontHeight));
					if (isCrossHair && (!showScale(axis) || rotateLabel)) {
						// If the label is too long to show horizontally, then rotate.
						// (e.g. full range category label on short range type scale)
						if (rotateLabel) {
							int x = showScale(axis) ? xBase-scaledFontHeight/2 : xBase+scaledFontHeight+crossHairBorder;
							int y = graphRect.y+graphRect.height;
							g.rotate(-Math.PI/2, x, y);
							textWidth = g.getFontMetrics().stringWidth(text);   // differs from not rotated font
							int textX = Math.max(x+crossHairBorder,
										Math.min(x+graphRect.height-textWidth-2*crossHairBorder+graphRect.y,
												 x+graphRect.height-axisPosition-textWidth/2+graphRect.y));
							drawScaleLabel(g, text, textX, y, isCrossHair);
							g.rotate(Math.PI/2, x, y);
							}
						else {
							int x = xBase + crossHairBorder;
							int y = Math.max(graphRect.y+scaledFontHeight,
									Math.min(graphRect.y+graphRect.height-crossHairBorder,
											axisPosition+scaledFontHeight/3));
							drawScaleLabel(g, text, x, y, isCrossHair);
							}
						}
					else {
						int x = xBase-scaledFontHeight/3-textWidth;
						int y = axisPosition+scaledFontHeight/3;
						drawScaleLabel(g, text, x, y, isCrossHair);
						}
					}
				else {
					drawScaleMolecule(g, graphRect, axis, position, (Depictor2D)label, isCrossHair);
					}
				}
			}
		}

	private void drawScaleLabel(Graphics2D g, String text, int x, int y, boolean isCrossHair) {
		Color textColor;

		if (isCrossHair) {
			Color background = getContrastGrey(SCALE_MEDIUM);
			textColor = getContrastGrey(SCALE_STRONG, background);

			Rectangle2D bounds = g.getFontMetrics().getStringBounds(text, g);
			float border = CROSSHAIR_LABEL_BORDER * scaleIfSplitView(mFontHeight);
			int arc = Math.round((float)bounds.getHeight() / 3);
			g.setColor(background);
			Composite original = g.getComposite();
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, CROSSHAIR_ALPHA));
			g.fillRoundRect(Math.round((float)bounds.getX() + x - border),
							Math.round((float)bounds.getY() + y - border),
							Math.round((float)bounds.getWidth() + 2*border),
							Math.round((float)bounds.getHeight() + 2*border), arc, arc);
			g.setComposite(original);
			}
		else {
			textColor = getContrastGrey(SCALE_MEDIUM);
			}
		g.setColor(textColor);
		g.drawString(text, x, y);
		}

	private void drawScaleMolecule(Graphics2D g, Rectangle graphRect, int axis, float position, Depictor2D depictor, boolean isCrossHair) {
		Color molForeground;
		Color molBackground = getViewBackground();

		int x,y,w,h;
		int scaledFontHeight = Math.round(scaleIfSplitView(mFontHeight));
		int size = !isCrossHair ? mScaleSize[axis]
				 : 2*scaledFontHeight // to extend into the axis name area
				   + (showScale(axis) && mScaleLineList[axis].size() != 0 ? mScaleSize[axis]
				 : DEFAULT_STRUCTURE_SCALE_SIZE*scaledFontHeight);
		h = w = size;
		if (axis == 0) {	// X-axis
			x = graphRect.x + (int)((float)graphRect.width * position) - w/2;
			y = graphRect.y + graphRect.height + mNaNSize[1];
			}
		else {  // Y-axis
			x = graphRect.x - mNaNSize[0] - w;
			y = graphRect.y + graphRect.height - (int)((float)graphRect.height * position) - h/2;
			}

		if (isCrossHair) {
			int crossHairBorder = Math.round(CROSSHAIR_LABEL_BORDER * scaledFontHeight);
			if (axis == 0) {
				x = Math.max(graphRect.x, Math.min(graphRect.x+graphRect.width-w, x));
				if (showScale(axis)) {
					y += crossHairBorder;
					}
				else if (mLegendList.size() == 0) {
					y -= h + crossHairBorder;
					}
				else {
					int legendSize = 0;
					for (VisualizationLegend legend:mLegendList)
						legendSize += legend.getHeight();

					y = Math.min(y+crossHairBorder, y-h-crossHairBorder+legendSize);
					}
				}
			else {
				x = !showScale(axis) ? x+w+crossHairBorder : Math.max(crossHairBorder, x-crossHairBorder);
				y = Math.max(graphRect.y, Math.min(graphRect.y+graphRect.height-h, y));
				}

			molBackground = getContrastGrey(SCALE_MEDIUM);
			molForeground = getContrastGrey(SCALE_STRONG, molBackground);

			int arc = Math.round(scaledFontHeight);
			g.setColor(molBackground);

			Composite original = g.getComposite();
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, CROSSHAIR_ALPHA));
			g.fillRoundRect(x, y, w, h, arc, arc);
			g.setComposite(original);
			}
		else {
			molForeground = getContrastGrey(SCALE_MEDIUM);
			}

		int maxAVBL = (int)(mRelativeFontSize * Depictor2D.cOptAvBondLen);
		if (mIsHighResolution)
			maxAVBL *= mFontScaling;

		Font oldFont = g.getFont();
		depictor.validateView(g, new Rectangle2D.Double(x, y, w, h), Depictor2D.cModeInflateToMaxAVBL+maxAVBL);
		depictor.setOverruleColor(molForeground, molBackground);
		depictor.paint(g);
		g.setFont(oldFont);
		}

	public int getShownCorrelationType() {
		return mShownCorrelationType;
		}

	public void setShownCorrelationType(int type) {
		if (mShownCorrelationType != type) {
			mShownCorrelationType = type;
			invalidateOffImage(false);
			}
		}
	
	public int getCurveMode() {
		return mCurveInfo & cCurveModeMask;
		}

	public float getCurveLineWidth() {
		return mCurveLineWidth;
		}

	public float getCurveSmoothing() {
		return mCurveSmoothing;
		}

	public void setCurveSmoothing(float smoothing) {
		if (mCurveSmoothing != smoothing) {
			mCurveSmoothing = smoothing;
			mCurveY = null;
			if ((mCurveInfo & cCurveModeMask) == cCurveModeSmooth)
				invalidateOffImage(false);
			}
		}

	public boolean isShowStandardDeviationArea() {
		return (mCurveInfo & cCurveStandardDeviation) != 0;
		}

	public boolean isCurveSplitByCategory() {
		return (mCurveInfo & cCurveSplitByCategory) != 0;
		}

	public void setCurveLineWidth(float lineWidth) {
		if (lineWidth != mCurveLineWidth) {
			mCurveLineWidth = lineWidth;
			invalidateOffImage(false);
			}
		}

	public int getCrossHairMode() {
		return mCrossHairMode;
		}

	public void setCrossHairMode(int mode) {
		mCrossHairMode = mode;
		}

	public void setCurveMode(int mode, boolean drawStdDevRange, boolean splitByCategory) {
		int newInfo = mode
					+ (drawStdDevRange ? cCurveStandardDeviation : 0)
					+ (splitByCategory ? cCurveSplitByCategory : 0);
		if (mCurveInfo != newInfo) {
			if (mode != cCurveModeSmooth
			 || splitByCategory != ((mCurveInfo & cCurveSplitByCategory) != 0))
				mCurveY = null;
			if (mode != cCurveModeExpression) {
				mCurveY = null;
				}
			mCurveInfo = newInfo;
			invalidateOffImage(false);
			}
		}

	public String getCurveExpression() {
		return mCurveExpression;
		}

	public void setCurveExpression(String e) {
		if (!isCurveExpressionValid(e))
			e = null;

		if ((e == null ^ mCurveExpression == null)
		 || (e != null && !mCurveExpression.equals(e))) {
			mCurveExpression = e;
			mCurveY = null;
			if (getCurveMode() == cCurveModeExpression)
				invalidateOffImage(false);
			}
		}

	private boolean isCurveExpressionValid(String expression) {
		if (expression != null && expression.length() != 0) {
			JEP parser = new JEP();
			parser.addStandardFunctions();
			parser.addStandardConstants();
			parser.addVariable("x", 0.0);
			parser.parseExpression(expression);
			if (!parser.hasError())
				return true;
			}
		return false;
		}

	public int[] getMultiValueMarkerColumns() {
		return mMultiValueMarkerColumns;
		}

	public int getMultiValueMarkerMode() {
		return mMultiValueMarkerMode;
		}

	public void setMultiValueMarkerColumns(int[] columns, int mode) {
		if (columns == null)
			mode = cMultiValueMarkerModeNone;
		if (mode == cMultiValueMarkerModeNone)
			columns = null;

		boolean isChange = (mMultiValueMarkerMode != mode);
		if (!isChange) {
			isChange = (columns != mMultiValueMarkerColumns);
			if (columns != null && mMultiValueMarkerColumns != null) {
				isChange = true;
				if (columns.length == mMultiValueMarkerColumns.length) {
					isChange = false;
					for (int i=0; i<columns.length; i++) {
						if (columns[i] != mMultiValueMarkerColumns[i]) {
							isChange = true;
							break;
							}
						}
					}
				}
			}
		if (isChange) {
			mMultiValueMarkerColumns = columns;
			mMultiValueMarkerMode = mode;
			invalidateOffImage(true);
			}
		}

	protected Color getMultiValueMarkerColor(int i) {
		return mMultiValueMarkerColor[i];
		}

	@Override
	protected VisualizationPoint createVisualizationPoint(CompoundRecord record) {
		return new VisualizationPoint2D(record);
		}

	protected int getStringWidth(String s) {
		// used by VisualizationLegend
		return (int)mG.getFontMetrics().getStringBounds(s, mG).getWidth();
		}

	protected void setFontHeightAndScaleToSplitView(float h) {
		setFontHeight((int)scaleIfSplitView(h));
		}

	protected void setFontHeight(int h) {
		if (mG.getFont().getSize2D() != h)
			mG.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, h));
		}

	/**
	 * If the view is split, then all text drawing is reduced in size
	 * depending on the number of split views. If we don't have view splitting
	 * then no scaling is done.
	 * @return value scaled down properly to be used in split view
	 */
	private float scaleIfSplitView(float value) {
		return (mHVCount <= 1) ? value : (float)(value / Math.pow(mHVCount, 0.3));
		}

	protected void setColor(Color c) {
		mG.setColor(c);
		}

	protected void drawLine(int x1, int y1, int x2, int y2) {
		mG.drawLine(x1, y1, x2, y2);
		}

	protected void drawRect(int x, int y, int w, int h) {
		mG.drawRect(x, y, w-1, h-1);
		}

	protected void fillRect(int x, int y, int w, int h) {
		mG.fillRect(x, y, w, h);
		}

	protected void drawString(String s, int x, int y) {
		mG.drawString(s, x, y);
		}

	protected void drawMolecule(StereoMolecule mol, Color color, Rectangle2D.Double rect, int mode, int maxAVBL) {
		Depictor2D d = new Depictor2D(mol, Depictor2D.cDModeSuppressChiralText);
		d.validateView(mG, rect, mode+maxAVBL);
		d.setOverruleColor(color, null);
		d.paint(mG);
		}

	protected void paintLegend(Rectangle bounds, boolean transparentBG) {
		mG.setStroke(mThinLineStroke);
		super.paintLegend(bounds, transparentBG);
		}

	@Override
	protected void addLegends(Rectangle bounds, int fontHeight) {
		super.addLegends(bounds, fontHeight);

		if (!mSuppressLegend
		 && mMultiValueMarkerMode != cMultiValueMarkerModeNone
		 && mMultiValueMarkerColumns != null
		 && mChartType != cChartTypeBars
		 && mChartType != cChartTypePies) {
			VisualizationLegend multiValueLegend = new VisualizationLegend(this, mTableModel, cColumnUnassigned, null,
														 VisualizationLegend.cLegendTypeMultiValueMarker);
			multiValueLegend.calculate(bounds, fontHeight);
			bounds.height -= multiValueLegend.getHeight();
			mLegendList.add(multiValueLegend);
			}

		if (!mSuppressLegend
		 && mBackgroundColor.getColorColumn() != cColumnUnassigned
		 && mChartType != cChartTypeBars) {
			VisualizationLegend backgroundLegend = new VisualizationLegend(this, mTableModel,
													mBackgroundColor.getColorColumn(),
													mBackgroundColor,
													mBackgroundColor.getColorListMode() == VisualizationColor.cColorListModeCategories ?
													  VisualizationLegend.cLegendTypeBackgroundColorCategory
													: VisualizationLegend.cLegendTypeBackgroundColorDouble);
			backgroundLegend.calculate(bounds, fontHeight);
			bounds.height -= backgroundLegend.getHeight();
			mLegendList.add(backgroundLegend);
			}
		}

	public int getAvailableShapeCount() {
		return cAvailableShapeCount;
		}

	public int[] getSupportedChartTypes() {
		return SUPPORTED_CHART_TYPE;
		}

	private boolean showScale(int axis) {
		return mScaleMode == cScaleModeShown
			|| (axis == 0 && mScaleMode == cScaleModeHideY)
			|| (axis == 1 && mScaleMode == cScaleModeHideX);
		}

	class LabelHelper {
		private Rectangle mBaseBounds,mBaseGraphRect;
		private MarkerLabelInfo[] mLabelInfo;
		private TreeMap<byte[],VisualizationPoint> mOneLabelPerCategoryMap;
		private int mLabelFlagNo;
		private boolean mIsTreeView;
		private LabelPosition2D[][] mLabelPosition;

		public LabelHelper(Rectangle baseBounds, Rectangle baseGraphRect) {
			mBaseBounds = baseBounds;
			mBaseGraphRect = baseGraphRect;
			mLabelFlagNo = getLabelFlag();
			mOneLabelPerCategoryMap = buildOnePerCategoryMap();
			mLabelInfo = new MarkerLabelInfo[mLabelColumn.length];
			mIsTreeView = isTreeViewGraph();
			for (int i=0; i<mLabelColumn.length; i++)
				if (mLabelColumn[i] != -1)
					mLabelInfo[i] = new MarkerLabelInfo();
			}

		public boolean hasLabels(VisualizationPoint vp) {
			return (mLabelList == cLabelsOnAllRows
				|| (mLabelFlagNo != -1 && vp.record.isFlagSet(mLabelFlagNo)))
			   && (mOneLabelPerCategoryMap == null
				|| vp == mOneLabelPerCategoryMap.get(vp.record.getData(mOnePerCategoryLabelCategoryColumn)));
			}

		public MarkerLabelInfo[] getLabelInfo() {
			return mLabelInfo;
			}

		public void calculateLabels() {
			ArrayList<LabelPosition2D>[] lpList = null;
			if (mOptimizeLabelPositions) {
				lpList = new ArrayList[mHVCount];
				for (int hv=0; hv<mHVCount; hv++)
					lpList[hv] = new ArrayList<>();
			}

			for (VisualizationPoint vp:mPoint) {
				if ((mChartType == cChartTypeScatterPlot && isVisible(vp))
				 || (mChartType == cChartTypeBars && isVisibleInBarsOrPies(vp))
				 || (mChartType == cChartTypeWhiskerPlot && isVisible(vp))
				 || (vp.chartGroupIndex == -1 && isVisible(vp))
				 || (mTreeNodeList != null && isVisible(vp))) {
					if (hasLabels(vp)) {
						for (int j = 0; j<mLabelColumn.length; j++) {
							if (mLabelColumn[j] != -1) {
								prepareMarkerLabelInfo(vp, j, mIsTreeView, mLabelInfo[j]);
								LabelPosition2D lp = calculateMarkerLabel(vp, j, mBaseGraphRect, mLabelInfo[j]);
								if (mOptimizeLabelPositions && lp != null)
									lpList[vp.hvIndex].add(lp);
								}
							}
						}
					}
				}

			if (mOptimizeLabelPositions) {
				mLabelPosition = new LabelPosition2D[mHVCount][];
				for (int hv = 0; hv<mHVCount; hv++)
					mLabelPosition[hv] = lpList[hv].toArray(new LabelPosition2D[0]);
				}
			}

		public void prepareLabels(VisualizationPoint vp) {
			for (int j = 0; j<mLabelColumn.length; j++) {
				if (mLabelColumn[j] != -1) {
					prepareMarkerLabelInfo(vp, j, mIsTreeView, mLabelInfo[j]);
					if (!mOptimizeLabelPositions)
						calculateMarkerLabel(vp, j, mBaseGraphRect, mLabelInfo[j]);
					copyCoordsToMarkerLabelInfo(vp.getOrCreateLabelPosition(mLabelColumn[j], false), mLabelInfo[j]);
					}
				}
			}

		public void optimizeLabels() {
			Rectangle[] graphRect = new Rectangle[mHVCount];

			if (isSplitView())
				for (int hv=0; hv<mHVCount; hv++)
					graphRect[hv] = mSplitter.getSubViewBounds(hv);
			else
				graphRect[0] = mBaseBounds;

			new LabelPostionOptimizer().optimize(graphRect, mLabelPosition, Math.round(0.3f * scaleIfSplitView(mFontHeight)));
			}

		public void drawLabelLines(VisualizationPoint vp, Color outlineColor) {
			for (int j = 0; j<mLabelColumn.length; j++)
				if (mLabelColumn[j] != -1)
					drawMarkerLabelLine(vp, mLabelInfo[j], outlineColor);
			}
		}

	class ScaleLine {
		float position;
		Object label;

		ScaleLine(float position, Object label) {
			this.position = position;
			this.label = label;
			}
		}

	class MarkerLabelInfo {
		int x,x1,x2,y,y1,y2,border;
		float fontSize;
		String label;
		Depictor2D depictor;
		}

	class MultiValueBars {
		private float top,bottom;	// relative usage of area above and below zero line (0...1); is the same for all markers in a view
		private float[] relValue;	// relative value of a specific marker compared to max/min (-1...1)
		int barWidth,firstBarX,zeroY;
		int[] barY,barHeight;

		MultiValueBars() {
			barY = new int[mMultiValueMarkerColumns.length];
			barHeight = new int[mMultiValueMarkerColumns.length];
			relValue = new float[mMultiValueMarkerColumns.length];
			calculateExtends();
			}

		/**
		 * The relative area above and below of the zero line, when showing
		 * multiple bars representing multiple column values of one row.
		 * @return float[2] with top and bottom area between 0.0 and 1.0 each
		 */
		private void calculateExtends() {
			for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
				float min = mTableModel.getMinimumValue(mMultiValueMarkerColumns[i]);
				float max = mTableModel.getMaximumValue(mMultiValueMarkerColumns[i]);
				if (min >= 0f) {
					top = 1f;
					continue;
					}
				if (max <= 0f) {
					bottom = 1f;
					continue;
					}
				float topPart = max / (max - min);
				if (top < topPart)
					top = topPart;
				if (bottom < 1f - topPart)
					bottom = 1f - topPart;
				}
			}

		private void calculate(float size, VisualizationPoint vp) {
			float factor = 0;
			for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
				float min = mTableModel.getMinimumValue(mMultiValueMarkerColumns[i]);
				float max = mTableModel.getMaximumValue(mMultiValueMarkerColumns[i]);
				float value = vp.record.getDouble(mMultiValueMarkerColumns[i]);
				relValue[i] = Float.isNaN(value) ? Float.NaN
							: (min >= 0f) ? value/max
							: (max <= 0f) ? value/-min
							: (max/top > -min/bottom) ? value*top/max : value*bottom/-min;
				if (!Float.isNaN(value))
					factor = Math.max(factor, Math.abs(relValue[i]));
				}

			float height = size*2f;	// value range of one bar in pixel; the histogram height is <= 2*height
			barWidth = Math.max(2, Math.round(size/(2f*(float)Math.sqrt(mMultiValueMarkerColumns.length))));

			// if we have not used all height, then we reduce barWidth and give it to height
			float widthReduction = 1f;
			int newBarWidth = Math.max(1, (int)((barWidth+1) * Math.sqrt(factor)));
			if (newBarWidth < barWidth) {
				widthReduction = (float)barWidth / (float)newBarWidth;
				barWidth = newBarWidth;
				}

			firstBarX = Math.round(vp.screenX - mMultiValueMarkerColumns.length * barWidth / 2);
			zeroY = Math.round(vp.screenY + height*factor*widthReduction*0.5f*(top-bottom));

			for (int i=0; i<mMultiValueMarkerColumns.length; i++) {
				if (Float.isNaN(relValue[i])) {
					barHeight[i] = -1;
					barY[i] = zeroY+1;
					}
				else if (relValue[i] > 0) {
					barHeight[i] = Math.round(height*widthReduction*relValue[i]);
					barY[i] = zeroY-barHeight[i];
					}
				else {
					barHeight[i] = -Math.round(height*widthReduction*relValue[i]);
					barY[i] = zeroY+1;
					}
				}
			}
		}

	class ViewPort {
		float[] min,max,visMin,visMax;

		ViewPort() {
			min = new float[2];
			max = new float[2];
			visMin = new float[2];
			visMax = new float[2];
			for (int i=0; i<2; i++) {
				int column = mAxisIndex[i];
				if (column == cColumnUnassigned || mTreeNodeList != null) {
					min[i] = mAxisVisMin[i];
					max[i] = mAxisVisMax[i];
					}
				else if (mIsCategoryAxis[i]) {
					min[i] = -0.5f;
					max[i] = -0.5f + mTableModel.getCategoryCount(column);
					}
				else {
					float[] minAndMax = getDataMinAndMax(i);
					min[i] = minAndMax[0];
					max[i] = minAndMax[1];
//					min[i] = mTableModel.getMinimumValue(column);
//					max[i] = mTableModel.getMaximumValue(column);
					}
				visMin[i] = mAxisVisMin[i];
				visMax[i] = mAxisVisMax[i];
				}
			}

		float getRange(int dimension) {
			return max[dimension] - min[dimension];
			}

		float getVisRangle(int dimension) {
			return visMax[dimension] - visMin[dimension];
			}
		}
	}
