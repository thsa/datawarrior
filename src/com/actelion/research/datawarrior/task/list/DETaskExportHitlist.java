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

package com.actelion.research.datawarrior.task.list;

import info.clearthought.layout.TableLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.datawarrior.task.file.JFilePathLabel;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.table.model.CompoundTableModel;

public class DETaskExportHitlist extends ConfigurableTask implements ActionListener {
	private static final String PROPERTY_FILENAME = "fileName";
	private static final String PROPERTY_LISTNAME = "listName";
	private static final String PROPERTY_KEY_COLUMN = "keyColumn";

	public static final String TASK_NAME = "Export Row List";

	private CompoundTableModel  mTableModel;
	private JComboBox		   mComboBoxHitlist,mComboBoxKeyColumn;
	private JFilePathLabel		mLabelFileName;
	private boolean				mIsInteractive,mCheckOverwrite;

	public DETaskExportHitlist(DEFrame parent, boolean isInteractive) {
		super(parent, false);
		mTableModel = parent.getTableModel();
		mIsInteractive = isInteractive;
		mCheckOverwrite = true;
		}
	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
	public JPanel createDialogContent() {
		JPanel mp = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 16, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8} };
		mp.setLayout(new TableLayout(size));

		String[] hitlistNames = mTableModel.getListHandler().getListNames();

		mComboBoxHitlist = (hitlistNames == null) ? new JComboBox() : new JComboBox(hitlistNames);
		mComboBoxHitlist.setEditable(!mIsInteractive);
		mp.add(new JLabel("Row list:"), "1,1");
		mp.add(mComboBoxHitlist, "3,1,5,1");

		ArrayList<String> columnList = new ArrayList<String>();
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.isColumnDisplayable(column))
				columnList.add(mTableModel.getColumnTitle(column));
		String[] columnName = columnList.toArray(new String[0]);
		Arrays.sort(columnName);
		mComboBoxKeyColumn = new JComboBox(columnName);
		mComboBoxKeyColumn.setEditable(!mIsInteractive);
		mp.add(new JLabel("Key column:"), "1,3");
		mp.add(mComboBoxKeyColumn, "3,3,5,3");

		mp.add(new JLabel("File name:"), "1,5");

		JButton buttonEdit = new JButton(JFilePathLabel.BUTTON_TEXT);
		buttonEdit.addActionListener(this);
		mp.add(buttonEdit, "5,5");

		mLabelFileName = new JFilePathLabel(!mIsInteractive);
		mp.add(mLabelFileName, "1,7,5,7");

		return mp;
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(JFilePathLabel.BUTTON_TEXT)) {
			String suggestedName = (mLabelFileName.getPath() != null) ? mLabelFileName.getPath() : (String)mComboBoxHitlist.getSelectedItem();
			String filename = new FileHelper(getParentFrame()).selectFileToSave("Save Row List File", FileHelper.cFileTypeTextTabDelimited, suggestedName);
			if (filename != null) {
				mLabelFileName.setPath(filename);
				mCheckOverwrite = false;
				}
			return;
			}
 		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mComboBoxHitlist.getItemCount() != 0)
			mComboBoxHitlist.setSelectedIndex(0);
		if (mComboBoxKeyColumn.getItemCount() != 0)
			mComboBoxKeyColumn.setSelectedIndex(0);
		mLabelFileName.setPath(null);
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mComboBoxHitlist.setSelectedItem(configuration.getProperty(PROPERTY_LISTNAME, ""));
		mComboBoxKeyColumn.setSelectedItem(configuration.getProperty(PROPERTY_KEY_COLUMN, ""));
		mLabelFileName.setPath(configuration.getProperty(PROPERTY_FILENAME));
		}

	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		String fileName = mLabelFileName.getPath();
		if (fileName != null)
			configuration.setProperty(PROPERTY_FILENAME, fileName);

		String keyColumn = (String)mComboBoxKeyColumn.getSelectedItem();
		if (keyColumn != null)
			configuration.setProperty(PROPERTY_KEY_COLUMN, keyColumn);

		String listName = (String)mComboBoxHitlist.getSelectedItem();
		if (listName != null)
			configuration.setProperty(PROPERTY_LISTNAME, listName);

		return configuration;
		}

	@Override
	public boolean isConfigurable() {
		if (mTableModel.getListHandler().getListCount() == 0) {
			showErrorMessage("No row list found.");
			return false;
			}

		return true;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (isLive && !isFileAndPathValid(configuration.getProperty(PROPERTY_FILENAME), true, mCheckOverwrite))
			return false;

		String listName = configuration.getProperty(PROPERTY_LISTNAME, "");
		if (listName.length() == 0) {
			showErrorMessage("No list name specified.");
			return false;
			}

		if (isLive) {
			String columnName = configuration.getProperty(PROPERTY_KEY_COLUMN, "");
			if (mTableModel.findColumn(columnName) == -1) {
				showErrorMessage("Column '"+columnName+"' not found.");
				return false;
				}
			}

		return true;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/lists.html#ImportExport";
		}

	@Override
	public void runTask(Properties configuration) {
		String fileName = configuration.getProperty(PROPERTY_FILENAME);
		String listName = configuration.getProperty(PROPERTY_LISTNAME);
		int column = mTableModel.findColumn(configuration.getProperty(PROPERTY_KEY_COLUMN));

		TreeSet<String> keySet = new TreeSet<String>();
		int flagNo = mTableModel.getListHandler().getListFlagNo(mTableModel.getListHandler().getListIndex(listName));
		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			if (mTableModel.getTotalRecord(row).isFlagSet(flagNo)) {
				String[] entry = mTableModel.separateEntries(mTableModel.getTotalValueAt(row, column));
				for (int i=0; i<entry.length; i++)
					if (entry[i].length() > 0)
						keySet.add(entry[i]);
				}
			}
		try {
			BufferedWriter theWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resolvePathVariables(fileName)) ,"UTF-8"));
			theWriter.write("<hitlistName=\""+listName+"\">");
			theWriter.newLine();
			theWriter.write("<keyColumn=\""+mTableModel.getColumnTitleNoAlias(column)+"\">");
			theWriter.newLine();
			for (String key:keySet) {
				theWriter.write(key);
				theWriter.newLine();
				}
			theWriter.close();
			}
		catch (IOException ioe) {
			showErrorMessage("Couldn't write file: "+ioe);
			}
		}
	}
