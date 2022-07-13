/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap.metric;

/**
 * Canberra distance.
 */
public final class CanberraMetric extends Metric {

  /** Canberra distance. */
  public static final CanberraMetric SINGLETON = new CanberraMetric();

  private CanberraMetric() {
    super(false);
  }

  @Override
  public float distance(final float[] x, final float[] y) {
    float result = 0;
    for (int i = 0; i < x.length; ++i) {
      final float denominator = Math.abs(x[i]) + Math.abs(y[i]);
      if (denominator > 0) {
        result += Math.abs(x[i] - y[i]) / denominator;
      }
    }
    return result;
  }
}
