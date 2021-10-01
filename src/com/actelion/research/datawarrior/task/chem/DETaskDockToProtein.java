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
import com.actelion.research.chem.docking.DockingEngine;
import com.actelion.research.chem.docking.DockingFailedException;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.io.pdb.parser.PDBFileParser;
import com.actelion.research.chem.io.pdb.parser.StructureAssembler;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;
import org.openmolecules.chem.conf.gen.ConformerGenerator;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class DETaskDockToProtein extends DETaskAbstractFromStructure {
	public static final String TASK_NAME = "Dock To Protein";

	private static final String[] COLUMN_TITLE = {"Docking Score", "Docking Pose", "poseCoordinates"};
	private static final int SCORE_COLUMN = 0;
	private static final int POSE_COLUMN = 1;
	private static final int COORDS_COLUMN = 2;

	private static final String PROPERTY_NEUTRALIZE = "neutralize";

	private static final double CROP_DISTANCE = 10.0;

	private JCheckBox mCheckBoxNeutralize;
	private File mInputFile;
	private boolean mNeutralizeFragment;
	private Molecule3D mProtein;
	private StereoMolecule mLigand;
	private DockingEngine[] mDockingEngine;
	private String mCroppedProteinIDCode;
	private Coordinates mCroppedProteinCOG;

	public DETaskDockToProtein(DEFrame parent) {
		super(parent, DESCRIPTOR_NONE, false, true);
	}

	@Override
	protected int getNewColumnCount() {
		return 3;
		}

	@Override
	protected String getNewColumnName(int column) {
		return COLUMN_TITLE[column];
		}

	@Override
	public boolean hasExtendedDialogContent() {
		return true;
	}

	@Override
	public JPanel getExtendedDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {TableLayout.PREFERRED}, {TableLayout.PREFERRED} };

		mCheckBoxNeutralize = new JCheckBox("Neutralize charges");

		JPanel ep = new JPanel();
		ep.setLayout(new TableLayout(size));
		ep.add(mCheckBoxNeutralize, "0,0");
		return ep;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public String getHelpURL() {
		return null /* "/html/help/chemistry.html#docking" */;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_NEUTRALIZE, mCheckBoxNeutralize.isSelected() ? "true" : "false");
		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mCheckBoxNeutralize.setSelected("true".equals(configuration.getProperty(PROPERTY_NEUTRALIZE)));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mCheckBoxNeutralize.setSelected(true);
		}

	@Override
	protected void setNewColumnProperties(int firstNewColumn) {
		getTableModel().setColumnProperty(firstNewColumn+POSE_COLUMN,
				CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnTypeIDCode);
		getTableModel().setColumnProperty(firstNewColumn+COORDS_COLUMN,
				CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnType3DCoordinates);
		getTableModel().setColumnProperty(firstNewColumn+COORDS_COLUMN,
				CompoundTableConstants.cColumnPropertyParentColumn, COLUMN_TITLE[POSE_COLUMN]);
		getTableModel().setColumnProperty(firstNewColumn+COORDS_COLUMN,
				CompoundTableConstants.cColumnPropertySuperposeMolecule, mCroppedProteinIDCode);
		}

	private Coordinates calculateCOG(StereoMolecule mol) {
		Coordinates cog = mol.getCoordinates(0);
		for (int i=1; i<mol.getAllAtoms(); i++)
			cog.add(mol.getCoordinates(i));
		cog.scale(1.0/mol.getAllAtoms());
		return cog;
		}

	private void cropAndCenterProtein() {
		Coordinates ligandCOG = calculateCOG(mLigand);
		double maxDistance = 0;
		for (int i=0; i<mLigand.getAllAtoms(); i++)
			maxDistance = Math.max(maxDistance, ligandCOG.distance(mLigand.getCoordinates(i)));

		boolean[] isInCropRadius = new boolean[mProtein.getAllAtoms()];
		for (int i=0; i<mProtein.getAllAtoms(); i++) {
			Coordinates pc = mProtein.getCoordinates(i);
			if (Math.abs(pc.x - ligandCOG.x) < maxDistance + CROP_DISTANCE
			 && Math.abs(pc.y - ligandCOG.y) < maxDistance + CROP_DISTANCE
			 && Math.abs(pc.z - ligandCOG.z) < maxDistance + CROP_DISTANCE
			 && pc.distance(ligandCOG) < maxDistance + CROP_DISTANCE) {
				for (int j=0; j<mLigand.getAllAtoms(); j++) {
					if (pc.distance(mLigand.getCoordinates(j)) < CROP_DISTANCE) {
						isInCropRadius[i] = true;
						break;
						}
					}
				}
			}

		StereoMolecule croppedProtein = new StereoMolecule();
		mProtein.copyMoleculeByAtoms(croppedProtein, isInCropRadius, false, null);
		mCroppedProteinCOG = calculateCOG(croppedProtein);
		croppedProtein.translate(-mCroppedProteinCOG.x, -mCroppedProteinCOG.y, -mCroppedProteinCOG.z);
		Canonizer canonizer = new Canonizer(croppedProtein);
		mCroppedProteinIDCode = canonizer.getIDCode()+" "+canonizer.getEncodedCoordinates();
		}

	@Override
	protected boolean preprocessRows(Properties configuration) {
		mNeutralizeFragment = "true".equals(configuration.getProperty(PROPERTY_NEUTRALIZE));
		if (!super.preprocessRows(configuration) || !loadLigandAndProtein())
			return false;

		int threadCount = Runtime.getRuntime().availableProcessors();
		mDockingEngine = new DockingEngine[threadCount];
		for (int i=0; i<threadCount; i++)
			mDockingEngine[i] = new DockingEngine(mProtein, mLigand);

		cropAndCenterProtein();

		return true;
		}

	private boolean loadLigandAndProtein() {
		mProtein = null;
		mLigand = null;
		try {
			SwingUtilities.invokeAndWait(() ->
				mInputFile = FileHelper.getFile(null, "Choose PDB-File", FileHelper.cFileTypePDB) );
			}
		catch (Exception e) {}

		if (mInputFile != null) {
			startProgress("Reading input file...", 0, 0);
			PDBFileParser parser = new PDBFileParser();
			try {
				Map<String, List<Molecule3D>> map = parser.parse(mInputFile).extractMols();
				List<Molecule3D> proteins = map.get(StructureAssembler.PROTEIN_GROUP);
				List<Molecule3D> ligands = map.get(StructureAssembler.LIGAND_GROUP);
				if (proteins == null || proteins.size() == 0) {
					System.out.println("No proteins found in file.");
					}
				else {
					mProtein = proteins.get(0);
					for (int i=1; i<proteins.size(); i++)
						mProtein.addMolecule(proteins.get(i));
					ConformerGenerator.addHydrogenAtoms(mProtein);
					}
				if (ligands == null || ligands.size() == 0) {
					System.out.println("No ligands found in file.");
					}
				else {
					mLigand = ligands.get(0);
					// we just take the first ligand for now!!!
					ConformerGenerator.addHydrogenAtoms(mLigand);
					}
				}
			catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				}
			}

		return mProtein != null && mLigand != null;
		}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol, int threadIndex) {
		CompoundRecord record = getTableModel().getTotalRecord(row);
		byte[] idcode = (byte[])record.getData(getChemistryColumn());
		if (idcode != null) {
			StereoMolecule query = getChemicalStructure(row, containerMol);
			if (query != null) {
				handleMolecule(query);

				try {
					DockingEngine.DockingResult dockingResult = mDockingEngine[threadIndex].dockMolecule(query);
					StereoMolecule pose = dockingResult.getPose();
					pose.translate(-mCroppedProteinCOG.x, -mCroppedProteinCOG.y, -mCroppedProteinCOG.z);

					Canonizer canonizer = new Canonizer(pose);
					getTableModel().setTotalValueAt(canonizer.getIDCode(), row, firstNewColumn+POSE_COLUMN);
					getTableModel().setTotalValueAt(canonizer.getEncodedCoordinates(), row, firstNewColumn+COORDS_COLUMN);
					getTableModel().setTotalValueAt(DoubleFormat.toString(dockingResult.getScore()), row, firstNewColumn+SCORE_COLUMN);
					}
				catch (DockingFailedException dfe) {
					System.out.println(dfe.getMessage());
					dfe.printStackTrace();
					}
				}
			}
		}

	private void handleMolecule(StereoMolecule mol) {
		mol.stripSmallFragments();
		if (mNeutralizeFragment)
			new MoleculeNeutralizer().neutralizeChargedMolecule(mol);

		ConformerGenerator.addHydrogenAtoms(mol);
//		new ConformerGenerator().getOneConformer()
		}
	}
