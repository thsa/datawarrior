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

package com.actelion.research.datawarrior.task.view;

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JVisualization2D;
import com.actelion.research.table.view.VisualizationPanel;
import com.actelion.research.table.view.VisualizationPanel2D;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Properties;
import java.util.TreeSet;

public class DETaskSetMultiValueMarker extends DETaskAbstractSetViewOptions implements ListSelectionListener {
	public static final String TASK_NAME = "Set Multi-Value Marker";

	private static final String PROPERTY_COLUMNS = "columnList";
	private static final String PROPERTY_MODE = "mode";

    private JComboBox			mComboBox;
    private JList				mList;
    private DefaultListModel	mListModel;
    private JTextArea			mTextArea;

	public DETaskSetMultiValueMarker(Frame owner, DEMainPane mainPane, VisualizationPanel2D view) {
		super(owner, mainPane, view);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return false;
		}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof VisualizationPanel2D) ? null : "Multi value markers can only be used in 2D-Views.";
		}

	@Override
	public JComponent createViewOptionContent() {
		JPanel p = new JPanel();
        double[][] size = { {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8},
                            {8, TableLayout.PREFERRED, 16, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8} };
        p.setLayout(new TableLayout(size));

        mComboBox = new JComboBox(JVisualization2D.MULTI_VALUE_MARKER_MODE_TEXT);
        mComboBox.addActionListener(this);
		p.add(new JLabel("Multi-value marker mode:"), "1,1");
		p.add(mComboBox, "3,1");

		p.add(new JLabel("Select values and define order:"), "1,3,3,3");

        JScrollPane listView = null;

        if (hasInteractiveView()) {
			mListModel = new DefaultListModel();
	        mList = new JList(mListModel);
	        mList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	        mList.setTransferHandler(new ListTransferHandler());
	        mList.setDragEnabled(true);
	        mList.getSelectionModel().addListSelectionListener(this);
	        listView = new JScrollPane(mList);
	        listView.setPreferredSize(new Dimension(240, Math.max(160, Math.min(640, (8 + getQualifyingColumnCount() * 20)))));
			}
        else {
        	mTextArea = new JTextArea();
	        listView = new JScrollPane(mTextArea);
	        listView.setPreferredSize(new Dimension(240, 320));
        	}

        p.add(listView, "1,5,3,5");

        return p;
		}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (mList.getSelectedIndices().length > 1)
			actionPerformed(new ActionEvent(mList, 0, "selectionChanged"));	// causes view to update
		}

	private int getQualifyingColumnCount() {
		int count = 0;
		for (int column=0; column<getTableModel().getTotalColumnCount(); column++)
			if (getTableModel().isColumnTypeDouble(column) && getTableModel().hasNumericalVariance(column))
				count++;
		return count;
		}

	@Override
	public void addViewConfiguration(CompoundTableView view, Properties configuration) {
		JVisualization2D visualization = (JVisualization2D)((VisualizationPanel)view).getVisualization();
		int mode = visualization.getMultiValueMarkerMode();
		int[] column = visualization.getMultiValueMarkerColumns();
		if (column != null) {
			StringBuilder sb = new StringBuilder();
	    	for (int i=0; i<column.length; i++) {
	    		if (sb.length() != 0)
	    			sb.append('\t');
	    		sb.append(getTableModel().getColumnTitleNoAlias(column[i]));
	    		}
			configuration.put(PROPERTY_MODE, JVisualization2D.MULTI_VALUE_MARKER_MODE_CODE[mode]);
			configuration.put(PROPERTY_COLUMNS, sb.toString());
			}
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		int mode = mComboBox.getSelectedIndex();
		if (mode != JVisualization2D.cMultiValueMarkerModeNone) {
			configuration.put(PROPERTY_MODE, JVisualization2D.MULTI_VALUE_MARKER_MODE_CODE[mode]);
			if (hasInteractiveView()) {
				StringBuilder sb = new StringBuilder();
		    	for (int i=0; i<mListModel.getSize(); i++) {
		    		if (mList.isSelectedIndex(i)) {
			    		if (sb.length() != 0)
				    		sb.append('\t');
			    		sb.append((String)mListModel.elementAt(i));
		    			}
		    		}
				configuration.put(PROPERTY_COLUMNS, sb.toString());
				}
			else {
				configuration.put(PROPERTY_COLUMNS, mTextArea.getText().replace('\n', '\t'));
				}
			}
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		int mode = ConfigurableTask.findListIndex(configuration.getProperty(PROPERTY_MODE),
				JVisualization2D.MULTI_VALUE_MARKER_MODE_CODE, JVisualization2D.cMultiValueMarkerModeNone);
		mComboBox.setSelectedIndex(mode);

		String itemString = configuration.getProperty(PROPERTY_COLUMNS, "");
		if (hasInteractiveView()) {
			mList.setEnabled(mode != JVisualization2D.cMultiValueMarkerModeNone);
			mListModel.clear();
			String[] itemList = (itemString.length() == 0) ? null : itemString.split("\\t");

			TreeSet<String> foundItemList = new TreeSet<String>();
			if (itemList != null) {
				for (String item:itemList) {
					int column = getTableModel().findColumn(item);
					if (column != -1) {
						String title = getTableModel().getColumnTitle(column);
						mListModel.addElement(title);
						foundItemList.add(title);
						}
					}
				}

			for (int column=0; column<getTableModel().getTotalColumnCount(); column++)
				if (getTableModel().isColumnTypeDouble(column) && getTableModel().hasNumericalVariance(column))
					if (!foundItemList.contains(getTableModel().getColumnTitle(column)))
						mListModel.addElement(getTableModel().getColumnTitle(column));

			mList.setSelectionInterval(0, foundItemList.size()-1);
			}
		else {
			mTextArea.setText(itemString.replace('\t', '\n'));
			}
		}

	@Override
	public void setDialogToDefault() {
		mComboBox.setSelectedItem(JVisualization2D.cMultiValueMarkerModeNone);
		if (hasInteractiveView()) {
			mList.setEnabled(false);
			mListModel.clear();
			for (int column=0; column<getTableModel().getTotalColumnCount(); column++)
				if (getTableModel().isColumnTypeDouble(column) && getTableModel().hasNumericalVariance(column))
					mListModel.addElement(getTableModel().getColumnTitle(column));
			}
		else {
			mTextArea.setText("");
			}
		}

	@Override
	public boolean isConfigurable() {
		if (!super.isConfigurable())
			return false;
		if (getQualifyingColumnCount() < 2) {
			showErrorMessage("Multi-value markers need at least two numerical columns.");
			return false;
			}
		return true;
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		int mode = ConfigurableTask.findListIndex(configuration.getProperty(PROPERTY_MODE),
				JVisualization2D.MULTI_VALUE_MARKER_MODE_CODE, JVisualization2D.cMultiValueMarkerModeNone);

		if (mode != JVisualization2D.cMultiValueMarkerModeNone) {
			String columnString = configuration.getProperty(PROPERTY_COLUMNS, "");
			if (columnString.length() != 0) {
				String[] columnName = columnString.split("\\t");
				if (columnName.length < 2) {
					showErrorMessage("Multi-value markers need at least two associated columns.");
					return false;
					}
				if (view != null) {
					int[] column =  getQualifyingAndSelectedColumns(columnName);
					if (column == null || column.length < 2) {
						showErrorMessage("Less than two matching columns found.");
						return false;
						}
					}
				}
			}

		return true;
		}

	private int[] getQualifyingAndSelectedColumns(String[] columnName) {
		int count = 0;
		for (String cn:columnName) {
			int column = getTableModel().findColumn(cn);
			if (column != -1
			 && getTableModel().isColumnTypeDouble(column)
			 && getTableModel().hasNumericalVariance(column))
				count++;
			}

		if (count == 0)
			return null;

		int[] foundColumn = new int[count];
		count = 0;
		for (String cn:columnName) {
			int column = getTableModel().findColumn(cn);
			if (column != -1
			 && getTableModel().isColumnTypeDouble(column)
			 && getTableModel().hasNumericalVariance(column))
				foundColumn[count++] = column;
			}
		return foundColumn;
		}

	@Override
	public void enableItems() {
		boolean isEnabled = (mComboBox.getSelectedIndex() != JVisualization2D.cMultiValueMarkerModeNone);
		if (hasInteractiveView())
			mList.setEnabled(isEnabled);
		else
			mTextArea.setEnabled(isEnabled);
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		if (view instanceof VisualizationPanel2D) {
			JVisualization2D visualization = (JVisualization2D)((VisualizationPanel2D)view).getVisualization();
			int mode = ConfigurableTask.findListIndex(configuration.getProperty(PROPERTY_MODE),
					JVisualization2D.MULTI_VALUE_MARKER_MODE_CODE, JVisualization2D.cMultiValueMarkerModeNone);
			String columnString = configuration.getProperty(PROPERTY_COLUMNS, "");
			if (columnString.length() == 0) {
				visualization.setMultiValueMarkerColumns(null, JVisualization2D.cMultiValueMarkerModeNone);
				}
			else {
				int[] column = getQualifyingAndSelectedColumns(columnString.split("\\t"));
				visualization.setMultiValueMarkerColumns(column, mode);
				}
			}
		}
	}
