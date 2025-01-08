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
import com.actelion.research.chem.conf.AtomAssembler;
import com.actelion.research.datawarrior.fx.EditableSmallMolMenuController;
import com.actelion.research.datawarrior.fx.EditableLargeMolMenuController;
import com.actelion.research.datawarrior.fx.JFXMolViewerPanel;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import org.openmolecules.datawarrior.plugin.IConformerPanel;
import org.openmolecules.fx.viewer3d.V3DMolecule;
import org.openmolecules.fx.viewer3d.V3DScene;

import java.awt.*;
import java.util.EnumSet;
import java.util.List;

public class PluginConformerPanel extends JFXMolViewerPanel implements IConformerPanel {
	public PluginConformerPanel(Frame owner, int mode) {
		super(false, buildSettings());
		adaptToLookAndFeelChanges();
//		setBackground(new java.awt.Color(24, 24, 96));
		if (mode == MODE_LIGAND_AND_PROTEIN) {
			setPreferredSize(new Dimension(HiDPIHelper.scale(320), HiDPIHelper.scale(240)));
			setPopupMenuController(new EditableLargeMolMenuController(this));
		}
		else {
			setPreferredSize(new Dimension(HiDPIHelper.scale(240), HiDPIHelper.scale(180)));
			setPopupMenuController(new EditableSmallMolMenuController(owner, this));
		}
	}

	private static EnumSet<V3DScene.ViewerSettings> buildSettings() {
		EnumSet<V3DScene.ViewerSettings> settings = V3DScene.CONFORMER_VIEW_MODE;
		settings.add(V3DScene.ViewerSettings.EDITING);
		return settings;
	}

	@Override public String getStructure(int role, int format) {
		V3DMolecule.MoleculeRole r = (role == ROLE_PROTEIN) ? V3DMolecule.MoleculeRole.MACROMOLECULE
								   : (role == ROLE_LIGAND) ? V3DMolecule.MoleculeRole.LIGAND : null;
		StereoMolecule mol = getMergedMolecule(r);
		if (mol == null)
			return null;

		if (format == FORMAT_IDCODE) {
			Canonizer pc = new Canonizer(mol);
			return pc.getIDCode() + " " + pc.getEncodedCoordinates();
		}

		return (format == FORMAT_MOLFILE_V2) ? new MolfileCreator(mol).getMolfile() : new MolfileV3Creator(mol).getMolfile();
	}

	private StereoMolecule getMergedMolecule(V3DMolecule.MoleculeRole role) {
		List<StereoMolecule> proteinList = getMolecules(role);
		StereoMolecule protein = null;
		for (StereoMolecule p:proteinList) {
			if (protein == null)
				protein = p;
			else
				protein.addMolecule(p);
		}
		return protein;
	}

	@Override public void setConformerFromIDCode(String idcode) {
		clear();
		StereoMolecule mol = getStructureFromIDCode(idcode);
		if (mol != null) {
			new AtomAssembler(mol).addImplicitHydrogens();
			addMolecule(mol, null, null);
			optimizeView();
		}
	}

	@Override public void setConformerFromMolfile(String molfile) {
		clear();
		StereoMolecule mol = (molfile == null) ? null : new MolfileParser().getCompactMolecule(molfile);
		if (mol != null) {
			new AtomAssembler(mol).addImplicitHydrogens();
			addMolecule(mol, null, null);
			optimizeView();
		}
	}

	@Override public void setProteinCavity(String proteinIDCode, String ligandIDCode) {
		clear();
		StereoMolecule cavity = getStructureFromIDCode(proteinIDCode);
		StereoMolecule ligand = getStructureFromIDCode(ligandIDCode);
		if (cavity != null)
			setProteinCavity(cavity, ligand, true, false);
		if (ligand != null)
			setOverlayMolecule(ligand);
		if (cavity != null || ligand != null)
			optimizeView();
	}

	private StereoMolecule getStructureFromIDCode(String idcode) {
		if (idcode == null)
			return null;

		String coords = null;
		int index = idcode.indexOf(" ");
		if (index != -1) {
			coords = idcode.substring(index+1);
			idcode = idcode.substring(0, index);
		}

		return new IDCodeParser(false).getCompactMolecule(idcode, coords);
	}
}
