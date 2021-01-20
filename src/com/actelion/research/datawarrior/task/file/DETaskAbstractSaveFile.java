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

import com.actelion.research.chem.io.CompoundFileHelper;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Properties;


public abstract class DETaskAbstractSaveFile extends ConfigurableTask implements ActionListener {
	protected static final String PROPERTY_FILENAME = "fileName";
	private static final String ASK_FOR_FILE = "#ask#";

	private DataWarrior		mApplication;
	private JFilePathLabel	mFilePathLabel;
	private JButton			mButtonEdit;
	private JCheckBox		mCheckBoxInteractive;
	private String			mDialogTitle;
	private CompoundTableModel	mTableModel;

	/**
	 * Creates a SaveAs/ExportAs task which only shows a configuration dialog,
	 * if the task is not invoked interactively.
	 * Otherwise a file chooser is shown to directly select the file to be saved.
	 * @param parent
	 * @param dialogTitle
	 */
	public DETaskAbstractSaveFile(DEFrame parent, String dialogTitle) {
		super(parent, false);
		mApplication = parent.getApplication();
		mTableModel = parent.getTableModel();
		mDialogTitle = dialogTitle;
		}

	public CompoundTableModel getTableModel() {
		return mTableModel;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		if (isInteractive()) {

			String fileName = askForFile(null);

			Properties configuration = new Properties();
			if (fileName != null)
				configuration.setProperty(PROPERTY_FILENAME, fileName);
			return configuration;
			}

		return null;	// show a configuration dialog
		}

	@Override
	public boolean isPredefinedStatusOK(Properties configuration) {
		return configuration.getProperty(PROPERTY_FILENAME) != null;	// a null indicates that the file dialog was cancelled
		}

	@Override
	public JPanel createDialogContent() {
		double[][] size = { {8, TableLayout.PREFERRED, TableLayout.FILL, 8},
							{8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 16, TableLayout.PREFERRED, 8, TableLayout.PREFERRED} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		mFilePathLabel = new JFilePathLabel(!isInteractive());
		mFilePathLabel.setListener(this);
		content.add(mFilePathLabel, "1,1,2,1");

		mButtonEdit = new JButton(JFilePathLabel.BUTTON_TEXT);
		mButtonEdit.addActionListener(this);
		content.add(mButtonEdit, "1,3");

		mCheckBoxInteractive = new JCheckBox("Choose file, when the macro is executed");
		mCheckBoxInteractive.addActionListener(this);
		content.add(mCheckBoxInteractive, "1,5,2,5");

		JComponent innerContent = createInnerDialogContent();
        if (innerContent != null)
        	content.add(innerContent, "0,7,3,7");

		return content;
		}

	/**
	 * Create the dialog content without the item used for file selection.
	 * @return null, if the file save task has no configuration other than file name.
	 */
	public abstract JComponent createInnerDialogContent();

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String fileName = configuration.getProperty(PROPERTY_FILENAME);
		if (ASK_FOR_FILE.equals(fileName))
			return true;
		if (isLive && !isFileAndPathValid(fileName, true, false))
			return false;
		if (FileHelper.getFileType(fileName) != getFileType()) {
			showErrorMessage("Incompatible file type.");
			return false;
			}
		return true;
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mFilePathLabel.setPath(null);
		mCheckBoxInteractive.setSelected(true);
		enableItems();
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String fileName = configuration.getProperty(PROPERTY_FILENAME);
		mFilePathLabel.setPath(fileName.equals(ASK_FOR_FILE) ? null : fileName);
		mCheckBoxInteractive.setSelected(fileName.equals(ASK_FOR_FILE));
		enableItems();
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		if (mCheckBoxInteractive.isSelected()) {
			configuration.setProperty(PROPERTY_FILENAME, ASK_FOR_FILE);
			}
		else {
			String fileName = mFilePathLabel.getPath();
			if (fileName != null)
				configuration.setProperty(PROPERTY_FILENAME, fileName);
			}

		return configuration;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(JFilePathLabel.BUTTON_TEXT)) {
			String fileName = askForFile(resolvePathVariables(mFilePathLabel.getPath()));
			if (fileName != null) {
				mFilePathLabel.setPath(fileName);
				enableItems();
				}
			return;
			}
		if (e.getSource() == mFilePathLabel) {
			enableItems();
			}
		if (e.getSource() == mCheckBoxInteractive) {
			enableItems();
			return;
			}
		}

	private void enableItems() {
		mButtonEdit.setEnabled(!mCheckBoxInteractive.isSelected());
		mFilePathLabel.setEnabled(!mCheckBoxInteractive.isSelected());
		setOKButtonEnabled(mCheckBoxInteractive.isSelected() || mFilePathLabel.getPath() != null);
		}

	private String askForFile(String suggestedName) {
		if (suggestedName == null) {
			suggestedName = getSuggestedFileName();
			}
		return new FileHelper(getParentFrame()).selectFileToSave(mDialogTitle, getFileType(), suggestedName);
		}

	/**
	 * Override this, if the initial suggested file name in the file-save-dialog
	 * shall not be derived from the document file name, or if not available from the
	 * window title.
	 * @return
	 */
	public String getSuggestedFileName() {
		File file = mTableModel.getFile();
		String suggestedName = (file == null) ? null : file.getAbsolutePath();
		if (suggestedName == null)
			suggestedName = ((DEFrame)getParentFrame()).getTitle().replace(' ', '_');
		if (suggestedName != null)
			suggestedName = CompoundFileHelper.removeExtension(suggestedName);
		return suggestedName;
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
	public void runTask(Properties configuration) {
		String fileName = configuration.getProperty(PROPERTY_FILENAME);

		if (isInteractive() && ASK_FOR_FILE.equals(fileName))
			return;	// Is interactive and was cancelled. Don't create an error message.

		if (ASK_FOR_FILE.equals(fileName)) {
			fileName = askForFile(null);
			if (fileName == null) {
				showErrorMessage("No file was chosen.");
				return;
				}
			}

		final File file = new File(resolvePathVariables(fileName));

		saveFile(file, configuration);

		if (SwingUtilities.isEventDispatchThread())
			mApplication.updateRecentFiles(file);
		else {
			SwingUtilities.invokeLater(
					new Runnable() {
						@Override
						public void run() {
							mApplication.updateRecentFiles(file);
						}
					}
				);
			}
		}

	public abstract int getFileType();
	public abstract void saveFile(File file, Properties configuration);
	}
