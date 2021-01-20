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

import java.awt.Rectangle;
import java.util.ArrayList;

public class RadialGraphOptimizer {
	private static final double RADIAL_SPACING = 0.5;

	public static void optimizeCoordinates(Rectangle bounds, VisualizationNode[][] nodeList, float preferredMarkerDistance) {
		int usedShellCount = nodeList.length;
		while (nodeList[usedShellCount-1].length == 0 && usedShellCount > 1)
			usedShellCount--;

		int centerX = bounds.x + bounds.width/2;
		int centerY = bounds.y + bounds.height/2;

		// calculate reasonable start angles and compile groups of rigidly connected visualization points.
		if (usedShellCount > 1) {
			int groupCount = nodeList[1].length;	// consider all members of first shell as individual groups
			ArrayList<RadialNodeGroup> groupList = new ArrayList<RadialNodeGroup>();
			for (int shell=1; shell<usedShellCount; shell++) {
				int spaceCount = (groupCount == 1) ? 0 : groupCount;
				double optAngleDif = 2.0 * Math.PI / (6.0 * shell);
				double maxAngleDif = 2.0 * Math.PI / (nodeList[shell].length + RADIAL_SPACING * spaceCount);
				boolean isRigidShell = (maxAngleDif <= optAngleDif);
				double angleDif = isRigidShell ? maxAngleDif : optAngleDif;
				double spaceDif = (2.0 * Math.PI - angleDif * nodeList[shell].length) / groupCount;
	
				RadialNodeGroup group = null;
				int firstInShell = groupList.size();
				if (isRigidShell) {
					group = new RadialNodeGroup(shell, nodeList[shell].length, groupCount, 0.0);
					groupList.add(group);
					}
	
				double angle = 0.0;
				RadialVisualizationNode parent = null;
				groupCount = 0;
				for (int i=0; i<nodeList[shell].length; i++) {
					RadialVisualizationNode node = (RadialVisualizationNode)nodeList[shell][i];
					if (node.getChildCount() != 0)
						groupCount++;

					// if node belongs to new group (on shell 1 we consider every node as independent group)
					if (shell == 1 || parent != node.getParentNode()) {
						if (shell == 1 || isRigidShell)
							angle += spaceDif;
						else
							angle = node.getParentNode().getAngle().getValue() - (angleDif * (node.getParentNode().getChildCount() - 1)) / 2.0;
	
						parent = node.getParentNode();
	
						if (!isRigidShell) {
							group = new RadialNodeGroup(shell, (shell == 1) ? 1 : parent.getChildCount(), 1, angleDif);
							groupList.add(group);
							}
						}
	
					node.setAngle(angle);
					angle += angleDif;
	
					group.addNode(node);
					}
	
				if (isRigidShell) {
					group.findSubgroups();
					}
				else if (groupList.size() > firstInShell+1) {	// if we have more than one group on the shell
					for (int i=firstInShell; i<groupList.size(); i++) {
						groupList.get(i).setRightNeighbour(groupList.get((i == groupList.size()-1) ? firstInShell : i+1));
						}
					}
				}

			// minimize strains between shell groups by moving them on the radial shells
			int fullLoops = 0;
			double v = 0.0;
			while (v < Math.PI/2.0) {
				for (RadialNodeGroup g:groupList)
					g.initForces();
				double maxForce = 0.0;
				for (RadialNodeGroup g:groupList)
					maxForce = Math.max(g.addForces(), maxForce);
				for (RadialNodeGroup g:groupList)
					g.relax(Math.cos(v));

				if (maxForce < 0.1 || fullLoops == 1000)
					v += 0.02;
				else
					fullLoops++;
				}
			}

		float[] radius = new float[usedShellCount];
		for (int shell=1; shell<usedShellCount; shell++)
			radius[shell] = Math.max(radius[shell-1] + preferredMarkerDistance,
					(float)nodeList[shell].length * preferredMarkerDistance / 6.28f);

		// calculate coordinates from angles
		float totalSize = Math.min(bounds.width, bounds.height);
		float usableSize = Math.max(totalSize-2*preferredMarkerDistance, 0.8f*totalSize);
		float zoom = Math.min(1f, 0.5f * usableSize / radius[usedShellCount-1]);

		for (int shell=0; shell<usedShellCount; shell++) {
			for (int i=0; i<nodeList[shell].length; i++) {
				RadialVisualizationNode node = (RadialVisualizationNode)nodeList[shell][i];
				node.setCoordinates(centerX + (int)(zoom * radius[shell] * node.getAngle().sin()),
									centerY - (int)(zoom * radius[shell] * node.getAngle().cos()));
				}
			}
		}
	}
