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

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

import com.actelion.research.chem.descriptor.*;
import com.actelion.research.chem.descriptor.DescriptorHandlerIntVector;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.AbstractTaskWithoutConfiguration;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;


public class DETestExtractPairwiseCompoundSimilarities extends AbstractTaskWithoutConfiguration {
    public static final String TASK_NAME = "Extract Pairwise Compound Similarities";

	private DataWarrior			mApplication;
	private DEFrame				mTargetFrame;
    private CompoundTableModel	mSourceTableModel;
    private AtomicInteger		mSMPRecordIndex,mSMPPairCount;
    private CountDownLatch		mSMPDoneSignal;

	public DETestExtractPairwiseCompoundSimilarities(DEFrame parent, DataWarrior application) {
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
		final AtomicLongArray[] similarityCount = new AtomicLongArray[descriptorCount];
		final AtomicLongArray[][] ySimilaritySum = new AtomicLongArray[descriptorCount][(descriptorCount)];
		for (int i=0; i<descriptorCount; i++)
			similarityCount[i] = new AtomicLongArray(101);
		for (int i=0; i<descriptorCount; i++)
			for (int j=0; j<descriptorCount; j++)
				if (i != j)
					ySimilaritySum[i][j] = new AtomicLongArray(101);

    	int threadCount = Runtime.getRuntime().availableProcessors();
    	mSMPRecordIndex = new AtomicInteger(rowCount);
    	mSMPPairCount = new AtomicInteger(0);
    	mSMPDoneSignal = new CountDownLatch(threadCount);

        for (int i=0; i<threadCount; i++) {
    		Thread t = new Thread("Similarity Calculator "+(i+1)) {
    			public void run() {
    				int row2 = mSMPRecordIndex.decrementAndGet();
    				float[] similarity = new float[descriptorColumn.length];
    				while (row2 >= 1) {
    					CompoundRecord r2 = mSourceTableModel.getTotalRecord(row2);
    					for (int row1=0; row1<row2 && !threadMustDie(); row1++) {
    						if (mSMPPairCount.incrementAndGet() % rowCount == 0)
    							updateProgress(-1);

    						boolean failed = false;
    						for (int i=0; i<descriptorColumn.length; i++) {
    							similarity[i] = Float.NaN;
	    						CompoundRecord r1 = mSourceTableModel.getTotalRecord(row1);
								if (r1.getData(descriptorColumn[i]) != null
								 && r2.getData(descriptorColumn[i]) != null)
									similarity[i] = mSourceTableModel.getDescriptorSimilarity(r1, r2, descriptorColumn[i]);
								if (Float.isNaN(similarity[i])) {
									failed = true;
									break;
									}
								}

    						if (!failed) {
        						for (int i=0; i<descriptorColumn.length; i++) {
        							int countIndex = (int)(100f*similarity[i]+0.5);
        							similarityCount[i].incrementAndGet(countIndex);
            						for (int j=0; j<descriptorColumn.length; j++) {
            							if (i != j) {
            								ySimilaritySum[i][j].addAndGet(countIndex, (long)(100f*similarity[j]+0.5));
            								}
            							}
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
	        final String[] columnName = { "xDescriptor", "xSimilarity", "yDescriptor", "ySimilarity", "Count" };
	        targetTableModel.initializeTable(descriptorColumn.length * (descriptorColumn.length-1) * 101, columnName.length);
	        for (int column=0; column<columnName.length; column++)
	        	targetTableModel.setColumnName(columnName[column], column);

	        int row = 0;
			for (int i=0; i<descriptorColumn.length; i++) {
				String xShortName = mSourceTableModel.getDescriptorHandler(descriptorColumn[i]).getInfo().shortName;
				for (int j=0; j<descriptorColumn.length; j++) {
					if (i != j) {
						String yShortName = mSourceTableModel.getDescriptorHandler(descriptorColumn[j]).getInfo().shortName;
						for (int k=0; k<=100; k++) {
							targetTableModel.setTotalValueAt(xShortName, row, 0);
							targetTableModel.setTotalValueAt(""+k, row, 1);
							targetTableModel.setTotalValueAt(yShortName, row, 2);
							if (similarityCount[i].get(k) == 0)
								targetTableModel.setTotalValueAt("", row, 3);
							else
								targetTableModel.setTotalValueAt(""+ySimilaritySum[i][j].get(k)/similarityCount[i].get(k), row, 3);
							targetTableModel.setTotalValueAt(""+similarityCount[i].get(k), row, 4);
							row++;
							}
						}
					}
				}

	        targetTableModel.finalizeTable(CompoundTableEvent.cSpecifierDefaultFiltersAndViews, this);
			}
		}

	private boolean qualifiesAsDescriptorColumn(int column) {
		if (!mSourceTableModel.isDescriptorColumn(column))
			return false;
		DescriptorHandler<?,?> dh = mSourceTableModel.getDescriptorHandler(column);
		return dh instanceof DescriptorHandlerLongFFP512
			|| dh instanceof DescriptorHandlerLongPFP512
			|| dh instanceof DescriptorHandlerLongCFP
			|| dh instanceof DescriptorHandlerSkeletonSpheres
			|| dh instanceof DescriptorHandlerFunctionalGroups
			|| dh instanceof DescriptorHandlerReactionFP
			|| dh instanceof DescriptorHandlerIntVector
			|| dh instanceof DescriptorHandlerFlexophore;
		}
	}
