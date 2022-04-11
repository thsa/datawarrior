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

public interface MarkerLabelConstants {
	public static final int	cTopLeft = 0;
	public static final int	cTopCenter = 1;
	public static final int	cTopRight = 2;
	public static final int	cMidLeft = 3;
	public static final int	cMidCenter = 4;
	public static final int	cMidRight = 5;
	public static final int	cBottomLeft = 6;
	public static final int	cBottomCenter = 7;
	public static final int	cBottomRight = 8;
	public static final int	cInTableLine1 = 9;
	public static final int	cInTableLine2 = 10;
	public static final int	cInTableLine3 = 11;
	public static final int	cInTableLine4 = 12;
	public static final int	cInTableLine5 = 13;
	public static final int	cInTableLine6 = 14;
	public static final int	cInTableLine7 = 15;
	public static final int	cInTableLine8 = 16;

	public static final int	cFirstTopPosition = 0;
	public static final int	cFirstMidPosition = 3;
	public static final int	cFirstBottomPosition = 6;
	public static final int	cFirstTablePosition = 9;

	public static int[] cCornerPositions = { cTopLeft, cTopRight, cBottomLeft, cBottomRight };
	public static int[] cEdgePositions = { cTopCenter, cMidLeft, cMidRight, cBottomCenter };

	public static final String[] cOnePerCategoryMode = { "highest", "average", "lowest", "magic" };
	public static final int	cOPCModeHighest = 0;
	public static final int	cOPCModeAverage = 1;
	public static final int	cOPCModeLowest = 2;
	public static final int	cOPCModeMagic = 3;

	public static final String[] cPositionOption = { "top left", "top center", "top right",
													 "mid left", "mid center (instead of marker)", "mid right",
													 "bottom left", "bottom center", "bottom right",
													 "table line 1", "table line 2",
													 "table line 3", "table line 4",
													 "table line 5", "table line 6",
													 "table line 7", "table line 8" };
	public static final String[] cPositionCode = { "topLeft", "topCenter", "topRight",
												   "midLeft", "midCenter", "midRight",
												   "bottomLeft", "bottomCenter", "bottomRight",
												   "below1", "below2", "below3", "below4",
												   "below5", "below6", "below7", "below8" };
	}
