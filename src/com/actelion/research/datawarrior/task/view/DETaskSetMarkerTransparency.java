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
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JVisualization2D;
import com.actelion.research.table.view.VisualizationPanel;
import com.actelion.research.table.view.VisualizationPanel2D;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Properties;


public class DETaskSetMarkerTransparency extends DETaskAbstractSetViewOptions {
	public static final String TASK_NAME = "Set Marker Transparency";

	private static final String PROPERTY_TRANSPARENCY = "transparency";
	private static final String PROPERTY_LABEL_TRANSPARENCY = "labelTransparency";
	private static final String PROPERTY_LINE_TRANSPARENCY = "lineTransparency";

	private JSlider     mMarkerSlider;
	private JSlider     mLabelSlider;
	private JSlider     mLineSlider;
	private JCheckBox   mCheckBox;

    public DETaskSetMarkerTransparency(Frame owner, DEMainPane mainPane, VisualizationPanel view) {
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
		return (view instanceof VisualizationPanel2D) ? null : "Marker transparencies can only be applied to 2D-Views.";
		}

	@Override
	public OTHER_VIEWS getOtherViewMode() {
		return OTHER_VIEWS.GRAPHICAL2D;
		}

	@Override
	public JComponent createViewOptionContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap},
				{gap, TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap} };
		JPanel sp = new JPanel();
		sp.setLayout(new TableLayout(size));

		mMarkerSlider = createSlider();
		sp.add(new JLabel("Marker Transparency:"), "1,1");
		sp.add(mMarkerSlider, "3,1");

		mCheckBox = new JCheckBox("Use same transparency for labels and connections");
		mCheckBox.addActionListener(this);
		sp.add(mCheckBox, "1,3,3,3");

		mLabelSlider = createSlider();
		sp.add(new JLabel("Label Transparency:"), "1,5");
		sp.add(mLabelSlider, "3,5");

		mLineSlider = createSlider();
		sp.add(new JLabel("Connection Transparency:"), "1,7");
		sp.add(mLineSlider, "3,7");

		enableItems();

		return sp;
	    }

	private JSlider createSlider() {
    	JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
		slider.setPreferredSize(new Dimension(HiDPIHelper.scale(100), slider.getPreferredSize().height));
		slider.setMinorTickSpacing(10);
		slider.setMajorTickSpacing(100);
		slider.addChangeListener(this);
		return slider;
		}

	@Override
	public void setDialogToDefault() {
		mMarkerSlider.setValue(0);
		mLabelSlider.setValue(0);
		mLineSlider.setValue(0);
		enableItems();
	}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		float transparency = 0f;
		try {
			transparency = Float.parseFloat(configuration.getProperty(PROPERTY_TRANSPARENCY, "0"));
			}
		catch (NumberFormatException nfe) {}
		mMarkerSlider.setValue((int)(100f*transparency));

		String text1 = configuration.getProperty(PROPERTY_LABEL_TRANSPARENCY, "");
		String text2 = configuration.getProperty(PROPERTY_LINE_TRANSPARENCY, "");
		boolean useForAll = (text1.length() == 0 || text2.length() == 0);
		mCheckBox.setSelected(useForAll);
		if (!useForAll) {
			try {
				mLabelSlider.setValue((int)(100f*Float.parseFloat(text1)));
				mLineSlider.setValue((int)(100f*Float.parseFloat(text2)));
				}
			catch (NumberFormatException nfe) {}
			}

		enableItems();
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		configuration.setProperty(PROPERTY_TRANSPARENCY, ""+(0.01f*mMarkerSlider.getValue()));
		if (!mCheckBox.isSelected()) {
			configuration.setProperty(PROPERTY_LABEL_TRANSPARENCY, ""+(0.01f*mLabelSlider.getValue()));
			configuration.setProperty(PROPERTY_LINE_TRANSPARENCY, ""+(0.01f*mLineSlider.getValue()));
			}
		}

	@Override
	public void addViewConfiguration(CompoundTableView view, Properties configuration) {
		JVisualization2D visualization = (JVisualization2D)((VisualizationPanel)view).getVisualization();
		float transparency = visualization.getMarkerTransparency();
		float transparency1 = visualization.getMarkerLabelTransparency();
		float transparency2 = visualization.getConnectionLineTransparency();
		configuration.setProperty(PROPERTY_TRANSPARENCY, ""+transparency);
		if (transparency1 != transparency || transparency2 != transparency) {
			configuration.setProperty(PROPERTY_LABEL_TRANSPARENCY, ""+transparency1);
			configuration.setProperty(PROPERTY_LINE_TRANSPARENCY, ""+transparency2);
			}
		}

	@Override
	public void actionPerformed(ActionEvent e) {
    	if (e.getSource() == mCheckBox) {
    		enableItems();
    		return;
		    }

    	super.actionPerformed(e);
		}

	@Override
	public void enableItems() {
    	boolean enabled = !mCheckBox.isSelected();
    	if (!enabled) {
		    mLabelSlider.setValue(mMarkerSlider.getValue());
		    mLineSlider.setValue(mMarkerSlider.getValue());
		    }
    	mLabelSlider.setEnabled(enabled);
		mLineSlider.setEnabled(enabled);
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		return true;
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		if (view instanceof VisualizationPanel2D) {
			float transparency = 0f;
			try {
				transparency = Float.parseFloat(configuration.getProperty(PROPERTY_TRANSPARENCY, "0"));
				}
			catch (NumberFormatException nfe) {}

			float transparency1 = transparency;
			float transparency2 = transparency;

			String text1 = configuration.getProperty(PROPERTY_LABEL_TRANSPARENCY, "");
			String text2 = configuration.getProperty(PROPERTY_LINE_TRANSPARENCY, "");
			if (text1.length() != 0 && text2.length() != 0) {
				try {
					transparency1 = Float.parseFloat(text1);
					transparency2 = Float.parseFloat(text2);
					}
				catch (NumberFormatException nfe) {}
				}

			JVisualization2D visualization = (JVisualization2D)((VisualizationPanel)view).getVisualization();
			visualization.setTransparency(transparency, transparency1, transparency2);
			}
		}
	}
