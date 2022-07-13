/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap;

import java.util.Arrays;
import java.util.Random;

/**
 * Arrays of heaps structure.
 * @author Leland McInnes (Python)
 * @author Sean A. Irvine
 * @author Richard Littin
 */
class Heap {

  private final int[][] mIndices;
  private final float[][] mWeights;
  private final boolean[][] mIsNew;

  private Heap(final int[][] indices, final float[][] weights) {
    mIndices = indices;
    mWeights = weights;
    mIsNew = new boolean[indices.length][indices[0].length];
  }

  /**
   * Construct an array of heaps. The heaps are used
   * for approximate nearest neighbor search, maintaining a list of potential
   * neighbors sorted by their distance. We also flag if potential neighbors
   * are newly added to the list or not.
   * @param points The number of data points to track in the heap.
   * @param size The number of items to keep on the heap for each data point.
   */
  Heap(final int points, final int size) {
    mIndices = new int[points][size];
    mWeights = new float[points][size];
    mIsNew = new boolean[points][size];
    for (final int[] a : mIndices) {
      Arrays.fill(a, -1);
    }
    for (final float[] a : mWeights) {
      Arrays.fill(a, Float.POSITIVE_INFINITY);
    }
  }

  int index(int row, int col) {
    return mIndices[row][col];
  }

  int[][] indices() {
    return mIndices;
  }

  float[][] weights() {
    return mWeights;
  }

  boolean isNew(int row, int col) {
    return mIsNew[row][col];
  }

  /**
   * Push a new element onto the heap. The heap stores potential neighbors
   * for each data point. The <code>row</code> parameter determines which data point we
   * are addressing, the <code>weight</code> determines the distance (for heap sorting),
   * the <code>index</code> is the element to add, and the flag determines whether this
   * is to be considered a new addition.
   * @param row Which actual heap within the heap object to push to
   * @param weight The priority value of the element to push onto the heap
   * @param index The actual value to be pushed
   * @param flag Whether to flag the newly added element or not.
   * @return True iff the pushed element is new.
   */
  boolean push(final int row, final float weight, final int index, final boolean flag) {
    synchronized (mIndices[row]) {
      final int[] indices = mIndices[row];
      final float[] weights = mWeights[row];
      final boolean[] isNew = mIsNew[row];

      if (weight >= weights[0]) {
        return false;
      }

      // break if we already have this element.
      for (final int value : indices) {
        if (index == value) {
          return false;
        }
      }

      // insert val at position zero
      weights[0] = weight;
      indices[0] = index;
      isNew[0] = flag;

      // descend the heap, swapping values until the max heap criterion is met
      int i = 0;
      while (true) {
        final int ic1 = 2 * i + 1;
        final int ic2 = ic1 + 1;
        final int iSwap;

        if (ic1 >= mIndices[0].length) {
          break;
        } else if (ic2 >= mIndices[0].length) {
          if (weights[ic1] > weight) {
            iSwap = ic1;
          } else {
            break;
          }
        } else if (weights[ic1] >= weights[ic2]) {
          if (weight < weights[ic1]) {
            iSwap = ic1;
          } else {
            break;
          }
        } else {
          if (weight < weights[ic2]) {
            iSwap = ic2;
          } else {
            break;
          }
        }

        weights[i] = weights[iSwap];
        indices[i] = indices[iSwap];
        isNew[i] = isNew[iSwap];

        i = iSwap;
      }

      weights[i] = weight;
      indices[i] = index;
      isNew[i] = flag;
      return true;
    }
  }

  /**
   * Push a new element onto the heap. The heap stores potential neighbors
   * for each data point. The <code>row</code> parameter determines which data point we
   * are addressing, the <code>weight</code> determines the distance (for heap sorting),
   * the <code>index</code> is the element to add, and the flag determines whether this
   * is to be considered a new addition.
   * @param row Which actual heap within the heap object to push to
   * @param weight The priority value of the element to push onto the heap
   * @param index The actual value to be pushed
   * @param flag Whether to flag the newly added element or not.
   * @return True iff the pushed element is new.
   */
  boolean uncheckedHeapPush(final int row, final float weight, final int index, final boolean flag) {
    final int[] indices = mIndices[row];
    final float[] weights = mWeights[row];
    final boolean[] isNew = mIsNew[row];

    if (weight >= weights[0]) {
      return false;
    }

    // insert val at position zero
    weights[0] = weight;
    indices[0] = index;
    isNew[0] = flag;

    // descend the heap, swapping values until the max heap criterion is met
    int i = 0;
    while (true) {
      final int ic1 = 2 * i + 1;
      final int ic2 = ic1 + 1;

      final int iSwap;
      if (ic1 >= mIndices[0].length) {
        break;
      } else if (ic2 >= mIndices[0].length) {
        if (weights[ic1] > weight) {
          iSwap = ic1;
        } else {
          break;
        }
      } else if (weights[ic1] >= weights[ic2]) {
        if (weight < weights[ic1]) {
          iSwap = ic1;
        } else {
          break;
        }
      } else {
        if (weight < weights[ic2]) {
          iSwap = ic2;
        } else {
          break;
        }
      }

      weights[i] = weights[iSwap];
      indices[i] = indices[iSwap];
      isNew[i] = isNew[iSwap];

      i = iSwap;
    }

    weights[i] = weight;
    indices[i] = index;
    isNew[i] = flag;
    return true;
  }

  // Restore the heap property for a heap with an out of place element
  // at position <code>elt</code>. This works with a heap pair where <code>heap1</code> carries
  // the weights and <code>heap2</code> holds the corresponding elements.
  private static void siftdown(final float[] heap1, final int[] heap2, final int length, final int elt) {
    int mid = elt;
    while (mid * 2 + 1 < length) {
      final int leftChild = mid * 2 + 1;
      final int rightChild = leftChild + 1;
      int swap = mid;

      if (heap1[swap] < heap1[leftChild]) {
        swap = leftChild;
      }

      if (rightChild < length && heap1[swap] < heap1[rightChild]) {
        swap = rightChild;
      }

      if (swap == mid) {
        break;
      } else {
        final float t = heap1[swap];
        heap1[swap] = heap1[mid];
        heap1[mid] = t;
        final int s = heap2[swap];
        heap2[swap] = heap2[mid];
        heap2[mid] = s;
        mid = swap;
      }
    }
  }

  /**
   * Given an array of heaps (of indices and weights), unpack the heap
   * out to give and array of sorted lists of indices and weights by increasing
   * weight. This is effectively just the second half of heap sort (the first
   * half not being required since we already have the data in a heap).
   * @return sorted result
   */
   Heap deheapSort() {
     for (int i = 0; i < mIndices.length; ++i) {
       final int[] indHeap = mIndices[i];
       final float[] distHeap = mWeights[i];

       for (int j = 0; j < indHeap.length - 1; ++j) {
         final int s = indHeap[0];
         indHeap[0] = indHeap[indHeap.length - j - 1];
         indHeap[indHeap.length - j - 1] = s;
         final float t = distHeap[0];
         distHeap[0] = distHeap[distHeap.length - j - 1];
         distHeap[distHeap.length - j - 1] = t;

         //siftdown(distHeap[:distHeap.shape[0] - j - 1], indHeap[:indHeap.shape[0] - j - 1],  0    );
         siftdown(distHeap, indHeap, distHeap.length - j - 1, 0);
       }
     }
     return new Heap(mIndices, mWeights);
   }

  /**
   * Search the heap for the smallest element that is still flagged.
   * @param row Which of the heaps to search
   * @return The index of the smallest flagged element
   * of the <code>row</code>th heap, or -1 if no flagged
   * elements remain in the heap.
   */
  int smallestFlagged(final int row) {
    final int[] ind = mIndices[row];
    final float[] dist = mWeights[row];
    final boolean[] flag = mIsNew[row];

    float minDist = Float.POSITIVE_INFINITY;
    int resultIndex = -1;

    for (int i = 0; i < ind.length; ++i) {
      if (flag[i] && dist[i] < minDist) {
        minDist = dist[i];
        resultIndex = i;
      }
    }

    if (resultIndex >= 0) {
      flag[resultIndex] = false;
      return ind[resultIndex];
    } else {
      return -1;
    }
  }

  /**
   * Build a heap of candidate neighbors for nearest neighbor descent. For
   * each vertex the candidate neighbors are any current neighbors, and any
   * vertices that have the vertex as one of their nearest neighbors.
   * @param nVertices The total number of vertices in the graph.
   * @param nNeighbors The number of neighbor edges per node in the current graph.
   * @param maxCandidates The maximum number of new candidate neighbors.
   * @param random Random source
   * @return A heap with an array of (randomly sorted) candidate
   * neighbors for each vertex in the graph.
   */
  Heap buildCandidates(final int nVertices, final int nNeighbors, final int maxCandidates, final Random random) {
    final Heap candidateNeighbors = new Heap(nVertices, maxCandidates);
    for (int i = 0; i < nVertices; ++i) {
      for (int j = 0; j < nNeighbors; ++j) {
        if (mIndices[i][j] < 0) {
          continue;
        }
        final int idx = mIndices[i][j];
        final boolean isn = mIsNew[i][j];
        final float d = random.nextFloat();
        candidateNeighbors.push(i, d, idx, isn);
        candidateNeighbors.push(idx, d, i, isn);
        mIsNew[i][j] = false;
      }
    }
    return candidateNeighbors;
  }
}
