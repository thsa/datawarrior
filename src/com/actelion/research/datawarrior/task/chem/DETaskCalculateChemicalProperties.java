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
import com.actelion.research.chem.conf.Conformer;
import com.actelion.research.chem.conf.ConformerSet;
import com.actelion.research.chem.conf.ConformerSetGenerator;
import com.actelion.research.chem.conf.MolecularFlexibilityCalculator;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.prediction.*;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DETableView;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.CompoundTableColorHandler;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.VisualizationColor;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;
import org.openmolecules.chem.conf.gen.ConformerGenerator;
import org.openmolecules.mesh.SurfaceAreaAndVolumeCalculator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DETaskCalculateChemicalProperties extends ConfigurableTask {
	public static final String TASK_NAME = "Calculate Compound Properties";

	private static final String CHEMPROPERTY_LIST_SEPARATOR = "\t";
	private static final String CHEMPROPERTY_LIST_SEPARATOR_REGEX = "\\t";
	private static final String CHEMPROPERTY_OPTION_SEPARATOR = "|";
	private static final String PROPERTY_STRUCTURE_COLUMN = "structureColumn";
	private static final String PROPERTY_CHEMPROPERTY_LIST = "propertyList";
	private static final String PROPERTY_TARGET_COLUMN = "targetColumn";

	private static final int PREDICTOR_COUNT			= 8;
	private static final int PREDICTOR_LOGP				= 0;
	private static final int PREDICTOR_LOGS				= 1;
	private static final int PREDICTOR_PKA				= 2;
	private static final int PREDICTOR_SURFACE			= 3;
	private static final int PREDICTOR_DRUGLIKENESS		= 4;
	private static final int PREDICTOR_TOXICITY			= 5;
	private static final int PREDICTOR_NASTY_FUNCTIONS	= 6;
	private static final int PREDICTOR_FLEXIBILITY		= 7;

	private static final int PREDICTOR_FLAG_LOGP			= (1 << PREDICTOR_LOGP);
	private static final int PREDICTOR_FLAG_LOGS			= (1 << PREDICTOR_LOGS);
	private static final int PREDICTOR_FLAG_PKA				= (1 << PREDICTOR_PKA);
	private static final int PREDICTOR_FLAG_SURFACE			= (1 << PREDICTOR_SURFACE);
	private static final int PREDICTOR_FLAG_DRUGLIKENESS	= (1 << PREDICTOR_DRUGLIKENESS);
	private static final int PREDICTOR_FLAG_TOXICITY		= (1 << PREDICTOR_TOXICITY);
	private static final int PREDICTOR_FLAG_NASTY_FUNCTIONS	= (1 << PREDICTOR_NASTY_FUNCTIONS);
	private static final int PREDICTOR_FLAG_FLEXIBILITY		= (1 << PREDICTOR_FLEXIBILITY);

	private static final int PROPERTY_COUNT = 68;

	private static final int TOTAL_WEIGHT = 0;
	private static final int FRAGMENT_WEIGHT = 1;
	private static final int FRAGMENT_ABS_WEIGHT = 2;
	private static final int LOGP = 3;
	private static final int LOGS = 4;
	private static final int LOGD = 5;
	private static final int ACCEPTORS = 6;
	private static final int DONORS = 7;
	private static final int SASA = 8;
	private static final int REL_PSA = 9;
	private static final int TPSA = 10;
	private static final int DRUGLIKENESS = 11;
	private static final int PERMEABILITY = 12;

	private static final int LE = 13;
//	private static final int SE = ;
	private static final int LLE = 14;
	private static final int LELP = 15;
	private static final int MUTAGENIC = 16;
	private static final int TUMORIGENIC = 17;
	private static final int REPRODUCTIVE_EFECTIVE = 18;
	private static final int IRRITANT = 19;
	private static final int NASTY_FUNCTIONS = 20;
	private static final int SHAPE = 21;
	private static final int FLEXIBILITY = 22;
	private static final int COMPLEXITY = 23;

	private static final int FRAGMENTS = 24;
	private static final int HEAVY_ATOMS = 25;
	private static final int NONCARBON_ATOMS = 26;
	private static final int METAL_ATOMS = 27;
	private static final int NEGATIVE_ATOMS = 28;
	private static final int STEREOCENTERS = 29;
	private static final int ROTATABLE_BONDS = 30;
	private static final int RING_CLOSURES = 31;
	private static final int AROMATIC_ATOMS = 32;
	private static final int SP3_CARBON_FRACTION = 33;
	private static final int SP3_ATOMS = 34;
	private static final int SYMMETRIC_ATOMS = 35;

	private static final int SMALL_RINGS = 36;
	private static final int SMALL_CARBO_RINGS = 37;
	private static final int SMALL_HETERO_RINGS = 38;
	private static final int SATURATED_RINGS = 39;
	private static final int NON_AROMATIC_RINGS = 40;
	private static final int AROMATIC_RINGS = 41;
	private static final int CARBO_SATURATED_RINGS = 42;
	private static final int CARBO_NON_AROMATIC_RINGS = 43;
	private static final int CARBO_AROMATIC_RINGS = 44;
	private static final int HETERO_SATURATED_RINGS = 45;
	private static final int HETERO_NON_AROMATIC_RINGS = 46;
	private static final int HETERO_AROMATIC_RINGS = 47;

	private static final int ALL_AMIDES = 48;
	private static final int ALL_AMINES = 49;
	private static final int ALKYL_AMINES = 50;
	private static final int ARYL_AMINES = 51;
	private static final int AROMATIC_NITROGEN = 52;
	private static final int BASIC_NITROGEN = 53;
	private static final int ACIDIC_OXYGEN = 54;
	private static final int STEREO_CONFIGURATION = 55;

	private static final int ACIDIC_PKA = 56;
	private static final int BASIC_PKA = 57;
	private static final int FRACTION_IA = 58;
	private static final int FRACTION_IB = 59;
	private static final int FRACTION_ZI = 60;
	private static final int FRACTION_CHARGED = 61;
	private static final int FRACTION_UNCHARGED = 62;
	private static final int CHARGE74 = 63;

	private static final int GLOBULARITY_SVD = 64;
	private static final int GLOBULARITY_VOL = 65;
	private static final int SURFACE_3D = 66;
	private static final int VOLUME_3D = 67;

	private static final Color[] TOX_COLOR_LIST = { Color.RED, Color.YELLOW, Color.GREEN };

	private static final String[] PROPERTY_CODE = { "totalWeight", "fragmentWeight", "fragmentAbsWeight", "logP", "logS", "logD",
													"acceptors", "donors", "sasa", "rpsa", "tpsa", "druglikeness", "permeability",
													"le", /*"se",*/ "lle", "lelp", "mutagenic", "tumorigenic", "reproEffective", "irritant", "nasty",
													"shape", "flexibility", "complexity", "fragments", "heavyAtoms", "nonCHAtoms", "metalAtoms", "negAtoms",
													"stereoCenters", "rotBonds", "closures", "aromAtoms", "sp3CFraction", "sp3Atoms", "symmetricAtoms",
													"rings", "carbo", "heteroRings", "satRings", "nonAromRings", "aromRings", "carboSatRings", "carboNonAromRings", "carboAromRings",
													"heteroSatRings", "heteroNonAromRings", "heteroAromRings",
													"amides", "amines", "alkylAmines", "arylAmines", "aromN", "basicN", "acidicO", "stereoConfiguration",
													"acidicPKA", "basicPKA", "acidicFI", "basicFI", "zwitterFI", "chargedF", "unchargedF", "charge74",
													"globularity", "globularity2", "surface3d", "volume3d" };

	private static final String[] TAB_GROUP = { "Druglikeness", "LE, Tox, Shape", "Atom Counts", "Ring Counts", "Functional Groups", "Ionization", "3D" };
	private static final String[][] TAB_HEADER = {null, {null, "Ki or IC50 in nmol/l"}, null, null, null, null, null};

	private DEFrame						mParentFrame;
	private CompoundTableModel			mTableModel;
	private DEProperty[]				mPropertyTable;
	private TreeMap<String,DEProperty>	mPropertyMap;
	private ArrayList<DEPropertyOrder>	mPropertyOrderList;
	private Object[]					mPredictor;
	private volatile int				mIDCodeColumn,mFragFpColumn,mFlexophoreColumn,mTargetColumn,mPropertyIndex;
	private JComboBox					mComboBoxStructureColumn;
	private JTabbedPane					mTabbedPane;
	private DEPropertyGUI[]				mPropertyGUI;
	private AtomicInteger				mSMPRecordIndex,mSMPWorkingThreads,mSMPErrorCount;

	public DETaskCalculateChemicalProperties(DEFrame parent) {
		super(parent, true);
		mTargetColumn = -1; // indicator that it is not a predefined task
		mParentFrame = parent;
		mTableModel = parent.getTableModel();
		}

	/**
	 * Use this constructor to create a predefined task to re-calculate a defined property into an existing column
	 * @param parent
	 * @param propertyCode
	 * @param idcodeColumn
	 * @param targetColumn
	 */
	public DETaskCalculateChemicalProperties(DEFrame parent, String propertyCode, int idcodeColumn, int targetColumn) {
		super(parent, true);
		mIDCodeColumn = idcodeColumn;
		mPropertyIndex = -1;
		for (int i=0; i<PROPERTY_CODE.length; i++) {
			if (PROPERTY_CODE[i].equals(propertyCode)) {
				mPropertyIndex = i;
				break;
				}
			}
		mTargetColumn = targetColumn;
		mParentFrame = parent;
		mTableModel = parent.getTableModel();
		}

	@Override
	public JPanel createDialogContent() {
		if (mPropertyMap == null)
			createPropertyMap();

		int gap = HiDPIHelper.scale(8);
		double[][] size1 = { {TableLayout.FILL, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, TableLayout.FILL},
							 {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED } };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size1));

		int[] structureColumn = mTableModel.getSpecialColumnList(CompoundTableModel.cColumnTypeIDCode);

		// create components
		mComboBoxStructureColumn = new JComboBox();
		if (structureColumn != null)
			for (int i=0; i<structureColumn.length; i++)
				mComboBoxStructureColumn.addItem(mTableModel.getColumnTitle(structureColumn[i]));
		if (!isInteractive())
			mComboBoxStructureColumn.setEditable(true);
		content.add(new JLabel("Structure column:", JLabel.RIGHT), "1,1");
		content.add(mComboBoxStructureColumn, "3,1");

		mTabbedPane = new JTabbedPane();
		mTabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		mPropertyGUI = new DEPropertyGUI[PROPERTY_COUNT];
		for (int tab=0; tab<TAB_GROUP.length; tab++) {
			JPanel cbp = new JPanel();
			double[][] size2 = { {gap, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, gap}, null };

			int count = (TAB_HEADER[tab] == null) ? 0 : 1;
			for (DEProperty property:mPropertyTable)
				if (property.tab == tab)
					count++;
			size2[1] = new double[2*count];

			for (int i=0; i<count; i++) {
				size2[1][2*i] = 2;
				size2[1][2*i+1] = TableLayout.PREFERRED;
				}
			if (TAB_HEADER[tab] != null) {
				size2[1][0] = 8;
				size2[1][2] = 8;
				}

			cbp.setLayout(new TableLayout(size2));
	
			if (TAB_HEADER[tab] != null)
				for (int i=0; i<TAB_HEADER[tab].length; i++)
					if (TAB_HEADER[tab][i] != null)
						cbp.add(new JLabel(TAB_HEADER[tab][i]), Integer.toString(1+2*i).concat(",1"));

			int row = (TAB_HEADER[tab] == null) ? 1 : 3;
			for (DEProperty property:mPropertyTable) {
				if (property.tab == tab) {
					mPropertyGUI[property.type] = new DEPropertyGUI(property);
					if (property.dependentColumnFilter == null) {
						cbp.add(mPropertyGUI[property.type].getCheckBox(), "1,"+row+",3,"+row);
						}
					else {
						cbp.add(mPropertyGUI[property.type].getCheckBox(), "1,"+row);
						cbp.add(mPropertyGUI[property.type].getComboBox(), "3,"+row);
						if (isInteractive() && mPropertyGUI[property.type].getComboBox().getItemCount() == 0)
							mPropertyGUI[property.type].getCheckBox().setEnabled(false);
						if (!isInteractive())
							mPropertyGUI[property.type].getComboBox().setEditable(true);
						}
		
					row += 2;
					}
				}

			mTabbedPane.add(TAB_GROUP[tab], cbp);
			}

		content.add(mTabbedPane, "0,3,4,3");
		return content;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/chemistry.html#MolecularProperties";
		}

	@Override
	public Properties getPredefinedConfiguration() {
		if (mTargetColumn == -1)
			return null;

		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_STRUCTURE_COLUMN, mTableModel.getColumnTitleNoAlias(mIDCodeColumn));
		configuration.setProperty(PROPERTY_CHEMPROPERTY_LIST, PROPERTY_CODE[mPropertyIndex]);
		configuration.setProperty(PROPERTY_TARGET_COLUMN, mTableModel.getColumnTitleNoAlias(mTargetColumn));
		return configuration;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		configuration.put(PROPERTY_STRUCTURE_COLUMN, mTableModel.getColumnTitleNoAlias((String)mComboBoxStructureColumn.getSelectedItem()));

		StringBuilder codeList = new StringBuilder();
		for (int i=0; i<mPropertyGUI.length; i++) {
			if (mPropertyGUI[i].getCheckBox().isEnabled() && mPropertyGUI[i].getCheckBox().isSelected()) {
				if (codeList.length() != 0)
					codeList.append(CHEMPROPERTY_LIST_SEPARATOR);

				codeList.append(PROPERTY_CODE[i]);
				if (mPropertyGUI[i].getComboBox() != null) {
					codeList.append(CHEMPROPERTY_OPTION_SEPARATOR);
					codeList.append(mTableModel.getColumnTitleNoAlias(mTableModel.findColumn((String)mPropertyGUI[i].getComboBox().getSelectedItem())));
					}
				}
			}

		if (codeList.length() != 0) {
			configuration.put(PROPERTY_CHEMPROPERTY_LIST, codeList.toString());
			}

		return configuration;
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mComboBoxStructureColumn.getItemCount() != 0)
			mComboBoxStructureColumn.setSelectedIndex(0);
		else if (!isInteractive())
			mComboBoxStructureColumn.setSelectedItem("Structure");

		for (int i=0; i<mPropertyGUI.length; i++) {
			mPropertyGUI[i].getCheckBox().setSelected(false);
			if (mPropertyGUI[i].getComboBox() != null) {
				for (int j=0; j<mPropertyGUI[i].getComboBox().getItemCount(); j++) {
					String item = (String)mPropertyGUI[i].getComboBox().getItemAt(j);
					if (item.contains("Ki ") || item.contains("IC50") || item.contains("EC50")) {
						mPropertyGUI[i].getComboBox().setSelectedItem(j);
						break;
						}
					}
				}
			}

		mTabbedPane.setSelectedIndex(0);
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String value = configuration.getProperty(PROPERTY_STRUCTURE_COLUMN, "");
		if (value.length() != 0) {
			int column = mTableModel.findColumn(value);
			if (column != -1)
				mComboBoxStructureColumn.setSelectedItem(mTableModel.getColumnTitle(column));
			else if (!isInteractive())
				mComboBoxStructureColumn.setSelectedItem(value);
			else if (mComboBoxStructureColumn.getItemCount() != 0)
				mComboBoxStructureColumn.setSelectedIndex(0);
			}
		else if (!isInteractive()) {
			mComboBoxStructureColumn.setSelectedItem("Structure");
			}

		for (int i=0; i<mPropertyGUI.length; i++)
			mPropertyGUI[i].getCheckBox().setSelected(false);

		value = configuration.getProperty(PROPERTY_CHEMPROPERTY_LIST);
		if (value == null)
			return;

		int lowestCheckedTab = Integer.MAX_VALUE;
		String[] codeList = value.split(CHEMPROPERTY_LIST_SEPARATOR_REGEX);
		for (String code:codeList) {
			int optionColumn = -2;
			int index = code.indexOf(CHEMPROPERTY_OPTION_SEPARATOR);
			if (index != -1) {
				String option = code.substring(index+CHEMPROPERTY_OPTION_SEPARATOR.length());
				optionColumn = mTableModel.findColumn(option);
				code = code.substring(0, index);
				}

			DEProperty property = getProperty(code);
			if (property != null && optionColumn != -1) {
				mPropertyGUI[property.type].getCheckBox().setSelected(true);
				if (lowestCheckedTab > property.tab)
					lowestCheckedTab = property.tab;
				if (optionColumn != -2)
					mPropertyGUI[property.type].getComboBox().setSelectedItem(mTableModel.getColumnTitle(optionColumn));
				}
			}

		if (lowestCheckedTab != Integer.MAX_VALUE)
			mTabbedPane.setSelectedIndex(lowestCheckedTab);
		}

	@Override
	public boolean isConfigurable() {
		if (mTableModel.getSpecialColumnList(CompoundTableModel.cColumnTypeIDCode) == null) {
			showErrorMessage("No column with chemical structures found.");
			return false;
			}

		return true;
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (isLive) {
			int idcodeColumn = selectStructureColumn(configuration);
			if (idcodeColumn == -1) {
				showErrorMessage("Structure column not found.");
				return false;
				}
			}
		return true;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	private int selectStructureColumn(Properties configuration) {
		int[] idcodeColumn = mTableModel.getSpecialColumnList(CompoundTableModel.cColumnTypeIDCode);
		if (idcodeColumn.length == 1)
			return idcodeColumn[0];	// there is no choice
		int column = mTableModel.findColumn(configuration.getProperty(PROPERTY_STRUCTURE_COLUMN));
		for (int i=0; i<idcodeColumn.length; i++)
			if (column == idcodeColumn[i])
				return column;
		return -1;
		}

	private void addPropertyOrderIfValid(String propertyCode) {
		int index = propertyCode.indexOf(CHEMPROPERTY_OPTION_SEPARATOR);
		String propertyName = (index == -1) ? propertyCode : propertyCode.substring(0, index);
		DEProperty property = getProperty(propertyName);
		if (property == null) {
			showErrorMessage("Cannot calculate unknown property '"+propertyName+"'.");
			return;
			}

		int dependentColumn = -1;
		if (index != -1) {
			String option = propertyCode.substring(index+CHEMPROPERTY_OPTION_SEPARATOR.length());
			dependentColumn = mTableModel.findColumn(option);
			if (dependentColumn == -1) {
				showErrorMessage("Cannot calculate property '"+propertyName+"': Column '"+option+"' not found.");
				return;
				}
			if (!mTableModel.isColumnTypeDouble(dependentColumn)) {
				showErrorMessage("Cannot calculate property '"+propertyName+"': Column '"+option+"' not numerical.");
				return;
				}
			}

		mPropertyOrderList.add(new DEPropertyOrder(property, dependentColumn));
		}

	private DEProperty getProperty(String propertyName) {
		if (mPropertyMap == null)
			createPropertyMap();

		return mPropertyMap.get(propertyName);
		}

	private void ensurePredictor(int predictorFlags) {
		for (int i=0; i<PREDICTOR_COUNT; i++) {
			int flag = (1 << i);
			if ((predictorFlags & flag) != 0 && mPredictor[i] == null) {
				mPredictor[i] = (flag == PREDICTOR_FLAG_LOGP) ? new CLogPPredictor()
							  : (flag == PREDICTOR_FLAG_LOGS) ? new SolubilityPredictor()
							  : (flag == PREDICTOR_FLAG_PKA && mParentFrame.getApplication().isCapkaAvailable()) ? new PKaPredictor()
							  : (flag == PREDICTOR_FLAG_SURFACE) ? new TotalSurfaceAreaPredictor()
							  : (flag == PREDICTOR_FLAG_DRUGLIKENESS) ? new DruglikenessPredictorWithIndex()
							  : (flag == PREDICTOR_FLAG_TOXICITY) ? new ToxicityPredictor()
							  : (flag == PREDICTOR_FLAG_NASTY_FUNCTIONS) ? new NastyFunctionDetector()
							  : (flag == PREDICTOR_FLAG_FLEXIBILITY) ? new MolecularFlexibilityCalculator()
//		  				  : (flag == PREDICTOR_HERG) ? new RiskOf_hERGActPredictor()
							  : null;
				}
			}
		}

	private void createPropertyMap() {
		mPropertyMap = new TreeMap<>();
		mPropertyTable = new DEProperty[PROPERTY_COUNT];

	   	addProperty(TOTAL_WEIGHT, 0, "Total Molweight", "Total average molweight in g/mol; natural abundance");
		addProperty(FRAGMENT_WEIGHT, 0, "Molweight", "Average molweight of largest fragment in g/mol; natural abundance");
		addProperty(FRAGMENT_ABS_WEIGHT, 0, "Monoisotopic Mass", "Monoisotopic mass of largest fragment in g/mol; most abundant isotopes");
		addProperty(LOGP, 0, "cLogP", "cLogP; P: conc(octanol)/conc(water)", null, null, PREDICTOR_FLAG_LOGP);
		addProperty(LOGS, 0, "cLogS", "cLogS; S: water solubility in mol/l, pH=7.5, 25C", null, null, PREDICTOR_FLAG_LOGS);
		addProperty(LOGD, 0, "cLogD (pH=7.4)", "cLogD at pH=7.4; via logP and ChemAxon pKa", null, null, PREDICTOR_FLAG_LOGP | PREDICTOR_FLAG_PKA);
		addProperty(ACCEPTORS, 0, "H-Acceptors", "H-Acceptors");
		addProperty(DONORS, 0, "H-Donors", "H-Donors");
		addProperty(SASA, 0, "Total Surface Area", "Total Surface Area (SAS Approximation, Van der Waals radii, 1.4Å probe)", null, null, PREDICTOR_FLAG_SURFACE);
		addProperty(REL_PSA, 0, "Relative PSA", "Relative Polar Surface Area (from polar and non-polar SAS Approximation)", null, null, PREDICTOR_FLAG_SURFACE);
		addProperty(TPSA, 0, "Polar Surface Area", "Topological Polar Surface Area (TPSA, P. Ertl approach)", null, null, PREDICTOR_FLAG_SURFACE);
		addProperty(DRUGLIKENESS, 0, "Druglikeness", "Druglikeness", null, DescriptorConstants.DESCRIPTOR_FFP512.shortName, PREDICTOR_FLAG_DRUGLIKENESS);
		addProperty(PERMEABILITY, 0, "Permeability", "Permeability (cMDCK-PLS, F. Broccatelli, DOI:10.1021/acs.molpharmaceut.6b00836)", null, null, PREDICTOR_FLAG_LOGP | PREDICTOR_FLAG_SURFACE | PREDICTOR_FLAG_PKA);

		addProperty(LE, 1, "LE", "Ligand Efficiency (LE) from", "ic50", null, 0);
//		addProperty(SE, 1, "SE", "Surface Efficiency (SE) from", "ic50", null, 0);
		addProperty(LLE, 1, "LLE", "Lipophilic Ligand Efficiency (LLE) from", "ic50", null, PREDICTOR_FLAG_LOGP);
		addProperty(LELP, 1, "LELP", "Ligand Efficiency Lipophilic Price (LELP) from", "ic50", null, PREDICTOR_FLAG_LOGP);
		addProperty(MUTAGENIC, 1, "Mutagenic", "Mutagenic", null, null, PREDICTOR_FLAG_TOXICITY);
		addProperty(TUMORIGENIC, 1, "Tumorigenic", "Tumorigenic", null, null, PREDICTOR_FLAG_TOXICITY);
		addProperty(REPRODUCTIVE_EFECTIVE, 1, "Reproductive Effective", "Reproductive Effective", null, null, PREDICTOR_FLAG_TOXICITY);
		addProperty(IRRITANT, 1, "Irritant", "Irritant", null, null, PREDICTOR_FLAG_TOXICITY);
		addProperty(NASTY_FUNCTIONS, 1, "Nasty Functions", "Nasty Functions", null, DescriptorConstants.DESCRIPTOR_FFP512.shortName, PREDICTOR_FLAG_NASTY_FUNCTIONS);
		addProperty(SHAPE, 1, "Shape Index", "Molecular Shape Index (spherical < 0.5 < linear; 2D-graph based method)");
		addProperty(FLEXIBILITY, 1, "Molecular Flexibility", "Molecular Flexibility (low < 0.5 < high)", null, null, PREDICTOR_FLAG_FLEXIBILITY);
		addProperty(COMPLEXITY, 1, "Molecular Complexity", "Molecular Complexity (low < 0.5 < high)");

		addProperty(FRAGMENTS, 2, "Fragments", "Disconnected Fragment Count");
		addProperty(HEAVY_ATOMS, 2, "Non-H Atoms", "Non-Hydrogen Atom Count");
		addProperty(NONCARBON_ATOMS, 2, "Non-C/H Atoms", "Non-Carbon/Hydrogen Atom Count");
		addProperty(METAL_ATOMS, 2, "Metal-Atoms", "Metal-Atom Count");
		addProperty(NEGATIVE_ATOMS, 2, "Electronegative Atoms", "Electronegative Atom Count (N, O, P, S, F, Cl, Br, I, As, Se)");
		addProperty(STEREOCENTERS, 2, "Stereo Centers", "Stereo Center Count");
		addProperty(ROTATABLE_BONDS, 2, "Rotatable Bonds", "Rotatable Bond Count");
		addProperty(RING_CLOSURES, 2, "Rings Closures", "Ring Closure Count");
		addProperty(AROMATIC_ATOMS, 2, "Aromatic Atoms", "Aromatic Atom Count");
		addProperty(SP3_CARBON_FRACTION, 2, "sp3-Carbon Fraction", "sp3-Carbon Count / Total Carbon Count");
		addProperty(SP3_ATOMS, 2, "sp3-Atoms", "sp3-Atom Count (Considering C,N,O,P,S)");
		addProperty(SYMMETRIC_ATOMS, 2, "Symmetric atoms", "Symmetric Atom Count");

		addProperty(SMALL_RINGS, 3, "Small Rings", "Small Ring Count (all rings up to 7 members)");
		addProperty(SMALL_CARBO_RINGS, 3, "Carbo-Rings", "Small Ring Count without Hereo Atoms");
		addProperty(SMALL_HETERO_RINGS, 3, "Hetero-Rings", "Small Ring Count with Hetero Atoms");
		addProperty(SATURATED_RINGS, 3, "Saturated Rings", "Small Fully Saturated Ring Count");
		addProperty(NON_AROMATIC_RINGS, 3, "Non-Aromatic Rings", "Small Non-Aromatic Ring Count");
		addProperty(AROMATIC_RINGS, 3, "Aromatic Rings", "Aromatic Ring Count");
		addProperty(CARBO_SATURATED_RINGS, 3, "Saturated Carbo-Rings", "Small Saturated Carbo-Ring Count");
		addProperty(CARBO_NON_AROMATIC_RINGS, 3, "Non-Aromatic Carbo-Rings", "Small Carbo-Non-Aromatic Ring Count");
		addProperty(CARBO_AROMATIC_RINGS, 3, "Carbo-Aromatic Rings", "Carbo-Aromatic Ring Count");
		addProperty(HETERO_SATURATED_RINGS, 3, "Saturated Hetero-Rings", "Small Saturated Hetero-Ring Count");
		addProperty(HETERO_NON_AROMATIC_RINGS, 3, "Non-Aromatic Hetero-Rings", "Small Hetero-Non-Aromatic Ring Count");
		addProperty(HETERO_AROMATIC_RINGS, 3, "Hetero-Aromatic Rings", "Hetero-Aromatic Ring Count");

		addProperty(ALL_AMIDES, 4, "Amides", "Amide Nitrogen Count (includes imides and sulfonamides)");
		addProperty(ALL_AMINES, 4, "Amines", "Amine Count (excludes enamines, aminales, etc.)");
		addProperty(ALKYL_AMINES, 4, "Alkyl-Amines", "Alkyl-Amine Count (excludes Aryl-,Alkyl-Amines)");
		addProperty(ARYL_AMINES, 4, "Aromatic Amines", "Aryl-Amine Count (includes Aryl-,Alkyl-Amines)");
		addProperty(AROMATIC_NITROGEN, 4, "Aromatic Nitrogens", "Aromatic Nitrogen Atom Count");
		addProperty(BASIC_NITROGEN, 4, "Basic Nitrogens", "Basic Nitrogen Atom Count (rough estimate: pKa above 7)");
		addProperty(ACIDIC_OXYGEN, 4, "Acidic Oxygens", "Acidic Oxygen Atom Count (rough estimate: pKa below 7)");
		addProperty(STEREO_CONFIGURATION, 4, "Stereo Configuration", "Stereo isomer count and relation (e.g. 'racemate', '4 diastereomers', '2 epimers'");

		addProperty(ACIDIC_PKA, 5, "acidic pKa", "lowest acidic pKa; ChemAxon method", null, null, PREDICTOR_FLAG_PKA);
		addProperty(BASIC_PKA, 5, "basic pKa", "highest basic pKa; ChemAxon method", null, null, PREDICTOR_FLAG_PKA);

		addProperty(FRACTION_IA, 5, "Fraction Ionized Acid", "Fraction Ionized Acid (Henderson–Hasselbalch, ChemAxon pKa)", null, null, PREDICTOR_FLAG_PKA);
		addProperty(FRACTION_IB, 5, "Fraction Ionized Base", "Fraction Ionized Base (Henderson–Hasselbalch, ChemAxon pKa)", null, null, PREDICTOR_FLAG_PKA);
		addProperty(FRACTION_ZI, 5, "Fraction Zwitter Ions", "Fraction Zwitter Ions (based on ChemAxon pKa)", null, null, PREDICTOR_FLAG_PKA);
		addProperty(FRACTION_CHARGED, 5, "Fraction Charged", "Fraction Charged (based on ChemAxon pKa)", null, null, PREDICTOR_FLAG_PKA);
		addProperty(FRACTION_UNCHARGED, 5, "Fraction Uncharged", "Fraction Uncharged (based on ChemAxon pKa)", null, null, PREDICTOR_FLAG_PKA);
		addProperty(CHARGE74, 5, "Charge (pH 7.4)", "Charge at pH=7.4 (based on ChemAxon pKa)", null, null, PREDICTOR_FLAG_PKA);

		addProperty(GLOBULARITY_SVD, 6, "Globularity SVD", "Globularity (flat/linear < 0.5 < spherical) using singular value decomposition of 3D-atom coordinates");
		addProperty(GLOBULARITY_VOL, 6, "Globularity Vol", "Globularity (non-spherical < 0.9 < spherical) from molecule volume and surface (VDW-radii, 1.4 A probe)");
		addProperty(SURFACE_3D, 6, "VDW-Surface", "Solvent excluded surface area (Van der Waals surface) using VDW-radii and 1.4 A probe");
		addProperty(VOLUME_3D, 6, "VDW-Volume", "Molecule volume inside solvent excluded surface using VDW-radii and 1.4 A probe");

		addBackgroundColor(MUTAGENIC, VisualizationColor.cColorListModeCategories, TOX_COLOR_LIST);
		addBackgroundColor(TUMORIGENIC, VisualizationColor.cColorListModeCategories, TOX_COLOR_LIST);
		addBackgroundColor(REPRODUCTIVE_EFECTIVE, VisualizationColor.cColorListModeCategories, TOX_COLOR_LIST);
		addBackgroundColor(IRRITANT, VisualizationColor.cColorListModeCategories, TOX_COLOR_LIST);
		}

	private void addProperty(int type, int tab, String columnTitle, String description) {
		addProperty(type, tab, columnTitle, description, null, null, 0);
		}

	private void addProperty(int type, int tab, String columnTitle, String description, String dependentColumnFilter,
							 String descriptorName, int predictorFlags) {
		DEProperty property = new DEProperty(type, tab, columnTitle, description, dependentColumnFilter,
											 descriptorName, predictorFlags);
		mPropertyTable[mPropertyMap.size()] = property;
		mPropertyMap.put(PROPERTY_CODE[type], property);
		}

	private void addBackgroundColor(int type, int colorMode, Color[] colorList) {
		mPropertyMap.get(PROPERTY_CODE[type]).backgroundColor = new DEBackgroundColor(colorMode, colorList);
		}

	@Override
	public void runTask(Properties configuration) {
		mIDCodeColumn = selectStructureColumn(configuration);

		String value = configuration.getProperty(PROPERTY_CHEMPROPERTY_LIST);
		if (value == null) {
			showErrorMessage("Property list missing.");
			return;
			}

		String[] codeList = value.split(CHEMPROPERTY_LIST_SEPARATOR_REGEX);
		mPropertyOrderList = new ArrayList<>();
		for (String code:codeList)
			addPropertyOrderIfValid(code);

		if (mPropertyOrderList.size() == 0)
			return;

		String[] columnName = new String[mPropertyOrderList.size()];
		int column = 0;
		mPredictor = new Object[PREDICTOR_COUNT];
		for (DEPropertyOrder order:mPropertyOrderList) {
			columnName[column++] = order.getColumnTitle();
			ensurePredictor(order.property.predictorFlags);
			}

		boolean fragFpNeeded = false;
		boolean pp3DNeeded = false;
		for (DEPropertyOrder order:mPropertyOrderList) {
			if (order.property.descriptorName != null) {
				if (order.property.descriptorName.equals(DescriptorConstants.DESCRIPTOR_FFP512.shortName))
					fragFpNeeded = true;
				if (order.property.descriptorName.equals(DescriptorConstants.DESCRIPTOR_Flexophore.shortName))
					pp3DNeeded = true;
				}
			}

		mFragFpColumn = -1;
		if (fragFpNeeded) {
			mFragFpColumn = mTableModel.getChildColumn(mIDCodeColumn, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
			if (mFragFpColumn == -1)
				mFragFpColumn = mTableModel.addDescriptorColumn(mIDCodeColumn, DescriptorConstants.DESCRIPTOR_FFP512.shortName, null);

			waitForDescriptor(mTableModel, mFragFpColumn);
			if (threadMustDie())
				return;
			}
		mFlexophoreColumn = -1;
		if (pp3DNeeded) {
			mFlexophoreColumn = mTableModel.getChildColumn(mIDCodeColumn, DescriptorConstants.DESCRIPTOR_Flexophore.shortName);
			if (mFlexophoreColumn == -1)
				mFlexophoreColumn = mTableModel.addDescriptorColumn(mIDCodeColumn, DescriptorConstants.DESCRIPTOR_Flexophore.shortName, null);

			waitForDescriptor(mTableModel, mFlexophoreColumn);
			if (threadMustDie())
				return;
			}

		if (threadMustDie())
			return;

		final int firstPropertyColumn = mTargetColumn != -1 ? mTargetColumn : mTableModel.addNewColumns(columnName);

		startProgress("Calculating properties...", 0, mTableModel.getTotalRowCount());

		if (mPredictor[PREDICTOR_PKA] == null)
			finishTaskMultiCore(firstPropertyColumn);
		else
			finishTaskSingleCore(firstPropertyColumn);
		}

	private void finishTaskSingleCore(final int firstPropertyColumn) {
		int errorCount = 0;

		StereoMolecule containerMol = new StereoMolecule();
		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			if ((row % 16) == 15)
				updateProgress(row);

			try {
				processRow(row, firstPropertyColumn, containerMol);
				}
			catch (Exception e) {
				errorCount++;
				}
			}

		if (!threadMustDie() && errorCount != 0)
			showErrorMessage("The task '"+TASK_NAME+"' failed on "+errorCount+" molecules.");

		finalizeTableModel(firstPropertyColumn);
		}

	private void finishTaskMultiCore(final int firstPropertyColumn) {
		int threadCount = Runtime.getRuntime().availableProcessors();
		mSMPRecordIndex = new AtomicInteger(mTableModel.getTotalRowCount());
		mSMPWorkingThreads = new AtomicInteger(threadCount);
		mSMPErrorCount = new AtomicInteger(0);

		Thread[] t = new Thread[threadCount];
		for (int i=0; i<threadCount; i++) {
			t[i] = new Thread("Chemical Property Calculator "+(i+1)) {
				public void run() {
					StereoMolecule containerMol = new StereoMolecule();
					int recordIndex = mSMPRecordIndex.decrementAndGet();
					while (recordIndex >= 0 && !threadMustDie()) {
						try {
							processRow(recordIndex, firstPropertyColumn, containerMol);
							}
						catch (Exception e) {
							mSMPErrorCount.incrementAndGet();
							e.printStackTrace();
							}

						updateProgress(-1);
						recordIndex = mSMPRecordIndex.decrementAndGet();
						}

					if (mSMPWorkingThreads.decrementAndGet() == 0) {
						if (!threadMustDie() && mSMPErrorCount.get() != 0)
							showErrorMessage("The task '"+TASK_NAME+"' failed on "+mSMPErrorCount.get()+" molecules.");

   						finalizeTableModel(firstPropertyColumn);
						}
					}
				};
			t[i].setPriority(Thread.MIN_PRIORITY);
			t[i].start();
			}

		// the controller thread must wait until all others are finished
		// before the next task can begin or the dialog is closed
		for (int i=0; i<threadCount; i++)
			try { t[i].join(); } catch (InterruptedException e) {}
		}

	private void finalizeTableModel(int firstPropertyColumn) {
		if (mTargetColumn != -1) {
			mTableModel.finalizeChangeAlphaNumericalColumn(mTargetColumn, 0, mTableModel.getTotalRowCount());
			}
		else {
			mTableModel.finalizeNewColumns(firstPropertyColumn, this);

			DETableView tableView = mParentFrame.getMainFrame().getMainPane().getTableView();
			if (tableView != null) {
				int column = firstPropertyColumn;
				for (DEPropertyOrder order:mPropertyOrderList) {
					mTableModel.setColumnProperty(column, CompoundTableConstants.cColumnPropertyCompoundProperty,
							PROPERTY_CODE[order.property.type]+"@"+mTableModel.getColumnTitleNoAlias(mIDCodeColumn));
					if (order.property.backgroundColor != null) {
						VisualizationColor vc = tableView.getColorHandler().getVisualizationColor(column, CompoundTableColorHandler.BACKGROUND);
						vc.setColor(column, order.property.backgroundColor.colorList, order.property.backgroundColor.colorMode);
						}
					column++;
					}
				}
			}
		}

	private void processRow(int row, int firstNewColumn, StereoMolecule containerMol) throws Exception {
		RowInfo rowInfo = new RowInfo(row, containerMol);

		int currentColumn = firstNewColumn;
		for (DEPropertyOrder order:mPropertyOrderList) {
			if (rowInfo.hasMolecule()) {
				String value = predict(row, order.property.type, order.dependentColumn, rowInfo);
				mTableModel.setTotalValueAt(value, row, currentColumn);
				}
			else {
				mTableModel.setTotalValueAt("", row, currentColumn);
				}
			currentColumn++;
			}
		}

	/**
	 * If the property is numerical and if it is in the cache, then it is returned from the cache.
	 * Otherwise it is predicted, cached (if numerical) and returned.
	 * Properties that depend on other properties call this function recursively.
	 * @param row
	 * @param propertyType
	 * @param dependentColumn
	 * @param rowInfo
	 * @return
	 */
	private String predict(int row, int propertyType, int dependentColumn, RowInfo rowInfo) {

		// nasty functions have no numerical value
		if (propertyType == NASTY_FUNCTIONS)
			return ((NastyFunctionDetector)mPredictor[PREDICTOR_NASTY_FUNCTIONS]).getNastyFunctionString(rowInfo.mol,
				(long[])mTableModel.getTotalRecord(row).getData(mFragFpColumn));

		Double numValue = rowInfo.cache.get(propertyType);
		double value = Double.NaN;
		if (numValue != null) {
			value = numValue.doubleValue();
			}
		else {
			try {
				value = predictNumerical(row, propertyType, dependentColumn, rowInfo);
				}
			catch (Exception e) {
				return e.getMessage();
				}
			}

		switch (propertyType) {
			case TOTAL_WEIGHT:
			case FRAGMENT_WEIGHT:
				return DoubleFormat.toString(value, 6);
			case FRAGMENT_ABS_WEIGHT:
				return DoubleFormat.toString(value, 9);
			case ACCEPTORS:
			case DONORS:
			case HEAVY_ATOMS:
			case NONCARBON_ATOMS:
			case METAL_ATOMS:
			case NEGATIVE_ATOMS:
			case STEREOCENTERS:
			case ROTATABLE_BONDS:
			case FRAGMENTS:
			case RING_CLOSURES:
			case SMALL_RINGS:
			case AROMATIC_RINGS:
			case AROMATIC_ATOMS:
			case SP3_ATOMS:
			case SYMMETRIC_ATOMS:
			case ALL_AMIDES:
			case ALL_AMINES:
			case ALKYL_AMINES:
			case ARYL_AMINES:
			case AROMATIC_NITROGEN:
			case BASIC_NITROGEN:
			case ACIDIC_OXYGEN:
				return Integer.toString((int)value);
			case STEREO_CONFIGURATION:
				return rowInfo.mol.getChiralText();
			case MUTAGENIC:
			case TUMORIGENIC:
			case REPRODUCTIVE_EFECTIVE:
			case IRRITANT:
				return ToxicityPredictor.RISK_NAME[(int)value];
			case ACIDIC_PKA:
			case BASIC_PKA:
				return Double.isNaN(value) ? "" : DoubleFormat.toString(value);
			default:
				return DoubleFormat.toString(value);
			}
		}

	/**
	 * If the property is numerical and if it is in the cache, then it is returned from the cache.
	 * Otherwise it is predicted, cached (if numerical) and returned.
	 * Properties that depend on other properties call this function recursively.
	 * @param row
	 * @param propertyType
	 * @param dependentColumn
	 * @param rowInfo
	 * @return
	 */
	private double predictNumerical(int row, int propertyType, int dependentColumn, RowInfo rowInfo) throws Exception {
		Double cachedValue = rowInfo.cache.get(propertyType);
		if (cachedValue != null)
			return cachedValue;

		StereoMolecule mol = rowInfo.mol;
		chemaxon.struc.Molecule camol = rowInfo.camol;
		TreeMap<Integer, Double> cache = rowInfo.cache;
		ConformerSet cs;

		double value = Double.NaN;
		double logP,fia,fib;
		RingCollection rc;
		switch (propertyType) {
			case TOTAL_WEIGHT:
				value = 0;	// if totalWeight is not already cached, we have no atoms
				break;
			case FRAGMENT_WEIGHT:
				value = new MolecularFormula(mol).getRelativeWeight();
				break;
			case FRAGMENT_ABS_WEIGHT:
				value = new MolecularFormula(mol).getAbsoluteWeight();
				break;
			case LOGP:
				value = ((CLogPPredictor)mPredictor[PREDICTOR_LOGP]).assessCLogP(mol);
				break;
			case LOGS:
				value = ((SolubilityPredictor)mPredictor[PREDICTOR_LOGS]).assessSolubility(mol);
				break;
			case LOGD:
				if (rowInfo.camol == null)
					throw new Exception("molecule conversion error");

				final double LOGD_PH = 7.4f;
				logP = predictNumerical(row, LOGP, dependentColumn, rowInfo);
				double aPKa = predictNumerical(row, ACIDIC_PKA, dependentColumn, rowInfo);
				double bPKa = predictNumerical(row, BASIC_PKA, dependentColumn, rowInfo);
				value = (Double.isNaN(aPKa) && Double.isNaN(bPKa)) ? logP
						: Double.isNaN(aPKa) ? logP - Math.log10(1.0 + Math.pow(10, bPKa-LOGD_PH))
						: Double.isNaN(bPKa) ? logP - Math.log10(1.0 + Math.pow(10, LOGD_PH-aPKa))
						: (LOGD_PH-aPKa > bPKa-LOGD_PH) ?
						logP - Math.log10(1.0 + Math.pow(10, LOGD_PH-aPKa))
						: logP - Math.log10(1.0 + Math.pow(10, bPKa-LOGD_PH));
				break;
			case ACCEPTORS:
				value = 0;
				for (int atom=0; atom<mol.getAllAtoms(); atom++)
					if ((mol.getAtomicNo(atom) == 7 || mol.getAtomicNo(atom) == 8) && mol.getAtomCharge(atom) <= 0)
						value++;
				break;
			case DONORS:
				value = 0;
				for (int atom=0; atom<mol.getAllAtoms(); atom++)
					if ((mol.getAtomicNo(atom) == 7 || mol.getAtomicNo(atom) == 8) && mol.getAllHydrogens(atom) > 0)
						value++;
				break;
			case SASA:
				value = ((TotalSurfaceAreaPredictor)mPredictor[PREDICTOR_SURFACE]).assessTotalSurfaceArea(mol);
				break;
			case REL_PSA:
				value = ((TotalSurfaceAreaPredictor)mPredictor[PREDICTOR_SURFACE]).assessRelativePolarSurfaceArea(mol);
				break;
			case TPSA:
				value = ((TotalSurfaceAreaPredictor)mPredictor[PREDICTOR_SURFACE]).assessPSA(mol);
				break;
			case DRUGLIKENESS:
				value = ((DruglikenessPredictorWithIndex)mPredictor[PREDICTOR_DRUGLIKENESS]).assessDruglikeness(mol,
						(long[])mTableModel.getTotalRecord(row).getData(mFragFpColumn), this);
				break;
			case LE:	// dG / HA
				// dG = -RT*ln(Kd) with R=1.986cal/(K*mol); T=300K; dG in kcal/mol
				// We use IC50 instead of Kd, which is acceptable according to
				// Andrew L. Hopkins, Colin R. Groom, Alexander Alex
				// Drug Discovery Today, Volume 9, Issue 10, 15 May 2004, Pages 430-431
				if (dependentColumn != -1) {
					double ic50 = mTableModel.getTotalOriginalDoubleAt(row, dependentColumn);
					if (!Double.isNaN(ic50))
						value = - 1.986 * 0.300 * Math.log(0.000000001 * ic50) / mol.getAtoms();
					}
				break;
/*			case SE:	// dG / molecule surface
						// dG = -RT*ln(Kd) with R=1.986cal/(K*mol); T=300K; dG in kcal/mol
						// We use IC50 instead of Kd, which is acceptable according to
						// Andrew L. Hopkins, Colin R. Groom, Alexander Alex
						// Drug Discovery Today, Volume 9, Issue 10, 15 May 2004, Pages 430-431
						// surface = sMethane * pow(nAtoms, 2/3); surface grows less quickly than volume!
						if (property.dependentColumn != -1) {
							double ic50 = mTableModel.getTotalOriginalDoubleAt(row, property.dependentColumn);
							if (!Double.isNaN(ic50)) {
								double dG = - 1.986 * 0.300 * Math.log(0.000000001 * ic50);
								double se = dG / (4.43 * Math.pow(mol.getAtoms(), 0.6666666667));
								value = DoubleFormat.toString(se);
								}
							}
						break;	*/
			case LLE:	// pIC50 - logP
				if (dependentColumn != -1) {
					double ic50 = mTableModel.getTotalOriginalDoubleAt(row, dependentColumn);
					if (!Double.isNaN(ic50)) {
						double pic50 = - Math.log10(0.000000001 * ic50);
						value = pic50 - ((CLogPPredictor)mPredictor[PREDICTOR_LOGP]).assessCLogP(mol);
						}
					}
				break;
			case LELP:	// logP / LE
				if (dependentColumn != -1) {
					double ic50 = mTableModel.getTotalOriginalDoubleAt(row, dependentColumn);
					if (!Double.isNaN(ic50)) {
						double le = - 1.986 * 0.300 * Math.log(0.000000001 * ic50) / mol.getAtoms();
						value = ((CLogPPredictor)mPredictor[PREDICTOR_LOGP]).assessCLogP(mol) / le;
						}
					}
				break;
			case MUTAGENIC:
				value = ((ToxicityPredictor)mPredictor[PREDICTOR_TOXICITY]).assessRisk(mol, ToxicityPredictor.cRiskTypeMutagenic, this);
				break;
			case TUMORIGENIC:
				value = ((ToxicityPredictor)mPredictor[PREDICTOR_TOXICITY]).assessRisk(mol, ToxicityPredictor.cRiskTypeTumorigenic, this);
				break;
			case REPRODUCTIVE_EFECTIVE:
				value = ((ToxicityPredictor)mPredictor[PREDICTOR_TOXICITY]).assessRisk(mol, ToxicityPredictor.cRiskTypeReproductiveEffective, this);
				break;
			case IRRITANT:
				value = ((ToxicityPredictor)mPredictor[PREDICTOR_TOXICITY]).assessRisk(mol, ToxicityPredictor.cRiskTypeIrritant, this);
				break;
/*			case HERG_RISK:
						value = ((RiskOf_hERGActPredictor)predictor[PREDICTOR_HERG]).assess_hERGRisk(mol, mProgressDialog);
						break;*/
			case SHAPE:
				value = MolecularShapeCalculator.assessShape(mol);
				break;
			case FLEXIBILITY:
				value = ((MolecularFlexibilityCalculator)mPredictor[PREDICTOR_FLEXIBILITY]).calculateMolecularFlexibility(mol);
				break;
			case COMPLEXITY:
				value = FastMolecularComplexityCalculator.assessComplexity(mol);
				break;
			case HEAVY_ATOMS:
				value = mol.getAtoms();
				break;
			case NONCARBON_ATOMS:
				value = 0;
				mol.ensureHelperArrays(Molecule.cHelperNeighbours);
				for (int atom=0; atom<mol.getAtoms(); atom++)
					if (mol.getAtomicNo(atom) != 6)
						value++;
				break;
			case METAL_ATOMS:
				value = 0;
				mol.ensureHelperArrays(Molecule.cHelperNeighbours);
				for (int atom=0; atom<mol.getAtoms(); atom++)
					if (mol.isMetalAtom(atom))
						value++;
				break;
			case NEGATIVE_ATOMS:
				value = 0;
				mol.ensureHelperArrays(Molecule.cHelperNeighbours);
				for (int atom=0; atom<mol.getAtoms(); atom++)
					if (mol.isElectronegative(atom))
						value++;
				break;
			case STEREOCENTERS:
				value = mol.getStereoCenterCount();
				break;
			case ROTATABLE_BONDS:
				value = mol.getRotatableBondCount();
				break;
			case RING_CLOSURES:
				int[] fNo = new int[mol.getAllAtoms()];
				int fragments = mol.getFragmentNumbers(fNo, false, false);
				value = fragments + mol.getAllBonds() - mol.getAllAtoms();
				break;
			case AROMATIC_ATOMS:
				value = 0;
				mol.ensureHelperArrays(Molecule.cHelperRings);
				for (int atom=0; atom<mol.getAtoms(); atom++)
					if (mol.isAromaticAtom(atom))
						value++;
				break;
			case SP3_CARBON_FRACTION:
				value = 0;
				int count = 0;
				mol.ensureHelperArrays(Molecule.cHelperRings);
				for (int atom=0; atom<mol.getAtoms(); atom++) {
					if (mol.getAtomicNo(atom) == 6) {
						count++;
						if (mol.getAtomPi(atom) == 0 && mol.getAtomCharge(atom) <= 0)
							value++;
						}
					}
				value /= count;
				break;
			case SP3_ATOMS:
				value = 0;
				mol.ensureHelperArrays(Molecule.cHelperRings);
				for (int atom=0; atom<mol.getAtoms(); atom++)
					if ((mol.getAtomicNo(atom) == 6 && mol.getAtomPi(atom) == 0)
							|| (mol.getAtomicNo(atom) == 7 && !mol.isFlatNitrogen(atom))
							|| (mol.getAtomicNo(atom) == 8 && mol.getAtomPi(atom) == 0 && !mol.isAromaticAtom(atom))
							|| (mol.getAtomicNo(atom) == 15)
							|| (mol.getAtomicNo(atom) == 16 && !mol.isAromaticAtom(atom)))
						value++;
				break;
			case SYMMETRIC_ATOMS:
				mol.ensureHelperArrays(Molecule.cHelperSymmetrySimple);
				int maxRank = 0;
				for (int atom=0; atom<mol.getAtoms(); atom++)
					if (maxRank < mol.getSymmetryRank(atom))
						maxRank = mol.getSymmetryRank(atom);
				value = (mol.getAtoms()-maxRank);
				break;
			case SMALL_RINGS:
				mol.ensureHelperArrays(Molecule.cHelperRings);
				value = mol.getRingSet().getSize();
				break;
			case SMALL_CARBO_RINGS:
				value = 0;
				mol.ensureHelperArrays(Molecule.cHelperRings);
				rc = mol.getRingSet();
				for (int i=0; i<rc.getSize(); i++) {
					boolean found = false;
					int[] ra = rc.getRingAtoms(i);
					for (int a:ra) {
						if (mol.getAtomicNo(a) != 6) {
							found = true;
							break;
						}
					}
					if (!found)
						value++;
				}
				break;
			case SMALL_HETERO_RINGS:
				value = 0;
				mol.ensureHelperArrays(Molecule.cHelperRings);
				rc = mol.getRingSet();
				for (int i=0; i<rc.getSize(); i++) {
					boolean found = false;
					int[] ra = rc.getRingAtoms(i);
					for (int a:ra) {
						if (mol.getAtomicNo(a) != 6) {
							found = true;
							break;
						}
					}
					if (found)
						value++;
				}
				break;
			case SATURATED_RINGS:
				value = 0;
				mol.ensureHelperArrays(Molecule.cHelperRings);
				rc = mol.getRingSet();
				for (int i=0; i<rc.getSize(); i++) {
					if (!rc.isAromatic(i)) {
						boolean found = false;
						int[] rb = rc.getRingBonds(i);
						for (int b:rb) {
							if (mol.getBondOrder(b) > 1) {
								found = true;
								break;
							}
						}
						if (!found)
							value++;
					}
				}
				break;
			case NON_AROMATIC_RINGS:
				value = 0;
				mol.ensureHelperArrays(Molecule.cHelperRings);
				rc = mol.getRingSet();
				for (int i=0; i<rc.getSize(); i++)
					if (!rc.isAromatic(i))
						value++;
				break;
			case AROMATIC_RINGS:
				value = 0;
				mol.ensureHelperArrays(Molecule.cHelperRings);
				rc = mol.getRingSet();
				for (int i=0; i<rc.getSize(); i++)
					if (rc.isAromatic(i))
						value++;
				break;
			case CARBO_SATURATED_RINGS:
				value = 0;
				mol.ensureHelperArrays(Molecule.cHelperRings);
				rc = mol.getRingSet();
				for (int i=0; i<rc.getSize(); i++) {
					if (!rc.isAromatic(i)) {
						boolean afound = false;
						int[] ra = rc.getRingAtoms(i);
						for (int a:ra) {
							if (mol.getAtomicNo(a) != 6) {
								afound = true;
								break;
							}
						}
						boolean bfound = false;
						int[] rb = rc.getRingBonds(i);
						for (int b:rb) {
							if (mol.getBondOrder(b) > 1) {
								bfound = true;
								break;
							}
						}
						if (!afound && !bfound)
							value++;
					}
				}
				break;
			case CARBO_NON_AROMATIC_RINGS:
				value = 0;
				mol.ensureHelperArrays(Molecule.cHelperRings);
				rc = mol.getRingSet();
				for (int i=0; i<rc.getSize(); i++) {
					if (!rc.isAromatic(i)) {
						boolean afound = false;
						int[] ra = rc.getRingAtoms(i);
						for (int a:ra) {
							if (mol.getAtomicNo(a) != 6) {
								afound = true;
								break;
							}
						}
						if (!afound)
							value++;
					}
				}
				break;
			case CARBO_AROMATIC_RINGS:
				value = 0;
				mol.ensureHelperArrays(Molecule.cHelperRings);
				rc = mol.getRingSet();
				for (int i=0; i<rc.getSize(); i++) {
					if (rc.isAromatic(i)) {
						boolean afound = false;
						int[] ra = rc.getRingAtoms(i);
						for (int a:ra) {
							if (mol.getAtomicNo(a) != 6) {
								afound = true;
								break;
							}
						}
						if (!afound)
							value++;
					}
				}
				break;
			case HETERO_SATURATED_RINGS:
				value = 0;
				mol.ensureHelperArrays(Molecule.cHelperRings);
				rc = mol.getRingSet();
				for (int i=0; i<rc.getSize(); i++) {
					if (!rc.isAromatic(i)) {
						boolean afound = false;
						int[] ra = rc.getRingAtoms(i);
						for (int a:ra) {
							if (mol.getAtomicNo(a) != 6) {
								afound = true;
								break;
							}
						}
						boolean bfound = false;
						int[] rb = rc.getRingBonds(i);
						for (int b:rb) {
							if (mol.getBondOrder(b) > 1) {
								bfound = true;
								break;
							}
						}
						if (afound && !bfound)
							value++;
					}
				}
				break;
			case HETERO_NON_AROMATIC_RINGS:
				value = 0;
				mol.ensureHelperArrays(Molecule.cHelperRings);
				rc = mol.getRingSet();
				for (int i=0; i<rc.getSize(); i++) {
					if (!rc.isAromatic(i)) {
						boolean afound = false;
						int[] ra = rc.getRingAtoms(i);
						for (int a:ra) {
							if (mol.getAtomicNo(a) != 6) {
								afound = true;
								break;
							}
						}
						if (afound)
							value++;
					}
				}
				break;
			case HETERO_AROMATIC_RINGS:
				value = 0;
				mol.ensureHelperArrays(Molecule.cHelperRings);
				rc = mol.getRingSet();
				for (int i=0; i<rc.getSize(); i++) {
					if (rc.isAromatic(i)) {
						boolean afound = false;
						int[] ra = rc.getRingAtoms(i);
						for (int a:ra) {
							if (mol.getAtomicNo(a) != 6) {
								afound = true;
								break;
							}
						}
						if (afound)
							value++;
					}
				}
				break;
			case ALL_AMIDES:
				value = 0;
				mol.ensureHelperArrays(Molecule.cHelperNeighbours);
				for (int atom=0; atom<mol.getAtoms(); atom++)
					if (AtomFunctionAnalyzer.isAmide(mol, atom))
						value++;
				break;
			case ALL_AMINES:
				value = 0;
				mol.ensureHelperArrays(Molecule.cHelperRings);
				for (int atom=0; atom<mol.getAtoms(); atom++)
					if (AtomFunctionAnalyzer.isAmine(mol, atom))
						value++;
				break;
			case ALKYL_AMINES:
				value = 0;
				mol.ensureHelperArrays(Molecule.cHelperRings);
				for (int atom=0; atom<mol.getAtoms(); atom++)
					if (AtomFunctionAnalyzer.isAlkylAmine(mol, atom))
						value++;
				break;
			case ARYL_AMINES:
				value = 0;
				mol.ensureHelperArrays(Molecule.cHelperRings);
				for (int atom=0; atom<mol.getAtoms(); atom++)
					if (AtomFunctionAnalyzer.isArylAmine(mol, atom))
						value++;
				break;
			case AROMATIC_NITROGEN:
				value = 0;
				mol.ensureHelperArrays(Molecule.cHelperRings);
				for (int atom=0; atom<mol.getAtoms(); atom++)
					if (mol.getAtomicNo(atom) == 7 && mol.isAromaticAtom(atom))
						value++;
				break;
			case BASIC_NITROGEN:
				value = 0;
				mol.ensureHelperArrays(Molecule.cHelperRings);
				for (int atom=0; atom<mol.getAtoms(); atom++)
					if (AtomFunctionAnalyzer.isBasicNitrogen(mol, atom))
						value++;
				break;
			case ACIDIC_OXYGEN:
				value = 0;
				mol.ensureHelperArrays(Molecule.cHelperRings);
				for (int atom=0; atom<mol.getAtoms(); atom++)
					if (AtomFunctionAnalyzer.isAcidicOxygen(mol, atom))
						value++;
				break;
			case ACIDIC_PKA:
				if (mPredictor[PREDICTOR_PKA] != null) {
					if (camol == null)
						throw new Exception("molecule conversion error");
					value = ((PKaPredictor)mPredictor[PREDICTOR_PKA]).getMostAcidicPKas(camol)[0];
					}
				break;
			case BASIC_PKA:
				if (mPredictor[PREDICTOR_PKA] != null) {
					if (camol == null)
						throw new Exception("molecule conversion error");
					value = ((PKaPredictor)mPredictor[PREDICTOR_PKA]).getMostBasicPKas(camol)[0];
					}
				break;
			case FRACTION_IA:
				if (mPredictor[PREDICTOR_PKA] != null) {
					double acidicpKa = predictNumerical(row, ACIDIC_PKA, dependentColumn, rowInfo);
					value = Double.isNaN(acidicpKa) ? 0.0 : Math.pow(10, 7.4 - acidicpKa) / (1.0 + Math.pow(10, 7.4 - acidicpKa));
					}
				break;
			case FRACTION_IB:
				if (mPredictor[PREDICTOR_PKA] != null) {
					double basicpKa = predictNumerical(row, BASIC_PKA, dependentColumn, rowInfo);
					value = Double.isNaN(basicpKa) ? 0.0 : Math.pow(10, basicpKa - 7.4) / (1.0 + Math.pow(10, basicpKa - 7.4));
					}
				break;
			case FRACTION_ZI:
				if (mPredictor[PREDICTOR_PKA] != null) {
					fia = predictNumerical(row, FRACTION_IA, dependentColumn, rowInfo);
					fib = predictNumerical(row, FRACTION_IB, dependentColumn, rowInfo);
					if (!Double.isNaN(fia) && !Double.isNaN(fib))
						value = fia * fib;
					}
				break;
			case FRACTION_CHARGED:
				if (mPredictor[PREDICTOR_PKA] != null) {
					double fuc = predictNumerical(row, FRACTION_UNCHARGED, dependentColumn, rowInfo);
					if (!Double.isNaN(fuc))
						value = 1.0 - fuc;
					}
				break;
			case FRACTION_UNCHARGED:
				if (mPredictor[PREDICTOR_PKA] != null) {
					fia = predictNumerical(row, FRACTION_IA, dependentColumn, rowInfo);
					if (Double.isNaN(fia))
						fia = 0.0;
					fib = predictNumerical(row, FRACTION_IB, dependentColumn, rowInfo);
					if (Double.isNaN(fib))
						fib = 0.0;
					value = (1.0 - fia) * (1.0 - fib);
					}
				break;
			case CHARGE74:
				if (mPredictor[PREDICTOR_PKA] != null) {
					if (camol == null)
						throw new Exception("molecule conversion error");

					double[] acidicPKa = ((PKaPredictor)mPredictor[PREDICTOR_PKA]).getMostAcidicPKas(camol);
					double[] basicPKa = ((PKaPredictor)mPredictor[PREDICTOR_PKA]).getMostBasicPKas(camol);
					value = 0;
					for (int i = 0; i<3; i++) {
						if (!Double.isNaN(acidicPKa[i]) && acidicPKa[i]<7.4)
							value--;
						if (!Double.isNaN(basicPKa[i]) && basicPKa[i]>7.4)
							value++;
						}
					}
				break;
			case PERMEABILITY:
				if (mPredictor[PREDICTOR_PKA] != null) {
					double mw = predictNumerical(row, FRAGMENT_WEIGHT, dependentColumn, rowInfo);
					double donors = predictNumerical(row, DONORS, dependentColumn, rowInfo);
					double rotBonds = predictNumerical(row, ROTATABLE_BONDS, dependentColumn, rowInfo);
					logP = predictNumerical(row, LOGP, dependentColumn, rowInfo);
					double tpsa = predictNumerical(row, TPSA, dependentColumn, rowInfo);
					double charge = predictNumerical(row, CHARGE74, dependentColumn, rowInfo);
					double fc = predictNumerical(row, FRACTION_CHARGED, dependentColumn, rowInfo);
					if (!Double.isNaN(mw) && !Double.isNaN(donors) && !Double.isNaN(rotBonds) && !Double.isNaN(logP)
							&& !Double.isNaN(tpsa) && !Double.isNaN(charge) && !Double.isNaN(fc))
						value = Math.pow(10, 1.0 - 0.0038 * tpsa + 0.0009 * mw - 0.092 * donors - 0.019 * rotBonds - 0.11 * fc + 0.0061 * charge + 0.075 * logP);
					}
				break;
			case GLOBULARITY_SVD:
				value = GlobularityCalculator.assessGlobularity(rowInfo.getConformerSet());
				break;
			case GLOBULARITY_VOL:
				cs = rowInfo.getConformerSet();
				if (cs == null || cs.size() == 0) {
					value = Double.NaN;
				}
				else {
					double globularity = 0.0;
					for (Conformer conf:cs) {
						conf.copyTo(conf.getMolecule());
						SurfaceAreaAndVolumeCalculator calculator = new SurfaceAreaAndVolumeCalculator(conf.getMolecule(), SurfaceAreaAndVolumeCalculator.MODE_AREA_AND_VOLUME);
						float volume = calculator.getVolume();
						float area = calculator.getArea();
						double r = Math.pow(volume / (1.333 * Math.PI), 0.333333);
						float idealArea = (float)(4*Math.PI*r*r);
						globularity += idealArea / area;
					}

					value = globularity / cs.size();
				}
				break;
			case SURFACE_3D:
				cs = rowInfo.getConformerSet();
				if (cs == null || cs.size() == 0) {
					value = Double.NaN;
				}
				else {
					double area = 0.0;
					for (Conformer conf:cs) {
						conf.copyTo(conf.getMolecule());
						SurfaceAreaAndVolumeCalculator calculator = new SurfaceAreaAndVolumeCalculator(conf.getMolecule(), SurfaceAreaAndVolumeCalculator.MODE_AREA);
						area += calculator.getArea();
					}

					value = area / cs.size();
				}
				break;
			case VOLUME_3D:
				cs = rowInfo.getConformerSet();
				if (cs == null || cs.size() == 0) {
					value = Double.NaN;
				}
				else {
					double volume = 0.0;
					for (Conformer conf:cs) {
						conf.copyTo(conf.getMolecule());
						SurfaceAreaAndVolumeCalculator calculator = new SurfaceAreaAndVolumeCalculator(conf.getMolecule(), SurfaceAreaAndVolumeCalculator.MODE_VOLUME);
						volume += calculator.getVolume();
					}

					value = volume / cs.size();
				}
				break;
			}

		cache.put(propertyType, value);
		return value;
		}

	private class DEProperty {
		public final String columnTitle;
		public final String description;
		public final String descriptorName;
		public final String dependentColumnFilter;	// if not null, e.g. 'ic50', serves as substring to prioritize numerical columns for selection, lower case!!!
		public final int predictorFlags,tab,type;
		public DEBackgroundColor backgroundColor;
	
		/**
		 * @param columnTitle
		 * @param description
		 * @param dependentColumnFilter
		 * @param descriptorName
		 * @param predictorFlags
		 */
		public DEProperty(int type, int tab, String columnTitle, String description, String dependentColumnFilter,
						  String descriptorName, int predictorFlags) {
			this.type = type;
			this.tab = tab;
			this.columnTitle = columnTitle;
			this.description = description;
			this.dependentColumnFilter = dependentColumnFilter;
			this.descriptorName = descriptorName;
			this.predictorFlags = predictorFlags;
			}
		}

	private class DEBackgroundColor {
		int colorMode;
		Color[] colorList;

		public DEBackgroundColor(int colorMode, Color[] colorList) {
			this.colorMode = colorMode;
			this.colorList = colorList;
			}
		}

	private class DEPropertyOrder {
		DEProperty property;
		int dependentColumn;

		public DEPropertyOrder(DEProperty property, int dependentColumn) {
			this.property = property;
			this.dependentColumn = dependentColumn;
			}

		public String getColumnTitle() {
			return (dependentColumn == -1) ? property.columnTitle
					: property.columnTitle + " from " + mTableModel.getColumnTitle(dependentColumn);
			}
		}

	private class DEPropertyGUI implements ActionListener {
		private JCheckBox mCheckBox;
		private JComboBox mComboBox;
		private DEProperty mProperty;

		/**
		 * @param property
		 */
		public DEPropertyGUI(DEProperty property) {
			mProperty = property;

			mCheckBox = new JCheckBox(property.description);
			mCheckBox.addActionListener(this);
			if (property.dependentColumnFilter != null) {
				mComboBox = new JComboBox();
				for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
					if (mTableModel.isColumnTypeDouble(i) && mTableModel.getColumnTitle(i).toLowerCase().contains(property.dependentColumnFilter))
						mComboBox.addItem(mTableModel.getColumnTitle(i));
				for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
					if (mTableModel.isColumnTypeDouble(i) && !mTableModel.getColumnTitle(i).toLowerCase().contains(property.dependentColumnFilter))
						mComboBox.addItem(mTableModel.getColumnTitle(i));

				if (mComboBox.getItemCount() == 0)
					mCheckBox.setEnabled(false);
				}

			// Check, whether the ChemAxon classes are available
			if ((property.predictorFlags & PREDICTOR_FLAG_PKA) != 0
			 && !mParentFrame.getApplication().isCapkaAvailable())
				mCheckBox.setEnabled(false);
			}

		public JCheckBox getCheckBox() {
			return mCheckBox;
			}

		public JComboBox getComboBox() {
			return mComboBox;
			}

		public void actionPerformed(ActionEvent e) {
			if (mProperty.descriptorName != null) {
				int structureColumn = mTableModel.findColumn((String)mComboBoxStructureColumn.getSelectedItem());
				if (mTableModel.getChildColumn(structureColumn, mProperty.descriptorName) == -1) {
					JOptionPane.showMessageDialog(getParentFrame(), "Calculating '" + mProperty.columnTitle + "' requires the '" + mProperty.descriptorName
															  + "' descriptor, which is not available.");
					((JCheckBox)e.getSource()).setSelected(false);
					}
				}
			}
		}

	private class RowInfo {
		public StereoMolecule mol;
		public chemaxon.struc.Molecule camol;
		public TreeMap<Integer, Double> cache;
		private int row;
		private ConformerSet conformerSet;

		public RowInfo(int row, StereoMolecule containerMol) {
			this.row = row;
			mol = mTableModel.getChemicalStructure(mTableModel.getTotalRecord(row), mIDCodeColumn, CompoundTableModel.ATOM_COLOR_MODE_NONE, containerMol);
			if (mol != null) {
				cache = new TreeMap<>();
				camol = null;
				if (mol.getAllAtoms() != 0) {
					for (DEPropertyOrder order:mPropertyOrderList) {
						if (order.property.type == TOTAL_WEIGHT)
							cache.put(TOTAL_WEIGHT, new MolecularFormula(mol).getRelativeWeight());
						else if (order.property.type == FRAGMENTS) {
							int[] fNo = new int[mol.getAllAtoms()];
							cache.put(FRAGMENTS, (double)mol.getFragmentNumbers(fNo, false, true));
							}
						}

					mol.stripSmallFragments(true);
					if (mPredictor[PREDICTOR_PKA] != null)
						camol = ((PKaPredictor)mPredictor[PREDICTOR_PKA]).convert(mol);
					}
				}
			}

		public boolean hasMolecule() {
			return mol != null && mol.getAllAtoms() != 0;
			}

		public ConformerSet getConformerSet() {
			if (conformerSet == null) {
				int confColumn = mTableModel.getChildColumn(mIDCodeColumn, CompoundTableConstants.cColumnType3DCoordinates);
				if (confColumn != -1) {
					CompoundRecord record = mTableModel.getRecord(row);
					byte[] idcode = (byte[])record.getData(mIDCodeColumn);
					byte[] coords = (byte[])record.getData(confColumn);
					conformerSet = new ConformerSet(idcode, coords);
					}
				else {
					conformerSet = new ConformerSetGenerator(16, ConformerGenerator.STRATEGY_LIKELY_RANDOM, false, 0).generateConformerSet(mol);
					}
				}

			return conformerSet;
			}
		}
	}
