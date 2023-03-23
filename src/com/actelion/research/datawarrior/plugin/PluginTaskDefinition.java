package com.actelion.research.datawarrior.plugin;

import org.openmolecules.datawarrior.plugin.IPluginTask;

public class PluginTaskDefinition {
	private IPluginTask mTask;
	private String mTaskGroupName;

	public PluginTaskDefinition(IPluginTask task, String taskGroupName) {
		mTask = task;
		mTaskGroupName = taskGroupName;
	}

	public IPluginTask getTask() {
		return mTask;
	}

	public String getTaskName() {
		return mTask == null ? null : mTask.getTaskName();
	}

	public String getTaskCode() {
		return mTask == null ? null : mTask.getTaskCode();
	}

	public String getTaskGroupName() {
		return mTaskGroupName;
	}
}
