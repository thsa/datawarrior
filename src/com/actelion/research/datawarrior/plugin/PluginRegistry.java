/*
 * Copyright 2017 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91, CH-4123 Allschwil, Switzerland
 *
 * This file is part of DataWarrior.
 *
 * DataWarrior is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * DataWarrior is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with DataWarrior.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package com.actelion.research.datawarrior.plugin;

import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.ITableRowTask;
import com.actelion.research.datawarrior.task.db.DETaskPluginTask;
import org.openmolecules.datawarrior.plugin.IPluginInitializer;
import org.openmolecules.datawarrior.plugin.IPluginStartHelper;
import org.openmolecules.datawarrior.plugin.IPluginStarter;
import org.openmolecules.datawarrior.plugin.IPluginTask;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;

public class PluginRegistry implements IPluginStartHelper {
	private static final String INITIALIZER_CLASS_NAME = "PluginInitializer";
	private static final String STARTER_CLASS_NAME = "org.openmolecules.datawarrior.plugin.PluginStarter";
	private static final String CONFIG_FILE_NAME = "config.txt";
	private static final String KEY_CUSTOM_PLUGIN_DIRS = "custom_plugin_dirs";

	private DataWarrior mApplication;
	private ArrayList<PluginMenuEntry> mMenuEntryList;
	private ArrayList<PluginTaskDefinition> mTaskDefinitionList;

	public PluginRegistry(DataWarrior application, ClassLoader parent) {
		mApplication = application;
		loadPlugins(parent);
	}

	public ArrayList<PluginTaskDefinition> getPluginTasks() {
		return mTaskDefinitionList;
	}

	public IPluginTask getPluginTask(String taskCode) {
		for (PluginTaskDefinition def:mTaskDefinitionList)
			if (def.getTaskCode().equals(taskCode))
				return def.getTask();
		return null;
	}

	private void loadPlugins(ClassLoader parent) {
		mMenuEntryList = new ArrayList<>();
		mTaskDefinitionList = new ArrayList<>();

		File rootPluginDir = mApplication.resolveResourcePath(DataWarrior.PLUGIN_DIR);

		Properties config = new Properties();

		File configFile = new File(rootPluginDir+File.separator+CONFIG_FILE_NAME);
		if (configFile.exists()) {
			try {
				config.load(new FileReader(configFile));
			} catch (IOException ioe) {}
		}

		// Load plugins from standard plugin directory
		if (rootPluginDir != null && rootPluginDir.isDirectory())
			loadPlugins(rootPluginDir, config, parent);

		// Load plugins from defined custom plugin directories
		String customPaths = config.getProperty(KEY_CUSTOM_PLUGIN_DIRS);
		if (customPaths != null) {
			for (String customPath:customPaths.split(",")) {
				File customPluginDir = new File(mApplication.resolvePathVariables(customPath.trim()));
				if (customPluginDir.exists() && customPluginDir.isDirectory())
					loadPlugins(customPluginDir, config, parent);
			}
		}
	}

	private void loadPlugins(File directory, Properties config, ClassLoader parent) {
		try {
			// First we try instantiating the starter class using the standard class loader, which should only succeed,
			// if it was added to the DataWarrior source code for the purpose of plugin development & debugging.
			// If the starter class is found, then it is allowed to initialize and loading of real plugins is skipped.
			Class starterClass = getClass().getClassLoader().loadClass(STARTER_CLASS_NAME);
			IPluginStarter starter = (IPluginStarter)starterClass.newInstance();
			starter.initialize(this, directory, config);
			return;
		}
		catch (Exception e) {}

		// Now we try loading all real plugins...
		File[] files = directory.listFiles(file -> !file.isDirectory() && file.getName().toLowerCase().endsWith(".jar"));
		if (files != null && files.length != 0) {
			Arrays.sort(files, Comparator.comparing(File::getName));
			for (File file : files) {
				try {
//					ClassLoader loader = URLClassLoader.newInstance(new URL[] { file.toURI().toURL() }, getClass().getClassLoader());
					ClassLoader loader = new URLClassLoader(new URL[] { file.toURI().toURL() }, parent);

					// Since Dec2022 plugins may contain an PluginStarter class. We try to load and run it...
					try {
						Class starterClass = loader.loadClass(STARTER_CLASS_NAME);
						IPluginStarter starter = (IPluginStarter)starterClass.newInstance();
						starter.initialize(this, directory, config);
					}
					catch (Exception e) {
						// If we don't find the PluginStarter class, we look for the older PluginInitializer
						// and then load and use plugin task names from the 'tasknames' file.

						try {
							// Since Sep2021 plugins may contain an Initializer class, which is already deprecated. We try to load and run it...
							Class initializerClass = loader.loadClass(INITIALIZER_CLASS_NAME);
							IPluginInitializer initializer = (IPluginInitializer)initializerClass.newInstance();
							initializer.initialize(directory, config);
						}
						catch (Exception ee) {
							// no error handling, because it is OK for the class not to be present
						}

						BufferedReader br = new BufferedReader(new InputStreamReader(loader.getResourceAsStream("tasknames")));
						String line = br.readLine();
						while (line != null) {
							if (!line.isEmpty() && !line.startsWith("#")) {
								int index = line.indexOf(','); // we may have one or multiple items per line: <className>[,spec1[;spec2[;spec3]]]
								String className = (index == -1 ? line : line.substring(0, index)).trim();
								String menuName = (index == -1) ? "Database" : line.substring(index+1).trim();

								Class pluginClass = null;
								try {
									// If the class is part of the DataWarrior source code (usually it is not),
									// then we instantiate it with the standard class loader to make it available for debugging.
									// For debugging the jar file defining plugin task names must still be in the plugin folder.
									pluginClass = Class.forName(className);
								}
								catch (ClassNotFoundException cnfe) {
									// However, typically the class is part of an external jar file and is instantiated
									// with an extra class loader directly from the plugin jar file.
									try {
										pluginClass = loader.loadClass(className);
									}
									catch (ClassNotFoundException icnfe) {
										System.out.println("Class '"+className+"' not found in plugin '"+file.getName()+"'.");
									}
									catch (Throwable t) {
										t.printStackTrace();
									}
								}
								if (pluginClass != null) {
									IPluginTask task = (IPluginTask)pluginClass.newInstance();
									String taskGroupName = menuName;

									// For old handling add separator, if menu exists
									if (menuName.equals("File")
									 || menuName.equals("Edit")
									 || menuName.equals("Data")
									 || menuName.equals("Chemistry")
									 || menuName.equals("List")
									 || menuName.equals("Macro")
									 || menuName.equals("Help")) {
										// add separator
										mMenuEntryList.add(new PluginMenuEntry(null, menuName, task.getTaskName()));
									}
									// Translate old menu name to menu path for new handling
									else if (menuName.equals("From Chemical Structure")) {
										// add separator
										mMenuEntryList.add(new PluginMenuEntry(null, menuName, task.getTaskName()));
										menuName = "Chemistry" + MENU_PATH_SEPARATOR + "From Chemical Structure";
										taskGroupName = "Chemistry";
										}
									else if (menuName.equals("From Chemical Reaction")) {
										// add separator
										mMenuEntryList.add(new PluginMenuEntry(null, menuName, task.getTaskName()));
										menuName = "Chemistry" + MENU_PATH_SEPARATOR + "From Chemical Reaction";
										taskGroupName = "Chemistry";
										}

									mTaskDefinitionList.add(new PluginTaskDefinition(task, taskGroupName));
									mMenuEntryList.add(new PluginMenuEntry(task, menuName, task.getTaskName()));
								}
							}
							line = br.readLine();
						}
						br.close();
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * This method implements part of the PluginStartHelper interface, which is currently preferred way
	 * to assign plugin tasks to menu items. This method introduces the task to the DataWarrior and, thus,
	 * makes it available for any use, which includes its usage as part of a macro.
	 * @param task
	 */
	@Override
	public void registerTask(IPluginTask task, String taskGroupName) {
		mTaskDefinitionList.add(new PluginTaskDefinition(task, taskGroupName));
	}

	/**
	 * This method implements part of the PluginStartHelper interface, which is currently preferred
	 * way to assign plugin tasks to menu items. These methods record menu additions and assignments to
	 * plugin classes to repeatedly add those menu items whenever a new menu is created.
	 * It also introduces the task to the task factory. Thus, it can be used in macros.
	 * @param menuPath
	 * @param menuItem
	 * @param task
	 */
	@Override
	public void addTaskToMenu(String menuPath, String menuItem, IPluginTask task) {
		mMenuEntryList.add(new PluginMenuEntry(task, menuPath, menuItem));
	}

	/**
	 * This method implements part of the PluginStartHelper interface, which is currently preferred
	 * way to assign plugin tasks to menu items. These methods record menu additions and assignments to
	 * plugin classes to repeatedly add those menu items whenever a new menu is created.
	 * @param menuPath
	 */
	@Override
	public void addMenuSeparator(String menuPath) {
		mMenuEntryList.add(new PluginMenuEntry(null, menuPath, null));
	}

	public void addPluginMenuItems(JMenuBar menuBar) {
		for (PluginMenuEntry pts:mMenuEntryList) {
			JMenu menu = getOrCreateDeepMenu(menuBar, pts.getMenuPath());
			if (pts.getTask() == null) {
				menu.addSeparator();
			}
			else {
				JMenuItem item = new JMenuItem(pts.getMenuItemName());
				IPluginTask delegate = pts.getTask();
				item.addActionListener(e -> {
					if (delegate instanceof ITableRowTask)
						((ITableRowTask)delegate).setTableRow(null, null);
					new DETaskPluginTask(mApplication, delegate).defineAndRun();
					} );
				menu.add(item);
			}
		}
	}

	private JMenu getOrCreateDeepMenu(JMenuBar menuBar, String menuPath) {
		String[] menuName = menuPath.split(MENU_PATH_SEPARATOR);
		JMenu menu = getOrCreateMenu(menuBar, menuName[0]);
		for (int i=1; i<menuName.length; i++)
			menu = getOrCreateMenu(menu, menuName[i]);

		return menu;
	}

	private JMenu getOrCreateMenu(JMenuBar parent, String menuName) {
		for (MenuElement c:parent.getSubElements())
			if (c instanceof JMenu && ((JMenu)c).getText().equalsIgnoreCase(menuName))
				return (JMenu)c;

		JMenu menu = new JMenu(menuName);
		parent.add(menu);
		return menu;
	}

	private JMenu getOrCreateMenu(JMenu parent, String menuName) {
		for (int i=0; i<parent.getItemCount(); i++) {
			JMenuItem item = parent.getItem(i);
			if (item instanceof JMenu && ((JMenu)item.getComponent()).getText().equalsIgnoreCase(menuName))
				return (JMenu)item;
		}

		JMenu menu = new JMenu(menuName);
		parent.add(menu);
		return menu;
	}
}
