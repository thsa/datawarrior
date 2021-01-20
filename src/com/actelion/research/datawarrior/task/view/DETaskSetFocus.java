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

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableListHandler;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.FocusableView;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;


public class DETaskSetFocus extends DETaskAbstractSetViewOptions {
	public static final String TASK_NAME = "Set Focus To Row List";

	private static final String PROPERTY_ROWS = "rows";
	private static final String ITEM_ALL_ROWS = "<All Rows>";
	private static final String CODE_ALL_ROWS = "<all>";
	private static final String ITEM_SELECTED_ROWS = "<Selected Rows>";
	private static final String CODE_SELECTED_ROWS = "<selected>";

	private JComboBox		mComboBox;
	
	public DETaskSetFocus(Frame owner, DEMainPane mainPane, FocusableView view) {
		super(owner, mainPane, view);
		}
	
	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return false;
		}

	@Override
	public OTHER_VIEWS getOtherViewMode() {
		return OTHER_VIEWS.GRAPHICAL;
		}

	@Override
	public JComponent createViewOptionContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap} };
		JPanel cp = new JPanel();
		cp.setLayout(new TableLayout(size));

		mComboBox = new JComboBox();
		mComboBox.addItem(ITEM_ALL_ROWS);
		mComboBox.addItem(ITEM_SELECTED_ROWS);
		for (int i = 0; i<getTableModel().getListHandler().getListCount(); i++)
			mComboBox.addItem(getTableModel().getColumnTitleExtended(CompoundTableListHandler.getColumnFromList(i)));
		mComboBox.setEditable(!hasInteractiveView());
		mComboBox.addItemListener(this);
		cp.add(new JLabel("Row focus on:"), "1,1");
		cp.add(mComboBox, "3,1");

		return cp;
	    }

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof FocusableView) ? null : "A focus can only be set on 2D-, 3D- and structure-views.";
		}

	@Override
	public void setDialogToDefault() {
        mComboBox.setSelectedIndex(0);
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		String columnName = configuration.getProperty(PROPERTY_ROWS, CODE_ALL_ROWS);
		if (columnName.equals(CODE_ALL_ROWS)) {
			mComboBox.setSelectedItem(ITEM_ALL_ROWS);
			}
		else if (columnName.equals(CODE_SELECTED_ROWS)) {
			mComboBox.setSelectedItem(ITEM_SELECTED_ROWS);
			}
		else {
			int pseudoColumn = getTableModel().findColumn(columnName);
			mComboBox.setSelectedItem(!hasInteractiveView() && pseudoColumn == -1 ? columnName : getTableModel().getColumnTitleExtended(pseudoColumn));
			}
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		String item = (String)mComboBox.getSelectedItem();
		configuration.setProperty(PROPERTY_ROWS, item.equals(ITEM_ALL_ROWS) ? CODE_ALL_ROWS
											   : item.equals(ITEM_SELECTED_ROWS) ? CODE_SELECTED_ROWS
											   : getTableModel().getColumnTitleNoAlias(item));
		}

	@Override
	public void addViewConfiguration(CompoundTableView view, Properties configuration) {
		int list = ((FocusableView)view).getFocusList();
		if (list == FocusableView.cFocusNone)
			configuration.setProperty(PROPERTY_ROWS, CODE_ALL_ROWS);
		else if (list == FocusableView.cFocusOnSelection)
			configuration.setProperty(PROPERTY_ROWS, CODE_SELECTED_ROWS);
		else
			configuration.setProperty(PROPERTY_ROWS, getTableModel().getColumnTitleNoAlias(CompoundTableListHandler.getColumnFromList(list)));
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		if (view != null) {
			String pseudoColumnName = configuration.getProperty(PROPERTY_ROWS, CODE_ALL_ROWS);
			if (!CODE_ALL_ROWS.equals(pseudoColumnName)
			 && !CODE_SELECTED_ROWS.equals(pseudoColumnName)) {
				int pseudoColumn = getTableModel().findColumn(pseudoColumnName);
				if (pseudoColumn == -1) {
					showErrorMessage(pseudoColumnName+"' not found.");
					return false;
					}
				}
			}

		return true;
		}

	@Override
	public void enableItems() {
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		if (!(view instanceof FocusableView))
			return;

		String pseudoColumnName = configuration.getProperty(PROPERTY_ROWS, CODE_ALL_ROWS);
		if (pseudoColumnName.equals(CODE_ALL_ROWS))
			((FocusableView)view).setFocusList(FocusableView.cFocusNone);
		else if (pseudoColumnName.equals(CODE_SELECTED_ROWS))
			((FocusableView)view).setFocusList(FocusableView.cFocusOnSelection);
		else
			((FocusableView)view).setFocusList(
					CompoundTableListHandler.convertToListIndex(getTableModel().findColumn(pseudoColumnName)));
		}
	}
