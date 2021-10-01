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
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.gui.JEditableStructureView;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class DETaskAddSubstructureCount extends DETaskAbstractFromStructure {
	public static final String TASK_NAME = "Add Substructure Count";

	private static final String PROPERTY_IDCODE = "idcode";
	private static final String PROPERTY_IDCOORDS = "idcoords";
	private static final String PROPERTY_ALLOW_OVERLAPS = "allowOverlaps";
	private static final String PROPERTY_COLUMN_NAME = "columnName";

	private JEditableStructureView mStructureView;
	private JCheckBox mCheckBoxAllowOverlaps;
	private JTextField mTextFieldColumnName;
	private volatile String mColumnName;
	private volatile int mCountMode,mFragFpColumn;
	private volatile ConcurrentHashMap<Thread,Object> mSearcherMap;
	private volatile StereoMolecule mFragment;

	public DETaskAddSubstructureCount(DEFrame parent) {
		super(parent, DESCRIPTOR_NONE, false, true);
	}

	@Override
	protected int getNewColumnCount() {
		return 1;
	}

	@Override
	protected String getNewColumnName(int column) {
		return mColumnName;
	}

	@Override
	public boolean hasExtendedDialogContent() {
		return true;
	}

	@Override
	public JPanel getExtendedDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {TableLayout.PREFERRED, gap, TableLayout.PREFERRED}, {TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED} };

		mTextFieldColumnName = new JTextField();
		mCheckBoxAllowOverlaps = new JCheckBox("Include overlapping substructure matches");

		StereoMolecule mol = new StereoMolecule();
		mol.setFragment(true);
		int scaled100 = HiDPIHelper.scale(120);
		mStructureView = new JEditableStructureView(mol);
		mStructureView.setMinimumSize(new Dimension(scaled100, scaled100));
		mStructureView.setPreferredSize(new Dimension(scaled100, scaled100));
		mStructureView.setBorder(BorderFactory.createTitledBorder("Structure"));

		JPanel ep = new JPanel();
		ep.setLayout(new TableLayout(size));
		ep.add(new JLabel("Column Name"), "0,0");
		ep.add(mTextFieldColumnName, "2,0");
		ep.add(mStructureView, "0,2,2,2");
		ep.add(mCheckBoxAllowOverlaps, "0,4,2,4");
		return ep;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public String getHelpURL() {
		return "/html/help/chemistry.html#AddSubstructureCount";
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_COLUMN_NAME, mTextFieldColumnName.getText());
		Canonizer canonizer = new Canonizer(mStructureView.getMolecule());
		configuration.setProperty(PROPERTY_IDCODE, canonizer.getIDCode());
		configuration.setProperty(PROPERTY_IDCOORDS, canonizer.getEncodedCoordinates());
		configuration.setProperty(PROPERTY_ALLOW_OVERLAPS, mCheckBoxAllowOverlaps.isSelected() ? "true" : "false");
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mTextFieldColumnName.setText(configuration.getProperty(PROPERTY_COLUMN_NAME, "Substructure Count"));
		mStructureView.setIDCode(configuration.getProperty(PROPERTY_IDCODE), configuration.getProperty(PROPERTY_IDCOORDS));
		mCheckBoxAllowOverlaps.setSelected("true".equals(configuration.getProperty(PROPERTY_ALLOW_OVERLAPS)));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mTextFieldColumnName.setText("Substructure Count");
		mStructureView.setIDCode(null, null);
		mCheckBoxAllowOverlaps.setSelected(false);
		}

	@Override
	protected boolean preprocessRows(Properties configuration) {
		mColumnName = configuration.getProperty(PROPERTY_COLUMN_NAME, "Substructure Count");
		mFragment = new IDCodeParser().getCompactMolecule(configuration.getProperty(PROPERTY_IDCODE), configuration.getProperty(PROPERTY_IDCOORDS));
		boolean allowOverlaps = "true".equals(configuration.getProperty(PROPERTY_ALLOW_OVERLAPS));
		mCountMode = allowOverlaps ? SSSearcher.cCountModeOverlapping : SSSearcher.cCountModeSeparated;
		mFragFpColumn = getTableModel().getChildColumn(getChemistryColumn(), DescriptorConstants.DESCRIPTOR_FFP512.shortName);
		mSearcherMap = new ConcurrentHashMap<>();

		return super.preprocessRows(configuration);
		}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol, int threadIndex) {
		CompoundRecord record = getTableModel().getTotalRecord(row);
		StereoMolecule mol = getTableModel().getChemicalStructure(record, getChemistryColumn(), CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
		if (mol != null) {
			Object searcher = mSearcherMap.get(Thread.currentThread());
			if (searcher == null) {
				if (mFragFpColumn == -1) {
					searcher = new SSSearcher();
					((SSSearcher)searcher).setFragment(mFragment);
					}
				else {
					searcher = new SSSearcherWithIndex();
					((SSSearcherWithIndex)searcher).setFragment(mFragment, (long[])null);
					}
				mSearcherMap.put(Thread.currentThread(), searcher);
				}

			int count;
			if (searcher instanceof SSSearcher) {
				((SSSearcher)searcher).setMolecule(mol);
				count = ((SSSearcher)searcher).findFragmentInMolecule(mCountMode, SSSearcher.cDefaultMatchMode);
				}
			else {
				((SSSearcherWithIndex)searcher).setMolecule(mol, (long[])null);
				count = ((SSSearcherWithIndex)searcher).findFragmentInMolecule(mCountMode, SSSearcher.cDefaultMatchMode);
				}

			getTableModel().setTotalValueAt(Integer.toString(count), row, firstNewColumn);
			}
		}
	}
