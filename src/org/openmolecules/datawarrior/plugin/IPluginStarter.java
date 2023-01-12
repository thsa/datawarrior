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
 * If an org.openmolecules.datawarrior.plugin.PluginStarter class is found within a plugin,
 * then it is instantiated before any of its other classes are loaded and its initialize()
 * method is called immediately. If this method returns false, then this plugin's task are
 * not loaded and not added to the menu.
 * Possible use cases for a PluginStarter class are:<br>
 * - The plugin could check, whether other plugins are out of date and could replace outdated
 *   plugin jar files with downloaded updates. Note: Plugins are loaded in alphabetical order.
 *   Thus, a plugin meant to update other plugins should ideally start with 'AAA'.<br>
 * - Instead of defining menu entries and associated plugin tasks in 'tasknames' in a static way
 *   at plugin compile time, the PluginStarter class could define menu bar items and associated
 *   task classes dynamically at launch time.
 */
public interface IPluginStarter {
	/**
	 * This method is called when the plugin is loaded.
	 * @param startHelper callback object providing task registration and assignment to menu items
	 * @param pluginDir directory containing the plugin
	 * @param config the configuration obtained from a config.txt file from the root plugin folder
	 */
	boolean initialize(IPluginStartHelper startHelper, File pluginDir, Properties config);
}
