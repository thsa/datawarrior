package com.actelion.research.datawarrior.task.chem.ml;

import com.actelion.research.calc.CorrelationCalculator;
import com.actelion.research.calc.Matrix;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.JVisualization2D;
import com.actelion.research.table.view.VisualizationColor;
import com.actelion.research.table.view.VisualizationPanel2D;
import com.actelion.research.util.ByteArrayComparator;
import com.actelion.research.util.DoubleFormat;
import com.actelion.research.util.datamodel.ModelXYIndex;
import com.actelion.research.calc.regression.ARegressionMethod;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class DETaskAssessPredictionQuality extends DETaskAbstractMachineLearning {
	public static final String TASK_NAME = "Assess Prediction Quality";

	private static final String PROPERTY_TIME_COLUMN = "timeColumn";

	private static final String[] TIME_STEP_COLUMN_NAME = { "Prediction Fraction" , "Predicted Value"};
	private static final int TIME_FRACTION_COUNT = 9;

	private JCheckBox mCheckBoxUseTimeAxis;
	private JComboBox mComboBoxTimeAxisColumn;
	private volatile int mTimeColumn;
	private volatile int[] mTimeFractionFirstRow;
	private volatile ARegressionMethod[] mTimeFractionMethod;
	private volatile AtomicInteger mSMPModelIndex;

	public DETaskAssessPredictionQuality(DEFrame parent) {
		super(parent);
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public String getHelpURL() {
		return "/html/help/ml.html#Predictivity";
	}

	@Override
	public String getDialogTitle() {
		return "Assess Prediction Quality Along Time Axis";
		}

	@Override
	protected int getNewColumnCount() {
		return TIME_STEP_COLUMN_NAME.length;
	}

	@Override
	protected String getNewColumnName(int column) {
		return TIME_STEP_COLUMN_NAME[column];
	}

	@Override
	protected boolean qualifiesAsValueColumn(int column) {
		return getTableModel().isColumnTypeDouble(column)
			&& getTableModel().hasNumericalVariance(column);
	}

	@Override
	protected String getNoQualifyingValueColumnMessage() {
		return "No complete numerical column with varying values found.";
	}

	private boolean qualifiesAsTimeColumn(int column) {
		if (getTableModel().getColumnSpecialType(column) != null)
			return false;

		if (!getTableModel().isColumnDataComplete(column))
			return false;

		if (getTableModel().isColumnTypeDouble(column)
		 && !getTableModel().hasNumericalVariance(column))
			return false;

		if (getTableModel().isColumnTypeCategory(column)
		 && getTableModel().getCategoryCount(column) <= TIME_FRACTION_COUNT)
			return false;

		return true;
	}

	public double[] getExtendedYLayout(int gap) {
		double[] extSizeY = {2*gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED};
		return extSizeY;
	}

	@Override
	public void addExtendedDialogContent(JPanel ep, int y) {
		y++;

		mComboBoxTimeAxisColumn = new JComboBox();
		for (int column=0; column<getTableModel().getTotalColumnCount(); column++)
			if (qualifiesAsTimeColumn(column))
				mComboBoxTimeAxisColumn.addItem(getTableModel().getColumnTitle(column));
		mComboBoxTimeAxisColumn.setEditable(!isInteractive());
		ep.add(new JLabel("Time, Date or ID column:"), "0,"+y);
		ep.add(mComboBoxTimeAxisColumn, "2,"+y);

		y += 2;
		mCheckBoxUseTimeAxis = new JCheckBox("Use random fractions instead of time based ones");
		mCheckBoxUseTimeAxis.addActionListener(this);
		ep.add(mCheckBoxUseTimeAxis, "0,"+y+",2,"+y);
	}

	@Override public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mCheckBoxUseTimeAxis) {
			if (mCheckBoxUseTimeAxis.isSelected()) {
				int answer = JOptionPane.showConfirmDialog(getParentFrame(),
						"Random cross-validations tend to over-optimistic\ncorrelations, which wrongly suggest predictivity.\nDo you want to continue anyway?",
						"Warning", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
				if (answer == JOptionPane.CANCEL_OPTION)
					mCheckBoxUseTimeAxis.setSelected(false);
				}

			enableComboBox();
			return;
			}

		super.actionPerformed(e);
		}

	private void enableComboBox() {
		mComboBoxTimeAxisColumn.setEnabled(!mCheckBoxUseTimeAxis.isSelected());
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		if (!mCheckBoxUseTimeAxis.isSelected())
			configuration.setProperty(PROPERTY_TIME_COLUMN, getTableModel().getColumnTitleNoAlias((String)mComboBoxTimeAxisColumn.getSelectedItem()));
		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);

		String timeColumnName = configuration.getProperty(PROPERTY_TIME_COLUMN, "");
		if (timeColumnName.length() != 0) {
			int column = getTableModel().findColumn(timeColumnName);
			if (column != -1 && qualifiesAsTimeColumn(column))
				timeColumnName = getTableModel().getColumnTitle(column);
			else if (isInteractive())
				timeColumnName = "";
		}
		if (timeColumnName.length() != 0)
			mComboBoxTimeAxisColumn.setSelectedItem(timeColumnName);

		mCheckBoxUseTimeAxis.setSelected(timeColumnName.length() == 0);

		enableComboBox();
	}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();

		if (mComboBoxTimeAxisColumn.getItemCount() != 0)
			mComboBoxTimeAxisColumn.setSelectedIndex(0);

		mCheckBoxUseTimeAxis.setSelected(mComboBoxTimeAxisColumn.getItemCount() == 0);

		enableComboBox();
	}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (!super.isConfigurationValid(configuration, isLive))
			return false;

		if (isLive) {
			String timeColumnName = configuration.getProperty(PROPERTY_TIME_COLUMN, "");
			if (timeColumnName.length() != 0) {
				int timeColumn = getTableModel().findColumn(timeColumnName);
				if (timeColumn == -1) {
					showErrorMessage("Time column '"+timeColumnName+"' not found.");
					return false;
				}
				if (!qualifiesAsTimeColumn(timeColumn)) {
					showErrorMessage("Time column '"+timeColumnName+"' cannot be used to sort value,\npossibly because it contains empty value or not enough distinct values.");
					return false;
				}
				String valueColumnName = configuration.getProperty(PROPERTY_VALUE_COLUMN);
				int valueColumn = getTableModel().findColumn(valueColumnName);
				if (timeColumn == valueColumn) {
					showErrorMessage("The time column and value column cannot be the same column.");
					return false;
				}
			}
		}

		return true;
	}

	@Override
	protected boolean preprocessRows(Properties configuration) {
		mTimeColumn = getTableModel().findColumn(configuration.getProperty(PROPERTY_TIME_COLUMN));
		return super.preprocessRows(configuration);
	}

	@Override
	protected int getMinimumTrainingRowCount() {
		return (1 + TIME_FRACTION_COUNT) * 3;
	}

	protected void createMethod(int method, String[] param) {
		mTimeFractionMethod = new ARegressionMethod[TIME_FRACTION_COUNT];
		for (int i = 0; i < TIME_FRACTION_COUNT; i++)
			mTimeFractionMethod[i] = createPredictionMethod(method, param);
	}

	@Override
	protected void sortFullDataRows(int[] row, int rowCount) {
		if (row != null) {
			if (mTimeColumn == -1 || getTableModel().isColumnTypeDouble(mTimeColumn)) {
				SortableDoubleRowRef[] ref = new SortableDoubleRowRef[rowCount];
				for (int i=0; i<rowCount; i++)
					ref[i] = new SortableDoubleRowRef(row[i], mTimeColumn == -1 ? (float)Math.random()
							: getTableModel().getTotalRecord(row[i]).getDouble(mTimeColumn));
				Arrays.sort(ref);
				for (int i=0; i<rowCount; i++)
					row[i] = ref[i].row;
			}
			else {
				SortableStringRowRef[] ref = new SortableStringRowRef[rowCount];
				for (int i=0; i<rowCount; i++)
					ref[i] = new SortableStringRowRef(row[i], (byte[])getTableModel().getTotalRecord(row[i]).getData(mTimeColumn));
				Arrays.sort(ref);
				for (int i=0; i<rowCount; i++)
					row[i] = ref[i].row;
			}
		}
	}

	@Override
	protected boolean buildModel(int validRowCount) {
		mTimeFractionFirstRow = new int[TIME_FRACTION_COUNT];
		for (int i=0; i<TIME_FRACTION_COUNT; i++)
			mTimeFractionFirstRow[i] = Math.round((float)(i+1)/(float)(TIME_FRACTION_COUNT+1) * getFullDataRowCount());

		startProgress("Building models...", 0, 0);
		buildTimeFractionModels();

		startProgress("Predicting...", 0, 0);
		return true;
	}

	private void buildTimeFractionModels() {
		int threadCount = Runtime.getRuntime().availableProcessors();
		mSMPModelIndex = new AtomicInteger(TIME_FRACTION_COUNT);

		Thread[] t = new Thread[threadCount];
		for (int i=0; i<threadCount; i++) {
			t[i] = new Thread("Model Calculator "+(i+1)) {
				public void run() {
					int modelIndex = mSMPModelIndex.decrementAndGet();
					while (modelIndex >= 0 && !threadMustDie()) {
						buildTimeFractionModel(modelIndex);
						modelIndex = mSMPModelIndex.decrementAndGet();
					}
				}
			};
			t[i].setPriority(Thread.MIN_PRIORITY);
			t[i].start();
		}

		// the controller thread must wait until all others are finished
		// before the next task can begin or the dialog is closed
		for (int i=0; i<threadCount; i++)
			try { t[i].join(); } catch (InterruptedException e) {}
	}

	private void buildTimeFractionModel(int timeFractionIndex) {
		int rowIndex1 = 0;
//		int rowIndex1 = (timeFractionIndex == 0 ? 0 : mTimeFractionFirstRow[timeFractionIndex-1]);
		int rowIndex2 = mTimeFractionFirstRow[timeFractionIndex];
		int validRowCount = rowIndex2 - rowIndex1;
		double[][] ax = new double[validRowCount][];
		double[][] ay = new double[validRowCount][1];

		for (int i=0; i<validRowCount; i++) {
			int row = getFullDataRow(rowIndex1+i);
			ax[i] = new double[getVariableCount()];
			calculateParameterRow(row, ax[i]);
			ay[i][0] = getTableModel().getTotalRecord(row).getDouble(getValueColumn());
		}

		ModelXYIndex modelXY = new ModelXYIndex();
		modelXY.X = new Matrix(ax, true);
		modelXY.Y = new Matrix(ay, true);

		if (threadMustDie())
			return;

		mTimeFractionMethod[timeFractionIndex].createModel(modelXY);	// does training
	}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol) throws Exception {
		// We interpret the input row index as pointer into the time sorted row index table
		if (row < getFullDataRowCount()) {
			int rowInTime = row;
			row = getFullDataRow(rowInTime);

			int timeFractionIndex = TIME_FRACTION_COUNT-1;
			while (timeFractionIndex >= 0 && rowInTime < mTimeFractionFirstRow[timeFractionIndex])
				timeFractionIndex--;

			getTableModel().setTotalValueAt(Integer.toString(2+timeFractionIndex), row, firstNewColumn);

			if (timeFractionIndex >= 0) { // we don't predict for the first fraction
				if (getDescriptor(row) != null) {
					double[][] x = new double[1][getVariableCount()];
					calculateParameterRow(row, x[0]);
					Matrix y = mTimeFractionMethod[timeFractionIndex].calculateYHat(new Matrix(x));
					double value = isValueLogarithmic() ? Math.pow(10.0, y.get(0,0)) : y.get(0,0);
					getTableModel().setTotalValueAt(DoubleFormat.toString(value), row, firstNewColumn+1);
				}
			}
		}
	}

	@Override
	protected void postprocess(int firstNewColumn) {
		stopProgress();

		getTableModel().finalizeNewColumns(firstNewColumn, this);
		if (isValueLogarithmic())
			getTableModel().setLogarithmicViewMode(firstNewColumn+1, true);

		String comment = "Predicted"
					   + (getDescriptorColumn() == -1 ? "" : " from "+getTableModel().getColumnTitle(getDescriptorColumn()))
					   + " using "+(mTimeColumn == -1 ? "random cross-validation"
							: TIME_FRACTION_COUNT+" fractions based on "+getTableModel().getColumnTitle(mTimeColumn));
		getTableModel().setColumnDescription(comment, firstNewColumn+1);

		SwingUtilities.invokeLater(() -> {
			String valueColumn = getTableModel().getColumnTitle(getValueColumn());
			String predictedValueColumn = getTableModel().getColumnTitle(firstNewColumn+1);
			VisualizationPanel2D vpanel = ((DEFrame)getParentFrame()).getMainFrame().getMainPane().add2DView("Prediction Correlation", null);
			JVisualization2D visualization = (JVisualization2D)vpanel.getVisualization();
			vpanel.setAxisColumnName(0, valueColumn);
			vpanel.setAxisColumnName(1, predictedValueColumn);
			visualization.setShowNaNValues(false);
			visualization.setGridMode(JVisualization.cGridModeHidden);
			visualization.setPreferredChartType(JVisualization.cChartTypeScatterPlot, -1, -1);
			visualization.setMarkerSize(0.5f, false);
			int colorListMode = VisualizationColor.cColorListModeCategories;
			visualization.getMarkerColor().setColor(firstNewColumn, null, colorListMode);
			visualization.setSplittingColumns(firstNewColumn, -1, 1f, false);
			visualization.setCurveMode(JVisualization2D.cCurveModeFitted, true, false);
			visualization.setShownCorrelationType(CorrelationCalculator.TYPE_BRAVAIS_PEARSON);
		} );
	}
}

class SortableDoubleRowRef implements Comparable<SortableDoubleRowRef> {
	int row;
	float value;

	public SortableDoubleRowRef(int row, float value) {
		this.row = row;
		this.value = value;
	}

	@Override
	public int compareTo(SortableDoubleRowRef o) {
		return Float.isNaN(value) ? (Float.isNaN(o.value) ? 0 : 1)
				: Float.isNaN(o.value) ? -1
				: (value == o.value) ? 0
				: (value < o.value) ? -1 : 1;
	}
}

class SortableStringRowRef implements Comparable<SortableStringRowRef> {
	static ByteArrayComparator c = new ByteArrayComparator();
	int row;
	byte[] value;

	public SortableStringRowRef(int row, byte[] value) {
		this.row = row;
		this.value = value;
	}

	@Override
	public int compareTo(SortableStringRowRef o) {
		return c.compare(value, o.value);
	}
}