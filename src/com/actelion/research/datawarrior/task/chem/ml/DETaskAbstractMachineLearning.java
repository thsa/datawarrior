package com.actelion.research.datawarrior.task.chem.ml;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.chem.DETaskAbstractFromStructure;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.calc.regression.ARegressionMethod;
import com.actelion.research.calc.regression.ConstantsRegressionMethods;
import com.actelion.research.calc.regression.RegressionMethodContainer;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Properties;

public abstract class DETaskAbstractMachineLearning extends DETaskAbstractFromStructure {
	public static final String TASK_NAME = "Predict Missing Values";

	protected static final String PROPERTY_VALUE_COLUMN = "valueColumn";
	private static final String PROPERTY_METHOD = "method";
	private static final String PROPERTY_PARAMS = "params";

	private JComboBox mComboBoxValueColumn;
	private JComboBox mComboBoxMethod;
	private JTextArea mTextAreaParams;

	private volatile boolean mValueIsLogarithmic;
	private volatile int mValueColumn;
	private volatile int mFullDataRowCount,mVariableCount,mDescriptorCount;
	private volatile int[] mFullDataRow,mInputColumn;
	private volatile Object[] mVaryingKeys;

	public DETaskAbstractMachineLearning(DEFrame parent) {
		super(parent, DESCRIPTOR_VECTOR, false, true);
	}

	@Override
	public boolean hasExtendedDialogContent() {
		return true;
	}

	protected abstract boolean qualifiesAsValueColumn(int column);
	protected abstract String getNoQualifyingValueColumnMessage();
	protected abstract double[] getExtendedYLayout(int gap);
	protected abstract void createMethod(int method, String[] param);
	protected abstract int getMinimumTrainingRowCount();
	protected abstract boolean buildModel(int validRowCount);

		@Override
	public JPanel getExtendedDialogContent() {
		JPanel ep = new JPanel();
		int gap = HiDPIHelper.scale(8);

		double[] sizeX = {TableLayout.PREFERRED, gap, TableLayout.PREFERRED};
		double[] sizeY = {TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap, HiDPIHelper.scale(80)};
		double[][] size = { sizeX, sizeY };

		double[] extendedSizeY = getExtendedYLayout(gap);
		if (extendedSizeY != null) {
			size[1] = Arrays.copyOf(sizeY, sizeY.length + extendedSizeY.length);
			for (int i=0; i<extendedSizeY.length; i++)
				size[1][i+sizeY.length] = extendedSizeY[i];
		}

		ep.setLayout(new TableLayout(size));

		mComboBoxValueColumn = new JComboBox();
		for (int i=0; i<getTableModel().getTotalColumnCount(); i++)
			if (qualifiesAsValueColumn(i))
				mComboBoxValueColumn.addItem(getTableModel().getColumnTitleExtended(i));
		mComboBoxValueColumn.setEditable(!isInteractive());
		ep.add(new JLabel("Value column:"), "0,0");
		ep.add(mComboBoxValueColumn, "2,0");

		mComboBoxMethod = new JComboBox(ConstantsRegressionMethods.ARR_MODEL);
		mComboBoxMethod.addActionListener(this);
		ep.add(new JLabel("Prediction method:"), "0,2");
		ep.add(mComboBoxMethod, "2,2");

		mTextAreaParams = new JTextArea();
		ep.add(new JLabel("Method parameters:"), "0,4");
		ep.add(mTextAreaParams, "2,4");

		addExtendedDialogContent(ep, sizeY.length);

		enableItems();

		return ep;
	}

	protected void addExtendedDialogContent(JPanel ep, int firstY) {}

	private void enableItems() {
/*		String selectedItem = (String)mComboBoxValueColumn.getSelectedItem();
		mComboBoxValueColumn.removeAllItems();
		for (int column=0; column<getTableModel().getTotalColumnCount(); column++)
			if (qualifiesAsValueColumn(column, !mCheckBoxTimeAxisPrediction.isSelected()))
				mComboBoxValueColumn.addItem(getTableModel().getColumnTitle(column));
		if (selectedItem != null)
			mComboBoxValueColumn.setSelectedItem(selectedItem);

		mComboBoxTimeAxisColumn.setEnabled(mCheckBoxTimeAxisPrediction.isSelected());
*/	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mComboBoxMethod) {
			updatePropertiesFromMethod(mComboBoxMethod.getSelectedIndex());
			return;
		}

		super.actionPerformed(e);
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_VALUE_COLUMN, getTableModel().getColumnTitleNoAlias((String)mComboBoxValueColumn.getSelectedItem()));
		configuration.setProperty(PROPERTY_METHOD, ConstantsRegressionMethods.ARR_MODEL_CODE[mComboBoxMethod.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_PARAMS, getMethodParamsFromDialog());
		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);

		String valueColumnName = configuration.getProperty(PROPERTY_VALUE_COLUMN, "");

		if (valueColumnName.length() != 0) {
			int column = getTableModel().findColumn(valueColumnName);
			if (column != -1 && qualifiesAsValueColumn(column))
				mComboBoxValueColumn.setSelectedItem(getTableModel().getColumnTitle(column));
			else if (!isInteractive())
				mComboBoxValueColumn.setSelectedItem(valueColumnName);
		}

		int method = findListIndex(configuration.getProperty(PROPERTY_METHOD), ConstantsRegressionMethods.ARR_MODEL_CODE, ConstantsRegressionMethods.DEFAULT_MODEL);
		mComboBoxMethod.setSelectedIndex(method);
		updatePropertiesFromMethod(method);

		String params = configuration.getProperty(PROPERTY_PARAMS);
		if (params == null)
			params = "";
		else
			params = params.replace(';', '\n');
		mTextAreaParams.setText(params);
	}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		if (mComboBoxValueColumn.getItemCount() != 0)
			mComboBoxValueColumn.setSelectedIndex(0);

		int method = ConstantsRegressionMethods.DEFAULT_MODEL;
		mComboBoxMethod.setSelectedIndex(method);
		updatePropertiesFromMethod(method);
	}

	private String getMethodParamsFromDialog() {
		String text = mTextAreaParams.getText();
		if (text.length() == 0)
			return null;

		StringBuilder sb = new StringBuilder();
		String[] lines = text.split("\\n");
		for (String line:lines) {
			int index = line.indexOf("=");
			if (index != -1 && line.indexOf(';') == -1) {
				String key = line.substring(0, index).trim();
				String value = line.substring(index+1).trim();
				if (key.length() != 0 && value.length() != 0) {
					if (sb.length() != 0)
						sb.append(";");
					sb.append(key);
					sb.append("=");
					sb.append(value);
				}
			}
		}
		return sb.toString();
	}

	private void updatePropertiesFromMethod(int method) {
		Properties params = RegressionMethodContainer.createRegressionMethod(ConstantsRegressionMethods.ARR_MODEL[method]).getProperties();
		StringBuilder sb = new StringBuilder();
		for (String key:params.stringPropertyNames()) {
			if (!key.equals("Name")) {
				sb.append(key);
				sb.append("=");
				sb.append(params.getProperty(key));
				sb.append("\n");
			}
		}
		mTextAreaParams.setText(sb.toString());
	}

	@Override
	public boolean isConfigurable() {
		boolean found = false;
		for (int i=0; i<getTableModel().getTotalColumnCount(); i++) {
			if (qualifiesAsValueColumn(i)) {
				found = true;
				break;
				}
			}

		if (!found) {
			showErrorMessage(getNoQualifyingValueColumnMessage());
			return false;
			}

		return super.isConfigurable();
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (!super.isConfigurationValid(configuration, isLive))
			return false;

		int method = findListIndex(configuration.getProperty(PROPERTY_METHOD), ConstantsRegressionMethods.ARR_MODEL_CODE, -1);
		if (method == -1) {
			showErrorMessage("Regression method '"+configuration.getProperty(PROPERTY_METHOD)+"' not found.");
			return false;
		}

		String params = configuration.getProperty(PROPERTY_PARAMS);
		String[] param = (params == null || params.length() == 0) ? new String[0] : params.split(";");

		Properties expectedParams = RegressionMethodContainer.createRegressionMethod(ConstantsRegressionMethods.ARR_MODEL[method]).getProperties();
		for (String key:expectedParams.stringPropertyNames()) {
			if (!key.equals("Name")) {
				boolean found = false;
				for (String p:param) {
					if (p.startsWith(key.concat("="))) {
						found = true;
						break;
					}
				}
				if (!found) {
					showErrorMessage("Method parameter '"+key+"' not found.");
					return false;
				}
			}
		}

		if (isLive) {
			String valueColumnName = configuration.getProperty(PROPERTY_VALUE_COLUMN);
			int valueColumn = getTableModel().findColumn(valueColumnName);
			if (valueColumn == -1) {
				showErrorMessage("Value column '"+valueColumnName+"' not found.");
				return false;
			}
			if (!getTableModel().isColumnTypeDouble(valueColumn)) {
				showErrorMessage("Value column '"+valueColumnName+"' is not perceived as containing numerical values.");
				return false;
			}
			if (!getTableModel().hasNumericalVariance(valueColumn)) {
				showErrorMessage("Value column '"+valueColumnName+"' does not contain numerically varying values.");
				return false;
			}
			if (valueColumnName.length() == 0 && getTableModel().isColumnDataComplete(valueColumn)) {
				showErrorMessage("Value column '"+valueColumnName+"' does not contain empty values.");
				return false;
			}
		}

		return true;
	}

	@Override
	protected boolean preprocessRows(Properties configuration) {
		mValueColumn = getTableModel().findColumn(configuration.getProperty(PROPERTY_VALUE_COLUMN));
		mValueIsLogarithmic = getTableModel().isLogarithmicViewMode(mValueColumn);
		mInputColumn = new int[1];    // currently one descriptor column and no numerical columns
		mInputColumn[0] = getDescriptorColumn();
		mDescriptorCount = 1;

		int method = findListIndex(configuration.getProperty(PROPERTY_METHOD), ConstantsRegressionMethods.ARR_MODEL_CODE, ConstantsRegressionMethods.DEFAULT_MODEL);
		String params = configuration.getProperty(PROPERTY_PARAMS);
		String[] param = (params == null || params.length() == 0) ? new String[0] : params.split(";");

		createMethod(method, param);

		int validRowCount = 0;
		for (int row = 0; row < getTableModel().getTotalRowCount(); row++)
			if (getDescriptor(row) != null)
				if (!Double.isNaN(getTableModel().getTotalRecord(row).getDouble(mValueColumn)))
					validRowCount++;

		if (validRowCount < getMinimumTrainingRowCount()) {
			showErrorMessage("Cannot build model because less than "+getMinimumTrainingRowCount()+" rows contain valid training data.");
			return false;
		}

		startProgress("Waiting for descriptor calculation...", 0, 0);
		waitForDescriptor(getTableModel(), getDescriptorColumn());

		startProgress("Initializing model...", 0, 0);

		determineFullDataRows();
		determineVaryingBits();

		sortFullDataRows(mFullDataRow, mFullDataRowCount);

		return buildModel(validRowCount);
	}

	protected void sortFullDataRows(int[] row, int rowCount) {} // no sorting as default

	protected ARegressionMethod createPredictionMethod(int method, String[] param) {
		ARegressionMethod rm = RegressionMethodContainer.createRegressionMethod(ConstantsRegressionMethods.ARR_MODEL[method]);

		Properties methodParams = rm.getProperties();
		for (String p : param) {
			int index = p.indexOf("=");
			methodParams.setProperty(p.substring(0, index), p.substring(index + 1));
		}

		rm.decodeProperties2Parameter();
		rm.setProgressController(this);

		return rm;
	}

	protected int getVariableCount() {
		return mVariableCount;
	}

	protected int getValueColumn() {
		return mValueColumn;
	}

	protected boolean isValueLogarithmic() {
			return mValueIsLogarithmic;
	}

	protected int getFullDataRow(int row) {
		return row >= mFullDataRowCount ? -1 : mFullDataRow[row];
	}

	protected int getFullDataRowCount() {
		return mFullDataRowCount;
	}

	private void determineFullDataRows() {
		mFullDataRowCount = 0;
		mFullDataRow = new int[getTableModel().getTotalRowCount()];
		for (int row=0; row<getTableModel().getTotalRowCount(); row++)
			if (isFullDataRow(row))
				mFullDataRow[mFullDataRowCount++] = row;

		if (mFullDataRowCount < 3)
			mFullDataRow = null;
	}

	private boolean isFullDataRow(int row) {
		CompoundRecord record = getTableModel().getTotalRecord(row);
		for (int i=0; i<mInputColumn.length; i++) {
			if (getTableModel().isDescriptorColumn(mInputColumn[i])) {
				if (record.getData(mInputColumn[i]) == null)
					return false;
			}
			else {
				if (Float.isNaN(record.getDouble(mInputColumn[i])))
					return false;
			}
		}
		return !Double.isNaN(getTableModel().getTotalRecord(row).getDouble(mValueColumn));
	}

	private void determineVaryingBits() {
		mVariableCount = 0;
		int[] varyingBits = new int[mDescriptorCount];
		mVaryingKeys = new Object[mDescriptorCount];
		for (int fp=0; fp<mDescriptorCount; fp++) {
			startProgress("Analysing '"+getTableModel().getColumnTitle(mInputColumn[fp])+"'...", 0, 0);
			if (getTableModel().getDescriptorHandler(mInputColumn[fp]).getInfo().isBinary) {
				if (getTableModel().getTotalRecord(mFullDataRow[0]).getData(mInputColumn[fp]) instanceof long[]) {
					long[] firstIndex = (long[])getTableModel().getTotalRecord(mFullDataRow[0]).getData(mInputColumn[fp]);
					mVaryingKeys[fp] = new long[firstIndex.length];
					for (int r=1; r<mFullDataRowCount; r++) {
						long[] currentIndex = (long[])getTableModel().getTotalRecord(mFullDataRow[r]).getData(mInputColumn[fp]);
						for (int i=0; i<firstIndex.length; i++)
							((long[]) mVaryingKeys[fp])[i] |= (firstIndex[i] ^ currentIndex[i]);
					}

					for (int i=0; i<firstIndex.length; i++)
						varyingBits[fp] += Long.bitCount(((long[]) mVaryingKeys[fp])[i]);
				}
				else {
					int[] firstIndex = (int[])getTableModel().getTotalRecord(mFullDataRow[0]).getData(mInputColumn[fp]);
					mVaryingKeys[fp] = new int[firstIndex.length];
					for (int r=1; r<mFullDataRowCount; r++) {
						int[] currentIndex = (int[])getTableModel().getTotalRecord(mFullDataRow[r]).getData(mInputColumn[fp]);
						for (int i = 0; i < firstIndex.length; i++)
							((int[]) mVaryingKeys[fp])[i] |= (firstIndex[i] ^ currentIndex[i]);
					}

					for (int i=0; i<firstIndex.length; i++)
						varyingBits[fp] += Integer.bitCount(((int[])mVaryingKeys[fp])[i]);
				}

				mVariableCount += varyingBits[fp];
			}
			else {
				byte[] firstIndex = (byte[])getTableModel().getTotalRecord(mFullDataRow[0]).getData(mInputColumn[fp]);
				boolean[] isVarying = new boolean[firstIndex.length];
				int varyingKeyCount = 0;
				for (int r=1; r<mFullDataRowCount; r++) {
					byte[] currentIndex = (byte[])getTableModel().getTotalRecord(mFullDataRow[r]).getData(mInputColumn[fp]);
					for (int i=0; i<firstIndex.length; i++) {
						if (!isVarying[i] && firstIndex[i] != currentIndex[i]) {
							isVarying[i] = true;
							varyingKeyCount++;
						}
					}
				}

				mVaryingKeys[fp] = new int[varyingKeyCount];
				int index = 0;
				for (int i=0; i<isVarying.length; i++)
					if (isVarying[i])
						((int[])mVaryingKeys[fp])[index++] = i;

				mVariableCount += varyingKeyCount;
			}
		}
	}

	/**
	 * @param row
	 * @param rowParameter
	 */
	protected void calculateParameterRow(int row, double[] rowParameter) {
		int paramIndex = 0;

		for (int fp=0; fp<mDescriptorCount; fp++) {
			if (getTableModel().getDescriptorHandler(mInputColumn[fp]).getInfo().isBinary) {
				if (getTableModel().getTotalRecord(row).getData(mInputColumn[fp]) instanceof long[]) {
					long[] currentIndex = (long[])getTableModel().getTotalRecord(row).getData(mInputColumn[fp]);
					for (int i=0; i<currentIndex.length; i++) {
						long theBit = 1L;
						for (int j=0; j<64; j++) {
							if ((((long[])mVaryingKeys[fp])[i] & theBit) != 0) {
								rowParameter[paramIndex++] = ((currentIndex[i] & theBit) != 0) ? 1.0 : 0.0;
							}
							theBit <<= 1;
						}
					}
				}
				else {
					int[] currentIndex = (int[])getTableModel().getTotalRecord(row).getData(mInputColumn[fp]);
					for (int i=0; i<currentIndex.length; i++) {
						int theBit = 1;
						for (int j=0; j<32; j++) {
							if ((((int[])mVaryingKeys[fp])[i] & theBit) != 0) {
								rowParameter[paramIndex++] = ((currentIndex[i] & theBit) != 0) ? 1.0 : 0.0;
							}
							theBit <<= 1;
						}
					}
				}
			}
			else {
				byte[] currentIndex = (byte[])getTableModel().getTotalRecord(row).getData(mInputColumn[fp]);
				for (int i=0; i<((int[])mVaryingKeys[fp]).length; i++)
					rowParameter[paramIndex++] = currentIndex[((int[])mVaryingKeys[fp])[i]];
			}
		}

		for (int i=mDescriptorCount; i<mInputColumn.length; i++) {
			int column = mInputColumn[i];
			rowParameter[paramIndex++] = getTableModel().getTotalDoubleAt(row, column);
		}
	}
}
