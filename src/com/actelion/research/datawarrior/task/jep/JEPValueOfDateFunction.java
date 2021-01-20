package com.actelion.research.datawarrior.task.jep;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import java.util.Calendar;
import java.util.Stack;

public class JEPValueOfDateFunction extends PostfixMathCommand {
	private int mType,mOffset;

	/**
	 * Constructor
	 */
	public JEPValueOfDateFunction(int type, int offset) {
		super();
		mType = type;
		mOffset = offset;
		numberOfParameters = 1;
	}

	/**
	 * Runs the operation on the inStack. The parameter is popped
	 * off the <code>inStack</code>, and the date value value is
	 * pushed back to the top of <code>inStack</code>.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void run(Stack inStack) throws ParseException {

		// check the stack
		checkStack(inStack);

		// get the parameter from the stack
		Object param = inStack.pop();

		if (!(param instanceof Double))
			throw new ParseException("Parameter type is not numerical");

		double date = ((Double) param).doubleValue();

		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(43200000L + 86400000L * (long)date);
		if (mType == Calendar.WEEK_OF_YEAR || mType == Calendar.YEAR) {
			if (mOffset == 0) { // ISO 8601: Week starts on Monday and Jan 4st is in week 1
				calendar.setFirstDayOfWeek(Calendar.MONDAY);
				calendar.setMinimalDaysInFirstWeek(4);
				}
			if (mOffset == 1) { // Jan 1st is in week 1, week starts with Monday
				calendar.setFirstDayOfWeek(Calendar.MONDAY);
				calendar.setMinimalDaysInFirstWeek(1);
				}
			if (mOffset == 2) { // Jan 1st is in week 1, week starts with Monday
				calendar.setFirstDayOfWeek(Calendar.SUNDAY);
				calendar.setMinimalDaysInFirstWeek(1);
				}
			if (mOffset == 3) { // Jan 1st is in week 1, week starts with Monday
				calendar.setFirstDayOfWeek(Calendar.SATURDAY);
				calendar.setMinimalDaysInFirstWeek(1);
				}
			}
		double value = calendar.get(mType);
		if (mType == Calendar.MONTH)
			value += 1.0;
		if (mType == Calendar.DAY_OF_WEEK && mOffset != 0) {
			value += mOffset;
			if (value < 1)
				value += 7;
			else if (value > 7)
				value -= 7;
			}
		if (mType == Calendar.YEAR && mOffset != -1) {
			int week = calendar.get(Calendar.WEEK_OF_YEAR);
			int month = calendar.get(Calendar.MONTH);
			if (week == 1 && month == 11)
				value++;
			else if (week > 50 && month == 0)
				value--;
			}
		inStack.push(new Double(value));
		}
	}

