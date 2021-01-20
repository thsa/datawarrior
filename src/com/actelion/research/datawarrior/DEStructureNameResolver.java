package com.actelion.research.datawarrior;

import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.SmilesParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.coords.CoordinateInventor;
import com.actelion.research.chem.name.IStructureNameResolver;
import org.openmolecules.n2s.N2SCommunicator;
import uk.ac.cam.ch.wwmm.opsin.NameToStructure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Created by thomas on 7/13/17.
 */
public class DEStructureNameResolver implements IStructureNameResolver {
	private N2SCommunicator sCommunicator;
	private TreeMap<String,String> sNameMap;

	public StereoMolecule resolveLocal(String name) {
		if (name == null)
			return null;

		name = name.trim();

		// Try local OPSIN first
		try {
			String smiles = NameToStructure.getInstance().parseToSmiles(name);
			if (smiles != null) {
				StereoMolecule mol = new StereoMolecule();
				new SmilesParser().parse(mol, smiles);
				if (mol.getAllAtoms() != 0) {
					mol.setFragment(false);
					new CoordinateInventor().invent(mol);
					return mol;
				}
			}
		}
		catch (Exception e) {}

		return null;
	}

	@Override
	public StereoMolecule resolveRemote(String name) {
		if (name == null)
			return null;

		name = name.trim();

		if (sCommunicator == null)
			sCommunicator = new N2SCommunicator(null, "datawarrior");
		String idcode = null;
		if (name != null && name.length() != 0) {
			if (sNameMap != null)
				idcode = sNameMap.get(name);

			if (idcode == null) {
				idcode = sCommunicator.getIDCode(name);

				if (sCommunicator.hasConnectionProblem())
					return null;

				if (idcode != null) {
					if (sNameMap == null)
						sNameMap = new TreeMap<>();
					sNameMap.put(name, idcode);
					}
				}

			if (idcode != null) {
				StereoMolecule mol = new IDCodeParser().getCompactMolecule(idcode);
				if (mol != null && mol.getAllAtoms() != 0)
					return mol;
				}
			}

		return null;
		}

	public String[] resolveRemote(String[] nameList) {
		if (nameList == null)
			return null;

		if (sCommunicator == null)
			sCommunicator = new N2SCommunicator(null, "datawarrior");

		ArrayList<String> unknownList = new ArrayList<>();
		StringBuilder names = new StringBuilder();
		for (String name:nameList) {
			if (name.length() != 0) {
				if (sNameMap == null || sNameMap.get(name) == null) {
					unknownList.add(name);
					names.append(name);
					names.append('\n');
					}
				}
			}

		String remoteResult = "";
		if (names.length() != 0) {
			remoteResult = sCommunicator.getIDCodeList(names.toString());

			if (sCommunicator.hasConnectionProblem())
				return null;
			}

		if (remoteResult.length() != 0) {
			BufferedReader reader = new BufferedReader(new StringReader(remoteResult));
			try {
				for (int i=0; i<unknownList.size(); i++) {
					String idcode = reader.readLine();
					if (idcode == null)
						break;
					if (idcode.length() != 0) {
						if (sNameMap == null)
							sNameMap = new TreeMap<>();
						sNameMap.put(unknownList.get(i), idcode);
						}
					}
				}
			catch (IOException ioe) {}
			}

		String[] result = new String[nameList.length];
		if (sNameMap != null) {
			for (int i=0; i<nameList.length; i++) {
				String name = nameList[i];
				if (name.length() != 0) {
					String idcode = sNameMap.get(name);
					if (idcode != null)
						result[i] = idcode;
					}
				}
			}

		return result;
		}
	}
