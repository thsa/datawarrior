package com.actelion.research.gui.form;

import com.actelion.research.chem.Molecule3D;
import com.actelion.research.chem.io.pdb.parser.PDBFileParser;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.layout.Pane;
import org.openmolecules.fx.surface.SurfaceMesh;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DScene;
import org.openmolecules.render.MoleculeArchitect;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

public class JFXPDBViewer extends JFXPanel {
	private V3DScene mScene;
	private volatile Runnable mLatestRunnable;

	public JFXPDBViewer () {
		Platform.runLater(new Runnable() {
			@Override
			public void run () {
				Group root = new Group();
				mScene = new V3DScene(root, 512, 384, V3DScene.GENERAL_MODE);

				Pane pane = new Pane();
				pane.getChildren().add(mScene);

				String css = getClass().getResource("/resources/molviewer.css").toExternalForm();
				Scene scene = new Scene(pane, 512, 384, true, SceneAntialiasing.BALANCED);
				scene.getStylesheets().add(css);
				mScene.widthProperty().bind(scene.widthProperty());
				mScene.heightProperty().bind(scene.heightProperty());
				setScene(scene);
			}
		});
	}

	public void setPDBData(final byte[] pdbData) {
		mLatestRunnable = new Runnable() {
			@Override
			public void run () {
				if (this == mLatestRunnable) {
					PDBFileParser parser = new PDBFileParser();
					try {
						Molecule3D mol = null;
						if (this == mLatestRunnable && pdbData != null) {
							System.out.print("loading data...");
//							Map<String, List<Molecule3D>> molMap = parser.parse(new File("/Users/thomas/Documents/data/pdb/reninOpenFlap.pdb")).extractMols();
							Map<String, List<Molecule3D>> molMap = parser.parse(new BufferedReader(new StringReader(new String(pdbData)))).extractMols();
							mol = new Molecule3D();
							for (List<Molecule3D> l:molMap.values())
								for (Molecule3D m:l)
									mol.addMolecule(m);

							System.out.println("done");
							}
						V3DMolecule vm = null;
						if (this == mLatestRunnable && pdbData != null) {
							vm = new V3DMolecule(mol, MoleculeArchitect.ConstructionMode.WIRES, 0, V3DMolecule.MoleculeRole.MACROMOLECULE);
							vm.setSurface(0, V3DMolecule.SurfaceMode.FILLED, SurfaceMesh.SURFACE_COLOR_ATOMIC_NOS, 0.5);
//							vm.activateEvents();
							}
						if (this == mLatestRunnable) {
							System.out.print("adding molecule to scene...  ");
							mScene.deleteAllMolecules();
							if (vm != null)
								mScene.addMolecule(vm);
							System.out.println("done");
							System.out.println("scene3D.width:" + mScene.getWidth() + " scene3D.height:" + mScene.getHeight());
							}
						}
					catch (Exception e) { e.printStackTrace(); }
					}
				}
			};
		Platform.runLater(mLatestRunnable);
		}
	}
