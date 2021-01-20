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

package com.actelion.research.datawarrior.task.jep;

import java.util.Stack;
import java.util.TreeMap;

import com.actelion.research.datawarrior.task.data.DETaskAddCalculatedValues;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import com.actelion.research.table.model.CompoundTableModel;

/**
 * An example custom function class for JEP.
 */
public class JEPFrequencyInCategoryFunction extends PostfixMathCommand {
	private DETaskAddCalculatedValues mParentTask;
	private CompoundTableModel mTableModel;
	private TreeMap<Integer,TreeMap<String,Integer>> mCountMaps;

	/**
	 * Constructor
	 */
	public JEPFrequencyInCategoryFunction(CompoundTableModel tableModel, DETaskAddCalculatedValues parentTask) {
		super();
		mParentTask = parentTask;
		mTableModel = tableModel;
		numberOfParameters = 2;
	}

	private TreeMap<String,Integer> createMap(int valueColumn, int categoryColumn) {
		if (mCountMaps == null)
			mCountMaps = new TreeMap<>();

		TreeMap<String,Integer> countMap = mCountMaps.get(categoryColumn + 0x00010000 * valueColumn);
		if (countMap == null) {
			countMap = new TreeMap<>();
			boolean isValueNumerical = mTableModel.isColumnTypeDouble(valueColumn);
			boolean isCategoryNumerical = mTableModel.isColumnTypeDouble(categoryColumn);
			for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
				String value = isValueNumerical ? Float.toString(mTableModel.getTotalDoubleAt(row, valueColumn))
						: mTableModel.getTotalValueAt(row, valueColumn);
				String category = isCategoryNumerical ? Float.toString(mTableModel.getTotalDoubleAt(row, categoryColumn))
						: mTableModel.getTotalValueAt(row, categoryColumn);
				String key = value.concat("@#").concat(category);
				Integer count = countMap.get(key);
				if (count == null)
					countMap.put(key, new Integer(1));
				else
					countMap.put(key, new Integer(count.intValue()+1));
				}
			mCountMaps.put(categoryColumn + 0x00010000 * valueColumn, countMap);
			}
		return countMap;
		}

	/**
	 * Runs the square root operation on the inStack. The parameter is popped
	 * off the <code>inStack</code>, and the square root of it's value is
	 * pushed back to the top of <code>inStack</code>.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void run(Stack inStack) throws ParseException {

		// check the stack
		checkStack(inStack);

		// get the parameter from the stack
		Object param2 = inStack.pop();
		Object param1 = inStack.pop();

		if (!(param1 instanceof String))
			throw new ParseException("1st parameter type is not 'String'");
		if (!(param2 instanceof String))
			throw new ParseException("2nd parameter type is not 'String'");

		int categoryColumn = mTableModel.findColumn((String)param1);
		if (categoryColumn == -1)
			throw new ParseException("Column '"+param1+"' not found.");

		int valueColumn = mTableModel.findColumn((String)param2);
		if (valueColumn == -1)
			throw new ParseException("Column '"+param2+"' not found.");

		if (mTableModel.isMultiEntryColumn(categoryColumn))
			throw new ParseException("Some cells of category column '"+param1+"' contain multiple values.");

		String category = mTableModel.isColumnTypeDouble(categoryColumn) ?
				Float.toString(mTableModel.getTotalDoubleAt(mParentTask.getCurrentRow(), categoryColumn))
				: mTableModel.getTotalValueAt(mParentTask.getCurrentRow(), categoryColumn);

		String value = mTableModel.isColumnTypeDouble(valueColumn) ?
				Float.toString(mTableModel.getTotalDoubleAt(mParentTask.getCurrentRow(), valueColumn))
				: mTableModel.getTotalValueAt(mParentTask.getCurrentRow(), valueColumn);

		String key = value.concat("@#").concat(category);

		Integer co = createMap(valueColumn, categoryColumn).get(key);
		int count = (co == null) ? 0 : co.intValue();
		inStack.push(new Double(count));
		}
	}
