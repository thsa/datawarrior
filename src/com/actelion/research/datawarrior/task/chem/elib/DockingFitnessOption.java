package com.actelion.research.datawarrior.task.chem.elib;

import com.actelion.research.calc.ProgressListener;
import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.IDCodeParserWithoutCoordinateInvention;
import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.AtomAssembler;
import com.actelion.research.chem.docking.DockingEngine;
import com.actelion.research.chem.docking.DockingFailedException;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.task.chem.DETaskDockIntoProteinCavity;
import com.actelion.research.table.model.CompoundTableModel;
import org.openmolecules.chem.conf.gen.ConformerGenerator;

import java.util.concurrent.ConcurrentHashMap;

public class DockingFitnessOption extends FitnessOption {
	private String mCavityIDCode,mLigandIDCode;
	private StereoMolecule mCavity,mLigand;
	private static ConcurrentHashMap<String,DockingEngine> sDockingEngineMap;

	public DockingFitnessOption(String params, ProgressListener pl) {
		String[] param = params.split("\\t");
		if (param.length == 3) {
			mSliderValue = Integer.parseInt(param[0]);
			mCavityIDCode = param[1];
			mLigandIDCode = param[2];
			mCavity = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(mCavityIDCode);
			mLigand = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(mLigandIDCode);
			DETaskDockIntoProteinCavity.assignLikelyProtonationStates(mCavity);
			DETaskDockIntoProteinCavity.assignLikelyProtonationStates(mLigand);
			new AtomAssembler(mCavity).addImplicitHydrogens();
			new AtomAssembler(mLigand).addImplicitHydrogens();
		}
	}

	public static String getParamError(String params) {
		String[] param = params.split("\\t");
		if (param.length != 3)
			return "Wrong parameter count.";

		StereoMolecule cavity = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(param[1]);
		if (cavity == null || cavity.getAllAtoms() == 0)
			return "No protein cavity given.";

		StereoMolecule ligand = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(param[2]);
		if (ligand == null || ligand.getAllAtoms() == 0)
			return "No natural ligand given.";

		return null;
	}

	@Override
	public String getName() {
		return "Docking Score";
	}

	@Override
	public int getResultColumnCount() {
		return super.getResultColumnCount() + 1;
	}

	@Override
	public String getResultColumnName(int i) {
		return (i < super.getResultColumnCount()) ? super.getResultColumnName(i) : "Docking Pose";
	}

	@Override
	public void setResultColumnProperties(int i, CompoundTableModel tableModel, int column) {
		if (i == super.getResultColumnCount()) {
			tableModel.setColumnProperty(column, CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnType3DCoordinates);
			tableModel.setColumnProperty(column, CompoundTableConstants.cColumnPropertyParentColumn, tableModel.getColumnTitleNoAlias(0));
			tableModel.setColumnProperty(column, CompoundTableConstants.cColumnPropertyProteinCavity, mCavityIDCode);
			tableModel.setColumnProperty(column, CompoundTableConstants.cColumnPropertyNaturalLigand, mLigandIDCode);
		}
	}

	@Override
	public float calculateProperty(StereoMolecule mol, String[][] customColumnValueHolder) {
		mol.ensureHelperArrays(Molecule.cHelperNeighbours);

		DockingEngine dockingEngine;

		synchronized (this) {
			if (sDockingEngineMap == null)
				sDockingEngineMap = new ConcurrentHashMap<>();

			dockingEngine = sDockingEngineMap.get(Thread.currentThread().getName());
			if (dockingEngine == null) {
				try {
					dockingEngine = new DockingEngine(mCavity, mLigand);
				}
				catch (DockingFailedException dfe) {
					dfe.printStackTrace();
				}
				sDockingEngineMap.put(Thread.currentThread().getName(), dockingEngine);
			}
		}

		StereoMolecule ligand = new StereoMolecule(mol);
		DETaskDockIntoProteinCavity.assignLikelyProtonationStates(ligand);
		ConformerGenerator.addHydrogenAtoms(ligand);

		DockingEngine.DockingResult dockingResult = null;

		try {
			dockingResult = dockingEngine.dockMolecule(ligand);
			}
		catch (DockingFailedException dfe) {
			System.out.println(dfe.getMessage());
			dfe.printStackTrace();
		}

		StereoMolecule pose = dockingResult.getPose();

		Canonizer canonizer = new Canonizer(pose);
		customColumnValueHolder[0] = new String[1];
		customColumnValueHolder[0][0] = canonizer.getEncodedCoordinates(true);

		return (float)dockingResult.getScore();
	}

	@Override
	public float evaluateFitness(float propertyValue) {
		// consider sim**2 to value those changes higher where we already have high similarities
		// return isSimilar ? propertyValue*propertyValue : 2.0*propertyValue - propertyValue*propertyValue;
		return 0.25f - propertyValue / 200f;
	}
}
