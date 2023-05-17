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
import com.actelion.research.chem.conf.AtomAssembler;
import com.actelion.research.chem.docking.DockingEngine;
import com.actelion.research.chem.docking.DockingFailedException;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.chem.elib.DockingPanelController;
import com.actelion.research.gui.form.JFXConformerPanel;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;
import javafx.application.Platform;
import org.openmolecules.chem.conf.gen.ConformerGenerator;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DScene;

import javax.swing.*;
import java.awt.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

public class DETaskDockIntoProteinCavity extends DETaskAbstractFromStructure {
	public static final String TASK_NAME = "Dock Into Protein Cavity";

	private static final String[] COLUMN_TITLE = {"Docking Score", "Docked Structure", "Docking Pose"};
	private static final int SCORE_COLUMN = 0;
	private static final int POSE_COLUMN = 1;
	private static final int COORDS_COLUMN = 2;

	private static final String PROPERTY_LIGAND = "ligand";
	private static final String PROPERTY_CAVITY = "cavity";
	private static final String PROPERTY_PROTONATE = "protonate";

	private JFXConformerPanel mConformerPanel;
	private JCheckBox mCheckBoxProtonate;
	private boolean mProtonateFragment;
	private DockingEngine[] mDockingEngine;

	public DETaskDockIntoProteinCavity(DEFrame parent) {
		super(parent, DESCRIPTOR_NONE, false, true);
	}

	@Override
	protected int getNewColumnCount() {
		return 3;
		}

	@Override
	protected String getNewColumnName(int column) {
		return COLUMN_TITLE[column];
		}

	@Override
	public boolean hasExtendedDialogContent() {
		return true;
	}

	@Override
	public JPanel getExtendedDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {TableLayout.PREFERRED}, {TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED} };

		mCheckBoxProtonate = new JCheckBox("Use likely ligand protonation states");

		EnumSet<V3DScene.ViewerSettings> settings = V3DScene.CONFORMER_VIEW_MODE;
		settings.add(V3DScene.ViewerSettings.EDITING);
		mConformerPanel = new JFXConformerPanel(false, settings);
		mConformerPanel.setPopupMenuController(new DockingPanelController(mConformerPanel));
		mConformerPanel.setBackground(new java.awt.Color(24, 24, 96));
		mConformerPanel.setPreferredSize(new Dimension(HiDPIHelper.scale(320), HiDPIHelper.scale(240)));

		JPanel ep = new JPanel();
		ep.setLayout(new TableLayout(size));
		ep.add(mCheckBoxProtonate, "0,0");
		ep.add(new JLabel("Protein cavity with natural ligand"), "0,2");
		ep.add(mConformerPanel, "0,4");
		return ep;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public String getHelpURL() {
		return null /* "/html/help/chemistry.html#docking" */;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_PROTONATE, mCheckBoxProtonate.isSelected() ? "true" : "false");
		List<StereoMolecule> protein = mConformerPanel.getMolecules(V3DMolecule.MoleculeRole.MACROMOLECULE);
		if (protein.size() == 1) {
			Canonizer canonizer = new Canonizer(protein.get(0));
			configuration.setProperty(PROPERTY_CAVITY, canonizer.getIDCode() + " " + canonizer.getEncodedCoordinates());
			}
		List<StereoMolecule> ligand = mConformerPanel.getMolecules(V3DMolecule.MoleculeRole.LIGAND);
		if (ligand.size() == 1) {
			Canonizer canonizer = new Canonizer(ligand.get(0));
			configuration.setProperty(PROPERTY_LIGAND, canonizer.getIDCode() + " " + canonizer.getEncodedCoordinates());
			}
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mCheckBoxProtonate.setSelected("true".equals(configuration.getProperty(PROPERTY_PROTONATE)));
		String cavityIDCode = configuration.getProperty(PROPERTY_CAVITY);
		StereoMolecule cavity = (cavityIDCode == null) ? null : new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(cavityIDCode);
		String ligandIDCode = configuration.getProperty(PROPERTY_LIGAND);
		StereoMolecule ligand = (ligandIDCode == null) ? null : new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(ligandIDCode);
		if (cavity != null) {
			Platform.runLater(() -> {
				mConformerPanel.setProteinCavity(cavity, ligand, true);
				V3DMolecule ligand3D = new V3DMolecule(ligand, 0, V3DMolecule.MoleculeRole.LIGAND);
				ligand3D.setColor(javafx.scene.paint.Color.CORAL);
				mConformerPanel.getV3DScene().addMolecule(ligand3D);
				} );
			}
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mCheckBoxProtonate.setSelected(true);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (configuration.getProperty(PROPERTY_CAVITY) == null) {
			showErrorMessage("No protein or too many protein structures.");
			return false;
			}
		if (configuration.getProperty(PROPERTY_LIGAND) == null) {
			showErrorMessage("No ligand or too many ligand structures.");
			return false;
			}
		return super.isConfigurationValid(configuration, isLive);
		}

	@Override
	protected void setNewColumnProperties(int firstNewColumn) {
		getTableModel().setColumnProperty(firstNewColumn+POSE_COLUMN,
				CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnTypeIDCode);
		getTableModel().setColumnProperty(firstNewColumn+COORDS_COLUMN,
				CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnType3DCoordinates);
		getTableModel().setColumnProperty(firstNewColumn+COORDS_COLUMN,
				CompoundTableConstants.cColumnPropertyParentColumn, COLUMN_TITLE[POSE_COLUMN]);
		getTableModel().setColumnProperty(firstNewColumn+COORDS_COLUMN,
				CompoundTableConstants.cColumnPropertyProteinCavity, getTaskConfiguration().getProperty(PROPERTY_CAVITY));
		getTableModel().setColumnProperty(firstNewColumn+COORDS_COLUMN,
				CompoundTableConstants.cColumnPropertyNaturalLigand, getTaskConfiguration().getProperty(PROPERTY_LIGAND));
		}

	@Override
	protected boolean preprocessRows(Properties configuration) {
		if (!super.preprocessRows(configuration))
			return false;

		startProgress("Preparing protein...", 0, 0);

		StereoMolecule protein = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(configuration.getProperty(PROPERTY_CAVITY));
		assignLikelyProtonationStates(protein);
		new AtomAssembler(protein).addImplicitHydrogens();

		StereoMolecule ligand = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(configuration.getProperty(PROPERTY_LIGAND));
		assignLikelyProtonationStates(ligand);
		new AtomAssembler(ligand).addImplicitHydrogens();

		int threadCount = Runtime.getRuntime().availableProcessors();
		mDockingEngine = new DockingEngine[threadCount];
		try {
			for (int i=0; i<threadCount; i++)
				mDockingEngine[i] = new DockingEngine(protein, ligand);
			}
		catch (DockingFailedException dfe) {
			showErrorMessage(dfe.getMessage());
			return false;
			}

		mProtonateFragment = "true".equals(configuration.getProperty(PROPERTY_PROTONATE));

		return true;
		}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol, int threadIndex) {
		CompoundRecord record = getTableModel().getTotalRecord(row);
		byte[] idcode = (byte[])record.getData(getChemistryColumn());
		if (idcode != null) {
			StereoMolecule query = getChemicalStructure(row, containerMol);
			if (query != null) {
				handleMolecule(query);

				try {
					DockingEngine.DockingResult dockingResult = mDockingEngine[threadIndex].dockMolecule(query);
					StereoMolecule pose = dockingResult.getPose();

					Canonizer canonizer = new Canonizer(pose);
					getTableModel().setTotalValueAt(canonizer.getIDCode(), row, firstNewColumn+POSE_COLUMN);
					getTableModel().setTotalValueAt(canonizer.getEncodedCoordinates(), row, firstNewColumn+COORDS_COLUMN);
					getTableModel().setTotalValueAt(DoubleFormat.toString(dockingResult.getScore()), row, firstNewColumn+SCORE_COLUMN);
					}
				catch (DockingFailedException dfe) {
					System.out.println(dfe.getMessage());
					dfe.printStackTrace();
					}
				}
			}
		}

	private void handleMolecule(StereoMolecule mol) {
		mol.stripSmallFragments();
		if (mProtonateFragment) {
			MoleculeNeutralizer.neutralizeChargedMolecule(mol);
			assignLikelyProtonationStates(mol);
			}

		ConformerGenerator.addHydrogenAtoms(mol);
		}

	public static void assignLikelyProtonationStates(StereoMolecule mol) {
		mol.ensureHelperArrays(Molecule.cHelperRings);
		for (int atom=0; atom<mol.getAtoms(); atom++) {
			if (AtomFunctionAnalyzer.isBasicNitrogen(mol, atom))
				mol.setAtomCharge(atom, +1);
			else if (AtomFunctionAnalyzer.isAcidicOxygen(mol, atom))
				mol.setAtomCharge(atom, -1);
			}
		}
	}
