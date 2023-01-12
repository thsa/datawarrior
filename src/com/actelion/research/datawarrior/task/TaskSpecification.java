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

package com.actelion.research.datawarrior.task;


import java.util.ArrayList;

public class TaskSpecification implements Comparable<TaskSpecification> {
	public static final int CATEGORY_FILE = 0;
	public static final int CATEGORY_EDIT = 1;
	public static final int CATEGORY_DATA = 2;
	public static final int CATEGORY_CHEMISTRY = 3;
	public static final int CATEGORY_LIST = 4;
	public static final int CATEGORY_DATABASE = 5;
	public static final int CATEGORY_FILTER = 6;
	public static final int CATEGORY_TABLE = 7;
	public static final int CATEGORY_VIEW = 8;
	public static final int CATEGORY_MACRO = 9;
	public static final int CATEGORY_TEST = 10;
	public static final String[] CATEGORY_NAME = { "File", "Edit", "Data", "Chemistry", "List", "Database", "Filter", "Table", "View", "Macro", "Test" };

	private static ArrayList<String> sCustomCategoryList = null;
	private int mCategory;
	private String mName;

	public TaskSpecification(int category, String name) {
		mCategory = category;
		mName = name;
		}

	public TaskSpecification(String customCategory, String name) {
		mCategory = -1;
		for (int i=0; i<CATEGORY_NAME.length; i++) {
			if (CATEGORY_NAME[i].equals(name)) {
				mCategory = i;
				break;
				}
			}

		if (mCategory == -1) {
			if (sCustomCategoryList == null)
				sCustomCategoryList = new ArrayList<>();
			if (!sCustomCategoryList.contains(customCategory))
				sCustomCategoryList.add(customCategory);
			mCategory = CATEGORY_NAME.length + sCustomCategoryList.indexOf(customCategory);
			}

		mName = name;
		}

	public int getCategory() {
		return mCategory;
		}

	public String getCategoryName() {
		return mCategory < CATEGORY_NAME.length ? CATEGORY_NAME[mCategory] : sCustomCategoryList.get(mCategory - CATEGORY_NAME.length);
		}

	public String getTaskName() {
		return mName;
		}

	@Override
	public int compareTo(TaskSpecification o) {
		return mCategory < o.mCategory ? -1
			 : mCategory > o.mCategory ? 1
			 : mName.compareTo(o.mName);
		}
	}
