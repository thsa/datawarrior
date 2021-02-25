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

import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.task.data.*;
import com.actelion.research.datawarrior.task.filter.DETaskAddNewFilter;
import com.actelion.research.datawarrior.task.table.*;
import com.actelion.research.gui.JProgressDialog;
import com.actelion.research.gui.JScrollableMenu;
import com.actelion.research.table.filter.JCategoryFilterPanel;
import com.actelion.research.table.filter.JFilterPanel;
import com.actelion.research.table.model.CompoundTableModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

public class DETablePopupMenu extends JPopupMenu implements ActionListener {
    private static final long serialVersionUID = 0x20060904;

    private static final String SET_COLUMN_ALIAS = "Set Column Alias...";
    private static final String SET_COLUMN_DESCRIPTION = "Set Column Description...";
	private static final String SET_COLUMN_REFERENCE = "Set Column Reference...";
	private static final String SET_COLUMN_DATA_TYPE = "Set Column Data Type To";
    private static final String SET_CATEGORY_CUSTOM_ORDER = "Set Category Custom Order...";
	private static final String UPDATE_FORMULA = "Update Formula And Re-Calculate...";
	private static final String RECALCULATE_ALL = "Re-Calculate All Columns";
    private static final String NEW_STRUCTURE_FILTER = "New Structure Filter";
    private static final String NEW_SSS_LIST_FILTER = "New SSS-List Filter";
    private static final String NEW_SIM_LIST_FILTER = "New Sim-List Filter";
    private static final String NEW_REACTION_FILTER = "New Reaction Filter";
	private static final String NEW_RETRON_FILTER = "New Retron Filter";
	private static final String NEW_REACTANT_FILTER = "New Reactant Structure Filter";
	private static final String NEW_PRODUCT_FILTER = "New Product Structure Filter";
    private static final String NEW_TEXT_FILTER = "New Text Filter";
    private static final String NEW_SLIDER_FILTER = "New Slider Filter";
    private static final String NEW_CATEGORY_FILTER = "New Category Filter";
    private static final String HIDE_VALUE_COUNT = "Hide Value Count";
    private static final String SHOW_STD_DEVIATION = "Show Standard Deviation";
    private static final String SHOW_ROUNDED_VALUES = "Show Rounded Values...";
    private static final String EXCLUDE_MODIFIER_VALUES = "Exclude Values With Modifiers";
    private static final String SET_STRUCTURE_COLOR = "Set Structure Color...";
	private static final String SET_REACTION_COLOR = "Set Structure Color...";
    private static final String SET_TEXT_COLOR = "Set Text Color...";
    private static final String SET_BACKGROUND_COLOR = "Set Background Color...";
    private static final String WRAP_TEXT = "Wrap Text";
    private static final String HIDE = "Hide '";
	private static final String HIDE_SELECTED = "Hide Selected Columns";
	private static final String SHOW_ALL = "Show All Columns";
    private static final String SHOW = "Show '";
    private static final String DELETE = "Delete '";
	private static final String DELETE_SELECTED = "Delete Selected Columns";
	private static final String DELETE_HIDDEN = "Delete Hidden Columns";
	private static final String DUPLICATE = "Duplicate '";
	private static final String TYPE = "type:";
	private static final String SUMMARY = "summary:";
	private static final String HILITE_STRUCTURE = "hiliteS:";
	private static final String HILITE_REACTION = "hiliteR:";

    private CompoundTableModel	mTableModel;
	private int					mColumn;
	private Frame    			mParentFrame;
	private DEParentPane		mParentPane;
	private DETableView         mTableView;

	public DETablePopupMenu(Frame parent, DEParentPane parentPane, DETableView tableView, int column) {
		super();

        mParentFrame = parent;
        mParentPane = parentPane;
        mTableView = tableView;
		mTableModel = tableView.getTableModel();
		mColumn = column;

		String specialType = mTableModel.getColumnSpecialType(column);

		if (specialType != null) {
			if (specialType.equals(CompoundTableModel.cColumnTypeIDCode)) {
				JMenu filterMenu = new JMenu("New Structure Filter");
				add(filterMenu);
				addItem(filterMenu, "Single Structure", NEW_STRUCTURE_FILTER);
				addItem(filterMenu, "Substructure List", NEW_SSS_LIST_FILTER);
				addItem(filterMenu, "Similar Structure List", NEW_SIM_LIST_FILTER);
				}
			else if (specialType.equals(CompoundTableModel.cColumnTypeRXNCode)) {
				addItem(NEW_REACTION_FILTER);
				addItem(NEW_RETRON_FILTER);
				addItem(NEW_REACTANT_FILTER);
				addItem(NEW_PRODUCT_FILTER);
				}
			}
		else {
			if (mTableModel.isColumnTypeString(column))
				addItem(NEW_TEXT_FILTER);

			if (mTableModel.isColumnTypeDouble(column) && mTableModel.hasNumericalVariance(column))
				addItem(NEW_SLIDER_FILTER);

			if (mTableModel.isColumnTypeCategory(column)
			 && mTableModel.getCategoryCount(column) < JCategoryFilterPanel.cMaxCheckboxCount)
				addItem(NEW_CATEGORY_FILTER);

			if (mTableModel.isColumnTypeRangeCategory(column))
				addItem(NEW_SLIDER_FILTER);
			}

		if (getComponentCount() != 0)
			addSeparator();

		addItem(SET_COLUMN_ALIAS);
		addItem(SET_COLUMN_DESCRIPTION);
		addItem(SET_COLUMN_REFERENCE);

		if (specialType == null) {
			JMenu dataTypeMenu = new JMenu(SET_COLUMN_DATA_TYPE);
			add(dataTypeMenu);
			for (int i=0; i<CompoundTableConstants.cDataTypeText.length; i++) {
				JRadioButtonMenuItem dataTypeItem = new JRadioButtonMenuItem(CompoundTableConstants.cDataTypeText[i],
						mTableModel.getExplicitDataType(column) == i);
				dataTypeItem.addActionListener(this);
				dataTypeItem.setActionCommand(TYPE+CompoundTableConstants.cDataTypeCode[i]);
				dataTypeMenu.add(dataTypeItem);
				}
			}

		if (DETaskSetCategoryCustomOrder.columnQualifies(mTableModel, column)) {
	        addSeparator();
			addItem(SET_CATEGORY_CUSTOM_ORDER);
			}

		String formula = mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyFormula);
		if (formula != null) {
			addSeparator();
			addItem(UPDATE_FORMULA);

			for (int i=0; i<mTableModel.getTotalColumnCount(); i++) {
				if (i != column && mTableModel.getColumnProperty(i, CompoundTableConstants.cColumnPropertyFormula) != null) {
					addItem(RECALCULATE_ALL);
					break;
					}
				}
			}

 		if (mTableModel.isColumnTypeDouble(column)) {
			addSeparator();
			JMenu summaryModeMenu = new JMenu("Show Multiple Values As");
            add(summaryModeMenu);
            for (int i=0; i<CompoundTableConstants.cSummaryModeText.length; i++) {
                JRadioButtonMenuItem summaryItem = new JRadioButtonMenuItem(CompoundTableConstants.cSummaryModeText[i],
                        mTableModel.getColumnSummaryMode(column) == i);
                summaryItem.addActionListener(this);
	            summaryItem.setActionCommand(SUMMARY+CompoundTableConstants.cSummaryModeCode[i]);
                summaryModeMenu.add(summaryItem);
                }
            summaryModeMenu.addSeparator();
            JCheckBoxMenuItem menuItem1 = new JCheckBoxMenuItem(HIDE_VALUE_COUNT);
            menuItem1.setState(mTableModel.isColumnSummaryCountHidden(column));
            menuItem1.setEnabled(mTableModel.getColumnSummaryMode(column) != CompoundTableConstants.cSummaryModeNormal);
            menuItem1.addActionListener(this);
            summaryModeMenu.add(menuItem1);

            if (!mTableModel.isColumnTypeDate(column)) {
	            JCheckBoxMenuItem menuItem2 = new JCheckBoxMenuItem(SHOW_STD_DEVIATION);
	            menuItem2.setState(mTableModel.isColumnStdDeviationShown(column));
	            menuItem2.setEnabled(mTableModel.getColumnSummaryMode(column) == CompoundTableConstants.cSummaryModeMean);
	            menuItem2.addActionListener(this);
	            summaryModeMenu.add(menuItem2);

	            addItem(SHOW_ROUNDED_VALUES);

	            if (mTableModel.isColumnWithModifiers(column)) {
	                addCheckBoxMenuItem(EXCLUDE_MODIFIER_VALUES, mTableModel.getColumnModifierExclusion(column));
	                }
			    }
			}

		if (mTableModel.getColumnSpecialType(column) == null) {
            addSeparator();
            addCheckBoxMenuItem(WRAP_TEXT, mTableView.getTextWrapping(column));
		    }

		addSeparator();
        if (CompoundTableModel.cColumnTypeIDCode.equals(specialType)) {
			JMenu hiliteModeMenu = new JMenu("Highlight Structure By");
            add(hiliteModeMenu);
            for (int i=0; i<CompoundTableConstants.cStructureHiliteModeText.length; i++) {
                JRadioButtonMenuItem hiliteItem = new JRadioButtonMenuItem(CompoundTableConstants.cStructureHiliteModeText[i],
                        mTableModel.getHiliteMode(mColumn) == i);
                hiliteItem.addActionListener(this);
	            hiliteItem.setActionCommand(HILITE_STRUCTURE+CompoundTableConstants.cStructureHiliteModeCode[i]);
                hiliteModeMenu.add(hiliteItem);
                }
        	addItem(SET_STRUCTURE_COLOR);
        	}
		else if (CompoundTableModel.cColumnTypeRXNCode.equals(specialType)) {
			JMenu hiliteModeMenu = new JMenu("Highlight Reaction By");
			add(hiliteModeMenu);
			for (int i=0; i<CompoundTableConstants.cReactionHiliteModeText.length; i++) {
				JRadioButtonMenuItem hiliteItem = new JRadioButtonMenuItem(CompoundTableConstants.cReactionHiliteModeText[i],
						mTableModel.getHiliteMode(mColumn) == i);
				hiliteItem.addActionListener(this);
				hiliteItem.setActionCommand(HILITE_REACTION+CompoundTableConstants.cReactionHiliteModeCode[i]);
				hiliteModeMenu.add(hiliteItem);
				}
			addItem(SET_REACTION_COLOR);
			}
        else {
        	addItem(SET_TEXT_COLOR);
        	}
		addItem(SET_BACKGROUND_COLOR);

		addSeparator();
		addItem(DUPLICATE+mTableModel.getColumnTitle(column)+"'");

		int[] hiddenColumn = mTableView.createHiddenColumnList();

		int selectionCount = 0;
		JTable table = tableView.getTable();
		for (int i=0; i<table.getColumnCount(); i++)
			if (table.isColumnSelected(i))
				selectionCount++;

		addSeparator();
		if (selectionCount > 1)
			addItem(DELETE_SELECTED);
		addItem(DELETE+mTableModel.getColumnTitle(column)+"'");
		if (hiddenColumn.length != 0)
			addItem(DELETE_HIDDEN);

		addSeparator();

		if (selectionCount > 1)
			addItem(HIDE_SELECTED);
		addItem(HIDE+mTableModel.getColumnTitle(column)+"'");

		if (hiddenColumn.length != 0) {
			addSeparator();

			if (hiddenColumn.length == 1)
				addItem(SHOW+mTableModel.getColumnTitle(hiddenColumn[0])+"'");
			else {
				addItem(SHOW_ALL);
				JMenu showMenu = new JScrollableMenu("Show Column");
				add(showMenu);
				for (int hd:hiddenColumn)
					addItem(showMenu, mTableModel.getColumnTitle(hd), SHOW+mTableModel.getColumnTitle(hd)+"'");
				}
			}
		}

	private void addItem(String text) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.addActionListener(this);
        add(menuItem);
	    }

	private void addItem(JMenuItem menu, String text, String actionCommand) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.setActionCommand(actionCommand);
        menuItem.addActionListener(this);
        menu.add(menuItem);
	    }

	private void addCheckBoxMenuItem(String text, boolean state) {
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(text);
        menuItem.setState(state);
        menuItem.addActionListener(this);
        add(menuItem);
	    }

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.startsWith(TYPE)) {
			int type = decodeItem(command.substring(TYPE.length()), CompoundTableConstants.cDataTypeCode);
			new DETaskSetColumnDataType(mParentFrame, mTableModel, mColumn, type).defineAndRun();
			return;
			}
		if (command.startsWith(SUMMARY)) {
			int summaryMode = decodeItem(command.substring(SUMMARY.length()), CompoundTableConstants.cSummaryModeCode);
            new DETaskSetNumericalColumnDisplayMode(mParentFrame, mTableModel, mColumn, summaryMode, -1, -1, -1, -1).defineAndRun();
	        return;
	        }
		if (command.startsWith(HILITE_STRUCTURE)) {
			int hiliteMode = decodeItem(command.substring(HILITE_STRUCTURE.length()), CompoundTableConstants.cStructureHiliteModeCode);
	        new DETaskSetStructureHiliteMode(mParentFrame, mTableModel, mColumn, hiliteMode).defineAndRun();
            return;
	        }
		if (command.startsWith(HILITE_REACTION)) {
		int hiliteMode = decodeItem(command.substring(HILITE_REACTION.length()), CompoundTableConstants.cReactionHiliteModeCode);
			new DETaskSetReactionHiliteMode(mParentFrame, mTableModel, mColumn, hiliteMode).defineAndRun();
			return;
			}
		if (e.getActionCommand().equals(SET_COLUMN_ALIAS)) {
			String alias = (String)JOptionPane.showInputDialog(
					mParentFrame,
					"Column Alias to be used for '"+mTableModel.getColumnTitleNoAlias(mColumn)+"'",
					"Set Column Alias",
					JOptionPane.QUESTION_MESSAGE,
					null,
					null,
					mTableModel.getColumnTitle(mColumn));
			if (alias != null)    // if not canceled
				new DETaskSetColumnAlias(mParentFrame, mTableModel, mColumn, alias).defineAndRun();
			}
		if (e.getActionCommand().equals(SET_COLUMN_DESCRIPTION)) {
			String description = (String)JOptionPane.showInputDialog(
					mParentFrame,
					"Column Description for '"+mTableModel.getColumnTitle(mColumn)+"'",
					"Set Column Description",
					JOptionPane.QUESTION_MESSAGE,
					null,
					null,
					mTableModel.getColumnDescription(mColumn));
			if (description != null)	// if not canceled
				new DETaskSetColumnDescription(mParentFrame, mTableModel, mColumn, description).defineAndRun();
			}
		if (e.getActionCommand().equals(SET_COLUMN_REFERENCE)) {
			new DETaskSetColumnReference(mParentFrame, mTableModel, mColumn).defineAndRun();
			}
		else if (e.getActionCommand().equals(UPDATE_FORMULA)) {
			String formula = mTableModel.getColumnProperty(mColumn, CompoundTableConstants.cColumnPropertyFormula);
			new DETaskAddCalculatedValues(mParentFrame, mTableModel, mColumn, formula, true).defineAndRun();
			}
		else if (e.getActionCommand().equals(RECALCULATE_ALL)) {
			final JProgressDialog progressDialog = new JProgressDialog(mParentFrame);
			// we need to make sure that cilumns are calculated one after another
			Thread calculationThread = new Thread(() -> {
				for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
					String formula = mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyFormula);
					if (formula != null) {
						DETaskAddCalculatedValues task = new DETaskAddCalculatedValues(mParentFrame, mTableModel, column, formula, false);
						Properties configuration = task.getPredefinedConfiguration();
						task.execute(configuration, progressDialog);
						}
					}
				progressDialog.close(mParentFrame);
				});
			calculationThread.setPriority(Thread.MIN_PRIORITY);
			calculationThread.start();
			}
		else if (e.getActionCommand().equals(SET_CATEGORY_CUSTOM_ORDER)) {
			new DETaskSetCategoryCustomOrder(mParentFrame, mTableModel, mColumn).defineAndRun();
			}
        else if (e.getActionCommand().startsWith("New ") && e.getActionCommand().endsWith(" Filter")) {
            DEPruningPanel pruningPanel = mTableView.getParentPane().getPruningPanel();
            if (e.getActionCommand().equals(NEW_STRUCTURE_FILTER)) {
                addDefaultDescriptors(mColumn);
            	new DETaskAddNewFilter(mParentFrame, pruningPanel, mColumn, JFilterPanel.FILTER_TYPE_STRUCTURE, null).defineAndRun();
                }
            else if (e.getActionCommand().equals(NEW_SSS_LIST_FILTER)) {
                addDefaultDescriptors(mColumn);
            	new DETaskAddNewFilter(mParentFrame, pruningPanel, mColumn, JFilterPanel.FILTER_TYPE_SSS_LIST, null).defineAndRun();
                }
            else if (e.getActionCommand().equals(NEW_SIM_LIST_FILTER)) {
                addDefaultDescriptors(mColumn);
            	new DETaskAddNewFilter(mParentFrame, pruningPanel, mColumn, JFilterPanel.FILTER_TYPE_SIM_LIST, null).defineAndRun();
                }
            else if (e.getActionCommand().equals(NEW_REACTION_FILTER)) {
                addDefaultDescriptors(mColumn);
            	new DETaskAddNewFilter(mParentFrame, pruningPanel, mColumn, JFilterPanel.FILTER_TYPE_REACTION, null).defineAndRun();
                }
			else if (e.getActionCommand().equals(NEW_RETRON_FILTER)) {
				addDefaultDescriptors(mColumn);
				new DETaskAddNewFilter(mParentFrame, pruningPanel, mColumn, JFilterPanel.FILTER_TYPE_RETRON, null).defineAndRun();
				}
			else if (e.getActionCommand().equals(NEW_REACTANT_FILTER)) {
				addDefaultDescriptors(mColumn);
				new DETaskAddNewFilter(mParentFrame, pruningPanel, mColumn, JFilterPanel.FILTER_TYPE_STRUCTURE, CompoundTableConstants.cReactionPartReactants).defineAndRun();
				}
			else if (e.getActionCommand().equals(NEW_PRODUCT_FILTER)) {
				addDefaultDescriptors(mColumn);
				new DETaskAddNewFilter(mParentFrame, pruningPanel, mColumn, JFilterPanel.FILTER_TYPE_STRUCTURE, CompoundTableConstants.cReactionPartProducts).defineAndRun();
				}
            else if (e.getActionCommand().equals(NEW_TEXT_FILTER)) {
            	new DETaskAddNewFilter(mParentFrame, pruningPanel, mColumn, JFilterPanel.FILTER_TYPE_TEXT, null).defineAndRun();
                }
            else if (e.getActionCommand().equals(NEW_SLIDER_FILTER)) {
            	new DETaskAddNewFilter(mParentFrame, pruningPanel, mColumn, JFilterPanel.FILTER_TYPE_DOUBLE, null).defineAndRun();
                }
            else if (e.getActionCommand().equals(NEW_CATEGORY_FILTER)) {
            	new DETaskAddNewFilter(mParentFrame, pruningPanel, mColumn, JFilterPanel.FILTER_TYPE_CATEGORY, null).defineAndRun();
                }
            }
        else if (e.getActionCommand().equals(HIDE_VALUE_COUNT)) {
			new DETaskSetNumericalColumnDisplayMode(mParentFrame, mTableModel, mColumn, -1,
					mTableModel.isColumnSummaryCountHidden(mColumn) ? 0 : 1, -1, -1, -1).defineAndRun();
			}
        else if (e.getActionCommand().equals(SHOW_STD_DEVIATION)) {
			new DETaskSetNumericalColumnDisplayMode(mParentFrame, mTableModel, mColumn, -1, -1,
					mTableModel.isColumnStdDeviationShown(mColumn) ? 1 : 0, -1, -1).defineAndRun();
			}
        else if (e.getActionCommand().equals(SHOW_ROUNDED_VALUES)) {
            int oldDigits = mTableModel.getColumnSignificantDigits(mColumn);
            String selection = (String)JOptionPane.showInputDialog(
                    mParentFrame,
                    "Number of significant digits:",
                    "Display Rounded Value",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
		            DETaskSetNumericalColumnDisplayMode.ROUNDING_TEXT,
		            DETaskSetNumericalColumnDisplayMode.ROUNDING_TEXT[oldDigits]);
            if (selection != null) {// if not cancelled
                int newDigits = 0;
                while (!selection.equals(DETaskSetNumericalColumnDisplayMode.ROUNDING_TEXT[newDigits]))
                    newDigits++;

                if (newDigits != oldDigits)  // if changed
	                new DETaskSetNumericalColumnDisplayMode(mParentFrame, mTableModel, mColumn, -1, -1, -1, newDigits, -1).defineAndRun();
                }
            }
        else if (e.getActionCommand().equals(EXCLUDE_MODIFIER_VALUES)) {
			new DETaskSetNumericalColumnDisplayMode(mParentFrame, mTableModel, mColumn, -1, -1, -1, -1, mTableModel.getColumnModifierExclusion(mColumn) ? 0 : 1).defineAndRun();
            }
        else if (e.getActionCommand().equals(SET_TEXT_COLOR)
       		  || e.getActionCommand().equals(SET_STRUCTURE_COLOR)
			  || e.getActionCommand().equals(SET_REACTION_COLOR)) {
        	new DETaskSetTextColor(mParentFrame, mParentPane.getMainPane(), mTableView, mColumn).defineAndRun();
        	}
        else if (e.getActionCommand().equals(SET_BACKGROUND_COLOR)) {
        	new DETaskSetTextBackgroundColor(mParentFrame, mParentPane.getMainPane(), mTableView, mColumn).defineAndRun();
        	}
        else if (e.getActionCommand().equals(WRAP_TEXT)) {
			new DETaskSetTextWrapping(mParentFrame, mTableView, convertToList(mColumn), !mTableView.getTextWrapping(mColumn)).defineAndRun();
            }
        else if (e.getActionCommand().startsWith(HIDE)) {
            new DETaskHideTableColumns(mParentFrame, mTableView, mTableModel, convertToList(mColumn)).defineAndRun();
            }
		else if (e.getActionCommand().startsWith(HIDE_SELECTED)) {
			int[] selectedColumn = mTableView.getSelectedColumns();
			if (selectedColumn == null)
				JOptionPane.showMessageDialog(mParentFrame, "No columns selected.");
			else
				new DETaskHideTableColumns(mParentFrame, mTableView, mTableModel, selectedColumn).defineAndRun();
			}
		else if (e.getActionCommand().startsWith(SHOW_ALL)) {
			new DETaskShowTableColumns(mParentFrame, mTableView, mTableView.createHiddenColumnList()).defineAndRun();
			}
        else if (e.getActionCommand().startsWith(SHOW)) {
        	int column = mTableModel.findColumn(e.getActionCommand().substring(SHOW.length(), e.getActionCommand().length()-1));
            new DETaskShowTableColumns(mParentFrame, mTableView, convertToList(column)).defineAndRun();
            }
		else if (e.getActionCommand().startsWith(DUPLICATE)) {
			new DETaskDuplicateColumn(mParentFrame, mTableModel, mColumn).defineAndRun();
			}
		else if (e.getActionCommand().startsWith(DELETE)) {
	        int doDelete = JOptionPane.showConfirmDialog(mParentFrame,
                    "Do you really want to delete the column '"+mTableModel.getColumnTitle(mColumn)+"'?",
                    "Delete Column?",
                    JOptionPane.OK_CANCEL_OPTION);
	        if (doDelete == JOptionPane.OK_OPTION)
	        	new DETaskDeleteColumns(mParentFrame, mTableModel, convertToList(mColumn)).defineAndRun();
			}
		else if (e.getActionCommand().startsWith(DELETE_SELECTED)) {
			int[] selectedColumn = mTableView.getSelectedColumns();
			if (selectedColumn == null) {
				JOptionPane.showMessageDialog(mParentFrame, "No columns selected.");
				}
			else {
				int doDelete = JOptionPane.showConfirmDialog(mParentFrame,
						"Do you really want to delete all selected columns?",
						"Delete Column?",
						JOptionPane.OK_CANCEL_OPTION);
				if (doDelete == JOptionPane.OK_OPTION)
					new DETaskDeleteColumns(mParentFrame, mTableModel, selectedColumn).defineAndRun();
				}
			}
		else if (e.getActionCommand().equals(DELETE_HIDDEN)) {
			int doDelete = JOptionPane.showConfirmDialog(mParentFrame,
					"Do you really want to delete all hidden columns?",
					"Delete Hidden Columns?",
					JOptionPane.OK_CANCEL_OPTION);
			if (doDelete == JOptionPane.OK_OPTION)
				new DETaskDeleteColumns(mParentFrame, mTableModel, mTableView.createHiddenColumnList()).defineAndRun();
			}
		}

	private int[] convertToList(int column) {
    	int[] columnList = new int[1];
    	columnList[0] = column;
		return columnList;
		}

	/**
	 * Tries to find item in itemList. If successful it returns the list index.
	 * If item is null or item is not found, defaultIndex is returned.
	 * @param item
	 * @param itemList
	 * @return
	 */
	private int decodeItem(String item, String[] itemList) {
		for (int i=0; i<itemList.length; i++)
			if (item.equals(itemList[i]))
				return i;
		return -1;  // should never happen
		}

	private void addDefaultDescriptors(int column) {
		if (mTableModel.isColumnTypeStructure(column)) {
			String ffpName = DescriptorConstants.DESCRIPTOR_FFP512.shortName;
			if (!mTableModel.hasDescriptorColumn(column, ffpName, null))
				mTableModel.addDescriptorColumn(column, ffpName, null);
			}

		if (mTableModel.isColumnTypeReaction(column)) {
			String ffpName = DescriptorConstants.DESCRIPTOR_FFP512.shortName;
			if (!mTableModel.hasDescriptorColumn(column, ffpName, CompoundTableConstants.cReactionPartReactants))
				mTableModel.addDescriptorColumn(column, ffpName, CompoundTableConstants.cReactionPartReactants);
			if (!mTableModel.hasDescriptorColumn(column, ffpName, CompoundTableConstants.cReactionPartProducts))
				mTableModel.addDescriptorColumn(column, ffpName, CompoundTableConstants.cReactionPartProducts);

			String rxnDescName = DescriptorConstants.DESCRIPTOR_ReactionFP.shortName;
			if (!mTableModel.hasDescriptorColumn(column, rxnDescName, null))
				mTableModel.addDescriptorColumn(column, rxnDescName, null);
			}
        }
    }
