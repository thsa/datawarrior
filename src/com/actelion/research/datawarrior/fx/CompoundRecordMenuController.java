package com.actelion.research.datawarrior.fx;

import com.actelion.research.chem.IDCodeParserWithoutCoordinateInvention;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.alignment3d.PheSAAlignmentOptimizer;
import com.actelion.research.chem.conf.AtomAssembler;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.table.DETaskSetColumnProperties;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.ArrayUtils;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SeparatorMenuItem;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DPopupMenuController;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

public class CompoundRecordMenuController implements V3DPopupMenuController {
	private final JFXMolViewerPanel mConformerPanel;
	private final CompoundTableModel mTableModel;
	private CompoundRecord mParentRecord;
	private int mCoordsColumn;
	private final boolean mAllowSuperposeReference;

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
	}

	@Override
	public void addExternalMenuItems(ContextMenu popup, int type) {
		if (type == V3DPopupMenuController.TYPE_VIEW) {
			boolean hasCavity = mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertyProteinCavity) != null;
			boolean hasLigand = mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertyNaturalLigand) != null;
			boolean hasQuery = mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertySuperposeMolecule) != null;
			boolean isShowLigand = !"false".equals(mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertyShowNaturalLigand));
			boolean isShowQuery = !"false".equals(mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertyShowSuperposeMolecule));
			boolean isSuperpose = CompoundTableConstants.cSuperposeValueReferenceRow.equals(mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertySuperpose));
			boolean isShapeAlign = CompoundTableConstants.cSuperposeAlignValueShape.equals(mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertySuperposeAlign));

			if (hasQuery) {
				javafx.scene.control.CheckMenuItem itemShowQuery = new CheckMenuItem("Show Query Molecule");
				itemShowQuery.setSelected(isShowQuery);
				itemShowQuery.setOnAction(e -> setShowQueryMolecule(!isShowQuery));
				popup.getItems().add(itemShowQuery);
			}

			if (!hasQuery && hasLigand) {   // query and ligand display are done via setOverlayMolecule(). Thus, we cannot have both!
				javafx.scene.control.CheckMenuItem itemShowLigand = new CheckMenuItem("Show Natural Ligand");
				itemShowLigand.setSelected(isShowLigand);
				itemShowLigand.setOnAction(e -> setShowNaturalLigand(!isShowLigand));
				popup.getItems().add(itemShowLigand);
			}

			if (mAllowSuperposeReference) {
				javafx.scene.control.CheckMenuItem itemSuperpose = new CheckMenuItem("Show Reference Row Structure");
				itemSuperpose.setSelected(isSuperpose);
				itemSuperpose.setOnAction(e -> setSuperposeMode(!isSuperpose, isShapeAlign, -1));
				popup.getItems().add(itemSuperpose);
			}

			if (mAllowSuperposeReference && !hasCavity && isSuperpose) {
				// don't allow shape alignment if we show a protein cavity
				javafx.scene.control.CheckMenuItem itemAlignShape = new CheckMenuItem("Align Shown Structures");
				itemAlignShape.setSelected(isShapeAlign);
				itemAlignShape.setDisable(false);
				itemAlignShape.setOnAction(e -> setSuperposeMode(isSuperpose, !isShapeAlign, -1));
				popup.getItems().add(itemAlignShape);
			}

			if (hasCavity) {
				boolean isShowInteractions = (hasLigand && mConformerPanel.getV3DScene().isShowInteractions());
				javafx.scene.control.CheckMenuItem itemShowInteractions = new CheckMenuItem("Show Interactions");
				itemShowInteractions.setSelected(isShowInteractions);
				itemShowInteractions.setDisable(!hasLigand);
				itemShowInteractions.setOnAction(e -> setShowInteractions(!isShowInteractions));
				popup.getItems().add(itemShowInteractions);
			}

			popup.getItems().add(new SeparatorMenuItem());
		}
	}

	@Override
	public void markCropDistanceForSurface(V3DMolecule fxmol, int type, int mode) {
		boolean hasCavity = mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertyProteinCavity) != null;
		boolean hasLigand = mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertyNaturalLigand) != null;
		if (hasLigand && hasCavity && fxmol.getRole() == V3DMolecule.MoleculeRole.MACROMOLECULE) {
			StereoMolecule ligand = mConformerPanel.getOverlayMolecule();
			JFXMolViewerPanel.markAtomsInCropDistance(fxmol.getMolecule(), ligand, ligand.getCenterOfGravity());
		}
	}

	private void setSuperposeMode(boolean isSuperpose, boolean isShapeAlign, int cavityColumn) {
		SwingUtilities.invokeLater(() -> {
			HashMap<String, String> map = new HashMap<>();
			map.put(CompoundTableConstants.cColumnPropertySuperpose, isSuperpose ? CompoundTableConstants.cSuperposeValueReferenceRow : null);
			map.put(CompoundTableConstants.cColumnPropertySuperposeAlign, isShapeAlign ? CompoundTableConstants.cSuperposeAlignValueShape : null);
			new DETaskSetColumnProperties(getFrame(), mCoordsColumn, map, false).defineAndRun();
			update3DView(isSuperpose, isShapeAlign, cavityColumn);
		});
	}

	private void setShowNaturalLigand(boolean isShowNaturalLigand) {
		String ligand = isShowNaturalLigand ? mTableModel.getColumnProperty(mCoordsColumn, CompoundTableConstants.cColumnPropertyNaturalLigand) : null;
		StereoMolecule ligandMol = (ligand == null) ? null : new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(ligand);
		mConformerPanel.setOverlayMolecule(ligandMol);
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
			new AtomAssembler(queryMol).addImplicitHydrogens();
		mConformerPanel.setOverlayMolecule(queryMol);
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

	private void setShowInteractions(boolean showInteractions) {
		mConformerPanel.getV3DScene().setShowInteractions(showInteractions);
	}

	/**
	 * @param isSuperpose whether the active row's conformer shall be shown in addition
	 * @param isAlign whether the shown conformer(s) shall be rigidly aligned to the active row's conformer (if shown)
	 */
	public void update3DView(boolean isSuperpose, boolean isAlign, int cavityColumn) {
		new Thread(() -> {
			StereoMolecule[] rowMol = null;
			if (mParentRecord != null)
				rowMol = getConformers(mCoordsColumn, mParentRecord, true);

			StereoMolecule[] refMol = null;
			if (isSuperpose && mTableModel.getActiveRow() != null && mTableModel.getActiveRow() != mParentRecord)
				refMol = getConformers(mCoordsColumn, mTableModel.getActiveRow(), false);

			if (rowMol != null) {
				StereoMolecule best = null;
				if (isAlign && refMol != null) {
					double maxFit = 0;
					for (StereoMolecule stereoMolecule : rowMol) {
						double fit = PheSAAlignmentOptimizer.alignTwoMolsInPlace(refMol[0], stereoMolecule);
						if (fit>maxFit) {
							maxFit = fit;
							best = stereoMolecule;
						}
					}

					rowMol = new StereoMolecule[1];
					rowMol[0] = best;
				}
			}

			if (cavityColumn != -1 && mParentRecord != null) {
				StereoMolecule[] cavity = getConformers(cavityColumn, mParentRecord, false);
				mConformerPanel.clear();
				mConformerPanel.setProteinCavity(cavity == null || cavity.length==0 ? null : cavity[0], rowMol == null ? null : rowMol[0], true);
			}

			mConformerPanel.updateConformers(rowMol, refMol == null ? null : refMol[0]);
		}).start();
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