package com.actelion.research.table.view.chart;

import com.actelion.research.table.view.JVisualization;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

public class ViolinPlot extends AbstractSmoothDistributionPlot {
	private BasicStroke mFatStroke,mLineStroke,mThinLineStroke;

	public ViolinPlot(JVisualization visualization, int hvCount, int doubleAxis) {
		super(visualization, hvCount, doubleAxis);
	}

	@Override
	protected float translate(int hv, int cat, int colorIndex, int fraction) {
		float leftEdge = mSCenter[hv][cat] - mTranslatedMaxWidth * mViolinWidth[hv][cat][mColor.length-1][fraction];
		return colorIndex == -1 ? leftEdge : leftEdge + 2f * mTranslatedMaxWidth * mViolinWidth[hv][cat][colorIndex][fraction];
	}

	@Override
	protected void drawMeanIndicators(Graphics2D g, float median, float mean, float bar, float lineWidth) {
		if (mThinLineStroke == null)
			mThinLineStroke = new BasicStroke(0.5f*lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		g.setStroke(mThinLineStroke);

		switch (mVisualization.getBoxplotMeanMode()) {
			case cBoxplotMeanModeMedian:
				drawIndicatorDot(g, median, bar, lineWidth * 3.5f, mVisualization.getContrastGrey(JVisualization.SCALE_STRONG));
				break;
			case cBoxplotMeanModeMean:
				drawIndicatorDot(g, mean, bar, lineWidth * 3.5f, Color.RED.darker());
				break;
			case cBoxplotMeanModeLines:
				drawIndicatorDot(g, mean, bar, lineWidth * 3.5f, Color.RED.darker());
				drawIndicatorDot(g, median, bar, lineWidth * 3.5f, mVisualization.getContrastGrey(JVisualization.SCALE_STRONG));
				break;
			case cBoxplotMeanModeTriangles:
				float width = 2f * lineWidth;
				float space = 2f * lineWidth;
				float tip = space + 1.5f * width;

				if (mDoubleAxis == 1) {
					drawIndicatorTriangle(g, bar+tip, median, bar+space, median-width, bar+space, median+width, Color.BLACK);
					drawIndicatorTriangle(g, bar-tip, mean, bar-space, mean-width, bar-space, mean+width, Color.RED);
				}
				else {
					drawIndicatorTriangle(g, median, bar+tip, median-width, bar+space, median+width, bar+space, Color.BLACK);
					drawIndicatorTriangle(g, mean, bar-tip, mean-width, bar-space, mean+width, bar-space, Color.RED);
				}
				break;
		}
	}

	@Override
	protected void drawQIndicators(Graphics2D g, float q1, float q3, float bar, float lineWidth, Color color) {
		if (mFatStroke == null)
			mFatStroke = new BasicStroke(3*lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		g.setStroke(mFatStroke);
		g.setColor(color);

		if (mDoubleAxis == 1)
			g.draw(new Line2D.Float(bar, q1, bar, q3));
		else
			g.draw(new Line2D.Float(q1, bar, q3, bar));
	}

	@Override
	protected void drawAVIndicators(Graphics2D g, float lav, float uav, float bar, float lineWidth, Color color) {
		if (mLineStroke == null)
			mLineStroke = new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		g.setStroke(mLineStroke);
		g.setColor(color);

		if (mDoubleAxis == 1)
			g.draw(new Line2D.Float(bar, lav, bar, uav));
		else
			g.draw(new Line2D.Float(lav, bar, uav, bar));
	}

	private void drawIndicatorDot(Graphics2D g, float median, float bar, float size, Color color) {
		Color veryLightGray = mVisualization.getContrastGrey(0f);
		g.setColor(veryLightGray);
		float innerSize = 0.6f * size;
		if (mDoubleAxis == 1) {
			g.fill(new Ellipse2D.Float(bar - 0.5f*size, median - 0.5f*size, size, size));
			g.setColor(color);
			g.fill(new Ellipse2D.Float(bar - 0.5f*innerSize, median - 0.5f*innerSize, innerSize, innerSize));
		}
		else {
			g.fill(new Ellipse2D.Float(median - 0.5f*size, bar - 0.5f*size, size, size));
			g.setColor(color);
			g.fill(new Ellipse2D.Float(median - 0.5f*innerSize, bar - 0.5f*innerSize, innerSize, innerSize));
		}
	}
}
