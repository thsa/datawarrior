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

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.util.ArrayList;

import static com.actelion.research.datawarrior.DataWarrior.LookAndFeel.*;

public class DataWarriorLinux extends DataWarrior {
	private static final LookAndFeel[] LOOK_AND_FEELS = { GRAPHITE, GRAY, MODERATE, CREME, SAHARA, NEBULA };

	private static final LookAndFeel DEFAULT_LAF = GRAPHITE;

	protected static DataWarriorLinux sDataExplorer;
	protected static ArrayList<String> sPendingDocumentList;

	private static class NewSubstanceFontSet implements org.pushingpixels.substance.api.fonts.FontSet {
		private float factor;
		private org.pushingpixels.substance.api.fonts.FontSet delegate;

		/**
		 * @param delegate The base Substance font set.
		 * @param factor Extra size in pixels. Can be positive or negative.
		 */
		public NewSubstanceFontSet(org.pushingpixels.substance.api.fonts.FontSet delegate, float factor) {
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

	private void setFontSetNewSubstance(final float factor) {
		// reset the base font policy to null - this
		// restores the original font policy (default size).
		org.pushingpixels.substance.api.SubstanceLookAndFeel.setFontPolicy(null);

		// reduce the default font size a little
		final org.pushingpixels.substance.api.fonts.FontSet substanceCoreFontSet = org.pushingpixels.substance.api.SubstanceLookAndFeel.getFontPolicy().getFontSet("Substance", null);
		org.pushingpixels.substance.api.fonts.FontPolicy newFontPolicy = new org.pushingpixels.substance.api.fonts.FontPolicy() {
			public org.pushingpixels.substance.api.fonts.FontSet getFontSet(String lafName, UIDefaults table) {
				return new NewSubstanceFontSet(substanceCoreFontSet, factor);
				}
			};
		org.pushingpixels.substance.api.SubstanceLookAndFeel.setFontPolicy(newFontPolicy);
		}

	@Override
	public boolean setLookAndFeel(LookAndFeel laf) {
		float fontFactor = 1f;
		String dpiFactor = System.getProperty("dpifactor");
		if (dpiFactor != null) {
			try { fontFactor = Float.parseFloat(dpiFactor); } catch (NumberFormatException nfe) {}
			System.getProperties().remove("dpifactor");  // prevent HiDPIHelper from applying factor a second time
			}

		if (super.setLookAndFeel(laf)) {
			if (fontFactor != 1f) {
				if (LookAndFeelHelper.isNewSubstance()) {
					setFontSetNewSubstance(fontFactor);
					}
				}
			return true;
			}
		return false;
		}

	public static void main(final String[] args) {
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
					String[] filename = sDataExplorer.deduceFileNamesFromArgs(args);
					for (String f:filename)
						sDataExplorer.readFile(f);
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