/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap.metric;

/**
 * Kulsinski distance.
 */
public final class KulsinskiMetric extends Metric {

  /** Kulsinski distance. */
  public static final KulsinskiMetric SINGLETON = new KulsinskiMetric();

  private KulsinskiMetric() {
    super(false);
  }

  @Override
  public float distance(final float[] x, final float[] y) {
    int numTrueTrue = 0;
    int numNotEqual = 0;
    for (int i = 0; i < x.length; ++i) {
      final boolean xTrue = x[i] != 0;
      final boolean yTrue = y[i] != 0;
      if (xTrue && yTrue) {
        ++numTrueTrue;
      }
      if (xTrue != yTrue) {
        ++numNotEqual;
      }
    }
    return numNotEqual == 0 ? 0 : (numNotEqual - numTrueTrue + x.length) / (float) (numNotEqual + x.length);
  }
}
