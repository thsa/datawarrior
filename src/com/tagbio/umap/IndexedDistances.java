/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap;

import java.util.List;

/**
 * Container for indices and distances.
 * @author Sean A. Irvine
 * @author Richard Littin
 */
class IndexedDistances {

  private final int[][] mIndices;
  private final float[][] mDistances;
  private final List<FlatTree> mForest;

  IndexedDistances(final int[][] indices, final float[][] distances, final List<FlatTree> forest) {
    mIndices = indices;
    mDistances = distances;
    mForest = forest;
  }

  int[][] getIndices() {
    return mIndices;
  }

  float[][] getDistances() {
    return mDistances;
  }

  List<FlatTree> getForest() {
    return mForest;
  }
}
