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

package com.actelion.research.datawarrior.task.view;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.dock.Dockable;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public abstract class DETaskAbstractNewView extends ConfigurableTask {
	private static final String PROPERTY_NEW_VIEW = "newView";
	private static final String PROPERTY_WHERE_VIEW = "whereView";
	private static final String PROPERTY_WHERE = "where";
	private static final String PROPERTY_SPLITTING = "splitting";

	private static final String[] TEXT_RELATION = { "Center",  "Top", "Left", "Bottom", "Right" };
	private static final String[] CODE_WHERE = { "center",  "top", "left", "bottom", "right" };

	private DEMainPane	mMainPane;
	private JTextField	mTextFieldViewName,mTextFieldSplitting;
	private JComboBox	mComboBoxView,mComboBoxWhere;
	private String		mWhereViewName;

	public DETaskAbstractNewView(Frame parent, DEMainPane mainPane, String whereViewName) {
		super(parent, false);
		mMainPane = mainPane;
		mWhereViewName = whereViewName;
	}

	public abstract void addInnerDialogContent(JPanel content);
	public abstract String getDefaultViewName();
	public abstract void createNewView(String viewName, String whereView, String where, Properties configuration);

	public DEMainPane getMainPane() {
		return mMainPane;
	}

		@Override
	public JComponent createDialogContent() {
		int gap = HiDPIHelper.scale(8);
		JPanel p = new JPanel();
		double[][] size = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED, gap},
				{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };
		p.setLayout(new TableLayout(size));
/*
for (Dockable d:mMainPane.getDockables())
 System.out.println("isVisible:"+d.isVisible()+" isVisibleDockable:"+d.isVisibleDockable());
*/
		p.add(new JLabel("New view's name:"), "1,1");
		mTextFieldViewName = new JTextField();
		p.add(mTextFieldViewName, "3,1,5,1");

		p.add(new JLabel("New view's position:"), "1,3");

		mComboBoxWhere = new JComboBox(TEXT_RELATION);
		p.add(mComboBoxWhere, "3,3");

		p.add(new JLabel(" of "), "4,3");

		mComboBoxView = new JComboBox();
		for (Dockable d:mMainPane.getDockables())
			if (d.isVisibleDockable())
				mComboBoxView.addItem(d.getTitle());
		mComboBoxView.setEditable(!isInteractive());
		p.add(mComboBoxView, "5,3");

		p.add(new JLabel("Space splitting:"), "1,5");
		mTextFieldSplitting = new JTextField();
		p.add(mTextFieldSplitting, "3,5");

		addInnerDialogContent(p);

		return p;
	}

	@Override
	public Properties getPredefinedConfiguration() {
		if (mWhereViewName == null)
			return null;

		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_WHERE_VIEW, mWhereViewName);
		configuration.setProperty(PROPERTY_WHERE, CODE_WHERE[0]);
		configuration.setProperty(PROPERTY_NEW_VIEW, getDefaultViewName());
		return configuration;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_NEW_VIEW, mTextFieldViewName.getText());
		configuration.setProperty(PROPERTY_WHERE_VIEW, (String)mComboBoxView.getSelectedItem());
		configuration.setProperty(PROPERTY_WHERE, CODE_WHERE[mComboBoxWhere.getSelectedIndex()]);
		if (mTextFieldSplitting.getText().length() != 0)
			configuration.setProperty(PROPERTY_SPLITTING, mTextFieldSplitting.getText());
		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mTextFieldViewName.setText(configuration.getProperty(PROPERTY_NEW_VIEW, getDefaultViewName()));
		String whereViewName = configuration.getProperty(PROPERTY_WHERE_VIEW);
		mComboBoxView.setSelectedItem(whereViewName);
		int where = findListIndex(configuration.getProperty(PROPERTY_WHERE), CODE_WHERE, 0);
		mComboBoxWhere.setSelectedIndex(where);
		mTextFieldSplitting.setText(configuration.getProperty(PROPERTY_SPLITTING, "0.5"));
	}

	@Override
	public void setDialogConfigurationToDefault() {
		mTextFieldViewName.setText("Untitled "+getDefaultViewName());
		mComboBoxView.setSelectedItem(mMainPane.getSelectedViewTitle());
		mTextFieldSplitting.setText("0.5");
	}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String viewName = configuration.getProperty(PROPERTY_WHERE_VIEW);
		if (viewName == null) {
			showErrorMessage("View name not defined.");
			return false;
		}
		String splitting = configuration.getProperty(PROPERTY_SPLITTING, "");
		if (splitting.length() != 0) {
			try {
				double s = Double.parseDouble(splitting);
				if (s < 0.1 || s > 1.0) {
					showErrorMessage("The splitting value must be between 0.1 and 1.0.");
					return false;
				}
			}
			catch (NumberFormatException nfe) {
				showErrorMessage("Splitting value is not numerical.");
				return false;
			}
		}
		if (isLive) {
			Dockable dockable = mMainPane.getDockable(viewName);
			if (dockable == null) {
				showErrorMessage("View '"+viewName+"' not found.");
				return false;
			}
	/*		if (!dockable.isVisibleDockable()) {	this should not be an issue
				showErrorMessage("View '"+viewName+"' is not visible.");
				return false;
				}*/
		}
		return true;
	}

	@Override
	public void runTask(Properties configuration) {
		String viewName = configuration.getProperty(PROPERTY_NEW_VIEW);
		String whereView = configuration.getProperty(PROPERTY_WHERE_VIEW);
		String where = CODE_WHERE[findListIndex(configuration.getProperty(PROPERTY_WHERE), CODE_WHERE, 0)];
		String splitting = configuration.getProperty(PROPERTY_SPLITTING, "");
		if (splitting.length() != 0)
			where = where.concat("\t").concat(splitting);
		createNewView(viewName, whereView, where, configuration);
	}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
	}
}
