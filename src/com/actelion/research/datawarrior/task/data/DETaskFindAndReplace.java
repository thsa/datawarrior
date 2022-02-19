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

package com.actelion.research.datawarrior.task.data;

import com.actelion.research.chem.*;
import com.actelion.research.chem.coords.CoordinateInventor;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DETable;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.JEditableStructureView;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Properties;


public class DETaskFindAndReplace extends ConfigurableTask implements ActionListener,Runnable {
	public static final long serialVersionUID = 0x20130131;

	public static final String TASK_NAME = "Find And Replace";

	private static final String PROPERTY_COLUMN = "column";
	private static final String PROPERTY_WHAT = "what";
	private static final String PROPERTY_WITH = "with";
	private static final String PROPERTY_IS_REGEX = "isRegex";
	private static final String PROPERTY_IS_STRUCTURE = "isStructure";
	private static final String PROPERTY_CASE_SENSITIVE = "caseSensitive";
	private static final String PROPERTY_ALLOW_SUBSTITUENTS = "allowSubstituents";
	private static final String PROPERTY_MODE = "mode";
	
	private static final String OPTION_ANY_COLUMN = "<any column>";
	private static final String OPTION_VISIBLE_COLUMN = "<visible columns>";
	private static final String OPTION_SELECTED_COLUMN = "<selected columns>";
	private static final String CODE_ANY_COLUMN = "<any>";
	private static final String CODE_VISIBLE_COLUMN = "<visible>";
	private static final String CODE_SELECTED_COLUMN = "<selected>";

	private static final int cColumnAny = -1;
	private static final int cColumnVisible = -2;
	private static final int cColumnSelected = -3;

	private static final int cModeAllRows = 0;
	private static final int cModeSelectedOnly = 1;
	private static final int cModeVisibleOnly = 2;

	private static final String[] WHAT_NAME = { "Find this:", "Find regex:", "Find empty", "Find any" };
	private static final int WHAT_THIS = 0;
	private static final int WHAT_REGEX = 1;
	private static final int WHAT_EMPTY = 2;
	private static final int WHAT_ANY = 3;
	private static final String CODE_WHAT_EMPTY = "<empty>";
	private static final String CODE_WHAT_ANY = "<any>";

	public static final String[] MODE_NAME = { "All rows", "Selected rows", "Visible rows" };
	public static final String[] MODE_CODE = { "all", "selected", "visible" };

	private DEFrame				mParentFrame;
	private CompoundTableModel	mTableModel;
	private JPanel				mDialogPanel;
	private JTextField			mTextFieldWhat,mTextFieldWith;
	private JEditableStructureView	mStructureFieldWhat,mStructureFieldWith;
	private JComboBox			mComboBoxColumn,mComboBoxWhat,mComboBoxMode;
	private JCheckBox			mCheckBoxIsStructureColumn,mCheckBoxCaseSensitive,mCheckBoxAllowSubstituents;
	private JLabel				mLabelUseRGroups;
	private JPanel				mCheckBoxPanel;
	private boolean				mIsStructureMode;
	private int                 mColumns,mReplacements;

	public DETaskFindAndReplace(DEFrame owner) {
		super(owner, true);
		mParentFrame = owner;
		mTableModel = mParentFrame.getTableModel();
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/chemistry.html#FindReplace";
		}

	@Override
	public boolean isConfigurable() {
		if (mTableModel.getTotalRowCount() != 0)
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
				if (qualifiesAsColumn(column))
					return true;

		showErrorMessage("Search and replace requires a column with\nalphanumerical content or with chemical structures.");
		return false;
		}

	@Override
	public JPanel createDialogContent() {
		mComboBoxColumn = new JComboBox();
		mComboBoxColumn.addItem(OPTION_ANY_COLUMN);
		mComboBoxColumn.addItem(OPTION_VISIBLE_COLUMN);
		mComboBoxColumn.addItem(OPTION_SELECTED_COLUMN);
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (qualifiesAsColumn(column))
				mComboBoxColumn.addItem(mTableModel.getColumnTitle(column));
		mComboBoxColumn.setEditable(!isInteractive());
		mComboBoxColumn.addActionListener(this);

		mComboBoxWhat = new JComboBox(WHAT_NAME);
		mComboBoxWhat.addActionListener(this);

		mTextFieldWhat = new JTextField(16);
		mTextFieldWith = new JTextField(16);
		mCheckBoxCaseSensitive = new JCheckBox("Case sensitive");
		if (!isInteractive()) {
			mCheckBoxIsStructureColumn = new JCheckBox("Is chemical structure");
			mCheckBoxIsStructureColumn.addActionListener(this);
			}

		mDialogPanel = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 4,
								TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8} };
		mDialogPanel.setLayout(new TableLayout(size));

		mDialogPanel.add(new JLabel("Column:", SwingConstants.RIGHT), "1,1");
		mDialogPanel.add(mComboBoxColumn, "3,1");
		if (mCheckBoxIsStructureColumn != null)
			mDialogPanel.add(mCheckBoxIsStructureColumn, "3,3");
		mDialogPanel.add(mComboBoxWhat, "1,5");
		mDialogPanel.add(mTextFieldWhat, "3,5");
		mDialogPanel.add(new JLabel("Replace with:"), "1,7");
		mDialogPanel.add(mTextFieldWith, "3,7");
		mDialogPanel.add(mCheckBoxCaseSensitive, "3,9");

		mComboBoxMode = new JComboBox(MODE_NAME);
		mDialogPanel.add(new JLabel("Target:", SwingConstants.RIGHT), "1,13");
		mDialogPanel.add(mComboBoxMode, "3,13");

		mIsStructureMode = false;

		return mDialogPanel;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mCheckBoxIsStructureColumn) {
			updateInputFields(mCheckBoxIsStructureColumn.isSelected());
			return;
			}
		if (isInteractive() && e.getSource() == mComboBoxColumn) {
			int column = mTableModel.findColumn((String)mComboBoxColumn.getSelectedItem());
			boolean isStructure = (column < 0) ? false : mTableModel.isColumnTypeStructure(column);
			updateInputFields(isStructure);
			}
		if (e.getSource() == mComboBoxWhat) {
			enableWhatFields();
			}
		}

	private void enableWhatFields() {
		boolean enabled = (mComboBoxWhat.getSelectedIndex() == WHAT_THIS || mComboBoxWhat.getSelectedIndex() == WHAT_REGEX);
		if (mTextFieldWhat != null)
			mTextFieldWhat.setEnabled(enabled);
		if (mStructureFieldWhat != null)
			mStructureFieldWhat.setEnabled(enabled);
		if (mCheckBoxCaseSensitive != null)
			mCheckBoxCaseSensitive.setEnabled(mComboBoxWhat.getSelectedIndex() == WHAT_THIS);
		}

	private void updateInputFields(boolean isStructure) {
		if (mCheckBoxIsStructureColumn != null
		 && mCheckBoxIsStructureColumn.isSelected() != isStructure) {
			mCheckBoxIsStructureColumn.removeActionListener(this);
			mCheckBoxIsStructureColumn.setSelected(isStructure);
			mCheckBoxIsStructureColumn.addActionListener(this);
			}

		if (isStructure && !mIsStructureMode) {
			if (mStructureFieldWhat == null) {
				Dimension editorSize = new Dimension(mTextFieldWhat.getPreferredSize().width, 100);
				mStructureFieldWhat = new JEditableStructureView();
				mStructureFieldWhat.getMolecule().setFragment(true);
				mStructureFieldWhat.setPreferredSize(editorSize);
				mStructureFieldWhat.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2));
				mStructureFieldWhat.setEnabled(mComboBoxWhat.getSelectedIndex() == WHAT_THIS);
				mStructureFieldWith = new JEditableStructureView();
				mStructureFieldWith.setPreferredSize(editorSize);
				mStructureFieldWith.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2));
				mLabelUseRGroups = new JLabel("Use R-groups (R1, R2, ...) to define links.", JLabel.CENTER);
				mCheckBoxAllowSubstituents = new JCheckBox("Allow and replace not drawn substituents");
				mCheckBoxPanel = new JPanel();
				mCheckBoxPanel.add(mCheckBoxAllowSubstituents);
				}
//			String what = mTextFieldWhat.getText();
//			String with = mTextFieldWith.getText();
			mDialogPanel.remove(mTextFieldWhat);
			mDialogPanel.remove(mTextFieldWith);
			mDialogPanel.remove(mCheckBoxCaseSensitive);
			mDialogPanel.add(mStructureFieldWhat, "3,5");
			mDialogPanel.add(mStructureFieldWith, "3,7");
			mDialogPanel.add(mLabelUseRGroups, "1,9,3,9");
			mDialogPanel.add(mCheckBoxPanel, "1,11,3,11");
			mDialogPanel.validate();
			getDialog().pack();
/*			if (what.length() != 0) {
				try {
					new IDCodeParser().parse(mStructureFieldWhat.getMolecule(), what);
					mStructureFieldWhat.structureChanged();
					}
				catch (Exception e) {}
				}
			if (with.length() != 0) {
				try {
					new IDCodeParser().parse(mStructureFieldWith.getMolecule(), with);
					mStructureFieldWith.structureChanged();
					}
				catch (Exception e) {}
				}
*/			mIsStructureMode = true;
//			mCheckBoxCaseSensitive.setEnabled(false);
			return;
			}

		if (!isStructure && mIsStructureMode) {
//			StereoMolecule whatMol = mStructureFieldWhat.getMolecule();
//			StereoMolecule withMol = mStructureFieldWith.getMolecule();
//			String what = (whatMol.getAllAtoms() == 0) ? "" : new Canonizer(whatMol).getIDCode();
//			String with = (withMol.getAllAtoms() == 0) ? "" : new Canonizer(withMol).getIDCode();
			mDialogPanel.remove(mStructureFieldWhat);
			mDialogPanel.remove(mStructureFieldWith);
			mDialogPanel.remove(mLabelUseRGroups);
			mDialogPanel.remove(mCheckBoxPanel);
			mDialogPanel.add(mTextFieldWhat, "3,5");
			mDialogPanel.add(mTextFieldWith, "3,7");
			mDialogPanel.add(mCheckBoxCaseSensitive, "3,9");
			mDialogPanel.validate();
			getDialog().pack();
//			mTextFieldWhat.setText(what);
//			mTextFieldWith.setText(with);
			mIsStructureMode = false;
//			mCheckBoxCaseSensitive.setEnabled(true);
			return;
			}
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		String column = OPTION_ANY_COLUMN.equals(mComboBoxColumn.getSelectedItem()) ? CODE_ANY_COLUMN
					: OPTION_VISIBLE_COLUMN.equals(mComboBoxColumn.getSelectedItem()) ? CODE_VISIBLE_COLUMN
					: OPTION_SELECTED_COLUMN.equals(mComboBoxColumn.getSelectedItem()) ? CODE_SELECTED_COLUMN
					: mTableModel.getColumnTitleNoAlias(mTableModel.findColumn((String)mComboBoxColumn.getSelectedItem()));
		configuration.setProperty(PROPERTY_COLUMN, column);

		configuration.setProperty(PROPERTY_IS_STRUCTURE, mIsStructureMode ? "true" : "false");

		if (mIsStructureMode) {
			StereoMolecule whatMol = mStructureFieldWhat.getMolecule();
			String what = (mComboBoxWhat.getSelectedIndex() == WHAT_ANY) ? CODE_WHAT_ANY
						: (mComboBoxWhat.getSelectedIndex() == WHAT_EMPTY) ? CODE_WHAT_EMPTY
						: (whatMol.getAllAtoms() == 0) ? "" : new Canonizer(whatMol).getIDCode();
			if (what.length() != 0)
				configuration.setProperty(PROPERTY_WHAT, what);

			StereoMolecule withMol = mStructureFieldWith.getMolecule();
			String with = (withMol.getAllAtoms() == 0) ? "" : new Canonizer(withMol).getIDCode();

			configuration.setProperty(PROPERTY_WITH, with);

			configuration.setProperty(PROPERTY_ALLOW_SUBSTITUENTS, mCheckBoxAllowSubstituents.isSelected() ? "true" : "false");
			}
		else {
			String what = (mComboBoxWhat.getSelectedIndex() == WHAT_ANY) ? CODE_WHAT_ANY
					    : (mComboBoxWhat.getSelectedIndex() == WHAT_EMPTY) ? CODE_WHAT_EMPTY
						: mTextFieldWhat.getText();
			if (what.length() != 0) {
				if (mComboBoxWhat.getSelectedIndex() == WHAT_REGEX)
					configuration.setProperty(PROPERTY_IS_REGEX, "true");
				configuration.setProperty(PROPERTY_WHAT, what);
				}

			configuration.setProperty(PROPERTY_WITH, mTextFieldWith.getText());
			}

		if (mCheckBoxCaseSensitive.isSelected())
			configuration.setProperty(PROPERTY_CASE_SENSITIVE, "true");

		if (mComboBoxMode.getSelectedIndex() != cModeAllRows)
			configuration.setProperty(PROPERTY_MODE, MODE_CODE[mComboBoxMode.getSelectedIndex()]);

		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String value = configuration.getProperty(PROPERTY_COLUMN);
		if (value == null || value.equals(CODE_ANY_COLUMN)) {
			mComboBoxColumn.setSelectedItem(OPTION_ANY_COLUMN);
			}
		else if (value.equals(CODE_VISIBLE_COLUMN)) {
			mComboBoxColumn.setSelectedItem(OPTION_VISIBLE_COLUMN);
			}
		else if (value.equals(CODE_SELECTED_COLUMN)) {
			mComboBoxColumn.setSelectedItem(OPTION_SELECTED_COLUMN);
			}
		else {
			int column = mTableModel.findColumn(value);
			if (column != -1 && qualifiesAsColumn(column))
				mComboBoxColumn.setSelectedItem(mTableModel.getColumnTitle(column));
			}

		String what = configuration.getProperty(PROPERTY_WHAT, "");
		String with = configuration.getProperty(PROPERTY_WITH, "");
		if ("true".equals(configuration.getProperty(PROPERTY_IS_STRUCTURE))) {
			updateInputFields(true);
			if (what.length() == 0) {
				mStructureFieldWhat.getMolecule().clear();
				}
			else {
				if (what.equals(CODE_WHAT_ANY))
					mComboBoxWhat.setSelectedIndex(WHAT_ANY);
				else if (what.equals(CODE_WHAT_EMPTY))
					mComboBoxWhat.setSelectedIndex(WHAT_EMPTY);
				else {
					mComboBoxWhat.setSelectedIndex(WHAT_THIS);
					try {
						new IDCodeParser().parse(mStructureFieldWhat.getMolecule(), what);
						mStructureFieldWhat.structureChanged();
						} catch (Exception e) {}
					}
				}
			if (with.length() == 0) {
				mStructureFieldWith.getMolecule().clear();
				}
			else {
				try {
					new IDCodeParser().parse(mStructureFieldWith.getMolecule(), with);
					mStructureFieldWith.structureChanged();
					}
				catch (Exception e) {}
				}
			mCheckBoxAllowSubstituents.setSelected("true".equals(configuration.getProperty(PROPERTY_ALLOW_SUBSTITUENTS)));
			}
		else {
			updateInputFields(false);
			if (what.equals(CODE_WHAT_ANY))
				mComboBoxWhat.setSelectedIndex(WHAT_ANY);
			else if (what.equals(CODE_WHAT_EMPTY))
				mComboBoxWhat.setSelectedIndex(WHAT_EMPTY);
			else {
				mComboBoxWhat.setSelectedIndex("true".equals(configuration.getProperty(PROPERTY_IS_REGEX)) ?
						WHAT_REGEX : WHAT_EMPTY);
				mTextFieldWhat.setText(what);
				}
			mTextFieldWith.setText(with);
			}

		value = configuration.getProperty(PROPERTY_CASE_SENSITIVE);
		mCheckBoxCaseSensitive.setSelected("true".equals(value));

		mComboBoxMode.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_MODE), MODE_CODE, 0));

		enableWhatFields();
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		boolean isStructureMode = "true".equals(configuration.getProperty(PROPERTY_IS_STRUCTURE));
		String what = configuration.getProperty(PROPERTY_WHAT, "");
		if (what.length() == 0) {
			showErrorMessage(isStructureMode ? "No structure defined." : "No search string defined.");
			return false;
			}
		if (what.contains("\t")) {
			showErrorMessage("TAB is not allowed in replace string.");
			return false;
			}
		if (isStructureMode) {
			String with = configuration.getProperty(PROPERTY_WITH, "");

			StereoMolecule withMol;
			if (with.length() == 0) {
				withMol = new StereoMolecule(0,0);
				}
			else {
				try {
					withMol = new IDCodeParser().getCompactMolecule(with);
					}
				catch (Exception e) {
					showErrorMessage("Invalid idcode of replacement structure.");
					return false;
					}
				}

			if (!what.equals(CODE_WHAT_EMPTY) && !what.equals(CODE_WHAT_ANY)) {
				boolean[] rGroupUsed = new boolean[16];
				int count = 0;
				try {
					StereoMolecule whatMol = new IDCodeParser().getCompactMolecule(what);
					whatMol.ensureHelperArrays(Molecule.cHelperNeighbours);
					for (int atom=0; atom<whatMol.getAllAtoms(); atom++) {
						int atomicNo = whatMol.getAtomicNo(atom);
						if (atomicNo > 128 && atomicNo <= 144) {
							if (rGroupUsed[atomicNo - 129]) {
								showErrorMessage("Duplicate R-groups used in search structure.");
								return false;
								}
							if (whatMol.getConnAtoms(atom) != 1) {
								showErrorMessage("R-groups must have exactly one neighbor atom.");
								return false;
								}
							rGroupUsed[atomicNo - 129] = true;
							count++;
							}
						}
	/*				if (count == 0) {
						showErrorMessage("No R-groups (R1,R2,...) defined in search structure.");
						return false;
						} we allow the replacement of separated fragments	*/
					}
				catch (Exception e) {
					showErrorMessage("Invalid idcode of search structure.");
					return false;
					}

				withMol.ensureHelperArrays(Molecule.cHelperNeighbours);
				for (int atom=0; atom<withMol.getAllAtoms(); atom++) {
					int atomicNo = withMol.getAtomicNo(atom);
					if (atomicNo > 128 && atomicNo <= 144) {
						if (!rGroupUsed[atomicNo - 129]) {
							showErrorMessage("R-groups in search and replacement structures must match.");
							return false;
							}
						if (withMol.getConnAtoms(atom) > 1) {	// we allow replacement mit hydrogen (conns==0)
							showErrorMessage("R-groups must have exactly one neighbor atom.");
							return false;
							}
						rGroupUsed[atomicNo - 129] = false;
						count--;
						}
					}
				if (count != 0) {
					showErrorMessage("Different number of R-groups used in search and replacement structures.");
					return false;
					}
				}
			}

		String columnName = configuration.getProperty(PROPERTY_COLUMN);
		if (isStructureMode) {
			if (columnName.equals(CODE_ANY_COLUMN)) {
				showErrorMessage("Substructure replacement cannot be combined with '<any column>'.");
				return false;
				}
			if (columnName.equals(CODE_VISIBLE_COLUMN)) {
				showErrorMessage("Substructure replacement cannot be combined with '<visible columns>'.");
				return false;
				}
			if (columnName.equals(CODE_SELECTED_COLUMN)) {
				showErrorMessage("Substructure replacement cannot be combined with '<selected columns>'.");
				return false;
				}
			}

		if (isLive) {
			if (columnName.equals(CODE_VISIBLE_COLUMN)) {
				DETable table = mParentFrame.getMainFrame().getMainPane().getTable();
				if (table.getColumnModel().getColumnCount() == 0) {
					showErrorMessage("No visible columns found.");
					return false;
					}
				}
			else if (columnName.equals(CODE_SELECTED_COLUMN)) {
				DETable table = mParentFrame.getMainFrame().getMainPane().getTable();
				if (table.getColumnModel().getSelectedColumnCount() == 0) {
					showErrorMessage("No selected columns found.");
					return false;
					}
				}
			else if (!columnName.equals(CODE_ANY_COLUMN)) {
				int column = mTableModel.findColumn(columnName);
				if (column == -1) {
					showErrorMessage("Column '"+columnName+"' not found.");
					return false;
					}
				if (!qualifiesAsColumn(column)) {
					showErrorMessage("Column '"+columnName+"' is not alphanumerical.");
					return false;
					}
				if (mTableModel.isColumnTypeStructure(column)) {
					if (!isStructureMode) {
						showErrorMessage("Text replacement cannot be done on '"+columnName+"', which is a structure column.");
						return false;
						}
					}
				else {
					if (isStructureMode) {
						showErrorMessage("Substructure replacement cannot be done on '"+columnName+"', which is a text column.");
						return false;
						}
					}
				}
			int mode = findListIndex(configuration.getProperty(PROPERTY_MODE), MODE_CODE, 0);
			if (mode == cModeVisibleOnly && mTableModel.getRowCount() == 0) {
				showErrorMessage("There are no visible rows.");
				return false;
				}
			if (mode == cModeSelectedOnly
			 && mParentFrame.getMainFrame().getMainPane().getTable().getSelectionModel().isSelectionEmpty()) {
				showErrorMessage("There are no selected rows.");
				return false;
				}
			}

		return true;
		}

	@Override
	public void setDialogConfigurationToDefault() {
		int selectedColumn = -1;
		if (isInteractive()) {
			DETable table = mParentFrame.getMainFrame().getMainPane().getTable();
			if (table.getSelectedRow() != -1) {
				int selectedColumns = table.getSelectedColumnCount();
				if (selectedColumns == 1)
					selectedColumn = table.convertTotalColumnIndexFromView(table.getSelectedColumn());
				else if (selectedColumns > 1 && selectedColumns < table.getColumnCount())
					selectedColumn = -2;
				}
			}

		if (selectedColumn == -1)
			mComboBoxColumn.setSelectedItem(OPTION_VISIBLE_COLUMN);
		else if (selectedColumn == -2)
			mComboBoxColumn.setSelectedItem(OPTION_SELECTED_COLUMN);
		else
			mComboBoxColumn.setSelectedItem(mTableModel.getColumnTitle(selectedColumn));

		mTextFieldWhat.setText("");
		mTextFieldWith.setText("");
		mCheckBoxCaseSensitive.setSelected(false);
		mComboBoxWhat.setSelectedIndex(WHAT_THIS);
		mComboBoxMode.setSelectedIndex(selectedColumn == -1 ? cModeAllRows : cModeSelectedOnly);

		enableWhatFields();
		}

	private boolean qualifiesAsColumn(int column) {
		return (mTableModel.isColumnTypeStructure(column)
			 || (mTableModel.getColumnSpecialType(column) == null
			  && (mTableModel.isColumnTypeString(column)
			   || mTableModel.isColumnTypeDouble(column)
			   || mTableModel.isColumnTypeCategory(column))
			  && !mTableModel.isColumnTypeRangeCategory(column)));
		}

	@Override
	public void runTask(Properties configuration) {
		String what = configuration.getProperty(PROPERTY_WHAT, "").replace("\\n", "\n");
		String with = configuration.getProperty(PROPERTY_WITH, "").replace("\\n", "\n");
		String value = configuration.getProperty(PROPERTY_COLUMN);
		int targetColumn = (value == null || value.equals(CODE_ANY_COLUMN)) ? cColumnAny
				: value.equals(CODE_VISIBLE_COLUMN) ? cColumnVisible
				: value.equals(CODE_SELECTED_COLUMN) ? cColumnSelected
				: mTableModel.findColumn(value);
		int mode = findListIndex(configuration.getProperty(PROPERTY_MODE), MODE_CODE, 0);

		if ("true".equals(configuration.getProperty(PROPERTY_IS_STRUCTURE))) {
			if (what.equals(CODE_WHAT_ANY) || what.equals(CODE_WHAT_EMPTY))
				replaceStructures(what, with, targetColumn, mode);
			else {
				boolean allowSubstituents = "true".equals(configuration.getProperty(PROPERTY_ALLOW_SUBSTITUENTS));
				replaceSubStructures(what, with, targetColumn, mode, allowSubstituents);
				}
			}
		else {
			boolean isRegex = "true".equals(configuration.getProperty(PROPERTY_IS_REGEX));
			boolean isCaseSensitive = "true".equals(configuration.getProperty(PROPERTY_CASE_SENSITIVE));
			replaceText(what, with, targetColumn, isRegex, isCaseSensitive, mode);
			}
		}

	private void replaceSubStructures(String what, String with, int column, int mode, boolean allowSubstituents) {
		StereoMolecule mol = null;
		StereoMolecule whatMol = null;
		StereoMolecule withMol = null;
		Link[] link = null;
		mol = new StereoMolecule();
		whatMol = new IDCodeParser().getCompactMolecule(what);
		withMol = (with.length() == 0) ? new StereoMolecule(0,0) : new IDCodeParser().getCompactMolecule(with);
		materializeImplicitHydrogensOnLinkAtoms(withMol);
		whatMol.ensureHelperArrays(Molecule.cHelperNeighbours);
		withMol.ensureHelperArrays(Molecule.cHelperParities);
		ArrayList<Link> linkList = new ArrayList<Link>();
		for (int atom1=0; atom1<whatMol.getAllAtoms(); atom1++) {
			int atomicNo = whatMol.getAtomicNo(atom1);
			if (atomicNo > 128 && atomicNo <= 144) {
				for (int atom2=0; atom2<withMol.getAllAtoms(); atom2++) {
					if (withMol.getAtomicNo(atom2) == atomicNo) {
						whatMol.setAtomQueryFeature(atom1, Molecule.cAtomQFAny, true);
						whatMol.setAtomMarker(atom1, true);
						int newStereoCenter = -1;
						int newParity = 0;
						if (withMol.getConnAtoms(atom2) != 0
						 && (withMol.getAtomParity(withMol.getConnAtom(atom2, 0)) == Molecule.cAtomParity1
						  || withMol.getAtomParity(withMol.getConnAtom(atom2, 0)) == Molecule.cAtomParity2)) {
							newStereoCenter = withMol.getConnAtom(atom2, 0);
							boolean inversion = false;
							for (int i=0; i<withMol.getConnAtoms(newStereoCenter); i++)
								if (atom2 > withMol.getConnAtom(newStereoCenter, i))
									inversion = !inversion;
							newParity = withMol.getAtomParity(newStereoCenter);
							if (inversion)
								newParity = (newParity == Molecule.cAtomParity1) ?
										Molecule.cAtomParity2 : Molecule.cAtomParity1;
							}

						linkList.add(new Link(atom1, whatMol.getConnAtom(atom1, 0), whatMol.getConnBondOrder(atom1, 0),
											  atom2, newStereoCenter, newParity,
											  newStereoCenter == -1 ? false : withMol.isAtomParityPseudo(newStereoCenter)));
						break;
						}
					}
				}
			else {
				if (!allowSubstituents)
					whatMol.setAtomQueryFeature(atom1, Molecule.cAtomQFNoMoreNeighbours, true);
				}
			}
		if (linkList.size() != 0) {
			withMol.ensureHelperArrays(Molecule.cHelperBitNeighbours);
			for (int i=0; i<linkList.size(); i++) {
				Link l = linkList.get(i);
				int bond = withMol.getConnBond(l.replaceAtom, 0);
				withMol.markAtomForDeletion(l.replaceAtom);
				withMol.markBondForDeletion(bond);
				l.setNewBondOrder(withMol.getConnBondOrder(l.replaceAtom, 0));
				l.replaceAtom = withMol.getConnAtom(l.replaceAtom, 0);
				}
			int[] atomMap = withMol.deleteMarkedAtomsAndBonds();
			for (Link l:linkList) {
				l.replaceAtom = atomMap[l.replaceAtom];
				if (l.newStereoCenter != -1)
					l.newStereoCenter = atomMap[l.newStereoCenter];
				}
			link = linkList.toArray(new Link[0]);
			}

		int maxProgress = mTableModel.getTotalRowCount() / 64;
		startProgress("Replacing sub-structures...", 0, maxProgress);

		int replacements = 0;
		int fragFpColumn = -1;
		SSSearcher searcher = null;
		SSSearcherWithIndex searcherWithIndex = null;
		fragFpColumn = mTableModel.getChildColumn(column, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
		if (fragFpColumn == -1) {
			searcher = new SSSearcher();
			searcher.setFragment(whatMol);
			}
		else {
			searcherWithIndex = new SSSearcherWithIndex();
			searcherWithIndex.setFragment(whatMol, searcherWithIndex.createLongIndex(whatMol));
			}

		boolean found = false;
		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			if ((row & 63) == 63)
				updateProgress(-1);

			CompoundRecord record = mTableModel.getTotalRecord(row);
			if ((mode == cModeSelectedOnly && !mTableModel.isVisibleAndSelected(record))
			 || (mode == cModeVisibleOnly && !mTableModel.isVisible(record)))
				continue;

			if (mTableModel.getChemicalStructure(mTableModel.getTotalRecord(row), column,
					CompoundTableModel.ATOM_COLOR_MODE_NONE, mol) == null)
				continue;

			mol.ensureHelperArrays(Molecule.cHelperParities);
			ArrayList<int[]> matchList = null;
			if (fragFpColumn == -1) {
				searcher.setMolecule(mol);
				if (searcher.findFragmentInMolecule() != 0)
					matchList = searcher.getMatchList();
				}
			else {
				searcherWithIndex.setMolecule(mol, (long[])mTableModel.getTotalRecord(row).getData(fragFpColumn));
				if (searcherWithIndex.findFragmentInMolecule() != 0)
					matchList = searcherWithIndex.getMatchList();
				}
			if (matchList != null) {
				boolean[] atomUsed = new boolean[mol.getAllAtoms()];
				float[] atad = mol.getAverageTopologicalAtomDistance();

				while (matchList.size() != 0) {
					// Remove those matches that overlapp with already processed matches.
					// Then find that remaining match whose link atoms are most central in the molecule
					// (this prefers small substituents to be eliminated)
					int[] bestMatch = null;
					float bestMatchATAD = Float.MAX_VALUE;

					for (int i=matchList.size()-1; i>=0; i--) {
						int[] match = matchList.get(i);
						boolean usedAtomFound = false;
						for (int atom:match) {
							if (atom != -1 && atomUsed[atom]) {
								usedAtomFound = true;
								break;
								}
							}
						if (usedAtomFound) {
							matchList.remove(match);
							continue;
							}

						float matchATAD = 0;
						for (int whatAtom=0; whatAtom<match.length; whatAtom++)
							if (whatMol.isMarkedAtom(whatAtom))
								matchATAD += atad[match[whatAtom]];

						if (matchATAD < bestMatchATAD) {
							matchATAD = bestMatchATAD;
							bestMatch = match;
							}
						}

					if (bestMatch == null)
						break;

					matchList.remove(bestMatch);

					boolean[] isLinkAtom = new boolean[mol.getAllAtoms()];
					for (int whatAtom=0; whatAtom<bestMatch.length; whatAtom++) {
						if (whatMol.isMarkedAtom(whatAtom)) {
							isLinkAtom[bestMatch[whatAtom]] = true;
							}
						else {
							int atom = bestMatch[whatAtom];
							atomUsed[atom] = true;
							mol.markAtomForDeletion(atom);	// don't delete link atoms
							}
						}
					if (allowSubstituents) { // remove substituents at what-fragment from molecule
						for (int whatAtom=0; whatAtom<bestMatch.length; whatAtom++) {
							if (!whatMol.isMarkedAtom(whatAtom)) {
								int atom = bestMatch[whatAtom];
								for (int i=0; i<mol.getConnAtoms(atom); i++) {
									int connAtom = mol.getConnAtom(atom, i);
									if (!isLinkAtom[connAtom]
									 && !mol.isAtomMarkedForDeletion(connAtom))
										markSubstituent(mol, atom, connAtom, isLinkAtom, atomUsed);
									}
								}
							}
						}
					for (int bond=0; bond<mol.getAllBonds(); bond++)
						if (mol.isAtomMarkedForDeletion(mol.getBondAtom(0, bond))
						 || mol.isAtomMarkedForDeletion(mol.getBondAtom(1, bond)))
							mol.markBondForDeletion(bond);
					int[] fromWithAtomToMolAtom = mol.addMolecule(withMol);
					if (link != null) {
						for (Link l:link) {
							mol.addBond(fromWithAtomToMolAtom[l.replaceAtom], bestMatch[l.searchAtom], l.newBondOrder);

							if (mol.getAtomParity(bestMatch[l.searchAtom]) != Molecule.cAtomParityNone) {
								// if we introduce a double bond then reset any parity on new neighbor atom
								int molChiralCenter = bestMatch[l.searchAtom];
								if (l.newBondOrder != 1 && l.bondOrderChanged) {
									mol.setAtomParity(molChiralCenter, Molecule.cAtomParityNone, false);
									}
								// repair parity 1 or 2 on mol side
								else if (mol.getAtomParity(molChiralCenter) == Molecule.cAtomParity1
									  || mol.getAtomParity(molChiralCenter) == Molecule.cAtomParity2) {
									int oldSubstituentIndex = bestMatch[l.searchAtomNeighbour];
									boolean inversion = false;
									// connAtoms should still be valid.
									// The new substituent gets a higher atom index than any mol atoms.
									// We count the chiral center's neighbors with higher atom index than
									// the substituent. Every swap of two neighbors inverts the parity.
									for (int i=0; i<mol.getConnAtoms(molChiralCenter); i++)
										if (mol.getConnAtom(molChiralCenter, i) > oldSubstituentIndex)
											inversion = !inversion;
									if (inversion) {
										int parity = mol.getAtomParity(molChiralCenter) == Molecule.cAtomParity1 ?
												Molecule.cAtomParity2 : Molecule.cAtomParity1;
										mol.setAtomParity(molChiralCenter, parity, mol.isAtomParityPseudo(molChiralCenter));
										}
									}
								}

							if (l.newStereoCenter != -1) {
								mol.setAtomParity(fromWithAtomToMolAtom[l.newStereoCenter], l.newParity, l.newParityIsPseudo);
								}
							}
						}
					replacements++;
					found = true;
					}
				mol.deleteMarkedAtomsAndBonds();
				mol.setParitiesValid(0);
				new CoordinateInventor().invent(mol);
//				mol.setStereoBondsFromParity(); not needed anymore
				Canonizer canonizer = new Canonizer(mol);
				mTableModel.setTotalValueAt(canonizer.getIDCode(), row, column);
				mTableModel.removeChildDescriptorsAndCoordinates(row, column);
				int coords2DColumn = mTableModel.getChildColumn(column, CompoundTableConstants.cColumnType2DCoordinates);
				if (coords2DColumn != -1)
					mTableModel.setTotalValueAt(canonizer.getEncodedCoordinates(), row, coords2DColumn);
				}
			}

		if (found) {
			mTableModel.finalizeChangeChemistryColumn(column, 0, mTableModel.getTotalRowCount(), false);
			}

		if (isInteractive())
			showInteractiveTaskMessage("The substructure was replaced "+replacements+" times.",
					JOptionPane.INFORMATION_MESSAGE);
		}

	private void materializeImplicitHydrogensOnLinkAtoms(StereoMolecule mol) {
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);
		int atoms = mol.getAtoms();
		for (int atom=0; atom<atoms; atom++)
			if (mol.getAtomicNo(atom) > 128 && mol.getAtomicNo(atom) <= 144 && mol.getAllConnAtoms(atom) == 0)
				mol.addBond(atom, mol.addAtom(1));
		}

	/**
	 * Starting from firstAtom and not touching coreAtom
	 * locates all connected hitherto unused atoms.
	 * If these connected unused atoms don't contain any link atom,
	 * then they are flagged as used and marked for deletion.
	 * @param mol
	 * @param coreAtom
	 * @param firstAtom
	 * @param isLinkAtom
	 * @param isUsedAtom
	 */
	private void markSubstituent(StereoMolecule mol, int coreAtom, int firstAtom, boolean[] isLinkAtom, boolean[] isUsedAtom) {
		int[] graphAtom = new int[mol.getAllAtoms()];
		boolean[] isGraphMember = new boolean[mol.getAllAtoms()];

		graphAtom[0] = firstAtom;
		isGraphMember[firstAtom] = true;
		int current = 0;
		int highest = 0;
		while (current <= highest) {
			int connAtoms = mol.getAllConnAtomsPlusMetalBonds(graphAtom[current]);
			for (int i=0; i<connAtoms; i++) {
				int candidate = mol.getConnAtom(graphAtom[current], i);
				if (isGraphMember[candidate]
				 || isUsedAtom[candidate]
				 || (candidate == coreAtom && current == 0))
					continue;

				if (isLinkAtom[candidate])
					return;

				isGraphMember[candidate] = true;
				graphAtom[++highest] = candidate;
				}
			current++;
			}

		for (int i=0; i<=highest; i++) {
			isUsedAtom[graphAtom[i]] = true;
			mol.markAtomForDeletion(graphAtom[i]);
			}
		}

	private void replaceStructures(String what, String with, int column, int mode) {
		int maxProgress = mTableModel.getTotalRowCount() / 64;
		startProgress("Replacing structures...", 0, maxProgress);

		int replacements = 0;
		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			if ((row & 63) == 63)
				updateProgress(-1);

			CompoundRecord record = mTableModel.getTotalRecord(row);
			if ((mode == cModeSelectedOnly && !mTableModel.isVisibleAndSelected(record))
					|| (mode == cModeVisibleOnly && !mTableModel.isVisible(record)))
				continue;

			String value = mTableModel.getTotalValueAt(row, column);
			if (what.equals(CODE_WHAT_EMPTY)) {
				if (value.length() == 0) {
					mTableModel.setTotalValueAt(with, row, column);
					replacements++;
					}
				}
			else if (what.equals(CODE_WHAT_ANY)) {
				mTableModel.setTotalValueAt(with, row, column);
				replacements++;
				}
			}

		if (replacements != 0)
			mTableModel.finalizeChangeChemistryColumn(column, 0, mTableModel.getTotalRowCount(), false);

		if (isInteractive())
			showInteractiveTaskMessage((what.equals(CODE_WHAT_ANY) ? "A " : "An empty ")
				+ "structure was replaced "+replacements+" times.", JOptionPane.INFORMATION_MESSAGE);
		}

	private void replaceText(String what, String with, int targetColumn, boolean isRegex, boolean isCaseSensitive, int mode) {
		if (!isCaseSensitive && !isRegex)
			what = what.toLowerCase();

		DETable table = mParentFrame.getMainFrame().getMainPane().getTable();
		TableColumnModel columnModel = table.getColumnModel();

		int maxProgress = (targetColumn == cColumnSelected) ? columnModel.getSelectedColumnCount()
				: (targetColumn == cColumnVisible) ? mTableModel.getColumnCount()
				: (targetColumn == cColumnAny) ? mTableModel.getColumnCount() : 1;
		startProgress("Replacing '"+what+"'...", 0, maxProgress);

		mReplacements = 0;
		mColumns = 0;

		for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
			int viewColumn = table.convertTotalColumnIndexToView(column);
			if (targetColumn == cColumnSelected && (viewColumn == -1 || !table.isColumnSelected(viewColumn)))
				continue;
			if (targetColumn == cColumnVisible && viewColumn == -1)
				continue;

			if (targetColumn < 0 && column != 0)
				updateProgress(-1);

			if (column == targetColumn || (targetColumn < 0 && mTableModel.getColumnSpecialType(column) == null)) {
				final int _column = column;
				final String _what = what;
				try {
					SwingUtilities.invokeAndWait(() -> replaceTextInOneColumn(_what, with, _column, mode, isRegex, isCaseSensitive));
					} catch (Exception e) {}
				}
			}

		if (isInteractive()) {
			String msg = what.equals(CODE_WHAT_ANY) ?
					"The cell content was replaced " + mReplacements + " times in " + mColumns + " columns."
							  : what.equals(CODE_WHAT_EMPTY) ?
					"" + mReplacements + " empty cells in " + mColumns + " columns were filled with '" + with + "'."
				  : "'" + what + "' was replaced " + mReplacements + " times in " + mColumns + " columns.";
			showInteractiveTaskMessage(msg, JOptionPane.INFORMATION_MESSAGE);
			}
		}

	private int replaceTextInOneColumn(String what, String with, int column, int mode, boolean isRegex, boolean isCaseSensitive) {
		int replacements = 0;

		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			CompoundRecord record = mTableModel.getTotalRecord(row);
			if ((mode == cModeSelectedOnly && !mTableModel.isVisibleAndSelected(record))
			 || (mode == cModeVisibleOnly && !mTableModel.isVisible(record)))
				continue;

			String value = mTableModel.getTotalValueAt(row, column);
			if (what.equals(CODE_WHAT_EMPTY)) {
				if (value.length() == 0) {
					mTableModel.setTotalValueAt(with, row, column);
					replacements++;
					}
				}
			else if (what.equals(CODE_WHAT_ANY)) {
				mTableModel.setTotalValueAt(with, row, column);
				replacements++;
				}
			else if (isRegex) {
				String newValue = value.replaceAll(what, with);
				if (!newValue.equals(value)) {
					mTableModel.setTotalValueAt(newValue, row, column);
					replacements++;
					}
				}
			else if (isCaseSensitive) {
				if (value.contains(what)) {
					mTableModel.setTotalValueAt(value.replace(what, with), row, column);
					replacements++;
					}
				}
			else {
				if (value.toLowerCase().contains(what)) {
					StringBuilder newValue = new StringBuilder();
					String lowerValue = value.toLowerCase();
					int oldValueIndex = 0;
					int index = lowerValue.indexOf(what);
					while (index != -1) {
						if (oldValueIndex < index)
							newValue.append(value.substring(oldValueIndex, index));

						newValue.append(with);
						oldValueIndex = index + what.length();

						index = lowerValue.indexOf(what, oldValueIndex);
						}

					if (oldValueIndex < value.length())
						newValue.append(value.substring(oldValueIndex));

					mTableModel.setTotalValueAt(newValue.toString(), row, column);
					replacements++;
					}
				}
			}

		if (replacements != 0) {
			mTableModel.finalizeChangeAlphaNumericalColumn(column, 0, mTableModel.getTotalRowCount());
			mColumns++;
			mReplacements += replacements;
			}

		return replacements;
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	private class Link {
		int searchAtom,searchAtomNeighbour,replaceAtom,oldBondOrder,newBondOrder,newStereoCenter,newParity;
		boolean bondOrderChanged,newParityIsPseudo;

		public Link(int searchAtom, int searchAtomNeighbour, int oldBondOrder,
				int replaceAtom, int newStereoCenter, int newParity, boolean newParityIsPseudo) {
			this.searchAtom = searchAtom;
			this.searchAtomNeighbour = searchAtomNeighbour;
			this.oldBondOrder = oldBondOrder;
			this.replaceAtom = replaceAtom;
			this.newStereoCenter = newStereoCenter;
			this.newParity = newParity;
			this.newParityIsPseudo = newParityIsPseudo;
			}

		public void setNewBondOrder(int bondOrder) {
			newBondOrder = bondOrder;
			bondOrderChanged = (oldBondOrder != newBondOrder);
			}
		}
	}