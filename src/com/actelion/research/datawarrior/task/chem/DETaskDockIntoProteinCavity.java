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
import com.actelion.research.chem.io.pdb.parser.PDBFileParser;
import com.actelion.research.chem.io.pdb.parser.StructureAssembler;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.form.JFXConformerPanel;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;
import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SeparatorMenuItem;
import org.openmolecules.chem.conf.gen.ConformerGenerator;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DPopupMenuController;
import org.openmolecules.pdb.MMTFParser;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class DETaskDockIntoProteinCavity extends DETaskAbstractFromStructure implements V3DPopupMenuController {
	public static final String TASK_NAME = "Dock Into Protein Cavity";

	private static final String[] COLUMN_TITLE = {"Docking Score", "Docking Pose", "poseCoordinates"};
	private static final int SCORE_COLUMN = 0;
	private static final int POSE_COLUMN = 1;
	private static final int COORDS_COLUMN = 2;

	private static final String PROPERTY_LIGAND = "ligand";
	private static final String PROPERTY_CAVITY = "cavity";
	private static final String PROPERTY_NEUTRALIZE = "neutralize";

	private JFXConformerPanel mConformerPanel;
	private JCheckBox mCheckBoxNeutralize;
	private boolean mNeutralizeFragment;
	private DockingEngine[] mDockingEngine;
	private volatile String mPDBCode;

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

		mCheckBoxNeutralize = new JCheckBox("Neutralize ligand charges");

		mConformerPanel = new JFXConformerPanel(false, false, true);
		mConformerPanel.setBackground(new java.awt.Color(24, 24, 96));
		mConformerPanel.setPreferredSize(new Dimension(HiDPIHelper.scale(320), HiDPIHelper.scale(240)));
		mConformerPanel.setPopupMenuController(this);

		JPanel ep = new JPanel();
		ep.setLayout(new TableLayout(size));
		ep.add(mCheckBoxNeutralize, "0,0");
		ep.add(new JLabel("Protein cavity with natural ligand"), "0,2");
		ep.add(mConformerPanel, "0,4");
		return ep;
		}

	@Override
	public void addExternalMenuItems(ContextMenu popup, int type) {
		if (type == V3DPopupMenuController.TYPE_FILE) {
			javafx.scene.control.MenuItem itemLoadPDBFile = new javafx.scene.control.MenuItem("Load Protein Cavity From PDB-File...");
			itemLoadPDBFile.setOnAction(e -> loadFromPDBFile());

			javafx.scene.control.MenuItem itemLoadPDBDB = new javafx.scene.control.MenuItem("Load Protein Cavity From PDB Database...");
			itemLoadPDBDB.setOnAction(e -> loadFromPDBDatabase());

			popup.getItems().add(itemLoadPDBFile);
			popup.getItems().add(itemLoadPDBDB);
			popup.getItems().add(new SeparatorMenuItem());
			}
		}

	private void showMessageInEDT(String msg) {
		SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mConformerPanel, msg) );
		}

	private void loadFromPDBFile() {
		SwingUtilities.invokeLater(() -> {
			File selectedFile = FileHelper.getFile(mConformerPanel, "Choose PDB-File", FileHelper.cFileTypePDB);
			if (selectedFile != null) {
				PDBFileParser parser = new PDBFileParser();
				try {
					Map<String, List<Molecule3D>> map = parser.parse(selectedFile).extractMols();
					List<Molecule3D> proteins = map.get(StructureAssembler.PROTEIN_GROUP);
					List<Molecule3D> ligands = map.get(StructureAssembler.LIGAND_GROUP);
					addProteinAndLigand(proteins, ligands);
					}
				catch (Exception e) {
					showMessageInEDT(e.getMessage());
					e.printStackTrace();
					}
				}
			} );
		}

	private void loadFromPDBDatabase() {
		mPDBCode = null;
		try {
			SwingUtilities.invokeLater(() -> {
				mPDBCode = JOptionPane.showInputDialog(mConformerPanel, "PDB Entry Code?");
				if (mPDBCode == null || mPDBCode.length() == 0)
					return;

				Molecule3D[] mol = MMTFParser.getStructureFromName(mPDBCode, MMTFParser.MODE_SPLIT_CHAINS);
				if (mol != null) {
					ArrayList<Molecule3D> proteins = new ArrayList<>();
					ArrayList<Molecule3D> ligands = new ArrayList<>();
					for (int i=0; i<mol.length; i++) {
						if (mol[i].getAllBonds() != 0) {
							if (mol[i].getAllAtoms() >= 100)
								proteins.add(mol[i]);
							else
								ligands.add(mol[i]);
							}
						}

					addProteinAndLigand(proteins, ligands);
					}
				} );
			}
		catch (Exception ie) {}
		}

	private void addProteinAndLigand(List<Molecule3D> proteins, List<Molecule3D> ligands) {
		Molecule3D protein = null;
		Molecule3D ligand = null;

		if (proteins == null || proteins.size() == 0) {
			JOptionPane.showMessageDialog(mConformerPanel, "No proteins found in file.");
			}
		else {
			protein = proteins.get(0);
			for (int i=1; i<proteins.size(); i++)
				protein.addMolecule(proteins.get(i));

			if (ligands == null || ligands.size() == 0) {
				JOptionPane.showMessageDialog(mConformerPanel, "No ligands found in file.");
				}
			else {
				int index = -1;
				if (ligands.size() == 1) {
					index = 0;
					}
				else {
					String[] ligandName = new String[ligands.size()];
					for (int i=0; i<ligands.size(); i++)
						ligandName[i] = (i+1)+": "+(ligands.get(i).getName() == null ? "Unnamed" : ligands.get(i).getName());
					String name = (String)JOptionPane.showInputDialog(mConformerPanel, "Select one of multiple ligands:", "Select Ligand", JOptionPane.QUESTION_MESSAGE, null, ligandName, ligandName[0]);
					index = Integer.parseInt(name.substring(0, name.indexOf(':')))-1;
					}

				ligand = ligands.get(index);
				}
			}

		if (protein != null && ligand != null) {
			Coordinates cog = JFXConformerPanel.calculateCOG(ligand);
			StereoMolecule croppedProtein = JFXConformerPanel.cropProtein(protein, ligand, cog);

			ligand.translate(-cog.x, -cog.y, -cog.z);
			croppedProtein.translate(-cog.x, -cog.y, -cog.z);

			final StereoMolecule _protein = croppedProtein;
			final StereoMolecule _ligand = ligand;
			Platform.runLater(() -> {
				mConformerPanel.setProteinCavity(_protein, _ligand);
				});
			}
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
		configuration.setProperty(PROPERTY_NEUTRALIZE, mCheckBoxNeutralize.isSelected() ? "true" : "false");
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
		mCheckBoxNeutralize.setSelected("true".equals(configuration.getProperty(PROPERTY_NEUTRALIZE)));
		String cavityIDCode = configuration.getProperty(PROPERTY_CAVITY);
		StereoMolecule cavity = (cavityIDCode == null) ? null : new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(cavityIDCode);
		String ligandIDCode = configuration.getProperty(PROPERTY_LIGAND);
		StereoMolecule ligand = (ligandIDCode == null) ? null : new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(ligandIDCode);
		if (cavity != null)
			mConformerPanel.setProteinCavity(cavity, ligand);
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mCheckBoxNeutralize.setSelected(true);
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
		System.out.println("added "+new AtomAssembler(protein).addImplicitHydrogens()+" explicit hydrogens to protein");

		StereoMolecule ligand = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(configuration.getProperty(PROPERTY_LIGAND));
		System.out.println("added "+new AtomAssembler(ligand).addImplicitHydrogens()+" explicit hydrogens to ligand");

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

		mNeutralizeFragment = "true".equals(configuration.getProperty(PROPERTY_NEUTRALIZE));

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
		if (mNeutralizeFragment)
			new MoleculeNeutralizer().neutralizeChargedMolecule(mol);

		ConformerGenerator.addHydrogenAtoms(mol);
		}
	}
