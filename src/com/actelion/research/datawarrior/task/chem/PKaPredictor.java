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

package com.actelion.research.datawarrior.task.chem;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.marvin.calculations.pKaPlugin;
import chemaxon.marvin.plugin.PluginException;

import com.actelion.research.chem.MolfileCreator;
import com.actelion.research.chem.StereoMolecule;

import java.util.Arrays;

public class PKaPredictor {
	private pKaPlugin plugin;

	public PKaPredictor() {
		plugin = new pKaPlugin();

		// set parameters
		plugin.setMaxIons(6);
		plugin.setBasicpKaLowerLimit(0.0);
		plugin.setAcidicpKaUpperLimit(14.0);
		plugin.setpHLower(3.0); // for ms distr
		plugin.setpHUpper(6.0); // for ms distr
		plugin.setpHStep(1.0);  // for ms distr
	}

	/**
	 * Protonates basic atoms and deprotonates acidc atoms according given
	 * @param mol
	 * @param basicpKa
	 * @param acidicpKa
	 * @param basicIndexes
	 * @param acidicIndexes
	 */
	public void getProtonationStates(StereoMolecule mol, double[] basicpKa, double[] acidicpKa,
									 int[] basicIndexes, int[] acidicIndexes) {
		try {
			plugin.setMolecule(convert(mol));
			plugin.run();
			plugin.getMacropKaValues(pKaPlugin.BASIC, basicpKa, basicIndexes);
			plugin.getMacropKaValues(pKaPlugin.ACIDIC, acidicpKa, acidicIndexes);
		}
		catch (PluginException pe) {
			Arrays.fill(basicIndexes, -1);
			Arrays.fill(acidicIndexes, -1);
			System.out.println("PluginException:"+pe);
		}
		catch (Throwable e) {
			Arrays.fill(basicIndexes, -1);
			Arrays.fill(acidicIndexes, -1);
			System.out.println("Unexpected ChemAxon Exception:"+e);
		}
	}

	public double[] getMostBasicPKas(chemaxon.struc.Molecule mol) {
		double[] basicpKa = new double[3];
		int[] basicIndexes = new int[3];

		try {
			plugin.setMolecule(mol);
			plugin.run();
			plugin.getMacropKaValues(pKaPlugin.BASIC, basicpKa, basicIndexes);
		}
		catch (PluginException pe) {
			Arrays.fill(basicpKa, Double.NaN);
			System.out.println("PluginException:"+pe);
		}
		catch (Throwable e) {
			Arrays.fill(basicpKa, Double.NaN);
			System.out.println("Unexpected ChemAxon Exception:"+e);
		}

		return basicpKa;
	}

	public double[] getMostAcidicPKas(chemaxon.struc.Molecule mol) {
		double[] acidicpKa = new double[3];
		int[] acidicIndexes = new int[3];
		try {
			plugin.setMolecule(mol);
			plugin.run();
			plugin.getMacropKaValues(pKaPlugin.ACIDIC, acidicpKa, acidicIndexes);
		}
		catch (PluginException pe) {
			Arrays.fill(acidicpKa, Double.NaN);
			System.out.println("PluginException:"+pe);
		}
		catch (Throwable e) {
			Arrays.fill(acidicpKa, Double.NaN);
			System.out.println("Unexpected ChemAxon Exception:"+e);
		}

		return acidicpKa;
	}

	public chemaxon.struc.Molecule convert(StereoMolecule actelionMol) {
		String molfile;
		try {
			molfile = new MolfileCreator(actelionMol).getMolfile();
			return MolImporter.importMol(molfile, "mol");
		}
		catch (MolFormatException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}
}

