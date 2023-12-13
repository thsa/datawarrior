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
import com.actelion.research.gui.hidpi.HiDPIIconButton;
//import com.sun.webkit.WebPage;
import info.clearthought.layout.TableLayout;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class FXHelpFrame extends JFrame {
	private static FXHelpFrame sHelpFrame;
	private static WebEngine sEngine;
	private static byte[] sSearchCache;
	private static String sURL;

	private JTextField  mTextFieldSearch;
	private JLabel      mLabelMatchCount;
	private int         mMatchCount,mMatch;

	private FXHelpFrame(Frame parent) {
		super("DataWarrior Help");

		final int gap = HiDPIHelper.scale(8);

		double[][] size = { {TableLayout.FILL, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED,
				gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap, HiDPIHelper.scale(80), gap },
				{gap, TableLayout.PREFERRED, gap} };
		JPanel searchPanel = new JPanel();
		searchPanel.setLayout(new TableLayout(size));
		mTextFieldSearch = new JTextField(10);
		mTextFieldSearch.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				super.keyTyped(e);
				SwingUtilities.invokeLater(() -> search());
				}
			} );
		HiDPIIconButton b1 = new HiDPIIconButton("toNext.png", null, "<", 180, "bevel");
		HiDPIIconButton b2 = new HiDPIIconButton("toNext.png", null, ">", 0, "bevel");
		b1.addActionListener(e -> previous());
		b2.addActionListener(e -> next());
		mLabelMatchCount = new JLabel("", JLabel.RIGHT);
		searchPanel.add(new JLabel("Find:"), "1,1");
		searchPanel.add(mTextFieldSearch, "3,1");
		searchPanel.add(b1, "5,1");
		searchPanel.add(b2, "7,1");
		searchPanel.add(mLabelMatchCount, "9,1");

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

		JPanel helpPanel = new JPanel();
		helpPanel.setLayout(new BorderLayout());
		helpPanel.add(searchPanel, BorderLayout.NORTH);
		helpPanel.add(fxPanel, BorderLayout.CENTER);
		getContentPane().add(helpPanel);

		Dimension fsize = new Dimension(HiDPIHelper.scale(740), HiDPIHelper.scale(820));
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setPreferredSize(fsize);
		setLocation(Math.min(parent.getX()+parent.getWidth()+HiDPIHelper.scale(16), screenSize.width - fsize.width), parent.getY());
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		pack();
		}

	public static void updateLookAndFeel() {
		if (sHelpFrame != null) {
			SwingUtilities.updateComponentTreeUI(sHelpFrame);
			}
		}

	public static void showResource(final String url, final Frame parent) {
		if (sHelpFrame == null) {
			sHelpFrame = new FXHelpFrame(parent);
			sHelpFrame.setVisible(true);
			}

		sSearchCache = null;
		sURL = url;
		Platform.runLater(() ->	sEngine.load(createURL(url).toExternalForm()) );
		}

	/**
	 * Run a new search after changing the query string
	 */
	private void search() {
		String query = mTextFieldSearch.getText();
		countMatches(query);

		if (mMatchCount == 0 || query.length() == 0) {
			mLabelMatchCount.setText("");
			}
		else {
			mMatch = 0;
			mLabelMatchCount.setText("1 of " + mMatchCount);
			}

		searchWebkit(query, true);
		}

	private void next() {
		if (mMatchCount <= 1 || mMatch == mMatchCount-1)
			return;

		String query = mTextFieldSearch.getText();
		mMatch++;
		mLabelMatchCount.setText((mMatch+1)+" of "+mMatchCount);
		searchWebkit(query, true);
		}

	private void previous() {
		if (mMatchCount <= 1 || mMatch == 0)
			return;

		String query = mTextFieldSearch.getText();
		mMatch--;
		mLabelMatchCount.setText((mMatch+1)+" of "+mMatchCount);
		searchWebkit(query, false);
		}

	private void searchWebkit(String query, boolean forward) {
		Platform.runLater(() -> {
			try {
				Field pageField = sEngine.getClass().getDeclaredField("page");
				pageField.setAccessible(true);

/* TODO find other solution
				WebPage page = (com.sun.webkit.WebPage)pageField.get(sEngine);
				page.find(query, forward, true, false);
 */
				}
			catch (Exception e) {}
			});
		}

	private void countMatches(String query) {
		mMatchCount = 0;

		if (query.length() != 0) {
			if (sSearchCache == null) {
				int index = sURL.indexOf('#');
				sSearchCache = DEHelpFrame.getHTMLBytes(index == -1 ? sURL : sURL.substring(0, index));
				}

			byte[] queryBytes = query.toLowerCase().getBytes(StandardCharsets.UTF_8);
			mMatchCount = getMatchCount(queryBytes);
			}
		}

	private int getMatchCount(byte[] query) {
		int cl = sSearchCache.length;
		int ql = query.length;
		int count = 0;
		for (int i=0; i<cl-ql+1; i++) {
			if (sSearchCache[i] == query[0]) {
				boolean match = true;
				for (int j=1; j<ql; j++) {
					if (sSearchCache[i+j] != query[j]) {
						match = false;
						break;
						}
					}
				if (match) {
					count++;
					i += ql-1;
					}
				}
			}
		return count;
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
