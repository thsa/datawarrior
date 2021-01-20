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

import java.util.Properties;
import java.util.Random;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.table.model.CompoundTableListHandler;
import com.actelion.research.table.model.CompoundTableModel;


public class DETaskSetReferenceRow extends ConfigurableTask implements Runnable {
	public static final long serialVersionUID = 0x20140119;

	public static final String TASK_NAME = "Set Reference Row";

	private static final String PROPERTY_WHICH = "which";
	private static final String PROPERTY_WHERE = "where";

	private static final String OPTION_IN_FILE = "<Entire File>";

//	private static final int WHICH_NEXT = 0;
	private static final int WHICH_RANDOM = 1;

	private static final String[] WHICH_CODE = { "next", "random" };
	private static final String[] WHICH_ITEM = { "the next row", "a random row" };

	private DEFrame				mParentFrame;
	private CompoundTableModel	mTableModel;
	private JComboBox			mComboBoxWhich,mComboBoxWhere;

	public DETaskSetReferenceRow(DEFrame owner) {
		super(owner, false);
		mParentFrame = owner;
		mTableModel = mParentFrame.getTableModel();
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isConfigurable() {
		return true;
		}

	@Override
	public JPanel createDialogContent() {
		mComboBoxWhich = new JComboBox(WHICH_ITEM);

		mComboBoxWhere = new JComboBox();
		mComboBoxWhere.addItem(OPTION_IN_FILE);
		String[] names = mTableModel.getListHandler().getListNames();
		if (names != null)
			for (String name:names)
				mComboBoxWhere.addItem(name);

		mComboBoxWhere.setEditable(true);

		JPanel p1 = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED,
							 4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8} };
		p1.setLayout(new TableLayout(size));

		p1.add(new JLabel("Make"), "1,1");
		p1.add(mComboBoxWhich, "3,1");
		p1.add(new JLabel("in list"), "5,1");
		p1.add(mComboBoxWhere, "7,1");
		p1.add(new JLabel("the reference row"), "9,1");

		return p1;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		configuration.setProperty(PROPERTY_WHICH,
				WHICH_CODE[findListIndex((String)mComboBoxWhich.getSelectedItem(), WHICH_ITEM, 0)]);

		String where = (String)mComboBoxWhere.getSelectedItem();
		if (where.length() != 0 && !where.equals(OPTION_IN_FILE))
			configuration.setProperty(PROPERTY_WHERE, where);

		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mComboBoxWhich.setSelectedItem(WHICH_ITEM[findListIndex(configuration.getProperty(PROPERTY_WHICH), WHICH_CODE, 0)]);

		String where = configuration.getProperty(PROPERTY_WHERE, "");
		if (where.length() == 0)
			mComboBoxWhere.setSelectedItem(OPTION_IN_FILE);
		else
			mComboBoxWhere.setSelectedItem(where);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		// if a defined list doesn't exist, we simply use the entire file and don't produce an error
		return true;
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mComboBoxWhich.setSelectedIndex(0);
		mComboBoxWhere.setSelectedIndex(0);
		}

	@Override
	public void runTask(Properties configuration) {
		int which = findListIndex(configuration.getProperty(PROPERTY_WHICH), WHICH_CODE, 0);
		String listName = configuration.getProperty(PROPERTY_WHERE, "");
		int list = -1;
		if (listName.length() != 0 && !listName.equals(OPTION_IN_FILE))
			list = mTableModel.getListHandler().getListIndex(listName);

		int oldRow = mTableModel.getActiveRowIndex();
		int newRow = -1;

		if (list != -1) {
			CompoundTableListHandler hlh = mTableModel.getListHandler();
			long mask = hlh.getListMask(list);

			int count = 0;
			for (int row=0; row<mTableModel.getTotalRowCount(); row++)
				if ((mTableModel.getRecord(row).getFlags() & mask) != 0)
					count++;

			if (count < 2) {
				list = -1;
				}
			else {
				if (which == WHICH_RANDOM) {
					do {
						newRow = new Random().nextInt(mTableModel.getTotalRowCount());
						} while (newRow == oldRow || (mTableModel.getRecord(newRow).getFlags() & mask) == 0);
					}
				else {	// WHICH_NEXT
					do {
						newRow = (oldRow+1 == mTableModel.getTotalRowCount()) ? 0 : oldRow+1;
						} while ((mTableModel.getRecord(newRow).getFlags() & mask) == 0);
					}
				}
			}

		if (list == -1) {	// entire file
			if (which == WHICH_RANDOM) {
				do {
					newRow = new Random().nextInt(mTableModel.getTotalRowCount());
					} while (newRow == oldRow);
				}
			else {	// WHICH_NEXT
				newRow = (oldRow+1 == mTableModel.getTotalRowCount()) ? 0 : oldRow+1;
				}
			}

		mTableModel.setActiveRow(newRow);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}