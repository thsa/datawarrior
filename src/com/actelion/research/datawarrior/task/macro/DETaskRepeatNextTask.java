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

package com.actelion.research.datawarrior.task.macro;

import com.actelion.research.chem.io.CompoundFileHelper;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.datawarrior.task.file.JFilePathLabel;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Properties;


public class DETaskRepeatNextTask extends ConfigurableTask implements ActionListener {
	public static final String TASK_NAME = "Repeat Next Tasks";

	public static final String CANCELLED_DIR = "<cancelled>";

	private static final String PROPERTY_DIRECTORY = "dir";
	private static final String PROPERTY_FILETYPE = "filetype";
	private static final String PROPERTY_COUNT = "count";
	private static final String PROPERTY_ALL_TASKS = "all";

	private static final String ASK_FOR_FILE = "#ask#";

	public static final int TASK_COUNT_ONE = 0;
	public static final int TASK_COUNT_ALL = 1;
	public static final int TASK_COUNT_TILL_LABEL = 2;
	private static final String[] TASK_COUNT_CODE = {"false", "true", "label"};
	private static final String[] TASK_COUNT__ITEM = {"just next task", "all following tasks", "all tasks till next label"};

	private static final int FILETYPE_DATAWARRIOR = 0;
	private static final int FILETYPE_SD = 1;
	private static final int FILETYPE_TEXT = 2;
	private static final int FILETYPE_DIR = 3;
	private static final String[] FILETYPE_ITEM = {"DataWarrior", "SD-Files", "Text", "Directories"};
	private static final String[] FILETYPE_CODE = {"datawarrior", "sd", "text", "dir"};

	private JTextField		mTextFieldCount;
    private JComboBox		mComboBoxTasks,mComboBoxFileType;
	private JFilePathLabel	mFilePathLabel;
	private JButton			mButtonEdit;
	private JCheckBox		mCheckBoxChooseDuringMacro;
	private JRadioButton	mRadioButtonForever,mRadioButtonCount,mRadioButtonFiles;
	private File            mDirectory;

	public DETaskRepeatNextTask(Frame sourceFrame) {
		super(sourceFrame, true);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isConfigurable() {
        return true;
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
    public JPanel createDialogContent() {
        ButtonGroup buttonGroup = new ButtonGroup();

        JPanel gp = new JPanel();
        int gap = HiDPIHelper.scale(8);
        double[][] size = { {gap, HiDPIHelper.scale(24), TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, TableLayout.FILL, TableLayout.PREFERRED, gap},
                            {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED,
							 gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED, gap} };
        gp.setLayout(new TableLayout(size));

		double[][] size1 = { {TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, TableLayout.FILL}, {TableLayout.PREFERRED} };
		JPanel tasksPanel = new JPanel(new TableLayout(size1));
		mComboBoxTasks = new JComboBox(TASK_COUNT__ITEM);
		tasksPanel.add(new JLabel("Repeat"), "0,0,");
		tasksPanel.add(mComboBoxTasks, "2,0");
        gp.add(tasksPanel, "1,1,6,1");

		mRadioButtonForever = new JRadioButton("Repeat forever");
		mRadioButtonForever.addActionListener(this);
		buttonGroup.add(mRadioButtonForever);
        gp.add(mRadioButtonForever, "1,3,6,3");

		double[][] size2 = { {TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, TableLayout.FILL}, {TableLayout.PREFERRED} };
		JPanel countPanel = new JPanel(new TableLayout(size2));
		mRadioButtonCount = new JRadioButton("Repeat");
		mRadioButtonCount.addActionListener(this);
		mTextFieldCount = new JTextField("10", 2);
		buttonGroup.add(mRadioButtonCount);
		countPanel.add(mRadioButtonCount, "0,0");
		countPanel.add(mTextFieldCount, "2,0");
		countPanel.add(new JLabel("times"), "4,0");
		gp.add(countPanel, "1,5,6,5");

		mRadioButtonFiles = new JRadioButton("Repeat for every file in directory using variable $FILENAME");
		mRadioButtonFiles.addActionListener(this);
		buttonGroup.add(mRadioButtonFiles);
        gp.add(mRadioButtonFiles, "1,7,6,7");

		mFilePathLabel = new JFilePathLabel(!isInteractive());
		mButtonEdit = new JButton(JFilePathLabel.BUTTON_TEXT);
		mButtonEdit.addActionListener(this);
		gp.add(new JLabel("Directory:"), "2,9");
		gp.add(mFilePathLabel, "4,9");
		gp.add(mButtonEdit, "6,9");

		mCheckBoxChooseDuringMacro = new JCheckBox("Choose directory during macro execution");
		mCheckBoxChooseDuringMacro.addActionListener(this);
		gp.add(mCheckBoxChooseDuringMacro, "2,11,6,11");

		double[][] size3 = { {TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, TableLayout.FILL}, {TableLayout.PREFERRED} };
		JPanel filetypePanel = new JPanel(new TableLayout(size3));
		mComboBoxFileType = new JComboBox(FILETYPE_ITEM);
		filetypePanel.add(new JLabel("Allowed file types:"), "0,0");
		filetypePanel.add(mComboBoxFileType, "2,0");
		gp.add(filetypePanel, "2,13,6,13");

		gp.add(new JLabel("Variable $LOOPINDEX has values 1,2,3, ...", JLabel.CENTER), "1,15,6,15");

		return gp;
		}

	@Override
    public Properties getDialogConfiguration() {
    	Properties configuration = new Properties();

    	if (mRadioButtonCount.isSelected())
    		configuration.setProperty(PROPERTY_COUNT, mTextFieldCount.getText());

   		configuration.setProperty(PROPERTY_ALL_TASKS, TASK_COUNT_CODE[mComboBoxTasks.getSelectedIndex()]);

   		if (mRadioButtonFiles.isSelected()) {
			if (!isInteractive() && mCheckBoxChooseDuringMacro.isSelected()) {
				configuration.setProperty(PROPERTY_DIRECTORY, ASK_FOR_FILE);
				}
			else {
				String dirName = mFilePathLabel.getPath();
				if (dirName != null)
					configuration.setProperty(PROPERTY_DIRECTORY, dirName);
				}
			configuration.setProperty(PROPERTY_FILETYPE, FILETYPE_CODE[mComboBoxFileType.getSelectedIndex()]);
			}

		return configuration;
		}

	@Override
    public void setDialogConfiguration(Properties configuration) {
		mComboBoxTasks.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_ALL_TASKS), TASK_COUNT_CODE, TASK_COUNT_ONE));

		String count = configuration.getProperty(PROPERTY_COUNT, "");
		int filetype = findListIndex(configuration.getProperty(PROPERTY_FILETYPE, ""), FILETYPE_CODE, -1);
		mTextFieldCount.setText(count);

		mRadioButtonForever.setSelected(count.length() == 0 && filetype == -1);
		mRadioButtonCount.setSelected(count.length() != 0);
		mRadioButtonFiles.setSelected(filetype != -1);

		String dirName = configuration.getProperty(PROPERTY_DIRECTORY);
		mFilePathLabel.setPath(ASK_FOR_FILE.equals(dirName) ? null : dirName);
		if (!isInteractive())
			mCheckBoxChooseDuringMacro.setSelected(ASK_FOR_FILE.equals(dirName));

		if (filetype != -1)
			mComboBoxFileType.setSelectedIndex(filetype);
		}

	@Override
    public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		try {
			String value = configuration.getProperty(PROPERTY_COUNT);
			if (value != null) {
				int count = Integer.parseInt(value);
				if (count < 2) {
					showErrorMessage("Repetition count must be at least 2.");
					return false;
					}
				}
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("Repetition count is not numerical.");
			return false;
			}

		String dirName = configuration.getProperty(PROPERTY_DIRECTORY);
		if (dirName != null) {
			if (ASK_FOR_FILE.equals(dirName))
				return true;
			if (isLive && !isFileAndPathValid(dirName, false, false))
				return false;
			}

		return true;
		}

	@Override
    public void setDialogConfigurationToDefault() {
		mTextFieldCount.setText("10");
		mRadioButtonForever.setSelected(true);
		mComboBoxTasks.setSelectedIndex(TASK_COUNT_ONE);

		mFilePathLabel.setPath(null);
		mCheckBoxChooseDuringMacro.setSelected(true);
		}

	@Override
    public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(JFilePathLabel.BUTTON_TEXT)) {
			File file = new FileHelper(getParentFrame()).selectFileToOpen("Choose Directory With Files To Process", CompoundFileHelper.cFileTypeDirectory, mFilePathLabel.getPath());
			if (file != null) {
				mFilePathLabel.setPath(file.getAbsolutePath());
			}
		}

		mTextFieldCount.setEnabled(mRadioButtonCount.isSelected());
		mCheckBoxChooseDuringMacro.setEnabled(mRadioButtonFiles.isSelected());
		mComboBoxFileType.setEnabled(mRadioButtonFiles.isSelected());
		boolean fileSelectionEnabled = mRadioButtonFiles.isSelected() && !mCheckBoxChooseDuringMacro.isSelected();
		mFilePathLabel.setEnabled(fileSelectionEnabled);
		mButtonEdit.setEnabled(fileSelectionEnabled);
		}

	@Override
	public void runTask(Properties configuration) {
		// don't do anything
		}

	/**
	 * @param configuration
	 * @return repetition count or -1 of forever
	 */
    public int getRepetitions(Properties configuration) {
    	return Integer.parseInt(configuration.getProperty(PROPERTY_COUNT, "-1"));
        }

    public int getTaskCountMode(Properties configuration) {
    	return findListIndex(configuration.getProperty(PROPERTY_ALL_TASKS), TASK_COUNT_CODE, TASK_COUNT_ONE);
    	}

	public String getDirectory(Properties configuration) {
    	String directoryName = configuration.getProperty(PROPERTY_DIRECTORY);

		if (ASK_FOR_FILE.equals(directoryName)) {
			File dir = askForFile(null);
			if (dir == null)
				return CANCELLED_DIR;	// no error message, because user cancelled and knows this
			directoryName = dir.getAbsolutePath();
			}
		else {
			directoryName = resolvePathVariables(directoryName);
			}

		return directoryName;
		}

	private File askForFileInEDT(final String selectedFile) {
		return new FileHelper(getParentFrame()).selectFileToOpen("Select Directory", FileHelper.cFileTypeDirectory, selectedFile);
		}

	/**
	 * This method can be called from any thread
	 * @param selectedFile
	 * @return
	 */
	private File askForFile(final String selectedFile) {
		if (SwingUtilities.isEventDispatchThread())
			return askForFileInEDT(selectedFile);

		try {
			SwingUtilities.invokeAndWait(() -> mDirectory = askForFileInEDT(selectedFile) );
			}
		catch (Exception e) {}

		return mDirectory;
		}

	public int getFiletypes(Properties configuration) {
		int filetypes = findListIndex(configuration.getProperty(PROPERTY_FILETYPE), FILETYPE_CODE, FILETYPE_TEXT);
		switch (filetypes) {
		case FILETYPE_DATAWARRIOR:
			return CompoundFileHelper.cFileTypeDataWarrior;
		case FILETYPE_SD:
			return CompoundFileHelper.cFileTypeSD;
		case FILETYPE_TEXT:
			return CompoundFileHelper.cFileTypeText;
		case FILETYPE_DIR:
			return CompoundFileHelper.cFileTypeDirectory;
		default:
			return CompoundFileHelper.cFileTypeUnknown;
			}
		}
	}