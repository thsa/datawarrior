package com.actelion.research.datawarrior.task.chem.elib;

import com.actelion.research.chem.Molecule3D;
import com.actelion.research.chem.MolfileParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.Mol2FileParser;
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
import org.openmolecules.pdb.MMTFParser;
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

			javafx.scene.control.MenuItem itemLoadPDBFile = new javafx.scene.control.MenuItem("Load Ligand from PDB-File...");
			itemLoadPDBFile.setOnAction(e -> loadPDBFile());

			javafx.scene.control.MenuItem itemLoadPDBDBLigand = new javafx.scene.control.MenuItem("Load Ligand From PDB Database...");
			itemLoadPDBDBLigand.setOnAction(e -> loadPDBLigand());

			popup.getItems().add(itemLoadMolecule);
			popup.getItems().add(itemLoadMolecules);
			popup.getItems().add(itemLoadPDBFile);
			popup.getItems().add(itemLoadPDBDBLigand);
			popup.getItems().add(new SeparatorMenuItem());
			}
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

	private void loadPDBFile() {
		SwingUtilities.invokeLater(() -> {
			File selectedFile = FileHelper.getFile(mConformerPanel, "Choose PDB-File", FileHelper.cFileTypePDB);
			if (selectedFile != null) {
				Platform.runLater(() -> {
					PDBFileParser parser = new PDBFileParser();
					V3DScene scene = mConformerPanel.getV3DScene();
					try {
						List<Molecule3D> ligands = parser.parse(selectedFile).extractMols().get(StructureAssembler.LIGAND_GROUP);
						if (ligands == null || ligands.size() == 0) {
							showMessageInEDT("No ligands found in file.");
							}
						else if (ligands.size() == 1) {
							Molecule3D mol = ligands.get(0);
							mol.center();
							scene.addMolecule(new V3DMolecule(mol));
							}
						else {
							for (Molecule3D mol : ligands)
								scene.addMolecule(new V3DMolecule(mol));
							}
					} catch (Exception e) {
						showMessageInEDT(e.getMessage());
						e.printStackTrace();
						}
					} );
				}
			} );
		}

	private void loadPDBLigand() {
		mPDBCode = null;
		try {
			SwingUtilities.invokeLater(() -> {
				mPDBCode = JOptionPane.showInputDialog(mConformerPanel, "PDB Entry Code?");
				if (mPDBCode == null || mPDBCode.length() == 0)
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
		}
	}
