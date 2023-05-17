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

package com.actelion.research.table.filter;

import com.actelion.research.gui.JProgressPanel;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.gui.hidpi.HiDPIIconButton;
import com.actelion.research.gui.hidpi.HiDPIToggleButton;
import com.actelion.research.gui.swing.SwingCursorHelper;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableListener;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class JFilterPanel extends JPanel
		implements ActionListener,CompoundTableListener,DragGestureListener,DragSourceListener {
	private static final long serialVersionUID = 0x20110325;

	public static final int FILTER_TYPE_TEXT = 0;
	public static final int FILTER_TYPE_DOUBLE = 1;
	public static final int FILTER_TYPE_CATEGORY = 2;
	public static final int FILTER_TYPE_STRUCTURE = 3;
	public static final int FILTER_TYPE_SSS_LIST = 4;
	public static final int FILTER_TYPE_SIM_LIST = 5;
	public static final int FILTER_TYPE_REACTION = 6;
	public static final int FILTER_TYPE_RETRON = 7;
	public static final int FILTER_TYPE_ROWLIST = 8;
	public static final int FILTER_TYPE_CATEGORY_BROWSER = 9;

	public static final int PSEUDO_COLUMN_ROW_LIST = -4;
	public static final int PSEUDO_COLUMN_ALL_COLUMNS = -5;    // currently only used by JTextFilterPanel

	public static final String ALL_COLUMN_TEXT = "<All Columns>";
	public static final String ALL_COLUMN_CODE = "#allColumns#";

	private static final String INVERSE_CODE = "#inverse#";
	private static final String DISABLED_CODE = "#disabled#";

	private static final int ALLOWED_DRAG_ACTIONS = DnDConstants.ACTION_COPY_OR_MOVE;
	private static final int MINIMUM_WIDTH = HiDPIHelper.scale(120);

	protected CompoundTableModel mTableModel;
	protected int mColumnIndex, mExclusionFlag;
	protected boolean mIsUserChange;    // Is set from derived classes once the UI is complete
										// and is temporarily disabled, whenever a programmatic change occurs.
	private JLabel mColumnNameLabel;
	private JPanel mTitlePanel;
	private HiDPIIconButton mAnimationButton;
	private HiDPIToggleButton mButtonInverse,mButtonDisabled;
	private JPopupMenu mAnimationPopup;
	private boolean mIsActive,mIsInverse,mSuppressErrorMessages,mIsMouseDown;
	private Animator mAnimator;
	private JDialog mOptionsDialog;
	private JProgressPanel mProgressPanel;
	private ArrayList<FilterListener> mListenerList;

	/**
	 * @param tableModel
	 * @param column
	 * @param exclusionFlag        if -1 then this filter is dead, i.e. it doesn't influence row visibility
	 * @param showAnimationOptions
	 */
	public JFilterPanel(CompoundTableModel tableModel, int column, int exclusionFlag, boolean showAnimationOptions, boolean useProgressBar) {
		mExclusionFlag = exclusionFlag;
		mIsActive = (exclusionFlag != -1);
		mTableModel = tableModel;
		mColumnIndex = column;

		setOpaque(false);
		setLayout(new BorderLayout());

		mTitlePanel = new JPanel();
		int gap = HiDPIHelper.scale(4);
		double[][] size = {{gap, TableLayout.FILL, gap, TableLayout.PREFERRED, TableLayout.PREFERRED, gap, TableLayout.PREFERRED},
							{TableLayout.PREFERRED, gap/2}};
		mTitlePanel.setLayout(new TableLayout(size));
		mTitlePanel.setOpaque(false);
		mColumnNameLabel = new JLabel(getTitle()) {
			private static final long serialVersionUID = 0x20080128;

			// Together with the TableLayout.FILL above, this allows both, abreviated and long full text labels!
			@Override public Dimension getPreferredSize() {
				Dimension size = super.getPreferredSize();
				size.width = Math.min(size.width, HiDPIHelper.scale(50));
				return size;
				}
			};

		mTitlePanel.add(mColumnNameLabel, "1,0");
		if (useProgressBar) {
			mProgressPanel = new JProgressPanel(false);
			mProgressPanel.setVisible(false);
			mTitlePanel.add(mProgressPanel, "1,0");
			}

		JPanel lbp = new JPanel();
		if (showAnimationOptions)
			showAnimationControls(lbp);
		addImageButtons(lbp);
		mButtonInverse = new HiDPIToggleButton("yy16.png", "iyy16.png", "Invert filter", "inverse");
		mButtonInverse.addActionListener(this);
		lbp.add(mButtonInverse);
		mButtonDisabled = new HiDPIToggleButton("disabled16.png", "idisabled16.png", "Disable filter", "disable");
		mButtonDisabled.addActionListener(this);
		lbp.add(mButtonDisabled);
		mTitlePanel.add(lbp, "4,0");

		if (isActive()) {
			JPanel rbp = new JPanel();
			rbp.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 5));  // 5 is the default vgap matching that of lbp
			JButton cb = new HiDPIIconButton("closeButton.png", null, "close", 0, "square");
			cb.addActionListener(this);
			rbp.add(cb);
			mTitlePanel.add(rbp, "6,0");
			}

		mColumnNameLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				mColumnNameLabel.setCursor(SwingCursorHelper.getCursor(SwingCursorHelper.cHandCursor));
				mIsMouseDown = false;
			}

			@Override
			public void mousePressed(MouseEvent e) {
				mColumnNameLabel.setCursor(SwingCursorHelper.getCursor(e.getButton() == MouseEvent.BUTTON1 ? SwingCursorHelper.cFistCursor : SwingCursorHelper.cPointerCursor));
				getParent().repaint();
				mIsMouseDown = true;
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				mColumnNameLabel.setCursor(SwingCursorHelper.getCursor(SwingCursorHelper.cHandCursor));
				getParent().repaint();
				mIsMouseDown = false;
			}

			@Override
			public void mouseExited(MouseEvent e) {
				if (!mIsMouseDown)
					mColumnNameLabel.setCursor(SwingCursorHelper.getCursor(SwingCursorHelper.cPointerCursor));
				mIsMouseDown = false;
				getParent().repaint();
			}
		});

		initializeDrag(ALLOWED_DRAG_ACTIONS);

		add(mTitlePanel, BorderLayout.NORTH);

		if (showAnimationOptions)
			mAnimator = new Animator(getDefaultFrameMillis());
		}

	protected void addPanel(JPanel panel) {
		add(panel, BorderLayout.CENTER);
		}

	@Override public Dimension getPreferredSize() {
		return new Dimension(MINIMUM_WIDTH, super.getPreferredSize().height);
		}

	@Override public Dimension getMinimumSize() {
		return new Dimension(MINIMUM_WIDTH, super.getPreferredSize().height);
	}

//	@Override public Dimension getMaximumSize() {
//		return new Dimension(mPreferredWidth, super.getPreferredSize().height);
//	}

//	public void setPreferredWidth(int width) {
//		mPreferredWidth = width;
//		}

	public boolean isPotentiallyDragged() {
		return mIsMouseDown;
		}

	/**
	 * Override to add filter specific image buttons
	 * @param panel
	 */
	public void addImageButtons(JPanel panel) {}

	private void initializeDrag(int dragAction) {
		if (dragAction != DnDConstants.ACTION_NONE)
			DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(mColumnNameLabel, dragAction, this);
		}

	@Override
	public void dragGestureRecognized(DragGestureEvent dge) {
		if ((dge.getDragAction() & ALLOWED_DRAG_ACTIONS) != 0) {
			Transferable transferable = getTransferable();
			if (transferable != null) {
				try {
					dge.startDrag(SwingCursorHelper.getCursor(SwingCursorHelper.cFistCursor), transferable, this);
					}
				catch (InvalidDnDOperationException idoe) {
					idoe.printStackTrace();
					}
				}
			}
		}

	public Transferable getTransferable() {
		return new FilterTransferable(this);
		}

	@Override
	public void dragEnter(DragSourceDragEvent dsde) {
		updateDragCursor(dsde);
		}

	@Override
	public void dragOver(DragSourceDragEvent dsde) {
		updateDragCursor(dsde);
		}

	@Override
	public void dropActionChanged(DragSourceDragEvent dsde) {
		updateDragCursor(dsde);
		}

	@Override
	public void dragExit(DragSourceEvent dse) {}

	@Override
	public void dragDropEnd(DragSourceDropEvent dsde) {
		mIsMouseDown = false;
		}

	private void updateDragCursor(DragSourceDragEvent dsde) {
		DragSourceContext context = dsde.getDragSourceContext();
		int dropAction = dsde.getDropAction();
		if ((dropAction & DnDConstants.ACTION_COPY) != 0) {
			context.setCursor(DragSource.DefaultCopyDrop);
			}
		else if ((dropAction & DnDConstants.ACTION_MOVE) != 0) {
			context.setCursor(DragSource.DefaultMoveDrop);
			}
		else {
			context.setCursor(DragSource.DefaultMoveNoDrop);
			}
		}

	public String getTitle() {
		return (mColumnIndex == PSEUDO_COLUMN_ROW_LIST) ? "List membership"
			 : (mColumnIndex == PSEUDO_COLUMN_ALL_COLUMNS) ? ALL_COLUMN_TEXT
			 : (mColumnIndex >= 0) ? mTableModel.getColumnTitle(mColumnIndex) : "";
		}

	public void addFilterListener(FilterListener l) {
		if (mListenerList == null)
			mListenerList = new ArrayList<FilterListener>();
		mListenerList.add(l);
		}

	public void removeFilterListener(FilterListener l) {
		if (mListenerList != null)
			mListenerList.remove(l);
		if (mListenerList.size() == 0)
			mListenerList = null;
		}

	public CompoundTableModel getTableModel() {
		return mTableModel;
		}

	/**
	 * Informs all listeners of a user induced filter change.
	 * Events are sent only, if mIsUserChange is true.
	 * To make this possible, any derived class
	 * is responsible to set mIsUserChange to true after the construction.
	 * Any derived class must also temporarily set mIsUserChange to false,
	 * if something that was not caused by a direct user interaction
	 * results in an updateExclusion() call or any direct tableModel call,
	 * which changes row exclusion.
	 * @param type
	 * @param isAdjusting
	 */
	public void fireFilterChanged(int type, boolean isAdjusting) {
		if (mListenerList != null && mIsUserChange)
			for (FilterListener l:mListenerList)
				l.filterChanged(new FilterEvent(this, type, isAdjusting));
		}

	/**
	 * Determines whether the filter is actively contributing to row visibility.
	 * Filters that are just used to configure a macro task or already removed filters are inactive.
	 * @return whether the filter is active
	 */
	public boolean isActive() {
		return mIsActive;
		}

	/**
	 * This is called to check whether an inversion can be performed.
	 * For disabled filters the state of the inversion checkbox is irrelevant
	 * and interactively toggling its state should not have an effect.
	 * @return true if filter is enabled and can be interactively inverted
	 */
//	public abstract boolean isFilterEnabled();

	/**
	 * If a CompoundTableEvent informs about a change that needs to update the filter settings,
	 * then this update should be delayed to not interfere with the completion of the
	 * original change through all listeners. Use this method to do so...
	 * This is should only be called, if the update request is indirect (not direct user change)
	 */
	public void updateExclusionLater() {
		SwingUtilities.invokeLater(() -> updateExclusion(false));
		}

	/**
	 * This causes the derived filter to update the exclusion with all settings on the tableModel.
	 * @param isUserChange whether the update request is caused by a direct user change of a filter
	 */
	public abstract void updateExclusion(boolean isUserChange);

	/**
	 * Enables or disables all components of the derived filter panel
	 * @param enabled
	 */
	public abstract void enableItems(boolean enabled);

	/**
	 * Override this if the filter cannot be enabled under certain circumstances.
	 * If the filter cannot be enabled, display a message, why.
	 * @return
	 */
	public boolean canEnable(boolean allowShowMessage) {
		return true;
		}

	/**
	 * This must be overwritten by filter panels which are not
	 * directly associated with a not changing column index.
	 */
	@Override
	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			mColumnIndex = e.getMapping()[mColumnIndex];
			if (mColumnIndex == -1)
				removePanel();
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnName) {
			if (mColumnIndex == e.getColumn())
				mColumnNameLabel.setText(mTableModel.getColumnTitle(mColumnIndex));
			}
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("close")) {
			fireFilterChanged(FilterEvent.FILTER_CLOSED, false);
			removePanel();
			return;
			}
		if (e.getActionCommand().equals("options")) {
			showOptionDialog(createAnimationOptionPanel());
			return;
			}
		if (e.getActionCommand().equals("optionsCancel")) {
			mOptionsDialog.setVisible(false);
			mOptionsDialog.dispose();
			return;
			}
		if (e.getActionCommand().equals("optionsOK")) {
			try {
				applyAnimationOptions();
				mOptionsDialog.setVisible(false);
				mOptionsDialog.dispose();
				fireFilterChanged(FilterEvent.FILTER_ANIMATION_CHANGED, false);
				}
			catch (NumberFormatException nfe) {
				}
			return;
			}
		if (e.getActionCommand().equals("start")) {
			startAnimation();
			fireFilterChanged(FilterEvent.FILTER_ANIMATION_STARTED, false);
			return;
			}
		if (e.getActionCommand().equals("stop")) {
			stopAnimation();
			fireFilterChanged(FilterEvent.FILTER_ANIMATION_STOPPED, false);
			return;
			}
		if (e.getSource() == mButtonInverse) {
			setInverse(mButtonInverse.isSelected());
			fireFilterChanged(FilterEvent.FILTER_UPDATED, false);
			return;
			}
		if (e.getSource() == mButtonDisabled) {
			boolean enable = !mButtonDisabled.isSelected();
			setEnabled(enable);
			if (enable && !isEnabled())	// if we could not enable
				mButtonDisabled.setSelected(true);
			else
				fireFilterChanged(FilterEvent.FILTER_UPDATED, false);
			return;
			}
		}

/*	protected void updateExclusionWithProgressBar(final Thread updateExclusionThread, final AtomicInteger concurrentIndex, final int maxIndexValue, String progressText) {
		mColumnNameLabel.setVisible(false);
		mProgressPanel.setVisible(true);
		mProgressPanel.startProgress(progressText, 0, maxIndexValue);
		updateExclusionThread.start();
		new Thread(() -> {
			int index = concurrentIndex.get();
			while (index >= 0) {
				try {
					Thread.sleep(100);
					index = concurrentIndex.get();
					mProgressPanel.updateProgress(maxIndexValue - index);
					}
				catch (InterruptedException ie) {
					index = -1;
					}
				}
			SwingUtilities.invokeLater(() -> {
				mProgressPanel.setVisible(false);
				mColumnNameLabel.setVisible(true);
				});
			}).start();
		}*/

	protected void showProgressBar(final AtomicInteger concurrentIndex, final int maxIndexValue, String progressText) {
		int threadCount = Runtime.getRuntime().availableProcessors();
		mColumnNameLabel.setVisible(false);
		mProgressPanel.setVisible(true);
		mProgressPanel.startProgress(progressText, 0, maxIndexValue);
		new Thread(() -> {
			int index;
			do {
				try {
					Thread.sleep(100);
					index = Math.min(maxIndexValue, Math.max(0, concurrentIndex.get() + threadCount));
					mProgressPanel.updateProgress(maxIndexValue - index);
					}
				catch (InterruptedException ie) {
					index = 0;
					}
				} while (index > 0);
			}).start();
		}

	protected void hideProgressBar() {
		SwingUtilities.invokeLater(() -> {
			mProgressPanel.setVisible(false);
			mColumnNameLabel.setVisible(true);
			});
		}

	private void showOptionDialog(JPanel content) {
		Component frame = this;
		while (!(frame instanceof Frame))
			frame = frame.getParent();
		
		JPanel ibp = new JPanel();
		ibp.setLayout(new GridLayout(1, 2, 8, 0));
		JButton bcancel = new JButton("Cancel");
		bcancel.setActionCommand("optionsCancel");
		bcancel.addActionListener(this);
		ibp.add(bcancel);
		JButton bok = new JButton("OK");
		bok.setActionCommand("optionsOK");
		bok.addActionListener(this);
		ibp.add(bok);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BorderLayout());
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		buttonPanel.add(ibp, BorderLayout.EAST);

		mOptionsDialog = new JDialog((Frame)frame, "Set Animation Options", true);
		mOptionsDialog.getContentPane().setLayout(new BorderLayout());
		mOptionsDialog.getContentPane().add(content, BorderLayout.CENTER);
		mOptionsDialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		mOptionsDialog.getRootPane().setDefaultButton(bok);
		mOptionsDialog.pack();
		mOptionsDialog.setLocationRelativeTo(frame);
		mOptionsDialog.setVisible(true);
		}

	/**
	 * Override the following methods, if your filter supports animations
	 * @return
	 */
	protected JPanel createAnimationOptionPanel() {
		return null;
		}

	public boolean isInverse() {
		return mIsInverse;
		}

	public void removePanel() {
		if (mAnimator != null)
			resetAnimation();

		if (mExclusionFlag != -1)
			mTableModel.freeRowFlag(mExclusionFlag);
		mExclusionFlag = -1;
		Container theParent = getParent();
		theParent.remove(this);
		theParent.getParent().validate();
		theParent.getParent().repaint();
		}

	public int getColumnIndex() {
		return mColumnIndex;
		}

	public void setColumnIndex(int index) {
		mColumnIndex = index;
		}

	protected void setText(String text, Color color) {
		if (color != null)
			mColumnNameLabel.setForeground(color);
		// Change font to allow displaying rare unicode characters, if necessary
		if (mColumnNameLabel.getFont().canDisplayUpTo(text) != -1)
			mColumnNameLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, mColumnNameLabel.getFont().getSize()));
		mColumnNameLabel.setText(text);
		}

	/**
	 * Returns a unique filter type ID: one of FILTER_TYPE_...
	 * @return
	 */
	public abstract int getFilterType();

	/**
	 * Override this, if a derived filter has extended animation settings.
	 * @return
	 */
	public String getAnimationSettings() {
		if (mAnimator == null || mAnimationButton == null)
			return null;

		String settings = mAnimationButton.isAnimating() ? "state=running" : "state=stopped";
		if (mAnimator.getFrameMillis() != getDefaultFrameMillis())
			settings = settings.concat(" delay=").concat(Long.toString(mAnimator.getFrameMillis()));

		return settings;
		}

	public void setIsUserChange(boolean b) {
		mIsUserChange = b;
		}

	@Override
	public void setEnabled(boolean b) {
		if (isEnabled() != b
		 && (!b || canEnable(mSuppressErrorMessages))) {
			super.setEnabled(b);
			mButtonDisabled.setSelected(!b);
			if (isActive()) {
				if (!b) {
					mTableModel.freeRowFlag(mExclusionFlag);
					mExclusionFlag = -1;
					}
				else {
					mExclusionFlag = mTableModel.getUnusedRowFlag(true);
					if (mExclusionFlag == -1) {
						mButtonDisabled.setSelected(true);
						b = false;
						}
					else {
						updateExclusion(mIsUserChange);
						}
					}
				}
			enableItems(b);
			mButtonInverse.setEnabled(b);
			if (mAnimationButton != null)
				mAnimationButton.setEnabled(b);
			if (mAnimator != null)
				mAnimator.setSuspended(!b);
			}
		}

	public void setInverse(boolean b) {
		if (mIsInverse != b) {
			mIsInverse = b;
			mButtonInverse.setSelected(b);
			if (isEnabled() && isActive()) {
				mTableModel.invertExclusion(mExclusionFlag);
				}
			}
		}

	public final String getSettings() {
		StringBuilder sb = new StringBuilder();
		if (isInverse())
			sb.append(INVERSE_CODE);
		if (!isEnabled()) {
			if (sb.length() != 0)
				sb.append('\t');
			sb.append(DISABLED_CODE);
			}
		String innerSettings = getInnerSettings();
		if (innerSettings != null) {
			if (sb.length() != 0)
				sb.append('\t');
			sb.append(innerSettings);
			}
		return (sb.length() == 0 && innerSettings == null) ? null : sb.toString();
		}

	public void applySettings(String settings, boolean suppressErrorMessages) {
		mIsUserChange = false;
		mSuppressErrorMessages = suppressErrorMessages;

		boolean inverse = false;
		if (settings != null && settings.startsWith(INVERSE_CODE)) {
			inverse = true;
			settings = (settings.length() == INVERSE_CODE.length()) ? null : settings.substring(INVERSE_CODE.length()+1);
			}
		boolean enabled = true;
		if (settings != null && (settings.startsWith(DISABLED_CODE) || settings.startsWith("<disabled>"))) {
			// || settings.startsWith("<disabled>") to be compatible with earlier "<disabled>" option as inner settings
			enabled = false;
			settings = (settings.length() == DISABLED_CODE.length()) ? null : settings.substring(DISABLED_CODE.length()+1);
			}

		setEnabled(enabled);
		setInverse(inverse);

		applyInnerSettings(settings);

		mSuppressErrorMessages = false;
		mIsUserChange = true;
		}

	public abstract void applyInnerSettings(String settings);
	public abstract String getInnerSettings();

	public Animator getAnimator() {
		return mAnimator;
		}

	public boolean canAnimate() {
		return mAnimator != null;
		}

	public boolean isAnimating() {
		return mAnimationButton != null && mAnimationButton.isAnimating();
		}

	public final void applyAnimationSettings(String settings) {
		mIsUserChange = false;
		resetAnimation();
		clearAnimationSettings();
		if (settings != null) {
			boolean running = false;
			for (String setting:settings.split(" ")) {
				int index = setting.indexOf('=');
				if (index != -1) {
					String key = setting.substring(0, index);
					String value = setting.substring(index+1);
					if (key.equals("state"))
						running = value.equals("running");
					else if (key.equals("delay"))
						try { setFrameMillis(Integer.parseInt(value)); } catch (NumberFormatException e) {}
					else
						applyAnimationSetting(key, value);
					}
				}

			if (running)
				startAnimation();
			else
				stopAnimation();
			}
		mIsUserChange = true;
		}

	/**
	 * Override this, if a derived filter has extended animation settings.
	 * @return
	 */
	protected void clearAnimationSettings() {}

	/**
	 * Override this, if a derived filter has extended animation settings.
	 * @return
	 */
	protected void applyAnimationSetting(String key, String value) {}

	protected String attachTABDelimited(String settingList, String setting) {
		if (settingList == null)
			return setting;

		return settingList + "\t" + setting;
		}

	protected String attachSpaceDelimited(String settingList, String setting) {
		if (settingList == null)
			return setting;

		return settingList + " " + setting;
		}

	/**
	 * Subclass should disable filtering or change to a setting that includes all records
	 * or if this is not possible then remove entire filter.
	 * This is only called, if the filter is active.
	 */
	public void reset() {
		mIsUserChange = false;
		if (mAnimator != null)
			resetAnimation();

		innerReset();

		setInverse(false);
		mIsUserChange = true;
		}

	/**
	 * Only called, if the filter is active.
	 */
	public abstract void innerReset();

	public void showAnimationControls(JPanel panel) {
//		JPanel mbp = new JPanel();
		mAnimationButton = new HiDPIIconButton("gear16.png", "Animation Options", "showPopup");
		mAnimationButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (mAnimationButton.isEnabled() && mAnimationPopup == null)
					showAnimationPopup(e);
				}
			});
		panel.add(mAnimationButton);
//		mTitlePanel.add(mbp, "3,0");
		}

	private void showAnimationPopup(MouseEvent e) {
		JButton button = (JButton)e.getSource();
		mAnimationPopup = new JPopupMenu() {
			private static final long serialVersionUID = 1L;

			@Override
			public void setVisible(boolean b) {
				super.setVisible(b);
				mAnimationPopup = null;
				}
			};
		if (isAnimating())
			mAnimationPopup.add(createPopupItem("Stop Animation", "stop"));
		else
			mAnimationPopup.add(createPopupItem("Start Animation", "start"));
		mAnimationPopup.addSeparator();
		mAnimationPopup.add(createPopupItem("Animation Options...", "options"));
		mAnimationPopup.show(button.getParent(),
							 button.getBounds().x,
							 button.getBounds().y + button.getBounds().height);
		}

	private JMenuItem createPopupItem(String title, String command) {
		JMenuItem item = new JMenuItem(title);
		item.setActionCommand(command);
		item.addActionListener(this);
		return item;
		}

	/**
	 * Override this, if your filter has a different default frame rate
	 * @return
	 */
	public int getDefaultFrameMillis() {
		return 50;
		}

	/**
	 * Override this, if your filter supports animations
	 */
	public void applyAnimationOptions() {}

	public int getFrameMillis() {
		return mAnimator.getFrameMillis();
		}

	/**
	 * Changes the delay that the animation timer waits between two subsequent calls to animate().
	 */
	public void setFrameMillis(int millis) {
		if (isActive())
			mAnimator.setFrameMillis(millis);
		}

	/**
	 * Stops the animation timer and sets the current frame to 0.
	 */
	public void resetAnimation() {
		if (isActive())
			mAnimator.reset();
		}

	/**
	 * Starts or continues the animation with the current frame rate and number.
	 */
	public void startAnimation() {
		if (canAnimate()) {
			if (isActive())
				mAnimator.start();
			mAnimationButton.startAnimation(4);
			}
		}

	/**
	 * Stops the animation timer without changing the current frame number.
	 */
	public void stopAnimation() {
		if (isActive())
			mAnimator.stop();
		mAnimationButton.stopAnimation();
		}

	public void setAnimationSuspended(boolean b) {
		if (mAnimator != null)
			mAnimator.setSuspended(b);
		}

	public boolean isAnimationSuspended() {
		return mAnimator != null && mAnimator.isSuspended();
		}

	public void skipAnimationFrames(long millis) {
		if (mAnimator != null)
			mAnimator.skipFrames(millis);
		}

	/**
	 * Override this if the derived filter supports animations.
	 * When an animation is running this method is called repeatedly
	 * after waiting for the frame delay with an increasing frame number.
	 * The filter should react by updating its filter settings to reflect
	 * the current frame number.
	 * @param frame
	 */
	public void setAnimationFrame(int frame) {}

	/**
	 * Override this if the derived filter supports animations.
	 * @return frame count for one full animation
	 */
	public int getFullFrameAnimationCount() {
		return 0;
	}

	public class Animator implements Runnable {
		private volatile long mStartMillis,mSuspendMillis;
		private volatile int mRecentFrame,mFrameMillis;
		private Thread mThread;

		public Animator(int frameRate) {
			mFrameMillis = frameRate;
			}

		public boolean isAnimating() {
			return mThread != null;
			}

		public int getFrameMillis() {
			return mFrameMillis;
			}

		public void setFrameMillis(int millis) {
			mFrameMillis = millis;
			}

		public void reset() {
			mThread = null;
			}

		public void start() {
			mRecentFrame = -1;
			mSuspendMillis = 0L;
			if (mThread == null) {
				mThread = new Thread(this);
				mThread.start();
				mStartMillis = System.currentTimeMillis();
				}
			}

		public void stop() {
			mThread = null;
			}

		public boolean isSuspended() {
			return mSuspendMillis != 0L;
			}

		public void setSuspended(boolean b) {
			if (b != isSuspended()) {
				if (b) {
					mSuspendMillis = System.currentTimeMillis();
					}
				else {
					mStartMillis += (System.currentTimeMillis() - mSuspendMillis);
					mSuspendMillis = 0L;
					}
				}
			}

		public void skipFrames(long millis) {
			if (mSuspendMillis != 0L) {
				mSuspendMillis += millis;
				showFrameInEDT(calculateFrameNo());
				}
			else {
				mStartMillis -= millis;
				if (mFrameMillis>100)
					showFrameInEDT(calculateFrameNo());
				}
			}

		@Override
		public void run() {
			while (Thread.currentThread() == mThread) {
				final int frame = calculateFrameNo();
				if (frame != mRecentFrame)
					try {
						SwingUtilities.invokeAndWait(() -> showFrameInEDT(frame));
						}
					catch (Exception e) {}

				try {
					Thread.sleep(mFrameMillis - (System.currentTimeMillis() - mStartMillis) % mFrameMillis);
					}
				catch (InterruptedException ie) {}
				}
			}

		private int calculateFrameNo() {
			long millis = (mSuspendMillis != 0L ? mSuspendMillis : System.currentTimeMillis()) - mStartMillis;
			return (int)(millis / mFrameMillis);
			}

		/**
		 * Determines from mStartMillis and currentTimeMillis() and mFrameRate the frame number
		 * that should be shown currently and calls animate, if the most recent call to this method
		 * used a different frame number.
		 * @param frame the frame number calculated from relevant start millis and millis needed for one frame
		 */
		public void showFrameInEDT(int frame) {
			if (mThread != null && frame != mRecentFrame) {
				mRecentFrame = frame;
				mIsUserChange = false;
				setAnimationFrame(frame);
				mIsUserChange = true;
				}
			}
		}
	}
