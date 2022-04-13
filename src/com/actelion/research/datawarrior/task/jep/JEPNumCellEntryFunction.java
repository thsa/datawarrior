package com.actelion.research.datawarrior.task.jep;

import com.actelion.research.datawarrior.task.data.DETaskAddCalculatedValues;
import com.actelion.research.table.model.CompoundTableModel;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import java.util.Arrays;
import java.util.Stack;

/**
 * A custom function class for JEP to return the n-th entry from a table cell
 * in numerical form.
 * syntax: numcellentry(String columnName, int n, int sortMode)
 */
public class JEPNumCellEntryFunction extends PostfixMathCommand {
	private DETaskAddCalculatedValues mParentTask;
	private CompoundTableModel mTableModel;

	public JEPNumCellEntryFunction(DETaskAddCalculatedValues parentTask, CompoundTableModel tableModel) {
		mParentTask = parentTask;
		mTableModel = tableModel;
		numberOfParameters = 3;
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

		// check whether the argument is of the right type
		if (!(param1 instanceof String))
			throw new ParseException("First parameter (column name) is not a string value.");
		if (!(param2 instanceof Double))
			throw new ParseException("Second parameter (n) is not numerical.");
		if (!(param3 instanceof Double))
			throw new ParseException("Third parameter (sort mode) is not numerical.");

		int column = mTableModel.findColumn((String)param1);
		if (column == -1)
			throw new ParseException("Column '"+param1+"' not found.");
		if (!mTableModel.isColumnTypeDouble(column))
			throw new ParseException("Column '"+param1+"' not numerical.");

		int n = (int)Math.round((Double)param2) - 1;

		int sortMode = (int)Math.round((Double)param3);
		if (sortMode < 0 || sortMode > 2)
			throw new ParseException("Third parameter (sort mode) must be 0, 1, or 2.");

		int row = mParentTask.getCurrentRow();

		String value =  (row < 1 || row > mTableModel.getTotalRowCount()) ?
				null : mTableModel.getTotalValueAt(row, column);

		double nthValue = Double.NaN;

		if (value != null) {
			String[] entry = mTableModel.separateEntries(value);

			if (n >= 0 && n <entry.length) {
				double[] numValue = new double[entry.length];
				for (int i = 0; i<entry.length; i++)
					numValue[i] = mTableModel.tryParseEntry(entry[i], column);

				if (sortMode != 0)
					Arrays.sort(numValue);

				if (sortMode == 2)
					n = entry.length - n - 1;

				nthValue = numValue[n];
				}
			}

		// push the result on the inStack
		inStack.push(nthValue);
		}
	}
