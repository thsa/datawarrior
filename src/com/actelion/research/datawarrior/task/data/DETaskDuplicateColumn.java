package com.actelion.research.datawarrior.task.data;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.AbstractSingleColumnTask;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskDuplicateColumn extends AbstractSingleColumnTask {
	public static final String TASK_NAME = "Duplicate Column";

	private static final String PROPERTY_NEW_COLUMN_NAME = "newColumn";

	private JTextField mTextFieldNewColumnName;

	public DETaskDuplicateColumn(Frame parentFrame, CompoundTableModel tableModel, int column) {
		super(parentFrame, tableModel, true, column);
		}

	@Override
	public boolean isCompatibleColumn(int column) {
		return getTableModel().isColumnDisplayable(column);
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public Properties getPredefinedConfiguration() {
		return null;	// always show dialog to allow to define a new column name
		}

		@Override
	public JPanel createInnerDialogContent() {
		double[][] size = { {TableLayout.PREFERRED, 4, TableLayout.PREFERRED},
							{8, TableLayout.PREFERRED, 8} };
		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		mTextFieldNewColumnName = new JTextField(16);
		content.add(new JLabel("New column name:"), "0,1");
		content.add(mTextFieldNewColumnName, "2,1");

		return content;
		}

	@Override
	public void columnChanged(int column) {
		if (mTextFieldNewColumnName != null)
			mTextFieldNewColumnName.setText(column == -1 ? "Column Name" : "Copy of "+getTableModel().getColumnTitle(column));
		}

	@Override
	public String getColumnLabelText() {
		return "Column to be duplicated:";
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_NEW_COLUMN_NAME, mTextFieldNewColumnName.getText());
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mTextFieldNewColumnName.setText(getPredefinedColumn() == -1 ?
				configuration.getProperty(PROPERTY_NEW_COLUMN_NAME, "") : "Copy of "+getSelectedColumnName());
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mTextFieldNewColumnName.setText(getPredefinedColumn() == -1 ? "" : "Copy of "+getSelectedColumnName());
		}

	@Override
	public void runTask(Properties configuration) {
		int sourceColumn = getColumn(configuration);

/*		int childColumnCount = 0;	currently no special columns allowed
		for (int column=0; column<getTableModel().getTotalColumnCount(); column++)
			if (column != sourceColumn && getTableModel().getParentColumn(column) == sourceColumn)
				childColumnCount++;
*/
		int newColumn = getTableModel().addNewColumns(1);
		getTableModel().setColumnName(configuration.getProperty(PROPERTY_NEW_COLUMN_NAME), newColumn);
		getTableModel().getColumnProperties(newColumn).putAll(getTableModel().getColumnProperties(sourceColumn));

		startProgress("Copying column...", 0, 0);
		for (int row=0; row<getTableModel().getTotalRowCount() && !threadMustDie(); row++) {
			String value = getTableModel().getTotalValueAt(row, sourceColumn);
			getTableModel().setTotalValueAt(value, row, newColumn);
			}

		getTableModel().finalizeNewColumns(newColumn, this);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
	}
}

