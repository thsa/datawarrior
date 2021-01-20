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
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationPanel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;


public class DETaskSetMarkerJittering extends DETaskAbstractSetViewOptions {
	public static final String TASK_NAME = "Set Marker Jittering";

	private static final String PROPERTY_JITTER = "jitter";
	private static final String PROPERTY_AXES = "axes";

	private JSlider		mSlider;
	private JCheckBox	mCheckBoxX,mCheckBoxY,mCheckBoxZ;

    public DETaskSetMarkerJittering(Frame owner, DEMainPane mainPane, VisualizationPanel view) {
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
		return (view instanceof VisualizationPanel) ? null : "Marker jittering can only be applied to 2D- or 3D-Views.";
		}

	@Override
	public JComponent createViewOptionContent() {
    	int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.FILL, TableLayout.PREFERRED, TableLayout.FILL, gap},
				{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED, gap*2} };
		JPanel p = new JPanel();
		p.setLayout(new TableLayout(size));

		JPanel sp = new JPanel();
		mSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
		mSlider.setPreferredSize(new Dimension(HiDPIHelper.scale(100), mSlider.getPreferredSize().height));
		mSlider.setMinorTickSpacing(10);
		mSlider.setMajorTickSpacing(100);
		mSlider.addChangeListener(this);
		sp.add(new JLabel("none"));
		sp.add(mSlider);
		sp.add(new JLabel("max"));
		p.add(sp, "1,1,3,1");

		mCheckBoxX = new JCheckBox("Jitter in X-direction");
		mCheckBoxX.addActionListener(this);
		p.add(mCheckBoxX, "2,3");

		mCheckBoxY = new JCheckBox("Jitter in Y-direction");
		mCheckBoxY.addActionListener(this);
		p.add(mCheckBoxY, "2,4");

		if (!isInteractive() || getInteractiveVisualization().getDimensionCount() == 3) {
			mCheckBoxZ = new JCheckBox("Jitter in Z-direction");
			mCheckBoxZ.addActionListener(this);
			p.add(mCheckBoxZ, "2,5");
			}

		return p;
	    }

	@Override
	public void setDialogToDefault() {
		mSlider.setValue(0);
		mCheckBoxX.setSelected(true);
		mCheckBoxY.setSelected(true);
		if (mCheckBoxZ != null)
			mCheckBoxZ.setSelected(true);
		}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		float jitter = 0f;
		int axes = 7;
		try {
			jitter = Float.parseFloat(configuration.getProperty(PROPERTY_JITTER, "0"));
			axes = Integer.parseInt(configuration.getProperty(PROPERTY_JITTER, "7"));
			}
		catch (NumberFormatException nfe) {}

		mSlider.setValue((int)(100f*jitter));
		mCheckBoxX.setSelected((axes & 1) != 0);
		mCheckBoxY.setSelected((axes & 2) != 0);
		if (mCheckBoxZ != null)
			mCheckBoxZ.setSelected((axes & 4) != 0);
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		configuration.setProperty(PROPERTY_JITTER, ""+(0.01f*mSlider.getValue()));
		int axes = (mCheckBoxX.isSelected() ? 1 : 0)
				 + (mCheckBoxY.isSelected() ? 2 : 0)
				 + (mCheckBoxZ != null && mCheckBoxZ.isSelected() ? 4 : 0);
		configuration.setProperty(PROPERTY_AXES, Integer.toString(axes));
		}

	@Override
	public void addViewConfiguration(CompoundTableView view, Properties configuration) {
		JVisualization visualization = ((VisualizationPanel)view).getVisualization();
		configuration.setProperty(PROPERTY_JITTER, ""+ visualization.getJittering());
		configuration.setProperty(PROPERTY_AXES, ""+ visualization.getJitterAxes());
		}

	@Override
	public void enableItems() {
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		return true;
		}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		if (view instanceof VisualizationPanel) {
			JVisualization visualization = ((VisualizationPanel)view).getVisualization();
			try {
				float jitter = Float.parseFloat(configuration.getProperty(PROPERTY_JITTER, "0"));
				int axes = Integer.parseInt(configuration.getProperty(PROPERTY_AXES, "7"));
				visualization.setJittering(jitter, axes, isAdjusting);
				}
			catch (NumberFormatException nfe) {}
			}
		}
	}
