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

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.IDCodeParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.gui.form.JFXConformerPanel;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class ConformerFitnessPanel extends FitnessPanel {
	private static final long serialVersionUID = 20200218L;

	protected JComboBox mComboBoxDescriptor;
	protected JFXConformerPanel mConformerPanel;

	/**
	 * Creates a new ConformerFitnessPanel, which is configured according to the given configuration.
	 * @param owner
	 * @param configuration without leading fitness option type
	 */
	protected ConformerFitnessPanel(Frame owner, UIDelegateELib delegate, String configuration) {
		this(owner, delegate);

		String[] param = configuration.split("\\t");
		if (param.length >= 3) {
			mComboBoxDescriptor.setSelectedItem(param[0]);
			mSlider.setValue(Integer.parseInt(param[1]));
			for (int i=2; i<param.length-1; i++)
				if (param[i].length() != 0 && param[i+1].length() != 0)
					mConformerPanel.addMolecule(new IDCodeParser(false).getCompactMolecule(param[i], param[i+1]), null, null);
		}
	}

	protected ConformerFitnessPanel(Frame owner, UIDelegateELib delegate) {
		super();

		mComboBoxDescriptor = new JComboBox(ConformerFitnessOption.ALGORITHM_TEXT);
		mComboBoxDescriptor.setSelectedIndex(0);

		mConformerPanel = new JFXConformerPanel(false, false, true);
		mConformerPanel.setBackground(new java.awt.Color(24, 24, 96));
		mConformerPanel.setPreferredSize(new Dimension(HiDPIHelper.scale(160), HiDPIHelper.scale(120)));
		mConformerPanel.setPopupMenuController(new ConformerViewController(owner, mConformerPanel));

		int gap = HiDPIHelper.scale(4);
		double[][] cpsize = {
				{gap, TableLayout.PREFERRED, TableLayout.PREFERRED, 4*gap, TableLayout.FILL, 4*gap, TableLayout.PREFERRED, gap},
				{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, TableLayout.FILL, TableLayout.PREFERRED, TableLayout.FILL, gap} };
		setLayout(new TableLayout(cpsize));

		add(new JLabel("Create molecules with similar conformers"), "1,1,2,1");
		add(new JLabel("Algorithm: ", JLabel.RIGHT), "1,3");
		add(mComboBoxDescriptor, "2,3");
		add(mConformerPanel, "4,1,4,6");
		add(mSliderPanel, "1,5,2,5");
		add(createCloseButton(), "6,1");
		}

	/**
	 * returns the configuration string including the leading type code.
	 */
	@Override
	protected String getConfiguration() {
		StringBuilder sb = new StringBuilder(CONFORMER_OPTION_CODE);
		sb.append('\t').append(mComboBoxDescriptor.getSelectedItem());
		sb.append('\t').append(mSlider.getValue());
		ArrayList<StereoMolecule> moleculeList = mConformerPanel.getMolecules(null);
		for (StereoMolecule mol:moleculeList) {
			Canonizer canonizer = new Canonizer(mol);
			sb.append('\t').append(canonizer.getIDCode());
			sb.append('\t').append(canonizer.getEncodedCoordinates());
			}
		return sb.toString();
		}
	}
