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

import com.actelion.research.datawarrior.task.data.DETaskSortRows;
import com.actelion.research.datawarrior.task.table.DETaskShowTableColumns;
import com.actelion.research.gui.ScrollPaneAutoScrollerWhenDragging;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.*;
import com.actelion.research.table.model.CompoundListSelectionModel;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableListEvent;
import com.actelion.research.table.model.CompoundTableListListener;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.ViewSelectionHelper;
import com.actelion.research.table.view.VisualizationColor;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

public class DETableView extends JScrollPane
		implements CompoundTableView,CompoundTableListListener,MouseListener,MouseMotionListener,Printable,CompoundTableColorHandler.ColorListener {
	private static final long serialVersionUID = 0x20060904;

	private static final int DEFAULT_HEADER_LINES = 2;

	private Frame				    mParentFrame;
	private DEParentPane		    mParentPane;
	private DETable				    mTable;
	private DECompoundTableModel	mTableModel;
	private DetailPopupProvider	    mDetailPopupProvider;
	private int					    mMouseX,mHeaderLineCount;
	private CompoundTableColorHandler	mColorHandler;
	private ViewSelectionHelper 	mViewSelectionHelper;

	public DETableView(Frame parentFrame, DEParentPane parentPane, DECompoundTableModel tableModel,
					   CompoundTableColorHandler colorHandler, CompoundListSelectionModel selectionModel) {
		mParentFrame = parentFrame;
		mParentPane = parentPane;
		mTableModel = tableModel;
		mColorHandler = colorHandler;

		mHeaderLineCount = DEFAULT_HEADER_LINES;

		mTable = new DETable(parentFrame, mParentPane.getMainPane(), selectionModel);
		mTable.getTableHeader().addMouseListener(this);
		mTable.addMouseListener(this);
		mTable.addMouseMotionListener(this);
		mTable.setColumnSelectionAllowed(true);
		mTable.getTableHeader().setDefaultRenderer(
				new WrappingHeaderCellRenderer(this, mTable.getTableHeader().getDefaultRenderer()));

		mColorHandler.addColorListener(this);

		setBorder(BorderFactory.createEmptyBorder());
		getViewport().add(mTable, null);

		setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);

		ScrollPaneAutoScrollerWhenDragging scroller = new ScrollPaneAutoScrollerWhenDragging(this, false);
		mTable.getTableHeader().addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				scroller.autoScroll();
				}
			});
		mTable.getTableHeader().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				scroller.stopScrolling();
				}
			@Override
			public void mouseExited(MouseEvent e) {
				scroller.stopScrolling();
				}
			});

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger())
					showBackgroundPopup(e);
				}
			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger())
					showBackgroundPopup(e);
				}
			});

		getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, Event.CTRL_MASK),"activeUp");
		getActionMap().put("activeUp", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int index = mTableModel.getActiveRowIndex();
				if (index != -1 && index > 0) {
					mTableModel.setActiveRow(index-1);
					mTable.scrollToRow(index-1);
					}
				}
			});

		getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Event.CTRL_MASK),"activeDown");
		getActionMap().put("activeDown", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int index = mTableModel.getActiveRowIndex();
				if (index != -1 && index < mTableModel.getRowCount()-1) {
					mTableModel.setActiveRow(index+1);
					mTable.scrollToRow(index+1);
					}
				}
			});

		setColumnHeader(new JViewport() {
			@Override public Dimension getPreferredSize() {
				Dimension d = super.getPreferredSize();
				d.height = Math.round((0.5f + 1.2f * mHeaderLineCount) * getFont().getSize());
				return d;
				}
			});
		}

	public int getHeaderLineCount() {
		return mHeaderLineCount;
		}

	/**
	 * Sets the logical number of rows that can be shown in the header, if html and br tags are used.
	 * This does update the header height considering headert row count and font size.
	 * @param headerLineCount
	 */
	public void setHeaderLineCount(int headerLineCount) {
		mHeaderLineCount = headerLineCount;
		mTable.getTableHeader().resizeAndRepaint();
		}

	public void cleanup() {
		mTable.cleanup();
		mColorHandler.removeColorListener(this);
		}

	private void showBackgroundPopup(MouseEvent e) {
		final int[] hiddenColumns = createHiddenColumnList();
		if (hiddenColumns.length != 0) {
			JPopupMenu popup = new JPopupMenu();
			JMenuItem menuItem = new JMenuItem("Show All Columns");
			menuItem.addActionListener(event ->
				new DETaskShowTableColumns(mParentFrame, this, hiddenColumns).defineAndRun()
				);
			popup.add(menuItem);
			popup.show(this, e.getX(), e.getY());
			}
		}

	public int[] createHiddenColumnList() {
		int count = 0;
		for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
			if (mTableModel.isColumnDisplayable(i) && !isColumnVisible(i))
				count++;

		int[] hiddenColumn = new int[count];
		count = 0;
		for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
			if (mTableModel.isColumnDisplayable(i) && !isColumnVisible(i))
				hiddenColumn[count++] = i;

		return hiddenColumn;
		}

	@Override
	public void setViewSelectionHelper(ViewSelectionHelper l) {
		mViewSelectionHelper = l;
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		}

	public void listChanged(CompoundTableListEvent e) {}

	public DEParentPane getParentPane() {
		return mParentPane;
		}

	public DECompoundTableModel getTableModel() {
		return mTableModel;
		}

	public int[] getSelectedColumns() {
		int[] selectedColumn = null;
		if (mTableModel.hasSelectedRows()) {
			int selectedCount = 0;
			for (int i = 0; i<mTable.getColumnCount(); i++)
				if (mTable.isColumnSelected(i))
					selectedCount++;
			selectedColumn = new int[selectedCount];
			selectedCount = 0;
			for (int i = 0; i<mTable.getColumnCount(); i++)
				if (mTable.isColumnSelected(i))
					selectedColumn[selectedCount++] = mTable.convertTotalColumnIndexFromView(i);
		}
		return selectedColumn;
	}

	@Override
	public boolean copyViewContent() {
		boolean skipHeader = mTable.getSelectedColumns().length <= 1;
//			|| ((CompoundListSelectionModel)mTable.getSelectionModel()).getSelectionCount() <= 1;
		new CompoundTableSaver(mParentFrame, mTableModel, mTable).copy(skipHeader);
		return true;
		}

	public boolean getTextWrapping(int column) {
		int modelColumn = mTable.convertTotalColumnIndexToView(column);
		if (modelColumn == -1)
			return false;
		TableCellRenderer renderer = mTable.getColumnModel().getColumn(modelColumn).getCellRenderer();
		return renderer instanceof MultiLineCellRenderer
			&& ((MultiLineCellRenderer)renderer).getLineWrap();
		}

	public void setTextWrapping(int column, boolean wrap) {
		int viewColumn = mTable.convertTotalColumnIndexToView(column);
		if (viewColumn != -1) {
			TableCellRenderer renderer = mTable.getColumnModel().getColumn(viewColumn).getCellRenderer();
			if (renderer instanceof MultiLineCellRenderer) {
				((MultiLineCellRenderer)renderer).setLineWrap(wrap);
				mTable.repaint();
				}
			}
		}

	public boolean isColumnVisible(int column) {
		return (mTable.convertTotalColumnIndexToView(column) != -1);
		}

	public void setColumnVisibility(int column, boolean b) {
		mTable.setColumnVisibility(column, b);
		}

	public void colorChanged(int column, int type, VisualizationColor color) {
		int viewColumn = mTable.convertTotalColumnIndexToView(column);
		if (viewColumn != -1) {
			TableCellRenderer renderer = mTable.getColumnModel().getColumn(viewColumn).getCellRenderer();
			if (renderer instanceof ColorizedCellRenderer) {
				((ColorizedCellRenderer)renderer).setColorHandler(color, type);
				mTable.repaint();
				}
			}
		}

	public void mouseClicked(MouseEvent e) {
		if (e.getSource() == mTable.getTableHeader()
		 && e.getX() == mMouseX
		 && (e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
			int column = mTable.getTableHeader().columnAtPoint(e.getPoint());
			boolean isShiftDown = ((e.getModifiers() & MouseEvent.SHIFT_MASK) != 0);
			if (column != -1)
				new DETaskSortRows(mParentFrame, mTableModel, mTable.convertTotalColumnIndexFromView(column), isShiftDown).defineAndRun();
			return;
			}

		if (e.getClickCount() == 2
		 && e.getSource() == mTable) {
			int column = mTable.convertTotalColumnIndexFromView(mTable.columnAtPoint(e.getPoint()));
			if (column != -1) {
				int row = mTable.rowAtPoint(e.getPoint());
				new DEDetailPopupMenu(mParentPane.getMainPane(), mTableModel.getRecord(row), null, this, null, column, e.isControlDown()).actionPerformed(
						new ActionEvent(this, ActionEvent.ACTION_PERFORMED, DEDetailPopupMenu.EDIT_VALUE+mTableModel.getColumnTitleNoAlias(column)));
				}
			}
		}

	public void mousePressed(MouseEvent e) {
		mViewSelectionHelper.setSelectedView(this);
		if (handlePopupTrigger(e))
			return;

		mMouseX = e.getX();
		}

	public void mouseReleased(MouseEvent e) {
		if (handlePopupTrigger(e))
			return;
		}

	public void mouseEntered(MouseEvent e) {
		}

	public void mouseExited(MouseEvent e) {
		}

	private boolean handlePopupTrigger(MouseEvent e) {
		if (e.isPopupTrigger()) {
			// This is certainly not dragging. Setting dragged column to null, because SubstanceLaF 7.3 crashes otherwise
			// when deleting last column
			getTable().getTableHeader().setDraggedColumn(null);

			if (e.getSource() == mTable.getTableHeader()) {
				int column = mTable.convertTotalColumnIndexFromView(mTable.getTableHeader().columnAtPoint(e.getPoint()));
				if (column != -1) {
					JPopupMenu popup = new DETablePopupMenu(mParentFrame, mParentPane, this, column);
					if (popup.getComponentCount() != 0)
						popup.show(mTable.getTableHeader(), e.getX(), e.getY());
					}
				}
			else if (e.getSource() == mTable) {
				int theRow = mTable.rowAtPoint(e.getPoint());
				if (theRow != -1) {
					int theColumn = mTable.convertTotalColumnIndexFromView(mTable.columnAtPoint(e.getPoint()));
					JPopupMenu popup = mDetailPopupProvider.createPopupMenu(mTableModel.getRecord(theRow), this, theColumn, e.isControlDown());
					if (popup != null)
						popup.show(mTable, e.getX(), e.getY());
					}
				}
			return true;
			}
		return false;
		}

	public void mouseDragged(MouseEvent e) {}

	public void mouseMoved(MouseEvent e) {
		if (e.getSource() == mTable)
			mTableModel.setHighlightedRow(mTable.rowAtPoint(e.getPoint()));
			}

	public void setDetailPopupProvider(DetailPopupProvider detailPopupProvider) {
		mDetailPopupProvider = detailPopupProvider;
		}

	public DETable getTable() {
		return mTable;
		}

	public CompoundTableColorHandler getColorHandler() {
		return mColorHandler;
		}

	public int print(Graphics g, PageFormat pageFormat, int pageIndex) throws PrinterException {
		Graphics2D g2 = (Graphics2D)g;
	 	g2.setColor(Color.black);
	 	int fontHeight = g2.getFontMetrics().getHeight();
	 	int fontDesent = g2.getFontMetrics().getDescent();

	 	//leave room for page number
	 	double pageHeight = pageFormat.getImageableHeight() - fontHeight;
	 	double pageWidth = pageFormat.getImageableWidth();
	 	double tableWidth = (double)mTable.getColumnModel().getTotalColumnWidth();
	 	double scale = 1;
	 	if (tableWidth >= pageWidth)
			scale = pageWidth / tableWidth;

	 	double headerHeightOnPage= mTable.getTableHeader().getHeight()*scale;
	 	double tableWidthOnPage = tableWidth*scale;

	 	double oneRowHeight = (mTable.getRowHeight() /* + mTable.getRowMargin()*/)*scale;
	 	int numRowsOnAPage = (int)((pageHeight-headerHeightOnPage) / oneRowHeight);
	 	double pageHeightForTable = oneRowHeight * numRowsOnAPage;
	 	int totalNumPages = (int)Math.ceil(((double)mTable.getRowCount()) / numRowsOnAPage);

	 	if(pageIndex >= totalNumPages)
			return NO_SUCH_PAGE;

	 	g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
	 	g2.drawString("Page: "+(pageIndex+1),
					  (int)pageWidth / 2-35,
					  (int)(pageHeight + fontHeight - fontDesent));

	 	g2.translate(0f,headerHeightOnPage);
	 	g2.translate(0f,-pageIndex*pageHeightForTable);

	 	//If this piece of the table is smaller
	 	//than the size available,
	 	//clip to the appropriate bounds.
	 	if (pageIndex + 1 == totalNumPages) {
			int lastRowPrinted = numRowsOnAPage * pageIndex;
			int numRowsLeft = mTable.getRowCount() - lastRowPrinted;
			g2.setClip(0, (int)(pageHeightForTable * pageIndex),
						  (int)Math.ceil(tableWidthOnPage),
						  (int)Math.ceil(oneRowHeight * numRowsLeft));
			}
	 	//else clip to the entire area available.
	 	else {
			g2.setClip(0, (int)(pageHeightForTable*pageIndex),
						  (int) Math.ceil(tableWidthOnPage),
						  (int) Math.ceil(pageHeightForTable));
			}

		g2.scale(scale,scale);
		mTable.paint(g2);
		g2.scale(1/scale,1/scale);
		g2.translate(0f,pageIndex*pageHeightForTable);
		g2.translate(0f, -headerHeightOnPage);
		g2.setClip(0, 0,
				   (int) Math.ceil(tableWidthOnPage),
				   (int)Math.ceil(headerHeightOnPage));
	 	g2.scale(scale, scale);
		mTable.getTableHeader().paint(g2);

	 	return Printable.PAGE_EXISTS;
		}
	}

class WrappingHeaderCellRenderer implements TableCellRenderer {
	private DETableView mTableView;
	private TableCellRenderer mOriginalCellRenderer;

	public WrappingHeaderCellRenderer(DETableView tableView, TableCellRenderer cellRenderer) {
		super();
		mTableView = tableView;
		mOriginalCellRenderer = cellRenderer;
		}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int modelColumn) {
		String title = (String)value;
		TableColumnModel columnModel = table.getTableHeader().getColumnModel();
		int columnWidth = columnModel.getColumn(modelColumn).getWidth() - HiDPIHelper.scale(5);
		int lines = mTableView.getHeaderLineCount();
		if (lines > 1) {
			FontMetrics metrics = table.getFontMetrics(table.getFont());
			int textWidth = metrics.stringWidth(title);
			if (textWidth > columnWidth) {
				// We need to wrap, which is done by converting into HTML and introducing <br> tags
				// As long as the remaining text to distribute is less wide than columnWidth*remainingLines we try to wrap
				// at the last SPACE in the current line. Once no surplus width is left, we do hard wrapping with cutting words.
				int surplus = lines * columnWidth - textWidth;
				int[] brk = new int[lines-1];
				int start = 0;
				for (int line=0; line<lines-1; line++) {
					if (start < title.length()) {
						brk[line] = start+1;
						while (brk[line] < title.length() && (title.charAt(brk[line])==' ' || metrics.stringWidth(title.substring(start, brk[line]+1))<=columnWidth))
							brk[line]++;
						if (surplus > 0 && title.charAt(brk[line]-1)!=' ' && brk[line]>start+2) {
							int spc = brk[line]-2;
							while (spc>start+1 && title.charAt(spc)!=' ')
								spc--;
							if (title.charAt(spc)==' ') {
								int width = metrics.stringWidth(title.substring(start, spc+1));
								if (surplus >= columnWidth - width)
									brk[line] = spc+1;
								}
							}
						surplus -= columnWidth - metrics.stringWidth(title.substring(start, brk[line]));
						start = brk[line];
						}
					else {
						brk[line] = start;
						}
					}
				title = makeMultiLineTitle(title, brk, columnWidth, metrics);
				}
			}

		return mOriginalCellRenderer.getTableCellRendererComponent(table, title, isSelected, hasFocus, row, modelColumn);
		}

	private String makeMultiLineTitle(String title, int[] brk, int columnWidth, FontMetrics metrics) {
		int start = brk[brk.length-1];
		int dots = -1;
		if (start<title.length() && metrics.stringWidth(title.substring(start)) > columnWidth) {
			int width = columnWidth - metrics.stringWidth("...");
			dots = start+1;
			while (dots < title.length() && metrics.stringWidth(title.substring(start, dots+1))<=width)
				dots++;
			}
		StringBuilder sb = new StringBuilder("<html>");
		start = 0;
		for (int b:brk) {
			if (start < title.length()) {
				sb.append(title, start, b);
				sb.append("<br>");
				start = b;
				}
			}
		if (dots != -1) {
			sb.append(title, start, dots);
			sb.append("...");
			}
		else if (start < title.length()) {
			sb.append(title, start, title.length());
			}
		return sb.toString();
		}
	}
