package com.actelion.research.datawarrior.task.view;

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.view.*;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskSetCrossHairs extends DETaskAbstractSetViewOptions {
	public static final String TASK_NAME = "Set Crosshairs";

	public static final int MODE_CLEAR = 1;
	public static final int MODE_FREEZE_CURRENT = 2;

	private static final String PROPERTY_COORDS = "coords";

	private JTextArea mTextAreaCoordinates;
	private int mMode;

	public DETaskSetCrossHairs(Frame owner, DEMainPane mainPane, VisualizationPanel2D view) {
		super(owner, mainPane, view);
		}

	public DETaskSetCrossHairs(Frame owner, DEMainPane mainPane, VisualizationPanel2D view, int mode) {
		super(owner, mainPane, view);
		mMode = mode;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public boolean isRedundant(Properties previousConfiguration, Properties currentConfiguration) {
		return true;
	}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return hasInteractiveView();
	}

	@Override
	public Properties getPredefinedConfiguration() {
		Properties configuration = super.getPredefinedConfiguration();
		if (configuration != null && mMode != 0) {
			JVisualization2D visualization = (JVisualization2D)getInteractiveVisualization();
			switch (mMode) {
			case MODE_FREEZE_CURRENT:
				visualization.freezeCrossHair();
				break;
			case MODE_CLEAR:
				visualization.clearCrossHairs();
				break;
				}

			configuration.setProperty(PROPERTY_COORDS, visualization.getCrossHairList());
			}
		return configuration;
		}

		@Override
	public OTHER_VIEWS getOtherViewMode() {
		return OTHER_VIEWS.GRAPHICAL2D;
	}

	@Override
	public JComponent createViewOptionContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, TableLayout.PREFERRED, gap, HiDPIHelper.scale(80), gap} };
		JPanel cp = new JPanel();
		cp.setLayout(new TableLayout(size));

		mTextAreaCoordinates = new JTextArea();
		cp.add(new JLabel("Please define cross hair position:"), "1,1");
		cp.add(new JLabel("(all empty or one X,Y pair per line)"), "1,2");
		cp.add(mTextAreaCoordinates, "1,4");

		return cp;
	}

	@Override
	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof VisualizationPanel2D) ? null : "Crosshairs can be defined for 2D-views only.";
		}

	@Override
	public void setDialogToDefault() {
		mTextAreaCoordinates.setText("");
	}

	@Override
	public void setDialogToConfiguration(Properties configuration) {
		mTextAreaCoordinates.setText(configuration.getProperty(PROPERTY_COORDS, "").replace(';', '\n'));
		}

	@Override
	public void addDialogConfiguration(Properties configuration) {
		String coords = mTextAreaCoordinates.getText().replace('\n', ';').replaceAll("\\s", "");
		configuration.setProperty(PROPERTY_COORDS, coords);
		}

	@Override
	public void addViewConfiguration(CompoundTableView view, Properties configuration) {
		String coords = ((JVisualization2D)((VisualizationPanel)view).getVisualization()).getCrossHairList();
		configuration.setProperty(PROPERTY_COORDS, coords);
		}

	@Override
	public boolean isViewConfigurationValid(CompoundTableView view, Properties configuration) {
		if (view != null) {
			String coords = configuration.getProperty(PROPERTY_COORDS, "");
			if (coords.length() != 0) {
				String coord[] = coords.split(";");
				for (String c:coord) {
					int index = c.indexOf(',');
					if (index == -1 || c.indexOf(',', index+1) != -1) {
						showErrorMessage("Invalid coordinate line (X and Y separated by a comma): '"+c+"'");
						return false;
						}
					try {
						Float.parseFloat(c.substring(0, index));
						Float.parseFloat(c.substring(index+1));
						}
					catch (NumberFormatException nfe) {
						showErrorMessage("Invalid coordinate(s): '"+c+"'");
						return false;
						}
					}
				}
			}
		return true;
		}

	@Override
	public void enableItems() {
	}

	@Override
	public void applyConfiguration(CompoundTableView view, Properties configuration, boolean isAdjusting) {
		// We don't need to apply if we have a predefined mode, because in that case getPredefinedConfiguration()
		// made the view change already to draw the proper configuration then from the view.
		if (view instanceof VisualizationPanel2D && mMode == 0) {
			JVisualization2D visualization = (JVisualization2D)((VisualizationPanel)view).getVisualization();
			visualization.setCrossHairList(configuration.getProperty(PROPERTY_COORDS));
			}
		}
	}
