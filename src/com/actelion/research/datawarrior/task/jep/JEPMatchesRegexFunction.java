/*
 * Copyright 2020 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91, CH-4123 Allschwil, Switzerland
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

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import java.util.Stack;

/**
 * An example custom function class for JEP.
 */
public class JEPMatchesRegexFunction extends PostfixMathCommand {

	/**
	 * Constructor
	 */
	public JEPMatchesRegexFunction() {
		super();
		numberOfParameters = 2;
		}

	public void run(Stack inStack) throws ParseException {

		// check the stack
		checkStack(inStack);

		// get the parameter from the stack
		Object param2 = inStack.pop();
		Object param1 = inStack.pop();

		// check whether the argument is of the right type
		if (!(param1 instanceof String))
			throw new ParseException("First parameter is not a string value.");
		if (!(param2 instanceof String))
			throw new ParseException("Second parameter is not a string value.");

		String regex = ((String)param2).replace("\\", "\\\\");

		inStack.push(new Double(((String)param1).matches(regex) ? 1 : 0));
	}
}
