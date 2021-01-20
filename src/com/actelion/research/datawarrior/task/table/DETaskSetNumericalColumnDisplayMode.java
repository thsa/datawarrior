package com.actelion.research.datawarrior.task.table;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.task.AbstractMultiColumnTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskSetNumericalColumnDisplayMode extends AbstractMultiColumnTask {
	public static final String TASK_NAME = "Set Numerical Column Display Options";

	private static final String PROPERTY_SUMMARY_MODE = "summaryMode";
	private static final String PROPERTY_SHOW_VALUE_COUNT = "showValueCount";
	private static final String PROPERTY_SHOW_STD_DEV = "showStdDev";
	private static final String PROPERTY_ROUNDING = "rounding";
	private static final String PROPERTY_MODIFIERS = "modifierValues";

	private static final String[] SHOW_HIDE_CODE = {"show", "hide"};
	private static final String[] SHOW_HIDE_TEXT = {"Show", "Hide"};

	public static final String[] ROUNDING_CODE = {"none", "1", "2", "3", "4", "5", "6", "7"};
	public static final String[] ROUNDING_TEXT = {"Not rounded", "1", "2", "3", "4", "5", "6", "7"};

	private static final String[] MODIFIER_CODE = {"consider", "exclude"};
	private static final String[] MODIFIER_TEXT = {"Consider values", "Exclude values"};

	private static final String UNCHANGED_CODE = "unchanged";
	private static final String UNCHANGED_TEXT = "Keep unchanged";

	public static final int UNCHANGED = -1;

	private JComboBox mComboBoxSummaryMode,mComboBoxValueCount,mComboBoxStdDev,mComboBoxRounding,mComboBoxModifiers;
	private int mSummaryMode,mShowValueCount,mShowStdDev,mRounding,mExcludeModifierValues;

	public DETaskSetNumericalColumnDisplayMode(Frame owner, CompoundTableModel tableModel) {
		super(owner, tableModel, false);
		}

	/**
	 * Constructor for interactively define and run the task without showing a configuration dialog.
	 * @param owner
	 * @param tableModel
	 * @param column valid column
	 * @param summaryMode valid summaryMode or -1 to keep untouched
	 * @param showValueCount 0 or 1; -1 to keep untouched
	 * @param showStdDev 0 or 1; -1 to keep untouched
	 * @param rounding 1 to 7; 0 for unrounded, -1 to keep untouched
	 * @param excludeModifierValues 0 (consider), 1 (exclude), -1 (keep untouched)
	 */
	public DETaskSetNumericalColumnDisplayMode(Frame owner, CompoundTableModel tableModel, int column, int summaryMode,
	                                           int showValueCount, int showStdDev, int rounding, int excludeModifierValues) {
		this(owner, tableModel, createColumnList(column), summaryMode, showValueCount, showStdDev, rounding, excludeModifierValues);
		}

	/**
	 * Constructor for interactively define and run the task without showing a configuration dialog.
	 * @param owner
	 * @param tableModel
	 * @param columnList valid column list
	 * @param summaryMode valid summaryMode or -1 to keep untouched
	 * @param showValueCount 0 or 1; -1 to keep untouched
	 * @param showStdDev 0 or 1; -1 to keep untouched
	 * @param rounding 1 to 7; 0 for unrounded, -1 to keep untouched
	 * @param excludeModifierValues 0 (consider), 1 (exclude), -1 (keep untouched)
	 */
	public DETaskSetNumericalColumnDisplayMode(Frame owner, CompoundTableModel tableModel, int[] columnList, int summaryMode,
	                                           int showValueCount, int showStdDev, int rounding, int excludeModifierValues) {
		super(owner, tableModel, false, columnList);
		mSummaryMode = summaryMode;
		mShowValueCount = showValueCount;
		mShowStdDev = showStdDev;
		mRounding = rounding;
		mExcludeModifierValues = excludeModifierValues;
		}

	private static int[] createColumnList(int column) {
		int[] columnList = new int[1];
		columnList[0] = column;
		return columnList;
		}

	@Override
	public boolean isCompatibleColumn(int column) {
		// this includes date, although date is only compatible with summaryMode and showValueCount
		return getTableModel().isColumnTypeDouble(column);
		}

	@Override
	public Properties getPredefinedConfiguration() {
		Properties configuration = super.getPredefinedConfiguration();
		if (configuration != null) {
			configuration.put(PROPERTY_SUMMARY_MODE, mSummaryMode==UNCHANGED ? UNCHANGED_CODE : CompoundTableConstants.cSummaryModeCode[mSummaryMode]);
			configuration.put(PROPERTY_SHOW_VALUE_COUNT, mShowValueCount==UNCHANGED ? UNCHANGED_CODE : SHOW_HIDE_CODE[mShowValueCount]);
			configuration.put(PROPERTY_SHOW_STD_DEV,  mShowStdDev==UNCHANGED ? UNCHANGED_CODE : SHOW_HIDE_CODE[mShowStdDev]);
			configuration.put(PROPERTY_ROUNDING,  mRounding==UNCHANGED ? UNCHANGED_CODE : ROUNDING_CODE[mRounding]);
			configuration.put(PROPERTY_MODIFIERS,  mExcludeModifierValues==UNCHANGED ? UNCHANGED_CODE : MODIFIER_CODE[mExcludeModifierValues]);
			}

		return configuration;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();

		boolean keepSummaryMode = (mComboBoxSummaryMode.getSelectedIndex() == CompoundTableConstants.cSummaryModeCode.length);
		configuration.put(PROPERTY_SUMMARY_MODE, keepSummaryMode ? UNCHANGED_CODE
				: CompoundTableConstants.cSummaryModeCode[mComboBoxSummaryMode.getSelectedIndex()]);
		boolean keepShowValueCount = (mComboBoxValueCount.getSelectedIndex() == SHOW_HIDE_CODE.length);
		configuration.put(PROPERTY_SHOW_VALUE_COUNT, keepShowValueCount ? UNCHANGED_CODE
				: SHOW_HIDE_CODE[mComboBoxValueCount.getSelectedIndex()]);
		boolean keepShowStdDev = (mComboBoxStdDev.getSelectedIndex() == SHOW_HIDE_CODE.length);
		configuration.put(PROPERTY_SHOW_STD_DEV, keepShowStdDev ? UNCHANGED_CODE
				: SHOW_HIDE_CODE[mComboBoxStdDev.getSelectedIndex()]);
		boolean keepRounding = (mComboBoxRounding.getSelectedIndex() == ROUNDING_CODE.length);
		configuration.put(PROPERTY_ROUNDING, keepRounding ? UNCHANGED_CODE
				: ROUNDING_CODE[mComboBoxRounding.getSelectedIndex()]);
		boolean keepModifierOption = (mComboBoxModifiers.getSelectedIndex() == MODIFIER_CODE.length);
		configuration.put(PROPERTY_MODIFIERS, keepModifierOption ? UNCHANGED_CODE
				: MODIFIER_CODE[mComboBoxModifiers.getSelectedIndex()]);

		return configuration;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mComboBoxSummaryMode.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_SUMMARY_MODE),
				CompoundTableConstants.cSummaryModeCode, CompoundTableConstants.cSummaryModeCode.length));
		mComboBoxValueCount.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_SHOW_VALUE_COUNT),
				SHOW_HIDE_CODE, SHOW_HIDE_CODE.length));
		mComboBoxStdDev.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_SHOW_STD_DEV),
				SHOW_HIDE_CODE, SHOW_HIDE_CODE.length));
		mComboBoxRounding.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_ROUNDING),
				ROUNDING_CODE, ROUNDING_CODE.length));
		mComboBoxModifiers.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_MODIFIERS),
				MODIFIER_CODE, MODIFIER_CODE.length));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mComboBoxSummaryMode.setSelectedIndex(mComboBoxSummaryMode.getItemCount()-1);
		mComboBoxValueCount.setSelectedIndex(mComboBoxValueCount.getItemCount()-1);
		mComboBoxStdDev.setSelectedIndex(mComboBoxStdDev.getItemCount()-1);
		mComboBoxRounding.setSelectedIndex(mComboBoxRounding.getItemCount()-1);
		mComboBoxModifiers.setSelectedIndex(mComboBoxModifiers.getItemCount()-1);
		}

	@Override
	public JPanel createInnerDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2,
							 TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap} };

		mComboBoxSummaryMode = new JComboBox(CompoundTableConstants.cSummaryModeText);
		mComboBoxSummaryMode.addItem(UNCHANGED_TEXT);
		mComboBoxValueCount = new JComboBox(SHOW_HIDE_TEXT);
		mComboBoxValueCount.addItem(UNCHANGED_TEXT);
		mComboBoxStdDev = new JComboBox(SHOW_HIDE_TEXT);
		mComboBoxStdDev.addItem(UNCHANGED_TEXT);
		mComboBoxRounding = new JComboBox(ROUNDING_TEXT);
		mComboBoxRounding.addItem(UNCHANGED_TEXT);
		mComboBoxModifiers = new JComboBox(MODIFIER_TEXT);
		mComboBoxModifiers.addItem(UNCHANGED_TEXT);

		JPanel ip = new JPanel();
		ip.setLayout(new TableLayout(size));
		ip.add(new JLabel("Show multiple values as:"), "1,1");
		ip.add(new JLabel("Show value count:"), "1,3");
		ip.add(new JLabel("Show standard deviation:"), "1,5");
		ip.add(new JLabel("Round to significant digits:"), "1,7");
		ip.add(new JLabel("Values with modifiers:"), "1,9");
		ip.add(mComboBoxSummaryMode, "3,1");
		ip.add(mComboBoxValueCount, "3,3");
		ip.add(mComboBoxStdDev, "3,5");
		ip.add(mComboBoxRounding, "3,7");
		ip.add(mComboBoxModifiers, "3,9");
		return ip;
		}

	@Override
	public void runTask(Properties configuration) {
		int summaryMode = findListIndex(configuration.getProperty(PROPERTY_SUMMARY_MODE), CompoundTableConstants.cSummaryModeCode, UNCHANGED);
		int showValueCount = findListIndex(configuration.getProperty(PROPERTY_SHOW_VALUE_COUNT), SHOW_HIDE_CODE, UNCHANGED);
		int showStdDev = findListIndex(configuration.getProperty(PROPERTY_SHOW_STD_DEV), SHOW_HIDE_CODE, UNCHANGED);
		int rounding = findListIndex(configuration.getProperty(PROPERTY_ROUNDING), ROUNDING_CODE, UNCHANGED);
		int modifierValues = findListIndex(configuration.getProperty(PROPERTY_MODIFIERS), MODIFIER_CODE, UNCHANGED);

		int[] columnList = getColumnList(configuration);

		for (int column:columnList) {
			if (summaryMode != UNCHANGED)
				getTableModel().setColumnSummaryMode(column, summaryMode);
			if (showValueCount != UNCHANGED)
				getTableModel().setColumnSummaryCountHidden(column, showValueCount == 1);
			if (showStdDev != UNCHANGED && !getTableModel().isColumnTypeDate(column))
				getTableModel().setColumnStdDeviationShown(column, showStdDev == 0);
			if (rounding != UNCHANGED && !getTableModel().isColumnTypeDate(column))
				getTableModel().setColumnSignificantDigits(column, rounding);
			if (modifierValues != UNCHANGED && !getTableModel().isColumnTypeDate(column))
				getTableModel().setColumnModifierExclusion(column, modifierValues == 1);
			}
		}
	}
