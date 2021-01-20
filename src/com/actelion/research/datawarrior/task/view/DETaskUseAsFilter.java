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
import com.actelion.research.gui.dock.Dockable;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.VisualizationPanel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskUseAsFilter extends AbstractViewTask {
	public static final String TASK_NAME = "Use View As Explicit Filter";

	private static final String PROPERTY_USE_AS_FILTER = "useAsFilter";

	private DEMainPane	mMainPane;
	private JCheckBox	mCheckBoxIsFilter;

	public DETaskUseAsFilter(Frame parent, DEMainPane mainPane, CompoundTableView view) {
		super(parent, mainPane, view);
		mMainPane = mainPane;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public JComponent createInnerDialogContent() {
		JPanel p = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap},
		        			{gap, TableLayout.PREFERRED, gap} };
        p.setLayout(new TableLayout(size));

		mCheckBoxIsFilter = new JCheckBox("Use View As Filter");
        p.add(mCheckBoxIsFilter, "1,1");

		return p;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		Properties configuration = super.getPredefinedConfiguration();
		if (configuration == null)
			return null;

		boolean useAsFilter = !((VisualizationPanel)getInteractiveView()).getVisualization().isUsedAsFilter();
		configuration.setProperty(PROPERTY_USE_AS_FILTER, useAsFilter ? "true" : "false");
		return configuration;
		}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return hasInteractiveView();
		}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return null;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_USE_AS_FILTER, mCheckBoxIsFilter.isSelected() ? "true" : "false");
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mCheckBoxIsFilter.setSelected("true".equals(configuration.getProperty(PROPERTY_USE_AS_FILTER)));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mCheckBoxIsFilter.setSelected(false);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		return super.isConfigurationValid(configuration, isLive);
		}

	@Override
	public void runTask(Properties configuration) {
		boolean useAsFilter = "true".equals(configuration.getProperty(PROPERTY_USE_AS_FILTER));
		if (useAsFilter)
			for(Dockable d:mMainPane.getDockables())
				if (d.getContent() instanceof VisualizationPanel
				 && d.getContent() != getConfiguredView(configuration))
					((VisualizationPanel)d.getContent()).getVisualization().setUseAsFilter(false);
		((VisualizationPanel)getConfiguredView(configuration)).getVisualization().setUseAsFilter(useAsFilter);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
