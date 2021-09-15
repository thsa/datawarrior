/*
 * Copyright 2021 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91, CH-4123 Allschwil, Switzerland
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

package org.openmolecules.datawarrior.plugin;

import java.io.File;
import java.util.Properties;

/**
 * This interface contains an initialization method of a PluginInitializer class, which allows
 * to run any Java code during plugin loading, just after DataWarrior has been launched.
 * Plugins may or may not contain a PluginInitializer class or a plugin may contain nothing else
 * than a PluginInitializer class.
 * If a plugin is loaded that contains a PluginInitializer class, then the class is loaded
 * by the classloader and its initialize() method is called immediately.<br>
 * Since plugins are loaded in alphabetical order, a plugin that is loaded first may be used
 * to update other plugin files if needed.
 */
public interface IPluginInitializer {
	/**
	 * This method is called when the plugin is loaded.
	 * @param pluginDir directory containing the plugin
	 */
	void initialize(File pluginDir, Properties config);
}
