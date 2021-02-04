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

import com.actelion.research.datawarrior.help.DETaskSetExplanationHTML;
import com.actelion.research.datawarrior.help.FXExplanationEditor;
import com.actelion.research.datawarrior.task.table.*;
import com.actelion.research.datawarrior.task.view.*;
import com.actelion.research.datawarrior.task.view.cards.DETaskConfigureCard;
import com.actelion.research.datawarrior.task.view.cards.DETaskSetCardViewOptions;
import com.actelion.research.gui.JScrollableMenu;
import com.actelion.research.gui.dock.Dockable;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.*;
import com.actelion.research.table.view.card.CardElement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class DEViewConfigPopupMenu extends JPopupMenu implements ActionListener,ItemListener {
	private static final long serialVersionUID = 0x20060904;

	private static final String TEXT_SET_FONT_SIZE = "Set Font Size...";
	private static final String TEXT_SET_HEADER_LINES = "Set Header Line Count ";
	private static final String TEXT_CHANGE_COLUMN_ORDER = "Change Column Order...";
	private static final String TEXT_SHOW_GROUP = "Show Column Group Only";
	private static final String TEXT_GROUP_SELECTED = "Group Selected Columns...";
	private static final String TEXT_ADD_TO_GROUP = "Add Selected Columns To";
	private static final String TEXT_REMOVE_GROUP = "Remove Column Group";

	private static final String TEXT_EDIT_WYSIWYG = "Edit WYSIWYG...";
	private static final String TEXT_EDIT_HTML = "Edit HTML...";
	private static final String TEXT_RELOAD = "Reload Explanation...";

	private static final String TEXT_STRUCTURE_LABELS = "Show/Hide/Size Labels...";
	private static final String TEXT_GENERAL_OPTIONS = "Set Graphical View Options...";
	private static final String TEXT_STATISTICAL_OPTIONS = DETaskSetStatisticalViewOptions.TASK_NAME+"...";
	private static final String TEXT_CHART_TYPE = DETaskSetPreferredChartType.TASK_NAME+"...";
	private static final String TEXT_SPLIT_VIEW = DETaskSplitView.TASK_NAME+"...";
	private static final String TEXT_MARKER_SIZE = DETaskSetMarkerSize.TASK_NAME+"...";
	private static final String TEXT_MARKER_SHAPE = DETaskSetMarkerShape.TASK_NAME+"...";
	private static final String TEXT_MARKER_COLOR = DETaskSetMarkerColor.TASK_NAME+"...";
	private static final String TEXT_MARKER_BG_COLOR = DETaskSetMarkerBackgroundColor.TASK_NAME+"...";
	private static final String TEXT_MARKER_LABELS = "Set Marker Labels...";
	private static final String TEXT_MARKER_CONNECTION = DETaskSetConnectionLines.TASK_NAME+"...";
	private static final String TEXT_MARKER_JITTERING = DETaskSetMarkerJittering.TASK_NAME+"...";
	private static final String TEXT_MARKER_TRANSPARENCY = DETaskSetMarkerTransparency.TASK_NAME+"...";
	private static final String TEXT_MULTI_VALUE_MARKER = DETaskSetMultiValueMarker.TASK_NAME+"...";
	private static final String TEXT_SEPARATE_CASES = DETaskSeparateCases.TASK_NAME+"...";
	private static final String TEXT_FOCUS = DETaskSetFocus.TASK_NAME+"...";
	private static final String TEXT_BACKGROUND_IMAGE = DETaskSetBackgroundImage.TASK_NAME+"...";
	private static final String TEXT_HORIZ_STRUCTURE_COUNT = DETaskSetHorizontalStructureCount.TASK_NAME;
	private static final String TEXT_COLOR_DISPLAY_MODE = "Set Structure Color Display Mode";
	private static final String TEXT_STEREO_DISPLAY_MODE = "Set Structure Stereo Display Mode";
	private static final String TEXT_USE_AS_FILTER = "Use View As Explicit Filter";

	private static final String TEXT_CARD_OPTIONS   = "Cards View Options..";
	private static final String TEXT_CARD_CONFIGURE = "Configure Cards..";

	/**
	 * Variables needed for card actions
	 */
	private CardElement mCEMouseOver = null;

	private static final String DELIMITER = "@#|";

	private static final String STEREO_DISPLAY_MODE = "stereoDMode" + DELIMITER;
	private static final String COLOR_DISPLAY_MODE = "colorDMode" + DELIMITER;

	private DEMainPane			mMainPane;
	private CompoundTableModel	mTableModel;
	private CompoundTableView	mSource;

	/**
	 * Creates a context dependent popup menu presenting options for one record.
	 * @param mainPane
	 * @param source
	 */
	public DEViewConfigPopupMenu(DEMainPane mainPane, CompoundTableView source) {
		super();

		mMainPane = mainPane;
		mTableModel = mainPane.getTableModel();
		mSource = source;

		if (source instanceof VisualizationPanel) {
			int chartType = ((VisualizationPanel)source).getVisualization().getChartType();

			addMenuItem(TEXT_CHART_TYPE);
			addSeparator();

			addMenuItem(TEXT_GENERAL_OPTIONS);
			if (source instanceof VisualizationPanel2D)
				addMenuItem(TEXT_STATISTICAL_OPTIONS);

			addSeparator();

			addMenuItem(TEXT_MARKER_SIZE);

			if (chartType != JVisualization.cChartTypeBars
			 && chartType != JVisualization.cChartTypePies)
				addMenuItem(TEXT_MARKER_SHAPE);

			addMenuItem(TEXT_MARKER_COLOR);

			if (source instanceof VisualizationPanel2D
			 && chartType != JVisualization.cChartTypeBars)
				addMenuItem(TEXT_MARKER_BG_COLOR);

			addSeparator();

			if (source instanceof VisualizationPanel2D
			 || chartType == JVisualization.cChartTypeScatterPlot)
			addMenuItem(TEXT_MARKER_LABELS);

			if (chartType != JVisualization.cChartTypeBars
			 && chartType != JVisualization.cChartTypePies) {
				addMenuItem(TEXT_MARKER_CONNECTION);
				addMenuItem(TEXT_MARKER_JITTERING);
				}

			if (source instanceof VisualizationPanel2D) {
				addMenuItem(TEXT_MARKER_TRANSPARENCY);
				}

			addSeparator();
			addMenuItem(TEXT_SEPARATE_CASES);
			if (source instanceof VisualizationPanel2D) {
				addMenuItem(TEXT_SPLIT_VIEW);

				if (chartType != JVisualization.cChartTypeBars
				 && chartType != JVisualization.cChartTypePies)
					addMenuItem(TEXT_MULTI_VALUE_MARKER);

				addSeparator();
				addMenuItem(TEXT_BACKGROUND_IMAGE);
				}
			}
		else if (source instanceof JStructureGrid) {
			addMenuItem(TEXT_STRUCTURE_LABELS);
			}

		if (source instanceof FocusableView && ! (source instanceof JCardView) ) {
			if (getComponentCount() > 0)
				addSeparator();
			addMenuItem(TEXT_FOCUS);
			}

		if (source instanceof VisualizationPanel3D) {
			JVisualization3D visualization3D = (JVisualization3D)((VisualizationPanel3D)source).getVisualization();

			addSeparator();

			JCheckBoxMenuItem itemIsStereo = new JCheckBoxMenuItem("Use Stereo", visualization3D.isStereo());
			itemIsStereo.addItemListener(this);
			add(itemIsStereo);

			JMenu stereoModeMenu = new JMenu("Stereo Mode");
			JCheckBoxMenuItem item2 = new JCheckBoxMenuItem("H-Interlace Left Eye First",
					visualization3D.getStereoMode() == JVisualization3D.STEREO_MODE_H_INTERLACE_LEFT_EYE_FIRST);
			item2.addItemListener(this);
			stereoModeMenu.add(item2);

			JCheckBoxMenuItem item3 = new JCheckBoxMenuItem("H-Interlace Right Eye First",
					visualization3D.getStereoMode() == JVisualization3D.STEREO_MODE_H_INTERLACE_RIGHT_EYE_FIRST);
			item3.addItemListener(this);
			stereoModeMenu.add(item3);

			JCheckBoxMenuItem item4 = new JCheckBoxMenuItem("Vertical Interlace",
					visualization3D.getStereoMode() == JVisualization3D.STEREO_MODE_V_INTERLACE);
			item4.addItemListener(this);
			stereoModeMenu.add(item4);

			JCheckBoxMenuItem item5 = new JCheckBoxMenuItem("Side By Side For 3D-TV",
					visualization3D.getStereoMode() == JVisualization3D.STEREO_MODE_3DTV_SIDE_BY_SIDE);
			item5.addItemListener(this);
			stereoModeMenu.add(item5);

			add(stereoModeMenu);
			}

		if (source instanceof VisualizationPanel) {
			VisualizationPanel thisVP = (VisualizationPanel)source;
			Dockable thisDockable = mMainPane.getDockable((JComponent)source);

			addSeparator();
			JCheckBoxMenuItem item = new JCheckBoxMenuItem(TEXT_USE_AS_FILTER, thisVP.getVisualization().isUsedAsFilter());
			item.addActionListener(this);
			add(item);

			JMenu menuSynchronize = null;
			ButtonGroup groupSynchronize = null;
			for (Dockable dockable:mMainPane.getDockables()) {
				if (dockable != thisDockable && dockable.getContent() instanceof VisualizationPanel) {
					VisualizationPanel otherVP = (VisualizationPanel)dockable.getContent();
					if (otherVP.getSynchronizationMaster() == null && thisVP.getDimensionCount() <= otherVP.getDimensionCount()) {
						if (menuSynchronize == null) {
							menuSynchronize = new JMenu("Synchronize View To ");
							groupSynchronize = new ButtonGroup();
							addRadioButtonItem(menuSynchronize, groupSynchronize, "<none>",
									"synchronizeToNone",
									thisVP.getSynchronizationMaster() == null);
						}

						addRadioButtonItem(menuSynchronize, groupSynchronize, dockable.getTitle(),
								"synchronizeTo_"+dockable.getTitle(),
								thisVP.getSynchronizationMaster() == otherVP);
					}
				}
			}
			if (menuSynchronize != null)
				add(menuSynchronize);
			}

		if (source instanceof ExplanationView) {
			addMenuItem(TEXT_EDIT_WYSIWYG);
			addMenuItem(TEXT_EDIT_HTML);
			addMenuItem(TEXT_RELOAD);
			}

		if (source instanceof DEFormView) {
			addMenuItem(TEXT_SET_FONT_SIZE);

			addSeparator();

			boolean isDesignMode = ((DEFormView)source).isDesignMode();
			JCheckBoxMenuItem item1 = new JCheckBoxMenuItem("Design Mode", isDesignMode);
			item1.addActionListener(this);
			add(item1);

			boolean isEditMode = ((DEFormView)source).isEditMode();
			JCheckBoxMenuItem item2 = new JCheckBoxMenuItem("Edit Mode", isEditMode);
			item2.addActionListener(this);
			add(item2);
			}

		if (source instanceof DETableView) {
			DETableView tableView = (DETableView)source;

			addMenuItem(TEXT_SET_FONT_SIZE);

			int currentValue = tableView.getHeaderLineCount();
			JMenu menuHeaderLines = new JMenu("Set Header Line Count ");
			ButtonGroup groupHeaderLines = new ButtonGroup();
			for (String option : DETaskSetHeaderLineCount.OPTIONS) {
				String command = TEXT_SET_HEADER_LINES + option;
				addRadioButtonItem(menuHeaderLines, groupHeaderLines, option, command, Integer.parseInt(option) == currentValue);
				}
			add(menuHeaderLines);

			addSeparator();
			addMenuItem(TEXT_CHANGE_COLUMN_ORDER);

			String[] columnGroup = DETaskShowTableColumnGroup.getAvailableGroupNames(mTableModel);
			if (columnGroup.length != 0) {
				addSeparator();

				JMenu showMenu = new JScrollableMenu(TEXT_SHOW_GROUP);
				add(showMenu);
				for (String groupName:columnGroup) {
					JMenuItem item = new JMenuItem(groupName);
					item.addActionListener(this);
					item.setActionCommand(TEXT_SHOW_GROUP + groupName);
					showMenu.add(item);
					}
				}

			addSeparator();

			int selectionCount = 0;
			JTable table = tableView.getTable();
			for (int i=0; i<table.getColumnCount(); i++)
				if (table.isColumnSelected(i))
					selectionCount++;

			addMenuItem(TEXT_GROUP_SELECTED).setEnabled(selectionCount != 0);

			if (columnGroup.length != 0) {
				if (selectionCount != 0) {
					JMenu addToGroupMenu = new JMenu(TEXT_ADD_TO_GROUP);
					add(addToGroupMenu);
					for (String groupName : columnGroup) {
						JMenuItem item = new JMenuItem(groupName);
						item.addActionListener(this);
						item.setActionCommand(TEXT_ADD_TO_GROUP + groupName);
						addToGroupMenu.add(item);
						}
					}

				JMenu ungroupMenu = new JMenu(TEXT_REMOVE_GROUP);
				add(ungroupMenu);
				for (String groupName:columnGroup) {
					JMenuItem item = new JMenuItem(groupName);
					item.addActionListener(this);
					item.setActionCommand(TEXT_REMOVE_GROUP + groupName);
					ungroupMenu.add(item);
					}
				}
			}

		if (source instanceof JStructureGrid) {
			if (getComponentCount() > 0)
				addSeparator();
			JMenu columnMenu = new JMenu(TEXT_HORIZ_STRUCTURE_COUNT);
			for (String count: DETaskSetHorizontalStructureCount.COUNT_OPTIONS) {
				JMenuItem menuItem = new JMenuItem(count);
				menuItem.addActionListener(this);
				columnMenu.add(menuItem);
				}
			add(columnMenu);

			JMenu colorModeMenu = new JMenu(TEXT_COLOR_DISPLAY_MODE);
			int colorModeIndex = DETaskSetStructureDisplayMode.findColorModeIndex(((JStructureGrid)source).getStructureDisplayMode() & DETaskSetStructureDisplayMode.COLOR_MODE_MASK);
			for (int i = 0; i<DETaskSetStructureDisplayMode.COLOR_MODE_TEXT.length; i++) {
				JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(DETaskSetStructureDisplayMode.COLOR_MODE_TEXT[i], i==colorModeIndex);
				menuItem.setActionCommand(COLOR_DISPLAY_MODE+DETaskSetStructureDisplayMode.COLOR_MODE_CODE[i]);
				menuItem.addActionListener(this);
				colorModeMenu.add(menuItem);
				}
			add(colorModeMenu);

			JMenu stereoModeMenu = new JMenu(TEXT_STEREO_DISPLAY_MODE);
			int stereoModeIndex = DETaskSetStructureDisplayMode.findStereoModeIndex(((JStructureGrid)source).getStructureDisplayMode() & DETaskSetStructureDisplayMode.STEREO_MODE_MASK);
			for (int i = 0; i<DETaskSetStructureDisplayMode.STEREO_MODE_TEXT.length; i++) {
				JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(DETaskSetStructureDisplayMode.STEREO_MODE_TEXT[i], i==stereoModeIndex);
				menuItem.setActionCommand(STEREO_DISPLAY_MODE+DETaskSetStructureDisplayMode.STEREO_MODE_CODE[i]);
				menuItem.addActionListener(this);
				stereoModeMenu.add(menuItem);
				}
			add(stereoModeMenu);
			}

		if (source instanceof JCardView && System.getProperty("development") != null) {
			if (getComponentCount() > 0)
				addSeparator();

			JMenuItem cardWizard = new JMenuItem(TEXT_CARD_CONFIGURE);
			cardWizard.addActionListener(this);
			add(cardWizard);

			addSeparator();

			JMenuItem graphicalOptions = new JMenuItem(TEXT_CARD_OPTIONS);
			graphicalOptions.addActionListener(this);
			add(graphicalOptions);
			}
		}


	public String encodeParams(String params) throws UnsupportedEncodingException {
		// The URLEncoder replaces ' ' by a '+', which seems to be the old way of doing it,
		// which is not compatible with wikipedia and molecule names that contain spaces.
		// (Wikipedia detects "%20" and converts them into '_', which they use in their page names instead of spaces)
		return URLEncoder.encode(params, "UTF-8").replace("+", "%20");
		}

	public void itemStateChanged(ItemEvent e) {
		if (((JCheckBoxMenuItem)e.getItem()).getText().equals("Use Stereo")) {
			((JVisualization3D)((VisualizationPanel)mSource).getVisualization()).setStereo(((JCheckBoxMenuItem)e.getItem()).isSelected());
			}
		else if (((JCheckBoxMenuItem)e.getItem()).getText().equals("H-Interlace Left Eye First")) {
			((JVisualization3D)((VisualizationPanel)mSource).getVisualization()).setStereoMode(JVisualization3D.STEREO_MODE_H_INTERLACE_LEFT_EYE_FIRST);
			}
		else if (((JCheckBoxMenuItem)e.getItem()).getText().equals("H-Interlace Right Eye First")) {
			((JVisualization3D)((VisualizationPanel)mSource).getVisualization()).setStereoMode(JVisualization3D.STEREO_MODE_H_INTERLACE_RIGHT_EYE_FIRST);
			}
		else if (((JCheckBoxMenuItem)e.getItem()).getText().equals("Vertical Interlace")) {
			((JVisualization3D)((VisualizationPanel)mSource).getVisualization()).setStereoMode(JVisualization3D.STEREO_MODE_V_INTERLACE);
			}
		else if (((JCheckBoxMenuItem)e.getItem()).getText().equals("Side By Side For 3D-TV")) {
			((JVisualization3D)((VisualizationPanel)mSource).getVisualization()).setStereoMode(JVisualization3D.STEREO_MODE_3DTV_SIDE_BY_SIDE);
			}
		}

	public void actionPerformed(ActionEvent e) {
		String actionCommand = e.getActionCommand();

		if (actionCommand.equals(TEXT_CHART_TYPE)) {
			new DETaskSetPreferredChartType(getParentFrame(), mMainPane, (VisualizationPanel) mSource).defineAndRun();
		} else if (actionCommand.equals(TEXT_STRUCTURE_LABELS)
				|| actionCommand.equals(TEXT_MARKER_LABELS)) {
			new DETaskSetMarkerLabels(getParentFrame(), mMainPane, mSource).defineAndRun();
		} else if (actionCommand.equals(TEXT_MARKER_SIZE)) {
			new DETaskSetMarkerSize(getParentFrame(), mMainPane, (VisualizationPanel) mSource).defineAndRun();
		} else if (actionCommand.equals(TEXT_MARKER_SHAPE)) {
			new DETaskSetMarkerShape(getParentFrame(), mMainPane, (VisualizationPanel) mSource).defineAndRun();
		} else if (actionCommand.equals(TEXT_MARKER_COLOR)) {
			new DETaskSetMarkerColor(getParentFrame(), mMainPane, (VisualizationPanel) mSource).defineAndRun();
		} else if (actionCommand.equals(TEXT_MARKER_BG_COLOR)) {
			new DETaskSetMarkerBackgroundColor(getParentFrame(), mMainPane, (VisualizationPanel2D) mSource).defineAndRun();
		} else if (actionCommand.equals(TEXT_MARKER_TRANSPARENCY)) {
			new DETaskSetMarkerTransparency(getParentFrame(), mMainPane, (VisualizationPanel) mSource).defineAndRun();
		} else if (actionCommand.equals(TEXT_MARKER_CONNECTION)) {
			new DETaskSetConnectionLines(getParentFrame(), mMainPane, (VisualizationPanel) mSource).defineAndRun();
		} else if (actionCommand.equals(TEXT_FOCUS)) {
			new DETaskSetFocus(getParentFrame(), mMainPane, (FocusableView) mSource).defineAndRun();
		} else if (actionCommand.equals(TEXT_MARKER_JITTERING)) {
			new DETaskSetMarkerJittering(getParentFrame(), mMainPane, (VisualizationPanel) mSource).defineAndRun();
		} else if (actionCommand.equals(TEXT_MULTI_VALUE_MARKER)) {
			new DETaskSetMultiValueMarker(getParentFrame(), mMainPane, (VisualizationPanel2D) mSource).defineAndRun();
		} else if (actionCommand.equals(TEXT_SEPARATE_CASES)) {
			new DETaskSeparateCases(getParentFrame(), mMainPane, (VisualizationPanel) mSource).defineAndRun();
		} else if (actionCommand.equals(TEXT_SPLIT_VIEW)) {
			new DETaskSplitView(getParentFrame(), mMainPane, (VisualizationPanel) mSource).defineAndRun();
		} else if (actionCommand.equals(TEXT_BACKGROUND_IMAGE)) {
			new DETaskSetBackgroundImage(getParentFrame(), mMainPane, (VisualizationPanel2D) mSource).defineAndRun();
		} else if (actionCommand.equals(TEXT_GENERAL_OPTIONS)) {
			new DETaskSetGraphicalViewOptions(getParentFrame(), mMainPane, (VisualizationPanel) mSource).defineAndRun();
		} else if (actionCommand.equals(TEXT_STATISTICAL_OPTIONS)) {
			new DETaskSetStatisticalViewOptions(getParentFrame(), mMainPane, (VisualizationPanel2D)mSource).defineAndRun();
		} else if (actionCommand.startsWith(COLOR_DISPLAY_MODE)) {
			int colorModeIndex = DETaskSetStructureDisplayMode.findColorModeIndex(actionCommand.substring(COLOR_DISPLAY_MODE.length()), -1);
			new DETaskSetStructureDisplayMode(getParentFrame(), mMainPane, (JStructureGrid)mSource, -1, colorModeIndex).defineAndRun();
		} else if (actionCommand.startsWith(STEREO_DISPLAY_MODE)) {
			int stereoModeIndex = DETaskSetStructureDisplayMode.findStereoModeIndex(actionCommand.substring(STEREO_DISPLAY_MODE.length()), -1);
			new DETaskSetStructureDisplayMode(getParentFrame(), mMainPane, (JStructureGrid)mSource, stereoModeIndex, -1).defineAndRun();
		} else if (actionCommand.equals(TEXT_USE_AS_FILTER)) {
			new DETaskUseAsFilter(getParentFrame(), mMainPane, mSource).defineAndRun();
		} else if (actionCommand.equals("synchronizeToNone")) {
			new DETaskSynchronizeView(getParentFrame(), mMainPane, mMainPane.getSelectedDockable().getTitle(), null).defineAndRun();
		} else if (actionCommand.startsWith("synchronizeTo_")) {
			String slave = mMainPane.getSelectedDockable().getTitle();
			String master = actionCommand.substring("synchronizeTo_".length());
			new DETaskSynchronizeView(getParentFrame(), mMainPane, slave, master).defineAndRun();
		} else if (actionCommand.equals("Design Mode")) {
			DEFormView formView = (DEFormView)mSource;
			boolean mode = ((JCheckBoxMenuItem)e.getSource()).isSelected();
			if (formView.isEditMode())
				formView.setEditMode(false);
			formView.setDesignMode(mode);
		} else if (actionCommand.equals("Edit Mode")) {
			DEFormView formView = (DEFormView)mSource;
			boolean mode = ((JCheckBoxMenuItem)e.getSource()).isSelected();
			if (formView.isDesignMode())
				formView.setDesignMode(false);
			formView.setEditMode(mode);
		} else if (actionCommand.equals(TEXT_EDIT_WYSIWYG)) {
			// TODO convert to task
			FXExplanationEditor htmlEditor = new FXExplanationEditor(getParentFrame(), mTableModel);
			htmlEditor.setVisible(true);
		} else if (actionCommand.equals(TEXT_EDIT_HTML)) {
			new DETaskSetExplanationHTML(getParentFrame(), mTableModel).defineAndRun();
		} else if (actionCommand.equalsIgnoreCase(TEXT_RELOAD)) {
			((ExplanationView)mSource).reload();
		} else if (actionCommand.equals(TEXT_CARD_CONFIGURE)) {
			new DETaskConfigureCard(getParentFrame(), mMainPane, ((JCardView)mSource).getCardPane(), ((JCardView) mSource).getDataWarriorLink()).defineAndRun();
		} else if (actionCommand.equals(TEXT_CARD_OPTIONS)) {
			new DETaskSetCardViewOptions(getParentFrame(), mMainPane, ((JCardView)mSource).getDataWarriorLink(), ((JCardView)mSource).getCardPane()).defineAndRun();
		} else if (actionCommand.equals(TEXT_SET_FONT_SIZE)) {
			new DETaskSetFontSize(getParentFrame(), mMainPane, mSource).defineAndRun();
		} else if (actionCommand.startsWith(TEXT_SET_HEADER_LINES)) {
			int index = TEXT_SET_HEADER_LINES.length();
			int lines = Integer.parseInt(actionCommand.substring(index));
			new DETaskSetHeaderLineCount(getParentFrame(), mMainPane, mSource, lines).defineAndRun();
		} else if (e.getActionCommand().equals(TEXT_CHANGE_COLUMN_ORDER)) {
			new DETaskChangeColumnOrder(getParentFrame(), ((DETableView)mSource)).defineAndRun();
		} else if (e.getActionCommand().equals(TEXT_GROUP_SELECTED)) {
			int[] selectedColumn = ((DETableView)mSource).getSelectedColumns();
			if (selectedColumn == null)
				JOptionPane.showMessageDialog(getParentFrame(), "No columns selected.");
			else
				new DETaskAddColumnsToGroup(getParentFrame(), ((DETableView)mSource), mTableModel, selectedColumn, null).defineAndRun();
		} else if (e.getActionCommand().startsWith(TEXT_SHOW_GROUP)) {
			String groupName = e.getActionCommand().substring(TEXT_SHOW_GROUP.length());
			new DETaskShowTableColumnGroup(getParentFrame(), ((DETableView)mSource), mTableModel, groupName).defineAndRun();
		} else if (e.getActionCommand().startsWith(TEXT_ADD_TO_GROUP)) {
			int[] selectedColumn = ((DETableView)mSource).getSelectedColumns();
			if (selectedColumn == null) {
				JOptionPane.showMessageDialog(getParentFrame(), "No columns selected.");
			}
			else {
				String groupName = e.getActionCommand().substring(TEXT_ADD_TO_GROUP.length());
				new DETaskAddColumnsToGroup(getParentFrame(), ((DETableView)mSource), mTableModel, selectedColumn, groupName).defineAndRun();
			}
		} else if (e.getActionCommand().startsWith(TEXT_REMOVE_GROUP)) {
			String groupName = e.getActionCommand().substring(TEXT_REMOVE_GROUP.length());
			new DETaskRemoveColumnGroup(getParentFrame(), ((DETableView)mSource), mTableModel, groupName).defineAndRun();
		} else {
			try {
				int columnCount = Integer.parseInt(e.getActionCommand());
				new DETaskSetHorizontalStructureCount(getParentFrame(), mMainPane, (JStructureGrid)mSource, columnCount).defineAndRun();
				}
			catch (NumberFormatException nfe) {}
			}
		}

	private JMenuItem addMenuItem(String text) {
		JMenuItem item = new JMenuItem(text);
		item.addActionListener(this);
		add(item);
		return item;
		}

	private void addRadioButtonItem(JMenu menu, ButtonGroup group, String text, String command, boolean isSelected) {
		JRadioButtonMenuItem item = new JRadioButtonMenuItem(text);
		item.setSelected(isSelected);
		item.setActionCommand(command);
		item.addActionListener(this);
		group.add(item);
		menu.add(item);
		}

	private DEFrame getParentFrame() {
		Component c = (Component)mSource;
		while (c != null && !(c instanceof DEFrame))
			c = c.getParent();
		return (DEFrame)c;
		}
	}
