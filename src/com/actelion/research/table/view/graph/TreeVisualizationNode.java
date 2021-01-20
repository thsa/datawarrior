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

public class TreeVisualizationNode extends VisualizationNode {
	private float mPos;

	public TreeVisualizationNode(VisualizationPoint vp, TreeVisualizationNode parent, float strength) {
		super(vp, parent, strength);
		}

	public TreeVisualizationNode getParentNode() {
		return (TreeVisualizationNode)super.getParentNode();
		}

	/**
	 * @return relative x (horizontal tree) or y (vertical tree) position
	 */
	public float getRelativePos() {
		return mPos;
		}

	/**
	 * @param pos relative x (horizontal tree) or y (vertical tree) position
	 */
	public void setRelativePos(float pos) {
		mPos = pos;
		}

	/**
	 * @param d change of relative x (horizontal tree) or y (vertical tree) position
	 */
	public void move(float d) {
		mPos += d;
		}
	}
