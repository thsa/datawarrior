package com.actelion.research.datawarrior.help;

import com.actelion.research.datawarrior.DataWarrior;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.prefs.Preferences;

import static com.actelion.research.datawarrior.help.DEUpdateHandler.PREFERENCES_KEY_UPDATE_PATH;

public class DETrustedPlugin {
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

	public String getVersion() {
		return mVersion;
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

	public boolean install() {
		String targetDir = DEUpdateHandler.getUpdatePath(DataWarrior.getApplication().getActiveFrame());
		if (targetDir == null) {
			mIsInstalled = false;
			return false;
		}

		File pluginFile = new File(targetDir.concat(File.separator.concat(getName())));
		if (pluginFile.exists()) {
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

		File pluginFile = new File(targetDir.concat(File.separator.concat(getFilename(true))));
		if (!pluginFile.exists() || pluginFile.delete()) {
			Frame parent = DataWarrior.getApplication().getActiveFrame();
			JOptionPane.showMessageDialog(parent, "The plugin was removed from your harddisk successfully.");
			mIsInstalled = false;
			return true;
		}

		mIsInstalled = true;
		return false;
	}
}
