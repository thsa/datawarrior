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

package com.actelion.research.datawarrior.plugin;

import com.actelion.research.chem.*;
import com.actelion.research.gui.JEditableStructureView;
import org.openmolecules.datawarrior.plugin.IChemistryPanel;

public class PluginChemistryPanel extends JEditableStructureView implements IChemistryPanel {
	public PluginChemistryPanel() {
		super(new StereoMolecule());
		}

	@Override public void setMode(int mode) {
		getMolecule().setFragment(mode != IChemistryPanel.MODE_MOLECULE);
		setAllowQueryFeatures(mode != MODE_FRAGMENT_WITHOUT_QUERY_FEATURES);
		structureChanged();
		}

	@Override public boolean isEmptyMolecule() {
		return getMolecule().getAllAtoms() == 0;
	}

	@Override public String getMoleculeAsIDCode() {
		return new Canonizer(getMolecule()).getIDCode();
		}

	@Override public String getMoleculeAsMolfileV2() {
		return new MolfileCreator(getMolecule()).getMolfile();
		}

	@Override public String getMoleculeAsMolfileV3() {
		return new MolfileV3Creator(getMolecule()).getMolfile();
		}

	@Override public String getMoleculeAsSmiles() {
		return new IsomericSmilesCreator(getMolecule()).getSmiles();
		}

	@Override public void setMoleculeFromIDCode(String idcode) {
		int index = idcode.indexOf(" ");
		if (index == -1)
			setIDCode(idcode);
		else
			setIDCode(idcode.substring(0, index), idcode.substring(index+1));
	}

	@Override public void setMoleculeFromMolfile(String molfile) {
		new MolfileParser().parse(getMolecule(), molfile);
		structureChanged();
	}

	@Override public void setMoleculeFromSmiles(String smiles) {
		try {
			new SmilesParser().parse(getMolecule(), smiles);
			structureChanged();
		} catch (Exception e) {}
	}
}
