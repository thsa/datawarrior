package com.actelion.research.table.view.chart;

import com.actelion.research.table.view.JVisualization;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;

public class RidgeLinePlot extends AbstractSmoothDistributionPlot {
	private static final float cBasePosition = 0.1f;

	private BasicStroke mFatStroke,mDoubleStroke,mNormalStroke;

	public RidgeLinePlot(JVisualization visualization, int hvCount, int doubleAxis) {
		super(visualization, hvCount, doubleAxis);
	}

	@Override
	public float getScaleLinePosition(int axis) {
		return axis != mDoubleAxis ? cBasePosition : super.getScaleLinePosition(axis);
	}

	@Override
	protected float addDirection(float screenDelta) {
		return mDoubleAxis == 0 ? -screenDelta : screenDelta;
	}

	@Override
	protected float translate(int hv, int cat, int colorIndex, int fraction) {
		float base = mSCenter[hv][cat] + baseLineShift();
		if (mDoubleAxis == 0)
			return colorIndex == -1 ? base : base - 2f * mTranslatedMaxWidth * mViolinWidth[hv][cat][colorIndex][fraction];
		else
			return colorIndex == -1 ? base : base + 2f * mTranslatedMaxWidth * mViolinWidth[hv][cat][colorIndex][fraction];
	}

	@Override
	protected void drawMeanIndicators(Graphics2D g, float median, float mean, float bar, float lineWidth) {
		switch (mVisualization.getBoxplotMeanMode()) {
			case cBoxplotMeanModeMedian:
				if (mFatStroke == null)
					mFatStroke = new BasicStroke(1.2f*lineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
				g.setStroke(mFatStroke);
				drawIndicator(g, median, bar, 5*lineWidth, Color.BLACK);
				break;
			case cBoxplotMeanModeMean:
				if (mFatStroke == null)
					mFatStroke = new BasicStroke(1.2f*lineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
				g.setStroke(mFatStroke);
				drawIndicator(g, mean, bar, 5*lineWidth, Color.RED.darker());
				break;
			case cBoxplotMeanModeLines:
				if (mFatStroke == null)
					mFatStroke = new BasicStroke(1.2f*lineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
				g.setStroke(mFatStroke);
				drawIndicator(g, median, bar, 5*lineWidth, Color.BLACK);
				drawIndicator(g, mean, bar, 5*lineWidth, Color.RED.darker());
				break;
			case cBoxplotMeanModeTriangles:
				drawIndicatorTriangle(g, median, bar, 6*lineWidth, Color.BLACK);
				drawIndicatorTriangle(g, mean, bar, 6*lineWidth, Color.RED.darker());
				break;
		}
	}

	@Override
	protected void drawQIndicators(Graphics2D g, float q1, float q3, float bar, float lineWidth, Color color) {
		if (mVisualization.getBoxplotMeanMode() == cBoxplotMeanModeTriangles) {
			drawIndicatorTriangle(g, q1, bar, 4*lineWidth, color);
			drawIndicatorTriangle(g, q3, bar, 4*lineWidth, color);
		}
		else {
			if (mDoubleStroke == null)
				mDoubleStroke = new BasicStroke(0.8f*lineWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER);
			g.setStroke(mDoubleStroke);
			g.setColor(color);
			drawLineIndicators(g, q1, q3, bar, 0.8f*lineWidth, 3.2f*lineWidth, false);
		}
	}

	@Override
	protected void drawAVIndicators(Graphics2D g, float lav, float uav, float bar, float lineWidth, Color color) {
		if (mVisualization.getBoxplotMeanMode() == cBoxplotMeanModeTriangles) {
			drawIndicatorTriangle(g, lav, bar, 2.5f*lineWidth, color);
			drawIndicatorTriangle(g, uav, bar, 2.5f*lineWidth, color);
		}
		else {
			if (mNormalStroke == null)
				mNormalStroke = new BasicStroke(0.5f*lineWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER);
			g.setStroke(mNormalStroke);
			g.setColor(color);
			drawLineIndicators(g, lav, uav, bar, 0.5f*lineWidth, 1.6f*lineWidth, true);
		}
	}

	private float baseLineShift() {
		float shift = ((0.5f - cBasePosition) * mCellWidth) / mCaseCount;
		return mDoubleAxis == 0 ? shift : -shift;
	}

	private void drawLineIndicators(Graphics2D g, float p1, float p2, float bar, float width, float height, boolean drawBaseLine) {
		float base = bar + baseLineShift();
		if (mDoubleAxis == 0) {
			if (drawBaseLine)
				g.draw(new Line2D.Float(p1, base+width/2, p2, base+width/2));
			g.draw(new Line2D.Float(p1, base, p1, base-height));
			g.draw(new Line2D.Float(p2, base, p2, base-height));
		}
		else {
			if (drawBaseLine)
				g.draw(new Line2D.Float(base-width/2, p1, base-width/2, p2));
			g.draw(new Line2D.Float(base, p1, base+height, p1));
			g.draw(new Line2D.Float(base, p2, base+height, p2));
		}
	}

	private void drawIndicator(Graphics2D g, float pos, float bar, float size, Color color) {
		float base = bar + baseLineShift();
		g.setColor(color);
		if (mDoubleAxis == 0)
			g.draw(new Line2D.Float(pos, base, pos, base-size));
		else
			g.draw(new Line2D.Float(base, pos, base+size, pos));
	}

	private void drawIndicatorTriangle(Graphics2D g, float pos, float bar, float size, Color color) {
		float base = bar + baseLineShift();
		float halfWidth = size / 4;
		if (mDoubleAxis == 0)
			drawTriangle(g, pos-halfWidth, base, pos+halfWidth, base, pos, base-size, color);
		else
			drawTriangle(g, base, pos-halfWidth, base, pos+halfWidth, base+size, pos, color);
	}

	private void drawTriangle(Graphics2D g, float x1, float y1, float x2, float y2, float x3, float y3, Color color) {
		GeneralPath polygon = new GeneralPath(GeneralPath.WIND_NON_ZERO, 3);

		polygon.moveTo(Math.round(x1), Math.round(y1));
		polygon.lineTo(Math.round(x2), Math.round(y2));
		polygon.lineTo(Math.round(x3), Math.round(y3));
		polygon.closePath();

		g.setColor(color);
		g.fill(polygon);
	}

	@Override
	protected int statisticsX(int hv, int cat, int textLineWidth, float textWidth, float gap, float graphX1, float graphX2, float offset) {
		if (mDoubleAxis == 1) {
			return Math.round(mSCenter[hv][cat] + baseLineShift());
		}
		else {
			return super.statisticsX(hv, cat, textLineWidth, textWidth, gap, graphX1, graphX2, offset);
		}
	}

	@Override
	protected int statisticsY(int hv, int cat, int line, int lineCount, int scaledFontHeight, float textHeight, float gap, float graphY1, float graphY2, float offset) {
		if (mDoubleAxis == 1) {
			return super.statisticsY(hv, cat, line, lineCount, scaledFontHeight, textHeight, gap, graphY1, graphY2, offset);
		}
		else {
			return Math.round(mSCenter[hv][cat] + baseLineShift()+(0.9f-lineCount)*scaledFontHeight+line*scaledFontHeight);
		}
	}
}
