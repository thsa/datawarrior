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

import com.actelion.research.gui.ImageDataSource;
import com.actelion.research.gui.JImagePanel;

import javax.swing.border.Border;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ImageObserver;

public class JImageDetailView extends JResultDetailView implements ImageObserver,ImageDataSource {
    private static final long serialVersionUID = 0x20070509;

    public static final String TYPE_IMAGE_JPEG = "image/jpeg";
	public static final String TYPE_IMAGE_GIF = "image/gif";
    public static final String TYPE_IMAGE_PNG = "image/png";
	public static final String TYPE_IMAGE_FROM_PATH = "zoomableImage";

	private Image mPrintImage;
	private int mImageStatus;
	
	public JImageDetailView(final ReferenceResolver referenceResolver,
							final ResultDetailPopupItemProvider popupItemProvider,
							final RemoteDetailSource detailSource) {
		super(referenceResolver, popupItemProvider, detailSource, new JImagePanel(null, true) {
			private static final long serialVersionUID = 20120502L;

			@Override
			public void setBorder(Border border) {
				if (border instanceof FormObjectBorder)
					super.setBorder(border);
				}
			} );

		((JImagePanel)mDetailView).setHighResolutionImageSource(this);
		((JImagePanel)mDetailView).setPopupItemProvider(this);
		mDetailView.setBackground(Color.white);
		}

    @Override
	public boolean hasOwnPopupMenu() {
    	return true;
    	}

    @Override
	protected void setDetailData(Object data) {
		if (data instanceof String)
			((JImagePanel)mDetailView).setFileName((String)data);
		else
			((JImagePanel)mDetailView).setImageData((byte[])data);
		}

	public void setImagePath(String imagePath) {
		((JImagePanel)mDetailView).setImagePath(imagePath);
		}

	public void setUseThumbNail(boolean value) {
		((JImagePanel)mDetailView).setUseThumbNail(value);
		}

    @Override
	public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
	    mImageStatus = infoflags;
	    return ((mImageStatus & (ImageObserver.ALLBITS | ImageObserver.ERROR | ImageObserver.ABORT)) == 0);
		}
	
    @Override
	public void print(Graphics g, Rectangle2D.Double r, float scale, Object data) {
	    boolean usesThumbNail = ((JImagePanel)mDetailView).usesThumbNail();
	    String path = ((JImagePanel)mDetailView).getImagePath();
	    boolean imageReady = false;
	    if (data instanceof String) {
			String filePath = JImagePanel.buildImagePath(path, (String)data, usesThumbNail);
			mPrintImage = Toolkit.getDefaultToolkit().createImage(filePath);
			imageReady = Toolkit.getDefaultToolkit().prepareImage(mPrintImage, -1, -1, this);
			}
		else {
		    mPrintImage = Toolkit.getDefaultToolkit().createImage((byte[])data);
		    imageReady = Toolkit.getDefaultToolkit().prepareImage(mPrintImage, -1, -1, this);
			}

	    if (!imageReady) {
	        mImageStatus = 0;
	        while ((mImageStatus & (ImageObserver.ALLBITS | ImageObserver.ERROR | ImageObserver.ABORT)) == 0)
	            try { Thread.sleep(1); } catch (InterruptedException ie) {}
	        imageReady = ((mImageStatus & ImageObserver.ALLBITS) != 0);
	    	}
	            
	    Graphics2D g2d = (Graphics2D)g;
	    if (imageReady) {
		    AffineTransform transform = new AffineTransform();

		    double scaling = 1.0;
		    if (r.width < mPrintImage.getWidth(null)
		     || r.height < mPrintImage.getHeight(null))
		        scaling = Math.min(r.width/mPrintImage.getWidth(null),
		                			r.height/mPrintImage.getHeight(null));
		    
		    double dx = (r.width - scaling*mPrintImage.getWidth(null))/2;
		    double dy = (r.height - scaling*mPrintImage.getHeight(null))/2;
		    transform.translate(r.x+dx, r.y+dy);

		    if (scaling != 1.0)
		        transform.scale(scaling, scaling);

		    g2d.setClip(r);
		    g2d.drawImage(mPrintImage, transform, this);
	    	}
	    else {
	        g2d.setColor(Color.RED);
	        g2d.setFont(g2d.getFont().deriveFont(0, (int)(9*scale+0.5)));
	        Rectangle2D bounds = g2d.getFontMetrics().getStringBounds("error", g2d);
	        g2d.drawString("error", (float)(r.x+r.width/2-bounds.getWidth()/2),	(float)(r.y+r.height/2));
	    	}
	    }

	@Override
	public byte[] getImageData() {
    	return getReferenceResolver().resolveReference(getDetailSource(), getCurrentReference(), ReferenceResolver.MODE_FULL_IMAGE);
		}
	}
