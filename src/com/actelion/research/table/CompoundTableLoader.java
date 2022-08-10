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

import com.actelion.research.calc.ProgressController;
import com.actelion.research.calc.ProgressListener;
import com.actelion.research.chem.*;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorHandler;
import com.actelion.research.chem.io.CompoundFileHelper;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.io.RDFileParser;
import com.actelion.research.chem.io.SDFileParser;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.chem.reaction.ReactionEncoder;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.JProgressDialog;
import com.actelion.research.io.BOMSkipper;
import com.actelion.research.table.model.*;
import com.actelion.research.table.view.config.ViewConfiguration;
import com.actelion.research.util.BinaryDecoder;
import com.actelion.research.util.ByteArrayArrayComparator;
import com.actelion.research.util.ByteArrayComparator;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

public class CompoundTableLoader implements CompoundTableConstants,Runnable {
	public static final String DATASET_COLUMN_TITLE = "Dataset Name";

	public static final byte NEWLINE = '\n';			// used in String values of TableModel
	public static final byte[] NEWLINE_BYTES = NEWLINE_STRING.getBytes();

	public static final byte TAB = '\t';				// used in String values of TableModel
	public static final byte[] TAB_BYTES = TAB_STRING.getBytes();

	public static final int	NEW_COLUMN = -1;	// pseudo destination columns for appending/merging data
	public static final int	NO_COLUMN = -2;

	public static final int MERGE_MODE_IS_KEY = 0;
	public static final int MERGE_MODE_IS_KEY_NO_CASE = 1;
	public static final int MERGE_MODE_IS_KEY_WORD_SEARCH = 2;
	public static final int MERGE_MODE_APPEND = 3;
	public static final int MERGE_MODE_KEEP = 4;
	public static final int MERGE_MODE_REPLACE = 5;
	public static final int MERGE_MODE_USE_IF_EMPTY = 6;
	public static final int MERGE_MODE_AS_PARENT = 7;	// <- from here merge mode(s) not being available for user selection

	public static final int READ_DATA = 1;		// load data into buffer
	public static final int REPLACE_DATA = 2;	// empty tablemodel and copy data from buffer to tablemodel
	public static final int APPEND_DATA = 4;	// append data from buffer to tablemodel
	public static final int MERGE_DATA = 8;		// merge data from buffer to tablemodel
	public static final int APPLY_TEMPLATE = 16;// apply the loaded template

	public static final byte[] QUOTES1_BYTES = "\\\"".getBytes();
	public static final byte[] QUOTES2_BYTES = "\"\"".getBytes();

	private static final int PROGRESS_LIMIT = 50000;
	private static final int PROGRESS_STEP = 200;

	private static final int MAX_COLUMNS_FOR_SMILES_CHECK = 64;
	private static final int MAX_ROWS_FOR_SMILES_CHECK = 20;
	private static final float MAX_TOLERATED_SMILES_FAILURE_RATE = 0.2f;


	private static volatile IdentifierHandler sIdentifierHandler = new IdentifierHandler() {
		@Override
		public TreeMap<String,String> addDefaultColumnProperties(String columnName, TreeMap<String,String> columnProperties) {
			return columnProperties;
			}

		@Override
		public String getSubstanceIdentifierName() {
			return "Substance-ID";
			}

		@Override
		public String getBatchIdentifierName() {
			return "Batch-ID";
			}

		@Override
		public boolean isIdentifierName(String s) {
			return s.equals(getSubstanceIdentifierName()) || s.equals(getBatchIdentifierName());
			}

		@Override
		public boolean isValidSubstanceIdentifier(String s) {
			return false;
			}

		@Override
		public boolean isValidBatchIdentifier(String s) {
			return false;
			}

		@Override
		public String normalizeIdentifierName(String identifierName) {
			return identifierName;
			}

		@Override
		public void setIdentifierAliases(CompoundTableModel tableModel) {}
	};

	private volatile CompoundTableModel mTableModel;
	private Frame				mParentFrame;
	private volatile ProgressController	mProgressController;
	private volatile File		mFile;
	private volatile Reader		mDataReader;
	private volatile int		mDataType,mAction;
	private volatile TreeMap<String,DataDependentPropertyReader> mDataDependentPropertyReaderMap;
	private int					mOldVersionIDCodeColumn,mOldVersionCoordinateColumn,mOldVersionCoordinate3DColumn;
	private boolean				mWithHeaderLine,mAppendRest,mCoordsMayBe3D,mIsGooglePatentsFile,
								mMolnameFound,mMolnameIsDifferentFromFirstField,mAssumeChiralFlag;
	private volatile boolean	mOwnsProgressController;
	private volatile char		mComma;
	private String				mNewWindowTitle,mVersion;
	private RuntimeProperties	mRuntimeProperties;
	private String[]			mFieldNames;
	private Object[][]			mFieldData;
	private volatile Thread		mThread;
	private byte[]				mSelectionData;
	private int					mAppendDatasetColumn,mFirstNewColumn,mSelectionRowCount,mSelectionOffset,mErrorCount;
	private int[]				mAppendDestColumn,mMergeDestColumn,mMergeMode;
	private int[][]				mSelectionDestRowMap;
	private String				mAppendDatasetNameExisting,mAppendDatasetNameNew;
	private ArrayList<String>	mHitlists;
	private TreeMap<String,String> mColumnProperties;
	private TreeMap<String,Object> mExtensionMap;
	private HashMap<String,byte[]> mDetails;

	public static void setColumnPropertyProvider(IdentifierHandler p) {
		sIdentifierHandler = p;
		}

	/**
	 * If this contructor is not invoked from the EventDispatchThread, then a valid
	 * progress controller must be specified.
	 * @param parent
	 * @param tableModel
	 * @param pc
	 */
	public CompoundTableLoader(Frame parent, CompoundTableModel tableModel, ProgressController pc) {
		mParentFrame = parent;
		mTableModel = tableModel;
		mProgressController = pc;
		}

	public void addDataDependentPropertyReader(String name, DataDependentPropertyReader ddpr) {
		if (mDataDependentPropertyReaderMap == null)
			mDataDependentPropertyReaderMap = new TreeMap<>();
		mDataDependentPropertyReaderMap.put(name, ddpr);
		}

	/**
	 * @param headerRow 0:no, 1:yes, 2:analyze
	 */
	public void paste(int headerRow) {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		if (!clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor))
			return;

		try {
			String s = (String)clipboard.getData(DataFlavor.stringFlavor);
			mWithHeaderLine = false;
			if (headerRow == 1)
				mWithHeaderLine = true;
			else if (headerRow != 0)
				mWithHeaderLine = analyzeHeaderLine(new StringReader(s));
			mDataReader = new StringReader(s);
			mAction = READ_DATA | REPLACE_DATA;
			mDataType = FileHelper.cFileTypeTextTabDelimited;
			mNewWindowTitle = "Data From Clipboard";
			mRuntimeProperties = null;
			processData();
			}
		catch (Exception e) {
			mTableModel.unlock();
			e.printStackTrace();
			showMessageOnEDT(e.toString(), "Paste Error", JOptionPane.WARNING_MESSAGE);
			}
		}

	/**
	 * Reads a
	 * @param url
	 * @param properties
	 */
	public void readFile(URL url, RuntimeProperties properties) {
		try {
			mDataReader = new InputStreamReader(url.openStream());
			}
		catch (IOException e) {
			mTableModel.unlock();
			showMessageOnEDT("IO-Exception during file retrieval.", "Retrieval Error", JOptionPane.WARNING_MESSAGE);
			return;
			}
		mDataType = FileHelper.cFileTypeDataWarrior;
		mAction = READ_DATA | REPLACE_DATA;
		mWithHeaderLine = true;
		mNewWindowTitle = url.toString();
		mRuntimeProperties = properties;
		processData();
		}

	public void readFile(File file, RuntimeProperties properties) {
		readFile(file, properties, FileHelper.cFileTypeDataWarrior, READ_DATA | REPLACE_DATA);
		}

	public void readTemplate(File file, RuntimeProperties properties) {
		readFile(file, properties, FileHelper.cFileTypeDataWarriorTemplate, READ_DATA | APPLY_TEMPLATE);
		}

	public void readFile(File file, RuntimeProperties properties, int dataType) {
		readFile(file, properties, dataType, READ_DATA | REPLACE_DATA);
		}

	public void readFile(File file, RuntimeProperties properties, int dataType, int action) {
		mFile = file;
		try {
			InputStream is = new FileInputStream(mFile);
			if (dataType == CompoundFileHelper.cFileTypeSDGZ) {
				is = new GZIPInputStream(is);
				dataType = CompoundFileHelper.cFileTypeSD;
				}
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			BOMSkipper.skip(reader);
			readStream(reader, properties, dataType, action, mFile.getName());
			}
		catch (FileNotFoundException e) {
			mTableModel.unlock();
			showMessageOnEDT("File not found.", "Error", JOptionPane.WARNING_MESSAGE);
			return;
			}
		catch (UnsupportedEncodingException e) {
			mTableModel.unlock();
			showMessageOnEDT("Unsupported encoding.", "File Format Error", JOptionPane.WARNING_MESSAGE);
			return;
			}
		catch (IOException e) {
			mTableModel.unlock();
			showMessageOnEDT("IO-Exception.", "Error", JOptionPane.WARNING_MESSAGE);
			return;
			}
		}

	public void readStream(BufferedReader reader, RuntimeProperties properties, int dataType, int action,
	                       String windowTitle) {
		mDataReader = reader;
		mDataType = dataType;
		mAction = action;
		mWithHeaderLine = true;
		mNewWindowTitle = windowTitle;
		mRuntimeProperties = properties;
		processData();
		}

	public String[] getFieldNames() {
		while (mThread != null)
			try { Thread.sleep(100); } catch (InterruptedException e) {}

		return mFieldNames;
		}

	public void appendFile(int[] destColumn, int datasetColumn, String existingSetName, String newSetName) {
		mAction = APPEND_DATA;
		mAppendDestColumn = destColumn;
		mAppendDatasetColumn = datasetColumn;
		mAppendDatasetNameExisting = existingSetName;
		mAppendDatasetNameNew = newSetName;
		processData();
		}

	/**
	 * Checks whether the columns defined in mergeMode as keys have content that uniquely
	 * identifies every row.
	 * @param keyColumnName matching the total previously read column count with at least one MERGE_MODE_IS_KEY entry
	 * @param isIgnoreCase defines for every key column, whether its content shall be treated in a case sensitive way
	 * @param pl null or progress listener to receive messages
	 * @return true if all 
	 */
	public boolean areMergeKeysUnique(String[] keyColumnName, boolean[] isIgnoreCase, ProgressListener pl) {
		if (pl != null)
			pl.startProgress("Sorting new keys...", 0, (mFieldData.length > PROGRESS_LIMIT) ? mFieldData.length : 0);

		int[] keyColumn = new int[keyColumnName.length];
		for (int i=0; i<keyColumnName.length; i++) {
			for (int j=0; j<mFieldNames.length; j++) {
				if (mFieldNames[j].equals(keyColumnName[i])) {
					keyColumn[i] = j;
					break;
					}
				}
			}

		TreeSet<byte[]> newKeySet = new TreeSet<byte[]>(new ByteArrayComparator());
		for (int row=0; row<mFieldData.length; row++) {
			if (pl != null && mFieldData.length > PROGRESS_LIMIT && row%PROGRESS_STEP == 0)
				pl.updateProgress(row);

			byte[] key = constructMergeKey(mFieldData[row], keyColumn, isIgnoreCase);
			if (key != null) {
				if (newKeySet.contains(key))
					return false;

				newKeySet.add(key);
				}
			}
		return true;
		}

	/**
	 * Combines all individual byte arrays from all key columns
	 * separated by TAB codes. If none of the columns contain any data, then null is returned.
	 * For columns that are marked to ignore the case, upper case letters (A-Z) are set to lower case
	 * @param rowData
	 * @param keyColumn
	 * @param isIgnoreCase
	 * @return null or row key as byte array
	 */
	private byte[] constructMergeKey(Object[] rowData, int[] keyColumn, boolean[] isIgnoreCase) {
		int count = keyColumn.length - 1;	// TABs needed
		for (int i=0; i<keyColumn.length; i++) {
			byte[] data = (byte[])rowData[keyColumn[i]];
			if (data != null)
				count += data.length;
			}
		if (count == keyColumn.length - 1)
			return null;

		byte[] key = new byte[count];
		int index = 0;
		for (int i=0; i<keyColumn.length; i++) {
			if (i != 0)
				key[index++] = '\t';
			byte[] data = (byte[])rowData[keyColumn[i]];
			if (data != null) {
				if (isIgnoreCase[i])
					data = new String(data).toLowerCase().getBytes();
				for (byte b : data)
					key[index++] = b;
				}
			}

		return key;
		}

	/**
	 * Merges previously read file content into the associated table model.
	 * Prior to this method either paste() or one of the readFile() methods
	 * must have been called. Then areMergeKeysUnique() must have been called
	 * and must have returned true.
	 * @param destColumn
	 * @param mergeMode
	 * @param appendRest
	 */
	public void mergeFile(int[] destColumn, int[] mergeMode, boolean appendRest) {
		mAction = MERGE_DATA;
		mMergeDestColumn = destColumn;
		mMergeMode = mergeMode;
		mAppendRest = appendRest;
		processData();
		}

	private void processData() {
		if (SwingUtilities.isEventDispatchThread()) {
			if (mProgressController == null) {
				mProgressController = new JProgressDialog(mParentFrame);
				mOwnsProgressController = true;
				}
	
			mThread = new Thread(this, "CompoundTableLoader");
			mThread.setPriority(Thread.MIN_PRIORITY);
			mThread.start();

			if (mOwnsProgressController)
				((JProgressDialog)mProgressController).setVisible(true);
			}
		else {
			run();
			}
		}

	private boolean analyzeHeaderLine(Reader reader) throws Exception {
		BufferedReader theReader = new BufferedReader(reader);

		ArrayList<String> lineList = new ArrayList<String>();
		try {
			while (true) {
				String theLine = theReader.readLine();
				if (theLine == null)
					break;

				lineList.add(theLine);
				}
			}
		catch (IOException e) {}
		theReader.close();

		if (lineList.size() < 2)
			return false;

		char columnSeparator = (mDataType == FileHelper.cFileTypeTextCommaSeparated) ? ',' : '\t';
		ArrayList<String> columnNameList = new ArrayList<String>();
		String header = lineList.get(0);
		int fromIndex = 0;
		int toIndex;
		do {
			String columnName;
			toIndex = header.indexOf(columnSeparator, fromIndex);

			if (toIndex == -1) {
				columnName = header.substring(fromIndex);
				}
			else {
				columnName = header.substring(fromIndex, toIndex);
				fromIndex = toIndex+1;
				}

			if (sIdentifierHandler.isIdentifierName(columnName)
			 || columnName.equalsIgnoreCase("Substance Name")
			 || columnName.equalsIgnoreCase("smiles")
			 || columnName.equalsIgnoreCase("idcode")
			 || columnName.endsWith("[idcode]")
			 || columnName.endsWith("[rxncode]")
			 || columnName.startsWith("fingerprint"))
				return true;

			columnNameList.add(columnName);
			} while (toIndex != -1);

		boolean[] isNotNumerical = new boolean[columnNameList.size()];
		for (int row=1; row<lineList.size(); row++) {
			String theLine = lineList.get(row);
			fromIndex = 0;
			int sourceColumn = 0;
			do {
				String value;
				toIndex = theLine.indexOf(columnSeparator, fromIndex);
				if (toIndex == -1) {
					value = theLine.substring(fromIndex);
					}
				else {
					value = theLine.substring(fromIndex, toIndex);
					fromIndex = toIndex+1;
					}

				if (!isNotNumerical[sourceColumn] && value.length() != 0) {
					try {
						Double.parseDouble(value);
						}
					catch (NumberFormatException e) {
						isNotNumerical[sourceColumn] = true;
						}
					}

				sourceColumn++;
				} while (sourceColumn<columnNameList.size() && toIndex != -1);
			}

		for (int column=0; column<columnNameList.size(); column++) {
			if (!isNotNumerical[column]) {
				try {
					Double.parseDouble(columnNameList.get(column));
					}
				catch (NumberFormatException e) {
					return true;
					}
				}
			}

		return false;
		}

	private boolean readTemplateOnly() {
		mProgressController.startProgress("Reading Template...", 0, 0);

		BufferedReader theReader = new BufferedReader(mDataReader);

		try {
			while (true) {
				String theLine = theReader.readLine();
				if (theLine == null)
					break;

				if (ViewConfiguration.isStartTag(theLine, cViewConfigTagName)) {
					mRuntimeProperties.readViewConfiguration(theReader, theLine);
					continue;
					}

				if (theLine.equals(cPropertiesStart)) {
					mRuntimeProperties.read(theReader);
					break;
					}
				}
			theReader.close();
			}
		catch (IOException e) {}

		return true;
		}

	private boolean readTextData() {
		BufferedReader theReader = new BufferedReader(mDataReader);
		String header = null;
		mVersion = null;
		int rowCount = -1;
		boolean wasTouched = false;
		boolean runtimePropertiesRead = false;
		CompoundTableExtensionHandler extensionHandler = mTableModel.getExtensionHandler();
		ArrayList<byte[]> lineList = new ArrayList<>();
		StringBuilder lineBuilder = new StringBuilder();

		try {
			while (true) {
				boolean isFirstLine = !wasTouched;
				wasTouched = true;
				String theLine = theReader.readLine();
				if (theLine == null)
					break;

				if (isFirstLine && theLine.equals(cNativeFileHeaderStart)) {
					rowCount = readFileHeader(theReader);
					if (rowCount > PROGRESS_LIMIT)
						mProgressController.startProgress("Reading Data...", 0, (rowCount > PROGRESS_LIMIT) ? rowCount : 0);
					continue;
					}

				if (isFirstLine && isGooglePatentsFile(theLine)) {
					prepareGooglePatentsFile();
					continue;
					}

				if (extensionHandler != null) {
					String name = extensionHandler.extractExtensionName(theLine);
					if (name != null) {
						Object data = extensionHandler.readData(name, theReader);
						if (data != null) {
							if (mExtensionMap == null)
								mExtensionMap = new TreeMap<>();
							mExtensionMap.put(name, data);
							}
						continue;
						}
					}

				if (theLine.equals(cColumnPropertyStart)) {
					readColumnProperties(theReader);
					continue;
					}

				if (theLine.equals(cHitlistDataStart)) {
					readHitlistData(theReader);
					continue;
					}

				if (theLine.equals(cDetailDataStart)) {
					readDetailData(theReader);
					continue;
					}

				if (ViewConfiguration.isStartTag(theLine, cViewConfigTagName)) {
					if ((mAction & APPEND_DATA) == 0
					 && (mAction & MERGE_DATA) == 0
					 && mRuntimeProperties != null)
						mRuntimeProperties.readViewConfiguration(theReader, theLine);
					continue;
					}

				if (theLine.equals(cPropertiesStart)) {
					if ((mAction & APPEND_DATA) == 0
					 && (mAction & MERGE_DATA) == 0
					 && mRuntimeProperties != null)
						mRuntimeProperties.read(theReader);

					runtimePropertiesRead = true;
					continue;
					}

				if (theLine.startsWith(cDataDependentPropertiesStart)) {
					String name = extractValue(theLine);
					if (mDataDependentPropertyReaderMap != null && mDataDependentPropertyReaderMap.get(name) != null) {
						mDataDependentPropertyReaderMap.get(name).read(theReader);
						}
					else {
						do {
							theLine = theReader.readLine();
							} while (theLine != null && !theLine.equals(cDataDependentPropertiesEnd));
						}
					}

				if (runtimePropertiesRead) {	// behind runtime properties we allow sections for future use
					if (theLine.startsWith("<") && theLine.endsWith(">")) {
						String endTag = "</"+theLine.substring(1);
						do {
							theLine = theReader.readLine();
							} while (theLine != null && !theLine.equals(endTag));
						continue;
						}
					break;
					}

				if (theLine.length() == 0 && (mDataType != FileHelper.cFileTypeDataWarrior || header != null))
					continue;

				if (mDataType != FileHelper.cFileTypeDataWarriorTemplate) {
					if (mWithHeaderLine && header == null) {
						evaluateSeparatorSymbol(theLine);
						header = convertCSVLine(theLine, lineBuilder, theReader);
						}
					else {
						lineList.add(convertCSVLine(theLine, lineBuilder, theReader).getBytes());
						if (rowCount > PROGRESS_LIMIT && lineList.size()%PROGRESS_STEP == 0)
							mProgressController.updateProgress(lineList.size());
						}
					}
				}
			theReader.close();
			}
		catch (IOException e) {}

		if (mWithHeaderLine && header == null) {
			showMessageOnEDT("No header line found.", "File Format Error", JOptionPane.WARNING_MESSAGE);
			return false;
			}

		if (mColumnProperties != null) {
			for (String key:mColumnProperties.keySet()) {
				if (key.endsWith("\tspecialType") && mColumnProperties.get(key).equals("Catalysts")
				 || key.endsWith("\treactionPart") && mColumnProperties.get(key).equals("catalysts")) {
					showMessageOnEDT("Outdated reaction file format. Please download update from 'openmolecules.org'.", "File Outdated", JOptionPane.WARNING_MESSAGE);
					return false;
					}
				}
			}

		if (mDataType != FileHelper.cFileTypeDataWarriorTemplate)
			processLines(header, lineList);

		return true;
		}

	private boolean isGooglePatentsFile(String firstLine) {
		if (firstLine.startsWith("search URL:")) {
			int index = firstLine.indexOf("https://patents.google.com");
			if (index != -1) {
				mTableModel.setExtensionData(cExtensionNameFileExplanation,
						"Google Patent Search\nURL: ".concat(firstLine.substring(index)));
				return true;
				}
			}

		return false;
		}

	private void prepareGooglePatentsFile() {
		mIsGooglePatentsFile = true;
		mWithHeaderLine = true;
		}

	private void postProcessGooglePatentsFile() {
		boolean linkFound = false;
		for (int column=0; column<mFieldNames.length; column++) {
			// sections column contains categories separated by '|'.
			if ("sections".equals(mFieldNames[column]))
				for (int row=0; row<mFieldData.length; row++)
					if (mFieldData[row][column] != null)
						mFieldData[row][column] = new String((byte[])mFieldData[row][column]).replace("|", "; ").getBytes();
			if (mFieldNames[column].endsWith(" link")) {
				linkFound = true;
				String url = getCommonURL(column);
				if (url != null) {
					int length = url.length();
					for (int row=0; row<mFieldData.length; row++)
						if (mFieldData[row][column] != null)
							mFieldData[row][column] = Arrays.copyOfRange((byte[])mFieldData[row][column], length, ((byte[])mFieldData[row][column]).length);
					addColumnProperty(mFieldNames[column], cColumnPropertyLookupCount, "1");
					addColumnProperty(mFieldNames[column], cColumnPropertyLookupURL+"0", url+"%s");
					addColumnProperty(mFieldNames[column], cColumnPropertyLookupName+"0", mFieldNames[column].substring(0, mFieldNames[column].length() - 5));
					}
				}
			}
		// if we have no link column, we create one from the 'id' column
		if (!linkFound) {
			for (int column=0; column<mFieldNames.length; column++) {
				if (mFieldNames[column].equals("id")) {
					addColumnProperty(mFieldNames[column], cColumnPropertyLookupCount, "1");
					addColumnProperty(mFieldNames[column], cColumnPropertyLookupURL+"0", "https://patents.google.com/patent/%s");
					addColumnProperty(mFieldNames[column], cColumnPropertyLookupName+"0", "original patent");
					addColumnProperty(mFieldNames[column], cColumnPropertyLookupFilter+"0", cColumnPropertyLookupFilterRemoveMinus);
					}
				}
			}
		}

	private String getCommonURL(int column) {
		int index = 0;
		byte[] first = null;
		for (int row=0; row<mFieldData.length; row++) {
			if (mFieldData[row][column] != null) {
				if (first == null) {
					first = (byte[])mFieldData[row][column];
					continue;
					}
				byte[] other = (byte[])mFieldData[row][column];
				int max = Math.min(other.length-1, index == 0 ? first.length-1 : index);
				index = 0;
				while (index<max && first[index]==other[index])
					index++;
				while (index > 0 && first[index] != '/')
					index--;
				}
			}
		return index == 0 ? null : new String(first, 0, index+1);
		}

	private void evaluateSeparatorSymbol(String line) {
		if (mDataType != FileHelper.cFileTypeTextCommaSeparated || line == null)
			return;

		mComma = ',';

		int commaCount = 0;
		int vlineCount = 0;
		int semicolonCount = 0;
		boolean isQuoted = false;
		for (int i=0; i<line.length(); i++) {
			char c = line.charAt(i);

			if (c == '"')
				isQuoted = !isQuoted;

			if (!isQuoted) {
				if (c == ',')
					commaCount++;
				else if (c == ';')
					semicolonCount++;
				else if (c == '|')
					vlineCount++;
				}
			}

		if (commaCount == 0) {
			if (semicolonCount != 0 && askOnEDT(
					"Your file seems to contain ';' separators instead of commas.\nDo you want to separate content using ';' characters?",
					"Warning", JOptionPane.WARNING_MESSAGE))
				mComma = ';';
			else if (vlineCount != 0 && askOnEDT(
				"Your file seems to contain '|' separators instead of commas.\nDo you want to separate content using '|' characters?",
				"Warning", JOptionPane.WARNING_MESSAGE))
				mComma = '|';
			}
		}

	/**
	 * In case of comma separated converts the line into TAB delimited, removes quotes and encodes NL
	 * @param line
	 * @param lineBuilder
	 * @param theReader
	 * @return
	 * @throws IOException
	 */
	private String convertCSVLine(String line, StringBuilder lineBuilder, BufferedReader theReader) throws IOException {
		if (mDataType != FileHelper.cFileTypeTextCommaSeparated)
			return line;

		// for comma-separated files we allow TAB & NL within quoted strings
		lineBuilder.setLength(0);
		boolean isFirstColumnChar = true;
		boolean isQuotedSection = false;

		while (true) {
			for (int i = 0; i < line.length(); i++) {
				char theChar = line.charAt(i);

				if (isFirstColumnChar) {
					if (theChar == '\t') {
						if (isQuotedSection)
							lineBuilder.append("<TAB>");
						continue;
						}

					if (theChar == ' ' && !isQuotedSection)
						continue;

					if (theChar != mComma)
						isFirstColumnChar = false;

					if (theChar == '\"') {
						isQuotedSection = true;
						continue;
						}
					}
				else {
					if (theChar == '\t') {
						lineBuilder.append("<TAB>");
						continue;
						}

					if (!isQuotedSection && theChar == mComma)
						isFirstColumnChar = true;

					if ((theChar == '\\' || theChar == '\"') && i+1 < line.length() && line.charAt(i+1) == '\"') {
						theChar = '\"';
						i++;
						}
					else if (theChar == '\"') {
						isQuotedSection = false;
						continue;
						}
					}

				if (!isQuotedSection && theChar == mComma)
					theChar = '\t';

				lineBuilder.append(theChar);
				}

			if (!isQuotedSection)
				break;

			lineBuilder.append(NEWLINE_STRING);
			line = theReader.readLine();
			}

		return lineBuilder.toString();
		}

	private void processLines(String header, ArrayList<byte[]> lineList) {
		ArrayList<String> columnNameList = new ArrayList<String>();
		byte columnSeparator = (byte)'\t';

		// In case we have an old style idcode column 'name [idcode]', which may also be a modern
		// clipboard transfer, then we may have space delimited atom coordinates and potentially
		// need insert a new column for the detached coordinates.
		ArrayList<Integer> oldStyleIDCodeColumnList = null;

		if (mWithHeaderLine) {
			int fromIndex = 0;
			int toIndex = 0;
			do {
				String columnName;
				toIndex = header.indexOf(columnSeparator, fromIndex);
				if (toIndex == -1) {
					columnName = header.substring(fromIndex);
					}
				else {
					columnName = header.substring(fromIndex, toIndex);
					fromIndex = toIndex+1;
					}

				if (columnName.startsWith("\"") && columnName.endsWith("\""))
					columnName = columnName.substring(1, columnName.length()-1).trim();

				String[] type = cParentSpecialColumnTypes;
				for (int i=0; i<type.length; i++) {
					if (columnName.endsWith("["+type[i]+"]")) {
						columnName = columnName.substring(0, columnName.length()-type[i].length()-2).trim();
						addColumnProperty(columnName, cColumnPropertySpecialType, type[i]);
						if (type[i].equals(cColumnTypeIDCode)) {
							if (oldStyleIDCodeColumnList == null)
								oldStyleIDCodeColumnList = new ArrayList<>();
							oldStyleIDCodeColumnList.add(columnNameList.size());
							}
						}
					}

				if (mDataType == FileHelper.cFileTypeDataWarrior)
					columnNameList.add(columnName);
				else
					columnNameList.add(sIdentifierHandler.normalizeIdentifierName(columnName));
				} while (toIndex != -1);
			}

		if (mVersion == null)
			createColumnPropertiesForFilesPriorVersion270(columnNameList);

		if (!mWithHeaderLine && lineList.size() > 0) {
			byte[] lineBytes = lineList.get(0);
			columnNameList.add("Column 1");
			int no = 2;
			for (byte b:lineBytes)
				if (b == columnSeparator)
					columnNameList.add("Column "+no++);
			}

		int columnCount = columnNameList.size();

		mFieldNames = new String[columnCount];
		mFieldData = new Object[lineList.size()][columnCount];

		for (int column=0; column<columnCount; column++)
			mFieldNames[column] = columnNameList.get(column);

		boolean[] descriptorValid = new boolean[columnCount];
		DescriptorHandler<?,?>[] descriptorHandler = new DescriptorHandler[columnCount];
		for (int column=0; column<columnCount; column++) {
			descriptorHandler[column] = CompoundTableModel.getDefaultDescriptorHandler(getColumnSpecialType(mFieldNames[column]));
			descriptorValid[column] = descriptorHandler[column] != null
					&& descriptorHandler[column].getVersion().equals(
							mColumnProperties.get(mFieldNames[column] + "\t" + cColumnPropertyDescriptorVersion));
			}

		mProgressController.startProgress("Processing Records...", 0, (mFieldData.length > PROGRESS_LIMIT) ? mFieldData.length : 0);

		for (int row=0; row<mFieldData.length; row++) {
			if (mProgressController.threadMustDie())
				break;
			if (mFieldData.length > PROGRESS_LIMIT && row%PROGRESS_STEP == 0)
				mProgressController.updateProgress(row);
			byte[] lineBytes = lineList.get(row);
			lineList.set(row, null);
			int fromIndex = 0;
			int column = 0;
			do {
				int toIndex = fromIndex;
				int spaces = 0;

				while (toIndex<lineBytes.length && lineBytes[toIndex] != columnSeparator)
					toIndex++;

				if (toIndex == fromIndex) {
					mFieldData[row][column] = null;
					}
				else {
					byte[] cellBytes = Arrays.copyOfRange(lineBytes, fromIndex, toIndex);
	
					if (descriptorHandler[column] == null)
						mFieldData[row][column] = convertNLAndTAB(cellBytes);
					else if (descriptorValid[column])
						mFieldData[row][column] = descriptorHandler[column].decode(cellBytes);
					}

				fromIndex = toIndex + 1 + spaces;
				column++;
				} while (fromIndex<lineBytes.length && column<columnCount);
			}

		if (!mWithHeaderLine)
			deduceColumnTitles();

		if (oldStyleIDCodeColumnList != null)
			handleOldStyleIDCodes(oldStyleIDCodeColumnList.toArray(new Integer[0]));

		if (mDataType != FileHelper.cFileTypeDataWarrior)
			handleSmiles();

		if (mIsGooglePatentsFile)
			postProcessGooglePatentsFile();

		addDefaultLookupColumnProperties();

		if (mVersion == null) // a version entry exists since V3.0
			handlePotentially3DCoordinates();
		}

	private void addColumnProperty(String columnName, String key, String value) {
		if (mColumnProperties == null)
			mColumnProperties = new TreeMap<>();
		mColumnProperties.put(columnName+"\t"+key, value);
		}

	private void handleOldStyleIDCodes(Integer[] idcodeColumn) {
		int oldColumnCount = mFieldNames.length;
		boolean[] hasCoords = new boolean[oldColumnCount];
		boolean[] is3D = new boolean[oldColumnCount];

		int newColumnCount = 0;
		for (int i=0; i<idcodeColumn.length; i++) {
			for (int row=0; !hasCoords[idcodeColumn[i]] && row<mFieldData.length; row++) {
				byte[] bytes = (byte[])mFieldData[row][idcodeColumn[i]];
				if (bytes != null) {
					for (int j=0; j<bytes.length; j++) {
						if (bytes[j] == ' ' && bytes.length > j+1) {
							hasCoords[idcodeColumn[i]] = true;
							newColumnCount++;
							is3D[idcodeColumn[i]] = new IDCodeParser().coordinatesAre3D(bytes, bytes, 0, j+1);
							break;
							}
						}
					}
				}
			}

		// coordinate columns are inserted right after the respective idcode columns
		if (newColumnCount != 0) {
			String[] newFieldNames = new String[oldColumnCount+newColumnCount];
			int[] oldToAllColumn = new int[oldColumnCount];
			newColumnCount = 0;
			for (int i=0; i<oldColumnCount; i++) {
				oldToAllColumn[i] = i + newColumnCount;
				newFieldNames[i + newColumnCount] = mFieldNames[i];
				if (hasCoords[i])
					newColumnCount++;
				}
			for (int i=0; i<oldColumnCount; i++) {
				if (hasCoords[i]) {
					int coordsColumn = oldToAllColumn[i] + 1;
					String type = is3D[i] ? cColumnType3DCoordinates : cColumnType2DCoordinates;
					newFieldNames[coordsColumn] = ensureUniqueness(type, newFieldNames);
					addColumnProperty(newFieldNames[coordsColumn], cColumnPropertySpecialType, type);
					addColumnProperty(newFieldNames[coordsColumn], cColumnPropertyParentColumn, newFieldNames[coordsColumn-1]);
					}
				}
			mFieldNames = newFieldNames;

			for (int row=0; row<mFieldData.length; row++) {
				Object[] newFieldData = new Object[oldColumnCount+newColumnCount];
				for (int i=0; i<oldColumnCount; i++) {
					int newColumnIndex = oldToAllColumn[i];
					newFieldData[newColumnIndex] = mFieldData[row][i];
					if (hasCoords[i]) {
						byte[] bytes = (byte[])newFieldData[newColumnIndex];
						int index = -1;
						if (bytes != null) {
							for (int j=0; j<bytes.length; j++) {
								if (bytes[j] == ' ' && bytes.length > j+1) {
									index = j;
									break;
									}
								}
							}
						if (index != -1) {
							newFieldData[newColumnIndex+1] = Arrays.copyOfRange(bytes, index+1, bytes.length);
							newFieldData[newColumnIndex] = Arrays.copyOf(bytes, index);
							}
						}
					}
				mFieldData[row] = newFieldData;
				}
			}
		}

	private void handleSmiles() {
		StereoMolecule mol = new StereoMolecule();
		int columns = Math.min(MAX_COLUMNS_FOR_SMILES_CHECK, mFieldNames.length);
		for (int column=columns-1; column>=0; column--)
			if (checkForSmiles(mol, column, false))
				insertChemistryFromSmiles(mol, column, false);
			else if (checkForSmiles(mol, column, true))
				insertChemistryFromSmiles(mol, column, true);
		}

	private void insertChemistryFromSmiles(StereoMolecule mol, int smilesColumn, boolean isReaction) {
		int columnCount = mFieldNames.length;

		final String[] reactionColumnName = { "Reaction", "RxnMapping", "RxnCoords", "ReactionFP", "ReactantFFP", "ProductFFP", "Catalysts", "RxnCatFragFp", "RxnCatOrgFunc" };
		final String[] moleculeColumnName = { "Structure of "+mFieldNames[smilesColumn], "SmilesFragFp" };
		String[] newColumnName = isReaction ? reactionColumnName : moleculeColumnName;

		for (int row=0; row<mFieldData.length; row++) {
			Object[] newFieldData = new Object[columnCount+newColumnName.length];
			for (int i=0; i<columnCount; i++)
				newFieldData[i < smilesColumn ? i : i+newColumnName.length] = mFieldData[row][i];
			mFieldData[row] = newFieldData;
			}

		if (mFieldData.length > 500)
			insertChemistryFromSmilesSMP(smilesColumn, smilesColumn+newColumnName.length, isReaction);
		else
			insertChemistryFromSmiles(mol, smilesColumn, smilesColumn+newColumnName.length, isReaction);

		if (isReaction) {
			boolean catalystsFound = false;
			for (int row=0; row<mFieldData.length; row++) {
				if (mFieldData[row][smilesColumn+6] != null) {
					catalystsFound = true;
					break;
					}
				}
			boolean mappingFound = false;
			for (int row=0; row<mFieldData.length; row++) {
				if (mFieldData[row][smilesColumn+1] != null) {
					mappingFound = true;
					break;
				}
			}
			if (!catalystsFound) {
				newColumnName = Arrays.copyOf(reactionColumnName, reactionColumnName.length-3);
				for (int row=0; row<mFieldData.length; row++) {
					Object[] newFieldData = new Object[columnCount+newColumnName.length];
					for (int i=0; i<newFieldData.length; i++)
						newFieldData[i] = mFieldData[row][i < smilesColumn+newColumnName.length ? i : i+3];
					mFieldData[row] = newFieldData;
					}
				}
			newColumnName[0] = newColumnName[0].concat(" of ").concat(mFieldNames[smilesColumn]);
			}

		String[] newFieldNames = new String[columnCount+newColumnName.length];
		for (int i=0; i<columnCount; i++)
			newFieldNames[i < smilesColumn ? i : i+newColumnName.length] = mFieldNames[i];
		for (int i=0; i<newColumnName.length; i++)
			newFieldNames[smilesColumn+i] = ensureUniqueness(newColumnName[i], mFieldNames);
		mFieldNames = newFieldNames;

		if (isReaction) {
			addColumnProperty(newFieldNames[smilesColumn], cColumnPropertySpecialType, cColumnTypeRXNCode);
			addColumnProperty(newFieldNames[smilesColumn+1], cColumnPropertySpecialType, cColumnTypeReactionMapping);
			addColumnProperty(newFieldNames[smilesColumn+1], cColumnPropertyParentColumn, newFieldNames[smilesColumn]);
			addColumnProperty(newFieldNames[smilesColumn+2], cColumnPropertySpecialType, cColumnType2DCoordinates);
			addColumnProperty(newFieldNames[smilesColumn+2], cColumnPropertyParentColumn, newFieldNames[smilesColumn]);
			addColumnProperty(newFieldNames[smilesColumn+3], cColumnPropertySpecialType, DescriptorConstants.DESCRIPTOR_ReactionFP.shortName);
			addColumnProperty(newFieldNames[smilesColumn+3], cColumnPropertyDescriptorVersion, DescriptorConstants.DESCRIPTOR_ReactionFP.version);
			addColumnProperty(newFieldNames[smilesColumn+3], cColumnPropertyParentColumn, newFieldNames[smilesColumn]);
			addColumnProperty(newFieldNames[smilesColumn+4], cColumnPropertySpecialType, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
			addColumnProperty(newFieldNames[smilesColumn+4], cColumnPropertyDescriptorVersion, DescriptorConstants.DESCRIPTOR_FFP512.version);
			addColumnProperty(newFieldNames[smilesColumn+4], cColumnPropertyReactionPart, cReactionPartReactants);
			addColumnProperty(newFieldNames[smilesColumn+4], cColumnPropertyParentColumn, newFieldNames[smilesColumn]);
			addColumnProperty(newFieldNames[smilesColumn+5], cColumnPropertySpecialType, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
			addColumnProperty(newFieldNames[smilesColumn+5], cColumnPropertyDescriptorVersion, DescriptorConstants.DESCRIPTOR_FFP512.version);
			addColumnProperty(newFieldNames[smilesColumn+5], cColumnPropertyReactionPart, cReactionPartProducts);
			addColumnProperty(newFieldNames[smilesColumn+5], cColumnPropertyParentColumn, newFieldNames[smilesColumn]);
			if (newColumnName.length == reactionColumnName.length) {
				String catalystColumnName = newFieldNames[smilesColumn+6];
				addColumnProperty(newFieldNames[smilesColumn], cColumnPropertyRelatedCatalystColumn, catalystColumnName);
				addColumnProperty(newFieldNames[smilesColumn+6], cColumnPropertySpecialType, cColumnTypeIDCode);
				addColumnProperty(newFieldNames[smilesColumn+7], cColumnPropertySpecialType, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
				addColumnProperty(newFieldNames[smilesColumn+7], cColumnPropertyDescriptorVersion, DescriptorConstants.DESCRIPTOR_FFP512.version);
				addColumnProperty(newFieldNames[smilesColumn+7], cColumnPropertyParentColumn, catalystColumnName);
				addColumnProperty(newFieldNames[smilesColumn+8], cColumnPropertySpecialType, DescriptorConstants.DESCRIPTOR_OrganicFunctionalGroups.shortName);
				addColumnProperty(newFieldNames[smilesColumn+8], cColumnPropertyDescriptorVersion, DescriptorConstants.DESCRIPTOR_OrganicFunctionalGroups.version);
				addColumnProperty(newFieldNames[smilesColumn+8], cColumnPropertyParentColumn, catalystColumnName);
				}
			}
		else {
			addColumnProperty(newFieldNames[smilesColumn], cColumnPropertySpecialType, cColumnTypeIDCode);
			addColumnProperty(newFieldNames[smilesColumn+1], cColumnPropertySpecialType, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
			addColumnProperty(newFieldNames[smilesColumn+1], cColumnPropertyDescriptorVersion, DescriptorConstants.DESCRIPTOR_FFP512.version);
			addColumnProperty(newFieldNames[smilesColumn+1], cColumnPropertyParentColumn, newFieldNames[smilesColumn]);
			}
		}

	/**
	 * @param name name to be checked and potentially updated to be different from all non-null names
	 * @param names may contain null entries
	 * @return
	 */
	private String ensureUniqueness(String name, String[] names) {
		while (true) {
			boolean found = false;
			for (String n:names) {
				if (n != null && name.equalsIgnoreCase(n)) {
					found = true;
					break;
					}
				}
			if (!found)
				break;

			int index = name.lastIndexOf(' ');
			if (index == -1) {
				name = name + " 2";
				}
			else {
				try {
					int suffix = Integer.parseInt(name.substring(index + 1));
					name = name.substring(0, index + 1) + (suffix + 1);
					}
				catch (NumberFormatException nfe) {
					name = name + " 2";
					}
				}
			}

		return name;
		}

	private void insertChemistryFromSmiles(StereoMolecule mol, int chemistryColumn, int smilesColumn, boolean isReaction) {
		for (int row=0; row<mFieldData.length; row++) {
			if (isReaction)
				insertReactionCodeFromSmiles(chemistryColumn, smilesColumn, row);
			else
				insertIDCodeFromSmiles(mol, chemistryColumn, smilesColumn, row);
			}
		}

	private void insertChemistryFromSmilesSMP(int chemistryColumn, int smilesColumn, boolean isReaction) {
		Thread st = new Thread("Smiles Supervisor") {
			public void run() {
				int threadCount = Runtime.getRuntime().availableProcessors();
				final AtomicInteger mSMPIndex = new AtomicInteger(mFieldData.length);
				mProgressController.startProgress("Converting Smiles...", 0, mFieldData.length);
				Thread[] t = new Thread[threadCount];
				for (int i=0; i<threadCount; i++) {
					t[i] = new Thread("Smiles Parser "+(i+1)) {
						public void run() {
							StereoMolecule mol = new StereoMolecule();
							int row = mSMPIndex.decrementAndGet();
							while (row >= 0) {
								if (isReaction)
									insertReactionCodeFromSmiles(chemistryColumn, smilesColumn, row);
								else
									insertIDCodeFromSmiles(mol, chemistryColumn, smilesColumn, row);

								int progress = mFieldData.length - row;
								if ((progress & 63) == 0)
									mProgressController.updateProgress(progress);

								row = mSMPIndex.decrementAndGet();
								}
							}
						};
					t[i].setPriority(Thread.MIN_PRIORITY);
					t[i].start();
					}
				for (int i = 0; i < threadCount; i++)
					try {
						t[i].join();
						}
					catch (InterruptedException ie) {
					}
				}
			};
		st.start();
		try {
			st.join();
			}
		catch (InterruptedException ie) {}
		}

	private void insertIDCodeFromSmiles(StereoMolecule mol, int structureColumn, int smilesColumn, int row) {
		byte[] smiles = (byte[])mFieldData[row][smilesColumn];
		if (smiles != null && isValidSmiles(mol, smiles))
			mFieldData[row][structureColumn] = getIDCodeFromMolecule(mol);
		}

	private void insertReactionCodeFromSmiles(int reactionColumn, int smilesColumn, int row) {
		Reaction rxn = getReactionFromSmiles((byte[])mFieldData[row][smilesColumn]);
		if (rxn != null) {
			String[] rxnData = ReactionEncoder.encode(rxn, false);
			if (rxnData != null && rxnData[0] != null) {
				mFieldData[row][reactionColumn] = rxnData[0].getBytes();
				if (rxnData[1] != null && rxnData[1].length() != 0)
					mFieldData[row][reactionColumn+1] = rxnData[1].getBytes();
				if (rxnData[2] != null && rxnData[2].length() != 0)
					mFieldData[row][reactionColumn+2] = rxnData[2].getBytes();
				if (rxnData[4] != null && rxnData[4].length() != 0)
					mFieldData[row][reactionColumn+6] = rxnData[4].getBytes();
				}
			}
		}

	/**
	 * Checks whether a column contains valid SMILES codes, which is considered true if<br>
	 * - the first MAX_ROWS_FOR_SMILES_CHECK non-null entries in the column are valid SMILES<br>
	 * - or (if the column contains less than MAX_ROWS_FOR_SMILES_CHECK rows) every row contains a valid SMILES
	 * @param column
	 * @return
	 */
	private boolean checkForSmiles(StereoMolecule mol, int column, boolean isReaction) {
		int checked = 0;
		int failures = 0;
		int maxFailures = (int)(MAX_TOLERATED_SMILES_FAILURE_RATE * MAX_ROWS_FOR_SMILES_CHECK);
		for (int row=0; row<mFieldData.length; row++) {
			byte[] data = (byte[])mFieldData[row][column];
			if (data != null && data.length > 3) {
				checked++;
				if ((!isReaction && !isValidSmiles(mol, data))
				 || (isReaction && getReactionFromSmiles(data) == null)) {
					if (++failures >= maxFailures)
						return false;
					}

				if (checked > MAX_ROWS_FOR_SMILES_CHECK)
					break;
				}
			}
		return (checked != 0 && (float)failures/(float)checked <= MAX_TOLERATED_SMILES_FAILURE_RATE);
		}

	private boolean isValidSmiles(StereoMolecule mol, byte[] smiles) {
		if (smiles != null && smiles.length != 0) {
			try {
				new SmilesParser(SmilesParser.SMARTS_MODE_GUESS, false).parse(mol, smiles);
				return mol.getAllAtoms() != 0;
				}
			catch (Exception e) {}
			}
		return false;
		}

	private Reaction getReactionFromSmiles(byte[] smiles) {
		if (smiles != null && smiles.length != 0) {
			try {
				return new SmilesParser(SmilesParser.SMARTS_MODE_IS_SMILES, false).parseReaction(smiles);
				}
			catch (Exception e) {}
			}
		return null;
		}

	private byte[] getIDCodeFromMolecule(StereoMolecule mol) {
		try {
			mol.normalizeAmbiguousBonds();
			mol.canonizeCharge(true);
			Canonizer canonizer = new Canonizer(mol);
			canonizer.setSingleUnknownAsRacemicParity();
			return canonizer.getIDCode().getBytes();
			}
		catch (Exception e) {}

		return null;
		}

	private void deduceColumnTitles() {
		if (mFieldData.length == 0)
			return;

		for (int column=0; column<mFieldNames.length; column++) {
			int firstRow = 0;
			while (firstRow<mFieldData.length && mFieldData[firstRow][column] == null)
				firstRow++;
			if (firstRow == mFieldData.length || !(mFieldData[firstRow][column] instanceof byte[]))
				continue;

			boolean isSubstanceID = sIdentifierHandler.isValidSubstanceIdentifier(new String((byte[])mFieldData[firstRow][column]).trim());
			for (int row=firstRow+1; isSubstanceID && row<mFieldData.length; row++)
				if (mFieldData[row][column] != null)
					isSubstanceID = sIdentifierHandler.isValidSubstanceIdentifier(new String((byte[])mFieldData[row][column]).trim());

			if (isSubstanceID) {
				mFieldNames[column] = sIdentifierHandler.getSubstanceIdentifierName();
				continue;
				}

			boolean isBatchID = sIdentifierHandler.isValidBatchIdentifier(new String((byte[])mFieldData[firstRow][column]).trim());
			for (int row=firstRow+1; isBatchID && row<mFieldData.length; row++)
				if (mFieldData[row][column] != null)
					isBatchID = sIdentifierHandler.isValidBatchIdentifier(new String((byte[])mFieldData[row][column]).trim());

			if (isBatchID) {
				mFieldNames[column] = sIdentifierHandler.getBatchIdentifierName();
				continue;
				}
			}
		}

	private void addDefaultLookupColumnProperties() {
		if (sIdentifierHandler != null)
			for (int column=0; column<mFieldNames.length; column++)
				mColumnProperties = sIdentifierHandler.addDefaultColumnProperties(mFieldNames[column], mColumnProperties);
		}

	private void createColumnPropertiesForFilesPriorVersion270(ArrayList<String> columnNameList) {
			// Native DataWarrior files before V2.7.0 didn't have column properties
			// for column headers 'idcode','idcoordinates','fingerprint_Vxxx'.
			// There types were recognized by the column header only.
		mOldVersionIDCodeColumn = -1;
		mOldVersionCoordinateColumn = -1;
		for (int i=0; i<columnNameList.size(); i++) {
			String columnName = columnNameList.get(i);
			if (columnName.equals("idcode") && !columnHasProperty(columnName)) {
				columnNameList.set(i, "Structure");
				addColumnProperty("Structure", cColumnPropertySpecialType, cColumnTypeIDCode);
				for (int j=0; j<columnNameList.size(); j++) {
					if (columnName.equals(sIdentifierHandler.getSubstanceIdentifierName())) {
						addColumnProperty("Structure", cColumnPropertyRelatedIdentifierColumn, sIdentifierHandler.getSubstanceIdentifierName());
						break;
						}
					}
				mOldVersionIDCodeColumn = i;
				}
			if (mOldVersionIDCodeColumn != -1
			 && (columnName.equals("idcoordinates") || columnName.equals("idcoords"))
			 && !columnHasProperty(columnName)) {
				columnNameList.set(i, cColumnType2DCoordinates);
				addColumnProperty(cColumnType2DCoordinates, cColumnPropertySpecialType, cColumnType2DCoordinates);
				addColumnProperty(cColumnType2DCoordinates, cColumnPropertyParentColumn, "Structure");
				mOldVersionCoordinateColumn = i;
				}
			if (mOldVersionIDCodeColumn != -1
			 && columnName.startsWith("fingerprint_")
			 && !columnHasProperty(columnName)) {
				columnNameList.set(i, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
				addColumnProperty(DescriptorConstants.DESCRIPTOR_FFP512.shortName, cColumnPropertySpecialType, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
				addColumnProperty(DescriptorConstants.DESCRIPTOR_FFP512.shortName, cColumnPropertyParentColumn, "Structure");
				addColumnProperty(DescriptorConstants.DESCRIPTOR_FFP512.shortName, cColumnPropertyDescriptorVersion, columnName.substring(12));
				}
			}
		if (mOldVersionCoordinateColumn != -1)
			mCoordsMayBe3D = true;
		}

	private boolean columnHasProperty(String columnName) {
		if (mColumnProperties != null) {
			for (String key:mColumnProperties.keySet())
				if (key.startsWith(columnName+"\t"))
					return true;
			}
		return false;
		}

	private int readFileHeader(BufferedReader theReader) {
		int rowCount = -1;
		try {
			while (true) {
				String theLine = theReader.readLine();
				if (theLine == null
				 || theLine.equals(cNativeFileHeaderEnd)) {
					break;
					}

				if (theLine.startsWith("<"+cNativeFileVersion)) {
					mVersion = extractValue(theLine);
					continue;
					}

				if (theLine.startsWith("<"+cNativeFileRowCount)) {
					try {
						rowCount = Integer.parseInt(extractValue(theLine));
						}
					catch (NumberFormatException nfe) {}
					continue;
					}
				}
			}
		catch (Exception e) {}
		return rowCount;
		}

	private void readColumnProperties(BufferedReader theReader) {
		try {
			String columnName = null;
			while (true) {
				String theLine = theReader.readLine();
				if (theLine == null
				 || theLine.equals(cColumnPropertyEnd)) {
					break;
					}

				if (theLine.startsWith("<"+cColumnName)) {
					columnName = extractValue(theLine);
					continue;
					}

				if (theLine.startsWith("<"+cColumnProperty)) {
					String keyAndValue = extractOneValueLine(theLine);	// formulas may contain encoded double quotes

					// to support deprecated property cColumnPropertyIsIDCode => "isIDCode"
					if (keyAndValue.equals("isIDCode\ttrue")) {
						addColumnProperty(columnName, cColumnPropertySpecialType, cColumnTypeIDCode);
						}
					else {
						int index = keyAndValue.indexOf('\t');
						if (index != -1)
							addColumnProperty(columnName, keyAndValue.substring(0, index), keyAndValue.substring(index+1));
						}

					continue;
					}
				}
			}
		catch (Exception e) {
			mColumnProperties = null;
			}
		}

	public String getParentColumnName(String columnName) {
		return mColumnProperties == null ? null : mColumnProperties.get(columnName + "\t" + cColumnPropertyParentColumn);
		}

	public String getColumnSpecialType(String columnName) {
		return mColumnProperties == null ? null : mColumnProperties.get(columnName + "\t" + cColumnPropertySpecialType);
		}

	private void readHitlistData(BufferedReader theReader) {
		mHitlists = new ArrayList<String>();
		try {
			String hitlistName = null;
			StringBuffer hitlistData = null;
			while (true) {
				String theLine = theReader.readLine();
				if (theLine == null
				 || theLine.equals(cHitlistDataEnd)) {
					mHitlists.add(hitlistName + "\t" + hitlistData);
					break;
					}

				if (theLine.startsWith("<"+cHitlistName)) {
					if (hitlistName != null)
						mHitlists.add(hitlistName + "\t" + hitlistData);

					hitlistName = extractValue(theLine);
					hitlistData = new StringBuffer();
					continue;
					}

				if (theLine.startsWith("<"+cHitlistData)) {
					hitlistData.append(extractValue(theLine));
					continue;
					}
				}
			}
		catch (Exception e) {
			mHitlists = null;
			}
		}

	private void readDetailData(BufferedReader theReader) {
		mDetails = new HashMap<String,byte[]>();
		try {
			while (true) {
				String theLine = theReader.readLine();
				if (theLine == null
				 || theLine.equals(cDetailDataEnd)) {
					break;
					}

				if (theLine.startsWith("<"+cDetailID)) {
					String detailID = extractValue(theLine);
					String encoding = extractParameterValue(theLine,"encoding");
					if (encoding == null || encoding.equals("binary")) {
						BinaryDecoder decoder = new BinaryDecoder(theReader);
						int size = decoder.initialize(8);
						byte[] detailData = new byte[size];
						for (int i=0; i<size; i++)
							detailData[i] = (byte)decoder.read();
						mDetails.put(detailID, detailData);
						}
					else {	// one line of text
						StringBuilder sb = new StringBuilder();
						theLine = theReader.readLine();
						while (theLine != null && !theLine.startsWith("</"+cDetailID)) {
							if (sb.length() != 0)
								sb.append("\n");
							sb.append(theLine);
							theLine = theReader.readLine();
							}
						byte[] detailData = sb.toString().getBytes();
						mDetails.put(detailID, detailData);
						}
					}
				}
			}
		catch (Exception e) {
			mDetails = null;
			}
		}

	/**
	 * extracts and returns the first double quoted value directly following an equal sign
	 * @param theLine
	 * @return
	 */
	static public String extractValue(String theLine) {
		int index1 = theLine.indexOf("=\"") + 2;
		int index2 = theLine.indexOf("\"", index1);
		return theLine.substring(index1, index2);
		}

	/**
	 * extracts and returns the first double quoted value directly following an equal sign
	 * and ending with the last occurence of a quote followed by a larger sign.
	 * This method allows double quotes to occurr in the value.
	 * @param theLine
	 * @return
	 */
	static public String extractOneValueLine(String theLine) {
		int index1 = theLine.indexOf("=\"") + 2;
		int index2 = theLine.lastIndexOf("\">");
		return theLine.substring(index1, index2);
		}

	/**
	 * extracts and returns the value of the given parameter, which must be double quoted
	 * @param theLine
	 * @return null if parameter not found
	 */
	static public String extractParameterValue(String theLine, String paramName) {
		String value = null;
		int index1 = theLine.indexOf(" "+paramName+"=\"");
		if (index1 != -1) {
			index1 = theLine.indexOf("=\"", index1)+2;
			int index2 = theLine.indexOf("\"", index1);
			value = theLine.substring(index1, index2);
			}
		return value;
		}

	private byte[] getBytes(String s) {
		return (s == null || s.length() == 0) ? null : s.getBytes();
		}

	private boolean initializeReaderFromFile() {
		boolean isGZipped = CompoundFileHelper.getFileType(mFile.getName()) == CompoundFileHelper.cFileTypeSDGZ;
		try {
			mDataReader.close();

			InputStream is = new FileInputStream(mFile);
			if (isGZipped)
				is = new GZIPInputStream(is);

			mDataReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			BOMSkipper.skip(mDataReader);
			return true;
			}
		catch (IOException e) {
			return false;
			}
		}

	private boolean readSDFile() {
		mProgressController.startProgress("Examining Records...", 0, 0);

		SDFileParser sdParser = new SDFileParser(mDataReader);
		String[] fieldNames = sdParser.getFieldNames();
		int fieldCount = fieldNames.length;

		mFieldNames = new String[fieldCount+3];
		mFieldNames[0] = "Structure";
		mFieldNames[1] = cColumnType2DCoordinates;
		mFieldNames[2] = "Molecule Name";   // use record no as default column
		for (int i=0; i<fieldCount; i++)
			mFieldNames[3+i] = sIdentifierHandler.normalizeIdentifierName(fieldNames[i]);

		ArrayList<Object[]> fieldDataList = new ArrayList<Object[]>();

		mOldVersionIDCodeColumn = 0;
		mOldVersionCoordinateColumn = 1;
		mCoordsMayBe3D = true;

		int structureIDColumn = (fieldCount != 0
	   						 && (fieldNames[0].equals("ID")
	   						  || fieldNames[0].equals("IDNUMBER")
	   						  || fieldNames[0].equals(sIdentifierHandler.getSubstanceIdentifierName())
	   						  || fieldNames[0].equals("code"))) ? 3 : -1;

		// this takes preference
		for (int i=0; i<fieldCount; i++) {
			if (fieldNames[i].equals(sIdentifierHandler.getSubstanceIdentifierName())
			 || fieldNames[i].equals("EMOL_VERSION_ID")) {
				structureIDColumn = 3 + i;
				}
			}

		// In a first run we check, whether all molfiles are V2000 without chiral flag == 0 despite some
		// molfiles having defined stereo centers. If found then we may process the file a second time
		// assuming that molecules with defined stereo centers are meant to be enantiomerically pure.
		if (processSDFile(fieldNames, structureIDColumn, fieldDataList, mAssumeChiralFlag)
		 && askOnEDT("Erroneously, some programs store pure enantiomers as racemates when exporting to an SD-file.\n"
						+ "All molecules in this SD-file are marked as racemate and some molecules contain stereo centers.\n"
						+ "Do you want DataWarrior to interpret those stereo centers as absolute? If you answer YES, then\n"
						+ "molecules with defined stereo conters will be assumed to be pure enantiomers rather then racemates.", "Warning",
				JOptionPane.WARNING_MESSAGE)) {
			fieldDataList.clear();
			processSDFile(fieldNames, structureIDColumn, fieldDataList, true);
			}

		addColumnProperty("Structure", cColumnPropertySpecialType, cColumnTypeIDCode);
		addColumnProperty(cColumnType2DCoordinates, cColumnPropertySpecialType, cColumnType2DCoordinates);
		addColumnProperty(cColumnType2DCoordinates, cColumnPropertyParentColumn, "Structure");

		mFieldData = fieldDataList.toArray(new Object[0][]);

		if (structureIDColumn != -1) {
			addColumnProperty("Structure", cColumnPropertyRelatedIdentifierColumn, mFieldNames[structureIDColumn]);
			}
		else if (mMolnameFound) {
			addColumnProperty("Structure", cColumnPropertyRelatedIdentifierColumn, mFieldNames[2]);
			}
		else {
			mFieldNames[2] = "Structure No";
			for (int row=0; row<mFieldData.length; row++)
				mFieldData[row][2] = (""+(row+1)).getBytes();
			}

		// if the molname column is redundant, then delete it
		if (structureIDColumn != -1 && (!mMolnameFound || !mMolnameIsDifferentFromFirstField)) {
			for (int column=3; column<mFieldNames.length; column++)
				mFieldNames[column-1] = mFieldNames[column];
			mFieldNames = Arrays.copyOf(mFieldNames, mFieldNames.length-1);

			for (int row=0; row<mFieldData.length; row++) {
				for (int column=3; column<mFieldData[row].length; column++)
					mFieldData[row][column-1] = mFieldData[row][column];
				mFieldData[row] = Arrays.copyOf(mFieldData[row], mFieldData[row].length-1);
				}
			}

		if (mErrorCount > 0) {
			final String message = ""+mErrorCount+" compound structures could not be generated because of molfile parsing errors.";
			showMessageOnEDT(message, "Import Errors", JOptionPane.WARNING_MESSAGE);
			}

		handlePotentially3DCoordinates();

		addDefaultLookupColumnProperties();

		return true;
		}

	/**
	 * @param fieldNames
	 * @param structureIDColumn
	 * @param fieldDataList
	 * @param assumeChiralTrue
	 * @return true if no V3000 molfiles found, not set chiral flag found, but molecules with explicit stereo centers found
	 */
	private boolean processSDFile(String[] fieldNames, int structureIDColumn, ArrayList<Object[]> fieldDataList, boolean assumeChiralTrue) {
		initializeReaderFromFile();
		SDFileParser sdParser = new SDFileParser(mDataReader, fieldNames);
		MolfileParser mfParser = new MolfileParser();
		mfParser.setAssumeChiralTrue(assumeChiralTrue);
		StereoMolecule mol = new StereoMolecule();
		int recordNo = 0;
		mErrorCount = 0;
		mMolnameFound = false;
		mMolnameIsDifferentFromFirstField = false;
		int recordCount = sdParser.getRowCount();

		// If we find V2000 molfiles only of which none have a set chiral flag and if some molecules
		// have stereo centers, then the creating software may have errorneously not set the chiral flag.
		// We need to ask the user for an optional correction.
		boolean chiralFlagOrV3000Found = false;
		boolean stereoCentersFound = false;

		mProgressController.startProgress("Processing Records...", 0, (recordCount != -1) ? recordCount : 0);

		while (sdParser.next()) {
			if (mProgressController.threadMustDie())
				break;
			if (recordCount != -1 && recordNo%PROGRESS_STEP == 0)
				mProgressController.updateProgress(recordNo);

			Object[] fieldData = new Object[mFieldNames.length];

			String molname = null;
			try {
				String molfile = sdParser.getNextMolFile();

				BufferedReader r = new BufferedReader(new StringReader(molfile));
				molname = r.readLine().trim();
				r.readLine();
				String comment = r.readLine();

				// exclude manually CCDC entries with atoms that are in multiple locations.
				if (comment.contains("From CSD data") && !comment.contains("No disordered atoms"))
					throw new Exception("CSD molecule with ambivalent atom location.");

				// exclude manually CCDC entries with matching problems.
				if (comment.contains("From CSD data") && comment.contains("Matching problem"))
					throw new Exception("CSD molecule with matching problem.");

				mfParser.parse(mol, molfile);
				if (mol.getAllAtoms() != 0) {
					mol.normalizeAmbiguousBonds();
					mol.canonizeCharge(true);
					Canonizer canonizer = new Canonizer(mol);
					canonizer.setSingleUnknownAsRacemicParity();
					byte[] idcode = getBytes(canonizer.getIDCode());
					byte[] coords = getBytes(canonizer.getEncodedCoordinates());
					fieldData[0] = idcode;
					fieldData[1] = coords;

					if (mfParser.isChiralFlagSet() || mfParser.isV3000())
						chiralFlagOrV3000Found = true;
					if (!chiralFlagOrV3000Found && mol.getStereoCenterCount() != 0)
						stereoCentersFound = true;
					}
				}
			catch (Exception e) {
				mErrorCount++;
				}

			if (molname.length() != 0) {
				mMolnameFound = true;
				fieldData[2] = getBytes(molname);
				if (structureIDColumn != -1 && !molname.equals(removeTabs(sdParser.getFieldData(structureIDColumn - 3))))
					mMolnameIsDifferentFromFirstField = true;
				}

			for (int i=0; i<fieldNames.length; i++)
				fieldData[3+i] = getBytes(removeTabs(sdParser.getFieldData(i)));

			fieldDataList.add(fieldData);
			recordNo++;
			}

		return !assumeChiralTrue && !chiralFlagOrV3000Found && stereoCentersFound;
		}

	private boolean readRDFile() {
		mProgressController.startProgress("Examining Records...", 0, 0);

		RDFileParser rdParser = new RDFileParser(mDataReader);
		boolean isReactions = rdParser.isReactionNext();

		String[] fieldNames = rdParser.getFieldNames();
		int textFieldCount = fieldNames.length;
		int chemFieldCount = isReactions ? 7 : 3;
		int totalFieldCount = textFieldCount + chemFieldCount;

		mFieldNames = new String[totalFieldCount];
		String chemObjectName = isReactions ? "Reaction" : "Structure";
		int index = 0;
		mFieldNames[index++] = chemObjectName+" Name";   // use record no as default column
		mFieldNames[index++] = chemObjectName;
		mFieldNames[index++] = cColumnType2DCoordinates;

		if (isReactions) {
			mFieldNames[index++] = cColumnTypeReactionMapping;
			mFieldNames[index++] = "reactionFP";
			mFieldNames[index++] = "reactantFFP";
			mFieldNames[index++] = "productFFP";
			}

		for (String fieldName:fieldNames)
			mFieldNames[index++] = sIdentifierHandler.normalizeIdentifierName(fieldName);

		ArrayList<Object[]> fieldDataList = new ArrayList<>();

		mOldVersionIDCodeColumn = 0;
		mOldVersionCoordinateColumn = 1;
		mCoordsMayBe3D = !isReactions;

		int structureIDColumn = (!isReactions && textFieldCount != 0
				&& (fieldNames[0].equals("ID")
				|| fieldNames[0].equals("IDNUMBER")
				|| fieldNames[0].equals(sIdentifierHandler.getSubstanceIdentifierName())
				|| fieldNames[0].equals("code"))) ? chemFieldCount : -1;

		rdParser = new RDFileParser(mFile);
		int errors = 0;
		String name = null;
		boolean nameFound = false;

		mProgressController.startProgress("Processing Records...", 0, 0);

		while (rdParser.hasNext()) {
			if (mProgressController.threadMustDie())
				break;

			Object[] fieldData = new Object[mFieldNames.length];

			try {
				if (isReactions) {
					Reaction rxn = rdParser.getNextReaction();
					if (rxn != null) {
						name = rxn.getName();
						String[] encoded = ReactionEncoder.encode(rxn, false);
						fieldData[1] = getBytes(encoded[0]);
						fieldData[2] = getBytes(encoded[2]);    // coords
						fieldData[3] = getBytes(encoded[1]);    // mapping
						}
					}
				else {
					StereoMolecule mol = rdParser.getNextMolecule();
					name = mol.getName();
					if (mol.getAllAtoms() != 0) {
						mol.normalizeAmbiguousBonds();
						mol.canonizeCharge(true);
						Canonizer canonizer = new Canonizer(mol);
						canonizer.setSingleUnknownAsRacemicParity();
						byte[] idcode = getBytes(canonizer.getIDCode());
						byte[] coords = getBytes(canonizer.getEncodedCoordinates());
						fieldData[1] = idcode;
						fieldData[2] = coords;
						}
					}
				}
			catch (Exception e) {
				errors++;
				}

			if (name != null && name.length() != 0) {
				nameFound = true;
				fieldData[0] = getBytes(name);
				}

			for (int i=chemFieldCount; i<totalFieldCount; i++)
				fieldData[i] = getBytes(removeTabs(rdParser.getFieldData(mFieldNames[i])));

			fieldDataList.add(fieldData);
			}

		addColumnProperty(chemObjectName, cColumnPropertySpecialType, isReactions ? cColumnTypeRXNCode : cColumnTypeIDCode);
		addColumnProperty(cColumnType2DCoordinates, cColumnPropertySpecialType, cColumnType2DCoordinates);
		addColumnProperty(cColumnType2DCoordinates, cColumnPropertyParentColumn, chemObjectName);
		if (isReactions) {
			addColumnProperty(cColumnTypeReactionMapping, cColumnPropertySpecialType, cColumnTypeReactionMapping);
			addColumnProperty(cColumnTypeReactionMapping, cColumnPropertyParentColumn, chemObjectName);
			addColumnProperty("reactionFP", cColumnPropertySpecialType, DescriptorConstants.DESCRIPTOR_ReactionFP.shortName);
			addColumnProperty("reactionFP", cColumnPropertyParentColumn, chemObjectName);
			addColumnProperty("reactantFFP", cColumnPropertySpecialType, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
			addColumnProperty("reactantFFP", cColumnPropertyParentColumn, chemObjectName);
			addColumnProperty("reactantFFP", cColumnPropertyReactionPart, cReactionPartReactants);
			addColumnProperty("productFFP", cColumnPropertySpecialType, DescriptorConstants.DESCRIPTOR_FFP512.shortName);
			addColumnProperty("productFFP", cColumnPropertyParentColumn, chemObjectName);
			addColumnProperty("productFFP", cColumnPropertyReactionPart, cReactionPartProducts);
			}

		mFieldData = fieldDataList.toArray(new Object[0][]);

		if (structureIDColumn != -1) {
			addColumnProperty(chemObjectName, cColumnPropertyRelatedIdentifierColumn, mFieldNames[structureIDColumn]);
			}
		else if (nameFound) {
			addColumnProperty(chemObjectName, cColumnPropertyRelatedIdentifierColumn, mFieldNames[0]);
			}
		else {
			mFieldNames[0] = chemObjectName + " No";
			for (int row=0; row<mFieldData.length; row++)
				mFieldData[row][0] = (""+(row+1)).getBytes();
			}

		if (errors > 0) {
			final String message = ""+errors+" compound structures could not be generated because of molfile parsing errors.";
			showMessageOnEDT(message, "Import Errors", JOptionPane.WARNING_MESSAGE);
			}

		if (!isReactions) {
			handlePotentially3DCoordinates();
			addDefaultLookupColumnProperties();
			}

		return true;
		}

	private String removeTabs(String s) {
		return (s == null) ? null : s.trim().replace('\t', ' ');
		}

	private void handleMissingChiralFlag() {
		if (!askOnEDT("Erroneously, some programs store pure enantiomers as racemates when exporting to an SD-file.\n"
			+ "All molecules in this SD-file are marked as racemate and some molecules contain stereo centers.\n"
			+ "Do you want DataWarrior to interpret those stereo centers as absolute?", "Warning",
			JOptionPane.WARNING_MESSAGE))
			return;

		StereoMolecule mol = new StereoMolecule();
		for (Object[] row:mFieldData) {
			if (row[0] != null) {
				new IDCodeParser().parse(mol, (byte[])row[0], (byte[])row[1]);
				if (mol.getAllAtoms() != 0) {
					mol.ensureHelperArrays(Molecule.cHelperCIP);
					int scCount = 0;
					for (int atom=0; atom<mol.getAtoms(); atom++) {
						if (mol.getAtomESRType(atom) == Molecule.cESRTypeAnd) {
							mol.setAtomESR(atom, Molecule.cESRTypeAbs, -1);
							scCount++;
							}
						}
					if (scCount != 0) {
						Canonizer canonizer = new Canonizer(mol);
						row[0] = canonizer.getIDCode().getBytes();
						if (row[1] != null)
							row[1] = canonizer.getIDCode().getBytes();
						}
					}
				}
			}
		}

	private void showMessageOnEDT(String message, String title, int type) {
		if (SwingUtilities.isEventDispatchThread())
			JOptionPane.showMessageDialog(mParentFrame, message, title, type);
		else
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mParentFrame, message, title, type) );
		}

	private boolean askOnEDT(String message, String title, int type) {
		if (SwingUtilities.isEventDispatchThread())
			return JOptionPane.showConfirmDialog(mParentFrame, message, title, JOptionPane.YES_NO_OPTION, type) == JOptionPane.YES_OPTION;

		AtomicBoolean answer = new AtomicBoolean();
		try {
			SwingUtilities.invokeAndWait(() -> {
				answer.set(JOptionPane.showConfirmDialog(mParentFrame, message, title,	JOptionPane.YES_NO_OPTION, type) == JOptionPane.YES_OPTION);
				} );
			}
		catch (Exception e) {}
		return answer.get();
		}

	/**
	 * SD-Files or native DataWarrior files before version 2.7.0 may end up with
	 * 2D- and/or 3D-coordinates in one column (cColumnType2DCoordinates).
	 * If we have a mix of 2D and 3D, we need to add a new column and separate the data.
	 * If we have 3D only, we need to change column properties accordingly.
	 */
	private void handlePotentially3DCoordinates() {
		mOldVersionCoordinate3DColumn = -1;

		if (!mCoordsMayBe3D)
			return;

		mProgressController.startProgress("Checking for 3D-coordinates...", 0, (mFieldData.length > PROGRESS_LIMIT) ? mFieldData.length : 0);

		boolean found2D = false;
		boolean found3D = false;
		IDCodeParser parser = new IDCodeParser(false);
		for (int row=0; row<mFieldData.length; row++) {
			if (mFieldData.length > PROGRESS_LIMIT && row%PROGRESS_STEP == 0)
				mProgressController.updateProgress(row);

			byte[] idcode = (byte[])mFieldData[row][mOldVersionIDCodeColumn];
			byte[] coords = (byte[])mFieldData[row][mOldVersionCoordinateColumn];
			if (idcode != null && coords != null) {
				if (parser.coordinatesAre3D(idcode, coords))
					found3D = true;
				else
					found2D = true;

				if (found2D && found3D)
					break;
				}
			}

		if (!found3D)
			return;

		if (!found2D) {
			mFieldNames[mOldVersionCoordinateColumn] = cColumnType3DCoordinates;
			mColumnProperties.remove(cColumnType2DCoordinates+"\t"+cColumnPropertySpecialType);
			mColumnProperties.remove(cColumnType2DCoordinates+"\t"+cColumnPropertyParentColumn);
			addColumnProperty(cColumnType3DCoordinates, cColumnPropertySpecialType, cColumnType3DCoordinates);
			addColumnProperty(cColumnType3DCoordinates, cColumnPropertyParentColumn, "Structure");
			return;
			}

		mOldVersionCoordinate3DColumn = mFieldNames.length;
		mFieldNames = Arrays.copyOf(mFieldNames, mOldVersionCoordinate3DColumn+1);
		mFieldNames[mOldVersionCoordinate3DColumn] = cColumnType3DCoordinates;

		mProgressController.startProgress("Separating 2D- from 3D-coordinates...", 0, (mFieldData.length > PROGRESS_LIMIT) ? mFieldData.length : 0);

		for (int row=0; row<mFieldData.length; row++) {
			if (mFieldData.length > PROGRESS_LIMIT && row%PROGRESS_STEP == 0)
				mProgressController.updateProgress(row);

			mFieldData[row] = Arrays.copyOf(mFieldData[row], mOldVersionCoordinate3DColumn+1);
			byte[] idcode = (byte[])mFieldData[row][mOldVersionIDCodeColumn];
			byte[] coords = (byte[])mFieldData[row][mOldVersionCoordinateColumn];
			if (idcode != null && coords != null) {
				if (parser.coordinatesAre3D(idcode, coords)) {
					mFieldData[row][mOldVersionCoordinate3DColumn] = mFieldData[row][mOldVersionCoordinateColumn];
					mFieldData[row][mOldVersionCoordinateColumn] = null;
					}
				}
			}
		addColumnProperty(cColumnType3DCoordinates, cColumnPropertySpecialType, cColumnType3DCoordinates);
		addColumnProperty(cColumnType3DCoordinates, cColumnPropertyParentColumn, "Structure");
		}

	private int populateTable() {
		mTableModel.initializeTable(mFieldData.length, mFieldNames.length);

		if (mExtensionMap != null)
			for (String name:mExtensionMap.keySet())
				mTableModel.setExtensionData(name, mExtensionMap.get(name));

		for (int column=0; column<mFieldNames.length; column++)
			mTableModel.setColumnName(mFieldNames[column], column);

		int rowCount = mFieldData.length;

		mProgressController.startProgress("Populating Table...", 0, (rowCount > PROGRESS_LIMIT) ? rowCount : 0);

		for (int row=0; row<rowCount; row++) {
			if (mProgressController.threadMustDie())
				break;
			if (rowCount > PROGRESS_LIMIT && row%PROGRESS_STEP == 0)
				mProgressController.updateProgress(row);

			for (int column=0; column<mFieldNames.length; column++)
				mTableModel.setTotalDataAt(mFieldData[row][column], row, column);
			}

		setColumnProperties(null);

		clearBufferedData();

		if (mDataType == FileHelper.cFileTypeDataWarrior)
			mTableModel.setFile(mFile);

		return rowCount;
		}

	private byte[] convertNLAndTAB(byte[] cellBytes) {
		return replaceSpecialSigns(replaceSpecialSigns(cellBytes, NEWLINE_BYTES, NEWLINE), TAB_BYTES, TAB);
		}

	private byte[] convertDoubleQuotes(byte[] cellBytes) {
		return replaceSpecialSigns(replaceSpecialSigns(cellBytes, QUOTES1_BYTES, (byte)'\"'), QUOTES2_BYTES, (byte)'\"');
		}

	private byte[] replaceSpecialSigns(byte[] cellBytes, byte[] what, byte with) {
		int index = 0;
		for (int i=0; i<cellBytes.length; i++) {
			boolean found = false;
			if (i <= cellBytes.length-what.length) {
				found = true;
				for (int j=0; j<what.length; j++) {
					if (cellBytes[i+j] != what[j]) {
						found = false;
						break;
						}
					}
				}
			if (found) {
				cellBytes[index++] = with;
				i += what.length-1;
				}
			else {
				cellBytes[index++] = cellBytes[i];
				}
			}

		if (index == cellBytes.length)
			return cellBytes;

		byte[] newBytes = new byte[index];
		for (int i=0; i<index; i++)
			newBytes[i] = cellBytes[i];

		return newBytes;
		}

	public void run() {
		try {
			boolean error = false;
			if ((mAction & READ_DATA) != 0)
				error = !readData();
	
			if ((mAction & REPLACE_DATA) != 0 && !error && !mProgressController.threadMustDie())
				replaceTable();
	
			if ((mAction & APPEND_DATA) != 0 && !error && !mProgressController.threadMustDie())
				appendTable();
	
			if ((mAction & MERGE_DATA) != 0 && !error && !mProgressController.threadMustDie())
				error = mergeTable();
	
			if (mOwnsProgressController) {
				mProgressController.stopProgress();
				((JProgressDialog)mProgressController).close(mParentFrame);
				}

			if ((mAction & (REPLACE_DATA | APPEND_DATA | MERGE_DATA | APPLY_TEMPLATE)) != 0
			 && mRuntimeProperties != null
			 && !error)
				mRuntimeProperties.apply();

			if (!error && mSelectionData != null) {
				setListFlags(CompoundRecord.cFlagSelected, mSelectionData, mSelectionRowCount, mSelectionOffset, mSelectionDestRowMap);
				mTableModel.invalidateSelectionModel();
				}

			if (!error && mDataDependentPropertyReaderMap != null)
				for (DataDependentPropertyReader ddpr:mDataDependentPropertyReaderMap.values())
					ddpr.apply();

			finalStatus(!error);
			}
		catch (Throwable t) {
			t.printStackTrace();
			}

		mThread = null;
		}

	private boolean readData() {
			// returns true if successful
		clearBufferedData();

		try {
			switch (mDataType) {
			case FileHelper.cFileTypeDataWarriorTemplate:
				return readTemplateOnly();
			case FileHelper.cFileTypeDataWarrior:
			case FileHelper.cFileTypeTextTabDelimited:
			case FileHelper.cFileTypeTextCommaSeparated:
				return readTextData();
			case FileHelper.cFileTypeRD:
				return readRDFile();
			case FileHelper.cFileTypeSD:
				return readSDFile();
				}
			}
		catch (OutOfMemoryError err) {
			showMessageOnEDT("Out of memory. Launch this application with Java option -Xms???m or -Xmx???m.", "Memory Error", JOptionPane.WARNING_MESSAGE);
			clearBufferedData();
			return false;
			}
		return false;
		}

	private void replaceTable() {
		if (mDataType != FileHelper.cFileTypeDataWarriorTemplate) {
			mFirstNewColumn = 0;
			int rowCount = populateTable();

			if (mProgressController.threadMustDie()) {
				mTableModel.initializeTable(0, 0);
				if (mParentFrame != null)
					mParentFrame.setTitle("no data");
				}
			else {
				if (mParentFrame != null)
					mParentFrame.setTitle(mNewWindowTitle);

				mTableModel.finalizeTable(mRuntimeProperties != null
						   && mRuntimeProperties.size() != 0 ?
								   CompoundTableEvent.cSpecifierNoRuntimeProperties
								 : mIsGooglePatentsFile || rowCount > 10000 ?
								   CompoundTableEvent.cSpecifierDefaultFilters
								 : CompoundTableEvent.cSpecifierDefaultFiltersAndViews,
										  mProgressController);

				if (mDataType == FileHelper.cFileTypeDataWarrior)
					sIdentifierHandler.setIdentifierAliases(mTableModel);

				populateHitlists(rowCount, 0, null);
				populateDetails();
				}
			}
		}

	private void appendTable() {
		resolveDetailIDCollisions();

		adaptColumnPropertiesForAppendOrMerge(mAppendDestColumn);

		mFirstNewColumn = mTableModel.getTotalColumnCount();
		int newDatasetNameColumns = (mAppendDatasetColumn == NEW_COLUMN) ? 1 : 0;
		int newColumns = newDatasetNameColumns;
		for (int i=0; i<mAppendDestColumn.length; i++)
			if (mAppendDestColumn[i] == NEW_COLUMN)
				newColumns++;

		if (newColumns != 0) {
			String[] columnName = new String[newColumns];
			if (newDatasetNameColumns != 0)
				columnName[0] = DATASET_COLUMN_TITLE;
			newColumns = newDatasetNameColumns;
			for (int i=0; i<mAppendDestColumn.length; i++)
				if (mAppendDestColumn[i] == NEW_COLUMN)
					columnName[newColumns++] = mFieldNames[i];

			int destinationColumn = mTableModel.addNewColumns(columnName);
			
			if (newDatasetNameColumns != 0) {
				mAppendDatasetColumn = destinationColumn++;
				for (int row=0; row<mTableModel.getTotalRowCount(); row++)
					mTableModel.setTotalValueAt(mAppendDatasetNameExisting, row, mAppendDatasetColumn);
				}

			for (int i=0; i<mAppendDestColumn.length; i++)
				if (mAppendDestColumn[i] == NEW_COLUMN)
					mAppendDestColumn[i] = destinationColumn++;
			}

		setColumnProperties(mAppendDestColumn);

		if (newColumns != 0)
			mTableModel.finalizeNewColumns(mFirstNewColumn, mProgressController);

		if (mRuntimeProperties != null) // do this after finalizeNewColumns()
			mRuntimeProperties.learn(); // to also copy the new dataset filter

		int existingRowCount = mTableModel.getTotalRowCount();
		int additionalRowCount = mFieldData.length;
		mTableModel.addNewRows(additionalRowCount, true);

		mProgressController.startProgress("Appending rows...", 0, additionalRowCount);

		for (int row=0; row<additionalRowCount; row++) {
			int newRow = existingRowCount + row;

			if (mAppendDatasetColumn != NO_COLUMN)
				mTableModel.setTotalValueAt(mAppendDatasetNameNew, newRow, mAppendDatasetColumn);
			for (int column=0; column<mFieldNames.length; column++)
				if (mAppendDestColumn[column] != NO_COLUMN)
					mTableModel.setTotalDataAt(mFieldData[row][column], newRow, mAppendDestColumn[column]);

			mProgressController.updateProgress(row);
			}

		clearBufferedData();

		mTableModel.finalizeNewRows(existingRowCount, mProgressController);

		populateHitlists(additionalRowCount, existingRowCount, null);
		populateDetails();
		}

	private void addSourceToDestRowEntry(int sourceRow, int destRow, TreeMap<Integer, int[]> sourceToDestRowMap) {
		int[] destRowList = sourceToDestRowMap.get(sourceRow);
		if (destRowList == null) {
			destRowList = new int[1];
			destRowList[0] = destRow;
			}
		else {
			int[] newDestRowList = new int[destRowList.length+1];
			for (int i=0; i<destRowList.length; i++)
				newDestRowList[i] = destRowList[i];
			newDestRowList[destRowList.length] = destRow;
			destRowList = newDestRowList;
			}
		sourceToDestRowMap.put(sourceRow, destRowList);
		}

	private boolean mergeTable() {
		mProgressController.startProgress("Preparing merge...", 0, 0);

		// construct key column array from mMergeMode
		int keyColumns = 0;
		int wordSearchIndex = -1;
		for (int sourceColumn=0; sourceColumn<mMergeMode.length; sourceColumn++) {
			if (mMergeMode[sourceColumn] == MERGE_MODE_IS_KEY
			 || mMergeMode[sourceColumn] == MERGE_MODE_IS_KEY_NO_CASE
			 || mMergeMode[sourceColumn] == MERGE_MODE_IS_KEY_WORD_SEARCH) {
				if (mMergeMode[sourceColumn] == MERGE_MODE_IS_KEY_WORD_SEARCH)
					wordSearchIndex = keyColumns;
				keyColumns++;
				}
			}

		int[] keyColumn = null;
		boolean[] isIgnoreCase = null;
		keyColumn = new int[keyColumns];
		isIgnoreCase = new boolean[keyColumns];
		keyColumns = 0;
		for (int sourceColumn=0; sourceColumn<mMergeMode.length; sourceColumn++) {
			if (mMergeMode[sourceColumn] == MERGE_MODE_IS_KEY
			 || mMergeMode[sourceColumn] == MERGE_MODE_IS_KEY_NO_CASE
			 || mMergeMode[sourceColumn] == MERGE_MODE_IS_KEY_WORD_SEARCH) {
				keyColumn[keyColumns] = sourceColumn;
				isIgnoreCase[keyColumns] = (mMergeMode[sourceColumn] != MERGE_MODE_IS_KEY);
				keyColumns++;
				}
			}

		TreeMap<byte[][],Integer> keyToSourceRowMap = new TreeMap<>(new ByteArrayArrayComparator());
		for (int row=0; row<mFieldData.length; row++) {
			byte[][] key = new byte[keyColumns][];
			for (int i=0; i<keyColumns; i++) {
				if (i == wordSearchIndex && mFieldData[row][keyColumn[i]] != null) {
					key[i] = new String((byte[])mFieldData[row][keyColumn[i]]).trim().toLowerCase().getBytes();
					}
				else if (isIgnoreCase[i] && mFieldData[row][keyColumn[i]] != null) {
					key[i] = new String((byte[])mFieldData[row][keyColumn[i]]).toLowerCase().getBytes();
					}
				else {
					key[i] = (byte[])mFieldData[row][keyColumn[i]];
					}
				}
			keyToSourceRowMap.put(key, row);
			}

		int maxWordCount = (wordSearchIndex == -1) ? -1 : determineMaxWordCount(keyToSourceRowMap.keySet());

		TreeMap<Integer, int[]> sourceToDestRowMap = new TreeMap<>();

		mProgressController.startProgress("Assigning new rows to current rows...", 0, mTableModel.getTotalRowCount());

		for (int destRow=0; destRow<mTableModel.getTotalRowCount(); destRow++) {
			if (mProgressController.threadMustDie())
				break;
			if (destRow % PROGRESS_STEP == 0)
				mProgressController.updateProgress(destRow);

			// create combined key array from all key columns and find
			byte[][] key = new byte[keyColumns][];
			for (int i=0; i<keyColumns; i++) {
				key[i] = (byte[])mTableModel.getTotalRecord(destRow).getData(mMergeDestColumn[keyColumn[i]]);
				if (key[i] != null && isIgnoreCase[i])
					key[i] = new String(key[i]).toLowerCase().getBytes();
				}

			if (wordSearchIndex == -1 || key[wordSearchIndex] == null) {
				Integer sourceRow = keyToSourceRowMap.get(key);
				if (sourceRow != null)
					addSourceToDestRowEntry(sourceRow, destRow, sourceToDestRowMap);
				}
			else {  // one of the key columns requires a sub-word search
				byte[] targetCellText = key[wordSearchIndex];
				int spaceCount = 0;
				for (int i=0; i<targetCellText.length; i++)
					if (targetCellText[i] == 32)
						spaceCount++;
				int[] wordIndex = new int[spaceCount+2];
				spaceCount = 0;
				for (int i=0; i<targetCellText.length; i++)
					if (targetCellText[i] == 32)
						wordIndex[++spaceCount] = i+1;
				wordIndex[++spaceCount] = targetCellText.length+1;

				boolean found = false;
				for (int i1=0; !found && i1<wordIndex.length-1; i1++) {
					if (wordIndex[i1+1]>wordIndex[i1]+1) {
						for (int i2=Math.min(i1+maxWordCount, wordIndex.length-1); !found && i2>i1; i2--) {
							if (wordIndex[i2-1]<wordIndex[i2]-1) {
								key[wordSearchIndex] = Arrays.copyOfRange(targetCellText, wordIndex[i1], wordIndex[i2] - 1);
								Integer sourceRow = keyToSourceRowMap.get(key);
								if (sourceRow != null) {
									addSourceToDestRowEntry(sourceRow, destRow, sourceToDestRowMap);
									found = true;
									}
								}
							}
						}
					}
				}
			}

		if (mProgressController.threadMustDie()) {
			clearBufferedData();
			return true;
			}

		resolveDetailIDCollisions();
		adaptColumnPropertiesForAppendOrMerge(mMergeDestColumn);

		if (mProgressController.threadMustDie()) {
			clearBufferedData();
			return true;
			}

		int[][] destRowMap = null;
		if (mHitlists != null)
			destRowMap = new int[mFieldData.length][];

		if (mRuntimeProperties != null)
			mRuntimeProperties.learn();

		int newColumns = 0;
		for (int i=0; i<mMergeDestColumn.length; i++) {
			if (mMergeMode[i] == MERGE_MODE_IS_KEY_WORD_SEARCH) {
				mMergeMode[i] = MERGE_MODE_REPLACE;
				mMergeDestColumn[i] = NEW_COLUMN;
				}
			if (mMergeDestColumn[i] == NEW_COLUMN)
				newColumns++;
			}

		mFirstNewColumn = mTableModel.getTotalColumnCount();
		if (newColumns != 0) {
			mProgressController.startProgress("Merging data...", 0, mFieldData.length);

			String[] columnName = new String[newColumns];
			newColumns = 0;
			for (int i=0; i<mMergeDestColumn.length; i++)
				if (mMergeDestColumn[i] == NEW_COLUMN)
					columnName[newColumns++] = mFieldNames[i];

			int destinationColumn = mTableModel.addNewColumns(columnName);
			for (int i=0; i<mMergeDestColumn.length; i++) {
				if (mMergeDestColumn[i] == NEW_COLUMN) {
					mMergeDestColumn[i] = destinationColumn++;
					mMergeMode[i] = MERGE_MODE_REPLACE;
					}
				}
			}

		int mergedColumns = 0;
		for (int sourceRow=0; sourceRow<mFieldData.length; sourceRow++) {
			if (mProgressController.threadMustDie())
				break;
			if (sourceRow % PROGRESS_STEP == 0)
				mProgressController.updateProgress(sourceRow);

			int[] rowList = sourceToDestRowMap.get(sourceRow);
			if (rowList != null) {
				for (int destRow:rowList) {
					// In case we have child columns with merge mode MERGE_MODE_AS_PARENT, we need to handle them first.
					for (int column=0; column<mMergeDestColumn.length; column++) {
						if (mMergeDestColumn[column] != NO_COLUMN) {
							if (mMergeMode[column] == MERGE_MODE_AS_PARENT) {
								int parentColumn = getSourceColumn(getParentColumnName(mFieldNames[column]));
								if (mTableModel.getTotalRecord(destRow).getData(mMergeDestColumn[parentColumn]) == null)
									mTableModel.setTotalDataAt(mFieldData[sourceRow][column], destRow, mMergeDestColumn[column]);
								}
							}
						}
					for (int column=0; column<mMergeDestColumn.length; column++) {
						if (mMergeDestColumn[column] != NO_COLUMN) {
							switch (mMergeMode[column]) {
							case MERGE_MODE_APPEND:
								mTableModel.appendTotalDataAt((byte[])mFieldData[sourceRow][column], destRow, mMergeDestColumn[column]);
								break;
							case MERGE_MODE_REPLACE:
								mTableModel.setTotalDataAt(mFieldData[sourceRow][column], destRow, mMergeDestColumn[column]);
								break;
							case MERGE_MODE_USE_IF_EMPTY:
								if (mTableModel.getTotalRecord(destRow).getData(mMergeDestColumn[column]) == null)
									mTableModel.setTotalDataAt(mFieldData[sourceRow][column], destRow, mMergeDestColumn[column]);
								break;
							default:	// merge key don't require handling and child column merging was handled before
								break;
								}
							}
						}
					}

				if (destRowMap != null)
					destRowMap[sourceRow] = rowList;

				mFieldData[sourceRow] = null;
				mergedColumns++;
				}
			}

		setColumnProperties(mMergeDestColumn);

		if (newColumns != 0)
			mTableModel.finalizeNewColumns(mFirstNewColumn, mProgressController);

		if (mProgressController.threadMustDie()) {
			clearBufferedData();
			return true;
			}

		final int existingRowCount = mTableModel.getTotalRowCount();
		final int additionalRowCount = mFieldData.length - mergedColumns;
		if (mAppendRest && additionalRowCount > 0) {

			mTableModel.addNewRows(additionalRowCount, true);

			int destRow = existingRowCount;

			mProgressController.startProgress("Appending remaining...", 0, additionalRowCount);

			for (int row=0; row<mFieldData.length; row++) {
				if (mFieldData[row] == null)
					continue;

				if (mProgressController.threadMustDie())
					break;

				for (int column=0; column<mMergeDestColumn.length; column++)
					if (mMergeDestColumn[column] != NO_COLUMN)
						mTableModel.setTotalDataAt(mFieldData[row][column], destRow, mMergeDestColumn[column]);

				if (destRowMap != null) {
					destRowMap[row] = new int[1];
					destRowMap[row][0] = destRow;
					}

				mProgressController.updateProgress(destRow - existingRowCount);

				destRow++;
				}
			}

		clearBufferedData();

		if (mAppendRest && additionalRowCount > 0)
			mTableModel.finalizeNewRows(existingRowCount, mProgressController);

		if (destRowMap != null)
			populateHitlists(destRowMap.length, -1, destRowMap);

		populateDetails();

		return false;
		}

	private int determineMaxWordCount(Set<byte[][]> keySet) {
		int maxCount = 0;
		for (byte[][] key:keySet) {
			String s = new String(key[0]);
			int count = (s.length() == 0) ? 0 : 1;
			for (int i=0; i<s.length(); i++)
				if (s.charAt(i) == ' ')
					count++;
			if (maxCount < count)
				maxCount = count;
			}
		return maxCount;
		}

	/**
	 * If we merge or append, for merged columns we cannot just add any new column properties
	 * to the existing column. Redundant and incompatible column properties are identified
	 * and removed from the column property list by this method.
	 * If matched source and destination columns contain non-matching details references,
	 * then the second file's detail reference indexes are translated and all references in
	 * the merged or appended file adapted.
	 * @param appendOrMergeDestColumn source to destination column mapping with NEW_COLUMN unresolved!!!
	 * @return
	 */
	private void adaptColumnPropertiesForAppendOrMerge(int[] appendOrMergeDestColumn) {
		if (mColumnProperties == null)
			return;

		for (int srcColumn=0; srcColumn<appendOrMergeDestColumn.length; srcColumn++) {
			int destColumn = appendOrMergeDestColumn[srcColumn];
			if (destColumn >= 0) {
				String value = mTableModel.getColumnProperty(destColumn, cColumnPropertyDetailCount);
				if (value != null) {
					int destCount = Integer.parseInt(value);
					value = mColumnProperties.get(mFieldNames[srcColumn]+"\t"+cColumnPropertyDetailCount);
					if (value != null) {
						int srcCount = Integer.parseInt(value);
						int[] newDetailIndex = new int[srcCount];
						for (int srcIndex=0; srcIndex<srcCount; srcIndex++) {
							value = mColumnProperties.get(mFieldNames[srcColumn]+"\t"+cColumnPropertyDetailName+srcIndex);
							newDetailIndex[srcIndex] = mTableModel.getColumnPropertyIndex(destColumn, cColumnPropertyDetailName, value);
							}

						// keep original source indexes if they are larger than highest dest index and detail name doesn't match a dest detail name
						for (int srcIndex=destCount; srcIndex<srcCount; srcIndex++)
							if (newDetailIndex[srcIndex] == -1)
								newDetailIndex[srcIndex] = srcIndex;

						int potentiallyFreeIndex = destCount;
						for (int srcIndex=0; srcIndex<Math.min(srcCount, destCount); srcIndex++) {
							while (newDetailIndex[srcIndex] == -1) {
								boolean isFree = true;
								for (int i=0; i<srcCount; i++) {
									if (newDetailIndex[i] == potentiallyFreeIndex) {
										isFree = false;
										break;
										}
									}
								if (isFree)
									newDetailIndex[srcIndex] = potentiallyFreeIndex;

								potentiallyFreeIndex++;
								}
							}

						int removalCount = 0;
						for (int srcIndex=0; srcIndex<srcCount; srcIndex++) {
							if (newDetailIndex[srcIndex] < destCount) {
								mColumnProperties.remove(mFieldNames[srcColumn]+"\t"+cColumnPropertyDetailName+srcIndex);
								mColumnProperties.remove(mFieldNames[srcColumn]+"\t"+cColumnPropertyDetailType+srcIndex);
								mColumnProperties.remove(mFieldNames[srcColumn]+"\t"+cColumnPropertyDetailSource+srcIndex);
								removalCount++;
								}
							}
						for (int srcIndex=srcCount-1; srcIndex>=0; srcIndex--) {	// backward srcIndex is always smaller than destIndex
							if (newDetailIndex[srcIndex] != srcIndex) {
								replacePropertyIndex(mFieldNames[srcColumn]+"\t"+cColumnPropertyDetailName, srcIndex, newDetailIndex[srcIndex]);
								replacePropertyIndex(mFieldNames[srcColumn]+"\t"+cColumnPropertyDetailType, srcIndex, newDetailIndex[srcIndex]);
								replacePropertyIndex(mFieldNames[srcColumn]+"\t"+cColumnPropertyDetailSource, srcIndex, newDetailIndex[srcIndex]);

								updateDetailIndex(srcColumn, srcIndex, newDetailIndex[srcIndex]);
								}
							}

						if (removalCount != srcCount) {
							addColumnProperty(mFieldNames[srcColumn], cColumnPropertyDetailCount, Integer.toString(srcCount+destCount-removalCount));
							}
						}
					}
				}
			}
		}

	private void replacePropertyIndex(String key, int oldIndex, int newIndex) {
		String value = mColumnProperties.get(key+oldIndex);
		if (value != null) {
			mColumnProperties.remove(key+oldIndex);
			mColumnProperties.put(key+newIndex, value);
			}
		}

	private void updateDetailIndex(int column, int oldIndex, int newIndex) {
		String separator = mColumnProperties.get(mFieldNames[column]+"\t"+cColumnPropertyDetailSeparator);
		if (separator == null)
			separator = CompoundTableModel.cDefaultDetailSeparator;

		String oldRef = separator.concat(Integer.toString(oldIndex)).concat(":");
		String newRef = separator.concat(Integer.toString(newIndex)).concat(":");

		if (oldRef.length() == newRef.length()) {	// if we have the same length, we just overwrite in place
			byte[] oldRefBytes = oldRef.getBytes();
			byte[] newRefBytes = newRef.getBytes();

			for (int row=0; row<mFieldData.length; row++) {
				byte[] bytes = (byte[])mFieldData[row][column];
				for (int i=0; i<bytes.length-oldRefBytes.length; i++) {
					boolean found = true;
					for (int j=0; j<oldRefBytes.length; j++) {
						if (bytes[i+j] != oldRefBytes[j]) {
							found = false;
							break;
							}
						}
					if (found) {
						for (int j=0; j<newRefBytes.length; j++)
							bytes[i+j] = newRefBytes[j];
						i += oldRefBytes.length -1;
						}
					}
				}
			}
		else { // otherwise do it the expensive way
			for (int row=0; row<mFieldData.length; row++) {
				byte[] bytes = (byte[])mFieldData[row][column];
				if (bytes != null) {
					String value = new String(bytes).replace(oldRef, newRef);
					mFieldData[row][column] = value.getBytes();
					}
				}
			}
		}

	/**
	 * If we append or merge we need to translate column indexes from source to destination tables.
	 * If we append or merge for existing columns we add compatible column properties only.
	 * @param appendOrMergeDestColumn null or source to destination column mapping with NEW_COLUMN already resolved!!!
	 */
	private void setColumnProperties(int[] appendOrMergeDestColumn) {
		if (mColumnProperties == null)
			return;

		for (String columnAndKey:mColumnProperties.keySet()) {
			int index = columnAndKey.indexOf('\t');
			String columnName = columnAndKey.substring(0, index);

			int column = getSourceColumn(columnName);

			if (column != NO_COLUMN) {
				if (appendOrMergeDestColumn != null)
					column = appendOrMergeDestColumn[column];

				if (column != NO_COLUMN) {
// We now overwrite properties when merging/appending after having called adaptColumnPropertiesForAppendOrMerge()
//					if (column >= mFirstNewColumn) {
						String key = columnAndKey.substring(index+1);
						String value = mColumnProperties.get(columnAndKey);

							// in case of merge/append column property references
							// to parent columns may need to be translated
						if (appendOrMergeDestColumn != null) {
							if (key.equals(cColumnPropertyParentColumn)) {
								int parentColumn = appendOrMergeDestColumn[getSourceColumn(value)];
								if (parentColumn == NO_COLUMN)	// visible columns that have a parent (e.g. cluster no)
									value = null;
								else
									value = mTableModel.getColumnTitleNoAlias(parentColumn);
								}
							}

						mTableModel.setColumnProperty(column, key, value);
						}
//					}
				}
			}
		}

	private int getSourceColumn(String columnName) {
		for (int j=0; j<mFieldNames.length; j++)
			if (columnName.equals(mFieldNames[j]))
				return j;
		return NO_COLUMN;
		}

	private void populateHitlists(int rowCount, int offset, int[][] destRowMap) {
			// use either offset or destRowMap to indicate mapping of original hitlists to current rows
		if (mHitlists != null) {
			CompoundTableListHandler hitlistHandler = mTableModel.getListHandler();
			for (int list=0; list<mHitlists.size(); list++) {
				String listString = mHitlists.get(list);
				int index = listString.indexOf('\t');
				String name = listString.substring(0, index);
				byte[] data = new byte[listString.length()-index-1];
				for (int i=0; i<data.length; i++)
					data[i] = (byte)(listString.charAt(++index) - 64);

				boolean isSelection = CompoundTableListHandler.LIST_CODE_SELECTION.equals(name);

				if (isSelection) {
					mSelectionRowCount = rowCount;
					mSelectionOffset = offset;
					mSelectionDestRowMap = destRowMap;
					mSelectionData = data;
					}
				else {
					String uniqueName = hitlistHandler.createList(name, -1, CompoundTableListHandler.EMPTY_LIST, -1, null, false);
					int flagNo = hitlistHandler.getListFlagNo(uniqueName);
					setListFlags(flagNo, data, rowCount, offset, destRowMap);
					}
				}
			}
		}

	public void setAssumeChiralFlag(boolean b) {
		mAssumeChiralFlag = b;
		}

	private void setListFlags(int flagNo, byte[] data, int rowCount, int offset, int[][] destRowMap) {
		int dataBit = 1;
		int dataIndex = 0;
		for (int row = 0; row < rowCount; row++) {
			if ((data[dataIndex] & dataBit) != 0) {
				if (destRowMap == null)
					mTableModel.getTotalRecord(row + offset).setFlag(flagNo);
				else if (destRowMap[row] != null)
					for (int destRow : destRowMap[row])
						mTableModel.getTotalRecord(destRow).setFlag(flagNo);
			}
			dataBit *= 2;
			if (dataBit == 64) {
				dataBit = 1;
				dataIndex++;
			}
		}
		}

	private void populateDetails() {
		CompoundTableDetailHandler detailHandler = mTableModel.getDetailHandler();
		if (detailHandler != null && mDetails != null) {
			HashMap<String,byte[]> existingDetails = detailHandler.getEmbeddedDetailMap();
			if (existingDetails != null)
				mDetails.putAll(existingDetails);
			detailHandler.setEmbeddedDetailMap(mDetails);
			}
		}

	private void resolveDetailIDCollisions() {
		if (mDetails != null && mTableModel.getDetailHandler().getEmbeddedDetailCount() != 0) {
						// Existing data as well a new data have embedded details.
						// Adding an offset to the IDs of existing details ensures collision-free merging/appending.
			int highID = 0;
			Iterator<String> iterator = mDetails.keySet().iterator();
			while (iterator.hasNext()) {
				try {
					int id = Math.abs(Integer.parseInt(iterator.next()));
					if (highID < id)
						highID = id;
					}
				catch (NumberFormatException nfe) {}
				}

			if (highID != 0)
				mTableModel.addOffsetToEmbeddedDetailIDs(highID);
			}
		}

	private void clearBufferedData() {
		mFieldNames = null;
		mFieldData = null;
		mColumnProperties = null;
		mAppendDestColumn = null;
		mMergeDestColumn = null;
		}

	/**
	 * This function serves as a callback function to report the success when the loader thread is done.
	 * Overwrite this, if you need the status after loading.
	 * @param success true if file was successfully read
	 */
	public void finalStatus(boolean success) {}
	}
