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

import com.actelion.research.gui.LookAndFeelHelper;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.util.Platform;
import org.pushingpixels.radiance.common.api.font.FontPolicy;
import org.pushingpixels.radiance.common.api.font.FontSet;
import org.pushingpixels.radiance.theming.api.RadianceThemingCortex;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.util.ArrayList;

import static com.actelion.research.datawarrior.DataWarrior.LookAndFeel.*;

public class DataWarriorLinux extends DataWarrior {
	private static final LookAndFeel[] LOOK_AND_FEELS = { NIGHT, GRAPHITE, GRAY, MODERATE, CREME, SAHARA, NEBULA };

	private static final LookAndFeel DEFAULT_LAF = GRAPHITE;

	protected static DataWarriorLinux sDataExplorer;
	protected static ArrayList<String> sPendingDocumentList;

	private static class NewSubstanceFontSet implements FontSet {
		private float factor;
		private FontSet delegate;

		/**
		 * @param delegate The base Substance font set.
		 * @param factor Extra size in pixels. Can be positive or negative.
		 */
		public NewSubstanceFontSet(FontSet delegate, float factor) {
			super();
			this.delegate = delegate;
			this.factor = factor;
		}

		/**
		 * @param systemFont Original font.
		 * @return Wrapped font.
		 */
		private FontUIResource getWrappedFont(FontUIResource systemFont) {
			return new FontUIResource(systemFont.getFontName(), systemFont.getStyle(),
									  Math.round(this.factor * systemFont.getSize()));
		}

		public FontUIResource getControlFont() {
			return this.getWrappedFont(this.delegate.getControlFont());
		}

		public FontUIResource getMenuFont() {
			return this.getWrappedFont(this.delegate.getMenuFont());
		}

		public FontUIResource getMessageFont() {
			return this.getWrappedFont(this.delegate.getMessageFont());
		}

		public FontUIResource getSmallFont() {
			return this.getWrappedFont(this.delegate.getSmallFont());
		}

		public FontUIResource getTitleFont() {
			return this.getWrappedFont(this.delegate.getTitleFont());
		}

		public FontUIResource getWindowTitleFont() {
			return this.getWrappedFont(this.delegate.getWindowTitleFont());
		}
	}

	/**
	 *  This is called from the Windows bootstrap process instead of main(), when
	 *  the user tries to open a new DataWarrior instance while one is already running.
	 * @param args
	 */
	public static void initSingleApplication(String[] args) {
		if (args != null && args.length != 0) {
			if (args[0].toLowerCase().startsWith("datawarrior:")) {
				try {
					SwingUtilities.invokeAndWait(() -> {
						if (sDataExplorer != null)
							sDataExplorer.handleCustomURI(args);
						} );
					}
				catch(Exception e) {}
				}
			else {
				String[] filename = sDataExplorer.deduceFileNamesFromArgs(args);
				if (sDataExplorer == null) {
					if (sPendingDocumentList == null)
						sPendingDocumentList = new ArrayList<>();

					for (String f:filename)
						sPendingDocumentList.add(f);
					}
				else {
					for (final String f:filename) {
						try {
							SwingUtilities.invokeAndWait(() -> sDataExplorer.readFile(f) );
							}
						catch(Exception e) {}
						}
					}
				}
			}
		}

	public boolean isMacintosh() {
		return false;
		}

	@Override
	public LookAndFeel[] getAvailableLAFs() {
		return LOOK_AND_FEELS;
		};

	@Override
	public LookAndFeel getDefaultLAF() {
		return DEFAULT_LAF;
		}

	private void setNewRadianceFontSet(final float factor) {
		// reset the base font policy to null - this
		// restores the original font policy (default size).
		RadianceThemingCortex.GlobalScope.setFontPolicy(null);

		// reduce the default font size a little
		final FontSet substanceCoreFontSet = RadianceThemingCortex.GlobalScope.getFontPolicy().getFontSet();
		FontPolicy newFontPolicy = () -> new NewSubstanceFontSet(substanceCoreFontSet, factor);
		RadianceThemingCortex.GlobalScope.setFontPolicy(newFontPolicy);
		}

	@Override
	public boolean setLookAndFeel(LookAndFeel laf) {
		if (super.setLookAndFeel(laf)) {
			float fontFactor = HiDPIHelper.getUIScaleFactor();
			if (fontFactor != 1f) {
				if (LookAndFeelHelper.isNewSubstance()
				 || LookAndFeelHelper.isRadiance()) {
					setNewRadianceFontSet(fontFactor);
					}
				}
			return true;
			}
		return false;
		}

	public static void main(final String[] args) {
		initModuleAccess();

		if (Platform.isWindows()) {
			// Liberica 21 moved libraries from jre\bin\javafx to jre\bin. These dlls are wrongly accessed via the old path.
			// If not found there, then the jre tries to find them using the java.library.path runtime variable.
			// For things to work correctly, java.library.path should start with "C:\\Program Files\\DataWarrior\\jre\\bin".
			// If an older JRE is installed, this variable may contain another path causing to load outdated libraries...
			final String javaLibraryPath = System.getProperty("java.library.path");
//			final String libericaLibraryPath = "C:\\Program Files\\DataWarrior\\jre\\bin";
			System.out.println("java.library.path is "+javaLibraryPath);
			}
		SwingUtilities.invokeLater(() -> {
			try {
				Thread.setDefaultUncaughtExceptionHandler((final Thread t, final Throwable e) ->
					SwingUtilities.invokeLater(() -> {
						e.printStackTrace();
						if (e.getMessage() != null)
							JOptionPane.showMessageDialog(sDataExplorer.getActiveFrame(), "Uncaught Exception:"+e.getMessage());
						} )
					);

				sDataExplorer = new DataWarriorLinux();

				if (args != null && args.length != 0) {
					if (args[0].toLowerCase().startsWith("datawarrior:")) {
						sDataExplorer.handleCustomURI(args);
						}
					else {
						String[] filename = sDataExplorer.deduceFileNamesFromArgs(args);
						for (String f:filename)
							sDataExplorer.readFile(f);
						}
					}

				if (sPendingDocumentList != null) {
					for (String doc:sPendingDocumentList)
						sDataExplorer.readFile(doc);
					sPendingDocumentList.clear();
					}
				}
			catch(Exception e) {
				e.printStackTrace();
				}
			} );
		}
	}