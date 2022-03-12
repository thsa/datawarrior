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

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.SmilesParser;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.JLoginDialog;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableEvent;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TreeMap;

import static com.actelion.research.chem.io.CompoundTableConstants.NEWLINE_REGEX;
import static com.actelion.research.chem.io.CompoundTableConstants.NEWLINE_STRING;

/**
 * Title:		DETaskSQLQuery
 * Description:	Retrieves the query results of an SQL statement
 * Copyright:	Copyright (c) 2005-2018
 * Company:		Idorsia Ltd.
 * @author		Thomas Sander
 */

public class DETaskSQLQuery extends ConfigurableTask implements ItemListener {
	public static final String TASK_NAME = "Run SQL Query";
	public static final String PROPERTY_DATABASE = "database";
	public static final String PROPERTY_CONNECT_STRING = "connect";
	public static final String PROPERTY_SQL = "sql";

	private static final String PREFS_KEY_SQL = "datawarriorSQL";
	private static final String PREFS_KEY_CONNECT_STRING = "datawarriorRecentConnectString";
	private static final String ITEM_EXPLICIT = "<Explicit connect string>";

	private static final int MAX_COLUMNS_FOR_SMILES_CHECK = 64;
	private static final int MAX_ROWS_FOR_SMILES_CHECK = 16;
	private static final int MAX_TOLERATED_SMILES_FAILURES = 4;
	private static final String STRUCTURE_COLUMN_NAME_START ="Structure of ";

	private static JLoginDialog sLoginDialog;
	private static boolean	sOracleDriverRegistered,sMySQLDriverRegistered,sPostgreSQLDriverRegistered,sSQLServerDriverRegistered,sMSAccessDriverRegistered;
	private static TreeMap<String,DatabaseSpec> sKnownDatabaseMap;	// map from database name to connect string
	private static TreeMap<String,Connection> sConnectionCache;	// map from connect string to connection

	private DEFrame			mTargetFrame;
	private DataWarrior		mApplication;
	private JComboBox		mComboBoxDatabase;
	private JTextArea		mTextAreaSQL;
	private JTextField		mTextFieldConnectString;

	/**
	 * When calling this method before actually using this task, then the dialog will show
	 * the database names passed in this method for easy selection and will use the associated
	 * connect strings to access these databases later.
	 * @param spec
	 */
	public static void addKnownDatabase(final String[] spec, final SQLConnector connector) {
		if (sKnownDatabaseMap == null)
			sKnownDatabaseMap = new TreeMap<>();

		sKnownDatabaseMap.put(spec[0], new DatabaseSpec(spec, connector));
		}

	public DETaskSQLQuery(Frame owner, DataWarrior application) {
		super(owner, true);
		mApplication = application;
		}

	@Override
	public void doOKAction() {
		String sql = mTextAreaSQL.getText();
		if (sql.length() != 0)
			DataWarrior.getPreferences().put(PREFS_KEY_SQL, sql);

		super.doOKAction();
   		}

	public Connection getConnection(Frame owner, String databaseName) {
		if (sKnownDatabaseMap == null)
			return null;

		DatabaseSpec spec = sKnownDatabaseMap.get(databaseName);
		if (spec == null)
			return null;

		if (sConnectionCache == null)
			sConnectionCache = new TreeMap<>();

		Connection connection = sConnectionCache.get(spec.connectString);
		if (connection == null)
			openConnection(owner, spec.connectString, spec.user, spec.password, spec.connector);

		return sConnectionCache.get(spec.connectString);
	 	}

	private void openConnectionUsingConnector(SQLConnector connector, Frame owner, String connectString) {
		Connection connection = connector.connect(owner);
		if (connection != null) {
			if (sConnectionCache == null)
				sConnectionCache = new TreeMap<>();
			sConnectionCache.put(connectString, connection);
			}
		}

	private void openConnection(String connectString, String user, String password) {
		Connection connection = null;

		if (connectString.startsWith("jdbc:"))
			connectString = connectString.substring(5);

		if (connectString.startsWith("oracle:")) {
			if (registerOracleDriver()) {
				try {
					java.util.Properties props = new java.util.Properties();
					props.put("v$session.program", "DataWarrior");
					props.put("user", user);
					props.put("password", password);
					connection = DriverManager.getConnection("jdbc:"+connectString, props);
					connection.setAutoCommit(false);
					}
				catch (Exception ex) {
					ex.printStackTrace();
					showErrorMessage(ex.getMessage());
					}
				}
			}

		if (connectString.startsWith("mysql:")) {
			try {
				registerMySQLDriver();
				connection = DriverManager.getConnection("jdbc:"+connectString, user, password);
				}
			catch (Exception ex) {
				ex.printStackTrace();
				showErrorMessage(ex.getMessage());
				}
			}

		if (connectString.startsWith("postgresql:")) {
			try {
				registerPostgreSQLDriver();
				connection = DriverManager.getConnection("jdbc:"+connectString, user, password);
			}
			catch (Exception ex) {
				ex.printStackTrace();
				showErrorMessage(ex.getMessage());
			}
		}

		if (connectString.startsWith("sqlserver:")) {
			try {
				registerSQLServerDriver();
				connection = DriverManager.getConnection("jdbc:"+connectString, user, password);
			}
			catch (Exception ex) {
				ex.printStackTrace();
				showErrorMessage(ex.getMessage());
			}
		}

		if (connectString.startsWith("ucanaccess:")) {
			try {
				registerMSAccessDriver();
				connection = DriverManager.getConnection("jdbc:"+connectString, user, password);
			}
			catch (Exception ex) {
				ex.printStackTrace();
				showErrorMessage(ex.getMessage());
			}
		}

		if (connection != null) {
			if (sConnectionCache == null)
				sConnectionCache = new TreeMap<>();
			sConnectionCache.put(connectString, connection);
			}
		}

	/**
	 *
	 * @param owner
	 * @param connectString expected to be valid
	 * @param user may be null
	 * @param password may be null
	 */
	private void openConnection(Frame owner, String connectString, String user, String password, SQLConnector connector) {
		if (connector != null) {
			openConnectionUsingConnector(connector, owner, connectString);
			}
		else if (user != null && user.length() != 0 && password != null) {
			openConnection(connectString, user, password);
			}
		else {
			try {
				SwingUtilities.invokeAndWait(() -> {
					sLoginDialog = new JLoginDialog(owner, e -> {
							if (e.getActionCommand().equals(JLoginDialog.cLoginOK)) {
								try {
									String _user = sLoginDialog.getUserID();
									String _password = sLoginDialog.getPassword();
									if (_user.length() == 0)
										return;

									openConnection(connectString, _user, _password);
									}
								catch (Exception ex) {
									ex.printStackTrace();
									JOptionPane.showMessageDialog(owner, ex);
									}
								}

							if (e.getActionCommand().equals(JLoginDialog.cLoginOK)
							 || e.getActionCommand().equals(JLoginDialog.cLoginCancel)) {
								sLoginDialog.setVisible(false);
								sLoginDialog.dispose();
								return;
								}
						});
					sLoginDialog.setVisible(true);
					});
				}
			catch (Exception e) {
				e.printStackTrace();
				}
			}
		}

	private boolean registerOracleDriver() {
		if (!sOracleDriverRegistered) {
			try {
				Class.forName("oracle.jdbc.driver.OracleDriver"); // instead of  DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
				sOracleDriverRegistered = true;
				}
			catch (Exception ex) {
				ex.printStackTrace();
				showErrorMessage(ex.getMessage());
				return false;
				}
			}
		return sOracleDriverRegistered;
		}

	private boolean registerMySQLDriver() {
		if (!sMySQLDriverRegistered) {
			try {
				Class.forName("com.mysql.jdbc.Driver").newInstance();
				sMySQLDriverRegistered = true;
				}
			catch (Exception ex) {
				ex.printStackTrace();
				showErrorMessage(ex.getMessage());
				}
			}
		return sMySQLDriverRegistered;
		}

	private boolean registerPostgreSQLDriver() {
		if (!sPostgreSQLDriverRegistered) {
			try {
				Class.forName("org.postgresql.Driver").newInstance();
				sPostgreSQLDriverRegistered = true;
			}
			catch (Exception ex) {
				ex.printStackTrace();
				showErrorMessage(ex.getMessage());
			}
		}
		return sPostgreSQLDriverRegistered;
	}

	private boolean registerSQLServerDriver() {
		if (!sSQLServerDriverRegistered) {
			try {
				Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver").newInstance();
				sSQLServerDriverRegistered = true;
			}
			catch (Exception ex) {
				ex.printStackTrace();
				showErrorMessage(ex.getMessage());
			}
		}
		return sSQLServerDriverRegistered;
	}

	private boolean registerMSAccessDriver() {
		if (!sMSAccessDriverRegistered) {
			try {
				Class.forName("net.ucanaccess.jdbc.UcanaccessDriver").newInstance();
				sMSAccessDriverRegistered = true;
			}
			catch (Exception ex) {
				ex.printStackTrace();
				showErrorMessage(ex.getMessage());
			}
		}
		return sMSAccessDriverRegistered;
	}

	@Override
	public boolean isConfigurable() {
		return true;
	 	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public String getHelpURL() {
		return "/html/help/databases.html#SQLQuery";
		}

	@Override
	public JComponent createDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.FILL, gap},
							{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED,
							 gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));
		
		mTextAreaSQL = new JTextArea(6, 64);
		mTextAreaSQL.setLineWrap(true);
		mTextAreaSQL.setWrapStyleWord(true);
		content.add(new JLabel("SQL-Statement:"), "1,1");
		content.add(mTextAreaSQL, "1,3,5,3");
		String sql = DataWarrior.getPreferences().get(PREFS_KEY_SQL, null);
		if (sql != null)
			mTextAreaSQL.setText(sql);

		if (sKnownDatabaseMap != null) {
			mComboBoxDatabase = new JComboBox();
			for (String databaseName:sKnownDatabaseMap.keySet())
				mComboBoxDatabase.addItem(databaseName);
			mComboBoxDatabase.addItem(ITEM_EXPLICIT);
			mComboBoxDatabase.addItemListener(this);
			content.add(new JLabel("Database name:"), "1,5");
			content.add(mComboBoxDatabase, "3,5");
			}

		mTextFieldConnectString = new JTextField();
		content.add(new JLabel("Connect string:"), "1,7");
		content.add(mTextFieldConnectString, "3,7,5,7");

		content.add(new JLabel("Examples:"), "1,9");
		content.add(new JLabel("mysql://other.server.com/test_db"), "3,9,5,9");
		content.add(new JLabel("postgresql://some.server.com/test_db"), "3,11,5,11");
		content.add(new JLabel("oracle:thin:@some.server.com:1521:my_sid"), "3,13,5,13");
		content.add(new JLabel("sqlserver://some.server.com:1433;databaseName=test_db"), "3,15,5,15");
		try {
			Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
			content.add(new JLabel("ucanaccess://c:/sample.mdb;memory=true"), "3,17,5,17");
			}
		catch (ClassNotFoundException cnfe) {}

		updateConnectStringTextField();

		return content;
		}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == mComboBoxDatabase && e.getStateChange() == ItemEvent.SELECTED) {
			updateConnectStringTextField();
			}
		}

	private void updateConnectStringTextField() {
		if (mComboBoxDatabase == null || ITEM_EXPLICIT.equals(mComboBoxDatabase.getSelectedItem())) {
			mTextFieldConnectString.setEnabled(true);
			String explicitConnectString = DataWarrior.getPreferences().get(PREFS_KEY_CONNECT_STRING, null);
			if (explicitConnectString != null)
				mTextFieldConnectString.setText(explicitConnectString);
			else
				mTextFieldConnectString.setText("");
			}
		else {
			mTextFieldConnectString.setEnabled(false);
			if (mTextFieldConnectString.getText().length() != 0)
				DataWarrior.getPreferences().put(PREFS_KEY_CONNECT_STRING, mTextFieldConnectString.getText());
			mTextFieldConnectString.setText(sKnownDatabaseMap.get(mComboBoxDatabase.getSelectedItem()).connectString);
			}
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		String databaseName = (mComboBoxDatabase == null) ? null : (String)mComboBoxDatabase.getSelectedItem();
		if (databaseName == null || ITEM_EXPLICIT.equals(databaseName))
			configuration.setProperty(PROPERTY_CONNECT_STRING, normalizeConnectString(mTextFieldConnectString.getText()));
		else
			configuration.setProperty(PROPERTY_DATABASE, databaseName);

		configuration.setProperty(PROPERTY_SQL, mTextAreaSQL.getText().replaceAll(NEWLINE_REGEX, NEWLINE_STRING));

		return configuration;
		}

	private String normalizeConnectString(String connectString) {
		return connectString.toLowerCase().startsWith("jdbc:") ? connectString.substring(5) : connectString;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		if (mComboBoxDatabase != null) {
			String databaseName = configuration.getProperty(PROPERTY_DATABASE);
			if (databaseName != null)
				mComboBoxDatabase.setSelectedItem(databaseName);
			else
				mComboBoxDatabase.setSelectedItem(ITEM_EXPLICIT);
			}
		mTextFieldConnectString.setText(configuration.getProperty(PROPERTY_CONNECT_STRING, ""));
		mTextAreaSQL.setText(configuration.getProperty(PROPERTY_SQL, "").replace(NEWLINE_STRING, "\n"));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mComboBoxDatabase != null)
			mComboBoxDatabase.setSelectedIndex(0);
		String sql = DataWarrior.getPreferences().get(PREFS_KEY_SQL, "");
		mTextAreaSQL.setText(sql);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String databaseName = configuration.getProperty(PROPERTY_DATABASE);
		if (databaseName != null && (sKnownDatabaseMap == null || sKnownDatabaseMap.get(databaseName) == null)) {
			showErrorMessage("Database '"+databaseName+"' is not supported");
			return false;
			}
		if (databaseName == null && configuration.getProperty(PROPERTY_CONNECT_STRING, "").length() == 0) {
			showErrorMessage("No connect string defined.");
			return false;
			}
		if (configuration.getProperty(PROPERTY_SQL, "").length() == 0) {
			showErrorMessage("No SQL-query defined.");
			return false;
			}
		return true;	// assuming that the connect string and sql syntax is OK
		}

	@Override
	public void runTask(Properties configuration) {
		String databaseName = configuration.getProperty(PROPERTY_DATABASE);
		DatabaseSpec spec = (databaseName == null) ? null : sKnownDatabaseMap.get(databaseName);
		String connectString = configuration.getProperty(PROPERTY_CONNECT_STRING, "");

		if (spec != null)
			openConnection(getParentFrame(), spec.connectString, spec.user, spec.password, spec.connector);
		else
			openConnection(getParentFrame(), connectString, null, null, null);

		Connection connection = (sConnectionCache == null) ? null : sConnectionCache.get(spec != null ? spec.connectString : connectString);

		if (connection == null) {
			showErrorMessage("Could not connect to database.");
			return;
			}

		startProgress("Retrieving data ...", 0, 0);

		String[] columnName = null;
		ArrayList<byte[][]> resultList = new ArrayList<byte[][]>();
		try {
			String sql = resolveVariables(configuration.getProperty(PROPERTY_SQL).replace(NEWLINE_STRING, " "));
			sql = resolveVariables(sql);
			Statement stmt = connection.createStatement();
			ResultSet rset = stmt.executeQuery (sql);
			ResultSetMetaData metaData = rset.getMetaData();

			columnName = new String[metaData.getColumnCount()];
			for (int column=0; column<metaData.getColumnCount(); column++)
				columnName[column] = metaData.getColumnName(column+1);
	
			while (rset.next()) {
				byte[][] result = new byte[metaData.getColumnCount()][];
				for (int column=0; column<metaData.getColumnCount(); column++) {
					String s = rset.getString(column+1);
					result[column] = (s == null) ? null : s.getBytes();
					}
				resultList.add(result);
				}
			rset.close();
			stmt.close();
			}
		catch (SQLException e) {
			showErrorMessage(e.toString());
			return;
			}

		int oldColumnCount = columnName.length;
		columnName = handleSmiles(columnName, resultList);

		mTargetFrame = mApplication.getEmptyFrame("Custom SQL Result");
		mTargetFrame.getTableModel().initializeTable(resultList.size(), columnName.length);

		for (int column=0; column<columnName.length; column++)
			mTargetFrame.getTableModel().setColumnName(columnName[column], column);

		startProgress("Populating table ...", 0, resultList.size());
		for (int row=0; row<resultList.size(); row++) {
			if (threadMustDie())
				break;

			if ((row & 15) == 15)
				updateProgress(row);

			byte[][] result = resultList.get(row);
			for (int column=0; column<columnName.length; column++)
				mTargetFrame.getTableModel().setTotalDataAt(result[column], row, column);
			}

		if (oldColumnCount != columnName.length)
			for (int i=0; i<columnName.length; i++)
				if (columnName[i].startsWith(STRUCTURE_COLUMN_NAME_START))
					mTargetFrame.getTableModel().setColumnProperty(i,
							CompoundTableConstants.cColumnPropertySpecialType, CompoundTableConstants.cColumnTypeIDCode);

		mTargetFrame.getTableModel().finalizeTable(CompoundTableEvent.cSpecifierDefaultFiltersAndViews, this);
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return mTargetFrame;
		}

	private String[] handleSmiles(String[] columnName, ArrayList<byte[][]> resultList) {
		StereoMolecule mol = new StereoMolecule();
		int columns = Math.min(MAX_COLUMNS_FOR_SMILES_CHECK, columnName.length);
		for (int column=columns-1; column>=0; column--) {
			if (checkForSmiles(resultList, mol, column)) {
				columnName = insertStructureColumnFromSmiles(columnName, resultList, mol, column);
				}
			}
		return columnName;
		}

	private String[] insertStructureColumnFromSmiles(String[] columnName, ArrayList<byte[][]> resultList, StereoMolecule mol, int smilesColumn) {
		int columnCount = columnName.length;
		String structureColumnName = STRUCTURE_COLUMN_NAME_START+columnName[smilesColumn];

		String[] newColumnName = new String[columnCount+1];
		for (int i=0; i<columnCount; i++) {
			newColumnName[i < smilesColumn ? i : i+1] = columnName[i];
			}
		newColumnName[smilesColumn] = structureColumnName;

		byte[][][] rowList = resultList.toArray(new byte[0][][]);
		resultList.clear();
		for (byte[][] row:rowList) {
			byte[][] newRow = new byte[columnCount+1][];
			for (int i=0; i<columnCount; i++)
				newRow[i < smilesColumn ? i : i+1] = row[i];
			if (isValidSmiles(mol, row[smilesColumn]))
				newRow[smilesColumn] = getIDCodeFromMolecule(mol);
			resultList.add(newRow);
			}

		return newColumnName;
		}

	/**
	 * Checks whether a column contains valid SMILES codes, which is considered true if<br>
	 * - the first MAX_ROWS_FOR_SMILES_CHECK non-null entries in the column are valid SMILES<br>
	 * - or (if the column contains less than MAX_ROWS_FOR_SMILES_CHECK rows) every row contains a valid SMILES
	 * @param column
	 * @return
	 */
	private boolean checkForSmiles(ArrayList<byte[][]> resultList, StereoMolecule mol, int column) {
		int found = 0;
		int failures = 0;
		for (byte[][] row:resultList) {
			byte[] data = row[column];
			if (data != null && data.length > 3) {
				if (!isValidSmiles(mol, data)) {
					failures++;
					if (failures > MAX_TOLERATED_SMILES_FAILURES)
						return false;
					}
				else {
					found++;
					if (found == MAX_ROWS_FOR_SMILES_CHECK)
						return true;
					}
				}
			}
		return (resultList.size() != 0 && found == resultList.size());
		}

	private boolean isValidSmiles(StereoMolecule mol, byte[] smiles) {
		if (smiles != null && smiles.length != 0) {
			try {
				new SmilesParser().parse(mol, smiles);
				return mol.getAllAtoms() != 0;
				}
			catch (Exception e) {}
			}
		return false;
		}

	private byte[] getIDCodeFromMolecule(StereoMolecule mol) {
		try {
			mol.normalizeAmbiguousBonds();
			mol.canonizeCharge(true);
			Canonizer canonizer = new Canonizer(mol);
			canonizer.setSingleUnknownAsRacemicParity();
			return canonizer.getIDCode().getBytes();
			}
		catch (Exception e) {}

		return null;
		}
	}

class DatabaseSpec {
	String name,connectString,user,password;
	SQLConnector connector;

	public DatabaseSpec(String[] spec, SQLConnector connector) {
		name = spec[0];
		connectString = spec[1];
		user = spec[2];
		password = spec[3];
		this.connector = connector;
		}
	}
