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

package com.actelion.research.datawarrior.task.data;

import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.SortedStringList;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.gui.ScrollPaneAutoScrollerWhenDragging;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.datawarrior.task.view.ListTransferHandler;
import com.actelion.research.gui.CompoundCollectionModel;
import com.actelion.research.gui.CompoundCollectionPane;
import com.actelion.research.gui.DefaultCompoundCollectionModel;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.gui.table.ChemistryCellRenderer;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Properties;
import java.util.TooManyListenersException;

public class DETaskSetCategoryCustomOrder extends ConfigurableTask implements ActionListener {
	public static final String TASK_NAME = "Set Category Custom Order";

	private static final String PROPERTY_COLUMN = "column";
	private static final String PROPERTY_LIST = "list";
	private static final String PROPERTY_SORT_COLUMN = "sortColumn";
	private static final String PROPERTY_SORT_MODE = "sortMode";
	private static final String PROPERTY_SORT_IS_ASCENDING = "isAscending";
	private static final String PROPERTY_IS_STRUCTURE = "isStructure";

	private static final int cSortModeSize = 0;
	private static final int cSortModeMean = 1;
	private static final int cSortModeMin = 2;
	private static final int cSortModeMax = 3;
	private static final int cSortModeSum = 4;
	private static final int cSortModeAlpha = 5;

	public static final String[] SORT_MODE_NAME = { "by row count", "by mean of", "by lowest of", "by highest of", "by sum of", "alphabetically by" };
	public static final String[] SORT_MODE_CODE = { "size", "mean", "min", "max", "sum", "alpha" };

	public static final String[] SORT_ORDER_NAME = { "in ascending order", "in descending order" };

	private JComboBox			mComboBoxColumn,mComboBoxSortMode,mComboBoxSortOrder,mComboBoxSortColumn;
	private CompoundTableModel  mTableModel;
	private JCheckBox			mCheckBoxUseCustomOrder, mCheckBoxIsStructure,mCheckBoxSort;
	private JList				mList;
	private JPanel				mDialogPanel;
	private JScrollPane			mScrollPane;
	private JTextArea			mTextArea;
	private CompoundCollectionPane<String> mStructurePane;
	private JButton				mButtonSort;
	private ListCellRenderer	mDefaultRenderer;
	private DefaultListModel	mListModel;
	private int					mDefaultColumn,mActiveSortMode,mActiveSortColumn;
	private boolean				mActiveSortIsAscending,mIsStructure,mNeglectListEvents;

	/**
	 * 
	 * @param parent
	 * @param tableModel
	 * @param defaultColumn -1 if not live
	 */
	public DETaskSetCategoryCustomOrder(Frame parent, CompoundTableModel tableModel, int defaultColumn) {
		super(parent, false);
		mTableModel = tableModel;
		mDefaultColumn = defaultColumn;
		mActiveSortMode = -1;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public JComponent createDialogContent() {
		mDialogPanel = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED,
							 gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, 3*gap/2, TableLayout.PREFERRED,
							 gap/2, TableLayout.PREFERRED, gap} };
		mDialogPanel.setLayout(new TableLayout(size));

		if (mDefaultColumn == -1) {
			mComboBoxColumn = new JComboBox();
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
				if (columnQualifies(mTableModel, column))
					mComboBoxColumn.addItem(mTableModel.getColumnTitle(column));
			mComboBoxColumn.addActionListener(this);
			mComboBoxColumn.setEditable(mDefaultColumn == -1);
			mDialogPanel.add(new JLabel("Column:"), "1,1");
			mDialogPanel.add(mComboBoxColumn, "3,1");
			}

		mCheckBoxUseCustomOrder = new JCheckBox("Use custom order");
		mCheckBoxUseCustomOrder.addActionListener(this);
		mDialogPanel.add(mCheckBoxUseCustomOrder, "1,3,3,3");

		mDialogPanel.add(new JLabel("Define order of category items:"), "1,5,3,5");

		int scaled80 = HiDPIHelper.scale(80);

		if (mDefaultColumn != -1) {
			mListModel = new DefaultListModel();
			mList = new JList(mListModel);
			mList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			mList.setDropMode(DropMode.INSERT);
			mList.setTransferHandler(new ListTransferHandler());
			mList.setDragEnabled(true);
			mList.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, mList.getFont().getSize()));
			mList.getModel().addListDataListener(new ListDataListener() {
				@Override
				public void intervalAdded(ListDataEvent e) {
					if (!mNeglectListEvents)
						mActiveSortMode = -1;
					}

				@Override
				public void intervalRemoved(ListDataEvent e) {
					if (!mNeglectListEvents)
						mActiveSortMode = -1;
					}

				@Override
				public void contentsChanged(ListDataEvent e) {
					if (!mNeglectListEvents)
						mActiveSortMode = -1;
					}
				});

			mScrollPane = new JScrollPane(mList);
			int height = Math.max(3*scaled80, Math.min(8*scaled80, (1 + mTableModel.getCategoryCount(mDefaultColumn))
						* ((mTableModel.getColumnSpecialType(mDefaultColumn) == null) ? scaled80/4 : scaled80)));
			mScrollPane.setPreferredSize(new Dimension(3*scaled80, height));

			// Hack to fix an issue with Swing's auto scrolling when dragging in a scroll pane
			ScrollPaneAutoScrollerWhenDragging scroller = new ScrollPaneAutoScrollerWhenDragging(mScrollPane, true);
			try {
				mList.getDropTarget().addDropTargetListener(new DropTargetAdapter() {
					@Override
					public void dragOver(DropTargetDragEvent dtde) {
						scroller.autoScroll();
					}

					@Override
					public void drop(DropTargetDropEvent dtde) {}
					});
				}
			catch (TooManyListenersException tmle) {}
			}
		else {
			mTextArea = new JTextArea();
			mScrollPane = new JScrollPane(mTextArea);
			mScrollPane.setPreferredSize(new Dimension(3*scaled80, 3*scaled80));
			mIsStructure = false;
			}

		mDialogPanel.add(mScrollPane, "1,7,3,7");

		if (mDefaultColumn == -1) {
			mCheckBoxIsStructure = new JCheckBox("Column contains chemical structures");
			mCheckBoxIsStructure.addActionListener(this);
			mDialogPanel.add(mCheckBoxIsStructure, "1,9,3,9");
			}

		if (mDefaultColumn != -1) {
			mButtonSort = new JButton("Sort categories");
			mButtonSort.addActionListener(this);
			mDialogPanel.add(mButtonSort, "1,11");
			}
		else {
			mCheckBoxSort = new JCheckBox("Sort Categories");
			mCheckBoxSort.addActionListener(this);
			mDialogPanel.add(mCheckBoxSort, "1,11");
			}
		mComboBoxSortOrder = new JComboBox(SORT_ORDER_NAME);
		mDialogPanel.add(mComboBoxSortOrder, "3,11");

		mComboBoxSortMode = new JComboBox(SORT_MODE_NAME);
		mComboBoxSortMode.addActionListener(this);
		mDialogPanel.add(mComboBoxSortMode, "1,13");

		mComboBoxSortColumn = new JComboBox();
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (columnQualifiesAsSortColumn(mTableModel, column, 0))
				mComboBoxSortColumn.addItem(mTableModel.getColumnTitle(column));
		mComboBoxSortColumn.addActionListener(this);
		mComboBoxSortColumn.setEditable(mDefaultColumn == -1);
		mDialogPanel.add(mComboBoxSortColumn, "3,13");

		return mDialogPanel;
		}

	public static boolean columnQualifies(CompoundTableModel tableModel, int column) {
		return !tableModel.isColumnTypeDouble(column)
			&&  tableModel.isColumnTypeCategory(column)
			&& !tableModel.isColumnTypeRangeCategory(column);
		}

	private boolean columnQualifiesAsSortColumn(CompoundTableModel tableModel, int column, int mode) {
		if (mode == cSortModeAlpha)
			return tableModel.getColumnSpecialType(column) == null;
		else
			return tableModel.isColumnTypeDouble(column)
			   &&  tableModel.getMaximumValue(column) != tableModel.getMinimumValue(column);
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		boolean isStructure = (mDefaultColumn != -1) ?
				mTableModel.isColumnTypeStructure(mDefaultColumn)
					: mCheckBoxIsStructure.isSelected();
		configuration.setProperty(PROPERTY_IS_STRUCTURE, isStructure ? "true":"false");

		String columnName = (mDefaultColumn == -1) ? (String)mComboBoxColumn.getSelectedItem()
							: mTableModel.getColumnTitleNoAlias(mDefaultColumn);
		configuration.setProperty(PROPERTY_COLUMN, columnName);
		if (mCheckBoxUseCustomOrder.isSelected()) {
			if (mDefaultColumn != -1) {
				if (mActiveSortMode != -1) {
					configuration.setProperty(PROPERTY_SORT_MODE, SORT_MODE_CODE[mActiveSortMode]);
					configuration.setProperty(PROPERTY_SORT_IS_ASCENDING, mActiveSortIsAscending ? "true" : "false");
					if (mActiveSortMode != cSortModeSize)
						configuration.setProperty(PROPERTY_SORT_COLUMN, mTableModel.getColumnTitleNoAlias(mActiveSortColumn));
					}
				else {
					StringBuilder sb = new StringBuilder((String)mListModel.elementAt(0));
					for (int i=1; i<mListModel.getSize(); i++)
						sb.append('\t').append((String)mListModel.elementAt(i));
					configuration.setProperty(PROPERTY_LIST, sb.toString());
					}
				}
			else {
				if (mCheckBoxSort.isSelected()) {
					configuration.setProperty(PROPERTY_SORT_IS_ASCENDING, mComboBoxSortOrder.getSelectedIndex()==0?"true":"false");
					configuration.setProperty(PROPERTY_SORT_MODE, SORT_MODE_CODE[mComboBoxSortMode.getSelectedIndex()]);
					if (mComboBoxSortMode.getSelectedIndex() != cSortModeSize)
						configuration.setProperty(PROPERTY_SORT_COLUMN, (String)mComboBoxSortColumn.getSelectedItem());
					}
				else {
					if (mCheckBoxIsStructure.isSelected()) {
						CompoundCollectionModel<String> model = mStructurePane.getModel();
						if (model.getSize() != 0) {
							StringBuilder sb = new StringBuilder();
							for (int i=0; i<model.getSize(); i++) {
								sb.append(model.getCompound(i));
								sb.append('\t');
								}
							configuration.put(PROPERTY_LIST, sb.toString());
							}
						}
					else {
						configuration.put(PROPERTY_LIST, mTextArea.getText().replace('\n', '\t'));
						}
					}
				}
			}
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		if (mDefaultColumn == -1) {
			int column = mTableModel.findColumn(configuration.getProperty(PROPERTY_COLUMN));
			if (column == -1)
				mComboBoxColumn.setSelectedItem(configuration.getProperty(PROPERTY_COLUMN));
			else
				mComboBoxColumn.setSelectedItem(mTableModel.getColumnTitle(column));

			mCheckBoxIsStructure.setSelected("true".equals(configuration.getProperty(PROPERTY_IS_STRUCTURE)));

			int sortMode = findListIndex(configuration.getProperty(PROPERTY_SORT_MODE), SORT_MODE_CODE, -1);
			if (sortMode == -1) {
				String itemString = configuration.getProperty(PROPERTY_LIST, "");
				mCheckBoxUseCustomOrder.setSelected(itemString.length() != 0);
				mTextArea.setText(itemString.replace('\t', '\n'));
				if ("true".equals(configuration.getProperty(PROPERTY_IS_STRUCTURE)))
					updateCategoryList(true);
				}
			else {
				mTextArea.setText("");
				mCheckBoxUseCustomOrder.setSelected(true);
				mCheckBoxSort.setSelected(true);
				mComboBoxSortMode.setSelectedIndex(sortMode);
				mComboBoxSortColumn.setSelectedItem(configuration.getProperty(PROPERTY_SORT_COLUMN, ""));
				}
			}
		else {
			boolean isCustomOrder = (mTableModel.getCategoryCustomOrder(mDefaultColumn) != null);
			mCheckBoxUseCustomOrder.setSelected(isCustomOrder);
			updateList(null, false);
			}

		enableItems();
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mDefaultColumn == -1) {
			if (mComboBoxColumn.getItemCount() != 0)
				mComboBoxColumn.setSelectedIndex(0);
			}

		boolean isCustomOrder = isInteractive()
				&& mDefaultColumn != -1
				&& (mTableModel.getCategoryCustomOrder(mDefaultColumn) != null);
		mCheckBoxUseCustomOrder.setSelected(isCustomOrder);
		updateList(null, false);
		enableItems();
		}

	private void updateRenderers(boolean isStructure) {
		if (isStructure) {
			if (mDefaultRenderer == null)
				mDefaultRenderer = mList.getCellRenderer();

			ChemistryCellRenderer renderer = new ChemistryCellRenderer();
			renderer.setAlternateRowBackground(true);
			mList.setCellRenderer(renderer);
			mList.setFixedCellHeight(80);
			}
		else {
			if (mDefaultRenderer != null) {
				mList.setCellRenderer(mDefaultRenderer);
				mList.setFixedCellHeight(-1);
				}
			}
		}

	private void updateCategoryList(boolean isStructure) {
		if (mIsStructure != isStructure) {
			mIsStructure = isStructure;
			if (isStructure) {
				String[] idcodeList = mTextArea.getText().split("\\n");
				if (mStructurePane == null) {
					DefaultCompoundCollectionModel.IDCode collectionModel = new DefaultCompoundCollectionModel.IDCode();
					int scaled80 = HiDPIHelper.scale(80);
					mStructurePane = new CompoundCollectionPane<String>(collectionModel, true);
					mStructurePane.setSelectable(true);
					mStructurePane.setEditable(true);
					mStructurePane.setInternalDragAndDropIsMove(true);
					mStructurePane.setClipboardHandler(new ClipboardHandler());
					mStructurePane.setShowValidationError(true);
					mStructurePane.setStructureSize(scaled80);
					mStructurePane.setPreferredSize(new Dimension(3*scaled80,
							Math.max(3*scaled80, Math.min(8*scaled80, scaled80 * idcodeList.length))));
					mStructurePane.setEnabled(mCheckBoxUseCustomOrder.isSelected() && !mCheckBoxSort.isSelected());
					}
				mStructurePane.getModel().clear();
				StereoMolecule mol = new StereoMolecule();
				for (String idcode:idcodeList) {
					try {
						idcode = idcode.trim();
						if (idcode.length() != 0) {
				            new IDCodeParser().parse(mol, idcode);	// test validity of idcode
							mStructurePane.getModel().addCompound(idcode);
							}
						}
					catch (Exception e) {}
					}
				mDialogPanel.remove(mScrollPane);
				mDialogPanel.add(mStructurePane, "1,7,3,7");
				mDialogPanel.validate();
				getDialog().pack();
				}
			else {
				StringBuilder sb = new StringBuilder();
				for (int i=0; i<mStructurePane.getModel().getSize(); i++) {
					sb.append(mStructurePane.getModel().getCompound(i));
					sb.append('\n');
					}
				mTextArea.setText(sb.toString());
				mDialogPanel.remove(mStructurePane);
				mDialogPanel.add(mScrollPane, "1,7,3,7");
				mDialogPanel.validate();
				getDialog().pack();
				mScrollPane.repaint();
				}
			}
		}

	private void updateSortColumnMenu(int sortMode) {
		String selectedItem = (String)mComboBoxSortColumn.getSelectedItem();
		mComboBoxSortColumn.removeAllItems();
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (columnQualifiesAsSortColumn(mTableModel, column, sortMode))
				mComboBoxSortColumn.addItem(mTableModel.getColumnTitle(column));
		if (mComboBoxSortColumn.getItemCount() != 0) {
			mComboBoxSortColumn.setSelectedIndex(0);	// default
			if (selectedItem != null)
				mComboBoxSortColumn.setSelectedItem(selectedItem);
			}
		}

	/**
	 * Clear list, then add category items in provided order, then (if !isCompleteList)
	 * add remaining category items, which were not in the provided list, in tableModel order.
	 * @param customList null or category list with intended order
	 * @param isCompleteList true if the list contains all items of tableModel
	 */
	private void updateList(String[] customList, boolean isCompleteList) {
		int column = (mDefaultColumn != -1) ? mDefaultColumn
				: mTableModel.findColumn((String)mComboBoxColumn.getSelectedItem());

		// if interactive, then set renderers
		if (mDefaultColumn != -1) {
			String specialType = (column == -1) ? null : mTableModel.getColumnSpecialType(column);
			updateRenderers(CompoundTableModel.cColumnTypeIDCode.equals(specialType)
						 || CompoundTableModel.cColumnTypeRXNCode.equals(specialType));
			mListModel.clear();
			}

		SortedStringList sortedCustomList = null;
		StringBuilder sb = (mDefaultColumn == -1) ? new StringBuilder() : null;

		// add category items from list in list order
		if (customList != null) {
			if (!isCompleteList)
				sortedCustomList = new SortedStringList();
			for (String customItem:customList) {
				if (mDefaultColumn != -1) {
					mListModel.addElement(customItem);
					}
				else {
					if (sb.length() != 0)
						sb.append('\n');
					sb.append(customItem);
					}
				if (!isCompleteList)
					sortedCustomList.addString(customItem);
				}
			}

		// add category items that were not in the list
		if (column != -1 && !isCompleteList) {
			for (String item:mTableModel.getCategoryList(column)) {
				if (!item.equals(CompoundTableModel.cTextMultipleCategories)
				 && (sortedCustomList == null || !sortedCustomList.contains(item))) {
					if (mDefaultColumn != -1) {
						mListModel.addElement(item);
						}
					else {
						if (sb.length() != 0)
							sb.append('\n');
						sb.append(item);
						}
					}
				}
			}

		if (mDefaultColumn == -1)
			mTextArea.setText(sb.toString());
		}

	@Override
	public boolean isConfigurable() {
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (columnQualifies(mTableModel, column))
				return true;

		return false;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String columnName = configuration.getProperty(PROPERTY_COLUMN, "");
		if (columnName.length() == 0) {
			showErrorMessage("Column not defined.");
			return false;
			}

		String sortColumnName = null;
		String list = configuration.getProperty(PROPERTY_LIST, "");
		if (list.length() == 0) {
			String sortMode = configuration.getProperty(PROPERTY_SORT_MODE, "");
			if (sortMode.length() != 0
			 && !sortMode.equals(SORT_MODE_CODE[cSortModeSize])) {
				sortColumnName = configuration.getProperty(PROPERTY_SORT_COLUMN, "");
				if (sortColumnName.length() == 0) {
					showErrorMessage("Sort column not defined.");
					return false;
					}
				}
			}

		if (isLive) {
			int column = mTableModel.findColumn(columnName);
			if (column == -1) {
				showErrorMessage("Column '"+columnName+"' not found.");
				return false;
				}
			if (!columnQualifies(mTableModel, column)) {
				showErrorMessage("Column '"+columnName+"' does not contain categories.");
				return false;
				}
			if (sortColumnName != null) {
				int sortColumn = mTableModel.findColumn(sortColumnName);
				if (sortColumn == -1) {
					showErrorMessage("Column '"+sortColumnName+"' not found.");
					return false;
					}
				int sortMode = findListIndex(configuration.getProperty(PROPERTY_SORT_MODE, ""), SORT_MODE_CODE, 0);
				if (!columnQualifiesAsSortColumn(mTableModel, sortColumn, sortMode)) {
					if (sortMode == cSortModeAlpha)
						showErrorMessage("Column '"+sortColumnName+"' does not contain alpha-numerical values.");
					else
						showErrorMessage("Column '"+sortColumnName+"' does not contain numerical values.");
					return false;
					}
				}
			}
		return true;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mComboBoxColumn) {
			updateList(null, false);
			}
		else if (e.getSource() == mCheckBoxUseCustomOrder) {
			if (!mCheckBoxUseCustomOrder.isSelected() && mDefaultColumn != -1)
				updateList(mTableModel.getCategoryList(mDefaultColumn), true);
			enableItems();
			}
		else if (e.getSource() == mComboBoxSortMode) {
			updateSortColumnMenu(mComboBoxSortMode.getSelectedIndex());
			if (!mCheckBoxUseCustomOrder.isSelected() && mDefaultColumn != -1)
				updateList(mTableModel.getCategoryList(mDefaultColumn), true);
			enableItems();
			}
		else if (e.getSource() == mCheckBoxSort) {
			enableItems();
			}
		else if (e.getSource() == mCheckBoxIsStructure) {
			updateCategoryList(mCheckBoxIsStructure.isSelected());
			}
		else if (e.getSource() == mButtonSort) {
			mActiveSortMode = mComboBoxSortMode.getSelectedIndex();
			mActiveSortColumn = (mActiveSortMode == cSortModeSize) ? -1 : mTableModel.findColumn((String)mComboBoxSortColumn.getSelectedItem());
			mActiveSortIsAscending = (mComboBoxSortOrder.getSelectedIndex() == 0);

			mNeglectListEvents = true;
			updateList(sortCategories(mDefaultColumn, mActiveSortMode, mActiveSortColumn, mActiveSortIsAscending, false), true);
			mNeglectListEvents = false;
			}
		}

	private void enableItems() {
		boolean enabled = mCheckBoxUseCustomOrder.isSelected();
		if (mDefaultColumn != -1) {
			mList.setEnabled(enabled);
			mButtonSort.setEnabled(enabled);
			}
		else {
			boolean listIsEnabled = enabled && !mCheckBoxSort.isSelected();
			mCheckBoxIsStructure.setEnabled(listIsEnabled);
			if (mStructurePane != null)
				mStructurePane.setEnabled(listIsEnabled);
			mTextArea.setEnabled(listIsEnabled);
			mCheckBoxSort.setEnabled(enabled);
			}
		boolean sortIsEnabled = enabled && (mCheckBoxSort == null || mCheckBoxSort.isSelected());
		mComboBoxSortOrder.setEnabled(sortIsEnabled);
		mComboBoxSortMode.setEnabled(sortIsEnabled);
		mComboBoxSortColumn.setEnabled(sortIsEnabled && mComboBoxSortMode.getSelectedIndex() != cSortModeSize);
		}

	@Override
	public void runTask(Properties configuration) {
		int column = mTableModel.findColumn(configuration.getProperty(PROPERTY_COLUMN, ""));
		String itemString = configuration.getProperty(PROPERTY_LIST, "");
		if (itemString.length() != 0) {
			String[] category = itemString.split("\\t");

			// TODO use attached id-coordinates in List and configuration
			// in case of idcodes get rid of potentially attached coordinates
			if (mTableModel.isColumnTypeStructure(column)) {
				for (int i=0; i<category.length; i++) {
					int index = category[i].indexOf(' ');
					if (index != -1)
						category[i] = category[i].substring(0, index);
					}
				}

			mTableModel.setCategoryCustomOrder(column, category);
			return;
			}

		String mode = configuration.getProperty(PROPERTY_SORT_MODE, "");
		if (mode.length() == 0) {
			mTableModel.setCategoryCustomOrder(column, null);
			return;
			}

		int sortMode = findListIndex(mode, SORT_MODE_CODE, 0);
		int sortColumn = (sortMode == cSortModeSize) ? -1 : mTableModel.findColumn(configuration.getProperty(PROPERTY_SORT_COLUMN, ""));
		boolean isAscending = !"false".equals(configuration.getProperty(PROPERTY_SORT_IS_ASCENDING));
		mTableModel.setCategoryCustomOrder(column, sortCategories(column, sortMode, sortColumn, isAscending, true));
		}

	private String[] sortCategories(int column, int sortMode, int sortColumn, final boolean isAscending, boolean removeCoords) {
		int categoryCount = mTableModel.getCategoryCount(column);
		if (mTableModel.isMultiCategoryColumn(column))
			categoryCount--;
		CategoryToSort[] category = new CategoryToSort[categoryCount];
		String[] categoryName = mTableModel.getCategoryList(column);
		if (removeCoords && mTableModel.isColumnTypeStructure(column)) {
			for (int i=0; i<categoryName.length; i++) {
				int index = categoryName[i].indexOf(' ');
				if (index != -1)
					categoryName[i] = categoryName[i].substring(0, index);
				}
			}
		for (int i=0; i<category.length; i++)
			category[i] = new CategoryToSort(categoryName[i]);
		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			int cat = mTableModel.getCategoryIndex(column, mTableModel.getTotalRecord(row));
			if (cat < categoryCount) {	// exclude rows belonging to multiple categories
				if (sortMode == cSortModeAlpha) {
					String[] entry = mTableModel.separateEntries(mTableModel.getTotalValueAt(row, sortColumn));
					for (String e:entry) {
						if (category[cat].alpha.length() == 0
						 || (isAscending && category[cat].alpha.compareTo(e) > 0)
						 || (!isAscending && category[cat].alpha.compareTo(e) < 0))
							category[cat].alpha = e;
						}
					}
				else if (sortMode == cSortModeSize) {
					category[cat].value++;
					}
				else {
					float v = mTableModel.getTotalDoubleAt(row, sortColumn);
					if (!Float.isNaN(v)) {
						category[cat].count++;
						switch (sortMode) {
						case cSortModeMean:
						case cSortModeSum:
							category[cat].value += v;
							break;
						case cSortModeMin:
						case cSortModeMax:
							if (category[cat].count == 1)
								category[cat].value = v;
							else if (sortMode == cSortModeMin)
								category[cat].value = Math.min(category[cat].value, v);
							else
								category[cat].value = Math.max(category[cat].value, v);
							break;
							}
						}
					}
				}
			}
		if (sortMode == cSortModeSize || sortMode == cSortModeAlpha) {
			for (int cat=0; cat<category.length; cat++)
				category[cat].count = 1;	// to not put empty categories at the end of the sorted list
			}
		if (sortMode == cSortModeMean) {
			for (int cat=0; cat<category.length; cat++)
				if (category[cat].count != 0)
					category[cat].value /= category[cat].count;
			}

		Arrays.sort(category, (c1, c2) -> {
			if (c1.count == 0)
				return (c2.count == 0) ? 0 : 1;
			if (c2.count == 0)
				return -1;
			if (sortMode == cSortModeAlpha)
				return (c1.alpha.equals(c2.alpha)) ? 0 : (isAscending ^ (c1.alpha.compareTo(c2.alpha)) > 0) ? -1 : 1;
			else
				return (c1.value == c2.value) ? 0 : (isAscending ^ (c1.value > c2.value)) ? -1 : 1;
			});

		String[] name = new String[category.length];
		for (int i=0; i<category.length; i++)
			name[i] = category[i].name;

		return name;
		}

	private class CategoryToSort {
		int count;
		String name;
		String alpha;
		float value;

		private CategoryToSort(String name) {
			this.name = name;
			this.alpha = "";
			}
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
