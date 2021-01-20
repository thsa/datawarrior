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
import com.actelion.research.util.ByteArrayComparator;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import java.util.Arrays;
import java.util.Stack;
import java.util.TreeMap;

/**
 * An example custom function class for JEP.
 */
public class JEPValueInCategoryFunction extends PostfixMathCommand {
	public static final int TYPE_FIRST = 1;
	public static final int TYPE_MIN = 2;
	public static final int TYPE_MAX = 3;
	public static final int TYPE_MEAN = 4;
	public static final int TYPE_MEDIAN = 5;
	public static final int TYPE_SUM = 6;

	private DETaskAddCalculatedValues mParentTask;
	private CompoundTableModel mTableModel;
	private TreeMap<Integer,TreeMap<byte[],Double>> mBytesMaps;
	private TreeMap<Integer,TreeMap<Double,Double>> mDoubleMaps;
	private int mType;

	/**
	 * Constructor
	 */
	public JEPValueInCategoryFunction(CompoundTableModel tableModel, int type, DETaskAddCalculatedValues parentTask) {
		super();
		mParentTask = parentTask;
		mTableModel = tableModel;
		mType = type;
		numberOfParameters = 2;
		}

	private TreeMap<byte[],Double> createBytesMap(int categoryColumn, int valueColumn) {
		if (mBytesMaps == null)
			mBytesMaps = new TreeMap<>();

		TreeMap<byte[],Double> bytesMap = mBytesMaps.get(categoryColumn + 0x00010000 * valueColumn);
		if (bytesMap == null) {
			bytesMap = new TreeMap<>(new ByteArrayComparator());
			TreeMap<byte[],Integer> countMap = (mType == TYPE_MEAN || mType == TYPE_MEDIAN) ?
					new TreeMap<>(new ByteArrayComparator()) : null;

			for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
				byte[] key = (byte[])mTableModel.getTotalRecord(row).getData(categoryColumn);
				if (key != null) {
					double val = mTableModel.getTotalOriginalDoubleAt(row, valueColumn);
					if (Double.isFinite(val)) {
						switch (mType) {
						case TYPE_FIRST:
							if (!bytesMap.containsKey(key))
								bytesMap.put(key, (double)mTableModel.getTotalOriginalDoubleAt(row, valueColumn));
							break;
						case TYPE_MIN:
							Double min = bytesMap.get(key);
							bytesMap.put(key, min == null ? val : Math.min(val, min));
							break;
						case TYPE_MAX:
							Double max = bytesMap.get(key);
							bytesMap.put(key, max == null ? val : Math.max(val, max));
							break;
						case TYPE_SUM:
						case TYPE_MEAN:
							Double sum = bytesMap.get(key);
							bytesMap.put(key, sum == null ? val : sum + val);
							break;
							}

						if (mType == TYPE_MEAN || mType == TYPE_MEDIAN) {
							Integer count = countMap.get(key);
							countMap.put(key, count == null ? 1 : count + 1);
							}
						}
					}
				}

			if (mType == TYPE_MEAN) {
				for (byte[] key:countMap.keySet())
					bytesMap.put(key, bytesMap.get(key) / countMap.get(key));
				}
			else if (mType == TYPE_MEDIAN) {
				TreeMap<byte[],double[]> valueMap = new TreeMap<>(new ByteArrayComparator());
				for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
					byte[] key = (byte[])mTableModel.getTotalRecord(row).getData(categoryColumn);
					if (key != null) {
						double val = mTableModel.getTotalOriginalDoubleAt(row, valueColumn);
						if (Double.isFinite(val)) {
							double[] values = valueMap.get(key);
							if (values == null) {
								values = new double[countMap.get(key)];
								values[0] = val;
								valueMap.put(key, values);
								countMap.put(key, 1);
								}
							else {
								int count = countMap.get(key);
								values[count] = val;
								countMap.put(key, count + 1);
								}
							}
						}
					}
				for (byte[] key:valueMap.keySet()) {
					double[] values = valueMap.get(key);
					Arrays.sort(values);
					int count = values.length;
					bytesMap.put(key, (count & 1) == 0 ? (values[count/2-1] + values[count/2]) / 2 : values[count/2]);
					}
				}
			mBytesMaps.put(categoryColumn + 0x00010000 * valueColumn, bytesMap);
			}

		return bytesMap;
		}

	private TreeMap<Double,Double> createDoubleMap(int categoryColumn, int valueColumn) {
		if (mDoubleMaps == null)
			mDoubleMaps = new TreeMap<>();

		TreeMap<Double,Double> doubleMap = mDoubleMaps.get(categoryColumn + 0x00010000 * valueColumn);
		if (doubleMap == null) {
			doubleMap = new TreeMap<>();
			TreeMap<Double, Integer> countMap = (mType == TYPE_MEAN || mType == TYPE_MEDIAN) ?
					new TreeMap<>() : null;

			for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
				double key = mTableModel.getTotalOriginalDoubleAt(row, categoryColumn);
				if (!Double.isNaN(key)) {
					double val = mTableModel.getTotalOriginalDoubleAt(row, valueColumn);
					if (Double.isFinite(val)) {
						switch (mType) {
							case TYPE_FIRST:
								if (!doubleMap.containsKey(key))
									doubleMap.put(key, (double)mTableModel.getTotalOriginalDoubleAt(row, valueColumn));
								break;
							case TYPE_MIN:
								Double min = doubleMap.get(key);
								doubleMap.put(key, min == null ? val : Math.min(val, min));
								break;
							case TYPE_MAX:
								Double max = doubleMap.get(key);
								doubleMap.put(key, max == null ? val : Math.max(val, max));
								break;
							case TYPE_SUM:
							case TYPE_MEAN:
								Double sum = doubleMap.get(key);
								doubleMap.put(key, sum == null ? val : sum + val);
								break;
						}

						if (mType == TYPE_MEAN || mType == TYPE_MEDIAN) {
							Integer count = countMap.get(key);
							countMap.put(key, count == null ? 1 : count + 1);
						}
					}
				}
			}

			if (mType == TYPE_MEAN) {
				for (double key:countMap.keySet())
					doubleMap.put(key, doubleMap.get(key) / countMap.get(key));
				}
			else if (mType == TYPE_MEDIAN) {
				TreeMap<Double,double[]> valueMap = new TreeMap<Double,double[]>();
				for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
					double key = mTableModel.getTotalOriginalDoubleAt(row, categoryColumn);
					if (!Double.isNaN(key)) {
						double val = mTableModel.getTotalOriginalDoubleAt(row, valueColumn);
						if (Double.isFinite(val)) {
							double[] values = valueMap.get(key);
							if (values == null) {
								values = new double[countMap.get(key)];
								values[0] = val;
								valueMap.put(key, values);
								countMap.put(key, 1);
							}
							else {
								int count = countMap.get(key);
								values[count] = val;
								countMap.put(key, count + 1);
							}
						}
					}
				}
				for (double key:valueMap.keySet()) {
					double[] values = valueMap.get(key);
					Arrays.sort(values);
					int count = values.length;
					doubleMap.put(key, (count & 1) == 0 ? (values[count/2-1] + values[count/2]) / 2 : values[count/2]);
					}
				}

			mDoubleMaps.put(categoryColumn + 0x00010000 * valueColumn, doubleMap);
			}

		return doubleMap;
		}

	/**
	 * Runs the operation on the inStack. The parameters are popped
	 * off the <code>inStack</code>, and the result is pushed back to the top of <code>inStack</code>.
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

		boolean isDoubleCategory = mTableModel.isColumnTypeDouble(categoryColumn);
		Object category = isDoubleCategory ?
				new Double(mTableModel.getTotalOriginalDoubleAt(mParentTask.getCurrentRow(), categoryColumn))
			  : mTableModel.getTotalValueAt(mParentTask.getCurrentRow(), categoryColumn);

		if (isDoubleCategory && !mTableModel.isColumnTypeDouble(categoryColumn))
			throw new ParseException("1st parameter is numerical, but column '"+param1+"' is not.");
		if (!isDoubleCategory && mTableModel.isColumnTypeDouble(categoryColumn))
			throw new ParseException("Column '"+param1+"' is numerical, but 1st parameter is not.");

		int valueColumn = mTableModel.findColumn((String)param2);
		if (valueColumn == -1)
			throw new ParseException("Column '"+param2+"' not found.");
		if (mType == TYPE_SUM && (!mTableModel.isColumnTypeDouble(valueColumn) || mTableModel.isColumnTypeDate(valueColumn)))
			throw new ParseException("Column '"+param2+"' is not numerical.");
		if (!mTableModel.isColumnTypeDouble(valueColumn))
			throw new ParseException("Column '"+param2+"' is not numerical nor date.");

		if (isDoubleCategory)
			inStack.push(new Double(createDoubleMap(categoryColumn, valueColumn).get(category)));
		else
			inStack.push(new Double(createBytesMap(categoryColumn, valueColumn).get(((String)category).getBytes())));
		}
	}
