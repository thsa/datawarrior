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
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.BinGenerator;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Properties;

public class DETaskAddBinsFromNumbers extends ConfigurableTask implements ChangeListener,ItemListener,KeyListener {
	public static final String TASK_NAME = "Add Bins From Numbers";

	private static final String PROPERTY_COLUMN = "column";
	private static final String PROPERTY_BIN_START = "binStart";
	private static final String PROPERTY_BIN_SIZE = "binSize";
	private static final String PROPERTY_LOGARITHMIC = "logarithmic";
	private static final String PROPERTY_CUSTOM_VALUES = "customValues";
	private static final String PROPERTY_CUSTOM_NAMES = "customNames";

	private static final String DAYS = " days";

	private double cMaxNoOfBins = 220.0;

	CompoundTableModel	mTableModel;
	JComboBox			mComboBoxColumn;
	JSlider				mSliderBinSize,mSliderBinShift;
	JTextField			mTextFieldBinSize,mTextFieldBinStart,mTextFieldCustomValues,mTextFieldCustomNames;
	JCheckBox			mCheckBoxLogarithmic,mCheckBoxUseCustomBins;
	BinningPreview		mPreview;
	int					mSliderBinSizeValue,mSliderBinShiftValue;
	double[]			mCustomValue;
	String[]            mCustomName;

	/**
	 * @param parent
	 */
	public DETaskAddBinsFromNumbers(DEFrame parent) {
		super(parent, true);
		mTableModel = parent.getTableModel();
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
	public boolean isConfigurable() {
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (columnQualifies(column))
				return true;

		showErrorMessage("No column with numerical data found.");
		return false;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public JComponent createDialogContent() {
		int gap = HiDPIHelper.scale(8);
		JPanel content = new JPanel();
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.FILL, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, TableLayout.FILL, gap},
							{gap, TableLayout.PREFERRED, gap, TableLayout.FILL,
								TableLayout.PREFERRED, gap, TableLayout.PREFERRED, TableLayout.FILL,
								TableLayout.PREFERRED, gap, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.FILL,
							3*gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED} };
		content.setLayout(new TableLayout(size));

		JPanel cp = new JPanel();
		mComboBoxColumn = new JComboBox();
		for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
			if (columnQualifies(i))
				mComboBoxColumn.addItem(mTableModel.getColumnTitleExtended(i));
		mComboBoxColumn.setEditable(!isInteractive());
		mComboBoxColumn.setSelectedIndex(-1);
		mComboBoxColumn.addItemListener(this);

		cp.add(new JLabel("Create bins from "));
		cp.add(mComboBoxColumn);
		content.add(cp, "1,1,9,1");

		mTextFieldBinSize = new JTextField(8);
		mTextFieldBinSize.addKeyListener(this);
		mTextFieldBinStart = new JTextField(8);
		mTextFieldBinStart.addKeyListener(this);
		content.add(new JLabel("Bin Size:", SwingConstants.RIGHT), "6,4");
		content.add(mTextFieldBinSize, "8,4");
		content.add(new JLabel("First Bin From:", SwingConstants.RIGHT), "6,8");
		content.add(mTextFieldBinStart, "8,8");
		mSliderBinSize = new JSlider(JSlider.HORIZONTAL, 0, 256, 128);
		mSliderBinShift = new JSlider(JSlider.HORIZONTAL, 0, 256, 128);
		content.add(mSliderBinSize, "5,6,9,6");
		content.add(mSliderBinShift, "5,10,9,10");
		mCheckBoxLogarithmic = new JCheckBox("Values are logarithms");
		mCheckBoxLogarithmic.setEnabled(!isInteractive());
		content.add(mCheckBoxLogarithmic, "5,11,9,11");

		content.add(mPreview = new BinningPreview(mTableModel), "1,3,3,12");

		if (isInteractive()) {
			mSliderBinSize.addChangeListener(this);
			mSliderBinShift.addChangeListener(this);
			}

		mCheckBoxUseCustomBins = new JCheckBox("Use custom bin threshold values");
		mCheckBoxUseCustomBins.addActionListener(e -> { enableItems(); updatePreview(); } );
		content.add(mCheckBoxUseCustomBins, "1,14,9,14");
		mTextFieldCustomValues = new JTextField();
		mTextFieldCustomValues.addKeyListener(this);
		mTextFieldCustomNames = new JTextField();
		mTextFieldCustomNames.addKeyListener(this);
		content.add(new JLabel("Custom threshold values:", SwingConstants.RIGHT), "1,16");
		content.add(mTextFieldCustomValues, "3,16,9,16");
		content.add(new JLabel("Custom bin names:", SwingConstants.RIGHT), "1,18");
		content.add(mTextFieldCustomNames, "3,18,9,18");

		enableItems();

		return content;
		}

	private void enableItems() {
		boolean isCustom = mCheckBoxUseCustomBins.isSelected();
		mSliderBinSize.setEnabled(!isCustom && isInteractive());
		mSliderBinShift.setEnabled(!isCustom && isInteractive());
		mTextFieldBinSize.setEnabled(!isCustom);
		mTextFieldBinStart.setEnabled(!isCustom);
		mCheckBoxLogarithmic.setEnabled(!isCustom && !isInteractive());
		mTextFieldCustomValues.setEnabled(isCustom);
		mTextFieldCustomNames.setEnabled(isCustom);
		validateCustomValueField();
		validateCustomNameField();
		}

	@Override
	public String getHelpURL() {
		return "/html/help/data.html#Binning";
		}

	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
			if (isInteractive()) {
				setSlidersToDefault();
				updateLogCheckBox();
				updatePreview();
				}
			}
		}

	private void setSlidersToDefault() {
		mSliderBinSize.setValue(mSliderBinSizeValue = 128);
		updateFieldBinSizeFromSlider();
		resetShiftSlider();
		}

	private void updateLogCheckBox() {
		int selectedColumn = getSelectedColumn();
		mCheckBoxLogarithmic.setSelected(selectedColumn != -1 && mTableModel.isLogarithmicViewMode(selectedColumn));
		}

	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == mSliderBinSize
		 && mSliderBinSizeValue != mSliderBinSize.getValue()) {
			mSliderBinSizeValue = mSliderBinSize.getValue();

			updateFieldBinSizeFromSlider();
			mTextFieldBinStart.setEnabled(true);
			mSliderBinShift.setEnabled(true);
			resetShiftSlider();
			updatePreview();
			}

		if (e.getSource() == mSliderBinShift
		 && mSliderBinShiftValue != mSliderBinShift.getValue()) {
			mSliderBinShiftValue = mSliderBinShift.getValue();

			updateFieldBinShiftFromSlider();
			updatePreview();
			}
		}

	public void keyPressed(KeyEvent e) {}
	public void keyTyped(KeyEvent e) {}

	public void keyReleased(KeyEvent e) {
		if (e.getSource() == mTextFieldBinSize) {
			validateBinSizeField();
			return;
			}
		if (e.getSource() == mTextFieldBinStart) {
			validateBinShiftField();
			return;
			}
		if (e.getSource() == mTextFieldCustomValues) {
			validateCustomValueField();
			updatePreview();
			return;
			}
		if (e.getSource() == mTextFieldCustomNames) {
			validateCustomNameField();
			updatePreview();
			return;
			}
		}

	private int getSelectedColumn() {
		return mTableModel.findColumn((String)mComboBoxColumn.getSelectedItem());
		}

	private boolean columnQualifies(int column) {
		return mTableModel.isColumnTypeDouble(column)
			&& mTableModel.getMaximumValue(column) - mTableModel.getMinimumValue(column) > 0.0;
		}

	private void resetShiftSlider() {
		if (isInteractive()) {
			int selectedColumn = getSelectedColumn();
			double binSize = parseBinSizeField();
			double binStart = binSize * Math.floor(mTableModel.getMinimumValue(selectedColumn) / binSize);
			double min = mTableModel.getMinimumValue(selectedColumn);
			mSliderBinShiftValue = (int)(256.0 * (1.0 + (binStart-min)/binSize));
			mSliderBinShift.setValue(mSliderBinShiftValue);
			updateFieldBinShiftFromSlider();
			}
		}

	private void updateFieldBinSizeFromSlider() {
		int selectedColumn = getSelectedColumn();
		double range = mTableModel.getMaximumValue(selectedColumn)
					 - mTableModel.getMinimumValue(selectedColumn);
		double value = mSliderBinSize.getValue();
		double min = range * Math.pow(cMaxNoOfBins, (value-1.0)/256.0 - 1.0);
		double max = range * Math.pow(cMaxNoOfBins, value/256.0 - 1.0);
		String text = mTableModel.isColumnTypeDate(selectedColumn) ?
				Math.round((max+min)/2)+DAYS : getRoundedValue(min, max);
		mTextFieldBinSize.setText(text);
		}

	private void updateFieldBinShiftFromSlider() {
		int selectedColumn = getSelectedColumn();
		double binSize = parseBinSizeField();
		double value = mSliderBinShift.getValue();
		double min = binSize * ((value-1.0)/256.0 - 1.0);
		double max = binSize * (value/256.0 - 1.0);
		double minValue = mTableModel.getMinimumValue(selectedColumn);
		String text = mTableModel.isColumnTypeDate(selectedColumn) ?
				  DateFormat.getDateInstance().format(new Date((long)(86400000*(minValue+(min+max)/2))))
				: getRoundedValue(minValue+min, minValue+max);
		mTextFieldBinStart.setText(text);
		}

	private double parseBinStartField() {
		String text = mTextFieldBinStart.getText();
		try {
			return Double.parseDouble(text);
			}
		catch (NumberFormatException nfe) {
			try {
				Date date = DateFormat.getDateInstance().parse(text);
				return (double)(date.getTime() / 86400000);
				}
			catch (ParseException pe) {
				return Double.NaN;
				}
			}
		}

	private double parseBinSizeField() {
		String text = mTextFieldBinSize.getText();
		if (text.endsWith(DAYS))
			text = text.substring(0, text.length()-DAYS.length());
		return Double.parseDouble(text);
		}

	private void validateBinSizeField() {
		try {
			double binSize = parseBinSizeField();
			if (binSize <= 0)
				throw new NumberFormatException();

			if (isInteractive()) {
				int selectedColumn = getSelectedColumn();
				double range = mTableModel.getMaximumValue(selectedColumn)
							 - mTableModel.getMinimumValue(selectedColumn);
				if (range/binSize > 1024)
					throw new NumberFormatException();

				if (mTableModel.isColumnTypeDate(selectedColumn)
				 && !mTextFieldBinSize.getText().endsWith(DAYS)) {
					mTextFieldBinSize.removeKeyListener(this);
					mTextFieldBinSize.setText(mTextFieldBinSize.getText() + DAYS);
					mTextFieldBinSize.addKeyListener(this);
					}

				mSliderBinSizeValue = (int)(256.0 * (1.0 + Math.log(binSize/range)/Math.log(cMaxNoOfBins)));
				mSliderBinSize.removeChangeListener(this);
				mSliderBinSize.setValue(mSliderBinSizeValue);
				mSliderBinSize.addChangeListener(this);
				mSliderBinShift.setEnabled(true);
				resetShiftSlider();
				updatePreview();
				}

			mTextFieldBinStart.setEnabled(true);
			mTextFieldBinSize.setBackground(UIManager.getColor("TextArea.background"));
			}
		catch (NumberFormatException ex) {
			mTextFieldBinSize.setBackground(Color.red);
			mTextFieldBinStart.setEnabled(false);
			if (isInteractive()) {
				mSliderBinShift.setEnabled(false);
				}
			}
		}

	private void validateBinShiftField() {
		try {
			double binStart = parseBinStartField();
			if (Double.isNaN(binStart))
				throw new NumberFormatException();

			if (isInteractive()) {
				double min = mTableModel.getMinimumValue(getSelectedColumn());
				if (binStart > min)
					throw new NumberFormatException();

				if (mTableModel.isColumnTypeDate(getSelectedColumn())) {
					try {
						// if we can parse without exception we need to change to date format
						long time = (long)Double.parseDouble(mTextFieldBinStart.getText());
						mTextFieldBinStart.removeKeyListener(this);
						mTextFieldBinStart.setText(DateFormat.getDateInstance().format(new Date(86400000*time+43200000)));
						mTextFieldBinStart.addKeyListener(this);
						}
					catch (NumberFormatException nfe) {}	// already in date format
					}

				double binSize = parseBinSizeField();
				if (binStart <= min-binSize) {
					int steps = (int)((min-binStart)/binSize);
					binStart += binSize*steps;
					}
				mSliderBinShiftValue = (int)(256.0 * (1.0 + (binStart-min)/binSize));
				mSliderBinShift.removeChangeListener(this);
				mSliderBinShift.setValue(mSliderBinShiftValue);
				mSliderBinShift.addChangeListener(this);
				updatePreview();
				}

			mTextFieldBinStart.setBackground(UIManager.getColor("TextArea.background"));
			}
		catch (NumberFormatException ex) {
			mTextFieldBinStart.setBackground(Color.red);
			}
		}

	private void validateCustomValueField() {
		if (!mCheckBoxUseCustomBins.isSelected()) {
			mTextFieldCustomValues.setBackground(UIManager.getColor("TextArea.background"));
			return;
			}

		String[] values = mTextFieldCustomValues.getText().split(",");
		mCustomValue = new double[values.length];
		for (int i=0; i<values.length; i++) {
			try {
				mCustomValue[i] = Double.parseDouble(values[i].trim());
				if (i != 0 && mCustomValue[i] <= mCustomValue[i-1]) {
					mCustomValue = null;
					break;
					}
				}
			catch (NumberFormatException nfe) {
				mCustomValue = null;
				break;
				}
			}

		if (mCustomValue == null)
			mTextFieldCustomValues.setBackground(Color.red);
		else
			mTextFieldCustomValues.setBackground(UIManager.getColor("TextArea.background"));
		}

	private void validateCustomNameField() {
		if (!mCheckBoxUseCustomBins.isSelected()) {
			mTextFieldCustomNames.setBackground(UIManager.getColor("TextArea.background"));
			return;
			}

		boolean errorFound = false;
		mCustomName = null;
		String values = mTextFieldCustomNames.getText().trim();
		if (values.length() != 0) {
			mCustomName = values.split(",");
			if (mCustomName.length != mCustomValue.length + 1) {
				errorFound = true;
				}
			else {
				for (int i=0; i<mCustomName.length; i++) {
					if (mCustomName[i].trim().length() == 0) {
						errorFound = true;
						break;
						}
					}
				}
			}

		if (errorFound)
			mTextFieldCustomNames.setBackground(Color.red);
		else
			mTextFieldCustomNames.setBackground(UIManager.getColor("TextArea.background"));
		}

	private void updatePreview() {
		if (mCheckBoxUseCustomBins.isSelected())
			mPreview.update(getSelectedColumn(),
					mCustomValue,
					mCustomName,
					isInteractive() && mTableModel.isLogarithmicViewMode(getSelectedColumn()),
					mTableModel.isColumnTypeDate(getSelectedColumn()));
		else
			mPreview.update(getSelectedColumn(),
				parseBinSizeField(),
				parseBinStartField(),
				mCheckBoxLogarithmic.isSelected(),
				mTableModel.isColumnTypeDate(getSelectedColumn()));
		}

	private String getRoundedValue(double min, double max) {
		if (max == min)
			return "0.0";

		double dif = max - min;
		int exponent = 0;
		while (dif < 10.0) {
			dif *= 10.0;
			min *= 10.0;
			max *= 10.0;
			exponent--;
			}
		while (dif >= 100.0) {
			dif *= 0.1;
			min *= 0.1;
			max *= 0.1;
			exponent++;
			}

		int intval = (int)((min + max) / 2);
		while (true) {
			int candidate = intval / 10 * 10;
			if ((double)candidate > min && (double)candidate < max) {
				intval = candidate / 10;
				min *= 0.1;
				max *= 0.1;
				exponent++;
				continue;
				}
			candidate += 10;
			if ((double)candidate > min && (double)candidate < max) {
				intval = candidate / 10;
				min *= 0.1;
				max *= 0.1;
				exponent++;
				continue;
				}

			candidate -= 5;
			if ((double)candidate > min && (double)candidate < max) {
				intval = candidate;
				break;
				}

			if (intval % 2 == 1) {
				if ((double)candidate > min && (double)candidate < max)
					intval --;
				else if ((double)candidate > min && (double)candidate < max)
					intval ++;
				}
			break;
			}

		return DoubleFormat.toShortString(intval, exponent);
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.put(PROPERTY_COLUMN, mTableModel.getColumnTitleNoAlias((String)mComboBoxColumn.getSelectedItem()));
		configuration.put(PROPERTY_BIN_START, Double.toString(parseBinStartField()));
		configuration.put(PROPERTY_BIN_SIZE, Double.toString(parseBinSizeField()));
		configuration.put(PROPERTY_LOGARITHMIC, mCheckBoxLogarithmic.isSelected() ? "true" : "false");
		if (mCheckBoxUseCustomBins.isSelected()) {
			configuration.setProperty(PROPERTY_CUSTOM_VALUES, mTextFieldCustomValues.getText());
			String customNames = mTextFieldCustomNames.getText().trim();
			if (customNames.length() != 0)
				configuration.setProperty(PROPERTY_CUSTOM_NAMES, customNames);
			}
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String columnName = configuration.getProperty(PROPERTY_COLUMN, "");
		int column = mTableModel.findColumn(columnName);
		if (column == -1 && isInteractive()) {
			setDialogConfigurationToDefault();
			}
		else {
			if (isInteractive())
				mComboBoxColumn.setSelectedItem(mTableModel.getColumnTitleExtended(column));
			else
				mComboBoxColumn.setSelectedItem(columnName);
	
			String binSize = configuration.getProperty(PROPERTY_BIN_SIZE, "");
			if (binSize.length() != 0) {
				mTextFieldBinSize.setText(binSize);
				validateBinSizeField();
	
				String binStart = configuration.getProperty(PROPERTY_BIN_START, "");
				if (binStart.length() != 0) {
					mTextFieldBinStart.setText(binStart);
					validateBinShiftField();
					}
				else {
					resetShiftSlider();
					}
				}
			}
		boolean isLogarithmic = "true".equals(configuration.getProperty(PROPERTY_LOGARITHMIC));
		if (isInteractive()) {
			mCheckBoxLogarithmic.setSelected((column == -1) ? isLogarithmic : mTableModel.isLogarithmicViewMode(column));
			}
		else {
			mCheckBoxLogarithmic.setSelected(isLogarithmic);
			}

		String customValues = configuration.getProperty(PROPERTY_CUSTOM_VALUES, "");
		if (customValues.length() != 0) {
			mTextFieldCustomValues.setText(customValues);
			mTextFieldCustomNames.setText(configuration.getProperty(PROPERTY_CUSTOM_NAMES, ""));
			mCheckBoxUseCustomBins.setSelected(true);
			}

		enableItems();

		if (isInteractive())
			updatePreview();
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mComboBoxColumn.getItemCount() != 0) {
			mComboBoxColumn.setSelectedIndex(0);
			}
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String customValues = configuration.getProperty(PROPERTY_CUSTOM_VALUES, "");
		double binSize = 0;
		double previousNumValue = -Double.MAX_VALUE;
		if (customValues.length() != 0) {
			String[] customValue = customValues.split(",");
			for (String value:customValue) {
				try {
					double numValue = Double.parseDouble(value);
					if (numValue <= previousNumValue) {
						showErrorMessage("Custom bin threshold values must be defined in increasing order.");
						return false;
						}
					previousNumValue = numValue;
					}
				catch (NumberFormatException nfe) {
					showErrorMessage("Value '"+value+"' is not numerical.");
					return false;
					}
				}
			String customNames = configuration.getProperty(PROPERTY_CUSTOM_NAMES, "");
			if (customNames.length() != 0 && customNames.split(",").length != customValue.length+1) {
				showErrorMessage("If custom names are defined, then there must be one more name than custom values.");
				return false;
				}
			}
		else {
			try {
				binSize = Double.parseDouble(configuration.getProperty(PROPERTY_BIN_SIZE));
				Double.parseDouble(configuration.getProperty(PROPERTY_BIN_START));
				}
			catch (NumberFormatException nfe) {
				showErrorMessage("Bin start or size not properly defined.");
				return false;
				}
			}

		if (isLive) {
			int column = mTableModel.findColumn(configuration.getProperty(PROPERTY_COLUMN, ""));
			if (column == -1) {
				showErrorMessage("Column '"+configuration.getProperty(PROPERTY_COLUMN)+"' not found.");
				return false;
				}
			if (!columnQualifies(column)) {
				showErrorMessage("Column '"+configuration.getProperty(PROPERTY_COLUMN)+"' cannot be used for binning.");
				return false;
				}
			if (customValues.length() == 0) {
				if ((mTableModel.getMaximumValue(column) - mTableModel.getMinimumValue(column)) / binSize > 10f*cMaxNoOfBins) {
					showErrorMessage("Maximum number of bins exceeded.");
					return false;
					}
				boolean isLogarithmic = "true".equals(configuration.getProperty(PROPERTY_LOGARITHMIC));
				if (isLogarithmic && !mTableModel.isLogarithmicViewMode(column) && mTableModel.getMinimumValue(column) <= 0.0) {
					showErrorMessage("Binning on logarithic data cannot be done, because some data is <= 0.");
					return false;
					}
				}
			}

		return true;
		}

	public void runTask(Properties configuration) {
		int column = mTableModel.findColumn(configuration.getProperty(PROPERTY_COLUMN));
		boolean isDate = mTableModel.isColumnTypeDate(column);

		String customValues = configuration.getProperty(PROPERTY_CUSTOM_VALUES, "");
		double[] customValue = null;
		String[] customName = null;
		if (customValues.length() != 0) {
			String[] valueString = customValues.split(",");
			customValue = new double[valueString.length];
			for (int i=0; i<valueString.length; i++)
				customValue[i] = Double.parseDouble(valueString[i]);
			String customNames = configuration.getProperty(PROPERTY_CUSTOM_NAMES, "");
			if (customNames.length() != 0) {
				customName = customNames.split(",");
				}
			else {
				customName = new String[customValue.length+1];
				customName[0] = "x < " + customValue[0];
				customName[customValue.length] = customValue[customValue.length-1] + " <= x";
				for (int i=1; i<customValue.length; i++)
					customName[i] = customValue[i-1] + CompoundTableModel.cRangeSeparation + customValue[i];
				}
			}

		String[] columnName = new String[1];
		columnName[0] = "Binned " + mTableModel.getColumnTitleExtended(column);
		int newColumn = mTableModel.addNewColumns(columnName);

		if (customValue != null) {
			for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
				int index = customValue.length;
				double value = mTableModel.getTotalDoubleAt(row, column);
				if (mTableModel.isLogarithmicViewMode(column))
					value = Math.pow(10.0, value);
				for (int i=0; i<customValue.length; i++) {
					if (value < customValue[i]) {
						index = i;
						break;
						}
					}
				mTableModel.setTotalValueAt(customName[index], row, newColumn);
				}
			}
		else {
			String binBase = configuration.getProperty(PROPERTY_BIN_START);
			String binSize = configuration.getProperty(PROPERTY_BIN_SIZE);
			boolean isLog = "true".equals(configuration.getProperty(PROPERTY_LOGARITHMIC));

			mTableModel.setColumnProperty(newColumn, CompoundTableConstants.cColumnPropertyBinSize, binSize);
			mTableModel.setColumnProperty(newColumn, CompoundTableConstants.cColumnPropertyBinBase, binBase);
			mTableModel.setColumnProperty(newColumn, CompoundTableConstants.cColumnPropertyBinIsLog, isLog ? "true" : "false");
			mTableModel.setColumnProperty(newColumn, CompoundTableConstants.cColumnPropertyBinIsDate, isDate ? "true" : "false");

			BinGenerator limits = new BinGenerator(mTableModel, column, new BigDecimal(binBase), new BigDecimal(binSize), isLog, isDate);

			for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
				int index = limits.getIndex(mTableModel.getTotalDoubleAt(row, column));
				mTableModel.setTotalValueAt(limits.getRangeString(index), row, newColumn);
				}
			}

		mTableModel.finalizeNewColumns(newColumn, null);
		if (customValue != null)
			mTableModel.setCategoryCustomOrder(newColumn, customName);
		}
	}
