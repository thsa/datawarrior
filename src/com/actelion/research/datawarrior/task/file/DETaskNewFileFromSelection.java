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

package com.actelion.research.datawarrior.task.file;

import com.actelion.research.chem.io.CompoundTableConstants;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.DERuntimeProperties;
import com.actelion.research.datawarrior.DataWarrior;
import com.actelion.research.datawarrior.task.AbstractTaskWithoutConfiguration;
import com.actelion.research.datawarrior.task.DEMacro;
import com.actelion.research.table.model.CompoundTableEvent;
import com.actelion.research.table.model.CompoundTableListHandler;
import com.actelion.research.table.model.CompoundTableModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.TreeSet;


public class DETaskNewFileFromSelection extends AbstractTaskWithoutConfiguration {
	public static final String TASK_NAME = "New File From Selection";

	private DEFrame		mSourceFrame,mTargetFrame;
	private DataWarrior	mApplication;

	public DETaskNewFileFromSelection(DEFrame sourceFrame, DataWarrior application) {
		super(sourceFrame, false);
		mSourceFrame = sourceFrame;
		mApplication = application;
		}

	@Override
	public boolean isConfigurable() {
		CompoundTableModel tableModel = mSourceFrame.getTableModel();

        if (!tableModel.hasSelectedRows()) {
        	showErrorMessage("No selected rows found.");
        	return false;
        	}

        return true;
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
	public void runTask(Properties configuration) {
	    DERuntimeProperties rp = new DERuntimeProperties(mSourceFrame.getMainFrame());
		rp.learn();

        CompoundTableModel sourceTableModel = mSourceFrame.getTableModel();
        CompoundTableListHandler sourceHitlistHandler = sourceTableModel.getListHandler();

       	boolean[] hitlistUsed = new boolean[sourceHitlistHandler.getListCount()];
       	long hitlistMask[] = null;
       	if (hitlistUsed.length != 0) {
       		hitlistMask = new long[hitlistUsed.length];
       		for (int i=0; i<hitlistUsed.length; i++)
       			hitlistMask[i] = sourceHitlistHandler.getListMask(i);
       		}

		int selectionCount = 0;
        for (int row=0; row<sourceTableModel.getRowCount(); row++) {
			if (sourceTableModel.isSelected(row)) {
				selectionCount++;
	       		for (int i=0; i<hitlistUsed.length; i++)
	       			if ((sourceTableModel.getRecord(row).getFlags() & hitlistMask[i]) != 0)
	       				hitlistUsed[i] = true;
				}
        	}

        mTargetFrame = mApplication.getEmptyFrame("Selection of "+mSourceFrame.getTitle());
        CompoundTableModel targetTableModel = mTargetFrame.getTableModel();
        CompoundTableListHandler targetHitlistHandler = targetTableModel.getListHandler();
        targetTableModel.initializeTable(selectionCount, sourceTableModel.getTotalColumnCount());
        for (int column=0; column<sourceTableModel.getTotalColumnCount(); column++)
        	targetTableModel.setColumnName(sourceTableModel.getColumnTitleNoAlias(column), column);

        for (int column=0; column<sourceTableModel.getTotalColumnCount(); column++) {
        	HashMap<String,String> properties = sourceTableModel.getColumnProperties(column);
        	for (String key:properties.keySet())
        		targetTableModel.setColumnProperty(column, key, properties.get(key));
        	}

		TreeSet<String> detaiIDSet = new TreeSet<String>();
       	for (int targetRow=0,row=0; row<sourceTableModel.getRowCount(); row++) {
			if (sourceTableModel.isSelected(row)) {
 		        for (int column=0; column<sourceTableModel.getTotalColumnCount(); column++) {
    				targetTableModel.setTotalValueAt(sourceTableModel.encodeDataWithDetail(sourceTableModel.getRecord(row), column), targetRow, column);
    		        String[][] key = sourceTableModel.getRecord(row).getDetailReferences(column);
    	            if (key != null)
   		                for (int detailIndex=0; detailIndex<key.length; detailIndex++)
   		                    if (key[detailIndex] != null)
   		                        for (int i=0; i<key[detailIndex].length; i++)
   		                        	detaiIDSet.add(key[detailIndex][i]);
 		        	}
 		        targetRow++;
				}
        	}

        targetTableModel.finalizeTable(CompoundTableEvent.cSpecifierNoRuntimeProperties, getProgressController());

        for (int i=0; i<hitlistUsed.length; i++) {
        	if (hitlistUsed[i]) {
        		int flagNo = targetHitlistHandler.getListFlagNo(targetHitlistHandler.createList(
        				sourceHitlistHandler.getListName(i), -1, CompoundTableListHandler.EMPTY_LIST, -1, null, false));
               	int tRow = 0;
               	for (int row=0; row<sourceTableModel.getRowCount(); row++) {
        			if (sourceTableModel.isSelected(row)) {
        				if ((sourceTableModel.getRecord(row).getFlags() & hitlistMask[i]) != 0)
        					targetHitlistHandler.addRecordSilent(targetTableModel.getTotalRecord(tRow), flagNo);
        				tRow++;
        				}
               		}
        		}
        	}

		HashMap<String,byte[]> detailMap = sourceTableModel.getDetailHandler().getEmbeddedDetailMap();
		if (detailMap != null)
			for (String detailID : detaiIDSet)
				targetTableModel.getDetailHandler().setEmbeddedDetail(detailID, detailMap.get(detailID));

		String explanation = (String)sourceTableModel.getExtensionData(CompoundTableConstants.cExtensionNameFileExplanation);
		if (explanation != null)
			targetTableModel.setExtensionData(CompoundTableConstants.cExtensionNameFileExplanation, explanation);

		ArrayList<DEMacro> macroList = (ArrayList<DEMacro>) sourceTableModel.getExtensionData(CompoundTableConstants.cExtensionNameMacroList);
		if (macroList != null && macroList.size() != 0) {
			ArrayList<DEMacro> copy = new ArrayList<>();
			for (DEMacro macro:macroList)
				copy.add(new DEMacro(macro.getName(), copy, macro));
			targetTableModel.setExtensionData(CompoundTableConstants.cExtensionNameMacroList, copy);
			}

		rp.setParentPane(mTargetFrame.getMainFrame());
        rp.apply();
		}
	}
