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
import com.actelion.research.table.view.VisualizationPanel;
import com.actelion.research.table.view.VisualizationPanel2D;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Properties;


public class DETaskSetMarkerSize extends DETaskAbstractSetViewOptions implements KeyListener {
	public static final String TASK_NAME = "Set Marker Size";

	private static final String PROPERTY_COLUMN = "column";
	private static final String PROPERTY_SIZE = "size";
	private static final String PROPERTY_MINIMUM = "min";
	private static final String PROPERTY_MAXIMUM = "max";
	private static final String PROPERTY_INVERSE = "inverse";
	private static final String PROPERTY_ADAPTIVE = "adaptive";
	private static final String PROPERTY_PROPORTIONAL = "proportional";

	private JSlider         mSlider;
    private JComboBox		mComboBox;
    private JCheckBox		mCheckBoxProportional,mCheckBoxInverse,mCheckBoxAdaptive;
	private JTextField		mTextFieldMin,mTextFieldMax;

    public DETaskSetMarkerSize(Frame owner, DEMainPane mainPane, VisualizationPanel view) {
		super(owner, mainPane, view);
    	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return false;
		}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof VisualizationPanel) ? null : "Marker sizes can only be applied to 2D- or 3D-Views.";
		}

	@Override
	public OTHER_VIEWS getOtherViewMode() {
		return OTHER_VIEWS.GRAPHICAL;
		}

	@Override
	public JComponent createViewOptionContent() {
    	int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.FILL, TableLayout.PREFERRED, TableLayout.FILL, gap},
							{gap, TableLayout.PREFERRED, TableLayout.PREFERRED,
							 gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED,
								TableLayout.PREFERRED, TableLayout.PREFERRED, gap} };
		JPanel p = new JPanel();
		p.setLayout(new TableLayout(size));

		JPanel sp = new JPanel();
		mSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
//		mSlider.setPreferredSize(new Dimension(HiDPIHelper.scale(120), HiDPIHelper.scale(20)));
//		mSlider.setMinorTickSpacing(10);
//		mSlider.setMajorTickSpacing(100);
		mSlider.addChangeListener(this);
		sp.add(new JLabel("small"));
		sp.add(mSlider);
		sp.add(new JLabel("large"));
		p.add(sp, "1,1,3,1");

		JPanel cp = new JPanel();
		mComboBox = new JComboBox();
		mComboBox.addItem(getTableModel().getColumnTitleExtended(-1));
		for (int i=0; i<getTableModel().getTotalColumnCount(); i++)
			if (getTableModel().isColumnTypeDouble(i))
				mComboBox.addItem(getTableModel().getColumnTitleExtended(i));
		for (int i=0; i<getTableModel().getTotalColumnCount(); i++)
			if (getTableModel().isDescriptorColumn(i))
				mComboBox.addItem(getTableModel().getColumnTitleExtended(i));
		for (int i = 0; i<getTableModel().getListHandler().getListCount(); i++)
			mComboBox.addItem(getTableModel().getColumnTitleExtended(CompoundTableListHandler.getColumnFromList(i)));
        mComboBox.setEditable(!hasInteractiveView());
		mComboBox.addItemListener(this);
		if (hasInteractiveView()) {
			JVisualization visualization = ((VisualizationPanel)getInteractiveView()).getVisualization();
			if (visualization.getChartType() == JVisualization.cChartTypePies)
				mComboBox.setEnabled(false);
			}
		cp.add(new JLabel("Size by: "));
		cp.add(mComboBox);
		p.add(cp, "1,2,3,2");

		mTextFieldMin = new JTextField(6);
		mTextFieldMin.addKeyListener(this);
		mTextFieldMax = new JTextField(6);
		mTextFieldMax.addKeyListener(this);
		JPanel rangepanel = new JPanel();
		rangepanel.add(new JLabel("Set size range from"));
		rangepanel.add(mTextFieldMin);
		rangepanel.add(new JLabel("to"));
		rangepanel.add(mTextFieldMax);
		p.add(rangepanel, "1,4,3,4");

		mCheckBoxProportional = new JCheckBox("Strictly proportional");
		mCheckBoxProportional.addActionListener(this);
		p.add(mCheckBoxProportional, "2,6");

		mCheckBoxInverse = new JCheckBox("Invert sizes");
		mCheckBoxInverse.addActionListener(this);
		p.add(mCheckBoxInverse, "2,7");

		mCheckBoxAdaptive = new JCheckBox("Adapt size to zoom state");
		mCheckBoxAdaptive.addActionListener(this);
		p.add(mCheckBoxAdaptive, "2,8");

		return p;
	    }

	@Override public void keyPressed(KeyEvent arg0) {}
	@Override public void keyTyped(KeyEvent arg0) {}
	@Override public void keyReleased(KeyEvent arg0) {
		updateColorRange();
	}

	private void updateColorRange() {
		float min = Float.NaN;
		float max = Float.NaN;
		int column = getTableModel().findColumn((String) mComboBox.getSelectedItem());
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
		else if (column > 0 && isLogarithmic && min <= 0) {
			mTextFieldMin.setBackground(Color.red);
			}
		else if (column > 0 && isLogarithmic && max <= 0) {
			mTextFieldMax.setBackground(Color.red);
			}
		else if (hasInteractiveView() && column > 0) {
			((VisualizationPanel2D)getInteractiveView()).getVisualization().setMarkerSizeColumn(column, min, max);
			}
		}

	@Override
	public void setDialogToDefault() {
        mComboBox.setSelectedIndex(0);
		mSlider.setValue(50);
		mCheckBoxProportional.setSelected(false);
		mCheckBoxInverse.setSelected(false);
		mCheckBoxAdaptive.setSelected(true);
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		String columnName = configuration.getProperty(PROPERTY_COLUMN, CompoundTableModel.cColumnUnassignedCode);
		int column = getTableModel().findColumn(columnName);
		mComboBox.setSelectedItem(!hasInteractiveView() && column == -1 ? columnName : getTableModel().getColumnTitleExtended(column));

		float size = 0.5f;
		try {
			size = Float.parseFloat(configuration.getProperty(PROPERTY_SIZE, "0.5"));
			}
		catch (NumberFormatException nfe) {}
		mSlider.setValue((int)(50.0*Math.sqrt(size)));

		mTextFieldMin.setText(configuration.getProperty(PROPERTY_MINIMUM, ""));
		mTextFieldMax.setText(configuration.getProperty(PROPERTY_MAXIMUM, ""));

		mCheckBoxProportional.setSelected("true".equals(configuration.getProperty(PROPERTY_PROPORTIONAL, "true")));
		mCheckBoxInverse.setSelected("true".equals(configuration.getProperty(PROPERTY_INVERSE)));
		mCheckBoxAdaptive.setSelected("true".equals(configuration.getProperty(PROPERTY_ADAPTIVE)));
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		configuration.setProperty(PROPERTY_COLUMN, getTableModel().getColumnTitleNoAlias((String)mComboBox.getSelectedItem()));
		configuration.setProperty(PROPERTY_SIZE, ""+(mSlider.getValue()*mSlider.getValue()/2500f));
		if (mTextFieldMin.isEnabled() && mTextFieldMin.getText().length() != 0)
			try { configuration.setProperty(PROPERTY_MINIMUM, ""+Float.parseFloat(mTextFieldMin.getText())); } catch (NumberFormatException nfe) {}
		if (mTextFieldMax.isEnabled() && mTextFieldMax.getText().length() != 0)
			try { configuration.setProperty(PROPERTY_MAXIMUM, ""+Float.parseFloat(mTextFieldMax.getText())); } catch (NumberFormatException nfe) {}
		configuration.setProperty(PROPERTY_PROPORTIONAL, mCheckBoxProportional.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_INVERSE, mCheckBoxInverse.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_ADAPTIVE, mCheckBoxAdaptive.isSelected() ? "true" : "false");
		}

	@Override
	public void addViewConfiguration(CompoundTableView view, Properties configuration) {
		JVisualization visualization = ((VisualizationPanel)view).getVisualization();
		configuration.setProperty(PROPERTY_COLUMN, getTableModel().getColumnTitleNoAlias(visualization.getMarkerSizeColumn()));
		configuration.setProperty(PROPERTY_SIZE, ""+ visualization.getMarkerSize());
		if (!Float.isNaN(visualization.getMarkerSizeMin()))
			configuration.setProperty(PROPERTY_MINIMUM, ""+visualization.getMarkerSizeMin());
		if (!Float.isNaN(visualization.getMarkerSizeMax()))
			configuration.setProperty(PROPERTY_MAXIMUM, ""+visualization.getMarkerSizeMax());
		configuration.setProperty(PROPERTY_PROPORTIONAL, visualization.getMarkerSizeProportional() ? "true" : "false");
		configuration.setProperty(PROPERTY_INVERSE, visualization.getMarkerSizeInversion() ? "true" : "false");
		configuration.setProperty(PROPERTY_ADAPTIVE, visualization.isMarkerSizeZoomAdapted() ? "true" : "false");
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		if (view != null) {
			String columnName = configuration.getProperty(PROPERTY_COLUMN);
			if (!CompoundTableModel.cColumnUnassignedCode.equals(columnName)) {
				int column = getTableModel().findColumn(columnName);
				if (column == -1) {
					showErrorMessage("Column '"+columnName+"' not found.");
					return false;
					}
				if (!getTableModel().isColumnTypeDouble(column)
				 && !getTableModel().isDescriptorColumn(column)
				 && !CompoundTableListHandler.isListColumn(column)) {
					showErrorMessage("Column '"+columnName+"' does not contain numerical values.");
					return false;
					}
				}
			}

		return true;
		}

	@Override
	public void enableItems() {
    	boolean enabled = (mComboBox.getSelectedIndex() != 0);
		mCheckBoxProportional.setEnabled(enabled);
		mCheckBoxInverse.setEnabled(enabled);
		mTextFieldMin.setEnabled(enabled && !mCheckBoxProportional.isSelected());
		mTextFieldMax.setEnabled(enabled && !mCheckBoxProportional.isSelected());
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		if (view instanceof VisualizationPanel) {
			float size = 1.0f;
			try {
				size = Float.parseFloat(configuration.getProperty(PROPERTY_SIZE, "1.0"));
				}
			catch (NumberFormatException nfe) {}

			int column = getTableModel().findColumn(configuration.getProperty(PROPERTY_COLUMN, CompoundTableModel.cColumnUnassignedCode));

			float min = Float.NaN;
			String value = configuration.getProperty(PROPERTY_MINIMUM);
			if (value != null)
				try { min = Float.parseFloat(value); } catch (NumberFormatException nfe) {}
			float max = Float.NaN;
			value = configuration.getProperty(PROPERTY_MAXIMUM);
			if (value != null)
				try { max = Float.parseFloat(value); } catch (NumberFormatException nfe) {}

			((VisualizationPanel)view).getVisualization().setMarkerSizeColumn(column, min, max);
			((VisualizationPanel)view).getVisualization().setMarkerSize(size, isAdjusting);
			((VisualizationPanel)view).getVisualization().setMarkerSizeProportional("true".equals(configuration.getProperty(PROPERTY_PROPORTIONAL, "true")));
			((VisualizationPanel)view).getVisualization().setMarkerSizeInversion("true".equals(configuration.getProperty(PROPERTY_INVERSE)));
			((VisualizationPanel)view).getVisualization().setMarkerSizeZoomAdaption("true".equals(configuration.getProperty(PROPERTY_ADAPTIVE)));
			}
		}
	}
