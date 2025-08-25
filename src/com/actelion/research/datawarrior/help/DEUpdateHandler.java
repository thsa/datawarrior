/*
 * Copyright 2023 Thomas Sander, 4153 Reinach, Switzerland
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

package com.actelion.research.datawarrior.help;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.AbstractTask;
import com.actelion.research.datawarrior.task.db.BBCommunicator;
import com.actelion.research.datawarrior.task.db.CODCommunicator;
import com.actelion.research.datawarrior.task.db.ChemblCommunicator;
import com.actelion.research.datawarrior.task.db.PatentReactionCommunicator;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.util.BrowserControl;
import com.actelion.research.util.Platform;
import info.clearthought.layout.TableLayout;
import org.openmolecules.comm.ServerErrorException;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.prefs.Preferences;

public class DEUpdateHandler extends JDialog implements ActionListener {
	private static final String URL1 = "https://dwversion.openmolecules.org";
	private static final String URL2 = "http://datawarrior.org:8084";
	private static final String DEFAULT_UPDATE_URL = "https://openmolecules.org/datawarrior/update";
	private static final String DEFAULT_PLUGIN_URL = "https://openmolecules.org/datawarrior/plugin";

	// IMPORTANT: When creating a new manual(!!!) installer (not an update for automatic deployment),
	// then DataWarriorLauncher.BASE_VERSION must also be changed to match this DATAWARRIOR_VERSION!
	public static final String DATAWARRIOR_VERSION = "v06.05.01";	// format must be v00.00.00

	private static final String PREFERENCES_2ND_POST_INSTALL_INFO_SERVER = "2nd_post_install_info_server";
	public static final String PREFERENCES_POST_INSTALL_INFO_FAILURE_MILLIS = "post_install_info_failure_time";
	public static final String PREFERENCES_KEY_LEGACY_UPDATE_CHECK = "automatic_update_check";
	public static final String PREFERENCES_KEY_UPDATE_MODE = "update_mode";
	public static final String[] PREFERENCES_UPDATE_MODE_CODE = {"auto", "ask", "never"};
	public static final String[] PREFERENCES_UPDATE_MODE_TEXT = {"Automatic", "Ask Whether To Update", "Never Update"};
	public static final int PREFERENCES_UPDATE_MODE_AUTO = 0;
	public static final int PREFERENCES_UPDATE_MODE_ASK = 1;
	public static final int PREFERENCES_UPDATE_MODE_NEVER = 2;
	public static final String PREFERENCES_KEY_UPDATE_PATH = "update_path";
	public static final String PREFERENCES_KEY_HANDLED_NEWS_IDS = "handledNewsIDs";
	public static final String PREFERENCES_KEY_TRUSTED_PLUGINS_FOR_REMOVAL = "trusted_plugins_for_removal";

	private static final String PROPERTY_2ND_POST_INSTALL_INFO_SERVER = "2nd_post_install_info_server";
	private static final String PROPERTY_BB_SERVER_1 = "bb_server_1";
	private static final String PROPERTY_BB_SERVER_2 = "bb_server_2";
	private static final String PROPERTY_CHEMBL_SERVER_1 = "chembl_server_1";
	private static final String PROPERTY_CHEMBL_SERVER_2 = "chembl_server_2";
	private static final String PROPERTY_COD_SERVER_1 = "cod_server_1";
	private static final String PROPERTY_COD_SERVER_2 = "cod_server_2";
	private static final String PROPERTY_PRXN_SERVER_1 = "prxn_server_1";
	private static final String PROPERTY_PRXN_SERVER_2 = "prxn_server_2";
	private static final String PROPERTY_AUTO_UPDATE_VERSION = "auto_update_version"; // format v00.00.00
	private static final String PROPERTY_AUTO_UPDATE_REQUIRED_BASE_VERSION = "auto_base_version"; // format v00.00.00
	private static final String PROPERTY_AUTO_UPDATE_URL = "auto_update_url";
	private static final String PROPERTY_AUTO_UPDATE_MD5SUM = "auto_update_md5sum";
	private static final String PROPERTY_MANUAL_UPDATE_VERSION = "manual_update_version"; // format v00.00.00
	private static final String PROPERTY_MANUAL_UPDATE_DETAIL = "manual_update_detail";
	private static final String PROPERTY_MANUAL_UPDATE_URL = "manual_update_url";

	private static final String PROPERTY_NEWS_TITLE = "news_title_";
	private static final String PROPERTY_NEWS_TEXT = "news_text_";
	private static final String PROPERTY_NEWS_IMAGE = "news_image_";
	private static final String PROPERTY_NEWS_URL = "news_url_";
	private static final String PROPERTY_NEWS_TYPE = "news_type_";
	private static final String PROPERTY_NEWS_TILLDATE = "news_tilldate_";
	private static final String PROPERTY_NEWS_OS = "news_os_";
	private static final String PROPERTY_NEWS_MIN_VERSION = "news_minversion_";
	private static final String PROPERTY_NEWS_MAX_VERSION = "news_maxversion_";

	private static final String PROPERTY_PLUGIN_SOURCE_URL = "plugin_source_url_";
	private static final String PROPERTY_PLUGIN_NAME = "plugin_name_";
	private static final String PROPERTY_PLUGIN_VERSION = "plugin_version_";
	private static final String PROPERTY_PLUGIN_MD5SUM = "plugin_md5sum_";
	private static final String PROPERTY_PLUGIN_INFO_URL = "plugin_info_url_";
	private static final String PROPERTY_PLUGIN_COMMENT = "plugin_comment_";

	private static final char[] DIGITS = { '0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};

	private static final long serialVersionUID = 20230822;
//	private static final String BROKEN_FILE_NAME = "broken_datawarrior.jar";

	private static volatile boolean sOK,sIsUpdating;
	private static volatile Properties sPostInstallInfo;

	public static boolean isUpdating() {
		return sIsUpdating;
		}

	public static void getPostInstallInfoAndHandleUpdates(DEFrame parent) {
		new Thread(() -> {
			Preferences prefs = DataWarrior.getPreferences();

			sPostInstallInfo = new Properties();
			String id = ""+prefs.getLong(DataWarrior.PREFERENCES_KEY_FIRST_LAUNCH, 0L);
			String os = System.getProperty("os.name").replace(' ', '_');
			String params = "?what=properties&os="+os+"&current="+DATAWARRIOR_VERSION+"&id="+id;
			String error = getPostInstallInfo(URL1+params);
			if (error != null) {
				String url = prefs.get(PREFERENCES_2ND_POST_INSTALL_INFO_SERVER, null);
				if (url != null)
					error = getPostInstallInfo(url + params);
				}
			if (error != null)
				error = getPostInstallInfo(URL2 + params);

			boolean consistentFailure = false;
			if (error == null) {
				updateServerURLs(prefs);
				handlePostInstallMessages(parent, prefs);
				prefs.putLong(PREFERENCES_POST_INSTALL_INFO_FAILURE_MILLIS, 0L);
				}
			else {
				long pipf_millis = prefs.getLong(PREFERENCES_POST_INSTALL_INFO_FAILURE_MILLIS, 0L);
				long current = System.currentTimeMillis();
				if (pipf_millis == 0L)
					prefs.putLong(PREFERENCES_POST_INSTALL_INFO_FAILURE_MILLIS, current);

				// if recent retrieval(s) were all failures and if first of them was more than 24 hours ago
				if (pipf_millis != 0L && current-pipf_millis > 86400000L)
					consistentFailure = true;
				}

			String modeString = prefs.get(PREFERENCES_KEY_UPDATE_MODE, PREFERENCES_UPDATE_MODE_CODE[PREFERENCES_UPDATE_MODE_ASK]);
			int updateMode = AbstractTask.findListIndex(modeString, PREFERENCES_UPDATE_MODE_CODE, PREFERENCES_UPDATE_MODE_ASK);
			if (updateMode != PREFERENCES_UPDATE_MODE_NEVER) {
				if (consistentFailure)
					askForBrowser(URL1 + params, parent, prefs, error);
				else
					handleUpdate(parent, updateMode);
				}

			if (!sPostInstallInfo.isEmpty())	// skip creating trusted plugin menu, if we couldn't download any data
				handleTrustesPlugins(parent, prefs);
			} ).start();
		}

	private static void updateServerURLs(Preferences prefs) {
		String current2ndURL = prefs.get(PREFERENCES_2ND_POST_INSTALL_INFO_SERVER, null);
		String new2ndURL = sPostInstallInfo.getProperty(PROPERTY_2ND_POST_INSTALL_INFO_SERVER);
		if (new2ndURL == null) {
			if (current2ndURL != null)
				prefs.remove(PREFERENCES_2ND_POST_INSTALL_INFO_SERVER);
			}
		else if (!new2ndURL.equals(current2ndURL)) {
			prefs.put(PREFERENCES_2ND_POST_INSTALL_INFO_SERVER, new2ndURL);
			}

		String bbServerURL1 = sPostInstallInfo.getProperty(PROPERTY_BB_SERVER_1);
		if (bbServerURL1 != null)
			BBCommunicator.setPrimaryServerURL(bbServerURL1);
		String bbServerURL2 = sPostInstallInfo.getProperty(PROPERTY_BB_SERVER_2);
		if (bbServerURL2 != null)
			BBCommunicator.setSecondaryServerURL(bbServerURL2);

		String chemblServerURL1 = sPostInstallInfo.getProperty(PROPERTY_CHEMBL_SERVER_1);
		if (chemblServerURL1 != null)
			ChemblCommunicator.setPrimaryServerURL(chemblServerURL1);
		String chemblServerURL2 = sPostInstallInfo.getProperty(PROPERTY_CHEMBL_SERVER_2);
		if (chemblServerURL2 != null)
			ChemblCommunicator.setSecondaryServerURL(chemblServerURL2);

		String codServerURL1 = sPostInstallInfo.getProperty(PROPERTY_COD_SERVER_1);
		if (codServerURL1 != null)
			CODCommunicator.setPrimaryServerURL(codServerURL1);
		String codServerURL2 = sPostInstallInfo.getProperty(PROPERTY_COD_SERVER_2);
		if (codServerURL2 != null)
			CODCommunicator.setSecondaryServerURL(codServerURL2);

		String prxnServerURL1 = sPostInstallInfo.getProperty(PROPERTY_PRXN_SERVER_1);
		if (prxnServerURL1 != null)
			PatentReactionCommunicator.setPrimaryServerURL(prxnServerURL1);
		String prxnServerURL2 = sPostInstallInfo.getProperty(PROPERTY_PRXN_SERVER_2);
		if (prxnServerURL2 != null)
			PatentReactionCommunicator.setSecondaryServerURL(prxnServerURL2);
		}

	private static void handlePostInstallMessages(DEFrame parent, Preferences prefs) {
		Set<String> propertyNames = sPostInstallInfo.stringPropertyNames();
		TreeMap<String, DENews> newsMap = new TreeMap<>();
		for (String propertyName : propertyNames) {
			if (propertyName.startsWith(PROPERTY_NEWS_TITLE)) {
				String newsID = propertyName.substring(PROPERTY_NEWS_TITLE.length());

				String os = sPostInstallInfo.getProperty(PROPERTY_NEWS_OS.concat(newsID));
				if (os != null
				 && ((os.equals("windows") && !Platform.isWindows())
				  || (os.equals("macintosh") && !Platform.isMacintosh())
				  || (os.equals("linux") && !Platform.isLinux())))
					continue;

				String minVersion = sPostInstallInfo.getProperty(PROPERTY_NEWS_MIN_VERSION.concat(newsID));
				if (minVersion != null && minVersion.compareTo(DATAWARRIOR_VERSION) > 0)
					continue;

				String maxVersion = sPostInstallInfo.getProperty(PROPERTY_NEWS_MAX_VERSION.concat(newsID));
				if (maxVersion != null && maxVersion.compareTo(DATAWARRIOR_VERSION) < 0)
					continue;

				String title = sPostInstallInfo.getProperty(propertyName);
				String text = sPostInstallInfo.getProperty(PROPERTY_NEWS_TEXT.concat(newsID));
				String image = sPostInstallInfo.getProperty(PROPERTY_NEWS_IMAGE.concat(newsID));
				String url = sPostInstallInfo.getProperty(PROPERTY_NEWS_URL.concat(newsID));
				String type = sPostInstallInfo.getProperty(PROPERTY_NEWS_TYPE.concat(newsID));
				String tillDate = sPostInstallInfo.getProperty(PROPERTY_NEWS_TILLDATE.concat(newsID));
				newsMap.put(newsID, new DENews(title, text, image, url, type, tillDate));
				}
			}

		String oldHandledNewsIDs = prefs.get(PREFERENCES_KEY_HANDLED_NEWS_IDS, "");
		StringBuilder newHandledNewsIDs = new StringBuilder(":");

		for (String newsID : newsMap.keySet()) {
			DENews news = newsMap.get(newsID);
			if (news.isPermanent())
				parent.getMainFrame().getMainPane().setPermanentNews(news);
			else if (!oldHandledNewsIDs.contains(":".concat(newsID).concat(":")))
				news.show();

			newHandledNewsIDs.append(newsID);
			newHandledNewsIDs.append(":");
			}

		SwingUtilities.invokeLater(() -> parent.getDEMenuBar().updateNewsMenu(newsMap) );

		prefs.put(PREFERENCES_KEY_HANDLED_NEWS_IDS, newHandledNewsIDs.toString());
		}

	private static void handleUpdate(DEFrame parent, int updateMode) {
		String availableInstaller = sPostInstallInfo.getProperty(PROPERTY_MANUAL_UPDATE_VERSION);
		if (availableInstaller != null
				&& availableInstaller.matches("v\\d\\d\\.\\d\\d\\.\\d\\d")
				&& availableInstaller.compareTo(DATAWARRIOR_VERSION) > 0) {
			String url = sPostInstallInfo.getProperty(PROPERTY_MANUAL_UPDATE_URL, "URL unexpectedly not available.");
			String detail = sPostInstallInfo.getProperty(PROPERTY_MANUAL_UPDATE_DETAIL, "Update detail information not available.");
			SwingUtilities.invokeLater(() ->
					new DEUpdateHandler(parent, availableInstaller, url, detail).setVisible(true) );
			return;
		}

		String availableVersion = sPostInstallInfo.getProperty(PROPERTY_AUTO_UPDATE_VERSION);
		if (availableVersion == null
				|| !availableVersion.matches("v\\d\\d\\.\\d\\d\\.\\d\\d")
				|| availableVersion.compareTo(DATAWARRIOR_VERSION) <= 0)
			return;

		String requiredBaseVersion = sPostInstallInfo.getProperty(PROPERTY_AUTO_UPDATE_REQUIRED_BASE_VERSION);
		if (requiredBaseVersion != null
				&& requiredBaseVersion.matches("v\\d\\d\\.\\d\\d\\.\\d\\d")
				&& requiredBaseVersion.compareTo(DATAWARRIOR_VERSION) > 0) {
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
					"DataWarrior couldn't update automatically, because the installed version '"+DATAWARRIOR_VERSION+"'\nis too old. Required is at least '"+requiredBaseVersion+"'.\nTry updating manually by using either the official installer or the development\npatch files from https://openmolecules.org/datawarrior/download.html",
					"Update Failed", JOptionPane.ERROR_MESSAGE));
			return;
		}

		String updateURL = sPostInstallInfo.getProperty(PROPERTY_AUTO_UPDATE_URL, DEFAULT_UPDATE_URL);
		if (updateURL == null)
			return;

		if (updateMode == PREFERENCES_UPDATE_MODE_ASK) {
			try {
				SwingUtilities.invokeAndWait(() ->
						sOK = JOptionPane.showConfirmDialog(parent,
								"A DataWarrior update (version: "+availableVersion+") is available. You use "+DATAWARRIOR_VERSION+"\n"
										+ "Do you want to download the update and use it, when you open DataWarrior next time?",
								"Download DataWarrior Update?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION);
			} catch (Exception ie) {}
			if (!sOK)
				return;
		}

		String targetDir = getUpdatePath(parent);
		if (targetDir != null) {
			String md5sum = sPostInstallInfo.getProperty(PROPERTY_AUTO_UPDATE_MD5SUM);
			final String fileNameNoExtention = "datawarrior_"+availableVersion;
			if (downloadJarFile(parent, updateURL, targetDir, fileNameNoExtention, md5sum)) {
				// remove older updates. Just keep the three newest ones
				try {
					File[] files = new File(targetDir).listFiles(file -> isDataWarriorUpdateJar(file));
					if (files != null && files.length > 3) {
						Arrays.sort(files, Comparator.comparing(File::getName));
						for (int i=0; i<files.length-3; i++)
							files[i].delete();
					}
				}
				catch (Exception e) {}

				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
						"DataWarrior successfully updated to version "+availableVersion+".\nIt will be used, when you open DataWarrior next time.",
						"Successfully Updated", JOptionPane.INFORMATION_MESSAGE));
			}
		}
	}

	public static boolean downloadJarFile(Frame parent, String sourceURL, String targetDir, String fileNameNoExtention, String md5sum) {
		final String tempFilePath = targetDir.concat(File.separator).concat(fileNameNoExtention).concat(".temp");
		final String finalFilePath = targetDir.concat(File.separator).concat(fileNameNoExtention).concat(".jar");

		// Don't download, if we have that file already
		File finalFile = new File(finalFilePath);
		if (finalFile.exists()) {
			if (md5sum == null || md5sum.equalsIgnoreCase(md5sum(finalFilePath)))
				return true;

			// If we have an older file with unexpected md5sum, then delete that.
			boolean success = false;
			String msg = null;
			try {
				success = finalFile.delete();
			}
			catch (SecurityException e) {
				msg = e.getMessage();
			}
			if (!success) {
				final String msgLine = (msg == null) ? "" : "\nMessage: "+msg;
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
						"DataWarrior could not delete broken file '"+finalFilePath+"'.\nTry deleting it manually!"+msgLine,
						"Deletion Failed", JOptionPane.ERROR_MESSAGE));
				return false;
			}
		}

		File tempFile = new File(tempFilePath);
		if (tempFile.exists()) {
			if (md5sum == null || md5sum.equalsIgnoreCase(md5sum(tempFilePath))) {
				boolean success = false;
				String msg = null;
				try {
					success = tempFile.renameTo(finalFile);
				}
				catch (SecurityException e) {
					msg = e.getMessage();
				}
				if (!success) {
					final String msgLine = (msg == null) ? "" : "\nMessage: "+msg;
					SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
							"DataWarrior could not rename successfully downloaded '" + tempFilePath + "'."+msgLine,
							"File Rename Failed", JOptionPane.ERROR_MESSAGE));
				}
				return success;
			}

			// If we have an older file with unexpected md5sum, then delete that.
			boolean success = false;
			String msg = null;
			try {
				success = tempFile.delete();
			}
			catch (SecurityException e) {
				msg = e.getMessage();
			}
			if (!success) {
				final String msgLine = (msg == null) ? "" : "\nMessage: "+msg;
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
						"DataWarrior could not delete broken file '"+tempFilePath+"'.\nTry deleting it manually!"+msgLine,
						"Deletion Failed", JOptionPane.ERROR_MESSAGE));
				return false;
			}
		}

		try {
			sIsUpdating = true;
			URL url = new URL(sourceURL+"/"+fileNameNoExtention+".jar");
			ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
			FileOutputStream fileOutputStream = new FileOutputStream(tempFilePath);
			FileChannel fileChannel = fileOutputStream.getChannel();
			fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
			fileChannel.close();
			fileOutputStream.close();
			if (md5sum != null) {
				String checksum = md5sum(tempFilePath);
				if (!md5sum.equalsIgnoreCase(checksum)) {
					SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
							"DataWarrior could download and write file '"+tempFilePath
									+ "', but its md5sum\n"+checksum+" does not match the expected one "+md5sum,
							"MD5 Mismatch", JOptionPane.ERROR_MESSAGE));
					sIsUpdating = false;
					return false;
				}
			}
			try {
				boolean success = tempFile.renameTo(finalFile);
				if (!success)
					SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
							"The plugin file was successfully downloaded, but DataWarrior could not change\nits extention from '.temp' to '.jar': "+tempFilePath,
							"File Rename Failed", JOptionPane.ERROR_MESSAGE));
				sIsUpdating = false;
				return success;
			}
			catch (SecurityException e) {
				SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
						"DataWarrior could not rename successfully downloaded '"+tempFilePath+"'.\nMessage: "+e.getMessage(),
						"File Rename Failed", JOptionPane.ERROR_MESSAGE));
				sIsUpdating = false;
				return false;
			}
		}
		catch (IOException ioe) {
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
					"DataWarrior could not download or write file '"+tempFilePath+"'.\nMessage: "+ioe.getMessage(),
					"Download Failed", JOptionPane.ERROR_MESSAGE));
		}
		catch (Throwable t) {
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
					"Unexpected failure in DataWarrior update procedure\nMessage: "+t.getMessage(),
					"DataWarrior Update Failed", JOptionPane.ERROR_MESSAGE));
		}
		sIsUpdating = false;
		return false;
	}

	private static void handleTrustesPlugins(DEFrame parent, Preferences prefs) {
		Set<String> propertyNames = sPostInstallInfo.stringPropertyNames();
		TreeMap<String, DETrustedPlugin> pluginMap = new TreeMap<>();
		for (String propertyName : propertyNames) {
			if (propertyName.startsWith(PROPERTY_PLUGIN_NAME)) {
				String pluginID = propertyName.substring(PROPERTY_PLUGIN_NAME.length());
				String name = sPostInstallInfo.getProperty(propertyName);
				String version = sPostInstallInfo.getProperty(PROPERTY_PLUGIN_VERSION.concat(pluginID));
				String md5sum = sPostInstallInfo.getProperty(PROPERTY_PLUGIN_MD5SUM.concat(pluginID));
				String infoURL = sPostInstallInfo.getProperty(PROPERTY_PLUGIN_INFO_URL.concat(pluginID));
				String sourceURL = sPostInstallInfo.getProperty(PROPERTY_PLUGIN_SOURCE_URL.concat(pluginID), DEFAULT_PLUGIN_URL);
				String comment = sPostInstallInfo.getProperty(PROPERTY_PLUGIN_COMMENT.concat(pluginID));
				DETrustedPlugin plugin = new DETrustedPlugin(pluginID, version, name, sourceURL, md5sum, infoURL, comment);
				plugin.checkInstallation(prefs);
				if (!plugin.isDevelopment() || System.getProperty("development") != null)
					pluginMap.put(pluginID, plugin);
			}
		}

		// List outdated plugins and update those that have an updated version available.
		String targetDir = prefs.get(PREFERENCES_KEY_UPDATE_PATH, null);
		try {
			if (targetDir != null) {
				File[] files = new File(targetDir).listFiles(file -> !file.isDirectory() && DETrustedPlugin.isValidFileName(file.getName()));
				if (files != null) {
					TreeSet<String> updatedPlugins = new TreeSet<>();
					String filesForDeletion = null;
					for (File f:files) {
						DETrustedPlugin plugin = new DETrustedPlugin(f.getName());
						DETrustedPlugin newPlugin = pluginMap.get(plugin.getID());
						boolean needsUpdate = newPlugin != null
							 && !plugin.getVersion().equals(newPlugin.getVersion());
						boolean deleteOutdated = newPlugin == null || needsUpdate;

						if (needsUpdate
						 && downloadJarFile(parent, newPlugin.getSourceURL(), targetDir, newPlugin.getFilename(false), newPlugin.getMD5Sum())
						 && !updatedPlugins.contains(plugin.getID())) {
							updatedPlugins.add(plugin.getID());
							SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "The trusted plugin '" + newPlugin.getName() + "' was successfully updated\nand will be used when you launch DataWarrior next time."));
							}

						if (deleteOutdated)
							filesForDeletion = (filesForDeletion == null) ? f.getName() : filesForDeletion+","+f.getName();
					}
					if (filesForDeletion == null)
						prefs.remove(PREFERENCES_KEY_TRUSTED_PLUGINS_FOR_REMOVAL);
					else
						prefs.put(PREFERENCES_KEY_TRUSTED_PLUGINS_FOR_REMOVAL, filesForDeletion);
				}
			}
		}
		catch (Exception e) {
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent, "Could not delete outdated trusted plugin files from '"+targetDir+"'.\n"+e.getMessage()));
		}

		SwingUtilities.invokeLater(() -> {
			parent.getDEMenuBar().updateTrustedPluginMenu(pluginMap);
		} );
	}

	private static String md5sum(String path) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(Files.readAllBytes(Paths.get(path)));
			return encodeHex(md.digest());
			}
		catch (Exception e) {
			e.printStackTrace();
			return "";
			}
		}

	private static String encodeHex(final byte[] data) {
		StringBuilder sb = new StringBuilder();
		for (byte b:data) {
			sb.append(DIGITS[(0xF0 & b) >>> 4]);
			sb.append(DIGITS[0x0F & b]);
			}
		return sb.toString();
		}

	private static String getPostInstallInfo(final String url) {
		try {
			InputStream is = new URI(url).toURL().openStream();
			if (is != null) {
				sPostInstallInfo.load(is);
				is.close();
				return null;
			}
		}
		catch (MalformedURLException mue) {
			mue.printStackTrace();
		}
		catch (ServerErrorException see) {  // server reached, but could not satisfy request
			return see.toString();
		}
		catch (ConnectException ce) {  // connection refused
			return ce.toString();
		}
		catch (SocketTimeoutException ste) {  // timed out
			return ste.toString();
		}
		catch (Exception e) {
			return e.toString();
		}
		return "Unexpected error when getting post install info.";
	}

	private static boolean isDataWarriorUpdateJar(File file) {
		// same logic as in DataWarriorLauncher.fileQualifies(File file)
		if (file.isDirectory())
			return false;

		String filename = file.getName().toLowerCase();
		return filename.startsWith("datawarrior_v")
				&& filename.endsWith(".jar")
				&& filename.length() == 25;
		}

	public static String getUpdatePath(final DEFrame parent) {
		Preferences prefs = DataWarrior.getPreferences();
		String baseDir = prefs.get(PREFERENCES_KEY_UPDATE_PATH, null);
		if (baseDir != null)
			baseDir = getWritableDir(baseDir, null);

		if (baseDir == null)
			baseDir = getWritableDir(parent.getApplication().getRootPath(), "update");

		if (baseDir == null) {
			if (Platform.isWindows()) {
				baseDir = getWritableDir("C:\\ProgramData", "DataWarrior");
				if (baseDir == null)
					baseDir = getWritableDir(System.getenv("ProgramData"), "DataWarrior");
				if (baseDir == null)
					baseDir = getWritableDir(System.getenv("AppData"), "DataWarrior");
				if (baseDir == null)
					baseDir = getWritableDir(System.getProperty("user.home"), ".datawarrior");
				}
			else if (Platform.isMacintosh()) {
				baseDir = getWritableDir("/Applications/DataWarrior.app/Contents/Java", "update");
				if (baseDir == null)
					baseDir = getWritableDir(System.getProperty("user.home")+"/Library/Application Support", "DataWarrior");
				if (baseDir == null)
					baseDir = getWritableDir(System.getProperty("user.home"), ".datawarrior");
				}
			else {  // Linux
				baseDir = getWritableDir("/opt/datawarrior", "update");
				if (baseDir == null)
					baseDir = getWritableDir(System.getProperty("user.home"), ".datawarrior");
				}
			}

		if  (baseDir == null) {
			JOptionPane.showMessageDialog(parent, "None of the default directories allows to write a file.\nPlease select a directory to permanently store DataWarrior update files!", "Select Update Directory", JOptionPane.WARNING_MESSAGE);
			while (true) {
				JFileChooser fileChooser = new JFileChooser(System.getProperty("user.home"));
				fileChooser.setDialogTitle("Select Directory For Update Files");
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fileChooser.showOpenDialog(parent);
				if (fileChooser.getSelectedFile() == null)
					break;

				baseDir = getWritableDir(fileChooser.getSelectedFile().getAbsolutePath(), "datawarrior_update");
				if (baseDir != null)
					break;

				JOptionPane.showMessageDialog(parent, "Cannot create a directory in "+fileChooser.getSelectedFile().getAbsolutePath()+".\nPlease select a directory to permanently store DataWarrior update files!", "Select Update Directory", JOptionPane.WARNING_MESSAGE);
				}
			}

		if (baseDir != null)
			prefs.put(PREFERENCES_KEY_UPDATE_PATH, baseDir);

		return baseDir;
		}

	private static String getWritableDir(String baseDirName, String dirName) {
		File baseDir = new File(baseDirName);
		if (!baseDir.exists())
			return null;

		String writabelDirName = baseDirName;
		try {
			if (dirName != null) {
				writabelDirName = baseDirName.concat(File.separator).concat(dirName);
				File file = new File(writabelDirName);
				if (!file.exists() && !file.mkdir())
					return null;
				}

			File testFile = new File(writabelDirName.concat(File.separator).concat("emptyABCXYZ.txt"));
			return (testFile.createNewFile() && testFile.delete()) ? writabelDirName : null;
			}
		catch (IOException e) {
			return null;
			}
		}

	private static void askForBrowser(final String url, final Frame parent, final Preferences prefs, final String error) {
		long lastErrorMillis = prefs.getLong(DataWarrior.PREFERENCES_KEY_LAST_VERSION_ERROR, 0L);
		if (System.currentTimeMillis() > lastErrorMillis + 86400L) {
			prefs.putLong(DataWarrior.PREFERENCES_KEY_LAST_VERSION_ERROR, System.currentTimeMillis());
			SwingUtilities.invokeLater(() -> {
				int answer = JOptionPane.showConfirmDialog(parent,
						"DataWarrior could not check whether you have the latest version.\n"
								+ "A firewall, local security software, or settings may prevent contacting the server.\n"
								+ "Do you want DataWarrior to open your web browser to check for an update?",
						"DataWarrior Update Check in Web-Browser", JOptionPane.YES_NO_OPTION);
				if (answer == JOptionPane.YES_OPTION) {
					BrowserControl.displayURL(url+"&error="+URLEncoder.encode(error, StandardCharsets.UTF_8));
				}
				});
			}
		}

	private DEUpdateHandler(Frame parent, String version, String updateURL, String text) {
		super(parent, "DataWarrior Update", true);
		double[][] size = { {8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 16, TableLayout.PREFERRED,
							 16, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 24, TableLayout.PREFERRED, 8} };
		getContentPane().setLayout(new TableLayout(size));
		getContentPane().add(new JLabel("A DataWarrior update is available, which requires manual installation.", JLabel.CENTER), "1,1");
		getContentPane().add(new JLabel("Available version: "+version+" (Currently installed version: "+DATAWARRIOR_VERSION+")", JLabel.CENTER), "1,3");

//		JEditorPane ep = new JEditorPane();
//		ep.setEditable(false);
//		ep.setContentType("text/plain");
//		ep.setText(text);
		JTextArea ep = new JTextArea();
		ep.setLineWrap(true);
		ep.setWrapStyleWord(true);
		ep.setText(text);

		JScrollPane sp = new JScrollPane(ep, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sp.setPreferredSize(new Dimension(HiDPIHelper.scale(540), HiDPIHelper.scale(240)));
		SwingUtilities.invokeLater(() -> sp.getVerticalScrollBar().setValue(sp.getVerticalScrollBar().getMinimum()));

		getContentPane().add(sp, "1,5");

		getContentPane().add(new JLabel("You may download the updated DataWarrior installer at: ", JLabel.CENTER), "1,7");
		getContentPane().add(new JLabel(updateURL, JLabel.CENTER), "1,9");

		JButton urlButton = new JButton("Open Web-Browser");
		urlButton.addActionListener(this);
		urlButton.setActionCommand("open"+updateURL);

		JButton copyButton = new JButton("Copy Link");
		copyButton.addActionListener(this);
		copyButton.setActionCommand("copy"+updateURL);

		JButton closeButton = new JButton("Update Later");
		closeButton.addActionListener(this);

		double[][] bpsize = { {TableLayout.PREFERRED, TableLayout.FILL, TableLayout.PREFERRED, TableLayout.FILL, TableLayout.PREFERRED},
							  {TableLayout.PREFERRED} };
		JPanel bp = new JPanel();
		bp.setLayout(new TableLayout(bpsize));
		bp.add(copyButton, "0,0");
		bp.add(urlButton, "2,0");
		bp.add(closeButton, "4,0");
		getContentPane().add(bp, "1,11");

		pack();
		setLocationRelativeTo(parent);
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().startsWith("copy")) {
			StringSelection theData = new StringSelection(e.getActionCommand().substring(4));
	    	Toolkit.getDefaultToolkit().getSystemClipboard().setContents(theData, theData);
			return;
			}
		if (e.getActionCommand().startsWith("open")) {
			BrowserControl.displayURL(e.getActionCommand().substring(4));
			return;
			}

		setVisible(false);
		}
	}
