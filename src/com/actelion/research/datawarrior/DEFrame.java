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

package com.actelion.research.datawarrior;

import com.actelion.research.chem.io.CompoundFileHelper;
import com.actelion.research.datawarrior.task.DEMacroRecorder;
import com.actelion.research.datawarrior.task.file.*;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.CompoundTableSaver;
import com.actelion.research.table.DataDependentPropertyWriter;
import com.actelion.research.table.RuntimePropertyEvent;
import com.actelion.research.table.RuntimePropertyListener;
import com.actelion.research.table.model.*;
import com.actelion.research.table.view.CompoundTableView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Properties;

public class DEFrame extends JFrame implements ApplicationViewFactory,CompoundTableListener,CompoundTableListListener,RuntimePropertyListener {
	private static final long serialVersionUID = 0x20070227;
	private static final String DEFAULT_TITLE = "DataWarrior";

	private StandardMenuBar	mMenuBar;
	private DEParentPane	mParentPane;
	private DataWarrior		mDataExplorer;
	private String			mOsirisQuery;
	private Properties		mOsirisBioQuery;
	private boolean			mIsDirty;

  /**Construct the frame*/
	public DEFrame(DataWarrior datawarrior, String title, boolean lockForImmediateUsage) {
		mDataExplorer = datawarrior;
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		try {
			setIconImage(Toolkit.getDefaultToolkit().createImage(getClass().getResource("/images/datawarrior.png")));

			DECompoundTableModel tableModel = new DECompoundTableModel();
			tableModel.setHitlistHandler(new CompoundTableListHandler(tableModel));
			tableModel.addCompoundTableListener(this);
			tableModel.getListHandler().addCompoundTableListListener(this);
			tableModel.setDetailHandler(datawarrior.createDetailHandler(this, tableModel));

			mParentPane = new DEParentPane(this, tableModel,
										   datawarrior.createDetailPane(this, tableModel),
										   datawarrior.createDatabaseActions(this));
			mParentPane.addRuntimePropertyListener(this);
			mParentPane.getMainPane().setApplicationViewFactory(this);
			if (lockForImmediateUsage)
				mParentPane.getTableModel().lock();

			Dimension size = new Dimension(HiDPIHelper.scale(1024), HiDPIHelper.scale(768));
			setSize(size);
			setPreferredSize(size);
			setTitle(title == null ? DEFAULT_TITLE : title);
			setContentPane(mParentPane);

			mMenuBar = datawarrior.createMenuBar(this);
			setJMenuBar(mMenuBar);

			mIsDirty = false;	// an empty document is not dirty
			}
		catch(Exception e) {
			e.printStackTrace();
			}
		}

	/**
	 * This was taken from the IdeFrameImpl class of the Intellij source code. Without this override frames could not be larger
	 * than one screen. In multi-monitor setups, however, frames that span more than one monitor may be useful
	 * 
	 * original intelij comment:
	 * This is overridden to get rid of strange Alloy LaF customization of frames. For unknown reason it sets the maxBounds rectangle
	 * and it does it plain wrong. Setting bounds to {@code null} means default value should be taken from the underlying OS.
	 */
	@Override
	public synchronized void setMaximizedBounds(Rectangle bounds) {
		super.setMaximizedBounds(null);
		}

	public CompoundTableView createApplicationView(int type, Frame frame) {
		if (type == DEMainPane.VIEW_TYPE_MACRO_EDITOR)
			return new DEMacroEditor((DEFrame)frame);

		return null;
		}

	public DataWarrior getApplication() {
		return mDataExplorer;
		}

	public StandardMenuBar getDEMenuBar() {
		return mMenuBar;
		}

	public DEParentPane getMainFrame() {
		return mParentPane;
		}

	@Deprecated
	public String getOsirisQuery() {
		return mOsirisQuery;
		}

	@Deprecated
	public void setOsirisQuery(String osirisQuery) {
		mOsirisQuery = osirisQuery;
		}

	public Properties getOsirisBioQuery() {
		return mOsirisBioQuery;
		}

	public void setOsirisBioQuery(Properties osirisBioQuery) {
		mOsirisBioQuery = osirisBioQuery;
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		if (e.getType() != CompoundTableEvent.cChangeExcluded
		 && e.getType() != CompoundTableEvent.cChangeSelection)
			setDirty(true);
		}

	public void listChanged(CompoundTableListEvent e) {
		setDirty(true);
		}

	public void runtimePropertyChanged(RuntimePropertyEvent e) {
		setDirty(true);
		}

	public boolean isDirty() {
		return mIsDirty;
		}

	public void setDirty(boolean isDirty) {
		if (mIsDirty != isDirty) {
			mIsDirty = isDirty;
			getRootPane().putClientProperty("Window.documentModified", isDirty ? Boolean.TRUE : Boolean.FALSE);
			}
		}

	/**
	 * Creates a suggested file name without extension to be used in a file-save-dialog
	 * to save derived files. If the table model has a file, then the file's name without extension
	 * is returned. Otherwise, if the frame has a title, the title (without known extensions) is returned.
	 * Otherwise "Untitled" is returned.
	 * @return suggested file name without extension
	 */
	public String getSuggestedFileName() {
		File file = mParentPane.getTableModel().getFile();
		String fileName = CompoundFileHelper.removePathAndExtension((file != null) ? file.getName() : getTitle());
		return (fileName.length() == 0) ? "Untitled" : CompoundFileHelper.removePathAndExtension(fileName);
		}

	/**
	 * Creates a new file with a suggested non-existing name in the home directory.
	 * The name is derived from the window title or from "Untitled", depending on whether a window title exists.
	 * @return
	 */
	private File suggestUniqueFile() {
		String fileName = CompoundFileHelper.removePathAndExtension((getTitle().length() == 0) ? getTitle() : "Untitled");
		String filePath = System.getProperty("user.home").concat(File.pathSeparator).concat(fileName);
		String extension = FileHelper.getExtension(FileHelper.cFileTypeDataWarrior);
		File file = new File(filePath.concat(extension));
		while (file.exists()) {
			int index = filePath.lastIndexOf(' ');
			if (index == -1)
				filePath = filePath.concat(" 2");
			else {
				try {
					int suffix = Integer.parseInt(filePath.substring(index+1));
					filePath = filePath.substring(0, index+1).concat(Integer.toString(suffix+1));
					}
				catch (NumberFormatException nfe) {
					filePath = filePath.concat(" 2");
					}
				}
			}
		return file;
		}

	/*	public void saveFile(final int fileType, final boolean useNewFile, final boolean visibleOnly) {
		JTable table = mParentPane.getMainPane().getTable();
		DERuntimeProperties rtp = (fileType == FileHelper.cFileTypeDataWarrior
								|| fileType == FileHelper.cFileTypeDataWarriorTemplate) ? new DERuntimeProperties(mParentPane) : null;
		if (table != null) {
			File file = null;
			String newFileName = null;
			if (!useNewFile) {
				file = mParentPane.getTableModel().getFile();
				if (file != null && fileType != FileHelper.getFileType(file.getName()))
					file = null;
				}
			if (file == null) {
				newFileName = getTitle();
				int titleFileType = FileHelper.getFileType(newFileName);
				if (titleFileType != FileHelper.cFileTypeDataWarrior && titleFileType != FileHelper.cFileTypeUnknown)
					newFileName = newFileName.substring(0, newFileName.lastIndexOf('.'))+".dwar";
				}
			new CompoundTableSaver(this, mParentPane.getMainPane().getTable()) {
				public void finalStatus(File file) {
					if (file != null
					 && fileType == FileHelper.cFileTypeDataWarrior
					 && !visibleOnly)
						setDirty(false);
					}
				}.save(rtp, fileType, file, newFileName, visibleOnly);
			}
		}*/

	/**
	 * Saves the frames content into the given file without asking any questions.
	 * @param file proper file with proper privileges
	 * @param visibleOnly
	 * @param embedDetails
	 */
	public void saveNativeFile(final File file, final boolean visibleOnly, boolean embedDetails) {
		DERuntimeProperties rtp = new DERuntimeProperties(mParentPane);
		CompoundTableSaver saver = new CompoundTableSaver(this, mParentPane.getTableModel(), mParentPane.getMainPane().getTable()) {
			public void finalStatus(File file) {
				if (file != null
				 && !visibleOnly)
					setDirty(false);
				}
			};
		DataDependentPropertyWriter clpw = new CustomLabelPositionWriter(getMainFrame().getMainPane());
		DataDependentPropertyWriter cvpw = new CardViewPositionWriter(getMainFrame().getMainPane());
		saver.addDataDependentPropertyWriter(clpw);
		saver.addDataDependentPropertyWriter(cvpw);
		saver.saveNative(rtp, file, visibleOnly, embedDetails);
		}

	/**
	 * @return true if frame can be closed savely
	 */
	protected boolean askSaveDataIfDirty() {
		// returns true if tablemodel can be replaced
		if (!mIsDirty || mParentPane.getTableModel().isEmpty())
			return true;

		boolean hasFile = (mParentPane.getTableModel().getFile() != null);
		
		int save = JOptionPane.showConfirmDialog(this,
					hasFile ? "Do you want to save the changes?"
							: "Do you want to save this window's data\nwith all view and filter settings?",
					hasFile ? "Save Changes?"
							: "Save Window Content?",
					JOptionPane.YES_NO_CANCEL_OPTION);
	
		if (save == JOptionPane.YES_OPTION) {
			if (hasFile)
				new DETaskSaveFile(this).defineAndRun();
			else
				new DETaskSaveFileAs(this).defineAndRun();

			return !mIsDirty;
			}
	
		return (save == JOptionPane.NO_OPTION);
		}

	protected void saveSilentlyIfDirty() {
		if (!mIsDirty || mParentPane.getTableModel().isEmpty())
			return;

		File file = mParentPane.getTableModel().getFile();
		if (file == null) {
			file = suggestUniqueFile();
			mParentPane.getTableModel().setFile(file);
			}

		saveNativeFile(file, false, true);
		}

	/**
	 * @return true if frame can be closed savely, i.e. no macro is recording (anymore)
	 */
	protected boolean askStopRecordingMacro() {
		if (!DEMacroRecorder.getInstance().isRecording()
		 || DEMacroRecorder.getInstance().getRecordingMacroOwner() != this)
			return true;

		int stop = JOptionPane.showConfirmDialog(this,
				"Do you want to stop recording the macro?",
				"Stop Recording?",
				JOptionPane.OK_CANCEL_OPTION);

		if (stop == JOptionPane.OK_OPTION) {
			DEMacroRecorder.getInstance().stopRecording();
			return true;
			}
	
		return false;
		}

	/**
	 * Depending on whether a macro is currently recording or running, this method
	 * updates enabling of macro menu and the macro display area.
	 */
	public void updateMacroStatus() {
		mMenuBar.enableMacroItems();
		getMainFrame().getMainPane().getMacroProgressPanel().showMessage(
				DEMacroRecorder.getInstance().isRecording() ? DEMacroRecorder.RECORDING_MESSAGE : "");
		}

	public DECompoundTableModel getTableModel() {
		return mParentPane.getTableModel();
		}

	@Override
	protected void processWindowEvent(WindowEvent e) {
		if (e.getID() == WindowEvent.WINDOW_CLOSING)
			new DETaskCloseWindow(mDataExplorer.getActiveFrame(), mDataExplorer, this).defineAndRun();
		}

	public void removeAllViews() {
		mParentPane.getMainPane().removeAllViews();
		setTitle(DEFAULT_TITLE);
		}
	}
