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

package com.actelion.research.datawarrior.task.db;

import com.actelion.research.chem.StructureSearchSpecification;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorHandlerLongFFP512;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DELogWriter;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;
import org.openmolecules.bb.BBServerConstants;

import javax.swing.*;
import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TreeMap;

public class DETaskMolportQuery extends DETaskStructureQuery implements BBServerConstants {
	static final long serialVersionUID = 0x20191204;

	public static final String TASK_NAME = "Search Molport Building Blocks";
	private static final String PROPERTY_VERIFIED_AMOUNT_ONLY = "varifiedAmount";
	private static final String PROPERTY_MAX_PRICE = "maxPrice";
	private static final String PROPERTY_MIN_PACKAGE_SIZE = "minPackageSize";
	private static final String PROPERTY_MOLWEIGHT = "molweight";
	private static final String PROPERTY_MAX_ROWS = "maxRows";

	private JCheckBox mCheckBoxVerifiedAmountOnly;
	private JTextField  mTextFieldMaxPrice,mTextFieldMinPackageSize,mTextFieldMolweight,mTextFieldMaxRows;
	private String[]	mColumnTitle;

	public DETaskMolportQuery(DEFrame owner, DataWarrior application) {
		super(owner, application);
		}

	@Override
	public JPanel createDialogContent() {
		JPanel panel = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap*2, TableLayout.PREFERRED, gap,
								TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };
		panel.setLayout(new TableLayout(size));

		panel.add(createComboBoxSearchType(SEARCH_TYPES_SSS_SIM_EXACT_NOSTEREO_TAUTO), "1,1");
		panel.add(createComboBoxQuerySource(), "3,1");
		panel.add(createSimilaritySlider(), "1,3");
		panel.add(createStructureView(), "3,3");

		mTextFieldMaxPrice = new JTextField(4);
		JPanel pricePanel = new JPanel();
		pricePanel.add(new JLabel("Maximum price:"));
		pricePanel.add(mTextFieldMaxPrice);
		pricePanel.add(new JLabel(" EUR"));
		panel.add(pricePanel, "1,5");

		mTextFieldMinPackageSize = new JTextField(4);
		JPanel sizePanel = new JPanel();
		sizePanel.add(new JLabel("Minimum package size:"));
		sizePanel.add(mTextFieldMinPackageSize);
		sizePanel.add(new JLabel(" mg"));
		panel.add(sizePanel, "1,7");

		mTextFieldMolweight = new JTextField(6);
		JPanel mwPanel = new JPanel();
		mwPanel.add(new JLabel("Molweight:"));
		mwPanel.add(mTextFieldMolweight);
		mwPanel.add(new JLabel(" e.g. '<300', '120-180'"));
		panel.add(mwPanel, "3,5");

		mTextFieldMaxRows = new JTextField(6);
		JPanel maxrowsPanel = new JPanel();
		maxrowsPanel.add(new JLabel("Maximum row count:"));
		maxrowsPanel.add(mTextFieldMaxRows);
		panel.add(maxrowsPanel, "3,7");

		mTextFieldMaxPrice.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				super.keyTyped(e);
				mTextFieldMaxPrice.setBackground(validateDoubleField(mTextFieldMaxPrice.getText()) ? UIManager.getColor("TextArea.background") : Color.RED);
				}
			});
		mTextFieldMinPackageSize.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				super.keyTyped(e);
				mTextFieldMinPackageSize.setBackground(validateDoubleField(mTextFieldMinPackageSize.getText()) ? UIManager.getColor("TextArea.background") : Color.RED);
			}
		});
		mTextFieldMolweight.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				super.keyTyped(e);
				mTextFieldMolweight.setBackground(validateFieldMolweight(mTextFieldMolweight.getText()) ? UIManager.getColor("TextArea.background") : Color.RED);
			}
		});
		mTextFieldMaxRows.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				super.keyTyped(e);
				mTextFieldMaxRows.setBackground(validateIntField(mTextFieldMaxRows.getText()) ? UIManager.getColor("TextArea.background") : Color.RED);
			}
		});

		mCheckBoxVerifiedAmountOnly = new JCheckBox("Verified amounts only");
		mCheckBoxVerifiedAmountOnly.setHorizontalAlignment(SwingConstants.CENTER);
		panel.add(mCheckBoxVerifiedAmountOnly, "1,9,3,9");

		return panel;
		}

	private boolean validateDoubleField(String value) {
		if (value.length() == 0)
			return true;

		try {
			return (Double.parseDouble(value) > 0);
			}
		catch (NumberFormatException nfe) {
			return false;
			}
		}

	private boolean validateIntField(String value) {
		if (value.length() == 0)
			return true;

		try {
			return (Integer.parseInt(value) > 0);
			}
		catch (NumberFormatException nfe) {
			return false;
		}
	}

	private boolean validateFieldMolweight(String molweight) {
		if (molweight.length() == 0)
			return true;

		try {
			Double.parseDouble(molweight);
			return true;
			}
		catch (NumberFormatException nfe) {}

		if (molweight.charAt(0) == '<' || molweight.charAt(0) == '>') {
			try {
				int index = (molweight.length() > 1 && molweight.charAt(1) == '=') ? 2 : 1;
				Double.parseDouble(molweight.substring(index));
				return true;
				}
			catch (NumberFormatException nfe) {
				return false;
				}
			}

		int index = molweight.indexOf('-');
		if (index <= 0 || index == molweight.length()-1)
			return false;

		try {
			Integer.parseInt(molweight.substring(0, index));
			Integer.parseInt(molweight.substring(index+1));
			return true;
			}
		catch (NumberFormatException nfe) {
			return false;
			}
		}

//	@Override
//	public String getHelpURL() {
//		return "/html/help/databases.html#Molport";
//		}

	@Override
	protected String getSQL() {
		return null;
		}

	@Override
	protected String[] getColumnNames() {
		// return names without structure related columns
		return mColumnTitle;
		}

	@Override
	protected int getIdentifierColumn() {
		return 0;
		}

	@Override
	protected String getDocumentTitle()  {
		return "Molport Building Blocks";
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();

		// for Molport we use always SkeletonSpheres rather than the default FFP512
		configuration.setProperty(PROPERTY_DESCRIPTOR_NAME, DescriptorConstants.DESCRIPTOR_SkeletonSpheres.shortName);

		if (mTextFieldMaxPrice.getText().length() != 0)
			configuration.setProperty(PROPERTY_MAX_PRICE, mTextFieldMaxPrice.getText());

		if (mTextFieldMinPackageSize.getText().length() != 0)
			configuration.setProperty(PROPERTY_MIN_PACKAGE_SIZE, mTextFieldMinPackageSize.getText());

		if (mTextFieldMolweight.getText().length() != 0)
			configuration.setProperty(PROPERTY_MOLWEIGHT, mTextFieldMolweight.getText());

		if (mTextFieldMaxRows.getText().length() != 0)
			configuration.setProperty(PROPERTY_MAX_ROWS, mTextFieldMaxRows.getText());

		configuration.setProperty(PROPERTY_VERIFIED_AMOUNT_ONLY, mCheckBoxVerifiedAmountOnly.isSelected() ? "true" : "false");

		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);

		String vOnly = configuration.getProperty(PROPERTY_VERIFIED_AMOUNT_ONLY);
		mCheckBoxVerifiedAmountOnly.setSelected(vOnly != null && vOnly.equals("true"));

		mTextFieldMaxPrice.setText(configuration.getProperty(PROPERTY_MAX_PRICE, ""));
		mTextFieldMinPackageSize.setText(configuration.getProperty(PROPERTY_MIN_PACKAGE_SIZE, ""));
		mTextFieldMolweight.setText(configuration.getProperty(PROPERTY_MOLWEIGHT, ""));
		mTextFieldMaxRows.setText(configuration.getProperty(PROPERTY_MAX_ROWS, ""));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();

		mTextFieldMaxPrice.setText("100");
		mTextFieldMinPackageSize.setText("10");
		mTextFieldMolweight.setText("<300");
		mTextFieldMaxRows.setText("10000");
		mCheckBoxVerifiedAmountOnly.setSelected(true);
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		StructureSearchSpecification ssSpec = getStructureSearchSpecification(configuration);
		if (ssSpec != null) {
			String errorMessage = ssSpec.validate();
			if (errorMessage != null) {
				showErrorMessage(errorMessage);
				return false;
				}
			}
		String minSize = configuration.getProperty(PROPERTY_MIN_PACKAGE_SIZE);
		if (minSize != null && !validateDoubleField(minSize)) {
			showErrorMessage("Invalidate minimum package size.");
			return false;
			}
		String maxPrice = configuration.getProperty(PROPERTY_MAX_PRICE);
		if (maxPrice != null && !validateDoubleField(maxPrice)) {
			showErrorMessage("Invalidate maximum price.");
			return false;
			}
		String molweight = configuration.getProperty(PROPERTY_MOLWEIGHT);
		if (molweight != null && !validateFieldMolweight(molweight)) {
			showErrorMessage("Invalidate molweight definition.");
			return false;
			}
		String maxRows = configuration.getProperty(PROPERTY_MAX_ROWS);
		if (maxRows != null && !validateIntField(maxRows)) {
			showErrorMessage("Invalidate maximum row count.");
			return false;
			}

		return super.isConfigurationValid(configuration, isLive);
		}

	@Override
	protected void retrieveRecords() throws Exception {
		TreeMap<String,Object> query = new TreeMap<String,Object>();

		Properties configuration = getTaskConfiguration();
		StructureSearchSpecification ssSpec = getStructureSearchSpecification(configuration);
   		if (ssSpec != null)
   			query.put(QUERY_STRUCTURE_SEARCH_SPEC, ssSpec);

		String minAmount = getTaskConfiguration().getProperty(PROPERTY_MIN_PACKAGE_SIZE);
		if (minAmount != null) {
			if ("true".equals(getTaskConfiguration().getProperty(PROPERTY_VERIFIED_AMOUNT_ONLY)))
				query.put(QUERY_VERIFIED_AMOUNT, minAmount);
			else
				query.put(QUERY_AMOUNT, minAmount);
			}

		String price = configuration.getProperty(PROPERTY_MAX_PRICE);
		if (price != null)
			query.put(QUERY_PRICE_LIMIT, price);

		String molweight = configuration.getProperty(PROPERTY_MOLWEIGHT);
		if (molweight != null)
			query.put(QUERY_MOLWEIGHT, molweight);

		String maxRows = configuration.getProperty(PROPERTY_MAX_ROWS);
		if (maxRows != null)
			query.put(QUERY_MAX_ROWS, maxRows);

		mResultList = new ArrayList<>();
   		byte[][][] resultTable = new MolportCommunicator(this, "datawarrior").search(query);
		if (resultTable != null) {
			mColumnTitle = new String[resultTable[0].length-RESULT_STRUCTURE_COLUMNS];	// title without structure related columns
			for (int col=RESULT_STRUCTURE_COLUMNS; col<resultTable[0].length; col++)
				mColumnTitle[col-RESULT_STRUCTURE_COLUMNS] = new String(resultTable[0][col]);
			for (int r=1; r<resultTable.length; r++) {
				byte[][] resultLine = resultTable[r];
				Object[] row = new Object[resultLine.length];
				for (int i=0; i<resultLine.length; i++)
					row[i] = resultLine[i];
				row[RESULT_COLUMN_FFP512] = DescriptorHandlerLongFFP512.getDefaultInstance().decode((byte[])row[RESULT_COLUMN_FFP512]);
				mResultList.add(row);
				}
			}

		DELogWriter.writeEntry("retrieveMolport", "records:"+mResultList.size());
		}

	@Override
	protected int getStructureColumnCount() {
		return RESULT_STRUCTURE_COLUMNS;
		}

	@Override
	protected int getFragFpColumn() {
		return RESULT_COLUMN_FFP512;
		}

	@Override
	protected void prepareStructureColumns(CompoundTableModel tableModel) {
		tableModel.prepareStructureColumns(RESULT_COLUMN_IDCODE, "Structure", true, true);
		tableModel.setColumnProperty(RESULT_COLUMN_IDCODE, CompoundTableConstants.cColumnPropertyRelatedIdentifierColumn, RESULT_COLUMN_NAME_BB_NO);
		}

	@Override
    protected int getRuntimePropertiesMode() {
    	return CompoundTableEvent.cSpecifierDefaultFilters;
    	}
	}
