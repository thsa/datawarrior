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

import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.gui.hidpi.HiDPIIconButton;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableListener;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class JCategoryBrowser extends JFilterPanel
				implements ActionListener,ChangeListener,CompoundTableListener,Runnable {
	private static final long serialVersionUID = 0x20060821;

	private static final String DISABLED_TEXT = "Category Browser";

	private Frame			mParentFrame;
	private JComboBox		mComboBox;
	private JSlider			mSlider;
	private JTextField		mTextFieldCategory;
	private JButton			mButtonLeft,mButtonRight,mPressedButton;
	private Thread			mThread;
	private String			mSelectedItem;
	private JTextField      mTextFieldFrameDelay;

	/**
	 * Creates the filter panel as UI to configure a task as part of a macro
	 * @param tableModel
	 */
	public JCategoryBrowser(Frame parent, CompoundTableModel tableModel) {
		this(parent, tableModel, -1);
		}

	public JCategoryBrowser(Frame parent, CompoundTableModel tableModel, int exclusionFlag) {
		super(tableModel, -1, exclusionFlag, true, false);

		mParentFrame = parent;

		JPanel contentPanel = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap/2, TableLayout.PREFERRED, gap/2, TableLayout.FILL, TableLayout.PREFERRED, gap/2},
							{TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap/2} };
		contentPanel.setLayout(new TableLayout(size));
		contentPanel.setOpaque(false);

		contentPanel.add(new JLabel("Column:"), "1,0");
		mComboBox = new JComboBox() {
			private static final long serialVersionUID = 0x20080611;
			public Dimension getPreferredSize() {
				Dimension size = super.getPreferredSize();
				size.width = Math.min(HiDPIHelper.scale(72), size.width);
				return size;
				}
			};
		mComboBox.setEditable(!isActive());
		contentPanel.add(mComboBox, "3,0,4,0");

		if (isActive()) {
			JPanel sliderPanel = new JPanel();
			sliderPanel.setLayout(new BorderLayout());
	
			mButtonLeft = createButton(180);
			sliderPanel.add(wrapButton(mButtonLeft), BorderLayout.WEST);
	
			mButtonRight = createButton(0);
			sliderPanel.add(wrapButton(mButtonRight), BorderLayout.EAST);
	
			mSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
			mSlider.setOpaque(false);
			mSlider.setPreferredSize(new Dimension(HiDPIHelper.scale(100), mSlider.getPreferredSize().height));
			mSlider.addChangeListener(this);
			mSlider.setEnabled(false);
			sliderPanel.add(mSlider, BorderLayout.CENTER);
			contentPanel.add(sliderPanel, "1,2,4,2");
			}
		else {
			contentPanel.add(new JLabel("Category:"), "1,2");
			mTextFieldCategory = new JTextField();
			contentPanel.add(mTextFieldCategory, "3,2,4,2");
			}

		updateComboBox();

		add(contentPanel, BorderLayout.CENTER);

		mIsUserChange = true;
		}

	@Override
	public boolean canEnable(boolean suppressErrorMessages) {
		if (isActive() && mComboBox.getItemCount() == 0) {
			if (!suppressErrorMessages)
				JOptionPane.showMessageDialog(mParentFrame, "This category browser cannot be enabled, because\n" +
					"the dataset has no columns with category data.");
			return false;
			}
		return true;
		}

	@Override
	public void enableItems(boolean b) {
		mComboBox.setEnabled(b);
		if (isActive()) {
			if (!b)
				setText(DISABLED_TEXT, Color.black);
			// the red enabled text is drawn by updateExclusion()

			mButtonLeft.setEnabled(b);
			mButtonRight.setEnabled(b);
			mSlider.setEnabled(b);
			}
		else {
			setText(DISABLED_TEXT, Color.black);
			mTextFieldCategory.setEnabled(b);
			}
		}

	private JPanel wrapButton(JButton button) {
		JPanel bp = new JPanel();
		double[][] size = { { TableLayout.PREFERRED }, {TableLayout.FILL, TableLayout.PREFERRED, TableLayout.FILL} };
		bp.setLayout(new TableLayout(size));
		bp.setOpaque(false);
		bp.add(button, "0,1");
		return bp;
		}

	private JButton createButton(int rotation) {
		JButton b = new HiDPIIconButton("toNext.png", null, null, rotation, "bevel") {
			public void processMouseEvent(MouseEvent e) {
				super.processMouseEvent(e);
				processButtonMouseEvent(e);
				}
			};
		b.setEnabled(false);
		return b;
		}

	@Override
	protected JPanel createAnimationOptionPanel() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap} };
		JPanel p = new JPanel();
		p.setLayout(new TableLayout(size));
		p.add(new JLabel("Time rate (ms):"), "1,1");

		mTextFieldFrameDelay = new JTextField(""+ getFrameMillis(), 6);
		p.add(mTextFieldFrameDelay, "3,1");

		return p;
		}

	@Override
	public void applyAnimationOptions() {
		setFrameMillis(Integer.parseInt(mTextFieldFrameDelay.getText()));
		}

	@Override
	public int getDefaultFrameMillis() {
		return 500;
		}

	@Override
	public int getFullFrameAnimationCount() {
		return mSlider.getMaximum() + 1;
		}

	@Override
	public void setAnimationFrame(int frame) {
		int category = frame % (mSlider.getMaximum() + 1);
		mSlider.setValue(category);
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mComboBox) {
			mColumnIndex = (mComboBox.getItemCount() == 0) ? -1
						 : mTableModel.findColumn((String)mComboBox.getSelectedItem());
			if (isActive()) {
				mSlider.setValue(0);
				mSlider.setMaximum(mTableModel.getCategoryCount(mColumnIndex)-1);
				}
			updateExclusion(false, mIsUserChange);
			return;
			}

		super.actionPerformed(e);
		}

	private void processButtonMouseEvent(MouseEvent e) {
		if (e.getID() == MouseEvent.MOUSE_PRESSED) {
			mPressedButton = (JButton)e.getSource();
			mThread = new Thread(this);
			mThread.start();
			}
		else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
			mThread = null;
			}
		}

	@Override
	public void run() {
		int delay = 500;
		while (Thread.currentThread() == mThread) {
			adaptSlider();
			try {
				Thread.sleep(delay);
				}
			catch (InterruptedException e) {}
			delay = 50;
			}
		}

	private void adaptSlider() {
		int currentValue = mSlider.getValue();
		if (mPressedButton == mButtonLeft) {
			int newValue = mSlider.getMinimum()-1;
			for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
				CompoundRecord record = mTableModel.getTotalRecord(row);
				if (mTableModel.isVisibleNeglecting(record, mExclusionFlag)) {
					int category = mTableModel.getCategoryIndex(mColumnIndex, record);
					if (category < currentValue && category > newValue)
						newValue = category;
					}
				}
			if (newValue >= mSlider.getMinimum())
				mSlider.setValue(newValue);
			}
		else if (mPressedButton == mButtonRight) {
			int newValue = mSlider.getMaximum()+1;
			for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
				CompoundRecord record = mTableModel.getTotalRecord(row);
				if (mTableModel.isVisibleNeglecting(record, mExclusionFlag)) {
					int category = mTableModel.getCategoryIndex(mColumnIndex, record);
					if (category > currentValue && category < newValue)
						newValue = category;
					}
				}
			if (newValue <= mSlider.getMaximum())
				mSlider.setValue(newValue);
			}
		}

	@Override
	public void stateChanged(ChangeEvent e) {
		updateExclusion(((JSlider)e.getSource()).getValueIsAdjusting(), mIsUserChange);
		}

	@Override
	public void compoundTableChanged(CompoundTableEvent e) {
		mIsUserChange = false;

		if (e.getType() == CompoundTableEvent.cAddColumns) {
			updateComboBox();
			}
		else if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			int oldColumnIndex = mColumnIndex;
			mColumnIndex = (mColumnIndex == -1) ? -1 : e.getMapping()[mColumnIndex];
			updateComboBox();
			if (oldColumnIndex != -1 && mColumnIndex == -1)
				updateExclusionLater();
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnName) {
			updateComboBox();
			}
		else if (e.getType() == CompoundTableEvent.cDeleteRows
			  || e.getType() == CompoundTableEvent.cAddRows
			  || e.getType() == CompoundTableEvent.cChangeColumnData) {
			int columnIndex = mColumnIndex;
			updateComboBox();	// may set mColumnIndex to -1 if column type changes
			if ((e.getType() == CompoundTableEvent.cChangeColumnData && e.getColumn() == columnIndex)
			 || e.getType() != CompoundTableEvent.cChangeColumnData) {
				if (mColumnIndex != -1) {
					mSlider.setMaximum(mTableModel.getCategoryCount(mColumnIndex)-1);
					String[] categoryList = mTableModel.getCategoryList(mColumnIndex);
					boolean found = false;
					for (int i=0; i<categoryList.length; i++) {
						if (categoryList[i].equals(mSelectedItem)) {
							mSlider.setValue(i);
							found = true;
							break;
							}
						}
					if (!found)
						mSlider.setValue(0);
					}
				updateExclusionLater();
				}
			}

		mIsUserChange = true;
		}

	@Override
	public void removePanel() {
		mTableModel.removeCompoundTableListener(this);
		super.removePanel();
		}

	@Override
	public String getInnerSettings() {
		if (isActive() && mComboBox.getItemCount() == 0)
			return null;

		String value = isActive() ? mTableModel.getCategoryList(mColumnIndex)[mSlider.getValue()]
								  : mTextFieldCategory.getText();
		return mComboBox.getSelectedItem()+"\t"+value;
		}

	@Override
	public void applyInnerSettings(String settings) {
		boolean found = false;
		int index = (settings == null) ? -1 : settings.indexOf('\t');
		if (index != -1) {
			String columnName = settings.substring(0, index);
			String category = settings.substring(index+1);
			if (isActive()) {
				int column = mTableModel.findColumn(columnName);
				if (column != -1 && mTableModel.isColumnTypeCategory(column)) {
					int value = mTableModel.getCategoryIndex(column, category);
					if (value == -1 || value > mSlider.getMaximum())
						value = 0;

					mComboBox.setSelectedItem(columnName);
					mSlider.setValue(value);
					found = true;
					}
				}
			else {
				mComboBox.setSelectedItem(columnName);
				mTextFieldCategory.setText(settings.substring(index+1));
				}
			}
		if (isActive()) {
			if (!found)
				setEnabled(false);
			else if (isEnabled())
				updateExclusion(false, false);
			}
		}

	@Override
	public void innerReset() {
		setEnabled(false);
		mSlider.setValue(0);
		}

	/**
	 * Populates combobox from existing category columns and selects mColumnIndex.
	 * If mColumnIndex is not a category column or -1, then it disables the filter
	 * ans selects the first item.
	 */
	private void updateComboBox() {
		mComboBox.removeActionListener(this);
		mComboBox.removeAllItems();
		boolean found = false;
		String[] columnList = getCategoryColumnList();
		for (int i=0; i<columnList.length; i++) {
			mComboBox.addItem(columnList[i]);
			if (mTableModel.findColumn(columnList[i]) == mColumnIndex) {
				mComboBox.setSelectedIndex(i);
				found = true;
				}
			}

		if (!found) {
			if (mComboBox.getItemCount() != 0) {
				mComboBox.setSelectedIndex(0);
				mColumnIndex = mTableModel.findColumn(columnList[0]);
				mSlider.setMaximum(mTableModel.getCategoryCount(mColumnIndex) - 1);
				mSlider.setValue(0);
				}
			else {
				mColumnIndex = -1;
				}
			setEnabled(false);
			}

		mComboBox.addActionListener(this);
		}

/*	private void disableSlider() {
		if (isActive()) {
			mSlider.removeChangeListener(this);
			mSlider.setValue(0);
			mSlider.setEnabled(false);
			mButtonLeft.setEnabled(false);
			mButtonRight.setEnabled(false);
			}
		}

	private void enableSlider() {
		if (isActive()) {
			mSlider.setMaximum(mTableModel.getCategoryCount(mColumnIndex)-1);
			mSlider.setEnabled(true);
			mSlider.addChangeListener(this);
			mButtonLeft.setEnabled(true);
			mButtonRight.setEnabled(true);
			}
		}*/

	private String[] getCategoryColumnList() {
		ArrayList<String> categoryColumnList = new ArrayList<String>();
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.isColumnTypeCategory(column))
				categoryColumnList.add(mTableModel.getColumnTitle(column));
		return categoryColumnList.toArray(new String[0]);
		}

	@Override
	public void updateExclusion(boolean isUserChange) {
		updateExclusion(false, isUserChange);
		}

	private void updateExclusion(boolean isAdjusting, boolean isUserChange) {
		if (!isActive() || !isEnabled())
			return;

		if (mColumnIndex < 0) {
			mSelectedItem = null;

			if (isActive())
				mTableModel.clearRowFlag(mExclusionFlag);
			}
		else {
			mSelectedItem = mTableModel.getCategoryList(mColumnIndex)[mSlider.getValue()];
			if (mTableModel.getColumnSpecialType(mColumnIndex) != null)
				setText(mTableModel.getColumnTitle(mColumnIndex)+" Group: "+(mSlider.getValue()+1), Color.red);
			else
				setText(mSelectedItem, Color.red);
			boolean[] selected = new boolean[mTableModel.getCategoryCount(mColumnIndex)];
			selected[mSlider.getValue()] = true;
	
			if (isActive())
				mTableModel.setCategoryExclusion(mExclusionFlag, mColumnIndex, selected, isInverse());
			}

		if (isUserChange)
			fireFilterChanged(FilterEvent.FILTER_UPDATED, isAdjusting);
		}

	@Override
	public int getFilterType() {
		return FILTER_TYPE_CATEGORY_BROWSER;
		}
	}
