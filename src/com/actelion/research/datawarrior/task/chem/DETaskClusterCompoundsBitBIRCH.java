package com.actelion.research.datawarrior.task.chem;

import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorHandler;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;
import org.openmolecules.bitbirch.BitBirch;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * DataWarrior task performing BitBIRCH-style clustering on binary fingerprints
 * already present in the current DataWarrior table.
 *
 * Input:
 *   A text column containing hexadecimal fingerprints.
 *
 * Output:
 *   A new integer column containing the cluster number for each row.
 *
 * Typical configuration:
 *   distance threshold = 0.30
 *   maximum cluster size = 100
 *   worker threads = available processors
 */
public class DETaskClusterCompoundsBitBIRCH extends ConfigurableTask {
	public static final String TASK_NAME = "Cluster Compounds BitBIRCH";

	private static final String PROPERTY_DESCRIPTOR_COLUMN = "descriptorColumn";
	private static final String PROPERTY_THRESHOLD = "threshold";
	private static final String PROPERTY_BRANCHING = "branching";

	private static final String[] cClusterColumnName = {"BitBIRCH Cluster No", "Is Representative"};

	private final DEFrame mParentFrame;
	private final CompoundTableModel mTableModel;

	private JComboBox<String> mComboBoxDescriptorColumn;
	private JTextField mTextFieldThreshold,mTextFieldBranching;

	public DETaskClusterCompoundsBitBIRCH(DEFrame parent) {
		super(parent, true);

	   mParentFrame = parent;
	   mTableModel = parent.getTableModel();
	}


	@Override
	public JPanel createDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, TableLayout.FILL, gap},
				{gap, TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		content.add(new JLabel("Descriptor:"), "1,1");
		mComboBoxDescriptorColumn = new JComboBox<>();
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (qualifiesAsDescriptorColumn(column))
				mComboBoxDescriptorColumn.addItem(mTableModel.getColumnTitle(column));
		mComboBoxDescriptorColumn.setEditable(!isInteractive());
		content.add(mComboBoxDescriptorColumn, "3,1,4,1");

		content.add(new JLabel("Threshold:"), "1,3");
		mTextFieldThreshold = new JTextField(4);
		content.add(mTextFieldThreshold, "3,3");

		content.add(new JLabel("Branching:"), "1,5");
		mTextFieldBranching = new JTextField(4);
		content.add(mTextFieldBranching, "3,5");

		return content;
	}

	private boolean qualifiesAsDescriptorColumn(int column) {
		return mTableModel.isDescriptorColumn(column) && mTableModel.getDescriptorHandler(column).getInfo().isBinary;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		String descriptorColumn = (String)mComboBoxDescriptorColumn.getSelectedItem();
		if (descriptorColumn != null)
			configuration.setProperty(PROPERTY_DESCRIPTOR_COLUMN, descriptorColumn);

		if (!mTextFieldThreshold.getText().isEmpty())
			configuration.setProperty(PROPERTY_THRESHOLD, mTextFieldThreshold.getText());

		if (!mTextFieldBranching.getText().isEmpty())
			configuration.setProperty(PROPERTY_BRANCHING, mTextFieldBranching.getText());

		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String value = configuration.getProperty(PROPERTY_DESCRIPTOR_COLUMN, "");
		if (!value.isEmpty()) {
			int column = mTableModel.findColumn(value);
			if (column != -1 && qualifiesAsDescriptorColumn(column))
				mComboBoxDescriptorColumn.setSelectedItem(mTableModel.getColumnTitle(column));
			else if (!isInteractive())
				mComboBoxDescriptorColumn.setSelectedItem(value);
			else if (mComboBoxDescriptorColumn.getItemCount() != 0)
				mComboBoxDescriptorColumn.setSelectedIndex(0);
			}
		else if (!isInteractive()) {
			mComboBoxDescriptorColumn.setSelectedItem("Structure ["+ DescriptorConstants.DESCRIPTOR_FFP512.shortName+"]");
			}

		mTextFieldThreshold.setText(configuration.getProperty(PROPERTY_THRESHOLD, ""));
		mTextFieldBranching.setText(configuration.getProperty(PROPERTY_BRANCHING, ""));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mComboBoxDescriptorColumn.getItemCount() != 0)
			mComboBoxDescriptorColumn.setSelectedIndex(0);

		mTextFieldThreshold.setText("0.65");
		mTextFieldBranching.setText("50");
	}


	@Override
	public Properties getPredefinedConfiguration() {
		return null;
	}

	@Override
	public boolean isConfigurable() {
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (qualifiesAsDescriptorColumn(column))
				return true;

		showErrorMessage("No binary descriptor found.");
		return false;
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String threshold = configuration.getProperty(PROPERTY_THRESHOLD);
		String branching = configuration.getProperty(PROPERTY_BRANCHING);

		if (threshold == null) {
			showErrorMessage("No threshold value defined.");
			return false;
		}

		if (branching == null) {
			showErrorMessage("No branching value defined.");
			return false;
		}

		try {
			double t = Double.parseDouble(threshold);
			if (t < 0.1 || t >= 1.0) {
				showErrorMessage("The similarity threshold must be between 0.1 and 1.0");
				return false;
				}
			}
		   catch (NumberFormatException nfe) {
			   showErrorMessage("The similarity threshold is not numerical");
			   return false;
		   }

		try {
			double b = Integer.parseInt(branching);
			if (b < 2 || b > 100) {
				showErrorMessage("The branching parameter should be between 2 and 100");
				return false;
			}
		}
		catch (NumberFormatException nfe) {
			showErrorMessage("The branching parameter is not an integer value.");
			return false;
		}

		String descriptorName = configuration.getProperty(PROPERTY_DESCRIPTOR_COLUMN, "");
		if (descriptorName.isEmpty()) {
			showErrorMessage("Descriptor column not defined.");
			return false;
		}
		int descriptorColumn = -1;
		if (isLive) {
			descriptorColumn = mTableModel.findColumn(descriptorName);
			if (descriptorColumn == -1) {
				showErrorMessage("Descriptor column '"+descriptorName+"' not found.");
				return false;
			}
			if (!qualifiesAsDescriptorColumn(descriptorColumn)) {
				showErrorMessage("Descriptor column '"+descriptorName+"' is not a binary descriptor.");
				return false;
			}
		}
		return true;
	}

	@Override
	public void runTask(Properties configuration) {
		int descriptorColumn = mTableModel.findColumn(configuration.getProperty(PROPERTY_DESCRIPTOR_COLUMN));
		waitForDescriptor(mTableModel, descriptorColumn);
		if (threadMustDie())
			return;

		double threshold = Double.parseDouble(configuration.getProperty( PROPERTY_THRESHOLD));
		int branching = Integer.parseInt(configuration.getProperty( PROPERTY_BRANCHING));

		int rowCount = mTableModel.getTotalRowCount();

		List<long[]> fpList = new ArrayList<>(rowCount);
		int[] indexToRow = new int[rowCount];

		for (int row = 0; row < rowCount; row++) {
			long[] fp = (long[])mTableModel.getTotalRecord(row).getData(descriptorColumn);

			if (fp == null)
				continue;

			indexToRow[fpList.size()] = row;
			fpList.add(fp);
		}

		if (fpList.isEmpty()) {
			showErrorMessage("No valid fingerprints were found.");
			return;
		}

		int threadCount = Runtime.getRuntime().availableProcessors();

		startProgress("BitBIRCH Clustering...", 0, 0);

		long[][] fps = fpList.toArray(new long[0][]);
		BitBirch t = BitBirch.fitParallel(fps, threshold, branching, threadCount);
		int[][] clusters = t.getClusters();						// member ids, largest cluster first
//		int[]	labels	= t.getLabels();						// labels[id] -> index into clusters
		long[][] centroids = t.getCentroids();					// binary majority vote, same order

		int clusterColumn = mTableModel.addNewColumns(cClusterColumnName);
		DescriptorHandler<long[], Molecule> dh = (DescriptorHandler<long[], Molecule>)mTableModel.getDescriptorHandler(descriptorColumn);

double[] meanDist = new double[clusters.length];
double[] maxDist = new double[clusters.length];
long[][] maxDistFP = new long[clusters.length][];

		for (int c=0; c<clusters.length; c++) {
			long[] centroid = centroids[c];
			int[] indexes = clusters[c];
			int represenative = -1;
			double bestSimilarity = 0.0;
if (indexes.length == 1)
	maxDistFP[c] = fps[indexes[0]];
			for (int i : indexes) {
				int row = indexToRow[i];
				double similarity = dh.getSimilarity(centroid, fps[i]);
meanDist[c] += 1.0 - similarity;
if (maxDist[c] < 1.0 - similarity) {
	maxDist[c] = 1.0 - similarity;
	maxDistFP[c] = fps[i];
}
				if (bestSimilarity < similarity) {
					represenative = row;
					bestSimilarity = similarity;
				}
				mTableModel.setTotalValueAt(Integer.toString(c+1), row, clusterColumn);
				mTableModel.setTotalValueAt("No", row, clusterColumn + 1);
			}
			mTableModel.setTotalValueAt("Yes", represenative, clusterColumn + 1);
meanDist[c] /= indexes.length;
		}

double[] minClusterDist = new double[clusters.length];
Arrays.fill(minClusterDist, 1.0);
double[] meanClusterDist = new double[clusters.length];
int[] minClusterID = new int[clusters.length];
double[] worstMemberToOtherClusterDist = new double[clusters.length];
Arrays.fill(worstMemberToOtherClusterDist, 1.0);
int[] otherClusterID = new int[clusters.length];
for (int c1=0; c1<clusters.length; c1++) {
	long[] centroid1 = centroids[c1];
	for (int c2=0; c2<clusters.length; c2++) {
		if (c1 != c2) {
			long[] centroid2 = centroids[c2];
			double dist = 1.0 - dh.getSimilarity(centroid1, centroid2);
			meanClusterDist[c1] += dist;
			if (minClusterDist[c1] > dist) {
				minClusterDist[c1] = dist;
				minClusterID[c1] = c2;
			}
			double worstMemberdist = 1.0 - dh.getSimilarity(maxDistFP[c1], centroid2);
			if (worstMemberToOtherClusterDist[c1] > worstMemberdist) {
				worstMemberToOtherClusterDist[c1] = worstMemberdist;
				otherClusterID[c1] = c2;
			}
		}
	}
	meanClusterDist[c1] /= (clusters.length - 1);
}
System.out.println("clusterID\tclusterSize\tmeanDistToCentroid\tmaxDistToCentroid\tmeanClusterDist\tminClusterDist\tminClusterID\tworstMemberToOtherClusterDist\totherClusterID");
for (int i=0; i<clusters.length; i++)
	System.out.println(i+"\t"+clusters[i].length+"\t"+DoubleFormat.toString(meanDist[i])+"\t"+DoubleFormat.toString(maxDist[i])
			+"\t"+DoubleFormat.toString(meanClusterDist[i])+"\t"+DoubleFormat.toString(minClusterDist[i])+"\t"+minClusterID[i]
			+"\t"+DoubleFormat.toString(worstMemberToOtherClusterDist[i])+"\t"+otherClusterID[i]);

		mTableModel.finalizeNewColumns(clusterColumn, this);
	}
}
