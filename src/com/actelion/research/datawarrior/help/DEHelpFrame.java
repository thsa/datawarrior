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

import com.actelion.research.chem.conf.TorsionDB;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.gui.hidpi.HiDPIIconButton;
import com.actelion.research.gui.hidpi.ScaledEditorKit;
import com.sun.webkit.WebPage;
import info.clearthought.layout.TableLayout;
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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;


public class DEHelpFrame extends JFrame implements HyperlinkListener {
    private static final long serialVersionUID = 0x20061025;

    private static final String[][] CONTENT_PAGES = {
		    { "General Concepts", "/html/help/basics.html"},
		    { "Loading Data", "/html/help/import.html"},
		    { "Main Views", "/html/help/views.html"},
		    { "Working with Data", "/html/help/data.html"},
		    { "Machine Learning", "/html/help/ml.html"},
		    { "Chemical Structures", "/html/help/chemistry.html"},
		    { "Chemistry in 3D", "/html/help/conformers.html"},
		    { "External Databases", "/html/help/databases.html"},
		    { "Row List Concept", "/html/help/lists.html"},
		    { "Using Macros", "/html/help/macros.html"},
		    { "Installation & Customization", "/html/help/installation.html"},
		    { "Keyboard Shortcuts", "/html/help/shortcuts.html"},
		    { "The Structure Editor", "/html/help/editor/editor.html"},
		    { "Similarity & Descriptors", "/html/help/similarity.html"},
		    { "PheSA Shape Alignment", "/html/help/phesa.html"},
		    { "Regular Expressions", "/html/help/regex.html"},
    };

    private static final String START_PAGE_URL = "/html/help/basics.html";

	// Artificial wait after between new page loading and searching, because there is some kind of a timing problem,
	// that prevents showing the first match after search, if we don't wait after loading.
    private static final long SEARCH_DELAY = 100L;

	private static DEHelpFrame sHelpFrame;
	private final JEditorPane mIndexArea;
	private WebEngine	mEngine;
	private final JTextField  mTextFieldSearch;
	private final JLabel      mLabelMatchCount;
	private final JLabel mLabelCurrentChapter;
	private byte[][]    mSearchCache;
	private int[]       mPageMatchCount;
	private int         mTotalMatchCount,mTotalMatch,mChapterMatch,mCurrentChapter;

	public static void updateLookAndFeel() {
		if (sHelpFrame != null) {
			SwingUtilities.updateComponentTreeUI(sHelpFrame);
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

		final int scaled250 = HiDPIHelper.scale(250);
		final int gap = HiDPIHelper.scale(8);

		double[][] size = { {gap, TableLayout.FILL, gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED,
							 gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap, HiDPIHelper.scale(80), gap },
							{gap, TableLayout.PREFERRED, gap} };
		JPanel searchPanel = new JPanel();
		searchPanel.setLayout(new TableLayout(size));
		mLabelCurrentChapter = new JLabel();
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
		searchPanel.add(mLabelCurrentChapter, "1,1");
		searchPanel.add(new JLabel("Find:"), "3,1");
		searchPanel.add(mTextFieldSearch, "5,1");
		searchPanel.add(b1, "7,1");
		searchPanel.add(b2, "9,1");
		searchPanel.add(mLabelMatchCount, "11,1");

		JComponent textArea = createFXPanel();

		JPanel textPanel = new JPanel();
		textPanel.setLayout(new BorderLayout());
		textPanel.add(searchPanel, BorderLayout.NORTH);
		textPanel.add(textArea, BorderLayout.CENTER);

		// Use Swing JEditorPane for clickable content index
		mIndexArea = new JEditorPane();
		mIndexArea.setEditorKit(HiDPIHelper.getUIScaleFactor() == 1f ? new HTMLEditorKit() : new ScaledEditorKit());
		mIndexArea.setEditable(false);
		mIndexArea.addHyperlinkListener(this);
		mIndexArea.setContentType("text/html");
		try {
			DataWarrior app = parent.getApplication();
			mIndexArea.setPage(getClass().getResource(
					app.isIdorsia() ? "/html/help/contentActelion.html" : "/html/help/content.html"));
			}
		catch (IOException e) {
			JOptionPane.showMessageDialog(parent,e.getMessage());
			return;
			}
		JScrollPane spIndexArea = new JScrollPane(mIndexArea);
		spIndexArea.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		spIndexArea.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		spIndexArea.setPreferredSize(new Dimension(scaled250, scaled250));

//		Create a split pane with the two scroll panes in it.
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, spIndexArea, textPanel);
	  	splitPane.setOneTouchExpandable(true);
	  	splitPane.setDividerLocation(scaled250);

//		Provide minimum sizes for the two components in the split pane
	  	Dimension minimumSize = new Dimension(HiDPIHelper.scale(100), HiDPIHelper.scale(50));
		spIndexArea.setMinimumSize(minimumSize);
		textArea.setMinimumSize(minimumSize);
		
		getContentPane().add(splitPane, BorderLayout.CENTER);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		Rectangle screenBounds = getGraphicsConfiguration().getBounds();
		setSize(HiDPIHelper.scale(980), Math.min(HiDPIHelper.scale(1024), getGraphicsConfiguration().getBounds().height - HiDPIHelper.scale(64)));

		int prefX = parent.getLocationOnScreen().x + parent.getSize().width + HiDPIHelper.scale(16);
		int prefY = parent.getLocationOnScreen().y + parent.getSize().height/2 - this.getSize().height/2;
		int x = Math.min(prefX, screenBounds.x + screenBounds.width - this.getSize().width - HiDPIHelper.scale(16));
		int y = Math.max(screenBounds.y + HiDPIHelper.scale(32), Math.min(prefY, screenBounds.y + screenBounds.height - this.getSize().height) - HiDPIHelper.scale(32));
		setLocation(new Point(x, y));

		setResizable(true);
		}

	private JComponent createFXPanel() {
		final JFXPanel fxPanel = new JFXPanel();

		Platform.setImplicitExit(false);
		Platform.runLater(() -> {
			WebView view = new WebView();
			view.setZoom(HiDPIHelper.getUIScaleFactor());
			mEngine = view.getEngine();
			fxPanel.setScene(new Scene(view));
			});

		loadURL(START_PAGE_URL);

		return fxPanel;
		}

	private void loadURL(String url) {
		loadURL(FXHelpFrame.createURL(url));
		}

	private void loadURL(java.net.URL url) {
		boolean found = false;
		for (int i=0; i<CONTENT_PAGES.length; i++) {
			if (url.toString().contains(CONTENT_PAGES[i][1])) {
				setCurrentChapter(i);
				found = true;
				break;
				}
			}
		if (!found)
			setCurrentChapter(-1);

		Platform.runLater(() ->	mEngine.load(url.toExternalForm()));
		}

	@Override
	protected void processWindowEvent(WindowEvent e) {
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			sHelpFrame = null;
			mIndexArea.removeHyperlinkListener(this);
			setVisible(false);
			dispose();
			}
		}

	public void hyperlinkUpdate(HyperlinkEvent e) {
		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			loadURL(e.getURL());
			}
		}

	private void setCurrentChapter(int chapter) {
		mCurrentChapter = chapter;
		mLabelCurrentChapter.setText(chapter == -1 ? "" : "Chapter: "+CONTENT_PAGES[chapter][0]);
		}

	/**
	 * Run a new search after changing the query string
	 */
	private void search() {
		String query = mTextFieldSearch.getText();
		countPageMatches(query);

		if (mTotalMatchCount == 0 || query.isEmpty()) {
			mLabelMatchCount.setText("");
			}
		else {
			mChapterMatch = 0;
			mTotalMatch = 0;

			// if we have no match in the currently displayed chapter, then we move to the first matching chapter
			if (mCurrentChapter == -1 || mPageMatchCount[mCurrentChapter] == 0) {
				mCurrentChapter = 0;
				while (mPageMatchCount[mCurrentChapter] == 0)
					mCurrentChapter++;
				loadURL(CONTENT_PAGES[mCurrentChapter][1]);
				try { Thread.sleep(SEARCH_DELAY); } catch (InterruptedException ie) {}
				}
			else {
				for (int i = 0; i<mCurrentChapter; i++)
					mTotalMatch += mPageMatchCount[i];
				}

			mLabelMatchCount.setText(mTotalMatchCount == 0 ? "" : (mTotalMatch+1) + " of " + mTotalMatchCount);
			}

		searchWebkit(query, true);
		}

	private void next() {
		if (mCurrentChapter == -1 || mTotalMatchCount <= 1)
			return;

		String query = mTextFieldSearch.getText();

		mChapterMatch++;
		mTotalMatch++;

		if (mChapterMatch >= mPageMatchCount[mCurrentChapter]) {
			do {
				mCurrentChapter++;
				if (mCurrentChapter == CONTENT_PAGES.length) {
					mCurrentChapter = 0;
					mTotalMatch = 0;
					}
				} while (mPageMatchCount[mCurrentChapter] == 0);
			mChapterMatch = 0;
			loadURL(CONTENT_PAGES[mCurrentChapter][1]);
			try { Thread.sleep(SEARCH_DELAY); } catch (InterruptedException ie) {}
			}

		mLabelMatchCount.setText((mTotalMatch+1)+" of "+mTotalMatchCount);

		searchWebkit(query, true);
		}

	private void previous() {
		if (mCurrentChapter == -1 || mTotalMatchCount <= 1)
			return;

		String query = mTextFieldSearch.getText();

		mChapterMatch--;
		mTotalMatch--;

		if (mChapterMatch < 0) {
			do {
				mCurrentChapter--;
				if (mCurrentChapter == -1) {
					mCurrentChapter = CONTENT_PAGES.length-1;
					mTotalMatch = mTotalMatchCount-1;
					}
				} while (mPageMatchCount[mCurrentChapter] == 0);
			mChapterMatch = mPageMatchCount[mCurrentChapter] - 1;
			loadURL(CONTENT_PAGES[mCurrentChapter][1]);
			try { Thread.sleep(SEARCH_DELAY); } catch (InterruptedException ie) {}
			}

		mLabelMatchCount.setText((mTotalMatch+1)+" of "+mTotalMatchCount);

		searchWebkit(query, false);
		}

	private void searchWebkit(String query, boolean forward) {
		Platform.runLater(() -> {
			try {
				Field pageField = mEngine.getClass().getDeclaredField("page");
				pageField.setAccessible(true);

				WebPage page = (WebPage)pageField.get(mEngine);
				page.find(query, forward, true, false);
				}
			catch (Exception e) {}
			});
		}

	private void countPageMatches(String query) {
		mTotalMatchCount = 0;
		mPageMatchCount = new int[CONTENT_PAGES.length];

		if (!query.isEmpty()) {
			if (mSearchCache == null) {
				mSearchCache = new byte[CONTENT_PAGES.length][];
				for (int i=0; i<CONTENT_PAGES.length; i++)
					mSearchCache[i] = getHTMLBytes(CONTENT_PAGES[i][1]);
				}

			byte[] queryBytes = query.toLowerCase().getBytes(StandardCharsets.UTF_8);
			for (int i=0; i<CONTENT_PAGES.length; i++) {
				mPageMatchCount[i] = (mSearchCache[i] == null) ? 0 : getPageMatchCount(i, queryBytes);
				mTotalMatchCount += mPageMatchCount[i];
				}
			}
		}

	private int getPageMatchCount(int cacheIndex, byte[] query) {
		byte[] cache = mSearchCache[cacheIndex];
		int cl = cache.length;
		int ql = query.length;
		int count = 0;
		for (int i=0; i<cl-ql+1; i++) {
			if (cache[i] == query[0]) {
				boolean match = true;
				for (int j=1; j<ql; j++) {
					if (cache[i+j] != query[j]) {
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

	/**
	 * Load an html file from the resources, removes all tags, removes surplus spaces,
	 * converts all to lower case, and returns the cleaned test as byte[].
	 * This allows for more efficient caching than using normal UTF16 Strings.
	 */
	public static byte[] getHTMLBytes(String path) {
		InputStream is = TorsionDB.class.getResourceAsStream(path);
		if (is == null)
			return null;

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		try {
			boolean isInTag = false;
			String line = reader.readLine();
			while (line != null) {
				byte[] bytes = line.trim().toLowerCase().getBytes(StandardCharsets.UTF_8);
				int start = 0;

				for (int i=0; i<bytes.length; i++) {
					if (isInTag) {
						if (bytes[i] == '>') {
							start = i+1;
							isInTag = false;
							}
						}
					else {
						if (bytes[i] == '<' && isOpenTag(bytes, i)) {
							isInTag = true;
							if (start < i)
								bos.write(bytes, start, i - start);
							}
						}
					}
				if (!isInTag) {
					bos.write(bytes, start, bytes.length - start);
					bos.write(' ');
					}

				line = reader.readLine();
				}
			reader.close();
			}
		catch (IOException ioe) {}

		return bos.toByteArray();
		}

	/**
	 * Checks, whether the '<' sign at the give position is really the start of an HTML tag.
	 * @param bytes
	 * @param index
	 * @return
	 */
	private static boolean isOpenTag(byte[] bytes, int index) {
		if (bytes.length - index < 3)
			return false;
		byte next = bytes[index+1];
		if (next != '/' && next != '!' && (next < 'a' || next > 'z'))
			return false;
		for (int i=index+2; i<bytes.length; i++) {
			if (bytes[i] == '<')
				return false;
			if (bytes[i] == '>')
				return true;
			}
		return false;
		}
	}
