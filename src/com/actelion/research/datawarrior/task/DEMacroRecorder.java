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

package com.actelion.research.datawarrior.task;

import com.actelion.research.calc.ProgressController;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.macro.*;

import javax.swing.*;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;


public class DEMacroRecorder implements ProgressController,Runnable {
	public static final String RECORDING_MESSAGE = "Recording Macro...";
	private static volatile DEMacroRecorder sRecorder = null;

	public static final int MESSAGE_MODE_SHOW_ERRORS = 0;
	public static final int MESSAGE_MODE_SHOW_FIRST_ERROR = 1;
	public static final int MESSAGE_MODE_SKIP_ERRORS = 2;
	public static final int DEFAULT_MESSAGE_MODE = MESSAGE_MODE_SHOW_ERRORS;

	private volatile boolean	mIsRecording;
	private volatile Thread		mMacroThread;
	private volatile DEFrame	mFrontFrame;
	private volatile DEMacro	mRunningMacro;
	private volatile int		mMessageMode;
	private volatile StandardTaskFactory	mTaskFactory;
	private volatile ConcurrentHashMap<String,String> mVariableMap;
	private DEFrame				mRecordingMacroOwner,mFrontFrameWhenRecordingStopped;
	private DEMacro				mRecordingMacro;

	/**
	 * If the macro recorder is currently recording a macro, then this method records a the given task.
	 * This method can savely be called from any thread. It waits until the task is recorded.
	 * @param task
	 * @param configuration
	 */
	public static void record(final AbstractTask task, final Properties configuration) {
		if (sRecorder != null && sRecorder.mIsRecording) {
			if (SwingUtilities.isEventDispatchThread()) {
				sRecorder.recordTask(task, configuration);
				}
			else {
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						@Override
						public void run() {
							sRecorder.recordTask(task, configuration);
							}
						} );
					}
				catch (Exception e) {}
				}
			}
		}

	public static DEMacroRecorder getInstance() {
		if (sRecorder == null)
			sRecorder = new DEMacroRecorder();

		return sRecorder;
		}

	private DEMacroRecorder() {
		mMessageMode = DEFAULT_MESSAGE_MODE;
		}

	public void setTaskFactory(StandardTaskFactory tf) {
		mTaskFactory = tf;
		}

	public void startRecording(DEMacro macro, DEFrame macroOwner) {
		mRecordingMacro = macro;
		mRecordingMacroOwner = macroOwner;
		mIsRecording = true;
		for (DEFrame frame:macroOwner.getApplication().getFrameList())
			frame.updateMacroStatus();
		}

	public void continueRecording() {
		mIsRecording = true;
		for (DEFrame frame:mRecordingMacroOwner.getApplication().getFrameList())
			frame.updateMacroStatus();
		}

	public boolean canContinueRecording(DEFrame frontFrame) {
		return !mIsRecording
			&& mRecordingMacro != null
			&& mFrontFrameWhenRecordingStopped == frontFrame;
		}

	public boolean isRecording() {
		return mIsRecording;
		}

	/**
	 * @return the owner of the currently recording macro or null
	 */
	public DEFrame getRecordingMacroOwner() {
		return mIsRecording ? mRecordingMacroOwner : null;
		}

	public void stopRecording() {
		mIsRecording = false;
		mFrontFrameWhenRecordingStopped = mRecordingMacroOwner.getApplication().getActiveFrame();
		for (DEFrame frame:mRecordingMacroOwner.getApplication().getFrameList())
			frame.updateMacroStatus();
		}

	public boolean isRecording(DEMacro macro) {
		return (mIsRecording && mRecordingMacro == macro);
		}

	public boolean isRunningMacro() {
		return (mMacroThread != null);
		}

	public String getVariable(String name) {
		return mVariableMap.get(name);
		}

	public int getMessageMode() {
		return mMessageMode;
		}

	public void setMessageMode(int mode) {
		mMessageMode = mode;
		}

	public void setVariable(String name, String value) {
		mVariableMap.put(name, value);
		}

	public String resolveVariables(String text) {
		if (text != null)
			for (String name:mVariableMap.keySet())
				text = text.replace("$".concat(name), mVariableMap.get(name));
		return text;
		}

	private void recordTask(AbstractTask task, Properties configuration) {
		String taskCode = mTaskFactory.getTaskCodeFromName(task.getTaskName());
		int previous = mRecordingMacro.getTaskCount()-1;
		if (previous != -1
		 && taskCode.equals(mRecordingMacro.getTaskCode(previous))
		 && task.isRedundant(mRecordingMacro.getTaskConfiguration(previous), configuration))
			mRecordingMacro.setTaskConfiguration(previous, configuration);
		else
			mRecordingMacro.addTask(taskCode, configuration);
		mRecordingMacroOwner.setDirty(true);
		}

	public void runMacro(DEMacro macro, DEFrame frontFrame) {
		if (!macro.isEmpty()) {
			if (mMacroThread == null) {
				mRunningMacro = macro;
				mFrontFrame = frontFrame;

				for (DEFrame frame:frontFrame.getApplication().getFrameList())
					frame.getMainFrame().getMainPane().getMacroProgressPanel().initializeThreadMustDie();
		
				mMacroThread = new Thread(this, "DataWarriorMacro");
				mMacroThread.setPriority(Thread.MIN_PRIORITY);
				mMacroThread.start();
				}
			}
		}

	private int findLabel(DEMacro macro, String name) {
		for (int i = 0; i < macro.getTaskCount(); i++) {
			DEMacro.Task task = macro.getTask(i);
			if (mTaskFactory.getTaskCodeFromName(DETaskDefineLabel.TASK_NAME).equals(task.getCode())
			 && task.getConfiguration().getProperty(DETaskDefineLabel.PROPERTY_LABEL, "").equalsIgnoreCase(name))
				return i;
			}
		return -1;
		}


	@Override
	public void run() {
		try {
			mMessageMode = DEFAULT_MESSAGE_MODE;
			mVariableMap = new ConcurrentHashMap<>();
			for (int currentTask=0; currentTask<mRunningMacro.getTaskCount(); currentTask++) {
				if (threadMustDie())
					break;

				// show position in macro as long as individual tasks don't display their own status
				startProgress("Running Macro...", 0, mRunningMacro.getTaskCount());
				updateProgress(currentTask);

				AbstractTask cf = mTaskFactory.createTaskFromCode(mFrontFrame, mRunningMacro.getTaskCode(currentTask));
				if (cf != null) {
					if (cf instanceof DETaskGotoLabel) {
						Properties config = mRunningMacro.getTaskConfiguration(currentTask);
						String label = resolveVariables(config.getProperty(DETaskDefineLabel.PROPERTY_LABEL));
						int index = findLabel(mRunningMacro, label);
						if (index != -1)
							currentTask = index;
						continue;	// If label is found, then continue with task after the label. Otherwise just continue.
						}

		    		if (cf instanceof DETaskRepeatNextTask && currentTask<mRunningMacro.getTaskCount()-1) {
		    			Properties config = mRunningMacro.getTaskConfiguration(currentTask);
					    int lastTask = ((DETaskRepeatNextTask)cf).repeatAllTasks(config) ? mRunningMacro.getTaskCount()-1 : currentTask+1;
						String directory = ((DETaskRepeatNextTask)cf).getDirectory(config);
						if (DETaskRepeatNextTask.CANCELLED_DIR.equals(directory)) {
							currentTask = lastTask;
							}
						else {
							int filetypes = ((DETaskRepeatNextTask)cf).getFiletypes(config);
							int count = ((DETaskRepeatNextTask)cf).getRepetitions(config);
			                mRunningMacro.defineLoop(currentTask+1, lastTask, count, directory, filetypes, mVariableMap);
							}
					    continue;
	    				}
	
		    		// if the task is a macro itself, then spawn a daughter macro
		    		if (cf instanceof GenericTaskRunMacro) {
						DEMacro daughterMacro = ((GenericTaskRunMacro)cf).getMacro(mRunningMacro.getTaskConfiguration(currentTask));
						if (daughterMacro == null)
							continue;	// just skip the internal macro
	
						daughterMacro.setParentMacro(mRunningMacro, currentTask);
						mRunningMacro = daughterMacro;
						currentTask = -1;
						continue;	// start with the first task of the daughter macro
						}
	
					cf.execute(mRunningMacro.getTaskConfiguration(currentTask), this);
					if (cf.getNewFrontFrame() != null) {
						mFrontFrame = cf.getNewFrontFrame();
						try {
		                    SwingUtilities.invokeAndWait(() -> mFrontFrame.toFront() );
							}
	                    catch (Exception e) {}
						}
					}

				int gotoIndex = mRunningMacro.getLoopStart(currentTask);
				if (gotoIndex != -1)
					currentTask = gotoIndex-1;

				// if we have finished a daughter macro, then continue with the parent one
				if (currentTask == mRunningMacro.getTaskCount()-1) {
					DEMacro parentMacro = mRunningMacro.getParentMacro();
					if (parentMacro != null) {
						currentTask = mRunningMacro.getParentIndex();
						mRunningMacro.setParentMacro(null, 0);
						mRunningMacro = parentMacro;
						}
					}
				}
			}
		catch (final Exception e) {
			e.printStackTrace();
			SwingUtilities.invokeLater(() ->
				JOptionPane.showMessageDialog(null, e.toString(), "Unexpected Error", JOptionPane.ERROR_MESSAGE)
				);
			}

		stopProgress();

		mVariableMap = null;
		mMacroThread = null;
		mRunningMacro = null;
		}

	@Override
	public void startProgress(final String text, final int min, final int max) {
		if (SwingUtilities.isEventDispatchThread()) {
			for (DEFrame frame:mFrontFrame.getApplication().getFrameList())
				frame.getMainFrame().getMainPane().getMacroProgressPanel().startProgress(text, min, max);
			}
		else {
			SwingUtilities.invokeLater(() -> {
				for (DEFrame frame:mFrontFrame.getApplication().getFrameList())
					frame.getMainFrame().getMainPane().getMacroProgressPanel().startProgress(text, min, max);
				} );
			}
		}

	@Override
	public void updateProgress(final int value) {
		updateProgress(value, null);
		}

	@Override
	public void updateProgress(final int value, final String message) {
		if (SwingUtilities.isEventDispatchThread()) {
			for (DEFrame frame:mFrontFrame.getApplication().getFrameList())
				frame.getMainFrame().getMainPane().getMacroProgressPanel().updateProgress(value, message);
			}
		else {
			SwingUtilities.invokeLater(() -> {
				for (DEFrame frame:mFrontFrame.getApplication().getFrameList())
					frame.getMainFrame().getMainPane().getMacroProgressPanel().updateProgress(value, message);
				} );
			}
		}

	@Override
	public void stopProgress() {
		if (SwingUtilities.isEventDispatchThread()) {
			for (DEFrame frame:mFrontFrame.getApplication().getFrameList())
				frame.getMainFrame().getMainPane().getMacroProgressPanel().stopProgress();
			}
		else {
			SwingUtilities.invokeLater(() -> {
				for (DEFrame frame:mFrontFrame.getApplication().getFrameList())
					frame.getMainFrame().getMainPane().getMacroProgressPanel().stopProgress();
				} );
			}
		}

	@Override
	public void showErrorMessage(final String message) {
		if (mMessageMode == MESSAGE_MODE_SHOW_ERRORS
		 || mMessageMode == MESSAGE_MODE_SHOW_FIRST_ERROR) {

			if (mMessageMode == MESSAGE_MODE_SHOW_FIRST_ERROR)
				mMessageMode = MESSAGE_MODE_SKIP_ERRORS;

			if (SwingUtilities.isEventDispatchThread()) {
				mFrontFrame.getApplication().getActiveFrame().getMainFrame().getMainPane().getMacroProgressPanel().showErrorMessage(message);
				}
			else {
				SwingUtilities.invokeLater(() ->
					mFrontFrame.getApplication().getActiveFrame().getMainFrame().getMainPane().getMacroProgressPanel().showErrorMessage(message)
					);
				}
			}
		}

	@Override
	public boolean threadMustDie() {
		for (DEFrame frame:mFrontFrame.getApplication().getFrameList())
			if (frame.getMainFrame().getMainPane().getMacroProgressPanel().threadMustDie())
				return true;
		return false;
		}

	/**
	 * Programmatically tells the frame's progress panel to stop the execution.
	 */
	public void stopMacro() {
		mFrontFrame.getApplication().getActiveFrame().getMainFrame().getMainPane().getMacroProgressPanel().cancel();
		}
	}
