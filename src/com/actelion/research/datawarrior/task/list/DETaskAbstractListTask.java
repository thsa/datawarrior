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

import java.util.Properties;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.table.model.CompoundTableModel;

public abstract class DETaskAbstractListTask extends ConfigurableTask {
	private static final String PROPERTY_HITLIST_NAME = "listName";

    private CompoundTableModel mTableModel;
    private JComboBox   mComboBoxListName;
    private int         mListIndex;

    /**
     * The listIndex parameter may be used to override the configuration's list name.
     * If listIndex is preconfigured (i.e. != -1) and defineAndRun() is called, then
     * this task will immediately run without showing a configuration dialog.
     * @param parent
     * @param listIndex -1 or list index
     */
    public DETaskAbstractListTask(DEFrame parent, int listIndex) {
		super(parent, true);
        mTableModel = parent.getTableModel();
        mListIndex = listIndex;
        }

    @Override
	public Properties getPredefinedConfiguration() {
    	if (mListIndex != -1) {
	    	Properties configuration = new Properties();
			configuration.setProperty(PROPERTY_HITLIST_NAME, mTableModel.getListHandler().getListName(mListIndex));
	        return configuration;
    		}

    	return null;
		}

	@Override
	public boolean isConfigurable() {
		if (mTableModel.getTotalRowCount() == 0) {
			showErrorMessage("No rows found");
			return false;
			}
		if (mTableModel.getListHandler().getListCount() == 0) {
			showErrorMessage("No row lists found");
			return false;
			}
		return true;
		}

	@Override
	public JComponent createDialogContent() {
		JPanel sp = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
		        			{8, TableLayout.PREFERRED, 8} };
        sp.setLayout(new TableLayout(size));

        sp.add(new JLabel("Row list name:"), "1,1");
        String[] listNames = mTableModel.getListHandler().getListNames();
        if (listNames != null)
        	mComboBoxListName = new JComboBox(listNames);
        else
        	mComboBoxListName = new JComboBox();
        mComboBoxListName.setEditable(true);
        sp.add(mComboBoxListName, "3,1");

        return sp;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_HITLIST_NAME, (String)mComboBoxListName.getSelectedItem());
        return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mComboBoxListName.setSelectedItem(configuration.getProperty(PROPERTY_HITLIST_NAME, ""));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		String listName = (mTableModel.getListHandler().getListCount() == 0) ? ""
						: mTableModel.getListHandler().getListName(0);
		mComboBoxListName.setSelectedItem(listName);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String listName = configuration.getProperty(PROPERTY_HITLIST_NAME, "");
		if (listName.length() == 0) {
			showErrorMessage("No row list name defined.");
			return false;
			}
		if (isLive) {
			if (mTableModel.getListHandler().getListIndex(listName) == -1) {
				showErrorMessage("Row list '"+listName+"' not found.");
				return false;
				}
			}
		
		return true;
		}

	public String getListName(Properties configuration) {
		return (mListIndex != -1) ? mTableModel.getListHandler().getListName(mListIndex)
				: configuration.getProperty(PROPERTY_HITLIST_NAME);
		}

	public int getListIndex(Properties configuration) {
		return (mListIndex != -1) ? mListIndex
				: mTableModel.getListHandler().getListIndex(configuration.getProperty(PROPERTY_HITLIST_NAME));
		}

	public CompoundTableModel getTableModel() {
		return mTableModel;
		}
	}
