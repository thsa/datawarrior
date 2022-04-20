package com.actelion.research.datawarrior.task.macro;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.datawarrior.task.DEMacroRecorder;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Properties;

/**
 * Created by thomas on 12/8/16.
 */
public class DETaskDefineVariable extends ConfigurableTask {
	public static final String TASK_NAME = "Define Variable";

	private static final String PROPERTY_NAME = "name";
	private static final String PROPERTY_VALUE = "value";
	private static final String PROPERTY_MESSAGE = "message";
	private static final String PROPERTY_OPTIONS = "options";
	private static final String PROPERTY_IS_PASSWORD = "password";

	private JTextField mTextFieldName,mTextFieldValue, mTextFieldMessage,mTextFieldOptions;
	private JCheckBox mCheckBoxIsPassword;

	public DETaskDefineVariable(Frame parent) {
		super(parent, false);
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
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, 3*gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED,
							 3*gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED,
							 3*gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		content.add(new JLabel("Variable Name:"), "1,1");
		mTextFieldName = new JTextField(12);
		content.add(mTextFieldName, "3,1");

		content.add(new JLabel("Variable Value:"), "1,3");
		mTextFieldValue = new JTextField(12);
		content.add(mTextFieldValue, "3,3");
		mTextFieldValue.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				super.keyTyped(e);
				enableItems();
			}
		});

		mCheckBoxIsPassword = new JCheckBox("Is Password");
		content.add(mCheckBoxIsPassword, "5,3");

		content.add(new JLabel("(If empty, then DataWarrior asks for a value when the macro is running)"), "1,5,5,5");

		content.add(new JLabel("Message Text:"), "1,7");
		mTextFieldMessage = new JTextField(24);
		content.add(mTextFieldMessage, "3,7,5,7");

		content.add(new JLabel("(Message or question to be shown, when DataWarrior askes for the value)"), "1,9,5,9");

		content.add(new JLabel("Variable Options:"), "1,11");
		mTextFieldOptions = new JTextField(24);
		content.add(mTextFieldOptions, "3,11,5,11");
		mTextFieldOptions.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				super.keyTyped(e);
				enableItems();
			}
		});

		content.add(new JLabel("(Empty for free text value, or comma delimited options to choose from)"), "1,13,5,13");

		enableItems();
		return content;
	}

	private void enableItems() {
		boolean enabled = (mTextFieldValue.getText().length() == 0);
		mTextFieldOptions.setEnabled(enabled);
		mTextFieldMessage.setEnabled(enabled);
		mCheckBoxIsPassword.setEnabled(enabled && mTextFieldOptions.getText().length() == 0);
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_NAME, mTextFieldName.getText());
		if (mTextFieldValue.getText().length() != 0)
			configuration.setProperty(PROPERTY_VALUE, mTextFieldValue.getText());
		else {
			if (mTextFieldMessage.getText().length() != 0)
				configuration.setProperty(PROPERTY_MESSAGE, mTextFieldMessage.getText());
			if (mTextFieldOptions.getText().length() != 0)
				configuration.setProperty(PROPERTY_OPTIONS, mTextFieldOptions.getText());
			if (mTextFieldOptions.getText().length() == 0 && mCheckBoxIsPassword.isSelected())
				configuration.setProperty(PROPERTY_IS_PASSWORD, "true");
		}
		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mTextFieldName.setText(configuration.getProperty(PROPERTY_NAME, ""));
		mTextFieldValue.setText(configuration.getProperty(PROPERTY_VALUE, ""));
		mTextFieldMessage.setText(configuration.getProperty(PROPERTY_MESSAGE, ""));
		mTextFieldOptions.setText(configuration.getProperty(PROPERTY_OPTIONS, ""));
		mCheckBoxIsPassword.setSelected("true".equals(configuration.getProperty(PROPERTY_IS_PASSWORD)));
		enableItems();
	}

	@Override
	public void setDialogConfigurationToDefault() {
		mTextFieldName.setText("");
		mTextFieldValue.setText("");
		mTextFieldMessage.setText("");
		mTextFieldOptions.setText("");
		mCheckBoxIsPassword.setSelected(false);
		enableItems();
	}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (configuration.getProperty(PROPERTY_NAME, "").length() == 0) {
			showErrorMessage("No variable name defined.");
			return false;
		}
		if ("true".equals(configuration.getProperty(PROPERTY_IS_PASSWORD))
		 && configuration.getProperty(PROPERTY_OPTIONS, "").length() != 0) {
			showErrorMessage("A password cannot be selected from predefined options.");
			return false;
		}

		return true;
	}

	@Override
	public void runTask(Properties configuration) {
		String name = configuration.getProperty(PROPERTY_NAME);
		String value = configuration.getProperty(PROPERTY_VALUE, "");
		if (value.length() == 0) {
			String question = configuration.getProperty(PROPERTY_MESSAGE, "Please define the value of variable '" + name + "'");
			String options = configuration.getProperty(PROPERTY_OPTIONS, "");
			boolean isPassword = "true".equals(configuration.getProperty(PROPERTY_IS_PASSWORD));
			if (options.length() != 0) {
				String[] option = options.split("\\s*,\\s*");
				if (option != null && option.length != 0)
					value = (String)JOptionPane.showInputDialog(getParentFrame(), question,
						"Define Variable", JOptionPane.QUESTION_MESSAGE, null, option, option[0]);
			}
			else if (isPassword) {
				JPasswordField passwordField = new JPasswordField();
				if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(getParentFrame(),
						passwordField, question, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE))
					value = new String(passwordField.getPassword());
			}
			else {
				value = JOptionPane.showInputDialog(getParentFrame(), question,
					"Define Variable", JOptionPane.QUESTION_MESSAGE);
			}

			if (value == null)
				((DEMacroRecorder)getProgressController()).stopMacro();
		}

		if (getProgressController() instanceof DEMacroRecorder)	// the progress controller of a running macro should always be a DEMacroRecorder
			((DEMacroRecorder)getProgressController()).setVariable(name, value);
	}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
	}
}
