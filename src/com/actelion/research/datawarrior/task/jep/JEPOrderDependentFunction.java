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
public class JEPOrderDependentFunction extends PostfixMathCommand {
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
	public JEPOrderDependentFunction(CompoundTableModel tableModel, int type, DETaskAddCalculatedValues parentTask) {
		super();
		mParentTask = parentTask;
		mTableModel = tableModel;
		mType = type;
		numberOfParameters = 1;
	}

	private double getResult(int valueColumn) {
		if (mResultMap == null)
			mResultMap = new TreeMap<>();

		double[] result = mResultMap.get(valueColumn);
		if (result == null) {
			result = new double[mTableModel.getTotalRowCount()];
			if (mType == TYPE_INCREASE || mType == TYPE_PERCENT_INCREASE) {
				double previousValue = Double.NaN;
				for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
					double value = mTableModel.getTotalDoubleAt(row, valueColumn);
					if (Double.isNaN(previousValue)) {
						result[row] = Double.NaN;
						}
					else {
						result[row] = Double.isNaN(value) ? Double.NaN
								: (mType == TYPE_INCREASE) ? value - previousValue : (value - previousValue) / previousValue;
						}
					previousValue = Double.isNaN(value) ? Double.NaN : value;
					}
				}
			else if (mType == TYPE_CUMULATIVE_SUM) {
				double sum = 0.0;
				for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
					double value = mTableModel.getTotalDoubleAt(row, valueColumn);
					if (Double.isNaN(value))
						value = 0;
					sum += value;
					result[row] = sum;
					}
				}
			mResultMap.put(valueColumn, result);
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
		Object param = inStack.pop();

		if (!(param instanceof String))
			throw new ParseException("Parameter type is not 'String'");

		int valueColumn = mTableModel.findColumn((String)param);
		if (valueColumn == -1)
			throw new ParseException("Column '"+param+"' not found.");

		if (!mTableModel.isColumnTypeDouble(valueColumn))
			throw new ParseException("Column '"+param+"' does not contain numerical values.");

		inStack.push(new Double(getResult(valueColumn)));
		}
	}
