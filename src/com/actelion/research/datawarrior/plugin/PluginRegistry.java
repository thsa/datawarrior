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
import org.openmolecules.datawarrior.plugin.IPluginTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

public class PluginRegistry {
	private ArrayList<IPluginTask> mPluginList;

	public PluginRegistry(DataWarrior application) {
		loadPlugins(application);
	}

	public ArrayList<IPluginTask> getPluginTasks() {
		return mPluginList;
	}

	private void loadPlugins(DataWarrior application) {
		mPluginList = new ArrayList<>();
		File directory = application.resolveResourcePath(DataWarrior.PLUGIN_DIR);
		if (directory != null) {
			FileFilter filter = file -> {
				if (file.isDirectory())
					return false;
				return (file.getName().toLowerCase().endsWith(".jar"));
			};

			File[] files = directory.listFiles(filter);
			if (files != null && files.length != 0) {
				for (File file : files) {
					try {
						ClassLoader loader = URLClassLoader.newInstance(new URL[] { file.toURI().toURL() }, getClass().getClassLoader());
						BufferedReader br = new BufferedReader(new InputStreamReader(loader.getResourceAsStream("tasknames")));
						String className = br.readLine();
						while (className != null && className.length() != 0) {
							Class pluginClass = loader.loadClass(className.trim());
							mPluginList.add((IPluginTask)pluginClass.newInstance());
							className = br.readLine();
							}
						br.close();
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
