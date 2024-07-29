package com.actelion.research.datawarrior.task.chem.elib;

import com.actelion.research.chem.MolecularFormula;
import com.actelion.research.chem.Molecule3D;
import com.actelion.research.chem.MolfileParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.Mol2FileParser;
import com.actelion.research.chem.io.pdb.parser.PDBCoordEntryFile;
import com.actelion.research.chem.io.pdb.parser.PDBFileParser;
import com.actelion.research.chem.io.pdb.parser.StructureAssembler;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.form.JFXConformerPanel;
import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SeparatorMenuItem;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DPopupMenuController;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.fx.viewer3d.io.V3DMoleculeParser;
import org.openmolecules.render.MoleculeArchitect;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

public class ConformerViewController implements V3DPopupMenuController {
	private Frame mParentFrame;
	private JFXConformerPanel mConformerPanel;
	private volatile String mPDBCode;

	public ConformerViewController(Frame owner, JFXConformerPanel conformerPanel) {
		mParentFrame = owner;
		mConformerPanel = conformerPanel;
		}

	@Override
	public void addExternalMenuItems(ContextMenu popup, int type) {
		if (type == V3DPopupMenuController.TYPE_FILE) {
			javafx.scene.control.MenuItem itemLoadMolecule = new javafx.scene.control.MenuItem("Load Conformer from Molfile/Mol2-File...");
			itemLoadMolecule.setOnAction(e -> loadMolecule());

			javafx.scene.control.MenuItem itemLoadMolecules = new javafx.scene.control.MenuItem("Load Conformer(s) from DataWarrior-/SD-File...");
			itemLoadMolecules.setOnAction(e -> loadMolecules());

			javafx.scene.control.MenuItem itemLoadPDBFile = new javafx.scene.control.MenuItem("Load Ligand from PDB File...");
			itemLoadPDBFile.setOnAction(e -> loadPDBLigand(false));

			javafx.scene.control.MenuItem itemLoadPDBDBLigand = new javafx.scene.control.MenuItem("Load Ligand From PDB Database...");
			itemLoadPDBDBLigand.setOnAction(e -> loadPDBLigand(true));

			popup.getItems().add(itemLoadMolecule);
			popup.getItems().add(itemLoadMolecules);
			popup.getItems().add(itemLoadPDBFile);
			popup.getItems().add(itemLoadPDBDBLigand);
			popup.getItems().add(new SeparatorMenuItem());
			}
		}

	@Override
	public void markCropDistanceForSurface(V3DMolecule fxmol, int type, V3DMolecule.SurfaceMode mode) {

	}

	private void loadMolecule() {
		SwingUtilities.invokeLater(() -> {
			int fileTypes = FileHelper.cFileTypeMOL | FileHelper.cFileTypeMOL2;
			File selectedFile = FileHelper.getFile(mParentFrame, "Open 3D-Molecule(s)", fileTypes);
			if (selectedFile != null) {
				Platform.runLater(() -> {
					StereoMolecule mol = null;
					if (FileHelper.getFileType(selectedFile.getName()) == FileHelper.cFileTypeMOL) {
						mol = new MolfileParser().getCompactMolecule(selectedFile);
						}
					else {  // MOL2
						try {
							mol = new Mol2FileParser().load(selectedFile);
							}
						catch (Exception e) {
							e.printStackTrace();
						}
					}

					if (mol != null && mol.getAllAtoms() != 0) {
						mol.center();
						V3DScene scene = mConformerPanel.getV3DScene();
						scene.addMolecule(new V3DMolecule(mol));
						scene.optimizeView();
						}
					});
				}
			});
		}

	private void loadMolecules() {
		SwingUtilities.invokeLater(() -> {
			int fileTypes = FileHelper.cFileTypeDataWarrior | FileHelper.cFileTypeSD;
			File selectedFile = FileHelper.getFile(mParentFrame, "Open 3D-Molecule(s)", fileTypes);
			if (selectedFile != null) {
				Platform.runLater(() -> {
					V3DScene scene = mConformerPanel.getV3DScene();
					V3DMoleculeParser.readMoleculeFile(scene, selectedFile.toString());
					scene.optimizeView();
				});
			}
		});
	}

	private void showMessageInEDT(String msg) {
		SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mConformerPanel, msg) );
		}

/*	private void loadPDBFile() {
		SwingUtilities.invokeLater(() -> {
			File selectedFile = FileHelper.getFile(mConformerPanel, "Choose PDB-File", FileHelper.cFileTypePDB);
			if (selectedFile != null) {
				Platform.runLater(() -> {
					PDBFileParser parser = new PDBFileParser();
					V3DScene scene = mConformerPanel.getV3DScene();
					try {
						PDBCoordEntryFile entryFile = parser.parse(selectedFile);
						List<Molecule3D> ligands = entryFile.extractMols().get(StructureAssembler.LIGAND_GROUP);
						if (ligands == null || ligands.isEmpty()) {
							ligands = entryFile.extractMols(true).get(StructureAssembler.LIGAND_GROUP);
							if (ligands == null || ligands.isEmpty())
								showMessageInEDT("No ligands found in file.");
							else
								showMessageInEDT("Covalent bound ligands were found and diconnected from the protein.");
							}

						if (ligands != null && !ligands.isEmpty()) {
							if (ligands.size() == 1) {
								Molecule3D mol = ligands.get(0);
								mol.center();
								scene.addMolecule(new V3DMolecule(mol));
								}
							else {
								for (Molecule3D mol : ligands)
									scene.addMolecule(new V3DMolecule(mol));
							}
						}
					} catch (Exception e) {
						showMessageInEDT(e.getMessage());
						e.printStackTrace();
						}
					} );
				}
			} );
		}*/

	private void loadPDBLigand(boolean fromRemote) {
		mPDBCode = null;
		try {
			SwingUtilities.invokeLater(() -> {
				File pdbFile = null;
				if (fromRemote) {
					mPDBCode = JOptionPane.showInputDialog(mConformerPanel, "PDB Entry Code?");
					if (mPDBCode == null || mPDBCode.isEmpty())
						return;
				}
				else {
					pdbFile = FileHelper.getFile(mConformerPanel, "Choose PDB-File", FileHelper.cFileTypePDB);
					if (pdbFile == null)
						return;
				}

				PDBFileParser parser = new PDBFileParser();
				try {
					PDBCoordEntryFile entryFile = (mPDBCode != null) ? parser.getFromPDB(mPDBCode) : parser.parse(pdbFile);
					List<Molecule3D> ligands = entryFile.extractMols().get(StructureAssembler.LIGAND_GROUP);

					if (ligands == null || ligands.isEmpty()) {
						ligands = entryFile.extractMols(true).get(StructureAssembler.LIGAND_GROUP);
						if (ligands == null || ligands.isEmpty())
							showMessageInEDT("No ligand structure found in "+(mPDBCode != null ? "PDB entry '"+mPDBCode : "'"+pdbFile.getName())+"'.");
						else
							showMessageInEDT("Only covalent ligand(s) were found and disconnected from the protein structure.");
					}
					if (ligands != null && !ligands.isEmpty()) {
						int index = -1;
						if (ligands.size() == 1) {
							index = 0;
						}
						else {
							String[] ligandName = new String[ligands.size()];
							for (int i=0; i<ligands.size(); i++) {
								String formula = new MolecularFormula(ligands.get(i)).getFormula();
								ligandName[i] = (i + 1) + ": " + formula + "; " + (ligands.get(i).getName() == null ? "Unnamed" : ligands.get(i).getName());
							}
							String name = (String)JOptionPane.showInputDialog(mConformerPanel, "Select one of multiple ligands:", "Select Ligand", JOptionPane.QUESTION_MESSAGE, null, ligandName, ligandName[0]);
							if (name != null)
								index = Integer.parseInt(name.substring(0, name.indexOf(':')))-1;
						}

						if (index != -1 && ligands.get(index).getAllAtoms() != 0) {
							final Molecule3D ligand = ligands.get(index);
							Platform.runLater(() -> {
								V3DScene scene = mConformerPanel.getV3DScene();

								ligand.center();
								scene.addMolecule(new V3DMolecule(ligand, MoleculeArchitect.ConstructionMode.BALL_AND_STICKS, MoleculeArchitect.HydrogenMode.ALL, 0, V3DMolecule.MoleculeRole.LIGAND, true));

								scene.optimizeView();
							});
						}
					}
				}
				catch (Exception e) {
					showMessageInEDT(e.getMessage());
					e.printStackTrace();
					}
				} );
			}
		catch (Exception ie) {}
		}

/*	private void loadPDBLigand() {      MMTF is not supported anymore from July 2nd, 2024
		mPDBCode = null;
		try {
			SwingUtilities.invokeLater(() -> {
				mPDBCode = JOptionPane.showInputDialog(mConformerPanel, "PDB Entry Code?");
				if (mPDBCode == null || mPDBCode.isEmpty())
					return;

				Molecule3D[] mol = MMTFParser.getStructureFromName(mPDBCode, MMTFParser.MODE_SPLIT_CHAINS);
				if (mol == null)
					return;

				MMTFParser.centerMolecules(mol);

				Platform.runLater(() -> {
					V3DScene scene = mConformerPanel.getV3DScene();

					int count = 0;
					for (int i=0; i<mol.length; i++) {
						if (mol[i].getAllBonds() != 0 && mol[i].getAllAtoms() < 100) {
							if (mol.length == 1)
								mol[0].center();
							scene.addMolecule(new V3DMolecule(mol[i], MoleculeArchitect.ConstructionMode.BALL_AND_STICKS, MoleculeArchitect.HydrogenMode.ALL, 0, V3DMolecule.MoleculeRole.LIGAND, true));
							count++;
							}
						}

					if (count == 0)
						showMessageInEDT("No ligand structure found in PDB entry.");
					else if (count > 1)
						showMessageInEDT("Multiple ligand structures found.\nRemove all but one for proper results.");

					scene.optimizeView();
					});
				});
			}
		catch (Exception ie) {}
		} */
	}
