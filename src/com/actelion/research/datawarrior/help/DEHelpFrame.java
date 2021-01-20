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
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.gui.hidpi.*;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;


public class DEHelpFrame extends JFrame implements ActionListener, HyperlinkListener {
    private static final long serialVersionUID = 0x20061025;

    private static final String START_PAGE_URL = "/html/help/basics.html";

	private static DEHelpFrame sHelpFrame;
    private DEFrame 	mParent;
//uncommentForSwing	private JEditorPane			mTextArea;
	private JEditorPane	mContentArea;
	private WebEngine	mEngine;

	public static void updateLookAndFeel() {
		if (sHelpFrame != null) {
			SwingUtilities.updateComponentTreeUI(sHelpFrame);
//uncommentForSwing				hf.mTextArea.setBackground(Color.white);
			}
		}

	public static void showInstance(DEFrame parent) {
		if (sHelpFrame == null) {
			sHelpFrame = new DEHelpFrame(parent);
			sHelpFrame.setVisible(true);
			}
		else {
			sHelpFrame.toFront();
			}
		}

	private DEHelpFrame(DEFrame parent) {
		super("DataWarrior Help");
		mParent = parent;

		final int scaled250 = HiDPIHelper.scale(250);

		getContentPane().setLayout(new BorderLayout());	//change this to tableLayout

//uncommentForSwing		JComponent textArea = createSwingTextArea(scaled250);
		JComponent textArea = createFXPanel();

		//2nd jEditorPane
		mContentArea = new JEditorPane();
		mContentArea.setEditorKit(HiDPIHelper.getUIScaleFactor() == 1f ? new HTMLEditorKit() : new ScaledEditorKit());
		mContentArea.setEditable(false);
		mContentArea.addHyperlinkListener(this);
		mContentArea.setContentType("text/html");
		try {
			DataWarrior app = parent.getApplication();
			mContentArea.setPage(getClass().getResource(
					app.isIdorsia() ? "/html/help/contentActelion.html" : "/html/help/content.html"));
			}
		catch (IOException e) {
			JOptionPane.showMessageDialog(mParent,e.getMessage());
			return;
			}
		JScrollPane spContentArea = new JScrollPane(mContentArea);
		spContentArea.setVerticalScrollBarPolicy(
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		spContentArea.setHorizontalScrollBarPolicy(
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		spContentArea.setPreferredSize(new Dimension(scaled250, scaled250));
		
//		Create a split pane with the two scroll panes in it.
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, spContentArea, textArea);
	  	splitPane.setOneTouchExpandable(true);
	  	splitPane.setDividerLocation(scaled250);

//		Provide minimum sizes for the two components in the split pane
	  	Dimension minimumSize = new Dimension(HiDPIHelper.scale(100), HiDPIHelper.scale(50));
		spContentArea.setMinimumSize(minimumSize);
		textArea.setMinimumSize(minimumSize);
		
		getContentPane().add(splitPane, BorderLayout.CENTER);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		Rectangle screenBounds = getGraphicsConfiguration().getBounds();
		setSize(HiDPIHelper.scale(980), Math.min(HiDPIHelper.scale(1024), getGraphicsConfiguration().getBounds().height - HiDPIHelper.scale(64)));

		int prefX = mParent.getLocationOnScreen().x + mParent.getSize().width + HiDPIHelper.scale(16);
		int prefY = mParent.getLocationOnScreen().y + mParent.getSize().height/2 - this.getSize().height/2;
		int x = Math.min(prefX, screenBounds.x + screenBounds.width - this.getSize().width - HiDPIHelper.scale(16));
		int y = Math.max(screenBounds.y + HiDPIHelper.scale(32), Math.min(prefY, screenBounds.y + screenBounds.height - this.getSize().height) - HiDPIHelper.scale(32));
		setLocation(new Point(x, y));

		setResizable(true);
		}

/*uncommentForSwing
	private JComponent createSwingTextArea(int size) {
		//1st jEditorPane
		mTextArea = new JEditorPane();
		mTextArea.setEditorKit(HiDPIHelper.getUIScaleFactor() == 1f ? new HTMLEditorKit() : new ScaledEditorKit());
		mTextArea.setEditable(false);
		mTextArea.addHyperlinkListener(this);
		mTextArea.setContentType("text/html");
		mTextArea.setBackground(Color.white);   // leads to gray in dark substance LaF

		try {
		    mTextArea.setPage(getClass().getResource(START_PAGE_URL));
			}
		catch (IOException ioe) {
			JOptionPane.showMessageDialog(mParent, ioe.getMessage());
			return null;
			}

		JScrollPane spTextArea = new JScrollPane(mTextArea);
		spTextArea.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		spTextArea.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		spTextArea.setPreferredSize(new Dimension(size, size));

		return spTextArea;
		}*/

	private JComponent createFXPanel() {
		final JFXPanel fxPanel = new JFXPanel();

		Platform.setImplicitExit(false);
		Platform.runLater(() -> {
			WebView view = new WebView();
			view.setZoom(HiDPIHelper.getUIScaleFactor());
			mEngine = view.getEngine();
			fxPanel.setScene(new Scene(view));
			});

		Platform.runLater(() ->	mEngine.load(FXHelpFrame.createURL(START_PAGE_URL).toExternalForm()) );

		return fxPanel;
		}

	@Override
	protected void processWindowEvent(WindowEvent e) {
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			sHelpFrame = null;
			mContentArea.removeHyperlinkListener(this);
			setVisible(false);
			dispose();
			}
		}

	public void hyperlinkUpdate(HyperlinkEvent e) {
		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			java.net.URL url = e.getURL();

			Platform.runLater(() ->	mEngine.load(url.toExternalForm()) );
/*//uncommentForSwing
			try {
				mTextArea.setPage(url);	// in case of Swing based browser
				}
			catch(IOException ioe) {
				JOptionPane.showMessageDialog(mParent, ioe.getMessage(), "DataWarrior Help", JOptionPane.WARNING_MESSAGE);
				return;
				}*/
			}
		}
	
	public void actionPerformed(ActionEvent e){	
//		DocumentRenderer DocumentRenderer = new DocumentRenderer();
//		DocumentRenderer.print(mTextArea);
		}
	}