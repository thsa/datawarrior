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

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Properties;


public class DETaskSetValueRange extends ConfigurableTask implements ActionListener {
	public static final String TASK_NAME = "Set Value Range";

	private static final String PROPERTY_MINIMUM = "min";
	private static final String PROPERTY_MAXIMUM = "max";
	private static final String PROPERTY_COLUMN = "column";

    private CompoundTableModel  	mTableModel;
    private JComboBox				mComboBoxColumn;
	private JTextField           	mTextFieldMin,mTextFieldMax;
	private int						mDefaultColumn;

    public DETaskSetValueRange(DEFrame parent, int defaultColumn) {
		super(parent, false);
		mTableModel = parent.getTableModel();
		mDefaultColumn = defaultColumn;
    	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mComboBoxColumn) {
			int column = mTableModel.findColumn((String)mComboBoxColumn.getSelectedItem());
			if (column != -1) {
				String min = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyDataMin);
				String max = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyDataMax);
				mTextFieldMin.setText(min == null ? "" : millisToDDMMYYYY(min));
				mTextFieldMax.setText(max == null ? "" : millisToDDMMYYYY(max));
				}
			return;
			}
		}

	@Override
	public boolean isConfigurable() {
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.isColumnTypeDouble(column) && !mTableModel.isColumnTypeDate(column))
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
		JPanel mp = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.FILL, gap},
							{gap, TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED, gap/2,
									TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };
		mp.setLayout(new TableLayout(size));

		mp.add(new JLabel("Data column:"), "1,1");
		mp.add(new JLabel("Minimum value:"), "1,3");
		mp.add(new JLabel("Maximum value:"), "1,5");
		mp.add(new JLabel("(use 'ddmmyyyy' for date values)"), "1,7,5,7");

		mComboBoxColumn = new JComboBox();
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.isColumnTypeDouble(column))
				mComboBoxColumn.addItem(mTableModel.getColumnTitle(column));
		if (mDefaultColumn == -1) {
			mComboBoxColumn.setEditable(true);
			}
		else {
			mComboBoxColumn.setEnabled(false);
			}
		mComboBoxColumn.addActionListener(this);
		mp.add(mComboBoxColumn, "3,1,5,1");

		mTextFieldMin = new JTextField(6);
		mTextFieldMax = new JTextField(6);
		mp.add(mTextFieldMin, "3,3");
		mp.add(mTextFieldMax, "3,5");

		return mp;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.put(PROPERTY_COLUMN, mTableModel.getColumnTitleNoAlias((String)mComboBoxColumn.getSelectedItem()));
		if (mTextFieldMin.getText().length() != 0)
			configuration.put(PROPERTY_MINIMUM, mTextFieldMin.getText());
		if (mTextFieldMax.getText().length() != 0)
			configuration.put(PROPERTY_MAXIMUM, mTextFieldMax.getText());
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		if (mDefaultColumn != -1) {
			mComboBoxColumn.setSelectedItem(mTableModel.getColumnTitle(mDefaultColumn));
			// min and max fields are updated via actionListener
			}
		else {
			String columnName = configuration.getProperty(PROPERTY_COLUMN);
			int column = mTableModel.findColumn(columnName);
			if (column != -1)
				columnName = mTableModel.getColumnTitle(column);
			mComboBoxColumn.setSelectedItem(columnName);
			mTextFieldMin.setText(configuration.getProperty(PROPERTY_MINIMUM, ""));
			mTextFieldMax.setText(configuration.getProperty(PROPERTY_MAXIMUM, ""));
			}
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mDefaultColumn != -1) {
			mComboBoxColumn.setSelectedItem(mTableModel.getColumnTitle(mDefaultColumn));
			// min and max fields are updated via actionListener
			}
		else {
			if (mComboBoxColumn.getItemCount() != 0)
				mComboBoxColumn.setSelectedIndex(0);
			}
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
    	int column = -1;
		if (isLive) {
			column = mTableModel.findColumn(configuration.getProperty(PROPERTY_COLUMN, ""));
			if (column == -1) {
				showErrorMessage("Column '"+configuration.getProperty(PROPERTY_COLUMN)+"' not found.");
		        return false;
				}
			}

	    String minString = configuration.getProperty(PROPERTY_MINIMUM);
	    String maxString = configuration.getProperty(PROPERTY_MAXIMUM);
	    float min = Float.NaN;
	    float max = Float.NaN;

	    if (isLive && mTableModel.isColumnTypeDate(column)) {
	    	if (minString != null && interpret8DigitDate(minString) == null) {
			    showErrorMessage("Invalid minimum date.");
			    return false;
			    }
		    if (maxString != null && interpret8DigitDate(maxString) == null) {
			    showErrorMessage("Invalid maximum date.");
			    return false;
			    }
	        }
	    else {
		    if (minString != null) {
			    try {
			        min = Float.parseFloat(minString);
			        }
			    catch (NumberFormatException nfe) {
					showErrorMessage("Invalid minimum value.");
			        return false;
			        }
		        }
		    if (maxString != null) {
			    try {
			        max = Float.parseFloat(maxString);
			        }
			    catch (NumberFormatException nfe) {
					showErrorMessage("Invalid maximum value.");
			        return false;
			        }
		        }
		    }

	    if (minString != null && maxString != null && (min >= max)) {
			showErrorMessage("Minimum value is not smaller than maximum.");
	        return false;
	    	}
		return true;
		}

	private String millisToDDMMYYYY(String millisString) {
    	long millis = 0;
    	try { millis = Long.parseLong(millisString); } catch (NumberFormatException nfe) {}
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(millis);
		StringBuilder date = new StringBuilder();
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		int month = calendar.get(Calendar.MONTH)+1;
		int year = calendar.get(Calendar.YEAR);
		if (day < 10)
			date.append("0");
		date.append(day);
		if (month < 10)
			date.append("0");
		date.append(month);
		if (year < 10)
			date.append("000");
		else if (year < 100)
			date.append("00");
		else if (year < 1000)
			date.append("0");
		date.append(year);
		return date.toString();
		}

	private Long interpret8DigitDate(String dateString) {
    	if (dateString != null && dateString.length() == 8) {
	        try {
		        int day = Integer.parseInt(dateString.substring(0, 2));
		        int month = Integer.parseInt(dateString.substring(2, 4));
		        int year = Integer.parseInt(dateString.substring(4));
		        if (day >= 1 && day <= 31 && month >= 1 && month <= 12 && year >= 0) {
			        Calendar calendar = Calendar.getInstance();
			        calendar.clear();
			        calendar.set(year, month-1, day, 12, 0, 0);
			        return calendar.getTimeInMillis();
			        }
	            }
	        catch (NumberFormatException nfe) {}
		    }

	    return null;
		}

	@Override
	public void runTask(Properties configuration) {
		int column = mTableModel.findColumn(configuration.getProperty(PROPERTY_COLUMN, ""));
	    String minString = configuration.getProperty(PROPERTY_MINIMUM);
	    String maxString = configuration.getProperty(PROPERTY_MAXIMUM);
	    if (mTableModel.isColumnTypeDate(column))
		    mTableModel.setValueRange(column, interpret8DigitDate(minString), interpret8DigitDate(maxString));
    	else
			mTableModel.setValueRange(column, minString, maxString);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
