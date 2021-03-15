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

import com.actelion.research.table.model.CompoundListSelectionModel;
import com.actelion.research.table.model.CompoundTableModel;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public class VisualizationPanel2D extends VisualizationPanel {
    private static final long serialVersionUID = 0x20060904;

    private float mX1,mX2,mY1,mY2,mDX,mDY;
	private Rectangle mGraphBounds;
	private int mMouseX,mMouseY;

    public VisualizationPanel2D(Frame parent, CompoundTableModel tableModel,
							    CompoundListSelectionModel selectionModel) {
        super(parent, tableModel);
		mVisualization = new JVisualization2D(tableModel, selectionModel);
		mDimensions = 2;

		mVisualization.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				mGraphBounds = null;
				if (e.isControlDown()) {
					setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
					mX1 = getActingPruningBar(0).getLowValue();
					mX2 = getActingPruningBar(0).getHighValue();
					mY1 = getActingPruningBar(1).getLowValue();
					mY2 = getActingPruningBar(1).getHighValue();
					mDX = 0;
					mDY = 0;
					mMouseX = e.getX();
					mMouseY = e.getY();
					mGraphBounds = ((JVisualization2D) mVisualization).getGraphBounds(mMouseX, mMouseY, false);
					}
				}
			@Override
			public void mouseReleased(MouseEvent e) {
				if (mGraphBounds != null) {
					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					}
				}
			});
		mVisualization.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (mGraphBounds != null) {
					float dx = (mX2 - mX1) * (float)(mMouseX - e.getX()) / (float)mGraphBounds.width;
					float dy = (mY2 - mY1) * (float)(e.getY() - mMouseY) / (float)mGraphBounds.height;
					if (dx < 0f) {
						if (dx < -mX1)
							dx = -mX1;
						}
					else {
						if (dx > 1f-mX2)
							dx = 1f-mX2;
						}
					if (dy < 0f) {
						if (dy < -mY1)
							dy = -mY1;
						}
					else {
						if (dy > 1f-mY2)
							dy = 1f-mY2;
						}
					if (mDX != dx || mDY != dy) {
						mDX = dx;
						mDY = dy;
						float[] low = new float[2];
						float[] high = new float[2];
						low[0] = mX1 + dx;
						low[1] = mY1 + dy;
						high[0] = mX2 + dx;
						high[1] = mY2 + dy;
						setZoom(low, high, true, false);
						}
					}
				}
			});

		initialize();
		}

	@Override
    public void zoom(int sx, int sy, int steps) {
    	final float MIN_ZOOM = 0.0001f;
    	Rectangle bounds = ((JVisualization2D)mVisualization).getGraphBounds(sx, sy, false);
    	if (bounds != null && bounds.contains(sx, sy)) {
    		boolean zoom = false;
    		float[] low = new float[2];
    		float[] high = new float[2];
    		low[0] = (float)getActingPruningBar(0).getLowValue();
    		high[0] = (float)getActingPruningBar(0).getHighValue();
    		float f = (float)Math.exp(steps / 20.0);
    		if ((steps < 0 && high[0]-low[0] > MIN_ZOOM) || (steps > 0 && high[0]-low[0] < 1.0)) {
	    		float x = low[0] + (float)(sx - bounds.x) * (high[0] - low[0]) / bounds.width;
	    		low[0] = Math.max(0, x-f*(x-low[0]));
	    		high[0] = Math.min(1.0f, x+f*(high[0]-x));
	    		zoom = true;
    			}
    		low[1] = (float)getActingPruningBar(1).getLowValue();
    		high[1] = (float)getActingPruningBar(1).getHighValue();
    		if ((steps < 0 && high[1]-low[1] > MIN_ZOOM) || (steps > 0 && high[1]-low[1] < 1.0)) {
	    		float y = low[1] + (float)(bounds.y + bounds.height - sy) * (high[1] - low[1]) / bounds.height;
	    		low[1] = Math.max(0, y-f*(y-low[1]));
	    		high[1] = Math.min(1.0f, y+f*(high[1]-y));
	    		zoom = true;
    			}
    		if (zoom)
    			setZoom(low, high, false, false);
    		}
    	}
	}
