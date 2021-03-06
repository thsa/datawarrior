package com.actelion.research.datawarrior.task.table;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.task.AbstractSingleColumnTask;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskSetStructureHiliteMode extends AbstractSingleColumnTask {
	public static final String TASK_NAME = "Set Structure Hilite Mode";

	private static final String PROPERTY_MODE = "hiliteMode";

	private JComboBox   mComboBox;
	private int         mHiliteMode;

	public DETaskSetStructureHiliteMode(Frame owner, CompoundTableModel tableModel) {
		this(owner, tableModel, -1, -1);
		}

	public DETaskSetStructureHiliteMode(Frame owner, CompoundTableModel tableModel, int column, int mode) {
		super(owner, tableModel, false, column);
		mHiliteMode = mode;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		Properties configuration = super.getPredefinedConfiguration();
		if (configuration != null)
			configuration.setProperty(PROPERTY_MODE, CompoundTableConstants.cStructureHiliteModeCode[mHiliteMode]);
		return configuration;
		}

	@Override
	public JPanel createInnerDialogContent() {
		double[][] size = { {TableLayout.PREFERRED, 4, TableLayout.PREFERRED}, {TableLayout.PREFERRED, 16} };

		mComboBox = new JComboBox(CompoundTableConstants.cStructureHiliteModeText);

		JPanel ip = new JPanel();
		ip.setLayout(new TableLayout(size));
		ip.add(new JLabel("Highlight structure by:"), "0,0");
		ip.add(mComboBox, "2,0");
		return ip;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.put(PROPERTY_MODE, CompoundTableConstants.cStructureHiliteModeCode[mComboBox.getSelectedIndex()]);
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mComboBox.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_MODE), CompoundTableConstants.cStructureHiliteModeCode, 0));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mComboBox.setSelectedIndex(0);
		}

	@Override
	public boolean isCompatibleColumn(int column) {
		return getTableModel().isColumnTypeStructure(column);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public void runTask(Properties configuration) {
		int hiliteMode = findListIndex(configuration.getProperty(PROPERTY_MODE), CompoundTableConstants.cStructureHiliteModeCode, 0);
		getTableModel().setHiliteMode(getColumn(configuration), hiliteMode);
		}
}
