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
			sb.append(new Canonizer(mScaffoldModel.getMolecule(i)).getIDCode());
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
				showErrorMessage("Some of the scaffold structures are not valid:\n"+e.toString());
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

		String[] scaffoldIDCode = configuration.getProperty(PROPERTY_SCAFFOLD_LIST, "").split("\\t");
		for (String idcode:scaffoldIDCode) {
try {   // TODO remove
			StereoMolecule scaffoldMol = new IDCodeParser(true).getCompactMolecule(idcode);
			if (!processScaffold(scaffoldMol, distinguishStereoCenters, coreInfo))
				notFoundCount++;
} catch (Exception e) { e.printStackTrace(); }
			}

		if (notFoundCount == scaffoldIDCode.length && isInteractive()) {
			final String message = "None of your scaffolds was found in in the '"+getTableModel().getColumnTitle(getChemistryColumn())+"' column.";
			showInteractiveTaskMessage(message, JOptionPane.INFORMATION_MESSAGE);
			return false;
			}

		int substituentCount = 0;
		for (int row=0; row<rowCount; row++)
			if (coreInfo[row] != null)
				substituentCount = Math.max(substituentCount, coreInfo[row].getSubstituentCount());

		if (substituentCount == 0) {
			mSubstituent = null;
			}
		else {
			for (int row=0; row<rowCount; row++) {
				if (mSubstituent[row] != null) {
					String[] newSubstituent = new String[substituentCount];
					for (int i=0; coreInfo[row] != null && i<coreInfo[row].getScaffoldAtomCount(); i++)
						if (coreInfo[row].getVaryingSubstituentIndex(i) != -1)
							newSubstituent[coreInfo[row].getVaryingSubstituentIndex(i)] = mSubstituent[row][i];
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
	 * @param scaffoldMol
	 * @param distinguishStereoCenters
	 * @return false if the scaffold could not be found in any row
	 */
	private boolean processScaffold(StereoMolecule scaffoldMol, boolean distinguishStereoCenters, CoreInfo[] coreInfoOfRow) {
		SSSearcherWithIndex searcher = new SSSearcherWithIndex();
		searcher.setFragment(scaffoldMol, (long[])null);

		scaffoldMol.ensureHelperArrays(Molecule.cHelperNeighbours);

		TreeMap<String,CoreInfo> coreMap = new TreeMap<>();

		mMultipleMatches = 0;

		startProgress("Analyzing substituents...", 0, getTableModel().getTotalRowCount());

        int coordinateColumn = getTableModel().getChildColumn(getChemistryColumn(), CompoundTableModel.cColumnType2DCoordinates);
        int fingerprintColumn = getTableModel().getChildColumn(getChemistryColumn(), DescriptorConstants.DESCRIPTOR_FFP512.shortName);
		StereoMolecule fragment = new StereoMolecule();
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
					if (matchCount > 1)
						mMultipleMatches++;

					int[] scaffoldToMolAtom = searcher.getMatchList().get(0);

					byte[] coords = (byte[])getTableModel().getTotalRecord(row).getData(coordinateColumn);
					StereoMolecule mol = new IDCodeParser(true).getCompactMolecule(idcode, coords);

						// store original fragment atom numbers incremented by 1 in atomMapNo
					for (int i=0; i<scaffoldToMolAtom.length; i++)
						if (scaffoldToMolAtom[i] != -1)
							mol.setAtomMapNo(scaffoldToMolAtom[i], i+1, false);

						// mark all atoms belonging to core fragment
					boolean[] isCoreAtom = new boolean[mol.getAllAtoms()];
					for (int i=0; i<scaffoldToMolAtom.length; i++)
						if (scaffoldToMolAtom[i] != -1)
							isCoreAtom[scaffoldToMolAtom[i]] = true;

					String extendedCoreIDCode = null;
					int[] coreAtomParity = null;
					if (distinguishStereoCenters) {
						boolean[] isExtendedCoreAtom = new boolean[mol.getAllAtoms()];	// core plus direct neighbours
						for (int i=0; i<scaffoldToMolAtom.length; i++) {
							int atom = scaffoldToMolAtom[i];
							if (atom != -1) {
								isExtendedCoreAtom[atom] = true;
								for (int j=0; j<mol.getConnAtoms(atom); j++)
									isExtendedCoreAtom[mol.getConnAtom(atom, j)] = true;
								}
							}

						StereoMolecule extendedCore = new StereoMolecule();	// core plus direct neighbours
						mol.copyMoleculeByAtoms(extendedCore, isExtendedCoreAtom, true, null);

							// change atomicNo of non-core atoms to 'R1'
						for (int atom=0; atom<extendedCore.getAllAtoms(); atom++)
							if (extendedCore.getAtomMapNo(atom) == 0)
								extendedCore.setAtomicNo(atom, 142);	// 'R1'

						extendedCore.ensureHelperArrays(Molecule.cHelperParities);

						boolean stereoCenterFound = false;
						coreAtomParity = new int[scaffoldToMolAtom.length];
						byte[] parityByte = new byte[scaffoldToMolAtom.length];
						for (int atom=0; atom<extendedCore.getAllAtoms(); atom++) {
							int scaffoldAtomNo = extendedCore.getAtomMapNo(atom) - 1;
							if (scaffoldAtomNo != -1) {
								if (extendedCore.isAtomStereoCenter(atom)) {
									int atomParity = extendedCore.getAtomParity(atom);
									coreAtomParity[scaffoldAtomNo] = atomParity;
									parityByte[scaffoldAtomNo] = (byte)('0'+atomParity);
	                                if (atomParity != Molecule.cAtomParityNone)
	                                    stereoCenterFound = true;
									if (atomParity == Molecule.cAtomParity1
									 || atomParity == Molecule.cAtomParity2) {
	                                    int esrType = extendedCore.getAtomESRType(atom);
	                                    if (esrType != Molecule.cESRTypeAbs) {
	                                        int esrEncoding = (extendedCore.getAtomESRGroup(atom) << 4)
	                                                        + ((esrType == Molecule.cESRTypeAnd) ? 4 : 8);
	                                        parityByte[scaffoldAtomNo] += esrEncoding;
	                                        coreAtomParity[scaffoldAtomNo] += esrEncoding;
	                                        }
	                                    }
									}
								}
							}
                        if (!stereoCenterFound)
	                        coreAtomParity = null;
                        else
                        	extendedCoreIDCode = new Canonizer(extendedCore).getIDCode();
						}

					StereoMolecule core = new StereoMolecule();
					int[] molToCoreAtom = new int[isCoreAtom.length];

					mol.copyMoleculeByAtoms(core, isCoreAtom, true, molToCoreAtom);
					for (int atom=0; atom<scaffoldMol.getAllAtoms(); atom++) {
						if (scaffoldToMolAtom[atom] != -1) {
							int coreAtom = molToCoreAtom[scaffoldToMolAtom[atom]];
							core.setAtomX(coreAtom, scaffoldMol.getAtomX(atom));
							core.setAtomY(coreAtom, scaffoldMol.getAtomY(atom));
							core.setAtomMarker(coreAtom, true);  // to keep the original scaffold coordinates
							}
						}

					core.setFragment(false);
					core.stripStereoInformation();
					String coreIDCode = (extendedCoreIDCode != null) ? extendedCoreIDCode : new Canonizer(core).getIDCode();
					coreInfoOfRow[row] = coreMap.get(coreIDCode);
					if (coreInfoOfRow[row] == null) {
						coreInfoOfRow[row] = new CoreInfo(core, coreAtomParity, scaffoldToMolAtom.length);
						coreMap.put(coreIDCode, coreInfoOfRow[row]);
						}

					for (int i=0; i<scaffoldToMolAtom.length; i++) {
						int atom = scaffoldToMolAtom[i];
						if (atom != -1) {
							mol.setAtomicNo(atom, 0);
							mol.setAtomCustomLabel(atom, Integer.toString(molToCoreAtom[atom]));	// we encode the core atom index
							}
						}

					int[] workAtom = new int[mol.getAllAtoms()];
					substituentConnectsBack[row] = new boolean[scaffoldMol.getAtoms()];
					for (int i=0; i<scaffoldToMolAtom.length; i++) {
						if (scaffoldToMolAtom[i] != -1
						 && mol.getConnAtoms(scaffoldToMolAtom[i]) > scaffoldMol.getConnAtoms(i) - scaffoldMol.getExcludedNeighbourCount(i)) {
							boolean[] isSubstituentAtom = new boolean[mol.getAllAtoms()];
							isSubstituentAtom[scaffoldToMolAtom[i]] = true;
							workAtom[0] = scaffoldToMolAtom[i];
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
												substituentConnectsBack[row][i] = true;
											}
										}
									}
								current++;
								}

							fragment.clear();
							mol.setAtomCustomLabel(scaffoldToMolAtom[i], (String)null);	// no encoding for the connection atom

							mol.copyMoleculeByAtoms(fragment, isSubstituentAtom, false, null);

							mol.setAtomCustomLabel(scaffoldToMolAtom[i], Integer.toString(molToCoreAtom[scaffoldToMolAtom[i]]));	// restore encoding
							fragment.setFragment(false);

							if (!distinguishStereoCenters)
								fragment.stripStereoInformation();

								// if substituent is a ring forming bridge to the startatom
							for (int bond=fragment.getAllBonds()-1; bond>=0; bond--)
								if (fragment.getAtomicNo(fragment.getBondAtom(0, bond)) == 0
								 && fragment.getAtomicNo(fragment.getBondAtom(1, bond)) == 0)
									fragment.deleteBond(bond);

							if (mSubstituent[row] == null)
								mSubstituent[row] = new String[scaffoldToMolAtom.length];
							mSubstituent[row][i] = (highest == 0) ? null : new Canonizer(fragment, Canonizer.ENCODE_ATOM_CUSTOM_LABELS).getIDCode();
							}
						}
					}
				}
			}

		if (coreMap.isEmpty())
			return false;

		if (threadMustDie())
			return true;

		// check for varying substituents to require a new column
		for (int atom=0; atom<scaffoldMol.getAtoms(); atom++)
			for (int row=0; row<getTableModel().getTotalRowCount(); row++)
				if (mSubstituent[row] != null)
					coreInfoOfRow[row].checkSubstituent(mSubstituent[row][atom], atom);

		// remove not varying substituent columns (expected outside of method)
		for (int atom=0; atom<scaffoldMol.getAtoms(); atom++)
			for (int row=0; row<getTableModel().getTotalRowCount(); row++)
				if (mSubstituent[row] != null && !coreInfoOfRow[row].substituentVaries(atom))
					mSubstituent[row][atom] = null;

		for (CoreInfo coreInfo:coreMap.values()) {
		    StereoMolecule core = coreInfo.core;

			// create scaffoldToCoreAtom array from stored mapping numbers
			int[] scaffoldToCoreAtom = new int[scaffoldMol.getAtoms()];
			Arrays.fill(scaffoldToCoreAtom, -1);	// account for exclude atoms
			for (int atom=0; atom<core.getAtoms(); atom++)
				scaffoldToCoreAtom[core.getAtomMapNo(atom) - 1] = atom;

			int substituentNo = 0;
			boolean[] closureCovered = new boolean[core.getAtoms()];
			for (int atom=0; atom<scaffoldMol.getAtoms(); atom++) {
						//	if substituent varies => attach an R group
				if (scaffoldToCoreAtom[atom] != -1) {
					if (coreInfo.substituentVaries(atom)) {
						int newAtom = core.addAtom((substituentNo < 3) ? 142+substituentNo : 126+substituentNo);
						core.addBond(scaffoldToCoreAtom[atom], newAtom, 1);
						coreInfo.coreAtomToRNo[scaffoldToCoreAtom[atom]] = substituentNo + 1;	// first one is 1
						substituentNo++;
						}
					else {	//	else => attach the non-varying substituent (if it is not null = 'unsubstituted')
						if (!closureCovered[scaffoldToCoreAtom[atom]] && coreInfo.constantSubstituent[atom] != null) {
							StereoMolecule theSubstituent = new IDCodeParser(true).getCompactMolecule(coreInfo.constantSubstituent[atom]);

							// Substitutions, which connect back to the core fragment are encoded with labels on the connecting atoms: "core atom index".
							// Now we translate labels back to atomMapNos, which are used by addSubstituent() to create back connections.
							for (int a=0; a<theSubstituent.getAllAtoms(); a++) {
								String label = theSubstituent.getAtomCustomLabel(a);
								if (label != null) {
									int coreAtom = Integer.parseInt(label);
									theSubstituent.setAtomCustomLabel(a, (String)null);
									theSubstituent.setAtomicNo(a, 0);
									theSubstituent.setAtomMapNo(a, coreAtom+1, false);
									closureCovered[coreAtom] = true;
									}
								}
							core.addSubstituent(theSubstituent, scaffoldToCoreAtom[atom], true);
							}
						}
					}
				}

			int[] parityList = coreInfo.getAtomParities();
			if (parityList != null) {
				for (int atom=0; atom<scaffoldMol.getAtoms(); atom++) {
					int parity = parityList[atom] & 3;
                    int esrType = (parityList[atom] & 0x0C);
                    int esrGroup = (parityList[atom] & 0xF0) >> 4;
                    core.setAtomParity(scaffoldToCoreAtom[atom], parity, false);
                    if (esrType != 0) {
                        core.setAtomESR(scaffoldToCoreAtom[atom], esrType == 4 ?
                                Molecule.cESRTypeAnd : Molecule.cESRTypeOr, esrGroup);
				        }
                    }
				core.setParitiesValid(0);
                }

			new CoordinateInventor(MODE_PREFER_MARKED_ATOM_COORDS).invent(core);	// creates stereo bonds from parities

			Canonizer canonizer = new Canonizer(core);
			coreInfo.idcodeWithRGroups = canonizer.getIDCode();
			coreInfo.idcoordsWithRGroups = canonizer.getEncodedCoordinates();
			}

		encodeSubstituentRingClosures(coreInfoOfRow, substituentConnectsBack);

		for (int row=0; row<getTableModel().getTotalRowCount(); row++) {
			if (coreInfoOfRow[row] != null) {
				mScaffold[row][0] = coreInfoOfRow[row].idcodeWithRGroups;
				mScaffold[row][1] = coreInfoOfRow[row].idcoordsWithRGroups;
				}
			}

		return true;
		}

	/**
	 * The core atom index in case of substituent ring closures was encoded as label in the substituent idcode.
	 * This ensured in the check for varying substituents that chains with inverted symmetry are recognized as
	 * different substituent. After this check and once we have a mapping from scaffold atom index to R-group index,
	 * we need to exchange the label by a new one with the R-Group index, which should be finally displayed to the user.
	 * @param coreInfoOfRow
	 * @param substituentConnectsBack [scaffold atom index][row index]
	 */
	private void encodeSubstituentRingClosures(CoreInfo[] coreInfoOfRow, boolean[][] substituentConnectsBack) {
		for (int row=0; row<coreInfoOfRow.length; row++) {
			for (int scaffoldAtom=0; substituentConnectsBack[row] != null && scaffoldAtom<coreInfoOfRow[row].getScaffoldAtomCount(); scaffoldAtom++) {
				if (coreInfoOfRow[row].substituentVaries(scaffoldAtom)) {
					if (substituentConnectsBack[row][scaffoldAtom]) {
						if (mSubstituent[row][scaffoldAtom] != null) {
							String newIDCode = coreInfoOfRow[row].oldToNewMap.get(mSubstituent[row][scaffoldAtom]);
							if (newIDCode != null) {
								mSubstituent[row][scaffoldAtom] = newIDCode;
								}
							else {
								StereoMolecule s = new IDCodeParser().getCompactMolecule(mSubstituent[row][scaffoldAtom]);
								for (int atom=0; atom<s.getAllAtoms(); atom++) {
									String label = s.getAtomCustomLabel(atom);
									if (label != null)
										s.setAtomCustomLabel(atom, Integer.toString(coreInfoOfRow[row].coreAtomToRNo[Integer.parseInt(label)]));
									}
								newIDCode = new Canonizer(s, Canonizer.ENCODE_ATOM_CUSTOM_LABELS).getIDCode();
								coreInfoOfRow[row].oldToNewMap.put(mSubstituent[row][scaffoldAtom], newIDCode);
								mSubstituent[row][scaffoldAtom] = newIDCode;
								}
							}
						}
					}
				}
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
	private int scaffoldAtomCount;
	public StereoMolecule core;
	private boolean[] substituentVaries,emptySubstituentSeen;
	private int[] atomParity,scaffoldToVaryingSubstituentIndex;
	String idcodeWithRGroups;
	String idcoordsWithRGroups;
	String[] constantSubstituent;
	int[] coreAtomToRNo;
	TreeMap<String,String> oldToNewMap;
	private int substituentCount = -1;

	public CoreInfo(StereoMolecule core, int[] atomParity, int scaffoldAtomCount) {
		this.core = core;
		this.atomParity = atomParity;
		this.scaffoldAtomCount = scaffoldAtomCount;
		this.emptySubstituentSeen = new boolean[scaffoldAtomCount];
		this.substituentVaries = new boolean[scaffoldAtomCount];
		this.constantSubstituent = new String[scaffoldAtomCount];
		this.coreAtomToRNo = new int[core.getAtoms()];
		this.oldToNewMap = new TreeMap<>();
		}

	public int getScaffoldAtomCount() {
		return scaffoldAtomCount;
		}

	public int[] getAtomParities() {
		return atomParity;
		}

	public boolean substituentVaries(int scaffoldAtom) {
		return substituentVaries[scaffoldAtom];
		}

	public void checkSubstituent(String substituent, int scaffoldAtom) {
		if (!substituentVaries[scaffoldAtom]) {
			if (substituent == null) {
				emptySubstituentSeen[scaffoldAtom] = true;
				if (constantSubstituent[scaffoldAtom] != null)
					substituentVaries[scaffoldAtom] = true;
				}
			else {
				if (emptySubstituentSeen[scaffoldAtom])
					substituentVaries[scaffoldAtom] = true;
				else if (constantSubstituent[scaffoldAtom] == null)
					constantSubstituent[scaffoldAtom] =	substituent;
				else if (!constantSubstituent[scaffoldAtom].equals(substituent))
						substituentVaries[scaffoldAtom] = true;
				}
			}
		}

	public int getSubstituentCount() {
		if (substituentCount == -1) {
			scaffoldToVaryingSubstituentIndex = new int[scaffoldAtomCount];
			substituentCount = 0;
			for (int i = 0; i < scaffoldAtomCount; i++) {
				if (substituentVaries[i])
					scaffoldToVaryingSubstituentIndex[i] = substituentCount++;
				else
					scaffoldToVaryingSubstituentIndex[i] = -1;
				}
		 	}
		return substituentCount;
		}

	public int getVaryingSubstituentIndex(int scaffoldAtom) {
		return scaffoldToVaryingSubstituentIndex[scaffoldAtom];
		}
	};