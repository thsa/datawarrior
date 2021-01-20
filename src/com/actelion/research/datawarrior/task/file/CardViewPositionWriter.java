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
import com.actelion.research.datawarrior.DEMainPane;
import com.actelion.research.gui.dock.Dockable;
import com.actelion.research.table.DataDependentPropertyWriter;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JCardView;

import java.io.BufferedWriter;
import java.io.IOException;

public class CardViewPositionWriter implements DataDependentPropertyWriter {
	public static final String PROPERTY_NAME = "CardView Positions";

	private DEMainPane	mMainPane;

	public CardViewPositionWriter(DEMainPane mainPane) {
		mMainPane = mainPane;
	}

	@Override
	public String getPropertyName() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean hasSomethingToWrite() {
		for (Dockable dockable:mMainPane.getDockables())
			if (dockable.getContent() instanceof JCardView)
				return true;

		return false;
	}

	@Override
	public void write(BufferedWriter writer) throws IOException {
		for (Dockable dockable:mMainPane.getDockables()) {
			CompoundTableView view = (CompoundTableView)dockable.getContent();
			if (view instanceof JCardView) {
				JCardView cardView = (JCardView)view;
				writer.write(CompoundTableConstants.cViewNameStart);
				writer.write(dockable.getTitle());
				writer.write("\">");
				writer.newLine();
				cardView.write(writer);
				writer.write(CompoundTableConstants.cViewNameEnd);
				writer.newLine();
			}
		}
	}
}
