package com.actelion.research.datawarrior.task.table;

import com.actelion.research.chem.*;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.name.StructureNameResolver;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.chem.reaction.ReactionEncoder;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DETable;
import com.actelion.research.datawarrior.task.AbstractSingleColumnTask;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Properties;

public class DETaskPasteIntoTable extends AbstractSingleColumnTask {
	public static final String TASK_NAME = "Paste Into Table";

	private static final String PROPERTY_FIRST_ROW = "firstRow";
	private static final String FIRST_ROW_AT_END_CODE = "append";
	public static final int ROW_APPEND = -2;

	private int mVisibleRow;
	private JTextField mTextFieldFirstRow;
	private DETable mTable;
	private String[][] mClipboardContent;

	/**
	 * @param parent
	 */
	public DETaskPasteIntoTable(DEFrame parent) {
		this(parent, -1, -1);
		}

	/**
	 * @param parent
	 * @param column -1 or first colunm
	 * @param visibleRow -1 or first row
	 */
	public DETaskPasteIntoTable(DEFrame parent, int column, int visibleRow) {
		super(parent, parent.getTableModel(), false, column);
		mVisibleRow = visibleRow;
		mTable = parent.getMainFrame().getMainPane().getTable();
		}

	@Override
	public boolean allowColumnNoneItem() {
		return true;
		}

	@Override
	public boolean isCompatibleColumn(int column) {
		return mTable.convertTotalColumnIndexToView(column) != -1;
		}

	@Override
	public boolean isConfigurable() {
		return mTable != null;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public String getHelpURL() {
		return "/html/help/import.html#PasteInto";
	}

	@Override
	public JPanel createInnerDialogContent() {
		int gap = HiDPIHelper.scale(8);
		JPanel content = new JPanel();
		double[][] size = { {TableLayout.PREFERRED, gap, TableLayout.PREFERRED}, {TableLayout.PREFERRED} };
		content.setLayout(new TableLayout(size));

		content.add(new JLabel("First Row:"), "0,0");
		mTextFieldFirstRow = new JTextField(6);
		content.add(mTextFieldFirstRow, "2,0");

		return content;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		Properties configuration = super.getPredefinedConfiguration();
		if (configuration == null)
			return null;

		String value = (mVisibleRow == ROW_APPEND) ? FIRST_ROW_AT_END_CODE : Integer.toString(mVisibleRow +1);
		configuration.setProperty(PROPERTY_FIRST_ROW, value);
		return configuration;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_FIRST_ROW, mTextFieldFirstRow.getText());
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mTextFieldFirstRow.setText(configuration.getProperty(PROPERTY_FIRST_ROW, "1"));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mTextFieldFirstRow.setText("1");
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (!super.isConfigurationValid(configuration, isLive))
			return false;

		try {
			String firstRowText = configuration.getProperty(PROPERTY_FIRST_ROW, "1");
			if (!firstRowText.equals(FIRST_ROW_AT_END_CODE)) {
				int firstRow = Integer.parseInt(firstRowText) - 1;
				if (firstRow < 0) {
					showErrorMessage("First row must be 1 or larger");
					return false;
					}
				if (isLive) {
					if (firstRow >= getTableModel().getRowCount()) {
						showErrorMessage("First row is larger than visible row count.");
						return false;
						}
					int column = getColumn(configuration);
					if (column != NO_COLUMN && mTable.convertTotalColumnIndexToView(column) == -1) {
						showErrorMessage("Defined column is not visible.");
						return false;
						}
					}
				}
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("First row is not numerical.");
			return false;
			}

		if (!analyzeClipboard(configuration))
			return false;

		if (isLive) {
			int targetColumn = getColumn(configuration);
			int firstVisColumn = (targetColumn == NO_COLUMN) ? 0 : mTable.convertTotalColumnIndexToView(targetColumn);
			int lastVisColumn = Math.min(mTable.getColumnCount(), firstVisColumn + mClipboardContent[0].length);
			for (int visColumn=firstVisColumn; visColumn<lastVisColumn; visColumn++) {
				String type = getTableModel().getColumnSpecialType(targetColumn);
				if (type != null
				 && !type.equals(CompoundTableConstants.cColumnTypeIDCode)
				 && !type.equals(CompoundTableConstants.cColumnTypeRXNCode)) {
					showErrorMessage("Cannot paste data into special columns other than structure or reaction columns.");
					return false;
					}
				}
			}

		return true;
		}

	/**
	 * Processes the clipboard content as TAB delimited table and puts it into mClipboardContent.
	 * Shows error message in case of error.
	 * @return whether an error occurred
	 */
	private boolean analyzeClipboard(Properties configuration) {
		ArrayList<String[]> rowList = new ArrayList<String[]>();
		int columnCount = 0;

		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable theData = clipboard.getContents(this);
		try {
			String s = (String) (theData.getTransferData(DataFlavor.stringFlavor));
			BufferedReader theReader = new BufferedReader(new StringReader(s));

			String line = theReader.readLine();
			while (line != null) {
				String[] row = line.split("\\t", -1);
				if (columnCount == 0) {
					columnCount = row.length;
					}
				else if (columnCount != row.length) {
					showErrorMessage("Inconsistent column count in clipboard content.");
					return false;
					}
				rowList.add(row);
				line = theReader.readLine();
				}

			theReader.close();
			}
		catch (UnsupportedFlavorException ufe) {
			// instead of a unicode String we may have a molecule or reaction
			int targetColumn = getColumn(configuration);
			if (targetColumn == NO_COLUMN)
				targetColumn = mTable.convertTotalColumnIndexFromView(0);
			String type = getTableModel().getColumnSpecialType(targetColumn);
			if (type != null) {
				if (type.equals(CompoundTableConstants.cColumnTypeIDCode)) {
					StereoMolecule mol = new ClipboardHandler().pasteMolecule();
					if (mol != null) {
						mol.setFragment("true".equals(getTableModel().getColumnProperty(targetColumn,
								CompoundTableConstants.cColumnPropertyIsFragment)));
						Canonizer canonizer = new Canonizer(mol);
						mClipboardContent = new String[1][1];
						mClipboardContent[0][0] = canonizer.getIDCode()+" "+canonizer.getEncodedCoordinates();
						return true;
						}
					}
				if (type.equals(CompoundTableConstants.cColumnTypeRXNCode)) {
					Reaction rxn = new ClipboardHandler().pasteReaction();
					if (rxn != null) {
						mClipboardContent = new String[1][1];
						mClipboardContent[0][0] = ReactionEncoder.encode(rxn, false,
								ReactionEncoder.INCLUDE_MAPPING | ReactionEncoder.INCLUDE_COORDS);;
						return true;
						}
					}
				}
			return false;
			}
		catch (Exception e) {
			showErrorMessage("Error while processing clipboard content:"+e.getMessage());
			return false;
			}

		if (rowList.isEmpty()) {
			showErrorMessage("No rows found on clipboard.");
			return false;
			}

		mClipboardContent = rowList.toArray(new String[0][]);
		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		int clipColumnCount = mClipboardContent[0].length;
		int clipRowCount = mClipboardContent.length;

		int tableColumnCount = mTable.getColumnCount();
		int tableRowCount = mTable.getRowCount();

		int firstColumn = getColumn(configuration);
		int firstVisColumn = (firstColumn == NO_COLUMN) ? 0 : mTable.convertTotalColumnIndexToView(firstColumn);

		String firstVisRowText = configuration.getProperty(PROPERTY_FIRST_ROW, "1");
		int firstVisRow = (firstVisRowText == FIRST_ROW_AT_END_CODE) ? tableRowCount : Integer.parseInt(firstVisRowText) - 1;

		int newColumnCount = Math.max(0, clipColumnCount - tableColumnCount + firstVisColumn);
		int newRowCount = Math.max(0, clipRowCount - tableRowCount + firstVisRow);

		int[] clipToDestColumn = new int[clipColumnCount];

		// First, add new columns, if necessary and populate them for the existing rows only. This avoids changing row visibility.
		if (newColumnCount != 0) {
			int firstNewColumn = getTableModel().getTotalColumnCount();
			getTableModel().addNewColumns(newColumnCount);
			for (int i=0; i<newColumnCount; i++) {
				int clipColumn = clipColumnCount - newColumnCount + i;
				int destColumn = firstNewColumn + i;
				clipToDestColumn[clipColumnCount-newColumnCount+i] = firstNewColumn+i;
				for (int clipRow=0; clipRow<clipRowCount-newRowCount; clipRow++) {
					int visRow = firstVisRow + clipRow;
					getTableModel().setValueAt(mClipboardContent[clipRow][clipColumn], visRow, destColumn);
					}
				}
			getTableModel().finalizeNewColumns(firstNewColumn, getProgressController());
			}

		// Try, resolving names, if a clipcolumn falls onto a structure column
		for (int clipColumn=0; clipColumn<clipColumnCount-newColumnCount; clipColumn++) {
			int destColumn = mTable.convertTotalColumnIndexFromView(firstVisColumn + clipColumn);
			if (CompoundTableConstants.cColumnTypeIDCode.equals(getTableModel().getColumnSpecialType(destColumn))) {
				String fragmentProperty = getTableModel().getColumnProperty(destColumn, CompoundTableConstants.cColumnPropertyIsFragment);
				int smartsMode = "true".equals(fragmentProperty) ? SmilesParser.SMARTS_MODE_IS_SMARTS
							   : "false".equals(fragmentProperty) ? SmilesParser.SMARTS_MODE_IS_SMILES : SmilesParser.SMARTS_MODE_GUESS;
				resolveStructureNamesToIDCodes(clipColumn, smartsMode);
				}
			}

		for (int clipColumn=0; clipColumn<clipColumnCount-newColumnCount; clipColumn++)
			clipToDestColumn[clipColumn] = mTable.convertTotalColumnIndexFromView(firstVisColumn+clipColumn);

			// Second, change cells of visible rows and originally existing columns. This may change row visibility.
		// Save a list of records to change, because row visibility may change from updated column to column.
		if (clipRowCount > newRowCount) {
			CompoundRecord[] targetRecord = new CompoundRecord[clipRowCount - newRowCount];
			for (int i=0; i<targetRecord.length; i++)
				targetRecord[i] = getTableModel().getRecord(firstVisRow+i);
			for (int clipColumn=0; clipColumn<clipColumnCount-newColumnCount; clipColumn++) {
				int destColumn = clipToDestColumn[clipColumn];

				boolean isStructure = CompoundTableConstants.cColumnTypeIDCode.equals(getTableModel().getColumnSpecialType(destColumn));
				boolean isReaction = CompoundTableConstants.cColumnTypeRXNCode.equals(getTableModel().getColumnSpecialType(destColumn));

				for (int i=0; i<targetRecord.length; i++)
					setValueAndChildValues(mClipboardContent[i][clipColumn], targetRecord[i], destColumn, isStructure, isReaction);

				if (isReaction || isStructure)
					getTableModel().finalizeChangeChemistryColumn(destColumn, 0, getTableModel().getTotalRowCount(), false);
				else
					getTableModel().finalizeChangeAlphaNumericalColumn(destColumn, 0, getTableModel().getTotalRowCount());
				}
			}

		// Third, add new rows, if necessary, and fill originally existing columns and new columns of them.
		if (newRowCount != 0) {
			int firstNewRow = getTableModel().getTotalRowCount();
			getTableModel().addNewRows(newRowCount, true);
			for (int clipColumn=0; clipColumn<clipColumnCount; clipColumn++) {
				int destColumn = clipToDestColumn[clipColumn];

				boolean isStructure = CompoundTableConstants.cColumnTypeIDCode.equals(getTableModel().getColumnSpecialType(destColumn));
				boolean isReaction = CompoundTableConstants.cColumnTypeRXNCode.equals(getTableModel().getColumnSpecialType(destColumn));

				for (int i=0; i<newRowCount; i++)
					setValueAndChildValues(mClipboardContent[clipRowCount-newRowCount+i][clipColumn], getTableModel().getTotalRecord(firstNewRow+i), destColumn, isStructure, isReaction);
				}

			getTableModel().finalizeNewRows(firstNewRow, this);
			}
		}

	private void resolveStructureNamesToIDCodes(int clipColumn, int smartsMode) {
		int clipRowCount = mClipboardContent.length;
		boolean[] needsRemoteResolution = new boolean[clipRowCount];
		SortedStringList unresolvedNameList = null;
		for (int clipRow=0; clipRow<clipRowCount; clipRow++) {
			String value = mClipboardContent[clipRow][clipColumn].trim();
			if (value.length() != 0) {
				StereoMolecule mol = new StereoMolecule();
				try {
					new SmilesParser(smartsMode, false).parse(mol, value);
					}
				catch (Exception e) {
					mol = null;
					}

				if (mol == null || mol.getAllAtoms() == 0)
					mol = StructureNameResolver.resolveLocal(value);

				if (mol == null || mol.getAllAtoms() == 0) {
					needsRemoteResolution[clipRow] = true;
					if (unresolvedNameList == null)
						unresolvedNameList = new SortedStringList();
					unresolvedNameList.addString(value);
					}
				else {
					Canonizer canonizer = new Canonizer(mol);
					mClipboardContent[clipRow][clipColumn] = canonizer.getIDCode()+" "+canonizer.getEncodedCoordinates();
					}
				}
			}

		if (unresolvedNameList != null && unresolvedNameList.getSize() != 0) {
			String[] idcodes = StructureNameResolver.resolveRemote(unresolvedNameList.toArray());
			for (int clipRow=0; clipRow<clipRowCount; clipRow++) {
				if (needsRemoteResolution[clipRow]) {
					String value = mClipboardContent[clipRow][clipColumn].trim();
					String idcode = idcodes[unresolvedNameList.getListIndex(value)];
					if (idcode != null && idcode.length() != 0)
						mClipboardContent[clipRow][clipColumn] = idcode;
					}
				}
			}
		}

	private void setValueAndChildValues(String value, CompoundRecord record, int column, boolean isStructure, boolean isReaction) {
		byte[] coords2D = null;
		byte[] coords3D = null;
		byte[] mapping = null;
		if (value != null && value.length() != 0) {
			if (isStructure) {
				if (isValidIDCode(value)) {
					int index = value.indexOf(' ');
					if (index != -1) {
						String coords = value.substring(index + 1);
						value = value.substring(0, index);
						if (new IDCodeParserWithoutCoordinateInvention().coordinatesAre3D(value, coords))
							coords3D = coords.getBytes();
						else
							coords2D = coords.getBytes();
						}
					}
				else {
					value = null;
					}
				}
			else if (isReaction) {
				if (!isValidRXNCode(value))
					value = null;
				else {
					int index1 = value.indexOf(ReactionEncoder.OBJECT_DELIMITER);
					if (index1 != -1) {
						int index2 = value.indexOf(ReactionEncoder.OBJECT_DELIMITER, index1+1);
						if (index2 == -1)
							mapping = value.substring(index1+1).getBytes();
						else if (index2 > index1+1)
							mapping = value.substring(index1+1, index2).getBytes();

						if (index2 != -1) {
							int index3 = value.indexOf(ReactionEncoder.OBJECT_DELIMITER, index2+1);
							if (index3 == -1)
								coords2D = value.substring(index2+1).getBytes();
							else if (index3 > index2+1)
								coords2D = value.substring(index2+1, index3).getBytes();
							}
						}
					if (index1 != -1)
						value = value.substring(0, index1);
					}
				}
			}

		record.setData(getTableModel().decodeData(value, column), column);

		if (isStructure) {
			for (int childColumn=0; childColumn<getTableModel().getTotalColumnCount(); childColumn++) {
				if (getTableModel().getParentColumn(childColumn) == column) {
					byte[] childValue =
							(CompoundTableConstants.cColumnType2DCoordinates.equals(getTableModel().getColumnSpecialType(childColumn))) ? coords2D
						  : (CompoundTableConstants.cColumnType3DCoordinates.equals(getTableModel().getColumnSpecialType(childColumn))) ? coords3D
						  : null;
					record.setData(childValue, childColumn);
					}
				}
			}
		else if (isReaction) {
			for (int childColumn=0; childColumn<getTableModel().getTotalColumnCount(); childColumn++) {
				if (getTableModel().getParentColumn(childColumn) == column) {
					if (CompoundTableConstants.cColumnType2DCoordinates.equals(getTableModel().getColumnSpecialType(childColumn)))
						record.setData(coords2D, childColumn);
					else if (CompoundTableConstants.cColumnTypeReactionMapping.equals(getTableModel().getColumnSpecialType(childColumn)))
						record.setData(mapping, childColumn);
					else
						record.setData(null, childColumn);
					}
				}
			}
		}

	private boolean isValidIDCode(String idcode) {
		try {
			StereoMolecule mol = new IDCodeParserWithoutCoordinateInvention().getCompactMolecule(idcode);
			return mol.getAllAtoms() != 0;
			}
		catch (Exception e) {
			return false;
			}
		}

	private boolean isValidRXNCode(String rxncode) {
		try {
			Reaction rxn = ReactionEncoder.decode(rxncode, false);
			return rxn.getReactants() != 0 && rxn.getProducts() != 0;
			}
		catch (Exception e) {
			return false;
			}
		}
	}
