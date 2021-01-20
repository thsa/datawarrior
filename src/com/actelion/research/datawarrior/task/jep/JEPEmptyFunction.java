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

import java.util.*;
import org.nfunk.jep.*;
import org.nfunk.jep.function.*;

/**
 * An example custom function class for JEP.
 */
public class JEPEmptyFunction extends PostfixMathCommand {

	/**
	 * Constructor
	 */
	public JEPEmptyFunction() {
		numberOfParameters = 1;
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
		Object param = inStack.pop();

		boolean isEmpty = false;

		// check whether the argument is of the right type
		if (param instanceof Double) {
			isEmpty = Double.isNaN((Double)param);
		} else if (param instanceof String) {
			isEmpty = (((String)param).length() == 0);
		} else if (param instanceof JEPParameter) {
			isEmpty = (((JEPParameter)param).record.getData(((JEPParameter)param).column) == null);
		} else {
			throw new ParseException("Invalid parameter type");
		}

		inStack.push(new Double(isEmpty ? 1 : 0));
	}
}
