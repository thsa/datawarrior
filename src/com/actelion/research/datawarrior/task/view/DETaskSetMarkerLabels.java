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
import com.actelion.research.table.MarkerLabelConstants;
import com.actelion.research.table.MarkerLabelDisplayer;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableListHandler;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationPanel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;


public class DETaskSetMarkerLabels extends DETaskAbstractSetViewOptions implements MarkerLabelConstants {
	public static final String TASK_NAME = "Set Marker Labels";

	private static final String PROPERTY_IN_DETAIL_GRAPH_ONLY = "inDetailGraphOnly";
	private static final String PROPERTY_LABEL_SIZE = "labelSize";
	private static final String PROPERTY_OPTIMIZE_LABEL_POSITIONS = "optimizeLabels";
	private static final String PROPERTY_BLACK_OR_WHITE = "blackOrWhite";
	private static final String PROPERTY_SHOW_BACKGROUND = "showBackground";
	private static final String PROPERTY_BACKGROUND_TRANSPARENCY = "backgroundTransparency";
	private static final String PROPERTY_SHOW_COLUMN_NAME_IN_TABLE = "showColumnNameInTable";
	private static final String PROPERTY_OPC_CATEGORY_COLUMN = "opcCategoryColumn";
	private static final String PROPERTY_OPC_VALUE_COLUMN = "opcValueColumn";
	private static final String PROPERTY_OPC_MODE = "opcMode";

	private static final String PROPERTY_ROWS = "rows";
	private static final String ITEM_ALL_ROWS = "<All Rows>";
	private static final String CODE_ALL_ROWS = "<all>";
	private static final String ITEM_SELECTED_ROWS = "<Selected Rows>";
	private static final String CODE_SELECTED_ROWS = "<selected>";

	private static final String TEXT_ADD_DEFAULT = "Add Default Labels";
	private static final String TEXT_REMOVE_ALL = "Remove All Labels";

	private static final String TEXT_NO_LABEL = "<no label>";

	private JComboBox[]	mComboBoxPosition;
	private JComboBox	mComboBoxLabelList, mComboBoxOPCCategoryColumn, mComboBoxOPCMode, mComboBoxOPCValueColumn;
	private JSlider		mSliderSize,mSliderTransparency;
	private JLabel		mLabelOPC,mLabelTransparency;
	private JCheckBox	mCheckBoxDetailGraphOnly,mCheckBoxShowBackground,mCheckBoxShowColumnNamesInTable,
						mCheckBoxOnePerCategory,mCheckBoxBlackOrWhite,mCheckBoxOptimizeLabelPositions;

	public DETaskSetMarkerLabels(Frame owner, DEMainPane mainPane, CompoundTableView view) {
		super(owner, mainPane, view);
		}

	private MarkerLabelDisplayer getLabelDisplayer(CompoundTableView view) {
		return (view == null) ? null
			 : (view instanceof VisualizationPanel) ? ((VisualizationPanel)view).getVisualization()
			 : (MarkerLabelDisplayer)view;
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
		return (view instanceof MarkerLabelDisplayer
			 || view instanceof VisualizationPanel) ? null : "Labels can only be shown in 2D-, 3D- and structure-views.";
		}

	@Override
	public JComponent createViewOptionContent() {
		ArrayList<String> columnNameList = new ArrayList<String>();
		columnNameList.add(TEXT_NO_LABEL);
		for (int column=0; column<getTableModel().getTotalColumnCount(); column++)
			if (columnQualifies(column))
				columnNameList.add(getTableModel().getColumnTitle(column));
		String[] columnName = columnNameList.toArray(new String[0]);

		MarkerLabelDisplayer mld = getLabelDisplayer(getInteractiveView());

		mComboBoxPosition = new JComboBox[cPositionCode.length];
		for (int i=0; i<cFirstMidPosition; i++)
			mComboBoxPosition[i] = new JComboBox(columnName);
		if (mld == null || mld.supportsMidPositionLabels())
			for (int i=cFirstMidPosition; i<MarkerLabelDisplayer.cFirstBottomPosition; i++)
				mComboBoxPosition[i] = new JComboBox(columnName);
		for (int i=cFirstBottomPosition; i<cFirstTablePosition; i++)
			mComboBoxPosition[i] = new JComboBox(columnName);
		int tableLines = (mld == null || mld.supportsMarkerLabelTable()) ? cPositionCode.length - cFirstTablePosition : 0;
		for (int i=0; i<tableLines; i++)
			mComboBoxPosition[cFirstTablePosition+i] = new JComboBox(columnName);

		for (JComboBox cb:mComboBoxPosition)
			if (cb != null)
				cb.addActionListener(this);

		int gap = HiDPIHelper.scale(8);
		double[] sizeY1a = {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2,
				TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, 2*gap };
		double[] sizeY1 = new double[sizeY1a.length+2*tableLines];
		int index = 0;
		for (double s:sizeY1a)
			sizeY1[index++] = s;
		for (int i=0; i<tableLines; i++) {
			sizeY1[index++] = TableLayout.PREFERRED;
			sizeY1[index++] = gap/2;
			}
		sizeY1[index-1] = gap;

		double[][] size1 = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap}, sizeY1 };
		JPanel cp1 = new JPanel();
		cp1.setLayout(new TableLayout(size1));

		cp1.add(new JLabel("Select labels with positions relative to marker or structure:"), "1,1,5,1");
		cp1.add(new JLabel("Top Left"), "1,3");
		cp1.add(new JLabel("Top Right", JLabel.RIGHT), "5,3");
		cp1.add(new JLabel("Bottom Left"), "1,11");
		cp1.add(new JLabel("Bottom Right", JLabel.RIGHT), "5,11");
		for (int i=0; i<cPositionCode.length; i++) {
			if (mComboBoxPosition[i] != null) {
				mComboBoxPosition[i].addItemListener(this);
				mComboBoxPosition[i].setEditable(!hasInteractiveView());
				if (i < cFirstTablePosition) {
					cp1.add(mComboBoxPosition[i], ""+(1+2*(i%3))+","+(5+2*(i/3)));
					}
				else {
					int y = 13+2*(i-cFirstTablePosition);
					cp1.add(new JLabel("Table line "+(1+i-cFirstTablePosition)+":", JLabel.RIGHT), "1,"+y);
					cp1.add(mComboBoxPosition[i], "3,"+y);
					}
				}
			}

		boolean showColumnNameOption = (mld == null || mld.supportsMarkerLabelTable());
		boolean showOptimizePositionOption = (mld == null || mld.supportsLabelPositionOptimization());
		boolean showBackgroundOption = (mld == null || mld.supportsLabelBackground());
		boolean showBackgroundSlider = (showBackgroundOption && (mld == null || mld.supportsLabelBackgroundTransparency()));
		boolean showListOption = (mld == null || mld.supportsLabelsByList());
		boolean showDetailGraphOption = (mld == null || mld.isTreeViewModeEnabled());

		double[] sizeY2 = new double[ 7 + (showColumnNameOption? 2:0)
										+ (showOptimizePositionOption? 2:0)
										+ (showBackgroundOption?(showBackgroundSlider? 4:2) : 0)
										+ (showListOption? 6:0)
										+ (showDetailGraphOption? 2:0)];

		index = 0;
		sizeY2[index++] = gap;
		if (showListOption) {
			sizeY2[index++] = TableLayout.PREFERRED;
			sizeY2[index++] = 2*gap;
			sizeY2[index++] = TableLayout.PREFERRED;
			sizeY2[index++] = gap/2;
			sizeY2[index++] = TableLayout.PREFERRED;
			sizeY2[index++] = 2*gap;
		}
		if (showColumnNameOption) {
			sizeY2[index++] = TableLayout.PREFERRED;
			sizeY2[index++] = gap/2;
		}
		if (showDetailGraphOption) {
			sizeY2[index++] = TableLayout.PREFERRED;
			sizeY2[index++] = gap/2;
		}
		if (showOptimizePositionOption) {
			sizeY2[index++] = TableLayout.PREFERRED;
			sizeY2[index++] = gap/2;
		}
		sizeY2[index++] = TableLayout.PREFERRED;
		sizeY2[index++] = gap/2;
		if (showBackgroundOption) {
			sizeY2[index++] = TableLayout.PREFERRED;
			sizeY2[index++] = gap/2;	// in case we have a slider
			if (showBackgroundSlider) {
				sizeY2[index++] = TableLayout.PREFERRED;
				sizeY2[index++] = gap;
			}
		}
		sizeY2[index-1] = gap;	// correct the last vertical spacer
		sizeY2[index++] = TableLayout.PREFERRED;
		sizeY2[index++] = 2*gap;
		sizeY2[index++] = TableLayout.PREFERRED;
		sizeY2[index++] = gap;

		double[][] size2 = { {gap, TableLayout.FILL, TableLayout.PREFERRED, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, TableLayout.FILL, gap}, sizeY2 };
		JPanel cp2 = new JPanel();
		cp2.setLayout(new TableLayout(size2));

		index = 1;
		if (showListOption) {
			mComboBoxLabelList = new JComboBox();
			mComboBoxLabelList.addItem(ITEM_ALL_ROWS);
			mComboBoxLabelList.addItem(ITEM_SELECTED_ROWS);
			for (int i = 0; i<getTableModel().getListHandler().getListCount(); i++)
				mComboBoxLabelList.addItem(getTableModel().getColumnTitleExtended(CompoundTableListHandler.getColumnFromList(i)));
			mComboBoxLabelList.setEditable(!hasInteractiveView());
			mComboBoxLabelList.addItemListener(this);
			cp2.add(new JLabel("Show labels on: ", JLabel.RIGHT), "2,"+index);
			cp2.add(mComboBoxLabelList, "3,"+index+",5,"+index);
			index += 2;

			mCheckBoxOnePerCategory = new JCheckBox("Show only one label per");
			mCheckBoxOnePerCategory.addActionListener(this);
			mComboBoxOPCCategoryColumn = new JComboBox();
			for (int i=0; i<getTableModel().getTotalColumnCount(); i++)
				if (getTableModel().isColumnTypeCategory(i))
					mComboBoxOPCCategoryColumn.addItem(getTableModel().getColumnTitle(i));
			mComboBoxOPCCategoryColumn.setEditable(!hasInteractiveView());
			mComboBoxOPCCategoryColumn.addActionListener(this);
			mComboBoxOPCMode = new JComboBox(cOnePerCategoryMode);
			mComboBoxOPCMode.addActionListener(this);
			mComboBoxOPCValueColumn = new JComboBox();
			updateOPCValueColumns();
			mComboBoxOPCValueColumn.addActionListener(this);
			mComboBoxOPCValueColumn.setEditable(!hasInteractiveView());

			cp2.add(mCheckBoxOnePerCategory, "2," + index);
			cp2.add(mComboBoxOPCCategoryColumn, "3," + index+",5," + index);
			index += 2;

			mLabelOPC = new JLabel("on that row having the ", JLabel.RIGHT);
			cp2.add(mLabelOPC, "2,"+index);
			cp2.add(mComboBoxOPCMode, "3,"+index);
			cp2.add(mComboBoxOPCValueColumn, "5,"+index);
			index += 2;
			}

		if (showColumnNameOption) {
			mCheckBoxShowColumnNamesInTable = new JCheckBox("Show column names in table");
			mCheckBoxShowColumnNamesInTable.addActionListener(this);
			cp2.add(mCheckBoxShowColumnNamesInTable, "2," + index + ",5," + index);
			index += 2;
			}

		if (showDetailGraphOption) {
			mCheckBoxDetailGraphOnly = new JCheckBox("Show labels in detail graph only");
			mCheckBoxDetailGraphOnly.setEnabled(mld == null || mld.isTreeViewModeEnabled());
			mCheckBoxDetailGraphOnly.addActionListener(this);
			cp2.add(mCheckBoxDetailGraphOnly, "2," + index + ",5," + index);
			index += 2;
			}

		if (showOptimizePositionOption) {
			mCheckBoxOptimizeLabelPositions = new JCheckBox("Automatically optimize positions");
			mCheckBoxOptimizeLabelPositions.addActionListener(this);
			cp2.add(mCheckBoxOptimizeLabelPositions, "2," + index + ",5," + index);
			index += 2;
			}

		mCheckBoxBlackOrWhite = new JCheckBox("Show labels in black or white");
		mCheckBoxBlackOrWhite.addActionListener(this);
		cp2.add(mCheckBoxBlackOrWhite, "2," + index + ",5," + index);
		index += 2;

		if (showBackgroundOption) {
			mCheckBoxShowBackground = new JCheckBox("Show rectangular label background");
//			mCheckBoxShowBackground.setHorizontalAlignment(SwingConstants.CENTER);
			mCheckBoxShowBackground.addActionListener(this);
			cp2.add(mCheckBoxShowBackground, "2," + index + ",5," + index);
			index += 2;

			if (showBackgroundSlider) {
				mSliderTransparency = new JSlider(JSlider.HORIZONTAL, 0, 80, 25);
				mSliderTransparency.setPreferredSize(new Dimension(HiDPIHelper.scale(150), mSliderTransparency.getPreferredSize().height));
				mSliderTransparency.addChangeListener(this);
				mLabelTransparency = new JLabel("Background transparency:", JLabel.RIGHT);
				cp2.add(mLabelTransparency, "2,"+index+",3,"+index);
				cp2.add(mSliderTransparency, "5,"+index);
				index += 2;
				}
			}

		mSliderSize = new JSlider(JSlider.HORIZONTAL, 0, 150, 50);
		mSliderSize.setPreferredSize(new Dimension(HiDPIHelper.scale(150), mSliderSize.getPreferredSize().height));
		mSliderSize.addChangeListener(this);
		cp2.add(new JLabel("Label size:", JLabel.RIGHT), "2,"+index+",3,"+index);
		cp2.add(mSliderSize, "5,"+index);
		index += 2;

		JPanel bp = new JPanel();
		bp.setLayout(new GridLayout(1, 2, gap, 0));
		JButton bdefault = new JButton(TEXT_ADD_DEFAULT);
		bdefault.addActionListener(this);
		bp.add(bdefault);
		JButton bnone = new JButton(TEXT_REMOVE_ALL);
		bnone.addActionListener(this);
		bp.add(bnone);
		cp2.add(bp, "2,"+index+",5,"+index);

		JPanel cp = new JPanel();
		cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
		cp.add(cp1);
		cp.add(cp2);
		return cp;
		}

	private boolean columnQualifies(int column) {
		return getTableModel().getColumnSpecialType(column) == null
			|| (getTableModel().isColumnTypeStructure(column)
			 && (!hasInteractiveView() || getInteractiveView() instanceof VisualizationPanel));
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(TEXT_REMOVE_ALL))
			removeAllLabels();

		if (e.getActionCommand().equals(TEXT_ADD_DEFAULT))
			addDefaultLabels();
	
		if (!isIgnoreEvents()) {
			for (JComboBox cb:mComboBoxPosition) {
				if (cb != null && e.getSource() == cb && !TEXT_NO_LABEL.equals(cb.getSelectedItem())) {
					setIgnoreEvents(true);
					for (JComboBox cbi:mComboBoxPosition)
						if (cbi != null && cbi != cb && cb.getSelectedItem().equals(cbi.getSelectedItem()))
							cbi.setSelectedItem(TEXT_NO_LABEL);
					setIgnoreEvents(false);
					}
				}

			if (e.getSource() == mComboBoxOPCCategoryColumn) {
				updateOPCValueColumns();
				}

			super.actionPerformed(e);	// causes a view update
			}
		}

	private void updateOPCValueColumns() {
		setIgnoreEvents(true);
		mComboBoxOPCValueColumn.removeAllItems();

		int categoryColumn = getTableModel().findColumn((String)mComboBoxOPCCategoryColumn.getSelectedItem());
		if (categoryColumn == -1)
			return;

		int categoryCount = getTableModel().getCategoryCount(categoryColumn);
		float[] valueInCategory = new float[categoryCount];
		for (int column=0; column<getTableModel().getTotalColumnCount(); column++) {
			if (getTableModel().isColumnTypeDouble(column) && column != categoryColumn) {
				Arrays.fill(valueInCategory, Float.NaN);
				boolean valuesInAtLeastOneCategoryDiffer = false;
				for (int row=0; row<getTableModel().getTotalRowCount(); row++) {
					CompoundRecord record = getTableModel().getTotalRecord(row);
					int cat = getTableModel().getCategoryIndex(categoryColumn, record);
					float v = record.getDouble(column);
					if (!Float.isNaN(v)) {
						if (Float.isNaN(valueInCategory[cat])) {
							valueInCategory[cat] = v;
							}
						else if (valueInCategory[cat] != v) {
							valuesInAtLeastOneCategoryDiffer = true;
							break;
							}
						}
					}
				if (valuesInAtLeastOneCategoryDiffer)
					mComboBoxOPCValueColumn.addItem(getTableModel().getColumnTitle(column));
				}
			}
		setIgnoreEvents(false);
		}

	private void removeAllLabels() {
		setIgnoreEvents(true);
		for (int i=0; i<mComboBoxPosition.length; i++)
			if (mComboBoxPosition[i] != null)
				mComboBoxPosition[i].setSelectedItem(TEXT_NO_LABEL);
		setIgnoreEvents(false);
		}

	private void addDefaultLabels() {
		removeAllLabels();

		setIgnoreEvents(true);
		int idCount = 0;
		int numCount = 0;
		for (int column=0; column<getTableModel().getTotalColumnCount(); column++) {
			if (columnQualifies(column) && getTableModel().getColumnSpecialType(column) == null) {
				if (getTableModel().isColumnTypeDouble(column)) {
					if ((!hasInteractiveView() || getLabelDisplayer(getInteractiveView()).supportsMarkerLabelTable()) && numCount < 6)
						mComboBoxPosition[cFirstTablePosition+numCount++].setSelectedItem(getTableModel().getColumnTitle(column));
					}
				else {
					if (idCount == 0) {
						mComboBoxPosition[0].setSelectedItem(getTableModel().getColumnTitle(column));
						idCount++;
						}
					else if (idCount == 1) {
						mComboBoxPosition[2].setSelectedItem(getTableModel().getColumnTitle(column));
						idCount++;
						}
					}
				}
			}
		setIgnoreEvents(false);
   		}
	
	@Override
	public void setDialogToDefault() {
		for (JComboBox cb:mComboBoxPosition)
			cb.setSelectedItem(TEXT_NO_LABEL);
		if (mComboBoxLabelList != null)
			mComboBoxLabelList.setSelectedIndex(0);
		if (mCheckBoxOnePerCategory != null)
			mCheckBoxOnePerCategory.setSelected(false);
		if (mCheckBoxOptimizeLabelPositions != null)
			mCheckBoxOptimizeLabelPositions.setSelected(false);
		if (mCheckBoxShowBackground != null)
			mCheckBoxShowBackground.setSelected(false);
		if (mSliderTransparency != null)
			mSliderTransparency.setValue(25);
		mSliderSize.setValue(50);
		mCheckBoxShowColumnNamesInTable.setSelected(true);
		mCheckBoxBlackOrWhite.setSelected(false);
		if (mCheckBoxDetailGraphOnly != null)
			mCheckBoxDetailGraphOnly.setSelected(false);
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		for (int i=0; i<cPositionCode.length; i++) {
			if (mComboBoxPosition[i] != null) {
				String columnName = configuration.getProperty(cPositionCode[i]);
				if (columnName == null)
					mComboBoxPosition[i].setSelectedItem(TEXT_NO_LABEL);
				else {
					int column = getTableModel().findColumn(columnName);
					mComboBoxPosition[i].setSelectedItem(column == -1 ? columnName : getTableModel().getColumnTitle(column));
					}
				}
			}

		if (mComboBoxLabelList != null) {
			String columnName = configuration.getProperty(PROPERTY_ROWS, CODE_ALL_ROWS);
			if (columnName.equals(CODE_ALL_ROWS)) {
				mComboBoxLabelList.setSelectedItem(ITEM_ALL_ROWS);
				}
			else if (columnName.equals(CODE_SELECTED_ROWS)) {
				mComboBoxLabelList.setSelectedItem(ITEM_SELECTED_ROWS);
				}
			else {
				int pseudoColumn = getTableModel().findColumn(columnName);
				mComboBoxLabelList.setSelectedItem(!hasInteractiveView() && pseudoColumn == -1 ? columnName : getTableModel().getColumnTitleExtended(pseudoColumn));
				}
			}
		if (mCheckBoxOnePerCategory != null) {
			String categoryColumnName = configuration.getProperty(PROPERTY_OPC_CATEGORY_COLUMN, "");
			int categoryColumn = getTableModel().findColumn(categoryColumnName);
			if (!getTableModel().isColumnTypeCategory(categoryColumn))
				categoryColumn = -1;
			String valueColumnName = configuration.getProperty(PROPERTY_OPC_VALUE_COLUMN, "");
			int valueColumn = getTableModel().findColumn(valueColumnName);
			if (!getTableModel().isColumnTypeDouble(valueColumn))
				valueColumn = -1;
			if (hasInteractiveView()) {
				if (categoryColumn == -1 || categoryColumn == -1) {
					mCheckBoxOnePerCategory.setSelected(false);
					}
				else {
					mCheckBoxOnePerCategory.setSelected(true);
					mComboBoxOPCCategoryColumn.setSelectedItem(getTableModel().getColumnTitleExtended(categoryColumn));
					updateOPCValueColumns();
					mComboBoxOPCValueColumn.setSelectedItem(getTableModel().getColumnTitleExtended(valueColumn));
					}
				}
			else {
				if (categoryColumnName.length() != 0 && valueColumnName.length() != 0) {
					mCheckBoxOnePerCategory.setSelected(true);
					mComboBoxOPCCategoryColumn.setSelectedItem(categoryColumnName);
					mComboBoxOPCValueColumn.setSelectedItem(valueColumnName);
					}
				}
			mComboBoxOPCMode.setSelectedItem(configuration.getProperty(PROPERTY_OPC_MODE, cOnePerCategoryMode[0]));
			}

		if (mCheckBoxShowColumnNamesInTable != null)
			mCheckBoxShowColumnNamesInTable.setSelected(!"false".equals(configuration.getProperty(PROPERTY_SHOW_COLUMN_NAME_IN_TABLE)));

		if (mCheckBoxOptimizeLabelPositions != null)
			mCheckBoxOptimizeLabelPositions.setSelected("true".equals(configuration.getProperty(PROPERTY_OPTIMIZE_LABEL_POSITIONS)));

		if (mCheckBoxShowBackground != null)
			mCheckBoxShowBackground.setSelected("true".equals(configuration.getProperty(PROPERTY_SHOW_BACKGROUND)));

		if (mSliderTransparency != null) {
			float transparency = 0.25f;
			try {
				transparency = Float.parseFloat(configuration.getProperty(PROPERTY_BACKGROUND_TRANSPARENCY, "0.25"));
				}
			catch (NumberFormatException nfe) {}
			mSliderTransparency.setValue(Math.round(100f * transparency));
			}

		float size = 1.0f;
		try {
			size = Float.parseFloat(configuration.getProperty(PROPERTY_LABEL_SIZE, "1.0"));
			}
		catch (NumberFormatException nfe) {}
		mSliderSize.setValue(50+(int)(50.0*Math.log(size)));

		mCheckBoxBlackOrWhite.setSelected("true".equals(configuration.getProperty(PROPERTY_BLACK_OR_WHITE)));

		if (mCheckBoxDetailGraphOnly != null)
			mCheckBoxDetailGraphOnly.setSelected("true".equals(configuration.getProperty(PROPERTY_IN_DETAIL_GRAPH_ONLY)));
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		for (int i=0; i<cFirstTablePosition; i++)
			if (mComboBoxPosition[i] != null && !mComboBoxPosition[i].getSelectedItem().equals(TEXT_NO_LABEL))
				configuration.setProperty(cPositionCode[i], ""+getTableModel().getColumnTitleNoAlias((String)mComboBoxPosition[i].getSelectedItem()));
		int tableIndex = cFirstTablePosition;
		for (int i=cFirstTablePosition; i<cPositionCode.length; i++)
			if (mComboBoxPosition[i] != null && !mComboBoxPosition[i].getSelectedItem().equals(TEXT_NO_LABEL))
				configuration.setProperty(cPositionCode[tableIndex++], ""+getTableModel().getColumnTitleNoAlias((String)mComboBoxPosition[i].getSelectedItem()));
		if (mComboBoxLabelList != null) {
			String item = (String) mComboBoxLabelList.getSelectedItem();
			configuration.setProperty(PROPERTY_ROWS,
					  item.equals(ITEM_ALL_ROWS) ? CODE_ALL_ROWS
					: item.equals(ITEM_SELECTED_ROWS) ? CODE_SELECTED_ROWS
					: getTableModel().getColumnTitleNoAlias(item));
			}
		if (mCheckBoxOnePerCategory != null && mCheckBoxOnePerCategory.isSelected()) {
			if (mComboBoxOPCCategoryColumn.getSelectedItem() != null)
				configuration.setProperty(PROPERTY_OPC_CATEGORY_COLUMN, getTableModel().getColumnTitleNoAlias((String)mComboBoxOPCCategoryColumn.getSelectedItem()));
			if (mComboBoxOPCValueColumn.getSelectedItem() != null)
				configuration.setProperty(PROPERTY_OPC_VALUE_COLUMN, getTableModel().getColumnTitleNoAlias((String)mComboBoxOPCValueColumn.getSelectedItem()));
			configuration.setProperty(PROPERTY_OPC_MODE, (String)mComboBoxOPCMode.getSelectedItem());
			}
		if (mCheckBoxShowColumnNamesInTable != null)
			configuration.setProperty(PROPERTY_SHOW_COLUMN_NAME_IN_TABLE, mCheckBoxShowColumnNamesInTable.isSelected() ? "true" : "false");
		if (mCheckBoxOptimizeLabelPositions != null)
			configuration.setProperty(PROPERTY_OPTIMIZE_LABEL_POSITIONS, mCheckBoxOptimizeLabelPositions.isSelected() ? "true" : "false");
		if (mCheckBoxShowBackground != null) {
			boolean showBackground = mCheckBoxShowBackground.isSelected();
			configuration.setProperty(PROPERTY_SHOW_BACKGROUND, showBackground ? "true" : "false");
			if (showBackground && mSliderTransparency != null) {
				float transparency = (float) mSliderTransparency.getValue() / 100f;
				configuration.setProperty(PROPERTY_BACKGROUND_TRANSPARENCY, "" + transparency);
				}
			}
		float size = (float)Math.exp((double)(mSliderSize.getValue()-50)/50.0);
		configuration.setProperty(PROPERTY_LABEL_SIZE, ""+size);
		configuration.setProperty(PROPERTY_BLACK_OR_WHITE, mCheckBoxBlackOrWhite.isSelected() ? "true" : "false");
		if (mCheckBoxDetailGraphOnly != null)
			configuration.setProperty(PROPERTY_IN_DETAIL_GRAPH_ONLY, mCheckBoxDetailGraphOnly.isSelected() ? "true" : "false");
		}

	@Override
	public void addViewConfiguration(CompoundTableView view, Properties configuration) {
		MarkerLabelDisplayer mld = getLabelDisplayer(view);
		for (int i=0; i<cPositionCode.length; i++) {
			int column = mld.getMarkerLabelColumn(i);
			if (column != JVisualization.cColumnUnassigned)
				configuration.setProperty(cPositionCode[i], ""+getTableModel().getColumnTitleNoAlias(column));
			}

		if (mld.supportsLabelsByList()) {
			int list = mld.getMarkerLabelList();
			if (list == MarkerLabelDisplayer.cLabelsOnAllRows)
				configuration.setProperty(PROPERTY_ROWS, CODE_ALL_ROWS);
			else if (list == MarkerLabelDisplayer.cLabelsOnSelection)
				configuration.setProperty(PROPERTY_ROWS, CODE_SELECTED_ROWS);
			else
				configuration.setProperty(PROPERTY_ROWS, getTableModel().getColumnTitleNoAlias(CompoundTableListHandler.getColumnFromList(list)));

			int[] opc = mld.getMarkerLabelOnePerCategory();
			if (opc != null) {
				configuration.setProperty(PROPERTY_OPC_CATEGORY_COLUMN, getTableModel().getColumnTitleNoAlias(opc[0]));
				configuration.setProperty(PROPERTY_OPC_VALUE_COLUMN, getTableModel().getColumnTitleNoAlias(opc[1]));
				configuration.setProperty(PROPERTY_OPC_MODE, getTableModel().getColumnTitleNoAlias(cOnePerCategoryMode[opc[2]]));
				}
			}

		if (mld.supportsMarkerLabelTable())
			configuration.setProperty(PROPERTY_SHOW_COLUMN_NAME_IN_TABLE, mld.isShowColumnNameInTable() ? "true" : "false");

		if (mld.supportsLabelPositionOptimization())
			configuration.setProperty(PROPERTY_OPTIMIZE_LABEL_POSITIONS, mld.isOptimizeLabelPositions() ? "true" : "false");

		if (mld.supportsLabelBackground() && mld.isShowLabelBackground()) {
			configuration.setProperty(PROPERTY_SHOW_BACKGROUND, mld.isShowLabelBackground() ? "true" : "false");
			if (mld.supportsLabelBackgroundTransparency())
				configuration.setProperty(PROPERTY_BACKGROUND_TRANSPARENCY, "" + mld.getLabelTransparency());
			}
		configuration.setProperty(PROPERTY_LABEL_SIZE, ""+mld.getMarkerLabelSize());
		configuration.setProperty(PROPERTY_BLACK_OR_WHITE, mld.isMarkerLabelBlackOrWhite() ? "true" : "false");
		configuration.setProperty(PROPERTY_IN_DETAIL_GRAPH_ONLY, mld.isMarkerLabelsInTreeViewOnly() ? "true" : "false");
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		if (view != null) {
			for (int i=0; i<cPositionCode.length; i++) {
				String columnName = configuration.getProperty(cPositionCode[i]);
				if (columnName != null) {
					int column = getTableModel().findColumn(columnName);
					if (column == -1) {
						showErrorMessage("Column '"+columnName+"' not found.");
						return false;
						}
					if (!columnQualifies(column)) {
						showErrorMessage("Column '"+columnName+"' cannot be used for displaying labels.");
						return false;
						}
					}
				}

			String pseudoColumnName = configuration.getProperty(PROPERTY_ROWS, CODE_ALL_ROWS);
			if (!CODE_ALL_ROWS.equals(pseudoColumnName)
			 && !CODE_SELECTED_ROWS.equals(pseudoColumnName)) {
				int pseudoColumn = getTableModel().findColumn(pseudoColumnName);
				if (pseudoColumn == -1) {
					showErrorMessage("'"+pseudoColumnName+"' not found.");
					return false;
					}
				}

			String categoryColumnName = configuration.getProperty(PROPERTY_OPC_CATEGORY_COLUMN);
			String valueColumnName = configuration.getProperty(PROPERTY_OPC_VALUE_COLUMN);
			if (categoryColumnName != null) {
//				if (categoryColumnName.equals(valueColumnName)) {
//					showErrorMessage("When showing one label per category, then the\ncategory column must be different from the value column");
//					return false;
//					}
				int categoryColumn = getTableModel().findColumn(categoryColumnName);
				if (categoryColumn == -1) {
					showErrorMessage("'"+categoryColumnName+"' not found.");
					return false;
					}
				if (!getTableModel().isColumnTypeCategory(categoryColumn)) {
					showErrorMessage("'"+categoryColumnName+"' does not contain categories.");
					return false;
					}
				}
			if (valueColumnName != null) {
				int valueColumn = getTableModel().findColumn(valueColumnName);
				if (valueColumn == -1) {
					showErrorMessage("'"+valueColumnName+"' not found.");
					return false;
				}
				if (!getTableModel().isColumnTypeDouble(valueColumn)) {
					showErrorMessage("'"+valueColumnName+"' does not contain numerical values.");
					return false;
					}
				}
			}

		return true;
		}

	@Override
	public void enableItems() {
		if (mSliderTransparency != null) {
			mLabelTransparency.setEnabled(mCheckBoxShowBackground.isSelected());
			mSliderTransparency.setEnabled(mCheckBoxShowBackground.isSelected());
			}
		if (mCheckBoxOnePerCategory != null) {
			boolean isOnePerCategory = mCheckBoxOnePerCategory.isSelected();
			mLabelOPC.setEnabled(isOnePerCategory);
			mComboBoxOPCCategoryColumn.setEnabled(isOnePerCategory);
			mComboBoxOPCMode.setEnabled(isOnePerCategory);
			mComboBoxOPCValueColumn.setEnabled(isOnePerCategory);
			}
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		int[] columnAtPosition = new int[cPositionCode.length];

		MarkerLabelDisplayer mld = getLabelDisplayer(view);
		if (mld == null)
			return;

		for (int i = 0; i<cPositionCode.length; i++) {
			String columnName = configuration.getProperty(cPositionCode[i]);
			if (columnName == null)
				columnAtPosition[i] = -1;
			else
				columnAtPosition[i] = getTableModel().findColumn(columnName);
		}
		mld.setMarkerLabels(columnAtPosition);

		if (mld.supportsLabelsByList()) {
			String pseudoColumnName = configuration.getProperty(PROPERTY_ROWS, CODE_ALL_ROWS);
			if (pseudoColumnName.equals(CODE_ALL_ROWS))
				mld.setMarkerLabelList(MarkerLabelDisplayer.cLabelsOnAllRows);
			else if (pseudoColumnName.equals(CODE_SELECTED_ROWS))
				mld.setMarkerLabelList(MarkerLabelDisplayer.cLabelsOnSelection);
			else
				mld.setMarkerLabelList(
						CompoundTableListHandler.convertToListIndex(getTableModel().findColumn(pseudoColumnName)));

			int categoryColumn = getTableModel().findColumn(configuration.getProperty(PROPERTY_OPC_CATEGORY_COLUMN));
			int valueColumn = getTableModel().findColumn(configuration.getProperty(PROPERTY_OPC_VALUE_COLUMN));
			int categoryMode = findListIndex(configuration.getProperty(PROPERTY_OPC_MODE), cOnePerCategoryMode, 0);
			mld.setMarkerLabelOnePerCategory(categoryColumn, valueColumn, categoryMode);
			}

		if (mld.supportsMarkerLabelTable())
			mld.setShowColumnNameInTable(!"false".equals(configuration.getProperty(PROPERTY_SHOW_COLUMN_NAME_IN_TABLE)));

		if (mld.supportsLabelPositionOptimization())
			mld.setOptimizeLabelPositions("true".equals(configuration.getProperty(PROPERTY_OPTIMIZE_LABEL_POSITIONS)));

		if (mld.supportsLabelBackground()) {
			mld.setShowLabelBackground("true".equals(configuration.getProperty(PROPERTY_SHOW_BACKGROUND)));
			if (mld.supportsLabelBackgroundTransparency()) {
				try {
					mld.setLabelTransparency(Float.parseFloat(configuration.getProperty(PROPERTY_BACKGROUND_TRANSPARENCY, "0.25")), isAdjusting);
					}
				catch (NumberFormatException nfe) {}
				}
			}

		try {
			mld.setMarkerLabelSize(Float.parseFloat(configuration.getProperty(PROPERTY_LABEL_SIZE, "1.0")), isAdjusting);
			}
		catch (NumberFormatException nfe) {}

		mld.setMarkerLabelsBlackOrWhite("true".equals(configuration.getProperty(PROPERTY_BLACK_OR_WHITE)));
		mld.setMarkerLabelsInTreeViewOnly("true".equals(configuration.getProperty(PROPERTY_IN_DETAIL_GRAPH_ONLY)));
		}
	}
