package com.actelion.research.datawarrior.task.table;

import com.actelion.research.datawarrior.task.AbstractSingleColumnTask;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskSetColumnDescription extends AbstractSingleColumnTask {
	public static final String TASK_NAME = "Set Column Description";

	private static final String PROPERTY_DESCRIPTION = "description";

	private JTextField  mTextAreaDescription;
	private String      mDescription;

	public DETaskSetColumnDescription(Frame owner, CompoundTableModel tableModel) {
		this(owner, tableModel, -1, null);
	}

	public DETaskSetColumnDescription(Frame owner, CompoundTableModel tableModel, int column, String description) {
		super(owner, tableModel, false, column);
		mDescription = description;
	}

	@Override
	public Properties getPredefinedConfiguration() {
		Properties configuration = super.getPredefinedConfiguration();
		if (configuration != null)
			configuration.setProperty(PROPERTY_DESCRIPTION, mDescription);
		return configuration;
	}

	@Override
	public JPanel createInnerDialogContent() {
		double[][] size = { {TableLayout.PREFERRED, 4, TableLayout.PREFERRED}, {TableLayout.PREFERRED, 16} };

		mTextAreaDescription = new JTextField(24);

		JPanel ip = new JPanel();
		ip.setLayout(new TableLayout(size));
		ip.add(new JLabel("Description:"), "0,0");
		ip.add(mTextAreaDescription, "2,0");
		return ip;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.put(PROPERTY_DESCRIPTION, mTextAreaDescription.getText());
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mTextAreaDescription.setText(configuration.getProperty(PROPERTY_DESCRIPTION, ""));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mTextAreaDescription.setText("");
		}

	@Override
	public boolean isCompatibleColumn(int column) {
		return true;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public void runTask(Properties configuration) {
		String description = configuration.getProperty(PROPERTY_DESCRIPTION);
		getTableModel().setColumnDescription(description, getColumn(configuration));
		}
	}
