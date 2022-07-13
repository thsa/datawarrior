/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap.metric;

/**
 * Special indicator that the metric has been precomputed.
 * @author Sean A. Irvine
 */
public final class PrecomputedMetric extends Metric {

  /** Special indicator that the metric has been precomputed. */
  public static final PrecomputedMetric SINGLETON = new PrecomputedMetric();

  private PrecomputedMetric() {
    super(false);
  }

  @Override
  public float distance(final float[] x, final float[] y) {
    throw new IllegalStateException("Attempt to computed distance when distances precomputed");
  }
}
