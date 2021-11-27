package com.actelion.research.datawarrior.task.chem.elib;

import com.actelion.research.chem.*;
import com.actelion.research.chem.conf.AtomAssembler;
import com.actelion.research.chem.io.Mol2FileParser;
import com.actelion.research.chem.io.pdb.parser.PDBFileParser;
import com.actelion.research.chem.io.pdb.parser.StructureAssembler;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.form.JFXConformerPanel;
import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.paint.Color;
import org.openmolecules.fx.surface.SurfaceMesh;
import org.openmolecules.fx.viewer3d.*;
import org.openmolecules.mesh.MoleculeSurfaceAlgorithm;
import org.openmolecules.pdb.MMTFParser;
import org.openmolecules.render.MoleculeArchitect;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DockingPanelController implements V3DPopupMenuController {
	private JFXConformerPanel mConformerPanel;
	private StereoMolecule mProtein,mLigand;

	public DockingPanelController(JFXConformerPanel conformerPanel) {
		mConformerPanel = conformerPanel;
		Platform.runLater(() ->
			mConformerPanel.getV3DScene().addSceneListener(new V3DSceneListener() {
				@Override
				public void addMolecule(V3DMolGroup fxmol) {}

				@Override
				public void removeMolecule(V3DMolGroup fxmol) {
					if (fxmol instanceof V3DMolecule) {
						if (((V3DMolecule)fxmol).getRole() == V3DMolecule.MoleculeRole.MACROMOLECULE)
							mProtein = null;
						else
							mLigand = null;
					}
				}

				@Override
				public void initialize() {
					mProtein = null;
					mLigand = null;
				}
		} ) );
	}

	@Override
	public void addExternalMenuItems(ContextMenu popup, int type) {
		if (type == V3DPopupMenuController.TYPE_FILE) {
			javafx.scene.control.MenuItem itemLoadPDBFile = new javafx.scene.control.MenuItem("Load Protein Cavity From PDB-File...");
			itemLoadPDBFile.setOnAction(e -> loadFromPDBFile());

			javafx.scene.control.MenuItem itemLoadPDBDB = new javafx.scene.control.MenuItem("Load Protein Cavity From PDB Database...");
			itemLoadPDBDB.setOnAction(e -> loadFromPDBDatabase());

			javafx.scene.control.MenuItem itemLoadProtein = new javafx.scene.control.MenuItem("Load Protein From Molfile/Mol2-File...");
			itemLoadProtein.setOnAction(e -> loadFromMolOrMol2File(true));

			javafx.scene.control.MenuItem itemLoadLigand = new javafx.scene.control.MenuItem("Load Natural Ligand From Molfile/Mol2-File...");
			itemLoadLigand.setOnAction(e -> loadFromMolOrMol2File(false));

			popup.getItems().add(itemLoadPDBFile);
			popup.getItems().add(itemLoadPDBDB);
			popup.getItems().add(itemLoadProtein);
			popup.getItems().add(itemLoadLigand);
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
		try {
			SwingUtilities.invokeLater(() -> {
				String pdbCode = JOptionPane.showInputDialog(mConformerPanel, "PDB Entry Code?");
				if (pdbCode == null || pdbCode.length() == 0)
					return;

				Molecule3D[] mol = MMTFParser.getStructureFromName(pdbCode, MMTFParser.MODE_SPLIT_CHAINS);
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
		if (proteins == null || proteins.size() == 0) {
			JOptionPane.showMessageDialog(mConformerPanel, "No proteins found in file.");
		}
		else {
			mProtein = proteins.get(0);
			mLigand = null;
			for (int i=1; i<proteins.size(); i++)
				mProtein.addMolecule(proteins.get(i));

			if (ligands != null && ligands.size() != 0) {
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

				if ((index != -1))
					mLigand = ligands.get(index);
			}

			addAllToScene();
		}
	}

	private void addAllToScene() {
		// if we have both, protein and ligand, then we can crop, center and add all to emptied scene
		if (mProtein != null && mLigand != null) {
			Coordinates cog = JFXConformerPanel.calculateCOG(mLigand);
			final StereoMolecule _cavity = JFXConformerPanel.cropProtein(mProtein, mLigand, cog);
			final StereoMolecule _ligand = new StereoMolecule(mLigand);

			_ligand.translate(-cog.x, -cog.y, -cog.z);
			_cavity.translate(-cog.x, -cog.y, -cog.z);

			new AtomAssembler(_ligand).addImplicitHydrogens();
			new AtomAssembler(_cavity).addImplicitHydrogens();

			Platform.runLater(() -> {
				V3DScene scene = mConformerPanel.getV3DScene();
				scene.clearAll();
				mConformerPanel.setProteinCavity(_cavity, _ligand);
				scene.addMolecule(new V3DMolecule(_ligand, 0, V3DMolecule.MoleculeRole.LIGAND));
				scene.optimizeView();
			});
		}
		// if we have only the protein, we don't add hydrogen, but surface and just add the protein to the scene
		else if (mProtein != null) {
			Platform.runLater(() -> {
				V3DScene scene = mConformerPanel.getV3DScene();

				List<V3DMolecule> fxmols = scene.getMolsInScene();
				for (V3DMolecule fxmol : fxmols) {
					if (fxmol.getRole() == V3DMolecule.MoleculeRole.MACROMOLECULE) {
						scene.removeMeasurements(fxmol);
						scene.delete(fxmol);
					}
				}

				V3DMolecule protein = new V3DMolecule(new StereoMolecule(mProtein), MoleculeArchitect.ConstructionMode.WIRES, 0, V3DMolecule.MoleculeRole.MACROMOLECULE);
				protein.setColor(Color.LIGHTGRAY);
				protein.setSurfaceMode(MoleculeSurfaceAlgorithm.CONNOLLY, V3DMolecule.SurfaceMode.FILLED);
				protein.setSurfaceColorMode(MoleculeSurfaceAlgorithm.CONNOLLY, SurfaceMesh.SURFACE_COLOR_ATOMIC_NOS);
				scene.addMolecule(protein);

				scene.optimizeView();
			});
		}
		// if we have only the ligand, we add hydrogen and add the ligand then to the scene
		else {
			final StereoMolecule ligand = new StereoMolecule(mLigand);
			new AtomAssembler(ligand).addImplicitHydrogens();

			Platform.runLater(() -> {
				V3DScene scene = mConformerPanel.getV3DScene();
				List<V3DMolecule> fxmols = scene.getMolsInScene();
				for (V3DMolecule fxmol:fxmols) {
					if (fxmol.getRole() == V3DMolecule.MoleculeRole.LIGAND) {
						scene.removeMeasurements(fxmol);
						scene.delete(fxmol);
					}
				}

				scene.addMolecule(new V3DMolecule(ligand, 1, V3DMolecule.MoleculeRole.LIGAND));
				scene.optimizeView();
			});
		}
	}

	private void loadFromMolOrMol2File(boolean isProtein) {
		SwingUtilities.invokeLater(() -> {
			int fileTypes = FileHelper.cFileTypeMOL | FileHelper.cFileTypeMOL2;
			String title = isProtein ? "Open Protein File" : "Open Natural Ligand File";
			File selectedFile = FileHelper.getFile(mConformerPanel, title, fileTypes);
			if (selectedFile != null) {
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
					if (isProtein)
						mProtein = mol;
					else
						mLigand = mol;

					addAllToScene();
				}
			}
		});
	}
}
