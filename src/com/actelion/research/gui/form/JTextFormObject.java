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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.geom.Rectangle2D;

public class JTextFormObject extends AbstractFormObject implements FocusListener {
	public static final int SINGLE_LINE_HEIGHT = 1;
	public static final int MULTI_LINE_HEIGHT = 2;

	private String mCurrentText;
	private JTextArea mTextArea;

	public JTextFormObject(String key, String type) {
		super(key, type);
		
		mTextArea = new JTextArea() {
			private static final long serialVersionUID = 0x20070509;
			public void setBorder(Border border) {
				if (border instanceof FormObjectBorder)
					super.setBorder(border);
				}
			};
		mTextArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, mTextArea.getFont().getSize()));
		mTextArea.addFocusListener(this);
		mTextArea.setEditable(false);
		mTextArea.setLineWrap(true);
		mTextArea.setWrapStyleWord(true);

		// set back to default behaviour, which is "TAB advances to next focusable item"
		mTextArea.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
		mTextArea.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
		mComponent = new JScrollPane(mTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		}

	public void focusGained(FocusEvent e) {
		mCurrentText = mTextArea.getText();
		}

	public void focusLost(FocusEvent e) {
		if (!mTextArea.getText().equals(mCurrentText))
			fireDataChanged();
		}
	
	public Object getData() {
		return mTextArea.getText();
		}

	public void setData(Object data) {
		mTextArea.setText((String)data);
		}

	public void setEditable(boolean b) {
		super.setEditable(b);
		mTextArea.setEditable(b);
		}

	public int getDefaultRelativeHeight() {
		return (mType.equals(FormObjectFactory.TYPE_MULTI_LINE_TEXT)) ? MULTI_LINE_HEIGHT : SINGLE_LINE_HEIGHT;
		}

	public void setFont(Font font) {
		super.setFont(font);
		mTextArea.setFont(font);
		}

	public void printContent(Graphics2D g2D, Rectangle2D.Double r, float scale, Object data, boolean isMultipleRows) {
		if (data != null) {
			if (mPrintBackground != null) {
				g2D.setColor(mPrintBackground);
				g2D.fill(r);
				}
			g2D.setColor(mPrintForeground != null ? mPrintForeground : Color.BLACK);
			g2D.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (int)(mTextArea.getFont().getSize2D()*scale+0.5)));
			FontMetrics metrics = g2D.getFontMetrics();
			double height = metrics.getHeight();
			double border = metrics.getStringBounds(" ", g2D).getWidth();
			double x = r.x + border;
			double y = r.y + metrics.getAscent();
			String text = (String)data;
			g2D.setClip(r);
			if (mType.equals(FormObjectFactory.TYPE_MULTI_LINE_TEXT)) {
				byte[] bytes = text.getBytes();
				double maxWidth = r.width - 2*border;
				int index = 0;
				while (index < bytes.length) {
					int len = 0;
					while (index+len<bytes.length
						&& bytes[index+len] != '\n'
						&& metrics.bytesWidth(bytes, index, len+1)<=maxWidth)
						len++;
					g2D.drawString(text.substring(index, index+len), (float)x, (float)y);
					y += height;
					index += len;
					if (index<bytes.length && bytes[index]=='\n')
						index++;
					}
				}
			else {
				g2D.drawString(text, (float)x, (float)y);
				}
			g2D.setClip(null);
			}
		}
	}
