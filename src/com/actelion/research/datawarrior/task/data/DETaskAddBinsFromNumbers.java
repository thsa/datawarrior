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

import info.clearthought.layout.TableLayout;

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

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.table.BinGenerator;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.DoubleFormat;

public class DETaskAddBinsFromNumbers extends ConfigurableTask implements ChangeListener,ItemListener,KeyListener {
	public static final String TASK_NAME = "Add Bins From Numbers";

	private static final String PROPERTY_COLUMN = "column";
	private static final String PROPERTY_BIN_START = "binStart";
	private static final String PROPERTY_BIN_SIZE = "binSize";
	private static final String PROPERTY_LOGARITHMIC = "logarithmic";

	private static final String DAYS = " days";

	private double cMaxNoOfBins = 220.0;

	CompoundTableModel	mTableModel;
	JComboBox			mComboBoxColumn;
	JSlider				mSliderBinSize,mSliderBinShift;
	JTextField			mTextFieldBinSize,mTextFieldBinStart;
	JCheckBox			mCheckBoxLogarithmic;
	BinningPreview		mPreview;
	int					mSliderBinSizeValue,mSliderBinShiftValue;
	double				mCurrentRange;

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
		JPanel content = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.FILL, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, TableLayout.FILL, 8},
							{8, TableLayout.PREFERRED, 8, TableLayout.FILL,
								TableLayout.PREFERRED, 8, TableLayout.PREFERRED, TableLayout.FILL,
								TableLayout.PREFERRED, 8, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.FILL} };
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
		content.add(cp, "1,1,7,1");

		mTextFieldBinSize = new JTextField(8);
		mTextFieldBinSize.addKeyListener(this);
		mTextFieldBinStart = new JTextField(8);
		mTextFieldBinStart.addKeyListener(this);
		content.add(new JLabel("Bin Size:", SwingConstants.RIGHT), "4,4");
		content.add(mTextFieldBinSize, "6,4");
		content.add(new JLabel("First Bin From:", SwingConstants.RIGHT), "4,8");
		content.add(mTextFieldBinStart, "6,8");
		mSliderBinSize = new JSlider(JSlider.HORIZONTAL, 0, 256, 128);
		mSliderBinShift = new JSlider(JSlider.HORIZONTAL, 0, 256, 128);
		content.add(mSliderBinSize, "3,6,7,6");
		content.add(mSliderBinShift, "3,10,7,10");
		mCheckBoxLogarithmic = new JCheckBox("Values are logarithms");
		mCheckBoxLogarithmic.setEnabled(!isInteractive());
		content.add(mCheckBoxLogarithmic, "3,11,7,11");

		content.add(mPreview = new BinningPreview(mTableModel), "1,3,1,12");

		if (isInteractive()) {
			mSliderBinSize.addChangeListener(this);
			mSliderBinShift.addChangeListener(this);
			}
		else {
			mSliderBinSize.setEnabled(false);
			mSliderBinShift.setEnabled(false);
			}

		return content;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/analysis.html#Binning";
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
		double value = (double)(mSliderBinSize.getValue());
		double min = range * Math.pow(cMaxNoOfBins, (value-1.0)/256.0 - 1.0);
		double max = range * Math.pow(cMaxNoOfBins, value/256.0 - 1.0);
		String text = mTableModel.isColumnTypeDate(selectedColumn) ?
				Math.round((max+min)/2)+DAYS : getRoundedValue(min, max);
		mTextFieldBinSize.setText(text);
		}

	private void updateFieldBinShiftFromSlider() {
		int selectedColumn = getSelectedColumn();
		double binSize = parseBinSizeField();
		double value = (double)mSliderBinShift.getValue();
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

	private void updatePreview() {
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

				if (isInteractive())
					updatePreview();
				}
			}
		boolean isLogarithmic = "true".equals(configuration.getProperty(PROPERTY_LOGARITHMIC));
		if (isInteractive()) {
			mCheckBoxLogarithmic.setSelected((column == -1) ? isLogarithmic : mTableModel.isLogarithmicViewMode(column));
			}
		else {
			mCheckBoxLogarithmic.setSelected(isLogarithmic);
			}
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mComboBoxColumn.getItemCount() != 0) {
			mComboBoxColumn.setSelectedIndex(0);
			}
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		double binSize;
		try {
			binSize = Double.parseDouble(configuration.getProperty(PROPERTY_BIN_SIZE));
			Double.parseDouble(configuration.getProperty(PROPERTY_BIN_START));
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("Bin start or size not properly defined.");
			return false;
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

		return true;
		}

	public void runTask(Properties configuration) {
		int column = mTableModel.findColumn(configuration.getProperty(PROPERTY_COLUMN));
		boolean isDate = mTableModel.isColumnTypeDate(column);

		String binBase = configuration.getProperty(PROPERTY_BIN_START);
		String binSize = configuration.getProperty(PROPERTY_BIN_SIZE);
		boolean isLog = "true".equals(configuration.getProperty(PROPERTY_LOGARITHMIC));

		BinGenerator limits = new BinGenerator(mTableModel, column, new BigDecimal(binBase), new BigDecimal(binSize), isLog, isDate);

		String[] columnName = new String[1];
		columnName[0] = "Binned " + mTableModel.getColumnTitleExtended(column);
		int newColumn = mTableModel.addNewColumns(columnName);
		mTableModel.setColumnProperty(newColumn, CompoundTableConstants.cColumnPropertyBinBase, binBase);
		mTableModel.setColumnProperty(newColumn, CompoundTableConstants.cColumnPropertyBinSize, binSize);
		mTableModel.setColumnProperty(newColumn, CompoundTableConstants.cColumnPropertyBinIsLog, isLog ? "true" : "false");
		mTableModel.setColumnProperty(newColumn, CompoundTableConstants.cColumnPropertyBinIsDate, isDate ? "true" : "false");

		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			int index = limits.getIndex(mTableModel.getTotalDoubleAt(row, column));
			mTableModel.setTotalValueAt(limits.getRangeString(index), row, newColumn);
			}

		mTableModel.finalizeNewColumns(newColumn, null);
		}
	}
