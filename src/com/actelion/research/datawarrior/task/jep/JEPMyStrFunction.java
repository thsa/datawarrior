package com.actelion.research.datawarrior.task.jep;

import com.actelion.research.util.DoubleFormat;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import java.util.Stack;

public class JEPMyStrFunction extends PostfixMathCommand {
	public JEPMyStrFunction() {
		numberOfParameters = 1;
		}

	public void run(Stack inStack) throws ParseException {
		checkStack(inStack);// check the stack
		Object param = inStack.pop();

		if (param instanceof Double) {
			String value = DoubleFormat.toString(((Double)param).doubleValue(), 8, true);
			inStack.push(value);
			}
		else {
			inStack.push(param.toString());
			}
		}
	}
