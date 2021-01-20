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

package com.actelion.research.table.view;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableListEvent;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.config.ViewConfiguration;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.io.BufferedReader;
import java.io.BufferedWriter;

import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.geometry.Orientation.VERTICAL;


public class ExplanationView extends JFXPanel implements CompoundTableConstants,CompoundTableView {
	private static final long serialVersionUID = 0x20130114;

	private ViewSelectionHelper mViewSelectionHelper;
	private CompoundTableModel mTableModel;
	private WebEngine mEngine;
	private String mHTMLText;

	private Point2D pLimit;
	private double width, height;

	public ExplanationView(CompoundTableModel tableModel) {
		mTableModel = tableModel;

//		Handler.install();

		Platform.runLater(() -> {
			WebView view = new WebView();
			mEngine = view.getEngine();

			view.setZoom(HiDPIHelper.getUIScaleFactor());
			view.setContextMenuEnabled(false);
			WebEventDispatcher webEventDispatcher = new WebEventDispatcher(view.getEventDispatcher(), view);

			// the default WebView allows text modification; we need to block this!
			mEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
				if(newValue.equals(Worker.State.SUCCEEDED)){
					view.setEventDispatcher(webEventDispatcher);
				}
			});

			setScene(new Scene(view));
		});

		final CompoundTableView _this = this;
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(java.awt.event.MouseEvent e) {
				super.mousePressed(e);
				mViewSelectionHelper.setSelectedView(_this);
			}
		});

		setText((String)mTableModel.getExtensionData(cExtensionNameFileExplanation));
	}

	public void reload() {
		Platform.runLater(() -> mEngine.loadContent(mHTMLText == null ? "" : mHTMLText));
	}

	public void setText(final String htmlText) {
		mHTMLText = htmlText;
		Platform.runLater(() -> mEngine.loadContent(htmlText == null ? "" : htmlText));
	}

	@Override
	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cChangeExtensionData
				&& cExtensionNameFileExplanation.equals(mTableModel.getExtensionHandler().getName(e.getSpecifier()))) {
			String explanation = (String)mTableModel.getExtensionData(cExtensionNameFileExplanation);
			setText(explanation == null ? "" : explanation);
		}
		if (e.getType() == CompoundTableEvent.cNewTable) {
			setText("");
		}
	}

	@Override
	public void setViewSelectionHelper(ViewSelectionHelper l) {
		mViewSelectionHelper = l;
	}

	public String getText() {
		return mHTMLText;
	}

	@Override
	public boolean copyViewContent() {
		StringSelection stringSelection = new StringSelection(getText());
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, null);
		return true;
	}

	@Override
	public void listChanged(CompoundTableListEvent e) {
	}

	@Override
	public void cleanup() {
		setText("");
	}

	@Override
	public CompoundTableModel getTableModel() {
		return mTableModel;
	}
}

class WebEventDispatcher implements EventDispatcher {
	private final EventDispatcher oldDispatcher;
	private final WebView view;

	public WebEventDispatcher(EventDispatcher oldDispatcher, WebView view) {
		this.oldDispatcher = oldDispatcher;
		this.view = view;
	}

	private boolean allowDrag=false;

	@Override
	public javafx.event.Event dispatchEvent(javafx.event.Event event, EventDispatchChain tail) {
		if (event instanceof MouseEvent){
			double[] limit = getLimit();

			MouseEvent m = (MouseEvent)event;
			if (event.getEventType().equals(MouseEvent.MOUSE_CLICKED) ||
					event.getEventType().equals(MouseEvent.MOUSE_PRESSED)) {
				Point2D origin=new Point2D(m.getX(),m.getY());
				allowDrag=!(origin.getX()<limit[0] && origin.getY()<limit[1]);
			}
			// avoid selection with mouse dragging, allowing dragging the scrollbars
			if (event.getEventType().equals(MouseEvent.MOUSE_DRAGGED)) {
				if(!allowDrag){
					event.consume();
				}
			}
			// Avoid selection of word, line, paragraph with mouse click
			if(m.getClickCount()>1){
				event.consume();
			}
		}
		if (event instanceof KeyEvent && event.getEventType().equals(KeyEvent.KEY_PRESSED)){
			KeyEvent k= (KeyEvent)event;

			// only allow navigation keys
			if (k.getCode() != KeyCode.LEFT
			 && k.getCode() != KeyCode.RIGHT
			 && k.getCode() != KeyCode.UP
			 && k.getCode() != KeyCode.DOWN
			 && k.getCode() != KeyCode.PAGE_UP
			 && k.getCode() != KeyCode.PAGE_DOWN
			 && k.getCode() != KeyCode.HOME
			 && k.getCode() != KeyCode.END
			 && !(k.getCode() == KeyCode.C && (k.isControlDown() || k.isMetaDown()))
			)
				event.consume();

/*				// Avoid copy with Ctrl+C or Ctrl+Insert
			if((k.getCode().equals(KeyCode.C) || k.getCode().equals(KeyCode.INSERT)) && k.isControlDown()){
				event.consume();
			}
			// Avoid selection with shift+Arrow
			if(k.isShiftDown() && (k.getCode().equals(KeyCode.RIGHT) || k.getCode().equals(KeyCode.LEFT) ||
					k.getCode().equals(KeyCode.UP) || k.getCode().equals(KeyCode.DOWN))){
				event.consume();
			}*/
		}
		return oldDispatcher.dispatchEvent(event, tail);
	}

	private double[] getLimit() {
		Point2D p = view.localToScene(view.getWidth(),view.getHeight());
		double[] limit = new double[2];
		limit[0] = p.getX();
		limit[1] = p.getY();
		view.lookupAll(".scroll-bar").stream()
				.map(s->(ScrollBar)s).forEach(s->{
			if(s.getOrientation().equals(VERTICAL)){
				limit[0] -= s.getBoundsInLocal().getWidth();
			}
			if(s.getOrientation().equals(HORIZONTAL)){
				limit[1] -= s.getBoundsInLocal().getHeight();
			}
		});
		return limit;
	}
}

/*
public class ExplanationView extends JScrollPane implements CompoundTableConstants,CompoundTableView {
    private static final long serialVersionUID = 0x20130114;

	private ViewSelectionHelper	mViewSelectionHelper;
    private CompoundTableModel	mTableModel;
    private JTextPane			mTextPane;

	public ExplanationView(CompoundTableModel tableModel) {
		mTableModel = tableModel;

		setBorder(BorderFactory.createEmptyBorder());
		mTextPane = new JTextPane();
		mTextPane.setBorder(null);
		mTextPane.setContentType("text/html");
		mTextPane.setEditable(false);
		mTextPane.setBackground(Color.white);

		setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
		setViewportView(mTextPane);

		String explanation = (String)mTableModel.getExtensionData(cExtensionNameFileExplanation);

		Handler.install();
		mTextPane.setText(explanation == null ? "" : explanation);
		mTextPane.setCaretPosition(0);

		final CompoundTableView _this = this;
		mTextPane.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				super.mousePressed(e);
				mViewSelectionHelper.setSelectedView(_this);
				}
			});
		}

	@Override
	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cChangeExtensionData
		 && cExtensionNameFileExplanation.equals(mTableModel.getExtensionHandler().getName(e.getSpecifier()))) {
			String explanation = (String)mTableModel.getExtensionData(cExtensionNameFileExplanation);
			mTextPane.setText(explanation == null ? "" : explanation);
			}
		if (e.getType() == CompoundTableEvent.cNewTable) {
			mTextPane.setText("");
			}
		}

	@Override
	public void setViewSelectionHelper(ViewSelectionHelper l) {
		mViewSelectionHelper = l;
		}

	public String getText() {
		return mTextPane.getText();
		}

	@Override
	public boolean copyViewContent() {
		StringSelection stringSelection = new StringSelection(getText());
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, null);
		return true;
		}

	public void setText(String text) {
		mTextPane.setText(text);
		}

	@Override
	public void listChanged(CompoundTableListEvent e) {
		}

	@Override
	public void cleanup() {
		mTextPane.setText("");
		}

	@Override
	public CompoundTableModel getTableModel() {
		return mTableModel;
		}
	}
*/