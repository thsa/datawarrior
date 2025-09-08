package com.actelion.research.datawarrior.fx;

import com.actelion.research.chem.MolecularFormula;
import com.actelion.research.chem.Molecule3D;
import com.actelion.research.chem.MolfileParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.HydrogenAssembler;
import com.actelion.research.chem.io.Mol2FileParser;
import com.actelion.research.chem.io.pdb.mmcif.MMCIFParser;
import com.actelion.research.chem.io.pdb.parser.PDBFileEntry;
import com.actelion.research.chem.io.pdb.parser.PDBFileParser;
import com.actelion.research.chem.io.pdb.parser.StructureAssembler;
import com.actelion.research.gui.FileHelper;
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

public class EditableSmallMolMenuController implements V3DPopupMenuController {
	private final Frame mParentFrame;
	private final JFXMolViewerPanel mConformerPanel;
	private volatile String mPDBCode;
	private javafx.scene.paint.Color mMoleculeColor;

	/**
	 * This controller adds menu items to change and add  conformers to the associated 3D-view.
	 */
	public EditableSmallMolMenuController(Frame owner, JFXMolViewerPanel conformerPanel) {
		mParentFrame = owner;
		mConformerPanel = conformerPanel;
		}

	public void setMoleculeColor(javafx.scene.paint.Color c) {
		mMoleculeColor = c;
		}

	@Override
	public void addExternalMenuItems(ContextMenu popup, int type) {
		if (type == V3DPopupMenuController.TYPE_FILE) {
			javafx.scene.control.MenuItem itemLoadMolecule = new javafx.scene.control.MenuItem("Load Conformer from Molfile/Mol2-File...");
			itemLoadMolecule.setOnAction(e -> loadMolecule());

			javafx.scene.control.MenuItem itemLoadMolecules = new javafx.scene.control.MenuItem("Load Conformer(s) from DataWarrior-/SD-File...");
			itemLoadMolecules.setOnAction(e -> loadMolecules());

			javafx.scene.control.MenuItem itemLoadPDBFile = new javafx.scene.control.MenuItem("Load Ligand from PDB/MMCIF File...");
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
	public void markCropDistanceForSurface(V3DMolecule fxmol, int type, int mode) {}

	private void loadMolecule() {
		SwingUtilities.invokeLater(() -> {
			int fileTypes = FileHelper.cFileTypeMOL | FileHelper.cFileTypeMOL2;
			File selectedFile = FileHelper.getFile(mParentFrame, "Open 3D-Molecule(s)", fileTypes);
			if (selectedFile != null) {
				Platform.runLater(() -> {
					StereoMolecule mol = null;
					if (FileHelper.getFileType(selectedFile) == FileHelper.cFileTypeMOL) {
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
						new HydrogenAssembler(mol).addImplicitHydrogens();
						mol.center();
						V3DScene scene = mConformerPanel.getV3DScene();
						V3DMolecule mol3D = new V3DMolecule(mol, true, scene.isSplitAllBonds());
						if (mMoleculeColor != null)
							mol3D.setColor(mMoleculeColor);
						scene.clearAll();
						scene.addMolecule(mol3D, false);
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
					scene.clearAll();
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
					if (mPDBCode != null && mPDBCode.trim().isEmpty())
						mPDBCode = null;
					if (mPDBCode == null)
						return;
				}
				else {
					pdbFile = FileHelper.getFile(mConformerPanel, "Choose PDB/MMCIF-File", FileHelper.cFileTypePDB | FileHelper.cFileTypeMMCIF);
					if (pdbFile == null)
						return;
				}

				try {
					PDBFileEntry entryFile;
					if (mPDBCode != null) {
						try {
							entryFile = MMCIFParser.getFromPDB(mPDBCode.trim());
						}
						catch (Exception e) {
							showMessageInEDT("Couldn't retrieve pdb-file of '"+mPDBCode.trim()+"' from PDB-database:\n"+e.getMessage()+"\nYou may try to download it manually from 'www.rcsb.org'.");
							e.printStackTrace();
							return;
						}
					}
					else {
						entryFile = FileHelper.getFileType(pdbFile) == FileHelper.cFileTypePDB ?
								new PDBFileParser().parse(pdbFile) : MMCIFParser.parse(pdbFile);
					}

					List<Molecule3D> ligands = entryFile.extractMols(true).get(StructureAssembler.LIGAND_GROUP);
					int covalentCount = 0;
					if (ligands != null)
						for (Molecule3D ligand : ligands)
							if (ligand.isCovalentLigand())
								covalentCount++;

					if (covalentCount != 0)
						showMessageInEDT(covalentCount+" of "+ligands.size()+" ligands were covalently bound and disconnected from the protein structure.");

					if (ligands != null && !ligands.isEmpty()) {
						int index = -1;
						if (ligands.size() == 1) {
							index = 0;
						}
						else {
							String[] ligandName = new String[ligands.size()];
							ligands.sort((o1, o2) -> Integer.compare(o2.getAllAtoms(), o1.getAllAtoms()));
							for (int i=0; i<ligands.size(); i++) {
								Molecule3D ligand = ligands.get(i);
								String formula = " " + new MolecularFormula(ligand).getFormula();
								String covalent = ligand.isCovalentLigand() ? " (covalent)" : "";
								ligandName[i] = (i+1) + ": " + (ligands.get(i).getName() == null ? "Unnamed" : ligands.get(i).getName()) + formula + covalent;
							}
							String name = (String)JOptionPane.showInputDialog(mConformerPanel, "Select one of multiple ligands:", "Select Ligand", JOptionPane.QUESTION_MESSAGE, null, ligandName, ligandName[0]);
							if (name != null)
								index = Integer.parseInt(name.substring(0, name.indexOf(':')))-1;
						}

						if (index != -1 && ligands.get(index).getAllAtoms() != 0) {
							final Molecule3D ligand = ligands.get(index);
							Platform.runLater(() -> {
								new HydrogenAssembler(ligand).addImplicitHydrogens();

								V3DMolecule mol3D = new V3DMolecule(ligand, MoleculeArchitect.CONSTRUCTION_MODE_STICKS, MoleculeArchitect.HYDROGEN_MODE_ALL, 0, V3DMolecule.MoleculeRole.LIGAND, true, false);
								if (mMoleculeColor != null)
									mol3D.setColor(mMoleculeColor);

								V3DScene scene = mConformerPanel.getV3DScene();
								scene.clearAll();
								scene.addMolecule(mol3D, false);
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
	}
