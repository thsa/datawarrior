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
import com.actelion.research.datawarrior.DERuntimeProperties;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;
import org.openmolecules.cod.CODServerConstants;

import javax.swing.*;
import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TreeMap;

public class DETaskCODQuery extends DETaskStructureQuery implements CODServerConstants {
	static final long serialVersionUID = 0x20061004;

	public static final String TASK_NAME = "Search Crystallographic Open Database";
	private static final String PROPERTY_ORGANIC_ONLY = "organicOnly";
	private static final String PROPERTY_AUTHOR = "author";
	private static final String PROPERTY_YEARS = "years";

	private JCheckBox	mCheckBoxOrganicOnly;
	private JTextField	mTextFieldAuthor,mTextFieldYears;
	private String[]	mColumnTitle;
	private byte[]      mTemplate;

	public DETaskCODQuery(DEFrame owner, DataWarrior application) {
		super(owner, application);
		}

	@Override
	public JPanel createDialogContent() {
		JPanel panel = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap},
							{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap*2, TableLayout.PREFERRED, gap*2,
								TableLayout.PREFERRED, gap/2,TableLayout.PREFERRED, gap} };
		panel.setLayout(new TableLayout(size));

		panel.add(createComboBoxSearchType(SEARCH_TYPES_SSS_SIM_EXACT_NOSTEREO_TAUTO), "1,1");
		panel.add(createComboBoxQuerySource(), "3,1");
		panel.add(createSimilaritySlider(), "1,3");
		panel.add(createStructureView(), "3,3");

		mCheckBoxOrganicOnly = new JCheckBox("Organic compounds only");
		mCheckBoxOrganicOnly.setHorizontalAlignment(SwingConstants.CENTER);
		panel.add(mCheckBoxOrganicOnly, "1,5,3,5");

		mTextFieldAuthor = new JTextField(8);
		JPanel authorPanel = new JPanel();
		authorPanel.add(new JLabel("Author field contains:"));
		authorPanel.add(mTextFieldAuthor);
		authorPanel.add(new JLabel(" (e.g. 'smith')"));
		panel.add(authorPanel, "1,7,3,7");

		mTextFieldYears = new JTextField(6);
		mTextFieldYears.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				super.keyTyped(e);
				mTextFieldYears.setBackground(validateFieldYears(mTextFieldYears.getText()) ? UIManager.getColor("TextArea.background") : Color.RED);
			}
		});
		JPanel yearPanel = new JPanel();
		yearPanel.add(new JLabel("Publication year(s):"));
		yearPanel.add(mTextFieldYears);
		yearPanel.add(new JLabel(" (e.g. '1992', '>2010', '2000-2005')"));
		panel.add(yearPanel, "1,9,3,9");

		return panel;
		}

	private boolean validateFieldYears(String years) {
		try {
			switch (years.length()) {
				case 0:
					return true;
				case 4:
					Integer.parseInt(years);
					return true;
				case 5:
					if (years.charAt(0) != '<' && years.charAt(0) != '>')
						return false;
					Integer.parseInt(years.substring(1));
					return true;
				case 6:
					if ((years.charAt(0) != '<' && years.charAt(0) != '>') || years.charAt(1) != '=')
						return false;
					Integer.parseInt(years.substring(2));
					return true;
				case 9:
					if (years.charAt(4) != '-')
						return false;
					Integer.parseInt(years.substring(0, 4));
					Integer.parseInt(years.substring(5));
					return true;
				default:
					return false;
				}
			}
		catch (NumberFormatException nfe) {
			return false;
			}
		}

	@Override
	public String getHelpURL() {
		return "/html/help/databases.html#COD";
		}

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
		return "Data From Crystallography Open Database";
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();

		configuration.setProperty(PROPERTY_ORGANIC_ONLY, mCheckBoxOrganicOnly.isSelected() ? "true" : "false");

		// for COD we use always SkeletonSpheres rather than the default FFP512
		configuration.setProperty(PROPERTY_DESCRIPTOR_NAME, DescriptorConstants.DESCRIPTOR_SkeletonSpheres.shortName);

		if (mTextFieldAuthor.getText().length() != 0)
			configuration.setProperty(PROPERTY_AUTHOR, mTextFieldAuthor.getText());

		if (mTextFieldYears.getText().length() != 0)
			configuration.setProperty(PROPERTY_YEARS, mTextFieldYears.getText());

		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);

		String group = configuration.getProperty(PROPERTY_ORGANIC_ONLY);
		mCheckBoxOrganicOnly.setSelected(group != null && group.equals("true"));

		mTextFieldAuthor.setText(configuration.getProperty(PROPERTY_AUTHOR, ""));
		mTextFieldYears.setText(configuration.getProperty(PROPERTY_YEARS, ""));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();

		mCheckBoxOrganicOnly.setSelected(true);
		mTextFieldAuthor.setText("");
		mTextFieldYears.setText("");
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
		String years = configuration.getProperty(PROPERTY_YEARS);
		if (years != null && !validateFieldYears(years)) {
			showErrorMessage("Invalidate 'years' condition.");
			return false;
			}
		String author = configuration.getProperty(PROPERTY_AUTHOR);
		if (ssSpec == null && author == null && years == null) {
			showErrorMessage("Neither structure search nor other query conditions defined.");
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

//		query.put(QUERY_ORGANIC_ONLY, new Boolean(!"false".equals(getTaskConfiguration().getProperty(PROPERTY_ORGANIC_ONLY))));
		query.put(QUERY_ORGANIC_ONLY, !"false".equals(getTaskConfiguration().getProperty(PROPERTY_ORGANIC_ONLY)));

		String years = configuration.getProperty(PROPERTY_YEARS);
		if (years != null)
			query.put(QUERY_YEAR, years);

		String author = configuration.getProperty(PROPERTY_AUTHOR);
		if (author != null)
			query.put(QUERY_AUTHOR, author);

		mResultList = new ArrayList<Object[]>();
   		byte[][][] resultTable = new CODCommunicator(this, "datawarrior").search(query);
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
			mTemplate = new CODCommunicator(this, "datawarrior").getTemplate();
			}

		DELogWriter.writeEntry("retrieveCOD", "records:"+mResultList.size());
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
		tableModel.setColumnProperty(RESULT_COLUMN_IDCODE, CompoundTableConstants.cColumnPropertyRelatedIdentifierColumn, RESULT_COLUMN_NAME_COD_NO);
		tableModel.setColumnName(CompoundTableConstants.cColumnType3DCoordinates, RESULT_COLUMN_COORDS3D);
		tableModel.setColumnProperty(RESULT_COLUMN_COORDS3D, CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnType3DCoordinates);
		tableModel.setColumnProperty(RESULT_COLUMN_COORDS3D, CompoundTableConstants.cColumnPropertyParentColumn, "Structure");

		for (int i=0; i<mColumnTitle.length; i++) {
			if ("PUBCHEM_EXT_DATASOURCE_REGID".equals(mColumnTitle[i])) {
				int column = RESULT_STRUCTURE_COLUMNS+i;
				tableModel.setColumnProperty(column, CompoundTableModel.cColumnPropertyLookupCount, "1");
				tableModel.setColumnProperty(column, CompoundTableModel.cColumnPropertyLookupName + "0", "COD-Page");
				tableModel.setColumnProperty(column, CompoundTableModel.cColumnPropertyLookupURL + "0", "http://www.crystallography.net/cod/%s.html");
				break;
				}
			}
		}

	@Override
    protected int getRuntimePropertiesMode() {
    	return CompoundTableEvent.cSpecifierNoRuntimeProperties;
    	}

	@Override
    protected void setRuntimeProperties() {
		if (mTemplate != null) {
			DERuntimeProperties rtp = new DERuntimeProperties(mTargetFrame.getMainFrame());
			try {
				rtp.read(new BufferedReader(new StringReader(new String(mTemplate))));
				rtp.apply();
				}
			catch (IOException ioe) {}
			}
        }
	}
