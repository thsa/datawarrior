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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.ImageObserver;
import java.io.File;

import javax.swing.JPanel;

import com.actelion.research.gui.FileHelper;

public class DEImagePanel extends JPanel implements ImageObserver,MouseListener,MouseMotionListener {
    private static final long serialVersionUID = 0x20110413;

	private String		        mFileName;
	private Image		        mImage,mOffImage;
	private Graphics	        mOffG;
	private boolean		        mOffImageValid,mImageAvailable,mSelecting,mFileNameChanged,mImageIsLoading;
	private int                 mMouseX,mMouseY,mImageX1,mImageY1,mImageX2,mImageY2,
								mImageWidth,mImageHeight,mSourceX1,mSourceY1,mSourceX2,mSourceY2;
	private Rectangle           mSelectionRect;

    public DEImagePanel() {
		addMouseListener(this);
		addMouseMotionListener(this);
		}

	public void paint(Graphics g) {
		Dimension theSize = getSize();
		if (mOffImage == null
		 || mOffImage.getWidth(null) != theSize.width
		 || mOffImage.getHeight(null) != theSize.height) {
			mOffImage = createImage(theSize.width, theSize.height);
			mOffG = mOffImage.getGraphics();
			mOffImageValid = false;
			}

		if (!mOffImageValid) {
			mOffG.setColor(Color.lightGray);
			mOffG.fillRect(0, 0, mOffImage.getWidth(null), mOffImage.getHeight(null));
			if (mFileName != null) {
				if (mFileNameChanged && !mImageIsLoading) {
                    if (mImage != null)
                        mImage.flush(); // take image out of buffer to make sure that imageUpdate is always called
					mImage = Toolkit.getDefaultToolkit().getImage(FileHelper.getCurrentDirectory()
						   + File.separator + "images" + File.separator + mFileName);
                    mImageIsLoading = true;
					mImageAvailable = true;
					mOffG.setColor(Color.black);
					mOffG.drawString("image loading...", 4, theSize.height - 4);
				    mFileNameChanged = false;
                    }

				if (mImageAvailable) {
					mImageWidth = mImage.getWidth(this);
					mImageHeight = mImage.getHeight(this);
					if (mImageWidth == -1 || mImageHeight == -1)
						mImageAvailable = false;

					if (mImageWidth > 0 && mImageHeight > 0) {
						if (mSelectionRect == null) {
							double factor = 1.0;
							if (mImageWidth > theSize.width || mImageHeight > theSize.height) {
								double factorX = (double)theSize.width/(double)mImageWidth;
								double factorY = (double)theSize.height/(double)mImageHeight;
								factor = (factorX < factorY) ? factorX : factorY;
								}

							int displayWidth = (int)((double)mImageWidth * factor);
							int displayHeight = (int)((double)mImageHeight * factor);
							mImageX1 = (theSize.width - displayWidth) / 2;
							mImageY1 = (theSize.height - displayHeight) / 2;
							mImageX2 = mImageX1 + displayWidth;
							mImageY2 = mImageY1 + displayHeight;
							mOffG.drawImage(mImage, mImageX1, mImageY1, displayWidth, displayHeight, this);
							}
						else {
							int selectionWidth = mSourceX2 - mSourceX1;
							int selectionHeight = mSourceY2 - mSourceY1;
							double factor = 1.0;
							if (selectionWidth > theSize.width || selectionHeight > theSize.height) {
								double factorX = (double)theSize.width/(double)selectionWidth;
								double factorY = (double)theSize.height/(double)selectionHeight;
								factor = (factorX < factorY) ? factorX : factorY;
								}
							int displayWidth = (int)((double)selectionWidth * factor);
							int displayHeight = (int)((double)selectionHeight * factor);
							mImageX1 = (theSize.width - displayWidth) / 2;
							mImageY1 = (theSize.height - displayHeight) / 2;
							mImageX2 = mImageX1 + displayWidth;
							mImageY2 = mImageY1 + displayHeight;
							mOffG.drawImage(mImage, mImageX1, mImageY1, mImageX2, mImageY2,
										    mSourceX1, mSourceY1, mSourceX2, mSourceY2, this);
							}
						}
					}
				else {
					mOffG.setColor(Color.red);
					mOffG.drawString("image not found", 4, theSize.height - 4);
					}
				}
			mOffImageValid = true;
			}

		g.drawImage(mOffImage, 0, 0, null);
		if (mSelecting) {
		    g.setColor(Color.red);
			g.drawRect(mSelectionRect.x, mSelectionRect.y, mSelectionRect.width, mSelectionRect.height);
			}
		}

	public void update(Graphics g) {
		paint(g);
		}

	public boolean imageUpdate(Image img, int flags, int x, int y, int width, int height) {
		if (((ImageObserver.ALLBITS
			| ImageObserver.ERROR
			| ImageObserver.ABORT) & flags) != 0) {

		    mImageAvailable = ((ImageObserver.ALLBITS & flags) != 0);

            mImageIsLoading = false;
			mOffImageValid = false;
			repaint();
		    return false;
			}

		return true;
		}

	public void setFileName(String fileName) {
		if (fileName != mFileName) {
			mFileName = fileName;
   			mFileNameChanged = true;

			mOffImageValid = false;
			mSelectionRect = null;
			repaint();
			}
		}

	public void mouseClicked(MouseEvent e) {}

	public void mousePressed(MouseEvent e) {
		mMouseX = e.getX();
		mMouseY = e.getY();
		if (mSelectionRect == null) {
			mSelecting = mImageAvailable;
			if (mMouseX < mImageX1)
				mMouseX = mImageX1;
			if (mMouseX > mImageX2)
				mMouseX = mImageX2;
			if (mMouseY < mImageY1)
				mMouseY = mImageY1;
			if (mMouseY > mImageY2)
				mMouseY = mImageY2;
			}
		else {
			mSelectionRect = null;
			mOffImageValid = false;
			repaint();
			}
		}

	public void mouseReleased(MouseEvent e) {
		if (mSelecting) {
			if (mSelectionRect.width > 4 && mSelectionRect.height > 4) {
				mSourceX1 = (int)((double)(mImageWidth * (mSelectionRect.x - mImageX1))
								/ (double)(mImageX2 - mImageX1));
				mSourceY1 = (int)((double)(mImageHeight * (mSelectionRect.y - mImageY1))
								/ (double)(mImageY2 - mImageY1));
				mSourceX2 = (int)((double)(mImageWidth
							      * (mSelectionRect.x + mSelectionRect.width - mImageX1))
								/ (double)(mImageX2 - mImageX1));
				mSourceY2 = (int)((double)(mImageHeight
							      * (mSelectionRect.y + mSelectionRect.height - mImageY1))
								/ (double)(mImageY2 - mImageY1));
				}
			else
				mSelectionRect = null;

		    mSelecting = false;
			mOffImageValid = false;
			repaint();
			}
		}

	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}

	public synchronized void mouseMoved(MouseEvent e) {}

	public synchronized void mouseDragged(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		if (x < mImageX1)
			x = mImageX1;
		if (x > mImageX2)
			x = mImageX2;
		if (y < mImageY1)
			y = mImageY1;
		if (y > mImageY2)
			y = mImageY2;

		if (mSelecting) {
			int width = Math.abs(mMouseX - x);
			int height = Math.abs(mMouseY - y);
	    	mSelectionRect = new Rectangle((mMouseX < x) ? mMouseX : x,
										   (mMouseY < y) ? mMouseY : y,
										   width, height);
			repaint();
			}
		}
	}
