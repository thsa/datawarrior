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

import java.io.File;
import java.util.Properties;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.actelion.research.chem.io.CompoundFileHelper;
import com.actelion.research.datawarrior.DEFrame;

public abstract class DETaskAbstractSaveNativeFile extends DETaskAbstractSaveFile {
	private static final String PROPERTY_EMBED_DETAIL = "embedDetail";

	private boolean mVisibleOnly;
	private JCheckBox mCheckBoxEmbedDetails;

	public DETaskAbstractSaveNativeFile(DEFrame parent, String dialogTitle, boolean visibleOnly) {
		super(parent, dialogTitle);
		mVisibleOnly = visibleOnly;
		}

	@Override
	public final int getFileType() {
		return CompoundFileHelper.cFileTypeDataWarrior;
		}

	@Override
	public JComponent createInnerDialogContent() {
		JPanel p = new JPanel();
		double[][] size = { {8, TableLayout.PREFERRED, 8},
							{8, TableLayout.PREFERRED, 8} };
		p.setLayout(new TableLayout(size));

		mCheckBoxEmbedDetails = new JCheckBox("Embed referenced detail data");
		p.add(mCheckBoxEmbedDetails, "1,1");

		return p;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		Properties configuration = super.getPredefinedConfiguration();
		if (configuration != null && isPredefinedStatusOK(configuration)) {
			if (getTableModel().hasReferencedDetail()) {
				int option = JOptionPane.showConfirmDialog(getParentFrame(),
						"Your data includes references to external detail information e.g. images.\n"
					  + "Do you wish to include the detail information within your file?",
						"Embed detail information?",
						JOptionPane.YES_NO_OPTION);
				configuration.setProperty(PROPERTY_EMBED_DETAIL, (option == JOptionPane.YES_OPTION) ? "true" : "false");
				}
			}
		return configuration;
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mCheckBoxEmbedDetails.setSelected(false);
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mCheckBoxEmbedDetails.setSelected("true".equals(configuration.getProperty(PROPERTY_EMBED_DETAIL)));
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_EMBED_DETAIL, mCheckBoxEmbedDetails.isSelected() ? "true" : "false");
		return configuration;
		}

	@Override
	public boolean isConfigurable() {
		if (getTableModel().isEmpty()) {
			showErrorMessage("Empty documents cannot be saved.");
			return false;
			}
		return true;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (!super.isConfigurationValid(configuration, isLive))
			return false;

		if (isInteractive()) {
			for (int column=0; column<getTableModel().getTotalColumnCount(); column++) {
	            if (getTableModel().isDescriptorColumn(column)
	             && !getTableModel().isDescriptorAvailable(column)) {
	                int answer = JOptionPane.showConfirmDialog(getParentFrame(),
	                        "The descriptor calculation has not finished yet.\nDo you want to save anyway?", "Warning",
	                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
	
	                if (answer != JOptionPane.OK_OPTION) {
	                	return false;
	                	}
	                }
	            }
			}

		return true;
		}

	@Override
	public void saveFile(File file, Properties configuration) {
		if (!isInteractive()) {
			for (int column=0; column<getTableModel().getTotalColumnCount(); column++) {
				if (getTableModel().isDescriptorColumn(column)) {
					waitForDescriptor(getTableModel(), column);
					if (threadMustDie())
						return;
					}
				}
			}

		boolean embedDetail = "true".equals(configuration.getProperty(PROPERTY_EMBED_DETAIL));
		((DEFrame)getParentFrame()).saveNativeFile(file, mVisibleOnly, embedDetail);
		}
	}
