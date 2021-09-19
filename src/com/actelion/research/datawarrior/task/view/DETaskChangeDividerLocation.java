package com.actelion.research.datawarrior.task.view;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEParentPane;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.dock.Dockable;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskChangeDividerLocation extends ConfigurableTask {
	public static final String TASK_NAME = "Change Divider Location";

	private static final String PROPERTY_LEFT_TARGET = "leftTarget";
	private static final String PROPERTY_RIGHT_TARGET = "rightTarget";
	private static final String PROPERTY_FRACTION = "fraction";

	public static final String VIEW_AREA = "<View Area>";
	public static final String FILTER_AREA = "<Filter Area>";
	public static final String DETAIL_AREA = "<Detail Area>";

	private DEParentPane mParentPane;
	private String mLeftTargetName,mRightTargetName;
	private double mFraction;
	private JComboBox mComboBoxLeftTarget;
	private JComboBox mComboBoxRightTarget;
	private JTextField mTextFieldFraction;

	public DETaskChangeDividerLocation(DEFrame parent, String leftTargetName, String rightTargetName, double fraction) {
		super(parent, false);
		mParentPane = parent.getMainFrame();
		mLeftTargetName = leftTargetName;
		mRightTargetName = rightTargetName;
		mFraction = fraction;
		}

	@Override
	public boolean isConfigurable() {
		return true;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		Properties configuration = null;
		if (mLeftTargetName != null) {
			configuration = new Properties();
			configuration.setProperty(PROPERTY_LEFT_TARGET, mLeftTargetName);
			configuration.setProperty(PROPERTY_RIGHT_TARGET, mRightTargetName);
			configuration.setProperty(PROPERTY_FRACTION, DoubleFormat.toString(mFraction));
			}
		return configuration;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String leftTargetName = configuration.getProperty(PROPERTY_LEFT_TARGET, "");
		if (leftTargetName.length() == 0) {
			showErrorMessage("Left/top view name missing.");
			return false;
			}
		String rightTargetName = configuration.getProperty(PROPERTY_RIGHT_TARGET, "");
		if (rightTargetName.length() == 0) {
			showErrorMessage("Right/bottom view name missing.");
			return false;
			}
		String fraction = configuration.getProperty(PROPERTY_FRACTION, "");
		if (fraction.length() == 0) {
			showErrorMessage("Divider location not defined.");
			return false;
			}
		try {
			double f = Double.parseDouble(fraction);
			if (f < 0.0 || f > 1.0) {
				showErrorMessage("Divider location must be between 0.0 and 1.0.");
				return false;
				}
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("Divider location is not numerical.");
			return false;
			}
		if (isLive && !DETAIL_AREA.equals(rightTargetName) && findSplitPane(leftTargetName, rightTargetName) == null) {
			showErrorMessage("Divider for separating '"+leftTargetName+"' from '"+rightTargetName+"' not found.");
			return false;
			}
		return true;
		}

	@Override
	public JComponent createDialogContent() {
		int gap = HiDPIHelper.scale(8);
		JPanel p = new JPanel();
		double[][] size = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };
		p.setLayout(new TableLayout(size));

		p.add(new JLabel("Choose divider that separates the following views:"), "1,1,3,1");

		p.add(new JLabel("Left/top view:"), "1,3");
		mComboBoxLeftTarget = new JComboBox();
		mComboBoxLeftTarget.addItem(VIEW_AREA);
		mComboBoxLeftTarget.addItem(FILTER_AREA);
		for (String viewName:mParentPane.getMainPane().getDockableTitles())
			if (mParentPane.getMainPane().isVisibleInSplitPane(viewName, true))
				mComboBoxLeftTarget.addItem(viewName);
		mComboBoxLeftTarget.setEditable(true);
		p.add(mComboBoxLeftTarget, "3,3");

		p.add(new JLabel("Right/bottom view:"), "1,5");
		mComboBoxRightTarget = new JComboBox();
		mComboBoxRightTarget.addItem(DETAIL_AREA);
		for (String viewName:mParentPane.getMainPane().getDockableTitles())
			if (mParentPane.getMainPane().isVisibleInSplitPane(viewName, false))
				mComboBoxRightTarget.addItem(viewName);
		mComboBoxRightTarget.setEditable(true);
		p.add(mComboBoxRightTarget, "3,5");

		p.add(new JLabel("Divider location (0-1):"), "1,7");
		mTextFieldFraction = new JTextField(4);
		p.add(mTextFieldFraction, "3,7");

		return p;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_LEFT_TARGET, (String)mComboBoxLeftTarget.getSelectedItem());
		configuration.setProperty(PROPERTY_RIGHT_TARGET, (String)mComboBoxRightTarget.getSelectedItem());
		configuration.setProperty(PROPERTY_FRACTION, mTextFieldFraction.getText());
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mComboBoxLeftTarget.setSelectedItem(configuration.getProperty(PROPERTY_LEFT_TARGET, VIEW_AREA));
		mComboBoxRightTarget.setSelectedItem(configuration.getProperty(PROPERTY_RIGHT_TARGET, DETAIL_AREA));
		mTextFieldFraction.setText(configuration.getProperty(PROPERTY_FRACTION, "0.5"));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mComboBoxLeftTarget.setSelectedItem(VIEW_AREA);
		mComboBoxRightTarget.setSelectedItem(DETAIL_AREA);
		mTextFieldFraction.setText("0.5");
		}

	@Override
	public boolean isRedundant(Properties previousConfiguration, Properties configuration) {
		String previousLeftTarget = previousConfiguration.getProperty(PROPERTY_LEFT_TARGET);
		String previousRightTarget = previousConfiguration.getProperty(PROPERTY_RIGHT_TARGET);
		String currentLeftTarget = configuration.getProperty(PROPERTY_LEFT_TARGET);
		String currentRightTarget = configuration.getProperty(PROPERTY_RIGHT_TARGET);
		if (DETAIL_AREA.equals(previousRightTarget) && DETAIL_AREA.equals(currentRightTarget)) {
			return (FILTER_AREA.equals(previousLeftTarget) && FILTER_AREA.equals(currentLeftTarget))
				|| (!FILTER_AREA.equals(previousLeftTarget) && !FILTER_AREA.equals(currentLeftTarget));
			}
		JSplitPane previousPane = findSplitPane(previousLeftTarget, previousRightTarget);
		JSplitPane currentPane = findSplitPane(currentLeftTarget, currentRightTarget);
		return previousPane == currentPane;
		}

	@Override
	public void runTask(Properties configuration) {
		double fraction = Double.parseDouble(configuration.getProperty(PROPERTY_FRACTION));
		String leftTarget = configuration.getProperty(PROPERTY_LEFT_TARGET);
		String rightTarget = configuration.getProperty(PROPERTY_RIGHT_TARGET);
		if (DETAIL_AREA.equals(rightTarget)) {
			if (FILTER_AREA.equals(leftTarget))
				mParentPane.setRightSplitting(fraction);
			else
				mParentPane.setMainSplitting(fraction);
			}
		else {
			findSplitPane(leftTarget, rightTarget).setDividerLocation(fraction);
			}
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	private JSplitPane findSplitPane(String leftTargetName, String rightTargetName) {
		Dockable leftDockable = mParentPane.getMainPane().getDockable(leftTargetName);
		if (leftDockable == null)
			return null;
		Component leftParent = leftDockable.getParent();
		if (leftParent instanceof JTabbedPane)
			leftParent = leftParent.getParent();

		Dockable rightDockable = mParentPane.getMainPane().getDockable(rightTargetName);
		if (rightDockable == null)
			return null;
		Component rightParent = rightDockable.getParent();
		if (rightParent instanceof JTabbedPane)
			rightParent = rightParent.getParent();

		if (!(leftParent instanceof JSplitPane) || !(rightParent instanceof JSplitPane))
			return null;

		if (leftParent == rightParent)
			return (JSplitPane)leftParent;

		int leftLevel = 0;
		Component splitPane = leftParent;
		while (splitPane.getParent() instanceof JSplitPane) {
			splitPane = splitPane.getParent();
			leftLevel++;
			}

		int rightLevel = 0;
		splitPane = rightParent;
		while (splitPane.getParent() instanceof JSplitPane) {
			splitPane = splitPane.getParent();
			rightLevel++;
			}

		for (; leftLevel > rightLevel; leftLevel--)
			leftParent = leftParent.getParent();
		for (; rightLevel > leftLevel; rightLevel--)
			rightParent = rightParent.getParent();

		while (leftLevel != 0 && leftParent != rightParent) {
			leftParent = leftParent.getParent();
			rightParent = rightParent.getParent();
			}

		return leftParent == rightParent ? (JSplitPane)leftParent : null;
		}
	}
