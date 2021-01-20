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

package com.actelion.research.datawarrior.task.chem.elib;

import com.actelion.research.gui.hidpi.HiDPIHelper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JPanel;

public class FitnessEvolutionPanel extends JPanel {
	private static final long serialVersionUID = 20140725L;

	private static float sBorder = HiDPIHelper.scale(2);

	private volatile float[] mAVGFitness;
	private volatile float[] mMaxFitness;
	private volatile int[] mCompounds;
	private volatile AtomicBoolean mLock;

	public FitnessEvolutionPanel() {
		super();
		mLock = new AtomicBoolean(false);
		}

	@Override
	public void paintComponent(Graphics g) {
		if (mLock.compareAndSet(false, true))	// skip painting if we are currently updating data
			return;

        super.paintComponent(g);

        if (mCompounds != null) {
			Dimension theSize = getSize();
			Insets insets = getInsets();
			theSize.width -= insets.left + insets.right + 2 * sBorder;
			theSize.height -= insets.top + insets.bottom + 2 * sBorder;

			if (theSize.width <= 0 || theSize.height <= 0)
				return;

			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			//        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

			int scaled4 = HiDPIHelper.scale(4);
			int scaled8 = HiDPIHelper.scale(8);
			int scaled12 = HiDPIHelper.scale(12);
			int scaled14 = HiDPIHelper.scale(14);
			int scaled22 = HiDPIHelper.scale(22);
			int scaled24 = HiDPIHelper.scale(24);

			g2.setFont(g2.getFont().deriveFont(0, scaled12));

			float x0 = insets.left + sBorder;
			float x1 = x0 + theSize.width - HiDPIHelper.scale(140);
			float y0 = insets.top + sBorder;
			float y1 = y0 + theSize.height;
			float usedHeight = theSize.height - HiDPIHelper.scale(32);    // three text lines a 14 pixel

			g2.drawString("Fitness Evolution", x0, y0 + scaled12);
			g2.drawString("generation average", x1, y0 + scaled12);
			g2.drawString("generation maximum", x1, y0 + scaled12 + scaled14);

			g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

			g2.setColor(Color.LIGHT_GRAY);
			g2.draw(new Line2D.Float(x0, y1 - usedHeight, x0 + theSize.width, y1 - usedHeight));
			g2.draw(new Line2D.Float(x0, y1, x0 + theSize.width, y1));

			g2.setColor(new Color(128, 0, 0));
			g2.draw(new Line2D.Float(x1 - scaled24, scaled8, x1 - scaled4, scaled8));
			drawCurve(g2, theSize, insets, mAVGFitness, usedHeight);

			g2.setColor(new Color(0, 128, 0));
			g2.draw(new Line2D.Float(x1 - scaled24, scaled22, x1 - scaled4, scaled22));
			drawCurve(g2, theSize, insets, mMaxFitness, usedHeight);
			}

		mLock.set(false);
		}

	private void drawCurve(Graphics2D g2, Dimension theSize, Insets insets, float[] fitness, float usedHeight) {
        float xOffset = (float)theSize.width / (mCompounds.length-2);

        float x1 = insets.left + sBorder;
        float y1 = insets.top + sBorder + theSize.height - fitness[0] * usedHeight;
        for (int i=1; i<mCompounds.length-1; i++) {
        	float x2 = x1 + xOffset;
            float y2 = insets.top + sBorder + theSize.height - fitness[i] * usedHeight;

            g2.draw(new Line2D.Float(x1, y1, x2, y2));

        	x1 = x2;
        	y1 = y2;
        	}
		}

	public void updateEvolution(int generations, TreeSet<EvolutionResult> resultSet) {
		if (mLock.get() || resultSet == null || generations < 2)
			return;

		if (mLock.compareAndSet(false, true))	// skip update if we are currently painting
			return;

		mAVGFitness = new float[generations+1];	// to include first parent generation (index = -1)
		mMaxFitness = new float[generations+1];
		mCompounds = new int[generations+1];

		for (EvolutionResult r:resultSet) {
			int generation = r.getGeneration()+1;	// first parent generation is actually -1
			mCompounds[generation]++;
			float fitness = r.getOverallFitness();
			mAVGFitness[generation] += fitness;
			if (mMaxFitness[generation] < fitness)
				mMaxFitness[generation] = fitness;
			}
		for (int i=0; i<generations; i++)
			mAVGFitness[i] /= mCompounds[i];

		repaint();
		mLock.set(false);
		}
	}
