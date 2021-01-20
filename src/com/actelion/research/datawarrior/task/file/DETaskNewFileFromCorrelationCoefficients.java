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

	public static final String TASK_NAME = "New File From Correlation Coefficient";

	private CompoundTableModel	mSourceTableModel;
	private JComboBox			mComboBoxCorrelationType;
	private DEFrame				mSourceFrame,mTargetFrame;
	private int					mCorrelationType;

	public DETaskNewFileFromCorrelationCoefficients(DEFrame sourceFrame, int correlationType) {
		super(sourceFrame, false);
		mSourceFrame = sourceFrame;
		mSourceTableModel = sourceFrame.getTableModel();
		mCorrelationType = correlationType;
		}

	@Override
	public JPanel createDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap}, {8, TableLayout.PREFERRED, 8} };
		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		content.add(new JLabel("Correlation Coefficient:"), "1,1");
		mComboBoxCorrelationType = new JComboBox(CorrelationCalculator.TYPE_LONG_NAME);
		content.add(mComboBoxCorrelationType, "3,1");

		return content;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		if (mCorrelationType == -1)
			return null;

		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_TYPE, CorrelationCalculator.TYPE_CODE[mCorrelationType]);
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
		return p;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		int type = findListIndex(configuration.getProperty(PROPERTY_TYPE), CorrelationCalculator.TYPE_CODE, 0);
		mComboBoxCorrelationType.setSelectedIndex(type);
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mComboBoxCorrelationType.setSelectedIndex(0);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		mTargetFrame = mSourceFrame.getApplication().getEmptyFrame("Correlations of "+mSourceFrame.getTitle());
		CompoundTableModel targetTableModel = mTargetFrame.getTableModel();

		int[] numericalColumn = DECorrelationDialog.getNumericalColumns(mSourceTableModel);

		targetTableModel.initializeTable(numericalColumn.length * (numericalColumn.length-1) / 2, 4);

		int type = findListIndex(configuration.getProperty(PROPERTY_TYPE), CorrelationCalculator.TYPE_CODE, 0);
		NumericalCompoundTableColumn[] nc = new NumericalCompoundTableColumn[numericalColumn.length];
		for (int i=0; i<numericalColumn.length; i++)
			nc[i] = new NumericalCompoundTableColumn(mSourceTableModel, numericalColumn[i]);
		CorrelationCalculator cc = new CorrelationCalculator();
		double[][] correlation = cc.calculateMatrix(nc, type);
		int[][] valueCount = cc.getValueCountMatrix();

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
