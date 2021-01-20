package com.actelion.research.datawarrior.task.table;

import com.actelion.research.datawarrior.DETableView;
import com.actelion.research.datawarrior.task.AbstractMultiColumnTask;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskSetTextWrapping extends AbstractMultiColumnTask {
	public static final String TASK_NAME = "Set Text Wrapping";

	private static final String PROPERTY_WRAP = "wrap";

	private DETableView mTableView;
	private JCheckBox   mCheckBoxWrap;
	private boolean     mWrap;

	public DETaskSetTextWrapping(Frame owner, DETableView tableView) {
		this(owner, tableView, null, false);
		}

	public DETaskSetTextWrapping(Frame owner, DETableView tableView, int[] columnList, boolean wrap) {
		super(owner, tableView.getTableModel(), false, columnList);
		mTableView = tableView;
		mWrap = wrap;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		Properties configuration = super.getPredefinedConfiguration();
		if (configuration != null)
			configuration.setProperty(PROPERTY_WRAP, mWrap ? "true" : "false");
		return configuration;
		}

	@Override
	public JPanel createInnerDialogContent() {
		double[][] size = { {TableLayout.PREFERRED}, {TableLayout.PREFERRED, 16} };

		mCheckBoxWrap = new JCheckBox("Wrap text in cells");

		JPanel ip = new JPanel();
		ip.setLayout(new TableLayout(size));
		ip.add(mCheckBoxWrap, "0,0");
		return ip;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.put(PROPERTY_WRAP, mCheckBoxWrap.isSelected() ? "true" : "false");
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mCheckBoxWrap.setSelected("true".equals(configuration.getProperty(PROPERTY_WRAP)));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mCheckBoxWrap.setSelected(false);
		}

	@Override
	public boolean isCompatibleColumn(int column) {
		return getTableModel().getColumnSpecialType(column) == null;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public void runTask(Properties configuration) {
		int[] columnList = getColumnList(configuration);
		boolean wrap = "true".equals(configuration.getProperty(PROPERTY_WRAP));
		for (int column:columnList)
			mTableView.setTextWrapping(column, wrap);
		}
	}
