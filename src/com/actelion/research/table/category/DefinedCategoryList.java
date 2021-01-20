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

public class DefinedCategoryList<T extends Comparable<? super T>> extends CategoryList<T> {
	private UniqueList<String> mDefinedOrder;

	public DefinedCategoryList(UniqueList<String> definedOrder, CategoryNormalizer<T> normalizer) {
		super(new UniqueList<T>(), normalizer);
		mDefinedOrder = definedOrder;
		}

	public void add(T v) {
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
