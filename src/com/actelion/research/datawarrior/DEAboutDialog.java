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

package com.actelion.research.datawarrior;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JDialog;

import com.actelion.research.datawarrior.task.file.DETaskAbstractOpenFile;
import com.actelion.research.gui.*;
import com.actelion.research.util.Platform;

public class DEAboutDialog extends JDialog implements MouseListener,Runnable {
	private static final long serialVersionUID = 20140219L;
	private int mMillis;

    public DEAboutDialog(DEFrame owner) {
		super(owner, "About OSIRIS DataWarrior", true);

		getContentPane().add(createImagePanel(owner.getApplication().isIdorsia()));

		addMouseListener(this);

		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
		}

    public DEAboutDialog(DEFrame owner, int millis) {
		super(owner, "About OSIRIS DataWarrior", true);

		getContentPane().add(createImagePanel(owner.getApplication().isIdorsia()));

		pack();
		setLocationRelativeTo(owner);

		mMillis = millis;
		new Thread(this).start();

		setVisible(true);
		}

	private JImagePanelFixedSize createImagePanel(boolean showDate) {
    	if (!showDate)
			return new JImagePanelFixedSize("/images/about.jpg");

    	return new JImagePanelFixedSize("/images/about.jpg") {
			@Override public void paintComponent(Graphics g) {
				super.paintComponent(g);
//				File installDir = DataWarrior.resolveResourcePath("");
//				File jarFile = installDir == null ? null : new File(installDir.getPath()
//						+File.separator+(Platform.isWindows() ? "x64\\DataWarrior64.exe" : "datawarrior.jar"));
//				String dateString = jarFile == null ? "development" : dateString(jarFile.lastModified());
				((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setFont(g.getFont().deriveFont(Font.BOLD, 10f));
				g.setColor(Color.BLUE);
				g.drawString(builtDate(), 430, 10);
				}
			};
		}

	public String dateString(long millis) {
		return new SimpleDateFormat("dd-MMM-yyyy").format(new Date(millis));
		}

	private String builtDate() {
		String date = "development";
		URL url = getClass().getResource("/resources/builtDate.txt");
		if(url != null) {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
				date = reader.readLine();
				reader.close();
				}
			catch (IOException e) {
				date = "exception";
				}
			}
		return date;
		}

	public void run() {
		try {
			Thread.sleep(mMillis);
			}
		catch (Exception e) {}
		dispose();
		}

	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {
		dispose();
		}
	}
