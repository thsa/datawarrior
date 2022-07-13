/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.tagbio.umap.metric.Metric;

/**
 * Nearest neighbor descent for a specified distance metric.
 * Nondeterministic parallel version.
 * @author Leland McInnes (Python)
 * @author Sean A. Irvine
 * @author Richard Littin
 */
class ParallelNearestNeighborDescent extends  NearestNeighborDescent {

  private final int mThreads;

  /**
   * Construct a nearest neighbor descent object for the given metric.
   * @param metric distance function
   * @param threads number of threads
   */
  ParallelNearestNeighborDescent(final Metric metric, final int threads) {
    super(metric);
    if (threads < 1) {
      throw new IllegalArgumentException();
    }
    mThreads = threads;
  }

  @Override
  Heap descent(final Matrix data, final int nNeighbors, final Random random, final int maxCandidates, final boolean rpTreeInit, final int nIters, final List<FlatTree> forest) {
    return descent(data, nNeighbors, random, maxCandidates, rpTreeInit, nIters, forest, 0.001F, 0.5F);
  }

  @Override
  Heap descent(final Matrix data, final int nNeighbors, final Random random, final int maxCandidates, final boolean rpTreeInit, final int nIters, final List<FlatTree> forest, final float delta, final float rho) {
    final ExecutorService executor = Executors.newFixedThreadPool(mThreads);
    try {
      UmapProgress.incTotal(nIters);

      final List<Future<Integer>> futures = new ArrayList<>();

      final int nVertices = data.rows();
      final Heap currentGraph = new Heap(data.rows(), nNeighbors);

      final int jobs = (int)(mThreads * (1 + MathUtils.log2(mThreads)));
      final int chunkSize = (nVertices + jobs - 1) / jobs;

      for (int t = 0; t < jobs; ++t) {
        final int lo = t * chunkSize;
        final int hi = Math.min((t + 1) * chunkSize, nVertices);
        futures.add(executor.submit(() -> {
          for (int i = lo; i < hi; ++i) {
            final float[] iRow = data.row(i);
            for (final int index : Utils.rejectionSample(nNeighbors, data.rows(), random)) {
              final float d = mMetric.distance(iRow, data.row(index));
              currentGraph.push(i, d, index, true);
              currentGraph.push(index, d, i, true);
            }
          }
          return 0;
        }));
      }
      waitForFutures(futures);

      if (rpTreeInit) {
        final int cs = (forest.size() + jobs - 1) / jobs;
        for (int t = 0; t < jobs; ++t) {
          final int lo = t * cs;
          final int hi = Math.min((t + 1) * cs, forest.size());
          futures.add(executor.submit(() -> {
            //System.out.println("T: " + lo + ":" + hi + " : " + leafArray.length);
            for (int l = lo; l < hi; ++l) {
              for (final int[] leaf : forest.get(l).getIndices()) {
                for (int i = 0; i < leaf.length; ++i) {
                  final float[] iRow = data.row(leaf[i]);
                  for (int j = i + 1; j < leaf.length; ++j) {
                    final float d = mMetric.distance(iRow, data.row(leaf[j]));
                    currentGraph.push(leaf[i], d, leaf[j], true);
                    currentGraph.push(leaf[j], d, leaf[i], true);
                  }
                }
              }
            }
            return 0;
          }));
        }
        waitForFutures(futures);
      }

      for (int n = 0; n < nIters; ++n) {
        if (mVerbose) {
          Utils.message("NearestNeighborDescent: " + (n + 1) + " / " + nIters);
        }

        final Heap candidateNeighbors = currentGraph.buildCandidates(nVertices, nNeighbors, maxCandidates, random);

        for (int t = 0; t < jobs; ++t) {
          final int lo = t * chunkSize;
          final int hi = Math.min((t + 1) * chunkSize, nVertices);
          futures.add(executor.submit(() -> {
            final boolean[] rejectStatus = new boolean[maxCandidates];
            int c = 0;
            for (int i = lo; i < hi; ++i) {
              for (int j = 0; j < maxCandidates; ++j) {
                rejectStatus[j] = random.nextFloat() < rho;
              }

              for (int j = 0; j < maxCandidates; ++j) {
                final int p = candidateNeighbors.index(i, j);
                if (p < 0) {
                  continue;
                }

                for (int k = 0; k <= j; ++k) {
                  final int q = candidateNeighbors.index(i, k);
                  if (q < 0 || (rejectStatus[j] && rejectStatus[k]) || (!candidateNeighbors.isNew(i, j) && !candidateNeighbors.isNew(i, k))) {
                    continue;
                  }

                  final float d = mMetric.distance(data.row(p), data.row(q));
                  if (currentGraph.push(p, d, q, true)) {
                    ++c;
                  }
                  if (currentGraph.push(q, d, p, true)) {
                    ++c;
                  }
                }
              }
            }
            return c;
          }));
        }
        final int c = waitForFutures(futures);

        if (c <= delta * nNeighbors * data.rows()) {
          UmapProgress.update(nIters - n);
          break;
        }
        UmapProgress.update();
      }

      return currentGraph.deheapSort();
    } catch (InterruptedException | ExecutionException ex) {
      throw new RuntimeException(ex);
    } finally {
      executor.shutdown();
    }
  }

  private static int waitForFutures(List<Future<Integer>> futures) throws InterruptedException, ExecutionException {
    //System.out.println("WAITING " + futures.size());
    int c = 0;
    for (Future<Integer> future : futures) {
      c += future.get();
    }
    futures.clear();
    //System.out.println("DONE " + futures.size() + " : " + c);
    return c;
  }
}
