package com.actelion.research.datawarrior.task.jep;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import java.util.Stack;

public class JEPTodayFunction extends PostfixMathCommand {

	/**
	 * Constructor
	 */
	public JEPTodayFunction() {
		super();
		numberOfParameters = 0;
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
		long currentMillis = System.currentTimeMillis();
		double days = currentMillis / 86400000L;
		inStack.push(new Double(days));
	}
}
