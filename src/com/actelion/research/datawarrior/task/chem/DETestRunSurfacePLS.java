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

package com.actelion.research.datawarrior.task.chem;

import com.actelion.research.calc.Matrix;
import com.actelion.research.calc.regression.ModelError;
import com.actelion.research.calc.regression.linear.pls.RegressionModelCalculatorOptimumFactors;
import com.actelion.research.chem.prediction.TotalSurfaceAreaPredictor;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.AbstractTaskWithoutConfiguration;
import com.actelion.research.table.model.CompoundTableModel;
import com.actelion.research.util.datamodel.ModelXYIndex;

import java.util.Properties;


public class DETestRunSurfacePLS extends AbstractTaskWithoutConfiguration {
    public static final long serialVersionUID = 0x20150812;

    private static final int MODE_POLAR = 0;
    private static final int MODE_NON_POLAR = 1;
    private static final int MODE_TOTAL = 2;
    private static final String[] SURFACE_COLUMN_TITLE =
    		{ "Polar Surface Area", "Non Polar Surface Area", "Total Surface Area" };

    public static final String TASK_NAME = "Run Surface PLS";

	private CompoundTableModel  mTableModel;

	public DETestRunSurfacePLS(DEFrame parent) {
		super(parent, true);
		mTableModel = parent.getTableModel();
		}

	@Override
	public DEFrame getNewFrontFrame() {
		return null;
		}

	@Override
    public String getTaskName() {
    	return TASK_NAME;
    	}

	@Override
	public boolean isConfigurable() {
        return true;
		}

	@Override
	public void runTask(Properties configuration) {
		int rowCount = mTableModel.getTotalRowCount();

		for (int mode=0; mode<3; mode++) {
			int parameterCount = (mode == MODE_POLAR) ? TotalSurfaceAreaPredictor.getPolarAtomTypeCount()
							   : (mode == MODE_TOTAL) ? TotalSurfaceAreaPredictor.getPolarAtomTypeCount()
									   				  + TotalSurfaceAreaPredictor.getNonPolarAtomTypeCount()
							   : 						TotalSurfaceAreaPredictor.getNonPolarAtomTypeCount();
	
			startProgress("Running PLS regression...", 0, 0);

			double[][] x = new double[rowCount][parameterCount];
			int[] count = new int[parameterCount];
			int p = 0;
			if (mode == MODE_POLAR || mode == MODE_TOTAL) {
				for (int i=0; i<TotalSurfaceAreaPredictor.getPolarAtomTypeCount(); i++) {
					int column = mTableModel.findColumn("pc"+(i+1));
					for (int r=0; r<rowCount; r++) {
						x[r][p] = mTableModel.getTotalDoubleAt(r, column);
						count[p] += (int)mTableModel.getTotalDoubleAt(r, column);
						}
					p++;
					}
				}
			if (mode == MODE_NON_POLAR || mode == MODE_TOTAL) {
				for (int i=0; i<TotalSurfaceAreaPredictor.getNonPolarAtomTypeCount(); i++) {
					int column = mTableModel.findColumn("npc"+(i+1));
					for (int r=0; r<rowCount; r++) {
						x[r][p] = mTableModel.getTotalDoubleAt(r, column);
						count[p] += (int)mTableModel.getTotalDoubleAt(r, column);
						}
					p++;
					}
				}
	
			int surfaceAreaColumn = mTableModel.findColumn(SURFACE_COLUMN_TITLE[mode]);
			double[][] y = new double[rowCount][1];
			for (int r=0; r<rowCount; r++)
				y[r][0] = mTableModel.getTotalDoubleAt(r, surfaceAreaColumn);
	
			System.out.println("############# "+SURFACE_COLUMN_TITLE[mode]+" ############");
			ModelXYIndex modelData = new ModelXYIndex();
			modelData.X = new Matrix(x);
			modelData.Y = new Matrix(y);
			RegressionModelCalculatorOptimumFactors rmc = new RegressionModelCalculatorOptimumFactors();
			ModelError error = rmc.calculateModel(modelData, parameterCount/2, parameterCount);
			System.out.println("average:"+error.error+" max:"+error.errMax);
	
			double[][] b = rmc.getB().getArray();
			for (int r=0; r<b.length; r++) {
				System.out.print(count[r]+": ");
				for (int c=0; c<b[r].length; c++)
					System.out.print(b[r][c]+" ");
				System.out.println();
				}
	
			if (!threadMustDie()) {
				startProgress("Extending table...", 0, 0);
	
				double[][] predictedY = rmc.getYHat().getArray();
	
				String[] columnTitle = new String[1];
				columnTitle[0] = "Predicted "+SURFACE_COLUMN_TITLE[mode];
				final int firstNewColumn = mTableModel.addNewColumns(columnTitle);
				for (int r=0; r<rowCount; r++)
					mTableModel.setTotalValueAt(""+predictedY[r][0], r, firstNewColumn);
	
				mTableModel.finalizeNewColumns(firstNewColumn, this);
				}
			}
		}
	}
