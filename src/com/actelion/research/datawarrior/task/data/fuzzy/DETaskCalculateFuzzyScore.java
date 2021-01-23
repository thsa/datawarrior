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

package com.actelion.research.datawarrior.task.data.fuzzy;

import com.actelion.research.chem.SortedStringList;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.chem.prediction.MolecularPropertyHelper;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.ConfigurableTask;
import com.actelion.research.gui.VerticalFlowLayout;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundRecord;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.DoubleFormat;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;


public class DETaskCalculateFuzzyScore extends ConfigurableTask implements ActionListener {
	public static final String TASK_NAME = "Calculate Fuzzy Score";

	private static final String PROPERTY_MEAN_COLUMN_NAME = "meanColumnName";
	private static final String PROPERTY_PRODUCT_COLUMN_NAME = "productColumnName";
	private static final String PROPERTY_STRUCTURE_COLUMN = "structureColumn";
	private static final String PROPERTY_INDIVIDUAL_SCORES = "individualScores";
	private static final String PROPERTY_PARAM_COUNT = "paramCount";
	private static final String PROPERTY_PARAM_WEIGHT = "paramWeight";
	private static final String PROPERTY_PARAM_COLUMN_NAME = "paramColumn";
	private static final String PROPERTY_PARAM_PROPERTY = "paramProperty";
	private static final String PROPERTY_PARAM_MIN = "paramMin";
	private static final String PROPERTY_PARAM_MAX = "paramMax";
	private static final String PROPERTY_PARAM_HALFWIDTH = "paramHalfWidth";
	private static final String PROPERTY_PARAM_LOGARITHMIC = "paramLogarithmic";

	private static final String CALCULATED = " (computed)";

	private static Properties sRecentConfiguration;

	private DEFrame				mSourceFrame;
	private CompoundTableModel	mTableModel;
	private JComboBox			mComboBoxParameter,mComboBoxStructureColumn;
	private JTextField			mTextFieldMeanName, mTextFieldGeomName;
	private JCheckBox			mCheckBoxMean, mCheckBoxGeom,mCheckBoxIndividualScores;
	private JPanel				mParameterPanel;
	private JScrollPane			mParameterScrollpane;

	public DETaskCalculateFuzzyScore(DEFrame sourceFrame) {
		super(sourceFrame, true);
		mSourceFrame = sourceFrame;
		mTableModel = mSourceFrame.getTableModel();
	}

	@Override
	public Properties getRecentConfiguration() {
		return sRecentConfiguration;
	}

	@Override
	public void setRecentConfiguration(Properties configuration) {
		sRecentConfiguration = configuration;
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public String getHelpURL() {
		return "/html/help/data.html#Fuzzy";
	}

	@Override
	public boolean isConfigurable() {
		return true;
	}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
	}

	@Override
	public JPanel createDialogContent() {
		int scaled4 = HiDPIHelper.scale(4);
		int scaled8 = HiDPIHelper.scale(8);
		int scaled16 = HiDPIHelper.scale(16);
		JPanel content = new JPanel();
		double[][] size = { {scaled8, TableLayout.FILL, TableLayout.PREFERRED, scaled8, TableLayout.PREFERRED, scaled8},
				{scaled8, TableLayout.PREFERRED, scaled16, TableLayout.PREFERRED, scaled4, TableLayout.PREFERRED, scaled4,
				TableLayout.PREFERRED, scaled16, TableLayout.PREFERRED, scaled16, TableLayout.PREFERRED, scaled4, TableLayout.FILL, scaled8} };
		content.setLayout(new TableLayout(size));

		mComboBoxStructureColumn = new JComboBox();
		for (int i=0; i<mTableModel.getTotalColumnCount(); i++)
			if (mTableModel.isColumnTypeStructure(i))
				mComboBoxStructureColumn.addItem(mTableModel.getColumnTitle(i));
		content.add(new JLabel("Structure used for computed properties:"), "1,1");
		content.add(mComboBoxStructureColumn, "2,1");
		if (!isInteractive())
			mComboBoxStructureColumn.setEditable(true);

		mCheckBoxMean = new JCheckBox("Mean of individual ratings", true);
		mCheckBoxMean.addActionListener(this);
		mCheckBoxGeom = new JCheckBox("Geometrical mean of ratings", false);
		mCheckBoxGeom.addActionListener(this);
		mTextFieldMeanName = new JTextField("Fuzzy Score (mean)");
		mTextFieldGeomName = new JTextField("Fuzzy Score (geom)");
		mTextFieldGeomName.setEnabled(false);
		content.add(new JLabel("Summary Method"), "1,3");
		content.add(new JLabel("Fuzzy score column titles"), "2,3");
		content.add(mCheckBoxMean, "1,5");
		content.add(mTextFieldMeanName, "2,5");
		content.add(mCheckBoxGeom, "1,7");
		content.add(mTextFieldGeomName, "2,7");

		mCheckBoxIndividualScores = new JCheckBox("Create columns with individual scores also");
		content.add(mCheckBoxIndividualScores, "1,9,2,9");

		SortedStringList parameterList = new SortedStringList();
		for (int column=0; column<mTableModel.getTotalColumnCount(); column++)
			if (mTableModel.isColumnTypeDouble(column) && mTableModel.hasNumericalVariance(column))
				parameterList.addString(mTableModel.getColumnTitle(column));
		if (!isInteractive() || mComboBoxStructureColumn.getItemCount() != 0)
			for (int i=0; i< MolecularPropertyHelper.getPropertyCount(); i++)
				parameterList.addString(MolecularPropertyHelper.getPropertyName(i)+CALCULATED);
		String[] paramNames = parameterList.toArray();
		Arrays.sort(paramNames, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				if (o1.endsWith(CALCULATED) ^ o2.endsWith(CALCULATED))
					return o1.endsWith(CALCULATED) ? 1 : -1;
				return o1.compareToIgnoreCase(o2);
			}
		});
		mComboBoxParameter = new JComboBox(paramNames);
		if (!isInteractive())
			mComboBoxParameter.setEditable(true);

		content.add(new JLabel("Contributing Values"), "1,11");
		content.add(mComboBoxParameter, "2,11");
		JButton bAdd = new JButton("Add Value");
		if (isInteractive() && paramNames.length == 0) {
			bAdd.setEnabled(false);
			}
		else {
			bAdd.setActionCommand("addValue");
			bAdd.addActionListener(this);
			}
		content.add(bAdd, "4,11");

		mParameterPanel = new JPanel() {
			private static final long serialVersionUID = 0x20140724;

			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);

				if (getComponentCount() == 0) {
					Dimension theSize = getSize();
					Insets insets = getInsets();
					theSize.width -= insets.left + insets.right;
					theSize.height -= insets.top + insets.bottom;

					g.setColor(Color.GRAY);
//	    	        g.setFont(new Font("Helvetica", Font.PLAIN, 10));
					FontMetrics metrics = g.getFontMetrics();
					final String message = "<to add values to the fuzzy score click 'Add Value'>";
					Rectangle2D bounds = metrics.getStringBounds(message, g);
					g.drawString(message, (int)(insets.left+theSize.width-bounds.getWidth())/2,
							(insets.top+theSize.height-metrics.getHeight())/2+metrics.getAscent());
				}

				Rectangle r = new Rectangle();
				g.setColor(Color.GRAY);
				for (int i=1; i<getComponentCount(); i++) {
					getComponent(i).getBounds(r);
					g.drawLine(r.x+2, r.y-3, r.x+r.width-3, r.y-3);
					g.drawLine(r.x+2, r.y-2, r.x+r.width-3, r.y-2);
				}
			}
		};
		Dimension parameterPanelSize = new Dimension(HiDPIHelper.scale(600), HiDPIHelper.scale(420));
		mParameterPanel.setLayout(new VerticalFlowLayout());
		mParameterScrollpane = new JScrollPane(mParameterPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		mParameterScrollpane.setMinimumSize(parameterPanelSize);
		mParameterScrollpane.setPreferredSize(parameterPanelSize);
		content.add(mParameterScrollpane, "1,13,4,13");

		return content;
	}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		String structureColumn = (String) mComboBoxStructureColumn.getSelectedItem();
		if (structureColumn != null && structureColumn.length() != 0)
				configuration.setProperty(PROPERTY_STRUCTURE_COLUMN, isInteractive() ?
						mTableModel.getColumnTitleNoAlias(structureColumn) : structureColumn);

		if (mCheckBoxMean.isSelected())
			configuration.setProperty(PROPERTY_MEAN_COLUMN_NAME, mTextFieldMeanName.getText());
		if (mCheckBoxGeom.isSelected())
			configuration.setProperty(PROPERTY_PRODUCT_COLUMN_NAME, mTextFieldGeomName.getText());
		if (mCheckBoxIndividualScores.isSelected())
			configuration.setProperty(PROPERTY_INDIVIDUAL_SCORES, "true");

		int parameterCount = mParameterPanel.getComponentCount();
		configuration.setProperty(PROPERTY_PARAM_COUNT, Integer.toString(parameterCount));
		for (int i=0; i<parameterCount; i++) {
			FussyParameterPanel fp = (FussyParameterPanel)mParameterPanel.getComponent(i);

			int property = fp.getProperty();
			if (property != -1)
				configuration.setProperty(PROPERTY_PARAM_PROPERTY+i, MolecularPropertyHelper.getPropertyCode(property));
			int column = fp.getColumn();
			if (column != -1)
				configuration.setProperty(PROPERTY_PARAM_COLUMN_NAME+i, mTableModel.getColumnTitleNoAlias(column));

			configuration.setProperty(PROPERTY_PARAM_LOGARITHMIC+i, fp.isLogarithmic() ? "true" : "false");
			configuration.setProperty(PROPERTY_PARAM_MIN+i, fp.getMinimum());
			configuration.setProperty(PROPERTY_PARAM_MAX+i, fp.getMaximum());
			configuration.setProperty(PROPERTY_PARAM_HALFWIDTH+i, Double.toString(fp.getHalfWidth()));
			configuration.setProperty(PROPERTY_PARAM_WEIGHT+i, DoubleFormat.toString(fp.getWeight(), 2));
		}

		return configuration;
	}

	@Override
	public void setDialogConfigurationToDefault() {
		if (mComboBoxStructureColumn.getItemCount() != 0)
			mComboBoxStructureColumn.setSelectedIndex(0);
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		String structureColumnName = configuration.getProperty(PROPERTY_STRUCTURE_COLUMN, "");
		int structureColumn = mTableModel.findColumn(structureColumnName);
		if (structureColumn != -1)
			mComboBoxStructureColumn.setSelectedItem(mTableModel.getColumnTitle(structureColumn));

		String meanColumnName = configuration.getProperty(PROPERTY_MEAN_COLUMN_NAME, "");
		mCheckBoxMean.setSelected(meanColumnName.length() != 0);
		mTextFieldMeanName.setEnabled(meanColumnName.length() != 0);
		if (meanColumnName.length() != 0)
			mTextFieldMeanName.setText(meanColumnName);

		String productColumnName = configuration.getProperty(PROPERTY_PRODUCT_COLUMN_NAME, "");
		mCheckBoxGeom.setSelected(productColumnName.length() != 0);
		mTextFieldGeomName.setEnabled(productColumnName.length() != 0);
		if (productColumnName.length() != 0)
			mTextFieldGeomName.setText(productColumnName);

		mCheckBoxIndividualScores.setSelected(configuration.getProperty(PROPERTY_INDIVIDUAL_SCORES, "false").equals("true"));

		int parameterCount = Integer.parseInt(configuration.getProperty(PROPERTY_PARAM_COUNT, "0"));
		for (int i=0; i<parameterCount; i++) {
			String columnName = configuration.getProperty(PROPERTY_PARAM_COLUMN_NAME+i, "");
			int column = mTableModel.findColumn(columnName);
			String propertyName = configuration.getProperty(PROPERTY_PARAM_PROPERTY+i, "");
			int property = MolecularPropertyHelper.getTypeFromCode(propertyName);

			if (column != -1 || property != -1) {
				boolean logarithmic = "true".equals(configuration.getProperty(PROPERTY_PARAM_LOGARITHMIC + i));
				if (isInteractive() && column != -1)
					logarithmic = mTableModel.isLogarithmicViewMode(column);

				String min = configuration.getProperty(PROPERTY_PARAM_MIN + i, "");
				String max = configuration.getProperty(PROPERTY_PARAM_MAX + i, "");
				String halfWidth = configuration.getProperty(PROPERTY_PARAM_HALFWIDTH + i);
				String weight = configuration.getProperty(PROPERTY_PARAM_WEIGHT + i, "1.0");

				FussyParameterPanel fuzzyPanel = new FussyParameterPanel(property, column, logarithmic, mTableModel, this);
				mParameterPanel.add(fuzzyPanel);
				fuzzyPanel.setMinimum(min);
				fuzzyPanel.setMaximum(max);
				if (halfWidth != null)
					fuzzyPanel.setHalfWidth(Double.parseDouble(halfWidth));
				fuzzyPanel.setWeight(Double.parseDouble(weight));
			}
		}
	}

	@Override
	public boolean isConfigurationValid(Properties configuration, boolean isLive) {
		String structureColumnName = configuration.getProperty(PROPERTY_STRUCTURE_COLUMN, "");

		String meanColumnName = configuration.getProperty(PROPERTY_MEAN_COLUMN_NAME, "");
		String productColumnName = configuration.getProperty(PROPERTY_PRODUCT_COLUMN_NAME, "");
		if (meanColumnName.length() == 0 && productColumnName.length() == 0) {
			showErrorMessage("No fuzzy score result columns defined.");
			return false;
		}

		int parameterCount = Integer.parseInt(configuration.getProperty(PROPERTY_PARAM_COUNT, "0"));
		if (parameterCount == 0) {
			showErrorMessage("No parameters defined.");
			return false;
		}

		for (int i=0; i<parameterCount; i++) {
			String columnName = configuration.getProperty(PROPERTY_PARAM_COLUMN_NAME+i, "");
			String propertyName = configuration.getProperty(PROPERTY_PARAM_PROPERTY+i, "");
			if (columnName.length() == 0 && propertyName.length() == 0) {
				showErrorMessage("Neither column name nor computable property name defined (parameter "+(i+1)+").");
				return false;
			}

			if (propertyName.length() != 0) {
				int property = MolecularPropertyHelper.getTypeFromCode(propertyName);
				if (property == -1) {
					showErrorMessage("Computable property '" + propertyName + "' not found.");
					return false;
				}
				if (structureColumnName.length() == 0) {
					showErrorMessage("Computable properties require a structure column.");
					return false;
				}
				if (isLive && mTableModel.findColumn(structureColumnName) == -1) {
					showErrorMessage("Structure column '"+structureColumnName+"' for computable property not found.");
					return false;
				}
			}

			String min = configuration.getProperty(PROPERTY_PARAM_MIN+i, "");
			if (min.length() != 0)
				try {
					Double.parseDouble(min);
				} catch (NumberFormatException nfe) {
					showErrorMessage("'Minimum' value must be empty or numerical (parameter "+(i+1)+").");
					return false;
				}
			String max = configuration.getProperty(PROPERTY_PARAM_MAX+i, "");
			if (max.length() != 0)
				try {
					Double.parseDouble(max);
				} catch (NumberFormatException nfe) {
					showErrorMessage("'Maximum' value must be empty or numerical (parameter "+(i+1)+").");
					return false;
				}

			if (isLive) {
				if (columnName.length() != 0) {
					int column = mTableModel.findColumn(columnName);
					if (column == -1) {
						showErrorMessage("Column '" + columnName + "' not found (parameter "+(i+1)+").");
						return false;
					}
				}
			}
		}

		return true;
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == mCheckBoxMean) {
			mTextFieldMeanName.setEnabled(mCheckBoxMean.isSelected());
			return;
		}

		if (e.getSource() == mCheckBoxGeom) {
			mTextFieldGeomName.setEnabled(mCheckBoxGeom.isSelected());
			return;
		}

		String name = (String)mComboBoxParameter.getSelectedItem();

		boolean isCalculated = name.endsWith(CALCULATED);

		int property = -1;
		int column = -1;

		if (isCalculated)
			property = MolecularPropertyHelper.getTypeFromName(name.substring(0, name.length() - CALCULATED.length()));
		else
			column = mTableModel.findColumn(name);

		boolean logarithmic = !isCalculated && mTableModel.isLogarithmicViewMode(column);
		FussyParameterPanel fuzzyPanel = new FussyParameterPanel(property, column, logarithmic, mTableModel, this);
		fuzzyPanel.setMinimum(column == -1 ? MolecularPropertyHelper.getPreferredMin(property) : "");
		fuzzyPanel.setMaximum(column == -1 ? MolecularPropertyHelper.getPreferredMax(property) : "");
		mParameterPanel.add(fuzzyPanel);
		mParameterScrollpane.validate();
		mParameterScrollpane.repaint();
	}

	@Override
	public void runTask(Properties configuration) {
		int structureColumn = mTableModel.findColumn(configuration.getProperty(PROPERTY_STRUCTURE_COLUMN));

		int parameterCount = Integer.parseInt(configuration.getProperty(PROPERTY_PARAM_COUNT));

		boolean computablePropertyFound = false;
		double[] min = new double[parameterCount];
		double[] max = new double[parameterCount];
		double[] halfWidth = new double[parameterCount];
		double[] weight = new double[parameterCount];
		boolean[] isLogarithmic = new boolean[parameterCount];
		int[] column = new int[parameterCount];
		int[] property = new int[parameterCount];

		for (int i=0; i<parameterCount; i++) {
			isLogarithmic[i] = "true".equals(configuration.getProperty(PROPERTY_PARAM_LOGARITHMIC + i));
			try {
				min[i] = Double.parseDouble(configuration.getProperty(PROPERTY_PARAM_MIN + i, ""));
				if (isLogarithmic[i])
					min[i] = (min[i] <= 0.0) ? Double.NaN : Math.log10(min[i]);
			} catch (NumberFormatException nfe) {
				min[i] = Double.NaN;
			}
			try {
				max[i] = Double.parseDouble(configuration.getProperty(PROPERTY_PARAM_MAX + i, ""));
				if (isLogarithmic[i])
					max[i] = (max[i] <= 0.0) ? Double.NaN : Math.log10(max[i]);
			} catch (NumberFormatException nfe) {
				max[i] = Double.NaN;
			}
			halfWidth[i] = Double.parseDouble(configuration.getProperty(PROPERTY_PARAM_HALFWIDTH + i));
			weight[i] = Double.parseDouble(configuration.getProperty(PROPERTY_PARAM_WEIGHT + i, "1.0"));

			String columnName = configuration.getProperty(PROPERTY_PARAM_COLUMN_NAME + i, "");
			column[i] = mTableModel.findColumn(columnName);

			String propertyCode = configuration.getProperty(PROPERTY_PARAM_PROPERTY + i, "");
			property[i] = MolecularPropertyHelper.getTypeFromCode(propertyCode);

			if (property[i] != -1)
				computablePropertyFound = true;
		}

		StereoMolecule mol = computablePropertyFound ? new StereoMolecule() : null;

		String meanColumnName = configuration.getProperty(PROPERTY_MEAN_COLUMN_NAME, "");
		String geomColumnName = configuration.getProperty(PROPERTY_PRODUCT_COLUMN_NAME, "");

		boolean addIndividualScores = configuration.getProperty(PROPERTY_INDIVIDUAL_SCORES, "false").equals("true");

		int columnCount = addIndividualScores ? parameterCount : 0;
		if (meanColumnName.length() != 0)
			columnCount++;
		if (geomColumnName.length() != 0)
			columnCount++;
		columnCount++;	// for Score Coverage

		String[] columnName = new String[columnCount];

		int index = 0;
		if (addIndividualScores)
			for (int i=0; i<parameterCount; i++)
				columnName[index++] = "Score of " + (column[i] == -1 ?
						MolecularPropertyHelper.getPropertyName(property[i]) : mTableModel.getColumnTitle(column[i]));

		if (meanColumnName.length() != 0)
			columnName[index++] = meanColumnName;
		if (geomColumnName.length() != 0)
			columnName[index++] = geomColumnName;

		columnName[index++] = "Score Coverage";

		int newColumn = mTableModel.addNewColumns(columnName);

		startProgress("Fuzzy scoring...", 0, mTableModel.getTotalRowCount());
		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			if (threadMustDie())
				break;
			updateProgress(row);

			CompoundRecord record = mTableModel.getTotalRecord(row);

			if (computablePropertyFound) {
				mol = mTableModel.getChemicalStructure(record, structureColumn, 0, mol);
				mol.stripSmallFragments();
			}

			double fuzzyMean = 0.0;
			double fuzzyGeom = 0.0;
			double weightSum = 0.0;
			double totalWeightSum = 0.0;
			boolean geomIsNull = false;
			int columnIndex = newColumn;
			for (int i=0; i<parameterCount; i++) {
				double value = column[i] != -1 ? mTableModel.getTotalDoubleAt(row, column[i])
						: MolecularPropertyHelper.calculateProperty(mol, property[i]);

				if (!Double.isNaN(value)) {
					double valuation = MolecularPropertyHelper.getValuation(value, min[i], max[i], halfWidth[i]);

					if (addIndividualScores)
						mTableModel.setTotalValueAt(formatScore(valuation), row, columnIndex);

					fuzzyMean += weight[i] * valuation;
					if (!geomIsNull) {
						if (valuation <= 0)
							geomIsNull = true;
						else
							fuzzyGeom += weight[i] * Math.log10(valuation);
					}
					weightSum += weight[i];
				}
				totalWeightSum += weight[i];
				if (addIndividualScores)
					columnIndex++;
			}

			if (weightSum != 0.0) {
				fuzzyMean /= weightSum;
				fuzzyGeom = geomIsNull ? 0.0 : Math.pow(10, fuzzyGeom / weightSum);
			}
			else {
				fuzzyMean = Double.NaN;
				fuzzyGeom = Double.NaN;
			}

			if (meanColumnName.length() != 0)
				mTableModel.setTotalValueAt(formatScore(fuzzyMean), row, columnIndex++);
			if (geomColumnName.length() != 0)
				mTableModel.setTotalValueAt(formatScore(fuzzyGeom), row, columnIndex++);
			mTableModel.setTotalValueAt(formatScore(weightSum/totalWeightSum), row, columnIndex++);
		}

		mTableModel.finalizeNewColumns(newColumn, getProgressController());
	}

	private String formatScore(double score) {
		return Double.isNaN(score) ? "" : Double.toString(Math.round(score*10000.0)/10000.0);
	}
}
