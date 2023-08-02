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

package com.actelion.research.datawarrior.task.chem;

import com.actelion.research.chem.*;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.gui.JEditableStructureView;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.util.Properties;

public class DETaskExtractFragment extends DETaskAbstractFromStructure {
	public static final String TASK_NAME = "Extract Fragment";

	private static final String PROPERTY_NEUTRALIZE = "neutralize";
	private static final String PROPERTY_AS_SUBSTRUCTURE = "asSubstructure";
	private static final String PROPERTY_SUBSTRUCTURE = "substructure";

	private static final String[] OPTIONS = {"Size (largest)", "Substructure"};
	private static final int OPTION_SIZE = 0;
	private static final int OPTION_SUBSTRUCTURE = 1;

	private JCheckBox mCheckBoxNeutralize,mCheckBoxAsSubstructure;
	private JComboBox mComboBoxExtractFragment;
	private JEditableStructureView mSubstructureView;
	private boolean mNeutralizeFragment,mAsSubstructure;
	private SSSearcher[] mSearcher;
	private SSSearcherWithIndex[] mSearcherWithIndex;
	private int mExtractOption,mFFPColumn;

    public DETaskExtractFragment(DEFrame parent) {
		super(parent, DESCRIPTOR_NONE, false, true);
		}

	@Override
	protected int getNewColumnCount() {
		int count = 1;
		for (int column=0; column<getTableModel().getTotalColumnCount(); column++)
			if (getTableModel().getParentColumn(column) == getChemistryColumn() && isCoordinateColumn(column))
			  count++;
		return count;
		}

	@Override
	protected String getNewColumnName(int column) {
		// is done by setNewColumnProperties()
		return "";
		}

	@Override
	public boolean hasExtendedDialogContent() {
		return true;
		}

	@Override
	public JPanel getExtendedDialogContent() {
    	int gap = HiDPIHelper.scale(8);
		double[][] size = { {TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, TableLayout.FILL},
							{TableLayout.PREFERRED, gap/2, HiDPIHelper.scale(80), gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED} };

		mComboBoxExtractFragment = new JComboBox(OPTIONS);
		mCheckBoxNeutralize = new JCheckBox("Neutralize charges");
		mCheckBoxAsSubstructure = new JCheckBox("Convert to sub-structure");
		mSubstructureView = new JEditableStructureView();
		mSubstructureView.getMolecule().setFragment(true);

		mComboBoxExtractFragment.addActionListener(e -> mSubstructureView.setEnabled(mComboBoxExtractFragment.getSelectedIndex() == OPTION_SUBSTRUCTURE));

		JPanel ep = new JPanel();
		ep.setLayout(new TableLayout(size));
		ep.add(new JLabel("Extract fragment by:"), "0,0");
		ep.add(mComboBoxExtractFragment, "2,0");
		ep.add(mSubstructureView, "2,2,3,2");
		ep.add(mCheckBoxNeutralize, "0,4,3,4");
		ep.add(mCheckBoxAsSubstructure, "0,6,3,6");
		return ep;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/chemistry.html#AddLargestFragment";
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (mComboBoxExtractFragment.getSelectedIndex() == OPTION_SUBSTRUCTURE
		 && mSubstructureView.getMolecule().getAllAtoms() == 0) {
			showErrorMessage("No substructure defined.");
			return false;
			}

		return super.isConfigurationValid(configuration, isLive);
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		if (mComboBoxExtractFragment.getSelectedIndex() == OPTION_SUBSTRUCTURE) {
			StereoMolecule mol = mSubstructureView.getMolecule();
			if (mol.getAllAtoms() != 0) {
				Canonizer canonizer = new Canonizer(mol);
				configuration.setProperty(PROPERTY_SUBSTRUCTURE, canonizer.getIDCode()+" "+canonizer.getEncodedCoordinates());
				}
			}
		configuration.setProperty(PROPERTY_NEUTRALIZE, mCheckBoxNeutralize.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_AS_SUBSTRUCTURE, mCheckBoxAsSubstructure.isSelected() ? "true" : "false");
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		String idcode = configuration.getProperty(PROPERTY_SUBSTRUCTURE);
		mComboBoxExtractFragment.setSelectedIndex(idcode == null ? OPTION_SIZE : OPTION_SUBSTRUCTURE);
		if (idcode != null)
			mSubstructureView.setIDCode(idcode);
		mCheckBoxNeutralize.setSelected("true".equals(configuration.getProperty(PROPERTY_NEUTRALIZE)));
		mCheckBoxAsSubstructure.setSelected("true".equals(configuration.getProperty(PROPERTY_AS_SUBSTRUCTURE)));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mComboBoxExtractFragment.setSelectedIndex(OPTION_SIZE);
		mCheckBoxNeutralize.setSelected(true);
		mCheckBoxAsSubstructure.setSelected(false);
		}

	@Override
	protected void setNewColumnProperties(int firstNewColumn) {
		String sourceColumnName = getTableModel().getColumnTitle(getChemistryColumn());

		getTableModel().setColumnName((mExtractOption == OPTION_SIZE ? "Largest Fragment of " : "Fragment of ")
				+ sourceColumnName, firstNewColumn);
		getTableModel().setColumnProperty(firstNewColumn, CompoundTableConstants.cColumnPropertySpecialType,
				CompoundTableConstants.cColumnTypeIDCode);
		if (mAsSubstructure)
			getTableModel().setColumnProperty(firstNewColumn, CompoundTableConstants.cColumnPropertyIsFragment, "true");

		int count = 1;
		for (int column=0; column<getTableModel().getTotalColumnCount(); column++) {
			if (getTableModel().getParentColumn(column) == getChemistryColumn() && isCoordinateColumn(column)) {
				getTableModel().setColumnName("fragmentCoordinates"+count, firstNewColumn+count);
				getTableModel().setColumnProperty(firstNewColumn+count,
						CompoundTableConstants.cColumnPropertySpecialType, getTableModel().getColumnSpecialType(column));
				getTableModel().setColumnProperty(firstNewColumn+count,
						CompoundTableConstants.cColumnPropertyParentColumn, getTableModel().getColumnTitleNoAlias(firstNewColumn));
				count++;
				}
			}
		}

	private boolean isCoordinateColumn(int column) {
		return CompoundTableConstants.cColumnType2DCoordinates.equals(getTableModel().getColumnSpecialType(column))
			|| CompoundTableConstants.cColumnType3DCoordinates.equals(getTableModel().getColumnSpecialType(column));
		}

	@Override
	protected boolean preprocessRows(Properties configuration) {
		mNeutralizeFragment = "true".equals(configuration.getProperty(PROPERTY_NEUTRALIZE));
		mAsSubstructure = "true".equals(configuration.getProperty(PROPERTY_AS_SUBSTRUCTURE));
		mFFPColumn = getTableModel().getChildColumn(getChemistryColumn(configuration), DescriptorConstants.DESCRIPTOR_FFP512.shortName);
		String idcode = configuration.getProperty(PROPERTY_SUBSTRUCTURE);
		mExtractOption = (idcode == null) ? OPTION_SIZE : OPTION_SUBSTRUCTURE;
		if (idcode != null) {
			StereoMolecule substructure = new IDCodeParser().getCompactMolecule(idcode);
			if (substructure.getAllAtoms() != 0) {
				int threadCount = Runtime.getRuntime().availableProcessors();
				if (mFFPColumn != -1) {
					mSearcherWithIndex = new SSSearcherWithIndex[threadCount];
					for (int i=0; i<threadCount; i++) {
						mSearcherWithIndex[i] = new SSSearcherWithIndex();
						mSearcherWithIndex[i].setFragment(substructure, (long[])null);
						}
					}
				else {
					mSearcher = new SSSearcher[threadCount];
					for (int i=0; i<threadCount; i++) {
						mSearcher[i] = new SSSearcher();
						mSearcher[i].setFragment(substructure);
						}
					}
				}
			}
		return super.preprocessRows(configuration);
    	}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol, int threadIndex) {
		CompoundRecord record = getTableModel().getTotalRecord(row);
		byte[] idcode = (byte[])record.getData(getChemistryColumn());
		if (idcode != null) {
			int count = 0;
			for (int column=0; column<getTableModel().getTotalColumnCount(); column++) {
				if (getTableModel().getParentColumn(column) == getChemistryColumn()) {
					if (record.getData(column) != null && isCoordinateColumn(column)) {
						count++;
						boolean is2D = CompoundTableConstants.cColumnType2DCoordinates.equals(getTableModel().getColumnSpecialType(column));
						StereoMolecule mol = new IDCodeParser(is2D).getCompactMolecule(idcode, (byte[])record.getData(column));
						handleMolecule(mol, row, threadIndex);
						Canonizer canonizer = new Canonizer(mol);
						getTableModel().setTotalValueAt(canonizer.getIDCode(), row, firstNewColumn);
						getTableModel().setTotalValueAt(canonizer.getEncodedCoordinates(), row, firstNewColumn+count);
						}
					}
				}
			if (count == 0) {
				StereoMolecule mol = getChemicalStructure(row, containerMol);
				if (mol != null) {
					handleMolecule(mol, row, threadIndex);
					Canonizer canonizer = new Canonizer(mol);
					getTableModel().setTotalValueAt(canonizer.getIDCode(), row, firstNewColumn);
					}
				}
			}
		}

	private void handleMolecule(StereoMolecule mol, int row, int threadIndex) {
		if (mExtractOption == OPTION_SUBSTRUCTURE) {
			int[] matchList = null;
			if (mSearcherWithIndex != null) {
				mSearcherWithIndex[threadIndex].setMolecule(mol, (long[])getTableModel().getTotalRecord(row).getData(mFFPColumn));
				if (mSearcherWithIndex[threadIndex].findFragmentInMolecule(SSSearcher.cCountModeFirstMatch, SSSearcher.cDefaultMatchMode) != 0) {
					matchList = mSearcherWithIndex[threadIndex].getGraphMatcher().getMatchList().get(0);
					}
				}
			else {
				mSearcher[threadIndex].setMolecule(mol);
				if (mSearcher[threadIndex].findFragmentInMolecule(SSSearcher.cCountModeFirstMatch, SSSearcher.cDefaultMatchMode) != 0)
					matchList = mSearcher[threadIndex].getMatchList().get(0);
				}

			if (matchList == null) {
				mol.clear();
				}
			else {
				int rootAtom = -1;
				for (int atom:matchList) {
					if (atom != -1) {
						rootAtom = atom;
						break;
						}
					}
				boolean[] isFragmentMember = new boolean[mol.getAllAtoms()];
				mol.getFragmentAtoms(rootAtom, false, isFragmentMember);
				for (int i=0; i<isFragmentMember.length; i++)
					isFragmentMember[i] = !isFragmentMember[i];
				mol.deleteAtoms(isFragmentMember);
				}
			}
		else  {
			mol.stripSmallFragments();
			}

		if (mNeutralizeFragment)
			MoleculeNeutralizer.neutralizeChargedMolecule(mol);
		if (mAsSubstructure)
			mol.setFragment(true);
		}
	}
