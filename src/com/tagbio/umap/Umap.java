/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap;

import com.tagbio.umap.metric.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Uniform Manifold Approximation and Projection.
 *
 * Finds a low dimensional embedding of the data that approximates an underlying manifold.
 *
 * @author Leland McInnes (Python)
 * @author Sean A. Irvine (Java port)
 * @author Richard Littin (Java port)
 */
public class Umap {

  private static final float SMOOTH_K_TOLERANCE = 1e-5F;
  private static final float MIN_K_DIST_SCALE = 1e-3F;

  private static final int SMALL_PROBLEM_THRESHOLD = 4096;

  /**
   * Compute a continuous version of the distance to the kth nearest
   * neighbor. That is, this is similar to knn-distance but allows continuous
   * k values rather than requiring an integral k. In essence we are simply
   * computing the distance such that the cardinality of fuzzy set we generate
   * is k.
   * @param distances array of shape <code>(nSamples, nNeighbors)</code>
   * Distances to nearest neighbors for each samples. Each row should be a
   * sorted list of distances to a given samples nearest neighbors.
   * @param k The number of nearest neighbors to approximate for.
   * @param nIter We need to binary search for the correct distance value. This is the
   * max number of iterations to use in such a search.
   * @param localConnectivity The local connectivity required; i.e., the number of nearest
   * neighbors that should be assumed to be connected at a local level.
   * The higher this value the more connected the manifold becomes
   * locally. In practice this should be not more than the local intrinsic
   * dimension of the manifold.
   * @param bandwidth The target bandwidth of the kernel, larger values will produce
   * larger return values.
   * @return two arrays knnDist array of shape <code>(nSamples)</code>
   * The distance to kth nearest neighbor, as suitably approximated.
   * nnDist: array of shape <code>(nSamples)</code>
   * The distance to the first nearest neighbor for each point.
   */
  private static float[][] smoothKnnDist(final float[][] distances, final float k, final int nIter, final int localConnectivity, final float bandwidth) {
    final float target = (float) (MathUtils.log2(k) * bandwidth);
    final float[] rho = new float[distances.length];
    final float[] result = new float[distances.length];

    final float meanDistances = MathUtils.mean(distances);

    for (int i = 0; i < distances.length; ++i) {
      float lo = 0;
      float hi = Float.POSITIVE_INFINITY;
      float mid = 1;

      final float[] ithDistances = distances[i];
      final float[] nonZeroDists = MathUtils.filterPositive(ithDistances);
      if (nonZeroDists.length >= localConnectivity) {
        final int index = (int) Math.floor(localConnectivity);
        final float interpolation = localConnectivity - index;
        if (index > 0) {
          rho[i] = nonZeroDists[index - 1];
          if (interpolation > SMOOTH_K_TOLERANCE) {
            rho[i] += interpolation * (nonZeroDists[index] - nonZeroDists[index - 1]);
          }
        } else {
          rho[i] = interpolation * nonZeroDists[0];
        }
      } else if (nonZeroDists.length > 0) {
        rho[i] = MathUtils.max(nonZeroDists);
      }

      for (int n = 0; n < nIter; ++n) {
        double pSum = 0.0;
        for (int j = 1; j < distances[0].length; ++j) {
          final double d = distances[i][j] - rho[i];
          pSum += d > 0 ? Math.exp(-(d / mid)) : 1;
        }

        if (Math.abs(pSum - target) < SMOOTH_K_TOLERANCE) {
          break;
        }

        if (pSum > target) {
          hi = mid;
          mid = (lo + hi) / 2.0F;
        } else {
          lo = mid;
          if (hi == Float.POSITIVE_INFINITY) {
            mid *= 2;
          } else {
            mid = (lo + hi) / 2.0F;
          }
        }
      }

      result[i] = mid;

      if (rho[i] > 0) {
        final float meanIthDistances = MathUtils.mean(ithDistances);
        if (result[i] < MIN_K_DIST_SCALE * meanIthDistances) {
          result[i] = MIN_K_DIST_SCALE * meanIthDistances;
        }
      } else {
        if (result[i] < MIN_K_DIST_SCALE * meanDistances) {
          result[i] = MIN_K_DIST_SCALE * meanDistances;
        }
      }
    }
    return new float[][]{result, rho};
  }

  static float[][] smoothKnnDist(final float[][] distances, final float k, final int localConnectivity) {
    return smoothKnnDist(distances, k, 64, localConnectivity, 1.0F);
  }

  /**
   * Compute the <code>nNeighbors</code> nearest points for each data point in <code>instances</code>
   * under <code>metric</code>. This may be exact, but more likely is approximated via
   * nearest neighbor descent.
   * @param instances The input data to compute the k-neighbor graph of.
   * @param nNeighbors The number of nearest neighbors to compute for each sample in <code>instances</code>.
   * @param metric The metric to use for the computation.
   * @param angular Whether to use angular rp trees in NN approximation.
   * @param random The random state to use for approximate NN computations.
   * @param verbose Whether to print status data during the computation.
   * @return knnIndices: array of shape <code>(nSamples, nNeighbors)</code>
   *   The indices on the <code>nNeighbors</code> closest points in the dataset.
   *   knnDists: array of shape <code>(nSamples, nNeighbors)</code>
   *   The distances to the <code>nNeighbors</code> closest points in the dataset.
   */
  static IndexedDistances nearestNeighbors(final Matrix instances, final int nNeighbors, final Metric metric, boolean angular, final Random random, final int threads, final boolean verbose) {
    if (verbose) {
      Utils.message("Finding nearest neighbors");
    }
    final int[][] knnIndices;
    final float[][] knnDists;
    final List<FlatTree> rpForest;
    if (metric.equals(PrecomputedMetric.SINGLETON)) {
      // Note that this does not support sparse distance matrices yet ...
      // Compute indices of n nearest neighbors
      knnIndices = Utils.fastKnnIndices(instances, nNeighbors);
      // Compute the nearest neighbor distances
      knnDists = new float[knnIndices.length][nNeighbors];
      for (int i = 0; i < knnDists.length; ++i) {
        for (int j = 0; j < nNeighbors; ++j) {
          knnDists[i][j] = instances.get(i, knnIndices[i][j]);
        }
      }
      rpForest = Collections.emptyList();
    } else {
      boolean isAngular = metric.isAngular();

      if (instances instanceof CsrMatrix) {
//        final CsrMatrix csrInstances = instances.toCsr();
//        // todo this is nonsense now, since metric cannot be a string at this point
//        if (Sparse.SPARSE_NAMED_DISTANCES.containsKey(metric)) {
//          distanceFunc = Sparse.SPARSE_NAMED_DISTANCES.get(metric);
////          if (Sparse.sparse_need_n_features.contains(metric)) {
////            metricKwds.put("n_features", instances.cols());
////          }
//        } else {
//          throw new IllegalArgumentException("Metric " + metric + " not supported for sparse data");
//        }
        throw new UnsupportedOperationException();
//        metric_nn_descent = Sparse.make_sparse_nn_descent(distanceFunc, tuple(metricKwds.values()));
//
//        int n_trees = 5 + (int) (Math.round(Math.pow(Y.rows(), 0.5) / 20.0));
//        int n_iters = Math.max(5, (int) (Math.round(MathUtils.log2(Y.rows()))));
//        if (verbose) {
//          Utils.message("Building RP forest with " + n_trees + " trees");
//        }
//
//        rpForest = RpTree.make_forest(X, nNeighbors, n_trees, random, angular);
//        final int[][] leaf_array = RpTree.rptree_leaf_array(rpForest);
//
//        if (verbose) {
//          Utils.message("NN descent for " + n_iters + " iterations");
//        }
//        final Object[] nn = NearestNeighborDescent.metric_nn_descent(Y.indices, Y.indptr, Y.data, Y.rows(), nNeighbors, random, /*max_candidates=*/60, /*rp_tree_init=*/true,     /*leaf_array=*/leaf_array,  /*n_iters=*/n_iters, verbose);
//        knnIndices = (int[][]) nn[0];
//        knnDists = (float[][]) nn[1];
      } else {
        final NearestNeighborDescent metricNearestNeighborsDescent = threads == 1 ? new NearestNeighborDescent(metric) :  new ParallelNearestNeighborDescent(metric, threads);
        final int nTrees = 5 + (int) (Math.round(Math.pow(instances.rows(), 0.5) / 20.0));
        final int nIters = Math.max(5, (int) (Math.round(MathUtils.log2(instances.rows()))));
        UmapProgress.incTotal(nIters + nTrees + 2);

        if (verbose) {
          Utils.message("Building random projection forest with " + nTrees + " trees");
        }
        rpForest = RandomProjectionTree.makeForest(instances, nNeighbors, nTrees, random, isAngular, threads);
        if (verbose) {
          long nodeCount = 0;
          for (final FlatTree tree : rpForest) {
            for (final int[] a : tree.getIndices()) {
              for (final int b : a) {
                if (b >= 0) {
                  ++nodeCount;
                }
              }
            }
          }
          Utils.message("Total number of values in forest: " + nodeCount);
          Utils.message("NN descent for " + nIters + " iterations");
        }
        metricNearestNeighborsDescent.setVerbose(verbose);
        final Heap nn = metricNearestNeighborsDescent.descent(instances, nNeighbors, random, 60, true, nIters, rpForest);
        knnIndices = nn.indices();
        knnDists = nn.weights();
      }

      if (MathUtils.containsNegative(knnIndices)) {
        Utils.message("Failed to correctly find nearest neighbors for some samples. Results may be less than ideal. Try re-running with different parameters.");
      }
    }
    if (verbose) {
      Utils.message("Finished nearest neighbor search");
    }
    return new IndexedDistances(knnIndices, knnDists, rpForest);
  }

  /**
   * Construct the membership strength data for the 1-skeleton of each local
   * fuzzy simplicial set -- this is formed as a sparse matrix where each row is
   * a local fuzzy simplicial set, with a membership strength for the
   * 1-simplex to each other data point.
   * @param knnIndices array of shape <code>(nSamples, nNeighbors)</code>
   * The indices on the <code>nNeighbors</code> closest points in the dataset.
   * @param knnDists array of shape <code>(nSamples, nNeighbors)</code>
   * The distances to the <code>nNeighbors</code> closest points in the dataset.
   * @param sigmas array of shape <code>(nSamples)</code>
   * The normalization factor derived from the metric tensor approximation.
   * @param rhos array of shape <code>(nSamples)</code>
   * The local connectivity adjustment.
   * @param rowCount number or rows in the result
   * @param colCount number or columns in the result
   * @return sparse matrix of shape <code>(nSamples, nNeighbors)</code>
   */
  static CooMatrix computeMembershipStrengths(final int[][] knnIndices, final float[][] knnDists, final float[] sigmas, final float[] rhos, final int rowCount, final int colCount) {
    final int nSamples = knnIndices.length;
    final int nNeighbors = knnIndices[0].length;
    final int size = nSamples * nNeighbors;

    final int[] rows = new int[size];
    final int[] cols = new int[size];
    final float[] vals = new float[size];

    for (int i = 0; i < nSamples; ++i) {
      for (int j = 0; j < nNeighbors; ++j) {
        if (knnIndices[i][j] == -1) {
          continue;  // We didn't get the full knn for i
        }
        final float val;
        if (knnIndices[i][j] == i) {
          val = 0;
        } else if (knnDists[i][j] - rhos[i] <= 0) {
          val = 1;
        } else {
          val = (float) Math.exp(-((knnDists[i][j] - rhos[i]) / (sigmas[i])));
        }
        rows[i * nNeighbors + j] = i;
        cols[i * nNeighbors + j] = knnIndices[i][j];
        vals[i * nNeighbors + j] = val;
      }
    }
    return new CooMatrix(vals, rows, cols, rowCount, colCount);
  }

  /**
   * Given a set of data X, a neighborhood size, and a measure of distance
   * compute the fuzzy simplicial set (here represented as a fuzzy graph in
   * the form of a sparse matrix) associated to the data. This is done by
   * locally approximating geodesic distance at each point, creating a fuzzy
   * simplicial set for each such point, and then combining all the local
   * fuzzy simplicial sets into a global one via a fuzzy union.
   * @param instances The data to be modelled as a fuzzy simplicial set.
   * @param nNeighbors The number of neighbors to use to approximate geodesic distance.
   * Larger numbers induce more global estimates of the manifold that can
   * miss finer detail, while smaller values will focus on fine manifold
   * structure to the detriment of the larger picture.
   * @param random Randomness source
   * @param metric The metric to use to compute distances in high dimensional space.
   * @param knnIndices array of shape <code>(nSamples, nNeighbors)</code> or null.
   * If the k-nearest neighbors of each point has already been calculated
   * you can pass them in here to save computation time. This should be
   * an array with the indices of the k-nearest neighbors as a row for
   * each data point.
   * @param knnDists array of shape <code>(nSamples, nNeighbors)</code> or null.
   * If the k-nearest neighbors of each point has already been calculated
   * you can pass them in here to save computation time. This should be
   * an array with the distances of the k-nearest neighbors as a row for
   * each data point.
   * @param angular Whether to use angular/cosine distance for the random projection
   * forest for seeding NN-descent to determine approximate nearest
   * neighbors.
   * @param setOpMixRatio Interpolate between (fuzzy) union and intersection as the set operation
   * used to combine local fuzzy simplicial sets to obtain a global fuzzy
   * simplicial sets. Both fuzzy set operations use the product t-norm.
   * The value of this parameter should be between 0.0 and 1.0; a value of
   * 1.0 will use a pure fuzzy union, while 0.0 will use a pure fuzzy
   * intersection.
   * @param localConnectivity The local connectivity required; i.e., the number of nearest
   * neighbors that should be assumed to be connected at a local level.
   * The higher this value the more connected the manifold becomes
   * locally. In practice this should be not more than the local intrinsic
   * dimension of the manifold.
   * @param threads Number of threads
   * @param verbose Whether to report information on the current progress of the algorithm.
   * @return A fuzzy simplicial set represented as a sparse matrix. The <code>(i, j)</code>
   * entry of the matrix represents the membership strength of the
   * 1-simplex between the ith and jth sample points.
   */
  static Matrix fuzzySimplicialSet(final Matrix instances, final int nNeighbors, final Random random, final Metric metric, int[][] knnIndices, float[][] knnDists, final boolean angular, final float setOpMixRatio, final int localConnectivity, final int threads, final boolean verbose) {

    if (knnIndices == null || knnDists == null) {
      final IndexedDistances nn = nearestNeighbors(instances, nNeighbors, metric, angular, random, threads, verbose);
      knnIndices = nn.getIndices();
      knnDists = nn.getDistances();
    }

    final float[][] sigmasRhos = smoothKnnDist(knnDists, nNeighbors, localConnectivity);
    final float[] sigmas = sigmasRhos[0];
    final float[] rhos = sigmasRhos[1];

    final Matrix result = computeMembershipStrengths(knnIndices, knnDists, sigmas, rhos, instances.rows(), instances.rows()).eliminateZeros();
    final Matrix prodMatrix = result.hadamardMultiplyTranspose();

    return result.addTranspose().subtract(prodMatrix).multiply(setOpMixRatio).add(prodMatrix.multiply(1.0F - setOpMixRatio)).eliminateZeros();
  }

  /**
   * Reset the local connectivity requirement -- each data sample should
   * have complete confidence in at least one 1-simplex in the simplicial set.
   * We can enforce this by locally rescaling confidences, and then remerging the
   * different local simplicial sets together.
   * @param simplicialSet The simplicial set for which to recalculate with respect to local connectivity.
   * @return The recalculated simplicial set, now with the local connectivity assumption restored.
   */
  private static Matrix resetLocalConnectivity(final Matrix simplicialSet) {
    final Matrix nss = simplicialSet.rowNormalize();
    final Matrix prodMatrix = nss.hadamardMultiplyTranspose();
    return nss.addTranspose().subtract(prodMatrix).eliminateZeros();
  }

  /**
   * Combine a fuzzy simplicial set with another fuzzy simplicial set
   * generated from categorical data using categorical distances. The target
   * data is assumed to be categorical label data (a vector of labels),
   * and this will update the fuzzy simplicial set to respect that label data.
   * @param simplicialSet The input fuzzy simplicial set.
   * @param target The categorical labels to use in the intersection.
   * @param unknownDist The distance an unknown label (-1) is assumed to be from any point.
   * @param farDist The distance between unmatched labels.
   * @return The resulting intersected fuzzy simplicial set.
   */
  private static Matrix categoricalSimplicialSetIntersection(final CooMatrix simplicialSet, final float[] target, final float unknownDist, final float farDist) {
    simplicialSet.fastIntersection(target, unknownDist, farDist);
    return resetLocalConnectivity(simplicialSet.eliminateZeros());
  }

  private static Matrix generalSimplicialSetIntersection(final Matrix simplicialSet1, final Matrix simplicialSet2, final float weight) {
    final CooMatrix result = simplicialSet1.add(simplicialSet2).toCoo();
    final CsrMatrix left = simplicialSet1.toCsr();
    final CsrMatrix right = simplicialSet2.toCsr();
    left.intersect(right, result, weight);
    return result;
  }

  /**
   * Given a set of weights and number of epochs generate the number of
   * epochs per sample for each weight.
   * @param weights The weights of how much we wish to sample each 1-simplex.
   * @param nEpochs The total number of epochs we want to train for.
   * @return An array of number of epochs per sample, one for each 1-simplex.
   */
  static float[] makeEpochsPerSample(final float[] weights, final int nEpochs) {
    final float[] result = new float[weights.length];
    Arrays.fill(result, -1.0F);
    final float[] nSamples = MathUtils.multiply(MathUtils.divide(weights, MathUtils.max(weights)), nEpochs);
    for (int k = 0; k < nSamples.length; ++k) {
      if (nSamples[k] > 0) {
        result[k] = (float) nEpochs / nSamples[k];
      }
    }
    return result;
  }

  /**
   * Standard clamping of a value into a fixed range (in this case -4.0 to 4.0)
   * @param val The value to be clamped.
   * @return clamped value
   */
  static float clip(final float val) {
    return val > 4 ? 4 : val < -4 ? -4 : val;
  }

  /**
   * Improve an embedding using stochastic gradient descent to minimize the
   * fuzzy set cross entropy between the 1-skeletons of the high dimensional
   * and low dimensional fuzzy simplicial sets. In practice this is done by
   * sampling edges based on their membership strength (with the (1-p) terms
   * coming from negative sampling similar to word2vec).
   * @param headEmbedding array of shape <code>(nSamples, nComponents)</code>
   * The initial embedding to be improved by SGD.
   * @param tailEmbedding array of shape <code>(sourceSamples, nComponents)</code>
   * The reference embedding of embedded points. If not embedding new
   * previously unseen points with respect to an existing embedding this
   * is simply the <code>headEmbedding</code> (again); otherwise it provides the
   * existing embedding to embed with respect to.
   * @param head array of shape (n_1_simplices)
   * The indices of the heads of 1-simplices with non-zero membership.
   * @param tail array of shape (n_1_simplices)
   * The indices of the tails of 1-simplices with non-zero membership.
   * @param nEpochs The number of training epochs to use in optimization.
   * @param nVertices The number of vertices (0-simplices) in the dataset.
   * @param epochsPerSample A float value of the number of epochs per 1-simplex. 1-simplices with
   * weaker membership strength will have more epochs between being sampled.
   * @param a Parameter of differentiable approximation of right adjoint functor
   * @param b Parameter of differentiable approximation of right adjoint functor
   * @param random Random source
   * @param gamma Weight to apply to negative samples.
   * @param initialAlpha Initial learning rate for the SGD.
   * @param negativeSampleRate Number of negative samples to use per positive sample.
   * @param verbose Whether to report information on the current progress of the algorithm.
   * @return array of shape <code>(nSamples, nComponents)</code> The optimized embedding.
   */
  private Matrix optimizeLayout(final Matrix headEmbedding, final Matrix tailEmbedding, final int[] head, final int[] tail, final int nEpochs, final int nVertices, final float[] epochsPerSample, final float a, final float b, final Random random, final float gamma, final float initialAlpha, final float negativeSampleRate, final boolean verbose) {

    if (!(headEmbedding instanceof DefaultMatrix)) {
      throw new UnsupportedOperationException("Require matrix we can set entries on");
    }

    final int dim = headEmbedding.cols();
    final boolean moveOther = headEmbedding.rows() == tailEmbedding.rows();
    float alpha = initialAlpha;

    final float[] epochsPerNegativeSample = MathUtils.divide(epochsPerSample, negativeSampleRate);
    final float[] epochOfNextNegativeSample = Arrays.copyOf(epochsPerNegativeSample, epochsPerNegativeSample.length);
    final float[] epochOfNextSample = Arrays.copyOf(epochsPerSample, epochsPerSample.length);

    for (int n = 0; n < nEpochs; ++n) {
      for (int i = 0; i < epochsPerSample.length; ++i) {
        if (epochOfNextSample[i] <= n) {
          final int j = head[i];
          final int k = tail[i];
          // Note this assumes that "current" is a pointer to the internal matrix data,
          // not ideal from a data encapsulation point of view.
          final float[] current = headEmbedding.row(j);
          float[] other = tailEmbedding.row(k);

          float distSquared = ReducedEuclideanMetric.SINGLETON.distance(current, other);

          float gradCoeff;
          if (distSquared > 0.0) {
            gradCoeff = (float) ((-2.0 * a * b * Math.pow(distSquared, b - 1.0)) / (a * Math.pow(distSquared, b) + 1.0));
          } else {
            gradCoeff = 0;
          }

          for (int d = 0; d < dim; ++d) {
            final float gradD = clip(gradCoeff * (current[d] - other[d]));
            current[d] += gradD * alpha;
            if (moveOther) {
              other[d] += -gradD * alpha;
            }
          }

          epochOfNextSample[i] += epochsPerSample[i];

          final int nNegSamples = (int) ((n - epochOfNextNegativeSample[i]) / epochsPerNegativeSample[i]);

          for (int p = 0; p < nNegSamples; ++p) {
            final int kr = random.nextInt(nVertices);
            other = tailEmbedding.row(kr);
            distSquared = ReducedEuclideanMetric.SINGLETON.distance(current, other);

            if (distSquared > 0) {
              gradCoeff = 2.0F * gamma * b / (float) ((0.001 + distSquared) * (a * Math.pow(distSquared, b) + 1));
            } else if (j == kr) {
              continue;
            } else {
              gradCoeff = 0;
            }

            for (int d = 0; d < dim; ++d) {
              final float gradD = gradCoeff > 0.0 ? clip(gradCoeff * (current[d] - other[d])) : 4;
              current[d] += gradD * alpha;
            }
          }

          epochOfNextNegativeSample[i] += nNegSamples * epochsPerNegativeSample[i];
        }
      }

      alpha = initialAlpha * (1 - (float) n / (float) nEpochs);

      if (verbose && n % (nEpochs / 10) == 0) {
        Utils.message("Completed " + n + "/" + nEpochs);
      }
      UmapProgress.update();
    }
    return headEmbedding;
  }

  /**
   * Perform a fuzzy simplicial set embedding, using a specified
   * initialisation method and then minimizing the fuzzy set cross entropy
   * between the 1-skeletons of the high and low dimensional fuzzy simplicial
   *  sets.
   * @param data array of shape <code>(nSamples, nFeatures)</code>
   * The source data to be embedded by UMAP.
   * @param graphIn The 1-skeleton of the high dimensional fuzzy simplicial set as
   * represented by a graph for which we require a sparse matrix for the
   * (weighted) adjacency matrix.
   * @param nComponents The dimensionality of the euclidean space into which to embed the data.
   * @param initialAlpha Initial learning rate for the SGD.
   * @param a Parameter of differentiable approximation of right adjoint functor.
   * @param b Parameter of differentiable approximation of right adjoint functor.
   * @param gamma Weight to apply to negative samples.
   * @param negativeSampleRate The number of negative samples to select per positive sample
   * in the optimization process. Increasing this value will result
   * in greater repulsive force being applied, greater optimization
   * cost, but slightly more accuracy.
   * @param nEpochs The number of training epochs to be used in optimizing the
   * low dimensional embedding. Larger values result in more accurate
   * embeddings. If 0 is specified a value will be selected based on
   * the size of the input dataset (200 for large datasets, 500 for small).
   * @param init How to initialize the low dimensional embedding. Options are:
   * * 'spectral': use a spectral embedding of the fuzzy 1-skeleton
   * * 'random': assign initial embedding positions at random.
   * * A numpy array of initial embedding positions.
   * @param random random source
   * @param metric The metric used to measure distance in high dimensional space; used if
   * multiple connected components need to be layed out.
   * @param verbose Whether to report information on the current progress of the algorithm.
   * @return array of shape <code>(nSamples, nComponents)</code>
   * The optimized of <code>graph</code> into an <code>nComponents</code> dimensional
   * Euclidean space.
   */
  private Matrix simplicialSetEmbedding(Matrix data, Matrix graphIn, int nComponents, float initialAlpha, float a, float b, float gamma, int negativeSampleRate, int nEpochs, String init, Random random, Metric metric, boolean verbose) {

    CooMatrix graph = graphIn.toCoo();
    final int nVertices = graph.cols();

    if (nEpochs <= 0) {
      // For smaller datasets we can use more epochs
      if (graph.rows() <= 10000) {
        nEpochs = 500;
      } else {
        nEpochs = 200;
      }
    }

    final float[] graphData = graph.data();
    MathUtils.zeroEntriesBelowLimit(graphData, MathUtils.max(graphData) / (float) nEpochs);
    graph = (CooMatrix) graph.eliminateZeros();

    final Matrix embedding;
    if ("random".equals(init)) {
      //embedding = random.uniform(low = -10.0, high = 10.0, size = (graph.rows(), nComponents)).astype(np.float32);
      embedding = new DefaultMatrix(MathUtils.uniform(random, -10, 10, graph.rows(), nComponents));
    } else if ("spectral".equals(init)) {
      throw new UnsupportedOperationException();
//      // We add a little noise to avoid local minima for optimization to come
//      float[][] initialisation = Spectral.spectral_layout(data, graph, nComponents, random, /*metric=*/metric, /*metric_kwds=*/metric_kwds);
//      float expansion = 10.0 / Math.abs(initialisation).max();
//      embedding = (MathUtils.multiply(initialisation, expansion)).astype(np.float32) + random.normal(scale = 0.0001, size =[graph.rows(), nComponents]).astype(np.float32);
    } else {
      // Situation where init contains prepared data
      throw new UnsupportedOperationException();
//      init_data = np.array(init);
//      if (len(init_data.shape) == 2) {
//        if (np.unique(init_data, /*axis =*/ 0).length < init_data.length) {
//          tree = KDTree(init_data);
//          float[][] dist /*, ind*/ = tree.query(init_data, k = 2);
//          double nndist = MathUtils.mean(dist, 1);
//          embedding = init_data + random.normal(scale = 0.001 * nndist, size = init_data.shape).astype(np.float32);
//        } else {
//          embedding = init_data;
//        }
//      }
    }

    final float[] epochsPerSample = makeEpochsPerSample(graph.data(), nEpochs);
    final int[] head = graph.row();
    final int[] tail = graph.col();

    // so (head, tail, epochsPerSample) is like a CooMatrix

    return optimizeLayout(embedding, embedding, head, tail, nEpochs, nVertices, epochsPerSample, a, b, random, gamma, initialAlpha, negativeSampleRate, verbose);
  }

  /**
   * Given indices and weights and an original embeddings
   * initialize the positions of new points relative to the
   * indices and weights (of their neighbors in the source data).
   * @param indices array of shape <code>(nNewSamples, nNeighbors)</code>
   * The indices of the neighbors of each new sample
   * @param weights array of shape <code>(nNewSamples, nNeighbors)</code>
   * The membership strengths of associated 1-simplices
   * for each of the new samples.
   * @param embedding array of shape <code>(nSamples, dim)</code>
   * The original embedding of the source data.
   * @return array of shape <code>(nNewSamples, dim)</code>
   * An initial embedding of the new sample points.
   */
  private static Matrix initTransform(final int[][] indices, final float[][] weights, final Matrix embedding) {
    final float[][] result = new float[indices.length][embedding.cols()];
    for (int i = 0; i < indices.length; ++i) {
      for (int j = 0; j < indices[i].length; ++j) {
        for (int d = 0; d < embedding.cols(); ++d) {
          result[i][d] += weights[i][j] * embedding.get(indices[i][j], d);
        }
      }
    }
    return new DefaultMatrix(result);
  }
  
  // Fit a, b params for the differentiable curve used in lower
  // dimensional fuzzy simplicial complex construction. We want the
  // smooth curve (from a pre-defined family with simple gradient) that
  // best matches an offset exponential decay.
  private static float[] findAbParams(float spread, float minDist) {
    //System.out.println("find_ab_params(" + spread + ", " + minDist + ")");
    /*
    float[] xv = MathUtils.linspace(0, spread * 3, 300);
    float[] yv = new float[xv.length];
    //  yv[xv < minDist] = 1.0;
    //  yv[xv >= minDist] = Math.exp(-(xv[xv >= minDist] - minDist) / spread   );
    for (int k = 0; k < yv.length; ++k) {
      if (xv[k] < minDist) {
        yv[k] = 1.0F;
      } else {
        yv[k] = (float) Math.exp(-(xv[k] - minDist) / spread);
      }
    }

    final float[] params = Curve.curve_fit(xv, yv);
    return new float[]{params[0], params[1]};
    */
    return Curve.curveFit(spread, minDist);
  }

  private boolean mAngularRpForest = false;
  private int mNNeighbors = 15;
  private int mNComponents = 2;
  private Integer mNEpochs = null;
  private Metric mMetric = EuclideanMetric.SINGLETON;
  private float mLearningRate = 1.0F;
  private float mRepulsionStrength = 1.0F;
  private float mMinDist = 0.1F;
  private float mSpread = 1.0F;
  private float mSetOpMixRatio = 1.0F;
  private int mLocalConnectivity = 1;
  private int mNegativeSampleRate = 5;
  private float mTransformQueueSize = 4.0F;
  private Metric mTargetMetric = CategoricalMetric.SINGLETON;
  private int mTargetNNeighbors = -1;
  private float mTargetWeight = 0.5F;
//  private int mTransformSeed = 42;
  private boolean mVerbose = false;
//  private final Float mA = null;
//  private final Float mB = null;
  private Random mRandom = new Random(42);
  private int mThreads = 1;

  private float mInitialAlpha;
  private int mRunNNeighbors;
  private float mRunA;
  private float mRunB;
  private Matrix mRawData;
  private SearchGraph mSearchGraph = null;
  private int[][] mKnnIndices;
  private float[][] mKnnDists;
  private List<FlatTree> mRpForest;
  private boolean mSmallData;
  private Matrix mGraph;
  private Matrix mEmbedding;
  private NearestNeighborSearch mSearch;

  /**
   * Set the size local neighborhood (in terms of number of neighboring
   * sample points) used for manifold approximation. Larger values
   * result in more global views of the manifold, while smaller
   * values result in more local data being preserved. In general
   * values should be in the range 2 to 100.  The default is 15.
   * @param neighbors number of neighbors
   * @return this Umap object
   */
  public Umap setNumberNearestNeighbours(final int neighbors) {
    if (neighbors < 2) {
      throw new IllegalArgumentException("Number of neighbors must be greater than 2.");
    }
    mNNeighbors = neighbors;
    return this;
  }

  /**
   * Set the dimension of the space to embed into. This defaults to 2 to
   * provide easy visualization, but can reasonably be set to any
   * integer value in the range 2 to 100.
   * @param components dimension of embedding space
   * @return this Umap object
   */
  public Umap setNumberComponents(final int components) {
    if (components < 1) {
      throw new IllegalArgumentException("Number of components must be greater than 0.");
    }
    mNComponents = components;
    return this;
  }

  /**
   * Set the number of training epochs to be used in optimizing the
   * low dimensional embedding. Larger values result in more accurate
   * embeddings. If null is specified a value will be selected based on
   * the size of the input dataset (200 for large datasets, 500 for small).
   * The minimum value is 11.
   * @param epochs number of epochs or null
   * @return this Umap object
   */
  public Umap setNumberEpochs(final Integer epochs) {
    if (epochs != null && epochs <= 10) {
      throw new IllegalArgumentException("Epochs must be larger than 10.");
    }
    mNEpochs = epochs;
    return this;
  }

  /**
   * Set the metric to use to compute distances in high dimensional space.  If the
   * metric requires additional parameters, then they are assumed to have been
   * already appropriately initialized.
   * @param metric metric function
   * @return this Umap object
   */
  public Umap setMetric(final Metric metric) {
    if (metric == null) {
      throw new NullPointerException("Null metric not permitted.");
    }
    mMetric = metric;
    return this;
  }

  /**
   * Set the metric to use to compute distances in high dimensional space by name.
   * Valid string metrics include:
   * euclidean,
   * manhattan,
   * chebyshev,
   * minkowski,
   * canberra,
   * braycurtis,
   * cosine,
   * correlation,
   * haversine,
   * hamming,
   * jaccard,
   * dice,
   * russelrao,
   * kulsinski,
   * rogerstanimoto,
   * sokalmichener,
   * sokalsneath,
   * yule.
   * @param metric metric function specified by name
   * @return this Umap object
   */
  public Umap setMetric(final String metric) {
    setMetric(Metric.getMetric(metric));
    return this;
  }

  /**
   * Set the initial learning rate for the embedding optimization.
   * Default 1.0.
   * @param rate learning rate
   * @return this Umap object
   */
  public Umap setLearningRate(final float rate) {
    if (rate <= 0.0) {
      throw new IllegalArgumentException("Learning rate must be positive.");
    }
    mLearningRate = rate;
    return this;
  }

  /**
   * Set weighting applied to negative samples in low dimensional embedding
   * optimization. Values higher than one will result in greater weight
   * being given to negative samples. Default 1.0.
   * @param repulsionStrength repulsion strength
   * @return this Umap object
   */
  public Umap setRepulsionStrength(final float repulsionStrength) {
    if (repulsionStrength < 0.0) {
      throw new IllegalArgumentException("Repulsion strength cannot be negative.");
    }
    mRepulsionStrength = repulsionStrength;
    return this;
  }

  /**
   * Set the effective minimum distance between embedded points. Smaller values
   * will result in a more clustered/clumped embedding where nearby points
   * on the manifold are drawn closer together, while larger values will
   * result on a more even dispersal of points. The value should be set
   * relative to the <code>spread</code> value, which determines the scale at which
   * embedded points will be spread out. Default 0.1.
   * @param minDist minimum distance
   * @return this Umap object
   */
  public Umap setMinDist(final float minDist) {
    if (minDist < 0.0) {
      throw new IllegalArgumentException("Minimum distance must be greater than 0.0.");
    }
    mMinDist = minDist;
    return this;
  }

  /**
   * Set the effective scale of embedded points. In combination with <code>minDist</code>
   * this determines how clustered/clumped the embedded points are. Default 1.0.
   * @param spread spread value
   * @return this Umap object
   */
  public Umap setSpread(final float spread) {
    mSpread = spread;
    return this;
  }

  /**
   * Interpolate between (fuzzy) union and intersection as the set operation
   * used to combine local fuzzy simplicial sets to obtain a global fuzzy
   * simplicial sets. Both fuzzy set operations use the product t-norm.
   * The value of this parameter should be between 0.0 and 1.0; a value of
   * 1.0 will use a pure fuzzy union, while 0.0 will use a pure fuzzy
   * intersection. Default 1.0.
   * @param setOpMixRatio set operation mixing ratio
   * @return this Umap object
   */
  public Umap setSetOpMixRatio(final float setOpMixRatio) {
    if (setOpMixRatio < 0.0 || setOpMixRatio > 1.0) {
      throw new IllegalArgumentException("Set operation mixing ratio be between 0.0 and 1.0.");
    }
    mSetOpMixRatio = setOpMixRatio;
    return this;
  }

  /**
   * Set the local connectivity required; i.e., the number of nearest
   * neighbors that should be assumed to be connected at a local level.
   * The higher this value the more connected the manifold becomes
   * locally. In practice this should be not more than the local intrinsic
   * dimension of the manifold. Default 1.
   * @param localConnectivity local connectivity
   * @return this Umap object
   */
  public Umap setLocalConnectivity(final int localConnectivity) {
    mLocalConnectivity = localConnectivity;
    return this;
  }

  /**
   * Set the number of negative samples to select per positive sample
   * in the optimization process. Increasing this value will result
   * in greater repulsive force being applied, greater optimization
   * cost, but slightly more accuracy. Default 5.
   * @param negativeSampleRate negative sample rate
   * @return this Umap object
   */
  public Umap setNegativeSampleRate(final int negativeSampleRate) {
    if (negativeSampleRate <= 0) {
      throw new IllegalArgumentException("Negative sample rate must be positive.");
    }
    mNegativeSampleRate = negativeSampleRate;
    return this;
  }

  /**
   * Set the metric used to measure distance for a target array is using supervised
   * dimension reduction. By default this is <code>CategoricalMetric.SINGLETON</code> which will measure
   * distance in terms of whether categories match or are different. Furthermore,
   * if semi-supervised is required target values of -1 will be treated as
   * unlabelled under the <code>CategoricalMetric</code> metric. If the target array takes
   * continuous values (e.g. for a regression problem) then metric of 'l1'
   * or 'l2' is probably more appropriate.
   * @param targetMetric target metric
   * @return this Umap object
   */
  public Umap setTargetMetric(final Metric targetMetric) {
    mTargetMetric = targetMetric;
    return this;
  }

  /**
   * Set the target metric by name (see <code>setMetric</code> for a list of values).
   * @param targetMetric target metric
   * @return this Umap object
   */
  public Umap setTargetMetric(final String targetMetric) {
    setTargetMetric(Metric.getMetric(targetMetric));
    return this;
  }

  /**
   * If true, turn on additional diagnostic output.
   * @param verbose verbose level
   * @return this Umap object
   */
  public Umap setVerbose(final boolean verbose) {
    mVerbose = verbose;
    return this;
  }

  /**
   * Set the random number generator to be used.
   * @param random randomness source
   * @return this Umap object
   */
  public Umap setRandom(final Random random) {
    mRandom = random;
    return this;
  }

  /**
   * Set the seed of the random number generator.
   * @param seed seed value
   * @return this Umap object
   */
  public Umap setSeed(final long seed) {
    mRandom.setSeed(seed);
    return this;
  }

  /**
   * For transform operations (embedding new points using a trained model
   * this will control how aggressively to search for nearest neighbors.
   * Larger values will result in slower performance but more accurate
   * nearest neighbor evaluation. Default 4.0.
   * @param transformQueueSize queue size
   * @return this Umap object
   */
  public Umap setTransformQueueSize(final float transformQueueSize) {
    mTransformQueueSize = transformQueueSize;
    return this;
  }

  /**
   * Whether to use an angular random projection forest to initialise
   * the approximate nearest neighbor search. This can be faster, but is
   * mostly on useful for metric that use an angular style distance such
   * as cosine, correlation etc. In the case of those metrics angular forests
   * will be chosen automatically.
   * @param angularRpForest true for an angular random projection forest
   * @return this Umap object
   */
  public Umap setAngularRpForest(final boolean angularRpForest) {
    mAngularRpForest = angularRpForest;
    return this;
  }

  /**
   * The number of nearest neighbors to use to construct the target simplicial
   * set. If set to -1 use the <code>nNeighbors</code> value.
   * @param targetNNeighbors target nearest neighbours
   * @return this Umap object
   */
  public Umap setTargetNNeighbors(final int targetNNeighbors) {
    if (targetNNeighbors < 2 && targetNNeighbors != -1) {
      throw new IllegalArgumentException("targetNNeighbors must be greater than 2");
    }
    mTargetNNeighbors = targetNNeighbors;
    return this;
  }

// a: float (optional, default null)
//     More specific parameters controlling the embedding. If null these
//     values are set automatically as determined by <code>minDist</code> and
//     <code>spread</code>.
// b: float (optional, default null)
//     More specific parameters controlling the embedding. If null these
//     values are set automatically as determined by <code>minDist</code> and
//     <code>spread</code>.

  /**
   * Set weighting factor between data topology and target topology. A value of
   * 0.0 weights entirely on data, a value of 1.0 weights entirely on target.
   * The default of 0.5 balances the weighting equally between data and target.
   * @param targetWeight target weighting factor
   * @return this Umap object
   */
  public Umap setTargetWeight(final float targetWeight) {
    mTargetWeight = targetWeight;
    return this;
  }

//  /**
//   * Random seed used for the stochastic aspects of the transform operation.
//   * This ensures consistency in transform operations. Default: 42.
//   * @param transformSeed random number generator seed
//   */
//  public Umap setTransformSeed(final int transformSeed) {
//    mTransformSeed = transformSeed;
//  }

  /**
   * Set the maximum number of threads to use (default 1).
   * @param threads number of threads
   * @return this Umap object
   */
  public Umap setThreads(final int threads) {
    if (threads < 1) {
      throw new IllegalArgumentException("threads must be at least 1");
    }
    mThreads = threads;
    return this;
  }

  private void validateParameters() {
    if (mMinDist > mSpread) {
      throw new IllegalArgumentException("minDist must be less than or equal to spread");
    }
//    if (!isinstance(init, str) && !isinstance(init, np.ndarray)) {
//      throw new IllegalArgumentException("init must be a string or ndarray");
//    }
//    if (isinstance(init, np.ndarray) && init.shape[1] != nComponents) {
//      throw new IllegalArgumentException("init ndarray must match nComponents value");
//    }
  }

  /**
   * Fit instances into an embedded space.
   * Optionally use y for supervised dimension reduction.
   * @param instances array of shape <code>(nSamples, nFeatures)</code> or <code>(nSamples, nSamples)</code>
   * If the metric is 'precomputed' instances must be a square distance
   * matrix. Otherwise it contains a sample per row. If the method
   * is 'exact',
   * @param y array of shape <code>(nSamples)</code>
   * A target array for supervised dimension reduction. How this is
   * handled is determined by parameters UMAP was instantiated with.
   * The relevant metric is <code>mTargetMetric</code>.
   * @throws IllegalArgumentException if the matrix contains non-finite elements.
   */
  private void fit(Matrix instances, float[] y) {

    if (!instances.isFinite()) {
      throw new IllegalArgumentException("Supplied matrix of instances contains non-finite elements");
    }

    UmapProgress.reset(5);

    if (mVerbose) {
      Utils.message("Starting fitting for " + instances.rows() + " instances with " + instances.cols() + " attributes");
    }

    mRawData = instances;

    // Handle all the optional arguments, setting default
    //if (mA == null || mB == null) {
      final float[] ab = findAbParams(mSpread, mMinDist);
      mRunA = ab[0];
      mRunB = ab[1];
//    } else {
//      mRunA = mA;
//      mRunB = mB;
//    }

    mInitialAlpha = mLearningRate;

    validateParameters();

    UmapProgress.update();

    // Error check n_neighbors based on data size
    if (instances.rows() <= mNNeighbors) {
      if (instances.rows() == 1) {
        mEmbedding = new DefaultMatrix(new float[1][mNComponents]);
        return;
      }

      Utils.message("nNeighbors is larger than the dataset size; truncating to X.length - 1");
      mRunNNeighbors = instances.rows() - 1;
    } else {
      mRunNNeighbors = mNNeighbors;
    }

    if (mVerbose) {
      Utils.message("Construct fuzzy simplicial set: " + instances.rows());
    }
    UmapProgress.update();

    // Handle small cases efficiently by computing all distances
    if (instances.rows() < SMALL_PROBLEM_THRESHOLD) {
      mSmallData = true;
      final Matrix dmat = PairwiseDistances.pairwiseDistances(instances, mMetric);
      mGraph = fuzzySimplicialSet(dmat, mRunNNeighbors, mRandom, PrecomputedMetric.SINGLETON, null, null, mAngularRpForest, mSetOpMixRatio, mLocalConnectivity, mThreads, mVerbose);
    } else {
      mSmallData = false;
      // Standard case
      final IndexedDistances nn = nearestNeighbors(instances, mRunNNeighbors, mMetric, mAngularRpForest, mRandom, mThreads, mVerbose);
      mKnnIndices = nn.getIndices();
      mKnnDists = nn.getDistances();
      mRpForest = nn.getForest();

      mGraph = fuzzySimplicialSet(instances, mNNeighbors, mRandom, mMetric, mKnnIndices, mKnnDists, mAngularRpForest, mSetOpMixRatio, mLocalConnectivity, mThreads, mVerbose);

      final Metric distanceFunc = mMetric;
      if (mMetric == PrecomputedMetric.SINGLETON) {
        Utils.message("Using precomputed metric; transform will be unavailable for new data");
      } else {
        mSearch = new NearestNeighborSearch(distanceFunc);
      }
    }
    UmapProgress.update();
    if (y != null) {
      if (instances.length() != y.length) {
        throw new IllegalArgumentException("Length of x =  " + instances.length() + ", length of y = " + y.length + ", while it must be equal.");
      }
      if (CategoricalMetric.SINGLETON.equals(mTargetMetric)) {
        final float farDist = mTargetWeight < 1 ? 2.5F * (1.0F / (1.0F - mTargetWeight)) : 1.0e12F;
        mGraph = categoricalSimplicialSetIntersection((CooMatrix) mGraph, y, 1, farDist);
      } else {
        final int targetNNeighbors = mTargetNNeighbors == -1 ? mRunNNeighbors : mTargetNNeighbors;

        final Matrix targetGraph;
        // Handle the small case as precomputed as before
        if (y.length < SMALL_PROBLEM_THRESHOLD) {
          final Matrix ydmat = PairwiseDistances.pairwiseDistances(MathUtils.promoteTranspose(y), mTargetMetric);
          targetGraph = fuzzySimplicialSet(ydmat, targetNNeighbors, mRandom, PrecomputedMetric.SINGLETON, null, null, false, 1, 1, mThreads, false);
        } else {
          // Standard case
          targetGraph = fuzzySimplicialSet(MathUtils.promoteTranspose(y), targetNNeighbors, mRandom, mTargetMetric, null, null, false, 1, 1, mThreads, false);
        }
        mGraph = generalSimplicialSetIntersection(mGraph, targetGraph, mTargetWeight);
        mGraph = resetLocalConnectivity(mGraph);
      }
    }
    UmapProgress.incTotal(mNEpochs == null ? (mGraph.rows() <= 10000 ? 500 : 200) : mNEpochs);
    UmapProgress.update();

    final int nEpochs = mNEpochs == null ? 0 : mNEpochs;

    if (mVerbose) {
      Utils.message("Construct embedding");
    }

    mEmbedding = simplicialSetEmbedding(mRawData, mGraph, mNComponents, mInitialAlpha, mRunA, mRunB, mRepulsionStrength, mNegativeSampleRate, nEpochs, "random", mRandom, mMetric, mVerbose);

    if (mVerbose) {
      Utils.message("Finished embedding");
    }
    UmapProgress.finished();
  }

  /**
   * Fit instances into an embedded space and return that transformed output.
   * @param instances array of shape <code>(nSamples, nFeatures)</code> or <code>(nSamples, nSamples)</code>
   * If the metric is <code>PrecomputedMetric.SINGLETON</code> instances must be a square distance
   * matrix. Otherwise it contains a sample per row.
   * @param y array of shape <code>(nSamples)</code>
   * A target array for supervised dimension reduction. How this is
   * handled is determined by parameters UMAP was instantiated with.
   * The relevant metric is <code>mTargetMetric</code>.
   * @return array of shape <code>(nSamples, nComponents)</code>
   * Embedding of the training data in low-dimensional space.
   */
  public Matrix fitTransform(final Matrix instances, final float[] y) {
    fit(instances, y);
    return mEmbedding;
  }

  /**
   * Fit instances into an embedded space and return that transformed output.
   * @param instances array of shape <code>(nSamples, nFeatures)</code> or <code>(nSamples, nSamples)</code>
   * If the metric is <code>PrecomputedMetric.SINGLETON</code> instances must be a square distance
   * matrix. Otherwise it contains a sample per row.
   * A target array for supervised dimension reduction. How this is
   * handled is determined by parameters UMAP was instantiated with.
   * The relevant metric is <code>mTargetMetric</code>.
   * @return array of shape <code>(nSamples, nComponents)</code>
   * Embedding of the training data in low-dimensional space.
   */
  public Matrix fitTransform(final Matrix instances) {
    return fitTransform(instances, null);
  }

  /**
   * Fit instances into an embedded space and return that transformed output.
   * @param instances array of shape <code>(nSamples, nFeatures)</code> or <code>(nSamples, nSamples)</code>
   * If the metric is <code>PrecomputedMetric.SINGLETON</code> instances must be a square distance
   * matrix. Otherwise it contains a sample per row.
   * A target array for supervised dimension reduction. How this is
   * handled is determined by parameters UMAP was instantiated with.
   * The relevant metric is <code>mTargetMetric</code>.
   * @return array of shape <code>(nSamples, nComponents)</code>
   * Embedding of the training data in low-dimensional space.
   */
  public float[][] fitTransform(final float[][] instances) {
    return fitTransform(new DefaultMatrix(instances), null).toArray();
  }

  /**
   * Fit instances into an embedded space and return that transformed output.
   * This version internally converts all the doubles to floats.
   * @param instances array of shape <code>(nSamples, nFeatures)</code> or <code>(nSamples, nSamples)</code>
   * If the metric is <code>PrecomputedMetric.SINGLETON</code> instances must be a square distance
   * matrix. Otherwise it contains a sample per row.
   * A target array for supervised dimension reduction. How this is
   * handled is determined by parameters UMAP was instantiated with.
   * The relevant metric is <code>mTargetMetric</code>.
   * @return array of shape <code>(nSamples, nComponents)</code>
   * Embedding of the training data in low-dimensional space.
   */
  public double[][] fitTransform(final double[][] instances) {
    final float[][] input = new float[instances.length][instances[0].length];
    for (int k = 0; k < instances.length; ++k) {
      for (int j = 0; j < instances[0].length; ++j) {
        input[k][j] = (float) instances[k][j];
      }
    }
    final Matrix result = fitTransform(new DefaultMatrix(input), null);
    final double[][] output = new double[result.rows()][result.cols()];
    for (int k = 0; k < result.rows(); ++k) {
      for (int j = 0; j < result.cols(); ++j) {
        output[k][j] = result.get(k, j);
      }
    }
    return output;
  }

  /**
   * Transform instances into the existing embedded space and return that
   * transformed output.
   * @param instances array, shape <code>(nSamples, nFeatures)</code>
   * New data to be transformed.
   * @return array, shape <code>(nSamples, nComponents)</code>
   * Embedding of the new data in low-dimensional space.
   * @throws IllegalArgumentException If we fit just a single instance then error.
   */
  public Matrix transform(Matrix instances) {
    if (mEmbedding.rows() == 1) {
      throw new IllegalArgumentException("Transform unavailable when model was fit with only a single data sample.");
    }
    if (mRawData instanceof CsrMatrix) {
      throw new IllegalArgumentException("Transform not available for sparse input.");
    } else if (mMetric instanceof PrecomputedMetric) {
      throw new IllegalArgumentException("Transform of new data not available for precomputed metric.");
    }
    UmapProgress.reset(4);

    int[][] indices;
    final float[][] dists;
    if (mSmallData) {
      final Matrix distanceMatrix = PairwiseDistances.pairwiseDistances(instances, mRawData, mMetric);
      indices = new int[distanceMatrix.rows()][];
      for (int k = 0; k < distanceMatrix.rows(); ++k) {
        indices[k] = MathUtils.argsort(Arrays.copyOf(distanceMatrix.row(k), distanceMatrix.cols()));
      }
      indices = MathUtils.subarray(indices, mRunNNeighbors);
      dists = Utils.submatrix(distanceMatrix, indices, mRunNNeighbors);
    } else {
      final Heap init = NearestNeighborDescent.initialiseSearch(mRpForest, mRawData, instances, (int) (mRunNNeighbors * mTransformQueueSize), mSearch, mRandom);
      if (mSearchGraph == null) {
        mSearchGraph = new SearchGraph(mRawData.rows());
        for (int k = 0; k < mKnnIndices.length; ++k) {
          for (int j = 0; j < mKnnIndices[k].length; ++j) {
            if (mKnnDists[k][j] != 0) {
              mSearchGraph.set(k, mKnnIndices[k][j]);
            }
          }
        }
      }
      final Heap result = mSearch.initializedNndSearch(mRawData, mSearchGraph, init, instances).deheapSort();
      indices = MathUtils.subarray(result.indices(), mRunNNeighbors);
      dists = MathUtils.subarray(result.weights(), mRunNNeighbors);
    }

    UmapProgress.update();

    final int adjustedLocalConnectivity = Math.max(0, mLocalConnectivity - 1);
    final float[][] sigmasRhos = smoothKnnDist(dists, mRunNNeighbors, adjustedLocalConnectivity);
    final float[] sigmas = sigmasRhos[0];
    final float[] rhos = sigmasRhos[1];
    CooMatrix graph = computeMembershipStrengths(indices, dists, sigmas, rhos, instances.rows(), mRawData.rows());

    UmapProgress.update();

    // This was a very specially constructed graph with constant degree.
    // That lets us do fancy unpacking by reshaping the Csr matrix indices
    // and data. Doing so relies on the constant degree assumption!
    final CsrMatrix csrGraph = graph.toCsr().l1Normalize().toCsr();
    final int[][] inds = csrGraph.reshapeIndicies(instances.rows(), mRunNNeighbors);
    final float[][] weights = csrGraph.reshapeWeights(instances.rows(), mRunNNeighbors);
    final Matrix embedding = initTransform(inds, weights, mEmbedding);

    final int nEpochs;
    if (mNEpochs == null) {
      // For smaller datasets we can use more epochs
      if (graph.rows() <= 10000) {
        nEpochs = 100;
      } else {
        nEpochs = 30;
      }
    } else {
      nEpochs = mNEpochs; // 3.0
    }

    MathUtils.zeroEntriesBelowLimit(graph.data(), MathUtils.max(graph.data()) / (float) nEpochs);
    graph = graph.eliminateZeros().toCoo();

    final float[] epochsPerSample = makeEpochsPerSample(graph.data(), nEpochs);

    final int[] head = graph.row();
    final int[] tail = graph.col();

    UmapProgress.update();
    UmapProgress.incTotal(nEpochs);
    final Matrix matrix = optimizeLayout(embedding, mEmbedding.copy(), head, tail, nEpochs, graph.cols(), epochsPerSample, mRunA, mRunB, mRandom, mRepulsionStrength, mInitialAlpha, mNegativeSampleRate, mVerbose);

    UmapProgress.finished();

    return matrix;
  }

  /**
   * Transform instances into the existing embedded space and return that
   * transformed output.
   * @param instances array, shape <code>(nSamples, nFeatures)</code>
   * New data to be transformed.
   * @return array, shape <code>(nSamples, nComponents)</code>
   * Embedding of the new data in low-dimensional space.
   * @throws IllegalArgumentException If we fit just a single instance then error.
   */
  public float[][] transform(final float[][] instances) {
    return transform(new DefaultMatrix(instances)).toArray();
  }
}
