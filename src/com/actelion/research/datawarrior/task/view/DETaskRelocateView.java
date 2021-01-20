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

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.task.AbstractViewTask;
import com.actelion.research.gui.dock.Dockable;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.view.CompoundTableView;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskRelocateView extends AbstractViewTask {
	public static final String TASK_NAME = "Relocate View";

	private static final String PROPERTY_WHERE_VIEW = "whereView";
	private static final String PROPERTY_WHERE = "where";
	private static final String PROPERTY_SPLITTING = "splitting";

	private static final String[] TEXT_RELATION = { "Center",  "Top", "Left", "Bottom", "Right" };
	private static final String[] CODE_WHERE = { "center",  "top", "left", "bottom", "right" };

	private String		mWhereViewName;
	private int			mWhereLocation;
	private JTextField	mTextFieldSplitting;
	private JComboBox	mComboBoxView,mComboBoxWhere;

	/**
	 * @param parent
	 * @param mainPane
	 * @param view null, if not interactive
	 * @param whereViewName null, if not interactive
	 * @param whereLocation -1, if not interactive
	 */
	public DETaskRelocateView(Frame parent, DEMainPane mainPane, CompoundTableView view, String whereViewName, int whereLocation) {
		super(parent, mainPane, view);
		mWhereViewName = whereViewName;
		mWhereLocation = whereLocation;
		}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return hasInteractiveView();
		}

	@Override
	public Properties getPredefinedConfiguration() {
		Properties configuration = super.getPredefinedConfiguration();
		if (configuration != null) {
			configuration.setProperty(PROPERTY_WHERE_VIEW, mWhereViewName);
			configuration.setProperty(PROPERTY_WHERE, CODE_WHERE[mWhereLocation]);
			configuration.setProperty(PROPERTY_SPLITTING, "0.5");
			}

		return configuration;
		}

	@Override
	public JComponent createInnerDialogContent() {
		JPanel p = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };
		p.setLayout(new TableLayout(size));

		p.add(new JLabel("Target view:"), "1,1");
		mComboBoxView = new JComboBox();
		for (Dockable d:getMainPane().getDockables())
//			if (d.isVisibleDockable())
				mComboBoxView.addItem(d.getTitle());
		mComboBoxView.setEditable(true);
		p.add(mComboBoxView, "3,1");

		p.add(new JLabel(" at "), "4,1");
		mComboBoxWhere = new JComboBox(TEXT_RELATION);
		p.add(mComboBoxWhere, "5,1");

		p.add(new JLabel("Splitting"), "1,3");
		mTextFieldSplitting = new JTextField(2);
		p.add(mTextFieldSplitting, "3,3");

		return p;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_WHERE_VIEW, (String)mComboBoxView.getSelectedItem());
		configuration.setProperty(PROPERTY_WHERE, CODE_WHERE[mComboBoxWhere.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_SPLITTING, mTextFieldSplitting.getText());
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		String whereViewName = configuration.getProperty(PROPERTY_WHERE_VIEW);
		mComboBoxView.setSelectedItem(whereViewName);
		int where = findListIndex(configuration.getProperty(PROPERTY_WHERE), CODE_WHERE, 0);
		mComboBoxWhere.setSelectedIndex(where);
		mTextFieldSplitting.setText(configuration.getProperty(PROPERTY_SPLITTING, "0.5"));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		if (mComboBoxView.getItemCount() != 0)
			mComboBoxView.setSelectedIndex(mComboBoxView.getItemCount()-1);
		mComboBoxWhere.setSelectedIndex(0);
		mTextFieldSplitting.setText("0.5");
		}

	@Override
	public boolean isConfigurable() {
		if (getMainPane().getDockableCount() < 2) {
			showErrorMessage("To relocate a view one needs multiple existing views.");
			return false;
			}

		return true;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String viewName = configuration.getProperty(PROPERTY_WHERE_VIEW);
		if (viewName == null) {
			showErrorMessage("Target view name not defined.");
			return false;
			}
		if (viewName.equals(getConfiguredViewName(configuration))) {
			showErrorMessage("The target view cannot be the moved view.");
			return false;
			}
		try {
			float splitting = Float.parseFloat(configuration.getProperty(PROPERTY_SPLITTING, "0.5"));
			if (splitting < 0.1f || splitting > 0.9f) {
				showErrorMessage("The splitting value must be between 0.1 and 0.9.");
				return false;
				}
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("The splitting value is not numerical.");
			return false;
			}
		if (isLive) {
			Dockable dockable = getMainPane().getDockable(viewName);
			if (dockable == null) {
				showErrorMessage("Target view '"+viewName+"' not found.");
				return false;
				}
			}

		return super.isConfigurationValid(configuration, isLive);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public void runTask(Properties configuration) {
		String viewName = getConfiguredViewName(configuration);
		String whereView = configuration.getProperty(PROPERTY_WHERE_VIEW);
		float splitting = Float.parseFloat(configuration.getProperty(PROPERTY_SPLITTING, "0.5"));
		int where = findListIndex(configuration.getProperty(PROPERTY_WHERE), CODE_WHERE, 0);
		getMainPane().doRelocateView(viewName, whereView, where, splitting);
		}
	}
