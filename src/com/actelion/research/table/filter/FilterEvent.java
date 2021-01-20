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

package com.actelion.research.table.filter;

public class FilterEvent {
	public static final int FILTER_UPDATED = 1;
	public static final int FILTER_CLOSED = 2;
	public static final int FILTER_ANIMATION_CHANGED = 3;
	public static final int FILTER_ANIMATION_STARTED = 4;
	public static final int FILTER_ANIMATION_STOPPED = 5;

	private JFilterPanel	mSource;
	private int				mType;
	private boolean			mIsAdjusting;

	public FilterEvent(JFilterPanel source, int type, boolean isAdjusting) {
		mSource = source;
		mType = type;
		mIsAdjusting = isAdjusting;
		}

	public JFilterPanel getSource() {
		return mSource;
		}

	public int getType() {
		return mType;
		}

	public boolean isAdjusting() {
		return mIsAdjusting;
		}
	}
