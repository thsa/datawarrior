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

import com.actelion.research.util.Angle;

public class RadialNodeGroup {
	private double mAngleSpan,mForce,mMaxForce,mDFDA;
	private int mShellIndex,mNodeIndex;
	private int[] mSubgroupIndex;
	private RadialVisualizationNode[] mNode;
	private RadialNodeGroup mRightGroup;

	/**
	 * 
	 * @param shellIndex
	 * @param nodeCount
	 * @param subgroupCount no of sub-groups, i.e. node groups having same parent
	 * @param angleDif positive angle difference between adjacent nodes in radians
	 */
	public RadialNodeGroup(int shellIndex, int nodeCount, int subgroupCount, double angleDif) {
		mShellIndex = shellIndex;
		mNode = new RadialVisualizationNode[nodeCount];
		mSubgroupIndex = new int[subgroupCount];
		mAngleSpan = angleDif * (nodeCount - 1);
		}

	public void addNode(RadialVisualizationNode node) {
		mNode[mNodeIndex++] = node;
		node.setNodeGroup(this);
		}

	public void setRightNeighbour(RadialNodeGroup neighbour) {
		mRightGroup = neighbour;
		}

	public void findSubgroups() {
		Node parent = null;
		int subGroup = 0;
		for (int i=0; i<mNode.length; i++) {
			if (parent != mNode[i].getParentNode()) {
				parent = mNode[i].getParentNode();
				mSubgroupIndex[subGroup++] = i;
				}
			}
		
		}

	public void initForces() {
		mForce = 0;						// positive strains cause positive angle changes (clockwise)
		mMaxForce = 0.0;
		}

	public double addForces() {
//System.out.println("calculateForces() shell:"+mShellIndex);
		if (mRightGroup != null)	// we have neighbors
			addRightNeighborForce();

		if (mShellIndex != 1)
			addRadialForce();
//System.out.println("total: f:"+mForce);

		return mForce;
		}

	public void relax(double factor) {
		if (mForce != 0.0) {
			double v = factor * mForce / mDFDA;
//			double v = factor * Math.min(mForce, Math.PI / (6 * mShellIndex));
//System.out.println("relax() shell:"+mShellIndex+" v:"+v+" a1:"+mNode[0].getAngle().toString()+" a2:"+mNode[mNode.length-1].getAngle().toString());
			for (RadialVisualizationNode node:mNode)
				node.move(v);
			}
		}

	private void addRightNeighborForce() {
		double angle = Angle.difference(mRightGroup.mNode[0].getAngle(), mNode[mNode.length-1].getAngle());
		if (angle < -mRightGroup.mAngleSpan)	// only consider force to full overlap
			return;

		angle *= mShellIndex;	// normalize to situation on first shell

		if (angle < Math.PI / 2.0) {
			// create normalized value n such that n=0.0 -> angleDistance=PI/2 and n=1.0 -> angleDistance=PI/3
			double n = 3.0 - 6.0 * angle / Math.PI;
			double f = n * n;
			double dfda = Math.abs(36.0 / Math.PI * (2.0 / Math.PI * angle - 1.0));
			dfda *= mShellIndex;	// de-normalize to outer shell situation

//System.out.println("between neighbours: f:"+f+" dfda:"+dfda);
			addForce(-f, dfda);
			mRightGroup.addForce(f, dfda);
			}
		}

	private void addRadialForce() {
		for (int i=0; i<mSubgroupIndex.length; i++) {
			RadialVisualizationNode parent = mNode[mSubgroupIndex[i]].getParentNode();
			double a = Angle.difference(parent.getAngle(), getGroupAngle(i));
			if (a != 0.0) {
				double f = Math.signum(a) * a * a;
				double dfda = Math.abs(2.0 * a);
//System.out.println("radial: f:"+f+" dfda:"+dfda+" parent:"+parent.getAngle()+" group:"+getGroupAngle(i)+" dif:"+a);
				addForce(f, dfda);
				parent.getNodeGroup().addForce(-f, dfda);
				}
			}
		}

	private void addForce(double force, double dfda) {
		mForce += force;
		if (mMaxForce < Math.abs(force)) {
			mMaxForce = Math.abs(force);
			mDFDA = dfda;
			}
		}

	/**
	 * Calculates the middle angle those nodes of this group that have the same parent.
	 * @return
	 */
	private Angle getGroupAngle(int subGroup) {
		int subGroupNodes = ((subGroup == mSubgroupIndex.length-1) ? mNode.length : mSubgroupIndex[subGroup+1]) - mSubgroupIndex[subGroup];
		int index = mSubgroupIndex[subGroup] + (subGroupNodes - 1) / 2;
		Angle a1 = mNode[index].getAngle();
		return ((subGroupNodes & 1) == 1) ? a1 : new Angle(Angle.mean(a1, mNode[index+1].getAngle()));
		}
	}
