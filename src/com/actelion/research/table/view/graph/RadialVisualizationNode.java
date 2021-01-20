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

package com.actelion.research.table.view.graph;

import com.actelion.research.table.view.VisualizationPoint;
import com.actelion.research.util.Angle;

public class RadialVisualizationNode extends VisualizationNode {
	private Angle mAngle;
	private RadialNodeGroup mNodeGroup;

	public RadialVisualizationNode(VisualizationPoint vp, RadialVisualizationNode parent, float strength) {
		super(vp, parent, strength);
		mAngle = new Angle();
		}

	public RadialVisualizationNode getParentNode() {
		return (RadialVisualizationNode)super.getParentNode();
		}

	public RadialNodeGroup getNodeGroup() {
		return mNodeGroup;
		}

	public void setNodeGroup(RadialNodeGroup group) {
		mNodeGroup = group;
		}

	public Angle getAngle() {
		return mAngle;
		}

	public void setAngle(double value) {
		mAngle.setValue(value);
		}

	public void setAngle(Angle a) {
		mAngle = a;
		}

	public void move(double d) {
		mAngle.add(d);
		}
	}
