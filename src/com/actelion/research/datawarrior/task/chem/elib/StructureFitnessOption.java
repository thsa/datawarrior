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
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorHandler;
import com.actelion.research.chem.descriptor.DescriptorHandlerFlexophore;
import com.actelion.research.datawarrior.task.AbstractTask;
import com.actelion.research.table.model.CompoundTableModel;
import org.openmolecules.chem.conf.gen.RigidFragmentCache;

import java.util.concurrent.ConcurrentHashMap;

public class StructureFitnessOption extends FitnessOption {
	private volatile boolean mIsFexophore;
	private int mSearchType;
	private String mDescriptorShortName;
	private MoleculeWithDescriptor[] mRefMoleculeList;
	private static ConcurrentHashMap<String,DescriptorHandlerFlexophore> sDescriptorHandlerMap;

	public StructureFitnessOption(String params, ProgressListener pl) {
		String[] param = params.split("\\t");
		if (param.length >= 3) {
			mSearchType = AbstractTask.findListIndex(param[0], StructureFitnessPanel.SEARCH_TYPE_CODE, 0);
			mDescriptorShortName = param[1];
			mSliderValue = Integer.parseInt(param[2]);
			mRefMoleculeList = new MoleculeWithDescriptor[param.length-3];
			@SuppressWarnings("unchecked")
			DescriptorHandler<Object,StereoMolecule> dh = CompoundTableModel.getDefaultDescriptorHandler(mDescriptorShortName);
			mIsFexophore = DescriptorConstants.DESCRIPTOR_Flexophore.shortName.equals(mDescriptorShortName);
			if (mIsFexophore)
				RigidFragmentCache.getDefaultInstance().loadDefaultCache();

			for (int i=0; i<mRefMoleculeList.length; i++) {
				StereoMolecule mol = new IDCodeParser(true).getCompactMolecule(param[i+3]);

				if (mIsFexophore)
					pl.startProgress("Calculating Flexophore...", 0, 0);

				Object descriptor = dh.createDescriptor(mol);

				if (mIsFexophore)
					pl.stopProgress();

				mRefMoleculeList[i] = new MoleculeWithDescriptor(mol, descriptor);
				}
			}
		}

	public static String getParamError(String params) {
		String[] param = params.split("\\t");
		if (param.length < 3)
			return "Wrong parameter count.";

		if (param.length == 3)
			return "A structure criterion has no structures.";

		for (int i=0; i<param.length-3; i++) {
			StereoMolecule mol = new IDCodeParser(true).getCompactMolecule(param[i+3]);
			try {
				mol.validate();
				}
			catch (Exception e) {
				return "Structure validation error:"+e.getMessage();
				}	
			}

		return null;
		}

	@Override
	public String getName() {
		return mDescriptorShortName + (mSearchType == 0 ? " sim score" : " dissim score");
		}

	@Override
	public float calculateProperty(StereoMolecule mol, String[][] customColumnValueHolder) {
		boolean isSimilar = (mSearchType == 0);
		@SuppressWarnings("unchecked")
		DescriptorHandler<Object,StereoMolecule> dh;
		if (mIsFexophore) {
			synchronized (this) {
				if (sDescriptorHandlerMap == null)
					sDescriptorHandlerMap = new ConcurrentHashMap<>();

				dh = sDescriptorHandlerMap.get(Thread.currentThread().getName());
				if (dh == null) {
					dh = CompoundTableModel.getDefaultDescriptorHandler(mDescriptorShortName).getThreadSafeCopy();
					sDescriptorHandlerMap.put(Thread.currentThread().getName(), (DescriptorHandlerFlexophore)dh);
					}
				}
			}
		else {
			dh = CompoundTableModel.getDefaultDescriptorHandler(mDescriptorShortName);
			}

		Object descriptor = dh.createDescriptor(mol);
		if (descriptor == null)
			return 0.0f;

		float property = 0.0f;
		for (MoleculeWithDescriptor mwd:mRefMoleculeList) {
			float similarity = dh.getSimilarity(descriptor, mwd.mDescriptor);
			property = Math.max(property, similarity);
			}

		// in case of dissimilarity, we consider 50% similarity already as completely dissimilar to avoid absurd structure modifications
		return isSimilar ? property : 2.0f * (1.0f - Math.max(property, 0.5f));
		}

	@Override
	public float evaluateFitness(float propertyValue) {
		boolean isSimilar = (mSearchType == 0);

		// consider sim**2 to value those changes higher where we already have high similarities
		// return isSimilar ? propertyValue*propertyValue : 2.0*propertyValue - propertyValue*propertyValue;

		if (isSimilar)
			return propertyValue;
		else
			// consider similarities lower than 50% as fully optimal
			return (propertyValue >= 0.5) ? 1f : propertyValue * 2f;
		}
	}
