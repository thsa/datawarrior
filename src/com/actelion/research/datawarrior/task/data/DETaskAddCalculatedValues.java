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

package com.actelion.research.datawarrior.task.data;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.datawarrior.task.jep.*;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableListHandler;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;
import org.nfunk.jep.JEP;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class DETaskAddCalculatedValues extends ConfigurableTask implements ActionListener {
	static final long serialVersionUID = 0x20061004;

	public static final String TASK_NAME = "Add Calculated Values";

	private static final String PROPERTY_FORMULA = "formula";
	private static final String PROPERTY_OVERWRITE_COLUMN = "isOverwrite";
	private static final String PROPERTY_COLUMN_NAME = "columnName";

	private static final String PSEUDO_FUNCTION_ASK_COLUMN_VAR = "askColumnVar(";
	private static final String PSEUDO_FUNCTION_ASK_COLUMN_TITLE = "askColumnTitle(";
	private static final String PSEUDO_FUNCTION_ASK_STRING = "askString(";
	private static final String PSEUDO_FUNCTION_ASK_NUMBER = "askNumber(";

	private static final String PREPROCESS_FUNCTION_TOTAL_VALUE_COUNT = "valueCount";
	private static final String PREPROCESS_FUNCTION_NON_EMPTY_VALUE_COUNT = "nonEmptyValueCount";
	private static final String PREPROCESS_FUNCTION_STDDEV = "valueStdDev";

	private static final String IS_VISIBLE_ROW = "isVisibleRow";
	private static final String IS_SELECTED_ROW = "isSelectedRow";
	private static final String IS_MEMBER_OF = "isMemberOf_";

	private volatile CompoundTableModel	mTableModel;
	private volatile String		mResolvedValue;
	private volatile String		mFormula;
	private volatile int		mColumn;
	private boolean mAllowEditing;
	private volatile JEP mParser;    // the parser used for function name checking and for final execution
	private int					mCurrentRow;
	private JPanel				mDialogPanel;
	private JComboBox			mComboBoxVariableName,mComboBoxColumnName;
	private JCheckBox			mCheckBoxOverwriteExisting;
	private JTextArea		    mTextAreaFormula;
	private JTextField          mTextFieldColumnName;
	private JLabel				mLabelColumnName;
	private volatile TreeMap<String,Integer> mRunTimeColumnMap;
	private volatile ArrayList<String> mPreprocessVariables;

	public DETaskAddCalculatedValues(Frame parent, CompoundTableModel tableModel, int column, String formula, boolean allowEditing) {
		super(parent, true);
		mTableModel = tableModel;
		mFormula = formula;
		mColumn = column;
		mParser = createParser();
		mAllowEditing = allowEditing;
		}

	private JEP createParser() {
		JEP parser = new JEP();
		parser.addStandardFunctions();
		parser.addStandardConstants();
		parser.addFunction(JEPChemSimilarityFunction.FUNCTION_NAME, new JEPChemSimilarityFunction(mTableModel));
		parser.addFunction(JEPChemSSSFunction.FUNCTION_NAME, new JEPChemSSSFunction(mTableModel));
		parser.addFunction("row", new JEPRowFunction(this));
		parser.addFunction("numvalue", new JEPNumValueFunction(this, mTableModel));
		parser.addFunction("entry", new JEPEntryFunction());
		parser.addFunction("numcellvalue", new JEPNumCellEntryFunction(this, mTableModel));
		parser.addFunction("indexof", new JEPIndexOfFunction());
		parser.addFunction("substring", new JEPSubstringFunction());
		parser.addFunction("round", new JEPRoundFunction());
		parser.addFunction("int", new JEPIntFunction());
		parser.addFunction("len", new JEPLenFunction());
		parser.addFunction("max", new JEPMaxFunction());
		parser.addFunction("min", new JEPMinFunction());
		parser.addFunction("str", new JEPMyStrFunction());
		parser.addFunction("isempty", new JEPEmptyFunction());
		parser.addFunction("replaceempty", new JEPReplaceEmptyFunction());
		parser.addFunction("replace", new JEPReplaceFunction());
		parser.addFunction("contains", new JEPContainsFunction());
		parser.addFunction("matchregex", new JEPMatchesRegexFunction());
		parser.addFunction("ligeff1", new JEPRateHTSFunction(mTableModel));
		parser.addFunction("ligeff2", new JEPLigEffFunction(mTableModel));
		parser.addFunction("normalize", new JEPNormalizeFunction(mTableModel, this));
		parser.addFunction("maxsim", new JEPMaxChemSimilarityFunction(mTableModel));
		parser.addFunction("increase", new JEPOrderDependentFunction(mTableModel, JEPOrderDependentFunction.TYPE_INCREASE, this));
		parser.addFunction("increaseInCategory", new JEPOrderDependentInCategoryFunction(mTableModel, JEPOrderDependentInCategoryFunction.TYPE_INCREASE, this));
		parser.addFunction("percentIncrease", new JEPOrderDependentFunction(mTableModel, JEPOrderDependentFunction.TYPE_PERCENT_INCREASE, this));
		parser.addFunction("percentIncreaseInCategory", new JEPOrderDependentInCategoryFunction(mTableModel, JEPOrderDependentInCategoryFunction.TYPE_PERCENT_INCREASE, this));
		parser.addFunction("cumulativeSum", new JEPOrderDependentFunction(mTableModel, JEPOrderDependentFunction.TYPE_CUMULATIVE_SUM, this));
		parser.addFunction("cumulativeSumInCategory", new JEPOrderDependentInCategoryFunction(mTableModel, JEPOrderDependentInCategoryFunction.TYPE_CUMULATIVE_SUM, this));
		parser.addFunction("frequency", new JEPFrequencyFunction(mTableModel));
		parser.addFunction("frequencyInCategory", new JEPFrequencyInCategoryFunction(mTableModel, this));
		parser.addFunction("categoryFirst", new JEPValueInCategoryFunction(mTableModel, JEPValueInCategoryFunction.TYPE_FIRST, this));
		parser.addFunction("categoryLast", new JEPValueInCategoryFunction(mTableModel, JEPValueInCategoryFunction.TYPE_LAST, this));
		parser.addFunction("categoryMin", new JEPValueInCategoryFunction(mTableModel, JEPValueInCategoryFunction.TYPE_MIN, this));
		parser.addFunction("categoryMax", new JEPValueInCategoryFunction(mTableModel, JEPValueInCategoryFunction.TYPE_MAX, this));
		parser.addFunction("categorySum", new JEPValueInCategoryFunction(mTableModel, JEPValueInCategoryFunction.TYPE_SUM, this));
		parser.addFunction("categoryMean", new JEPValueInCategoryFunction(mTableModel, JEPValueInCategoryFunction.TYPE_MEAN, this));
		parser.addFunction("categoryMedian", new JEPValueInCategoryFunction(mTableModel, JEPValueInCategoryFunction.TYPE_MEDIAN, this));
		parser.addFunction("previousInCategory", new JEPPreviousInCategoryFunction(mTableModel, this));
		parser.addFunction("movingAverageInCategory", new JEPMovingInCategoryFunction(mTableModel, this, JEPMovingInCategoryFunction.MOVING_AVERAGE, true));
		parser.addFunction("movingSumInCategory", new JEPMovingInCategoryFunction(mTableModel, this, JEPMovingInCategoryFunction.MOVING_SUM, false));
		parser.addFunction("refvalue", new JEPRefValueOfCategoryFunction(this, mTableModel));
		parser.addFunction("year", new JEPValueOfDateFunction(Calendar.YEAR, -1));  // no correction
		parser.addFunction("yearISO", new JEPValueOfDateFunction(Calendar.YEAR, 0));
		parser.addFunction("yearEU", new JEPValueOfDateFunction(Calendar.YEAR, 1));
		parser.addFunction("yearUS", new JEPValueOfDateFunction(Calendar.YEAR, 2));
		parser.addFunction("yearME", new JEPValueOfDateFunction(Calendar.YEAR, 3));
		parser.addFunction("month", new JEPValueOfDateFunction(Calendar.MONTH, 0));
		parser.addFunction("weekISO", new JEPValueOfDateFunction(Calendar.WEEK_OF_YEAR, 0));
		parser.addFunction("weekEU", new JEPValueOfDateFunction(Calendar.WEEK_OF_YEAR, 1));
		parser.addFunction("weekUS", new JEPValueOfDateFunction(Calendar.WEEK_OF_YEAR, 2));
		parser.addFunction("weekME", new JEPValueOfDateFunction(Calendar.WEEK_OF_YEAR, 3));
		parser.addFunction("dayOfWeekEU", new JEPValueOfDateFunction(Calendar.DAY_OF_WEEK, -1));
		parser.addFunction("dayOfWeekUS", new JEPValueOfDateFunction(Calendar.DAY_OF_WEEK, 0));
		parser.addFunction("dayOfWeekME", new JEPValueOfDateFunction(Calendar.DAY_OF_WEEK, 1));
		parser.addFunction("dayOfMonth", new JEPValueOfDateFunction(Calendar.DAY_OF_MONTH, 0));
		parser.addFunction("dayOfYear", new JEPValueOfDateFunction(Calendar.DAY_OF_YEAR, 0));
		parser.addFunction("today", new JEPTodayFunction());
		return parser;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		if (mAllowEditing == true)
			return null;

		Properties configuration = new Properties();
		configuration.put(PROPERTY_FORMULA, mFormula.replace("\n", "<NL>"));
		configuration.put(PROPERTY_OVERWRITE_COLUMN, "true");
		configuration.put(PROPERTY_COLUMN_NAME, mTableModel.getColumnTitleNoAlias(mColumn));
		return configuration;
		}

	@Override
	public JComponent createDialogContent() {
		mDialogPanel = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.FILL, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.FILL, gap},
							{gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, 3*gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap} };
		mDialogPanel.setLayout(new TableLayout(size));

		mDialogPanel.add(new JLabel("Please enter a formula:"), "1,1,7,1");

		mTextAreaFormula = new JTextArea(6,32);
		mTextAreaFormula.setLineWrap(true);
		mTextAreaFormula.setWrapStyleWord(true);
		mDialogPanel.add(mTextAreaFormula, "1,3,7,3");
		
		JButton buttonAdd = new JButton("Add Variable");
		buttonAdd.addActionListener(this);
		mDialogPanel.add(buttonAdd, "3,5");

		TreeMap<String,Integer> columnMap = createColumnMap();

		mComboBoxVariableName = new JComboBox();
		for (String varName:columnMap.keySet())
			if (!mTableModel.isDescriptorColumn(columnMap.get(varName).intValue()))
				mComboBoxVariableName.addItem(varName);
		for (String varName:columnMap.keySet())
			if (mTableModel.isDescriptorColumn(columnMap.get(varName).intValue()))
				mComboBoxVariableName.addItem(varName);

		mComboBoxVariableName.addItem(IS_VISIBLE_ROW);
		mComboBoxVariableName.addItem(IS_SELECTED_ROW);

		mDialogPanel.add(mComboBoxVariableName, "5,5");

		mCheckBoxOverwriteExisting = new JCheckBox("Overwrite existing column");
		mCheckBoxOverwriteExisting.addActionListener(this);
		mDialogPanel.add(mCheckBoxOverwriteExisting, "1,7,7,7");

		mLabelColumnName = new JLabel("New column name:", JLabel.RIGHT);
		mDialogPanel.add(mLabelColumnName, "1,9,3,9");

		mComboBoxColumnName = new JComboBox();
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.getColumnSpecialType(column) == null)
				mComboBoxColumnName.addItem(mTableModel.getColumnTitle(column));
		mComboBoxColumnName.setEditable(!isInteractive());

		mTextFieldColumnName = new JTextField();
		mDialogPanel.add(mTextFieldColumnName, "5,9,7,9");

		return mDialogPanel;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/data.html#JEP";
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mCheckBoxOverwriteExisting) {
			updateColumnNameComponent(mCheckBoxOverwriteExisting.isSelected());
			return;
			}

		if (e.getActionCommand().equals("Add Variable")) {
			int index = mTextAreaFormula.getCaretPosition();
			String text = mTextAreaFormula.getText();
			String name = (String) mComboBoxVariableName.getSelectedItem();
			if (index == -1)
				mTextAreaFormula.setText(text+name);
			else
				mTextAreaFormula.setText(text.substring(0, index)+name+text.substring(index));
			return;
			}
		}

	private void updateColumnNameComponent(boolean isOverwrite) {
		if (isOverwrite) {
			mLabelColumnName.setText("Column to overwrite:");
			String item = mTextFieldColumnName.getText();
			mDialogPanel.remove(mTextFieldColumnName);
			mDialogPanel.add(mComboBoxColumnName, "5,9,7,9");
			mComboBoxColumnName.setSelectedItem(item == null ? "" : item);
			mComboBoxColumnName.repaint();
			}
		else {
			mLabelColumnName.setText("New column name:");
			String item = (String)mComboBoxColumnName.getSelectedItem();
			mDialogPanel.remove(mComboBoxColumnName);
			mDialogPanel.add(mTextFieldColumnName, "5,9,7,9");
			mTextFieldColumnName.setText(item == null ? "" : item);
			mTextFieldColumnName.repaint();
			}
		}

	private TreeMap<String,Integer> createColumnMap() {
		TreeSet<String> varNameList = new TreeSet<String>();
		varNameList.add(IS_VISIBLE_ROW);
		varNameList.add(IS_SELECTED_ROW);

		TreeMap<String,Integer> columnMap = new TreeMap<String,Integer>();
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
			if (mTableModel.isColumnDisplayable(column)) {
				String varName = removeConflictingChars(mTableModel.getColumnTitle(column));
				columnMap.put(ensureUniqueness(varName, varNameList), new Integer(column));
				}
			else if (mTableModel.isDescriptorColumn(column)) {
				String specialType = mTableModel.getColumnSpecialType(column);
				String parentName = mTableModel.getColumnTitle(mTableModel.getParentColumn(column));
				String varName = specialType+"_of_"+removeConflictingChars(parentName);
				columnMap.put(ensureUniqueness(varName, varNameList), new Integer(column));
				}
			}

		for (int i = 0; i<mTableModel.getListHandler().getListCount(); i++) {
			String varName = IS_MEMBER_OF + removeConflictingChars(mTableModel.getListHandler().getListName(i));
			columnMap.put(ensureUniqueness(varName, varNameList), new Integer(CompoundTableListHandler.getColumnFromList(i)));
			}


		return columnMap;
		}

	/**
	 * Does any necessary preprocessing of the formula before the formula is passed to JEP.
	 * This includes running pseudo functions, which ask the user for input at runtime.
	 * @param formula
	 * @param isValidation if true, then don't ask, but return just something reasonable for formula validation
	 * @return formula with pseudo functions replaced by variables or null, if the user cancelled a dialog
	 */
	private String preprocessFormula(String formula, boolean isValidation) {
		formula = duplicateIDCodeBackslashes(formula);
		formula = resolvePseudoFunctions(formula, PSEUDO_FUNCTION_ASK_COLUMN_TITLE, isValidation);
		formula = resolvePseudoFunctions(formula, PSEUDO_FUNCTION_ASK_COLUMN_VAR, isValidation);
		formula = resolvePseudoFunctions(formula, PSEUDO_FUNCTION_ASK_STRING, isValidation);
		formula = resolvePseudoFunctions(formula, PSEUDO_FUNCTION_ASK_NUMBER, isValidation);

		mPreprocessVariables = null;
		formula = resolvePreprocessFunctions(formula, PREPROCESS_FUNCTION_TOTAL_VALUE_COUNT, isValidation);
		formula = resolvePreprocessFunctions(formula, PREPROCESS_FUNCTION_NON_EMPTY_VALUE_COUNT, isValidation);
		formula = resolvePreprocessFunctions(formula, PREPROCESS_FUNCTION_STDDEV, isValidation);
		return formula;
		}

	/**
	 * JEP evidently assumes that '\' within the formula escape the following character.
	 * Intentional '\' characters need to be passed as '\\'. Since idcodes may contain
	 * backslash characters, these need to be duplicated to correctly be treated by JEP. 
	 * @param formula
	 * @return
	 */
	private String duplicateIDCodeBackslashes(String formula) {
		if (formula == null)
			return null;

		int index = -1;
		while (true) {
			index = formula.indexOf(JEPChemSimilarityFunction.FUNCTION_NAME+"(", index+1);
			if (index == -1)
				break;

			index += JEPChemSimilarityFunction.FUNCTION_NAME.length() + 1;

			int index2 = formula.indexOf(')', index);
			if (index2 == -1)
				break;

			String substring = formula.substring(index, index2);
			if (substring.indexOf('\\') != -1)
				formula = replacePseudoFunction(formula, index, index2, substring.replace("\\", "\\\\"));

			index = index2;
			}

		return formula;
		}

	/**
	 * Converts preprocess functions (currently only valueCount()) into variable name.
	 * @param formula
	 * @param functionName
	 * @param isValidation if true, then don't ask, but return just something reasonable for formula validation
	 * @return formula with pseudo functions replaced by variables or null, if the user cancelled a dialog
	 */
	private String resolvePreprocessFunctions(String formula, String functionName, boolean isValidation) {
		if (formula == null)
			return null;

		int index = -1;
		while (true) {
			index = formula.indexOf(functionName+"(", index+1);
			if (index == -1)
				break;

			int index2 = index+functionName.length()+1;
			int index3 = formula.indexOf(')', index2);
			if (index3 == -1)
				return formula;	// we have a syntax error; let the parser deal with it

			String resolvedValue = "1";	// default for validation: number or string
			if (!isValidation) {
				Integer column = mRunTimeColumnMap.get(formula.substring(index2, index3).trim());
				if (column == null || column < 0 || !mTableModel.isColumnDisplayable(column))
					return formula;    // we didn't find the column; let the parser create a syntax error

				resolvedValue = functionName.concat(column.toString());

				if (mPreprocessVariables == null)
					mPreprocessVariables = new ArrayList<String>();
				mPreprocessVariables.add(resolvedValue);
				}

			formula = replacePseudoFunction(formula, index, index3+1, resolvedValue);
			}

		return formula;
		}

	/**
	 * Converts pseudo functions of one type into variables.
	 * @param formula
	 * @param functionName
	 * @param isValidation if true, then don't ask, but return just something reasonable for formula validation
	 * @return formula with pseudo functions replaced by variables or null, if the user cancelled a dialog
	 */
	private String resolvePseudoFunctions(String formula, final String functionName, boolean isValidation) {
		if (formula == null)
			return null;

		int index = -1;
		while (true) {
			index = formula.indexOf(functionName, index+1);
			if (index == -1)
				break;

			int index2 = index+functionName.length();
			String msg = extractPseudoFunctionMessage(formula, index2);
			if (msg == null || formula.charAt(index2+msg.length()+2) != ')')	// we expect a closing bracket
				return formula;	// we have a syntax error; let the parser deal with it

			if (isValidation) {
				if (PSEUDO_FUNCTION_ASK_COLUMN_TITLE.equals(functionName))
					mResolvedValue = (mTableModel.getTotalColumnCount() == 0) ? "\"Title\"" : "\""+mTableModel.getColumnTitle(0)+"\"";
				else if (PSEUDO_FUNCTION_ASK_COLUMN_TITLE.equals(functionName))
					mResolvedValue = (mRunTimeColumnMap.size() == 0) ? "title" : mRunTimeColumnMap.firstKey();
				else
					mResolvedValue = "0";	// number or string
				}
			else if (SwingUtilities.isEventDispatchThread()) {
				mResolvedValue = ask(msg, functionName);
				}
			else {
				try {
					final String _msg = msg;
					SwingUtilities.invokeAndWait(new Runnable() {
						@Override
						public void run() {
							mResolvedValue = ask(_msg, functionName);
							}
						});
					}
				catch (Exception e) {}
				}

			if (mResolvedValue == null)
				return null;

			formula = replacePseudoFunction(formula, index, index2+msg.length()+3, mResolvedValue);
			}

		return formula;
		}

	/**
	 * Extracts and returns the text between two double quotes.
	 * Returns null, if index doesn't point to a double quote or if closing double quote is not found
	 * of if the formula ends with the closing double quote.
	 * @param formula
	 * @param index of starting double quote
	 * @return null if syntax error or extracted message, which may be an empty String
	 */
	private String extractPseudoFunctionMessage(String formula, int index) {
		if (formula.charAt(index) != '"')
			return null;

		int index2 = formula.indexOf('"', index+1);
		if (index2 == -1 || index2+1 == formula.length())
			return null;

		return formula.substring(index+1, index2);
		}

	/**
	 * @param formula
	 * @param index1 first char of function name
	 * @param index2 first char after closing bracket
	 * @param varName
	 * @return formula with function replaced by variable name
	 */
	private String replacePseudoFunction(String formula, int index1, int index2, String varName) {
		return formula.substring(0, index1).concat(varName).concat(formula.substring(index2));
		}

	private String ask(final String msg, String functionName) {
		if (functionName.equals(PSEUDO_FUNCTION_ASK_STRING)) {
			String text = JOptionPane.showInputDialog(getParentFrame(), msg, "Define String Value", JOptionPane.QUESTION_MESSAGE);
			return "\""+text+"\"";
			}

		if (functionName.equals(PSEUDO_FUNCTION_ASK_NUMBER)) {
			while (true) {
				String text = JOptionPane.showInputDialog(getParentFrame(), msg, "Define Numerical Value", JOptionPane.QUESTION_MESSAGE);
				try {
					Double.parseDouble(text);
					}
				catch (NumberFormatException nfe) {
					JOptionPane.showMessageDialog(getParentFrame(), "'"+text+"' is not numeric. Try again.", "Number Format Error", JOptionPane.ERROR_MESSAGE);
					continue;
					}
				return text;
				}
			}

		if (functionName.equals(PSEUDO_FUNCTION_ASK_COLUMN_TITLE)
		 || functionName.equals(PSEUDO_FUNCTION_ASK_COLUMN_VAR)) {
			String[] columnNameList = new String[mTableModel.getTotalColumnCount()];
			for (int i=0; i<columnNameList.length; i++)
				columnNameList[i] = mTableModel.getColumnTitle(i);
	
			String title = (String)JOptionPane.showInputDialog(getParentFrame(),
								msg,
								"Select Column",
								JOptionPane.QUESTION_MESSAGE,
								null,
								columnNameList,
								columnNameList[0]);
			if (functionName.equals(PSEUDO_FUNCTION_ASK_COLUMN_TITLE))
				return "\""+title+"\"";

			int column = mTableModel.findColumn(title);
			for (String key:mRunTimeColumnMap.keySet())
				if (mRunTimeColumnMap.get(key).intValue() == column)
					return key;
			}

		return null;	// shouldn't happen
		}

	private boolean parseFormula(JEP parser, String formula) {
		if (mPreprocessVariables != null) {
			for (String varName:mPreprocessVariables) {
				if (varName.startsWith(PREPROCESS_FUNCTION_TOTAL_VALUE_COUNT))
					parser.addVariable(varName, 1);
				if (varName.startsWith(PREPROCESS_FUNCTION_NON_EMPTY_VALUE_COUNT))
					parser.addVariable(varName, 1);
				if (varName.startsWith(PREPROCESS_FUNCTION_STDDEV))
					parser.addVariable(varName, 0);
				}
			}
		Iterator<String> keyIterator = mRunTimeColumnMap.keySet().iterator();
		while (keyIterator.hasNext()) {
			String varName = keyIterator.next();
			int column = mRunTimeColumnMap.get(varName).intValue();
			if (CompoundTableListHandler.isListColumn(column)) {
				parser.addVariable(varName, 0.0);
				}
			else {
				if (mTableModel.getColumnSpecialType(column) != null)
					parser.addVariable(varName, new JEPParameter(null, column));
				else if (mTableModel.isColumnTypeDouble(column)
				 && !mTableModel.isColumnTypeDate(column))
					parser.addVariable(varName, 0.0);
				else
					parser.addVariable(varName, "");
				}
			}
		parser.addVariable(IS_VISIBLE_ROW, 0.0);
		parser.addVariable(IS_SELECTED_ROW, 0.0);

		parser.parseExpression(formula);
		if (parser.hasError()) {
			showErrorMessage(parser.getErrorInfo());
			return false;
			}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		startProgress("Calculating values...", 0, mTableModel.getTotalRowCount());

		// before 28Jul2020 '\n' were not encoded as '<NL>'
		String formula = resolveVariables(configuration.getProperty(PROPERTY_FORMULA,"").replace("\n", "").replace("<NL>", ""));
		if (!parseFormula(mParser, preprocessFormula(formula, false)))
			return;

		boolean isOverwrite = "true".equals(configuration.getProperty(PROPERTY_OVERWRITE_COLUMN));

		String[] columnName = new String[1];
		columnName[0] = configuration.getProperty(PROPERTY_COLUMN_NAME);
		int targetColumn = isOverwrite ? mTableModel.findColumn(configuration.getProperty(PROPERTY_COLUMN_NAME)) : mTableModel.addNewColumns(columnName);
		mTableModel.setColumnProperty(targetColumn, CompoundTableConstants.cColumnPropertyFormula, formula);

		CompoundTableListHandler hitlistHandler = mTableModel.getListHandler();

		for (mCurrentRow=0; mCurrentRow<mTableModel.getTotalRowCount(); mCurrentRow++) {
			if (threadMustDie())
				break;
			if (mCurrentRow % 16 == 0)
				updateProgress(mCurrentRow);

			CompoundRecord record = mTableModel.getTotalRecord(mCurrentRow);
			if (mPreprocessVariables != null) {
				for (String varName:mPreprocessVariables) {
					if (varName.startsWith(PREPROCESS_FUNCTION_TOTAL_VALUE_COUNT)) {
						int column = Integer.parseInt(varName.substring(PREPROCESS_FUNCTION_TOTAL_VALUE_COUNT.length()));
						int count = record.getData(column) == null ? 0
							: mTableModel.separateEntries(mTableModel.encodeData(record, column)).length;
						mParser.addVariable(varName, count);
						}
					if (varName.startsWith(PREPROCESS_FUNCTION_NON_EMPTY_VALUE_COUNT)) {
						int column = Integer.parseInt(varName.substring(PREPROCESS_FUNCTION_NON_EMPTY_VALUE_COUNT.length()));
						int count = 0;
						try {
							mTableModel.tryParseDouble(record, column, CompoundTableConstants.cSummaryModeMean, true);
							count = mTableModel.getParseDoubleValueCount();
							}
						catch (NumberFormatException nfe) {}
						mParser.addVariable(varName, count);
						}
					if (varName.startsWith(PREPROCESS_FUNCTION_STDDEV)) {
						int column = Integer.parseInt(varName.substring(PREPROCESS_FUNCTION_STDDEV.length()));
						double stdDev = Double.NaN;
						if (mTableModel.isColumnTypeDouble(column)) {
							try {
								mTableModel.tryParseDouble(record, column, CompoundTableConstants.cSummaryModeMean, true);
								stdDev = mTableModel.getParseDoubleStdDev();
								}
							catch (NumberFormatException nfe) {}
							}
						mParser.addVariable(varName, stdDev);
						}
					}
				}
			for (String varName:mRunTimeColumnMap.keySet()) {
				int column = mRunTimeColumnMap.get(varName).intValue();
				if (CompoundTableListHandler.isListColumn(column)) {
					long hitlistMask = hitlistHandler.getListMask(CompoundTableListHandler.convertToListIndex(column));
					mParser.addVariable(varName, (record.getFlags() & hitlistMask) != 0 ? 1.0 : 0.0 );
					}
				else {
					if (mTableModel.getColumnSpecialType(column) != null)
						mParser.addVariable(varName, new JEPParameter(record, column));
					else if (mTableModel.isColumnTypeDouble(column)) {  // this includes date for the date functions
						double value = mTableModel.getTotalOriginalDoubleAt(mCurrentRow, column);
						mParser.addVariable(varName, value);
						}
					else
						mParser.addVariable(varName, mTableModel.getValue(record, column));
					}
				}
			mParser.addVariable(IS_VISIBLE_ROW, mTableModel.isVisible(record) ? 1.0 : 0.0);
			mParser.addVariable(IS_SELECTED_ROW, mTableModel.isVisibleAndSelected(record) ? 1.0 : 0.0);

			Object o = mParser.getValueAsObject();

//			String value = (o == null) ? "NaN" : o.toString();
//			if (o instanceof Double && value.endsWith(".0"))
//  			value = value.substring(0, value.length()-2);

			String value = (o == null) ? "NaN"
					: !(o instanceof Double) ? o.toString()
					: DoubleFormat.toString((Double)o, 7, true);

			mTableModel.setTotalValueAt(value, mCurrentRow, targetColumn);
			}

		if (isOverwrite)
			mTableModel.finalizeChangeAlphaNumericalColumn(targetColumn, 0, mTableModel.getTotalRowCount());
		else
			mTableModel.finalizeNewColumns(targetColumn, this);
		}

	/**
	 * During task execution this gets the currently executed row index.
	 * @return
	 */
	public int getCurrentRow() {
		return mCurrentRow;
		}

	private String removeConflictingChars(String s) {
		StringBuffer buf = new StringBuffer(1+s.length());

		// don't start with a digit to avoid implicit multiplication errors
		if (s.length() != 0 && Character.isDigit(s.charAt(0)))
			buf.append('_');

		for (int i=0; i<s.length(); i++) {
			char theChar = s.charAt(i);
			if (theChar == 'µ')	// special handling, because JEP produces syntax errors with this char
				theChar = 'u';
			if (Character.isLetterOrDigit(theChar) || theChar=='_')
				buf.append(theChar);
			}
		return buf.toString();
		}

	private String ensureUniqueness(String varName, TreeSet<String> varNameList) {
		// where variable names collide with a function name, we enforce upper case first letters
		if (mParser.getFunctionTable().containsKey(varName) && Character.isLowerCase(varName.charAt(0)))
			varName = varName.substring(0, 1).toUpperCase().concat(varName.substring(1));

		if (varNameList.contains(varName)) {
			for (int suffix = 2; true; suffix++) {
				String suggestedName = varName.concat("_").concat(Integer.toString(suffix));
				if (!varNameList.contains(suggestedName)) {
					varName = suggestedName;
					break;
					}
				}
			}

		varNameList.add(varName);
		return varName;
		}

	@Override
	public boolean isConfigurable() {
		if (mTableModel.getTotalColumnCount() == 0 || mTableModel.getTotalRowCount() == 0) {
			showErrorMessage("The data table is empty.");
			return false;
			}
		return true;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public Properties getDialogConfiguration() {
		boolean isOverwrite = mCheckBoxOverwriteExisting.isSelected();
		Properties configuration = new Properties();
		configuration.put(PROPERTY_FORMULA, mTextAreaFormula.getText().replace("\n", "<NL>"));
		configuration.put(PROPERTY_OVERWRITE_COLUMN, isOverwrite ? "true" : "false");
		configuration.put(PROPERTY_COLUMN_NAME, mTableModel.getColumnTitleNoAlias(
				isOverwrite ? (String)mComboBoxColumnName.getSelectedItem() : mTextFieldColumnName.getText()));
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		if (mFormula != null) {
			mTextAreaFormula.setText(mFormula);
			mCheckBoxOverwriteExisting.setSelected(true);
			mComboBoxColumnName.setSelectedItem(mTableModel.getColumnTitle(mColumn));
			updateColumnNameComponent(true);
			}
		else {
			boolean isOverwrite = "true".equals(configuration.getProperty(PROPERTY_OVERWRITE_COLUMN));
			mTextAreaFormula.setText(configuration.getProperty(PROPERTY_FORMULA, "").replace("<NL>", "\n"));
			mCheckBoxOverwriteExisting.setSelected(isOverwrite);
			String columnName = configuration.getProperty(PROPERTY_COLUMN_NAME, "");
			if (isOverwrite) {
				int column = mTableModel.findColumn(columnName);
				if (column != -1)
					columnName = mTableModel.getColumnTitle(column);
				mComboBoxColumnName.setSelectedItem(columnName);
				updateColumnNameComponent(true);
				}
			else {
				mTextFieldColumnName.setText(columnName);
				}
			}
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mFormula == null) {
			mTextAreaFormula.setText("");
			mCheckBoxOverwriteExisting.setSelected(false);
			mTextFieldColumnName.setText("Calculated Column");
			}
		else {
			mTextAreaFormula.setText(mFormula);
			mCheckBoxOverwriteExisting.setSelected(true);
			mComboBoxColumnName.setSelectedItem(mTableModel.getColumnTitle(mColumn));
			updateColumnNameComponent(true);
			}
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String columnName = configuration.getProperty(PROPERTY_COLUMN_NAME, "");
		if (columnName.length() == 0) {
			showErrorMessage("No column name specified.");
			return false;
			}

		String formula = resolveVariables(configuration.getProperty(PROPERTY_FORMULA, "").replace("\n", "").replace("<NL>", ""));
		if (formula.length() == 0) {
			showErrorMessage("No formula specified.");
			return false;
			}

		if (isLive) {
			boolean isOverwrite = "true".equals(configuration.getProperty(PROPERTY_OVERWRITE_COLUMN));
			if (isOverwrite) {
				int column = mTableModel.findColumn(columnName);
				if (column == -1) {
					showErrorMessage("Target column not found.");
					return false;
					}
				if (mTableModel.getColumnSpecialType(column) != null) {
					showErrorMessage("Target column is not alpha-numerical.");
					return false;
					}
				}

			mRunTimeColumnMap = createColumnMap();
			formula = preprocessFormula(formula, true);
			if (formula == null)
				return false;	// no error message because the user cancelled

			if (!parseFormula(createParser(), formula))
				return false;	// parseFormula shows error message
			}
		return true;
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
