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
public interface IChemistryPanel {
	public static final int MODE_MOLECULE = 0;
	public static final int MODE_FRAGMENT = 1;
	public static final int MODE_FRAGMENT_WITHOUT_QUERY_FEATURES = 3;

	/**
	 * Sets the mode to either <i>molecule</i> or <i>fragment</i> mode. In molecule mode
	 * open valences are understood as being filled with implicit hydrogens, which are not
	 * shown on carbon atoms, but shown on hetero atoms.
	 * In fragment mode the drawn chemical moiety is considered to be a
	 * sub-structure fragment with unoccupied valences understood to be open and with the
	 * ability to defined atom- and bond-specific query features.
	 * @param mode MODE_MOLECULE or MODE_FRAGMENT
	 */
	public void setMode(int mode);

	/**
	 * @return whether the currently edited molecule has no atoms and bonds
	 */
	public boolean isEmptyMolecule();

	/**
	 * @return drawn molecule/fragment as canonical idcode.
	 */
	public String getMoleculeAsIDCode();

	/**
	 * @return drawn molecule/fragment as molfile version 2000. Query features are as close as the molfile format permits.
	 */
	public String getMoleculeAsMolfileV2();

	/**
	 * @return drawn molecule/fragment as molfile version 3000. Query features are as close as the molfile format permits.
	 */
	public String getMoleculeAsMolfileV3();

	/**
	 * @return drawn molecule/fragment as SMILES string not including any query features.
	 */
	public String getMoleculeAsSmiles();

	/**
	 * @return set the chemistry panel's molecule to the molecule defined by the given idcode.
	 */
	public void setMoleculeFromIDCode(String idcode);

	/**
	 * @return set the chemistry panel's molecule to the molecule defined by the given molfile.
	 */
	public void setMoleculeFromMolfile(String molfile);

	/**
	 * @return set the chemistry panel's molecule to the molecule defined by the given smiles.
	 */
	public void setMoleculeFromSmiles(String smiles);
}
