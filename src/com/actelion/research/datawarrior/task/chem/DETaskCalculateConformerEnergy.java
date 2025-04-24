package com.actelion.research.datawarrior.task.chem;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.conf.AtomAssembler;
import com.actelion.research.chem.conf.TorsionDescriptor;
import com.actelion.research.chem.conf.TorsionDescriptorHelper;
import com.actelion.research.chem.forcefield.mmff.ForceFieldMMFF94;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;
import org.openmolecules.chem.conf.gen.ConformerGenerator;
import org.openmolecules.chem.conf.gen.RigidFragmentCache;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DETaskCalculateConformerEnergy extends DETaskAbstractFromStructure {
	/* TODO remove
	private static final boolean WRITE_DEBUG_FILE = true;
	private BufferedWriter mDebugWriter;
	*/
	private static final boolean WRITE_CONTRIBUTION_FILE = true;
	private BufferedWriter mContributionWriter;

	private static final String MMFF_TABLE_SET = ForceFieldMMFF94.MMFF94SPLUS;
	public static final String TASK_NAME = "Calculate Conformer Energy";

	private static final String PROPERTY_CALC_LOCAL = "calcLocal";
	private static final String PROPERTY_CALC_GLOBAL = "calcGlobal";
	private static final String PROPERTY_GLOBAL_CONFORMERS = "globalMaxConformers";
	private static final String PROPERTY_ADD_CONFORMERS = "addConformers";
	private static final String PROPERTY_DIELECTRIC_CONSTANT = "dielectricConstant";

	private JCheckBox mCheckBoxCalcLocalEnergyDif,mCheckBoxCalcGlobalEnergyDif, mCheckBoxAddConformers;
	private JTextField mTextFieldConformerCount,mTextFieldDielectricConstant;
	private volatile int mMaxConformers,mMinimizationErrors;
	private volatile boolean mAddConformers,mCalcLocal,mCalcGlobal;
	private volatile Map<String,Object> mMMFFOptions;

	public DETaskCalculateConformerEnergy(DEFrame parent) {
		super(parent, DESCRIPTOR_3D_COORDINATES, false, true);
	}

	@Override
	public boolean hasExtendedDialogContent() {
		return true;
	}

	@Override
	public JPanel getExtendedDialogContent() {
		int gap = HiDPIHelper.scale(8);
		int indent = HiDPIHelper.scale(32);
		double[][] size = { {indent, indent, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED},
							{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap>>2, TableLayout.PREFERRED,
							 gap>>2, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED} };

		mTextFieldConformerCount = new JTextField(3);
		mCheckBoxCalcLocalEnergyDif = new JCheckBox("Local minimum energy conformer");
		mCheckBoxCalcGlobalEnergyDif = new JCheckBox("Global minimum energy conformer");
		mCheckBoxAddConformers = new JCheckBox("Add structures of minimum energy conformer(s)");
		mCheckBoxCalcGlobalEnergyDif.addActionListener(e -> mTextFieldConformerCount.setEnabled(mCheckBoxCalcGlobalEnergyDif.isSelected()));

		mTextFieldDielectricConstant = new JTextField(3);
		JPanel dePanel = new JPanel();
		double[][] deSize = { {TableLayout.PREFERRED, gap, TableLayout.PREFERRED, TableLayout.FILL}, {TableLayout.PREFERRED}};
		dePanel.setLayout(new TableLayout(deSize));
		dePanel.add(new JLabel("Dielectric constant:"), "0,0");
		dePanel.add(mTextFieldDielectricConstant, "2,0");

		JPanel ep = new JPanel();
		ep.setLayout(new TableLayout(size));
		ep.add(new JLabel("Calculate energy difference to:", JLabel.LEFT), "0,1,6,1");
		ep.add(mCheckBoxCalcLocalEnergyDif, "1,3,6,3");
		ep.add(mCheckBoxCalcGlobalEnergyDif, "1,5,6,5");
		ep.add(new JLabel("Generate up to ", JLabel.RIGHT), "2,7");
		ep.add(mTextFieldConformerCount, "4,7");
		ep.add(new JLabel("conformers", JLabel.LEFT), "6,7");
		ep.add(mCheckBoxAddConformers, "1,9,6,9");
		ep.add(dePanel, "0,11,6,11");
		return ep;
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public String getHelpURL() {
		return "/html/help/conformers.html#Energy";
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();

		configuration.setProperty(PROPERTY_CALC_LOCAL, mCheckBoxCalcLocalEnergyDif.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_CALC_GLOBAL, mCheckBoxCalcGlobalEnergyDif.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_GLOBAL_CONFORMERS, mTextFieldConformerCount.getText());
		configuration.setProperty(PROPERTY_ADD_CONFORMERS, mCheckBoxAddConformers.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_DIELECTRIC_CONSTANT, mTextFieldDielectricConstant.getText());

		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);

		mCheckBoxCalcLocalEnergyDif.setSelected(configuration.getProperty(PROPERTY_CALC_LOCAL, "").equals("true"));
		mCheckBoxCalcGlobalEnergyDif.setSelected(configuration.getProperty(PROPERTY_CALC_GLOBAL, "").equals("true"));
		mTextFieldConformerCount.setText(configuration.getProperty(PROPERTY_GLOBAL_CONFORMERS, "64"));
		mCheckBoxAddConformers.setSelected(configuration.getProperty(PROPERTY_ADD_CONFORMERS, "").equals("true"));
		mTextFieldDielectricConstant.setText(configuration.getProperty(PROPERTY_DIELECTRIC_CONSTANT, "1.0"));
	}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mCheckBoxCalcLocalEnergyDif.setSelected(true);
		mCheckBoxCalcGlobalEnergyDif.setSelected(false);
		mTextFieldConformerCount.setText("64");
		mTextFieldConformerCount.setEnabled(false);
		mCheckBoxAddConformers.setSelected(false);
		mTextFieldDielectricConstant.setText("80.0");
	}

	@Override
	protected void setNewColumnProperties(int firstNewColumn) {
		getTableModel().setColumnName("Conformer Energy", firstNewColumn);
		getTableModel().setExplicitDataType(firstNewColumn++, CompoundTableConstants.cDataTypeFloat, false);

		if (mCalcLocal) {
			getTableModel().setColumnName("Energy-Dif Local Minimum", firstNewColumn);
			getTableModel().setExplicitDataType(firstNewColumn++, CompoundTableConstants.cDataTypeFloat, false);
		}

		if (mCalcGlobal) {
			getTableModel().setColumnName("Energy-Dif Global Minimum", firstNewColumn);
			getTableModel().setExplicitDataType(firstNewColumn++, CompoundTableConstants.cDataTypeFloat, false);
		}

		if (mAddConformers) {
			int idcodeColumn = getChemistryColumn();

			if (mCalcLocal) {
				getTableModel().setColumnName("Local Min-Energy Conformer", firstNewColumn);
				getTableModel().setColumnProperty(firstNewColumn, CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnType3DCoordinates);
				getTableModel().setColumnProperty(firstNewColumn, CompoundTableConstants.cColumnPropertyParentColumn, getTableModel().getColumnTitleNoAlias(idcodeColumn));
				firstNewColumn++;
			}
			if (mCalcGlobal) {
				getTableModel().setColumnName("Global Min-Energy Conformer", firstNewColumn);
				getTableModel().setColumnProperty(firstNewColumn, CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnType3DCoordinates);
				getTableModel().setColumnProperty(firstNewColumn, CompoundTableConstants.cColumnPropertyParentColumn, getTableModel().getColumnTitleNoAlias(idcodeColumn));
				firstNewColumn++;
			}
		}
	}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (!super.isConfigurationValid(configuration, isLive))
			return false;

		mCalcLocal = configuration.getProperty(PROPERTY_CALC_LOCAL, "").equals("true");
		mCalcGlobal = configuration.getProperty(PROPERTY_CALC_GLOBAL, "").equals("true");
		mAddConformers = configuration.getProperty(PROPERTY_ADD_CONFORMERS, "").equals("true");

		String dielectricConstant = configuration.getProperty(PROPERTY_DIELECTRIC_CONSTANT, "");
		if (dielectricConstant.isEmpty()) {
			showErrorMessage("Dielectric constant is undefined.");
			return false;
		}

		try {
			double constant = Double.parseDouble(dielectricConstant);
			if (constant < 1.0) {
				showErrorMessage("Dielectric constant cannot be smaller than 1.0.");
				return false;
			}
		} catch (NumberFormatException e) {
			showErrorMessage("Dielectric constant is not numerical.");
			return false;
		}

		mMaxConformers = 0;
		if (mCalcGlobal) {
			String maxCount = configuration.getProperty(PROPERTY_GLOBAL_CONFORMERS, "");
			if (maxCount.isEmpty()) {
				showErrorMessage("Global conformer count value is undefined.");
				return false;
			}

			try {
				mMaxConformers = Integer.parseInt(maxCount);
				if (mMaxConformers <= 2) {
					showErrorMessage("Conformer count value too low.");
					return false;
				}
			} catch (NumberFormatException e) {
				showErrorMessage("Conformer count value is not numerical.");
				return false;
			}
		}

		if (isLive) {
			int idcodeColumn = getChemistryColumn(configuration);
			if (getTableModel().getChildColumn(idcodeColumn, CompoundTableConstants.cColumnType3DCoordinates) == -1) {
				showErrorMessage("The selected structure column does not contain 3D-atom-coordinates.");
				return false;
			}
		}

		return true;
	}

	@Override
	protected int getNewColumnCount() {
		int count = 1;
		if (mCalcLocal)
			count++;

		if (mCalcGlobal)
			count++;

		if (mAddConformers) {
			if (mCalcLocal)
				count++;
			if (mCalcGlobal)
				count++;
		}

		return count;
	}

	@Override
	protected String getNewColumnName(int column) {
		// names are given by setNewColumnProperties();
		return "";
	}

	@Override
	protected boolean preprocessRows(Properties configuration) {
		mCalcLocal = configuration.getProperty(PROPERTY_CALC_LOCAL, "").equals("true");
		mCalcGlobal = configuration.getProperty(PROPERTY_CALC_GLOBAL, "").equals("true");
		try { mMaxConformers = Integer.parseInt(configuration.getProperty(PROPERTY_GLOBAL_CONFORMERS, "64")); } catch (NumberFormatException e) {}
		mAddConformers = configuration.getProperty(PROPERTY_ADD_CONFORMERS, "").equals("true");
		double dielectricConstant = 1.0;
		try { dielectricConstant = Double.parseDouble(configuration.getProperty(PROPERTY_DIELECTRIC_CONSTANT, "1.0")); } catch (NumberFormatException e) {}

		ForceFieldMMFF94.initialize(MMFF_TABLE_SET);
		mMMFFOptions = new HashMap<>();
		mMMFFOptions.put("dielectric constant", dielectricConstant);
		mMinimizationErrors = 0;
		if (mCalcGlobal) {
			RigidFragmentCache.getDefaultInstance().loadDefaultCache();
			RigidFragmentCache.getDefaultInstance().resetAllCounters();
		}

		/*
		if (WRITE_DEBUG_FILE)
			try { mDebugWriter = new BufferedWriter(new FileWriter("/home/thomas/data/conformers/energies/debug.sdf")); } catch (IOException ioe) {
				ioe.printStackTrace();
				System.exit(0);
			} */
		if (WRITE_CONTRIBUTION_FILE)
			try { mContributionWriter = new BufferedWriter(new FileWriter("/home/thomas/data/conformers/energies/contributions.sdf")); } catch (
					IOException ioe) {
				ioe.printStackTrace();
				System.exit(0);
			}

		return true;
	}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule mol, int threadIndex) {
		getTableModel().getChemicalStructure(getTableModel().getTotalRecord(row), getCoordinates3DColumn(), CompoundTableModel.ATOM_COLOR_MODE_NONE, mol);
		if (mol == null || mol.getAllAtoms() == 0)
			return;

// TODO remove!!! this is to check, whether our H-addition procedure contains less strain than whatever was used before
//mol.removeExplicitHydrogens(true);

		new AtomAssembler(mol).addImplicitHydrogens();

/* Check all hydrogen bond lengths and any mutual hydrogen distances for too short ones
// and write all suspicious molecules into a diagnostic sd-file.
mol.ensureHelperArrays(Molecule.cHelperNeighbours);
StringBuilder lengthBuilder = new StringBuilder();
StringBuilder distanceBuilder = new StringBuilder();
for (int atom1=mol.getAtoms(); atom1<mol.getAllAtoms(); atom1++) {
	double bondLength = mol.getAtomCoordinates(atom1).distance(mol.getAtomCoordinates(mol.getConnAtom(atom1, 0)));
	if (bondLength < 0.9) {
		lengthBuilder.append("H:" + atom1 + " conn:" + mol.getConnAtom(atom1, 0) + " bondLength:" + DoubleFormat.toString(bondLength)+"\n");
	}
	for (int atom2=mol.getAtoms(); atom2<mol.getAllAtoms(); atom2++) {
		if (atom1 != atom2) {
			double distance = mol.getAtomCoordinates(atom1).distance(mol.getAtomCoordinates(atom2));
			if (distance < 2.0 && mol.getConnAtom(atom1, 0) != mol.getConnAtom(atom2, 0)) {
				distanceBuilder.append("a1:" + atom1 + " a2:" + atom2 + " distance:" + DoubleFormat.toString(distance)+"\n");
				found = true;
			}
		}
	}
}
if (!lengthBuilder.isEmpty() || !distanceBuilder.isEmpty()) {
	if (WRITE_DEBUG_FILE)
		synchronized (this) {
			try {
				mDebugWriter.write(new MolfileCreator(mol).getMolfile());
				mDebugWriter.newLine();
				if (!lengthBuilder.isEmpty()) {
					mDebugWriter.write(">  <Bond length Issues>");
					mDebugWriter.newLine();
					mDebugWriter.write(lengthBuilder.toString());
					mDebugWriter.newLine();
					mDebugWriter.newLine();
				}
				if (!distanceBuilder.isEmpty()) {
					mDebugWriter.write(">  <Atom Distance Issues>");
					mDebugWriter.newLine();
					mDebugWriter.write(distanceBuilder.toString());
					mDebugWriter.newLine();
					mDebugWriter.newLine();
				}
				mDebugWriter.write("$$$$");
				mDebugWriter.newLine();
			} catch (IOException ioe) {}
		}
	else {
		System.out.println();
		if (!lengthBuilder.isEmpty())
			System.out.println(lengthBuilder.toString().replace("<NL>", "\\n"));
		if (!distanceBuilder.isEmpty())
			System.out.println(distanceBuilder.toString().replace("<NL>", "\\n"));
		System.out.println(new MolfileCreator(mol).getMolfile());
	}
}
*/

		StringBuilder detail = new StringBuilder();
		ForceFieldMMFF94 ff = new ForceFieldMMFF94(mol, MMFF_TABLE_SET, mMMFFOptions);
		double absEnergy = ff.getTotalEnergy(detail);
		System.out.println(detail);
		System.out.println();

		getTableModel().setTotalValueAt(DoubleFormat.toString(absEnergy), row, firstNewColumn);
/* alternatively torsion strain only:		getTableModel().setTotalValueAt(DoubleFormat.toString(calculateTorsionStrain(mol)), row, firstNewColumn); */

		String localCoords = null;
		double minEnergy = Double.MAX_VALUE;
		boolean minimizationError = false;
		if (mCalcLocal) {
			firstNewColumn++;
			MinimizationResult result = new MinimizationResult();
			minimize(mol, result);
			if (result.error().isEmpty()) {
				minEnergy = result.energy;
				getTableModel().setTotalValueAt(DoubleFormat.toString(absEnergy - result.energy), row, firstNewColumn);
				localCoords = new Canonizer(mol).getEncodedCoordinates(true);
			}
			else {
				minimizationError = true;
				getTableModel().setTotalValueAt(result.error(), row, firstNewColumn);
			}
		}

		String globalCoords = localCoords;
		if (mCalcGlobal) {
			firstNewColumn++;

			if (!minimizationError) {
				ConformerGenerator cg = null;

				String error = null;

				for (int i=0; i<mMaxConformers; i++) {
					if (cg == null) {
						cg = new ConformerGenerator(true);
						cg.initializeConformers(mol, ConformerGenerator.STRATEGY_ADAPTIVE_RANDOM, 1000, false);
					}
					mol = cg.getNextConformerAsMolecule(mol);

					if (mol != null && mol.getAllAtoms() != 0) {
						MinimizationResult result = new MinimizationResult();
						minimize(mol, result);

						if (result.error().isEmpty()) {
							if (minEnergy > result.energy) {
								minEnergy = result.energy;
								globalCoords = new Canonizer(mol).getEncodedCoordinates(true);
							}
						}
						else {
							error = result.error();
							minimizationError = true;
						}
					}
				}

				getTableModel().setTotalValueAt(minimizationError ? error : DoubleFormat.toString(absEnergy - minEnergy), row, firstNewColumn);
			}
		}

		if (minimizationError)
			mMinimizationErrors++;

		if (mAddConformers) {
			if (mCalcLocal)
				getTableModel().setTotalValueAt(localCoords, row, ++firstNewColumn);
			if (mCalcGlobal)
				getTableModel().setTotalValueAt(globalCoords, row, ++firstNewColumn);
		}
	}

	private double calculateTorsionStrain(StereoMolecule mol) {
		Map<String,Object> options = new HashMap<>();
		options.put("angle bend", Boolean.FALSE);
		options.put("bond stretch", Boolean.FALSE);
		options.put("electrostatic", Boolean.FALSE);
		options.put("out of plane", Boolean.FALSE);
		options.put("stretch bend", Boolean.FALSE);
		options.put("torsion angle", Boolean.TRUE);
		options.put("van der waals", Boolean.FALSE);
		ForceFieldMMFF94 ff = new ForceFieldMMFF94(mol, MMFF_TABLE_SET, options);
		return ff.getTotalEnergy();
	}

	/**
	 * Minimizes the molecule with the method defined in mMinimization.
	 * The starting conformer is taken from mol.
	 * When this method finishes, then the minimized atom coordinates are in mol,
	 * even if mMinimization == MINIMIZE_NONE.
	 * @param mol receives minimized coodinates; taken as start conformer
	 * @param result receives energy and possibly error message
	 */
	private void minimize(StereoMolecule mol, MinimizationResult result) {
		try {
			ForceFieldMMFF94 ff = new ForceFieldMMFF94(mol, MMFF_TABLE_SET, mMMFFOptions);
			int error = ff.minimise(10000, 0.0001, 1.0e-6);
			if (error != 0)
				throw new Exception("MMFF94 error code "+error);
			result.energy = (float)ff.getTotalEnergy();
		}
		catch (Exception e) {
			result.energy = Double.NaN;
			result.errorMessage = e.getLocalizedMessage();
		}
	}

	private boolean isRedundantConformer(TorsionDescriptorHelper torsionHelper, ArrayList<TorsionDescriptor> torsionDescriptorList) {
		TorsionDescriptor ntd = torsionHelper.getTorsionDescriptor();
		for (TorsionDescriptor td:torsionDescriptorList)
			if (td.equals(ntd))
				return true;

		torsionDescriptorList.add(ntd);
		return false;
	}

	protected void postprocess(int firstNewColumn) {
		if (mMinimizationErrors != 0 && isInteractive())
			showInteractiveTaskMessage("Forcefield minimization failed in "+mMinimizationErrors+" cases.", JOptionPane.INFORMATION_MESSAGE);

//		if (WRITE_DEBUG_FILE)
//			try { mDebugWriter.close(); } catch (IOException ioe) {}
	}

	private static class MinimizationResult {
		private double energy;
		private String errorMessage;

		public MinimizationResult() {
			energy = Double.NaN;
			errorMessage = null;
		}

		public String energy() {
			return Double.isNaN(energy) ? "" : DoubleFormat.toString(energy);
		}

		public String error() {
			return errorMessage == null ? "" : errorMessage;
		}
	}
}
