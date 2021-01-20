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

public class TreeGraphOptimizer {
	public static void optimizeCoordinates(Rectangle bounds, VisualizationNode[][] nodeList, boolean isVertical, boolean isInverted, int maxLayerDistance, int maxNeighborDistance) {
		if (nodeList.length == 1) {
			nodeList[0][0].setCoordinates(bounds.x + bounds.width/2, bounds.y + bounds.height/2);
			return;
			}

		for (int n=0; n<nodeList.length; n++)
			for (int i=0; i<nodeList[n].length; i++)
				((TreeVisualizationNode)nodeList[n][i]).setRelativePos(0);

		int levelCount = nodeList.length;
		int level = levelCount - 2;

		// size contains space requirements of all subtrees starting at a given level,
		// i.e. left and right extent of every level below and relative to the sub-tree root.
		float[][][] size = new float[nodeList[level].length][1][2];
		int childNodeIndex = 0;
		for (int n=0; n<nodeList[level].length; n++) {
			Node node = nodeList[level][n];
			int childCount = node.getChildCount();
			if (childCount == 0) {
				size[n][0][0] = Float.NaN;
				size[n][0][1] = Float.NaN;
				}
			else {
				float space = (node.getChildCount() - 1) / 2.0f;
				size[n][0][0] = -space;
				size[n][0][1] = space;
				for (int i=0; i<childCount; i++)
					((TreeVisualizationNode)nodeList[level+1][childNodeIndex++]).setRelativePos(-space+i);
				}
			}

		while (--level >= 0) {
			float[][][] newSize = new float[nodeList[level].length][size[0].length+1][2];
			childNodeIndex = 0;
			for (int n=0; n<nodeList[level].length; n++) {
				Node node = nodeList[level][n];
				int childCount = node.getChildCount();
				if (childCount == 0) {
					for (int i=0; i<newSize[0].length; i++) {
						newSize[n][i][0] = Float.NaN;
						newSize[n][i][1] = Float.NaN;
						}
					}
				else {
					newSize[n][0][0] = 0.0f;
					newSize[n][0][1] = 0.0f;
					for (int i=0; i<size[0].length; i++) {
						newSize[n][i+1][0] = size[childNodeIndex][i][0];
						newSize[n][i+1][1] = size[childNodeIndex][i][1];
						}
					if (childCount > 1) {
						float shift = 0.0f;
						for (int c=childNodeIndex+1; c<childNodeIndex+childCount; c++) {
	                        shift += 1.0f;   // minimum for every child
							for (int i=0; i<size[c].length; i++) {
								if (!Double.isNaN(newSize[n][i+1][1])
								 && !Double.isNaN(size[c][i][0])) {
								    float distance = 1.0f;	// here we could add distance contributions depending on collision distance to root
									float neededShift = distance + newSize[n][i+1][1] - size[c][i][0];
									if (shift < neededShift)
										shift = neededShift;
									}
								}
							for (int i=0; i<size[c].length; i++) {
								if (!Double.isNaN(size[c][i][0])) {
									if (Double.isNaN(newSize[n][i+1][1]))
										newSize[n][i+1][0] = size[c][i][0]+shift;

									newSize[n][i+1][1] = size[c][i][1]+shift;
									}
								}

							((TreeVisualizationNode)nodeList[level+1][c]).setRelativePos(((TreeVisualizationNode)nodeList[level+1][childNodeIndex]).getRelativePos() + shift);
							}
						newSize[n][0][1] = shift;

						// center the sub-tree's first level under the root node
						shift *= -0.5f;
						for (int i=0; i<newSize[n].length; i++) {
							newSize[n][i][0] += shift;
							newSize[n][i][1] += shift;
							}
						for (int c=childNodeIndex; c<childNodeIndex+childCount; c++)
							((TreeVisualizationNode)nodeList[level+1][c]).move(shift);
						}
					childNodeIndex += childCount;
					}
				}
			size = newSize;
			}

		float relMin = Float.MAX_VALUE;
		float relMax = Float.MIN_VALUE;
		for (int i=0; i<size[0].length; i++) {
			if (relMin > size[0][i][0])
			    relMin = size[0][i][0];
			if (relMax < size[0][i][1])
			    relMax = size[0][i][1];
			}
		float relMid = (relMin+relMax) / 2.0f;

		if (isVertical) {
			int yMid = bounds.y + bounds.height / 2;
			int levelWidth = Math.min(maxLayerDistance, bounds.width / (nodeList.length + 1));
			int x = bounds.x + (bounds.width - (nodeList.length - 1) * levelWidth) / 2;

			double factor = Math.min(maxNeighborDistance, (int)((float)bounds.height / (relMax-relMin+2.0f)));
			if (factor == 0.0)
				factor = Math.min(maxNeighborDistance, (float)bounds.height / (relMax-relMin+2.0f));
	
			for (int l=0; l<nodeList.length; l++) {
				for (int n=0; n<nodeList[l].length; n++) {
					TreeVisualizationNode node = (TreeVisualizationNode)nodeList[l][n];
					double dy = -relMid;
					while (node.getParentNode() != null) {
					    dy += node.getRelativePos();
						node = node.getParentNode();
						}

					int ll = isInverted ? nodeList.length-l-1 : l;
					nodeList[l][n].setCoordinates(x+ll*levelWidth, yMid+factor*dy);
					}
				}
			}
		else {
			int xMid = bounds.x + bounds.width / 2;
			int levelHeight = Math.min(maxLayerDistance, bounds.height / (nodeList.length + 1));
			int y = bounds.y + (bounds.height - (nodeList.length - 1) * levelHeight) / 2;
	
			double factor = Math.min(maxNeighborDistance, (int)((float)bounds.width / (relMax-relMin+2.0f)));
			if (factor == 0.0)
				factor = Math.min(maxNeighborDistance, (float)bounds.width / (relMax-relMin+2.0f));

			for (int l=0; l<nodeList.length; l++) {
				for (int n=0; n<nodeList[l].length; n++) {
					TreeVisualizationNode node = (TreeVisualizationNode)nodeList[l][n];
					double dx = -relMid;
					while (node.getParentNode() != null) {
					    dx += node.getRelativePos();
						node = node.getParentNode();
						}

					int ll = isInverted ? nodeList.length-l-1 : l;
					nodeList[l][n].setCoordinates(xMid+factor*dx, y+ll*levelHeight);
					}
				}
			}
		}
	}
