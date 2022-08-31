package com.actelion.research.datawarrior.plugin;

import org.openmolecules.datawarrior.plugin.IPluginTask;

public class PluginSpec {
	private IPluginTask mTask;
	private String mMenu;
	private boolean mMenuFound;

	public PluginSpec(IPluginTask task, String menu) {
		mTask = task;
		mMenu = menu;
		mMenuFound = false;
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

	public boolean isMenuFound() {
		return mMenuFound;
	}

	public void setMenuFound(boolean b) {
		mMenuFound = b;
	}
}
