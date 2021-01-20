package com.actelion.research.datawarrior.task.chem;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;

import javax.swing.*;
import java.util.Properties;

public class DETaskSortReactionsBySimilarity extends DETaskAbstractSortChemistryBySimilarity {
	public static final String TASK_NAME = "Sort Reactions By Similarity";

	private JComboBox mComboBoxReactionPart;
	private String mReactionPart;

	public DETaskSortReactionsBySimilarity(DEFrame parent, int descriptorColumn, String reactionPart, CompoundRecord record) {
		super(parent, descriptorColumn, record);
		mReactionPart = reactionPart;
		}

	@Override
	public Properties getPredefinedConfiguration() {
		Properties configuration = super.getPredefinedConfiguration();
		if (configuration != null)
			configuration.setProperty(PROPERTY_REACTION_PART, mReactionPart);

		return configuration;
		}

	@Override
	protected JComboBox getComboBoxReactionPart() {
		if (mComboBoxReactionPart == null) {
			mComboBoxReactionPart = new JComboBox();
			mComboBoxReactionPart.addItem(CompoundTableConstants.cReactionPartReactants);
			mComboBoxReactionPart.addItem(CompoundTableConstants.cReactionPartProducts);
			mComboBoxReactionPart.setSelectedIndex(1);
			mComboBoxReactionPart.addActionListener(this);
			}
		return mComboBoxReactionPart;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.setProperty(PROPERTY_REACTION_PART, (String)mComboBoxReactionPart.getSelectedItem());
		return configuration;
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mComboBoxReactionPart.setSelectedItem(CompoundTableConstants.cReactionPartProducts);
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		String reactionPart = configuration.getProperty(PROPERTY_REACTION_PART, CompoundTableConstants.cReactionPartProducts);
		mComboBoxReactionPart.setSelectedItem(reactionPart);
		}

	@Override
	protected String getChemistryName() { return "Reaction"; }

	@Override
	protected String getParentColumnType() { return CompoundTableModel.cColumnTypeRXNCode; }

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}
}
