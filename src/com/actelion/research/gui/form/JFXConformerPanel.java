package com.actelion.research.gui.form;

import com.actelion.research.chem.StereoMolecule;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.openmolecules.fx.surface.SurfaceMesh;
import org.openmolecules.fx.viewer3d.*;
import org.openmolecules.mesh.MoleculeSurfaceAlgorithm;
import sun.awt.AppContext;
import sun.awt.SunToolkit;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.concurrent.CountDownLatch;

public class JFXConformerPanel extends JFXPanel {
	private V3DScene mScene;
	private StereoMolecule mRefMol;
	private V3DPopupMenuController mController;

	public JFXConformerPanel(boolean withSidePanel, boolean synchronousRotation, boolean allowEditing) {
		this(withSidePanel, 512, 384, synchronousRotation, allowEditing);
	}

	public JFXConformerPanel(boolean withSidePanel, int width, int height, boolean synchronousRotation, boolean allowEditing) {
		super();
		Platform.runLater(() -> {
			Scene scene;

			EnumSet<V3DScene.ViewerSettings> settings = V3DScene.CONFORMER_VIEW_MODE;
			if (allowEditing)
				settings.add(V3DScene.ViewerSettings.EDITING);

			if (withSidePanel) {
				V3DSceneWithSidePane sceneWithSidePanel = new V3DSceneWithSidePane(width, height, settings);
				sceneWithSidePanel.getMoleculePanel().initialize(false);
				mScene = sceneWithSidePanel.getScene3D();
				mScene.setIndividualRotationModus(synchronousRotation);
				scene = new Scene(sceneWithSidePanel, width, height, true, SceneAntialiasing.BALANCED);
			}
			else {
				mScene = new V3DScene(new Group(), width, height, settings);
				mScene.setIndividualRotationModus(synchronousRotation);
				V3DSceneWithSelection sws = new V3DSceneWithSelection(mScene);
				scene = new Scene(sws, width, height, true, SceneAntialiasing.BALANCED);
			}

			String css = getClass().getResource("/resources/molviewer.css").toExternalForm();
			scene.getStylesheets().add(css);
			mScene.widthProperty().bind(scene.widthProperty());
			mScene.heightProperty().bind(scene.heightProperty());
			mScene.setPopupMenuController(mController);
			setScene(scene);
		} );
	}

	public V3DScene getV3DScene() {
		return mScene;
	}

	/**
	 * Must be called directly after instantiation
	 * @param controller
	 */
	public void setPopupMenuController(V3DPopupMenuController controller) {
		mController = controller;
	}

	// this fixes an issue, where the JFXPanel, if not in focus does not properly handle popup menus
	// This is supposed to be fixed in Java9. Thus remove this when moving to Java9 or later. TODO
	@Override
	protected void processMouseEvent(MouseEvent e) {
		try {
			if ((e.getID() == MouseEvent.MOUSE_PRESSED)&& (e.getButton() != MouseEvent.BUTTON1)) {
				if (!hasFocus()) {
					requestFocus();
					AppContext context = SunToolkit.targetToAppContext(this);
					if (context != null) {
						SunToolkit.postEvent(context, e);
					}
				}
			}
		} catch (Exception ex) {}
		super.processMouseEvent(e);
	}

	public void clear() {
		Platform.runLater(() -> {
			mScene.clearAll(true);
			if (mRefMol != null && mRefMol.getAllAtoms() != 0) {
				V3DMolecule fxmol = new V3DMolecule(mRefMol);
				fxmol.setColor(Color.LIGHTGRAY);
				mScene.addMolecule(fxmol);
			}
		} );
	}

	public void optimizeView() {
		Platform.runLater(() -> mScene.optimizeView() );
	}

	@Override
	public void setBackground(java.awt.Color bg) {
		Platform.runLater(() -> {
			int r = bg.getRed();
			int g = bg.getGreen();
			int b = bg.getBlue();
			mScene.setFill(Color.rgb(r, g, b, 1));
		} );
	}

	public void setConformerSplitting(double value) {
		Platform.runLater(() -> {
			int count = 0;
			double molSize = 0;
			for (Node node:mScene.getWorld().getChildren()) {
				if (node instanceof V3DMolecule) {
					molSize = Math.max(molSize, 2 * Math.sqrt(((V3DMolecule)node).getMolecule().getAllAtoms()));
					count++;
				}
			}

			double maxLineWidth = 0.01 + molSize * Math.max(2.0, Math.round(1.2 * Math.sqrt(count)));
			int maxPerLine = (value == 0) ? count : (int)(1.0 + (maxLineWidth - molSize) / (value * molSize));
			int lineCount = (count == 0) ? 0 : 1 + (count - 1) / maxPerLine;
			int countPerLine = (count == 0) ? 0 : 1 + (count - 1) / lineCount;

			double x = 0.5;
			double y = 0.5;
			for (Node node:mScene.getWorld().getChildren()) {
				if (node instanceof V3DMolecule) {
					node.setTranslateX((x-(double)countPerLine/2) * molSize * value);
					node.setTranslateY((y-(double)lineCount/2) * molSize);
					x += 1;
					if (x > countPerLine) {
						x = 0.5;
						y += 1.0;
					}
				}
			}
		} );
	}

	public void setConollySurfaceMode(int mode) {
		Platform.runLater(() -> {
			V3DMolGroup molGroup = mScene.getWorld();
			for (Node node:mScene.getWorld().getAllChildren())
				if (node instanceof V3DMolecule)
					((V3DMolecule)node).setSurfaceMode(MoleculeSurfaceAlgorithm.CONNOLLY, V3DMolecule.SurfaceMode.values()[mode]);
		} );
	}

	public void setReferenceMolecule(StereoMolecule refmol) {
		mRefMol = refmol;
		}

	public void addMolecule(StereoMolecule mol, Color color, Point3D centerOfRotation) {
		Platform.runLater(() -> {
			V3DMolecule fxmol = new V3DMolecule(mol);
			if (color != null)
				fxmol.setColor(color);
			fxmol.setSurfaceColorMode(MoleculeSurfaceAlgorithm.CONNOLLY, SurfaceMesh.SURFACE_COLOR_INHERIT);
			fxmol.setCenterOfRotation(centerOfRotation);
			mScene.addMolecule(fxmol);
		} );
	}

	public ArrayList<StereoMolecule> getConformers() {
		final ArrayList<StereoMolecule> conformerList = new ArrayList<>();

		final CountDownLatch latch = new CountDownLatch(1);

		Platform.runLater(() -> {
			for (Node node:mScene.getWorld().getChildren())
				if (node instanceof V3DMolecule)
					conformerList.add(((V3DMolecule)node).getMolecule());
			latch.countDown();
		} );

		try { latch.await(); } catch (InterruptedException ie) {}
		return conformerList;
	}

	public WritableImage getContentImage() {
		final ArrayList<WritableImage> imageList = new ArrayList<>();

		final CountDownLatch latch = new CountDownLatch(1);

		Platform.runLater(() -> {
			WritableImage image = mScene.snapshot(null, null);
			imageList.add(image);
			latch.countDown();
		} );

		try { latch.await(); } catch (InterruptedException ie) {}

		return imageList.isEmpty() ? null : imageList.get(0);
	}
}
