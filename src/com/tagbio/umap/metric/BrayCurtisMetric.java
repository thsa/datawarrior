/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap.metric;

/**
 * Bray Curtis distance.
 */
public final class BrayCurtisMetric extends Metric {

  /** Bray Curtis distance. */
  public static final BrayCurtisMetric SINGLETON = new BrayCurtisMetric();

  private BrayCurtisMetric() {
    super(false);
  }

  @Override
  public float distance(final float[] x, final float[] y) {
    float numerator = 0;
    float denominator = 0;
    for (int i = 0; i < x.length; ++i) {
      numerator += Math.abs(x[i] - y[i]);
      denominator += Math.abs(x[i] + y[i]);
    }
    return denominator > 0 ? numerator / denominator : 0;
  }
}
