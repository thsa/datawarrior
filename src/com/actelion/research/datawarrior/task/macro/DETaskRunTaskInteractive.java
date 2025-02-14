package com.actelion.research.datawarrior.task.macro;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.AbstractTask;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.datawarrior.task.StandardTaskFactory;
import com.actelion.research.datawarrior.task.TaskSpecification;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.util.Properties;
import java.util.TreeSet;

public class DETaskRunTaskInteractive extends ConfigurableTask {
	public static final String TASK_NAME = "Run Task Interactive";

	private static final String PROPERTY_TASK_CODE = "taskCode";
	private static final String PROPERTY_EXIT_ON_CANCEL = "exitOnCancel";

	private JComboBox<String> mComboBoxGroup,mComboBoxTask;
	private JCheckBox mCheckBoxExitOnCancel;

	public DETaskRunTaskInteractive(DEFrame parent) {
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
							{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		content.add(new JLabel("Task groupe:"), "1,1");
		mComboBoxGroup = new JComboBox<>();
		content.add(mComboBoxGroup, "3,1");

		content.add(new JLabel("Task name:"), "1,3");
		mComboBoxTask = new JComboBox<>();
		content.add(mComboBoxTask, "3,3");

		mCheckBoxExitOnCancel = new JCheckBox("Stop macro when configuration dialog is cancelled");
		content.add(mCheckBoxExitOnCancel, "1,5,3,5");

		TreeSet<TaskSpecification> dictionary = DataWarrior.getApplication().getTaskFactory().getTaskDictionary((DEFrame)getParentFrame());
		String group = "";
		for (TaskSpecification task : dictionary) {
			if (!group.equals(task.getCategoryName())) {
				group = task.getCategoryName();
				mComboBoxGroup.addItem(group);
			}
		}
		updateComboBoxTask();
		mComboBoxGroup.addActionListener(e -> updateComboBoxTask());

		return content;
	}

	private void updateComboBoxTask() {
		String group = (String)mComboBoxGroup.getSelectedItem();
		mComboBoxTask.removeAllItems();
		TreeSet<TaskSpecification> dictionary = DataWarrior.getApplication().getTaskFactory().getTaskDictionary((DEFrame)getParentFrame());
		for (TaskSpecification task : dictionary)
			if (group.equals(task.getCategoryName()))
				mComboBoxTask.addItem(task.getTaskName());
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_TASK_CODE, StandardTaskFactory.constructTaskCodeFromName((String)mComboBoxTask.getSelectedItem()));
		configuration.setProperty(PROPERTY_EXIT_ON_CANCEL, mCheckBoxExitOnCancel.isSelected() ? "true" : "false");
		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String taskCode = configuration.getProperty(PROPERTY_TASK_CODE);
		TreeSet<TaskSpecification> dictionary = DataWarrior.getApplication().getTaskFactory().getTaskDictionary((DEFrame)getParentFrame());
		for (TaskSpecification task : dictionary)
			if (StandardTaskFactory.constructTaskCodeFromName(task.getTaskName()).equals(taskCode)) {
				mComboBoxGroup.setSelectedItem(task.getCategoryName());
				mComboBoxTask.setSelectedItem(task.getTaskName());
			}
		mCheckBoxExitOnCancel.setSelected("true".equals(configuration.getProperty(PROPERTY_EXIT_ON_CANCEL)));
	}

	@Override
	public void setDialogConfigurationToDefault() {
		mComboBoxGroup.setSelectedIndex(0);
		mComboBoxTask.setSelectedIndex(0);
		mCheckBoxExitOnCancel.setSelected(false);
	}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		return true;
	}

	@Override
	public void runTask(Properties configuration) {
		String taskCode = configuration.getProperty(PROPERTY_TASK_CODE);
		boolean exitOnCancel = "true".equals(configuration.getProperty(PROPERTY_EXIT_ON_CANCEL));

		DataWarrior application = DataWarrior.getApplication();

		final AbstractTask task = application.getTaskFactory().createTaskFromCode((DEFrame)getParentFrame(), taskCode);

		final Properties[] configurationHolder = new Properties[1];
		try {
			SwingUtilities.invokeAndWait(() -> configurationHolder[0] = task.showDialog(configuration, true));
		}
		catch (Exception ie) {}

		if (task.isStatusOK())
			task.execute(configurationHolder[0], getProgressController());
		else if (exitOnCancel)
			setStopMacro();
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
	}
}
