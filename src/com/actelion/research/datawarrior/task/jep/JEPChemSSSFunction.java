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
import com.actelion.research.chem.SSSearcher;
import com.actelion.research.chem.SSSearcherWithIndex;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorHandlerStandardFactory;
import com.actelion.research.table.model.CompoundTableModel;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

import java.util.Stack;

/**
 * An Actelion custom function class for JEP
 * to calculate structural similarity to a given compound
 * applying a specified descriptor
 */


public class JEPChemSSSFunction extends PostfixMathCommand {
	public static final String FUNCTION_NAME = "chemsss";

	private CompoundTableModel mTableModel;
	private String mPreviousChemCode;
	private StereoMolecule mPreviousFragment;
	private long[] mPreviousDescriptor;

	public JEPChemSSSFunction(CompoundTableModel tableModel) {
		super();
		mTableModel = tableModel;
		numberOfParameters = 3;
	}

	/**
	 * Runs the operation on the inStack. The parameters are popped
	 * off the <code>inStack</code>, and the square root of its value is
	 * pushed back to the top of <code>inStack</code>.
	 */

	@Override
	public void run(Stack inStack) throws ParseException {
		// check the stack
		checkStack(inStack);

		// get the parameters from the stack
		Object param3 = inStack.pop();
		Object param2 = inStack.pop();
		Object param1 = inStack.pop();

		// check whether the argument is of the right type

		if (param1 instanceof JEPParameter
		 && (param2 instanceof JEPParameter || param2 instanceof String)
		 && param3 instanceof Double) {

			int mode = ((Double)param3).intValue();
			if (mode < 0 || mode > 3)
				throw new ParseException("3rd parameter of chemsss() must be 0, 1, 2, or 3.");

			int countMode = (mode == 0) ? SSSearcher.cCountModeExistence
						  : (mode == 1) ? SSSearcher.cCountModeSeparated
						  : (mode == 2) ? SSSearcher.cCountModeOverlapping
						  : SSSearcher.cCountModeRigorous;

					// calculate the result
			JEPParameter jepParam1 = (JEPParameter)param1;
			if (jepParam1 == null || !mTableModel.isColumnTypeStructure(jepParam1.column))
				throw new ParseException("1st parameter of chemsss() is not a structure column.");

			StereoMolecule molecule = mTableModel.getChemicalStructure(jepParam1.record, jepParam1.column, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
			if (molecule == null)
				throw new ParseException("1st parameter of chemsss() is empty");

			int descriptor1Column = mTableModel.getChildColumn(jepParam1.column, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
			long[] moleculeFFP = (descriptor1Column == -1) ? null : (long[])jepParam1.record.getData(descriptor1Column);

			StereoMolecule fragment = null;
			long[] fragmentFFP = null;

			if (param2 instanceof JEPParameter) {
				JEPParameter jepParam2 = (JEPParameter)param2;
				if (jepParam2 == null || !mTableModel.isColumnTypeStructure(jepParam2.column))
					throw new ParseException("2nd parameter of chemsss() is not a structure column.");

				fragment = mTableModel.getChemicalStructure(jepParam2.record, jepParam2.column, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
				if (fragment == null)
					throw new ParseException("2nd parameter of chemsss() is empty");

				int descriptor2Column = mTableModel.getChildColumn(jepParam2.column, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
				fragmentFFP = (descriptor2Column == -1) ? null : (long[])jepParam2.record.getData(descriptor2Column);
				}

			if (param2 instanceof String) {
				if (!param2.equals(mPreviousChemCode)) {
					try {
						mPreviousChemCode = (String)param2;
						mPreviousFragment = new IDCodeParser(DescriptorConstants.DESCRIPTOR_FFP512.needsCoordinates).getCompactMolecule(mPreviousChemCode);
						mPreviousFragment.setFragment(true);
						mPreviousDescriptor = (long[])DescriptorHandlerStandardFactory.getFactory().getDefaultDescriptorHandler(DescriptorConstants.DESCRIPTOR_FFP512.shortName).createDescriptor(mPreviousFragment);
						}
					catch (Exception e) {
						throw new ParseException("Second parameter of chemsss() is invalid.");
						}
					}
				fragment = mPreviousFragment;
				fragmentFFP = mPreviousDescriptor;
				}

			if (moleculeFFP != null && fragmentFFP != null) {
				SSSearcherWithIndex searcher = new SSSearcherWithIndex();
				searcher.setMolecule(molecule, moleculeFFP);
				searcher.setFragment(fragment, fragmentFFP);
				inStack.push(Double.valueOf(searcher.findFragmentInMolecule(countMode, SSSearcher.cDefaultMatchMode)));
				}
			else {
				SSSearcher searcher = new SSSearcher();
				searcher.setMolecule(molecule);
				searcher.setFragment(fragment);
				inStack.push(Double.valueOf(searcher.findFragmentInMolecule(countMode, SSSearcher.cDefaultMatchMode)));
				}
			}
		else {
			throw new ParseException("Invalid parameter type");
			}
		}
	}
