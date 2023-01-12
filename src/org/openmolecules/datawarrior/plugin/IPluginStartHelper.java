package org.openmolecules.datawarrior.plugin;

public interface IPluginStartHelper {
	String MENU_PATH_SEPARATOR = ";";

	void registerTask(IPluginTask task, String taskGroupName);
	void addMenuSeparator(String menuPath);
	void addTaskToMenu(String menuPath, String menuItem, IPluginTask task);
}
