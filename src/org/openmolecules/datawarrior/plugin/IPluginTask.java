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

package org.openmolecules.datawarrior.plugin;

import javax.swing.*;
import java.util.Properties;

/**
 * This interface defines the methods needed to fulfill a DataWarrior PluginTask.
 * A PluginTask is an externally defined DataWarrior task that is represented by an additional
 * menu item, which, if selected shows a configuration dialog. When the dialog is closed, it
 * creates a configuration object, which DataWarrior may pass to the PluginTask's run() method
 * or store as part of a macro.
 * A PluginTask's class is stored within a jar file that resides in DataWarrior's 'plugin' directory.
 */
public interface IPluginTask {
	// These methods are called from the event dispatch thread

	/**
	 * This is the task name to be used in the macro editor and for the menu item,
	 * e.g. "Search Corporate Database".
	 * @return short task name
	 */
	String getTaskName();       // e.g. "Search Corporate Database" (used for menu item and macro tasks)

	/**
	 * Task codes are used to identify the task uniquely within DataWarrior macros.
	 * @return worldwide unique task code, e.g. "MerckSearchCorporateDB".
	 */
	String getTaskCode();       // e.g. "MerckSearchCorporateDB" (should be unique and unchanged over time)

	/**
	 * Creates the user interface to let the user define any task options.
	 * For a database retrieval task this might be a list of assay parameters,
	 * a chemical sub-structure, a date span, electronic notebook numbers, etc.
	 * Typically, this is a JPanel with JTextFields, JComboBoxes, and an 8 pixel
	 * border around it. The UI components should not include Cancel & OK buttons,
	 * because they are provided automatically. Initial user interface elements
	 * should reflect a valid default configuration.
	 * @return
	 */
	JComponent createDialogContent(IUserInterfaceHelper dialogHelper);

	/**
	 * This method is called when the user presses the dialog's OK button.
	 * Therefore all UI elements of the dialog content are existing and their
	 * current content should reflect the user's wishes. This method should
	 * create a Properties object with key-value pair reflecting the UI element's
	 * settings.
	 * @return
	 */
	Properties getDialogConfiguration();

	/**
	 * This method is used to re-configure all UI elements after the dialog
	 * is built with createDialogContent() to represent an earlier user-defined
	 * configuration, e.g. when the dialog opens and the respective task was
	 * performed earlier, then it is configured with the last successful configuration.
	 * This method is also used, when a task configuration of a macro is edited.
	 * @param configuration
	 */
	void setDialogConfiguration(Properties configuration);

	/**
	 * Checks, whether the task configuration is valid such that the task can be executed without forseeable errors.
	 * @param configuration
	 * @return null if configuration is valid; otherwise human-readable error message
	 */
	String checkConfiguration(Properties configuration);

	/**
	 * When this method is called to execute the task, the dialog and its UI elements are
	 * not existing. This method must take execution details from the configuration object.
	 * This method is not(!) called from the event dispatch thread and should not use any UI components.
	 * @param configuration
	 * @param dwInterface
	 */
	void run(Properties configuration, IPluginHelper dwInterface); // runs the task with the given configuration
}
