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

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableDetailHandler;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationPanel2D;
import info.clearthought.layout.TableLayout;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.*;


public class DETaskNewFileFromReversePivoting extends ConfigurableTask {
	private static final String PROPERTY_DATA_COLUMNS = "dataColumns";
	private static final String PROPERTY_GROUP_COLUMN_NAME = "groupColumnName";
	private static final String PROPERTY_DATA_COLUMN_NAME = "dataColumnName";

	public static final String TASK_NAME = "New File From Reverse-Pivoting";

	private CompoundTableModel  mSourceTableModel;
	private JList				mDataColumns;
	private JTextField			mTextFieldGroupColumnName,mTextFieldDataColumnName;
	private DEFrame				mSourceFrame,mTargetFrame;
	private DataWarrior		mApplication;

	public DETaskNewFileFromReversePivoting(DEFrame sourceFrame, DataWarrior application) {
		super(sourceFrame, false);
		mSourceFrame = sourceFrame;
		mSourceTableModel = sourceFrame.getTableModel();
		mApplication = application;
		}

	@Override
	public JPanel createDialogContent() {
		double[][] size = { {8, TableLayout.PREFERRED, 16, TableLayout.PREFERRED, 16, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 32,
								TableLayout.PREFERRED, 8, TableLayout.PREFERRED, TableLayout.FILL, 8} };
		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		try {
			final BufferedImage pivotImage = ImageIO.read(new BufferedInputStream(getClass().getResourceAsStream("/images/depivot.png")));
			content.add(new JPanel() {
				private static final long serialVersionUID = 0x20131213;
	
				@Override
	            protected void paintComponent(Graphics g) {
	                super.paintComponent(g);
	                g.drawImage(pivotImage, (getWidth()-pivotImage.getWidth())/2, (getHeight()-pivotImage.getHeight())/2, this);
	            	}
	
	            @Override
	            public Dimension getPreferredSize() {
	                return new Dimension(pivotImage.getWidth(), pivotImage.getHeight());
	            	}
	 			}, "1,3,1,8");
			}
		catch (IOException ioe) {}

		Comparator<String> itemComparator = new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
				return s1.compareToIgnoreCase(s2);
				}
			};

		JLabel dataLabel = new JLabel("Data column(s):");
		dataLabel.setBackground(new Color(212, 180, 0));
		dataLabel.setOpaque(true);
		content.add(dataLabel, "3,1");
		ArrayList<String> dataColumnList = new ArrayList<String>();
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++)
			if (mSourceTableModel.getColumnSpecialType(column) == null)
				dataColumnList.add(mSourceTableModel.getColumnTitle(column));
		String[] dataItemList = dataColumnList.toArray(new String[0]);
		Arrays.sort(dataItemList, itemComparator);
		mDataColumns = new JList(dataItemList);
		JScrollPane scrollPane3 = new JScrollPane(mDataColumns);
		scrollPane3.setPreferredSize(new Dimension(200,200));
		content.add(scrollPane3, "3,3,3,8");

		JLabel columnNameLabel = new JLabel("Column name for groups:");
		columnNameLabel.setBackground(new Color(212, 104, 0));
		columnNameLabel.setOpaque(true);
		content.add(columnNameLabel, "5,1");
		mTextFieldGroupColumnName = new JTextField(16);
		content.add(mTextFieldGroupColumnName, "5,3");
		JLabel dataNameLabel = new JLabel("Column name for data:");
		dataNameLabel.setBackground(new Color(212, 180, 0));
		dataNameLabel.setOpaque(true);
		content.add(dataNameLabel, "5,5");
		mTextFieldDataColumnName = new JTextField(16);
		content.add(mTextFieldDataColumnName, "5,7");

		return content;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/data.html#ReversePivoting";
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isConfigurable() {
		if (mSourceTableModel.getTotalColumnCount() < 3) {
			showErrorMessage("Less than 3 columns found.");
			return false;
			}

		int columns = 0;
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++)
			if (mSourceTableModel.getColumnSpecialType(column) == null)
				columns++;

		if (columns < 2) {
			showErrorMessage("Less than 2 potential data columns found.");
			return false;
			}

		return true;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties p = new Properties();
		String dataColumnNames = getSelectedColumnsFromList(mDataColumns, mSourceTableModel);
		if (dataColumnNames != null)
			p.setProperty(PROPERTY_DATA_COLUMNS, dataColumnNames);

		p.setProperty(PROPERTY_GROUP_COLUMN_NAME, mTextFieldGroupColumnName.getText().length() == 0 ? "Kind" : mTextFieldGroupColumnName.getText());
		p.setProperty(PROPERTY_DATA_COLUMN_NAME, mTextFieldDataColumnName.getText().length() == 0 ? "Value" : mTextFieldDataColumnName.getText());

		return p;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		selectColumnsInList(mDataColumns, configuration.getProperty(PROPERTY_DATA_COLUMNS), mSourceTableModel);
		mTextFieldGroupColumnName.setText(configuration.getProperty(PROPERTY_GROUP_COLUMN_NAME, "Kind"));
		mTextFieldDataColumnName.setText(configuration.getProperty(PROPERTY_DATA_COLUMN_NAME, "Value"));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		StringBuilder columns = new StringBuilder();
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++) {
			if (mSourceTableModel.getColumnSpecialType(column) == null
			 && mSourceTableModel.isColumnTypeDouble(column)) {
				if (columns.length() != 0)
					columns.append('\t');
				columns.append(mSourceTableModel.getColumnTitle(column));
				}
			}

		selectColumnsInList(mDataColumns, columns.toString(), mSourceTableModel);
		mTextFieldGroupColumnName.setText("Kind");
		mTextFieldDataColumnName.setText("Value");
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String dataColumnList = configuration.getProperty(PROPERTY_DATA_COLUMNS);
		if (dataColumnList == null) {
			showErrorMessage("No data column(s) defined.");
			return false;
			}

		if (isLive) {
			String[] dataColumnName = dataColumnList.split("\\t");
			for (int i=0; i<dataColumnName.length; i++) {
				int column = mSourceTableModel.findColumn(dataColumnName[i]);
				if (column == -1) {
					showErrorMessage("Column '"+dataColumnName[i]+"' not found.");
					return false;
					}
				if (mSourceTableModel.getColumnSpecialType(column) != null) {
					showErrorMessage("Column '"+dataColumnName[i]+"' does not qualify as data column.");
					return false;
					}
				}
			}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		String[] dataColumnName = configuration.getProperty(PROPERTY_DATA_COLUMNS).split("\\t");
		int[] dataColumn = new int[dataColumnName.length];
		for (int i=0; i<dataColumnName.length; i++)
			dataColumn[i] = mSourceTableModel.findColumn(dataColumnName[i]);

		int newColumnCount = mSourceTableModel.getTotalColumnCount()-dataColumn.length+2;
		ArrayList<Object[]> rowList = new ArrayList<Object[]>();
		ArrayList<DetailInfo> detailList = new ArrayList<DetailInfo>();

        boolean[] isDataColumn = new boolean[mSourceTableModel.getTotalColumnCount()];
		for (int column:dataColumn)
			isDataColumn[column] = true;

		for (int row=0; row<mSourceTableModel.getTotalRowCount(); row++) {
        	CompoundRecord record = mSourceTableModel.getTotalRecord(row);

        	for (int currentDataColumn:dataColumn) {
        		byte[] currentData = (byte[])record.getData(currentDataColumn);
        		if (currentData != null) {
        			Object[] rowData = new Object[newColumnCount];
        			int targetColumn = 0;
        			for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++) {
        				if (!isDataColumn[column]) {
                			Object cellData = record.getData(column);
                			if (cellData != null) {
                				if (cellData instanceof byte[])
                					rowData[targetColumn] = ((byte[])cellData).clone();
                				else if (cellData instanceof int[])
                					rowData[targetColumn] = ((int[])cellData).clone();
                				}
							addDetailReferences(record.getDetailReferences(column), rowList.size(), targetColumn, detailList);
                			targetColumn++;
        					}
        				}
    				rowData[targetColumn++] = mSourceTableModel.getColumnTitle(currentDataColumn).getBytes().clone();
    				rowData[targetColumn] = currentData.clone();
					addDetailReferences(record.getDetailReferences(currentDataColumn), rowList.size(), targetColumn, detailList);

        			rowList.add(rowData);
        			}
        		}
        	}

        mTargetFrame = mApplication.getEmptyFrame("Reverse-Pivoting of "+mSourceFrame.getTitle());
        CompoundTableModel targetTableModel = mTargetFrame.getTableModel();
        targetTableModel.initializeTable(rowList.size(), newColumnCount);

        // build column titles
		int targetColumn = 0;
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++) {
			if (!isDataColumn[column]) {
				targetTableModel.setColumnName(mSourceTableModel.getColumnTitleNoAlias(column), targetColumn);
				mSourceTableModel.copyColumnProperties(column, targetColumn, targetTableModel, true);

	        	targetColumn++;
				}
			}
		targetTableModel.setColumnName(configuration.getProperty(PROPERTY_GROUP_COLUMN_NAME, "Kind"), targetColumn++);
		targetTableModel.setColumnName(configuration.getProperty(PROPERTY_DATA_COLUMN_NAME, "Value"), targetColumn);
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++)
			if (isDataColumn[column])
				mSourceTableModel.copyColumnProperties(column, targetColumn, targetTableModel, false);

        // set cell values
        int row = 0;
        for (Object[] rowData:rowList) {
        	int column = 0;
        	for (Object data:rowData)
        		targetTableModel.setTotalDataAt(data, row, column++);

        	row++;
        	}

		for (DetailInfo di:detailList)
        	targetTableModel.getTotalRecord(di.row).setDetailReferences(di.column, di.ref);

		HashMap<String,byte[]> sourceMap = mSourceTableModel.getDetailHandler().getEmbeddedDetailMap();
		if (sourceMap != null) {
			HashMap<String,byte[]> targetMap = new HashMap<String,byte[]>();
			for (String key:sourceMap.keySet())
				targetMap.put(key,sourceMap.get(key).clone());
			targetTableModel.getDetailHandler().setEmbeddedDetailMap(targetMap);
			}

		targetTableModel.finalizeTable(CompoundTableEvent.cSpecifierNoRuntimeProperties, getProgressController());

        VisualizationPanel2D view = mTargetFrame.getMainFrame().getMainPane().add2DView("2D View", null);
        view.getVisualization().setPreferredChartType(JVisualization.cChartTypeBoxPlot, -1, -1);
        view.setAxisColumnName(0, targetTableModel.getColumnTitle(newColumnCount-2));
        view.setAxisColumnName(1, targetTableModel.getColumnTitle(newColumnCount-1));
		}

	private void addDetailReferences(String[][] detailRef, int row, int column, ArrayList<DetailInfo> detailList) {
		if (detailRef != null)
			detailList.add(new DetailInfo(row, column, detailRef));
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return mTargetFrame;
		}

	private class DetailInfo {
		int row,column;
		String[][] ref;

		public DetailInfo(int row, int column, String[][] detailRef) {
			this.row = row;
			this.column = column;
			ref = new String[detailRef.length][];
			for (int i=0; i<detailRef.length; i++) {
				ref[i] = new String[detailRef[i].length];
				for (int j=0; j<detailRef[i].length; j++)
					ref[i][j] = new String(detailRef[i][j]);
				}
			}
		}
	}
