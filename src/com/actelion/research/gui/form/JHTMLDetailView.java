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

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public class JHTMLDetailView extends JResultDetailView {
    private static final long serialVersionUID = 0x20110912;

    public static final String TYPE_TEXT_HTML = "text/html";
	public static final String TYPE_TEXT_PLAIN = "text/plain";

    private static final String COPY_TEXT = "Copy";

	private JEditorPane mEditorPane;

	public JHTMLDetailView(ReferenceResolver referenceResolver,
						   final ResultDetailPopupItemProvider popupItemProvider,
						   RemoteDetailSource detailSource, String mimetype) {
		super(referenceResolver, popupItemProvider, detailSource,
					new JScrollPane(new JEditorPane() {
                        private static final long serialVersionUID = 0x20070509;
                        public void setBorder(Border border) {
                            if (border instanceof FormObjectBorder)
                                super.setBorder(border);
                            }
                        },
					                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
									JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
		mEditorPane = (JEditorPane)((JScrollPane)mDetailView).getViewport().getView();
		mEditorPane.setEditable(false);
		mEditorPane.setContentType(mimetype);
		((JScrollPane)mDetailView).setBorder(null);
        addPopupItem(COPY_TEXT);
		}

    @Override
	public boolean hasOwnPopupMenu() {
    	return false;
    	}

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(COPY_TEXT)) {
            StringSelection text = new StringSelection(mEditorPane.getText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(text, text);
            }
        else {
            super.actionPerformed(e);
            }
        }

    @Override
	public void setDetailData(Object data) {
		String text = (data == null) ? "" : new String((byte[])data, 0, ((byte[])data).length);
		mEditorPane.setText(text);
		}

    @Override
	public void print(Graphics g, Rectangle2D.Double r, float scale, Object data) {
	    final double internalScale = 0.75;
	    
	    if (data == null)
		    return;

		String text = new String((byte[])data, 0, ((byte[])data).length);

		JEditorPane jeditorPane = new JEditorPane() {
		    private static final long serialVersionUID = 0x20070509;
            public void setBorder(Border border) {
                if (border instanceof FormObjectBorder)
                    super.setBorder(border);
                }
            };
	    jeditorPane.setContentType(mEditorPane.getContentType());
	    jeditorPane.setText(text);

	    scale *= internalScale;

	    Graphics2D graphics2D = (Graphics2D)g;

	    jeditorPane.setSize((int)(r.width/scale), (int)(r.height/scale));
	    jeditorPane.validate();

//	    View rootView = jeditorPane.getUI().getRootView(jeditorPane);
	    AffineTransform originalTransform = graphics2D.getTransform();

	    graphics2D.setColor(Color.BLACK);
	    graphics2D.setClip(r);
	    graphics2D.translate(r.x, r.y);
	    graphics2D.scale(scale, scale);

	    jeditorPane.paint(graphics2D);

/*	    Rectangle allocation = new Rectangle(0, 0,
	            	(int)(jeditorPane.getMinimumSize().getWidth()),
	            	(int)(jeditorPane.getPreferredSize().getHeight()));

	    printView(graphics2D, allocation, rootView);
*/
	    graphics2D.setTransform(originalTransform);
		}

/*	private void printView(Graphics2D graphics2D, Shape allocation, View view) {
	    if (view.getViewCount() > 0 && !view.getElement().getName().equalsIgnoreCase("td")) {
	        for (int i=0; i<view.getViewCount(); i++) {
	            Shape childAllocation = view.getChildAllocation(i,allocation);
	            if (childAllocation != null) {
	                View childView = view.getView(i);
	                printView(graphics2D,childAllocation,childView);
		            }
	        	}
		    }
	    else {
		    Rectangle clipRectangle = graphics2D.getClipBounds();
	        if (allocation.getBounds().getMaxY() >= clipRectangle.getY()) {
	            if ((allocation.getBounds().getHeight() > clipRectangle.getHeight()) &&
	                (allocation.intersects(clipRectangle))) {
	                view.paint(graphics2D, allocation);
	            	}
	            else if (allocation.getBounds().getY() >= clipRectangle.getY()
	                  && allocation.getBounds().getMaxY() <= clipRectangle.getMaxY()) {
	                view.paint(graphics2D,allocation);
		            }
	        	}
		    }
		}*/
	}
