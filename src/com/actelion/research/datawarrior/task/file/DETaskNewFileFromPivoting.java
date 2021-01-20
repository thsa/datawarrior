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
import com.actelion.research.datawarrior.DEPruningPanel.FilterException;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationPanel2D;
import com.actelion.research.util.ByteArrayArrayComparator;
import info.clearthought.layout.TableLayout;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.*;


public class DETaskNewFileFromPivoting extends ConfigurableTask {
	private static final String PROPERTY_GROUP_COLUMNS = "groupColumns";
	private static final String PROPERTY_SPLIT_COLUMNS = "splitColumns";
	private static final String PROPERTY_DATA_COLUMNS = "dataColumns";

	public static final String TASK_NAME = "New File From Pivoting";

	private CompoundTableModel
	mSourceTableModel;
	private JList				mGroupColumns,mSplitColumns,mDataColumns;
	private DEFrame				mSourceFrame,mTargetFrame;
	private DataWarrior		mApplication;

	public DETaskNewFileFromPivoting(DEFrame sourceFrame, DataWarrior application) {
		super(sourceFrame, false);
		mSourceFrame = sourceFrame;
		mSourceTableModel = sourceFrame.getTableModel();
		mApplication = application;
		}

	@Override
	public JPanel createDialogContent() {
		double[][] size = { {8, TableLayout.PREFERRED, 16, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8} };
		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		try {
			final BufferedImage pivotImage = ImageIO.read(new BufferedInputStream(getClass().getResourceAsStream("/images/pivot.png")));
			content.add(new JPanel() {
				private static final long serialVersionUID = 0x20131016;
	
				@Override
	            protected void paintComponent(Graphics g) {
	                super.paintComponent(g);
	                g.drawImage(pivotImage, (getWidth()-pivotImage.getWidth())/2, (getHeight()-pivotImage.getHeight())/2, this);
	            	}
	
	            @Override
	            public Dimension getPreferredSize() {
	                return new Dimension(pivotImage.getWidth(), pivotImage.getHeight());
	            	}
	 			}, "1,3");
			}
		catch (IOException ioe) {}

		Comparator<String> itemComparator = new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
				return s1.compareToIgnoreCase(s2);
				}
			};

		JLabel groupLabel = new JLabel("Group rows sharing:");
		groupLabel.setBackground(new Color(136, 136, 232));
		groupLabel.setOpaque(true);
		content.add(groupLabel, "3,1");
		ArrayList<String> groupColumnList = new ArrayList<String>();
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++)
			if (mSourceTableModel.isColumnTypeCategory(column))
				groupColumnList.add(mSourceTableModel.getColumnTitle(column));
		String[] groupItemList = groupColumnList.toArray(new String[0]);
		Arrays.sort(groupItemList, itemComparator);
		mGroupColumns = new JList(groupItemList);
		JScrollPane scrollPane1 = new JScrollPane(mGroupColumns);
		scrollPane1.setPreferredSize(new Dimension(200,200));
		content.add(scrollPane1, "3,3");

		JLabel splitLabel = new JLabel("Split columns by:");
		splitLabel.setBackground(new Color(212, 104, 0));
		splitLabel.setOpaque(true);
		content.add(splitLabel, "5,1");
		ArrayList<String> splitColumnList = new ArrayList<String>();
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++)
			if (mSourceTableModel.isColumnTypeCategory(column)
			 && mSourceTableModel.getColumnSpecialType(column) == null)
				splitColumnList.add(mSourceTableModel.getColumnTitle(column));
		String[] splitItemList = splitColumnList.toArray(new String[0]);
		Arrays.sort(splitItemList, itemComparator);
		mSplitColumns = new JList(splitColumnList.toArray(new String[0]));
		JScrollPane scrollPane2 = new JScrollPane(mSplitColumns);
		scrollPane2.setPreferredSize(new Dimension(200,200));
		content.add(scrollPane2, "5,3");

		JLabel dataLabel = new JLabel("Data column(s):");
		dataLabel.setBackground(new Color(212, 180, 0));
		dataLabel.setOpaque(true);
		content.add(dataLabel, "7,1");
		ArrayList<String> dataColumnList = new ArrayList<String>();
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++)
			if (mSourceTableModel.getColumnSpecialType(column) == null)
				dataColumnList.add(mSourceTableModel.getColumnTitle(column));
		String[] dataItemList = dataColumnList.toArray(new String[0]);
		Arrays.sort(dataItemList, itemComparator);
		mDataColumns = new JList(dataItemList);
		JScrollPane scrollPane3 = new JScrollPane(mDataColumns);
		scrollPane3.setPreferredSize(new Dimension(200,200));
		content.add(scrollPane3, "7,3");

		return content;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/analysis.html#Pivoting";
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

		int categoryColumns = 0;
		int nonCategoryColumns = 0;
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++)
			if (mSourceTableModel.isColumnTypeCategory(column))
				categoryColumns++;
			else if (mSourceTableModel.getColumnSpecialType(column) == null)
				nonCategoryColumns++;

		if (categoryColumns < 2) {
			showErrorMessage("Less than 2 category columns found.");
			return false;
			}
		if (nonCategoryColumns == 0 && categoryColumns == 2) {
			showErrorMessage("No potential data column found.");
			return false;
			}

		return true;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties p = new Properties();
		String groupColumnNames = getSelectedColumnsFromList(mGroupColumns, mSourceTableModel);
		if (groupColumnNames != null)
			p.setProperty(PROPERTY_GROUP_COLUMNS, groupColumnNames);

		String splitColumnNames = getSelectedColumnsFromList(mSplitColumns, mSourceTableModel);
		if (splitColumnNames != null)
			p.setProperty(PROPERTY_SPLIT_COLUMNS, splitColumnNames);

		String dataColumnNames = getSelectedColumnsFromList(mDataColumns, mSourceTableModel);
		if (dataColumnNames != null)
			p.setProperty(PROPERTY_DATA_COLUMNS, dataColumnNames);

		return p;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		selectColumnsInList(mGroupColumns, configuration.getProperty(PROPERTY_GROUP_COLUMNS), mSourceTableModel);
		selectColumnsInList(mSplitColumns, configuration.getProperty(PROPERTY_SPLIT_COLUMNS), mSourceTableModel);
		selectColumnsInList(mDataColumns, configuration.getProperty(PROPERTY_DATA_COLUMNS), mSourceTableModel);
		}

	@Override
	public void setDialogConfigurationToDefault() {
		int splitColumn = -1;
		int minCategoryCount = Integer.MAX_VALUE;
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++) {
			if (mSourceTableModel.isColumnTypeCategory(column)
			 && mSourceTableModel.getColumnSpecialType(column) == null) {
				if (minCategoryCount > mSourceTableModel.getCategoryCount(column)) {
					minCategoryCount = mSourceTableModel.getCategoryCount(column);
					splitColumn = column;
					}
				}
			}

		int groupColumn = -1;
		float maxFactor = 0f;
		float optCategoryCount = (float)mSourceTableModel.getTotalRowCount() / (float)minCategoryCount;
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++) {
			if (column != splitColumn
			 && mSourceTableModel.isColumnTypeCategory(column)) {
				float factor = optCategoryCount / mSourceTableModel.getCategoryCount(column);
				if (factor > 0)
					factor = 1 / factor;
				if (maxFactor < factor) {
					maxFactor = factor;
					groupColumn = column;
					}
				}
			}

		int dataColumn = -1;
		int maxRank = 0;
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++) {
			if (column != splitColumn && column != groupColumn
			 && mSourceTableModel.getColumnSpecialType(column) == null) {
				int rank = (mSourceTableModel.isColumnTypeDouble(column) ? 16 : 0)
						 + (mSourceTableModel.isColumnTypeCategory(column) ? 0 : 8)
						 + (mSourceTableModel.isColumnDataComplete(column) ? 4 : 0);
				if (maxRank < rank) {
					maxRank = rank;
					dataColumn = column;
					}
				}
			}

		selectColumnsInList(mGroupColumns, mSourceTableModel.getColumnTitle(groupColumn), mSourceTableModel);
		selectColumnsInList(mSplitColumns, mSourceTableModel.getColumnTitle(splitColumn), mSourceTableModel);
		selectColumnsInList(mDataColumns, mSourceTableModel.getColumnTitle(dataColumn), mSourceTableModel);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String groupColumnList = configuration.getProperty(PROPERTY_GROUP_COLUMNS);
		if (groupColumnList == null) {
			showErrorMessage("No identifier column(s) for grouping rows defined.");
			return false;
			}
		String splitColumnList = configuration.getProperty(PROPERTY_SPLIT_COLUMNS);
		if (splitColumnList == null) {
			showErrorMessage("No category column(s) for splitting data defined.");
			return false;
			}
		String dataColumnList = configuration.getProperty(PROPERTY_DATA_COLUMNS);
		if (dataColumnList == null) {
			showErrorMessage("No data column(s) defined.");
			return false;
			}

		if (!isLive)
			return true;

		boolean[] isInUse = new boolean[mSourceTableModel.getTotalColumnCount()];

		String[] groupColumnName = groupColumnList.split("\\t");
		for (int i=0; i<groupColumnName.length; i++) {
			int column = mSourceTableModel.findColumn(groupColumnName[i]);
			if (column == -1) {
				showErrorMessage("Column '"+groupColumnName[i]+"' not found.");
				return false;
				}
			if (!mSourceTableModel.isColumnTypeCategory(column)) {
				showErrorMessage("Column '"+groupColumnName[i]+"' doesn't contain categories.");
				return false;
				}
			isInUse[column] = true;
			}

		String[] splitColumnName = splitColumnList.split("\\t");
		for (int i=0; i<splitColumnName.length; i++) {
			int column = mSourceTableModel.findColumn(splitColumnName[i]);
			if (column == -1) {
				showErrorMessage("Column '"+splitColumnName[i]+"' not found.");
				return false;
				}
			if (!mSourceTableModel.isColumnTypeCategory(column)) {
				showErrorMessage("Column '"+splitColumnName[i]+"' doesn't contain categories.");
				return false;
				}
			if (isInUse[column]) {
				showErrorMessage("Column '"+splitColumnName[i]+"' is assigned twice.");
				return false;
				}
			isInUse[column] = true;
			}

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
			if (isInUse[column]) {
				showErrorMessage("Column '"+dataColumnName[i]+"' is assigned twice.");
				return false;
				}
			}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		String[] groupColumnName = configuration.getProperty(PROPERTY_GROUP_COLUMNS).split("\\t");
		int[] groupColumn = new int[groupColumnName.length];
		for (int i=0; i<groupColumnName.length; i++)
			groupColumn[i] = mSourceTableModel.findColumn(groupColumnName[i]);

		String[] splitColumnName = configuration.getProperty(PROPERTY_SPLIT_COLUMNS).split("\\t");
		int[] splitColumn = new int[splitColumnName.length];
		for (int i=0; i<splitColumnName.length; i++)
			splitColumn[i] = mSourceTableModel.findColumn(splitColumnName[i]);

		String[] dataColumnName = configuration.getProperty(PROPERTY_DATA_COLUMNS).split("\\t");
		int[] dataColumn = new int[dataColumnName.length];
		for (int i=0; i<dataColumnName.length; i++)
			dataColumn[i] = mSourceTableModel.findColumn(dataColumnName[i]);

		int categoryCount = 1;
		for (int i=0; i<splitColumn.length; i++)
			categoryCount *= mSourceTableModel.getCategoryCount(splitColumn[i]);

		TreeMap<byte[][],byte[][]> newRowMap = new TreeMap<byte[][],byte[][]>(new ByteArrayArrayComparator());

        for (int row=0; row<mSourceTableModel.getTotalRowCount(); row++) {
        	CompoundRecord record = mSourceTableModel.getTotalRecord(row);

        	byte[][] key = new byte[groupColumn.length][];
        	for (int i=0; i<groupColumn.length; i++)
        		key[i] = (byte[])record.getData(groupColumn[i]);

        	byte[][] newRecord = newRowMap.get(key);
        	if (newRecord == null) {
        		newRecord = new byte[groupColumn.length + categoryCount*dataColumn.length][];
            	newRowMap.put(key, newRecord);
            	int i = 0;
            	for (byte[] id:key)
            		newRecord[i++] = id;
        		}

    		int category = mSourceTableModel.getCategoryIndex(splitColumn[0], record);
    		for (int i=1; i<splitColumn.length; i++)
    			category = category * mSourceTableModel.getCategoryCount(splitColumn[i])
    								+ mSourceTableModel.getCategoryIndex(splitColumn[i], record);

        	for (int i=0; i<dataColumn.length; i++) {
        		int column = groupColumn.length + i*categoryCount + category;
        		newRecord[column] = append(newRecord[column], (byte[])record.getData(dataColumn[i]));
        		}
        	}

        // determine, which target columns are really populated with some data (category and data columns)
        boolean[] isUsedTargetColumn = new boolean[groupColumn.length+categoryCount*dataColumn.length];
        for (byte[][] newRecord:newRowMap.values())
        	for (int i=0; i<newRecord.length; i++)
        		if (newRecord[i] != null)
        			isUsedTargetColumn[i] = true;

        // create map to translate target column index to a used target column index
        int usedTargetColumnCount = 0;
        int[] usedTargetColumn = new int[isUsedTargetColumn.length];
        for (int i=0; i<isUsedTargetColumn.length; i++)
        	if (isUsedTargetColumn[i])
        		usedTargetColumn[i] = usedTargetColumnCount++;
        	else
        		usedTargetColumn[i] = -1;

        mTargetFrame = mApplication.getEmptyFrame("Pivoting of "+mSourceFrame.getTitle());
        CompoundTableModel targetTableModel = mTargetFrame.getTableModel();
        targetTableModel.initializeTable(newRowMap.size(), usedTargetColumnCount);

        // build column titles
        int column = 0;
		for (int i=0; i<groupColumn.length; i++) {
			if (isUsedTargetColumn[i])
				targetTableModel.setColumnName(mSourceTableModel.getColumnTitleNoAlias(groupColumn[i]), usedTargetColumn[column]);
			column++;
			}
		for (int i=0; i<dataColumn.length; i++) {
			int[] index = new int[splitColumn.length];
			int[] count = new int[splitColumn.length];
			for (int j=0; j<splitColumn.length; j++)
				count[j] = mSourceTableModel.getCategoryCount(splitColumn[j]);

			while (index[0] < count[0]) {
				StringBuilder title = new StringBuilder(mSourceTableModel.getCategoryList(splitColumn[0])[index[0]]);

				for (int j=1; j<splitColumn.length; j++) {
					title.append(" | ");
					title.append(mSourceTableModel.getCategoryList(splitColumn[j])[index[j]]);
					}

				if (dataColumn.length > 1) {
					title.append(" - ");
					title.append(mSourceTableModel.getColumnTitle(dataColumn[i]));
					}

				if (isUsedTargetColumn[column])
					targetTableModel.setColumnName(title.toString(), usedTargetColumn[column]);
				column++;

				int j = splitColumn.length-1;
				index[j]++;
				while (j>0) {
					if (index[j] < count[j])
						break;
					index[j] = 0;
					index[--j]++;
					}
				}
			}

        // set cell values
        int row = 0;
        for (byte[][] key:newRowMap.keySet()) {
        	byte[][] value = newRowMap.get(key);
        	for (int i=0; i<value.length; i++) {
        		if (isUsedTargetColumn[i])
        			targetTableModel.setTotalDataAt(value[i], row, usedTargetColumn[i]);
        		}

        	row++;
        	}

        for (int i=0; i<groupColumn.length; i++) {
        	if (isUsedTargetColumn[i]) {
		        mSourceTableModel.copyColumnProperties(groupColumn[i], usedTargetColumn[i], targetTableModel, true);
        		}
        	}
        for (int i=0; i<dataColumn.length; i++) {
        	for (int j=0; j<categoryCount; j++) {
        		column = groupColumn.length + i*categoryCount + j;
        		if (isUsedTargetColumn[column])
        			mSourceTableModel.copyColumnProperties(dataColumn[i], usedTargetColumn[column], targetTableModel, false);
        		}
        	}

        targetTableModel.finalizeTable(CompoundTableEvent.cSpecifierNoRuntimeProperties, getProgressController());

        int firstDataColumn = 0;
        for (int i=0; i<groupColumn.length; i++)
        	if (isUsedTargetColumn[i])
        		firstDataColumn++;

        VisualizationPanel2D view = mTargetFrame.getMainFrame().getMainPane().add2DView("2D View", "Table\tbottom");
        view.getVisualization().setPreferredChartType(JVisualization.cChartTypeScatterPlot, -1, -1);
        view.setAxisColumnName(0, targetTableModel.getColumnTitle(firstDataColumn));
        view.setAxisColumnName(1, targetTableModel.getColumnTitle(firstDataColumn+1));

        try {
        	mTargetFrame.getMainFrame().getPruningPanel().addCategoryBrowser(targetTableModel);
	        for (int i=0; i<groupColumn.length; i++)
	        	if (isUsedTargetColumn[i])
	        		mTargetFrame.getMainFrame().getPruningPanel().addDefaultFilter(usedTargetColumn[i]);
        	}
        catch (FilterException fpe) {
        	showErrorMessage(fpe.getMessage());
        	}
		}

	private byte[] append(byte[] b1, byte[] b2) {
		if (b1 == null)
			return b2;
		if (b2 == null)
			return b1;
		byte[] b3 = new byte[b1.length+b2.length+2];
		int i = 0;
		for (byte b:b1)
			b3[i++] = b;
		b3[i++] = ';';
		b3[i++] = ' ';
		for (byte b:b2)
			b3[i++] = b;
		return b3;
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return mTargetFrame;
		}
	}
