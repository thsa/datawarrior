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

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

import javax.swing.*;
import javax.swing.plaf.MenuBarUI;

import static com.actelion.research.datawarrior.DataWarrior.LookAndFeel.*;

public class DataWarriorOSX extends DataWarrior {
	private static final LookAndFeel[] LOOK_AND_FEELS = { AQUA, GRAPHITE, GRAY, NEBULA };

	private static final LookAndFeel DEFAULT_LAF = GRAPHITE;

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

	@Override
	public StandardMenuBar createMenuBar(DEFrame frame) {
		return makeOSXMenuBar(super.createMenuBar(frame));
		}

	public StandardMenuBar makeOSXMenuBar(StandardMenuBar menuBar) {
		try {
			menuBar.setUI((MenuBarUI) Class.forName("com.apple.laf.AquaMenuBarUI").newInstance());
			}
		catch (Exception ex) {
			ex.printStackTrace();
			}
		return menuBar;
		}

	@Override
	public boolean updateLookAndFeel(LookAndFeel laf) {
		if (super.updateLookAndFeel(laf)) {
			for (DEFrame f : getFrameList()) {
				try {
					StandardMenuBar menubar = f.getDEMenuBar();
					menubar.setUI((MenuBarUI) Class.forName("com.apple.laf.AquaMenuBarUI").newInstance());
					}
				catch (Exception ex) {
					ex.printStackTrace();
					}
				}
			return true;
			}
		return false;
		}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
            public void run() {
				try {
					System.setProperty("com.apple.macos.use-file-dialog-packages", "true");
		            System.setProperty("apple.laf.useScreenMenuBar", "true");

					final DataWarriorOSX explorer = new DataWarriorOSX();

					Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
						@Override
						public void uncaughtException(final Thread t, final Throwable e) {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
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
									}
								});
							}
						});

// They may be deprecated, but still with Bellsoft JRE 1.8 232 on OSX 10.15.7 Catalina
// opening doucuments by icon double click works over these events!!! TLS 21Jan2021

		            Application app = new Application();
		            app.addApplicationListener(new ApplicationAdapter() {
		                public void handleAbout(ApplicationEvent event) {
//							JOptionPane.showMessageDialog(null, "com.apple.eawt.ApplicationEvent: about");
		                    new DEAboutDialog(explorer.getActiveFrame());
		                    event.setHandled(true);
		                    }
		                    
		                public void handleOpenFile(ApplicationEvent event) {
//							JOptionPane.showMessageDialog(null, "com.apple.eawt.ApplicationEvent: open");
		                    explorer.readFile(event.getFilename());
		                    event.setHandled(true);
		                    }
		                    
		                public void handlePrintFile(ApplicationEvent event) {
//							JOptionPane.showMessageDialog(null, "com.apple.eawt.ApplicationEvent: print");
		                    explorer.getActiveFrame().getDEMenuBar().menuFilePrint();
		                    event.setHandled(true);
		                    }

		                public void handleQuit(ApplicationEvent event) {
//							JOptionPane.showMessageDialog(null, "com.apple.eawt.ApplicationEvent: quit");
		                    explorer.closeApplication(true);
		                    event.setHandled(true);
		                    }

		//            public void handleOpenApplication(ApplicationEvent event) {}
		//            public void handlePreferences(ApplicationEvent event) {}
		//            public void handleReOpenApplication(ApplicationEvent event) {}
		                } );
					}
				catch(Exception e) {
					JOptionPane.showMessageDialog(null, "Unexpected Exception: "+e);
					e.printStackTrace();
					}
            	}
			} );
        }
	}