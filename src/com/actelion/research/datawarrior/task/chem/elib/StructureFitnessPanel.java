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
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.datawarrior.task.AbstractTask;
import com.actelion.research.gui.CompoundCollectionModel;
import com.actelion.research.gui.CompoundCollectionPane;
import com.actelion.research.gui.DefaultCompoundCollectionModel;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

public class StructureFitnessPanel extends FitnessPanel {
	private static final long serialVersionUID = 20140724L;

	private static final String[] SEARCH_TYPE_TEXT = {"similar to any", "dissimilar to all"};
	protected static final String[] SEARCH_TYPE_CODE = {"similar", "dissimilar"};

	private static final String FILE_OPTION = "Structure(s) from file";
	private static final String CUSTOM_OPTION = "Custom structure(s)";

	protected JComboBox mComboBoxSearchType,mComboBoxDescriptor,mComboBoxRefStructures;
	protected CompoundCollectionPane<StereoMolecule> mStructurePane;
	protected ArrayList<MoleculeWithDescriptor> mRefMoleculeList;
	private Frame mParentFrame;
	private UIDelegateELib mUIDelegate;

	/**
	 * Creates a new StructureFitnessPanel, which is configured according to the given configuration.
	 * @param owner
	 * @param configuration without leading fitness option type
	 */
	protected StructureFitnessPanel(Frame owner, UIDelegateELib delegate, String configuration) {
		this(owner, delegate);

		String[] param = configuration.split("\\t");
		if (param.length >= 3) {
			mComboBoxSearchType.setSelectedIndex(AbstractTask.findListIndex(param[0], SEARCH_TYPE_CODE, 0));
			mComboBoxDescriptor.setSelectedItem(param[1]);
			mSlider.setValue(Integer.parseInt(param[2]));
			mComboBoxRefStructures.setSelectedIndex(0);
			for (int i=3; i<param.length; i++)
				mStructurePane.getModel().addCompound(new IDCodeParser(true).getCompactMolecule(param[i]));
			}
		}

	protected StructureFitnessPanel(Frame owner, UIDelegateELib delegate) {
		super();

		mParentFrame = owner;
		mUIDelegate = delegate;

		mComboBoxSearchType = new JComboBox(SEARCH_TYPE_TEXT);
		mComboBoxSearchType.setSelectedIndex(0);
		mComboBoxRefStructures = new JComboBox();
		mComboBoxRefStructures.addItem(CUSTOM_OPTION);
		mComboBoxRefStructures.addItem(FILE_OPTION);
		delegate.addStructureOptions(mComboBoxRefStructures);
		mComboBoxRefStructures.addActionListener(this);
		mComboBoxDescriptor = new JComboBox();
		for (int i=0; i<DescriptorConstants.DESCRIPTOR_LIST.length; i++)
			if (DescriptorConstants.DESCRIPTOR_LIST[i].type == DescriptorConstants.DESCRIPTOR_TYPE_MOLECULE)
				mComboBoxDescriptor.addItem(DescriptorConstants.DESCRIPTOR_LIST[i].shortName);
		mComboBoxDescriptor.setSelectedItem(DescriptorConstants.DESCRIPTOR_SkeletonSpheres.shortName);

		DefaultCompoundCollectionModel.Molecule collectionModel = new DefaultCompoundCollectionModel.Molecule();
		mStructurePane = new CompoundCollectionPane<>(collectionModel, false);
		mStructurePane.setEditable(true);
		mStructurePane.setClipboardHandler(new ClipboardHandler());
		mStructurePane.setShowValidationError(true);
		mStructurePane.setPreferredSize(new Dimension(HiDPIHelper.scale(160), HiDPIHelper.scale(80)));

		int gap = HiDPIHelper.scale(4);
		double[][] cpsize = { {gap, TableLayout.PREFERRED, TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED, TableLayout.FILL, TableLayout.PREFERRED, gap},
							  {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, TableLayout.FILL, TableLayout.PREFERRED, gap} };
		setLayout(new TableLayout(cpsize));

		add(new JLabel("Create molecules "), "1,1");
		add(mComboBoxSearchType, "2,1");
		add(mComboBoxRefStructures, "4,1");
		add(new JLabel("Descriptor used: ", JLabel.RIGHT), "1,3");
		add(mComboBoxDescriptor, "2,3");
		add(mStructurePane, "4,3,6,5");
		add(mSliderPanel, "1,5,2,5");
		add(createCloseButton(), "6,1");
		}

	/**
	 * returns the configuration string including the leading type code.
	 */
	@Override
	protected String getConfiguration() {
		StringBuilder sb = new StringBuilder(STRUCTURE_OPTION_CODE);
		sb.append('\t').append(SEARCH_TYPE_CODE[mComboBoxSearchType.getSelectedIndex()]);
		sb.append('\t').append(mComboBoxDescriptor.getSelectedItem());
		sb.append('\t').append(Integer.toString(mSlider.getValue()));
		CompoundCollectionModel<StereoMolecule> model = mStructurePane.getModel();
		for (int i=0; i<model.getSize(); i++)
			sb.append('\t').append(new Canonizer(model.getMolecule(i)).getIDCode());
		return sb.toString();
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mComboBoxRefStructures) {
			mStructurePane.getModel().clear();
			String targetSetOption = (String)mComboBoxRefStructures.getSelectedItem();
			if (targetSetOption.equals(FILE_OPTION)) {
				ArrayList<StereoMolecule> compounds = new FileHelper(mParentFrame).readStructuresFromFile(false);
				if (compounds != null) {
					for (StereoMolecule mol:compounds)
						mStructurePane.getModel().addCompound(mol);
					}
				}
			else if (!targetSetOption.equals(CUSTOM_OPTION)) {
				ArrayList<MoleculeWithDescriptor> mwdl = mUIDelegate.getSelectedMolecules(targetSetOption, null);
				if (mwdl != null)
					for (MoleculeWithDescriptor mwd:mwdl)
						mStructurePane.getModel().addCompound(mwd.mMol);
				}
			return;
			}

		super.actionPerformed(e);
		}
	}
