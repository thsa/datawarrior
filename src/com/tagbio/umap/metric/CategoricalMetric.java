/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap.metric;

/**
 * Special indicator for categorical data.
 * @author Sean A. Irvine
 */
public final class CategoricalMetric extends Metric {

  /** Special indicator for categorical data. */
  public static final CategoricalMetric SINGLETON = new CategoricalMetric();

  private CategoricalMetric() {
    super(false);
  }

  @Override
  public float distance(final float[] x, final float[] y) {
    throw new IllegalStateException();
  }
}
