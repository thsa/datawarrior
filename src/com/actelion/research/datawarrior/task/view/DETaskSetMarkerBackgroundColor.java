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
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableListHandler;
import com.actelion.research.table.view.*;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskSetMarkerBackgroundColor extends DETaskAbstractSetColor {
	public static final String TASK_NAME = "Set Marker Background Color";

	private static final String PROPERTY_CONSIDER = "consider";
	private static final String PROPERTY_CONSIDER_VISIBLE = "visible";
	private static final String PROPERTY_CONSIDER_ALL = "all";
	private static final String PROPERTY_RADIUS = "radius";
	private static final String PROPERTY_FADING = "fading";

	private static final String ITEM_VISIBLE_ROWS = "Visible Rows";
	private static final String ITEM_ALL_ROWS = "All Rows";

    private JSlider		mSliderRadius,mSliderFading;
    private JComboBox	mComboBoxConsider;

	public DETaskSetMarkerBackgroundColor(Frame owner,
									DEMainPane mainPane,
									VisualizationPanel2D view) {
		super(owner, mainPane, view, "Set Marker Background Color");
		}

	@Override
	public OTHER_VIEWS getOtherViewMode() {
		return OTHER_VIEWS.GRAPHICAL2D;
		}

	@Override
	public VisualizationColor getVisualizationColor(CompoundTableView view) {
		return ((JVisualization2D)((VisualizationPanel)view).getVisualization()).getBackgroundColor();
		}

	@Override
	public VisualizationColor getVisualizationColor(CompoundTableView view, Properties configuration) {
		return getVisualizationColor(view);
		}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return false;
		}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof VisualizationPanel2D) ? null : "Marker background colors can only be assigned to 2D-Views.";
		}

	@Override
	public JComponent createViewOptionContent() {
		JPanel p = (JPanel)super.createViewOptionContent();

		mComboBoxConsider = new JComboBox();
        mComboBoxConsider.addItem(ITEM_VISIBLE_ROWS);
        mComboBoxConsider.addItem(ITEM_ALL_ROWS);
		for (int i = 0; i<getTableModel().getListHandler().getListCount(); i++) {
			int pseudoColumn = CompoundTableListHandler.getColumnFromList(i);
			mComboBoxConsider.addItem(getTableModel().getColumnTitleExtended(pseudoColumn));
			}
		mComboBoxConsider.setEditable(!hasInteractiveView());
        mComboBoxConsider.addItemListener(this);
		p.add(new JLabel("Consider:"), "1,3");
		p.add(mComboBoxConsider, "3,3");

		int gap = HiDPIHelper.scale(4);
		double size[][] = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap},
				 			{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };
		JPanel sliderpanel = new JPanel();
		sliderpanel.setLayout(new TableLayout(size));
		mSliderRadius = new JSlider(0, 20, 10);
		mSliderRadius.addChangeListener(this);
		mSliderRadius.setMinorTickSpacing(1);
		mSliderRadius.setMajorTickSpacing(5);
		mSliderRadius.setPaintTicks(true);
		mSliderRadius.setPaintLabels(true);
		mSliderRadius.setPreferredSize(new Dimension(HiDPIHelper.scale(160), HiDPIHelper.scale(42)));
		sliderpanel.add(new JLabel("Radius:"), "1,1");
		sliderpanel.add(mSliderRadius, "1,3");

		mSliderFading = new JSlider(0, 20, 10);
		mSliderFading.addChangeListener(this);
		mSliderFading.setMinorTickSpacing(1);
		mSliderFading.setMajorTickSpacing(5);
		mSliderFading.setPaintTicks(true);
		mSliderFading.setPaintLabels(true);
		mSliderFading.setPreferredSize(new Dimension(HiDPIHelper.scale(160), HiDPIHelper.scale(42)));
		sliderpanel.add(new JLabel("Fading:"), "3,1");
		sliderpanel.add(mSliderFading, "3,3");

		p.add(sliderpanel, "1,13,3,13");
		return p;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public void setDialogToDefault() {
		super.setDialogToDefault();
		mComboBoxConsider.setSelectedIndex(0);
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		super.setDialogToConfiguration(configuration);
		String consider = configuration.getProperty(PROPERTY_CONSIDER, PROPERTY_CONSIDER_VISIBLE);
		if (consider.equals(PROPERTY_CONSIDER_VISIBLE)) {
			mComboBoxConsider.setSelectedIndex(0);
			}
		else if (consider.equals(PROPERTY_CONSIDER_ALL)) {
			mComboBoxConsider.setSelectedIndex(1);
			}
		else {
			int pseudoColumn = getTableModel().findColumn(consider);
			mComboBoxConsider.setSelectedItem(!hasInteractiveView() && pseudoColumn == -1 ? consider : getTableModel().getColumnTitleExtended(pseudoColumn));
			}

		int radius = 10;
		try {
			radius = Integer.parseInt(configuration.getProperty(PROPERTY_RADIUS, "10"));
			}
		catch (NumberFormatException nfe) {}
		mSliderRadius.setValue(radius);

		int fading = 10;
		try {
			fading = Integer.parseInt(configuration.getProperty(PROPERTY_FADING, "10"));
			}
		catch (NumberFormatException nfe) {}
		mSliderFading.setValue(20-fading);
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		super.addDialogConfiguration(configuration);
		if (mComboBoxConsider.getSelectedIndex() == 0)
			configuration.setProperty(PROPERTY_CONSIDER, PROPERTY_CONSIDER_VISIBLE);
		else if (mComboBoxConsider.getSelectedIndex() == 1)
			configuration.setProperty(PROPERTY_CONSIDER, PROPERTY_CONSIDER_ALL);
		else
			configuration.setProperty(PROPERTY_CONSIDER, getTableModel().getColumnTitleNoAlias((String)mComboBoxConsider.getSelectedItem()));
		configuration.setProperty(PROPERTY_RADIUS, ""+(mSliderRadius.getValue()));
		configuration.setProperty(PROPERTY_FADING, ""+(20-mSliderFading.getValue()));
		}

	@Override
	public void addViewConfiguration(CompoundTableView view, Properties configuration) {
		super.addViewConfiguration(view, configuration);
		JVisualization2D visualization = (JVisualization2D)((VisualizationPanel)view).getVisualization();
		int hitlist = visualization.getBackgroundColorConsidered();
		if (hitlist == JVisualization2D.BACKGROUND_VISIBLE_RECORDS)
			configuration.setProperty(PROPERTY_CONSIDER, PROPERTY_CONSIDER_VISIBLE);
		else if (hitlist == JVisualization2D.BACKGROUND_ALL_RECORDS)
			configuration.setProperty(PROPERTY_CONSIDER, PROPERTY_CONSIDER_ALL);
		else
			configuration.setProperty(PROPERTY_CONSIDER, getTableModel().getColumnTitleNoAlias(CompoundTableListHandler.getColumnFromList(hitlist)));
		configuration.setProperty(PROPERTY_RADIUS, ""+visualization.getBackgroundColorRadius());
		configuration.setProperty(PROPERTY_FADING, ""+visualization.getBackgroundColorFading());
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		if (view != null) {
			String consider = configuration.getProperty(PROPERTY_CONSIDER, PROPERTY_CONSIDER_VISIBLE);
			if (!consider.equals(PROPERTY_CONSIDER_VISIBLE)
			 && !consider.equals(PROPERTY_CONSIDER_ALL)) {
				int pseudoColumn = getTableModel().findColumn(consider);
				if (pseudoColumn == -1) {
					showErrorMessage("Column '"+consider+"' not found.");
					return false;
					}
				}
			}
		return super.isViewConfigurationValid(view, configuration);
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		if (!(view instanceof VisualizationPanel2D))
			return;

		super.applyConfiguration(view, configuration, isAdjusting);

		JVisualization2D v2D = (JVisualization2D)((VisualizationPanel)view).getVisualization();

		String consider = configuration.getProperty(PROPERTY_CONSIDER, PROPERTY_CONSIDER_VISIBLE);
		if (consider.equals(PROPERTY_CONSIDER_VISIBLE))
			v2D.setBackgroundColorConsidered(JVisualization2D.BACKGROUND_VISIBLE_RECORDS);
		else if (consider.equals(PROPERTY_CONSIDER_ALL))
			v2D.setBackgroundColorConsidered(JVisualization2D.BACKGROUND_ALL_RECORDS);
		else
			v2D.setBackgroundColorConsidered(CompoundTableListHandler.convertToListIndex(getTableModel().findColumn(consider)));

		try {
			v2D.setBackgroundColorRadius(Math.max(1, Integer.parseInt(configuration.getProperty(PROPERTY_RADIUS, "10"))));
			}
		catch (NumberFormatException nfe) {}

		try {
			v2D.setBackgroundColorFading(Math.max(1, Integer.parseInt(configuration.getProperty(PROPERTY_FADING, "10"))));
			}
		catch (NumberFormatException nfe) {}
		}
	}
