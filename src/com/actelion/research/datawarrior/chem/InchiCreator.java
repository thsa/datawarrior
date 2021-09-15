package com.actelion.research.datawarrior.chem;

public class InchiCreator {/*
	public static String createStandardInchi(StereoMolecule mol) {
		if (mol.isFragment())	// no support for substructures
			return null;

		mol.ensureHelperArrays(Molecule.cHelperParities);
		JniInchiInput input = createInchiInput(mol);

		synchronized(InchiCreator.class) {
			try {
				JniInchiOutput output = JniInchiWrapper.getStdInchi(input);
				String inchi = output.getInchi();
				if (inchi == null || inchi.length() == 0) {
					inchi = output.getMessage();
					System.out.println(output.getLog());
					}
				return inchi;
			}
			catch (JniInchiException e) {
				e.printStackTrace();
			}
		}

	return null;
	}

	public static String createInchiKey(StereoMolecule mol) {
		if (mol.isFragment())	// no support for substructures
			return null;

		mol.ensureHelperArrays(Molecule.cHelperParities);
		JniInchiInput input = createInchiInput(mol);

		synchronized(InchiCreator.class) {
			try {
				JniInchiOutput output = JniInchiWrapper.getStdInchi(input);
				String inchi = output.getInchi();
				if (inchi == null || inchi.length() == 0) {
					System.out.println(output.getLog());
					return output.getMessage();
				}
				return JniInchiWrapper.getInchiKey(inchi).getKey();
			}
			catch (JniInchiException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	private static JniInchiInput createInchiInput(StereoMolecule mol) {
		JniInchiInput input = new JniInchiInput();

		JniInchiAtom[] inchiAtom = new JniInchiAtom[mol.getAtoms()];
		for (int atom=0; atom<mol.getAtoms(); atom++) {
			inchiAtom[atom] = input.addAtom(new JniInchiAtom(0.0, 0.0, 0.0, Molecule.cAtomLabel[mol.getAtomicNo(atom)]));
		}

		for (int bond=0; bond<mol.getBonds(); bond++) {
			int type = mol.getBondTypeSimple(bond);
			int order = mol.getBondOrder(bond);
			input.addBond(new JniInchiBond(inchiAtom[mol.getBondAtom(0, bond)],
					inchiAtom[mol.getBondAtom(1, bond)],
					type == Molecule.cBondTypeMetalLigand ? INCHI_BOND_TYPE.NONE		// TODO ligand field bond???
							: type == Molecule.cBondTypeDouble ? INCHI_BOND_TYPE.DOUBLE
							: type == Molecule.cBondTypeTriple ? INCHI_BOND_TYPE.TRIPLE : INCHI_BOND_TYPE.SINGLE));
		}

		// we need to add explicit hydrogens wherever we have a stereo feature
		JniInchiAtom[] stereoHydrogen = new JniInchiAtom[mol.getAtoms()];
		for (int atom=0; atom<mol.getAtoms(); atom++) {
			if (mol.isAtomStereoCenter(atom) && mol.getConnAtoms(atom) == 3) {
				stereoHydrogen[atom] = input.addAtom(new JniInchiAtom(0.0, 0.0, 0.0, "H"));
				input.addBond(new JniInchiBond(inchiAtom[atom], stereoHydrogen[atom], INCHI_BOND_TYPE.SINGLE));
			}
		}
//			for (int bond=0; bond<mol.getBonds(); bond++) {
//				if (mol.getBondParity(bond) != Molecule.cBondParityNone) {
//					for (int i=0; i<2; i++) {
//						int atom = mol.getBondAtom(i, bond);
//						if (mol.getConnAtoms(atom) == 2) {
//							stereoHydrogen[atom] = input.addAtom(new JniInchiAtom(0.0, 0.0, 0.0, "H"));
//							input.addBond(new JniInchiBond(inchiAtom[atom], stereoHydrogen[atom], INCHI_BOND_TYPE.SINGLE));
//						}
//					}
//				}
//			}

		for (int atom=0; atom<mol.getAtoms(); atom++) {
			int implicitHydrogen = mol.getImplicitHydrogens(atom) - (stereoHydrogen[atom] == null ? 0 : 1);
			if (implicitHydrogen > 0)
				inchiAtom[atom].setImplicitH(implicitHydrogen);

			int charge = mol.getAtomCharge(atom);
			if (charge != 0)
				inchiAtom[atom].setCharge(charge);

			int radical = mol.getAtomRadical(atom);
			if (radical != 0)
				inchiAtom[atom].setRadical(radical == Molecule.cAtomRadicalStateS ? INCHI_RADICAL.SINGLET
						: radical == Molecule.cAtomRadicalStateD ? INCHI_RADICAL.DOUBLET
						: radical == Molecule.cAtomRadicalStateT ? INCHI_RADICAL.TRIPLET
						: INCHI_RADICAL.NONE);

			int mass = mol.getAtomMass(atom);
			if (mass != 0)
				inchiAtom[atom].setIsotopicMass(mass);
		}

		JniInchiAtom[] a = new JniInchiAtom[4];
		for (int atom=0; atom<mol.getAtoms(); atom++) {
			int connCount = mol.getConnAtoms(atom);
			if (mol.isAtomStereoCenter(atom) && connCount >= 3 && connCount <= 4) {
				a[0] = inchiAtom[mol.getConnAtom(atom, 0)];
				a[1] = inchiAtom[mol.getConnAtom(atom, 1)];
				a[2] = inchiAtom[mol.getConnAtom(atom, 2)];
				a[3] = (mol.getConnAtoms(atom) == 4) ? inchiAtom[mol.getConnAtom(atom, 3)] : stereoHydrogen[atom];
				input.addStereo0D(JniInchiStereo0D.createNewTetrahedralStereo0D(inchiAtom[atom], a[0], a[1], a[2], a[3],
						mol.getAtomParity(atom) == Molecule.cAtomParity1 ? INCHI_PARITY.EVEN
								: mol.getAtomParity(atom) == Molecule.cAtomParity2 ? INCHI_PARITY.ODD : INCHI_PARITY.UNKNOWN));
			}
		}

		for (int bond=0; bond<mol.getBonds(); bond++) {
			if (mol.getBondParity(bond) != Molecule.cBondParityNone) {
				for (int i=0; i<2; i++) {
					int atom1 = mol.getBondAtom(i, bond);
					int atom2 = mol.getBondAtom(1-i, bond);
					for (int j=0; j<mol.getConnAtoms(atom1); j++) {
						int conn = mol.getConnAtom(atom1, j);
						if (conn != atom2) {
							a[3*i] = inchiAtom[conn];
							break;
						}
						a[1+i] = inchiAtom[atom1];
					}
				}

				input.addStereo0D(JniInchiStereo0D.createNewDoublebondStereo0D(a[0], a[1], a[2], a[3],
						mol.getBondParity(bond) == Molecule.cBondParityEor1 ? INCHI_PARITY.EVEN
								: mol.getBondParity(bond) == Molecule.cBondParityZor2 ? INCHI_PARITY.ODD : INCHI_PARITY.UNKNOWN));	// check assignment
			}
		}

		return input;
	}*/
}
