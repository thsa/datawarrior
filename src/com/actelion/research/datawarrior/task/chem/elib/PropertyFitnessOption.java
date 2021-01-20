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

package com.actelion.research.datawarrior.task.chem.elib;

import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.prediction.MolecularPropertyHelper;

public class PropertyFitnessOption extends FitnessOption {
	private int mType;
	private float mHalfFitnessWidth,mValueMin,mValueMax;

	public PropertyFitnessOption(int type, String params) {
		mType = type;
		mHalfFitnessWidth = MolecularPropertyHelper.getHalfFitnessWidth(type);
		String[] param = params.split("\\t");
		if (param.length == 3) {
			mValueMin = (param[0].length() == 0) ? Float.NaN : Float.parseFloat(param[0]);
			mValueMax = (param[1].length() == 0) ? Float.NaN : Float.parseFloat(param[1]);
			mSliderValue = Integer.parseInt(param[2]);
			}
		}

	@Override public float calculateProperty(StereoMolecule mol, String[][] customColumnValueHolder) {
		return MolecularPropertyHelper.calculateProperty(mol, mType);
		}

	@Override
	public String getName() {
		return MolecularPropertyHelper.getPropertyName(mType);
		}

	@Override
	public int getResultColumnCount() {
		return 2;
		}

	@Override
	public String getResultColumnName(int i) {
		return i == 0 ? getName() : getName()+" Fitness";
		}

	@Override
	public boolean hasFitnessColumn() {
		return true;
		}

	public static String getParamError(int type, String params) {
		String[] param = params.split("\\t");
		if (param.length != 3)
			return "Wrong parameter count.";

		if (param[0].length() == 0 && param[1].length() == 0)
			return "Min and max values are empty.";

		try {
			float min = (param[0].length() == 0) ? Float.NaN : Float.parseFloat(param[0]);
			float max = (param[1].length() == 0) ? Float.NaN : Float.parseFloat(param[1]);

			if (param[0].length() != 0 && param[1].length() != 0 && min >= max)
				return "Max value of criterion must be larger than min value.";

			return null;
			}
		catch (NumberFormatException nfe) {
			return "Input value is not numerical.";
			}
		}

	@Override
	public float evaluateFitness(float propertyValue) {
		if (!Float.isNaN(mValueMin) && propertyValue < mValueMin)
			return (float)Math.exp((propertyValue-mValueMin)/mHalfFitnessWidth);
		if (!Float.isNaN(mValueMax) && propertyValue > mValueMax)
			return (float)Math.exp((mValueMax-propertyValue)/mHalfFitnessWidth);
		return 1.0f;
		}
	}
