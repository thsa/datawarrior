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

public class JFXPDBDetailView extends JResultDetailView {
		private static final long serialVersionUID = 0x20110912;

		public static final String TYPE_CHEMICAL_PDB = "chemical/x-pdb";

		private JFXPDBViewer mViewer;

		public JFXPDBDetailView(ReferenceResolver referenceResolver,
							   final ResultDetailPopupItemProvider popupItemProvider,
							   RemoteDetailSource detailSource) {
			super(referenceResolver, popupItemProvider, detailSource,
					new JFXPDBViewer() {
						private static final long serialVersionUID = 0x20170901;
						public void setBorder(Border border) {
							if (border instanceof FormObjectBorder)
								super.setBorder(border);
						}
					});
			mViewer = (JFXPDBViewer)getViewComponent();
//			addPopupItem(COPY_TEXT);
		}

		@Override
		public boolean hasOwnPopupMenu() {
			return true;
		}

		@Override
		public void setDetailData(Object data) {
			mViewer.setPDBData((byte[])data);
		}

		@Override
		public void print(Graphics g, Rectangle2D.Double r, float scale, Object data) {
			final double internalScale = 0.75;

			if (data == null)
				return;

			JTextArea viewer = new JTextArea() {
				private static final long serialVersionUID = 0x20070509;
				public void setBorder(Border border) {
					if (border instanceof FormObjectBorder)
						super.setBorder(border);
					}
			};
			viewer.setText("Not supported yet");

			scale *= internalScale;

			Graphics2D graphics2D = (Graphics2D)g;

			viewer.setSize((int)(r.width/scale), (int)(r.height/scale));
			viewer.validate();

//	    View rootView = jeditorPane.getUI().getRootView(jeditorPane);
			AffineTransform originalTransform = graphics2D.getTransform();

			graphics2D.setColor(Color.BLACK);
			graphics2D.setClip(r);
			graphics2D.translate(r.x, r.y);
			graphics2D.scale(scale, scale);

			viewer.paint(graphics2D);

/*	    Rectangle allocation = new Rectangle(0, 0,
	            	(int)(jeditorPane.getMinimumSize().getWidth()),
	            	(int)(jeditorPane.getPreferredSize().getHeight()));

	    printView(graphics2D, allocation, rootView);
*/
			graphics2D.setTransform(originalTransform);
		}
	}
