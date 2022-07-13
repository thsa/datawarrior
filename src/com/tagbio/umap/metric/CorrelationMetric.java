/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap.metric;

/**
 * Correlation distance.
 */
public final class CorrelationMetric extends Metric {

  /** Correlation distance. */
  public static final CorrelationMetric SINGLETON = new CorrelationMetric();

  private CorrelationMetric() {
    super(true);
  }

  @Override
  public float distance(final float[] x, final float[] y) {
     float muX = 0.0F;
     float muY = 0.0F;
     float normX = 0.0F;
     float normY = 0.0F;
     float dotProduct = 0.0F;

    for (int i = 0; i < x.length; ++i) {
       muX += x[i];
       muY += y[i];
     }

     muX /= x.length;
     muY /= x.length;

    for (int i = 0; i < x.length; ++i) {
      final float shiftedX = x[i] - muX;
      final float shiftedY = y[i] - muY;
      normX += shiftedX * shiftedX;
      normY += shiftedY * shiftedY;
      dotProduct += shiftedX * shiftedY;
    }

     if (normX == 0.0 && normY == 0.0) {
       return 0;
     } else if (dotProduct == 0.0) {
       return 1;
     } else {
       return (float) (1 - (dotProduct / Math.sqrt(normX * normY)));
     }
  }
}
