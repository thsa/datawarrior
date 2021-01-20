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

package com.actelion.research.table.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

public interface CompoundTableExtensionHandler {

	/**
	 * Returns a unique identifying id for the extension name
	 * @param name
	 * @return -1 if not known or number not smaller than 0
	 */
	public int getID(String name);

	/**
	 * Returns the identifying name from an extension ID
	 * @param id
	 * @return null if not known or name of a supported type
	 */
	public String getName(int id);

	/**
	 * Appends CompoundTableModel extension data to the given writer.
	 * This includes the lines with start tag and end tags.
	 * @param name
	 * @param data
	 * @param writer
	 * @throws IOException 
	 */
	public void writeData(String name, Object data, BufferedWriter writer) throws IOException;

	/**
	 * Reads and parses the lines excluding the start tag, but including the end tag
	 * of the extension identified by name and returns the object.
	 * If the extension type is not supported, null is returned and the reader position
	 * advanced after the end tag.
	 * @param name
	 * @param reader
	 * @return null or valid extension object
	 */
	public Object readData(String name, BufferedReader reader);

	/**
	 * Checks, whether startTagLine is a valid start line of an extention file entry
	 * and extracts the identifying name.
	 * @param startTagLine
	 * @return null if invalid format or name not supported
	 */
	public String extractExtensionName(String startTagLine);
	}
