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

import com.actelion.research.table.model.CompoundRecord;

public class VisualizationPoint3D extends VisualizationPoint {
	protected float[] coord = new float[3];
	protected float zoom;
	protected short screenZ;		// In stereo mode screenX still reflects the non stereo x-position
    protected short stereoOffset;   // Offset added to screenX for left eye; subtracted for right eye.
    								// In the left eye image markers behind monitor plane are left shifted,
    								// thus stereoOffset is negative for these markers.

	protected VisualizationPoint3D(CompoundRecord r) {
		super(r);
		}
	}
