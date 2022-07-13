/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap.metric;

/**
 * Russel Rao distance.
 */
public final class RussellRaoMetric extends Metric {

  /** Russel Rao distance. */
  public static final RussellRaoMetric SINGLETON = new RussellRaoMetric();

  private RussellRaoMetric() {
    super(false);
  }

  private int countNonZero(final float[] values) {
    int c = 0;
    for (final float v : values) {
      if (v != 0) {
        ++c;
      }
    }
    return c;
  }

  @Override
  public float distance(final float[] x, final float[] y) {
    int numTrueTrue = 0;
    for (int i = 0; i < x.length; ++i) {
      final boolean xTrue = x[i] != 0;
      final boolean yTrue = y[i] != 0;
      if (xTrue && yTrue) {
        ++numTrueTrue;
      }
    }
    if (numTrueTrue == countNonZero(x) && numTrueTrue == countNonZero(y)) {
      return 0;
    } else {
      return (x.length - numTrueTrue) / (float) x.length;
    }
  }
}
