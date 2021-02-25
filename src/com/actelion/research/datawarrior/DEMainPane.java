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

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.task.AbstractTask;
import com.actelion.research.datawarrior.task.DEMacroRecorder;
import com.actelion.research.datawarrior.task.view.*;
import com.actelion.research.gui.JProgressPanel;
import com.actelion.research.gui.LookAndFeelHelper;
import com.actelion.research.gui.dock.Dockable;
import com.actelion.research.gui.dock.JDockingPanel;
import com.actelion.research.gui.hidpi.HiDPIIconButton;
import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.RuntimePropertyEvent;
import com.actelion.research.table.model.*;
import com.actelion.research.table.view.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class DEMainPane extends JDockingPanel
		implements CompoundTableListener,CompoundTableListListener,ListSelectionListener,ViewSelectionHelper,VisualizationListener {
	private static final long serialVersionUID = 0x20060904;

	public static final String[] VIEW_TYPE_ITEM = {"2D-View", "3D-View"};
	public static final String[] VIEW_TYPE_CODE = {"2D", "3D"};
	public static final int VIEW_TYPE_2D = 0;
	public static final int VIEW_TYPE_3D = 1;
	public static final int VIEW_TYPE_TEXT = 2;
	public static final int VIEW_TYPE_MACRO_EDITOR = 3;
	public static final int VIEW_TYPE_STRUCTURE = 4;
	public static final int VIEW_TYPE_FORM = 5;
	public static final int VIEW_TYPE_CARD = 6;
	public static final Dimension MINIMUM_SIZE = new Dimension(128, 128);
	public static final Dimension MINIMUM_VIEW_SIZE = new Dimension(64, 64);

	private static final String COMMAND_NEW_2D_VIEW = "new2D_";
	private static final String COMMAND_NEW_3D_VIEW = "new3D_";
	private static final String COMMAND_NEW_STRUCTURE_GRID = "newSG_";
	private static final String COMMAND_NEW_FORM_VIEW = "newFV_";
	private static final String COMMAND_NEW_CARDS_VIEW = "newCV_";
	private static final String COMMAND_NEW_EXPLANATION_VIEW = "newEV_";
	private static final String COMMAND_NEW_MACRO_EDITOR = "newME_";

	private static final String COMMAND_DUPLICATE = "dup_";
	private static final String COMMAND_RENAME = "rename_";
	private static final String COMMAND_COPY_VIEW = "copyView_";
	private static final String COMMAND_COPY_STATISTICS = "copyStat_";
	private static final String ITEM_NEW_MACRO_EDITOR = "New Macro Editor";
	private static final String ITEM_NEW_EXPLANATION_VIEW = "New Explanation View";


	private DEFrame						mParentFrame;
	private ApplicationViewFactory		mAppViewFactory;	// creates views that are not supported by the DataWarriorApplet
	private DECompoundTableModel		mTableModel;
	private CompoundListSelectionModel  mListSelectionModel;
	private DEParentPane				mParentPane;
	private DEDetailPane				mDetailPane;
	private DEStatusPanel				mStatusPanel;
	private CompoundTableColorHandler	mColorHandler;

	public DEMainPane(DEFrame parent,
					  DECompoundTableModel tableModel,
					  DEDetailPane detailPane,
					  DEStatusPanel statusPanel,
					  DEParentPane mainFrame) {
		mParentFrame = parent;
		mTableModel = tableModel;
		mTableModel.addCompoundTableListener(this);
		mTableModel.getListHandler().addCompoundTableListListener(this);
		mDetailPane = detailPane;
		mStatusPanel = statusPanel;
		mParentPane = mainFrame;

		mColorHandler = new CompoundTableColorHandler(mTableModel);
		mDetailPane.setColorHandler(mColorHandler);

		setMinimumSize(MINIMUM_SIZE);
		setPreferredSize(MINIMUM_SIZE);
		setBorder(BorderFactory.createEmptyBorder(6, 6, 0, 0));
		setToolTipText("");

		addPopupListener();

		mListSelectionModel = new CompoundListSelectionModel(mTableModel);
		mListSelectionModel.addListSelectionListener(this);
		mListSelectionModel.addListSelectionListener(statusPanel);
		}

	public DataWarrior getApplication() {
		return mParentFrame.getApplication();
		}

	public DEParentPane getParentPane() {
		return mParentPane;
		}

	public void setApplicationViewFactory(ApplicationViewFactory factory) {
		mAppViewFactory = factory;
		}

	public void valueChanged(ListSelectionEvent e) {
		if (!e.getValueIsAdjusting()) {
			CompoundListSelectionModel selectionModel = (CompoundListSelectionModel)e.getSource();
			if (selectionModel.getSelectionCount() == 1)
if (selectionModel.getMinSelectionIndex() != selectionModel.getMaxSelectionIndex()) System.out.println("UNEXPECTED LARGE SELECTION RANGE"); else
				mDetailPane.highlightChanged(mTableModel.getRecord(selectionModel.getMinSelectionIndex()));
			}
		}

	public DECompoundTableModel getTableModel() {
		return mTableModel;
		}

	public DETableView getTableView() {
		Dockable selectedDockable = getSelectedDockable();
		if (selectedDockable != null && selectedDockable.getContent() instanceof DETableView)
			return (DETableView)selectedDockable.getContent();

		for (Dockable dockable:getDockables())
			if (dockable.getContent() instanceof DETableView)
				return (DETableView)dockable.getContent();

		return null;
		}

	private void addPopupListener() {
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				handlePopupTrigger(e);
				}

			@Override
			public void mouseReleased(MouseEvent e) {
				handlePopupTrigger(e);
				}
			} );
		}

	private void handlePopupTrigger(MouseEvent e) {
		if (e.isPopupTrigger() && getDockableCount() == 0) {
			JPopupMenu popup = new JPopupMenu();
			
			JMenuItem item1 = new JMenuItem(ITEM_NEW_EXPLANATION_VIEW);
	        item1.addActionListener(this);
	        popup.add(item1);
			JMenuItem item2 = new JMenuItem(ITEM_NEW_MACRO_EDITOR);
	        item2.addActionListener(this);
	        popup.add(item2);

	        popup.show(this, e.getX(), e.getY());
			}
		}

	private boolean hasMacroEditorView() {
		for (Dockable dockable:getDockables())
			if (dockable.getContent() instanceof DEMacroEditor)
				return true;

		return false;
		}

	public DETable getTable() {
		DETableView tv = getTableView();
		return (tv == null) ? null : tv.getTable();
		}

	public void resetAllFilters() {
		for (Dockable dockable:getDockables())
			if (dockable.getContent() instanceof VisualizationPanel)
				((VisualizationPanel)dockable.getContent()).resetAllFilters();
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() == CompoundTableEvent.cAddColumns) {
			for (int column=e.getColumn(); column<mTableModel.getTotalColumnCount(); column++) {
				if (mTableModel.isColumnTypeStructure(column)
				 && mTableModel.getColumnTitleNoAlias(column).equals("Structure")) {
					addStructureView("Structures", getSelectedViewTitle()+"\tcenter", column);
					}
				}
			}
		if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			int[] columnMap = e.getMapping();
			ArrayList<Dockable> removalList = new ArrayList<Dockable>();
			for (Dockable dockable:getDockables()) {
				CompoundTableView view = (CompoundTableView)dockable.getContent();
				if (view instanceof JStructureGrid) {
					int column = ((JStructureGrid)view).getStructureColumn();
					if (column != -1 && columnMap[column] == -1)
						removalList.add(dockable);
					}
				}
			for (Dockable dockable:removalList)
				removeView(dockable);
			}
		if (e.getType() == CompoundTableEvent.cNewTable) {
			removeAllViews();
			addTableView("Table", "root");
			if (e.getSpecifier() == CompoundTableEvent.cSpecifierDefaultFiltersAndViews) {
				add2DView("2D View", "Table\tbottom").setDefaultColumns();
				add3DView("3D View", "2D View\tright").setDefaultColumns();
				for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
					if (mTableModel.isColumnTypeStructure(column)) {
						String title = mTableModel.getColumnTitleNoAlias(column).equals("Structure") ?
								"Structures" : mTableModel.getColumnTitle(column);
						addStructureView(title, "Table\tright", column);
						break;
						}
					}
				if (mTableModel.getExtensionData(CompoundTableConstants.cExtensionNameFileExplanation) != null)
					addExplanationView("Explanation", "Table\ttop\t0.25");
				}
			}

		for (Dockable dockable:getDockables())
			((CompoundTableView)dockable.getContent()).compoundTableChanged(e);

		mColorHandler.compoundTableChanged(e);

		updateStatusPanel();
		}

	public void listChanged(CompoundTableListEvent e) {
		for (Dockable dockable:getDockables())
			((CompoundTableView)dockable.getContent()).listChanged(e);

		mColorHandler.hitlistChanged(e);
		}

	@Override
	public void visualizationChanged(VisualizationEvent e) {
		if (DEMacroRecorder.getInstance().isRecording()) {
			AbstractTask task = null;
			switch (e.getType()) {
			case AXIS:
				task = new DETaskAssignOrZoomAxes(mParentFrame, this, e.getSource());
				break;
			case ROTATION:
				task = new DETaskSetRotation(mParentFrame, this, (VisualizationPanel3D)e.getSource());
				break;
				}
			DEMacroRecorder.record(task, task.getRecentConfiguration() /* this is the view configuration */);
			}
		}

	public JProgressPanel getMacroProgressPanel() {
		return mStatusPanel.getMacroProgressPanel();
		}

	private void updateStatusPanel() {
		mStatusPanel.setNoOfRecords(mTableModel.getTotalRowCount());
		mStatusPanel.setNoOfVisible(mTableModel.getRowCount());
		}

	private JPopupMenu createNewViewPopupMenu(CompoundTableView view) {
		Dockable dockable = getDockable((JComponent)view);
		String title = dockable.getTitle();
		JPopupMenu popup = new JPopupMenu();

		// Currently there is always exactly one TableView.
		//	  addPopupItem(popup, "New Table-View", "newTable_"+title);

		if (!isMaximized()) {
			addMenuItem(popup, "New 2D-View", COMMAND_NEW_2D_VIEW +title);
			addMenuItem(popup, "New 3D-View", COMMAND_NEW_3D_VIEW +title);
			if (DataWarrior.USE_CARDS_VIEW)
				addMenuItem(popup, "New Cards View", COMMAND_NEW_CARDS_VIEW +title);
			addMenuItem(popup, "New Form View", COMMAND_NEW_FORM_VIEW+title);
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
				if (mTableModel.isColumnTypeStructure(column)) {
					addMenuItem(popup, "New Structure View", COMMAND_NEW_STRUCTURE_GRID +title);
					break;
					}
				}
		   	addMenuItem(popup, "New Explanation View", COMMAND_NEW_EXPLANATION_VIEW +title);
		   	if (mAppViewFactory != null && !hasMacroEditorView())
		   		addMenuItem(popup, "New Macro Editor", COMMAND_NEW_MACRO_EDITOR+title);

			popup.addSeparator();

			if (isDuplicatableView((CompoundTableView)getDockable(title).getContent()))
				addMenuItem(popup, "Duplicate View", COMMAND_DUPLICATE+title);
			}
		else {
			JMenuItem item1 = new JMenuItem("   De-maximize this view ");
			item1.setEnabled(false);
			popup.add(item1);
			JMenuItem item2 = new JMenuItem("   to enable view creation!");
			item2.setEnabled(false);
			popup.add(item2);
			popup.addSeparator();
			}

		addMenuItem(popup, "Rename View...", COMMAND_RENAME+title);

		Dockable thisDockable = getDockable(title);
		if (thisDockable.getContent() instanceof DETableView) {
			popup.addSeparator();
			addMenuItem(popup, "Copy Column Statistics", COMMAND_COPY_STATISTICS + title);
			}
		else if (thisDockable.getContent() instanceof VisualizationPanel) {
			popup.addSeparator();
			addMenuItem(popup, "Copy View Image...", COMMAND_COPY_VIEW+title);
			addMenuItem(popup, "Copy Statistical Values", COMMAND_COPY_STATISTICS+title);
			}

		if (thisDockable.getContent() instanceof JStructureGrid) {
			popup.addSeparator();
			addMenuItem(popup, "Copy View Image...", COMMAND_COPY_VIEW+title);
			}

		if (getDockable(title).getContent() instanceof DEFormView) {
			popup.addSeparator();
			addMenuItem(popup, "Copy View Image...", COMMAND_COPY_VIEW+title);
			}

		return popup;
		}

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		String viewName = (command.indexOf("_") == -1) ? null : command.substring(command.indexOf("_")+1);
		CompoundTableView view = getView(viewName);
		if (command.equals(ITEM_NEW_EXPLANATION_VIEW)) {
			addExplanationView(getDefaultViewName(VIEW_TYPE_TEXT, -1), "root");
			}
		if (command.equals(ITEM_NEW_MACRO_EDITOR)) {
			addApplicationView(VIEW_TYPE_MACRO_EDITOR, getDefaultViewName(VIEW_TYPE_MACRO_EDITOR, -1), "root");
			}
		else if (command.startsWith("selected_")) {
			new DETaskSelectView(mParentFrame, this, view).defineAndRun();
			}
		else if (command.startsWith(COMMAND_DUPLICATE)) {
			new DETaskDuplicateView(mParentFrame, this, view).defineAndRun();
			}
		else if (command.startsWith(COMMAND_RENAME)) {
			new DETaskRenameView(mParentFrame, this, view).defineAndRun();
			}
		else if (command.startsWith(COMMAND_COPY_VIEW)) {
			new DETaskCopyViewImage(mParentFrame, this, view).defineAndRun();
			}
		else if (command.startsWith(COMMAND_COPY_STATISTICS)) {
			new DETaskCopyStatisticalValues(mParentFrame, this, view).defineAndRun();
			}
		else if (command.startsWith(COMMAND_NEW_2D_VIEW)) {
			new DETaskNew2DView(mParentFrame, this, viewName).defineAndRun();
			}
		else if (command.startsWith(COMMAND_NEW_3D_VIEW)) {
			new DETaskNew3DView(mParentFrame, this, viewName).defineAndRun();
			}
		else if (command.startsWith(COMMAND_NEW_STRUCTURE_GRID)) {
			int column = selectStructureColumn();
			if (column != -1)
				new DETaskNewStructureView(mParentFrame, this, viewName, column).defineAndRun();
			}
		else if (command.startsWith(COMMAND_NEW_CARDS_VIEW)) {
			if (mTableModel.getTotalRowCount() <= 10000)
				new DETaskNewCardsView(mParentFrame, this, viewName).defineAndRun();
			else
				JOptionPane.showMessageDialog(mParentFrame, "Cards-Views cannot be created for more that 10000 rows.");
			}
		else if (command.startsWith(COMMAND_NEW_FORM_VIEW)) {
			new DETaskNewFormView(mParentFrame, this, viewName).defineAndRun();
			}
		else if (command.startsWith(COMMAND_NEW_EXPLANATION_VIEW)) {
			new DETaskNewTextView(mParentFrame, this, viewName).defineAndRun();
			}
		else if (command.startsWith(COMMAND_NEW_MACRO_EDITOR)) {
			new DETaskNewMacroEditor(mParentFrame, this, viewName).defineAndRun();
			}
		}

	private void showNewViewPopupMenu(CompoundTableView view, JButton button) {
		JPopupMenu popup = createNewViewPopupMenu(view);
		popup.show(button.getParent(), button.getBounds().x,
				button.getBounds().y + button.getBounds().height);
		}

	private void showViewConfigPopupMenu(CompoundTableView view, JButton button) {
		JPopupMenu popup = new DEViewConfigPopupMenu(this, view);
		popup.show(button.getParent(), button.getBounds().x,
				button.getBounds().y + button.getBounds().height);
		}

	//@Override
	public void maximize(CompoundTableView view, JToolBar maximizeToolBar) {
		Dockable dockable = getDockable((JComponent)view);

		if (view instanceof JStructureGrid)
			((JStructureGrid)view).setMaximized(isMaximized()? 1f : (float)getWidth()/dockable.getContent().getWidth());

		if (!isMaximized())	// we are maximizing rather than de-maximizing
			for (Dockable d:getDockables())
				if (d != dockable && d.getContent() instanceof VisualizationPanel)
					((VisualizationPanel)d.getContent()).hideControls();

		JToolBar toolBar = null;
		if (!isMaximized()) {
			boolean hasAxisPopup = view instanceof VisualizationPanel
								|| view instanceof JCardView;
			toolBar = createViewToolBar(view);
			}

		super.maximize(dockable.getTitle(), isMaximized() ? null : toolBar);
		}

	public void renameView(String oldTitle, String newTitle) {
		if (newTitle != null && newTitle.length() != 0 && !newTitle.equals(oldTitle)) {
			changeTitle(oldTitle, newTitle);
			mParentPane.fireRuntimePropertyChanged(
					new RuntimePropertyEvent(this, RuntimePropertyEvent.TYPE_RENAME_VIEW, -1));
			}
		}

	public void synchronizeView(VisualizationPanel vp, VisualizationPanel master) {
		if (master == null) {
			vp.setSynchronizationMaster(null);
			}
		else {
			// make master the new master of all panels currently controlled by vp including vp itself
			ArrayList<VisualizationPanel> childList = new ArrayList<VisualizationPanel>(vp.getSynchronizationChildList());
			childList.add(vp);
			for (VisualizationPanel child:childList)
				child.setSynchronizationMaster(master);
			}
		mParentPane.fireRuntimePropertyChanged(
				new RuntimePropertyEvent(this, RuntimePropertyEvent.TYPE_SYNCHRONIZE_VIEW, -1));
		}

	/**
	 * Adds a new view with default name on top of another view
	 * @param type
	 * @param whereViewTitle
	 * @param structureColumn -1 if the view doesn't require a structure column
	 */
	public CompoundTableView createNewView(int type, String whereViewTitle, int structureColumn) {
		return createNewView(null, type, whereViewTitle, "center", structureColumn);
		}

	/**
	 * Adds a new view relative to another view optionally defining the divider position
	 * @param viewName null for default name
	 * @param type
	 * @param whereViewTitle
	 * @param whereLocation relation[\tdividerposition]
	 * @param structureColumn -1 if the view doesn't require a structure column
	 */
	public CompoundTableView createNewView(String viewName, int type, String whereViewTitle, String whereLocation, int structureColumn) {
		String dockInfo = whereViewTitle + "\t" + whereLocation;

		if (viewName == null)
			viewName = getDefaultViewName(type, structureColumn);

		CompoundTableView view;
		switch (type) {
		case VIEW_TYPE_2D:
			view = add2DView(viewName, dockInfo);
			((VisualizationPanel)view).setDefaultColumns();
			break;
		case VIEW_TYPE_3D:
			view = add3DView(viewName, dockInfo);
			((VisualizationPanel)view).setDefaultColumns();
			break;
		case VIEW_TYPE_CARD:
			view = addCardsView(viewName, dockInfo);
			break;
		case VIEW_TYPE_STRUCTURE:
			view = addStructureView(viewName, dockInfo, structureColumn);
			break;
		case VIEW_TYPE_FORM:
			view = addFormView(viewName, dockInfo, false);
			break;
		case VIEW_TYPE_TEXT:
			view = addExplanationView(viewName, dockInfo);
			break;
		default:
			view = addApplicationView(type, viewName, dockInfo);
			break;
			}

		mParentPane.fireRuntimePropertyChanged(
					new RuntimePropertyEvent(this, RuntimePropertyEvent.TYPE_ADD_VIEW, -1));

		return view;
		}

	public String getDefaultViewName(int viewType, int structureColumn) {
		switch (viewType) {
		case VIEW_TYPE_2D:
			return "2D View";
		case VIEW_TYPE_3D:
			return "3D View";
		case VIEW_TYPE_CARD:
			return "Cards View";
		case VIEW_TYPE_STRUCTURE:
			return (structureColumn == -1 || mTableModel.getColumnTitle(structureColumn).equals("Structure")) ? 
						"Structures" : mTableModel.getColumnTitle(structureColumn);
		case VIEW_TYPE_FORM:
			return "Form View";
		case VIEW_TYPE_TEXT:
			return "Explanation";
		case VIEW_TYPE_MACRO_EDITOR:
			return "Macro Editor";
		default:
			return "Unknown View";
			}
		}

	public boolean hasDuplicatableView() {
		for (Dockable d:getDockables())
			if (isDuplicatableView((CompoundTableView)d.getContent()))
				return true;

		return false;
		}

	public boolean isDuplicatableView(CompoundTableView view) {
		return view instanceof DEFormView
			|| view instanceof VisualizationPanel2D
			|| view instanceof VisualizationPanel3D
			|| view instanceof JStructureGrid;
		}

	/**
	 * @param title the title of an existing view
	 */
	public void closeView(String title) {
		if (isMaximized())
			maximize(title, null);

		((CompoundTableView)getDockable(title).getContent()).cleanup();
		undock(title, false);

		mParentPane.fireRuntimePropertyChanged(
				new RuntimePropertyEvent(this, RuntimePropertyEvent.TYPE_REMOVE_VIEW, -1));
		}

	/**
	 * Duplicates a view with all properties and docks it on top of the copied view.
	 * @param title of the existing view to be copied
	 */
	public void duplicateView(String title) {
		duplicateView(title, "Copy of "+title, title+"\tcenter");
		}

	/**
	 * Duplicates a view with all properties and docks it into the tree of views.
	 * @param title of the existing view to be copied
	 * @param newTitle title of the new view
	 * @param position e.g. "someViewTitle\tright\t0.25" or "otherViewTitle\tcenter"
	 */
	public void duplicateView(String title, String newTitle, String position) {
			// change view's name temporarily and learn view's properties
		DERuntimeProperties properties = new DERuntimeProperties(mParentPane);
		properties.learn();

		Component view = getDockable(title).getContent();
		CompoundTableView newView = null;
		if (view instanceof DEFormView)
			newView = addFormView(newTitle, title+"\tcenter", true);
		else if (view instanceof VisualizationPanel2D)
			newView = add2DView(newTitle, title+"\tcenter");
		else if (view instanceof VisualizationPanel3D)
			newView = add3DView(newTitle, title+"\tcenter");
		else if (view instanceof JStructureGrid)
			newView = addStructureView(newTitle, title+"\tcenter", ((JStructureGrid)view).getStructureColumn());
		properties.applyViewProperties(newView, "_"+title);

		if (view instanceof VisualizationPanel2D
		 || view instanceof VisualizationPanel3D) {
			VisualizationPanel masterView = ((VisualizationPanel) view).getSynchronizationMaster();
			if (masterView != null)
				((VisualizationPanel) newView).setSynchronizationMaster(masterView);
			}

		mParentPane.fireRuntimePropertyChanged(
				new RuntimePropertyEvent(this, RuntimePropertyEvent.TYPE_ADD_VIEW, -1));
		}

	private JMenuItem addMenuItem(Container menu, String itemText, String command) {
		JMenuItem item = new JMenuItem(itemText);
		item.setActionCommand(command);
		item.addActionListener(this);
		menu.add(item);
		return item;
		}

	private JToolBar createViewToolBar(CompoundTableView view) {
		JToolBar toolbar = new JToolBar();
		if (LookAndFeelHelper.isSubstance())
			toolbar.addSeparator();

		JButton plusButton = new HiDPIIconButton("plusButton.png", "New/copy/rename view", null, 0, null);
		plusButton.addActionListener(e -> showNewViewPopupMenu(view, plusButton));
		toolbar.add(plusButton);
		if (LookAndFeelHelper.isSubstance())
			toolbar.addSeparator();

		JButton configButton = new HiDPIIconButton("configButton.png", "Configure View", null, 0, null);
		configButton.addActionListener(e -> showViewConfigPopupMenu(view, configButton));
		toolbar.add(configButton);
		if (LookAndFeelHelper.isSubstance())
			toolbar.addSeparator();

		if (view instanceof VisualizationPanel) {
			String buttonName = view instanceof VisualizationPanel3D ? "xyzButton.png" : "xyButton.png";
			JButton axisButton = new HiDPIIconButton(buttonName, "Assign columns to axes", null, 0, null);
			axisButton.addActionListener(e -> ((VisualizationPanel)view).showControls());
			toolbar.add(axisButton);
			if (LookAndFeelHelper.isSubstance())
				toolbar.addSeparator();
			}

		JButton maxButton = new HiDPIIconButton("maxButton.png", "Maximize view", null, 0, null);
		maxButton.addActionListener(e -> maximize(view, null));
		toolbar.add(maxButton);
		if (LookAndFeelHelper.isSubstance())
			toolbar.addSeparator();

		if (!(view instanceof DETableView)) {
			JButton closeButton = new HiDPIIconButton("closeButton.png", "Close View", null, 0, null);
			closeButton.addActionListener(e -> new DETaskCloseView(mParentFrame, this, view).defineAndRun());
			toolbar.add(closeButton);
			if (LookAndFeelHelper.isSubstance())
				toolbar.addSeparator();
			}

		toolbar.setFloatable(false);
		toolbar.setRollover(true);

		toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		toolbar.setOpaque(false);
		return toolbar;
		}

	/**
	 * Adds a new table view at the given position defined by dockInfo
	 * @param title
	 * @param dockInfo "root" or "title[\tposition[\tdividerlocation]]" e.g. "2D-view\tbottom\t0.4"
	 * @return new view
	 */
	public DETableView addTableView(String title, String dockInfo) {
		DETableView tableView = new DETableView(mParentFrame, mParentPane, mTableModel, mColorHandler, mListSelectionModel);
		tableView.setDetailPopupProvider(mParentPane);
		tableView.setViewSelectionHelper(this);
		title = validateTitle(title);
		JToolBar toolBar = createViewToolBar(tableView);
		Dockable dockable = new Dockable(this, tableView, validateTitle(title), toolBar);
		dockable.setContentMinimumSize(MINIMUM_VIEW_SIZE);
		dock(dockable, dockInfo);
		return tableView;
		}

	/**
	 * Adds a new structure view at the given position defined by dockInfo
	 * @param title
	 * @param dockInfo "root" or "title[\tposition[\tdividerlocation]]" e.g. "2D-view\tbottom\t0.4"
	 * @return new view
	 */
	public JStructureGrid addStructureView(String title, String dockInfo, int column) {
		JStructureGrid structureGrid = new JStructureGrid(mParentFrame, mTableModel, mColorHandler, mListSelectionModel, column, 6);
		structureGrid.setDetailPopupProvider(mParentPane);
		structureGrid.setViewSelectionHelper(this);
		title = validateTitle(title);
		JToolBar toolBar = createViewToolBar(structureGrid);
		Dockable dockable = new Dockable(this, structureGrid, title, toolBar);
		dockable.setContentMinimumSize(MINIMUM_VIEW_SIZE);
		dock(dockable, dockInfo);
		return structureGrid;
		}

	/**
	 * Adds a new card view at the given position defined by dockInfo
	 * @param title
	 * @param dockInfo "root" or "title[\tposition[\tdividerlocation]]" e.g. "2D-view\tbottom\t0.4"
	 * @return new view
	 */
	public JCardView addCardsView(String title, String dockInfo) {
		JCardView cardView = new JCardView(mParentFrame, mTableModel, mColorHandler, mListSelectionModel);
		cardView.setDetailPopupProvider(mParentPane);
		cardView.setViewSelectionHelper(this);
		title = validateTitle(title);
		JToolBar toolBar = createViewToolBar(cardView);
		Dockable dockable = new Dockable(this, cardView, title, toolBar);
		dockable.setContentMinimumSize(MINIMUM_VIEW_SIZE);
		dock(dockable, dockInfo);
		return cardView;
		}

	/**
	 * Adds a new view of a type that is not known to DEMainPane, e.g. if it is not supported in applets.
	 * @param type e.g. VIEW_TYPE_MACRO_EDITOR
	 * @param title
	 * @param dockInfo "root" or "title[\tposition[\tdividerlocation]]" e.g. "2D-view\tbottom\t0.4"
	 * @return requested view or minimum view showing error message
	 */
	public CompoundTableView addApplicationView(int type, String title, String dockInfo) {
		CompoundTableView view = (mAppViewFactory == null) ? null : mAppViewFactory.createApplicationView(type, mParentFrame);
		if (view == null)
			view = new ErrorView("View type not supported!");

		view.setViewSelectionHelper(this);
		title = validateTitle(title);
		JToolBar toolBar = createViewToolBar(view);
		Dockable dockable = new Dockable(this, (JComponent)view, title, toolBar);
		dockable.setContentMinimumSize(MINIMUM_VIEW_SIZE);
		dock(dockable, dockInfo);
		return view;
		}

	/**
	 * Adds a new explanation view at the given position defined by dockInfo
	 * @param title
	 * @param dockInfo "root" or "title[\tposition[\tdividerlocation]]" e.g. "2D-view\tbottom\t0.4"
	 * @return new view
	 */
	public ExplanationView addExplanationView(String title, String dockInfo) {
		ExplanationView view = new ExplanationView(mTableModel);
		view.setViewSelectionHelper(this);
		title = validateTitle(title);
		JToolBar toolBar = createViewToolBar(view);
		Dockable dockable = new Dockable(this, view, title, toolBar);
		dockable.setContentMinimumSize(MINIMUM_VIEW_SIZE);
		dock(dockable, dockInfo);
		return view;
		}

	/**
	 * Adds a new 2D-view at the given position defined by dockInfo
	 * @param title
	 * @param dockInfo "root" or "title[\tposition[\tdividerlocation]]" e.g. "2D-view\tbottom\t0.4"
	 * @return new view
	 */
	public VisualizationPanel2D add2DView(String title, String dockInfo) {
		try {
			VisualizationPanel2D panel2D = new VisualizationPanel2D(mParentFrame, mTableModel, mListSelectionModel);
			panel2D.getVisualization().setDetailPopupProvider(mParentPane);
			panel2D.addVisualizationListener(this);
			panel2D.setViewSelectionHelper(this);
			title = validateTitle(title);
			JToolBar toolBar = createViewToolBar(panel2D);
			Dockable dockable = new Dockable(this, panel2D, title, toolBar);
			dockable.setContentMinimumSize(MINIMUM_VIEW_SIZE);
			dock(dockable, dockInfo);
			return panel2D;
			}
		catch (Exception e) {
			JOptionPane.showMessageDialog(mParentFrame, e.toString());
			e.printStackTrace();
			}
		return null;
		}

	/**
	 * Adds a new 3D-view at the given position defined by dockInfo
	 * @param title
	 * @param dockInfo "root" or "title[\tposition[\tdividerlocation]]" e.g. "2D-view\tbottom\t0.4"
	 * @return new view
	 */
	public VisualizationPanel3D add3DView(String title, String dockInfo) {
		VisualizationPanel3D panel3D = new VisualizationPanel3D(mParentFrame, mTableModel, mListSelectionModel);
		panel3D.getVisualization().setDetailPopupProvider(mParentPane);
		panel3D.addVisualizationListener(this);
		panel3D.setViewSelectionHelper(this);
		title = validateTitle(title);
		JToolBar toolBar = createViewToolBar(panel3D);
		Dockable dockable = new Dockable(this, panel3D, title, toolBar);
		dockable.setContentMinimumSize(MINIMUM_VIEW_SIZE);
		dock(dockable, dockInfo);
		return panel3D;
		}

	/**
	 * Adds a new form view at the given position defined by dockInfo
	 * @param title
	 * @param dockInfo "root" or "title[\tposition[\tdividerlocation]]" e.g. "2D-view\tbottom\t0.4"
	 * @return new view
	 */
	public DEFormView addFormView(String title, String dockInfo, boolean createDefaultLayout) {
		DEFormView form = new DEFormView(mParentFrame, mTableModel, mColorHandler);
		form.setDetailPopupProvider(mParentPane);
		if (createDefaultLayout)
			form.createDefaultLayout();
		form.setViewSelectionHelper(this);
		title = validateTitle(title);
		JToolBar toolBar = createViewToolBar(form);
		Dockable dockable = new Dockable(this, form, title, toolBar);
		dockable.setContentMinimumSize(MINIMUM_VIEW_SIZE);
		dock(dockable, dockInfo);
		return form;
		}

	public void removeView(String title) {
		removeView(getDockable(title));
		}

	public void removeView(Dockable dockable) {
		((CompoundTableView)dockable.getContent()).cleanup();;
		undock(dockable.getTitle());
		}

	public void removeAllViews() {
		for (Dockable dockable:getDockables())
			((CompoundTableView)dockable.getContent()).cleanup();
		undockAll();
		}

	public CompoundTableView getView(String name) {
		if (name == null)
			return null;
		Dockable d = getDockable(name);
		return (d == null) ? null : (CompoundTableView)d.getContent();
		}

	public String getViewTitle(CompoundTableView view) {
		if (view == null)
			return null;
		for (Dockable dockable:getDockables())
			if (dockable.getContent() == view)
				return getTitle(dockable);
		return null;
		}

	public String getSelectedViewTitle() {
		Dockable selected = getSelectedDockable();
		return selected == null ? null : getSelectedDockable().getTitle();
		}

	public CompoundTableView getSelectedView() {
		return (CompoundTableView)getSelectedDockable().getContent();
		}

	/**
	 * Used when interactively clicked into a view to select it.
	 * @param view
	 */
	@Override
	public void setSelectedView(CompoundTableView view) {
		new DETaskSelectView(mParentFrame, this, view).defineAndRun();
		}

	public void setSelectedView(String uniqueID) {
		selectDockable(getDockable(uniqueID));
		}

	/**
	 * From the currently selected view find the next visible dockable in the list and make its view
	 * the selected one, provided that at least two views are visible.
	 */
	public void selectNextVisibleView() {
		if (!isMaximized()) {
			Collection<Dockable> dockables = getDockables();
			Dockable selected = getSelectedDockable();
			boolean found = false;
			for (int i=0; i<2; i++) {
				for (Dockable d:dockables) {
					if (d == selected) {
						found = true;
						continue;
						}
					if (found && d.isVisible()) {
						setSelectedView((CompoundTableView)d.getContent());
						return;
						}
					}
				}
			}
		}

	@Override
	public void relocateView(String movedDockableName, String targetDockableName, int targetPosition, float dividerPosition) {
		new DETaskRelocateView(mParentFrame, this, getView(movedDockableName), targetDockableName, targetPosition).defineAndRun();
		}

	public void doRelocateView(String movedDockableName, String targetDockableName, int targetPosition, float dividerPosition) {
		super.relocateView(movedDockableName, targetDockableName, targetPosition, dividerPosition);
		}

	public void visibilityChanged(Dockable dockable, boolean isVisible) {
		super.visibilityChanged(dockable, isVisible);

		Component view = dockable.getContent();
		if (view instanceof DETableView) {
			DETable table = ((DETableView)view).getTable();
			if (!isVisible)
				table.hideSearchControls();
			else if (table.getColumnFilterText().length() != 0)
				table.showSearchControls();
			}
		else if (view instanceof VisualizationPanel) {
			VisualizationPanel vp = (VisualizationPanel)view;
	   		vp.getVisualization().setSuspendGlobalExclusion(!isVisible);
	   		if (!isVisible)
	   			vp.hideControls();
			}

//		else if (view instanceof JCardView){
//			JCardView cv = (JCardView) view;
//			cv.getCardPane().getCardPaneModel().setRowHiding( !isVisible && cv.getCardPane().getCardPaneModel().isRowHiding() );
//			getTableModel().updateExternalExclusion( cv.getCardPane().getCardPaneModel().getFlagExclusion(),true,true);
//		}

	}


	public String validateTitle(String name) {
		Set<String> titleSet = getDockableTitles();
		if (titleSet.contains(name)) {
			int suffix=2;
			while (true) {
				String newName = name+"_"+(suffix++);
				if (!titleSet.contains(newName))
					return newName;
				}
			}
		return name;
		}

	private int selectStructureColumn() {
		int column = -1;
		ArrayList<String> structureColumnList = null;
		for (int i=0; i<mTableModel.getTotalColumnCount(); i++) {
			if (mTableModel.isColumnTypeStructure(i)) {
				if (column == -1)
					column = i;
				else if (structureColumnList == null) {
					structureColumnList = new ArrayList<String>();
					structureColumnList.add(mTableModel.getColumnTitle(column));
					structureColumnList.add(mTableModel.getColumnTitle(i));
					}
				else {
					structureColumnList.add(mTableModel.getColumnTitle(i));
					}
				}
			}
		if (structureColumnList != null) {
			String option = (String)JOptionPane.showInputDialog(mParentFrame,
					"Please select a column with chemical structures!",
					"Select Structure Column",
					JOptionPane.QUESTION_MESSAGE,
					null,
					structureColumnList.toArray(),
					structureColumnList.get(0));
			column = mTableModel.findColumn(option);
			}
		return column;
		}
	}

class ErrorView extends JPanel implements CompoundTableView {
	private static final long serialVersionUID = 20131211L;

	String mMessage;
	ViewSelectionHelper mViewSelectionListener;

	public ErrorView(String message) {
		mMessage = message;
		}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Dimension size = getSize();
		FontMetrics metrics = g.getFontMetrics();
		g.setColor(Color.RED);
		g.drawString(mMessage, (size.width-metrics.stringWidth(mMessage)) / 2, (size.height+g.getFont().getSize())/2);
		}

	@Override
	public boolean copyViewContent() {
		return false;
		}

	@Override
	public void compoundTableChanged(CompoundTableEvent e) {
		}

	@Override
	public void listChanged(CompoundTableListEvent e) {
		}

	@Override
	public void cleanup() {
		}

	@Override
	public void setViewSelectionHelper(ViewSelectionHelper l) {
		mViewSelectionListener = l;
		}

	@Override
	public CompoundTableModel getTableModel() {
		return null;
		}
	}
