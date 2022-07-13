/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap.metric;

/**
 * Reduced Euclidean distance.
 * @author Leland McInnes
 * @author Sean A. Irvine
 * @author Richard Littin
 */
public final class ReducedEuclideanMetric extends Metric {

  /** Reduced Euclidean distance. */
  public static final ReducedEuclideanMetric SINGLETON = new ReducedEuclideanMetric();

  private ReducedEuclideanMetric() {
    super(false);
  }

  @Override
  public float distance(final float[] x, final float[] y) {
    //  D(x, y) = \sum_i (x_i - y_i)^2
    float result = 0;
    for (int i = 0; i < x.length; ++i) {
      final float d = x[i] - y[i];
      result += d * d;
    }
    return result;
  }
}
