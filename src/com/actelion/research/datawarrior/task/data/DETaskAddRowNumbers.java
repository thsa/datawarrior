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
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;


public class DETaskAddRowNumbers extends ConfigurableTask implements ActionListener {
	public static final String TASK_NAME = "Add Row Numbers";

	private static final String PROPERTY_COLUMN_NAME = "columnName";
	private static final String PROPERTY_FIRST_NUMBER = "firstNumber";
	private static final String PROPERTY_COUNT_MODE = "randomOrder";    // was binary choice before (increase and random)
	private static final String PROPERTY_VISIBLE_ONLY = "visibleOnly";
	private static final String PROPERTY_CATEGORY = "category";
	private static final String PROPERTY_CATEGORY_MODE = "categoryMode";
	private static final String PROPERTY_SHARED_ROW_COUNT = "shareRowCount";

	private static final String CATEGORY_MODE_INDEPENDENT = "independent";
	private static final String CATEGORY_MODE_SAME = "same";

	private static final String[] COUNT_MODE_CODE = { "false", "true", "decrease" };
	private static final String[] COUNT_MODE_TEXT = { "Increasing Numbers", "Random order", "Descreasing Numbers" };
	private static final int COUNT_MODE_INCREASING = 0;
	private static final int COUNT_MODE_RANDOM = 1;
	private static final int COUNT_MODE_DECREASING = 2;

	private DEFrame				mSourceFrame;
    private JTextField          mTextFieldColumnName,mTextFieldFirstNo,mTextFieldSharedRowCount;
    private JCheckBox           mCheckBoxVisibleOnly,mCheckBoxSharedRowNumbers,mCheckBoxUseSameForSame,mCheckBoxCountWithinCategory;
    private JComboBox           mComboBoxCountMode,mComboBoxCategory;

	public DETaskAddRowNumbers(DEFrame sourceFrame) {
		super(sourceFrame, true);
		mSourceFrame = sourceFrame;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isConfigurable() {
		if (mSourceFrame.getTableModel().getTotalColumnCount() == 0) {
			showErrorMessage("Cannot add row numbers to an empty table. Open file, paste data, or create a new table first.");
			return false;
			}
		return true;
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
    public JPanel createDialogContent() {
        mTextFieldColumnName = new JTextField("Row No", 12);
        mTextFieldFirstNo = new JTextField("1", 3);
		mComboBoxCountMode = new JComboBox(COUNT_MODE_TEXT);
        mCheckBoxVisibleOnly = new JCheckBox("Visible rows only");
		mCheckBoxSharedRowNumbers = new JCheckBox("Share same row number for");
		mCheckBoxSharedRowNumbers.addActionListener(this);
        mCheckBoxUseSameForSame = new JCheckBox("Use same number within same category");
        mCheckBoxUseSameForSame.addActionListener(this);
		mTextFieldSharedRowCount = new JTextField(3);
        mComboBoxCategory = new JComboBox();
        mComboBoxCategory.setEnabled(false);
        mComboBoxCategory.setEditable(!isInteractive());
		mCheckBoxCountWithinCategory = new JCheckBox("Use independent row numbers in each category");
		mCheckBoxCountWithinCategory.addActionListener(this);
        CompoundTableModel tableModel = mSourceFrame.getTableModel();
        for (int column=0; column<tableModel.getTotalColumnCount(); column++)
            if (tableModel.isColumnTypeCategory(column))
                mComboBoxCategory.addItem(tableModel.getColumnTitle(column));
        if (isInteractive() && mComboBoxCategory.getItemCount() == 0)
        	mCheckBoxUseSameForSame.setEnabled(false);

        JPanel gp = new JPanel();
        int gap = HiDPIHelper.scale(8);
        double[][] size = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, TableLayout.FILL, gap},
                            {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, TableLayout.PREFERRED, gap*2,
							 TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap*2} };
        gp.setLayout(new TableLayout(size));
        gp.add(new JLabel("Title of new column:", JLabel.RIGHT), "1,1");
        gp.add(new JLabel("First number to use:", JLabel.RIGHT), "1,3");
        gp.add(mTextFieldColumnName, "3,1,6,1");
        gp.add(mTextFieldFirstNo, "3,3");
		gp.add(new JLabel("Count mode:", JLabel.RIGHT), "1,5");
		gp.add(mComboBoxCountMode, "3,5,6,5");
        gp.add(mCheckBoxVisibleOnly, "1,6");
        gp.add(mCheckBoxSharedRowNumbers, "1,8");
		gp.add(mTextFieldSharedRowCount, "3,8");
		gp.add(new JLabel("consecutive rows"), "5,8");
        gp.add(mCheckBoxUseSameForSame, "1,9,6,9");
		gp.add(mCheckBoxCountWithinCategory, "1,10,6,10");
		gp.add(new JLabel("Category:", JLabel.RIGHT), "1,12");
		gp.add(mComboBoxCategory, "3,12,6,12");

        return gp;
		}

	@Override
    public Properties getDialogConfiguration() {
    	Properties configuration = new Properties();

    	String value = mTextFieldColumnName.getText();
    	if (value.length() != 0)
    		configuration.setProperty(PROPERTY_COLUMN_NAME, value);

    	value = mTextFieldFirstNo.getText();
    	if (value.length() != 0) {
	    	try {
	    		configuration.setProperty(PROPERTY_FIRST_NUMBER, ""+Integer.parseInt(value));
	    		}
	    	catch (NumberFormatException nfe) {}
    		}

   		configuration.setProperty(PROPERTY_COUNT_MODE, COUNT_MODE_CODE[mComboBoxCountMode.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_VISIBLE_ONLY, mCheckBoxVisibleOnly.isSelected() ? "true" : "false");

		if (mCheckBoxSharedRowNumbers.isSelected())
			configuration.setProperty(PROPERTY_SHARED_ROW_COUNT, mTextFieldSharedRowCount.getText());

   		if (mCheckBoxUseSameForSame.isSelected()
		 || mCheckBoxCountWithinCategory.isSelected()) {
			configuration.setProperty(PROPERTY_CATEGORY_MODE, mCheckBoxUseSameForSame.isSelected() ?
										CATEGORY_MODE_SAME : CATEGORY_MODE_INDEPENDENT);
			configuration.setProperty(PROPERTY_CATEGORY, (String) mComboBoxCategory.getSelectedItem());
			}

		return configuration;
		}

	@Override
    public void setDialogConfiguration(Properties configuration) {
		String value = configuration.getProperty(PROPERTY_COLUMN_NAME);
		mTextFieldColumnName.setText(value == null ? "Record No" : value);

		value = configuration.getProperty(PROPERTY_FIRST_NUMBER);
		mTextFieldFirstNo.setText(value == null ? "1" : value);

		mComboBoxCountMode.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_COUNT_MODE), COUNT_MODE_CODE, 0));
		mCheckBoxVisibleOnly.setSelected("true".equals(configuration.getProperty(PROPERTY_VISIBLE_ONLY)));

		value = configuration.getProperty(PROPERTY_SHARED_ROW_COUNT);
		if (value != null) {
			mCheckBoxSharedRowNumbers.setSelected(true);
			mTextFieldSharedRowCount.setText(value);
			}

		value = configuration.getProperty(PROPERTY_CATEGORY);
		if (value != null) {
			mComboBoxCategory.setSelectedItem(value);
			if (CATEGORY_MODE_INDEPENDENT.equals(configuration.getProperty(PROPERTY_CATEGORY_MODE))) {
				mCheckBoxUseSameForSame.setSelected(false);
				mCheckBoxCountWithinCategory.setSelected(true);
				}
			else {
				mCheckBoxUseSameForSame.setSelected(true);
				mCheckBoxCountWithinCategory.setSelected(false);
				}
			}
		else {
			mCheckBoxUseSameForSame.setSelected(false);
			mCheckBoxCountWithinCategory.setSelected(false);
			}

		enableItems();
		}

	private void enableItems() {
		mTextFieldSharedRowCount.setEnabled(mCheckBoxSharedRowNumbers.isSelected());
		mComboBoxCategory.setEnabled(mCheckBoxUseSameForSame.isSelected() || mCheckBoxCountWithinCategory.isSelected());
		}

	@Override
    public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String value = configuration.getProperty(PROPERTY_SHARED_ROW_COUNT);
		if (value != null) {
			try {
				int sharedRowCount = Integer.parseInt(value);
				if (sharedRowCount <= 1) {
					showErrorMessage("The row count value must be large than one.");
					return false;
					}
				}
			catch (NumberFormatException nfe) {
				showErrorMessage("The row count value is not numerical.");
				return false;
				}
			}

		if (isLive) {
			String category = configuration.getProperty(PROPERTY_CATEGORY);
			if (category != null) {
				int column = mSourceFrame.getTableModel().findColumn(category);
				if (column == -1) {
					showErrorMessage("Category column '"+category+"' was not found.");
					return false;
					}
				if (!mSourceFrame.getTableModel().isColumnTypeCategory(column)) {
					showErrorMessage("Column '"+category+"' does not contain categories.");
					return false;
					}		
				}
			}

		return true;
		}

	@Override
    public void setDialogConfigurationToDefault() {
		mTextFieldColumnName.setText("Row No");
		mTextFieldFirstNo.setText("1");
		mComboBoxCountMode.setSelectedIndex(0);
		mCheckBoxVisibleOnly.setSelected(false);
		mCheckBoxSharedRowNumbers.setSelected(false);
		mTextFieldSharedRowCount.setText("2");
		mCheckBoxUseSameForSame.setSelected(false);
		mCheckBoxCountWithinCategory.setSelected(false);
		mTextFieldSharedRowCount.setEnabled(false);
        mComboBoxCategory.setEnabled(false);
		}

	@Override
    public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mCheckBoxSharedRowNumbers) {
			if (mCheckBoxSharedRowNumbers.isSelected()) {
				mCheckBoxCountWithinCategory.setSelected(false);
				mCheckBoxUseSameForSame.setSelected(false);
				}
			enableItems();
			return;
			}
        if (e.getSource() == mCheckBoxUseSameForSame) {
			if (mCheckBoxUseSameForSame.isSelected()) {
				mCheckBoxSharedRowNumbers.setSelected(false);
				mCheckBoxCountWithinCategory.setSelected(false);
				}
			enableItems();
            return;
            }
		if (e.getSource() == mCheckBoxCountWithinCategory) {
			if (mCheckBoxCountWithinCategory.isSelected()) {
				mCheckBoxSharedRowNumbers.setSelected(false);
				mCheckBoxUseSameForSame.setSelected(false);
				}
			enableItems();
			return;
			}
		}

	@Override
	public void runTask(Properties configuration) {
        CompoundTableModel tableModel = mSourceFrame.getTableModel();

        String[] columnName = new String[1];
		columnName[0] = configuration.getProperty(PROPERTY_COLUMN_NAME, "Row No");

		int recordNoColumn = tableModel.addNewColumns(columnName);
        int categoryColumn = tableModel.findColumn(configuration.getProperty(PROPERTY_CATEGORY));
        int shareRowCount = Integer.parseInt(configuration.getProperty(PROPERTY_SHARED_ROW_COUNT, "1"));

		int countMode = findListIndex(configuration.getProperty(PROPERTY_COUNT_MODE), COUNT_MODE_CODE, 0);
        boolean visibleOnly = "true".equals(configuration.getProperty(PROPERTY_VISIBLE_ONLY));

		boolean sameInCategory = false;
		boolean independentCategories = false;
		if (categoryColumn != -1) {
			if (CATEGORY_MODE_INDEPENDENT.equals(configuration.getProperty(PROPERTY_CATEGORY_MODE)))
				independentCategories = true;
			else
				sameInCategory = true;
			}

        int rowCount = visibleOnly ? tableModel.getRowCount() : tableModel.getTotalRowCount();
		String value = configuration.getProperty(PROPERTY_FIRST_NUMBER);
		int firstNo = (value == null) ? 1 : Integer.parseInt(value);

		int[] randomMapOrCount = null;
		TreeMap<String,int[]> categoryRandomOrCountMap = null;
		if (countMode != COUNT_MODE_INCREASING) {
			if (categoryColumn != -1) {
				if (sameInCategory) {
					TreeSet<String> set = new TreeSet<String>();
					for (int row=0; row<rowCount; row++) {
						CompoundRecord record = visibleOnly ? tableModel.getRecord(row) : tableModel.getTotalRecord(row);
						String[] entries = mSourceFrame.getTableModel().separateEntries(tableModel.encodeData(record, categoryColumn));
						for (String entry:entries)
							set.add(entry);
						}
					if (countMode == COUNT_MODE_RANDOM) {
						randomMapOrCount = generateRandomMap(set.size(), 1);
						}
					else {   // COUNT_MODE_DECREASING
						randomMapOrCount = new int[1];
						randomMapOrCount[0] = set.size();
						}
					}
				else {
					categoryRandomOrCountMap = new TreeMap<>();
					for (int row=0; row<rowCount; row++) {
						CompoundRecord record = visibleOnly ? tableModel.getRecord(row) : tableModel.getTotalRecord(row);
						String[] entries = mSourceFrame.getTableModel().separateEntries(tableModel.encodeData(record, categoryColumn));
						for (String entry:entries) {
							int[] count = categoryRandomOrCountMap.get(entry);
							if (count == null) {
								count = new int[1];
								count[0] = 1;
								categoryRandomOrCountMap.put(entry, count);
								}
							else {
								count[0]++;
								}
							}
						}
					if (countMode == COUNT_MODE_RANDOM) {
						for (String category:categoryRandomOrCountMap.keySet()) {
							int[] count = categoryRandomOrCountMap.get(category);
							categoryRandomOrCountMap.put(category, generateRandomMap(count[0], 1));
							}
						}
					}
				}
			else {
				if (countMode == COUNT_MODE_RANDOM) {
					randomMapOrCount = generateRandomMap(rowCount, shareRowCount);
					}
				else {
					randomMapOrCount = new int[1];
					randomMapOrCount[0] = rowCount;
					}
				}
			}

		StringBuilder sb = new StringBuilder();
		TreeMap<String,Integer> map = (categoryColumn == -1) ? null : new TreeMap<>();
		for (int row=0; row<rowCount; row++) {
            CompoundRecord record = visibleOnly ? tableModel.getRecord(row) : tableModel.getTotalRecord(row);
			String data = null;

			if (categoryColumn != -1) {
				String[] entries = mSourceFrame.getTableModel().separateEntries(tableModel.encodeData(record, categoryColumn));
				sb.setLength(0);
				for (String entry:entries) {
					Integer index = map.get(entry);
					if (sameInCategory) {
						if (index == null) {
							index = Integer.valueOf(map.size());
							map.put(entry, index);
							}
						}
					else {
						if (index == null)
							index = Integer.valueOf(0);
						else
							index = Integer.valueOf(index+1);
						map.put(entry, index);
						}

					if (sb.length() != 0)
						sb.append(CompoundTableModel.cEntrySeparator);
					if (countMode == COUNT_MODE_RANDOM)
						index = Integer.valueOf(sameInCategory ? randomMapOrCount[index] : categoryRandomOrCountMap.get(entry)[index]);
					else if (countMode == COUNT_MODE_DECREASING)
						index = Integer.valueOf(sameInCategory ? randomMapOrCount[0]-index-1 : categoryRandomOrCountMap.get(entry)[0]-index-1);

					sb.append(firstNo+index);
					}
				data = sb.toString();
				}
			else {
				int index = countMode == COUNT_MODE_RANDOM ? randomMapOrCount[row]
						  : countMode == COUNT_MODE_DECREASING ? randomMapOrCount[0]-row-1
						  : (shareRowCount != 1) ? row / shareRowCount : row;
				data = Integer.toString(firstNo+index);
				}

            record.setData(tableModel.decodeData(data, recordNoColumn), recordNoColumn);
            }

        tableModel.finalizeNewColumns(recordNoColumn, null);
		}

	private int[] generateRandomMap(int length, int shareRowCount) {
		Random random = new Random();
		long[] la = new long[length];
		for (int i=0; i<length; i++)
			la[i] = (((long)random.nextInt()) << 32) + i;
		Arrays.sort(la);
		int[] ia = new int[length];
		for (int i=0; i<length; i++)
			ia[i] = (shareRowCount != 1) ? (int)la[i] / shareRowCount : (int)la[i];
		return ia;
		}

/*  private String getCategoryNumbers(int firstNo, boolean isSame, TreeMap<String,Integer> map, String categories, StringBuilder sb) {
        String[] entries = mSourceFrame.getTableModel().separateEntries(categories);
        sb.setLength(0);
        for (String entry:entries) {
            Integer index = map.get(entry);
            if (index == null)
                index = map.put(entry, map.size());

            if (sb.length() != 0)
                sb.append(CompoundTableModel.cEntrySeparator);
			sb.append(firstNo+index);
            }
        return sb.toString();
        }*/
	}

