package com.actelion.research.datawarrior.task.jep;

import com.actelion.research.util.DoubleFormat;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import java.util.Stack;

public class JEPReplaceFunction extends PostfixMathCommand {
	public JEPReplaceFunction() {
		numberOfParameters = 3;
	}

	/**
	 * Runs the square root operation on the inStack. The parameter is popped
	 * off the <code>inStack</code>, and the square root of it's value is
	 * pushed back to the top of <code>inStack</code>.
	 */
	public void run(Stack inStack) throws ParseException {

		// check the stack
		checkStack(inStack);

		// get the parameter from the stack
		Object param3 = inStack.pop();
		Object param2 = inStack.pop();
		Object param1 = inStack.pop();

		if (param3 instanceof Double)
			param3 = DoubleFormat.toString(((Double)param3).doubleValue());

			// check whether the argument is of the right type
		if (param1 instanceof String && param2 instanceof String && param3 instanceof String) {
			String p1 = (String)param1;
			String p2 = (String)param2;
			String p3 = (String)param3;

			inStack.push(p1.replaceAll(p2, p3));
			}
		else {
			throw new ParseException("All three parameters of the replace() function need to be string values.");
			}
		}
	}
