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

package com.actelion.research.datawarrior.task.filter;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.filter.*;
import info.clearthought.layout.TableLayout;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEPruningPanel;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.RuntimePropertyEvent;
import com.actelion.research.table.filter.JRangeFilterPanel;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2013-2017
 * Company:
 * @author
 * @version 1.0
 */

public class DETaskAddNewFilter extends ConfigurableTask implements ActionListener {
	public static final String TASK_NAME = "Create Filter";

	// Order matches the JFilterPanel.FILTER_TYPE_??? types defined in JFilterPanel
	protected static final String[] FILTER_NAME = {
    	"[Text]",
    	"[Slider]",
    	"[Category]",
    	"[Structure]",
    	"[Structure List,SSS]",
    	"[Structure List,Similarity]",
    	"[Reaction]",
		"[Retron]",
    	"[Row List]",
    	"[Category Browser]"
    	};

	// Order matches the JFilterPanel.FILTER_TYPE_??? types defined in JFilterPanel
    protected static final String[] FILTER_CODE = {
    	"text",
    	"slider",
    	"category",
    	"structure",
    	"sssList",
    	"simList",
    	"reaction",
		"retron",
    	"list",
    	"browser"
    	};

	// Order matches the JFilterPanel.FILTER_TYPE_??? types defined in JFilterPanel
    protected static final boolean[] FILTER_NEEDS_COLUMN = {
    	true,
    	true,
    	true,
    	true,
		true,
		true,
		true,
		true,
    	false,
    	false
    	};

    private static final String PROPERTY_SHOW_DUPLICATES = "showDuplicates";
    private static final String PROPERTY_FILTER_COUNT = "filterCount";
    private static final String PROPERTY_FILTER = "filter";

	private CompoundTableModel  mTableModel;
	private DEPruningPanel      mPruningPanel;
	private JList				mFilterList;
	private JCheckBox			mCheckBox;
	private JTextArea			mTextArea;
	private int					mColumn,mFilterType;
	private String				mReactionPart;

    public DETaskAddNewFilter(DEFrame parent, DEPruningPanel pruningPanel) {
		super(parent, false);

		mPruningPanel = pruningPanel;
		mTableModel = pruningPanel.getTableModel();
		mFilterType = -1;	// if interactive, then show dialog
    	}

    /**
     * Instantiates this task interactively with a pre-defined configuration.
     * @param parent
     * @param column
     * @param filterType
     */
    public DETaskAddNewFilter(Frame parent, DEPruningPanel pruningPanel, int column, int filterType, String reactionPart) {
		super(parent, false);
    	
		mPruningPanel = pruningPanel;
		mTableModel = pruningPanel.getTableModel();
		mFilterType = filterType;
		mColumn = column;
		mReactionPart = reactionPart;
    	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mCheckBox) {
			populateFilterList(mCheckBox.isSelected());
			return;
			}
		}

	@Override
	public Properties getPredefinedConfiguration() {
		if (mFilterType == -1)
			return null;

		Properties configuration = new Properties();
	    configuration.setProperty(PROPERTY_FILTER_COUNT, "1");
		String columnName = !FILTER_NEEDS_COLUMN[mFilterType] ? null
						  : mColumn == JFilterPanel.PSEUDO_COLUMN_ALL_COLUMNS ? JFilterPanel.ALL_COLUMN_CODE
						  : mTableModel.getColumnTitleNoAlias(mColumn);
		if (columnName != null && mReactionPart != null)
			columnName = columnName.concat(":").concat(mReactionPart);
		configuration.setProperty(PROPERTY_FILTER+"0", (columnName == null) ? FILTER_CODE[mFilterType] : FILTER_CODE[mFilterType]+":"+columnName);
		return configuration;
		}

	private void populateFilterList(boolean allowDuplicates) {
        ArrayList<String> itemList = new ArrayList<String>();

        for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
            if (mTableModel.isColumnTypeCategory(column)) {
                addItem(itemList, -1, JFilterPanel.FILTER_TYPE_CATEGORY_BROWSER, allowDuplicates);
                break;
                }
            }

		for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
			if (mTableModel.getColumnSpecialType(column) == null) {
				addItem(itemList, JFilterPanel.PSEUDO_COLUMN_ALL_COLUMNS, JFilterPanel.FILTER_TYPE_TEXT, allowDuplicates);
				break;
				}
			}

		for (int i=0; i<mTableModel.getTotalColumnCount(); i++) {
            String specialType = mTableModel.getColumnSpecialType(i);
            if (specialType != null) {
                if (mTableModel.isColumnTypeStructure(i)) {
                    addItem(itemList, i, JFilterPanel.FILTER_TYPE_STRUCTURE, allowDuplicates);
                    addItem(itemList, i, JFilterPanel.FILTER_TYPE_SSS_LIST, allowDuplicates);
                    addItem(itemList, i, JFilterPanel.FILTER_TYPE_SIM_LIST, allowDuplicates);
                	}
                if (mTableModel.isColumnTypeReaction(i)) {
					addItem(itemList, i, JFilterPanel.FILTER_TYPE_REACTION, null, allowDuplicates);
					addItem(itemList, i, JFilterPanel.FILTER_TYPE_RETRON, null, allowDuplicates);
					addItem(itemList, i, JFilterPanel.FILTER_TYPE_STRUCTURE, CompoundTableConstants.cReactionPartReactants, allowDuplicates);
					addItem(itemList, i, JFilterPanel.FILTER_TYPE_SSS_LIST, CompoundTableConstants.cReactionPartReactants, allowDuplicates);
					addItem(itemList, i, JFilterPanel.FILTER_TYPE_SIM_LIST, CompoundTableConstants.cReactionPartReactants, allowDuplicates);
					addItem(itemList, i, JFilterPanel.FILTER_TYPE_STRUCTURE, CompoundTableConstants.cReactionPartProducts, allowDuplicates);
					addItem(itemList, i, JFilterPanel.FILTER_TYPE_SSS_LIST, CompoundTableConstants.cReactionPartProducts, allowDuplicates);
					addItem(itemList, i, JFilterPanel.FILTER_TYPE_SIM_LIST, CompoundTableConstants.cReactionPartProducts, allowDuplicates);
					}
                }
            else {
   				addItem(itemList, i, JFilterPanel.FILTER_TYPE_TEXT, allowDuplicates);
    
    		    if (mTableModel.isColumnTypeDouble(i)
                 && mTableModel.hasNumericalVariance(i))
    				addItem(itemList, i, JFilterPanel.FILTER_TYPE_DOUBLE, allowDuplicates);

    		    if (mTableModel.isColumnTypeRangeCategory(i))
    				addItem(itemList, i, JFilterPanel.FILTER_TYPE_DOUBLE, allowDuplicates);
                }

			if (mTableModel.isColumnTypeCategory(i)
			 && mTableModel.getCategoryCount(i) < JCategoryFilterPanel.cMaxCheckboxCount)
				addItem(itemList, i, JFilterPanel.FILTER_TYPE_CATEGORY, allowDuplicates);
			}

		if (mTableModel.getListHandler().getListCount() > 0)
			addItem(itemList, -1, JFilterPanel.FILTER_TYPE_ROWLIST, allowDuplicates);

		Collections.sort(itemList, String.CASE_INSENSITIVE_ORDER);
        mFilterList.setListData(itemList.toArray());
		}

	private void addItem(ArrayList<String> itemList, int column, int type, boolean allowDuplicates) {
    	addItem(itemList, column, type, null, allowDuplicates);
		}

	private void addItem(ArrayList<String> itemList, int column, int type, String reactionPart, boolean allowDuplicates) {
		if (!allowDuplicates) {
			for (int i=0; i<mPruningPanel.getFilterCount(); i++) {
				JFilterPanel filter = mPruningPanel.getFilter(i);
				if (type == JFilterPanel.FILTER_TYPE_ROWLIST) {
					if (filter instanceof JHitlistFilterPanel)
						return;
					}
                else if (type == JFilterPanel.FILTER_TYPE_CATEGORY_BROWSER) {
                    if (filter instanceof JCategoryBrowser)
                        return;
                    }
				else if (filter.getColumnIndex() == column) {
					switch (type) {
                        case JFilterPanel.FILTER_TYPE_STRUCTURE:
                            if (filter instanceof JSingleStructureFilterPanel
							 && matchesReactionPart((JStructureFilterPanel)filter, reactionPart))
                                return;
                            break;
                        case JFilterPanel.FILTER_TYPE_SSS_LIST:
                            if (filter instanceof JMultiStructureFilterPanel
                       		 && ((JMultiStructureFilterPanel)filter).supportsSSS()
							 && matchesReactionPart((JStructureFilterPanel)filter, reactionPart))
                                return;
                            break;
                        case JFilterPanel.FILTER_TYPE_SIM_LIST:
                            if (filter instanceof JMultiStructureFilterPanel
                      		 && ((JMultiStructureFilterPanel)filter).supportsSim()
							 && matchesReactionPart((JStructureFilterPanel)filter, reactionPart))
                                return;
                            break;
                        case JFilterPanel.FILTER_TYPE_REACTION:
                            if (filter instanceof JReactionFilterPanel)
                                return;
                            break;
						case JFilterPanel.FILTER_TYPE_RETRON:
							if (filter instanceof JRetronFilterPanel)
								return;
							break;
                        case JFilterPanel.FILTER_TYPE_TEXT:
							if (filter instanceof JTextFilterPanel)
								return;
							break;
						case JFilterPanel.FILTER_TYPE_DOUBLE:
							if (filter instanceof JRangeFilterPanel)
								return;
							break;
						case JFilterPanel.FILTER_TYPE_CATEGORY:
							if (filter instanceof JCategoryFilterPanel)
								return;
							break;
						}
					}
				}
			}

		itemList.add(getFilterDisplayName(type, column, reactionPart));
		}

	private boolean matchesReactionPart(JStructureFilterPanel filter, String reactionPart) {
    	String frp = filter.getReactionPart();
    	if (reactionPart == null)
    		return (frp == null);

    	return reactionPart.equals(frp);
		}

	private String getFilterDisplayName(int type, int column, String reactionPart) {
		if (!FILTER_NEEDS_COLUMN[type])
			return FILTER_NAME[type];

		String columnName = (column < 0) ? null : mTableModel.getColumnTitleExtended(column);

		if (type == JFilterPanel.FILTER_TYPE_DOUBLE) {
			String text = columnName+" "+FILTER_NAME[type];
            return mTableModel.isColumnDataComplete(column) ? text : text+" ";
			}

		if (column == JFilterPanel.PSEUDO_COLUMN_ALL_COLUMNS)
			columnName = JFilterPanel.ALL_COLUMN_TEXT;

		String reactionPartText = (reactionPart == null) ? "" : reactionPart+" ";

		return columnName+" "+reactionPartText+FILTER_NAME[type];
		}

	@Override
	public boolean isConfigurable() {
		return mTableModel.getTotalColumnCount() != 0;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public JComponent createDialogContent() {
		JScrollPane scrollPane = null;

		if (isInteractive()) {
	        mFilterList = new JList();
	        mFilterList.setCellRenderer(new DefaultListCellRenderer() {
	            private static final long serialVersionUID = 0x20110526;
	
	            public Component getListCellRendererComponent(JList list,
	            	      Object value, int index, boolean isSelected, boolean cellHasFocus) {
	            	Component renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
	
	            	if (((String)value).endsWith(" "))
	            		renderer.setForeground(Color.RED);
	            	return renderer;
	            	}
	        	});
	        scrollPane = new JScrollPane(mFilterList);
	        scrollPane.setPreferredSize(new Dimension(HiDPIHelper.scale(320) ,HiDPIHelper.scale(240)));
	
			mCheckBox = new JCheckBox("Show duplicate filters");
			mCheckBox.addActionListener(this);
			}
		else {
			mTextArea = new JTextArea();
			scrollPane = new JScrollPane(mTextArea);
			scrollPane.setPreferredSize(new Dimension(HiDPIHelper.scale(320), HiDPIHelper.scale(128)));
			}

		JPanel content = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };
		content.setLayout(new TableLayout(size));
		String message = isInteractive() ? "Select New Filter(s)" : "Type new filter(s) as 'type:column name'";
		content.add(new JLabel(message), "1,1");
		content.add(scrollPane, "1,3");

		if (isInteractive())
			content.add(mCheckBox, "1,5");

		return content;
		}

	@Override
	public Properties getDialogConfiguration() {
    	Properties configuration = new Properties();

    	if (isInteractive()) {
        	if (mCheckBox.isSelected())
        	    configuration.setProperty(PROPERTY_SHOW_DUPLICATES, "true");

        	List<String> selectedFilters = mFilterList.getSelectedValuesList();
	
		    configuration.setProperty(PROPERTY_FILTER_COUNT, ""+selectedFilters.size());
	
			for (int filter=0; filter<selectedFilters.size(); filter++) {
				String selected = selectedFilters.get(filter).trim();	// get rid of color indication
	
				int type = getFilterTypeFromName(selected);
	
				String columnName = null;
				String reactionPart = "";
				if (FILTER_NEEDS_COLUMN[type]) {
					columnName = selected.substring(0, selected.length() - FILTER_NAME[type].length() - 1);
					if (JFilterPanel.ALL_COLUMN_TEXT.equals(columnName))
						columnName = JFilterPanel.ALL_COLUMN_CODE;
					else {
						// We check, if we have added the reaction part to the column name. If so, then remove it.
						if (columnName.endsWith(" "+CompoundTableConstants.cReactionPartReactants)
						 || columnName.endsWith(" "+CompoundTableConstants.cReactionPartProducts)) {
						 	int index = columnName.lastIndexOf(' ');
						 	int column = mTableModel.findColumn(columnName.substring(0, index));
						 	if (column != -1 && mTableModel.isColumnTypeReaction(column)) {
								reactionPart = ":"+columnName.substring(index+1);
								columnName = columnName.substring(0, index);
						 		}
							}
						columnName = mTableModel.getColumnTitleNoAlias(columnName);
						}
					}

				configuration.setProperty(PROPERTY_FILTER+filter, (columnName == null) ? FILTER_CODE[type] : FILTER_CODE[type]+":"+columnName+reactionPart);
				}
    		}
    	else {
    		String[] filterDefList = mTextArea.getText().split("\\n");
		    configuration.setProperty(PROPERTY_FILTER_COUNT, ""+filterDefList.length);
			for (int filter=0; filter<filterDefList.length; filter++)
				configuration.setProperty(PROPERTY_FILTER+filter, filterDefList[filter]);
    		}

		return configuration;
		}

	private int getFilterTypeFromName(String filterName) {
		for (int i=0; i<FILTER_NAME.length; i++)
			if (filterName.endsWith(FILTER_NAME[i]))
				return i;

		return -1;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		int filterCount = Integer.parseInt(configuration.getProperty(PROPERTY_FILTER_COUNT, "0"));

		if (isInteractive()) {
			mCheckBox.setSelected("true".equals(configuration.getProperty(PROPERTY_SHOW_DUPLICATES)));

			populateFilterList(mCheckBox.isSelected());
	
			mFilterList.clearSelection();
			for (int filter=0; filter<filterCount; filter++) {
				String filterDef = configuration.getProperty(PROPERTY_FILTER+filter);
				int type = getFilterTypeFromCode(filterDef);
				if (type != -1) {
					int column = getColumnFromCode(type, filterDef);
					String reactionPart = getReactionPartFromCode(filterDef);
					String displayName = getFilterDisplayName(type, column, reactionPart);
					for (int i=0; i<mFilterList.getModel().getSize(); i++) {
						if (mFilterList.getModel().getElementAt(i).equals(displayName)) {
							mFilterList.addSelectionInterval(i, i);
							break;
							}
						}
					}
				}
			}
		else {
			StringBuilder sb = new StringBuilder();
			for (int filter=0; filter<filterCount; filter++) {
				String filterDef = configuration.getProperty(PROPERTY_FILTER+filter);
				if (filterDef != null)
					sb.append(filterDef).append('\n');
				}
			mTextArea.setText(sb.toString());
			}
		}

	private int getFilterTypeFromCode(String def) {
		if (def != null)
			for (int i=0; i<FILTER_CODE.length; i++)
				if (def.equals(FILTER_CODE[i]) || def.startsWith(FILTER_CODE[i]+":"))
					return i;
		return -1;
		}

	/**
	 * Checks whether a column name is given, whether the column can be found
	 * and whether a column is needed with this filter type. Return values are:<br>
	 * >=0: column needed, name given and column found<br>
	 * -1: column needed, name given, but not found<br>
	 * -2: column needed, no name given<br>
	 * -3: column not needed, but name given<br>
	 * -4: column not needed and no name given<br>
	 * -5: JFilterPanel.PSEUDO_COLUMN_ALL_COLUMNS<br>
	 * @param type
	 * @param def
	 * @return valid column or negative result code
	 */
	private int getColumnFromCode(int type, String def) {
		if (FILTER_NEEDS_COLUMN[type]) {
			if (def.length() < FILTER_CODE[type].length()+2)
				return -2;
			int index1 = FILTER_CODE[type].length();
			String columnName = def.substring(index1+1);
			if (JFilterPanel.ALL_COLUMN_CODE.equals(columnName))
				return JFilterPanel.PSEUDO_COLUMN_ALL_COLUMNS;

			String reactionPart = getReactionPartFromCode(def);
			if (reactionPart != null)
				columnName = columnName.substring(0, columnName.length() - reactionPart.length() -1);

			return mTableModel.findColumn(columnName);
			}
		return (def.length() < FILTER_CODE[type].length()+2) ? -4 : -3;
		}

	private String getReactionPartFromCode(String def) {
		int index1 = def.indexOf(':');
		if (index1 != -1) {
			int index2 = def.lastIndexOf(':');
			if (index2 != -1 && index2 != index1) {
				String reactionPart = def.substring(index2+1);
				if (reactionPart.equals(CompoundTableConstants.cReactionPartReactants)
				 || reactionPart.equals(CompoundTableConstants.cReactionPartProducts))
					return reactionPart;
				}
			}
		return null;
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (isInteractive()) {
			mCheckBox.setSelected(false);
			populateFilterList(mCheckBox.isSelected());
			}
		else {
			mTextArea.setText("");
			}
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		int filterCount = Integer.parseInt(configuration.getProperty(PROPERTY_FILTER_COUNT, "0"));
		if (filterCount == 0) {
			showErrorMessage("No filters defined.");
			return false;
			}
		for (int filter=0; filter<filterCount; filter++) {
			String def = configuration.getProperty(PROPERTY_FILTER+filter, "");
			int type = getFilterTypeFromCode(def);
			if (type == -1) {
				StringBuilder sb = new StringBuilder(FILTER_CODE[0]);
				for (int i=1; i<FILTER_CODE.length; i++)
					sb.append(", ").append(FILTER_CODE[i]);
				showErrorMessage("No valid filter type found in '"+def+"'.\nValid filter types are: "+sb.toString()+".");
				return false;
				}
			int column = getColumnFromCode(type, def);
			if (column == JFilterPanel.PSEUDO_COLUMN_ALL_COLUMNS && type != JFilterPanel.FILTER_TYPE_TEXT) {
				showErrorMessage("Option '<All Columns>' is only supported by text filters.");
				return false;
				}
			if (column == -2) {
				showErrorMessage("Column name missing. "+FILTER_NAME[type]+" filters must be defined as '"+FILTER_CODE[type]+":<column name>'.");
				return false;
				}
			if (column == -3) {
				showErrorMessage("Superflous column name specified. "+FILTER_NAME[type]+" filters must be specified as '"+FILTER_CODE[type]+"'.");
				return false;
				}
			boolean isStructureFilter = (type == JFilterPanel.FILTER_TYPE_STRUCTURE
					|| type == JFilterPanel.FILTER_TYPE_SSS_LIST
					|| type == JFilterPanel.FILTER_TYPE_SIM_LIST);
			String reactionPart = getReactionPartFromCode(def);
			if (reactionPart != null && !isStructureFilter) {
				showErrorMessage("Specifying a reaction part is valid for structure filters only.");
				return false;
				}
			if (isLive) {
				if (FILTER_NEEDS_COLUMN[type]) {
					String columnName = def.substring(FILTER_CODE[type].length()+1);
					if (column == -1) {
						showErrorMessage("Column '"+columnName+"' not found.");
						return false;
						}
					if (mTableModel.isColumnTypeStructure(column) && !(isStructureFilter
					 || (mTableModel.isColumnTypeCategory(column) && (type == JFilterPanel.FILTER_TYPE_CATEGORY || type == JFilterPanel.FILTER_TYPE_CATEGORY_BROWSER)))) {
						showErrorMessage("Column '"+columnName+"' is a structure column and is compatible with structure filters only.");
						return false;
						}
					if (mTableModel.isColumnTypeReaction(column)) {
						if (!isStructureFilter && type != JFilterPanel.FILTER_TYPE_REACTION && type != JFilterPanel.FILTER_TYPE_RETRON) {
							showErrorMessage("Column '"+columnName+"' is compatible with reaction, retron, or structure filters only.");
							return false;
							}
						if (isStructureFilter && reactionPart == null) {
							showErrorMessage("Structure filters on a reaction column need the reaction part to be defined.");
							return false;
							}
						}
					}
				}
			}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		int filterCount = Integer.parseInt(configuration.getProperty(PROPERTY_FILTER_COUNT, "0"));
		int foundCount = 0;
		for (int filter=0; filter<filterCount; filter++) {
			String filterDef = configuration.getProperty(PROPERTY_FILTER+filter);
			int type = getFilterTypeFromCode(filterDef);
			if (type != -1) {
				int column = getColumnFromCode(type, filterDef);
				if (!FILTER_NEEDS_COLUMN[type] || column >= 0 || column == JFilterPanel.PSEUDO_COLUMN_ALL_COLUMNS) {
					try {
						boolean isReactionColumn = (column >= 0) && mTableModel.isColumnTypeReaction(column);
						String reactionPart = isReactionColumn ? getReactionPartFromCode(filterDef) : null;
						switch (type) {
						case JFilterPanel.FILTER_TYPE_TEXT:
							mPruningPanel.addTextFilter(mTableModel, column);
							break;
						case JFilterPanel.FILTER_TYPE_DOUBLE:
							mPruningPanel.addDoubleFilter(mTableModel, column);
							break;
						case JFilterPanel.FILTER_TYPE_CATEGORY:
							mPruningPanel.addCategoryFilter(mTableModel, column);
							break;
						case JFilterPanel.FILTER_TYPE_STRUCTURE:
							ensureDescriptor(column, DescriptorConstants.DESCRIPTOR_FFP512.shortName, reactionPart);
		                    mPruningPanel.addStructureFilter(mTableModel, column, reactionPart, null);
							break;
						case JFilterPanel.FILTER_TYPE_SSS_LIST:
							ensureDescriptor(column, DescriptorConstants.DESCRIPTOR_FFP512.shortName, reactionPart);
		                    mPruningPanel.addStructureListFilter(mTableModel, column, reactionPart, true);
							break;
						case JFilterPanel.FILTER_TYPE_SIM_LIST:
							ensureDescriptor(column, DescriptorConstants.DESCRIPTOR_FFP512.shortName, reactionPart);
		                    mPruningPanel.addStructureListFilter(mTableModel, column, reactionPart, false);
							break;
						case JFilterPanel.FILTER_TYPE_REACTION:
							ensureDescriptor(column, DescriptorConstants.DESCRIPTOR_ReactionFP.shortName, null);
		                    mPruningPanel.addReactionFilter(mTableModel, column, null);
							break;
						case JFilterPanel.FILTER_TYPE_RETRON:
							ensureDescriptor(column, DescriptorConstants.DESCRIPTOR_FFP512.shortName, CompoundTableConstants.cReactionPartReactants);
							ensureDescriptor(column, DescriptorConstants.DESCRIPTOR_FFP512.shortName, CompoundTableConstants.cReactionPartProducts);
							mPruningPanel.addRetronFilter(mTableModel, column, null);
							break;
						case JFilterPanel.FILTER_TYPE_ROWLIST:
							mPruningPanel.addHitlistFilter(mTableModel);
							break;
						case JFilterPanel.FILTER_TYPE_CATEGORY_BROWSER:
		                    mPruningPanel.addCategoryBrowser(mTableModel);
		                	break;
							}
		
						foundCount++;
						}
					catch (DEPruningPanel.FilterException fpe) {
						showErrorMessage(fpe.getMessage());
						}
					}
				}
			}

		if (foundCount != 0) {
			mPruningPanel.getParentPane().fireRuntimePropertyChanged(
					new RuntimePropertyEvent(mPruningPanel, RuntimePropertyEvent.TYPE_ADD_FILTER, -1));
			}
		}

	private void ensureDescriptor(int column, String shortName, String reactionPart) {
		if (!mTableModel.hasDescriptorColumn(column, shortName, reactionPart))
			mTableModel.addDescriptorColumn(column, shortName, reactionPart);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
