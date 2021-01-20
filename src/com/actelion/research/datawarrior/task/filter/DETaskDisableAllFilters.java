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

package com.actelion.research.datawarrior.task.filter;

import java.util.Properties;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.DEPruningPanel;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.dock.Dockable;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.view.VisualizationPanel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;

public class DETaskDisableAllFilters extends ConfigurableTask {
    public static final String TASK_NAME = "Disable All Filters";

    private static final String PROPERTY_INCLUDE_VIEWS = "includeViews";

	private DEPruningPanel  mPruningPanel;
	private DEMainPane      mMainPane;
	private JCheckBox       mCheckBoxIncludeViews;

	public DETaskDisableAllFilters(DEFrame parent) {
		super(parent, false);
		mPruningPanel = parent.getMainFrame().getPruningPanel();
		mMainPane = parent.getMainFrame().getMainPane();
		}

	@Override
	public Properties getPredefinedConfiguration() {
		if (isInteractive()) {
			boolean rowHidingViewsFound = false;
			for(Dockable d:mMainPane.getDockables())
				if (d.getContent() instanceof VisualizationPanel
				 && ((VisualizationPanel)d.getContent()).getVisualization().isGloballyHidingRows())
					rowHidingViewsFound = true;

			boolean includeViews = rowHidingViewsFound
					&& JOptionPane.showConfirmDialog(getParentFrame(),
					"The configuration of some graphical views causes rows to be hidden in other views.\n"
							+ "Do you want to switch off 'global row hiding' for those views?/n"
							+ "(you may revert this in 'Set Graphical View Options -> Row hiding')", "Include Graphical Views?",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;

			Properties configuration = new Properties();
			configuration.setProperty(PROPERTY_INCLUDE_VIEWS, includeViews ? "true" : "false");
			return configuration;
			}

		return null;
		}

	@Override
	public JComponent createDialogContent() {
		mCheckBoxIncludeViews = new JCheckBox("Configure graphical views to not contribute to global row hiding");

		JPanel content = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap}, {gap, TableLayout.PREFERRED, gap} };
		content.setLayout(new TableLayout(size));

		content.add(mCheckBoxIncludeViews, "1,1");

		return content;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_INCLUDE_VIEWS, mCheckBoxIncludeViews.isSelected() ? "true" : "false");
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mCheckBoxIncludeViews.setSelected("true".equals(configuration.getProperty(PROPERTY_INCLUDE_VIEWS)));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mCheckBoxIncludeViews.setSelected(false);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		return true;
		}

	@Override
	public boolean isConfigurable() {
		return true;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public void runTask(Properties configuration) {
		mPruningPanel.disableAllFilters();
		for(Dockable d:mMainPane.getDockables())
			if (d.getContent() instanceof VisualizationPanel)
				((VisualizationPanel)d.getContent()).getVisualization().setUseAsFilter(false);
		if ("true".equals(configuration.getProperty(PROPERTY_INCLUDE_VIEWS)))
			for(Dockable d:mMainPane.getDockables())
				if (d.getContent() instanceof VisualizationPanel
				 && ((VisualizationPanel)d.getContent()).getVisualization().isGloballyHidingRows())
					((VisualizationPanel)d.getContent()).getVisualization().setAffectGlobalExclusion(false);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
