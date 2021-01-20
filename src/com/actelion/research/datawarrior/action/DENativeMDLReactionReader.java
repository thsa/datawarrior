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

package com.actelion.research.datawarrior.action;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.ExtendedMolecule;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.io.NativeMDLReactionReader;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.chem.reaction.ReactionEncoder;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.gui.FileHelper;
import com.actelion.research.gui.JProgressDialog;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableModel;

import javax.swing.*;
import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;


public class DENativeMDLReactionReader implements Runnable {
    private Frame                   mParentFrame;
	private DataWarrior				mApplication;
	private DEFrame					mTargetFrame;
	private JProgressDialog    		mProgressDialog;
    private NativeMDLReactionReader mReader;
    private String					mDirectoryName;
    private int                     mAnswer;

    public DENativeMDLReactionReader(Frame owner, DataWarrior application) {
		mParentFrame = owner;
		mApplication = application;
	    }

	public void read() {
        JFileChooser fc = new JFileChooser();

		// file chooser height does not automatically grow with UI scale factor
		if (HiDPIHelper.getUIScaleFactor() > 1)
			fc.setPreferredSize(new Dimension(fc.getPreferredSize().width, HiDPIHelper.scale(fc.getPreferredSize().height)));

        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setCurrentDirectory(FileHelper.getCurrentDirectory());
        if (fc.showOpenDialog(mParentFrame) == JFileChooser.APPROVE_OPTION) {
            FileHelper.setCurrentDirectory(fc.getSelectedFile().getParentFile());
            try {
				mDirectoryName = fc.getSelectedFile().getName();
                mReader = new NativeMDLReactionReader(fc.getSelectedFile().getPath());
                if (mReader.getReactionCount() > 0) {
                    mProgressDialog = new JProgressDialog(mParentFrame);
            
            		Thread t = new Thread(this, "DENativeMDLReactionReader");
                    t.setPriority(Thread.MIN_PRIORITY);
                    t.start();
                    }
                }
            catch (IOException e) {
                JOptionPane.showMessageDialog(mParentFrame, e.toString());
                }
            }
		}

	public void run() {
		try {
			readData();
			}
		catch (OutOfMemoryError e) {
			final String message = "Out of memory. Launch DataWarrior with Java option -Xms???m or -Xmx???m.";
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(mParentFrame, message);
					}
				} );
			}

		mProgressDialog.close(mTargetFrame);
		}

	private void readData() {
        int rows = mReader.getReactionCount();
        Object[] fieldName = mReader.getFieldNames();

        String[][] rxnList = new String[rows][];
        byte[][] catalystList = new byte[rows][];
        byte[][] solventList = new byte[rows][];
        byte[][][] fieldDataList = new byte[rows][fieldName.length][];
        byte[][][] moleculeDataList = new byte[rows][4][];

        StereoMolecule catalysts = new StereoMolecule();
        StereoMolecule solvents = new StereoMolecule();

        mProgressDialog.startProgress("Reading reactions...", 0, rows);
        for (int row=0; row<rows; row++) {
            if ((row & 0x0F) == 0x0F) {
                mProgressDialog.updateProgress(row);
                }

            if (mProgressDialog.threadMustDie()) {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            mAnswer = JOptionPane.showConfirmDialog(mParentFrame,
                                    "Do you want to open those reactions that you have already loaded?",
                                    "Use Reactions?",
                                    JOptionPane.YES_NO_OPTION);
                            }
                        } );
                    if (mAnswer == JOptionPane.YES_OPTION) {
                        rows = row;
                        break;
                        }
                    }
                catch (Exception e) {}
                return;
                }

            try {
                Reaction rxn = mReader.getReaction(row, 0);
                rxnList[row] = ReactionEncoder.encode(rxn, false);

                try {
                    catalysts.deleteMolecule();
                    ArrayList<ExtendedMolecule> list = mReader.getCatalysts();
                    for (int i=0; i<list.size(); i++)
                        catalysts.addMolecule(list.get(i));
                    catalystList[row] = new Canonizer(catalysts).getIDCode().getBytes();
                    }
                catch (IllegalArgumentException iae) {}
                try {
                    solvents.deleteMolecule();
                    ArrayList<ExtendedMolecule> list = mReader.getSolvents();
                    for (int i=0; i<list.size(); i++)
                        solvents.addMolecule(list.get(i));
                    solventList[row] = new Canonizer(solvents).getIDCode().getBytes();
                    }
                catch (IllegalArgumentException iae) {}

                String[] fieldData = mReader.getFieldData(row, 0);
                for (int i=0; i<fieldData.length; i++)
                    fieldDataList[row][i] = fieldData[i] == null ? null : fieldData[i].getBytes();

                String reactantData = mReader.getReactantData();
                moleculeDataList[row][0] = reactantData == null ? null : reactantData.getBytes();
                String productData = mReader.getProductData();
                moleculeDataList[row][1] = productData == null ? null : productData.getBytes();
                String catalystData = mReader.getCatalystData();
                moleculeDataList[row][2] = catalystData == null ? null : catalystData.getBytes();
                String solventData = mReader.getSolventData();
                moleculeDataList[row][3] = solventData == null ? null : solventData.getBytes();
                }
            catch (FileNotFoundException fnfe) {
                final String message = fnfe.getMessage();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        JOptionPane.showMessageDialog(mParentFrame, message);
                        }
                    } );
                return;
                }
            catch (IOException e) {
                System.out.println("NativeMDLReactionReader.getReaction() failed. Stacktrace:");
                e.printStackTrace();
                }
            }

//        mReader.printPointerStatistics();

        mTargetFrame = mApplication.getEmptyFrame("Reactions From "+mDirectoryName);
        CompoundTableModel tableModel = mTargetFrame.getTableModel();
        String[] specialFieldName = { "Registry No", "Reaction", "mapping", "coords", "Reaction Catalysts", "rxnfp", "reactantffp",
				"productffp", "catalystffp", "Solvents", "Reactant Data", "Product Data", "Catalyst Data", "Solvent Data" };
        tableModel.initializeTable(rows, specialFieldName.length+fieldName.length);
        for (int i=0; i<specialFieldName.length; i++)
        	tableModel.setColumnName(specialFieldName[i], i);
        for (int i=0; i<fieldName.length; i++)
        	tableModel.setColumnName((String)fieldName[i], specialFieldName.length+i);
        for (int row=0; row<rows; row++) {
            if (rxnList[row] != null) {
            	tableModel.setTotalValueAt(""+(row+1), row, 0);
            	if (rxnList[row] != null) {
            		if (rxnList[row][0] != null && rxnList[row][0].length() != 0)
						tableModel.setTotalDataAt(rxnList[row][0].getBytes(), row, 1);
					if (rxnList[row][1] != null && rxnList[row][1].length() != 0)
						tableModel.setTotalDataAt(rxnList[row][1].getBytes(), row, 2);
					if (rxnList[row][2] != null && rxnList[row][2].length() != 0)
						tableModel.setTotalDataAt(rxnList[row][2].getBytes(), row, 3);
	            	tableModel.setTotalDataAt(catalystList[row], row, 4);
					}
            	tableModel.setTotalDataAt(solventList[row], row, 9);
                for (int i=0; i<4; i++)
                	tableModel.setTotalDataAt(moleculeDataList[row][i], row, 10+i);
                for (int i=0; i<fieldName.length; i++)
                	tableModel.setTotalDataAt(fieldDataList[row][i], row, specialFieldName.length+i);
                }
            }

		tableModel.prepareReactionColumns(1, "Reaction", false, true,
				true, true, true, true, true, true);
        tableModel.setColumnProperty(9, CompoundTableModel.cColumnPropertySpecialType, CompoundTableModel.cColumnTypeIDCode);
        tableModel.finalizeTable(CompoundTableEvent.cSpecifierDefaultFiltersAndViews, mProgressDialog);
		}
    }
