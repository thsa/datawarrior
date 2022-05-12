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

package com.actelion.research.datawarrior.task.table;

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.DETableView;
import com.actelion.research.datawarrior.task.view.DETaskAbstractSetColor;
import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.VisualizationColor;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public abstract class DETaskAbstractSetTableColor extends DETaskAbstractSetColor {
	private static final String PROPERTY_COLUMN = "column";

    private int			mTableColumn,mColorType;
    private JComboBox	mComboBoxColumn;

	public DETaskAbstractSetTableColor(Frame owner,
									   DEMainPane mainPane,
									   DETableView view,
									   int tableColumn,
									   int colorType) {
		super(owner, mainPane, view,
				(colorType == CompoundTableColorHandler.FOREGROUND) ? "Set Text/Structure Color" : "Set Table Cell Background Color");
		mTableColumn = tableColumn;
		mColorType = colorType;

		super.initialize();	// this is a hack to run initialize() after setting mVisualizationColor.
		}

	@Override
	protected void initialize() {}

	@Override
	public VisualizationColor getVisualizationColor(CompoundTableView view) {
		return ((DETableView)view).getColorHandler().getVisualizationColor(mTableColumn, mColorType);
		}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof DETableView) ? null : "Text color and background can only be set on table views.";
		}

	@Override
	public JComponent createViewOptionContent() {
		JPanel p = (JPanel)super.createViewOptionContent();

		int selected = -1;
		mComboBoxColumn = new JComboBox();
		for (int column=0; column<getTableModel().getTotalColumnCount(); column++) {
			if (getTableModel().isColumnDisplayable(column)) {
				if (column == mTableColumn)
					selected = mComboBoxColumn.getItemCount();
				mComboBoxColumn.addItem(getTableModel().getColumnTitleExtended(column));
				}
			}
		if (selected != -1)
			mComboBoxColumn.setSelectedIndex(selected);
		else if (mComboBoxColumn.getItemCount() != 0)
			mComboBoxColumn.setSelectedIndex(0);
		mComboBoxColumn.setEditable(mTableColumn == -1);
		mComboBoxColumn.setEnabled(mTableColumn == -1);
		
		mComboBoxColumn.addItemListener(this);
		p.add(new JLabel("Table column:"), "1,3");
		p.add(mComboBoxColumn, "3,3");
		return p;
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		if (view instanceof DETableView)
			super.applyConfiguration(view, configuration, isAdjusting);
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		super.setDialogToConfiguration(configuration);
		if (!hasInteractiveView()) {
			int column = getTableModel().findColumn(configuration.getProperty(PROPERTY_COLUMN));
			if (column == -1)
				mComboBoxColumn.setSelectedItem(configuration.getProperty(PROPERTY_COLUMN));
			else
				mComboBoxColumn.setSelectedItem(getTableModel().getColumnTitle(column));
			}
		else if (mTableColumn != -1) {
			mComboBoxColumn.setSelectedItem(getTableModel().getColumnTitleExtended(mTableColumn));
			}
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		super.addDialogConfiguration(configuration);
		configuration.setProperty(PROPERTY_COLUMN, getTableModel().getColumnTitleNoAlias((String)mComboBoxColumn.getSelectedItem()));
		}

	@Override
	public void addViewConfiguration(CompoundTableView view, Properties configuration) {
		super.addViewConfiguration(view, configuration);
		configuration.setProperty(PROPERTY_COLUMN, getTableModel().getColumnTitleNoAlias(mTableColumn));
		}

	@Override
	public boolean isRedundant(Properties previousConfiguration, Properties currentConfiguration) {
		return super.isRedundant(previousConfiguration, currentConfiguration)
			&& previousConfiguration.getProperty(PROPERTY_COLUMN).equals(
			   currentConfiguration.getProperty(PROPERTY_COLUMN));
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		if (view != null) {
			int column = view.getTableModel().findColumn(configuration.getProperty(PROPERTY_COLUMN));
			if (column == -1) {
				showErrorMessage("Column '"+configuration.getProperty(PROPERTY_COLUMN)+"' not found.");
				return false;
				}
			}
		return super.isViewConfigurationValid(view, configuration);
		}

	@Override
	public VisualizationColor getVisualizationColor(CompoundTableView view, Properties configuration) {
		int column = view.getTableModel().findColumn(configuration.getProperty(PROPERTY_COLUMN));
		return ((DETableView)view).getColorHandler().getVisualizationColor(column, mColorType);
		}
	}
