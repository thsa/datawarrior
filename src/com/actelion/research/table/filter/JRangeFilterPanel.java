/*
 * Copyright 2020 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91, CH-4123 Allschwil, Switzerland
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

package com.actelion.research.table.filter;

import com.actelion.research.gui.JPruningBar;
import com.actelion.research.gui.PruningBarEvent;
import com.actelion.research.gui.PruningBarListener;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableListener;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;

public class JRangeFilterPanel extends JFilterPanel
				implements ActionListener,CompoundTableListener,FocusListener,KeyListener,PruningBarListener {
	private static final long serialVersionUID = 0x20060904;

	private static final int DEFAULT_ANIMATION_MILLIS = 10000;

	private JPruningBar	mPruningBar;
	private JTextField  mLabelLow,mLabelHigh,mFieldLowStart,mFieldLowEnd,mFieldHighStart,mFieldHighEnd,mFieldTime;
	private JCheckBox   mCheckBoxBackAndForth;
	private String      mLowStart,mLowEnd,mHighStart,mHighEnd;
	private int         mAnimationMillis;
	private boolean		mIsLogarithmic,mAnimateBackAndForth;

	/**
	 * Creates the filter panel as UI to configure a task as part of a macro
	 * @param tableModel
	 */
	public JRangeFilterPanel(CompoundTableModel tableModel) {
		this(tableModel, -1, -1);
		}

	public JRangeFilterPanel(CompoundTableModel tableModel, int column, int exclusionFlag) {
		super(tableModel, column, exclusionFlag, true, false);

		if (isActive()) {
			tableModel.initializeDoubleExclusion(mExclusionFlag, column);
	
			mPruningBar = new JPruningBar(mTableModel.getMinimumValue(mColumnIndex),
										  mTableModel.getMaximumValue(mColumnIndex), true, 0);
			mPruningBar.setUseRedColor(!mTableModel.isColumnDataComplete(mColumnIndex) && !isInverse());
			add(mPruningBar, BorderLayout.CENTER);
			mPruningBar.addPruningBarListener(this);
	
			mIsLogarithmic = mTableModel.isLogarithmicViewMode(mColumnIndex);
	
			JPanel p = new JPanel();
			p.setOpaque(false);
			p.setLayout(new GridLayout(1,2));
			mLabelLow = new JTextField();
			mLabelLow.setOpaque(false);
			mLabelLow.setBorder(BorderFactory.createEmptyBorder());
			mLabelLow.addFocusListener(this);
			mLabelHigh = new JTextField();
			mLabelHigh.setOpaque(false);
			mLabelHigh.setBorder(BorderFactory.createEmptyBorder());
			mLabelHigh.setHorizontalAlignment(JTextField.RIGHT);
			mLabelHigh.addFocusListener(this);
			p.add(mLabelLow);
			p.add(mLabelHigh);
			add(p, BorderLayout.SOUTH);
	
			setLabelTextFromPruningBars();
	
			if ((!mTableModel.isColumnTypeDate(mColumnIndex)
			  && !mTableModel.isColumnTypeRangeCategory(mColumnIndex))) {
				mLabelLow.addActionListener(this);
				mLabelHigh.addActionListener(this);
				}
			else {
				mLabelLow.setEditable(false);
				mLabelHigh.setEditable(false);
				}
			}
		else {
			JPanel p = new JPanel();
			p.setOpaque(false);
			p.setLayout(new GridLayout(2,2,4,4));
			mLabelLow = new JTextField();
			mLabelLow.addKeyListener(this);
			mLabelHigh = new JTextField();
			mLabelHigh.addKeyListener(this);
			p.add(new JLabel("Low value:"));
			p.add(mLabelLow);
			p.add(new JLabel("High value:"));
			p.add(mLabelHigh);
			add(p, BorderLayout.SOUTH);
			}

		mLowStart = "";
		mLowEnd = "80%";
		mHighStart = "20%";
		mHighEnd = "";
		mAnimationMillis = DEFAULT_ANIMATION_MILLIS;
		mAnimateBackAndForth = false;

		mIsUserChange = true;
		}

	@Override
	public void enableItems(boolean b) {
		if (isActive()) {
			mPruningBar.setEnabled(b);
			}
		mLabelLow.setEnabled(b);
		mLabelHigh.setEnabled(b);
		}

	@Override
	public void pruningBarChanged(PruningBarEvent e) {
		updateExclusion(e.isAdjusting(), mIsUserChange);
		}

	@Override
	public void compoundTableChanged(CompoundTableEvent e) {
		super.compoundTableChanged(e);

		mIsUserChange = false;

		if (e.getType() == CompoundTableEvent.cAddRows
		 || e.getType() == CompoundTableEvent.cDeleteRows
		 || (e.getType() == CompoundTableEvent.cChangeColumnData && e.getColumn() == mColumnIndex)) {
			if (!mTableModel.isColumnTypeDouble(mColumnIndex)
			 || mTableModel.getMinimumValue(mColumnIndex) == mTableModel.getMaximumValue(mColumnIndex)) {
				removePanel();
				return;
				}

			if (mPruningBar.getMinimumValue() != mTableModel.getMinimumValue(mColumnIndex)
			 || mPruningBar.getMaximumValue() != mTableModel.getMaximumValue(mColumnIndex)) {
				float low = mPruningBar.getLowValue();
				float high = mPruningBar.getHighValue();
				boolean lowIsMin = (low == mPruningBar.getMinimumValue());
				boolean highIsMax = (high == mPruningBar.getMaximumValue());

				mPruningBar.setMinAndMax(mTableModel.getMinimumValue(mColumnIndex),
										 mTableModel.getMaximumValue(mColumnIndex));

				if (!lowIsMin || !highIsMax) {
					if (mIsLogarithmic != mTableModel.isLogarithmicViewMode(mColumnIndex)) {
						if (lowIsMin)	// don't calculate in this case to prevent arithmetic discrepancies
							low = mTableModel.getMinimumValue(mColumnIndex);
						else if (mIsLogarithmic)
							low = (float)Math.pow(10.0, low);
						else
							low = (float)Math.log10(low);

						if (highIsMax)	// don't calculate in this case to prevent arithmetic discrepancies
							high = mTableModel.getMaximumValue(mColumnIndex);
						else if (mIsLogarithmic)
							high = (float)Math.pow(10.0, high);
						else
							high = (float)Math.log10(high);
						}
					mPruningBar.setLowAndHigh(low, high, false);
					}

				mIsLogarithmic = mTableModel.isLogarithmicViewMode(mColumnIndex);
				setLabelTextFromPruningBars();
				}

			updateExclusionLater();
			mPruningBar.setUseRedColor(!mTableModel.isColumnDataComplete(mColumnIndex) && !isInverse());
			}

		mIsUserChange = true;
		}

	@Override
	protected JPanel createAnimationOptionPanel() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };
		mFieldLowStart = new JTextField(mLowStart, 4);
		mFieldLowEnd = new JTextField(mLowEnd,4);
		mFieldHighStart = new JTextField(mHighStart,4);
		mFieldHighEnd = new JTextField(mHighEnd,4);
		mFieldTime = new JTextField(DoubleFormat.toString(0.001 * mAnimationMillis),4);
		JPanel p = new JPanel();
		p.setLayout(new TableLayout(size));
		p.add(new JLabel("Low value travels from ", JLabel.RIGHT), "1,1");
		p.add(mFieldLowStart, "2,1");
		p.add(new JLabel(" to "), "3,1");
		p.add(mFieldLowEnd, "4,1");
		p.add(new JLabel("High value travels from ", JLabel.RIGHT), "1,3");
		p.add(mFieldHighStart, "2,3");
		p.add(new JLabel(" to "), "3,3");
		p.add(mFieldHighEnd, "4,3");
		p.add(new JLabel("(keep empty or input value, date, 'min', 'max', or 'NN%')", JLabel.RIGHT), "1,5,4,5");
		p.add(new JLabel("Animation time:", JLabel.RIGHT), "1,7");
		p.add(mFieldTime, "2,7");
		p.add(new JLabel("seconds (one run)"), "3,7,4,7");

		mCheckBoxBackAndForth = new JCheckBox("Run animation backwards before repeating", mAnimateBackAndForth);
		p.add(mCheckBoxBackAndForth, "1,9,4,9");

		return p;
		}

	@Override
	public void applyAnimationOptions() {
		mLowStart = clean(mFieldLowStart.getText());
		mLowEnd = clean(mFieldLowEnd.getText());
		mHighStart = clean(mFieldHighStart.getText());
		mHighEnd = clean(mFieldHighEnd.getText());
		try { mAnimationMillis = Math.round(1000f * Float.parseFloat(mFieldTime.getText())); } catch (NumberFormatException nfe) {}
		mAnimateBackAndForth = mCheckBoxBackAndForth.isSelected();
		}

	private String clean(String value) {
		return value.trim().replace(' ', '-');
		}

	private float parse(String value) {
		float f = Float.NaN;
		if (value != null && value.length() != 0) {
			float min = Float.NaN;
			float max = Float.NaN;
			if (isActive()) {
				min = mPruningBar.getMinimumValue();
				max = mPruningBar.getMaximumValue();
				}
			else {
				try { min = Float.parseFloat(mLabelLow.getText()); } catch (NumberFormatException nfe) {}
				try { max = Float.parseFloat(mLabelHigh.getText()); } catch (NumberFormatException nfe) {}
				}
			if (value.equals("min"))
				return min;
			if (value.equals("max"))
				return max;
			if (value.length()>1 && value.endsWith("%") && Character.isDigit(value.charAt(value.length()-2))) {
				try {
					int p = Integer.parseInt(value.substring(0, value.length()-1));
					if (p >= 0 && p <= 100) {
						if (p == 0)
							f = min;
						else if (p == 100)
							f = max;
						else
							f = min + (max - min) * p / 100;
						}
					} catch (NumberFormatException nfe) {}
				}
			else {
				try {
					f = mTableModel.tryParseEntry(value, mColumnIndex);
					} catch (NumberFormatException nfe) {}
				}
			}
		return f;
		}

	@Override
	public String getAnimationSettings() {
		String s = super.getAnimationSettings();
		if (s == null
		 || (mLowStart.equals(mLowEnd) && mHighStart.equals(mHighEnd)))
			return null;

		StringBuilder sb = new StringBuilder(s);
		if (!Double.isNaN(parse(mLowStart))) {
			sb.append(" low1=");
			sb.append(mLowStart);
			}
		if (!Double.isNaN(parse(mLowEnd))) {
			sb.append(" low2=");
			sb.append(mLowEnd);
			}
		if (!Double.isNaN(parse(mHighStart))) {
			sb.append(" high1=");
			sb.append(mHighStart);
			}
		if (!Double.isNaN(parse(mHighEnd))) {
			sb.append(" high2=");
			sb.append(mHighEnd);
			}
		sb.append(" time=");
		sb.append(DoubleFormat.toString(0.001 * mAnimationMillis));
		if (mAnimateBackAndForth)
			sb.append(" back=true");
		return sb.toString();
		}

	protected void clearAnimationSettings() {
		mLowStart = "";
		mLowEnd = "";
		mHighStart = "";
		mHighEnd = "";
		mAnimationMillis = DEFAULT_ANIMATION_MILLIS;
		mAnimateBackAndForth = false;
		}

	@Override
	protected void applyAnimationSetting(String key, String value) {
		if (key.equals("low1"))
			mLowStart = value;
		else if (key.equals("low2"))
			mLowEnd = value;
		else if (key.equals("high1"))
			mHighStart = value;
		else if (key.equals("high2"))
			mHighEnd = value;
		else if (key.equals("back"))
			mAnimateBackAndForth = value.equals("true");
		else if (key.equals("time"))
			try { mAnimationMillis = Math.round(1000f * Float.parseFloat(value)); }
			catch (NumberFormatException nfe) { mAnimationMillis = DEFAULT_ANIMATION_MILLIS; }
		}

	@Override
	public boolean canAnimate() {
		boolean canAnimate = !mLowStart.equals(mLowEnd) || !mHighStart.equals(mHighEnd);
		if (!canAnimate) {
			Component parent = getParent();
			while (parent != null && !(parent instanceof Frame))
				parent = parent.getParent();
			JOptionPane.showMessageDialog(parent, "At least one of the slider's low and high values\nshould have different start and end values.");
			}
		return canAnimate;
	}

	@Override
	public int getFullFrameAnimationCount() {
		return (mAnimateBackAndForth ? 2 : 1) * mAnimationMillis / getFrameMillis();
		}

	@Override
	public void setAnimationFrame(int frame) {
		float min = mPruningBar.getMinimumValue();
		float max = mPruningBar.getMaximumValue();
		if (max > min) {
			int frameCount = mAnimationMillis / getFrameMillis();
			float progress = ((float)frame % frameCount + 1) / frameCount;
			if (mAnimateBackAndForth && (frame % (2 * frameCount)) >= frameCount)
				progress = 1.0f - progress;
			float low1 = parse(mLowStart);
			if (Float.isNaN(low1))
				low1 = min;
			float low2 = parse(mLowEnd);
			if (Float.isNaN(low2))
				low2 = min;
			float high1 = parse(mHighStart);
			if (Float.isNaN(high1))
				high1 = max;
			float high2 = parse(mHighEnd);
			if (Float.isNaN(high2))
				high2 = max;
			float low = Math.min(max, Math.max(min, low1+progress*(low2-low1)));
			float high = Math.min(max, Math.max(min, high1+progress*(high2-high1)));
			mPruningBar.setLowAndHigh(low, high, false);
			}
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		if (e.getSource() == mLabelLow || e.getSource() == mLabelHigh) {
			labelUpdated((JTextField)e.getSource());
			}
		else if (isActive()) {
			mPruningBar.setUseRedColor(!mTableModel.isColumnDataComplete(mColumnIndex) && !isInverse());
			}
		}

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void keyPressed(KeyEvent e) {}

	@Override
	public void keyReleased(KeyEvent e) {
		float low = Float.NaN;
		float high = Float.NaN;
		try { low = Float.parseFloat(mLabelLow.getText()); } catch (NumberFormatException nfe) {}
		try { high = Float.parseFloat(mLabelHigh.getText()); } catch (NumberFormatException nfe) {}
		JTextField source = (JTextField)e.getSource();
		String text = source.getText();
		float value = (source == mLabelLow) ? low : high;
		boolean isError = (text.length() != 0
						&& (Float.isNaN(value)
//						 || (mTableModel.isLogarithmicViewMode(mColumnIndex) && value <= 0) is not live
						 || (!Float.isNaN(low) && !Float.isNaN(high) && low >= high)));
		source.setBackground(isError ? Color.RED : UIManager.getColor("TextField.background"));
		}

	private void labelUpdated(JTextField source) {
		String error = null;
		try {
			String valueString = source.getText();

			float value = 0;
			if (valueString.equalsIgnoreCase("mean")) {
				for (int row=0; row<mTableModel.getTotalRowCount(); row++)
					value += mTableModel.getTotalDoubleAt(row, mColumnIndex);
				value /= mTableModel.getTotalRowCount();
				}
			else if (valueString.equalsIgnoreCase("median")) {
				float[] values = new float[mTableModel.getTotalRowCount()];
				for (int row=0; row<mTableModel.getTotalRowCount(); row++)
					values[row] = mTableModel.getTotalDoubleAt(row, mColumnIndex);
				Arrays.sort(values);
				int index = mTableModel.getTotalRowCount() / 2;
				if ((values.length & 1) == 1)
					value = values[index];
				else
					value = (values[index-1]+values[index]) / 2;
				}
			else {
				value = Float.parseFloat(valueString);
				if (mTableModel.isLogarithmicViewMode(mColumnIndex))
					value = (float) Math.log10(value);

				float endValue = (source == mLabelLow) ? mPruningBar.getMinimumValue() : mPruningBar.getMaximumValue();
				String endValueString = DoubleFormat.toString(mTableModel.isLogarithmicViewMode(mColumnIndex) ? Math.pow(10.0, endValue) : endValue);

				// to avoid strange edge effects through rounding
				if (valueString.equals(endValueString))
					value = endValue;
				}

			// used to compensate the rounding problem
//			float uncertainty = (mPruningBar.getMaximumValue() - mPruningBar.getMinimumValue()) / 100000;

			if (source == mLabelLow
			 && value >= mPruningBar.getMinimumValue() // - uncertainty
			 && value <= mPruningBar.getHighValue() /* + uncertainty*/) {
				if (value < mPruningBar.getMinimumValue())
					value = mPruningBar.getMinimumValue();
				if (value > mPruningBar.getHighValue())
					value = mPruningBar.getHighValue();
				mPruningBar.setLowValue(value);
				}
			else if (source == mLabelHigh
				  && value <= mPruningBar.getMaximumValue() // + uncertainty
				  && value >= mPruningBar.getLowValue() /* - uncertainty*/) {
				if (value > mPruningBar.getMaximumValue())
					value = mPruningBar.getMaximumValue();
				if (value < mPruningBar.getLowValue())
					value = mPruningBar.getLowValue();
				mPruningBar.setHighValue(value);
				}
			else {
				error = "out of range";
				}
			}
		catch (NumberFormatException nfe) {
			error = "not a number";
			}

		if (error != null) {
			source.setText(error);
			validate();
			new Thread(new Runnable() {
				public void run() {
					try {
						Thread.sleep(1000);
						}
					catch (InterruptedException ie) {}
					setLabelTextFromPruningBars();
					}
				}).start();
			}
		}

	private void setLabelTextFromPruningBars() {
		if (mTableModel.isColumnTypeDate(mColumnIndex)) {
			DateFormat df = DateFormat.getDateInstance();
			mLabelLow.setText(df.format(new Date(86400000*(long)(1.0+mPruningBar.getLowValue()))));
			mLabelHigh.setText(df.format(new Date(86400000*(long)mPruningBar.getHighValue())));
			}
		else if (mTableModel.isColumnTypeRangeCategory(mColumnIndex)) {
			String[] categoryList = mTableModel.getCategoryList(mColumnIndex);
			int low = (int)(mPruningBar.getLowValue()+0.49999);
			int high = (int)(mPruningBar.getHighValue()-0.49999);
			mLabelLow.setText((low >= categoryList.length) ? "" : categoryList[low]);
			mLabelHigh.setText((high < 0) ? "" : categoryList[high]);
			}
		else {	// double values
			if (mTableModel.isLogarithmicViewMode(mColumnIndex)) {
				mLabelLow.setText(DoubleFormat.toString(Math.pow(10.0, mPruningBar.getLowValue())));
				mLabelHigh.setText(DoubleFormat.toString(Math.pow(10.0, mPruningBar.getHighValue())));
				}
			else {
				mLabelLow.setText(DoubleFormat.toString(mPruningBar.getLowValue()));
				mLabelHigh.setText(DoubleFormat.toString(mPruningBar.getHighValue()));
				}
			}
		validate();
		}

	@Override
	public void focusGained(FocusEvent e) {}

	@Override
	public void focusLost(FocusEvent e) {
		if (e.getSource() == mLabelLow || e.getSource() == mLabelHigh) {
			labelUpdated((JTextField)e.getSource());
			}
		}
	
	@Override
	public void updateExclusion(boolean isUserChange) {
		updateExclusion(false, isUserChange);
		}

	private void updateExclusion(boolean isAdjusting, boolean isUserChange) {
		if (isEnabled()) {
			setLabelTextFromPruningBars();
			float low  = mPruningBar.getLowValue();
			float high = mPruningBar.getHighValue();
	
			boolean oldUserChange = mIsUserChange;
			// setDoubleExclusion causes CompoundTableEvents that interfere with the userChange flag
			// (this is a hack. One might alternatively increment and decrement a userChangeInteger instead of setting a flag to count nested events)
	
			mTableModel.setDoubleExclusion(mColumnIndex, mExclusionFlag, low, high, isInverse(), isAdjusting);

			if (mIsUserChange != oldUserChange)
				System.out.println("ERROR: need to repair user change flag in JRangeFilterPanel!");

			if (isUserChange)
				fireFilterChanged(FilterEvent.FILTER_UPDATED, isAdjusting);
			}
		}

	@Override
	public void removePanel() {
		mTableModel.removeCompoundTableListener(this);
		super.removePanel();
		}

	@Override
	public String getInnerSettings() {
		if (isActive()) {
			if (mPruningBar.getLowValue() > mPruningBar.getMinimumValue()
			 || mPruningBar.getHighValue() < mPruningBar.getMaximumValue()) {
				float low = mPruningBar.getLowValue();
				float high = mPruningBar.getHighValue();
				if (mTableModel.isLogarithmicViewMode(getColumnIndex())) {
					low = (float)Math.pow(10, low);
					high = (float)Math.pow(10, high);
					}
				return Float.toString(low)+'\t'+Float.toString(high);
				}
			}
		else {
			String low = mLabelLow.getText();
			String high = mLabelHigh.getText();
			if (low.length() != 0)
				try { Float.parseFloat(low); } catch (NumberFormatException nfe) { low = ""; }
			if (high.length() != 0)
				try { Float.parseFloat(high); } catch (NumberFormatException nfe) { high = ""; }
			if (low.length() != 0 || high.length() != 0)
				return low+'\t'+high;
			}

		return null;
		}

	@Override
	public void applyInnerSettings(String settings) {
		if (settings == null) {
			if (isActive()) {
				mPruningBar.setLowValue(mPruningBar.getMinimumValue());
				mPruningBar.setHighValue(mPruningBar.getMaximumValue());
				}
			else {
				mLabelLow.setText("");
				mLabelHigh.setText("");
				}
			}
		else {
			int index = settings.indexOf('\t');
			if (index != -1) {
				if (isActive()) {
					try {
						float low = Float.parseFloat(settings.substring(0, index));
						float high = Float.parseFloat(settings.substring(index+1));
						if (mTableModel.isLogarithmicViewMode(getColumnIndex())) {
							// min and max are the logarithmic values
							float min = mTableModel.getMinimumValue(getColumnIndex());
							float max = mTableModel.getMaximumValue(getColumnIndex());
							float logLow = (float)Math.log10(low);
							float logHigh = (float)Math.log10(high);
							if (Float.isNaN(logLow) || logLow < min)
								logLow = min;
							if (Float.isNaN(logHigh) || logHigh > max)
								logHigh = max;
							mPruningBar.setLowValue(logLow);
							mPruningBar.setHighValue(logHigh);
							}
						else {	// not log mode
							mPruningBar.setLowValue(low);
							mPruningBar.setHighValue(high);
							}
						}
					catch (NumberFormatException nfe) {}
					}
				else {
					mLabelLow.setText(settings.substring(0, index));
					mLabelHigh.setText(settings.substring(index+1));
					}
				}
			}
		}

	@Override
	public void innerReset() {
		mPruningBar.reset();
		}

	@Override
	public int getFilterType() {
		return FILTER_TYPE_DOUBLE;
		}
	}
