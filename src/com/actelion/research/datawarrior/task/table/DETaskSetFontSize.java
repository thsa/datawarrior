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

package com.actelion.research.datawarrior.task.table;

import com.actelion.research.datawarrior.DEFormView;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.DETable;
import com.actelion.research.datawarrior.DETableView;
import com.actelion.research.datawarrior.task.AbstractViewTask;
import com.actelion.research.datawarrior.task.view.DETaskSetGraphicalViewOptions;
import com.actelion.research.gui.form.JFormView;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.view.CompoundTableView;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskSetFontSize extends AbstractViewTask {
    public static final String TASK_NAME = "Set Font Size";

    private static final String PROPERTY_FONT_SIZE = "fontSize";

    private JTextField mTextFieldFontSize;

	public DETaskSetFontSize(Frame owner, DEMainPane mainPane, CompoundTableView view) {
		super(owner, mainPane, view);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	public String getViewQualificationError(CompoundTableView view) {
		return (view instanceof DETableView || view instanceof DEFormView) ? null
				: "The 'Set Font Size' task applies to table and form views only."
				+ "\nTo change font sizes of graphical view use '"+ DETaskSetGraphicalViewOptions.TASK_NAME+"'.";
		}

	@Override
	public boolean isViewTaskWithoutConfiguration() {
		return false;
		}

	@Override
	public JComponent createInnerDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));
		content.add(new JLabel("Table Font Size:"), "1,1");
		mTextFieldFontSize = new JTextField(4);
		content.add(mTextFieldFontSize, "3,1");

		return content;
		}

	@Override
    public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_FONT_SIZE, mTextFieldFontSize.getText());
		return configuration;
		}

	@Override
    public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mTextFieldFontSize.setText(configuration.getProperty(PROPERTY_FONT_SIZE, ""));
		}

	@Override
    public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mTextFieldFontSize.setText(Integer.toString(DETable.DEFAULT_FONT_SIZE));
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (!super.isConfigurationValid(configuration, isLive))
			return false;

		String value = configuration.getProperty(PROPERTY_FONT_SIZE);
		if (value != null) {
			try {
				int size = Integer.parseInt(value);
				if (size < 4 || size > 64) {
					showErrorMessage("Font size out of range.");
					return false;
					}
				}
			catch (NumberFormatException nfe) {
				showErrorMessage("Font size is not an integer.");
				return false;
				}
			}

		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		CompoundTableView view = getConfiguredView(configuration);
		String fontSize = configuration.getProperty(PROPERTY_FONT_SIZE);
		if (view instanceof DETableView)
			((DETableView)view).getTable().setFontSize(fontSize == null ? DETable.DEFAULT_FONT_SIZE : Integer.parseInt(fontSize));
		else if (view instanceof DEFormView)
			((DEFormView)view).getCompoundTableForm().setFontSize(fontSize == null ? JFormView.DEFAULT_FONT_SIZE : Integer.parseInt(fontSize));
		}
	}
