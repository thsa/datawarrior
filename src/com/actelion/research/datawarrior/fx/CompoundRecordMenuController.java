package com.actelion.research.datawarrior.fx;

import com.actelion.research.chem.Coordinates;
import com.actelion.research.chem.IDCodeParserWithoutCoordinateInvention;
import com.actelion.research.chem.SSSearcher;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.alignment3d.PheSAAlignmentOptimizer;
import com.actelion.research.chem.conf.HydrogenAssembler;
import com.actelion.research.chem.forcefield.mmff.ForceFieldMMFF94;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.mcs.MCS;
import com.actelion.research.chem.shredder.FragmentGeometry3D;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DERuntimeProperties;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.DETaskPaste;
import com.actelion.research.datawarrior.task.table.DETaskSetColumnProperties;
import com.actelion.research.table.CompoundTableLoader;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.ArrayUtils;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DPopupMenuController;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;

public class CompoundRecordMenuController implements V3DPopupMenuController {
	public static final int ALIGN_DONT_NONE = 0;
	public static final int ALIGN_BY_SHAPE = 1;
	public static final int ALIGN_BY_MCS = 2;

	private final JFXMolViewerPanel mConformerPanel;
	private final CompoundTableModel mTableModel;
	private CompoundRecord mParentRecord,mNonNullParentRecord;
	private int mCoordsColumn;
	private final int[] mCoordsColumns;
	private final boolean mAllowSuperposeReference;
	private volatile Thread mUpdateThread;

	/**
	 * This controller adds menu items and functionality to 3D-Views in the detail area or in form views.
	 * It allows to change display options, but not to change any structures.
	 * @param conformerPanel
	 * @param tableModel
	 * @param coordsColumn
	 * @param allowSuperposeReference
	 */
	public CompoundRecordMenuController(JFXMolViewerPanel conformerPanel, CompoundTableModel tableModel, int coordsColumn, boolean allowSuperposeReference) {
		mConformerPanel = conformerPanel;
		mTableModel = tableModel;
		mCoordsColumn = coordsColumn;
		mAllowSuperposeReference = allowSuperposeReference;
		mCoordsColumns = mTableModel.getSpecialColumnList(CompoundTableConstants.cColumnType3DCoordinates);
	}

	/**
	 * Must be updated from outside, if columns are deleted from the table and coordsColumn index changes
	 * @param coordsColumn
	 */
	public void updateCoordsColumn(int coordsColumn) {
		mCoordsColumn = coordsColumn;
	}

	/**
	 * Must be called to keep underlying record up-to-date only if mAllowSuperposeReference is true
	 * @param record
	 */
	public void setParentRecord(CompoundRecord record) {
		mParentRecord = record;
		if (record != null)
			mNonNullParentRecord = record;
	}

	@Override
	public void addExternalMenuItems(ContextMenu popup, int type) {
		if (type == V3DPopupMenuController.TYPE_VIEW) {
			String superposeWhat = mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertySuperpose);
			boolean hasCavity = mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertyProteinCavity) != null;
			boolean hasLigand = mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertyNaturalLigand) != null;
			boolean hasQuery = mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertySuperposeMolecule) != null;
			boolean isShowLigand = !"false".equals(mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertyShowNaturalLigand));
			boolean isShowQuery = !"false".equals(mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertyShowSuperposeMolecule));
			boolean isSuperposeRefRow = CompoundTableConstants.cSuperposeValueReferenceRow.equals(superposeWhat);
			String alignMethodString = mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertySuperposeAlign);
			int alignMethod = CompoundTableConstants.cSuperposeAlignValueShape.equals(alignMethodString) ? ALIGN_BY_SHAPE
							: CompoundTableConstants.cSuperposeAlignValueMCS.equals(alignMethodString) ? ALIGN_BY_MCS : ALIGN_DONT_NONE;
			int superposeColumn = isSuperposeRefRow ? -1 : mTableModel.findColumn(superposeWhat);

			if (hasQuery) {
				javafx.scene.control.CheckMenuItem itemShowQuery = new CheckMenuItem("Overlay Query Molecule");
				itemShowQuery.setSelected(isShowQuery);
				itemShowQuery.setOnAction(e -> setShowQueryMolecule(!isShowQuery));
				popup.getItems().add(itemShowQuery);
			}

			if (!hasQuery && hasLigand) {   // query and ligand display are done via setOverlayMolecule(). Thus, we cannot have both!
				javafx.scene.control.CheckMenuItem itemShowLigand = new CheckMenuItem("Overlay Natural Ligand");
				itemShowLigand.setSelected(isShowLigand);
				itemShowLigand.setOnAction(e -> setShowNaturalLigand(!isShowLigand));
				popup.getItems().add(itemShowLigand);
			}

			if (mAllowSuperposeReference) {
				javafx.scene.control.CheckMenuItem itemSuperposeRefRow = new CheckMenuItem("Overlay Structure From Reference Row");
				itemSuperposeRefRow.setSelected(isSuperposeRefRow);
				itemSuperposeRefRow.setOnAction(e -> setSuperposeMode(!isSuperposeRefRow, alignMethod, superposeColumn, -1));
				popup.getItems().add(itemSuperposeRefRow);
			}

			for (int coordsColumn : mCoordsColumns) {
				if (coordsColumn != mCoordsColumn
				 && !"none".equals(mTableModel.getColumnProperty(coordsColumn, CompoundTableConstants.cColumnPropertyDetailView))) {
					javafx.scene.control.CheckMenuItem itemSuperposeColumn = new CheckMenuItem("Overlay '"+mTableModel.getColumnTitle(coordsColumn)+"'");
					itemSuperposeColumn.setSelected(superposeColumn == coordsColumn);
					itemSuperposeColumn.setOnAction(e -> setSuperposeMode(false, alignMethod, superposeColumn == coordsColumn ? -1 : coordsColumn, -1));
					popup.getItems().add(itemSuperposeColumn);
				}
			}

			if (mAllowSuperposeReference && !hasCavity) {
				boolean overlayShown = isSuperposeRefRow || superposeColumn != -1;

				// don't allow shape alignment if we show a protein cavity
				javafx.scene.control.CheckMenuItem itemAlignShape = new CheckMenuItem("Align Structures By Shape");
				itemAlignShape.setSelected(alignMethod == ALIGN_BY_SHAPE);
				itemAlignShape.setDisable(!overlayShown);
				itemAlignShape.setOnAction(e -> setSuperposeMode(isSuperposeRefRow, alignMethod == ALIGN_BY_SHAPE ? ALIGN_DONT_NONE : ALIGN_BY_SHAPE, superposeColumn, -1));

				javafx.scene.control.CheckMenuItem itemAlignMCS = new CheckMenuItem("Align Structures By MCS");
				itemAlignMCS.setSelected(alignMethod == ALIGN_BY_MCS);
				itemAlignMCS.setDisable(!overlayShown);
				itemAlignMCS.setOnAction(e -> setSuperposeMode(isSuperposeRefRow, alignMethod == ALIGN_BY_MCS ? ALIGN_DONT_NONE : ALIGN_BY_MCS, superposeColumn, -1));

				popup.getItems().addAll(new SeparatorMenuItem(), itemAlignShape, itemAlignMCS);
			}

//			if (hasCavity) {
//				boolean isShowInteractions = (hasLigand && mConformerPanel.getV3DScene().isShowInteractions());
//				javafx.scene.control.CheckMenuItem itemShowInteractions = new CheckMenuItem("Show Interactions");
//				itemShowInteractions.setSelected(isShowInteractions);
//				itemShowInteractions.setDisable(!hasLigand);
//				itemShowInteractions.setOnAction(e -> setShowInteractions(!isShowInteractions));
//				popup.getItems().add(itemShowInteractions);
//			}

			// TODO remove this and the following TEMPLATE
			if (System.getProperty("development") != null) {
				javafx.scene.control.MenuItem itemDev = new MenuItem("Investigate MMFF94S+ Contributions");
				itemDev.setOnAction(e -> {
					if (mNonNullParentRecord != null) {
						StereoMolecule[] mol = getConformers(mCoordsColumn, mNonNullParentRecord, false);
						if (mol != null) {
							StringBuilder detail = new StringBuilder();
							HashMap<String,Object> options = new HashMap<>();
							options.put("dielectric constant", Double.valueOf(80.0));
							ForceFieldMMFF94.initialize(ForceFieldMMFF94.MMFF94SPLUS);
							ForceFieldMMFF94 ff = new ForceFieldMMFF94(mol[0], ForceFieldMMFF94.MMFF94SPLUS, options);
							if (!Double.isNaN(ff.getTotalEnergy(detail, false))) {
								SwingUtilities.invokeLater(() -> {
									StringSelection theData = new StringSelection(detail.toString());
									Toolkit.getDefaultToolkit().getSystemClipboard().setContents(theData, theData);

									DEFrame newFrame = DataWarrior.getApplication().getEmptyFrame("MMFF94S+ energy contributions (diel=80)");
									new CompoundTableLoader(newFrame, newFrame.getTableModel(), null).paste(DETaskPaste.HEADER_WITH, false);

									DERuntimeProperties props = new DERuntimeProperties(newFrame.getMainFrame());
									try { props.read(new BufferedReader(new StringReader(TEMPLATE_ENERGY_DETAILS))); } catch (IOException ex) {}
									props.apply();
								});
							}
						}
					}
				} );
				popup.getItems().addAll(new SeparatorMenuItem(), itemDev);
			}

			popup.getItems().add(new SeparatorMenuItem());
		}
	}

	private static final String TEMPLATE_ENERGY_DETAILS = "<datawarrior properties>\n" +
			"<axisColumn_2D View_0=\"no\">\n" +
			"<axisColumn_2D View_1=\"energy\">\n" +
			"<chartType_2D View=\"scatter\">\n" +
			"<colorColumn_2D View=\"type\">\n" +
			"<colorCount_2D View=\"7\">\n" +
			"<colorListMode_2D View=\"Categories\">\n" +
			"<color_2D View_0=\"-11992833\">\n" +
			"<color_2D View_1=\"-65494\">\n" +
			"<color_2D View_2=\"-16732826\">\n" +
			"<color_2D View_3=\"-256\">\n" +
			"<color_2D View_4=\"-11546120\">\n" +
			"<color_2D View_5=\"-2252554\">\n" +
			"<color_2D View_6=\"-21735\">\n" +
			"<crosshairList_2D View=\"\">\n" +
			"<defaultColor_labelBG_2D View=\"-2039584\">\n" +
			"<detailView=\"height[Data]=1\">\n" +
			"<fastRendering_2D View=\"false\">\n" +
			"<mainSplitting=\"1\">\n" +
			"<mainView=\"2D View\">\n" +
			"<mainViewCount=\"2\">\n" +
			"<mainViewDockInfo0=\"root\">\n" +
			"<mainViewDockInfo1=\"Table\tbottom\t0.499\">\n" +
			"<mainViewName0=\"Table\">\n" +
			"<mainViewName1=\"2D View\">\n" +
			"<mainViewType0=\"tableView\">\n" +
			"<mainViewType1=\"2Dview\">\n" +
			"<markersize_2D View=\"1.21\">\n" +
			"<rightSplitting=\"0.66233\">\n" +
			"<rowHeight_Table=\"16\">\n" +
			"<scaleStyle_2D View=\"frame\">\n" +
			"<scatterplotMargin_2D View=\"0.025\">\n" +
			"<showNaNValues_2D View=\"true\">\n" +
			"<sizeColumn_2D View=\"energy\">\n" +
			"<sizeProportional_2D View=\"true\">\n" +
			"</datawarrior properties>\n";

	@Override
	public void markCropDistanceForSurface(V3DMolecule fxmol, int type, int mode) {
		boolean hasCavity = mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertyProteinCavity) != null;
		boolean hasLigand = mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertyNaturalLigand) != null;
		if (hasLigand && hasCavity && fxmol.getRole() == V3DMolecule.MoleculeRole.MACROMOLECULE) {
			StereoMolecule ligand = mConformerPanel.getOverlayMolecule();
			JFXMolViewerPanel.markAtomsInCropDistance(fxmol.getMolecule(), ligand, ligand.getCenterOfGravity());
		}
	}

	/**
	 *
	 * @param isSuperposeRefRow whether the reference row's conformer shall be superposed to this row's one
	 * @param alignmentMethod whether and how the reference row's conformer shall be also aligned to this row's one
	 * @param superposeColumn whether and which other 3D-coord column's conformer of the same row shall be superposed to this column's one
	 * @param cavityColumn whether and which cavity column shall be shown with this column's ligand structure
	 */
	private void setSuperposeMode(boolean isSuperposeRefRow, int alignmentMethod, int superposeColumn, int cavityColumn) {
		SwingUtilities.invokeLater(() -> {
			HashMap<String, String> map = new HashMap<>();
			String superposeWhat = isSuperposeRefRow ? CompoundTableConstants.cSuperposeValueReferenceRow
					: (superposeColumn != -1) ? mTableModel.getColumnTitleNoAlias(superposeColumn) : null;
			map.put(CompoundTableConstants.cColumnPropertySuperpose, superposeWhat);
			map.put(CompoundTableConstants.cColumnPropertySuperposeAlign, alignmentMethod == ALIGN_BY_SHAPE ? CompoundTableConstants.cSuperposeAlignValueShape
																		: alignmentMethod == ALIGN_BY_MCS ? CompoundTableConstants.cSuperposeAlignValueMCS : null);
			new DETaskSetColumnProperties(getFrame(), mCoordsColumn, map, false).defineAndRun();
			update3DView(isSuperposeRefRow, alignmentMethod, superposeColumn, cavityColumn);
		});
	}

	private void setShowNaturalLigand(boolean isShowNaturalLigand) {
		String ligand = isShowNaturalLigand ? mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertyNaturalLigand) : null;
		StereoMolecule ligandMol = (ligand == null) ? null : new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(ligand);
		mConformerPanel.setOverlayMolecule(ligandMol, true);
		SwingUtilities.invokeLater(() -> {
			HashMap<String, String> map = new HashMap<>();
			map.put(CompoundTableConstants.cColumnPropertyShowNaturalLigand, isShowNaturalLigand ? null : "false");
			new DETaskSetColumnProperties(getFrame(), mCoordsColumn, map, false).defineAndRun();
		});
	}

	private void setShowQueryMolecule(boolean isShowQuery) {
		String query = isShowQuery ? mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertySuperposeMolecule) : null;
		StereoMolecule queryMol = (query == null) ? null : new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(query);
		if (queryMol != null)
			new HydrogenAssembler(queryMol).addImplicitHydrogens();
		mConformerPanel.setOverlayMolecule(queryMol, true);
		SwingUtilities.invokeLater(() -> {
			HashMap<String, String> map = new HashMap<>();
			map.put(CompoundTableConstants.cColumnPropertyShowSuperposeMolecule, isShowQuery ? null : "false");
			new DETaskSetColumnProperties(getFrame(), mCoordsColumn, map, false).defineAndRun();
		});
	}

	private DEFrame getFrame() {
		Component c = mConformerPanel;
		while (!(c instanceof DEFrame))
			c = c.getParent();

		return (DEFrame)c;
	}

	/**
	 * @param isSuperposeRefRow whether the active row's conformer shall be shown in addition
	 * @param alignmentMethod whether and how the shown conformer(s) shall be rigidly aligned to the active row's conformer (if shown)
	 * @param superposeColumn -1 or coords3D column of another conformer column which shall be superposed to this column's conformer
	 * @param cavityColumn -1 or coords3D column of cavity column that is assigned to this controller's ligand column to complete a binding site structure
	 */
	public void update3DView(boolean isSuperposeRefRow, int alignmentMethod, int superposeColumn, int cavityColumn) {
		mUpdateThread = new Thread(() -> {
			// cancel all FX-threads that work on older updates, because a new one is on its way
			mConformerPanel.increaseUpdateID();

			StereoMolecule[] rowMol = null;
			if (mParentRecord != null)
				rowMol = getConformers(mCoordsColumn, mParentRecord, true);

			StereoMolecule[] refMol = null;
			if (isSuperposeRefRow) {
				if (mTableModel.getActiveRow() != null && mTableModel.getActiveRow() != mParentRecord)
					refMol = getConformers(mCoordsColumn, mTableModel.getActiveRow(), false);
			}
			else if (superposeColumn != -1) {
				if (mParentRecord != null)
					refMol = getConformers(superposeColumn, mParentRecord, false);
			}

			if (rowMol != null) {
				StereoMolecule bestRowConf = null;
				StereoMolecule bestRefConf = null;
				if (alignmentMethod != ALIGN_DONT_NONE && refMol != null) {
					if (rowMol.length == 1) {
						alignConformer(rowMol[0], refMol[0], alignmentMethod);
					}
					else {	// we align the ref-mol on all row conformers and only show the one that fits best
						double maxFit = 0;
						for (StereoMolecule rowConf : rowMol) {
							if (Thread.currentThread() != mUpdateThread)
								break;

							StereoMolecule refConf = new StereoMolecule(refMol[0]);
							double fit = alignConformer(rowConf, refConf, alignmentMethod);
							if (fit>maxFit) {
								maxFit = fit;
								bestRowConf = rowConf;
								bestRefConf = refConf;
							}
						}

						rowMol = new StereoMolecule[1];
						rowMol[0] = bestRowConf;
						refMol = new StereoMolecule[1];
						refMol[0] = bestRefConf;
					}
				}
			}

			if (cavityColumn != -1 && mParentRecord != null && Thread.currentThread() == mUpdateThread) {
				StereoMolecule[] cavity = getConformers(cavityColumn, mParentRecord, false);
				mConformerPanel.clear();
				mConformerPanel.setProteinCavity(cavity == null || cavity.length==0 ? null : cavity[0], rowMol == null ? null : rowMol[0], true, true);
			}

			if (Thread.currentThread() == mUpdateThread)
				mConformerPanel.updateConformers(rowMol, refMol == null ? null : refMol[0]);
		});

		mUpdateThread.start();
	}

	/**
	 *
	 * @param staticMol
	 * @param movingMol
	 * @param alignMethod
	 * @return larger values are better fit, 0 := failed fit
	 */
	private double alignConformer(StereoMolecule staticMol, StereoMolecule movingMol, int alignMethod) {
		if (alignMethod == ALIGN_BY_SHAPE) {
			return PheSAAlignmentOptimizer.alignTwoMolsInPlace(staticMol, movingMol, null);
		}
		else if (alignMethod == ALIGN_BY_MCS) {
			MCS mcsSearcher = new MCS();
			mcsSearcher.set(staticMol, movingMol);
			StereoMolecule mcs = mcsSearcher.getMCS();
			if (mcs != null && (mcs.getAllAtoms() >= 3)) {
				SSSearcher sssSearcher = new SSSearcher();
				sssSearcher.setFragment(mcs);
				sssSearcher.setMolecule(staticMol);
				if (sssSearcher.findFragmentInMolecule(SSSearcher.cCountModeFirstMatch, SSSearcher.cDefaultMatchMode) == 1) {
					int[] staticMatch = sssSearcher.getMatchList().get(0);
					sssSearcher.setMolecule(movingMol);
					if (sssSearcher.findFragmentInMolecule(SSSearcher.cCountModeFirstMatch, SSSearcher.cDefaultMatchMode) == 1) {
						int[] movingMatch = sssSearcher.getMatchList().get(0);
						Coordinates[] staticCoords = new Coordinates[staticMatch.length];
						Coordinates[] movingCoords = new Coordinates[staticMatch.length];
						for (int i=0; i<staticMatch.length; i++) {
							staticCoords[i] = staticMol.getAtomCoordinates(staticMatch[i]);
							movingCoords[i] = movingMol.getAtomCoordinates(movingMatch[i]);
						}
						// make sure the value is positive and increases with the quality of fit
						return 1000 - alignAndGetRMSD(staticCoords, movingCoords, movingMol);
					}
				}
			}
		}
		return 0;
	}

	private double alignAndGetRMSD(Coordinates[] staticCoords, Coordinates[] movingCoords, StereoMolecule mol) {
		Coordinates staticCOG = FragmentGeometry3D.centerOfGravity(staticCoords);
		Coordinates movingCOG = FragmentGeometry3D.centerOfGravity(movingCoords);
		double[][] matrix = FragmentGeometry3D.kabschAlign(staticCoords, movingCoords, staticCOG, movingCOG);

		for (int atom=0; atom<mol.getAllAtoms(); atom++) {
			Coordinates c = mol.getAtomCoordinates(atom);
			c.sub(movingCOG);
			c.rotate(matrix);
			c.add(staticCOG);
		}

		return Coordinates.getRmsd(staticCoords, movingCoords);
	}

	private StereoMolecule[] getConformers(int coordsColumn, CompoundRecord record, boolean allowMultiple) {
		byte[] idcode = (byte[]) record.getData(mTableModel.getParentColumn(coordsColumn));
		byte[] coords = (byte[]) record.getData(coordsColumn);
		if (idcode != null && coords != null) {
			boolean split3DFragments = "true".equals(mTableModel.getColumnProperty(coordsColumn, CompoundTableModel.cColumnProperty3DFragmentSplit));
			if (split3DFragments) {
				return new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(idcode, coords).getFragments();
			}
			else {
				int count = 1;
				int index = ArrayUtils.indexOf(coords, (byte) 32);
				if (index != -1 && allowMultiple) {
					count++;
					for (int i = index + 1; i < coords.length; i++)
						if (coords[i] == (byte) 32)
							count++;
					index = -1;
				}
				StereoMolecule[] mol = new StereoMolecule[count];
				for (int i=0; i<count; i++) {
					mol[i] = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(idcode, coords, 0, index + 1);
					index = ArrayUtils.indexOf(coords, (byte) 32, index + 1);
				}
				return mol;
			}
		}
		return null;
	}
}