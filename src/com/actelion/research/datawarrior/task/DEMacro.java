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

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.task.chem.DETaskClusterCompounds;
import com.actelion.research.datawarrior.task.chem.elib.DETaskBuildEvolutionaryLibrary;
import com.actelion.research.datawarrior.task.data.*;
import com.actelion.research.datawarrior.task.data.fuzzy.DETaskCalculateFuzzyScore;
import com.actelion.research.datawarrior.task.db.DETaskBuildingBlockQuery;
import com.actelion.research.datawarrior.task.table.DETaskCopyTableCells;
import com.actelion.research.datawarrior.task.table.DETaskJumpToReferenceRow;
import com.actelion.research.datawarrior.task.view.*;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.io.BOMSkipper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class DEMacro implements CompoundTableConstants {
	public static final String MACRO_START = "<macro name=\"";
	private static final String MACRO_END = "</macro";
	private static final String DESCRIPTION_START = "<description";
	private static final String DESCRIPTION_END = "</description";
	private static final String TASK_START = "<task name=\"";
	private static final String TASK_END = "</task";
	private static final String AUTOSTART = " auto-start=\"true\"";
	private String			mName,mDescription;
	private ArrayList<Task> mTaskList;
	private ArrayList<DEMacroListener> mListenerList;
	private ArrayList<Loop> mLoopList;
	private DEMacro			mParentMacro;
	private int				mParentIndex;
	private boolean			mIsAutoStarting;

	public static String extractMacroName(String headerLine) {
		if (!headerLine.startsWith(MACRO_START))
			return null;

		int index = headerLine.indexOf('"', MACRO_START.length());
		if (index == -1)
			return null;

		return headerLine.substring(MACRO_START.length(), index);
		}

	public static boolean isAutoStarting(String headerLine) {
		return headerLine.startsWith(MACRO_START) && headerLine.indexOf(AUTOSTART) != -1;
		}

	/**
	 * Creates an empty DEMacro with the given name.
	 * If macroList is given than the name is may be slightly adapted to make it unique.
	 * @param name
	 * @param macroList
	 */
	public DEMacro(String name, ArrayList<DEMacro> macroList) {
		mTaskList = new ArrayList<Task>();
		mListenerList = new ArrayList<DEMacroListener>();
		mName = getUniqueName(name, macroList);
		}

	/**
	 * Creates a new DEMacro as exact copy of the specified sourceMacro.
	 * If macroList is given than the name is may be slightly adapted to make it unique.
	 * @param name
	 * @param macroList
	 * @param sourceMacro macro to be cloned
	 */
	public DEMacro(String name, ArrayList<DEMacro> macroList, DEMacro sourceMacro) {
		mTaskList = new ArrayList<>();
		mListenerList = new ArrayList<>();
		mName = getUniqueName(name, macroList);
		mIsAutoStarting = sourceMacro != null && sourceMacro.isAutoStarting();
		if (sourceMacro != null) {
			for (int i=0; i<sourceMacro.getTaskCount(); i++) {
				Task sourceTask = sourceMacro.getTask(i);
				mTaskList.add(new Task(sourceTask.getCode(), new Properties(sourceTask.getConfiguration())));
				}
			}
		}

	/**
	 * Creates a new DEMacro from a datawarrior macro file (.dwam).
	 * @param file
	 * @throws IOException
	 */
	public DEMacro(File file, ArrayList<DEMacro> macroList) throws IOException {
		this(new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8")), macroList);
		}

	/**
	 * Creates a new DEMacro from a text representation of a macro.
	 * @param reader
	 * @throws IOException
	 */
	public DEMacro(BufferedReader reader, ArrayList<DEMacro> macroList) throws IOException {
		mTaskList = new ArrayList<>();
		mListenerList = new ArrayList<>();
		BOMSkipper.skip(reader);
		String headerLine = reader.readLine();
		mName = extractMacroName(headerLine);
		if (mName != null) {
			mName = getUniqueName(mName, macroList);
			mIsAutoStarting = isAutoStarting(headerLine);
			readMacro(reader);
			}
		reader.close();
		}

	public DEMacro(String name, BufferedReader reader) {
		mTaskList = new ArrayList<>();
		mListenerList = new ArrayList<>();
		mName = name;
		try {
			readMacro(reader);
			}
		catch (IOException ioe) {}
		}

	private String getUniqueName(String name, ArrayList<DEMacro> macroList) {
		if (macroList == null)
			return name;

		while (true) {
			boolean nameIsUnique = true;
			for (DEMacro macro:macroList) {
				if (name.equals(macro.getName())) {
					nameIsUnique = false;
					break;
					}
				}

			if (nameIsUnique)
				return name;

			int index = name.lastIndexOf(' ');
			try {
				int number = Integer.parseInt(name.substring(index+1));
				name = name.substring(0, index+1) + (number+1);
				}
			catch (NumberFormatException nfe) {
				name = name + " 2";
				}
			}
		}

	public void addMacroListener(DEMacroListener l) {
		mListenerList.add(l);
		}

	public void removeMacroListener(DEMacroListener l) {
		mListenerList.remove(l);
		}

	private void fireContentChanged() {
		for (DEMacroListener l:mListenerList)
			l.macroContentChanged(this);
		}

	private void fireNameChanged() {
		for (DEMacroListener l:mListenerList)
			l.macroNameChanged(this);
		}

	public String getDescription() {
		return mDescription;
		}

	public String getName() {
		return mName;
		}

	public void setName(String name, ArrayList<DEMacro> macroList) {
		if (!mName.equals(name)) {
			mName = getUniqueName(name, macroList);
			fireNameChanged();
			}
		}

	public boolean isEmpty() {
		return mTaskList.size() == 0;
		}

	public void clear() {
		mTaskList.clear();
		fireContentChanged();
		}

	public void addTask(String taskCode, Properties configuration) {
		mTaskList.add(new Task(taskCode, configuration));
		fireContentChanged();
		}

	public void duplicateTask(int index) {
		Task task = mTaskList.get(index);
		Properties oldConfiguration = task.getConfiguration();
		Properties newConfiguration = new Properties();
		for (String key:oldConfiguration.stringPropertyNames())
			newConfiguration.put(key, oldConfiguration.getProperty(key));
		mTaskList.add(index+1, new Task(task.getCode(), newConfiguration));
		fireContentChanged();
		}

	public void removeTask(int index) {
		mTaskList.remove(index);
		fireContentChanged();
		}

	public Task getTask(int index) {
		return mTaskList.get(index);
		}

	public int getTaskCount() {
		return mTaskList.size();
		}

	public String getTaskCode(int index) {
		return mTaskList.get(index).getCode();
		}

	public Properties getTaskConfiguration(int index) {
		return mTaskList.get(index).getConfiguration();
		}

	public void setTaskConfiguration(int index, Properties configuration) {
		mTaskList.get(index).setConfiguration(configuration);
		}

	/**
	 * Updates the order of old task according to index list.
	 * May introduce a new task, if the associated old index is -1.
	 * @param oldIndex
	 * @return
	 */
	public ArrayList<Task> changeTaskOrder(int[] oldIndex) {
		ArrayList<Task> oldTaskList = mTaskList;
		mTaskList = new ArrayList<Task>();
		for (int i:oldIndex) {
			if (i == -1)
				mTaskList.add(new Task());
			else
				mTaskList.add(oldTaskList.get(i));
			}
		return mTaskList;
		}

	public void writeMacro(BufferedWriter writer) throws IOException {
		writer.write(MACRO_START+mName+"\"");
		if (mIsAutoStarting)
			writer.write(AUTOSTART);
		writer.write(">");
		writer.newLine();
		for (Task task:mTaskList) {
			writer.write(TASK_START+task.getCode()+"\">");
			writer.newLine();
			if (task.configuration != null) {
				for (String key:task.configuration.stringPropertyNames()) {
					writer.write(key+"="+encode(task.configuration.getProperty(key)));
					writer.newLine();
					}
				}
			writer.write(TASK_END+">");
			writer.newLine();
			}
		writer.write(MACRO_END+">");
		writer.newLine();
		}

	private String decode(String s) {
		return s.replace(NEWLINE_STRING, "\n");
		}

	private String encode(String s) {
		return s.replaceAll(NEWLINE_REGEX, NEWLINE_STRING);
		}

	public boolean isAutoStarting() {
		return mIsAutoStarting;
		}

	public void setAutoStarting(boolean b) {
		mIsAutoStarting = b;
		}

	public void readMacro(BufferedReader reader) throws IOException {
		mTaskList.clear();
		String theLine = reader.readLine();
		if (theLine != null && theLine.startsWith(DESCRIPTION_START)) {
			StringBuilder descBuilder = new StringBuilder();
			theLine = reader.readLine();
			while (theLine != null && !theLine.startsWith(DESCRIPTION_END)) {
				descBuilder.append(theLine);
				descBuilder.append("\n");
				theLine = reader.readLine();
				}
			mDescription = descBuilder.toString();
			if (theLine != null)
				theLine = reader.readLine();
			}
		while (theLine != null && theLine.startsWith(TASK_START)) {
			String taskCode = theLine.substring(12, theLine.indexOf('\"', 12));

			Properties configuration = new Properties();
			theLine = reader.readLine();
			while (theLine != null && !theLine.startsWith(TASK_END)) {
				int index = theLine.indexOf("=");
				if (index != -1)
					configuration.setProperty(theLine.substring(0, index), decode(theLine.substring(index+1)));
				theLine = reader.readLine();
				}

			taskCode = updateLegacyTasks(taskCode, configuration);

			mTaskList.add(new Task(taskCode, configuration));
			theLine = reader.readLine();
			if (theLine == null || theLine.startsWith(MACRO_END))
				break;
			}
		fireContentChanged();
		}

	/**
	 * Tasks may change their names or codes for some reason over time, e.g. if a task is split into multiple tasks,
	 * or if a task is renamed to improve consistency. In these cases the task codes change accordingly.
	 * To keep old macros compatible, this method translates old task codes into the current ones.
	 * @param taskCode
	 * @param configuration
	 * @return
	 */
	private String updateLegacyTasks(String taskCode, Properties configuration) {
		if (taskCode.equals("newView")) {
			String type = configuration.getProperty("type");
			if ("structure".equals(type)) {
				configuration.remove("type");
				return StandardTaskFactory.constructTaskCodeFromName(DETaskNewStructureView.TASK_NAME);
				}
			if ("form".equals(type)) {
				configuration.remove("type");
				return StandardTaskFactory.constructTaskCodeFromName(DETaskNewFormView.TASK_NAME);
				}
			if ("2D".equals(type)) {
				configuration.remove("type");
				return StandardTaskFactory.constructTaskCodeFromName(DETaskNew2DView.TASK_NAME);
				}
			if ("3D".equals(type)) {
				configuration.remove("type");
				return StandardTaskFactory.constructTaskCodeFromName(DETaskNew3DView.TASK_NAME);
				}
			if ("text".equals(type)) {
				configuration.remove("type");
				return StandardTaskFactory.constructTaskCodeFromName(DETaskNewTextView.TASK_NAME);
				}
			if ("macro".equals(type)) {
				configuration.remove("type");
				return StandardTaskFactory.constructTaskCodeFromName(DETaskNewMacroEditor.TASK_NAME);
				}
			}
		else if (taskCode.equals("addFuzzyScore"))
			return StandardTaskFactory.constructTaskCodeFromName(DETaskCalculateFuzzyScore.TASK_NAME);
		else if (taskCode.equals("copy"))
			return StandardTaskFactory.constructTaskCodeFromName(DETaskCopyTableCells.TASK_NAME);
		else if (taskCode.equals("createBinsFromNumbers"))
			return StandardTaskFactory.constructTaskCodeFromName(DETaskAddBinsFromNumbers.TASK_NAME);
		else if (taskCode.equals("createEvolutionaryLibrary"))
			return StandardTaskFactory.constructTaskCodeFromName(DETaskBuildEvolutionaryLibrary.TASK_NAME);
		else if (taskCode.equals("clusterSimilarCompounds"))
			return StandardTaskFactory.constructTaskCodeFromName(DETaskClusterCompounds.TASK_NAME);
		else if (taskCode.equals("calculateNewColumn"))
			return StandardTaskFactory.constructTaskCodeFromName(DETaskAddCalculatedValues.TASK_NAME);
		else if (taskCode.equals("deleteRedundantRows"))
			return StandardTaskFactory.constructTaskCodeFromName(DETaskDeleteDuplicateRows.TASK_NAME[DETaskDeleteDuplicateRows.MODE_REMOVE_DUPLICATE]);
		else if (taskCode.equals("jumpToCurrentRow"))
			return StandardTaskFactory.constructTaskCodeFromName(DETaskJumpToReferenceRow.TASK_NAME);
		else if (taskCode.equals("searchAndReplace"))
			return StandardTaskFactory.constructTaskCodeFromName(DETaskFindAndReplace.TASK_NAME);
		else if (taskCode.equals("setCurrentRow"))
			return StandardTaskFactory.constructTaskCodeFromName(DETaskSetReferenceRow.TASK_NAME);
		else if (taskCode.equals("showLabels"))
			return StandardTaskFactory.constructTaskCodeFromName(DETaskSetMarkerLabels.TASK_NAME);
		else if (taskCode.equals("searchEnamineBuildingBlocks"))
			return StandardTaskFactory.constructTaskCodeFromName(DETaskBuildingBlockQuery.TASK_NAME);

		return taskCode;
		}

	public DEMacro getParentMacro() {
		return mParentMacro;
		}

	public int getParentIndex() {
		return mParentIndex;
		}

	public void setParentMacro(DEMacro parentMacro, int parentIndex) {
		mParentMacro = parentMacro;
		mParentIndex = parentIndex;
		}

	/**
	 * Defines a sequence of tasks to be repeated for a couple of times.
	 * @param firstTask
	 * @param lastTask
	 * @param count how many times the task sequence should be run
	 */
	public void defineLoop(int firstTask, int lastTask, int count, String dir, int filetypes, ConcurrentHashMap variableMap) {
		if (mLoopList == null)
			mLoopList = new ArrayList<>();
		mLoopList.add(new Loop(firstTask, lastTask, count-1, dir, filetypes, variableMap));
		}

	/**
	 * If currentTask refers to the last task of an active loop, then the index
	 * of the first task of that loop is returned and the loop counter decremented.
	 * @param currentTask
	 * @return the index of the loop's first task or -1, if not at the end of a loop
	 */
	public int getLoopStart(int currentTask) {
		if (mLoopList != null && mLoopList.size() != 0) {
			int loop = mLoopList.size()-1;
			Loop currentLoop = mLoopList.get(loop);
			if (currentTask == currentLoop.lastTask) {
				currentLoop.increment();
				int firstTask = currentLoop.firstTask;
				currentLoop.count--;
				if (currentLoop.count <= 1)
					mLoopList.remove(loop);
				return currentLoop.count == 0 ? -1 : firstTask;
				}
			}
		return -1;
		}

	public class Task {
		private String code;
		private Properties configuration;

		public Task() {
			this(null, null);
			}

		public Task(String code, Properties configuration) {
			this.code = code;
			this.configuration = configuration;
			}

		public String getCode() {
			return code;
			}

		public Properties getConfiguration() {
			return configuration;
			}

		public void setCode(String code) {
			this.code = code;
			}

		public void setConfiguration(Properties configuration) {
			this.configuration = configuration;
			}
		}

	private class Loop {
		private int firstTask,lastTask,count,index;
		private ArrayList<File> filelist;
		private ConcurrentHashMap variableMap;

		public Loop(int firstTask, int lastTask, int count, String dirname, int filetypes, ConcurrentHashMap variableMap) {
			this.firstTask = firstTask;
			this.lastTask = lastTask;
			this.variableMap = variableMap;
			this.index = 0;
			if (dirname != null) {
				this.filelist = FileHelper.getCompatibleFileList(new File(dirname), filetypes);
				this.count = filelist.size();
				}
			else {
				this.count = count;
				}
			increment();
			}

		public void increment() {
			variableMap.put("LOOPINDEX", Integer.toString(++index));
			if (filelist != null)
				updateFileVariable();
			}

		private void updateFileVariable() {
			if (filelist.size() > 0) {
				File file = filelist.remove(0);
				variableMap.put("FILENAME", file.getAbsolutePath());
				}
			else {
				variableMap.remove("FILENAME");
				}
			}
		}
	}
