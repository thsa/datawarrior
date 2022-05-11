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

import com.actelion.research.chem.Molecule;
import com.actelion.research.chem.name.StructureNameResolver;
import com.actelion.research.chem.reaction.mapping.ChemicalRuleEnhancedReactionMapper;
import com.actelion.research.datawarrior.plugin.PluginRegistry;
import com.actelion.research.datawarrior.task.DEMacroRecorder;
import com.actelion.research.datawarrior.task.DETaskSelectWindow;
import com.actelion.research.datawarrior.task.StandardTaskFactory;
import com.actelion.research.datawarrior.task.file.DETaskOpenFile;
import com.actelion.research.datawarrior.task.file.DETaskRunMacroFromFile;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.HeaderPaintHelper;
import com.actelion.research.gui.editor.GenericEditorArea;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableDetailHandler;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.Platform;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.io.IOException;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.prefs.Preferences;

public abstract class DataWarrior implements WindowFocusListener {
	public static final String PROGRAM_NAME = "DataWarrior";

	private static final String PREFERENCES_ROOT = "org.openmolecules.datawarrior";
	public static final String PREFERENCES_KEY_FIRST_LAUNCH = "first_launch";
	public static final String PREFERENCES_KEY_LAST_VERSION_ERROR = "last_version_error";
	public static final String PREFERENCES_KEY_AUTO_UPDATE_CHECK = "automatic_update_check";
	public static final String PREFERENCES_KEY_LAF_NAME = "laf_name";
	public static final String PREFERENCES_KEY_SPAYA_SERVER = "spaya_server";

	public static boolean USE_CARDS_VIEW = false;

	public static final String[] RESOURCE_DIR = { "Reference", "Example", "Tutorial" };
	public static final String MACRO_DIR = "Macro";
	public static final String PLUGIN_DIR = "Plugin";

	public enum LookAndFeel {
		GRAPHITE("Graphite", "org.pushingpixels.substance.api.skin.SubstanceGraphiteAquaLookAndFeel", new Color(0x3838C0), new Color(0x252560)),
		GRAY("Gray", "org.pushingpixels.substance.api.skin.SubstanceOfficeBlack2007LookAndFeel", new Color(0xAEDBFF), new Color(0x0060FF)),
		CREME("Creme Coffee", "org.pushingpixels.substance.api.skin.SubstanceCremeCoffeeLookAndFeel", new Color(0xDEC59D), new Color(0xAA784F)),
		MODERATE("Moderate", "org.pushingpixels.substance.api.skin.SubstanceModerateLookAndFeel", new Color(0x6D96B3), new Color(0x1E4C6F)),
		NEBULA("Nebula", "org.pushingpixels.substance.api.skin.SubstanceNebulaLookAndFeel", new Color(0xA7BBCD), new Color(0x55585E)),
		SAHARA("Sahara", "org.pushingpixels.substance.api.skin.SubstanceSaharaLookAndFeel", new Color(0xA6B473), new Color(0x6E7841)),
	//	CLASSIC("Classic", "org.jvnet.substance.SubstanceLookAndFeel"),	causes long delays when opening large files with many selected rows, because it draws the background of even invisible cells
	//	VAQUA("VAqua", "org.violetlib.aqua.AquaLookAndFeel"),
		AQUA("Aqua", "com.apple.laf.AquaLookAndFeel", new Color(0xAEDBFF), new Color(0x0060FF));

		private final String displayName;
		private String className;
		private Color c1,c2;

		LookAndFeel(String displayName, String className, Color c1, Color c2) {
			this.displayName = displayName;
			this.className = className;
			this.c1 = c1;
			this.c2 = c2;
			}
		public String displayName() { return displayName; }
		public String className() { return className; }
		public Color getColor1() { return c1; }
		public Color getColor2() { return c2; }
		}

	private static DataWarrior	sApplication;

	private ArrayList<DEFrame>	mFrameList;
	private DEFrame				mFrameOnFocus;
	private StandardTaskFactory	mTaskFactory;
	private PluginRegistry mPluginRegistry;

	public static DataWarrior getApplication() {
		return sApplication;
		}

	/**
	 * If the given path starts with a valid variable name, then this
	 * is replaced by the corresponding path on the current system and all file separators
	 * are converted to the correct ones for the current platform.
	 * Valid variable names are $HOME, $TEMP, or $PARENT.
	 * @param path possibly starting with variable, e.g. "$HOME/drugs.dwar"
	 * @return untouched path or path with resolved variable, e.g. "/home/thomas/drugs.dwar"
	 */
	public static String resolveOSPathVariables(String path) {
		if (path != null) {
			if (path.toLowerCase().startsWith("$home"))
				return System.getProperty("user.home").concat(correctFileSeparators(path.substring(5)));
			if (path.toLowerCase().startsWith("$temp"))
				try { return File.createTempFile("temp-", "tmp").getParent().concat(correctFileSeparators(path.substring(5))); } catch (IOException ioe) {}
			if (path.toLowerCase().startsWith("$parent") && sApplication != null) {
				DEFrame frame = sApplication.getActiveFrame();
				if (frame != null && frame.getTableModel().getFile() != null)
					return frame.getTableModel().getFile().getParent().concat(File.separator).concat(correctFileSeparators(path.substring(7)));
				}
			}

		return path;
		}

	/**
	 * Tries to find the directory with the specified name in the DataWarrior installation directory.
	 * resourceDir may contain capital letters, but it is converted to lower case before checked against
	 * installed resource directories, which are supposed to be lower case.
	 * @param resourceDir as shown to the user, e.g. "Example"; "" to return datawarrior installation dir
	 * @return null or full path to resource directory
	 */
	public File resolveResourcePath(String resourceDir) {
		File directory = Platform.isWindows() ?
			  new File("C:\\Program Files\\DataWarrior\\" + resourceDir.toLowerCase())
					   : Platform.isMacintosh() ?
			  new File("/Applications/DataWarrior.app/"+resourceDir.toLowerCase())
			: new File("/opt/datawarrior/"+resourceDir.toLowerCase());

		return FileHelper.fileExists(directory) ? directory : null;
		}

	/**
	 * Creates a path variable name from a resource directory name.
	 * @param resourceDir as shown to the user, e.g. "Example"
	 * @return path variable name, e.g. $EXAMPLE
	 */
	public static String makePathVariable(String resourceDir) {
		return "$"+resourceDir.toUpperCase();
	}

	/**
	 * If the given path starts with a valid variable name, then this
	 * is replaced by the corresponding path on the current system and all file separators
	 * are converted to the correct ones for the current platform.
	 * Valid variable names are $HOME, $TEMP, $PARENT or resource file names.
	 * @param path possibly starting with variable, e.g. "$EXAMPLE/drugs.dwar"
	 * @return untouched path or path with resolved variable, e.g. "/opt/datawarrior/example/drugs.dwar"
	 */
	public String resolvePathVariables(String path) {
		path = resolveOSPathVariables(path);
		if (path != null && path.startsWith("$")) {
			for (String dirName:RESOURCE_DIR) {
				String varName = makePathVariable(dirName);
				if (path.startsWith(varName)) {
					File dir = resolveResourcePath(dirName);
					if (dir != null) {
						return dir.getAbsolutePath().concat(DataWarrior.correctFileSeparators(path.substring(varName.length())));
					}
				}
			}
			String varName = makePathVariable(MACRO_DIR);
			if (path.startsWith(varName)) {
				File dir = resolveResourcePath(MACRO_DIR);
				if (dir != null)
					return dir.getAbsolutePath().concat(DataWarrior.correctFileSeparators(path.substring(varName.length())));
			}
		}
		return path;
	}

	/**
	 * If the given path starts with a valid variable name, then this
	 * is replaced by the corresponding path on the current system and all file separators
	 * are converted to the correct ones for the current platform.
	 * Valid variable names are $HOME, $TEMP, $PARENT.
	 * @param url possibly starting with variable, e.g. "$TEMP/drugs.dwar"
	 * @return untouched path or path with resolved variable
	 */
	public String resolveURLVariables(String url) {
		return resolveOSPathVariables(url);
		}

	/**
	 * Replaces all path separator of the given path with the correct ones for the current platform.
	 * @param path
	 * @return
	 */
	public static String correctFileSeparators(String path) {
		return Platform.isWindows() ? path.replace('/', '\\') : path.replace('\\', '/');
		}

	public DataWarrior() {
		mPluginRegistry = new PluginRegistry(this);
		setInitialLookAndFeel();

		mFrameList = new ArrayList<>();
		createNewFrame(null, false);
		new DEAboutDialog(mFrameOnFocus, 2000);

		initialize();

		if (!isIdorsia() && System.getProperty("development") == null) {
			try {
				Preferences prefs = getPreferences();

				long firstLaunchMillis = prefs.getLong(PREFERENCES_KEY_FIRST_LAUNCH, 0L);
				if (firstLaunchMillis == 0L) {
					prefs.putLong(PREFERENCES_KEY_FIRST_LAUNCH, System.currentTimeMillis());
					}

				if (prefs.getBoolean(PREFERENCES_KEY_AUTO_UPDATE_CHECK, true))
					checkVersion(false);
				}
			catch (Exception e) {}
			}

		mTaskFactory = createTaskFactory();
		DEMacroRecorder.getInstance().setTaskFactory(mTaskFactory);

		sApplication = this;
		}

	public StandardTaskFactory createTaskFactory() {
		return new StandardTaskFactory(this);
		}

	public DEDetailPane createDetailPane(DEFrame frame, CompoundTableModel tableModel) {
		return new DEDetailPane(frame, tableModel);
		}

	public CompoundTableDetailHandler createDetailHandler(Frame parent, CompoundTableModel tableModel) {
		return new CompoundTableDetailHandler(tableModel);
		}

	public void initialize() {
		javafx.application.Platform.setImplicitExit(false);
		ToolTipManager.sharedInstance().setDismissDelay(600000);    // 10 min
		Molecule.setDefaultAverageBondLength(HiDPIHelper.scale(24));
		StructureNameResolver.setInstance(new DEStructureNameResolver());
		GenericEditorArea.setReactionMapper(new ChemicalRuleEnhancedReactionMapper());
		}

	public void checkVersion(boolean showUpToDateMessage) {
		DEVersionChecker.checkVersion(mFrameOnFocus, showUpToDateMessage);
		}

	public StandardMenuBar createMenuBar(DEFrame frame) {
		return new StandardMenuBar(frame);
		}

	public DatabaseActions createDatabaseActions(DEFrame parent) {
		return null;
		}

	public boolean isIdorsia() {
		return false;
		}

	public StandardTaskFactory getTaskFactory() {
		return mTaskFactory;
		}

	public PluginRegistry getPluginRegistry() {
		return mPluginRegistry;
		}

	@Override
	public void windowGainedFocus(WindowEvent e) {
		for (DEFrame f:mFrameList) {
			if (f == e.getSource()
			 && mFrameOnFocus != f) {	// if mFrameOnFocus==e.getSource() then the frame was just created or a dialog was closed

				// we try to identify those changes, which are interactively caused by the user
				if (mFrameOnFocus != null	// if mFrameOnFocus==null then a frame was closed
				 && e.getOppositeWindow() instanceof DEFrame) {
					if (DEMacroRecorder.getInstance().isRecording()) {
						DETaskSelectWindow task = new DETaskSelectWindow(f, this, f);
						DEMacroRecorder.record(task, task.getPredefinedConfiguration());
						}
					}

				mFrameOnFocus = f;
				}
			}
		}

	@Override
	public void windowLostFocus(WindowEvent e) {}

	/**
	 * Creates a new DEFrame as front window that is expected to be populated with data immediately.
	 * This method can be called safely from any thread. If a modal dialog, e.g. a progress
	 * dialog is visible during the call of this method, then moving the new DEFrame to
	 * the front fails. In this case toFront() must be called on this frame after the
	 * dialog has been closed.
	 * The DEFrame returned has its CompoundTable lock set to indicate that the frame is
	 * about to be filled. When adding content fails for any reason, then this lock must
	 * be released with tableModel.unlock() to make the frame again available for other purposes.
	 * @param title use null for default title
	 * @return empty DEFrame to be populated
	 */
	public DEFrame getEmptyFrame(final String title) {
		for (DEFrame f:mFrameList)
			if (f.getMainFrame().getTableModel().isEmpty()
			 && f.getMainFrame().getTableModel().lock()) {
				f.setTitle(title);
				f.toFront();
				mFrameOnFocus = f;
				return f;
				}

		if (SwingUtilities.isEventDispatchThread()) {
			createNewFrame(title, true);
			}
		else {
			// if we are not in the event dispatcher thread we need to use invokeAndWait
			try {
				SwingUtilities.invokeAndWait(() -> createNewFrame(title,  true) );
				}
			catch (Exception e) {}
			}

		return mFrameOnFocus;
		}

	/**
	 * @param isInteractive
	 */
	public void closeApplication(boolean isInteractive) {
		while (mFrameList.size() != 0) {
			DEFrame frame = getActiveFrame();
			if (!disposeFrameSafely(frame, isInteractive))
				return;
			}

		System.exit(0);
		}

	/**
	 * If the frame contains unsaved content, then the user is asked, whether
	 * its data shall be saved. If the user cancels the dialog the frame stays open.
	 * If the frame is the owner of a running macro, then the frame is not closed
	 * and an appropriate error message is displayed unless it is the macro itself
	 * that asks to close the frame (isInteractive==false).
	 * If this frame is the only frame, then the application is exited unless
	 * we run on a Macintosh, where the frame is cleared but stays open.
	 * @param frame
	 * @param isInteractive
	 */
	public void closeFrameSafely(DEFrame frame, boolean isInteractive) {
		disposeFrameSafely(frame, isInteractive);

		if (!isMacintosh() && mFrameList.size() == 0)
			System.exit(0);
		}

	/**
	 * If the frame contains unsaved content and saveContent==true then
	 * the frame's content is saved without user interaction.
	 * If a file is already assigned to the frame then this file is overwritten.
	 * Otherwise a new file is saved in the home directory.
	 * If a macro is recording, then this call does not record any tasks.
	 * @param frame
	 * @param saveContent if true, then the window is closed without asking to save
	 */
	public void closeFrameSilently(DEFrame frame, boolean saveContent) {
		if (saveContent)
			frame.saveSilentlyIfDirty();

		disposeFrame(frame);
		}

	/**
	 * If the frame contains unsaved content, then the user is asked, whether
	 * its data shall be saved. If the user cancels the dialog the frame stays open.
	 * If the frame is the owner of a running macro, then the frame is not closed
	 * and an appropriate error message is displayed unless it is the macro itself
	 * that asks to close all frames (isInteractive==false).
	 * The application is exited after closing the last frame unless
	 * we run on a Macintosh, where the frame is cleared but stays open.
	 * @param isInteractive
	 */
	public void closeAllFramesSafely(boolean isInteractive) {
		while (mFrameList.size() != 0)
			if (!disposeFrameSafely(getActiveFrame(), isInteractive))
				return;

		if (!isMacintosh())
			System.exit(0);
		}

	/**
	 * If the frame contains unsaved content and saveContent==true then
	 * the frame's content is saved without user interaction.
	 * If a file is already assigned to the frame then this file is overwritten.
	 * Otherwise a new file is saved in the home directory.
	 * The application is exited after closing the last frame.
	 * If a macro is recording, then this call does not record any tasks.
	 */
	public void closeAllFramesSilentlyAndExit(boolean saveContent) {
		while (mFrameList.size() != 0)
			closeFrameSilently(getActiveFrame(), saveContent);

		System.exit(0);
		}

	private boolean disposeFrameSafely(DEFrame frame, boolean isInteractive) {
		if (isInteractive && (frame == mFrameOnFocus)
		 && DEMacroRecorder.getInstance().isRunningMacro()) {
			JOptionPane.showMessageDialog(frame, "You cannot close the front window while a macro is running.");
			return false;
			}

		if (frame.askSaveDataIfDirty()
		 && frame.askStopRecordingMacro()) {	// stop recording after potentially saving to include save task in macro
			disposeFrame(frame);
			return true;
			}
		return false;
		}

	/**
	 * get rid of frame; no questions asked.
	 */
	private void disposeFrame(DEFrame frame) {
		mFrameList.remove(frame);
		frame.getTableModel().initializeTable(0, 0);
		frame.setVisible(false);
		frame.dispose();
		if (mFrameOnFocus == frame)
			mFrameOnFocus = null;
		}

	/**
	 * Sets the look&feel, which is defined in the preferences. If the preferences doesn't contain
	 * a look&feel name, then the default look&feel for this platform is chosen.
	 */
	public void setInitialLookAndFeel() {
		Preferences prefs = Preferences.userRoot().node(PREFERENCES_ROOT);
		String lafClass = prefs.get(PREFERENCES_KEY_LAF_NAME, "");

		// we don't support the old substance LaF anymore. Use NEBULA instead
		if ("org.jvnet.substance.SubstanceLookAndFeel".equals(lafClass))
			lafClass = LookAndFeel.NEBULA.className();

		LookAndFeel[] lafs = getAvailableLAFs();
		LookAndFeel selectedLAF = getDefaultLAF();
		for (LookAndFeel laf:lafs) {
			if (laf.className().equals(lafClass)) {
				selectedLAF = laf;
				break;
				}
			}
		setLookAndFeel(selectedLAF);
		}

	/**
	 * Simple implementation that just set the look&feel without adapting issues like
	 * font sizes to any platform. Override, if you need more.
	 * @param laf
	 * @return false, if the look&feel could not be found or activated
	 */
	public boolean setLookAndFeel(LookAndFeel laf) {
		try {
			UIManager.setLookAndFeel(laf.className);
			HeaderPaintHelper.setSpotColors(laf.getColor1(), laf.getColor2());
			try {
				Color bg = UIManager.getColor("ToolTip.background");
				Color translucentBG = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 0x80);
				UIManager.put("ToolTip.background", translucentBG);
				}
			catch (Exception e) {
				e.printStackTrace();
				}
			return true;
			}
		catch (Exception e) {
			return false;
			}
		}

	public LookAndFeel getLookAndFeel(String displayName) {
		LookAndFeel[] lafs = getAvailableLAFs();
		for (LookAndFeel laf:lafs)
			if (laf.displayName.equals(displayName))
				return laf;

		return getDefaultLAF();
		}

	/**
	 * Changes the look&feel and, if successful, updates the component hierarchy
	 * and stores the new look&feel name in the preferences.
	 * @param laf
	 * @return true if the LaF could be changed successfully
	 */
	public boolean updateLookAndFeel(LookAndFeel laf) {
		if (setLookAndFeel(laf)) {
			for (DEFrame f : mFrameList)
				SwingUtilities.updateComponentTreeUI(f);

			getPreferences().put(PREFERENCES_KEY_LAF_NAME, laf.className);
			return true;
			}
		return false;
		}

	public static Preferences getPreferences() {
		return Preferences.userRoot().node(PREFERENCES_ROOT);
		}

	public abstract boolean isMacintosh();
	public abstract LookAndFeel getDefaultLAF();
	public abstract LookAndFeel[] getAvailableLAFs();

	/**
	 * Opens the file, runs the query, starts the macro depending on the file type.
	 * @param filename
	 * @return new frame or null if no frame was opened
	 *
	public DEFrame readFile(String filename) {
		final int filetype = FileHelper.getFileType(filename);
		switch (filetype) {
		case FileHelper.cFileTypeDataWarrior:
		case FileHelper.cFileTypeSD:
		case FileHelper.cFileTypeTextTabDelimited:
		case FileHelper.cFileTypeTextCommaSeparated:
			final DEFrame _emptyFrame = getEmptyFrame(filename);
			new CompoundTableLoader(_emptyFrame, _emptyFrame.getTableModel()) {
				public void finalStatus(boolean success) {
					if (success && filetype == FileHelper.cFileTypeDataWarrior)
						_emptyFrame.setDirty(false);
					}
				}.readFile(new File(filename), new DERuntimeProperties(_emptyFrame.getMainFrame()), filetype);

			return _emptyFrame;
		case FileHelper.cFileTypeDataWarriorQuery:
			new DEFileLoader(getActiveFrame(), this).openAndRunQuery(new File(filename));
			return null;
		case FileHelper.cFileTypeDataWarriorMacro:
			new DEFileLoader(getActiveFrame(), this).openAndRunMacro(new File(filename));
			return null;
		default:
			JOptionPane.showMessageDialog(getActiveFrame(), "Unsupported file type.\n"+filename);
			return null;
			}
		}	*/

	/**
	 * When the program is launched with file names as arguments, and if file names
	 * contain white space, then this method tries to reconstruct the original file names.
	 * @param args
	 * @return list of file names
	 */
	public String[] deduceFileNamesFromArgs(String[] args) {
		if (args == null || args.length < 2)
			return args;

		int validCount = 0;
		boolean[] hasValidExtention = new boolean[args.length];
		for (int i=0; i<args.length; i++) {
			hasValidExtention[i] = (FileHelper.getFileType(args[i]) != FileHelper.cFileTypeUnknown);
			if (hasValidExtention[i])
				validCount++;
			}

		if (validCount == 0 || validCount == args.length)
			return args;

		// we need to concatenate assuming that the white space is a simple SPACE
		int argIndex = -1;
		String[] filename = new String[validCount];
		for (int i=0; i<validCount; i++) {
			filename[i] = args[++argIndex];
			while (!hasValidExtention[argIndex])
				filename[i] = filename[i].concat(" ").concat(args[++argIndex]);
			}

		return filename;
		}

	/**
	 * Opens the file, runs the query, starts the macro depending on the file type.
	 * @param filename
	 */
	public void readFile(String filename) {
		final int filetype = FileHelper.getFileType(filename);
		switch (filetype) {
		case FileHelper.cFileTypeDataWarrior:
		case FileHelper.cFileTypeSD:
		case FileHelper.cFileTypeTextTabDelimited:
		case FileHelper.cFileTypeTextCommaSeparated:
		    new DETaskOpenFile(this, filename).defineAndRun();
			return;
		case FileHelper.cFileTypeDataWarriorMacro:
			new DETaskRunMacroFromFile(this, filename).defineAndRun();
			return;
		default:
			JOptionPane.showMessageDialog(getActiveFrame(), "Unsupported file type.\n"+filename);
			return;
			}
		}

	public void updateRecentFiles(File file) {
		if (file == null || !file.exists())
			return;

		int type = FileHelper.getFileType(file.getName());
		if (type != FileHelper.cFileTypeDataWarrior
				&& type != FileHelper.cFileTypeSD
				&& type != FileHelper.cFileTypeTextTabDelimited
				&& type != FileHelper.cFileTypeTextCommaSeparated)
			return;

		try {
			Preferences prefs = getPreferences();

			String[] recentFileName = new String[StandardMenuBar.MAX_RECENT_FILE_COUNT+1];
			for (int i=1; i<=StandardMenuBar.MAX_RECENT_FILE_COUNT; i++)
				recentFileName[i] = prefs.get(StandardMenuBar.PREFERENCES_KEY_RECENT_FILE+i, "");

			recentFileName[0] = file.getCanonicalPath();
			for (int i=1; i<StandardMenuBar.MAX_RECENT_FILE_COUNT; i++) {
				if (recentFileName[0].equals(recentFileName[i])) {
					for (int j=i+1; j<=StandardMenuBar.MAX_RECENT_FILE_COUNT; j++)
						recentFileName[j-1] = recentFileName[j];
					}
				}

			for (int i=0; i<StandardMenuBar.MAX_RECENT_FILE_COUNT && recentFileName[i].length() != 0; i++)
				prefs.put(StandardMenuBar.PREFERENCES_KEY_RECENT_FILE+(i+1), recentFileName[i]);

			for (DEFrame frame:getFrameList())
				frame.getDEMenuBar().updateRecentFileMenu();
			}
		catch (Exception e) {}
		}

	public ArrayList<DEFrame> getFrameList() {
		return mFrameList;
		}

	public DEFrame getActiveFrame() {
		if (mFrameList == null || mFrameList.size() == 0)
			return null;

		for (DEFrame f:mFrameList)
			if (f == mFrameOnFocus)
				return f;

		return mFrameList.get(0);
		}

	/**
	 * If not called from the event dispatch thread and if called after closeFrameSafely()
	 * then this call waits until this class receives a windowGainedFocus() and
	 * returns the frame that has gotten the focus. If no frames are left after
	 * one was closed, then null is returned. 
	 * @return active frame or null
	 */
	public DEFrame getNewFrontFrameAfterClosing() {
		if (mFrameList.size() == 0)
			return null;

		if (!SwingUtilities.isEventDispatchThread()) {
			while (mFrameOnFocus == null)
				try { Thread.sleep(100); } catch (InterruptedException ie) {}
			}

		return mFrameOnFocus;
		}

	/**
	 * Select another frame mimicking the user selecting the window interactively:
	 * If a macro is recording, this will cause a SelectWindow task to be recorded.
	 * @param frame
	 */
	public void setActiveFrame(DEFrame frame) {
		if (frame != mFrameOnFocus) {
			frame.toFront();

			// mFrameOnFocus = frame; Don't do this, to mimic user interaction; mFrameOnFocus will be updated through windowGainedFocus() call
			}
		}

	private void createNewFrame(String title, boolean lockForImmediateUsage) {
		GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

		DEFrame f = new DEFrame(this, title, lockForImmediateUsage);
		f.validate();

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension frameSize = f.getSize();
		int borderX = HiDPIHelper.scale(64);
		int borderY = HiDPIHelper.scale(24);
		int blockShift = HiDPIHelper.scale(64);
		int surplus = Math.min(screenSize.width-frameSize.width - borderX,
							   screenSize.height-frameSize.height - borderY);
		int offset = HiDPIHelper.scale(16);
		int steps = (surplus < 16 * offset) ? 8 : surplus / 16;
		int block = mFrameList.size() / steps;
		int index = mFrameList.size() % steps;

		mFrameList.add(f);
		mFrameOnFocus = f;

		f.setLocation(borderX + gc.getBounds().x + offset * index + blockShift * block,
					  borderY + gc.getBounds().y + offset * index);
		f.setVisible(true);
		f.toFront();
		f.addWindowFocusListener(this);

		f.updateMacroStatus();
		}

	/**
	 * Tries to return the datawarrior.jar file.
	 * @return null if DataWarrior was not launched from .jar file in file system.
	 */
	public static File getDataWarriorJarFile() {
		try {
			CodeSource cs = DataWarrior.class.getProtectionDomain().getCodeSource();
			if (cs != null) {
				File file = new File(cs.getLocation().toURI());
				if (file.getName().endsWith(".jar"))	// on Windows this gets a file from the cache ??
					return file;
				}
			}
		catch (Exception e) {}
		return null;
		}
	}