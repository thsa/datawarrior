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
import com.actelion.research.gui.JPruningBar;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationPanel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Properties;

public class DETaskAssignOrZoomAxes extends DETaskAbstractSetViewOptions {
	public static final String TASK_NAME = "Assign Or Zoom Axes";

	private static final String PROPERTY_COLUMN = "column";
	private static final String PROPERTY_LOW = "high";
	private static final String PROPERTY_HIGH = "low";
	private static final String PROPERTY_MILLIS = "millis";	// if it is animated

	private JPruningBar[]		mPruningBar;
    private JComboBox[]			mComboBoxColumn;
    private JCheckBox			mCheckBoxAnimated;

    public DETaskAssignOrZoomAxes(Frame owner, DEMainPane mainPane, VisualizationPanel view) {
		super(owner, mainPane, view, true);
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
		return (view instanceof VisualizationPanel) ? null : "Axes only exist in 2D- or 3D-Views.";
		}

	@Override
	public JComponent createViewOptionContent() {
		mPruningBar = new JPruningBar[3];
		for (int axis=0; axis<3; axis++) {
			mPruningBar[axis] = new JPruningBar(true, axis);
			mPruningBar[axis].setMinAndMax(0.0f, 1.0f);
			}

		String[] columnList = getQualifyingColumns();
		mComboBoxColumn = new JComboBox[3];
		for (int axis=0; axis<3; axis++) {
            mComboBoxColumn[axis] = new JComboBox(columnList);
            mComboBoxColumn[axis].setEditable(!hasInteractiveView());
			}

		mCheckBoxAnimated = new JCheckBox("Animated zooming", true);
		mCheckBoxAnimated.setHorizontalAlignment(SwingConstants.CENTER);

		double[][] size = { {8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED,
							16, TableLayout.PREFERRED, 4, TableLayout.PREFERRED,
							16, TableLayout.PREFERRED, 4, TableLayout.PREFERRED,
							12, TableLayout.PREFERRED, 4} };
		JPanel p = new JPanel();
		p.setLayout(new TableLayout(size));

		for (int axis=0; axis<3; axis++) {
			p.add(mComboBoxColumn[axis], "1,"+(1+4*axis));
			p.add(mPruningBar[axis], "1,"+(3+4*axis));
			}
		p.add(mCheckBoxAnimated, "1,13");

		return p;
		}

	private String[] getQualifyingColumns() {
		ArrayList<String> columnList = new ArrayList<String>();
		for (int i=0; i<getTableModel().getTotalColumnCount(); i++)
			if (getTableModel().hasNumericalVariance(i))
				columnList.add(getTableModel().getColumnTitleExtended(i));

		for (int i=0; i<getTableModel().getTotalColumnCount(); i++)
            if (getTableModel().isDescriptorColumn(i))
				columnList.add(getTableModel().getColumnTitleExtended(i));

		columnList.add(VisualizationPanel.UNASSIGNED_TEXT);

		return columnList.toArray(new String[0]);
		}

	@Override
	public void addViewConfiguration(CompoundTableView view, Properties configuration) {
		VisualizationPanel vp = (VisualizationPanel)view;
		for (int i=0; i<vp.getDimensionCount(); i++) {
			if (vp.getSelectedColumn(i) != JVisualization.cColumnUnassigned) {
				configuration.setProperty(PROPERTY_COLUMN+i, vp.getAxisColumnName(i));
				configuration.setProperty(PROPERTY_LOW+i, ""+ vp.getPruningBar(i).getLowValue());
				configuration.setProperty(PROPERTY_HIGH+i, ""+ vp.getPruningBar(i).getHighValue());
				}
			}
		configuration.setProperty(PROPERTY_MILLIS, "1000");
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		for (int i=0; i<3; i++) {
			if (!VisualizationPanel.UNASSIGNED_TEXT.equals(mComboBoxColumn[i].getSelectedItem())) {
				configuration.setProperty(PROPERTY_COLUMN+i, getTableModel().getColumnTitleNoAlias((String)mComboBoxColumn[i].getSelectedItem()));
				configuration.setProperty(PROPERTY_LOW+i, ""+mPruningBar[i].getLowValue());
				configuration.setProperty(PROPERTY_HIGH+i, ""+mPruningBar[i].getHighValue());
				}
			}
		configuration.setProperty(PROPERTY_MILLIS, mCheckBoxAnimated.isSelected() ? "1000" : "0");
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		for (int i=0; i<3; i++) {
			mComboBoxColumn[i].setSelectedItem(configuration.getProperty(PROPERTY_COLUMN+i, VisualizationPanel.UNASSIGNED_TEXT));
			float low = Float.parseFloat(configuration.getProperty(PROPERTY_LOW+i, "0.0"));
			float high = Float.parseFloat(configuration.getProperty(PROPERTY_HIGH+i, "1.0"));
			mPruningBar[i].setLowAndHigh(low, high, true);
			}
		mCheckBoxAnimated.setSelected(!configuration.getProperty(PROPERTY_MILLIS, "1000").equals("0"));
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		if (view != null) {
			for (int i=0; i<3; i++) {
				String columnName = configuration.getProperty(PROPERTY_COLUMN+i);
				if (columnName != null) {
					int column = getTableModel().findColumn(columnName);
					if (column == -1) {
						showErrorMessage("Column '"+columnName+"' not found.");
						return false;
						}
					if (!getTableModel().isDescriptorColumn(column)
					 && !getTableModel().hasNumericalVariance(column)) {
						showErrorMessage("Column '"+columnName+"' is neither a descriptor nor does it have a numerical variance.");
						return false;
						}
					}
				}
			}

		return true;
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		final VisualizationPanel vp = (VisualizationPanel)view;
		final int dimensions = vp.getDimensionCount();
		for (int i=0; i<dimensions; i++) {
			String columnName = configuration.getProperty(PROPERTY_COLUMN+i);
			if (columnName == null)
				vp.setAxisColumnName(i, VisualizationPanel.UNASSIGNED_TEXT);
			else
				vp.setAxisColumnName(i, columnName);
			}

		final float[] l2 = new float[dimensions];
		final float[] h2 = new float[dimensions];
		for (int i=0; i<dimensions; i++) {
			l2[i] = Float.parseFloat(configuration.getProperty(PROPERTY_LOW+i, "0.0"));
			h2[i] = Float.parseFloat(configuration.getProperty(PROPERTY_HIGH+i, "1.0"));
			}

		final long totalMillis = Long.parseLong(configuration.getProperty(PROPERTY_MILLIS, "1000"));
		if (totalMillis >= 100) {
			// we need to animate and also need to know the start values
			float[] l1 = new float[dimensions];
			float[] h1 = new float[dimensions];
			for (int i=0; i<dimensions; i++) {
				l1[i] = vp.getPruningBar(i).getLowValue();
				h1[i] = vp.getPruningBar(i).getHighValue();
				}

			long millis = System.currentTimeMillis();
			long startMillis = millis - 20;	// assume 20 millis for the calculation of first frame
			long endMillis = startMillis + totalMillis;
			final float[] low = new float[dimensions];
			final float[] high = new float[dimensions];
			while (millis < endMillis) {
				float progress = (1.0f - (float)Math.cos(Math.PI * (millis - startMillis) / totalMillis)) / 2.0f;
				for (int i=0; i<dimensions; i++) {
					low[i]  = l1[i] + (l2[i] - l1[i]) * progress;
					high[i] = h1[i] + (h2[i] - h1[i]) * progress;
					}
				try {
					SwingUtilities.invokeAndWait(() -> vp.setZoom(low, high, true, false));
					}
				catch (Exception e) {}
				millis = System.currentTimeMillis();
				}
			}

		try {
			SwingUtilities.invokeAndWait(() -> vp.setZoom(l2, h2, false, false));
			}
		catch (Exception e) {}
		}

	@Override
	public void enableItems() {
		}

	@Override
	public void setDialogToDefault() {
		for (int i=0; i<3; i++) {
			mComboBoxColumn[i].setSelectedItem(VisualizationPanel.UNASSIGNED_TEXT);
			mPruningBar[i].setLowAndHigh(0.0f, 1.0f, false);
			}
		mCheckBoxAnimated.setSelected(true);
		}
	}
