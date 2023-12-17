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

package com.actelion.research.datawarrior.task.list;

import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.gui.hidpi.HiDPIIcon;
import com.actelion.research.gui.swing.SwingImage;

import javax.swing.*;
import java.awt.*;

public class HitlistOptionRenderer extends JPanel implements /*ImageObserver,*/ListCellRenderer {
	private static final long serialVersionUID = 0x20130227;

	public static final String[] OPERATION_TEXT = { "logical AND",
													"logical OR",
													"logical XOR",
													"logical NOT"};
	public static final String[] OPERATION_CODE = { "and", "or", "xor", "not" };

	private static final int cItemWidth = HiDPIHelper.scale(120);
	private static final int cItemHeight = HiDPIHelper.scale(18);
	private static final int cImageWidth = HiDPIHelper.scale(24);
	private static final int cImageHeight = HiDPIHelper.scale(16);
	private static final String IMAGE_NAME = "booleanOperations.png";

	private static Image	sImage;

	private Object			mParameterValue;
	private int				mParameterIndex;
	private boolean			mIsSelected,mIsActiveItem;

	public HitlistOptionRenderer() {
		SwingImage image = HiDPIIcon.createIconImage(IMAGE_NAME);
		HiDPIIcon.adaptForLookAndFeel(image);
		sImage = (Image)HiDPIIcon.scale(image).get();

		setPreferredSize(new Dimension(cItemWidth, cItemHeight));
		}

	public void paintComponent(Graphics g) {
		if (mParameterIndex != -1) {
			Dimension theSize = getSize();
			int verticalBorder = (cItemHeight - cImageHeight) / 2;

			if (mIsSelected) {
				g.setColor(UIManager.getColor("TextArea.selectionBackground"));
				g.fillRect(0, 0, theSize.width, theSize.height);
				}
			int gap = HiDPIHelper.scale(4);
			g.setColor(UIManager.getColor("Label.foreground"));
			g.drawString((String)mParameterValue, cImageWidth+2*gap, cItemHeight-HiDPIHelper.scale(1)-verticalBorder-g.getFontMetrics().getDescent());

			g.setClip(gap, verticalBorder, cImageWidth, cImageHeight);
			if (HiDPIHelper.getRetinaScaleFactor() == 2)
				g.drawImage(sImage, gap - mParameterIndex * cImageWidth, verticalBorder, cImageWidth, cImageHeight, null);
			else
				g.drawImage(sImage, gap - mParameterIndex * cImageWidth, verticalBorder, null);
			}
		}

	public Component getListCellRendererComponent(JList list,
												  Object value,
												  int index,
												  boolean isSelected,
												  boolean cellHasFocus) {
		mIsActiveItem = false;
		if (index == -1) {
			index = list.getSelectedIndex();
			mIsActiveItem = (index != -1);
			}

		mParameterValue = value;
		mParameterIndex = index;
		mIsSelected = isSelected;
		return this;
		}
	}