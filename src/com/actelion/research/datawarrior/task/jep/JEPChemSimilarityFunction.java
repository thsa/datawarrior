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

import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorHandler;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.chem.reaction.ReactionEncoder;
import com.actelion.research.table.model.CompoundTableModel;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import java.util.Stack;

/**
 * An Actelion custom function class for JEP
 * to calculate structural similarity to a given compound
 * applying a specified descriptor
 */


public class JEPChemSimilarityFunction extends PostfixMathCommand {
	public static final String FUNCTION_NAME = "chemsim";

	private CompoundTableModel mTableModel;
    private String mPreviousChemCode;
    private Object mDescriptor;

    /**
	 * Constructor
	 */

	public JEPChemSimilarityFunction(CompoundTableModel tableModel) {
        super();
        mTableModel = tableModel;
		numberOfParameters = 2;
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

		// get the parameters from the stack
        Object param2 = inStack.pop();
		Object param1 = inStack.pop();

		// check whether the argument is of the right type

		if (param1 instanceof JEPParameter
         && (param2 instanceof JEPParameter || param2 instanceof String)) {

            // calculate the result
            JEPParameter jepParam1 = (JEPParameter)param1;
            DescriptorHandler handler1 = (jepParam1 == null) ? null : mTableModel.getDescriptorHandler(jepParam1.column);
            if (handler1 == null)
                throw new ParseException("1st parameter of chemsim() is not a descriptor column.");

            Object value1 = (jepParam1 == null) ? null : jepParam1.record.getData(jepParam1.column);
            Object value2 = null;

            if (value1 == null)
                throw new ParseException("1st parameter of chemsim() is empty");

            if (param2 instanceof JEPParameter) {
                JEPParameter jepParam2 = (JEPParameter)param2;

                if (jepParam2.record.getData(jepParam2.column) == null)
                    throw new ParseException("2nd parameter of chemsim() is empty");

                if (mTableModel.isDescriptorColumn(jepParam2.column)) {
	                DescriptorHandler handler2 = mTableModel.getDescriptorHandler(jepParam2.column);
	                if (!handler1.getInfo().shortName.equals(handler2.getInfo().shortName))
	                    throw new ParseException("1st and 2nd parameters of chemsim() are incompatible descriptors.");
	                value2 = (jepParam2 == null) ? null : jepParam2.record.getData(jepParam2.column);
	                }
	            else if (mTableModel.isColumnTypeStructure(jepParam2.column)) {
	            	if (!mTableModel.isColumnTypeStructure(mTableModel.getParentColumn(jepParam1.column)))
		                throw new ParseException("2st parameter of chemsim() refers to reactions while the 1st doesn't.");
                    Object chemObject = mTableModel.getChemicalStructure(jepParam2.record, jepParam2.column, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
                    value2 = handler1.createDescriptor(chemObject);
	            	}
	            else if (mTableModel.isColumnTypeReaction(jepParam2.column)) {
	            	if (!mTableModel.isColumnTypeReaction(mTableModel.getParentColumn(jepParam1.column)))
		                throw new ParseException("2st parameter of chemsim() refers to molecules while the 1st doesn't.");
                    Object chemObject = mTableModel.getChemicalReaction(jepParam2.record, jepParam2.column, CompoundTableModel.ATOM_COLOR_MODE_NONE);
                    value2 = handler1.createDescriptor(chemObject);
	            	}
	            else {
	                throw new ParseException("2nd parameter of chemsim() is neither a chemical descriptor nor a chemical object.");
			        }
            	}

            if (param2 instanceof String) {
                if (!param2.equals(mPreviousChemCode)) {
                    try {
    	            	if (mTableModel.isColumnTypeStructure(mTableModel.getParentColumn(jepParam1.column))) {
    	            		mPreviousChemCode = (String)param2;
    	            		StereoMolecule mol = new IDCodeParser(handler1.getInfo().needsCoordinates).getCompactMolecule(mPreviousChemCode);
                            mDescriptor = handler1.createDescriptor(mol);
    	            		}
    	            	else if (mTableModel.isColumnTypeReaction(mTableModel.getParentColumn(jepParam1.column))) {
    	            		mPreviousChemCode = (String)param2;
    	            		Reaction rxn = ReactionEncoder.decode(mPreviousChemCode, handler1.getInfo().needsCoordinates);
                            mDescriptor = handler1.createDescriptor(rxn);
    	            		}
                        }
                    catch (Exception e) {
                        throw new ParseException("Second parameter of chemsim() is invalid.");
                        }
                    }
            	value2 = mDescriptor;
            	}

            double similarity = handler1.getSimilarity(value1, value2);
            inStack.push(new Double(similarity));
		    }
        else {
            throw new ParseException("Invalid parameter type");
    		}
    	}
    }
