package com.actelion.research.datawarrior.task.data.fuzzy;

import com.actelion.research.chem.prediction.MolecularPropertyHelper;
import com.actelion.research.gui.LookAndFeelHelper;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.ScaleLabel;
import com.actelion.research.util.ScaleLabelCreator;

import javax.swing.*;
import java.awt.*;
import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class FussyCurvePanel extends JPanel {
	private static final Color CURVE_COLOR = new Color(160, 0, 224);
	private static final double HALF_WIDTH_FACTOR = 0.025;	// fraction of halfWidth in total range (if slope = 1.0)
	private static final int BINCOUNT = 96;

	private CompoundTableModel mTableModel;
	private int		mColumn,mProperty;
	private double	mLowValue,mHighValue,mMinValue,mMaxValue,mHalfWidth;
	private boolean	mIsLogarithmic;
	private ArrayList<ScaleLabel> mLabelList;
	private Image	mOffImage;

	/**
	 * Sets the computed property or existing column for this panel. Either column or property must be -1.
	 * @param tableModel
	 * @param column
	 * @param property
	 */
	public FussyCurvePanel(CompoundTableModel tableModel, int property, int column, double low, double high, boolean logarithmic) {
		mTableModel = tableModel;
		mColumn = column;
		mProperty = property;
		mLowValue = low;
		mHighValue = high;
		mIsLogarithmic = logarithmic;
		mMaxValue = Double.NaN;
		mMinValue = Double.NaN;
		mHalfWidth = calcHalfWidth(1.0);
		updateLabelList();
	}

	private void updateLabelList() {
		if (mIsLogarithmic)
			mLabelList = ScaleLabelCreator.createLogarithmicLabelList(mLowValue, mHighValue);
		else
			mLabelList = ScaleLabelCreator.createLinearLabelList(mLowValue, mHighValue);
	}

	private double calcHalfWidth(double slope) {
		return HALF_WIDTH_FACTOR * (mHighValue - mLowValue) / slope;
	}

	public double calcSlope(double halfWidth) {
		return HALF_WIDTH_FACTOR * (mHighValue - mLowValue) / halfWidth;
	}

	public void setMinValue(double min) {
		mMinValue = mIsLogarithmic ? Math.log10(min) : min;
		repaint();
	}

	public void setMaxValue(double max) {
		mMaxValue = mIsLogarithmic ? Math.log10(max) : max;
		repaint();
	}

	public void setSlope(double slope) {
		mHalfWidth = calcHalfWidth(slope);
		repaint();
	}

	public void setLogarithmic(boolean b) {
		if (mIsLogarithmic != b) {
			double slope = calcSlope(mHalfWidth);
			if (b) {	// convert numbers to logarithms
				mLowValue = Math.log10(mLowValue);
				mHighValue = Math.log10(mHighValue);
				if (!Double.isNaN(mMinValue))
					mMinValue = Math.log10(mMinValue);
				if (!Double.isNaN(mMaxValue))
					mMaxValue = Math.log10(mMaxValue);
			}
			else {
				mLowValue = Math.pow(10, mLowValue);
				mHighValue = Math.pow(10, mHighValue);
				if (!Double.isNaN(mMinValue))
					mMinValue = Math.pow(10, mMinValue);
				if (!Double.isNaN(mMaxValue))
					mMaxValue = Math.pow(10, mMaxValue);
			}
			mHalfWidth = calcHalfWidth(slope);

			mIsLogarithmic = b;
			updateLabelList();

			mOffImage = null;
			repaint();
		}
	}

	public double getHalfWidth() {
		return mHalfWidth;
	}

	@Override public void paintComponent(Graphics g) {
		Dimension size = getSize();
		if (size.width <= 0 || size.height <= 0)
			return;

		float fontSize = HiDPIHelper.scale(12);
		boolean isDarkLaF = LookAndFeelHelper.isDarkLookAndFeel();
		Color background = getBackground();

		float x1 = size.width / 20;
		float x2 = size.width - x1;
		float y1 = size.height / 20;
		float y2 = size.height - 1.2f * fontSize;

		if (mOffImage == null
		 || mOffImage.getWidth(null) != size.width
		 || mOffImage.getHeight(null) != size.height) {
			mOffImage = createImage(size.width,size.height);

			Graphics2D offG = (Graphics2D) mOffImage.getGraphics();
			offG.setColor(background);
			offG.fillRect(0, 0, size.width, size.height);

			offG.setColor(isDarkLaF ? Color.GRAY : Color.LIGHT_GRAY);
			offG.fill(new Rectangle2D.Float(x1, y1, x2-x1, y2-y1));

			if (mHighValue <= mLowValue)	// should not happen
				return;

			// paint histogram
			if (mColumn != -1) {
				offG.setColor(isDarkLaF ? Color.GRAY.darker() : Color.GRAY);
				int[] count = new int[BINCOUNT];
				for (int row = 0; row < mTableModel.getTotalRowCount(); row++) {
					double value = mTableModel.getTotalDoubleAt(row, mColumn);
					if (!Double.isNaN(value)) {

						if (!mIsLogarithmic && mTableModel.isLogarithmicViewMode(mColumn))
							value = Math.pow(10, value);
						else if (mIsLogarithmic && !mTableModel.isLogarithmicViewMode(mColumn))
							value = Math.log10(value);

						int index = (int)((float)BINCOUNT * (value - mLowValue) / (mHighValue - mLowValue));
						index = Math.min(BINCOUNT-1, Math.max(0, index));
						count[index]++;
					}
				}
				int maxCount = 0;
				for (int i=0; i<BINCOUNT; i++)
					if (maxCount < count[i])
						maxCount = count[i];
				float width = (x2 - x1) / BINCOUNT;
				float gap = width / 5;
				float x = x1 + gap / 2;
				for (int i=0; i<BINCOUNT; i++) {
					if (count[i] != 0) {
						float height = 0.9f * (y2 - y1) * count[i] / maxCount;
						offG.fill(new Rectangle2D.Float(x, y2-height, width-gap, height));
					}
					x += width;
				}
			}

			offG.setColor(isDarkLaF ? Color.LIGHT_GRAY : Color.DARK_GRAY);
			offG.setFont(g.getFont().deriveFont(Font.PLAIN, fontSize));
			float y = y2 + offG.getFontMetrics().getAscent();
			for (ScaleLabel sl:mLabelList) {
				float x = x1 + (float)sl.position * (x2-x1);
				offG.draw(new Line2D.Float(x, y1, x, y2));
				offG.drawString(sl.label, x - offG.getFontMetrics().stringWidth(sl.label)/2, y);
			}
		}

		g.drawImage(mOffImage, 0, 0, this);

		g.setColor(CURVE_COLOR);
		((Graphics2D)g).setStroke(new BasicStroke(HiDPIHelper.scale(2)));

		float previousX = x1;
		float previousY = y1;
		for (float f=0f; f<=1f; f+=0.01f) {
			float x = x1 + f * (x2 - x1);
			float y = y2 - (y2 - y1) * (float)MolecularPropertyHelper.getValuation(mLowValue+f*(mHighValue-mLowValue), mMinValue, mMaxValue, mHalfWidth);
			if (f != 0f)
				((Graphics2D)g).draw(new Line2D.Float(previousX, previousY, x, y));
			previousX = x;
			previousY = y;
		}
	}
}
