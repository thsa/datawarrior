package com.actelion.research.datawarrior.fx;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.gui.hidpi.HiDPIIcon;
import com.actelion.research.util.DoubleFormat;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import org.openmolecules.fx.surface.SurfaceMesh;
import org.openmolecules.fx.viewer3d.*;
import org.openmolecules.fx.viewer3d.nodes.Ribbons;
import org.openmolecules.mesh.MoleculeSurfaceAlgorithm;
import org.openmolecules.render.MoleculeArchitect;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;

public class JFXMolViewerPanel extends JFXPanel {
	public static final double CAVITY_CROP_DISTANCE = 10.0;
	private static boolean sURLStreamHandlerSet = false;
	private static final String EDIT_MESSAGE = "<right mouse click to add content>";
	private static final Color DEFAULT_CAVITY_MOL_COLOR = Color.LIGHTGRAY;
	private static final Color DEFAULT_REFMOL_COLOR = Color.INDIANRED;
	private static final Color DEFAULT_OVERLAY_MOL_COLOR = Color.LIGHTGRAY;
	private static final Color DEFAULT_SINGLE_CONF_COLOR = Color.GRAY.darker(); // for some reason, DARKGREY is lighter than GREY

	private V3DScene mScene;
	private volatile V3DMolecule mCavityMol,mOverlayMol,mRefMol,mSingleConformer;
	private V3DPopupMenuController mController;
	private FutureTask<Object> mConstructionTask;
	private volatile int mCurrentUpdateID,mCavityConstructionMode,mCavityHydrogenMode,mCavityRibbonMode,mCavitySurfaceMode,mCavitySurfaceColorMode;
	private boolean mAdaptToLookAndFeelChanges;
	private final boolean mIsEditable;
	private volatile Color mCavityMolColor,mRefMolColor,mOverlayMolColor,mSingleConformerColor,mCavitySurfaceColor;
	private volatile double mCavitySurfaceTransparency;
	private java.awt.Color mSceneBackground, mLookAndFeelSpotColor,
			mMenuItemBackground,mMenuItemForeground,/*mMenuItemSelectionBackground,*/mMenuItemSelectionForeground;
	private Vector<StructureChangeListener> mListeners;

	public JFXMolViewerPanel(boolean withSidePanel) {
		this(withSidePanel, 512, 384, V3DScene.CONFORMER_VIEW_MODE);
	}

	public JFXMolViewerPanel(boolean withSidePanel, EnumSet<V3DScene.ViewerSettings> settings) {
		this(withSidePanel, 512, 384, settings);
	}

	public JFXMolViewerPanel(boolean withSidePanel, int width, int height, EnumSet<V3DScene.ViewerSettings> settings) {
		super();
		if (!sURLStreamHandlerSet) {
			URL.setURLStreamHandlerFactory(new StringURLStreamHandlerFactory());
			sURLStreamHandlerSet = true;
		}

		mIsEditable = settings.contains(V3DScene.ViewerSettings.EDITING);

		mCavityMolColor = DEFAULT_CAVITY_MOL_COLOR;
		mOverlayMolColor = DEFAULT_OVERLAY_MOL_COLOR;
		mRefMolColor = DEFAULT_REFMOL_COLOR;
		mSingleConformerColor = null;

		mCavityConstructionMode = MoleculeArchitect.CONSTRUCTION_MODE_WIRES;
		mCavityHydrogenMode = MoleculeArchitect.HYDROGEN_MODE_ALL;
		mCavityRibbonMode = Ribbons.MODE_NONE;
		mCavitySurfaceMode = V3DMolecule.SURFACE_MODE_FILLED;
		mCavitySurfaceColorMode = SurfaceMesh.SURFACE_COLOR_ATOMIC_NOS;
		mCavitySurfaceColor = DEFAULT_CAVITY_MOL_COLOR;
		mCavitySurfaceTransparency = 0.2;

		collectLookAndFeelColors(); // we have to do this on the EDT

		mConstructionTask = new FutureTask<>(() -> {
			Scene scene;
			if (withSidePanel) {
				V3DSceneWithSidePane sceneWithSidePanel = new V3DSceneWithSidePane(width, height, settings);
				mScene = sceneWithSidePanel.getScene3D();
				scene = new Scene(sceneWithSidePanel, width, height, true, SceneAntialiasing.BALANCED);
			} else {
				mScene = new V3DScene(new Group(), width, height, settings);
				V3DSceneWithSelection sws = new V3DSceneWithSelection(mScene);
				scene = new Scene(sws, width, height, true, SceneAntialiasing.BALANCED);
			}
			setScene(scene);

			setLookAndFeelMenuBackground();
			mScene.widthProperty().bind(scene.widthProperty());
			mScene.heightProperty().bind(scene.heightProperty());
			mScene.setPopupMenuController(mController);
		}, null);
		Platform.runLater(mConstructionTask);
	}

	/**
	 * Call this before a set of calls replaces the content of this panel.
	 * This allows skipping sets of calls if a newer set of calles is already cued
	 * to replace the panels content again.
	 */
	public void increaseUpdateID() {
		mCurrentUpdateID++;
	}

	public void addStructureChangeListener(StructureChangeListener l) {
		if (mListeners == null)
			mListeners = new Vector<>();

		mListeners.add(l);
	}

	public void removeStructureChangeListener(StructureChangeListener l) {
		if (mListeners != null)
			mListeners.remove(l);
	}

	private void fireStructureChanged() {
		if (mListeners != null)
			for (StructureChangeListener l : mListeners)
				l.structureChanged();
	}

	/**
	 * Generates and returns a string encoding of the geometry as camera position and world rotation.
	 * @return valid encoding of geometry
	 */
	public String getGeometry() {
		StringBuilder config = new StringBuilder();

		config.append(DoubleFormat.toString(mScene.getCamera().getTranslateX())).append(";");
		config.append(DoubleFormat.toString(mScene.getCamera().getTranslateY())).append(";");
		config.append(DoubleFormat.toString(mScene.getCamera().getTranslateZ())).append(";");

		Transform t = mScene.getWorld().getRotation();
		config.append(DoubleFormat.toString(t.getMxx())).append(";");
		config.append(DoubleFormat.toString(t.getMxy())).append(";");
		config.append(DoubleFormat.toString(t.getMxz())).append(";");
		config.append(DoubleFormat.toString(t.getTx())).append(";");
		config.append(DoubleFormat.toString(t.getMyx())).append(";");
		config.append(DoubleFormat.toString(t.getMyy())).append(";");
		config.append(DoubleFormat.toString(t.getMyz())).append(";");
		config.append(DoubleFormat.toString(t.getTy())).append(";");
		config.append(DoubleFormat.toString(t.getMzx())).append(";");
		config.append(DoubleFormat.toString(t.getMzy())).append(";");
		config.append(DoubleFormat.toString(t.getMzz())).append(";");
		config.append(DoubleFormat.toString(t.getTz())).append(";");

		config.append(DoubleFormat.toString(mScene.getWorld().getTranslateX())).append(";");
		config.append(DoubleFormat.toString(mScene.getWorld().getTranslateY())).append(";");
		config.append(DoubleFormat.toString(mScene.getWorld().getTranslateZ()));

		return config.toString();
	}

	/**
	 * Takes the string encoding geometry as camera position and world rotation.
	 * @param config valid encoding of custom configuration
	 */
	public void setGeometry(String config) {
		Platform.runLater(() -> {
			String[] value = config.split(";");

			int index = 0;
			mScene.setCameraXY(Double.parseDouble(value[index++]), Double.parseDouble(value[index++]));
			mScene.setCameraZ(Double.parseDouble(value[index++]));

			mScene.getWorld().setTransform(Transform.affine(
					Double.parseDouble(value[index++]),
					Double.parseDouble(value[index++]),
					Double.parseDouble(value[index++]),
					Double.parseDouble(value[index++]),
					Double.parseDouble(value[index++]),
					Double.parseDouble(value[index++]),
					Double.parseDouble(value[index++]),
					Double.parseDouble(value[index++]),
					Double.parseDouble(value[index++]),
					Double.parseDouble(value[index++]),
					Double.parseDouble(value[index++]),
					Double.parseDouble(value[index++])));

			mScene.getWorld().setTranslateX(Double.parseDouble(value[index++]));
			mScene.getWorld().setTranslateY(Double.parseDouble(value[index++]));
			mScene.getWorld().setTranslateZ(Double.parseDouble(value[index++]));
		} );
	}

	public boolean isAnimate() {
		return mScene.isAnimate();
	}

	public void setAnimate(boolean b) {
		Platform.runLater(() -> mScene.setAnimate(b));
	}

	public String getOverlayMolColor() {
		Color color = (mOverlayMol != null) ? mOverlayMol.getColor() : mOverlayMolColor;
		return color == null ? "none" : toRGBString(color);
	}

	public String getCavityMolColor() {
		Color color = (mCavityMol != null) ? mCavityMol.getColor() : mCavityMolColor;
		return color == null ? "none" : toRGBString(color);
	}

	public String getCavitySurfaceColor() {
		Color color = (mCavityMol != null) ? mCavityMol.getSurfaceColor(MoleculeSurfaceAlgorithm.CONNOLLY) : mCavitySurfaceColor;
		return color == null ? "none" : toRGBString(color);
	}

	public int getCavityConstructionMode() {
		return mCavityMol != null ? mCavityMol.getConstructionMode() : mCavityConstructionMode;
	}

	public int getCavityHydrogenMode() {
		return mCavityMol != null ? mCavityMol.getHydrogenMode() : mCavityHydrogenMode;
	}

	public int getCavityRibbonMode() {
		return mCavityMol != null ? mCavityMol.getRibbonMode() : mCavityRibbonMode;
	}

	public int getCavitySurfaceMode() {
		return mCavityMol != null ? mCavityMol.getSurfaceMode(MoleculeSurfaceAlgorithm.CONNOLLY) : mCavitySurfaceMode;
	}

	public int getCavitySurfaceColorMode() {
		return mCavityMol != null ? mCavityMol.getSurfaceColorMode(MoleculeSurfaceAlgorithm.CONNOLLY) : mCavitySurfaceColorMode;
	}

	public double getCavitySurfaceTransparency() {
		return mCavityMol != null ? mCavityMol.getSurfaceTransparency(MoleculeSurfaceAlgorithm.CONNOLLY) : mCavitySurfaceTransparency;
	}

	public String getRefMolColor() {
		Color color = (mRefMol != null) ? mRefMol.getColor() : mRefMolColor;
		return color == null ? "none" : toRGBString(color);
	}

	public String getSingleConformerColor() {
		Color color = (mSingleConformer != null) ? mSingleConformer.getColor() : mSingleConformerColor;
		return color == null ? "none" : toRGBString(color);
	}

	public void setOverlayMolColor(String color) {
		mOverlayMolColor = color.equals("none") ? null : Color.valueOf(color);
		if (mOverlayMol != null)
			Platform.runLater(() -> mOverlayMol.setColor(mOverlayMolColor) );
	}

	public void setCavityMolColor(String color) {
		mCavityMolColor = color.equals("none") ? null : Color.valueOf(color);
		if (mCavityMol != null)
			Platform.runLater(() -> mCavityMol.setColor(mCavityMolColor) );
	}
	public void setCavitySurfaceColor(String color) {
		mCavitySurfaceColor = color.equals("none") ? null : Color.valueOf(color);
		if (mCavityMol != null)
			Platform.runLater(() -> mCavityMol.setSurfaceColor(MoleculeSurfaceAlgorithm.CONNOLLY, mCavitySurfaceColor) );
	}

	public void setCavityConstructionMode(int mode) {
		mCavityConstructionMode = mode;
		if (mCavityMol != null)
			Platform.runLater(() -> mCavityMol.setConstructionMode(mCavityConstructionMode) );
	}

	public void setCavityHydrogenMode(int mode) {
		mCavityHydrogenMode = mode;
		if (mCavityMol != null)
			Platform.runLater(() -> mCavityMol.setHydrogenMode(mCavityHydrogenMode) );
	}

	public void setCavityRibbonMode(int mode) {
		mCavityRibbonMode = mode;
		if (mCavityMol != null)
			Platform.runLater(() -> mCavityMol.setRibbonMode(mCavityRibbonMode) );
	}

	public void setCavitySurfaceMode(int mode) {
		mCavitySurfaceMode = mode;
		if (mCavityMol != null)
			Platform.runLater(() -> mCavityMol.setSurfaceMode(MoleculeSurfaceAlgorithm.CONNOLLY, mCavitySurfaceMode) );
	}

	public void setCavitySurfaceColorMode(int mode) {
		mCavitySurfaceColorMode = mode;
		if (mCavityMol != null)
			Platform.runLater(() -> mCavityMol.setSurfaceColorMode(MoleculeSurfaceAlgorithm.CONNOLLY, mCavitySurfaceColorMode) );
	}

	public void setCavitySurfaceTransparency(double transparency) {
		mCavitySurfaceTransparency = transparency;
		if (mCavityMol != null)
			Platform.runLater(() -> mCavityMol.setSurfaceTransparency(MoleculeSurfaceAlgorithm.CONNOLLY, mCavitySurfaceTransparency) );
	}

	public void setRefMolColor(String color) {
		mRefMolColor = color.equals("none") ? null : Color.valueOf(color);
		if (mRefMol != null)
			Platform.runLater(() -> mRefMol.setColor(mRefMolColor) );
	}

	public void setSingleConformerColor(String color) {
		mSingleConformerColor = color.equals("none") ? null : Color.valueOf(color);
		if (mSingleConformer != null)
			Platform.runLater(() -> mSingleConformer.setColor(mSingleConformerColor) );
	}

	private String toRGBString(Color color) {
		return String.format( "#%02X%02X%02X",
				Math.min(255, (int)(color.getRed() * 256)),
				Math.min(255, (int)(color.getGreen() * 256)),
				Math.min(255, (int)(color.getBlue() * 256)));
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		if (mIsEditable
		 && (mScene == null || mScene.getWorld().getGroups().isEmpty())) {
			Dimension theSize = getSize();
			Insets insets = getInsets();
			theSize.width -= insets.left + insets.right;
			theSize.height -= insets.top + insets.bottom;

			g.setFont(g.getFont().deriveFont(Font.PLAIN, HiDPIHelper.scale(10)));
			FontMetrics metrics = g.getFontMetrics();
			Rectangle2D bounds = metrics.getStringBounds(EDIT_MESSAGE, g);
			g.drawString(EDIT_MESSAGE, (int)(insets.left+theSize.width-bounds.getWidth())/2,
					(insets.top+theSize.height-metrics.getHeight())/2+metrics.getAscent());
			}
		}

	/**
	 * This waits for the constructor's with runLater() deferred initialization to complete
	 */
	public void waitForCompleteConstruction() {
		if (mConstructionTask != null) {
			try {
				mConstructionTask.get();
			} catch (Exception ie) {
			}
			mConstructionTask = null;
		}
	}

	public V3DScene getV3DScene() {
		return mScene;
	}

	public V3DPopupMenuController getPopupMenuController() {
		return mController;
	}

	/**
	 * @param controller
	 */
	public void setPopupMenuController(V3DPopupMenuController controller) {
		mController = controller;
		if (mScene != null)
			mScene.setPopupMenuController(controller);
	}

	/**
	 * Removes all except the reference molecule from the scene
	 */
	public void clear() {
		Platform.runLater(() -> {
			boolean changed = false;
			for (V3DMolecule fxmol : mScene.getMolsInScene()) {
				if (fxmol != mOverlayMol && fxmol != mCavityMol) {
					mScene.delete(fxmol);
					changed = true;
				}
			}
			if (changed)
				SwingUtilities.invokeLater(() -> fireStructureChanged());
		});
	}

	public void optimizeView() {
		Platform.runLater(() -> mScene.optimizeView());
	}

	@Override
	public void setBackground(java.awt.Color bg) {
		Platform.runLater(() -> {
			int r = bg.getRed();
			int g = bg.getGreen();
			int b = bg.getBlue();
			mScene.setFill(Color.rgb(r, g, b, 1));
		});
	}

	public void adaptToLookAndFeelChanges() {
		mAdaptToLookAndFeelChanges = true;
		setLookAndFeelBackground();
	}

	@Override
	public void updateUI() {
		super.updateUI();
		collectLookAndFeelColors();
		if (mAdaptToLookAndFeelChanges) {
			setLookAndFeelBackground();
			setLookAndFeelMenuBackground();
		}
	}

	private void collectLookAndFeelColors() {
		mSceneBackground = UIManager.getColor(isEnabled() ? "TextField.background" : "TextField.inactiveBackground");
		mLookAndFeelSpotColor = new java.awt.Color(HiDPIIcon.getThemeSpotRGBs()[0]);
		mMenuItemBackground = UIManager.getColor("MenuItem.background");
		mMenuItemForeground = UIManager.getColor("MenuItem.foreground");
//		mMenuItemSelectionBackground = UIManager.getColor("MenuItem.selectionBackground");
		mMenuItemSelectionForeground = UIManager.getColor("MenuItem.selectionForeground");
	}

	private void setLookAndFeelBackground() {
		if (mSceneBackground != null)
			setBackground(mSceneBackground);
	}

	private void setLookAndFeelMenuBackground() {
		StringURLConnection.updateCSS(this, generateStyleSheet() );
	}

	private String generateStyleSheet() {
		String textSize = "12 pt;";
		String lafSpot = toStyleText(mLookAndFeelSpotColor == null ? java.awt.Color.CYAN : mLookAndFeelSpotColor);
		String menuBG = toStyleText(mMenuItemBackground == null ? java.awt.Color.LIGHT_GRAY : mMenuItemBackground);
		String menuFG = toStyleText(mMenuItemForeground == null ? java.awt.Color.DARK_GRAY : mMenuItemForeground);
//		String menuSBG = toStyleText(mMenuItemSelectionBackground == null ? java.awt.Color.WHITE : mMenuItemSelectionBackground);
		String menuSFG = toStyleText(mMenuItemSelectionForeground == null ? java.awt.Color.BLACK : mMenuItemSelectionForeground);

		return ".root {\n" +
				" -fx-text-fill: "+menuFG+"\n" +
				" -fx-base: rgba(0, 0, 0, 1.0);\n" +
				" -fx-background-color: rgba(0, 0, 128, 1.0);\n" +
				" -fx-control-inner-background: "+menuBG+"\n" +
				" -fx-focus-color: rgba(128, 128, 0, 1.0);\n" +
				" -fx-select-color: rgba(128, 0, 0, 1.0);\n" +
				" -fx-faint-focus-color: rgba(0, 128, 0, 1.0);\n" +
				" }\n" +
			".menu {\n" +
				" -fx-text-fill: "+menuFG+"\n" +
				" -fx-background-color: "+menuBG+"\n" +
//				" -fx-focus-color: "+toStyleText(java.awt.Color.RED)+"\n" +
				" -fx-effect: null;"+"\n" +
				"}\n" +
			".menu-item {\n" +
				" -fx-background-color: "+menuBG+"\n" +
//				" -fx-focus-color: "+toStyleText(java.awt.Color.RED)+"\n" +
				" -fx-effect: null;\n" +
				"}\n" +
			".menu-item .label{\n" +
				" -fx-font-size: "+textSize+"\n" +
				" -fx-text-fill: "+menuFG+"\n" +
				"}\n" +
			".menu-item:hover{\n" +
				" -fx-font-size: "+textSize+"\n" +
				" -fx-background-color: "+lafSpot+"\n" +
				" -fx-text-fill: "+menuSFG+"\n" +
				"}\n";
	}

	private String toStyleText(java.awt.Color c) {
		return "#"+Integer.toHexString(c.getRGB()).substring(2)+";";
//		return "rgba("+c.getRed()+", "+c.getGreen()+", "+c.getBlue()+", 1.0);";
	}

	public void setConformerSplitting(double value) {
		Platform.runLater(() -> {
			int count = 0;
			double molSize = 0;
			for (Node node : mScene.getWorld().getChildren()) {
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
			for (Node node : mScene.getWorld().getChildren()) {
				if (node instanceof V3DMolecule) {
					node.setTranslateX((x - (double)countPerLine / 2) * molSize * value);
					node.setTranslateY((y - (double)lineCount / 2) * molSize);
					x += 1;
					if (x>countPerLine) {
						x = 0.5;
						y += 1.0;
					}
				}
			}
		});
	}

	public void setConollySurfaceMode(int mode) {
		Platform.runLater(() -> {
			for (Node node : mScene.getWorld().getAllAttachedRotatableGroups())
				if (node instanceof V3DMolecule)
					((V3DMolecule)node).setSurfaceMode(MoleculeSurfaceAlgorithm.CONNOLLY, mode);
		});
	}

	/**
	 * Note: Don't use this, if this 3D-View can be edited by the user, because editings
	 * are not reflected by the locally cached variable mOverlayMol. Use getMolecules()
	 * instead.
	 * @return the molecule originally passed with setOverlayMolecule()
	 */
	public StereoMolecule getOverlayMolecule() {
		return mOverlayMol == null ? null : mOverlayMol.getMolecule();
	}

	/**
	 * Adds the given small molecule into the scene.
	 * @param mol
	 */
	public void setOverlayMolecule(StereoMolecule mol) {
		Platform.runLater(() -> {
			boolean changed = false;
			if (mOverlayMol != null) {
				mOverlayMolColor = mOverlayMol.getColor();
				mScene.delete(mOverlayMol);
				mOverlayMol = null;
				changed = true;
			}
			if (mol != null) {
				mOverlayMol = new V3DMolecule(mol, 0, V3DMolecule.MoleculeRole.LIGAND);
				mOverlayMol.setColor(mOverlayMolColor);
				mScene.addMolecule(mOverlayMol, false);
				changed = true;
			}
			if (changed)
				SwingUtilities.invokeLater(() -> fireStructureChanged());
		});
	}

	/**
	 * Adds the given, typically cropped, protein cavity into the scene using color LIGHT_GRAY.
	 * If a ligand is given, all cavity atoms being in a distance of about CAVITY_CROP_DISTANCE
	 * of any ligand atom are marked. Then, a surface for the protein is generated, which covers
	 * unmarked atoms only.<br>
	 * NOTE: The ligand structure is not added to the scene!
	 * @param cavity
	 * @param ligand may be null
	 * @param optimizeView whether the view shall be centered after adding cavity
	 */
	public void setProteinCavity(StereoMolecule cavity, StereoMolecule ligand, boolean optimizeView) {
		final int updateID = mCurrentUpdateID;
		Platform.runLater(() -> {
			if (updateID != mCurrentUpdateID)	// skip this, if we have already cued another set of updates
				return;

			if (mCavityMol != null) {
				mCavityConstructionMode = mCavityMol.getConstructionMode();
				mCavityHydrogenMode = mCavityMol.getHydrogenMode();
				mCavityRibbonMode = mCavityMol.getRibbonMode();
				mCavityMolColor = mCavityMol.getColor();
				mCavitySurfaceMode = mCavityMol.getSurfaceMode(MoleculeSurfaceAlgorithm.CONNOLLY);
				mCavitySurfaceColorMode = mCavityMol.getSurfaceColorMode(MoleculeSurfaceAlgorithm.CONNOLLY);
				mCavitySurfaceColor = mCavityMol.getSurfaceColor(MoleculeSurfaceAlgorithm.CONNOLLY);
				mCavitySurfaceTransparency = mCavityMol.getSurfaceTransparency(MoleculeSurfaceAlgorithm.CONNOLLY);
				mScene.delete(mCavityMol);
			}

			if (updateID != mCurrentUpdateID)
				return;

			if (cavity != null) {
				ArrayList<StereoMolecule> ligands = null;
				if (ligand != null) {
					markAtomsInCropDistance(cavity, ligand, calculateCOG(ligand));
					ligands = new ArrayList<>();
					ligands.add(ligand);
				}

				mCavityMol = new V3DMolecule(cavity, ligands, mCavityConstructionMode, mCavityHydrogenMode, mCavityRibbonMode,
						mCavitySurfaceMode, mCavitySurfaceColorMode, mCavitySurfaceColor, mCavitySurfaceTransparency, 0);
				mCavityMol.setColor(mCavityMolColor);

				if (updateID != mCurrentUpdateID)
					return;

				mScene.getWorld().clearTransform();
				mScene.addMolecule(mCavityMol, false);

				mCavityMol.getMolecule().removeAtomMarkers();

				if (updateID != mCurrentUpdateID)
					return;

				if (optimizeView) {
					// rotate such that one looks into the cavity
					if (ligand != null) {
						Point3D worldCOG = mScene.getCOGInGroup(mScene.getWorld());
						Coordinates v1 = cavity.getCenterOfGravity().sub(ligand.getCenterOfGravity());	// direction from ligand to cavity
						Coordinates v2 = new Coordinates(0, 0.0, 1.0);	// intended new vector direction from ligand to cavity
						Coordinates axis = v1.cross(v2);
						double angle = 180 * v1.getAngle(v2) / Math.PI;
						mScene.getWorld().rotate(new Rotate(angle, worldCOG.getX(), worldCOG.getY(), worldCOG.getZ(), new Point3D(axis.x, axis.y, axis.z)));
					}
					mScene.optimizeView();
				}

				if (updateID != mCurrentUpdateID)
					return;

				mScene.reviveAnimation();	// just in case, there was a stopped anumation
				mScene.setShowInteractions(true);

				SwingUtilities.invokeLater(() -> fireStructureChanged());
			}
		});
	}

	public static Coordinates calculateCOG(StereoMolecule mol) {
		Coordinates cog = new Coordinates(mol.getCoordinates(0));
		for (int i = 1; i<mol.getAllAtoms(); i++)
			cog.add(mol.getCoordinates(i));
		cog.scale(1.0 / mol.getAllAtoms());
		return cog;
	}

	public static StereoMolecule cropProtein(StereoMolecule protein, StereoMolecule ligand, Coordinates ligandCOG) {
		protein.ensureHelperArrays(Molecule.cHelperNeighbours);

		double maxDistance = 0;
		for (int i = 0; i<ligand.getAllAtoms(); i++)
			maxDistance = Math.max(maxDistance, ligandCOG.distance(ligand.getCoordinates(i)));

		double cropDistance = JFXMolViewerPanel.CAVITY_CROP_DISTANCE;
		maxDistance += cropDistance;

		// mark all protein atoms within crop distance
		boolean[] isInCropRadius = new boolean[protein.getAllAtoms()];
		for (int i = 0; i<protein.getAllAtoms(); i++) {
			Coordinates pc = protein.getCoordinates(i);
			if (Math.abs(pc.x - ligandCOG.x)<maxDistance
					&& Math.abs(pc.y - ligandCOG.y)<maxDistance
					&& Math.abs(pc.z - ligandCOG.z)<maxDistance
					&& pc.distance(ligandCOG)<maxDistance) {
				for (int j = 0; j<ligand.getAllAtoms(); j++) {
					if (pc.distance(ligand.getCoordinates(j))<cropDistance) {
						isInCropRadius[i] = true;
						break;
					}
				}
			}
		}

		int[] uncroppedNeighbours = new int[protein.getAllAtoms()];
		for (int i = 0; i<protein.getAllBonds(); i++) {
			int atom1 = protein.getBondAtom(0, i);
			int atom2 = protein.getBondAtom(1, i);
			if (isInCropRadius[atom1])
				uncroppedNeighbours[atom2]++;
			if (isInCropRadius[atom2])
				uncroppedNeighbours[atom1]++;
		}
		for (int i = 0; i<protein.getAllAtoms(); i++) {
			if (isInCropRadius[i] && uncroppedNeighbours[i] == 0)
				isInCropRadius[i] = false;
			else if (!isInCropRadius[i] && uncroppedNeighbours[i] != 0 && protein.getAllConnAtoms(i) == uncroppedNeighbours[i])
				isInCropRadius[i] = true;
		}

		// set atomicNo=0 for outside crop distance atoms, which are connected to inside atoms
		// and determine for every uncropped atom, whether it has an uncropped neighbour
		for (int i = 0; i<protein.getAllBonds(); i++) {
			int atom1 = protein.getBondAtom(0, i);
			int atom2 = protein.getBondAtom(1, i);
			if (isInCropRadius[atom1] && protein.getAtomicNo(atom1) != 0 && !isInCropRadius[atom2]) {
				protein.setAtomicNo(atom2, 0);
				isInCropRadius[atom2] = true;
			} else if (isInCropRadius[atom2] && protein.getAtomicNo(atom2) != 0 && !isInCropRadius[atom1]) {
				protein.setAtomicNo(atom1, 0);
				isInCropRadius[atom1] = true;
			}
		}

		StereoMolecule croppedProtein = new StereoMolecule();
		protein.copyMoleculeByAtoms(croppedProtein, isInCropRadius, false, null);
		croppedProtein.setFragment(false);
		return croppedProtein;
	}

	/**
	 * If the panel is supposed to show a protein cavity created by cropping a larger protein,
	 * and if the cavity surface shall be shown in the area of the natural ligand, then this
	 * method can be used to mark all cavity atoms that shall be covered by the surface.
	 *
	 * @param cavity
	 * @param ligand
	 */
	public static void markAtomsInCropDistance(StereoMolecule cavity, StereoMolecule ligand, Coordinates ligandCOG) {
		double maxDistance = 0;
		for (int i = 0; i<ligand.getAllAtoms(); i++)
			maxDistance = Math.max(maxDistance, ligandCOG.distance(ligand.getCoordinates(i)));

		double markDistance = JFXMolViewerPanel.CAVITY_CROP_DISTANCE - 3.2;
		maxDistance += markDistance;

		for (int i = 0; i<cavity.getAllAtoms(); i++)
			cavity.setAtomMarker(i, true);

		// reset mark for all cavity atoms near any ligand atom
		for (int i = 0; i<cavity.getAllAtoms(); i++) {
			Coordinates pc = cavity.getCoordinates(i);
			if (Math.abs(pc.x - ligandCOG.x)<maxDistance
					&& Math.abs(pc.y - ligandCOG.y)<maxDistance
					&& Math.abs(pc.z - ligandCOG.z)<maxDistance
					&& pc.distance(ligandCOG)<maxDistance) {
				for (int j = 0; j<ligand.getAllAtoms(); j++) {
					if (pc.distance(ligand.getCoordinates(j))<markDistance) {
						cavity.setAtomMarker(i, false);
						break;
					}
				}
			}
		}
	}

	/**
	 * @param mol
	 * @param color
	 * @param centerOfRotation
	 */
	public void addMolecule(StereoMolecule mol, Color color, Point3D centerOfRotation) {
		Platform.runLater(() -> {
			addMoleculeNow(mol, color, centerOfRotation, false);
			SwingUtilities.invokeLater(() -> fireStructureChanged());
		} );
	}

	private V3DMolecule addMoleculeNow(StereoMolecule mol, Color color, Point3D centerOfRotation, boolean showTorsionStrain) {
		V3DMolecule fxmol = new V3DMolecule(mol, true, mScene.isSplitAllBonds());
		fxmol.setColor(color);
		fxmol.setSurfaceColorMode(MoleculeSurfaceAlgorithm.CONNOLLY, SurfaceMesh.SURFACE_COLOR_INHERIT);
		fxmol.setCenterOfRotation(centerOfRotation);
		if (showTorsionStrain)
			fxmol.addTorsionStrainVisualization();
		mScene.addMolecule(fxmol, false);
		return fxmol;
	}

	/**
	 * Removes all molecules except the cavity and overlay molecules, if they exist.
	 * A cavity, natural ligand or PheSA query conformer, which need to be shown statically,
	 * are not touched by this update, if they are defined with setProteinCavity() or setOverlayMolecule().
	 * Then, adds the passed conformer(s) or docked ligand.
	 * Then, optionally adds the reference conformer.
	 * Unless there is a protein cavity or an overlay molecule, it finally optimizes the view.
	 * If you use this call potentially many times in e.g. to update the detail view content upon mose movement,
	 * then you should call increaseUpdateID() first to allow skipping outdated cued update threads!
	 * @param conformers   multiple or one conformer, which may also be a ligand structure
	 * @param refConformer optional second conformer or ligand structure for comparison (not the natural ligand or PheSA query)
	 */
	public void updateConformers(StereoMolecule[] conformers, StereoMolecule refConformer) {
		final int updateID = mCurrentUpdateID;
		Platform.runLater(() -> {
			if (updateID != mCurrentUpdateID)
				return;

			// store current colors of refmol and single conf for later use
			if (mRefMol != null)
				mRefMolColor = mRefMol.getColor();
			mRefMol = null;

			if (mSingleConformer != null)
				mSingleConformerColor = mSingleConformer.getColor();
			mSingleConformer = null;

			boolean isTorsionStrainVisible = false;
			for (V3DMolecule fxmol : mScene.getMolsInScene())
				if (fxmol != mOverlayMol
				 && fxmol != mCavityMol
				 && updateID == mCurrentUpdateID) {
					isTorsionStrainVisible |= (fxmol.getTorsionStrainVis() != null);
					mScene.delete(fxmol);
				}

			if (conformers != null) {
				if (conformers.length == 1) {
					if (updateID == mCurrentUpdateID) {
						mSingleConformer = addMoleculeNow(conformers[0], mSingleConformerColor, null, isTorsionStrainVisible);
					}
				} else {
					Point3D cor = new Point3D(0, 0, 0);
					for (int i = 0; i<conformers.length && updateID == mCurrentUpdateID; i++) {
						Color c = Color.hsb(360f * i / conformers.length, 0.75, 0.6);
						addMoleculeNow(conformers[i], c, cor, false);
					}
				}
			}

			if (refConformer != null && updateID == mCurrentUpdateID) {
				mRefMol = addMoleculeNow(refConformer, mRefMolColor, null, isTorsionStrainVisible);
			}

			// Don't optimize view if we have a cavity or overlay molecules. User may have optimized the view to look into the cavity
			if ((conformers != null || refConformer != null) && mOverlayMol == null && mCavityMol == null && updateID == mCurrentUpdateID)
				mScene.optimizeView();

			SwingUtilities.invokeLater(() -> fireStructureChanged());
		});
	}

	public ArrayList<StereoMolecule> getMolecules(V3DMolecule.MoleculeRole role) {
		final ArrayList<StereoMolecule> conformerList = new ArrayList<>();

		final CountDownLatch latch = new CountDownLatch(1);

		Platform.runLater(() -> {
			for (Node node : mScene.getWorld().getChildren())
				if (node instanceof V3DMolecule
				 && (role == null
				  || role == ((V3DMolecule)node).getRole()))
					conformerList.add(((V3DMolecule)node).getMolecule());
			latch.countDown();
		});

		try {
			latch.await();
		} catch (InterruptedException ie) {
		}
		return conformerList;
	}

	public ArrayList<StereoMolecule> getMoleculesInFXThread(V3DMolecule.MoleculeRole role) {
		final ArrayList<StereoMolecule> conformerList = new ArrayList<>();

		for (Node node : mScene.getWorld().getChildren())
			if (node instanceof V3DMolecule
			 && (role == null
			  || role == ((V3DMolecule)node).getRole()))
				conformerList.add(((V3DMolecule)node).getMolecule());

		return conformerList;
	}

	public WritableImage getContentImage() {
		final ArrayList<WritableImage> imageList = new ArrayList<>();

		final CountDownLatch latch = new CountDownLatch(1);

		Platform.runLater(() -> {
			WritableImage image = mScene.snapshot(null, null);
			imageList.add(image);
			latch.countDown();
		});

		try {
			latch.await();
		} catch (InterruptedException ie) {
		}

		return imageList.isEmpty() ? null : imageList.get(0);
	}

	private static class StringURLConnection extends URLConnection {
		private static String sCSS;

		private static void updateCSS(JFXMolViewerPanel conformerPanel, final String css) {
			sCSS = css;
//			Platform.runLater(() ->
					conformerPanel.getScene().getStylesheets().setAll("internal:"+System.nanoTime()+"stylesheet.css");
		}

		public StringURLConnection(URL url) {
			super(url);
		}

		@Override
		public void connect() throws IOException {
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new StringBufferInputStream(sCSS);
		}
	}

	private static class StringURLStreamHandlerFactory implements URLStreamHandlerFactory {
		URLStreamHandler streamHandler = new URLStreamHandler() {
			@Override
			protected URLConnection openConnection(URL url) throws IOException {
			if (url.toString().toLowerCase().endsWith(".css")) {
				return new StringURLConnection(url);
			}
			throw new FileNotFoundException();
			}
		};

		@Override
		public URLStreamHandler createURLStreamHandler(String protocol) {
			if ("internal".equals(protocol)) {
				return streamHandler;
			}
			return null;
		}
	}
}