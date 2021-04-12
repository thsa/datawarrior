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

import com.actelion.research.chem.*;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableEvent;
import info.clearthought.layout.TableLayout;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

public class DETaskChemSpaceQuery extends DETaskStructureQuery {
	// ChemSpace API Documentation: https://api.chem-space.com/docs/

	static final long serialVersionUID = 0x20201106;

	public static final String TASK_NAME = "Search ChemSpace Chemicals";

	private static final String[] RESULT_COLUMNS = {"CS-id", "link", "smiles", "iupac_name", "cas", "mfcd"};
	private static final int IDENTIFIER_COLUMN = 0;
	private static final int SMILES_COLUMN = 2;

	private static final String PROPERTY_TARGET_DB = "targetDB";
	private static final String PROPERTY_MAX_PRICE = "maxPrice";
	private static final String PROPERTY_MIN_PACKAGE_SIZE = "minPackageSize";
	private static final String PROPERTY_MOLWEIGHT = "molweight";
	private static final String PROPERTY_MAX_ROWS = "maxRows";

	private static final String[] TARGET_DB_TEXT = {"in building blocks", "in screening compounds"};
	private static final String[] TARGET_DB_CODE = {"bb", "sc"};

	private static final String BASE_URL = "https://api.chem-space.com/v2/";
	private static final String TOKEN_URL = "https://api.chem-space.com/auth/token";

	private static final String URL_BB_SSS = BASE_URL + "search/smarts/bb/sub";
	private static final String URL_BB_SIM = BASE_URL + "search/smarts/bb/sim";
	private static final String URL_SC_SSS = BASE_URL + "search/smarts/sc/sub";
	private static final String URL_SC_SIM = BASE_URL + "search/smarts/sc/sim";

	private static final String AUTHORIZATION = "Bearer kJHNqvD71MT75E7pfuuUDsn4UKyl2yjLqK3N6Y_aqC0nYqfvyI9DynNMGLQi-RFS";
	private static final String ACCEPT = "application/json; version=2.7";
	private static final String CHARSET = "UTF-8";

	private JTextField  mTextFieldMaxRows;
	private String      mToken;
	private JComboBox   mComboBoxTargetDB;

	public DETaskChemSpaceQuery(DEFrame owner, DataWarrior application) {
		super(owner, application);
	}

	@Override
	public JPanel createDialogContent() {
		mToken = getToken();
		if (mToken == null)
			return null;

		JPanel panel = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap},
				{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.FILL, 2*gap,
						TableLayout.PREFERRED, gap} };
		panel.setLayout(new TableLayout(size));

		panel.add(createComboBoxSearchType(SEARCH_TYPES_SSS_SIM), "1,1");
		panel.add(createComboBoxTargetDB(), "1,3");
		panel.add(createStructureView(), "3,1,5,5");

		mTextFieldMaxRows = new JTextField(6);
		panel.add(new JLabel("Maximum row count:", JLabel.RIGHT), "1,7");
		panel.add(mTextFieldMaxRows, "3,7");

		mTextFieldMaxRows.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				super.keyTyped(e);
				mTextFieldMaxRows.setBackground(validateMaxRowsField(mTextFieldMaxRows.getText()) ? UIManager.getColor("TextArea.background") : Color.RED);
			}
		});

		return panel;
	}

	@Override
	protected JComponent createStructureView() {
		JComponent view = super.createStructureView();
		int preferred = HiDPIHelper.scale(160);
		view.setMinimumSize(new Dimension(preferred, preferred));
		view.setPreferredSize(new Dimension(preferred, preferred));
		return view;
	}

	protected JComponent createComboBoxTargetDB() {
		mComboBoxTargetDB = new JComboBox(TARGET_DB_TEXT);
		return mComboBoxTargetDB;
		}

	private boolean validateMaxRowsField(String value) {
		if (value.length() == 0)
			return true;

		try {
			int maxRows = Integer.parseInt(value);
			return (maxRows > 0 && maxRows <= 2000);
		}
		catch (NumberFormatException nfe) {
			return false;
		}
	}

	@Override
	public String getHelpURL() {
		return "/html/help/databases.html#ChemSpace";
		}

	@Override
	protected String getSQL() {
		return null;
	}

	@Override
	protected String[] getColumnNames() {
		return RESULT_COLUMNS;
		}

	@Override
	protected int getIdentifierColumn() {
		return IDENTIFIER_COLUMN;
	}

	@Override
	protected String getDocumentTitle()  {
		return "ChemSpace Query Result";
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();

		configuration.setProperty(PROPERTY_TARGET_DB, TARGET_DB_CODE[mComboBoxTargetDB.getSelectedIndex()]);

/*		if (mTextFieldMaxPrice.getText().length() != 0)
			configuration.setProperty(PROPERTY_MAX_PRICE, mTextFieldMaxPrice.getText());

		if (mTextFieldMinPackageSize.getText().length() != 0)
			configuration.setProperty(PROPERTY_MIN_PACKAGE_SIZE, mTextFieldMinPackageSize.getText());

		if (mTextFieldMolweight.getText().length() != 0)
			configuration.setProperty(PROPERTY_MOLWEIGHT, mTextFieldMolweight.getText());   */

		if (mTextFieldMaxRows.getText().length() != 0)
			configuration.setProperty(PROPERTY_MAX_ROWS, mTextFieldMaxRows.getText());

		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);

		mComboBoxTargetDB.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_TARGET_DB), TARGET_DB_CODE, 0));

/*		mTextFieldMaxPrice.setText(configuration.getProperty(PROPERTY_MAX_PRICE, ""));
		mTextFieldMinPackageSize.setText(configuration.getProperty(PROPERTY_MIN_PACKAGE_SIZE, ""));
		mTextFieldMolweight.setText(configuration.getProperty(PROPERTY_MOLWEIGHT, "")); */
		mTextFieldMaxRows.setText(configuration.getProperty(PROPERTY_MAX_ROWS, ""));
	}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		mComboBoxTargetDB.setSelectedIndex(0);
		mTextFieldMaxRows.setText("2000");
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		if (createSmilesOrSmarts(configuration) == null)
			return false;

		String maxRows = configuration.getProperty(PROPERTY_MAX_ROWS);
		if (maxRows != null && (!validateMaxRowsField(maxRows))) {
			showErrorMessage("Maximum row count must be between 1 and 2000.");
			return false;
			}

		return super.isConfigurationValid(configuration, isLive);
		}

	private String getToken() {
		String token = null;
		InputStreamReader reader = getTokenReader();
		if (reader != null) {
			JSONObject jo = new JSONObject(new JSONTokener(reader));
			token = jo.getString("access_token");
			if (token == null) {
				String msg = jo.getString("message");
				showErrorMessage(msg != null ? msg : "Unidentified error while trying to get access token.");
				}
			}
		return token;
		}

	private String createSmilesOrSmarts(Properties configuration) {
		String idcode = getQueryIDCode(configuration);
		if (idcode == null || new IDCodeParser().getAtomCount(idcode) == 0) {
			showErrorMessage("No query structure defined.");
			return null;
			}

		StereoMolecule mol = new IDCodeParser().getCompactMolecule(idcode);
		if (mol == null || mol.getAllAtoms() == 0) {
			showErrorMessage("Unexpected problem when parsing idcode.");
			return null;
			}

		mol.setFragment(false); // TODO remove and add query feature support to IsomericSmilesCreator
		return new IsomericSmilesCreator(mol).getSmiles();
		}

	@Override
	protected void retrieveRecords() throws Exception {
		mResultList = new ArrayList<>();

		Properties configuration = getTaskConfiguration();
		boolean isSSS = (SEARCH_TYPE_SSS == getSearchType(configuration));
		boolean isBB = configuration.getProperty(PROPERTY_TARGET_DB, "").equals(TARGET_DB_CODE[0]);

		String smiles = createSmilesOrSmarts(configuration);

		String url = isSSS ? (isBB ? URL_BB_SSS : URL_SC_SSS)
						   : (isBB ? URL_BB_SIM : URL_SC_SIM);

		String maxRows = configuration.getProperty(PROPERTY_MAX_ROWS);
		if (maxRows != null && maxRows.length() != 0 && validateMaxRowsField(maxRows))
			url = url.concat("?count="+maxRows);

		JSONObject result = null;
		try {
			BufferedReader reader = new BufferedReader(getSearchResultReader(url, smiles));

/*			System.out.println(url);    // print the result rather than process it
			String line = reader.readLine();
			while (line != null) {
				System.out.println(line);
				line = reader.readLine();
				}   */

			result = new JSONObject(new JSONTokener(reader));
			Integer count = (Integer)result.get("count");
			if (count != null && count != 0) {
				addResultRows(RESULT_COLUMNS, SMILES_COLUMN, (JSONArray)result.get("items"));
				}
			}
		catch (JSONException je) {
			showErrorMessage("Exception during result retrieval: "+je.getMessage());
			if (result != null)
				System.out.println(result.toString());
			return;
			}
		catch (IOException ioe) {
			showErrorMessage("Exception during result retrieval: "+ioe.getMessage());
			return;
			}

		if (mResultList.size() == 0) {
			showErrorMessage("Your ChemSpace search didn't return any result rows.");
			return;
			}
		}

	private String[] splitCells(String row) {
		ArrayList<String> cellList = new ArrayList<String>();
		int index1 = 0;
		while (index1 <= row.length()) {
			if (index1 == row.length()) {
				cellList.add("");
				break;
			}
			if (row.charAt(index1) == '"') {
				int index2 = row.indexOf('"', index1+1);
				if (index2 == -1) {	// we are missing a closing double quote
					cellList.add(row.substring(index1+1));
					break;
				}
				// don't accept closing double quotes if they are not followed by a comma
				// or they are not at the end of the row string
				while (index2 < row.length()-1 && row.charAt(index2+1) != ',') {
					index2 = row.indexOf('"', index2 + 1);
					if (index2 == -1) {	// we are missing a closing double quote
						cellList.add(row.substring(index1+1));
						break;
					}
				}
				cellList.add(row.substring(index1+1, index2));
				index1 = index2 + 2;
			} else {
				int index2 = row.indexOf(',', index1);
				if (index2 == -1) {	// no more comma, i.e. last cell entry
					cellList.add(row.substring(index1));
					break;
				}
				cellList.add(row.substring(index1, index2));
				index1 = index2+1;
			}
		}
		return cellList.toArray(new String[0]);
	}

	private JSONObject retrieveObject(String url, boolean isPost) {
		try {
			return new JSONObject(new JSONTokener(getSearchResultReader(url, null)));
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private JSONArray retrieveArray(String url) {
		try {
			return new JSONArray(new JSONTokener(getSearchResultReader(url, null)));
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private InputStreamReader getTokenReader() {
		HttpURLConnection connection = null;
		try {
			URL url = new URL(TOKEN_URL);
			connection = (HttpURLConnection)url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("User-Agent", "DataWarrior");
			connection.setRequestProperty("Accept", ACCEPT);
			connection.setRequestProperty("Authorization", AUTHORIZATION);
			return new InputStreamReader(connection.getInputStream());
			}
		catch (IOException ioe) {
			if (connection != null) {
				try {
					InputStream errorStream = connection.getErrorStream();
					if (errorStream != null)
						return new InputStreamReader(errorStream);
					}
				catch (Exception e) {}
				}

			showErrorMessage("Connection error while trying to get access token: "+ioe.getMessage());
			return null;
			}
		}

	private InputStreamReader getSearchResultReader(String url, String smiles) throws IOException {
		HttpsURLConnection connection = null;
		try {
			connection = (HttpsURLConnection) new URL(url).openConnection();
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setUseCaches(false);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("User-Agent", "Mozilla/5.0");
			connection.setRequestProperty("Accept", ACCEPT);
			connection.setRequestProperty("Authorization", "Bearer " + mToken);
//			connection.setRequestProperty("Connection", "Keep-Alive");
//			connection.setRequestProperty("Cache-Control", "no-cache");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");


			if (smiles != null) {
				OutputStream directOutput = connection.getOutputStream();
				PrintWriter body = new PrintWriter(new OutputStreamWriter(directOutput, CHARSET), true);
				body.append("SMILES=" + URLEncoder.encode(smiles, "UTF-8") + "\r\n");
				body.flush();
				}

			int responseCode = connection.getResponseCode();
			if (responseCode == 200)
				return new InputStreamReader(connection.getInputStream());
			else
				return new InputStreamReader(connection.getErrorStream());
			}
		catch (IOException ioe) {
			if (connection != null) {
				JSONObject jo = new JSONObject(new JSONTokener(connection.getErrorStream()));
				System.out.println(jo.toString());
				}
			throw ioe;
			}
		}

	/**
	 *
	 * @param keys
	 * @param smilesColumn if != -1, then add structure columns and generate their content from SMILES
	 * @param jsonArray
	 */
	private void addResultRows(String[] keys, int smilesColumn, JSONArray jsonArray) {
		int structureColumns = (smilesColumn == -1) ? 0 : getStructureColumnCount();
		StereoMolecule mol = (smilesColumn == -1) ? null : new StereoMolecule();
		Iterator<Object> rows = jsonArray.iterator();
		while (rows.hasNext()) {
			byte[][] resultRow = new byte[structureColumns+keys.length][];
			JSONObject row = (JSONObject)rows.next();
			for (int i=0; i<keys.length; i++) {
				if (smilesColumn != -1) {
					Object smiles = row.get(keys[smilesColumn]);
					if (smiles != null && (smiles instanceof String) && ((String)smiles).length() != 0) {
						try {
							new SmilesParser().parse(mol, (String)smiles);
							Canonizer canonizer = new Canonizer(mol);
							resultRow[0] = canonizer.getIDCode().getBytes();
							resultRow[1] = canonizer.getEncodedCoordinates().getBytes();
							}
						catch (Exception e) {}
						}
					}
				Object object = row.get(keys[i]);
				if (object != null && object instanceof String)
					resultRow[structureColumns + i] = ((String)object).getBytes();
				}
			mResultList.add(resultRow);
			}
		}

	@Override
	protected int getRuntimePropertiesMode() {
		return CompoundTableEvent.cSpecifierDefaultFilters;
	}
}
