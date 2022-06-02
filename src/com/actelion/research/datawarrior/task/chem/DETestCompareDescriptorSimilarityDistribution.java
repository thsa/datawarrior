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

package com.actelion.research.datawarrior.task.chem;

import com.actelion.research.chem.descriptor.*;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DERuntimeProperties;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.AbstractTaskWithoutConfiguration;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;


public class DETestCompareDescriptorSimilarityDistribution extends AbstractTaskWithoutConfiguration {
	public static final String TASK_NAME = "Compare Descriptor Similarities";

	private static final String TEMPLATE = "<datawarrior properties>\n" +
			"<axisColumn_2D View_0=\"Similarity\">\n" +
			"<axisColumn_2D View_1=\"Count\">\n" +
			"<chartType_2D View=\"scatter\">\n" +
			"<colorColumn_2D View=\"Descriptor\">\n" +
			"<colorCount_2D View=\"7\">\n" +
			"<colorListMode_2D View=\"Categories\">\n" +
			"<color_2D View_0=\"-11992833\">\n" +
			"<color_2D View_1=\"-65494\">\n" +
			"<color_2D View_2=\"-16732826\">\n" +
			"<color_2D View_3=\"-256\">\n" +
			"<color_2D View_4=\"-11546120\">\n" +
			"<color_2D View_5=\"-2252554\">\n" +
			"<color_2D View_6=\"-21735\">\n" +
			"<columnFilter_Table=\"\">\n" +
			"<columnWidth_Table_Count=\"80\">\n" +
			"<columnWidth_Table_Descriptor=\"80\">\n" +
			"<columnWidth_Table_Similarity=\"80\">\n" +
			"<curveLineWidth_2D View=\"2.3631606\">\n" +
			"<curveSmoothing_2D View=\"0.63\">\n" +
			"<detailView=\"height[Data]=1\">\n" +
			"<fastRendering_2D View=\"false\">\n" +
			"<filter0=\"#browser#\t#disabled#\tDescriptor\tFlexophore\">\n" +
			"<filter1=\"#category#\tDescriptor\">\n" +
			"<filter2=\"#double#\tSimilarity\">\n" +
			"<filter3=\"#double#\tCount\">\n" +
			"<filterAnimation0=\"state=stopped\">\n" +
			"<filterAnimation2=\"state=stopped low2=80% high1=20% time=10\">\n" +
			"<filterAnimation3=\"state=stopped low2=80% high1=20% time=10\">\n" +
			"<headerLines_Table=\"2\">\n" +
			"<mainSplitting=\"0.72634\">\n" +
			"<mainView=\"2D View\">\n" +
			"<mainViewCount=\"2\">\n" +
			"<mainViewDockInfo0=\"root\">\n" +
			"<mainViewDockInfo1=\"Table\tbottom\t0.499\">\n" +
			"<mainViewName0=\"Table\">\n" +
			"<mainViewName1=\"2D View\">\n" +
			"<mainViewType0=\"tableView\">\n" +
			"<mainViewType1=\"2Dview\">\n" +
			"<markersize_2D View=\"0.1764\">\n" +
			"<meanLineMode_2D View=\"smooth\">\n" +
			"<rightSplitting=\"0.66926\">\n" +
			"<rowHeight_Table=\"16\">\n" +
			"<scaleStyle_2D View=\"frame\">\n" +
			"<scatterplotMargin_2D View=\"0.025\">\n" +
			"<showNaNValues_2D View=\"true\">\n" +
			"<splitCurveByCategory_2D View=\"true\">\n" +
			"</datawarrior properties>";

	private DataWarrior			mApplication;
	private DEFrame				mTargetFrame;
    private CompoundTableModel	mSourceTableModel;
    private AtomicInteger		mSMPRecordIndex,mSMPPairCount;
    private CountDownLatch		mSMPDoneSignal;

	public DETestCompareDescriptorSimilarityDistribution(DEFrame parent, DataWarrior application) {
		super(parent, true);
		mSourceTableModel = parent.getTableModel();
		mApplication = application;
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return mTargetFrame;
		}

	@Override
	public boolean isConfigurable() {
		boolean descriptorFound = false;

		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++) {
			if (qualifiesAsDescriptorColumn(column)) {
				descriptorFound = true;
				break;
				}
			}

		if (!descriptorFound) {
			showErrorMessage("No chemical descriptor for similarity calculation found.");
			return false;
			}

		return true;
		}


	@Override
    public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		return true;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public void runTask(final Properties configuration) {
		int descriptorCount = 0;
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++) {
			if (qualifiesAsDescriptorColumn(column)) {
	            waitForDescriptor(mSourceTableModel, column);
				if (threadMustDie())
					return;

				descriptorCount++;
				}
			}

		final int[] descriptorColumn = new int[descriptorCount];
		descriptorCount = 0;
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++)
			if (qualifiesAsDescriptorColumn(column))
				descriptorColumn[descriptorCount++] = column;

		final int rowCount = mSourceTableModel.getTotalRowCount();

		startProgress("Calculating Similarities...", 0, (rowCount-1)/2);
		final AtomicIntegerArray[] similarityCount = new AtomicIntegerArray[descriptorCount];
		for (int i=0; i<descriptorCount; i++)
			similarityCount[i] = new AtomicIntegerArray(101);

    	int threadCount = Runtime.getRuntime().availableProcessors();
    	mSMPRecordIndex = new AtomicInteger(rowCount);
    	mSMPPairCount = new AtomicInteger(0);
    	mSMPDoneSignal = new CountDownLatch(threadCount);

        for (int i=0; i<threadCount; i++) {
    		Thread t = new Thread("Similarity Calculator "+(i+1)) {
    			public void run() {
    				int row2 = mSMPRecordIndex.decrementAndGet();
    				while (row2 >= 1) {
    					CompoundRecord r2 = mSourceTableModel.getTotalRecord(row2);
    					for (int row1=0; row1<row2 && !threadMustDie(); row1++) {
    						if (mSMPPairCount.incrementAndGet() % rowCount == 0)
    							updateProgress(-1);

    						for (int descriptor=0; descriptor<descriptorColumn.length; descriptor++) {
	    						CompoundRecord r1 = mSourceTableModel.getTotalRecord(row1);
								if (r1.getData(descriptorColumn[descriptor]) != null
								 && r2.getData(descriptorColumn[descriptor]) != null) {
									float similarity = mSourceTableModel.getDescriptorSimilarity(r1, r2, descriptorColumn[descriptor]);
									similarityCount[descriptor].incrementAndGet((int)(100f*similarity+0.5));
									}
								}
    						}

    					row2 = mSMPRecordIndex.decrementAndGet();
    					}
    				mSMPDoneSignal.countDown();
    				}
    			};
    		t.setPriority(Thread.MIN_PRIORITY);
    		t.start();
    		}

		try {
			mSMPDoneSignal.await();
			}
		catch (InterruptedException e) {}


		if (!threadMustDie()) {
			startProgress("Populating new document...", 0, 0);

			mTargetFrame = mApplication.getEmptyFrame("Descriptor Similarity Comparison");
	        CompoundTableModel targetTableModel = mTargetFrame.getTableModel();
	        final String[] columnName = { "Descriptor", "Similarity", "Count" };
	        targetTableModel.initializeTable(descriptorColumn.length * 101, columnName.length);
	        for (int column=0; column<columnName.length; column++)
	        	targetTableModel.setColumnName(columnName[column], column);

	        int row = 0;
			for (int descriptor=0; descriptor<descriptorColumn.length; descriptor++) {
				String shortName = mSourceTableModel.getDescriptorHandler(descriptorColumn[descriptor]).getInfo().shortName;
				for (int i=0; i<=100; i++) {
					targetTableModel.setTotalValueAt(shortName, row, 0);
					targetTableModel.setTotalValueAt(""+i, row, 1);
					targetTableModel.setTotalValueAt(""+similarityCount[descriptor].get(i), row, 2);
					row++;
					}
				}

	        targetTableModel.finalizeTable(CompoundTableEvent.cSpecifierDefaultFiltersAndViews, this);
			}

		if (!threadMustDie()) {
			try {
				DERuntimeProperties rtp = new DERuntimeProperties(mTargetFrame.getMainFrame());
				rtp.read(new BufferedReader(new StringReader(TEMPLATE)));
				rtp.apply();
			}
			catch (IOException e) {}
			}
		}

	private boolean qualifiesAsDescriptorColumn(int column) {
		if (!mSourceTableModel.isDescriptorColumn(column))
			return false;
		DescriptorHandler<?,?> dh = mSourceTableModel.getDescriptorHandler(column);
		return dh instanceof DescriptorHandlerLongFFP512
			|| dh instanceof DescriptorHandlerLongPFP512
			|| dh instanceof DescriptorHandlerAllFragmentsFP
			|| dh instanceof DescriptorHandlerLongCFP
			|| dh instanceof DescriptorHandlerSkeletonSpheres
			|| dh instanceof DescriptorHandlerFunctionalGroups
			|| dh instanceof DescriptorHandlerIntVector
			|| dh instanceof DescriptorHandlerFlexophore;
		}
	}
