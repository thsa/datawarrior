/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap;

import java.util.Random;

/**
 * Flattened tree.
 * @author Leland McInnes (Python)
 * @author Sean A. Irvine
 * @author Richard Littin
 */
class FlatTree {

  // Used for a floating point "nearly zero" comparison
  private static final float EPS = 1e-8F;

  private final Object mHyperplanes;
  private final float[] mOffsets;
  private final int[][] mChildren;
  private final int[][] mIndices;

  FlatTree(final Object hyperplanes, final float[] offsets, final int[][] children, final int[][] indices) {
    mHyperplanes = hyperplanes;
    mOffsets = offsets;
    mChildren = children;
    mIndices = indices;
  }

  int[][] getIndices() {
    return mIndices;
  }

  private static boolean selectSide(final float[] hyperplane, final float offset, final float[] point, final Random random) {
    float margin = offset;
    for (int d = 0; d < point.length; ++d) {
      margin += hyperplane[d] * point[d];
    }
    if (Math.abs(margin) < EPS) {
      return random.nextBoolean();
    } else {
      return margin <= 0;
    }
  }

  int[] searchFlatTree(final float[] point, final Random random) {
    int node = 0;
    while (mChildren[node][0] > 0) {
      final boolean side = selectSide(((float[][]) mHyperplanes)[node], mOffsets[node], point, random);
      node = mChildren[node][side ? 1 : 0];
    }
    return mIndices[-mChildren[node][0]];
  }
}
