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

import com.actelion.research.calc.ProgressListener;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.prediction.MolecularPropertyHelper;
import com.actelion.research.table.model.CompoundTableModel;

public abstract class FitnessOption {
	protected int mSliderValue;

	protected static FitnessOption createFitnessOption(String params, ProgressListener pl) {
		int index = (params == null) ? -1 : params.indexOf('\t');
		if (index == -1)
			return null;
		String optionCode = params.substring(0, index);
		if (optionCode.equals(FitnessPanel.CONFORMER_OPTION_CODE))
			return new ConformerFitnessOption(params.substring(index+1), pl);
		if (optionCode.equals(FitnessPanel.STRUCTURE_OPTION_CODE))
			return new StructureFitnessOption(params.substring(index+1), pl);
		int type = MolecularPropertyHelper.getTypeFromCode(optionCode);
		return (type == -1) ? null : new PropertyFitnessOption(type, params.substring(index+1));
		}

	/**
	 * @return 0.25 ... 1.0 ... 4.0
	 */
	public float getWeight() {
		return (float)Math.pow(4.0f, (float)mSliderValue / 100.0f);
		}

	public static String getParamError(String params) {
		int index = (params == null) ? -1 : params.indexOf('\t');
		if (index == -1)
			return "Fitness option error.";
		String optionCode = params.substring(0, index);
		if (optionCode.equals(FitnessPanel.CONFORMER_OPTION_CODE))
			return ConformerFitnessOption.getParamError(params.substring(index+1));
		if (optionCode.equals(FitnessPanel.STRUCTURE_OPTION_CODE))
			return StructureFitnessOption.getParamError(params.substring(index+1));
		int type = MolecularPropertyHelper.getTypeFromCode(optionCode);
		return (type == -1) ? "Fitness option error." : PropertyFitnessOption.getParamError(type, params.substring(index+1));
		}

	/**
	 * @param mol
	 * @param columnValueHolder may return values for custom column values beyond the default columns
	 * @return
	 */
	public abstract float calculateProperty(StereoMolecule mol, String[][] columnValueHolder);
	public abstract float evaluateFitness(float propertyValue);
	public abstract String getName();

	public boolean hasDeferredColumnValues() { return false; }
	public void calculateDeferredColumnValues(StereoMolecule mol, String[] v) {}

	/**
	 * Default is to have one result column named as the fitness option itself containing the property value
	 * @return no of result columns
	 */
	public int getResultColumnCount() {
		return 1;
		}

	/**
	 * Default is to have one result column named as the fitness option itself
	 * @return result column header
	 */
	public String getResultColumnName(int i) {
		return i==0 ? getName() : null;
		}

	/**
	 * If the property value is speaking for itself (e.g. compound similarity), then there is no need for a fitness column.
	 * If there is a fitness column, then it is the second one, just after the property column.
	 * @return whether there is a fitness column
	 */
	public boolean hasFitnessColumn() {
		return false;
		}

	public void setResultColumnProperties(int i, CompoundTableModel tableModel, int column) {}
	}
