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
package com.actelion.research.datawarrior.task.data.fuzzy;

import com.actelion.research.chem.prediction.MolecularPropertyHelper;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.gui.hidpi.HiDPIIconButton;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Hashtable;

public class FussyParameterPanel extends JPanel implements ActionListener,ChangeListener {
	private JCheckBox	mCheckBoxLogarithmic;
	private JTextField	mTextFieldValueMin,mTextFieldValueMax;
	private SliderPanel	mWeightPanel,mSlopePanel;
	private FussyCurvePanel	mCurvePanel;
	private int mColumn,mProperty;

	/**
	 * Creates a panel to define one property that shall contribute to a fussy score.
	 * Properties may either be an existing numerical column or a computable compound property.
	 * One of property and column must be -1.
	 * @param property -1 or a computable property
	 * @param column -1 or existing column in table model
	 * @param logarithmic
	 * @param tableModel
	 * @param task
	 */
	public FussyParameterPanel(int property, int column, boolean logarithmic, CompoundTableModel tableModel, DETaskCalculateFuzzyScore task) {
		super();

		mProperty = property;
		mColumn = column;

		int scaled4 = HiDPIHelper.scale(4);
		int scaled8 = HiDPIHelper.scale(8);
		double[][] size = { {scaled8, TableLayout.FILL, scaled8, TableLayout.PREFERRED, scaled8, 10*scaled8, 2*scaled8, TableLayout.PREFERRED, scaled8},
				{scaled8, TableLayout.PREFERRED, scaled4, TableLayout.PREFERRED, scaled4, TableLayout.PREFERRED, scaled8, TableLayout.FILL, scaled8, TableLayout.PREFERRED, scaled8} };
		setLayout(new TableLayout(size));

		String propertyName = (column == -1) ? "Computable property: " + MolecularPropertyHelper.getPropertyName(property)
											 : "Column: " + tableModel.getColumnTitle(column);

		KeyAdapter keyAdapter = new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				super.keyTyped(e);
				try {
					if (e.getSource() == mTextFieldValueMin)
						mCurvePanel.setMinValue(Double.parseDouble(mTextFieldValueMin.getText()));
				}
				catch (NumberFormatException nfe) {
					mCurvePanel.setMinValue(Double.NaN);
				}
				try {
					if (e.getSource() == mTextFieldValueMax)
						mCurvePanel.setMaxValue(Double.parseDouble(mTextFieldValueMax.getText()));
				}
				catch (NumberFormatException nfe) {
					mCurvePanel.setMaxValue(Double.NaN);
				}
			}
		};
		mTextFieldValueMin = new JTextField(4);
		mTextFieldValueMin.addKeyListener(keyAdapter);
		mTextFieldValueMax = new JTextField(4);
		mTextFieldValueMax.addKeyListener(keyAdapter);
		double low = (column == -1) ? MolecularPropertyHelper.getRangeMin(property) : tableModel.getMinimumValue(column);
		double high = (column == -1) ?  MolecularPropertyHelper.getRangeMax(property): tableModel.getMaximumValue(column);
		mCurvePanel = new FussyCurvePanel(tableModel, property, column, low, high, logarithmic);
		mSlopePanel = new SliderPanel("Slope:", 10);
		mSlopePanel.slider.addChangeListener(this);
		mWeightPanel = new SliderPanel("Weight:", 4);

		add(new JLabel(propertyName), "1,1");
		add(new JLabel("Minimum:"), "3,1");
		add(mTextFieldValueMin, "5,1");
		add(createCloseButton(), "7,1");
		add(new JLabel("Maximum:"), "3,3");
		add(mTextFieldValueMax, "5,3");
		add(mCurvePanel, "1,3,1,7");
		add(mSlopePanel, "3,5,6,5");
		add(mWeightPanel, "3,7,6,9");

		mCheckBoxLogarithmic = new JCheckBox("Treat logarithmically", logarithmic);
		mCheckBoxLogarithmic.setEnabled(!task.isInteractive() && low > 0);
		mCheckBoxLogarithmic.addActionListener(this);
		add(mCheckBoxLogarithmic, "1,9");
	}

	public boolean isLogarithmic() {
		return mCheckBoxLogarithmic.isSelected();
	}

	public void setLogarithmic(boolean b) {
		mCheckBoxLogarithmic.setSelected(b);
	}

	public String getMaximum() {
		return mTextFieldValueMax.getText();
	}

	public void setMaximum(String max) {
		mTextFieldValueMax.setText(max);
		try {
			mCurvePanel.setMaxValue(Double.parseDouble(max));
		}
		catch (NumberFormatException nfe) {
			mCurvePanel.setMaxValue(Double.NaN);
		}
	}

	public String getMinimum() {
		return mTextFieldValueMin.getText();
	}

	public void setMinimum(String min) {
		mTextFieldValueMin.setText(min);
		try {
			mCurvePanel.setMinValue(Double.parseDouble(min));
		}
		catch (NumberFormatException nfe) {
			mCurvePanel.setMinValue(Double.NaN);
		}
	}

	public double getHalfWidth() {
		return mCurvePanel.getHalfWidth();
	}

	public void setHalfWidth(double halfWidth) {
		mSlopePanel.setValue(mCurvePanel.calcSlope(halfWidth));
	}

	public double getWeight() {
		return mWeightPanel.getValue();
	}

	public void setWeight(double value) {
		mWeightPanel.setValue(value);
	}

	public int getColumn() {
		return mColumn;
	}

	public int getProperty() {
		return mProperty;
	}

	private JPanel createCloseButton() {
		JButton b = new HiDPIIconButton("closeButton.png", null, "close", 0, "square");
		b.addActionListener(this);
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(b, BorderLayout.NORTH);
		return p;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mCheckBoxLogarithmic) {
			mCurvePanel.setLogarithmic(mCheckBoxLogarithmic.isSelected());
			return;
		}

		if (e.getActionCommand().equals("close")) {
			Container theParent = getParent();
			theParent.remove(this);
			theParent.getParent().validate();
			theParent.getParent().repaint();
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		mCurvePanel.setSlope(mSlopePanel.getValue());
	}

	private class SliderPanel extends JPanel implements ChangeListener {
		public JSlider slider;
		private JLabel label;
		private double factor;

		public SliderPanel(String name, double factor) {
			this.factor = factor;
			Hashtable<Integer,JLabel> labels = new Hashtable<Integer,JLabel>();
			labels.put(Integer.valueOf(-100), new JLabel(Double.toString(1.0/factor)));
			labels.put(Integer.valueOf(0), new JLabel("1.0"));
			labels.put(Integer.valueOf(100), new JLabel(Double.toString(1.0*factor)));
			slider = new JSlider(JSlider.HORIZONTAL, -100, 100, 0);
			slider.setMinorTickSpacing(10);
			slider.setMajorTickSpacing(100);
			slider.setLabelTable(labels);
			slider.setPaintLabels(true);
			slider.setPaintTicks(true);
			int height = slider.getPreferredSize().height;
			slider.setMinimumSize(new Dimension(HiDPIHelper.scale(120), height));
			slider.setPreferredSize(new Dimension(HiDPIHelper.scale(120), height));
			slider.addChangeListener(this);

			double[][] size = { {TableLayout.FILL, TableLayout.PREFERRED, TableLayout.PREFERRED},
					{TableLayout.FILL, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.FILL} };
			setLayout(new TableLayout(size));
			add(new JLabel(name), "1,1");
			label = new JLabel("1.0", JLabel.CENTER);
			add(label, "1,2");
			add(slider, "2,0,2,3");
			}

		public double getValue() {
			return Math.pow(factor, slider.getValue() / 100.0f);
			}

		public void setValue(double value) {
			slider.setValue((int)Math.round(100 * Math.min(1.0, Math.max(-1.0, Math.log(value) / Math.log(factor)))));
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if (e.getSource() == slider) {
				label.setText(DoubleFormat.toString(getValue(), 2));
				}
			}
		}
	}
