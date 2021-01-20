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

import info.clearthought.layout.TableLayout;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.dock.Dockable;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.VisualizationPanel;
import com.actelion.research.table.view.VisualizationPanel2D;
import com.actelion.research.table.view.VisualizationPanel3D;

public class DETaskSynchronizeView extends ConfigurableTask implements ActionListener {
	public static final String TASK_NAME = "Synchronize View";

	private static final String PROPERTY_VIEW = "view";
	private static final String PROPERTY_MASTER = "master";

	private static final String NO_MASTER_ITEM = "<none>";

	private DEMainPane	mMainPane;
	private JComboBox	mComboBoxView,mComboBoxMaster;
	private String		mViewName,mMasterViewName;

	public DETaskSynchronizeView(Frame parent, DEMainPane mainPane, String viewName, String masterViewName) {
		super(parent, false);
		mMainPane = mainPane;
		mViewName = viewName;
		mMasterViewName = masterViewName;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public JComponent createDialogContent() {
		JPanel p = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8} };
		p.setLayout(new TableLayout(size));

		p.add(new JLabel("Slave view:"), "1,1");
		mComboBoxView = new JComboBox(DEMainPane.VIEW_TYPE_ITEM);
		mComboBoxView.addActionListener(this);
		p.add(mComboBoxView, "3,1");

		p.add(new JLabel("Master name:"), "1,3");
		mComboBoxMaster = new JComboBox();
		mComboBoxMaster.addItem(NO_MASTER_ITEM);
		for (Dockable d:mMainPane.getDockables())
			if (d.getContent() instanceof VisualizationPanel)
				mComboBoxMaster.addItem(d.getTitle());
		mComboBoxMaster.setEditable(true);
		p.add(mComboBoxMaster, "3,3");

		return p;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mComboBoxView) {
			Dockable dockable = mMainPane.getDockable((String)mComboBoxView.getSelectedItem());
			if (dockable != null) {
				CompoundTableView view = (CompoundTableView)dockable.getContent();
				if (view instanceof VisualizationPanel) {
					int dimensions = ((VisualizationPanel)view).getDimensionCount();
					mComboBoxMaster.removeAllItems();
					mComboBoxMaster.addItem(NO_MASTER_ITEM);
					for (Dockable d:mMainPane.getDockables())
						if (d != dockable
						 && d.getContent() instanceof VisualizationPanel
						 && ((VisualizationPanel)d.getContent()).getDimensionCount() >= dimensions)
							mComboBoxMaster.addItem(d.getTitle());
					}
				}
			return;
			}
		}

	@Override
	public Properties getPredefinedConfiguration() {
		if (mViewName == null)
			return null;

		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_VIEW, mViewName);
		if (mMasterViewName != null)
			configuration.setProperty(PROPERTY_MASTER, mMasterViewName);
		return configuration;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_VIEW, (String)mComboBoxView.getSelectedItem());
		if (mComboBoxMaster.getSelectedIndex() != 0)
			configuration.setProperty(PROPERTY_MASTER, (String)mComboBoxMaster.getSelectedItem());
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mComboBoxView.setSelectedItem(configuration.getProperty(PROPERTY_VIEW));
		String master = configuration.getProperty(PROPERTY_MASTER);
		mComboBoxMaster.setSelectedItem(master == null ? NO_MASTER_ITEM : master);
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mComboBoxMaster.setSelectedItem(NO_MASTER_ITEM);
		}

	@Override
	public boolean isConfigurable() {
		int count = 0;
		for (Dockable d:mMainPane.getDockables())
			if (d.getContent() instanceof VisualizationPanel)
				count++;
		if (count < 2) {
			showErrorMessage("Less than two 2D- or 3D-Views found.");
			return false;
			}
		return true;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String n1 = configuration.getProperty(PROPERTY_VIEW);
		if (n1 == null) {
			showErrorMessage("Slave view not defined.");
			return false;
			}
		if (isLive) {
			Dockable d1 = mMainPane.getDockable(n1);
			if (d1 == null) {
				showErrorMessage("View '"+n1+"' not found.");
				return false;
				}
			CompoundTableView v1 = (CompoundTableView)d1.getContent();
			if (!(v1 instanceof VisualizationPanel)) {
				showErrorMessage("View '"+n1+"' is neigher a 2D- nor a 3D-View.");
				return false;
				}
			String n2 = configuration.getProperty(PROPERTY_MASTER);
			if (n2 != null) {
				Dockable d2 = mMainPane.getDockable(n2);
				if (d2 == null) {
					showErrorMessage("View '"+n2+"' not found.");
					return false;
					}
				CompoundTableView v2 = (CompoundTableView)d2.getContent();
				if (!(v2 instanceof VisualizationPanel)) {
					showErrorMessage("View '"+n1+"' is neigher a 2D- nor a 3D-View.");
					return false;
					}
				if (v1 instanceof VisualizationPanel3D && v2 instanceof VisualizationPanel2D) {
					showErrorMessage("Master view has less dimensions than the slave view.");
					return false;
					}
				}
			}
		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		VisualizationPanel slave = (VisualizationPanel)mMainPane.getDockable(configuration.getProperty(PROPERTY_VIEW)).getContent();
		String masterName = configuration.getProperty(PROPERTY_MASTER);
		VisualizationPanel master = (masterName == null) ? null : (VisualizationPanel)mMainPane.getDockable(masterName).getContent();
		mMainPane.synchronizeView(slave, master);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
