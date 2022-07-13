/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap.metric;

/**
 * Manhattan distance.
 */
public final class ManhattanMetric extends Metric {

  /** Manhattan distance. */
  public static final ManhattanMetric SINGLETON = new ManhattanMetric();

  private ManhattanMetric() {
    super(false);
  }

  @Override
  public float distance(final float[] x, final float[] y) {
    //  D(x, y) = \sum_i |x_i - y_i|
    float result = 0;
    for (int i = 0; i < x.length; ++i) {
      result += Math.abs(x[i] - y[i]);
    }
    return result;
  }
}
