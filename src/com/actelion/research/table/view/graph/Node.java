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

public abstract class Node {
	private Node mParentNode;
	private int mChildCount;
	private float mStrength;

	public Node(Node parent, float strength) {
		mParentNode = parent;
		if (parent != null)
			mParentNode.mChildCount++;
		mStrength = strength;
		}

	public abstract void setCoordinates(double x, double y);
	public abstract double getX();
	public abstract double getY();

	public Node getParentNode() {
		return mParentNode;
		}

	public void setParentNode(Node parent) {
		if (parent != mParentNode) {
			if (mParentNode != null)
				mParentNode.mChildCount--;

			mParentNode = parent;
			mParentNode.mChildCount++;
			}
		}

	public int getChildCount() {
		return mChildCount;
		}

	public float getStrength() {
		return mStrength;
		}

    public void setStrength(float s) {
        mStrength = s;
        }
    }
