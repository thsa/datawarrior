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

import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.table.MarkerLabelDisplayer;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.*;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;


public class DETaskAnalyseActivityCliffs extends ConfigurableTask implements ItemListener,Runnable {
	private static final String PROPERTY_IDENTIFIER_COLUMN = "identifierColumn";
	private static final String PROPERTY_DESCRIPTOR_COLUMN = "descriptorColumn";
	private static final String PROPERTY_ACTIVITY_COLUMN = "activityColumn";
	private static final String PROPERTY_GROUP_BY_COLUMN = "groupBy";
	private static final String PROPERTY_SIMILARITY = "similarity";
	private static final String PROPERTY_NEW_VIEW = "createSimView";
	private static final String PROPERTY_NEW_DOCUMENT = "createNew";
	private static final String PROPERTY_VALUE_CREATE_COLUMN = "<createNew>";
	private static final String PROPERTY_VALUE_SIMILARITY_AUTOMATIC = "automatic";
	private static final String ITEM_SIMILARITY_ONLY = "<none; calculate similarity only>";
	private static final String ITEM_CREATE_COLUMN = "<create one>";
	private static final String ITEM_NONE = "<none>";

	private static final String COLUMN_NAME_ROW_ID = "Row-ID";
	private static final int AVERAGE_NEIGHBOR_COUNT = 6;
	private static final int MIN_SIMILARITY = 80;
	private static final int DEFAULT_SIMILARITY = 95;
	private static final int VIEW_CYCLE_COUNT = 20000;

	private static final String[] IDENTIFIER = { COLUMN_NAME_ROW_ID, "Idorsia No" };

	public static final String TASK_NAME = "Analyse Activity Cliffs";

	private DataWarrior			mApplication;
	private DEFrame				mParentFrame,mTargetFrame;
	private CompoundTableModel	mSourceTableModel;
	private JComboBox			mComboBoxDescriptorColumn,mComboBoxActivityColumn,mComboBoxIdentifierColumn,mComboBoxGroupByColumn;
	private JSlider				mSimilaritySlider;
	private JCheckBox			mCheckBoxSimilarityAutomatic,mCheckBoxNewSimilarityView,mCheckBoxNewDocument;
	private ExecutorService		mExecutor;
	private AtomicIntegerArray  mX,mY,mDX,mDY;
	private int[]				mNeighborCount;
	private ArrayList<SimilarPair> mPairList;
	private Integer[]			mSortedID;
	private SMPWorker[]			mSMPWorker;
	private AtomicInteger		mSMPRecordIndex,mSMPPairCount,mSMPPairIndex,mSMPMaxSali;
	private CountDownLatch		mSMPDoneSignal;

	public DETaskAnalyseActivityCliffs(DEFrame parent, DataWarrior application) {
		super(parent, true);
		mParentFrame = parent;
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
	public JPanel createDialogContent() {
		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8,
								TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));
		
		// create components
		mComboBoxDescriptorColumn = new JComboBox();
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++)
			if (qualifiesAsDescriptorColumn(column))
				mComboBoxDescriptorColumn.addItem(mSourceTableModel.getColumnTitle(column));
		mComboBoxDescriptorColumn.setEditable(!isInteractive());
		content.add(new JLabel("Similarity on:"), "1,1");
		content.add(mComboBoxDescriptorColumn, "3,1,5,1");

		mComboBoxActivityColumn = new JComboBox();
		mComboBoxActivityColumn.addItem(ITEM_SIMILARITY_ONLY);
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++)
			if (qualifiesAsActivityColumn(column))
				mComboBoxActivityColumn.addItem(mSourceTableModel.getColumnTitle(column));
		mComboBoxActivityColumn.setEditable(!isInteractive());
		mComboBoxActivityColumn.addItemListener(this);
		content.add(new JLabel("Activity column:"), "1,3");
		content.add(mComboBoxActivityColumn, "3,3,5,3");

		mComboBoxIdentifierColumn = new JComboBox();
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++)
			if (qualifiesAsIdentifierColumn(column))
				mComboBoxIdentifierColumn.addItem(mSourceTableModel.getColumnTitle(column));
		mComboBoxIdentifierColumn.addItem(ITEM_CREATE_COLUMN);
		mComboBoxIdentifierColumn.setEditable(!isInteractive());
		content.add(new JLabel("Identifier column:"), "1,5");
		content.add(mComboBoxIdentifierColumn, "3,5,5,5");

		mComboBoxGroupByColumn = new JComboBox();
		mComboBoxGroupByColumn.addItem(ITEM_NONE);
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++)
			if (qualifiesAsGroupByColumn(column))
				mComboBoxGroupByColumn.addItem(mSourceTableModel.getColumnTitle(column));
		mComboBoxGroupByColumn.setEditable(!isInteractive());
		content.add(new JLabel("Separate groups by:"), "1,7");
		content.add(mComboBoxGroupByColumn, "3,7,5,7");

		mCheckBoxSimilarityAutomatic = new JCheckBox("Automatic");
		mCheckBoxSimilarityAutomatic.addItemListener(this);
		content.add(new JLabel("Similarity limit:"), "1,9");
		content.add(mCheckBoxSimilarityAutomatic, "3,9");
		content.add(createSimilaritySlider(), "5,9");

		mCheckBoxNewSimilarityView = new JCheckBox("Create view based on similarity relationships");
		content.add(mCheckBoxNewSimilarityView, "1,11,5,11");

		mCheckBoxNewDocument = new JCheckBox("Create document of structure pairs");
		content.add(mCheckBoxNewDocument, "1,13,5,13");

		return content;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/chemistry.html#SimAnalysis";
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String descriptorName = configuration.getProperty(PROPERTY_DESCRIPTOR_COLUMN);
		if (descriptorName == null) {
			showErrorMessage("Descriptor column not defined.");
			return false;
			}

		if (!isLive)
			return true;

		String activityColumn = configuration.getProperty(PROPERTY_ACTIVITY_COLUMN);
		if (activityColumn != null && mSourceTableModel.findColumn(activityColumn) == -1) {
			showErrorMessage("Activity column '"+activityColumn+"' not found.");
			return false;
			}
		int descriptorColumn = mSourceTableModel.findColumn(descriptorName);
		if (descriptorColumn == -1) {
			showErrorMessage("Descriptor column '"+descriptorName+"' not found.");
			return false;
			}
		if (!qualifiesAsDescriptorColumn(descriptorColumn)) {
			showErrorMessage("Descriptor column '"+descriptorName+"' does not qualify.");
			return false;
			}
		String identifierColumn = configuration.getProperty(PROPERTY_IDENTIFIER_COLUMN);
		if (identifierColumn != null && !identifierColumn.equals(PROPERTY_VALUE_CREATE_COLUMN)) {
			int column = mSourceTableModel.findColumn(identifierColumn);
			if (column != -1) {	// otherwise silently create new identifier column
				if (!mSourceTableModel.isColumnDataUnique(column)) {
					showErrorMessage("Identifier column '"+mSourceTableModel.getColumnTitle(column)+"' contains redundant or empty entries.");
					return false;
					}
				}
			}
		String groupByColumn = configuration.getProperty(PROPERTY_GROUP_BY_COLUMN);
		if (groupByColumn != null) {
			int column = mSourceTableModel.findColumn(groupByColumn);
			if (column == -1) {
				showErrorMessage("Group-by column not found.");
				return false;
				}
			if (!qualifiesAsGroupByColumn(column)) {
				showErrorMessage("The group-by column '"+mSourceTableModel.getColumnTitle(column)+"' does not contain categories.");
				return false;
				}
			}
		return true;
		}

	private JComponent createSimilaritySlider() {
		Hashtable<Integer,JLabel> labels = new Hashtable<Integer,JLabel>();
		labels.put(new Integer(MIN_SIMILARITY), new JLabel(""+MIN_SIMILARITY+"%"));
		labels.put(new Integer((100+MIN_SIMILARITY)/2), new JLabel(""+((100+MIN_SIMILARITY)/2)+"%"));
		labels.put(new Integer(100), new JLabel("100%"));
		mSimilaritySlider = new JSlider(JSlider.HORIZONTAL, MIN_SIMILARITY, 100, DEFAULT_SIMILARITY);
		mSimilaritySlider.setMinorTickSpacing(1);
		mSimilaritySlider.setMajorTickSpacing(10);
		mSimilaritySlider.setLabelTable(labels);
		mSimilaritySlider.setPaintLabels(true);
		mSimilaritySlider.setPaintTicks(true);
//		mSimilaritySlider.setPreferredSize(new Dimension(120, mSimilaritySlider.getPreferredSize().height));
		JPanel spanel = new JPanel();
		spanel.add(mSimilaritySlider);
		return spanel;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String value = configuration.getProperty(PROPERTY_DESCRIPTOR_COLUMN, "");
		if (value.length() != 0) {
			int column = mSourceTableModel.findColumn(value);
			if (column != -1)
				mComboBoxDescriptorColumn.setSelectedItem(mSourceTableModel.getColumnTitle(column));
			else if (!isInteractive())
				mComboBoxDescriptorColumn.setSelectedItem(value);
			else if (mComboBoxDescriptorColumn.getItemCount() != 0)
				mComboBoxDescriptorColumn.setSelectedIndex(0);
			}
		else if (!isInteractive()) {
			mComboBoxDescriptorColumn.setSelectedItem("Structure ["+DescriptorConstants.DESCRIPTOR_SkeletonSpheres.shortName+"]");
			}

		value = configuration.getProperty(PROPERTY_ACTIVITY_COLUMN, "");
		mComboBoxActivityColumn.setSelectedItem(ITEM_SIMILARITY_ONLY);	// default
		if (value.length() != 0) {
			int column = mSourceTableModel.findColumn(value);
			if (column != -1 && qualifiesAsActivityColumn(column))
				mComboBoxActivityColumn.setSelectedItem(mSourceTableModel.getColumnTitle(column));
			else if (!isInteractive())
				mComboBoxActivityColumn.setSelectedItem(value);
			}

		value = configuration.getProperty(PROPERTY_IDENTIFIER_COLUMN, "");
		mComboBoxIdentifierColumn.setSelectedItem(ITEM_CREATE_COLUMN);	// default
		if (value.length() != 0 && !PROPERTY_VALUE_CREATE_COLUMN.equals(value)) {
			int column = mSourceTableModel.findColumn(value);
			if (column != -1 && qualifiesAsIdentifierColumn(column))
				mComboBoxIdentifierColumn.setSelectedItem(mSourceTableModel.getColumnTitle(column));
			else if (!isInteractive())
				mComboBoxIdentifierColumn.setSelectedItem(value);
			}

		value = configuration.getProperty(PROPERTY_GROUP_BY_COLUMN, "");
		if (value.length() != 0) {
			int column = mSourceTableModel.findColumn(value);
			if (column != -1 && qualifiesAsGroupByColumn(column))
				mComboBoxGroupByColumn.setSelectedItem(mSourceTableModel.getColumnTitle(column));
			else if (!isInteractive())
				mComboBoxGroupByColumn.setSelectedItem(value);
			}

		String similarity = configuration.getProperty(PROPERTY_SIMILARITY);
		if (similarity == null || similarity.equals(PROPERTY_VALUE_SIMILARITY_AUTOMATIC)) {
			mSimilaritySlider.setValue(DEFAULT_SIMILARITY);
			mCheckBoxSimilarityAutomatic.setSelected(true);
			}
		else {
			mSimilaritySlider.setValue(Integer.parseInt(similarity));
			mCheckBoxSimilarityAutomatic.setSelected(false);
			}

		mCheckBoxNewSimilarityView.setSelected(configuration.getProperty(PROPERTY_NEW_VIEW, "true").equals("true"));
		mCheckBoxNewDocument.setSelected(configuration.getProperty(PROPERTY_NEW_DOCUMENT, "false").equals("true"));
		enableGroupByMenu();
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mComboBoxDescriptorColumn.getItemCount() != 0)
			mComboBoxDescriptorColumn.setSelectedIndex(0);
		for (int i=0; i<mComboBoxDescriptorColumn.getItemCount(); i++) {
			if (((String)mComboBoxDescriptorColumn.getItemAt(i)).contains(DescriptorConstants.DESCRIPTOR_SkeletonSpheres.shortName)) {
				mComboBoxDescriptorColumn.setSelectedIndex(i);
				break;
				}
			}
		if (!isInteractive()) {
			String descriptorColumn = (String)mComboBoxDescriptorColumn.getSelectedItem();
			if (descriptorColumn == null || descriptorColumn.length() == 0)
				mComboBoxDescriptorColumn.setSelectedItem("Structure ["+DescriptorConstants.DESCRIPTOR_SkeletonSpheres.shortName+"]");
			}
		boolean identifierFound = false;
		for (int i=0; i<IDENTIFIER.length && !identifierFound; i++) {
			for (int j=0; j<mComboBoxIdentifierColumn.getItemCount(); j++) {
				if (((String)mComboBoxIdentifierColumn.getItemAt(j)).equals(IDENTIFIER[i])) {
					mComboBoxIdentifierColumn.setSelectedIndex(j);
					identifierFound = true;
					break;
					}
				}
			}
		mComboBoxActivityColumn.setSelectedIndex(0);
		mComboBoxIdentifierColumn.setSelectedIndex(0);
		mComboBoxGroupByColumn.setSelectedIndex(0);
		mSimilaritySlider.setValue(DEFAULT_SIMILARITY);
		mCheckBoxSimilarityAutomatic.setSelected(true);
		mCheckBoxNewSimilarityView.setSelected(true);
		mCheckBoxNewDocument.setSelected(false);
		enableGroupByMenu();
		}

	@Override
	public String getDialogTitle() {
		return "Analyze Activity Cliffs / Compound Similarity";
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		String descriptorColumn = (String)mComboBoxDescriptorColumn.getSelectedItem();
		if (descriptorColumn.length() != 0)
			configuration.setProperty(PROPERTY_DESCRIPTOR_COLUMN, descriptorColumn);

		String activityColumn = (String)mComboBoxActivityColumn.getSelectedItem();
		if (activityColumn.length() != 0 && !activityColumn.equals(ITEM_SIMILARITY_ONLY))
			configuration.setProperty(PROPERTY_ACTIVITY_COLUMN, mSourceTableModel.getColumnTitleNoAlias(activityColumn));

		String identifierColumn = (String)mComboBoxIdentifierColumn.getSelectedItem();
		if (identifierColumn.length() == 0 || identifierColumn.equals(ITEM_CREATE_COLUMN))
			configuration.setProperty(PROPERTY_IDENTIFIER_COLUMN, PROPERTY_VALUE_CREATE_COLUMN);
		else
			configuration.setProperty(PROPERTY_IDENTIFIER_COLUMN, mSourceTableModel.getColumnTitleNoAlias(identifierColumn));

		String groupByColumn = (String)mComboBoxGroupByColumn.getSelectedItem();
		if (mComboBoxGroupByColumn.isEnabled() && (groupByColumn.length() == 0 || !ITEM_NONE.equals(groupByColumn)))
			configuration.setProperty(PROPERTY_GROUP_BY_COLUMN, mSourceTableModel.getColumnTitleNoAlias(groupByColumn));

		configuration.setProperty(PROPERTY_SIMILARITY, mCheckBoxSimilarityAutomatic.isSelected() ? PROPERTY_VALUE_SIMILARITY_AUTOMATIC : ""+mSimilaritySlider.getValue());
		configuration.setProperty(PROPERTY_NEW_VIEW, mCheckBoxNewSimilarityView.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_NEW_DOCUMENT, mCheckBoxNewDocument.isSelected() ? "true" : "false");

		return configuration;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == mCheckBoxSimilarityAutomatic)
			mSimilaritySlider.setEnabled(!mCheckBoxSimilarityAutomatic.isSelected());
		else if (e.getSource() == mComboBoxActivityColumn && e.getStateChange() == ItemEvent.SELECTED)
			enableGroupByMenu();
		}

	private void enableGroupByMenu() {
		mComboBoxGroupByColumn.setEnabled(!ITEM_SIMILARITY_ONLY.equals(mComboBoxActivityColumn.getSelectedItem()));
		}

	@Override
	public void runTask(final Properties configuration) {
		final int activityColumn = mSourceTableModel.findColumn(configuration.getProperty(PROPERTY_ACTIVITY_COLUMN));
		final int descriptorColumn = mSourceTableModel.findColumn(configuration.getProperty(PROPERTY_DESCRIPTOR_COLUMN));

		waitForDescriptor(mSourceTableModel, descriptorColumn);
		if (threadMustDie())
			return;

		String similarityValue = configuration.getProperty(PROPERTY_SIMILARITY);
		boolean automaticSimilarityLimit = (similarityValue == null || similarityValue.equals(PROPERTY_VALUE_SIMILARITY_AUTOMATIC));
		final float initialSimilarityLimit = (automaticSimilarityLimit ? (float)MIN_SIMILARITY : Float.parseFloat(similarityValue)) / 100.0f;
		String descriptorShortName = mSourceTableModel.getColumnSpecialType(descriptorColumn);

		String propertyValue = configuration.getProperty(PROPERTY_IDENTIFIER_COLUMN);
		boolean createIDColumn = PROPERTY_VALUE_CREATE_COLUMN.equals(propertyValue);
		final int identifierColumn = createIDColumn ? mSourceTableModel.getTotalColumnCount() : mSourceTableModel.findColumn(propertyValue);
		final int groupByColumn = mSourceTableModel.findColumn(configuration.getProperty(PROPERTY_GROUP_BY_COLUMN));

		final int rowCount = mSourceTableModel.getTotalRowCount();
		mPairList = new ArrayList<SimilarPair>();

		startProgress((activityColumn == -1) ? "Calculating Similarities..." : "Calculating Activity Cliffs...", 0, (rowCount-1)/2);
		float maxSali = 0f;
		final AtomicIntegerArray similarityCount = new AtomicIntegerArray(100-MIN_SIMILARITY+1);

		int threadCount = Runtime.getRuntime().availableProcessors();
		mSMPRecordIndex = new AtomicInteger(rowCount);
		mSMPPairCount = new AtomicInteger(0);
		mSMPDoneSignal = new CountDownLatch(threadCount);

		mSMPMaxSali = new AtomicInteger(Float.floatToIntBits(0.0f));

		for (int i=0; i<threadCount; i++) {
			Thread t = new Thread("SALI Similarity Calculator "+(i+1)) {
				public void run() {
					int row2 = mSMPRecordIndex.decrementAndGet();
					while (row2 >= 1) {
						CompoundRecord r2 = mSourceTableModel.getTotalRecord(row2);
						if (r2.getData(descriptorColumn) != null) {
							for (int row1=0; row1<row2 && !threadMustDie(); row1++) {
								if (mSMPPairCount.incrementAndGet() % rowCount == 0)
									updateProgress(-1);
	
								CompoundRecord r1 = mSourceTableModel.getTotalRecord(row1);
								if (r1.getData(descriptorColumn) != null) {
									float similarity = mSourceTableModel.getDescriptorSimilarity(r1, r2, descriptorColumn);
									if (similarity >= initialSimilarityLimit-0.004999) {	// -0.005 (adapted for rounding problem) to have a complete bin
										similarityCount.incrementAndGet((int)(100*similarity+0.5-MIN_SIMILARITY));

										boolean calculateSALI = (activityColumn != -1
												  && !Double.isNaN(r1.getDouble(activityColumn))
												  && !Double.isNaN(r2.getDouble(activityColumn))
												  && (groupByColumn == -1
												   || Arrays.equals((byte[])r1.getData(groupByColumn),
														   			(byte[])r2.getData(groupByColumn))));

										if (calculateSALI) {
											float activityDif = Math.abs(r1.getDouble(activityColumn) - r2.getDouble(activityColumn));
											float saliValue = activityDif / (1.0f - similarity);
											if (!Double.isInfinite(saliValue)) {
												int maxSali = mSMPMaxSali.get();
												while (Float.intBitsToFloat(maxSali) < saliValue) {
													if (mSMPMaxSali.compareAndSet(maxSali, Float.floatToIntBits(saliValue)))
														break;
													maxSali = mSMPMaxSali.get();
													}
												}
		   									synchronized(mPairList) {
												mPairList.add(new SimilarPair(row1, row2, activityDif, similarity, saliValue));
												}
											}
										else {
											synchronized(mPairList) {
												mPairList.add(new SimilarPair(row1, row2, Float.NaN, similarity, Float.NaN));
												}
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

		float optSimilarityLimit = initialSimilarityLimit;
		if (automaticSimilarityLimit) {	// determine reasonable limit and remove sali pairs below this
			int desiredPairCount = rowCount * AVERAGE_NEIGHBOR_COUNT / 2;	// on average 4 neighbors for every row; divided by two because pairs count twice
			if (mPairList.size() > desiredPairCount) {
				int pairCount = 0;
				for (int i=100-MIN_SIMILARITY; i>0; i--) {
					pairCount += similarityCount.get(i);
					if (pairCount > desiredPairCount) {
						optSimilarityLimit = (float)(MIN_SIMILARITY+i)/100f;
						break;
						}
					}
				}
			}

		boolean adaptiveSimilarityLimit = true;
		if (adaptiveSimilarityLimit) {
			startProgress("Adapting row specific similarity limits...", 0, 0);

			float similarityMargin = (1f - optSimilarityLimit) / 2f;
			float minSimilarityLimit = optSimilarityLimit - similarityMargin;
			float maxSimilarityLimit = optSimilarityLimit + similarityMargin;

			int[] minSimilarityNeighborCount = new int[rowCount];
			int[] maxSimilarityNeighborCount = new int[rowCount];

			int minSimilarityCount = 0;
			int maxSimilarityCount = 0;
			for (SimilarPair sp:mPairList) {
				if (sp.similarity > minSimilarityLimit) {
					minSimilarityNeighborCount[sp.row1]++;
					minSimilarityNeighborCount[sp.row2]++;
					minSimilarityCount++;
					}
				if (sp.similarity > maxSimilarityLimit) {
					maxSimilarityNeighborCount[sp.row1]++;
					maxSimilarityNeighborCount[sp.row2]++;
					maxSimilarityCount++;
					}
				}
			if (maxSimilarityCount == 0)	// just to be on the save side
				maxSimilarityCount = 1;
			if (minSimilarityCount <= maxSimilarityCount)
				minSimilarityCount = maxSimilarityCount + 1;

			// this is the factor between count at minSimilarityLimit/optSimilarityLimit or optSimilarityLimit/maxSimilarityLimit
			float steepnessFactor = (1f + (float)minSimilarityCount/(float)maxSimilarityCount) / 2f;
			float log2Steepness = (float)Math.log(2.0 * steepnessFactor);

			// depending on the combined number of neighbors adapt similarity limit
			// and remove pair, if its similarity is below that limit
			for (int i=mPairList.size()-1; i>=0; i--) {
				SimilarPair sp = mPairList.get(i);
				float neighborCount = (float)(minSimilarityNeighborCount[sp.row1] + minSimilarityNeighborCount[sp.row2]
											+ maxSimilarityNeighborCount[sp.row1] + maxSimilarityNeighborCount[sp.row2]) / 4f;
				float countFactor = (float)neighborCount / (float)AVERAGE_NEIGHBOR_COUNT;
				float similarityShift = similarityMargin * (float)Math.log(countFactor) / log2Steepness;
				float similarityLimit = optSimilarityLimit + Math.max(Math.min(similarityShift, similarityMargin), -similarityMargin);

				if (sp.similarity < similarityLimit)
					mPairList.remove(i);
//				else
//					mPairList.get(i).attraction = (mPairList.get(i).similarity - similarityLimit) / (1f - similarityLimit);
				}
			}
		else {
			// just remove all pairs below similarity limit
			for (int i=mPairList.size()-1; i>=0; i--) {
				if (mPairList.get(i).similarity < optSimilarityLimit)
					mPairList.remove(i);
//						else
//							mPairList.get(i).attraction = (mPairList.get(i).similarity - optSimilarityLimit) / (1f - optSimilarityLimit);
				}
			}

		String[] similarity = null;
		String[] identifier = null;
		String[] sali = null;
		mNeighborCount = null;
		if (!threadMustDie()) {
			similarity = new String[rowCount];
			if (activityColumn != -1)
				sali = new String[rowCount];
			identifier = (identifierColumn == -1) ? null : new String[rowCount];
			mNeighborCount = new int[rowCount];
			for (SimilarPair sp:mPairList) {
				mNeighborCount[sp.row1]++;
				mNeighborCount[sp.row2]++;
				similarity[sp.row1] = addValue(similarity[sp.row1], DoubleFormat.toString(sp.similarity));
				similarity[sp.row2] = addValue(similarity[sp.row2], DoubleFormat.toString(sp.similarity));
				if (identifierColumn != -1) {
					String id1 = createIDColumn ? ""+(sp.row1+1) : mSourceTableModel.getTotalValueAt(sp.row1, identifierColumn);
					String id2 = createIDColumn ? ""+(sp.row2+1) : mSourceTableModel.getTotalValueAt(sp.row2, identifierColumn);
					identifier[sp.row1] = addValue(identifier[sp.row1], id2);
					identifier[sp.row2] = addValue(identifier[sp.row2], id1);
					}
				if (!Float.isNaN(sp.sali)) {
					if (Float.isInfinite(sp.sali))
						sp.sali = 2 * maxSali;
					sali[sp.row1] = addValue(sali[sp.row1], DoubleFormat.toString(sp.sali));
					sali[sp.row2] = addValue(sali[sp.row2], DoubleFormat.toString(sp.sali));
					}
				}
			}

		mX = null;
		mY = null;
		final boolean addCoords = configuration.getProperty(PROPERTY_NEW_VIEW, "false").equals("true");
		if (!threadMustDie() && addCoords) {
			startProgress("Calculating similarity based positions...", 0, VIEW_CYCLE_COUNT);

			if (threadCount != 1) {
				mExecutor = Executors.newFixedThreadPool(threadCount);
				mSMPWorker = new SMPWorker[threadCount];
				for (int t=0; t<threadCount; t++)
					mSMPWorker[t] = new SMPWorker();
				}

			mX = new AtomicIntegerArray(rowCount);
			mY = new AtomicIntegerArray(rowCount);
			for (int row=0; row<rowCount; row++) {
				mX.set(row, Float.floatToIntBits((float)Math.random()));
				mY.set(row, Float.floatToIntBits((float)Math.random()));
				}

			mDX = new AtomicIntegerArray(rowCount);
			mDY = new AtomicIntegerArray(rowCount);

			mSMPPairIndex = new AtomicInteger(0);
			mSMPRecordIndex = new AtomicInteger(0);

			mSortedID = new Integer[rowCount];
			for (int row=0; row<rowCount; row++)
				mSortedID[row] = new Integer(row);

			int zero = Float.floatToIntBits(0.0f);

			for (int cycle=0; cycle<VIEW_CYCLE_COUNT; cycle++) {
				if (threadMustDie())
					break;
				updateProgress(cycle);

				Arrays.sort(mSortedID, new Comparator<Integer>() {
					@Override
					public int compare(Integer i1, Integer i2) {
						float x1 = Float.intBitsToFloat(mX.get(i1.intValue()));
						float x2 = Float.intBitsToFloat(mX.get(i2.intValue()));
						return x1 < x2 ? -1	: x1 == x2 ? 0 : 1;
						}
					} );

				for (int i=0; i<rowCount; i++) {
					mDX.set(i, zero);
					mDY.set(i, zero);
					}

				mSMPPairIndex.set(0);
				mSMPRecordIndex.set(0);

				if (threadCount != 1) {
					optimizeCoordinatesSMP(cycle);
					}
				else {
					optimizeCoordinates(cycle);
					}

				for (int i=0; i<rowCount; i++) {
					mX.set(i, Float.floatToIntBits(Math.min(Math.max(
							Float.intBitsToFloat(mX.get(i))+Float.intBitsToFloat(mDX.get(i)), 0f), 1.0f)));
					mY.set(i, Float.floatToIntBits(Math.min(Math.max(
							Float.intBitsToFloat(mY.get(i))+Float.intBitsToFloat(mDY.get(i)), 0f), 1.0f)));
					}
				}
			}

		if (!threadMustDie() && addCoords) {
			startProgress("Cleaning Positions...", 0, 0);
			Arrays.sort(mSortedID, new Comparator<Integer>() {
				@Override
				public int compare(Integer i1, Integer i2) {
					float x1 = Float.intBitsToFloat(mX.get(i1.intValue()))-0.5f;
					float y1 = Float.intBitsToFloat(mY.get(i1.intValue()))-0.5f;
					float d1 = (float)Math.sqrt(x1*x1+y1*y1);
					float x2 = Float.intBitsToFloat(mX.get(i2.intValue()))-0.5f;
					float y2 = Float.intBitsToFloat(mY.get(i2.intValue()))-0.5f;
					float d2 = (float)Math.sqrt(x2*x2+y2*y2);
					return d1 < d2 ? -1	: d1 == d2 ? 0 : 1;
					}
				} );
			for (int i=0; i<rowCount; i++) {
				int row = mSortedID[i].intValue();
				float x = Float.intBitsToFloat(mX.get(row))-0.5f;
				float y = Float.intBitsToFloat(mY.get(row))-0.5f;

				double a;
				if (y != 0f) {
					a = (float)Math.atan(x/y);
					if (y < 0) {
						if (x < 0)
							a -= Math.PI;
						else
							a += Math.PI;
						}
					}
				else {
					a = (x > 0f) ? (float)Math.PI/2 : -(float)Math.PI/2;
					}

				x = (float)(Math.sqrt((0.5f+i)/rowCount) * Math.sin(a));
				y = (float)(Math.sqrt((0.5f+i)/rowCount) * Math.cos(a));
				mX.set(row, Float.floatToIntBits(x));
				mY.set(row, Float.floatToIntBits(y));
				}
			}

		if (!threadMustDie()) {
			startProgress("Populating new columns...", 0, 0);

			int newColumnCount = (createIDColumn ? 1 : 0) + 2 + (identifierColumn == -1 ? 0 : 1) + (addCoords ? 2 : 0) + (activityColumn == -1 ? 0 : 1);
			final String[] columnName = new String[newColumnCount];
			int index = 0;
			if (createIDColumn)
				columnName[index++] = COLUMN_NAME_ROW_ID;
			int similarityIndex = index++;
			columnName[similarityIndex] = "Neighbor Similarity "+descriptorShortName+" "+((int)(100*optSimilarityLimit+0.5))+"%";
			columnName[index++] =  "Neighbor Count";
			int neighborIndex = -1;
			if (identifierColumn != -1) {
				columnName[index] = "Neighbor";
				neighborIndex = index++;
				}
			if (addCoords) {
				columnName[index++] = "Neighbor Analysis X";
				columnName[index++] = "Neighbor Analysis Y";
				}
			int saliIndex = -1;
			if (activityColumn != -1) {
				saliIndex = index;
				columnName[saliIndex] = "SALI "+mSourceTableModel.getColumnTitle(activityColumn)+"/"+descriptorShortName;
				}

			final int firstNewColumn = mSourceTableModel.addNewColumns(columnName);
			final int similarityColumn = firstNewColumn+similarityIndex;
			final int referencingColumn = (identifierColumn == -1) ? -1 : firstNewColumn+neighborIndex;
			final int xColumn = addCoords ? firstNewColumn+index-2 : -1;
			final int yColumn = addCoords ? firstNewColumn+index-1 : -1;
			final int saliColumn = (saliIndex == -1) ? -1 : firstNewColumn+saliIndex;

			if (identifierColumn != -1) {
				mSourceTableModel.setColumnProperty(referencingColumn,
													CompoundTableModel.cColumnPropertyReferencedColumn,
													createIDColumn ? columnName[0]
																   : mSourceTableModel.getColumnTitleNoAlias(identifierColumn));
				mSourceTableModel.setColumnProperty(referencingColumn, CompoundTableModel.cColumnPropertyReferenceStrengthColumn,
																		  columnName[similarityIndex]);
				mSourceTableModel.setColumnProperty(referencingColumn, CompoundTableModel.cColumnPropertyReferenceType,
																		  CompoundTableModel.cColumnPropertyReferenceTypeRedundant);
				}

			for (int row=0; row<rowCount; row++) {
				int column = firstNewColumn;

				if (createIDColumn)
					mSourceTableModel.setTotalValueAt(""+(row+1), row, column++);

				if (similarity[row] != null)
					mSourceTableModel.setTotalValueAt(similarity[row], row, column);
				column++;

				mSourceTableModel.setTotalValueAt(""+mNeighborCount[row], row, column++);

				if (identifierColumn != -1) {
					if (identifier[row] != null)
						mSourceTableModel.setTotalValueAt(identifier[row], row, column);
					column++;
					}

				if (addCoords) {
					mSourceTableModel.setTotalValueAt(""+Float.intBitsToFloat(mX.get(row)), row, column++);
					mSourceTableModel.setTotalValueAt(""+Float.intBitsToFloat(mY.get(row)), row, column++);
					}

				if (saliColumn != -1 && sali[row] != null)
					mSourceTableModel.setTotalValueAt(sali[row], row, column);
				column++;
				}

			mSourceTableModel.setColumnSummaryMode(similarityColumn, CompoundTableConstants.cSummaryModeMaximum);
			if (saliColumn != -1)
				mSourceTableModel.setColumnSummaryMode(saliColumn, CompoundTableConstants.cSummaryModeMaximum);
			mSourceTableModel.finalizeNewColumns(firstNewColumn, this);

			sali = null;
			identifier = null;
			mX = null;
			mY = null;

			if (addCoords || activityColumn != -1) {
				final float similarityColorMin = 1f-2f*(1f-optSimilarityLimit);
				try {
					SwingUtilities.invokeAndWait(() -> {
						DEMainPane mainPane = mParentFrame.getMainFrame().getMainPane();
						VisualizationPanel2D vpanel1 = mainPane.add2DView(activityColumn != -1 ? "SALI Plot" : "Similarity Chart", null);
						if (addCoords) {
							vpanel1.setAxisColumnName(0, mSourceTableModel.getColumnTitle(xColumn));
							vpanel1.setAxisColumnName(1, mSourceTableModel.getColumnTitle(yColumn));
							}
						else {
							vpanel1.setAxisColumnName(0, mSourceTableModel.getColumnTitle(similarityColumn));
							vpanel1.setAxisColumnName(1, configuration.getProperty(PROPERTY_ACTIVITY_COLUMN));
							}
						if (activityColumn != -1)
							vpanel1.getVisualization().setMarkerSizeColumn(saliColumn, Float.NaN, Float.NaN);
						vpanel1.getVisualization().setMarkerSize(12f/(float)Math.sqrt(rowCount), false);
						vpanel1.getVisualization().setScaleMode(JVisualization.cScaleModeHidden);
						vpanel1.getVisualization().setGridMode(JVisualization.cGridModeHidden);
						vpanel1.getVisualization().setPreferredChartType(JVisualization.cChartTypeScatterPlot, -1, -1);

						int colorColumn,colorListMode;
						Color[] colorList = null;
						if (activityColumn != -1) {
							colorColumn = activityColumn;
							colorListMode = VisualizationColor.cColorListModeHSBLong;
							colorList = VisualizationColor.createColorWedge(Color.green, Color.blue, colorListMode, null);
							}
						else {
							colorColumn = descriptorColumn;
							colorListMode = VisualizationColor.cColorListModeHSBShort;
							colorList = VisualizationColor.createColorWedge(Color.red, Color.green, colorListMode, null);
							}
						vpanel1.getVisualization().getMarkerColor().setColor(colorColumn, colorList, colorListMode);
	/*
						int colorListMode2 = VisualizationColor.cColorListModeHSBLong;
						Color[] colorList2 = VisualizationColor.createColorWedge(new Color(255,166,166), new Color(166,166,255), colorListMode2, null);
						((JVisualization2D)vpanel1.getVisualization()).getBackgroundColor().setColor(activityColumn, colorList2, colorListMode2);
						((JVisualization2D)vpanel1.getVisualization()).setBackgroundColorFading(4);
						((JVisualization2D)vpanel1.getVisualization()).setBackgroundColorRadius(10);
	*/
						if (rowCount > 10000)
							vpanel1.getVisualization().setFastRendering(true);

						if (referencingColumn != -1) {
							vpanel1.getVisualization().setConnectionColumns(referencingColumn, -1);
							vpanel1.getVisualization().setConnectionLineWidth(Math.max(0.5f, 10f/(float)Math.sqrt(rowCount)), false);
							}

						int idcodeColumn = mSourceTableModel.getParentColumn(descriptorColumn);
						if (mSourceTableModel.isColumnTypeStructure(idcodeColumn)) {
							if (mSourceTableModel.getDescriptorHandler(descriptorColumn).getInfo().isGraphSimilarity)
								mSourceTableModel.setHiliteMode(idcodeColumn, CompoundTableModel.cStructureHiliteModeCurrentRow);
							String title = mainPane.validateTitle("Structure".equals(mSourceTableModel.getColumnTitle(idcodeColumn)) ?
									"Structure" : "Structure of " + mSourceTableModel.getColumnTitle(idcodeColumn));
							String dockInfo = mainPane.getViewTitle(vpanel1) + "\tbottom\t0.7";
							JStructureGrid structureView = mainPane.addStructureView(title, dockInfo, idcodeColumn);
							structureView.setColumnCount(5);
							structureView.setFocusList(FocusableView.cFocusOnSelection);
							int[] columnAtPosition = new int[MarkerLabelDisplayer.cPositionCode.length];
							for (int i=0; i<columnAtPosition.length; i++)
								columnAtPosition[i] = -1;
							int position = MarkerLabelDisplayer.cInTableLine1;
							if (saliColumn != -1)
								columnAtPosition[position++] = saliColumn;
							if (activityColumn != -1)
								columnAtPosition[position++] = activityColumn;
							columnAtPosition[position++] = similarityColumn;
							structureView.setMarkerLabels(columnAtPosition);

							if (referencingColumn != -1) {
								// create a tree view panel
								VisualizationPanel2D vpanel2 = mainPane.add2DView("Neighbor Tree", title+"\tleft\t0.25");
								vpanel2.getVisualization().setPreferredChartType(JVisualization.cChartTypeScatterPlot, -1, -1);

								vpanel2.getVisualization().getMarkerColor().setColor(colorColumn, colorList, colorListMode);
								if (activityColumn == -1)
									vpanel2.getVisualization().getMarkerColor().setColorRange(similarityColorMin, 1f);
								vpanel2.getVisualization().setAffectGlobalExclusion(false);
								vpanel2.getVisualization().setFontSize(2.5f, JVisualization.cFontSizeModeRelative, false);
								vpanel2.getVisualization().setConnectionColumns(referencingColumn, -1);
								vpanel2.getVisualization().setConnectionLineWidth(2f, false);
								vpanel2.getVisualization().setTreeViewMode(JVisualization.cTreeViewModeRadial, 5, false, false, false);
								}
							}
						} );
					}
				catch (Exception e) {}
				}
 			}

		if (configuration.getProperty(PROPERTY_NEW_DOCUMENT, "false").equals("true")) {
			startProgress("Populating new document...", 0, 0);

			int idcodeSourceColumn = mSourceTableModel.getParentColumn(descriptorColumn);
			int coordsSourceColumn = mSourceTableModel.getChildColumn(idcodeSourceColumn, CompoundTableModel.cColumnType2DCoordinates);
			boolean prepareCoordinates = (coordsSourceColumn != -1);
			final int structureColumnCount = 2 + (prepareCoordinates ? 1 : 0);
			final int dataColumnCount = (activityColumn == -1 ? 3 : 7) + (groupByColumn != -1 ? 1 : 0);
//final int dataColumnCount = (activityColumn == -1 ? 5 : 9) + (groupByColumn != -1 ? 1 : 0);

			mTargetFrame = mApplication.getEmptyFrame("Activity-Cliff Analysis");
			CompoundTableModel targetTableModel = mTargetFrame.getTableModel();
			targetTableModel.initializeTable(mPairList.size(), 2 * structureColumnCount + dataColumnCount);
			targetTableModel.prepareStructureColumns(0, "Structure 1", prepareCoordinates, descriptorShortName);
			targetTableModel.prepareStructureColumns(structureColumnCount, "Structure 2", prepareCoordinates, descriptorShortName);
			int column = 2 * structureColumnCount;
			targetTableModel.setColumnName("ID 1", column++);
			targetTableModel.setColumnName("ID 2", column++);
		   	targetTableModel.setColumnName("Similarity", column++);
//targetTableModel.setColumnName("minSimNeighborCount", column++);
//targetTableModel.setColumnName("maxSimNeighborCount", column++);
		   	if (activityColumn != -1) {
			   	targetTableModel.setColumnName("Activity 1", column++);
				targetTableModel.setColumnName("Activity 2", column++);
			 	targetTableModel.setColumnName("Delta Activity", column++);
				targetTableModel.setColumnName("SALI", column++);
		   		}
			if (groupByColumn != -1)
				targetTableModel.setColumnName(mSourceTableModel.getColumnTitle(groupByColumn), column++);

			for (int row=0; row<mPairList.size(); row++) {
				SimilarPair sp = mPairList.get(row);
				targetTableModel.setTotalValueAt(mSourceTableModel.getTotalValueAt(sp.row1, idcodeSourceColumn), row, 0);
				targetTableModel.setTotalValueAt(mSourceTableModel.getTotalValueAt(sp.row2, idcodeSourceColumn), row, structureColumnCount);
				if (prepareCoordinates) {
					targetTableModel.setTotalValueAt(mSourceTableModel.getTotalValueAt(sp.row1, coordsSourceColumn), row, 1);
					targetTableModel.setTotalValueAt(mSourceTableModel.getTotalValueAt(sp.row2, coordsSourceColumn), row, 1+structureColumnCount);
					}
				targetTableModel.setTotalValueAt(mSourceTableModel.getTotalValueAt(sp.row1, descriptorColumn), row, structureColumnCount-1);
				targetTableModel.setTotalValueAt(mSourceTableModel.getTotalValueAt(sp.row2, descriptorColumn), row, 2*structureColumnCount-1);
				column = 2 * structureColumnCount;
				targetTableModel.setTotalValueAt(mSourceTableModel.getTotalValueAt(sp.row1, identifierColumn), row, column++);
				targetTableModel.setTotalValueAt(mSourceTableModel.getTotalValueAt(sp.row2, identifierColumn), row, column++);
				targetTableModel.setTotalValueAt(DoubleFormat.toString(sp.similarity), row, column++);
//targetTableModel.setTotalValueAt(""+(minSimilarityNeighborCount[sp.row1]+minSimilarityNeighborCount[sp.row2]), row, column++);
//targetTableModel.setTotalValueAt(""+(maxSimilarityNeighborCount[sp.row1]+minSimilarityNeighborCount[sp.row2]), row, column++);
			   	if (activityColumn != -1) {
					targetTableModel.setTotalValueAt(mSourceTableModel.getTotalValueAt(sp.row1, activityColumn), row, column++);
					targetTableModel.setTotalValueAt(mSourceTableModel.getTotalValueAt(sp.row2, activityColumn), row, column++);
					targetTableModel.setTotalValueAt(DoubleFormat.toString(sp.activityDif), row, column++);
					targetTableModel.setTotalValueAt(DoubleFormat.toString(sp.sali), row, column++);
			   		}
				if (groupByColumn != -1)
					targetTableModel.setTotalValueAt(mSourceTableModel.getTotalValueAt(sp.row1, groupByColumn), row, column++);
				}

			targetTableModel.finalizeTable(CompoundTableEvent.cSpecifierDefaultFiltersAndViews, this);

		   	if (activityColumn != -1) {
		   		try {
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							mTargetFrame.getTableModel().setLogarithmicViewMode(2 * structureColumnCount + 3, true);
					   		mTargetFrame.getTableModel().setLogarithmicViewMode(2 * structureColumnCount + 4, true);
		
							mTargetFrame.getMainFrame().setMainSplitting(0.7);
							mTargetFrame.getMainFrame().setRightSplitting(0.4);
							VisualizationPanel2D vpanel1 = mTargetFrame.getMainFrame().getMainPane().add2DView("SALI Plot", null);
				
							vpanel1.setAxisColumnName(0, "Activity 1");
							vpanel1.setAxisColumnName(1, "Activity 2");
		
							vpanel1.getVisualization().setMarkerSizeColumn(2 * structureColumnCount + 6, Float.NaN, Float.NaN);
							vpanel1.getVisualization().setMarkerSize(0.64f, false);
		
							int colorListMode1 = VisualizationColor.cColorListModeHSBLong;
							Color[] colorList1 = VisualizationColor.createColorWedge(Color.red, Color.blue, colorListMode1, null);
							vpanel1.getVisualization().getMarkerColor().setColor(2 * structureColumnCount + 5, colorList1, colorListMode1);
							}
						} );
		   			}
		   		catch (Exception e) {}
				}
			}
		}

	private void optimizeCoordinatesSMP(int cycle) {
		CountDownLatch doneSignal = new CountDownLatch(mSMPWorker.length);
		for (SMPWorker w:mSMPWorker) {
			w.initialize(doneSignal, cycle);
			mExecutor.execute(w);
			}
		try {
			doneSignal.await();
			}
		catch (InterruptedException e) {}
		}

	private void optimizeCoordinates(int cycle) {
		int rowCount = mX.length();
		float minDistance = 1.0f / (float)Math.sqrt(rowCount);
		float cycleState = (float)cycle / (float)VIEW_CYCLE_COUNT;
		float cycleMinDistance = cycleState*(2.0f-cycleState) * minDistance;

		float attractionCycleFactor = 0.8f * (1.0f - cycleState) * ((cycleState < 0.5) ? 0.5f : (1.0f - cycleState));
		float repulsionCycleFactor = (cycleState < 0.5) ? 0.5f : (1.0f - cycleState);

		int current,updated;

		while (true) {
			int listIndex = mSMPPairIndex.getAndIncrement();
			if (listIndex >= mPairList.size())
				break;

			SimilarPair sp = mPairList.get(listIndex);
			float dx = Float.intBitsToFloat(mX.get(sp.row2))-Float.intBitsToFloat(mX.get(sp.row1));
			float dy = Float.intBitsToFloat(mY.get(sp.row2))-Float.intBitsToFloat(mY.get(sp.row1));
			float distance = (float)Math.sqrt(dx*dx+dy*dy);
// reducing minDistance with higher sp.attraction (i.e. similarity) indeed more often puts closest neighbors in the first shell,
// but comes with the price of putting other neighbors in third and even forth shell, while some positions in 2nd and 3rd shell are
// occupied with rwcords not being a neighbor at all. Finally, it is not worth it.
//			float strengthAdaptedMinDistance = minDistance + 2f * (0.5f - sp.attraction) * minDistance;
//			float shift = distance - strengthAdaptedMinDistance;
			float shift = distance - minDistance;
			if (shift > 0) {
				dx *= shift/distance;
				dy *= shift/distance;
				float neighborFactor1 = (mNeighborCount[sp.row1] > 4) ? 4.0f / mNeighborCount[sp.row1] : 1.0f;
				float neighborFactor2 = (mNeighborCount[sp.row2] > 4) ? 4.0f / mNeighborCount[sp.row2] : 1.0f;

				do {
					current = mDX.get(sp.row1);
					updated = Float.floatToIntBits(Float.intBitsToFloat(current) + dx * attractionCycleFactor * neighborFactor1);
					} while (!mDX.compareAndSet(sp.row1, current, updated));
				do {
					current = mDY.get(sp.row1);
					updated = Float.floatToIntBits(Float.intBitsToFloat(current) + dy * attractionCycleFactor * neighborFactor1);
					} while (!mDY.compareAndSet(sp.row1, current, updated));
				do {
					current = mDX.get(sp.row2);
					updated = Float.floatToIntBits(Float.intBitsToFloat(current) - dx * attractionCycleFactor * neighborFactor2);
					} while (!mDX.compareAndSet(sp.row2, current, updated));
				do {
					current = mDY.get(sp.row2);
					updated = Float.floatToIntBits(Float.intBitsToFloat(current) - dy * attractionCycleFactor * neighborFactor2);
					} while (!mDY.compareAndSet(sp.row2, current, updated));
				}
			}

		while (true) {
			int i = mSMPRecordIndex.getAndIncrement();
			if (i >= rowCount-1)
				break;

			int row1 = mSortedID[i].intValue();
			for (int j=i+1; j<rowCount; j++) {
				int row2 = mSortedID[j].intValue();
				float dx = Float.intBitsToFloat(mX.get(row2))-Float.intBitsToFloat(mX.get(row1));
				if (Math.abs(dx) >= cycleMinDistance)
					break;

				float dy = Float.intBitsToFloat(mY.get(row2))-Float.intBitsToFloat(mY.get(row1));
				if (Math.abs(dy) < cycleMinDistance) {
					double distance = Math.sqrt(dx*dx+dy*dy);
					if (distance < cycleMinDistance) {
						if (distance == 0.0) {
							double angle = 2.0 * Math.PI * Math.random();
							dx = (float)Math.sin(angle) * cycleMinDistance;
							dy = (float)Math.cos(angle) * cycleMinDistance;
							}
						else {
							double shift = cycleMinDistance - distance;
							dx *= shift/distance;
							dy *= shift/distance;
							}
						do {
							current = mDX.get(row1);
							updated = Float.floatToIntBits(Float.intBitsToFloat(current) - dx * repulsionCycleFactor);
							} while (!mDX.compareAndSet(row1, current, updated));
						do {
							current = mDY.get(row1);
							updated = Float.floatToIntBits(Float.intBitsToFloat(current) - dy * repulsionCycleFactor);
							} while (!mDY.compareAndSet(row1, current, updated));
						do {
							current = mDX.get(row2);
							updated = Float.floatToIntBits(Float.intBitsToFloat(current) + dx * repulsionCycleFactor);
							} while (!mDX.compareAndSet(row2, current, updated));
						do {
							current = mDY.get(row2);
							updated = Float.floatToIntBits(Float.intBitsToFloat(current) + dy * repulsionCycleFactor);
							} while (!mDY.compareAndSet(row2, current, updated));
						}
					}
				}
			}
		}

	private String addValue(String oldValue, String value) {
		return (oldValue == null) ? value : oldValue + CompoundTableModel.cEntrySeparator + value;
		}

	private boolean qualifiesAsIdentifierColumn(int column) {
		return mSourceTableModel.getColumnSpecialType(column) == null
			&& mSourceTableModel.isColumnDataUnique(column);
		}

	private boolean qualifiesAsDescriptorColumn(int column) {
		return mSourceTableModel.isDescriptorColumn(column);
		}

	private boolean qualifiesAsActivityColumn(int column) {
		return mSourceTableModel.isColumnTypeDouble(column);
		}

	private boolean qualifiesAsGroupByColumn(int column) {
		return mSourceTableModel.isColumnTypeCategory(column);
		}

	private class SMPWorker implements Runnable {
		private CountDownLatch	mDoneSignal;
		private int				mCycle;

		private SMPWorker() {
			}

		public void initialize(CountDownLatch doneSignal, int cycle) {
			mDoneSignal = doneSignal;
			mCycle = cycle;
			
			}

		public void run() {
			optimizeCoordinates(mCycle);

			mDoneSignal.countDown();
			}
		}

	private class SimilarPair {
		public int row1,row2;
		public float sali,activityDif,similarity /* ,attraction */;

		public SimilarPair(int row1, int row2, float activityDif, float similarity, float sali) {
			this.row1 = row1;
			this.row2 = row2;
			this.activityDif = activityDif;
			this.similarity = similarity;
			this.sali = sali;
			}
		}
	}
