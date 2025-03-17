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

package org.openmolecules.datawarrior.plugin;

/**
 * This interface provides access to a JPanel that displays a chemical (sub-)structure,
 * which may be edited to define a chemical search (sub-structure, similarity, exact, ...).
 * The panel is meant to be used within the plugin dialog.
 * The editor's behaviour can be toggled between molecule and fragment logic.
 * In molecule mode open valences are understood as being filled with implicit hydrogens,
 * which are not shown on carbon atoms, but shown on hetero atoms.
 * In fragment mode the drawn chemical moiety is considered to be a sub-structure fragment
 * with unoccupied valences understood to be open and with the ability to defined atom-
 * and bond-specific query features.<br>
 * An IChemistryPanel is garanteed to be derived from JPanel and, thus, can be embedded into
 * any Swing based containers.
 */
public interface IConformerPanel {
	public static final int MODE_CONFORMER = 0;
	public static final int MODE_LIGAND_AND_PROTEIN = 1;

	public static final int FORMAT_IDCODE = 0;
	public static final int FORMAT_MOLFILE_V2 = 1;
	public static final int FORMAT_MOLFILE_V3 = 2;

	public static final int ROLE_CONFORMER = 0;
	public static final int ROLE_LIGAND = 1;
	public static final int ROLE_PROTEIN = 2;

	/**
	 * @return drawn molecule/fragment as canonical idcode or as molfile V2 or V3.
	 */
	public String getStructure(int role, int format);

	/**
	 * @return set the chemistry panel's molecule to the molecule defined by the given idcode.
	 */
	public void setConformerFromIDCode(String idcode);

	/**
	 * @return set the chemistry panel's molecule to the molecule defined by the given molfile.
	 */
	public void setConformerFromMolfile(String molfile);

	public void setProteinCavity(String protein, String ligand);
}
