/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap.metric;

/**
 * Euclidean distance standardised against a vector of standard deviations per coordinate.
 * @author Sean A. Irvine
 */
public class StandardisedEuclideanMetric extends Metric {

  private final float[] mSigma;

  public StandardisedEuclideanMetric(final float[] sigma) {
    super(false);
    mSigma = sigma;
  }

  @Override
  public float distance(final float[] x, final float[] y) {
    //  D(x, y) = \sqrt{\sum_i \frac{(x_i - y_i)**2}{v_i}}
    float result = 0;
    for (int i = 0; i < x.length; ++i) {
      final float d = x[i] - y[i];
      result += d * d / mSigma[i];
    }
    return (float) Math.sqrt(result);
  }
}
