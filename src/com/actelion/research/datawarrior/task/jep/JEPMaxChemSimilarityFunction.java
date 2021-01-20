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

import com.actelion.research.chem.descriptor.DescriptorHandler;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import java.util.Stack;

public class JEPMaxChemSimilarityFunction extends PostfixMathCommand {
	private CompoundTableModel mTableModel;

	public JEPMaxChemSimilarityFunction(CompoundTableModel tableModel) {
		super();
		mTableModel = tableModel;
		numberOfParameters = 1;
		}

	/**
	 * Runs the operation on the inStack. The parameters are popped
	 * off the <code>inStack</code>, and the square root of it's value is
	 * pushed back to the top of <code>inStack</code>.
	 */

	@Override
	public void run(Stack inStack) throws ParseException {
		// check the stack
		checkStack(inStack);

		// get the parameter from the stack
		Object param = inStack.pop();

		// check whether the argument is of the right type

		if (param instanceof JEPParameter) {

			// calculate the result
			JEPParameter jepParam = (JEPParameter)param;
			DescriptorHandler handler = mTableModel.getDescriptorHandler(jepParam.column);
			if (handler == null)
				throw new ParseException("The parameter of maxsim() is not a descriptor column.");

			Object refDescriptor = (jepParam == null) ? null : jepParam.record.getData(jepParam.column);

			if (refDescriptor == null)
				throw new ParseException("The parameter of maxsim() is empty");

			float maxSim = Float.NaN;
			for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
				CompoundRecord record = mTableModel.getTotalRecord(row);
				if (record != jepParam.record) {
					Object descriptor = record.getData(jepParam.column);
					if (descriptor != null) {
						float similarity = handler.getSimilarity(refDescriptor, descriptor);
						if (Float.isNaN(maxSim) || maxSim < similarity)
							maxSim = similarity;
						}
					}
				}
			inStack.push(new Double(maxSim));
			}
		else {
			throw new ParseException("Invalid parameter type");
			}
		}
	}
