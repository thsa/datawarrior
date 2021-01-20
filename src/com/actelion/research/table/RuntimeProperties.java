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

package com.actelion.research.table;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.config.AbstractConfiguration;
import com.actelion.research.table.view.config.CardsViewConfiguration;
import com.actelion.research.table.view.config.ViewConfiguration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.TreeMap;

public class RuntimeProperties extends AbstractConfiguration implements CompoundTableConstants {
    private static final long serialVersionUID = 0x20061101;

    public static final String cColumnAlias = "columnAlias";
    public static final String cColumnAliasCount = "columnAliasCount";

    private static final String cLogarithmicViewMode = "logarithmicView";
	private static final String cColumnDataType = "explicitColumnType";
	private static final String cColumnDataTypeCount = "explicitColumnTypeCount";
    private static final String cSignificantDigits = "significantDigits";
    private static final String cSignificantDigitColumnCount = "significantDigitColumnCount";
    private static final String cModifierValuesExcluded = "modifierValuesExcluded";
    private static final String cCurrentRecord = "currentRecord";
    private static final String cColumnDescription = "columnDescription";
    private static final String cColumnDescriptionCount = "columnDescriptionCount";
    private static final String cCustomOrderCount = "customOrderCount";
    private static final String cCustomOrder = "customOrder";
    private static final String cSummaryCountHidden = "summaryCountHidden";
    private static final String cStdDeviationShown = "stdDeviationShown";

	protected CompoundTableModel mTableModel;
	private TreeMap<String,ViewConfiguration> mViewConfigurationMap;

	public RuntimeProperties(CompoundTableModel tableModel) {
        super(cTemplateTagName);
		mTableModel = tableModel;
		mViewConfigurationMap = new TreeMap();
		}

	@Override
	public void clear() {
		super.clear();
		mViewConfigurationMap.clear();
		}

	public void apply() {
	    RuntimePropertyColumnList list = new RuntimePropertyColumnList(cLogarithmicViewMode);
	    for (int column=list.next(); column != -1; column=list.next())
	        mTableModel.setLogarithmicViewMode(column, true);

	    list = new RuntimePropertyColumnList(cModifierValuesExcluded);
	    for (int column=list.next(); column != -1; column=list.next())
	        mTableModel.setColumnModifierExclusion(column, true);

	    for (int summaryMode = 1; summaryMode< cSummaryModeCode.length; summaryMode++) {
    	    list = new RuntimePropertyColumnList(cSummaryModeCode[summaryMode]);
    	    for (int column=list.next(); column != -1; column=list.next())
                mTableModel.setColumnSummaryMode(column, summaryMode);
	        }

	    list = new RuntimePropertyColumnList(cSummaryCountHidden);
	    for (int column=list.next(); column != -1; column=list.next())
            mTableModel.setColumnSummaryCountHidden(column, true);

	    list = new RuntimePropertyColumnList(cStdDeviationShown);
	    for (int column=list.next(); column != -1; column=list.next())
            mTableModel.setColumnStdDeviationShown(column, true);

	    for (int hiliteMode = 1; hiliteMode< cStructureHiliteModeCode.length; hiliteMode++) {
    	    list = new RuntimePropertyColumnList(cStructureHiliteModeCode[hiliteMode]);
    	    for (int column=list.next(); column != -1; column=list.next())
                mTableModel.setHiliteMode(column, hiliteMode);
	        }

		String currentRecord = getProperty(cCurrentRecord);
		if (currentRecord != null) {
		    try {	// this is hardly useful for a dataset that differs from the original
				int record = Integer.parseInt(currentRecord);
		        if (record < mTableModel.getRowCount())
		            mTableModel.setActiveRow(record);
		        }
		    catch (NumberFormatException e) {}
			}
		String columnCount = getProperty(cColumnAliasCount);
		if (columnCount != null) {
			try {
				int count = Integer.parseInt(columnCount);
				for (int i=0; i<count; i++) {
					String columnAlias = getProperty(cColumnAlias+"_"+i);
					if (columnAlias != null) {
						int index = columnAlias.indexOf('\t');
						if (index != -1) {
							int column = mTableModel.findColumn(columnAlias.substring(0, index));
							if (column != -1)
								mTableModel.setColumnAlias(columnAlias.substring(index+1), column);
							}
						}
					}
				} catch (NumberFormatException e) {}
			}
		columnCount = getProperty(cColumnDescriptionCount);
		if (columnCount != null) {
			try {
				int count = Integer.parseInt(columnCount);
				for (int i=0; i<count; i++) {
					String columnDescription = getProperty(cColumnDescription+"_"+i);
					if (columnDescription != null) {
						int index = columnDescription.indexOf('\t');
						if (index != -1) {
							int column = mTableModel.findColumn(columnDescription.substring(0, index));
							if (column != -1)
								mTableModel.setColumnDescription(columnDescription.substring(index+1).replace("<NL>", "\n"), column);
							}
						}
					}
				} catch (NumberFormatException e) {}
			}
		columnCount = getProperty(cColumnDataTypeCount);
		if (columnCount != null) {
			try {
				int count = Integer.parseInt(columnCount);
				for (int i=0; i<count; i++) {
					String explicitType = getProperty(cColumnDataType+"_"+i);
					if (explicitType != null) {
						int index = explicitType.indexOf('\t');
						if (index != -1) {
							int column = mTableModel.findColumn(explicitType.substring(0, index));
							if (column != -1) {
								int type = findCode(explicitType.substring(index+1), cDataTypeCode, cDataTypeAutomatic);
								mTableModel.setExplicitDataType(column, type);
								}
							}
						}
					}
				} catch (NumberFormatException e) {}
			}
		columnCount = getProperty(cSignificantDigitColumnCount);
        if (columnCount != null) {
            try {
                int count = Integer.parseInt(columnCount);
                for (int i=0; i<count; i++) {
                    String significantDigits = getProperty(cSignificantDigits+"_"+i);
                    if (significantDigits != null) {
                        int index = significantDigits.indexOf('\t');
                        if (index != -1) {
                            int column = mTableModel.findColumn(significantDigits.substring(0, index));
                            if (column != -1) {
                                try {
                                    int digits = Integer.parseInt(significantDigits.substring(index+1));
                                    mTableModel.setColumnSignificantDigits(column, digits);
                                    } catch (NumberFormatException e) {}
                                }
                            }
                        }
                    }
                } catch (NumberFormatException e) {}
            }
        columnCount = getProperty(cCustomOrderCount);
        if (columnCount != null) {
            try {
                int count = Integer.parseInt(columnCount);
                for (int i=0; i<count; i++) {
                    String customOrder = getProperty(cCustomOrder+"_"+i);
                    if (customOrder != null) {
                        int index = customOrder.indexOf('\t');
                        if (index != -1) {
                            int column = mTableModel.findColumn(customOrder.substring(0, index));
                            if (column != -1) {
                                String[] customOrderItems = customOrder.substring(index+1).split("\\t");
                                mTableModel.setCategoryCustomOrder(column, customOrderItems);
                                }
                            }
                        }
                    }
                } catch (NumberFormatException e) {}
            }
		}

	public void readViewConfiguration(BufferedReader reader, String line) throws IOException {
		String name = extractName(line);
		String type = extractType(line);
		if (name != null) {
			if (CardsViewConfiguration.VIEW_TYPE.equals(type)) {
				CardsViewConfiguration config = new CardsViewConfiguration(mTableModel);
				config.read(reader);
				mViewConfigurationMap.put(name, config);
			}
		}
	}

	public void learnAndWrite(BufferedWriter writer) throws IOException {
		learn();
		super.write(writer, null, null);
		for (String viewName:mViewConfigurationMap.keySet()) {    // write configurations of views, which were learned earlier
			ViewConfiguration config = mViewConfigurationMap.get(viewName);
			config.write(writer, viewName, config.getViewType());
			}
		}

	public void addViewConfiguration(String name, ViewConfiguration config) {
		mViewConfigurationMap.put(name, config);
		}

	public ViewConfiguration getViewConfiguration(String name) {
		return mViewConfigurationMap.get(name);
		}

	protected void learn() {
		clear();
		String columnList = "";

		for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
			if (mTableModel.isLogarithmicViewMode(column)) {
				if (columnList.length() != 0)
				    columnList = columnList.concat("\t");
				columnList = columnList.concat(mTableModel.getColumnTitleNoAlias(column));
				}
		    }
        if (columnList.length() != 0)
            setProperty(cLogarithmicViewMode, columnList);

        columnList = "";
        for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
            if (mTableModel.getColumnModifierExclusion(column)) {
                if (columnList.length() != 0)
                    columnList = columnList.concat("\t");
                columnList = columnList.concat(mTableModel.getColumnTitleNoAlias(column));
                }
            }
        if (columnList.length() != 0)
            setProperty(cModifierValuesExcluded, columnList);

        for (int summaryMode = 1; summaryMode< cSummaryModeCode.length; summaryMode++) {
            columnList = "";
            for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
                if (mTableModel.getColumnSummaryMode(column) == summaryMode) {
                    if (columnList.length() != 0)
                        columnList = columnList.concat("\t");
                    columnList = columnList.concat(mTableModel.getColumnTitleNoAlias(column));
                    }
                }
            if (columnList.length() != 0)
                setProperty(cSummaryModeCode[summaryMode], columnList);

            columnList = "";
            for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
                if (mTableModel.isColumnSummaryCountHidden(column)) {
                    if (columnList.length() != 0)
                        columnList = columnList.concat("\t");
                    columnList = columnList.concat(mTableModel.getColumnTitleNoAlias(column));
                    }
                }
            if (columnList.length() != 0)
                setProperty(cSummaryCountHidden, columnList);

            columnList = "";
            for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
                if (mTableModel.isColumnStdDeviationShown(column)) {
                    if (columnList.length() != 0)
                        columnList = columnList.concat("\t");
                    columnList = columnList.concat(mTableModel.getColumnTitleNoAlias(column));
                    }
                }
            if (columnList.length() != 0)
                setProperty(cStdDeviationShown, columnList);
    		}

        for (int hiliteMode = 1; hiliteMode< cStructureHiliteModeCode.length; hiliteMode++) {
            columnList = "";
            for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
                if (mTableModel.getHiliteMode(column) == hiliteMode) {
                    if (columnList.length() != 0)
                        columnList = columnList.concat("\t");
                    columnList = columnList.concat(mTableModel.getColumnTitleNoAlias(column));
                    }
                }
            if (columnList.length() != 0)
                setProperty(cStructureHiliteModeCode[hiliteMode], columnList);
            }

		CompoundRecord currentRecord = mTableModel.getActiveRow();
		if (currentRecord != null) {
			for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
				if (mTableModel.getTotalRecord(row) == currentRecord) {
					setProperty(cCurrentRecord, ""+row);
					break;
					}
				}
			}

		int columnAliasCount = 0;
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.getColumnAlias(column) != null)
				columnAliasCount++;
		if (columnAliasCount != 0) {
			setProperty(cColumnAliasCount, ""+columnAliasCount);
			columnAliasCount = 0;
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
				if (mTableModel.getColumnAlias(column) != null)
					setProperty(cColumnAlias+"_"+columnAliasCount++,
							mTableModel.getColumnTitleNoAlias(column)+"\t"+mTableModel.getColumnAlias(column));
			}
		int columnDescriptionCount = 0;
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.getColumnDescription(column) != null)
				columnDescriptionCount++;
		if (columnDescriptionCount != 0) {
			setProperty(cColumnDescriptionCount, ""+columnDescriptionCount);
			columnDescriptionCount = 0;
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
				if (mTableModel.getColumnDescription(column) != null)
					setProperty(cColumnDescription+"_"+columnDescriptionCount++,
							mTableModel.getColumnTitleNoAlias(column)+"\t"+mTableModel.getColumnDescription(column).replace("\n", "<NL>"));
			}
		int columnTypeCount = 0;
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.getExplicitDataType(column) != cDataTypeAutomatic)
				columnTypeCount++;
		if (columnTypeCount != 0) {
			setProperty(cColumnDataTypeCount, ""+columnTypeCount);
			columnTypeCount = 0;
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
				if (mTableModel.getExplicitDataType(column) != cDataTypeAutomatic)
					setProperty(cColumnDataType+"_"+columnTypeCount++,
							mTableModel.getColumnTitleNoAlias(column)+"\t"+cDataTypeCode[mTableModel.getExplicitDataType(column)]);
		}
        int significantDigitColumnCount = 0;
        for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
            if (mTableModel.getColumnSignificantDigits(column) != 0)
                significantDigitColumnCount++;
        if (significantDigitColumnCount != 0) {
            setProperty(cSignificantDigitColumnCount, ""+significantDigitColumnCount);
            significantDigitColumnCount = 0;
            for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
                if (mTableModel.getColumnSignificantDigits(column) != 0)
                    setProperty(cSignificantDigits+"_"+significantDigitColumnCount++,
                            mTableModel.getColumnTitleNoAlias(column)+"\t"+mTableModel.getColumnSignificantDigits(column));
            }
		int customOrderCount = 0;
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.getCategoryCustomOrder(column) != null)
				customOrderCount++;
		if (customOrderCount != 0) {
			setProperty(cCustomOrderCount, ""+customOrderCount);
			customOrderCount = 0;
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
				String[] customOrder = mTableModel.getCategoryCustomOrder(column);
				if (customOrder != null) {
					StringBuilder sb = new StringBuilder(customOrder[0]);
					for (int i=1; i<customOrder.length; i++)
						sb.append('\t').append(customOrder[i]);
					setProperty(cCustomOrder+"_"+customOrderCount++,
							mTableModel.getColumnTitleNoAlias(column)+"\t"+sb.toString());
					}
				}
			}
		}

	private int findCode(String code, String[] option, int defaultCode) {
		for (int i=0; i<option.length; i++)
			if (code.equals(option[i]))
				return i;

		return defaultCode;
		}

	class RuntimePropertyColumnList {
	    private String columnList = null;

	    public RuntimePropertyColumnList(String key) {
	        columnList = getProperty(key);
	        }

	    public int next() {
	        while (columnList != null) {
                int index = columnList.indexOf('\t');
                String name = null;
                if (index != -1) {
                    name = columnList.substring(0, index);
                    columnList = columnList.substring(index+1);
                    }
                else {
                    name = columnList;
                    columnList = null;
                    }
                int column = mTableModel.findColumn(name);
                if (column != -1)
                    return column;
	            }
            return -1;
	        }
	    }
	}
