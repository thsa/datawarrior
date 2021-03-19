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
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public abstract class AbstractFormObject {
	protected String			mType;
	protected JComponent		mComponent;
	protected FormObjectBorder  mBorder;
	protected Color				mPrintForeground,mPrintBackground;

	private ArrayList<FormObjectListener> mListener;
	private String				mKey;
	private int                 mRelativeHeight;

	public AbstractFormObject(String key, String type) {
			// overwrite to set mComponent
		mKey = key;
		mType = type;
		mListener = new ArrayList<>();
		mRelativeHeight = -1;
		}

	public Border getBorder() {
		return mBorder;
		}

	public JComponent getComponent() {
		return mComponent;
		}

	public void setBackground(Color c) {
		if (mComponent instanceof JScrollPane)
			((JScrollPane)mComponent).getViewport().getView().setBackground(c);
		else
			mComponent.setBackground(c);
		}

	public void setForeground(Color c) {
		if (mComponent instanceof JScrollPane)
			((JScrollPane)mComponent).getViewport().getView().setForeground(c);
		else
			mComponent.setForeground(c);
		}

	public void setFont(Font font) {
		mComponent.setFont(font);
		if (mBorder != null)
			mBorder.setFont(font);
		}

	public String getKey() {
		return mKey;
		}

	public String getTitle() {
		return (mBorder == null) ? mKey : mBorder.getTitle();
		}

	public String getType() {
		return mType;
		}

	public void setKey(String key) {
		mKey = key;
		}

	public void setTitle(String title) {
		if (mBorder == null) {
			mBorder = new FormObjectBorder(title, mComponent.getFont());
			mComponent.setBorder(mBorder);
			}
		else {
			mBorder.setTitle(title);
			}
		}

	public void addFormObjectListener(FormObjectListener l) {
		mListener.add(l);
		}

	public void removeFormObjectListener(FormObjectListener l) {
		mListener.remove(l);
		}

	public void fireDataChanged() {
		for (FormObjectListener l:mListener)
			l.dataChanged(this);
		}

	public void setPrintForeground(Color fg) {
		mPrintForeground = fg;
		}

	public void setPrintBackground(Color bg) {
		mPrintBackground = bg;
		}

	public void print(Graphics2D g2D, Rectangle2D.Double r, float scale, Object data, boolean isMultipleRows) {
		if (mBorder != null)
			mBorder.printBorder(g2D, r, scale);

		Shape oldClip = g2D.getClip();
		g2D.setClip(r);
		printContent(g2D, r, scale, data, isMultipleRows);
		g2D.setClip(oldClip);
		}

	/**
	 * Override if the form object supports editing
	 * @param b
	 */
	public void setEditable(boolean b) {
		mComponent.setBackground(UIManager.getColor(b ? "TextArea.background" : "TextArea.inactiveBackground"));
		mComponent.getDropTarget().setActive(b);
		if (mBorder != null)
			mBorder.setEditMode(b);
		}

	public int getRelativeHeight() {
		return mRelativeHeight != -1 ? mRelativeHeight : getDefaultRelativeHeight();
		}

	public void setRelativeHeight(int h) {
		mRelativeHeight = h;
		}

	public abstract Object getData();
	public abstract void setData(Object data);
	public abstract int getDefaultRelativeHeight();
	public abstract void printContent(Graphics2D g2D, Rectangle2D.Double r, float scale, Object data, boolean isMultipleRows);
	}
