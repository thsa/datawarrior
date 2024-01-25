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

package com.actelion.research.datawarrior.task.chem.dev;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

import com.actelion.research.chem.descriptor.*;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.AbstractTaskWithoutConfiguration;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;


public class DevTaskExtractPairwiseStuff extends AbstractTaskWithoutConfiguration {
    public static final String TASK_NAME = "Extract Pairwise Compound Similarities";

	private DataWarrior			mApplication;
	private DEFrame				mTargetFrame;
    private CompoundTableModel	mSourceTableModel;
    private AtomicInteger		mSMPRecordIndex,mSMPPairCount;
    private CountDownLatch		mSMPDoneSignal;

	public DevTaskExtractPairwiseStuff(DEFrame parent) {
		super(parent, true);
		mSourceTableModel = parent.getTableModel();
		mApplication = parent.getApplication();
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

		int positionCount = 0;
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++)
			if (qualifiesAsXCoordinate(column) && findMatchingYCoordinate(column) != -1)
				positionCount++;

		final int[][] positionColumn = new int[positionCount][2];
		positionCount = 0;
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++)
			if (qualifiesAsXCoordinate(column) && findMatchingYCoordinate(column) != -1) {
				positionColumn[positionCount][0] = column;
				positionColumn[positionCount][1] = findMatchingYCoordinate(column);
				positionCount++;
				}
		final boolean[] positionIsCyclic = new boolean[positionCount];
		final float[] positionColumnDataMax = new float[positionCount];
		for (int i=0; i<positionCount; i++) {
			String property = mSourceTableModel.getColumnProperty(positionColumn[i][0], CompoundTableConstants.cColumnPropertyCyclicDataMax);
			if (property != null) {
				positionIsCyclic[i] = true;
				positionColumnDataMax[i] = Float.parseFloat(property);
				}
			else if (mSourceTableModel.getMaximumValue(positionColumn[i][0]) > 0.9f
				  && mSourceTableModel.getMaximumValue(positionColumn[i][0]) < 1.1f
				  && mSourceTableModel.getMaximumValue(positionColumn[i][1]) > 0.9f
				  && mSourceTableModel.getMaximumValue(positionColumn[i][1]) < 1.1f) {
				if (mSourceTableModel.getMinimumValue(positionColumn[i][0]) > -0.1f
				 && mSourceTableModel.getMinimumValue(positionColumn[i][0]) <  0.1f
				 && mSourceTableModel.getMinimumValue(positionColumn[i][1]) > -0.1f
				 && mSourceTableModel.getMinimumValue(positionColumn[i][1]) <  0.1f) {
					positionColumnDataMax[i] = 1.0f;
					}
				else if (mSourceTableModel.getMinimumValue(positionColumn[i][0]) > -1.1f
					  && mSourceTableModel.getMinimumValue(positionColumn[i][0]) < -0.9f
					  && mSourceTableModel.getMinimumValue(positionColumn[i][1]) > -1.1f
					  && mSourceTableModel.getMinimumValue(positionColumn[i][1]) < -0.9f) {
					positionColumnDataMax[i] = 2.0f;
					}
				}

			if (positionColumnDataMax[i] == 0f)
				positionColumnDataMax[i] = (mSourceTableModel.getMaximumValue(positionColumn[i][0])
										  + mSourceTableModel.getMaximumValue(positionColumn[i][1])
										  - mSourceTableModel.getMinimumValue(positionColumn[i][0])
										  - mSourceTableModel.getMinimumValue(positionColumn[i][1])) / 2f;
			}

		final int rowCount = mSourceTableModel.getTotalRowCount();

		startProgress("Calculating Similarities...", 0, (rowCount-1)/2);
		final AtomicLongArray[][][] matchCount = new AtomicLongArray[descriptorCount][(positionCount)][101];
		for (int i=0; i<descriptorCount; i++)
			for (int j=0; j<positionCount; j++)
				for (int k=0; k<101; k++)
					matchCount[i][j][k] = new AtomicLongArray(101);

    	int threadCount = Runtime.getRuntime().availableProcessors();
    	mSMPRecordIndex = new AtomicInteger(rowCount);
    	mSMPPairCount = new AtomicInteger(0);
    	mSMPDoneSignal = new CountDownLatch(threadCount);

        for (int i=0; i<threadCount; i++) {
    		Thread t = new Thread("Similarity Calculator "+(i+1)) {
    			public void run() {
    				int row2 = mSMPRecordIndex.decrementAndGet();
    				float[] similarity = new float[descriptorColumn.length];
    				float[] distance = new float[positionColumn.length];
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
        						for (int i=0; i<positionColumn.length; i++) {
    	    						CompoundRecord r1 = mSourceTableModel.getTotalRecord(row1);
        							float x1 = r1.getDouble(positionColumn[i][0]);
        							float y1 = r1.getDouble(positionColumn[i][1]);
        							float x2 = r2.getDouble(positionColumn[i][0]);
        							float y2 = r2.getDouble(positionColumn[i][1]);
        							float dx = Math.abs(x2-x1);
        							float dy = Math.abs(y2-y1);
        							if (Float.isNaN(x1) || Float.isNaN(y1) || Float.isNaN(x2) || Float.isNaN(y2)) {
    									failed = true;
    									break;
        								}
        							if (positionIsCyclic[i]) {
        								if (dx*2f > positionColumnDataMax[i])
        									dx = positionColumnDataMax[i] - dx;
        								if (dy*2f > positionColumnDataMax[i])
        									dy = positionColumnDataMax[i] - dy;
        								}
        							distance[i] = (float)Math.sqrt(dx*dx+dy*dy) / positionColumnDataMax[i];
        							}
    							}

    						if (!failed) {
        						for (int i=0; i<descriptorColumn.length; i++) {
        							int descriptorCountIndex = (int)(100f*similarity[i]+0.5);
            						for (int j=0; j<positionColumn.length; j++) {
            							int distanceCountIndex = (int)(100f*distance[j]+0.5);
            							if (distanceCountIndex <= 100)
            								matchCount[i][j][descriptorCountIndex].incrementAndGet(distanceCountIndex);
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
	        final String[] columnName = { "Type", "Similarity", "Distance", "Count" };
	        targetTableModel.initializeTable(descriptorColumn.length * positionColumn.length * 101 * 101, columnName.length);
	        for (int column=0; column<columnName.length; column++)
	        	targetTableModel.setColumnName(columnName[column], column);

	        int row = 0;
			for (int i=0; i<descriptorColumn.length; i++) {
				String descriptorName = mSourceTableModel.getDescriptorHandler(descriptorColumn[i]).getInfo().shortName;
				for (int j=0; j<positionColumn.length; j++) {
					String positionName = mSourceTableModel.getColumnTitle(positionColumn[j][0]).replace("X", "XY");
					for (int k=0; k<=100; k++) {
						for (int l=0; l<=100; l++) {
							targetTableModel.setTotalValueAt(descriptorName+"|"+positionName, row, 0);
							targetTableModel.setTotalValueAt(""+k, row, 1);
							targetTableModel.setTotalValueAt(""+l, row, 2);
							targetTableModel.setTotalValueAt(""+matchCount[i][j][k].get(l), row, 3);
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
		return dh instanceof DescriptorHandlerFFP512
			|| dh instanceof DescriptorHandlerPFP512
			|| dh instanceof DescriptorHandlerHashedCFp
			|| dh instanceof DescriptorHandlerSkeletonSpheres
			|| dh instanceof DescriptorHandlerFunctionalGroups
			|| dh instanceof DescriptorHandlerReactionFP
			|| dh instanceof DescriptorHandlerIntVector
			|| dh instanceof DescriptorHandlerFlexophore;
		}

	private boolean qualifiesAsXCoordinate(int column) {
		String title = mSourceTableModel.getColumnTitle(column);
		return title.contains("X");
		}

	private int findMatchingYCoordinate(int column) {
		String title = mSourceTableModel.getColumnTitle(column).replace('X', 'Y');
		for (int i=0; i<mSourceTableModel.getTotalColumnCount(); i++)
			if (mSourceTableModel.getColumnTitle(i).equals(title))
				return i;
		return -1;
		}
	}
