package com.actelion.research.datawarrior.plugin;

import org.openmolecules.datawarrior.plugin.IPluginTask;

public class PluginSpec {
	private IPluginTask mTask;
	private String mMenu;

	public PluginSpec(IPluginTask task, String menu) {
		mTask = task;
		mMenu = menu;
	}

	public IPluginTask getTask() {
		return mTask;
	}

	public String getTaskName() {
		return mTask.getTaskName();
	}

	public String getTaskCode() {
		return mTask.getTaskCode();
	}

	public String getMenuName() {
		return mMenu;
	}
}
