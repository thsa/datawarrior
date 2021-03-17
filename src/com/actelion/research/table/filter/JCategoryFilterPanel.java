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

import com.actelion.research.chem.AbstractDepictor;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.gui.JStructureView;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableListener;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class JCategoryFilterPanel extends JFilterPanel
				implements ActionListener,CompoundTableListener,MouseListener {
	private static final long serialVersionUID = 0x20060821;

	public static final int cPreferredCheckboxCount = 16;
	public static final int cMaxCheckboxCount = 128;

	private String[]	mCategoryList;
	private JCheckBox[]	mCheckBox;
	private JTextArea	mTextArea;
	private JPanel		mCategoryOptionPanel;

	/**
	 * Creates the filter panel as UI to configure a task as part of a macro
	 * @param tableModel
	 */
	public JCategoryFilterPanel(CompoundTableModel tableModel) {
		this(tableModel, -1, -1);
		}

	public JCategoryFilterPanel(CompoundTableModel tableModel, int columnIndex, int exclusionFlag) {
		super(tableModel, columnIndex, exclusionFlag, false, false);

		mCategoryOptionPanel = new JPanel();
		mCategoryOptionPanel.setOpaque(false);
		if (isActive()) {
			addCheckBoxes();
			addMouseListener(this);
			}
		else {
			addTextField();
			}
		add(mCategoryOptionPanel, BorderLayout.CENTER);

		mIsUserChange = true;
		}

	private void addTextField() {
		double[][] size = { {4, TableLayout.PREFERRED, 4},
							{TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 4} };
		mCategoryOptionPanel.setLayout(new TableLayout(size));
		mTextArea = new JTextArea();
		mTextArea.setPreferredSize(new Dimension(300, 128));
		JScrollPane scrollPane = new JScrollPane(mTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		mCategoryOptionPanel.add(new JLabel("Excluded categories:"), "1,0");
		mCategoryOptionPanel.add(scrollPane, "1,2");
		}

	private void addCheckBoxes() {
		boolean isIDCode = mTableModel.isColumnTypeStructure(mColumnIndex);
		mCategoryList = mTableModel.getCategoryList(mColumnIndex);
		mCheckBox = new JCheckBox[mCategoryList.length];

		double[] sizeV = new double[mCategoryList.length+1];
		for (int i=0; i<mCategoryList.length; i++)
			sizeV[i] = HiDPIHelper.scale(isIDCode && !CompoundTableConstants.cTextMultipleCategories.equals(mCategoryList[i]) ?
					Math.max(36, Math.min(100, (int)Math.sqrt(200*mCategoryList[i].length()))) : 18);
		sizeV[mCategoryList.length] = 4;
		double[] sizeH = isIDCode ? new double[2] : new double[1];
		sizeH[0] = isIDCode ? HiDPIHelper.scale(24) : TableLayout.PREFERRED;
		if (isIDCode)
			sizeH[1] = TableLayout.PREFERRED;
		double[][] size = { sizeH, sizeV };
		mCategoryOptionPanel.setLayout(new TableLayout(size));

		for (int i=0; i<mCategoryList.length; i++) {
			String categoryName = isIDCode ? "" : mCategoryList[i];
			if (categoryName.length() > 32)
				categoryName = categoryName.substring(0, 30) + " ...";
			mCheckBox[i] = new JCheckBox(categoryName, true);
			// Change font to allow displaying rare unicode characters
			mCheckBox[i].setFont(new Font(Font.SANS_SERIF, Font.PLAIN, mCheckBox[i].getFont().getSize()));
			mCheckBox[i].addMouseListener(this);
			mCheckBox[i].setActionCommand("cat"+i);
			mCheckBox[i].addActionListener(this);
			mCategoryOptionPanel.add(mCheckBox[i], "0,"+i);
			if (isIDCode) {
				String idcode = mCategoryList[i];
				if (idcode != null && idcode.length() != 0) {
					if (idcode.equals(CompoundTableConstants.cTextMultipleCategories)) {
						mCategoryOptionPanel.add(new JLabel("multiple structures"), "1,"+i);
						}
					else {
						int index = idcode.indexOf(' ');
						JStructureView view = new JStructureView(DnDConstants.ACTION_COPY_OR_MOVE, 0);
						view.setDisplayMode(AbstractDepictor.cDModeHiliteAllQueryFeatures | AbstractDepictor.cDModeSuppressChiralText);
						view.setClipboardHandler(new ClipboardHandler());
						if (index == -1)
							view.setIDCode(idcode);
						else
							view.setIDCode(idcode.substring(0, index), idcode.substring(index+1));
						view.setPreferredSize(new Dimension(HiDPIHelper.scale(120), HiDPIHelper.scale(48)));
						mCategoryOptionPanel.add(view, "1, "+i);
						}
					}
				}
			}
		}

	@Override
	public void enableItems(boolean b) {
		if (isActive()) {
			for (int i=0; i<mCheckBox.length; i++)
				mCheckBox[i].setEnabled(b);
			}
		else {
			mTextArea.setEnabled(b);
			}
		}

	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}

	public void mousePressed(MouseEvent e) {
		handlePopupTrigger(e);
		}

	public void mouseReleased(MouseEvent e) {
		handlePopupTrigger(e);
		}

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.startsWith("Select All")) {
			boolean update = false;
			for (int i=0; i<mCheckBox.length; i++) {
				if (!mCheckBox[i].isSelected()) {
					mCheckBox[i].setSelected(true);
					update = true;
					}
				}
			if (update)
				updateExclusion(true);
			return;
			}
		if (command.startsWith("Deselect All")) {
			boolean update = false;
			for (int i=0; i<mCheckBox.length; i++) {
				if (mCheckBox[i].isSelected()) {
					mCheckBox[i].setSelected(false);
					update = true;
					}
				}
			if (update)
				updateExclusion(true);
			return;
			}
		if (command.startsWith("cat")) {
			updateExclusion(true);
			return;
			}

		super.actionPerformed(e);
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		super.compoundTableChanged(e);

		mIsUserChange = false;

		if (e.getType() == CompoundTableEvent.cAddRows
		 || e.getType() == CompoundTableEvent.cDeleteRows
		 || (e.getType() == CompoundTableEvent.cChangeColumnData && e.getColumn() == mColumnIndex)) {
			if (!mTableModel.isColumnTypeCategory(mColumnIndex)) {
				removePanel();
				return;
				}
			if (updateCheckboxes() && e.getType() != CompoundTableEvent.cDeleteRows)
				updateExclusionLater();
			}

		mIsUserChange = true;
		}

	private void handlePopupTrigger(MouseEvent e) {
		if (mCheckBox.length != 0 && e.isPopupTrigger()) {
			JPopupMenu popup = new JPopupMenu();
			JMenuItem item = new JMenuItem("Select All");
			item.addActionListener(this);
			popup.add(item);
			item = new JMenuItem("Deselect All");
			item.addActionListener(this);
			int x = e.getX();
			int y = e.getY();
			if (e.getSource() instanceof JCheckBox) {
				x += ((JCheckBox)e.getSource()).getX();
				y += ((JCheckBox)e.getSource()).getY();
				}
			popup.add(item);
			popup.show(this, x, y);
			}
		}

	private boolean updateCheckboxes() {
		String[] newCategoryList = mTableModel.getCategoryList(mColumnIndex);
		if (newCategoryList.length <= 1
		 || newCategoryList.length > cMaxCheckboxCount) {
			removePanel();
			return false;
			}

		String[] oldCategoryList = mCategoryList;
		boolean categoriesChanged = (newCategoryList.length != oldCategoryList.length);
		if (!categoriesChanged) {
			for (int i=0; i<newCategoryList.length; i++) {
				if (!newCategoryList[i].equals(oldCategoryList[i])) {
					categoriesChanged = true;
					break;
					}
				}
			}
		
		if (categoriesChanged) {
			JCheckBox[] oldCheckBox = mCheckBox;
			mCategoryOptionPanel.removeAll();
			addCheckBoxes();
			for (int i=0; i<mCategoryList.length; i++) {
				int oldIndex = -1;
				for (int j=0; j<oldCategoryList.length; j++) {
					if (newCategoryList[i].equals(oldCategoryList[j])) {
						oldIndex = j;
						break;
						}
					}
				if (oldIndex != -1 && !oldCheckBox[oldIndex].isSelected())
					mCheckBox[i].setSelected(false);
				}
			}
		getParent().getParent().validate();
		return true;
		}

	@Override
	public String getInnerSettings() {
		String settings = null;
		if (isActive()) {
			for (int i=0; i<mCheckBox.length; i++)
				if (!mCheckBox[i].isSelected())
					settings = (settings == null) ?
							mCheckBox[i].getText() : settings+"\t"+mCheckBox[i].getText();
			}
		else {
			settings = mTextArea.getText().replace('\n', '\t');
			int index = settings.length();	// remove trailing TABs
			while (index>0 && settings.charAt(index-1) == '\t')
				index--;
			if (index < settings.length())
				settings = settings.substring(0, index);
			}
		return settings;
		}

	@Override
	public void applyInnerSettings(String settings) {
		if (isActive()) {
			for (int i=0; i<mCheckBox.length; i++)
				mCheckBox[i].setSelected(true);

			int index = 0;
			while (index != -1) {
				int index2 = settings.indexOf('\t', index);
				if (index2 == -1) {
					exclude(settings.substring(index));
					index = -1;
					}
				else {
					exclude(settings.substring(index, index2));
					index = index2+1;
					}
				}

			updateExclusion(false);
			}
		else {
			mTextArea.setText(settings.replace('\t', '\n'));
			}
		}

	@Override
	public void innerReset() {
		if (isActive()) {
			boolean found = false;
			for (int i=0; i<mCheckBox.length; i++) {
				if (!mCheckBox[i].isSelected()) {
					mCheckBox[i].setSelected(true);
					found = true;
					}
				}
			if (found)
				mTableModel.clearRowFlag(mExclusionFlag);
			}
		else {
			mTextArea.setText("");
			}
		}

	@Override
	public void updateExclusion(boolean isUserChange) {
		if (!isEnabled())
			return;

		boolean[] selected = new boolean[mCheckBox.length];
		for (int j=0; j<mCheckBox.length; j++)
			selected[j] = mCheckBox[j].isSelected();

		if (isActive()) {
			mTableModel.setCategoryExclusion(mExclusionFlag, mColumnIndex, selected, isInverse());

			if (isUserChange)
				fireFilterChanged(FilterEvent.FILTER_UPDATED, false);
			}
		}

	private void exclude(String category) {
		for (int i=0; i<mCheckBox.length; i++) {
			if (mCheckBox[i].getText().equals(category)) {
				mCheckBox[i].setSelected(false);
				break;
				}
			}
		}

	@Override
	public int getFilterType() {
		return FILTER_TYPE_CATEGORY;
		}
	}
