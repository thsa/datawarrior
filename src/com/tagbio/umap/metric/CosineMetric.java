/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap.metric;

/**
 * Cosine distance.
 * @author Sean A. Irvine
 */
public final class CosineMetric extends Metric {

  /** Cosine distance. */
  public static final CosineMetric SINGLETON = new CosineMetric();

  private CosineMetric() {
    super(true);
  }

  @Override
  public float distance(final float[] x, final float[] y) {
    double result = 0.0;
    double normX = 0.0;
    double normY = 0.0;
    for (int i = 0; i < x.length; ++i) {
      result += x[i] * y[i];
      normX += x[i] * x[i];
      normY += y[i] * y[i];
    }
    if (normX == 0.0 && normY == 0.0) {
      return 0;
    } else if (normX == 0.0 || normY == 0.0) {
      return 1;
    } else {
      return (float) (1 - (result / Math.sqrt(normX * normY)));
    }
  }
}
