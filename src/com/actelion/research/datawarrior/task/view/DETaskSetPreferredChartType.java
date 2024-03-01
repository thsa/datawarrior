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
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.JVisualization2D;
import com.actelion.research.table.view.VisualizationPanel;
import com.actelion.research.table.view.chart.ChartType;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;


public class DETaskSetPreferredChartType extends DETaskAbstractSetViewOptions {
	public static final String TASK_NAME = "Set Preferred Chart Type";

	private static final String PROPERTY_TYPE = "type";
	private static final String PROPERTY_MODE = "mode";
	private static final String PROPERTY_COLUMN = "column";
	private static final String PROPERTY_EDGE_SMOOTHING = "smoothing";


	private JComboBox	mComboBoxType,mComboBoxColumn,mComboBoxMode;
	private JLabel		mLabelSizeBy,mLabelColumn;
	private JSlider     mSliderEdgeSmoothing;
	
	public DETaskSetPreferredChartType(Frame owner, DEMainPane mainPane, VisualizationPanel view) {
		super(owner, mainPane, view);
		}
	
	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return false;
		}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof VisualizationPanel) ? null : "Preferred chart types can only be chosen in 2D- or 3D-Views.";
		}

	@Override
	public JComponent createViewOptionContent() {
		JPanel p1 = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };
		p1.setLayout(new TableLayout(size));

		p1.add(new JLabel("Preferred Chart Type:"), "1,1");
		mComboBoxType = new JComboBox();
		if (hasInteractiveView())
			for (int type: getInteractiveVisualization().getSupportedChartTypes())
				mComboBoxType.addItem(ChartType.TYPE_NAME[type]);
		else
			for (String name:ChartType.TYPE_NAME)
				mComboBoxType.addItem(name);
		mComboBoxType.addItemListener(this);
		p1.add(mComboBoxType, "3,1");

		mLabelSizeBy = new JLabel("Bar/Pie size by:");
		p1.add(mLabelSizeBy, "1,3");
		mComboBoxMode = new JComboBox(ChartType.MODE_NAME);
		mComboBoxMode.addItemListener(this);
		p1.add(mComboBoxMode, "3,3");

		mLabelColumn = new JLabel("of");
		p1.add(mLabelColumn, "5,3");
		mComboBoxColumn = new JComboBox();
		for (int column=0; column<getTableModel().getTotalColumnCount(); column++)
			if (columnQualifies(column))
				mComboBoxColumn.addItem(getTableModel().getColumnTitle(column));
		mComboBoxColumn.setEditable(!hasInteractiveView());
		mComboBoxColumn.addItemListener(this);
		p1.add(mComboBoxColumn, "7,3");

		mSliderEdgeSmoothing = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
//		mSliderEdgeSmoothing.setPreferredSize(new Dimension(HiDPIHelper.scale(150), mSliderEdgeSmoothing.getPreferredSize().height));
		mSliderEdgeSmoothing.addChangeListener(this);
		p1.add(new JLabel("Edge Smoothing:"), "1,5");
		p1.add(mSliderEdgeSmoothing, "3,5");

		mComboBoxMode.setEnabled(!hasInteractiveView() || mComboBoxColumn.getItemCount() != 0);

		return p1;
		}

	private boolean columnQualifies(int column) {
		return getTableModel().isColumnTypeDouble(column);
		}

	@Override
	public void setDialogToDefault() {
        mComboBoxType.setSelectedIndex(0);
        mComboBoxMode.setSelectedIndex(0);
        if (mComboBoxColumn.getItemCount() != 0)
        	mComboBoxColumn.setSelectedIndex(0);
		mSliderEdgeSmoothing.setValue(50);
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		int type = findListIndex(configuration.getProperty(PROPERTY_TYPE), ChartType.TYPE_CODE, ChartType.cTypeScatterPlot);
		mComboBoxType.setSelectedItem(ChartType.TYPE_NAME[type]);

		int mode = findListIndex(configuration.getProperty(PROPERTY_MODE), ChartType.MODE_CODE, ChartType.cModeCount);
		mComboBoxMode.setSelectedItem(ChartType.MODE_NAME[mode]);

		if (mode != ChartType.cModeCount && mode != ChartType.cModePercent) {
			String columnName = configuration.getProperty(PROPERTY_COLUMN, "<column name>");
			int column = getTableModel().findColumn(columnName);
			mComboBoxColumn.setSelectedItem(!hasInteractiveView() && column == -1 ? columnName : getTableModel().getColumnTitleExtended(column));
			}

		String value = configuration.getProperty(PROPERTY_EDGE_SMOOTHING);
		float smoothing = (value == null) ? JVisualization2D.DEFAULT_EDGE_SMOOTHING : Float.parseFloat(value);
		mSliderEdgeSmoothing.setValue(Math.round(100*smoothing));
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		int type = findListIndex((String)mComboBoxType.getSelectedItem(), ChartType.TYPE_NAME, ChartType.cTypeScatterPlot);
		configuration.setProperty(PROPERTY_TYPE, ChartType.TYPE_CODE[type]);
		if (ChartType.supportsProportionalFractions(type)) {
			int mode = mComboBoxMode.getSelectedIndex();
			configuration.setProperty(PROPERTY_MODE, ChartType.MODE_CODE[mode]);
			if (mode != ChartType.cModeCount && mode != ChartType.cModePercent && mComboBoxColumn.getItemCount() != 0)
				configuration.setProperty(PROPERTY_COLUMN, ""+getTableModel().getColumnTitleNoAlias((String)mComboBoxColumn.getSelectedItem()));
			}
		if (ChartType.supportsEdgeSmoothing(type)) {
			float smoothing = (float)mSliderEdgeSmoothing.getValue()/100f;
			configuration.setProperty(PROPERTY_EDGE_SMOOTHING, ""+smoothing);
			}
		}

	@Override
	public void addViewConfiguration(CompoundTableView view, Properties configuration) {
		JVisualization visualization = ((VisualizationPanel)view).getVisualization();
		int type = visualization.getPreferredChartType();
		configuration.setProperty(PROPERTY_TYPE, ChartType.TYPE_CODE[type]);
		if (ChartType.supportsProportionalFractions(type)) {
			int mode = visualization.getPreferredChartMode();
			configuration.setProperty(PROPERTY_MODE, ChartType.MODE_CODE[mode]);
			if (mode != ChartType.cModeCount && mode != ChartType.cModePercent)
				configuration.setProperty(PROPERTY_COLUMN, ""+getTableModel().getColumnTitleNoAlias(visualization.getPreferredChartColumn()));
			}
		if (ChartType.supportsEdgeSmoothing(type))
			configuration.setProperty(PROPERTY_EDGE_SMOOTHING, DoubleFormat.toString(((JVisualization2D)visualization).getEdgeSmoothing()));
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		int type = findListIndex(configuration.getProperty(PROPERTY_TYPE), ChartType.TYPE_CODE, ChartType.cTypeScatterPlot);
		if (ChartType.supportsProportionalFractions(type)) {
			int mode = findListIndex(configuration.getProperty(PROPERTY_MODE), ChartType.MODE_CODE, ChartType.cModeCount);
			if (mode != ChartType.cModeCount && mode != ChartType.cModePercent) {
				String columnName = configuration.getProperty(PROPERTY_COLUMN);
				if (columnName == null) {
					showErrorMessage("No numerical column available or defined.");
					return false;
					}
				if (view != null) {
					int column = view.getTableModel().findColumn(columnName);
					if (column == -1) {
						showErrorMessage("Column '"+columnName+"' not found.");
						return false;
						}
					if (!columnQualifies(column)) {
						showErrorMessage("Column '"+columnName+"' is not numerical.");
						return false;
						}
					}
				}
			}

		return true;
		}

	@Override
	public void enableItems() {
		int type = ConfigurableTask.findListIndex((String)mComboBoxType.getSelectedItem(), ChartType.TYPE_NAME, -1);
		boolean supportsSizeBy = ChartType.supportsProportionalFractions(type);
		boolean columnEnabled = supportsSizeBy && mComboBoxMode.getSelectedIndex() != ChartType.cModeCount && mComboBoxMode.getSelectedIndex() != ChartType.cModePercent;
		mLabelSizeBy.setEnabled(supportsSizeBy);
		mComboBoxMode.setEnabled(supportsSizeBy);
		mLabelColumn.setEnabled(columnEnabled);
		mComboBoxColumn.setEnabled(columnEnabled);
		mSliderEdgeSmoothing.setEnabled(ChartType.supportsEdgeSmoothing(type));
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		if (view instanceof VisualizationPanel) {
			JVisualization visualization = ((VisualizationPanel)view).getVisualization();
			int type = findListIndex(configuration.getProperty(PROPERTY_TYPE), ChartType.TYPE_CODE, ChartType.cTypeScatterPlot);
			int mode = ChartType.cModeCount;
			int column = -1;
			if (ChartType.supportsProportionalFractions(type)) {
				mode = findListIndex(configuration.getProperty(PROPERTY_MODE), ChartType.MODE_CODE, ChartType.cModeCount);
				if (mode != ChartType.cModeCount && mode != ChartType.cModePercent)
					column = getTableModel().findColumn(configuration.getProperty(PROPERTY_COLUMN));
				}
			visualization.setPreferredChartType(type, mode, column);
			String smoothing = configuration.getProperty(PROPERTY_EDGE_SMOOTHING);
			((JVisualization2D)visualization).setEdgeSmoothing(smoothing != null ? Float.parseFloat(smoothing) : JVisualization2D.DEFAULT_EDGE_SMOOTHING);
			}
		}
	}
