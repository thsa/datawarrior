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

package com.actelion.research.table;

import com.actelion.research.gui.LookAndFeelHelper;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationColor;
import com.actelion.research.util.ColorHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.TreeMap;

import static com.actelion.research.chem.io.CompoundTableConstants.*;

public class MultiLineCellRenderer extends JTextArea implements ColorizedCellRenderer,TableCellRenderer {
    static final long serialVersionUID = 0x20070312;

    private boolean mAlternateBackground,mIsURL;
    private VisualizationColor mForegroundColor,mBackgroundColor;
    private int mMouseX,mMouseY,mMouseRow;
    private TreeMap<Integer,String> mClickableEntryMap;

    public MultiLineCellRenderer() {
		setLineWrap(true);
		setWrapStyleWord(true);
	    setOpaque(false);
	    mMouseX = 10000;
	    mMouseY = 10000;
	    mClickableEntryMap = new TreeMap<>();
		}

	public void setAlternateRowBackground(boolean b) {
		mAlternateBackground = b;
		}

	public void setColorHandler(VisualizationColor vc, int type) {
		switch (type) {
		case CompoundTableColorHandler.FOREGROUND:
	    	mForegroundColor = vc;
	    	break;
		case CompoundTableColorHandler.BACKGROUND:
	    	mBackgroundColor = vc;
	    	break;
			}
		}

	public String getClickableEntry(int row) {
    	return mClickableEntryMap.get(row);
		}

	public void setIsURL(boolean isURL) {
    	mIsURL = isURL;
		}

	public void setMouseLocation(int x, int y, int row) {
		mMouseX = x;
		mMouseY = y;
		mMouseRow = row;
		}

	@Override
	public void paintComponent(Graphics g) {
		// Substance Graphite LaF does not consider the defined background
		if (LookAndFeelHelper.isNewSubstance() || LookAndFeelHelper.isRadiance()) {
			Rectangle r = new Rectangle(new java.awt.Point(0,0), getSize());
			g.setColor(getBackground());
			((Graphics2D) g).fill(r);
			setOpaque(false);    // if the panel is opaque (e.g. after LaF change) new substance may crash
			super.paintComponent(g);
			}
		else {
			super.paintComponent(g);
			}
		}

    public Component getTableCellRendererComponent(JTable table, Object value,
							boolean isSelected, boolean hasFocus, int row, int column) {
	    if (LookAndFeelHelper.isAqua()
			    // Quaqua does not use the defined background color if CellRenderer is translucent
	     || (LookAndFeelHelper.isQuaQua()
		  && mBackgroundColor != null
		  && mBackgroundColor.getColorColumn() != JVisualization.cColumnUnassigned))
		    setOpaque(true);
		else
		    setOpaque(false);

		if (isSelected) {
            setForeground(UIManager.getColor("Table.selectionForeground"));
            setBackground(UIManager.getColor("Table.selectionBackground"));
			}
		else {
            if (mForegroundColor != null && mForegroundColor.getColorColumn() != JVisualization.cColumnUnassigned) {
            	CompoundRecord record = ((CompoundTableModel)table.getModel()).getRecord(row);
            	setForeground(mForegroundColor.getColorForForeground(record));
            	}
            else
            	setForeground(UIManager.getColor("Table.foreground"));

            if (mBackgroundColor != null && mBackgroundColor.getColorColumn() != JVisualization.cColumnUnassigned) {
            	CompoundRecord record = ((CompoundTableModel)table.getModel()).getRecord(row);
            	setBackground(mBackgroundColor.getColorForBackground(record));
            	}
            else {
            	if (!LookAndFeelHelper.isQuaQua()) {	// simulate the quaqua table style "striped"
		            Color bg = UIManager.getColor("Table.background");
		            setBackground(!mAlternateBackground || (row & 1) == 0 ? bg : ColorHelper.darker(bg, 0.94f));
            		}
            	}
			}

	    setFont(new Font(Font.SANS_SERIF, Font.PLAIN, table.getFont().getSize()));
		if (hasFocus) {
			setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
			if (table.isCellEditable(row, column)) {
				setForeground( UIManager.getColor("Table.focusCellForeground") );
				setBackground( UIManager.getColor("Table.focusCellBackground") );
				}
			}
		else {
			setBorder(new EmptyBorder(1, 2, 1, 2));
			}

		try {
			setText((value == null) ? "" : value.toString());
			}
		catch (Exception e) {
			setText("Unicode Error!!!");
			}    // some unicode chars create exceptions with setText() and then with paintComponent()

	    if (mIsURL) {
	    	Color color = LookAndFeelHelper.isDarkLookAndFeel() ? Color.CYAN : Color.BLUE;
		    setForeground(color);
		    try {
			    getHighlighter().addHighlight(0, ((String)value).length(), new UnderlinePainter(row, color));
			    }
		    catch (Exception e) {}
	        }

		return this;
		}

	/*
	 *  Implements a simple highlight painter that renders an underline
	 */
	class UnderlinePainter extends DefaultHighlighter.DefaultHighlightPainter {
		private ArrayList<int[]> mEntryIndexList;
		private int mEntryUnderMouse,mRow;

		public UnderlinePainter(int row, Color color) {
			super(color);
			mEntryUnderMouse = -1;
			mRow = row;
			}

		/**
		 * Paints a portion of a highlight.
		 *
		 * @param  g the graphics context
		 * @param  offs0 the starting model offset >= 0
		 * @param  offs1 the ending model offset >= offs1
		 * @param  bounds the bounding box of the view, which is not
		 *	       necessarily the region to paint.
		 * @param  c the editor
		 * @param  view View painting for
		 * @return region drawing occured in
		 */
		public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c, View view) {
			// The cell may contains multiple entries. Find start- and end-indexes of every entry
			// and locate that entry that is under the mouse pointer
			if (mEntryIndexList == null) {
				mClickableEntryMap.put(mRow, null);
				mEntryIndexList = new ArrayList<>();

				String value = c.getText();
				int indexAtMousePosition = (mMouseRow == mRow) ? viewToModel(new Point(mMouseX, mMouseY)) : -1;

				int ei1 = 0;
				while (ei1<value.length()) {
					int candidate1 = value.indexOf(cEntrySeparator, ei1);
					int candidate2 = value.indexOf(cLineSeparator, ei1);
					if (candidate1 == -1 && candidate2 == -1) {
						addEntry(ei1, value.length(), indexAtMousePosition, value);
						break;
						}
					int ei2 = (candidate1 == -1) ? candidate2
							: (candidate2 == -1) ? candidate1
							: Math.min(candidate1, candidate2);
					if (ei2 != ei1)
						addEntry(ei1, ei2, indexAtMousePosition, value);

					ei1 = ei2 + (value.charAt(ei2) == cLineSeparatorByte ? 1 : 2);
					}
				}

			// Depending on wrapping, offs0 and offs1 may overlap with entries and separators
			// We may need to split into multiple sections.
			if (mEntryUnderMouse != -1) {
				int[] entryIndex = mEntryIndexList.get(mEntryUnderMouse);
				if (offs0<entryIndex[1] && offs1>entryIndex[0])
					paintLayerSection(g, Math.max(offs0, entryIndex[0]), Math.min(offs1, entryIndex[1]), bounds, c, view);
				}

			return getDrawingArea(offs0, offs1, bounds, view);
			}

		public void paintLayerSection(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent c, View view) {
			Rectangle r = getDrawingArea(offs0, offs1, bounds, view);
			if (r != null) {
				g.setColor(getColor());

				int linewidth = Math.max(1, Math.round(HiDPIHelper.getUIScaleFactor() * c.getFont().getSize() / 12));
				g.fillRect(r.x, r.y+r.height-linewidth, r.width, linewidth);
				}
			}

		private void addEntry(int index1, int index2, int indexAtMousePosition, String value) {
			if (indexAtMousePosition >= index1 && indexAtMousePosition < index2) {
				mEntryUnderMouse = mEntryIndexList.size();
				mClickableEntryMap.put(mRow, value.substring(index1, index2));
				}

			int[] indexes = new int[2];
			indexes[0] = index1;
			indexes[1] = index2;
			mEntryIndexList.add(indexes);
			}

		private Rectangle getDrawingArea(int offs0, int offs1, Shape bounds, View view) {
			if (offs0 == view.getStartOffset() && offs1 == view.getEndOffset()) {
				return (bounds instanceof Rectangle) ? (Rectangle)bounds : bounds.getBounds();
				}
			else {
				try {
					Shape shape = view.modelToView(offs0, Position.Bias.Forward, offs1,Position.Bias.Backward, bounds);
					return (shape instanceof Rectangle) ? (Rectangle)shape : shape.getBounds();
					}
				catch (BadLocationException e) {}
				}
			return null;
			}
		}
	}
