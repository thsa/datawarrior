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

package com.actelion.research.datawarrior.task.data;

import info.clearthought.layout.TableLayout;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.actelion.research.calc.SelfOrganizedMap;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.datawarrior.task.file.JFilePathLabel;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.CompoundTableSOM;
import com.actelion.research.table.MarkerLabelDisplayer;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.JVisualization2D;
import com.actelion.research.table.view.VisualizationColor;
import com.actelion.research.table.view.VisualizationPanel2D;

public class DETaskCalculateSOM extends ConfigurableTask implements ActionListener {
	public static final String TASK_NAME = "Create Self Organizing Map";

	private static final String PROPERTY_COLUMN_LIST = "columnList";
	private static final String PROPERTY_MAP_SIZE = "mapSize";
	private static final String PROPERTY_FUNCTION = "function";
	private static final String PROPERTY_GROW = "grow";
	private static final String PROPERTY_TOROIDAL = "toroidal";
	private static final String PROPERTY_FAST = "fastBestMatch";
	private static final String PROPERTY_LANDSCAPE = "landscape";
	private static final String PROPERTY_PIVOT = "pivot";
	private static final String PROPERTY_PIVOT_GROUP_COLUMN = "pivotGroupColumn";
	private static final String PROPERTY_PIVOT_DATA_COLUMN = "pivotDataColumn";
	private static final String PROPERTY_FILENAME = "fileName";

	private static final String[] MAP_SIZE_OPTIONS = { "10", "25", "50", "100", "150", "200" };
	private static final String[] FUNCTION_OPTIONS = {"Gaussean", "Mexican Hat", "Linear"};
	private static final String[] FUNCTION_CODE = {"gaussean", "mexicanHat", "linear"};

	private DEFrame				mParentFrame;
    private CompoundTableModel  mTableModel;
	private JComboBox			mComboBoxMapSize,mComboBoxFunction,
                                mComboBoxPivotGroupColumn,mComboBoxPivotDataColumn;
    private TreeMap<String,Integer> mColumnMap;
	private JList				mListColumns;
	private JTextArea			mTextArea;
	private JCheckBox			mCheckboxGrow,mCheckboxToroidal,mCheckboxCreateLandscape,mCheckBoxSaveMap,
                                mCheckboxFastBestMatch,mCheckBoxPivotTable;
	private JButton				mButtonEdit;
	private CompoundTableSOM	mSOM;
	private BufferedImage       mBackgroundImage;
    private JFilePathLabel		mLabelFileName;
	private boolean				mCheckOverwrite;

    public DETaskCalculateSOM(DEFrame parent, boolean checkOverride) {
		super(parent, true);
		mParentFrame = parent;
		mTableModel = parent.getTableModel();
		mCheckOverwrite = checkOverride;
    	}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
	public boolean isConfigurable() {
        if (mTableModel.getTotalRowCount() < 2) {
        	showErrorMessage("No data rows found.");
            return false;
            }
        int columnCount = 0;
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (columnQualifies(column))
				columnCount += (mTableModel.isDescriptorColumn(column) ? 2 : 1);
        if (columnCount < 2) {
        	showErrorMessage("Found neither a complete chemical descriptor\n"
        					+"nor two numerical complete columns.");
            return false;
            }

		return true;
		}

    private boolean columnQualifies(int column) {
        String specialType = mTableModel.getColumnSpecialType(column);
        if (specialType == null) {
            return mTableModel.hasNumericalVariance(column)
            	&& mTableModel.isColumnDataComplete(column);
            }
        else {
        	return mTableModel.isDescriptorColumn(column)
        		&& mTableModel.isColumnDataComplete(column)
        		&& CompoundTableSOM.isDescriptorSupported(specialType);
            }
    	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public JComponent createDialogContent() {
        mColumnMap = new TreeMap<String,Integer>();
        for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
            String specialType = mTableModel.getColumnSpecialType(column);
            if (mTableModel.isColumnDataComplete(column)
             && ((specialType == null && mTableModel.hasNumericalVariance(column))
              || (mTableModel.isDescriptorColumn(column) && CompoundTableSOM.isDescriptorSupported(specialType))))
                mColumnMap.put(mTableModel.getColumnTitle(column), new Integer(column));
        	}

        String[] columnList = mColumnMap.keySet().toArray(new String[0]);
        Arrays.sort(columnList, new Comparator<String>() {
                    public int compare(String s1, String s2) {
                        return s1.compareToIgnoreCase(s2);
                        }
                    } );

        JPanel optionPanel = new JPanel();
        double[][] size = { {8, TableLayout.PREFERRED, 16, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8, TableLayout.PREFERRED, 8},
                            {8, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 8,
        						TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.PREFERRED, 16,
        						TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 16,
        						TableLayout.PREFERRED, 4, TableLayout.PREFERRED, 16} };
        optionPanel.setLayout(new TableLayout(size));

        mComboBoxMapSize = new JComboBox(MAP_SIZE_OPTIONS);
		mComboBoxMapSize.setEditable(true);
		mComboBoxMapSize.setSelectedIndex(0);
        optionPanel.add(new JLabel("Neurons per axis:"),"3,3");
        optionPanel.add(mComboBoxMapSize, "5,3,7,3");

        mComboBoxFunction = new JComboBox(FUNCTION_OPTIONS);
		mComboBoxFunction.setSelectedIndex(0);
        optionPanel.add(new JLabel("Neighbourhood function:"), "3,5");
        optionPanel.add(mComboBoxFunction, "5,5,7,5");

        optionPanel.add(new JLabel("Parameters used:"), "1,1");
        JScrollPane scrollPane = null;
		if (isInteractive()) {
	        mListColumns = new JList(columnList);
			scrollPane = new JScrollPane(mListColumns);
			}
		else {
			mTextArea = new JTextArea();
			scrollPane = new JScrollPane(mTextArea);
			}
		scrollPane.setPreferredSize(new Dimension(180,120));
        optionPanel.add(scrollPane, "1,3,1,20");

        mCheckboxGrow = new JCheckBox("Grow map during optimization", false);
        mCheckboxToroidal = new JCheckBox("Create unlimited map", true);
        mCheckboxFastBestMatch = new JCheckBox("Fast best match finding", true);
        mCheckboxCreateLandscape = new JCheckBox("Show vector similarity landscape in background", true);
        optionPanel.add(mCheckboxGrow, "3,7,7,7");
        optionPanel.add(mCheckboxToroidal, "3,8,7,8");
        optionPanel.add(mCheckboxFastBestMatch, "3,9,7,9");
        optionPanel.add(mCheckboxCreateLandscape, "3,10,7,10");

        ArrayList<String> categoryColumnList = new ArrayList<String>();
        for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
            if (mTableModel.isColumnTypeCategory(column))
                categoryColumnList.add(mTableModel.getColumnTitle(column));
        String[] categoryColumns = categoryColumnList.toArray(new String[0]);

        mCheckBoxPivotTable = new JCheckBox("Use Pivot Table", false);
        mCheckBoxPivotTable.addActionListener(this);
        optionPanel.add(mCheckBoxPivotTable, "3,12,7,12");
        optionPanel.add(new JLabel("Group by:", JLabel.RIGHT), "3,14");
        optionPanel.add(new JLabel("Split data by:", JLabel.RIGHT), "3,16");
        mComboBoxPivotGroupColumn = new JComboBox(categoryColumns);
        mComboBoxPivotGroupColumn.setEnabled(false);
        mComboBoxPivotGroupColumn.setEditable(!isInteractive());
        optionPanel.add(mComboBoxPivotGroupColumn, "5,14,7,14");
        mComboBoxPivotDataColumn = new JComboBox(categoryColumns);
        mComboBoxPivotDataColumn.setEnabled(false);
        mComboBoxPivotDataColumn.setEditable(!isInteractive());
        optionPanel.add(mComboBoxPivotDataColumn, "5,16,7,16");

        mCheckBoxSaveMap = new JCheckBox("Save file with SOM vectors", false);
        mCheckBoxSaveMap.addActionListener(this);
		optionPanel.add(mCheckBoxSaveMap, "3,18,5,18");
		mButtonEdit = new JButton(JFilePathLabel.BUTTON_TEXT);
		mButtonEdit.addActionListener(this);
		JPanel ep = new JPanel();
		ep.add(mButtonEdit);
		optionPanel.add(ep, "7,18");
		mLabelFileName = new JFilePathLabel(!isInteractive());
		optionPanel.add(mLabelFileName, "3,20,7,20");

        return optionPanel;
	    }

	@Override
	public String getHelpURL() {
		return "/html/help/ml.html#SOM";
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		String columnNames = isInteractive() ?
				  getSelectedColumnsFromList(mListColumns, mTableModel)
				: mTextArea.getText().replace('\n', '\t');
		if (columnNames != null && columnNames.length() != 0)
			configuration.setProperty(PROPERTY_COLUMN_LIST, columnNames);

		configuration.setProperty(PROPERTY_MAP_SIZE, (String)mComboBoxMapSize.getSelectedItem());
		configuration.setProperty(PROPERTY_FUNCTION, FUNCTION_CODE[mComboBoxFunction.getSelectedIndex()]);
		configuration.setProperty(PROPERTY_GROW, mCheckboxGrow.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_TOROIDAL, mCheckboxToroidal.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_FAST, mCheckboxFastBestMatch.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_LANDSCAPE, mCheckboxCreateLandscape.isSelected() ? "true" : "false");
		configuration.setProperty(PROPERTY_PIVOT, mCheckBoxPivotTable.isSelected() ? "true" : "false");
		if (mCheckBoxPivotTable.isSelected()) {
			configuration.setProperty(PROPERTY_PIVOT_GROUP_COLUMN, mTableModel.getColumnTitleNoAlias(mTableModel.findColumn((String)mComboBoxPivotGroupColumn.getSelectedItem())));
			configuration.setProperty(PROPERTY_PIVOT_DATA_COLUMN, mTableModel.getColumnTitleNoAlias(mTableModel.findColumn((String)mComboBoxPivotDataColumn.getSelectedItem())));
			}
		if (mCheckBoxSaveMap.isSelected()) {
	    	String fileName = mLabelFileName.getPath();
    		configuration.setProperty(PROPERTY_FILENAME, (fileName == null) ? "" : fileName);
			}
		return configuration;
		}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String columnNames = configuration.getProperty(PROPERTY_COLUMN_LIST, "");
		if (isInteractive())
			selectColumnsInList(mListColumns, columnNames, mTableModel);
		else
			mTextArea.setText(columnNames.replace('\t', '\n'));

		mComboBoxMapSize.setSelectedItem(configuration.getProperty(PROPERTY_MAP_SIZE, MAP_SIZE_OPTIONS[0]));
		mComboBoxFunction.setSelectedIndex(findListIndex(configuration.getProperty(PROPERTY_FUNCTION), FUNCTION_CODE, 0));
		mCheckboxGrow.setSelected("true".equals(configuration.getProperty(PROPERTY_GROW)));
		mCheckboxToroidal.setSelected("true".equals(configuration.getProperty(PROPERTY_TOROIDAL)));
		mCheckboxFastBestMatch.setSelected("true".equals(configuration.getProperty(PROPERTY_FAST)));
		mCheckboxCreateLandscape.setSelected("true".equals(configuration.getProperty(PROPERTY_LANDSCAPE)));
		mCheckBoxPivotTable.setSelected("true".equals(configuration.getProperty(PROPERTY_PIVOT)));
		if (mCheckBoxPivotTable.isSelected()) {
			int pivotGroupColumn = mTableModel.findColumn(configuration.getProperty(PROPERTY_PIVOT_GROUP_COLUMN));
			if (pivotGroupColumn != -1 && mTableModel.isColumnTypeCategory(pivotGroupColumn))
				mComboBoxPivotGroupColumn.setSelectedItem(mTableModel.getColumnTitle(pivotGroupColumn));
			int pivotDataColumn = mTableModel.findColumn(configuration.getProperty(PROPERTY_PIVOT_DATA_COLUMN));
			if (pivotDataColumn != -1 && mTableModel.isColumnTypeCategory(pivotDataColumn))
				mComboBoxPivotDataColumn.setSelectedItem(mTableModel.getColumnTitle(pivotDataColumn));
			}
		String fileName = configuration.getProperty(PROPERTY_FILENAME);
		mCheckBoxSaveMap.setSelected(fileName != null);
		mButtonEdit.setEnabled(mCheckBoxSaveMap.isSelected());
		mLabelFileName.setPath(fileName);
		}

	@Override
	public void setDialogConfigurationToDefault() {
		if (isInteractive())
			mListColumns.clearSelection();
		else
			mTextArea.setText("");

		mComboBoxMapSize.setSelectedIndex(0);
		mComboBoxFunction.setSelectedIndex(0);
		mCheckboxGrow.setSelected(false);
		mCheckboxToroidal.setSelected(true);
		mCheckboxFastBestMatch.setSelected(true);
		mCheckboxCreateLandscape.setSelected(true);
		mCheckBoxPivotTable.setSelected(false);
		mCheckBoxSaveMap.setSelected(false);
		mButtonEdit.setEnabled(false);
		mLabelFileName.setPath(null);
		}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String columnList = configuration.getProperty(PROPERTY_COLUMN_LIST);
		if (columnList == null) {
			showErrorMessage("No parameter columns defined.");
			return false;
			}

		boolean doPivot = "true".equals(configuration.getProperty(PROPERTY_PIVOT));

		if (isLive) {
			String[] columnName = columnList.split("\\t");
	        int selectedDescriptors = 0;
	        int selectedParameters = 0;
			for (int i=0; i<columnName.length; i++) {
				int column = mTableModel.findColumn(columnName[i]);
				if (column == -1) {
					showErrorMessage("Column '"+columnName[i]+"' not found.");
					return false;
					}
	            if (mTableModel.isDescriptorColumn(column))
	                selectedDescriptors++;
	            else
	                selectedParameters++;
				}
	        if (selectedDescriptors > 1) {
	        	showErrorMessage("More than one descriptor selected.");
	            return false;
	            }
	        if (selectedDescriptors == 1 && selectedParameters != 0) {
	        	showErrorMessage("A mix of descriptors and other columns selected.");
	            return false;
	            }
	        if (selectedDescriptors == 1 && doPivot) {
	        	showErrorMessage("A pivot table cannot be done with a descriptor as parameter.");
	            return false;
	            }
	        if (selectedDescriptors == 0 && !doPivot && selectedParameters < 2) {
	        	showErrorMessage("Either a descriptor or at least two other columns need to be selected.");
	            return false;
	            }
			}

        try {
			Integer.parseInt(configuration.getProperty(PROPERTY_MAP_SIZE, ""));
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("The number of neurons per axis is not numerical.");
			return false;
			}

        if ("true".equals(configuration.getProperty(PROPERTY_GROW))
		 && "false".equals(configuration.getProperty(PROPERTY_TOROIDAL))) {
			showErrorMessage("SOM growing is only supported for maps with toroidal topology.");
			return false;
			}

        if (doPivot) {
            int groupColumn = mTableModel.findColumn(configuration.getProperty(PROPERTY_PIVOT_GROUP_COLUMN, ""));
            int dataColumn = mTableModel.findColumn(configuration.getProperty(PROPERTY_PIVOT_DATA_COLUMN, ""));
            if (groupColumn == dataColumn) {
                showErrorMessage("Cannot use the same columns for pivot grouping and data splitting.");
                return false;
                }
            if (isLive) {
	    		if (groupColumn == -1) {
	    			showErrorMessage("Pivot group column '"+configuration.getProperty(PROPERTY_PIVOT_GROUP_COLUMN)+"' not found.");
	    	        return false;
	    			}
	    		if (dataColumn == -1) {
	    			showErrorMessage("Pivot group column '"+configuration.getProperty(PROPERTY_PIVOT_DATA_COLUMN)+"' not found.");
	    	        return false;
	    			}
            	}
            }

        if (isLive) {
	        String fileName = configuration.getProperty(PROPERTY_FILENAME);
			if (fileName != null && !isFileAndPathValid(fileName, true, mCheckOverwrite))
				return false;
        	}

        return true;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
        if (e.getSource() == mCheckBoxPivotTable) {
            if (mComboBoxPivotGroupColumn.getItemCount() < 2) {
                JOptionPane.showMessageDialog(getParentFrame(), "You need at least two category columns for this option.");
                mCheckBoxPivotTable.setSelected(false);
                return;
                }
            mComboBoxPivotGroupColumn.setEnabled(mCheckBoxPivotTable.isSelected());
            mComboBoxPivotDataColumn.setEnabled(mCheckBoxPivotTable.isSelected());
            return;
            }
        if (e.getSource() == mCheckBoxSaveMap) {
        	mButtonEdit.setEnabled(mCheckBoxSaveMap.isSelected());
        	}
		if (e.getActionCommand().equals(JFilePathLabel.BUTTON_TEXT)
		 || (e.getSource() == mCheckBoxSaveMap && mCheckBoxSaveMap.isSelected() && mLabelFileName.getPath() == null)) {
			String filename = resolvePathVariables(mLabelFileName.getPath());
			if (filename == null)
				filename = mParentFrame.getSuggestedFileName();
			filename = new FileHelper(getParentFrame()).selectFileToSave(
					"Save SOM Vector File", FileHelper.cFileTypeSOM, filename);
			if (filename != null) {
				mLabelFileName.setPath(filename);
				mCheckOverwrite = false;
				}
			}
		}

	@Override
	public void runTask(final Properties configuration) {
		if (!threadMustDie()) {
			String[] columnName = configuration.getProperty(PROPERTY_COLUMN_LIST).split("\\t");
            int[] columnList = new int[columnName.length];
			for (int i=0; i<columnList.length; i++) {
				columnList[i] = mTableModel.findColumn(columnName[i]);
				if (mTableModel.isDescriptorColumn(columnList[i])) {
		            waitForDescriptor(mTableModel, columnList[i]);
					if (threadMustDie())
						return;
					}
				}

			int mapSize = Integer.parseInt(configuration.getProperty(PROPERTY_MAP_SIZE));

			int mode = ("true".equals(configuration.getProperty(PROPERTY_FAST)) ? SelfOrganizedMap.cModeFastBestMatchFinding : 0)
					 + ("true".equals(configuration.getProperty(PROPERTY_TOROIDAL)) ? SelfOrganizedMap.cModeTopologyUnlimited : 0)
					 + ("true".equals(configuration.getProperty(PROPERTY_GROW)) ? SelfOrganizedMap.cModeGrowDuringOptimization : 0);

			switch (findListIndex(configuration.getProperty(PROPERTY_FUNCTION), FUNCTION_CODE, 0)) {
			case 0:
				mode += SelfOrganizedMap.cModeNeighbourhoodGaussean;
				break;
			case 1:
				mode += SelfOrganizedMap.cModeNeighbourhoodMexicanHat;
				break;
			case 2:
				mode += SelfOrganizedMap.cModeNeighbourhoodLinear;
				break;
				}

			mSOM = new CompoundTableSOM(mapSize, mapSize, mode, mTableModel, columnList);
            if ("true".equals(configuration.getProperty(PROPERTY_PIVOT))) {
                int groupColumn = mTableModel.findColumn(configuration.getProperty(PROPERTY_PIVOT_GROUP_COLUMN));
                int dataColumn = mTableModel.findColumn(configuration.getProperty(PROPERTY_PIVOT_DATA_COLUMN));
                mSOM.setPivotColumns(groupColumn, dataColumn);
                }
			mSOM.addProgressListener(this);
			mSOM.setThreadMaster(this);
			mSOM.organize();
			if ("true".equals(configuration.getProperty(PROPERTY_LANDSCAPE)))
			    mBackgroundImage = mSOM.createSimilarityMapImage(Math.max(mapSize*4, 768), Math.max(mapSize*4, 768));
            mSOM.positionRecords();
			}

        String fileName = configuration.getProperty(PROPERTY_FILENAME);
		if (fileName != null)
			writeSOM(new File(resolvePathVariables(fileName)));

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
                int somFitColumn = mTableModel.getTotalColumnCount()-1;
                String xColumn = mTableModel.getColumnTitle(somFitColumn-2);
                String yColumn = mTableModel.getColumnTitle(somFitColumn-1);
                if (xColumn.startsWith("SOM_X") && yColumn.startsWith("SOM_Y")) {
                    VisualizationPanel2D vpanel1 = mParentFrame.getMainFrame().getMainPane().add2DView("SOM", null);
                    vpanel1.setAxisColumnName(0, xColumn);
                    vpanel1.setAxisColumnName(1, yColumn);
                    ((JVisualization2D)vpanel1.getVisualization()).setPreferredChartType(JVisualization.cChartTypeScatterPlot, -1, -1);
                    int colorListMode = VisualizationColor.cColorListModeHSBLong;
                    Color[] colorList = VisualizationColor.createColorWedge(Color.red, Color.blue, colorListMode, null);
                    vpanel1.getVisualization().getMarkerColor().setColor(somFitColumn, colorList, colorListMode);
                    int groupColumn = mTableModel.findColumn(configuration.getProperty(PROPERTY_PIVOT_GROUP_COLUMN));
                    if ("true".equals(configuration.getProperty(PROPERTY_PIVOT)) && mTableModel.getCategoryCount(groupColumn) < 256) {
                    	int[] labelColumn = new int[MarkerLabelDisplayer.cPositionCode.length];
                    	labelColumn[JVisualization.cTopLeft] = groupColumn;
                        vpanel1.getVisualization().setMarkerLabels(labelColumn);
                    	}
                    if (mBackgroundImage != null) {
                        ((JVisualization2D)vpanel1.getVisualization()).setBackgroundImage(mBackgroundImage);
                        vpanel1.getVisualization().setScaleMode(JVisualization.cScaleModeHidden);
                        vpanel1.getVisualization().setGridMode(JVisualization.cGridModeHidden);
                    	}
                    }
                }
			} );
		}

	private void writeSOM(File file) {
		try {
			BufferedWriter theWriter = new BufferedWriter(new FileWriter(file));
			theWriter.write("<datawarriorSOM type=\""+CompoundTableSOM.SOM_TYPE_FILE[mSOM.getType()]+"\">");
			theWriter.newLine();
	
			mSOM.write(theWriter);
	
			theWriter.write("</datawarriorSOM>");
			theWriter.newLine();
			theWriter.close();
			}
		catch (IOException ioe) {
			ioe.printStackTrace();
			}
		}
	}
