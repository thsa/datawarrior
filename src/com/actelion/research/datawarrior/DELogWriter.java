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

package com.actelion.research.datawarrior;

import java.net.URL;

public class DELogWriter {
	private static String sLogURL = null;

	// Idorsia's specific version of DataWarrior writes some log entries, if it is inside the Idorsia network:
	// running database queries, running macros, opening files...
	// In the public DataWarrior version no logs are written, because sLogURL is NULL!!!
	public static void setURL(String url) {
		sLogURL = url;
		}

	public static void writeEntry(String action, String params) {
        if (sLogURL != null && System.getProperty("development") == null) {
            final String _action = action.replaceAll("\\s", "_");
            final String _params = params.replaceAll("\\s", "_");
            new Thread(() -> {
                try {
                    new URL(sLogURL+"?project=DataWarrior&username="+System.getProperty("user.name")+"&action="+_action+"&parameters="+_params).getContent();
                    }
                catch(Exception e) {
                    e.printStackTrace();
                    }
                }).start();
            }
        }
    }
