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
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

public class JTextFormObject extends AbstractFormObject implements FocusListener {
	public static final int SINGLE_LINE_HEIGHT = 1;
	public static final int MULTI_LINE_HEIGHT = 2;

	// Rough factor to control tooltip width assuming that average character width is 50% of its height.
	private static final float TOOLTIP_WIDTH_FACTOR = 2;    // Use 2 for tooltips to be about as wide as the textarea

	private String mCurrentText;

	public JTextFormObject(String key, String type) {
		super(key, type);
		
		mComponent = new JTextArea() {
			private static final long serialVersionUID = 0x20070509;

			@Override
			public void setBorder(Border border) {
				if (border instanceof FormObjectBorder)
					super.setBorder(border);
				}

			@Override
			public Point getToolTipLocation(MouseEvent e) {
//				Point p1 = getParent().getLocation();    // viewport location in scrollpane (includes border insets)
//				Point p2 = getLocation();    // text field in viewport (negative y values in case of scrolled down text)
//				return new Point(-p1.x - p2.x, -p1.y - p2.y);
				return new Point(0, 0);
				}

			@Override
			public String getToolTipText(MouseEvent e) {
				return createToolTipText();
				}
			};
		mComponent.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, mComponent.getFont().getSize()));
		mComponent.addFocusListener(this);
		((JTextArea)mComponent).setEditable(false);
		((JTextArea)mComponent).setLineWrap(true);
		((JTextArea)mComponent).setWrapStyleWord(true);

		// set back to default behaviour, which is "TAB advances to next focusable item"
		mComponent.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
		mComponent.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

		mComponent.setToolTipText("");	// to switch on tool-tips
		}

	public void focusGained(FocusEvent e) {
		mCurrentText = ((JTextArea)mComponent).getText();
		}

	public void focusLost(FocusEvent e) {
		if (!((JTextArea)mComponent).getText().equals(mCurrentText))
			fireDataChanged();
		}
	
	public Object getData() {
		return ((JTextArea)mComponent).getText();
		}

	public void setData(Object data) {
		((JTextArea)mComponent).setText((String)data);
		}

	public void setEditable(boolean b) {
		super.setEditable(b);
		((JTextArea)mComponent).setEditable(b);
		}

	public int getDefaultRelativeHeight() {
		return (mType.equals(FormObjectFactory.TYPE_MULTI_LINE_TEXT)) ? MULTI_LINE_HEIGHT : SINGLE_LINE_HEIGHT;
		}

	public void setFont(Font font) {
		super.setFont(font);
		mComponent.setFont(font);
		}

	public void printContent(Graphics2D g2D, Rectangle2D.Double r, float scale, Object data, boolean isMultipleRows) {
		if (data != null) {
			if (mPrintBackground != null) {
				g2D.setColor(mPrintBackground);
				g2D.fill(r);
				}
			g2D.setColor(mPrintForeground != null ? mPrintForeground : Color.BLACK);
			g2D.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (int)(mComponent.getFont().getSize2D()*scale+0.5)));
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

	private String createToolTipText() {
		if (((JTextArea)mComponent).isEditable())
			return null;

		StringBuilder sb = new StringBuilder();
		sb.append("<html>");

		Border border = getBorder();
		if (border != null && border instanceof FormObjectBorder) {
			sb.append("<b>");
			sb.append(((FormObjectBorder)border).getTitle()+":");
			sb.append("</b><br>");
			}

		String text = ((JTextArea)mComponent).getText();
		int width = mComponent.getWidth();   // scrollpane width
		int maxCharsPerLine = (int)(TOOLTIP_WIDTH_FACTOR * width / mComponent.getFont().getSize());

		int index1 = 0;
		while (index1 < text.length()) {
			// skip white space
			while (index1<text.length() && Character.isWhitespace(text.charAt(index1)))
				index1++;

			int index2 = index1+1;
			while (index2<text.length()
					&& text.charAt(index2) != '\n'
					&& index2 - index1 < maxCharsPerLine)
				index2++;

			// if possible go back a few chars to break line at white space
			if (index2<text.length()
			 && !Character.isWhitespace(text.charAt(index2))) {
				int index = index2;
				int minChars = Math.max(5, maxCharsPerLine*2/3); // not less than 5 chars per line or 2/3 of max length
				while (index > index1+minChars
					&& !Character.isWhitespace(text.charAt(index)))
					index--;

				if (Character.isWhitespace(text.charAt(index))) {    // white space found within acceptable range
					while (index > index1+minChars
						&& Character.isWhitespace(text.charAt(index-1)))
						index--;
					index2 = index;
					}
				}

			sb.append(text, index1, index2);
			sb.append("<br>");

			index1 = index2;
			}

		sb.append("</html>");
		return sb.toString();
		}
	}
