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

package com.actelion.research.datawarrior.task;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.gui.dock.Dockable;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.CompoundTableView;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

/**
 * This class handles the redundancies that all classes operating on a view have:<br>
 * - If the task is started interactively, the referred view is known and should not be part of the dialog.
 * - If the dialog is opened as to edit a macro, an editable combo box allows to select existing and non existing view.
 */
public abstract class AbstractViewTask extends ConfigurableTask {
	private static final String PROPERTY_VIEW_NAME = "viewName";
	private static final String ITEM_SELECTED_VIEW = "<Selected View>";

	private DEMainPane			mMainPane;
	private JComboBox			mComboBox;
	private CompoundTableView	mView;

	/**
	 * @param parent
     * @param mainPane
	 * @param view null, if editing a task of a macro
	 */
	public AbstractViewTask(Frame parent, DEMainPane mainPane, CompoundTableView view) {
		this(parent, mainPane, view, false);
		}

	public AbstractViewTask(Frame parent, DEMainPane mainPane, CompoundTableView view, boolean useOwnThread) {
		super(parent, useOwnThread);
		mMainPane = mainPane;
		mView = view;
		}

	@Override
	public final JComponent createDialogContent() {
		if (hasInteractiveView())
			return createInnerDialogContent();

		JPanel p = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
		        			{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED} };
        p.setLayout(new TableLayout(size));

        p.add(new JLabel("View name:"), "1,1");
        mComboBox = new JComboBox();
        if (allowsSelectedView())
	    	mComboBox.addItem(ITEM_SELECTED_VIEW);
        for (Dockable d:mMainPane.getDockables())
        	if (getViewQualificationError((CompoundTableView)d.getContent()) == null)
        		mComboBox.addItem(d.getTitle());
        mComboBox.setEditable(true);
        p.add(mComboBox, "3,1");

        if (!isViewTaskWithoutConfiguration())
        	p.add(createInnerDialogContent(), "0,3,4,3");

        return p;
		}

    /**
     * Override this and return false if the combobox for view selection
     * shall not contain the meta item 'Selected View'.
     * @return
     */
    public boolean allowsSelectedView() { return true; }
	/**
	 * en- or disables the combobox for view selection
	 */
	protected void setViewSelectionEnabled(boolean b) {
		mComboBox.setEnabled(b);
		}

	/**
	 * @return true if there are no dialog elements apart from the view selector
	 */
	public abstract boolean isViewTaskWithoutConfiguration();

	/**
	 * Create the dialog content without the combo box to select from views.
	 * @return null, if the view task has no configuration other than the view name
	 */
	public abstract JComponent createInnerDialogContent();

	/**
	 * Override this if not all views qualify for this task.
	 * @param view
	 * @return error message if the view doesn't qualify for the task action
	 */
	public String getViewQualificationError(CompoundTableView view) {
		return null;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		if (!hasInteractiveView() || !isViewTaskWithoutConfiguration())
			return null;

		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_VIEW_NAME, getInteractiveViewName());
		return configuration;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		String viewName = hasInteractiveView() ? getInteractiveViewName() : (String)mComboBox.getSelectedItem();
		if (viewName != null && viewName.length() != 0)
			configuration.setProperty(PROPERTY_VIEW_NAME, viewName);
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		if (!hasInteractiveView())
			mComboBox.setSelectedItem(configuration.getProperty(PROPERTY_VIEW_NAME));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		}

	@Override
	public boolean isConfigurable() {
        for (Dockable d:mMainPane.getDockables())
            if (getViewQualificationError((CompoundTableView)d.getContent()) == null)
                return true;

		showErrorMessage("No qualifying view found.");
		return false;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String viewName = configuration.getProperty(PROPERTY_VIEW_NAME);
		if (viewName == null) {
			showErrorMessage("View name not defined.");
			return false;
			}
		if (isLive) {
			Dockable dockable = (ITEM_SELECTED_VIEW.equals(viewName)) ?
					mMainPane.getSelectedDockable() : mMainPane.getDockable(viewName);
			if (dockable == null) {
				showErrorMessage("View '"+viewName+"' not found.");
				return false;
				}
			String error = getViewQualificationError((CompoundTableView)dockable.getContent());
			if (error != null) {
				showErrorMessage("View '"+viewName+"': "+error);
				return false;
				}
			}
		return true;
		}

	@Override
	public boolean isRedundant(Properties previousConfiguration, Properties currentConfiguration) {
		return previousConfiguration.getProperty(PROPERTY_VIEW_NAME).equals(
				currentConfiguration.getProperty(PROPERTY_VIEW_NAME));
		}

	/**
	 * Returns the name of the interactive view that was passed with the constructor.
	 * @return null if task was not created interactively 
	 */
	public String getInteractiveViewName() {
		return mMainPane.getViewTitle(mView);
		}

	/**
	 * Returns the associated view that was passed with the constructor.
	 * @return null if task was not created interactively 
	 */
	public CompoundTableView getInteractiveView() {
		return mView;
		}

	/**
	 * @return true if this task was launched with a view for immediate update
	 */
	public boolean hasInteractiveView() {
		return mView != null;
		}

	/**
	 * Assuming that the configuration is valid, this returns the name of the configured view.
     * If the configuration refers to the 'selected view', then the name of the selected view is returned.
	 * @param configuration
	 * @return
	 */
	public String getConfiguredViewName(Properties configuration) {
        String viewName = configuration.getProperty(PROPERTY_VIEW_NAME);
		return ITEM_SELECTED_VIEW.equals(viewName) ? mMainPane.getSelectedDockable().getTitle() : viewName;
		}

	/**
	 * Assuming that the configuration contains a valid view name, this returns named view.
	 * @param configuration
	 * @return
	 */
	public CompoundTableView getConfiguredView(Properties configuration) {
		String viewName = configuration.getProperty(PROPERTY_VIEW_NAME);
		return ITEM_SELECTED_VIEW.equals(viewName) ? mMainPane.getSelectedView() : mMainPane.getView(viewName);
		}

	/**
	 * Assuming that this task was instantiated with a valid interactive view, its name is set in the configuration.
	 * @param configuration
	 */
	public void setConfiguredView(Properties configuration) {
		configuration.setProperty(PROPERTY_VIEW_NAME, getInteractiveViewName());
		}

	/**
	 * @return the task's DEMainPane, i.e. DockingPanel
	 */
	public DEMainPane getMainPane() {
		return mMainPane;
		}

	/**
	 * @return the task's table model (is never null)
	 */
	public CompoundTableModel getTableModel() {
		return mMainPane.getTableModel();
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
