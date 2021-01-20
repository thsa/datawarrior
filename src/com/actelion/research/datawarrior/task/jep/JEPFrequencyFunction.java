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

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.ByteArrayComparator;

/**
 * An example custom function class for JEP.
 */
public class JEPFrequencyFunction extends PostfixMathCommand {
	private CompoundTableModel mTableModel;
    private TreeMap<Integer,TreeMap<byte[],Integer>> mByteArrayMaps;
    private TreeMap<Integer,TreeMap<Double,Integer>> mDoubleMaps;

	/**
	 * Constructor
	 */
	public JEPFrequencyFunction(CompoundTableModel tableModel) {
        super();
        mTableModel = tableModel;
		numberOfParameters = 2;
		}

	private TreeMap<byte[],Integer> createByteArrayMap(int column) {
		if (mByteArrayMaps == null)
			mByteArrayMaps = new TreeMap<>();

		TreeMap<byte[],Integer> byteArrayMap = mByteArrayMaps.get(column);
		if (byteArrayMap == null) {
			byteArrayMap = new TreeMap<byte[],Integer>(new ByteArrayComparator());
			for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
				byte[] key = (byte[])mTableModel.getTotalRecord(row).getData(column);
				if (key != null) {
					Integer count = byteArrayMap.get(key);
					if (count == null)
						byteArrayMap.put(key, new Integer(1));
					else
						byteArrayMap.put(key, new Integer(count.intValue()+1));
					}
				}
			mByteArrayMaps.put(column, byteArrayMap);
			}
		return byteArrayMap;
		}

	private TreeMap<Double,Integer> createDoubleMap(int column) {
		if (mDoubleMaps == null)
			mDoubleMaps = new TreeMap<>();

		TreeMap<Double,Integer> doubleMap = mDoubleMaps.get(column);
		if (doubleMap == null) {
			doubleMap = new TreeMap<>();
			for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
				Double key = new Double(mTableModel.getTotalRecord(row).getDouble(column));
//				if (!Double.isNaN(key)) {
					Integer count = doubleMap.get(key);
					if (count == null)
						doubleMap.put(key, new Integer(1));
					else
						//count++;
						doubleMap.put(key, new Integer(count.intValue()+1));
//					}
				}
			mDoubleMaps.put(column, doubleMap);
			}
		return doubleMap;
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

		if (param2 instanceof String) {
			int column = mTableModel.findColumn((String)param2);
			if (column == -1)
				throw new ParseException("Column '"+param2+"' not found.");

			if (param1 instanceof String || param1 instanceof Double || param1 instanceof JEPParameter) {
				if (mTableModel.isColumnTypeDouble(column)) {
					if (param1 instanceof Double) {
						Integer co = createDoubleMap(column).get((Double)param1);
						int count = (co == null) ? 0 : co.intValue();
						inStack.push(new Double(count));
						}
					else {
						throw new ParseException("1st parameter is not numerical.");
						}
					}
				else {
					if (param1 instanceof String || param1 instanceof JEPParameter) {
						byte[] bytes = null;
						if (param1 instanceof String) {
							bytes = ((String)param1).getBytes();
							}
						else {
							JEPParameter jepp = (JEPParameter)param1;
							if (jepp.record != null)
								bytes = (byte[])jepp.record.getData(jepp.column);
							}
						Integer co = createByteArrayMap(column).get(bytes);
						int count = (co == null) ? 0 : co.intValue();
						inStack.push(new Double(count));
						}
					else {
						throw new ParseException("1st parameter is not type 'String'.");
						}
					}
				}
			else {
				throw new ParseException("Invalid parameter type");
				}
			}
		else {
			throw new ParseException("2nd parameter type is not 'String'");
			}
		}
	}
