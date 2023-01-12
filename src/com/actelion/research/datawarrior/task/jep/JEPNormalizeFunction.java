package com.actelion.research.datawarrior.task.jep;

import com.actelion.research.datawarrior.task.data.DETaskAddCalculatedValues;
import com.actelion.research.table.model.CompoundTableModel;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import java.util.Stack;
import java.util.TreeMap;

public class JEPNormalizeFunction extends PostfixMathCommand {
	private DETaskAddCalculatedValues mParentTask;
	private CompoundTableModel mTableModel;
	private TreeMap<Integer,double[]> mColumnToSkalingParamMap;

	/**
	 * Constructor
	 */
	public JEPNormalizeFunction(CompoundTableModel tableModel, DETaskAddCalculatedValues parentTask) {
		super();
		mParentTask = parentTask;
		mTableModel = tableModel;
		numberOfParameters = 2;
		}

	private double[] getSkalingParams(int column) {
		if (mColumnToSkalingParamMap == null)
			mColumnToSkalingParamMap = new TreeMap<Integer,double[]>();

		double[] params = mColumnToSkalingParamMap.get(column);
		if (params == null) {
			params = new double[2];	// mean and sqrt(variance)

			// calculate mean
			int count = 0;
			for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
				float value = mTableModel.getTotalDoubleAt(row, column);
				if (!Float.isNaN(value)) {
					params[0] += value;
					count++;
					}
				}
			if (count != 0)
				params[0] /= count;

			// calculate variance
			if (count > 1) {
				double variance = 0;
				for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
					float value = mTableModel.getTotalDoubleAt(row, column);
					if (!Float.isNaN(value)) {
						double dif = (value - params[0]);
						variance += dif * dif;
						}
					}
				variance /= (count - 1);
				params[1] = Math.sqrt(variance);
				}

			mColumnToSkalingParamMap.put(column, params);
			}

		return params;
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

		// get the parameters from the stack
		Object param2 = inStack.pop();
		Object param1 = inStack.pop();

		if (!(param2 instanceof String))
			throw new ParseException("Type of 2nd parameter is not 'String'");

		int column = mTableModel.findColumn((String)param2);
		if (column == -1)
			throw new ParseException("Column '"+param2+"' not found.");
		if (!mTableModel.isColumnTypeDouble(column))
			throw new ParseException("Column '"+param2+"' is not numerical.");

		if (!(param1 instanceof Double))
			throw new ParseException("1st parameter is not numerical.");

		double value = ((Double)param1).doubleValue();

		if (Double.isNaN(value)) {
			inStack.push(new Double(Double.NaN));
			}
		else {
			double[] params = getSkalingParams(column);
			value = (value - params[0]) / params[1];
			inStack.push(new Double(value));
			}
		}
	}
