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

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.file.DETaskAbstractOpenFile;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.io.BOMSkipper;
import com.actelion.research.table.CompoundTableLoader;
import com.actelion.research.table.model.CompoundTableListHandler;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.TreeSet;

public class DETaskImportHitlist extends DETaskAbstractOpenFile implements ActionListener {
	private static final String PROPERTY_LISTNAME = "listName";
	private static final String PROPERTY_KEYCOLUMN = "keyColumn";
	private static final String PROPERTY_CASE_SENSITIVE = "caseSensitive";

	private static final String ITEM_COLUMN_FROM_FILE = "<use column from list file>";

	public static final String TASK_NAME = "Import Row List";

    private CompoundTableModel mTableModel;
    private String mListName;
    private int	mKeyColumn;
    private String[] mPossibleKeyColumn;
    private TreeSet<String> mKeySet;
    private JTextField mFieldHitlistName;
    private JComboBox mComboBox;
	private JCheckBox mCheckBoxCaseSensitive;

	public DETaskImportHitlist(DataWarrior application) {
		super(application, "Open Row List File", FileHelper.cFileTypeTextTabDelimited);
        mTableModel = application.getActiveFrame().getTableModel();
		}

	@Override
	public Properties getPredefinedConfiguration() {
		return null;
		}

	@Override
    public JPanel createInnerDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED},
							{TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		content.add(new JLabel("Column containing key values:"), "0,0");
		mComboBox = new JComboBox(createDefaultColumns());
		mComboBox.setEditable(!isInteractive());
		content.add(mComboBox, "2,0");

		mFieldHitlistName = new JTextField("imported list");
		content.add(new JLabel("Name of new row list:"), "0,2");
		content.add(mFieldHitlistName, "2,2");
		if (!isInteractive())
			content.add(new JLabel("(Keep empty to use name defined in list file)", JLabel.CENTER), "0,4,2,4");

		mCheckBoxCaseSensitive = new JCheckBox("Case sensitive");
		mCheckBoxCaseSensitive.addActionListener(this);
		content.add(mCheckBoxCaseSensitive, "0,6,2,6");

		return content;
    	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mCheckBoxCaseSensitive) {
			String fileName = getDialogConfiguration().getProperty(PROPERTY_FILENAME, ASK_FOR_FILE);
			if (!ASK_FOR_FILE.equals(fileName))
				analyzeListFile(new File(fileName), mCheckBoxCaseSensitive.isSelected());
			return;
			}

		super.actionPerformed(e);
		}

	@Override
    public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (!super.isConfigurationValid(configuration, isLive))
			return false;

		if (isLive) {
			String listName = configuration.getProperty(PROPERTY_LISTNAME, "");
			if (listName.length() == 0) {
				showErrorMessage("No list name specified.");
				return false;
				}
			String fileName = configuration.getProperty(PROPERTY_FILENAME, "");
			if (fileName.length() == 0) {
				showErrorMessage("No file specified.");
				return false;
				}
			if (!ASK_FOR_FILE.equals(fileName)) {
				boolean caseSensitive = "true".equals(configuration.getProperty(PROPERTY_CASE_SENSITIVE, "true"));
				String error = analyzeListFile(new File(fileName), caseSensitive);
				if (error != null) {
					showErrorMessage(error);
					return false;
					}
				}
			String keyColumnName = configuration.getProperty(PROPERTY_KEYCOLUMN, "");
			if (keyColumnName.length() != 0) {
				int keyColumn = mTableModel.findColumn(keyColumnName);
				if (keyColumn == -1) {
					showErrorMessage("Key column '"+keyColumnName+"' not found.");
					return false;
					}
				if (isInteractive() && !isPossibleKeyColumn(keyColumn)) {
					showErrorMessage("Key column '"+keyColumn+"' does not contains any of the list keys.");
					return false;
					}
				}
			}
    	return true;
		}

	@Override
    public void setDialogConfigurationToDefault() {
		mCheckBoxCaseSensitive.setSelected(true);	// do this first to be known for analyseHitlist()
		super.setDialogConfigurationToDefault();

		/* other items are set through fileChanged()
		mComboBox.setSelectedItem(ITEM_COLUMN_FROM_FILE);
		mFieldHitlistName.setText("");
		*/
		}

	@Override
    public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);

		mComboBox.setSelectedItem(ITEM_COLUMN_FROM_FILE);	// default
		String keyColumnName = configuration.getProperty(PROPERTY_KEYCOLUMN, "");
		if (keyColumnName.length() != 0)
			mComboBox.setSelectedItem(keyColumnName);

		mFieldHitlistName.setText(configuration.getProperty(PROPERTY_LISTNAME, ""));
		mCheckBoxCaseSensitive.setSelected("true".equals(configuration.getProperty(PROPERTY_CASE_SENSITIVE, "true")));
		}

    public Properties getDialogConfiguration() {
    	Properties configuration = super.getDialogConfiguration();

    	String keyColumnName = (String)mComboBox.getSelectedItem();
    	if (keyColumnName != null && !keyColumnName.equals(ITEM_COLUMN_FROM_FILE))
    		configuration.setProperty(PROPERTY_KEYCOLUMN, keyColumnName);

    	String listName = mFieldHitlistName.getText();
    	if (listName != null)
    		configuration.setProperty(PROPERTY_LISTNAME, listName);

    	configuration.setProperty(PROPERTY_CASE_SENSITIVE, mCheckBoxCaseSensitive.isSelected() ? "true" : "false");

    	return configuration;
    	}

	/**
	 * If the file is valid, analyze file and configure all UI elements
	 * according to file content. Then preselect options according to
	 * current dialog configuration.
	 * @param file or null
	 */
	@Override
	protected void fileChanged(File file) {
		String error = null;
		if (file != null && file.exists()) {
			error = analyzeListFile(file, mCheckBoxCaseSensitive.isSelected());
			if (error == null) {
				mComboBox.removeAllItems();
				mComboBox.addItem(ITEM_COLUMN_FROM_FILE);
				for (String item:mPossibleKeyColumn)
					mComboBox.addItem(item);

				mComboBox.setSelectedItem(ITEM_COLUMN_FROM_FILE);	// default
				if (mKeyColumn != -1)
					mComboBox.setSelectedItem(mTableModel.getColumnTitle(mKeyColumn));

				mFieldHitlistName.setText(mListName);
				}
			else {
	            showErrorMessage(error);
				}
    		}

		boolean fileIsValid = (file != null && file.exists() && error == null);

		if (!fileIsValid) {
            mComboBox.removeAllItems();
			for (String item:createDefaultColumns())
				mComboBox.addItem(item);

			mComboBox.setSelectedItem(ITEM_COLUMN_FROM_FILE);
			mFieldHitlistName.setText("");
			}
		}

	/**
	 * Creates a list of all displayable columns as potential key column.
	 * @return the list
	 */
	private String[] createDefaultColumns() {
		ArrayList<String> columnList = new ArrayList<String>();
		columnList.add(ITEM_COLUMN_FROM_FILE);
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.isColumnDisplayable(column))
				columnList.add(mTableModel.getColumnTitle(column));

		return columnList.toArray(new String[0]);
		}

	/**
	 * Read and analyzes a hitlist file and sets mKeySet, mListName, mKeyColumn.
	 * If the file is not a valid list file, but contains potential list entries,
	 * then mListName is 'imported list' and mKeyColumn is set to that column that
	 * matches the found keys best or -1, if there are no matches.
	 * @param file
	 * @return error message or null
	 */
	private String analyzeListFile(File file, boolean caseSensitive) {
		mListName = null;
		String keyColumnName = null;
		BufferedReader theReader = null;
		try {
            theReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			BOMSkipper.skip(theReader);

            String hitlistNameLine = theReader.readLine();
            mListName = (hitlistNameLine != null
                      && hitlistNameLine.startsWith("<hitlistName=")) ?
                        CompoundTableLoader.extractValue(hitlistNameLine) : null;
            String keyColumnLine = theReader.readLine();
            keyColumnName = (keyColumnLine != null
                       && keyColumnLine.startsWith("<keyColumn=")) ?
                        CompoundTableLoader.extractValue(keyColumnLine) : null;
			}
		catch (IOException ioe) {}

		boolean isHitlistFile = (mListName != null && keyColumnName != null);

		try {
			if (theReader != null)
				theReader.close();

			theReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			BOMSkipper.skip(theReader);

			if (isHitlistFile) {
				theReader.readLine();
				theReader.readLine();
				}

			mKeySet = readKeys(theReader, caseSensitive);
            theReader.close();

            if (mKeySet.isEmpty())
                return "No keys were found in file '"+file.getName()+"'.";

            if (!isHitlistFile) {
            	for (String key:mKeySet)
            		if (key.indexOf('\t') != -1)
                        return "Row list files must not contain multiple entries per line.\nThe file '"+file.getName()+"' cannot be used as row list file.";

            	mListName = "imported list";
                }

        	int maxMatchColumn = -1;
        	int maxMatchCount = 0;
            ArrayList<String> columnList = new ArrayList<String>();
            for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
                if (mTableModel.isColumnDisplayable(column)) {
                	int matchCount = 0;
                	for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
                        String[] entry = mTableModel.separateEntries(mTableModel.getTotalValueAt(row, column));
                        for (int i=0; i<entry.length; i++) {
                        	String key = caseSensitive ? entry[i] : entry[i].toLowerCase();
                    		if (key.length() != 0 && mKeySet.contains(key)) {
                    			matchCount++;
                        		break;
                    			}
                			}
                		}
                	if (matchCount != 0) {
                        columnList.add(mTableModel.getColumnTitle(column));
                    	if (maxMatchCount < matchCount) {
                    		maxMatchCount = matchCount;
                    		maxMatchColumn = column;
                    		}
                		}
                	}
                }

            mKeyColumn = mTableModel.findColumn(keyColumnName);
            if (keyColumnName == null || mKeyColumn == -1 && !columnList.contains(mTableModel.getColumnTitle(mKeyColumn)))
            	mKeyColumn = maxMatchColumn;

            mPossibleKeyColumn = columnList.toArray(new String[0]);
            Arrays.sort(mPossibleKeyColumn);
            return null;
			}
		catch (IOException ioe) {
            return "Couldn't read file: "+ioe;
            }
		}

	/**
	 * @param column
	 * @return true if the column contains any of the keys of the most recently analyzed list file
	 */
	private boolean isPossibleKeyColumn(int column) {
		String columnName = mTableModel.getColumnTitle(column);
		for (String key:mPossibleKeyColumn)
			if (key.equals(columnName))
				return true;

		return false;
		}

	@Override
	public boolean isConfigurable() {
		if (!super.isConfigurable())
			return false;

		if (mTableModel.getTotalRowCount() == 0) {
			showErrorMessage("Cannot import row list if there are no rows.");
			return false;
			}
		if (mTableModel.getUnusedRowFlagCount() == 0) {
			showErrorMessage("Cannot import any row list, because the\n"
					+"maximum number of filters/lists is reached.");
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
	public DEFrame openFile(File file, Properties configuration) {
		boolean caseSensitive = "true".equals(configuration.getProperty(PROPERTY_CASE_SENSITIVE, "true"));

		// If we have a valid file name, then the file was already analyzed. Otherwise we have to analyze now.
		if (ASK_FOR_FILE.equals(configuration.getProperty(PROPERTY_FILENAME))) {
			String error = analyzeListFile(file, caseSensitive);
			if (error != null) {
				showErrorMessage(error);
				return null;
				}
			}

		startProgress("Creating row list", 0, 0);

		String keyColumnName = configuration.getProperty(PROPERTY_KEYCOLUMN, "");
		int keyColumn = (keyColumnName.length() == 0) ? mKeyColumn : mTableModel.findColumn(keyColumnName);

		String listName = configuration.getProperty(PROPERTY_LISTNAME);
		if (listName != null)
			mListName = listName;	// otherwise take the default created by analyzeHitlist()

		mTableModel.getListHandler().createList(mListName, -1, CompoundTableListHandler.FROM_KEY_SET, keyColumn, mKeySet, !caseSensitive);

        return null;
		}

	private TreeSet<String> readKeys(BufferedReader theReader, boolean caseSensitive) throws IOException {
        TreeSet<String> keySet = new TreeSet<String>();
        String key = theReader.readLine();
        while (key != null) {
        	if (!caseSensitive)
        		key = key.toLowerCase();
            keySet.add(key);
            key = theReader.readLine();
            }
        return keySet;
		}
	}
