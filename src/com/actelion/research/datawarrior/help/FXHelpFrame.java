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
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URL;

public class FXHelpFrame extends JFrame {
	private static FXHelpFrame sHelpFrame;
	private static WebEngine sEngine;

	private FXHelpFrame(Frame parent) {
		super("DataWarrior Help");

		final JFXPanel fxPanel = new JFXPanel();

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				sHelpFrame = null;
			}
		});

		Platform.runLater(() -> {
			WebView view = new WebView();
			view.setZoom(HiDPIHelper.getUIScaleFactor());
			sEngine = view.getEngine();
			fxPanel.setScene(new Scene(view));
		});

		getContentPane().add(fxPanel);

		Dimension size = new Dimension(HiDPIHelper.scale(740), HiDPIHelper.scale(740));
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setPreferredSize(size);
		setLocation(Math.min(parent.getX()+parent.getWidth()+HiDPIHelper.scale(16), screenSize.width - size.width), parent.getY());
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		pack();
	}

	public static void showResource(final String resource, final Frame parent) {
		if (sHelpFrame == null) {
			sHelpFrame = new FXHelpFrame(parent);
			sHelpFrame.setVisible(true);
		}

		Platform.runLater(() ->	sEngine.load(createURL(resource).toExternalForm()) );
	}

/*	private static void addHyperLinkListener() {
		sEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
			public void changed(ObservableValue ov, State oldState, State newState) {
				if (newState == Worker.State.SUCCEEDED) {
					// note next classes are from org.w3c.dom domain
					EventListener listener = new EventListener() {
						@Override
						public void handleEvent(Event ev) {
							String href = ((Element)ev.getTarget()).getAttribute("href");
							System.out.println(href);
						}
					};

					Document doc = sEngine.getDocument();
					Element el = doc.getElementById("a");
					NodeList lista = doc.getElementsByTagName("a");
					System.out.println("Liczba elementow: "+ lista.getLength());
					for (int i=0; i<lista.getLength(); i++)
						((EventTarget)lista.item(i)).addEventListener("click", listener, false);
				}
			}
		});
	}*/

	public static URL createURL(String urlText) {
		String ref = null;
		int index = urlText.indexOf('#');
		if (index != -1) {
			ref = urlText.substring(index);
			urlText = urlText.substring(0, index);
		}
		URL theURL = FXHelpFrame.class.getResource(urlText);
		if (ref != null) {
			try {
				theURL = new URL(theURL, ref);
			}
			catch (IOException e) {
				return null;
			}
		}
		return theURL;
	}

	public static void main(String[] param) {
		showResource("/html/help/basics.html", new Frame());
	}
}
