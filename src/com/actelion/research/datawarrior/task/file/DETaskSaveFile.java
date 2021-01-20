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

package com.actelion.research.datawarrior.task.file;

import info.clearthought.layout.TableLayout;

import java.util.Properties;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.table.model.CompoundTableModel;

public class DETaskSaveFile extends ConfigurableTask {
    public static final String TASK_NAME = "Save File";

	private static final String PROPERTY_EMBED_DETAIL = "embedDetail";

	private JCheckBox mCheckBoxEmbedDetails;

	public DETaskSaveFile(DEFrame parent) {
		super(parent, false);
		}

	@Override
	public Properties getPredefinedConfiguration() {
		if (isInteractive()) {
			Properties configuration = new Properties();
			if (((DEFrame)getParentFrame()).getTableModel().hasReferencedDetail()) {
				int option = JOptionPane.showConfirmDialog(getParentFrame(),
						"Your data includes references to external detail information e.g. images.\n"
					  + "Do you wish to include the detail information within your file?",
						"Embed detail information?",
						JOptionPane.YES_NO_OPTION);
				configuration.setProperty(PROPERTY_EMBED_DETAIL, (option == JOptionPane.YES_OPTION) ? "true" : "false");
				}
			return configuration;
			}
		return null;
		}

	@Override
	public JComponent createDialogContent() {
		JPanel p = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8} };
		p.setLayout(new TableLayout(size));

		mCheckBoxEmbedDetails = new JCheckBox("Embed referenced detail data");
		p.add(mCheckBoxEmbedDetails, "1,1");

		return p;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_EMBED_DETAIL, mCheckBoxEmbedDetails.isSelected() ? "true" : "false");
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		mCheckBoxEmbedDetails.setSelected("true".equals(configuration.getProperty(PROPERTY_EMBED_DETAIL)));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mCheckBoxEmbedDetails.setSelected(false);
		}

	@Override
	public boolean isConfigurable() {
		CompoundTableModel tableModel = ((DEFrame)getParentFrame()).getTableModel();
		if (tableModel.isEmpty()) {
			showErrorMessage("Empty documents cannot be saved.");
			return false;
			}
		if (tableModel.getFile() == null) {
			showErrorMessage("The data of his window was never saved to a file. Use 'Save As...' instead.");
			return false;
			}
		if (!isFileAndPathValid(tableModel.getFile().getPath(), true, false)) {
			showErrorMessage("The file '"+tableModel.getFile().getName()+"' cannot be saved.");
			return false;
			}

		return true;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		boolean embedDetail = "true".equals(configuration.getProperty(PROPERTY_EMBED_DETAIL));
		((DEFrame)getParentFrame()).saveNativeFile(((DEFrame)getParentFrame()).getTableModel().getFile(), false, embedDetail);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}
	}
