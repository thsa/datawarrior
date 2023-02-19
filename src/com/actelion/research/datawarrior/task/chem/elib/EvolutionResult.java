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

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.Mutation;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.coords.CoordinateInventor;
import com.actelion.research.util.DoubleFormat;

import java.util.ArrayList;

public class EvolutionResult implements Comparable<EvolutionResult> {
	private StereoMolecule mMol;
	private String mIDCode;
	private ArrayList<Mutation> mMutationList;
	private int mGeneration,mParentGeneration,mID,mParentID,mChildIndex,mRun,mCycle;
	private float[] mProperty,mFitness;
	private String[][] mCustomColumnValue;
	private float mOverallFitness;
	private boolean mCoordinatesValid;
	private FitnessOption[] mFitnessOptionList;
	private String[][] mResult;

	/**
	 * Creates a new EvolutionStep and calculates its fitness.
	 * One of mol and idcode may be null.
	 * @param mol
	 * @param idcode
	 */
	public EvolutionResult(StereoMolecule mol, String idcode, EvolutionResult parent,
						   FitnessOption[] fitnessOptionList, int id, int run, int cycle) {
		mMol = mol;
		mIDCode = idcode;
		mID = id;
		mRun = run;
		mCycle = cycle;
		mFitnessOptionList = fitnessOptionList;
		if (parent != null) {
			mParentID = parent.mID;
			mParentGeneration = parent.mGeneration;
			mGeneration = parent.mGeneration+1;
			}
		else {
			mParentID = -1;
			mParentGeneration = -2;
			mGeneration = -1;
			}

		if (mMol == null)
			mMol = new IDCodeParser(true).getCompactMolecule(idcode);
		else if (mIDCode == null)
			mIDCode = new Canonizer(mMol).getIDCode();

		calculateIndividualFitness();
		summarizeFitness();
		compileResultValues();
		}

	public int getChildIndex() {
		return mChildIndex;
		}

	public float getFitness(int i) {
		return mFitness[i];
		}

	public void calculateDeferredColumnValue(int fitnessOptionIndex) {
		mFitnessOptionList[fitnessOptionIndex].calculateDeferredColumnValues(mMol, mResult[fitnessOptionIndex]);
		}

	public String getResultValue(int fitnessOptionIndex, int i) {
		return mResult[fitnessOptionIndex][i];
		}

	public int getCycle() {
		return mCycle;
		}

	public int getGeneration() {
		return mGeneration;
		}

	public int getID() {
		return mID;
		}

	public int getRun() {
		return mRun;
	}

	public String getIDCode() {
		return mIDCode;
		}

	public StereoMolecule getMolecule() {
		return mMol;
		}

	public ArrayList<Mutation> getMutationList() {
		return mMutationList;
		}

	public float getOverallFitness() {
		return mOverallFitness;
		}

	public int getParentID() {
		return mParentID;
		}

	public int getParentGeneration() {
		return mParentGeneration;
		}

	public float getProperty(int i) {
		return mProperty[i];
		}

	public void ensureCoordinates() {
		if (!mCoordinatesValid) {
			mCoordinatesValid = true;
			new CoordinateInventor().invent(mMol);
			}
		}

	public void setChildIndex(int i) {
		mChildIndex = i;
		}

	public void setMutationList(ArrayList<Mutation> ml) {
		mMutationList = ml;
		}

	private void calculateIndividualFitness() {
		mProperty = new float[mFitnessOptionList.length];
		mFitness = new float[mFitnessOptionList.length];
		mCustomColumnValue = new String[mFitnessOptionList.length][];

		int index = 0;
		String[][] columnValueHolder = new String[1][0];
		for (FitnessOption fo:mFitnessOptionList) {
			mProperty[index] = fo.calculateProperty(mMol, columnValueHolder);
			mCustomColumnValue[index] = columnValueHolder[0];
			if (columnValueHolder[0] != null)
				columnValueHolder[0] = new String[0];
			mFitness[index] = fo.evaluateFitness(mProperty[index]);
			index++;
			}
		}

	private void summarizeFitness() {
		mOverallFitness = 1.0f;
		float weightSum = 0.0f;
		int index = 0;
		for (FitnessOption fo:mFitnessOptionList) {

			mOverallFitness *= Math.pow(mFitness[index++], fo.getWeight());
			weightSum += fo.getWeight();
			}
		mOverallFitness = (float)Math.pow(mOverallFitness, 1.0f / weightSum);
		}

	private void compileResultValues() {
		mResult = new String[mFitnessOptionList.length][];
		for (int foi=0; foi<mFitnessOptionList.length; foi++) {
			mResult[foi] = new String[mFitnessOptionList[foi].getResultColumnCount()];
			int index = 0;
			mResult[foi][index++] = DoubleFormat.toString(mProperty[foi]);
			if (mFitnessOptionList[foi].hasFitnessColumn())
				mResult[foi][index++] = DoubleFormat.toString(mFitness[foi]);
			for (String columnValue:mCustomColumnValue[foi])
				mResult[foi][index++] = columnValue;
			}
		}

	/**
	 * Considers EvolutionResults with lower fitness as larger
	 * that TreeSets contain high fitness results first.
	 * As first priority put the latest run results at the top.
	 */
	public int compareTo(EvolutionResult o) {
		return (mRun < o.mRun) ? 1
			 : (mRun > o.mRun) ? -1
			 : (mOverallFitness < o.mOverallFitness) ? 1
			 : (mOverallFitness > o.mOverallFitness) ? -1
			 : mIDCode.compareTo(o.mIDCode);
		}
	}
