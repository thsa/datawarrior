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

package com.actelion.research.table;

import java.util.EventObject;

public class RuntimePropertyEvent extends EventObject {
    private static final long serialVersionUID = 0x20100729;

    public static final int TYPE_ADD_VIEW = 1;
    public static final int TYPE_REMOVE_VIEW = 2;
    public static final int TYPE_RENAME_VIEW = 3;
    public static final int TYPE_SYNCHRONIZE_VIEW = 4;
    public static final int TYPE_ADD_FILTER = 5;
    public static final int TYPE_REMOVE_FILTER = 6;

    private int mType,mSubtype;

	public RuntimePropertyEvent(Object source, int type, int subtype) {
		super(source);
		mType = type;
		mSubtype = subtype;
    	}

	/**
	 * @return type of change
	 */
	public int getType() {
		return mType;
		}

	/**
	 * currently subtypes are not used
	 * @return
	 */
	public int getSubtype() {
		return mSubtype;
		}
	}
