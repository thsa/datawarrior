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
import com.actelion.research.gui.*;
import com.actelion.research.table.model.*;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

public abstract class VisualizationPanel extends JPanel
				implements ComponentListener,ItemListener,CompoundTableView,FocusableView,MouseWheelListener,Printable,PruningBarListener {
	private static final long serialVersionUID = 0x20100602;

	public static final String UNASSIGNED_TEXT = "<unassigned>";

	protected JVisualization	mVisualization;
	protected int				mDimensions;
	private Frame				mParentFrame;
	private CompoundTableModel	mTableModel;
	private JPruningBar[]		mPruningBar;
	private JComboBox[]			mComboBoxColumn;
	private JWindow				mControls,mMessagePopup,mVisiblePopup;
	private int					mQualifyingColumns,mAutoZoomColumn;
	private int[]				mQualifyingColumn;
	private float               mAutoZoomFactor;
	private float[][]           mPruningBarCache;
	private boolean				mDisableEvents,mIsProgrammaticChange;
	private VisualizationPanel	mMasterPanel;
	private Point				mPopupLocation;
	private ArrayList<VisualizationPanel> mSynchronizationChildList;
	private ArrayList<VisualizationListener> mListenerList;

	private static final float MAX_AUTO_ZOOM_VALUE = 0.005f;
	private static final int ZOOM_ANIMATION_LIMIT = 50000;
	private static final int ZOOM_ANIMATION_TOTAL_MILLIS = 1000;
	private static final int ZOOM_ANIMATION_FRAME_MILLIS = 10;

	public VisualizationPanel(Frame parent, CompoundTableModel tableModel) {
		mParentFrame = parent;
		mTableModel = tableModel;
		mMasterPanel = this;
		mAutoZoomFactor = 0f;
		mAutoZoomColumn = -1;
		addMouseWheelListener(this);
		}

	@Override
	public void updateUI() {
		super.updateUI();

		if (mControls != null)
			SwingUtilities.updateComponentTreeUI(mControls);
		if (mMessagePopup != null)
			SwingUtilities.updateComponentTreeUI(mMessagePopup);
		}

	@Override
	public void cleanup() {
		removeMouseWheelListener(this);

		// make all controlled views independent
		ArrayList<VisualizationPanel> dependentChildList = new ArrayList<VisualizationPanel>();
		for (VisualizationPanel vp:mSynchronizationChildList)
			if (vp != this)
				dependentChildList.add(vp);
		for (VisualizationPanel vp:dependentChildList)
			vp.setSynchronizationMaster(null);

		for (int axis=0; axis<mDimensions; axis++)
			mPruningBar[axis].removePruningBarListener(this);

		mVisualization.cleanup();
		}

	public void addVisualizationListener(VisualizationListener l) {
		mListenerList.add(l);
		}

	public void removeVisualizationListener(VisualizationListener l) {
		mListenerList.remove(l);
		}

	@Override
	public void setViewSelectionHelper(ViewSelectionHelper l) {
		mVisualization.setViewSelectionHelper(l);
		}

	@Override
	public boolean copyViewContent() {
		return mVisualization.copyViewContent();
		}

		/**
		 * Shows or hides a popup at the top left corner containing
		 * comboboxes for column selection and pruning bars.
		 */
	public void showControls() {
		if (mVisiblePopup != null) {
			hideControls();
			return;
			}

		/* using JWindow as popup has the problem that it doesn't move with the parent frame and */
		mVisiblePopup = (mMasterPanel == this) ? mControls : mMessagePopup;
		mVisiblePopup.pack();

		mPopupLocation = getLocationOnScreen();
		mPopupLocation.translate(getWidth()-64, 0);
		mVisiblePopup.setLocation(mPopupLocation);
		mVisiblePopup.setVisible(true);
		mVisiblePopup.toFront();

		mParentFrame.addComponentListener(this);
		addComponentListener(this);
		}

	public void hideControls() {
		if (mVisiblePopup != null) {
			mVisiblePopup.setVisible(false);
			mVisiblePopup = null;
			mParentFrame.removeComponentListener(this);
			removeComponentListener(this);
			return;
			}
		}
	
	@Override
	public void componentHidden(ComponentEvent e) {}

	@Override
	public void componentMoved(ComponentEvent e) {
		mPopupLocation = getLocationOnScreen();
		mPopupLocation.translate(getWidth()-64, 0);
		mVisiblePopup.setLocation(mPopupLocation);
		}

	@Override
	public void componentResized(ComponentEvent e) {
		mPopupLocation = getLocationOnScreen();
		mPopupLocation.translate(getWidth()-64, 0);
		mVisiblePopup.setLocation(mPopupLocation);
		}

	@Override
	public void componentShown(ComponentEvent e) {}

	protected void initialize() {
		setLayout(new BorderLayout());
		add(mVisualization, BorderLayout.CENTER);

		mPruningBar = new JPruningBar[mDimensions];
		mPruningBarCache = new float[2][mDimensions];
		mComboBoxColumn = new JComboBox[mDimensions];
		for (int axis=0; axis<mDimensions; axis++) {
			mPruningBar[axis] = new JPruningBar(0.0f, 1.0f, true, axis, true);
			mPruningBar[axis].addPruningBarListener(this);
			mPruningBarCache[0][axis] = 0f;
			mPruningBarCache[1][axis] = 1f;
			}

		for (int axis=0; axis<mDimensions; axis++) {
			mComboBoxColumn[axis] = new JComboBoxWithColor();
			mComboBoxColumn[axis].setMaximumRowCount(24);
			mComboBoxColumn[axis].addItemListener(this);
			}

		double[] size2 = new double[3*mDimensions+1];
		for (int i=0; i<mDimensions; i++) {
			if (i != 0)
				size2[3*i-1]   = 4;
			size2[3*i]   = TableLayout.PREFERRED;
			size2[3*i+1] = TableLayout.PREFERRED;
			}
		double[][] size = { {TableLayout.PREFERRED}, size2 };

		mControls = new JWindow(mParentFrame);
		mControls.getContentPane().setLayout(new TableLayout(size));
		for (int axis=0; axis<mDimensions; axis++) {
			mControls.getContentPane().add(mComboBoxColumn[axis], "0,"+(3*axis));
			mControls.getContentPane().add(mPruningBar[axis], "0,"+(3*axis+1));
			}
		mMessagePopup = new JWindow(mParentFrame);
		mMessagePopup.add(new JLabel("This view is controlled by another view."));
		mVisiblePopup = null;

		mVisualization.initializeDataPoints(false, false);
		if (mTableModel.getTotalRowCount() > 100000)
			mVisualization.setFastRendering(true);

		mSynchronizationChildList = new ArrayList<VisualizationPanel>();
		mSynchronizationChildList.add(this);

		mListenerList = new ArrayList<VisualizationListener>();

		setupQualifyingColumns();
		for (int axis=0; axis<mDimensions; axis++) {
			setupColorChoice(mComboBoxColumn[axis], JVisualization.cColumnUnassigned);

			// this causes ItemEvents to be sent
			setComboBox(axis, mQualifyingColumns);
			}
		}

	/**
	 * If zooming, column-axis assignment and rotation state (3D only) of this panel's
	 * visualization is synchronized with an external master panel, then the master
	 * is returned.
	 * @return external master panel or null
	 */
	public VisualizationPanel getSynchronizationMaster() {
		return (mMasterPanel == this) ? null : mMasterPanel;
		}

	public ArrayList<VisualizationPanel> getSynchronizationChildList() {
		return mSynchronizationChildList;
		}

	/**
	 * Determines whether this VisualizationPanel serves as synchronization master
	 * for another VisualizationPanel being different from this instance.
	 * @return
	 */
	public boolean isSynchronizationMaster() {
		for (VisualizationPanel vp:mSynchronizationChildList)
			if (vp != this)
				return true;

		return false;
		}

	/**
	 * Changes the control of the panel from the current master to the given master panel.
	 * The control includes zoom & rotation (only 3D panels) state as well as column-axis
	 * assignment. A panel that is not its own master has no visible axis controls
	 * (comboboxes and pruning bars). When this method is called, it informs old and
	 * new master by calling addSynchronizedChild() and removeSynchronizedChild().
	 * @param newMaster new master or null, which is equivalent to this panel
	 */
	public void setSynchronizationMaster(VisualizationPanel newMaster) {
		if (newMaster == null)
			newMaster = this;

		if (mMasterPanel == newMaster)
			return;

		VisualizationPanel oldMaster = mMasterPanel;

		if (newMaster == this)
			synchronizeControlsWithMaster();

		oldMaster.removeSynchronizedChild(this);
		newMaster.addSynchronizedChild(this);

		if (newMaster != this) {
			if (mControls.isVisible())
				mControls.setVisible(false);
			synchronizeViewWithMaster(newMaster);
			}

		mMasterPanel = newMaster;
		}

	private void synchronizeControlsWithMaster() {
		for (int axis=0; axis<mDimensions; axis++) {
			int index = mMasterPanel.getComboBoxSelectedIndex(axis);
			if (mComboBoxColumn[axis].getSelectedIndex() != index) {
				setComboBox(axis, index);
				initializePruningBar(axis, mQualifyingColumn[index]);
				}
			mPruningBar[axis].setLowAndHigh(mMasterPanel.getPruningBar(axis).getLowValue(),
											mMasterPanel.getPruningBar(axis).getHighValue(), false);
			}
		}

	protected void synchronizeViewWithMaster(VisualizationPanel newMaster) {
		for (int axis=0; axis<mDimensions; axis++) {
			if (newMaster.getSelectedColumn(axis) != mMasterPanel.getSelectedColumn(axis))
				mVisualization.setColumnIndex(axis, newMaster.getSelectedColumn(axis));

			JPruningBar pb = newMaster.getPruningBar(axis);
			mVisualization.updateVisibleRange(axis, pb.getLowValue(), pb.getHighValue(), false);
			}		
		}

	private void addSynchronizedChild(VisualizationPanel vp) {
		mSynchronizationChildList.add(vp);
		}

	private void removeSynchronizedChild(VisualizationPanel vp) {
		mSynchronizationChildList.remove(vp);
		}

	@Override
	public int print(Graphics g, PageFormat f, int pageIndex) {
		return mVisualization.print(g, f, pageIndex);
		}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(64, 64);
		}

	/**
	 * Sets a visible range for the given axis in non-logarithmic value space.
	 * For categories (0...n) typical values are: NaN, 0.5, 1.5, ... n-1.5
	 * @param axis
	 * @param low NaN if there is no low limit
	 * @param high NaN if there is no high limit
	 */
	public void setVisibleRange(int axis, float low, float high) {
		int column = mVisualization.getColumnIndex(axis);
		if (column != -1) {
			float limit1 = 0f;
			float limit2 = 1f;
			if (mVisualization.isCategoryAxis(axis)) {
				int categoryCount = mTableModel.getCategoryCount(column);
				if (!Float.isNaN(low) && low > 0f)
					limit1 = (low >= categoryCount-1) ? 1f : (0.5f + low) / (float)categoryCount;
				if (!Float.isNaN(high) && high < categoryCount-1)
					limit2 = (high < 0f) ? 0f : (0.5f + high) / (float)categoryCount;
				if (limit1 > limit2)
					limit1 = limit2;
				}
			else {
				float[] minAndMax = mVisualization.getDataMinAndMax(axis);
				if (minAndMax[0] < minAndMax[1]) {
					if (!Float.isNaN(low) && low > minAndMax[0])
						limit1 = (low >= minAndMax[1]) ? 1f : (low - minAndMax[0]) / (minAndMax[1] - minAndMax[0]);
					if (!Float.isNaN(high) && high < minAndMax[1])
						limit2 = (high <= minAndMax[0]) ? 0f : (high - minAndMax[0]) / (minAndMax[1] - minAndMax[0]);
					if (limit1 > limit2)
						limit1 = limit2;
					}
				}
			getPruningBar(axis).setLowAndHigh(limit1, limit2, false);
			}
		}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		zoom(e.getX()-mVisualization.getX(), e.getY()-mVisualization.getY(), e.getWheelRotation());
		}

	/**
	 * Changes the current zoom state of this view and all synchronized views, such that
	 * the screen point (sx,sy) stays and its surrounding is zoomed in or out depending on the
	 * sign of scroll wheel steps.
	 * @param sx
	 * @param sy
	 * @param steps
	 */
	public abstract void zoom(int sx, int sy, int steps);

	/**
	 * Sets a new zoom state by defining low and high values for all dimensions.
	 * This effectively updates all zoom sliders and then causes an update of the view.
	 * If this view is a synchronization master or slave, then all synchronized views
	 * are effected.
	 * @param low
	 * @param high
	 * @param isAdjusting
	 */
	public void setZoom(float[] low, float[] high, boolean isAdjusting, boolean isAutoZoom) {
		if (!isAutoZoom) {
			for (int i=0; i<mDimensions; i++) {
				mPruningBarCache[0][i] = low[i];
				mPruningBarCache[1][i] = high[i];
				}
			}

		int dimensions = Math.min(low.length, mDimensions);
		for (int i=0; i<dimensions; i++)
			getActingPruningBar(i).setLowAndHigh(low[i], high[i], true);

		for (VisualizationPanel child:mMasterPanel.getSynchronizationChildList())
			for (int i=0; i<dimensions; i++)
				child.getVisualization().updateVisibleRange(i, low[i], high[i], isAdjusting);

		if (!isAdjusting)
			fireVisualizationChanged(VisualizationEvent.TYPE.AXIS);
		}

	public float getCachedPruningBarLow(int axis) {
		return mPruningBarCache[0][axis];
		}

	public float getCachedPruningBarHigh(int axis) {
		return mPruningBarCache[1][axis];
		}

	public void setCachedPruningBarLow(int axis, float low) {
		mPruningBarCache[0][axis] = low;
		}

	public void setCachedPruningBarHigh(int axis, float high) {
		mPruningBarCache[1][axis] = high;
		}

	public int getAutoZoomColumn() {
		return mAutoZoomColumn;
	}

	public float getAutoZoomFactor() {
		return mAutoZoomFactor;
	}

	public void setAutoZoom(float factor, int column, boolean zoomImmediately) {
		if (mAutoZoomFactor != factor
		 || (mAutoZoomColumn != column && factor != 0f)) {
			mAutoZoomFactor = factor;
			mAutoZoomColumn = column;
			if (zoomImmediately)
				doAutoZoom();
			}
		}

	private void doAutoZoom() {
		if (mMasterPanel != this)
			return;

		float[] low = new float[mDimensions];
		float[] high = new float[mDimensions];
		CompoundRecord ref = mTableModel.getActiveRow();
		if (ref == null || mAutoZoomFactor == 0f) {
			for (int i=0; i<mDimensions; i++) {
				low[i] = mPruningBarCache[0][i];
				high[i] = mPruningBarCache[1][i];
				}
			}
		else {
			float zoom = 1f/mAutoZoomFactor;

			float whFactor = 1f;
			if (mDimensions == 2) { // if we have a background picture, we keep width/height ratio
				BufferedImage bgi = ((JVisualization2D)mVisualization).getBackgroundImage();
				if (bgi != null && bgi.getWidth() != 0 && mVisualization.getHeight() != 0)
					whFactor = (float)Math.sqrt((double)(bgi.getHeight()*mVisualization.getWidth())
											  / (double)(bgi.getWidth()*mVisualization.getHeight()));
				}

			if (mAutoZoomColumn != -1) {
				float value = ref.getDouble(mAutoZoomColumn);
				if (!Float.isNaN(value)) {
					float average = 0.5f * (mTableModel.getMinimumValue(mAutoZoomColumn) + mTableModel.getMaximumValue(mAutoZoomColumn));
					zoom *= Math.abs(value / average);
					}
				}
			if (zoom > 1f)
				zoom = 1f;
			else if (zoom < MAX_AUTO_ZOOM_VALUE)
				zoom = MAX_AUTO_ZOOM_VALUE;

			for (int i=0; i<mDimensions; i++) {
				float value = mVisualization.getPruningBarMappedValue(ref, i);
				if (Float.isNaN(value)) {
					low[i] = 0f;
					high[i] = 1f;
					}
				else {
					float factor = 0.5f;
					if (whFactor != 1f) {
						if (i == 0)
							factor *= whFactor;
						else
							factor /= whFactor;
						}
					low[i] = Math.max(0f, value-zoom*factor);
					high[i] = Math.min(1f, value+zoom*factor);
					}
				}
			}

		animateAutoZoom(low, high);
		}

	private void animateAutoZoom(float[] endLow, float[] endHigh) {
		float maxInc = 0f;
		float[] lowInc = new float[mDimensions];
		float[] highInc = new float[mDimensions];
		for (int i=0; i<mDimensions; i++) {
			lowInc[i] = endLow[i] - mPruningBar[i].getLowValue();
			highInc[i] = endHigh[i] - mPruningBar[i].getHighValue();
			maxInc = Math.max(maxInc, Math.abs(lowInc[i]));
			maxInc = Math.max(maxInc, Math.abs(highInc[i]));
			}
		if (mTableModel.getRowCount() > ZOOM_ANIMATION_LIMIT) {
			setZoom(endLow, endHigh, false, true);
			}
		else {
			new Thread(() -> {
				float[] frameLow = new float[mDimensions];
				float[] frameHigh = new float[mDimensions];
				long currentMillis = System.currentTimeMillis();
				long startMillis = currentMillis - ZOOM_ANIMATION_FRAME_MILLIS;    // virtual start one frame ago
				while (currentMillis - startMillis < ZOOM_ANIMATION_TOTAL_MILLIS) {
					float factor = (float)(ZOOM_ANIMATION_TOTAL_MILLIS - currentMillis + startMillis) / ZOOM_ANIMATION_TOTAL_MILLIS;
					for (int i=0; i<mDimensions; i++) {
						frameLow[i] = endLow[i] - factor * lowInc[i];
						frameHigh[i] = endHigh[i] - factor * highInc[i];
						}
					SwingUtilities.invokeLater(() -> setZoom(frameLow, frameHigh, true, true) );
					try { Thread.sleep(ZOOM_ANIMATION_FRAME_MILLIS); } catch (Exception e) {}
					currentMillis = System.currentTimeMillis();
					}
				SwingUtilities.invokeLater(() -> setZoom(endLow, endHigh, false, true));
				} ).start();
			}
		}

	@Override
	public void pruningBarChanged(PruningBarEvent e) {
		if (e.getType() == PruningBarEvent.TYPE_TYPED) {
			// we take coordinate translation info from first child and update pruning bar, which causes then a new event
			for (VisualizationPanel child:getSynchronizationChildList()) {
				float low = e.getLowValue();
				float high = e.getHighValue();
				float[] range = new float[2];
				boolean isLog = child.getVisualization().getFullDataRange(e.getID(), range);
				float val = Float.isNaN(low) ? high : low;
				if (isLog)
					val = (float)Math.log10(val);
				float pos = (val - range[0]) / (range[1] - range[0]);
				if (Float.isNaN(low))
					mPruningBar[e.getID()].setHighValue(pos);
				else
					mPruningBar[e.getID()].setLowValue(pos);
				return;
				}
			}

		mPruningBarCache[0][e.getID()] = mPruningBar[e.getID()].getLowValue();
		mPruningBarCache[1][e.getID()] = mPruningBar[e.getID()].getHighValue();

		for (VisualizationPanel child:getSynchronizationChildList())
			child.getVisualization().updateVisibleRange(e.getID(), e.getLowValue(), e.getHighValue(), e.isAdjusting());

		if (!e.isAdjusting())
			fireVisualizationChanged(VisualizationEvent.TYPE.AXIS);
		}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED && !mDisableEvents) {
			for (int axis=0; axis<mDimensions; axis++) {
				if (e.getSource() == mComboBoxColumn[axis]) {
					int index = mComboBoxColumn[axis].getSelectedIndex();
					if (index != -1) {
						for (VisualizationPanel child:getSynchronizationChildList())
							if (axis < child.getDimensionCount())
								child.setColumnIndex(axis, mQualifyingColumn[index]);

						fireVisualizationChanged(VisualizationEvent.TYPE.AXIS);
						}
					}
				}
			}
		}

	protected void fireVisualizationChanged(VisualizationEvent.TYPE type) {
		if (!mIsProgrammaticChange)
			for (VisualizationListener vl:mListenerList)
				vl.visualizationChanged(new VisualizationEvent(this, type));
		}

	private void setColumnIndex(int axis, int column) {
		mVisualization.setColumnIndex(axis, column);
		initializePruningBar(axis, column);
		}

	/**
	 * Gets the column index that is currently selected by the combobox.
	 * This may be different from the displayed column if this panel is
	 * synchronized to another VisualizationPanel.
	 * @param axis
	 * @return
	 */
	public int getSelectedColumn(int axis) {
		return mQualifyingColumn[mComboBoxColumn[axis].getSelectedIndex()];
		}

	public int getQualifyingColumn(int index) {
		return mQualifyingColumn[index];
		}

	private int getComboBoxSelectedIndex(int i) {
		return mComboBoxColumn[i].getSelectedIndex();
		}

	public int getFocusList() {
		return mVisualization.getFocusList();
		}

	public void setFocusList(int no) {
		mVisualization.setFocusList(no);
		}

	@Override
	public void compoundTableChanged(CompoundTableEvent e) {
		// don't make mVisualization a direct listener because in case of
		// a table structure change the pruning panel needs to be updated first
		if (e.getType() == CompoundTableEvent.cAddRows
		 || e.getType() == CompoundTableEvent.cDeleteRows
		 || e.getType() == CompoundTableEvent.cChangeColumnData) {
			int[] selected = new int[mDimensions];
			for (int axis=0; axis<mDimensions; axis++)
				selected[axis] = mQualifyingColumn[mComboBoxColumn[axis].getSelectedIndex()];
			setupQualifyingColumns();
			for (int axis=0; axis<mDimensions; axis++) {
				setupColorChoice(mComboBoxColumn[axis], selected[axis]);
				int column = mVisualization.getColumnIndex(axis);
				if (column != JVisualization.cColumnUnassigned
				 && (e.getType() != CompoundTableEvent.cChangeColumnData
				  || column == e.getColumn())) {
					boolean found = false;
					for (int j = 0; j<=mQualifyingColumns; j++) {
						if (selected[axis] == mQualifyingColumn[j]) {
							mDisableEvents = true;
							setComboBox(axis, j);
							mDisableEvents = false;
							float newMin = (mPruningBar[axis].getLowValue() == 0.0) ? 0f : mVisualization.getPruningBarLow(axis);
							float newMax = (mPruningBar[axis].getHighValue() == 1.0) ? 1f : mVisualization.getPruningBarHigh(axis);

							// silently update sliders in case of logMode change
							mPruningBar[axis].setLowAndHigh(newMin, newMax, true);
							mPruningBar[axis].setUseRedColor(column != JVisualization.cColumnUnassigned
									&& !mTableModel.isColumnDataComplete(column)
									&& mTableModel.isColumnTypeDouble(column));

							mVisualization.updateVisibleRange(axis, newMin, newMax, false);

							found = true;
							break;
							}
						}
					if (!found) {
						mDisableEvents = true;
						setComboBox(axis, mQualifyingColumns);
						mDisableEvents = false;
						for (VisualizationPanel vp:mSynchronizationChildList)
							if (axis < vp.getDimensionCount())
								vp.getVisualization().setColumnIndex(axis, JVisualization.cColumnUnassigned);
						initializePruningBar(axis, JVisualization.cColumnUnassigned);
						}
					}
				}
			}
		else if (e.getType() == CompoundTableEvent.cAddColumns) {
			int[] selected = new int[mDimensions];
			for (int axis=0; axis<mDimensions; axis++)
				selected[axis] = mQualifyingColumn[mComboBoxColumn[axis].getSelectedIndex()];
			setupQualifyingColumns();
			for (int axis=0; axis<mDimensions; axis++)
				setupColorChoice(mComboBoxColumn[axis], selected[axis]);
			}
		else if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			int[] columnMapping = e.getMapping();
			int[] selected = new int[mDimensions];
			for (int axis=0; axis<mDimensions; axis++)
				selected[axis] = mQualifyingColumn[mComboBoxColumn[axis].getSelectedIndex()];
			setupQualifyingColumns();
			for (int axis=0; axis<mDimensions; axis++) {
				int newSelected = (selected[axis] == JVisualization.cColumnUnassigned) ?
						JVisualization.cColumnUnassigned : columnMapping[selected[axis]];
				setupColorChoice(mComboBoxColumn[axis], newSelected);
				if (selected[axis] != JVisualization.cColumnUnassigned
				 && newSelected == JVisualization.cColumnUnassigned) {
					initializePruningBar(axis, JVisualization.cColumnUnassigned);
					}
				}
			if (mAutoZoomColumn != JVisualization.cColumnUnassigned) {
				mAutoZoomColumn = columnMapping[mAutoZoomColumn];
				}
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnName) {
			int column = e.getColumn();
			for (int i = 0; i<mQualifyingColumns; i++) {
				if (mQualifyingColumn[i] == column) {
					for (int axis=0; axis<mDimensions; axis++) {
						((ComboBoxColorItem)mComboBoxColumn[axis].getItemAt(i)).setText(mVisualization.getAxisTitle(column));
						mComboBoxColumn[axis].validate();
						mComboBoxColumn[axis].repaint();
						}
					break;
					}
				}
			}
		else if (e.getType() == CompoundTableEvent.cChangeActiveRow) {
			if (mAutoZoomFactor != 0)
				doAutoZoom();
			}

		for (VisualizationPanel vp:mSynchronizationChildList)
			vp.getVisualization().compoundTableChanged(e);
		}

	@Override
	public void listChanged(CompoundTableListEvent e) {
		mVisualization.listChanged(e);
		}

	@Override
	public CompoundTableModel getTableModel() {
		return mTableModel;
		}

	public JVisualization getVisualization() {
		return mVisualization;
		}

	public int getDimensionCount() {
		return mDimensions;
		}

	/**
	 * Returns this panel's pruning bar, which may currently not be used if
	 * the panel is synchronized to another master panel.
	 * If this is not what you want, check getActingPruningBar(int axis).
	 * @param axis
	 * @return
	 */
	public JPruningBar getPruningBar(int axis) {
		return mPruningBar[axis];
		}

	/**
	 * Returns the pruning bar that is currently controlling the axis of this panel.
	 * It may belong to another panel, if this panel is synchronized with another master panel.
	 * @param axis
	 * @return
	 */
	public JPruningBar getActingPruningBar(int axis) {
		return mMasterPanel.mPruningBar[axis];
		}

	public String getAxisColumnName(int axis) {
		int index = mComboBoxColumn[axis].getSelectedIndex();
		if (index == mQualifyingColumns)
			return UNASSIGNED_TEXT;

		return mTableModel.getColumnTitleNoAlias(mQualifyingColumn[index]);
		}

	/**
	 * Assigns a column to a given axis by specifying the column name.
	 * If there is no column with this name then nothing happens.
	 * Effectively, this updates the column selecting combo box of this
	 * visualization, which in turn updates all synchronized child views.
	 * If this view is child view of another, then this method has no effect
	 * until the synchronization is broken.
	 * @param axis
	 * @param name
	 * @return true if the column assigned to this axis was changed
	 */
	public boolean setAxisColumnName(int axis, String name) {
		int index = mQualifyingColumns;		// default: unassigned
		if (!name.equals(UNASSIGNED_TEXT)) {
			int column = mTableModel.findColumn(name);
			if (column != -1) {
				for (int i = 0; i<mQualifyingColumns; i++) {
					if (column == mQualifyingColumn[i]) {
						index = i;
						break;
						}
					}
				}
			}

		if (index != mComboBoxColumn[axis].getSelectedIndex()) {
			setComboBox(axis, index);
			return true;
			}

		return false;
		}

	/**
	 * Changes the index of a combobox without sending VisualizationEvents
	 * @param axis
	 * @param index
	 */
	private void setComboBox(final int axis, final int index) {
		mIsProgrammaticChange = true;
		if (SwingUtilities.isEventDispatchThread()) {
			mComboBoxColumn[axis].setSelectedIndex(index);
			}
		else {
			try {
				SwingUtilities.invokeAndWait(() -> mComboBoxColumn[axis].setSelectedIndex(index) );
				}
			catch (Exception e) {}
			}
		mIsProgrammaticChange = false;
		}

	public void resetAllFilters() {
		for (int axis=0; axis<mDimensions; axis++) {
			int column = mQualifyingColumn[mComboBoxColumn[axis].getSelectedIndex()];
			if (column != JVisualization.cColumnUnassigned) {
				if (!mTableModel.isColumnDataComplete(column)) {
					setComboBox(axis, mQualifyingColumns);
					initializePruningBar(axis, JVisualization.cColumnUnassigned);
					}
				else {
					mPruningBar[axis].reset();
					}
				}
			}
		}

	public void setDefaultColumns() {
		int count = 0;
		for (int i = 0; i<mQualifyingColumns; i++)
			if (mTableModel.isColumnTypeDouble(mQualifyingColumn[i]))
				count++;
		NumericalCompoundTableColumn[] numericalColumn = new NumericalCompoundTableColumn[count];
		count = 0;
		for (int i = 0; i<mQualifyingColumns; i++)
			if (mTableModel.isColumnTypeDouble(mQualifyingColumn[i]))
				numericalColumn[count++] = new NumericalCompoundTableColumn(mTableModel ,mQualifyingColumn[i]);

		int[] index = new int[mDimensions];
		for (int axis=0; axis<mDimensions; axis++)
			index[axis] = -1;

		if (numericalColumn.length >= 2) {
			double[][] correlation = new CorrelationCalculator().calculateMatrix(numericalColumn, CorrelationCalculator.TYPE_BRAVAIS_PEARSON);
	
			double maxCorrelation = 0;
			for (int i=1; i<correlation.length; i++) {
				for (int j=0; j<correlation[i].length; j++) {
					if (!Double.isNaN(correlation[i][j])) {
						if (maxCorrelation < Math.abs(correlation[i][j])) {
							maxCorrelation = Math.abs(correlation[i][j]);
							index[0] = j;
							index[1] = i;
							}
						}
					}
				}

			if (mDimensions == 3 && index[0] != -1) {
				maxCorrelation = 0;
				for (int i=0; i<correlation.length; i++) {
					if (i != index[0] && i != index[1]) {
						for (int j=0; j<2; j++) {
							double c = (i < index[j]) ? correlation[index[j]][i] : correlation[i][index[j]];
							if (!Double.isNaN(c)) {
								if (maxCorrelation < Math.abs(c)) {
									maxCorrelation = Math.abs(c);
									index[2] = i;
									}
								}
							}
						}
					}
				}
			}

		int nonCorrelationIndex = 0;
		boolean[] inUse = new boolean[mQualifyingColumns];
		for (int axis=0; axis<mDimensions; axis++) {
			if (index[axis] != -1) {
				for (int j=0; j<mQualifyingColumns; j++) {
					if (numericalColumn[index[axis]].getColumn() == mQualifyingColumn[j]) {
						setComboBox(axis, j);
						inUse[j] = true;
						break;
						}
					}
				}
			else {
				while (nonCorrelationIndex<mQualifyingColumns
					&& (inUse[nonCorrelationIndex]
					 || !mTableModel.isColumnDataComplete(mQualifyingColumn[nonCorrelationIndex])
					 || !mTableModel.hasNumericalVariance(mQualifyingColumn[nonCorrelationIndex])))
					nonCorrelationIndex++;

				setComboBox(axis, (nonCorrelationIndex<mQualifyingColumns) ? nonCorrelationIndex : mQualifyingColumns);
				}
			}
		}

	private void setupQualifyingColumns() {
		Comparator<Integer> comparator = new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				String title1 = mTableModel.getColumnTitle(o1);
				String title2 = mTableModel.getColumnTitle(o2);
				return title1.toLowerCase(Locale.ROOT).compareTo(title2.toLowerCase(Locale.ROOT));
				}
			};

		ArrayList<Integer> columnList = new ArrayList<>();
		for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
			if (mTableModel.hasNumericalVariance(i) || mTableModel.isDescriptorColumn(i))
				columnList.add(i);

		mQualifyingColumn = new int[mTableModel.getTotalColumnCount()+1];

		mQualifyingColumns = 0;
		Integer[] columns = columnList.toArray(new Integer[0]);
		Arrays.sort(columns, comparator);
		for (Integer column:columns)
			mQualifyingColumn[mQualifyingColumns++] = column;

/*		mQualifyingColumn = new int[mTableModel.getTotalColumnCount()+1];
		mFirstChoiceColumns = 0;
		for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
			if (mTableModel.hasNumericalVariance(i)
			 && (mTableModel.isColumnDataComplete(i) || !mTableModel.isColumnTypeDouble(i)))
				mQualifyingColumn[mFirstChoiceColumns++] = i;
		mQualifyingColumns = mFirstChoiceColumns;
		for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
			if (mTableModel.hasNumericalVariance(i)
			 && (!mTableModel.isColumnDataComplete(i) && mTableModel.isColumnTypeDouble(i)))
				mQualifyingColumn[mQualifyingColumns++] = i;
		for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
			if (mTableModel.isDescriptorColumn(i))
				mQualifyingColumn[mQualifyingColumns++] = i;
		mQualifyingColumn[mQualifyingColumns] = JVisualization.cColumnUnassigned;
		*/
		}

	/**
	 * Populates the combobox with all entries of mQualifyingColumn
	 * without sending any events.
	 * @param choice
	 * @param column
	 */
	private void setupColorChoice(JComboBox choice, int column) {
		mDisableEvents = true;
		mIsProgrammaticChange = true;

		Color defaultColor = UIManager.getColor("ComboBox.foreground");
		choice.removeAllItems();
		for (int j=0; j<mQualifyingColumns; j++) {
			Color color = (mTableModel.isColumnTypeDouble(mQualifyingColumn[j])
						|| mTableModel.isDescriptorColumn(mQualifyingColumn[j]))
					   && !mTableModel.isColumnDataComplete(mQualifyingColumn[j]) ? Color.RED
						: mTableModel.isColumnTypeCategory(mQualifyingColumn[j]) ?
					(LookAndFeelHelper.isDarkLookAndFeel() ? Color.CYAN : Color.BLUE) : defaultColor;
			choice.addItem(new ComboBoxColorItem(mVisualization.getAxisTitle(mQualifyingColumn[j]), color));
			}
		choice.addItem(new ComboBoxColorItem(UNASSIGNED_TEXT, defaultColor));

		for (int j=0; j<=mQualifyingColumns; j++) {
			if (column == mQualifyingColumn[j]) {
				choice.setSelectedIndex(j);
				break;
				}
			}

		mIsProgrammaticChange = false;
		mDisableEvents = false;
		}

	/**
	 * Maximizes pruning bar's setting to cover the full range.
	 * Updates the pruning bar color to reflect (in-)completeness of data.
	 * Does not send any events.
	 * @param axis
	 * @param column
	 */
	private void initializePruningBar(int axis, int column) {
		mPruningBar[axis].setMinAndMax(0.0f, 1.0f);
		mPruningBarCache[0][axis] = 0f;
		mPruningBarCache[1][axis] = 1f;
		if (column == JVisualization.cColumnUnassigned) {
			mPruningBar[axis].setUseRedColor(false);
			}
		else if (mTableModel.isDescriptorColumn(column)) {
			mPruningBar[axis].setUseRedColor(mTableModel.isColumnDataComplete(column));
			}
		else {
			mPruningBar[axis].setUseRedColor(!mTableModel.isColumnDataComplete(column)
										  && mTableModel.isColumnTypeDouble(column));
			}
		}
	}
