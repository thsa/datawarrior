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

import com.actelion.research.datawarrior.help.DEAboutDialog;

import javax.swing.*;
import java.awt.*;
import java.io.File;

import static com.actelion.research.datawarrior.DataWarrior.LookAndFeel.*;

public class DataWarriorOSX extends DataWarrior {
	private static final LookAndFeel[] LOOK_AND_FEELS = { AQUA, NIGHT, GRAPHITE, GRAY, MODERATE, NEBULA, CREME, SAHARA };

	private static final LookAndFeel DEFAULT_LAF = NIGHT;

	// when switching from substance to aqua with some open views there may be
	// exceptions in the "org.pushingpixels." classes. In that case we only act on the first one
	// by asking the user to quit and restart.
	private static boolean sSubstanceExceptionOccurred = false;

    public boolean isMacintosh() {
        return true;
        }

	@Override
	public LookAndFeel[] getAvailableLAFs() {
		return LOOK_AND_FEELS;
		};

	@Override
	public LookAndFeel getDefaultLAF() {
		return DEFAULT_LAF;
		}

	@Override
	public boolean setLookAndFeel(LookAndFeel laf) {
		if (laf.equals(AQUA)
		 && getActiveFrame() != null
		 && getActiveFrame().getMainFrame().getMainPane().getTableView() != null) {
			JOptionPane.showMessageDialog(getActiveFrame(), "For technical reasons you cannot switch to the Aqua L&F after you have opened a data file.");
			return false;
			}
		if (super.setLookAndFeel(laf))
			return true;

		return false;
		}

	private void registerAppleEvents() {
		Desktop desktop = Desktop.getDesktop();
		if (desktop != null) {
			desktop.setAboutHandler(e -> new DEAboutDialog(getActiveFrame()));
			desktop.setOpenFileHandler(e -> {
				for (File f:e.getFiles())
					readFile(f.getPath());
				} );
			desktop.setOpenURIHandler(e -> {
				JOptionPane.showMessageDialog(getActiveFrame(), "Open URI:"+e.getURI());
				} );
			desktop.setQuitHandler((e,response) -> {
				if (closeApplication(true))
					response.performQuit();
				else
					response.cancelQuit();
				} );
			desktop.setPreferencesHandler(null);	// No preferences menu item!
			}
		}

	public static void main(String[] args) {
		initModuleAccess();

		SwingUtilities.invokeLater(() -> {
			try {
				System.setProperty("com.apple.macos.use-file-dialog-packages", "true");

				// On Sonoma 14.0 and 14.1 and if started with applauncher, using useScreenMenuBar causes a crash:
				// "References to Carbon menus are disallowed with AppKit menu system (see rdar://101002625).
				// Use instances of NSMenu and NSMenuItem directly instead."
				if (!isBuggySonoma())
					System.setProperty("apple.laf.useScreenMenuBar", "true");

				System.setProperty("com.apple.mrj.application.apple.menu.about.name", "DataWarrior");

				final DataWarriorOSX explorer = new DataWarriorOSX();

				Thread.setDefaultUncaughtExceptionHandler((t, e) -> SwingUtilities.invokeLater(() -> {
					if (e.getStackTrace()[0].getClassName().startsWith("org.pushingpixels")) {
						if (!sSubstanceExceptionOccurred) {
							sSubstanceExceptionOccurred = true;
							e.printStackTrace();
							JOptionPane.showMessageDialog(explorer.getActiveFrame(), "Uncaught L&F Exception: Please quit and start again.");
							}
						}
					else {
						e.printStackTrace();
						JOptionPane.showMessageDialog(explorer.getActiveFrame(), "Uncaught Exception:" + e.getMessage());
						}
					}));

				explorer.registerAppleEvents();
				}
			catch(Exception e) {
				JOptionPane.showMessageDialog(null, "Unexpected Exception: "+e);
				e.printStackTrace();
				}
			});
        }

	private static boolean isBuggySonoma() {
		return System.getProperty("os.name").toLowerCase().startsWith("mac")
			&& (System.getProperty("os.version").equals("14.0")
			 || System.getProperty("os.version").equals("14.1"));
		}
	}