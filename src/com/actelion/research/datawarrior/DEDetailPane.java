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

package com.actelion.research.datawarrior;

import com.actelion.research.chem.*;
import com.actelion.research.chem.conf.AtomAssembler;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.flexophore.FlexophoreAtomContributionColors;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.datawarrior.fx.CompoundRecordMenuController;
import com.actelion.research.datawarrior.fx.JFXMolViewerPanel;
import com.actelion.research.gui.*;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.gui.form.JHTMLDetailView;
import com.actelion.research.gui.form.JImageDetailView;
import com.actelion.research.gui.form.JResultDetailView;
import com.actelion.research.gui.form.JSVGDetailView;
import com.actelion.research.table.CompoundTableChemistryCellRenderer;
import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.JDetailTable;
import com.actelion.research.table.model.*;
import com.actelion.research.table.view.VisualizationColor;
import org.openmolecules.fx.viewer3d.V3DScene;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.DnDConstants;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;

public class DEDetailPane extends JMultiPanelView implements HighlightListener,CompoundTableListener,CompoundTableColorHandler.ColorListener {
	private static final long serialVersionUID = 0x20060904;

	private static final String TYPE_STRUCTURE_2D = "Structure";
	private static final String TYPE_STRUCTURE_3D = "3D-Structure";
	private static final String TYPE_REACTION = "Reaction";
	private static final String TYPE_ROW_DATA = "Data";
	private static final String TYPE_IMAGE = "Image";
	protected static final String TYPE_DETAIL = "Detail";

	private final DEFrame mFrame;
	private final CompoundTableModel mTableModel;
	private CompoundRecord mHighlightedRecord, mActiveRecord;
	private final JDetailTable mDetailTable;
	private final ArrayList<DetailViewInfo> mDetailViewList;

	public DEDetailPane(DEFrame frame, CompoundTableModel tableModel) {
		super();
		mFrame = frame;
		mTableModel = tableModel;
		mTableModel.addCompoundTableListener(this);
		mTableModel.addHighlightListener(this);

		setMinimumSize(new Dimension(100, 100));
		setPreferredSize(new Dimension(100, 100));

		mDetailTable = new JDetailTable(new DetailTableModel(mTableModel));
		Font tableFont = UIManager.getFont("Table.font");
		mDetailTable.setFont(tableFont.deriveFont(Font.PLAIN, tableFont.getSize() * 11 / 12));
		mDetailTable.putClientProperty("Quaqua.Table.style", "striped");

		// to eliminate the disabled default action of the JTable when typing menu-V
		mDetailTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "none");

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(mDetailTable);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		add(scrollPane, TYPE_ROW_DATA);

		mDetailViewList = new ArrayList<>();
	}

	public void setColorHandler(CompoundTableColorHandler colorHandler) {
		mDetailTable.setColorHandler(colorHandler);
		colorHandler.addColorListener(this);
	}

	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cAddColumns) {
			int firstNewView = mDetailViewList.size();
			addColumnDetailViews(e.getColumn());

			for (int i = firstNewView; i < mDetailViewList.size(); i++)
				updateDetailView(mDetailViewList.get(i));
		} else if (e.getType() == CompoundTableEvent.cNewTable) {
			mHighlightedRecord = null;
			mActiveRecord = null;

			for (DetailViewInfo viewInfo : mDetailViewList)
				remove(viewInfo.view);
			mDetailViewList.clear();

			addColumnDetailViews(0);
		} else if (e.getType() == CompoundTableEvent.cChangeColumnName) {
			for (DetailViewInfo viewInfo : mDetailViewList) {
				if (e.getColumn() == viewInfo.column) {
					String title = createDetailTitle(viewInfo);
					if (title != null) {
						for (int i = 0; i < getViewCount(); i++) {
							if (getView(i) == viewInfo.view) {
								setTitle(i, title);
								break;
							}
						}
					}
				}
			}
		} else if (e.getType() == CompoundTableEvent.cChangeColumnData) {
			for (DetailViewInfo viewInfo : mDetailViewList)
				if (e.getColumn() == viewInfo.column)
					updateDetailView(viewInfo);
		} else if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			for (int i = mDetailViewList.size() - 1; i >= 0; i--) {
				DetailViewInfo viewInfo = mDetailViewList.get(i);
				viewInfo.column = e.getMapping()[viewInfo.column];

				// for STRUCTURE(_3D) types viewInfo.detail contains the coordinate column
				if (viewInfo.type.equals(TYPE_STRUCTURE_2D) || viewInfo.type.equals(TYPE_REACTION)) {
					if (viewInfo.detail != -1)  // 2D-coords may be generated on-the-fly
						viewInfo.detail = e.getMapping()[viewInfo.detail];
				}
				if (viewInfo.type.equals(TYPE_STRUCTURE_3D)) {
					viewInfo.detail = e.getMapping()[viewInfo.detail];
					if (viewInfo.detail == -1)  // remove view if 3D-coords missing
						viewInfo.column = -1;
					else
						((CompoundRecordMenuController)((JFXMolViewerPanel)viewInfo.view).getPopupMenuController()).updateCoordsColumn(viewInfo.detail);
				}

				if (viewInfo.column == -1) {
					remove(viewInfo.view);
					mDetailViewList.remove(i);
				}
			}
		} else if (e.getType() == CompoundTableEvent.cAddColumnDetails) {
			int column = e.getColumn();
			// If column >=mTableModel.getTotalColumnCount(), then we are adding new columns with details.
			// In this case we get an addColumns event later and add the detail views then.
			if (column < mTableModel.getTotalColumnCount()) {
				for (int detail = 0; detail < mTableModel.getColumnDetailCount(column); detail++) {
					boolean found = false;
					for (DetailViewInfo viewInfo : mDetailViewList) {
						if (viewInfo.type.equals(TYPE_DETAIL) && e.getColumn() == viewInfo.column && viewInfo.detail == detail) {
							found = true;
							break;
						}
					}
					if (!found)
						addResultDetailView(column, detail);
				}
			}
		} else if (e.getType() == CompoundTableEvent.cRemoveColumnDetails) {
			for (int i = mDetailViewList.size() - 1; i >= 0; i--) {
				DetailViewInfo viewInfo = mDetailViewList.get(i);
				if (viewInfo.type.equals(TYPE_DETAIL) && e.getColumn() == viewInfo.column) {
					viewInfo.detail = e.getMapping()[viewInfo.detail];
					if (viewInfo.detail == -1) {
						remove(viewInfo.view);
						mDetailViewList.remove(i);
					}
				}
			}
		} else if (e.getType() == CompoundTableEvent.cChangeActiveRow) {
			if (mActiveRecord != mTableModel.getActiveRow()) {
				mActiveRecord = mTableModel.getActiveRow();

				for (DetailViewInfo viewInfo : mDetailViewList)
					if (viewInfo.type.equals(TYPE_STRUCTURE_3D))    // currently, only the 3D-view may show the reference (active) compound
						updateDetailView(viewInfo);
			}
		}
	}

	public void colorChanged(int column, int type, VisualizationColor color) {
		for (DetailViewInfo viewInfo : mDetailViewList) {
			if (viewInfo.column == column) {
				if (viewInfo.type.equals(TYPE_STRUCTURE_2D) || viewInfo.type.equals(TYPE_REACTION)) {
					updateDetailView(viewInfo);
				}
			}
		}
		mDetailTable.repaint();
	}

	public CompoundTableModel getTableModel() {
		return mTableModel;
	}

	public ArrayList<DetailViewInfo> getDetailViewInfos() {
		return mDetailViewList;
	}

	protected void addColumnDetailViews(int firstColumn) {
		for (int column = firstColumn; column < mTableModel.getTotalColumnCount(); column++) {
			String columnName = mTableModel.getColumnTitleNoAlias(column);
			String specialType = mTableModel.getColumnSpecialType(column);
			if (CompoundTableModel.cColumnTypeIDCode.equals(specialType)) {
				int coordinateColumn = mTableModel.getChildColumn(column, CompoundTableModel.cColumnType2DCoordinates);
				JStructureView view = new JStructureView(DnDConstants.ACTION_COPY_OR_MOVE, DnDConstants.ACTION_NONE);
				view.setDisplayMode(AbstractDepictor.cDModeHiliteAllQueryFeatures | AbstractDepictor.cDModeSuppressChiralText);
				view.setClipboardHandler(new ClipboardHandler());
				addColumnDetailView(view, column, coordinateColumn, TYPE_STRUCTURE_2D, mTableModel.getColumnTitle(column));
				continue;
			}
			if (CompoundTableModel.cColumnTypeRXNCode.equals(specialType)) {
				int coordinateColumn = mTableModel.getChildColumn(column, CompoundTableModel.cColumnType2DCoordinates);
				JChemistryView view = new JChemistryView(ExtendedDepictor.TYPE_REACTION);
				addColumnDetailView(view, column, coordinateColumn, TYPE_REACTION, mTableModel.getColumnTitle(column));
				continue;
			}
			if (CompoundTableModel.cColumnType3DCoordinates.equals(specialType)) {
				EnumSet<V3DScene.ViewerSettings> settings = V3DScene.CONFORMER_VIEW_MODE;
				final JFXMolViewerPanel view = new JFXMolViewerPanel(false, settings);
				view.adaptToLookAndFeelChanges();
				String overlay = mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertySuperposeMolecule);
				StereoMolecule overlayMol = (overlay == null) ? null : new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(overlay);
				if (overlayMol != null) {
					new AtomAssembler(overlayMol).addImplicitHydrogens();
					view.setOverlayMolecule(overlayMol);
					}

				String cavity = mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyProteinCavity);
				StereoMolecule cavityMol = (cavity == null) ? null : new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(cavity);
				boolean showLigand = !"false".equals(mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyShowNaturalLigand));
				String ligand = showLigand ? mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyNaturalLigand) : null;
				StereoMolecule ligandMol = (ligand == null) ? null : new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(ligand);
				if (cavityMol != null)
					view.setProteinCavity(cavityMol, ligandMol, true);
				if (ligandMol != null)
					view.setOverlayMolecule(ligandMol);

				addColumnDetailView(view, mTableModel.getParentColumn(column), column, TYPE_STRUCTURE_3D, mTableModel.getColumnTitle(column));
				view.setPopupMenuController(new CompoundRecordMenuController(view, mTableModel, column, true));
				continue;
			}
			if (columnName.equalsIgnoreCase("imagefilename")
					|| columnName.equals("#Image#")
					|| mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyImagePath) != null) {
				boolean useThumbNail = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyUseThumbNail).equals("true");
				String imagePath = mTableModel.getColumnProperty(column, CompoundTableModel.cColumnPropertyImagePath);
				if (imagePath == null)
					imagePath = FileHelper.getCurrentDirectory() + File.separator + "images" + File.separator;
				JImagePanel view = new JImagePanel(imagePath, useThumbNail);
				String viewName = (columnName.equalsIgnoreCase("imagefilename")
						|| columnName.equals("#Image#")) ? TYPE_IMAGE : mTableModel.getColumnTitle(column);
				addColumnDetailView(view, column, -1, TYPE_IMAGE, viewName);
				continue;
			}
			for (int detail = 0; detail < mTableModel.getColumnDetailCount(column); detail++) {
				addResultDetailView(column, detail);
			}
		}
	}

	private String createDetailTitle(DetailViewInfo info) {
		if (info.type.equals(TYPE_STRUCTURE_2D) || info.type.equals(TYPE_REACTION))
			return mTableModel.getColumnTitle(info.column);
		if (info.type.equals(TYPE_STRUCTURE_3D))
			return mTableModel.getColumnTitle(info.detail);
		if (info.type.equals(TYPE_IMAGE)) {
			String columnName = mTableModel.getColumnTitleNoAlias(info.column);
			return (columnName.equalsIgnoreCase("imagefilename")
					|| columnName.equals("#Image#")) ? TYPE_IMAGE : mTableModel.getColumnTitle(info.column);
		}
		if (info.type.equals(TYPE_DETAIL))
			return mTableModel.getColumnDetailName(info.column, info.detail) + " (" + mTableModel.getColumnTitle(info.column) + ")";
		return null;
	}

	private void addResultDetailView(int column, int detail) {
		String mimetype = mTableModel.getColumnDetailType(column, detail);
		JResultDetailView view = createResultDetailView(column, detail, mimetype);
		if (view != null)
			addColumnDetailView(view, column, detail, TYPE_DETAIL, mTableModel.getColumnDetailName(column, detail)
					+ " (" + mTableModel.getColumnTitle(column) + ")");
	}

	protected JResultDetailView createResultDetailView(int column, int detail, String mimetype) {
		CompoundTableDetailSpecification spec = new CompoundTableDetailSpecification(getTableModel(), column, detail);

		if (mimetype.equals(JHTMLDetailView.TYPE_TEXT_PLAIN)
				|| mimetype.equals(JHTMLDetailView.TYPE_TEXT_HTML))
			return new JHTMLDetailView(mTableModel.getDetailHandler(), mTableModel.getDetailHandler(), spec, mimetype);

		if (mimetype.equals(JImageDetailView.TYPE_IMAGE_JPEG)
				|| mimetype.equals(JImageDetailView.TYPE_IMAGE_GIF)
				|| mimetype.equals(JImageDetailView.TYPE_IMAGE_PNG))
			return new JImageDetailView(mTableModel.getDetailHandler(), mTableModel.getDetailHandler(), spec);

		if (mimetype.equals(JSVGDetailView.TYPE_IMAGE_SVG))
			return new JSVGDetailView(mTableModel.getDetailHandler(), mTableModel.getDetailHandler(), spec);

		return null;
	}

	protected void addColumnDetailView(JComponent view, int column, int detail, String type, String title) {
		boolean split3DFragments = "true".equals(mTableModel.getColumnProperty(detail, CompoundTableModel.cColumnProperty3DFragmentSplit));
		DetailViewInfo viewInfo = new DetailViewInfo(view, column, detail, type, split3DFragments);
		mDetailViewList.add(viewInfo);
		add(view, title);
	}

	public void highlightChanged(CompoundRecord record) {
		if (mHighlightedRecord != record && record != null) {
			mHighlightedRecord = record;

			for (DetailViewInfo viewInfo : mDetailViewList)
				updateDetailView(viewInfo);
		}
	}

	private void updateDetailView(DetailViewInfo viewInfo) {
		switch (viewInfo.type) {
			case TYPE_STRUCTURE_2D -> {
				StereoMolecule mol = null;
				StereoMolecule displayMol = null;
				if (mHighlightedRecord != null) {
					byte[] idcode = (byte[]) mHighlightedRecord.getData(viewInfo.column);
					if (idcode != null) {
						int coordinateColumn = (viewInfo.column == -1) ? -1
								: mTableModel.getChildColumn(viewInfo.column, CompoundTableModel.cColumnType2DCoordinates);
						if ((coordinateColumn != -1 && mHighlightedRecord.getData(coordinateColumn) != null)
								|| new IDCodeParser().getAtomCount(idcode, 0) <= CompoundTableChemistryCellRenderer.ON_THE_FLY_COORD_MAX_ATOMS) {
							mol = mTableModel.getChemicalStructure(mHighlightedRecord, viewInfo.column, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
							displayMol = mTableModel.getChemicalStructure(mHighlightedRecord, viewInfo.column, CompoundTableModel.ATOM_COLOR_MODE_ALL, null);
							addFlexophoreContributions(viewInfo);
						}
					}
				}
				((JStructureView) viewInfo.view).structureChanged(mol, displayMol);
			}
			case TYPE_STRUCTURE_3D -> {
				boolean isSuperpose = CompoundTableConstants.cSuperposeValueReferenceRow.equals(mTableModel.getColumnProperty(viewInfo.detail, CompoundTableConstants.cColumnPropertySuperpose));
				boolean isAlign = CompoundTableConstants.cSuperposeAlignValueShape.equals(mTableModel.getColumnProperty(viewInfo.detail, CompoundTableConstants.cColumnPropertySuperposeAlign));
				CompoundRecordMenuController controller = (CompoundRecordMenuController)((JFXMolViewerPanel)viewInfo.view).getPopupMenuController();
				controller.setParentRecord(mHighlightedRecord);
				controller.update3DView(isSuperpose, isAlign);
			}
			case TYPE_REACTION -> {
				Reaction rxn = null;
				if (mHighlightedRecord != null)
					rxn = mTableModel.getChemicalReaction(mHighlightedRecord, viewInfo.column, CompoundTableModel.ATOM_COLOR_MODE_ALL);
				((JChemistryView)viewInfo.view).setContent(rxn);
			}
			case TYPE_IMAGE -> ((JImagePanel)viewInfo.view).setFileName((mHighlightedRecord == null) ? null
					: mTableModel.encodeData(mHighlightedRecord, viewInfo.column));
			case TYPE_DETAIL -> {
				if (mHighlightedRecord == null) {
					((JResultDetailView)viewInfo.view).setReferences(null);
				} else {
					String[][] reference = mHighlightedRecord.getDetailReferences(viewInfo.column);
					((JResultDetailView)viewInfo.view).setReferences(reference == null
							|| reference.length<=viewInfo.detail ?
							null : reference[viewInfo.detail]);
				}
			}
		}
	}


	/**
	 * If a Flexophore similarity filter is active, then this method calculates atom contributions to the
	 * Flexophore match and adds them as proper color/radius encodings to the 2D structure display.
	 */
	private void addFlexophoreContributions(DetailViewInfo viewInfo) {
		FlexophoreAtomContributionColors facc = null;
		CompoundRecord highlightedRow = mTableModel.getHighlightedRow();
		if (highlightedRow != null) {
			int flexophoreColumn = mTableModel.getChildColumn(viewInfo.column, DescriptorConstants.DESCRIPTOR_Flexophore.shortName);
			if (flexophoreColumn != -1)
				facc = mTableModel.getMostRecentExclusionFlexophoreColors(flexophoreColumn);
			}
		((JStructureView)viewInfo.view).setAtomHighlightColors(facc == null ? null : facc.getMolARGB(), facc == null ? null : facc.getMolRadius());
	}

	public static class DetailViewInfo {
		public JComponent view;
		public int column, detail;
		public String type;
		public boolean split3DFragments;

		public DetailViewInfo(JComponent view, int column, int detail, String type, boolean split3DFragments) {
			this.view = view;
			this.column = column;   // is idcode column in case of STRUCTURE(_3D)
			this.detail = detail;   // is coordinate column in case of STRUCTURE(_3D)
			this.type = type;
			this.split3DFragments = split3DFragments;
		}
	}
}