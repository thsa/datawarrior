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

import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.DoubleFormat;
import org.apache.commons.math.MathException;
import org.apache.commons.math.stat.inference.TTestImpl;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.text.DecimalFormat;
import java.util.Arrays;

public abstract class AbstractDistributionPlot extends AbstractCategoryChart {
	protected static final int cBoxplotMeanModeMedian = 1;
	protected static final int cBoxplotMeanModeMean = 2;
	protected static final int cBoxplotMeanModeLines = 3;
	protected static final int cBoxplotMeanModeTriangles = 4;

	float[][] mBoxQ1;	// 1st quartile
	float[][] mMedian;	// 2nd quartile = median
	float[][] mBoxQ3;	// 3rd quartile
	float[][] mBoxUAV;	// upper adjacent value
	float[][] mBoxLAV;	// lower adjacent value
	float[][] mPValue;	// t-test based p-value
	float[][] mFoldChange;// fold change compared to reference category
	int[][] mOutlierCount;// count of rows outside of LAV and UAV
	int mDoubleColumn;

    public AbstractDistributionPlot(JVisualization visualization, int hvCount, int doubleAxis) {
    	super(visualization, hvCount, doubleAxis);
    	}

	@Override
	public boolean isChartWithMarkers() {
		// NaN markers (all plots), outliers (box plot), all markers (whisker plot)
		return true;
		}

	public boolean isOutsideValue(int hv, int cat, float value) {
    	return (value < mBoxLAV[hv][cat]) || (value > mBoxUAV[hv][cat]);
    	}

	protected void calculate(int chartType) {
		mDoubleColumn = mVisualization.getColumnIndex(mDoubleAxis);

		VisualizationPoint[] point = mVisualization.getDataPoints();

		// create array with all visible values separated by hv and cat
		int[][] vCount = new int[mHVCount][mCatCount];
		for (VisualizationPoint vp:point) {
			if (mVisualization.isVisibleExcludeNaN(vp)) {
				int cat = mVisualization.getChartCategoryIndex(vp);
				int hv = vp.hvIndex;
				vCount[hv][cat]++;
			}
		}
		double[][][] value = new double[mHVCount][mCatCount][];
		for (int hv = 0; hv<mHVCount; hv++)
			for (int cat = 0; cat<mCatCount; cat++)
				if (vCount[hv][cat] != 0)
					value[hv][cat] = new double[vCount[hv][cat]];

		// fill in values
		mMean = new float[mHVCount][mCatCount];
		vCount = new int[mHVCount][mCatCount];
		for (VisualizationPoint vp:point) {
			if (mVisualization.isVisibleExcludeNaN(vp)) {
				int hv = vp.hvIndex;
				int cat = mVisualization.getChartCategoryIndex(vp);
				float d = mVisualization.getAxisValue(vp.record, mDoubleAxis);
				mMean[hv][cat] += d;
				value[hv][cat][vCount[hv][cat]] = d;
				vCount[hv][cat]++;
			}
		}

		// calculate mean
		for (int hv = 0; hv<mHVCount; hv++) {
			for (int cat = 0; cat<mCatCount; cat++) {
				if (vCount[hv][cat] != 0) {
					mMean[hv][cat] /= vCount[hv][cat];
				}
			}
		}

		// calculate standard deviation and error margin using the values applied to barAxis
		calculateStdDevAndErrorMargin(mVisualization, mCatCount, vCount, mDoubleAxis, -1);

		mBoxQ1 = new float[mHVCount][mCatCount];
		mMedian = new float[mHVCount][mCatCount];
		mBoxQ3 = new float[mHVCount][mCatCount];
		mBoxLAV = new float[mHVCount][mCatCount];
		mBoxUAV = new float[mHVCount][mCatCount];

		mPointsInCategory = new int[mHVCount][mCatCount];

		// calculate statistical parameters from sorted values
		for (int hv = 0; hv<mHVCount; hv++) {
			for (int cat = 0; cat<mCatCount; cat++) {
				if (vCount[hv][cat] != 0) {
					Arrays.sort(value[hv][cat]);
					mBoxQ1[hv][cat] = (float)getQuartile(value[hv][cat], 1);
					mMedian[hv][cat] = (float)getQuartile(value[hv][cat], 2);
					mBoxQ3[hv][cat] = (float)getQuartile(value[hv][cat], 3);

					// set lower and upper adjacent values
					float iqr = mBoxQ3[hv][cat] - mBoxQ1[hv][cat];
					float lowerLimit = mBoxQ1[hv][cat] - 1.5f * iqr;
					float upperLimit = mBoxQ3[hv][cat] + 1.5f * iqr;
					int i = 0;
					while (value[hv][cat][i] < lowerLimit)
						i++;
					mBoxLAV[hv][cat] = (float)value[hv][cat][i];
					i = value[hv][cat].length - 1;
					while (value[hv][cat][i] > upperLimit)
						i--;
					mBoxUAV[hv][cat] = (float)value[hv][cat][i];

					mPointsInCategory[hv][cat] = vCount[hv][cat];
				}
			}
		}

		boolean isShowPValue = mVisualization.isShowPValue();
		boolean isShowFoldChange = mVisualization.isShowFoldChange();

		int pValueColumn = mVisualization.getPValueColumn();
		if (pValueColumn != JVisualization.cColumnUnassigned) {
			int categoryIndex = mVisualization.getCategoryIndex(pValueColumn, mVisualization.getPValueRefCategory());
			if (categoryIndex != -1) {
				if (isShowFoldChange)
					mFoldChange = new float[mHVCount][mCatCount];
				if (isShowPValue)
					mPValue = new float[mHVCount][mCatCount];
				int[] individualIndex = new int[1+ mVisualization.getDimensionCount()];
				for (int hv = 0; hv<mHVCount; hv++) {
					for (int cat = 0; cat<mCatCount; cat++) {
						if (vCount[hv][cat] != 0) {
							int refHV = mVisualization.getReferenceHV(hv, pValueColumn, categoryIndex);
							int refCat = mVisualization.getReferenceCat(cat, pValueColumn, categoryIndex, individualIndex);
							if ((refHV != hv || refCat != cat) && vCount[refHV][refCat] != 0) {
								if (isShowFoldChange) {
									if (mVisualization.getTableModel().isLogarithmicViewMode(mDoubleColumn))
										mFoldChange[hv][cat] = 3.321928094887363f * (mMean[hv][cat] - mMean[refHV][refCat]);	// this is the log2(fc)
									else
										mFoldChange[hv][cat] = mMean[hv][cat] / mMean[refHV][refCat];
								}
								if (isShowPValue) {
									try {
										mPValue[hv][cat] = (float) new TTestImpl().tTest(value[hv][cat], value[refHV][refCat]);
									}
									catch (IllegalArgumentException | MathException e) {
										mPValue[hv][cat] = Float.NaN;
									}
								}
							}
							else {
								if (isShowFoldChange)
									mFoldChange[hv][cat] = Float.NaN;
								if (isShowPValue)
									mPValue[hv][cat] = Float.NaN;
							}
						}
					}
				}
			}
		}
	}

	/**
	 *
	 * @param value
	 * @param no 1(lower), 2(mean), or 3(upper)
	 * @return
	 */
	private double getQuartile(double[] value, int no) {
		int length = value.length;
		if (length == 1)
			return value[0];

		int index;
		switch (no) {
			case 1:
				index = length / 4;
				if ((length & 1) == 0) {
					return ((length & 2) == 2) ? value[index] : (value[index-1] + value[index]) / 2;
				}
				else if ((length & 3) == 1) {
					return (value[index-1] + 3*value[index]) / 4;
				}
				else {
					return (3*value[index] + value[index+1]) / 4;
				}
			case 2:
				index = length / 2;
				return ((length & 1) == 1) ? value[index] : (value[index-1] + value[index]) / 2;
			case 3:
				index = length / 2 + length / 4;
				if ((length & 1) == 0) {
					return ((length & 2) == 2) ? value[index] : (value[index-1] + value[index]) / 2;
				}
				else if ((length & 3) == 1) {
					return (3*value[index] + value[index+1]) / 4;
				}
				else {
					return (value[index] + 3*value[index+1]) / 4;
				}
		}
		return 0;
	}

	protected void calculateCoordinates(Rectangle baseGraphRect, float[][] position, float[][] mean, float[][] median,
	                                    float[][] lav, float[][] uav, float[][] q1, float[][] q3, float[][][] barColorEdge) {
		int dimensions = mVisualization.getDimensionCount();
		float[] axisVisMin = new float[dimensions];
		float[] axisVisMax = new float[dimensions];
		for (int i=0; i<dimensions; i++) {
			axisVisMin[i] = mVisualization.getVisibleMin(i);
			axisVisMax[i] = mVisualization.getVisibleMax(i);
		}

		float cellWidth = (mDoubleAxis == 1) ?
				(float)baseGraphRect.width / (float)mVisualization.getCategoryVisCount(0)
				: (float)baseGraphRect.height / (float)mVisualization.getCategoryVisCount(1);
		float cellHeight = (mDoubleAxis == 1) ?
				(float)baseGraphRect.height
				: (float)baseGraphRect.width;
		float valueRange = (mDoubleAxis == 1) ?
				axisVisMax[1]-axisVisMin[1]
				: axisVisMax[0]-axisVisMin[0];

		int caseSeparationCategoryCount = mVisualization.getCaseSeparationCategoryCount();
		mBarWidth = Math.min(0.2f * cellHeight, 0.5f * cellWidth / caseSeparationCategoryCount);

		int focusFlagNo = mVisualization.getFocusFlag();
		int basicColorCount = mVisualization.getMarkerColor().getColorList().length + 2;
		int colorCount = basicColorCount * ((focusFlagNo == -1) ? 1 : 2);
		int axisCatCount = mVisualization.getCategoryVisCount(mDoubleAxis == 1 ? 0 : 1);

		mInnerDistance = new float[mHVCount][mCatCount];
		float csWidth = (mDoubleAxis == 1 ? cellWidth : -cellWidth)
				* mVisualization.getCaseSeparationValue() / caseSeparationCategoryCount;
		float csOffset = csWidth * (1 - caseSeparationCategoryCount) / 2.0f;
		for (int hv = 0; hv<mHVCount; hv++) {
			int hOffset = 0;
			int vOffset = 0;
			if (mVisualization.isSplitView()) {
				VisualizationSplitter splitter = mVisualization.getSplitter();
				hOffset = splitter.getHIndex(hv) * splitter.getGridWidth();
				vOffset = splitter.getVIndex(hv) * splitter.getGridHeight();
			}

			for (int i=0; i<axisCatCount; i++) {
				for (int j=0; j<caseSeparationCategoryCount; j++) {
					int cat = i*caseSeparationCategoryCount + j;
					if (mPointsInCategory[hv][cat] != 0) {
						mInnerDistance[hv][cat] = (mBoxQ3[hv][cat] - mBoxQ1[hv][cat])
								* cellHeight / valueRange / (float)mPointsInCategory[hv][cat];

						int offset = 0;
						float visMin = 0;
						float factor = 0;
						float distance = mInnerDistance[hv][cat];
						if (mDoubleAxis == 1) {
							position[hv][cat] = baseGraphRect.x + hOffset + i*cellWidth + cellWidth/2;

							offset = baseGraphRect.y + vOffset + baseGraphRect.height;
							visMin = axisVisMin[1];
							factor =  - (float)baseGraphRect.height / valueRange;
							distance = -distance;
						}
						else {
							position[hv][cat] = baseGraphRect.y + vOffset + baseGraphRect.height - i*cellWidth - cellWidth/2;

							offset = baseGraphRect.x + hOffset;
							visMin = axisVisMin[0];
							factor =  (float)baseGraphRect.width / valueRange;
						}

						if (caseSeparationCategoryCount != 1)
							position[hv][cat] += csOffset + j*csWidth;

						if (barColorEdge != null) {
							barColorEdge[hv][cat][0] = offset + factor * (mBoxQ1[hv][cat] - visMin);

							for (int k=0; k<colorCount; k++)
								barColorEdge[hv][cat][k + 1] = barColorEdge[hv][cat][k] + distance
										* (float)mPointsInColorCategory[hv][cat][k];
						}

						if (lav != null)
							lav[hv][cat] = offset + factor * (mBoxLAV[hv][cat] - visMin);
						if (uav != null)
							uav[hv][cat] = offset + factor * (mBoxUAV[hv][cat] - visMin);
						if (q1 != null)
							q1[hv][cat] = offset + factor * (mBoxQ1[hv][cat] - visMin);
						if (q3 != null)
							q3[hv][cat] = offset + factor * (mBoxQ3[hv][cat] - visMin);
						if (mean != null)
							mean[hv][cat] = offset + factor * (mMean[hv][cat] - visMin);
						if (median != null)
							median[hv][cat] = offset + factor * (mMedian[hv][cat] - visMin);
					}
				}
			}
		}
	}

	protected void drawConnectionLines(Graphics2D g, float[][] position, float[][] mean, float[][] median) {
		VisualizationColor markerColor = mVisualization.getMarkerColor();
		Color strongGray = mVisualization.getContrastGrey(JVisualization.SCALE_STRONG);
		int boxplotMeanMode = mVisualization.getBoxplotMeanMode();

		int connectionColumn = mVisualization.getConnectionColumn();
		int caseSeparationColumn = mVisualization.getCaseSeparationColumn();
		int caseSeparationCategoryCount = mVisualization.getCaseSeparationCategoryCount();

		int focusFlagNo = mVisualization.getFocusFlag();
		int basicColorCount = mVisualization.getMarkerColor().getColorList().length + 2;
		int colorCount = basicColorCount * ((focusFlagNo == -1) ? 1 : 2);
		int axisCatCount = mVisualization.getCategoryVisCount(mDoubleAxis == 1 ? 0 : 1);

		// draw connection lines
		if (connectionColumn == JVisualization.cConnectionColumnConnectCases
				|| connectionColumn == mVisualization.getColumnIndex(1- mDoubleAxis)) {
			g.setStroke(((JVisualization2D)mVisualization).getConnectionStroke());
			g.setColor(boxplotMeanMode == cBoxplotMeanModeMean ? Color.RED.darker() : strongGray);
			for (int hv = 0; hv<mHVCount; hv++) {
				for (int j=0; j<caseSeparationCategoryCount; j++) {
					int oldX = Integer.MAX_VALUE;
					int oldY = Integer.MAX_VALUE;
					if (caseSeparationCategoryCount != 1
							&& markerColor.getColorColumn() == caseSeparationColumn) {
						if (markerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) {
							g.setColor(markerColor.getColor(j));
						}
						else {
							for (int k=0; k<colorCount; k++) {
								if (mPointsInColorCategory[hv][j][k] != 0) {
									g.setColor(mColor[k]);
									break;
								}
							}
						}
					}

					for (int i=0; i<axisCatCount; i++) {
						int cat = i*caseSeparationCategoryCount + j;

						if (mPointsInCategory[hv][cat] > 0) {
							int value = Math.round(boxplotMeanMode == cBoxplotMeanModeMean ? mean[hv][cat] : median[hv][cat]);
							int newX = Math.round(mDoubleAxis == 1 ? position[hv][cat] : value);
							int newY = Math.round(mDoubleAxis == 1 ? value : position[hv][cat]);
							if (oldX != Integer.MAX_VALUE) {
								g.drawLine(oldX, oldY, newX, newY);
							}
							oldX = newX;
							oldY = newY;
						}
					}
				}
			}
		}
		else if (caseSeparationCategoryCount != 1 && connectionColumn == caseSeparationColumn) {
			g.setStroke(((JVisualization2D)mVisualization).getConnectionStroke());
			g.setColor(boxplotMeanMode == cBoxplotMeanModeMean ? Color.RED.darker() : strongGray);
			for (int hv = 0; hv<mHVCount; hv++) {
				for (int i=0; i<axisCatCount; i++) {
					int oldX = Integer.MAX_VALUE;
					int oldY = Integer.MAX_VALUE;
					if (markerColor.getColorColumn() == mVisualization.getColumnIndex(1-mDoubleAxis)) {
						if (markerColor.getColorListMode() == VisualizationColor.cColorListModeCategories) {
							g.setColor(markerColor.getColor(i));
						}
						else {
							for (int k=0; k<colorCount; k++) {
								if (mPointsInColorCategory[hv][i*caseSeparationCategoryCount][k] != 0) {
									g.setColor(mColor[k]);
									break;
								}
							}
						}
					}

					for (int j=0; j<caseSeparationCategoryCount; j++) {
						int cat = i*caseSeparationCategoryCount + j;

						if (mPointsInCategory[hv][cat] > 0) {
							int value = Math.round(boxplotMeanMode == cBoxplotMeanModeMean ? mean[hv][cat] : median[hv][cat]);
							int newX = Math.round(mDoubleAxis == 1 ? position[hv][cat] : value);
							int newY = Math.round(mDoubleAxis == 1 ? value : position[hv][cat]);
							if (oldX != Integer.MAX_VALUE) {
								g.drawLine(oldX, oldY, newX, newY);
							}
							oldX = newX;
							oldY = newY;
						}
					}
				}
			}
		}
	}

	protected void paintStatisticsInfo(Graphics2D g, Rectangle baseGraphRect, float[][] position, float[][] lav, float[][] uav, float offset) {
		CompoundTableModel tableModel = mVisualization.getTableModel();

		Color strongGray = mVisualization.getContrastGrey(JVisualization.SCALE_STRONG);

		if (mVisualization.isShowMeanAndMedianValues()
		 || mVisualization.isShowStandardDeviation()
		 || mVisualization.isShowConfidenceInterval()
		 || mVisualization.isShowValueCount()
		 || mFoldChange != null
		 || mPValue != null) {
			String[] lineText = new String[7];
			g.setColor(strongGray);
			int scaledFontHeight = Math.round(((JVisualization2D)mVisualization).getFontHeightScaledToSplitView());
			boolean isLogarithmic = tableModel.isLogarithmicViewMode(mDoubleColumn);
			for (int hv = 0; hv<mHVCount; hv++) {
				int hOffset = 0;
				int vOffset = 0;
				if (mVisualization.isSplitView()) {
					VisualizationSplitter splitter = mVisualization.getSplitter();
					hOffset = splitter.getHIndex(hv) * splitter.getGridWidth();
					vOffset = splitter.getVIndex(hv) * splitter.getGridHeight();
				}
				for (int cat=0; cat<mCatCount; cat++) {
					if (mPointsInCategory[hv][cat] > 0) {
						int lineCount = 0;
						if (mVisualization.isShowMeanAndMedianValues()) {
							int digits = tableModel.isColumnTypeInteger(mDoubleColumn) ? AbstractCategoryChart.INT_DIGITS : AbstractCategoryChart.FLOAT_DIGITS;
							float meanValue = isLogarithmic ? (float)Math.pow(10, mMean[hv][cat]) : mMean[hv][cat];
							float medianValue = isLogarithmic ? (float)Math.pow(10, mMedian[hv][cat]) : mMedian[hv][cat];
							switch (mVisualization.getBoxplotMeanMode()) {
								case cBoxplotMeanModeMedian:
									lineText[lineCount++] = "median="+ DoubleFormat.toString(medianValue, digits);
									break;
								case cBoxplotMeanModeMean:
									lineText[lineCount++] = "mean="+DoubleFormat.toString(meanValue, digits);
									break;
								case cBoxplotMeanModeLines:
								case cBoxplotMeanModeTriangles:
									lineText[lineCount++] = "mean="+DoubleFormat.toString(meanValue, digits);
									lineText[lineCount++] = "median="+DoubleFormat.toString(medianValue, digits);
									break;
							}
						}
						if (mVisualization.isShowStandardDeviation()) {
							if (Float.isInfinite(mStdDev[hv][cat])) {
								lineText[lineCount++] = "\u03C3=Infinity";
							}
							else {
								double sdev = isLogarithmic ? Math.pow(10, mStdDev[hv][cat]) : mStdDev[hv][cat];
								lineText[lineCount++] = "\u03C3=" + DoubleFormat.toString(sdev, AbstractCategoryChart.FLOAT_DIGITS);
							}
						}
						if (mVisualization.isShowConfidenceInterval()) {
							if (Float.isInfinite(mErrorMargin[hv][cat])) {
								lineText[lineCount++] = "CI95: Infinity";
							}
							else {
								double ll = mMean[hv][cat] - mErrorMargin[hv][cat];
								double hl = mMean[hv][cat] + mErrorMargin[hv][cat];
								if (isLogarithmic) {
									ll = Math.pow(10, ll);
									hl = Math.pow(10, hl);
								}
								lineText[lineCount++] = "CI95: ".concat(DoubleFormat.toString(ll, AbstractCategoryChart.FLOAT_DIGITS)).concat("-").concat(DoubleFormat.toString(hl, AbstractCategoryChart.FLOAT_DIGITS));
							}
						}
						if (mVisualization.isShowValueCount()) {
							int outliers = (mOutlierCount == null) ? 0 : mOutlierCount[hv][cat];
							lineText[lineCount++] = "N="+(mPointsInCategory[hv][cat]+outliers);
						}
						if (mFoldChange != null && !Float.isNaN(mFoldChange[hv][cat])) {
							String label = isLogarithmic ? "log2fc=" : "fc=";
							lineText[lineCount++] = label+new DecimalFormat("#.###").format(mFoldChange[hv][cat]);
						}
						if (mPValue != null && !Float.isNaN(mPValue[hv][cat])) {
							lineText[lineCount++] = "p="+new DecimalFormat("#.####").format(mPValue[hv][cat]);
						}

						// calculate the needed space of the text area
						int textWidth = 0;
						int textHeight = lineCount * scaledFontHeight;
						int[] textLineWidth = new int[lineCount];
						for (int line=0; line<lineCount; line++) {
							textLineWidth[line] = g.getFontMetrics().stringWidth(lineText[line]);
							textWidth = Math.max(textWidth, textLineWidth[line]);
						}

						int gap = Math.round((baseGraphRect.width + baseGraphRect.height) / 200f);

						int graphX1 = baseGraphRect.x + hOffset;
						int graphX2 = baseGraphRect.x+baseGraphRect.width + hOffset;
						int graphY1 = baseGraphRect.y + vOffset;
						int graphY2 = baseGraphRect.y+baseGraphRect.height + vOffset;

						for (int line=0; line<lineCount; line++) {
							int x,y;
							if (mDoubleAxis == 1) {
								x = Math.round(position[hv][cat] - textLineWidth[line]/2f);
								if (graphY2-lav[hv][cat]-offset < textHeight + 2*gap
								 && uav[hv][cat]-graphY1-offset > textHeight + 2*gap)
									// top
									y = Math.round(Math.max(graphY1+gap+0.4f*scaledFontHeight, uav[hv][cat]-offset-gap-textHeight)+(0.8f+line)*scaledFontHeight);
								else
									// bottom
									y = Math.round(Math.min(graphY2-textHeight-gap+0.8f*scaledFontHeight,
											lav[hv][cat]+gap+offset+1.0f*scaledFontHeight)+line*scaledFontHeight);
							}
							else {
								if (graphX2-uav[hv][cat]-offset < textWidth + 2*gap
								 && lav[hv][cat]-graphX1-offset > textWidth + 2*gap)
									// left
									x = Math.round(Math.max(graphX1+gap, lav[hv][cat]-textLineWidth[line]-gap-offset));
								else
									// right
									x = Math.round(Math.min(uav[hv][cat]+gap+offset, graphX2-textLineWidth[line]-gap));
								y = Math.round(position[hv][cat]+(0.9f-lineCount/2f)*scaledFontHeight+line*scaledFontHeight);
							}
							g.drawString(lineText[line], x, y);
						}
					}
				}
			}
		}
	}

	protected void drawMeanIndicators(Graphics2D g, float median, float mean, float bar, float lineWidth) {
		switch (mVisualization.getBoxplotMeanMode()) {
			case cBoxplotMeanModeMedian:
				drawIndicatorLine(g, median, bar, lineWidth, mVisualization.getContrastGrey(JVisualization2D.SCALE_STRONG));
				break;
			case cBoxplotMeanModeMean:
				drawIndicatorLine(g, mean, bar, lineWidth, Color.RED.darker());
				break;
			case cBoxplotMeanModeLines:
				drawIndicatorLine(g, mean, bar, lineWidth, Color.RED.darker());
				drawIndicatorLine(g, median, bar, lineWidth, mVisualization.getContrastGrey(JVisualization2D.SCALE_STRONG));
				break;
			case cBoxplotMeanModeTriangles:
				float width = mBarWidth / 4;
				float space = width / 3;
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

	protected void drawIndicatorLine(Graphics2D g, float position, float bar, float lineWidth, Color color) {
		g.setColor(color);
		if (mDoubleAxis == 1) {
			g.fillRect(Math.round(bar- mBarWidth /2),
					Math.round(position-lineWidth/2),
					Math.round(mBarWidth),
					Math.round(lineWidth));
		}
		else {
			g.fillRect(Math.round(position-lineWidth/2),
					Math.round(bar- mBarWidth /2),
					Math.round(lineWidth),
					Math.round(mBarWidth));
		}
	}

	protected void drawIndicatorTriangle(Graphics2D g, float x1, float y1, float x2, float y2, float x3, float y3, Color color) {
		GeneralPath polygon = new GeneralPath(GeneralPath.WIND_NON_ZERO, 3);

		polygon.moveTo(Math.round(x1), Math.round(y1));
		polygon.lineTo(Math.round(x2), Math.round(y2));
		polygon.lineTo(Math.round(x3), Math.round(y3));
		polygon.closePath();

		g.setColor(color);
		g.fill(polygon);
		g.setColor(mVisualization.getContrastGrey(JVisualization2D.SCALE_STRONG));
		g.draw(polygon);
	}
}
