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

package com.actelion.research.datawarrior.task.file;

import com.actelion.research.calc.CorrelationCalculator;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEPruningPanel.FilterException;
import com.actelion.research.datawarrior.action.DECorrelationDialog;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.filter.JTextFilterPanel;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableListHandler;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.model.NumericalCompoundTableColumn;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationColor;
import com.actelion.research.table.view.VisualizationPanel2D;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;


public class DETaskNewFileFromCorrelationCoefficients extends ConfigurableTask {
	private static final String PROPERTY_TYPE = "type";
	private static final String PROPERTY_ROWS = "rows";

	public static final String TASK_NAME = "New File From Correlation Coefficient";

	private CompoundTableModel	mSourceTableModel;
	private JComboBox			mComboBoxCorrelationType,mComboBoxRowMode;
	private DEFrame				mSourceFrame,mTargetFrame;
	private int					mCorrelationType,mRowMode;
	private String              mListName;

	public DETaskNewFileFromCorrelationCoefficients(DEFrame sourceFrame, int correlationType, int rowMode, String listName) {
		super(sourceFrame, false);
		mSourceFrame = sourceFrame;
		mSourceTableModel = sourceFrame.getTableModel();
		mCorrelationType = correlationType;
		mRowMode = rowMode;
		mListName = listName;
		}

	@Override
	public JPanel createDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };
		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		content.add(new JLabel("Correlation Coefficient:"), "1,1");
		mComboBoxCorrelationType = new JComboBox(CorrelationCalculator.TYPE_LONG_NAME);
		content.add(mComboBoxCorrelationType, "3,1");

		mComboBoxRowMode = new JComboBox(DECorrelationDialog.ROW_MODE_TEXT);
		for (int i = 0; i<mSourceTableModel.getListHandler().getListCount(); i++)
			mComboBoxRowMode.addItem(mSourceTableModel.getListHandler().getListName(i));
		mComboBoxRowMode.setEditable(!isInteractive());
		content.add(new JLabel("Considered rows:", JLabel.RIGHT), "1,3");
		content.add(mComboBoxRowMode, "3,3");

		return content;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		if (mCorrelationType == -1)
			return null;

		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_TYPE, CorrelationCalculator.TYPE_CODE[mCorrelationType]);
		configuration.setProperty(PROPERTY_ROWS, mRowMode < DECorrelationDialog.ROW_MODE_CODE.length ?
				DECorrelationDialog.ROW_MODE_CODE[mRowMode] : mListName);
		return configuration;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public boolean isConfigurable() {
		int[] nc = DECorrelationDialog.getNumericalColumns(mSourceTableModel);
		if (nc.length < 2) {
			showErrorMessage("Less than two numerical columns found.");
			return false;
			}

		return true;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties p = new Properties();
		p.setProperty(PROPERTY_TYPE, CorrelationCalculator.TYPE_CODE[mComboBoxCorrelationType.getSelectedIndex()]);
		p.setProperty(PROPERTY_ROWS, mComboBoxRowMode.getSelectedIndex() < DECorrelationDialog.ROW_MODE_CODE.length ?
				DECorrelationDialog.ROW_MODE_CODE[mComboBoxRowMode.getSelectedIndex()] : (String)mComboBoxRowMode.getSelectedItem());
		return p;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		int type = findListIndex(configuration.getProperty(PROPERTY_TYPE), CorrelationCalculator.TYPE_CODE, 0);
		mComboBoxCorrelationType.setSelectedIndex(type);
		int rows = findListIndex(configuration.getProperty(PROPERTY_ROWS, DECorrelationDialog.ROW_MODE_CODE[0]), DECorrelationDialog.ROW_MODE_CODE, -1);
		if (rows != -1)
			mComboBoxRowMode.setSelectedIndex(rows);
		else
			mComboBoxRowMode.setSelectedItem(configuration.getProperty(PROPERTY_ROWS));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mComboBoxCorrelationType.setSelectedIndex(0);
		mComboBoxRowMode.setSelectedIndex(0);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		return true;
		}

	@Override
	public void runTask(Properties configuration) {

		int[] numericalColumn = DECorrelationDialog.getNumericalColumns(mSourceTableModel);

		String rowsText = configuration.getProperty(PROPERTY_ROWS);
		int type = findListIndex(configuration.getProperty(PROPERTY_TYPE), CorrelationCalculator.TYPE_CODE, 0);
		int rows = findListIndex(rowsText != null ? rowsText : DECorrelationDialog.ROW_MODE_CODE[0],
				DECorrelationDialog.ROW_MODE_CODE, -1);
		if (rows == -1) {
			int listIndex = mSourceTableModel.getListHandler().getListIndex(rowsText);
			if (listIndex == -1)
				rows = DECorrelationDialog.ROWS_ALL;
			}
		int rowMode = (rows == DECorrelationDialog.ROWS_ALL) ? NumericalCompoundTableColumn.MODE_ALL_ROWS
					: (rows == DECorrelationDialog.ROWS_VISIBLE) ? NumericalCompoundTableColumn.MODE_VISIBLE_ROWS
					: NumericalCompoundTableColumn.MODE_CUSTOM_ROWS;
		int[] customRows = null;
		if (rowMode == NumericalCompoundTableColumn.MODE_CUSTOM_ROWS) {
			int listIndex = (rows == DECorrelationDialog.ROWS_SELECTED) ? CompoundTableListHandler.LISTINDEX_SELECTION
					: mSourceTableModel.getListHandler().getListIndex(rowsText);
			customRows = NumericalCompoundTableColumn.compileCustomRows(listIndex, mSourceTableModel);
			}
		NumericalCompoundTableColumn[] nc = new NumericalCompoundTableColumn[numericalColumn.length];
		for (int i=0; i<numericalColumn.length; i++)
			nc[i] = new NumericalCompoundTableColumn(mSourceTableModel, numericalColumn[i], rowMode, customRows);
		CorrelationCalculator cc = new CorrelationCalculator();
		double[][] correlation = cc.calculateMatrix(nc, type);
		int[][] valueCount = cc.getValueCountMatrix();

		String subset = (rows == DECorrelationDialog.ROWS_SELECTED) ? " (selected subset)"
					  : (rows == DECorrelationDialog.ROWS_VISIBLE) ? " (visible subset)"
					  : (rows == DECorrelationDialog.ROWS_ALL) ? "" : "(list: "+rowsText+")";
		mTargetFrame = mSourceFrame.getApplication().getEmptyFrame("Correlations of "+mSourceFrame.getTitle()+subset);
		CompoundTableModel targetTableModel = mTargetFrame.getTableModel();

		targetTableModel.initializeTable(numericalColumn.length * (numericalColumn.length-1) / 2, 4);

		// build column titles
		targetTableModel.setColumnName("Column A", 0);
		targetTableModel.setColumnName("Column B", 1);
		targetTableModel.setColumnName(CorrelationCalculator.TYPE_NAME[type]+" Correlation", 2);
		targetTableModel.setColumnName("Used Value Count", 3);

		int row = 0;
		for (int i=1; i<numericalColumn.length; i++) {
			for (int j=0; j<i; j++) {
				targetTableModel.setTotalDataAt(mSourceTableModel.getColumnTitle(numericalColumn[i]).getBytes(), row, 0);
				targetTableModel.setTotalDataAt(mSourceTableModel.getColumnTitle(numericalColumn[j]).getBytes(), row, 1);
				targetTableModel.setTotalDataAt(DoubleFormat.toString(correlation[i][j]).getBytes(), row, 2);
				targetTableModel.setTotalDataAt(Integer.toString(valueCount[i][j]).getBytes(), row, 3);
				row++;
				}
			}

		targetTableModel.finalizeTable(CompoundTableEvent.cSpecifierNoRuntimeProperties, getProgressController());

		VisualizationPanel2D view = mTargetFrame.getMainFrame().getMainPane().add2DView("Correlation View", "Table\tbottom");
		view.getVisualization().setPreferredChartType(JVisualization.cChartTypeScatterPlot, -1, -1);
		view.setAxisColumnName(0, targetTableModel.getColumnTitle(0));
		view.setAxisColumnName(1, targetTableModel.getColumnTitle(1));
		view.getVisualization().setMarkerSizeColumn(2, Float.NaN, Float.NaN);
		view.getVisualization().setMarkerSizeProportional(true);
		int colorListMode = VisualizationColor.cColorListModeStraight;
		Color[] colorList = VisualizationColor.createColorWedge(new Color(0.9f, 0.9f, 1.0f), Color.blue, colorListMode, null);
		view.getVisualization().getMarkerColor().setColor(3, colorList, colorListMode);

		try {
			mTargetFrame.getMainFrame().getPruningPanel().addDoubleFilter(targetTableModel, 2);
			mTargetFrame.getMainFrame().getPruningPanel().addTextFilter(targetTableModel, JTextFilterPanel.PSEUDO_COLUMN_ALL_COLUMNS);
			}
		catch (FilterException fpe) {
			showErrorMessage(fpe.getMessage());
			}
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return mTargetFrame;
		}
	}
