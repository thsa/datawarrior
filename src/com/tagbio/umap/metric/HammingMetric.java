/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap.metric;

/**
 * Hamming distance.
 */
public final class HammingMetric extends Metric {

  /** Hamming distance. */
  public static final HammingMetric SINGLETON = new HammingMetric();

  private HammingMetric() {
    super(false);
  }

  @Override
  public float distance(final float[] x, final float[] y) {
    float result = 0;
    for (int i = 0; i < x.length; ++i) {
      if (x[i] != y[i]) {
        ++result;
      }
    }
    return result / x.length;
  }
}
