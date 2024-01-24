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
import com.actelion.research.gui.generic.GenericRectangle;
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
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.VolatileImage;
import java.awt.print.PageFormat;
import java.io.*;
import java.text.DateFormat;
import java.util.*;

import static com.actelion.research.table.view.VisualizationColor.cUseAsFilterColor;

public class JVisualization2D extends JVisualization {
	@Serial
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
	private static final float cMinTextLabelSpace = 1.3f;	// multiplied with text height is minimum distance between category text labels
	private static final float cMinStructureLabelSpace = 2.0f;	// multiplied with text height is minimum distance between structure labels
	private static final float ARROW_TIP_SIZE = 0.6f;
	private static final float OUTLINE_LIMIT = 3;

	private static final int cPrintScaling = 16;

	protected static final float MARKER_OUTLINE = 0.7f;
	private static final float NAN_WIDTH = 2.0f;
	private static final float NAN_SPACING = 0.5f;
	private static final float AXIS_TEXT_PADDING = 0.5f;
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

	public static final String[] CURVE_MODE_TEXT = { "<none>", "Vertical Line", "Horizontal Line", "Fitted Line", "Smooth Curve", "Use Formula (Shown)", "Use Formula (Not Shown)" };
	public static final String[] CURVE_MODE_CODE = { "none", "abscissa", "ordinate", "fitted", "smooth", "formula", "formula2" };
	private static final int cCurveModeNone = 0;
	private static final int cCurveModeMask = 7;
	public static final int cCurveModeVertical = 1;
	public static final int cCurveModHorizontal = 2;
	public static final int cCurveModeFitted = 3;
	public static final int cCurveModeSmooth = 4;
	public static final int cCurveModeByFormulaShow = 5;
	public static final int cCurveModeByFormulaHide = 6;
	private static final int cCurveStandardDeviation = 8;
	private static final int cCurveSplitByCategory = 16;
	private static final int cCurveTruncateArea = 32;

	public static final int cCurveRowListVisible = CompoundTableListHandler.LISTINDEX_NONE;
	public static final int cCurveRowListSelected = CompoundTableListHandler.LISTINDEX_SELECTION;
	public static final String ROW_LIST_CODE_VISIBLE = "<visible>";
	public static final String ROW_LIST_CODE_SELECTED = "<selected>";

	public static final float DEFAULT_CURVE_LINE_WIDTH = 1.5f;
	public static final float DEFAULT_CURVE_SMOOTHING = 0.5f;

	private static final int[] SUPPORTED_CHART_TYPE = { cChartTypeScatterPlot, cChartTypeWhiskerPlot, cChartTypeBoxPlot, cChartTypeViolins, cChartTypeBars, cChartTypePies };

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

	private Rectangle       mBoundsWithoutLegend;
	private Graphics2D		mG;
	private Composite		mLabelBackgroundComposite;
	private Stroke          mThinLineStroke,mNormalLineStroke,mFatLineStroke,mVeryFatLineStroke,mConnectionStroke;
	private float[]			mCorrelationCoefficient;
	private float			mBackgroundColorRadius,mBackgroundColorFading,mFontScaling,mMarkerTransparency,
							mMarkerLabelTransparency,mConnectionLineTransparency,mCurveLineWidth,mCurveSmoothing;
	private int				mBorder,mCurveInfo,mBackgroundHCount,mBackgroundVCount,mCrossHairMode,
							mBackgroundColorConsidered,mCurveSplitCategoryColumn,mCurveRowList,mCaseSeparationAxis,
							mConnectionFromIndex1,mConnectionFromIndex2,mShownCorrelationType,mMultiValueMarkerMode;
	private long			mPreviousPaintEnd,mPreviousFullDetailPaintMillis,mMostRecentRepaintMillis;
	private boolean			mBackgroundValid,mIsHighResolution,mScaleTitleCentered,
							mDrawMarkerOutline,mDrawBarPieBoxOutline;
	private int[]			mScaleTextMode,mScaleDepictorOffset,mSplittingMolIndex,mMultiValueMarkerColumns;
	private int[]           mScaleSize; // 0: label area height beneath X-axis; 1: label area width left of y-axis
	private int[]           mNaNSize;   // 0: NaN area width left of Y-axis; 1: NaN area height beneath X-axis
	private VisualizationColor	mBackgroundColor;
	private LabelHelper     mLabelHelper;
	private Color[]			mMultiValueMarkerColor;
	private Depictor2D[][]	mScaleDepictor,mSplittingDepictor;
	private VolatileImage	mOffImage;
	private BufferedImage   mBackgroundImage;		// primary data
	private BufferedImage[] mMarkerBackgroundImage;
	private byte[]			mBackgroundImageData;	// cached if delivered or constructed from mBackgroundImage if needed
//	private byte[]			mSVGBackgroundData;		// alternative to mBackgroundImage
	private Graphics2D		mOffG;
	private ArrayList<ScaleLine>[] mScaleLineList;
	private ArrayList<GraphPoint> mCrossHairList;
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
		mCrossHairList = new ArrayList<>();

		mBackgroundColor = new VisualizationColor(mTableModel, this);

		initialize();
		}

	protected void initialize() {
		super.initialize();
		mBackgroundColorConsidered = BACKGROUND_VISIBLE_RECORDS;
		mMarkerShapeColumn = cColumnUnassigned;
		mChartColumn = cColumnUnassigned;
		mChartMode = cChartModeCount;
		mCurveSplitCategoryColumn = cColumnUnassigned;
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
		mCurveRowList = cCurveRowListVisible;
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

		float retinaFactor = HiDPIHelper.getRetinaScaleFactor();

		if (mOffImage == null
		 || mOffImage.getWidth(null) != width*retinaFactor
		 || mOffImage.getHeight(null) != height*retinaFactor) {
			mOffImage = ((Graphics2D)g).getDeviceConfiguration().createCompatibleVolatileImage(Math.round(width*retinaFactor), Math.round(height*retinaFactor), Transparency.OPAQUE);
			mCoordinatesValid = false;
			}
		else if (mOffImage.validate(((Graphics2D)g).getDeviceConfiguration()) == VolatileImage.IMAGE_INCOMPATIBLE) {
			mOffImageValid = false;
			}

		if (!mCoordinatesValid) {
			mBackgroundValid = false;
			mOffImageValid = false;
			}

		if (!mOffImageValid) {
			do  {
				if (mOffImage.validate(((Graphics2D)g).getDeviceConfiguration()) == VolatileImage.IMAGE_INCOMPATIBLE)
					mOffImage = ((Graphics2D) g).getDeviceConfiguration().createCompatibleVolatileImage(Math.round(width * retinaFactor), Math.round(height * retinaFactor), Transparency.OPAQUE);

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
					if (!mIsFastRendering && !mSkipPaintDetails) {
						mOffG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//					mOffG.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);	// no sub-pixel accuracy looks cleaner
					}
		
					mOffG.setColor(getViewBackground());
					mOffG.fillRect(0, 0, width, height);
					Insets insets = getInsets();
					Rectangle bounds = new Rectangle(insets.left, insets.top, width-insets.left-insets.right, height-insets.top-insets.bottom);
		
					mCorrelationCoefficient = null;

					mFontHeight = calculateFontSize(bounds.width, bounds.height, 1f, retinaFactor, true);

					mG = mOffG;
					paintContent(bounds, false);

					if (mWarningMessage != null && width > HiDPIHelper.scale(100)) {
						setColor(Color.RED);
						setFontHeight(mFontHeight);
						String msg = mWarningMessage;
						int y = mFontHeight;
						while (!msg.isEmpty()) {
							int index = msg.length();
							while (getStringWidth(msg.substring(0, index)) > width) {
								int lastSpaceIndex = msg.lastIndexOf(' ', index-1);
								index = (lastSpaceIndex != -1) ? lastSpaceIndex : index-1;
								}
							drawString(msg.substring(0, index), 0, y);
							msg = msg.substring(index < msg.length() && msg.charAt(index) == ' ' ? index + 1 : index);
							y += mFontHeight;
							}
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
					if (mOffG != null)
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

		// draw all crosshairs in list
		if (!mCrossHairList.isEmpty()) {
			if (isSplitView()) {
				for (int hv = 0; hv<mHVCount; hv++) {
					Rectangle graphBounds = getGraphBounds(mSplitter.getSubViewBounds(hv));
					for (GraphPoint p:mCrossHairList)
						drawCrossHair((Graphics2D)g, (float)p.getRelativeX(), (float)p.getRelativeY(), graphBounds);
					}
				}
			else {
				Rectangle graphBounds = getGraphBounds(mBoundsWithoutLegend);
				for (GraphPoint p:mCrossHairList)
					drawCrossHair((Graphics2D)g, (float)p.getRelativeX(), (float)p.getRelativeY(), graphBounds);
				}
			}

		// draw interactive crosshair if mouse is in area
		if (showCrossHair()) {
			Rectangle graphBounds = getGraphBounds(mMouseX1, mMouseY1, true);
			if (graphBounds != null)
				drawCrossHair((Graphics2D)g, (mMouseX1-graphBounds.x)/(float)graphBounds.width,
						(graphBounds.y+graphBounds.height-mMouseY1)/(float)graphBounds.height, graphBounds);
			}

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

	private void paintContent(final Rectangle bounds, boolean transparentBG) {
		if (validateSplittingIndices())
			mBackgroundValid = false;

		mChartInfo = null;
		mLabelHelper = null;

		calculateMarkerSize(bounds);    // marker sizes are needed for size legend
		mBoundsWithoutLegend = new Rectangle(bounds);
		calculateLegend(mBoundsWithoutLegend, (int)scaleIfSplitView(mFontHeight));

		if (isSplitView()) {
			int scaledFontHeight = Math.round(scaleIfSplitView(mFontHeight));
			if (!mLegendList.isEmpty())
				mBoundsWithoutLegend.height -= scaledFontHeight / 2;
			compileSplittingHeaderMolecules();
			int count1 = mShowEmptyInSplitView ? mTableModel.getCategoryCount(mSplittingColumn[0]) : calculateVisibleCategoryCount(mSplittingColumn[0]);
			int count2 = (mSplittingColumn[1] == cColumnUnassigned) ? -1
					: mShowEmptyInSplitView ? mTableModel.getCategoryCount(mSplittingColumn[1]) : calculateVisibleCategoryCount(mSplittingColumn[1]);
			boolean largeHeader = (mSplittingDepictor[0] != null
					|| mSplittingDepictor[1] != null);
			mSplitter = new VisualizationSplitter(mBoundsWithoutLegend, count1, count2, scaledFontHeight, largeHeader, mSplittingAspectRatio);
			}

		switch (mChartType) {
			case cChartTypeBoxPlot:
				mChartInfo = new BoxPlot(this, mHVCount, determineBoxPlotDoubleAxis());
				mChartInfo.calculate();
				break;
			case cChartTypeWhiskerPlot:
				mChartInfo = new WhiskerPlot(this, mHVCount, determineBoxPlotDoubleAxis());
				mChartInfo.calculate();
				break;
			case cChartTypeViolins:
				mChartInfo = new ViolinPlot(this, mHVCount, determineBoxPlotDoubleAxis());
				mChartInfo.calculate();
				break;
			case cChartTypeBars:
				mChartInfo = new BarChart(this, mHVCount, mChartMode);
				mChartInfo.calculate();
				break;
			case cChartTypePies:
				mChartInfo = new PieChart(this, mHVCount, mChartMode);
				mChartInfo.calculate();
				break;
			}

		// total bounds of first split (or total unsplit) graphical including scales
		Rectangle baseBounds = isSplitView() ? mSplitter.getSubViewBounds(0) : mBoundsWithoutLegend;

		mCaseSeparationAxis = getCaseSeparationAxis();

		boolean hasFreshCoords = false;
		if (!mCoordinatesValid) {
			calculateCoordinates(mG, baseBounds);
			hasFreshCoords = true;
			}

		// bounds of first split (or total unsplit) graph area. This excluded scales, NaN area, border, etc.
		Rectangle baseGraphRect = getGraphBounds(baseBounds);
		if (baseGraphRect.width <= 0 || baseGraphRect.height <= 0)
			return;

		// We may need to enlarge the visible scale range to create space for statistics labels
		if (hasFreshCoords
		 && (mChartType == cChartTypeBars
		  || mChartType == cChartTypePies))
			((AbstractBarOrPieChart)mChartInfo).adaptDoubleScalesForStatisticalLabels(baseGraphRect);

		if (mOptimizeLabelPositions
		 && mChartType != cChartTypeBars
		 && mChartType != cChartTypePies) {
			mLabelHelper = new LabelHelper(baseBounds, baseGraphRect);
			mLabelHelper.calculateLabels();
			mLabelHelper.optimizeLabels();
			}

		int curveMode = mCurveInfo & cCurveModeMask;
		if (curveMode == cCurveModeSmooth && mCurveY == null)
			calculateSmoothCurve(baseGraphRect.width);
		if ((curveMode == cCurveModeByFormulaShow || curveMode == cCurveModeByFormulaHide) && mCurveY == null)
			calculateCurveByFormula(baseGraphRect);

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
				paintGraph(getGraphBounds(mSplitter.getSubViewBounds(hv)), hv);

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
					GenericRectangle r = new GenericRectangle(x1, titleArea.y, molWidth, titleArea.height);
					int maxAVBL = Depictor2D.cOptAvBondLen;
					if (mIsHighResolution)
						maxAVBL = Math.round(maxAVBL * mFontScaling);
					mSplittingDepictor[0][cat1Index].validateView(mG, r, Depictor2D.cModeInflateToMaxAVBL+maxAVBL);
					mSplittingDepictor[0][cat1Index].paint(mG);
					}

				if (mSplittingColumn[1] != cColumnUnassigned) {
					mG.drawString(" | ", titleArea.x+(titleArea.width-totalWidth)/2+title1Width, textY);

					int x2 = titleArea.x+(totalWidth+titleArea.width)/2-title2Width;
					if (mSplittingDepictor[1] == null)
						mG.drawString(title2, x2, textY);
					else if (mSplittingDepictor[1][mSplitter.getVIndex(hv)] != null) {
						GenericRectangle r = new GenericRectangle(x2, titleArea.y, molWidth, titleArea.height);
						int maxAVBL = Depictor2D.cOptAvBondLen;
						if (mIsHighResolution)
							maxAVBL = Math.round(maxAVBL * mFontScaling);
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
			paintGraph(baseGraphRect, 0);
			}

		if (mChartType == cChartTypeScatterPlot || mChartInfo.isChartWithMarkers())
			paintMarkers(baseBounds, baseGraphRect);
		if (mChartType != cChartTypeScatterPlot)
			mChartInfo.paint(mG, baseBounds, baseGraphRect);

		paintLegend(mBoundsWithoutLegend, transparentBG);

		if (!mIsHighResolution)
			paintMessages(mG, mBoundsWithoutLegend.x, mBoundsWithoutLegend.width);
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
			Rectangle bounds = getGraphBounds(mBoundsWithoutLegend);
			if (tolerant) {
				int gap = Math.min(mBorder, mFontHeight);
				if (screenX > mBoundsWithoutLegend.x + gap
				 && screenX < mBoundsWithoutLegend.x + mBoundsWithoutLegend.width - mBorder
				 && screenY > mBoundsWithoutLegend.y + mBorder
				 && screenY < mBoundsWithoutLegend.y + mBoundsWithoutLegend.height - gap)
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
	 * In case of a split view this method returns the bounds of that rectangle, which contains P(x,y).
	 * This includes NaN area, border, scales and axis text. If the view is not split, then the full view
	 * bounds are returned.
	 * @param x
	 * @param y
	 * @return may be null, if x,y is not within a view area
	 */
	public Rectangle getViewBounds(int x, int y) {
		if (isSplitView()) {
			int hv = mSplitter.getHVIndex(x, y, true);
			return (hv != -1) ? mSplitter.getSubViewBounds(hv) : null;
			}
		return mBoundsWithoutLegend;
		}

	public Rectangle getAllBoundsWithoutLegend() {
		return mBoundsWithoutLegend;
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
					if (mAxisIndex[0] != cColumnUnassigned || (mChartType == cChartTypeBars && mChartInfo.mDoubleAxis == 0))
						graphBounds.height -= (AXIS_TEXT_PADDING+1.0) * scaledFontHeight;
					}
				if (showScale(1)) {
					if (mAxisIndex[1] != cColumnUnassigned || (mChartType == cChartTypeBars && mChartInfo.mDoubleAxis == 1)) {
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

	private void paintGraph(Rectangle graphRect, int hvIndex) {
		setFontHeightAndScaleToSplitView(mFontHeight);

		if (hasColorBackground()) {
			if (mSplitter != null
			 && (mSplitter.getHCount() != mBackgroundHCount
			  || mSplitter.getVCount() != mBackgroundVCount))
				mBackgroundValid = false;

			if (!mBackgroundValid)
				calculateBackground(graphRect);
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
				else if (getCurveMode() == cCurveModeByFormulaShow || getCurveMode() == cCurveModeByFormulaHide)
					drawCurveByFormulaArea(hvIndex, graphRect);
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
			mG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(1.0 - mMarkerTransparency)));
			}
		Composite labelComposite = (mMarkerLabelTransparency == mMarkerTransparency) ? null
				: AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)(1.0 - mMarkerLabelTransparency));

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

				VisualizationPoint vp = mPoint[i];

				boolean drawLabels = false;
				if (isVisible(vp)
				 && (mChartType == cChartTypeScatterPlot
				  || mTreeNodeList != null
				  || mChartInfo.paintMarker(vp))) {
					vp.widthOrAngle1 = vp.heightOrAngle2 = (int)getMarkerSize(vp);
					boolean inFocus = (focusFlagNo == -1 || vp.record.isFlagSet(focusFlagNo));

					Color color = (isFilter && !vp.record.isFlagSet(mUseAsFilterFlagNo)) ? cUseAsFilterColor
							: (vp.record.isSelected() && useSelectionColor) ? VisualizationColor.cSelectedColor
							: mMarkerColor.getColorList()[vp.markerColorIndex];

					Color labelBG = mLabelBackgroundColor.getColorForBackground(vp.record, isDarkBackground);
					if (labelBG == null)
						labelBG = mLabelBackgroundColor.getDefaultDataColor();
					if (!inFocus)
						labelBG = VisualizationColor.lowContrastColor(labelBG, getViewBackground());

					Color markerColor = inFocus ? color : VisualizationColor.lowContrastColor(color, getViewBackground());
					Color outlineColor = isDarkBackground ? markerColor.brighter() : markerColor.darker();

					drawLabels = mLabelHelper != null && mLabelHelper.hasLabels(vp);
					if (drawLabels) {
						mLabelHelper.prepareLabels(vp);
						mLabelHelper.drawLabelLines(vp, outlineColor, labelComposite);
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
						drawMarkerLabels(mLabelHelper.getLabelInfo(), markerColor, labelBG, outlineColor, isTreeView, labelComposite);
					}

				if (!drawLabels)
					vp.removeNonCustomLabelPositions();
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

	protected void drawMarkerLabels(MarkerLabelInfo[] labelInfo, Color labelFG, Color labelBG, Color outlineColor, boolean isTreeView, Composite composite) {
		if (mMarkerLabelSize != 1.0)
			setFontHeightAndScaleToSplitView(mMarkerLabelSize * mFontHeight);

		boolean isDarkBackground = (ColorHelper.perceivedBrightness(getViewBackground()) <= 0.5);
		labelFG = mIsMarkerLabelsBlackAndWhite ? getContrastGrey(1f) : isDarkBackground ? labelFG : labelFG.darker();

		Composite original = null;
		if (composite != null) {
			original = mG.getComposite();
			mG.setComposite(composite);
			}

		if (mLabelColumn[MarkerLabelDisplayer.cMidCenter] != cColumnUnassigned && (!mLabelsInTreeViewOnly || isTreeView)) {
			drawMarkerLabel(labelInfo[MarkerLabelDisplayer.cMidCenter], labelFG, labelBG, outlineColor);
			}

		for (int i=0; i<labelInfo.length; i++) {
			if (i != MarkerLabelDisplayer.cMidCenter
			 && mLabelColumn[i] != cColumnUnassigned
			 && (!mLabelsInTreeViewOnly || isTreeView)) {
				drawMarkerLabel(labelInfo[i], labelFG, labelBG, outlineColor);
				}
			}

		if (mMarkerLabelSize != 1.0)
			setFontHeightAndScaleToSplitView(mFontHeight);

		if (original != null)
			mG.setComposite(original);
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
			if (mli.label.isEmpty())
				mli.label = null;
			}
		}

	private void copyCoordsToMarkerLabelInfo(LabelPosition2D labelPosition, MarkerLabelInfo mli) {
		mli.x1 = labelPosition.getScreenX1();
		mli.y1 = labelPosition.getScreenY1();
		mli.x2 = labelPosition.getScreenX2();
		mli.y2 = labelPosition.getScreenY2();

		if (mli.depictor != null) {
			GenericRectangle molRect = mli.depictor.getBoundingRect();
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
			GenericRectangle molRect = mli.depictor.getBoundingRect();
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

	private void drawMarkerLabel(MarkerLabelInfo mli, Color labelFG, Color labelBG, Color outlineColor) {
		if (mli.label != null || mli.depictor != null) {
			if (mShowLabelBackground) {
				Composite original = null;
				if (mLabelBackgroundComposite != null) {
					original = mG.getComposite();
					mG.setComposite(mLabelBackgroundComposite);
					}

				mG.setColor(labelBG);
				mG.fillRect(mli.x1, mli.y1, mli.x2-mli.x1, mli.y2-mli.y1);
				mG.setColor(outlineColor);
				mG.setStroke(mThinLineStroke);
				mG.drawRect(mli.x1, mli.y1, mli.x2-mli.x1, mli.y2-mli.y1);

				if (original != null)
					mG.setComposite(original);
				}

			// For custom located labels we may need to draw a line from marker to label edge

			if (mli.depictor != null) {
				mli.depictor.setOverruleColor(labelFG.getRGB(), getViewBackground().getRGB());
				mli.depictor.paint(mG);
				}
			else {
				mG.setColor(labelFG);
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

		Composite original = null;
		if (mConnectionLineTransparency != mMarkerTransparency) {
			original = mG.getComposite();
			mG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f - mConnectionLineTransparency));
			}

		mG.setStroke(mConnectionStroke);

		while (true) {
			int toIndex1 = fromIndex2;

			while (toIndex1<mConnectionLinePoint.length
				&& !isVisibleExcludeNaN(mConnectionLinePoint[toIndex1]))
				toIndex1++;

			if (toIndex1 == mConnectionLinePoint.length)
				break;

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

		if (original != null)
			mG.setComposite(original);
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

		Composite original = null;
		if (mConnectionLineTransparency != mMarkerTransparency || strengthColumn == -1)
			original = mG.getComposite();
		if (mConnectionLineTransparency != mMarkerTransparency)
			mG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f - mConnectionLineTransparency));

		mG.setStroke(mConnectionStroke);

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
							drawConnectionLine(vp1, vp2, considerFocus && !inFocus,
									(1f-strength) * mConnectionLineTransparency, !isRedundant);
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
								float transparency = mConnectionLineTransparency;
								if (strength != null) {
									try {
										float value = Math.min(max, Math.max(min, mTableModel.tryParseEntry(strength[index++], strengthColumn)));
										transparency *= Float.isNaN(value) ? 1.0f : (max-value) / dif;
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

		Color color = ColorHelper.intermediateColor(mMarkerColor.getColorList()[p1.markerColorIndex],
													mMarkerColor.getColorList()[p2.markerColorIndex], 0.5f);
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
		return mCrossHairMode != CROSSHAIR_MODE_NONE
			&& (qualifiesAsCrossHairAxis(0) || qualifiesAsCrossHairAxis(1))
			&& (mSplitter == null || mSplitter.getHVIndex(mMouseX1, mMouseY1, false) != -1);
		}

	private void drawCrossHair(Graphics2D g, float positionX, float positionY, Rectangle graphRect) {
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (int)scaleIfSplitView(mFontHeight)));
		if (positionX >= 0 && qualifiesAsCrossHairAxis(0)) {
			Object label = calculateDynamicScaleLabel(0, positionX);
			if (label != null)
				drawScaleLine(g, graphRect, 0, label, positionX, true, false);
			}
		if (positionY >= 0 && qualifiesAsCrossHairAxis(1)) {
			Object label = calculateDynamicScaleLabel(1, positionY);
			if (label != null)
				drawScaleLine(g, graphRect, 1, label, positionY, true, false);
			}
		}

	private boolean qualifiesAsCrossHairAxis(int axis) {
		return mCrossHairMode == CROSSHAIR_MODE_BOTH
			|| (mCrossHairMode == CROSSHAIR_MODE_X && axis == 0)
			|| (mCrossHairMode == CROSSHAIR_MODE_Y && axis == 1)
			|| (mCrossHairMode == CROSSHAIR_MODE_AUTOMATIC
			 && (mScaleLineList[axis].size() == 0
			  || (mAxisIndex[axis] != -1 && !mIsCategoryAxis[axis])
			  || (mAxisIndex[axis] == -1 && mChartType == cChartTypeBars && mChartInfo.mDoubleAxis == axis)));
		}

	private void drawCurves(Rectangle baseGraphRect) {
		float lineWidth = scaleIfSplitView(mFontHeight)/12f;
		Stroke curveStroke = new BasicStroke(lineWidth * mCurveLineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		mG.setColor(getContrastGrey(SCALE_STRONG));
		mG.setStroke(curveStroke);

		switch (mCurveInfo & cCurveModeMask) {
		case cCurveModeVertical:
			drawVerticalMeanLine(baseGraphRect, getCurveBounds());
			break;
		case cCurveModHorizontal:
			drawHorizontalMeanLine(baseGraphRect, getCurveBounds());
			break;
		case cCurveModeFitted:
			drawFittedLine(baseGraphRect, getCurveBounds());
			break;
		case cCurveModeSmooth:
			drawSmoothCurve();
			break;
		case cCurveModeByFormulaShow:
			drawCurveByFormula(baseGraphRect, true);
			break;
		case cCurveModeByFormulaHide:
			drawCurveByFormula(baseGraphRect, false);
			break;
			}
		}

	private Rectangle[][] getCurveBounds() {
		if ((mCurveInfo & cCurveTruncateArea) == 0)
			return null;

		int catCount = getSplitCurveCategoryCount();
		Rectangle[][] bounds = new Rectangle[mHVCount][catCount];
		long mask = mTableModel.getListHandler().getListMask(mCurveRowList);
		for (int i=0; i<mDataPoints; i++) {
			if (isConsideredForCurve(mPoint[i], mask)) {
				int hv = mPoint[i].hvIndex;
				int cat = (catCount == 1) ? 0 : getSplitCurveCategoryIndex(mPoint[i]);
				int x = Math.round(mPoint[i].screenX);
				int y = Math.round(mPoint[i].screenY);
				if (bounds[hv][cat] == null)
					bounds[hv][cat] = new Rectangle(x, y, 0, 0);
				else
					bounds[hv][cat].add(x, y);
				}
			}

		return bounds;
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
		int catCount = getSplitCurveCategoryCount();
		int[][] count = new int[mHVCount][catCount];
		float[][] xmean = new float[mHVCount][catCount];
		float[][] stdDev = new float[mHVCount][catCount];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int cat = (catCount == 1) ? 0 : getSplitCurveCategoryIndex(mPoint[i]);
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
					int cat = (catCount == 1) ? 0 : getSplitCurveCategoryIndex(mPoint[i]);
					stdDev[mPoint[i].hvIndex][cat] += (getAxisValue(mPoint[i].record, axis) - xmean[mPoint[i].hvIndex][cat])
											   		* (getAxisValue(mPoint[i].record, axis) - xmean[mPoint[i].hvIndex][cat]);
					}
				}
			}

		String[][] splittingCategory = getSplitViewCategories();
		String[] colorCategory = isEffectiveCurveSplitByColorCategory() ? mTableModel.getCategoryList(colorColumn) : null;
		String[] secondCategory = isEffectiveCurveSplitBySecondCategory() ? mTableModel.getCategoryList(mCurveSplitCategoryColumn) : null;

//		writer.write((axis == 0) ? "Vertical Mean Line:" : "Horizontal Mean Line:");	// without this line we can paste the data into DataWarrior
//		writer.newLine();

		// if we have distinct curve statistics by color categories and if we we split the view by the same column,
		// the we have lots of empty combinations that we should suppress in the output
		boolean isRedundantSplitting = colorCategory != null && isSplitView()
				&& (mSplittingColumn[0] == colorColumn
				|| (mSplittingColumn[1] != cColumnUnassigned && mSplittingColumn[1] == colorColumn));

		if (isSplitView())
			writer.write(mTableModel.getColumnTitle(mSplittingColumn[0])+"\t");
		if (isSplitView() && mSplittingColumn[1] != cColumnUnassigned)
			writer.write(mTableModel.getColumnTitle(mSplittingColumn[1])+"\t");
		if (colorCategory != null && !isRedundantSplitting)
			writer.write(mTableModel.getColumnTitle(colorColumn)+"\t");
		if (secondCategory != null)
			writer.write(mTableModel.getColumnTitle(mCurveSplitCategoryColumn)+"\t");
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
				if (colorCategory != null && !isRedundantSplitting)
					writer.write(colorCategory[getCurveColorIndexFromCombinedCategoryIndex(cat)]+"\t");
				if (secondCategory != null)
					writer.write(secondCategory[getCurveSecondCategoryIndexFromCombinedCategoryIndex(cat)]+"\t");
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
		int catCount = getSplitCurveCategoryCount();
		int[][] count = new int[mHVCount][catCount];
		float[][] sx = new float[mHVCount][catCount];
		float[][] sy = new float[mHVCount][catCount];
		float[][] sx2 = new float[mHVCount][catCount];
		float[][] sxy = new float[mHVCount][catCount];
		for (int i=0; i<mDataPoints; i++) {
			if (isVisibleExcludeNaN(mPoint[i])) {
				int cat = (catCount == 1) ? 0 : getSplitCurveCategoryIndex(mPoint[i]);
				sx[mPoint[i].hvIndex][cat] += getAxisValue(mPoint[i].record, 0);
				sy[mPoint[i].hvIndex][cat] += getAxisValue(mPoint[i].record, 1);
				sx2[mPoint[i].hvIndex][cat] += getAxisValue(mPoint[i].record, 0) * getAxisValue(mPoint[i].record, 0);
				sxy[mPoint[i].hvIndex][cat] += getAxisValue(mPoint[i].record, 0) * getAxisValue(mPoint[i].record, 1);
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

		float[][] stdDev = null;
		if ((mCurveInfo & cCurveStandardDeviation) != 0) {
			stdDev = new float[mHVCount][catCount];
			for (int i=0; i<mDataPoints; i++) {
				if (isVisibleExcludeNaN(mPoint[i])) {
					int cat = (catCount == 1) ? 0 : getSplitCurveCategoryIndex(mPoint[i]);
					float b2 = getAxisValue(mPoint[i].record, 1) + getAxisValue(mPoint[i].record, 0)/m[mPoint[i].hvIndex][cat];
					float xs = (b2-b[mPoint[i].hvIndex][cat])/(m[mPoint[i].hvIndex][cat]+1.0f/m[mPoint[i].hvIndex][cat]);
					float ys = -xs/m[mPoint[i].hvIndex][cat] + b2;
					stdDev[mPoint[i].hvIndex][cat] += (getAxisValue(mPoint[i].record, 0)-xs)*(getAxisValue(mPoint[i].record, 0)-xs);
					stdDev[mPoint[i].hvIndex][cat] += (getAxisValue(mPoint[i].record, 1)-ys)*(getAxisValue(mPoint[i].record, 1)-ys);
					}
				}
			}

		String[][] splittingCategory = getSplitViewCategories();
		String[] colorCategory = isEffectiveCurveSplitByColorCategory() ? mTableModel.getCategoryList(colorColumn) : null;
		String[] secondCategory = isEffectiveCurveSplitBySecondCategory() ? mTableModel.getCategoryList(mCurveSplitCategoryColumn) : null;

//		writer.write("Fitted Straight Line:");	// without this line we can paste the data into DataWarrior
//		writer.newLine();

		if (mTableModel.isLogarithmicViewMode(mAxisIndex[0]) || mTableModel.isLogarithmicViewMode(mAxisIndex[1])) {
			writer.write("Gradient m and standard deviation are based on logarithmic values.");
			writer.newLine();
			}

		// if we have distinct curve statistics by color categories and if we we split the view by the same column,
		// the we have lots of empty combinations that we should suppress in the output
		boolean isRedundantSplitting = colorCategory != null && isSplitView()
				&& (mSplittingColumn[0] == colorColumn
				|| (mSplittingColumn[1] != cColumnUnassigned && mSplittingColumn[1] == colorColumn));

		if (isSplitView())
			writer.write(mTableModel.getColumnTitle(mSplittingColumn[0])+"\t");
		if (isSplitView() && mSplittingColumn[1] != cColumnUnassigned)
			writer.write(mTableModel.getColumnTitle(mSplittingColumn[1])+"\t");
		if (colorCategory != null && !isRedundantSplitting)
			writer.write(mTableModel.getColumnTitle(colorColumn)+"\t");
		if (secondCategory != null)
			writer.write(mTableModel.getColumnTitle(mCurveSplitCategoryColumn)+"\t");
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
				if (colorCategory != null && !isRedundantSplitting)
					writer.write(colorCategory[getCurveColorIndexFromCombinedCategoryIndex(cat)]+"\t");
				if (secondCategory != null)
					writer.write(secondCategory[getCurveSecondCategoryIndexFromCombinedCategoryIndex(cat)]+"\t");
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

	private boolean isConsideredForCurve(VisualizationPoint vp, long listMask) {
		if (!isVisibleExcludeNaN(vp) || vp.hvIndex == -1)
			return false;

		if (mCurveRowList == cCurveRowListVisible)
			return true;

		return (vp.record.getFlags() & listMask) != 0;
		}

	private void drawVerticalMeanLine(Rectangle baseGraphRect, Rectangle[][] splitCurveBounds) {
		int catCount = getSplitCurveCategoryCount();
		int[][] count = new int[mHVCount][catCount];
		float[][] xmean = new float[mHVCount][catCount];
		float[][] stdDev = new float[mHVCount][catCount];
		long mask = mTableModel.getListHandler().getListMask(mCurveRowList);
		for (int i=0; i<mDataPoints; i++) {
			if (isConsideredForCurve(mPoint[i], mask)) {
				int cat = (catCount == 1) ? 0 : getSplitCurveCategoryIndex(mPoint[i]);
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
				if (isConsideredForCurve(mPoint[i], mask)) {
					int cat = (catCount == 1) ? 0 : getSplitCurveCategoryIndex(mPoint[i]);
					stdDev[mPoint[i].hvIndex][cat] += (mPoint[i].screenX - xmean[mPoint[i].hvIndex][cat])
											   		* (mPoint[i].screenX - xmean[mPoint[i].hvIndex][cat]);
					}
				}
			}

		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (count[hv][cat] != 0) {
					int vOffset = 0;
					if (isSplitView() && splitCurveBounds == null)    // splitCurveBounds contain offsets already
						vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();

					Rectangle bounds = (splitCurveBounds == null) ? baseGraphRect : splitCurveBounds[hv][cat];
					int ymin = bounds.y + vOffset;
					int ymax = ymin + bounds.height;
	
					if (isEffectiveCurveSplitByColorCategory())
						mG.setColor(mMarkerColor.getColor(getCurveColorIndexFromCombinedCategoryIndex(cat)));

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

	private void drawHorizontalMeanLine(Rectangle baseGraphRect, Rectangle[][] splitCurveBounds) {
		int catCount = getSplitCurveCategoryCount();
		int[][] count = new int[mHVCount][catCount];
		float[][] ymean = new float[mHVCount][catCount];
		float[][] stdDev = new float[mHVCount][catCount];
		long mask = mTableModel.getListHandler().getListMask(mCurveRowList);
		for (int i=0; i<mDataPoints; i++) {
			if (isConsideredForCurve(mPoint[i], mask)) {
				int cat = (catCount == 1) ? 0 : getSplitCurveCategoryIndex(mPoint[i]);
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
				if (isConsideredForCurve(mPoint[i], mask)) {
					int cat = (catCount == 1) ? 0 : getSplitCurveCategoryIndex(mPoint[i]);
					stdDev[mPoint[i].hvIndex][cat] += (mPoint[i].screenY - ymean[mPoint[i].hvIndex][cat])
													* (mPoint[i].screenY - ymean[mPoint[i].hvIndex][cat]);
					}
				}
			}

		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat=0; cat<catCount; cat++) {
				if (count[hv][cat] != 0) {
					int hOffset = 0;
					if (isSplitView() && splitCurveBounds == null)    // splitCurveBounds contain offsets already
						hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();

					Rectangle bounds = (splitCurveBounds == null) ? baseGraphRect : splitCurveBounds[hv][cat];
					int xmin = bounds.x + hOffset;
					int xmax = xmin + bounds.width;

					if (isEffectiveCurveSplitByColorCategory())
						mG.setColor(mMarkerColor.getColor(getCurveColorIndexFromCombinedCategoryIndex(cat)));

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

	private void calculateCurveByFormula(Rectangle baseGraphRect) {
		final int EXPRESSION_CURVE_STEPS = 512; // steps used for full width curve

		if (mCurveExpression == null
		 || mAxisIndex[0] == -1
		 || mAxisIndex[1] == -1
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

		long mask = mTableModel.getListHandler().getListMask(mCurveRowList);
		if ((mCurveInfo & cCurveStandardDeviation) != 0) {
			mCurveStdDev = new float[mHVCount][1];
			int[][] stdDevCount = new int[mHVCount][1];
			for (int i=0; i<mDataPoints; i++) {
				if (isConsideredForCurve(mPoint[i], mask)) {
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

		int catCount = getSplitCurveCategoryCount();

		mCurveXMin = new float[mHVCount][catCount];
		float[][] xmax = new float[mHVCount][catCount];
		mCurveXInc = new float[mHVCount][catCount];
		for (int hv=0; hv<mHVCount; hv++) {
			for (int cat = 0; cat<catCount; cat++) {
				mCurveXMin[hv][cat] = Float.MAX_VALUE;
				xmax[hv][cat] = Float.MIN_VALUE;
				}
			}
		long mask = mTableModel.getListHandler().getListMask(mCurveRowList);
		for (int i=0; i<mDataPoints; i++) {
			if (isConsideredForCurve(mPoint[i], mask)) {
				int cat = (catCount == 1) ? 0 : getSplitCurveCategoryIndex(mPoint[i]);
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
			if (isConsideredForCurve(mPoint[i], mask)) {
				int cat = (catCount == 1) ? 0 : getSplitCurveCategoryIndex(mPoint[i]);
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
				if (isConsideredForCurve(mPoint[i], mask)) {
					int cat = (catCount == 1) ? 0 : getSplitCurveCategoryIndex(mPoint[i]);
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
		for (int cat=0; cat<mCurveY[hv].length; cat++) {
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
				if (isEffectiveCurveSplitByColorCategory())
					mG.setColor(mMarkerColor.getColor(getCurveColorIndexFromCombinedCategoryIndex(cat)));
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
					if (isEffectiveCurveSplitByColorCategory())
						mG.setColor(mMarkerColor.getColor(getCurveColorIndexFromCombinedCategoryIndex(cat)));
					float x = mCurveXMin[hv][cat];
					for (int i=0; i<mCurveY[hv][cat].length-1; i++) {
						mG.draw(new Line2D.Float(x, mCurveY[hv][cat][i], x+mCurveXInc[hv][cat], mCurveY[hv][cat][i+1]));
						x += mCurveXInc[hv][cat];
						}
					}
				}
			}
		}

	private void drawCurveByFormulaArea(int hv, Rectangle graphRect) {
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

	private void drawCurveByFormula(Rectangle baseGraphRect, boolean showFormula) {
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

				if (showFormula && hv == mHVCount-1) {
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

	private void drawFittedLine(Rectangle baseGraphRect, Rectangle[][] splitCurveBounds) {
		int catCount = getSplitCurveCategoryCount();
		int[][] count = new int[mHVCount][catCount];
		float[][] sx = new float[mHVCount][catCount];
		float[][] sy = new float[mHVCount][catCount];
		float[][] sx2 = new float[mHVCount][catCount];
		float[][] sxy = new float[mHVCount][catCount];
		long mask = mTableModel.getListHandler().getListMask(mCurveRowList);
		for (int i=0; i<mDataPoints; i++) {
			if (isConsideredForCurve(mPoint[i], mask)) {
				int cat = (catCount == 1) ? 0 : getSplitCurveCategoryIndex(mPoint[i]);
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
				if (isConsideredForCurve(mPoint[i], mask)) {
					int cat = (catCount == 1) ? 0 : getSplitCurveCategoryIndex(mPoint[i]);
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

				Rectangle bounds = (splitCurveBounds == null) ? baseGraphRect : splitCurveBounds[hv][cat];

				if (isEffectiveCurveSplitByColorCategory())
					mG.setColor(mMarkerColor.getColor(getCurveColorIndexFromCombinedCategoryIndex(cat)));

				if (count[hv][cat]*sx2[hv][cat] == sx[hv][cat]*sx[hv][cat]) {
					float x = sx[hv][cat] / count[hv][cat];
					float ymin = bounds.y;
					if (isSplitView() && splitCurveBounds == null)
						ymin += mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
					drawVerticalLine(x, ymin, ymin+bounds.height, dxy);
					continue;
					}
				if (count[hv][cat]*sxy[hv][cat] == sx[hv][cat]*sy[hv][cat]) {
					float y = sy[hv][cat] / count[hv][cat];
					float xmin = bounds.x;
					if (isSplitView() && splitCurveBounds == null)
						xmin += mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
					drawHorizontalLine(xmin, xmin+bounds.width, y, dxy);
					continue;
					}

				int hOffset = 0;
				int vOffset = 0;
				if (isSplitView() && splitCurveBounds == null) {
					hOffset = mSplitter.getHIndex(hv) * mSplitter.getGridWidth();
					vOffset = mSplitter.getVIndex(hv) * mSplitter.getGridHeight();
					}

				int xmin = bounds.x+hOffset;
				int xmax = xmin+bounds.width;
				int ymin = bounds.y+vOffset;
				int ymax = ymin+bounds.height;

				drawInclinedLine(xmin, xmax, ymin, ymax, m[hv][cat], b[hv][cat], dxy);
				}
			}
		}

	private void drawInclinedLine(int xmin, int xmax, int ymin, int ymax, float m, float b, float stdDev) {
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

		final long mask = mTableModel.getListHandler().getListMask(mCurveRowList);

		mCorrelationCoefficient = new float[mHVCount];
		if (mHVCount == 1) {
			float r = (float)new CorrelationCalculator().calculateCorrelation(
					new INumericalDataColumn() {
						public int getValueCount() {
							return mDataPoints;
							}
						public double getValueAt(int row) {
							return isConsideredForCurve(mPoint[row], mask) ? getAxisValue(mPoint[row].record, 0) : Float.NaN;
							}
						},
					new INumericalDataColumn() {
						public int getValueCount() {
							return mDataPoints;
							}
						public double getValueAt(int row) {
							return isConsideredForCurve(mPoint[row], mask) ? getAxisValue(mPoint[row].record, 1) : Float.NaN;
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
				if (isConsideredForCurve(mPoint[i], mask))
					count[mPoint[i].hvIndex]++;
			float[][][] value = new float[mHVCount][2][];
			for (int hv=0; hv<mHVCount; hv++) {
				value[hv][0] = new float[count[hv]];
				value[hv][1] = new float[count[hv]];
				}
			count = new int[mHVCount];
			for (int i=0; i<mDataPoints; i++) {
				if (isConsideredForCurve(mPoint[i], mask)) {
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
				setSimilarityColors(mBackgroundColor, VisualizationPoint.COLOR_TYPE_MARKER_BG, -1);
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
				if (mChartInfo.mInnerDistance != null || mChartInfo.mAbsValueFactor != null) {
					float width = mChartInfo.mBarWidth;
					if (mChartInfo.useProportionalWidths())
						width *= mChartInfo.getBarWidthFactor(vp.record.getDouble(mMarkerSizeColumn));

					if (mChartInfo.mDoubleAxis == 1) {
						sizeX = width + 2f*GAP;
						sizeY = (mChartInfo.mInnerDistance != null ? mChartInfo.mInnerDistance[hv][cat]
								: Math.abs(vp.record.getDouble(mChartColumn)) * mChartInfo.mAbsValueFactor[hv][cat])
								+ 2f*GAP;
						}
					else {
						sizeX = (mChartInfo.mInnerDistance != null ? mChartInfo.mInnerDistance[hv][cat]
								: Math.abs(vp.record.getDouble(mChartColumn)) * mChartInfo.mAbsValueFactor[hv][cat])
								+ 2f*GAP;
						sizeY = width + 2f*GAP;
						}

					hSizeX = sizeX/2;
					hSizeY = sizeY/2;
					g.draw(new Rectangle2D.Float(vp.screenX-hSizeX, vp.screenY-hSizeY, sizeX, sizeY));
					}
				}
			}
		else if (mChartType == cChartTypeViolins && isVisibleExcludeNaN(vp)) {
			g.draw(new Ellipse2D.Float(vp.screenX-vp.widthOrAngle1/2, vp.screenY-vp.heightOrAngle2/2, vp.widthOrAngle1, vp.heightOrAngle2));
			}
		else if (mChartType == cChartTypePies) {
			if (!mChartInfo.mBarOrPieDataAvailable)
				return;

			int hv = vp.hvIndex;
			int cat = getChartCategoryIndex(vp);
			if (cat != -1) {
				float x = mChartInfo.mPieX[hv][cat];
				float y = mChartInfo.mPieY[hv][cat];
				float r = mChartInfo.mPieSize[hv][cat]/2 + GAP;
				float dif,angle;
				if (mChartInfo.useProportionalFractions()) {
					angle = vp.widthOrAngle1;
					dif = vp.heightOrAngle2 - vp.widthOrAngle1;
					}
				else {
					dif = 360f / (float)mChartInfo.mPointsInCategory[hv][cat];
					angle = dif * vp.chartGroupIndex;
					}
				if (mChartInfo.mPointsInCategory[hv][cat] == 1)
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
			 || mChartType == cChartTypeWhiskerPlot
			 || mChartType == cChartTypeViolins) {
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

			if (mCurveSplitCategoryColumn != cColumnUnassigned
			 && !mTableModel.isColumnTypeCategory(mCurveSplitCategoryColumn))
				mCurveSplitCategoryColumn = cColumnUnassigned;

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
						int[] newColumn = new int[mMultiValueMarkerColumns.length-count];
						int index = 0;
						for (int column : mMultiValueMarkerColumns)
							if (column != cColumnUnassigned)
								newColumn[index++] = column;
						mMultiValueMarkerColumns = newColumn;
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
			if (mCurveSplitCategoryColumn != cColumnUnassigned) {
				mCurveSplitCategoryColumn = columnMapping[mCurveSplitCategoryColumn];
				if (mCurveSplitCategoryColumn == cColumnUnassigned)
					invalidateOffImage(false);
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
				for (int mvmColumn : mMultiValueMarkerColumns)
					if (column == mvmColumn)
						invalidateOffImage(false);

			if (mChartMode != cChartModeCount
			 && mChartMode != cChartModePercent
			 && mChartColumn == column) {
				invalidateOffImage(true);
				}

			if (mCurveSplitCategoryColumn != cColumnUnassigned
			 && !mTableModel.isColumnTypeCategory(mCurveSplitCategoryColumn)) {
				mCurveSplitCategoryColumn = cColumnUnassigned;
				invalidateOffImage(false);
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
			if (mCurveRowList >= 0) {
				if (e.getListIndex() == mCurveRowList) {
					mCurveRowList = cCurveRowListVisible;
					mCurveY = null;
					invalidateOffImage(false);
					}
				else if (mCurveRowList > e.getListIndex()) {
					mCurveRowList--;
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
			if (mCurveRowList >= 0) {	// is a list index
				if (e.getListIndex() == mCurveRowList) {
					mCurveY = null;
					invalidateOffImage(false);
					}
				}
			}

		mBackgroundColor.listChanged(e);
		}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		super.valueChanged(e);

		if (!e.getValueIsAdjusting()) {
			if (mCurveRowList == cCurveRowListSelected) {
				mCurveY = null;
				invalidateOffImage(false);
				}
			}
		}

	@Override
	public void colorChanged(VisualizationColor source) {
		if (source == mBackgroundColor) {
			updateColorIndices(mBackgroundColor, VisualizationPoint.COLOR_TYPE_MARKER_BG);
			mBackgroundValid = false;
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

	public float getBackgroundColorRadius() {
		return mBackgroundColorRadius;
		}

	public float getBackgroundColorFading() {
		return mBackgroundColorFading;
		}

	public void setBackgroundColorConsidered(int considered) {
		if (mBackgroundColorConsidered != considered) {
			mBackgroundColorConsidered = considered;
			mBackgroundValid = false;
			invalidateOffImage(false);
			}
		}

	public void setBackgroundColorRadius(float radius) {
		if (mBackgroundColorRadius != radius) {
			mBackgroundColorRadius = radius;
			mBackgroundValid = false;
			invalidateOffImage(false);
			}
		}

	public void setBackgroundColorFading(float fading) {
		if (mBackgroundColorFading != fading) {
			mBackgroundColorFading = fading;
			mBackgroundValid = false;
			invalidateOffImage(false);
			}
		}

	@Override
	protected void addMarkerTooltips(VisualizationPoint vp, TreeSet<Integer> columnSet, StringBuilder sb) {
		if (mMultiValueMarkerColumns != null) {
			for (int column : mMultiValueMarkerColumns)
		        addTooltipRow(vp.record, column, null, columnSet, sb);
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

	public float getMarkerLabelTransparency() {
		return mMarkerLabelTransparency;
		}

	public float getConnectionLineTransparency() {
		return mConnectionLineTransparency;
		}

	protected Stroke getConnectionStroke() {
		return mConnectionStroke;
		}

	/**
	 * Changes the marker transparency for non-histogram views
	 * @param markerTransparency value from 0.0 to 1.0
	 * @param labelTransparency value from 0.0 to 1.0
	 * @param connectionLineTransparency value from 0.0 to 1.0
	 */
	public void setTransparency(float markerTransparency, float labelTransparency, float connectionLineTransparency) {
		if (mMarkerTransparency != markerTransparency
		 || mMarkerLabelTransparency != labelTransparency
		 || mConnectionLineTransparency != connectionLineTransparency) {
			mMarkerTransparency = markerTransparency;
			mMarkerLabelTransparency = labelTransparency;
			mConnectionLineTransparency = connectionLineTransparency;
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
			if (mChartInfo != null && mChartInfo.mBarOrPieDataAvailable) {
				int catCount = getCategoryVisCount(0)* getCategoryVisCount(1)*mCaseSeparationCategoryCount;
				for (int hv=mHVCount-1; hv>=0; hv--) {
					for (int cat=catCount-1; cat>=0; cat--) {
						float dx = x - mChartInfo.mPieX[hv][cat];
						float dy = mChartInfo.mPieY[hv][cat] - y;
						float radius = Math.round(mChartInfo.mPieSize[hv][cat]/2);
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
								int index = (int)(mChartInfo.mPointsInCategory[hv][cat] * angle/(2*Math.PI));
								if (index>=0 && index<mChartInfo.mPointsInCategory[hv][cat]) {
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
		  || (mChartType == cChartTypeBoxPlot && vp.chartGroupIndex == -1)
		  || (mChartType == cChartTypeViolins && vp.chartGroupIndex == -1))) {   // TODO calculate violin marker distance
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
		 || (mChartType == cChartTypeBoxPlot && p.chartGroupIndex != -1)
		 || (mChartType == cChartTypeViolins && p.chartGroupIndex != -1))    // TODO getViolinFractionSize()
			return getBarFractionSize(p, mChartInfo.mDoubleAxis == 0);
		else
			return getMarkerSize(p);
		}

	@Override
	protected float getMarkerHeight(VisualizationPoint p) {
		// Pie charts don't use this function because marker location is handled
		// by overwriting the findMarker() method.
		if (mChartType == cChartTypeBars
		 || (mChartType == cChartTypeBoxPlot && p.chartGroupIndex != -1)
		 || (mChartType == cChartTypeViolins && p.chartGroupIndex != -1))    // TODO getViolinFractionSize()
			return getBarFractionSize(p, mChartInfo.mDoubleAxis == 1);
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
					  Math.abs(p.record.getDouble(mAxisIndex[mChartInfo.mDoubleAxis])) * mChartInfo.mAbsValueFactor[p.hvIndex][cat]
					: mChartInfo.mInnerDistance[p.hvIndex][cat];
			}
		else {
			return mChartInfo.mBarWidth;
			}
		}

	public void initializeAxis(int axis) {
		super.initializeAxis(axis);

		mCrossHairList.clear();
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
						d.validateView(g, new GenericRectangle(0, 0, maxSize, maxSize), Depictor2D.cModeInflateToMaxAVBL + maxAVBL);
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
			if (mChartType == cChartTypeBars && mChartInfo.mDoubleAxis == axis)
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
			if (mChartType != cChartTypeBars || axis != mChartInfo.mDoubleAxis)
				return null;

			double v = mChartInfo.mAxisMin + position * (mChartInfo.mAxisMax - mChartInfo.mAxisMin);

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
			float scalePosition = (mChartType == cChartTypeBars && axis == mChartInfo.mDoubleAxis) ?
				(mChartInfo.mBarBase - mChartInfo.mAxisMin) / (mChartInfo.mAxisMax - mChartInfo.mAxisMin) - 0.5f + i : i;
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
				if (category.equals(CompoundTableConstants.cRangeNotAvailable)) {
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

			axisStart = mChartInfo.mAxisMin;
			axisLength = mChartInfo.mAxisMax - mChartInfo.mAxisMin;
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
			axisStart = mChartInfo.mAxisMin;
			axisLength = mChartInfo.mAxisMax - mChartInfo.mAxisMin;
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
		float min = (mAxisIndex[axis] == -1) ? mChartInfo.mAxisMin : mAxisVisMin[axis];
		float max = (mAxisIndex[axis] == -1) ? mChartInfo.mAxisMax : mAxisVisMax[axis];
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
			float splittingFactor = (float)Math.pow(Math.max(1, mHVCount), 0.33);

			if (mChartType == cChartTypeBoxPlot || mChartType == cChartTypeWhiskerPlot || mChartType == cChartTypeViolins) {
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
		 || mChartType == cChartTypeViolins
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
				if (mCaseSeparationAxis != -1) {
					float width = mCaseSeparationAxis == 0 ? graphRect.width : graphRect.height;
					float categoryWidth = (mAxisIndex[mCaseSeparationAxis] == cColumnUnassigned) ? width
										 : width / (mAxisVisMax[mCaseSeparationAxis]-mAxisVisMin[mCaseSeparationAxis]);	// mCategoryCount[csAxis]; 	mCategoryCount is undefined for scatter plots
					categoryWidth *= mCaseSeparationValue;
					float csCategoryCount = mTableModel.getCategoryCount(mCaseSeparationColumn);
					csCategoryWidth = categoryWidth / csCategoryCount;
					csOffset = (csCategoryWidth - categoryWidth) / 2;
					}

				int xNaN = Math.round(graphRect.x - mNaNSize[0] * (0.5f * NAN_WIDTH + NAN_SPACING) / (NAN_WIDTH + NAN_SPACING));
				int yNaN = Math.round(graphRect.y + graphRect.height + mNaNSize[1] * (0.5f * NAN_WIDTH + NAN_SPACING) / (NAN_WIDTH + NAN_SPACING));
				if (mChartType == cChartTypeScatterPlot) {
					for (VisualizationPoint vp:mPoint) {
						// calculating coordinates for invisible records also allows to skip coordinate recalculation
						// when the visibility changes (JVisualization3D uses the inverse approach)
						float doubleX = (mAxisIndex[0] == cColumnUnassigned) ? 0.0f : getAxisValue(vp.record, 0);
						float doubleY = (mAxisIndex[1] == cColumnUnassigned) ? 0.0f : getAxisValue(vp.record, 1);
						vp.screenX = Float.isNaN(doubleX) ? xNaN : graphRect.x
										  + (doubleX-mAxisVisMin[0])*graphRect.width / (mAxisVisMax[0]-mAxisVisMin[0]);
						vp.screenY = Float.isNaN(doubleY) ? yNaN : graphRect.y + graphRect.height
										  + (mAxisVisMin[1]-doubleY)*graphRect.height / (mAxisVisMax[1]-mAxisVisMin[1]);
						if (jitterMaxX != 0)
							vp.screenX += (mRandom.nextDouble() - 0.5) * jitterMaxX;
						if (jitterMaxY != 0)
							vp.screenY += (mRandom.nextDouble() - 0.5) * jitterMaxY;

						if (mCaseSeparationAxis != -1) {
							float csShift = csOffset + csCategoryWidth * mTableModel.getCategoryIndex(mCaseSeparationColumn, vp.record);
							if (mCaseSeparationAxis == 0)
								vp.screenX += csShift;
							else
								vp.screenY -= csShift;
							}
						}
					}
				else {	// mChartType == cChartTypeBoxPlot or cChartTypeWhiskerPlot
					boolean xIsDoubleCategory = mChartInfo.mDoubleAxis == 1
											 && mAxisIndex[0] != cColumnUnassigned
											 && mTableModel.isColumnTypeDouble(mAxisIndex[0]);
					boolean yIsDoubleCategory = mChartInfo.mDoubleAxis == 0
											 && mAxisIndex[1] != cColumnUnassigned
											 && mTableModel.isColumnTypeDouble(mAxisIndex[1]);
					for (VisualizationPoint vp:mPoint) {
						if (mChartType == cChartTypeWhiskerPlot
						 || vp.chartGroupIndex == -1) {
							if (mAxisIndex[0] == cColumnUnassigned)
								vp.screenX = graphRect.x + graphRect.width * 0.5f;
							else if (xIsDoubleCategory)
								vp.screenX = graphRect.x + graphRect.width
											* (0.5f + getCategoryIndex(0, vp)) / getCategoryVisCount(0);
							else {
								float doubleX = getAxisValue(vp.record, 0);
								vp.screenX = Float.isNaN(doubleX) ? xNaN : graphRect.x
										  + (doubleX-mAxisVisMin[0])*graphRect.width / (mAxisVisMax[0]-mAxisVisMin[0]);							}
							if (mAxisIndex[1] == cColumnUnassigned)
								vp.screenY = graphRect.y + graphRect.height * 0.5f;
							else if (yIsDoubleCategory)
								vp.screenY = graphRect.y + graphRect.height - graphRect.height
											* (0.5f + getCategoryIndex(1, vp)) / getCategoryVisCount(1);
							else {
								float doubleY = getAxisValue(vp.record, 1);
								vp.screenY = Float.isNaN(doubleY) ? yNaN : graphRect.y + graphRect.height
										  + (mAxisVisMin[1]-doubleY)*graphRect.height / (mAxisVisMax[1]-mAxisVisMin[1]);
								}

							if (jitterMaxX != 0)
								vp.screenX += (mRandom.nextDouble() - 0.5) * jitterMaxX;
							if (jitterMaxY != 0)
								vp.screenY += (mRandom.nextDouble() - 0.5) * jitterMaxY;

							if (mCaseSeparationAxis != -1) {
								float csShift = csOffset + csCategoryWidth * mTableModel.getCategoryIndex(mCaseSeparationColumn, vp.record);
								if (mCaseSeparationAxis == 0)
									vp.screenX += csShift;
								else
									vp.screenY -= csShift;
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
	 * @param graphBounds
	 */
	private void calculateBackground(Rectangle graphBounds) {
		float visFactorX = mPruningBarHigh[0] - mPruningBarLow[0];
		float visFactorY = mPruningBarHigh[1] - mPruningBarLow[1];

		if (visFactorX == 0 || visFactorY == 0) {
			mBackgroundValid = true;
			mMarkerBackgroundImage = null;
			return;
			}

		int pixelPerColor = (int)(1+mBackgroundColorRadius/4) * (mIsHighResolution? 1 : !renderFaster() ? 2 : 4);

		// The background grid has one value every n pixel starting with pixel 0 and last value after last pixel
		int visBGWidth = (graphBounds.width+2*pixelPerColor-1) / pixelPerColor;
		int visBGHeight = (graphBounds.height+2*pixelPerColor-1) / pixelPerColor;
		float rawRadius = mBackgroundColorRadius * (float)Math.sqrt(graphBounds.width * graphBounds.height) / (100 * pixelPerColor);

		// effective radius in background grid space
		int bgRadiusX = Math.max(1, Math.round(rawRadius / visFactorX));
		int bgRadiusY = Math.max(1, Math.round(rawRadius / visFactorY));

		boolean xIsCyclic = mAxisIndex[0] != cColumnUnassigned && (mTableModel.getColumnProperty(mAxisIndex[0],
				CompoundTableModel.cColumnPropertyCyclicDataMax) != null);
		boolean yIsCyclic = mAxisIndex[1] != cColumnUnassigned && (mTableModel.getColumnProperty(mAxisIndex[1],
				CompoundTableModel.cColumnPropertyCyclicDataMax) != null);

		boolean considerVisibleRecords = (mBackgroundColorConsidered == BACKGROUND_VISIBLE_RECORDS) || (mTreeNodeList != null);
		boolean considerAllRecords = (mBackgroundColorConsidered == BACKGROUND_ALL_RECORDS && !considerVisibleRecords);

		// In case of zoomed-in state, these are the invisible (bacause zoomed-out) margins in background grid space
		int bgZoomX0 = mPruningBarLow[0] == 0 ? 0 : Math.round(mPruningBarLow[0] * graphBounds.width / (visFactorX * pixelPerColor));
		int bgZoomX1 = mPruningBarHigh[0] == 1 ? 0 : Math.round((1f-mPruningBarHigh[0]) * graphBounds.width / (visFactorX * pixelPerColor));
		int bgZoomY0 = mPruningBarLow[1] == 0 ? 0 : Math.round(mPruningBarLow[1] * graphBounds.height / (visFactorY * pixelPerColor));
		int bgZoomY1 = mPruningBarHigh[1] == 1 ? 0 : Math.round((1f-mPruningBarHigh[1]) * graphBounds.height / (visFactorY * pixelPerColor));

		// If we zoomed-out width is larger than the influence radius, then the effectively needed margin
		// to be calculated and then propargated to the visible background is only as large as the radius.
		// In case of cyclic data we need the margin always to be as wide as the influence radius.
		int bgMarginX0 = xIsCyclic ? bgRadiusX : considerVisibleRecords ? 0 : Math.min(bgZoomX0, bgRadiusX);
		int bgMarginX1 = xIsCyclic ? bgRadiusX : considerVisibleRecords ? 0 : Math.min(bgZoomX1, bgRadiusX);
		int bgMarginY0 = yIsCyclic ? bgRadiusY : considerVisibleRecords ? 0 : Math.min(bgZoomY0, bgRadiusY);
		int bgMarginY1 = yIsCyclic ? bgRadiusY : considerVisibleRecords ? 0 : Math.min(bgZoomY1, bgRadiusY);

		if (mSplitter == null) {
			mBackgroundHCount = 1;
			mBackgroundVCount = 1;
			}
		else {
			mBackgroundHCount = mSplitter.getHCount();
			mBackgroundVCount = mSplitter.getVCount();
			}

		int bgWidth = visBGWidth + bgMarginX0 + bgMarginX1;
		int bgHeight = visBGHeight + bgMarginY0 + bgMarginY1;
		int bgFullDataXRange = Math.round((float)graphBounds.width / (visFactorX * pixelPerColor));
		int bgFullDataYRange = Math.round((float)graphBounds.height / (visFactorY * pixelPerColor));

		// add all points' RGB color components to respective grid cells
		// consider all points that are less than backgroundColorRadius away from visible area
		float[][][] backgroundR = new float[mHVCount][bgWidth][bgHeight];
		float[][][] backgroundG = new float[mHVCount][bgWidth][bgHeight];
		float[][][] backgroundB = new float[mHVCount][bgWidth][bgHeight];
		float[][][] backgroundC = new float[mHVCount][bgWidth][bgHeight];

		float dataLowX,dataHighX,dataLowY,dataHighY;    // data limits of visible area
		if (mTreeNodeList != null) {
			dataLowX = graphBounds.x;
			dataLowY = graphBounds.y + graphBounds.height;
			dataHighX = graphBounds.x + graphBounds.width;
			dataHighY = graphBounds.y;
			}
		else {
			dataLowX = mAxisVisMin[0];
			dataHighX = mAxisVisMax[0];
			dataLowY = mAxisVisMin[1];
			dataHighY = mAxisVisMax[1];
			}

		float pixelFactor = 1f / pixelPerColor;
		float pixelOffset = pixelFactor / 2f;
		float visDataRangeX = dataHighX - dataLowX;
		float visDataRangeY = dataHighY - dataLowY;
		int listFlagNo = (considerVisibleRecords || considerAllRecords) ? -1
				: mTableModel.getListHandler().getListFlagNo(mBackgroundColorConsidered);

		float csCategoryWidth = 0;
		float csOffset = 0;
		if (mCaseSeparationAxis != -1) {
			float width = mCaseSeparationAxis == 0 ? graphBounds.width : graphBounds.height;
			float categoryWidth = (mAxisIndex[mCaseSeparationAxis] == cColumnUnassigned) ? width
					: width / (mAxisVisMax[mCaseSeparationAxis]-mAxisVisMin[mCaseSeparationAxis]);	// mCategoryCount[csAxis]; 	mCategoryCount is undefined for scatter plots
			categoryWidth *= mCaseSeparationValue;
			float csCategoryCount = mTableModel.getCategoryCount(mCaseSeparationColumn);
			csCategoryWidth = categoryWidth / csCategoryCount;
			csOffset = (csCategoryWidth - categoryWidth) / 2;
		}

		for (VisualizationPoint vp:mPoint) {
			if (considerAllRecords
			 || (considerVisibleRecords && isVisibleExcludeNaN(vp))
			 || (!considerVisibleRecords && vp.record.isFlagSet(listFlagNo)))	{
				float valueX;
				float valueY;
				if (mTreeNodeList != null) {
					valueX = vp.screenX;
					valueY = vp.screenY;
					}
				else {
					valueX = (mAxisIndex[0] == cColumnUnassigned) ? (dataLowX + dataHighX) / 2 : getAxisValue(vp.record, 0);
					valueY = (mAxisIndex[1] == cColumnUnassigned) ? (dataLowY + dataHighY) / 2 : getAxisValue(vp.record, 1);
					}

				if (Float.isNaN(valueX) || Float.isNaN(valueY))
					continue;

				// 0-based coordinates of VP in graphRect (in case of zoomed-in state these may be outside graphRect)
				int grx = Math.round(graphBounds.width * (valueX - dataLowX) / visDataRangeX);
				int gry = Math.round(graphBounds.height * (valueY - dataLowY) / visDataRangeY);

				if (mCaseSeparationAxis != -1) {
					float csShift = csOffset + csCategoryWidth * mTableModel.getCategoryIndex(mCaseSeparationColumn, vp.record);
					if (mCaseSeparationAxis == 0)
						grx += csShift;
					else
						gry -= csShift;
				}

				// 0-based coordinates of closest top-left background grid point of VP.
				// If bgx (and bgy) are in the range between 0->bgWidth, then the VP has influence on the
				// background coloring of parts of the visible area.
				int bgx = bgMarginX0 + grx / pixelPerColor;
				int bgy = bgMarginY0 + gry / pixelPerColor;

				if (xIsCyclic) {
					if (bgx<0)
						bgx += bgFullDataXRange;
					else if (bgx >= bgWidth)
						bgx -= bgFullDataXRange;
					}
				if (yIsCyclic) {
					if (bgy<0)
						bgy += bgFullDataYRange;
					else if (bgy >= bgWidth)
						bgy -= bgFullDataYRange;
					}

				if (bgx >= 0 && bgx+1 < bgWidth && bgy >= 0 && bgy+1 < bgHeight) {
						// influence factors on the four closest grid points
					float xf1 = pixelOffset + pixelFactor * (grx < 0 ? pixelPerColor - 1 + grx % pixelPerColor : grx % pixelPerColor);
					float yf1 = pixelOffset + pixelFactor * (gry < 0 ? pixelPerColor - 1 + gry % pixelPerColor : gry % pixelPerColor);
					float xf0 = 1f - xf1;
					float yf0 = 1f - yf1;

					Color c = mBackgroundColor.getColorList()[((VisualizationPoint2D)vp).backgroundColorIndex];

					// the data point contributes to the four corners of the pixel square depending on its exact location
					float f = xf0 * yf0;
					backgroundR[vp.hvIndex][bgx][bgy] += f * c.getRed();
					backgroundG[vp.hvIndex][bgx][bgy] += f * c.getGreen();
					backgroundB[vp.hvIndex][bgx][bgy] += f * c.getBlue();
					backgroundC[vp.hvIndex][bgx][bgy] += f;

					f = xf1 * yf0;
					backgroundR[vp.hvIndex][bgx+1][bgy] += f * c.getRed();
					backgroundG[vp.hvIndex][bgx+1][bgy] += f * c.getGreen();
					backgroundB[vp.hvIndex][bgx+1][bgy] += f * c.getBlue();
					backgroundC[vp.hvIndex][bgx+1][bgy] += f;

					f = xf0 * yf1;
					backgroundR[vp.hvIndex][bgx][bgy+1] += f * c.getRed();
					backgroundG[vp.hvIndex][bgx][bgy+1] += f * c.getGreen();
					backgroundB[vp.hvIndex][bgx][bgy+1] += f * c.getBlue();
					backgroundC[vp.hvIndex][bgx][bgy+1] += f;

					f = xf1 * yf1;
					backgroundR[vp.hvIndex][bgx+1][bgy+1] += f * c.getRed();
					backgroundG[vp.hvIndex][bgx+1][bgy+1] += f * c.getGreen();
					backgroundB[vp.hvIndex][bgx+1][bgy+1] += f * c.getBlue();
					backgroundC[vp.hvIndex][bgx+1][bgy+1] += f;
					}
				}
			}

		// Create helper array with influence factors to be applied
		float[][] influence = new float[bgRadiusX][bgRadiusY];
		for (int x=0; x<bgRadiusX; x++) {
			float dx = (0.5f + x) / bgRadiusX;
			for (int y=0; y<bgRadiusY; y++) {
				float dy = (0.5f + y) / bgRadiusY;
				float distance = (float)Math.sqrt(dx*dx + dy*dy);
				if (distance <= 1f)
					influence[x][y] = (float)(0.5 + Math.cos(Math.PI*distance) / 2.0);
//					influence[x][y] = 1f - distance;
				}
			}

		float[][][] smoothR = new float[mHVCount][visBGWidth][visBGHeight];
		float[][][] smoothG = new float[mHVCount][visBGWidth][visBGHeight];
		float[][][] smoothB = new float[mHVCount][visBGWidth][visBGHeight];
		float[][][] smoothC = new float[mHVCount][visBGWidth][visBGHeight];

		int marginX = bgZoomX0 + bgZoomX1;
		int marginY = bgZoomY0 + bgZoomY1;

		for (int x=0; x<bgWidth; x++) {
			int xmin = x-bgRadiusX+1-bgMarginX0;
			if (xmin < 0 && (!xIsCyclic || xmin >= -marginX))
				xmin = 0;
			int xmax = x+bgRadiusX-1-bgMarginX0;
			if (xmax >= visBGWidth && (!xIsCyclic || xmax - visBGWidth < marginX))
				xmax = visBGWidth-1;

			for (int y=0; y<bgHeight; y++) {
				int ymin = y-bgRadiusY+1-bgMarginY0;
				if (ymin < 0 && (!yIsCyclic || ymin >= -marginY))
					ymin = 0;
				int ymax = y+bgRadiusY-1-bgMarginY0;
				if (ymax >= visBGHeight && (!yIsCyclic || ymax - visBGHeight < marginY))
					ymax = visBGHeight-1;

				for (int hv=0; hv<mHVCount; hv++) {
					if (backgroundC[hv][x][y] > 0f) {
						for (int ix=xmin; ix<=xmax; ix++) {
							if ((ix < 0 && ix >= -marginX) || (ix >= visBGWidth && ix < visBGWidth + marginX))
								continue;

							int dx = Math.abs(x-ix-bgMarginX0);

							int destX = ix;
							if (destX < 0)
								destX += visBGWidth + marginX;
							else if (destX >= visBGWidth)
								destX -= visBGWidth + marginX;

							for (int iy=ymin; iy<=ymax; iy++) {
								if ((iy < 0 && iy >= -marginY) || (iy >= visBGHeight && iy < visBGHeight + marginY))
									continue;

								int dy = Math.abs(y-iy-bgMarginY0);

								int destY = iy;
								if (destY < 0)
									destY += visBGHeight + marginY;
								else if (destY >= visBGHeight)
									destY -= visBGHeight + marginY;

								if (influence[dx][dy] > 0f) {
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
			for (int x=0; x<visBGWidth; x++)
				for (int y=0; y<visBGHeight; y++)
					if (max < smoothC[hv][x][y])
						max = smoothC[hv][x][y];

		float fading = (float)Math.exp(Math.log(1.0)-mBackgroundColorFading/20*(Math.log(1.0)-Math.log(0.1)));

		short[][][][] background = new short[4][mHVCount][visBGWidth][visBGHeight]; // A,B,G,R
		for (int hv=0; hv<mHVCount; hv++) {
			for (int x=0; x<visBGWidth; x++) {
				for (int y=0; y<visBGHeight; y++) {
					if (smoothC[hv][x][y] == 0) {
						background[0][hv][x][y] = (byte)0;
						background[1][hv][x][y] = (byte)0;
						background[2][hv][x][y] = (byte)0;
						background[3][hv][x][y] = (byte)0;
						}
					else {
						float f = (float)Math.exp(fading*Math.log(smoothC[hv][x][y] / max));
						background[0][hv][x][y] = (short)(f * 255);
						background[1][hv][x][y] = (short)(smoothB[hv][x][y] / smoothC[hv][x][y]);
						background[2][hv][x][y] = (short)(smoothG[hv][x][y] / smoothC[hv][x][y]);
						background[3][hv][x][y] = (short)(smoothR[hv][x][y] / smoothC[hv][x][y]);
						}
					}
				}
			}

		float[][] f = new float[pixelPerColor][pixelPerColor];
		for (int i=0; i<pixelPerColor; i++)
			for (int j=0; j<pixelPerColor; j++)
				f[i][j] = (1f - (pixelOffset + pixelFactor * i)) * (1f - (pixelOffset + pixelFactor * j));

		int d = pixelPerColor - 1;
		mMarkerBackgroundImage = new BufferedImage[mHVCount];
		for (int hv=0; hv<mHVCount; hv++) {
			mMarkerBackgroundImage[hv] = new BufferedImage(graphBounds.width, graphBounds.height, BufferedImage.TYPE_4BYTE_ABGR);
			byte[] data = ((DataBufferByte)mMarkerBackgroundImage[hv].getRaster().getDataBuffer()).getData();
			int index = 0;
			for (int y=graphBounds.height-1; y>=0; y--) {
				int sy = y / pixelPerColor;
				int iy = y % pixelPerColor;
				for (int x=0; x<graphBounds.width; x++) {
					int sx = x / pixelPerColor;
					int ix = x % pixelPerColor;
					data[index++] = (byte)(f[iy][ix] * background[0][hv][sx][sy]
								  + f[iy][d-ix] * background[0][hv][sx+1][sy]
								  + f[d-iy][ix] * background[0][hv][sx][sy+1]
								  + f[d-iy][d-ix] * background[0][hv][sx+1][sy+1]);
					data[index++] = (byte)(f[iy][ix] * background[1][hv][sx][sy]
								  + f[iy][d-ix] * background[1][hv][sx+1][sy]
								  + f[d-iy][ix] * background[1][hv][sx][sy+1]
								  + f[d-iy][d-ix] * background[1][hv][sx+1][sy+1]);
					data[index++] = (byte)(f[iy][ix] * background[2][hv][sx][sy]
								  + f[iy][d-ix] * background[2][hv][sx+1][sy]
								  + f[d-iy][ix] * background[2][hv][sx][sy+1]
								  + f[d-iy][d-ix] * background[2][hv][sx+1][sy+1]);
					data[index++] = (byte)(f[iy][ix] * background[3][hv][sx][sy]
								  + f[iy][d-ix] * background[3][hv][sx+1][sy]
								  + f[d-iy][ix] * background[3][hv][sx][sy+1]
								  + f[d-iy][d-ix] * background[3][hv][sx+1][sy+1]);
					}
				}
			}

		mBackgroundValid = true;
		}

	private void drawBackground(Graphics2D g, Rectangle graphRect, int hvIndex) {
		ViewPort port = new ViewPort();

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

		if (mMarkerBackgroundImage != null && mMarkerBackgroundImage[hvIndex] != null) {
			g.drawImage(mMarkerBackgroundImage[hvIndex], graphRect.x, graphRect.y, null);
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
		  || (mChartType == cChartTypeBars && mChartInfo.mDoubleAxis == 0))) {
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
		  || (mChartType == cChartTypeBars && mChartInfo.mDoubleAxis == 1))) {
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

	protected void transformScaleLinePositions(int axis, float shift, float scale) {
		for (JVisualization2D.ScaleLine sl : mScaleLineList[axis])
			sl.position = shift + sl.position * scale;
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
				Rectangle bounds = getViewBounds(graphRect.x, graphRect.y);
				x = Math.max(graphRect.x, Math.min(graphRect.x+graphRect.width-w, x));
				y = Math.min(y+crossHairBorder, bounds.y+bounds.height-h-crossHairBorder);
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
		depictor.validateView(g, new GenericRectangle(x, y, w, h), Depictor2D.cModeInflateToMaxAVBL+maxAVBL);
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

	public boolean isCurveAreaTruncated() {
		return (mCurveInfo & cCurveTruncateArea) != 0;
		}

	public boolean isShowStandardDeviationArea() {
		return (mCurveInfo & cCurveStandardDeviation) != 0;
		}

	/**
	 * @return true if user selected curve splitting by marker color AND marker colors are category mode
	 */
	public boolean isCurveSplitByColorCategory() {
		return (mCurveInfo & cCurveSplitByCategory) != 0
			 && mMarkerColor.getColorColumn() != cColumnUnassigned
			 && mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories;
		}

	public int getCurveSplitSecondCategoryColumn() {
		return mCurveSplitCategoryColumn;
		}

	/**
	 * @return true if (user selected curve splitting by marker color AND/OR curve split column is also marker color column) AND marker colors are category mode
	 */
	private boolean isEffectiveCurveSplitByColorCategory() {
		return isCurveSplitByColorCategory()
			|| (mCurveSplitCategoryColumn != cColumnUnassigned
			 && mCurveSplitCategoryColumn == mMarkerColor.getColorColumn()
			 && mMarkerColor.getColorListMode() == VisualizationColor.cColorListModeCategories);
		}

	/**
	 * @return true if user selected curve splitting by second column AND second column is not marker color column with category mode
	 */
	private boolean isEffectiveCurveSplitBySecondCategory() {
		return mCurveSplitCategoryColumn != cColumnUnassigned
			&& (mCurveSplitCategoryColumn != mMarkerColor.getColorColumn()
			 || mMarkerColor.getColorListMode() != VisualizationColor.cColorListModeCategories);
		}

	/**
	 * If curves are split by the color category and/or by another selected category column,
	 * then this method returns the total number of combined categories.
	 * It does not consider view splitting.
	 * @return 1, color category count, second category count, or color category count * second category count
	 */
	private int getSplitCurveCategoryCount() {
		int catCount = 1;
		if (isEffectiveCurveSplitByColorCategory())
			catCount = mTableModel.getCategoryCount(mMarkerColor.getColorColumn());

		if (isEffectiveCurveSplitBySecondCategory())
			catCount *= mTableModel.getCategoryCount(mCurveSplitCategoryColumn);

		return catCount;
		}

	/**
	 * If curves are split by the color category and/or by  another selected category column,
	 * then this method returns a combined category index for the given visualization point.
	 * @param vp
	 * @return
	 */
	private int getSplitCurveCategoryIndex(VisualizationPoint vp) {
		int colorIndex = 0;
		if (isEffectiveCurveSplitByColorCategory())
			colorIndex = vp.markerColorIndex - VisualizationColor.cSpecialColorCount;

		if (isEffectiveCurveSplitBySecondCategory())
			return colorIndex * mTableModel.getCategoryCount(mCurveSplitCategoryColumn)
					+ mTableModel.getCategoryIndex(mCurveSplitCategoryColumn, vp.record);

		return colorIndex;
		}

	private int getCurveColorIndexFromCombinedCategoryIndex(int cat) {
		return isEffectiveCurveSplitBySecondCategory() ? cat / mTableModel.getCategoryCount(mCurveSplitCategoryColumn) : cat;
		}

	private int getCurveSecondCategoryIndexFromCombinedCategoryIndex(int cat) {
		return cat % mTableModel.getCategoryCount(mCurveSplitCategoryColumn);
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

	public void freezeCrossHair() {
		Rectangle bounds = getGraphBounds(mPopupX, mPopupY, true);
		if (bounds != null && showCrossHair())
			mCrossHairList.add(new GraphPoint(mPopupX, mPopupY, bounds));
		invalidateOffImage(false);
		}

	public void clearCrossHairs() {
		mCrossHairList.clear();
		invalidateOffImage(false);
		}

	/**
	 * @return non-log 'x,y' coordinate pairs of all crosshairs ';' separated
	 */
	public String getCrossHairList() {
		StringBuilder list = new StringBuilder();
		for (GraphPoint p:mCrossHairList) {
			if (list.length() != 0)
				list.append(";");
			list.append(p.toString());
			}

		return list.toString();
		}

	/**
	 * @param coords non-log 'x,y' coordinate pairs of all crosshairs ';' separated
	 */
	public void setCrossHairList(String coords) {
		mCrossHairList.clear();
		if (coords != null) {
			String[] points = coords.split(";");
			for (String point:points) {
				GraphPoint p = new GraphPoint(point);
				if (p.isValid())
					mCrossHairList.add(p);
				}
			}
		invalidateOffImage(false);
		}

	public void setCurveMode(int mode, int rowList, boolean drawStdDevRange, boolean truncateArea, boolean splitByCategory, int splitCurveColumn) {
		if (!getTableModel().isColumnTypeCategory(splitCurveColumn))
			splitCurveColumn = cColumnUnassigned;
		if (mode == cCurveModeNone)
			rowList = cCurveRowListVisible;

		int newInfo = mode
					+ (drawStdDevRange ? cCurveStandardDeviation : 0)
					+ (truncateArea ? cCurveTruncateArea : 0)
					+ (splitByCategory ? cCurveSplitByCategory : 0);

		if (mCurveInfo != newInfo
		 || mCurveSplitCategoryColumn != splitCurveColumn
		 || rowList != mCurveRowList) {
			if (splitByCategory != ((mCurveInfo & cCurveSplitByCategory) != 0)
			 || splitCurveColumn != mCurveSplitCategoryColumn
			 || rowList != mCurveRowList
			 || (mode != cCurveModeByFormulaShow && mode != cCurveModeByFormulaHide))
				mCurveY = null;

			mCurveInfo = newInfo;
			mCurveRowList = rowList;
			mCurveSplitCategoryColumn = splitCurveColumn;
			invalidateOffImage(false);
			}
		}

	public int getCurveRowList() {
		return mCurveRowList;
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
			if (getCurveMode() == cCurveModeByFormulaShow || getCurveMode() == cCurveModeByFormulaHide)
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

	@Override
	protected int getStringWidth(String s) {
		return (int)mG.getFontMetrics().getStringBounds(s, mG).getWidth();
		}

	/**
	 * Sets a new sans-serif, plain font with size: factor * mFontHeight.
	 * mFontHeight is the standard reference font size during drawing.
	 * The size is properly scaled down in case of a split view.
	 * @param factor
	 * @return used scaled font size
	 */
	protected int setRelativeFontHeightAndScaleToSplitView(float factor) {
		int fontSize = Math.round(scaleIfSplitView(factor * mFontHeight));
		setFontHeight(fontSize);
		return fontSize;
		}

	protected float getFontHeightScaledToSplitView() {
		return scaleIfSplitView(mFontHeight);
	}

	private void setFontHeightAndScaleToSplitView(float h) {
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

	protected void drawMolecule(StereoMolecule mol, Color color, GenericRectangle rect, int mode, int maxAVBL) {
		Depictor2D d = new Depictor2D(mol, Depictor2D.cDModeSuppressChiralText);
		d.validateView(mG, rect, mode+maxAVBL);
		d.setOverruleColor(color, null);
		d.paint(mG);
		}

@Override
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

	protected LabelHelper createLabelHelper(Rectangle baseBounds, Rectangle baseGraphRect) {
		return new LabelHelper(baseBounds, baseGraphRect);
		}

	protected class LabelHelper {
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

		public void drawLabelLines(VisualizationPoint vp, Color outlineColor, Composite composite) {
			Composite original = null;
			if (composite != null) {
				original = mG.getComposite();
				mG.setComposite(composite);
				}

			for (int j = 0; j<mLabelColumn.length; j++) {
				if (mLabelColumn[j] != -1) {
					if (composite != null && original == null) {
						original = mG.getComposite();
						mG.setComposite(composite);
						}

					drawMarkerLabelLine(vp, mLabelInfo[j], outlineColor);
					}
				}

			if (original != null)
				mG.setComposite(original);
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

	class GraphPoint {
		private float valueX,valueY;

		public GraphPoint(String coords) {
			valueX = Float.NaN;
			valueY = Float.NaN;
			String[] coord = coords.split(",");
			if (coord.length == 2) {
				try { valueX = Float.parseFloat(coord[0]); } catch (NumberFormatException nfe) {}
				try { valueY = Float.parseFloat(coord[1]); } catch (NumberFormatException nfe) {}
				}
			}

		public GraphPoint(int x, int y, Rectangle bounds) {
			// relative position in zoomed graph
			float positionX = (x-bounds.x)/(float)bounds.width;
			float positionY = (bounds.y+bounds.height-y)/(float)bounds.height;

			// data values at axis positions
			valueX = mAxisVisMin[0] + positionX * (mAxisVisMax[0] - mAxisVisMin[0]);
			valueY = mAxisVisMin[1] + positionY * (mAxisVisMax[1] - mAxisVisMin[1]);

			if (mAxisVisRangeIsLogarithmic[0])
				valueX = (float)Math.pow(10, valueX);
			if (mAxisVisRangeIsLogarithmic[1])
				valueY = (float)Math.pow(10, valueY);
			}

		public boolean isValid() {
			return !Float.isNaN(valueX) && !Float.isNaN(valueX);
			}

		/**
		 * @return relative x-position on currently zoomed axis
		 */
		public double getRelativeX() {
			if (mPruningBarLow[0] == mPruningBarHigh[0])
				return 0.5f;

			return ((mAxisVisRangeIsLogarithmic[0] ? Math.log10(valueX) : valueX) - mAxisVisMin[0]) / (mAxisVisMax[0] - mAxisVisMin[0]);
			}

		/**
		 * @return relative y-position on currently zoomed axis
		 */
		public double getRelativeY() {
			if (mPruningBarLow[1] == mPruningBarHigh[1])
				return 0.5f;

			return ((mAxisVisRangeIsLogarithmic[1] ? Math.log10(valueY) : valueY) - mAxisVisMin[1]) / (mAxisVisMax[1] - mAxisVisMin[1]);
			}

		public String toString() {
			return valueX+","+valueY;
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
					AxisDataRange range = calculateDataMinAndMax(i);
					min[i] = range.scaledMin();
					max[i] = range.scaledMax();
					}
				visMin[i] = mAxisVisMin[i];
				visMax[i] = mAxisVisMax[i];
				}
			}

		float getRange(int dimension) {
			return max[dimension] - min[dimension];
			}
		}
	}
