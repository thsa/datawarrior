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

package com.actelion.research.datawarrior.task.file;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class JFilePathLabel extends JLabel {
	private static final long serialVersionUID = 20130228L;

	public static final String BUTTON_TEXT = "Choose...";
	public static final String ACTION_COMMAND = "pathChanged";
	private static final String NO_FILE_SPECIFIED = "<not specified>";

	private ActionListener	mListener;
	private String			mPath;

	public JFilePathLabel(boolean allowEditing) {
		mPath = null;
		if (allowEditing) {
			setToolTipText("Double-click to edit");
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (JFilePathLabel.this.isEnabled() && e.getClickCount() == 2) {
						String inPath = mPath == null ? "" : mPath;
						String outPath = JOptionPane.showInputDialog(JFilePathLabel.this, "Type in complete file path!", inPath);
						if (outPath != null && !inPath.equals(outPath)) {
							setPath(outPath);
							if (mListener != null)
								mListener.actionPerformed(new ActionEvent(JFilePathLabel.this, ActionEvent.ACTION_PERFORMED, ACTION_COMMAND));
							}
						}
					}
				} );
			}
		}

	public void setListener(ActionListener listener) {
		mListener = listener;
		}

	/**
	 * @return null or a file path and name
	 */
	public String getPath() {
		return mPath;
		}

	@Override
	@Deprecated
	/**
	 * Use setPath() instead...
	 */
	public void setText(String text) {
		super.setText(text);
		}

	/**
	 * @param path null or a file path and name
	 */
	public void setPath(String path) {
		if (path == null || path.length() == 0) {
			super.setText(NO_FILE_SPECIFIED);
			mPath = null;
			}
		else if (path.startsWith("$")) {
			super.setText(path);
			mPath = path;
			}
		else {
			int index = path.lastIndexOf(File.separatorChar);
			super.setText(index == -1 ? path : "..."+path.substring(index));
			mPath = path;
			}
		}
	}
