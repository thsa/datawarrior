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

import com.actelion.research.chem.*;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.MarkerLabelDisplayer;
import com.actelion.research.table.category.CategoryList;
import com.actelion.research.table.category.CategoryMolecule;
import com.actelion.research.table.model.CompoundListSelectionModel;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.ColorHelper;
import com.actelion.research.util.CursorHelper;
import com.actelion.research.util.DoubleFormat;
import org.jmol.g3d.Graphics3D;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.print.PageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.actelion.research.table.view.VisualizationColor.cUseAsFilterColor;

public class JVisualization3D extends JVisualization implements ComponentListener,RotationListener {
	private static final long serialVersionUID = 0x20061002;

	private static final int[] SUPPORTED_CHART_TYPE = { cChartTypeScatterPlot, /*cChartTypeWhiskerPlot, cChartTypeBoxPlot,*/ cChartTypeBars };

	public static final int DEFAULT_GRAPH_FACE_COLOR = 0xFFECECFA;

	private static final Point3f[][] cPoint = {
		/* spere */		{ new Point3f( 0.0f,  0.0f,  0.0f) },
		/* cube  */		{ new Point3f(-0.5f, -0.5f, -0.5f), new Point3f( 0.5f, -0.5f, -0.5f),
							new Point3f(-0.5f,  0.5f, -0.5f), new Point3f(-0.5f, -0.5f,  0.5f),
							new Point3f( 0.5f,  0.5f, -0.5f), new Point3f( 0.5f, -0.5f,  0.5f),
							new Point3f(-0.5f,  0.5f,  0.5f), new Point3f( 0.5f,  0.5f,  0.5f) },
		/* cone */		{ new Point3f( 0.0f,  0.0f, -0.52f), new Point3f( 0.0f,  0.0f,  1.04f) },
		/* cylinder */	{ new Point3f( 0.0f,  0.0f,  0.5f), new Point3f( 0.0f,  0.0f, -0.5f) },
		/* tetraeder */	{ new Point3f( 1.02f, 0.0f, 0.72f), new Point3f(-1.02f, 0.0f, 0.72f),
						  new Point3f(0.0f, -1.02f, -0.72f), new Point3f(0.0f, 1.02f, -0.72f) },
		/* stick */		{ new Point3f(-0.35f, -1.0f, -0.35f), new Point3f( 0.35f, -1.0f, -0.35f),
						  new Point3f(-0.35f,  1.0f, -0.35f), new Point3f(-0.35f, -1.0f,  0.35f),
						  new Point3f( 0.35f,  1.0f, -0.35f), new Point3f( 0.35f, -1.0f,  0.35f),
						  new Point3f(-0.35f,  1.0f,  0.35f), new Point3f( 0.35f,  1.0f,  0.35f) },
		/* octaeder */	{ new Point3f( 0.0f, 0.0f, 0.91f), new Point3f( 0.64f, 0.64f, 0.0f),
							new Point3f(-0.64f, 0.64f, 0.0f), new Point3f(-0.64f, -0.64f,  0.0f),
							new Point3f( 0.64f, -0.64f, 0.0f), new Point3f( 0.0f,  0.0f, -0.91f) }
		};
	private static final int cPointOfFace[][][] = {	// clockwise, if looked at from outside
		/* spere */		null,
		/* cube */		{ { 0, 1, 2, 4 }, { 0, 3, 1, 5 }, { 0, 2, 3, 6 },
						  { 1, 5, 4, 7 }, { 2, 4, 6, 7 }, { 3, 6, 5, 7 } },
		/* cone */		null,
		/* cylinder */	null,
		/* tetraeder */ { { 0, 1, 3 }, { 0, 3, 2 }, { 0, 2, 1 }, { 1, 2, 3 } },
		/* stick */		{ { 0, 1, 2, 4 }, { 0, 3, 1, 5 }, { 0, 2, 3, 6 },
						  { 1, 5, 4, 7 }, { 2, 4, 6, 7 }, { 3, 6, 5, 7 } },
		/* octaeder */  { { 0, 2, 1 }, { 0, 3, 2 }, { 0, 4, 3 }, { 0, 1, 4 },
						  { 1, 2, 5 }, { 2, 3, 5 }, { 3, 4, 5 }, { 4, 1, 5 } }
		};
	private static final float[] cDiameter = {
			/* spere */		1.24f,
			/* cube */	  0.0f,
			/* cone */		1.56f,
			/* cylinder */	1.13f
			};

	private static final Point3f[] cGraphPoint = {
							new Point3f(-1.0f, -1.0f, -1.0f), new Point3f( 1.0f, -1.0f, -1.0f),
							  new Point3f(-1.0f,  1.0f, -1.0f), new Point3f(-1.0f, -1.0f,  1.0f),
							  new Point3f( 1.0f,  1.0f, -1.0f), new Point3f( 1.0f, -1.0f,  1.0f),
							  new Point3f(-1.0f,  1.0f,  1.0f), new Point3f( 1.0f,  1.0f,  1.0f) };
	private static final int cEdgeOfFace[][] = { { 0, 1, 3, 5 }, { 0, 2, 4, 7 }, { 1, 2, 6, 8 },
												 { 3, 4, 9, 10 }, { 5, 6, 9, 11 }, { 7, 8, 10, 11 } };
	private static final int cCornerOfEdge[][] = { { 0, 1 }, { 0, 2 }, { 0, 3 }, { 1, 4 },
												   { 1, 5 }, { 2, 4 }, { 2, 6 }, { 3, 5 },
												   { 3, 6 }, { 4, 7 }, { 5, 7 }, { 6, 7 }, };

	public static final String[] SCALE_MODE_TEXT = { "Show all scales", "Hide all scales" };
	public static final String[] GRID_MODE_TEXT = { "Show grid", "Hide grid" };

	private static final int STEREO_CURSOR_WIDTH = 12;
	private static final int STEREO_CURSOR_LENGTH = 40;
	private static final int MAX_FONT_HEIGHT = 32;

	public static final int cAvailableShapeCount = cPoint.length;
	private static final int cSphere = 0;
	private static final int cCube = 1;
	private static final int cCone = 2;
	private static final int cCylinder = 3;

	public static final String STEREO_MODE_PROPERTY = "actelion.stereo";
//	public static final String STEREO_MODE_STRING_SIDE_BY_SIDE = "parallel";
	public static final String STEREO_MODE_STRING_3DTV_SIDE_BY_SIDE = "sideBySide3DTV";
//  public static final String STEREO_MODE_STRING_RED_BLUE = "redBlue";
	public static final String STEREO_MODE_STRING_H_INTERLACE_LEFT_EYE_FIRST = "hInterlacedLeftEyeFirst";
	public static final String STEREO_MODE_STRING_H_INTERLACE_RIGHT_EYE_FIRST = "hInterlacedRightEyeFirst";
	public static final String STEREO_MODE_STRING_V_INTERLACE = "vInterlaced";

	public static final int STEREO_MODE_NONE = 0;
	public static final int STEREO_MODE_H_INTERLACE_LEFT_EYE_FIRST = 1;
	public static final int STEREO_MODE_H_INTERLACE_RIGHT_EYE_FIRST = 2;
	public static final int STEREO_MODE_V_INTERLACE = 3;
	public static final int STEREO_MODE_3DTV_SIDE_BY_SIDE = 4;

	private static final int DRAG_MODE_NONE = 0;
	private static final int DRAG_MODE_FLEXIBLE = 1;
	private static final int DRAG_MODE_HORIZONTAL = 2;
	private static final int DRAG_MODE_VERTICAL = 3;

	private static final int cStereoEyeDistance = 100;

	private static final float cScreenZoom = 0.29f;
	private static final float cMarkerSize = 0.018f;
	private static final float cConnectionLineWidth = 0.012f;
	private static final float cLocation = 8.0f;
	private static final float cMolsizeFactor = 0.071f;
	private static final float cZoomStopMolCount = 6.3f;

	private static final int cPrintScaling = 2;

	private Frame				mParentFrame;
	private float[][]			mMatrix,mRotation;
	private Point3i[]			mScreenCorner;
	private int[]				mMoleculeAxisIndex,mMoleculeAxisSize;
	private boolean[]			mFaceHidden,mEdgeHidden,mMetaCoordsValid;
	private int					mScreenCenterX,mScreenCenterY,mScreenCenterOffset,
								mCurrentFontSize3D,mStereoMode,mContentScaling,mAAFactor,
								mThreadCount,mGraphFaceColor,mDragMode;
	private float				mScreenZoom,mEyeOffset;
	private boolean				mIsStereo,mDepthOrderValid,mIsAdjusting,mPopupVisible,mIsHighResolution;
	private StereoMolecule[][]	mScaleMolecule;
	private float[][]			mMoleculeOffsetX,mMoleculeOffsetY,mMetaBarPosition,mMetaBarColorEdge;
	private Graphics3D			mG3D,m2ndG3D;
	private ComposedObject[]	mComposedMarker;
	private Image				mNonHighlightedImage;
	private BufferedImage		mStereoImage;
	private Matrix3f			mNullRotationMatrix;
	private ExecutorService		mExecutor;
	private V3DWorker[]			mV3DWorker;
	private ArrayList<RotationListener> mRotationListenerList;

	public JVisualization3D(Frame owner,
							CompoundTableModel tableModel,
							CompoundListSelectionModel selectionModel) {
		super(tableModel, selectionModel, 3);

		mParentFrame = owner;
		mMatrix = new float[3][3];
		mRotation = new float[3][3];
		mRotation[0][0] = 1.0f;
		mRotation[1][1] = 1.0f;
		mRotation[2][2] = 1.0f;

		mComposedMarker = new ComposedObject[cAvailableShapeCount];
		for (int i=0; i<cAvailableShapeCount; i++)
			mComposedMarker[i] = new ComposedObject(i, cPoint[i], cPointOfFace[i]);

		mScreenCorner = new Point3i[8];
		for (int i=0; i<8; i++)
			mScreenCorner[i] = new Point3i();

		mFaceHidden = new boolean[6];
		mEdgeHidden = new boolean[12];
		mMetaCoordsValid = new boolean[3];
		mPoint = new VisualizationPoint3D[0];
		mMoleculeAxisIndex = new int[3];
		mMoleculeAxisIndex[0] = cColumnUnassigned;
		mMoleculeAxisIndex[1] = cColumnUnassigned;
		mMoleculeAxisIndex[2] = cColumnUnassigned;
		mMoleculeAxisSize = new int[3];
		mScaleMolecule = new StereoMolecule[3][];
		mMoleculeOffsetX = new float[3][];
		mMoleculeOffsetY = new float[3][];

		mNullRotationMatrix = new Matrix3f();
		mNullRotationMatrix.setIdentity();

		mMetaBarPosition = new float[3][];

		mIsStereo = false;
		mGraphFaceColor = DEFAULT_GRAPH_FACE_COLOR;
		
		String mode = DataWarrior.getPreferences().get(STEREO_MODE_PROPERTY, null);
		mStereoMode = STEREO_MODE_H_INTERLACE_LEFT_EYE_FIRST;	// the default
		if (mode != null) {
			mParentFrame.addComponentListener(this);
			if (mode.equals(STEREO_MODE_STRING_H_INTERLACE_LEFT_EYE_FIRST))
				mStereoMode = STEREO_MODE_H_INTERLACE_LEFT_EYE_FIRST;
			else if (mode.equals(STEREO_MODE_STRING_H_INTERLACE_RIGHT_EYE_FIRST))
				mStereoMode = STEREO_MODE_H_INTERLACE_RIGHT_EYE_FIRST;
			else if (mode.equals(STEREO_MODE_STRING_V_INTERLACE))
				mStereoMode = STEREO_MODE_V_INTERLACE;
			else if (mode.equals(STEREO_MODE_STRING_3DTV_SIDE_BY_SIDE))
				mStereoMode = STEREO_MODE_3DTV_SIDE_BY_SIDE;
			}

		mThreadCount = Runtime.getRuntime().availableProcessors();
		if (mThreadCount != 1) {
			mExecutor = Executors.newFixedThreadPool(mThreadCount);
			mV3DWorker = new V3DWorker[mThreadCount];
			for (int t=0; t<mThreadCount; t++)
				mV3DWorker[t] = new V3DWorker(t);
			}

		mRotationListenerList = new ArrayList<RotationListener>();

		initialize();
		}

	public void cleanup() {
		super.cleanup();
		if (mG3D != null)
			mG3D.destroy();
		if (m2ndG3D != null)
			m2ndG3D.destroy();
		}

	public void componentResized(ComponentEvent e) {}
	public void componentShown(ComponentEvent e) {}
	public void componentHidden(ComponentEvent e) {}

	public void componentMoved(ComponentEvent e) {
		if (mIsStereo)
			invalidateOffImage(false);
		}

	@Override
	public boolean supportsLabelPositionOptimization() {
		return false;
		}

	public synchronized void paintComponent(Graphics g) {
		mIsHighResolution = false;
		boolean antialiasing = !mIsAdjusting;
		mAAFactor = antialiasing ? 2 : 1;
		float retinaFactor = HiDPIHelper.getRetinaScaleFactor();
		mContentScaling = (int)(retinaFactor * mAAFactor);

		Dimension panelSize = getSize();
		Dimension renderSize = (retinaFactor == 1f) ? panelSize
				: new Dimension((int)(panelSize.width*retinaFactor), (int)(panelSize.height*retinaFactor));
		if (mG3D == null
		 || mG3D.getRenderWidth() != panelSize.width*mContentScaling
		 || mG3D.getRenderHeight() != panelSize.height*mContentScaling) {
			mCoordinatesValid = false;
			}

		if (!mCoordinatesValid)
			mOffImageValid = false;

		if (!mOffImageValid) {
			mFontHeight = (int)(mRelativeFontSize * retinaFactor * Math.sqrt(panelSize.width*panelSize.height) / 50f);

			Image image = paintAllOnImage(g, renderSize, antialiasing, null);
			g.drawImage(image, 0, 0, panelSize.width, panelSize.height, this);

			if (mNonHighlightedImage == null
			 || mNonHighlightedImage.getWidth(null) != renderSize.width
			 || mNonHighlightedImage.getHeight(null) != renderSize.height)
				mNonHighlightedImage = createImage(renderSize.width, renderSize.height);

			mNonHighlightedImage.getGraphics().drawImage(image, 0, 0, null);

			mOffImageValid = true;
			}
		else {
			g.drawImage(mNonHighlightedImage, 0, 0, panelSize.width, panelSize.height, this);
			if (!mIsAdjusting && mHighlightedPoint != null)
				drawHighlightedMarker(g);

			drawSelectionOutline(g);
			}
		drawHighlightedLabelOutline(g);
		paintTouchIcon(g);

		if (!mIsHighResolution)
			paintMessages(g, 0, renderSize.width);
		}

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
		mCoordinatesValid = false;

		mFontHeight = (int)(mRelativeFontSize * bounds.width / 60f);

		boolean antialiasing = true;
		mContentScaling = (antialiasing) ? 2 : 1;

		Image image = paintAllOnImage(g, new Dimension(bounds.width, bounds.height), antialiasing, null);

		if (isPrinting)
			// fontScaling was also used to inflate bounds to gain resolution
			// and has to be compensated by inverted scaling of the g2D
			g.scale(1.0/fontScaling, 1.0/fontScaling);

		   g.drawImage(image, bounds.x, bounds.y, null);

		mCoordinatesValid = false;
		}

	public Image getViewImage(Rectangle bounds, float fontScaling, int stereoMode) {
		// JMol has a problem with transparent BG and antializing: see Swing3D.allocateImage()
		// Therefore transparent backgrounds are not supported in 3D-View.

		// font sizes are optimized for screen resolution are need to be scaled by fontScaling
		mCoordinatesValid = false;

		// font size limitations used to cause different view layouts when resizing a view, TLS 20-Dez-2013
		mFontHeight = (int)(mRelativeFontSize * bounds.width / 60f);

		boolean antialiasing = true;
		mContentScaling = (antialiasing) ? 2 : 1;

		boolean screenIsStereo = mIsStereo;
		int screenStereoMode = mStereoMode;

		mIsStereo = (stereoMode != STEREO_MODE_NONE);
		mStereoMode = stereoMode;

		Image image = paintAllOnImage(getGraphics(), new Dimension(bounds.width, bounds.height), antialiasing, null);

		mCoordinatesValid = false;

		mIsStereo = screenIsStereo;
		mStereoMode = screenStereoMode;
		
		return image;
		}

	/**
	 * Depending on clipRect creates and returns either a complete view image with
	 * legend and none of the markers highlighted (if clipRect==null) or an updated
	 * picture with just the area updated that contains the highlightedMarker. In this
	 * case clipRect is filled to define the region to be updated.
	 * @param g
	 * @param size not used if clipRect != null
	 * @param antialiasing
	 * @param clipRect if clipRect!=null then draw mActiveHighlighted in highlight color
	 * @return
	 */
	private Image paintAllOnImage(Graphics g, Dimension size, boolean antialiasing, Rectangle clipRect) {
		if (!mIsStereo) {
			mEyeOffset = 0;
			mScreenCenterOffset = 0;
			}
		else {
				// mEyeOffset > 0 ==>> left eye
			mEyeOffset = mContentScaling*cStereoEyeDistance/2;

				// 0.5 would be directly in the plane; we place it slightly behind
				// that axis labels behind the display edges look naturally clipped.
			mScreenCenterOffset = -(int)(0.6*mContentScaling*cStereoEyeDistance);
			}

		if (clipRect != null) {
			size = new Dimension(mNonHighlightedImage.getWidth(null),
								 mNonHighlightedImage.getHeight(null));

			if (mChartType == cChartTypeBars
			 || (mChartType == cChartTypeBoxPlot && mHighlightedPoint.chartGroupIndex != -1)) {
				BarFraction fraction = getBarFraction((VisualizationPoint3D)mHighlightedPoint);
				if (fraction != null) { // should not happen
					fraction.calculateBounds((VisualizationPoint3D)mHighlightedPoint, false);
					clipRect.setBounds(fraction.bounds);
					}
				}
			else {
				mComposedMarker[mHighlightedPoint.shape].calculate((VisualizationPoint3D)mHighlightedPoint);
				mComposedMarker[mHighlightedPoint.shape].calculateBounds((VisualizationPoint3D)mHighlightedPoint, isTreeViewGraph());
				clipRect.setBounds(mComposedMarker[mHighlightedPoint.shape].bounds);
				}
			if (mEyeOffset != 0.0)
				clipRect.height += STEREO_CURSOR_LENGTH;
			}

		Rectangle graphBounds = new Rectangle(0, 0, size.width, size.height);
		paintInit(size, antialiasing, clipRect);
		setFontHeight(mFontHeight);
		paintContent(g, graphBounds, clipRect);
		if (clipRect == null)
			paintLegend(graphBounds, false);
		BufferedImage image1 = paintEnd();

		if (!mIsStereo) {
			if (m2ndG3D != null) {
				m2ndG3D.destroy();
				m2ndG3D = null;
				}
			if (clipRect != null) {
				clipRect.x /= mAAFactor;
				clipRect.y /= mAAFactor;
				clipRect.width /= mAAFactor;
				clipRect.height /= mAAFactor;
				}
			return image1;
			}

		Graphics3D leftG3D = mG3D;
		mG3D = m2ndG3D;

		mCoordinatesValid = false;
		mEyeOffset *= -1;
		mScreenCenterOffset *= -1;

		paintInit(size, antialiasing, clipRect);
		setFontHeight(mFontHeight);
		paintContent(g, graphBounds, clipRect);
		if (clipRect == null)
			paintLegend(graphBounds, false);
		BufferedImage image2 = paintEnd();

		m2ndG3D = mG3D;
		mG3D = leftG3D;

		Rectangle area = null;
		if (clipRect != null) {
			clipRect.x /= mAAFactor;
			clipRect.y /= mAAFactor;
			clipRect.width /= mAAFactor;
			clipRect.height /= mAAFactor;
			area = clipRect;
			}
		else {
			area = new Rectangle(0, 0, size.width, size.height);
			}
		mergeStereoImages(image1, image2, area);
		
		return mStereoImage;
		}

/*	private void paintContentSMP(Graphics g, Rectangle graphBounds, Rectangle clipRect) {
		if (clipRect == null)
			clipRect = new Rectangle(0, 0, graphBounds.width, graphBounds.height);
		CountDownLatch doneSignal = new CountDownLatch(mThreadCount);
		for (V3DWorker worker:mV3DWorker) {
			worker.initPaintContent(g, graphBounds, clipRect, doneSignal);
			mExecutor.execute(worker);
			}
		try {
			doneSignal.await();
			}
		catch (InterruptedException e) {}
		}*/

	private void mergeStereoImages(BufferedImage image1, BufferedImage image2, Rectangle area) {
		int width = image1.getWidth();
		int height = image1.getHeight();
		if (mStereoImage == null
		 || mStereoImage.getWidth() != width
		 || mStereoImage.getHeight() != height)
			mStereoImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		int[] rgb = ((DataBufferInt)mStereoImage.getRaster().getDataBuffer()).getData();
		int[] rgb1 = ((DataBufferInt)image1.getRaster().getDataBuffer()).getData();
		int[] rgb2 = ((DataBufferInt)image2.getRaster().getDataBuffer()).getData();

		Point s = new Point(area.x, area.y);
		SwingUtilities.convertPointToScreen(s, this);
		boolean isOdd = (mStereoMode == STEREO_MODE_H_INTERLACE_LEFT_EYE_FIRST) ? ((s.y & 1) != 0)
					  : (mStereoMode == STEREO_MODE_H_INTERLACE_RIGHT_EYE_FIRST) ? ((s.y & 1) == 0)
					  : ((s.x & 1) != 0);

		if (mThreadCount == 1)
			mergeStereoImageArea(rgb, rgb1, rgb2, isOdd, area);
		else
			mergeStereoImageAreaSMP(rgb, rgb1, rgb2, isOdd, area);
		}

	private void mergeStereoImageAreaSMP(int[] rgb, int[] rgb1, int[] rgb2, boolean isOdd, Rectangle area) {
		CountDownLatch doneSignal = new CountDownLatch(mThreadCount);
		for (V3DWorker worker:mV3DWorker) {
			worker.initMergeImages(rgb, rgb1, rgb2, isOdd, area, doneSignal);
			mExecutor.execute(worker);
			}
		try {
			doneSignal.await();
			}
		catch (InterruptedException e) {}
		}

	private void mergeStereoImageArea(int[] rgb, int[] rgb1, int[] rgb2, boolean isOdd, Rectangle area) {
		int width = mStereoImage.getWidth();

		switch (mStereoMode) {
		case STEREO_MODE_V_INTERLACE:
			for (int x=0; x<area.width; x++) {
				int i=area.x+x+area.y*width;
				if (isOdd) {
					if (x == 0) {
						for (int y=0; y<area.height; y++) {
							rgb[i] = rgb2[i];
							i+=width;
							}
						}
					else {
						for (int y=0; y<area.height; y++) {
							rgb[i]   = ((rgb2[i] & 0xFEFEFEFE) >> 1) + ((rgb2[i-1] & 0xFEFEFEFE) >> 1);
							i+=width;
							}
						}
					}
				else {
					if (x == area.width-1) {
						for (int y=0; y<area.height; y++) {
							rgb[i] = rgb1[i];
							i+=width;
							}
						}
					else {
						for (int y=0; y<area.height; y++) {
							rgb[i]   = ((rgb1[i] & 0xFEFEFEFE) >> 1) + ((rgb1[i+1] & 0xFEFEFEFE) >> 1);
							i+=width;
							}
						}
					}
				isOdd = !isOdd;
				}
			break;
		case STEREO_MODE_H_INTERLACE_RIGHT_EYE_FIRST:
			isOdd = !isOdd;
		case STEREO_MODE_H_INTERLACE_LEFT_EYE_FIRST:
			for (int y=0; y<area.height; y++) {
				int i=area.x+(area.y+y)*width;
				if (isOdd) {
					if (y == 0) {
						for (int x=0; x<area.width; x++) {
							rgb[i] = rgb1[i];
							i++;
							}
						}
					else {
						for (int x=0; x<area.width; x++) {
							rgb[i] = ((rgb1[i] & 0xFEFEFEFE) >> 1) + ((rgb1[i-width] & 0xFEFEFEFE) >> 1);
							i++;
							}
						}
					}
				else {
					if (y == area.height-1) {
						for (int x=0; x<area.width; x++) {
							rgb[i] = rgb2[i];
							i++;
							}
						}
					else {
						for (int x=0; x<area.width; x++) {
							rgb[i] = ((rgb2[i] & 0xFEFEFEFE) >> 1) + ((rgb2[i+width] & 0xFEFEFEFE) >> 1);
							i++;
							}
						}
					}
				isOdd = !isOdd;
				}
			break;
		case STEREO_MODE_3DTV_SIDE_BY_SIDE:
			if (area.x % 2 != 0) {
				area.x--;
				area.width++;
				}
			if (area.width % 2 != 0) {
				if (area.x + area.width < width)
					area.width++;
				else
					area.width--;
				}

			for (int y=0; y<area.height; y++) {
				int i0 = (area.y+y)*width;
				int i1 = i0+area.x/2;
				int i2 = i1 + width/2;
				for (int i=i0+area.x; i<i0+area.x+area.width; i+=2) {
					rgb[i1] = ((rgb1[i] & 0xFEFEFEFE) >> 1) + ((rgb1[i+1] & 0xFEFEFEFE) >> 1);
					rgb[i2] = ((rgb2[i] & 0xFEFEFEFE) >> 1) + ((rgb2[i+1] & 0xFEFEFEFE) >> 1);
					i1++;
					i2++;
					}
				}
			break;
			}
		}

	private void paintInit(Dimension size, boolean antialiasing, Rectangle clipRect) {
		if (mG3D == null) {
			mG3D = new Graphics3D(this);
			mG3D.setBackgroundTransparent(false);
			mG3D.setBackgroundArgb(0xFF000000 | getViewBackground().getRGB());
			mG3D.setSlabAndDepthValues(Integer.MIN_VALUE, Integer.MAX_VALUE, true);
			Graphics3D.setSpecularPower(70);
			}

		mG3D.setWindowParameters(size.width, size.height, antialiasing);
		mG3D.beginRendering(mNullRotationMatrix);

		mCurrentFontSize3D = -1;	// to force an initial g3D.setFont()
		}

	private void paintContent(Graphics g, Rectangle bounds, Rectangle clipRect) {
		mHVCount = 1;	// we don't use view splitting in 3D-views

		if (mChartInfo == null) {
			if (mChartType == cChartTypeBoxPlot
			 || mChartType == cChartTypeWhiskerPlot)
				calculateBoxPlot();
			else if (mChartType != cChartTypeScatterPlot)
				calculateBarsOrPies();

			for (int i=0; i<3; i++)
				mMetaCoordsValid[i] = false;
			}

		for (int i=0; i<3; i++) {
			if (!mMetaCoordsValid[i]) {
				calculateMetaCoordinates(i);
				mCoordinatesValid = false;
				}
			}

		if (!mCoordinatesValid)
			calculateCoordinates(g, bounds);

		if (!mIsAdjusting && !mDepthOrderValid) {
			// this is needed for highlighting the frontmost marker by mouse movement
			   Arrays.sort(mPoint, new VisualizationPoint3DComparator());
			   mDepthOrderValid = true;
			   }

		if (!(mGridMode == cGridModeHidden && mScaleMode == cScaleModeHidden)) {
			calculateAxes();
			drawFaces();
			drawAxes();
			}

		if (mChartType == cChartTypeBars) {
			drawBars(clipRect);
			}
		else {
			if (mChartType == cChartTypeBoxPlot
			 || mChartType == cChartTypeWhiskerPlot)
				drawBoxesOrWhiskers(clipRect);

			drawConnectionLines(clipRect);
			drawMarkers(clipRect);
			}

		if (clipRect != null && mIsStereo)
			drawStereoCursor();
		}

	private BufferedImage paintEnd() {
		mG3D.endRendering();
		return (BufferedImage)mG3D.getScreenImage();
		}

	protected int getStringWidth(String s) {
		return mG3D.getFont3DCurrent().fontMetrics.stringWidth(s)/mAAFactor;
		}

	/**
	 * Sets the font height of the Graphics3D limited to MAX_FONT_HEIGHT.
	 * The parameter h must not include mAAFactor.
	 * @param h
	 */
	protected void setFontHeight(int h) {
		// fontsize in Graphics3D is limited to 63. We need to limit to 50% of that
		// in order to assure that the mAAFactor always has enough room...
		if (h > MAX_FONT_HEIGHT)
			h = MAX_FONT_HEIGHT;
		if (mCurrentFontSize3D != h) {
			mCurrentFontSize3D = h;

			mG3D.setFont(mG3D.getFont3D(mAAFactor*h));
			}
		}

	protected void setColor(Color c) {
		mG3D.setColix(Graphics3D.getColix(c.getRGB()));
		}

	/**
	 * Coordinates include retina-scaling, but not AA-factor
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 */
	protected void drawLine(int x1, int y1, int x2, int y2) {
		mG3D.drawLine(mAAFactor*x1, mAAFactor*y1, 0, mAAFactor*x2, mAAFactor*y2, 0);
		}

	protected void drawRect(int x, int y, int w, int h) {
		mG3D.drawRect(mAAFactor*x, mAAFactor*y, 0, 0, mAAFactor*w, mAAFactor*h);
		}

	protected void fillRect(int x, int y, int w, int h) {
		mG3D.fillRect(mAAFactor*x, mAAFactor*y, 1, 0, mAAFactor*w, mAAFactor*h);
		}

	protected void drawString(String s, int x, int y) {
		if (s != null && s.length() != 0)
			mG3D.drawStringNoSlab(s, null, mAAFactor*x, mAAFactor*y, 0);
		}

	protected void drawMolecule(StereoMolecule mol, Color color, Rectangle2D.Double rect, int mode, int maxAVBL) {
		Depictor3D d = new Depictor3D(mol, Depictor3D.cDModeSuppressChiralText);
		rect.x *= mAAFactor;
		rect.y *= mAAFactor;
		rect.width *= mAAFactor;
		rect.height *= mAAFactor;
		d.validateView(mG3D, rect, mode+mAAFactor*maxAVBL);
		d.setOverruleColor(color, null);
		d.paint(mG3D);
		}

	private void drawHighlightedMarker(Graphics g) {
		Rectangle clipRect = new Rectangle();
		Image image = paintAllOnImage(g, null, true, clipRect);

		int x1 = clipRect.x;
		int y1 = clipRect.y;
		int x2 = x1 + clipRect.width - 1;
		int y2 = y1 + clipRect.height - 1;
		float f = HiDPIHelper.getRetinaScaleFactor();
		g.drawImage(image, (int)(x1/f), (int)(y1/f), (int)(x2/f), (int)(y2/f), x1, y1, x2, y2, null);
		}

	private void drawHighlightedLabelOutline(Graphics g)  {
		if (mHighlightedLabelPosition != null) {
			g.setColor(ColorHelper.perceivedBrightness(getViewBackground()) > 0.5f ? Color.BLACK : Color.WHITE);
			g.drawRect(mHighlightedLabelPosition.getScreenX1(),
					   mHighlightedLabelPosition.getScreenY1(),
					   mHighlightedLabelPosition.getScreenWidth(),
					   mHighlightedLabelPosition.getScreenHeight());
			}
		}

	public VisualizationPoint findMarker(int x, int y) {
		// TODO handle marker finding for bars & boxes

		return super.findMarker(x, y);
		}

	@Override
	protected void updateHighlightedLabelPosition() {
		int newX = mHighlightedLabelPosition.getLabelCenterOnScreenX();
		int newY = mHighlightedLabelPosition.getLabelCenterOnScreenY();
		float[] c = getMetaFromScreenCoordinates(newX, newY, ((LabelPosition3D)mHighlightedLabelPosition).getScreenZ());
		float x = mAxisVisMin[0] + (c[0] + 1f) * (mAxisVisMax[0] - mAxisVisMin[0]) / 2f;
		float y = mAxisVisMin[1] + (c[1] + 1f) * (mAxisVisMax[1] - mAxisVisMin[1]) / 2f;
		float z = mAxisVisMin[2] + (c[2] + 1f) * (mAxisVisMax[2] - mAxisVisMin[2]) / 2f;
		mHighlightedLabelPosition.setCustom(true);
		mHighlightedLabelPosition.setXY(x, y);
		((LabelPosition3D)mHighlightedLabelPosition).setZ(z);
		}

	private DoubleDimension getLabelDimension(VisualizationPoint vp, boolean isMolecule, int position, boolean isTreeView, DoubleDimension size) {
		if (isMolecule) {
			if (mLabelMolecule == null)
				mLabelMolecule = new StereoMolecule();
			StereoMolecule mol = mTableModel.getChemicalStructure(vp.record, mLabelColumn[position], CompoundTableModel.ATOM_COLOR_MODE_NONE, mLabelMolecule);
			if (mol == null)
				return null;

			Depictor3D depictor = new Depictor3D(mol, Depictor3D.cDModeSuppressChiralText);
			depictor.validateView(mG3D, DEPICTOR_RECT, AbstractDepictor.cModeInflateToHighResAVBL+mContentScaling*Math.max(1, (int)(256*getLabelAVBL(vp, position, isTreeView))));
			Rectangle2D.Double bounds = depictor.getBoundingRect();
			size.width = bounds.width;
			size.height = bounds.height;
			return size;
			}

		String label = mTableModel.getValue(vp.record, mLabelColumn[position]);
		if (label.length() == 0)
			return null;

		setFontHeight((int)getLabelFontSize(vp, position, isTreeView));
		size.width = mG3D.getFont3DCurrent().fontMetrics.stringWidth(label);
		size.height = mG3D.getFont3DCurrent().fontMetrics.getHeight();
		return size;
		}

	@Override
	protected float getLabelFontSize(VisualizationPoint vp, int position, boolean isTreeView) {
		return Math.min(MAX_FONT_HEIGHT, super.getLabelFontSize(vp, position, isTreeView));
		}

	@Override
	protected float getMarkerWidth(VisualizationPoint p) {
		return getMarkerSize((VisualizationPoint3D)p);
		}

	@Override
	protected float getMarkerHeight(VisualizationPoint p) {
		return getMarkerSize((VisualizationPoint3D)p);
		}

	@Override
	protected float getMarkerSize(VisualizationPoint vp) {
		return super.getMarkerSize(vp) * ((VisualizationPoint3D)vp).zoom / HiDPIHelper.getRetinaScaleFactor();
		}

	@Override
	public void setJittering(float jittering, int axes, boolean isAdjusting) {
		if (mMarkerJittering != jittering || mMarkerJitteringAxes != axes || mIsAdjusting != isAdjusting) {
			mMarkerJittering = jittering;
			mMarkerJitteringAxes = axes;
			invalidateMetaCoordinates(-1);
			mIsAdjusting = isAdjusting;
			mOffImageValid = false;
			repaint();
			}
		}

	public void setMarkerSize(float size, boolean isAdjusting) {
		if (mRelativeMarkerSize != size)
			mRelativeMarkerSize = size;

		if (mIsAdjusting != isAdjusting)
			mCoordinatesValid = false;

		mIsAdjusting = isAdjusting;
		mOffImageValid = false;
		repaint();
		}

	@Override
	public void setShowNaNValues(boolean b) {
		invalidateMetaCoordinates(-1);
		super.setShowNaNValues(b);
		}

	@Override
	public boolean setViewBackground(Color c) {
		if (mG3D != null)
			mG3D.setBackgroundArgb(0xFF000000 | c.getRGB());
		if (m2ndG3D != null)
			m2ndG3D.setBackgroundArgb(0xFF000000 | c.getRGB());

		return super.setViewBackground(c);
		}

	public Color getGraphFaceColor() {
		return new Color(0x00FFFFFF & mGraphFaceColor);
		}

	public void setGraphFaceColor(Color c) {
		int color = 0xFF000000 | c.getRGB();
		if (mGraphFaceColor != color) {
			mGraphFaceColor = color;
			mOffImageValid = false;
			repaint();
			}
		}

	public boolean isStereo() {
		return mIsStereo;
		}

	public void setStereo(boolean isStereo) {
		if (mIsStereo != isStereo) {
			mIsStereo = isStereo;
			if (!isStereo)
				mParentFrame.removeComponentListener(this);
			else
				mParentFrame.addComponentListener(this);
			invalidateOffImage(false);
			}
		}

	public int getStereoMode() {
		return mStereoMode;
		}

	public void setStereoMode(int mode) {
		if (mStereoMode != mode) {
			switch (mode) {
			case STEREO_MODE_H_INTERLACE_LEFT_EYE_FIRST:
				DataWarrior.getPreferences().put(STEREO_MODE_PROPERTY, STEREO_MODE_STRING_H_INTERLACE_LEFT_EYE_FIRST);
				break;
			case STEREO_MODE_H_INTERLACE_RIGHT_EYE_FIRST:
				DataWarrior.getPreferences().put(STEREO_MODE_PROPERTY, STEREO_MODE_STRING_H_INTERLACE_RIGHT_EYE_FIRST);
				break;
			case STEREO_MODE_V_INTERLACE:
				DataWarrior.getPreferences().put(STEREO_MODE_PROPERTY, STEREO_MODE_STRING_V_INTERLACE);
				break;
			case STEREO_MODE_3DTV_SIDE_BY_SIDE:
				DataWarrior.getPreferences().put(STEREO_MODE_PROPERTY, STEREO_MODE_STRING_3DTV_SIDE_BY_SIDE);
				break;
			default:
				return;
				}

			mStereoMode = mode;
			invalidateOffImage(false);
			}
		}

	public void mouseMoved(MouseEvent e) {
		super.mouseMoved(e);

		if (mHighlightedPoint != null && mIsStereo)
			setCursor(CursorHelper.getCursor(CursorHelper.cInvisibleCursor));
		else
			setCursor(CursorHelper.getCursor(CursorHelper.cPointerCursor));
		}

	public void mouseClicked(MouseEvent e) {
		VisualizationPoint oldPoint = mActivePoint;
		super.mouseClicked(e);
		if (oldPoint != mActivePoint) {
			invalidateOffImage(false);
			}
		}

	public void mousePressed(MouseEvent e) {
		mDragMode = DRAG_MODE_NONE;
		mPopupVisible = false;
		super.mousePressed(e);
		}

	public void mouseReleased(MouseEvent e) {
		super.mouseReleased(e);
		if (mIsAdjusting) {
			mCoordinatesValid = false;
			mIsAdjusting = false;
			repaint();
			}
		}

	@Override
	protected void setActivePoint(VisualizationPoint newReference) {
		super.setActivePoint(newReference);

		for (int axis=0; axis<mDimensions; axis++)
			if (mAxisIndex[axis] != cColumnUnassigned && mTableModel.isDescriptorColumn(mAxisIndex[axis]))
				mMetaCoordsValid[axis] = false;

		invalidateOffImage(false);
		}

	public boolean delayPopupMenu() {
		return true;
		}

	public boolean showDelayedPopupMenu() {
		return mDragMode == DRAG_MODE_NONE;
		}

	protected boolean showPopupMenu() {
		mPopupVisible = super.showPopupMenu();
		return mPopupVisible;
		}

	public float[][] getRotationMatrix() {
		return mRotation;
		}

	public void setRotationMatrix(final float[][] rotation) {
		if (javax.swing.SwingUtilities.isEventDispatchThread()) {
			for (int i=0; i<3; i++)
				for (int j=0; j<3; j++)
					mRotation[i][j] = rotation[i][j];
			mDepthOrderValid = false;
			mCoordinatesValid = false;
			repaint();
			}
		else {
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					for (int i=0; i<3; i++)
						for (int j=0; j<3; j++)
							mRotation[i][j] = rotation[i][j];
					mDepthOrderValid = false;
					mCoordinatesValid = false;
					repaint();
					}
				});
			}
		}

	public void mouseDragged(MouseEvent e) {
		// after a small delay the popup is shown. In that case dragging does not take place
		if (mPopupVisible)
			return;

		super.mouseDragged(e);

		if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0 || isTouchFunctionActive()) {
			int dx = mMouseY1 - mMouseY2;
			int dy = mMouseX2 - mMouseX1;

			// determine drag mode with first drag
			if (mDragMode == DRAG_MODE_NONE) {
				mIsAdjusting = true;

				if (!e.isShiftDown())
					mDragMode = DRAG_MODE_FLEXIBLE;
				else if (Math.abs(dx) > Math.abs(dy))
					mDragMode = DRAG_MODE_HORIZONTAL;
				else
					mDragMode = DRAG_MODE_VERTICAL;
				}

			if (mDragMode == DRAG_MODE_VERTICAL)
				dx = 0;
			else if (mDragMode == DRAG_MODE_HORIZONTAL)
				dy = 0;

			if (dx != 0) {
				float angle = (float)dx * mAAFactor / mScreenZoom;
				float cos = (float)Math.cos(angle);
				float sin = (float)Math.sin(angle);

				float y = mRotation[0][1];//L2;
				float z = mRotation[0][2];//L3;
				mRotation[0][1] = cos * y + sin * z;
				mRotation[0][2] = cos * z - sin * y;

				y = mRotation[1][1];//M2;
				z = mRotation[1][2];//M3;
				mRotation[1][1] = cos * y + sin * z;
				mRotation[1][2] = cos * z - sin * y;

				y = mRotation[2][1];//N2;
				z = mRotation[2][2];//N3;
				mRotation[2][1] = cos * y + sin * z;
				mRotation[2][2] = cos * z - sin * y;

				mCoordinatesValid = false;
				}

			if (dy != 0) {
				float angle = (float)dy / mScreenZoom;
				float cos = (float)Math.cos(angle);
				float sin = (float)Math.sin(angle);

				float x = mRotation[0][0];//L1;
				float z = mRotation[0][2];//L3;
				mRotation[0][0] = cos * x + sin * z;
				mRotation[0][2] = cos * z - sin * x;

				x = mRotation[1][0];//M1;
				z = mRotation[1][2];//M3;
				mRotation[1][0] = cos * x + sin * z;
				mRotation[1][2] = cos * z - sin * x;

				x = mRotation[2][0];//N1;
				z = mRotation[2][2];//N3;
				mRotation[2][0] = cos * x + sin * z;
				mRotation[2][2] = cos * z - sin * x;

				mCoordinatesValid = false;
				}

			mMouseX1 = mMouseX2;
			mMouseY1 = mMouseY2;
			mDepthOrderValid = false;

			for (RotationListener rl:mRotationListenerList)
				rl.rotationChanged(this, mRotation);
			}

		repaint();
		}

	public void addRotationListener(RotationListener rl) {
		mRotationListenerList.add(rl);
		}

	public void removeRotationListener(RotationListener rl) {
		mRotationListenerList.remove(rl);
		}

	public void rotationChanged(JVisualization3D source, float[][] rotation) {
		for (int i=0; i<3; i++)
			for (int j=0; j<3; j++)
				mRotation[i][j] = rotation[i][j];

		mCoordinatesValid = false;
		mDepthOrderValid = false;
		repaint();
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cChangeExcluded) {
			mIsAdjusting = e.isAdjusting();
			if (!mIsAdjusting)
				mDepthOrderValid = false;
			invalidateOffImage(true);
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnData) {
			for (int axis=0; axis<mDimensions; axis++)
				if (mAxisIndex[axis] == e.getColumn())
					invalidateMetaCoordinates(axis);
			invalidateOffImage(false);
			}
		else if (e.getType() == CompoundTableEvent.cAddRows
			  || e.getType() == CompoundTableEvent.cDeleteRows) {
			for (int axis=0; axis<mDimensions; axis++) {
				invalidateMetaCoordinates(axis);
				if (mMoleculeAxisIndex[axis] != cColumnUnassigned)
					mScaleMolecule[axis] = null;
				}
			invalidateOffImage(true);
			}

		super.compoundTableChanged(e);
		}

	@Override
	public void updateVisibleRange(int axis, float low, float high, boolean isAdjusting) {
		if (mAxisIndex[axis] != -1)
			invalidateMetaCoordinates(axis);

		if (!isAdjusting && mIsAdjusting)
			invalidateOffImage(false);
		mIsAdjusting = isAdjusting;

		super.updateVisibleRange(axis, low, high, isAdjusting);
		}

	public void initializeAxis(int axis) {
		super.initializeAxis(axis);

		invalidateMetaCoordinates(axis);
		mMoleculeAxisIndex[axis] = cColumnUnassigned;
		mScaleMolecule[axis] = null;
		}

	public void colorChanged(VisualizationColor source) {
		super.colorChanged(source);
		mChartInfo = null;
		}

	public void valueChanged(ListSelectionEvent e) {
		super.valueChanged(e);
		invalidateMetaCoordinates(-1);
		}

	protected void invalidateOffImage(boolean invalidateCoordinates) {
		super.invalidateOffImage(invalidateCoordinates);
		mChartInfo = null;
		}

	protected void calculateBarsOrPies() {
		super.calculateBarsOrPies(1.0);

		for (int axis=0; axis<3; axis++) {
			if (mChartInfo.barAxis != axis) {
				float cellSize = 2.0f / (float)getCategoryVisCount(axis);
				if (mChartInfo.barWidth == 0 || mChartInfo.barWidth > 0.5 * cellSize)
					mChartInfo.barWidth = 0.5f * cellSize;
				}
			}
		}

	/**
	 * Depending on mChartType invalidates given axis or all axis.
	 * @param axis use -1 for all axis
	 */
	private void invalidateMetaCoordinates(int axis) {
		if (axis == -1 || mChartType == cChartTypeBars) {
			mChartInfo = null;
			for (int i=0; i<3; i++)
				mMetaCoordsValid[i] = false;
			}
		else {
			mMetaCoordsValid[axis] = false;
			}
		}

	private void calculateMetaCoordinates(int axis) {
		if (mChartType == cChartTypeBars) {
			calculateMetaBarCoordinates(axis);
			}
		else {
			mMetaBarPosition[axis] = null;
			if (mAxisIndex[axis] == -1) {
				for (int i=0; i<mDataPoints; i++)
					((VisualizationPoint3D)mPoint[i]).coord[axis] = 0.0f;
				}
			else {
				float mid = (mAxisVisMin[axis] + mAxisVisMax[axis]) / 2;
				float halfLen = (mAxisVisMax[axis] - mAxisVisMin[axis]) / 2;
				for (int i=0; i<mDataPoints; i++) {
					VisualizationPoint3D vp = (VisualizationPoint3D)mPoint[i];
					float value = getAxisValue(vp.record, axis);
					if (Float.isNaN(value))
						((VisualizationPoint3D)mPoint[i]).coord[axis] = -1.15f;
					else
						((VisualizationPoint3D)mPoint[i]).coord[axis] = (value - mid) / halfLen;
					}
				}

/*
			// In box- and whisker-plots the category axis may be based on doubleCategory types.
			// In this case we need to consider category numbers rather than the doubleValue for positioning.

	  		boolean isDoubleCategory = (mChartType == cChartTypeBoxPlot || mChartType == cChartTypeWhiskerPlot)
									&& axis != mChartInfo.barAxis
									&& mAxisIndex[axis] != cColumnUnassigned
									&& mTableModel.isColumnTypeDouble(mAxisIndex[axis]);

			float mid = mIsCategoryAxis[axis] ? mCategoryVisibleCount[axis] / 2 : (mAxisVisMin[axis] + mAxisVisMax[axis]) / 2;
			float halfLen = mIsCategoryAxis[axis] ? mCategoryVisibleCount[axis] / 2 : (mAxisVisMax[axis] - mAxisVisMin[axis]) / 2;

			if (mChartType == cChartTypeScatterPlot
			 || mChartType == cChartTypeWhiskerPlot) {
				mMetaBarPosition[axis] = null;
				if (mAxisIndex[axis] == -1) {
					for (int i=0; i<mDataPoints; i++)
						((VisualizationPoint3D)mPoint[i]).coord[axis] = 0.0f;
					}
				else if (mTableModel.isDescriptorColumn(mAxisIndex[axis])) {
					if (mAxisSimilarity[axis] == null)
						for (int i=0; i<mDataPoints; i++)
							((VisualizationPoint3D)mPoint[i]).coord[axis] = 0.0f;
					else
						for (int i=0; i<mDataPoints; i++)
							((VisualizationPoint3D)mPoint[i]).coord[axis] = (mAxisSimilarity[axis][mPoint[i].record.getID()] - mid) / halfLen;
					}
				else if (mIsCategoryAxis[axis]) {
					for (int i=0; i<mDataPoints; i++)
						((VisualizationPoint3D)mPoint[i]).coord[axis]
							= (0.5f + getCategoryIndex(axis, mPoint[i]) - mid) / halfLen;
					}
				else {
					for (int i=0; i<mDataPoints; i++) {
						float value = mPoint[i].record.getDouble(mAxisIndex[axis]);
						if (Float.isNaN(value))
							((VisualizationPoint3D)mPoint[i]).coord[axis] = -1.15f;
						else
							((VisualizationPoint3D)mPoint[i]).coord[axis] = (value - mid) / halfLen;
						}
					}

				if (mMarkerJittering > 0.0 && (mMarkerJitteringAxes & (1 << axis)) != 0) {
					float jittering = 0.4f * mMarkerJittering;
					for (int i=0; i<mDataPoints; i++)
						((VisualizationPoint3D)mPoint[i]).coord[axis] += (mRandom.nextDouble() - 0.5) * jittering;
					}
				}
			else if (mChartType == cChartTypeBoxPlot) {	// handle box plots separately because of data point specific if condition
				// In box- and whisker-plots the category axis may be based on doubleCategory types.
				// In this case we need to consider category numbers rather than the doubleValue for positioning.
				if (mAxisIndex[axis] == -1) {
					for (int i=0; i<mDataPoints; i++)
						if (mPoint[i].chartGroupIndex == -1)
							((VisualizationPoint3D)mPoint[i]).coord[axis] = 0.0f;
					}
				else if (mTableModel.isDescriptorColumn(mAxisIndex[axis])) {
					if (mAxisSimilarity[axis] == null) {
						for (int i=0; i<mDataPoints; i++)
							if (mPoint[i].chartGroupIndex == -1)
								((VisualizationPoint3D)mPoint[i]).coord[axis] = 0.0f;
						}
					else {
						for (int i=0; i<mDataPoints; i++)
							if (mPoint[i].chartGroupIndex == -1)
								((VisualizationPoint3D)mPoint[i]).coord[axis] = (mAxisSimilarity[axis][mPoint[i].record.getID()] - mid) / halfLen;
						}
					}
				else if (isDoubleCategory) {
					for (int i=0; i<mDataPoints; i++)
						if (mPoint[i].chartGroupIndex == -1)
							((VisualizationPoint3D)mPoint[i]).coord[axis]
								= (0.5f + getCategoryIndex(axis, mPoint[i]) - mid) / halfLen;
					}
				else {
					for (int i=0; i<mDataPoints; i++)
						if (mPoint[i].chartGroupIndex == -1)
							((VisualizationPoint3D)mPoint[i]).coord[axis]
								= (mPoint[i].record.getDouble(mAxisIndex[axis]) - mid) / halfLen;
					}

				if (mMarkerJittering > 0.0) {
					float jittering = 0.4f * mMarkerJittering;
					for (int i=0; i<mDataPoints; i++)
						if (mPoint[i].chartGroupIndex == -1)
							((VisualizationPoint3D)mPoint[i]).coord[axis] += (mRandom.nextDouble() - 0.5) * jittering;
					}
				}*/
			}

		mMetaCoordsValid[axis] = true;
		mDepthOrderValid = false;
		}

	private void calculateMetaBarCoordinates(int axis) {
		if (!mChartInfo.barOrPieDataAvailable)
			return;

		float axisSize = mChartInfo.axisMax - mChartInfo.axisMin;
		float cellSize = 2.0f / (float)getCategoryVisCount(axis);
		int catCount = getCategoryVisCount(0)* getCategoryVisCount(1)* getCategoryVisCount(2);

		if (mChartInfo.barAxis == axis) {
			int focusFlagNo = getFocusFlag();
			int basicColorCount = mMarkerColor.getColorList().length + 1;
			int colorCount = basicColorCount * ((focusFlagNo == -1) ? 1 : 2);
	
			float barBaseOffset = (mChartInfo.barBase - mChartInfo.axisMin) * cellSize / axisSize;
	
			if (mChartInfo.useProportionalFractions())
				mChartInfo.absValueFactor = new float[1][catCount];
			else
				mChartInfo.innerDistance = new float[1][catCount];

			mMetaBarColorEdge = new float[catCount][colorCount+1];
			mMetaBarPosition[axis] = null;

			for (int i = 0; i<getCategoryVisCount(0); i++) {
				for (int j = 0; j<getCategoryVisCount(1); j++) {
					for (int k = 0; k<getCategoryVisCount(2); k++) {
						int cat = i+j* getCategoryVisCount(0)+k* getCategoryVisCount(0)* getCategoryVisCount(1);
						float barHeight = cellSize * Math.abs(mChartInfo.barValue[0][cat] - mChartInfo.barBase) / axisSize;
						if (mChartInfo.useProportionalFractions())
							mChartInfo.absValueFactor[0][cat] = barHeight / mChartInfo.absValueSum[0][cat];
						else
							mChartInfo.innerDistance[0][cat] = barHeight / (float)mChartInfo.pointsInCategory[0][cat];

						float barOffset = (mChartInfo.barValue[0][cat] >= 0.0f) ? 0.0f
								: cellSize * (mChartInfo.barValue[0][cat] - mChartInfo.barBase) / axisSize;
						int ii = (axis==0) ? i : (axis==1) ? j : k;
						mMetaBarColorEdge[cat][0] = -1.0f + barBaseOffset + barOffset + ii * cellSize;
						if (mChartInfo.useProportionalFractions()) {
							for (int l=0; l<colorCount; l++)
								mMetaBarColorEdge[cat][l+1] = mMetaBarColorEdge[cat][l]
										+ mChartInfo.absColorValueSum[0][cat][l] * mChartInfo.absValueFactor[0][cat];
							}
						else {
							for (int l=0; l<colorCount; l++)
								mMetaBarColorEdge[cat][l+1] = mMetaBarColorEdge[cat][l]
										+ mChartInfo.innerDistance[0][cat] * (float)mChartInfo.pointsInColorCategory[0][cat][l];
							}
						}
					}
				}

			// calculate meta coords of individual records as basis for screen position
			float[][] barColorEdge = null;
			if (mChartInfo.absColorValueSum != null) {
				barColorEdge = new float[catCount][colorCount];
				for (int i=0; i<catCount; i++)
					for (int j=0; j<colorCount; j++)
						barColorEdge[i][j] = mMetaBarColorEdge[i][j];
				}

			int colorListLength = mMarkerColor.getColorList().length;

			for (int i=0; i<mDataPoints; i++) {
				VisualizationPoint vp = mPoint[i];
				if (isVisibleExcludeNaN(vp)) {
					int cat = getChartCategoryIndex(mPoint[i]);
					if (mChartInfo.useProportionalFractions()) {
						int colorIndex = getColorIndex(vp, colorListLength, focusFlagNo);
						float fractionHeight = Math.abs(vp.record.getDouble(mChartColumn)) * mChartInfo.absValueFactor[0][cat];
						((VisualizationPoint3D)vp).coord[axis] = barColorEdge[cat][colorIndex] + 0.5f * fractionHeight;
						barColorEdge[cat][colorIndex] += fractionHeight;
						}
					else {
						((VisualizationPoint3D)vp).coord[axis] = mMetaBarColorEdge[cat][0]
											 + mChartInfo.innerDistance[0][cat]*(0.5f+vp.chartGroupIndex);
						}
					}
				}
			}
		else {
			mMetaBarPosition[axis] = new float[catCount];
			for (int i = 0; i<getCategoryVisCount(0); i++) {
				for (int j = 0; j<getCategoryVisCount(1); j++) {
					for (int k = 0; k<getCategoryVisCount(2); k++) {
						int cat = i+j* getCategoryVisCount(0)+k* getCategoryVisCount(0)* getCategoryVisCount(1);
						int ii = (axis==0) ? i : (axis==1) ? j : k;
						mMetaBarPosition[axis][cat] = -1.0f + ii * cellSize + cellSize/2;
						}
					}
				}

			// calculate meta coords of individual records as basis for screen position
			for (int i=0; i<mDataPoints; i++) {
				if (isVisibleExcludeNaN(mPoint[i])) {
					int cat = getChartCategoryIndex(mPoint[i]);
					((VisualizationPoint3D)mPoint[i]).coord[axis] = mMetaBarPosition[axis][cat];
					}
				}
			}
		}

	/**
	 * Needs to be called before validateLegend(), because the marker size legend depends on it.
	 * bounds and thus mAbsoluteConnectionLineWidth include the retina factor, but not the anti-aliasing factor.
	 */
	private void calculateMarkerSize(Rectangle bounds) {
		mAbsoluteMarkerSize = mRelativeMarkerSize * cMarkerSize
				* (float)((bounds.width > bounds.height) ? bounds.height : bounds.width);
		mAbsoluteConnectionLineWidth = mRelativeConnectionLineWidth * cConnectionLineWidth
				* (float)((bounds.width > bounds.height) ? bounds.height : bounds.width);
		if (!Float.isNaN(mMarkerSizeZoomAdaption))
			mAbsoluteConnectionLineWidth *= mMarkerSizeZoomAdaption;
		}

	private void calculateCoordinates(Graphics g, Rectangle bounds) {
		if (mEyeOffset >= 0) {	// if we are not calculating for the second stereo image
			calculateMarkerSize(bounds);
			calculateLegend(bounds, mCurrentFontSize3D);

			mScreenZoom = mAAFactor * cScreenZoom * (float)Math.min(bounds.width, bounds.height);
			mScreenCenterX = mAAFactor * (bounds.x + bounds.width / 2);
			mScreenCenterY = mAAFactor * (bounds.y + bounds.height / 2);
	
			initializeMoleculeCoordinates(g, mAAFactor*bounds.width, mAAFactor*bounds.height);
			calculateMatrix();
			}

		for (int i=0; i<mDataPoints; i++) {
			VisualizationPoint3D vp = (VisualizationPoint3D)mPoint[i];
			float x = mMatrix[0][0]*vp.coord[0]+mMatrix[1][0]*vp.coord[1]+mMatrix[2][0]*vp.coord[2];
			float y = mMatrix[0][1]*vp.coord[0]+mMatrix[1][1]*vp.coord[1]+mMatrix[2][1]*vp.coord[2];
			float z = mMatrix[0][2]*vp.coord[0]+mMatrix[1][2]*vp.coord[1]+mMatrix[2][2]*vp.coord[2];
			vp.zoom = cLocation / (cLocation - z);
			vp.screenX = (mScreenCenterX+(int)(x*vp.zoom))/mContentScaling;
			vp.screenY = (mScreenCenterY-(int)(y*vp.zoom))/mContentScaling;
			vp.screenZ = (short)((cLocation-z)*mScreenZoom/mContentScaling);
			if (mEyeOffset > 0)	// left eye
				vp.stereoOffset = (short)Math.round(mScreenCenterOffset + mEyeOffset * vp.zoom);
			}

		mCoordinatesValid = true;
		}

	/**
	 * Assuming a neutral z position (0.0) this method translates screen
	 * coordinates into 3D meta coordinates within the cube in its current
	 * rotations state. Returned meta coordinates are limited to the valid range of -1.0 to 1.0.
	 * Screen coords don't include retina nor AA scaling.
	 * @param sx
	 * @param sy
	 * @return
	 */
	protected float[] getMetaFromScreenCoordinates(int sx, int sy, int sz) {
		float x = (mContentScaling*sx - mScreenCenterX) / mScreenZoom;
		float y = -(mContentScaling*sy - mScreenCenterY) / mScreenZoom;
		float z = 0f;   // TODO use z

		float[] c = new float[3];

		/* this is without VECMATH the hard way...
		c[0] = (float)Math.max(-1.0, Math.min(1.0,
				 (mRotation[1][0]*mRotation[1][1]*mRotation[2][1]*z
				- mRotation[1][1]*mRotation[1][1]*mRotation[2][0]*z
				  + mRotation[1][1]*mRotation[1][2]*mRotation[2][0]*y
				  - mRotation[1][0]*mRotation[1][1]*mRotation[2][2]*y
				  + mRotation[1][1]*mRotation[1][1]*mRotation[2][2]*x
				  - mRotation[1][1]*mRotation[1][2]*mRotation[2][1]*x)
				 / (mRotation[0][0]*mRotation[1][1]*mRotation[1][1]*mRotation[2][2]
				  - mRotation[0][0]*mRotation[1][1]*mRotation[1][2]*mRotation[2][1]
				  - mRotation[0][1]*mRotation[1][0]*mRotation[1][1]*mRotation[2][2]
				  + mRotation[0][1]*mRotation[1][1]*mRotation[1][2]*mRotation[2][0]
				  + mRotation[0][2]*mRotation[1][0]*mRotation[1][1]*mRotation[2][1]
				  - mRotation[0][2]*mRotation[1][1]*mRotation[1][1]*mRotation[2][0])));

		c[1] = (float)Math.max(-1.0, Math.min(1.0,
				 (mRotation[0][1]*mRotation[2][0]*mRotation[2][1]*z
				- mRotation[0][0]*mRotation[2][1]*mRotation[2][1]*z
				+ mRotation[0][0]*mRotation[2][1]*mRotation[2][2]*y
				- mRotation[0][2]*mRotation[2][0]*mRotation[2][1]*y
				+ mRotation[0][2]*mRotation[2][1]*mRotation[2][1]*x
				- mRotation[0][1]*mRotation[2][1]*mRotation[2][2]*x)
			   / (mRotation[0][0]*mRotation[1][1]*mRotation[2][1]*mRotation[2][2]
				- mRotation[0][0]*mRotation[1][2]*mRotation[2][1]*mRotation[2][1]
				- mRotation[0][1]*mRotation[1][0]*mRotation[2][1]*mRotation[2][2]
				+ mRotation[0][1]*mRotation[1][2]*mRotation[2][0]*mRotation[2][1]
				+ mRotation[0][2]*mRotation[1][0]*mRotation[2][1]*mRotation[2][1]
				- mRotation[0][2]*mRotation[1][1]*mRotation[2][0]*mRotation[2][1])));

		c[2] = (float)Math.max(-1.0, Math.min(1.0,
				 (mRotation[0][0]*mRotation[0][1]*mRotation[1][1]*z
				- mRotation[0][1]*mRotation[0][1]*mRotation[1][0]*z
				+ mRotation[0][1]*mRotation[0][2]*mRotation[1][0]*y
				- mRotation[0][0]*mRotation[0][1]*mRotation[1][2]*y
				+ mRotation[0][1]*mRotation[0][1]*mRotation[1][2]*x
				- mRotation[0][1]*mRotation[0][2]*mRotation[1][1]*x)
			   / (mRotation[0][1]*mRotation[0][1]*mRotation[1][2]*mRotation[2][0]
				- mRotation[0][1]*mRotation[0][2]*mRotation[1][1]*mRotation[2][0]
				- mRotation[0][0]*mRotation[0][1]*mRotation[1][2]*mRotation[2][1]
				+ mRotation[0][1]*mRotation[0][2]*mRotation[1][0]*mRotation[2][1]
				+ mRotation[0][0]*mRotation[0][1]*mRotation[1][1]*mRotation[2][2]
				- mRotation[0][1]*mRotation[0][1]*mRotation[1][0]*mRotation[2][2])));
				*/

// An alternative, especially when many calculations with the same inverted matrix need to be done, is using VECMATH
		Matrix3f m = new Matrix3f(mRotation[0][0], mRotation[0][1], mRotation[0][2],
								  mRotation[1][0], mRotation[1][1], mRotation[1][2],
								  mRotation[2][0], mRotation[2][1], mRotation[2][2]);

		m.invert();

		c[0] = (float)Math.max(-1.0, Math.min(1.0, m.m00*x+m.m10*y+m.m20*z));
		c[1] = (float)Math.max(-1.0, Math.min(1.0, m.m01*x+m.m11*y+m.m21*z));
		c[2] = (float)Math.max(-1.0, Math.min(1.0, m.m02*x+m.m12*y+m.m22*z));

		return c;
		}

	private void calculateMatrix() {
		for (int i=0; i<3; i++) {
			mMatrix[i][0] = mRotation[i][0] * mScreenZoom;
			mMatrix[i][1] = mRotation[i][1] * mScreenZoom;
			mMatrix[i][2] = mRotation[i][2];
			}
		}

	private void initializeMoleculeCoordinates(Graphics g, int width, int height) {
		int size = Math.min(width, height);

		for (int axis=0; axis<3;axis++) {
			if (mMoleculeAxisIndex[axis] != mAxisIndex[axis]
			 || mMoleculeAxisSize[axis] != size)
				mScaleMolecule[axis] = null;

			if (mAxisIndex[axis] != cColumnUnassigned) {
				String specialType = mTableModel.getColumnSpecialType(mAxisIndex[axis]);
				if (specialType != null && specialType.equals(CompoundTableModel.cColumnTypeIDCode)) {
					 if (mScaleMolecule[axis] == null) {
						int moleculeCount = mTableModel.getCategoryCount(mAxisIndex[axis]);
						mMoleculeOffsetX[axis] = new float[moleculeCount];
						mMoleculeOffsetY[axis] = new float[moleculeCount];
						mScaleMolecule[axis] = new StereoMolecule[moleculeCount];
						mMoleculeAxisIndex[axis] = mAxisIndex[axis];
						mMoleculeAxisSize[axis] = size;
						}
					}
				}

			if (mScaleMolecule[axis] != null
			 && mAxisVisMax[axis] - mAxisVisMin[axis] <= 32.0) {
				CategoryList<CategoryMolecule> list = (CategoryList<CategoryMolecule>)mTableModel.getNativeCategoryList(mAxisIndex[axis]);
				float molsize = (float)size * cMolsizeFactor;

				int min = Math.round(mAxisVisMin[axis] + 0.5f);
				int max = Math.round(mAxisVisMax[axis] - 0.5f);
				for (int i=min; i<=max; i++) {
					if (mScaleMolecule[axis][i] == null) {
						mScaleMolecule[axis][i] = list.get(i).getMolecule();
						if (mScaleMolecule[axis][i] != null) {
							new Depictor(mScaleMolecule[axis][i], Depictor.cDModeSuppressChiralText).updateCoords(g,
												new Rectangle2D.Double(-molsize/2, -molsize/2, molsize, molsize),
												AbstractDepictor.cModeInflateToMaxAVBL
												+ (int)(molsize/3));	// maximum average bond length
							mScaleMolecule[axis][i].zoomAndRotateInit(0, 0);
							}
						}
					}
				}
			}
		}

	private void screenPosition3D(Point3f meta, Point3i screen) {
		float rotx = mEyeOffset
				   + mMatrix[0][0]*meta.x + mMatrix[1][0]*meta.y + mMatrix[2][0]*meta.z;
		float roty = mMatrix[0][1]*meta.x + mMatrix[1][1]*meta.y + mMatrix[2][1]*meta.z;
		float rotz = mMatrix[0][2]*meta.x + mMatrix[1][2]*meta.y + mMatrix[2][2]*meta.z;

		float zoom = cLocation / (cLocation - rotz);
		screen.x = mScreenCenterX+(int)(rotx*zoom)+mScreenCenterOffset;
		screen.y = mScreenCenterY-(int)(roty*zoom);
		screen.z = (int)((cLocation-rotz)*mScreenZoom);
		}

	private boolean isFaceVisible(Point3i p1, Point3i p2, Point3i p3) {
		return (Molecule.getAngleDif(Molecule.getAngle(p1.x, p1.y, p2.x, p2.y),
									 Molecule.getAngle(p2.x, p2.y, p3.x, p3.y)) > 0);
		}

	/* TODO check, whether we need to reset meta coordinates somewhere
	@Override
	protected void determineChartBasics() {
		int oldChartType = mChartType;
		super.determineChartBasics();
		if (mChartType != oldChartType)
			invalidateMetaCoordinates(-1);
		}*/

	private void calculateAxes() {
		for (int i=0; i<8; i++)
			screenPosition3D(cGraphPoint[i], mScreenCorner[i]);

		for (int i=0; i<12; i++)
			mEdgeHidden[i] = false;
		
		for (int i=0; i<6; i++) {
			Point3i p1 = mScreenCorner[cPointOfFace[cCube][i][0]];
			Point3i p2 = mScreenCorner[cPointOfFace[cCube][i][1]];
			Point3i p3 = mScreenCorner[cPointOfFace[cCube][i][2]];
			mFaceHidden[i] = !isFaceVisible(p1, p2, p3);

			if (mFaceHidden[i])
				for (int j=0; j<4; j++)
					mEdgeHidden[cEdgeOfFace[i][j]] = true;
			}
		}

	private void applyZoomToFontSize(float zoom) {
		setFontHeight((int)((float)mFontHeight * zoom));
		}

	private void drawFaces() {
		mG3D.setColix(Graphics3D.getColix(mGraphFaceColor));
		for (int i=0; i<3; i++) {
			if (mFaceHidden[i]) {
					// draw faces 1 or 2 pixels behind axis grid to not conflict with it
				Point3i p1 = mScreenCorner[cPointOfFace[cCube][i][0]];
				Point3i p2 = mScreenCorner[cPointOfFace[cCube][i][1]];
				Point3i p3 = mScreenCorner[cPointOfFace[cCube][i][2]];
				Point3i p4 = mScreenCorner[cPointOfFace[cCube][i][3]];
				p1.z += mContentScaling;
				p2.z += mContentScaling;
				p3.z += mContentScaling;
				p4.z += mContentScaling;
				mG3D.calcSurfaceShade(p1, p2, p3);
				mG3D.fillTriangle(p1, p2, p3);
				mG3D.fillTriangle(p2, p3, p4);
				p1.z -= mContentScaling;
				p2.z -= mContentScaling;
				p3.z -= mContentScaling;
				p4.z -= mContentScaling;
				}
			}
		}

	private void drawStereoCursor() {
		VisualizationPoint3D vp = (VisualizationPoint3D)mHighlightedPoint;
		int stereoOffset = (mEyeOffset > 0) ? vp.stereoOffset : -vp.stereoOffset;
		int s = (int)(1+mContentScaling*getMarkerSize(vp));
		int x = Math.round(vp.screenX*mContentScaling) + stereoOffset;
		int y = Math.round(vp.screenY*mContentScaling) + s/8;
		int z = vp.screenZ*mContentScaling - s/2;
		int l = STEREO_CURSOR_LENGTH*mContentScaling;
		int w = STEREO_CURSOR_WIDTH*mContentScaling;

		mG3D.setColix(Graphics3D.getColix(0xFF000000));
		mG3D.drawfillTriangle(x, y, z, x-w/2, y+l/4, z-l, x+w/2, y+l/4, z-l);
		mG3D.drawfillTriangle(x-w/4, y+l/4, z-l, x+w/4, y+l/4, z-l, x-w/4, y+l/2, z-2*l);
		mG3D.drawfillTriangle(x+w/4, y+l/2, z-3*l, x+w/4, y+l/4, z-l, x-w/4, y+l/2, z-2*l);
		}

	private void drawAxes() {
		if (mScaleMode != cScaleModeHidden) {
//			boolean showArrow = (mScaleStyle == cScaleStyleArrows);
			boolean showArrow = true;
			for (int i=0; i<3; i++) {
				int x1 = mScreenCorner[cCornerOfEdge[i][0]].x;
				int x2 = mScreenCorner[cCornerOfEdge[i][1]].x;
				int y1 = mScreenCorner[cCornerOfEdge[i][0]].y;
				int y2 = mScreenCorner[cCornerOfEdge[i][1]].y;
				int z1 = mScreenCorner[cCornerOfEdge[i][0]].z;
				int z2 = mScreenCorner[cCornerOfEdge[i][1]].z;
				int surplusX = showArrow ? (x2-x1) / 20 : 0;
				int surplusY = showArrow ? (y2-y1) / 20 : 0;
				int surplusZ = showArrow ? (z2-z1) / 20 : 0;
	
				String label = null;
				if (mAxisIndex[i] != -1)
					label = getAxisTitle(mAxisIndex[i]);
				else if (mChartType == cChartTypeBars && mChartInfo.barAxis == i)
					label = (mChartMode == cChartModeCount ? "Count"
							: mChartMode == cChartModePercent ? "Percent"
							: CHART_MODE_AXIS_TEXT[mChartMode]+"("+mTableModel.getColumnTitle(mChartColumn)+")");
	
				Point3i base = new Point3i(x2+surplusX, y2+surplusY, z2+surplusZ);
				Point3i tip = new Point3i(x2+2*surplusX, y2+2*surplusY, z2+2*surplusZ);

				if (showArrow) {
					// fillCone changes current COLIX
					mG3D.setColix(Graphics3D.getColix(0xFF000000 | getContrastGrey(0.8f).getRGB()));
					mG3D.fillCone(Graphics3D.ENDCAPS_FLAT, (int) (mScreenZoom / 30), base, tip);
					}
	
				mG3D.setColix(Graphics3D.getColix(0xFF000000 | getContrastGrey(0.8f).getRGB()));
				mG3D.drawLine(x1, y1, z1, x2+surplusX, y2+surplusY, z2+surplusZ);
	
				if (label != null && label.length() != 0) {
					applyZoomToFontSize(mScreenZoom * cLocation / mScreenCorner[cCornerOfEdge[i][1]].z);

					if (showArrow) {
						if (x2 < x1)
							tip.x -= mG3D.getFont3DCurrent().fontMetrics.stringWidth(label);
						if (y2 > y1)
							tip.y += mG3D.getFont3DCurrent().fontMetrics.getHeight();

						mG3D.drawStringNoSlab(label, null, tip.x, tip.y, tip.z);
						}
					else {
						// not yet supported
						// If we have no arrow tips, then labels should not be positioned at the arrow positions anymore.
						// They should go to the middle of the axes, translated a little away from the cube center
						// and rotated in space to be parallel to the axis.
						// Graphics3D does not support font rotation in space. Thus, a satisfactory solution would be difficult.
						}
					}
	
				}
			}

		if (mGridMode == cGridModeShown) {
			mG3D.setColix(Graphics3D.getColix(0xFF000000 | getContrastGrey(0.6f).getRGB()));
			int firstEdge = (mScaleMode == cScaleModeHidden) ? 0 : 3;
			for (int i=firstEdge; i<12; i++)
				mG3D.drawLine(mScreenCorner[cCornerOfEdge[i][0]].x,
							  mScreenCorner[cCornerOfEdge[i][0]].y,
							  mScreenCorner[cCornerOfEdge[i][0]].z,
							  mScreenCorner[cCornerOfEdge[i][1]].x,
							  mScreenCorner[cCornerOfEdge[i][1]].y,
							  mScreenCorner[cCornerOfEdge[i][1]].z);
			}

		for (int i=0; i<3; i++)
			drawGrid(i);
		}

	private void drawGrid(int face) {
		for (int edge=0; edge<2; edge++) {
			int axis = cEdgeOfFace[face][edge];

			if (mAxisIndex[axis] == cColumnUnassigned) {
				if (mChartType == cChartTypeBars && mChartInfo.barAxis == axis)
					drawDoubleScale(face, edge, axis);
				}
			else {
				if (mIsCategoryAxis[axis])
					drawCategoryScale(face, edge, axis);
				else
					drawDoubleScale(face, edge, axis);
				}
			}
		}

	private void drawCategoryScale(int face, int edge, int axis) {
		if (mAxisVisMax[axis] - mAxisVisMin[axis] > 32.0)
			return;

		String[] categoryList = mTableModel.getCategoryList(mAxisIndex[axis]);
		if (categoryList.length == 0)
			return;

		int min = Math.round(mAxisVisMin[axis] + 0.5f);
		int max = Math.round(mAxisVisMax[axis] - 0.5f);

		if (mTableModel.isColumnTypeRangeCategory(mAxisIndex[axis])
		 && (mChartType != cChartTypeBars || axis != mChartInfo.barAxis)) {
			for (int i=min; i<=max+1; i++) {
				float position = ((float)i - 0.5f - mAxisVisMin[axis]) / (mAxisVisMax[axis] - mAxisVisMin[axis]) * 2.0f - 1.0f;
				int index = categoryList[Math.min(i, max)].indexOf(CompoundTableConstants.cRangeSeparation);
				String label = (i <= max) ? categoryList[i].substring(0, index)
						: categoryList[max].substring(index+CompoundTableConstants.cRangeSeparation.length());
				drawScaleLine(face, edge, axis, i, position, label);
				}
			}
		else {
			for (int i=min; i<=max; i++) {
				float scalePosition = (mChartType == cChartTypeBars && axis == mChartInfo.barAxis && mChartInfo.axisMax != mChartInfo.axisMin) ?
						(mChartInfo.barBase - mChartInfo.axisMin) / (mChartInfo.axisMax - mChartInfo.axisMin) - 0.5f + i : i;
				float edgePosition = (scalePosition - mAxisVisMin[axis])
						/ (mAxisVisMax[axis] - mAxisVisMin[axis]) * 2.0f - 1.0f;
				String label = (mScaleMolecule[axis] == null) ? categoryList[i] : null;
				drawScaleLine(face, edge, axis, i, edgePosition, label);
				}
			}
		}

	private void drawDoubleScale(int face, int edge, int axis) {
		float axisStart,axisLength,totalRange;

		if (mAxisIndex[axis] == -1) {	// bar axis of bar chart
			if (mChartMode != cChartModeCount
			 && mChartMode != cChartModePercent
			 && mTableModel.isLogarithmicViewMode(mChartColumn)) {
				drawLogarithmicScale(face, edge, axis);
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
				drawLogarithmicScale(face, edge, axis);
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
			float edgePosition = ((float)theMarker-axisStart) / axisLength * 2.0f - 1.0f;

			if (mAxisIndex[axis] != -1 && mTableModel.isColumnTypeDate(mAxisIndex[axis])) {
				String label = createDateLabel(theMarker, exponent);
				if (label != null)
					drawScaleLine(face, edge, axis, -1, edgePosition, label);
				}
			else
				drawScaleLine(face, edge, axis, -1, edgePosition, DoubleFormat.toShortString(theMarker, exponent));

			theMarker += gridSpacing;
			}
		}

	private void drawLogarithmicScale(int face, int edge, int axis) {
		float axisStart,axisLength,totalRange;

		if (mAxisIndex[axis] == -1) {	// bar axis of bar chart
			axisStart = mChartInfo.axisMin;
			axisLength = mChartInfo.axisMax - mChartInfo.axisMin;
			totalRange = axisLength;
			}
		else {
			axisStart = mAxisVisMin[axis];
			axisLength = mAxisVisMax[axis] - mAxisVisMin[axis];
			totalRange = mTableModel.getMaximumValue(mAxisIndex[axis])
					   - mTableModel.getMinimumValue(mAxisIndex[axis]);
			}

		if (axisLength == 0.0
		 || axisLength < totalRange/100000)
			return;

		int intMin = (int)Math.floor(axisStart);
		int intMax = (int)Math.floor(axisStart+axisLength);

		if (axisLength > 4.5) {
			int step = 1 + (int)axisLength/10;
			for (int i=intMin; i<=intMax; i+=step)
				drawLogarithmicScaleLine(face, edge, axis, i);
			}
		else if (axisLength > 3.0) {
			for (int i=intMin; i<=intMax; i++) {
				drawLogarithmicScaleLine(face, edge, axis, i);
				drawLogarithmicScaleLine(face, edge, axis, i + 0.47712125472f);
				}
			}
		else if (axisLength > 1.5) {
			for (int i=intMin; i<=intMax; i++) {
				drawLogarithmicScaleLine(face, edge, axis, i);
				drawLogarithmicScaleLine(face, edge, axis, i + 0.301029996f);
				drawLogarithmicScaleLine(face, edge, axis, i + 0.698970004f);
				}
			}
		else if (axisLength > 1.0) {
			for (int i=intMin; i<=intMax; i++) {
				drawLogarithmicScaleLine(face, edge, axis, i);
				drawLogarithmicScaleLine(face, edge, axis, i + 0.176091259f);
				drawLogarithmicScaleLine(face, edge, axis, i + 0.301029996f);
				drawLogarithmicScaleLine(face, edge, axis, i + 0.477121255f);
				drawLogarithmicScaleLine(face, edge, axis, i + 0.698970004f);
				drawLogarithmicScaleLine(face, edge, axis, i + 0.84509804f);
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
				float edgePosition = (log-axisStart) / axisLength * 2.0f - 1.0f;
				drawScaleLine(face, edge, axis, -1, edgePosition, DoubleFormat.toShortString(theMarker, exponent));
				theMarker += gridSpacing;
				}
			}
		}

	private void drawLogarithmicScaleLine(int face, int edge, int axis, float value) {
		float min = (mAxisIndex[axis] == -1) ? mChartInfo.axisMin : mAxisVisMin[axis];
		float max = (mAxisIndex[axis] == -1) ? mChartInfo.axisMax : mAxisVisMax[axis];
		if (value >= min && value <= max) {
			float edgePosition = (value-min) / (max - min) * 2.0f - 1.0f;
			drawScaleLine(face, edge, axis, -1, edgePosition, DoubleFormat.toString(Math.pow(10, value), 3, true));
			}
		}

	private void drawScaleLine(int face, int edge, int axis, int index, float edgePosition, String label) {
		Point3f p1 = new Point3f();
		Point3f p2 = new Point3f();
		switch (face) {
		case 0:
			if (edge == 0) {
				p1.x = p2.x = edgePosition;
				p1.y = -1;
				p2.y = 1;
				}
			else {
				p1.x = -1;
				p2.x = 1;
				p1.y = p2.y = edgePosition;
				}
			p1.z = p2.z = -1;
			break;
		case 1:
			if (edge == 0) {
				p1.x = p2.x = edgePosition;
				p1.z = -1;
				p2.z = 1;
				}
			else {
				p1.x = -1;
				p2.x = 1;
				p1.z = p2.z = edgePosition;
				}
			p1.y = p2.y = -1;
			break;
		default:
			if (edge == 0) {
				p1.y = p2.y = edgePosition;
				p1.z = -1;
				p2.z = 1;
				}
			else {
				p1.y = -1;
				p2.y = 1;
				p1.z = p2.z = edgePosition;
				}
			p1.x = p2.x = -1;
			break;
			}

		Point3i s1 = new Point3i();
		Point3i s2 = new Point3i();
		screenPosition3D(p1, s1);
		screenPosition3D(p2, s2);

		if (mGridMode == cGridModeShown) {
			float ddx = (p2.x - p1.x) / 10;
			float ddy = (p2.y - p1.y) / 10;
			float ddz = (p2.z - p1.z) / 10;
			Point3f t = new Point3f(p1);
			Point3i st1 = new Point3i();
			Point3i st2 = new Point3i();
	
			mG3D.setColix(Graphics3D.getColix(0xFF000000 | getContrastGrey(0.5f, getGraphFaceColor()).getRGB()));
			for (int i=0; i<10; i++) {
				screenPosition3D(t, st1);
				t.x += ddx;
				t.y += ddy;
				t.z += ddz;
				screenPosition3D(t, st2);
				mG3D.drawLine(st1.x, st1.y, st1.z, st2.x, st2.y, st2.z);
				}
			}

		if (mScaleMode != cScaleModeHidden) {
			if (label != null && label.length() != 0) {
				applyZoomToFontSize(mScreenZoom * cLocation / s2.z);
				FontMetrics metrics = mG3D.getFont3DCurrent().fontMetrics;
				float lx = 0.5f * (float)metrics.stringWidth(label);
				float ly = 0.5f * (float)metrics.getHeight();
				float dx = s2.x - s1.x;
				float dy = s2.y - s1.y;
	
				float fx,fy;
				if (dy == 0) {
					fy = 0.0f;
					fx = (dx > 0) ? 1.0f : -1.0f;
					}
				else {
					fy = 1.0f/(float)Math.sqrt(1.0+dx*dx/(dy*dy));
					if (dy < 0)
						fy = -fy;
	
					fx = fy*dx/dy;
					}
	
				mG3D.setColix(Graphics3D.getColix(0xFF000000 | getContrastGrey(0.80f).getRGB()));
				mG3D.drawStringNoSlab(label, null, s2.x+(int)((1.3*fx-1.0)*lx), s2.y-3+(int)((1.2*fy+1.0)*ly), s2.z);
				}
			else if (mScaleMolecule[axis] != null) {
				if (mScaleMolecule[axis][index] != null) {
					float zoom = mScreenZoom * cLocation / s2.z;
		
					float axisLength = mAxisVisMax[axis] - mAxisVisMin[axis];
					if (axisLength > cZoomStopMolCount)
						zoom *= cZoomStopMolCount/axisLength;
		
					mScaleMolecule[axis][index].zoomAndRotate(zoom, 0.0f, false);
					float l = 0.5f * zoom * cMolsizeFactor * (float)mMoleculeAxisSize[axis];
					float dx = s2.x - s1.x;
					float dy = s2.y - s1.y;
		
					float f = 1.2f * l / (float)Math.sqrt(dx*dx+dy*dy);
		
					drawScaleMolecule(mG3D, (int)(s2.x+f*dx), (int)(s2.y+f*dy), s2.z, axis, index);
					}
				}
			}
		}

	private void drawScaleMolecule(Graphics3D g3D, int x, int y, int z, int axis, int index) {
		StereoMolecule mol = mScaleMolecule[axis][index];
		if (mMoleculeOffsetX[axis][index] != x
		 || mMoleculeOffsetY[axis][index] != y) {
			float dx = x - mMoleculeOffsetX[axis][index];
			float dy = y - mMoleculeOffsetY[axis][index];
			mol.translateCoords(dx, dy);
			mMoleculeOffsetX[axis][index] = x;
			mMoleculeOffsetY[axis][index] = y;
			}

		Depictor3D depictor = new Depictor3D(mol, Depictor3D.cDModeSuppressChiralText);
		depictor.setOverruleColor(getContrastGrey(1.0f), getViewBackground());
		depictor.paint(g3D);
		mCurrentFontSize3D = -1;	// invalidate
		}

	protected void drawMarker(Color color, int shape, int size, int x, int y) {
		mComposedMarker[shape].calculate(mAAFactor*size,
				mAAFactor*x-mScreenCenterX,
				mAAFactor*y-mScreenCenterY,
										 -(int)(mScreenZoom*cLocation/2));
		mComposedMarker[shape].draw(Graphics3D.getColix(color.getRGB()));
		}

	private void drawMarkers(Rectangle clipRect) {
		int focusFlagNo = getFocusFlag();
		int labelFlagNo = getLabelFlag();

		boolean showAnyLabels = showAnyLabels();
		boolean isTreeView = isTreeViewGraph();
		boolean isDarkBackground = (ColorHelper.perceivedBrightness(getGraphFaceColor()) < 0.5);

		TreeMap<byte[],VisualizationPoint> oneLabelPerCategoryMap = showAnyLabels ? buildOnePerCategoryMap() : null;

		boolean isFilter = mUseAsFilterFlagNo != -1 && !mTableModel.isRowFlagSuspended(mUseAsFilterFlagNo);

		// If two markers with equal coordinates are drawn in a Graphics3D
		// then the first to be drawn is the one finally visible!
		// This is inverse to the JVisualization2D and since 
		for (int i=mDataPoints-1; i>=0; i--) {
			boolean drawLabels = false;
			if (isVisible(mPoint[i])) {
				boolean outOfFocus = focusFlagNo != -1 && !mPoint[i].record.isFlagSet(focusFlagNo);
				VisualizationPoint3D vp = (VisualizationPoint3D)mPoint[i];

				drawLabels = showAnyLabels
						&& (labelFlagNo == cLabelsOnAllRows || mPoint[i].record.isFlagSet(labelFlagNo))
					    && (oneLabelPerCategoryMap==null || vp==oneLabelPerCategoryMap.get(vp.record.getData(mOnePerCategoryLabelCategoryColumn)));

				mComposedMarker[vp.shape].calculate(vp);
				boolean drawCenterLabel = mLabelColumn[MarkerLabelDisplayer.cMidCenter] != cColumnUnassigned
									   && drawLabels
									   && (!mLabelsInTreeViewOnly || isTreeView);
				boolean drawMarker = mComposedMarker[vp.shape].size != 0
								  && !drawCenterLabel
								  && (clipRect == null
								   || mComposedMarker[vp.shape].calculateBounds(vp, isTreeView).intersects(clipRect));

				if (drawMarker || drawLabels) {
					Color color = (vp == mActivePoint) ? Color.red
								: (isFilter && !vp.record.isFlagSet(mUseAsFilterFlagNo)) ? cUseAsFilterColor
								: (vp.record.isSelected() && mFocusList != FocusableView.cFocusOnSelection) ?
										   VisualizationColor.cSelectedColor : mMarkerColor.getColorList()[vp.colorIndex];

					if (vp == mHighlightedPoint && (clipRect != null || mIsAdjusting))
						color = color.darker().darker();

					if (outOfFocus || isNaN(mPoint[i]))
						color = VisualizationColor.grayOutColor(color);

					short colix = Graphics3D.getColix(color.getRGB());
					if (drawMarker)
						mComposedMarker[vp.shape].draw(colix);

					if (drawLabels) {
						Color lc = mIsMarkerLabelsBlackAndWhite ? getContrastGrey(1f, getGraphFaceColor())
								: isDarkBackground ? color : color.darker();
						mComposedMarker[vp.shape].drawLabels(vp, lc, isTreeView, clipRect);
						}
					}
				}

			if (!drawLabels)
				mPoint[i].removeNonCustomLabelPositions();
			}
		}

	private void drawConnectionLines(Rectangle clipRect) {
		if (mConnectionColumn != cColumnUnassigned) {
			if (mAbsoluteConnectionLineWidth < 0.5f)
				return;

			// accounts for line thicker than 1
			if (clipRect != null) {
				clipRect = new Rectangle(clipRect);
				clipRect.grow((int)(mContentScaling*mAbsoluteConnectionLineWidth/2), (int)(mContentScaling*mAbsoluteConnectionLineWidth/2));
				}

			String value = (mConnectionColumn < 0) ?
					null : mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferencedColumn);
			if (value == null) {
				drawCategoryConnectionLines(clipRect);
				}
			else {
				int referencedColumn = mTableModel.findColumn(value);
				if (referencedColumn != -1)
					drawReferenceConnectionLines(clipRect, referencedColumn);
				}
			}
		}

	private void drawCategoryConnectionLines(Rectangle clipRect) {
		if (mConnectionLinePoint == null || mConnectionLinePoint.length != mPoint.length)
			mConnectionLinePoint = new VisualizationPoint[mPoint.length];
		for (int i=0; i<mPoint.length; i++)
			mConnectionLinePoint[i] = mPoint[i];

		Arrays.sort(mConnectionLinePoint, new Comparator<VisualizationPoint>() {
			public int compare(VisualizationPoint p1, VisualizationPoint p2) {
				return compareConnectionLinePoints(p1, p2);
				}
			} );

		int fromIndex1 = 0;
		while (fromIndex1<mConnectionLinePoint.length
			&& !isVisibleExcludeNaN(mConnectionLinePoint[fromIndex1]))
			fromIndex1++;

		if (fromIndex1 == mConnectionLinePoint.length)
			return;

		int fromIndex2 = getNextChangedConnectionLinePointIndex(fromIndex1);
		if (fromIndex2 == mConnectionLinePoint.length)
			return;

		while (true) {
			int toIndex1 = fromIndex2;

			while (toIndex1<mConnectionLinePoint.length
				&& !isVisibleExcludeNaN(mConnectionLinePoint[toIndex1]))
				toIndex1++;

			if (toIndex1 == mConnectionLinePoint.length) {
				return;
				}

			int toIndex2 = getNextChangedConnectionLinePointIndex(toIndex1);

			if (isConnectionLinePossible(mConnectionLinePoint[fromIndex1], mConnectionLinePoint[toIndex1]))
				for (int i=fromIndex1; i<fromIndex2; i++)
					if (isVisibleExcludeNaN(mConnectionLinePoint[i]))
						for (int j=toIndex1; j<toIndex2; j++)
							if (isVisibleExcludeNaN(mConnectionLinePoint[j])
							 && (clipRect == null || clipRect.intersectsLine(mContentScaling*mConnectionLinePoint[i].screenX,
																			  mContentScaling*mConnectionLinePoint[i].screenY,
																			  mContentScaling*mConnectionLinePoint[j].screenX,
																			  mContentScaling*mConnectionLinePoint[j].screenY)))
								drawConnectionLine(mConnectionLinePoint[i], mConnectionLinePoint[j], 1.0f, false);

			fromIndex1 = toIndex1;
			fromIndex2 = toIndex2;
			}
		}

	private void drawReferenceConnectionLines(Rectangle clipRect, int referencedColumn) {
		if (mConnectionLineMap == null)
			mConnectionLineMap = createReferenceMap(mConnectionColumn, referencedColumn);

		String value = (mConnectionColumn < 0) ?
				null : mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferencedColumn);
		if (value != null) {
			boolean isRedundant = CompoundTableConstants.cColumnPropertyReferenceTypeRedundant.equals(
					mTableModel.getColumnProperty(mConnectionColumn, CompoundTableConstants.cColumnPropertyReferenceType));
			int strengthColumn = mTableModel.findColumn(mTableModel.getColumnProperty(mConnectionColumn,
					CompoundTableConstants.cColumnPropertyReferenceStrengthColumn));

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
							if (vp2 != null
							 && isVisible(vp2)
							 && (!isRedundant || (vp1.record.getID() < vp2.record.getID()))
							 && (clipRect == null || clipRect.intersectsLine(mContentScaling*vp1.screenX, mContentScaling*vp1.screenY,
																			  mContentScaling*vp2.screenX, mContentScaling*vp2.screenY))) {
								float intensity = 1.0f;
								if (strength != null) {
									try {
										float v = mTableModel.tryParseEntry(strength[index++], strengthColumn);
										intensity = Float.isNaN(v) ? 0.2f : (float)((v-min) / dif);
										}
									catch (NumberFormatException nfe) {}
									}
								drawConnectionLine(vp1, vp2, intensity, !isRedundant);
								}
							}
						}
					}
				}
			}
		}

	private void drawConnectionLine(VisualizationPoint p1, VisualizationPoint p2, float intensity, boolean showArrow) {
		int focusFlagNo = (mFocusList == FocusableView.cFocusNone) ? -1
						: (mFocusList == FocusableView.cFocusOnSelection) ? CompoundRecord.cFlagSelected
						: mTableModel.getListHandler().getListFlagNo(mFocusList);

		VisualizationPoint3D vp1 = (VisualizationPoint3D)p1;
		VisualizationPoint3D vp2 = (VisualizationPoint3D)p2;

		boolean grayOut1 = focusFlagNo != -1 && !vp1.record.isFlagSet(focusFlagNo);
		boolean grayOut2 = focusFlagNo != -1 && !vp2.record.isFlagSet(focusFlagNo);

		int so1 = !mIsStereo ? 0 : (mEyeOffset > 0) ? vp1.stereoOffset : -vp1.stereoOffset;
		int so2 = !mIsStereo ? 0 : (mEyeOffset > 0) ? vp2.stereoOffset : -vp2.stereoOffset;

		int x1 = Math.round(mContentScaling*vp1.screenX)+so1;
		int y1 = Math.round(mContentScaling*vp1.screenY);
		int z1 = Math.round(mContentScaling*vp1.screenZ);
		int x2 = Math.round(mContentScaling*vp2.screenX)+so2;
		int y2 = Math.round(mContentScaling*vp2.screenY);
		int z2 = Math.round(mContentScaling*vp2.screenZ);

		Color color1 = mMarkerColor.getColorList()[p1.colorIndex];

		if (showArrow) {
			float dx = x2 - x1;
			float dy = y2 - y1;
			float dz = z2 - z1;
			float len = (float)Math.sqrt(dx*dx+dy*dy+dz*dz);
			float s = 1.5f*mContentScaling*mAbsoluteConnectionLineWidth;
			if (len > 2*s) {
				float xm = x1 + dx / 2;
				float ym = y1 + dy / 2;
				float zm = z1 + dz / 2;
				float ndx = dx * s / len;
				float ndy = dy * s / len;
				float ndz = dz * s / len;
				if (mIsConnectionLineInverted) {
					ndx = -ndx;
					ndy = -ndy;
					ndz = -ndz;
					}
				Point3i base = new Point3i(Math.round(xm - ndx), Math.round(ym - ndy), Math.round(zm - ndz));
				Point3i tip = new Point3i(Math.round(xm + ndx), Math.round(ym + ndy), Math.round(zm + ndz));

				// fillCone changes current COLIX
				mG3D.setColix(Graphics3D.getColix(color1.getRGB()));
				mG3D.fillCone(Graphics3D.ENDCAPS_FLAT, Math.round(2*s), base, tip);
				}
			}

		if (p1.colorIndex != p2.colorIndex || grayOut1 != grayOut2) {
			Color color2 = mMarkerColor.getColorList()[p2.colorIndex];
			if (intensity != 1.0) {
				color1 = ColorHelper.intermediateColor(getViewBackground(), color1, intensity);
				color2 = ColorHelper.intermediateColor(getViewBackground(), color2, intensity);
				}
			if (grayOut1)
				color1 = VisualizationColor.grayOutColor(color1);
			if (grayOut2)
				color2 = VisualizationColor.grayOutColor(color2);
	
			if ((int)mAbsoluteConnectionLineWidth == 1)
				mG3D.drawLine(Graphics3D.getColix(color1.getRGB()),Graphics3D.getColix(color2.getRGB()),
							  x1, y1, z1, x2, y2, z2);
			else
				mG3D.fillCylinder(Graphics3D.getColix(color1.getRGB()),Graphics3D.getColix(color2.getRGB()),
								  Graphics3D.ENDCAPS_SPHERICAL, (int)(mContentScaling*mAbsoluteConnectionLineWidth),
								  x1, y1, z1, x2, y2, z2);
			}
		else {
			if (grayOut1)
				color1 = VisualizationColor.grayOutColor(color1);
	
			mG3D.setColix(Graphics3D.getColix(color1.getRGB()));

			if ((int)mAbsoluteConnectionLineWidth == 1)
				mG3D.drawLine(x1, y1, z1, x2, y2, z2);
			else
				mG3D.fillCylinder(Graphics3D.ENDCAPS_SPHERICAL, (int)(mContentScaling*mAbsoluteConnectionLineWidth),
								  x1, y1, z1, x2, y2, z2);
			}
		}

	private void drawBars(Rectangle clipRect) {
		if (!mChartInfo.barOrPieDataAvailable)
			return;

		int catCount = getCategoryVisCount(0)* getCategoryVisCount(1)* getCategoryVisCount(2);
		int focusFlagNo = getFocusFlag();
		int basicColorCount = mMarkerColor.getColorList().length + 2;
		int colorCount = basicColorCount * ((focusFlagNo == -1) ? 1 : 2);

		if (mHighlightedPoint != null && (clipRect != null || mIsAdjusting)) {
			short colix = Graphics3D.getColix(mChartInfo.color[mHighlightedPoint.colorIndex].darker().darker().getRGB());
			getBarFraction((VisualizationPoint3D)mHighlightedPoint).draw(colix);
			}

		if (mActivePoint != null && isVisibleExcludeNaN(mActivePoint)) {
			short colix = Graphics3D.getColix(Color.RED.getRGB());
			getBarFraction((VisualizationPoint3D)mActivePoint).draw(colix);
			}

		BarFraction barFragment = new BarFraction(mChartInfo.barAxis, mChartInfo.barWidth, false);

		for (int cat=0; cat<catCount; cat++) {
			for (int k=0; k<colorCount; k++) {
				if (mChartInfo.pointsInColorCategory[0][cat][k] > 0) {
					float xoffset = (mChartInfo.barAxis == 0) ? mMetaBarColorEdge[cat][k] : mMetaBarPosition[0][cat];
					float yoffset = (mChartInfo.barAxis == 1) ? mMetaBarColorEdge[cat][k] : mMetaBarPosition[1][cat];
					float zoffset = (mChartInfo.barAxis == 2) ? mMetaBarColorEdge[cat][k] : mMetaBarPosition[2][cat];
					float height = mMetaBarColorEdge[cat][k+1] - mMetaBarColorEdge[cat][k];
					barFragment.calculate(height, xoffset, yoffset, zoffset);
					if (clipRect == null || barFragment.calculateBounds(null, false).intersects(clipRect)) {
						short colix = Graphics3D.getColix(mChartInfo.color[k].getRGB());
						barFragment.draw(colix);
						}
					}
				}
			}
		}

	private BarFraction getBarFraction(VisualizationPoint3D vp) {
		int cat = getChartCategoryIndex(vp);
		if (cat == -1)
			return null;

		if (mChartInfo.innerDistance == null && mChartInfo.absValueFactor == null)
			return null;

		BarFraction fraction = new BarFraction(mChartInfo.barAxis, mChartInfo.barWidth, true);
		if (mChartInfo.useProportionalFractions()) {
			float height = Math.abs(vp.record.getDouble(mChartColumn)) * mChartInfo.absValueFactor[0][cat];
			fraction.calculate(height, vp.coord[0], vp.coord[1], vp.coord[2]);
			}
		else {
			fraction.calculate(mChartInfo.innerDistance[0][cat], vp.coord[0], vp.coord[1], vp.coord[2]);
			}
		return fraction;
		}

	private void drawBoxesOrWhiskers(Rectangle clipRect) {
		// TODO
		}

	@Override
	protected VisualizationPoint createVisualizationPoint(CompoundRecord record) {
		return new VisualizationPoint3D(record);
		}

	public int getAvailableShapeCount() {
		return cAvailableShapeCount;
		}

	public int[] getSupportedChartTypes() {
		return SUPPORTED_CHART_TYPE;
		}

	class ComposedObject {
		Point3f[]	point;
		Point3i[]	screenPoint;	// used as temporary buffer for markers while drawing
		Point3f		metaPoint;		// used as temporary buffer for markers while drawing
		int[][]		pointOfFace;
		int			faceCount,type;
		float		size;
		Rectangle	bounds;
	
		public ComposedObject(int type, Point3f[] point, int[][] pointOfFace) {
			this.type = type;
			this.point = point;
			if (point != null) {
				this.metaPoint = new Point3f();
				this.screenPoint = new Point3i[point.length];
				for (int i=0; i<point.length; i++)
					screenPoint[i] = new Point3i();
				}
			this.pointOfFace = pointOfFace;
			if (pointOfFace != null)
				this.faceCount = pointOfFace.length;
			this.bounds = new Rectangle();
			}
	
		void calculate(float size, int xoffset, int yoffset, int zoffset) {
			this.size = size;
			float f = this.size / mScreenZoom;
			for (int i=0; i<point.length; i++) {
				metaPoint.x = f*point[i].x;
				metaPoint.y = f*point[i].y;
				metaPoint.z = f*point[i].z;
				screenPosition3D(metaPoint, screenPoint[i]);
				screenPoint[i].x += xoffset;
				screenPoint[i].y += yoffset;
				screenPoint[i].z += zoffset;
				}
			}

		void calculate(VisualizationPoint3D vp) {
			float markerSize = getMarkerSize(vp);
			vp.widthOrAngle1 = (int)markerSize;
			vp.heightOrAngle2 = (int)markerSize;
			this.size = mContentScaling*markerSize;
			float f = this.size / mScreenZoom;
			for (int i=0; i<point.length; i++) {
				metaPoint.x = vp.coord[0] + f*point[i].x;
				metaPoint.y = vp.coord[1] + f*point[i].y;
				metaPoint.z = vp.coord[2] + f*point[i].z;
				screenPosition3D(metaPoint, screenPoint[i]);
				}
			}

		Rectangle calculateBounds(VisualizationPoint3D vp, boolean isTreeView) {
			if (mLabelColumn[MarkerLabelDisplayer.cMidCenter] != cColumnUnassigned
			   && (!mLabelsInTreeViewOnly || isTreeView)) {
				int stereoOffset = (mEyeOffset > 0) ? vp.stereoOffset : -vp.stereoOffset;
				int column = mLabelColumn[MarkerLabelDisplayer.cMidCenter];
				boolean isMolecule = mTableModel.isColumnTypeStructure(column);
				bounds.x = Math.round(mContentScaling*vp.screenX)+stereoOffset;
				bounds.y = Math.round(mContentScaling*vp.screenY);

				DoubleDimension size = getLabelDimension(vp, isMolecule, cMidCenter, isTreeView, new DoubleDimension());
				if (size != null) {
					bounds.width = (int)(mContentScaling * size.width);
					bounds.height = (int)(mContentScaling * size.height);
					bounds.x -= bounds.width / 2;
					bounds.y -= bounds.height / 2;
					}
				else {
					bounds.width = 0;
					bounds.height = 0;
					}
				}
			else {
				switch (type) {
				case cSphere:
					int diameter = (int)(this.size*cDiameter[cSphere]+1);
					bounds.width = diameter;
					bounds.height = diameter;
					bounds.x = screenPoint[0].x - bounds.width/2;
					bounds.y = screenPoint[0].y - bounds.height/2;
					break;
				case cCone:
					bounds.width = (int)(this.size*cDiameter[cCone]+1);
					bounds.height = (int)(this.size*cDiameter[cCone]+1);
					bounds.x = screenPoint[0].x - bounds.width/2;
					bounds.y = screenPoint[0].y - bounds.height/2;
					bounds.add(screenPoint[1].x, screenPoint[1].y);
					break;
				case cCylinder:
					bounds.x = screenPoint[0].x;
					bounds.y = screenPoint[0].y;
					bounds.width = 0;
					bounds.height = 0;
					bounds.add(screenPoint[1].x, screenPoint[1].y);
					int radius = (int)(this.size*cDiameter[cCylinder]+1)/2;
					bounds.grow(radius, radius);
					break;
				default:
					bounds.x = screenPoint[0].x;
					bounds.y = screenPoint[0].y;
					int x2 = bounds.x;
					int y2 = bounds.y;
					for (int i=1; i<point.length; i++) {
						if (bounds.x > screenPoint[i].x)
							bounds.x = screenPoint[i].x;
						else if (x2 < screenPoint[i].x)
							x2 = screenPoint[i].x;
						if (bounds.y > screenPoint[i].y)
							bounds.y = screenPoint[i].y;
						else if (y2 < screenPoint[i].y)
							y2 = screenPoint[i].y;
						}
					bounds.width = x2-bounds.x+1;
					bounds.height = y2-bounds.y+1;
					}
				}

/* no need to increase cliprect as long as we only draw outline
			VisualizationLabelPosition lp = vp.labelPosition;
			while (lp != null) {
				bounds.add((lp.getScreenX()-1) * mAAFactor, (lp.getScreenY()-1) * mAAFactor);
				bounds.add((lp.getScreenX2()+1) * mAAFactor, (lp.getScreenY2()+1) * mAAFactor);
				lp = lp.getNext();
				}*/

			if (vp != null) {	// TODO involve stereo offset for BarFraction
				if (mEyeOffset != 0) {
					if (bounds.width < STEREO_CURSOR_WIDTH) {
						bounds.x -= (STEREO_CURSOR_WIDTH - bounds.width) / 2;
						bounds.width = STEREO_CURSOR_WIDTH;
						}
					int stereoShift = 2*Math.abs(vp.stereoOffset);
					if ((mEyeOffset > 0) ^ (vp.stereoOffset < 0))
						bounds.x -= stereoShift;
					bounds.width += stereoShift;
					}
				}

			if (mAAFactor == 2) {   // make sure that we can divide bounds in half without rest
				if ((bounds.x & 1) == 1) {
					bounds.x--;
					bounds.width++;
					}
				if ((bounds.width & 1) == 1) {
					bounds.width++;
					}
				if ((bounds.y & 1) == 1) {
					bounds.y--;
					bounds.height++;
					}
				if ((bounds.height & 1) == 1) {
					bounds.height++;
					}
				}

			return bounds;
			}
		
		void draw(short colix) {
			mG3D.setColix(colix);
			switch (type) {
			case cSphere:
				mG3D.fillSphereCentered((int)(this.size*cDiameter[cSphere]), screenPoint[0]);
				break;
			case cCone:
				mG3D.fillCone(Graphics3D.ENDCAPS_FLAT, (int)(this.size*cDiameter[cCone]), screenPoint[0], screenPoint[1]);
				break;
			case cCylinder:
				mG3D.fillCylinder(Graphics3D.ENDCAPS_FLAT, (int)(this.size*cDiameter[cCylinder]), screenPoint[0], screenPoint[1]);
				break;
			default:
				for (int i=0; i<faceCount; i++) {
					Point3i p1 = screenPoint[pointOfFace[i][0]];
					Point3i p2 = screenPoint[pointOfFace[i][1]];
					Point3i p3 = screenPoint[pointOfFace[i][2]];
					if (isFaceVisible(p1, p2, p3)) {
						mG3D.calcSurfaceShade(p1, p2, p3);
						mG3D.fillTriangle(p1, p2, p3);
						if (pointOfFace[i].length == 4)
							mG3D.fillTriangle(p2, p3, screenPoint[pointOfFace[i][3]]);
						}
					}
				}
			}

		private void drawLabels(VisualizationPoint3D vp, Color color, boolean isTreeView, Rectangle clipRect) {
			if (mLabelColumn[MarkerLabelDisplayer.cMidCenter] != cColumnUnassigned
			 && (!mLabelsInTreeViewOnly || isTreeView))
				drawLabel(vp, MarkerLabelDisplayer.cMidCenter, color, isTreeView, clipRect);

			for (int i=0; i<MarkerLabelDisplayer.cInTableLine1; i++)
				if (i != MarkerLabelDisplayer.cMidCenter
				 && mLabelColumn[i] != cColumnUnassigned
				 && (!mLabelsInTreeViewOnly || isTreeView))
					drawLabel(vp, i, color, isTreeView, clipRect);
			}

		private void drawLabel(VisualizationPoint3D vp, int position, Color color, boolean isTreeView, Rectangle clipRect) {
			int stereoOffset = (mEyeOffset > 0) ? vp.stereoOffset : -vp.stereoOffset;
			int column = mLabelColumn[position];
			boolean isMolecule = mTableModel.isColumnTypeStructure(column);
			int x = Math.round(mContentScaling*vp.screenX)+stereoOffset;
			int y = Math.round(mContentScaling*vp.screenY);
			int z = Math.round(mContentScaling*vp.screenZ);
			int w = Math.round(mContentScaling*vp.widthOrAngle1);
			int h = Math.round(mContentScaling*vp.heightOrAngle2);
			String label = null;
			Depictor3D depictor = null;
			Rectangle2D.Double molRect = null;

			// in case we have an empty label replacing the marker
			if (position == MarkerLabelDisplayer.cMidCenter) {
				vp.widthOrAngle1 = 0;
				vp.heightOrAngle2 = 0;
				}

			if (isMolecule) {
				if (mLabelMolecule == null)
					mLabelMolecule = new StereoMolecule();
				StereoMolecule mol = mTableModel.getChemicalStructure(vp.record, column, CompoundTableModel.ATOM_COLOR_MODE_EXPLICIT, mLabelMolecule);
				if (mol == null)
					return;

				depictor = new Depictor3D(mol, Depictor3D.cDModeSuppressChiralText);
				depictor.validateView(mG3D, DEPICTOR_RECT, AbstractDepictor.cModeInflateToHighResAVBL+mAAFactor*Math.max(1, (int)(256*getLabelAVBL(vp, position, isTreeView))));
				molRect = depictor.getBoundingRect();
				w = (int)molRect.width;
				h = (int)molRect.height;
				}
			else {
				label = mTableModel.getValue(vp.record, column);
				if (label.length() == 0)
					return;

				setFontHeight((int)getLabelFontSize(vp, position, isTreeView));
				w = mG3D.getFont3DCurrent().fontMetrics.stringWidth(label);
				h = mG3D.getFont3DCurrent().fontMetrics.getHeight();
				}

			float vpWidth = mAAFactor*vp.widthOrAngle1;
			float vpHeight = mAAFactor*vp.heightOrAngle2;

			LabelPosition3D labelPosition = (LabelPosition3D)vp.getOrCreateLabelPosition(column, true);
			if (labelPosition.isCustom()) {
				float dataX = labelPosition.getX();
				float dataY = labelPosition.getY();
				float dataZ = labelPosition.getZ();
				Point3f meta = new Point3f();
				meta.x = (mAxisVisMax[0] == mAxisVisMin[0]) ? 0f : 2f * (dataX - mAxisVisMin[0]) / (mAxisVisMax[0] - mAxisVisMin[0]) - 1f;
				meta.y = (mAxisVisMax[1] == mAxisVisMin[1]) ? 0f : 2f * (dataY - mAxisVisMin[1]) / (mAxisVisMax[1] - mAxisVisMin[1]) - 1f;
				meta.z = (mAxisVisMax[2] == mAxisVisMin[2]) ? 0f : 2f * (dataZ - mAxisVisMin[2]) / (mAxisVisMax[2] - mAxisVisMin[2]) - 1f;
				Point3i screen = new Point3i();
				screenPosition3D(meta, screen);

				x = screen.x - w/2;
				y = screen.y - h/2;
				z = screen.z;
				}
			else {
				switch (position) {
					case MarkerLabelDisplayer.cTopLeft:
						x -= vpWidth / 2 + w;
						y -= vpHeight / 6 + h;
						break;
					case MarkerLabelDisplayer.cTopCenter:
						x -= w / 2;
						y -= vpHeight / 2 + h;
						break;
					case MarkerLabelDisplayer.cTopRight:
						x += vpWidth / 2;
						y -= vpHeight / 6 + h;
						break;
					case MarkerLabelDisplayer.cMidLeft:
						x -= vpWidth * 2 / 3 + w;
						y -= h / 2;
						break;
					case MarkerLabelDisplayer.cMidCenter:
						vp.widthOrAngle1 = w / mAAFactor;
						vp.heightOrAngle2 = h / mAAFactor;
						x -= w / 2;
						y -= h / 2;
						break;
					case MarkerLabelDisplayer.cMidRight:
						x += vpWidth * 2 / 3;
						y -= h / 2;
						break;
					case MarkerLabelDisplayer.cBottomLeft:
						x -= vpWidth / 2 + w;
						y += vpHeight / 6;
						break;
					case MarkerLabelDisplayer.cBottomCenter:
						x -= w / 2;
						y += vpHeight / 2;
						break;
					case MarkerLabelDisplayer.cBottomRight:
						x += vpWidth / 2;
						y += vpHeight / 6;
						break;
					}
				}

			int border = 0;

			if (clipRect == null || clipRect.intersects(x, y, w, h)) {
				if (mShowLabelBackground) {
					border = (int)(0.15f*mAAFactor*mCurrentFontSize3D);
					short colix = Graphics3D.getColix(mLabelBackground.getRGB());
//					if (mLabelBackgroundTransparency == 0f)	// translucency in JMol doesn't seem to work
						mG3D.setColix(colix);
//					else
//						mG3D.setColix(Graphics3D.getColixTranslucent(colix, true, mLabelBackgroundTransparency));
					mG3D.fillRect(x-border, y-border, z+1, 0, w+2*border, h+2*border);
					mG3D.setColix(Graphics3D.getColix(color.getRGB()));
					mG3D.drawRect(x-border, y-border, z, 0, w+2*border, h+2*border);
					}

				if (clipRect == null && !mIsHighResolution) {
					labelPosition.setScreenLocation((x - border) / mContentScaling, (y - border) / mContentScaling,
							(x + w + border) / mContentScaling, (y + h + border) / mContentScaling);
					labelPosition.setScreenZ(z / mContentScaling);
					}

				// For custom located labels we may need to draw a line from marker to label edge
				if (labelPosition.isCustom()) {
					Point connectionPoint = getLabelConnectionPoint(vp.screenX*mContentScaling, vp.screenY*mContentScaling,
							x - border, y - border, x + w + border, y + h + border);
					if (connectionPoint != null) {
						mG3D.setColix(Graphics3D.getColix(color.getRGB()));
//						mG3D.drawLine(vp.screenX*mContentScaling, vp.screenY*mContentScaling, vp.screenZ,
//								connectionPoint.x, connectionPoint.y, z*mContentScaling);
						int diameter = Math.max(1, Math.round(0.12f*mFontHeight));
						mG3D.fillCylinder(Graphics3D.ENDCAPS_FLAT,diameter, Math.round(vp.screenX*mContentScaling),
								Math.round(vp.screenY*mContentScaling), vp.screenZ*mAAFactor,
								connectionPoint.x, connectionPoint.y, z);
						}
					}

				if (isMolecule) {
					depictor.applyTransformation(new DepictorTransformation(1.0f, x - molRect.x, y - molRect.y));
					depictor.setOverruleColor(color, getViewBackground());
					depictor.setZ(z);
					depictor.paint(mG3D);

					mG3D.setFont(mG3D.getFont3D((float)mAAFactor*mCurrentFontSize3D));	// restore font size
					}
				else {
					mG3D.setColix(Graphics3D.getColix(color.getRGB()));
					mG3D.drawString(label, null, x, y+mG3D.getFont3DCurrent().fontMetrics.getAscent(), z, 0);
					}
				}
			}
		}

	class BarFraction extends ComposedObject {
		private int axis;

		private BarFraction(int axis, float barWidth, boolean isMarker) {
			  super(cCube, cPoint[cCube], cPointOfFace[cCube]);
			  Point3f[] np = new Point3f[point.length];
			  float offset = isMarker ? 0f : 0.5f;
			  float factor = isMarker ? 1.1f : 1.0f;
			  for (int i=0; i<point.length; i++) {
				  np[i] = new Point3f();
				  switch (axis) {
				  case 0:
					  np[i].x = point[i].x * factor + offset;
					  np[i].y = point[i].y * factor * barWidth;
					  np[i].z = point[i].z * factor * barWidth;
					  break;
				  case 1:
					  np[i].x = point[i].x * factor * barWidth;
					  np[i].y = point[i].y * factor + offset;
					  np[i].z = point[i].z * factor * barWidth;
					  break;
				  case 2:
					  np[i].x = point[i].x * factor * barWidth;
					  np[i].y = point[i].y * factor * barWidth;
					  np[i].z = point[i].z * factor + offset;
					  break;
					  }
				  }
			  point = np;
			  this.axis = axis;
			}

		void calculate(float height, float xoffset, float yoffset, float zoffset) {
			float fx = (axis == 0) ? height : 1.0f;
			float fy = (axis == 1) ? height : 1.0f;
			float fz = (axis == 2) ? height : 1.0f;
			for (int i=0; i<point.length; i++) {
				metaPoint.x = fx * point[i].x + xoffset;
				metaPoint.y = fy * point[i].y + yoffset;
				metaPoint.z = fz * point[i].z + zoffset;
				screenPosition3D(metaPoint, screenPoint[i]);
				}
			}
		}

	private class V3DWorker implements Runnable {
		private static final int PAINT_CONTENT = 1;
		private static final int MERGE_IMAGES = 2;

		private CountDownLatch mDoneSignal;
		private int mThreadIndex;
		private int mAction;
		private Rectangle mSubArea;
		private boolean mIsOdd;
		private int[] mRGB,mRGB1,mRGB2;
		private Graphics mG;
		private Rectangle mGraphBounds;

		private V3DWorker(int threadIndex) {
			mThreadIndex = threadIndex;
			}

/*		public void initPaintContent(Graphics g, Rectangle graphBounds, Rectangle area, CountDownLatch doneSignal) {
			mAction = PAINT_CONTENT;
			mG = g;
			mGraphBounds = graphBounds;
			mSubArea = getSubArea(area);
			mDoneSignal = doneSignal;
			}
*/
		/**
		 * 
		 * @param rgb buffer for stereo image
		 * @param rgb1 left eye buffer
		 * @param rgb2 right eye buffer
		 * @param isOdd true if STEREO_MODE_VERTICAL_INTERLACE and area.x is odd screen pixel
		 *				   or if STEREO_MODE_HORIZONTAL_INTERLACE and area.y is odd screen pixel
		 * @param area
		 * @param doneSignal
		 */
		public void initMergeImages(int[] rgb, int[] rgb1, int[] rgb2, boolean isOdd, Rectangle area, CountDownLatch doneSignal) {
			mAction = MERGE_IMAGES;
			mRGB  = rgb;
			mRGB1 = rgb1;
			mRGB2 = rgb2;
			mIsOdd = isOdd;
			mSubArea = getSubArea(area);
			if (mStereoMode == STEREO_MODE_H_INTERLACE_LEFT_EYE_FIRST && ((area.y - mSubArea.y) & 1) == 1)
				mIsOdd = !mIsOdd;
			if (mStereoMode == STEREO_MODE_H_INTERLACE_RIGHT_EYE_FIRST && ((area.y - mSubArea.y) & 1) == 0)
				mIsOdd = !mIsOdd;
			mDoneSignal = doneSignal;
			}

		public void run() {
			switch (mAction) {
			case PAINT_CONTENT:
				paintContent(mG, mGraphBounds, mSubArea);
				break;
			case MERGE_IMAGES:
				mergeStereoImageArea(mRGB, mRGB1, mRGB2, mIsOdd, mSubArea);
				break;
				}
			mDoneSignal.countDown();
			}

		/**
		 * Calculate the sub-area that this worker needs to work on.
		 * All parameters of input area and sub-area are even numbers.
		 * @param area total area for the imaging task
		 * @return fraction of the total area for this thread to work on
		 */
		private Rectangle getSubArea(Rectangle area) {
			Rectangle subArea = new Rectangle(area);
			int subHeight = area.height / mThreadCount;
			int heightRest = area.height % mThreadCount;
			subArea.y += mThreadIndex * subHeight + Math.min(mThreadIndex, heightRest);
			subArea.height = subHeight + (heightRest > mThreadIndex ? 1 : 0);
			return subArea;
			}
		}
	}

class MarkerDetail {
	public boolean[] faceVisible;
	public float[] x,y,z;
	public int[] screenx,screeny;
	public int[][] polygonx,polygony;

	public MarkerDetail() {
		faceVisible = new boolean[6];
		x = new float[8];
		y = new float[8];
		z = new float[8];
		screenx = new int[8];
		screeny = new int[8];
		polygonx = new int[6][4];
		polygony = new int[6][4];
		}
	}

class VisualizationPoint3DComparator implements Comparator<VisualizationPoint> {
	public int compare(VisualizationPoint o1, VisualizationPoint o2) {
		float z1 = ((VisualizationPoint3D)o1).zoom;
		float z2 = ((VisualizationPoint3D)o2).zoom;
		return (z1 < z2) ? -1 : (z1 == z2) ? 0 : 1;
		}
	}
