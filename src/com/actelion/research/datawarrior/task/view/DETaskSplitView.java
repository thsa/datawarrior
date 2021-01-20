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
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationPanel;
import com.actelion.research.table.view.VisualizationPanel2D;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;


public class DETaskSplitView extends DETaskAbstractSetViewOptions {
	public static final String TASK_NAME = "Split View By Categories";

	private static final String PROPERTY_COLUMN1 = "column1";
	private static final String PROPERTY_COLUMN2 = "column2";
	private static final String PROPERTY_ASPECT = "aspect";
	private static final String PROPERTY_SHOW_EMPTY_VIEWS = "showEmpty";

	private JComboBox	mComboBoxColumn1,mComboBoxColumn2;
	private JSlider		mSlider;
	private JCheckBox	mCheckBoxShowEmpty;
	private boolean		mHighMultiplicityAccepted;

	public DETaskSplitView(Frame owner, DEMainPane mainPane, VisualizationPanel view) {
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
		return (view instanceof VisualizationPanel2D) ? null : "Only 2D-Views can be split by categories.";
		}

	@Override
	public OTHER_VIEWS getOtherViewMode() {
		return OTHER_VIEWS.GRAPHICAL2D;
		}

	@Override
	public JComponent createViewOptionContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap} };
		JPanel p = new JPanel();
		p.setLayout(new TableLayout(size));

		mComboBoxColumn1 = new JComboBox();
		mComboBoxColumn2 = new JComboBox();
		mComboBoxColumn1.addItem(getTableModel().getColumnTitleExtended(-1));
		mComboBoxColumn2.addItem(getTableModel().getColumnTitleExtended(-1));
		for (int column=0; column<getTableModel().getTotalColumnCount(); column++) {
			if (columnQualifies(column)) {
				mComboBoxColumn1.addItem(getTableModel().getColumnTitle(column));
				mComboBoxColumn2.addItem(getTableModel().getColumnTitle(column));
				}
			}
		mComboBoxColumn1.addItem(CompoundTableListHandler.LIST_NAME_SELECTION);
		mComboBoxColumn2.addItem(CompoundTableListHandler.LIST_NAME_SELECTION);
		for (int i = 0; i<getTableModel().getListHandler().getListCount(); i++) {
			mComboBoxColumn1.addItem(getTableModel().getColumnTitleExtended(CompoundTableListHandler.getColumnFromList(i)));
			mComboBoxColumn2.addItem(getTableModel().getColumnTitleExtended(CompoundTableListHandler.getColumnFromList(i)));
			}
		if (!hasInteractiveView()) {
			mComboBoxColumn1.setEditable(true);
			mComboBoxColumn2.setEditable(true);
			}
		mComboBoxColumn1.addItemListener(this);
		mComboBoxColumn2.addItemListener(this);

		p.add(new JLabel("1st column:"), "1,1");
		p.add(mComboBoxColumn1, "3,1");
		p.add(new JLabel("2nd column:"), "1,3");
		p.add(mComboBoxColumn2, "3,3");

		p.add(new JLabel("View shape:"), "1,5");
		JPanel sp = new JPanel();
		mSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
		mSlider.addChangeListener(this);
		mSlider.setPreferredSize(new Dimension(HiDPIHelper.scale(120), mSlider.getPreferredSize().height));
		sp.add(new JLabel("narrow"));
		sp.add(mSlider);
		sp.add(new JLabel("wide"));
		p.add(sp, "3,5");

		mCheckBoxShowEmpty = new JCheckBox("Show empty categories");
		mCheckBoxShowEmpty.addActionListener(this);
		p.add(mCheckBoxShowEmpty, "3,7");

		return p;
		}

	private boolean columnQualifies(int column) {
		return getTableModel().isColumnTypeCategory(column);
		}

	@Override
	public void setDialogToDefault() {
		mComboBoxColumn1.setSelectedIndex(0);
		mComboBoxColumn2.setSelectedIndex(0);
		mSlider.setValue(50);
		mCheckBoxShowEmpty.setSelected(true);
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		String columnName1 = configuration.getProperty(PROPERTY_COLUMN1, CompoundTableModel.cColumnUnassignedCode);
		String columnName2 = configuration.getProperty(PROPERTY_COLUMN2, CompoundTableModel.cColumnUnassignedCode);
		int column1 = getTableModel().findColumn(columnName1);
		int column2 = getTableModel().findColumn(columnName2);
		mComboBoxColumn1.setSelectedItem(!hasInteractiveView() && column1 == -1 ? columnName1 : getTableModel().getColumnTitleExtended(column1));
		mComboBoxColumn2.setSelectedItem(!hasInteractiveView() && column2 == -1 ? columnName2 : getTableModel().getColumnTitleExtended(column2));

		float aspect = 1.0f;
		try {
			aspect = Float.parseFloat(configuration.getProperty(PROPERTY_ASPECT, "1.0"));
			}
		catch (NumberFormatException nfe) {}
		mSlider.setValue(Math.max(0, Math.min(100, (int)(0.5 + 50.0 * (1.0 + Math.log10(aspect))))));

		mCheckBoxShowEmpty.setSelected("true".equals(configuration.getProperty(PROPERTY_SHOW_EMPTY_VIEWS, "true")));
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		configuration.setProperty(PROPERTY_COLUMN1, ""+getTableModel().getColumnTitleNoAlias((String)mComboBoxColumn1.getSelectedItem()));
		configuration.setProperty(PROPERTY_COLUMN2, ""+getTableModel().getColumnTitleNoAlias((String)mComboBoxColumn2.getSelectedItem()));
		configuration.setProperty(PROPERTY_ASPECT, DoubleFormat.toString(Math.pow(10, 0.02 * (mSlider.getValue() - 50))));
		configuration.setProperty(PROPERTY_SHOW_EMPTY_VIEWS, mCheckBoxShowEmpty.isSelected() ? "true" : "false");
		}

	@Override
	public void addViewConfiguration(CompoundTableView view, Properties configuration) {
		JVisualization visualization = ((VisualizationPanel)view).getVisualization();
		int[] column = getInteractiveVisualization().getSplittingColumns();
		configuration.setProperty(PROPERTY_COLUMN1, ""+getTableModel().getColumnTitleNoAlias(column[0]));
		configuration.setProperty(PROPERTY_COLUMN2, ""+getTableModel().getColumnTitleNoAlias(column[1]));
		configuration.setProperty(PROPERTY_ASPECT, ""+ visualization.getSplittingAspectRatio());
		configuration.setProperty(PROPERTY_SHOW_EMPTY_VIEWS, visualization.isShowEmptyInSplitView() ? "true" : "false");

		// if an interactive view is configured to exceed split view limit, then we don't show a warning later
		mHighMultiplicityAccepted = (getMultiplicity(configuration) > JVisualization.cMaxSplitViewCount);
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		int multiplicity = getMultiplicity(configuration);
		if (multiplicity == 0)
			return false;

		// if interactive and split view count exceeds limit
		if (view != null
		 && multiplicity > JVisualization.cMaxSplitViewCount) {
			// don't allow exceeded views, if empty views are not skipped
			if ("true".equals(configuration.getProperty(PROPERTY_SHOW_EMPTY_VIEWS, "true"))) {
				showErrorMessage("Your column selection exceeds the limit of "+JVisualization.cMaxSplitViewCount+" individual views."
								+"\nDon't select 'Show empty views' and use filters to reduce visible categories.");
				return false;
				}

			// if we not have issued the warning before
			if (!mHighMultiplicityAccepted) {
				String message = "Your column selection exceeds the limit of "+JVisualization.cMaxSplitViewCount+" individual views."
							   + "\nTo actually split views you will need to use filters to hide some rows"
							   +"\nand fall below the limit. Do you want to continue?";
				if (JOptionPane.showConfirmDialog(getParentFrame(), message, "Split View Limit Exceeded", JOptionPane.OK_CANCEL_OPTION)
						!= JOptionPane.OK_OPTION)
					return false;

				mHighMultiplicityAccepted = true;
				}
			}

		return true;
		}

	private int getMultiplicity(Properties configuration) {
		return getMultiplicity(configuration.getProperty(PROPERTY_COLUMN1))
			 * getMultiplicity(configuration.getProperty(PROPERTY_COLUMN2));
		}

	private int getMultiplicity(String columnName) {
		if (!CompoundTableModel.cColumnUnassignedCode.equals(columnName)) {
			int column = getTableModel().findColumn(columnName);
			if (column == -1) {
				showErrorMessage("Column '"+columnName+"' not found.");
				return 0;
				}
			if (!CompoundTableListHandler.isListOrSelectionColumn(column) && !columnQualifies(column)) {
				showErrorMessage("Column '"+columnName+"' does not contain categories.");
				return 0;
				}
			return CompoundTableListHandler.isListOrSelectionColumn(column) ? 2 : getTableModel().getCategoryCount(column);
			}
		return 1;
		}

	@Override
	public void enableItems() {
		mSlider.setEnabled(mComboBoxColumn1.getSelectedIndex() == 0 || mComboBoxColumn2.getSelectedIndex() == 0);
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		if (view instanceof VisualizationPanel) {
			int column1 = getTableModel().findColumn(configuration.getProperty(PROPERTY_COLUMN1, CompoundTableModel.cColumnUnassignedCode));
			int column2 = getTableModel().findColumn(configuration.getProperty(PROPERTY_COLUMN2, CompoundTableModel.cColumnUnassignedCode));
			float aspect = 1.0f;
			try {
				aspect = Float.parseFloat(configuration.getProperty(PROPERTY_ASPECT, "1.0"));
				}
			catch (NumberFormatException nfe) {}
			boolean showEmptyViews = "true".equals(configuration.getProperty(PROPERTY_SHOW_EMPTY_VIEWS, "true"));
			((VisualizationPanel)view).getVisualization().setSplittingColumns(column1, column2, aspect, showEmptyViews);
			}
		}
	}
