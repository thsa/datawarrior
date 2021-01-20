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

package com.actelion.research.datawarrior.task;

import com.actelion.research.util.ColorHelper;

import javax.swing.*;
import java.awt.*;

public class DEColorPanel extends JPanel {
	private static final long serialVersionUID = 0x20110427;
	private Color mOriginalColor,mColor;

	public DEColorPanel(Color c) {
		super();
		mOriginalColor = mColor = c;
	}

	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Color c = mColor;
		if (!isEnabled()) {
			float b = ColorHelper.perceivedBrightness(c);
			c = new Color(b, b, b);
			}
		g.setColor(c);
		g.fillRoundRect(1, 1, getWidth()-2, getHeight()-2, 2, 2);
		g.setColor(Color.GRAY);
		g.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 6, 6);
	}

	public Color getColor() {
		return mColor;
	}

	public void setColor(Color c) {
		mColor = c;
		repaint();
	}

	public Color getOriginalColor() {
		return mOriginalColor;
	}
}
