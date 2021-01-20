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

public interface TaskConstantsMergeFile {
	public static final String PROPERTY_FILENAME = "file";
	public static final String PROPERTY_COLUMN_COUNT = "columnCount";
	public static final String PROPERTY_SOURCE_COLUMN = "sourceColumn";
	public static final String PROPERTY_DEST_COLUMN = "destColumn";
	public static final String PROPERTY_OPTION = "option";
	public static final String PROPERTY_APPEND_ROWS = "appendRows";
	public static final String PROPERTY_APPEND_COLUMNS = "appendColumns";

	public static final String DEST_COLUMN_ADD = "<add>";
	public static final String DEST_COLUMN_TRASH = "<trash>";

	// matching integer merge modes are defined in CompoundTableLoader
    public static final String[] OPTION_TEXT = { "as merge key", "as merge key (ignore case)", "as merge key (word search)", "append values", "keep existing", "replace with new", "replace if empty" };
    public static final String[] OPTION_CODE = { "key", "keyNoCase", "keySearch", "append", "keep", "replace", "useIfEmpty" };
	}
