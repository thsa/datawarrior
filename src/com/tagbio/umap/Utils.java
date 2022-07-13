/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

/**
 * Utility functions.
 * @author Leland McInnes (Python)
 * @author Sean A. Irvine
 * @author Richard Littin
 */
final class Utils {

  private Utils() {
  }

  /**
   * Get the current date and time as a string of the form
   * <code>YYYY-MM-DD hh:mm:ss</code>.
   * @return date string
   */
  static String now() {
    final StringBuilder sb = new StringBuilder();
    final Calendar cal = new GregorianCalendar();
    sb.append(cal.get(Calendar.YEAR)).append('-');
    final int month = 1 + cal.get(Calendar.MONTH);
    if (month < 10) {
      sb.append('0');
    }
    sb.append(month).append('-');
    final int date = cal.get(Calendar.DATE);
    if (date < 10) {
      sb.append('0');
    }
    sb.append(date).append(' ');
    final int hour = cal.get(Calendar.HOUR_OF_DAY);
    if (hour < 10) {
      sb.append('0');
    }
    sb.append(hour).append(':');
    final int min = cal.get(Calendar.MINUTE);
    if (min < 10) {
      sb.append('0');
    }
    sb.append(min).append(':');
    final int sec = cal.get(Calendar.SECOND);
    if (sec < 10) {
      sb.append('0');
    }
    sb.append(sec).append(' ');
    return sb.toString();
  }

  /**
   * Print a dated message on standard output.
   * @param message message to print
   */
  static void message(final String message) {
    System.out.println(now() + message);
  }

  /**
   * A fast computation of knn indices.
   * @param instances array of shape <code>(nSamples, nFeatures)</code>
   * @param nNeighbors the number of nearest neighbors to compute for each sample in <code>instances</code>
   * @return array of shape <code>(nSamples, nNeighbors)</code> containing the indices of the <code>nNeighbours</code>
   * closest points in the dataset.
   */
  static int[][] fastKnnIndices(final Matrix instances, final int nNeighbors) {
    final int[][] knnIndices = new int[instances.rows()][nNeighbors];
    for (int row = 0; row < instances.rows(); ++row) {
      final int[] v = MathUtils.argsort(Arrays.copyOf(instances.row(row), instances.cols())); // copy to avoid changing original instances
      knnIndices[row] = Arrays.copyOf(v, nNeighbors); // todo Math.min(nNeighbors, v.length) ???
    }
    return knnIndices;
  }

  /**
   * L2 norm of a vector.
   * @param vec vector
   * @return L2 norm
   */
  static float norm(final float[] vec) {
    float result = 0;
    for (final float v : vec) {
      result += v * v;
    }
    return (float) Math.sqrt(result);
  }

  /**
   * Generate <code>nSamples</code> many integers from 0 to <code>poolSize</code> such that no
   * integer is selected twice. The duplication constraint is achieved via
   * rejection sampling.
   * @param nSamples The number of random samples to select from the pool
   * @param poolSize The size of the total pool of candidates to sample from
   * @param random Randomness source
   * @return <code>nSamples </code>randomly selected elements from the pool.
   */
  static int[] rejectionSample(final int nSamples, final int poolSize, final Random random) {
    if (nSamples > poolSize) {
      throw new IllegalArgumentException();
    }
    final int[] result = new int[nSamples];
    for (int i = 0; i < result.length; ++i) {
      int j;
      boolean ok;
      do {
        j = random.nextInt(poolSize);
        ok = true;
        for (int k = 0; k < i; ++k) {
          if (j == result[k]) {
            ok = false;
            break;
          }
        }
      } while (!ok);
      result[i] = j;
    }
    return result;
  }


// @numba.njit(parallel=true)
// def new_build_candidates(
//     current_graph,
//     n_vertices,
//     n_neighbors,
//     max_candidates,
//     rng_state,
//     rho=0.5,
// ):  # pragma: no cover
//     """Build a heap of candidate neighbors for nearest neighbor descent. For
//     each vertex the candidate neighbors are any current neighbors, and any
//     vertices that have the vertex as one of their nearest neighbors.

//     Parameters
//     ----------
//     current_graph: heap
//         The current state of the graph for nearest neighbor descent.

//     n_vertices: int
//         The total number of vertices in the graph.

//     n_neighbors: int
//         The number of neighbor edges per node in the current graph.

//     max_candidates: int
//         The maximum number of new candidate neighbors.

//     rng_state: array of int64, shape (3,)
//         The internal state of the rng

//     Returns
//     -------
//     candidate_neighbors: A heap with an array of (randomly sorted) candidate
//     neighbors for each vertex in the graph.
//     """
//     new_candidate_neighbors = make_heap(
//         n_vertices, max_candidates
//     )
//     old_candidate_neighbors = make_heap(
//         n_vertices, max_candidates
//     )

//     for i in numba.prange(n_vertices):
//         for j in range(n_neighbors):
//             if current_graph[0, i, j] < 0:
//                 continue
//             idx = current_graph[0, i, j]
//             isn = current_graph[2, i, j]
//             d = tau_rand(rng_state)
//             if tau_rand(rng_state) < rho:
//                 c = 0
//                 if isn:
//                     c += heap_push(
//                         new_candidate_neighbors,
//                         i,
//                         d,
//                         idx,
//                         isn,
//                     )
//                     c += heap_push(
//                         new_candidate_neighbors,
//                         idx,
//                         d,
//                         i,
//                         isn,
//                     )
//                 else:
//                     heap_push(
//                         old_candidate_neighbors,
//                         i,
//                         d,
//                         idx,
//                         isn,
//                     )
//                     heap_push(
//                         old_candidate_neighbors,
//                         idx,
//                         d,
//                         i,
//                         isn,
//                     )

//                 if c > 0:
//                     current_graph[2, i, j] = 0

//     return new_candidate_neighbors, old_candidate_neighbors

  /**
   * Return a submatrix given an original matrix and the indices to keep.
   * @param matrix Original matrix of shape <code>(nSamples, nSamples)</code>.
   * @param indicesCol Indices to keep of shape <code>(nSamples, nNeighbors)</code>.
   * Each row consists of the indices of the columns.
   * @param nNeighbors Number of neighbors.
   * @return array, shape <code>(nSamples, nNeighbors)</code>
   * The corresponding submatrix.
   */
  static float[][] submatrix(Matrix matrix, int[][] indicesCol, int nNeighbors) {
    final int nSamplesTransform = matrix.rows();
    final float[][] submat = new float[nSamplesTransform][nNeighbors];
    for (int i = 0; i < nSamplesTransform; ++i) {
      for (int j = 0; j < nNeighbors; ++j) {
        submat[i][j] = matrix.get(i, indicesCol[i][j]);
      }
    }
    return submat;
  }

  static Random[] splitRandom(final Random random, final int n) {
    final Random[] randoms = new Random[n];
    final long baseSeed = random.nextLong();
    for (int j = 0; j < n; ++j) {
      randoms[j] = new Random(baseSeed * (j + 1) + j); 
    }
    return randoms;
  }
}
