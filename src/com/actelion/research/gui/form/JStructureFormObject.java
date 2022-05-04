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

import com.actelion.research.chem.AbstractDepictor;
import com.actelion.research.chem.Depictor2D;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.gui.JEditableStructureView;
import com.actelion.research.gui.JStructureView;
import com.actelion.research.gui.StructureListener;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.gui.generic.GenericRectangle;

import javax.swing.border.Border;
import java.awt.*;
import java.awt.geom.Rectangle2D;

public class JStructureFormObject extends AbstractFormObject implements StructureListener {
	public JStructureFormObject(String key, String type) {
		super(key, type);
		mComponent = new JEditableStructureView() {
            private static final long serialVersionUID = 0x20070509;
            public void setBorder(Border border) {
                if (border instanceof FormObjectBorder)
                    super.setBorder(border);
                }
            };
        ((JEditableStructureView)mComponent).setEditable(false);
		((JEditableStructureView)mComponent).setClipboardHandler(new ClipboardHandler());
        ((JEditableStructureView)mComponent).addStructureListener(this);
		}

	public void setEditable(boolean b) {
	    super.setEditable(b);
        ((JEditableStructureView)mComponent).setEditable(b);
	    }

	public Object getData() {
		return ((JStructureView)mComponent).getMolecule();
		}

	public void setData(Object data) {
	    ((JStructureView)mComponent).removeStructureListener(this);
		if (data == null)
			((JStructureView)mComponent).structureChanged(null);
		else if (data instanceof StereoMolecule)
			((JStructureView)mComponent).structureChanged((StereoMolecule)data);
		else if (data instanceof String) {
			String idcode = (String)data;
			int index = idcode.indexOf('\t');
			if (index == -1)
				((JStructureView)mComponent).setIDCode(idcode);
			else	// idcode and coordinates TAB delimited
				((JStructureView)mComponent).setIDCode(idcode, idcode.substring(index+1));
			}
        ((JStructureView)mComponent).addStructureListener(this);
		}

	public int getDefaultRelativeHeight() {
		return 4;
		}

	public void structureChanged(StereoMolecule mol) {
	    fireDataChanged();
	    }

	public void printContent(Graphics2D g2D, Rectangle2D.Double r, float scale, Object data, boolean isMultipleRows) {
        if (data != null) {
	        StereoMolecule mol = null;
			if (data instanceof StereoMolecule) {
				mol = (StereoMolecule)data;
				}
			else if (data instanceof String) {
				String idcode = (String)data;
				int index = idcode.indexOf('\t');
				if (index == -1)
					mol = new IDCodeParser(true).getCompactMolecule(idcode);
				else	// idcode and coordinates TAB delimited
				    mol = new IDCodeParser(true).getCompactMolecule(idcode, idcode.substring(index+1));
				}
	
		    if(mol != null) {
				if (mPrintBackground != null) {
					g2D.setColor(mPrintBackground);
					g2D.fill(r);
					}

				Depictor2D d = new Depictor2D(mol);
				if (mPrintForeground != null)
					d.setOverruleColor(mPrintForeground, mPrintBackground);
				d.validateView(g2D, new GenericRectangle(r.x, r.y, r.width, r.height), AbstractDepictor.cModeInflateToMaxAVBL+(int)(24*scale));
				d.paint(g2D);
				}
        	}
		}
	}
