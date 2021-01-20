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

package com.actelion.research.table;

import java.awt.*;
import java.io.*;
import javax.swing.*;
import com.actelion.research.calc.*;
import com.actelion.research.table.model.CompoundTableModel;

public class CompoundTableSOMLoader {
	private static final int ACTION_APPLY = 1;
	private static final int ACTION_ANALYSE = 2;

	private CompoundTableModel mTableModel;
	private Frame   			mParentFrame;
	private ProgressController  mProgressController;
	private Reader              mDataReader;
	private int					mAction;
	private String				mDocumentTitle;

	public CompoundTableSOMLoader(Frame parent, ProgressController pc) {
		mParentFrame = parent;
		mProgressController = pc;
		}

	/**
	 * @param file
	 * @param tableModel contains SOM input records and receives new columns with neuron assignment
	 */
	public void apply(File file, CompoundTableModel tableModel) {
		mTableModel = tableModel;
		mAction = ACTION_APPLY;
		startAction(file);
		}

	/**
	 * @param file
	 * @param tableModel this is the tableModel to receive the analysis data
	 */
	public void analyse(File file, CompoundTableModel tableModel) {
		mTableModel = tableModel;
		mAction = ACTION_ANALYSE;
		startAction(file);
		}

	private void startAction(File file) {
		try {
			mDataReader = new FileReader(file);
			}
		catch (FileNotFoundException e) {
			mTableModel.unlock();
			JOptionPane.showMessageDialog(mParentFrame, "File not found.");
			return;
			}

		if (mAction == ACTION_ANALYSE) {
		    mDocumentTitle = "Dissimilarity Map of "+file.getName();
			}

		try {
			runSOMAction();
		    }
		catch (Exception e) {
		    final String message = e.toString();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(mParentFrame, message);
					}
				} );
			}
		}

	private void runSOMAction() throws Exception {
		BufferedReader theReader = new BufferedReader(mDataReader);
		String firstLine = theReader.readLine();
		
		int somType = -1;
        if (firstLine.equals("<datawarrior SOM>")) {  // old VectorSOMs without type declaration
            somType = CompoundTableSOM.SOM_TYPE_DOUBLE;
            }
        else if (firstLine.startsWith("<datawarriorSOM type=")) {
		    String typeString = SelfOrganizedMap.extractValue(firstLine);
		    for (int i=0; i<CompoundTableSOM.SOM_TYPE_FILE.length; i++)
		        if (typeString.equals(CompoundTableSOM.SOM_TYPE_FILE[i]))
		            somType = i;
		    if (somType == -1) {
				theReader.close();
				throw new IOException("Unknown SOM type found.");
				}
			}
		else {
			theReader.close();
			throw new IOException("Invalid SOM file format");
			}

        CompoundTableSOM som = new CompoundTableSOM(mTableModel, somType);	// in case of ACTION_ANALYSE tableModel is not used, but must not be null
        som.addProgressListener(mProgressController);
        som.setThreadMaster(mProgressController);
        som.read(theReader);

		String lastLine = theReader.readLine();
		if (!lastLine.equals("</datawarrior SOM>")	// old VectorSOMs without type declaration
		 && !lastLine.equals("</datawarriorSOM>")) {
			theReader.close();
			throw new IOException("SOM file corrupted.");
			}

		theReader.close();

		if (mAction == ACTION_APPLY) {
			som.checkCompatibility();
			som.positionRecords();
			}
		else if (mAction == ACTION_ANALYSE) {
			som.createSimilarityMap(mTableModel);
        	mParentFrame.setTitle(mDocumentTitle);
			}
		}
	}
