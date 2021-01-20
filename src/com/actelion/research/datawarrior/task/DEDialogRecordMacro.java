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

package com.actelion.research.datawarrior.task;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEMacroEditor;
import com.actelion.research.datawarrior.DEMainPane;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * Created by thomas on 1/12/17.
 */
public class DEDialogRecordMacro extends JDialog implements ActionListener {
	private DEFrame			mDEFrame;
	private JRadioButton	mRadioButtonExtendExisting,mRadioButtonStartNew;
	private JTextField		mTextFieldMacroName;
	private JComboBox		mComboBoxExistingMacro;

	public DEDialogRecordMacro(DEFrame parent) {
		super(parent, "Start Recording A Macro", true);

		mDEFrame = parent;

		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
				{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 16,
					TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		mRadioButtonStartNew = new JRadioButton("Create new macro", true);
		mRadioButtonExtendExisting = new JRadioButton("Extend Existing macro", false);
		mRadioButtonStartNew.addActionListener(this);
		mRadioButtonExtendExisting.addActionListener(this);
		ButtonGroup bg = new ButtonGroup();
		bg.add(mRadioButtonStartNew);
		bg.add(mRadioButtonExtendExisting);
		content.add(mRadioButtonStartNew, "1,1,3,1");
		content.add(mRadioButtonExtendExisting, "1,5,3,5");

		mTextFieldMacroName = new JTextField("Untitled Macro", 10);
		content.add(new JLabel("New macro name:"), "1,3");
		content.add(mTextFieldMacroName, "3,3");

		mComboBoxExistingMacro = new JComboBox();
		ArrayList<DEMacro> macroList = (ArrayList<DEMacro>)mDEFrame.getTableModel().getExtensionData(CompoundTableConstants.cExtensionNameMacroList);
		if (macroList != null && macroList.size() != 0) {
			for (DEMacro macro : macroList)
				mComboBoxExistingMacro.addItem(macro.getName());
			}
		else {
			mRadioButtonExtendExisting.setEnabled(false);
			}
		mComboBoxExistingMacro.setEnabled(false);
		content.add(new JLabel("Existing Macro:"), "1,7");
		content.add(mComboBoxExistingMacro, "3,7");

		JPanel bp = new JPanel();
		bp.setLayout(new BorderLayout());
		bp.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
		JPanel ibp = new JPanel();
		ibp.setLayout(new GridLayout(1, 2, 8, 8));
		JButton bcancel = new JButton("Cancel");
		bcancel.addActionListener(this);
		ibp.add(bcancel);
		JButton bok = new JButton("OK");
		bok.addActionListener(this);
		ibp.add(bok);
		bp.add(ibp, BorderLayout.EAST);

		p.add(content, BorderLayout.CENTER);
		p.add(bp, BorderLayout.SOUTH);
		getContentPane().add(p);

		getRootPane().setDefaultButton(bok);

		pack();
		setLocationRelativeTo(parent);
		setVisible(true);
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mRadioButtonStartNew) {
			mComboBoxExistingMacro.setEnabled(!mRadioButtonStartNew.isSelected());
			return;
			}
		if (e.getSource() == mRadioButtonExtendExisting) {
			mComboBoxExistingMacro.setEnabled(mRadioButtonExtendExisting.isSelected());
			return;
			}
		if (e.getActionCommand().equals("OK")) {
			if (mRadioButtonStartNew.isSelected()) {
				String macroName = mTextFieldMacroName.getText();
				if (macroName.length() == 0) {
					JOptionPane.showMessageDialog(mDEFrame, "You need to give a name for the macro.", "Start Recording A Macro", JOptionPane.WARNING_MESSAGE);
					return;
					}

				DEMacro macro = DEMacroEditor.addNewMacro(mDEFrame, macroName, null);

				boolean frameIsEmpty = mDEFrame.getTableModel().isEmpty() && mDEFrame.getMainFrame().getMainPane().getDockableCount() == 0;
				if (frameIsEmpty)
					mDEFrame.getMainFrame().getMainPane().addApplicationView(DEMainPane.VIEW_TYPE_MACRO_EDITOR, "Macro Editor", "root");

				DEMacroRecorder.getInstance().startRecording(macro, mDEFrame);
				}
			else {
				String macroName = (String)mComboBoxExistingMacro.getSelectedItem();
				ArrayList<DEMacro> macroList = (ArrayList<DEMacro>)mDEFrame.getTableModel().getExtensionData(CompoundTableConstants.cExtensionNameMacroList);
				for (DEMacro macro:macroList) {
					if (macro.getName().equals(macroName)) {
						DEMacroRecorder.getInstance().startRecording(macro, mDEFrame);
						break;
						}
					}
				}
			}
		setVisible(false);
		dispose();
		}
	}
