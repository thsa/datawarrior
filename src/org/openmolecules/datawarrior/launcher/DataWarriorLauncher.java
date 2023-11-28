package org.openmolecules.datawarrior.launcher;

import javax.swing.*;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.prefs.Preferences;

public class DataWarriorLauncher {
	// IMPORTANT: This must match DEUpdateHandler.DATAWARRIOR_VERSION within datawarrior_all.jar
	//            included in the manual(!!!) installer package, which also contains this launcher.
	//            Automatic updates are considered only, if their version is newer than this one.
	private static final String BASE_VERSION = "v05.09.00";

	// These settings are copies from the DataWarrior class:
	private static final String PREFERENCES_ROOT = "org.openmolecules.datawarrior";
	private static final String PREFERENCES_KEY_UPDATE_PATH = "update_path";
	private static Class sDatawarriorClass;

	public static void main(final String[] args) {
		SwingUtilities.invokeLater(() -> {
			try {
				File appJar = null;

				// Try using newest datawarrior_vXX.XX.XX.jar from datawarrior/update directory
				Preferences prefs = Preferences.userRoot().node(PREFERENCES_ROOT);
				File updateDir = new File(prefs.get(PREFERENCES_KEY_UPDATE_PATH, ""));
				if (updateDir.exists() && updateDir.isDirectory()) {
					File[] files = updateDir.listFiles(file -> fileQualifies(file));
					if (files != null && files.length != 0) {
						Arrays.sort(files, Comparator.comparing(File::getName));
						appJar = files[files.length-1];
						}
					}

				if (appJar == null) {
					String path = isWindows() ? "C:\\Program Files\\DataWarrior\\datawarrior_all.jar"
								: isMacintosh() ? "/Applications/DataWarrior.app/Contents/Java/update/datawarrior_all.jar"
								: "/opt/datawarrior/datawarrior_all.jar";
					appJar = new File(path);
					if (!appJar.exists()) {
						JOptionPane.showMessageDialog(null, "DataWarrior application not found: " + path);
						return;
						}
					}

				if (!appJar.canRead()) {
					JOptionPane.showMessageDialog(null, "Cannot open application file: " + appJar.getPath());
					return;
					}

System.out.println("Loading "+appJar.getPath()+" ...");
				ClassLoader loader = new URLClassLoader(new URL[]{appJar.toURI().toURL()}, Thread.currentThread().getContextClassLoader());
				Thread.currentThread().setContextClassLoader(loader);

				sDatawarriorClass = loader.loadClass(getAppClassName());

				try {
					final Method method = sDatawarriorClass.getMethod("main", String[].class);
					final Object[] argsHolder = new Object[1];
					argsHolder[0] = args;
					method.invoke(null, (Object)args);
					}
				catch (final Exception e) {
					e.printStackTrace();
					}
				}

			catch(Exception e) {
				e.printStackTrace();
				}
			} );
		}

	private static boolean fileQualifies(File file) {
		if (file.isDirectory())
			return false;

		String filename = file.getName().toLowerCase();
		return filename.startsWith("datawarrior_v")
			&& filename.endsWith(".jar")
			&& filename.length() == 25
			&& filename.substring(12, 21).compareTo(BASE_VERSION) > 0;
		}

	/**
	 *  This is called from the Windows bootstrap process instead of main(), when
	 *  the user tries to open a new DataWarrior instance while one is already running.
	 * @param args
	 */
	public static void initSingleApplication(String[] args) {
		try {
//			final Class datawarriorClass = Thread.currentThread().getContextClassLoader().loadClass(getAppClassName());
			final Method method = sDatawarriorClass.getMethod("initSingleApplication", String[].class);
			final Object[] argsHolder = new Object[1];
			argsHolder[0] = args;
			method.invoke(null, (Object)args);
			}
		catch (final Exception e) {
			e.printStackTrace();
			}
		}

	private static String getAppClassName() {
		return isMacintosh() ? "com.actelion.research.datawarrior.DataWarriorOSX" : "com.actelion.research.datawarrior.DataWarriorLinux";
		}

	private static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().indexOf("win") >= 0;
		}

	private static boolean isMacintosh() {
		return System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0;
		}
	}

