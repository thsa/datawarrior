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
import com.actelion.research.chem.coords.InventorTemplate;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.gui.CompoundCollectionModel;
import com.actelion.research.gui.CompoundCollectionPane;
import com.actelion.research.gui.DefaultCompoundCollectionModel;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TreeMap;


public class DETaskAdd2DCoordinates extends DETaskAbstractFromStructure implements ActionListener,Runnable {
	public static final String TASK_NAME = "Generate 2D-Atom-Coordinates";

	private static final String PROPERTY_SCAFFOLD_LIST = "scaffolds";
	private static final String PROPERTY_AUTOMATIC = "automatic";
	private static final String PROPERTY_SCAFFOLD_MODE = "scaffoldMode";
	private static final String PROPERTY_COLORIZE_SCAFFOLDS = "colorizeScaffolds";

	private static final int SCAFFOLD_CENTRAL_RING = 0;
	private static final int SCAFFOLD_MURCKO = 1;

	private static final String SCAFFOLD_COLOR = "orange:";

	private static final String[] SCAFFOLD_TEXT = { "Most central ring system", "Murcko scaffolds" };
	private static final String[] SCAFFOLD_CODE = { "centralRing", "murcko" };

	private CompoundCollectionPane<String>	mStructurePane;
	private JCheckBox						mCheckBoxAutomatic;
	private JComboBox						mComboBoxScaffoldMode;
	private JCheckBox                       mCheckBoxColorizeAtoms;
	private ArrayList<InventorTemplate>		mExplicitScaffoldList;
	private TreeMap<String,StereoMolecule>  mImplicitScaffoldMap;
	private int								mCoordinateColumn,mFFPColumn,mScaffoldMode,mIDCodeErrors;
	private byte[][]                        mScaffoldAtoms;
	private StringBuilder                   mScaffoldColorBuilder;
	private StereoMolecule					mScaffoldContainer;

	public DETaskAdd2DCoordinates(DEFrame parent) {
		super(parent, DESCRIPTOR_NONE, false, false);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/chemistry.html#Add2DCoords";
		}

	@Override
	public boolean hasExtendedDialogContent() {
		return true;
		}

	@Override
	public JPanel getExtendedDialogContent() {
		JPanel ep = new JPanel();
		int unit = HiDPIHelper.scale(24);
		double[][] size = { {20*unit},
							{TableLayout.PREFERRED, 4, 4*unit, unit, TableLayout.PREFERRED,
							 TableLayout.PREFERRED, unit/2, TableLayout.PREFERRED} };
		
		ep.setLayout(new TableLayout(size));
		ep.add(new JLabel("Enforce atom coordinates for these scaffolds:"), "0,0");

		mStructurePane = new CompoundCollectionPane<String>(new DefaultCompoundCollectionModel.IDCode(), false);
		mStructurePane.setEditable(true);
		mStructurePane.setClipboardHandler(new ClipboardHandler());
		mStructurePane.setShowValidationError(true);
		mStructurePane.setCreateFragments(true);
		ep.add(mStructurePane, "0,2");

		mCheckBoxAutomatic = new JCheckBox("Automatically detect scaffolds and unify their orientation");
		mCheckBoxAutomatic.addActionListener(this);
		ep.add(mCheckBoxAutomatic, "0,4");

		JPanel tp = new JPanel();
		tp.add(new JLabel("Scaffold detection method: "));
		mComboBoxScaffoldMode = new JComboBox(SCAFFOLD_TEXT);
		tp.add(mComboBoxScaffoldMode);
		ep.add(tp, "0,5");

		mCheckBoxColorizeAtoms = new JCheckBox("Show scaffolds by colorizing atoms");
		ep.add(mCheckBoxColorizeAtoms, "0,7");

		enableItems();

		return ep;
		}

	private void enableItems() {
		mComboBoxScaffoldMode.setEnabled(mCheckBoxAutomatic.isSelected());
		mCheckBoxColorizeAtoms.setEnabled(mStructurePane.getModel().getSize() != 0 || mCheckBoxAutomatic.isSelected());
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mCheckBoxAutomatic) {
			enableItems();
			return;
			}

		super.actionPerformed(e);
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		CompoundCollectionModel<String> model = mStructurePane.getModel();
		if (model.getSize() != 0) {
			StringBuilder sb = new StringBuilder();
			for (int i=0; i<model.getSize(); i++) {
				sb.append(model.getCompound(i));
				sb.append('\t');
				}
			configuration.put(PROPERTY_SCAFFOLD_LIST, sb.toString());
			}
		configuration.setProperty(PROPERTY_AUTOMATIC, mCheckBoxAutomatic.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_SCAFFOLD_MODE, SCAFFOLD_CODE[mComboBoxScaffoldMode.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_COLORIZE_SCAFFOLDS, mCheckBoxColorizeAtoms.isSelected() ? "true" : "false");
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);

		mStructurePane.getModel().clear();
		String scaffolds = configuration.getProperty(PROPERTY_SCAFFOLD_LIST, "");
		if (scaffolds.length() != 0) {
			String[] idcodeList = scaffolds.split("\\t");
			for (String idcode:idcodeList)
				mStructurePane.getModel().addCompound(idcode);
			}

		mCheckBoxAutomatic.setSelected(!"false".equals(configuration.getProperty(PROPERTY_AUTOMATIC)));
		mComboBoxScaffoldMode.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_SCAFFOLD_MODE), SCAFFOLD_CODE, SCAFFOLD_CENTRAL_RING));

		mCheckBoxColorizeAtoms.setSelected("true".equals(configuration.getProperty(PROPERTY_COLORIZE_SCAFFOLDS)));

		enableItems();
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mStructurePane.getModel().clear();
		mCheckBoxAutomatic.setSelected(true);
		mComboBoxScaffoldMode.setSelectedIndex(SCAFFOLD_CENTRAL_RING);
		mCheckBoxColorizeAtoms.setSelected(false);
		enableItems();
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (!super.isConfigurationValid(configuration, isLive))
			return false;

		return true;
		}

	@Override
	protected int getNewColumnCount() {
		return (mCoordinateColumn == -1) ? 1 : 0;
		}

	@Override
	protected void setNewColumnProperties(int firstNewColumn) {
		getTableModel().setColumnProperty(firstNewColumn,
				CompoundTableModel.cColumnPropertySpecialType,
				CompoundTableModel.cColumnType2DCoordinates);
		getTableModel().setColumnProperty(firstNewColumn,
				CompoundTableModel.cColumnPropertyParentColumn,
				getTableModel().getColumnTitleNoAlias(getChemistryColumn()));
		}

	@Override
	protected String getNewColumnName(int column) {
		return "2D-"+getTableModel().getColumnTitle(getChemistryColumn());
		}

	@Override
	protected boolean preprocessRows(Properties configuration) {
		mIDCodeErrors = 0;

		int idcodeColumn = getChemistryColumn();
		mCoordinateColumn = getTableModel().getChildColumn(idcodeColumn, CompoundTableConstants.cColumnType2DCoordinates);

		String scaffolds = configuration.getProperty(PROPERTY_SCAFFOLD_LIST, "");
		if (scaffolds.length() != 0) {
			mExplicitScaffoldList = new ArrayList<>();
			SSSearcherWithIndex ffpCreator = new SSSearcherWithIndex();
			String[] idcodeList = scaffolds.split("\\t");
			for (int i=0; i<idcodeList.length; i++) {
				StereoMolecule scaffold = new IDCodeParser().getCompactMolecule(idcodeList[i]);
				ensureMatchEZParityQueryFeatures(scaffold);
				long[] ffp = ffpCreator.createLongIndex(scaffold);
				mExplicitScaffoldList.add(new InventorTemplate(scaffold, ffp, true));
				}
			}

		if ("true".equals(configuration.getProperty(PROPERTY_AUTOMATIC))) {
			mImplicitScaffoldMap = new TreeMap<>();
			mScaffoldContainer = new StereoMolecule();
			mScaffoldMode = findListIndex(configuration.getProperty(PROPERTY_SCAFFOLD_MODE), SCAFFOLD_CODE, SCAFFOLD_CENTRAL_RING);
			}

		if (mExplicitScaffoldList != null || mScaffoldContainer != null) {
			mFFPColumn = getTableModel().getChildColumn(idcodeColumn, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
			}

		if ("true".equals(configuration.getProperty(PROPERTY_COLORIZE_SCAFFOLDS))) {
			mScaffoldAtoms = new byte[getTableModel().getTotalRowCount()][];
			mScaffoldColorBuilder = new StringBuilder();
			}

		return true;
		}

	private void ensureMatchEZParityQueryFeatures(StereoMolecule scaffold) {
		scaffold.ensureHelperArrays(Molecule.cHelperParities);
		for (int bond=0; bond<scaffold.getBonds(); bond++)
			if (scaffold.getBondParity(bond) == Molecule.cBondParityEor1
			 || scaffold.getBondParity(bond) == Molecule.cBondParityZor2)
				scaffold.setBondQueryFeature(bond, Molecule.cBondQFMatchStereo, true);
		}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol, int threadIndex) {
		int coordinateColumn = (mCoordinateColumn != -1) ? mCoordinateColumn : firstNewColumn;
		StereoMolecule mol = getChemicalStructure(row, containerMol);
		if (mol != null && mol.getAllAtoms() != 0) {
			String freshIDCode = new Canonizer(mol).getIDCode();

			boolean found = false;
			if (mExplicitScaffoldList != null) {
				mol.ensureHelperArrays(Molecule.cHelperParities);

				if (mFFPColumn == -1) {
					SSSearcher searcher = new SSSearcher();
					searcher.setMolecule(mol);
					for (InventorTemplate s:mExplicitScaffoldList) {
						searcher.setFragment(s.getFragment());
						if (searcher.findFragmentInMolecule(SSSearcher.cCountModeFirstMatch, SSSearcher.cDefaultMatchMode) != 0) {
							found = true;
							break;
							}
						}
					}
				else {
					SSSearcherWithIndex searcher = new SSSearcherWithIndex();
					searcher.setMolecule(mol, (long[])getTableModel().getTotalRecord(row).getData(mFFPColumn));
					for (InventorTemplate s:mExplicitScaffoldList) {
						searcher.setFragment(s.getFragment(), s.getFFP());
						if (searcher.findFragmentInMolecule(SSSearcher.cCountModeFirstMatch, SSSearcher.cDefaultMatchMode) != 0) {
							found = true;
							break;
							}
						}
					}

				if (found) {
					CoordinateInventor inventor = new CoordinateInventor();
					inventor.setCustomTemplateList(mExplicitScaffoldList);
					inventor.invent(mol);

					if (mScaffoldAtoms != null) {
						mScaffoldColorBuilder.setLength(0);
						boolean[] isScaffoldAtom = inventor.getCustomTemplateAtomMask();
						for (int atom=0; atom<isScaffoldAtom.length; atom++) {
							if (isScaffoldAtom[atom]) {
								if (mScaffoldColorBuilder.length()==0) {
									mScaffoldColorBuilder.append(SCAFFOLD_COLOR);
									}
								else {
									mScaffoldColorBuilder.append(',');
									}
								mScaffoldColorBuilder.append(atom);
								}
							}
						mScaffoldAtoms[row] = mScaffoldColorBuilder.toString().getBytes();
						}
					}
				}

			if (mImplicitScaffoldMap != null && !found) {
				StereoMolecule strippedMol = mol.getCompactCopy();

				int[] atomToStrippedAtom = strippedMol.stripSmallFragments();
				if (atomToStrippedAtom == null) {
					atomToStrippedAtom = new int[strippedMol.getAllAtoms()];
					for (int i=0; i<atomToStrippedAtom.length; i++)
						atomToStrippedAtom[i] = i;
					}

				boolean[] isCoreAtom = (mScaffoldMode == SCAFFOLD_MURCKO) ?
						ScaffoldHelper.findMurckoScaffold(strippedMol) : ScaffoldHelper.findMostCentralRingSystem(strippedMol);
				if (isCoreAtom != null) {
					int[] strippedAtomToScaffoldAtom = new int[strippedMol.getAllAtoms()];
					strippedMol.copyMoleculeByAtoms(mScaffoldContainer, isCoreAtom, true, strippedAtomToScaffoldAtom);
					Canonizer canonizer = new Canonizer(mScaffoldContainer);
					String idcode = canonizer.getIDCode();
					if (!mImplicitScaffoldMap.containsKey(idcode)) {
						new CoordinateInventor().invent(mScaffoldContainer);
						mImplicitScaffoldMap.put(idcode, canonizer.getCanMolecule());

						int[] scaffoldAtomToAtom = new int[mScaffoldContainer.getAtoms()];
						for (int atom=0; atom<mol.getAllAtoms(); atom++) {
							if (atomToStrippedAtom[atom] != -1) {
								int scaffoldAtom = strippedAtomToScaffoldAtom[atomToStrippedAtom[atom]];
								if (scaffoldAtom != -1)
									scaffoldAtomToAtom[scaffoldAtom] = atom;
								}
							}
						updateCoords(mol, mScaffoldContainer, scaffoldAtomToAtom, row);
						found = true;
						}
					else {
						int[] canonicalAtomToScaffoldAtom = canonizer.getGraphIndexes();
						int[] canonicalAtomToAtom = new int[mScaffoldContainer.getAtoms()];
						for (int atom=0; atom<mol.getAllAtoms(); atom++) {
							if (atomToStrippedAtom[atom] != -1) {
								int scaffoldAtom = strippedAtomToScaffoldAtom[atomToStrippedAtom[atom]];
								if (scaffoldAtom != -1)
									canonicalAtomToAtom[canonicalAtomToScaffoldAtom[scaffoldAtom]] = atom;
								}
							}

						StereoMolecule scaffold = mImplicitScaffoldMap.get(idcode);
						updateCoords(mol, scaffold, canonicalAtomToAtom, row);
						found = true;
						}
					}
				}

			if (!found) {
				mol.ensureHelperArrays(Molecule.cHelperParities);
				new CoordinateInventor().invent(mol);
				}

			Canonizer canonizer = new Canonizer(mol);
			if (!canonizer.getIDCode().equals(getTableModel().getTotalValueAt(row, getChemistryColumn()))) {
				boolean idcodesDiffer = !freshIDCode.equals(getTableModel().getTotalValueAt(row, getChemistryColumn()));
				if (!idcodesDiffer)
					mIDCodeErrors++;

				if (System.getProperty("development") != null) {
					if (idcodesDiffer) {
						System.out.println("ERROR: idcodes before 2D-coordinate generation differ!!!");
						System.out.println(" file: " + getTableModel().getTotalValueAt(row, getChemistryColumn()));
						System.out.println("fresh: " + freshIDCode);
						}
					else {
						System.out.println("WARNING: idcodes after 2D-coordinate generation differ!!!");
						System.out.println("old: " + getTableModel().getTotalValueAt(row, getChemistryColumn()));
						System.out.println("new: " + canonizer.getIDCode());
						}
					}
				}
			else {	// don't change coordinates if idcode from new coordinates doesn't match old one
				getTableModel().setTotalValueAt(canonizer.getEncodedCoordinates(true), row, coordinateColumn);
				}
			}
		else {
			getTableModel().setTotalValueAt(null, row, coordinateColumn);
			}
		}

	private void updateCoords(StereoMolecule mol, StereoMolecule scaffold, int[] scaffoldAtomToAtom, int row) {
		mol.ensureHelperArrays(Molecule.cHelperParities);
		for (int atom=0; atom<scaffold.getAllAtoms(); atom++) {
			mol.setAtomX(scaffoldAtomToAtom[atom], scaffold.getAtomX(atom));
			mol.setAtomY(scaffoldAtomToAtom[atom], scaffold.getAtomY(atom));
			mol.setAtomMarker(scaffoldAtomToAtom[atom], true);
			}
		new CoordinateInventor(CoordinateInventor.MODE_PREFER_MARKED_ATOM_COORDS).invent(mol);

		if (mScaffoldAtoms != null) {
			mScaffoldColorBuilder.setLength(0);
			mScaffoldColorBuilder.append(SCAFFOLD_COLOR);
			for (int i=0; i<scaffoldAtomToAtom.length; i++) {
				mScaffoldColorBuilder.append(i==0 ? ':' : ',');
				mScaffoldColorBuilder.append(scaffoldAtomToAtom[i]);
				}
			mScaffoldAtoms[row] = mScaffoldColorBuilder.toString().getBytes();
			}
		}

	@Override
	protected void postprocess(int firstNewColumn) {
		if (mCoordinateColumn != -1)	// we use an existing column
			getTableModel().finalizeChangeChemistryColumn(getTableModel().getParentColumn(mCoordinateColumn),
					0, getTableModel().getTotalRowCount(), false);

		if (mScaffoldAtoms != null) {
			final String[] columnName = { "colorInfo" };
			int colorColumn = getTableModel().addNewColumns(columnName);
			getTableModel().setColumnProperty(colorColumn, CompoundTableConstants.cColumnPropertySpecialType,
					CompoundTableConstants.cColumnTypeAtomColorInfo);
			getTableModel().setColumnProperty(colorColumn, CompoundTableConstants.cColumnPropertyParentColumn,
					getTableModel().getColumnTitleNoAlias(getChemistryColumn()));
			for (int row=0; row<getTableModel().getTotalRowCount(); row++)
				getTableModel().setTotalDataAt(mScaffoldAtoms[row], row, colorColumn);
			getTableModel().finalizeNewColumns(colorColumn, this);
			}

		if (isInteractive() && mIDCodeErrors != 0)
			showInteractiveTaskMessage("Coordinates were not changed for "+mIDCodeErrors
					+" structures, because original stereo configurations would have been changed.", JOptionPane.INFORMATION_MESSAGE);
		}
	}
