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
import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.alignment3d.PheSAAlignmentOptimizer;
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorHandlerFlexophore;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.table.model.CompoundTableModel;
import org.openmolecules.chem.conf.gen.ConformerGenerator;
import org.openmolecules.chem.conf.gen.RigidFragmentCache;

import java.util.concurrent.ConcurrentHashMap;

public class ConformerFitnessOption extends FitnessOption {
	private static final int PHESA_CONFORMER_COUNT = 64;
	private String mDescriptorShortName;
	private StereoMolecule[] mRefConformer;
	private static ConcurrentHashMap<String,DescriptorHandlerFlexophore> sFlexophoreHandlerMap;

	public ConformerFitnessOption(String params, ProgressListener pl) {
		String[] param = params.split("\\t");
		if (param.length >= 3) {
			mDescriptorShortName = param[0];
			mSliderValue = Integer.parseInt(param[1]);
			mRefConformer = new StereoMolecule[(param.length-2)/2];
			for (int i = 0; i<mRefConformer.length; i++)
				mRefConformer[i] = new IDCodeParser(false).getCompactMolecule(param[i*2+2], param[i*2+3]);

			RigidFragmentCache.getDefaultInstance().loadDefaultCache();
			}
		}

	public static String getParamError(String params) {
		String[] param = params.split("\\t");
		if (param.length < 2)
			return "Wrong parameter count.";

		if (param.length == 2)
			return "Conformer criterion has no conformer.";

		for (int i=2; i<param.length-1; i+=2) {
			StereoMolecule mol = new IDCodeParser(false).getCompactMolecule(param[i], param[i+1]);
			if (mol == null || mol.getAllAtoms() == 0)
				return "No reference conformer given.";
			}

		return null;
		}

	@Override
	public String getName() {
		return mDescriptorShortName + " similarity";
		}

	@Override
	public int getResultColumnCount() {
		return super.getResultColumnCount() + 1;
	}

	@Override
	public String getResultColumnName(int i) {
		return (i < super.getResultColumnCount()) ? super.getResultColumnName(i) : "Best Conformer";
		}

	@Override
	public void setResultColumnProperties(int i, CompoundTableModel tableModel, int column) {
		if (i == super.getResultColumnCount()) {
			tableModel.setColumnProperty(column, CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnType3DCoordinates);
			tableModel.setColumnProperty(column, CompoundTableConstants.cColumnPropertyParentColumn, tableModel.getColumnTitleNoAlias(0));
			if (mRefConformer.length == 1) {
				Canonizer canonizer = new Canonizer(mRefConformer[0]);
				String refIDCodeAndCoords = canonizer.getIDCode() + " " + canonizer.getEncodedCoordinates(true);
				tableModel.setColumnProperty(column, CompoundTableConstants.cColumnPropertySuperposeMolecule, refIDCodeAndCoords);
				}
			}
		}

	@Override
	public float calculateProperty(StereoMolecule mol, String[][] customColumnValueHolder) {
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);


		if (DescriptorConstants.DESCRIPTOR_Flexophore.shortName.equals(mDescriptorShortName)) {
			DescriptorHandlerFlexophore dh;

			synchronized (this) {
				if (sFlexophoreHandlerMap == null)
					sFlexophoreHandlerMap = new ConcurrentHashMap<>();

				dh = sFlexophoreHandlerMap.get(Thread.currentThread().getName());
				if (dh == null) {
					dh = (DescriptorHandlerFlexophore)CompoundTableModel.getDefaultDescriptorHandler(mDescriptorShortName).getThreadSafeCopy();
					sFlexophoreHandlerMap.put(Thread.currentThread().getName(), dh);
					}
				}

			float bestFit = 0.0f;
			int bestMol = -1;
			for (int i=0; i<mRefConformer.length; i++) {
				StereoMolecule refMol = mRefConformer[i];
				Object d1 = dh.createDescriptorSingleConf(refMol);
				Object d2 = dh.createDescriptor(mol);
				float similarity = dh.getSimilarity(d2, d1);
				if (bestFit < similarity) {
					bestFit = similarity;
					bestMol = i;
					}
				}

			// because we don't have the best conformer, we create it later for all results that show up in the table
			customColumnValueHolder[0] = new String[1];
			customColumnValueHolder[0][0] = Integer.toString(bestMol);

			return bestFit;
			}

		if (DescriptorConstants.DESCRIPTOR_ShapeAlign.shortName.equals(mDescriptorShortName)) {
			int implicitHydrogens = 0;
			for (int atom=0; atom<mol.getAtoms(); atom++)
				implicitHydrogens += mol.getImplicitHydrogens(atom);

			StereoMolecule conformer = new StereoMolecule(mol.getAllAtoms()+implicitHydrogens, mol.getAllBonds()+implicitHydrogens);
			mol.copyMolecule(conformer);

			Conformer bestConformer = null;
			double bestFit = 0.0f;
			for (StereoMolecule refMol:mRefConformer) {
				ConformerGenerator generator = new ConformerGenerator(true);
				generator.initializeConformers(conformer);

				for (int i = 0; i<PHESA_CONFORMER_COUNT; i++) {
					if (generator.getNextConformerAsMolecule(conformer) == null)
						break;

					double fit = PheSAAlignmentOptimizer.alignTwoMolsInPlace(refMol, conformer, 0.5);
					if (bestFit < fit) {
						bestFit = fit;
						bestConformer = new Conformer(conformer);
						}
					}
				}

			if (bestConformer != null) {
				bestConformer.toMolecule(conformer);
				Canonizer canonizer = new Canonizer(conformer);
				customColumnValueHolder[0] = new String[1];
				customColumnValueHolder[0][0] = canonizer.getEncodedCoordinates(true);
				}

			return (float)bestFit;
			}

		return 0;
		}

	@Override
	public float evaluateFitness(float propertyValue) {
		// consider sim**2 to value those changes higher where we already have high similarities
		// return isSimilar ? propertyValue*propertyValue : 2.0*propertyValue - propertyValue*propertyValue;
		return propertyValue;
		}

	@Override
	public boolean hasDeferredColumnValues() {
		return DescriptorConstants.DESCRIPTOR_Flexophore.shortName.equals(mDescriptorShortName);
		}

	@Override
	public void calculateDeferredColumnValues(StereoMolecule mol, String[] value) {
		int index = super.getResultColumnCount();
		int refMolIndex = Integer.parseInt(value[index]);
		StereoMolecule refMol = mRefConformer[refMolIndex];

		int implicitHydrogens = 0;
		for (int atom=0; atom<mol.getAtoms(); atom++)
			implicitHydrogens += mol.getImplicitHydrogens(atom);

		StereoMolecule conformer = new StereoMolecule(mol.getAllAtoms()+implicitHydrogens, mol.getAllBonds()+implicitHydrogens);
		mol.copyMolecule(conformer);

		ConformerGenerator generator = new ConformerGenerator(true);
		generator.initializeConformers(conformer);

		double bestFit = 0.0;
		Conformer bestConformer = null;
		for (int i = 0; i<PHESA_CONFORMER_COUNT; i++) {
			if (generator.getNextConformerAsMolecule(conformer) == null)
				break;

			double fit = PheSAAlignmentOptimizer.alignTwoMolsInPlace(refMol, conformer, 0.5);
			if (bestFit < fit) {
				bestFit = fit;
				bestConformer = new Conformer(conformer);
				}
			}

		if (bestConformer != null) {
			bestConformer.toMolecule(conformer);
			Canonizer canonizer = new Canonizer(conformer);
			value[index] = canonizer.getEncodedCoordinates(true);
			}
		}
	}
