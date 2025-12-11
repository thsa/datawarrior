/*
 * Copyright Thomas Sander, Switzerland
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
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.conf.ConformerSet;
import com.actelion.research.chem.conf.ConformerSetGenerator;
import com.actelion.research.chem.docking.DockingEngine;
import com.actelion.research.chem.docking.LigandPose;
import com.actelion.research.chem.docking.scoring.AbstractScoringEngine;
import com.actelion.research.chem.docking.scoring.ChemPLP;
import com.actelion.research.chem.forcefield.mmff.ForceFieldMMFF94;
import com.actelion.research.chem.interactions.InteractionPoint;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.io.pdb.calc.MoleculeGrid;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;
import org.openmolecules.chem.conf.gen.ConformerGenerator;
import org.openmolecules.chem.conf.gen.RigidFragmentCache;
import org.openmolecules.fx.viewer3d.interactions.drugscore.DrugScoreAtomClassifier;
import org.openmolecules.fx.viewer3d.interactions.drugscore.DrugScoreInteractionCalculator;
import org.openmolecules.fx.viewer3d.interactions.drugscore.DrugScorePotential;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DETaskAddDockingScore extends ConfigurableTask {
	private static final String MMFF_TABLE_SET = ForceFieldMMFF94.MMFF94SPLUS;
	private static final double DIELECTRIC_CONSTANT = 80.0;

	public static final String TASK_NAME = "Add Docking Score";

	private static final String PROPERTY_LIGAND_COLUMN = "ligandColumn";
	private static final String PROPERTY_SCORE_TYPE = "scoreType";
	private static final String[] TYPE_CODE = {"chemplp", "drugscore"};
	private static final String[] TYPE_TEXT = {"ChemPLP Score", "Drugscore 2018"};
	private static final int SCORE_TYPE_CHEMPLP = 0;
	private static final int SCORE_TYPE_DRUGSCORE = 1;

	private final CompoundTableModel mTableModel;
	private JComboBox<String> mComboBoxLigandCoords,mComboBoxType;
	private int mScoreType;
	private volatile Map<String,Object> mMMFFOptions;

	public DETaskAddDockingScore(DEFrame parent) {
		super(parent, true);
		mTableModel = parent.getTableModel();
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
	}

	@Override
	public JPanel createDialogContent() {
		JPanel content = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };
		content.setLayout(new TableLayout(size));

		int[] ligandCoordsColumn = getCompatibleChemistryColumnList();
		mComboBoxLigandCoords = new JComboBox<>();
		if (ligandCoordsColumn != null)
			for (int column:ligandCoordsColumn)
				mComboBoxLigandCoords.addItem(mTableModel.getColumnTitle(column));

		mComboBoxLigandCoords.setEditable(!isInteractive());
		content.add(new JLabel("3D-Ligand column:"), "1,1");
		content.add(mComboBoxLigandCoords, "3,1");

		mComboBoxType = new JComboBox<>(TYPE_TEXT);
		content.add(new JLabel("Scoring algorithm:"), "1,3");
		content.add(mComboBoxType, "3,3");
		return content;
	}

//	@Override
//	public String getHelpURL() {
//		return "/html/help/chemistry.html#AddDockingScore";
//	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_LIGAND_COLUMN, mTableModel.getColumnTitleNoAlias((String)mComboBoxLigandCoords.getSelectedItem()));
		configuration.setProperty(PROPERTY_SCORE_TYPE, TYPE_CODE[mComboBoxType.getSelectedIndex()]);
		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		int ligandCoordsColumn = mTableModel.findColumn(configuration.getProperty(PROPERTY_LIGAND_COLUMN));
		if (ligandCoordsColumn != -1 && isCompatibleLigandColumn(ligandCoordsColumn))
			mComboBoxLigandCoords.setSelectedItem(mTableModel.getColumnTitle(ligandCoordsColumn));
		mComboBoxType.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_SCORE_TYPE), TYPE_CODE, 0));
	}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mComboBoxLigandCoords.getItemCount() != 0)
			mComboBoxLigandCoords.setSelectedIndex(0);
		mComboBoxType.setSelectedIndex(0);
	}

	private int[] getCompatibleChemistryColumnList() {
		ArrayList<Integer> ligandColumnList = new ArrayList<>();
		for (int coordsColumn=0; coordsColumn<mTableModel.getTotalColumnCount(); coordsColumn++)
			if (isCompatibleLigandColumn(coordsColumn))
				ligandColumnList.add(coordsColumn);

		if (ligandColumnList.isEmpty())
			return null;

		int[] ligandColumn = new int[ligandColumnList.size()];
		for (int i=0; i<ligandColumn.length; i++)
			ligandColumn[i] = ligandColumnList.get(i);
		return ligandColumn;
	}

	private boolean isCompatibleLigandColumn(int coordsColumn) {
		if (!CompoundTableConstants.cColumnType3DCoordinates.equals(mTableModel.getColumnSpecialType(coordsColumn)))
			return false;

		int ligandColumn = mTableModel.getParentColumn(coordsColumn);
		String cavityIDCode = mTableModel.getColumnProperty(coordsColumn, CompoundTableConstants.cColumnPropertyProteinCavity);
		if (cavityIDCode != null)
			return true;

		int cavityColumn = mTableModel.findColumn(mTableModel.getColumnProperty(coordsColumn, CompoundTableConstants.cColumnPropertyProteinCavityColumn));
		if (cavityColumn != -1)
			return true;

		return false;
	}

	@Override
	public boolean isConfigurable() {
		if (getCompatibleChemistryColumnList() == null) {
			showErrorMessage("Running '"+getTaskName()+"' requires the presence of a column containing 3D-ligand structures\n"
							+ "plus either a protein cavity structure assigned to this column\n"
							+ "or another associated column that contains protein cavities for every table row. ");
			return false;
		}

		return true;
	}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String ligandCoordsColumnName = configuration.getProperty(PROPERTY_LIGAND_COLUMN, "");
		if (ligandCoordsColumnName.isEmpty()) {
			showErrorMessage("No ligand column defined.");
			return false;
		}
		if (isLive) {
			int ligandCoordsColumn = mTableModel.findColumn(ligandCoordsColumnName);
			if (ligandCoordsColumn == -1) {
				showErrorMessage("Ligand column '"+ligandCoordsColumnName+"' not found.");
				return false;
			}
			if (!isCompatibleLigandColumn(ligandCoordsColumn)) {
				showErrorMessage("Ligand column '"+ligandCoordsColumnName+"' doesn't contain 3D-structures or has no assigned cavity.");
				return false;
			}
		}
		return true;
	}

	@Override
	public void runTask(Properties configuration) {
		final int scoreType = findListIndex(configuration.getProperty(PROPERTY_SCORE_TYPE), TYPE_CODE, 0);
		final int ligandCoordsColumn = mTableModel.findColumn(configuration.getProperty(PROPERTY_LIGAND_COLUMN));
		final int ligandIDCodeColumn = mTableModel.getParentColumn(ligandCoordsColumn);
		final int cavityCoordsColumn = mTableModel.findColumn(mTableModel.getColumnProperty(ligandCoordsColumn, CompoundTableConstants.cColumnPropertyProteinCavityColumn));
		final int cavityIDCodeColumn = (cavityCoordsColumn == -1) ? -1 : mTableModel.getParentColumn(cavityCoordsColumn);

		String cavityIDCode = mTableModel.getColumnProperty(ligandCoordsColumn, CompoundTableConstants.cColumnPropertyProteinCavity);
		final StereoMolecule sharedCavity = (cavityIDCode == null) ? null : new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(cavityIDCode);

		final int totalRowCount = mTableModel.getTotalRowCount();
		final AtomicInteger remaining = new AtomicInteger(totalRowCount);
		final double[] score = new double[totalRowCount];

		final TreeMap<String, DrugScorePotential> drugScorePotentials = (scoreType == SCORE_TYPE_DRUGSCORE) ?
				DrugScoreInteractionCalculator.getPotentials() : null;

		if (scoreType == SCORE_TYPE_CHEMPLP)
			initializeMMFF94();

		startProgress("Processing binding sites...", 0, totalRowCount);

		int threadCount = Math.min(totalRowCount, Runtime.getRuntime().availableProcessors());
		Thread[] thread = new Thread[threadCount];
		for (int i=0; i<threadCount; i++) {
			thread[i] = new Thread("Binding Site Scorer "+(i+1)) {
				public void run() {
					StereoMolecule ligand = new StereoMolecule();
					IDCodeParserWithoutCoordinateInvention parser = new IDCodeParserWithoutCoordinateInvention();

					int m = remaining.decrementAndGet();
					while (m >= 0 && !threadMustDie()) {
						int row = totalRowCount - m - 1;
						updateProgress(row);
						CompoundRecord record = mTableModel.getTotalRecord(row);
						byte[] ligandIDCode = (byte[])record.getData(ligandIDCodeColumn);
						byte[] ligandCoords = (byte[])record.getData(ligandCoordsColumn);
						if (ligandIDCode != null && ligandCoords != null) {
							try {
								parser.parse(ligand, ligandIDCode, ligandCoords);
								StereoMolecule cavity = sharedCavity;
								if (cavity == null) {
									byte[] cavityIDCode = (byte[])record.getData(cavityIDCodeColumn);
									byte[] cavityCoords = (byte[])record.getData(cavityCoordsColumn);
									cavity = parser.getCompactMolecule(cavityIDCode, cavityCoords);
								}

								score[row] = (scoreType == SCORE_TYPE_CHEMPLP) ?
										calculateChemPLPScore(cavity, ligand)
										   : (scoreType == SCORE_TYPE_DRUGSCORE) ?
										calculateDrugScore2018(cavity, ligand, drugScorePotentials)
										   : Double.NaN;
							}
							catch (Exception e) {
								e.printStackTrace();
							}
						}
						m = remaining.decrementAndGet();
					}
				}
			};
		}

		for (Thread t:thread)
			t.start();

		for (Thread t:thread)
			try { t.join(); } catch (InterruptedException ie) {}

		int scoreColumn = mTableModel.addNewColumns(1);
		mTableModel.setColumnName(TYPE_TEXT[scoreType], scoreColumn);
		for (int row=0; row<score.length; row++)
			mTableModel.setTotalValueAt(DoubleFormat.toString(score[row]), row, scoreColumn);
		mTableModel.finalizeNewColumns(scoreColumn, this);
	}

	private double calculateDrugScore2018(StereoMolecule cavity, StereoMolecule ligand, TreeMap<String, DrugScorePotential> drugScorePotentials) {
		List<InteractionPoint> ligandIPList = determineDrugscoreInteractionPoints(ligand);
		List<InteractionPoint> cavityIPList = determineDrugscoreInteractionPoints(cavity);

		double drugscore = 0.0;
		for (InteractionPoint p1: ligandIPList) {
			for (InteractionPoint p2 : cavityIPList) {
				Coordinates c1 = p1.getMol().getAtomCoordinates(p1.getAtom());
				Coordinates c2 = p2.getMol().getAtomCoordinates(p2.getAtom());
				double distance = c1.distance(c2);

				String key1 = DrugScoreAtomClassifier.typeName(p1.getType());
				String key2 = DrugScoreAtomClassifier.typeName(p2.getType());
				String key = key1.compareTo(key2) < 0 ? key1+"-"+key2 : key2+"-"+key1;
				DrugScorePotential dsp = drugScorePotentials.get(key);
				if (dsp != null)
					drugscore += dsp.getPotential(distance);
			}
		}

		return drugscore;
	}

	private List<InteractionPoint> determineDrugscoreInteractionPoints(StereoMolecule mol) {
		int[] type = new DrugScoreAtomClassifier().classifyAtoms(mol);
		ArrayList<InteractionPoint> list = new ArrayList<>();
		for (int atom=0; atom<type.length; atom++)
			list.add(new InteractionPoint(mol, atom, type[atom]));
		return list;
	}

	private double calculateChemPLPScore(StereoMolecule cavity, StereoMolecule ligand) {
		try {
			final double GRID_DIMENSION = 6;
			final double GRID_RESOLUTION = 0.5;
			MoleculeGrid grid = new MoleculeGrid(ligand, GRID_RESOLUTION, new Coordinates(GRID_DIMENSION, GRID_DIMENSION, GRID_DIMENSION));

			AbstractScoringEngine engine = null;
			Set<Integer> bindingSiteAtoms = new HashSet<Integer>();

			// ChemPLP
			DockingEngine.getBindingSiteAtoms(cavity, bindingSiteAtoms, grid, true);
			engine = new ChemPLP(new Molecule3D(cavity), bindingSiteAtoms, grid);

			// IdoScore
			//		DockingEngine.getBindingSiteAtoms(receptor, bindingSiteAtoms, grid, false);
			//		engine = new IdoScore(receptor,bindingSiteAtoms, getReceptorAtomTypes(receptor),grid);

			double e0 = getMinConformerEnergy(ligand);
			engine.init(new LigandPose(new Conformer(ligand), engine, e0), e0);

			return engine.getScore();
		}
		catch (Exception e) {
			System.out.println("ERROR calculating ChemPLP score:"+e.getMessage());
			return Double.NaN;
		}
	}

	private void initializeMMFF94() {
		ForceFieldMMFF94.initialize(MMFF_TABLE_SET);
		mMMFFOptions = new HashMap<>();
		mMMFFOptions.put("dielectric constant", DIELECTRIC_CONSTANT);
		RigidFragmentCache.getDefaultInstance().loadDefaultCache();
		RigidFragmentCache.getDefaultInstance().resetAllCounters();
		}

	private double getMinConformerEnergy(StereoMolecule ligand) {
		Molecule3D nativePose = new Molecule3D(ligand);

		ConformerSetGenerator confSetGen = new ConformerSetGenerator(64, ConformerGenerator.STRATEGY_LIKELY_RANDOM, false, LigandPose.SEED);
		ConformerSet confSet = confSetGen.generateConformerSet(nativePose);
		double eMin = Double.MAX_VALUE;

		for (Conformer conformer : confSet) {
			StereoMolecule conf = conformer.toMolecule();
			ForceFieldMMFF94 mmff = new ForceFieldMMFF94(conf, ForceFieldMMFF94.MMFF94SPLUS, mMMFFOptions);
			mmff.minimise();
			double e = mmff.getTotalEnergy();
			if (eMin > e)
				eMin = e;
		}
		return eMin;
	}
}
