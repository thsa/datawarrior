package com.actelion.research.datawarrior.task.macro;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Properties;

public class DETaskIfThen extends ConfigurableTask {
	public static final String TASK_NAME = "If Then";

	private static final String PROPERTY_VALUE = "value";
	private static final String PROPERTY_INVERSE = "inverse";
	private static final String PROPERTY_MODE = "mode";
	private static final String[] MODE_TEXT = { "Column exists", "File exists" };
	private static final String[] MODE_CODE = { "column", "file" };
	private static final int MODE_COLUMN = 0;
	private static final int MODE_FILE = 1;

	private JComboBox<String> mComboBoxMode;
	private JTextField mTextFieldValue;
	private JCheckBox mCheckBoxInverse;

	public DETaskIfThen(Frame parent) {
		super(parent, true);
	}

	@Override
	public boolean isConfigurable() {
		return true;
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public JPanel createDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap>>1, TableLayout.PREFERRED, gap},
				{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED, gap} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		mComboBoxMode = new JComboBox<>(MODE_TEXT);
		content.add(new JLabel("Mode:"), "1,1");
		content.add(mComboBoxMode, "3,1");

		content.add(new JLabel("Label name:"), "1,3");
		mTextFieldValue = new JTextField(6);
		content.add(mTextFieldValue, "3,3");

		content.add(new JLabel("Inverse:"), "1,5");
		mCheckBoxInverse = new JCheckBox();
		content.add(mCheckBoxInverse, "3,5");

		return content;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_MODE, MODE_CODE[mComboBoxMode.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_VALUE, mTextFieldValue.getText().trim());
		configuration.setProperty(PROPERTY_INVERSE, mCheckBoxInverse.isSelected() ? "true" : "false");
		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mComboBoxMode.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_MODE), MODE_CODE, 0));
		mTextFieldValue.setText(configuration.getProperty(PROPERTY_VALUE, ""));
		mCheckBoxInverse.setSelected("true".equals(configuration.getProperty(PROPERTY_INVERSE)));
	}

	@Override
	public void setDialogConfigurationToDefault() {
		mComboBoxMode.setSelectedIndex(0);
		mTextFieldValue.setText("");
		mCheckBoxInverse.setSelected(false);
	}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String value = configuration.getProperty(PROPERTY_VALUE);
		if (value == null || value.isEmpty()) {
			showErrorMessage("No value defined.");
			return false;
		}

		return true;
	}

	public boolean isConditionMet(Properties configuration, DEFrame parentFrame) {
		int mode = findListIndex(configuration.getProperty(PROPERTY_MODE), MODE_CODE, 0);
		String value = configuration.getProperty(PROPERTY_VALUE);
		boolean inverse = "true".equals(configuration.getProperty(PROPERTY_INVERSE));
		if (mode == MODE_COLUMN)
			return inverse ^ (parentFrame.getTableModel().findColumn(value) != -1);
		if (mode == MODE_FILE)
			return inverse ^ (new File(value).exists());

		return true;    // should never reach this
	}

	@Override
	public void runTask(Properties configuration) {
		// the if/then is actually performed by the DEMacroRecorder
	}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
	}
}
