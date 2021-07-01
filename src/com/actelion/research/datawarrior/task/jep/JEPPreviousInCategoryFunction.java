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

import com.actelion.research.datawarrior.task.data.DETaskAddCalculatedValues;
import com.actelion.research.table.model.CompoundTableModel;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import java.util.Stack;
import java.util.TreeMap;

/**
 * An example custom function class for JEP.
 */
public class JEPPreviousInCategoryFunction extends PostfixMathCommand {
	private DETaskAddCalculatedValues mParentTask;
	private CompoundTableModel mTableModel;
	private TreeMap<Long,Object[]> mResultMap;

	/**
	 * Constructor
	 */
	public JEPPreviousInCategoryFunction(CompoundTableModel tableModel, DETaskAddCalculatedValues parentTask) {
		super();
		mParentTask = parentTask;
		mTableModel = tableModel;
		numberOfParameters = 3;
		}

	private Object getResult(int valueColumn, int categoryColumn, int n) {
		if (mResultMap == null)
			mResultMap = new TreeMap<>();

		// just in case we have multiple movingAverageInCategory() functions in one equation, we need to distinguish
		long key = ((long)categoryColumn << 40) + ((long)valueColumn << 16) + n;
		Object[] result = mResultMap.get(key);
		if (result == null) {
			result = new Object[mTableModel.getTotalRowCount()];
			TreeMap<String,MovingWindow> categoryValueMap = new TreeMap<>();
			for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
				String category = mTableModel.getTotalValueAt(row, categoryColumn);
				Object value;
				if (mTableModel.isColumnTypeDouble(valueColumn)) {
					float v = mTableModel.getTotalRecord(row).getDouble(valueColumn);
					value = new Double(mTableModel.isLogarithmicViewMode(valueColumn) ? Math.pow(10, v) : v);
					}
				else {
					value = mTableModel.getTotalRecord(row).getData(valueColumn);
					}
				MovingWindow window = categoryValueMap.get(category);
				if (window == null) {
					window = new MovingWindow(n);
					categoryValueMap.put(category, window);
					}
				window.addValue(value, row, result);
				}

			mResultMap.put(key, result);
			}
		return result[mParentTask.getCurrentRow()];
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
		Object param3 = inStack.pop();
		Object param2 = inStack.pop();
		Object param1 = inStack.pop();

		if (!(param1 instanceof String))
			throw new ParseException("1st parameter type is not 'String'");
		if (!(param2 instanceof String))
			throw new ParseException("2nd parameter type is not 'String'");
		if (!(param3 instanceof Double))
			throw new ParseException("3rd parameter type is not numerical");

		int valueColumn = mTableModel.findColumn((String)param2);
		if (valueColumn == -1)
			throw new ParseException("Column '"+param2+"' not found.");

		int categoryColumn = mTableModel.findColumn((String)param1);
		if (categoryColumn == -1)
			throw new ParseException("Column '"+param1+"' not found.");

		if (!mTableModel.isColumnTypeCategory(categoryColumn) && !mTableModel.isEqualValueColumn(categoryColumn))
			throw new ParseException("Column '"+param1+"' does not contain category values.");

		if (mTableModel.isMultiEntryColumn(categoryColumn))
			throw new ParseException("Some cells of category column '"+param1+"' contain multiple values.");

		int n = (int)Math.round((Double)param3);
		if (n<=0 || n>255)
			throw new ParseException("n must be a positive integer value smaller than 256.");

		Object result = getResult(valueColumn, categoryColumn, n);
		if (mTableModel.isColumnTypeDouble(valueColumn))
			inStack.push(result == null ? Double.NaN : result);
		else
			inStack.push(result == null ? "" : new String((byte[])result));
		}

	private class MovingWindow {
		public Object[] value;	// cyclic cache of row values
		public int[] rowIndex;	// associated cyclic cache of originating row indexes
		public int cacheIndex,valueCount,n;

		public MovingWindow(int n) {
			value = new Object[n+1];
			rowIndex = new int[n+1];
			this.n = n;
			}

		public void addValue(Object newValue, int row, Object[] result) {
			value[cacheIndex] = newValue;
			rowIndex[cacheIndex] = row;
			valueCount++;
			cacheIndex = cyclicIndex(valueCount);

			if (valueCount > n) {
				int previousIndex = cyclicIndex(valueCount - n - 1);
				result[row] = value[previousIndex];
				}
			else {
				result[row] = null;
				}
			}

		private int cyclicIndex(int index) {
			return index % value.length;
			}
		}
	}
