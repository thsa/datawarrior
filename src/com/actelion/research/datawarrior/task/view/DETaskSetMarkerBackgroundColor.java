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
import com.actelion.research.util.DoubleFormat;
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

	private static final int SLIDER_MAX = 100;
	private static final double VALUE_MAX = 20.0;  // must stay 20 to keep it compatible with views
	private static final int SLIDER_TICK_S_SPACING = 5;
	private static final int SLIDER_TICK_L_SPACING = 25;
	private static final double SLIDER_SENSITIVITY = 1.5;

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
		return (view instanceof VisualizationPanel2D) ? null : "Marker background colors can be used in 2D/3D-Views only.";
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
		double size[][] = { {TableLayout.FILL, TableLayout.PREFERRED, TableLayout.FILL, TableLayout.PREFERRED, TableLayout.FILL},
				 			{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };
		JPanel sliderpanel = new JPanel();
		sliderpanel.setLayout(new TableLayout(size));
		mSliderRadius = new JSlider(0, SLIDER_MAX, SLIDER_MAX/2);
		mSliderRadius.addChangeListener(this);
		mSliderRadius.setMinorTickSpacing(SLIDER_TICK_S_SPACING);
		mSliderRadius.setMajorTickSpacing(SLIDER_TICK_L_SPACING);
		mSliderRadius.setPaintTicks(true);
		mSliderRadius.setPreferredSize(new Dimension(HiDPIHelper.scale(160), HiDPIHelper.scale(42)));
		sliderpanel.add(new JLabel("Radius:"), "1,1");
		sliderpanel.add(mSliderRadius, "1,3");

		mSliderFading = new JSlider(0, SLIDER_MAX, SLIDER_MAX/2);
		mSliderFading.addChangeListener(this);
		mSliderFading.setMinorTickSpacing(SLIDER_TICK_S_SPACING);
		mSliderFading.setMajorTickSpacing(SLIDER_TICK_L_SPACING);
		mSliderFading.setPaintTicks(true);
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

		double radius = VALUE_MAX/2;
		try {
			radius = Double.parseDouble(configuration.getProperty(PROPERTY_RADIUS, ""));
			}
		catch (NumberFormatException nfe) {}
		mSliderRadius.setValue(radiusValueToSlider(radius));

		double fading = VALUE_MAX/2;
		try {
			fading = Double.parseDouble(configuration.getProperty(PROPERTY_FADING, ""));
			}
		catch (NumberFormatException nfe) {}
		mSliderFading.setValue(fadingValueToSlider(VALUE_MAX-fading));
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
		configuration.setProperty(PROPERTY_RADIUS, DoubleFormat.toString(radiusSliderToValue(mSliderRadius.getValue())));
		configuration.setProperty(PROPERTY_FADING, DoubleFormat.toString(fadingSliderToValue(SLIDER_MAX-mSliderFading.getValue())));
		}

	private double radiusSliderToValue(int sliderValue) {
		return VALUE_MAX * Math.pow((double)sliderValue / SLIDER_MAX, SLIDER_SENSITIVITY);
		}

	private int radiusValueToSlider(double value) {
		return (int)Math.round(SLIDER_MAX * Math.pow(value / VALUE_MAX, 1.0 / SLIDER_SENSITIVITY));
		}

	private double fadingSliderToValue(int sliderValue) {
		return VALUE_MAX * sliderValue / SLIDER_MAX;
		}

	private int fadingValueToSlider(double value) {
		return (int)Math.round(SLIDER_MAX * value / VALUE_MAX);
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
		configuration.setProperty(PROPERTY_RADIUS, DoubleFormat.toString(visualization.getBackgroundColorRadius()));
		configuration.setProperty(PROPERTY_FADING, DoubleFormat.toString(visualization.getBackgroundColorFading()));
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
			v2D.setBackgroundColorRadius(Float.parseFloat(configuration.getProperty(PROPERTY_RADIUS, "")));
			}
		catch (NumberFormatException nfe) {}

		try {
			v2D.setBackgroundColorFading(Float.parseFloat(configuration.getProperty(PROPERTY_FADING, "")));
			}
		catch (NumberFormatException nfe) {}
		}
	}
