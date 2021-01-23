package com.actelion.research.datawarrior.task.table;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.task.AbstractSingleColumnTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskSetColumnReference extends AbstractSingleColumnTask {
	public static final String TASK_NAME = "Set Column Reference";

	private static final String PROPERTY_REFERENCED_COLUMN = "refColumn";
	private static final String PROPERTY_STRENGTH_COLUMN = "strengthColumn";
	private static final String PROPERTY_DIRECTIONALITY = "directionality"; // allowed: 'bi','mono'

	private static final String ITEM_NONE = "<none>";

	private JComboBox mComboBoxRefColumn,mComboBoxStrengthColumn;
	private JCheckBox mCheckBoxBidirectional;

	public DETaskSetColumnReference(Frame owner, CompoundTableModel tableModel, int column) {
		super(owner, tableModel, false, column);
	}

	@Override
	public Properties getPredefinedConfiguration() {
		return null;
	}

	@Override
	public JPanel createInnerDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED},
				{TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, 2*gap} };

		mComboBoxRefColumn = new JComboBox();
		mComboBoxRefColumn.addItem(ITEM_NONE);
		if (isInteractive()) {
			for (int i = 0; i<getTableModel().getTotalColumnCount(); i++)
				if (getTableModel().isColumnDisplayable(i) && getTableModel().isColumnDataUnique(i) && i != getPredefinedColumn())
					mComboBoxRefColumn.addItem(getTableModel().getColumnTitle(i));
			}
		else {
			mComboBoxRefColumn.setEditable(true);
			}

		mComboBoxStrengthColumn = new JComboBox();
		mComboBoxStrengthColumn.addItem(ITEM_NONE);
		if (isInteractive()) {
			for (int i = 0; i<getTableModel().getTotalColumnCount(); i++)
				if (getTableModel().isColumnTypeDouble(i))
					mComboBoxStrengthColumn.addItem(getTableModel().getColumnTitle(i));
			}
		else {
			mComboBoxStrengthColumn.setEditable(true);
			}

		mCheckBoxBidirectional = new JCheckBox("Links are bi-directional");

		JPanel ip = new JPanel();
		ip.setLayout(new TableLayout(size));
		ip.add(new JLabel("Referenced column:"), "0,0");
		ip.add(mComboBoxRefColumn, "2,0");
		ip.add(new JLabel("Strength column:"), "0,2");
		ip.add(mComboBoxStrengthColumn, "2,2");
		ip.add(mCheckBoxBidirectional, "0,4,2,4");
		return ip;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();

		String item = (String)mComboBoxRefColumn.getSelectedItem();
		if (item != null && item.length() != 0 && !ITEM_NONE.equals(item))
			configuration.setProperty(PROPERTY_REFERENCED_COLUMN, getTableModel().getColumnTitleNoAlias(item));

		item = (String)mComboBoxStrengthColumn.getSelectedItem();
		if (item != null && item.length() != 0 && !ITEM_NONE.equals(item))
			configuration.setProperty(PROPERTY_STRENGTH_COLUMN, getTableModel().getColumnTitleNoAlias(item));

		configuration.setProperty(PROPERTY_DIRECTIONALITY, mCheckBoxBidirectional.isSelected() ? "bi" : "mono");

		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		if (getPredefinedColumn() != -1) {
			setDialogConfigurationToCurrentState();
			}
		else {
			String refColumn = configuration.getProperty(PROPERTY_REFERENCED_COLUMN, "");
			if (refColumn.length() != 0) {
				int column = getTableModel().findColumn(refColumn);
				if (column != -1)
					refColumn = getTableModel().getColumnTitleNoAlias(column);
				}
			mComboBoxRefColumn.setSelectedItem(refColumn.length() == 0 ? ITEM_NONE : refColumn);

			String strengthColumn = configuration.getProperty(PROPERTY_STRENGTH_COLUMN, "");
			if (strengthColumn.length() != 0) {
				int column = getTableModel().findColumn(strengthColumn);
				if (column != -1)
					strengthColumn = getTableModel().getColumnTitleNoAlias(column);
				}
			mComboBoxStrengthColumn.setSelectedItem(strengthColumn.length() == 0 ? ITEM_NONE : strengthColumn);

			mCheckBoxBidirectional.setSelected("bi".equalsIgnoreCase(configuration.getProperty(PROPERTY_DIRECTIONALITY)));
			}
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		if (getPredefinedColumn() != -1) {
			setDialogConfigurationToCurrentState();
			}
		else {
			mComboBoxRefColumn.setSelectedItem(ITEM_NONE);
			mComboBoxStrengthColumn.setSelectedItem(ITEM_NONE);
			mCheckBoxBidirectional.setSelected(false);
			}
		}

	private void setDialogConfigurationToCurrentState() {
		int column = getPredefinedColumn();
		int refColumn = getTableModel().findColumn(getTableModel().getColumnProperty(column, CompoundTableConstants.cColumnPropertyReferencedColumn));
		mComboBoxRefColumn.setSelectedItem(refColumn == -1 ? ITEM_NONE : getTableModel().getColumnTitle(refColumn));
		int strengthColumn = getTableModel().findColumn(getTableModel().getColumnProperty(column, CompoundTableConstants.cColumnPropertyReferenceStrengthColumn));
		mComboBoxStrengthColumn.setSelectedItem(strengthColumn == -1 ? ITEM_NONE : getTableModel().getColumnTitle(strengthColumn));
		mCheckBoxBidirectional.setSelected(CompoundTableConstants.cColumnPropertyReferenceTypeRedundant.equals(getTableModel().getColumnProperty(column, CompoundTableConstants.cColumnPropertyReferenceType)));
		}

	@Override
	public boolean isCompatibleColumn(int column) {
		return getTableModel().isColumnDisplayable(column);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (!super.isConfigurationValid(configuration, isLive))
			return false;

		if (isLive) {
			String refTitle = configuration.getProperty(PROPERTY_REFERENCED_COLUMN);
			if (refTitle != null) {
				int refColumn = getTableModel().findColumn(refTitle);
				if (refColumn == -1) {
					showErrorMessage("Referenced column '"+refTitle+"' not found.");
					return false;
					}
				if (!getTableModel().isColumnDisplayable(refColumn) || !getTableModel().isColumnDataUnique(refColumn)) {
					showErrorMessage("Referenced column '"+refTitle+"' doesn't contain unique values.");
					return false;
					}

				String strengthTitle = configuration.getProperty(PROPERTY_STRENGTH_COLUMN);
				if (strengthTitle != null) {
					int strengthColumn = getTableModel().findColumn(strengthTitle);
					if (strengthColumn == -1) {
						showErrorMessage("Referenced column '"+strengthTitle+"' not found.");
						return false;
						}
					if (!getTableModel().isColumnTypeDouble(strengthColumn)) {
						showErrorMessage("Strength column '"+strengthTitle+"' doesn't contain numerical values.");
						return false;
						}
					}
				}
			}

		return true;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/data.html#Graph";
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public void runTask(Properties configuration) {
		int column = super.getColumn(configuration);
		int refColumn = getTableModel().findColumn(configuration.getProperty(PROPERTY_REFERENCED_COLUMN, ""));
		int strengthColumn = getTableModel().findColumn(configuration.getProperty(PROPERTY_STRENGTH_COLUMN, ""));
		getTableModel().setColumnReference(column, refColumn, strengthColumn,
				"bi".equals(configuration.getProperty(PROPERTY_DIRECTIONALITY)));
		}
	}
