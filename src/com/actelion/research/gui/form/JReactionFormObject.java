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
import com.actelion.research.chem.ExtendedDepictor;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.chem.reaction.ReactionEncoder;
import com.actelion.research.gui.JEditableChemistryView;

import javax.swing.border.Border;
import java.awt.*;
import java.awt.geom.Rectangle2D;

public class JReactionFormObject extends AbstractFormObject {
    private Object mChemistry;

	public JReactionFormObject(String key, String type) {
		super(key, type);

		mComponent = new JEditableChemistryView(ExtendedDepictor.TYPE_REACTION) {
			private static final long serialVersionUID = 0x20070509;
			public void setBorder(Border border) {
				if (border instanceof FormObjectBorder)
					super.setBorder(border);
				}
			};
		((JEditableChemistryView)mComponent).setOpaqueBackground(true);

//		((JEditableChemistryView)mComponent).setClipboardHandler(new ClipboardHandler());
		}

	public void setEditable(boolean b) {
		super.setEditable(b);
		((JEditableChemistryView)mComponent).setEditable(b);
		}

	public Object getData() {
		return mChemistry;
		}

	public void setData(Object data) {
		mChemistry = null;
		if (data != null) {
			if (data instanceof Reaction)
				mChemistry = data;
			else if (data instanceof String)
				mChemistry = ReactionEncoder.decode((String)data, true);
			}

		((JEditableChemistryView)mComponent).setContent((Reaction)mChemistry);
		}

	public int getDefaultRelativeHeight() {
		return 4;
		}

	public void printContent(Graphics2D g2D, Rectangle2D.Double r, float scale, Object data, boolean isMultipleRows) {
		if (data != null) {
			Reaction rxn = null;
			if (data instanceof Reaction)
				rxn = (Reaction)data;
			else if (data instanceof String)
				rxn = ReactionEncoder.decode((String)data, true);

			if (rxn != null) {
				ExtendedDepictor d = new ExtendedDepictor(rxn, null, rxn.isReactionLayoutRequired(), true);
				d.validateView(g2D, r, AbstractDepictor.cModeInflateToMaxAVBL);
				d.paint(g2D);
				}
			}
		}
    }
