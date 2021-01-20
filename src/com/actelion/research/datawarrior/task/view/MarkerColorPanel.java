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

package com.actelion.research.datawarrior.task.view;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.VisualizationColor;


public class MarkerColorPanel extends JPanel
				implements MouseListener,MouseMotionListener {
    static final long serialVersionUID = 0x20060821;

//	private static final Color cBackgroundColor = new Color(204, 204, 204);
//	private static final Color cShadowColor     = new Color(153, 153, 153);
	private static final Color cDarkShadowColor = new Color(102, 102, 102);

	private static final int cBorder = 8;
	private static final int cSpacing = 4;

	private static final int cColorWedgeButton = 2;
	private static final int cDefaultCategoryColorCount = 8;
	private static final int cMaxEditableColors = 24;

	private Frame               mOwner;
	private int                 mPressedButton,mColorListMode,mLastColorListMode;
	private boolean             mMouseOverButton;
	private Color[]             mWedgeColorList,mCategoryColorList;
	private Rectangle[]         mRect;
	private ActionListener		mListener;

	public MarkerColorPanel(Frame owner,
							CompoundTableModel tableModel,
							ActionListener listener,
							boolean isBackgroundColor) {
		mOwner = owner;
		mListener = listener;

 		setSize(new Dimension(236, 36));
        setMinimumSize(new Dimension(236, 36));
        setPreferredSize(new Dimension(236, 36));
        setMaximumSize(new Dimension(236, 36));
        setOpaque(true);

		mLastColorListMode = VisualizationColor.cColorListModeHSBLong;
		updateColorList(-1);

		addMouseListener(this);
		addMouseMotionListener(this);
		mPressedButton = -1;
		}

	public void paintComponent(Graphics g) {
        super.paintComponent(g);
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Dimension theSize = getSize();

		if (mColorListMode == VisualizationColor.cColorListModeCategories) {
			int categories = mCategoryColorList.length;
			if (categories <= cMaxEditableColors) {
				int fieldWidth = (theSize.width - 2 * cBorder + cSpacing) / categories - cSpacing;
				mRect = new Rectangle[categories];
				for (int i=0; i<categories; i++) {
					mRect[i] = new Rectangle(cBorder+i*(fieldWidth+cSpacing), cBorder, fieldWidth, theSize.height-2*cBorder);
					drawColorButton(g, i);
					}
				}
			else {
				final String message = "<too many colors to edit>";
				g.setColor(Color.GRAY);
				g.setFont(new Font("Arial", Font.BOLD, 13));
				FontMetrics m = g.getFontMetrics();
				g.drawString(message, (theSize.width-m.stringWidth(message))/2, (theSize.height+m.getHeight())/2-m.getDescent());
				}
		    }
		else {
			int size = theSize.height-2*cBorder;
			int x1 = 2*cBorder+size;
			int x2 = theSize.width-2*cBorder-size;
			mRect = new Rectangle[3];
		    mRect[0] = new Rectangle(cBorder, cBorder, size, size);
		    mRect[1] = new Rectangle(theSize.width-size-cBorder,
							   theSize.height-size-cBorder, size, size);
			mRect[cColorWedgeButton] = new Rectangle(x1, cBorder, x2-x1, size);
			drawColorButton(g, 0);
			drawColorButton(g, 1);
			drawColorButton(g, cColorWedgeButton);
			}
		}

	public void mouseClicked(MouseEvent e) {
		}

	public void mousePressed(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		for (int i=0; mRect != null && i<mRect.length; i++) {
			if (mRect[i].contains(x, y)) {
				mPressedButton = i;
				mMouseOverButton = true;
				repaint();
				}
			}
		}

	public void mouseReleased(MouseEvent e) {
		if (mMouseOverButton && mPressedButton != -1) {
			boolean needsUpdate = false;
            if (mColorListMode == VisualizationColor.cColorListModeCategories) {
                Color newColor = JColorChooser.showDialog(
				    mOwner, "Select category color", mCategoryColorList[mPressedButton]);
				if (newColor != null) {
					mCategoryColorList[mPressedButton] = newColor;
					needsUpdate = true;
					}
                }
			else {
				if (mPressedButton == cColorWedgeButton) {
					if (++mColorListMode > VisualizationColor.cColorListModeStraight)
						mColorListMode = VisualizationColor.cColorListModeHSBShort;
					updateColorList(-1);
					needsUpdate = true;
					}
				else {
					int index = mPressedButton * (mWedgeColorList.length-1);
					Color newColor = JColorChooser.showDialog(
					    mOwner, "Select color for min/max value", mWedgeColorList[index]);
					if (newColor != null) {
						mWedgeColorList[index] = newColor;
						updateColorList(-1);
						needsUpdate = true;
				    	}
	                }
				}

			mPressedButton = -1;
			mMouseOverButton = false;

            if (needsUpdate) {
            	mListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "colorChanged"));
	    		repaint();
                }
			}
		}

	public void mouseEntered(MouseEvent e) {
		}

	public void mouseExited(MouseEvent e) {
		}

	public void mouseMoved(MouseEvent e) {
		}

	public void mouseDragged(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		boolean oldMouseOverButton = mMouseOverButton;
		if (mPressedButton != -1)
			mMouseOverButton = mRect[mPressedButton].contains(x, y);
		if (mMouseOverButton ^ oldMouseOverButton)
			repaint();
		}

	/**
	 * 
	 * @return number of currently displayed categories or -1 if mode is not cColorListModeCategories
	 */
	public int getCategoryCount() {
		return (mColorListMode == VisualizationColor.cColorListModeCategories) ?
				mCategoryColorList.length : -1;
		}

	public int getColorListMode() {
		return mColorListMode;
		}

	public Color[] getColorList() {
		if (mColorListMode == VisualizationColor.cColorListModeCategories)
			return mCategoryColorList;
		else
			return mWedgeColorList;
		}

	/**
	 * Sets all properties of this panel and calls repaint().
	 * If colorList is null, then a default colorlist is generated according to the mode.
	 * @param colorList null or depending on mode a complete color wedge or all category colors
	 * @param colorListMode
	 * @param categoryCount used to generate default category color list
	 */
	public void setColor(Color[] colorList, int colorListMode, int categoryCount) {
		mColorListMode = colorListMode;
		if (mColorListMode == VisualizationColor.cColorListModeCategories)
			mCategoryColorList = colorList;
		else
			mWedgeColorList = colorList;

		if (colorList == null)
			updateColorList(categoryCount);

		repaint();
		}

	/**
	 * Update the color panel appearance (mode and category count).
	 * When switching from category mode to numerical, then the most recent numerical mode is restored.
	 * @param categoryCount number of categories or -1 if numerical mode
	 */
	public void updateColorListMode(int categoryCount) {
		if (categoryCount == -1) {
			if (mColorListMode != VisualizationColor.cColorListModeCategories)
				return;
			}
		else {
			if (mColorListMode == VisualizationColor.cColorListModeCategories && mCategoryColorList.length == categoryCount)
				return;
			}

		// if we switch from categories to numerical then use the most recent numerical mode
		if (categoryCount == -1 && mColorListMode == VisualizationColor.cColorListModeCategories) {
			mColorListMode = mLastColorListMode;
			}
		// if we switch from numerical to categories then remember the current numerical mode
		else if (categoryCount != -1 && mColorListMode != VisualizationColor.cColorListModeCategories) {
			mLastColorListMode = mColorListMode;
			mColorListMode = VisualizationColor.cColorListModeCategories;
			}

		updateColorList(categoryCount);
		repaint();
		}

	private void updateColorList(int categoryCount) {
		if (mColorListMode == VisualizationColor.cColorListModeCategories) {
			if (mCategoryColorList == null || mCategoryColorList.length != categoryCount)
				mCategoryColorList = VisualizationColor.createDiverseColorList((categoryCount == -1) ?
						cDefaultCategoryColorCount : categoryCount);
			}
		else {
			if (mWedgeColorList == null)
			    mWedgeColorList = VisualizationColor.createColorWedge(Color.RED, Color.BLUE, mColorListMode, null);
			else
			    mWedgeColorList = VisualizationColor.createColorWedge(mWedgeColorList[0], mWedgeColorList[mWedgeColorList.length-1], mColorListMode, null);
			}
		}

	private void drawColorButton(Graphics g, int no) {
		g.setColor(cDarkShadowColor);
		g.drawLine(mRect[no].x, mRect[no].y, mRect[no].x+mRect[no].width-2, mRect[no].y);
		g.drawLine(mRect[no].x, mRect[no].y, mRect[no].x, mRect[no].y+mRect[no].height-2);
		g.drawLine(mRect[no].x+mRect[no].width-2, mRect[no].y+2,
			       mRect[no].x+mRect[no].width-2, mRect[no].y+mRect[no].height-2);
		g.drawLine(mRect[no].x+2, mRect[no].y+mRect[no].height-2,
			       mRect[no].x+mRect[no].width-2, mRect[no].y+mRect[no].height-2);

		if (mPressedButton != no || !mMouseOverButton)
			g.setColor(Color.white);

		g.drawLine(mRect[no].x+1, mRect[no].y+1, mRect[no].x+mRect[no].width-3, mRect[no].y+1);
		g.drawLine(mRect[no].x+1, mRect[no].y+1, mRect[no].x+1, mRect[no].y+mRect[no].height-3);

		if (mPressedButton == no && mMouseOverButton)
			g.setColor(Color.white);

		g.drawLine(mRect[no].x+mRect[no].width-1, mRect[no].y+1,
			       mRect[no].x+mRect[no].width-1, mRect[no].y+mRect[no].height-1);
		g.drawLine(mRect[no].x+1, mRect[no].y+mRect[no].height-1,
			       mRect[no].x+mRect[no].width-1, mRect[no].y+mRect[no].height-1);

		if (mColorListMode != VisualizationColor.cColorListModeCategories && no==cColorWedgeButton) {
			int x1 = mRect[no].x+2;
			int x2 = x1+mRect[no].width-4;
			if (x1<x2) {
				for (int x=x1; x<x2; x++) {
					int c = mWedgeColorList.length*(x-x1)/(x2-x1);
					g.setColor((mPressedButton == no && mMouseOverButton) ?
							mWedgeColorList[c].darker() : mWedgeColorList[c]);
					g.drawLine(x, mRect[no].y+2, x, mRect[no].y+mRect[no].height-3);
					}
				}
			}
		else {
			Color color = (mColorListMode == VisualizationColor.cColorListModeCategories) ?
					mCategoryColorList[no] : mWedgeColorList[no*(mWedgeColorList.length-1)];
			if (mPressedButton == no && mMouseOverButton)
				color = color.darker();

			g.setColor(color);
    	    g.fillRect(mRect[no].x+2, mRect[no].y+2, mRect[no].width-4, mRect[no].height-4);
			}
		}
	}
