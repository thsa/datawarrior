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

package com.actelion.research.table;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.util.Date;

import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.DoubleFormat;

public class BinGenerator {
	private static final int NONE = 0;
	private static final int TO_LOG = 1;
	private static final int FROM_LOG = 2;
	
	private int mValueValidation;
	private String[] mCategory;
	private BigDecimal mBinStart,mBinSize;
	private BigDecimal[] mLimit;

	/**
	 * Creates categories from bin size and start bin for a given numerical column.
	 * Bin start value is shifted if needed by n*binSize such that the lowest existing
	 * value ends up in the first bin.
	 * @param tableModel
	 * @param column numerical column with varying data
	 * @param binStart
	 * @param binSize
	 */
	public BinGenerator(CompoundTableModel tableModel, int column, BigDecimal binStart, BigDecimal binSize, boolean useLogValues, boolean isDate) {
		boolean sourceIsLog = tableModel.isLogarithmicViewMode(column);
		mValueValidation = (sourceIsLog == useLogValues) ? NONE : sourceIsLog ? FROM_LOG : TO_LOG;

		if (binSize == null || binSize.doubleValue() == 0.0) {
			mCategory = new String[1];
			mCategory[0] = CompoundTableModel.cRangeNotAvailable;
			return;
			}

		double dataMin = validateValue(tableModel.getMinimumValue(column));
		double dataMax = validateValue(tableModel.getMaximumValue(column));

		if (binStart == null)
			binStart = new BigDecimal(dataMin);

		initialize(binStart, binSize, useLogValues, dataMin, dataMax, isDate);
		}

	public BinGenerator(BigDecimal binStart, BigDecimal binSize, boolean useLogValues, double dataMin, double dataMax, boolean isDate) {
		mValueValidation = useLogValues ? TO_LOG : NONE;
		initialize(binStart, binSize, useLogValues, validateValue(dataMin), validateValue(dataMax), isDate);
		}

	private void initialize(BigDecimal binStart, BigDecimal binSize, boolean useLogValues, double dataMin, double dataMax, boolean isDate) {
		while (binStart.add(binSize).doubleValue() < dataMin)
			binStart = binStart.add(binSize);
		while (binStart.subtract(binSize).doubleValue() >= dataMin)
			binStart = binStart.subtract(binSize);

		int binCount = 0;
		BigDecimal bdDataMax = new BigDecimal(dataMax);
		for (BigDecimal v=binStart; v.compareTo(bdDataMax) <= 0; v=v.add(binSize))
			binCount++;

		mBinStart = binStart;
		mBinSize = binSize;

		mCategory = new String[binCount+1];
		mCategory[binCount] = CompoundTableModel.cRangeNotAvailable;
	
		mLimit = null;	// for logarithmic columns we define rounded limit values on the non-log values
		if (useLogValues) {
			mLimit = new BigDecimal[binCount+1];
			BigDecimal bd = binStart.round(new MathContext(10));
			for (int i=0; i<=binCount; i++) {
				RoundingMode roundingMode = (i == binCount) ? RoundingMode.CEILING : RoundingMode.FLOOR;
				mLimit[i] = new BigDecimal(Math.pow(10.0, bd.doubleValue())).round(new MathContext(3, roundingMode));
				bd = bd.add(binSize);
				}
			for (int i=0; i<binCount; i++) {
				mCategory[i] = mLimit[i].toString() + CompoundTableModel.cRangeSeparation + mLimit[i+1].toString();
				}
			}
		else {
			BigDecimal bd = mBinStart;
			for (int i=0; i<binCount; i++) {
				mCategory[i] = toString(bd, isDate) + CompoundTableModel.cRangeSeparation;
				bd = bd.add(binSize);
				mCategory[i] = mCategory[i].concat(toString(bd, isDate));
				}
			}
		}

	private String toString(BigDecimal bd, boolean isDate) {
		if (isDate)
			return DateFormat.getDateInstance().format(new Date(bd.longValue()*86400000));

//		return bd.stripTrailingZeros().toString();	// scientific notation even for small numbers, e.g.: 1E+2
		return DoubleFormat.toString(bd.doubleValue());
		}

	private double validateValue(double value) {
		return (mValueValidation == NONE) ? value : (mValueValidation == TO_LOG) ? (float)Math.log10(value) : (float)Math.pow(10.0, value);
		}

	public int getBinCount() {
		return mCategory.length - 1;
		}

	/**
	 * @param value taken from the tableModel (log value, if column is defined as such)
	 * @return
	 */
	public int getIndex(double value) {
		if (mBinSize == null || mBinSize.doubleValue() == 0.0)
			return 0;

		int binCount = mCategory.length - 1;
		int index = binCount;
		if (!Double.isNaN(value)) {
			value = validateValue(value);
			if (mLimit != null) {	// if we have limits, we have log mode
				BigDecimal bd = new BigDecimal(Math.pow(10.0, value));
				if (mLimit[0].compareTo(bd) <= 0) {
					for (int i=0; i<binCount; i++) {
						if (mLimit[i+1].compareTo(bd) == 1) {
							index = i;
							break;
							}
						}
					}
				}
			else {
				BigDecimal bd = new BigDecimal(value);
				int likelyIndex = (int)((value - mBinStart.doubleValue()) / mBinSize.doubleValue());

				// check to prevent rounding errors
				BigDecimal lowerLimit = mBinStart.add(mBinSize.multiply(new BigDecimal(likelyIndex)));
				if (lowerLimit.compareTo(bd) > 0)
					return likelyIndex-1;
				if (lowerLimit.add(mBinSize).compareTo(bd) <= 0)
					return likelyIndex+1;
				else
					return likelyIndex;
				}
			}

		return index;
		}

	public String getRangeString(int index) {
		return mCategory[index];
		}
	}
