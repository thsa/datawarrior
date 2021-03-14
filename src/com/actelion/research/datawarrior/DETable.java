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

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.datawarrior.task.table.DETaskJumpToReferenceRow;
import com.actelion.research.gui.JDrawDialog;
import com.actelion.research.gui.dnd.MoleculeTransferable;
import com.actelion.research.gui.dnd.ReactionTransferable;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.gui.hidpi.HiDPIIconButton;
import com.actelion.research.gui.table.JTableWithRowNumbers;
import com.actelion.research.table.CompoundTableChemistryCellRenderer;
import com.actelion.research.table.MultiLineCellRenderer;
import com.actelion.research.table.model.*;
import com.actelion.research.table.view.VisualizationColor;
import com.actelion.research.util.BrowserControl;
import com.actelion.research.util.CursorHelper;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.regex.PatternSyntaxException;

public class DETable extends JTableWithRowNumbers implements ActionListener,ComponentListener,CompoundTableListener {
	private static final long serialVersionUID = 0x20060904;

	public static final int DEFAULT_FONT_SIZE = 12;
	private static final Color LOOKUP_COLOR = new Color(99, 99, 156);
	private static final Color EMBEDDED_DETAIL_COLOR = new Color(108, 156, 99);
	private static final Color REFERENCED_DETAIL_COLOR = new Color(156, 99, 99);

	private Frame mParentFrame;
	private DEMainPane mMainPane;
	private JScrollPane mScrollPane;
	private CompoundRecord mCurrentRecord;
	private TreeMap<String,Integer> mNonExpandedColumnSizes/*,mHiddenColumnSizes*/;
	private TreeMap<String,DEHiddenColumn> mHiddenColumnMap;
	private TreeMap<String,Color> mColumnGroupToColorMap;
	private JButton mJumpButton, mExpandButton, mSearchButton;
	private Object mDraggedObject;
	private JTextField mTextFieldSearch;
	private JWindow mSearchControls;
	private Point mSearchPopupLocation;
	private DEColumnOrder mIntendedColumnOrder;
	private int mPreviousClickableCellCol,mPreviousClickableCellRow;

	public DETable(Frame parentFrame, DEMainPane mainPane, ListSelectionModel sm) {
		super(mainPane.getTableModel(), null, sm);

		mParentFrame = parentFrame;
		mMainPane = mainPane;
		mainPane.getTableModel().addCompoundTableListener(this);

		mPreviousClickableCellRow = -1;
		mPreviousClickableCellCol = -1;

		// to eliminate the disabled default action of the JTable when typing menu-V
		getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "none");

		// to eliminate the default action of the JTable when typing menu-C that the higher level DataWarrior menu can take over (and include the header line when copying)
		getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "none");

		getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "editCell");
		getActionMap().put("editCell", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (getSelectedRowCount() == 1
						&& getSelectedColumnCount() == 1) {
					int selectedRow = getSelectedRow();
					final CompoundRecord record = mainPane.getTableModel().getRecord(selectedRow);
					int column = convertTotalColumnIndexFromView(getSelectedColumn());
					editCell(record, column);
					SwingUtilities.invokeLater(() -> {
						if (mainPane.getTableModel().isVisible(record))
							getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
					});
				}
			}
		});

		mCurrentRecord = mainPane.getTableModel().getActiveRow();

		mIntendedColumnOrder = new DEColumnOrder(mainPane.getTableModel(), getColumnModel());
		mHiddenColumnMap = new TreeMap<>();

		setFontSize(DEFAULT_FONT_SIZE);

		// JTable() calls setModel() causing updating table renderers, which sets the correct row height.
		// Then it calls initializeLocalVars() to set the row height back to 16. Thus, we need to correct again...
		updateRowHeight();

		setDragEnabled(true);
		setDropMode(DropMode.ON);   // change drag cell selection behaviour to drag cell content
		setTransferHandler(new TransferHandler(null) {
			@Override
			public int getSourceActions(JComponent c) {
				return COPY_OR_MOVE;
			}

			@Override
			public Transferable createTransferable(JComponent c) {
				if (mDraggedObject instanceof Reaction)
					return new ReactionTransferable(new Reaction((Reaction)mDraggedObject));
				if (mDraggedObject instanceof StereoMolecule)
					return new MoleculeTransferable(((StereoMolecule)mDraggedObject).getCompactCopy());
				else
					return new StringSelection((String)mDraggedObject);
			}

			@Override
			public void exportDone(JComponent c, Transferable t, int action) {
				mDraggedObject = null;
//				if (action == MOVE)
//					removeCellContent();
			}
		});

		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				updateClickableCellsAfterMouseMotion(e.getPoint());
				setCursor(CursorHelper.getCursor(
						isClickableCell(e.getPoint()) && getClickableCellEntry(e.getPoint()) != null ?
								CursorHelper.cPointedHandCursor
						: isSelectedFilledCell(e.getPoint()) ? CursorHelper.cHandCursor
						: CursorHelper.cPointerCursor));
			}
		});

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (!isClickableCell(e.getPoint()) || getClickableCellEntry(e.getPoint()) == null)
					setCursor(CursorHelper.getCursor(e.getButton() == MouseEvent.BUTTON1 ?
							CursorHelper.cFistCursor : CursorHelper.cPointerCursor));
				}

			@Override
			public void mouseClicked(MouseEvent e) {
				Point p = e.getPoint();
				if (isClickableCell(p) && getClickableCellEntry(p) != null) {
					int column = convertTotalColumnIndexFromView(columnAtPoint(p));
					String entry = getClickableCellEntry(p);
					if (entry != null) {
						CompoundTableModel tableModel = (CompoundTableModel)getModel();
						String url = tableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyLookupURL+"0");
						if (CompoundTableConstants.cColumnPropertyLookupFilterRemoveMinus.equals(
								tableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyLookupFilter+"0")))
							entry = entry.replace("-", "");
						BrowserControl.displayURL(url.replace("%s", entry));
						}
					}
				else if (isSelectedFilledCell(e.getPoint())) {
					setCursor(CursorHelper.getCursor(CursorHelper.cHandCursor));
					}
				}

			@Override
			public void mouseExited(MouseEvent e) {
				setCursor(CursorHelper.getCursor(CursorHelper.cPointerCursor));
				}
			});

		mTextFieldSearch = new JTextField(6);
		int gap = HiDPIHelper.scale(2);
		double[][] size = {{gap, TableLayout.PREFERRED, gap}, {gap, TableLayout.PREFERRED, gap}};
		mSearchControls = new JWindow(mParentFrame);
		mSearchControls.getContentPane().setLayout(new TableLayout(size));
		mSearchControls.getContentPane().add(mTextFieldSearch, "1,1");

		mTextFieldSearch.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
					mTextFieldSearch.setText("");
					filterColumnVisibility("");
					hideSearchControls();
				}
				super.keyTyped(e);
				SwingUtilities.invokeLater(() -> {
					String s = mTextFieldSearch.getText();
					filterColumnVisibility(s);
					} );
				}
			} );

		SwingUtilities.invokeLater(() -> createTopLeftButtons());
		SwingUtilities.invokeLater(() -> createTopRightButton());
		}

	public void editCell(CompoundRecord record, int valueColumn) {
		CompoundTableModel tableModel = (CompoundTableModel)getModel();
		String columnType = tableModel.getColumnSpecialType(valueColumn);
		if (columnType == null) {
			byte[] bytes = (byte[])record.getData(valueColumn);
			String oldValue = (bytes == null) ? "" : new String(bytes);
			DETableDialog dialog = new DETableDialog(mParentFrame, "Edit '" + tableModel.getColumnTitle(valueColumn) + "'", oldValue);
			String newValue = dialog.getNewValue();
			if (newValue != null) {
				if (tableModel.isColumnTypeRangeCategory(valueColumn)
						&& !tableModel.getNativeCategoryList(valueColumn).containsString(newValue)) {
					JOptionPane.showMessageDialog(mParentFrame, "For columns that contain range categories, you need to type an existing range.");
				} else {
					record.setData(newValue.length() == 0 ? null : newValue.getBytes(), valueColumn, true);
					tableModel.finalizeChangeCell(record, valueColumn);
				}
			}
		} else if (columnType.equals(CompoundTableModel.cColumnTypeIDCode)) {
			StereoMolecule oldMol = tableModel.getChemicalStructure(record, valueColumn, CompoundTableModel.ATOM_COLOR_MODE_EXPLICIT, null);
			JDrawDialog dialog = new JDrawDialog(mParentFrame, oldMol, "Edit '" + tableModel.getColumnTitle(valueColumn) + "'");
			if (tableModel.getChildColumn(valueColumn, CompoundTableConstants.cColumnTypeAtomColorInfo) != -1)
				dialog.getDrawArea().setAtomColorSupported(true);
			dialog.setVisible(true);
			if (!dialog.isCancelled()) {
				if (tableModel.setChemicalStructure(record, dialog.getStructure(), valueColumn))
					tableModel.finalizeChangeCell(record, valueColumn);
			}
		} else if (columnType.equals(CompoundTableModel.cColumnTypeRXNCode)) {
			Reaction oldRxn = tableModel.getChemicalReaction(record, valueColumn, CompoundTableModel.ATOM_COLOR_MODE_EXPLICIT);
			JDrawDialog dialog = new JDrawDialog(mParentFrame, oldRxn, "Edit '" + tableModel.getColumnTitle(valueColumn) + "'");
			dialog.setVisible(true);
			if (!dialog.isCancelled()) {
				Reaction newRxn = dialog.getReactionAndDrawings();

				// rescue catalysts, because the editor doesn't handle them
				if (newRxn != null && oldRxn != null)
					for (int i = 0; i<oldRxn.getCatalysts(); i++)
						newRxn.addCatalyst(oldRxn.getCatalyst(i));
				if (tableModel.setChemicalReaction(record, newRxn, valueColumn))
					tableModel.finalizeChangeCell(record, valueColumn);
			}
		}
	}

	@Override
	public void processMouseEvent(MouseEvent e) {
		if (e.getID() == MouseEvent.MOUSE_PRESSED)
			mDraggedObject = getDraggedObject(e.getPoint());

		super.processMouseEvent(e);
	}

	private Object getDraggedObject(Point p) {
		int row = rowAtPoint(p);
		int col = columnAtPoint(p);

		if (!isCellSelected(row, col))
			return null;

		CompoundTableModel tableModel = (CompoundTableModel)getModel();
		int column = convertTotalColumnIndexFromView(col);
		CompoundRecord record = tableModel.getRecord(row);
		if (tableModel.isColumnTypeReaction(column))
			return tableModel.getChemicalReaction(record, column, CompoundTableModel.ATOM_COLOR_MODE_NONE);
		if (tableModel.isColumnTypeStructure(column))
			return tableModel.getChemicalStructure(record, column, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
		else if (tableModel.getColumnSpecialType(column) == null)
			return tableModel.encodeData(record, column);

		return null;
		}

	private boolean isClickableCell(Point p) {
		int row = rowAtPoint(p);
		int col = columnAtPoint(p);
		return isClickableCell(row, col);
		}

	private void updateClickableCellsAfterMouseMotion(Point p) {
		int row = rowAtPoint(p);
		int col = columnAtPoint(p);
		boolean isClickableCell = isClickableCell(row, col);

		if (mPreviousClickableCellRow != -1
		 && (mPreviousClickableCellRow != row || mPreviousClickableCellCol != col)) {
			((MultiLineCellRenderer)getColumnModel().getColumn(mPreviousClickableCellCol).getCellRenderer()).setMouseLocation(10000, 10000, mPreviousClickableCellRow);
			repaint(getCellRect(mPreviousClickableCellRow, mPreviousClickableCellCol, false));
			mPreviousClickableCellRow = -1;
			mPreviousClickableCellCol = -1;
			}

		if (isClickableCell) {
			Rectangle cellRect = getCellRect(row, col, false);
			((MultiLineCellRenderer)getColumnModel().getColumn(col).getCellRenderer()).setMouseLocation(p.x - cellRect.x, p.y - cellRect.y, row);
			repaint(cellRect);
			mPreviousClickableCellRow = row;
			mPreviousClickableCellCol = col;
			}
		}

	private boolean isClickableCell(int row, int col) {
		CompoundTableModel tableModel = (CompoundTableModel)getModel();
		int column = convertTotalColumnIndexFromView(col);
		return tableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyLookupCount) != null
			&& tableModel.getRecord(row).getData(column) != null;
		}

	private String getClickableCellEntry(Point p) {
		int row = rowAtPoint(p);
		int col = columnAtPoint(p);
		TableColumn tc = getColumnModel().getColumn(col);
		return ((MultiLineCellRenderer)tc.getCellRenderer()).getClickableEntry(row);
		}

	private boolean isSelectedFilledCell(Point p) {
		int row = rowAtPoint(p);
		int col = columnAtPoint(p);
		if (!isCellSelected(row, col))
			return false;

		CompoundTableModel tableModel = (CompoundTableModel)getModel();
		int column = convertTotalColumnIndexFromView(col);
		CompoundRecord record = tableModel.getRecord(row);
		return record.getData(column) != null;
	}

	private void updateRowHeight() {
		int rowHeight = 16;
		for (int viewColumn = 0; viewColumn<getColumnCount(); viewColumn++)
			rowHeight = Math.max(rowHeight, getPreferredRowHeight(viewColumn));
		setRowHeight(HiDPIHelper.scale(rowHeight));
	}

	private int getPreferredRowHeight(int viewColumn) {
		int column = convertTotalColumnIndexFromView(viewColumn);
		if (((CompoundTableModel)getModel()).getColumnSpecialType(column) != null)
			return 80;
		return ((CompoundTableModel)getModel()).isMultiLineColumn(column) ? 40 : 16;
	}

	/**
	 * @return font size of parent table devided by UI scaling factor
	 */
	@Override
	public int getFontSize() {
		return Math.round(super.getFontSize() / HiDPIHelper.getUIScaleFactor());
	}

	/**
	 * @param fontSize font size not including UI scaling factor
	 */
	@Override
	public void setFontSize(int fontSize) {
		super.setFontSize(HiDPIHelper.scale(fontSize));
		}

	@Override
	public void updateUI() {
		super.updateUI();

		if (mSearchControls != null)
			SwingUtilities.updateComponentTreeUI(mSearchControls);

		SwingUtilities.invokeLater(() -> updateRowHeaderMinWidth() );
		}

	public void updateTableRenderers(boolean initializeColumnWidths) {
		int rowHeight = 16;
		for (int viewColumn=0; viewColumn<getColumnCount(); viewColumn++)
			rowHeight = Math.max(rowHeight, updateTableRenderer(viewColumn, initializeColumnWidths));
		setRowHeight(HiDPIHelper.scale(rowHeight));
		}

	@Override
	public void setRowHeight(int height) {
		super.setRowHeight(height);
		}

	private void createTopLeftButtons() {
		JPanel bp = new JPanel();
		bp.setBackground(UIManager.getColor("TableHeader.background"));
		double[][] size = {{TableLayout.PREFERRED, TableLayout.FILL, TableLayout.PREFERRED},{TableLayout.FILL}};
		bp.setLayout(new TableLayout(size));

		mJumpButton = new HiDPIIconButton("jumpTo.png", "Jump to reference row", "jump");
		mJumpButton.addActionListener(this);
		bp.add(mJumpButton, "0,0");

		mExpandButton = new HiDPIIconButton("expand.png", "Expand all columns", "expand");
		mExpandButton.addActionListener(this);
		bp.add(mExpandButton, "2,0");

		updateRowHeaderMinWidth();

		((JScrollPane)getParent().getParent()).setCorner(JScrollPane.UPPER_LEFT_CORNER, bp);
		}

	private void createTopRightButton() {
		JPanel bp = new JPanel();
		bp.setBackground(UIManager.getColor("TableHeader.background"));
		double[][] size = {{TableLayout.FILL},{TableLayout.FILL}};
		bp.setLayout(new TableLayout(size));

		mSearchButton = new HiDPIIconButton("search.png", "Filter table columns", "search");
		mSearchButton.addActionListener(this);
		bp.add(mSearchButton, "0,0");

		((JScrollPane)getParent().getParent()).setCorner(JScrollPane.UPPER_RIGHT_CORNER, bp);
		}

	/**
	 * Shows or hides a popup at the top left corner containing
	 * comboboxes for column selection and pruning bars.
	 */
	public void showSearchControls() {
		if (mSearchButton == null) {
			// If the table view is set to front with a filled column search field from
			// DERuntimeProperties when opening a file, then it may happen that mSearchButton
			// is not existing yet, because adding the buttons is delayed in the DETable
			// constructor, because the parent JScollpane is not yet accessible.
			SwingUtilities.invokeLater(() -> showSearchControls());
			}
		else {
			mSearchPopupLocation = mSearchButton.getLocationOnScreen();
			mSearchPopupLocation.translate(mSearchButton.getWidth(), 0);
			mSearchControls.pack();
			mSearchControls.setLocation(mSearchPopupLocation);
			mSearchControls.setVisible(true);
			mSearchControls.toFront();

			mParentFrame.addComponentListener(this);
			getParent().getParent().addComponentListener(this);	// this is the parent scrollpane
			}
		}

	public void hideSearchControls() {
		if (mSearchControls.isVisible()) {
			mSearchControls.setVisible(false);
			mParentFrame.removeComponentListener(this);
			removeComponentListener(this);
			return;
			}
		}

	@Override
	public void componentHidden(ComponentEvent e) {
		if (mSearchControls.isVisible())
			hideSearchControls();
		}

	@Override
	public void componentMoved(ComponentEvent e) {
		relocateSearchPopup();
		}

	@Override
	public void componentResized(ComponentEvent e) {
		relocateSearchPopup();
		}

	private void relocateSearchPopup() {
		if (!mSearchButton.isShowing()) {
			hideSearchControls();
			}
		else {
			mSearchPopupLocation = mSearchButton.getLocationOnScreen();
			mSearchPopupLocation.translate(mSearchButton.getWidth(), 0);
			mSearchControls.setLocation(mSearchPopupLocation);
			}
		}

	@Override
	public void componentShown(ComponentEvent e) {}

	private void updateRowHeaderMinWidth() {
		if (mJumpButton != null)
			setRowHeaderMinWidth(mJumpButton.getPreferredSize().width + mExpandButton.getPreferredSize().width);
		}

	public void scrollToRow(int row) {
		scrollRectToVisible(new Rectangle(0, row * getRowHeight(), getWidth(), getRowHeight()));
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("jump")) {
			new DETaskJumpToReferenceRow(mParentFrame, mMainPane).defineAndRun();
			return;
			}
		if (e.getActionCommand().equals("expand")) {
			CompoundTableModel tableModel = (CompoundTableModel)getModel();
			if (mNonExpandedColumnSizes == null) {
				mNonExpandedColumnSizes = new TreeMap<String,Integer>();
				for (int viewColumn=0; viewColumn<getColumnCount(); viewColumn++) {
					int column = convertTotalColumnIndexFromView(viewColumn);
					String key = tableModel.getColumnTitleNoAlias(column);
					int oldWidth = getColumnModel().getColumn(viewColumn).getPreferredWidth();
					mNonExpandedColumnSizes.put(key, new Integer(oldWidth));
					String name = tableModel.getColumnTitle(column);
					int width = getFont().getSize() + SwingUtilities.computeStringWidth(getFontMetrics(getFont()), name);
					getColumnModel().getColumn(viewColumn).setPreferredWidth(Math.max(oldWidth, width));
					}
				}
			else {
				for (int viewColumn=0; viewColumn<getColumnCount(); viewColumn++) {
					int column = convertTotalColumnIndexFromView(viewColumn);
					String name = tableModel.getColumnTitleNoAlias(column);
					Integer width = mNonExpandedColumnSizes.get(name);
					if (width != null)
						getColumnModel().getColumn(viewColumn).setPreferredWidth(width);
					}
				mNonExpandedColumnSizes = null;
				}
			return;
			}
		if (e.getActionCommand().equals("search")) {
			if (mSearchControls.isVisible())
				hideSearchControls();
			else
				showSearchControls();
			}
		}

	/**
	 * @param viewColumn
	 * @return preferred row height (not corrected for HiDPI displays)
	 */
	private int updateTableRenderer(int viewColumn, boolean initializeColumnWidths) {
		int column = convertTotalColumnIndexFromView(viewColumn);
		TableColumn tc = getColumnModel().getColumn(viewColumn);

		String specialType = ((CompoundTableModel)getModel()).getColumnSpecialType(column);
		if (specialType != null) {
			TableCellRenderer renderer = tc.getCellRenderer();
			if (!(renderer instanceof CompoundTableChemistryCellRenderer)) {
				renderer = new CompoundTableChemistryCellRenderer();
				((CompoundTableChemistryCellRenderer)renderer).setAlternateRowBackground(true);
				tc.setCellRenderer(renderer);
				}

			boolean isReaction = specialType.equals(CompoundTableModel.cColumnTypeRXNCode);
			((CompoundTableChemistryCellRenderer)renderer).setReaction(isReaction);

			if (initializeColumnWidths)
		   		tc.setPreferredWidth(HiDPIHelper.scale(isReaction ? 200 : 100));

			return 80;
			}

		boolean isURL = ((CompoundTableModel)getModel()).getColumnProperty(column,
				CompoundTableConstants.cColumnPropertyLookupCount) != null;

		if (((CompoundTableModel)getModel()).isMultiLineColumn(column)) {
			if (!(tc.getCellRenderer() instanceof MultiLineCellRenderer)) {
				MultiLineCellRenderer renderer = new MultiLineCellRenderer();
				renderer.setAlternateRowBackground(true);
				renderer.setIsURL(isURL);
				tc.setCellRenderer(renderer);
				}

			if (initializeColumnWidths)
				tc.setPreferredWidth(HiDPIHelper.scale(80));

			return 40;
			}

		if (!(tc.getCellRenderer() instanceof MultiLineCellRenderer)) {
			MultiLineCellRenderer renderer = new MultiLineCellRenderer();
			renderer.setLineWrap(false);
			renderer.setAlternateRowBackground(true);
			renderer.setIsURL(isURL);
			tc.setCellRenderer(renderer);
			}

		if (initializeColumnWidths)
			tc.setPreferredWidth(HiDPIHelper.scale(80));

		return 16;
		}

	public void cleanup() {
		mHiddenColumnMap.clear();
		getModel().removeTableModelListener(this);
		((CompoundTableModel)getModel()).removeCompoundTableListener(this);
		}

	@Override
	public void paintComponent(Graphics g) {
			// Deleting large numbers of records at the end of an even larger table
			// causes sometimes paint() calls from which g.getClipBounds() and this.getBounds()
			// reflect the situation before the actual deletion while rowAtPoint() accesses the
			// new situation and thus returns -1. The standard implementation of TableUIs in various
			// LAFs repaints all(!!!) table rows. This may take minutes if we have some 100.000 rows
			// and must be prevented by the following.
		Rectangle clip = g.getClipBounds();
		Point upperLeft = clip.getLocation();
		Point lowerRight = new Point(clip.x + clip.width - 1, clip.y
				+ clip.height - 1);
		int rMin = rowAtPoint(upperLeft);
		int rMax = rowAtPoint(lowerRight);
		if (rMin == -1 && rMax == -1)
			return;

		CompoundTableModel tableModel = (CompoundTableModel)getModel();

		super.paintComponent(g);

		if (getColumnCount() != 0 && getRowCount() != 0) {
			int firstRow = 0;
			int lastRow = getRowCount()-1;

			if (mScrollPane != null) {
				Rectangle viewRect = mScrollPane.getViewport().getViewRect();
				int rowAtTop = rowAtPoint(new Point(0, viewRect.y));
				int rowAtBottom = rowAtPoint(new Point(0, viewRect.y + viewRect.height));
				firstRow = Math.max(firstRow, rowAtTop);
				lastRow = (rowAtBottom == -1) ? lastRow : Math.min(lastRow, rowAtBottom);
				}

					// draw red frame of current record
			if (mCurrentRecord != null) {
				for (int row=firstRow; row<=lastRow; row++) {
					if (((CompoundTableModel)getModel()).getRecord(row) == mCurrentRecord) {
						int tableWidth = getWidth();
						Rectangle cellRect = getCellRect(row, 0, false);
						g.setColor(Color.red);
						g.drawRect(0, cellRect.y-1, tableWidth-1, cellRect.height+1);
						g.drawRect(1, cellRect.y, tableWidth-3, cellRect.height-1);
						break;
						}
					}
				}

			int[] firstAndLastColumn = getFirstAndLastColumn();

			for (int column=firstAndLastColumn[0]; column<=firstAndLastColumn[1]; column++) {
				int modelColumn = convertTotalColumnIndexFromView(column);
				int detailCount = tableModel.getColumnDetailCount(modelColumn);
				int lookupCount = tableModel.getColumnLookupCount(modelColumn);
				if (detailCount != 0 || lookupCount != 0) {
					int fontSize = HiDPIHelper.scale(7);
					g.setFont(g.getFont().deriveFont(0, fontSize));
					for (int row=firstRow; row<=lastRow; row++) {
						Color color = LOOKUP_COLOR;	// lowest priority
						int count = 0;

						String[][] detail = tableModel.getRecord(row).getDetailReferences(modelColumn);
						if (detail != null) {
							for (int i=0; i<detail.length; i++) {
								if (detail[i] != null) {
									count += detail[i].length;
									if (!tableModel.getColumnDetailSource(modelColumn, i).equals(CompoundTableDetailHandler.EMBEDDED))
										color = REFERENCED_DETAIL_COLOR;
									else if (color == LOOKUP_COLOR)
										color = EMBEDDED_DETAIL_COLOR;
									}
								}
							}

						// if we have a lookup key we have potentially one detail per lookup-URL
						String[] key = tableModel.separateUniqueEntries(tableModel.encodeData(tableModel.getRecord(row), modelColumn));
						if (key != null)
							count += lookupCount * key.length;

						if (count != 0) {
							String detailString = ""+count;
							int stringWidth = g.getFontMetrics().stringWidth(detailString);
							int drawWidth = Math.max(stringWidth, HiDPIHelper.scale(12));
							Rectangle cellRect = getCellRect(row, column, false);
							g.setColor(color);
							g.fillRect(cellRect.x+cellRect.width-drawWidth, cellRect.y, drawWidth, HiDPIHelper.scale(9));
							g.setColor(Color.white);
							g.drawString(detailString, cellRect.x+cellRect.width-(drawWidth+stringWidth)/2, cellRect.y+fontSize);
							}
						}
					}
				}
			}
		}

	private int[] getFirstAndLastColumn() {
		int[] firstAndLastColumn = new int[2];
		firstAndLastColumn[1] = getColumnCount()-1;

		Rectangle viewRect = mScrollPane != null ?
				mScrollPane.getViewport().getViewRect() : new Rectangle(0, 0, getWidth(), getHeight());

		int columnAtLeft = columnAtPoint(new Point(viewRect.x, 0));
		int columnAtRight = columnAtPoint(new Point(viewRect.x + viewRect.width, 0));
		if (firstAndLastColumn[0] < columnAtLeft)
			firstAndLastColumn[0] = columnAtLeft;
		if (firstAndLastColumn[1] > columnAtRight && columnAtRight != -1)
			firstAndLastColumn[1] = columnAtRight;

		return firstAndLastColumn;
		}

	public void addNotify() {
		super.addNotify();

		if (getParent().getParent() instanceof JScrollPane)
			mScrollPane = (JScrollPane) getParent().getParent();
		}

	@Override
	protected JTableHeader createDefaultTableHeader() {
		return new JTableHeader(this.getColumnModel()) {
			private static final long serialVersionUID = 0x20110105;

			@Override
			public void paintComponent(Graphics g) {
				// This shouldn't be necessary, but without clearing the background we sometimes
				// have double buffering artefacts and see pixel conten t from other views in the background!
				// TODO Check with >JRE8 and updated Substance LaF, whether this is still necessary:
				// Open worldfactbock and click a few table cells and see, whether table header shows artefacts!
				g.setColor(getBackground());
				g.fillRect(0,0, getWidth(), getHeight());

				super.paintComponent(g);

				buildColumnGroupToColorMap();
				int[] firstAndLastColumn = getFirstAndLastColumn();

				// Draw colored lines on visible headers to indicate column groups!
				// (Using a custom header renderer to indicate column groups IS NOT AN OPTION,
				//  because the Substance LaF uses its own SubstanceDefaultTableCellHeaderRenderer.)
				for (int visColumn=firstAndLastColumn[0]; visColumn<=firstAndLastColumn[1]; visColumn++) {
					int column = convertTotalColumnIndexFromView(visColumn);
					String groupNames = ((CompoundTableModel)getModel()).getColumnProperty(column, CompoundTableConstants.cColumnPropertyDisplayGroup);
					if (groupNames != null) {
						Rectangle rect = getTableHeader().getHeaderRect(visColumn);
						String[] groupName = groupNames.split(";");
						int gap = HiDPIHelper.scale(2);
						int x = rect.x+gap;
						int w = rect.width-2*gap;
						int count = groupName.length;
						int l = (w+count-1) / count;
						for (int i=0; i<groupName.length; i++) {
							Color color = mColumnGroupToColorMap.get(groupName[i]);
							g.setColor(color == null ? Color.GRAY : color);
							g.fillRect(x + w*i/count, rect.y+gap, l, gap);
							}
						}
					}
				}

			public String getToolTipText(MouseEvent e) {
				int visColumn = getTableHeader().columnAtPoint(e.getPoint());
				if (visColumn != -1) {
					CompoundTableModel model = (CompoundTableModel)getModel();
					int column = convertTotalColumnIndexFromView(visColumn);
					StringBuilder html = new StringBuilder("<html><b>"+model.getColumnTitleNoAlias(column)+"</b>");
					String alias = model.getColumnAlias(column);
					if (alias != null)
						html.append("<br><i>alias:</i> "+alias);
					String group = model.getColumnProperty(column, CompoundTableConstants.cColumnPropertyDisplayGroup);
					if (group != null) {
						String pluralS = group.indexOf(';') == -1 ? "" : "s";
						html.append("<br><i>column group"+pluralS+":</i> " + group);
						}
					String description = model.getColumnDescription(column);
					if (description != null)
						html.append("<br><i>description:</i> "+description);
					String formula = model.getColumnProperty(column, CompoundTableConstants.cColumnPropertyFormula);
					if (formula != null)
						html.append("<br><i>Formula used:</i> "+formula);
					html.append("<br><i>perceived data type:</i> ");
					if (model.isColumnTypeStructure(column))
						html.append("chemical structure");
					else if (model.isColumnTypeReaction(column))
						html.append("chemical reaction");
					else if (model.isColumnTypeDouble(column))
						html.append("numerical");
					else if (model.isColumnTypeString(column))
						html.append("text");
					if (model.isColumnTypeCategory(column)) {
						html.append(" categories:"+model.getCategoryCount(column));
						if (model.isColumnTypeRangeCategory(column))
							html.append(" (ranges)");
						}
					if (model.isColumnTypeDate(column))
						html.append(" (date values)");
					if (model.getCategoryCustomOrder(column) != null)
						html.append("<br>Column has custom order of categories.");
					if (model.isLogarithmicViewMode(column))
						html.append("<br>Values are interpreted logarithmically.");
					if (model.isColumnDataComplete(column))
						html.append("<br>Column does not contain empty values.");
					else
						html.append("<br>Column contains empty values.");
					if (model.isColumnDataUnique(column))
						html.append("<br>Column contains single, unique values.");
					else
						html.append("<br>Column contains duplicate values.");
					if (model.isMultiEntryColumn(column))
						html.append("<br>Some cells contain multiple values.");
					if (model.isMultiLineColumn(column))
						html.append("<br>Some cells contain multiple lines.");
					if (model.getColumnDetailCount(column) != 0)
						html.append("<br>Column has "+model.getColumnDetailCount(column)+" associated details.");
					if (model.getColumnSummaryMode(column) != CompoundTableModel.cSummaryModeNormal) {
						html.append("<br>Of multiple numerical values only the <b>");
						html.append(model.getColumnSummaryMode(column) == CompoundTableModel.cSummaryModeMean ? " mean"
								  : model.getColumnSummaryMode(column) == CompoundTableModel.cSummaryModeMedian ? " median"
								  : model.getColumnSummaryMode(column) == CompoundTableModel.cSummaryModeMinimum ? " min"
							  	  : model.getColumnSummaryMode(column) == CompoundTableModel.cSummaryModeMaximum ? " max"
								  : model.getColumnSummaryMode(column) == CompoundTableModel.cSummaryModeSum ? " sum" : " ???");
						html.append("</b> value is shown.");
						}
					if (model.getColumnSignificantDigits(column) != 0)
						html.append("<br>Numerical values are rounded to "+model.getColumnSignificantDigits(column)+" significant digits.");
					String refColumn = model.getColumnProperty(column, CompoundTableConstants.cColumnPropertyReferencedColumn);
					if (refColumn != null) {
						String refColumnTitle = model.getColumnTitle(model.findColumn(refColumn));
						String monoOrBi = CompoundTableConstants.cColumnPropertyReferenceTypeRedundant.equals(
								model.getColumnProperty(column, CompoundTableConstants.cColumnPropertyReferenceType)) ? "Bi" : "Mono";
						html.append("<br>"+monoOrBi+"-directionally references '" + refColumnTitle + "'.");
						}

					html.append("</html>");
					return html.toString();
					}
				return null;
				}
			};
		}

	private void buildColumnGroupToColorMap() {
		if (mColumnGroupToColorMap == null)
			mColumnGroupToColorMap = new TreeMap<>();
		else
			mColumnGroupToColorMap.clear();

		CompoundTableModel tableModel = (CompoundTableModel)getModel();
		for (int column=0; column<tableModel.getTotalColumnCount(); column++) {
			String groups = tableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyDisplayGroup);
			if (groups != null)
				for (String group:groups.split(";"))
					mColumnGroupToColorMap.put(group, Color.GRAY); // place holder
		}
		int colorCount = mColumnGroupToColorMap.keySet().size();
		if (colorCount != 0) {
			Color[] color = VisualizationColor.createDiverseColorList(colorCount);
			String[] groups = mColumnGroupToColorMap.keySet().toArray(new String[0]);
			for (int i=0; i<groups.length; i++)
				mColumnGroupToColorMap.put(groups[i], color[i]);
			}
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cNewTable) {
			mHiddenColumnMap.clear();
			}
		else if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			// remove from mHiddenColumnMap all columns that cannot be found anymore
			Iterator<String> iterator = mHiddenColumnMap.keySet().iterator();
			while (iterator.hasNext())
				if (((CompoundTableModel)getModel()).findColumn(iterator.next()) == -1)
					iterator.remove();

			// also remove all visible columns from columnModel, which are not in the tableModel anymore
			for (int i=getColumnModel().getColumnCount()-1; i>=0; i--) {
				TableColumn column = getColumnModel().getColumn(i);
				if (((CompoundTableModel) getModel()).findColumn((String)column.getHeaderValue()) == -1) {
					getColumnModel().removeColumn(column);
					}
				}
			}
		else if (e.getType() == CompoundTableEvent.cChangeActiveRow) {
			mCurrentRecord = ((CompoundTableModel)getModel()).getActiveRow();
			repaint();
			}
		else if (e.getType() == CompoundTableEvent.cChangeColumnDetailSource) {
			repaint();
			}
		}

	public void tableChanged(TableModelEvent e) {
		// the first call to this is by the JTable constructor, i.e. before the DETable initialization!!!

		ArrayList<DEColumnProperty> columnPropertyList = null;
		int[] rowSelection = null;

		// insertion, deletion or renaming of a column
		if (e.getFirstRow() == TableModelEvent.HEADER_ROW && e.getColumn() != TableModelEvent.ALL_COLUMNS) {
			columnPropertyList = getColumnPropertyList(e.getType() != TableModelEvent.UPDATE);
			rowSelection = getRowSelection();
			}

		super.tableChanged(e);

		// re-apply column visibility
		if (mHiddenColumnMap != null
		 && e.getFirstRow() == TableModelEvent.HEADER_ROW && e.getColumn() != TableModelEvent.ALL_COLUMNS)
			for (int column = 0; column < ((CompoundTableModel) getModel()).getTotalColumnCount(); column++)
				if (mHiddenColumnMap.keySet().contains(((CompoundTableModel) getModel()).getColumnTitleNoAlias(column)))
					setColumnVisibility(column, false);

		if (e.getFirstRow() == TableModelEvent.HEADER_ROW)
			updateTableRenderers(true);

		if (columnPropertyList != null)
			applyColumnProperties(columnPropertyList);

		if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
			if (e.getColumn() == TableModelEvent.ALL_COLUMNS) {
				getColumnModel().getSelectionModel().setSelectionInterval(0, getColumnModel().getColumnCount() - 1);
				}
			else {
				restoreRowSelection(rowSelection);
				((CompoundTableModel)getModel()).invalidateSelectionModel();
				}
			}
		}

	/**
	 * @param column total column index of visible or hidden column
	 * @return column width in pixel independent of UI scaling
	 */
	public int getColumnWidth(int column) {
		int viewColumn = convertTotalColumnIndexToView(column);
		if (viewColumn != -1)
			return (int)(getColumnModel().getColumn(viewColumn).getPreferredWidth()/HiDPIHelper.getUIScaleFactor());

		DEHiddenColumn hiddenColumnSpec = mHiddenColumnMap.get(((CompoundTableModel)getModel()).getColumnTitleNoAlias(column));
		return (hiddenColumnSpec == null) ? 80 : (int)(hiddenColumnSpec.columnWidth / HiDPIHelper.getUIScaleFactor());
		}

	/**
	 * @param column total column index of visible or hidden column
	 * @return true, if column has a MultiLineCellRenderer set to wrap text
	 */
	public boolean isTextWrapped(int column) {
		TableCellRenderer renderer = null;
		int viewColumn = convertTotalColumnIndexToView(column);
		if (viewColumn != -1) {
			renderer = getColumnModel().getColumn(viewColumn).getCellRenderer();
			}
		else {
			DEHiddenColumn hiddenColumnSpec = mHiddenColumnMap.get(((CompoundTableModel)getModel()).getColumnTitleNoAlias(column));
			if (hiddenColumnSpec != null)
				renderer = hiddenColumnSpec.cellRenderer;
			}

		return (renderer != null && renderer instanceof MultiLineCellRenderer) ? ((MultiLineCellRenderer)renderer).getLineWrap() : false;
		}

	/**
	 * Returns true, if the column is visible despite the visibility filter would hide it;
	 * return false, if the column was explicitly declared hidden; returns null otherwise.
	 * @return null or "true" or "false"
	 */
	public String getExplicitColumnVisibilityString(int column) {
		int displayableColumn = ((CompoundTableModel)getModel()).convertToDisplayableColumnIndex(column);
		if (displayableColumn == -1)
			return null;

		int viewColumn = convertColumnIndexToView(displayableColumn);

		if (viewColumn != -1) {	// is visible
			if (mTextFieldSearch.getText().length() != 0
			 && !((CompoundTableModel)getModel()).getColumnTitle(column).contains(mTextFieldSearch.getText())) {
				return "true";
				}
			}
		else {
			String columnName = ((CompoundTableModel)getModel()).getColumnTitleNoAlias(column);
			DEHiddenColumn hiddenColumnSpec = mHiddenColumnMap.get(columnName);
			if (hiddenColumnSpec.isManuallyHidden)
				return "false";
			}

		return null;
		}

	public void setColumnVisibility(int column, boolean b) {
		setColumnVisibility(column, b, false);
		}

	private void setColumnVisibility(int column, boolean b, boolean isFilterAction) {
		int displayableColumn = ((CompoundTableModel)getModel()).convertToDisplayableColumnIndex(column);
		if (displayableColumn == -1)
			return;

		int viewColumn = convertColumnIndexToView(displayableColumn);
		boolean isVisible = (viewColumn != -1);
		String columnName = ((CompoundTableModel)getModel()).getColumnTitleNoAlias(column);

		if (isVisible) {
			if (b)
				return;

			TableColumn tc = getColumnModel().getColumn(viewColumn);
			mHiddenColumnMap.put(columnName, new DEHiddenColumn(tc.getCellRenderer(), tc.getPreferredWidth(),
					viewColumn, isColumnSelected(viewColumn), !isFilterAction, isFilterAction));
			removeColumn(tc);
			return;
			}

		DEHiddenColumn hiddenColumnSpec = mHiddenColumnMap.get(columnName);
		if (isFilterAction) {
			hiddenColumnSpec.isHiddenByFilter = !b;
			}
		else {
			if (b)	// if filtered out but manually selected to show, then override filter
				hiddenColumnSpec.isHiddenByFilter = false;

			hiddenColumnSpec.isManuallyHidden = !b;
			}

		if (!hiddenColumnSpec.isHiddenByFilter && !hiddenColumnSpec.isManuallyHidden) {
			int targetColumn = mIntendedColumnOrder.getColumnIndexForInsertion(column);

			addColumn(new TableColumn(displayableColumn));
			viewColumn = convertColumnIndexToView(displayableColumn);
			if (viewColumn != targetColumn) {
				mIntendedColumnOrder.setNeglectMoveColumnEvents(true);
				moveColumn(viewColumn, targetColumn);
				mIntendedColumnOrder.setNeglectMoveColumnEvents(false);
				viewColumn = targetColumn;
				}

			getColumnModel().getColumn(viewColumn).setPreferredWidth(hiddenColumnSpec.columnWidth);
			getColumnModel().getColumn(viewColumn).setCellRenderer(hiddenColumnSpec.cellRenderer);
			getColumnModel().getSelectionModel().addSelectionInterval(viewColumn, viewColumn);
			mHiddenColumnMap.remove(columnName);
			}
		}

/*	private void printHiddenStatus(String pos, boolean b, int column) {
		System.out.print(pos+": visCount:"+getColumnModel().getColumnCount()+" totalCount:"+((CompoundTableModel)getModel()).getTotalColumnCount());
		System.out.print((b?", show '":", hide '")+((CompoundTableModel)getModel()).getColumnTitle(column)+"'");
		System.out.print(", hidden:"); for (String key:mHiddenColumnMap.keySet()) System.out.print(" "+key); System.out.println();
		}*/

	private void filterColumnVisibility(String s) {
		for (int column=0; column<((CompoundTableModel)getModel()).getTotalColumnCount(); column++) {
			int displayableColumn = ((CompoundTableModel)getModel()).convertToDisplayableColumnIndex(column);
			if (displayableColumn != -1) {
				boolean isVisible = true;
				if (s != null && s.length() != 0) {
					String title = ((CompoundTableModel)getModel()).getColumnTitle(column);
					if (s.startsWith("regex:"))
						try { isVisible = title.matches(s.substring(6)); } catch (PatternSyntaxException pse) {}
					else if (s.indexOf(',') == -1)
						isVisible = title.contains(s);
					else {
						String[] ss = s.split(",");
						isVisible = false;
						for (String sss:ss) {
							if (title.contains(sss)) {
								isVisible = true;
								break;
								}
							}
						}
					}
				setColumnVisibility(column, isVisible);
				}
			}
		}

	public void setColumnFilterText(String s) {
		mTextFieldSearch.setText(s);
		filterColumnVisibility(s);
		}

	public String getColumnFilterText() {
		return mTextFieldSearch.getText();
		}

	public ArrayList<DEColumnProperty> getColumnPropertyList(boolean columnIndicesDirty) {
		CompoundTableModel tableModel = (CompoundTableModel)getModel();
		ArrayList<DEColumnProperty> columnPropertyList = new ArrayList<DEColumnProperty>();
		if (columnIndicesDirty) {
			// If INSERT or DELETE, visible column titles are kept,
			// but column order and count may change.
			for (int column=0; column<getColumnCount(); column++) {
				String visName = (String)getColumnModel().getColumn(column).getHeaderValue();
				String name = tableModel.getColumnTitleNoAlias(tableModel.findColumn(visName));
				if (name != null)
					columnPropertyList.add(new DEColumnProperty(name, getColumnModel().getColumn(column),
							getColumnModel().getSelectionModel().isSelectedIndex(column)));
				}
			}
		else {
			// If e.g. UPDATE on header, visible column title may change,
			// but column order and count is kept.
			for (int viewColumn=0; viewColumn<getColumnCount(); viewColumn++) {
				int column = convertTotalColumnIndexFromView(viewColumn);
				String name = tableModel.getColumnTitleNoAlias(column);
				columnPropertyList.add(new DEColumnProperty(name, getColumnModel().getColumn(viewColumn),
						getColumnModel().getSelectionModel().isSelectedIndex(column)));
				}
			}
		return columnPropertyList;
		}

	/**
	 * Using a DEColumnOrderHelper, this class tracks the desired order of displayable columns.
	 * If a hidden column is shown, then it should appear, where is was before hiding it.
	 * If visible columns were repositioned in-between, then the DEColumnOrderHelper does its
	 * best to find a suitable position.
	 * This method returnes a TAB-delimited list of all displayable columns in the desired
	 * display order. If this order exactly matches the order of the native column order in
	 * the underlying CompoundTableModel, the null is returned.
	 * @return TAB delimited String of visible column names or null if in native order
	 */
	public String getColumnOrderString() {
		if (mIntendedColumnOrder.isNative())
			return null;

		CompoundTableModel tableModel = (CompoundTableModel)getModel();
		StringBuffer buf = new StringBuffer();
		for (int column: mIntendedColumnOrder) {
			if (buf.length() != 0)
				buf.append('\t');
			buf.append(tableModel.getColumnTitleNoAlias(column));
			}

		return buf.toString();

/*		int previousColumn = -1;
		boolean inNativeOrder = true;
		for (int viewColumn=0; viewColumn<getColumnCount(); viewColumn++) {
			int column = convertTotalColumnIndexFromView(viewColumn);
			if (column < previousColumn) {
				inNativeOrder = false;
				break;
				}
			previousColumn = column;
			}
		if (inNativeOrder)
			return null;

		CompoundTableModel tableModel = (CompoundTableModel)getModel();
		StringBuffer buf = new StringBuffer();
		for (int viewColumn=0; viewColumn<getColumnCount(); viewColumn++) {
			int column = convertTotalColumnIndexFromView(viewColumn);
			if (buf.length() != 0)
				buf.append('\t');
			buf.append(tableModel.getColumnTitleNoAlias(column));
			}
		return buf.toString();  */
		}

	public DEColumnOrder getIntendedColumnOrder() {
		return mIntendedColumnOrder;
		}

	/**
	 * Rearranges visible columns to match given column order.
	 * @param columnOrder TAB delimited String of column names
	 */
	public void setColumnOrderString(String columnOrder) {
		setColumnOrder(columnOrder.split("\\t"));
		}

	private void setColumnOrder(String[] columnTitle) {
		mIntendedColumnOrder.setNeglectMoveColumnEvents(true);

		CompoundTableModel tableModel = (CompoundTableModel)getModel();
		int dispIndex = 0;
		int visIndex = 0;
		for (String title:columnTitle) {
			int column = tableModel.findColumn(title);
			if (column != -1) {
				int displayableColumn = tableModel.convertToDisplayableColumnIndex(column);
				if (displayableColumn != -1) {
					Integer object = new Integer(column);
					mIntendedColumnOrder.remove(object);
					mIntendedColumnOrder.add(dispIndex++, object);

					int viewColumn = convertColumnIndexToView(displayableColumn);
					if (viewColumn != -1)
						getColumnModel().moveColumn(viewColumn, visIndex++);
					}
				}
			}

		mIntendedColumnOrder.setNeglectMoveColumnEvents(false);
		}

	/**
	 * Restores column order and column widths from a columnPropertyList.
	 * @param columnPropertyList
	 */
	private void applyColumnProperties(ArrayList<DEColumnProperty> columnPropertyList) {
		for (DEColumnProperty columnProperty:columnPropertyList)
			columnProperty.apply(this);

		String[] columnTitle = new String[columnPropertyList.size()];
		for (int i=0; i<columnPropertyList.size(); i++)
			columnTitle[i] = columnPropertyList.get(i).getColumnName();
		setColumnOrder(columnTitle);
		}

	/**
	 * Converts from CompoundTableModel's total column index to JTable's view column index.
	 * If the column is not displayable or if it is set to hidden in the view -1 is returned.
	 * @param column total column index
	 * @return view column index or -1, if view doesn't display that column
	 */
	public int convertTotalColumnIndexToView(int column) {
		int modelColumn = ((CompoundTableModel)getModel()).convertToDisplayableColumnIndex(column);
		return (modelColumn == -1) ? -1 : convertColumnIndexToView(modelColumn);
		}

	/**
	 * Converts from JTable's view column index to CompoundTableModel's total column index.
	 * @param viewColumn view column index
	 * @return total column index
	 */
	public int convertTotalColumnIndexFromView(int viewColumn) {
		return (viewColumn == -1) ? -1
			 : ((CompoundTableModel)getModel()).convertFromDisplayableColumnIndex(convertColumnIndexToModel(viewColumn));
		}

	public void rowNumberClicked(int row) {
		mCurrentRecord = ((CompoundTableModel)getModel()).getRecord(row);
		((CompoundTableModel)getModel()).setActiveRow(mCurrentRecord);
		repaint();
		}

	private int[] getRowSelection() {
		CompoundTableModel tableModel = (CompoundTableModel)getModel();
		int rowCount = tableModel.getTotalRowCount();
		int[] selection = new int[(rowCount+31)/32];
		for (int row=0; row<rowCount; row++)
			if (tableModel.getTotalRecord(row).isSelected())
				selection[row >> 5] |= (1 << (row % 32));
		return selection;
		}

	private void restoreRowSelection(int[] selection) {
		CompoundTableModel tableModel = (CompoundTableModel)getModel();
		int rowCount = tableModel.getTotalRowCount();
		for (int row=0; row<rowCount; row++)
			if ((selection[row >> 5] & (1 << (row % 32))) != 0)
				tableModel.getTotalRecord(row).setSelection(true);
		}
	}

class DETableDialog extends JDialog implements ActionListener {
	private JTextArea mTextArea;
	private String mOldValue,mNewValue;

	public DETableDialog(Frame owner, String title, String oldValue) {
		super(owner, title, true);

		mOldValue = oldValue;
		mTextArea = new JTextArea(oldValue);

		// Change font to allow displaying rare unicode characters
		mTextArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, mTextArea.getFont().getSize()));
		mTextArea.transferFocus();
		JScrollPane scrollPane = new JScrollPane(mTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setPreferredSize(new Dimension(HiDPIHelper.scale(320), HiDPIHelper.scale(100)));

//		mTextArea.getInputMap(JTextArea.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0),"dialogOK");
//		mTextArea.getActionMap().put("dialogOK", new AbstractAction() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				done(true);
//				}
//			});
//		mTextArea.getInputMap(JTextArea.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK),"fakeEnter");
//		mTextArea.getActionMap().put("fakeEnter", new AbstractAction() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				int pos = mTextArea.getCaretPosition();
//				mTextArea.insert("\n", pos);
//				mTextArea.setCaretPosition(pos + 1);
//				}
//			});
		mTextArea.getInputMap(JTextArea.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),"dialogCancel");
		mTextArea.getActionMap().put("dialogCancel", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				done(false);
			}
		});

		JLabel message = new JLabel("Press ESC (Cancel) or Ctrl-ENTER (OK) to close dialog!", JLabel.RIGHT);
		message.setFont(message.getFont().deriveFont(0,HiDPIHelper.scale(11)));

		JButton bcancel = new JButton("Cancel");
		bcancel.addActionListener(this);
		JButton bok = new JButton("OK");
		bok.addActionListener(this);
		getRootPane().setDefaultButton(bok);

		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap },
							{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };
		getContentPane().setLayout(new TableLayout(size));
		getContentPane().add(scrollPane, "1,1,5,1");
		getContentPane().add(message, "1,3,5,3");
		getContentPane().add(bcancel, "3,5");
		getContentPane().add(bok, "5,5");

		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
		}

	public String getNewValue() {
		return mNewValue;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		done("OK".equals(e.getActionCommand()));
		}

	private void done(boolean isOK) {
		if (isOK && !mTextArea.getText().equals(mOldValue))
			mNewValue = mTextArea.getText();

		setVisible(false);
		}
	}

class DEHiddenColumn {
	boolean isManuallyHidden,isHiddenByFilter;
	TableCellRenderer cellRenderer;
	int columnWidth, viewIndex;
	boolean isSelected;

	public DEHiddenColumn(TableCellRenderer cellRenderer, int columnWidth, int viewIndex, boolean isSelected, boolean isManuallyHidden, boolean isHiddenByFilter) {
		this.cellRenderer = cellRenderer;
		this.columnWidth = columnWidth;
		this.viewIndex = viewIndex;
		this.isSelected = isSelected;
		this.isManuallyHidden = isManuallyHidden;
		this.isHiddenByFilter = isHiddenByFilter;
		}
	}

class DEColumnProperty {
	private String columnName;
	TableCellRenderer cellRenderer;
	int width;
	boolean isSelected;

	public DEColumnProperty(String columnName, TableColumn column, boolean isSelected) {
		this.columnName = columnName;
		cellRenderer = column.getCellRenderer();
		width = column.getPreferredWidth();
		this.isSelected = isSelected;
		}

	public String getColumnName() {
		return columnName;
		}

	public void apply(DETable table) {
		int column = ((CompoundTableModel)table.getModel()).findColumn(columnName);
		int viewColumn = table.convertTotalColumnIndexToView(column);
		if (viewColumn != -1) {
			TableColumn tableColumn = table.getColumnModel().getColumn(viewColumn);
			tableColumn.setCellRenderer(cellRenderer);
			tableColumn.setPreferredWidth(width);
			table.getColumnModel().getSelectionModel().addSelectionInterval(viewColumn, viewColumn);
			}
		}
	}
