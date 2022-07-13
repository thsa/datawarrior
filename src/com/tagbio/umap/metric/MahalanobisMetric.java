/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap.metric;

/**
 * Mahalanobis distance.
 */
public class MahalanobisMetric extends Metric {

  private final float[][] mV;

  public MahalanobisMetric(final float[][] v) {
    super(false);
    mV = v;
  }

  @Override
  public float distance(final float[] x, final float[] y) {
    float result = 0;
    final float[] diff = new float[x.length];
    for (int i = 0; i < x.length; ++i) {
      diff[i] = x[i] - y[i];
    }
    for (int i = 0; i < x.length; ++i) {
      double tmp = 0.0;
      for (int j = 0; j < x.length; ++j) {
        tmp += mV[i][j] * diff[j];
      }
      result += tmp * diff[i];
    }
    return (float) Math.sqrt(result);
  }
}

