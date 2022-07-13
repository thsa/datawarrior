/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap.metric;

import java.util.HashMap;
import java.util.Map;

/**
 * Definition of metrics. Individual subclasses implement specific metrics.
 * A convenience function to select metrics by a string name is also provided.
 * @author Leland McInnes (Python)
 * @author Sean A. Irvine
 * @author Richard Littin
 */
public abstract class Metric {

  private final boolean mIsAngular;

  Metric(final boolean isAngular) {
    mIsAngular = isAngular;
  }

  /**
   * Distance metric.
   * @param x first point
   * @param y second point
   * @return distance between the points
   */
  public abstract float distance(final float[] x, final float[] y);

  /**
   * Is this an angular metric.
   * @return true iff this metric is angular.
   */
  public boolean isAngular() {
    return mIsAngular;
  }

  private static Map<String, Metric> sMETRICS = null;

  /**
   * Retrieve a metric by name.
   * @param name name of metric
   * @return metric
   */
  public static Metric getMetric(final String name) {
    if (sMETRICS == null) {
      sMETRICS = new HashMap<>();
      sMETRICS.put("euclidean", EuclideanMetric.SINGLETON);
      sMETRICS.put("l2", EuclideanMetric.SINGLETON);
      sMETRICS.put("manhattan", ManhattanMetric.SINGLETON);
      sMETRICS.put("l1", ManhattanMetric.SINGLETON);
      sMETRICS.put("taxicab", ManhattanMetric.SINGLETON);
      sMETRICS.put("chebyshev", ChebyshevMetric.SINGLETON);
      sMETRICS.put("linfinity", ChebyshevMetric.SINGLETON);
      sMETRICS.put("linfty", ChebyshevMetric.SINGLETON);
      sMETRICS.put("linf", ChebyshevMetric.SINGLETON);
      sMETRICS.put("canberra", CanberraMetric.SINGLETON);
      sMETRICS.put("cosine", CosineMetric.SINGLETON);
      sMETRICS.put("correlation", CorrelationMetric.SINGLETON);
      sMETRICS.put("haversine", HaversineMetric.SINGLETON);
      sMETRICS.put("braycurtis", BrayCurtisMetric.SINGLETON);
      sMETRICS.put("hamming", HammingMetric.SINGLETON);
      sMETRICS.put("jaccard", JaccardMetric.SINGLETON);
      sMETRICS.put("dice", DiceMetric.SINGLETON);
      sMETRICS.put("matching", MatchingMetric.SINGLETON);
      sMETRICS.put("kulsinski", KulsinskiMetric.SINGLETON);
      sMETRICS.put("rogerstanimoto", RogersTanimotoMetric.SINGLETON);
      sMETRICS.put("russellrao", RussellRaoMetric.SINGLETON);
      sMETRICS.put("sokalsneath", SokalSneathMetric.SINGLETON);
      sMETRICS.put("sokalmichener", SokalMichenerMetric.SINGLETON);
      sMETRICS.put("yule", YuleMetric.SINGLETON);
    }

    final Metric m = sMETRICS.get(name.toLowerCase());
    if (m == null) {
      throw new IllegalArgumentException("Unknown metric: " + name);
    }
    return m;
  }
}
