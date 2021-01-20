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

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Properties;


public abstract class DETaskAbstractOpenFile extends ConfigurableTask implements ActionListener {
	protected static final String PROPERTY_FILENAME = "fileName";
	protected static final String ASK_FOR_FILE = "#ask#";

	private DataWarrior		mApplication;
	private JFilePathLabel	mFilePathLabel;
	private JButton			mButtonEdit;
	private JCheckBox		mCheckBoxChooseDuringMacro;
	private int				mAllowedFileTypes;
	private String			mDialogTitle;
	private String			mPredefinedFilePath;
	private DEFrame			mNewFrame;
	private volatile File	mFile;

	/**
	 * Creates an open-file task which only shows a configuration dialog, if the task
	 * is not invoked interactively. Otherwise a file chooser is shown to directly select
	 * the file to be opened.
	 * @param application
	 * @param dialogTitle
	 * @param allowedFileTypes
	 */
	public DETaskAbstractOpenFile(DataWarrior application, String dialogTitle, int allowedFileTypes) {
		super(application.getActiveFrame(), true);	// we want a progress bar
		// All tasks that use CompoundTableLoader need to run in an own thread if they run in a macro
		// to prevent the CompoundTableLoader to run processData() in a new thread without waiting
		// in the EDT
		mApplication = application;
		mDialogTitle = dialogTitle;
		mAllowedFileTypes = allowedFileTypes;
		mPredefinedFilePath = null;
		}

	/**
	 * Creates an open-file task with a file as parameter. This constructor is used when
	 * the user interactively chooses to open a specific file without file dialog.
	 * @param application
	 * @param dialogTitle
	 * @param allowedFileTypes
	 * @param filePath
	 */
	public DETaskAbstractOpenFile(DataWarrior application, String dialogTitle, int allowedFileTypes, String filePath) {
		super(application.getActiveFrame(), true);	// we want a progress bar
		mApplication = application;
		mDialogTitle = dialogTitle;
		mAllowedFileTypes = allowedFileTypes;
		mPredefinedFilePath = filePath;
		}

	public DataWarrior getApplication() {
		return mApplication;
		}

	/**
	 * For interactive execution this method returns the file name in a configuration,
	 * which suppresses a dialog. If a derived class has additional configuration properties,
	 * which need to be configured in createInnerDialogContent(), then that class must override
	 * this method and return null;
	 */
	@Override
	public Properties getPredefinedConfiguration() {
		if (isInteractive()) {
			String fileName = mPredefinedFilePath;
			if (fileName == null) {
				File file = askForFile(null);
				if (file != null)
					fileName = file.getAbsolutePath();
				}

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
	public DEFrame getNewFrontFrame() {
		return mNewFrame;
		}

	@Override
	public JPanel createDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, TableLayout.FILL, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED } };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		mFilePathLabel = new JFilePathLabel(!isInteractive());
		mFilePathLabel.setListener(this);
		content.add(new JLabel("File:"), "1,1");
		content.add(mFilePathLabel, "3,1");

		mButtonEdit = new JButton(JFilePathLabel.BUTTON_TEXT);
		mButtonEdit.addActionListener(this);
		content.add(mButtonEdit, "5,1");

		if (!isInteractive()) {
			mCheckBoxChooseDuringMacro = new JCheckBox("Choose file during macro execution");
			mCheckBoxChooseDuringMacro.addActionListener(this);
			content.add(mCheckBoxChooseDuringMacro, "1,3,5,3");
			}

		JPanel moreOptions = createInnerDialogContent();
		if (moreOptions != null)
			content.add(moreOptions, "1,5,5,5");
		
		return content;
		}

	/**
	 * Override this if your subclass needs more dialog options.
	 * There should not be any border except for an 8 pixel spacing at the bottom.
	 * @return
	 */
	public JPanel createInnerDialogContent() {
		return null;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String fileName = configuration.getProperty(PROPERTY_FILENAME);
		if (ASK_FOR_FILE.equals(fileName))
			return true;
		if (isLive && !isFileAndPathValid(fileName, false, false))
			return false;
		if ((FileHelper.getFileType(fileName) & mAllowedFileTypes) == 0) {
			showErrorMessage("Incompatible file type.");
			return false;
			}
		return true;
		}

	@Override
	public void setDialogConfigurationToDefault() {
		File file = null;
		if (isInteractive()) {
			String fileName = mPredefinedFilePath;
			if (fileName == null) {
				file = askForFile(null);
				if (file != null)
					fileName = file.getAbsolutePath();
				}
			mFilePathLabel.setPath(fileName);
			}
		else {
			mFilePathLabel.setPath(null);
			mCheckBoxChooseDuringMacro.setSelected(true);
			}
		enableItems();
		fileChanged(file);
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String fileName = configuration.getProperty(PROPERTY_FILENAME);
		mFilePathLabel.setPath(ASK_FOR_FILE.equals(fileName) ? null : fileName);
		if (!isInteractive())
			mCheckBoxChooseDuringMacro.setSelected(ASK_FOR_FILE.equals(fileName));
		enableItems();
		fileChanged(ASK_FOR_FILE.equals(fileName) || fileName==null ? null : new File(fileName));
		}

	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		if (!isInteractive() && mCheckBoxChooseDuringMacro.isSelected()) {
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
			File file = askForFile(resolvePathVariables(mFilePathLabel.getPath()));
			if (file != null) {
				mFilePathLabel.setPath(file.getAbsolutePath());
				fileChanged(file);
				}
			enableItems();
			return;
			}
		if (e.getSource() == mFilePathLabel) {
			enableItems();
			}
		if (!isInteractive() && e.getSource() == mCheckBoxChooseDuringMacro) {
			enableItems();
			return;
			}
		}

	private void enableItems() {
		boolean chooseDuringMacro = (!isInteractive() && mCheckBoxChooseDuringMacro.isSelected());
		mButtonEdit.setEnabled(!chooseDuringMacro);
		mFilePathLabel.setEnabled(!chooseDuringMacro);
		setOKButtonEnabled(chooseDuringMacro || mFilePathLabel.getPath() != null);
		}

	/**
	 * Override this, if additional user interface elements need to be updated from file content
	 * @param file
	 */
	protected void fileChanged(File file) {
		}

	private File askForFileInEDT(final String selectedFile) {
		return new FileHelper(getParentFrame()).selectFileToOpen(mDialogTitle, mAllowedFileTypes, selectedFile);
		}

	/**
	 * This method can be called from any thread
	 * @param selectedFile
	 * @return
	 */
	protected File askForFile(final String selectedFile) {
		if (SwingUtilities.isEventDispatchThread())
			return askForFileInEDT(selectedFile);

		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					mFile = askForFileInEDT(selectedFile);
					}
				});
			}
		catch (Exception e) {}

		return mFile;
		}

	@Override
	public boolean isConfigurable() {
		return true;
		}

	@Override
	public void runTask(Properties configuration) {
		String fileName = configuration.getProperty(PROPERTY_FILENAME);

// TODO Check this: Interactive tasks should never have the ASK_FOR_FILE property
		if (isInteractive() && ASK_FOR_FILE.equals(fileName))
			return;	// Is interactive and was cancelled. Don't create an error message.

		File file = null;
		if (ASK_FOR_FILE.equals(fileName)) {
			file = askForFile(null);
			if (file == null)
				return;	// no error message, because user cancelled and knows this
			}
		else {
			file = new File(resolvePathVariables(fileName));
			}

		if (SwingUtilities.isEventDispatchThread())
			mApplication.updateRecentFiles(file);
		else {
			final File _file = file;
			SwingUtilities.invokeLater(() -> mApplication.updateRecentFiles(_file));
			}

		mNewFrame = openFile(file, configuration);
		}

	public abstract DEFrame openFile(File file, Properties configuration);
	}
