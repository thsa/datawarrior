/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap.metric;

/**
 * Sokal Michener distance.
 */
public final class SokalMichenerMetric extends RogersTanimotoMetric {

  /** Sokal Michener distance. */
  public static final SokalMichenerMetric SINGLETON = new SokalMichenerMetric();
}
