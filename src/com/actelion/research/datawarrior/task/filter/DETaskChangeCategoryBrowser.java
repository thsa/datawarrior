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

package com.actelion.research.datawarrior.task.filter;

import java.awt.Frame;
import java.util.Properties;

import com.actelion.research.datawarrior.DEPruningPanel;
import com.actelion.research.table.filter.JCategoryBrowser;
import com.actelion.research.table.filter.JFilterPanel;

public class DETaskChangeCategoryBrowser extends DEAbstractFilterTask {
	public static final String TASK_NAME = "Change Category Browser";

	public DETaskChangeCategoryBrowser(Frame parent, DEPruningPanel pruningPanel, JFilterPanel filter) {
		super(parent, pruningPanel, filter);
		}

	@Override
	public boolean isRedundant(Properties previousConfiguration, Properties currentConfiguration) {
		return previousConfiguration.getProperty(PROPERTY_DUPLICATE).equals(currentConfiguration.getProperty(PROPERTY_DUPLICATE));
		}

	@Override
	public String getTaskName() {
		return TASK_NAME;
		}

	@Override
	public JFilterPanel createFilterUI() {
		return new JCategoryBrowser(getParentFrame(), getTableModel());
		}

	@Override
	protected String getColumnQualificationError(int column) {
		return getTableModel().isColumnTypeCategory(column) ? null
				: "Category browsers can be applied category data only.";
		}

	@Override
	public int getFilterType() {
		return JFilterPanel.FILTER_TYPE_CATEGORY_BROWSER;
		}
	}
