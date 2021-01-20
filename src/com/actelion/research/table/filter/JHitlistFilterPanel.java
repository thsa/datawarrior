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

package com.actelion.research.table.filter;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.actelion.research.table.model.*;
import com.actelion.research.table.model.CompoundTableListHandler;

public class JHitlistFilterPanel extends JFilterPanel implements ActionListener,CompoundTableListListener {
	private static final long serialVersionUID = 0x20061013;
	private static final String LIST_ANY = "<any>";

	private JComboBox		mComboBox;

	/**
	 * Creates the filter panel as UI to configure a task as part of a macro
	 * @param tableModel
	 */
	public JHitlistFilterPanel(CompoundTableModel tableModel) {
		this(tableModel, -1);
		}

	public JHitlistFilterPanel(CompoundTableModel tableModel, int exclusionFlag) {
		super(tableModel, PSEUDO_COLUMN_ROW_LIST, exclusionFlag, false, false);

		JPanel p1 = new JPanel();
		p1.setOpaque(false);
		p1.add(new JLabel("List name:"));

		mComboBox = new JComboBox();
		mComboBox.addItem(CompoundTableListHandler.LISTNAME_NONE);
		for (int i = 0; i<mTableModel.getListHandler().getListCount(); i++)
			mComboBox.addItem(mTableModel.getListHandler().getListName(i));
		mComboBox.addItem(CompoundTableListHandler.LISTNAME_ANY);
		if (isActive())
			mComboBox.addActionListener(this);
		else
			mComboBox.setEditable(true);
		p1.add(mComboBox);

		add(p1, BorderLayout.CENTER);

		mIsUserChange = true;
		}

	@Override
	public void enableItems(boolean b) {
		mComboBox.setEnabled(b);
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mComboBox) {
			updateExclusion(mIsUserChange);
			return;
			}

		super.actionPerformed(e);
		}

	private int getHitlistIndex() {
		int selectedIndex = mComboBox.getSelectedIndex();
		return (selectedIndex == 0) ?
					CompoundTableListHandler.LISTINDEX_NONE
			 : (selectedIndex == mComboBox.getItemCount()-1) ?
					CompoundTableListHandler.LISTINDEX_ANY
			 :	  selectedIndex - 1;
		}

	@Override
	public void updateExclusion(boolean isUserChange) {
		if (isActive() && isEnabled()) {
			mTableModel.setHitlistExclusion(getHitlistIndex(), mExclusionFlag, isInverse());

			if (isUserChange)
				fireFilterChanged(FilterEvent.FILTER_UPDATED, false);
			}
		}

	@Override
	public String getInnerSettings() {
		String selected = (String)mComboBox.getSelectedItem();
		return (CompoundTableListHandler.LISTNAME_NONE.equals(selected)) ? null
			 : (CompoundTableListHandler.LISTNAME_ANY.equals(selected)) ? LIST_ANY
			 : selected;
		}

	@Override
	public void applyInnerSettings(String settings) {
		if (settings != null
		 && !CompoundTableListHandler.LISTNAME_NONE.equals(settings)) {		// was this an ancient way of encoding???
			if (LIST_ANY.equals(settings)) {
				mComboBox.setSelectedIndex(mComboBox.getItemCount()-1);
				}
			else {
				if (!isActive()) {
					mComboBox.setSelectedItem(settings);
					}
				else {
					for (int i = 0; i<mTableModel.getListHandler().getListCount(); i++) {
						if (mTableModel.getListHandler().getListName(i).equals(settings)) {
							mComboBox.setSelectedIndex(i+1);
							break;
							}
						}
					}
				}
			}

		if (isInverse() || mComboBox.getSelectedIndex() != 0)
			updateExclusion(false);
		}

	@Override
	public void innerReset() {
		if (mComboBox.getSelectedIndex() != 0) {
			mComboBox.setSelectedIndex(0);
			updateExclusion(false);
			}
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		// avoid the default behaviour;
		}

	public void listChanged(CompoundTableListEvent e) {
		CompoundTableListHandler hitlistHandler = mTableModel.getListHandler();
		int hitlistCount = hitlistHandler.getListCount();
		boolean anySelected = CompoundTableListHandler.LISTNAME_ANY.equals(mComboBox.getSelectedItem());
		boolean changedListSelected = (mComboBox.getSelectedIndex()-1 == e.getListIndex());
		boolean update = false;
		if (e.getType() == CompoundTableListEvent.cAdd) {
			update = anySelected;
			mComboBox.insertItemAt(hitlistHandler.getListName(hitlistCount-1), hitlistCount);
			}
		else if (e.getType() == CompoundTableListEvent.cDelete) {
			update = (anySelected || changedListSelected);
			if (changedListSelected)
				mComboBox.setSelectedIndex(0);
			mComboBox.removeItemAt(e.getListIndex()+1);
			}
		else if (e.getType() == CompoundTableListEvent.cChange) {
			update = (anySelected || changedListSelected);
			}

		if (update) {
			mIsUserChange = false;
			updateExclusionLater();
			mIsUserChange = true;
			}
		}

	@Override
	public int getFilterType() {
		return FILTER_TYPE_ROWLIST;
		}
	}
