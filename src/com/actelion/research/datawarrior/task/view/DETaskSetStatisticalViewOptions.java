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

package com.actelion.research.datawarrior.task.view;

import com.actelion.research.calc.CorrelationCalculator;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.gui.table.ChemistryCellRenderer;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.*;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Properties;


public class DETaskSetStatisticalViewOptions extends DETaskAbstractSetViewOptions {

	public static final String TASK_NAME = "Set Statistical View Options";

    private static final String PROPERTY_CORRELATION_TYPE = "correlationType";
    private static final String PROPERTY_CURVE_TYPE = "meanLineType";
	private static final String PROPERTY_CURVE_LIST = "curveList";
    private static final String PROPERTY_SHOW_STDDEV_AREA = "showStdDev";
	private static final String PROPERTY_TRUNCATE_CURVE = "truncateCurve";
    private static final String PROPERTY_SPLIT_BY_CATEGORY = "splitCurvesByCategory";
	private static final String PROPERTY_SPLIT_CURVE_COLUMN = "splitCurveColumn";
	private static final String PROPERTY_CURVE_LINE_WIDTH = "curveWidth";
	private static final String PROPERTY_CURVE_SMOOTHING = "curveSmoothing";
    private static final String PROPERTY_SHOW_BAR_OR_PIE_SIZE_VALUE = "showBarOrPieSizeValue";
	private static final String PROPERTY_BOXPLOT_SHOW_VALUE_COUNT = "showValueCount";
    private static final String PROPERTY_BOXPLOT_SHOW_STDDEV = "showBoxStdDev";
    private static final String PROPERTY_BOXPLOT_SHOW_CONF_INTERVAL = "showConfInterval";
    private static final String PROPERTY_BOXPLOT_SHOW_PVALUE = "showPValues";
    private static final String PROPERTY_BOXPLOT_SHOW_FOLDCHANGE = "showFoldChange";
    private static final String PROPERTY_PVALUE_COLUMN = "pValueColumn";
    private static final String PROPERTY_PVALUE_REF_CATEGORY = "pValueRefCategory";
    private static final String PROPERTY_BOXPLOT_MEAN_MODE = "boxPlotMeanMode";
    private static final String PROPERTY_BOXPLOT_MEAN_VALUES = "boxPlotMeanValues";
	private static final String PROPERTY_CURVE_FORMULA = "formula";

	private static final String ROW_LIST_TEXT_VISIBLE = "Visible Rows";
	private static final String ROW_LIST_TEXT_SELECTED = "Selected Rows";

	private JComboBox	mComboBoxCorrelationType, mComboBoxCurveMode,mComboBoxBoxplotMeanMode,mComboBoxSplitCurveColumn,
						mComboBoxCurveList;
	private JCheckBox   mCheckBoxShowStdDev1, mCheckBoxSplitCurvesByColor,mCheckBoxShowMeanValues, mCheckBoxTruncateCurve,
						mCheckBoxShowPValues,mCheckBoxShowFoldChange,mCheckBoxShowBarOrPieSizeValue,
						mCheckBoxShowStdDev2,mCheckBoxShowConfInterval,mCheckBoxShowValueCount;
	private JTextField  mTextFieldFormula;
	private JSlider     mSliderCurveLineWidth,mSliderCurveSmoothing;
	private JComponent	mSelectorPValueColumn,mSelectorPValueRefCategory;
	private ListCellRenderer mDefaultPValueRefCategoryRenderer;
	private Dimension   mDefaultPValueRefCategorySize;

	/**
	 * @param owner
	 * @param mainPane
	 * @param view null or view that is interactively updated
	 */
	public DETaskSetStatisticalViewOptions(Frame owner, DEMainPane mainPane, VisualizationPanel2D view) {
		super(owner, mainPane, view);
		}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return false;
		}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof VisualizationPanel) ? null : "Statistical view options can only be defined for 2D- and 3D-Views.";
		}

	@Override
	public OTHER_VIEWS getOtherViewMode() {
		return OTHER_VIEWS.GRAPHICAL2D;
		}

	@Override
	public JComponent createViewOptionContent() {
		JTabbedPane tabbedPane = new JTabbedPane();

		int gap = HiDPIHelper.scale(8);
		double[][] scatterPlotSize = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap},
									   {gap, TableLayout.PREFERRED, gap*3, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap,
											   TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/4,
											   TableLayout.PREFERRED, gap/4, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap,
											   TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap} };
		JPanel scatterPlotPanel = new JPanel();
		scatterPlotPanel.setLayout(new TableLayout(scatterPlotSize));

		scatterPlotPanel.add(new JLabel("Show correlation coefficient:", JLabel.RIGHT), "1,1");

		mComboBoxCorrelationType = new JComboBox();
		mComboBoxCorrelationType.addItem("<none>");
		for (int i=0; i<CorrelationCalculator.TYPE_LONG_NAME.length; i++)
	        mComboBoxCorrelationType.addItem(CorrelationCalculator.TYPE_LONG_NAME[i]);
		mComboBoxCorrelationType.addActionListener(this);
		scatterPlotPanel.add(mComboBoxCorrelationType, "3,1");

		scatterPlotPanel.add(new JLabel("Display line/curve:", JLabel.RIGHT), "1,3");
		mComboBoxCurveMode = new JComboBox(JVisualization2D.CURVE_MODE_TEXT);
		mComboBoxCurveMode.addActionListener(this);
		scatterPlotPanel.add(mComboBoxCurveMode, "3,3");

		mComboBoxCurveList = new JComboBox();
		mComboBoxCurveList.addItem(ROW_LIST_TEXT_VISIBLE);
		mComboBoxCurveList.addItem(ROW_LIST_TEXT_SELECTED);
		for (int i = 0; i<getTableModel().getListHandler().getListCount(); i++)
			mComboBoxCurveList.addItem(getTableModel().getListHandler().getListName(i));
		mComboBoxCurveList.setEditable(!hasInteractiveView());
		mComboBoxCurveList.setEnabled(false);
		mComboBoxCurveList.addActionListener(this);
		scatterPlotPanel.add(new JLabel("Considered rows:", JLabel.RIGHT), "1,5");
		scatterPlotPanel.add(mComboBoxCurveList, "3,5");

		scatterPlotPanel.add(new JLabel("Formula: f(x)=", JLabel.RIGHT), "1,7");
		mTextFieldFormula = new JTextField();
		mTextFieldFormula.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				super.keyReleased(e);
				update(false);
				}
			});
		scatterPlotPanel.add(mTextFieldFormula, "3,7");

		mCheckBoxShowStdDev1 = new JCheckBox("Display standard deviation");
        mCheckBoxShowStdDev1.setEnabled(false);
		mCheckBoxShowStdDev1.addActionListener(this);
		scatterPlotPanel.add(mCheckBoxShowStdDev1, "3,9");

		mCheckBoxTruncateCurve = new JCheckBox("Truncate curve/line to marker area");
		mCheckBoxTruncateCurve.setEnabled(false);
		mCheckBoxTruncateCurve.addActionListener(this);
		scatterPlotPanel.add(mCheckBoxTruncateCurve, "3,11");

		scatterPlotPanel.add(new JLabel("Split line/curve into sections", JLabel.RIGHT), "1,13");
		mCheckBoxSplitCurvesByColor = new JCheckBox("by current marker color categories");
		mCheckBoxSplitCurvesByColor.setEnabled(false);
		mCheckBoxSplitCurvesByColor.addActionListener(this);
		scatterPlotPanel.add(mCheckBoxSplitCurvesByColor, "3,13");

		scatterPlotPanel.add(new JLabel("and/or by category column:", JLabel.RIGHT), "1,15");
		mComboBoxSplitCurveColumn = new JComboBox();
		mComboBoxSplitCurveColumn.addItem("<none>");
		for (int column=0; column<getTableModel().getTotalColumnCount(); column++)
			if (getTableModel().isColumnTypeCategory(column))
				mComboBoxSplitCurveColumn.addItem(getTableModel().getColumnTitle(column));
		mComboBoxSplitCurveColumn.setEnabled(false);
		mComboBoxSplitCurveColumn.setEditable(!isInteractive());
		mComboBoxSplitCurveColumn.addActionListener(this);
		scatterPlotPanel.add(mComboBoxSplitCurveColumn, "3,15");

		mSliderCurveSmoothing = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
		mSliderCurveSmoothing.setPreferredSize(new Dimension(HiDPIHelper.scale(150), mSliderCurveSmoothing.getPreferredSize().height));
		mSliderCurveSmoothing.addChangeListener(this);
		scatterPlotPanel.add(new JLabel("Smoothing grade:", JLabel.RIGHT), "1,17");
		scatterPlotPanel.add(mSliderCurveSmoothing, "3,17");

		mSliderCurveLineWidth = new JSlider(JSlider.HORIZONTAL, 0, 100, 20);
		mSliderCurveLineWidth.setPreferredSize(new Dimension(HiDPIHelper.scale(150), mSliderCurveLineWidth.getPreferredSize().height));
		mSliderCurveLineWidth.addChangeListener(this);
		scatterPlotPanel.add(new JLabel("Curve line width:", JLabel.RIGHT), "1,19");
		scatterPlotPanel.add(mSliderCurveLineWidth, "3,19");

		tabbedPane.add(scatterPlotPanel, "Scatter Plot");

		boolean isBoxPlot = (!hasInteractiveView()
				|| getInteractiveVisualization().getChartType() == JVisualization.cChartTypeBoxPlot
				|| getInteractiveVisualization().getChartType() == JVisualization.cChartTypeWhiskerPlot
				|| getInteractiveVisualization().getChartType() == JVisualization.cChartTypeViolins);
		double[][] boxPlotSize = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, TableLayout.FILL},
								   {gap/2, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED,
										   gap, TableLayout.PREFERRED, TableLayout.PREFERRED,
										   gap/2, TableLayout.PREFERRED, gap/4, TableLayout.PREFERRED,
										   gap*2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2} };
		JPanel boxPlotPanel = new JPanel();
		boxPlotPanel.setLayout(new TableLayout(boxPlotSize));

		CompoundTableModel tableModel = getTableModel();

		mCheckBoxShowBarOrPieSizeValue = new JCheckBox("Show bar/pie size value");
		mCheckBoxShowBarOrPieSizeValue.addActionListener(this);
		mCheckBoxShowBarOrPieSizeValue.setEnabled(!isBoxPlot);
		boxPlotPanel.add(mCheckBoxShowBarOrPieSizeValue, "1,1,3,1");

		mCheckBoxShowStdDev2 = new JCheckBox("Show standard deviation");
		mCheckBoxShowStdDev2.addActionListener(this);
		boxPlotPanel.add(mCheckBoxShowStdDev2, "1,2,3,2");

		mCheckBoxShowConfInterval = new JCheckBox("Show confidence interval (95 %)");
		mCheckBoxShowConfInterval.addActionListener(this);
		boxPlotPanel.add(mCheckBoxShowConfInterval, "1,3,3,3");

		mCheckBoxShowValueCount = new JCheckBox("Show value count N");
		mCheckBoxShowValueCount.addActionListener(this);
		boxPlotPanel.add(mCheckBoxShowValueCount, "1,4,3,4");

		mCheckBoxShowPValues = new JCheckBox("Show p-values");
		mCheckBoxShowPValues.addActionListener(this);
		mCheckBoxShowPValues.setEnabled(isBoxPlot);
		boxPlotPanel.add(mCheckBoxShowPValues, "1,6,3,6");

		mCheckBoxShowFoldChange = new JCheckBox("Show fold-change");
		mCheckBoxShowFoldChange.addActionListener(this);
		mCheckBoxShowFoldChange.setEnabled(isBoxPlot);
		boxPlotPanel.add(mCheckBoxShowFoldChange, "1,7,3,7");

		boxPlotPanel.add(new JLabel("Compare values on:"), "1,9");
		if (hasInteractiveView()) {
			JComboBox cb = new JComboBox();
			for (int column=0; column<tableModel.getTotalColumnCount(); column++)
				if (getInteractiveVisualization().isValidPValueColumn(column))
					cb.addItem(tableModel.getColumnTitle(column));
			cb.addActionListener(this);
			mSelectorPValueColumn = cb;
			}
		else {
			mSelectorPValueColumn = new JTextField();
			}
		boxPlotPanel.add(mSelectorPValueColumn, "3,9");

		boxPlotPanel.add(new JLabel("Reference category:"), "1,11");
		if (hasInteractiveView()) {
			JComboBox cb = new JComboBox();
			int column = getTableModel().findColumn((String)((JComboBox)mSelectorPValueColumn).getSelectedItem());
			if (column != -1) {
				updateRenderer(cb, column);
	    		String[] categories = getTableModel().getCategoryList(column);
	    		for (String category:categories)
	    			cb.addItem(category);
				}
			cb.addActionListener(this);
			mSelectorPValueRefCategory = cb;
			}
		else {
			mSelectorPValueRefCategory = new JTextField();
			}
		boxPlotPanel.add(mSelectorPValueRefCategory, "3,11");

		if (hasInteractiveView()
		 && ((JComboBox)mSelectorPValueColumn).getItemCount() == 0) {
			mCheckBoxShowPValues.setEnabled(false);
			mCheckBoxShowFoldChange.setEnabled(false);
			}

		boxPlotPanel.add(new JLabel("Mean/median mode:"), "1,13");
		mComboBoxBoxplotMeanMode = new JComboBox(JVisualization2D.BOXPLOT_MEAN_MODE_TEXT);
		mComboBoxBoxplotMeanMode.setEnabled(isBoxPlot);
		mComboBoxBoxplotMeanMode.addActionListener(this);
		boxPlotPanel.add(mComboBoxBoxplotMeanMode, "3,13");

		mCheckBoxShowMeanValues = new JCheckBox(isBoxPlot ? "Show mean/median values" : "Show mean values");
		mCheckBoxShowMeanValues.addActionListener(this);
		boxPlotPanel.add(mCheckBoxShowMeanValues, "1,15,3,15");

		tabbedPane.add(boxPlotPanel, "Box Plot / Bar Chart");

		if (hasInteractiveView() && getInteractiveVisualization().getChartType() != JVisualization.cChartTypeScatterPlot)
			tabbedPane.setSelectedComponent(boxPlotPanel);

		return tabbedPane;
		}

	private void updateRenderer(JComboBox cb, int column) {
		if (mDefaultPValueRefCategoryRenderer == null) {
			mDefaultPValueRefCategoryRenderer = cb.getRenderer();
			mDefaultPValueRefCategorySize = cb.getPreferredSize();
			}
		if (getTableModel().isColumnTypeStructure(column)
		 || getTableModel().isColumnTypeReaction(column)) {
			// After replacing the JComboBox cell renderer with one with a different preferred size,
			// the JComboBox's preferred size does not adapt automatically. We need to adapt that as well.
			Dimension preferredSize = new Dimension(HiDPIHelper.scale(120), HiDPIHelper.scale(80));
			cb.setRenderer(new ChemistryCellRenderer(preferredSize));
			cb.setPreferredSize(preferredSize);
			}
		else {
			cb.setRenderer(mDefaultPValueRefCategoryRenderer);
			cb.setPreferredSize(mDefaultPValueRefCategorySize);
			}
		if (getDialog() != null)
			getDialog().pack();
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		if (mComboBoxCorrelationType.getSelectedIndex() != 0)
			configuration.setProperty(PROPERTY_CORRELATION_TYPE, CorrelationCalculator.TYPE_NAME[mComboBoxCorrelationType.getSelectedIndex()-1]);

		if (mComboBoxCurveMode.getSelectedIndex() != 0) {
			configuration.setProperty(PROPERTY_CURVE_TYPE, JVisualization2D.CURVE_MODE_CODE[mComboBoxCurveMode.getSelectedIndex()]);
			if (mComboBoxCurveList.isEnabled()) {
				String listName = (String)mComboBoxCurveList.getSelectedItem();
				configuration.setProperty(PROPERTY_CURVE_LIST, ROW_LIST_TEXT_VISIBLE.equals(listName) ?
						JVisualization2D.ROW_LIST_CODE_VISIBLE : ROW_LIST_TEXT_SELECTED.equals(listName) ?
						JVisualization2D.ROW_LIST_CODE_SELECTED : listName);
				}
			configuration.setProperty(PROPERTY_SHOW_STDDEV_AREA, mCheckBoxShowStdDev1.isSelected() ? "true" : "false");
			if (mCheckBoxTruncateCurve.isEnabled())
				configuration.setProperty(PROPERTY_TRUNCATE_CURVE, mCheckBoxTruncateCurve.isSelected() ? "true" : "false");
			if (mCheckBoxSplitCurvesByColor.isEnabled())
				configuration.setProperty(PROPERTY_SPLIT_BY_CATEGORY, mCheckBoxSplitCurvesByColor.isSelected() ? "true" : "false");
			if (mComboBoxSplitCurveColumn.isEnabled() && mComboBoxSplitCurveColumn.getSelectedIndex() != 0)
				configuration.setProperty(PROPERTY_SPLIT_CURVE_COLUMN, getTableModel().getColumnTitleNoAlias((String)mComboBoxSplitCurveColumn.getSelectedItem()));
			configuration.setProperty(PROPERTY_CURVE_FORMULA, mTextFieldFormula.getText());
			}

		configuration.setProperty(PROPERTY_BOXPLOT_SHOW_STDDEV, mCheckBoxShowStdDev2.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_BOXPLOT_SHOW_CONF_INTERVAL, mCheckBoxShowConfInterval.isSelected() ? "true" : "false");
		float lineWidth = (float)Math.exp((double)mSliderCurveLineWidth.getValue()/50.0);
		configuration.setProperty(PROPERTY_CURVE_LINE_WIDTH, ""+lineWidth);
		float smoothing = (float)mSliderCurveSmoothing.getValue()/100f;
		configuration.setProperty(PROPERTY_CURVE_SMOOTHING, ""+smoothing);
		configuration.setProperty(PROPERTY_BOXPLOT_SHOW_VALUE_COUNT, mCheckBoxShowValueCount.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_SHOW_BAR_OR_PIE_SIZE_VALUE, mCheckBoxShowBarOrPieSizeValue.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_BOXPLOT_SHOW_PVALUE, mCheckBoxShowPValues.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_BOXPLOT_SHOW_FOLDCHANGE, mCheckBoxShowFoldChange.isSelected() ? "true" : "false");

		if (mSelectorPValueColumn instanceof JComboBox) {
			if (((JComboBox)mSelectorPValueColumn).getItemCount() != 0
			 && ((JComboBox)mSelectorPValueRefCategory).getItemCount() >= 2) {
				String pValueColumn = (String)((JComboBox)mSelectorPValueColumn).getSelectedItem();
				configuration.setProperty(PROPERTY_PVALUE_COLUMN, getTableModel().getColumnTitleNoAlias(getTableModel().findColumn(pValueColumn)));
				String pValueRefCategory = (String)((JComboBox)mSelectorPValueRefCategory).getSelectedItem();
				configuration.setProperty(PROPERTY_PVALUE_REF_CATEGORY, pValueRefCategory);
				}
			}
		else {
			String pValueColumn = ((JTextField)mSelectorPValueColumn).getText();
			if (pValueColumn.length() != 0) {
				configuration.setProperty(PROPERTY_PVALUE_COLUMN, pValueColumn);
				configuration.setProperty(PROPERTY_PVALUE_REF_CATEGORY, ((JTextField)mSelectorPValueRefCategory).getText());
				}
			}

		configuration.setProperty(PROPERTY_BOXPLOT_MEAN_MODE, JVisualization.BOXPLOT_MEAN_MODE_CODE[mComboBoxBoxplotMeanMode.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_BOXPLOT_MEAN_VALUES, mCheckBoxShowMeanValues.isSelected() ? "true" : "false");
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		mComboBoxCorrelationType.setSelectedIndex(1+findListIndex(configuration.getProperty(PROPERTY_CORRELATION_TYPE),
																  CorrelationCalculator.TYPE_NAME, -1));
		mComboBoxCurveMode.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_CURVE_TYPE),
														 JVisualization2D.CURVE_MODE_CODE, 0));
		String listName = configuration.getProperty(PROPERTY_CURVE_LIST, JVisualization2D.ROW_LIST_CODE_VISIBLE);
		mComboBoxCurveList.setSelectedItem(JVisualization2D.ROW_LIST_CODE_VISIBLE.equals(listName) ? ROW_LIST_TEXT_VISIBLE
				: JVisualization2D.ROW_LIST_CODE_SELECTED.equals(listName) ? ROW_LIST_TEXT_SELECTED : listName);
		mCheckBoxShowStdDev1.setSelected("true".equals(configuration.getProperty(PROPERTY_SHOW_STDDEV_AREA)));
		mCheckBoxTruncateCurve.setSelected("true".equals(configuration.getProperty(PROPERTY_TRUNCATE_CURVE)));
		mCheckBoxSplitCurvesByColor.setSelected("true".equals(configuration.getProperty(PROPERTY_SPLIT_BY_CATEGORY)));
		int splitCurveColumn = getTableModel().findColumn(configuration.getProperty(PROPERTY_SPLIT_CURVE_COLUMN));
		if (splitCurveColumn != -1 && getTableModel().isColumnTypeCategory(splitCurveColumn))
			mComboBoxSplitCurveColumn.setSelectedItem(getTableModel().getColumnTitle(splitCurveColumn));
		else
			mComboBoxSplitCurveColumn.setSelectedIndex(0);

		mTextFieldFormula.setText(configuration.getProperty(PROPERTY_CURVE_FORMULA));

		mCheckBoxShowStdDev2.setSelected("true".equals(configuration.getProperty(PROPERTY_BOXPLOT_SHOW_STDDEV)));
		String value = configuration.getProperty(PROPERTY_CURVE_SMOOTHING);
		float smoothing = (value == null) ? JVisualization2D.DEFAULT_CURVE_SMOOTHING : Float.parseFloat(value);
		mSliderCurveSmoothing.setValue(Math.round(100*smoothing));
		value = configuration.getProperty(PROPERTY_CURVE_LINE_WIDTH);
		float lineWidth = (value == null) ? JVisualization2D.DEFAULT_CURVE_LINE_WIDTH : Float.parseFloat(value);
		mSliderCurveLineWidth.setValue((int)(50.0*Math.log(lineWidth)));
		mCheckBoxShowBarOrPieSizeValue.setSelected("true".equals(configuration.getProperty(PROPERTY_SHOW_BAR_OR_PIE_SIZE_VALUE)));
		mCheckBoxShowConfInterval.setSelected("true".equals(configuration.getProperty(PROPERTY_BOXPLOT_SHOW_CONF_INTERVAL)));
		mCheckBoxShowValueCount.setSelected("true".equals(configuration.getProperty(PROPERTY_BOXPLOT_SHOW_VALUE_COUNT)));
		mCheckBoxShowPValues.setSelected("true".equals(configuration.getProperty(PROPERTY_BOXPLOT_SHOW_PVALUE)));
		mCheckBoxShowFoldChange.setSelected("true".equals(configuration.getProperty(PROPERTY_BOXPLOT_SHOW_FOLDCHANGE)));

		String pValueColumn = configuration.getProperty(PROPERTY_PVALUE_COLUMN);
		if (mSelectorPValueColumn instanceof JComboBox) {
			int column = getTableModel().findColumn(pValueColumn);
			if (getInteractiveVisualization().isValidPValueColumn(column)) {
				((JComboBox)mSelectorPValueColumn).setSelectedItem(getTableModel().getColumnTitle(column));
				((JComboBox)mSelectorPValueRefCategory).setSelectedItem(configuration.getProperty(PROPERTY_PVALUE_REF_CATEGORY));
				}
			}
		else if (pValueColumn != null) {
			((JTextField)mSelectorPValueColumn).setText(pValueColumn);
			((JTextField)mSelectorPValueRefCategory).setText(configuration.getProperty(PROPERTY_PVALUE_REF_CATEGORY));
			}

		mComboBoxBoxplotMeanMode.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_BOXPLOT_MEAN_MODE),
				 												JVisualization.BOXPLOT_MEAN_MODE_CODE, 3));
		mCheckBoxShowMeanValues.setSelected("true".equals(configuration.getProperty(PROPERTY_BOXPLOT_MEAN_VALUES)));

		enableItems();
		}

	@Override
	public void setDialogToDefault() {
		mComboBoxCorrelationType.setSelectedIndex(0);
		mComboBoxCurveMode.setSelectedIndex(0);
		mComboBoxSplitCurveColumn.setSelectedIndex(0);
		mCheckBoxShowBarOrPieSizeValue.setSelected(false);
		mCheckBoxShowStdDev2.setSelected(false);
		mTextFieldFormula.setText("");
		mSliderCurveSmoothing.setValue(50);
		mSliderCurveLineWidth.setValue(20);
		mCheckBoxShowConfInterval.setSelected(false);
		mCheckBoxShowValueCount.setSelected(false);
		mCheckBoxShowPValues.setSelected(false);
		mCheckBoxShowFoldChange.setSelected(false);
		((JTextField)mSelectorPValueColumn).setText("");
		((JTextField)mSelectorPValueRefCategory).setText("");
		mComboBoxBoxplotMeanMode.setSelectedIndex(JVisualization.BOXPLOT_DEFAULT_MEAN_MODE);

		enableItems();
		}

	@Override
	public String getDialogTitle() {
		return "Set Statistical View Options";
		}

	@Override
	public void addViewConfiguration(CompoundTableView view, Properties configuration) {
		JVisualization2D v2d = (JVisualization2D)((VisualizationPanel)view).getVisualization();
		int correlationType = v2d.getShownCorrelationType();
		if (correlationType != -1)
			configuration.setProperty(PROPERTY_CORRELATION_TYPE, CorrelationCalculator.TYPE_NAME[correlationType]);

		configuration.setProperty(PROPERTY_CURVE_TYPE, JVisualization2D.CURVE_MODE_CODE[v2d.getCurveMode()]);
		int list = v2d.getCurveRowList();
		configuration.setProperty(PROPERTY_CURVE_LIST, list == JVisualization2D.cCurveRowListVisible ?
				JVisualization2D.ROW_LIST_CODE_VISIBLE : list == JVisualization2D.cCurveRowListSelected ?
				JVisualization2D.ROW_LIST_CODE_SELECTED : getTableModel().getListHandler().getListName(list));
		configuration.setProperty(PROPERTY_SHOW_STDDEV_AREA, v2d.isShowStandardDeviationArea() ? "true" : "false");
		configuration.setProperty(PROPERTY_TRUNCATE_CURVE, v2d.isCurveAreaTruncated() ? "true" : "false");
		configuration.setProperty(PROPERTY_SPLIT_BY_CATEGORY, v2d.isCurveSplitByColorCategory() ? "true" : "false");
		int splitCurveColumn = v2d.getCurveSplitSecondCategoryColumn();
		if (splitCurveColumn != JVisualization.cColumnUnassigned)
			configuration.setProperty(PROPERTY_SPLIT_CURVE_COLUMN, getTableModel().getColumnTitleNoAlias(splitCurveColumn));
		configuration.setProperty(PROPERTY_CURVE_LINE_WIDTH, DoubleFormat.toString(v2d.getCurveLineWidth()));
		configuration.setProperty(PROPERTY_CURVE_SMOOTHING, DoubleFormat.toString(v2d.getCurveSmoothing()));
		String expression = v2d.getCurveExpression();
		if (expression != null)
			configuration.setProperty(PROPERTY_CURVE_FORMULA, expression);

		configuration.setProperty(PROPERTY_SHOW_BAR_OR_PIE_SIZE_VALUE, v2d.isShowBarOrPieSizeValue() ? "true" : "false");
		configuration.setProperty(PROPERTY_BOXPLOT_SHOW_STDDEV, v2d.isShowStandardDeviation() ? "true" : "false");
		configuration.setProperty(PROPERTY_BOXPLOT_SHOW_CONF_INTERVAL, v2d.isShowConfidenceInterval() ? "true" : "false");
		configuration.setProperty(PROPERTY_BOXPLOT_SHOW_VALUE_COUNT, v2d.isShowValueCount() ? "true" : "false");

		int pValueColumn = v2d.getPValueColumn();
		if (pValueColumn != JVisualization.cColumnUnassigned) {
			configuration.setProperty(PROPERTY_PVALUE_COLUMN, getTableModel().getColumnTitleNoAlias(pValueColumn));
			configuration.setProperty(PROPERTY_PVALUE_REF_CATEGORY, v2d.getPValueRefCategory());
			configuration.setProperty(PROPERTY_BOXPLOT_SHOW_PVALUE, v2d.isShowPValue() ? "true" : "false");
			configuration.setProperty(PROPERTY_BOXPLOT_SHOW_FOLDCHANGE, v2d.isShowFoldChange() ? "true" : "false");
			}

		configuration.setProperty(PROPERTY_BOXPLOT_MEAN_MODE, JVisualization.BOXPLOT_MEAN_MODE_CODE[v2d.getBoxplotMeanMode()]);
		configuration.setProperty(PROPERTY_BOXPLOT_MEAN_VALUES, v2d.isShowMeanAndMedianValues() ? "true" : "false");
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		return true;
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		if (!(view instanceof VisualizationPanel2D))
			return;

		JVisualization2D visualization = (JVisualization2D)((VisualizationPanel2D)view).getVisualization();
		visualization.setShownCorrelationType(findListIndex(configuration.getProperty(PROPERTY_CORRELATION_TYPE),
															 CorrelationCalculator.TYPE_NAME, -1));
		int mode = findListIndex(configuration.getProperty(PROPERTY_CURVE_TYPE), JVisualization2D.CURVE_MODE_CODE, 0);
		String listName = configuration.getProperty(PROPERTY_CURVE_LIST, JVisualization2D.ROW_LIST_CODE_VISIBLE);
		int rowList = listName.equals(JVisualization2D.ROW_LIST_CODE_VISIBLE) ? JVisualization2D.cCurveRowListVisible
				: listName.equals(JVisualization2D.ROW_LIST_CODE_SELECTED) ? JVisualization2D.cCurveRowListSelected
				: getTableModel().getListHandler().getListIndex(listName);
		boolean stdDev = "true".equals(configuration.getProperty(PROPERTY_SHOW_STDDEV_AREA));
		boolean truncate = "true".equals(configuration.getProperty(PROPERTY_TRUNCATE_CURVE));
		boolean split = "true".equals(configuration.getProperty(PROPERTY_SPLIT_BY_CATEGORY)) && mode != JVisualization2D.cCurveModeByFormulaShow;
		int splitCurveColumn = getTableModel().findColumn(configuration.getProperty(PROPERTY_SPLIT_CURVE_COLUMN));
		visualization.setCurveMode(mode, rowList, stdDev, truncate, split, splitCurveColumn);
		visualization.setCurveExpression(configuration.getProperty(PROPERTY_CURVE_FORMULA));
		String value = configuration.getProperty(PROPERTY_CURVE_LINE_WIDTH);
		if (value != null)
			visualization.setCurveLineWidth(Float.parseFloat(value));
		String smoothing = configuration.getProperty(PROPERTY_CURVE_SMOOTHING);
		if (smoothing != null)
			visualization.setCurveSmoothing(Float.parseFloat(smoothing));
		visualization.setShowBarOrPieSizeValue("true".equals(configuration.getProperty(PROPERTY_SHOW_BAR_OR_PIE_SIZE_VALUE)));
		visualization.setShowStandardDeviation("true".equals(configuration.getProperty(PROPERTY_BOXPLOT_SHOW_STDDEV)));
		visualization.setShowConfidenceInterval("true".equals(configuration.getProperty(PROPERTY_BOXPLOT_SHOW_CONF_INTERVAL)));
		visualization.setShowValueCount("true".equals(configuration.getProperty(PROPERTY_BOXPLOT_SHOW_VALUE_COUNT)));
		visualization.setPValueColumn(getTableModel().findColumn(configuration.getProperty(PROPERTY_PVALUE_COLUMN)), configuration.getProperty(PROPERTY_PVALUE_REF_CATEGORY));
		visualization.setShowPValue("true".equals(configuration.getProperty(PROPERTY_BOXPLOT_SHOW_PVALUE)));
		visualization.setShowFoldChange("true".equals(configuration.getProperty(PROPERTY_BOXPLOT_SHOW_FOLDCHANGE)));
		visualization.setBoxplotMeanMode(findListIndex(configuration.getProperty(PROPERTY_BOXPLOT_MEAN_MODE), JVisualization.BOXPLOT_MEAN_MODE_CODE, 0));
		visualization.setShowMeanAndMedianValues("true".equals(configuration.getProperty(PROPERTY_BOXPLOT_MEAN_VALUES)));
		}

	@Override
	public void enableItems() {
		int curveMode = mComboBoxCurveMode.getSelectedIndex();
		boolean isFormula = curveMode == JVisualization2D.cCurveModeByFormulaShow
						 || curveMode == JVisualization2D.cCurveModeByFormulaHide;
		boolean canSplitCurve = curveMode != 0 && !isFormula;
		boolean canTruncateCurve = curveMode != 0 && !isFormula && curveMode != JVisualization2D.cCurveModeSmooth;
		mComboBoxCurveList.setEnabled(canSplitCurve);
		mTextFieldFormula.setEnabled(isFormula);
		mCheckBoxShowStdDev1.setEnabled(curveMode != 0);
		mCheckBoxTruncateCurve.setEnabled(canTruncateCurve);
		mCheckBoxSplitCurvesByColor.setEnabled(canSplitCurve);
		mComboBoxSplitCurveColumn.setEnabled(canSplitCurve);
		mSliderCurveSmoothing.setEnabled(curveMode == JVisualization2D.cCurveModeSmooth);

		boolean isBoxPlot = (!hasInteractiveView()
				|| getInteractiveVisualization().getChartType() == JVisualization.cChartTypeBoxPlot
				|| getInteractiveVisualization().getChartType() == JVisualization.cChartTypeViolins);
		if (isBoxPlot) {
			mCheckBoxShowMeanValues.setEnabled(mComboBoxBoxplotMeanMode.getSelectedIndex() != 0);

			boolean needsBoxPlotCategories = mCheckBoxShowPValues.isSelected() || mCheckBoxShowFoldChange.isSelected();
			mSelectorPValueColumn.setEnabled(needsBoxPlotCategories);
			mSelectorPValueRefCategory.setEnabled(needsBoxPlotCategories);
			}
		else {
			mSelectorPValueColumn.setEnabled(false);
			mSelectorPValueRefCategory.setEnabled(false);
			}
		}

	@Override
	public void actionPerformed(ActionEvent e) {
	    if (e.getSource() == mSelectorPValueColumn) {
	    	((JComboBox)mSelectorPValueRefCategory).removeActionListener(this);
	    	String pValueColumn = (String)((JComboBox)mSelectorPValueColumn).getSelectedItem();
	    	((JComboBox)mSelectorPValueRefCategory).removeAllItems();
    		int column = getTableModel().findColumn(pValueColumn);
    		updateRenderer((JComboBox)mSelectorPValueRefCategory, column);
    		String[] categories = getTableModel().getCategoryList(column);
    		for (String category:categories)
    			((JComboBox)mSelectorPValueRefCategory).addItem(category);
	    	((JComboBox)mSelectorPValueRefCategory).addActionListener(this);
	        }

	    // super does handling of OK, Cancel, view updating, item enabling
	    super.actionPerformed(e);
		}
	}
