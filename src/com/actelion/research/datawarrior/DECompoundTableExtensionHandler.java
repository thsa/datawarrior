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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.task.DEMacro;
import com.actelion.research.table.model.CompoundTableExtensionHandler;

public class DECompoundTableExtensionHandler implements CompoundTableConstants,CompoundTableExtensionHandler {
	public static final int ID_FILE_EXPLANATION = 0;
	public static final int ID_MACRO = 1;
	private static final String[] SUPPORTED_NAME = { cExtensionNameFileExplanation, cExtensionNameMacroList };

	@Override
	public int getID(String name) {
		for (int i=0; i<SUPPORTED_NAME.length; i++)
			if (SUPPORTED_NAME[i].equals(name))
				return i;
		return -1;
		}

	@Override
	public String getName(int id) {
		return SUPPORTED_NAME[id];
		}

	@SuppressWarnings("unchecked")
	@Override
	public void writeData(String name, Object data, BufferedWriter writer) throws IOException {
		if (name.equals(cExtensionNameFileExplanation))
			writeExplanation(writer, (String)data);
		if (name.equals(cExtensionNameMacroList))
			writeMacroList(writer, (ArrayList<DEMacro>)data);
		}

	@Override
	public Object readData(String name, BufferedReader reader) {
		if (name.equals(cExtensionNameFileExplanation))
			return readExplanation(reader);
		if (name.equals(cExtensionNameMacroList))
			return readMacroList(reader);
		return null;
		}

	@Override
	public String extractExtensionName(String startTagLine) {
		for (String name:SUPPORTED_NAME)
			if (startTagLine.equals("<datawarrior "+name+">"))
				return name;

		return null;
		}

	private void writeExplanation(BufferedWriter writer, String explanation) throws IOException {
		writer.write(cFileExplanationStart);
		writer.newLine();
		for (String line:explanation.split("\\n")) {
			writer.write(line);
			writer.newLine();
			}
		writer.write(cFileExplanationEnd);
		writer.newLine();
		}

	private String readExplanation(BufferedReader reader) {
		StringBuilder explanation = new StringBuilder();
		try {
			while (true) {
				String theLine = reader.readLine();
				if (theLine == null
				 || theLine.equals(cFileExplanationEnd)) {
					break;
					}

				explanation.append(theLine + "\n");
				}

			if (explanation.length() != 0)
				return explanation.toString();
			}
		catch (Exception e) {}
		return null;
		}

	private void writeMacroList(BufferedWriter writer, ArrayList<DEMacro> macroList) throws IOException {
		writer.write(cMacroListStart);
		writer.newLine();

		for (DEMacro macro:macroList)
			macro.writeMacro(writer);

		writer.write(cMacroListEnd);
		writer.newLine();
		}

	private ArrayList<DEMacro> readMacroList(BufferedReader reader) {
		ArrayList<DEMacro> macroList = null;
		String theLine = null;
		try {
			while (true) {
				theLine = reader.readLine();
				if (theLine == null
				 || theLine.equals(cMacroListEnd)) {
					break;
					}

				String name = DEMacro.extractMacroName(theLine);
				if (name != null) {
					boolean autoStarts = DEMacro.isAutoStarting(theLine);
					DEMacro macro = new DEMacro(name, reader);
//					if (!macro.isEmpty()) {	// keep also empty macros TLS 31-Mar-14
						macro.setAutoStarting(autoStarts);
						if (macroList == null)
							macroList = new ArrayList<DEMacro>();
						macroList.add(macro);
//						}
					}
				}

			if (macroList != null)
				return macroList;
			}
		catch (Exception e) {
			e.printStackTrace();
			try {
				while (theLine != null && !theLine.equals(cMacroListEnd))
					theLine = reader.readLine();
				}
			catch (IOException ioe) {}
			}
		return null;
		}
	}
