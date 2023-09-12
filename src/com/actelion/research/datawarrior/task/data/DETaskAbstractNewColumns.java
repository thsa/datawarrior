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

import com.actelion.research.datawarrior.DEFormView;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Properties;

public abstract class DETaskAbstractNewColumns extends ConfigurableTask implements ActionListener {
    private static final String PROPERTY_COLUMN_NAME_LIST = "columnNames";

    private static final String[] COLUMN_TYPE_NAME = { "Text", "Structure", "Substructure", "Reaction", "Transformation", "Weblink" };

	private static final int COLUMN_TYPE_TEXT = 0;
    private static final int COLUMN_TYPE_STRUCTURE = 1;
	private static final int COLUMN_TYPE_SUBSTRUCTURE = 2;
    private static final int COLUMN_TYPE_REACTION = 3;
	private static final int COLUMN_TYPE_TRANSFORMATION = 4;
	private static final int COLUMN_TYPE_WEBLINK = 5;
	private static final String[] DEFAULT_COLUMN_NAME = { "Column", "Structure", "Substructure", "Reaction", "Transformation", "Weblink" };

    private DataWarrior			mApplication;
	private JComboBox           mComboBox;
	private JTextField			mTextField;
	private JButton             mButtonAdd;
    private DefaultListModel	mListModel;
	private DEFrame				mNewFrame;


    public DETaskAbstractNewColumns(DataWarrior application) {
		super(application.getActiveFrame(), true);
		mApplication = application;
    	}

	@Override
	public JComponent createDialogContent() {
		JPanel p = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 8, 320, 8},
		                    {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 24, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 32, TableLayout.PREFERRED, 8} };
		p.setLayout(new TableLayout(size));

        mListModel = new DefaultListModel();
        final JList list = new JList(mListModel);
        list.addKeyListener(new KeyAdapter() {
        	@Override
        	public void keyPressed(KeyEvent e) {
        		if (e.getKeyCode() == KeyEvent.VK_DELETE) {
        			int[] selected = list.getSelectedIndices();
        			for (int i=selected.length-1; i>=0; i--)
        			mListModel.remove(selected[i]);
        			}
        		}
        	} );
        JScrollPane sp = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        p.add(sp, "3,1,3,9");

		p.add(new JLabel("Column Type:"), "1,1");
        mComboBox = new JComboBox(COLUMN_TYPE_NAME);
        mComboBox.addActionListener(this);
        p.add(mComboBox, "1,3");

        p.add(new JLabel("Column Name:"), "1,5");
        mTextField = new JTextField(getUniqueName(COLUMN_TYPE_NAME[0]));
        p.add(mTextField, "1,7");

        mButtonAdd = new JButton("Add Column");
        mButtonAdd.addActionListener(this);
        p.add(mButtonAdd, "1,9");

        return p;
		}

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == mButtonAdd) {
            String name = getUniqueName(mTextField.getText());
            mListModel.addElement(name+" ["+mComboBox.getSelectedItem()+"]");
            if (mComboBox.getSelectedIndex() != COLUMN_TYPE_TEXT)
                mComboBox.setSelectedIndex(COLUMN_TYPE_TEXT);
            mTextField.setText(getUniqueName(DEFAULT_COLUMN_NAME[COLUMN_TYPE_TEXT]+" "+(1+mListModel.getSize())));
			return;
			}

        if (e.getSource() == mComboBox) {
            if (mComboBox.getSelectedIndex() == COLUMN_TYPE_TEXT)
	            mTextField.setText(getUniqueName(DEFAULT_COLUMN_NAME[COLUMN_TYPE_TEXT]+" "+(1+mListModel.getSize())));
			else
				mTextField.setText(getUniqueName(DEFAULT_COLUMN_NAME[mComboBox.getSelectedIndex()]));
            return;
            }
    	}

	private String getUniqueName(String desiredName) {
	    String[] externalName = new String[mListModel.getSize()];
	    for (int i=0; i<mListModel.getSize(); i++) {
            String item = (String)mListModel.get(i);
	        int index = item.lastIndexOf(" [");
	        externalName[i] = item.substring(0, index);
	        }
	    CompoundTableModel refModel = createNewTable() ? null : mApplication.getActiveFrame().getTableModel();
	    return CompoundTableModel.validateColumnName(desiredName, -1, externalName, externalName.length, refModel);
	    }

	protected abstract boolean createNewTable();

	@Override
	public boolean isConfigurable() {
		return true;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		if (mListModel.getSize() != 0) {
			StringBuilder sb = new StringBuilder((String)mListModel.get(0));
		    for (int i=1; i<mListModel.getSize(); i++) {
		    	sb.append('\t');
		    	sb.append((String)mListModel.get(i));
		    	}
		    configuration.setProperty(PROPERTY_COLUMN_NAME_LIST, sb.toString());
			}
		
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String columnNames = configuration.getProperty(PROPERTY_COLUMN_NAME_LIST, "");
		if (columnNames.length() != 0) {
			String[] columnName = columnNames.split("\\t");
			for (String cn:columnName)
				mListModel.addElement(cn);
			}
		}

	@Override
	public void setDialogConfigurationToDefault() {
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String columnNames = configuration.getProperty(PROPERTY_COLUMN_NAME_LIST, "");
		if (columnNames.length() == 0) {
			showErrorMessage("No column names defined.");
			return false;
			}

		return true;
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return mNewFrame;
		}

	protected void addNewColumns(Properties configuration) {
		String titles = configuration.getProperty(PROPERTY_COLUMN_NAME_LIST, "");
		if (titles.length() == 0)
			return;

		String[] title = titles.split("\\t");
		int columnCount = title.length;
		for (String t:title) {
			if (t.endsWith(COLUMN_TYPE_NAME[COLUMN_TYPE_STRUCTURE] + "]")
			 || t.endsWith(COLUMN_TYPE_NAME[COLUMN_TYPE_SUBSTRUCTURE] + "]"))
				columnCount += 2;
			else if (t.endsWith(COLUMN_TYPE_NAME[COLUMN_TYPE_REACTION] + "]"))
				columnCount += 5;
			else if (t.endsWith(COLUMN_TYPE_NAME[COLUMN_TYPE_TRANSFORMATION] + "]"))
				columnCount += 5;
			}

        int firstNewColumn = 0;
        CompoundTableModel tableModel = null;
        DEFrame oldFrame = null;
        if (createNewTable()) {
        	mNewFrame = mApplication.getEmptyFrame("Untitled");
        	tableModel = mNewFrame.getTableModel();
            tableModel.initializeTable(1, columnCount);
        	}
        else {
        	oldFrame = mApplication.getActiveFrame();
        	tableModel = oldFrame.getTableModel();
            firstNewColumn = tableModel.addNewColumns(new String[columnCount]);
        	}

        int column = firstNewColumn;
		for (String t:title) {
            int index = t.lastIndexOf(" [");
            String columnName = t.substring(0, index);
            if (t.endsWith(COLUMN_TYPE_NAME[COLUMN_TYPE_STRUCTURE]+"]")
             || t.endsWith(COLUMN_TYPE_NAME[COLUMN_TYPE_SUBSTRUCTURE]+"]")) {
                column += tableModel.prepareStructureColumns(column, columnName, true, true);
				if (t.endsWith(COLUMN_TYPE_NAME[COLUMN_TYPE_SUBSTRUCTURE]+"]"))
					tableModel.setColumnProperty(column, CompoundTableModel.cColumnPropertyIsFragment, "true");
                }
			else if (t.endsWith(COLUMN_TYPE_NAME[COLUMN_TYPE_REACTION]+"]")) {
				column += tableModel.prepareReactionColumns(column, columnName,false,true, true, false, true, true, true, false);
				}
			else if (t.endsWith(COLUMN_TYPE_NAME[COLUMN_TYPE_TRANSFORMATION]+"]")) {
				column += tableModel.prepareReactionColumns(column, columnName,true,true, true, false, true, true, true, false);
				}
			else if (t.endsWith(COLUMN_TYPE_NAME[COLUMN_TYPE_WEBLINK]+"]")) {
				tableModel.setColumnName(columnName, column);
				tableModel.setColumnProperty(column, CompoundTableModel.cColumnPropertyLookupCount, "1");
				tableModel.setColumnProperty(column, CompoundTableModel.cColumnPropertyLookupName+"0", columnName);
				tableModel.setColumnProperty(column, CompoundTableModel.cColumnPropertyLookupURL+"0", "%s");
				tableModel.setColumnProperty(column, CompoundTableModel.cColumnPropertyLookupEncode+"0", "false");
				column++;
				}
            else {
                tableModel.setColumnName(columnName, column);
                column++;
                }
            }

		if (SwingUtilities.isEventDispatchThread()) {
			createNewViews(tableModel, firstNewColumn, oldFrame);
			}
		else {
	        try {
		        final CompoundTableModel _tableModel = tableModel;
		        final int _firstNewColumn = firstNewColumn;
		        final DEFrame _oldFrame = oldFrame;
		        SwingUtilities.invokeAndWait(() -> createNewViews(_tableModel, _firstNewColumn, _oldFrame) );
	            }
	        catch (Exception e) {}
            }
		}

	private void createNewViews(final CompoundTableModel tableModel, final int firstNewColumn, final DEFrame oldFrame) {
		if (createNewTable()) {
			tableModel.finalizeTable(CompoundTableEvent.cSpecifierNoRuntimeProperties, null);
			tableModel.setActiveRow(0);
			DEFormView form = mNewFrame.getMainFrame().getMainPane().addFormView("Form View", null, true);
			form.setEditMode(true);
			}
		else {
			tableModel.finalizeNewColumns(firstNewColumn, null);
			if (oldFrame.getMainFrame().getMainPane().getTableView() == null)
				oldFrame.getMainFrame().getMainPane().addTableView("Table", null);
			}
		}
	}
