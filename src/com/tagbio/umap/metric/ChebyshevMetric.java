/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap.metric;

/**
 * Chebyshev distance.
 */
public final class ChebyshevMetric extends Metric {

  /** Chebyshev distance. */
  public static final ChebyshevMetric SINGLETON = new ChebyshevMetric();

  private ChebyshevMetric() {
    super(false);
  }

  @Override
  public float distance(final float[] x, final float[] y) {
    // D(x, y) = \max_i |x_i - y_i|
    float result = 0;
    for (int i = 0; i < x.length; ++i) {
      result = Math.max(result, Math.abs(x[i] - y[i]));
    }
    return result;
  }
}
