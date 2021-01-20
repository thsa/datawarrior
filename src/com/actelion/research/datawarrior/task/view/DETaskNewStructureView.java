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

package com.actelion.research.datawarrior.task.view;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.table.model.CompoundTableModel;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskNewStructureView extends DETaskAbstractNewView {
	public static final String TASK_NAME = "New Structure View";

	private static final String PROPERTY_REF_COLUMN = "refColumn";

	private JComboBox mComboBoxStructureColumn;
	private CompoundTableModel mTableModel;
	private int mStructureColumn;

	public DETaskNewStructureView(Frame parent, DEMainPane mainPane, String whereViewName, int structureColumn) {
		super(parent, mainPane, whereViewName);
		mTableModel = mainPane.getTableModel();
		mStructureColumn = structureColumn;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public String getDefaultViewName() {
		return getMainPane().getDefaultViewName(DEMainPane.VIEW_TYPE_STRUCTURE, mStructureColumn);
	}

	@Override
	public void addInnerDialogContent(JPanel content) {
		content.add(new JLabel("Structure column:"), "1,7");
		mComboBoxStructureColumn = new JComboBox();
		int[] column = mTableModel.getSpecialColumnList(CompoundTableModel.cColumnTypeIDCode);
		if (column != null)
			for (int i:column)
				mComboBoxStructureColumn.addItem(mTableModel.getColumnTitle(i));
		mComboBoxStructureColumn.setEditable(true);
		content.add(mComboBoxStructureColumn, "3,7,5,7");
		}

	@Override
	public Properties getPredefinedConfiguration() {
		if (mStructureColumn == -1)
			return null;

		Properties configuration = super.getPredefinedConfiguration();
		if (configuration != null) {
			configuration.setProperty(PROPERTY_REF_COLUMN, mTableModel.getColumnTitleNoAlias(mStructureColumn));
		}

		return configuration;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_REF_COLUMN, mTableModel.getColumnTitleNoAlias((String)mComboBoxStructureColumn.getSelectedItem()));
		return configuration;
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		if (mComboBoxStructureColumn.getItemCount() != 0)
			mComboBoxStructureColumn.setSelectedIndex(0);
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		String refColumn = configuration.getProperty(PROPERTY_REF_COLUMN);
		if (refColumn != null)
			mComboBoxStructureColumn.setSelectedItem(refColumn);
		}

	@Override
	public boolean isConfigurable() {
		return mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnTypeIDCode) != null;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (!super.isConfigurationValid(configuration, isLive))
			return false;

		if (isLive) {
			boolean found = false;
			for (int c=0; c<mTableModel.getTotalColumnCount(); c++) {
				if (mTableModel.isColumnTypeStructure(c)) {
					found = true;
					break;
				}
			}
			if (!found) {
				showErrorMessage("No structure columns found.");
				return false;
			}
			String refColumn = configuration.getProperty(PROPERTY_REF_COLUMN);
			if (refColumn != null) {
				int column = mTableModel.findColumn(refColumn);
				if (column == -1) {
					showErrorMessage("Structure column '"+refColumn+"' not found.");
					return false;
				}
				if (!mTableModel.isColumnTypeStructure(column)) {
					showErrorMessage(refColumn+" does not contain chemical structures.");
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public void createNewView(String viewName, String whereView, String where, Properties configuration) {
		int refColumn = mTableModel.findColumn(configuration.getProperty(PROPERTY_REF_COLUMN));
		if (refColumn == -1) {
			for (int c=0; c<mTableModel.getTotalColumnCount(); c++) {
				if (mTableModel.isColumnTypeStructure(c)) {
					refColumn = c;
					break;
				}
			}
		}
		getMainPane().createNewView(viewName, DEMainPane.VIEW_TYPE_STRUCTURE, whereView, where, refColumn);
	}
}
