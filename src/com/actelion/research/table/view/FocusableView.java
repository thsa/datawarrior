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

package com.actelion.research.table.view;

public interface FocusableView extends CompoundTableView {
	public static final int cFocusNone = -1;
    public static final int cFocusOnSelection = -2;

	public int getFocusList();

	/**
	 * Instructs the view to highlight all members of given hitlist,
	 * while dimming other records. The focus may also be put on all selected records.
	 * 
	 * @param no a valid hitlist number,cFocusOnSelection or cHitlistUnassigned
	 */
	public void setFocusList(int no);
	}
