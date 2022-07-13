/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap;

/**
 * Container for a hyperplane.
 * @author Leland McInnes (Python)
 * @author Sean A. Irvine
 * @author Richard Littin
 */
class Hyperplane {

  //private final int[] mInds;
  private final float[] mData;
  private final int[] mShape;

  Hyperplane(final int[] inds, final float[] data) {
    //mInds = inds;
    mData = data;
    mShape = inds == null ? new int[] {data.length} : new int[] {inds.length, 2};
  }

  Hyperplane(final float[] data) {
    this(null, data);
  }

  public float[] data() {
    return mData;
  }

  public int[] shape() {
    return mShape;
  }
}
