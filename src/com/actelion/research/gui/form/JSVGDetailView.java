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

import com.actelion.research.util.Platform;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.print.PrintTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.svg.SVGDocument;

import javax.swing.border.Border;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class JSVGDetailView extends JResultDetailView {
    private static final long serialVersionUID = 0x20070509;

    public static final String TYPE_IMAGE_SVG = "image/svg";

    private static final String COPY_IMAGE_WINDOWS = "Copy Image";
    private static final String COPY_IMAGE_MAC_LINUX = "Copy/Save Image...";
    private static final String COPY_SVG = "Copy SVG";
    private byte[] mData;

    public JSVGDetailView(ReferenceResolver referenceResolver, ResultDetailPopupItemProvider popupItemProvider, RemoteDetailSource detailSource) {
		super(referenceResolver, popupItemProvider, detailSource, new JSVGCanvas() {
            private static final long serialVersionUID = 0x20070509;
            public void setBorder(Border border) {
                if (border instanceof FormObjectBorder)
                    super.setBorder(border);
                }
            } );
        if (Platform.isWindows())
            addPopupItem(COPY_IMAGE_WINDOWS);
        else
            addPopupItem(COPY_IMAGE_MAC_LINUX);

        addPopupItem(COPY_SVG);
        }

    @Override
	public boolean hasOwnPopupMenu() {
    	return false;
    	}

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(COPY_SVG)) {
            StringSelection theData = new StringSelection(new String(mData));
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(theData, theData);
            }
        else if (e.getActionCommand().equals(COPY_IMAGE_MAC_LINUX)
              || e.getActionCommand().equals(COPY_IMAGE_WINDOWS)) {
            // Here we should find the natural bounds of the SVG for a correct aspect ratio.

            // the document size is the original canvas size and doesn't change when resizing 
            // Dimension2D size = ((JSVGCanvas)mDetailView).getSVGDocumentSize();

            // the canvas bounds change with resizing of the canvas
            Rectangle bounds = ((JSVGCanvas)mDetailView).getBounds();

            // the viewbox is usually null and cannot be used as natural size of SVG
            // ((JSVGCanvas)mDetailView).getSVGDocument().getRootElement().getViewBox();


/* this doesn't work, therefore do the same as on all platforms
           if (e.getActionCommand().equals(COPY_IMAGE_WINDOWS)) {
                WMF wmf = new WMF();
                wmf.setWindowOrg(0, 0);
                wmf.setWindowExt(bounds.width, bounds.height);
                wmf.setViewportOrg(bounds.x, bounds.y);
                wmf.setViewportExt(bounds.width, bounds.height);
                WMFGraphics2D g = new WMFGraphics2D(wmf, bounds.width, bounds.height, Color.black, Color.white);
    
                mDetailView.paint(g);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try {
                    wmf.writeWMF(out);
                    out.close();
                    NativeClipboardHandler.copyMetaFile(out.toByteArray());
                    }
                catch (IOException ioe) {
                    ioe.printStackTrace();
                    }
                }
            else {  // Linux or Macintosh
*/
                final int width = bounds.width;
                final int height = bounds.height;
/* With the correct aspect ration we should calculate height automatically:
                final double factor = (double)height / (double)width;
                new JComponentCopyDialog(null, mDetailView, "Create image from SVG", true) {
                    private static final long serialVersionUID = 0x20080407;
                    public int calculateImageHeight(int width) {
                        return (int)(factor*width);
                        }
                    public Dimension getDefaultImageSize() {
                        return new Dimension(width, height);
                        }
                    };*/

// Now we allow free selection of width and height instead
                Component c = getParent();
                while (!(c instanceof Frame))
                    c = c.getParent();
                new JComponentCopyDialog((Frame)c, mDetailView, "Create image from SVG", false) {
                    private static final long serialVersionUID = 0x20080407;
                    public Dimension getDefaultImageSize() {
                        return new Dimension(width, height);
                        }
                    public Image createComponentImage(int width, int height) {
                        return createImageFromSVG(width, height);
                        }
                    };
//              }
            }
        else {
            super.actionPerformed(e);
            }
        }

    @Override
    protected void setDetailData(Object data) {
        mData = (byte[])data;
		if (data == null) {
			((JSVGCanvas)mDetailView).setSVGDocument(null);
			return;
			}

		org.w3c.dom.Document doc = null;
        try {
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
            doc = f.createDocument(null, new ByteArrayInputStream((byte[])data));
	        }
		catch (java.io.IOException ex) {
            ex.printStackTrace();
        	}
        ((JSVGCanvas)mDetailView).setSVGDocument((SVGDocument)doc);
		}

    public Image createImageFromSVG(int width, int height) {
        BufferedImageTranscoder t = new BufferedImageTranscoder();
        if (width != 0 && height != 0)
            t.setDimensions(width, height);

        InputStream is = new ByteArrayInputStream(mData);
        TranscoderInput ti = new TranscoderInput(is);
        try {
            t.transcode(ti, null);
            }
        catch (TranscoderException te) {
            return null;
            }

        return t.getBufferedImage();
        }

    @Override
	public void print(Graphics g, Rectangle2D.Double r, float scale, Object data) {
        try {
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
            org.w3c.dom.Document doc = f.createDocument(null, new ByteArrayInputStream((byte[])data));

            Graphics2D g2d = (Graphics2D)g;
            PrintTranscoder transcoder = new PrintTranscoder();
            TranscoderInput ti = new TranscoderInput(doc);
            transcoder.addTranscodingHint(PrintTranscoder.KEY_WIDTH, new Float(r.width));
            transcoder.addTranscodingHint(PrintTranscoder.KEY_HEIGHT, new Float(r.height));
            transcoder.addTranscodingHint(PrintTranscoder.KEY_MARGIN_BOTTOM, new Float(1.0));
            transcoder.addTranscodingHint(PrintTranscoder.KEY_MARGIN_TOP, new Float(1.0));
            transcoder.addTranscodingHint(PrintTranscoder.KEY_MARGIN_LEFT, new Float(1.0));
            transcoder.addTranscodingHint(PrintTranscoder.KEY_MARGIN_RIGHT, new Float(1.0));
            transcoder.transcode(ti, null);

            PageFormat pageFormat = new PageFormat();
            Paper paper= new Paper();
            paper.setSize(r.x+r.width, r.y+r.height);
            paper.setImageableArea(r.x, r.y, r.width, r.height);
            pageFormat.setPaper(paper);

            transcoder.print(g2d, pageFormat, 0);
        	}
        catch (Exception pe) {
            pe.printStackTrace();
        	}
		}
	}
