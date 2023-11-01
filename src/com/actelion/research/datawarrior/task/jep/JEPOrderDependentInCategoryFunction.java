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
public class JEPOrderDependentInCategoryFunction extends PostfixMathCommand {
	public static final int TYPE_INCREASE = 1;
	public static final int TYPE_PERCENT_INCREASE = 2;
	public static final int TYPE_CUMULATIVE_SUM = 3;

	private DETaskAddCalculatedValues mParentTask;
	private CompoundTableModel mTableModel;
	private TreeMap<Integer,double[]> mResultMap;
	private int mType;

	/**
	 * Constructor
	 */
	public JEPOrderDependentInCategoryFunction(CompoundTableModel tableModel, int type, DETaskAddCalculatedValues parentTask) {
		super();
		mParentTask = parentTask;
		mTableModel = tableModel;
		mType = type;
		numberOfParameters = 2;
	}

	private double getResult(int valueColumn, int categoryColumn) {
		if (mResultMap == null)
			mResultMap = new TreeMap<>();

		double[] result = mResultMap.get(categoryColumn + 0x00010000 * valueColumn);
		if (result == null) {
			result = new double[mTableModel.getTotalRowCount()];
			TreeMap<String,Double> categoryValueMap = new TreeMap<>();
			if (mType == TYPE_INCREASE || mType == TYPE_PERCENT_INCREASE) {
				for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
					String category = mTableModel.getTotalValueAt(row, categoryColumn);
					double value = mTableModel.getTotalDoubleAt(row, valueColumn);
					Double previousValue = categoryValueMap.get(category);
					if (previousValue == null) {
						result[row] = Double.NaN;
						}
					else {
						result[row] = Double.isNaN(value) ? Double.NaN
								: (mType == TYPE_INCREASE) ? value - previousValue : (value - previousValue) / previousValue;
						}
					categoryValueMap.put(category, Double.isNaN(value) ? null : value);
					}
				}
			else if (mType == TYPE_CUMULATIVE_SUM) {
				for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
					String category = mTableModel.getTotalValueAt(row, categoryColumn);
					double value = mTableModel.getTotalDoubleAt(row, valueColumn);
					if (Double.isNaN(value)) {
						result[row] = Double.NaN;
						}
					else {
						Double previousSum = categoryValueMap.get(category);
						result[row] = (previousSum == null) ? value : previousSum + value;
						categoryValueMap.put(category, result[row]);
						}
					}
				}
			mResultMap.put(categoryColumn + 0x00010000 * valueColumn, result);
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
		Object param2 = inStack.pop();
		Object param1 = inStack.pop();

		if (!(param1 instanceof String))
			throw new ParseException("1st parameter type is not 'String'");
		if (!(param2 instanceof String))
			throw new ParseException("2nd parameter type is not 'String'");

		int valueColumn = mTableModel.findColumn((String)param2);
		if (valueColumn == -1)
			throw new ParseException("Column '"+param2+"' not found.");

		int categoryColumn = mTableModel.findColumn((String)param1);
		if (categoryColumn == -1)
			throw new ParseException("Column '"+param1+"' not found.");

		if (!mTableModel.isColumnTypeCategory(categoryColumn))
			throw new ParseException("Column '"+param1+"' does not contain category values.");

		if (mTableModel.isMultiEntryColumn(categoryColumn))
			throw new ParseException("Some cells of category column '"+param1+"' contain multiple values.");

		if (!mTableModel.isColumnTypeDouble(valueColumn))
			throw new ParseException("Column '"+param2+"' does not contain numerical values.");

		inStack.push(Double.valueOf(getResult(valueColumn, categoryColumn)));
		}
	}
