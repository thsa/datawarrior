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

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.ScaffoldHelper;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.sar.CoreBasedSARAnalyzer;
import com.actelion.research.chem.sar.ExitVector;
import com.actelion.research.chem.sar.SARMolecule;
import com.actelion.research.chem.sar.SARScaffold;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.gui.CompoundCollectionPane;
import com.actelion.research.gui.DefaultCompoundCollectionModel;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.util.Properties;
import java.util.TreeMap;

public class DETaskDecomposeRGroups extends DETaskAbstractFromStructure {
	public static final String TASK_NAME = "Decompose R-Groups";

	private static final String PROPERTY_SCAFFOLD_MODE = "scaffoldMode";
	private static final String PROPERTY_SCAFFOLD_LIST = "scaffoldList";
	private static final String PROPERTY_USE_EXISTING_COLUMNS = "useExistingColumns";

	private static final int SCAFFOLD_CENTRAL_RING = 0;
	private static final int SCAFFOLD_MURCKO = 1;
	private static final int SCAFFOLD_CUSTOM = 2;
	private static final int DEFAULT_SCAFFOLD_MODE = SCAFFOLD_CUSTOM;

	private static final String[] SCAFFOLD_TEXT = { "Most central ring system", "Murcko scaffold", "Custom sub-structure(s)" };
	private static final String[] SCAFFOLD_CODE = { "centralRing", "murcko", "custom" };

	private static final String SCAFFOLD_COLUMN_NAME = "Scaffold";
	private static final int cTableColumnNew = -2;

	private DefaultCompoundCollectionModel.Molecule mScaffoldQueryModel;
	private CompoundCollectionPane<StereoMolecule> mScaffoldQueryPane;
	private JLabel          mScaffoldQueryLabel;
	private JComboBox       mComboBoxScaffoldMode;
	private JCheckBox		mCheckBoxUseExistingColumns;
	private boolean			mIs2ndStepSAR;
	private int				mFirstRGroup,mStructureColumn,mScaffoldColumn,mScaffoldCoordsColumn,mMultipleMatches,mNewColumnCount;
	private int[]			mSubstituentColumn;
	private SARMolecule[]	mMoleculeData;

    public DETaskDecomposeRGroups(DEFrame parent) {
		super(parent, DESCRIPTOR_NONE, false, false);
	    }

	/**
	 * Checks, if the no-alias column name is compatible looks like an R-group column title
	 * originally given by this DETaskCoreBasedSAR class. If the title looks like 'Rn' (or 'Rn 2','Rn 3',...),
	 * and if the column contains chemical structures, then the int representation of 'n' is returned.
	 * Otherwise -1 is returned.
	 * @param tableModel
	 * @param column
	 * @return R-group number extracted from column title or -1
	 */
	public static int getRGoupNoFromColumnName(CompoundTableModel tableModel, int column) {
		if (!tableModel.isColumnTypeStructure(column))
			return -1;

		String columnName = tableModel.getColumnTitleNoAlias(column);
		if (columnName.length() >= 2 && columnName.charAt(0) == 'R' && Character.isDigit(columnName.charAt(1))) {
			if (columnName.length() == 2
			 || (columnName.length() == 4 && columnName.charAt(2) == ' ' && Character.isDigit(columnName.charAt(3))))
				return columnName.charAt(1) - '0';
			if ((columnName.length() == 3 && Character.isDigit(columnName.charAt(2)))
			 || (columnName.length() == 5 && Character.isDigit(columnName.charAt(2)) && columnName.charAt(3) == ' ' && Character.isDigit(columnName.charAt(4))))
				return Integer.parseInt(columnName.substring(1, 3));
			}

		return -1;
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
		int width = HiDPIHelper.scale(480);
		JPanel ep = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {width/2, gap/2, width/2},
							{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap/2, HiDPIHelper.scale(96), 2*gap, TableLayout.PREFERRED} };
		ep.setLayout(new TableLayout(size));

		ep.add(new JLabel("Determine scaffold as:"), "0,1");
		mComboBoxScaffoldMode = new JComboBox(SCAFFOLD_TEXT);
		mComboBoxScaffoldMode.addActionListener(e -> enableItems() );
		ep.add(mComboBoxScaffoldMode, "2,1");

		mScaffoldQueryLabel = new JLabel("Define custom scaffold sub-structures:");
		mScaffoldQueryModel = new DefaultCompoundCollectionModel.Molecule();
		mScaffoldQueryPane = new CompoundCollectionPane<>(mScaffoldQueryModel, false);
		mScaffoldQueryPane.setCreateFragments(true);
		mScaffoldQueryPane.setEditable(true);
		mScaffoldQueryPane.setClipboardHandler(new ClipboardHandler());
		mScaffoldQueryPane.setShowValidationError(true);
		ep.add(mScaffoldQueryLabel, "0,3,2,3");
		ep.add(mScaffoldQueryPane, "0,5,2,5");

		mCheckBoxUseExistingColumns = new JCheckBox("Use existing columns for scaffold and R-groups", true);
		if (isInteractive() && getTableModel().findColumn(SCAFFOLD_COLUMN_NAME) == -1)
			mCheckBoxUseExistingColumns.setEnabled(false);
		ep.add(mCheckBoxUseExistingColumns, "0,7,2,7");

		return ep;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();

		configuration.setProperty(PROPERTY_SCAFFOLD_MODE, SCAFFOLD_CODE[mComboBoxScaffoldMode.getSelectedIndex()]);

		if (mComboBoxScaffoldMode.getSelectedIndex() == SCAFFOLD_CUSTOM) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i<mScaffoldQueryModel.getSize(); i++) {
				if (sb.length() != 0)
					sb.append('\t');
				Canonizer canonizer = new Canonizer(mScaffoldQueryModel.getMolecule(i));
				sb.append(canonizer.getIDCode() + " " + canonizer.getEncodedCoordinates());
				}
			configuration.setProperty(PROPERTY_SCAFFOLD_LIST, sb.toString());
			}

		configuration.setProperty(PROPERTY_USE_EXISTING_COLUMNS, mCheckBoxUseExistingColumns.isSelected() ? "true" : "false");

		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);

		mComboBoxScaffoldMode.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_SCAFFOLD_MODE), SCAFFOLD_CODE, SCAFFOLD_CUSTOM));

		String scaffoldList = configuration.getProperty(PROPERTY_SCAFFOLD_LIST, "");
		if (scaffoldList.length() != 0)
			for (String idcode:scaffoldList.split("\\t"))
				mScaffoldQueryModel.addCompound(new IDCodeParser(true).getCompactMolecule(idcode));

		mCheckBoxUseExistingColumns.setSelected("true".equals(configuration.getProperty(PROPERTY_USE_EXISTING_COLUMNS)));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mComboBoxScaffoldMode.setSelectedIndex(DEFAULT_SCAFFOLD_MODE);
		enableItems();
		if (!isInteractive() || getTableModel().findColumn(SCAFFOLD_COLUMN_NAME) != -1)
			mCheckBoxUseExistingColumns.setSelected(true);
		}

	private void enableItems() {
		boolean enabled = (mComboBoxScaffoldMode.getSelectedIndex() == SCAFFOLD_CUSTOM);
		mScaffoldQueryLabel.setEnabled(enabled);
		mScaffoldQueryPane.setEnabled(enabled);
		}

	@Override
	protected int getNewColumnCount() {
		return mNewColumnCount;
		}

	@Override
	protected String getNewColumnName(int column) {
		if (column == 0 && mScaffoldColumn == cTableColumnNew)
			return SCAFFOLD_COLUMN_NAME;
		int index = (mScaffoldColumn == cTableColumnNew) ? 1 : 0;
		for (int i=0; i<mSubstituentColumn.length; i++) {
			if (mSubstituentColumn[i] == cTableColumnNew) {
				if (index == column)
					return "R"+(i+mFirstRGroup);

				index++;
				}
			}
		return "scaffoldAtomCoords";
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		int scaffoldMode = findListIndex(configuration.getProperty(PROPERTY_SCAFFOLD_MODE), SCAFFOLD_CODE, SCAFFOLD_CUSTOM);

		if (scaffoldMode == SCAFFOLD_CUSTOM) {
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
			}

		return super.isConfigurationValid(configuration, isLive);
		}

	@Override
	public boolean preprocessRows(Properties configuration) {
		int rowCount = getTableModel().getTotalRowCount();
		mStructureColumn = getChemistryColumn(configuration);

		// If we run a SAR deconvolution from a scaffold column that is the result of a previous SAR deconvolution, then
		// - we expect R-groups as part of the source structure
		// - we need to start the R-group numbering with a higher number to avoid already existing R-group columns
		// - we need to store that R-group offset as column property in the scaffold column for repeated runs
		mIs2ndStepSAR = getTableModel().getColumnTitleNoAlias(mStructureColumn).startsWith(SCAFFOLD_COLUMN_NAME);

		int notFoundCount = 0;

		mFirstRGroup = 1;
		if (mIs2ndStepSAR) {
			// If the scaffold column was already used as source for a SAR table, i.e. its content was split into
			// sub-scaffolds and additional R-groups, then column property should define the first R-group number used.
			String firstRGroupNo = getTableModel().getColumnProperty(mStructureColumn, CompoundTableConstants.cColumnPropertySARFirstRGroup);
			if (firstRGroupNo != null)
				mFirstRGroup = Integer.parseInt(firstRGroupNo);
			for (int column = 0; column<getTableModel().getTotalColumnCount(); column++)
				mFirstRGroup = Math.max(mFirstRGroup, 1 + getRGoupNoFromColumnName(getTableModel(), column));
			}

		int scaffoldMode = findListIndex(configuration.getProperty(PROPERTY_SCAFFOLD_MODE), SCAFFOLD_CODE, SCAFFOLD_CUSTOM);

		String[] queryIDCode;
		if (scaffoldMode == SCAFFOLD_CUSTOM) {
			queryIDCode = configuration.getProperty(PROPERTY_SCAFFOLD_LIST, "").split("\\t");
			}
		else {
			queryIDCode = compileAutomaticScaffoldList(scaffoldMode);
			if (queryIDCode.length == 0) {
				if (isInteractive()) {
					final String message = "None of your scaffolds were found in the '" + getTableModel().getColumnTitle(mStructureColumn) + "' column.";
					showInteractiveTaskMessage(message, JOptionPane.INFORMATION_MESSAGE);
					}
				return false;
				}
			}

		mMoleculeData = new SARMolecule[rowCount];
		int substituentCount = 0;

		startProgress("Processing query structures...", 0, rowCount);

		for (String idcode:queryIDCode) {
try {   // TODO remove
			if (threadMustDie())
				break;
			updateProgress(-1);

			StereoMolecule query = new IDCodeParser(true).getCompactMolecule(idcode);
			CoreBasedSARAnalyzer analyzer = new CoreBasedSARAnalyzer(query, rowCount);
			if (!processScaffoldGroup(analyzer))
				notFoundCount++;

			if (substituentCount < analyzer.getRGroupCount())
				substituentCount = analyzer.getRGroupCount();

} catch (Exception e) { e.printStackTrace(); }
			}

		if (notFoundCount == queryIDCode.length) {
			if (isInteractive()) {
				final String message = "None of your scaffolds were found in the '" + getTableModel().getColumnTitle(mStructureColumn) + "' column.";
				showInteractiveTaskMessage(message, JOptionPane.INFORMATION_MESSAGE);
				}
			return false;
			}

		mScaffoldColumn = cTableColumnNew;
		mScaffoldCoordsColumn = cTableColumnNew;
		mSubstituentColumn = new int[substituentCount];
		for (int i=0; i<substituentCount; i++)
			mSubstituentColumn[i] = cTableColumnNew;

		boolean useExistingColumns = "true".equals(configuration.getProperty(PROPERTY_USE_EXISTING_COLUMNS));
		if (useExistingColumns) {
			for (int column=0; column<getTableModel().getTotalColumnCount(); column++) {
				if (getTableModel().isColumnTypeStructure(column)
				 && getTableModel().getColumnTitleNoAlias(column).startsWith(SCAFFOLD_COLUMN_NAME)
				 && getTableModel().getColumnTitleNoAlias(mStructureColumn).equals(getTableModel().getColumnProperty(column, CompoundTableConstants.cColumnPropertyParentColumn))) {
		            mScaffoldColumn = column;
		            int coordsColumn = getTableModel().getChildColumn(column, CompoundTableConstants.cColumnType2DCoordinates);
		            if (coordsColumn != -1)
		                mScaffoldCoordsColumn = coordsColumn;
		            }
				}

			for (int i=0; i<substituentCount; i++) {
				String columnName = "R"+(i+mFirstRGroup);
				int column = getTableModel().findColumn(columnName);
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

	private String[] compileAutomaticScaffoldList(int scaffoldMode) {
		int rowCount = getTableModel().getTotalRowCount();
		startProgress("Analyzing scaffolds...", 0, rowCount);

		StereoMolecule core = new StereoMolecule();
		StereoMolecule container = new StereoMolecule();
		TreeMap<String,String> idcodeMap = new TreeMap<>();

		for (int row=0; row<rowCount; row++) {
			if ((row % 16) == 15) {
				if (threadMustDie())
					break;
				updateProgress(row);
			}

			StereoMolecule mol = getChemicalStructure(row, container);
			if (mol != null) {
				mol.stripSmallFragments();
				boolean[] isCoreAtom = (scaffoldMode == SCAFFOLD_MURCKO) ?
						ScaffoldHelper.findMurckoScaffold(mol) : ScaffoldHelper.findMostCentralRingSystem(mol);
				if (isCoreAtom != null) {
					int[] molToCoreAtom = new int[mol.getAllAtoms()];
					mol.copyMoleculeByAtoms(core, isCoreAtom, true, molToCoreAtom);
					core.setFragment(true);
					Canonizer canonizer = new Canonizer(core);
					String idcode = canonizer.getIDCode();
					String coords = canonizer.getEncodedCoordinates();
					idcodeMap.put(idcode, idcode.concat(" ").concat(coords));
					}
				}
			}

		return idcodeMap.values().toArray(new String[0]);
		}

	/**
	 * Processes entire table with one of the defined scaffold queries:<br>
	 * For every row with no previously found scaffold it checks whether the row's structure contains query as substructure.
	 * With all rows, where the query was found as a substructure<br>
	 * - A canonical scaffold core structure is determined from the query match plus potentially matching bridge bonds
	 * - For every distinct core structure all exit vectors are checked whether they carry changing substituents through all matching rows.<br>
	 * - For core exit vector with changing substituents, the substituent of every row is created and put into mSubstituent.<br>
	 * - For core exit vector with no or a constant substituent, mSubstituent is set to null and a constant substituent is attached to the core structure.<br>
	 * - The decorated scaffold structure is written into mScaffold for these rows.<br>
	 * @param analyzer
	 * @return false if the scaffold could not be found in any row or an error accurrs
	 */
	private boolean processScaffoldGroup(CoreBasedSARAnalyzer analyzer) {
		mMultipleMatches = 0;

		startProgress("Analyzing substituents...", 0, getTableModel().getTotalRowCount());

        int coordinateColumn = getTableModel().getChildColumn(mStructureColumn, CompoundTableModel.cColumnType2DCoordinates);
        int fingerprintColumn = getTableModel().getChildColumn(mStructureColumn, DescriptorConstants.DESCRIPTOR_FFP512.shortName);

		for (int row=0; row<getTableModel().getTotalRowCount(); row++) {
			if (threadMustDie())
				break;

			if (mMoleculeData[row] != null)
				continue;

			updateProgress(row+1);

			byte[] idcode = (byte[])getTableModel().getTotalRecord(row).getData(mStructureColumn);
			if (idcode != null) {
				byte[] coords = (byte[])getTableModel().getTotalRecord(row).getData(coordinateColumn);
				long[] ffp = (long[])getTableModel().getTotalRecord(row).getData(fingerprintColumn);
				int matchCount = analyzer.setMolecule(idcode, coords, ffp, row);
				if (matchCount > 1)
					mMultipleMatches++;
				}
			}

		if (analyzer.getScaffoldGroup().getScaffoldList().size() == 0)
			return false;

		if (threadMustDie())
			return false;

		if (!analyzer.analyze(mFirstRGroup)) {
			final String message = "Some scaffolds could not be processed, because\rthe maximum number of allowed R-groups was exceeded.";
			showInteractiveTaskMessage(message, JOptionPane.INFORMATION_MESSAGE);
			}

		for (int row=0; row<getTableModel().getTotalRowCount(); row++)
			if (analyzer.getMoleculeData(row) != null)
				mMoleculeData[row] = analyzer.getMoleculeData(row);

		return true;
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

		getTableModel().setColumnProperty(mScaffoldColumn, CompoundTableModel.cColumnPropertyParentColumn, getTableModel().getColumnTitleNoAlias(mStructureColumn));
		if (mIs2ndStepSAR)
			getTableModel().setColumnProperty(mScaffoldColumn, CompoundTableModel.cColumnPropertySARFirstRGroup, Integer.toString(mFirstRGroup));

		for (int i=firstNewColumn; i<lastNewColumn; i++)
            getTableModel().setColumnProperty(i, CompoundTableModel.cColumnPropertySpecialType, CompoundTableModel.cColumnTypeIDCode);

		if (mScaffoldCoordsColumn == cTableColumnNew)
			mScaffoldCoordsColumn = lastNewColumn;

		getTableModel().setColumnProperty(mScaffoldCoordsColumn, CompoundTableModel.cColumnPropertySpecialType, CompoundTableModel.cColumnType2DCoordinates);
		getTableModel().setColumnProperty(mScaffoldCoordsColumn, CompoundTableModel.cColumnPropertyParentColumn, getTableModel().getColumnTitleNoAlias(mScaffoldColumn));
		}

	@Override
	public void processRow(int row, int firstNewColumn, StereoMolecule containerMol, int threadIndex) {
		if (mMoleculeData[row] != null) {
			getTableModel().removeChildDescriptorsAndCoordinates(row, mScaffoldColumn);
			getTableModel().setTotalValueAt(mMoleculeData[row].getScaffold().getIDCodeWithRGroups(), row, mScaffoldColumn);
			getTableModel().setTotalValueAt(mMoleculeData[row].getScaffold().getIDCoordsWithRGroups(), row, mScaffoldCoordsColumn);

			String[] substituent = mMoleculeData[row].getSubstituents();
			SARScaffold scaffold = mMoleculeData[row].getScaffold();
			for (int exitVectorIndex = 0; exitVectorIndex<scaffold.getExitVectorCount(); exitVectorIndex++) {
				ExitVector exitVector = scaffold.getExitVector(exitVectorIndex);
				if (exitVector.getRGroupNo() != 0) {
					int index = exitVector.getRGroupNo()-mFirstRGroup;
					getTableModel().setTotalValueAt(substituent[exitVectorIndex], row, mSubstituentColumn[index]);
					getTableModel().removeChildDescriptorsAndCoordinates(row, mSubstituentColumn[index]);
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
