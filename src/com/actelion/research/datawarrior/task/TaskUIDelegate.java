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

package com.actelion.research.datawarrior.task;

import java.util.Properties;
import javax.swing.JComponent;

public interface TaskUIDelegate {
    /**
	 * Creates a JPanel with all UI elements to define or update a given configuration.
	 * This call is followed by either setDialogConfiguration(Properties configuration)
	 * or by setDialogConfigurationToDefault().
	 * This method may return null in these cases:
	 * 1.) The task has no configuration items, i.e. doesn't need to be configured.
	 * 2.) The user cancels e.g. a login dialog for retrieving needed dialog options.
	 * 3.) An unexpected or database connection error occurs, which must be shown with an
	 * appropriate message dialog.
     * @return JPanel with UI components or null if user cancelled e.g. login dialog to retrieve
     */
    public JComponent createDialogContent();

    /**
     * Creates a configuration object from the current state of the dialog elements.
     * This method is called after calling createDialogContent().
     * @return current dialog configuration or null for non-configurable tasks
     */
    public Properties getDialogConfiguration();

    /**
     * Updates current dialog's UI items to reflect given configuration.
     * This may only be called after calling createDialogContent().
     * The passed configuration was one a valid one, but may not be valid
     * in the current environment.
     * @param configuration dialog settings to be shown
     */
    public void setDialogConfiguration(Properties configuration);

    /**
     * Updates current dialog's UI items to a default configuration.
     * This may only be called after calling createDialogContent().
     */
    public void setDialogConfigurationToDefault();
	}
