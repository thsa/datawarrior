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

package com.actelion.research.datawarrior.help;

import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.HTMLEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class FXExplanationEditor extends JFrame {
	private HTMLEditor mEditor;
	private volatile String mHTMLResult;
	private CompoundTableModel mTableModel;

	public FXExplanationEditor(Frame parent, CompoundTableModel tableModel) {
		super("Explanation Editor");

		mTableModel = tableModel;

		final JFXPanel fxPanel = new JFXPanel();

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				Platform.runLater(() -> {
					final String html = mEditor.getHtmlText();
					SwingUtilities.invokeLater( () ->
						mTableModel.setExtensionData(CompoundTableModel.cExtensionNameFileExplanation, html)
					);
				});
			}
		});

		Platform.runLater(() -> {
			mEditor = new HTMLEditor();
			String html = (String)mTableModel.getExtensionData(CompoundTableModel.cExtensionNameFileExplanation);
			if (html != null)
				mEditor.setHtmlText(html);
			fxPanel.setScene(new Scene(mEditor));
		});

		getContentPane().add(fxPanel);

		Dimension size = new Dimension(HiDPIHelper.scale(740), HiDPIHelper.scale(600));
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setPreferredSize(size);
		setLocation(Math.min(parent.getX()+parent.getWidth()+HiDPIHelper.scale(16), screenSize.width - size.width), parent.getY());
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		pack();
	}
}
