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

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.SmilesParser;
import com.actelion.research.chem.SortedStringList;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.name.StructureNameResolver;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.AbstractSingleColumnTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import info.clearthought.layout.TableLayout;
import uk.ac.cam.ch.wwmm.opsin.NameToStructure;

import javax.swing.*;
import java.util.Properties;

public class DETaskAddStructureFromName extends AbstractSingleColumnTask {
	public static final String TASK_NAME = "Add Structures From Name";

//  Cactus is not used anymore.
//	private static final String CACTUS_URL = "https://cactus.nci.nih.gov/chemical/structure/";
//	private static final String CACTUS_FORMAT_SDF = "/sdf";	// alternatives e.g.: smiles,stdinchikey
//	private static final String CACTUS_FORMAT_SMILES = "/smiles";

	private static final String PROPERTY_USE_SERVER = "useServer";
	private static final String PROPERTY_IS_SMARTS = "isSmarts";

	private static final String[] cSourceColumnName = { "substance name", "compound name", "iupac name" };

	private JCheckBox mCheckBoxIsSmarts,mCheckBoxUseServer;

	public DETaskAddStructureFromName(DEFrame parentFrame) {
    	super(parentFrame, parentFrame.getTableModel(), true);
    	}

    @Override
	public boolean isCompatibleColumn(int column) {
		return getTableModel().isColumnTypeString(column) && getTableModel().getColumnSpecialType(column) == null;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public JPanel createInnerDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {TableLayout.PREFERRED},
							{gap/2, TableLayout.PREFERRED, gap*2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };
		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		mCheckBoxIsSmarts = new JCheckBox("Interpret SMILES as SMARTS");
		content.add(mCheckBoxIsSmarts, "0,1");

		content.add(new JLabel("If DataWarrior cannot interpret a name as IUPAC-name nor as SMILES,"), "0,3");
		content.add(new JLabel("then DataWarrior may connect to openmolecules.org to resolve names."), "0,5");

		mCheckBoxUseServer = new JCheckBox("Allow openmolecules.org name-to-structure service");
		content.add(mCheckBoxUseServer, "0,7");

		return content;
		}

	@Override
	public String getColumnLabelText() {
		return "Structure name or SMILES column:";
		}

	@Override
	public void selectDefaultColumn(JComboBox comboBox) {
		for (String name:cSourceColumnName) {
			for (int i=0; i<comboBox.getItemCount(); i++) {
				if (name.equals(((String)comboBox.getItemAt(i)).toLowerCase())) {
					comboBox.setSelectedIndex(i);
					return;
					}
				}
			}

		// default handling, if no obvious name column was found
		super.selectDefaultColumn(comboBox);
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		String isSmarts = (mCheckBoxIsSmarts == null) ? "false" : mCheckBoxIsSmarts.isSelected()? "true" : "false";
		configuration.setProperty(PROPERTY_IS_SMARTS, isSmarts);
		String useServer = (mCheckBoxUseServer == null) ? "true" : mCheckBoxUseServer.isSelected()? "true" : "false";
		configuration.setProperty(PROPERTY_USE_SERVER, useServer);
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		if (mCheckBoxIsSmarts != null)
			mCheckBoxIsSmarts.setSelected("true".equals(configuration.getProperty(PROPERTY_IS_SMARTS, "false")));
		if (mCheckBoxUseServer != null)
			mCheckBoxUseServer.setSelected("true".equals(configuration.getProperty(PROPERTY_USE_SERVER, "true")));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		if (mCheckBoxIsSmarts != null)
			mCheckBoxIsSmarts.setSelected(false);
		if (mCheckBoxUseServer != null)
			mCheckBoxUseServer.setSelected(true);
		}

	@Override
	public void runTask(Properties configuration) {
		int sourceColumn = getColumn(configuration);

        int firstNewColumn = getTableModel().addNewColumns(3);
        int idcodeColumn = firstNewColumn;
        int coordsColumn = firstNewColumn+1;
		getTableModel().prepareStructureColumns(idcodeColumn, "Structure", true, true);

		NameToStructure opsinN2S = NameToStructure.getInstance();
		StereoMolecule mol = new StereoMolecule();

		boolean useServer = "true".equals(configuration.getProperty(PROPERTY_USE_SERVER, "true"))
							&& StructureNameResolver.getInstance() != null;
		SortedStringList unresolvedList = useServer ? new SortedStringList() : null;

		startProgress("Generating Structures...", 0, getTableModel().getTotalRowCount());
		for (int row=0; row<getTableModel().getTotalRowCount() && !threadMustDie(); row++) {
			updateProgress(row);

			String name = getTableModel().getTotalValueAt(row, sourceColumn).trim();
			if (name.length() != 0) {
				String smiles = opsinN2S.parseToSmiles(name);
				if (smiles == null)
					smiles = name;

				try {
					boolean isSmarts = "true".equals(configuration.getProperty(PROPERTY_IS_SMARTS, "false"));
					int smilesMode = isSmarts ? SmilesParser.SMARTS_MODE_IS_SMARTS : SmilesParser.SMARTS_MODE_GUESS;
					new SmilesParser(smilesMode, false).parse(mol, smiles);
					}
				catch (Exception e) {
					mol.clear();
					}

				if (mol.getAllAtoms() != 0) {
					try {
						mol.normalizeAmbiguousBonds();
						mol.canonizeCharge(true);
						Canonizer canonizer = new Canonizer(mol);
						canonizer.setSingleUnknownAsRacemicParity();
						getTableModel().setTotalValueAt(canonizer.getIDCode(), row, idcodeColumn);
						getTableModel().setTotalValueAt(canonizer.getEncodedCoordinates(), row, coordsColumn);
						}
					catch (Exception e) {
						e.printStackTrace();
						}
					}
				else if (useServer) {
					unresolvedList.addString(name);
					}
				}
			}

		if (useServer && unresolvedList.getSize() != 0) {
			startProgress("Sending "+unresolvedList.getSize()+" Names...", 0, 0);
			String[] idcodes = StructureNameResolver.getInstance().resolveRemote(unresolvedList.toArray());
			for (int row=0; row<getTableModel().getTotalRowCount() && !threadMustDie(); row++) {
				String name = getTableModel().getTotalValueAt(row, sourceColumn).trim();
				if (name.length() != 0 && getTableModel().getTotalRecord(row).getData(idcodeColumn) == null) {
					String idcode = idcodes[unresolvedList.getListIndex(name)];
					if (idcode != null && idcode.length() != 0) {
						int index = idcode.indexOf(' ');
						if (index == -1) {
							getTableModel().setTotalValueAt(idcode, row, idcodeColumn);
							}
						else {
							getTableModel().setTotalValueAt(idcode.substring(0, index), row, idcodeColumn);
							getTableModel().setTotalValueAt(idcode.substring(index+1), row, coordsColumn);
							}
						}
					}
				}
			}

		getTableModel().finalizeNewColumns(firstNewColumn, this);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
