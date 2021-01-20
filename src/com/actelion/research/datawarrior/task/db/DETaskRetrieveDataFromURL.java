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

import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DERuntimeProperties;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.CompoundTableLoader;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;


public class DETaskRetrieveDataFromURL extends ConfigurableTask {
	public static final String TASK_NAME = "Retrieve Data From URL";

	private static final String PROPERTY_URL = "url";
	private static final String PROPERTY_FORMAT = "format";
	private static final String PROPERTY_TEMPLATE = "template";
	private static final String PROPERTY_WINDOW_NAME = "windowName";

	private static final String[] FORMAT_TEXT = { "TAB delimited", "comma separated" };
	private static final String[] FORMAT_CODE = { "td", "cs" };
	private static final int[] FORMAT = { FileHelper.cFileTypeTextTabDelimited, FileHelper.cFileTypeTextCommaSeparated };

	private DataWarrior mApplication;
	private DEFrame     mTargetFrame;
	private JTextArea   mTextAreaURL;
	private JComboBox   mComboBoxFormat;

	public DETaskRetrieveDataFromURL(Frame parent, DataWarrior application) {
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
		return "/html/help/databases.html#CustomURL";
		}

	@Override
	public JComponent createDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {gap, TableLayout.FILL, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, TableLayout.FILL, gap},
							{gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED, gap} };

		JPanel content = new JPanel();
		content.setLayout(new TableLayout(size));

		content.add(new JLabel("Data URL:"), "1,1,3,1");
		mTextAreaURL = new JTextArea();
		mTextAreaURL.setPreferredSize(new Dimension(HiDPIHelper.scale(400), HiDPIHelper.scale(80)));
		mTextAreaURL.setLineWrap(true);
		content.add(mTextAreaURL, "1,3,5,3");

		mComboBoxFormat = new JComboBox(FORMAT_TEXT);
		content.add(new JLabel("Data Format:"), "2,5");
		content.add(mComboBoxFormat, "4,5");

		return content;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();
		configuration.setProperty(PROPERTY_FORMAT, FORMAT_CODE[mComboBoxFormat.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_URL, mTextAreaURL.getText());
		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		int format = findListIndex(configuration.getProperty(PROPERTY_FORMAT), FORMAT_CODE, 0);
		mComboBoxFormat.setSelectedIndex(format);

		mTextAreaURL.setText(configuration.getProperty(PROPERTY_URL, ""));
	}

	@Override
	public void setDialogConfigurationToDefault() {
		mComboBoxFormat.setSelectedIndex(0);
		mTextAreaURL.setText("");
	}

	@Override
	public DEFrame getNewFrontFrame() {
		return mTargetFrame;
	}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String url = configuration.getProperty(PROPERTY_URL, "");
		if (url.length() == 0) {
			showErrorMessage("Missing URL for data retrieval.");
			return false;
		}

		String format = configuration.getProperty(PROPERTY_FORMAT, "");
		if (findListIndex(format, FORMAT_CODE, -1) == -1) {
			showErrorMessage("Unknown URL data format.");
			return false;
		}

		return true;
	}

	@Override
	public void runTask(Properties configuration) {
		try {
			startProgress("Retrieving Data From URL...", 0, 0);

			URLConnection con = new URL(configuration.getProperty(PROPERTY_URL)).openConnection();
			con.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)");
			con.setRequestProperty("Content-Type", "text/plain");

			InputStream is = con.getInputStream();
			if (is != null) {
				String title = configuration.getProperty(PROPERTY_WINDOW_NAME, "Data From URL");
				mTargetFrame = mApplication.getEmptyFrame(title);
				CompoundTableLoader loader = new CompoundTableLoader(mTargetFrame, mTargetFrame.getTableModel(), this);
				DERuntimeProperties rtp = "none".equals(configuration.getProperty(PROPERTY_TEMPLATE)) ?
						DERuntimeProperties.getTableOnlyProperties(mTargetFrame.getMainFrame())
						: new DERuntimeProperties(mTargetFrame.getMainFrame());
				int format = FORMAT[findListIndex(configuration.getProperty(PROPERTY_FORMAT, ""), FORMAT_CODE, -1)];
				int action = CompoundTableLoader.READ_DATA | CompoundTableLoader.REPLACE_DATA;
				loader.readStream(new BufferedReader(new InputStreamReader(is)), rtp, format, action, title);
			}
		}
		catch (Exception e) {
			if (isInteractive()) {
				SwingUtilities.invokeLater(() ->
				JOptionPane.showMessageDialog(getParentFrame(), "Communication error:"+e.getMessage()
						+"\nA firewall or local security software or settings may prevent contacting the server."));
			}
		}
	}
}
