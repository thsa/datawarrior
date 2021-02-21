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

package com.actelion.research.datawarrior.task.view;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.task.AbstractViewTask;
import com.actelion.research.datawarrior.task.DEMacroRecorder;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.view.*;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Properties;


public abstract class DETaskAbstractSetViewOptions extends AbstractViewTask implements ActionListener,ChangeListener,ItemListener {
	public enum OTHER_VIEWS { NONE, GRAPHICAL, GRAPHICAL2D, STRUCTURE };
	private static final String PROPERTY_VIEW_NAME = "viewName";
	private static final String PROPERTY_ALL_VIEWS = "allViews";

	private Properties	mOldConfiguration,mLastGoodConfiguration;
	private boolean		mIgnoreEvents;
	private JCheckBox	mCheckBoxApplyToAll;
	private HashMap<CompoundTableView,Properties> mOtherViewConfigurationMap;

	/**
	 * Instantiates this task to be run in the event dispatch thread
	 * @param owner
	 * @param view null or view that is interactively updated
	 */
	public DETaskAbstractSetViewOptions(Frame owner, DEMainPane mainPane, CompoundTableView view) {
		this(owner, mainPane, view, false);
		}

	/**
	 * @param owner
	 * @param view null or view that is interactively updated
	 * @param useOwnThread
	 */
	public DETaskAbstractSetViewOptions(Frame owner, DEMainPane mainPane, CompoundTableView view, boolean useOwnThread) {
		super(owner, mainPane, view, useOwnThread);
		mIgnoreEvents = false;

		initialize();
		}

	/**
	 * Initialization to be called from the constructor. If a derived class' constructor
	 * defines members, which are needed in a derived initialization (i.e. in getViewConfiguration()),
	 * then override this method with an empty one and to the initialization after the derived
	 * constructor has completed.
	 */
	protected void initialize() {
		if (hasInteractiveView()) {
			mOldConfiguration = getViewConfiguration();
			mLastGoodConfiguration = mOldConfiguration;
			}
		}

	public abstract JComponent createViewOptionContent();

	public final JComponent createInnerDialogContent() {
		if (getOtherViewMode() == OTHER_VIEWS.NONE
		|| (getOtherViewMode() == OTHER_VIEWS.GRAPHICAL2D && getInteractiveView() != null && getInteractiveView() instanceof VisualizationPanel3D))
			return createViewOptionContent();

		int gap = HiDPIHelper.scale(8);
		double size[][] = { {TableLayout.FILL, TableLayout.PREFERRED, TableLayout.FILL}, {TableLayout.PREFERRED, gap, TableLayout.PREFERRED} };

		JPanel p = new JPanel();
		p.setLayout(new TableLayout(size));

		p.add(createViewOptionContent(), "0,0,2,0");

		String text = getOtherViewMode() == OTHER_VIEWS.GRAPHICAL ? "Apply to all graphical views"
				    : getOtherViewMode() == OTHER_VIEWS.GRAPHICAL2D ? "Apply to all graphical 2D-views"
					: getOtherViewMode() == OTHER_VIEWS.STRUCTURE ? "Apply to all structure views" : "Apply to all views";
		mCheckBoxApplyToAll = new JCheckBox(text);
		mCheckBoxApplyToAll.addActionListener(this);
		p.add(mCheckBoxApplyToAll, "1,2");

		return p;
		}

		@Override
	public final DEFrame getNewFrontFrame() {
		return null;
		}

	public boolean isIgnoreEvents() {
		return mIgnoreEvents;
		}

	/**
	 * Set temporarily to true, if programmatical changes of checkboxes, popups and sliders
	 * shall not cause calls of getDialogConfiguration() and applyConfiguration().
	 * @param b
	 */
	public void setIgnoreEvents(boolean b) {
		mIgnoreEvents = b;
		}

	/**
	 * Returns the 2D- or 3D-JVisualization object of the interactive view passed with the constructor.
	 * @return null if not interactive or if no 2D-/3D-view is associated with this task
	 */
	public JVisualization getInteractiveVisualization() {
		CompoundTableView view  = getInteractiveView();
		return (view == null || !(view instanceof VisualizationPanel)) ? null : ((VisualizationPanel)view).getVisualization();
		}

	@Override
	/**
	 * If no view is specified, then this returns the recently applied view settings.
	 * Otherwise it returned the currently active view settings of the specified view.
	 */
	public final Properties getRecentConfiguration() {
		return (!hasInteractiveView())	? super.getRecentConfiguration() : getViewConfiguration();
		}

	/**
	 * Creates and returns the current configuration, i.e. the respective view settings
	 * of the interactive view.
	 * @return
	 */
	private Properties getViewConfiguration() {
		Properties configuration = new Properties();

		configuration.setProperty(PROPERTY_VIEW_NAME, getInteractiveViewName());
		if (mCheckBoxApplyToAll != null)
			configuration.setProperty(PROPERTY_ALL_VIEWS, mCheckBoxApplyToAll.isSelected() ? "true" : "false");

		addViewConfiguration(getInteractiveView(), configuration);
		return configuration;
		}

	/**
	 * Compiles the current configuration, i.e. all settings of the
	 * passed view in the context of this task.
	 * @param view to get the configuration from
	 * @param configuration is pre-initialized with the view identifying name
	 */
	public abstract void addViewConfiguration(CompoundTableView view, Properties configuration);

	@Override
	public final Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		if (mCheckBoxApplyToAll != null)
			configuration.setProperty(PROPERTY_ALL_VIEWS, mCheckBoxApplyToAll.isSelected() ? "true" : "false");
		addDialogConfiguration(configuration);
		return configuration;
		}

	/**
	 * Compiles the configuration currently defined by the dialog GUI elements.
	 * @param configuration is pre-initialized with the view identifying name
	 */
	public abstract void addDialogConfiguration(Properties configuration);

	@Override
	public final void setDialogConfigurationToDefault() {
		if (mOldConfiguration != null) {
			setDialogConfiguration(mOldConfiguration);
			}
		else {
			setDialogToDefault();
			}

		enableItems();
		}

	@Override
	public final void setDialogConfiguration(Properties configuration) {
		mIgnoreEvents = true;
		super.setDialogConfiguration(configuration);
		if (mCheckBoxApplyToAll != null)
			mCheckBoxApplyToAll.setSelected("true".equals(configuration.getProperty(PROPERTY_ALL_VIEWS)));
		setDialogToConfiguration(configuration);
		enableItems();
		mIgnoreEvents = false;
//		if (hasInteractiveView() /* && isConfigurationValid(configuration, true)  must be valid, because in case of interactive view it was taken from view itself, besides we don't want warning messages */)
//			mLastGoodConfiguration = configuration;
		}

	public abstract void setDialogToConfiguration(Properties configuration);

	@Override
	public final boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (!super.isConfigurationValid(configuration, isLive))
			return false;

		return isViewConfigurationValid(getConfiguredView(configuration), configuration);
		}

	/**
	 * @param view null (if editing macro) or the live view (if executing or editing live)
	 * @param configuration
	 * @return
	 */
	public abstract boolean isViewConfigurationValid(CompoundTableView view, Properties configuration);

	@Override
	public final void runTask(Properties configuration) {
		CompoundTableView view = getConfiguredView(configuration);
		applyConfiguration(view, configuration, false);

		if (getOtherViewMode() != OTHER_VIEWS.NONE && "true".equals(configuration.getProperty(PROPERTY_ALL_VIEWS)))
			applyConfigurationForOtherViews(configuration);
		}

	private void applyConfigurationForOtherViews(Properties configuration) {
		CompoundTableView viewToExclude = getConfiguredView(configuration);
		for (String title:getMainPane().getDockableTitles()) {
			CompoundTableView otherView = getMainPane().getView(title);
			if (otherView != viewToExclude)
				applyConfiguration(otherView, configuration, false);
			}
		}

	private void buildOtherViewConfigurationMap() {
		OTHER_VIEWS mode = getOtherViewMode();
		if (mOtherViewConfigurationMap == null && mode != OTHER_VIEWS.NONE) {
			mOtherViewConfigurationMap = new HashMap<>();
			for (String title:getMainPane().getDockableTitles()) {
				CompoundTableView otherView = getMainPane().getView(title);
				if (otherView != getInteractiveView()
				 && ((mode == OTHER_VIEWS.GRAPHICAL && otherView instanceof VisualizationPanel)
				  || (mode == OTHER_VIEWS.GRAPHICAL2D && otherView instanceof VisualizationPanel2D)
				  || (mode == OTHER_VIEWS.STRUCTURE && otherView instanceof JStructureGrid))) {
					Properties configuration = new Properties();
					addViewConfiguration(otherView, configuration);
					mOtherViewConfigurationMap.put(otherView, configuration);
					}
				}
			}
		}

	/**
	 * Override to allow setting options for all views. In this case the task is responsible to set options on valid views only
	 * @return
	 */
	public OTHER_VIEWS getOtherViewMode() {
		return OTHER_VIEWS.NONE;
		}

	/**
	 * This may be called on views of a different or even incompatible type. The specific SetViewOptions task
	 * needs to decide, whether it can apply its configuration on the view type presented.
	 * @param view may be an unexpected view type, thus check!!!
	 * @param configuration
	 * @param isAdjusting
	 */
	public abstract void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting);

	/**
	 * This is supposed to enable/disable UI elements of which the enabling state
	 * depends dynamically on the setting of other UI element.
	 */
	public abstract void enableItems();

	/**
	 * This sets all dialog elements to reasonable default settings.
	 * The method is called only, if no associated view is available.
	 */
	public abstract void setDialogToDefault();

	@Override
	public void doCancelAction() {
		// View settings are immediately changed, when the dialog is modified.
		// Therefore, when pressing 'Cancel', we need to roll back...
		if (hasInteractiveView()) {
			applyConfiguration(getInteractiveView(), mOldConfiguration, false);
			revertOtherViews();
			}
		closeDialog();
		}

	private void revertOtherViews() {
		if (mOtherViewConfigurationMap != null) {
			for (CompoundTableView otherView : mOtherViewConfigurationMap.keySet())
				applyConfiguration(otherView, mOtherViewConfigurationMap.get(otherView), false);

			mOtherViewConfigurationMap = null;
			}
		}

	@Override
	public void doOKAction() {
		// View settings are immediately changed, when the dialog is modified.
		// Therefore, when pressing 'OK', we don't need to perform the task.
		if (hasInteractiveView()) {
			// don't set status OK to avoid running the full task, but record task if is recording and apply for other views, if needed
			DEMacroRecorder.record(this, getDialogConfiguration());
			}
		else {
			// set OK is needed, if a new task was created to keep it with the given configuration
			setStatusOK();
			}
		closeDialog();
		}

	/**
	 * If you override this method, then make sure you call this super method at the end.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mCheckBoxApplyToAll) {
			if (mCheckBoxApplyToAll.isSelected()) {
				buildOtherViewConfigurationMap();
				for (CompoundTableView otherView:mOtherViewConfigurationMap.keySet())
					applyConfiguration(otherView, mLastGoodConfiguration, false);
				}
			else {
				revertOtherViews();
				}
			}
		else if (!mIgnoreEvents) {
			update(false);
			}
		}

	/**
	 * If you override this method, then make sure you call this super method at the end.
	 */
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (!mIgnoreEvents
		 && (!(e.getSource() instanceof JComboBox) || e.getStateChange() == ItemEvent.SELECTED))
			SwingUtilities.invokeLater(new Runnable() {	// wait for the combo popup to close before showing any message caused by isViewConfigurationValid()
				@Override
				public void run() {
					update(false);
				}
			});
		}

	/**
	 * If you override this method, then make sure you call this super method at the end.
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		if (!mIgnoreEvents) {
			boolean isAdjusting = (e.getSource() instanceof JSlider) && ((JSlider)e.getSource()).getValueIsAdjusting();
			update(isAdjusting);
			}
		}

	/**
	 * Calls enableItems(), gets the current dialog configuration with getDialogConfiguration(),
	 * checks the configuration with isConfigurationValid() and if that returns true calls applyConfiguration().
	 * If the current dialog configuration is not valid, calls setDialogToConfiguration(lastValidConfiguration)
	 * with disabled events.
	 * This method is called automatically by actionPerformed(),itemStateChanged() and stateChanged()
	 */
	protected void update(boolean isAdjusting) {
		enableItems();

		if (hasInteractiveView()) {
			Properties configuration = getDialogConfiguration();
			if (isConfigurationValid(configuration, true)) {
				mLastGoodConfiguration = configuration;
				applyConfiguration(getInteractiveView(), configuration, isAdjusting);
				if (getOtherViewMode() != OTHER_VIEWS.NONE && mOtherViewConfigurationMap != null)
					for (CompoundTableView otherView:mOtherViewConfigurationMap.keySet())
						applyConfiguration(otherView, configuration, isAdjusting);
				}
/*
 * TODO: One could consider instead of going back to the last valid configuration
 * (which is not necessarily available, if we edit a task of a macro)
 * catching the message by overriding showErrorMessage() and displaying it in
 * an additional JPanel below the dialog content.		
 */
			else if (mLastGoodConfiguration != null) {
				mIgnoreEvents = true;
				setDialogToConfiguration(mLastGoodConfiguration);
				enableItems();
				mIgnoreEvents = false;
				}
			}
		}

	@Override
	public void showErrorMessage(final String msg) {
		// we have to postpone, because the message dialog should not interfere with the shown popup
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				DETaskAbstractSetViewOptions.super.showErrorMessage(msg);
				}
			});
		}
	}

