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

package com.actelion.research.datawarrior.task.jep;

import com.actelion.research.chem.descriptor.DescriptorEncoder;
import com.actelion.research.table.model.CompoundRecord;

/**
 * Passing a JEPParameter rather than a value object allows
 * a JEPFunction to access column properties if it knows the
 * ColumnTableModel. All values in columns with specialColumnType
 * are passed as JEPParameters.
 */
public class JEPParameter implements Comparable<JEPParameter> {
    public int column;
    public CompoundRecord record;

    /**
	 * Constructor
	 */
	public JEPParameter(CompoundRecord record, int column) {
	    this.record = record;
        this.column = column;
	    }

	@Override
	public int compareTo(JEPParameter p) {
		return this.toString().compareTo(p.toString());
		}

	@Override
	public String toString() {
	    Object data = record.getData(column);
	    if (data == null)
	        return "";
	    if (data instanceof byte[])
	        return new String((byte[])data);
        if (data instanceof int[])
            return new String(new DescriptorEncoder().encode((int[])data));
	    return data.toString();
	    }
    }
