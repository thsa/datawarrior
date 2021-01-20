package com.actelion.research.datawarrior.task.view;

import com.actelion.research.chem.AbstractDepictor;
import com.actelion.research.chem.Depictor2D;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JStructureGrid;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskSetStructureDisplayMode extends DETaskAbstractSetViewOptions {
	public static final String TASK_NAME = "Set Structure Display Mode";

	public static final String[] COLOR_MODE_CODE = { "full", "atomsOnly", "stereoOnly", "none" };
	public static final String[] COLOR_MODE_TEXT = {
			"Full Color", "Atom labels in color", "Stereo labels in color", "Black and white" };

	public static final String[] STEREO_MODE_CODE = { "full", "ESRAndCIP", "CIPOnly", "ESROnly", "upDownOnly" };
	public static final String[] STEREO_MODE_TEXT = {
			"Full stereo information", "ESR and CIP labels", "CIP labels only", "ESR labels only", "No labels, up/down bonds only" };

	public static final int COLOR_MODE_MASK = AbstractDepictor.cDModeNoStereoProblem | AbstractDepictor.cDModeNoColorOnESRAndCIP | AbstractDepictor.cDModeNoImplicitAtomLabelColors;
	public static final int[] COLOR_MODE = { 0,
			AbstractDepictor.cDModeNoStereoProblem | AbstractDepictor.cDModeNoColorOnESRAndCIP,
			AbstractDepictor.cDModeNoImplicitAtomLabelColors,
			AbstractDepictor.cDModeNoStereoProblem | AbstractDepictor.cDModeNoColorOnESRAndCIP | AbstractDepictor.cDModeNoImplicitAtomLabelColors };

	public static final int STEREO_MODE_MASK = Depictor2D.cDModeSuppressChiralText | AbstractDepictor.cDModeSuppressESR | AbstractDepictor.cDModeSuppressCIPParity;
	public static final int[] STEREO_MODE = { 0,
			Depictor2D.cDModeSuppressChiralText,
			Depictor2D.cDModeSuppressChiralText | AbstractDepictor.cDModeSuppressESR,
			Depictor2D.cDModeSuppressChiralText | AbstractDepictor.cDModeSuppressCIPParity,
			Depictor2D.cDModeSuppressChiralText | AbstractDepictor.cDModeSuppressESR | AbstractDepictor.cDModeSuppressCIPParity };

	private static final int DEFAULT_STEREO_MODE_INDEX = 1;
	private static final int DEFAULT_COLOR_MODE_INDEX = 0;
	public static int DEFAULT_STEREO_MODE = STEREO_MODE[DEFAULT_STEREO_MODE_INDEX];
	public static int DEFAULT_COLOR_MODE = COLOR_MODE[DEFAULT_COLOR_MODE_INDEX];

	private static final String PROPERTY_COLOR_MODE = "colorMode";
	private static final String PROPERTY_STEREO_MODE = "stereoMode";

	private JComboBox mComboBoxStereoMode,mComboBoxColorMode;
	private int mStereoModeIndex,mColorModeIndex;

	public DETaskSetStructureDisplayMode(Frame owner, DEMainPane mainPane, JStructureGrid view) {
		super(owner, mainPane, view);
		mColorModeIndex = -1;
		mStereoModeIndex = -1;
	}

	public DETaskSetStructureDisplayMode(Frame owner, DEMainPane mainPane, JStructureGrid view, int stereoModeIndex, int colorModeIndex) {
		super(owner, mainPane, view);
		mStereoModeIndex = stereoModeIndex;
		mColorModeIndex = colorModeIndex;
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return mStereoModeIndex != -1 || mColorModeIndex != -1;
	}

	@Override
	public Properties getPredefinedConfiguration() {
		Properties configuration = super.getPredefinedConfiguration();
		if (configuration != null) {
			if (mColorModeIndex != -1)
				configuration.put(PROPERTY_COLOR_MODE, COLOR_MODE_CODE[mColorModeIndex]);
			if (mStereoModeIndex != -1)
				configuration.put(PROPERTY_STEREO_MODE, STEREO_MODE_CODE[mStereoModeIndex]);
		}

		return configuration;
	}

	@Override
	public JComponent createViewOptionContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap} };
		JPanel cp = new JPanel();
		cp.setLayout(new TableLayout(size));

		mComboBoxColorMode = new JComboBox(COLOR_MODE_TEXT);
		mComboBoxColorMode.addItemListener(this);
		cp.add(new JLabel("Color display mode:"), "1,1");
		cp.add(mComboBoxColorMode, "3,1");

		mComboBoxStereoMode = new JComboBox(STEREO_MODE_TEXT);
		mComboBoxStereoMode.addItemListener(this);
		cp.add(new JLabel("Stereo display mode:"), "1,3");
		cp.add(mComboBoxStereoMode, "3,3");

		return cp;
	}

	public static int findStereoModeIndex(String modeCode, int defaultIndex) {
		return findListIndex(modeCode, STEREO_MODE_CODE, defaultIndex);
	}

	public static int findColorModeIndex(String modeCode, int defaultIndex) {
		return findListIndex(modeCode, COLOR_MODE_CODE, defaultIndex);
	}

	public static int findStereoModeIndex(int mode) {
		for (int i=0; i<STEREO_MODE.length; i++)
			if (mode == STEREO_MODE[i])
				return i;
		return DEFAULT_STEREO_MODE_INDEX;
	}

	public static int findColorModeIndex(int mode) {
		for (int i=0; i<COLOR_MODE.length; i++)
			if (mode == COLOR_MODE[i])
				return i;
		return DEFAULT_COLOR_MODE_INDEX;
	}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof JStructureGrid) ? null : "The structure display mode can only be set on structure-views.";
	}

	@Override
	public void setDialogToDefault() {
		mComboBoxStereoMode.setSelectedItem(DEFAULT_STEREO_MODE_INDEX);
		mComboBoxColorMode.setSelectedItem(DEFAULT_COLOR_MODE_INDEX);
	}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		mComboBoxStereoMode.setSelectedIndex(findStereoModeIndex(configuration.getProperty(PROPERTY_STEREO_MODE), DEFAULT_STEREO_MODE_INDEX));
		mComboBoxColorMode.setSelectedIndex(findColorModeIndex(configuration.getProperty(PROPERTY_COLOR_MODE), DEFAULT_COLOR_MODE_INDEX));
	}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		configuration.setProperty(PROPERTY_STEREO_MODE, STEREO_MODE_CODE[mComboBoxStereoMode.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_COLOR_MODE, COLOR_MODE_CODE[mComboBoxColorMode.getSelectedIndex()]);
	}

	@Override
	public void addViewConfiguration(CompoundTableView view, Properties configuration) {
		int stereoMode = ((JStructureGrid)view).getStructureDisplayMode() & STEREO_MODE_MASK;
		configuration.setProperty(PROPERTY_STEREO_MODE, STEREO_MODE_CODE[findStereoModeIndex(stereoMode)]);
		int colorMode = ((JStructureGrid)view).getStructureDisplayMode() & COLOR_MODE_MASK;
		configuration.setProperty(PROPERTY_COLOR_MODE, COLOR_MODE_CODE[findColorModeIndex(colorMode)]);
	}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		return true;
	}

	@Override
	public void enableItems() {
	}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		if (view instanceof JStructureGrid) {
			int mode = ((JStructureGrid)view).getStructureDisplayMode();

			int stereoModeIndex = findListIndex(configuration.getProperty(PROPERTY_STEREO_MODE), STEREO_MODE_CODE, -1);
			if (stereoModeIndex != -1)
				mode = (mode & ~STEREO_MODE_MASK) | STEREO_MODE[stereoModeIndex];

			int colorModeIndex = findListIndex(configuration.getProperty(PROPERTY_COLOR_MODE), COLOR_MODE_CODE, -1);
			if (colorModeIndex != -1)
				mode = (mode & ~COLOR_MODE_MASK) | COLOR_MODE[colorModeIndex];

			((JStructureGrid)view).setStructureDisplayMode(mode);
		}
	}
}
