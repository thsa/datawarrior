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

package com.actelion.research.datawarrior.action;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

import com.actelion.research.chem.*;
import com.actelion.research.table.CompoundTableLoader;
import com.actelion.research.table.model.CompoundTableModel;

public class DEAppendFileDialog extends JDialog
                                implements ActionListener,ItemListener,Runnable {
    static final long serialVersionUID = 0x20061005;
    private static final int IS_NOT_DISPLAYABLE = -1;
    private static final int IS_NORMAL_DISPLAYABLE = 0;

    private Frame               mParentFrame;
	private CompoundTableModel mTableModel;
	private JCheckBox			mCheckBoxNewColumn;
	private JComboBox			mComboBoxNewColumn;
	private JComboBox[]			mComboBoxList;
	private JTextField			mTextFieldOldSetName,mTextFieldNewSetName;
	private String				mOldSetName,mNewSetFileName;
	private CompoundTableLoader mLoader;
    private String[]            mVisibleFieldName,mTotalFieldName;

    public DEAppendFileDialog(Frame owner, CompoundTableModel tableModel,
							  String newSetFileName, CompoundTableLoader loader) {
		super(owner, "Define Column Mapping", true);
		mParentFrame = owner;
		mTableModel = tableModel;
        mNewSetFileName = newSetFileName;
		mLoader = loader;

            // Some methods called on mLoader sleep until file loading is done.
            // If these methods would be called from event dispatcher thread
            // then instantiating this dialog would block all UI updating.
        new Thread(this).start();
        }

    @SuppressWarnings("unchecked")
	private void showDialog() {
		mOldSetName = "Dataset 1";
		File file = mTableModel.getFile();
		if (file != null) {
			String oldSetFileName = file.getName();
            int index = oldSetFileName.lastIndexOf('.');
			mOldSetName = (index == -1) ? oldSetFileName
			                            : oldSetFileName.substring(0, index);
			}

		getContentPane().setLayout(new BorderLayout());

		int selectedIndex = 0;
		ArrayList<String> categoryColumnList = new ArrayList<String>();
		categoryColumnList.add("<new column>");
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.isColumnTypeCategory(column)) {
				String columnName = mTableModel.getColumnTitle(column);
				if (columnName.equals(CompoundTableLoader.DATASET_COLUMN_TITLE))
					selectedIndex = categoryColumnList.size();
				categoryColumnList.add(columnName);
				}

		mCheckBoxNewColumn = new JCheckBox("Write name of dataset into", true);
		mCheckBoxNewColumn.addItemListener(this);
		mComboBoxNewColumn = new JComboBox(categoryColumnList.toArray());
		mComboBoxNewColumn.setSelectedIndex(selectedIndex);
		mComboBoxNewColumn.addItemListener(this);
		JPanel tp1 = new JPanel();
		tp1.add(mCheckBoxNewColumn);
		tp1.add(mComboBoxNewColumn);

		if (selectedIndex == 0) {
			mTextFieldOldSetName = new JTextField(mOldSetName, 16);
			}
		else {
			mTextFieldOldSetName = new JTextField("N/A", 16);
			mTextFieldOldSetName.setEnabled(false);
			}
        int index = mNewSetFileName.lastIndexOf('.');
        String newSetName = (index == -1) ? mNewSetFileName
                                          : mNewSetFileName.substring(0, index);
		mTextFieldNewSetName = new JTextField(newSetName, 16);
		JPanel tp21 = new JPanel();
		tp21.setLayout(new GridLayout(2, 2, 4, 4));
		tp21.add(new JLabel("Existing dataset name:", SwingConstants.RIGHT));
		tp21.add(mTextFieldOldSetName);
		tp21.add(new JLabel("New dataset name:", SwingConstants.RIGHT));
		tp21.add(mTextFieldNewSetName);
		JPanel tp2 = new JPanel();
		tp2.add(tp21);

		JPanel tp = new JPanel();
		tp.setLayout(new BorderLayout());
		tp.add(tp1, BorderLayout.NORTH);
		tp.add(tp2, BorderLayout.CENTER);
		tp.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // create lists of potential mapping columns for these column types:
        // non-special and all parent special types (idcode, rxncode, <more?>)
        ArrayList<String>[] columnListBySpecialType = new ArrayList[1+CompoundTableModel.cParentSpecialColumnTypes.length];
        for (int i=0; i<=CompoundTableModel.cParentSpecialColumnTypes.length; i++) {
            columnListBySpecialType[i] = new ArrayList<String>();
            columnListBySpecialType[i].add("<new column>");
            columnListBySpecialType[i].add("<trash it>");
            }
        for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
            int typeIndex = getDisplayableType(mTableModel.getColumnSpecialType(column));
            if (typeIndex != IS_NOT_DISPLAYABLE)
                columnListBySpecialType[typeIndex].add(mTableModel.getColumnTitle(column));
            }

        mComboBoxList = new JComboBox[mVisibleFieldName.length];
		JPanel mp0 = new JPanel();
		mp0.setLayout(new GridLayout(mVisibleFieldName.length, 2, 4, 4));
		for (int i=0; i<mVisibleFieldName.length; i++) {
            int typeIndex = getDisplayableType(mLoader.getColumnSpecialType(mVisibleFieldName[i]));
            mComboBoxList[i] = new JComboBox(columnListBySpecialType[typeIndex].toArray());
            int column = mTableModel.findColumn(mVisibleFieldName[i]);	// this way we match also columns with alias in current data set
            for (int j=2; j<columnListBySpecialType[typeIndex].size(); j++)
                if (mVisibleFieldName[i].equalsIgnoreCase(columnListBySpecialType[typeIndex].get(j))
                 || (column != -1 && column == mTableModel.findColumn(columnListBySpecialType[typeIndex].get(j))))
                    mComboBoxList[i].setSelectedIndex(j);

            mp0.add(new JLabel("assign '"+mVisibleFieldName[i]+"' to ", SwingConstants.RIGHT));
			mp0.add(mComboBoxList[i]);
			}
		mp0.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
		JComponent mp = null;
		if (mVisibleFieldName.length > 5) {
			JScrollPane mps = new JScrollPane(mp0, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {
                static final long serialVersionUID = 0x20061005;
				public Dimension getPreferredSize() { return new Dimension(getViewport().getView().getPreferredSize().width+16, 156); }
				};
			mp = new JPanel();
			mp.add(mps);
			mp.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
			}
		else {
			mp = mp0;
			}

		JPanel bp = new JPanel();
		bp.setBorder(BorderFactory.createEmptyBorder(12, 8, 8, 8));
		bp.setLayout(new BorderLayout());
		JPanel ibp = new JPanel();
		ibp.setLayout(new GridLayout(1, 2, 8, 0));
		JButton bcancel = new JButton("Cancel");
		bcancel.addActionListener(this);
		ibp.add(bcancel);
		JButton bok = new JButton("OK");
		bok.addActionListener(this);
		ibp.add(bok);
		bp.add(ibp, BorderLayout.EAST);

		getContentPane().add(tp, BorderLayout.NORTH);
		getContentPane().add(mp, BorderLayout.CENTER);
		getContentPane().add(bp, BorderLayout.SOUTH);
		getRootPane().setDefaultButton(bok);

		pack();
		setLocationRelativeTo(mParentFrame);
		setVisible(true);
		}

    private int getDisplayableType(String specialType) {
        if (specialType == null)
            return IS_NORMAL_DISPLAYABLE;

        for (int i=0; i<CompoundTableModel.cParentSpecialColumnTypes.length; i++)
            if (specialType.equals(CompoundTableModel.cParentSpecialColumnTypes[i]))
                return i+1;

        return IS_NOT_DISPLAYABLE;
        }

    public void run() {
        mTotalFieldName = mLoader.getFieldNames();
        ArrayList<String> visibleFieldList = new ArrayList<String>();
        for (int i=0; i<mTotalFieldName.length; i++) {
            int typeIndex = getDisplayableType(mLoader.getColumnSpecialType(mTotalFieldName[i]));
            if (typeIndex != IS_NOT_DISPLAYABLE)
                visibleFieldList.add(mTotalFieldName[i]);
            }
        mVisibleFieldName = visibleFieldList.toArray(new String[0]);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                showDialog();
                }
            } );
        }

    public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("OK")) {
			if (mCheckBoxNewColumn.isSelected()) {
				if (mTextFieldNewSetName.getText().length() == 0) {
					JOptionPane.showMessageDialog(mParentFrame, "You didn't give a name for the new dataset.");
					return;
					}
			 	if (mComboBoxNewColumn.getSelectedIndex() == 0
				 && mTextFieldOldSetName.getText().length() == 0) {
					JOptionPane.showMessageDialog(mParentFrame, "You didn't give a name for the existing dataset.");
					return;
					}
				}

			SortedStringList usedColumnList = new SortedStringList();
			if (mCheckBoxNewColumn.isSelected()
			 && mComboBoxNewColumn.getSelectedIndex() != 0)
				usedColumnList.addString((String)mComboBoxNewColumn.getSelectedItem());
			for (int i=0; i<mComboBoxList.length; i++) {
				if (mComboBoxList[i].getSelectedIndex() > 1
				 && usedColumnList.addString((String)mComboBoxList[i].getSelectedItem()) == -1) {
					JOptionPane.showMessageDialog(mParentFrame, "Column '"+mComboBoxList[i].getSelectedItem()+" is used twice.");
					return;
					}
				}
			}

		setVisible(false);
		dispose();	//

		if (e.getActionCommand().equals("OK")) {
            int[] visibleDestColumn = new int[mVisibleFieldName.length];
            for (int i=0; i<mVisibleFieldName.length; i++) {
                switch (mComboBoxList[i].getSelectedIndex()) {
                case 0:
                    visibleDestColumn[i] = CompoundTableLoader.NEW_COLUMN;
                    break;
                case 1:
                    visibleDestColumn[i] = CompoundTableLoader.NO_COLUMN;
                    break;
                default:
                    visibleDestColumn[i] = mTableModel.findColumn((String)mComboBoxList[i].getSelectedItem());
                    break;
                    }
                }

            int visIndex = 0;
            int[] destColumn = new int[mTotalFieldName.length];
            for (int i=0; i<mTotalFieldName.length; i++) {
                String specialType = mLoader.getColumnSpecialType(mTotalFieldName[i]);
                if (getDisplayableType(specialType) == IS_NOT_DISPLAYABLE) {
                    destColumn[i] = CompoundTableLoader.NO_COLUMN;  // just in case parent column cannot be found
                    String parentColumn = mLoader.getParentColumnName(mTotalFieldName[i]);
                    for (int j=0; j<mVisibleFieldName.length; j++) {
                        if (mVisibleFieldName[j].equals(parentColumn)) {
                            if (visibleDestColumn[j] == CompoundTableLoader.NEW_COLUMN
                             || visibleDestColumn[j] == CompoundTableLoader.NO_COLUMN)
                                destColumn[i] = visibleDestColumn[j];
                            else {
                                int column = mTableModel.getChildColumn(visibleDestColumn[j], specialType);
                                if (column != -1)
                                    destColumn[i] = column;
                                else
                                    destColumn[i] = CompoundTableLoader.NEW_COLUMN;
                                }
                            }
                        }
                    }
                else {
                    destColumn[i] = visibleDestColumn[visIndex++];
                    }
                }

			int datasetColumn = CompoundTableLoader.NO_COLUMN;
			if (mCheckBoxNewColumn.isSelected())
				datasetColumn = (mComboBoxNewColumn.getSelectedIndex() == 0) ?
						CompoundTableLoader.NEW_COLUMN : mTableModel.findColumn((String)mComboBoxNewColumn.getSelectedItem());

			mLoader.appendFile(destColumn, datasetColumn, mTextFieldOldSetName.getText(), mTextFieldNewSetName.getText());
			}
		}

	public void itemStateChanged(ItemEvent e) {
		boolean enabled = mCheckBoxNewColumn.isSelected();
		mComboBoxNewColumn.setEnabled(enabled);
		mTextFieldNewSetName.setEnabled(enabled);
		boolean fieldOldSetNameEnabled = enabled && (mComboBoxNewColumn.getSelectedIndex() == 0);
		if (fieldOldSetNameEnabled != mTextFieldOldSetName.isEnabled()) {
			mTextFieldOldSetName.setEnabled(fieldOldSetNameEnabled);
			if (fieldOldSetNameEnabled)
				mTextFieldOldSetName.setText(mOldSetName);
			else {
				mOldSetName = mTextFieldOldSetName.getText();
				mTextFieldOldSetName.setText("<N/A>");
				}
			}
		}
	}