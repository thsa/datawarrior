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

package com.actelion.research.datawarrior.task.chem;

import com.actelion.research.chem.Clusterer;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

public class DETaskClusterCompounds extends ConfigurableTask implements ActionListener {
    static final long serialVersionUID = 0x20061004;

    private static final String PROPERTY_DESCRIPTOR_COLUMN = "descriptorColumn";
    private static final String PROPERTY_SIMILARITY_LIMIT = "similarityLimit";
    private static final String PROPERTY_CLUSTER_COUNT_LIMIT = "clusterCountLimit";

    private static final String[] cClusterColumnName = {"Cluster No", "Is Representative"};

    public static final String TASK_NAME = "Cluster Compounds Or Reactions";

	private CompoundTableModel  mTableModel;
	private JComboBox			mComboBoxDescriptorColumn;
	private JCheckBox	        mOptionButton1,mOptionButton2;
	private JTextField          mTextField1,mTextField2;

    public DETaskClusterCompounds(DEFrame parent) {
		super(parent, true);
		mTableModel = parent.getTableModel();
    	}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
	public boolean isConfigurable() {
		boolean descriptorFound = false;
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
			if (qualifiesAsDescriptorColumn(column)) {
				if (!descriptorFound) {
					int count = getStructureCount(column);
					if (count > 100000) {
						showErrorMessage("Clustering is limited to 100,000 compounds/reactions.");
						return false;
						}
					if (count > 20000) {
						showMessage("Clustering of more than 20,000 compounds/reactions may take hours\n" +
								"and multiple GB of memory.", WARNING_MESSAGE);
						}

					descriptorFound = true;
					}
				}
			}

		if (!descriptorFound) {
			showErrorMessage("No chemical descriptor found.");
			return false;
			}

		return true;
		}

	@Override
    public JPanel createDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED, gap} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		content.add(new JLabel("Stop clustering when"), "1,1,5,1");

		mOptionButton1 = new JCheckBox("number of clusters reaches", false);
		mOptionButton1.addActionListener(this);
		content.add(mOptionButton1, "1,3,3,3");
		mTextField1 = new JTextField(4);
		mTextField1.setEnabled(false);
		content.add(mTextField1, "5,3");

		mOptionButton2 = new JCheckBox("highest similarity falls below", true);
		mOptionButton2.addActionListener(this);
		content.add(mOptionButton2, "1,5,3,5");
		mTextField2 = new JTextField("0.8", 4);
		content.add(mTextField2, "5,5");

		content.add(new JLabel("Descriptor:"), "1,7");
		mComboBoxDescriptorColumn = new JComboBox();
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (qualifiesAsDescriptorColumn(column))
				mComboBoxDescriptorColumn.addItem(mTableModel.getColumnTitle(column));
		mComboBoxDescriptorColumn.setEditable(!isInteractive());
		content.add(mComboBoxDescriptorColumn, "3,7,5,7");

		return content;
	    }

	@Override
	public String getHelpURL() {
		return "/html/help/chemistry.html#ClusterCompounds";
		}

	@Override
    public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String similarityLimit = configuration.getProperty(PROPERTY_SIMILARITY_LIMIT);
		String clusterCountLimit = configuration.getProperty(PROPERTY_CLUSTER_COUNT_LIMIT);

		if (similarityLimit == null && clusterCountLimit == null) {
			showErrorMessage("Neither a similarity threshold nor final cluster count are defined.");
			return false;
			}

		if (similarityLimit != null) {
			try {
				double simLimit = Double.parseDouble(similarityLimit);
				if (simLimit < 0.1 || simLimit >= 1.0) {
					showErrorMessage("The similarity threshold must be between 0.1 and 1.0");
					return false;
					}
				}
			catch (NumberFormatException nfe) {
				showErrorMessage("The similarity threshold is not numerical");
				return false;
				}
			}

		String descriptorName = configuration.getProperty(PROPERTY_DESCRIPTOR_COLUMN, "");
		if (descriptorName.length() == 0) {
			showErrorMessage("Descriptor column not defined.");
			return false;
			}
		int descriptorColumn = -1;
		if (isLive) {
			descriptorColumn = mTableModel.findColumn(descriptorName);
			if (descriptorColumn == -1) {
				showErrorMessage("Descriptor column '"+descriptorName+"' not found.");
				return false;
				}
			if (!qualifiesAsDescriptorColumn(descriptorColumn)) {
				showErrorMessage("Descriptor column '"+descriptorName+"' does not qualify.");
				return false;
				}
			}

		if (clusterCountLimit != null) {
			try {
				int countLimit = Integer.parseInt(clusterCountLimit);
				if (isLive) {
					int structureCount = getStructureCount(descriptorColumn);
					if (countLimit < 1 || countLimit >= structureCount) {
						showErrorMessage("The final cluster count must be at least 1\n and less than the number of available compounds/reactions ("+structureCount+").");
						return false;
						}
					}
				}
			catch (NumberFormatException nfe) {
				showErrorMessage("The final cluster count is not numerical");
				return false;
				}
			}

		return true;
		}

	@Override
    public void setDialogConfiguration(Properties configuration) {
		String value = configuration.getProperty(PROPERTY_CLUSTER_COUNT_LIMIT);
		if (value != null) {
			mOptionButton1.setSelected(true);
			mTextField1.setEnabled(true);
			mTextField1.setText(value);
			}
		else {
			mOptionButton1.setSelected(false);
			mTextField1.setEnabled(false);
			mTextField1.setText("");
			}

		value = configuration.getProperty(PROPERTY_SIMILARITY_LIMIT);
		if (value != null) {
			mOptionButton2.setSelected(true);
			mTextField2.setEnabled(true);
			mTextField2.setText(value);
			}
		else {
			mOptionButton2.setSelected(false);
			mTextField2.setEnabled(false);
			mTextField2.setText("");
			}

		value = configuration.getProperty(PROPERTY_DESCRIPTOR_COLUMN, "");
		if (value.length() != 0) {
			int column = mTableModel.findColumn(value);
			if (column != -1 && qualifiesAsDescriptorColumn(column))
				mComboBoxDescriptorColumn.setSelectedItem(mTableModel.getColumnTitle(column));
			else if (!isInteractive())
				mComboBoxDescriptorColumn.setSelectedItem(value);
			else if (mComboBoxDescriptorColumn.getItemCount() != 0)
				mComboBoxDescriptorColumn.setSelectedIndex(0);
			}
		else if (!isInteractive()) {
			mComboBoxDescriptorColumn.setSelectedItem("Structure ["+DescriptorConstants.DESCRIPTOR_FFP512.shortName+"]");
			}
		}

	@Override
    public void setDialogConfigurationToDefault() {
		if (mComboBoxDescriptorColumn.getItemCount() != 0)
			mComboBoxDescriptorColumn.setSelectedIndex(0);
		else if (!isInteractive())
			mComboBoxDescriptorColumn.setSelectedItem("Structure ["+DescriptorConstants.DESCRIPTOR_FFP512.shortName+"]");

		mOptionButton1.setSelected(false);
		mTextField1.setEnabled(false);
		mTextField1.setText("");

		mOptionButton2.setSelected(true);
		mTextField2.setEnabled(true);
		mTextField2.setText("0.8");
		}

	@Override
    public Properties getDialogConfiguration() {
    	Properties configuration = new Properties();

    	if (mOptionButton1.isSelected() && mTextField1.getText().length() != 0)
    		configuration.setProperty(PROPERTY_CLUSTER_COUNT_LIMIT, mTextField1.getText());

    	if (mOptionButton2.isSelected() && mTextField2.getText().length() != 0)
    		configuration.setProperty(PROPERTY_SIMILARITY_LIMIT, mTextField2.getText());

    	String descriptorColumn = (String)mComboBoxDescriptorColumn.getSelectedItem();
    	if (descriptorColumn != null)
    		configuration.setProperty(PROPERTY_DESCRIPTOR_COLUMN, descriptorColumn);

    	return configuration;
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	private boolean qualifiesAsDescriptorColumn(int column) {
		return mTableModel.isDescriptorColumn(column);
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mOptionButton1) {
			mTextField1.setEnabled(mOptionButton1.isSelected());
			return;
			}
		if (e.getSource() == mOptionButton2) {
			mTextField2.setEnabled(mOptionButton2.isSelected());
			return;
			}
		}

	private int getStructureCount(int descriptorColumn) {
		int count = 0;
		for (int i=0; i<mTableModel.getTotalRowCount(); i++)
			if (mTableModel.getTotalRecord(i).getData(descriptorColumn) != null)
				count++;

		return count;
		}

	@Override
	public void runTask(Properties configuration) {
		int descriptorColumn = mTableModel.findColumn(configuration.getProperty(PROPERTY_DESCRIPTOR_COLUMN));
		waitForDescriptor(mTableModel, descriptorColumn);
		if (threadMustDie())
			return;

        int idcodeColumn = mTableModel.getParentColumn(descriptorColumn);

		int structureCount = getStructureCount(descriptorColumn);
		int[] originalIndex = new int[structureCount];
		Object[] descriptorList = new Object[structureCount];
		int count = 0;
		for (int i=0; i<mTableModel.getTotalRowCount(); i++) {
			Object descriptor = mTableModel.getTotalRecord(i).getData(descriptorColumn);
			if (descriptor != null) {
				descriptorList[count] = descriptor;
				originalIndex[count] = i;
				count++;
				}
			}

		double similarityLimit = Double.parseDouble(configuration.getProperty(PROPERTY_SIMILARITY_LIMIT, "0.0"));
		int clusterCountLimit = Integer.parseInt(configuration.getProperty(PROPERTY_CLUSTER_COUNT_LIMIT, "-1"));

		Clusterer clusterer = new Clusterer(mTableModel.getDescriptorHandler(descriptorColumn), descriptorList);
		clusterer.addProgressListener(this);
		clusterer.setThreadMaster(this);
		clusterer.cluster(similarityLimit, clusterCountLimit);

		if (!threadMustDie()) {
			clusterer.regenerateClusterNos();

			int firstNewColumn = mTableModel.addNewColumns(cClusterColumnName);
			for (int i=0; i<structureCount; i++) {
				mTableModel.setTotalValueAt(""+clusterer.getClusterNo(i), originalIndex[i], firstNewColumn);
				mTableModel.setTotalValueAt(clusterer.isRepresentative(i) ? "Yes" : "No", originalIndex[i], firstNewColumn+1);
				}

			mTableModel.setColumnProperty(firstNewColumn, CompoundTableModel.cColumnPropertyIsClusterNo, "true");
            mTableModel.setColumnProperty(firstNewColumn, CompoundTableModel.cColumnPropertyParentColumn, mTableModel.getColumnTitleNoAlias(idcodeColumn));
			mTableModel.finalizeNewColumns(firstNewColumn, this);
			}
		}
	}
