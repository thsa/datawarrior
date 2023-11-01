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

package com.actelion.research.datawarrior.task.data;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.JVisualization2D;
import com.actelion.research.table.view.VisualizationColor;
import com.actelion.research.table.view.VisualizationPanel2D;
import com.actelion.research.util.SortedList;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;


public class DETaskArrangeGraphNodes extends ConfigurableTask implements ActionListener {
	private static final String PROPERTY_KEY_COLUMN = "keyColumn";
	private static final String PROPERTY_REFERENCING_COLUMN = "referencingColumn";
	private static final String PROPERTY_STRENGTH_COLUMN = "strengthColumn";
	private static final String PROPERTY_NEGLECT_NODES = "neglectNodes";
	private static final String PROPERTY_ADAPTIVE_LIMIT = "adaptiveLimit";
	private static final String PROPERTY_VALUE_NONE = "<none>";
	private static final String ITEM_NONE = "<none>";

	private static final int MIN_SIMILARITY_PERCENT = 50;
	private static final float MIN_SIMILARITY = (float)MIN_SIMILARITY_PERCENT / 100f;

	private static final int AVERAGE_NEIGHBOR_COUNT = 6;
	private static final int VIEW_CYCLE_COUNT = 20000;

	public static final String TASK_NAME = "Arrange Graph Nodes (2D-RBS)";

	private DEFrame				mParentFrame;
	private CompoundTableModel	mSourceTableModel;
	private JComboBox			mComboBoxKeyColumn,mComboBoxReferencingColumn,mComboBoxStrengthColumn;
	private JCheckBox			mCheckBoxNeglectNodes,mCheckBoxAdaptiveLimit;
	private ExecutorService		mExecutor;
	private AtomicIntegerArray  mX,mY,mDX,mDY;
	private int[]				mNeighborCount;
	private SortedList<SimilarPair> mPairList;
	private Integer[]			mSortedID;
	private SMPWorker[]			mSMPWorker;
	private AtomicInteger		mSMPRecordIndex,mSMPPairIndex;

	public DETaskArrangeGraphNodes(DEFrame parent) {
		super(parent, true);
		mParentFrame = parent;
		mSourceTableModel = parent.getTableModel();
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
	public boolean isConfigurable() {

		boolean keyColumnFound = false;
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++) {
			if (qualifiesAsKeyColumn(column)) {
				keyColumnFound = true;
				break;
				}
			}
		if (!keyColumnFound) {
			showErrorMessage("No column found that contains unique keys for every row.");
			return false;
			}

		boolean referencingColumnFound = false;
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++) {
			if (qualifiesAsReferencingColumn(column)) {
				referencingColumnFound = true;
				break;
				}
			}
		if (!referencingColumnFound) {
			showErrorMessage("No column found that may serve to reference a key column.");
			return false;
			}

		return true;
		}

	@Override
	public JPanel createDialogContent() {
		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8,
								TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));
		
		// create components
		mComboBoxKeyColumn = new JComboBox();
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++)
			if (qualifiesAsKeyColumn(column))
				mComboBoxKeyColumn.addItem(mSourceTableModel.getColumnTitle(column));
		mComboBoxKeyColumn.setEditable(!isInteractive());
		content.add(new JLabel("Unique row keys:"), "1,1");
		content.add(mComboBoxKeyColumn, "3,1");

		mComboBoxReferencingColumn = new JComboBox();
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++)
			if (qualifiesAsReferencingColumn(column))
				mComboBoxReferencingColumn.addItem(mSourceTableModel.getColumnTitle(column));
		mComboBoxReferencingColumn.setEditable(!isInteractive());
		content.add(new JLabel("Row key references:"), "1,3");
		content.add(mComboBoxReferencingColumn, "3,3");

		mComboBoxStrengthColumn = new JComboBox();
		mComboBoxStrengthColumn.addItem(ITEM_NONE);
		for (int column=0; column<mSourceTableModel.getTotalColumnCount(); column++)
			if (qualifiesAsStrengthColumn(column))
				mComboBoxStrengthColumn.addItem(mSourceTableModel.getColumnTitle(column));
		mComboBoxStrengthColumn.setEditable(!isInteractive());
		if (isInteractive())
			mComboBoxStrengthColumn.addActionListener(this);
		content.add(new JLabel("Node connection strength:"), "1,5");
		content.add(mComboBoxStrengthColumn, "3,5");

		mCheckBoxNeglectNodes = new JCheckBox("Neglect lower strength neighbors in dense clusters");
		mCheckBoxNeglectNodes.addActionListener(this);
		content.add(mCheckBoxNeglectNodes, "1,7,3,7");

		mCheckBoxAdaptiveLimit = new JCheckBox("Adapt strength limit by cluster density");
		content.add(mCheckBoxAdaptiveLimit, "1,9,3,9");

		return content;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		enableCheckBoxes();
		}

	private void enableCheckBoxes() {
		mCheckBoxNeglectNodes.setEnabled(!ITEM_NONE.equals(mComboBoxStrengthColumn.getSelectedItem()));
		mCheckBoxAdaptiveLimit.setEnabled(!ITEM_NONE.equals(mComboBoxStrengthColumn.getSelectedItem())
										&& mCheckBoxNeglectNodes.isSelected());
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String keyColumnName = configuration.getProperty(PROPERTY_KEY_COLUMN);
		if (keyColumnName == null) {
			showErrorMessage("Key column not defined.");
			return false;
			}

		String referencingColumnName = configuration.getProperty(PROPERTY_REFERENCING_COLUMN);
		if (referencingColumnName == null) {
			showErrorMessage("Key referencing column not defined.");
			return false;
			}

		if (!isLive)
			return true;

		int strengthColumn = -1;
		String strengthColumnName = configuration.getProperty(PROPERTY_STRENGTH_COLUMN);
		if (strengthColumnName != null) {
			strengthColumn = mSourceTableModel.findColumn(strengthColumnName);
			if (strengthColumn == -1) {
				showErrorMessage("Connection strength column '"+strengthColumnName+"' not found.");
				return false;
				}
			}
		int keyColumn = mSourceTableModel.findColumn(keyColumnName);
		if (keyColumn == -1) {
			showErrorMessage("Key column '"+keyColumnName+"' not found.");
			return false;
			}
		int referencingColumn = mSourceTableModel.findColumn(referencingColumnName);
		if (referencingColumn == -1) {
			showErrorMessage("Key referencing column '"+referencingColumnName+"' not found.");
			return false;
			}

		return true;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String value = configuration.getProperty(PROPERTY_KEY_COLUMN, "");
		if (value.length() != 0) {
			int column = mSourceTableModel.findColumn(value);
			if (column != -1 && qualifiesAsKeyColumn(column))
				mComboBoxKeyColumn.setSelectedItem(mSourceTableModel.getColumnTitle(column));
			else if (!isInteractive())
				mComboBoxKeyColumn.setSelectedItem(value);
			}

		value = configuration.getProperty(PROPERTY_REFERENCING_COLUMN, "");
		if (value.length() != 0) {
			int column = mSourceTableModel.findColumn(value);
			if (column != -1 && qualifiesAsReferencingColumn(column))
				mComboBoxReferencingColumn.setSelectedItem(mSourceTableModel.getColumnTitle(column));
			else if (!isInteractive())
				mComboBoxReferencingColumn.setSelectedItem(value);
			}

		value = configuration.getProperty(PROPERTY_STRENGTH_COLUMN, "");
		mComboBoxStrengthColumn.setSelectedItem(ITEM_NONE);	// default
		if (value.length() != 0 && !PROPERTY_VALUE_NONE.equals(value)) {
			int column = mSourceTableModel.findColumn(value);
			if (column != -1 && qualifiesAsStrengthColumn(column))
				mComboBoxStrengthColumn.setSelectedItem(mSourceTableModel.getColumnTitle(column));
			else if (!isInteractive())
				mComboBoxStrengthColumn.setSelectedItem(value);
			}

		mCheckBoxNeglectNodes.setSelected(configuration.getProperty(PROPERTY_NEGLECT_NODES, "true").equals("true"));
		mCheckBoxAdaptiveLimit.setSelected(configuration.getProperty(PROPERTY_ADAPTIVE_LIMIT, "true").equals("true"));

		enableCheckBoxes();
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mComboBoxKeyColumn.getItemCount() != 0)
			mComboBoxKeyColumn.setSelectedIndex(0);
		if (mComboBoxReferencingColumn.getItemCount() != 0)
			mComboBoxReferencingColumn.setSelectedIndex(0);
		mComboBoxStrengthColumn.setSelectedItem(ITEM_NONE);

		if (isInteractive()) {
			for (int i=0; i<mComboBoxReferencingColumn.getItemCount(); i++) {
				int referencingColumn = mSourceTableModel.findColumn((String)mComboBoxReferencingColumn.getItemAt(i));
				int referencedColumn = mSourceTableModel.findColumn(mSourceTableModel.getColumnProperty(referencingColumn,
						CompoundTableConstants.cColumnPropertyReferencedColumn));
				if (referencedColumn != -1) {
					mComboBoxKeyColumn.setSelectedItem(mSourceTableModel.getColumnTitle(referencedColumn));
					mComboBoxReferencingColumn.setSelectedIndex(i);
					int strengthColumn = mSourceTableModel.findColumn(mSourceTableModel.getColumnProperty(referencingColumn,
							CompoundTableConstants.cColumnPropertyReferenceStrengthColumn));
					if (strengthColumn != -1 && qualifiesAsStrengthColumn(strengthColumn))
						mComboBoxStrengthColumn.setSelectedItem(mSourceTableModel.getColumnTitle(strengthColumn));
					break;
					}
				}
			}

		mCheckBoxNeglectNodes.setSelected(true);
		mCheckBoxAdaptiveLimit.setSelected(true);

		enableCheckBoxes();
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		String keyColumn = (String)mComboBoxKeyColumn.getSelectedItem();
		if (keyColumn.length() != 0)
			configuration.setProperty(PROPERTY_KEY_COLUMN, mSourceTableModel.getColumnTitleNoAlias(keyColumn));

		String referencingColumn = (String)mComboBoxReferencingColumn.getSelectedItem();
		if (referencingColumn.length() != 0)
			configuration.setProperty(PROPERTY_REFERENCING_COLUMN, mSourceTableModel.getColumnTitleNoAlias(referencingColumn));

		String strengthColumn = (String)mComboBoxStrengthColumn.getSelectedItem();
		if (strengthColumn.length() != 0 && !strengthColumn.equals(ITEM_NONE))
			configuration.setProperty(PROPERTY_STRENGTH_COLUMN, mSourceTableModel.getColumnTitleNoAlias(strengthColumn));

		configuration.setProperty(PROPERTY_NEGLECT_NODES, mCheckBoxNeglectNodes.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_ADAPTIVE_LIMIT, mCheckBoxAdaptiveLimit.isSelected() ? "true" : "false");

		return configuration;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public void runTask(final Properties configuration) {
		final int keyColumn = mSourceTableModel.findColumn(configuration.getProperty(PROPERTY_KEY_COLUMN));
		final int referencingColumn = mSourceTableModel.findColumn(configuration.getProperty(PROPERTY_REFERENCING_COLUMN));
		final int strengthColumn = mSourceTableModel.findColumn(configuration.getProperty(PROPERTY_STRENGTH_COLUMN));

		final int rowCount = mSourceTableModel.getTotalRowCount();
		mPairList = new SortedList<SimilarPair>();

		startProgress("Analyzing Unique IDs...", 0, 0);
		TreeMap<String,Integer> rowMap = new TreeMap<String,Integer>();
		for (int row=0; row<mSourceTableModel.getTotalRowCount(); row++) {
			String key = mSourceTableModel.getTotalValueAt(row, keyColumn);
			rowMap.put(key, row);
			}
		startProgress("Analyzing Graph Data...", 0, rowCount);
		for (int row=0; row<mSourceTableModel.getTotalRowCount(); row++) {
			if ((row & 15) == 15)
				updateProgress(row);

			CompoundRecord record = mSourceTableModel.getTotalRecord(row);

			int keyRow = rowMap.get(mSourceTableModel.encodeData(record, keyColumn));
			String[] entry = mSourceTableModel.separateUniqueEntries(mSourceTableModel.encodeData(record, referencingColumn));
			if (entry != null) {
				for (int i=0; i<entry.length; i++) {
					Integer referencedRow = rowMap.get(entry[i]);
					if (referencedRow != null) {
						float strength = 1f;
						if (strengthColumn != -1) {
							String[] strengthes = mSourceTableModel.separateUniqueEntries(mSourceTableModel.encodeData(record, strengthColumn));
							if (strengthes.length > i) {
								strength = mSourceTableModel.tryParseEntry(strengthes[i], strengthColumn);
// this is critical, because individual entries may indeed exceed the maximum value, which is the maximum of mean values
//							float value = mSourceTableModel.tryParseEntry(strengthes[i], strengthColumn);
//							strength = MIN_SIMILARITY + (1.0f - MIN_SIMILARITY) * (value - mSourceTableModel.getMinimumValue(strengthColumn))
//									/ (mSourceTableModel.getMaximumValue(strengthColumn) - mSourceTableModel.getMinimumValue(strengthColumn));
								}
							}
						mPairList.add(new SimilarPair(keyRow, referencedRow, strength));
						}
					}
				}
			}

		if (configuration.getProperty(PROPERTY_NEGLECT_NODES, "true").equals("true"))
			removeLowSimilarPairs(configuration, rowCount);

		mNeighborCount = new int[rowCount];
		for (int i=0; i<mPairList.size(); i++) {
			SimilarPair pair = mPairList.get(i);
			mNeighborCount[pair.row1]++;
			mNeighborCount[pair.row2]++;
			}

		int threadCount = Runtime.getRuntime().availableProcessors();
		mSMPRecordIndex = new AtomicInteger(rowCount);

		mX = null;
		mY = null;
		if (!threadMustDie()) {
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
				mSortedID[row] = Integer.valueOf(row);

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

		if (!threadMustDie()) {
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

			final String[] columnName = {"Graph Neighbor Count", "Graph X", "Graph Y"};

			final int firstNewColumn = mSourceTableModel.addNewColumns(columnName);
			final int neighborCountColumn = firstNewColumn;
			final int xColumn = firstNewColumn+1;
			final int yColumn = firstNewColumn+2;

			for (int row=0; row<rowCount; row++) {
				mSourceTableModel.setTotalValueAt(""+mNeighborCount[row], row, neighborCountColumn);
				mSourceTableModel.setTotalValueAt(""+Float.intBitsToFloat(mX.get(row)), row, xColumn);
				mSourceTableModel.setTotalValueAt(""+Float.intBitsToFloat(mY.get(row)), row, yColumn);
				}
			mSourceTableModel.finalizeNewColumns(firstNewColumn, this);

			mX = null;
			mY = null;

			try {
				SwingUtilities.invokeAndWait(new Runnable() {
					public void run() {
						String title = "Graph View";
						DEMainPane mainPane = mParentFrame.getMainFrame().getMainPane();
						VisualizationPanel2D vpanel1 = mainPane.add2DView(title, null);
						vpanel1.setAxisColumnName(0, mSourceTableModel.getColumnTitle(xColumn));
						vpanel1.setAxisColumnName(1, mSourceTableModel.getColumnTitle(yColumn));
						vpanel1.getVisualization().setMarkerSize(12f/(float)Math.sqrt(rowCount), false);
						vpanel1.getVisualization().setScaleMode(JVisualization.cScaleModeHidden);
						vpanel1.getVisualization().setGridMode(JVisualization.cGridModeHidden);
						((JVisualization2D)vpanel1.getVisualization()).setPreferredChartType(JVisualization.cChartTypeScatterPlot, -1, -1);

						int colorColumn = (strengthColumn != -1) ? strengthColumn : neighborCountColumn;
						int colorListMode = VisualizationColor.cColorListModeHSBLong;
						Color[] colorList = VisualizationColor.createColorWedge(Color.green, Color.blue, colorListMode, null);
						vpanel1.getVisualization().getMarkerColor().setColor(colorColumn, colorList, colorListMode);

						if (rowCount > 10000)
							vpanel1.getVisualization().setFastRendering(true);
		
						if (referencingColumn != -1) {
							vpanel1.getVisualization().setConnectionColumns(referencingColumn, -1);
							vpanel1.getVisualization().setConnectionLineWidth(Math.max(0.5f, 10f/(float)Math.sqrt(rowCount)), false);
							}

						// create a tree view panel
						VisualizationPanel2D vpanel2 = mainPane.add2DView("Neighbor Tree", title+"\tright\t0.75");
						((JVisualization2D)vpanel2.getVisualization()).setPreferredChartType(JVisualization.cChartTypeScatterPlot, -1, -1);

						vpanel2.getVisualization().getMarkerColor().setColor(colorColumn, colorList, colorListMode);
						if (strengthColumn != -1)
							vpanel2.getVisualization().getMarkerColor().setColorRange(0.5f, 1f);
						vpanel2.getVisualization().setAffectGlobalExclusion(false);
						vpanel2.getVisualization().setFontSize(2.5f, JVisualization.cFontSizeModeRelative, false);
						vpanel2.getVisualization().setConnectionColumns(referencingColumn, -1);
						vpanel2.getVisualization().setConnectionLineWidth(2f, false);
						vpanel2.getVisualization().setTreeViewMode(JVisualization.cTreeViewModeLeftRoot, 5, false, false, false);
						}
					} );
				}
			catch (Exception e) {}
			}
		}

	private void removeLowSimilarPairs(Properties configuration, int rowCount) {
		int[] similarityCount = new int[100-MIN_SIMILARITY_PERCENT+1];
		for (int i=0; i<mPairList.size(); i++) {
			SimilarPair sp = mPairList.get(i);
			similarityCount[(int)(100*sp.similarity+0.5)-MIN_SIMILARITY_PERCENT]++;
			}

		float optSimilarityLimit = MIN_SIMILARITY;	// initial similarity values are relative from MIN_SIMILARITY to 1.0

		boolean automaticSimilarityLimit = configuration.getProperty(PROPERTY_NEGLECT_NODES, "true").equals("true");
		if (automaticSimilarityLimit) {	// determine reasonable limit and remove pairs below this
			int desiredPairCount = rowCount * AVERAGE_NEIGHBOR_COUNT / 2;	// on average 4 neighbors for every row; divided by two because pairs count twice
			if (mPairList.size() > desiredPairCount) {
				int pairCount = 0;
				for (int i=100-MIN_SIMILARITY_PERCENT; i>0; i--) {
					pairCount += similarityCount[i];
					if (pairCount > desiredPairCount) {
						optSimilarityLimit = MIN_SIMILARITY + (float)i/100f;
						break;
						}
					}
				}
			}

		if (optSimilarityLimit > MIN_SIMILARITY
		 && configuration.getProperty(PROPERTY_ADAPTIVE_LIMIT, "true").equals("true")) {
			startProgress("Adapting row specific similarity limits...", 0, 0);

			float similarityMargin = (1f - optSimilarityLimit) / 2f;
			float minSimilarityLimit = optSimilarityLimit - similarityMargin;
			float maxSimilarityLimit = optSimilarityLimit + similarityMargin;

			int[] minSimilarityNeighborCount = new int[rowCount];
			int[] maxSimilarityNeighborCount = new int[rowCount];

			int minSimilarityCount = 0;
			int maxSimilarityCount = 0;
			for (int i=0; i<mPairList.size(); i++) {
				SimilarPair sp = mPairList.get(i);
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

	private boolean qualifiesAsKeyColumn(int column) {
		return mSourceTableModel.getColumnSpecialType(column) == null
			&& mSourceTableModel.isColumnDataUnique(column);
		}

	private boolean qualifiesAsReferencingColumn(int column) {
		return mSourceTableModel.getColumnSpecialType(column) == null;
		}

	private boolean qualifiesAsStrengthColumn(int column) {
		return mSourceTableModel.isColumnTypeDouble(column);
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

	private class SimilarPair implements Comparable<SimilarPair> {
		public int row1,row2;
		public float similarity;

		public SimilarPair(int row1, int row2, float similarity) {
			assert(row1 != row2);
			if (row1 < row2) {
				this.row1 = row1;
				this.row2 = row2;
				}
			else {
				this.row1 = row2;
				this.row2 = row1;
				}
			this.similarity = similarity;
			}

		@Override
		public int compareTo(SimilarPair o) {
			return (row1 < o.row1) ? -1 : (row1 > o.row1) ? 1 : (row2 < o.row2) ? -1 : (row2 > o.row2) ? 1 : 0;
			}
		}
	}
