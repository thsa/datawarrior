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

package com.actelion.research.table;

import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.MolfileCreator;
import com.actelion.research.chem.MolfileV3Creator;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.reaction.ReactionEncoder;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.JProgressDialog;
import com.actelion.research.gui.form.ReferenceResolver;
import com.actelion.research.table.model.*;
import com.actelion.research.util.ArrayUtils;
import com.actelion.research.util.BinaryEncoder;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.util.*;

public class CompoundTableSaver implements CompoundTableConstants,Runnable {
	public static final String cCurrentFileVersion = "3.3";

	public static final int ID_USE_PROPERTY = -1;
	public static final int ID_BUILD_ONE = -2;

	private static final int MULTI_CONFORMER_ROWS_TO_CHECK = 256;

	private JTable				mTable;
	private CompoundTableModel	mTableModel;
	private JProgressDialog		mProgressDialog;
	private Frame   			mParentFrame;
	private File				mFile;
	private Writer				mDataWriter;
	private int					mDataType,mSDColumnStructure,mSDColumnIdentifier,
								mSDColumnCoordinates;
	private boolean				mVisibleOnly,mToClipboard,mEmbedDetails,mSkipHeader;
	private RuntimeProperties	mRuntimeProperties;
	private ArrayList<DataDependentPropertyWriter> mDataDependentPropertyWriterList;
	private StereoMolecule[]    mSDReferenceMolecules;

	public CompoundTableSaver(Frame parent, CompoundTableModel tableModel, JTable table) {
		mTableModel = tableModel;
		mTable = table;
		mParentFrame = parent;
		mSkipHeader = false;
		}

	/**
	 * Writes the associated tableModel's data into a native file without asking any questions.
	 * Before returning this method calls finalStatus(File file) with file== null if it couldn't be successfully written.
	 * Error checking should be done before calling this function.
	 * @param properties must be given if fileType==FileHelper.cFileTypeDataWarrior or ...Template
	 * @param file a valid file with proper write privileges
	 * @param visibleOnly if true, then only visible records are written
	 * @param embedDetails if true, referenced detail information is retrieved and embedded in the file
	 */
	public void saveNative(RuntimeProperties properties, File file, boolean visibleOnly, boolean embedDetails) {
		mRuntimeProperties = properties;
		mDataType = FileHelper.cFileTypeDataWarrior;
		mFile = file;
		mVisibleOnly = visibleOnly;
		mEmbedDetails = embedDetails;

		saveFile();
		}

	public void addDataDependentPropertyWriter(DataDependentPropertyWriter ddpw) {
		if (mDataDependentPropertyWriterList == null)
			mDataDependentPropertyWriterList = new ArrayList<>();

		mDataDependentPropertyWriterList.add(ddpw);
		}

	public void saveTemplate(RuntimeProperties properties, File file) {
		mRuntimeProperties = properties;
		mDataType = FileHelper.cFileTypeDataWarriorTemplate;
		mFile = file;
		mVisibleOnly = false;
		mEmbedDetails = false;

		saveFile();
		}

	/**
	 * Exports the associated tableModel's data into a TAB delimited text file without asking any questions.
	 * Before returning this method calls finalStatus(File file) with file=null if it couldn't be successfully written.
	 * Error checking should be done before calling this function.
	 * @param file a valid file with proper write privileges
	 */
	public void saveText(File file) {
		mRuntimeProperties = null;
		mDataType = FileHelper.cFileTypeTextTabDelimited;
		mFile = file;
		mVisibleOnly = false;
		mEmbedDetails = false;
		
		saveFile();
		}

	/**
	 * Exports the associated tableModel's data into an SD-file without asking any questions.
	 * Before returning this method calls finalStatus(File file) with file=null if it couldn't be successfully written.
	 * Error checking should be done before calling this function.
	 * @param file a valid file with proper write privileges
	 * @param fileType CompoundFileHelper.cFileTypeSDV3 or CompoundFileHelper.cFileTypeSDV2
	 * @param structureColumn column containing the idcode encoded structure
	 * @param idColumn column containing compound identifiers or ID_USE_PROPERTY or ID_BUILD_ONE
	 * @param coordsColumn if -1, then 2D-coords are generated on the fly
	 * @param refMol null or one or some reference molecules to be written on top of the SD-file
	 */
	public void saveSDFile(File file, int fileType, int structureColumn, int idColumn, int coordsColumn, StereoMolecule[] refMol) {
		mRuntimeProperties = null;
		mDataType = fileType;
		mFile = file;
		mVisibleOnly = false;
		mEmbedDetails = false;

		mSDColumnStructure = structureColumn;
		mSDColumnIdentifier = idColumn;
		mSDColumnCoordinates = coordsColumn;
		mSDReferenceMolecules = refMol;

		saveFile();
		}

/*	public void writeFile(String filename, RuntimeProperties properties) {
		mFile = new File(filename);
		mDataType = FileHelper.cFileTypeDataWarrior;
		mVisibleOnly = false;
		mEmbedDetails = false;
		saveFile();
		}
*/
	private void saveFile() {
		try {
			mDataWriter = new OutputStreamWriter(new FileOutputStream(mFile),"UTF-8");
			mToClipboard = false;
			processData();
			}
		catch (Exception e) {
			JOptionPane.showMessageDialog(mParentFrame, e);
			finalStatus(null);
			}
		}

	public void copy(boolean skipHeader) {
		mDataWriter = new StringWriter(1024);
		mDataType = FileHelper.cFileTypeTextTabDelimited;
		mEmbedDetails = false;
		mVisibleOnly = false;
		mToClipboard = true;
		mSkipHeader = skipHeader;
		try {
			writeTextData();
			StringSelection theData = new StringSelection(mDataWriter.toString());
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(theData, theData);
			}
		catch (Exception e) {
			JOptionPane.showMessageDialog(mParentFrame, e);
			}
		}

	private void processData() {
		if (mParentFrame != null)
			mProgressDialog = new JProgressDialog(mParentFrame, false);

		Thread t = new Thread(this, "CompoundTableSaver");
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();

		if (mProgressDialog != null)
			mProgressDialog.setVisible(true);
		}

	private void writeTextData() throws IOException {
		synchronized(mDataWriter) {
			BufferedWriter theWriter = new BufferedWriter(mDataWriter);

			if (mDataType == FileHelper.cFileTypeDataWarrior) {
				writeFileHeader(theWriter);
				writeTableExtensions(theWriter);
				writeColumnProperties(theWriter);
				}

			if ((mProgressDialog == null
			  || !mProgressDialog.threadMustDie())
			 && mDataType != FileHelper.cFileTypeDataWarriorTemplate)
				writeRecords(theWriter);

			if ((mProgressDialog == null
			  || !mProgressDialog.threadMustDie())
			 && mDataType == FileHelper.cFileTypeDataWarrior) {
				writeHitlists(theWriter);
				}

			if ((mProgressDialog == null
			  || !mProgressDialog.threadMustDie())
			 && mDataType == FileHelper.cFileTypeDataWarrior) {
				writeEmbeddedDetails(theWriter);
				}

			if ((mProgressDialog == null
			  || !mProgressDialog.threadMustDie())
			 && (mDataType == FileHelper.cFileTypeDataWarrior
			  || mDataType == FileHelper.cFileTypeDataWarriorTemplate)
			 && mRuntimeProperties != null)
				mRuntimeProperties.learnAndWrite(theWriter);

			if ((mProgressDialog == null
			  || !mProgressDialog.threadMustDie())
			 && mDataType == FileHelper.cFileTypeDataWarrior) {
				writeDataDependentProperties(theWriter);
				}

			theWriter.close();
			}
		}

	private void writeRecords(BufferedWriter theWriter) throws IOException {
		if (mToClipboard && mTable == null)	// just to make sure
			return;

		// first write non-displayable columns
		if (mDataType == FileHelper.cFileTypeDataWarrior) {
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
				if (!mTableModel.isColumnDisplayable(column)) {
					theWriter.write(mTableModel.getColumnTitleNoAlias(column));
					theWriter.write("\t");
					}
				}
			}

		if (mToClipboard) {	// selected columns only
			if (!mSkipHeader) {
				int[] selectedColumn = mTable.getSelectedColumns();
				for (int i=0; i<selectedColumn.length; i++) {
					int column = mTableModel.convertFromDisplayableColumnIndex(
									  mTable.convertColumnIndexToModel(selectedColumn[i]));
					if (mDataType == FileHelper.cFileTypeTextTabDelimited)
						theWriter.write(mTableModel.getColumnTitleWithSpecialType(column));
					else
						theWriter.write(mTableModel.getColumnTitleNoAlias(column));
					if (i < selectedColumn.length-1)
						theWriter.write("\t");
					}
				theWriter.write("\n");
				}
			}
		else {
			// now write displayable columns in table model order
			int tabs = mTableModel.getColumnCount() - 1;
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
				if (mTableModel.isColumnDisplayable(column)) {
					if (mDataType == FileHelper.cFileTypeTextTabDelimited)
						theWriter.write(mTableModel.getColumnTitleNoAliasWithSpecialType(column));
					else
						theWriter.write(mTableModel.getColumnTitleNoAlias(column));
					if (tabs-- > 0)
						theWriter.write("\t");
					}
				}
			}

		if (!mToClipboard)
			theWriter.newLine();

		int rowCount = mVisibleOnly ? mTableModel.getRowCount() : mTableModel.getTotalRowCount();

		if (mProgressDialog != null)
			mProgressDialog.startProgress("Saving Records...", 0, rowCount);

		for (int row=0; row<rowCount; row++) {
			CompoundRecord record = (mVisibleOnly) ? mTableModel.getRecord(row)
					   : mTableModel.getTotalRecord(row);

			if (!mToClipboard || mTableModel.isVisibleAndSelected(record)) {
				if (mProgressDialog != null) {
					if (mProgressDialog.threadMustDie())
						break;
					mProgressDialog.updateProgress(row);
					}

				if (mDataType == FileHelper.cFileTypeDataWarrior) {
					// write non-displayable columns first
					for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
						if (!mTableModel.isColumnDisplayable(column)) {
							theWriter.write(convertNewlines(getValue(record, column)));
							theWriter.write("\t");
							}
						}
					}

				if (mToClipboard) {	// selected columns only
					int[] selectedColumn = mTable.getSelectedColumns();
					for (int i=0; i<selectedColumn.length; i++) {
						int column = mTableModel.convertFromDisplayableColumnIndex(
									 mTable.convertColumnIndexToModel(selectedColumn[i]));
						theWriter.write(convertNewlines(getValue(record, column)));
						if (i < selectedColumn.length-1)
							theWriter.write("\t");
						}
					theWriter.write("\n");
					}
				else {
					int tabs = mTableModel.getColumnCount() - 1;
					for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
						if (mTableModel.isColumnDisplayable(column)) {
							theWriter.write(convertNewlines(getValue(record, column)));
							if (tabs-- > 0)
								theWriter.write("\t");
							}
						}
					}

				if (!mToClipboard)
					theWriter.newLine();
				}
			}
		}

	private String getValue(CompoundRecord record, int column) {
		return (mDataType == FileHelper.cFileTypeDataWarrior) ?
				   mTableModel.encodeDataWithDetail(record, column)
			 : (cColumnTypeIDCode.equals(mTableModel.getColumnSpecialType(column))) ? getIDCodeAndCoords(record, column)
			 : (cColumnTypeRXNCode.equals(mTableModel.getColumnSpecialType(column))) ? getRXNCodeWithCoordsAndMapping(record, column)
			 : (mToClipboard) ? // use the display value (mean, max, sum, etc.)
				   mTableModel.getValue(record, column)
				 : mTableModel.encodeData(record, column);
		}

	private String getIDCodeAndCoords(CompoundRecord record, int column) {
		String idcode = mTableModel.getValue(record, column);
		if (idcode.length() == 0)
			return idcode;
		int coordsColumn = mTableModel.getChildColumn(column, CompoundTableConstants.cColumnType2DCoordinates);
		if (coordsColumn == -1)
			coordsColumn = mTableModel.getChildColumn(column, CompoundTableConstants.cColumnType3DCoordinates);
		if (coordsColumn == -1)
			return idcode;
		String coords = mTableModel.getValue(record, coordsColumn);
		return (coords.length() == 0) ? idcode : idcode.concat(" ").concat(coords);
		}

	private String getRXNCodeWithCoordsAndMapping(CompoundRecord record, int column) {
		String rxncode = mTableModel.getValue(record, column);
		if (rxncode.length() == 0)
			return rxncode;

		int coordsColumn = mTableModel.getChildColumn(column, CompoundTableConstants.cColumnType2DCoordinates);
		String coords = (coordsColumn == -1) ? "" : mTableModel.getValue(record, coordsColumn);

		int mappingColumn = mTableModel.getChildColumn(column, CompoundTableConstants.cColumnTypeReactionMapping);
		String mapping = (mappingColumn == -1) ? "" : mTableModel.getValue(record, mappingColumn);

		if (mapping.length() != 0 || coords.length() != 0)
			rxncode = rxncode + ReactionEncoder.OBJECT_DELIMITER;

		if (mapping.length() != 0) {
			rxncode = rxncode + mapping;
			if (coords.length() != 0)
				rxncode = rxncode + ReactionEncoder.OBJECT_DELIMITER;
			}

		if (coords.length() != 0)
			rxncode = rxncode + coords;

		return rxncode;
		}

	public static String convertNewlines(String value) {
		value = value.replaceAll(NEWLINE_REGEX, NEWLINE_STRING);
		value = value.replace("\t", CompoundTableLoader.TAB_STRING);
		return value;
		}

	private void writeFileHeader(BufferedWriter theWriter) throws IOException {
		theWriter.write(cNativeFileHeaderStart);
		theWriter.newLine();
		theWriter.write("<"+cNativeFileVersion+"=\""+cCurrentFileVersion+"\">");
		theWriter.newLine();
		theWriter.write("<"+cNativeFileCreated+"=\""+System.currentTimeMillis()+"\">");
		theWriter.newLine();
		int rowCount = mVisibleOnly ? mTableModel.getRowCount() : mTableModel.getTotalRowCount();
		theWriter.write("<"+cNativeFileRowCount+"=\""+rowCount+"\">");
		theWriter.newLine();
		theWriter.write(cNativeFileHeaderEnd);
		theWriter.newLine();
		}

	private void writeTableExtensions(BufferedWriter theWriter) throws IOException {
		CompoundTableExtensionHandler extensionHandler = mTableModel.getExtensionHandler();
		if (extensionHandler != null) {
			String[] nameList = mTableModel.getAvailableExtensionNames();
			if (nameList != null)
				for (String name:nameList)
					extensionHandler.writeData(name, mTableModel.getExtensionData(name), theWriter);
			}
		}

	private void writeColumnProperties(BufferedWriter theWriter) throws IOException {
		boolean found = false;
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
			HashMap<String,String> map = mTableModel.getColumnProperties(column);
			if (map != null && !map.isEmpty()) {
				if (!found) {
					theWriter.write(cColumnPropertyStart);
					theWriter.newLine();
					found = true;
					}
				theWriter.write("<"+cColumnName+"=\""+mTableModel.getColumnTitleNoAlias(column)+"\">");
				theWriter.newLine();
				Iterator<String> iterator = map.keySet().iterator();
				while (iterator.hasNext()) {
					String key = iterator.next();
					theWriter.write("<"+cColumnProperty+"=\""+key+"\t"+map.get(key)+"\">");
					theWriter.newLine();
					}
				}
			}
		if (found) {
			theWriter.write(cColumnPropertyEnd);
			theWriter.newLine();
			}
		}

	private void writeHitlists(BufferedWriter theWriter) throws IOException {
		CompoundTableListHandler hitlistHandler = mTableModel.getListHandler();
		if (mTableModel.hasSelectedRows() || (hitlistHandler != null && hitlistHandler.getListCount() != 0)) {
			theWriter.write(cHitlistDataStart);
			theWriter.newLine();
			if (mTableModel.hasSelectedRows())
				writeOneHitlist(theWriter, CompoundRecord.cFlagSelected, CompoundTableListHandler.LIST_CODE_SELECTION);
			if (hitlistHandler != null)
				for (int list = 0; list<hitlistHandler.getListCount(); list++)
					writeOneHitlist(theWriter, hitlistHandler.getListFlagNo(list), hitlistHandler.getListName(list));
			theWriter.write(cHitlistDataEnd);
			theWriter.newLine();
			}
		}

	private void writeOneHitlist(BufferedWriter theWriter, int flagNo, String listName) throws IOException {
		int rowCount = mVisibleOnly ? mTableModel.getRowCount() : mTableModel.getTotalRowCount();
		byte[] data = new byte[(5+rowCount)/6];
		int dataBit = 1;
		int dataIndex = 0;
		for (int row=0; row<rowCount; row++) {
			CompoundRecord record = mVisibleOnly ? mTableModel.getRecord(row) : mTableModel.getTotalRecord(row);
			if (record.isFlagSet(flagNo))
				data[dataIndex] |= dataBit;
			dataBit *= 2;
			if (dataBit == 64) {
				dataBit = 1;
				dataIndex++;
				}
			}
		for (int i=0; i<data.length; i++)
			data[i] += 64;
		theWriter.write("<"+cHitlistName+"=\""+listName+"\">");
		theWriter.newLine();
		for (int offset=0; offset<data.length; offset+=80) {
			String value = new String(data, offset, Math.min(80, data.length-offset));
			theWriter.write("<"+cHitlistData+"=\""+value+"\">");
			theWriter.newLine();
			}
		}

	private void writeEmbeddedDetails(BufferedWriter theWriter) throws IOException {
		HashMap<String,byte[]> detailMap = mTableModel.getDetailHandler().getEmbeddedDetailMap();

		TreeSet<String> usedIDSet = null;
		if (mVisibleOnly) {		// save only details that are referenced from visible records
			usedIDSet = new TreeSet<>();
			for (int row=0; row<mTableModel.getRowCount(); row++) {
				for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
					String[][] key = mTableModel.getRecord(row).getDetailReferences(column);
					if (key != null)
						for (int detailIndex=0; detailIndex<key.length; detailIndex++)
							if (key[detailIndex] != null)
								for (int i=0; i<key[detailIndex].length; i++)
									usedIDSet.add(key[detailIndex][i]);
					}
				}
			}

		if (detailMap != null && detailMap.size() != 0 && (usedIDSet == null || !usedIDSet.isEmpty())) {
			theWriter.write(cDetailDataStart);
			theWriter.newLine();
			for (String id:detailMap.keySet()) {
				if (usedIDSet == null || usedIDSet.contains(id)) {
					byte[] detail = detailMap.get(id);
					if (detail != null) {
						theWriter.write("<"+cDetailID+"=\""+id+"\">");
						theWriter.newLine();

						BinaryEncoder encoder = new BinaryEncoder(theWriter);
						encoder.initialize(8, detail.length);
						for (int i=0; i<detail.length; i++)
							encoder.write(detail[i]);
						encoder.finalize();

						theWriter.write("</"+cDetailID+">");
						theWriter.newLine();
						}
					}
				}
			theWriter.write(cDetailDataEnd);
			theWriter.newLine();
			}
		}

	private void embedAllDetails() {
		int errorCount = 0;
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
			for (int detail=0; detail<mTableModel.getColumnDetailCount(column); detail++) {
				String source = mTableModel.getColumnDetailSource(column, detail);
				if (!source.equals(CompoundTableDetailHandler.EMBEDDED)) {
					ArrayList<String> keyList = new ArrayList<String>();
					for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
						String[][] references = mTableModel.getTotalRecord(row).getDetailReferences(column);
						if (references != null && references.length>detail && references[detail] != null) {
							for (int i=0; i<references[detail].length; i++) {
								keyList.add(references[detail][i]);
								}
							}
						}

					String type = mTableModel.getColumnDetailType(column, detail);
					CompoundTableDetailSpecification sourceSpec = new CompoundTableDetailSpecification(mTableModel, column, detail);
					HashMap<String,String> oldToNewKeyMap = mTableModel.getDetailHandler().embedDetails(keyList.toArray(), sourceSpec, ReferenceResolver.MODE_DEFAULT, type, mProgressDialog);

					if (oldToNewKeyMap != null) {
						for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
							String[][] references = mTableModel.getTotalRecord(row).getDetailReferences(column);
							if (references != null && references.length>detail && references[detail] != null) {
								for (int i=0; i<references[detail].length; i++) {
									String newKey = oldToNewKeyMap.get(references[detail][i]);
									if (newKey != null)
										references[detail][i] = newKey;
									}
								}
							}
						mTableModel.setColumnDetailSource(column, detail, CompoundTableDetailHandler.EMBEDDED);
						}
					else {
						errorCount++;
						}
					}
				}
			}

		if (errorCount != 0 && mProgressDialog != null)
			mProgressDialog.showErrorMessage("Some detail data could not be embedded and won't be saved.");
		}

	private void writeDataDependentProperties(BufferedWriter theWriter) throws IOException {
		if (mDataDependentPropertyWriterList != null) {
			for (DataDependentPropertyWriter ddpw:mDataDependentPropertyWriterList) {
				if (ddpw.hasSomethingToWrite()) {
					theWriter.write(cDataDependentPropertiesStart);
					theWriter.write(ddpw.getPropertyName());
					theWriter.write("\">");
					theWriter.newLine();
					ddpw.write(theWriter);
					theWriter.write(cDataDependentPropertiesEnd);
					theWriter.newLine();
					}
				}
			}
		}

	private void writeSDData() throws IOException {
		synchronized(mDataWriter) {
			boolean is3D = CompoundTableConstants.cColumnType3DCoordinates.equals(mTableModel.getColumnSpecialType(mSDColumnCoordinates));
			boolean writeMultipleConformers = is3D && hasMultiConformerRows();

			BufferedWriter theWriter = new BufferedWriter(mDataWriter);

			if (mProgressDialog != null)
				mProgressDialog.startProgress("Saving Records...", 0, mTableModel.getTotalRowCount());

			if (mSDReferenceMolecules != null) {
				for (StereoMolecule mol:mSDReferenceMolecules) {
					if (mDataType == FileHelper.cFileTypeSDV3)
						new MolfileV3Creator(mol).writeMolfile(theWriter);
					else
						new MolfileCreator(mol).writeMolfile(theWriter);

					theWriter.write("$$$$");
					theWriter.newLine();
					}
				}

			StereoMolecule mol = new StereoMolecule();

			IDCodeParser parser = new IDCodeParser(!is3D);

			for (int row=0; row<mTableModel.getTotalRowCount(); row++) {			
				if (mProgressDialog != null)
					mProgressDialog.updateProgress(row);

				boolean nextRecordAvailable = true;
				int coordsIndex = -1;
				int conformerNo = 0;

				while (nextRecordAvailable) {
					nextRecordAvailable = false;
					if (mProgressDialog != null && mProgressDialog.threadMustDie())
						break;

					CompoundRecord record = mTableModel.getTotalRecord(row);
					if (mSDColumnStructure != -1) {
						byte[] idcode = (byte[])record.getData(mSDColumnStructure);
						byte[] coords = (byte[])record.getData(mSDColumnCoordinates);

						if (idcode != null) {
							if (is3D) {
								parser.parse(mol, idcode, coords, 0, coordsIndex+1);
								if (writeMultipleConformers) {
									conformerNo++;
									coordsIndex = ArrayUtils.indexOf(coords, (byte)' ', coordsIndex+1);
									nextRecordAvailable = (coordsIndex != -1);
									}
								}
							else {
								parser.parse(mol, idcode, coords);
								}

							if (mSDColumnIdentifier != -1) {
								byte[] name = (mSDColumnIdentifier == ID_BUILD_ONE) ?
										  ("Compound ".concat(Integer.toString(row+1))).getBytes()
										: (byte[])record.getData(mSDColumnIdentifier);
								if (name != null)
									mol.setName(new String(name).replace("\n", "; "));
								}
							}
						else {
							mol.clear();
							}
						}

					if (mDataType == FileHelper.cFileTypeSDV3)
						new MolfileV3Creator(mol).writeMolfile(theWriter);
					else
						new MolfileCreator(mol).writeMolfile(theWriter);

					if (writeMultipleConformers) {
						theWriter.write(">  <Conformer No>");
						theWriter.newLine();
						theWriter.write(Integer.toString(conformerNo));
						theWriter.newLine();
						theWriter.newLine();
						}

					for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
						if (mTableModel.getColumnSpecialType(column) == null) {
							theWriter.write(">  <"+mTableModel.getColumnTitle(column)+">");
							theWriter.newLine();

							// if we split conformers then check for an 'Energy' column and split energy values also
							if (writeMultipleConformers
									&& ("Energy".equals(mTableModel.getColumnTitle(column))
									 || "Minimization Error".equals(mTableModel.getColumnTitle(column)))) {
								String[] entries = mTableModel.separateEntries(mTableModel.encodeData(record, column));
								if (entries.length >= conformerNo) {
									theWriter.write(entries[conformerNo-1]);
									theWriter.newLine();
									}
								}
							else {
								String[] entries = mTableModel.encodeData(record, column).split(NEWLINE_REGEX);
								int count = 0;
								for (String entry : entries) {
									if (entry.length() != 0) {
										theWriter.write(entry);
										theWriter.newLine();
										count++;
										}
									}
								if (count == 0)
									theWriter.newLine();
								}

							theWriter.newLine();
							}
						}

					theWriter.write("$$$$");
					theWriter.newLine();
					}
				}
			theWriter.close();
			}
		}

	private boolean hasMultiConformerRows() {
		for (int row=0; row<mTableModel.getTotalRowCount() && row<MULTI_CONFORMER_ROWS_TO_CHECK; row++) {
			byte[] coords = (byte[])mTableModel.getTotalRecord(row).getData(mSDColumnCoordinates);
			if (coords != null && ArrayUtils.indexOf(coords, (byte)' ') != -1)
				return true;
			}
		return false;
		}

	public void run() {
		boolean successful = true;

		if (mEmbedDetails)
			embedAllDetails();

		try {
			switch (mDataType) {
			case FileHelper.cFileTypeDataWarrior:
			case FileHelper.cFileTypeDataWarriorTemplate:
			case FileHelper.cFileTypeTextTabDelimited:
				writeTextData();
				break;
			case FileHelper.cFileTypeSDV2:
			case FileHelper.cFileTypeSDV3:
				writeSDData();
				break;
				}

			if (mDataType == FileHelper.cFileTypeDataWarrior && !mVisibleOnly) {
				mTableModel.setFile(mFile);
				if (mParentFrame != null)
					mParentFrame.setTitle(mFile.getName());
				}
			}
		catch (IOException e) {
			if (mProgressDialog != null)
				mProgressDialog.showErrorMessage(e.toString());
			successful = false;
			}

		if (mProgressDialog != null && mProgressDialog.threadMustDie())	// action was cancelled
			successful = false;

		finalStatus(successful ? mFile : null);
		if (mProgressDialog != null)
			mProgressDialog.close(null);
		}

	/**
	 * This function serves as a callback function to report the success when the saver thread is done.
	 * Overwrite this, if you need the status after saving.
	 * @param file valid file if successful, otherwise null
	 */
	public void finalStatus(File file) {}
	}
