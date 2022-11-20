package com.actelion.research.table.model;

import java.util.Calendar;

public class CompoundTableRangeBorder {
	private String mText;
	private boolean mIsPercent,mIsFixedWidth; // percent width, relative value, or absolute value

	public CompoundTableRangeBorder(String text) {
		mText = text;
		mIsPercent = text.endsWith("%");
		mIsFixedWidth = text.endsWith("#");
	}

	public boolean isAbsolute() {
		return !mIsFixedWidth && !mIsPercent;
	}

	public boolean isFixedWidth() {
		return mIsFixedWidth;
	}

	public boolean isPercent() {
		return mIsPercent;
	}

	public String getText() {
		return mText;
	}

	/**
	 * Calculates for non-date columns the new limit interpreting the defining text.
	 * If the limit does not extend the range, then the natural data limit is returned.
	 * @param dataLimit
	 * @param isMin
	 * @param isLogarithmic
	 * @return
	 */
	public float getBorderDoubleValue(float dataLimit, float dataRange, boolean isMin, boolean isLogarithmic) {
		if (mText.endsWith("#") || mText.endsWith("%")) {
			try {
				float value = Float.parseFloat(mText.substring(0, mText.length()-1));
				if (value <= 0)
					return dataLimit;

				if (mText.endsWith("%"))
					value = dataRange * value / 100;
				else if (isLogarithmic) {
					float nonLogLimit = (float)Math.pow(10, dataLimit);
					if (isMin)
						nonLogLimit -= value;
					else
						nonLogLimit += value;
					return nonLogLimit > 0 ? (float)Math.log10(nonLogLimit) : dataLimit;
				}

				return isMin ? dataLimit - value : dataLimit + value;
			}
			catch (NumberFormatException nfe) {
				return dataLimit;
			}
		}

		float limit = Float.parseFloat(mText);
		if (isLogarithmic && limit <= 0.0f)
			return dataLimit;

		if (isLogarithmic)
			limit = (float)Math.log10(limit);

		if (isMin)
			return (limit < dataLimit) ? limit : dataLimit;
		else
			return (limit > dataLimit) ? limit : dataLimit;
		}

	/**
	 * Calculates for non-date columns the new limit interpreting the defining text.
	 * If the limit does not extend the range, then the natural data limit is returned.
	 * @param dateLimit
	 * @param isMin
	 * @return
	 */
	public float getBorderDateValue(float dateLimit, float dateRange, boolean isMin) {
		if (mText.endsWith("#") || mText.endsWith("%")) {
			try {
				float value = Float.parseFloat(mText.substring(0, mText.length()-1));
				if (value <= 0)
					return dateLimit;

				if (mText.endsWith("%"))
					value = dateRange * value / 100;

				return isMin ? dateLimit - value : dateLimit + value;
			}
			catch (NumberFormatException nfe) {
				return dateLimit;
			}
		}

		if (mText.length() != 8)
			return dateLimit;

		try {
			int day = Integer.parseInt(mText.substring(0, 2));
			int month = Integer.parseInt(mText.substring(2, 4));
			int year = Integer.parseInt(mText.substring(4));
			if (day < 1 || day > 31 || month < 1 || month > 12 || year < 0)
				return dateLimit;

			Calendar calendar = Calendar.getInstance();
			calendar.clear();
			calendar.set(year, month-1, day, 12, 0, 0);
			long millis = calendar.getTimeInMillis();
			float days = (float)(millis/86400000) + (isMin ? -0.5f : 0.5f);
			if (isMin)
				return (days < dateLimit) ? days : dateLimit;
			else
				return (days > dateLimit) ? days : dateLimit;
		}
		catch (NumberFormatException nfe) {
			return dateLimit;
		}
	}

	public boolean isValid(boolean isDate) {
		if (mText.endsWith("#") || mText.endsWith("%")) {
			try {
				float value = Float.parseFloat(mText.substring(0, mText.length()-1));
				return value > 0;
			}
			catch (NumberFormatException nfe) {
				return false;
			}
		}

		if (isDate) {
			if (mText.length() == 8) {
				int day = Integer.parseInt(mText.substring(0, 2));
				int month = Integer.parseInt(mText.substring(2, 4));
				int year = Integer.parseInt(mText.substring(4));
				if (day >= 1 && day <= 31 && month >= 1 && month <= 12 && year >= 0)
					return true;
			}
			return false;
		}

		try {
			Float.parseFloat(mText);
			return true;
		}
		catch (NumberFormatException nfe) {
			return false;
		}
	}

	public static String millisToDDMMYYYY(String millisString) {
		// TODO... handle trailing symbol # or %
		long millis = 0;
		try { millis = Long.parseLong(millisString); } catch (NumberFormatException nfe) {}
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(millis);
		StringBuilder date = new StringBuilder();
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		int month = calendar.get(Calendar.MONTH)+1;
		int year = calendar.get(Calendar.YEAR);
		if (day < 10)
			date.append("0");
		date.append(day);
		if (month < 10)
			date.append("0");
		date.append(month);
		if (year < 10)
			date.append("000");
		else if (year < 100)
			date.append("00");
		else if (year < 1000)
			date.append("0");
		date.append(year);
		return date.toString();
	}
}
