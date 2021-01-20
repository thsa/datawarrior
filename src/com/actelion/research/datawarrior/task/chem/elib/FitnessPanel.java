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
import com.actelion.research.gui.hidpi.HiDPIIconButton;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

public abstract class FitnessPanel extends JPanel implements ActionListener,ChangeListener {
	private static final long serialVersionUID = 20140724L;

	protected static final String STRUCTURE_OPTION_TEXT = "Structural (dis)similarity";
	protected static final String STRUCTURE_OPTION_CODE = "structure";
	protected static final String CONFORMER_OPTION_TEXT = "Conformer similarity";
	protected static final String CONFORMER_OPTION_CODE = "conformer";
	protected static final int STRUCTURE_OPTION = -2;
	protected static final int CONFORMER_OPTION = -1;

	private static ImageIcon sIcon;

	protected int mType;
	protected JSlider mSlider;
	protected JPanel mSliderPanel;
	private JLabel mLabelWeight1,mLabelWeight2;

	protected static FitnessPanel createFitnessPanel(Frame owner, UIDelegateELib delegate, String configuration) {
		int index = (configuration == null) ? -1 : configuration.indexOf('\t');
		if (index == -1)
			return null;
		String optionCode = configuration.substring(0, index);
		if (optionCode.equals(CONFORMER_OPTION_CODE))
			return new ConformerFitnessPanel(owner, delegate, configuration.substring(index+1));
		if (optionCode.equals(STRUCTURE_OPTION_CODE))
			return new StructureFitnessPanel(owner, delegate, configuration.substring(index+1));
		int type = MolecularPropertyHelper.getTypeFromCode(optionCode);
		return (type == -1) ? null : new PropertyFitnessPanel(type, configuration.substring(index+1));
		}

	protected abstract String getConfiguration();

	protected static FitnessPanel createFitnessPanel(Frame owner, UIDelegateELib delegate, int type) {
		if (type == CONFORMER_OPTION)
			return new ConformerFitnessPanel(owner, delegate);
		if (type == STRUCTURE_OPTION)
			return new StructureFitnessPanel(owner, delegate);

		return new PropertyFitnessPanel(type, MolecularPropertyHelper.getMinText(type), MolecularPropertyHelper.getMaxText(type));
		}

	protected FitnessPanel() {
		super();
		createWeightSlider();
		}

	protected JPanel createCloseButton() {
		JButton b = new HiDPIIconButton("closeButton.png", null, "close", 0, "square");
		b.addActionListener(this);
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(b, BorderLayout.NORTH);
		return p;
		}

	private void createWeightSlider() {
		Hashtable<Integer,JLabel> labels = new Hashtable<Integer,JLabel>();
		labels.put(new Integer(-100), new JLabel("0.25"));
		labels.put(new Integer(0), new JLabel("1.0"));
		labels.put(new Integer(100), new JLabel("4.0"));
		mSlider = new JSlider(JSlider.HORIZONTAL, -100, 100, 0);
		mSlider.setMinorTickSpacing(10);
		mSlider.setMajorTickSpacing(100);
		mSlider.setLabelTable(labels);
		mSlider.setPaintLabels(true);
		mSlider.setPaintTicks(true);
		int height = mSlider.getPreferredSize().height;
		mSlider.setMinimumSize(new Dimension(HiDPIHelper.scale(116), height));
		mSlider.setPreferredSize(new Dimension(HiDPIHelper.scale(116), height));
		mSlider.addChangeListener(this);
		mSliderPanel = new JPanel();
		double[][] size = { {TableLayout.FILL, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.FILL},
							{TableLayout.FILL, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.FILL} };
		mSliderPanel.setLayout(new TableLayout(size));
		mLabelWeight1 = new JLabel("Weight:");
		mSliderPanel.add(mLabelWeight1, "1,1");
		mLabelWeight2 = new JLabel("1.0", JLabel.CENTER);
		mSliderPanel.add(mLabelWeight2, "1,2");
		mSliderPanel.add(mSlider, "2,0,2,3");
		}

	/**
	 * @return 0.25 ... 1.0 ... 4.0
	 */
	public float getWeight() {
		return (float)Math.pow(4.0f, (float)mSlider.getValue() / 100.0f);
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("close")) {
			Container theParent = getParent();
			theParent.remove(this);
			theParent.getParent().validate();
			theParent.getParent().repaint();
			}
		}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == mSlider) {
			mLabelWeight2.setText(DoubleFormat.toString(getWeight(), 2));
			}
		}
	}
