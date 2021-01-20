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
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationPanel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2013
 * Company:
 * @author
 * @version 1.0
 */

public class DETaskSeparateCases extends DETaskAbstractSetViewOptions {
	public static final String TASK_NAME = "Separate Cases";

	private static final String PROPERTY_COLUMN = "column";
	private static final String PROPERTY_AMOUNT = "amount";

	private JSlider         mSlider;
    private JComboBox		mComboBox;

    public DETaskSeparateCases(Frame owner, DEMainPane mainPane, VisualizationPanel view) {
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
		return (view instanceof VisualizationPanel) ? null : "Cases can only be separated in 2D- and 3D-Views.";
		}

	@Override
	public OTHER_VIEWS getOtherViewMode() {
		return OTHER_VIEWS.GRAPHICAL2D;
		}

	@Override
	public JComponent createViewOptionContent() {
		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8} };

		JPanel sp = new JPanel();
		sp.setLayout(new TableLayout(size));

		mComboBox = new JComboBox();
		mComboBox.addItem(getTableModel().getColumnTitleExtended(-1));
        for (int column=0; column<getTableModel().getTotalColumnCount(); column++)
            if (columnQualifies(column))
            	mComboBox.addItem(getTableModel().getColumnTitle(column));
        mComboBox.setEditable(!hasInteractiveView());
        mComboBox.addItemListener(this);

		sp.add(new JLabel("Separate cases by"), "1,1");
		sp.add(mComboBox, "3,1");

		sp.add(new JLabel("Case distance:"), "1,3");

		mSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
		mSlider.setPreferredSize(new Dimension(HiDPIHelper.scale(120), mSlider.getPreferredSize().height));
		mSlider.setMinorTickSpacing(10);
		mSlider.setMajorTickSpacing(100);
		mSlider.setEnabled(mComboBox.getSelectedIndex() != 0);
		mSlider.addChangeListener(this);
		sp.add(mSlider, "3,3");

		return sp;
		}

	private boolean columnQualifies(int column) {
		return getTableModel().isColumnTypeCategory(column)
			&& getTableModel().getCategoryCount(column) <= JVisualization.cMaxCaseSeparationCategoryCount;
		}

	@Override
	public void setDialogToDefault() {
        mComboBox.setSelectedIndex(0);
		mSlider.setValue(50);
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		String columnName = configuration.getProperty(PROPERTY_COLUMN, CompoundTableModel.cColumnUnassignedCode);
		int column = getTableModel().findColumn(columnName);
		mComboBox.setSelectedItem(!hasInteractiveView() && column == -1 ? columnName : getTableModel().getColumnTitleExtended(column));

		float amount = 0.5f;
		try {
			amount = Float.parseFloat(configuration.getProperty(PROPERTY_AMOUNT, "0.5"));
			}
		catch (NumberFormatException nfe) {}
		mSlider.setValue((int)(100f*amount));
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		configuration.setProperty(PROPERTY_COLUMN, ""+getTableModel().getColumnTitleNoAlias((String)mComboBox.getSelectedItem()));
		configuration.setProperty(PROPERTY_AMOUNT, ""+(0.01f*mSlider.getValue()));
		}

	@Override
	public void addViewConfiguration(CompoundTableView view, Properties configuration) {
		JVisualization visualization = ((VisualizationPanel)view).getVisualization();
		configuration.setProperty(PROPERTY_COLUMN, ""+getTableModel().getColumnTitleNoAlias(getInteractiveVisualization().getCaseSeparationColumn()));
		configuration.setProperty(PROPERTY_AMOUNT, ""+ visualization.getCaseSeparationValue());
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		String columnName = configuration.getProperty(PROPERTY_COLUMN);
		if (!CompoundTableModel.cColumnUnassignedCode.equals(columnName)) {
			if (view != null) {
				int column = getTableModel().findColumn(columnName);
				if (column == -1) {
					showErrorMessage("Column '"+columnName+"' not found.");
					return false;
					}
				if (!columnQualifies(column)) {
					showErrorMessage("Column '"+columnName+"' does not contain categories or contains to many categories.");
					return false;
					}
				}
			}

		return true;
		}

	@Override
	public void enableItems() {
		mSlider.setEnabled(mComboBox.getSelectedIndex() != 0);
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		if (!(view instanceof VisualizationPanel))
			return;

		int column = getTableModel().findColumn(configuration.getProperty(PROPERTY_COLUMN, CompoundTableModel.cColumnUnassignedCode));

		float amount = 0.5f;
		try {
			amount = Float.parseFloat(configuration.getProperty(PROPERTY_AMOUNT, "0.5"));
			}
		catch (NumberFormatException nfe) {}

		((VisualizationPanel)view).getVisualization().setCaseSeparation(column, amount, isAdjusting);
		}
	}
