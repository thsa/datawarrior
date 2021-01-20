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
import com.actelion.research.table.DataDependentPropertyReader;
import com.actelion.research.table.view.CompoundTableView;
import com.actelion.research.table.view.JCardView;
import com.actelion.research.table.view.card.CardPaneModel;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CardViewPositionReader implements DataDependentPropertyReader {
	volatile private DEFrame mDEFrame;
	volatile private ArrayList<CardViewPositions> mViewList;

	public CardViewPositionReader(DEFrame frame) {
		mDEFrame = frame;
	}

	@Override
	public void read(BufferedReader reader) throws IOException {
		mViewList = new ArrayList<>();
		String line = reader.readLine();
		while (line.startsWith(CompoundTableConstants.cViewNameStart)) {
			String title = line.substring(CompoundTableConstants.cViewNameStart.length(), line.length()-2);
			JCardView.CardViewDataDependentStorageValues cedl = JCardView.read(reader, CompoundTableConstants.cViewNameEnd);
			mViewList.add(new CardViewPositions(title, cedl));
			line = reader.readLine();
		}
	}

	@Override
	public void apply() {
		// we need to add to end of event list to make sure all views are ready
		if (mViewList != null && mViewList.size() != 0) {
			SwingUtilities.invokeLater(() -> {
				for (CardViewPositions list:mViewList) {
					CompoundTableView view = mDEFrame.getMainFrame().getMainPane().getView(list.viewName);
					if (view != null && view instanceof JCardView) {
                        ((JCardView) view).initializeCards(list.cardViewData.mCEData, false);
                        ((JCardView) view).getCardPane().setViewport(list.cardViewData.viewport_cx,
                                list.cardViewData.viewport_cy,list.cardViewData.viewport_zoom);
                    }
				}
			});
		}
	}

	private class CardViewPositions {
		public final String viewName;
		public final JCardView.CardViewDataDependentStorageValues cardViewData;

		public CardViewPositions(String viewName, JCardView.CardViewDataDependentStorageValues cardViewData) {
			this.viewName = viewName;
			this.cardViewData = cardViewData;
		}
	}
}
