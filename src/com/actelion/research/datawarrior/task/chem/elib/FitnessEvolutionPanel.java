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

import com.actelion.research.gui.LookAndFeelHelper;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.util.ColorHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class FitnessEvolutionPanel extends JPanel {
	private static final long serialVersionUID = 20140725L;

	private static int sMinDisplayedGenerations = 32;

	private ArrayList<ArrayList<Fitness>> mFinessList;
	private volatile AtomicBoolean mLock;

	public FitnessEvolutionPanel() {
		super();
		mFinessList = new ArrayList<>();
		mLock = new AtomicBoolean(false);
		}

	@Override
	public void paintComponent(Graphics g) {
		if (!mLock.compareAndSet(false, true))	// skip painting if we are currently updating data
			return;

        super.paintComponent(g);

        if (mFinessList.size() != 0) {
			Dimension theSize = getSize();
			Insets insets = getInsets();
	        int border = Math.round(HiDPIHelper.scale(2));
	        int textAreaHeight = Math.round(HiDPIHelper.scale(32));    // three text lines a 14 pixel
			Rectangle r = new Rectangle(border + insets.left, border + insets.top + textAreaHeight,
					theSize.width - insets.left - insets.right - 2*border,
					theSize.height - insets.top - insets.bottom - textAreaHeight - 2*border);

			if (r.width <= 0 || r.height <= 0)
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

			float xm = r.x + r.width/2;

			g2.drawString("Fitness Evolution", r.x, r.y - textAreaHeight + scaled12);
			g2.drawString("generation average", xm, r.y - textAreaHeight + scaled12);
			g2.drawString("generation maximum", xm, r.y - textAreaHeight + scaled12 + scaled14);

	        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
	        g2.setFont(g2.getFont().deriveFont(0, scaled8));
	        g2.setColor(Color.GRAY);
			drawHLine(g2, r, 0.75f);
	        drawHLine(g2, r, 0.5f);
	        drawHLine(g2, r, 0.25f);

	        int displayedGenerations = getDisplayedGenerations();

	        // from a user perspecive generations start with 1, thus G9 is perceived as G10
	        for (int generation=9; generation<displayedGenerations; generation+=10)
		        drawVLine(g2, r, generation, (generation+1) % 50 == 0, displayedGenerations);

	        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
	        g2.setColor(LookAndFeelHelper.isDarkLookAndFeel() ? Color.LIGHT_GRAY : Color.DARK_GRAY);
	        g2.draw(new Line2D.Float(r.x, r.y, r.x+r.width, r.y));
	        g2.draw(new Line2D.Float(r.x, r.y+r.height, r.x+r.width, r.y+r.height));

	        Color avgColor = new Color(224, 0, 0);
	        Color maxColor = new Color(0, 192, 0);
	        Color avgFadedColor = ColorHelper.intermediateColor(getBackground(), avgColor, 0.3f);
	        Color maxFadedColor = ColorHelper.intermediateColor(getBackground(), maxColor, 0.3f);

	        g2.setColor(avgColor);
	        g2.draw(new Line2D.Float(xm - scaled24, scaled8, xm - scaled4, scaled8));
	        g2.setColor(maxColor);
	        g2.draw(new Line2D.Float(xm - scaled24, scaled22, xm - scaled4, scaled22));

	        for (int i=0; i<mFinessList.size(); i++) {
	        	boolean isCurrentRun = (i == mFinessList.size()-1);
		        boolean isPreviousRun = (i == mFinessList.size()-2);

		        if (isCurrentRun || isPreviousRun) {
			        g2.setColor(isCurrentRun ? avgColor : avgFadedColor);
			        drawCurve(g2, r, mFinessList.get(i), displayedGenerations, false);
		            }

		        g2.setColor(isCurrentRun ? maxColor : isPreviousRun ? maxFadedColor : Color.GRAY);
		        drawCurve(g2, r, mFinessList.get(i), displayedGenerations, true);
		        }
			}

		mLock.set(false);
		}

	private int getDisplayedGenerations() {
		int displayedGenerations = sMinDisplayedGenerations;
		for (ArrayList<Fitness> fitnessList:mFinessList) {
			int neededGenerations = fitnessList.get(fitnessList.size()-1).generation + 1;
			if (displayedGenerations < neededGenerations)
				displayedGenerations = neededGenerations;
			}
		return displayedGenerations;
		}

	private void drawHLine(Graphics2D g2, Rectangle r, float value) {
		float y = r.y + r.height - value*value*r.height;
		g2.drawString(Float.toString(value), r.x, y-2);
		g2.draw(new Line2D.Float(r.x, y, r.x+r.width, y));
		}

	private void drawVLine(Graphics2D g2, Rectangle r, int generation, boolean isBold, int displayedGenerations) {
		g2.setColor(!isBold ? Color.GRAY : LookAndFeelHelper.isDarkLookAndFeel() ? Color.LIGHT_GRAY : Color.DARK_GRAY);
		float x = r.x + generation * r.width / (displayedGenerations-1);
		g2.draw(new Line2D.Float(x, r.y, x, r.y+r.height));
		}

	private void drawCurve(Graphics2D g2, Rectangle r, ArrayList<Fitness> fitnessList, int displayedGenerations, boolean isMax) {
        float dx = (float)r.width / (displayedGenerations-1);

        float lx = -1f;
        float ly = -1f;
        for (Fitness fitness:fitnessList) {
        	float f = isMax ? fitness.maximum : fitness.average;
        	float x = r.x + dx * fitness.generation;
            float y = r.y + r.height - f*f * r.height;

            if (lx != -1f)
	            g2.draw(new Line2D.Float(lx, ly, x, y));

        	lx = x;
        	ly = y;
        	}
		}

	public void updateEvolution(int run, int generation, ConcurrentSkipListSet<EvolutionResult> generationResults) {
		if (mLock.get() || generationResults.size() == 0 || generation < 1)
			return;

		if (!mLock.compareAndSet(false, true))	// skip update if we are currently painting
			return;

		if (run == mFinessList.size()) {
			ArrayList<Fitness>fitnesses = new ArrayList<>();
			mFinessList.add(fitnesses);
			}

		mFinessList.get(run).add(new Fitness(generation, generationResults));

		repaint();
		mLock.set(false);
		}

	private class Fitness {
		float average,maximum;
		int generation;

		public Fitness(int generation, ConcurrentSkipListSet<EvolutionResult> generationResults) {
			for (EvolutionResult r:generationResults) {
				maximum = Math.max(maximum, r.getOverallFitness());
				average += r.getOverallFitness();
				}
			average /= generationResults.size();
			this.generation = generation;
			}
		}
	}
