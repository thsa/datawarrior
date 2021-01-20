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

package com.actelion.research.datawarrior.task.view;

import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.table.model.CompoundTableModel;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class DETaskNewCardsView extends DETaskAbstractNewView {
	public static final String TASK_NAME = "New Cards View";

	private CompoundTableModel mTableModel;

	public DETaskNewCardsView(Frame parent, DEMainPane mainPane, String whereViewName) {
		super(parent, mainPane, whereViewName);
		mTableModel = mainPane.getTableModel();
	}

	@Override
	public String getTaskName() {
		return TASK_NAME;
	}

	@Override
	public String getDefaultViewName() {
		return getMainPane().getDefaultViewName(DEMainPane.VIEW_TYPE_CARD, -1);
	}

	@Override
	public void addInnerDialogContent(JPanel content) {
	}

	@Override
	public boolean isConfigurable() {
		return true;
	}

	@Override
	public void createNewView(String viewName, String whereView, String where, Properties configuration) {
		getMainPane().createNewView(viewName, DEMainPane.VIEW_TYPE_CARD, whereView, where, -1);
	}
}
