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

package com.actelion.research.table.category;

import com.actelion.research.util.SortedList;

public abstract class CategoryList<T extends Comparable<? super T>> {
	private CategoryNormalizer<T> mNormalizer;
	private SortedList<T> mCategoryList;

	public CategoryList(SortedList<T> categoryList, CategoryNormalizer<T> normalizer) {
		mCategoryList = categoryList;
		mNormalizer = normalizer;
		}

	public void add(T v) {
		mCategoryList.add(v);
		}

	public void addString(String s) {
		add(mNormalizer.normalizeIn(s));
		}

	public T get(int i) {
		return mCategoryList.get(i);
		}

	public String getString(int i) {
		return mNormalizer.normalizeOut(mCategoryList.get(i));
		}

	public int getSize() {
		return mCategoryList.size();
		}

	public int getIndex(T v) {
		return mCategoryList.getIndex(v);
		}

	public int getIndexOfString(String s) {
		return mCategoryList.getIndex(mNormalizer.normalizeIn(s));
		}

	public boolean contains(T v) {
		return getIndex(v) != -1;
		}

	public boolean containsString(String s) {
		return getIndex(mNormalizer.normalizeIn(s)) != -1;
		}

	public int getIndexBelowEqual(T v) {
		return mCategoryList.getIndexBelowEqual(v);
		}

	public int getIndexAboveEqual(T v) {
		return mCategoryList.getIndexAboveEqual(v);
		}

	protected SortedList<T> getCategoryList() {
		return mCategoryList;
		}

	public CategoryNormalizer<T> getNormalizer() {
		return mNormalizer;
		}
	}
