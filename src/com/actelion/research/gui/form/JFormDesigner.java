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

package com.actelion.research.gui.form;

import com.actelion.research.gui.JScrollablePopupMenu;
import info.clearthought.layout.TableLayoutConstants;
import info.clearthought.layout.TableLayoutConstraints;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class JFormDesigner extends JComponent implements ActionListener,MouseListener, MouseMotionListener {
    private static final long serialVersionUID = 0x20061016;

    private static final int RULER_SIZE = 16;
	private static final int MARKER_SIZE = 6;
	private static final int MINIMUM_CELL_SIZE = 8;
	private static final int CLOSE_BOX_SIZE = 12;
	private static final int CLOSE_BOX_MARGIN = 8;

	private static final int SIZE_MODE_FIXED = 1;
	private static final int SIZE_MODE_RELATIVE = 2;
	private static final int SIZE_MODE_FILL = 3;

	private static final int MODE_NONE = 0;
	private static final int MODE_RESIZING_N = 1;
	private static final int MODE_RESIZING_E = 2;
	private static final int MODE_RESIZING_S = 4;
	private static final int MODE_RESIZING_W = 8;
	private static final int MODE_RESIZING_NW = MODE_RESIZING_N | MODE_RESIZING_W;
	private static final int MODE_RESIZING_SW = MODE_RESIZING_S | MODE_RESIZING_W;
	private static final int MODE_RESIZING_NE = MODE_RESIZING_N | MODE_RESIZING_E;
	private static final int MODE_RESIZING_SE = MODE_RESIZING_S | MODE_RESIZING_E;
	private static final int MODE_RESIZING = MODE_RESIZING_N | MODE_RESIZING_W | MODE_RESIZING_S | MODE_RESIZING_E;
	private static final int MODE_RELOCATING = 16;
	private static final int MODE_RESIZING_GRID = 32;
	private static final int MODE_SELECTING = 64;
	private static final int MODE_CLOSING = 128;

	private static final Color cThumbColor          = new Color(153, 153, 204);
	private static final Color cThumbShadowColor    = new Color(102, 102, 153);
	private static final Color cThumbHighlightColor = new Color(204, 204, 255);
	private static final Color cSelectedThumbColor  = new Color(110, 108, 174);
	private static final Color cSelectedBackgroundColor  = new Color(144, 180, 230);

	private JFormView	mFormView;
	private int[][]		mLayout;		// current column/row sizes in pixel
	private int[][]		mLayoutType;
	private int[][]		mLayoutSize;
	private boolean[][]	mIsSelected;
	private Dimension	mSize;
	private ArrayList<FormItem>	mItemList;
	private FormItem	mItemInFocus;
	private int			mDraggingMode,mRulerNo,mRulerIndex,mRulerCellIndex;
	private Point		mMouseLocation;	// grid location or exact location on drag start

	public JFormDesigner(JFormView formView) {
		mFormView = formView;
		addMouseListener(this);
		addMouseMotionListener(this);
		}

	public void getFormLayout() {
				// copy current layout from JFormView
		double[][] layoutDesc = mFormView.getFormLayout();

		mLayout = new int[2][];
		mLayout[0] = new int[layoutDesc[0].length+1];
		mLayout[1] = new int[layoutDesc[1].length+1];

		mLayoutType = new int[2][];
		mLayoutType[0] = new int[layoutDesc[0].length];
		mLayoutType[1] = new int[layoutDesc[1].length];

		mLayoutSize = new int[2][];
		mLayoutSize[0] = new int[layoutDesc[0].length];
		mLayoutSize[1] = new int[layoutDesc[1].length];

		mIsSelected = new boolean[2][];
		mIsSelected[0] = new boolean[layoutDesc[0].length];
		mIsSelected[1] = new boolean[layoutDesc[1].length];

		mItemList = new ArrayList<FormItem>();
		for (int i=0; i<mFormView.getFormObjectCount(); i++) {
			TableLayoutConstraints constraints = mFormView.getFormObjectConstraints(i);
			AbstractFormObject formObject = mFormView.getFormObject(i);
			mItemList.add(new FormItem(formObject.getKey(),
									   mFormView.getFormObjectTitle(i),
									   formObject.getType(),
									   constraints.col1,
									   constraints.col2,
									   constraints.row1,
									   constraints.row2));
			}

		mSize = null;
		}

	public void setFormLayout() {
		encodeLayout();

		for (int i=0; i<mItemList.size(); i++) {
			FormItem item = mItemList.get(i);
			String layout = (item.col1 == item.col2 && item.row1 == item.row2) ?
							""+item.col1+", "+item.row1
						  : ""+item.col1+", "+item.row1+", "+""+item.col2+", "+item.row2;
			mFormView.addFormObject(item.key, item.type, layout);
			}
		}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Dimension size = getSize();
		if (mSize == null)
			decodeLayout(mFormView.getFormLayout());
		else if (mSize.width != size.width || mSize.height != size.height)
			updateLayout();

		mSize = size;

		g.setColor(cThumbColor);
		g.fillRect(RULER_SIZE, 0, size.width - RULER_SIZE, RULER_SIZE);
		g.fillRect(0, RULER_SIZE, RULER_SIZE, size.height - RULER_SIZE);

	    // draw background of selected columns and rows
		for (int i=1; i<mLayout[0].length; i++) {
		    if (mIsSelected[0][i-1]) {
				g.setColor(cSelectedThumbColor);
		        g.fillRect(mLayout[0][i-1], 0, mLayout[0][i]-mLayout[0][i-1], RULER_SIZE);
				g.setColor(cSelectedBackgroundColor);
		        g.fillRect(mLayout[0][i-1], RULER_SIZE, mLayout[0][i]-mLayout[0][i-1], size.height - RULER_SIZE);
		    	}
			}
		for (int i=1; i<mLayout[1].length; i++) {
		    if (mIsSelected[1][i-1]) {
				g.setColor(cSelectedThumbColor);
		        g.fillRect(0, mLayout[1][i-1], RULER_SIZE, mLayout[1][i]-mLayout[1][i-1]);
				g.setColor(cSelectedBackgroundColor);
		        g.fillRect(RULER_SIZE, mLayout[1][i-1], size.width - RULER_SIZE, mLayout[1][i]-mLayout[1][i-1]);
		    	}
			}

			// draw buttons
		g.setColor(cThumbShadowColor);
		g.drawRect(RULER_SIZE, 0, size.width - RULER_SIZE, RULER_SIZE);
		g.drawRect(0, RULER_SIZE, RULER_SIZE, size.height - RULER_SIZE);
		for (int i=1; i<mLayout[0].length; i++)
	        g.drawLine(mLayout[0][i], 1, mLayout[0][i], RULER_SIZE-1);
		for (int i=1; i<mLayout[1].length; i++)
	        g.drawLine(1, mLayout[1][i], RULER_SIZE-1, mLayout[1][i]);
		g.setColor(cThumbHighlightColor);
		for (int i=0; i<mLayout[0].length-1; i++)
	        g.drawLine(mLayout[0][i]+1, 1, mLayout[0][i]+1, RULER_SIZE-1);
		for (int i=0; i<mLayout[1].length-1; i++)
	        g.drawLine(1, mLayout[1][i]+1, RULER_SIZE-1, mLayout[1][i]+1);

			// draw resize markers
        g.setColor(cThumbShadowColor.darker());
		for (int i=1; i<mLayout[0].length; i++)
		    if (mLayoutType[0][i-1] != SIZE_MODE_FILL) {
		        g.drawLine(mLayout[0][i]+2, RULER_SIZE-7, mLayout[0][i]+2, RULER_SIZE-2);
		        g.drawLine(mLayout[0][i]+3, RULER_SIZE-6, mLayout[0][i]+3, RULER_SIZE-3);
		        g.drawLine(mLayout[0][i]+4, RULER_SIZE-5, mLayout[0][i]+4, RULER_SIZE-4);
		        g.drawLine(mLayout[0][i]-2, RULER_SIZE-7, mLayout[0][i]-2, RULER_SIZE-2);
		        g.drawLine(mLayout[0][i]-3, RULER_SIZE-6, mLayout[0][i]-3, RULER_SIZE-3);
		        g.drawLine(mLayout[0][i]-4, RULER_SIZE-5, mLayout[0][i]-4, RULER_SIZE-4);
		    	}
		for (int i=1; i<mLayout[1].length; i++)
		    if (mLayoutType[1][i-1] != SIZE_MODE_FILL) {
		        g.drawLine(RULER_SIZE-7, mLayout[1][i]+2, RULER_SIZE-2, mLayout[1][i]+2);
		        g.drawLine(RULER_SIZE-6, mLayout[1][i]+3, RULER_SIZE-3, mLayout[1][i]+3);
		        g.drawLine(RULER_SIZE-5, mLayout[1][i]+4, RULER_SIZE-4, mLayout[1][i]+4);
		        g.drawLine(RULER_SIZE-7, mLayout[1][i]-2, RULER_SIZE-2, mLayout[1][i]-2);
		        g.drawLine(RULER_SIZE-6, mLayout[1][i]-3, RULER_SIZE-3, mLayout[1][i]-3);
		        g.drawLine(RULER_SIZE-5, mLayout[1][i]-4, RULER_SIZE-4, mLayout[1][i]-4);
		    	}

			// draw grid
		g.setColor(Color.gray);
		for (int i=1; i<mLayout[0].length; i++)
			g.drawLine(mLayout[0][i], RULER_SIZE+1, mLayout[0][i], size.height);
		for (int i=1; i<mLayout[1].length; i++)
			g.drawLine(RULER_SIZE+1, mLayout[1][i], size.width, mLayout[1][i]);

		for (int i=0; i<mItemList.size(); i++) {
			FormItem item = mItemList.get(i);
			if (item != mItemInFocus)
				drawFormItem(g, item);
			}

		if (mItemInFocus != null)	// draw this over all the others
			drawFormItem(g, mItemInFocus);
		}

	private void drawFormItem(Graphics g, FormItem item) {
		Rectangle itemRect = item.getRect();
		Rectangle closeBoxRect = getCloseBoxRect(itemRect);
		
		g.setClip(itemRect.intersection(getBounds()));

		g.setColor(Color.white);
		g.fillRect(itemRect.x+1, itemRect.y+1, itemRect.width-1, itemRect.height-1);

		g.setColor(item == mItemInFocus ? Color.red : Color.darkGray);

		if (item == mItemInFocus) {
			int xa = itemRect.x;
			int ya = itemRect.y;
			int xc = xa+itemRect.width-MARKER_SIZE;
			int yc = ya+itemRect.height-MARKER_SIZE;
			int xb = (xa+xc)/2;
			int yb = (ya+yc)/2;
			g.fillRect(xa, ya, MARKER_SIZE, MARKER_SIZE);
			g.fillRect(xa, yb, MARKER_SIZE, MARKER_SIZE);
			g.fillRect(xa, yc, MARKER_SIZE, MARKER_SIZE);
			g.fillRect(xb, ya, MARKER_SIZE, MARKER_SIZE);
			g.fillRect(xb, yc, MARKER_SIZE, MARKER_SIZE);
			g.fillRect(xc, ya, MARKER_SIZE, MARKER_SIZE);
			g.fillRect(xc, yb, MARKER_SIZE, MARKER_SIZE);
			g.fillRect(xc, yc, MARKER_SIZE, MARKER_SIZE);

			g.drawRect(closeBoxRect.x, closeBoxRect.y, closeBoxRect.width, closeBoxRect.height);
			g.drawLine(closeBoxRect.x, closeBoxRect.y, closeBoxRect.x+closeBoxRect.width, closeBoxRect.y+closeBoxRect.height);
			g.drawLine(closeBoxRect.x, closeBoxRect.y+closeBoxRect.height, closeBoxRect.x+closeBoxRect.width, closeBoxRect.y);
			}

		g.drawRect(itemRect.x, itemRect.y, itemRect.width-1, itemRect.height-1);

		g.drawString(item.title,
					 itemRect.x+(itemRect.width-g.getFontMetrics().stringWidth(item.title))/2,
					 itemRect.y+itemRect.height/2+4);
		}

	private Rectangle getCloseBoxRect(Rectangle itemRect) {
		Rectangle closeBoxRect = new Rectangle();
		int hSpace = 2 * CLOSE_BOX_MARGIN + CLOSE_BOX_SIZE - itemRect.width;
		int vSpace = 2 * CLOSE_BOX_MARGIN + CLOSE_BOX_SIZE - itemRect.height;
		if (hSpace <= 0) {
			closeBoxRect.x = itemRect.x + itemRect.width - CLOSE_BOX_MARGIN - CLOSE_BOX_SIZE;
			closeBoxRect.width = CLOSE_BOX_SIZE;
			}
		else {
			closeBoxRect.x = itemRect.x + CLOSE_BOX_MARGIN - (hSpace+2) / 4;
			closeBoxRect.width = CLOSE_BOX_SIZE - (hSpace+1) / 2;
			}
		if (vSpace <= 0) {
			closeBoxRect.y = itemRect.y + CLOSE_BOX_MARGIN;
			closeBoxRect.height = CLOSE_BOX_SIZE;
			}
		else {
			closeBoxRect.y = itemRect.y + CLOSE_BOX_MARGIN - (vSpace+2) / 4;
			closeBoxRect.height = CLOSE_BOX_SIZE - (vSpace+1) / 2;
			}
		return closeBoxRect;
		}

	private void decodeLayout(double[][] layoutDesc) {
		Dimension size = getSize();
		for (int i=0; i<2; i++) {
			int total = ((i==0) ? size.width : size.height) - RULER_SIZE - 1;
			int remaining = total;
			for (int j=0; j<layoutDesc[i].length; j++)
				if (layoutDesc[i][j] > 1.0)
				    total -= (int)layoutDesc[i][j];
			int relativeCells = 0;
			int fillCells = 0;
			for (int j=0; j<layoutDesc[i].length; j++) {
				if (layoutDesc[i][j] > 1.0) {
					int pixels = (int)layoutDesc[i][j];
					mLayoutSize[i][j] = pixels;
					remaining -= pixels;
					mLayoutType[i][j] = SIZE_MODE_FIXED;
					}
				else if (layoutDesc[i][j] >= 0.0) {
					relativeCells++;
					int pixels = Math.max(MINIMUM_CELL_SIZE ,(int)(layoutDesc[i][j] * total));
					mLayoutSize[i][j] = pixels;
					remaining -= pixels;
					mLayoutType[i][j] = SIZE_MODE_RELATIVE;
					}
				else {
					fillCells++;
					mLayoutType[i][j] = SIZE_MODE_FILL;
					}
				}
			if (fillCells != 0) {
				int fillPixels = remaining / fillCells;
				for (int j=0; j<layoutDesc[i].length; j++) {
					if (layoutDesc[i][j] < 0.0) {
						int pixels = (--fillCells == 0) ? remaining : fillPixels;
						mLayoutSize[i][j] = pixels;
						remaining -= pixels;
						}
					}
				}
			else if (relativeCells != 0) {	// if we have no fill cells, then add remaining space evenly to all relative cells
				int fillPixels = remaining / relativeCells;
				for (int j=0; j<layoutDesc[i].length; j++) {
					if (layoutDesc[i][j] >= 0.0 && layoutDesc[i][j] <= 1.0) {
						int pixels = (--fillCells == 0) ? remaining : fillPixels;
						mLayoutSize[i][j] += pixels;
						remaining -= pixels;
						}
					}
				}

			mLayout[i][0] = RULER_SIZE;
			for (int j=0; j<layoutDesc[i].length; j++)
				mLayout[i][j+1] = mLayout[i][j] + mLayoutSize[i][j];
			}
		}

	private void updateLayout() {
		Dimension newSize = getSize();
		for (int i=0; i<2; i++) {
			int oldTotal = ((i==0) ? mSize.width : mSize.height) - RULER_SIZE - 1;
			int newTotal = ((i==0) ? newSize.width : newSize.height) - RULER_SIZE - 1;

			int fillCount = 0;
			int remaining = newTotal;
			for (int j=0; j<mLayoutSize[i].length; j++) {
				switch (mLayoutType[i][j]) {
				case SIZE_MODE_FIXED:
					remaining -= mLayoutSize[i][j];
					break;
				case SIZE_MODE_RELATIVE:
					mLayoutSize[i][j] = Math.max(MINIMUM_CELL_SIZE ,mLayoutSize[i][j] * newTotal / oldTotal);
					remaining -= mLayoutSize[i][j];
					break;
				case SIZE_MODE_FILL:
					fillCount++;
					}
				}

			for (int j=0; j<mLayoutSize[i].length; j++) {
				if (mLayoutType[i][j] == SIZE_MODE_FILL) {
					mLayoutSize[i][j] = remaining / fillCount--;
					remaining -= mLayoutSize[i][j];
					}
				}

			mLayout[i][0] = RULER_SIZE;
			for (int j=0; j<mLayoutSize[i].length; j++) {
				mLayout[i][j+1] = mLayout[i][j] + mLayoutSize[i][j];
				}
			}
		}

	private void encodeLayout() {
		Dimension size = getSize();
		double[][] layoutDesc = new double[2][];
		layoutDesc[0] = new double[mLayoutType[0].length];
		layoutDesc[1] = new double[mLayoutType[1].length];
		for (int i=0; i<2; i++) {
			double total = ((i==0) ? size.width : size.height) - RULER_SIZE - 1;
			for (int j=0; j<layoutDesc[i].length; j++)
				if (mLayoutType[i][j] == SIZE_MODE_FIXED)
				    total -= mLayoutSize[i][j];

			for (int j=0; j<layoutDesc[i].length; j++) {
				layoutDesc[i][j] = (mLayoutType[i][j] == SIZE_MODE_FIXED) ? mLayoutSize[i][j]
								 : (mLayoutType[i][j] == SIZE_MODE_RELATIVE) ? (double)mLayoutSize[i][j] / total
								 : TableLayoutConstants.FILL;
				}
			}

		mFormView.setFormLayout(layoutDesc);
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().startsWith("Insert")) {
			int size = mLayoutType[mRulerNo].length+1;

			int[] newLayoutType = new int[size];
			int[] newLayoutSize = new int[size];

			int sourceIndex = 0;
			int insertionIndex = mRulerCellIndex;
			if (e.getActionCommand().endsWith("Right")
			 || e.getActionCommand().endsWith("Below"))
				insertionIndex++;
			for (int i=0; i<size; i++) {
				if (i == insertionIndex) {
					newLayoutType[i] = SIZE_MODE_FILL;
					}
				else {
					newLayoutType[i] = mLayoutType[mRulerNo][sourceIndex];
					newLayoutSize[i] = mLayoutSize[mRulerNo][sourceIndex];
					sourceIndex++;
					}
				}
			mLayoutType[mRulerNo] = newLayoutType;
			mLayoutSize[mRulerNo] = newLayoutSize;
			mLayout[mRulerNo] = new int[size+1];
			mIsSelected[mRulerNo] = new boolean[size];
			updateLayout();

			for (int i=0; i<mItemList.size(); i++) {
				FormItem item = mItemList.get(i);
				if (mRulerNo == 0) {
					if (item.col1 >= insertionIndex) {
						item.col1++;
						item.col2++;
						}
					else if (item.col2 >= insertionIndex) {
						item.col2++;
						}
					}
				else {
					if (item.row1 >= insertionIndex) {
						item.row1++;
						item.row2++;
						}
					else if (item.row2 >= insertionIndex) {
						item.row2++;
						}
					}
				}

			repaint();
			}
		else if (e.getActionCommand().equals("Remove Column")
			  || e.getActionCommand().equals("Remove Row")) {
		    removeRowOrColumn(mRulerCellIndex);
			mIsSelected[mRulerNo] = new boolean[mLayoutType[mRulerNo].length];
		    repaint();
			}
		else if (e.getActionCommand().equals("Remove Selected Columns")
			  || e.getActionCommand().equals("Remove Selected Rows")) {
		    for (int i=mIsSelected[mRulerNo].length-1; i>=0; i--)
		        if (mIsSelected[mRulerNo][i])
				    removeRowOrColumn(i);
			mIsSelected[mRulerNo] = new boolean[mLayoutType[mRulerNo].length];
		    repaint();
			}
		else if (e.getActionCommand().equals("Fixed Size")) {
			if (mLayoutType[mRulerNo][mRulerCellIndex] != SIZE_MODE_FIXED) {
				mLayoutType[mRulerNo][mRulerCellIndex] = SIZE_MODE_FIXED;
				repaint();
				}
			}
		else if (e.getActionCommand().equals("Relative Size")) {
			if (mLayoutType[mRulerNo][mRulerCellIndex] != SIZE_MODE_RELATIVE) {
				mLayoutType[mRulerNo][mRulerCellIndex] = SIZE_MODE_RELATIVE;
				repaint();
				}
			}
		else if (e.getActionCommand().equals("Share Remaining Space")) {
			if (mLayoutType[mRulerNo][mRulerCellIndex] != SIZE_MODE_FILL) {
				mLayoutType[mRulerNo][mRulerCellIndex] = SIZE_MODE_FILL;
				updateLayout();
				repaint();
				}
			}
		else if (e.getActionCommand().equals("Remove Item")) {
			mItemList.remove(mItemInFocus);
			mItemInFocus = null;
			repaint();
			}
		else if (e.getActionCommand().equals("Remove All Items")) {
			if (JOptionPane.showConfirmDialog(this,
					"Do you really want to remove all items?",
					"Remove All Items?",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				mItemList.clear();
				mItemInFocus = null;
				repaint();
				}
			}
		else if (e.getActionCommand().startsWith("Add ")) {
			String key = e.getActionCommand().substring(4);
			FormItem item = new FormItem(key,
									mFormView.getModel().getTitle(key),
									mFormView.getModel().getType(key),
									mMouseLocation.x, mMouseLocation.x,
									mMouseLocation.y, mMouseLocation.y);
			mItemList.add(item);
			repaint();
			}
		else {
			mItemInFocus.key = e.getActionCommand();
			mItemInFocus.title = mFormView.getModel().getTitle(mItemInFocus.key);
			mItemInFocus.type = mFormView.getModel().getType(mItemInFocus.key);
			repaint();
			}
		}

	private void removeRowOrColumn(int index) {
		int size = mLayoutType[mRulerNo].length-1;

		int[] newLayoutType = new int[size];
		int[] newLayoutSize = new int[size];

		int sourceIndex = 0;
		for (int i=0; i<size; i++) {
			if (i == index)
				sourceIndex++;

			newLayoutType[i] = mLayoutType[mRulerNo][sourceIndex];
			newLayoutSize[i] = mLayoutSize[mRulerNo][sourceIndex];
			sourceIndex++;
			}
		mLayoutType[mRulerNo] = newLayoutType;
		mLayoutSize[mRulerNo] = newLayoutSize;
		mLayout[mRulerNo] = new int[size+1];
		updateLayout();

		for (int i=mItemList.size()-1; i>=0; i--) {
			FormItem item = mItemList.get(i);
			if (mRulerNo == 0) {
				if (item.col1 == index
				 && item.col2 == index) {
					mItemList.remove(i);
					}
				else {
					if (item.col1 > index)
						item.col1--;
					if (item.col2 >= index)
						item.col2--;
					}
				}
			else {
				if (item.row1 == index
				 && item.row2 == index) {
					mItemList.remove(i);
					}
				else {
					if (item.row1 > index)
						item.row1--;
					if (item.row2 >= index)
						item.row2--;
					}
				}
			}
		}
	
    public void mouseClicked(MouseEvent e) {
	    }

    public void mousePressed(MouseEvent e) {
        if (handlePopupTrigger(e))
            return;

        if (e.getButton() == MouseEvent.BUTTON1) {
			if (mDraggingMode == MODE_CLOSING) {
				mItemList.remove(mItemInFocus);
				mItemInFocus = null;
				repaint();
				}
			else if (mDraggingMode == MODE_RESIZING_GRID)
				mMouseLocation = e.getPoint();
			else
				mMouseLocation = getGridLocation(e.getPoint());

			if (mDraggingMode == MODE_SELECTING) {
				if (!e.isShiftDown())
					mIsSelected[mRulerNo] = new boolean[mLayoutType[mRulerNo].length];
				mIsSelected[1-mRulerNo] = new boolean[mLayoutType[1-mRulerNo].length];
				int index = (mRulerNo == 0) ? mMouseLocation.x : mMouseLocation.y;
				mIsSelected[mRulerNo][index] = true;
				repaint();
				}
			}
    	}

    public void mouseReleased(MouseEvent e) {
        if (handlePopupTrigger(e))
            return;
	    }

    public void mouseEntered(MouseEvent e) {
	    }

    public void mouseExited(MouseEvent e) {
	    }

    public void mouseDragged(MouseEvent e) {
		if (mDraggingMode == MODE_SELECTING) {
		    Point location = getGridLocation(e.getPoint());
		    int index1 = (mRulerNo == 0) ? mMouseLocation.x : mMouseLocation.y;
		    int index2 = (mRulerNo == 0) ? location.x : location.y;
		    if (index1 > index2) {
		        int temp = index1;
		        index1 = index2;
		        index2 = temp;
		    	}
		    for (int i=0; i<mIsSelected[mRulerNo].length; i++) {
		        boolean selected = (i >= index1 && i <= index2);
		        if (selected ^ mIsSelected[mRulerNo][i]) {
		            mIsSelected[mRulerNo][i] = selected;
		            repaint();
		        	}
		    	}
			}
		else if (mDraggingMode == MODE_RESIZING_GRID) {
			int delta = (mRulerNo == 0) ? e.getX() - mMouseLocation.x : e.getY() - mMouseLocation.y;
			if (mLayoutSize[mRulerNo][mRulerIndex] + delta < MINIMUM_CELL_SIZE)
				delta = MINIMUM_CELL_SIZE - mLayoutSize[mRulerNo][mRulerIndex];

			if (delta != 0) {
				int fillCount = 0;
				int fillSize = 0;
				for (int j=0; j<mLayoutType[mRulerNo].length; j++) {
					if (mLayoutType[mRulerNo][j] == SIZE_MODE_FILL) {
						fillCount++;
						fillSize += mLayoutSize[mRulerNo][j] - MINIMUM_CELL_SIZE;
						}
					}

				if (fillSize > delta) {
					fillSize -= delta;
					for (int j=0; j<mLayoutType[mRulerNo].length; j++) {
						if (mLayoutType[mRulerNo][j] == SIZE_MODE_FILL) {
							mLayoutSize[mRulerNo][j] = MINIMUM_CELL_SIZE + fillSize/fillCount--;
							fillSize -= mLayoutSize[mRulerNo][j] - MINIMUM_CELL_SIZE;
							}
						}
					}
				else {
					for (int j=0; j<mLayoutType[mRulerNo].length; j++) {
						if (mLayoutType[mRulerNo][j] == SIZE_MODE_FILL) {
							mLayoutSize[mRulerNo][j] = MINIMUM_CELL_SIZE;
							}
						}
					delta = fillSize;
					}

				mLayoutSize[mRulerNo][mRulerIndex] += delta;

				mLayout[mRulerNo][0] = RULER_SIZE;
				for (int j=0; j<mLayoutType[mRulerNo].length; j++)
					mLayout[mRulerNo][j+1] = mLayout[mRulerNo][j] + mLayoutSize[mRulerNo][j];

				mMouseLocation = e.getPoint();
				repaint();
				}
			}
		if (mDraggingMode == MODE_RELOCATING) {
			Point location = getGridLocation(e.getPoint());
			if (!location.equals(mMouseLocation)) {
				int dx = location.x - mMouseLocation.x;
				int dy = location.y - mMouseLocation.y;
				dx = Math.min(Math.max(dx, -mItemInFocus.col1), mLayoutType[0].length-mItemInFocus.col2-1);
				dy = Math.min(Math.max(dy, -mItemInFocus.row1), mLayoutType[1].length-mItemInFocus.row2-1);
				if (dx != 0 || dy != 0) {
					mItemInFocus.col1 += dx;
					mItemInFocus.col2 += dx;
					mItemInFocus.row1 += dy;
					mItemInFocus.row2 += dy;
					mMouseLocation.x += dx;
					mMouseLocation.y += dy;
					repaint();
					}
				}
			}
		else if ((mDraggingMode & MODE_RESIZING) != 0) {
			Point location = getGridLocation(e.getPoint());
			boolean gridLocationChanged = false;
			if (mMouseLocation.x != location.x) {
				if ((mDraggingMode & MODE_RESIZING_W) != 0 && location.x <= mItemInFocus.col2 && location.x != mItemInFocus.col1) {
					mItemInFocus.col1 = location.x;
					gridLocationChanged = true;
					}
				if ((mDraggingMode & MODE_RESIZING_E) != 0 && location.x >= mItemInFocus.col1 && location.x != mItemInFocus.col2) {
					mItemInFocus.col2 = location.x;
					gridLocationChanged = true;
					}
				}
			if (mMouseLocation.y != location.y) {
				if ((mDraggingMode & MODE_RESIZING_N) != 0 && location.y <= mItemInFocus.row2 && location.y != mItemInFocus.row1) {
					mItemInFocus.row1 = location.y;
					gridLocationChanged = true;
					}
				if ((mDraggingMode & MODE_RESIZING_S) != 0 && location.y >= mItemInFocus.row1 && location.y != mItemInFocus.row2) {
					mItemInFocus.row2 = location.y;
					gridLocationChanged = true;
					}
				}
			if (gridLocationChanged) {
				mMouseLocation = location;
				repaint();
				}
			}
		}

    public void mouseMoved(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();

		mDraggingMode = MODE_NONE;

		Cursor oldCursor = getCursor();
		Cursor cursor = Cursor.getDefaultCursor();

		FormItem oldItemInFocus = mItemInFocus;
		mItemInFocus = null;

		if ((x < RULER_SIZE) ^ (y < RULER_SIZE)) {
			mRulerNo = (x < RULER_SIZE) ? 1 : 0;
			if (mRulerNo == 1) {
				int temp = x;
				x = y;
				y = temp;
				}
			for (int i=0; i<mLayout[mRulerNo].length; i++) {
				if (x >= mLayout[mRulerNo][i]-1
				 && x <= mLayout[mRulerNo][i]+1) {
					if (i>0 && mLayoutType[mRulerNo][i-1] != SIZE_MODE_FILL) {
						mRulerIndex = i-1;
						mDraggingMode = MODE_RESIZING_GRID;
						cursor = Cursor.getPredefinedCursor((mRulerNo == 0) ? Cursor.W_RESIZE_CURSOR : Cursor.S_RESIZE_CURSOR);
						}
					break;
					}
				}
			if (mDraggingMode == MODE_NONE)
			    mDraggingMode = MODE_SELECTING;
			}
		else {
			for (int i=0; i<mItemList.size(); i++) {
				FormItem item = mItemList.get(i);
				Rectangle itemRect = item.getRect();
				if (itemRect.contains(x, y)) {
					mItemInFocus = item;
					int xa = itemRect.x;
					int ya = itemRect.y;
					int xc = xa+itemRect.width-MARKER_SIZE;
					int yc = ya+itemRect.height-MARKER_SIZE;
					int xb = (xa+xc)/2;
					int yb = (ya+yc)/2;
					if (getCloseBoxRect(itemRect).contains(x, y)) {
						cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
						mDraggingMode = MODE_CLOSING;
						}
					else if (new Rectangle(xa, ya, MARKER_SIZE, MARKER_SIZE).contains(x, y)) {
						cursor = Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
						mDraggingMode = MODE_RESIZING_NW;
						}
					else if (new Rectangle(xa, yb, MARKER_SIZE, MARKER_SIZE).contains(x, y)) {
						cursor = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
						mDraggingMode = MODE_RESIZING_W;
						}
					else if (new Rectangle(xa, yc, MARKER_SIZE, MARKER_SIZE).contains(x, y)) {
						cursor = Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
						mDraggingMode = MODE_RESIZING_SW;
						}
					else if (new Rectangle(xb, ya, MARKER_SIZE, MARKER_SIZE).contains(x, y)) {
						cursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
						mDraggingMode = MODE_RESIZING_N;
						}
					else if (new Rectangle(xb, yc, MARKER_SIZE, MARKER_SIZE).contains(x, y)) {
						cursor = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
						mDraggingMode = MODE_RESIZING_S;
						}
					else if (new Rectangle(xc, ya, MARKER_SIZE, MARKER_SIZE).contains(x, y)) {
						cursor = Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
						mDraggingMode = MODE_RESIZING_NE;
						}
					else if (new Rectangle(xc, yb, MARKER_SIZE, MARKER_SIZE).contains(x, y)) {
						cursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
						mDraggingMode = MODE_RESIZING_E;
						}
					else if (new Rectangle(xc, yc, MARKER_SIZE, MARKER_SIZE).contains(x, y)) {
						cursor = Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
						mDraggingMode = MODE_RESIZING_SE;
						}
					else {
						cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
						mDraggingMode = MODE_RELOCATING;
						}
					}
				}
			}

		if (mItemInFocus != oldItemInFocus)
			repaint();

		if (!cursor.equals(oldCursor))
			setCursor(cursor);
		}

    private boolean handlePopupTrigger(MouseEvent e) {
        if (e.isPopupTrigger()) {
			int x = e.getX();
			int y = e.getY();

			if ((x < RULER_SIZE) ^ (y < RULER_SIZE)) {
				mRulerNo = (x < RULER_SIZE) ? 1 : 0;
				int position = (mRulerNo == 0) ? x : y;
				int noOfSelected = 0;
				for (int i=0; i<mIsSelected[mRulerNo].length; i++)
				    if (mIsSelected[mRulerNo][i])
				        noOfSelected++;
				for (int i=0; i<mLayoutType[mRulerNo].length; i++) {
					if (position > mLayout[mRulerNo][i]
					 && position < mLayout[mRulerNo][i+1]) {
						mRulerCellIndex = i;

						JPopupMenu popup = new JPopupMenu();
						JMenuItem menuItem1 = new JMenuItem(mRulerNo == 0 ? "Insert Column Left" : "Insert Row Above");
						menuItem1.addActionListener(this);
						popup.add(menuItem1);
						JMenuItem menuItem2 = new JMenuItem(mRulerNo == 0 ? "Insert Column Right" : "Insert Row Below");
						menuItem2.addActionListener(this);
						popup.add(menuItem2);
						popup.addSeparator();
						boolean removeItemAdded = false;
						if (mLayoutType[mRulerNo].length > 1) {
						    JMenuItem menuItem3 = new JMenuItem(mRulerNo == 0 ? "Remove Column" : "Remove Row");
						    menuItem3.addActionListener(this);
						    popup.add(menuItem3);
						    removeItemAdded = true;
							}
						if (noOfSelected != 0 && noOfSelected < mLayoutType[mRulerNo].length) {
							JMenuItem menuItem4 = new JMenuItem(mRulerNo == 0 ? "Remove Selected Columns" : "Remove Selected Rows");
							menuItem4.addActionListener(this);
							popup.add(menuItem4);
						    removeItemAdded = true;
							}
						if (removeItemAdded)
						    popup.addSeparator();
						ButtonGroup buttonGroup = new ButtonGroup();
						JRadioButtonMenuItem rbFixed = new JRadioButtonMenuItem("Fixed Size",
									mLayoutType[mRulerNo][mRulerCellIndex] == SIZE_MODE_FIXED);
						buttonGroup.add(rbFixed);
						rbFixed.addActionListener(this);
						popup.add(rbFixed);
						JRadioButtonMenuItem rbRelative = new JRadioButtonMenuItem("Relative Size",
									mLayoutType[mRulerNo][mRulerCellIndex] == SIZE_MODE_RELATIVE);
						buttonGroup.add(rbRelative);
						rbRelative.addActionListener(this);
						popup.add(rbRelative);
						JRadioButtonMenuItem rbFill = new JRadioButtonMenuItem("Share Remaining Space",
									mLayoutType[mRulerNo][mRulerCellIndex] == SIZE_MODE_FILL);
						buttonGroup.add(rbFill);
						rbFill.addActionListener(this);
						popup.add(rbFill);
						popup.show(this, x, y);
						}
					}
				}
			else if ((x > RULER_SIZE) && (y > RULER_SIZE)) {
				mMouseLocation = getGridLocation(e.getPoint());
				FormModel model = mFormView.getModel();
				if (mItemInFocus != null) {
					JPopupMenu popup = new JScrollablePopupMenu();
					JMenuItem menuItem1 = new JMenuItem("Remove Item");
					menuItem1.addActionListener(this);
					popup.add(menuItem1);
					JMenuItem menuItem2 = new JMenuItem("Remove All Items");
					menuItem2.addActionListener(this);
					popup.add(menuItem2);
					popup.addSeparator();
					ButtonGroup buttonGroup = new ButtonGroup();
					for (int i=0; i<model.getKeyCount(); i++) {
						String key = model.getKey(i);
						String title = model.getTitle(key);
						JRadioButtonMenuItem rb = new JRadioButtonMenuItem(title,
										key.equals(mItemInFocus.key));
						buttonGroup.add(rb);
						rb.setActionCommand(key);
						rb.addActionListener(this);
						popup.add(rb);
						}
					popup.show(this, x, y);
					}
				else {
					JPopupMenu popup = new JScrollablePopupMenu();
					for (int i=0; i<model.getKeyCount(); i++) {
						String key = model.getKey(i);
						String title = model.getTitle(key);
						JMenuItem mi = new JMenuItem("Add "+title);
						mi.setActionCommand("Add "+key);
						mi.addActionListener(this);
						popup.add(mi);
						}
					popup.show(this, x, y);
					}
				}

			return true;
			}
        return false;
		}

	private Point getGridLocation(Point p) {
		int x = 1;
		int y = 1;
		while (x < mLayout[0].length-1 && p.x > mLayout[0][x])
			x++;
		while (y < mLayout[1].length-1 && p.y > mLayout[1][y])
			y++;

		return new Point(x-1, y-1);
		}

	class FormItem {
		String	key,title,type;
		int		col1,col2,row1,row2;

		public FormItem(String key, String title, String type, int col1, int col2, int row1, int row2) {
			this.key = key;
			this.title = title;
			this.type = type;
			this.col1 = col1;
			this.col2 = col2;
			this.row1 = row1;
			this.row2 = row2;
			}

		public Rectangle getRect() {
			int x1 = mLayout[0][col1]+1;
			int x2 = mLayout[0][col2+1];
			int y1 = mLayout[1][row1]+1;
			int y2 = mLayout[1][row2+1];
			return new Rectangle(x1, y1, x2-x1, y2-y1);
			}
		}
	}
