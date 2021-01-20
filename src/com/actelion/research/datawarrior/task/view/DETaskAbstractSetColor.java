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

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableListHandler;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationColor;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Properties;


public abstract class DETaskAbstractSetColor extends DETaskAbstractSetViewOptions implements KeyListener {
	private static final String PROPERTY_COLOR_BY = "colorBy";
	private static final String PROPERTY_MODE = "mode";
	private static final String PROPERTY_COLOR_LIST = "colorList";
	private static final String PROPERTY_MINIMUM = "min";
	private static final String PROPERTY_MAXIMUM = "max";

	private static final String COLOR_DELIMITER = ";";

	private static final int DEFAULT_CATEGORY_COUNT = 12;

	private MarkerColorPanel	mColorPanel;
	private JComboBox mComboBoxColorByColumn;
	private JCheckBox			mCheckBoxByCategories;
	private JTextField			mTextFieldMin,mTextFieldMax;
	private String				mDialogTitle;

	public DETaskAbstractSetColor(Frame owner,
								DEMainPane mainPane,
								CompoundTableView view,
								String title) {
		super(owner, mainPane, view);
		mDialogTitle = title;
		}

	/**
	 * @return the associated VisualizationColor (if interactive) or null, if is editing/running a macro
	 */
	public abstract VisualizationColor getVisualizationColor(CompoundTableView view);

	/**
	 * Tries to identify the VisualizationColor that matches the configuration.
	 * Calls showErrorMessage(), if the VisualizationColor could not be identified.
	 * @return the VisualizationColor referred by the configuration or null
	 */
	public abstract VisualizationColor getVisualizationColor(CompoundTableView view, Properties configuration);

	@Override
	public String getDialogTitle() {
		return mDialogTitle;
		}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return false;
		}

	@Override
	public JComponent createViewOptionContent() {
		int gap = HiDPIHelper.scale(8);
		double size[][] = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap},
				 			{gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap,
								TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap,
								TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };

		JPanel p = new JPanel();
		p.setLayout(new TableLayout(size));

		mComboBoxColorByColumn = new JComboBox();
		p.add(new JLabel("Color by:"), "1,1");
		p.add(mComboBoxColorByColumn, "3,1");

		mColorPanel = new MarkerColorPanel(getParentFrame(), getTableModel(), this, false);
		p.add(mColorPanel, "1,5,3,5");

		mCheckBoxByCategories = new JCheckBox("Color by categories");
		mCheckBoxByCategories.setHorizontalAlignment(SwingConstants.CENTER);
		mCheckBoxByCategories.addActionListener(this);
		p.add(mCheckBoxByCategories, "1,7,3,7");

		mTextFieldMin = new JTextField(4);
		mTextFieldMin.addKeyListener(this);
		mTextFieldMax = new JTextField(4);
		mTextFieldMax.addKeyListener(this);
		JPanel rangepanel = new JPanel();
		rangepanel.add(new JLabel("Set color range from"));
		rangepanel.add(mTextFieldMin);
		rangepanel.add(new JLabel("to"));
		rangepanel.add(mTextFieldMax);
		p.add(rangepanel, "1,9,3,9");

		mComboBoxColorByColumn.addItem(getTableModel().getColumnTitleExtended(-1));
		for (int i=0; i<getTableModel().getTotalColumnCount(); i++) {
			if ((getTableModel().isColumnTypeCategory(i)
			  && getTableModel().getCategoryCount(i) <= VisualizationColor.cMaxColorCategories)
			 || getTableModel().isColumnTypeDouble(i)) {
				mComboBoxColorByColumn.addItem(getTableModel().getColumnTitleExtended(i));
				}
			}
		for (int i=0; i<getTableModel().getTotalColumnCount(); i++) {
			if (getTableModel().isDescriptorColumn(i)) {
				mComboBoxColorByColumn.addItem(getTableModel().getColumnTitleExtended(i));
				}
			}
		for (int i = 0; i<getTableModel().getListHandler().getListCount(); i++) {
			int pseudoColumn = CompoundTableListHandler.getColumnFromList(i);
			mComboBoxColorByColumn.addItem(getTableModel().getColumnTitleExtended(pseudoColumn));
			}
		mComboBoxColorByColumn.setEditable(!hasInteractiveView());
		mComboBoxColorByColumn.addItemListener(this);

		return p;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mCheckBoxByCategories) {
			updateColorPanel();
			}

		super.actionPerformed(e);
		}

	/**
	 * Based on the combobox setting (column) and the checkbox setting (categories of numerical)
	 * it updates the colorpanel to show the correct number of categories or the numerical color button.
	 */
	protected void updateColorPanel() {
		int column = getTableModel().findColumn((String) mComboBoxColorByColumn.getSelectedItem());
		int categoryCount = !mCheckBoxByCategories.isSelected() ? -1 : (column == -1) ? DEFAULT_CATEGORY_COUNT : getTableModel().getCategoryCount(column);
		mColorPanel.updateColorListMode(categoryCount);
		}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == mComboBoxColorByColumn && e.getStateChange() == ItemEvent.SELECTED) {
			int column = getTableModel().findColumn((String) mComboBoxColorByColumn.getSelectedItem());
			updateCheckbox(column);

			// if the user changed to flexophore coloring, he may cancel the similarity calculation
			// which results in setting the colorColumn to unassigned. In this case we close the dialog.
//			if (mVisualizationColor.getColorColumn() != column) {
//				setVisible(false);
//				dispose();
// TODO			}

			mTextFieldMin.setText("");
			mTextFieldMax.setText("");
			}

		super.itemStateChanged(e);
		}

	private void updateCheckbox(int column) {
		boolean enabled = false;
		boolean selected = false;
		if (!hasInteractiveView()) {
			enabled = true;
			}
		else if (CompoundTableListHandler.isListColumn(column)) {
			selected = true;
			}
		else if (column != JVisualization.cColumnUnassigned
			  && !getTableModel().isDescriptorColumn(column)) {
			if (getTableModel().isColumnTypeCategory(column)
				  && (getTableModel().isColumnTypeDouble(column) || getTableModel().isColumnTypeRangeCategory(column))
				  && getTableModel().getCategoryCount(column) <= VisualizationColor.cMaxColorCategories) {
				enabled = true;
				selected = !getTableModel().isColumnTypeDouble(column);
				}
			else if (!getTableModel().isColumnTypeDouble(column)) {
				selected = true;
				}
			}
		boolean changed = (mCheckBoxByCategories.isSelected() != selected);
		mCheckBoxByCategories.setEnabled(enabled);
		mCheckBoxByCategories.setSelected(selected);
		if (changed
		 || (selected && mColorPanel.getCategoryCount() != getTableModel().getCategoryCount(column)))
			updateColorPanel();
		}

	@Override public void keyPressed(KeyEvent arg0) {}
	@Override public void keyTyped(KeyEvent arg0) {}
	@Override public void keyReleased(KeyEvent arg0) {
		updateColorRange();
		}

	private void updateColorRange() {
		float min = Float.NaN;
		float max = Float.NaN;
		int column = getTableModel().findColumn((String) mComboBoxColorByColumn.getSelectedItem());
		boolean isLogarithmic = (column == -1) ? false : getTableModel().isLogarithmicViewMode(column);
		try {
			if (mTextFieldMin.getText().length() != 0)
				min = Float.parseFloat(mTextFieldMin.getText());
			mTextFieldMin.setBackground(UIManager.getColor("TextArea.background"));
			}
		catch (NumberFormatException nfe) {
			mTextFieldMin.setBackground(Color.red);
			}
		try {
			if (mTextFieldMax.getText().length() != 0)
				max = Float.parseFloat(mTextFieldMax.getText());
			mTextFieldMax.setBackground(UIManager.getColor("TextArea.background"));
			}
		catch (NumberFormatException nfe) {
			mTextFieldMax.setBackground(Color.red);
			}
		if (min >= max) {
			mTextFieldMin.setBackground(Color.red);
			mTextFieldMax.setBackground(Color.red);
			}
		else if (column != -1 && isLogarithmic && min <= 0) {
			mTextFieldMin.setBackground(Color.red);
			}
		else if (column != -1 && isLogarithmic && max <= 0) {
			mTextFieldMax.setBackground(Color.red);
			}
		else if (hasInteractiveView()) {
			getVisualizationColor(getInteractiveView()).setColorRange(min, max);
			}
		}

	@Override
	public void enableItems() {
		int column = getTableModel().findColumn((String) mComboBoxColorByColumn.getSelectedItem());
		boolean enabled = !hasInteractiveView()
				|| (column != JVisualization.cColumnUnassigned
				 && !mCheckBoxByCategories.isSelected()
				 && !getTableModel().isColumnTypeDate(column)
				 && !getTableModel().isColumnTypeRangeCategory(column));
		mTextFieldMin.setEnabled(enabled);
		mTextFieldMax.setEnabled(enabled);
		}

	@Override
	public void setDialogToDefault() {
		mComboBoxColorByColumn.setSelectedIndex(0);
		}

	public void setColorByColumn(int column) {
		mComboBoxColorByColumn.setSelectedItem(getTableModel().getColumnTitleExtended(column));
		updateCheckbox(column);
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		String columnName = configuration.getProperty(PROPERTY_COLOR_BY, CompoundTableModel.cColumnUnassignedCode);
		int column = getTableModel().findColumn(columnName);
		mComboBoxColorByColumn.setSelectedItem(!hasInteractiveView() && column == -1 ? columnName : getTableModel().getColumnTitleExtended(column));
		updateCheckbox(column);

		int mode = findListIndex(configuration.getProperty(PROPERTY_MODE), VisualizationColor.COLOR_LIST_MODE_CODE, VisualizationColor.cColorListModeHSBLong);
		mCheckBoxByCategories.setSelected(mode == VisualizationColor.cColorListModeCategories);

		Color[] colorList = decodeColorList(configuration, mode);
		int categoryCount = (mode == VisualizationColor.cColorListModeCategories && getTableModel().isColumnTypeCategory(column)) ?
				getTableModel().getCategoryCount(column) : -1;
		mColorPanel.setColor(colorList, mode, categoryCount);

		mTextFieldMin.setText(configuration.getProperty(PROPERTY_MINIMUM, ""));
		mTextFieldMax.setText(configuration.getProperty(PROPERTY_MAXIMUM, ""));
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		configuration.setProperty(PROPERTY_COLOR_BY, getTableModel().getColumnTitleNoAlias((String) mComboBoxColorByColumn.getSelectedItem()));
		configuration.setProperty(PROPERTY_MODE, VisualizationColor.COLOR_LIST_MODE_CODE[mColorPanel.getColorListMode()]);
		configuration.setProperty(PROPERTY_COLOR_LIST, encodeColorList(mColorPanel.getColorList(), mColorPanel.getColorListMode()));
		if (mTextFieldMin.isEnabled() && mTextFieldMin.getText().length() != 0)
			try { configuration.setProperty(PROPERTY_MINIMUM, ""+Float.parseFloat(mTextFieldMin.getText())); } catch (NumberFormatException nfe) {}
		if (mTextFieldMax.isEnabled() && mTextFieldMax.getText().length() != 0)
			try { configuration.setProperty(PROPERTY_MAXIMUM, ""+Float.parseFloat(mTextFieldMax.getText())); } catch (NumberFormatException nfe) {}
		}

	@Override
	public void addViewConfiguration(CompoundTableView view, Properties configuration) {
		VisualizationColor vc = getVisualizationColor(view);
		int mode = vc.getColorListMode();
		configuration.setProperty(PROPERTY_COLOR_BY, getTableModel().getColumnTitleNoAlias(vc.getColorColumn()));
		configuration.setProperty(PROPERTY_MODE, VisualizationColor.COLOR_LIST_MODE_CODE[mode]);
		configuration.setProperty(PROPERTY_COLOR_LIST, encodeColorList(vc.getColorListWithoutDefaults(), mode));
		if (!Float.isNaN(vc.getColorMin()))
			configuration.setProperty(PROPERTY_MINIMUM, ""+vc.getColorMin());
		if (!Float.isNaN(vc.getColorMax()))
			configuration.setProperty(PROPERTY_MAXIMUM, ""+vc.getColorMax());
		}

	private String encodeColorList(Color[] colorList, int colorListMode) {
		if (colorList == null)
			return "";
		StringBuilder sb = new StringBuilder(""+colorList[0].getRGB());
		if (colorListMode == VisualizationColor.cColorListModeCategories)
			for (int i=1; i<colorList.length; i++)
				sb.append(COLOR_DELIMITER+colorList[i].getRGB());
		else
			sb.append(COLOR_DELIMITER+colorList[colorList.length-1].getRGB());

		return sb.toString();
		}

	private Color[] decodeColorList(Properties configuration, int colorListMode) {
		String colorListString = configuration.getProperty(PROPERTY_COLOR_LIST, "");
		if (colorListString.length() == 0)
			return null;
		String[] color = colorListString.split(COLOR_DELIMITER);
		Color[] colorList = new Color[color.length];
		for (int i=0; i<color.length; i++)
			try { colorList[i] = Color.decode(color[i]); } catch (NumberFormatException nfe) {}
		if (colorListMode != VisualizationColor.cColorListModeCategories)
			colorList = VisualizationColor.createColorWedge(colorList[0], colorList[1], colorListMode, null);
		return colorList;
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		String columnName = configuration.getProperty(PROPERTY_COLOR_BY);
		if (!CompoundTableModel.cColumnUnassignedCode.equals(columnName)) {
			int column = view.getTableModel().findColumn(columnName);
			if (column == -1) {
				showErrorMessage("Column '"+columnName+"' not found.");
				return false;
				}
			String mode = configuration.getProperty(PROPERTY_MODE);
			if (VisualizationColor.COLOR_LIST_MODE_CODE[VisualizationColor.cColorListModeCategories].equals(mode)) {
				if (!CompoundTableListHandler.isListColumn(column)
				 && !view.getTableModel().isColumnTypeCategory(column)) {
					showErrorMessage("Column '"+columnName+"' does not contain categories.");
					return false;
					}
				}
			else {
				if (!view.getTableModel().isColumnTypeDouble(column)
				 && !view.getTableModel().isColumnTypeRangeCategory(column)
				 && !view.getTableModel().isDescriptorColumn(column)) {
					showErrorMessage("Column '"+columnName+"' does not contain numerical values.");
					return false;
					}
				}
			}

		// getVisualizationColor() may create a VisualizationColor with preassigned default column, which must not happen,
		// if the configuration is invalid. Therefore, this must be the last check in isViewConfigurationValid()
		if (view != null && getVisualizationColor(view, configuration) == null)
			return false;

		return true;
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		VisualizationColor vc = getVisualizationColor(view, configuration);
		if (vc != null) {
			int column = view.getTableModel().findColumn(configuration.getProperty(PROPERTY_COLOR_BY));
			int mode = findListIndex(configuration.getProperty(PROPERTY_MODE), VisualizationColor.COLOR_LIST_MODE_CODE, VisualizationColor.cColorListModeHSBLong);
			Color[] colorList = decodeColorList(configuration, mode);

			vc.setColor(column, colorList, mode);

			float min = Float.NaN;
			String value = configuration.getProperty(PROPERTY_MINIMUM);
			if (value != null)
				try { min = Float.parseFloat(value); } catch (NumberFormatException nfe) {}
			float max = Float.NaN;
			value = configuration.getProperty(PROPERTY_MAXIMUM);
			if (value != null)
				try { max = Float.parseFloat(value); } catch (NumberFormatException nfe) {}

			vc.setColorRange(min, max);
			}
		}
	}
