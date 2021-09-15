package com.actelion.research.datawarrior.task.chem;

/**
 * JNA-InChI - Library for calling InChI from Java
 * Copyright © 2018 Daniel Lowe
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.github.dan2097.jnainchi.*;
import com.github.dan2097.jnainchi.InchiOptions.InchiOptionsBuilder;
import uk.ac.ebi.beam.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SmilesToInchi {

	/**
	 * Convert a SMILES string to InChI using the default options
	 * (hence StdInChI will be the output)
	 * Throws an IOException if there is an issue with the SMILES string
	 * or IllegalArgumentException if given null input
	 * @param smiles
	 * @return
	 * @throws IOException
	 */
	public static InchiOutput toInchi(String smiles) throws IOException {
		return toInchi(smiles, new InchiOptionsBuilder().build());
	}

	/**
	 * Convert a SMILES string to InChI using the given options
	 * Throws an IOException if there is an issue with the SMILES string
	 * or IllegalArgumentException if given null input
	 * @param smiles
	 * @param options
	 * @return
	 * @throws IOException
	 */
	public static InchiOutput toInchi(String smiles, InchiOptions options) throws IOException {
		if (smiles == null) {
			throw new IllegalArgumentException("SMILES should not be null");
		}
		if (options == null) {
			throw new IllegalArgumentException("options must not be null");
		}
		Graph g = Graph.fromSmiles(smiles);
		InchiInput input = graphToInput(g);
		return JnaInchi.toInchi(input, options);
	}

	private static InchiInput graphToInput(Graph g) throws IOException {
		g = g.kekule();
		InchiInput input = new InchiInput();
		for (int i = 0, len = g.order(); i < len; i++) {
			Atom smiAtom = g.atom(i);
			String elementSymbol = smiAtom.element().symbol();
			if ("*".equals(elementSymbol)) {
				elementSymbol = "Zz";
			}
			InchiAtom a = new InchiAtom(elementSymbol);
			a.setCharge(smiAtom.charge());
			a.setImplicitHydrogen(g.implHCount(i));
			if (smiAtom.isotope() != -1) {
				a.setIsotopicMass(smiAtom.isotope());
			}
			input.addAtom(a);
		}
		for (int i = 0, len = g.order(); i < len; i++) {
			InchiAtom a = input.getAtom(i);
			Configuration stereoConfig = g.configurationOf(i);
			switch (stereoConfig.type()) {
				case Tetrahedral: {
					int[] neighbours = g.neighbors(i);
					InchiStereoParity parity;
					InchiAtom[] atoms = new InchiAtom[4];
					if (neighbours.length == 3) {
						// implicit hydrogen/lone pair
						neighbours = Arrays.copyOf(neighbours, 4);
						neighbours[3] = i;
						Arrays.sort(neighbours);
					}
					if (neighbours.length == 4) {
						for (int j = 0; j < 4; j++) {
							atoms[j] = input.getAtom(neighbours[j]);
						}
					} else {
						// is this actually tetrahedral???
						continue;
					}
					parity = stereoConfig == Configuration.TH1 ? InchiStereoParity.ODD : InchiStereoParity.EVEN;
					input.addStereo(new InchiStereo(atoms, a, InchiStereoType.Tetrahedral, parity));
				}
				break;
				case ExtendedTetrahedral:
					addAllenalStereo(input, g, i);
					break;
				case DoubleBond:
				case Octahedral:
				case SquarePlanar:
				case TrigonalBipyramidal:
				case Implicit:
				case None:
					break;
				default:
					break;
			}
		}

		for (Edge smiBond : g.edges()) {
			int start = smiBond.either();
			int end = smiBond.other(start);
			InchiBondType type;
			switch (smiBond.bond()) {
				case AROMATIC:
					//Shoudn't occur as molecule has been kekulized
					type = InchiBondType.ALTERN;
					break;
				case DOT:
					type = InchiBondType.NONE;
					break;
				case DOUBLE:
				case DOUBLE_AROMATIC:
					type = InchiBondType.DOUBLE;
					break;
				case DOWN:
				case UP:
				case IMPLICIT:
				case IMPLICIT_AROMATIC:
				case SINGLE:
					type = InchiBondType.SINGLE;
					break;
				case TRIPLE:
				case QUADRUPLE:
					type = InchiBondType.TRIPLE;
				default:
					type = InchiBondType.SINGLE;
					break;
			}
			InchiBond b = new InchiBond(input.getAtom(start), input.getAtom(end), type);
			input.addBond(b);

			if (smiBond.bond().order() == 2) {
				// find double bond stereochemistry
				Edge dirEdge1 = findDirectionalEdge(g, start);
				if (dirEdge1 != null) {
					Edge dirEdge2 = findDirectionalEdge(g, end);
					if (dirEdge2 != null) {
						InchiStereoParity parity = (dirEdge1.bond(start) == dirEdge2.bond(end)) ? InchiStereoParity.ODD
								: InchiStereoParity.EVEN;

						InchiAtom[] atoms = new InchiAtom[4];
						atoms[0] = input.getAtom(dirEdge1.other(start));
						atoms[1] = input.getAtom(start);
						atoms[2] = input.getAtom(end);
						atoms[3] = input.getAtom(dirEdge2.other(end));

						input.addStereo(new InchiStereo(atoms, null, InchiStereoType.DoubleBond, parity));
					}
				}
			}
		}
		return input;
	}

	private static Edge findDirectionalEdge(Graph g, int atom) {
		List<Edge> edges = g.edges(atom);
		if (edges.size() > 1) {
			for (Edge e : edges) {
				Bond b = e.bond();
				if (b == Bond.UP || b == Bond.DOWN) {
					return e;
				}
			}
		}
		return null;
	}

	private static void addAllenalStereo(InchiInput input, Graph g, int allenalCenter) {
		List<Edge> bonds = g.edges(allenalCenter);
		if (bonds.size() !=2 || bonds.get(0).bond().order() != 2 || bonds.get(1).bond().order() != 2) {
			return;
		}
		int next1 = bonds.get(0).other(allenalCenter);
		int next2 = bonds.get(1).other(allenalCenter);
		int prev1 = allenalCenter;
		int prev2 = allenalCenter;

		int tmp1;
		int tmp2;
		while ((tmp1 = nextDb(g, next1, prev1)) >=0 && (tmp2 = nextDb(g, next2, prev2)) >=0 ) {
			prev1 = next1;
			prev2 = next2;
			next1 = tmp1;
			next2 = tmp2;
		}
		int[] atomIdxs = new int[4];
		for (Edge e : g.edges(next1)) {
			if (e.bond().order() == 1) {
				atomIdxs[0] = e.other(next1);
			}
		}
		atomIdxs[1] = next1;
		atomIdxs[2] = next2;
		for (Edge e : g.edges(next2)) {
			if (e.bond().order() == 1) {
				atomIdxs[3] = e.other(next2);
			}
		}
		Arrays.sort(atomIdxs);

		InchiAtom[] atoms = new InchiAtom[4];
		for (int i = 0; i < atomIdxs.length; i++) {
			atoms[i] = input.getAtom(atomIdxs[i]);
		}
		InchiStereoParity parity = (g.configurationOf(allenalCenter) == Configuration.AL1) ? InchiStereoParity.ODD : InchiStereoParity.EVEN;
		input.addStereo(new InchiStereo(atoms, input.getAtom(allenalCenter), InchiStereoType.Allene, parity));
		//FIXME ...is this actually correctly implemented right?
	}

	private static int nextDb(Graph g, int current, int prev) {
		List<Edge> bonds = g.edges(current);
		if (bonds.size() !=2 || bonds.get(0).bond().order() != 2 || bonds.get(1).bond().order() != 2) {
			return -1;
		}
		for (Edge e : bonds) {
			int next = e.other(current);
			if (next != prev) {
				return next;
			}
		}
		return -1;
	}
}