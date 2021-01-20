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

import com.actelion.research.gui.hidpi.HiDPIHelper;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;

public class FormObjectBorder extends AbstractBorder {
	private static final long serialVersionUID = 0x20090731;
/*
	private static final Color TITLE_BACKGROUND = UIManager.getColor("TextArea.inactiveBackground");
	private static final Color TITLE_FOREGROUND = UIManager.getColor("TextArea.foreground");
	private static final Color BORDER_COLOR = ColorHelper.intermediateColor(TITLE_BACKGROUND, Color.BLUE, 0.3f);
	private static final Color EDIT_BORDER_COLOR = ColorHelper.getContrastColor(Color.yellow, TITLE_BACKGROUND);
*/
	private static final int BORDER = 1;
	private static final int MIN_CONTENT_HEIGHT = 12;
	private String mTitle;
	private Font mFont;
	private FontMetrics mMetrics;
	private boolean mIsEditMode;

	public FormObjectBorder(String title, Font font) {
		mTitle = title;
		mFont = font.deriveFont(Font.BOLD);
		}

	public void setFont(Font font) {
		mFont = font.deriveFont(Font.BOLD);
		mMetrics = null;
		}

	public Insets getBorderInsets(Component c) {
		if (c.getGraphics() == null)
			return new Insets(0, 0, 0, 0);

		int minContentHeight = HiDPIHelper.scale(MIN_CONTENT_HEIGHT);

		Dimension size = c.getSize();
		Insets insets;
		if (mTitle == null) {
			if (size.height >= minContentHeight + 2*BORDER)
				insets = new Insets(BORDER, BORDER, BORDER, BORDER);
			else
				insets = new Insets(0, 0, 0, 0);
			}
		else {
			if (mMetrics == null)
				mMetrics = c.getGraphics().getFontMetrics(mFont);

			int titleWidth = (int)mMetrics.getStringBounds(" "+mTitle+": ", c.getGraphics()).getWidth();
			
			if (size.height >= minContentHeight + 2*BORDER + mMetrics.getHeight())
				insets = new Insets(BORDER+mMetrics.getHeight(), BORDER, BORDER, BORDER);
			else if (size.height >= minContentHeight + 2 * BORDER)
				insets = new Insets(BORDER, Math.min(size.width/2, BORDER+titleWidth), BORDER, BORDER);
			else
				insets = new Insets(0, Math.min(size.width/2, titleWidth), 0, 0);
			}

		return insets;
		}

	public Insets getBorderInsets(Component c, Insets insets) {
		return getBorderInsets(c);
		}

	public String getTitle() {
		return mTitle;
		}
	
	public void setTitle(String title) {
		mTitle = title;
		}

	public void setEditMode(boolean b) {
		mIsEditMode = b;
		}

	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		// switch off antialiasing and sub-pixel accuracy for the border, which some implementation may have used
		((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		((Graphics2D)g).setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
		((Graphics2D)g).setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);

		Color titleBackground = UIManager.getColor("TextArea.inactiveBackground");
		Color titleForeground = UIManager.getColor("TextArea.foreground");
//		Color borderColor = ColorHelper.intermediateColor(titleBackground, Color.BLUE, 0.3f);
//		Color editBorderColor = ColorHelper.getContrastColor(Color.yellow, titleBackground);

		Insets insets = getBorderInsets(c);
		if (mTitle != null) {
			String title = " "+mTitle+": ";
			if (insets.top > BORDER) {	// title on top
				g.setColor(titleBackground);
				g.fillRect(x+BORDER, y+BORDER, width-2*BORDER, insets.top-BORDER);
				g.setColor(titleForeground);
				g.setFont(mFont);
				g.drawString(title, x, y+BORDER+mMetrics.getAscent());
				}
			else {	// title on left side
				g.setColor(titleBackground);
				g.fillRect(x+insets.top, y+insets.top, insets.left-insets.top, height-2*insets.top);
				g.setColor(titleForeground);
				g.setFont(mFont);
				int titleWidth = (int)mMetrics.getStringBounds(title, g).getWidth();
				g.drawString(title, x+insets.left-titleWidth, y+insets.top+mMetrics.getAscent());
				}
			}
//		if (insets.top != 0) {
//			g.setColor(mIsEditMode ? editBorderColor : borderColor);
//			g.drawRect(x, y, width-1, height-1);
//			}
		}

	public void printBorder(Graphics2D g2D, Rectangle2D.Double rect, float scale) {
		Color titleBackground = new Color(224,224,224);
		Color borderColor = new Color(208,208,208);;

		g2D.setColor(borderColor);
		float lineWidth = scale;
		g2D.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
		shrink(rect, lineWidth/2);
		g2D.draw(rect);
		shrink(rect, lineWidth/2);

		g2D.setFont(mFont.deriveFont(Font.PLAIN, scale*mFont.getSize2D()+0.5f));
		FontMetrics metrics = g2D.getFontMetrics();
 		GlyphVector title = g2D.getFont().createGlyphVector(g2D.getFontRenderContext(), " "+mTitle+": ");
		Rectangle2D.Double titleRect = (Rectangle2D.Double)rect.clone();
		if (rect.height >= 2*metrics.getHeight()) {
			titleRect.height = metrics.getHeight();
			rect.y += metrics.getHeight();
			rect.height -= metrics.getHeight();
			g2D.setColor(titleBackground);
			g2D.fill(titleRect);
			g2D.setColor(Color.black);
			}
		else {
			Rectangle2D titleBounds = title.getLogicalBounds();
			titleRect.width = Math.min(titleRect.width/2f, (float)titleBounds.getWidth());
			rect.x += titleRect.width;
			rect.width -= titleRect.width;
			g2D.setColor(titleBackground);
			g2D.fill(titleRect);
			g2D.setColor(Color.black);
			}
		Shape clip = g2D.getClip();
		g2D.setClip(titleRect);
		g2D.drawGlyphVector(title, (float)titleRect.x, (float)titleRect.y+metrics.getAscent());
		g2D.setClip(clip);
		}

	private void shrink(Rectangle2D.Double r, float d) {
		r.x += d;
		r.y += d;
		r.width -= 2*d;
		r.height -= 2*d;
		}
	}
