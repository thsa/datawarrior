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
import com.actelion.research.datawarrior.help.DEAboutDialog;
import com.actelion.research.datawarrior.help.DEUpdateHandler;
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
import com.actelion.research.gui.hidpi.HiDPIIcon;
import com.actelion.research.table.model.CompoundTableDetailHandler;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.util.Platform;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.prefs.Preferences;

public abstract class DataWarrior implements WindowFocusListener {
	public static final String PROGRAM_NAME = "DataWarrior";

	private static final String PREFERENCES_ROOT = "org.openmolecules.datawarrior";
	public static final String PREFERENCES_KEY_FIRST_LAUNCH = "first_launch";
	public static final String PREFERENCES_KEY_LAST_VERSION_ERROR = "last_version_error";
	public static final String PREFERENCES_KEY_DPI_SCALING = "dpiScaling";
	public static final String PREFERENCES_KEY_LAF_NAME = "laf_name";
	public static final String PREFERENCES_KEY_SPAYA_SERVER = "spaya_server";
	public static final String PREFERENCES_KEY_RECENT_FILE = "recentFile";
	public static final int MAX_RECENT_FILE_COUNT = 24;

	public static boolean USE_CARDS_VIEW = false;

	public static final String[] RESOURCE_DIR = { "Reference", "Example", "Tutorial" };
	public static final String MACRO_DIR = "Macro";
	public static final String PLUGIN_DIR = "Plugin";
	public static final String QUERY_REACTION_TEMPLATE_FILE = "template/reactionqueries.txt";

	private boolean mIsCapkaAvailable;

	public enum LookAndFeel {
		NIGHT("Night", "org.pushingpixels.radiance.theming.api.skin.RadianceNightShadeLookAndFeel", true, 0x592090, 0x2e0951, 0xbc8beb),
		GRAPHITE("Graphite", "org.pushingpixels.radiance.theming.api.skin.RadianceGraphiteLookAndFeel", true, 0x3838C0, 0x252560, 0xa6a6fa),
		GRAY("Gray", "org.pushingpixels.radiance.theming.api.skin.RadianceMistSilverLookAndFeel", false, 0xAEDBFF, 0x0060FF, 0x003bdb),
		CREME("Creme Coffee", "org.pushingpixels.radiance.theming.api.skin.RadianceCremeCoffeeLookAndFeel", false, 0xDEC59D, 0xAA784F, 0x754a06),
		MODERATE("Moderate", "org.pushingpixels.radiance.theming.api.skin.RadianceModerateLookAndFeel", false, 0x6D96B3, 0x1E4C6F, 0x126194),
		NEBULA("Nebula", "org.pushingpixels.radiance.theming.api.skin.RadianceNebulaLookAndFeel", false, 0xA7BBCD, 0x55585E, 0x48729c),
		SAHARA("Sahara", "org.pushingpixels.radiance.theming.api.skin.RadianceSaharaLookAndFeel", false, 0xA6B473, 0x6E7841, 0x69801a),
//		VAQUA("VAqua", "org.violetlib.aqua.AquaLookAndFeel", false, 0xAEDBFF, 0x0060FF),
		AQUA("Aqua", "com.apple.laf.AquaLookAndFeel", false, 0xAEDBFF, 0x0060FF, 0x006aff);

		private final String displayName;
		private String className;
		private boolean isDark;
		private int rgb1,rgb2,rgb3;

		/**
		 *
		 * @param displayName
		 * @param className
		 * @param isDark
		 * @param rgb1 lighter header gradient at top
		 * @param rgb2 darker header gradient at bottom
		 * @param rgb3 icon spot color
		 */
		LookAndFeel(String displayName, String className, boolean isDark, int rgb1, int rgb2, int rgb3) {
			this.displayName = displayName;
			this.className = className;
			this.isDark = isDark;
			this.rgb1 = rgb1;
			this.rgb2 = rgb2;
			this.rgb3 = rgb3;
			}

		public String displayName() { return displayName; }
		public String className() { return className; }
		public int getRGB1() {
			if (System.getProperty("development") != null)
				return isDark ? 0xC0C000 : 0xFFFFCD;
			return rgb1;
			}

		public int getRGB2() {
			if (System.getProperty("development") != null)
				return isDark ? 0x404000 : 0xAD9C00;
			return rgb2;
			}
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
		mPluginRegistry = new PluginRegistry(this, Thread.currentThread().getContextClassLoader());
		setInitialLookAndFeel();

		mFrameList = new ArrayList<>();
		createNewFrame(null, false);
		new DEAboutDialog(mFrameOnFocus, 2000);

		initialize();

		DEUpdateHandler.getPostInstallInfoAndHandleUpdates(mFrameOnFocus, isIdorsia());

		mTaskFactory = createTaskFactory();
		DEMacroRecorder.getInstance().setTaskFactory(mTaskFactory);

		sApplication = this;
		}

	public boolean isCapkaAvailable() {
		return mIsCapkaAvailable;
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
		Preferences prefs = getPreferences();
		long firstLaunchMillis = prefs.getLong(PREFERENCES_KEY_FIRST_LAUNCH, 0L);
		if (firstLaunchMillis == 0L)
			prefs.putLong(PREFERENCES_KEY_FIRST_LAUNCH, System.currentTimeMillis());

		javafx.application.Platform.setImplicitExit(false);
		ToolTipManager.sharedInstance().setDismissDelay(600000);    // 10 min
		Molecule.setDefaultAverageBondLength(HiDPIHelper.scale(24));
		StructureNameResolver.setInstance(new DEStructureNameResolver());
		GenericEditorArea.setReactionMapper(new ChemicalRuleEnhancedReactionMapper());

		try {
			mIsCapkaAvailable = false;
			Class.forName("chemaxon.marvin.calculations.pKaPlugin");
			if (new chemaxon.marvin.calculations.pKaPlugin().isLicensed())
				mIsCapkaAvailable = true;
			else
				JOptionPane.showMessageDialog(getActiveFrame(), "The ChemAxon pKa plugin 'capka.jar' was found, but the license file\nseems to be missing or invalid. pKa calculations won't be available.");
			}
		catch (ClassNotFoundException cnfe) {}
		catch (Throwable t) {
			t.printStackTrace();
			}

		initializeCustomTemplates();
		}

	private void initializeCustomTemplates() {
		File rtf = resolveResourcePath(QUERY_REACTION_TEMPLATE_FILE);
		if (rtf != null) {
			try {
				ArrayList<String[]> list = new ArrayList<>();
				BufferedReader reader = new BufferedReader(new FileReader(rtf));
				String[] keyAndValue = parseTemplateLine(reader);
				if (keyAndValue != null
				 && keyAndValue[0].equals(GenericEditorArea.TEMPLATE_TYPE_KEY)
				 && keyAndValue[1].equals(GenericEditorArea.TEMPLATE_TYPE_REACTION_QUERIES)) {
					keyAndValue = parseTemplateLine(reader);
					while (keyAndValue != null) {
						list.add(keyAndValue);
						keyAndValue = parseTemplateLine(reader);
					}
				}
				reader.close();
				if (list.size() != 0)
					GenericEditorArea.setReactionQueryTemplates(list.toArray(new String[0][]));
			}
			catch (IOException ioe) {}
			}
		}

	private String[] parseTemplateLine(BufferedReader reader) throws IOException {
		String line = reader.readLine();
		if (line == null)
			return null;

		int index = line.indexOf(':');
		if (index == -1)
			return null;

		String[] template = new String[2];
		template[0] = line.substring(0, index).trim();
		template[1] = line.substring(index+1).trim();
		return template;
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
	 * @return true, if all windows could be closed
	 */
	public boolean closeApplication(boolean isInteractive) {
		while (mFrameList.size() != 0) {
			DEFrame frame = getActiveFrame();
			if (disposeFrameSafely(frame, isInteractive) == 0)
				return false;
			}
		return true;
		}

	private void exit() {
		if (DEUpdateHandler.isUpdating())
			JOptionPane.showMessageDialog(getActiveFrame(), "Cannot exit, while update is downloading....");
		while (DEUpdateHandler.isUpdating())
			try { Thread.sleep(100); } catch (InterruptedException ie) {}

		System.exit(0);
		}

	/**
	 * If the frame contains unsaved content, then the user is asked whether
	 * its data shall be saved. If the user cancels the dialog the frame stays open.
	 * If the frame is the owner of a running macro, then the frame is not closed
	 * and an appropriate error message is displayed unless it is the macro itself
	 * that asks to close the frame (isInteractive==false).
	 * If this frame is the only frame, then the application is exited unless
	 * we run on a Macintosh, where the frame is cleared but stays open.
	 * @param frame
	 * @param isInteractive
	 * @return 0: could not close; 1: closed without saving; 2: saved and closed
	 */
	public int closeFrameSafely(DEFrame frame, boolean isInteractive) {
		int result = disposeFrameSafely(frame, isInteractive);

		if (!isMacintosh() && mFrameList.size() == 0)
			exit();

		return result;
		}

	/**
	 * If the frame contains unsaved content and saveContent==true then
	 * the frame's content is saved without user interaction.
	 * If a file is already assigned to the frame then this file is overwritten.
	 * Otherwise, a new file is saved in the home directory.
	 * If a macro is recording, then this call does not record any tasks.
	 * If a macro is recording and the frame to be closed is the macro owner, then
	 * the recording is stopped before the frame is saved and closed.
	 * @param frame
	 * @param saveContent if true, then the window is closed without asking to save
	 */
	public void closeFrameSilently(DEFrame frame, boolean saveContent) {
		if (DEMacroRecorder.getInstance().isRecording()
		 && DEMacroRecorder.getInstance().getRecordingMacroOwner() == frame)
			DEMacroRecorder.getInstance().stopRecording();

		if (saveContent)
			frame.saveSilentlyIfDirty();

		disposeFrame(frame);
		}

	/**
	 * If the frame contains unsaved content, then the user is asked whether
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
			if (disposeFrameSafely(getActiveFrame(), isInteractive) == 0)
				return;

		if (!isMacintosh())
			exit();
		}

	/**
	 * If the frame contains unsaved content and saveContent==true then
	 * the frame's content is saved without user interaction.
	 * If a file is already assigned to the frame then this file is overwritten.
	 * Otherwise, a new file is saved in the home directory.
	 * The application exits after closing the last frame.
	 * If a macro is recording, then this call does not record any tasks.
	 */
	public void closeAllFramesSilentlyAndExit(boolean saveContent) {
		while (mFrameList.size() != 0)
			closeFrameSilently(getActiveFrame(), saveContent);

		exit();
		}

	/**
	 * @param frame
	 * @param isInteractive
	 * @return 0: could not close; 1: closed without saving; 2: saved and closed
	 */
	private int disposeFrameSafely(DEFrame frame, boolean isInteractive) {
		if (isInteractive && (frame == mFrameOnFocus)
		 && DEMacroRecorder.getInstance().isRunningMacro()) {
			JOptionPane.showMessageDialog(frame, "You cannot close the front window while a macro is running.");
			return 0;
			}

		if (frame.askSaveDataIfDirty()
		 && frame.askStopRecordingMacro()) {	// stop recording after potentially saving to include save task in macro
			int result = frame.isDirty() ? 1 : 2;
			disposeFrame(frame);
			return result;
			}

		return 0;
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
	 * Sets the look&feel, which is defined in the preferences. If the preferences don't contain
	 * a look&feel name, then the default look&feel for this platform is chosen.
	 */
	public void setInitialLookAndFeel() {
		Preferences prefs = Preferences.userRoot().node(PREFERENCES_ROOT);

		String dpiScaling = prefs.get(PREFERENCES_KEY_DPI_SCALING, "");
		if (!dpiScaling.isEmpty())
			HiDPIHelper.setUIScaleFactor(Float.parseFloat(dpiScaling));

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
		final int[] DEV_SPOT_COLORS_DARK_LAF = { 0xE0E000, 0xE0E0E0 };
		final int[] DEV_SPOT_COLORS_BRIGHT_LAF = { 0x707000, 0x000000 };
		try {
			UIManager.setLookAndFeel(laf.className);
			int[] rgb = new int[2];
			rgb[0] = laf.getRGB1();
			rgb[1] = laf.getRGB2();
			HeaderPaintHelper.setThemeColors(rgb);
			if (System.getProperty("development") != null) {
				if (laf.isDark)
					HiDPIIcon.setIconSpotColors(DEV_SPOT_COLORS_DARK_LAF);
				else
					HiDPIIcon.setIconSpotColors(DEV_SPOT_COLORS_BRIGHT_LAF);
				}
			else {
				int[] spotColorRGB = new int[2];
				spotColorRGB[0] = laf.rgb3;
				spotColorRGB[1] = laf.isDark ? DEV_SPOT_COLORS_DARK_LAF[1] : DEV_SPOT_COLORS_BRIGHT_LAF[1];
				HiDPIIcon.setIconSpotColors(spotColorRGB);
				}
			makeTooltipsTranslucent();
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

	private void makeTooltipsTranslucent() {
		try {
			Color bg = UIManager.getColor("Label.background");
			Color translucentBG = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), Platform.isLinux() ? 0x70 : 0xA8 );
			UIManager.put("ToolTip.background", translucentBG);
			}
		catch (Exception e) {
			e.printStackTrace();
			}

		// Although ToolTip.background is defined to be half transparent, first tooltips shown are opaque,
		// because the JPanels underneith are opaque for some reason.
		// In addition, tooltips that are partially outside their parent window aren't transparent either,
		// because they are shown on top an opaque heavyweight window.
		// The following hack solves both issues.
		PopupFactory.setSharedInstance(new PopupFactory() {
			@Override
			public Popup getPopup(Component owner, Component contents, int x, int y) throws IllegalArgumentException {
				Popup p = super.getPopup(owner, contents, x, y);
				if (contents instanceof JToolTip && owner instanceof JVisualization) {
					boolean found = false;
					Class<?> clazz = p.getClass();
					while (clazz != null && !found) {
						try {
							Method m = clazz.getDeclaredMethod("getComponent");
							m.setAccessible(true);
							Component c = (Component)m.invoke(p);
							if (c instanceof JWindow)
								((JWindow)c).setOpacity(0.7f);
							else if (c instanceof JPanel)
								((JPanel)c).setOpaque(false);
							found = true;
							}
						catch (Exception e) {}
						clazz = clazz.getSuperclass();
						}
					}

				return p;
				}
			});
		}

	public static Preferences getPreferences() {
		return Preferences.userRoot().node(PREFERENCES_ROOT);
		}

	public abstract boolean isMacintosh();
	public abstract LookAndFeel getDefaultLAF();
	public abstract LookAndFeel[] getAvailableLAFs();

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
		case FileHelper.cFileTypeTextAnyCSV:
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

	public void handleCustomURI(String[] args) {
		// For testing purposes we just display the URI data...
		System.out.println("Custom URI:");
		for (String arg:args)
			System.out.println(arg);
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

			String[] recentFileName = new String[MAX_RECENT_FILE_COUNT+1];
			for (int i=1; i<=MAX_RECENT_FILE_COUNT; i++)
				recentFileName[i] = prefs.get(PREFERENCES_KEY_RECENT_FILE+i, "");

			recentFileName[0] = file.getCanonicalPath();
			for (int i=1; i<MAX_RECENT_FILE_COUNT; i++) {
				if (recentFileName[0].equals(recentFileName[i])) {
					for (int j=i+1; j<=MAX_RECENT_FILE_COUNT; j++)
						recentFileName[j-1] = recentFileName[j];
					}
				}

			for (int i=0; i<MAX_RECENT_FILE_COUNT && recentFileName[i].length() != 0; i++)
				prefs.put(PREFERENCES_KEY_RECENT_FILE+(i+1), recentFileName[i]);

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

	public static void initModuleAccess() {
		try {
			// export and open package for WebEngine/WebPage search, highlighting and navigation at run time:
			jdk.internal.module.Modules.addExportsToAllUnnamed(ModuleLayer.boot().findModule("javafx.web").orElseThrow(), "com.sun.webkit");
			jdk.internal.module.Modules.addOpensToAllUnnamed(ModuleLayer.boot().findModule("javafx.web").orElseThrow(), "javafx.scene.web");
			if (Platform.isMacintosh()) {
				jdk.internal.module.Modules.addExportsToAllUnnamed(ModuleLayer.boot().findModule("java.desktop").orElseThrow(), "com.apple.eawt");
				jdk.internal.module.Modules.addOpensToAllUnnamed(ModuleLayer.boot().findModule("java.desktop").orElseThrow(), "com.apple.eawt");
//				jdk.internal.module.Modules.addOpensToAllUnnamed(ModuleLayer.boot().findModule("java.desktop").orElseThrow(), "com.apple.eawt.event");
//				jdk.internal.module.Modules.addOpensToAllUnnamed(ModuleLayer.boot().findModule("java.desktop").orElseThrow(), "com.apple.laf");
			}
		}
		catch (Exception|Error e) {
			System.out.println("Could not export packages. Run with JRE option: '--add-exports java.base/jdk.internal.module=ALL-UNNAMED',");
		}
	}

	public static void main(final String[] args) {
		if (Platform.isMacintosh())
			DataWarriorOSX.main(args);
		else
			DataWarriorLinux.main(args);
	}
}