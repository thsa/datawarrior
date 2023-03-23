package com.actelion.research.datawarrior;

import com.actelion.research.datawarrior.task.AbstractTask;
import com.actelion.research.datawarrior.task.ITableRowTask;
import com.actelion.research.datawarrior.task.db.DETaskPluginTask;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableEvent;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

/**
 * The runtime properties section of a DataWarrior file may define specific tasks,
 * which can be triggered from the row popup menu and use data from the chosen row to perform the task.
 * This class defines invoking menu item, optionally a parent menu item that groups multiple tasks,
 * the task code, and optionally a task configuration taken from the runtime properties.
 * It also provides the popup menu item's ActionListener that creates and invokes the task.
 */
public class DETableRowTaskDef implements ActionListener {
	private DEFrame mParentFrame;
	private CompoundRecord mRecord;
	private String mTaskCode,mTaskConfig,mParentMenu,mMenuItem;

	/**
	 * @param parent
	 * @param taskCode
	 * @param taskConfig null or task configuration as one line of key1=value1[&key2=value2]...
	 * @param parentMenu null or parent menu name that groups this task with others
	 * @param menuItem menu item name shown in the row pupup menu to lauch this task
	 */
	public DETableRowTaskDef(DEFrame parent, String taskCode, String taskConfig, String parentMenu, String menuItem) {
		mParentFrame = parent;
		mTaskCode = taskCode;
		mTaskConfig = taskConfig;
		mParentMenu = parentMenu;
		mMenuItem = menuItem;
	}

	public void setTableRow(CompoundRecord record) {
		mRecord = record;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		AbstractTask task = createTask();
		if (task != null)
			task.defineAndRun();
	}

	private AbstractTask createTask() {
		AbstractTask task = mParentFrame.getApplication().getTaskFactory().createTaskFromCode(mParentFrame, mTaskCode);
		if (task != null) {
			// The task may implement ITableRowTask itself or
			// if it is a DETaskPluginTask, then its delegate may implement ITableRowTask.
			ITableRowTask tableRowTask;
			if (task instanceof ITableRowTask)
				tableRowTask = (ITableRowTask)task;
			else if (task instanceof DETaskPluginTask && ((DETaskPluginTask)task).getDelegate() instanceof ITableRowTask)
				tableRowTask = (ITableRowTask)((DETaskPluginTask)task).getDelegate();
			else
				return null;

			tableRowTask.setTableRow(mRecord, createConfig());
		}
		return task;
	}

	/**
	 * @return Properties constructed from mTaskConfig
	 */
	private Properties createConfig() {
		if (mTaskConfig == null)
			return null;

		Properties configuration = new Properties();
		for (String propertyline:mTaskConfig.split(ITableRowTask.CONFIG_DELIMITER)) {
			int index = propertyline.indexOf('=');
			if (index != -1)
				configuration.put(propertyline.substring(0, index).trim(), propertyline.substring(index+1).trim());
		}
		return configuration;
	}

	/**
	 * Handles CompoundTableEvent if needed and returns true, if the task can still be executed
	 * @param e
	 * @return false, if task is obsolete and should be removed from list
	 */
	public boolean handleCompoundTableChanged(CompoundTableEvent e) {
		boolean taskIsValid = true;

		// if task is associated with a table column and if that column is deleted, then the task is meaningless
		if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			if (mTaskConfig != null) {
				String keyColumn = createConfig().getProperty(ITableRowTask.CONFIG_KEY_COLUMN);
				if (keyColumn != null) {
					int[] columnMap = e.getMapping();
					int column = mParentFrame.getTableModel().findColumn(keyColumn);
					if (column == -1 || columnMap[column] == -1)
						taskIsValid = false;
					}
				}
			}

		return taskIsValid;
	}

	/**
	 * @return false if a key column is defined and this column is not found in the current table model
	 */
	public boolean isValid() {
		if (mTaskConfig != null) {
			String keyColumn = createConfig().getProperty(ITableRowTask.CONFIG_KEY_COLUMN);
			if (keyColumn != null && mParentFrame.getTableModel().findColumn(keyColumn) == -1)
				return false;
		}

		return true;
	}

	/**
	 * @return unique task code for the task factory to instantialte the task
	 */
	public String getTaskCode() {
		return mTaskCode;
	}

	/**
	 * @return null or predefined configuration needed by the task to fullfill its action
	 */
	public String getTaskConfig() {
		return mTaskConfig;
	}

	/**
	 * @return null or menu name that groups multiple related row task popup menu items
	 */
	public String getParentMenu() {
		return mParentMenu;
	}

	/**
	 * @return menu item text for the row popup menu that triggers this task
	 */
	public String getMenuItem() {
		return mMenuItem;
	}
}
