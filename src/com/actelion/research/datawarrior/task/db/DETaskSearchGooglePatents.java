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
import com.actelion.research.datawarrior.DEPruningPanel;
import com.actelion.research.datawarrior.DERuntimeProperties;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.JEditableStructureView;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.CompoundTableLoader;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.BrowserControl;
import info.clearthought.layout.TableLayout;
import org.pushingpixels.substance.internal.utils.border.SubstanceTextComponentBorder;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Properties;


public class DETaskSearchGooglePatents extends ConfigurableTask implements ActionListener {
	public static final String TASK_NAME = "Search Google Patents";


	private static final String PROPERTY_KEYWORDS = "keywords";
	private static final String PROPERTY_ASSIGNEE = "assignee";
	private static final String PROPERTY_INVENTOR = "inventor";
	private static final String PROPERTY_IDCODE = "idcode";
	private static final String PROPERTY_IDCOORDS = "idcoordinates";
	private static final String PROPERTY_SEARCH_TYPE = "searchType";
	private static final String PROPERTY_DATE_TYPE = "dateType";
	private static final String PROPERTY_DATE_AFTER = "after";
	private static final String PROPERTY_DATE_BEFORE = "before";
	private static final String PROPERTY_TYPE = "type";
	private static final String PROPERTY_STATUS = "status";
	private static final String PROPERTY_LITIGATION = "litigation";
	private static final String PROPERTY_WITH_CONCEPTS = "withConcepts";

	private static final String BROWSER_URL_START = "https://patents.google.com/?";
	private static final String BASE_URL = "https://patents.google.com/xhr/query?url=";
	private static final String URL_END_CSV = "&exp=&download=true";
	private static final String URL_END_STRUCTURES = "&exp=&download=concepts";
	private static final String URL_TEST = "inventor=boss&after=priority:20180101&language=ENGLISH,GERMAN,SPANISH&type=PATENT&litigation=NO";
	private static final String URL_TEST2 = "https://patents.google.com/xhr/query?url=inventor%3Dboss%26after%3Dpriority%3A20180101%26language%3DENGLISH%2CGERMAN%2CSPANISH%26type%3DPATENT%26litigation%3DNO%26exp%3D%26download%3Dtrue";
	private static final String URL_TEST3 = "https://patents.google.com/xhr/query?url=inventor%3Dkubiny&exp=&download=true";

	private static final String WORKING_EXAMPLE = "https://patents.google.com/xhr/query?url=inventor%3DH%2BKubinyi%26language%3DENGLISH%2CGERMAN%2CSPANISH%26litigation%3DNO&exp=&download=true";
	private static final String SMILES_EXAMPLE = "https://patents.google.com/xhr/query?url=q%3DSMILES%253dS(%253dO)(%253dO)(NC)C1(%253dC(N)C%253dC(OC)C(%253dC1)C(%253dO)NCC2(N(CCC2)CC%253dC))%26inventor%3Dyamashita&exp=&download=true";

	private static final String EXACT_QUERY = "SMILES=S(=O)(=O)(NC)C1(=C(N)C=C(OC)C(=C1)C(=O)NCC2(N(CCC2)CC=C))";
	private static final String REST_OF_QUERY = "&inventor=yamashita";

	private static final String[] DATE_TYPE_CODE = {"priority", "filing", "publication" };

	private static final String[] SEARCH_TYPE_TEXT = {"Similar Structures", "Super-structures", "Equal Structures" };
	private static final String[] SEARCH_TYPE_CODE = {"sim", "sss", "exact" };
	private static final int SEARCH_TYPE_SIMILAR = 0;
	private static final int SEARCH_TYPE_SSS = 1;
	private static final int SEARCH_TYPE_EXACT = 2;

	private static final String[] TYPE_TEXT = {"Any", "Patent", "Design" };
	private static final String[] TYPE_CODE = {"ANY", "PATENT", "DESIGN" };
	private static final String[] STATUS_TEXT = {"Any", "Grant", "Application" };
	private static final String[] STATUS_CODE = {"ANY", "GRANT", "APPLICATION" };
	private static final String[] LITIGATION_TEXT = {"Any", "No known litigation", "Has related litigation" };
	private static final String[] LITIGATION_CODE = {"ANY", "NO", "YES" };

	private DataWarrior mApplication;
	private DEFrame mTargetFrame;
	private JEditableStructureView mStructureView;
	private JComboBox mComboBoxSearchType,mComboBoxDateType,mComboBoxType,mComboBoxStatus,mComboBoxLitigation;
	private JTextField  mTextFieldKeywords,mTextFieldAssignee,mTextFieldInventor,mTextFieldAfter,mTextFieldBefore;
	private JCheckBox   mCheckBoxWithConcepts;

	public DETaskSearchGooglePatents(Frame parent, DataWarrior application) {
		super(parent, true);
		mApplication = application;
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
		return null; // "/html/help/databases.html#GooglePatents";
	}

	@Override
	public JComponent createDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, TableLayout.FILL, gap},
							{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.FILL, gap, TableLayout.PREFERRED,
							2*gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED,
							gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED,
							gap/2, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		content.add(new JLabel("Query Structure:", JLabel.RIGHT), "1,1");

		StereoMolecule mol = new StereoMolecule();
//		mol.setFragment(true);
		int scaled100 = HiDPIHelper.scale(120);
		mStructureView = new JEditableStructureView(mol);
		mStructureView.setMinimumSize(new Dimension(2*scaled100, scaled100));
		mStructureView.setPreferredSize(new Dimension(2*scaled100, scaled100));
		mStructureView.setBorder(new SubstanceTextComponentBorder(new Insets(2,2,2,2)));
//		mStructureView.setOpaqueBackground(true);
		mStructureView.setClipboardHandler(new ClipboardHandler());
		content.add(mStructureView, "3,1,4,5");

		content.add(new JLabel("Search Type:", JLabel.RIGHT), "1,7");
		mComboBoxSearchType = new JComboBox(SEARCH_TYPE_TEXT);
		mComboBoxSearchType.addActionListener(this);
		content.add(mComboBoxSearchType, "3,7");

		content.add(new JLabel("Keywords:", JLabel.RIGHT), "1,9");
		mTextFieldKeywords = new JTextField();
		content.add(mTextFieldKeywords, "3,9,4,9");

		content.add(new JLabel("Assignee:", JLabel.RIGHT), "1,11");
		mTextFieldAssignee = new JTextField();
		content.add(mTextFieldAssignee, "3,11,4,11");

		content.add(new JLabel("Inventor:", JLabel.RIGHT), "1,13");
		mTextFieldInventor = new JTextField();
		content.add(mTextFieldInventor, "3,13,4,13");

		content.add(new JLabel("Date:", JLabel.RIGHT), "1,15");
		JPanel datePanel = new JPanel();
		mComboBoxDateType = new JComboBox(DATE_TYPE_CODE);
		datePanel.add(mComboBoxDateType, "1,15");
		mTextFieldAfter = new JTextField(6);
		mTextFieldBefore = new JTextField(6);
		datePanel.add(mTextFieldAfter);
		datePanel.add(new JLabel("-"));
		datePanel.add(mTextFieldBefore);
		content.add(datePanel, "3,15,4,15");

		content.add(new JLabel("Type:", JLabel.RIGHT), "1,17");
		mComboBoxType = new JComboBox(TYPE_TEXT);
		content.add(mComboBoxType, "3,17");

		content.add(new JLabel("Status:", JLabel.RIGHT), "1,19");
		mComboBoxStatus = new JComboBox(STATUS_TEXT);
		content.add(mComboBoxStatus, "3,19");

		content.add(new JLabel("Litigation:", JLabel.RIGHT), "1,21");
		mComboBoxLitigation = new JComboBox(LITIGATION_TEXT);
		content.add(mComboBoxLitigation, "3,21");

		mCheckBoxWithConcepts = new JCheckBox("Include individual concepts/structures (larger file)");
		mCheckBoxWithConcepts.setHorizontalAlignment(SwingConstants.CENTER);
		content.add(mCheckBoxWithConcepts, "1,23,4,23");

		return content;
		}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mComboBoxSearchType) {
			boolean isSSS = (mComboBoxSearchType.getSelectedIndex() == SEARCH_TYPE_SSS);
			mStructureView.setDisplayMode(isSSS ? AbstractDepictor.cDModeNoImplicitHydrogen : 0);
			}
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		if (mStructureView.getMolecule().getAllAtoms() != 0) {
			Canonizer canonizer = new Canonizer(mStructureView.getMolecule());
			configuration.setProperty(PROPERTY_IDCODE, canonizer.getIDCode());
			configuration.setProperty(PROPERTY_IDCOORDS, canonizer.getEncodedCoordinates());
			configuration.setProperty(PROPERTY_SEARCH_TYPE, SEARCH_TYPE_CODE[mComboBoxSearchType.getSelectedIndex()]);
			}

		if (mTextFieldKeywords.getText().length() != 0)
			configuration.setProperty(PROPERTY_KEYWORDS, mTextFieldKeywords.getText());
		if (mTextFieldAssignee.getText().length() != 0)
			configuration.setProperty(PROPERTY_ASSIGNEE, mTextFieldAssignee.getText());
		if (mTextFieldInventor.getText().length() != 0)
			configuration.setProperty(PROPERTY_INVENTOR, mTextFieldInventor.getText());

		String after = mTextFieldAfter.getText();
		if (after.length() != 0)
			configuration.setProperty(PROPERTY_DATE_AFTER, after);
		String before = mTextFieldBefore.getText();
		if (before.length() != 0)
			configuration.setProperty(PROPERTY_DATE_BEFORE, before);
		if (after.length() != 0 || before.length() != 0)
			configuration.setProperty(PROPERTY_DATE_TYPE, DATE_TYPE_CODE[mComboBoxDateType.getSelectedIndex()]);

		configuration.setProperty(PROPERTY_TYPE, TYPE_CODE[mComboBoxType.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_STATUS, STATUS_CODE[mComboBoxStatus.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_LITIGATION, LITIGATION_CODE[mComboBoxLitigation.getSelectedIndex()]);

		configuration.setProperty(PROPERTY_WITH_CONCEPTS, mCheckBoxWithConcepts.isSelected() ? "true" : "false");

		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String idcode = configuration.getProperty(PROPERTY_IDCODE, "");
		String coords = configuration.getProperty(PROPERTY_IDCOORDS, "");
		if (idcode.length() != 0 && coords.length() != 0) {
			new IDCodeParser(true).parse(mStructureView.getMolecule(), idcode, coords);
			mStructureView.structureChanged();

			int searchType = findListIndex(configuration.getProperty(PROPERTY_SEARCH_TYPE), SEARCH_TYPE_CODE, 0);
			mComboBoxSearchType.setSelectedIndex(searchType);
			}

		mTextFieldKeywords.setText(configuration.getProperty(PROPERTY_KEYWORDS, ""));
		mTextFieldAssignee.setText(configuration.getProperty(PROPERTY_ASSIGNEE, ""));
		mTextFieldInventor.setText(configuration.getProperty(PROPERTY_INVENTOR, ""));

		mTextFieldAfter.setText(configuration.getProperty(PROPERTY_DATE_AFTER, ""));
		mTextFieldBefore.setText(configuration.getProperty(PROPERTY_DATE_BEFORE, ""));
		int dateType = findListIndex(configuration.getProperty(PROPERTY_DATE_TYPE), DATE_TYPE_CODE, 0);
		mComboBoxDateType.setSelectedIndex(dateType);

		mComboBoxType.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_TYPE), TYPE_CODE, 0));
		mComboBoxStatus.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_STATUS), STATUS_CODE, 0));
		mComboBoxLitigation.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_LITIGATION), LITIGATION_CODE, 0));

		mCheckBoxWithConcepts.setSelected("true".equals(configuration.getProperty(PROPERTY_WITH_CONCEPTS)));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return mTargetFrame;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String idcode = configuration.getProperty(PROPERTY_IDCODE, "");
		String coords = configuration.getProperty(PROPERTY_IDCOORDS, "");
		String keywords = configuration.getProperty(PROPERTY_KEYWORDS, "");
		String assignee = configuration.getProperty(PROPERTY_ASSIGNEE, "");
		String inventor = configuration.getProperty(PROPERTY_INVENTOR, "");

		if (idcode.length() == 0
		 && keywords.length() == 0
		 && assignee.length() == 0
		 && inventor.length() == 0) {
			showErrorMessage("No patent search criteria defined.");
			return false;
			}

		String after = configuration.getProperty(PROPERTY_DATE_AFTER, "");
		if (after.length() != 0 && !checkDateFormat(after))
			return false;
		String before = configuration.getProperty(PROPERTY_DATE_BEFORE, "");
		if (before.length() != 0 && !checkDateFormat(before))
			return false;

		mTextFieldBefore.setText(configuration.getProperty(PROPERTY_DATE_BEFORE, ""));

		return true;
		}

	private boolean checkDateFormat(String date) {
		if (date.length() != 4 && date.length() != 8) {
			showErrorMessage("Unexpected date format. (Allowed: YYYY or YYYYMMDD)");
			return false;
			}
		try {
			int year = Integer.parseInt(date.substring(0, 4));
			if (year < 1800 || year > 2100) {
				showErrorMessage("Invalid year:"+date.substring(0, 4));
				return false;
				}
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("Invalid year:"+date.substring(0, 4));
			return false;
			}
		if (date.length() == 8) {
			try {
				int month = Integer.parseInt(date.substring(4, 6));
				if (month < 1 || month > 12) {
					showErrorMessage("Invalid month:"+date.substring(4, 6));
					return false;
					}
				}
			catch (NumberFormatException nfe) {
				showErrorMessage("Invalid month:"+date.substring(4, 6));
				return false;
				}
			try {
				int day = Integer.parseInt(date.substring(6));
				if (day < 1 || day > 31) {
					showErrorMessage("Invalid day of month:"+date.substring(6));
					return false;
					}
				}
			catch (NumberFormatException nfe) {
				showErrorMessage("Invalid day of month:"+date.substring(6));
				return false;
				}
			}
		return true;
		}

	private static String encodeValue(String value) {
		try {
			return URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20");
			}
		catch (UnsupportedEncodingException ex) {
			throw new RuntimeException(ex.getCause());
			}
		}

	@Override
	public void runTask(Properties configuration) {
		String queryString = null;
		try {
			startProgress("Retrieving Data From Google Patents...", 0, 0);

			ArrayList<String> searchTermList = new ArrayList<>();

			// structure searches are handled as search terms
			String idcode = configuration.getProperty(PROPERTY_IDCODE, "");
			String coords = configuration.getProperty(PROPERTY_IDCOORDS, "");
			if (idcode.length() != 0 && coords.length() != 0) {
				StereoMolecule mol = new IDCodeParser(true).getCompactMolecule(idcode, coords);
				String smiles = new IsomericSmilesCreator(mol).getSmiles();
				int searchType = findListIndex(configuration.getProperty(PROPERTY_SEARCH_TYPE), SEARCH_TYPE_CODE, 0);
				String structureQuery = (searchType == SEARCH_TYPE_SSS) ? "SSS=SMILES="+smiles
									  : (searchType == SEARCH_TYPE_SIMILAR) ? "SMILES=~"+smiles : "SMILES="+smiles;
				searchTermList.add(encodeValue(structureQuery));
				}

			String searchTerms = configuration.getProperty(PROPERTY_KEYWORDS, "");
			if (searchTerms.length() != 0)
				for (String keyword:searchTerms.split(","))
					searchTermList.add(keyword.trim());

			StringBuilder sb = new StringBuilder();

			if (searchTermList.size() != 0) {
				sb.append("q=");
				sb.append(searchTermList.get(0));
				for (int i=1; i<searchTermList.size(); i++) {
					sb.append(",");
					sb.append(searchTermList.get(i));
					}
				}

			String assignee = configuration.getProperty(PROPERTY_ASSIGNEE, "");
			if (assignee.length() != 0) {
				if (sb.length() != 0)
					sb.append("&");
				sb.append("assignee=");
				sb.append(assignee);
				}

			String inventor = configuration.getProperty(PROPERTY_INVENTOR, "");
			if (inventor.length() != 0) {
				if (sb.length() != 0)
					sb.append("&");
				sb.append("inventor=");
				sb.append(inventor);
				}

			String dateType = configuration.getProperty(PROPERTY_INVENTOR, "");
			if (dateType.length() != 0) {
				String after = configuration.getProperty(PROPERTY_DATE_AFTER, "");
				if (after.length() != 0) {
					if (sb.length() != 0)
						sb.append("&");
					sb.append("after=");
					sb.append(dateType);
					sb.append(":");
					sb.append(after);
					if (after.length() == 4)
						sb.append("0101");
					}
				String before = configuration.getProperty(PROPERTY_DATE_BEFORE, "");
				if (before.length() != 0) {
					if (sb.length() != 0)
						sb.append("&");
					sb.append("before=");
					sb.append(dateType);
					sb.append(":");
					sb.append(before);
					if (before.length() == 4)
						sb.append("1231");
					}
				}

			int type = findListIndex(configuration.getProperty(PROPERTY_TYPE), TYPE_CODE, 0);
			if (type != 0) {
				if (sb.length() != 0)
					sb.append("&");
				sb.append("type=");
				sb.append(TYPE_CODE[type]);
				}

			int status = findListIndex(configuration.getProperty(PROPERTY_STATUS), STATUS_CODE, 0);
			if (status != 0) {
				if (sb.length() != 0)
					sb.append("&");
				sb.append("status=");
				sb.append(STATUS_CODE[status]);
				}

			int litigation = findListIndex(configuration.getProperty(PROPERTY_LITIGATION), LITIGATION_CODE, 0);
			if (litigation != 0) {
				if (sb.length() != 0)
					sb.append("&");
				sb.append("litigation=");
				sb.append(LITIGATION_CODE[litigation]);
				}

			queryString = sb.toString();
			String end = "true".equals(configuration.getProperty(PROPERTY_WITH_CONCEPTS)) ? URL_END_STRUCTURES : URL_END_CSV;
			final String url = BASE_URL.concat(encodeValue(queryString)).concat(end);

			URLConnection con = new URL(url).openConnection();
			con.setRequestProperty("User-Agent", "DataWarrior");
			con.setRequestProperty("Content-Type", "text/plain");

			InputStream is = con.getInputStream();
			if (is != null) {
				String title = "Google Patent Search Result";
				mTargetFrame = mApplication.getEmptyFrame(title);
				CompoundTableLoader loader = new CompoundTableLoader(mTargetFrame, mTargetFrame.getTableModel(), this);
				DERuntimeProperties rtp = DERuntimeProperties.getTableOnlyProperties(mTargetFrame.getMainFrame());
				int format = FileHelper.cFileTypeTextCommaSeparated;
				int action = CompoundTableLoader.READ_DATA | CompoundTableLoader.REPLACE_DATA;
				loader.readStream(new BufferedReader(new InputStreamReader(is)), rtp, format, action, title);

				SwingUtilities.invokeLater(() -> addViewsAndFilters());
				}
			}
		catch (Exception e) {
			if (isInteractive()) {
				String message = e.getMessage();
				boolean blockedByGoogle = message.contains("HTTP response code: 429");
				if (blockedByGoogle && queryString != null) {
					final String _queryString = queryString;
					SwingUtilities.invokeLater(() -> {
						String[] options = { "Copy URL To Clipboard", "Open URL in Browser", "Cancel"};
						String text = "The query request was probably blocked by Google because of misuse."
								+ "\nDataWarrior has built a Google Patents search URL from your query."
								+ "\nYou may copy the URL to the clipboard or directly open it in your web browser."
								+ "\nThen, you need to download the result CSV-file to open it in DataWarrior.";
						int result = JOptionPane.showOptionDialog(getParentFrame(), text, "Query Blocked",
								JOptionPane.YES_NO_CANCEL_OPTION,
								JOptionPane.WARNING_MESSAGE, null, options, options[0]);
						if (result != JOptionPane.CANCEL_OPTION) {
							String browserURL = BROWSER_URL_START + _queryString;
							if (result == 0) {
								StringSelection theData = new StringSelection(browserURL);
								Toolkit.getDefaultToolkit().getSystemClipboard().setContents(theData, theData);
								}
							else {
								BrowserControl.displayURL(browserURL);
								}
							}
						} );
					}
				else if (message.contains("HTTP response code")) {
					SwingUtilities.invokeLater(() ->
							JOptionPane.showMessageDialog(getParentFrame(), "Communication error:"+message));
					}
				else {
					SwingUtilities.invokeLater(() ->
							JOptionPane.showMessageDialog(getParentFrame(), "Communication error:"+e.getMessage()
									+"\nA firewall or local security software or settings may prevent contacting the server."));
					}
				}
			}
		}

	private void addViewsAndFilters() {
		CompoundTableModel tableModel = mTargetFrame.getTableModel();
		DEPruningPanel pruningPanel = mTargetFrame.getMainFrame().getPruningPanel();
		for (int column=0; column<tableModel.getTotalColumnCount(); column++) {
			if (!tableModel.getColumnTitleNoAlias(column).endsWith(" link")) {
				try {
					pruningPanel.addDefaultFilter(column);
					}
				catch (DEPruningPanel.FilterException fe) {}
				}
			}
		}

	private boolean isURLColumn(CompoundTableModel tableModel, int column) {
		int rowCount = tableModel.getTotalRowCount();
		int urlCount = 0;
		for (int row=0; row<rowCount; row++) {
			String value = tableModel.getTotalValueAt(row, column);
			if (value.length() != 0) {
				if (!value.startsWith("http://") && !value.startsWith("https://"))
					return false;
				if (++urlCount == 10)
					return true;
				}
			}
		return urlCount != 0;
		}
	}
