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
public class JEPMovingInCategoryFunction extends PostfixMathCommand {
	private DETaskAddCalculatedValues mParentTask;
	private CompoundTableModel mTableModel;
	private TreeMap<Long,double[]> mResultMap;
	private boolean mIsAverage,mCalculateEdgeValues;

	/**
	 * Constructor
	 */
	public JEPMovingInCategoryFunction(CompoundTableModel tableModel, DETaskAddCalculatedValues parentTask,
	                                   boolean isAverage, boolean calculateEdgeValues) {
		super();
		mParentTask = parentTask;
		mTableModel = tableModel;
		numberOfParameters = 4;
		mIsAverage = isAverage;
		mCalculateEdgeValues = calculateEdgeValues;
		}

	private double getResult(int valueColumn, int categoryColumn, int n1, int n2) {
		if (mResultMap == null)
			mResultMap = new TreeMap<>();

		// just in case we have multiple movingAverageInCategory() functions in one equation, we need to distinguish
		long key = ((long)categoryColumn << 40) + ((long)valueColumn << 16) + (n1 << 8) + n2;
		double[] result = mResultMap.get(key);
		if (result == null) {
			result = new double[mTableModel.getTotalRowCount()];
			TreeMap<String,MovingWindow> categoryValueMap = new TreeMap<>();
			for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
				String category = mTableModel.getTotalValueAt(row, categoryColumn);
				double value = mTableModel.getTotalDoubleAt(row, valueColumn);
				MovingWindow window = categoryValueMap.get(category);
				if (window == null) {
					window = new MovingWindow(n1, n2);
					categoryValueMap.put(category, window);
					}
				window.addValue(value, row, result);
				}

			if (mCalculateEdgeValues)
				for (MovingWindow window:categoryValueMap.values())
					window.processTailValues(result);

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
		Object param4 = inStack.pop();
		Object param3 = inStack.pop();
		Object param2 = inStack.pop();
		Object param1 = inStack.pop();

		if (!(param1 instanceof String))
			throw new ParseException("1st parameter type is not 'String'");
		if (!(param2 instanceof String))
			throw new ParseException("2nd parameter type is not 'String'");
		if (!(param3 instanceof Double))
			throw new ParseException("3rd parameter type is not numerical");
		if (!(param4 instanceof Double))
			throw new ParseException("4th parameter type is not numerical");

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

		if (!mTableModel.isColumnTypeDouble(valueColumn))
			throw new ParseException("Column '"+param2+"' does not contain numerical values.");

		int n1 = (int)Math.round((Double)param3);
		int n2 = (int)Math.round((Double)param4);
		if (n1 < 0 || n2 < 0 || n1 + n2 == 0 || n1 + n2 > 255)
			throw new ParseException("Either of both, n1 and n2, must be positive integer values. n1+n2 must be smaller than 255.");

		inStack.push(new Double(getResult(valueColumn, categoryColumn, n1, n2)));
		}

	private class MovingWindow {
		public double[] value;	// cyclic cache of category values
		public int[] rowIndex;	// associated cyclic cache of originating row indexes
		public int cacheIndex,valueCount,n1,n2;

		public MovingWindow(int n1, int n2) {
			value = new double[n1+n2+1];
			rowIndex = new int[n1+n2+1];
			this.n1 = n1;
			this.n2 = n2;
			}

		public void addValue(double newValue, int row, double[] result) {
			value[cacheIndex] = newValue;
			rowIndex[cacheIndex] = row;
			valueCount++;
			cacheIndex = cyclicIndex(valueCount);

			if (valueCount > n2) {
				int centerIndex = cyclicIndex(valueCount - n2 - 1);

				if (valueCount < value.length)
					result[rowIndex[centerIndex]] = mCalculateEdgeValues ? getAverage(0, valueCount) : Double.NaN;
				else
					result[rowIndex[centerIndex]] = getAverage(0, value.length);
				}
			}

		public void processTailValues(double[] result) {
			for (int valueIndex=valueCount-n2; valueIndex<valueCount; valueIndex++) {
				int centerIndex = cyclicIndex(valueIndex);
				int i1 = cyclicIndex(Math.max(0, valueIndex-n1));
				int i2 = cyclicIndex(valueCount);
				result[rowIndex[centerIndex]] = mCalculateEdgeValues ? getAverage(i1, i2) : Double.NaN;
				}
			}

		private double getAverage(int i1, int i2) {
			double sum = 0;
			int count = 0;
			if (i1 < i2) {
				for (int i=i1; i<i2; i++) {
					if (!Double.isNaN(value[i])) {
						sum += value[i];
						count++;
						}
					}
				}
			else {
				for (int i=0; i<i2; i++) {
					if (!Double.isNaN(value[i])) {
						sum += value[i];
						count++;
						}
					}
				for (int i=i1; i<value.length; i++) {
					if (!Double.isNaN(value[i])) {
						sum += value[i];
						count++;
						}
					}
				}
			return count == 0 ? Double.NaN : mIsAverage ? sum / count : sum;
			}

		private int cyclicIndex(int index) {
			return index % value.length;
			}
		}
	}
