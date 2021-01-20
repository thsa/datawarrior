package com.actelion.research.datawarrior.task.table;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.AbstractSingleColumnTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.Properties;

public class DETaskSetColumnProperties extends AbstractSingleColumnTask {
	public static final String TASK_NAME = "Set Column Properties";

	private static final String PROPERTY_PROPERTIES = "properties";
	private static final String PROPERTY_REPLACE = "replace";

	private static final String NULL = "<null>";

	private DEFrame mFrame;
	private JTextArea mTextAreaProperties;
	private JCheckBox mCheckBoxReplace;
	private HashMap<String,String> mProperties;
	private boolean mReplace;

	public DETaskSetColumnProperties(DEFrame owner) {
		this(owner, -1, null, false);
	}

	public DETaskSetColumnProperties(DEFrame owner, int column, HashMap<String,String> properties, boolean replace) {
		super(owner, owner.getTableModel(), false, column);
		mFrame = owner;
		mProperties = properties;
		mReplace = replace;
	}

	@Override
	public Properties getPredefinedConfiguration() {
		Properties configuration = super.getPredefinedConfiguration();
		if (configuration != null) {
			configuration.setProperty(PROPERTY_PROPERTIES, encode(mProperties));
			configuration.setProperty(PROPERTY_REPLACE, mReplace ? "true" : "false");
			}
		return configuration;
		}

	@Override
	public JPanel createInnerDialogContent() {
		int gap = HiDPIHelper.scale(8);
		double[][] size = { {TableLayout.PREFERRED, gap/2, HiDPIHelper.scale(320) },
							{gap, TableLayout.PREFERRED, HiDPIHelper.scale(64), gap, TableLayout.PREFERRED, 2*gap} };

		mTextAreaProperties = new JTextArea();
		JScrollPane sp = new JScrollPane(mTextAreaProperties, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		JPanel ip = new JPanel();
		ip.setLayout(new TableLayout(size));
		ip.add(new JLabel("Properties:"), "0,1");
		ip.add(sp, "2,1,2,2");

		mCheckBoxReplace = new JCheckBox("Replace existing properties");
		ip.add(mCheckBoxReplace, "2,4");

		return ip;
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = super.getDialogConfiguration();
		configuration.put(PROPERTY_PROPERTIES, mTextAreaProperties.getText());
		configuration.put(PROPERTY_REPLACE, mCheckBoxReplace.isSelected() ? "true" : "false");
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		super.setDialogConfiguration(configuration);
		mTextAreaProperties.setText(configuration.getProperty(PROPERTY_PROPERTIES, ""));
		mCheckBoxReplace.setSelected(!"false".equals(configuration.getProperty(PROPERTY_REPLACE)));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		super.setDialogConfigurationToDefault();
		columnChanged(getSelectedColumn());
		mCheckBoxReplace.setSelected(true);
		}

	@Override
	public void columnChanged(int column) {
		if (mTextAreaProperties != null)
			mTextAreaProperties.setText(column == -1 ? "" : encode(getTableModel().getColumnProperties(column)));
		}

	@Override
	public boolean isCompatibleColumn(int column) {
		return getTableModel().isColumnDisplayable(column) || CompoundTableConstants.cColumnType3DCoordinates.equals(getTableModel().getColumnSpecialType(column));
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		boolean replace = !"false".equals(configuration.getProperty(PROPERTY_REPLACE));
		HashMap<String,String> prop = decode(configuration.getProperty(PROPERTY_PROPERTIES));
		if (!replace && prop.size() == 0) {
			showErrorMessage("No column properties found.");
			return false;
			}

		return super.isConfigurationValid(configuration, isLive);
		}

	public boolean isRedundant(Properties previousConfiguration, Properties currentConfiguration) {
		int column1 = getColumn(previousConfiguration);
		int column2 = getColumn(currentConfiguration);
		if (column1 != column2)
			return false;

		boolean replace2 = !"false".equals(currentConfiguration.getProperty(PROPERTY_REPLACE));
		if (replace2)
			return true;

		boolean replace1 = !"false".equals(previousConfiguration.getProperty(PROPERTY_REPLACE));
		if (replace1)
			return false;

		HashMap<String,String> prop1 = decode(previousConfiguration.getProperty(PROPERTY_PROPERTIES));
		HashMap<String,String> prop2 = decode(currentConfiguration.getProperty(PROPERTY_PROPERTIES));
		for (String key1:prop1.keySet())
			if (!prop2.keySet().contains(key1))
				return false;

		return true;
		}

	private HashMap<String,String> decode(String s) {
		HashMap<String,String> properties = new HashMap<String,String>();
		if (s != null && s.length() != 0) {
			String[] list = s.split("\\n");
			for (String p:list) {
				int index = p.indexOf('=');
				if (index > 0 && index < p.length()-1) {
					String value = p.substring(index+1);
					properties.put(p.substring(0, index), value.equals(NULL) ? null : value);
					}
				}
			}
		return properties;
		}

	private String encode(HashMap<String,String> properties) {
		if (properties == null || properties.size() == 0)
			return "";

		StringBuilder sb = new StringBuilder();
		for (String key:properties.keySet()) {
			sb.append(key);
			sb.append('=');
			String value = properties.get(key);
			sb.append(value == null ? NULL : value);
			sb.append('\n');
			}
		return sb.toString();
		}

	@Override
	public void runTask(Properties configuration) {
		HashMap<String,String> properties = decode(configuration.getProperty(PROPERTY_PROPERTIES));
		int column = getColumn(configuration);
		boolean replace = !"false".equals(configuration.getProperty(PROPERTY_REPLACE));
		if (replace)
			getTableModel().setColumnProperties(column, properties);
		else if (properties != null)
			for (String key : properties.keySet())
				getTableModel().setColumnProperty(column, key, properties.get(key));

		mFrame.setDirty(true);
		}
	}
