package com.actelion.research.datawarrior.task.chem.elib;

import com.actelion.research.chem.StereoMolecule;

public class MoleculeWithDescriptor {
	StereoMolecule mMol;
	Object mDescriptor;

	protected MoleculeWithDescriptor(StereoMolecule mol, Object descriptor) {
		mMol = mol;
		mDescriptor = descriptor;
		}
	}
