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

import com.actelion.research.chem.*;
import com.actelion.research.chem.coords.CoordinateInventor;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.gui.CompoundCollectionPane;
import com.actelion.research.gui.DefaultCompoundCollectionModel;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.TreeMap;

import static com.actelion.research.chem.coords.CoordinateInventor.MODE_PREFER_MARKED_ATOM_COORDS;

public class DETaskCoreBasedSAR extends DETaskAbstractFromStructure {
	public static final String TASK_NAME = "Core-Based SAR Analysis";

	private static final String PROPERTY_SCAFFOLD_LIST = "scaffoldList";
	private static final String PROPERTY_USE_EXISTING_COLUMNS = "useExistingColumns";
	private static final String PROPERTY_DISTINGUISH_STEREO_ISOMERS = "considerStereo";

	private static final String CORE_FRAGMENT_COLUMN_NAME = "Scaffold";
	private static final int MAX_R_GROUPS = 16;
	private static final int cTableColumnNew = -2;

	private DefaultCompoundCollectionModel.Molecule mScaffoldModel;
	private JCheckBox			mCheckBoxDistinguishStereocenters,mCheckBoxUseExistingColumns;
	private String[][]			mScaffold;
	private String[][]			mSubstituent;
	private int					mScaffoldColumn,mScaffoldCoordsColumn,mMultipleMatches,mNewColumnCount;
	private int[]				mSubstituentColumn;

    public DETaskCoreBasedSAR(DEFrame parent) {
		super(parent, DESCRIPTOR_NONE, false, false);
	    }

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/chemistry.html#SARTables";
		}

	@Override
	public boolean hasExtendedDialogContent() {
		return true;
		}

	@Override
	public JPanel getExtendedDialogContent() {
		JPanel ep = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {HiDPIHelper.scale(480)},
							{gap, TableLayout.PREFERRED, gap/2, HiDPIHelper.scale(96), gap, TableLayout.PREFERRED, TableLayout.PREFERRED} };
		ep.setLayout(new TableLayout(size));
		ep.add(new JLabel("Define scaffold structures:"), "0,1");

		mScaffoldModel = new DefaultCompoundCollectionModel.Molecule();
		CompoundCollectionPane<StereoMolecule> scaffoldPane = new CompoundCollectionPane<StereoMolecule>(mScaffoldModel, false);
		scaffoldPane.setCreateFragments(true);
		scaffoldPane.setEditable(true);
		scaffoldPane.setClipboardHandler(new ClipboardHandler());
		scaffoldPane.setShowValidationError(true);
		ep.add(scaffoldPane, "0,3");

        mCheckBoxDistinguishStereocenters = new JCheckBox("Distinguish stereoisomers", true);
		ep.add(mCheckBoxDistinguishStereocenters, "0,5");

		mCheckBoxUseExistingColumns = new JCheckBox("Use existing columns for scaffold and substituents", true);
		ep.add(mCheckBoxUseExistingColumns, "0,7");

		return ep;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();

		StringBuilder sb = new StringBuilder();
		for (int i=0; i<mScaffoldModel.getSize(); i++) {
			if (sb.length() != 0)
				sb.append('\t');
			Canonizer canonizer = new Canonizer(mScaffoldModel.getMolecule(i));
			sb.append(canonizer.getIDCode()+" "+canonizer.getEncodedCoordinates());
			}
		configuration.setProperty(PROPERTY_SCAFFOLD_LIST, sb.toString());

		configuration.setProperty(PROPERTY_DISTINGUISH_STEREO_ISOMERS, mCheckBoxDistinguishStereocenters.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_USE_EXISTING_COLUMNS, mCheckBoxUseExistingColumns.isSelected() ? "true" : "false");

		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);

		for (String idcode:configuration.getProperty(PROPERTY_SCAFFOLD_LIST, "").split("\\t"))
			mScaffoldModel.addCompound(new IDCodeParser(true).getCompactMolecule(idcode));

		mCheckBoxDistinguishStereocenters.setSelected("true".equals(configuration.getProperty(PROPERTY_DISTINGUISH_STEREO_ISOMERS)));
		mCheckBoxUseExistingColumns.setSelected("true".equals(configuration.getProperty(PROPERTY_USE_EXISTING_COLUMNS)));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mCheckBoxUseExistingColumns.setSelected(true);
		}

	@Override
	protected int getNewColumnCount() {
		return mNewColumnCount;
		}

	@Override
	protected String getNewColumnName(int column) {
		if (column == 0 && mScaffoldColumn == cTableColumnNew)
			return CORE_FRAGMENT_COLUMN_NAME;
		int index = (mScaffoldColumn == cTableColumnNew) ? 1 : 0;
		for (int i=0; i<mSubstituentColumn.length; i++) {
			if (mSubstituentColumn[i] == cTableColumnNew) {
				if (index == column)
					return "R"+(i+1);

				index++;
				}
			}
		return "scaffoldAtomCoords";
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String scaffoldList = configuration.getProperty(PROPERTY_SCAFFOLD_LIST, "");
		if (scaffoldList.length() == 0) {
			showErrorMessage("No scaffolds defined.");
			return false;
			}
		for (String idcode:scaffoldList.split("\\t")) {
			try {
				new IDCodeParser(true).getCompactMolecule(idcode).validate();
				}
			catch (Exception e) {
				showErrorMessage("Some of the scaffold structures are not valid:\n"+e);
				return false;
				}
			}

		return super.isConfigurationValid(configuration, isLive);
		}

	@Override
	public boolean preprocessRows(Properties configuration) {
		boolean distinguishStereoCenters = "true".equals(configuration.getProperty(PROPERTY_DISTINGUISH_STEREO_ISOMERS));

		int rowCount = getTableModel().getTotalRowCount();
		mScaffold = new String[rowCount][2];
		mSubstituent = new String[rowCount][];
		int notFoundCount = 0;

		CoreInfo[] coreInfo = new CoreInfo[rowCount];

		String[] queryIDCode = configuration.getProperty(PROPERTY_SCAFFOLD_LIST, "").split("\\t");

		for (String idcode:queryIDCode) {
try {   // TODO remove
			StereoMolecule query = new IDCodeParser(true).getCompactMolecule(idcode);
			ScaffoldGroup sg = processScaffoldsOfQuery(query, distinguishStereoCenters, coreInfo);
			if (sg == null)
				notFoundCount++;
} catch (Exception e) { e.printStackTrace(); }
			}

		if (notFoundCount == queryIDCode.length && isInteractive()) {
			final String message = "None of your scaffolds were found in the '"+getTableModel().getColumnTitle(getChemistryColumn())+"' column.";
			showInteractiveTaskMessage(message, JOptionPane.INFORMATION_MESSAGE);
			return false;
			}

		int substituentCount = 0;
		for (int row=0; row<rowCount; row++)
			if (coreInfo[row] != null)
				substituentCount = Math.max(substituentCount, coreInfo[row].getRGroupCount());

		if (substituentCount == 0) {
			mSubstituent = null;
			}
		else {
			for (int row=0; row<rowCount; row++) {
				if (mSubstituent[row] != null) {
					String[] newSubstituent = new String[substituentCount];
					if (coreInfo[row] != null)
						for (int coreAtom=0; coreAtom<coreInfo[row].getCoreAtomCount(); coreAtom++)
							if (coreInfo[row].getRGroupNo(coreAtom) != 0)
								newSubstituent[coreInfo[row].getRGroupNo(coreAtom)-1] = mSubstituent[row][coreAtom];
					mSubstituent[row] = newSubstituent;
					}
				}
			}

		mScaffoldColumn = cTableColumnNew;
		mScaffoldCoordsColumn = cTableColumnNew;
		mSubstituentColumn = new int[substituentCount];
		for (int i=0; i<substituentCount; i++)
			mSubstituentColumn[i] = cTableColumnNew;

		boolean useExistingColumns = "true".equals(configuration.getProperty(PROPERTY_USE_EXISTING_COLUMNS));
		if (useExistingColumns) {
			int column = getTableModel().findColumn(CORE_FRAGMENT_COLUMN_NAME);
			if (column != -1) {
	            String specialType = getTableModel().getColumnSpecialType(column);
	            if (specialType != null && specialType.equals(CompoundTableModel.cColumnTypeIDCode)) {
		            mScaffoldColumn = column;
		            int coordsColumn = getTableModel().getChildColumn(column, CompoundTableConstants.cColumnType2DCoordinates);
		            if (coordsColumn != -1)
		            	mScaffoldCoordsColumn = coordsColumn;
	                }
	            }
			for (int i=0; i<substituentCount; i++) {
				String columnName = "R"+(i+1);
				column = getTableModel().findColumn(columnName);
				if (column != -1) {
		            String specialType = getTableModel().getColumnSpecialType(column);
		            if (specialType != null && specialType.equals(CompoundTableModel.cColumnTypeIDCode))
		            	mSubstituentColumn[i] = column;
		            }
				}
			}

		mNewColumnCount = (mScaffoldColumn == cTableColumnNew ? 1 : 0)
						+ (mScaffoldCoordsColumn == cTableColumnNew ? 1 : 0);
		for (int i=0; i<mSubstituentColumn.length; i++)
			if (mSubstituentColumn[i] == cTableColumnNew)
				mNewColumnCount++;

		return true;
		}

	/**
	 * Processes entire table with one of the defined scaffolds:<br>
	 * For every row with no previously found scaffold it checks whether the row's structure contains scaffold as substructure.
	 * With all rows, where scaffold was found as substructure<br>
	 * - For every scaffold atom it is determined whether these bear changing substituents through matching rows.<br>
	 * - For every scaffold atom with changing substituents, the substituent of every row is created and put into substituent.<br> 
	 * - For every scaffold atom with no or a constant substituent, substituent is set to null and the constant substituent is attached to the scaffold structure.<br>
	 * - The decorated scaffold structure is written into mScaffold for these rows.<br>
	 * @param query
	 * @param distinguishStereoCenters
	 * @return false if the scaffold could not be found in any row
	 */
	private ScaffoldGroup processScaffoldsOfQuery(StereoMolecule query, boolean distinguishStereoCenters, CoreInfo[] coreInfoOfRow) {
		SSSearcherWithIndex searcher = new SSSearcherWithIndex();
		searcher.setFragment(query, (long[])null);

		query.ensureHelperArrays(Molecule.cHelperNeighbours);

		TreeMap<String,CoreInfo> coreMap = new TreeMap<>();
		ScaffoldGroup scaffoldGroup = new ScaffoldGroup(query);

		mMultipleMatches = 0;

		startProgress("Analyzing substituents...", 0, getTableModel().getTotalRowCount());

        int coordinateColumn = getTableModel().getChildColumn(getChemistryColumn(), CompoundTableModel.cColumnType2DCoordinates);
        int fingerprintColumn = getTableModel().getChildColumn(getChemistryColumn(), DescriptorConstants.DESCRIPTOR_FFP512.shortName);
		StereoMolecule fragment = new StereoMolecule();
		boolean[] isQueryMatch = new boolean[getTableModel().getTotalRowCount()];
		boolean[][] substituentConnectsBack = new boolean[getTableModel().getTotalRowCount()][];

		for (int row=0; row<getTableModel().getTotalRowCount(); row++) {
			if (threadMustDie())
				break;

			if (mScaffold[row][0] != null)
				continue;

			updateProgress(row+1);

			byte[] idcode = (byte[])getTableModel().getTotalRecord(row).getData(getChemistryColumn());
			if (idcode != null) {
				searcher.setMolecule(idcode, (long[])getTableModel().getTotalRecord(row).getData(fingerprintColumn));
				int matchCount = searcher.findFragmentInMolecule(SSSearcher.cCountModeRigorous, SSSearcher.cDefaultMatchMode);
				if (matchCount > 0) {
					isQueryMatch[row] = true;

					int match = 0;  // currently consider just the first match. Later do that more prudently

					if (matchCount > 1)
						mMultipleMatches++;

					int[] queryToMolAtom = searcher.getGraphMatcher().getMatchList().get(match);

					byte[] coords = (byte[])getTableModel().getTotalRecord(row).getData(coordinateColumn);
					StereoMolecule mol = new IDCodeParser(true).getCompactMolecule(idcode, coords);

						// store original fragment atom numbers incremented by 1 in atomMapNo
					for (int i=0; i<queryToMolAtom.length; i++)
						if (queryToMolAtom[i] != -1)
							mol.setAtomMapNo(queryToMolAtom[i], i+1, false);

					// Mark all atoms belonging to core fragment
					boolean[] isCoreAtom = new boolean[mol.getAtoms()];
					for (int i=0; i<queryToMolAtom.length; i++)
						if (queryToMolAtom[i] != -1)
							isCoreAtom[queryToMolAtom[i]] = true;

					boolean[] isBridgeAtom = searcher.getGraphMatcher().getMatchingBridgeBondAtoms(match);
					if (isBridgeAtom != null)
						for (int i=0; i<isBridgeAtom.length; i++)
							if (isBridgeAtom[i])
								isCoreAtom[i] = true;

// TODO for symmetrical matches choose the most reasonable one (ideally by minimizing different substitution patterns AND! kinds of substituents)

					// This is basically the query match, cut out of the real molecules.
					// In case of bridge bonds, it has more atoms than the query itself!
					StereoMolecule core = new StereoMolecule();
					int[] molToCoreAtom = new int[isCoreAtom.length];
					mol.copyMoleculeByAtoms(core, isCoreAtom, true, molToCoreAtom);
					Canonizer coreCanonizer = new Canonizer(core);
					int[] coreGraphIndex = coreCanonizer.getGraphIndexes();
					for (int i=0; i<molToCoreAtom.length; i++)
						if (molToCoreAtom[i] != -1)
							molToCoreAtom[i] = coreGraphIndex[molToCoreAtom[i]];
					core = coreCanonizer.getCanMolecule(false);
					core.ensureHelperArrays(Molecule.cHelperNeighbours);
					int[] coreToMolAtom = new int[core.getAtoms()];
					for (int atom=0; atom<mol.getAtoms(); atom++)
						if (molToCoreAtom[atom] != -1)
							coreToMolAtom[molToCoreAtom[atom]] = atom;

					String extendedCoreIDCode = null;
					int[] coreAtomParity = null;
					if (distinguishStereoCenters) {
						boolean[] isExtendedCoreAtom = new boolean[mol.getAtoms()];	// core plus direct neighbours
						for (int coreAtom=0; coreAtom<core.getAtoms(); coreAtom++) {
							int molAtom = coreToMolAtom[coreAtom];
							isExtendedCoreAtom[molAtom] = true;
							for (int j=0; j<mol.getConnAtoms(molAtom); j++)
								isExtendedCoreAtom[mol.getConnAtom(molAtom, j)] = true;
							}

						StereoMolecule extendedCore = new StereoMolecule();	// core plus direct neighbours
						int[] molToExtendedCoreAtom = new int[mol.getAtoms()];
						mol.copyMoleculeByAtoms(extendedCore, isExtendedCoreAtom, true, molToExtendedCoreAtom);

						// Mark atomicNo of non-core atoms with atominNo=0 to make sure that the atom order of the
						// core atoms withint canonical extendedCore matches the one in canonical core:
						// This way we can directly copy parities from extendedCore to the new molecule constructed from core.
						for (int atom=0; atom<mol.getAtoms(); atom++)
							if (isExtendedCoreAtom[atom] && !isCoreAtom[atom])
								extendedCore.setAtomicNo(molToExtendedCoreAtom[atom], 0);	// '?'

						Canonizer extendedCoreCanonizer = new Canonizer(extendedCore);
						extendedCoreIDCode = extendedCoreCanonizer.getIDCode();
						int[] extendedCoreGraphIndex = extendedCoreCanonizer.getGraphIndexes();
						for (int i=0; i<molToExtendedCoreAtom.length; i++)
							if (molToExtendedCoreAtom[i] != -1)
								molToExtendedCoreAtom[i] = extendedCoreGraphIndex[molToExtendedCoreAtom[i]];
						extendedCore = extendedCoreCanonizer.getCanMolecule(false);

						extendedCore.ensureHelperArrays(Molecule.cHelperParities);

						boolean stereoCenterFound = false;
						coreAtomParity = new int[core.getAtoms()];
						for (int coreAtom=0; coreAtom<core.getAtoms(); coreAtom++) {
							int molAtom = coreToMolAtom[coreAtom];
							int ecAtom = molToExtendedCoreAtom[molAtom];
							if (extendedCore.isAtomStereoCenter(ecAtom)) {
								int atomParity = extendedCore.getAtomParity(ecAtom);
                                if (atomParity != Molecule.cAtomParityNone)
                                    stereoCenterFound = true;
								coreAtomParity[coreAtom] = atomParity;
								if (atomParity == Molecule.cAtomParity1
								 || atomParity == Molecule.cAtomParity2) {
                                    int esrType = extendedCore.getAtomESRType(ecAtom);
                                    if (esrType != Molecule.cESRTypeAbs) {
                                        int esrEncoding = (extendedCore.getAtomESRGroup(ecAtom) << 4)
                                                        + ((esrType == Molecule.cESRTypeAnd) ? 4 : 8);
                                        coreAtomParity[coreAtom] += esrEncoding;
                                        }
                                    }
								}
							}
                        if (!stereoCenterFound)
	                        coreAtomParity = null;
                        else
                        	extendedCoreIDCode = new Canonizer(extendedCore).getIDCode();
						}

					core.setFragment(false);
					core.stripStereoInformation();
					String coreIDCode = (extendedCoreIDCode != null) ? extendedCoreIDCode : new Canonizer(core).getIDCode();

					// Create one CoreInfo for every distinct core structure and assign all rows
					// having the same core structure to the respective CoreInfo.
					coreInfoOfRow[row] = coreMap.get(coreIDCode);
					if (coreInfoOfRow[row] == null) {
						int[] queryToCoreAtom = new int[query.getAtoms()];
						int[] coreToQueryAtom = new int[core.getAtoms()];
						Arrays.fill(queryToCoreAtom, -1);	// account for exclude atoms
						Arrays.fill(coreToQueryAtom, -1);	// account for bridge atoms
						for (int queryAtom=0; queryAtom<query.getAtoms(); queryAtom++) {
							int molAtom = queryToMolAtom[queryAtom];
							if (molAtom != -1) {
								int coreAtom = molToCoreAtom[molAtom];
								queryToCoreAtom[queryAtom] = coreAtom;
								coreToQueryAtom[coreAtom] = queryAtom;
								}
							}

						adaptCoreAtomCoordsFromQuery(query, core, queryToCoreAtom, isBridgeAtom != null);

						coreInfoOfRow[row] = new CoreInfo(core, coreAtomParity, coreToQueryAtom, scaffoldGroup);
						coreMap.put(coreIDCode, coreInfoOfRow[row]);
						scaffoldGroup.add(coreInfoOfRow[row]);
						}

					// We change all molecule atoms, which belong to the core, to connection point atoms
					// in order to easily extract/copy substituents including connections points from the molecule.
					for (int i=0; i<core.getAtoms(); i++) {
						int atom = coreToMolAtom[i];
						mol.setAtomicNo(atom, 0);
						mol.setAtomCustomLabel(atom, Integer.toString(molToCoreAtom[atom]));	// we encode the core atom index
						}

					// For all core atoms that carry substituents in the molecule,
					// create substituent idcodes and assign them to the respective core atoms.
					int[] workAtom = new int[mol.getAllAtoms()];
					substituentConnectsBack[row] = new boolean[core.getAtoms()];
					for (int coreAtom=0; coreAtom<core.getAllAtoms(); coreAtom++) {
						if (mol.getConnAtoms(coreToMolAtom[coreAtom]) > core.getConnAtoms(coreAtom)) {
							boolean[] isSubstituentAtom = new boolean[mol.getAtoms()];
							isSubstituentAtom[coreToMolAtom[coreAtom]] = true;
							workAtom[0] = coreToMolAtom[coreAtom];
							int current = 0;
							int highest = 0;
							while (current <= highest) {
								for (int j=0; j<mol.getConnAtoms(workAtom[current]); j++) {
									if (current == 0 || !isCoreAtom[workAtom[current]]) {
										int candidate = mol.getConnAtom(workAtom[current], j);
										if (!isSubstituentAtom[candidate]
										 && (current != 0 || !isCoreAtom[candidate])) {
											isSubstituentAtom[candidate] = true;
											workAtom[++highest] = candidate;
											if (isCoreAtom[candidate])
												substituentConnectsBack[row][coreAtom] = true;
											}
										}
									}
								current++;
								}

							fragment.clear();
							mol.setAtomCustomLabel(coreToMolAtom[coreAtom], (String)null);	// no encoding for the connection atom

							mol.copyMoleculeByAtoms(fragment, isSubstituentAtom, false, null);

							mol.setAtomCustomLabel(coreToMolAtom[coreAtom], Integer.toString(coreAtom));	// restore encoding
							fragment.setFragment(false);

							if (!distinguishStereoCenters)
								fragment.stripStereoInformation();

								// if substituent is a ring forming bridge to the startatom
							for (int bond=fragment.getAllBonds()-1; bond>=0; bond--)
								if (fragment.getAtomicNo(fragment.getBondAtom(0, bond)) == 0
								 && fragment.getAtomicNo(fragment.getBondAtom(1, bond)) == 0)
									fragment.deleteBond(bond);

							if (mSubstituent[row] == null)
								mSubstituent[row] = new String[core.getAtoms()];

							mSubstituent[row][coreAtom] = (highest == 0) ? null : new Canonizer(fragment, Canonizer.ENCODE_ATOM_CUSTOM_LABELS).getIDCode();
							}
						}
					}
				}
			}

		if (coreMap.isEmpty())
			return null;

		if (threadMustDie())
			return null;

		// check for varying substituents to require a new column
		for (int row=0; row<getTableModel().getTotalRowCount(); row++)
			if (isQueryMatch[row])
				for (int coreAtom=0; coreAtom<coreInfoOfRow[row].getCoreAtomCount(); coreAtom++)
					coreInfoOfRow[row].checkSubstituent(mSubstituent[row] == null ? null : mSubstituent[row][coreAtom], coreAtom);

		// Remove substituents from atoms, which didn't see a varying substitution
		for (int row=0; row<getTableModel().getTotalRowCount(); row++)
			if (isQueryMatch[row] && mSubstituent[row] != null)
				for (int coreAtom=0; coreAtom<coreInfoOfRow[row].getCoreAtomCount(); coreAtom++)
					if (mSubstituent[row][coreAtom] != null
					 && !coreInfoOfRow[row].substituentVaries(coreAtom))
						mSubstituent[row][coreAtom] = null;

		int scaffoldRGroupCount = scaffoldGroup.assignRGroupsToAtoms();

		for (CoreInfo coreInfo:scaffoldGroup) {
			int rGroupsOnBridges = coreInfo.assignRGroupsToBridgeAtoms(scaffoldRGroupCount);

			if (scaffoldRGroupCount + rGroupsOnBridges > MAX_R_GROUPS) {
				final String message = "Found "+(scaffoldRGroupCount + rGroupsOnBridges)+" R-groups, which exceeds the allowed maximum: "+MAX_R_GROUPS;
				showInteractiveTaskMessage(message, JOptionPane.INFORMATION_MESSAGE);
				for (int row=0; row<getTableModel().getTotalRowCount(); row++)
					if (coreInfoOfRow[row].getRGroupCount() > MAX_R_GROUPS)
						coreInfoOfRow[row] = null;
				return null;
				}

			StereoMolecule core = coreInfo.getCoreStructure();

			boolean[] closureCovered = new boolean[core.getAtoms()];
			for (int coreAtom=0; coreAtom<core.getAtoms(); coreAtom++) {
						//	if substituent varies => attach an R group
				if (coreInfo.substituentVaries(coreAtom)) {
					int rGroupNo = coreInfo.getRGroupNo(coreAtom);
					int newAtom = core.addAtom((rGroupNo <= 3) ? 141+rGroupNo : 125+rGroupNo);
					core.addBond(coreAtom, newAtom, 1);
					}
				else {	//	else => attach the non-varying substituent (if it is not null = 'unsubstituted')
					if (!closureCovered[coreAtom] && coreInfo.getConstantSubstituent(coreAtom) != null) {
						StereoMolecule theSubstituent = new IDCodeParser(true).getCompactMolecule(coreInfo.getConstantSubstituent(coreAtom));

						// Substitutions, which connect back to the core fragment are encoded with labels on the connecting atoms: "core atom index".
						// Now we translate labels back to atomMapNos, which are used by addSubstituent() to create back connections.
						for (int a=0; a<theSubstituent.getAllAtoms(); a++) {
							String label = theSubstituent.getAtomCustomLabel(a);
							if (label != null) {
								int atom = Integer.parseInt(label);
								theSubstituent.setAtomCustomLabel(a, (String)null);
								theSubstituent.setAtomicNo(a, 0);
								theSubstituent.setAtomMapNo(a, atom+1, false);
								closureCovered[atom] = true;
								}
							}
						core.addSubstituent(theSubstituent, coreAtom, true);
						}
					}
				}

			int[] parityList = coreInfo.getAtomParities();
			if (parityList != null) {
				for (int coreAtom=0; coreAtom<core.getAtoms(); coreAtom++) {
					int parity = parityList[coreAtom] & 3;
                    int esrType = (parityList[coreAtom] & 0x0C);
                    int esrGroup = (parityList[coreAtom] & 0xF0) >> 4;
                    core.setAtomParity(coreAtom, parity, false);
					if (esrType != 0) {
                        core.setAtomESR(coreAtom, esrType == 4 ?
                                Molecule.cESRTypeAnd : Molecule.cESRTypeOr, esrGroup);
				        }
                    }
				core.setParitiesValid(0);
                }

			coreInfo.buildIDCodeAndCoords();
			}

		encodeSubstituentRingClosures(coreInfoOfRow, substituentConnectsBack);

		for (int row=0; row<getTableModel().getTotalRowCount(); row++) {
			if (coreInfoOfRow[row] != null) {
				mScaffold[row][0] = coreInfoOfRow[row].getIDCodeWithRGroups();
				mScaffold[row][1] = coreInfoOfRow[row].getIDCoordsWithRGroups();
				}
			}

		return scaffoldGroup;
		}

	/**
	 * The core atom index in case of substituent ring closures was encoded as label in the substituent idcode.
	 * This ensured in the check for varying substituents that chains with inverted symmetry are recognized as
	 * different substituent. After this check and once we have a mapping from scaffold atom index to R-group index,
	 * we need to exchange the label by a new one with the R-Group index, which should be finally displayed to the user.
	 * @param coreInfoOfRow
	 * @param substituentConnectsBack [core atom index][row index]
	 */
	private void encodeSubstituentRingClosures(CoreInfo[] coreInfoOfRow, boolean[][] substituentConnectsBack) {
		for (int row=0; row<coreInfoOfRow.length; row++) {
			if (substituentConnectsBack[row] != null) {
				for (int coreAtom=0; coreAtom<coreInfoOfRow[row].getCoreAtomCount(); coreAtom++) {
					if (coreInfoOfRow[row].substituentVaries(coreAtom)
					 && substituentConnectsBack[row][coreAtom]
					 && mSubstituent[row][coreAtom] != null) {
						String newIDCode = coreInfoOfRow[row].getOldToNewMap().get(mSubstituent[row][coreAtom]);
						if (newIDCode != null) {
							mSubstituent[row][coreAtom] = newIDCode;
							}
						else {
							StereoMolecule s = new IDCodeParser().getCompactMolecule(mSubstituent[row][coreAtom]);
							for (int atom=0; atom<s.getAllAtoms(); atom++) {
								String label = s.getAtomCustomLabel(atom);
								if (label != null)
									s.setAtomCustomLabel(atom, Integer.toString(coreInfoOfRow[row].getRGroupNo(Integer.parseInt(label))));
								}
							newIDCode = new Canonizer(s, Canonizer.ENCODE_ATOM_CUSTOM_LABELS).getIDCode();
							coreInfoOfRow[row].getOldToNewMap().put(mSubstituent[row][coreAtom], newIDCode);
							mSubstituent[row][coreAtom] = newIDCode;
							}
						}
					}
				}
			}
		}

	private void adaptCoreAtomCoordsFromQuery(StereoMolecule query, StereoMolecule core, int[] queryToCoreAtom, boolean hasBridgeAtoms) {
		if (!hasBridgeAtoms) {
			// just copy query atom coordinates and mark them to be untouched for later coordinate invention
			for (int queryAtom = 0; queryAtom<queryToCoreAtom.length; queryAtom++) {
				if (queryToCoreAtom[queryAtom] != -1) {
					int coreAtom = queryToCoreAtom[queryAtom];
					core.setAtomX(coreAtom, query.getAtomX(queryAtom));
					core.setAtomY(coreAtom, query.getAtomY(queryAtom));
					core.setAtomMarker(coreAtom, true);  // to later keep the original query coordinates
				}
			}
		} else {
			// Generate new core coordinates and flip and rotate to closely match query orientation
			new CoordinateInventor().invent(core);
			double[] cogQuery = new double[2];
			double[] cogCore = new double[2];
			int sharedAtomCount = 0;
			for (int queryAtom = 0; queryAtom<queryToCoreAtom.length; queryAtom++) {
				if (queryToCoreAtom[queryAtom] != -1) {
					int coreAtom = queryToCoreAtom[queryAtom];
					cogCore[0] += core.getAtomX(coreAtom);
					cogCore[1] += core.getAtomY(coreAtom);
					cogQuery[0] += query.getAtomX(queryAtom);
					cogQuery[1] += query.getAtomY(queryAtom);
					sharedAtomCount++;
				}
			}
			cogCore[0] /= sharedAtomCount;
			cogCore[1] /= sharedAtomCount;
			cogQuery[0] /= sharedAtomCount;
			cogQuery[1] /= sharedAtomCount;

			double[] weight = new double[sharedAtomCount];
			double[] rotation = new double[sharedAtomCount];
			double[] flippedRotation = new double[sharedAtomCount];
			int index = 0;
			for (int queryAtom = 0; queryAtom<queryToCoreAtom.length; queryAtom++) {
				if (queryToCoreAtom[queryAtom] != -1) {
					int coreAtom = queryToCoreAtom[queryAtom];

					double cx = core.getAtomX(coreAtom) - cogCore[0];
					double cy = core.getAtomY(coreAtom) - cogCore[1];
					double squareDistCore = cx * cx + cy * cy;

					double qx = query.getAtomX(queryAtom) - cogQuery[0];
					double qy = query.getAtomY(queryAtom) - cogQuery[1];
					double squareDistQuery = qx * qx + qy * qy;

					weight[index] = Math.sqrt(squareDistCore * squareDistQuery);

					double angleQuery = Molecule.getAngle(cogQuery[0], cogQuery[1], query.getAtomX(queryAtom), query.getAtomY(queryAtom));
					double angleCore = Molecule.getAngle(cogCore[0], cogCore[1], core.getAtomX(coreAtom), core.getAtomY(coreAtom));
					rotation[index] = Molecule.getAngleDif(angleCore, angleQuery);
					flippedRotation[index] = Molecule.getAngleDif(-angleCore, angleQuery);

					index++;
				}
			}

			double meanRotation = 0.0;
			double meanFlippedRotation = 0.0;
			double weightSum = 0.0;
			for (int i = 0; i<index; i++) {
				meanRotation += weight[i] * rotation[i];
				meanFlippedRotation += weight[i] * flippedRotation[i];
				weightSum += weight[i];
				}
			meanRotation /= weightSum;
			meanFlippedRotation /= weightSum;

			double penalty = 0.0;
			double flippedPanalty = 0.0;
			for (int i = 0; i<index; i++) {
				penalty += weight[i] * Math.abs(Molecule.getAngleDif(rotation[i], meanRotation));
				flippedPanalty += weight[i] * Math.abs(Molecule.getAngleDif(flippedRotation[i], meanFlippedRotation));
				}

			if (penalty < flippedPanalty) {
				core.zoomAndRotateInit(cogCore[0], cogCore[1]);
				core.zoomAndRotate(1.0, meanRotation, false);
				}
			else {
				for (int coreAtom=0; coreAtom<core.getAllAtoms(); coreAtom++)
					core.setAtomX(coreAtom, 2.0 * cogCore[0] - core.getAtomX(coreAtom));
				for (int coreBond=0; coreBond<core.getAllBonds(); coreBond++)
					if (core.isStereoBond(coreBond))
						core.setBondType(coreBond, core.getBondType(coreBond) == Molecule.cBondTypeUp ? Molecule.cBondTypeDown : Molecule.cBondTypeUp);
				core.zoomAndRotateInit(cogCore[0], cogCore[1]);
				core.zoomAndRotate(1.0, meanFlippedRotation, false);
				}

			for (int coreAtom=0; coreAtom<core.getAllAtoms(); coreAtom++)
				core.setAtomMarker(coreAtom, true);  // to later keep the original query coordinates
			}
		}

	@Override
	protected void setNewColumnProperties(int firstNewColumn) {
		int lastNewColumn = firstNewColumn;
		if (mScaffoldColumn == cTableColumnNew)
			mScaffoldColumn = lastNewColumn++;
		if (mSubstituentColumn != null)
			for (int i=0; i<mSubstituentColumn.length; i++)
				if (mSubstituentColumn[i] == cTableColumnNew)
					mSubstituentColumn[i] = lastNewColumn++;

		for (int i=firstNewColumn; i<lastNewColumn; i++)
            getTableModel().setColumnProperty(i, CompoundTableModel.cColumnPropertySpecialType, CompoundTableModel.cColumnTypeIDCode);

		if (mScaffoldCoordsColumn == cTableColumnNew)
			mScaffoldCoordsColumn = lastNewColumn;

		getTableModel().setColumnProperty(mScaffoldCoordsColumn, CompoundTableModel.cColumnPropertySpecialType, CompoundTableModel.cColumnType2DCoordinates);
		getTableModel().setColumnProperty(mScaffoldCoordsColumn, CompoundTableModel.cColumnPropertyParentColumn, getTableModel().getColumnTitleNoAlias(mScaffoldColumn));
		}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol, int threadIndex) {
		if (mScaffold[row][0] != null) {
			getTableModel().removeChildDescriptorsAndCoordinates(row, mScaffoldColumn);
			getTableModel().setTotalValueAt(mScaffold[row][0], row, mScaffoldColumn);
			getTableModel().setTotalValueAt(mScaffold[row][1], row, mScaffoldCoordsColumn);
			if (mSubstituent != null && mSubstituent[row] != null) {
				for (int i=0; i<mSubstituent[row].length; i++) {
					getTableModel().setTotalValueAt(mSubstituent[row][i], row, mSubstituentColumn[i]);
					getTableModel().removeChildDescriptorsAndCoordinates(row, mSubstituentColumn[i]);
					}
				}
			}
		}

	@Override
	public void postprocess(int firstNewColumn) {
		if (mScaffoldColumn < firstNewColumn)
			getTableModel().finalizeChangeChemistryColumn(mScaffoldColumn, 0, getTableModel().getTotalRowCount(), false);
		if (mSubstituentColumn != null)
			for (int i=0; i<mSubstituentColumn.length; i++)
				if (mSubstituentColumn[i] < firstNewColumn)
					getTableModel().finalizeChangeChemistryColumn(mSubstituentColumn[i], 0, getTableModel().getTotalRowCount(), false);

		if (isInteractive() && mMultipleMatches > 0) {
			final String message = "In "+mMultipleMatches+" cases a symmetrical scaffold could be matched multiple times.\n"
								 + "In these cases R-groups could not be assigned in a unique way.\n"
								 + "You may try avoiding this by specifying less symmetric scaffold structures.";
			showInteractiveTaskMessage(message, JOptionPane.WARNING_MESSAGE);
			}
		}
	}

class CoreInfo {
	private StereoMolecule mCore;
	private int[] mCoreToQueryAtom;
	private boolean[] mSubstituentVaries,mEmptySubstituentSeen;
	private int[] mAtomParity;
	private String mIDCodeWithRGroups, mIDCoordsWithRGroups;
	private String[] mConstantSubstituent;
	private int[] mCoreAtomToRGroupNo;
	private TreeMap<String,String> mOldToNewMap;
	private int mCoreAtomCount,mBridgeAtomRGroupCount;
	private ScaffoldGroup mScaffoldGroup;

	public CoreInfo(StereoMolecule core, int[] atomParity, int[] coreToQueryAtom, ScaffoldGroup scaffoldGroup) {
		mCore = core;
		mCoreAtomCount = core.getAtoms();
		mAtomParity = atomParity;
		mCoreToQueryAtom = coreToQueryAtom;
		mScaffoldGroup = scaffoldGroup;
		mEmptySubstituentSeen = new boolean[core.getAtoms()];
		mSubstituentVaries = new boolean[core.getAtoms()];
		mConstantSubstituent = new String[core.getAtoms()];
		mCoreAtomToRGroupNo = new int[core.getAtoms()];
		mOldToNewMap = new TreeMap<>();
		mBridgeAtomRGroupCount = -1;
		}

	public StereoMolecule getCoreStructure() {
		return mCore;
		}

	public int getCoreAtomCount() {
		return mCoreAtomCount;
		}

	public String getIDCodeWithRGroups() {
		return mIDCodeWithRGroups;
		}

	public String getIDCoordsWithRGroups() {
		return mIDCoordsWithRGroups;
		}

	public int[] getAtomParities() {
		return mAtomParity;
		}

	public TreeMap<String,String> getOldToNewMap() {
		return mOldToNewMap;
		}

	/**
	 * Checks, whether the coreAtom that carries the substituent is part of a bridge bond
	 * or whether it is shared by the entire scaffold group. In the first case it is checked,
	 * whether the substituent was not yet seen on the level of the this CoreInfo.
	 * In the second case it is checked, whether the substituent was not yet seen on the level
	 * of the scaffold group, i.e. all CoreInfos belonging to the same scaffold query.
	 * @param substituent
	 * @param coreAtom
	 */
	public void checkSubstituent(String substituent, int coreAtom) {
		int queryAtom = mCoreToQueryAtom[coreAtom];
		if (queryAtom != -1) {
			mScaffoldGroup.checkSubstituent(substituent, queryAtom);
			}
		else {
			if (!mSubstituentVaries[coreAtom]) {
				if (substituent == null) {
					mEmptySubstituentSeen[coreAtom] = true;
					if (mConstantSubstituent[coreAtom] != null)
						mSubstituentVaries[coreAtom] = true;
					}
				else {
					if (mEmptySubstituentSeen[coreAtom])
						mSubstituentVaries[coreAtom] = true;
					else if (mConstantSubstituent[coreAtom] == null)
						mConstantSubstituent[coreAtom] = substituent;
					else if (!mConstantSubstituent[coreAtom].equals(substituent))
						mSubstituentVaries[coreAtom] = true;
					}
				}
			}
		}

	/**
	 * Checks, whether the coreAtom that carries the substituent is part of a bridge bond
	 * or whether it is shared by the entire scaffold group. In the first case the substituent
	 * variation analysis result of this local CoreInfo is returned. In the second case
	 * the substituent variation analysis result of the entire scaffold group is returned.
	 * @param coreAtom
	 */
	public boolean substituentVaries(int coreAtom) {
		int queryAtom = mCoreToQueryAtom[coreAtom];
		if (queryAtom != -1)
			return mScaffoldGroup.substituentVaries(queryAtom);
		else
			return mSubstituentVaries[coreAtom];
		}

	public String getConstantSubstituent(int coreAtom) {
		int queryAtom = mCoreToQueryAtom[coreAtom];
		if (queryAtom != -1)
			return mScaffoldGroup.getConstantSubstituent(queryAtom);
		else
			return mConstantSubstituent[coreAtom];
		}

	public int assignRGroupsToBridgeAtoms(int firstRGroup) {
		if (mBridgeAtomRGroupCount == -1) {
			mBridgeAtomRGroupCount = 0;
			for (int coreAtom=0; coreAtom<mCore.getAtoms(); coreAtom++) {
				int queryAtom = mCoreToQueryAtom[coreAtom];
				if (queryAtom != -1) {
					mCoreAtomToRGroupNo[coreAtom] = mScaffoldGroup.getRGroupNo(queryAtom);
					}
				else {
					if (mSubstituentVaries[coreAtom])
						mCoreAtomToRGroupNo[coreAtom] = firstRGroup + ++mBridgeAtomRGroupCount;
					}
				}
			}
		return mBridgeAtomRGroupCount;
		}

	/**
	 * @param coreAtom
	 * @return 1-based R-group number
	 */
	public int getRGroupNo(int coreAtom) {
		return mCoreAtomToRGroupNo[coreAtom];
		}

	public int getRGroupCount() {
		return mScaffoldGroup.getRGroupCount() + mBridgeAtomRGroupCount;
		}

	public void buildIDCodeAndCoords() {
		new CoordinateInventor(MODE_PREFER_MARKED_ATOM_COORDS).invent(mCore);	// creates stereo bonds from parities

		Canonizer canonizer = new Canonizer(mCore);
		mIDCodeWithRGroups = canonizer.getIDCode();
		mIDCoordsWithRGroups = canonizer.getEncodedCoordinates();
		}
	}

/**
 * Contains all CoreInfo objects created by the same query structure
 */
class ScaffoldGroup extends ArrayList<CoreInfo> {
	private int mRGroupCount,mQueryAtomCount;
	private int[] mQueryAtomToRGroupNo;
	private boolean[] mSubstituentVaries,mEmptySubstituentSeen;
	private String[] mConstantSubstituent;

	public ScaffoldGroup(StereoMolecule query) {
		super();
		mQueryAtomCount = query.getAtoms();
		mEmptySubstituentSeen = new boolean[mQueryAtomCount];
		mSubstituentVaries = new boolean[mQueryAtomCount];
		mConstantSubstituent = new String[mQueryAtomCount];
		mQueryAtomToRGroupNo = new int[mQueryAtomCount];
		mRGroupCount = -1;
		}

	public int getRGroupCount() {
		return mRGroupCount;
		}

	/**
	 * @param queryAtom
	 * @return 1-based R-group number
	 */
	public int getRGroupNo(int queryAtom) {
		return mQueryAtomToRGroupNo[queryAtom];
		}

	public int assignRGroupsToAtoms() {
		if (mRGroupCount == -1) {
			mRGroupCount = 0;
			for (int i=0; i<mQueryAtomCount; i++)
				if (mSubstituentVaries[i])
					mQueryAtomToRGroupNo[i] = ++mRGroupCount;
			}
		return mRGroupCount;
		}

	/**
	 * Checks for the entire scaffold group, whether the substituent was not yet seen
	 * at the given queryAtom to determine, whether the substitution on the query atoms
	 * varies throughout rows belomging to this scaffold group.
	 * @param substituent
	 * @param queryAtom
	 */
	public void checkSubstituent(String substituent, int queryAtom) {
		if (!mSubstituentVaries[queryAtom]) {
			if (substituent == null) {
				mEmptySubstituentSeen[queryAtom] = true;
				if (mConstantSubstituent[queryAtom] != null)
					mSubstituentVaries[queryAtom] = true;
			}
			else {
				if (mEmptySubstituentSeen[queryAtom])
					mSubstituentVaries[queryAtom] = true;
				else if (mConstantSubstituent[queryAtom] == null)
					mConstantSubstituent[queryAtom] = substituent;
				else if (!mConstantSubstituent[queryAtom].equals(substituent))
					mSubstituentVaries[queryAtom] = true;
				}
			}
		}

	/**
	 * @param queryAtom
	 * @return whether different substituent have been seen at queryAtom within the entire scaffold group
	 */
	public boolean substituentVaries(int queryAtom) {
		return mSubstituentVaries[queryAtom];
	}

	public String getConstantSubstituent(int queryAtom) {
		return mConstantSubstituent[queryAtom];
	}
}