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

import com.actelion.research.chem.descriptor.DescriptorConstants;
import com.actelion.research.chem.descriptor.DescriptorHandlerReactionFP;
import com.actelion.research.chem.descriptor.DescriptorHelper;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.table.view.JVisualization;
import com.actelion.research.table.view.VisualizationColor;
import com.actelion.research.table.view.VisualizationPanel2D;
import com.actelion.research.table.view.VisualizationPanel3D;
import com.jujutsu.tsne.TSneConfiguration;
import com.jujutsu.tsne.barneshut.BarnesHutTSne;
import com.jujutsu.tsne.barneshut.ParallelBHTsne;
import com.jujutsu.utils.TSneUtils;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;


public class DETaskCreateTSNEVisualization extends ConfigurableTask {
    public static final long serialVersionUID = 0x20180802;

    public static final String TASK_NAME = "Create t-SNE Visualization";

	private static final String PROPERTY_COLUMN_LIST = "columnList";
	private static final String PROPERTY_DIMENSIONS = "dimensions";
	private static final String PROPERTY_PERPLEXITY = "perplexity";
	private static final String PROPERTY_PCADIMENSIONS = "pcadimensions";
	private static final String PROPERTY_ITERATIONS = "iterations";
	private static final String PROPERTY_CREATE_VIEW = "createView";

    private DEFrame				mParentFrame,mTargetFrame;
	private CompoundTableModel  mTableModel;
	private JComboBox			mComboBoxNoOfComponents;
	private JCheckBox			mCheckBoxCreateViews;
	private JList				mListColumns;
	private JTextArea			mTextArea;
	private JTextField			mTextFieldPerplexity,mTextFieldIterations,mTextFieldPCADimensions;
	private boolean				mIsInteractive;
	private int					mFullDataRowCount;
	private int[]				mFullDataRow;

	public DETaskCreateTSNEVisualization(DEFrame parent, boolean isInteractive) {
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
			showErrorMessage("A t-SNE visualization either needs a chemical descriptor or at least 3 numerical columns.");
			return false;
			}

        return true;
		}

	private boolean qualifiesAsParameter(int column) {
		if (mTableModel.isDescriptorColumn(column)) {
			String shortName = mTableModel.getColumnSpecialType(column);
			return DescriptorHelper.isBinaryFingerprint(shortName)
				|| shortName.equals(DescriptorConstants.DESCRIPTOR_ReactionFP.shortName)
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
        int gap = HiDPIHelper.scale(8);
        double[][] size = { {gap, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap/2, TableLayout.PREFERRED, gap},
                            {gap, TableLayout.PREFERRED, gap, HiDPIHelper.scale(128), gap,
									TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap} };
        p1.setLayout(new TableLayout(size));

		final String[] optionList = {"2", "3"};
        mComboBoxNoOfComponents = new JComboBox(optionList);
		mComboBoxNoOfComponents.setSelectedIndex(0);
        p1.add(new JLabel("No of target dimensions:"), "1,1,3,1");
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

		JLabel parameterLabel = new JLabel("Used columns:");
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
		scrollPane.setPreferredSize(new Dimension(HiDPIHelper.scale(240),HiDPIHelper.scale(180)));
		p1.add(parameterLabel, "1,3");
		p1.add(scrollPane, "3,3,5,3");

		mTextFieldPerplexity = new JTextField((6));
		p1.add(new JLabel("Perplexity:"), "1,5");
		p1.add(mTextFieldPerplexity, "3,5");

		mTextFieldPCADimensions = new JTextField((6));
		p1.add(new JLabel("Source dimensions:"), "1,7");
		p1.add(mTextFieldPCADimensions, "3,7");

		mTextFieldIterations = new JTextField((6));
		p1.add(new JLabel("Iterations:"), "1,9");
		p1.add(mTextFieldIterations, "3,9");

		mCheckBoxCreateViews = new JCheckBox("Automatically create 2D- or 3D-view");
        p1.add(mCheckBoxCreateViews, "1,11,5,11");

        return p1;
	    }

	@Override
	public String getHelpURL() {
		return "/html/help/me.html#TSNE";
		}

	@Override
    public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		String columnNames = mIsInteractive ?
				  getSelectedColumnsFromList(mListColumns, mTableModel)
				: mTextArea.getText().replace('\n', '\t');
		if (columnNames != null && columnNames.length() != 0)
			configuration.setProperty(PROPERTY_COLUMN_LIST, columnNames);

		configuration.put(PROPERTY_DIMENSIONS, mComboBoxNoOfComponents.getSelectedItem());
		configuration.put(PROPERTY_CREATE_VIEW, mCheckBoxCreateViews.isSelected() ? "true" : "false");

		configuration.put(PROPERTY_PERPLEXITY, mTextFieldPerplexity.getText());
		configuration.put(PROPERTY_PCADIMENSIONS, mTextFieldPCADimensions.getText());
		configuration.put(PROPERTY_ITERATIONS, mTextFieldIterations.getText());

		return configuration;
		}

	@Override
    public void setDialogConfiguration(Properties configuration) {
    	String value = configuration.getProperty(PROPERTY_DIMENSIONS, "3");
    	if (value != null)
    		mComboBoxNoOfComponents.setSelectedItem(value);

		String columnNames = configuration.getProperty(PROPERTY_COLUMN_LIST, "");
		if (mIsInteractive)
			selectColumnsInList(mListColumns, columnNames, mTableModel);
		else
			mTextArea.setText(columnNames.replace('\t', '\n'));

		mTextFieldPerplexity.setText(configuration.getProperty(PROPERTY_PERPLEXITY, "20.0"));
		mTextFieldPCADimensions.setText(configuration.getProperty(PROPERTY_PCADIMENSIONS, "50"));
		mTextFieldIterations.setText(configuration.getProperty(PROPERTY_ITERATIONS, "1000"));

		mCheckBoxCreateViews.setSelected("true".equals(configuration.getProperty(PROPERTY_CREATE_VIEW, "true")));
		}

	@Override
    public void setDialogConfigurationToDefault() {
		mComboBoxNoOfComponents.setSelectedItem("3");

		if (mIsInteractive)
			mListColumns.clearSelection();
		else
			mTextArea.setText("");

		mTextFieldPerplexity.setText("20.0");
		mTextFieldPCADimensions.setText("50");
		mTextFieldIterations.setText("1000");

		mCheckBoxCreateViews.setSelected(true);
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

		String perplexity = configuration.getProperty(PROPERTY_PERPLEXITY, "20");
    	try {
    		Float.parseFloat(perplexity);
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("Perplexity value is not numerical.");
			return false;
			}

		String dimensions = configuration.getProperty(PROPERTY_PCADIMENSIONS, "50");
		try {
			Integer.parseInt(dimensions);
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("Source dimensions value is not an integer.");
			return false;
			}

		String iterations = configuration.getProperty(PROPERTY_ITERATIONS, "1000");
		try {
			Integer.parseInt(iterations);
			}
		catch (NumberFormatException nfe) {
			showErrorMessage("Iterations value is not an integer.");
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
				showErrorMessage("A t-SNE visualization needs at least 3 numerical columns or one chemical descriptor.");
			    return false;
				}
			if (countFullDataRows(column) < 3) {
				showErrorMessage("Less than 3 usable rows. All other rows have empty values.");
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
					if (record.getData(column[i]) == null
					 || mTableModel.getDescriptorHandler(column[i]).calculationFailed(record.getData(column[i]))) {
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
        	if (mTableModel.getDescriptorHandler(descriptorColumn[fp]).getInfo().isBinary
	         || DescriptorConstants.DESCRIPTOR_ReactionFP.shortName.equals(mTableModel.getColumnSpecialType(descriptorColumn[fp]))) {
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

		int outputDims = 2;
		try { outputDims = Integer.parseInt(configuration.getProperty(PROPERTY_DIMENSIONS, "2")); } catch (NumberFormatException nfe) {}

		startProgress("Calculating t-SNE input array...", 0, mFullDataRowCount);
		double [][] X = new double[mFullDataRowCount][regularCount];
		for (int r=0; r<mFullDataRowCount; r++) {
			if (threadMustDie())
				break;
			updateProgress(r);

			calculateParameterRow(descriptorColumn, regularColumn, mFullDataRow[r], varyingKey, rowParameter);
			for (int i=0; i<regularCount; i++)
				X[r][i] = (rowParameter[i] - meanParameter[i]) / Math.sqrt(variance[i]);
			}

		double perplexity = Float.parseFloat(configuration.getProperty(PROPERTY_PERPLEXITY, "20.0"));
		int initial_dims = Integer.parseInt(configuration.getProperty(PROPERTY_PCADIMENSIONS, "50"));
		int iterations = Integer.parseInt(configuration.getProperty(PROPERTY_ITERATIONS, "1000"));
		double[][] Y = null;
		try {
			BarnesHutTSne tsne = new ParallelBHTsne();    // this is the single thread alternative: new BHTSne();

//BarnesHutTSne tsne = new BHTSne();	// for some small datasets the parallel implementation seems to hang; TLS 26-Oct-2018
// on 04-Apr-2019 arofab suggested a change in BarnesHutTSne that seems to solve the issue. Thus going back to parallel; TLS 09-Apr-2019

			TSneConfiguration config = TSneUtils.buildConfig(X, outputDims, initial_dims, perplexity, iterations);
			Y = tsne.tsne(config, this);
			}
		catch (Exception e) {
			showErrorMessage(e.getMessage());
			return;
			}

		if (!threadMustDie()) {
			String[] columnTitle = new String[outputDims];
			for (int i=0; i<outputDims; i++)
				columnTitle[i] = "t-SNE "+(char)('X'+i);
			final int firstNewColumn = mTableModel.addNewColumns(columnTitle);
			for (int i=0; i<outputDims; i++)
				for (int r=0; r<mFullDataRowCount; r++)
					mTableModel.setTotalValueAt(""+Y[r][i], mFullDataRow[r], firstNewColumn+i);

			mTableModel.finalizeNewColumns(firstNewColumn, this);

			if (outputDims >= 2 && "true".equals(configuration.getProperty(PROPERTY_CREATE_VIEW, "true"))) {
				int colorColumn = -1;
				for (int column=0; column<firstNewColumn; column++) {
					if (mTableModel.isColumnTypeDouble(column)) {
						colorColumn = column;
						break;
						}
					}
				if (colorColumn == -1) {
					for (int column=0; column<firstNewColumn; column++) {
						if (mTableModel.isColumnTypeCategory(column) && mTableModel.getCategoryCount(column) < VisualizationColor.cMaxColorCategories) {
							colorColumn = column;
							break;
							}
						}
					}

				final int dimensions = outputDims;
				final int _colorColumn = colorColumn;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						DEMainPane mainPane = mParentFrame.getMainFrame().getMainPane();

						int colorListMode = VisualizationColor.cColorListModeHSBLong;
						Color[] colorList = (_colorColumn == -1) ? null : VisualizationColor.createColorWedge(Color.red, Color.blue, colorListMode, null);

						if (dimensions == 2) {
							VisualizationPanel2D vpanel1 = mainPane.add2DView("t-SNE 2D", null);
							vpanel1.setAxisColumnName(0, mTableModel.getColumnTitle(firstNewColumn));
							vpanel1.setAxisColumnName(1, mTableModel.getColumnTitle(firstNewColumn + 1));
							vpanel1.getVisualization().setPreferredChartType(JVisualization.cChartTypeScatterPlot, -1, -1);
		                    if (_colorColumn != -1)
								vpanel1.getVisualization().getMarkerColor().setColor(_colorColumn, colorList, colorListMode);
							}
	                    if (dimensions == 3) {
		                    VisualizationPanel3D vpanel2 = mainPane.add3DView("t-SNE 3D", null);
		                    vpanel2.setAxisColumnName(0, mTableModel.getColumnTitle(firstNewColumn));
		                    vpanel2.setAxisColumnName(1, mTableModel.getColumnTitle(firstNewColumn+1));
		                    vpanel2.setAxisColumnName(2, mTableModel.getColumnTitle(firstNewColumn+2));
		                    vpanel2.getVisualization().setPreferredChartType(JVisualization.cChartTypeScatterPlot, -1, -1);
							if (_colorColumn != -1)
			                    vpanel2.getVisualization().getMarkerColor().setColor(_colorColumn, colorList, colorListMode);
	                    	}
						}
					} );
				}
			}
		}

	private void calculateParameterRow(int[] descriptorColumn, int[] regularColumn, int row, Object[] varyingKey, double[] rowParameter) {
		int paramIndex = 0;

        for (int fp=0; fp<descriptorColumn.length; fp++) {
	        if (DescriptorConstants.DESCRIPTOR_ReactionFP.shortName.equals(mTableModel.getColumnSpecialType(descriptorColumn[fp]))) {
		        long[] currentIndex = (long[])mTableModel.getTotalRecord(row).getData(descriptorColumn[fp]);
		        for (int i=0; i<currentIndex.length; i++) {
			        boolean isReactionCenter = (i < DescriptorHandlerReactionFP.REACTION_CENTER_LONG_COUNT);
			        long theBit = 1L;
			        for (int j=0; j<64; j++) {
				        if ((((long[])varyingKey[fp])[i] & theBit) != 0) {
					        rowParameter[paramIndex++] = ((currentIndex[i] & theBit) == 0) ? 0.0
						: isReactionCenter ? DescriptorHandlerReactionFP.REACTION_CENTER_WEIGHT : DescriptorHandlerReactionFP.PERIPHERY_WEIGHT;
					        }
				        theBit <<= 1;
				        }
			        }
		        }
	        else if (mTableModel.getDescriptorHandler(descriptorColumn[fp]).getInfo().isBinary) {
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

/*	private class TSNEDetailDialog implements Runnable {
		private String mMessage;
		public TSNEDetailDialog(String message) {
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

			JOptionPane.showMessageDialog(getParentFrame(), sp, "t-SNE Visualization", JOptionPane.INFORMATION_MESSAGE);
			}
		}*/
	}
