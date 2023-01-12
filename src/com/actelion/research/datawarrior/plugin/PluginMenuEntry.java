package com.actelion.research.datawarrior.plugin;

import org.openmolecules.datawarrior.plugin.IPluginTask;

public class PluginMenuEntry {
	private IPluginTask mTask;
	private String mMenuPath,mMenuItem;

	public PluginMenuEntry(IPluginTask task, String menuPath, String menuItem) {
		mTask = task;
		mMenuPath = menuPath;
		mMenuItem = menuItem;
	}

	public boolean isSeparator() {
		return mTask == null;
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

	public String getMenuPath() {
		return mMenuPath;
	}

	public String getMenuItemName() {
		return mMenuItem != null ? mMenuItem : mTask == null ? null : mTask.getTaskName();
	}
}
