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
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.AbstractTask;
import com.actelion.research.datawarrior.task.DEMacroRecorder;
import com.actelion.research.datawarrior.task.macro.DETaskRunMacro;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableListEvent;
import com.actelion.research.table.model.CompoundTableModel;
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
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serial;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.prefs.Preferences;

import static javafx.geometry.Orientation.HORIZONTAL;
import static javafx.geometry.Orientation.VERTICAL;


public class ExplanationView extends JFXPanel implements CompoundTableConstants,CompoundTableView {
	@Serial
	private static final long serialVersionUID = 0x20130114;

	private ViewSelectionHelper mViewSelectionHelper;
	private final CompoundTableModel mTableModel;
	private WebEngine mEngine;
	private String mHTMLText;

	private Point2D pLimit;
	private double width, height;

	public ExplanationView(CompoundTableModel tableModel) {
		mTableModel = tableModel;

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

					Document document = mEngine.getDocument();
					NodeList nodeList = document.getElementsByTagName("a");
					for (int i=0 ; i<nodeList.getLength() ; i++) {
						String link = nodeList.item(i).toString();
						if (link.startsWith("macro:")) {
							((com.sun.webkit.dom.HTMLAnchorElementImpl)nodeList.item(i)).addEventListener("click", e ->
									SwingUtilities.invokeLater(() -> {
										DEFrame frame = DataWarrior.getApplication().getActiveFrame();
										if (DEMacroRecorder.getInstance().isRunningMacro())
											JOptionPane.showMessageDialog(frame, "A macro is running already. Please wait until that macro has finished.");
										else
											new DETaskRunMacro(frame, link.substring(6)).defineAndRun();
							} ), false );
						}
						if (link.startsWith("task:")) {
							((com.sun.webkit.dom.HTMLAnchorElementImpl)nodeList.item(i)).addEventListener("click", e ->
									SwingUtilities.invokeLater(() -> {
										DEFrame frame = DataWarrior.getApplication().getActiveFrame();
										AbstractTask task = DataWarrior.getApplication().getTaskFactory().createTaskFromCode(frame, link.substring(5));
										if (task == null)
											JOptionPane.showMessageDialog(frame, "Couldn't open search dialog, because\ntask '"+link.substring(5)+"' is not available.\nProbably, you miss a required plugin for it.");
										else
											task.defineAndRun();
									} ), false );
						}
						if (link.startsWith("datawarrior:")) {
							((com.sun.webkit.dom.HTMLAnchorElementImpl)nodeList.item(i)).addEventListener("click", e ->
									SwingUtilities.invokeLater(() -> {
										try {
											DataWarrior.getApplication().handleCustomURI(new URI(link));
										}
										catch (URISyntaxException use) {
											DEFrame frame = DataWarrior.getApplication().getActiveFrame();
											JOptionPane.showMessageDialog(frame, "URISyntaxException:"+link);
										}
									} ), false );
						}
					}
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

		String text = (String)mTableModel.getExtensionData(cExtensionNameFileExplanation);

		// We allow including and resolving specific content from the preferences
		text = resolveVariable("$DW_PREFERENCES(", text, key -> {
			int index3 = key.lastIndexOf('.');
			if (index3 == -1)
				return null;

			try {
				Preferences prefs = Preferences.userRoot().node(key.substring(0, index3));
				return prefs.get(key.substring(index3 + 1), null);
			}
			catch (Exception e) {
				return null;
			}
		});

		// We also allow including dynamic HTML content from a given URL (after preferences to allow them to be part of a URL)
		text = resolveVariable("$DW_REMOTE_HTML(", text, url -> {
			try {
				URLConnection con = new URI(url).toURL().openConnection();
				con.setRequestProperty("Content-Type", "text/plain");
				InputStream is = con.getInputStream();
				if (is != null) {
					BufferedReader reader = new BufferedReader(new InputStreamReader(is));
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = reader.readLine()) != null) {
						sb.append(line + "\n");
					}
					is.close();
					return sb.toString();
				}
			}
			catch (Exception e) {}
			return null;
		});

		setText(text);
	}

	private String resolveVariable(String variableName, String text, VariableResolver resolver) {
		// We allow including and resolving specific content from the preferences
		int index = text.indexOf(variableName);
		if (index != -1) {
			boolean error = false;
			StringBuilder sb = new  StringBuilder(text.substring(0, index));
			while (index != -1) {
				int index2 = text.indexOf(")", index);
				if (index2 == -1) {
					error = true;
					break;
				}

				String key = text.substring(index + variableName.length(), index2);
				String value = resolver.resolve(key);
				if (value != null)
					sb.append(value);

				index = text.indexOf(variableName, index2+1);
				sb.append(index == -1 ? text.substring(index2+1) : text.substring(index2+1, index));
			}
			if (!error)
				text = sb.toString();
		}
		return text;
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
			String explanation = (String)mTableModel.getExtensionData(cExtensionNameFileExplanation);
			setText(explanation == null ? "" : explanation);
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
	public void pixelScalingChanged(float pixelScaling) {

	}

	@Override
	public CompoundTableModel getTableModel() {
		return mTableModel;
	}

	private interface VariableResolver {
		String resolve(String key);
	};
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
		if (event instanceof MouseEvent m){
			double[] limit = getLimit();

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
		if (event instanceof KeyEvent k && event.getEventType().equals(KeyEvent.KEY_PRESSED)){

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

/*			// Avoid copy with Ctrl+C or Ctrl+Insert
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