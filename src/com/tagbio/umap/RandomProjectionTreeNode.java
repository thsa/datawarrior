/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package com.tagbio.umap;

/**
 * Node in a random projection tree.
 * @author Sean A. Irvine
 * @author Richard Littin
 */
class RandomProjectionTreeNode {

  private final int[] mIndices;
  private final Hyperplane mHyperplane;
  private final Float mOffset;
  private final RandomProjectionTreeNode mLeftChild;
  private final RandomProjectionTreeNode mRightChild;

  RandomProjectionTreeNode(final int[] indices, final Hyperplane hyperplane, final Float offset, final RandomProjectionTreeNode leftChild, final RandomProjectionTreeNode rightChild) {
    mIndices = indices;
    mHyperplane = hyperplane;
    mOffset = offset;
    mLeftChild = leftChild;
    mRightChild = rightChild;
  }

  private boolean isLeaf() {
    return mLeftChild == null && mRightChild == null;
  }

  private int numNodes() {
    return 1 + (mLeftChild != null ? mLeftChild.numNodes() : 0) + (mRightChild != null ? mRightChild.numNodes() : 0);
  }

  private int numLeaves() {
    return isLeaf() ? 1 : mLeftChild.numLeaves() + mRightChild.numLeaves();
  }

  private int[] recursiveFlatten(final Object hyperplanes, final float[] offsets, final int[][] children, final int[][] indices, final int nodeNum, final int leafNum) {
    if (isLeaf()) {
      children[nodeNum] = new int[]{-leafNum, -1};
      //indices[leafNum, :tree.getIndices().shape[0]] =tree.getIndices();
      indices[leafNum] = mIndices;
      return new int[]{nodeNum, leafNum + 1};
    } else {
      if (mHyperplane.shape().length > 1) {
        // sparse case
        ((float[][][]) hyperplanes)[nodeNum] = new float[][] {mHyperplane.data()}; // todo dubious
        //hyperplanes[nodeNum][:, :tree.getHyperplane().shape[1]] =tree.getHyperplane();
      } else {
        ((float[][]) hyperplanes)[nodeNum] = mHyperplane.data();
      }
      offsets[nodeNum] = mOffset;
      final int[] flattenInfo = mLeftChild.recursiveFlatten(hyperplanes, offsets, children, indices, nodeNum + 1, leafNum);
      children[nodeNum] = new int[] {nodeNum + 1, flattenInfo[0] + 1};
      return mRightChild.recursiveFlatten(hyperplanes, offsets, children, indices, flattenInfo[0] + 1, flattenInfo[1]);
    }
  }

  // Determine the most number on non zeros in a hyperplane entry.
  private int maxSparseHyperplaneSize() {
    if (isLeaf()) {
      return 0;
    } else {
      return Math.max(mHyperplane.shape()[1], Math.max(mLeftChild.maxSparseHyperplaneSize(), mRightChild.maxSparseHyperplaneSize()));
    }
  }

  FlatTree flatten() {
    final int nNodes = numNodes();
    final int numLeaves = numLeaves();

    final Object hyperplanes;
    if (mHyperplane.shape().length > 1) {
      // sparse case
      final int maxHyperplaneNnz = maxSparseHyperplaneSize();
      hyperplanes = new float[nNodes][mHyperplane.shape()[0]][maxHyperplaneNnz];
    } else {
      hyperplanes = new float[nNodes][mHyperplane.shape()[0]];
    }
    final float[] offsets = new float[nNodes];
    final int[][] children = new int[nNodes][];
    final int[][] indices = new int[numLeaves][];
    recursiveFlatten(hyperplanes, offsets, children, indices, 0, 0);
    return new FlatTree(hyperplanes, offsets, children, indices);
  }

}
