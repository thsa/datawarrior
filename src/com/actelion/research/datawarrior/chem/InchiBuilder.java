package com.actelion.research.datawarrior.chem;

import com.actelion.research.chem.IsomericSmilesCreator;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.datawarrior.task.chem.SmilesToInchi;
import com.github.dan2097.jnainchi.*;

import java.io.IOException;

/**
 * InchiBuilder creates an InChi using the JNA-InChi project, which is a JNA based Java wrapper
 * for the InChi 1.0.6 code from inchi-trust.org. This class is meant to replace the older
 * InchiCreator, which based on the JNI-InChi projecdt using JNI to wrap the InChi 1.0.3 code
 * from 2010.
 */
public class InchiBuilder {
	private static final boolean USE_SMILES_TO_INCHI = true;

	public static String createStandardInchi(StereoMolecule mol) {
		if (mol.isFragment())	// no support for substructures
			return null;

		if (USE_SMILES_TO_INCHI) {
			try {
				String smiles = IsomericSmilesCreator.createSmiles(mol);
				InchiOutput io = SmilesToInchi.toInchi(smiles);
				return io.getStatus() != InchiStatus.ERROR ? io.getInchi() : null;
				}
			catch (IOException ioe) {
				return null;
				}
			}

		mol.ensureHelperArrays(Molecule.cHelperParities);

		synchronized(InchiBuilder.class) {
			try {
				InchiInput input = createInchiInput(mol);
				InchiOutput output = JnaInchi.toInchi(input);
				String inchi = output.getInchi();
				if (inchi == null || inchi.length() == 0) {
					inchi = output.getMessage();
					System.out.println(output.getLog());
					}
				return inchi;
				}
			catch (Exception e) {
				e.printStackTrace();
				return null;
				}
			}
		}

	public static String createInchiKey(StereoMolecule mol) {
		if (mol.isFragment())	// no support for substructures
			return null;

		mol.ensureHelperArrays(Molecule.cHelperParities);

		synchronized(InchiCreator.class) {
			try {
				InchiInput input = createInchiInput(mol);
				InchiOutput output = JnaInchi.toInchi(input);
				String inchi = output.getInchi();
				if (inchi == null || inchi.length() == 0) {
					System.out.println(output.getLog());
					return output.getMessage();
					}
				InchiKeyOutput keyOutput = JnaInchi.inchiToInchiKey(inchi);
				return (keyOutput.getStatus() == InchiKeyStatus.OK) ? keyOutput.getInchiKey() : null;
				}
			catch (Exception e) {
				e.printStackTrace();
				return null;
				}
			}
		}

	private static InchiInput createInchiInput(StereoMolecule mol) {
		InchiInput input = new InchiInput();

		InchiAtom[] inchiAtom = new InchiAtom[mol.getAtoms()];
		for (int atom=0; atom<mol.getAtoms(); atom++) {
			inchiAtom[atom] = new InchiAtom(mol.getAtomLabel(atom));
			input.addAtom(inchiAtom[atom]);
			}

		for (int bond=0; bond<mol.getBonds(); bond++) {
			int atom1 = mol.getBondAtom(0, bond);
			int atom2 = mol.getBondAtom(1, bond);
			int order = mol.getBondOrder(bond);
			InchiBondType type = (order == 1) ? InchiBondType.SINGLE
					: (order == 2) ? InchiBondType.DOUBLE
					: (order == 3) ? InchiBondType.TRIPLE : InchiBondType.NONE;
			InchiBond b = new InchiBond(input.getAtom(atom1), input.getAtom(atom2), type);
			input.addBond(b);
			}

		// we need to add explicit hydrogens wherever we have a stereo feature
		InchiAtom[] stereoHydrogen = new InchiAtom[mol.getAtoms()];
		for (int atom=0; atom<mol.getAtoms(); atom++) {
/*			if (mol.isAtomStereoCenter(atom) && mol.getConnAtoms(atom) == 3) {
				stereoHydrogen[atom] = new InchiAtom("H");
				input.addAtom(stereoHydrogen[atom]);
				input.addBond(new InchiBond(inchiAtom[atom], stereoHydrogen[atom], InchiBondType.SINGLE));
				}*/
			}
/*			for (int bond=0; bond<mol.getBonds(); bond++) {
				if (mol.getBondParity(bond) != Molecule.cBondParityNone) {
					for (int i=0; i<2; i++) {
						int atom = mol.getBondAtom(i, bond);
						if (mol.getConnAtoms(atom) == 2) {
							stereoHydrogen[atom] = new JniInchiAtom("H");
							input.addAtom(stereoHydrogen[atom]);
							input.addBond(new InchiBond(inchiAtom[atom], stereoHydrogen[atom], InchiBondType.SINGLE));
						}
					}
				}
			}
*/
		for (int atom=0; atom<mol.getAtoms(); atom++) {
			int implicitHydrogen = mol.getImplicitHydrogens(atom) - (stereoHydrogen[atom] == null ? 0 : 1);
			if (implicitHydrogen > 0)
				inchiAtom[atom].setImplicitHydrogen(implicitHydrogen);

			int charge = mol.getAtomCharge(atom);
			if (charge != 0)
				inchiAtom[atom].setCharge(charge);

			int radical = mol.getAtomRadical(atom);
			if (radical != 0)
				inchiAtom[atom].setRadical(radical == Molecule.cAtomRadicalStateS ? InchiRadical.SINGLET
						: radical == Molecule.cAtomRadicalStateD ? InchiRadical.DOUBLET
						: radical == Molecule.cAtomRadicalStateT ? InchiRadical.TRIPLET
						: InchiRadical.NONE);

			int mass = mol.getAtomMass(atom);
			if (mass != 0)
				inchiAtom[atom].setIsotopicMass(mass);
		}

		for (int atom=0; atom<mol.getAtoms(); atom++) {
			int connCount = mol.getConnAtoms(atom);
			if (mol.isAtomStereoCenter(atom) && connCount >= 3 && connCount <= 4) {
				int parity = mol.getAtomParity(atom);
				if (parity == Molecule.cAtomParity1 || parity == Molecule.cAtomParity2) {
					int[] c = new int[4];
					for (int i=0; i<connCount; i++)
						c[i] = mol.getConnAtom(atom, i);
					if (connCount == 3) {
						c[3] = atom;
				//		Arrays.sort(c);
						}
					InchiAtom[] a = new InchiAtom[4];
					for (int i=0; i<4; i++)
						a[i] = inchiAtom[c[i]];

//					InchiAtom[] a = new InchiAtom[4];
//					a[0] = inchiAtom[mol.getConnAtom(atom, 0)];
//					a[1] = inchiAtom[mol.getConnAtom(atom, 1)];
//					a[2] = inchiAtom[mol.getConnAtom(atom, 2)];
//					a[3] = (mol.getConnAtoms(atom) == 4) ? inchiAtom[mol.getConnAtom(atom, 3)] : stereoHydrogen[atom];

					InchiStereoParity inchiParity = (parity == Molecule.cAtomParity1) ? InchiStereoParity.EVEN
												  : (parity == Molecule.cAtomParity2) ? InchiStereoParity.ODD : InchiStereoParity.UNKNOWN;
					input.addStereo(new InchiStereo(a, inchiAtom[atom], InchiStereoType.Tetrahedral, inchiParity));
					}
				}
			}

		for (int bond=0; bond<mol.getBonds(); bond++) {
			int parity = mol.getBondParity(bond);
			if (mol.getBondParity(bond) != Molecule.cBondParityNone) {
				InchiAtom[] a = new InchiAtom[4];
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

				InchiStereoParity inchiParity = (parity == Molecule.cBondParityEor1) ? InchiStereoParity.EVEN
						: (parity == Molecule.cBondParityZor2) ? InchiStereoParity.ODD : InchiStereoParity.UNKNOWN;
				input.addStereo(new InchiStereo(a, null, InchiStereoType.DoubleBond, inchiParity));
				}
			}

		return input;
		}
	}
