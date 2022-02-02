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

import com.actelion.research.calc.SingularValueDecomposition;
import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorHelper;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.MarkerLabelConstants;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationColor;
import com.actelion.research.table.view.VisualizationPanel2D;
import com.actelion.research.table.view.VisualizationPanel3D;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.util.Properties;


public class DETaskPCA extends ConfigurableTask {
    public static final long serialVersionUID = 0x20060904;

    public static final String TASK_NAME = "Calculate Principal Components";

	private static final String PROPERTY_COLUMN_LIST = "columnList";
	private static final String PROPERTY_COMPONENT_COUNT = "componentCount";
	private static final String PROPERTY_CREATE_VIEWS = "createViews";
	private static final String PROPERTY_NEW_EIGEN_VALUE_WINDOW = "newEigenValueWindow";

    private DEFrame				mParentFrame,mTargetFrame;
	private CompoundTableModel  mTableModel;
	private JComboBox			mComboBoxNoOfComponents;
	private JCheckBox			mCheckBoxCreateViews,mCheckBoxNewEigenValueWindow;
	private JList				mListColumns;
	private JTextArea			mTextArea;
	private boolean				mIsInteractive;
	private int					mFullDataRowCount;
	private int[]				mFullDataRow;

	public DETaskPCA(DEFrame parent, boolean isInteractive) {
		super(parent, true);
		mParentFrame = parent;
		mIsInteractive = isInteractive;
		mTableModel = parent.getTableModel();
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return mTargetFrame;
		}

	@Override
    public String getTaskName() {
    	return TASK_NAME;
    	}

	@Override
	public boolean isConfigurable() {
		if (getDescriptorCount() == 0 && getParameterCount() < 3) {
			showErrorMessage("PCA needs either a chemical descriptor or at least 3 columns with varying numerical values.");
			return false;
			}

        return true;
		}

	private boolean qualifiesAsParameter(int column) {
		if (mTableModel.isDescriptorColumn(column)) {
			String shortName = mTableModel.getColumnSpecialType(column);
			return DescriptorHelper.isBinaryFingerprint(shortName)
				|| shortName.equals(DescriptorConstants.DESCRIPTOR_SkeletonSpheres.shortName);
			}

        return mTableModel.isColumnTypeDouble(column)
            && mTableModel.hasNumericalVariance(column);
		}

	private int getParameterCount() {
		int count = 0;
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (!mTableModel.isDescriptorColumn(column) && qualifiesAsParameter(column))
				count++;

		return count;
		}

	private int getDescriptorCount() {
		int count = 0;
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.isDescriptorColumn(column) && qualifiesAsParameter(column))
				count++;

		return count;
		}

	@Override
    public JPanel createDialogContent() {
        JPanel p1 = new JPanel();
		int space = HiDPIHelper.scale(8);
		double[][] size = { {space, TableLayout.PREFERRED, space/2, TableLayout.PREFERRED, space/2, TableLayout.PREFERRED, space},
				{space, TableLayout.PREFERRED, space, HiDPIHelper.scale(128), space, TableLayout.PREFERRED, space/2, TableLayout.PREFERRED, space} };
        p1.setLayout(new TableLayout(size));

		final String[] optionList = {"1", "2", "3", "4", "5", "6", "7", "8"};
        mComboBoxNoOfComponents = new JComboBox(optionList);
		mComboBoxNoOfComponents.setSelectedIndex(2);
        p1.add(new JLabel("No of principal components to calculate:"), "1,1,3,1");
        p1.add(mComboBoxNoOfComponents, "5,1");

        int index = 0;
		int optionCount = getDescriptorCount() + getParameterCount();
		String[] columnNameList = new String[optionCount];
        for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.isDescriptorColumn(column) && qualifiesAsParameter(column))
				columnNameList[index++] = mTableModel.getColumnTitle(column);

		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (!mTableModel.isDescriptorColumn(column) && qualifiesAsParameter(column))
				columnNameList[index++] = mTableModel.getColumnTitle(column);

		JLabel parameterLabel = new JLabel("Used parameters:");
		parameterLabel.setVerticalAlignment(SwingConstants.TOP);
		
		JScrollPane scrollPane = null;
		if (mIsInteractive) {
			mListColumns = new JList(columnNameList);
			scrollPane = new JScrollPane(mListColumns);
			}
		else {
			mTextArea = new JTextArea();
			scrollPane = new JScrollPane(mTextArea);
			}
		scrollPane.setPreferredSize(new Dimension(HiDPIHelper.scale(240),HiDPIHelper.scale(160)));
		p1.add(parameterLabel, "1,3");
		p1.add(scrollPane, "3,3,5,3");

        mCheckBoxCreateViews = new JCheckBox("Automatically create 2D and 3D views");
        p1.add(mCheckBoxCreateViews, "1,5,5,5");

		mCheckBoxNewEigenValueWindow = new JCheckBox("Open new window with eigenvalues");
		p1.add(mCheckBoxNewEigenValueWindow, "1,7,5,7");

        return p1;
	    }

	@Override
	public String getHelpURL() {
		return "/html/help/ml.html#PCA";
		}

	@Override
    public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		String columnNames = mIsInteractive ?
				  getSelectedColumnsFromList(mListColumns, mTableModel)
				: mTextArea.getText().replace('\n', '\t');
		if (columnNames != null && columnNames.length() != 0)
			configuration.setProperty(PROPERTY_COLUMN_LIST, columnNames);

		configuration.put(PROPERTY_COMPONENT_COUNT, mComboBoxNoOfComponents.getSelectedItem());
		configuration.put(PROPERTY_CREATE_VIEWS, mCheckBoxCreateViews.isSelected() ? "true" : "false");
		configuration.put(PROPERTY_NEW_EIGEN_VALUE_WINDOW, mCheckBoxNewEigenValueWindow.isSelected() ? "true" : "false");

		return configuration;
		}

	@Override
    public void setDialogConfiguration(Properties configuration) {
    	String value = configuration.getProperty(PROPERTY_COMPONENT_COUNT, "3");
    	if (value != null)
    		mComboBoxNoOfComponents.setSelectedItem(value);

		String columnNames = configuration.getProperty(PROPERTY_COLUMN_LIST, "");
		if (mIsInteractive)
			selectColumnsInList(mListColumns, columnNames, mTableModel);
		else
			mTextArea.setText(columnNames.replace('\t', '\n'));

		mCheckBoxCreateViews.setSelected("true".equals(configuration.getProperty(PROPERTY_CREATE_VIEWS, "true")));
		mCheckBoxNewEigenValueWindow.setSelected("true".equals(configuration.getProperty(PROPERTY_NEW_EIGEN_VALUE_WINDOW, "false")));
		}

	@Override
    public void setDialogConfigurationToDefault() {
		mComboBoxNoOfComponents.setSelectedItem("3");

		if (mIsInteractive)
			mListColumns.clearSelection();
		else
			mTextArea.setText("");

		mCheckBoxCreateViews.setSelected(true);
		mCheckBoxNewEigenValueWindow.setSelected(false);
		}

	@Override
    public boolean isConfigurationValid(Properties configuration, boolean isLive) {
	    int descriptorCount = 0;
	    int parameterCount = 0;
    	String columnList = configuration.getProperty(PROPERTY_COLUMN_LIST);
    	if (columnList == null) {
            showErrorMessage("Column list not defined.");
            return false;
    		}

    	if (isLive) {
	    	String[] columnName = columnList.split("\\t");
	    	int[] column = new int[columnName.length];
	
	    	for (int i=0; i<columnName.length; i++) {
				column[i] = mTableModel.findColumn(columnName[i]);
				if (column[i] == -1) {
		            showErrorMessage("Column '"+columnName+"' not found.");
		            return false;
					}
	
				if (mTableModel.isDescriptorColumn(column[i])) {
		            if (!mTableModel.isDescriptorAvailable(column[i])) {
	                    showErrorMessage("Descriptor calculation has not finished yet: '"+mTableModel.getColumnTitle(column[i])+"'.");
	                    return false;
		                }
	
					descriptorCount++;
					}
				else {
					if (!qualifiesAsParameter(column[i])) {
						showErrorMessage("Parameter '"+columnName+"' is not a complete numerical column with varying values.");
						return false;
						}
	
					parameterCount++;
					}
				}

	    	if (descriptorCount == 0 && parameterCount < 3) {
				showErrorMessage("A PCA needs at least 3 regular columns or one chemical descriptor.");
			    return false;
				}
			if (countFullDataRows(column) < 3) {
				showErrorMessage("Less than 3 usable rows. All other rows have at least one empty value.");
			    return false;
				}
    		}

		return true;
    	}

	private int countFullDataRows(int[] column) {
		mFullDataRowCount = 0;
		mFullDataRow = new int[mTableModel.getTotalRowCount()];
		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			boolean isEmpty = false;
			for (int i=0; i<column.length; i++) {
				CompoundRecord record = mTableModel.getTotalRecord(row);
				if (mTableModel.isDescriptorColumn(column[i])) {
					if (record.getData(column[i]) == null) {
						isEmpty = true;
						break;
						}
					}
				else {
					if (Float.isNaN(record.getDouble(column[i]))) {
						isEmpty = true;
						break;
						}
					}
				}
			if (!isEmpty) {
				mFullDataRow[mFullDataRowCount++] = row;
				}
			}

		if (mFullDataRowCount < 3)
			mFullDataRow = null;

		return mFullDataRowCount;
		}

	@Override
	public void runTask(Properties configuration) {
    	final String[] columnName = configuration.getProperty(PROPERTY_COLUMN_LIST).split("\\t");

    	int descriptorCount = 0;
	    int regularCount = 0;
		for (int i=0; i<columnName.length; i++) {
			int column = mTableModel.findColumn(columnName[i]);
			if (mTableModel.isDescriptorColumn(column)) {
				descriptorCount++;
	            waitForDescriptor(mTableModel, column);
				if (threadMustDie())
					return;
				}
			else {
				regularCount++;
				}
			}

		final int[] descriptorColumn = new int[descriptorCount];
		final int[] regularColumn = new int[regularCount];
	    descriptorCount = 0;
	    regularCount = 0;
		for (int i=0; i<columnName.length; i++) {
			int column = mTableModel.findColumn(columnName[i]);
			if (mTableModel.isDescriptorColumn(column))
				descriptorColumn[descriptorCount++] = column;
			else
				regularColumn[regularCount++] = column;
			}

		int[] varyingBits = new int[descriptorCount];
		Object[] varyingKey = new Object[descriptorCount];
		for (int fp=0; fp<descriptorCount; fp++) {
			startProgress("Analysing '"+mTableModel.getColumnTitle(descriptorColumn[fp])+"'...", 0, 0);
        	if (mTableModel.getDescriptorHandler(descriptorColumn[fp]).getInfo().isBinary) {
        		if (mTableModel.getTotalRecord(mFullDataRow[0]).getData(descriptorColumn[fp]) instanceof long[]) {
			        long[] firstIndex = (long[])mTableModel.getTotalRecord(mFullDataRow[0]).getData(descriptorColumn[fp]);
			        varyingKey[fp] = new long[firstIndex.length];
			        for (int r=1; r<mFullDataRowCount; r++) {
				        long[] currentIndex = (long[])mTableModel.getTotalRecord(mFullDataRow[r]).getData(descriptorColumn[fp]);
				        for (int i=0; i<firstIndex.length; i++)
					        ((long[]) varyingKey[fp])[i] |= (firstIndex[i] ^ currentIndex[i]);
			            }

			        for (int i=0; i<firstIndex.length; i++)
				        varyingBits[fp] += Long.bitCount(((long[]) varyingKey[fp])[i]);
		            }
		        else {
			        int[] firstIndex = (int[])mTableModel.getTotalRecord(mFullDataRow[0]).getData(descriptorColumn[fp]);
			        varyingKey[fp] = new int[firstIndex.length];
			        for (int r=1; r<mFullDataRowCount; r++) {
				        int[] currentIndex = (int[])mTableModel.getTotalRecord(mFullDataRow[r]).getData(descriptorColumn[fp]);
				        for (int i = 0; i < firstIndex.length; i++)
					        ((int[]) varyingKey[fp])[i] |= (firstIndex[i] ^ currentIndex[i]);
			            }

			        for (int i=0; i<firstIndex.length; i++)
				        varyingBits[fp] += Integer.bitCount(((int[])varyingKey[fp])[i]);
		            }

				regularCount += varyingBits[fp];
        		}
        	else {
				byte[] firstIndex = (byte[])mTableModel.getTotalRecord(mFullDataRow[0]).getData(descriptorColumn[fp]);
				boolean[] isVarying = new boolean[firstIndex.length];
				int varyingKeyCount = 0;
				for (int r=1; r<mFullDataRowCount; r++) {
					byte[] currentIndex = (byte[])mTableModel.getTotalRecord(mFullDataRow[r]).getData(descriptorColumn[fp]);
					for (int i=0; i<firstIndex.length; i++) {
						if (!isVarying[i] && firstIndex[i] != currentIndex[i]) {
							isVarying[i] = true;
							varyingKeyCount++;
							}
						}
					}
	
				varyingKey[fp] = new int[varyingKeyCount];
				int index = 0;
				for (int i=0; i<isVarying.length; i++)
					if (isVarying[i])
						((int[])varyingKey[fp])[index++] = i;

				regularCount += varyingKeyCount;
        		}
			}

		double[] rowParameter = new double[regularCount];

		startProgress("Calculating mean parameters...", 0, mFullDataRowCount);
		double[] meanParameter = new double[regularCount];
		for (int r=0; r<mFullDataRowCount; r++) {
			if (threadMustDie())
				break;
			updateProgress(r);

			calculateParameterRow(descriptorColumn, regularColumn, mFullDataRow[r], varyingKey, rowParameter);

			for (int i=0; i<regularCount; i++)
				meanParameter[i] += rowParameter[i];
			}
		if (!threadMustDie())
			for (int i=0; i<regularCount; i++)
				meanParameter[i] /= (double)mFullDataRowCount;

		startProgress("Calculating variance...", 0, mFullDataRowCount);
		double[] variance = new double[regularCount];
		for (int r=0; r<mFullDataRowCount; r++) {
			if (threadMustDie())
				break;
			updateProgress(r);

			calculateParameterRow(descriptorColumn, regularColumn, mFullDataRow[r], varyingKey, rowParameter);

			for (int i=0; i<regularCount; i++) {
				double dif = (rowParameter[i] - meanParameter[i]);
				variance[i] += dif * dif;
				}
			}
		if (!threadMustDie())
			for (int i=0; i<regularCount; i++)
				variance[i] /= (double)(mFullDataRowCount-1);

		startProgress("Building parameter matrix...", 0, mFullDataRowCount);

		double[][] squareMatrix = new double[regularCount][regularCount];
		for (int r=0; r<mFullDataRowCount; r++) {
			if (threadMustDie())
				break;
			updateProgress(r);

			calculateParameterRow(descriptorColumn, regularColumn, mFullDataRow[r], varyingKey, rowParameter);
			for (int i=0; i<regularCount; i++)
				rowParameter[i] = (rowParameter[i] - meanParameter[i]) / Math.sqrt(variance[i]);

			for (int i=0; i<regularCount; i++)
				for (int j=0; j<regularCount; j++)
					squareMatrix[i][j] += rowParameter[i] * rowParameter[j];
			}

		for (int i=0; i<regularCount; i++)
			for (int j=0; j<regularCount; j++)
				squareMatrix[i][j] /= (mFullDataRowCount-1);

			// Principle component analysis
		double[][] eigenVectorsLeft = null;
		double[][] factorArray = null;
		SingularValueDecomposition svd = null;

		if (!threadMustDie()) {
			svd = new SingularValueDecomposition(squareMatrix, this, this);
			}

		if (!threadMustDie()) {
			eigenVectorsLeft = svd.getU();
//			for (int i=0; i<eigenVectorsLeft.length; i++)
//				System.out.println("EigenVectorsLeft["+i+"][0]"+i+": "+(eigenVectorsLeft[i][0]));
			}

		int pcaCount = 3;
    	try { pcaCount = Integer.parseInt(configuration.getProperty(PROPERTY_COMPONENT_COUNT, "3")); } catch (NumberFormatException nfe) {}

		if (!threadMustDie()) {
			factorArray = new double[mFullDataRowCount][pcaCount];
			int eigenValueN = (eigenVectorsLeft.length == 0) ? 0 : eigenVectorsLeft[0].length;
			if (pcaCount > eigenValueN)
				pcaCount = eigenValueN;

			startProgress("Multiplying descriptors with eigenvalues...", 0, mFullDataRowCount);
			for (int r=0; r<mFullDataRowCount; r++) {
				if (threadMustDie())
					break;
				updateProgress(r);

				calculateParameterRow(descriptorColumn, regularColumn, mFullDataRow[r], varyingKey, rowParameter);
				for (int i=0; i<regularCount; i++)
					rowParameter[i] = (rowParameter[i] - meanParameter[i]) / Math.sqrt(variance[i]);

				for (int i=0; i<pcaCount; i++)
					for (int j=0; j<regularCount; j++)
						factorArray[r][i] += rowParameter[j] * eigenVectorsLeft[j][i];
				}
			}

		if (!threadMustDie()) {
			String[] columnTitle = new String[pcaCount];
			for (int i=0; i<pcaCount; i++)
				columnTitle[i] = "pc"+(i+1);
			final int firstNewColumn = mTableModel.addNewColumns(columnTitle);
			for (int i=0; i<pcaCount; i++)
				for (int r=0; r<mFullDataRowCount; r++)
					mTableModel.setTotalValueAt(""+factorArray[r][i], mFullDataRow[r], firstNewColumn+i);

			mTableModel.finalizeNewColumns(firstNewColumn, this);

			if (pcaCount >= 2 && "true".equals(configuration.getProperty(PROPERTY_CREATE_VIEWS, "true"))) {
				final int bestCorrelatingColumn2D = (descriptorCount != 0) ? descriptorColumn[0]
						: findBestCorrelatingColumn(regularColumn, eigenVectorsLeft, 2);
				final int bestCorrelatingColumn3D = (pcaCount < 3) ? -1
												  : (descriptorCount != 0) ? descriptorColumn[0]
						: findBestCorrelatingColumn(regularColumn, eigenVectorsLeft, 3);

				final int dimensions = pcaCount;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						DEMainPane mainPane = mParentFrame.getMainFrame().getMainPane();
	                    VisualizationPanel2D vpanel1 = mainPane.add2DView("2D-PCA", null);
	                    vpanel1.setAxisColumnName(0, mTableModel.getColumnTitle(firstNewColumn));
	                    vpanel1.setAxisColumnName(1, mTableModel.getColumnTitle(firstNewColumn+1));
	                    vpanel1.getVisualization().setPreferredChartType(JVisualization.cChartTypeScatterPlot, -1, -1);
	                    int colorListMode = VisualizationColor.cColorListModeHSBLong;
	                    Color[] colorList = VisualizationColor.createColorWedge(Color.red, Color.blue, colorListMode, null);
	                    vpanel1.getVisualization().getMarkerColor().setColor(bestCorrelatingColumn2D, colorList, colorListMode);

	                    if (dimensions >= 3) {
	                    	String title = mainPane.getDockable(vpanel1).getTitle();
		                    VisualizationPanel3D vpanel2 = mainPane.add3DView("3D-PCA", title+"\tright");
		                    vpanel2.setAxisColumnName(0, mTableModel.getColumnTitle(firstNewColumn));
		                    vpanel2.setAxisColumnName(1, mTableModel.getColumnTitle(firstNewColumn+1));
		                    vpanel2.setAxisColumnName(2, mTableModel.getColumnTitle(firstNewColumn+2));
		                    vpanel2.getVisualization().setPreferredChartType(JVisualization.cChartTypeScatterPlot, -1, -1);
		                    vpanel2.getVisualization().getMarkerColor().setColor(bestCorrelatingColumn3D, colorList, colorListMode);
	                    	}
						}
					} );
				}
			}

		mTargetFrame = null;

		if (!threadMustDie() && configuration.getProperty(PROPERTY_NEW_EIGEN_VALUE_WINDOW, "false").equals("true")) {
			final double[][] eigenValues = eigenVectorsLeft;
			final int _pcaCount = pcaCount;
			final int _paramCount = regularCount;
			mTargetFrame = mParentFrame.getApplication().getEmptyFrame("Eigenvalues of " + mParentFrame.getTitle());
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					CompoundTableModel targetTableModel = mTargetFrame.getTableModel();
					targetTableModel.initializeTable(_paramCount, 1 + _pcaCount);
					targetTableModel.setColumnName("Variable Name", 0);
					for (int column = 1; column <= _pcaCount; column++)
						targetTableModel.setColumnName("pc" + column, column);

					int row = 0;
					for (int d = 0; d < descriptorColumn.length; d++)
						for (int i = 0; i < varyingBits[d]; i++)
							targetTableModel.setTotalValueAt(mTableModel.getColumnTitle(descriptorColumn[d]) + " bit " + i, row++, 0);
					for (int i = 0; i < regularColumn.length; i++)
						targetTableModel.setTotalValueAt(mTableModel.getColumnTitle(regularColumn[i]), row++, 0);

					for (row = 0; row < _paramCount; row++)
						for (int i = 0; i < _pcaCount; i++)
							targetTableModel.setTotalValueAt(DoubleFormat.toString(eigenValues[row][i], 8), row, i + 1);

					targetTableModel.finalizeTable(CompoundTableEvent.cSpecifierNoRuntimeProperties, getProgressController());

//					SwingUtilities.invokeLater(new Runnable() {
//						public void run() {
							DEMainPane mainPane = mTargetFrame.getMainFrame().getMainPane();
							VisualizationPanel2D vpanel1 = mainPane.add2DView("Eigenvalues 2D", "Table\tbottom");
							vpanel1.setAxisColumnName(0, targetTableModel.getColumnTitle(1));
							vpanel1.setAxisColumnName(1, targetTableModel.getColumnTitle(2));
							vpanel1.getVisualization().setPreferredChartType(JVisualization.cChartTypeScatterPlot, -1, -1);
							vpanel1.getVisualization().addMarkerLabel(MarkerLabelConstants.cTopRight, 0);
							vpanel1.getVisualization().setMarkerLabelSize(2f, false);
							vpanel1.getVisualization().getMarkerColor().setDefaultDataColor(Color.BLUE);

							if (_pcaCount >= 3) {
								String title = mainPane.getDockable(vpanel1).getTitle();
								VisualizationPanel3D vpanel2 = mainPane.add3DView("Eigenvalues 3D", title+"\tright");
								vpanel2.setAxisColumnName(0, targetTableModel.getColumnTitle(1));
								vpanel2.setAxisColumnName(1, targetTableModel.getColumnTitle(2));
								vpanel2.setAxisColumnName(2, targetTableModel.getColumnTitle(3));
								vpanel2.getVisualization().setPreferredChartType(JVisualization.cChartTypeScatterPlot, -1, -1);
								vpanel1.getVisualization().addMarkerLabel(MarkerLabelConstants.cTopRight, 0);
								vpanel2.getVisualization().setMarkerLabelSize(2f, false);
								vpanel2.getVisualization().getMarkerColor().setDefaultDataColor(Color.BLUE);
								}
//							}
//						} );
					}
				} );
			}

		if (!threadMustDie()) {
			double[] singularValue = svd.getSingularValues();
			double sum = 0.0;
			for (int i=0; i<singularValue.length; i++)
				sum += singularValue[i];
			int maxIndex = Math.min(10, singularValue.length);
			StringBuffer sb = new StringBuffer();
			sb.append("<b>Explained variance percentage of<BR>the first "+maxIndex+" Principal Components</b><BR>");
			for (int i=0; i<maxIndex; i++)
				sb.append("Explained variance percentage of PC"+(i+1)+": "+((double)((int)(100000*singularValue[i]/sum))/1000)+"<BR>");

			// we need to wait until dialog is closed before continueing and moving target frame to front
			try { SwingUtilities.invokeAndWait(new PCADetailDialog(sb.toString())); } catch (Exception ie) {}
			}
		}

	private int findBestCorrelatingColumn(int[] regularColumn, double[][] eigenVectorsLeft, int dimensions) {
		double[] correlation = new double[regularColumn.length];
		for (int i=0; i<regularColumn.length; i++)
			for (int j=0; j<dimensions; j++)
				correlation[i] += Math.abs(eigenVectorsLeft[i][j]);

		int bestCorrelatingColumn = -1;
		double bestCorrelation = 0.0;
		for (int i=0; i<regularColumn.length; i++) {
			if (bestCorrelation < correlation[i]) {
				bestCorrelation = correlation[i];
				bestCorrelatingColumn = regularColumn[i];
				}
			}

		return bestCorrelatingColumn;
		}

	private void calculateParameterRow(int[] descriptorColumn, int[] regularColumn, int row, Object[] varyingKey, double[] rowParameter) {
		int paramIndex = 0;

        for (int fp=0; fp<descriptorColumn.length; fp++) {
        	if (mTableModel.getDescriptorHandler(descriptorColumn[fp]).getInfo().isBinary) {
		        if (mTableModel.getTotalRecord(row).getData(descriptorColumn[fp]) instanceof long[]) {
					long[] currentIndex = (long[])mTableModel.getTotalRecord(row).getData(descriptorColumn[fp]);
					for (int i=0; i<currentIndex.length; i++) {
						long theBit = 1L;
						for (int j=0; j<64; j++) {
							if ((((long[])varyingKey[fp])[i] & theBit) != 0) {
								rowParameter[paramIndex++] = ((currentIndex[i] & theBit) != 0) ? 1.0 : 0.0;
								}
							theBit <<= 1;
							}
						}
		            }
				else {
			        int[] currentIndex = (int[])mTableModel.getTotalRecord(row).getData(descriptorColumn[fp]);
			        for (int i=0; i<currentIndex.length; i++) {
				        int theBit = 1;
				        for (int j=0; j<32; j++) {
					        if ((((int[])varyingKey[fp])[i] & theBit) != 0) {
						        rowParameter[paramIndex++] = ((currentIndex[i] & theBit) != 0) ? 1.0 : 0.0;
						        }
					        theBit <<= 1;
					        }
				        }
			        }
        		}
        	else {
				byte[] currentIndex = (byte[])mTableModel.getTotalRecord(row).getData(descriptorColumn[fp]);
				for (int i=0; i<((int[])varyingKey[fp]).length; i++)
					rowParameter[paramIndex++] = currentIndex[((int[])varyingKey[fp])[i]];
        		}
			}

		for (int i=0; i<regularColumn.length; i++) {
			int column = regularColumn[i];
			rowParameter[paramIndex++] = mTableModel.getTotalDoubleAt(row, column);
			}
		}

	private class PCADetailDialog implements Runnable {
		private String mMessage;
		public PCADetailDialog(String message) {
			mMessage = message;
			}

		public void run() {
			JEditorPane textArea = new JEditorPane();
			textArea.setEditorKit(new HTMLEditorKit());
			textArea.setEditable(false);
			textArea.setBorder(BorderFactory.createLineBorder(Color.gray));
			textArea.setText(mMessage);
			JScrollPane sp = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			sp.setPreferredSize(new Dimension(400, 200));

			JOptionPane.showMessageDialog(getParentFrame(), sp, "Principal Component Analysis", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}
