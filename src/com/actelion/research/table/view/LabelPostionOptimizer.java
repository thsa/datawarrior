package com.actelion.research.table.view;

import java.awt.*;
import java.util.Arrays;

public class LabelPostionOptimizer {
	private static final int MAX_LABELS = 1024;
	private static final int MAX_STEPS = 20;

	public void optimize(Rectangle[] graphRect, LabelPosition2D[][] labelPosition, int border) {
		for (int i=0; i<graphRect.length; i++)
			optimize(graphRect[i], labelPosition[i], border);
		}

	private void optimize(Rectangle graphRect, LabelPosition2D[] lp, int border) {
		int labelCount = lp.length;
		if (labelCount > MAX_LABELS)
			return;

		int[] forceX = new int[labelCount];
		int[] forceY = new int[labelCount];

		int previousTotalForce = Integer.MAX_VALUE;
		float factor = 0.5f;

		for (int s=0; s<MAX_STEPS; s++) {
			int totalForce = addLabelCollision(lp, forceX, forceY, border);
			if (totalForce == 0 || totalForce >= previousTotalForce)
				break;

			int minX = graphRect.x;
			int minY = graphRect.y;
			int maxX = graphRect.x + graphRect.width;
			int maxY = graphRect.y + graphRect.height;

			for (int i=0; i<labelCount; i++) {
				int dx = Math.round(factor * forceX[i]);
				if (dx < minX - lp[i].getScreenX1())
					dx = minX - lp[i].getScreenX1();
				if (dx > maxX - lp[i].getScreenX2())
					dx = maxX - lp[i].getScreenX2();
				if (dx != 0) {
					int maxDX = (int)(factor * lp[i].getScreenWidth());
					lp[i].translateX(dx < 0 ? Math.max(-maxDX, dx) : Math.min(maxDX, dx));
					}
				int dy = Math.round(factor * forceY[i]);
				if (dy < minY - lp[i].getScreenY1())
					dy = minY - lp[i].getScreenY1();
				if (dy > maxY - lp[i].getScreenY2())
					dy = maxY - lp[i].getScreenY2();
				if (dy != 0) {
					int maxDY = (int)(factor * lp[i].getScreenHeight());
					lp[i].translateY(dy < 0 ? Math.max(-maxDY, dy) : Math.min(maxDY, dy));
					}
				}

			factor *= 0.8f;
			previousTotalForce = totalForce;
			Arrays.fill(forceX, 0);
			Arrays.fill(forceY, 0);
			}
		}

	private int addLabelCollision(LabelPosition2D[] lp, int[] forceX, int[] forceY, int border) {
		int labelCount = forceX.length;
		int forceSum = 0;
		for (int i=1; i<labelCount; i++) {
			LabelPosition2D lp1 = lp[i];
			for (int j=0; j<i; j++) {
				LabelPosition2D lp2 = lp[j];
				if ((lp2.getScreenX1() - lp1.getScreenX2() < border)
				 && (lp1.getScreenX1() - lp2.getScreenX2() < border)
				 && (lp2.getScreenY1() - lp1.getScreenY2() < border)
				 && (lp1.getScreenY1() - lp2.getScreenY2() < border)) {
					int dx = lp1.getScreenX1() + lp1.getScreenX2() < lp2.getScreenX1() + lp2.getScreenX2() ?
							 lp1.getScreenX2() - lp2.getScreenX1() + border
						   : lp1.getScreenX1() - lp2.getScreenX2() - border;
					int dy = lp1.getScreenY1() + lp1.getScreenY2() < lp2.getScreenY1() + lp2.getScreenY2() ?
							 lp1.getScreenY2() - lp2.getScreenY1() + border
						   : lp1.getScreenY1() - lp2.getScreenY2() - border;
					if (Math.abs(dx) < Math.abs(dy)) {
						forceX[i] -= dx;
						forceX[j] += dx;
						forceSum += Math.abs(dx);
						}
					else {
						forceY[i] -= dy;
						forceY[j] += dy;
						forceSum += Math.abs(dy);
						}
					}
				}
			}
		return forceSum;
		}
	}
