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
import org.openmolecules.datawarrior.plugin.IPluginInitializer;
import org.openmolecules.datawarrior.plugin.IPluginTask;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class PluginRegistry {
	private static final String INITIALIZER_CLASS_NAME = "PluginInitializer";
	private static final String CONFIG_FILE_NAME = "config.txt";
	private static final String KEY_CUSTOM_PLUGIN_DIRS = "custom_plugin_dirs";

	private ArrayList<PluginSpec> mPluginList;

	public PluginRegistry(DataWarrior application) {
		loadPlugins(application);
	}

	public ArrayList<PluginSpec> getPlugins() {
		return mPluginList;
	}

	private void loadPlugins(DataWarrior application) {
		mPluginList = new ArrayList<>();

		File rootPluginDir = application.resolveResourcePath(DataWarrior.PLUGIN_DIR);

		Properties config = new Properties();

		File configFile = new File(rootPluginDir+File.separator+CONFIG_FILE_NAME);
		if (configFile.exists()) {
			try {
				config.load(new FileReader(configFile));
			} catch (IOException ioe) {}
		}

		// Load plugins from standard plugin directory
		if (rootPluginDir != null && rootPluginDir.isDirectory())
			loadPlugins(rootPluginDir, config);

		// Load plugins from defined custom plugin directories
		String customPaths = config.getProperty(KEY_CUSTOM_PLUGIN_DIRS);
		if (customPaths != null) {
			for (String customPath:customPaths.split(",")) {
				File customPluginDir = new File(application.resolvePathVariables(customPath.trim()));
				if (customPluginDir.exists() && customPluginDir.isDirectory())
					loadPlugins(customPluginDir, config);
			}
		}
	}

	private void loadPlugins(File directory, Properties config) {
		File[] files = directory.listFiles(file -> !file.isDirectory() && file.getName().toLowerCase().endsWith(".jar"));
		if (files != null && files.length != 0) {
			Arrays.sort(files, Comparator.comparing(File::getName));
			for (File file : files) {
				try {
					ClassLoader loader = URLClassLoader.newInstance(new URL[] { file.toURI().toURL() }, getClass().getClassLoader());

					// Since Sep2021 plugins may contain an Initializer class. We try to load and run it...
					try {
						Class initializerClass = loader.loadClass(INITIALIZER_CLASS_NAME);
						IPluginInitializer initializer = (IPluginInitializer)initializerClass.newInstance();
						initializer.initialize(directory, config);
					}
					catch (Exception e) {
						// no error handling, because it is OK for the class not to be present
					}

					BufferedReader br = new BufferedReader(new InputStreamReader(loader.getResourceAsStream("tasknames")));
					String line = br.readLine();
					while (line != null && line.length() != 0) {
						String[] lineEntry = line.split(","); // we may have one or two items per line: <className>[,menuName]
						if (lineEntry != null && lineEntry.length != 0) {
							Class pluginClass = loader.loadClass(lineEntry[0].trim());
							String menuName = (lineEntry.length == 1 || lineEntry[1].length() == 0) ? null : lineEntry[1].trim();
							mPluginList.add(new PluginSpec((IPluginTask)pluginClass.newInstance(), menuName));
							line = br.readLine();
						}
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
