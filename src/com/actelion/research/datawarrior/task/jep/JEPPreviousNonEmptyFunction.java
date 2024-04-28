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
public class JEPPreviousNonEmptyFunction extends PostfixMathCommand {
	private final DETaskAddCalculatedValues mParentTask;
	private final CompoundTableModel mTableModel;
	private TreeMap<Integer,String[]> mResultMap;

	/**
	 * Constructor
	 */
	public JEPPreviousNonEmptyFunction(CompoundTableModel tableModel, DETaskAddCalculatedValues parentTask) {
		super();
		mParentTask = parentTask;
		mTableModel = tableModel;
		numberOfParameters = 1;
	}

	private String getResult(int valueColumn) {
		if (mResultMap == null)
			mResultMap = new TreeMap<>();

		String[] result = mResultMap.get(valueColumn);
		if (result == null) {
			result = new String[mTableModel.getTotalRowCount()];
			String value = "";
			for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
				String v = mTableModel.encodeData(mTableModel.getTotalRecord(row), valueColumn);
				if (!v.isEmpty())
					value = v;
				result[row] = value;
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

		inStack.push(getResult(valueColumn));
	}
}
