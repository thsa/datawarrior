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

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.gui.JImagePanelFixedSize;
import com.actelion.research.gui.hidpi.HiDPIHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DEAboutDialog extends JDialog implements MouseListener,Runnable {
	private static final long serialVersionUID = 20140219L;
	private int mMillis;

    public DEAboutDialog(DEFrame owner) {
		super(owner, "About DataWarrior", true);

	    setUndecorated(true);
		getContentPane().add(createImagePanel());

		addMouseListener(this);

		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
		}

    public DEAboutDialog(DEFrame owner, int millis) {
		super(owner, "About DataWarrior", true);

	    setUndecorated(true);
		getContentPane().add(createImagePanel());

		pack();
		setLocationRelativeTo(owner);

		mMillis = millis;
		new Thread(this).start();

		setVisible(true);
		}

	private JImagePanelFixedSize createImagePanel() {
		final double scale = 0.5;
    	return new JImagePanelFixedSize("/images/about.jpg", scale) {
			@Override public void paintComponent(Graphics g) {
				super.paintComponent(g);
				((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				int fontSize = HiDPIHelper.scale(12);
				String text = builtDate();
				int x = HiDPIHelper.scale((int)(12 * scale));
				int y = HiDPIHelper.scale((int)(750 * scale));
				g.setFont(g.getFont().deriveFont(Font.BOLD, (float)fontSize));
				g.setColor(Color.WHITE);
				g.drawString(text, x, y);
				}
			};
		}

	public String dateString(long millis) {
		return new SimpleDateFormat("dd-MMM-yyyy").format(new Date(millis));
		}

	private String builtDate() {
		URL url = getClass().getResource("/resources/builtDate.txt");
		if(url != null) {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
				String version = DEUpdateHandler.DATAWARRIOR_VERSION + " (built " + reader.readLine() + ")";
				reader.close();
				return version;
				}
			catch (IOException e) {}
			}
		return DEUpdateHandler.DATAWARRIOR_VERSION;
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
