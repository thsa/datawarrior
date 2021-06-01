package com.actelion.research.datawarrior.task.view;

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JStructureGrid;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskSetHorizontalStructureCount extends DETaskAbstractSetViewOptions {
	public static final String TASK_NAME = "Set Horizontal Structure Count";
	public static final String[] COUNT_OPTIONS = { "1", "2", "3", "4", "5", "6", "7", "8", "10", "12", "14", "16" };
	private static final String DEFAULT_COUNT = "6";

	private static final String PROPERTY_COUNT = "count";

	private JComboBox mComboBox;
	private int mStructureCount;

	public DETaskSetHorizontalStructureCount(Frame owner, DEMainPane mainPane, JStructureGrid view) {
		super(owner, mainPane, view);
		mStructureCount = -1;
	}

	public DETaskSetHorizontalStructureCount(Frame owner, DEMainPane mainPane, JStructureGrid view, int structureCount) {
		super(owner, mainPane, view);
		mStructureCount = structureCount;
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return mStructureCount != -1;
	}

	@Override
	public Properties getPredefinedConfiguration() {
		Properties configuration = super.getPredefinedConfiguration();
		if (configuration != null)
			configuration.put(PROPERTY_COUNT, Integer.toString(mStructureCount));

		return configuration;
	}

	@Override
	public JComponent createViewOptionContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap} };
		JPanel cp = new JPanel();
		cp.setLayout(new TableLayout(size));

		mComboBox = new JComboBox(COUNT_OPTIONS);
		mComboBox.addItemListener(this);
		cp.add(new JLabel("Horizontal structure count:"), "1,1");
		cp.add(mComboBox, "3,1");

		return cp;
	}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof JStructureGrid) ? null : "The horizontal structure count can only be set on structure-views.";
	}

	@Override
	public void setDialogToDefault() {
		mComboBox.setSelectedItem(DEFAULT_COUNT);
	}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		String count = configuration.getProperty(PROPERTY_COUNT, DEFAULT_COUNT);
		mComboBox.setSelectedItem(count);
	}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		String count = (String)mComboBox.getSelectedItem();
		configuration.setProperty(PROPERTY_COUNT, count);
	}

	@Override
	public void addViewConfiguration(CompoundTableView view, Properties configuration) {
		int count = ((JStructureGrid)view).getColumnCount();
		configuration.setProperty(PROPERTY_COUNT, Integer.toString(count));
	}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		if (view != null) {
			try {
				Integer.parseInt(configuration.getProperty(PROPERTY_COUNT, DEFAULT_COUNT));
			} catch (NumberFormatException nfe) {
				showErrorMessage("Horizontal structure count is not numerical.");
				return false;
			}
		}

		return true;
	}

	@Override
	public void enableItems() {
	}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		if (view instanceof JStructureGrid) {
			int count = Integer.parseInt(configuration.getProperty(PROPERTY_COUNT, DEFAULT_COUNT));
			((JStructureGrid)view).setColumnCount(count);
		}
	}
}
