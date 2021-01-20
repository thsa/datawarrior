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

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationPanel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.util.Properties;


public class DETaskSetConnectionLines extends DETaskAbstractSetViewOptions {
	public static final String TASK_NAME = "Set Connection Lines";

	private static final String PROPERTY_CONNECTION = "column";
    private static final String PROPERTY_CONNECTION_CODE_NONE = "<none>";
    private static final String PROPERTY_CONNECTION_CODE_ALL = "<all>";
    private static final String PROPERTY_CONNECTION_CODE_CASES = "<cases>";
    private static final String ITEM_CONNECTION_NONE = "<No connection lines>";
    private static final String ITEM_CONNECTION_ALL = "<Don't group, connect all>";
    private static final String ITEM_CONNECTION_CASES = "<Main cases>";

    private static final String PROPERTY_ORDER = "order";
    private static final String PROPERTY_ORDER_CODE_X_AXIS = "xAxis";
    private static final String ITEM_ORDER_X_AXIS = "<X-axis>";

    private static final String PROPERTY_RADIUS = "radius";
    private static final int DEFAULT_RADIUS = 5;
    private static final String PROPERTY_LINE_WIDTH = "lineWidth";
    private static final float DEFAULT_LINE_WIDTH = 1.0f;

	private static final String PROPERTY_INVERT_ARROWS = "invertArrows";
    private static final String PROPERTY_TREE_VIEW = "treeView";
    private static final String PROPERTY_SHOW_ALL = "showAll";
    private static final String PROPERTY_DYNAMIC = "dynamic";
	private static final String PROPERTY_INVERT_TREE = "invert";

	JSlider             mSliderLineWidth,mSliderRadius;
	JComboBox			mComboBox1,mComboBox2,mComboBox3;
	JCheckBox			mCheckBoxInvertArrows,mCheckBoxShowAll,mCheckBoxIsDynamic,mCheckBoxInvertTree;
	JLabel				mLabelLevels,mLabelRadius;

	public DETaskSetConnectionLines(Frame owner, DEMainPane mainPane, VisualizationPanel view) {
		super(owner, mainPane, view);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/views.html#ConnectionLines";
		}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return false;
		}

	@Override
	public OTHER_VIEWS getOtherViewMode() {
		return OTHER_VIEWS.GRAPHICAL;
		}

	@Override
	public JComponent createViewOptionContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap },
    						{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap/2,
								TableLayout.PREFERRED, gap*3, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap/4,
								TableLayout.PREFERRED, gap/4, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap } };
		JPanel cp = new JPanel();
		cp.setLayout(new TableLayout(size));

		mComboBox1 = new JComboBox();
		mComboBox1.addItem(ITEM_CONNECTION_NONE);
		if (getInteractiveVisualization() == null
		 || getInteractiveVisualization().getChartType() == JVisualization.cChartTypeBoxPlot
		 || getInteractiveVisualization().getChartType() == JVisualization.cChartTypeWhiskerPlot) {
			mComboBox1.addItem(ITEM_CONNECTION_CASES);
			}
		if (getInteractiveVisualization() == null
		 || (getInteractiveVisualization().getChartType() != JVisualization.cChartTypeBoxPlot
		  && getInteractiveVisualization().getChartType() != JVisualization.cChartTypeWhiskerPlot)) {
			mComboBox1.addItem(ITEM_CONNECTION_ALL);
			}
		for (int i=0; i<getTableModel().getTotalColumnCount(); i++)
			if (getTableModel().isColumnTypeCategory(i)
			 || getTableModel().getColumnProperty(i, CompoundTableConstants.cColumnPropertyReferencedColumn) != null)
				mComboBox1.addItem(getTableModel().getColumnTitle(i));
		mComboBox1.setEditable(!hasInteractiveView());
		mComboBox1.addItemListener(this);
		cp.add(new JLabel("Group & connect by: "), "1,1");
		cp.add(mComboBox1, "3,1,5,1");

		mComboBox2 = new JComboBox();
		mComboBox2.addItem(ITEM_ORDER_X_AXIS);
		for (int i=0; i<getTableModel().getTotalColumnCount(); i++) {
		    if (getTableModel().isColumnTypeCategory(i)
		     || getTableModel().isColumnTypeDouble(i)
		     || getTableModel().isColumnTypeDate(i)) {
				mComboBox2.addItem(getTableModel().getColumnTitle(i));
				}
			}
		mComboBox2.setEditable(!hasInteractiveView());
		mComboBox2.addItemListener(this);
		cp.add(new JLabel("Connection order by: "), "1,3");
		cp.add(mComboBox2, "3,3,5,3");

		mSliderLineWidth = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
//		mSliderLineWidth.setMinorTickSpacing(10);
//		mSliderLineWidth.setMajorTickSpacing(100);
		mSliderLineWidth.setPreferredSize(new Dimension(HiDPIHelper.scale(100), mSliderLineWidth.getPreferredSize().height));
		mSliderLineWidth.addChangeListener(this);
		cp.add(new JLabel("Relative line width:"), "1,5");
		cp.add(mSliderLineWidth, "3,5");

		mCheckBoxInvertArrows = new JCheckBox("Invert arrow direction (directed graphs only)");
		mCheckBoxInvertArrows.addActionListener(this);
		cp.add(mCheckBoxInvertArrows, "1,7,5,7");

		mComboBox3 = new JComboBox(JVisualization.TREE_VIEW_MODE_NAME);
    	mComboBox3.addItemListener(this);
		cp.add(new JLabel("Detail tree mode: "), "1,9");
		cp.add(mComboBox3, "3,9,5,9");

		mCheckBoxShowAll = new JCheckBox("Show all markers if no tree root (reference row) is chosen");
		mCheckBoxShowAll.addActionListener(this);
		cp.add(mCheckBoxShowAll, "1,11,5,11");

		mCheckBoxIsDynamic = new JCheckBox("If nodes get invisible, re-arrange and remove sub-branches");
		mCheckBoxIsDynamic.addActionListener(this);
		cp.add(mCheckBoxIsDynamic, "1,13,5,13");

		mCheckBoxInvertTree = new JCheckBox("Invert tree direction (directed graphs only)");
		mCheckBoxInvertTree.addActionListener(this);
		cp.add(mCheckBoxInvertTree, "1,15,5,15");

		mSliderRadius = new JSlider(JSlider.HORIZONTAL, 0, 20, DEFAULT_RADIUS);
//		mSliderRadius.setMinorTickSpacing(1);
//		mSliderRadius.setMajorTickSpacing(5);
		mSliderRadius.setPreferredSize(new Dimension(HiDPIHelper.scale(100), mSliderRadius.getPreferredSize().height));
		mSliderRadius.addChangeListener(this);
		mLabelLevels = new JLabel("Detail tree layers:");
		cp.add(mLabelLevels, "1,17");
		cp.add(mSliderRadius, "3,17");
		mLabelRadius = new JLabel(""+DEFAULT_RADIUS);
		mLabelRadius.setPreferredSize(new Dimension(HiDPIHelper.scale(32), mLabelRadius.getPreferredSize().height));
		cp.add(mLabelRadius, "5,17");

		return cp;
	    }

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof VisualizationPanel) ? null : "Connection lines can only be shown in 2D- and 3D-Views.";
		}

	@Override
	public void setDialogToDefault() {
		mComboBox1.setSelectedItem(ITEM_CONNECTION_NONE);
		mComboBox2.setSelectedItem(ITEM_ORDER_X_AXIS);
		mComboBox3.setSelectedItem(JVisualization.cTreeViewModeNone);
		mSliderRadius.setValue(DEFAULT_RADIUS);
		mCheckBoxInvertArrows.setSelected(false);
		mSliderLineWidth.setValue((int)(50.0*Math.sqrt(DEFAULT_LINE_WIDTH)));
		mCheckBoxShowAll.setSelected(true);
		mCheckBoxIsDynamic.setSelected(false);
		mCheckBoxInvertTree.setSelected(false);
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		String connection = configuration.getProperty(PROPERTY_CONNECTION, PROPERTY_CONNECTION_CODE_NONE);
		if (connection.equals(PROPERTY_CONNECTION_CODE_NONE))
			mComboBox1.setSelectedItem(ITEM_CONNECTION_NONE);
		else if (connection.equals(PROPERTY_CONNECTION_CODE_ALL))
			mComboBox1.setSelectedItem(ITEM_CONNECTION_ALL);
		else if (connection.equals(PROPERTY_CONNECTION_CODE_CASES))
			mComboBox1.setSelectedItem(ITEM_CONNECTION_CASES);
		else {
			int column = getTableModel().findColumn(connection);
			mComboBox1.setSelectedItem(!hasInteractiveView() && column == -1 ? connection : getTableModel().getColumnTitle(column));
			}

		String order = configuration.getProperty(PROPERTY_ORDER, PROPERTY_ORDER_CODE_X_AXIS);
		if (order.equals(PROPERTY_ORDER_CODE_X_AXIS))
			mComboBox2.setSelectedItem(ITEM_ORDER_X_AXIS);
		else {
			int column = getTableModel().findColumn(order);
			mComboBox2.setSelectedItem(!hasInteractiveView() && column == -1 ? order : getTableModel().getColumnTitle(column));
			}

		mComboBox3.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_TREE_VIEW),
					JVisualization.TREE_VIEW_MODE_CODE, JVisualization.cTreeViewModeNone));

		mCheckBoxInvertArrows.setSelected(configuration.getProperty(PROPERTY_INVERT_ARROWS, "false").equals("true"));
		mCheckBoxShowAll.setSelected(configuration.getProperty(PROPERTY_SHOW_ALL, "true").equals("true"));
		mCheckBoxIsDynamic.setSelected(configuration.getProperty(PROPERTY_DYNAMIC, "false").equals("true"));
		mCheckBoxInvertTree.setSelected(configuration.getProperty(PROPERTY_INVERT_TREE, "false").equals("true"));

		int radius = DEFAULT_RADIUS;
		try {
			radius = Integer.parseInt(configuration.getProperty(PROPERTY_RADIUS, ""+DEFAULT_RADIUS));
			}
		catch (NumberFormatException nfe) {}
		mSliderRadius.setValue(radius);

		float lineWidth = DEFAULT_LINE_WIDTH;
		try {
			lineWidth = Float.parseFloat(configuration.getProperty(PROPERTY_LINE_WIDTH, ""+DEFAULT_LINE_WIDTH));
			}
		catch (NumberFormatException nfe) {}
		mSliderLineWidth.setValue((int)(50.0*Math.sqrt(lineWidth)));
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		String connection = (String)mComboBox1.getSelectedItem();
		if (connection.equals(ITEM_CONNECTION_NONE))
			configuration.setProperty(PROPERTY_CONNECTION, PROPERTY_CONNECTION_CODE_NONE);
		else if (connection.equals(ITEM_CONNECTION_ALL))
			configuration.setProperty(PROPERTY_CONNECTION, PROPERTY_CONNECTION_CODE_ALL);
		else if (connection.equals(ITEM_CONNECTION_CASES))
			configuration.setProperty(PROPERTY_CONNECTION, PROPERTY_CONNECTION_CODE_CASES);
		else
			configuration.setProperty(PROPERTY_CONNECTION, getTableModel().getColumnTitleNoAlias(connection));

		String order = (String)mComboBox2.getSelectedItem();
		if (order.equals(ITEM_ORDER_X_AXIS))
			configuration.setProperty(PROPERTY_ORDER, PROPERTY_ORDER_CODE_X_AXIS);
		else
			configuration.setProperty(PROPERTY_ORDER, getTableModel().getColumnTitleNoAlias(order));

		configuration.setProperty(PROPERTY_TREE_VIEW, JVisualization.TREE_VIEW_MODE_CODE[mComboBox3.getSelectedIndex()]);

		configuration.setProperty(PROPERTY_INVERT_ARROWS, mCheckBoxInvertArrows.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_SHOW_ALL, mCheckBoxShowAll.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_DYNAMIC, mCheckBoxIsDynamic.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_INVERT_TREE, mCheckBoxInvertTree.isSelected() ? "true" : "false");

		configuration.setProperty(PROPERTY_RADIUS, ""+mSliderRadius.getValue());
		configuration.setProperty(PROPERTY_LINE_WIDTH, ""+((float)(mSliderLineWidth.getValue()*mSliderLineWidth.getValue())/2500.0f));
		}

	@Override
	public void addViewConfiguration(CompoundTableView view, Properties configuration) {
		JVisualization visualization = ((VisualizationPanel)view).getVisualization();
		int selectedConnectionColumn = visualization.getConnectionColumn();
		if (selectedConnectionColumn == JVisualization.cColumnUnassigned)
			configuration.setProperty(PROPERTY_CONNECTION, PROPERTY_CONNECTION_CODE_NONE);
		else if (selectedConnectionColumn == JVisualization.cConnectionColumnConnectAll)
			configuration.setProperty(PROPERTY_CONNECTION, PROPERTY_CONNECTION_CODE_ALL);
		else if (selectedConnectionColumn == JVisualization.cConnectionColumnConnectCases)
			configuration.setProperty(PROPERTY_CONNECTION, PROPERTY_CONNECTION_CODE_CASES);
		else
			configuration.setProperty(PROPERTY_CONNECTION, getTableModel().getColumnTitleNoAlias(selectedConnectionColumn));

		int selectedOrderColumn = visualization.getConnectionOrderColumn();
		if (selectedOrderColumn == JVisualization.cColumnUnassigned)
			configuration.setProperty(PROPERTY_ORDER, PROPERTY_ORDER_CODE_X_AXIS);
		else
			configuration.setProperty(PROPERTY_ORDER, getTableModel().getColumnTitleNoAlias(selectedOrderColumn));

		configuration.setProperty(PROPERTY_TREE_VIEW, JVisualization.TREE_VIEW_MODE_CODE[visualization.getTreeViewMode()]);

		configuration.setProperty(PROPERTY_SHOW_ALL, visualization.isTreeViewShowAll() ? "true" : "false");
		configuration.setProperty(PROPERTY_DYNAMIC, visualization.isTreeViewDynamic() ? "true" : "false");
		configuration.setProperty(PROPERTY_INVERT_TREE, visualization.isTreeViewInverted() ? "true" : "false");

		configuration.setProperty(PROPERTY_RADIUS, ""+ visualization.getTreeViewRadius());
		configuration.setProperty(PROPERTY_LINE_WIDTH, ""+ visualization.getConnectionLineWidth());
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		if (view != null) {
			String connection = configuration.getProperty(PROPERTY_CONNECTION, PROPERTY_CONNECTION_CODE_NONE);
			if (!connection.equals(PROPERTY_CONNECTION_CODE_NONE)
			 && !connection.equals(PROPERTY_CONNECTION_CODE_ALL)
			 && !connection.equals(PROPERTY_CONNECTION_CODE_CASES)
			 && getTableModel().findColumn(connection) == -1) {
				showErrorMessage("Column '"+connection+"' not found.");
				return false;
				}
	
			String order = configuration.getProperty(PROPERTY_ORDER, PROPERTY_ORDER_CODE_X_AXIS);
			if (!order.equals(PROPERTY_ORDER_CODE_X_AXIS)
			 && getTableModel().findColumn(order) == -1) {
				showErrorMessage("Column '"+order+"' not found.");
				return false;
				}
			}

		return true;
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		if (!(view instanceof VisualizationPanel))
			return;

		JVisualization visualization = ((VisualizationPanel)view).getVisualization();
		String connection = configuration.getProperty(PROPERTY_CONNECTION, PROPERTY_CONNECTION_CODE_NONE);
		int column = connection.equals(PROPERTY_CONNECTION_CODE_NONE) ? JVisualization.cColumnUnassigned
				   : connection.equals(PROPERTY_CONNECTION_CODE_ALL) ? JVisualization.cConnectionColumnConnectAll
				   : connection.equals(PROPERTY_CONNECTION_CODE_CASES) ? JVisualization.cConnectionColumnConnectCases
				   : getTableModel().findColumn(connection);
		if (column >= 0 && getTableModel().isEqualValueColumn(column))
			column = JVisualization.cColumnUnassigned;

		String orderString = configuration.getProperty(PROPERTY_ORDER, PROPERTY_ORDER_CODE_X_AXIS);
		int orderColumn = orderString.equals(PROPERTY_ORDER_CODE_X_AXIS) ? JVisualization.cColumnUnassigned
				   : getTableModel().findColumn(orderString);

		int mode = findListIndex(configuration.getProperty(PROPERTY_TREE_VIEW),
					JVisualization.TREE_VIEW_MODE_CODE, JVisualization.cTreeViewModeNone);

		int radius = DEFAULT_RADIUS;
		try {
			radius = Integer.parseInt(configuration.getProperty(PROPERTY_RADIUS, ""+DEFAULT_RADIUS));
			}
		catch (NumberFormatException nfe) {}

		float lineWidth = DEFAULT_LINE_WIDTH;
		try {
			lineWidth = Float.parseFloat(configuration.getProperty(PROPERTY_LINE_WIDTH, ""+DEFAULT_LINE_WIDTH));
			}
		catch (NumberFormatException nfe) {}

		boolean invertArrows = configuration.getProperty(PROPERTY_INVERT_ARROWS, "true").equals("true");
		boolean showAll = configuration.getProperty(PROPERTY_SHOW_ALL, "true").equals("true");
		boolean isDynamic = configuration.getProperty(PROPERTY_DYNAMIC, "false").equals("true");
		boolean invertTree = configuration.getProperty(PROPERTY_INVERT_TREE, "false").equals("true");

		visualization.setConnectionColumns(column, orderColumn);
		visualization.setConnectionLineInversion(invertArrows);
		visualization.setTreeViewMode(mode, radius, showAll, isDynamic, invertTree);
		visualization.setConnectionLineWidth(lineWidth, isAdjusting);
		}

	@Override
	public void enableItems() {
		String item = (String)mComboBox1.getSelectedItem();
		boolean isReferencedConnection = false;
		boolean isDirectionalConnection = false;
		if (!item.equals(ITEM_CONNECTION_NONE)
		 && !item.equals(ITEM_CONNECTION_ALL)
		 && !item.equals(ITEM_CONNECTION_CASES)) {
			int column = getTableModel().findColumn(item);
			if (column != -1
			 && getTableModel().getColumnProperty(column, CompoundTableModel.cColumnPropertyReferencedColumn) != null) {
				isReferencedConnection = true;
				if (CompoundTableModel.cColumnPropertyReferenceTypeTopDown.equals(getTableModel().getColumnProperty(column, CompoundTableModel.cColumnPropertyReferenceType)))
					isDirectionalConnection = true;
				}
			}
		mCheckBoxInvertArrows.setEnabled(!hasInteractiveView() || isDirectionalConnection);
		mComboBox2.setEnabled(!item.equals(ITEM_CONNECTION_NONE) && !item.equals(ITEM_CONNECTION_CASES) && !isReferencedConnection);
		mSliderLineWidth.setEnabled(!item.equals(ITEM_CONNECTION_NONE));
		mComboBox3.setEnabled(!hasInteractiveView() || isReferencedConnection);

		boolean showDetailGraph = ((!hasInteractiveView() || isReferencedConnection)
								&& mComboBox3.getSelectedIndex() != JVisualization.cTreeViewModeNone);
		mCheckBoxShowAll.setEnabled(showDetailGraph);
		mCheckBoxIsDynamic.setEnabled(showDetailGraph);
		mCheckBoxInvertTree.setEnabled(showDetailGraph);
		mSliderRadius.setEnabled(showDetailGraph);
		mLabelRadius.setEnabled(showDetailGraph && (!hasInteractiveView() || isDirectionalConnection));
		mLabelLevels.setEnabled(showDetailGraph);
		}

	/**
	 * If you override this method, then make sure you call this super method at the end.
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == mSliderRadius)
			mLabelRadius.setText(""+mSliderRadius.getValue());

		super.stateChanged(e);
		}
	}
