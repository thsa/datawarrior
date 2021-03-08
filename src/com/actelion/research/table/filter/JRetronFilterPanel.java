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

package com.actelion.research.table.filter;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.gui.JEditableStructureView;
import com.actelion.research.gui.StructureListener;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

public class JRetronFilterPanel extends JFilterPanel implements DescriptorConstants,StructureListener {
	private static final long serialVersionUID = 0x20060925;

	private static final int MIN_ROWS_TO_SHOW_PROGRESS = 1000;

	private Frame mParentFrame;
	private JEditableStructureView  mStructureView;

	/**
	 * Creates the filter panel as UI to configure a task as part of a macro
	 * @param parent
	 * @param tableModel
	 * @param mol
	 */
	public JRetronFilterPanel(Frame parent, CompoundTableModel tableModel, StereoMolecule mol) {
		this(parent, tableModel, -1, -1, mol);
		}

	public JRetronFilterPanel(Frame parent, CompoundTableModel tableModel, int column, int exclusionFlag, StereoMolecule mol) {
		super(tableModel, column, exclusionFlag, false, true);

		JPanel contentPanel = new JPanel();
		double[][] size = { {4, TableLayout.FILL, 4},
							{4, TableLayout.FILL, 4} };
		contentPanel.setLayout(new TableLayout(size));
		contentPanel.setOpaque(false);

		if (mol == null) {
			StereoMolecule fragment = new StereoMolecule();
			fragment.setFragment(true);
			mStructureView = new JEditableStructureView(fragment);
		}
		else {
			mStructureView = new JEditableStructureView(mol);
			}

		int s = HiDPIHelper.scale(100);
		mStructureView.setClipboardHandler(new ClipboardHandler());
		mStructureView.setMinimumSize(new Dimension(s, s));
		mStructureView.setPreferredSize(new Dimension(s, s));
		mStructureView.setBackground(getBackground());
		mStructureView.addStructureListener(this);
		mStructureView.setAllowFragmentStatusChangeOnPasteOrDrop(false);
		contentPanel.add(mStructureView, "1,1");

		enableItems(isEnabled());

		add(contentPanel, BorderLayout.CENTER);

		if (mol != null)
			updateExclusion(false);

		mParentFrame = parent;
		setText(getTitle(), null);

		mIsUserChange = true;
		}

	@Override
	public String getTitle() {
		if (mColumnIndex < 0)
			return "";

		return mTableModel.getColumnTitle(mColumnIndex) + " Retron";
		}

	@Override
	public boolean canEnable(boolean suppressErrorMessages) {
		if (isActive()
		 && (getFFPColumn(CompoundTableConstants.cReactionPartReactants) == -1
		  || getFFPColumn(CompoundTableConstants.cReactionPartProducts) == -1)) {
			if (!suppressErrorMessages)
				JOptionPane.showMessageDialog(mParentFrame, "This retron filter cannot be enabled, because it requires FragFp descriptors\n" +
						"'"+mTableModel.getColumnTitle(mColumnIndex)+"for the reactants and products of '"+mTableModel.getColumnTitle(mColumnIndex)+"'.");
			return false;
			}
		return true;
		}

	@Override
	public void enableItems(boolean b) {
		mStructureView.setEnabled(b);
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		super.compoundTableChanged(e);
		if (mColumnIndex == -1) // filter was already removed by super
			return;

		mIsUserChange = false;

		if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			// correct column mapping of mColumnIndex is done by JFilterPanel

			if (getFFPColumn(CompoundTableConstants.cReactionPartReactants) == -1
			 || getFFPColumn(CompoundTableConstants.cReactionPartProducts) == -1) {
				removePanel();
				return;
				}
			}

		mIsUserChange = true;
		}

	@Override
	public void structureChanged(StereoMolecule mol) {
		if (mStructureView.getMolecule().getAllAtoms() == 0)
			setInverse(false);

		updateExclusion(mIsUserChange);
		}

	@Override
	public void innerReset() {
		if (mStructureView.getMolecule().getAllAtoms() != 0) {
			mStructureView.getMolecule().deleteMolecule();
			mStructureView.structureChanged();
			}
		}

	@Override
	public String getInnerSettings() {
		if (mStructureView.getMolecule().getAllAtoms() != 0) {
			StereoMolecule mol = mStructureView.getMolecule();
			return new Canonizer(mol).getIDCode();
			}
		return null;
		}

	@Override
	public void applyInnerSettings(String settings) {
		mStructureView.setIDCode(settings);
		updateExclusion(false);
		}

	@Override
	public int getFilterType() {
		return FILTER_TYPE_RETRON;
		}

	private int getFFPColumn(String reactionPart) {
		return mTableModel.getChildColumn(getColumnIndex(), DESCRIPTOR_FFP512.shortName, reactionPart);
		}

	@Override
	public void updateExclusion(boolean isUserChange) {
		if (!isActive())
			return;

		int reactantFFPColumn = getFFPColumn(CompoundTableConstants.cReactionPartReactants);
		int productFFPColumn = getFFPColumn(CompoundTableConstants.cReactionPartProducts);

		if (isEnabled()
		 && (!mTableModel.isDescriptorAvailable(reactantFFPColumn)
		  || !mTableModel.isDescriptorAvailable(productFFPColumn))) {
			setEnabled(false);
			JOptionPane.showMessageDialog(mParentFrame, "A structure filter cannot be applied and was set to <disabled>,\n" +
					"because the descriptor calculation has not finished yet.");
			}

		if (mStructureView.getMolecule().getAllAtoms() == 0) {
			mTableModel.clearRowFlag(mExclusionFlag);
			}
		else {
			AtomicInteger concurrentIndex = new AtomicInteger();
			int rowCount = mTableModel.getTotalRowCount();

			if (rowCount < MIN_ROWS_TO_SHOW_PROGRESS) {
				mTableModel.setRetronExclusion(concurrentIndex, mExclusionFlag, mColumnIndex, mStructureView.getMolecule(), isInverse());
				}
			else {
				showProgressBarWithUpdates(concurrentIndex, mTableModel.getTotalRowCount(), "Matching retrons...");
				new Thread(() -> mTableModel.setRetronExclusion(concurrentIndex, mExclusionFlag, mColumnIndex, mStructureView.getMolecule(), isInverse())).start();
				}

			if (isUserChange)
				fireFilterChanged(FilterEvent.FILTER_UPDATED, false);
			}
		}
	}
