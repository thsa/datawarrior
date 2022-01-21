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
import com.actelion.research.chem.descriptor.flexophore.FlexophoreAtomContributionColors;
import com.actelion.research.gui.JEditableStructureView;
import com.actelion.research.gui.StructureListener;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.model.HighlightListener;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

public class JSingleStructureFilterPanel extends JStructureFilterPanel implements DescriptorConstants,HighlightListener,StructureListener {
	private static final long serialVersionUID = 0x20060925;

	private JEditableStructureView  mStructureView;
	private boolean 				mDisableEvents;
	private StereoMolecule			mMoleculeWithQueryFeatures;

	/**
	 * Creates the filter panel as UI to configure a task as part of a macro
	 * @param parent
	 * @param tableModel
	 * @param mol
	 */
	public JSingleStructureFilterPanel(Frame parent, CompoundTableModel tableModel, StereoMolecule mol) {
		this(parent, tableModel, -1, null, -1, mol);
		}

	public JSingleStructureFilterPanel(Frame parent, CompoundTableModel tableModel, int column, String reactionPart, int exclusionFlag, StereoMolecule mol) {
		super(parent, tableModel, column, reactionPart, exclusionFlag);

		JPanel contentPanel = new JPanel();
		double[][] size = { {4, TableLayout.FILL, 4, TableLayout.PREFERRED},
							{TableLayout.PREFERRED, TableLayout.FILL} };
		contentPanel.setLayout(new TableLayout(size));
		contentPanel.setOpaque(false);

		mComboBox = new JComboBox() {
			private static final long serialVersionUID = 0x20080611;
			public Dimension getPreferredSize() {
				Dimension size = super.getPreferredSize();
				size.width = Math.min(72, size.width);
				return size;
				} 
			};
		contentPanel.add(mComboBox, "1,0");
		contentPanel.add(getSimilaritySlider(), "3,0,3,1");

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
		mStructureView.setAllowFragmentStatusChangeOnPasteOrDrop(true);
		contentPanel.add(mStructureView, "1,1");

		updateComboBox(mol==null ? cItemContains : cItemIsSimilarTo);
		enableItems(isEnabled());

		add(contentPanel, BorderLayout.CENTER);

		if (mol != null)
			updateExclusion(false);

		mTableModel.addHighlightListener(this);

		mIsUserChange = true;
		}

	@Override
	public void removePanel() {
		mTableModel.removeHighlightListener(this);
		super.removePanel();
		}

	@Override
	public void enableItems(boolean b) {
		mComboBox.setEnabled(b);
		getSimilaritySlider().setEnabled(b && ((String)mComboBox.getSelectedItem()).startsWith(cItemIsSimilarTo));
		mStructureView.setEnabled(b);
		}

	@Override
	public void itemStateChanged(ItemEvent e) {
		super.itemStateChanged(e);

		if (e.getSource() == mComboBox && !mDisableEvents) {
			String item = (String)e.getItem();
			boolean isSSS = cItemContains.equals(item);

			if (e.getStateChange() == ItemEvent.DESELECTED) {
				if (isSSS)
					mMoleculeWithQueryFeatures = new StereoMolecule(mStructureView.getMolecule());	// store molecule with query features
				}
			else {
				mDisableEvents = true;

				if (isSSS) {
					getSimilaritySlider().setEnabled(false);
					if (mMoleculeWithQueryFeatures != null)
						mStructureView.structureChanged(mMoleculeWithQueryFeatures);
					else {
						mStructureView.getMolecule().setFragment(true);
						mStructureView.structureChanged();
						}
					}
				else {  // similarity
					getSimilaritySlider().setEnabled(true);
					mStructureView.getMolecule().setFragment(false);
					mStructureView.structureChanged();
					}

				mDisableEvents = false;

				updateExclusion(mIsUserChange);
				}

			updateFlexophoreContributionColors();
			}
		}

	@Override
	public void structureChanged(StereoMolecule mol) {
		if (mDisableEvents)	// avoid recursive calls
			return;

		mDisableEvents = true;

		mSimilarity = null;
		mMoleculeWithQueryFeatures = null;
		mol.removeAtomSelection();
		String selectedItem = (String)mComboBox.getSelectedItem();
		if (mol.isFragment()) {
			if (!cItemContains.equals(selectedItem)) {
				if (((String)mComboBox.getItemAt(0)).equals(cItemContains)) {
					mComboBox.setSelectedItem(cItemContains);
					getSimilaritySlider().setEnabled(false);
					}
				else {
					mol.setFragment(false);
					}
				}
			}
		else {
			if (selectedItem == null || !selectedItem.startsWith(cItemIsSimilarTo)) {
				boolean found = false;
				for (int i=0; i<mComboBox.getItemCount(); i++) {
					if (((String)mComboBox.getItemAt(i)).startsWith(cItemIsSimilarTo)) {
						mComboBox.setSelectedIndex(i);
						getSimilaritySlider().setEnabled(true);
						found = true;
						break;
						}
					}
				if (!found)
					mol.setFragment(true);
				}
			}

		mDisableEvents = false;

		if (mStructureView.getMolecule().getAllAtoms() == 0)
			setInverse(false);

		updateExclusion(mIsUserChange);
		}

	@Override
	public void setEnabled(boolean b) {
		super.setEnabled(b);
		updateFlexophoreContributionColors();
	}


	@Override
	public void highlightChanged(CompoundRecord record) {
		updateFlexophoreContributionColors();
		}

	private void updateFlexophoreContributionColors() {
		FlexophoreAtomContributionColors facc = null;
		CompoundRecord highlightedRow = mTableModel.getHighlightedRow();
		int descriptorColumn = getCurrentDescriptorColumn();
		if (highlightedRow != null
		 && isActive()
		 && isEnabled()
		 && DESCRIPTOR_Flexophore.shortName.equals(mTableModel.getColumnSpecialType(descriptorColumn)))
			facc = mTableModel.getMostRecentExclusionFlexophoreColors(descriptorColumn);

		mStructureView.setAtomHighlightColors(facc == null ? null : facc.getRefARGB(), facc == null ? null : facc.getRefRadius());
		}

	@Override
	protected int getStructureCount() {
		return (mStructureView.getMolecule().getAllAtoms() == 0) ? 0 : 1;
		}

	@Override
	protected StereoMolecule getStructure(int i) {
		return (i == 0) ? mStructureView.getMolecule() : null;
		}

	@Override
	protected boolean supportsSSS() {
		return true;
		}

	@Override
	protected boolean supportsSim() {
		return true;
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
			String item = (String)mComboBox.getSelectedItem();
			String settings = item == null || item.equals(cItemContains) ? cFilterBySubstructure
					: itemToDescriptor(item)+"\t"+getSimilaritySlider().getValue();

			StereoMolecule mol = mStructureView.getMolecule();
			settings = attachTABDelimited(settings, new Canonizer(mol).getIDCode());
			return settings;
			}
		return null;
		}

	@Override
	public void applyInnerSettings(String settings) {
		String desiredItem;
		if (settings == null) {
			mStructureView.setIDCode(null);
			desiredItem = cItemContains;
			}
		else if (settings.startsWith(cFilterBySubstructure)) {
			String idcode = settings.substring(cFilterBySubstructure.length()+1);
			mStructureView.setIDCode(idcode);
			desiredItem = cItemContains;
			}
		else {
			int index1 = settings.indexOf('\t');
			int index2 = settings.indexOf('\t', index1+1);
			if (index1 == -1 || index2 == -1) {
				mStructureView.setIDCode(null);
				desiredItem = cItemContains;
				}
			else {
				String descriptor = settings.substring(0, index1);

					// to be compatible with format prior V2.7.0
				if (descriptor.equals(cFilterBySimilarity))
					descriptor = DESCRIPTOR_FFP512.shortName;

				int similarity = Integer.parseInt(settings.substring(index1+1, index2));
				getSimilaritySlider().setValue(similarity);
				String idcode = settings.substring(index2+1);
				mStructureView.setIDCode(idcode);
				desiredItem = descriptorToItem(descriptor);
				}
			}

		if (!desiredItem.equals(mComboBox.getSelectedItem()))
			mComboBox.setSelectedItem(desiredItem);
		else
			updateExclusion(false);
		}

	@Override
	public int getFilterType() {
		return FILTER_TYPE_STRUCTURE;
		}
	}
