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

import com.actelion.research.chem.CanonizerUtil;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.util.Properties;

public class DETaskAddCanonicalCode extends DETaskAbstractFromStructure {
	public static final String TASK_NAME = "Add Canonical Code";

	private static final String PROPERTY_LARGEST_FRAGMENT_ONLY = "largestFragmentOnly";
	private static final String PROPERTY_DISTINGUISH_STEREO_ISOMERS = "distinguishStereoIsomers";
	private static final String PROPERTY_DISTINGUISH_TAUTOMERS = "distinguishTautomers";

	private JCheckBox mCheckBoxDistinguishTautomers,mCheckBoxDistinguishStereoIsomers,mCheckBoxLargestFragmentOnly;
	private boolean mLargestFragmentOnly,mDistinguishStereoIsomers,mDistinguishTautomers;

    public DETaskAddCanonicalCode(DEFrame parent) {
		super(parent, DESCRIPTOR_NONE, false, true);
		}

	@Override
	protected int getNewColumnCount() {
		return 1;
		}

	@Override
	protected String getNewColumnName(int column) {
		return "Canonical Code S:"+(mDistinguishStereoIsomers?"y":"n")+" T:"+(mDistinguishTautomers?"y":"n")+" LF:"+(mLargestFragmentOnly?"y":"n");
		}

	@Override
	public boolean hasExtendedDialogContent() {
		return true;
		}

	@Override
	public JPanel getExtendedDialogContent() {
		double[][] size = { {TableLayout.PREFERRED}, {TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED} };

		mCheckBoxDistinguishStereoIsomers = new JCheckBox("Distinguish stereo isomers");
		mCheckBoxDistinguishTautomers = new JCheckBox("Distinguish tautomers");
		mCheckBoxLargestFragmentOnly = new JCheckBox("Largest Fragment Only");

		JPanel ep = new JPanel();
		ep.setLayout(new TableLayout(size));
		ep.add(mCheckBoxDistinguishStereoIsomers, "0,0");
		ep.add(mCheckBoxDistinguishTautomers, "0,1");
		ep.add(mCheckBoxLargestFragmentOnly, "0,2");
		return ep;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/chemistry.html#AddCanonicalCode";
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_DISTINGUISH_STEREO_ISOMERS, mCheckBoxDistinguishStereoIsomers.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_DISTINGUISH_TAUTOMERS, mCheckBoxDistinguishTautomers.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_LARGEST_FRAGMENT_ONLY, mCheckBoxLargestFragmentOnly.isSelected() ? "true" : "false");
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mCheckBoxDistinguishStereoIsomers.setSelected("true".equals(configuration.getProperty(PROPERTY_DISTINGUISH_STEREO_ISOMERS)));
		mCheckBoxDistinguishTautomers.setSelected("true".equals(configuration.getProperty(PROPERTY_DISTINGUISH_TAUTOMERS)));
		mCheckBoxLargestFragmentOnly.setSelected("true".equals(configuration.getProperty(PROPERTY_LARGEST_FRAGMENT_ONLY)));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mCheckBoxDistinguishStereoIsomers.setSelected(true);
		mCheckBoxDistinguishTautomers.setSelected(true);
		mCheckBoxLargestFragmentOnly.setSelected(true);
		}

	@Override
	protected boolean preprocessRows(Properties configuration) {
		mDistinguishStereoIsomers = "true".equals(configuration.getProperty(PROPERTY_DISTINGUISH_STEREO_ISOMERS));
		mDistinguishTautomers = "true".equals(configuration.getProperty(PROPERTY_DISTINGUISH_TAUTOMERS));
		mLargestFragmentOnly = "true".equals(configuration.getProperty(PROPERTY_LARGEST_FRAGMENT_ONLY));
		return super.preprocessRows(configuration);
    	}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol, int threadIndex) {
		long hash = 0L;
		if (mDistinguishStereoIsomers && mDistinguishTautomers && !mLargestFragmentOnly) {
			String idcode = getTableModel().getTotalValueAt(row, getChemistryColumn());
			if (idcode.length() != 0)
				hash = CanonizerUtil.StrongHasher.hash(idcode);
			}
		else {
			CompoundRecord record = getTableModel().getTotalRecord(row);
	    	StereoMolecule mol = getTableModel().getChemicalStructure(record, getChemistryColumn(), CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
			if (mol != null) {
				if (mDistinguishStereoIsomers && mDistinguishTautomers)
					hash = CanonizerUtil.getHash(mol, CanonizerUtil.IDCODE_TYPE.NORMAL, mLargestFragmentOnly);
				else if (mDistinguishStereoIsomers && !mDistinguishTautomers)
					hash = CanonizerUtil.getTautomerHash(mol, mLargestFragmentOnly);
				else if (!mDistinguishStereoIsomers && mDistinguishTautomers)
					hash = CanonizerUtil.getNoStereoHash(mol, mLargestFragmentOnly);
				else
					hash = CanonizerUtil.getNoStereoTautomerHash(mol, mLargestFragmentOnly);
				}
			}

		if (hash != 0L)
			getTableModel().setTotalValueAt(Long.toHexString(hash), row, firstNewColumn);
		}
	}
