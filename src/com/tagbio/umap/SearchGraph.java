/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap;

import java.util.Set;
import java.util.TreeSet;

/**
 * Stores unordered pairs.
 * @author Sean A. Irvine
 * @author Richard Littin
 */
class SearchGraph {

  private final TreeSet<Integer>[] mRows;

  @SuppressWarnings("unchecked")
  SearchGraph(final int rows) {
    mRows = (TreeSet<Integer>[]) new TreeSet[rows];
    for (int k = 0; k < rows; ++k) {
      mRows[k] = new TreeSet<>();
    }
  }

  /**
   * Set the unordered pair of instances.
   * @param x instance index
   * @param y instance index
   */
  void set(final int x, final int y) {
    mRows[x].add(y);
    mRows[y].add(x);
  }

  /**
   * Set of indices for an instance.
   * @param row instance number
   * @return set of instance numbers
   */
  Set<Integer> row(final int row) {
    return mRows[row];
  }
}
