package com.actelion.research.datawarrior.task.chem.ml;

import com.actelion.research.calc.Matrix;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.util.DoubleFormat;
import com.actelion.research.util.datamodel.ModelXYIndex;
import com.actelion.research.calc.regression.ARegressionMethod;

public class DETaskPredictMissingValues extends DETaskAbstractMachineLearning {
	public static final String TASK_NAME = "Predict Missing Values";

	// TODO add uncertainty column
	private static final String[] PREDICTION_COLUMN_NAME = {};

	private volatile ARegressionMethod mPredictionMethod;

	public DETaskPredictMissingValues(DEFrame parent) {
		super(parent);
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public String getHelpURL() {
		return "/html/help/ml.html#MissingValues";
	}

	@Override
	protected int getNewColumnCount() {
		return PREDICTION_COLUMN_NAME.length;
	}

	@Override
	protected String getNewColumnName(int column) {
		return PREDICTION_COLUMN_NAME[column];
	}

	@Override
	protected double[] getExtendedYLayout(int gap) {
		return null;
	}

	@Override
	protected boolean qualifiesAsValueColumn(int column) {
		return getTableModel().isColumnTypeDouble(column)
			&& getTableModel().hasNumericalVariance(column)
			&& !getTableModel().isColumnDataComplete(column);
		}

	@Override
	protected String getNoQualifyingValueColumnMessage() {
		return "No numerical column with empty and varying values found.";
	}

	@Override
	protected void createMethod(int method, String[] param) {
		mPredictionMethod = createPredictionMethod(method, param);
	}

	@Override
	protected int getMinimumTrainingRowCount() {
		return 3;
	}

	protected boolean buildModel(int validRowCount) {
		double[][] ax = new double[validRowCount][];
		double[][] ay = new double[validRowCount][1];

		for (int i=0; i<getFullDataRowCount(); i++) {
			int row = getFullDataRow(i);
			ax[i] = new double[getVariableCount()];
			calculateParameterRow(row, ax[i]);
			ay[i][0] = getTableModel().getTotalRecord(row).getDouble(getValueColumn());
		}

		ModelXYIndex modelXY = new ModelXYIndex();
		modelXY.X = new Matrix(ax, true);
		modelXY.Y = new Matrix(ay, true);

		if (threadMustDie())
			return false;
		startProgress("Building model...", 0, 0);

		mPredictionMethod.createModel(modelXY);	// does training

		if (threadMustDie())
			return false;
		startProgress("Predicting...", 0, 0);

		return true;
	}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol) throws Exception {
		if (Double.isNaN(getTableModel().getTotalRecord(row).getDouble(getValueColumn())) && getDescriptor(row) != null) {
			double[] x = new double[getVariableCount()];
			calculateParameterRow(row, x);
			double y = mPredictionMethod.calculateYHat(x);
			double value = isValueLogarithmic() ? Math.pow(10.0, y) : y;
			getTableModel().setTotalValueAt(DoubleFormat.toString(value), row, getValueColumn());
		}
	}

	@Override
	protected void postprocess(int firstNewColumn) {
		stopProgress();
		getTableModel().finalizeChangeAlphaNumericalColumn(getValueColumn(), 0, getTableModel().getTotalRowCount());
	}
}
