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

import com.actelion.research.util.UniqueList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;

public class DefinedCategoryList<T extends Comparable<? super T>> extends CategoryList<T> {
	private final UniqueList<String> mDefinedOrder;
	private ArrayList<T> mBulkList;
	private TreeSet<T> mBulkSet;

	public DefinedCategoryList(UniqueList<String> definedOrder, CategoryNormalizer<T> normalizer) {
		super(new UniqueList<T>(), normalizer);
		mDefinedOrder = definedOrder;
		}

	/**
	 * Adding categories one by one inserts them into the middle of the UniqueList, which invalidates
	 * its index; the next contains() rebuilds it. With thousands of categories this is quadratic.
	 * Between startBulkAdd() and finishBulkAdd() new categories are only collected and are then
	 * brought into the same order that add() would have produced in one step: first all categories
	 * of the defined order in that order, then all others in the order they were added.
	 */
	public void startBulkAdd() {
		mBulkList = new ArrayList<>();
		mBulkSet = new TreeSet<>();
		}

	public void finishBulkAdd() {
		if (mBulkList == null)
			return;

		ArrayList<T> bulkList = mBulkList;
		mBulkList = null;
		mBulkSet = null;

		ArrayList<T> definedList = new ArrayList<>();
		ArrayList<T> otherList = new ArrayList<>();
		for (T v:bulkList) {
			if (mDefinedOrder.getIndex(getNormalizer().normalizeOut(v)) == -1)
				otherList.add(v);
			else
				definedList.add(v);
			}

		Object[] defined = definedList.toArray();
		Arrays.sort(defined, (o1, o2) -> Integer.compare(mDefinedOrder.getIndex(getNormalizer().normalizeOut((T)o1)),
														 mDefinedOrder.getIndex(getNormalizer().normalizeOut((T)o2))));
		UniqueList<T> list = (UniqueList<T>)getCategoryList();
		for (Object v:defined)
			list.add((T)v);
		for (T v:otherList)
			list.add(v);
		}

	@Override
	public int getSize() {
		return (mBulkList != null) ? mBulkList.size() : super.getSize();
		}

	public void add(T v) {
		if (mBulkList != null) {
			if (mBulkSet.add(v))
				mBulkList.add(v);
			return;
			}

		if (!contains(v)) {
			int definedIndex = mDefinedOrder.getIndex(getNormalizer().normalizeOut(v));
			if (definedIndex == -1) {
				((UniqueList<T>)getCategoryList()).add(v);	// just add it to the end, if it is not in the defined list of ordered items
				return;
				}

			definedIndex--;
			while (definedIndex >= 0 && !contains(getNormalizer().normalizeIn(mDefinedOrder.get(definedIndex))))
				definedIndex--;

			if (definedIndex == -1)
				((UniqueList<T>)getCategoryList()).add(0, v);
			else
				((UniqueList<T>)getCategoryList()).add(1+getIndex(getNormalizer().normalizeIn(mDefinedOrder.get(definedIndex))), v);
			}
		}
	}
