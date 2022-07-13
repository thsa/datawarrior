/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap.metric;

/**
 * Minkowski distance.
 */
public class MinkowskiMetric extends Metric {

  private final double mPower;

  public MinkowskiMetric(final double power) {
    super(false);
    mPower = power;
  }

  @Override
  public float distance(final float[] x, final float[] y) {
    // D(x, y) = \left(\sum_i |x_i - y_i|^p\right)^{\frac{1}{p}}
    double result = 0.0;
    for (int i = 0; i < x.length; ++i) {
      result += Math.pow(Math.abs(x[i] - y[i]), mPower);
    }
    return (float) Math.pow(result, 1 / mPower);
  }
}
