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

import com.actelion.research.chem.prediction.MolecularPropertyHelper;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import info.clearthought.layout.TableLayout;

import javax.swing.JLabel;
import javax.swing.JTextField;

public class PropertyFitnessPanel extends FitnessPanel {
	private static final long serialVersionUID = 20140724L;

	private JTextField mTextFieldValueMin,mTextFieldValueMax;
	private int mType;

	/**
	 * Creates a new PropertyFitnessPanel, which is configured according to the given configuration.
	 * @param type
	 * @param configuration without leading fitness option type
	 */
	protected PropertyFitnessPanel(int type, String configuration) {
		this(type, "", "");
		mType = type;

		String[] param = configuration.split("\\t");
		if (param.length == 3) {
			mTextFieldValueMin.setText(param[0]);
			mTextFieldValueMax.setText(param[1]);
			mSlider.setValue(Integer.parseInt(param[2]));
			}
		}

	protected PropertyFitnessPanel(int type, String defaultMin, String defaultMax) {
		super();
		mType = type;

		mTextFieldValueMin = new JTextField(defaultMin, 4);
		mTextFieldValueMax = new JTextField(defaultMax, 4);

		int gap = HiDPIHelper.scale(8);
		double[][] cpsize = { {gap/2, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED,
								2*gap, TableLayout.FILL, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap/2},
							  {gap/2, TableLayout.FILL, TableLayout.PREFERRED, TableLayout.FILL} };
		setLayout(new TableLayout(cpsize));

		add(new JLabel("Prefer '"+ MolecularPropertyHelper.getPropertyName(type)+"' >= "), "1,2");
		add(mTextFieldValueMin, "2,2");
		add(new JLabel(" and <= "), "3,2");
		add(mTextFieldValueMax, "4,2");
		add(mSliderPanel, "7,1,7,3");
		add(createCloseButton(), "9,1,9,2");
		}

	/**
	 * returns the configuration string including the leading type code.
	 */
	@Override
	protected String getConfiguration() {
		StringBuilder sb = new StringBuilder(MolecularPropertyHelper.getPropertyCode(mType));
		sb.append('\t').append(mTextFieldValueMin.getText());
		sb.append('\t').append(mTextFieldValueMax.getText());
		sb.append('\t').append(Integer.toString(mSlider.getValue()));
		return sb.toString();
		}
	}
