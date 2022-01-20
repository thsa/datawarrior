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
import com.actelion.research.gui.JProgressDialog;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class JStructureFilterPanel extends JFilterPanel implements ChangeListener,DescriptorConstants,ItemListener {
	private static final long serialVersionUID = 0x20060925;

	private static final int MIN_ROWS_TO_SHOW_PROGRESS = 20000;

	protected static final String cFilterBySubstructure = "#substructure#";
	protected static final String cFilterBySimilarity = "#similarity#";

	protected static final String cItemContains = "contains";
	protected static final String cItemIsSimilarTo = "is similar to";

	protected float[][]	mSimilarity;
	protected int[]		mDescriptorColumn;
	protected JComboBox	mComboBox;

	private Frame		mParentFrame;
	private JSlider		mSimilaritySlider;
	private int			mCurrentDescriptorColumn;
	private String		mReactionPart;

	public JStructureFilterPanel(Frame parent, CompoundTableModel tableModel, int column, String reactionPart, int exclusionFlag) {
		super(tableModel, column, exclusionFlag, false, true);
		mParentFrame = parent;
		mReactionPart = reactionPart;
		mCurrentDescriptorColumn = -1;
		setText(getTitle(), null);
		}

	public String getReactionPart() {
		return mReactionPart;
		}

	protected JSlider getSimilaritySlider() {
		if (mSimilaritySlider == null) {
			Hashtable<Integer,JLabel> labels = new Hashtable<Integer,JLabel>();
			labels.put(new Integer(0), new JLabel("0"));
			labels.put(new Integer(50), new JLabel("\u00BD"));
			labels.put(new Integer(100), new JLabel("1"));
			mSimilaritySlider = new JSlider(JSlider.VERTICAL, 0, 100, 80);
			mSimilaritySlider.setOpaque(false);
			mSimilaritySlider.setMinorTickSpacing(10);
			mSimilaritySlider.setMajorTickSpacing(100);
			mSimilaritySlider.setLabelTable(labels);
			mSimilaritySlider.setPaintLabels(true);
			mSimilaritySlider.setPaintTicks(true);
			mSimilaritySlider.setPreferredSize(new Dimension(HiDPIHelper.scale(44), HiDPIHelper.scale(100)));
			mSimilaritySlider.addChangeListener(this);
			}
		return mSimilaritySlider;
		}

	@Override
	public String getTitle() {
		if (mColumnIndex < 0)
			return "";

		String title = mTableModel.getColumnTitle(mColumnIndex);

		if (mReactionPart != null)
			title = title + " " + mReactionPart;

		return title;
		}

	@Override
	public boolean canEnable(boolean suppressErrorMessages) {
		if (isActive() && mComboBox != null && mComboBox.getItemCount() == 0) {
			if (!suppressErrorMessages)
				JOptionPane.showMessageDialog(mParentFrame, "This structure filter cannot be enabled, because\n" +
					"'"+mTableModel.getColumnTitle(mColumnIndex)+"' has no descriptor columns.");
			return false;
			}
		return true;
		}

	public void compoundTableChanged(CompoundTableEvent e) {
		super.compoundTableChanged(e);
		if (mColumnIndex == -1) // filter was already removed by super
			return;

		mIsUserChange = false;

		if (e.getType() == CompoundTableEvent.cAddColumns) {
			updateComboBoxLater((String)mComboBox.getSelectedItem());
			}
		else if (e.getType() == CompoundTableEvent.cRemoveColumns) {
			// correct column mapping of mColumnIndex is done by JFilterPanel

			if (!mTableModel.hasDescriptorColumn(mColumnIndex, mReactionPart) && !supportsSSS()) {
				removePanel();
				return;
				}

			String selectedItem = (String)mComboBox.getSelectedItem();
			updateComboBoxLater(selectedItem);
			}
		if (e.getType() == CompoundTableEvent.cAddRows
		 || (e.getType() == CompoundTableEvent.cChangeColumnData && e.getColumn() == mColumnIndex)) {
			mSimilarity = null;	// TODO keep old values and calculate changes only
			updateExclusionLater();
			}
		else if (e.getType() == CompoundTableEvent.cDeleteRows && mSimilarity != null) {
			int[] rowMapping = e.getMapping();
			float[][] newSimilarity = new float[mSimilarity.length][rowMapping.length];
			for (int i=0; i<mSimilarity.length; i++)
				for (int j=0; j<rowMapping.length; j++)
					newSimilarity[i][j] = mSimilarity[i][rowMapping[j]];
			mSimilarity = newSimilarity;
			}

		mIsUserChange = true;
		}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == mComboBox
		 && e.getStateChange() == ItemEvent.SELECTED) {
			int newDescriptorColumn = mDescriptorColumn[mComboBox.getSelectedIndex()];
			if (newDescriptorColumn != -1
			 && newDescriptorColumn != mCurrentDescriptorColumn) {
				mCurrentDescriptorColumn = -1;
				mSimilarity = null;	// TODO cache descriptors
				}
			}
		}

	public int getCurrentDescriptorColumn() {
		return mCurrentDescriptorColumn;
		}

	/**
	 * If a CompoundTableEvent informs about a change that need to update the filter settings,
	 * then this update should be delayed to not interfere with the completion of the
	 * original change through all listeners. Use this method to do so...
	 */
	private void updateComboBoxLater(final String selectedItem) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				updateComboBox(selectedItem);
				}
			});
		}

	/**
	 * Freshly populates the content of the combo box with<br>
	 * - the 'contains' item (provided SSS is supported and the FragFp is present)<br>
	 * - and an 'is similar to' item per available descriptor (provided similarity is supported)<br>
	 * The first item name that startsWith(selectedItem) will be selected. Otherwise the filter is disabled.
	 * @param selectedItem
	 */
	protected void updateComboBox(String selectedItem) {
		mComboBox.removeItemListener(this);
		mComboBox.removeAllItems();

		if (isActive()) {
			int fingerprintColumn = mTableModel.getChildColumn(mColumnIndex, DESCRIPTOR_FFP512.shortName, null);
			int descriptorCount = 0;
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
				if (mTableModel.isDescriptorColumn(column)
				 && mTableModel.getParentColumn(column) == mColumnIndex
				 && (mReactionPart == null || mReactionPart.equals(mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyReactionPart))))
					descriptorCount++;
	
			mDescriptorColumn = new int[((supportsSSS() && fingerprintColumn != -1) ? 1 : 0)
									   + (supportsSim() ? descriptorCount : 0)];
			int itemIndex = 0;
			if (supportsSSS() && fingerprintColumn != -1) {
				mDescriptorColumn[itemIndex++] = fingerprintColumn;
				mComboBox.addItem(cItemContains);
				}
			if (supportsSim()) {
				for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
					if (mTableModel.isDescriptorColumn(column)
					 && mTableModel.getParentColumn(column) == mColumnIndex
					 && (mReactionPart == null || mReactionPart.equals(mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyReactionPart)))) {
						mDescriptorColumn[itemIndex++] = column;
						mComboBox.addItem(descriptorToItem(mTableModel.getColumnSpecialType(column)));
						}
					}
				}
			}
		else {
			mDescriptorColumn = new int[(supportsSSS() ? 1 : 0)
									  + (supportsSim() ? DescriptorConstants.DESCRIPTOR_LIST.length : 0)];
			if (supportsSSS())
				mComboBox.addItem(cItemContains);
			if (supportsSim())
				for (int i=0; i<DescriptorConstants.DESCRIPTOR_LIST.length; i++)
					mComboBox.addItem(descriptorToItem(DescriptorConstants.DESCRIPTOR_LIST[i].shortName));
			}

		boolean found = false;
		if (selectedItem != null) {
			for (int i=0; i<mComboBox.getItemCount(); i++) {
				String item = (String)mComboBox.getItemAt(i);
				if (item.startsWith(selectedItem)) {
					mComboBox.setSelectedIndex(i);
					found = true;
					break;
					}
				}
			}

		if (!found) {
			setEnabled(false);
			if (mComboBox.getItemCount() != 0)
				mComboBox.setSelectedIndex(0);
			}

		mComboBox.addItemListener(this);
		}

	public void stateChanged(ChangeEvent e) {
		updateExclusion(mIsUserChange);
		}

	@Override
	public void updateExclusion(boolean isUserChange) {
		if (!isActive())
			return;

		if (isEnabled()
		 && !mTableModel.isDescriptorAvailable(mDescriptorColumn[mComboBox.getSelectedIndex()])
		 && getStructureCount() != 0) {
			setEnabled(false);
			JOptionPane.showMessageDialog(mParentFrame, "A structure filter cannot be applied and was set to <disabled>,\n" +
														"because the descriptor calculation has not finished yet.");
			}

		if (getStructureCount() == 0)
			mTableModel.clearRowFlag(mExclusionFlag);
		else {
			if ((mComboBox.getSelectedItem()).equals(cItemContains)) {
				AtomicInteger concurrentIndex = new AtomicInteger();
				int rowCount = getStructureCount() * mTableModel.getTotalRowCount();

				if (SwingUtilities.isEventDispatchThread() && isUserChange
				 && (rowCount >= MIN_ROWS_TO_SHOW_PROGRESS || isPotentiallyLengthyQuery())) {
					showProgressBar(concurrentIndex, rowCount, "Searching structures...");
					// In a macro the thread would cause next macro task to start before exclusion is finished.
					// This is prevented by 'isUserChange' above!!!
					new Thread(() -> {
						mTableModel.setSubStructureExclusion(concurrentIndex, mExclusionFlag, mColumnIndex, getStructures(), mReactionPart, isInverse());
						hideProgressBar();
						} ).start();
					}
				else {
					mTableModel.setSubStructureExclusion(concurrentIndex, mExclusionFlag, mColumnIndex, getStructures(), mReactionPart, isInverse());
					}
				}
			else {
				int descriptorColumn = mDescriptorColumn[mComboBox.getSelectedIndex()];

				if (mSimilarity == null)
					mSimilarity = new float[getStructureCount()][];

				for (int i=0; i<getStructureCount(); i++) {
					if (mSimilarity[i] == null) {
						mSimilarity[i] = createSimilarityList(getStructure(i), descriptorColumn);
						mCurrentDescriptorColumn = descriptorColumn;

						if (mSimilarity[i] == null) {	// user cancelled SMP dialog
							mSimilarity = null;
							break;
							}
						}
					}

				if (mSimilarity == null) {
					setEnabled(false);	// treat this as user change, since the user actively cancelled
					}
				else {
					// If we use a flexophore, we have to tell the table model, because it tracks most recent flexophores
					// for atom contribution coloring.
					if (DESCRIPTOR_Flexophore.shortName.equals(mTableModel.getColumnSpecialType(descriptorColumn))
					 && getStructureCount() == 1)
						mTableModel.setMostRecentExclusionFlexophore(getStructure(0), mExclusionFlag, descriptorColumn);

					mTableModel.setStructureSimilarityExclusion(mExclusionFlag,
							mDescriptorColumn[mComboBox.getSelectedIndex()], getStructures(), mSimilarity,
							(float)mSimilaritySlider.getValue() / (float)100.0, isInverse(),
							mSimilaritySlider.getValueIsAdjusting());
					}
				}
			}

		if (isUserChange)
			fireFilterChanged(FilterEvent.FILTER_UPDATED, false);
		}

	private boolean isPotentiallyLengthyQuery() {
		for (int i=0; i<getStructureCount(); i++) {
			StereoMolecule mol = getStructure(i);
			int bridgeCount = 0;
			for (int bond=0; bond<mol.getAllBonds(); bond++)
				if (mol.isBondBridge(bond))
					bridgeCount++;

			if (bridgeCount > 1)
				return true;
			}

		return false;
		}

	protected float[] createSimilarityList(StereoMolecule mol, int descriptorColumn) {
		return (DESCRIPTOR_Flexophore.shortName.equals(mTableModel.getColumnSpecialType(descriptorColumn))
			 || DESCRIPTOR_ShapeAlign.shortName.equals(mTableModel.getColumnSpecialType(descriptorColumn))
			 || mTableModel.getTotalRowCount() > 500000) ?

			// if we have the slow 3DPPMM2 then use a progress dialog
			createSimilarityListSMP(mol, descriptorColumn)

			// else calculate similarity list in event dispatcher thread
			: mTableModel.createStructureSimilarityList(mol, null, descriptorColumn);
		}

	protected abstract boolean supportsSSS();
	protected abstract boolean supportsSim();
	protected abstract int getStructureCount();
	protected abstract StereoMolecule getStructure(int i);

	private StereoMolecule[] getStructures() {
		StereoMolecule[] structures = new StereoMolecule[getStructureCount()];
		for (int i=0; i<structures.length; i++)
			structures[i] = getStructure(i);
		return structures;
		}

	private float[] createSimilarityListSMP(Object chemObject, int descriptorColumn) {
		float[] similarity = mTableModel.getStructureSimilarityListFromCache(new Canonizer((StereoMolecule)chemObject).getIDCode(), descriptorColumn);
		if (similarity != null)
			return similarity;
		
		JProgressDialog progressDialog = new JProgressDialog(mParentFrame) {
			private static final long serialVersionUID = 0x20110325;

			public void stopProgress() {
				super.stopProgress();
				close();
				}
			};

	   	mTableModel.createSimilarityListSMP(chemObject, null, null, descriptorColumn, progressDialog, false);

	   	progressDialog.setVisible(true);
		similarity = mTableModel.getSimilarityListSMP(false);

		return similarity;
 		}

	protected String descriptorToItem(String descriptor) {
		return cItemIsSimilarTo+" ["+descriptor+"]";
		}

	protected String itemToDescriptor(String item) {
		return item.substring(cItemIsSimilarTo.length()+2, item.length()-1);
		}
	}
