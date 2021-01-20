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

import java.util.Comparator;
import java.util.Stack;
import java.util.TreeMap;

import com.actelion.research.datawarrior.task.data.DETaskAddCalculatedValues;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.ByteArrayComparator;

/**
 * An Actelion custom function class for JEP
 * to lookup a reference value within a category defined by an identifier re-occuring in evey category.
 * Parameters: refID column name, category column name, value column name, reference id, category
 */
public class JEPRefValueOfCategoryFunction extends PostfixMathCommand {
	private DETaskAddCalculatedValues mParentTask;
	private CompoundTableModel		mTableModel;
    private TreeMap<byte[],byte[]>	mCategoryValueMap;

	public JEPRefValueOfCategoryFunction(DETaskAddCalculatedValues parentTask, CompoundTableModel tableModel) {
        super();
		mParentTask = parentTask;
		mTableModel = tableModel;
		numberOfParameters = 4;
	    }

	private void createCategoryValueMap(int refIDColumn, int categoryColumn, int valueColumn, byte[] refID) {
		if (mCategoryValueMap == null) {
			Comparator<byte[]> comparator = new ByteArrayComparator();
			mCategoryValueMap = new TreeMap<byte[],byte[]>(comparator);
			for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
				byte[] id = (byte[])mTableModel.getTotalRecord(row).getData(refIDColumn);
				if (id != null && comparator.compare(refID, id) == 0) {
					byte[] category = (byte[])mTableModel.getTotalRecord(row).getData(categoryColumn);
					byte[] value = (byte[])mTableModel.getTotalRecord(row).getData(valueColumn);
					byte[] oldValue = mCategoryValueMap.get(category);
					if (oldValue == null)
						mCategoryValueMap.put(category, value);
					else
						mCategoryValueMap.put(category, append(oldValue, value));
					}
				}
			}
		}

	private byte[] append(byte[] oldValue, byte[] newValue) {
		byte[] merged = new byte[oldValue.length+1+newValue.length];
		int i = 0;
		for (byte b:oldValue)
			merged[i++] = b;
		merged[i++] = CompoundTableConstants.cLineSeparatorByte;
		for (byte b:newValue)
			merged[i++] = b;
		return merged;
		}

	/**
	 * Runs the operation on the inStack. The parameters are popped
	 * off the <code>inStack</code>, and the square root of it's value is 
	 * pushed back to the top of <code>inStack</code>.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void run(Stack inStack) throws ParseException {

		// check the stack
		checkStack(inStack);

		// get the parameters from the stack
		Object param4 = inStack.pop();
		Object param3 = inStack.pop();
		Object param2 = inStack.pop();
		Object param1 = inStack.pop();

		// check whether the argument is of the right type
		if (param1 instanceof String
		 && param2 instanceof String
		 && param3 instanceof String
		 && param4 instanceof String) {
			if (mCategoryValueMap == null) {
				int refIDColumn = mTableModel.findColumn((String)param1);
				if (refIDColumn == -1)
					throw new ParseException("Column '"+param1+"' not found.");
	
				int categoryColumn = mTableModel.findColumn((String)param2);
				if (categoryColumn == -1)
					throw new ParseException("Column '"+param2+"' not found.");
	
				int valueColumn = mTableModel.findColumn((String)param3);
				if (valueColumn == -1)
					throw new ParseException("Column '"+param3+"' not found.");
	
				byte[] refID = ((String)param4).getBytes();
				if (refID == null)	// user cancelled input
					throw new ParseException("No reference-ID specified.");

				createCategoryValueMap(refIDColumn, categoryColumn, valueColumn, refID);
				}

			int categoryColumn = mTableModel.findColumn((String)param2);
			byte[] category = (byte[])mTableModel.getTotalRecord(mParentTask.getCurrentRow()).getData(categoryColumn);
			byte[] refValue = mCategoryValueMap.get(category);

			inStack.push((refValue == null) ? "" : new String(refValue));
		    }
		else {
			throw new ParseException("Invalid parameter type");
		    }
	    }
    }

