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

import com.actelion.research.chem.ExtendedDepictor;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorHandlerLongFFP512;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.chem.reaction.ReactionEncoder;
import com.actelion.research.gui.*;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

public class JReactionFilterPanel extends JFilterPanel implements ChangeListener,ItemListener,StructureListener {
	private static final long serialVersionUID = 0x20061002;

	private static final int MIN_ROWS_TO_SHOW_PROGRESS = 50000;

	private static final String	cFilterBySubstructure = "#substructure#";
	private static final String	cFilterBySimilarity = "#similarity#";

	private static final String cItemContains = "contains";
	private static final String cItemIsSimilarTo = "is similar to";

	private static final int ENCODING_MODE = ReactionEncoder.INCLUDE_MAPPING | ReactionEncoder.INCLUDE_COORDS | ReactionEncoder.RETAIN_REACTANT_AND_PRODUCT_ORDER;

	private Frame					mParentFrame;
	private JEditableChemistryView mReactionView;
	private Reaction				mReaction,mReactionWithQueryFeatures;
	private JComboBox				mComboBox;
	private JSlider					mSliderRectionCenter, mSliderPeriphery;
	private int						mCurrentSimilarityDescriptorColumn;
	private float[][][]				mSimilarity;	// 1st index: reaction no; 2nd index: 0->reaction center, 1->periphery; 3rd index: row
	private int[]					mDescriptorColumn;
	private boolean					mDisableEvents;

	/*
	 * Creates the filter panel as UI to configure a task as part of a macro
	 * @param parent
	 * @param tableModel
	 * @param rxn
	 */
	public JReactionFilterPanel(Frame parent, CompoundTableModel tableModel, Reaction rxn) {
		this(parent, tableModel, -1, -1, rxn);
		}

	/**
	 *
	 * @param parent
	 * @param tableModel
	 * @param column
	 * @param exclusionFlag
	 * @param rxn if given, then the filter opens as appropriate substructure or similarity filter
	 */
	public JReactionFilterPanel(Frame parent, CompoundTableModel tableModel, int column, int exclusionFlag, Reaction rxn) {
		super(tableModel, column, exclusionFlag, false, true);
		mParentFrame = parent;

		int gap = HiDPIHelper.scale(8);
		JPanel p = new JPanel();
		double[][] size = { {TableLayout.FILL, gap, TableLayout.FILL},
							{TableLayout.PREFERRED, gap, TableLayout.PREFERRED, TableLayout.PREFERRED, gap, TableLayout.PREFERRED} };
		p.setLayout(new TableLayout(size));

		mReaction = rxn;
		if (mReaction == null) {
			mReaction = new Reaction();
			mReaction.addReactant(new StereoMolecule());	// we need these to carry the fragment state
			mReaction.addProduct(new StereoMolecule());
			mReaction.setFragment(true);
			}

		mComboBox = new JComboBox();
		populateCompoBox();
		p.add(mComboBox, "0,0,2,0");
		if (mComboBox.getItemCount() != 0)
			mComboBox.setSelectedIndex(0);
		mComboBox.addItemListener(this);

		Hashtable<Integer,JLabel> labels = new Hashtable<Integer,JLabel>();
		labels.put(new Integer(0), new JLabel("0"));
		labels.put(new Integer(50), new JLabel("\u00BD"));
		labels.put(new Integer(100), new JLabel("1"));

		mSliderRectionCenter = new JSlider(JSlider.HORIZONTAL, 0, 100, 80);
		mSliderRectionCenter.setMinorTickSpacing(10);
		mSliderRectionCenter.setMajorTickSpacing(100);
		mSliderRectionCenter.setLabelTable(labels);
		mSliderRectionCenter.setPaintLabels(true);
		mSliderRectionCenter.setPaintTicks(true);
		mSliderRectionCenter.setEnabled(false);
		mSliderRectionCenter.setPreferredSize(new Dimension(HiDPIHelper.scale(60), HiDPIHelper.scale(44)));
		mSliderRectionCenter.addChangeListener(this);
		p.add(new JLabel("reaction Center", JLabel.CENTER), "0,2");
		p.add(mSliderRectionCenter, "0,3");

		mSliderPeriphery = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
		mSliderPeriphery.setMinorTickSpacing(10);
		mSliderPeriphery.setMajorTickSpacing(100);
		mSliderPeriphery.setLabelTable(labels);
		mSliderPeriphery.setPaintLabels(true);
		mSliderPeriphery.setPaintTicks(true);
		mSliderPeriphery.setEnabled(false);
		mSliderPeriphery.setPreferredSize(new Dimension(HiDPIHelper.scale(60), HiDPIHelper.scale(44)));
		mSliderPeriphery.addChangeListener(this);
		p.add(new JLabel("periphery", JLabel.CENTER), "2,2");
		p.add(mSliderPeriphery, "2,3");

		mReactionView = new JEditableChemistryView(ExtendedDepictor.TYPE_REACTION);
		mReactionView.setPasteAndDropOptions(JChemistryView.PASTE_AND_DROP_OPTION_REMOVE_CATALYSTS
										   | JChemistryView.PASTE_AND_DROP_OPTION_REMOVE_DRAWING_OBJECTS
										   | JChemistryView.PASTE_AND_DROP_OPTION_LAYOUT_REACTION
										   | JChemistryView.PASTE_AND_DROP_OPTION_ALLOW_FRAGMENT_STATE_CHANGE);
		mReactionView.setAllowDropOrPasteWhenDisabled(true);
		mReactionView.setContent(mReaction);

		int s = HiDPIHelper.scale(80);
		mReactionView.setPreferredSize(new Dimension(2*s, s));
		mReactionView.setBackground(getBackground());
		mReactionView.addStructureListener(this);
		p.add(mReactionView, "0,5,2,5");

		add(p, BorderLayout.CENTER);

		matchSearchTypeToReactionFragmentState();

		enableItems(true);

		if (mComboBox.getItemCount() != 0 && rxn != null)
			updateExclusion(false);

		mIsUserChange = true;
		}

	/**
	 * If the reaction is fragment and search type isn't SSS or if reaction isn't a fragment and search type is SSS,
	 * i.e. if reaction fragment state doesn't match the search type, then this method tries to change the combobox
	 * selection to SSS or the first similarity option, whatever is appropriate. If a proper option doesn't exist,
	 * then the reactions fragment state is changed to be compatible with the current combobox setting.
	 * If the combobox is empty, then nothing is changed and false is returned.
	 * @return 0: nothing changed; 1: combobox selection changed; 2: reaction's fragment state changed
	 */
	private int matchSearchTypeToReactionFragmentState() {
		if (mReaction.getMolecules() == 0 || mComboBox.getItemCount() == 0)
			return 0;

		boolean isFragment = mReaction.isFragment();
		if (isFragment == cItemContains.equals(mComboBox.getSelectedItem()))
			return 0;

		for (int i=0; i<mComboBox.getItemCount(); i++) {
			if (isFragment == cItemContains.equals(mComboBox.getItemAt(i))) {
				mComboBox.removeItemListener(this);
				mComboBox.setSelectedIndex(i);
				mComboBox.addItemListener(this);
				enableItems(isEnabled());
				return 1;
				}
			}

		mReaction.setFragment(!isFragment);
		if (mReactionView != null)
			mReactionView.repaint();
		return 2;
		}

	@Override
	public boolean canEnable(boolean suppressErrorMessages) {
		if (isActive() && mComboBox.getItemCount() == 0) {
			if (!suppressErrorMessages)
				JOptionPane.showMessageDialog(mParentFrame, "This reaction filter cannot be enabled, because\n" +
					"'"+mTableModel.getColumnTitle(mColumnIndex)+"' has no descriptor columns.");
			return false;
			}
		return true;
		}

	@Override
	public void enableItems(boolean b) {
		b &= (mComboBox.getItemCount() != 0);
		mComboBox.setEnabled(b);
		mReactionView.setEnabled(b);

		boolean enableSliders = b && ((String)mComboBox.getSelectedItem()).startsWith(cItemIsSimilarTo);
		mSliderRectionCenter.setEnabled(enableSliders);
		mSliderPeriphery.setEnabled(enableSliders);
		}

	private void populateCompoBox() {
		int reactantFPColumn = -1;
		int productFPColumn = -1;
		int descriptorCount = 0;
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
			if (mTableModel.isDescriptorColumn(column)
			 && mTableModel.getParentColumn(column) == mColumnIndex) {
				if (mTableModel.getDescriptorHandler(column).getInfo().type == DescriptorConstants.DESCRIPTOR_TYPE_REACTION)
					descriptorCount++;
// for reaction substructure filtering we need both FFP512 on the reactant and product side
				else if (mTableModel.getDescriptorHandler(column) instanceof DescriptorHandlerLongFFP512) {
					String reactionPart = mTableModel.getColumnProperty(column, CompoundTableConstants.cColumnPropertyReactionPart);
					if (CompoundTableConstants.cReactionPartReactants.equals(reactionPart))
						reactantFPColumn = column;
					if (CompoundTableConstants.cReactionPartProducts.equals(reactionPart))
						productFPColumn = column;
					}
				}
			}

		mComboBox.removeAllItems();
		boolean sss = supportsSSS() && reactantFPColumn != -1 && productFPColumn != -1;
		boolean sim = supportsSim();
		mDescriptorColumn = new int[(sim ? descriptorCount : 0) + (sss ? 1:0)];
		int descriptorIndex = 0;
		if (sss) {
			mDescriptorColumn[0] = productFPColumn;
			mComboBox.addItem(cItemContains);
			descriptorIndex++;
			}
		if (sim) {
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
				if (mTableModel.isDescriptorColumn(column)
				 && mTableModel.getParentColumn(column) == mColumnIndex
				 && mTableModel.getDescriptorHandler(column).getInfo().type == DescriptorConstants.DESCRIPTOR_TYPE_REACTION) {
					mDescriptorColumn[descriptorIndex++] = column;
					mComboBox.addItem(descriptorToItem(mTableModel.getDescriptorHandler(column).getInfo().shortName));
					}
				}
			}
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

			if (!mTableModel.hasDescriptorColumn(mColumnIndex, DescriptorConstants.DESCRIPTOR_ReactionFP.shortName, null) && !supportsSSS()) {
				removePanel();
				return;
				}

			String selectedItem = (String)mComboBox.getSelectedItem();
			updateComboBoxLater(selectedItem);
			}
		if (e.getType() == CompoundTableEvent.cAddRows
		 || (e.getType() == CompoundTableEvent.cChangeColumnData && e.getColumn() == mColumnIndex)) {
			mSimilarity = null;
			updateExclusionLater();
			}
		else if (e.getType() == CompoundTableEvent.cDeleteRows && mSimilarity != null) {
			int[] rowMapping = e.getMapping();
			float[][][] newSimilarity = new float[mSimilarity.length][2][rowMapping.length];
			for (int i=0; i<mSimilarity.length; i++)
				for (int j=0; j<2; j++)
					for (int k=0; k<rowMapping.length; k++)
					newSimilarity[i][j][k] = mSimilarity[i][j][rowMapping[k]];
			mSimilarity = newSimilarity;
			}

		mIsUserChange = true;
		}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == mComboBox
		 && !mDisableEvents) {
			String item = (String)e.getItem();
			boolean isSSS = cItemContains.equals(item);
			boolean isSimilarity = item != null & item.startsWith(cItemIsSimilarTo);

			if (e.getStateChange() == ItemEvent.DESELECTED) {
				if (isSSS)
					mReactionWithQueryFeatures = new Reaction(mReaction);	// store reaction with query features
				}
			else {
				if (isSimilarity) {	// a descriptor similarity has been chosen
					int newDescriptorColumn = mDescriptorColumn[mComboBox.getSelectedIndex()];
					if (mCurrentSimilarityDescriptorColumn != newDescriptorColumn) {
						mCurrentSimilarityDescriptorColumn = -1;
						mSimilarity = null;
						}
					}

				mDisableEvents = true;

				mSliderRectionCenter.setEnabled(isSimilarity);
				mSliderPeriphery.setEnabled(isSimilarity);

				if (isSSS && mReactionWithQueryFeatures != null)
					mReaction = mReactionWithQueryFeatures;
				else
					mReaction.setFragment(!isSimilarity);
				mReactionView.setContent(mReaction);

				mDisableEvents = false;

				updateExclusion(mIsUserChange);
				}
			}
		}


	@Override
	public void structureChanged(StereoMolecule mol) {
		if (!mDisableEvents) {
			mReaction = mReactionView.getReaction();
			mReactionWithQueryFeatures = null;
			mSimilarity = null;
			if (!mReaction.isEmpty())
				checkMappingAndNotify();
			matchSearchTypeToReactionFragmentState();
			updateExclusion(mIsUserChange);
			}
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
			populateCompoBox();
			}
		else {
			mDescriptorColumn = new int[(supportsSSS() ? 1 : 0) + (supportsSim() ? 1 : 0)];
			if (supportsSSS())
				mComboBox.addItem(cItemContains);
			if (supportsSim())
				mComboBox.addItem(descriptorToItem(DescriptorConstants.DESCRIPTOR_ReactionFP.shortName));
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

		enableItems(isEnabled());

		mReaction.setFragment(cItemContains.equals(mComboBox.getSelectedItem()));
		mReactionView.repaint();

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
		 && getReactionCount() != 0) {
			setEnabled(false);
			JOptionPane.showMessageDialog(mParentFrame, "A reaction filter was set to <disabled>,\n" +
					"because the descriptor calculation has not finished yet.");
			}

		if (getReactionCount() == 0)
			mTableModel.clearRowFlag(mExclusionFlag);
		else {
			if ((mComboBox.getSelectedItem()).equals(cItemContains)) {
				AtomicInteger concurrentIndex = new AtomicInteger();
				int rowCount = mTableModel.getTotalRowCount();

				if (rowCount < MIN_ROWS_TO_SHOW_PROGRESS) {
					mTableModel.setSubReactionExclusion(concurrentIndex, mExclusionFlag, mColumnIndex, getReactions(), isInverse());
					}
				else {
					showProgressBarWithUpdates(concurrentIndex, rowCount, "Searching reactions...");
					new Thread(() -> mTableModel.setSubReactionExclusion(concurrentIndex, mExclusionFlag, mColumnIndex, getReactions(), isInverse())).start();
					}
				}
			else {
				int descriptorColumn = mDescriptorColumn[mComboBox.getSelectedIndex()];

				if (mSimilarity == null) {
					mSimilarity = new float[getReactionCount()][][];
					}

				for (int i=0; i<getReactionCount(); i++) {
					if (mSimilarity[i] == null) {
						mSimilarity[i] = createSimilarityList(getReaction(i), descriptorColumn);
						mCurrentSimilarityDescriptorColumn = descriptorColumn;

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
					mTableModel.setReactionSimilarityExclusion(mExclusionFlag,
							mDescriptorColumn[mComboBox.getSelectedIndex()], mSimilarity,
							(float)mSliderRectionCenter.getValue() / (float)100.0,
							(float)mSliderPeriphery.getValue() / (float)100.0,
							isInverse(), mSliderRectionCenter.getValueIsAdjusting() || mSliderPeriphery.getValueIsAdjusting());
					}
				}
			}

		if (isUserChange)
			fireFilterChanged(FilterEvent.FILTER_UPDATED, false);
		}

	protected float[][] createSimilarityList(Reaction rxn, int descriptorColumn) {
		return (mTableModel.getTotalRowCount() > 400000) ?

				// if we have the slow 3DPPMM2 then use a progress dialog
				createSimilarityListSMP(rxn, descriptorColumn)

				// else calculate similarity list in event dispatcher thread
				: mTableModel.createReactionSimilarityList(rxn, null, descriptorColumn);
	}

	private Reaction[] getReactions() {
		Reaction[] reactions = new Reaction[getReactionCount()];
		for (int i=0; i<reactions.length; i++)
			reactions[i] = getReaction(i);
		return reactions;
	}

	private float[][] createSimilarityListSMP(Reaction rxn, int descriptorColumn) {
		JProgressDialog progressDialog = new JProgressDialog(mParentFrame) {
			private static final long serialVersionUID = 0x20110325;

			public void stopProgress() {
				super.stopProgress();
				close();
			}
		};

		mTableModel.createSimilarityListSMP(rxn, null, null, descriptorColumn, progressDialog, false);

		progressDialog.setVisible(true);

		float[][] similarity = new float[2][];
		similarity[0] = mTableModel.getSimilarityListSMP(true);
		similarity[1] = mTableModel.getSimilarityListSMP(false);

		return similarity;
	}

	protected String descriptorToItem(String descriptor) {
		return cItemIsSimilarTo+" ["+descriptor+"]";
		}

	protected String itemToDescriptor(String item) {
		return item.substring(cItemIsSimilarTo.length()+2, item.length()-1);
		}

	@Override
	public int getFilterType() {
		return FILTER_TYPE_REACTION;
		}



	/****************************************************************
	 * FROM HERE WE HAVE THE METHODS THAT MAY MOVE INTO A DERIVED JSingleReactionFilterPanel in case we ever implement ReactionListFilters
	 ****************************************************************/

	private void checkMappingAndNotify() {
		int mappedAtomCount = 0;
		for (int i=0; i<mReaction.getReactants(); i++) {
			StereoMolecule mol = mReaction.getReactant(i);
			for (int atom=0; atom<mol.getAllAtoms(); atom++)
				if (mol.getAtomMapNo(atom) != 0)
					mappedAtomCount++;
			}

		if (mappedAtomCount == 0) {
			JOptionPane.showMessageDialog(mParentFrame, "None of your reaction's atoms are mapped. For reasonable reaction filtering\n" +
							"you need to use the mapping tool to map reactant atoms to products atoms.");
			}
		}

	protected int getReactionCount() {
		for (int i=0; i<mReaction.getMolecules(); i++)
			if (mReaction.getMolecule(i).getAllAtoms() != 0)
				return 1;

		return 0;
		}

	protected Reaction getReaction(int i) {
		return (i == 0) ? mReaction : null;
		}

	protected boolean supportsSSS() {
		return true;
		}

	protected boolean supportsSim() {
		return true;
		}

	@Override
	public void innerReset() {
		if (!mReaction.isEmpty()) {
			mReaction.clear();
			mReactionWithQueryFeatures = null;
			mReactionView.setContent(mReaction);
			}
		}

	@Override
	public String getInnerSettings() {
		if (!mReaction.isEmpty()) {
			String item = (String)mComboBox.getSelectedItem();
			String settings = item == null || item.equals(cItemContains) ? cFilterBySubstructure
					: itemToDescriptor(item)+"\t"+mSliderRectionCenter.getValue()+"\t"+mSliderPeriphery.getValue();

			settings = attachTABDelimited(settings, ReactionEncoder.encode(mReaction, true, ENCODING_MODE));
			return settings;
			}
		return null;
		}

	@Override
	public void applyInnerSettings(String settings) {
		if (settings != null) {
			String desiredItem = null;
			if (settings.startsWith(cFilterBySubstructure)) {
				String rxncode = settings.substring(cFilterBySubstructure.length()+1);
				mReaction = ReactionEncoder.decode(rxncode, ENCODING_MODE, null);
				mReactionView.setContent(mReaction);
				desiredItem = cItemContains;
				}
			else {
				int index1 = settings.indexOf('\t');
				int index2 = settings.indexOf('\t', index1+1);
				int index3 = settings.indexOf('\t', index2+1);
				if (index1 == -1 || index2 == -1) {
					mReactionView.setContent((Reaction)null);
					desiredItem = cItemContains;
					}
				else {
					String descriptor = settings.substring(0, index1);

					int reactionCenterSimilarity = Integer.parseInt(settings.substring(index1+1, index2));
					int peripherySimilarity = Integer.parseInt(settings.substring(index2+1, index3));
					mSliderRectionCenter.setValue(reactionCenterSimilarity);
					mSliderPeriphery.setValue(peripherySimilarity);
					mReaction = ReactionEncoder.decode(settings.substring(index3+1), ENCODING_MODE, null);
					mReactionView.setContent(mReaction);
					desiredItem = descriptorToItem(descriptor);
					}
				}

			if (!desiredItem.equals(mComboBox.getSelectedItem()))
				mComboBox.setSelectedItem(desiredItem);
			else
				updateExclusion(false);
			}
		}
	}
