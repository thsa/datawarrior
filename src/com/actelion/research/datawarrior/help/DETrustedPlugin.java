package com.actelion.research.datawarrior.help;

import com.actelion.research.datawarrior.DataWarrior;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.prefs.Preferences;

import static com.actelion.research.datawarrior.help.DEUpdateHandler.PREFERENCES_KEY_TRUSTED_PLUGINS_FOR_REMOVAL;
import static com.actelion.research.datawarrior.help.DEUpdateHandler.PREFERENCES_KEY_UPDATE_PATH;

public class DETrustedPlugin implements Comparable<DETrustedPlugin> {
	private String mID,mVersion,mName,mSourceURL,mMD5Sum,mInfoURL,mComment;
	private boolean mIsInstalled;

	public static boolean isValidFileName(String name) {
		DETrustedPlugin plugin = new DETrustedPlugin(name);
		return plugin.getID() != null;
	}

	public DETrustedPlugin(String id, String version, String name, String sourceURL, String md5Sum, String infoURL, String comment) {
		mID = id;
		mVersion = version;
		mName = name;
		mSourceURL = sourceURL;
		mMD5Sum = md5Sum;
		mInfoURL = infoURL;
		mComment = comment;
	}

	public DETrustedPlugin(String filename) {
		filename = filename.toLowerCase();
		if (filename.startsWith("plugin_") && Character.isDigit(filename.charAt(7))) {
			int index1 = 7;
			int index2 = index1;
			while (index2 < filename.length()+1 && Character.isDigit(filename.charAt(index2)))
				index2++;
			if (filename.charAt(index2) == '_' && filename.charAt(index2+1) == 'v' && Character.isDigit(filename.charAt(index2+2)) && filename.endsWith(".jar")) {
				mID = filename.substring(index1, index2);
				mVersion = filename.substring(index2+2, filename.length()-4);
			}
		}
	}

	public String getFilename(boolean includeExtention) {
		return "plugin_".concat(mID).concat("_v").concat(mVersion).concat(includeExtention ? ".jar" : "");
	}

	public void checkInstallation(Preferences prefs) {
		String baseDir = prefs.get(PREFERENCES_KEY_UPDATE_PATH, null);
		mIsInstalled = baseDir != null && new File(baseDir.concat(File.separator).concat(getFilename(true))).exists();
	}

	public String getID() {
		return mID;
	}

	public int getNumID() {
		return Integer.parseInt(mID);
	}

	public String getVersion() {
		return mVersion;
	}

	public int getNumVersion() {
		return Integer.parseInt(mVersion.endsWith("dev") ? mVersion.substring(0, mVersion.length()-3) : mVersion);
	}

	public String getName() {
		return mName;
	}

	public String getMD5Sum() {
		return mMD5Sum;
	}

	public String getComment() {
		return mComment;
	}

	public String getInfoURL() {
		return mInfoURL;
	}

	public String getSourceURL() {
		return mSourceURL;
	}

	public boolean isInstalled() {
		return mIsInstalled;
	}

	public boolean isDevelopment() {
		return mVersion != null && mVersion.endsWith("dev");
	}

	@Override
	public int compareTo(DETrustedPlugin tp) {
		return !mID.equals(tp.mID) ? Integer.compare(getNumID(), tp.getNumID())
				: Integer.compare(getNumVersion(), tp.getNumVersion());
	}

	public boolean install() {
		String targetDir = DEUpdateHandler.getUpdatePath(DataWarrior.getApplication().getActiveFrame());
		if (targetDir == null) {
			mIsInstalled = false;
			return false;
		}

		File pluginFile = new File(targetDir.concat(File.separator.concat(getName())));
		if (pluginFile.exists()) {
			unscheduleForRemoval(); // in case it was just scheduled for removal
			mIsInstalled = true;
			return true;
		}

		Frame parent = DataWarrior.getApplication().getActiveFrame();
		if (DEUpdateHandler.downloadJarFile(parent, mSourceURL, targetDir, getFilename(false), mMD5Sum)) {
			JOptionPane.showMessageDialog(parent, "The plugin was installed successfully and will be active when you launch DataWarrior next time.");
			mIsInstalled = true;
			return true;
		}

		mIsInstalled = false;
		return false;
	}

	public boolean uninstall() {
		String targetDir = DEUpdateHandler.getUpdatePath(DataWarrior.getApplication().getActiveFrame());
		if (targetDir == null) {
			mIsInstalled = false;
			return true;
		}

		File pluginFile = new File(targetDir.concat(File.separator).concat(getFilename(true)));
		if (pluginFile.exists())
			scheduleForRemoval();

		Frame parent = DataWarrior.getApplication().getActiveFrame();
		JOptionPane.showMessageDialog(parent, "The plugin was scheduled to be removed from your harddisk.");
		mIsInstalled = false;
		return true;
	}

	private void scheduleForRemoval() {
		Preferences prefs = DataWarrior.getPreferences();
		String pluginsForRemoval = prefs.get(PREFERENCES_KEY_TRUSTED_PLUGINS_FOR_REMOVAL, null);

		String filename = getFilename(true);

		if (pluginsForRemoval == null) {
			prefs.put(PREFERENCES_KEY_TRUSTED_PLUGINS_FOR_REMOVAL, filename);
			return;
		}

		for (String fn : pluginsForRemoval.split(","))
			if (fn.equals(filename))
				return;

		prefs.put(PREFERENCES_KEY_TRUSTED_PLUGINS_FOR_REMOVAL, pluginsForRemoval+","+filename);
	}

	private void unscheduleForRemoval() {
		Preferences prefs = DataWarrior.getPreferences();
		String pluginsForRemoval = prefs.get(PREFERENCES_KEY_TRUSTED_PLUGINS_FOR_REMOVAL, null);

		if (pluginsForRemoval == null)
			return;

		String filename = getFilename(true);

		if (pluginsForRemoval.equals(filename)) {
			prefs.remove(PREFERENCES_KEY_TRUSTED_PLUGINS_FOR_REMOVAL);
			return;
		}

		int index = pluginsForRemoval.indexOf(filename);
		if (index == -1)
			return;

		prefs.put(PREFERENCES_KEY_TRUSTED_PLUGINS_FOR_REMOVAL, pluginsForRemoval.endsWith(filename) ?
			  pluginsForRemoval.substring(0, index-1)
			: pluginsForRemoval.substring(0, index).concat(pluginsForRemoval.substring(index+filename.length()+1)));
	}
}
