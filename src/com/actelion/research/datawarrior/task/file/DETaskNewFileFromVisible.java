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

import java.util.*;


public class DETaskNewFileFromVisible extends AbstractTaskWithoutConfiguration {
	public static final String TASK_NAME = "New File From Visible Rows";

	private final DEFrame		mSourceFrame;
	private DEFrame				mTargetFrame;

	public DETaskNewFileFromVisible(DEFrame sourceFrame) {
		super(sourceFrame, false);
		mSourceFrame = sourceFrame;
		}

	@Override
	public boolean isConfigurable() {
		CompoundTableModel tableModel = mSourceFrame.getTableModel();

        if (tableModel.getRowCount() == 0) {
        	showErrorMessage("No visible rows found.");
        	return false;
        	}

        return true;
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
       	long[] hitlistMask = null;
       	if (hitlistUsed.length != 0) {
       		hitlistMask = new long[hitlistUsed.length];
       		for (int i=0; i<hitlistUsed.length; i++)
       			hitlistMask[i] = sourceHitlistHandler.getListMask(i);
       		}

        for (int row=0; row<sourceTableModel.getRowCount(); row++)
       		for (int i=0; i<hitlistUsed.length; i++)
       			if ((sourceTableModel.getRecord(row).getFlags() & hitlistMask[i]) != 0)
       				hitlistUsed[i] = true;

        mTargetFrame = getEmptyFrame("Subset of "+mSourceFrame.getTitle());
        CompoundTableModel targetTableModel = mTargetFrame.getTableModel();
        CompoundTableListHandler targetHitlistHandler = targetTableModel.getListHandler();
        targetTableModel.initializeTable(sourceTableModel.getRowCount(), sourceTableModel.getTotalColumnCount());
        for (int column=0; column<sourceTableModel.getTotalColumnCount(); column++)
        	targetTableModel.setColumnName(sourceTableModel.getColumnTitleNoAlias(column), column);

        for (int column=0; column<sourceTableModel.getTotalColumnCount(); column++) {
        	HashMap<String,String> properties = sourceTableModel.getColumnProperties(column);
        	for (String key:properties.keySet())
        		targetTableModel.setColumnProperty(column, key, properties.get(key));
        	}

		TreeSet<String> detaiIDSet = new TreeSet<>();
       	for (int row=0; row<sourceTableModel.getRowCount(); row++) {
			for (int column=0; column<sourceTableModel.getTotalColumnCount(); column++) {
				targetTableModel.setTotalValueAt(sourceTableModel.encodeDataWithDetail(sourceTableModel.getRecord(row), column), row, column);
				String[][] key = sourceTableModel.getRecord(row).getDetailReferences(column);
				if (key != null)
					for (String[] strings : key)
						if (strings != null)
							Collections.addAll(detaiIDSet, strings);
				}
        	}

        targetTableModel.finalizeTable(CompoundTableEvent.cSpecifierNoRuntimeProperties, getProgressController());

        for (int i=0; i<hitlistUsed.length; i++) {
        	if (hitlistUsed[i]) {
        		int flagNo = targetHitlistHandler.getListFlagNo(targetHitlistHandler.createList(
        				sourceHitlistHandler.getListName(i), -1, CompoundTableListHandler.EMPTY_LIST, -1, null, false));
               	for (int row=0; row<sourceTableModel.getRowCount(); row++)
       				if ((sourceTableModel.getRecord(row).getFlags() & hitlistMask[i]) != 0)
       					targetHitlistHandler.addRecordSilent(targetTableModel.getTotalRecord(row), flagNo);
        		}
        	}

		HashMap<String,byte[]> detailMap = sourceTableModel.getDetailHandler().getEmbeddedDetailMap();
		if (detailMap != null)
			for (String detailID:detaiIDSet)
				targetTableModel.getDetailHandler().setEmbeddedDetail(detailID, detailMap.get(detailID));

		String explanation = (String)sourceTableModel.getExtensionData(CompoundTableConstants.cExtensionNameFileExplanation);
		if (explanation != null)
			targetTableModel.setExtensionData(CompoundTableConstants.cExtensionNameFileExplanation, explanation);

		ArrayList<DEMacro> macroList = (ArrayList<DEMacro>) sourceTableModel.getExtensionData(CompoundTableConstants.cExtensionNameMacroList);
		if (macroList != null && !macroList.isEmpty()) {
			ArrayList<DEMacro> copy = new ArrayList<>();
			for (DEMacro macro:macroList)
				copy.add(new DEMacro(macro.getName(), copy, macro));
			targetTableModel.setExtensionData(CompoundTableConstants.cExtensionNameMacroList, copy);
			}

        rp.setParentPane(mTargetFrame.getMainFrame());
        rp.apply();
		}
	}
